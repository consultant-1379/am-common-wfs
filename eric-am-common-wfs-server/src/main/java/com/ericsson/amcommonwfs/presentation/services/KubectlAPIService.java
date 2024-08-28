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

import static com.ericsson.amcommonwfs.MessageConstants.FORBIDDEN_CLUSTER_MESSAGE;
import static com.ericsson.amcommonwfs.util.Constants.CONNECTIVITY_TEST_FAILED;
import static com.ericsson.amcommonwfs.util.Constants.CONNECTIVITY_TEST_KUB_EXCEPTION;
import static com.ericsson.amcommonwfs.util.Constants.DEPLOYMENT_KIND;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_CHECK_NAMESPACE_IS_EVNFM_NAMESPACE_AND_CLUSTER;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_GET_SPEC_FROM_KUBERNETES_RESOURCE;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE;
import static com.ericsson.amcommonwfs.util.Constants.STATEFULSET_KIND;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_DELETE_PVCS;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_SCALE_DOWN_RESOURCES;
import static com.ericsson.amcommonwfs.util.Utility.sanitizeError;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_INSTANCE_LABEL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.amcommonwfs.common.EvnfmNamespaceService;
import com.ericsson.amcommonwfs.exception.KubeConfigValidationException;
import com.ericsson.amcommonwfs.exception.KubectlAPIException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.presentation.services.mapper.SecretResponseMapper;
import com.ericsson.amcommonwfs.presentation.services.messaging.GenericMessagingService;
import com.ericsson.amcommonwfs.secret.PatchRequest;
import com.ericsson.amcommonwfs.util.Utility;
import com.ericsson.amcommonwfs.utils.CommonUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResource;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfo;
import com.ericsson.workflow.orchestration.mgmt.model.NamespaceValidationResponse;
import com.ericsson.workflow.orchestration.mgmt.model.Pod;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.SecretAttribute;
import com.ericsson.workflow.orchestration.mgmt.model.Secrets;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;
import com.ericsson.workflow.orchestration.mgmt.model.v3.SecretInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonSyntaxException;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1DeploymentStatus;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1StatefulSetSpec;
import io.kubernetes.client.openapi.models.V1StatefulSetStatus;
import io.kubernetes.client.util.PatchUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * An implementation of {@link KubectlService} which executes kubectl commands on the terminal
 */
@Service
@Slf4j
public class KubectlAPIService implements KubectlService {

    public static final String PATCH_REQUEST_ERROR = "Issue patching the secret %s with the following ERROR %s";
    public static final String UNABLE_TO_DELETE_NAMESPACE = "Unable to delete the namespace %s with the following ERROR %s";
    public static final int DEFAULT_TIME_OUT = 8;
    private static final List<String> KUBE_NAMESPACES = Arrays
            .asList("default", "kube-system", "kube-public", "kube-node-lease");
    private static final String APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION = "app.kubernetes.io/instance";

    @Autowired
    private GenericMessagingService genericMessagingService;

    @Value("${app.command.execute.defaultTimeOut}")
    private String defaultTimeOut;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;

    @Autowired
    private EvnfmNamespaceService evnfmNamespaceService;

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    @Override
    public ClusterServerDetailsResponse getClusterServerDetails(final Path kubeConfigFile) {
        LOGGER.info("Getting cluster host url defined in: {}", kubeConfigFile);
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(kubeConfigFile.toString());
            String hostUrl = kubectlApiRetryTemplate.execute(context -> coreV1Api.getApiClient().getBasePath());
            ClusterServerDetailsResponse response = new ClusterServerDetailsResponse();
            V1NamespaceList clusterNamespaces =
                    kubectlApiRetryTemplate.execute(context -> coreV1Api.listNamespace()
                            .pretty(Boolean.FALSE.toString())
                            .allowWatchBookmarks(Boolean.FALSE)
                            .watch(Boolean.FALSE).execute()
                    );
            List<Namespace> namespaces = clusterNamespaces.getItems().stream().map(V1Namespace::getMetadata)
                    .map(nsmeta -> new Namespace().name(nsmeta.getName()).uid(nsmeta.getUid()))
                    .collect(Collectors.toList());
            String kubernetesVersion = new VersionApi(coreV1Api.getApiClient()).getCode().execute().getGitVersion();
            String formattedK8sVersion = kubernetesVersion.substring(1, kubernetesVersion.lastIndexOf("."));
            return response.hostUrl(hostUrl).version(formattedK8sVersion).namespaces(namespaces);
        } catch (IOException e) {
            Utility.deleteClusterConfigFile(kubeConfigFile);
            throw new KubeConfigValidationException(String.format(CONNECTIVITY_TEST_FAILED, e.getMessage()), e);
        } catch (ApiException e) {
            Utility.deleteClusterConfigFile(kubeConfigFile);
            String errorDetails = sanitizeError(e.getMessage(), e.getResponseBody());

            if (errorDetails != null && errorDetails.contains(CONNECTIVITY_TEST_KUB_EXCEPTION)) {
                errorDetails = String.format(CONNECTIVITY_TEST_FAILED, errorDetails);
            }
            throw new KubeConfigValidationException(errorDetails, e);
        }
    }

    @Override
    public NamespaceValidationResponse isNamespaceUsedForEvnfmDeployment(final String namespace, final String cluster) {
        try {
            boolean isEvnfmNamespaceAndCluster = evnfmNamespaceService.checkEvnfmNamespace(namespace, cluster);
            return new NamespaceValidationResponse(isEvnfmNamespaceAndCluster);
        } catch (IOException | ApiException e) {
            LOGGER.error(e.getMessage(), e);
            throw new KubectlAPIException(String.format(FAILED_TO_CHECK_NAMESPACE_IS_EVNFM_NAMESPACE_AND_CLUSTER, namespace, e.getMessage()));
        } finally {
            Utility.deleteClusterConfigFile(Path.of(cluster));
        }
    }

    @Override
    public PodStatusResponse getPodStatusByReleaseName(final String releaseName, final String clusterConfig) {
        List<Pod> podByReleaseName;
        try {
            podByReleaseName = getPodByReleaseName(String.format(APPLICATION_INSTANCE_LABEL, releaseName), clusterConfig);
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
        PodStatusResponse podStatusResponse = new PodStatusResponse();
        podStatusResponse.setPods(podByReleaseName);
        return podStatusResponse;
    }

    @Override
    public List<PodStatusResponse> getPodStatusByReleaseNames(final List<String> releaseNames, final String clusterConfig) {
        String labelSelector = buildLabelSelectorByInstanceLabel(releaseNames);
        final List<Pod> pods;
        try {
            pods = getPodByReleaseName(labelSelector, clusterConfig);
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
        return buildPodStatusResponses(pods);
    }

    private static List<PodStatusResponse> buildPodStatusResponses(List<Pod> pods) {
        Map<String, List<Pod>> podsByReleaseName = pods.stream()
                .collect(Collectors.groupingBy(pod -> pod.getLabels().get(APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION)));
        return podsByReleaseName.entrySet().stream()
                .map(podsEntry -> buildPodStatusResponse(podsEntry.getKey(), podsEntry.getValue()))
                .collect(Collectors.toList());
    }

    private static PodStatusResponse buildPodStatusResponse(String releaseName, List<Pod> pods) {
        PodStatusResponse podStatusResponse = new PodStatusResponse();
        podStatusResponse.setPods(pods);
        podStatusResponse.setReleaseName(releaseName);
        return podStatusResponse;
    }

    private static String buildLabelSelectorByInstanceLabel(List<String> releaseNames) {
        String releaseNamesJoined = String.join(", ", releaseNames);
        return APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION + " in (" + releaseNamesJoined + ")";
    }

    @Override
    public List<KubernetesResourceInfo> getKubernetesResourceStatusInfoByReleaseNames(final List<String> releaseNames, final String clusterConfig) {
        List<KubernetesResourceInfo> kubernetesResourceInfoList = new ArrayList<>();

        try {
            String labelSelector = buildLabelSelectorByInstanceLabel(releaseNames);
            final List<Pod> pods = getPodByReleaseName(labelSelector, clusterConfig);
            final List<KubernetesResource> deployments = getDeploymentsByReleaseName(labelSelector, clusterConfig);
            final List<KubernetesResource> statefulSets = getStatefulSetsByReleaseName(labelSelector, clusterConfig);

            for (String releaseName : releaseNames) {
                final List<Pod> podsByReleaseName = pods.stream().filter(pod -> releaseName.equals(pod.getLabels().get(
                                APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION)))
                        .collect(Collectors.toList());
                final List<KubernetesResource> deploymentsByReleaseName =
                        deployments.stream().filter(deployment -> releaseName.equals(deployment.getInstanceLabel()))
                                .collect(Collectors.toList());
                final List<KubernetesResource> statefulSetsByReleaseName =
                        statefulSets.stream().filter(statefulSet -> releaseName.equals(statefulSet.getInstanceLabel()))
                                .collect(Collectors.toList());
                KubernetesResourceInfo kubernetesResourceInfo = KubernetesResourceInfo.builder()
                        .releaseName(releaseName)
                        .pods(podsByReleaseName)
                        .deployments(deploymentsByReleaseName)
                        .statefulSets(statefulSetsByReleaseName)
                        .build();
                kubernetesResourceInfoList.add(kubernetesResourceInfo);
            }
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }

        return kubernetesResourceInfoList;
    }

    @Async
    @Override
    public void deleteNamespace(AsyncDeleteNamespaceRequestDetails asyncDeleteNamespaceRequestDetails,
                                String clusterConfig) {

        String timeOut = asyncDeleteNamespaceRequestDetails.getTimeout();
        String namespace = asyncDeleteNamespaceRequestDetails.getNamespace();
        String lifecycleOperationId = asyncDeleteNamespaceRequestDetails.getLifecycleOperationId();
        String release = asyncDeleteNamespaceRequestDetails.getReleaseName();
        String idempotencyKey = asyncDeleteNamespaceRequestDetails.getIdempotencyKey();
        final String applicationTimeout = CommonUtils.validateAppTimeout(timeOut) ? timeOut : defaultTimeOut;

        if (doesNamespaceExist(namespace, clusterConfig)) {
            CoreV1Api coreV1Api = null;
            try {
                coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
                checkNamespace(namespace, clusterConfig, lifecycleOperationId, release, idempotencyKey);
                final CoreV1Api coreV1ApiC = coreV1Api;
                kubectlApiRetryTemplate.execute(context -> coreV1ApiC.deleteNamespace(namespace).propagationPolicy("Foreground").execute());
                verifyNamespaceDeleted(coreV1Api, namespace, applicationTimeout, lifecycleOperationId, release, idempotencyKey);
            } catch (JsonSyntaxException e) {
                // Due to https://github.com/kubernetes-client/java/issues/86 need to catch the exception for wrong
                // response from kubernetes
                if (e.getCause() instanceof IllegalStateException &&
                        e.getMessage() != null &&
                        e.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                    LOGGER.warn("Catching exception as kubernetes API failed to deserialize the response correctly: {}", e.getMessage());
                    verifyNamespaceDeleted(coreV1Api, namespace, applicationTimeout, lifecycleOperationId, release, idempotencyKey);
                } else {
                    handleFailedDeleteNamespace(namespace, e, lifecycleOperationId, release, idempotencyKey);
                }
            } catch (IOException e) {
                handleFailedDeleteNamespace(namespace, e, lifecycleOperationId, release, idempotencyKey);
            } catch (ApiException e) {
                handleFailedDeleteNamespaceApiException(namespace, e, lifecycleOperationId, release, idempotencyKey);
            } finally {
                Utility.deleteClusterConfigFile(Path.of(clusterConfig));
            }
        } else {
            handleAlreadyDeletedNamespace(namespace, lifecycleOperationId, release, idempotencyKey);
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
    }

    private void checkNamespace(final String namespace, final String clusterConfig, final String lifecycleOperationId, final String release,
                                final String idempotencyKey) {
        try {
            if (evnfmNamespaceService.checkEvnfmNamespace(namespace, clusterConfig)) {
                throw new IllegalArgumentException(
                        "Requested namespace contains EVNFM unable to delete please specify a different namespace");
            }
            if (KUBE_NAMESPACES.contains(namespace)) {
                throw new IllegalArgumentException(
                        "Requested namespace is restricted and cannot be deleted please specify a different namespace");
            }
        } catch (IOException | ApiException e) {
            handleFailedDeleteNamespace(namespace, e, lifecycleOperationId, release, idempotencyKey);
        }
    }

    private void verifyNamespaceDeleted(CoreV1Api coreV1Api,
                                        String namespace,
                                        String timeOut,
                                        String lifecycleOperationId,
                                        String release,
                                        String idempotencyKey) {
        if (coreV1Api == null) {
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(TimeUnit.SECONDS) < Integer.parseInt(timeOut)) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage());
                Thread.currentThread().interrupt();
            }
            try {
                V1NamespaceList namespaceList = kubectlApiRetryTemplate
                        .execute(context -> coreV1Api.listNamespace().allowWatchBookmarks(true).limit(0).watch(false).execute());
                Optional<V1Namespace> deletedNamespace =
                        namespaceList.getItems()
                                .stream()
                                .filter(ns -> ns.getMetadata() != null && namespace.equals(ns.getMetadata().getName()))
                                .findFirst();
                if (deletedNamespace.isEmpty()) {
                    LOGGER.info("Namespace {} is deleted.", namespace);
                    sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.COMPLETED,
                                "Deleted namespace successfully.", release, idempotencyKey);
                    return;
                }
                LOGGER.info("Namespace {} is being deleted.", namespace);
            } catch (ApiException e) {
                LOGGER.error("Kubectl API client error occurred: ", e);
                sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                            e.getMessage(), release, idempotencyKey);
                return;
            }
        }
    }

    private void handleFailedDeleteNamespace(final String namespace,
                                             final Exception e,
                                             final String lifecycleOperationId,
                                             final String release,
                                             final String idempotencyKey) {
        String message = String
                .format(UNABLE_TO_DELETE_NAMESPACE, namespace, e.getMessage());
        LOGGER.error(message);
        sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                    message, release, idempotencyKey);
    }

    private void handleAlreadyDeletedNamespace(final String namespace,
                                               final String lifecycleOperationId,
                                               final String release,
                                               final String idempotencyKey) {
        String message = String.format("Namespace %s does not exist or already deleted", namespace);
        LOGGER.warn(message);
        sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.COMPLETED,
                    message, release, idempotencyKey);
    }

    private void handleFailedDeleteNamespaceApiException(final String namespace,
                                                         final ApiException e,
                                                         final String lifecycleOperationId,
                                                         final String release,
                                                         final String idempotencyKey) {
        String message = String
                .format(UNABLE_TO_DELETE_NAMESPACE, namespace, e.getMessage());
        LOGGER.error(message);
        if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
            sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                        FORBIDDEN_CLUSTER_MESSAGE, release, idempotencyKey);
        } else {
            sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                        message, release, idempotencyKey);
        }
    }

    @Async
    @Override
    public void scaleDownResources(String clusterConfig, InternalScaleInfo internalScaleInfo, String idempotencyKey) {
        String namespace = internalScaleInfo.getNamespace();
        String releaseName = internalScaleInfo.getReleaseName();

        try {
            AppsV1Api appsV1Api = kubeClientBuilder.getAppsV1Api(clusterConfig);
            List<V1Deployment> deployments = getDeploymentsByNamespaceAndReleaseName(namespace, releaseName, appsV1Api);
            LOGGER.info("{} Deployment returned for release {} in namespace {}", deployments.size(), releaseName,
                        namespace);
            for (V1Deployment deployment : deployments) {
                boolean isScaleDeploymentSuccess = scaleResource(internalScaleInfo, deployment, 0, appsV1Api, idempotencyKey);
                if (!isScaleDeploymentSuccess) {
                    return;
                }
            }
            List<V1StatefulSet> statefulSets = getStatefulSetsByNamespaceAndReleaseName(namespace, releaseName,
                                                                                        appsV1Api);
            LOGGER.info("{} StatefulSet returned for release {} in namespace {}", statefulSets.size(), releaseName,
                        namespace);
            for (V1StatefulSet statefulSet : statefulSets) {
                boolean isScaleStatefulSetSuccess = scaleResource(internalScaleInfo, statefulSet, 0, appsV1Api, idempotencyKey);
                if (!isScaleStatefulSetSuccess) {
                    return;
                }
            }

            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            String applicationTimeOutStr = internalScaleInfo.getApplicationTimeOut();
            int applicationTimeout = resolveTimeout(applicationTimeOutStr);
            verifyScaleDownComplete(internalScaleInfo, applicationTimeout, appsV1Api, coreV1Api, idempotencyKey);
        } catch (ApiException e) { // NOSONAR
            if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
                handleFailedScaleResources(internalScaleInfo, FORBIDDEN_CLUSTER_MESSAGE, idempotencyKey);
            } else {
                handleFailedScaleResources(internalScaleInfo, e.getMessage(), idempotencyKey);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Scale down resource failed, message {}", e.getMessage(), e);
            handleFailedScaleResources(internalScaleInfo, e.getMessage(), idempotencyKey);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
    }

    @Async
    @Override
    public void deletePvcs(final String clusterConfig, final AsyncDeletePvcsRequestDetails asyncDeletePvcsRequestDetails) {

        String namespace = asyncDeletePvcsRequestDetails.getNamespace();
        String releaseName = asyncDeletePvcsRequestDetails.getReleaseName();
        String applicationTimeOut = asyncDeletePvcsRequestDetails.getTimeout();
        String lifecycleOperationId = asyncDeletePvcsRequestDetails.getLifecycleOperationId();
        String idempotencyKey = asyncDeletePvcsRequestDetails.getIdempotencyKey();
        List<String> labels = asyncDeletePvcsRequestDetails.getLabels();
        try {
            processPvcDeletion(clusterConfig, namespace, releaseName, applicationTimeOut, labels);
            handleSuccessPvc(lifecycleOperationId, releaseName, "Successfully deleted PVCs", idempotencyKey);
        } catch (ApiException e) { // NOSONAR
            String message = String.format(UNABLE_TO_DELETE_PVCS, namespace, releaseName, e.getMessage());
            if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
                handleFailedPvc(lifecycleOperationId, FORBIDDEN_CLUSTER_MESSAGE, releaseName, idempotencyKey);
            } else {
                handleFailedPvc(lifecycleOperationId, message, releaseName, idempotencyKey);
            }
        } catch (IOException e) { // NOSONAR
            String message = String.format(UNABLE_TO_DELETE_PVCS, namespace, releaseName, e.getMessage());
            handleFailedPvc(lifecycleOperationId, message, releaseName, idempotencyKey);
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
    }

    private void processPvcDeletion(String clusterConfig, String namespace,
                                    String releaseName, String applicationTimeOut,
                                    List<String> labels) throws IOException, ApiException {
        if (doesNamespaceExist(namespace, clusterConfig)) {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            int applicationTimeout = resolveTimeout(applicationTimeOut);
            String pvcResource = String.format(APPLICATION_INSTANCE_LABEL, releaseName);
            if (CollectionUtils.isEmpty(labels)) {
                deletePvc(namespace, pvcResource, applicationTimeout, coreV1Api);
            } else {
                for (String label : labels) {
                    deletePvc(namespace, pvcResource + "," + label, applicationTimeout, coreV1Api);
                }
            }
        }
    }

    public List<V1Deployment> getDeploymentsByNamespaceAndReleaseName(String namespace, String releaseName,
                                                                      AppsV1Api appsV1Api) throws ApiException { // NOSONAR
        return kubectlApiRetryTemplate.execute(context -> appsV1Api.listNamespacedDeployment(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .execute().getItems());
    }

    public List<V1StatefulSet> getStatefulSetsByNamespaceAndReleaseName(String namespace, String releaseName,
                                                                        AppsV1Api appsV1Api) throws ApiException { // NOSONAR
        return kubectlApiRetryTemplate.execute(context -> appsV1Api.listNamespacedStatefulSet(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .execute().getItems());
    }

    public List<V1ReplicaSet> getReplicaSetsByNamespaceAndReleaseName(String namespace, String releaseName,
                                                                      AppsV1Api appsV1Api) throws ApiException { // NOSONAR
        return kubectlApiRetryTemplate.execute(context -> appsV1Api.listNamespacedReplicaSet(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .execute().getItems());
    }

    public List<V1Pod> getPodsByNamespaceAndReleaseName(String namespace, String releaseName, CoreV1Api coreV1Api) throws ApiException { // NOSONAR
        return kubectlApiRetryTemplate.execute(context -> coreV1Api.listNamespacedPod(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .execute().getItems());
    }

    @VisibleForTesting
    <T> boolean scaleResource(InternalScaleInfo internalScaleInfo, T resource, int replicaCount, AppsV1Api appsV1Api,
                              String idempotencyKey) throws ApiException { // NOSONAR
        // Need to update this method to use the kubectl scale feature when the Kubernetes Java client has this
        // feature released
        String jsonPatchStr = String
                .format("[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":%d}]", replicaCount);
        V1Patch patch = new V1Patch(jsonPatchStr);
        boolean isScaleResourceSuccess = true;
        if (resource.getClass().equals(V1Deployment.class)) {
            V1Deployment deployment = (V1Deployment) resource;
            Optional<V1ObjectMeta> metadata = Optional.ofNullable(deployment.getMetadata());
            if (metadata.isPresent()) {
                kubectlApiRetryTemplate.execute(context ->
                                                        PatchUtils.patch(V1Scale.class,
                                                                         () -> appsV1Api
                                                                                 .patchNamespacedDeploymentScale(metadata.get().getName(),
                                                                                                                 internalScaleInfo.getNamespace(),
                                                                                                                 patch)
                                                                                 .buildCall(null),
                                                                         V1Patch.PATCH_FORMAT_JSON_PATCH,
                                                                         appsV1Api.getApiClient())
                );
            } else {
                isScaleResourceSuccess = false;
                handleFailedScaleResources(internalScaleInfo, "metadata of Deployment not found.", idempotencyKey);
            }
        } else if (resource.getClass().equals(V1StatefulSet.class)) {
            V1StatefulSet statefulSet = (V1StatefulSet) resource;
            Optional<V1ObjectMeta> metadata = Optional.ofNullable(statefulSet.getMetadata());
            if (metadata.isPresent()) {
                kubectlApiRetryTemplate.execute(context ->
                                                        PatchUtils.patch(V1Scale.class,
                                                                         () -> appsV1Api
                                                                                 .patchNamespacedStatefulSetScale(metadata.get().getName(),
                                                                                                                  internalScaleInfo.getNamespace(),
                                                                                                                  patch)
                                                                                 .buildCall(null),
                                                                         V1Patch.PATCH_FORMAT_JSON_PATCH,
                                                                         appsV1Api.getApiClient())
                );
            } else {
                isScaleResourceSuccess = false;
                handleFailedScaleResources(internalScaleInfo, "metadata of StatefulSet not found.", idempotencyKey);
            }
        }

        return isScaleResourceSuccess;
    }

    private void verifyScaleDownComplete(InternalScaleInfo internalScaleInfo, int applicationTimeout,
                                         AppsV1Api appsV1Api, CoreV1Api coreV1Api,
                                         String idempotencyKey) throws ApiException, InterruptedException { // NOSONAR
        String namespace = internalScaleInfo.getNamespace();
        String releaseName = internalScaleInfo.getReleaseName();
        boolean isReplicaSetDownZero;
        boolean isStatefulSetDownZero;
        boolean isPodDownZero;

        LOGGER.info("Verifying if all ReplicaSets and StatefulSets have been scaled down to 0");
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(TimeUnit.SECONDS) < applicationTimeout) {
            List<V1ReplicaSet> replicaSets = getReplicaSetsByNamespaceAndReleaseName(namespace, releaseName, appsV1Api);
            List<V1StatefulSet> statefulSets = getStatefulSetsByNamespaceAndReleaseName(namespace, releaseName,
                                                                                        appsV1Api);
            List<V1Pod> pods = getPodsByNamespaceAndReleaseName(namespace, releaseName, coreV1Api);

            isReplicaSetDownZero = replicaSets.stream().allMatch(replicaSet -> {
                int desiredReplicas = replicaSet.getSpec().getReplicas();
                int availableReplicas = Optional.ofNullable(replicaSet.getStatus().getAvailableReplicas()).orElse(0);
                int readyReplicas = Optional.ofNullable(replicaSet.getStatus().getReadyReplicas()).orElse(0);
                return desiredReplicas == 0 && availableReplicas == 0 && readyReplicas == 0;
            });

            isStatefulSetDownZero = statefulSets.stream().allMatch(statefulSet -> {
                int desiredReplicas = statefulSet.getSpec().getReplicas();
                int availableReplicas = Optional.ofNullable(statefulSet.getStatus().getCurrentReplicas()).orElse(0);
                int readyReplicas = Optional.ofNullable(statefulSet.getStatus().getReadyReplicas()).orElse(0);
                return desiredReplicas == 0 && availableReplicas == 0 && readyReplicas == 0;
            });

            isPodDownZero = pods.isEmpty();

            if (isReplicaSetDownZero && isStatefulSetDownZero && isPodDownZero) {
                handleSuccessScaleResources(internalScaleInfo, idempotencyKey);
                return;
            }
            Thread.sleep(5000);
        }
        handleFailedScaleResources(internalScaleInfo,
                                   String.format("timeout verifying completion of scaling down within %d seconds", applicationTimeout),
                                   idempotencyKey);
    }

    @VisibleForTesting
    public boolean doesNamespaceExist(String namespace, String clusterConfig) {
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            kubectlApiRetryTemplate.execute(context -> coreV1Api.readNamespace(namespace).execute());
            return true;
        } catch (IOException | ApiException e) {
            LOGGER.error("Unable to read namespace {} due to {}", namespace, e);
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
            return false;
        }
    }

    @VisibleForTesting
    void handleSuccessScaleResources(InternalScaleInfo internalScaleInfo, String idempotencyKey) {
        sendDownResultMessage(internalScaleInfo, WorkflowServiceEventStatus.COMPLETED,
                              "Successfully scaled down all ReplicaSets and StatefulSets to 0.", idempotencyKey);
    }

    public void handleFailedScaleResources(final InternalScaleInfo internalScaleInfo, final String errorMessage, String idempotencyKey) {
        String message = String.format(UNABLE_TO_SCALE_DOWN_RESOURCES, internalScaleInfo.getNamespace(),
                                       internalScaleInfo.getReleaseName(), errorMessage);
        sendDownResultMessage(internalScaleInfo, WorkflowServiceEventStatus.FAILED, message, idempotencyKey);
    }

    void handleFailedPvc(final String lifecycleOperationId, final String message, final String releaseName, final String idempotencyKey) {
        sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_PVC, WorkflowServiceEventStatus.FAILED,
                    message, releaseName, idempotencyKey);
    }

    public void handleSuccessPvc(final String lifecycleOperationId, final String releaseName, final String message, final String idempotencyKey) {
        sendMessage(lifecycleOperationId, WorkflowServiceEventType.DELETE_PVC, WorkflowServiceEventStatus.COMPLETED,
                    message, releaseName, idempotencyKey);
    }

    @VisibleForTesting
    void sendDownResultMessage(InternalScaleInfo internalScaleInfo, WorkflowServiceEventStatus status, String message, String idempotencyKey) {
        LOGGER.info(message);
        String releaseName = internalScaleInfo.getReleaseName();
        String lifecycleOperationId = internalScaleInfo.getLifecycleOperationId();
        WorkflowServiceEventMessage eventMessage = new WorkflowServiceEventMessage(lifecycleOperationId,
                                                                                   WorkflowServiceEventType.DOWNSIZE, status, message,
                                                                                   releaseName);
        genericMessagingService.prepareAndSend(eventMessage, idempotencyKey);
    }

    @VisibleForTesting
    void sendMessage(String lifecycleOperationId, WorkflowServiceEventType workflowServiceEventType,
                     WorkflowServiceEventStatus status, String message, String releaseName, String idempotencyKey) {
        LOGGER.info(message);
        WorkflowServiceEventMessage eventMessage = new WorkflowServiceEventMessage(lifecycleOperationId,
                                                                                   workflowServiceEventType, status, message, releaseName);
        genericMessagingService.prepareAndSend(eventMessage, idempotencyKey);
    }

    public V1PodList getV1PodList(final String releaseName, final String clusterConfig) {
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            LOGGER.info("Attempting to retrieve Pod List information for release {} in cluster {} ", releaseName,
                        clusterConfig);
            return kubectlApiRetryTemplate.execute(context -> coreV1Api
                    .listPodForAllNamespaces().labelSelector(releaseName).timeoutSeconds(DEFAULT_TIME_OUT).execute());
        } catch (IOException | ApiException e) {
            String message = String
                    .format("Issue getting the podStatusByReleaseName with the following ERROR %s", e.getMessage());
            LOGGER.error(message, e);
            throw new KubectlAPIException(message);
        }
    }

    public List<Pod> getPodByReleaseName(final String releaseName, final String clusterConfig) {
        V1PodList v1PodList = getV1PodList(releaseName, clusterConfig); // NOSONAR
        LOGGER.info("Successfully retrieved Pod List information for release {} in cluster {}", releaseName, clusterConfig);
        return v1PodList.getItems().isEmpty() ? Collections.emptyList() : getPods(v1PodList);
    }

    public List<KubernetesResource> getDeploymentsByReleaseName(final String releaseName, final String clusterConfig) {
        V1DeploymentList v1DeploymentList = getV1DeploymentList(releaseName, clusterConfig);
        return getDeployments(v1DeploymentList);
    }

    public List<KubernetesResource> getStatefulSetsByReleaseName(final String releaseName, final String clusterConfig) {
        V1StatefulSetList v1StatefulSetList = getV1StatefulSetList(releaseName, clusterConfig);
        return getStatefulSets(v1StatefulSetList);
    }

    private V1DeploymentList getV1DeploymentList(final String releaseName, final String clusterConfig) {
        try {
            AppsV1Api appsV1Api = kubeClientBuilder.getAppsV1Api(clusterConfig);
            LOGGER.info("Started to retrieve Deployment List information for release {} in cluster {}", releaseName,
                        clusterConfig);
            return kubectlApiRetryTemplate.execute(context -> appsV1Api
                    .listDeploymentForAllNamespaces().labelSelector(releaseName).timeoutSeconds(DEFAULT_TIME_OUT).execute());
        } catch (IOException | ApiException e) {
            String message = String
                    .format("Failed to retrieve Deployment List information for release %s in cluster %s with the following error: %s", releaseName,
                            clusterConfig, e.getMessage());
            LOGGER.error(message, e);
            throw new KubectlAPIException(message);
        }
    }

    private V1StatefulSetList getV1StatefulSetList(final String releaseName, final String clusterConfig) {
        try {
            AppsV1Api appsV1Api = kubeClientBuilder.getAppsV1Api(clusterConfig);
            LOGGER.info("Started to retrieve StatefulSet List information for release {} in cluster {}", releaseName,
                        clusterConfig);
            return kubectlApiRetryTemplate.execute(context -> appsV1Api
                    .listStatefulSetForAllNamespaces().labelSelector(releaseName).timeoutSeconds(DEFAULT_TIME_OUT).execute());
        } catch (IOException | ApiException e) {
            String message = String
                    .format("Failed to retrieve StatefulSet List information for release %s in cluster %s with the following error: %s", releaseName,
                            clusterConfig, e.getMessage());
            LOGGER.error(message, e);
            throw new KubectlAPIException(message);
        }
    }

    @SuppressWarnings("squid:S4248")
    @VisibleForTesting
    List<Pod> getPods(final V1PodList v1PodList) {
        return v1PodList.getItems().stream().peek(v1Pod -> {
            if (v1Pod.getMetadata() == null || StringUtils.isEmpty(v1Pod.getMetadata().getName())
                    || v1Pod.getStatus() == null || StringUtils.isEmpty(v1Pod.getStatus().getPhase())) {
                LOGGER.error("Issue fetching the metadata or status in the Pod : {} ", v1Pod);
                throw new KubectlAPIException("Unable to get the metadata or status from Pod details");
            }
            if (v1Pod.getSpec() == null) {
                LOGGER.error("Issue fetching the spec in the Pod : {} ", v1Pod);
                throw new KubectlAPIException("Unable to get the spec from Pod details");
            }
        }).map(v1Pod -> {
            V1ObjectMeta metadata = v1Pod.getMetadata();
            V1PodStatus status = v1Pod.getStatus();
            V1PodSpec spec = v1Pod.getSpec();
            String hostname = spec.getNodeName();
            Pod pod = new Pod(metadata.getUid(), metadata.getName(), status.getPhase(), metadata.getNamespace(), hostname);
            LOGGER.debug("Creating mapping for Pod: uid={}, name={}, status={}, namespace={}, "
                                 + "labels{}, annotations={}, ownerReferences={}", pod.getUid(), pod.getName(),
                         pod.getStatus(), pod.getNamespace(),
                         pod.getLabels(), pod.getAnnotations(), pod.getOwnerReferences());
            pod.setLabels(metadata.getLabels());
            pod.setAnnotations(metadata.getAnnotations());
            pod.setOwnerReferences(metadata.getOwnerReferences());
            return pod;
        }).collect(Collectors.toList());
    }

    private static List<KubernetesResource> getDeployments(final V1DeploymentList v1DeploymentList) {
        List<V1Deployment> v1Deployments = v1DeploymentList.getItems();
        return v1Deployments.stream()
                .map(KubectlAPIService::buildDeployment)
                .collect(Collectors.toList());
    }

    private static List<KubernetesResource> getStatefulSets(final V1StatefulSetList v1StatefulSetList) {
        List<V1StatefulSet> v1StatefulSets = v1StatefulSetList.getItems();
        return v1StatefulSets.stream()
                .map(KubectlAPIService::buildStatefulSet)
                .collect(Collectors.toList());
    }

    private static KubernetesResource buildDeployment(V1Deployment v1Deployment) {
        V1DeploymentStatus status = v1Deployment.getStatus();
        V1ObjectMeta metadata = v1Deployment.getMetadata();
        V1DeploymentSpec spec = v1Deployment.getSpec();

        if (metadata == null || StringUtils.isEmpty(metadata.getName())) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
        }
        if (status == null) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
        }
        if (spec == null) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_SPEC_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
        }

        final Integer availableReplicas = Optional.ofNullable(status.getAvailableReplicas())
                .or(() -> Optional.ofNullable(status.getReadyReplicas()))
                .orElse(0);
        final Integer desiredReplicas = Optional.ofNullable(spec.getReplicas())
                .orElse(1);

        KubernetesResource kubernetesResource = KubernetesResource.builder()
                .uid(metadata.getUid())
                .name(metadata.getName())
                .kind(DEPLOYMENT_KIND)
                .namespace(metadata.getNamespace())
                .replicas(desiredReplicas)
                .availableReplicas(availableReplicas)
                .ownerReferences(metadata.getOwnerReferences())
                .build();
        if (metadata.getLabels() != null) {
            kubernetesResource.setInstanceLabel(metadata.getLabels().getOrDefault(APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION, null));
        }

        return kubernetesResource;
    }

    private static KubernetesResource buildStatefulSet(V1StatefulSet v1StatefulSet) {
        V1StatefulSetStatus status = v1StatefulSet.getStatus();
        V1ObjectMeta metadata = v1StatefulSet.getMetadata();
        V1StatefulSetSpec spec = v1StatefulSet.getSpec();

        if (metadata == null || StringUtils.isEmpty(metadata.getName())) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE, STATEFULSET_KIND));
        }
        if (status == null) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE, STATEFULSET_KIND));
        }
        if (spec == null) {
            throw new KubectlAPIException(String.format(FAILED_TO_GET_SPEC_FROM_KUBERNETES_RESOURCE, STATEFULSET_KIND));
        }

        final Integer availableReplicas = Optional.ofNullable(status.getAvailableReplicas())
                .or(() -> Optional.ofNullable(status.getReadyReplicas()))
                .orElse(0);
        final Integer desiredReplicas = Optional.ofNullable(spec.getReplicas())
                .orElse(1);

        KubernetesResource kubernetesResource = KubernetesResource.builder()
                .uid(metadata.getUid())
                .name(metadata.getName())
                .kind(STATEFULSET_KIND)
                .namespace(metadata.getNamespace())
                .replicas(desiredReplicas)
                .availableReplicas(availableReplicas)
                .ownerReferences(metadata.getOwnerReferences())
                .build();
        if (metadata.getLabels() != null) {
            kubernetesResource.setInstanceLabel(metadata.getLabels().getOrDefault(APPLICATION_INSTANCE_LABEL_FOR_BULK_SELECTION, null));
        }

        return kubernetesResource;
    }

    @Override
    public Secrets getAllSecretInTheNamespace(String namespace, String clusterConfig, String fetchTimeOut) {
        Secrets secrets = new Secrets();
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            kubectlApiRetryTemplate.execute(context -> coreV1Api.readNamespace(namespace).execute());
            int timeOut = StringUtils.isNotBlank(fetchTimeOut) && StringUtils.isNumeric(fetchTimeOut) ?
                    Integer.parseInt(fetchTimeOut) : DEFAULT_TIME_OUT;
            V1SecretList secretList = kubectlApiRetryTemplate.execute(context -> coreV1Api.listNamespacedSecret(namespace)
                    .timeoutSeconds(timeOut)
                    .execute()
            );
            if (secretList == null || secretList.getItems().isEmpty()) {
                return secrets;
            }

            Map<String, SecretAttribute> secretAttributeMap = new HashMap<>();
            for (V1Secret secret : secretList.getItems()) {
                //Those secrets would be skipped that is not having name or metadata
                if (secret.getMetadata() != null && secret.getMetadata().getName() != null) {
                    secretAttributeMap.put(secret.getMetadata().getName(),
                                           SecretResponseMapper.INSTANCE.mapV1SecretToSecretAttribute(secret));
                }
            }

            secrets.setAllSecrets(secretAttributeMap);
        } catch (IOException | ApiException e) { // NOSONAR
            LOGGER.error("Unable to read secret in namespace {} due to {}", namespace, e);
        } finally {
            Utility.deleteClusterConfigFile(Path.of(clusterConfig));
        }
        return secrets;
    }

    @Override
    public void patchSecretInNamespace(String name, SecretInfo secretInfo) {
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(secretInfo.getClusterName());
            patchSecret(name, secretInfo, coreV1Api);
        } catch (IOException e) { // NOSONAR
            String message = String
                    .format(PATCH_REQUEST_ERROR, name, e.getMessage());
            throw new KubectlAPIException(message);
        } finally {
            if (secretInfo.getClusterName() != null) {
                Utility.deleteClusterConfigFile(Path.of(secretInfo.getClusterName()));
            }
        }
    }

    @VisibleForTesting
    void deletePvc(final String namespace, final String pvcResource, final int applicationTimeout, CoreV1Api coreV1Api) throws ApiException {
        // NOSONAR
        kubectlApiRetryTemplate.execute(context -> coreV1Api.deleteCollectionNamespacedPersistentVolumeClaim(namespace)
                .labelSelector(pvcResource)
                .timeoutSeconds(applicationTimeout)
                .execute()
        );
    }

    @VisibleForTesting
    private void patchSecret(String name, SecretInfo secretInfo, CoreV1Api coreV1Api) throws JsonProcessingException {
        try {
            V1Patch v1Patch = getV1Path("replace", "/data/" + secretInfo.getKey(),
                                        Base64.getEncoder().encodeToString(secretInfo.getValue().getBytes(StandardCharsets.UTF_8)));
            kubectlApiRetryTemplate.execute(context ->
                                                    PatchUtils.patch(V1Secret.class,
                                                                     () -> coreV1Api.patchNamespacedSecret(name, secretInfo.getNamespace(), v1Patch)
                                                                             .buildCall(null),
                                                                     V1Patch.PATCH_FORMAT_JSON_PATCH,
                                                                     coreV1Api.getApiClient()));
        } catch (ApiException e) { // NOSONAR
            if (e.getMessage().contains("NOT FOUND")) {
                String message = String.format("The secret %s is not found in the namespace : %s in the cluster : %s", name,
                                               secretInfo.getNamespace(), secretInfo.getClusterName());
                throw new NotFoundException(message);
            } else {
                String message = String
                        .format(PATCH_REQUEST_ERROR, name, e.getResponseBody());
                throw new KubectlAPIException(message);
            }
        }
    }

    private static V1Patch getV1Path(String operation, String path, String value) throws JsonProcessingException {
        List<PatchRequest> patchRequests = new ArrayList<>();
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setOperation(operation);
        patchRequest.setPath(path);
        patchRequest.setValue(value);
        patchRequests.add(patchRequest);

        ObjectMapper objectMapper = new ObjectMapper();
        return new V1Patch(objectMapper.writeValueAsString(patchRequests));
    }

    private int resolveTimeout(final String applicationTimeout) {
        return CommonUtils.validateAppTimeout(applicationTimeout)
                ? Integer.parseInt(applicationTimeout)
                : Integer.parseInt(defaultTimeOut);
    }
}
