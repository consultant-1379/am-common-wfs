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

import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_TERMINATED;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFY_CMD_EXEC_RESULT;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationTerminationTask implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info(APP_TERMINATED);
        execution.setVariable(VERIFY_CMD_EXEC_RESULT, APP_TERMINATED);
    }
}
