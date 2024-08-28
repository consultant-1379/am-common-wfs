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

import static com.ericsson.amcommonwfs.constant.CommandTaskConstants.BASH;
import static com.ericsson.amcommonwfs.constant.CommandTaskConstants.BASH_ARG;
import static com.ericsson.amcommonwfs.constant.CommandTaskConstants.POWER_SHELL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_EXIT_STATUS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_OUTPUT;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.util.CollectionUtils;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("squid:S2068")
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandUtils {
    private static final Pattern PASSWORD = Pattern.compile("password=\\w+");
    private static final Pattern SECRET_VALUE_CREATION = Pattern
            .compile("(--from-literal=)([a-zA-Z0-9]+=)([a-zA-Z0-9\\W]+)");
    private static final Pattern SECRET_VALUE = Pattern
            .compile("(--set 'day0\\.configuration)([a-zA-Z0-9.]+value'=)([a-zA-Z0-9\\W]+)");
    private static final String SECRET_VALUE_REPLACEMENT = "$1$2*******";
    private static final String PASSWORD_REPLACEMENT = "password=*******";

    public static List<String> constructFullCommand(final String command) {
        List<String> commandsToExecute = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commandsToExecute.add(POWER_SHELL);
        } else {
            commandsToExecute.add(BASH);
            commandsToExecute.add(BASH_ARG);
        }
        commandsToExecute.add(command);
        LOGGER.info("Commands to execute :: {}", hideSensitiveData(commandsToExecute));
        return commandsToExecute;
    }

    public static List<String> hideSensitiveData(final List<String> commandsToExecute) {
        List<String> hashedPasswords = new ArrayList<>();
        if (!CollectionUtils.isEmpty(commandsToExecute)) {
            hashedPasswords = commandsToExecute.stream().map(CommandUtils::hideSensitiveData)
                    .collect(Collectors.toList());
        }
        return hashedPasswords;
    }

    public static String hideSensitiveData(String command) {
        String processedCommand = PASSWORD.matcher(command).replaceAll(PASSWORD_REPLACEMENT);
        processedCommand = SECRET_VALUE_CREATION.matcher(processedCommand).replaceAll(SECRET_VALUE_REPLACEMENT);
        if (processedCommand.contains("day0.configuration")) {
            processedCommand = SECRET_VALUE.matcher(processedCommand).replaceAll(SECRET_VALUE_REPLACEMENT);
        }
        return processedCommand;
    }

    public static String verifyCommand(DelegateExecution execution) {
        final StringBuilder commandBuilder = (StringBuilder) execution.getVariable(COMMAND);
        if (commandBuilder == null || Strings.isNullOrEmpty(commandBuilder.toString())) {
            BusinessProcessExceptionUtils
                    .handleException(ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION, "Command not provided", execution);
        }
        return commandBuilder.toString(); //NOSONAR
    }

    public static void checkCommandExitStatus(final DelegateExecution execution, final ErrorCode errorCode) {
        int commandExitStatus = (int) execution.getVariable(COMMAND_EXIT_STATUS);
        if (commandExitStatus != 0) {
            String errorOutput = ((StringBuilder) execution.getVariable(COMMAND_OUTPUT)).toString();
            BusinessProcessExceptionUtils.handleException(errorCode, errorOutput, execution);
        }
    }
}
