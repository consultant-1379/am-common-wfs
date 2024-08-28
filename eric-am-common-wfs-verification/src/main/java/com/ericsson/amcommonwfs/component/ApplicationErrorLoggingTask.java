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

import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.IS_APPLICATION_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationErrorLoggingTask implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        final String commandOutput = ((StringBuilder) execution.getVariable(COMMAND_OUTPUT)).toString();
        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String errorMessage = (String) execution.getVariable(ERROR_MESSAGE);

        LOGGER.error(String.format(errorMessage  +
                " Printing the resources status of release %s to console....\n%s", releaseName, commandOutput));

        execution.setVariable(IS_APPLICATION_TIMED_OUT, true);
        execution.setVariable(ERROR_MESSAGE, commandOutput);
    }
}
