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

import java.nio.file.Path;
import java.util.List;

import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfo;
import com.ericsson.workflow.orchestration.mgmt.model.NamespaceValidationResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.Secrets;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.SecretInfo;

/**
 * A service for the execution of any kubectl commands
 */
public interface KubectlService {

    /**
     * @param releaseName   the release name of the resource
     * @param clusterConfig the cluster config file for the cluster the resource is deployed in
     * @return list of PodStatusResponse returns a list of pod objects
     */
    PodStatusResponse getPodStatusByReleaseName(String releaseName, String clusterConfig);

    /**
     * @param releaseNames   the release names of the resource
     * @param clusterConfig the cluster config file for the cluster the resource is deployed in
     * @return list of PodStatusResponse returns a list of pod objects
     */
    List<PodStatusResponse> getPodStatusByReleaseNames(List<String> releaseNames, String clusterConfig);

    /**
     * @param releaseNames   the release names of the resource
     * @param clusterConfig the cluster config file for the cluster the resource is deployed in
     * @return KubernetesResourceInfo returns a list of pods deployments and statefulsets
     */
    List<KubernetesResourceInfo> getKubernetesResourceStatusInfoByReleaseNames(List<String> releaseNames, String clusterConfig);

    /**
     * @param asyncDeleteNamespaceRequestDetails wrapper for the request
     * @param clusterConfig the cluster config file for the cluster the resource is deployed in
     */
    void deleteNamespace(AsyncDeleteNamespaceRequestDetails asyncDeleteNamespaceRequestDetails,
                         String clusterConfig);

    /**
     * @param clusterConfig     the cluster config file for the cluster the resource is deployed in
     * @param internalScaleInfo the object that stores scale info needed to downsize resources
     */

    void scaleDownResources(String clusterConfig, InternalScaleInfo internalScaleInfo, String idempotencyKey);

    /**
     * @param internalScaleInfo detail of the resources to be scaled
     * @param namespace         the namespace where the resources exist
     */
    void handleFailedScaleResources(InternalScaleInfo internalScaleInfo, String namespace, String idempotencyKey);

    /**
     * @param clusterConfig        the cluster config file for the cluster the resource is deployed in
     * @param asyncDeletePvcsRequestDetails    wrapped for the pvc request details
     */
    void deletePvcs(String clusterConfig, AsyncDeletePvcsRequestDetails asyncDeletePvcsRequestDetails);


    /**
     * @param namespace     the namespace to get resources
     * @param clusterConfig the cluster config file for the cluster the resource is deployed in
     * @return
     */
    boolean doesNamespaceExist(String namespace, String clusterConfig);

    /**
     * Retrieves al the secret data in the provided namespaces
     *
     * @param namespace
     * @param clusterConfig
     * @param fetchTimeOut
     * @return returns all secret
     */
    Secrets getAllSecretInTheNamespace(String namespace, String clusterConfig, String fetchTimeOut);

    /***
     * To patch a secret with the given key/value in a given namespace
     *
     * @param name the secret name for which the patch request needs to be applied
     * @param secretInfo the object that contains the details of namespace, cluster config, key and value for the secret to be patched
     */
    void patchSecretInNamespace(String name, SecretInfo secretInfo);

    /**
     * @param kubeConfigFile path to config file
     * @return cluster server details
     *
     * @throws com.ericsson.amcommonwfs.exception.KubeConfigValidationException if operation fails
     */

    ClusterServerDetailsResponse getClusterServerDetails(Path kubeConfigFile);

    NamespaceValidationResponse isNamespaceUsedForEvnfmDeployment(String namespace, String cluster);
}
