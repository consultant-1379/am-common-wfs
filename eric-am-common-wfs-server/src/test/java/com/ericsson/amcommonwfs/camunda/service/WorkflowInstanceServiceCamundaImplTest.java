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
package com.ericsson.amcommonwfs.camunda.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.impl.HistoricVariableInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessInstanceWithVariablesImpl;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.util.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class WorkflowInstanceServiceCamundaImplTest {

    private static final String DEFAULT_NAMESPACE="default";
    private static final String DEFAULT_CHART_NAME="my-chartName";
    private static final String DEFAULT_RELEASE_NAME="my-releaseName";
    private static final String DEFAULT_INSTANCE_ID="instanceId1";
    private static final String DEFAULT_EXECUTION_ID="executionId";

    @InjectMocks
    private WorkflowInstanceServiceCamundaImpl workflowInstanceServiceCamunda;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private HistoryService historyService;

    @Mock
    private HistoricVariableInstanceQueryImpl historicVariableInstanceQuery;

    @Test
    public void startWorkflowInstanceByDefinitionKeyAndVariables() {

        HashMap<String, Object> variables = getCommonVariablesMap();


        Mockito.when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
                .thenReturn(getProcessInstanceWithVariables(variables, DEFAULT_EXECUTION_ID));

        ResourceResponseSuccess response = workflowInstanceServiceCamunda
                .startWorkflowInstanceByDefinitionKeyAndVariables(INSTANTIATE_DEFINITION_KEY, variables);

        Assert.assertEquals(DEFAULT_EXECUTION_ID, response.getInstanceId());
        Assert.assertEquals(DEFAULT_RELEASE_NAME, response.getReleaseName());
    }

    @Test
    public void startWorkflowInstanceByDefinitionKeyAndVariablesThrowsProcessEngineException() {

        assertThrows(ProcessEngineException.class, () -> {
            HashMap<String, Object> variables = getCommonVariablesMap();

            Mockito.when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
                    .thenThrow(new ProcessEngineException("processEngineException"));

            workflowInstanceServiceCamunda
                    .startWorkflowInstanceByDefinitionKeyAndVariables(INSTANTIATE_DEFINITION_KEY, variables);
        });
    }

    @Test
    public void getWorkflowHistoryByReleaseName() {
        try(MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {

            List<HistoricVariableInstance> historicVariableInstanceList = createHistoricInstanceList();
            List<HistoricVariableInstance> historicVariableInstancesByInstance = historicVariableInstanceList.stream()
                            .filter(historicVariableInstance -> DEFAULT_INSTANCE_ID.equals(historicVariableInstance.getProcessInstanceId()))
                            .collect(Collectors.toList());
            Mockito.when(historyService.createHistoricVariableInstanceQuery()).thenReturn(historicVariableInstanceQuery);
            Mockito.when(historicVariableInstanceQuery.variableValueEquals(any(), any())).thenReturn(historicVariableInstanceQuery);
            Mockito.when(historicVariableInstanceQuery.list()).thenReturn(historicVariableInstanceList);
            Mockito.when(Utility.getHistoryProcessInstance(any(), any())).thenReturn(createProcessList(historicVariableInstancesByInstance));

            WorkflowQueryResponse response= workflowInstanceServiceCamunda
                    .getWorkflowHistoryByReleaseName(DEFAULT_RELEASE_NAME, DEFAULT_INSTANCE_ID);

            Assert.assertEquals(3, response.getWorkflowQueries().size());
            Assert.assertEquals(3, response.getMetadata().getCount());
        }
    }

    @Test
    public void getWorkflowHistoryByReleaseNameWithEmptyInstanceId() {
       try(MockedStatic<Utility> utilityMock = Mockito.mockStatic(Utility.class)) {

           List<HistoricVariableInstance> historicVariableInstanceList = createHistoricInstanceList();
           Mockito.when(historyService.createHistoricVariableInstanceQuery()).thenReturn(historicVariableInstanceQuery);
           Mockito.when(historicVariableInstanceQuery.variableValueEquals(any(), any())).thenReturn(historicVariableInstanceQuery);
           Mockito.when(historicVariableInstanceQuery.list()).thenReturn(historicVariableInstanceList);
           Mockito.when(Utility.getHistoryProcessInstance(any(), any())).thenReturn(createProcessList(historicVariableInstanceList));

           WorkflowQueryResponse response= workflowInstanceServiceCamunda
                   .getWorkflowHistoryByReleaseName(DEFAULT_RELEASE_NAME, null);

           Assert.assertEquals(5, response.getWorkflowQueries().size());
           Assert.assertEquals(5, response.getMetadata().getCount());
       }
    }

    private List<ProcessInstance> createProcessList(List<HistoricVariableInstance> historicVariableInstanceList) {
        List<ProcessInstance> processInstances = new ArrayList<>();
        historicVariableInstanceList.forEach(historicVariableInstance -> {
                ProcessInstance processInstance = createProcessInstance(historicVariableInstance.getProcessDefinitionKey(),
                                                                        historicVariableInstance.getProcessInstanceId());
                processInstances.add(processInstance);
                                             });
        return processInstances;
    }

    private ProcessInstanceWithVariables getProcessInstanceWithVariables(Map<String, Object> variablesMap, String executionId) {
        ExecutionEntity execution = new ExecutionEntity();
        execution.setId(executionId);

        return new ProcessInstanceWithVariablesImpl(execution, Variables.fromMap(variablesMap));
    }

    private HashMap<String, Object> getCommonVariablesMap() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put(RELEASE_NAME, DEFAULT_RELEASE_NAME);
        variables.put(NAMESPACE, DEFAULT_NAMESPACE);
        variables.put(CHART_NAME, DEFAULT_CHART_NAME);
        return variables;
    }

    private ProcessInstance createProcessInstance(String definitionKey, String instanceId) {
        ResourceProcessInstance processInstance = new ResourceProcessInstance();
        processInstance.setInstanceId(instanceId);
        processInstance.setDefinitionKey(definitionKey);
        processInstance.setReleaseName(DEFAULT_RELEASE_NAME);
        return processInstance;
    }

    private List<HistoricVariableInstance> createHistoricInstanceList() {
        List<HistoricVariableInstance> variableInstanceList = new ArrayList<>();
        variableInstanceList.add(createHistoricInstance(INSTANTIATE_DEFINITION_KEY, DEFAULT_INSTANCE_ID));
        variableInstanceList.add(createHistoricInstance(UPGRADE_DEFINITION_KEY, DEFAULT_INSTANCE_ID));
        variableInstanceList.add(createHistoricInstance(ROLLBACK_DEFINITION_KEY, DEFAULT_INSTANCE_ID));
        variableInstanceList.add(createHistoricInstance(INSTANTIATE_DEFINITION_KEY, "instanceId2"));
        variableInstanceList.add(createHistoricInstance(INSTANTIATE_DEFINITION_KEY, "instanceId3"));
        return variableInstanceList;
    }

    private HistoricVariableInstance createHistoricInstance(String definitionKey, String processInstanceId){
        HistoricVariableInstanceEntity instanceEntity = new HistoricVariableInstanceEntity();
        instanceEntity.setProcessDefinitionKey(definitionKey);
        instanceEntity.setProcessInstanceId(processInstanceId);
        return instanceEntity;
    }


 }