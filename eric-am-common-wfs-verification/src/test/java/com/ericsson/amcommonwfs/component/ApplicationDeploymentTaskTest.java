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
package com.ericsson.amcommonwfs.component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_DEPLOYED_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_ROLLBACK_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_SCALE_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_UPGRADED_SUCCESS;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFY_CMD_EXEC_RESULT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ApplicationDeploymentTask.class })
public class ApplicationDeploymentTaskTest {

    @Autowired
    private ApplicationDeploymentTask applicationDeploymentTask;

    private DelegateExecution execution;
    private String appName = "Test";

    @MockBean
    private VerifyExecution verifyExecution;

    @BeforeEach
    public void init() {
        execution = mock(DelegateExecution.class);
    }

    @Test
    public void verifyExecuteWithInstantiateDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn(INSTANTIATE_DEFINITION_KEY);
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);
        applicationDeploymentTask.execute(execution);
        verify(execution, times(1)).setVariable(VERIFY_CMD_EXEC_RESULT, APP_DEPLOYED_SUCCESS + appName);
    }

    @Test
    public void verifyExecuteWithUpgradeDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn(UPGRADE_DEFINITION_KEY);
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);
        applicationDeploymentTask.execute(execution);
        verify(execution, times(1)).setVariable(VERIFY_CMD_EXEC_RESULT, APP_UPGRADED_SUCCESS + appName);
    }

    @Test
    public void verifyExecuteWithRollbackDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn(ROLLBACK_DEFINITION_KEY);
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);
        applicationDeploymentTask.execute(execution);
        verify(execution, times(1)).setVariable(VERIFY_CMD_EXEC_RESULT, APP_ROLLBACK_SUCCESS + appName);
    }

    @Test
    public void verifyExecuteWithScaleDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn(SCALE_DEFINITION_KEY);
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);

        applicationDeploymentTask.execute(execution);

        verify(execution, times(1)).setVariable(VERIFY_CMD_EXEC_RESULT, APP_SCALE_SUCCESS + appName);
    }

    @Test
    public void verifyExecuteWithCrdDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn(CRD_DEFINITION_KEY);
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);

        applicationDeploymentTask.execute(execution);

        verify(execution, times(1)).setVariable(VERIFY_CMD_EXEC_RESULT, APP_DEPLOYED_SUCCESS + appName);
    }

    @Test
    public void verifyExecuteWithRandomDefinitionKey() {
        when(verifyExecution.getDefinitionKey(any())).thenReturn("dummy_def_key");
        when(execution.getVariable(RELEASE_NAME)).thenReturn(appName);

        assertThatThrownBy(() -> applicationDeploymentTask.execute(execution)).isInstanceOf(BpmnError.class);
    }
}
