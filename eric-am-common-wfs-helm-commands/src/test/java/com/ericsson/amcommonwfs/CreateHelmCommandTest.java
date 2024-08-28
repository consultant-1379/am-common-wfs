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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = { CreateHelmCommand.class })
@ContextConfiguration(classes = CreateHelmCommandTest.Config.class)
public class CreateHelmCommandTest {

    @Autowired
    private CreateHelmCommand createHelmCommand;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void verifyCorrectTimeoutFormat() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(4000);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        createHelmCommand.setTimeout(execution);
        assertThat(execution.getVariable("waitTime")).isEqualTo("P0DT1H6M41S");
    }

    @Test
    public void verifyCorrectTimeoutFormatWithDays() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(104001);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        createHelmCommand.setTimeout(execution);
        assertThat(execution.getVariable("waitTime")).isEqualTo("P1DT4H53M22S");
    }

    @Configuration
    static class Config {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
