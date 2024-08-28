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

import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_CODE;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PostLongRunningCommandExecution implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info("Received notification of a long running command for process Id {}", execution.getProcessInstanceId());
        String commandType = (String) execution.getVariable("commandType");
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder((String) execution.getVariable(COMMAND_OUTPUT)));
        int commandExitStatus = (int) execution.getVariable(COMMAND_EXIT_STATUS);
        if (commandExitStatus != 0 && !isCrdFailedWithReleaseAlreadyExists(commandType, execution)) {
            String commandOutput = ((StringBuilder) execution.getVariable(COMMAND_OUTPUT)).toString();
            LOGGER.info("Command failed with output: {}", commandOutput);
            ErrorCode errorCode = ErrorCode.getErrorCode((String) execution.getVariable(ERROR_CODE));
            String errorOutput = errorCode.translate(commandOutput);
            LOGGER.info("Translating output to: {}", errorOutput);
            BusinessProcessExceptionUtils.handleException((String) execution.getVariable(ERROR_CODE), errorOutput, execution);
        }
    }

    private static boolean isCrdFailedWithReleaseAlreadyExists(final String commandType, final DelegateExecution execution) {
        String commandOutput = ((StringBuilder) execution.getVariable(COMMAND_OUTPUT)).toString();
        LOGGER.info("Failed due to : {} for command : {} ", commandOutput, commandType);
        return CommandType.CRD.getCommandType() == commandType && commandOutput.contains("release: already exists");
    }
}