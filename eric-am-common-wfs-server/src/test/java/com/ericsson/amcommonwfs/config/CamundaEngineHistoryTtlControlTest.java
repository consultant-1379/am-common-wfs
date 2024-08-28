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
package com.ericsson.amcommonwfs.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.inject.Inject;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "camunda.historyTimeToLive=5"
})
@AutoConfigureObservability
class CamundaEngineHistoryTtlControlTest {

    @Inject
    private ProcessEngine processEngine;

    @Test
    void testHistoryTtlControlUpdate() {
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .list();

        assertThat(processDefinitions).isNotEmpty().allMatch(procDef -> procDef.getHistoryTimeToLive() == 5);
    }
}
