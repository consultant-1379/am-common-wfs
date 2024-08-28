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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.registry.secret.CreateSecret.REGISTRY_SECRET;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;

@SpringBootTest(classes = { CreateSecret.class, TemporaryFileServiceImpl.class, ClusterFileUtils.class,
        RetryProperties.class,  RetryTemplateConfig.class })
@TestPropertySource(properties = { "docker.registry.url=testRegistryUrl", "docker.registry.username=vnfm",
        "docker.registry.password=Er1csson0n!" })
public class CreateSecretTest {
    public static final String DOCKER_CONFIG_VALUE = "{\"auths\":{\"testRegistryUrl\":{\"username\":\"vnfm\",\"password\":\"Er1csson0n!\","
            + "\"auth\":\"dm5mbTpFcjFjc3NvbjBuIQ==\"}}}";
    private static final String DUMMY_NAME_SPACE = "dummy_name_space";
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_CLUSTER_NAME = "test01.config";
    private static final String DUMMY_CLUSTER_CONFIG = "dummy_cluster_config.config";

    @Autowired
    private CreateSecret createSecret;

    @MockBean
    private DockerConfigUtils dockerConfigUtils;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    ClusterFileUtils clusterFileUtils;

    @Mock
    private CoreV1Api coreV1Api;

    private ExecutionImpl execution = new ExecutionImpl();

    @MockBean
    private KubeClientBuilder kubeClientBuilder;

    @Mock
    private CoreV1Api.APIcreateNamespacedSecretRequest apiCreateNamespacedSecretRequest;

    @BeforeEach
    public void init() throws IOException {
        createRulesForMockito();
        putVariablesForExecution();
        when(coreV1Api.createNamespacedSecret(any(), any())).thenReturn(apiCreateNamespacedSecretRequest);
    }

    @Test
    public void testDockerSecret() throws JsonProcessingException {
        when(dockerConfigUtils.constructDockerConfigJson()).thenReturn(DOCKER_CONFIG_VALUE);
        V1Secret v1Secret = createSecret.buildDockerRegistrySecret("docker-registry");

        assertThat(v1Secret.getType()).isEqualTo(REGISTRY_SECRET);
        assertThat(v1Secret.getStringData()).containsValue(DOCKER_CONFIG_VALUE);
    }

    @Test
    public void testExecuteWithApiError409() throws ApiException, IOException {
        ApiException apiException = new ApiException(409, null, null, "AlreadyExists");

        when(apiCreateNamespacedSecretRequest.execute()).thenThrow(apiException);

        assertThatNoException().isThrownBy(() -> createSecret.execute(execution));
    }

    @Test
    public void testExecuteWithApiError() throws ApiException, IOException {
        ApiException apiException = new ApiException(0, null, null, "Dummy_api_exception");

        when(apiCreateNamespacedSecretRequest.execute()).thenThrow(apiException);

        assertThatThrownBy(() -> createSecret.execute(execution)).isInstanceOf(BpmnError.class);
    }
    @Test
    public void testExecute() throws ApiException, IOException {
        createSecret.execute(execution);

        verify(apiCreateNamespacedSecretRequest).execute();
    }

    @Test
    public void testExecuteWithIOException() throws Exception {
        putVariablesForExecution();
        IOException ioException = new IOException("Dummy_IOException");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenThrow(ioException);

        assertThatThrownBy(() -> createSecret.execute(execution)).isInstanceOf(BpmnError.class);
    }

    private void createRulesForMockito() throws IOException {
        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenReturn(coreV1Api);
    }

    private void putVariablesForExecution() {
        execution.setVariable(NAMESPACE, DUMMY_NAME_SPACE);
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
    }
}
