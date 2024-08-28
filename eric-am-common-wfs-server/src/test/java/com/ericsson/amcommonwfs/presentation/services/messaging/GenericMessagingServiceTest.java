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
package com.ericsson.amcommonwfs.presentation.services.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;

@SpringBootTest(classes = GenericMessagingService.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "messaging.retry.time=3",
        "messaging.retry.interval=1",
        "spring.flyway.enabled=false"})
public class GenericMessagingServiceTest {

    private static final String IDEMPOTENCY_KEY = "dummyKey";

    @MockBean
    private MessagingHealth messagingHealth;

    @MockBean
    private MessagingService messagingService;

    @Autowired
    private GenericMessagingService genericMessagingService;

    @Test
    public void shouldNotSendWhenMessageServiceIsDown() throws JsonProcessingException {
        // given
        when(messagingHealth.isUp()).thenReturn(false);

        // when
        genericMessagingService.prepareAndSend(new Object(), IDEMPOTENCY_KEY);

        // then
        verify(messagingService, never()).sendMessage(any(), any());
    }

    @Test
    public void shouldRetrySendingAfterFailure() throws JsonProcessingException {
        // given
        when(messagingHealth.isUp()).thenReturn(true);
        doThrow(new RuntimeException("Sending failed")).doNothing().when(messagingService).sendMessage(any(), any());

        // when
        genericMessagingService.prepareAndSend(new Object(), IDEMPOTENCY_KEY);

        // then
        verify(messagingService, times(2)).sendMessage(any(), eq(IDEMPOTENCY_KEY));
    }

    @Test
    public void shouldStopRetryingAfterRetryTimeExceeded() throws JsonProcessingException {
        // given
        when(messagingHealth.isUp()).thenReturn(true);
        doThrow(new RuntimeException("Sending failed")).when(messagingService).sendMessage(any(), any());

        // when
        genericMessagingService.prepareAndSend(new Object(), IDEMPOTENCY_KEY);

        // then
        verify(messagingService, atLeast(3)).sendMessage(any(), eq(IDEMPOTENCY_KEY));
    }
}
