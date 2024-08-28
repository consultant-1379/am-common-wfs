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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_CODE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;

public class ErrorCodeTranslatorTest {

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void verifyCorrectErrorMessageWhenReleaseAlreadyExists() {
        PostLongRunningCommandExecution postHelmCommandExecution = new PostLongRunningCommandExecution();
        execution.setProcessInstance(new ExecutionImpl());
        execution.setVariable(COMMAND_OUTPUT, releaseAlreadyExists());
        execution.setVariable(ERROR_CODE, "error.com.install.failed");
        execution.setVariable(COMMAND_EXIT_STATUS, 1);
        assertThatThrownBy(() -> postHelmCommandExecution.execute(execution)).isInstanceOf(BpmnError.class);
        assertThat((String) execution.getVariable(ERROR_MESSAGE)).contains("A resource named conflict-test already "
                + "exists. Please use a different name or delete the resource with this name.");
    }

    private String releaseAlreadyExists() {
        return "Hang tight while we grab the latest from your chart repositories...\\n...Skip local chart "
                + "repository\\n...Successfully got an update from the \\\"adp-am\\\" chart repository\\n.."
                + ".Successfully got an update from the \\\"stable\\\" chart repository\\nUpdate Complete.  Happy "
                + "Helming!\\nError: a release named conflict-test already exists.\\nRun: helm ls --all "
                + "conflict-test; to check the status of the release\\nOr run: helm del --purge conflict-test; to "
                + "delete it";
    }

    @Test
    public void verifyCorrectErrorMessageWhenDeletionTimesOut(){
        PostLongRunningCommandExecution postHelmCommandExecution = new PostLongRunningCommandExecution();
        execution.setProcessInstance(new ExecutionImpl());
        execution.setVariable(COMMAND_OUTPUT, "error: no matching resources found");
        execution.setVariable(ERROR_CODE, "error.com.deletion.failure");
        execution.setVariable(COMMAND_EXIT_STATUS, 1);
        assertThatThrownBy(() -> postHelmCommandExecution.execute(execution)).isInstanceOf(BpmnError.class);
        assertThat((String) execution.getVariable(ERROR_MESSAGE)).contains("Deletion of the resource timed out");
    }
}
