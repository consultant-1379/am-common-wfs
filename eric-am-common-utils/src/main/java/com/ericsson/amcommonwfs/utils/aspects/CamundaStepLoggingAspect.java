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

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOutAspect;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;


@Aspect
@Component
@Slf4j
public class CamundaStepLoggingAspect {

    @Before("@annotation(com.ericsson.amcommonwfs.utils.CamundaStepLogging)")
    public void logBefore(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        DelegateExecution execution = (DelegateExecution) ((Stream) Arrays.stream(args).sequential())
            .findFirst()
            .orElse(new ExecutionEntity());
        String timeOut = resolveTimeOutAspect(execution);
        String commandType = (String) execution.getVariable("commandType");
        if (commandType != null) {
            String releaseName = (String) execution.getVariable(RELEASE_NAME);
            LOGGER.info("CAMUNDA START STEP: {}.{} with timeout: {}", className, methodName, timeOut);
            LOGGER.info("CAMUNDA START STEP: {}.{} with commandType {} and releaseName {}",
                className, methodName, commandType, releaseName);
        } else {
            LOGGER.info("CAMUNDA START STEP: {}.{}", className, methodName);
        }
    }

    @After("@annotation(com.ericsson.amcommonwfs.utils.CamundaStepLogging)")
    public void logAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        LOGGER.info("CAMUNDA FINISH STEP: {}. {}", className, methodName);
    }
}
