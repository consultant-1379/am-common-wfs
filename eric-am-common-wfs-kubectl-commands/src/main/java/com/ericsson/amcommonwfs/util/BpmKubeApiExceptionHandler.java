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
package com.ericsson.amcommonwfs.util;

import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import io.kubernetes.client.openapi.ApiException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.http.HttpStatus;

import static com.ericsson.amcommonwfs.MessageConstants.FORBIDDEN_CLUSTER_MESSAGE;

@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class BpmKubeApiExceptionHandler {

    public static void handleGenericException(ErrorCode errorCode, DelegateExecution execution, ApiException e) {
        if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
            BusinessProcessExceptionUtils.handleException(errorCode, FORBIDDEN_CLUSTER_MESSAGE, execution);
        } else {
            String exceptionMessage = BusinessProcessExceptionUtils.buildApiExceptionMessage(e.getMessage(), e.getResponseBody());
            BusinessProcessExceptionUtils.handleException(errorCode, exceptionMessage, execution);
        }
    }
}
