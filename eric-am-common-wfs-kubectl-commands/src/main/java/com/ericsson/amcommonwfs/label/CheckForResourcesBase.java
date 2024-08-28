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
package com.ericsson.amcommonwfs.label;

import com.ericsson.amcommonwfs.pod.GetPods;
import com.ericsson.amcommonwfs.pod.UnfinishedPollingException;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Optional;

import static com.ericsson.amcommonwfs.util.BpmKubeApiExceptionHandler.handleGenericException;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_INSTANCE_LABEL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PODS_POLLING_CONTINUE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

public abstract class CheckForResourcesBase implements JavaDelegate {
    @Autowired
    private ClusterFileUtils clusterFileUtils;
    @Autowired
    protected GetPods getPods;

    protected CheckForResourcesBase(ClusterFileUtils clusterFileUtils, GetPods getPods) {
        this.clusterFileUtils = clusterFileUtils;
        this.getPods = getPods;
    }

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String namespace = (String) execution.getVariable(NAMESPACE);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        getLogger().info("Checking for resources on cluster");

        final int applicationTimeOut = Integer.parseInt(resolveTimeOut(execution));
        Optional<V1PodList> podList = Optional.empty();
        try {
            if (StringUtils.isNotEmpty(namespace)) {
                podList = Optional.ofNullable(getPodsInNamespace(releaseName, namespace, clusterConfig, applicationTimeOut));
            } else {
                podList = Optional.ofNullable(getPodsInAllNamespaces(releaseName, clusterConfig, applicationTimeOut));
                podList.ifPresent(podlist -> podlist.getItems().stream()
                        .findFirst()
                        .filter(pod -> pod != null && pod.getMetadata() != null)
                        .ifPresent(somePod -> execution.setVariable(NAMESPACE, somePod.getMetadata())));
            }
        } catch (UnfinishedPollingException ex) {
            getLogger().info("Polling for pods not completed, retrying.");
            execution.setVariable(PODS_POLLING_CONTINUE, Boolean.TRUE);
        } catch (final ApiException ex) {
            handleGenericException(BPMN_KUBECTL_FAILURE, execution, ex);
        } catch (IOException ex) { // NOSONAR
            handleErrorFromKubectl(execution, ex.getMessage());
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
        if (!podList.isPresent() || CollectionUtils.isEmpty(podList.get().getItems())) {
            String missingLabel = String.format(APPLICATION_INSTANCE_LABEL, releaseName);
            logDeploymentWarningMessage(releaseName, missingLabel);
            BusinessProcessExceptionUtils.throwBusinessProcessException(ErrorCode.BPMN_REQUIRED_LABEL_NOT_PRESENT, String.format("The label '%s' "
                            + "is not on any resources",
                    missingLabel));
        }
        execution.setVariable(PODS_POLLING_CONTINUE, Boolean.FALSE);
    }

    protected abstract V1PodList getPodsInAllNamespaces(String releaseName, String clusterConfig,
                                                        int applicationTimeOut) throws IOException, ApiException;

    protected abstract V1PodList getPodsInNamespace(String releaseName, String namespace, String clusterConfig,
                                                    int applicationTimeOut) throws IOException, ApiException;

    protected abstract Logger getLogger();

    private void logDeploymentWarningMessage(final String releaseName, final String label) {
        getLogger().warn("No pod resources were found associated with the release name: {} matching label: {}.",
                releaseName, label);
        getLogger().warn("As no resources have been found matching the label: {}, the verification must be skipped",
                label);
        getLogger().warn("It is highly recommended to check the status of the application resources");
    }

    private void handleErrorFromKubectl(final DelegateExecution execution, final String message) {
        getLogger().error("Retrieve label failed due to {} ", message);
        BusinessProcessExceptionUtils.handleException(BPMN_KUBECTL_FAILURE, message, execution);
    }
}
