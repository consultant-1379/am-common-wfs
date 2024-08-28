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
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getAdditionalParamsWithSecret;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getCommonVariablesMap;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getProcessInstance;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.waitUntilNoActiveJobs;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
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
public class TerminateBpmnTest extends AbstractBpmnTest {

    private static final String NAMESPACE = "default";

    private static final String AUX_SECRET_NAME = "auxiliarySecret";

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

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
    public void testInvalidReleaseName() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("releaseName", "My-Release");
        ProcessInstance processInstance = getProcessInstance(variables, "Terminate");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches(
                "releaseName must consist of lower case alphanumeric characters or - It must start with an alphabetic character, and end with an "
                        + "alphanumeric character");
        assertBpmnOrderPublishErrorMessage(processInstance);
    }

    @Test
    public void testNonExistentClusterConfigFile() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put(RELEASE_NAME, "my-release");
        variables.put(ORIGINAL_CLUSTER_NAME, "nonExistent");
        ProcessInstance processInstance = getProcessInstance(variables, "Terminate");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("cluster config not present, please add the " +
                                                 "config file using 'add cluster config rest api' and then use this parameter");
        assertBpmnOrderPublishErrorMessage(processInstance);
    }

    @Test
    public void testAuxSecretRemovedAfterVerifyTermination() {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("TerminateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .startAfterActivity("Activity_Verify_Termination")
                .execute();

        BpmnAwareTests.assertThat(processInstance)
                .isEnded()
                .hasPassedInOrder("Activity_RemoveAuxiliarySecretOnTermination",
                                  "Task_RemoveRegistrySecret",
                                  "Task_SetWorkflowStatus",
                                  "Activity_Publish_Lifecycle_Message_End",
                                  "EndEvent_TerminateSuccessful");
    }

    @Test
    public void testBoundaryEventForAuxSecretRemove() {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("TerminateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(ERROR_MESSAGE, "Helm/kubectl command has timed out")
                .startAfterActivity("BoundaryEvent_RemoveAuxiliarySecretOnTermination")
                .execute();

        BpmnAwareTests.assertThat(processInstance)
                .isEnded()
                .hasPassedInOrder("Task_RemoveTemporaryFilesOnFailure", "Task_LogTerminationFailure",
                                  "Activity_Publish_Lifecycle_Message_Error_End",
                                  "EndEvent_Terminate_Error");
    }

    @Test
    public void testTempFilesRemovedAfterAuxSecretRemove() throws Exception {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        mockDeleteSecretCommand();
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("TerminateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .startAfterActivity("Activity_RemoveAuxiliarySecretOnTermination")
                .execute();

        assertFalse(fileExistInTmp(CLUSTER_CONFIG));
        assertFalse(fileExistInTmp(VALUES_YAML));
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveRegistrySecret", "Task_RemoveTemporaryFilesOnSuccess", "Task_SetWorkflowStatus", "Activity_Publish_Lifecycle_Message_End", "EndEvent_TerminateSuccessful");
    }

    private void assertBpmnOrderPublishErrorMessage(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance).isEnded()
                .hasPassedInOrder("StartEvent_1", "Task_Terminate_Validate", "BoundaryEvent_Terminate_ValidateInput",
                                  "Task_RemoveTemporaryFilesOnValidationFailure",
                                  "Script_Log_Validation_Failures", "Terminate_Validation_Error_Event")
                .hasNotPassed("Activity_Publish_Lifecycle_Message_Error_End");
    }
}
