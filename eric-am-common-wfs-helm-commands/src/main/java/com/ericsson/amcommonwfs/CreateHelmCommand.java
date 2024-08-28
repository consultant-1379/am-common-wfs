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

import static com.ericsson.amcommonwfs.constants.CommandConstants.COMMAND_TYPE;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_CODE;

import java.util.concurrent.TimeUnit;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CreateHelmCommand implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) throws Exception {
        HelmCommand helmCommand = HelmCommandFactory.getService((String) execution.getVariable(COMMAND_TYPE));
        helmCommand.createCommand(execution);
        setTimeout(execution);
        execution.setVariable(ERROR_CODE, helmCommand.getErrorCode().getErrorCodeAsString());
    }

    @VisibleForTesting
    public void setTimeout(DelegateExecution execution) {
        final String timeOut = resolveTimeOut(execution);
        int defaultTimeOutAsInt = Integer.parseInt(timeOut) + 2;
        int days = (int) TimeUnit.SECONDS.toDays(defaultTimeOutAsInt);
        long hours = TimeUnit.SECONDS.toHours(defaultTimeOutAsInt) -
                TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(defaultTimeOutAsInt) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(defaultTimeOutAsInt));
        long seconds = TimeUnit.SECONDS.toSeconds(defaultTimeOutAsInt) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(defaultTimeOutAsInt));
        String waitTime = "P" + days + "DT" + hours + "H" + minutes + "M" + seconds + "S";
        LOGGER.info("Setting timeout for helm command execution {}", waitTime);
        execution.setVariable("waitTime", waitTime);
    }
}
