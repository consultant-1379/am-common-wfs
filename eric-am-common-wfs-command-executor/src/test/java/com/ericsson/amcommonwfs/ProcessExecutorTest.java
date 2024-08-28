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

import com.ericsson.amcommonwfs.utility.DataParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessExecutorTest {

    private static final String INVALID_KUBE_COMMAND = "kube test command";

    public static final String VALID_KUBECTL_COMMAND = "kubectl get deployments";

    public static final String CMD_TIMED_OUT_ERR_MSG = "Unable to get the result in the time specified";

    public static final String SAMPLE_VALID_HELM_COMMAND = "helm status sample-release-name";

    private ProcessExecutor processExecutor = new ProcessExecutor();

    private Process process;

    private MockedConstruction<ProcessBuilder> processBuilderMockedConstruction;

    private MockedConstruction<InputStreamReader> inputStreamReaderMockedConstruction;

    private MockedConstruction<BufferedReader> bufferedReaderMockedConstruction;


    @BeforeEach
    public void init() throws InterruptedException {
        process = mock(Process.class);
        processBuilderMockedConstruction = mockConstruction(ProcessBuilder.class, (mock, context) -> when(mock.start()).thenReturn(process));
        inputStreamReaderMockedConstruction = mockConstruction(InputStreamReader.class);
    }

    @AfterEach
    public void cleanUp() {
        processBuilderMockedConstruction.close();
        inputStreamReaderMockedConstruction.close();
        if(!bufferedReaderMockedConstruction.isClosed()) {
            bufferedReaderMockedConstruction.close();
        }
    }

    @Test
    public void testSuccessfulExecution() throws CommandTimedOutException, IOException, InterruptedException {
        Stream<String> str = Stream.generate(() -> DataParser.readFile("processExecSuccess.txt")).limit(1);

        bufferedReaderMockedConstruction = mockConstruction(BufferedReader.class, (mock, context) -> {
            when(mock.lines()).thenReturn(str);
        });
        when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);
        ProcessExecutorResponse processExecutorResponse = processExecutor.executeProcess(VALID_KUBECTL_COMMAND, 5, false);

        verify(process, times(1)).destroy();
        verify(bufferedReaderMockedConstruction.constructed().get(0), times(1)).close();
        assertThat(processExecutorResponse.getCmdResult()).isNotNull();
        assertThat(processExecutorResponse.getExitValue()).isEqualTo(0);

    }

    @Test
    public void testProcessOutputFormat() throws Exception {
        Stream<String> strFormat = Stream.generate(() -> DataParser.readFile("processExecFormat.txt")).limit(1);

        bufferedReaderMockedConstruction = mockConstruction(BufferedReader.class, (mock, context) -> {
            when(mock.lines()).thenReturn(strFormat);
        });
        when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);
        ProcessExecutorResponse processExecutorResponse = processExecutor.executeProcess(SAMPLE_VALID_HELM_COMMAND, 5, false);
        bufferedReaderMockedConstruction.close();

        assertThat(processExecutorResponse.getCmdResult()).hasLineCount(20);
        assertThat(processExecutorResponse.getCmdResult().contains("[^\\p{ASCII}]")).isFalse();

    }

    @Test
    public void testCommandExecutionTimeOut()  throws Exception {
        Stream<String> str = Stream.generate(() -> DataParser.readFile("processExecTimeout.txt")).limit(1);

        bufferedReaderMockedConstruction = mockConstruction(BufferedReader.class, (mock, context) -> {
            when(mock.lines()).thenReturn(str);
        });
        when(process.waitFor(0, TimeUnit.SECONDS)).thenReturn(false);
        assertThatThrownBy(() -> processExecutor.executeProcess(VALID_KUBECTL_COMMAND, 0, false))
                .isInstanceOf(CommandTimedOutException.class)
                .hasMessage(CMD_TIMED_OUT_ERR_MSG);

    }

    @Test
    public void testFailureExecution() throws Exception{
        Stream<String> errorResponse = Stream.generate(() -> DataParser.readFile("processExecError.txt")).limit(1);

        bufferedReaderMockedConstruction = mockConstruction(BufferedReader.class, (mock, context) -> {
            when(mock.lines()).thenReturn(errorResponse);
        });
        when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(1);
        ProcessExecutorResponse processExecutorResponse = processExecutor.executeProcess(INVALID_KUBE_COMMAND, 5, false);

        verify(process, times(1)).destroy();
        verify(bufferedReaderMockedConstruction.constructed().get(0), times(1)).close();
        assertThat(processExecutorResponse.getCmdResult()).isNotNull();
        assertThat(processExecutorResponse.getCmdResult().contains("CommandNotFoundException")).isTrue();
        assertThat(processExecutorResponse.getExitValue()).isEqualTo(1);
    }
}
