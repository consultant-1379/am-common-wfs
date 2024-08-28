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
package com.ericsson.amcommonwfs.component;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.CONDITION_TIMED_OUT_MESSAGE;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.INSTANCE_LABEL;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.NO_MATCHING_RESOURCES_MESSAGE;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.PODS_NOT_READY_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_DEPLOYED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VerifyDeployedTask implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) throws Exception {
        final String podReadyStatus = ((StringBuilder) execution.getVariable(COMMAND_OUTPUT)).toString();
        final int commandExitValue = (Integer) execution.getVariable(COMMAND_EXIT_STATUS);
        final String releaseName = (String) execution.getVariable(RELEASE_NAME);

        execution.setVariable(APP_DEPLOYED, false);

        if (commandExitValue == 0) {
            LOGGER.info("Command exited with value of 0, verifying output contains no errors.");
            checkOutputForErrorMessage(podReadyStatus, execution, releaseName);
            LOGGER.info("No error messages were found in the command output.");
            execution.setVariable(APP_DEPLOYED, true);
        } else if (commandExitValue == 1) {
            LOGGER.error("Command exited with value of 1, checking output to determine the exit value cause.");
            checkOutputForErrorMessage(podReadyStatus, execution, releaseName);
        } else {
            LOGGER.error("Error when verifying successful pods' readiness, command returned exit value" +
                    " of {}. Printing command output to console:\n{}", commandExitValue, podReadyStatus);
        }
    }

    private static void checkOutputForErrorMessage(final String podReadyStatusOutput, final DelegateExecution execution,
                                                   final String releaseName) {
        if (podReadyStatusOutput.contains(CONDITION_TIMED_OUT_MESSAGE)) {
            execution.setVariable(APP_DEPLOYED, false);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_APPLICATION_DEPLOYED_TIMEOUT_EXCEPTION,
                    PODS_NOT_READY_ERROR_MESSAGE, execution);
        } else if (podReadyStatusOutput.contains(NO_MATCHING_RESOURCES_MESSAGE)) {
            LOGGER.info("Cannot determine if application resources were successfully deployed." +
                    " No pod resources were found associated with release name:" +
                    " {} matching label: {}.", releaseName, INSTANCE_LABEL);
            LOGGER.info("As no resources have been found matching label: {}, " +
                    "the application will be understood to have deployed successfully", INSTANCE_LABEL);
            LOGGER.info("It is highly recommended to check the status of the application " +
                    "resources to determine successful deployment.");
            execution.setVariable(APP_DEPLOYED, true);
        }
    }
}
