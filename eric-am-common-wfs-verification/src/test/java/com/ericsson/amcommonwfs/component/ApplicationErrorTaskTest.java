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

import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFY_CMD_EXEC_RESULT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.IS_APPLICATION_TIMED_OUT;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ApplicationErrorTask.class})
public class ApplicationErrorTaskTest {
    private static final String DUMMY_ERROR_MESSAGE = "Happy Helming! Error: dummy_error_message";

    @Autowired
    private ApplicationErrorTask applicationErrorTask;

    @MockBean
    private CryptoService cryptoService;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testExecute() {
        execution.setVariable(ERROR_MESSAGE, DUMMY_ERROR_MESSAGE);

        applicationErrorTask.execute(execution);

        assertThat(execution.getVariable(ERROR_MESSAGE))
                .isEqualTo("{\"detail\":\"Error: dummy_error_message\",\"status\":\"422 UNPROCESSABLE_ENTITY\"}");
        assertThat(execution.getVariable(VERIFY_CMD_EXEC_RESULT)).isEqualTo("Error: dummy_error_message");
    }

    @Test
    public void testExecuteWithEmptyErrorMessage() {
        execution.setVariable(ERROR_MESSAGE, null);
        execution.setVariable(IS_APPLICATION_TIMED_OUT, true);

        applicationErrorTask.execute(execution);

        assertThat(execution.getVariable(ERROR_MESSAGE))
                .isEqualTo("{\"detail\":\"{\\\"detail\\\":\\\"Verification of the lifecycle operation failed. Please try increasing the "
                                   + "applicationTimeOut.\\\",\\\"status\\\":\\\"422 UNPROCESSABLE_ENTITY\\\"}\",\"status\":\"422 "
                                   + "UNPROCESSABLE_ENTITY\"}");
        assertThat(execution.getVariable(VERIFY_CMD_EXEC_RESULT))
                .isEqualTo("{\"detail\":\"Verification of the lifecycle operation failed. Please try increasing the "
                                   + "applicationTimeOut.\",\"status\":\"422 UNPROCESSABLE_ENTITY\"}");
    }

    @Test
    public void testExecuteWithEmptyExecution() {
        applicationErrorTask.execute(execution);

        assertThat(execution.getVariable(ERROR_MESSAGE))
                .isEqualTo("{\"detail\":\"Application Error.\",\"status\":\"422 UNPROCESSABLE_ENTITY\"}");
        assertThat(execution.getVariable(VERIFY_CMD_EXEC_RESULT))
                .isEqualTo("Application Error.");
    }
}
