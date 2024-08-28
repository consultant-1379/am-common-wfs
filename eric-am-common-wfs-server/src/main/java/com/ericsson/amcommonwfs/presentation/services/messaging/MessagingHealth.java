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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessagingHealth {
    @Autowired
    private RedisHealthIndicator healthIndicator;

    public boolean isUp() {
        LOGGER.debug("Checking health of messaging");
        Health health = healthIndicator.health();
        LOGGER.debug("Health check {}", health.getStatus());
        return Status.UP == health.getStatus();
    }
}
