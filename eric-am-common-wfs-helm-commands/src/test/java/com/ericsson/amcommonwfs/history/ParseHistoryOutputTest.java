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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_REVISION_RESPONSE;
import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_REVISION_STATUS;
import static com.ericsson.amcommonwfs.constants.CommandConstants.INVALID_UNINSTALLING_REVISION_STATUS;
import static com.ericsson.amcommonwfs.constants.CommandConstants.REVISION_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_VERSION_IN_CLUSTER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LAST_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TERMINATE_DEFINITION_KEY;

import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.amcommonwfs.component.VerifyExecution;

@ExtendWith(MockitoExtension.class)
public class ParseHistoryOutputTest {
    @InjectMocks
    @Spy
    private ParseHistoryOutput parseHistoryOutput;

    @Mock
    private DelegateExecution execution;

    @Mock
    private ExecutionEntity executionEntity;

    @Mock
    private ProcessEngineServices processEngineServices;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ProcessDefinition processDefinition;

    @Mock
    private VerifyExecution verifyExecution;

    private static final String HISTORY =
            "[{\"revision\":1,\"updated\":\"Tue Nov 27 13:17:31 2018\",\"status\":\"SUPERSEDED\","
                    + "\"chart\":\"elasticsearch-exporter-0.4.0\",\"description\":\"Install complete\"},"
                    + "{\"revision\":2," + "\"updated\":\"Tue Nov 27 13:19:03 2018\",\"status\":\"SUPERSEDED\","
                    + "\"chart\":\"elasticsearch-exporter-0.4.1\",\"description\":\"Upgrade complete\"},"
                    + "{\"revision\":3," + "\"updated\":\"Tue Nov 27 13:19:44 2018\",\"status\":\"FAILED\","
                    + "\"chart\":\"elasticsearch-exporter-0.4.0\",\"description\":\"Rollback failed\"},"
                    + "{\"revision\":4," + "\"updated\":\"Tue Nov 27 13:20:44 2018\",\"status\":\"DEPLOYED\","
                    + "\"chart\":\"elasticsearch-exporter-0.4.0\",\"description\":\"Rollback to 1\"}]";

    private static final String UNINSTALLING_HISTORY =
            "[{\"revision\":4,\"updated\":\"Tue Nov 27 13:20:44 2018\",\"status\":\"UNINSTALLING\","
                    + "\"chart\":\"elasticsearch-exporter-0.4.0\",\"description\":\"Deletion in progress (or silently failed)\"}]";

    private static final String HISTORY_ALPHANUMERIC_CHART_WITH_BUILD_NUMBER =
            "[{\"revision\":1,\"updated\":\"Tue Nov 27 13:17:31 2018\",\"status\":\"SUPERSEDED\","
                    + "\"chart\":\"elasticsearch-exporter-123-0.4.0*3\",\"description\":\"Install complete\"},"
                    + "{\"revision\":2," + "\"updated\":\"Tue Nov 27 13:19:03 2018\",\"status\":\"SUPERSEDED\","
                    + "\"chart\":\"elasticsearch-exporter-123-0.4.1*3\",\"description\":\"Upgrade complete\"}]";

    private static final String RELEASE_NOT_FOUND = "Error: release \" non-existent\" not found";

    @Test
    public void parseSuccessfulHistory() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(0);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(HISTORY));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("2");
        when(execution.getProcessInstance()).thenReturn(executionEntity);
        when(executionEntity.getProcessDefinitionId()).thenReturn(ROLLBACK_DEFINITION_KEY);
        when(executionEntity.getProcessEngineServices()).thenReturn(processEngineServices);
        when(executionEntity.getProcessEngineServices().getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.getProcessDefinition(any())).thenReturn(processDefinition);
        when(processDefinition.getKey()).thenReturn(ROLLBACK_DEFINITION_KEY);

        parseHistoryOutput.execute(execution);
        verify(execution, times(1)).setVariable(REVISION_NUMBER, "2");
        verify(execution, times(1)).setVariable(REVISION_STATUS, "SUPERSEDED");
        verify(execution, times(1)).setVariable(CRD_VERSION_IN_CLUSTER, "0.4.1");
    }

    @Test
    public void parseHistoryWithNonExistentRevision() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(0);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(HISTORY));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("22");
        assertThatThrownBy(() -> parseHistoryOutput.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessage(INVALID_REVISION_RESPONSE);
    }

    @Test
    public void parseHistoryWithIncorrectExitStatus() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(1);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(RELEASE_NOT_FOUND));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("1");
        assertThatThrownBy(() -> parseHistoryOutput.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessage(RELEASE_NOT_FOUND);
    }

    @Test
    public void parseHistoryWithIncorrectRevisionStatus() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(0);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(HISTORY));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("3");
        assertThatThrownBy(() -> parseHistoryOutput.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessage(INVALID_REVISION_STATUS);
        verify(parseHistoryOutput).checkReleaseStatus(execution, "FAILED", "3", null);
    }

    @Test
    public void parseHistoryWithUninstallingRevisionStatusWhenTerminate() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(0);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(UNINSTALLING_HISTORY));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("4");
        when(execution.getVariable(LAST_HISTORY)).thenReturn("true");
        when(verifyExecution.getDefinitionKey(execution)).thenReturn(TERMINATE_DEFINITION_KEY);
        assertThatThrownBy(() -> parseHistoryOutput.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessage(INVALID_UNINSTALLING_REVISION_STATUS);
        verify(parseHistoryOutput).checkReleaseStatus(execution, "UNINSTALLING", "4", "true");
    }

    @Test
    public void testExtractVersionFromAlphaNumericChartWithBuildNumber() {
        when((Integer) execution.getVariableLocal(COMMAND_EXIT_STATUS)).thenReturn(0);
        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(HISTORY_ALPHANUMERIC_CHART_WITH_BUILD_NUMBER.replace("*",
                                                                                                                                           "-")));
        when(execution.getVariableLocal(REVISION_NUMBER)).thenReturn("2");
        when(execution.getProcessInstance()).thenReturn(executionEntity);
        when(executionEntity.getProcessEngineServices()).thenReturn(processEngineServices);
        when(executionEntity.getProcessEngineServices().getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.getProcessDefinition(any())).thenReturn(processDefinition);

        parseHistoryOutput.execute(execution);
        verify(execution, times(1)).setVariable(CRD_VERSION_IN_CLUSTER, "0.4.1-3");

        when(execution.getVariableLocal(COMMAND_OUTPUT)).thenReturn(new StringBuilder(HISTORY_ALPHANUMERIC_CHART_WITH_BUILD_NUMBER.replace("*",
                                                                                                                                           "+")));
        parseHistoryOutput.execute(execution);
        verify(execution, times(1)).setVariable(CRD_VERSION_IN_CLUSTER, "0.4.1+3");
    }
}
