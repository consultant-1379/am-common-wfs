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

import com.ericsson.amcommonwfs.services.crypto.DevCryptoService;
import com.ericsson.amcommonwfs.util.DefinitionKey;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;

import io.kubernetes.client.openapi.ApiException;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import static com.ericsson.amcommonwfs.TestConstants.CLUSTER_CONFIG;
import static com.ericsson.amcommonwfs.TestConstants.VALUES_YAML;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getCommonVariablesMap;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.getProcessInstance;
import static com.ericsson.amcommonwfs.camunda.bpm.BpmnTestUtilities.waitUntilNoActiveJobs;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.processEngine;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles("dev")
@DirtiesContext
@SpringBootTest
public class ScaleBpmnTest extends AbstractBpmnTest {

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
    public void testInvalidReleaseName() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("releaseName", "my-Release");
        ProcessInstance processInstance = getProcessInstance(variables, "Scale");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertEquals(
                "releaseName must consist of lower case alphanumeric characters or - It must start with an alphabetic"
                        + " character, and end with an alphanumeric character", errorMessage);
        assertBpmOrderValidationPath(processInstance);
    }

    @Test
    public void testMissingChartNameOrUrl() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("releaseName", "my-release");
        variables.put("namespace", "default");
        ProcessInstance processInstance = getProcessInstance(variables, "Scale");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        assertEquals("Required properties are missing/null - provide valid input for the following: chartName or "
                             + "chartUrl ", errorMessage);
        assertThat(processInstance).isEnded();
        assertBpmOrderValidationPath(processInstance);
    }

    @Test
    public void testPathCheckLabelFailureWithUpgradeFailureResponseAcceptedHelmTimeout()
    throws InterruptedException, IOException, ApiException {
        mockCheckForReleaseNameWithReleaseLabelNotPresent();
        mockJobService();
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.put(APPLICATION_TIME_OUT, "30");
        ProcessInstance processInstance = getProcessInstance(variables, "Scale");
        waitUntilNoActiveJobs(processEngine, 60000);
        sleep(5000);
        String errorMessage = getErrorMessage(processInstance);
        assertTrue(errorMessage.contains("Helm/kubectl command has timed out"));
        assertBpmOrderPath(processInstance);
    }

    @Disabled("Need to re-visit the testing logic here")
    @Test
    public void testPathCheckLabelFailureWithVerifyFailure()
    throws Exception {
        mockCheckForReleaseName();
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.put("waitTime", "P0DT0H0M2S");
        variables.put("commandTimeOut", "1");
        variables.put("isAnnotated", false);
        mockJobService();
        ProcessInstance processInstance = processEngine().getRuntimeService()
                .createProcessInstanceByKey(DefinitionKey.getProcessDefinitionKey("Scale"))
                .startAfterActivity("Task_ScaleApplication").setVariables(variables).execute();

        waitUntilNoActiveJobs(processEngine, 30000);
        sleep(5000);
        String errorMessage = getErrorMessage(processInstance);
        assertTrue(errorMessage.contains("Helm/kubectl command has timed out"));
        assertBpmOrderVerifyPath(processInstance);
    }

    @Test
    @Disabled("Need to re-visit the testing logic here")
    public void testCorrectVerifyPathUsed()
    throws Exception {
        mockCheckForReleaseName();
        HashMap<String, Object> variables = getCommonVariablesMap();
        variables.put("waitTime", "P0DT0H0M2S");
        variables.put("isAnnotated", true);
        mockJobService();
        mockCheckForHistory("");
        ProcessInstance processInstance = processEngine().getRuntimeService()
                .createProcessInstanceByKey(DefinitionKey.getProcessDefinitionKey("Scale"))
                .startAfterActivity("Task_ScaleApplication").setVariables(variables).execute();

        waitUntilNoActiveJobs(processEngine, 30000);
        sleep(5000);
        assertBpmOrderNewVerifyPath(processInstance);
    }

    @Test
    public void testNonExistentClusterConfigFile() throws InterruptedException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put(RELEASE_NAME, "my-release");
        variables.put(CHART_NAME, "my-chart");
        variables.put(ORIGINAL_CLUSTER_NAME, "nonExistent");
        ProcessInstance processInstance = getProcessInstance(variables, "Scale");
        waitUntilNoActiveJobs(processEngine, 1000);
        String errorMessage = getErrorMessage(processInstance);
        Assertions.assertThat(errorMessage).matches("cluster config not present, please add the " +
                                                            "config file using 'add cluster config rest api' and then use this parameter");
        assertBpmOrderValidationPath(processInstance);
    }

    @Test
    public void testTempFilesRemoved() throws IOException {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("ScaleApplication__top")
                .setVariables(getCommonVariablesMap())
                .setVariable(APP_TIMEOUT, toEpochSecond)
                .startAfterActivity("CallActivity_SetRevisionScale")
                .execute();

        assertFalse(fileExistInTmp(CLUSTER_CONFIG));
        assertFalse(fileExistInTmp(VALUES_YAML));
        BpmnAwareTests.assertThat(processInstance).hasPassed("Task_RemoveTemporaryFilesOnSuccess");
    }

    private void assertBpmOrderPath(final ProcessInstance processInstance) {
        assertThat(processInstance).isEnded().hasNotPassed("Task_GetNameSpaceScale")
                .hasPassedInOrder("StartScaleEvent_1",
                                  "task_ValidateScaleInput",
                                  "task_CalculateDelay",
                                  "Task_CheckReleaseLabelScale",
                                  "BoundaryEvent_CheckReleaseLabelScale",
                                  "Task_ScaleApplication",
                                  "BoundaryEvent_ScaleApplication",
                                  "Task_RemoveTemporaryFilesOnFailure",
                                  "Task_LogScaleValue",
                                  "Activity_Publish_Lifecycle_Message_Error_End",
                                  "EndEvent_ErrorEventScale");
    }

    private void assertBpmOrderVerifyPath(final ProcessInstance processInstance) {
        assertThat(processInstance).isEnded().hasNotPassed("CallActivity_SetRevisionScale")
                .hasPassedInOrder("CallActivity_VerifyScale",
                                  "BoundaryEvent_VerifyScale",
                                  "Task_RemoveTemporaryFilesOnFailure",
                                  "Task_LogScaleValue",
                                  "Activity_Publish_Lifecycle_Message_Error_End",
                                  "EndEvent_ErrorEventScale");
    }

    private void assertBpmOrderNewVerifyPath(final ProcessInstance processInstance) {
        assertThat(processInstance).isEnded()
                .hasPassedInOrder("CallActivity_VerifyScale",
                                  "Task_RemoveTemporaryFilesOnSuccess",
                                  "CallActivity_SetRevisionScale",
                                  "Task_WorkflowStatusScale",
                                  "Activity_Publish_Lifecycle_Message_End",
                                  "EndEvent_Scale");
    }

    private void assertBpmOrderValidationPath(final ProcessInstance processInstance) {
        assertThat(processInstance).isEnded().hasNotPassed("Task_CheckReleaseLabelScale")
                .hasPassedInOrder("StartScaleEvent_1", "task_ValidateScaleInput", "BoundaryEvent_validateScaleInput",
                                  "Task_RemoveTemporaryFilesOnValidationFailure",
                                  "Task_Log_Validation_Failures", "Scale_Validation_Error_Event")
                .hasNotPassed("Activity_Publish_Lifecycle_Message_Error_End");
    }

    private void sleep(long wait) {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

