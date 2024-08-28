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
package com.ericsson.amcommonwfs.presentation.services.messaging;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_BUS_RETRY_INTERVAL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_BUS_RETRY_TIME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_RETRIES_COMPLETED;

@Component
public class MessageBusRetryTime implements JavaDelegate {

    @Value("${messaging.retry.time}")
    private String messageRetryTime;

    @Value("${messaging.retry.interval}")
    private String messageRetryInterval;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) throws Exception {
        long retryTime = LocalDateTime.now().plusSeconds(Long.parseLong(messageRetryTime)).toEpochSecond(ZoneOffset.UTC);
        String retryInterval = "PT" + messageRetryInterval + "S";
        execution.setVariable(MESSAGE_BUS_RETRY_TIME, retryTime);
        execution.setVariable(MESSAGE_RETRIES_COMPLETED, false);
        execution.setVariable(MESSAGE_BUS_RETRY_INTERVAL, retryInterval);
    }
}
