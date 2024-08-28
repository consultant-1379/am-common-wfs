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
package com.ericsson.amcommonwfs.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ericsson.amcommonwfs.presentation.repositories.RequestProcessingDetailsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("prod")
public class RequestDetailsCleanupJob {

    private final RequestProcessingDetailsMapper requestProcessingDetailsMapper;

    @Autowired
    public RequestDetailsCleanupJob(RequestProcessingDetailsMapper requestProcessingDetailsMapper) {
        this.requestProcessingDetailsMapper = requestProcessingDetailsMapper;
    }

    @Value("${idempotency.requestDetailsExpirationSeconds}")
    private long requestDetailsExpirationSeconds;

    @Scheduled(fixedDelay = 5000)
    public void cleanUpOldRequestDetails() {
        int cleanedUp = requestProcessingDetailsMapper
                .deleteExpiredRequestProcessingDetails(LocalDateTime.now(ZoneOffset.UTC)
                                                               .minusSeconds(requestDetailsExpirationSeconds));
        if (cleanedUp > 0) {
            LOGGER.info("Cleaned {} expired request processing details", cleanedUp);
        }
    }
}
