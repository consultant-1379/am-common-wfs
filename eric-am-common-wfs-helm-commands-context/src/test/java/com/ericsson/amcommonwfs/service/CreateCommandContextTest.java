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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.model.CommandType.INSTALL;
import static com.ericsson.amcommonwfs.model.CommandType.UPGRADE;
import static com.ericsson.amcommonwfs.util.Constant.COMMAND_TYPE;
import static com.ericsson.amcommonwfs.util.TestConstants.EXECUTION_VARIABLES_WITH_CHART_URL;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_REDIS_JSON_STRING;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_CODE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY_PREFIX;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_INSTALL_FAILED;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ContextConfiguration;

import com.ericsson.amcommonwfs.exception.CommandContextException;
import com.ericsson.amcommonwfs.factory.HelmCommandParamsMapperFactory;
import com.ericsson.amcommonwfs.mapper.InstallCommandParamsMapper;
import com.ericsson.amcommonwfs.util.MapperUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

@SpringBootTest(classes = { CreateCommandContext.class, HelmCommandParamsMapperFactory.class })
@ContextConfiguration(classes = CreateCommandContextTest.Config.class)
public class CreateCommandContextTest {

    @Autowired
    private CreateCommandContext createCommandContext;

    @Autowired
    private HelmCommandParamsMapperFactory helmCommandParamsMapperFactory;

    @Spy
    private ExecutionImpl execution;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    public void verifyCorrectTimeoutFormat() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(4000);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        createCommandContext.setTimeout(execution);
        assertThat(execution.getVariable("waitTime")).isEqualTo("P0DT1H6M41S");
    }

    @Test
    public void verifyCorrectTimeoutFormatWithDays() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(104001);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        createCommandContext.setTimeout(execution);
        assertThat(execution.getVariable("waitTime")).isEqualTo("P1DT4H53M22S");
    }

    @Test
    public void verifyExecute() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        execution.setVariable(COMMAND_TYPE, INSTALL.getCommandType());
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        createCommandContext.execute(execution);

        verify(valueOperations, times(1)).set(any(), eq(EXPECTED_REDIS_JSON_STRING), any());

        assertThat(execution.getVariable(ERROR_CODE)).isEqualTo(BPMN_INSTALL_FAILED.getErrorCodeAsString());
        assertTrue(execution.getVariable(HELM_EXECUTOR_REDIS_KEY).toString().startsWith(HELM_EXECUTOR_REDIS_KEY_PREFIX));
    }

    @Test
    public void verifyExecuteThrowException() {
        execution.setVariable(COMMAND_TYPE, UPGRADE.getCommandType());
        final CommandContextException exception = assertThrows(CommandContextException.class, () -> createCommandContext.execute(execution));
        assertThat(exception.getMessage()).isEqualTo("Unknown helm command context : " + UPGRADE.getCommandType());
    }

    @Configuration
    static class Config {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public List<HelmCommandParamsMapper> helmCommandContexts() {
            List<HelmCommandParamsMapper> mappers = new ArrayList<>();
            mappers.add(new InstallCommandParamsMapper());

            return mappers;
        }
    }
}
