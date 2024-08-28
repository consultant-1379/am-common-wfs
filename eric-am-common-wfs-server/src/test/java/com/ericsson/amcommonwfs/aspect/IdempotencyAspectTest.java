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
package com.ericsson.amcommonwfs.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.amcommonwfs.logging.InMemoryAppender;
import com.ericsson.amcommonwfs.presentation.repositories.RequestProcessingDetailsMapper;
import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;
import com.ericsson.amcommonwfs.util.Constants;
import com.ericsson.amcommonwfs.util.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        IdempotencyAspect.class,
        IdempotencyService.class,
        RequestProcessingDetailsMapper.class,
        ObjectMapper.class, })
public class IdempotencyAspectTest {

    @Autowired
    private IdempotencyAspect idempotencyAspect;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private RequestProcessingDetailsMapper requestProcessingDetailsMapper;

    @MockBean
    ObjectMapper objectMapper;

    static private InMemoryAppender inMemoryAppender;
    static private final Logger LOGGER = (Logger) LoggerFactory.getLogger("com.ericsson.amcommonwfs");

    final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

    final ResponseEntity<Object> response = new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT);

    @BeforeAll
    static public void init() {
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        LOGGER.addAppender(inMemoryAppender);
        LOGGER.setLevel(Level.INFO);
        inMemoryAppender.start();
    }

    @AfterEach
    public void reset() {
        inMemoryAppender.reset();
    }

    @Test
    public void testIdempotencyAspectIdempotencyKeyFail() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(response).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.setRequestURI("dummy");
            utilities.when(Utility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains(String.format("Idempotency key is not present for call to %s", "dummy"), Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectServletRequestIsNullFail() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(response).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = null;
            utilities.when(Utility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains("Unable to get incoming HttpServletRequest", Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectTrowIllegalStateExceptionFail() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(response).when(pjp).proceed();

            utilities.when(Utility::getCurrentHttpRequest).thenThrow(new IllegalStateException("dummy"));
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains("dummy", Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectSignatureIsNullFail() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(new Object()).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(Constants.IDEMPOTENCY_KEY_HEADER, "dummyKey");
            servletRequest.setRequestURI("dummyURI");

            utilities.when(Utility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            assertThat(inMemoryAppender.contains("Cannot invoke \"org.aspectj.lang.reflect.MethodSignature.getMethod()\" because \"signature\" is null",
                                                 Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectSuccessful() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(response).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(Constants.IDEMPOTENCY_KEY_HEADER, "dummyKey");
            utilities.when(Utility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            verify(idempotencyService, times(1)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectCheckInternalRuntimeExceptionFail() throws Throwable {
        try (MockedStatic<Utility> utilities = mockStatic(Utility.class)) {
            doReturn(response).when(pjp).proceed();

            String idempotencyKey = "dummyKey";
            doThrow(new IllegalArgumentException("")).when(idempotencyService).saveIdempotentResponse(eq(idempotencyKey), any());

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
            utilities.when(Utility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            verify(idempotencyService, times(1)).saveIdempotentResponse(any(), any());

            assertThat(inMemoryAppender.contains(String.format("The request processing details was not updated for idempotencyKey = %s",
                                                               idempotencyKey),
                                                 Level.ERROR)).isTrue();
        }
    }
}
