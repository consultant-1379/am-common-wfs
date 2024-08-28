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

import static com.ericsson.amcommonwfs.utility.CommandUtils.constructFullCommand;
import static com.ericsson.amcommonwfs.utility.CommandUtils.hideSensitiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessExecutor {
    public static final String CMD_TIMED_OUT_ERR_MSG = "Unable to get the result in the time specified";

    private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");

    public ProcessExecutorResponse executeProcess(String command, int timeOut, boolean formatted)
                throws IOException, InterruptedException, CommandTimedOutException {

        List<String> commandsToExecute = constructFullCommand(command);
        final ProcessBuilder pb = new ProcessBuilder(commandsToExecute);
        return runProcess(pb, timeOut, formatted);
    }

    public ProcessExecutorResponse executeProcess(List<String> commandArgs, int timeOut)
                throws IOException, InterruptedException, CommandTimedOutException {
        return runProcess(new ProcessBuilder(commandArgs), timeOut, false);
    }

    private static ProcessExecutorResponse runProcess(ProcessBuilder processBuilder, int timeOut, boolean formatted)
                throws IOException, InterruptedException, CommandTimedOutException {
        processBuilder.redirectErrorStream(true);

        Process process = null;
        ProcessExecutorResponse processExecutorResponse;
        try {
            process = processBuilder.start();
            boolean isCmdExecSuccess = process.waitFor(timeOut, TimeUnit.SECONDS);
            if (!isCmdExecSuccess) {
                if (timeOut == 0) {
                    LOGGER.error("Command :: {} had no timeout", hideSensitiveData(processBuilder.command()));
                } else {
                    LOGGER.error("Command :: {} took more than : {} seconds", hideSensitiveData(processBuilder.command()), timeOut);
                }
                process.destroy();
                processOutput(process, formatted);
                throw new CommandTimedOutException(CMD_TIMED_OUT_ERR_MSG);
            }
            processExecutorResponse = processOutput(process, formatted);

        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return processExecutorResponse;
    }

    private static ProcessExecutorResponse processOutput(final Process process, boolean formatted) throws IOException {
        if (formatted) {
            return processFormattedOutput(process);
        } else {
            return processOutput(process);
        }
    }

    private static ProcessExecutorResponse processOutput(final Process process) throws IOException {
        final ProcessExecutorResponse processExecutorResponse = new ProcessExecutorResponse();
        processExecutorResponse.setExitValue(process.exitValue());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) { // NOSONAR
            String cmdResult = br.lines().map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
            Matcher m = NON_ASCII_CHARS.matcher(cmdResult);
            cmdResult = m.replaceAll("");
            processExecutorResponse.setCmdResult(cmdResult);
        }
        LOGGER.info("ProcessExecutorResponse :: {} ", processExecutorResponse);
        return processExecutorResponse;
    }

    private static ProcessExecutorResponse processFormattedOutput(final Process process) throws IOException {
        final ProcessExecutorResponse processExecutorResponse = new ProcessExecutorResponse();
        String theString = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        processExecutorResponse.setCmdResult(theString);

        LOGGER.info("ProcessExecutorResponse :: {} ", processExecutorResponse);
        return processExecutorResponse;
    }
}
