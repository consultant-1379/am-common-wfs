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

import static com.ericsson.amcommonwfs.util.Utility.serializeCurrentTracingContext;
import static java.util.stream.Collectors.toList;

import static com.ericsson.amcommonwfs.util.Constants.WITH_ERROR;
import static com.ericsson.amcommonwfs.util.Utility.getLoggableVariables;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PARENT_WORKFLOW_SUB_STRING;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TRACING_CONTEXT;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.amcommonwfs.util.Utility;
import com.ericsson.amcommonwfs.util.v3.ControllerUtilities;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.google.common.base.Strings;

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WorkflowInstanceServiceCamundaImpl implements WorkflowInstanceServiceCamunda {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;
    @Autowired
    private Tracing tracing;

    @Override
    public ResourceResponseSuccess startWorkflowInstanceByDefinitionKeyAndVariables(final String definitionKey,
                                                                                    final Map<String, Object> processVariable) {
        LOGGER.info("Definition Key :: {} , process variable :: {}", definitionKey, getLoggableVariables(processVariable));
        if (tracing != null) {
            processVariable.put(TRACING_CONTEXT, serializeCurrentTracingContext(tracing));
        }
        try {
            final ProcessInstanceWithVariables processInstance = (ProcessInstanceWithVariables) runtimeService
                    .startProcessInstanceByKey(definitionKey, processVariable);
            return ControllerUtilities.extractResourceResponse(processInstance);
        } catch (final ProcessEngineException processEngineException) {
            LOGGER.error("Error starting the workflow with definition key :: {} and process variable :: {}, failed "
                                 + WITH_ERROR, definitionKey, getLoggableVariables(processVariable), processEngineException.getMessage());
            throw processEngineException;
        }
    }

    @Override
    public ResourceResponseSuccess startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(final String definitionKey,
                                                                                               final String businessKey,
                                                                                               final Map<String, Object> processVariable) {
        LOGGER.info("Definition Key :: {} , business key {} , process variable :: {}", definitionKey, businessKey,
                    getLoggableVariables(processVariable));
        if (tracing != null) {
            processVariable.put(TRACING_CONTEXT, serializeCurrentTracingContext(tracing));
        }
        try {
            final ProcessInstanceWithVariables processInstance = (ProcessInstanceWithVariables) runtimeService
                    .startProcessInstanceByKey(definitionKey, businessKey, processVariable);
            return ControllerUtilities.extractResourceResponse(processInstance);
        } catch (final ProcessEngineException processEngineException) {
            LOGGER.error("Error starting the workflow with definition key :: {}, business key {} and process variable :: {}, failed "
                                 + WITH_ERROR, definitionKey, businessKey, getLoggableVariables(processVariable),
                         processEngineException.getMessage());
            throw processEngineException;
        }
    }

    @Override
    public WorkflowQueryResponse getWorkflowHistoryByReleaseName(final String releaseName, final String instanceId) {
        LOGGER.info("Getting history for releaseName :: {}", releaseName);
        try {

            final List<HistoricVariableInstance> historicVariableInstanceQueryList = historyService.
                    createHistoricVariableInstanceQuery().variableValueEquals(RELEASE_NAME, releaseName).list();

            List<String> processInstanceIds = historicVariableInstanceQueryList.stream()
                    .filter(c -> c.getProcessDefinitionKey().endsWith(PARENT_WORKFLOW_SUB_STRING))
                    .map(HistoricVariableInstance::getProcessInstanceId).distinct().collect(toList());

            if (Strings.isNullOrEmpty(instanceId)) {
                return getWorkflowQueryResponse(processInstanceIds);
            } else {
                List<String> specificId =
                        processInstanceIds.stream().filter(p -> p.equals(instanceId)).collect(toList());
                return getWorkflowQueryResponse(specificId);
            }
        } catch (final ProcessEngineException processEngineException) {
            LOGGER.error("Error getting workflow instance details with releaseName :: {} " + "failed " + WITH_ERROR,
                         releaseName, processEngineException.getMessage());
            throw processEngineException;
        }
    }

    private WorkflowQueryResponse getWorkflowQueryResponse(final List<String> processInstanceIds) {
        List<ProcessInstance> processInstances = Utility
                .getHistoryProcessInstance(processInstanceIds, historyService);
        WorkFlowQueryMetaData metaData = new WorkFlowQueryMetaData();
        metaData.setCount(processInstances.size());
        WorkflowQueryResponse response = new WorkflowQueryResponse();
        response.setWorkflowQueries(processInstances);
        response.setMetadata(metaData);
        return response;
    }

}
