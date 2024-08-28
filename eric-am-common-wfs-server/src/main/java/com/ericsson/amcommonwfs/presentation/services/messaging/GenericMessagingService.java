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

import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_PUBLISH_MESSAGE;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GenericMessagingService {

    @Autowired
    private MessagingHealth messagingHealth;

    @Autowired
    private MessagingService messagingService;

    @Value("${messaging.retry.time}")
    private String messageRetryTime;

    @Value("${messaging.retry.interval}")
    private String messageRetryInterval;

    public <T> void prepareAndSend(T message, String idempotencyKey) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime retryTimeout = currentTime.plusSeconds(Long.parseLong(messageRetryTime));
        while (currentTime.isBefore(retryTimeout)) {
            if (messagingHealth.isUp()) {
                try {
                    messagingService.sendMessage(message, idempotencyKey);
                    LOGGER.info("Message sent: {}", message);
                    return;
                } catch (Exception e) {
                    LOGGER.error(UNABLE_TO_PUBLISH_MESSAGE, e);
                }
            } else {
                LOGGER.warn(UNABLE_TO_PUBLISH_MESSAGE, "messaging service is down");
            }
            try {
                TimeUnit.SECONDS.sleep(Integer.parseInt(messageRetryInterval));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("An error occurred: ", e);
            }
            currentTime = LocalDateTime.now();
        }
        LOGGER.error("Messaging service failed to come up.");
    }
}
