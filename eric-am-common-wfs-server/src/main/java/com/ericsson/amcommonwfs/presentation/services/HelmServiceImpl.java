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
package com.ericsson.amcommonwfs.presentation.services;

import static com.ericsson.amcommonwfs.CreateCommandUtils.provideArgument;
import static com.ericsson.amcommonwfs.constants.CommandConstants.GET_COMMAND;
import static com.ericsson.amcommonwfs.constants.CommandConstants.KUBE_CONFIG_ARGUMENT;
import static com.ericsson.amcommonwfs.constants.CommandConstants.NAMESPACE_ARGUMENT;
import static com.ericsson.amcommonwfs.constants.CommandConstants.SPACE;
import static com.ericsson.amcommonwfs.constants.CommandConstants.VALUES;
import static com.ericsson.amcommonwfs.presentation.services.KubectlAPIService.DEFAULT_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DEFAULT_HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_BINARIES_LOCATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIST_COMMAND;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.exception.CommandExecutionException;
import com.ericsson.amcommonwfs.util.Constants;
import com.ericsson.amcommonwfs.util.MapUtils;
import com.ericsson.amcommonwfs.util.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;
import com.google.common.base.Strings;

@Component
public class HelmServiceImpl implements HelmService {

    @Autowired
    private ProcessExecutor processExecutor;


    @Override
    public Map<String, Object> getValues(String releaseName, String clusterConfig, String namespace, String fetchTimeOut) {
        String getValuesCommand = buildGetValuesCommand(releaseName, namespace, clusterConfig);
        int timeOut = Strings.isNullOrEmpty(fetchTimeOut) && StringUtils.isNumeric(fetchTimeOut) ?
                Integer.parseInt(fetchTimeOut) : DEFAULT_TIME_OUT;
        try {
            ProcessExecutorResponse processExecutorResponse = processExecutor
                    .executeProcess(getValuesCommand, timeOut, true);
            return processResult(processExecutorResponse);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CommandExecutionException("Error during processing a command: ", ie);
        } catch (IOException | CommandTimedOutException e) {
            throw new CommandExecutionException("Error during processing a command: ", e);
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
    }

    @Override
    public HelmVersionsResponse getHelmVersions() {
        String getHelmVersions = buildGetHelmVersionsCommand();

        try {
            ProcessExecutorResponse processExecutorResponse = processExecutor
                    .executeProcess(getHelmVersions, DEFAULT_TIME_OUT, true);
            List<String> versions = processHelmVersionsResult(processExecutorResponse);
            return new HelmVersionsResponse(versions);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CommandExecutionException("Error during processing a command: ", ie);
        } catch (IOException | CommandTimedOutException e) {
            throw new CommandExecutionException("Error during processing a command: ", e);
        }
    }

    private static String buildGetValuesCommand(String releaseName, String namespace, String clusterConfig) {
        StringBuilder commandBuilder = new StringBuilder(DEFAULT_HELM_CLIENT_VERSION).append(SPACE).append(GET_COMMAND);
        commandBuilder.append(SPACE).append(VALUES);
        commandBuilder.append(SPACE).append(releaseName);
        provideArgument(commandBuilder, NAMESPACE_ARGUMENT, namespace);
        if (!Strings.isNullOrEmpty(clusterConfig) && !Constants.DEFAULT.equals(clusterConfig)) {
            provideArgument(commandBuilder, KUBE_CONFIG_ARGUMENT, clusterConfig);
        }
        return commandBuilder.toString();
    }

    private static Map<String, Object> processResult(ProcessExecutorResponse processExecutorResponse) {
        if (processExecutorResponse.getExitValue() == 0) {
            return MapUtils.convertYamlToMap(processExecutorResponse.getCmdResult());
        } else {
            throw new CommandExecutionException(processExecutorResponse.getCmdResult());
        }
    }

    private static String buildGetHelmVersionsCommand() {
        StringBuilder commandBuilder = new StringBuilder(LIST_COMMAND)
                .append(SPACE)
                .append(HELM_BINARIES_LOCATION);
        return commandBuilder.toString();
    }

    private static List<String> processHelmVersionsResult(ProcessExecutorResponse processExecutorResponse) {
        if (processExecutorResponse.getExitValue() == 0) {
            return Arrays.stream(processExecutorResponse.getCmdResult().split("\n"))
                    .filter(s -> s.contains(DEFAULT_HELM_CLIENT_VERSION + "-"))
                    .map(s -> s.replaceAll(DEFAULT_HELM_CLIENT_VERSION + "-", ""))
                    .collect(Collectors.toList());
        } else {
            throw new CommandExecutionException(processExecutorResponse.getCmdResult());
        }
    }
}
