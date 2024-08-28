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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_VERSION_IN_CLUSTER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PROCEED_WITH_CRD_INSTALL;

import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = CrdVersionLogic.class)
@ContextConfiguration(classes = CrdVersionLogicTest.Config.class)
public class CrdVersionLogicTest {

    @Autowired
    private CrdVersionLogic crdVersionLogic;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testWithNoVersionInCluster() {
        execution.setVariable(CHART_VERSION, "1.2.1+2");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "");
        assertThatThrownBy(() -> crdVersionLogic.execute(execution)).isInstanceOf(BpmnError.class).hasMessage("Invalid chartVersion");
    }

    @Test
    public void testOlderChartVersion() {
        execution.setVariable(CHART_VERSION, "0.1.0+2");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+2");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isFalse();
    }

    @Test
    public void testNewerChartVersion() {
        execution.setVariable(CHART_VERSION, "1.2.4+2");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+2");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isTrue();
    }

    @Test
    public void testSameVersion() {
        execution.setVariable(CHART_VERSION, "1.2.3+2");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+2");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isFalse();
    }

    @Test
    public void testSameVersionWithoutBuildNumbers() {
        execution.setVariable(CHART_VERSION, "1.2.3");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isFalse();
    }

    @Test
    public void testSameVersionNewerChartBuildNumber() {
        execution.setVariable(CHART_VERSION, "1.2.3+3");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+2");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isTrue();
    }

    @Test
    public void testSameVersionOlderChartBuildNumber() {
        execution.setVariable(CHART_VERSION, "1.2.3+2");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+3");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isFalse();
    }

    @Test
    public void testSameVersionWithoutChartBuildNumber(){
        execution.setVariable(CHART_VERSION, "1.2.3");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3+4");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isFalse();
    }

    @Test
    public void testSameVersionWithoutClusterBuildNumber(){
        execution.setVariable(CHART_VERSION, "1.2.3+4");
        execution.setVariable(CRD_VERSION_IN_CLUSTER, "1.2.3");
        crdVersionLogic.execute(execution);
        assertThat((Boolean) execution.getVariable(PROCEED_WITH_CRD_INSTALL)).isTrue();
    }

    @Configuration
    static class Config {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
