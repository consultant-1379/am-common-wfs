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
package com.ericsson.amcommonwfs.service;

import brave.Span;
import brave.Tracing;
import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTION_ID_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY;

@Component
@Slf4j
public class ScheduleHelmJob implements JavaDelegate {

    private final HelmCommandJobService helmCommandJobService;

    private final Tracing tracing;

    @Autowired
    public ScheduleHelmJob(HelmCommandJobService helmCommandJobService, Tracing tracing) {
        this.helmCommandJobService = helmCommandJobService;
        this.tracing = tracing;
    }

    @Override
    @CamundaStepLogging
    @Observed
    public void execute(DelegateExecution execution) throws Exception {
        final String contextKey = (String) execution.getVariable(HELM_EXECUTOR_REDIS_KEY);
        execution.setVariable(HELM_EXECUTION_ID_KEY, execution.getProcessInstanceId());
        helmCommandJobService.submit(execution.getProcessInstanceId(), contextKey, getTraceId());
    }

    private String getTraceId() {
        Span span = tracing.tracer().currentSpan();
        return span.context().traceIdString();
    }
}
