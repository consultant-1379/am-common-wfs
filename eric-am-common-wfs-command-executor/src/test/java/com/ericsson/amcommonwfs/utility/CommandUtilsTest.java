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
package com.ericsson.amcommonwfs.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

public class CommandUtilsTest {

    @Test
    public void testConstructFullCommandLinux() {
        assumeTrue(SystemUtils.IS_OS_LINUX);

        assertThat(CommandUtils.constructFullCommand("cat"))
                .containsExactly("bash", "-c", "cat");
    }

    @Test
    public void testHideSensitiveDataList() {
        assertThat(CommandUtils.hideSensitiveData(List.of("password=paSSword",
                                                          "--from-literal=serviceSecret=servicePassword",
                                                          "--set 'day0.configuration.secret.value'=someSecret")))
                .containsExactly("password=*******",
                                 "--from-literal=serviceSecret=*******",
                                 "--set 'day0.configuration.secret.value'=*******");
    }

    @Test
    public void testHideSensitiveData() {
        assertThat(CommandUtils.hideSensitiveData(
                "password=paSSword --from-literal=serviceSecret=servicePassword"))
                .isEqualTo("password=******* --from-literal=serviceSecret=*******");
    }

    @Test
    public void testVerifyCommandNoCommand() {
        // given
        final var delegate = mock(DelegateExecution.class);
        when(delegate.getVariable(eq(Constants.COMMAND))).thenReturn(null);

        // when and then
        assertThatThrownBy(() -> CommandUtils.verifyCommand(delegate))
                .isInstanceOf(BpmnError.class)
                .hasMessage("Command not provided");
    }

    @Test
    public void testVerifyCommandEmptyCommand() {
        // given
        final var execution = mock(DelegateExecution.class);
        when(execution.getVariable(eq(Constants.COMMAND))).thenReturn(new StringBuilder());

        // when and then
        assertThatThrownBy(() -> CommandUtils.verifyCommand(execution))
                .isInstanceOf(BpmnError.class)
                .hasMessage("Command not provided");
    }

    @Test
    public void testVerifyCommandPresent() {
        // given
        final var execution = mock(DelegateExecution.class);
        when(execution.getVariable(eq(Constants.COMMAND))).thenReturn(new StringBuilder("cat"));

        // when and then
        assertThat(CommandUtils.verifyCommand(execution)).isEqualTo("cat");
    }

    @Test
    public void testCheckCommandExitStatusZero() {
        // given
        final var execution = mock(DelegateExecution.class);
        when(execution.getVariable(eq(Constants.COMMAND_EXIT_STATUS))).thenReturn(0);

        // when and then
        assertThatNoException().isThrownBy(
                () -> CommandUtils.checkCommandExitStatus(execution, ErrorCode.BPMN_APPLICATION_CONTAINERS_TIMEOUT_EXCEPTION));
    }

    @Test
    public void testCheckCommandExitStatusNonZero() {
        // given
        final var execution = mock(DelegateExecution.class);
        when(execution.getVariable(eq(Constants.COMMAND_EXIT_STATUS))).thenReturn(1);
        when(execution.getVariable(eq(Constants.COMMAND_OUTPUT))).thenReturn(new StringBuilder("Some error"));

        // when and then
        assertThatThrownBy(() -> CommandUtils.checkCommandExitStatus(execution, ErrorCode.BPMN_APPLICATION_CONTAINERS_TIMEOUT_EXCEPTION))
                .asInstanceOf(InstanceOfAssertFactories.throwable(BpmnError.class))
                .hasMessage("Some error")
                .extracting(BpmnError::getErrorCode)
                .isEqualTo(ErrorCode.BPMN_APPLICATION_CONTAINERS_TIMEOUT_EXCEPTION.getErrorCodeAsString());
    }
}