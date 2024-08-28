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
package com.ericsson.amcommonwfs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.camunda.bpm.engine.history.HistoricProcessInstance.STATE_ACTIVE;
import static org.camunda.bpm.engine.history.HistoricProcessInstance.STATE_COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.util.Constants.CLUSTER_CONFIG_FILE_EXTENSION;
import static com.ericsson.amcommonwfs.util.Constants.DEFAULT;
import static com.ericsson.amcommonwfs.util.Constants.PROCESS_INSTANCE_NULL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PARENT_WORKFLOW_SUB_STRING;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstanceQuery;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.variable.VariableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.amcommonwfs.exception.InstanceServiceException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowInstanceResource;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowResponseError;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowResponseSuccess;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;

@ExtendWith(SpringExtension.class)
public class UtilityTest {

    private static final String DUMMY_INSTANCE_ID = "dummy_instance_id";

    private static final String DUMMY_DEFINITION_ID = "dummy_definition_id";

    private static final String DUMMY_BUSINESS_KEY = "dummy_business_key";

    private static final String DUMMY_ERROR_MSG = "dummy_error_message";

    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";

    private static final String DUMMY_CLUSTER_CONFIG_DIR = "config_dir";

    private static final String DUMMY_CLUSTER_NAME = "cluster_name";

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private ProcessInstanceWithVariables processInstanceWithVariables;

    @Mock
    private VariableMap variableMap;

    @Mock
    private HistoryService historyService;

    @Mock
    private HistoricVariableInstanceQuery historicVariableInstanceQuery;

    @Mock
    private HistoricVariableInstance historicVariableInstance;

    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @Mock
    private HistoricActivityInstanceQuery historicActivityInstanceQuery;

    private List<ProcessInstance> processInstances;

    @BeforeEach
    public void init() {
        processInstances = new ArrayList<>();
        processInstances.add(processInstance);
    }

    @Test
    public void testExtractWorkflowInstanceResource() {
        when(processInstance.getId()).thenReturn(DUMMY_INSTANCE_ID);
        when(processInstance.getProcessDefinitionId()).thenReturn(DUMMY_DEFINITION_ID);
        when(processInstance.getBusinessKey()).thenReturn(DUMMY_BUSINESS_KEY);
        final WorkflowInstanceResource workflowInstanceResource = Utility.extractWorkflowInstanceResource(processInstance);
        assertThat(workflowInstanceResource.getBusinessKey()).isEqualTo(DUMMY_BUSINESS_KEY);
        assertThat(workflowInstanceResource.getDefinitionId()).isEqualTo(DUMMY_DEFINITION_ID);
        assertThat(workflowInstanceResource.getInstanceId()).isEqualTo(DUMMY_INSTANCE_ID);
    }

    @Test
    public void testExtractWorkflowInstanceResourceWithNullInstance() {
        try {
            Utility.extractWorkflowInstanceResource(null);
            Assertions.fail("Expected an InstanceServiceException to be thrown");
        } catch (final InstanceServiceException instanceServiceException) {
            assertThat(instanceServiceException.getMessage()).isEqualTo(PROCESS_INSTANCE_NULL);
        }
    }

    @Test
    public void testExtractListOfWorkflowInstanceResource() {
        when(processInstance.getId()).thenReturn(DUMMY_INSTANCE_ID);
        when(processInstance.getProcessDefinitionId()).thenReturn(DUMMY_DEFINITION_ID);
        when(processInstance.getBusinessKey()).thenReturn(DUMMY_BUSINESS_KEY);
        final List<WorkflowInstanceResource> allWorkflowInstance = Utility
                .extractListOfWorkflowInstanceResource(processInstances);
        assertThat(allWorkflowInstance.get(0).getBusinessKey()).isEqualTo(DUMMY_BUSINESS_KEY);
        assertThat(allWorkflowInstance.get(0).getDefinitionId()).isEqualTo(DUMMY_DEFINITION_ID);
        assertThat(allWorkflowInstance.get(0).getInstanceId()).isEqualTo(DUMMY_INSTANCE_ID);
    }

    @Test
    public void testExtractListOfWorkflowInstanceResourceWithNullInstance() {
        final List<WorkflowInstanceResource> allWorkflowInstance = Utility.extractListOfWorkflowInstanceResource(null);
        assertThat(allWorkflowInstance.size()).isEqualTo(0);
    }

    @Test
    public void testExtractWorkflowResponseSuccess() {
        when(processInstanceWithVariables.getId()).thenReturn(DUMMY_INSTANCE_ID);
        when(processInstanceWithVariables.getProcessDefinitionId()).thenReturn(DUMMY_DEFINITION_ID);
        when(processInstanceWithVariables.getBusinessKey()).thenReturn(DUMMY_BUSINESS_KEY);
        when(processInstanceWithVariables.getVariables()).thenReturn(variableMap);
        when(processInstanceWithVariables.getVariables().get(RELEASE_NAME)).thenReturn(DUMMY_RELEASE_NAME);
        final WorkflowResponseSuccess workflowResponse = (WorkflowResponseSuccess) Utility
                .extractWorkflowResponse(processInstanceWithVariables);
        assertThat(workflowResponse.getBusinessKey()).isEqualTo(DUMMY_BUSINESS_KEY);
        assertThat(workflowResponse.getDefinitionId()).isEqualTo(DUMMY_DEFINITION_ID);
        assertThat(workflowResponse.getInstanceId()).isEqualTo(DUMMY_INSTANCE_ID);
        assertThat(workflowResponse.getReleaseName()).isEqualTo(DUMMY_RELEASE_NAME);
    }

    @Test
    public void testExtractWorkflowResponseError() {
        when(processInstanceWithVariables.getId()).thenReturn(DUMMY_INSTANCE_ID);
        when(processInstanceWithVariables.getVariables()).thenReturn(variableMap);
        when(processInstanceWithVariables.getVariables().containsKey(ERROR_MESSAGE)).thenReturn(true);
        when(processInstanceWithVariables.getVariables().get(ERROR_MESSAGE)).thenReturn(DUMMY_ERROR_MSG);
        final WorkflowResponseError workflowResponseError = (WorkflowResponseError) Utility
                .extractWorkflowResponse(processInstanceWithVariables);
        assertThat(workflowResponseError.getInstanceId()).isEqualTo(DUMMY_INSTANCE_ID);
        assertThat(workflowResponseError.getErrorMessage()).isEqualTo(DUMMY_ERROR_MSG);
    }

    @Test
    public void testGetLoggableVariablesWithNull() {
        Map<String, Object> dummyMap = null;
        assertThat(Utility.getLoggableVariables(dummyMap)).isNull();
    }

    @Test
    public void testGetLoggableVariablesWithNullConfig() {
        Map<String, Object> dummyMap = new HashMap<>();
        dummyMap.put(DAY0_CONFIGURATION, null);
        assertThat(Utility.getLoggableVariables(dummyMap).get(DAY0_CONFIGURATION)).isNull();
    }

    @Test
    public void testGetLoggableVariablesWithDayConfig() {
        Map<String, Object> dummyMap = Map.of(DAY0_CONFIGURATION, "anyValue");
        assertThat(Utility.getLoggableVariables(dummyMap)).containsValue("*******");
    }

    @Test
    public void testFormatClusterConfigFile() {
        assertThat(Utility.formatClusterConfigFile(DUMMY_CLUSTER_NAME, DUMMY_CLUSTER_CONFIG_DIR))
                .isEqualTo("config_dir/cluster_name" + CLUSTER_CONFIG_FILE_EXTENSION);
    }

    @Test
    public void testFormatClusterConfigFileWithConfig() {
        assertThat(Utility.formatClusterConfigFile(DUMMY_CLUSTER_NAME, DUMMY_CLUSTER_CONFIG_DIR))
                .isEqualTo("config_dir/cluster_name" + CLUSTER_CONFIG_FILE_EXTENSION);
    }

    @Test
    public void testCheckClusterFileExistsWithException() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> {
                    Utility.checkClusterFileExists(DUMMY_CLUSTER_NAME, DUMMY_CLUSTER_CONFIG_DIR);
                });
    }

    @Test
    public void testCheckClusterFileExistsWithNull() {
        assertThat(Utility.checkClusterFileExists(null, "randomDirName")).isNull();
    }

    @Test
    public void testCheckClusterFileExistsWithDefault() {
        assertThat(Utility.checkClusterFileExists(DEFAULT, "randomDirName")).isEqualTo(DEFAULT);
    }

    @Test
    public void testHistoryProcessInstanceIsInProcessingState() {
        // when
        var historicProcessInstance = buildHistoricProcessInstance(STATE_ACTIVE);
        var historicActivityInstance = buildHistoricActivityInstance("TEST_TYPE");

        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.processInstanceId(anyString())).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.list()).thenReturn(List.of(historicVariableInstance));
        when(historicVariableInstance.getName()).thenReturn("TEST_NAME");
        when(historicVariableInstance.getValue()).thenReturn("TEST_VALUE");

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(anyString())).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.list()).thenReturn(List.of(historicProcessInstance));

        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.processInstanceId(anyString())).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.list()).thenReturn(List.of(historicActivityInstance));

        // then
        var historyProcessInstance = Utility.getHistoryProcessInstance(List.of(DUMMY_INSTANCE_ID), historyService);

        // assert
        assertNotNull(historyProcessInstance);
        var resultProcessInstance = historyProcessInstance.get(0);

        assertEquals(WorkflowState.PROCESSING, resultProcessInstance.getWorkflowState());
        assertEquals(historicProcessInstance.getStartTime(), resultProcessInstance.getStartTime());
        assertEquals(historicProcessInstance.getProcessDefinitionKey(), resultProcessInstance.getDefinitionKey());
    }

    @Test
    public void testHistoryProcessInstanceIsInFailedState() {
        // when
        var historicProcessInstance = buildHistoricProcessInstance(STATE_ACTIVE);
        var historicActivityInstance = buildHistoricActivityInstance("errorEndEvent");

        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.processInstanceId(anyString())).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.list()).thenReturn(List.of(historicVariableInstance));
        when(historicVariableInstance.getName()).thenReturn("TEST_NAME");
        when(historicVariableInstance.getValue()).thenReturn("TEST_VALUE");

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(anyString())).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.list()).thenReturn(List.of(historicProcessInstance));

        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.processInstanceId(anyString())).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.list()).thenReturn(List.of(historicActivityInstance));

        // then
        var historyProcessInstance = Utility.getHistoryProcessInstance(List.of(DUMMY_INSTANCE_ID), historyService);

        // assert
        assertNotNull(historyProcessInstance);
        var resultProcessInstance = historyProcessInstance.get(0);

        assertEquals(WorkflowState.FAILED, resultProcessInstance.getWorkflowState());
        assertEquals(historicProcessInstance.getStartTime(), resultProcessInstance.getStartTime());
        assertEquals(historicProcessInstance.getProcessDefinitionKey(), resultProcessInstance.getDefinitionKey());
    }

    @Test
    public void testHistoryProcessInstanceIsInCompletedState() {
        // when
        var historicProcessInstance = buildHistoricProcessInstance(STATE_COMPLETED);
        var historicActivityInstance = buildHistoricActivityInstance("TEST_TYPE");

        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.processInstanceId(anyString())).thenReturn(historicVariableInstanceQuery);
        when(historicVariableInstanceQuery.list()).thenReturn(List.of(historicVariableInstance));
        when(historicVariableInstance.getName()).thenReturn("TEST_NAME");
        when(historicVariableInstance.getValue()).thenReturn("TEST_VALUE");

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(anyString())).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.list()).thenReturn(List.of(historicProcessInstance));

        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.processInstanceId(anyString())).thenReturn(historicActivityInstanceQuery);
        when(historicActivityInstanceQuery.list()).thenReturn(List.of(historicActivityInstance));

        // then
        var historyProcessInstance = Utility.getHistoryProcessInstance(List.of(DUMMY_INSTANCE_ID), historyService);

        // assert
        assertNotNull(historyProcessInstance);
        var resultProcessInstance = historyProcessInstance.get(0);

        assertEquals(WorkflowState.COMPLETED, resultProcessInstance.getWorkflowState());
        assertEquals(historicProcessInstance.getStartTime(), resultProcessInstance.getStartTime());
        assertEquals(historicProcessInstance.getProcessDefinitionKey(), resultProcessInstance.getDefinitionKey());
    }

    @Test
    public void testIsValidJsonStringWithValidJson() {
        assertTrue(Utility.isValidJsonString("{\"test\":\"test\"}"));
    }

    @Test
    public void testIsValidJsonStringWithInvalidJson() {
        assertFalse(Utility.isValidJsonString("{test}"));
    }

    @Test
    public void testGetErrorDetailsWithMessage() {
        String error = "Unauthorized";
        String errorJson = String.format("{\"message\":\"%s\"}", error);
        assertEquals(error, Utility.getErrorDetails(errorJson));
    }

    @Test
    public void testGetErrorDetailsWithoutMessage() {
        String error = "Unauthorized";
        String errorJson = String.format("{\"test\":\"%s\"}", error);
        assertEquals(Utility.UNEXPECTED_EXCEPTION_OCCURRED, Utility.getErrorDetails(errorJson));
    }

    @Test
    public void testSanitizeErrorWithMessage() {
        String error = "Unauthorized";
        assertEquals(error, Utility.sanitizeError(error, null));
    }

    @Test
    public void testSanitizeErrorWithApiExceptionMessage() {
        String error = "Message: Something went wrong \nHTTP response code: 0 \nHTTP response body: HTTP response headers:\"";
        assertEquals("Something went wrong", Utility.sanitizeError(error, null));
    }

    @Test
    public void testSanitizeErrorWithApiExceptionMessageInResponseBody() {
        String error = "Message: \n"
                + "    HTTP response code: 403\n"
                + "    HTTP response body: {\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"namespaces is forbidden: User \\\"system:anonymous\\\" cannot list resource \\\"namespaces\\\" in API group \\\"\\\" at the cluster scope\",\"reason\":\"Forbidden\",\"details\":{\"kind\":\"namespaces\"},\"code\":403}\n"
                + "    \n"
                + "    HTTP response headers: {audit-id=[5e227036-ac4c-4c54-967e-73e0c4de7dd3], cache-control=[no-cache, private], content-length=[271], content-type=[application/json], date=[Thu, 14 Mar 2024 10:18:19 GMT], x-content-type-options=[nosniff], x-kubernetes-pf-flowschema-uid=[476ade6a-a622-4c53-b688-facff275e375], x-kubernetes-pf-prioritylevel-uid=[6c69fc76-d85c-47c0-8a26-77eeb9857d98]}";
        assertThat(Utility.sanitizeError(error, null)).startsWith("namespaces is forbidden:");
    }

    @Test
    public void testSanitizeErrorWithMessageInResponseBody() {
        String error = "Unauthorized";
        String errorJson = String.format("{\"message\":\"%s\"}", error);
        assertEquals(error, Utility.sanitizeError(null, errorJson));
    }

    @Test
    public void testSanitizeErrorWithoutMessage() {
        String error = "Unauthorized";
        String errorJson = String.format("{\"test\":\"%s\"}", error);
        assertEquals(Utility.UNEXPECTED_EXCEPTION_OCCURRED, Utility.sanitizeError(null, errorJson));
    }

    private static HistoricProcessInstance buildHistoricProcessInstance(String state) {
        var historicProcessInstance = new HistoricProcessInstanceEntity();
        historicProcessInstance.setProcessDefinitionKey(PARENT_WORKFLOW_SUB_STRING);
        historicProcessInstance.setStartTime(Date.from(Instant.parse("2022-11-30T00:00:00.000Z")));
        historicProcessInstance.setState(state);

        return historicProcessInstance;
    }

    private static HistoricActivityInstance buildHistoricActivityInstance(String activityType) {
        var historicActivityInstance = new HistoricActivityInstanceEntity();
        historicActivityInstance.setActivityType(activityType);

        return historicActivityInstance;
    }
}
