/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.presentation.controllers.filter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;

import com.ericsson.amcommonwfs.model.ProcessingState;
import com.ericsson.amcommonwfs.model.entity.RequestProcessingDetails;
import com.ericsson.amcommonwfs.presentation.controllers.filter.cachewrapper.CachedBodyRequestWrapper;
import com.ericsson.amcommonwfs.presentation.controllers.filter.cachewrapper.CachedMultipartRequestWrapper;
import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;
import com.ericsson.amcommonwfs.util.v3.ControllerUtilities;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import lombok.extern.slf4j.Slf4j;

import static com.ericsson.amcommonwfs.util.Constants.IDEMPOTENCY_KEY_HEADER;

@Slf4j
@Component
@Order(1)
public class IdempotencyFilter extends OncePerRequestFilter {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyProps idempotencyProps;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ObjectMapper objectMapper;

    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = httpServletRequest.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null) {
            LOGGER.error("Idempotency key isn't presented as header value for request {}", httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value());
            httpServletResponse.flushBuffer();
            return;
        }

        HttpServletRequest currentRequest = mapRequest(httpServletRequest);
        String hash;
        try {
            hash = calculateRequestHash(currentRequest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    String.format("Failed to generate digest for %s due to %s", httpServletRequest.getRequestURI(), e.getMessage()), e);
        }

        RequestProcessingDetails requestProcessingDetails = idempotencyService.getRequestProcessingDetails(idempotencyKey);

        if (requestProcessingDetails == null) {
            Integer retryAfter = idempotencyProps.findEndpointLatency(currentRequest.getRequestURI(), currentRequest.getMethod());
            idempotencyService.createProcessingRequest(idempotencyKey, hash, retryAfter);
            filterChain.doFilter(currentRequest, httpServletResponse);
        } else {
            proceedWithExistedRequestDetails(currentRequest, httpServletResponse, filterChain, requestProcessingDetails, hash);
        }
    }

    private static HttpServletRequest mapRequest(final HttpServletRequest httpServletRequest) throws IOException {
        if (WebUtils.getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class) == null) {
            if (httpServletRequest.getContentType() != null &&
                    httpServletRequest.getContentType().contains(ContentType.MULTIPART_FORM_DATA.getMimeType())) {
                return new CachedMultipartRequestWrapper(httpServletRequest);
            } else {
                return new CachedBodyRequestWrapper(httpServletRequest);
            }
        }
        return httpServletRequest;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) {
        return idempotencyProps.findEndpointLatency(request.getRequestURI(), request.getMethod()) == null;
    }

    private void proceedWithExistedRequestDetails(final HttpServletRequest httpServletRequest,
                                                  final HttpServletResponse httpServletResponse,
                                                  final FilterChain filterChain,
                                                  final RequestProcessingDetails requestProcessingDetails,
                                                  final String hash) throws IOException, ServletException {
        if (requestProcessingDetails.getRequestHash().equals(hash)) {
            if (requestProcessingDetails.getProcessingState() == ProcessingState.STARTED) {
                proceedWithStartedRequest(httpServletRequest, httpServletResponse, filterChain, requestProcessingDetails);
            } else {
                idempotencyService.updateResponseWithProcessedData(httpServletResponse, requestProcessingDetails);
            }
        } else {
            LOGGER.error("Request with the same idempotency key, but with different hash already exist for {}", httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    private void proceedWithStartedRequest(final HttpServletRequest httpServletRequest,
                                           final HttpServletResponse httpServletResponse,
                                           final FilterChain filterChain,
                                           final RequestProcessingDetails requestProcessingDetails) throws IOException, ServletException {
        LocalDateTime creationTime = requestProcessingDetails.getCreationTime();
        String idempotencyKey = requestProcessingDetails.getId();
        var process = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(idempotencyKey)
                .singleResult();

        if (process != null) {
            LOGGER.info("Request with the businessKey {} exist in Camunda", idempotencyKey);
            ResourceResponseSuccess responseResponse =
                    ControllerUtilities.prepareResourceResponseSuccess(process.getId(), runtimeService.getVariables(process.getId()));
            ResponseEntity<ResourceResponseSuccess> responseEntity =
                    ControllerUtilities.buildOperationResponse(responseResponse, httpServletRequest);
            idempotencyService.saveIdempotentResponse(idempotencyKey, responseEntity);

            httpServletResponse.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
            httpServletResponse.setStatus(responseEntity.getStatusCode().value());
        } else if (LocalDateTime.now(ZoneOffset.UTC).isAfter(creationTime.plusSeconds(2L * requestProcessingDetails.getRetryAfter()))) {
            idempotencyService.updateProcessingRequestCreationTime(requestProcessingDetails);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            LOGGER.info("Request {} processing still in progress", httpServletRequest.getRequestURI());
            httpServletResponse.setStatus(HttpStatus.TOO_EARLY.value());
            httpServletResponse.addHeader(HttpHeaders.RETRY_AFTER, requestProcessingDetails.getRetryAfter().toString());
        }
    }

    private static String calculateRequestHash(final HttpServletRequest httpServletRequest) throws IOException, NoSuchAlgorithmException {

        MultipartHttpServletRequest multipartRequest = WebUtils
                .getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class);

        if (multipartRequest != null) {
            return calculateMultipartRequestHash(multipartRequest);
        } else {
            return calculateInputStreamRequestHash(httpServletRequest);
        }
    }

    private static String calculateInputStreamRequestHash(final HttpServletRequest httpServletRequest) throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);

        digest.update(httpServletRequest.getRequestURI().getBytes());
        digest.update(httpServletRequest.getMethod().getBytes());
        digest.update(IOUtils.toByteArray(httpServletRequest.getInputStream()));

        byte[] hash = digest.digest();
        String stringHash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        LOGGER.debug("Hash {} has been calculated for request {}", hash, httpServletRequest.getRequestURI());
        return stringHash;
    }

    private static String calculateMultipartRequestHash(final MultipartHttpServletRequest multipartRequest) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);

        digest.update(multipartRequest.getRequestURI().getBytes());
        digest.update(multipartRequest.getMethod().getBytes());

        Map<String, String[]> parameterMap = new TreeMap<>(multipartRequest.getParameterMap());
        parameterMap.forEach((key, value1) -> {
            digest.update(key.getBytes());
            Arrays.stream(value1).map(String::getBytes).forEach(digest::update);
        });

        multipartRequest.getMultiFileMap().forEach((key, value) -> {
            digest.update(key.getBytes());
            value.forEach(file -> {
                try {
                    digest.update(file.getBytes());
                } catch (IOException e) {
                    LOGGER.warn("Multipart file {} content cannot be parsed", key);
                }
            });
        });

        byte[] hash = digest.digest();
        String stringHash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        LOGGER.debug("Hash {} has been calculated for request {}", hash, multipartRequest.getRequestURI());
        return stringHash;
    }
}

