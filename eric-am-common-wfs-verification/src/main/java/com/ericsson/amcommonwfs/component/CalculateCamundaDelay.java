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

import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RETRIES_DELAY;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CalculateCamundaDelay implements JavaDelegate {

    private static final int SECONDS_IN_A_WEEK = 604800;

    @Value("${app.command.execute.defaultTimeOut}")
    private String defaultTimeOut;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        long applicationTimeOut = Long.parseLong(execution.getVariable(APPLICATION_TIME_OUT) != null ?
                (String) execution.getVariable(APPLICATION_TIME_OUT) : defaultTimeOut);
        if (applicationTimeOut > SECONDS_IN_A_WEEK) {
            throw new IllegalArgumentException(String.format(
                    "application time out of %s is too large. This results in over a week of "
                            + "verification. If human intervention is required to apply day 1 configuration "
                            + "please consider skipping this verification as detailed in the CPI", applicationTimeOut));
        }
        long camundaDelay = calculateDelay(applicationTimeOut);
        String waitTime = getWaitTime(camundaDelay);
        LOGGER.info("Setting retry delay for delay for verification command {}", waitTime);
        execution.setVariable(RETRIES_DELAY, waitTime);
        setApplicationTimeout(execution, applicationTimeOut);
    }

    private static long calculateDelay(final long applicationTimeOut) {
        long camundaDelay;
        if (applicationTimeOut < 15) {
            camundaDelay = 3;
        } else if (applicationTimeOut < 300) {
            camundaDelay = applicationTimeOut / 10;
        } else {
            camundaDelay = 30;
        }
        return camundaDelay;
    }

    private static void setApplicationTimeout(DelegateExecution execution, final long applicationTimeOut) {
        Long waitTime = applicationTimeOut < 15 ? 15 : applicationTimeOut;
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(waitTime);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        execution.setVariable(APP_TIMED_OUT, false);
        LOGGER.info("Setting application timeout {}", timeout);
    }

    private static String getWaitTime(long waitTime) {
        int days = (int) TimeUnit.SECONDS.toDays(waitTime);
        long hours = TimeUnit.SECONDS.toHours(waitTime) -
                TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(waitTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(waitTime));
        long seconds = TimeUnit.SECONDS.toSeconds(waitTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(waitTime));
        return "P" + days + "DT" + hours + "H" + minutes + "M" + seconds + "S";
    }
}
