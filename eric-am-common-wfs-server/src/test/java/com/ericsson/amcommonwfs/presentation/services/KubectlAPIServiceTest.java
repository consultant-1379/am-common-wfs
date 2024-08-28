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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.MessageConstants.FORBIDDEN_CLUSTER_MESSAGE;
import static com.ericsson.amcommonwfs.presentation.services.KubectlAPIService.PATCH_REQUEST_ERROR;
import static com.ericsson.amcommonwfs.presentation.services.KubectlAPIService.UNABLE_TO_DELETE_NAMESPACE;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE;
import static com.ericsson.amcommonwfs.util.Constants.FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE;
import static com.ericsson.amcommonwfs.util.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_DELETE_PVCS;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_SCALE_DOWN_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.amcommonwfs.common.EvnfmNamespaceService;
import com.ericsson.amcommonwfs.exception.KubeConfigValidationException;
import com.ericsson.amcommonwfs.exception.KubectlAPIException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.presentation.services.messaging.GenericMessagingService;
import com.ericsson.amcommonwfs.util.Utility;
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
import com.google.gson.JsonSyntaxException;

import io.kubernetes.client.openapi.ApiClient;
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
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetSpec;
import io.kubernetes.client.openapi.models.V1ReplicaSetStatus;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1StatefulSetSpec;
import io.kubernetes.client.openapi.models.V1StatefulSetStatus;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.openapi.models.VersionInfo;
import io.kubernetes.client.util.PatchUtils;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class KubectlAPIServiceTest {

    private static final String DEFAULT_APPLICATION_TIMEOUT = "20";
    private static final String TEST_NAMESPACE = "namespace";
    private static final String DEFAULT = "default";
    private static final String EVNFM_NAMESPACE = "evnfm-namespace";
    private static final String CLUSTER_CONFIG = "clusterConfig";
    private static final String LIFECYCLE_ID = "lifecycleOperationId";
    private static final String RELEASE_NAME = "releaseName";
    private static final String CLUSTER_IS_UNREACHABLE_ERROR_MESSAGE = "Cluster is unreachable";
    private static final String DEPLOYMENT_KIND = "Deployment";
    private static final String STATEFULSET_KIND = "Statefulset";
    private static final String IDEMPOTENCY_KEY = "dummyKey";

    private static String pathToTempDirectory = System.getProperty("java.io.tmpdir");

    @TempDir
    public File folder;

    @Spy
    @InjectMocks
    private KubectlAPIService kubectlService = new KubectlAPIService();

    @Mock
    private GenericMessagingService genericMessagingService;

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private AppsV1Api appsV1Api;

    @Mock
    private EvnfmNamespaceService evnfmNamespaceService;

    @Mock
    private KubeClientBuilder kubeClientBuilder;

    @Mock
    private CoreV1Api.APIdeleteCollectionNamespacedPersistentVolumeClaimRequest apiDeleteCollectionNamespacedPersistentVolumeClaimRequest;

    @Mock
    private CoreV1Api.APIreadNamespaceRequest apiReadNamespaceRequest;

    @Mock
    private CoreV1Api.APIlistNamespaceRequest apiListNamespaceRequest;

    @Mock
    private VersionApi.APIgetCodeRequest apiGetCodeRequest;

    @Mock
    private AppsV1Api.APIlistNamespacedStatefulSetRequest apiListNamespacedStatefulSetRequest;

    @Mock
    private AppsV1Api.APIlistStatefulSetForAllNamespacesRequest apiListStatefulSetForAllNamespacesRequest;

    @Mock
    private AppsV1Api.APIlistDeploymentForAllNamespacesRequest apiListDeploymentForAllNamespacesRequest;

    @Mock
    private AppsV1Api.APIpatchNamespacedStatefulSetScaleRequest apiPatchNamespacedStatefulSetScaleRequest;

    @Mock
    private CoreV1Api.APIdeleteNamespaceRequest apiDeleteNamespaceRequest;

    @Mock
    private CoreV1Api.APIlistPodForAllNamespacesRequest apiListPodForAllNamespacesRequest;

    @Mock
    private CoreV1Api.APIlistNamespacedSecretRequest apiListNamespacedSecretRequest;


    @BeforeAll
    public static void setUp() {
        mockStatic(PatchUtils.class);
    }

    @BeforeEach
    public void init() {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000l);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        RetryPolicy retryPolicy = new SimpleRetryPolicy(3,
                                                        Map.of(ApiException.class, Boolean.TRUE));
        retryTemplate.setRetryPolicy(retryPolicy);
        ReflectionTestUtils.setField(kubectlService, "kubectlApiRetryTemplate", retryTemplate);
        ReflectionTestUtils.setField(kubectlService, "defaultTimeOut", DEFAULT_APPLICATION_TIMEOUT);

        mockApiDeleteCollectionNamespacedPersistentVolumeClaimRequest();
        mockApiReadNamespaceRequest();
        mockApiListNamespaceRequest();
        mockApiDeleteNamespaceRequest();
        mockApiListPodForAllNamespacesRequest();
        mockApiListNamespacedSecretRequest();
        mockApiListNamespacedStatefulSetRequest();
        mockApiListStatefulSetForAllNamespacesRequest();
        mockApiListDeploymentForAllNamespacesRequest();
    }

    @AfterEach
    public void tearDown() {
        reset(PatchUtils.class);
    }

    @AfterAll
    public static void cleanClusterConfigsIfAny() {
        Utility.deleteClusterConfigFile(new File(pathToTempDirectory + File.separator + CLUSTER_CONFIG).toPath());
        Utility.deleteClusterConfigFile(new File(pathToTempDirectory + File.separator + DEFAULT).toPath());
    }

    @Test
    public void testDeletePvcWithMultipleLabels() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));
        List<String> labels = Arrays.asList("app=1", "app=2");

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        doNothing().when(kubectlService).handleSuccessPvc(anyString(), anyString(), eq("Successfully deleted PVCs"), eq(IDEMPOTENCY_KEY));
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "3000", LIFECYCLE_ID, IDEMPOTENCY_KEY, labels));
        verify(kubectlService, times(1)).deletePvc(anyString(), eq("app.kubernetes.io/instance=releaseName,app=1"), anyInt(), any(CoreV1Api.class));
        verify(kubectlService, times(1)).deletePvc(anyString(), eq("app.kubernetes.io/instance=releaseName,app=2"), anyInt(), any(CoreV1Api.class));
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithTimeoutOverflow() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        doNothing().when(kubectlService).handleSuccessPvc(anyString(), anyString(), eq("Successfully deleted PVCs"), eq(IDEMPOTENCY_KEY));
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "2000000000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.singletonList("app=1")));
        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(),
                                                   eq(Integer.parseInt(DEFAULT_APPLICATION_TIMEOUT)), any(CoreV1Api.class));

        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithTimeoutNegative() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        doNothing().when(kubectlService).handleSuccessPvc(anyString(), anyString(), eq("Successfully deleted PVCs"), eq(IDEMPOTENCY_KEY));
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(DEFAULT));
        kubectlService.deletePvcs(DEFAULT, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "-12",
                                                          LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.singletonList("app=1")));
        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(),
                                                   eq(Integer.parseInt(DEFAULT_APPLICATION_TIMEOUT)), any(CoreV1Api.class));
    }

    @Test
    public void testDeletePvcWithTimeoutNaN() throws Exception {

        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        doNothing().when(kubectlService).handleSuccessPvc(anyString(), anyString(), eq("Successfully deleted PVCs"), eq(IDEMPOTENCY_KEY));
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "NotANumber",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.singletonList("app=1")));
        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(),
                                                   eq(Integer.parseInt(DEFAULT_APPLICATION_TIMEOUT)), any(CoreV1Api.class));

        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithTimeout() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        doNothing().when(kubectlService).handleSuccessPvc(anyString(), anyString(), eq("Successfully deleted PVCs"), eq(IDEMPOTENCY_KEY));
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "5000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.singletonList("app=1")));
        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(), eq(5000), any(CoreV1Api.class));
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithForbiddenApiException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        var expectedException = new ApiException(HttpStatus.FORBIDDEN.value(), "FAILED");

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest.execute()).thenThrow(expectedException);

        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "5000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.emptyList()));

        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(), eq(5000), any(CoreV1Api.class));
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_PVC,
                                                WorkflowServiceEventStatus.FAILED, FORBIDDEN_CLUSTER_MESSAGE, RELEASE_NAME)), eq(IDEMPOTENCY_KEY));

        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithApiException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));
        var expectedException = new ApiException("FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(DEFAULT));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest.execute()).thenThrow(expectedException);

        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "5000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.emptyList()));

        verify(kubectlService, times(1)).deletePvc(anyString(), anyString(), eq(5000), any(CoreV1Api.class));
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_PVC, WorkflowServiceEventStatus.FAILED,
                                                String.format(UNABLE_TO_DELETE_PVCS, TEST_NAMESPACE, RELEASE_NAME, expectedException.getMessage()),
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcWithIOException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));
        IOException expectedException = new IOException("FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenThrow(expectedException);

        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "5000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.emptyList()));

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                                                                         new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_PVC, WorkflowServiceEventStatus.FAILED,
                                                                                                         String.format(UNABLE_TO_DELETE_PVCS, TEST_NAMESPACE, RELEASE_NAME, expectedException.getMessage()), RELEASE_NAME)),
                                                                 eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void testDeletePvcSuccess() throws Exception {
        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(DEFAULT));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        when(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest.execute()).thenReturn(new V1Status());
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deletePvcs(tempClusterConfig, buildDeletePvc(TEST_NAMESPACE, RELEASE_NAME, "5000",
                                                                    LIFECYCLE_ID, IDEMPOTENCY_KEY, Collections.emptyList()));

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_PVC,
                                                WorkflowServiceEventStatus.COMPLETED, "Successfully deleted PVCs", RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void kubeConfigFileIsNotValidatedWhenTheCommandReturnsAnErrorCausedByIOException() throws IOException {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenThrow(new IOException());

        final Path inValidConfigFile = Files.createTempFile(folder.toPath(), null, null);
        assertThatThrownBy(() -> kubectlService.getClusterServerDetails(inValidConfigFile))
                .isInstanceOf(KubeConfigValidationException.class)
                .hasMessageContaining("Connectivity test failed, please check your connection to the target cluster");
        assertThat(inValidConfigFile).doesNotExist();
    }

    @Test
    public void kubeConfigFileIsNotValidatedWhenTheCommandReturnsAnErrorCausedByApiException() throws IOException, ApiException {
        var apiException = new ApiException("Api Exception");
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.getApiClient()).thenReturn(new ApiClient().setBasePath("TEST_PATH"));
        when(apiListNamespaceRequest.execute()).thenThrow(apiException);

        final Path inValidConfigFile = Files.createTempFile(folder.toPath(), null, null);
        assertThatThrownBy(() -> kubectlService.getClusterServerDetails(inValidConfigFile))
                .isInstanceOf(KubeConfigValidationException.class)
                .hasMessageContaining("Api Exception");
        assertThat(inValidConfigFile).doesNotExist();
    }

    @Test
    public void testGetClusterServerDetailsSuccessWithServerVersion() throws IOException, ApiException {
        var apiClient = new ApiClient().setBasePath("TEST_PATH");
        var v1NamespaceList = new V1NamespaceList().addItemsItem(new V1Namespace().metadata(new V1ObjectMeta().name(TEST_NAMESPACE)));

        VersionInfo versionInfoMock = Mockito.mock(VersionInfo.class);
        MockedConstruction<VersionApi> versionApiMock = mockConstruction(VersionApi.class, (mock, context) -> {
            when(mock.getCode()).thenReturn(apiGetCodeRequest);
            when(apiGetCodeRequest.execute()).thenReturn(versionInfoMock);
        });

        when(versionInfoMock.getGitVersion()).thenReturn("v1.26.6");
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.getApiClient()).thenReturn(apiClient);
        when(apiListNamespaceRequest.execute()).thenReturn(v1NamespaceList);

        var expected = new ClusterServerDetailsResponse()
                .hostUrl(apiClient.getBasePath())
                .version("1.26")
                .namespaces(v1NamespaceList.getItems().stream().map(V1Namespace::getMetadata)
                                    .map(nsmeta -> new Namespace().name(nsmeta.getName()).uid(nsmeta.getUid()))
                                    .collect(Collectors.toList()));

        final Path inValidConfigFile = Files.createTempFile(folder.toPath(), null, null);
        var actual = kubectlService.getClusterServerDetails(inValidConfigFile);

        assertEquals(expected, actual);
        versionApiMock.close();
    }

    @Test
    public void validateGetPods() {
        final V1PodList podStatus = new V1PodList();
        podStatus.addItemsItem(createPod("eric-am-onboarding-service-85748b467-tg2vf", "Running"));
        podStatus.addItemsItem(createPod("eric-lcm-container-registry-registry-0", "Running"));
        podStatus.addItemsItem(createPod("eric-lcm-helm-chart-registry-75789844cb-6d56", "Running"));

        final List<Pod> pods = kubectlService.getPods(podStatus);
        assertThat(pods.size()).isEqualTo(3);
    }

    @Test
    public void validateGetPodsReturnsCorrectDetails() {
        final V1PodList podStatus = createPodListWithDetailedV1Pod();

        final List<Pod> pods = kubectlService.getPods(podStatus);
        assertThat(pods.size()).isEqualTo(1);

        Pod pod = pods.get(0);
        assertDetailedPod(pod);
    }

    private void assertDetailedPod(final Pod pod) {
        assertThat(pod.getName()).isEqualTo("eric-am-onboarding-service-85748b467-tg2vf");
        assertThat(pod.getStatus()).isEqualTo("Running");
        assertThat(pod.getLabels().get("app.kubernetes.io/instance")).isEqualTo("releaseName");
        assertThat(pod.getAnnotations().get("cni.projectcalico.org/podIP")).isEqualTo("192.168.200.217/32");
        assertThat(pod.getUid()).isEqualTo("1b13dec2-f587-40c7-959a-54b4e7f0961f");
        assertThat(pod.getNamespace()).isNull();
        assertThat(pod.getOwnerReferences()).isNull();
    }

    @Test
    public void validateGetPodsThrowsNotFoundException() {
        final V1PodList podStatus = new V1PodList();
        podStatus.addItemsItem(createPod("eric-am-onboarding-service-85748b467-tg2vf", "Running"));
        podStatus.addItemsItem(createPod("eric-lcm-container-registry-registry-0", "Running"));
        podStatus.addItemsItem(createPod("eric-lcm-helm-chart-registry-75789844cb-6d56", ""));

        assertThatThrownBy(() -> kubectlService.getPods(podStatus))
                .isInstanceOf(KubectlAPIException.class).hasMessageContaining("Unable to get the metadata or status from Pod details");
    }

    @Test
    public void validateGetPodsWithoutSpecThrowsException() {
        V1PodList podList = new V1PodList();
        V1Pod pod = createPod("eric-am-onboarding-service-85748b467-tg2vf", "Running");
        pod.setSpec(null);
        podList.addItemsItem(pod);

        assertThatThrownBy(() -> kubectlService.getPods(podList))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining("Unable to get the spec from Pod details");
    }

    @Test
    public void validateScaleDownResourcesSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        List<V1Deployment> deploymentsBeforeScale = createDeploymentList(3);
        List<V1StatefulSet> statefulSetsBeforeScale = createStatefulSetList(1, 1);
        List<V1ReplicaSet> replicaSetsBeforeScale = createReplicaSetList(3, 1);

        List<V1StatefulSet> statefulSetsAfterScale = createStatefulSetList(1, 0);
        List<V1ReplicaSet> replicaSetsAfterScale = createReplicaSetList(3, 0);
        final List<V1Pod> podsBeforeScale = createPodList(3, "Running");

        doReturn(deploymentsBeforeScale).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(statefulSetsBeforeScale).doReturn(statefulSetsBeforeScale)
                .doReturn(statefulSetsAfterScale).when(kubectlService).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(replicaSetsBeforeScale).doReturn(replicaSetsAfterScale).when(kubectlService)
                .getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(podsBeforeScale).doReturn(new ArrayList<>()).when(kubectlService)
                .getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(true).when(kubectlService).scaleResource(any(), any(), anyInt(), any(), any());
        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());

        kubectlService.scaleDownResources(tempClusterConfig, createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                                     RELEASE_NAME, tempClusterConfig,
                                                                                     DEFAULT_APPLICATION_TIMEOUT), IDEMPOTENCY_KEY);

        verify(kubectlService, times(1)).handleSuccessScaleResources(any(InternalScaleInfo.class), any());
        verify(genericMessagingService, times(1)).prepareAndSend(any(WorkflowServiceEventMessage.class), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(2)).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(2)).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(3)).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailTimeout() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        List<V1Deployment> deployments = createDeploymentList(1);
        List<V1StatefulSet> statefulSets = createStatefulSetList(1, 1);
        List<V1ReplicaSet> replicaSets = createReplicaSetList(1, 1);
        final List<V1Pod> pods = createPodList(1, "Running");

        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doReturn(deployments).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(statefulSets).when(kubectlService).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(replicaSets).when(kubectlService).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(pods).when(kubectlService).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(true).when(kubectlService).scaleResource(any(), any(), anyInt(), any(), any());

        kubectlService.scaleDownResources(tempClusterConfig, createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                                     RELEASE_NAME, tempClusterConfig,
                                                                                     DEFAULT_APPLICATION_TIMEOUT), IDEMPOTENCY_KEY);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(InternalScaleInfo.class), anyString(), any());
        verify(genericMessagingService, times(1)).prepareAndSend(any(WorkflowServiceEventMessage.class), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, atLeastOnce()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, atLeastOnce()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, atLeast(2)).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailCannotGetResources() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doThrow(new ApiException(404, "Not Found")).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());

        kubectlService.scaleDownResources(tempClusterConfig, createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                                     RELEASE_NAME, tempClusterConfig,
                                                                                     DEFAULT_APPLICATION_TIMEOUT), IDEMPOTENCY_KEY);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(InternalScaleInfo.class), anyString(), any());
        verify(genericMessagingService, times(1)).prepareAndSend(any(WorkflowServiceEventMessage.class), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, never()).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).scaleResource(any(InternalScaleInfo.class), any(), anyInt(), any(), any());
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailCannotScaleWithAPIException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        List<V1Deployment> deploymentsBeforeScale = createDeploymentList(3);
        var apiException = new ApiException(404, "Not Found");
        InternalScaleInfo internalScaleInfo = createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                      RELEASE_NAME, DEFAULT, DEFAULT_APPLICATION_TIMEOUT);

        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doReturn(deploymentsBeforeScale).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(true).doThrow(apiException)
                .when(kubectlService).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());

        kubectlService.scaleDownResources(tempClusterConfig, internalScaleInfo, IDEMPOTENCY_KEY);

        var expectedMessage = new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DOWNSIZE, WorkflowServiceEventStatus.FAILED,
                                                              String.format(UNABLE_TO_SCALE_DOWN_RESOURCES, internalScaleInfo.getNamespace(), internalScaleInfo.getReleaseName(), apiException.getMessage()),
                                                              RELEASE_NAME);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(), anyString(), any());
        verify(genericMessagingService, times(1)).prepareAndSend(eq(expectedMessage), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, never()).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(2)).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailCannotScaleWithForbiddenAPIException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        var deploymentsBeforeScale = createDeploymentList(3);
        var internalScaleInfo = createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                        RELEASE_NAME, tempClusterConfig, DEFAULT_APPLICATION_TIMEOUT);
        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doReturn(deploymentsBeforeScale).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(true).doThrow(new ApiException(403, "Forbidden"))
                .when(kubectlService).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());

        kubectlService.scaleDownResources(tempClusterConfig, internalScaleInfo, IDEMPOTENCY_KEY);

        var expectedMessage = new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DOWNSIZE, WorkflowServiceEventStatus.FAILED,
                                                              String.format(UNABLE_TO_SCALE_DOWN_RESOURCES, internalScaleInfo.getNamespace(), internalScaleInfo.getReleaseName(), FORBIDDEN_CLUSTER_MESSAGE),
                                                              RELEASE_NAME);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(), anyString(), any());
        verify(genericMessagingService, times(1))
                .prepareAndSend(eq(expectedMessage), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, never()).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(2)).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailCannotScaleWithIOException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        var deploymentsBeforeScale = createDeploymentList(3);
        var ioException = new IOException("IO Exception");
        var internalScaleInfo = createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                        RELEASE_NAME, tempClusterConfig, DEFAULT_APPLICATION_TIMEOUT);

        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doReturn(deploymentsBeforeScale).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        doReturn(true).when(kubectlService).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());
        mockForGetStatefulSetsByNamespaceAndReleaseName(createDetailedStatefulSetList());
        when(kubeClientBuilder.getCoreV1Api(tempClusterConfig)).thenThrow(ioException);

        kubectlService.scaleDownResources(tempClusterConfig, internalScaleInfo, IDEMPOTENCY_KEY);

        var expectedMessage = new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DOWNSIZE, WorkflowServiceEventStatus.FAILED,
                                                              String.format(UNABLE_TO_SCALE_DOWN_RESOURCES, internalScaleInfo.getNamespace(), internalScaleInfo.getReleaseName(), ioException.getMessage()),
                                                              RELEASE_NAME);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(), anyString(), any());
        verify(genericMessagingService, times(1))
                .prepareAndSend(eq(expectedMessage), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, never()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(4)).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleDownResourcesFailNoMetadata() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, DEFAULT)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(DEFAULT));

        List<V1Deployment> deploymentsBeforeScale = new ArrayList<>();
        deploymentsBeforeScale.add(new V1Deployment());
        deploymentsBeforeScale.add(new V1Deployment());
        deploymentsBeforeScale.add(new V1Deployment());

        InternalScaleInfo internalScaleInfo = createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                      RELEASE_NAME, tempClusterConfig,
                                                                      DEFAULT_APPLICATION_TIMEOUT);

        doReturn(true).when(kubectlService).doesNamespaceExist(anyString(), anyString());
        doReturn(deploymentsBeforeScale).when(kubectlService).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());

        kubectlService.scaleDownResources(tempClusterConfig, internalScaleInfo, IDEMPOTENCY_KEY);

        verify(kubectlService, times(1)).handleFailedScaleResources(any(InternalScaleInfo.class), anyString(), any());
        verify(genericMessagingService, times(1)).prepareAndSend(any(WorkflowServiceEventMessage.class), eq(IDEMPOTENCY_KEY));
        verify(kubectlService, never()).getStatefulSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getReplicaSetsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, never()).getPodsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(1)).getDeploymentsByNamespaceAndReleaseName(anyString(), anyString(), any());
        verify(kubectlService, times(1)).scaleResource(eq(internalScaleInfo), any(), anyInt(), any(), any());
        Assert.assertFalse(fileExistInTmp(DEFAULT));
    }

    @Test
    public void validateScaleResourceDeployment() throws ApiException {
        AppsV1Api appsV1Api = Mockito.mock(AppsV1Api.class);
        V1Deployment deployment = createDeployment("eric-adp-gs-testapp", "test-namespace");

        when(PatchUtils.patch(any(), any(), anyString(), any())).thenReturn(new V1Scale());

        boolean isScaleResourceSuccess = kubectlService.scaleResource(createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                                              RELEASE_NAME, DEFAULT,
                                                                                              DEFAULT_APPLICATION_TIMEOUT),
                                                                      deployment, 0, appsV1Api, IDEMPOTENCY_KEY);

        assertTrue(isScaleResourceSuccess);
    }

    @Test
    public void validateScaleResourceStatefulSet() throws ApiException {
        AppsV1Api appsV1Api = Mockito.mock(AppsV1Api.class);
        V1StatefulSet statefulSet = createStatefulSet("eric-pm-server", 0);

        when(PatchUtils.patch(any(), any(), anyString(), any())).thenReturn(new V1Scale());

        boolean isScaleResourceSuccess = kubectlService.scaleResource(createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                                              RELEASE_NAME, DEFAULT,
                                                                                              DEFAULT_APPLICATION_TIMEOUT),
                                                                      statefulSet, 0, appsV1Api, IDEMPOTENCY_KEY);

        assertTrue(isScaleResourceSuccess);
    }

    @Test
    public void validateScaleResourceFailNoMetadata() throws ApiException {
        AppsV1Api appsV1Api = Mockito.mock(AppsV1Api.class);
        V1StatefulSet statefulSet = new V1StatefulSet();

        InternalScaleInfo internalScaleInfo = createInternalScaleInfo(LIFECYCLE_ID, TEST_NAMESPACE,
                                                                      RELEASE_NAME, DEFAULT,
                                                                      DEFAULT_APPLICATION_TIMEOUT);

        boolean isScaleResourceSuccess = kubectlService.scaleResource(internalScaleInfo, statefulSet, 0, appsV1Api, IDEMPOTENCY_KEY);

        verify(kubectlService, times(1)).handleFailedScaleResources(eq(internalScaleInfo), anyString(), any());
        verify(genericMessagingService, times(1)).prepareAndSend(any(WorkflowServiceEventMessage.class), eq(IDEMPOTENCY_KEY));
        assertFalse(isScaleResourceSuccess);
        verify(apiPatchNamespacedStatefulSetScaleRequest, never()).buildCall(any());
    }

    @Test
    public void validateScaleResourceFailNotFoundException() throws ApiException {
        AppsV1Api appsV1Api = Mockito.mock(AppsV1Api.class);
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenThrow(new ApiException(404, "Not Found"));

        V1Deployment deployment = createDeployment("deployment-not-exist", "test-namespace");
        assertThatThrownBy(() -> kubectlService.scaleResource(createInternalScaleInfo(LIFECYCLE_ID, "test-namespace",
                                                                                      RELEASE_NAME, DEFAULT,
                                                                                      DEFAULT_APPLICATION_TIMEOUT), deployment, 0, appsV1Api,
                                                              IDEMPOTENCY_KEY))
                .isInstanceOf(ApiException.class).hasMessageContaining("Not Found");
    }

    @Test
    public void testSuccessfulPatchOfSecret() throws Exception {

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        SecretInfo secretInfo = new SecretInfo();
        secretInfo.setClusterName(DEFAULT);
        secretInfo.setNamespace(DEFAULT);
        secretInfo.setKey("key1");
        secretInfo.setValue("updateValue");
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenReturn(new V1Secret());
        assertDoesNotThrow(() -> kubectlService.patchSecretInNamespace("test-secret", secretInfo));
    }

    @Test
    public void testPatchSecretNotFound() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        SecretInfo secretInfo = new SecretInfo();
        secretInfo.setClusterName(DEFAULT);
        secretInfo.setNamespace(DEFAULT);
        secretInfo.setKey("key1");
        secretInfo.setValue("updateValue");
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenThrow(new ApiException("NOT FOUND"));
        assertThatThrownBy(() -> kubectlService.patchSecretInNamespace("test-secret-evnfm", secretInfo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("The secret test-secret-evnfm is not found in the namespace : default in the cluster : default");
    }

    @Test
    public void testPatchSecretBadRequest() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        SecretInfo secretInfo = new SecretInfo();
        secretInfo.setClusterName(DEFAULT);
        secretInfo.setNamespace(DEFAULT);
        secretInfo.setKey("key1///");
        secretInfo.setValue("updateValue");
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenThrow(new ApiException(500, null, "BAD REQUEST"));
        assertThatThrownBy(() -> kubectlService.patchSecretInNamespace("test-secret-vdu", secretInfo))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining("Issue patching the secret test-secret-vdu with the following ERROR BAD REQUEST");
    }

    @Test
    public void testSuccessfulPatchOfSecretThrowsKubectlAPIException() throws Exception {
        var ioException = new IOException("Api Exception");
        var secretInfo = new SecretInfo();
        secretInfo.setClusterName(DEFAULT);

        when(kubeClientBuilder.getCoreV1Api(secretInfo.getClusterName())).thenThrow(ioException);

        var expectedMessage = String.format(PATCH_REQUEST_ERROR, "test-secret-vdu", ioException.getMessage());
        Executable executable = () -> kubectlService.patchSecretInNamespace("test-secret-vdu", secretInfo);
        var kubectlException = assertThrows(KubectlAPIException.class, executable);
        assertEquals(expectedMessage, kubectlException.getMessage());
    }

    @Test
    public void testDeleteNamespaceSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(false);

        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.COMPLETED, "Deleted namespace successfully.",
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));

        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));

    }

    @Test
    public void testDeleteAlreadyDeletedNamespaceFailed() throws Exception{
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        String errorMessage = String.format("Namespace %s does not exist or already deleted", TEST_NAMESPACE);

        doReturn(false).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.COMPLETED, errorMessage, RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteEvnfmNamespaceFailed() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(EVNFM_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(true);
        Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> kubectlService.deleteNamespace(
                buildDeleteNamespace(EVNFM_NAMESPACE,RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                tempClusterConfig));

        Assert.assertEquals("Requested namespace contains EVNFM unable to delete please specify a different namespace", exception.getMessage());
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteKubeNamespaceFailed() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq("kube-system"), eq(tempClusterConfig));

        Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> kubectlService.deleteNamespace(
                buildDeleteNamespace("kube-system", RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                tempClusterConfig));

        Assert.assertEquals("Requested namespace is restricted and cannot be deleted please specify a different namespace", exception.getMessage());
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteEvnfmNamespaceOnAnotherClusterSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(EVNFM_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(false);

        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());

        kubectlService.deleteNamespace(buildDeleteNamespace(EVNFM_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.COMPLETED, "Deleted namespace successfully.",
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientThrowsExceptionSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        JsonSyntaxException expectedException = new JsonSyntaxException("Expected a string but was BEGIN_OBJECT",
                                                                        new IllegalStateException());

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenThrow(expectedException);

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.COMPLETED, "Deleted namespace successfully.",
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientThrowsExceptionFailure() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        JsonSyntaxException expectedException = new JsonSyntaxException("Expected a string but was BEGIN_OBJECT");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenThrow(expectedException);

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                                                String.format(UNABLE_TO_DELETE_NAMESPACE, TEST_NAMESPACE, expectedException.getMessage()),
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientThrowsForbiddenApiExceptionFailed() throws IOException, ApiException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));
        var expectedException = new ApiException(HttpStatus.FORBIDDEN.value(), "FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenThrow(expectedException);

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.FAILED, FORBIDDEN_CLUSTER_MESSAGE, RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientThrowsApiExceptionFailed() throws IOException, ApiException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));
        var expectedException = new ApiException("FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenThrow(expectedException);

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.FAILED, String
                                                        .format(UNABLE_TO_DELETE_NAMESPACE, TEST_NAMESPACE, expectedException.getMessage()),
                                                RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientThrowsIOExceptionFailed() throws IOException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));
        var expectedException = new IOException("FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(tempClusterConfig));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenThrow(expectedException);

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);

        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                                                                         new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                                                                         WorkflowServiceEventStatus.FAILED, String
                                                                                                                 .format(UNABLE_TO_DELETE_NAMESPACE, TEST_NAMESPACE, expectedException.getMessage()), RELEASE_NAME)),
                                                                 eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientVerifyNamespaceDeletedFailed() throws IOException, ApiException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));
        var expectedException = new ApiException("FAILED");

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(CLUSTER_CONFIG));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());
        mockApiListNamespaceRequest();
        when(apiListNamespaceRequest.execute()).thenThrow(expectedException);
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE,
                                                WorkflowServiceEventStatus.FAILED, expectedException.getMessage(), RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceKubeClientVerifyNamespaceDeletedSuccess() throws IOException, ApiException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));
        var v1NamespaceList = new V1NamespaceList().addItemsItem(new V1Namespace().metadata(new V1ObjectMeta().name(TEST_NAMESPACE)));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(CLUSTER_CONFIG));
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());
        mockApiListNamespaceRequest();
        when(apiListNamespaceRequest.execute()).thenReturn(v1NamespaceList);
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       tempClusterConfig);
        verifyNoInteractions(genericMessagingService);
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDeleteNamespaceCheckNamespaceFailedCausedByIOException() throws IOException, ApiException {
        var ioException = new IOException("IO Exception");
        var v1NamespaceList = new V1NamespaceList().addItemsItem(new V1Namespace().metadata(new V1ObjectMeta().name(TEST_NAMESPACE)));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(CLUSTER_CONFIG));
        when(evnfmNamespaceService.checkEvnfmNamespace(TEST_NAMESPACE, CLUSTER_CONFIG)).thenThrow(ioException);
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());
        mockApiListNamespaceRequest();
        when(apiListNamespaceRequest.execute()).thenReturn(v1NamespaceList);
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       CLUSTER_CONFIG);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                                                String.format(UNABLE_TO_DELETE_NAMESPACE, TEST_NAMESPACE, ioException.getMessage()), RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
    }

    @Test
    public void testDeleteNamespaceCheckNamespaceFailedCausedByApiException() throws IOException, ApiException {
        var apiException = new ApiException("Api Exception");
        var v1NamespaceList = new V1NamespaceList().addItemsItem(new V1Namespace().metadata(new V1ObjectMeta().name(TEST_NAMESPACE)));

        doReturn(true).when(kubectlService).doesNamespaceExist(eq(TEST_NAMESPACE), eq(CLUSTER_CONFIG));
        when(evnfmNamespaceService.checkEvnfmNamespace(TEST_NAMESPACE, CLUSTER_CONFIG)).thenThrow(apiException);
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListNamespaceRequest.execute()).thenReturn(createNamespaceList(DEFAULT));
        when(apiDeleteNamespaceRequest.execute()).thenReturn(new V1Status());
        mockApiListNamespaceRequest();
        when(apiListNamespaceRequest.execute()).thenReturn(v1NamespaceList);
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());

        kubectlService.deleteNamespace(buildDeleteNamespace(TEST_NAMESPACE, RELEASE_NAME, DEFAULT_APPLICATION_TIMEOUT, LIFECYCLE_ID, IDEMPOTENCY_KEY),
                                       CLUSTER_CONFIG);
        verify(genericMessagingService, times(1)).prepareAndSend(eq(
                new WorkflowServiceEventMessage(LIFECYCLE_ID, WorkflowServiceEventType.DELETE_NAMESPACE, WorkflowServiceEventStatus.FAILED,
                                                String.format(UNABLE_TO_DELETE_NAMESPACE, TEST_NAMESPACE, apiException.getMessage()), RELEASE_NAME)), eq(IDEMPOTENCY_KEY));
    }

    @Test
    public void testGetPodStatusByReleaseNameSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListPodForAllNamespacesRequest.execute()).thenReturn(createPodListWithDetailedV1Pod());

        PodStatusResponse actualResult = kubectlService
                .getPodStatusByReleaseName(RELEASE_NAME, tempClusterConfig);

        assertThat(actualResult.getPods()).hasSize(1);
        assertDetailedPod(actualResult.getPods().get(0));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetPodStatusByReleaseNameSuccessReturnsEmpty() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        when(apiListPodForAllNamespacesRequest.execute()).thenReturn(new V1PodList());

        PodStatusResponse actual = kubectlService
                .getPodStatusByReleaseName(RELEASE_NAME, tempClusterConfig);

        assertThat(actual.getPods()).isEmpty();
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetPodStatusInfoByReleaseNames() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        when(apiListPodForAllNamespacesRequest.execute()).thenReturn(createPodListWithDetailedV1Pod());

        List<PodStatusResponse> actual = kubectlService.getPodStatusByReleaseNames(List.of(RELEASE_NAME), tempClusterConfig);

        assertThat(actual).hasSize(1);
        PodStatusResponse actualPodStatusResponse = actual.get(0);
        assertThat(actualPodStatusResponse.getReleaseName()).isEqualTo(RELEASE_NAME);
        assertDetailedPod(actualPodStatusResponse.getPods().get(0));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetPodStatusInfoByReleaseNamesFailureIOException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        var ioException = new IOException("IO Exception");
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenThrow(ioException);
        var expectedMessage = String.format("Issue getting the podStatusByReleaseName with the following ERROR %s", ioException.getMessage());
        var releaseNames = List.of(RELEASE_NAME);

        var kubectlException = assertThrows(KubectlAPIException.class,
                                            () -> kubectlService.getPodStatusByReleaseNames(releaseNames, tempClusterConfig));
        assertEquals(expectedMessage, kubectlException.getMessage());
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetPodStatusInfoByReleaseNamesFailureAPIException() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        ApiException apiException = new ApiException("API Exception");
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListPodForAllNamespacesRequest.execute()).thenThrow(apiException);
        var expectedMessage = String.format("Issue getting the podStatusByReleaseName with the following ERROR %s", apiException.getMessage());
        var releaseNames = List.of(RELEASE_NAME);

        KubectlAPIException kubectlException = assertThrows(KubectlAPIException.class,
                                                            () -> kubectlService.getPodStatusByReleaseNames(releaseNames, tempClusterConfig));
        assertEquals(expectedMessage, kubectlException.getMessage());
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetKubernetesResourceStatusInfoByReleaseNames() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        List<KubernetesResource> expectedDeployments = createExpectedDetailedKubernetesResource(DEPLOYMENT_KIND);
        List<KubernetesResource> expectedStatefulsets = createExpectedDetailedKubernetesResource(STATEFULSET_KIND);

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(apiListPodForAllNamespacesRequest.execute()).thenReturn(createPodListWithDetailedV1Pod());
        mockForGetDeploymentsByReleaseNameSuccess(createDetailedDeploymentList("eric-pm-server"));
        mockForGetStatefulSetsByReleaseNameSuccess(createDetailedStatefulSetList());

        List<KubernetesResourceInfo> actual = kubectlService
                .getKubernetesResourceStatusInfoByReleaseNames(List.of(RELEASE_NAME), tempClusterConfig);

        assertThat(actual).hasSize(1);
        KubernetesResourceInfo actualKubernetesResourceInfo = actual.get(0);
        assertDetailedPod(actualKubernetesResourceInfo.getPods().get(0));
        assertThat(actualKubernetesResourceInfo.getDeployments()).containsAll(expectedDeployments);
        assertThat(actualKubernetesResourceInfo.getStatefulSets()).containsAll(expectedStatefulsets);
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testGetDeploymentsByReleaseNameSuccess() throws Exception {
        V1DeploymentList v1DeploymentList = createDetailedDeploymentList("eric-pm-server");
        List<KubernetesResource> expected = createExpectedDetailedKubernetesResource(DEPLOYMENT_KIND);

        mockForGetDeploymentsByReleaseNameSuccess(v1DeploymentList);

        List<KubernetesResource> actual = kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    public void testGetDeploymentsByReleaseNameEmptySuccess() throws Exception {
        mockForGetDeploymentsByReleaseNameSuccess(new V1DeploymentList());

        List<KubernetesResource> actual = kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertThat(actual).isEmpty();
    }

    @Test
    public void testGetDeploymentsByReleaseNameShouldThrowException() throws Exception {
        mockForGetDeploymentsByReleaseNameFailed(new ApiException("Failed to retrieve Deployments"));

        assertThatThrownBy(() -> kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining("Failed to retrieve Deployment List information for release");
    }

    @Test
    public void testGetDeploymentsByReleaseNameWithoutMetadataShouldThrowException() throws Exception {
        V1DeploymentList v1DeploymentList = createDetailedDeploymentList("eric-pm-server");
        v1DeploymentList.getItems().get(0).setMetadata(null);

        mockForGetDeploymentsByReleaseNameSuccess(v1DeploymentList);

        assertThatThrownBy(() -> kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining(String.format(FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
    }

    @Test
    public void testGetDeploymentsByReleaseNameWithoutMetadataNameShouldThrowException() throws Exception {
        V1DeploymentList v1DeploymentList = createDetailedDeploymentList("");

        mockForGetDeploymentsByReleaseNameSuccess(v1DeploymentList);

        assertThatThrownBy(() -> kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining(String.format(FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
    }

    @Test
    public void testGetDeploymentsByReleaseNameWithoutStatusShouldThrowException() throws Exception {
        V1DeploymentList v1DeploymentList = createDetailedDeploymentList("eric-pm-server");
        v1DeploymentList.getItems().get(0).setStatus(null);

        mockForGetDeploymentsByReleaseNameSuccess(v1DeploymentList);

        assertThatThrownBy(() -> kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining(String.format(FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE, DEPLOYMENT_KIND));
    }

    @Test
    public void testGetStatefulSetsByReleaseNameSuccess() throws Exception {
        V1StatefulSetList v1StatefulSetList = createDetailedStatefulSetList();
        List<KubernetesResource> expected = createExpectedDetailedKubernetesResource(STATEFULSET_KIND);

        mockForGetStatefulSetsByReleaseNameSuccess(v1StatefulSetList);

        List<KubernetesResource> actual = kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    public void testGetStatefulSetsByReleaseNameEmptySuccess() throws Exception {
        mockForGetStatefulSetsByReleaseNameSuccess(new V1StatefulSetList());

        List<KubernetesResource> actual = kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertThat(actual).isEmpty();
    }

    @Test
    public void testGetStatefulSetsByReleaseNameWithoutLabelsSuccess() throws Exception {
        List<KubernetesResource> expected = createExpectedDetailedKubernetesResource(STATEFULSET_KIND);
        expected.forEach(kubernetesResource -> kubernetesResource.setInstanceLabel(null));

        V1StatefulSetList v1StatefulSetList = createDetailedStatefulSetList();
        v1StatefulSetList.getItems().forEach(v1StatefulSet -> Optional.ofNullable(v1StatefulSet.getMetadata())
                .ifPresent(v1ObjectMeta -> v1ObjectMeta.setLabels(null)));
        mockForGetStatefulSetsByReleaseNameSuccess(v1StatefulSetList);

        List<KubernetesResource> actual = kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertThat(actual).containsAll(expected);
    }

    @Test
    public void testGetDeploymentsByReleaseNameWithoutLabelsSuccess() throws Exception {
        List<KubernetesResource> expected = createExpectedDetailedKubernetesResource(DEPLOYMENT_KIND);
        expected.forEach(kubernetesResource -> kubernetesResource.setInstanceLabel(null));

        V1DeploymentList v1DeploymentList = createDetailedDeploymentList("eric-pm-server");
        v1DeploymentList.getItems().forEach(v1Deployment -> Optional.ofNullable(v1Deployment.getMetadata())
                .ifPresent(v1ObjectMeta -> v1ObjectMeta.setLabels(null)));
        mockForGetDeploymentsByReleaseNameSuccess(v1DeploymentList);

        List<KubernetesResource> actual = kubectlService.getDeploymentsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG);

        assertThat(actual).containsAll(expected);
    }

    @Test
    public void testGetStatefulSetsByReleaseNameShouldThrowException() throws Exception {
        mockForGetStatefulSetsByReleaseNameFailed(new ApiException("Failed to retrieve StatefulSets"));

        assertThatThrownBy(() -> kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining("Failed to retrieve StatefulSet List information for release");
    }

    @Test
    public void testGetStatefulSetsByReleaseNameWithoutMetadataShouldThrowException() throws Exception {
        V1StatefulSetList v1StatefulSetList = createDetailedStatefulSetList();
        v1StatefulSetList.getItems().get(0).setMetadata(null);

        mockForGetStatefulSetsByReleaseNameSuccess(v1StatefulSetList);

        assertThatThrownBy(() -> kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining(String.format(FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE, STATEFULSET_KIND));
    }

    @Test
    public void testGetStatefulSetsByReleaseNameWithoutStatusShouldThrowException() throws Exception {
        V1StatefulSetList v1StatefulSetList = createDetailedStatefulSetList();
        v1StatefulSetList.getItems().get(0).setStatus(null);

        mockForGetStatefulSetsByReleaseNameSuccess(v1StatefulSetList);

        assertThatThrownBy(() -> kubectlService.getStatefulSetsByReleaseName(RELEASE_NAME, CLUSTER_CONFIG))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContaining(String.format(FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE, STATEFULSET_KIND));
    }


    @Test
    public void testGetAllSecretsInTheNamespaceEmptySuccess() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());
        when(apiListNamespacedSecretRequest.execute()).thenReturn(new V1SecretList());

        Secrets secrets = kubectlService.getAllSecretInTheNamespace(TEST_NAMESPACE, CLUSTER_CONFIG, "100");
        assertEquals(0, secrets.getAllSecrets().size());
    }

    @Test
    public void testGetAllSecretsInTheNamespaceSuccess() throws Exception {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);

        V1SecretList v1SecretList = createV1SecretList("secretName");
        v1SecretList.addItemsItem(new V1Secret());

        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());
        when(apiListNamespacedSecretRequest.execute()).thenReturn(v1SecretList);

        Secrets secrets = kubectlService.getAllSecretInTheNamespace(TEST_NAMESPACE, tempClusterConfig, "100");

        assertEquals(1, secrets.getAllSecrets().size());
        SecretAttribute secretAttribute = secrets.getAllSecrets().get("secretName");
        assertNotNull(secretAttribute);

        assertNotNull(secretAttribute.getData().get("namespace"));
        assertEquals(DEFAULT, secretAttribute.getData().get("namespace"));

        assertNotNull(secretAttribute.getData().get("token"));
        assertEquals("12345", secretAttribute.getData().get("token"));
        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDoesNamespaceExistSuccess() throws IOException, ApiException {
        when(kubeClientBuilder.getCoreV1Api(DEFAULT)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(new V1Namespace());
        assertTrue(kubectlService.doesNamespaceExist(NAMESPACE, DEFAULT));
    }

    @Test
    public void testDoesNamespaceExistThrowsIOException() throws IOException {
        String tempClusterConfig = String.valueOf(Files.createFile(Paths.get(pathToTempDirectory, CLUSTER_CONFIG)).toAbsolutePath());
        Assert.assertTrue(fileExistInTmp(CLUSTER_CONFIG));

        when(kubeClientBuilder.getCoreV1Api(tempClusterConfig)).thenThrow(new IOException("IO Exception"));
        assertFalse(kubectlService.doesNamespaceExist(NAMESPACE, tempClusterConfig));

        Assert.assertFalse(fileExistInTmp(CLUSTER_CONFIG));
    }

    @Test
    public void testDoesNamespaceExistThrowsAPIException() throws IOException, ApiException {
        when(kubeClientBuilder.getCoreV1Api(DEFAULT)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(new ApiException());
        assertFalse(kubectlService.doesNamespaceExist(NAMESPACE, DEFAULT));
    }

    @Test
    public void testIsNamespaceUsedForEvnfmDeploymentSuccess() throws IOException, ApiException {
        String clusterName = "cluster002";
        String namespace = "evnfm-ns";

        when(evnfmNamespaceService.checkEvnfmNamespace(namespace, clusterName)).thenReturn(Boolean.TRUE);

        final NamespaceValidationResponse actual = kubectlService.isNamespaceUsedForEvnfmDeployment(namespace, clusterName);

        assertThat(actual.isEvnfmAndClusterNamespace()).isTrue();
    }

    @Test
    public void testIsNamespaceUsedForEvnfmDeploymentThrowShouldException() throws IOException, ApiException {
        String clusterName = "cluster002";
        String namespace = "evnfm-ns";

        doThrow(new ApiException(CLUSTER_IS_UNREACHABLE_ERROR_MESSAGE)).when(evnfmNamespaceService).checkEvnfmNamespace(namespace, clusterName);

        assertThatThrownBy(() -> kubectlService.isNamespaceUsedForEvnfmDeployment(namespace, clusterName))
                .isInstanceOf(KubectlAPIException.class)
                .hasMessageContainingAll(namespace, CLUSTER_IS_UNREACHABLE_ERROR_MESSAGE);
    }

    private V1SecretList createV1SecretList(String... secretNames) {
        V1SecretList v1SecretList = new V1SecretList();

        for (String secretName : secretNames) {
            V1Secret secret = new V1Secret();

            V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
            v1ObjectMeta.setName(secretName);

            Map<String, byte[]> data = new HashMap<>();
            data.put("namespace", DEFAULT.getBytes());
            data.put("token", "12345".getBytes());

            secret.setData(data);
            secret.setMetadata(v1ObjectMeta);

            v1SecretList.addItemsItem(secret);
        }
        return v1SecretList;
    }

    private V1PodList createPodListWithDetailedV1Pod() {
        final V1PodList podStatus = new V1PodList();
        Map<String, String> labels = new TreeMap<>();
        labels.put("app.kubernetes.io/instance", "releaseName");
        Map<String, String> annotations = new TreeMap<>();
        annotations.put("cni.projectcalico.org/podIP", "192.168.200.217/32");
        V1PodSpec v1PodSpec = new V1PodSpec();
        v1PodSpec.setNodeName("node-10-158-164-65");
        String uid = "1b13dec2-f587-40c7-959a-54b4e7f0961f";
        V1Pod v1Pod = createPod("eric-am-onboarding-service-85748b467-tg2vf", "Running");
        v1Pod.getMetadata().setUid(uid);
        v1Pod.getMetadata().setLabels(labels);
        v1Pod.getMetadata().setAnnotations(annotations);
        v1Pod.setSpec(v1PodSpec);
        podStatus.addItemsItem(v1Pod);
        return podStatus;
    }

    private V1DeploymentList createDetailedDeploymentList(String name) {
        V1DeploymentList v1DeploymentList = new V1DeploymentList();

        V1Deployment v1Deployment = new V1Deployment();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setUid("63e7fe0e-022d-4266-b67e-c0734032ad4c");
        v1ObjectMeta.setName(name);
        v1ObjectMeta.setNamespace("basic-lcm-multi-ns");
        v1ObjectMeta.putLabelsItem("app.kubernetes.io/instance", "releaseName");

        List<V1OwnerReference> v1OwnerReferences = new ArrayList<>();
        V1OwnerReference v1OwnerReference = new V1OwnerReference();
        v1OwnerReference.setName("eric-pm-server-owner-reference");
        v1OwnerReferences.add(v1OwnerReference);
        v1ObjectMeta.setOwnerReferences(v1OwnerReferences);

        V1DeploymentStatus v1DeploymentStatus = new V1DeploymentStatus();
        v1DeploymentStatus.setAvailableReplicas(1);

        v1Deployment.setMetadata(v1ObjectMeta);
        v1Deployment.setStatus(v1DeploymentStatus);

        V1DeploymentSpec v1DeploymentSpec = new V1DeploymentSpec();
        v1DeploymentSpec.setReplicas(1);
        v1Deployment.setSpec(v1DeploymentSpec);

        v1DeploymentList.addItemsItem(v1Deployment);

        return v1DeploymentList;
    }

    private V1StatefulSetList createDetailedStatefulSetList() {
        V1StatefulSetList v1StatefulSetList = new V1StatefulSetList();

        V1StatefulSet v1StatefulSet = new V1StatefulSet();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setUid("63e7fe0e-022d-4266-b67e-c0734032ad4c");
        v1ObjectMeta.setName("eric-pm-server");
        v1ObjectMeta.setNamespace("basic-lcm-multi-ns");
        v1ObjectMeta.putLabelsItem("app.kubernetes.io/instance", "releaseName");

        List<V1OwnerReference> v1OwnerReferences = new ArrayList<>();
        V1OwnerReference v1OwnerReference = new V1OwnerReference();
        v1OwnerReference.setName("eric-pm-server-owner-reference");
        v1OwnerReferences.add(v1OwnerReference);
        v1ObjectMeta.setOwnerReferences(v1OwnerReferences);

        V1StatefulSetStatus v1StatefulSetStatus = new V1StatefulSetStatus();
        v1StatefulSetStatus.setAvailableReplicas(1);

        v1StatefulSet.setMetadata(v1ObjectMeta);
        v1StatefulSet.setStatus(v1StatefulSetStatus);

        V1StatefulSetSpec v1StatefulSetSpec = new V1StatefulSetSpec();
        v1StatefulSetSpec.setReplicas(1);

        v1StatefulSet.setSpec(v1StatefulSetSpec);

        v1StatefulSetList.addItemsItem(v1StatefulSet);

        return v1StatefulSetList;
    }

    private List<KubernetesResource> createExpectedDetailedKubernetesResource(String kind) {
        KubernetesResource statefulSet = new KubernetesResource();
        statefulSet.setUid("63e7fe0e-022d-4266-b67e-c0734032ad4c");
        statefulSet.setName("eric-pm-server");
        statefulSet.setKind(kind);
        statefulSet.setNamespace("basic-lcm-multi-ns");
        statefulSet.setReplicas(1);
        statefulSet.setAvailableReplicas(1);

        List<V1OwnerReference> v1OwnerReferences = new ArrayList<>();
        V1OwnerReference v1OwnerReference = new V1OwnerReference();
        v1OwnerReference.setName("eric-pm-server-owner-reference");
        v1OwnerReferences.add(v1OwnerReference);
        statefulSet.setOwnerReferences(v1OwnerReferences);

        statefulSet.setInstanceLabel("releaseName");

        return List.of(statefulSet);
    }

    private void mockForGetStatefulSetsByNamespaceAndReleaseName(V1StatefulSetList v1StatefulSetList) throws Exception {
        when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        when(apiListNamespacedStatefulSetRequest.execute()).thenReturn(v1StatefulSetList);
    }

    private void mockForGetStatefulSetsByReleaseNameSuccess(V1StatefulSetList v1StatefulSetList) throws Exception {
        when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        when(apiListStatefulSetForAllNamespacesRequest.execute()).thenReturn(v1StatefulSetList);
    }

    private void mockForGetStatefulSetsByReleaseNameFailed(Throwable throwable) throws Exception {
        when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        when(apiListStatefulSetForAllNamespacesRequest.execute()).thenThrow(throwable);
    }

    private void mockForGetDeploymentsByReleaseNameSuccess(V1DeploymentList v1DeploymentList) throws Exception {
        when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        when(apiListDeploymentForAllNamespacesRequest.execute()).thenReturn(v1DeploymentList);
    }

    private void mockForGetDeploymentsByReleaseNameFailed(Throwable throwable) throws Exception {
        when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        when(apiListDeploymentForAllNamespacesRequest.execute()).thenThrow(throwable);
    }

    private V1NamespaceList createNamespaceList(String... names) {
        V1NamespaceList namespaceList = new V1NamespaceList();
        for (String name : names) {
            V1Namespace namespace = new V1Namespace();
            V1ObjectMeta metadata = new V1ObjectMeta();
            metadata.setName(name);
            namespace.setMetadata(metadata);
            namespaceList.addItemsItem(namespace);
        }
        return namespaceList;
    }

    private V1Pod createPod(String name, String phase) {
        V1Pod v1Pod = new V1Pod();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(name);
        v1Pod.setMetadata(v1ObjectMeta);

        V1PodStatus v1PodStatus = new V1PodStatus();
        v1PodStatus.setPhase(phase);
        v1Pod.setStatus(v1PodStatus);

        V1PodSpec v1PodSpec = new V1PodSpec();
        v1PodSpec.setHostname("hostname");
        v1Pod.setSpec(v1PodSpec);

        return v1Pod;
    }

    private V1Pod createPodWithoutHostName() {
        V1Pod v1Pod = new V1Pod();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("eric-am-onboarding-service-85748b467-tg2vf");

        v1Pod.setMetadata(v1ObjectMeta);

        V1PodStatus v1PodStatus = new V1PodStatus();
        v1PodStatus.setPhase("Running");
        v1Pod.setStatus(v1PodStatus);

        v1Pod.setSpec(new V1PodSpec());

        return v1Pod;
    }

    private List<V1Pod> createPodList(int length, String phase) {
        List<V1Pod> podList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            podList.add(createPod("eric-pod-" + i, phase));
        }
        return podList;
    }

    private V1Deployment createDeployment(String name, String namespace) {
        V1Deployment v1Deployment = new V1Deployment();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(name);
        v1ObjectMeta.setNamespace(namespace);
        v1Deployment.setMetadata(v1ObjectMeta);

        return v1Deployment;
    }

    private List<V1Deployment> createDeploymentList(int length) {
        List<V1Deployment> deploymentList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            deploymentList.add(createDeployment("eric-deployment-" + i, "test-namespace"));
        }
        return deploymentList;
    }

    private V1StatefulSet createStatefulSet(String name, int replicas) {
        V1StatefulSet v1StatefulSet = new V1StatefulSet();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(name);
        v1StatefulSet.setMetadata(v1ObjectMeta);

        V1StatefulSetSpec spec = new V1StatefulSetSpec();
        spec.setReplicas(replicas);
        v1StatefulSet.setSpec(spec);

        V1StatefulSetStatus statefulSetStatus = new V1StatefulSetStatus();
        statefulSetStatus.setCurrentReplicas(replicas);
        statefulSetStatus.setReadyReplicas(replicas);
        v1StatefulSet.setStatus(statefulSetStatus);

        return v1StatefulSet;
    }

    private List<V1StatefulSet> createStatefulSetList(int length, int replicas) {
        List<V1StatefulSet> statefulSetList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            statefulSetList.add(createStatefulSet("eric-statefulset-" + i, replicas));
        }
        return statefulSetList;
    }

    private V1ReplicaSet createReplicaSet(String name, int replicas) {
        V1ReplicaSet v1ReplicaSet = new V1ReplicaSet();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(name);
        v1ReplicaSet.setMetadata(v1ObjectMeta);

        V1ReplicaSetSpec spec = new V1ReplicaSetSpec();
        spec.setReplicas(replicas);
        v1ReplicaSet.setSpec(spec);

        V1ReplicaSetStatus status = new V1ReplicaSetStatus();
        status.setAvailableReplicas(replicas);
        status.setReadyReplicas(replicas);
        v1ReplicaSet.setStatus(status);

        return v1ReplicaSet;
    }

    private List<V1ReplicaSet> createReplicaSetList(int length, int replicas) {
        List<V1ReplicaSet> replicaSetList = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            replicaSetList.add(createReplicaSet("eric-replicaset-" + i, replicas));
        }

        return replicaSetList;
    }

    private InternalScaleInfo createInternalScaleInfo(String operationId, String namespace, String releaseName, String clusterName, String timeout) {
        InternalScaleInfo internalScaleInfo = new InternalScaleInfo();

        internalScaleInfo.setNamespace(namespace);
        internalScaleInfo.setReleaseName(releaseName);
        internalScaleInfo.setClusterName(clusterName);
        internalScaleInfo.setApplicationTimeOut(timeout);
        internalScaleInfo.setLifecycleOperationId(operationId);

        return internalScaleInfo;
    }

    private V1Namespace buildNamespaceWithUid(String name, String uid) {
        V1Namespace v1Namespace = new V1Namespace();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(name);
        meta.setUid(uid);
        v1Namespace.setMetadata(meta);
        return v1Namespace;
    }

    protected boolean fileExistInTmp(String fileName) {
        File file = new File(pathToTempDirectory + File.separator + fileName);
        return file.exists();
    }

    private AsyncDeletePvcsRequestDetails buildDeletePvc(String namespace, String release, String timeout, String lifecycleOperationId,
                                                         String idempotencyKey, List<String> labels) {
        AsyncDeletePvcsRequestDetails asyncDeletePvcsRequestDetails = new AsyncDeletePvcsRequestDetails();
        asyncDeletePvcsRequestDetails.setNamespace(namespace);
        asyncDeletePvcsRequestDetails.setReleaseName(release);
        asyncDeletePvcsRequestDetails.setTimeout(timeout);
        asyncDeletePvcsRequestDetails.setLifecycleOperationId(lifecycleOperationId);
        asyncDeletePvcsRequestDetails.setIdempotencyKey(idempotencyKey);
        asyncDeletePvcsRequestDetails.setLabels(labels);

        return asyncDeletePvcsRequestDetails;
    }

    private AsyncDeleteNamespaceRequestDetails buildDeleteNamespace(String namespace, String release, String timeout, String lifecycleOperationId,
                                                                    String idempotencyKey) {
        AsyncDeleteNamespaceRequestDetails asyncDeleteNamespaceRequestDetails = new AsyncDeleteNamespaceRequestDetails();
        asyncDeleteNamespaceRequestDetails.setNamespace(namespace);
        asyncDeleteNamespaceRequestDetails.setReleaseName(release);
        asyncDeleteNamespaceRequestDetails.setTimeout(timeout);
        asyncDeleteNamespaceRequestDetails.setLifecycleOperationId(lifecycleOperationId);
        asyncDeleteNamespaceRequestDetails.setIdempotencyKey(idempotencyKey);

        return asyncDeleteNamespaceRequestDetails;
    }

    private void mockApiDeleteCollectionNamespacedPersistentVolumeClaimRequest() {
        when(coreV1Api.deleteCollectionNamespacedPersistentVolumeClaim(any())).thenReturn(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest);
        when(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest.labelSelector(any())).thenReturn(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest);
        when(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest.timeoutSeconds(any())).thenReturn(apiDeleteCollectionNamespacedPersistentVolumeClaimRequest);
    }

    private void mockApiReadNamespaceRequest() {
        when(coreV1Api.readNamespace(any())).thenReturn(apiReadNamespaceRequest);
    }

    private void mockApiListNamespaceRequest() {
        when(coreV1Api.listNamespace()).thenReturn(apiListNamespaceRequest);
        when(apiListNamespaceRequest.pretty(any())).thenReturn(apiListNamespaceRequest);
        when(apiListNamespaceRequest.allowWatchBookmarks(any())).thenReturn(apiListNamespaceRequest);
        when(apiListNamespaceRequest.limit(any())).thenReturn(apiListNamespaceRequest);
        when(apiListNamespaceRequest.watch(any())).thenReturn(apiListNamespaceRequest);
    }

    private void mockApiDeleteNamespaceRequest() {
        when(coreV1Api.deleteNamespace(any())).thenReturn(apiDeleteNamespaceRequest);
        when(apiDeleteNamespaceRequest.propagationPolicy(any())).thenReturn(apiDeleteNamespaceRequest);
    }

    private void mockApiListPodForAllNamespacesRequest() {
        when(coreV1Api.listPodForAllNamespaces()).thenReturn(apiListPodForAllNamespacesRequest);
        when(apiListPodForAllNamespacesRequest.labelSelector(any())).thenReturn(apiListPodForAllNamespacesRequest);
        when(apiListPodForAllNamespacesRequest.timeoutSeconds(any())).thenReturn(apiListPodForAllNamespacesRequest);
    }

    private void mockApiListNamespacedSecretRequest() {
        when(coreV1Api.listNamespacedSecret(any())).thenReturn(apiListNamespacedSecretRequest);
        when(apiListNamespacedSecretRequest.timeoutSeconds(any())).thenReturn(apiListNamespacedSecretRequest);
    }

    private void mockApiListNamespacedStatefulSetRequest() {
        when(appsV1Api.listNamespacedStatefulSet(any())).thenReturn(apiListNamespacedStatefulSetRequest);
        when(apiListNamespacedStatefulSetRequest.labelSelector(any())).thenReturn(apiListNamespacedStatefulSetRequest);
    }

    private void mockApiListStatefulSetForAllNamespacesRequest() {
        when(appsV1Api.listStatefulSetForAllNamespaces()).thenReturn(apiListStatefulSetForAllNamespacesRequest);
        when(apiListStatefulSetForAllNamespacesRequest.labelSelector(any())).thenReturn(apiListStatefulSetForAllNamespacesRequest);
        when(apiListStatefulSetForAllNamespacesRequest.timeoutSeconds(any())).thenReturn(apiListStatefulSetForAllNamespacesRequest);
    }

    private void mockApiListDeploymentForAllNamespacesRequest() {
        when(appsV1Api.listDeploymentForAllNamespaces()).thenReturn(apiListDeploymentForAllNamespacesRequest);
        when(apiListDeploymentForAllNamespacesRequest.labelSelector(any())).thenReturn(apiListDeploymentForAllNamespacesRequest);
        when(apiListDeploymentForAllNamespacesRequest.timeoutSeconds(any())).thenReturn(apiListDeploymentForAllNamespacesRequest);
    }
}
