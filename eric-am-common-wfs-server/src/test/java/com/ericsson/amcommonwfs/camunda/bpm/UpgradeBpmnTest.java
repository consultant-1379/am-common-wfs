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
package com.ericsson.amcommonwfs.camunda.bpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.TestConstants.CLUSTER_CONFIG;
import static com.ericsson.amcommonwfs.TestConstants.VALUES_YAML;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getCommonVariablesMap;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getProcessInstance;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.waitUntilNoActiveJobs;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amcommonwfs.services.crypto.DevCryptoService;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;

@ActiveProfiles("dev")
@DirtiesContext
@SpringBootTest
public class UpgradeBpmnTest extends AbstractBpmnTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private DevCryptoService cryptoService;

    @MockBean
    private CamundaFileRepository camundaFileRepository;

    @MockBean
    private KubeClientBuilder kubeClientBuilder;

    @BeforeEach
    public void setup() {
        init(processEngine);
        when(camundaFileRepository.get(eq("dummy-config-content-key")))
                .thenReturn(cryptoService.encryptString("Dummy cluster content").getBytes());
    }

    @Test
    public void testInvalidAdditionalParams() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("additionalParams", "test");
        variables.put("releaseName", "my-release");
        variables.put("chartName", "my-chart");
        ProcessInstance processInstance = getProcessInstance(variables, "Upgrade");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("AdditionalParams need to be in MAP format. Please provide valid input");
        assertBpmOrderValidationPath(processInstance);
    }

    @Test
    public void testNonExistentClusterConfigFile() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put(RELEASE_NAME, "my-release");
        variables.put(CHART_NAME, "my-chart");
        variables.put(ORIGINAL_CLUSTER_NAME, "nonExistent");
        ProcessInstance processInstance = getProcessInstance(variables, "Upgrade");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("cluster config not present, please add the " +
                                                 "config file using 'add cluster config rest api' and then use this parameter");
        assertBpmOrderValidationPath(processInstance);
    }

    @Test
    public void testTempFilesRemoved() throws IOException {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("UpgradeApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .startAfterActivity("CallActivity_086sw0b")
                .execute();

        assertFalse(fileExistInTmp(CLUSTER_CONFIG));
        assertFalse(fileExistInTmp(VALUES_YAML));
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveTemporaryFilesOnSuccess");
    }

    private void assertBpmOrderValidationPath(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance).isEnded().hasNotPassed("Task_CheckReleaseLabelScale")
                .hasPassedInOrder("StartEvent_Upgrade", "Task_Upgrade_ValidateInput", "BoundaryEvent_Upgrade_ValidateInput",
                                  "Task_RemoveTemporaryFilesOnValidationFailure",
                                  "Script_Log_Failures", "Upgrade_Validation_Error_Event")
                .hasNotPassed("Activity_Publish_Lifecycle_Message_Error_End");
    }
}
