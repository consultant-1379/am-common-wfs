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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.CONDITION_TIMED_OUT_MESSAGE;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.NO_MATCHING_RESOURCES_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_DEPLOYED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VerifyDeployedTask.class })
public class VerifyDeployedTaskTest {
    private static final int COMMAND_EXIT_VALUE_ZERO = 0;
    private static final Object COMMAND_EXIT_VALUE_ONE = 1;

    private final ExecutionImpl execution = new ExecutionImpl();

    @Autowired
    private VerifyDeployedTask verifyDeployedTask;

    @Test
    public void caseExecuteStatusZeroAndOutputConditionTimedOutTest() {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(CONDITION_TIMED_OUT_MESSAGE));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ZERO);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> verifyDeployedTask.execute(execution));
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isFalse();
    }

    @Test
    public void caseExecuteStatusZeroAndOutputNoMatchingResourceTest() throws Exception {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(NO_MATCHING_RESOURCES_MESSAGE));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ZERO);

        verifyDeployedTask.execute(execution);
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isTrue();
    }

    @Test
    public void caseExecuteStatusZeroAndOutputAnyMessageTest() throws Exception {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder("any other message"));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ZERO);

        verifyDeployedTask.execute(execution);
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isTrue();
    }

    @Test
    public void caseExecuteStatusUnitAndOutputConditionTimedOutTest() {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(CONDITION_TIMED_OUT_MESSAGE));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ONE);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> verifyDeployedTask.execute(execution));
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isFalse();
    }

    @Test
    public void caseExecuteStatusUnitAndOutputNoMatchingResourceTest() throws Exception {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(NO_MATCHING_RESOURCES_MESSAGE));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ONE);

        verifyDeployedTask.execute(execution);
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isTrue();
    }

    @Test
    public void caseExecuteStatusUnitAndOutputAnyMessageTest() throws Exception {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder("any other message"));
        execution.setVariable(COMMAND_EXIT_STATUS, COMMAND_EXIT_VALUE_ONE);

        verifyDeployedTask.execute(execution);
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isFalse();
    }

    @Test
    public void caseExecuteStatusAnythingButZeroOrUnitTest() {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(NO_MATCHING_RESOURCES_MESSAGE));
        execution.setVariable(COMMAND_EXIT_STATUS, 3);

        assertThatNoException().isThrownBy(() -> verifyDeployedTask.execute(execution));
        assertThat((Boolean) execution.getVariable(APP_DEPLOYED)).isFalse();
    }
}