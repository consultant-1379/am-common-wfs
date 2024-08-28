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

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

@Component
public class LongRunningCommandExecutionTimeout implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) throws Exception {
        String message = "Helm/kubectl command has timed out";
        BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_COMMAND_TIMEOUT_EXCEPTION, message, execution);
    }
}
