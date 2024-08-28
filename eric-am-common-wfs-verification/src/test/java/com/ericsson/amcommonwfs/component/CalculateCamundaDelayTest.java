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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RETRIES_DELAY;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.component.CalculateCamundaDelayTest.Config;

@SpringBootTest(classes = CalculateCamundaDelay.class)
@ContextConfiguration(classes = Config.class)
@TestPropertySource(properties = "app.command.execute.defaultTimeOut=270")
public class CalculateCamundaDelayTest {

    private ExecutionImpl execution = new ExecutionImpl();

    @Autowired
    private CalculateCamundaDelay calculateCamundaDelay;



    @Test
    public void testWithTinyTimeout(){
        execution.setVariable(APPLICATION_TIME_OUT, "1");
        calculateCamundaDelay.execute(execution);
        assertThat(execution.getVariable(RETRIES_DELAY)).isEqualTo("P0DT0H0M3S");
    }

    @Test
    public void testWithSmallOddTimeout(){
        execution.setVariable(APPLICATION_TIME_OUT, "21");
        calculateCamundaDelay.execute(execution);
        assertThat(execution.getVariable(RETRIES_DELAY)).isEqualTo("P0DT0H0M2S");
    }

    @Test
    public void testWithEvenMediumTimeout(){
        execution.setVariable(APPLICATION_TIME_OUT, "88");
        calculateCamundaDelay.execute(execution);
        assertThat(execution.getVariable(RETRIES_DELAY)).isEqualTo("P0DT0H0M8S");
    }

    @Test
    public void testWithOddLargeTimeout(){
        execution.setVariable(APPLICATION_TIME_OUT, "481");
        calculateCamundaDelay.execute(execution);
        assertThat(execution.getVariable(RETRIES_DELAY)).isEqualTo("P0DT0H0M30S");
    }

    @Test
    public void testWithMaxLongTimeout(){
        execution.setVariable(APPLICATION_TIME_OUT, Long.toString(Long.MAX_VALUE));
        assertThatThrownBy(() -> calculateCamundaDelay.execute(execution)).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("application time out");
    }

    @Test
    public void testWithTimeoutAtLimit(){
        execution.setVariable(APPLICATION_TIME_OUT, "604800");
        calculateCamundaDelay.execute(execution);
        assertThat(execution.getVariable(RETRIES_DELAY)).isEqualTo("P0DT0H0M30S");
    }

    @Configuration
    static class Config {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
