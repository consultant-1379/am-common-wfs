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
package com.ericsson.amcommonwfs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExecuteCommandTest {

    @Mock
    private ProcessExecutor processExecutor;

    @InjectMocks
    private ExecuteCommand executeCommand;

    @Mock
    private DelegateExecution execution;

    @Test
    public void testExecuteSuccess() throws CommandTimedOutException, IOException, InterruptedException {
        // given
        when(execution.getVariable(eq(Constants.COMMAND))).thenReturn(new StringBuilder("cat"));
        when(execution.getVariable(eq(Constants.APP_TIMEOUT))).thenReturn(Instant.now().getEpochSecond());

        final var response = new ProcessExecutorResponse();
        response.setCmdResult("Success");
        response.setExitValue(0);
        when(processExecutor.executeProcess(anyString(), anyInt(), anyBoolean())).thenReturn(response);

        // when
        executeCommand.execute(execution);

        // then
        verify(processExecutor).executeProcess(eq("cat"), eq(0), eq(false));

        verify(execution).setVariable(eq(Constants.COMMAND_TIME_TAKEN), lt(1000L));
        verify(execution).setVariable(eq(Constants.COMMAND_OUTPUT), argThat(stringBuilder -> Objects.equals(stringBuilder.toString(), "Success")));
        verify(execution).setVariable(eq(Constants.COMMAND_EXIT_STATUS), eq(0));
    }

    @Test
    public void testExecuteWhenIOExceptionOccurs() throws CommandTimedOutException, IOException, InterruptedException {
        testExecuteWhenExceptionOccurs(new IOException("IO exception"), ErrorCode.BPMN_IO_EXCEPTION);
    }

    @Test
    public void testExecuteWhenInterruptedExceptionOccurs() throws CommandTimedOutException, IOException, InterruptedException {
        testExecuteWhenExceptionOccurs(new InterruptedException("Interrupted exception"), ErrorCode.BPMN_INTERRUPTED_EXCEPTION);
    }

    @Test
    public void testExecuteWhenCommandTimedOutExceptionOccurs() throws CommandTimedOutException, IOException, InterruptedException {
        testExecuteWhenExceptionOccurs(
                new CommandTimedOutException("Command timed out exception"),
                "{\"detail\":\"Command timed out exception\",\"status\":\"422 UNPROCESSABLE_ENTITY\"}",
                ErrorCode.BPMN_COMMAND_TIMEOUT_EXCEPTION);
    }

    @Test
    public void testExecuteWhenRuntimeExceptionOccurs() throws CommandTimedOutException, IOException, InterruptedException {
        testExecuteWhenExceptionOccurs(new RuntimeException("Runtime exception"), ErrorCode.BPMN_UNKNOWN_EXCEPTION);
    }

    private void testExecuteWhenExceptionOccurs(final Throwable exception, final ErrorCode error)
    throws IOException, InterruptedException, CommandTimedOutException {
        testExecuteWhenExceptionOccurs(exception, exception.getMessage(), error);
    }

    private void testExecuteWhenExceptionOccurs(final Throwable exception, String message, final ErrorCode error)
    throws IOException, InterruptedException, CommandTimedOutException {
        // given
        when(execution.getVariable(eq(Constants.COMMAND))).thenReturn(new StringBuilder("cat"));
        when(execution.getVariable(eq(Constants.APP_TIMEOUT))).thenReturn(Instant.now().getEpochSecond());

        when(processExecutor.executeProcess(anyString(), anyInt(), anyBoolean())).thenThrow(exception);

        // when and then
        assertThatThrownBy(() -> executeCommand.execute(execution))
                .asInstanceOf(InstanceOfAssertFactories.throwable(BpmnError.class))
                .hasMessage(message)
                .extracting(BpmnError::getErrorCode)
                .isEqualTo(error.getErrorCodeAsString());

        // then
        verify(execution, never()).setVariable(eq(Constants.COMMAND_OUTPUT), anyString());
        verify(execution).setVariable(eq(Constants.COMMAND_EXIT_STATUS), eq(1));
    }
}
