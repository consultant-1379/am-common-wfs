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
package com.ericsson.amcommonwfs;

import static com.ericsson.amcommonwfs.TaskConstants.REVISION_NUMBER_ERROR_MSG;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ValidateRevisionNumber {

    @VisibleForTesting
    static void checkRevisionNumber(final DelegateExecution execute) {
        String revisionNumber = (String) execute.getVariable(REVISION_NUMBER);
        if (!isValidRevisionNumber(revisionNumber)) {
            BusinessProcessExceptionUtils
                    .handleException(ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION, REVISION_NUMBER_ERROR_MSG, execute);
        }
        LOGGER.info("Validated revision number {}", revisionNumber);
    }

    @VisibleForTesting
    static boolean isValidRevisionNumber(final String revisionNumber) {
        return StringUtils.isNumeric(revisionNumber);
    }

}
