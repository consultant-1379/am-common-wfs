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
package com.ericsson.amcommonwfs.uninstall;

import static com.ericsson.amcommonwfs.CommandType.GET;
import static com.ericsson.amcommonwfs.CreateCommandUtils.provideArgument;
import static com.ericsson.amcommonwfs.constants.CommandConstants.GET_COMMAND;
import static com.ericsson.amcommonwfs.constants.CommandConstants.KUBE_CONFIG_ARGUMENT;
import static com.ericsson.amcommonwfs.constants.CommandConstants.NAMESPACE_ARGUMENT;
import static com.ericsson.amcommonwfs.constants.CommandConstants.SPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.CommandType;
import com.ericsson.amcommonwfs.HelmCommand;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

@Component
public class VerifyUninstalledCommand implements HelmCommand {

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.BPMN_GET_FAILURE;
    }

    public CommandType getType() {
        return GET;
    }

    @Override
    public void createCommand(final DelegateExecution execution) {
        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String namespace = (String) execution.getVariable(NAMESPACE);

        final String helmClientVersion = (String) execution.getVariable(HELM_CLIENT_VERSION);

        StringBuilder commandBuilder = new StringBuilder(helmClientVersion).append(SPACE).append(GET_COMMAND);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        execution.setVariable(CLUSTER_NAME, clusterConfig);

        commandBuilder.append(SPACE).append(releaseName);
        provideArgument(commandBuilder, NAMESPACE_ARGUMENT, namespace);
        provideArgument(commandBuilder, KUBE_CONFIG_ARGUMENT, clusterConfig);
        execution.setVariable(COMMAND, commandBuilder);
    }
}
