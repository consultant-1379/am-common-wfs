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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.INSTANCE_LABEL;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFICATION_ANNOTATION;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_INSTANCE_LABEL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_DEPLOYED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.amcommonwfs.exceptions.InvalidAnnotationException;
import com.ericsson.amcommonwfs.models.ContainerDetails;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1DeploymentStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1StatefulSetSpec;
import io.kubernetes.client.openapi.models.V1StatefulSetStatus;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VerificationHelper {

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Autowired
    @Qualifier("kubectlApiRetryTemplate")
    private RetryTemplate kubectlApiRetryTemplate;

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    public void verifyApplicationDeployedUsingAnnotation(DelegateExecution execution) {
        LOGGER.info("Verifying application deployed using annotation");

        execution.setVariable(APP_DEPLOYED, false);

        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String namespace = (String) execution.getVariable(NAMESPACE);
        final String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        final boolean skipJobVerification = (Boolean) execution.getVariable(SKIP_JOB_VERIFICATION);
        final int applicationTimeOut = Integer.parseInt(resolveTimeOut(execution));

        try {
            if (podsAreNotCreatedInRelease(namespace, releaseName, applicationTimeOut, clusterConfig)) {
                return;
            }

            List<ContainerDetails> containerDetails = populateContainerDetails(releaseName,
                                                                               namespace,
                                                                               clusterConfig,
                                                                               skipJobVerification,
                                                                               applicationTimeOut);

            if (CollectionUtils.isEmpty(containerDetails)) {
                logNoContainerDetails();

                execution.setVariable(APP_DEPLOYED, true);
            } else {
                List<ContainerDetails> containersNotReady = containerDetails.stream()
                        .filter(container -> !container.isStatePassed())
                        .collect(toList());

                LOGGER.debug("Status of containers waiting to reach expected state: {}", containersNotReady);
                LOGGER.info("Total no of containers waiting to reach expected state: {}", containersNotReady.size());

                execution.setVariable(APP_DEPLOYED, containersNotReady.isEmpty());
            }
        } catch (Exception e) { // NOSONAR
            handlerErrorFromKubectl(execution, e.getMessage());
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

    private List<ContainerDetails> populateContainerDetails(final String releaseName,
                                                            final String namespace,
                                                            final String clusterConfig,
                                                            final boolean skipJobVerification,
                                                            final int applicationTimeOut) throws IOException, ApiException {

        V1PodList v1PodList = getV1PodList(namespace, clusterConfig, releaseName, applicationTimeOut);
        Map<String, Map<String, List<ContainerDetails>>> podContainersBasedOnState = getPodContainersBasedOnState(v1PodList, skipJobVerification);
        List<ContainerDetails> containerDetailsList = getContainerDetails(podContainersBasedOnState);
        v1PodList.getItems().parallelStream()
                .filter(v1Pod -> !isPodCreatedByJob(v1Pod))
                .forEach(checkExpectedStateForPods(podContainersBasedOnState));

        return containerDetailsList;
    }

    private static void logNoContainerDetails() {
        LOGGER.info("No Container details to be verified. Please check whether the following are provided in the Chart:" +
                            "1. Release label " + INSTANCE_LABEL + " must be present in the pod" +
                            "2. Container names in the annotation must match the ones present in the pod" +
                            "3. If the container name is getting overridden at runtime, then the same must be given in the annotation");
        LOGGER.info("No verification is done. It is highly recommended to check the status of the " +
                            "application resources to determine successful deployment.");
    }

    private static List<ContainerDetails> getContainerDetails(
            final Map<String, Map<String, List<ContainerDetails>>> podContainersBasedOnState) {
        return podContainersBasedOnState.values()
                .stream()
                .flatMap(containersMap -> containersMap
                        .values()
                        .stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static Consumer<V1Pod> checkExpectedStateForPods(Map<String, Map<String, List<ContainerDetails>>> podContainersBasedOnState) {
        return v1Pod -> {
            if (v1Pod.getMetadata() != null && v1Pod.getStatus() != null) {
                Map<String, List<ContainerDetails>> containerDetails = podContainersBasedOnState.get(v1Pod.getMetadata().getName());
                if (!CollectionUtils.isEmpty(v1Pod.getStatus().getContainerStatuses()) && !CollectionUtils.isEmpty(containerDetails)) {
                    v1Pod.getStatus().getContainerStatuses().forEach(verifyContainerState(containerDetails));
                }
            }
        };
    }

    private static Consumer<V1ContainerStatus> verifyContainerState(Map<String, List<ContainerDetails>> containerDetails) {
        return v1ContainerStatus -> {
            List<ContainerDetails> containerDetailsList = containerDetails.get(v1ContainerStatus.getName());
            if (!CollectionUtils.isEmpty(containerDetailsList)) {
                ContainerDetails detail = containerDetailsList.get(0);
                detail.setStatePassed(hasContainerReachedExpectedState(v1ContainerStatus, detail));
            }
        };
    }

    private static boolean hasContainerReachedExpectedState(V1ContainerStatus v1ContainerStatus,
                                                            ContainerDetails detail) {
        ContainerDetails.ContainerState state = detail.getState();
        V1ContainerState containerStatusState = v1ContainerStatus.getState();
        return detail.isReady() == v1ContainerStatus.getReady().booleanValue() &&
                getContainerState(state, containerStatusState);
    }

    private static Boolean getContainerState(final ContainerDetails.ContainerState state,
                                             final V1ContainerState containerStatusState) {
        switch (state) {
            case RUNNING:
                return containerStatusState.getRunning() != null;
            case WAITING:
                return containerStatusState.getWaiting() != null;
            case TERMINATED:
                return containerStatusState.getTerminated() != null;
            default:
                return false;
        }
    }

    private static boolean isPodCreatedByJob(V1Pod v1Pod) {
        if (v1Pod.getMetadata() != null && !CollectionUtils.isEmpty(v1Pod.getMetadata().getOwnerReferences())) {
            return v1Pod.getMetadata().getOwnerReferences().stream()
                    .anyMatch(v1OwnerReference -> "Job".equalsIgnoreCase(v1OwnerReference.getKind())
                            && BooleanUtils.toBooleanDefaultIfNull(v1OwnerReference.getController(), false));
        }
        return false;
    }

    public Map<String, Map<String, List<ContainerDetails>>> getPodContainersBasedOnState(final V1PodList v1PodList,
                                                                                         final Boolean skipJobVerification) {
        return v1PodList.getItems().stream()
                .filter(v1Pod -> !skipJobVerification || !isPodCreatedByJob(v1Pod))
                .filter(v -> v.getMetadata() != null)
                .collect(toMap(pod -> pod.getMetadata().getName(), pod -> collectContainersFromPod().apply(pod)))
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(groupingBy(ContainerDetails::getPodName, groupingBy(ContainerDetails::getContainerName)));
    }

    public void verifyApplicationDeployed(DelegateExecution execution) {
        LOGGER.info("Verifying application deploy ready status");

        execution.setVariable(APP_DEPLOYED, false);

        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String namespace = (String) execution.getVariable(NAMESPACE);
        final String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        final boolean skipJobVerification = (Boolean) execution.getVariable(SKIP_JOB_VERIFICATION);
        final int commandTimeOut = Integer.parseInt(resolveTimeOut(execution));

        try {
            if (podsAreNotCreatedInRelease(namespace, releaseName, commandTimeOut, clusterConfig)) {
                return;
            }

            V1PodList releasePods = getV1PodList(namespace, clusterConfig, releaseName, commandTimeOut);

            List<V1Pod> podsToVerify = releasePods.getItems().stream()
                    .filter(VerificationHelper::isPodNotEvicted)
                    .filter(pod -> verifyJobCreatedPod(skipJobVerification, pod))
                    .collect(toList());

            LOGGER.info("Total number of pods to be verified : {} ", podsToVerify.size());

            if (podsInReadyState(podsToVerify) == podsToVerify.size()) {
                execution.setVariable(APP_DEPLOYED, true);
            }
        } catch (Exception e) { // NOSONAR
            handlerErrorFromKubectl(execution, e.getMessage());
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

    private static boolean isPodNotEvicted(final V1Pod pod) {
        return pod.getStatus() != null
                && !"Evicted".equals(pod.getStatus().getReason());
    }

    private static void handlerErrorFromKubectl(final DelegateExecution execution, final String message) {
        execution.setVariable(ERROR_MESSAGE, message);
        LOGGER.error("Application failed due to {} ", message);
        BusinessProcessExceptionUtils.handleException(BPMN_KUBECTL_FAILURE, message, execution);
    }

    private boolean podsAreNotCreatedInRelease(final String namespace,
                                               final String releaseName,
                                               final int applicationTimeOut,
                                               final String clusterConfig) throws IOException, ApiException {

        final AppsV1Api appsV1Api = kubeClientBuilder.getAppsV1Api(clusterConfig);

        return !podsAreCreatedInReleaseDeployments(namespace, releaseName, applicationTimeOut, appsV1Api) ||
                !podsAreCreatedInReleaseStatefulSets(namespace, releaseName, applicationTimeOut, appsV1Api);
    }


    private boolean podsAreCreatedInReleaseStatefulSets(final String namespace,
                                                        final String releaseName,
                                                        final int timeOut,
                                                        final AppsV1Api appsV1Api) throws ApiException {

        final List<V1StatefulSet> statefulSets = getV1StatefulSetList(namespace, releaseName, timeOut, appsV1Api).getItems();

        LOGGER.info("Total number of stateful sets to be verified: {}", statefulSets.size());

        final int numStatefulSetsWithCreatedPods = (int) statefulSets.stream()
                .filter(Objects::nonNull)
                .filter(VerificationHelper::podsAreCreatedInStatefulSet)
                .count();

        final int numStatefulSetsWithoutCreatedPods = statefulSets.size() - numStatefulSetsWithCreatedPods;
        if (numStatefulSetsWithoutCreatedPods > 0) {
            LOGGER.info("Total number of stateful sets without created pods: {}", numStatefulSetsWithoutCreatedPods);
        }

        return numStatefulSetsWithoutCreatedPods == 0;
    }

    private static boolean podsAreCreatedInStatefulSet(final V1StatefulSet statefulSet) {
        final V1StatefulSetSpec spec = statefulSet.getSpec();
        final V1StatefulSetStatus status = statefulSet.getStatus();

        final int expectedReplicas = defaultIfNull(spec != null ? spec.getReplicas() : null, 0);
        final int updatedReplicas = defaultIfNull(status != null ? status.getUpdatedReplicas() : null, 0);

        LOGGER.debug("Number of expected replicas: {} and number of updated replicas: {} in stateful set: {}",
                     expectedReplicas,
                     updatedReplicas,
                     getName(statefulSet.getMetadata()));

        return expectedReplicas == updatedReplicas;
    }

    private boolean podsAreCreatedInReleaseDeployments(final String namespace,
                                                       final String releaseName,
                                                       final int timeOut,
                                                       final AppsV1Api appsV1Api) throws ApiException {

        final List<V1Deployment> releaseDeployments = getV1DeploymentList(namespace, releaseName, timeOut, appsV1Api).getItems();

        LOGGER.info("Total number of deployments to be verified: {}", releaseDeployments.size());

        final int numDeploymentsWithCreatedPods = (int) releaseDeployments.stream()
                .filter(Objects::nonNull)
                .filter(VerificationHelper::podsAreCreatedInDeployment)
                .count();

        final int numDeploymentsWithoutCreatedPods = releaseDeployments.size() - numDeploymentsWithCreatedPods;
        if (numDeploymentsWithoutCreatedPods > 0) {
            LOGGER.info("Total number of deployments without created pods: {}", numDeploymentsWithoutCreatedPods);
        }

        return numDeploymentsWithoutCreatedPods == 0;
    }

    private static boolean podsAreCreatedInDeployment(final V1Deployment deployment) {
        final V1DeploymentSpec spec = deployment.getSpec();
        final V1DeploymentStatus status = deployment.getStatus();

        final int expectedReplicas = defaultIfNull(spec != null ? spec.getReplicas() : null, 0);
        final int updatedReplicas = defaultIfNull(status != null ? status.getUpdatedReplicas() : null, 0);

        LOGGER.debug("Number of expected replicas: {} and number of updated replicas {} in deployment: {}",
                     expectedReplicas,
                     updatedReplicas,
                     getName(deployment.getMetadata()));

        return expectedReplicas == updatedReplicas;
    }

    int podsInReadyState(final List<V1Pod> releasePods) {
        return (int) releasePods.stream()
                .map(V1Pod::getStatus)
                .filter(Objects::nonNull)
                .map(V1PodStatus::getConditions)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(this::checkPodReadyConditionFromPodStatus).count();
    }

    public static boolean verifyJobCreatedPod(final boolean skipJobVerification, final V1Pod v1Pod) {
        if (!skipJobVerification) {
            return true;
        } else {
            return !isPodCreatedByJob(v1Pod);
        }
    }

    boolean checkPodReadyConditionFromPodStatus(V1PodCondition cond) {
        if ("Ready".equals(cond.getType())) {
            return ("True".equals(cond.getStatus()) ||
                    (cond.getReason() != null && "PodCompleted".equals(cond.getReason())));
        }
        return false;
    }

    public V1StatefulSetList getV1StatefulSetList(final String namespace,
                                                  final String releaseName,
                                                  final int timeOut,
                                                  AppsV1Api appsV1Api) throws ApiException { // NOSONAR

        return kubectlApiRetryTemplate.execute(context -> appsV1Api.listNamespacedStatefulSet(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .timeoutSeconds(timeOut).execute()
              );
    }

    public V1DeploymentList getV1DeploymentList(final String namespace,
                                                final String releaseName,
                                                final int timeOut,
                                                AppsV1Api appsV1Api) throws ApiException { // NOSONAR

        return kubectlApiRetryTemplate.execute(context -> appsV1Api.listNamespacedDeployment(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .timeoutSeconds(timeOut).execute()
        );
    }

    public V1PodList getV1PodList(final String namespace, final String clusterName, final String releaseName,
                                  final int timeOut) throws IOException, ApiException { // NOSONAR
        CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterName);
        return kubectlApiRetryTemplate.execute(context -> coreV1Api.listNamespacedPod(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .timeoutSeconds(timeOut).execute()
        );
    }

    private static String getName(V1ObjectMeta metaData) {
        return metaData != null ? metaData.getName() : null;
    }

    @VisibleForTesting
    protected Function<V1Pod, List<ContainerDetails>> collectContainersFromPod() {
        return v1Pod -> {
            boolean isPodAnnotated = false;
            List<ContainerDetails> containerDetailsList = new ArrayList<>();
            if (v1Pod.getMetadata() != null && v1Pod.getSpec() != null) {
                String containerDetails = Optional.ofNullable(v1Pod.getMetadata().getAnnotations()).isPresent()
                        ? v1Pod.getMetadata().getAnnotations().get(VERIFICATION_ANNOTATION) : null;

                if (StringUtils.isNotEmpty(containerDetails)) {
                    containerDetailsList = convertContainerDetailsToModel(containerDetails);
                    containerDetailsList.removeIf(containerDetail -> isContainersNotPresentInPod(v1Pod, containerDetail));

                    isPodAnnotated = !containerDetailsList.isEmpty();
                }

                Map<String, ContainerDetails> containerDetailsMap = containerDetailsList.stream()
                        .collect(toMap(ContainerDetails::getContainerName, containerDetail -> containerDetail));

                v1Pod.getSpec().getContainers().forEach(populateContainerDetails(v1Pod, containerDetailsMap, isPodAnnotated));

                return new ArrayList<>(containerDetailsMap.values());
            }
            return new ArrayList<>();
        };
    }

    private static Consumer<V1Container> populateContainerDetails(V1Pod v1Pod,
                                                                  Map<String, ContainerDetails> containerDetailsMap, boolean isPodAnnotated) {
        return container -> {
            V1ObjectMeta metadata = v1Pod.getMetadata();
            if (metadata != null) {
                if (!isPodAnnotated) {
                    containerDetailsMap.put(container.getName(), new ContainerDetails(
                            container.getName(), ContainerDetails.ContainerState.RUNNING, true,
                            metadata.getName()));
                } else if (containerDetailsMap.get(container.getName()) != null) {
                    containerDetailsMap.get(container.getName()).setPodName(metadata.getName());
                }
            }
        };
    }

    private static List<ContainerDetails> convertContainerDetailsToModel(String containerDetails) {
        List<ContainerDetails> containerDetailsList;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        try {
            containerDetailsList = objectMapper.readValue(containerDetails, new TypeReference<>() {
            });
        } catch (IOException e) { // NOSONAR
            LOGGER.error("Error while parsing the JSON array: ", e);
            throw new InvalidAnnotationException(e.getMessage());
        }
        return containerDetailsList;
    }

    private static boolean isContainersNotPresentInPod(V1Pod v1Pod, ContainerDetails containerDetail) {
        var spec = v1Pod.getSpec();
        if (spec != null && !CollectionUtils.isEmpty(spec.getContainers()) &&
                !v1Pod.getSpec().getContainers().stream()
                        .filter(v1Container -> v1Container.getName() != null)
                        .map(v1Container -> v1Container.getName().toLowerCase()).collect(toList()) // NOSONAR
                        .contains(containerDetail.getContainerName().toLowerCase())) { // NOSONAR
            V1ObjectMeta metadata = v1Pod.getMetadata();
            if (metadata != null) {
                LOGGER.info("Container - {} is not present in the pod - {} ", containerDetail.getContainerName(),
                            metadata.getName());
            }
            return true;
        }
        return false;
    }
}
