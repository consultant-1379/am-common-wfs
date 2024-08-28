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

import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_CODE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY_PREFIX;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.factory.HelmCommandParamsMapperFactory;
import com.ericsson.amcommonwfs.model.CommandContext;
import com.ericsson.amcommonwfs.util.Constant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CreateCommandContext implements JavaDelegate {

    private static final String VERSION = "v1";

    private final HelmCommandParamsMapperFactory helmCommandParamsMapperFactory;

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper mapper;

    public CreateCommandContext(HelmCommandParamsMapperFactory helmCommandParamsMapperFactory,
                                RedisTemplate<String, String> redisTemplate) {
        this.helmCommandParamsMapperFactory = helmCommandParamsMapperFactory;
        this.redisTemplate = redisTemplate;
        this.mapper = new ObjectMapper();
    }

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) throws JsonProcessingException {
        HelmCommandParamsMapper helmCommandParamsMapper = helmCommandParamsMapperFactory
                .getMapper((String) execution.getVariable(Constant.COMMAND_TYPE));

        Map<String, Object> commandParams = helmCommandParamsMapper.apply(execution);
        CommandContext commandContext = new CommandContext()
                .setVersion(VERSION)
                .setHelmClientVersion((String) execution.getVariable(HELM_CLIENT_VERSION))
                .setCommandType((String) execution.getVariable(Constant.COMMAND_TYPE))
                .setCommandParams(commandParams);

        String helmExecutorRedisKey = HELM_EXECUTOR_REDIS_KEY_PREFIX + "-" + UUID.randomUUID();

        convertAndSet(helmExecutorRedisKey, commandContext);

        setTimeout(execution);
        execution.setVariable(ERROR_CODE, helmCommandParamsMapper.getErrorCode().getErrorCodeAsString());
        execution.setVariable(HELM_EXECUTOR_REDIS_KEY, helmExecutorRedisKey);
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
        LOGGER.info("Setting timeout for helm command context execution {}", waitTime);
        execution.setVariable("waitTime", waitTime);
    }

    private void convertAndSet(final String helmExecutorRedisKey,
                               final CommandContext commandContext) throws JsonProcessingException {
        Long timeout = (Long) commandContext.getCommandParams().get(Constant.TIMEOUT);
        String jsonString = mapper.writeValueAsString(commandContext);
        redisTemplate.opsForValue().set(helmExecutorRedisKey, jsonString, Duration.ofSeconds(timeout));
    }
}
