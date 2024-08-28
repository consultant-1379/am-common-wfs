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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.TestUtils.readTypedDataFromFile;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFICATION_ANNOTATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_DEPLOYED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.exceptions.InvalidAnnotationException;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.amcommonwfs.models.ContainerDetails;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1ContainerStateRunning;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        VerificationHelper.class,
        TemporaryFileServiceImpl.class,
        ClusterFileUtils.class,
        RetryProperties.class,
        RetryTemplateConfig.class,
        KubeClientBuilder.class })
public class VerificationHelperTest {
    private static final String DUMMY_NAME_SPACE = "dummy_name_space";
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_CLUSTER_NAME = "test01.config";
    private static final String DUMMY_CLUSTER_CONFIG = "dummy_cluster_config.config";

    @Spy
    private ExecutionImpl execution = new ExecutionImpl();

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private AppsV1Api appsV1Api;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    @MockBean
    private CamundaFileRepository camundaFileRepository;

    @SpyBean
    @Qualifier("kubectlApiRetryTemplate")
    private RetryTemplate kubectlApiRetryTemplate;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private KubeClientBuilder kubeClientBuilder;

    @InjectMocks
    @Autowired
    private VerificationHelper verificationHelper;

    @Mock
    private AppsV1Api.APIlistNamespacedDeploymentRequest apiListNamespacedDeploymentRequest;

    @Mock
    private AppsV1Api.APIlistNamespacedStatefulSetRequest apiListNamespacedStatefulSetRequest;

    @Mock
    private CoreV1Api.APIlistNamespacedPodRequest apiListNamespacedPodRequest;

    @BeforeEach
    public void setup() {
        createExecutionImpl();
        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        try {
            when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
            when(kubeClientBuilder.getAppsV1Api(anyString())).thenReturn(appsV1Api);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mockApiListNamespacedRequests();
    }

    @Test
    public void testCollectContainersFromPodWithoutAnnotation_CheckDefaults() {

        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(getMockedPodDetails());
        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isTrue();
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_SingleQuoted() {

        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'},{'containerName':'container-2','state':'Running',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.TERMINATED);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isFalse();
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_DoubleQuoted() {

        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{\"containerName\":\"container-1\",\"state\":\"Waiting\",\"ready\":\"true\"},{\"containerName\":\"container-2\","
                + "\"state\":\"Running\",\"ready\":\"true\"}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.WAITING);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isTrue();
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_IgnoreContainersNotInPod() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'},{'containerName':'container-3','state':'Running',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.size()).isEqualTo(1);
        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.TERMINATED);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_InvalidContainerState() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'},{'containerName':'container-3','state':'Succeeded',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Invalid Container state - Succeeded");
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_caseInsensitiveState() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'TERMINATED','ready':'true'},{'containerName':'container-2','state':'RuNnInG',"
                + "'ready':'false'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.TERMINATED);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isFalse();
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_NoContainerName() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'state':'Terminated','ready':'True'},{'containerName':'container-2','state':'Running','ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Missing creator property 'containerName'");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_EmptyContainerName() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'','state':'Terminated','ready':'True'},{'containerName':'container-2','state':'Running','ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Mandatory fields are missing/empty. Please provide valid inputs for containerName, state and ready");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_NoReadyField() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated'},{'containerName':'container-2','state':'Running','ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Missing creator property 'ready'");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_EmptyReadyField() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':''},{'containerName':'container-2','state':'Running',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Null value for creator property 'ready'");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_NoContainerState() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','ready':'False'},{'containerName':'container-2','state':'Running','ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Missing creator property 'state'");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidAnnotation_EmptyContainerState() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'','ready':'True'},{'containerName':'container-2','state':'Running',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Invalid Container state - ");
    }

    @Test
    public void testCollectContainersFromPodWithInvalidJsonAnnotation() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'},{'containerName':'container-3','state':'Running',"
                + "'ready':'False'}";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        assertThatThrownBy(() -> verificationHelper.collectContainersFromPod().apply(v1Pod)).isInstanceOf(InvalidAnnotationException.class)
                .hasMessageContaining("Unexpected end-of-input: expected close marker for Array");
    }

    @Test
    public void testCollectContainersFromPodWithValidAnnotation_IgnoreContainersNotInAnnotation() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.TERMINATED);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();
    }

    @Test
    public void testCollectContainersFromPodWithEmptyAnnotation() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isTrue();
    }

    @Test
    public void testCollectContainersFromPodWithOnlyWrongContainersInAnnotation() {
        V1Pod v1Pod = getMockedPodDetails();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-3','state':'Terminated','ready':'True'},{'containerName':'container-4','state':'Running',"
                + "'ready':'False'}]";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        v1Pod.getMetadata().setAnnotations(verificationAnnotations);
        List<ContainerDetails> containerDetailsList = verificationHelper.collectContainersFromPod().apply(v1Pod);

        assertThat(containerDetailsList.get(0).getContainerName()).isEqualTo("container-1");
        assertThat(containerDetailsList.get(0).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(0).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(0).isReady()).isTrue();

        assertThat(containerDetailsList.get(1).getContainerName()).isEqualTo("container-2");
        assertThat(containerDetailsList.get(1).getPodName()).isEqualTo("pod-1");
        assertThat(containerDetailsList.get(1).getState()).isEqualTo(ContainerDetails.ContainerState.RUNNING);
        assertThat(containerDetailsList.get(1).isReady()).isTrue();
    }

    private V1Pod getMockedPodDetails() {
        V1Pod v1Pod = new V1Pod();

        List<V1Container> containers = getMockedContainersList();
        V1PodSpec v1PodSpec = new V1PodSpec();

        v1PodSpec.setContainers(containers);
        v1Pod.setSpec(v1PodSpec);

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("pod-1");

        v1Pod.setMetadata(v1ObjectMeta);

        return v1Pod;
    }

    private List<V1Container> getMockedContainersList() {
        List<V1Container> containersList = new ArrayList<>();

        V1Container v1Container = new V1Container();
        v1Container.setName("container-1");
        containersList.add(v1Container);

        V1Container v1Container1 = new V1Container();
        v1Container1.setName("container-2");
        containersList.add(v1Container1);

        return containersList;
    }

    @Test
    public void checkPodReadyFromPodStatus() {
        V1PodCondition condition = createCondition("True");
        assertThat(verificationHelper.checkPodReadyConditionFromPodStatus(condition)).isTrue();
    }

    @Test
    public void checkPodNotReadyFromPodStatus() {
        V1PodCondition condition = createCondition("False");
        assertThat(verificationHelper.checkPodReadyConditionFromPodStatus(condition)).isFalse();
    }

    @Test
    public void checkPodCompletedFromPodStatus() {
        V1PodCondition condition = createCondition("False");
        condition.setReason("PodCompleted");
        assertThat(verificationHelper.checkPodReadyConditionFromPodStatus(condition)).isTrue();
    }

    @Test
    public void iterateThroughPods() {
        final V1PodList releasePods = new V1PodList();
        final V1Pod readyPod = createPod("True", null);
        releasePods.addItemsItem(readyPod);
        final V1Pod notReadyPod = createPod("False", null);
        releasePods.addItemsItem(notReadyPod);
        final V1Pod jobCompleted = createPod("False", "PodCompleted");
        releasePods.addItemsItem(jobCompleted);
        int readyPods = verificationHelper.podsInReadyState(releasePods.getItems());
        assertThat(readyPods).isEqualTo(2);
    }

    @Test
    public void verifyPodsInNullStatus() {
        final V1PodList releasePods = new V1PodList();
        final V1Pod v1Pod = new V1Pod();
        v1Pod.setStatus(null);
        releasePods.addItemsItem(v1Pod);
        int readyPods = verificationHelper.podsInReadyState(releasePods.getItems());
        assertThat(readyPods).isEqualTo(0);
    }

    @Test
    public void verifyPodsWithNullConditions() {
        final V1PodList releasePods = new V1PodList();
        final V1Pod v1Pod = new V1Pod();
        final V1PodStatus v1PodStatus = new V1PodStatus();
        v1PodStatus.setConditions(null);
        v1Pod.setStatus(v1PodStatus);
        releasePods.addItemsItem(v1Pod);
        int readyPods = verificationHelper.podsInReadyState(releasePods.getItems());
        assertThat(readyPods).isEqualTo(0);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInDeployment() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithNotAllPodsCreated.json", V1DeploymentList.class);
        createMockedKubectlObjects(deployments, null, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedStatefulSetRequest, never()).execute();
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInDeploymentWithEmptySpec() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        deployments.getItems().stream().findFirst().get().setSpec(null);

        createMockedKubectlObjects(deployments, null, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInDeploymentWithEmptySpecReplicas() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        deployments.getItems().stream().findFirst().get().getSpec().setReplicas(null);

        createMockedKubectlObjects(deployments, null, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInDeploymentWithEmptyStatus() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        deployments.getItems().stream().findFirst().get().setStatus(null);

        createMockedKubectlObjects(deployments, null, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInDeploymentWithEmptyStatusUpdatedReplicas() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        deployments.getItems().stream().findFirst().get().getStatus().setUpdatedReplicas(null);

        createMockedKubectlObjects(deployments, null, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInStatefulSet() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithNotAllPodsCreated.json", V1StatefulSetList.class);
        createMockedKubectlObjects(deployments, statefulSetList, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInStatefulSetWithEmptySpec() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        statefulSetList.getItems().stream().findFirst().get().setSpec(null);

        createMockedKubectlObjects(deployments, statefulSetList, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInStatefulSetWithEmptySpecReplicas() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        statefulSetList.getItems().stream().findFirst().get().getSpec().setReplicas(null);

        createMockedKubectlObjects(deployments, statefulSetList, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInStatefulSetWithEmptyStatus() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        statefulSetList.getItems().stream().findFirst().get().setStatus(null);

        createMockedKubectlObjects(deployments, statefulSetList, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotAllPodsInStatefulSetWithEmptyStatusUpdatedReplicas() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        statefulSetList.getItems().stream().findFirst().get().getStatus().setUpdatedReplicas(null);

        createMockedKubectlObjects(deployments, statefulSetList, null);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(apiListNamespacedPodRequest, never()).execute();
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithNotReadyPods() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        V1PodList pods = readKubernetesObject("podListNotWithNotReady.json", V1PodList.class);
        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithErrorFromKubectl() throws ApiException {
        //given
        String errorMsg = "some dummy text";

        when(apiListNamespacedDeploymentRequest.execute()).thenThrow(new ApiException(errorMsg));

        //when and then
        assertThatThrownBy(() -> verificationHelper.verifyApplicationDeployed(execution)).isInstanceOf(BpmnError.class);
        verify(execution).setVariable(ERROR_MESSAGE, new ApiException(errorMsg).getMessage());
    }

    @Test
    public void testVerifyApplicationDeployedWithEvictedPodsSuccess() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        V1PodList pods = readKubernetesObject("podListWithReady.json", V1PodList.class);
        pods.addItemsItem(createEvictedPod());

        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithZeroDesiredReplicasSuccess() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithNotAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithNotAllPodsCreated.json", V1StatefulSetList.class);
        V1PodList pods = readKubernetesObject("podListWithReady.json", V1PodList.class);

        final V1Deployment firstDeployment = deployments.getItems().get(0);
        firstDeployment.getSpec().setReplicas(0);
        firstDeployment.getStatus().setUpdatedReplicas(null);

        final V1StatefulSet firstStatefulSet = statefulSetList.getItems().get(0);
        firstStatefulSet.getSpec().setReplicas(0);
        firstStatefulSet.getStatus().setUpdatedReplicas(null);

        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithEmptyDeploymentsSuccess() throws ApiException {
        //given
        V1DeploymentList deployments = new V1DeploymentList();
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        V1PodList pods = readKubernetesObject("podListWithReady.json", V1PodList.class);
        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedWithEmptyStatefulSetListSuccess() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = new V1StatefulSetList();
        V1PodList pods = readKubernetesObject("podListWithReady.json", V1PodList.class);
        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployed(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, false);
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedUsingAnnotationWithEmptyKubectlObjects() throws ApiException {
        //given
        execution.setVariable(SKIP_JOB_VERIFICATION, false);
        createMockedKubectlObjects(new V1DeploymentList(), new V1StatefulSetList(), new V1PodList());

        //when
        verificationHelper.verifyApplicationDeployedUsingAnnotation(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedUsingAnnotationWithoutSkipJobVerification() throws ApiException {
        //given
        execution.setVariable(SKIP_JOB_VERIFICATION, false);
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        V1PodList v1PodList = createV1PodList();
        createMockedKubectlObjects(deployments, statefulSetList, v1PodList);

        //when
        verificationHelper.verifyApplicationDeployedUsingAnnotation(execution);

        //then
        verify(execution, times(2)).setVariable(APP_DEPLOYED, false);
        verify(execution, never()).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedUsingAnnotationWithSkipJobVerification() throws ApiException {
        //given
        V1PodList v1PodList = createV1PodList();
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        createMockedKubectlObjects(deployments, statefulSetList, v1PodList);

        //when
        verificationHelper.verifyApplicationDeployedUsingAnnotation(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testVerifyApplicationDeployedUsingAnnotationWithApiException() throws ApiException {
        //given
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        execution.setVariable(SKIP_JOB_VERIFICATION, false);
        String errorMsg = "Dummy_ApiException";

        when(apiListNamespacedDeploymentRequest.execute()).thenReturn(deployments);
        when(apiListNamespacedStatefulSetRequest.execute()).thenReturn(statefulSetList);
        when(apiListNamespacedPodRequest.execute()).thenThrow(new ApiException(errorMsg));

        //when and then
        assertThatThrownBy(() -> verificationHelper.verifyApplicationDeployedUsingAnnotation(execution))
                .isInstanceOf(BpmnError.class);
        verify(execution).setVariable(ERROR_MESSAGE, new ApiException(errorMsg).getMessage());
    }

    @Test
    public void testVerifyApplicationDeployedUsingAnnotationWithEmptyContainersDetails() throws ApiException {
        //given
        execution.setVariable(SKIP_JOB_VERIFICATION, false);
        V1PodList pods = readKubernetesObject("podListWithReady.json", V1PodList.class);
        V1DeploymentList deployments = readKubernetesObject("deploymentListWithAllPodsCreated.json", V1DeploymentList.class);
        V1StatefulSetList statefulSetList = readKubernetesObject("statefulSetListWithAllPodsCreated.json", V1StatefulSetList.class);
        pods.getItems().stream().forEach(pod -> pod.getSpec().setContainers(new ArrayList<>()));

        createMockedKubectlObjects(deployments, statefulSetList, pods);

        //when
        verificationHelper.verifyApplicationDeployedUsingAnnotation(execution);

        //then
        verify(execution).setVariable(APP_DEPLOYED, true);
    }

    @Test
    public void testGetPodContainersBasedOnStateWithoutSkipJobVerification() {
        V1PodList v1PodList1 = createV1PodList();
        boolean skipJobVerification = false;

        Map<String, Map<String, List<ContainerDetails>>> dummyMap =
                verificationHelper.getPodContainersBasedOnState(v1PodList1, skipJobVerification);

        assertThat(dummyMap).isNotEmpty();
        assertThat(dummyMap).containsKey("pod-1");
        assertThat(dummyMap).containsKey("pod-2");
    }

    @Test
    public void testGetPodContainersBasedOnStateWithSkipJobVerification() {
        V1PodList v1PodList1 = createV1PodList();
        boolean skipJobVerification = true;

        Map<String, Map<String, List<ContainerDetails>>> dummyMap =
                verificationHelper.getPodContainersBasedOnState(v1PodList1, skipJobVerification);

        assertThat(dummyMap).isNotEmpty();
        assertThat(dummyMap).containsKey("pod-1");
        assertThat(dummyMap).doesNotContainKey("pod-2");
    }

    private void createExecutionImpl() {
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(NAMESPACE, DUMMY_NAME_SPACE);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
        execution.setVariable(APP_TIMEOUT, LocalDateTime.now().plusSeconds(100).toEpochSecond(ZoneOffset.UTC));
        execution.setVariable(SKIP_JOB_VERIFICATION, true);
    }

    public void createMockedKubectlObjects(V1DeploymentList deploymentList,
                                           V1StatefulSetList statefulSetList,
                                           V1PodList releasePods) throws ApiException {
        when(apiListNamespacedDeploymentRequest.execute()).thenReturn(deploymentList);
        when(apiListNamespacedStatefulSetRequest.execute()).thenReturn(statefulSetList);
        when(apiListNamespacedPodRequest.execute()).thenReturn(releasePods);
    }

    private V1PodList createV1PodList() {
        V1PodList v1PodList = new V1PodList();

        V1Pod firstDummyPod = getMockedPodDetails();
        V1PodStatus v1PodStatus = new V1PodStatus();
        List<V1ContainerStatus> v1ContainerStatusList = List.of(createV1ContainerStatus("container-1"),
                                                                createV1ContainerStatus("container-2"));
        v1PodStatus.setContainerStatuses(v1ContainerStatusList);
        firstDummyPod.setStatus(v1PodStatus);

        V1Pod secondDummyPod = getMockedPodDetails();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("pod-2");
        V1OwnerReference v1OwnerReference = new V1OwnerReference();
        v1OwnerReference.setController(true);
        v1OwnerReference.setKind("Job");
        List<V1OwnerReference> v1OwnerReferenceList = List.of(v1OwnerReference);
        v1ObjectMeta.setOwnerReferences(v1OwnerReferenceList);
        secondDummyPod.setMetadata(v1ObjectMeta);

        v1PodList.addItemsItem(firstDummyPod);
        v1PodList.addItemsItem(secondDummyPod);

        return v1PodList;
    }

    private V1ContainerStatus createV1ContainerStatus(String containerName) {
        V1ContainerState state = new V1ContainerState();
        V1ContainerStateRunning v1ContainerStateRunning = new V1ContainerStateRunning();
        state.setRunning(v1ContainerStateRunning);

        V1ContainerStatus v1ContainerStatus = new V1ContainerStatus();
        v1ContainerStatus.setName(containerName);
        v1ContainerStatus.setState(state);
        v1ContainerStatus.setReady(true);
        return v1ContainerStatus;
    }

    private V1Pod createPod(final String ready, final String reason) {
        final V1Pod readyPod = new V1Pod();
        final V1PodStatus readyStatus = new V1PodStatus();
        final V1PodCondition readyCondition = createCondition(ready);
        if (reason != null) {
            readyCondition.setReason(reason);
        }
        readyStatus.addConditionsItem(readyCondition);
        readyPod.setStatus(readyStatus);
        return readyPod;
    }

    private V1PodCondition createCondition(final String aTrue) {
        final V1PodCondition readyCondition = new V1PodCondition();
        readyCondition.setType("Ready");
        readyCondition.setStatus(aTrue);
        return readyCondition;
    }

    private V1Pod createEvictedPod() {
        V1Pod pod = new V1Pod();
        V1PodStatus podStatus = new V1PodStatus();
        podStatus.setReason("Evicted");
        pod.setStatus(podStatus);
        return pod;
    }

    private <T> T readKubernetesObject(final String fileName, final Class<T> valueType) {
        return readTypedDataFromFile(this.getClass(), fileName, valueType);
    }

    private void mockApiListNamespacedRequests() {
        when(appsV1Api.listNamespacedDeployment(any())).thenReturn(apiListNamespacedDeploymentRequest);
        when(apiListNamespacedDeploymentRequest.labelSelector(any())).thenReturn(apiListNamespacedDeploymentRequest);
        when(apiListNamespacedDeploymentRequest.timeoutSeconds(any())).thenReturn(apiListNamespacedDeploymentRequest);
        when(appsV1Api.listNamespacedStatefulSet(any())).thenReturn(apiListNamespacedStatefulSetRequest);
        when(apiListNamespacedStatefulSetRequest.labelSelector(any())).thenReturn(apiListNamespacedStatefulSetRequest);
        when(apiListNamespacedStatefulSetRequest.timeoutSeconds(any())).thenReturn(apiListNamespacedStatefulSetRequest);
        when(coreV1Api.listNamespacedPod(any())).thenReturn(apiListNamespacedPodRequest);
        when(apiListNamespacedPodRequest.labelSelector(any())).thenReturn(apiListNamespacedPodRequest);
        when(apiListNamespacedPodRequest.timeoutSeconds(any())).thenReturn(apiListNamespacedPodRequest);

    }
}
