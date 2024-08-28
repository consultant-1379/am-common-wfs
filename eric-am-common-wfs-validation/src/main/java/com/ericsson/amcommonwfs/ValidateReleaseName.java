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

import static com.ericsson.amcommonwfs.TaskConstants.RELEASE_NAME_ERROR_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.RELEASE_NAME_PATTERN;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.util.regex.Matcher;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidateReleaseName {

    @VisibleForTesting
    static void checkReleaseName(final DelegateExecution execute) {
        String releaseName = (String) execute.getVariable(RELEASE_NAME);
        if (!isValidReleaseName(releaseName)) {
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION, RELEASE_NAME_ERROR_MSG, execute);
        }
        LOGGER.info("Release name has been validated ");
    }

    @VisibleForTesting
    static boolean isValidReleaseName(final String variable) {
        Matcher m = RELEASE_NAME_PATTERN.matcher(variable);
        return m.matches();
    }

}
