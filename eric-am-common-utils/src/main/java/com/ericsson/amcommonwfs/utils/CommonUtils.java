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
package com.ericsson.amcommonwfs.utils;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class CommonUtils {
    private static final int UPPER_BOUND_UI_APP_TIMEOUT = 1_000_000_000;

    public static String convertToJSONString(final String errorMsg, final String statusCode) {
        JSONObject jsonError = new JSONObject();
        jsonError.put("detail", errorMsg);
        jsonError.put("status", statusCode);
        return jsonError.toString();
    }

    public static String resolveTimeOut(DelegateExecution execution) {
        long appTimeout = (long) execution.getVariable(APP_TIMEOUT);
        return resolveTimeout(appTimeout);
    }

    public static String resolveTimeOutAspect(DelegateExecution execution) {
        Long appTimeout = (Long) execution.getVariable(APP_TIMEOUT);
        if (appTimeout == null) {
            return "is empty for this step";
        }
        return resolveTimeout(appTimeout);
    }

    @NotNull
    private static String resolveTimeout(Long appTimeout) {
        Instant instant = Instant.ofEpochSecond(appTimeout);
        LocalDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime currentTime = LocalDateTime.now();
        long remainingAppTime = ChronoUnit.SECONDS.between(currentTime, zonedDateTime);
        if (remainingAppTime < 0) {
            LOGGER.info("Application timeout has elapsed setting remainingAppTime to 0");
            remainingAppTime = 0;
        }
        return Long.toString(remainingAppTime);
    }

    public static boolean validateAppTimeout(String applicationTimeout) {
        if (!StringUtils.isNumeric(applicationTimeout)) {
            return false;
        }
        int timeout = Integer.parseInt(applicationTimeout);

        return Range.between(0, UPPER_BOUND_UI_APP_TIMEOUT).contains(timeout);
    }
}
