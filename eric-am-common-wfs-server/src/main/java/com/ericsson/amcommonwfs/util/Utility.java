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

import static com.ericsson.amcommonwfs.util.Constants.CLUSTER_CONFIG_FILE_EXTENSION;
import static com.ericsson.amcommonwfs.util.Constants.DEFAULT;
import static com.ericsson.amcommonwfs.util.Constants.DIRECTORY_PATH_SEPARATOR;
import static com.ericsson.amcommonwfs.util.Constants.PROCESS_INSTANCE_NULL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PARENT_WORKFLOW_SUB_STRING;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ericsson.amcommonwfs.exception.InstanceServiceException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowInstanceResource;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowResponse;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowResponseError;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowResponseSuccess;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;
import com.google.common.base.Strings;

import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utility {

    public static final String UNEXPECTED_EXCEPTION_OCCURRED = "Unexpected Exception occurred";
    private static final String ERROR_END_EVENT_TYPE = "errorEndEvent";
    private static final String PROCESS_INSTANCE_DETAILS = "Process Instance details :: {}";
    private static final String ERROR_MESSAGE_FIELD = "message";

    public static List<WorkflowInstanceResource> extractListOfWorkflowInstanceResource(
            final List<org.camunda.bpm.engine.runtime.ProcessInstance> processInstances) {
        LOGGER.debug(PROCESS_INSTANCE_DETAILS, processInstances);
        List<WorkflowInstanceResource> workflowInstanceResources = new ArrayList<>();
        if (processInstances != null && !processInstances.isEmpty()) {
            for (org.camunda.bpm.engine.runtime.ProcessInstance processInstance : processInstances) {
                workflowInstanceResources.add(extractWorkflowInstanceResource(processInstance));
            }
        }
        LOGGER.debug("Workflow Instance created with details :: {}", workflowInstanceResources);
        return workflowInstanceResources;
    }

    public static WorkflowInstanceResource extractWorkflowInstanceResource(
            final org.camunda.bpm.engine.runtime.ProcessInstance processInstance) {
        LOGGER.debug(PROCESS_INSTANCE_DETAILS, processInstance);
        if (processInstance != null) {
            final WorkflowInstanceResource workflowInstanceResource = new WorkflowInstanceResource();
            if (processInstance.getId() != null) {
                workflowInstanceResource.setInstanceId(processInstance.getId());
            }
            if (processInstance.getBusinessKey() != null) {
                workflowInstanceResource.setBusinessKey(processInstance.getBusinessKey());
            }
            if (processInstance.getProcessDefinitionId() != null) {
                workflowInstanceResource.setDefinitionId(processInstance.getProcessDefinitionId());
            }
            LOGGER.debug("Workflow Instance created with details :: {}", workflowInstanceResource);
            return workflowInstanceResource;
        } else {
            throw new InstanceServiceException(PROCESS_INSTANCE_NULL);
        }
    }

    public static WorkflowResponse extractWorkflowResponse(
            final ProcessInstanceWithVariables processInstanceWithVariables) {
        LOGGER.debug(PROCESS_INSTANCE_DETAILS, processInstanceWithVariables);
        WorkflowResponse workflowResponse;
        if (processInstanceWithVariables != null) {
            if (processInstanceWithVariables.getVariables() != null && processInstanceWithVariables.getVariables()
                    .containsKey(ERROR_MESSAGE)) {
                workflowResponse = new WorkflowResponseError();
                ((WorkflowResponseError) workflowResponse)
                        .setErrorMessage((String) processInstanceWithVariables.getVariables().get(ERROR_MESSAGE));
            } else {
                workflowResponse = new WorkflowResponseSuccess();
                ((WorkflowResponseSuccess) workflowResponse)
                        .setBusinessKey(processInstanceWithVariables.getBusinessKey());
                ((WorkflowResponseSuccess) workflowResponse)
                        .setDefinitionId(processInstanceWithVariables.getProcessDefinitionId());
            }

            workflowResponse.setInstanceId(processInstanceWithVariables.getId());
            workflowResponse.setReleaseName((String) processInstanceWithVariables.getVariables().get(RELEASE_NAME));
            return workflowResponse;
        } else {
            throw new InstanceServiceException(PROCESS_INSTANCE_NULL);
        }
    }

    public static List<ProcessInstance> getHistoryProcessInstance(final List<String> processInstanceIds,
                                                                  final HistoryService historyService) {

        return processInstanceIds.stream()
                .map(instanceId -> getHistoricVariableInstanceQuery(historyService, instanceId))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProcessInstance::getStartTime).reversed()).collect(Collectors.toList());
    }

    public static Map<String, String> serializeCurrentTracingContext(Tracing tracing) {
        Span span = tracing.tracer().currentSpan();
        Map<String, String> tracingContextSerialized = new HashMap<>();
        TraceContext.Injector<Map<String, String>> injector = tracing.propagation().injector(Map<String, String>::put);
        injector.inject(span.context(), tracingContextSerialized);
        return tracingContextSerialized;
    }

    private static ProcessInstance getHistoricVariableInstanceQuery(final HistoryService historyService,
                                                                    final String instanceId) {

        List<HistoricVariableInstance> historicVariableInstanceList = historyService
                .createHistoricVariableInstanceQuery().processInstanceId(instanceId).list();

        if (CollectionUtils.isEmpty(historicVariableInstanceList)) {
            return null;
        }
        Set<String> typeSet = new HashSet<>();

        Map<String, Object> variableMap = historicVariableInstanceList.stream().filter(c -> typeSet.add(c.getName()))
                .filter(i -> i.getValue() != null)
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));

        ProcessInstance processInstance = new ResourceProcessInstance(variableMap, instanceId);

        List<HistoricProcessInstance> historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instanceId).list();
        Optional<HistoricProcessInstance> historicProcessInstance = historicProcessInstanceQuery.stream()
                .filter(c -> c.getProcessDefinitionKey().endsWith(PARENT_WORKFLOW_SUB_STRING)).findFirst();
        if (historicProcessInstance.isPresent()) {
            HistoricProcessInstance instance = historicProcessInstance.get();
            setWorkflowState(instance, processInstance);
            processInstance.setStartTime(instance.getStartTime());
            processInstance.setDefinitionKey(instance.getProcessDefinitionKey());
            if (processInstance instanceof WorkflowProcessInstance) {
                ((WorkflowProcessInstance) processInstance).setBusinessKey(instance.getBusinessKey());
            }
        }
        final HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.
                createHistoricActivityInstanceQuery().processInstanceId(instanceId);
        setWorkflowState(historicActivityInstanceQuery, processInstance);

        return processInstance;
    }

    private static void setWorkflowState(final HistoricProcessInstance instance,
                                         final ProcessInstance workflowQueryResponse) {
        if (instance.getState().equals(HistoricProcessInstance.STATE_ACTIVE)) {
            workflowQueryResponse.setWorkflowState(WorkflowState.PROCESSING);
        } else if (instance.getState().equals(HistoricProcessInstance.STATE_COMPLETED)) {
            workflowQueryResponse.setWorkflowState(WorkflowState.COMPLETED);
        } else {
            workflowQueryResponse.setWorkflowState(WorkflowState.FAILED);
        }
    }

    private static void setWorkflowState(HistoricActivityInstanceQuery historicActivityInstanceQuery,
                                         final ProcessInstance instance) {
        List<HistoricActivityInstance> allActivityInstance = historicActivityInstanceQuery.list();
        Optional<HistoricActivityInstance> first = allActivityInstance.stream()
                .filter(c -> !Strings.isNullOrEmpty(c.getActivityType()) && c.getActivityType()
                        .equals(ERROR_END_EVENT_TYPE)).findFirst();
        if (first.isPresent()) {
            instance.setWorkflowState(WorkflowState.FAILED);
        }
    }

    public static String checkClusterFileExists(final String clusterName, final String clusterConfigDir) {
        if (clusterName == null || DEFAULT.equalsIgnoreCase(clusterName)) {
            return clusterName;
        }
        String clusterConfigFile = formatClusterConfigFile(clusterName, clusterConfigDir);
        if (!Files.exists(Paths.get(clusterConfigFile))) {
            String clusterNotFoundMessage = String.format("cluster config not present, Cluster config file %s not found",
                                                          clusterConfigFile);
            throw new NotFoundException(clusterNotFoundMessage);
        }
        return clusterConfigFile;
    }

    public static String formatClusterConfigFile(final String clusterName, final String clusterConfigDir) {
        StringBuilder clusterConfig = new StringBuilder(clusterConfigDir).append(DIRECTORY_PATH_SEPARATOR)
                .append(clusterName);
        if (!clusterName.contains(CLUSTER_CONFIG_FILE_EXTENSION)) {
            clusterConfig.append(CLUSTER_CONFIG_FILE_EXTENSION);
        }
        return clusterConfig.toString();
    }

    public static void deleteClusterConfigFile(final Path clusterConfigFile) {
        try {
            Files.delete(clusterConfigFile);
        } catch (IOException exception) {
            LOGGER.error(String.format("Failed to delete %s", clusterConfigFile), exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getLoggableVariables(Map<String, Object> processVariables) {
        if (processVariables == null || processVariables.get(DAY0_CONFIGURATION) == null) {
            return processVariables;
        }
        Map<String, Object> loggableVariables = new LinkedHashMap<>(processVariables);
        loggableVariables.put(DAY0_CONFIGURATION, "*******");
        return loggableVariables;
    }

    public static boolean isValidJsonString(String jsonString) {
        try {
            new JSONObject(jsonString);
            return true;
        } catch (Exception e) {
            LOGGER.debug("An error occurred during JSON validation", e);
            return false;
        }
    }

    public static String getErrorDetails(final String message) {
        final var errorJson = new JSONObject(message);

        return errorJson.isNull(ERROR_MESSAGE_FIELD)
                ? UNEXPECTED_EXCEPTION_OCCURRED
                : String.valueOf(errorJson.get(ERROR_MESSAGE_FIELD));
    }

    public static String sanitizeError(String message, String responseBody) {
        if (!Strings.isNullOrEmpty(message)) {
            return formatApiExceptionMessage(message);
        }

        if (isValidJsonString(responseBody)) {
            return getErrorDetails(responseBody);
        }

        return !Strings.isNullOrEmpty(message) ? responseBody : UNEXPECTED_EXCEPTION_OCCURRED;
    }

    public static HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private static String formatApiExceptionMessage(String message) {
        Pattern messagePattern = Pattern.compile("Message: (.+?)\\sHTTP response code:");
        Matcher messageMatcher = messagePattern.matcher(message);

        Pattern responseBodyPattern = Pattern.compile("HTTP response body: (.*)HTTP response headers:", Pattern.DOTALL);
        Matcher responseBodyMatcher = responseBodyPattern.matcher(message);

        if (messageMatcher.find()) {
            return messageMatcher.group(1).trim();
        } else if (responseBodyMatcher.find()) {
            final String responseBody = responseBodyMatcher.group(1).trim();
            if (isValidJsonString(responseBody)) {
                return getErrorDetails(responseBody);
            } else {
                return message;
            }
        } else {
            return message;
        }
    }
}
