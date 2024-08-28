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

import static com.ericsson.amcommonwfs.VerifyTaskConstants.IS_ANNOTATED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VerifyApplicationDeployed implements JavaDelegate {

    @Autowired
    private VerificationHelper verificationHelper;

    @Autowired
    private VerifyExecution verifyExecution;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timeout = (long) execution.getVariable(APP_TIMEOUT);
        if (currentTime >= timeout) {
            execution.setVariable(APP_TIMED_OUT, true);
        }
        if (isAnnotated(execution)) {
            verificationHelper.verifyApplicationDeployedUsingAnnotation(execution);
        } else {
            verificationHelper.verifyApplicationDeployed(execution);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage());
        }
    }

    private boolean isAnnotated(final DelegateExecution execution) {
        String definitionKey = verifyExecution.getDefinitionKey(execution);
        if (UPGRADE_DEFINITION_KEY.equals(definitionKey) || SCALE_DEFINITION_KEY.equals(definitionKey)) {
            return false;
        }
        return (boolean) execution.getVariable(IS_ANNOTATED);
    }
}
