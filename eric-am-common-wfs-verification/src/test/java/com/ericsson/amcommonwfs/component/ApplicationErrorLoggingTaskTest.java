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

import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.IS_APPLICATION_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ApplicationErrorLoggingTask.class})
public class ApplicationErrorLoggingTaskTest {
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_ERROR_MESSAGE = "dummy_error_message";
    private static final String DUMMY_COMMAND_OUTPUT = "dummy_command_output";


    @Autowired
    private ApplicationErrorLoggingTask applicationErrorLoggingTask;

    @MockBean
    private CryptoService cryptoService;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testExecute() {
        execution.setVariable(COMMAND_OUTPUT, new StringBuilder(DUMMY_COMMAND_OUTPUT));
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(ERROR_MESSAGE, DUMMY_ERROR_MESSAGE);

        applicationErrorLoggingTask.execute(execution);

        assertThat(execution.getVariable(IS_APPLICATION_TIMED_OUT)).isEqualTo(true);
        assertThat(execution.getVariable(ERROR_MESSAGE)).isEqualTo(DUMMY_COMMAND_OUTPUT);
    }
}
