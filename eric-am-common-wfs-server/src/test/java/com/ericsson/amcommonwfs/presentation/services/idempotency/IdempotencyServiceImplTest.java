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
package com.ericsson.amcommonwfs.presentation.services.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ericsson.amcommonwfs.model.ProcessingState;
import com.ericsson.amcommonwfs.model.entity.RequestProcessingDetails;
import com.ericsson.amcommonwfs.presentation.repositories.RequestProcessingDetailsMapper;
import com.ericsson.amcommonwfs.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        IdempotencyServiceImpl.class,
        ObjectMapper.class
})
class IdempotencyServiceImplTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestProcessingDetailsMapper requestProcessingDetailsMapper;

    @MockBean
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }

    @Test
    void testSaveIdempotentResponse() throws Exception {
        when(requestProcessingDetailsMapper.findById(any())).thenReturn(buildRequestProcessingDetails());
        var requestProcessingDetailsCaptor = ArgumentCaptor.forClass(RequestProcessingDetails.class);

        String idempotencyKey = "dummyKey";
        String body = "{\"name\": \"test\"}";
        Integer code = 201;
        Map<String, String> headers = Map.of("header", "dummy");

        ResponseEntity<String> responseToSave = buildResponseEntity(body, code, headers);

        idempotencyService.saveIdempotentResponse(idempotencyKey, responseToSave);

        verify(requestProcessingDetailsMapper, Mockito.times(1)).updateRequestProcessingDetails(requestProcessingDetailsCaptor.capture());

        RequestProcessingDetails savedDetails = requestProcessingDetailsCaptor.getValue();
        assertThat(savedDetails.getResponseBody()).isEqualTo(objectMapper.writeValueAsString(responseToSave.getBody()));
        assertThat(savedDetails.getResponseCode()).isEqualTo(responseToSave.getStatusCode().value());
        assertThat(savedDetails.getResponseHeaders()).isEqualTo(objectMapper.writeValueAsString(responseToSave.getHeaders()));
    }

    @Test
    void testCreateProcessingRequest() {
        final String idempotencyKey = "dummyKey";
        final String hash = "hash";
        final int retryAfter = 30;
        idempotencyService.createProcessingRequest(idempotencyKey, hash, retryAfter);

        var requestProcessingDetailsCaptor = ArgumentCaptor.forClass(RequestProcessingDetails.class);
        verify(requestProcessingDetailsMapper, Mockito.times(1)).insertRequestProcessingDetails(requestProcessingDetailsCaptor.capture());

        RequestProcessingDetails savedDetails = requestProcessingDetailsCaptor.getValue();
        assertThat(savedDetails.getId()).isEqualTo(idempotencyKey);
        assertThat(savedDetails.getRequestHash()).isEqualTo(hash);
        assertThat(savedDetails.getRetryAfter()).isEqualTo(retryAfter);
        assertThat(savedDetails.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(savedDetails.getCreationTime()).isNotNull();
    }

    @Test
    void testGetRequestProcessingDetails() {
        RequestProcessingDetails requestProcessingDetails = buildRequestProcessingDetails();
        when(requestProcessingDetailsMapper.findById(any())).thenReturn(requestProcessingDetails);

        RequestProcessingDetails response = idempotencyService.getRequestProcessingDetails("dummyKey");
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(requestProcessingDetails.getId());
        assertThat(response.getRequestHash()).isEqualTo(requestProcessingDetails.getRequestHash());
        assertThat(response.getRetryAfter()).isEqualTo(requestProcessingDetails.getRetryAfter());
        assertThat(response.getProcessingState()).isEqualTo(requestProcessingDetails.getProcessingState());
    }

    @Test
    void testBuildResponseEntity() throws Exception {
        String body = objectMapper.writeValueAsString("{\"name\": \"test\"}");
        Map<String, String> headers = Map.of("header", "dummy");
        int code = 201;

        RequestProcessingDetails requestProcessingDetails = buildRequestProcessingDetails();
        requestProcessingDetails.setResponseCode(code);
        requestProcessingDetails.setResponseBody(body);
        requestProcessingDetails.setResponseHeaders(objectMapper.writeValueAsString(headers));

        ResponseEntity<String> response = idempotencyService.buildResponseEntity(requestProcessingDetails);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(code));
        assertThat(response.getBody()).isEqualTo(body);
        assertThat(response.getHeaders()).isNotNull();
        HttpHeaders httpHeaders = response.getHeaders();
        assertThat(httpHeaders.get("header")).isNotNull();
        assertThat(httpHeaders.get("header").contains("dummy")).isTrue();
    }

    @Test
    void testExecuteIdempotentCall() throws Exception {
        var requestProcessingDetailsCaptor = ArgumentCaptor.forClass(RequestProcessingDetails.class);
        Supplier<ResponseEntity<String>> supplier = () -> new ResponseEntity<>("{\"name\": \"test\"}", null, HttpStatus.CREATED);

        when(httpServletRequest.getHeader(eq(Constants.IDEMPOTENCY_KEY_HEADER))).thenReturn("dummyKey");
        when(requestProcessingDetailsMapper.findById(any())).thenReturn(buildRequestProcessingDetails());

        ResponseEntity<String> response = idempotencyService.executeTransactionalIdempotentCall(supplier);

        verify(requestProcessingDetailsMapper, Mockito.times(1)).updateRequestProcessingDetails(requestProcessingDetailsCaptor.capture());

        RequestProcessingDetails savedDetails = requestProcessingDetailsCaptor.getValue();
        assertThat(savedDetails.getResponseBody()).isEqualTo(objectMapper.writeValueAsString("{\"name\": \"test\"}"));
        assertThat(savedDetails.getResponseCode()).isEqualTo(201);
        assertThat(savedDetails.getResponseHeaders()).isEqualTo("{}");
    }

    @Test
    void testExecuteIdempotentCallWithoutIdempotency() {
        Supplier<ResponseEntity<String>> supplier = () -> new ResponseEntity<>("{\"name\": \"test\"}", null, HttpStatus.CREATED);

        when(requestProcessingDetailsMapper.findById(any())).thenReturn(buildRequestProcessingDetails());

        ResponseEntity<String> response = idempotencyService.executeTransactionalIdempotentCall(supplier);

        verify(requestProcessingDetailsMapper, Mockito.times(0)).updateRequestProcessingDetails(any());
        assertThat(response).isNotNull();
    }

    private RequestProcessingDetails buildRequestProcessingDetails() {
        RequestProcessingDetails requestProcessingDetails = new RequestProcessingDetails();
        requestProcessingDetails.setId("idempotency-dummy-key");
        requestProcessingDetails.setRequestHash("dummy-hash");
        requestProcessingDetails.setRetryAfter(30);
        requestProcessingDetails.setProcessingState(ProcessingState.STARTED);
        return requestProcessingDetails;
    }

    private ResponseEntity<String> buildResponseEntity(String body, Integer code, Map<String, String> headers) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        headers.forEach(multiValueMap::add);
        return new ResponseEntity<>(body, multiValueMap, HttpStatus.valueOf(code));
    }
}