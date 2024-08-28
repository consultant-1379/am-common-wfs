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
package com.ericsson.amcommonwfs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
public class WorkflowServiceExceptionClassifierRetryPolicy extends ExceptionClassifierRetryPolicy {
    private static final long serialVersionUID = 2683045748879390543L;
    private static final List<String> RETRYABLE_MESSAGES = List.of("Cipher not initialized");

    public WorkflowServiceExceptionClassifierRetryPolicy(int maxAttempts) {
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(maxAttempts);
        this.setExceptionClassifier(throwable -> resolveRetryPolicy(throwable, simpleRetryPolicy)); // NOSONAR
    }

    private static RetryPolicy resolveRetryPolicy(Throwable exception, SimpleRetryPolicy simpleRetryPolicy) {
        LOGGER.error("Request failed, exceptionMessage={}", exception.getMessage());
        if (isRetryableException(exception) || isRetryableMessage(exception)) {
            return simpleRetryPolicy;
        }

        return new NeverRetryPolicy();
    }

    private static boolean isRetryableException(Throwable e) {
        return e instanceof SocketTimeoutException
                || e instanceof SocketException
                || e instanceof TimeoutException;
    }

    private static boolean isRetryableMessage(Throwable throwable) {
        String exceptionMessage = throwable.getMessage();
        return exceptionMessage != null && RETRYABLE_MESSAGES.stream().anyMatch(exceptionMessage::contains);
    }
}
