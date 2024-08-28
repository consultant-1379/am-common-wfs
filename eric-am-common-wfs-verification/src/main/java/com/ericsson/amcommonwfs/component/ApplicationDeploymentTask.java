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

import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_DEPLOYED_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_ROLLBACK_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_SCALE_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_UPGRADED_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.ERROR_UNKNOWN_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFY_CMD_EXEC_RESULT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationDeploymentTask implements JavaDelegate {

    @Autowired
    private VerifyExecution verifyExecution;

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        String definitionKey = verifyExecution.getDefinitionKey(execution);
        switch (definitionKey) {
            case INSTANTIATE_DEFINITION_KEY:
                LOGGER.info("Application is deployed successfully");
                execution.setVariable(VERIFY_CMD_EXEC_RESULT,
                        APP_DEPLOYED_SUCCESS + execution.getVariable(RELEASE_NAME));
                break;
            case UPGRADE_DEFINITION_KEY:
                LOGGER.info("Application is upgraded successfully");
                execution.setVariable(VERIFY_CMD_EXEC_RESULT,
                        APP_UPGRADED_SUCCESS + execution.getVariable(RELEASE_NAME));
                break;
            case ROLLBACK_DEFINITION_KEY:
                LOGGER.info("Application has been rolled back successfully");
                execution.setVariable(VERIFY_CMD_EXEC_RESULT,
                        APP_ROLLBACK_SUCCESS + execution.getVariable(RELEASE_NAME));
                break;
            case SCALE_DEFINITION_KEY:
                LOGGER.info("Application has scaled  successfully");
                execution.setVariable(VERIFY_CMD_EXEC_RESULT,
                        APP_SCALE_SUCCESS + execution.getVariable(RELEASE_NAME));
                break;
            case CRD_DEFINITION_KEY:
                LOGGER.info("CRDs have been installed/upgraded successfully");
                execution.setVariable(VERIFY_CMD_EXEC_RESULT,
                        APP_DEPLOYED_SUCCESS + execution.getVariable(RELEASE_NAME));
                break;
            default:
                LOGGER.error(ERROR_UNKNOWN_DEFINITION_KEY);
                BusinessProcessExceptionUtils
                        .handleException(BPMN_INVALID_ARGUMENT_EXCEPTION, ERROR_UNKNOWN_DEFINITION_KEY, execution);
        }
    }
}

