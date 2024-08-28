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

import static com.ericsson.amcommonwfs.utils.CommonUtils.convertToJSONString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;

import java.io.IOException;
import java.util.Optional;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for Camunda service task java delegate executing external commands.
 * Now there are two implementations, one using shell (bash or powershell depending on host OS) and other
 * executing commands directly. This class offers a template implementation {@link #execute(DelegateExecution)
 * method along with overridable methods performing actual work on building command and handling execution result.
 */
@Slf4j
public abstract class ExecuteCommandBase implements JavaDelegate {
    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        execution.setVariableLocal(COMMAND_OUTPUT, new StringBuilder());
        execution.setVariable(COMMAND_EXIT_STATUS, 1);
        try {
            Optional<ProcessExecutorResponse> processExecutorResponseOptional = prepareAndExecuteCommand(execution);
            if (processExecutorResponseOptional.isEmpty()) {
                return;
            }
            ProcessExecutorResponse processExecutorResponse = processExecutorResponseOptional.get();
            execution.setVariable(COMMAND_OUTPUT, new StringBuilder(processExecutorResponse.getCmdResult()));
            execution.setVariable(COMMAND_EXIT_STATUS, processExecutorResponse.getExitValue());
        } catch (final IOException ioe) {
            LOGGER.error("IO Exception occurred :: ", ioe);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_IO_EXCEPTION, ioe.getMessage(), execution);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted exception occurred :: ", ie);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_INTERRUPTED_EXCEPTION, ie.getMessage(), execution);
        } catch (final CommandTimedOutException cte) {
            LOGGER.error("Command timed out :: ", cte);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_COMMAND_TIMEOUT_EXCEPTION,
                                                          convertToJSONString(cte.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY.toString()),
                                                          execution);
        } catch (final Exception ex) {
            LOGGER.error("Unknown exception occurred :: ", ex);
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_UNKNOWN_EXCEPTION, ex.getMessage(), execution);
        }
        processCommandResult(execution);
    }

    /**
     * Implementations expected to prepare and run external process according to context represented by {@code execution} parameter.
     * In some cases process execution may be skipped and an empty {@link Optional} should be returned. Otherwise
     * returned {@link ProcessExecutorResponse} should contain exit code and process output.
     *
     * @param execution workflow service task context.
     * @return
     * @throws IOException              during an attempt to start external process
     * @throws InterruptedException     when waiting for a process completion
     * @throws CommandTimedOutException if command didn't complete before timeout reached
     */
    protected abstract Optional<ProcessExecutorResponse> prepareAndExecuteCommand(DelegateExecution execution) throws IOException,
            InterruptedException, CommandTimedOutException;

    /**
     * Implementations expected to complete processing of execution results.
     * It may fail a task when command completed unsuccessfully (though it is not a requirement) and/or process command output.
     * Default implementation does nothing.
     *
     * @param execution workflow service task context.
     */
    protected void processCommandResult(final DelegateExecution execution) {
    }
}
