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
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE;

import java.io.File;
import java.io.IOException;
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

import com.ericsson.amcommonwfs.presentation.services.messaging.MessagingHealth;
import com.ericsson.amcommonwfs.presentation.services.messaging.MessagingService;
import com.ericsson.amcommonwfs.registry.secret.CheckSecretExist;
import com.ericsson.amcommonwfs.services.crypto.DevCryptoService;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;

@DirtiesContext
@ActiveProfiles("dev")
@SpringBootTest
public class InstantiateBpmnTest extends AbstractBpmnTest {

    private static final String NAMESPACE = "default";

    private static final String AUX_SECRET_NAME = "auxiliarySecret";

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private RuntimeService runtimeService;

    @MockBean
    private MessagingHealth messagingHealth;

    @Autowired
    private CheckSecretExist checkSecretExist;
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
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.put(ADDITIONAL_PARAMS, "invalidObjectType");
        ProcessInstance processInstance = getProcessInstance(variables, "Instantiate");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("AdditionalParams need to be in MAP format. Please provide valid input");
        assertBpmnOrderValidationFailed(processInstance);
    }

    @Test
    public void testNamespaceNotProvided() throws InterruptedException {
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.remove(Constants.NAMESPACE);
        ProcessInstance processInstance = getProcessInstance(variables, "Instantiate");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("Required properties are missing/null - provide valid input for the following: namespace");
        assertBpmnOrderValidationFailed(processInstance);
    }

    @Test
    public void testNonExistentClusterConfigFile() throws InterruptedException {
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.put(ORIGINAL_CLUSTER_NAME, "nonExistent");
        variables.remove(CLUSTER_CONFIG_CONTENT_KEY);
        ProcessInstance processInstance = getProcessInstance(variables, "Instantiate");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertThat(errorMessage).matches("cluster config not present, please add the " +
                                                 "config file using 'add cluster config rest api' and then use this parameter");
        assertBpmnOrderValidationFailed(processInstance);
    }

    @Test
    public void testAuxSecretRemovedOnInstallationFail() {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(ERROR_MESSAGE, "Helm/kubectl command has timed out")
                .startAfterActivity("BoundaryEvent_InstantiateApplicationFailed")
                .execute();

        assertAuxSecretRemovedOnFail(processInstance);
    }

    @Test
    public void testAuxSecretRemovedOnVerificationFail() {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(ERROR_MESSAGE, "The lifecycle operation on the resource timed out. It may complete in the background "
                        + "on the cluster. You can clean up the resource on the UI")
                .startAfterActivity("BoundaryEvent_VerifyApplicationInstalled")
                .execute();

        assertAuxSecretRemovedOnFail(processInstance);
    }

    @Test
    public void testAuxSecretCreatedForValidSecretParams() {
        mockCreateAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .startAfterActivity("Activity_Check_Image_PullSecret")
                .execute();

        assertAuxSecretCreated(processInstance);
    }

    @Test
    public void testAuxSecretRemovedOnSuccess() {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .startAfterActivity("Activity_VerifyApplicationInstalled")
                .execute();

        assertAuxSecretRemovedAfterApplicationInstalled(processInstance);
    }

    @Test
    public void testTempFilesRemovedOnSuccess() throws IOException {

        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .startAfterActivity("Activity_Set_Revision_Desc")
                .execute();

        assertFalse(fileExistInTmp(CLUSTER_CONFIG));
        assertFalse(fileExistInTmp(VALUES_YAML));
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveTemporaryFilesOnSuccess");
    }

    @Test
    public void testTaskNotFailWhenNoTempFilesExist() throws IOException{
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        String tempClusterConfig = pathToTempDirectory + File.separator + CLUSTER_CONFIG;
        String tempValuesFile = pathToTempDirectory + File.separator + VALUES_YAML;

        assertFalse(fileExistInTmp(tempClusterConfig));
        assertFalse(fileExistInTmp(tempValuesFile));

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(CLUSTER_NAME, tempClusterConfig)
                .setVariable(VALUES_FILE, tempValuesFile)
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .startAfterActivity("Activity_Set_Revision_Desc")
                .execute();

        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveTemporaryFilesOnSuccess");
    }

    @Test
    public void testTaskNotFailWhenOnlyConfigFileExists() throws IOException {
        mockDeleteAuxiliarySecretCommand(AUX_SECRET_NAME, NAMESPACE);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("InstantiateApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .setVariable(DAY0_CONFIGURATION, getAdditionalParamsWithSecret())
                .startAfterActivity("Activity_Set_Revision_Desc")
                .execute();

        assertFalse(fileExistInTmp(CLUSTER_CONFIG));
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveTemporaryFilesOnSuccess");
    }

    private void assertBpmnOrderValidationFailed(final ProcessInstance processInstance) {
        BpmnAwareTests
                .assertThat(processInstance).isEnded()
                .hasPassedInOrder("StartEvent_1", "Task_Instantiate_Validate", "BoundaryEvent_InstantiateValidateFailed",
                                  "Task_RemoveTemporaryFilesOnValidationFailure",
                                  "Script_Log_Validation_Failures", "Instantiate_Validation_Error_Event")
                .hasNotPassed("Activity_Publish_Lifecycle_Message_Error_End");
    }

    private void assertAuxSecretRemovedOnFail(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance)
                .isEnded()
                .hasPassedInOrder("Task_RemoveAuxiliarySecretOnFail",
                                  "Task_LogInstantiateFailed",
                                  "EndEvent_Instantiate_Error");
    }

    private void assertWorkflowFailedAndNoAuxSecretCreated(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance)
                .isEnded()
                .hasPassedInOrder("Task_CreateAuxiliarySecret",
                                  "Task_LogInstantiateFailed",
                                  "EndEvent_Instantiate_Error");
    }

    private void assertAuxSecretCreated(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance).hasPassedInOrder("Task_CreateAuxiliarySecret");
    }

    private void assertAuxSecretRemovedAfterApplicationInstalled(final ProcessInstance processInstance) {
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveAuxiliarySecretOnSuccess");
    }
}
