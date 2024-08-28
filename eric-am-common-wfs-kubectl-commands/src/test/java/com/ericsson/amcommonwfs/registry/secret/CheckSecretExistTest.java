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
package com.ericsson.amcommonwfs.registry.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.MessageConstants.FORBIDDEN_CLUSTER_MESSAGE;
import static com.ericsson.amcommonwfs.registry.secret.CreateSecret.DOCKERCONFIGJSON;
import static com.ericsson.amcommonwfs.registry.secret.CreateSecretTest.DOCKER_CONFIG_VALUE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.GLOBAL_REGISTRY_SECRET_PRESENT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_IO_EXCEPTION;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.PatchUtils;

@SpringBootTest(classes = {CheckSecretExist.class, TemporaryFileServiceImpl.class,
        ClusterFileUtils.class, RetryProperties.class, RetryTemplateConfig.class})
@TestPropertySource(properties = {
        "docker.registry.url=testRegistryUrl",
        "containerRegistry.global.registry.pullSecret=regcred",
        "autoConfigureDocker.enabled=true"
})
public class CheckSecretExistTest {
    public static final String DOCKER_CONFIG_VALUE1 = "{\"auths\":{\"testRegistryUrl\":{\"username\":\"vnfm\",\"password\":\"DefaultP123!\","
            + "\"auth\":\"dm5mbTpEZWZhdWx0UDEyMyE=\"}}}";
    private static final String DUMMY_NAME_SPACE = "dummy_name_space";
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_CLUSTER_NAME = "test01.config";
    private static final String DUMMY_CLUSTER_CONFIG = "dummy_cluster_config.config";

    private ExecutionImpl execution;

    @MockBean
    private DockerConfigUtils dockerConfigUtils;

    @Autowired
    private CheckSecretExist checkSecretExist;

    @MockBean
    private CoreV1Api coreV1Api;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    @Mock
    private V1Namespace v1Namespace;

    @Mock
    private V1ObjectMeta v1ObjectMeta;

    @MockBean
    private KubeClientBuilder kubeClientBuilder;

    @Mock
    private CoreV1Api.APIreadNamespacedSecretRequest apiReadNamespacedSecretRequest;

    @Mock
    private CoreV1Api.APIdeleteNamespacedSecretRequest apiDeleteNamespacedSecretRequest;

    @Mock
    private CoreV1Api.APIreadNamespaceRequest apiReadNamespaceRequest;

    @Mock
    private CoreV1Api.APIcreateNamespaceRequest apiCreateNamespaceRequest;

    @BeforeAll
    public static void setUp() {
        mockStatic(PatchUtils.class);
    }

    @BeforeEach
    public void init() {
        execution = new ExecutionImpl();
        execution.setVariable(NAMESPACE, DUMMY_NAME_SPACE);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);

        when(coreV1Api.readNamespacedSecret(any(), any())).thenReturn(apiReadNamespacedSecretRequest);
        when(coreV1Api.readNamespace(any())).thenReturn(apiReadNamespaceRequest);
        when(coreV1Api.deleteNamespacedSecret(any(), any())).thenReturn(apiDeleteNamespacedSecretRequest);
        when(coreV1Api.createNamespace(any())).thenReturn(apiCreateNamespaceRequest);
    }

    @AfterEach
    public void tearDown() {
        reset(PatchUtils.class);
        ReflectionTestUtils.setField(checkSecretExist, "autoConfigureDockerEnabled", true);
    }

    @Test
    public void testSecretAlreadyExistsInNamespace() throws ApiException, JsonProcessingException {
        Map<String, byte[]> secretData = new HashMap<>();
        secretData.put(DOCKERCONFIGJSON, DOCKER_CONFIG_VALUE.getBytes());

        V1Secret v1Secret = new V1Secret();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("eric-sec-tls-crd");
        v1Secret.setMetadata(v1ObjectMeta);
        v1Secret.setData(secretData);
        execution.setVariable(RELEASE_NAME, "eric-sec-tls-crd");
        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(dockerConfigUtils.constructDockerConfigJson()).thenReturn(DOCKER_CONFIG_VALUE);
        checkSecretExist.checkRegistrySecretInNamespace("test-ns", coreV1Api, execution);
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(true);
    }

    @Test
    public void testSecretDoesNotAlreadyExistsInNamespace() throws ApiException {
        V1Secret v1Secret = new V1Secret();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("regcred");
        v1Secret.setMetadata(v1ObjectMeta);
        execution.setVariable(RELEASE_NAME, "regcred");
        when(apiReadNamespacedSecretRequest.execute())
                .thenThrow(new ApiException(404, "Not found"));
        checkSecretExist.checkRegistrySecretInNamespace("test-ns", coreV1Api, execution);
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(false);
    }

    @Test
    public void testDockerSecretDataNotFound() throws ApiException {
        V1Secret v1Secret = new V1Secret();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("eric-sec-tls-crd");
        v1Secret.setMetadata(v1ObjectMeta);
        execution.setVariable(RELEASE_NAME, "eric-sec-tls-crd");
        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(apiDeleteNamespacedSecretRequest.execute()).thenReturn(null);

        checkSecretExist.checkRegistrySecretInNamespace("test-ns", coreV1Api, execution);
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(false);
    }


    @Test
    public void testSecretContentsNotMatching() throws ApiException, JsonProcessingException {
        Map<String, byte[]> secretData = new HashMap<>();
        V1Secret v1Secret = new V1Secret();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("eric-sec-tls-crd");
        v1Secret.setMetadata(v1ObjectMeta);
        secretData.put(DOCKERCONFIGJSON, DOCKER_CONFIG_VALUE.getBytes());
        v1Secret.setData(secretData);
        execution.setVariable(RELEASE_NAME, "eric-sec-tls-crd");
        execution.setVariable(CLUSTER_NAME, "default");
        execution.setVariable(NAMESPACE, "test-ns");

        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(dockerConfigUtils.constructDockerConfigJson()).thenReturn(DOCKER_CONFIG_VALUE1);
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenReturn(v1Secret);

        checkSecretExist.checkRegistrySecretInNamespace("test-ns", coreV1Api, execution);
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(true);
    }

    @Test
    public void testSecretErrorWhenPatching() throws ApiException, JsonProcessingException {
        Map<String, byte[]> secretData = new HashMap<>();
        V1Secret v1Secret = new V1Secret();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName("eric-sec-tls-crd");
        v1Secret.setMetadata(v1ObjectMeta);
        secretData.put(DOCKERCONFIGJSON, DOCKER_CONFIG_VALUE.getBytes());
        v1Secret.setData(secretData);
        execution.setVariable(RELEASE_NAME, "eric-sec-tls-crd");
        execution.setVariable(CLUSTER_NAME, "default");
        execution.setVariable(NAMESPACE, "test-ns");

        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(dockerConfigUtils.constructDockerConfigJson()).thenReturn(DOCKER_CONFIG_VALUE1);
        when(PatchUtils.patch(any(), any(), anyString(), any())).thenThrow(new ApiException("Unable to patch the secret : eric-sec-tls-crd"));

        assertThatThrownBy(() -> checkSecretExist.checkRegistrySecretInNamespace("test-ns", coreV1Api, execution))
                .hasMessageContaining("Issue patching the secret eric-sec-tls-crd with the following ERROR")
                .isInstanceOf(BpmnError.class);
    }

    @Test
    public void testExecuteWithIOException() throws Exception {
        IOException ioException = new IOException("Dummy_IOException");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenThrow(ioException);

        assertThatThrownBy(() -> checkSecretExist.execute(execution)).isInstanceOf(BpmnError.class);
    }

    @Test
    public void testExecute() throws Exception {
        ReflectionTestUtils.setField(checkSecretExist, "autoConfigureDockerEnabled", false);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiCreateNamespaceRequest.execute()).thenReturn(v1Namespace);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        checkSecretExist.execute(execution);
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(true);
    }

    @Test
    public void testExecuteWithApiError409() throws Exception {
        ReflectionTestUtils.setField(checkSecretExist, "autoConfigureDockerEnabled", false);
        var apiException = new ApiException(409, null, null, "AlreadyExists");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);

        assertThatNoException().isThrownBy(() -> checkSecretExist.execute(execution));
        assertThat(execution.getVariable(GLOBAL_REGISTRY_SECRET_PRESENT)).isEqualTo(true);
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithMessageFromResponse() throws Exception {
        var exceptionMessage = "response message";
        var jsonObject = new JSONObject();
        jsonObject.put("message", exceptionMessage);
        var apiException = new ApiException(404, "ERROR", new HashMap<>(), jsonObject.toString());

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(apiException);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(exceptionMessage, bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithExceptionMessageResponseWithoutMessage() throws Exception {
        var exceptionMessage = "response message";
        var jsonObject = new JSONObject();
        jsonObject.put("notMessage", exceptionMessage);
        var apiException = new ApiException(404, "ERROR", new HashMap<>(), jsonObject.toString());

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(apiException);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(apiException.getMessage(), bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithExceptionMessageWhenResponseAbsent() throws Exception {
        var apiException = new ApiException(404, "ERROR");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(apiException);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(apiException.getMessage(), bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithExceptionMessageWhenErrorThrown() throws Exception {
        var apiException = new ApiException(404, "ERROR", new HashMap<>(), "illegal message");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(apiException);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(apiException.getMessage(), bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithForbiddenClusterMessage() throws Exception {
        var apiException = new ApiException(403, "ERROR", new HashMap<>(), "illegal message");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiCreateNamespaceRequest.execute()).thenThrow(apiException);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn("dummy_string");

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(FORBIDDEN_CLUSTER_MESSAGE, bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithIOErrorCode() throws Exception {
        Map<String, byte[]> secretData = new HashMap<>();
        secretData.put(DOCKERCONFIGJSON, DOCKER_CONFIG_VALUE.getBytes());

        var v1Secret = new V1Secret();
        v1Secret.setMetadata(v1ObjectMeta);
        v1Secret.setData(secretData);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(apiCreateNamespaceRequest.execute()).thenReturn(v1Namespace);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn(DUMMY_RELEASE_NAME);
        when(dockerConfigUtils.constructDockerConfigJson()).thenThrow(new JsonMappingException(null, "ERROR"));

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals("ERROR", bpmnError.getMessage());
        Assertions.assertEquals(BPMN_IO_EXCEPTION.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithKubectlFailureCode() throws Exception {
        Map<String, byte[]> secretData = new HashMap<>();
        secretData.put(DOCKERCONFIGJSON, DOCKER_CONFIG_VALUE.getBytes());

        var v1Secret = new V1Secret();
        v1Secret.setMetadata(v1ObjectMeta);
        v1Secret.setData(secretData);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiCreateNamespaceRequest.execute()).thenReturn(v1Namespace);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn(DUMMY_RELEASE_NAME);
        when(apiReadNamespacedSecretRequest.execute()).thenThrow(new ApiException("ERROR"));

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        assertThat(bpmnError.getMessage()).contains("ERROR");
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithKubectlFailureCodeWhenDeleteSecretFails() throws Exception {
        var v1Secret = new V1Secret();
        v1Secret.setMetadata(v1ObjectMeta);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiReadNamespacedSecretRequest.execute()).thenReturn(v1Secret);
        when(apiCreateNamespaceRequest.execute()).thenReturn(v1Namespace);
        when(v1Namespace.getMetadata()).thenReturn(v1ObjectMeta);
        when(v1ObjectMeta.getName()).thenReturn(DUMMY_RELEASE_NAME);
        when(apiDeleteNamespacedSecretRequest.execute()).thenThrow(new ApiException("ERROR"));

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        assertThat( bpmnError.getMessage()).contains("ERROR");
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithKubectlFailureCodeWhenCreateNamespace() throws Exception {
        var v1Secret = new V1Secret();
        v1Secret.setMetadata(v1ObjectMeta);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenThrow(new ApiException("ERROR"));

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        assertThat(bpmnError.getMessage()).contains("ERROR");
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }

    @Test
    public void testExecuteThrowsBpmnErrorWithSpecificMessageWhenCreateNamespace() throws Exception {
        var v1Secret = new V1Secret();
        v1Secret.setMetadata(v1ObjectMeta);

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
        when(apiReadNamespaceRequest.execute()).thenReturn(null);
        when(apiCreateNamespaceRequest.execute()).thenReturn(null);

        Executable executable = () -> checkSecretExist.execute(execution);
        var bpmnError = Assertions.assertThrows(BpmnError.class, executable);
        Assertions.assertEquals(String.format("Failed to create namespace %s", DUMMY_NAME_SPACE), bpmnError.getMessage());
        Assertions.assertEquals(BPMN_KUBECTL_FAILURE.getErrorCodeAsString(), bpmnError.getErrorCode());
    }
}
