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
package com.ericsson.amcommonwfs.utils.error;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BusinessProcessExceptionUtils {
    public static final String DEFAULT_ERROR_MESSAGE = "Error message is null, Unknown error occurred";

    private BusinessProcessExceptionUtils() {
        throw new UnsupportedOperationException();
    }

    public static void throwBusinessProcessException(ErrorCode errorCode, String exceptionMessage) {
        String notNullExceptionMessage = defaultIfBlank(exceptionMessage, DEFAULT_ERROR_MESSAGE);
        LOGGER.error("Exception occurred during business process exceptionMessage={}", notNullExceptionMessage);
        throw new BpmnError(errorCode.getErrorCodeAsString(), notNullExceptionMessage);
    }

    public static void handleException(String errorCode, String exceptionMessage, DelegateExecution execution) {
        LOGGER.error("Exception occurred during business process errorCode={}, exceptionMessage={},", errorCode, exceptionMessage);

        final String currentExecutionError = (String) execution.getVariable(ERROR_MESSAGE);
        if (isEmpty(currentExecutionError)) {
            execution.setVariable(ERROR_MESSAGE, exceptionMessage);
        }
        throw new BpmnError(errorCode, exceptionMessage);
    }

    public static void handleException(ErrorCode errorCode, String exceptionMessage, DelegateExecution execution) {
        handleException(errorCode.getErrorCodeAsString(), exceptionMessage, execution);
    }

    public static String buildApiExceptionMessage(String errorMessage, String apiResponseBody) {
        if (errorMessage == null && apiResponseBody == null) {
            return DEFAULT_ERROR_MESSAGE;
        } else if (apiResponseBody == null) {
            return errorMessage;
        }
        return String.format("Exception occurred, message=%s, apiResponse=%s", errorMessage, apiResponseBody);
    }
}
