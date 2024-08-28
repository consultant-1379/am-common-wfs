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
package com.ericsson.amcommonwfs.history;

import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_HISTORY_RESPONSE;
import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_REVISION_RESPONSE;
import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_REVISION_STATUS;
import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_UNINSTALLING_REVISION_STATUS;
import static com.ericsson.amcommonwfs.constants.CommandConstants.REVISION_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_VERSION_IN_CLUSTER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DESCRIPTION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LAST_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TERMINATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VERSION_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.component.VerifyExecution;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ParseHistoryOutput implements JavaDelegate {

    private static final Pattern REGEX_HISTORY = Pattern.compile("\\[[^\\]]*\\]");

    @Autowired
    private VerifyExecution verifyExecution;

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        final String historyOutput = ((StringBuilder) execution.getVariableLocal(COMMAND_OUTPUT)).toString();
        final String revisionNumber = (String) execution.getVariableLocal(REVISION_NUMBER);

        LOGGER.info("History command output is {}", historyOutput);
        Integer cmdExitStatus = (Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS);
        if (cmdExitStatus == 0) {
            parseHistoryResponse(execution, historyOutput, revisionNumber);
        } else {
            LOGGER.error("History for specified release not found");
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_HISTORY_FAILURE, historyOutput, execution);
        }
    }

    private void parseHistoryResponse(DelegateExecution execution, String historyOutput, String revisionNumber) {
        try {
            List<HistoryObject> historyObjects = getHistoryResponseList(historyOutput);
            getRevisionStatus(historyObjects, revisionNumber, execution);
        } catch (BpmnError bpmnError) { // NOSONAR
            LOGGER.error("Unable to parse history command output ");
            BusinessProcessExceptionUtils.handleException(bpmnError.getErrorCode(), bpmnError.getMessage(), execution);
        } catch (Exception e) { // NOSONAR
            LOGGER.error("Unable to parse history command output ", e);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_HISTORY_FAILURE, INVALID_HISTORY_RESPONSE, execution);
        }
    }

    private static List<HistoryObject> getHistoryResponseList(final String historyOutput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String extractedVersion = "";
        Matcher matcher = REGEX_HISTORY.matcher(historyOutput);
        if (matcher.find()) {
            extractedVersion = historyOutput.substring(matcher.start()).trim();
            LOGGER.info("History Command has been formatted: {}", extractedVersion);
        }
        return mapper.readValue(extractedVersion, new TypeReference<>() {
        });
    }

    private void getRevisionStatus(List<HistoryObject> historyObjects, String revisionNumber,
                                   DelegateExecution execution) {
        String lastHistory = (String) execution.getVariable(LAST_HISTORY);
        Optional<HistoryObject> foundHistoryObject;
        if (Strings.isNullOrEmpty(lastHistory)) {
            foundHistoryObject = isFoundRevision(historyObjects, revisionNumber);
        } else {
            foundHistoryObject = isFoundRevision(historyObjects);
        }
        if (foundHistoryObject.isPresent()) {
            setRevisionVariables(foundHistoryObject.get(), execution, lastHistory);
        } else {
            LOGGER.error("Revision not found");
            BusinessProcessExceptionUtils
                    .handleException(ErrorCode.BPMN_INVALID_RESPONSE_EXCEPTION, INVALID_REVISION_RESPONSE, execution);
        }
    }

    private static Optional<HistoryObject> isFoundRevision(List<HistoryObject> historyObjects, String revisionNumber) {
        return historyObjects.stream().filter(c -> c.getRevision().equals(revisionNumber)).findFirst();
    }

    private static Optional<HistoryObject> isFoundRevision(List<HistoryObject> historyObjects) {
        return historyObjects.stream().findFirst();
    }

    private void setRevisionVariables(HistoryObject historyObject, DelegateExecution execution,
                                      final String lastHistory) {
        String status = historyObject.getStatus().toUpperCase(); // NOSONAR
        String chartVersion = historyObject.getChart();
        String description = historyObject.getDescription();
        String revision = historyObject.getRevision();
        String crdVersionInCluster = getVersionFromChart(chartVersion);

        checkReleaseStatus(execution, status, revision, lastHistory);

        if (Strings.isNullOrEmpty(lastHistory)) {
            LOGGER.info("Status of revision {} is {} with following chart {}", revision, status, chartVersion);
        }
        execution.setVariable(REVISION_NUMBER, revision);
        execution.setVariable(REVISION_STATUS, status);
        if (ROLLBACK_DEFINITION_KEY.equalsIgnoreCase(getDefinitionKey(execution))) {
            execution.setVariable(CHART_VERSION, chartVersion);
        }
        execution.setVariable(CRD_VERSION_IN_CLUSTER, crdVersionInCluster);
        execution.setVariable(REVISION, revision);
        execution.setVariable(DESCRIPTION, description);
    }

    private static String getDefinitionKey(final DelegateExecution execution) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution.getProcessInstance();
        return executionEntity.getProcessEngineServices().getRepositoryService()
                .getProcessDefinition(executionEntity.getProcessDefinitionId()).getKey();
    }

    private static String getVersionFromChart(final String chart) {
        Matcher matcher = VERSION_REGEX.matcher(chart);
        String extractedVersion = "";
        if (matcher.find()) {
            extractedVersion = chart.substring(matcher.start()).trim();
            LOGGER.info("Extracted version {} from {}", extractedVersion, chart);
        }
        return extractedVersion;
    }

    void checkReleaseStatus(DelegateExecution execution, String status, String revision, String lastHistory) {
        if (!("SUPERSEDED".equals(status) || ("DEPLOYED".equals(status))) && Strings.isNullOrEmpty(lastHistory)) {
            LOGGER.error("Invalid revision {} status \"{}\" found, failed to process release history", revision, status);
            BusinessProcessExceptionUtils
                    .handleException(ErrorCode.BPMN_INVALID_RESPONSE_EXCEPTION, INVALID_REVISION_STATUS, execution);
        }

        if (TERMINATE_DEFINITION_KEY.equals(verifyExecution.getDefinitionKey(execution)) && ("UNINSTALLING".equals(status))) {
            LOGGER.error("Revision \"{}\" is already in uninstalling status, failed to process release history", revision);
            BusinessProcessExceptionUtils
                    .handleException(ErrorCode.BPMN_INVALID_RESPONSE_EXCEPTION, INVALID_UNINSTALLING_REVISION_STATUS, execution);
        }
    }
}


