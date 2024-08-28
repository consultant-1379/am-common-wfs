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

import static com.ericsson.amcommonwfs.util.Constants.IDEMPOTENCY_KEY_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;

import com.ericsson.amcommonwfs.model.ProcessingState;
import com.ericsson.amcommonwfs.model.entity.RequestProcessingDetails;
import com.ericsson.amcommonwfs.presentation.controllers.InternalResourceControllerImpl;
import com.ericsson.amcommonwfs.presentation.controllers.v3.ResourceApiControllerImpl;
import com.ericsson.amcommonwfs.presentation.repositories.RequestProcessingDetailsMapper;
import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest
@DirtiesContext
@AutoConfigureObservability
class IdempotencyFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestProcessingDetailsMapper requestProcessingDetailsMapper;

    @MockBean
    private InternalResourceControllerImpl internalResourceController;

    @MockBean
    private ResourceApiControllerImpl resourceApiController;

    @SpyBean
    private IdempotencyService idempotencyService;

    @Captor
    private ArgumentCaptor<RequestProcessingDetails> detailsArgumentCaptor;
    private static final String SCALE_DOWN_PATH = "/api/internal/kubernetes/pods/scale/down";


    @Test
    public void testIdempotencyFilterForNewRequest() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);
        String hash = calculateHash(SCALE_DOWN_PATH, "POST", body);
        String idempotencyKey = UUID.randomUUID().toString();
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(null);
        when(requestProcessingDetailsMapper.insertRequestProcessingDetails(detailsArgumentCaptor.capture()))
                .thenReturn(1);

        makePostRequest(SCALE_DOWN_PATH, body, idempotencyKey);

        RequestProcessingDetails capturedDetails = detailsArgumentCaptor.getValue();
        assertThat(capturedDetails.getId()).isNotNull();
        assertThat(capturedDetails.getRequestHash()).isEqualTo(hash);
        assertThat(capturedDetails.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(capturedDetails.getRetryAfter()).isEqualTo(5);
        assertThat(capturedDetails.getCreationTime()).isNotNull();
    }

    @Test
    public void testIdempotencyFilterForExistedRequest() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);
        String hash = calculateHash(SCALE_DOWN_PATH, "POST", body);
        String idempotencyKey = UUID.randomUUID().toString();
        RequestProcessingDetails details = buildRequestProcessingDetails(idempotencyKey, hash, ProcessingState.STARTED);
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(details);

        MvcResult result = makePostRequest(SCALE_DOWN_PATH, body, idempotencyKey);

        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_EARLY.value());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }

    @Test
    public void testIdempotencyFilterForExistedMultipartRequest() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        MockMultipartFile config = getConfigFile();
        String hash = calculateHash(SCALE_DOWN_PATH, "POST", config);
        RequestProcessingDetails details = buildRequestProcessingDetails(idempotencyKey, hash, ProcessingState.STARTED);
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(details);

        MvcResult result = makeMultipartPostRequest(SCALE_DOWN_PATH, config, idempotencyKey);

        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_EARLY.value());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }

    @Test
    public void testFinishedIdempotentRequest() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);
        String hash = calculateHash(SCALE_DOWN_PATH, "POST", body);
        String idempotencyKey = UUID.randomUUID().toString();
        RequestProcessingDetails details = buildRequestProcessingDetails(idempotencyKey, hash, ProcessingState.FINISHED);
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(details);

        MvcResult result = makePostRequest(SCALE_DOWN_PATH, body, idempotencyKey);

        HttpServletResponse response = result.getResponse();
        verify(internalResourceController, times(0))
                .scaleDown(any(), any());
        assertThat(response.getStatus()).isEqualTo(details.getResponseCode());
        assertThat(response.getHeader("header")).isEqualTo("dummy");
    }


    @Test
    public void testExpiredIdempotentRequest() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);
        String hash = calculateHash(SCALE_DOWN_PATH, "POST", body);
        String idempotencyKey = UUID.randomUUID().toString();
        RequestProcessingDetails details = buildRequestProcessingDetails(idempotencyKey, hash, ProcessingState.STARTED);
        details.setCreationTime(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(details);
        when(requestProcessingDetailsMapper.updateRequestProcessingDetails(detailsArgumentCaptor.capture()))
                .thenReturn(1);

        makePostRequest(SCALE_DOWN_PATH, body, idempotencyKey);

        RequestProcessingDetails capturedDetails = detailsArgumentCaptor.getValue();
        assertThat(capturedDetails.getId()).isEqualTo(idempotencyKey);
        assertThat(capturedDetails.getRequestHash()).isEqualTo(hash);
        assertThat(capturedDetails.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(capturedDetails.getRetryAfter()).isEqualTo(5);

        long periodBetween = ChronoUnit.SECONDS.between(LocalDateTime.now(ZoneOffset.UTC), details.getCreationTime());
        assertThat(periodBetween < 5).isTrue();
    }

    @Test
    public void testIdempotentRequestWithWrongHash() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);
        String idempotencyKey = UUID.randomUUID().toString();
        RequestProcessingDetails details = buildRequestProcessingDetails(idempotencyKey, "hash", ProcessingState.STARTED);
        when(requestProcessingDetailsMapper.findById(idempotencyKey)).thenReturn(details);

        MvcResult result = makePostRequest(SCALE_DOWN_PATH, body, idempotencyKey);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        verify(internalResourceController, times(0)).scaleDown(any(), any());
    }

    @Test
    public void testPostRequestWithoutIdempotencyHeader() throws Exception {
        InternalScaleInfo instantiateInfo = new InternalScaleInfo();
        String body = objectMapper.writeValueAsString(instantiateInfo);

        MvcResult result = makePostRequest(SCALE_DOWN_PATH, body, null);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        verify(internalResourceController, times(0)).scaleDown(any(), any());
    }

    private MvcResult makePostRequest(final String requestUrl, final String body, String idempotencyKey) throws Exception {

        final var requestBuilder = post(requestUrl)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        if (idempotencyKey != null) {
            requestBuilder.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeMultipartPostRequest(String requestUrl, MockMultipartFile multipartFile, String idempotencyKey) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                        .multipart(requestUrl)
                        .file(multipartFile)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .param("description", "Dummy cluster config")
                        .queryParam("isDefault", "true"))
                .andReturn();
    }

    private static String calculateHash(String url, String method, String body) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(url.getBytes());
        messageDigest.update(method.getBytes());
        messageDigest.update(body.getBytes());

        return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
    }

    private static String calculateHash(String url, String method, MultipartFile multipartFile) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(url.getBytes());
        messageDigest.update(method.getBytes());

        Map<String, String[]> parameterMap = new TreeMap<>();
        parameterMap.put("description", new String[]{"Dummy cluster config"});
        parameterMap.put("isDefault", new String[]{"true"});
        parameterMap.forEach((key, value1) -> {
                    messageDigest.update(key.getBytes());
                    Arrays.stream(value1).map(String::getBytes).forEach(messageDigest::update);
                });

        messageDigest.update("clusterConfig".getBytes());
        messageDigest.update(multipartFile.getBytes());

        return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
    }

    private static MockMultipartFile getConfigFile() throws IOException {
        Resource fileResource = new ClassPathResource("cluster01.config");

        return new MockMultipartFile(
                "clusterConfig", fileResource.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource.getInputStream());
    }

    private RequestProcessingDetails buildRequestProcessingDetails(String idempotency, String hash,
                                                                   ProcessingState processingState) throws JsonProcessingException {
        RequestProcessingDetails requestProcessingDetails = new RequestProcessingDetails();
        requestProcessingDetails.setId(idempotency);
        requestProcessingDetails.setRequestHash(hash);
        requestProcessingDetails.setRetryAfter(5);
        requestProcessingDetails.setProcessingState(processingState);
        requestProcessingDetails.setCreationTime(LocalDateTime.now(ZoneOffset.UTC));
        requestProcessingDetails.setResponseCode(221);
        requestProcessingDetails.setResponseBody(objectMapper.writeValueAsString("{\"name\": \"test\"}"));
        requestProcessingDetails.setResponseHeaders(objectMapper.writeValueAsString(Map.of("header", List.of("dummy"))));
        return requestProcessingDetails;
    }
}