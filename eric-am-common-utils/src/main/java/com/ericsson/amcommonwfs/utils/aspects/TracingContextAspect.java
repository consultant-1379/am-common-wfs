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
package com.ericsson.amcommonwfs.utils.aspects;

import static com.ericsson.amcommonwfs.utils.constants.Constants.TRACING_CONTEXT;

import java.util.Map;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import brave.Span;
import brave.Tracing;

@Aspect
@Component
public class TracingContextAspect {

    @Autowired
    private Tracing tracing;

    @Before("execution(* org.camunda.bpm.engine.delegate.JavaDelegate.execute(..)) && !within(is(FinalType)) && args(execution)")
    @SuppressWarnings("unchecked")
    public void getTracingContext(DelegateExecution execution) {
        Map<String, String> tracingContextSerialized = (Map<String, String>) execution.getVariable(TRACING_CONTEXT);
        if (tracingContextSerialized == null) {
            return;
        }
        Span span = tracing.tracer()
                .toSpan(tracing.propagation().extractor(Map<String, String>::get).extract(tracingContextSerialized).context());
        tracing.tracer().withSpanInScope(span);
    }
}