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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;

@SpringBootTest(classes = { DeleteSecret.class, TemporaryFileServiceImpl.class, ClusterFileUtils.class,
        RetryProperties.class,  RetryTemplateConfig.class })
public class DeleteSecretTest {
    private static final String DUMMY_NAME_SPACE = "dummy_name_space";
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_CLUSTER_NAME = "test01.config";
    private static final String DUMMY_CLUSTER_CONFIG = "dummy_cluster_config.config";

    @Autowired
    private DeleteSecret deleteSecret;

    @MockBean
    ClusterFileUtils clusterFileUtils;

    @Mock
    private CoreV1Api coreV1Api;

    @MockBean
    private KubeClientBuilder kubeClientBuilder;

    @Mock
    private CoreV1Api.APIdeleteNamespacedSecretRequest apiDeleteNamespacedSecretRequest;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testExecuteWithApiError() throws Exception {
        createRulesForMockito();
        putVariablesForExecution();

        when(coreV1Api.deleteNamespacedSecret(anyString(), anyString())).thenReturn(apiDeleteNamespacedSecretRequest);
        when(apiDeleteNamespacedSecretRequest.execute()).thenThrow(ApiException.class);

        assertThatNoException().isThrownBy(() -> deleteSecret.execute(execution));
    }

    @Test
    public void testExecuteWithIOException() throws Exception {
        putVariablesForExecution();
        IOException ioException = new IOException("Dummy_IOException");

        when(clusterFileUtils.createClusterConfigForHelm(execution)).thenReturn(DUMMY_CLUSTER_CONFIG);
        when(kubeClientBuilder.getCoreV1Api(DUMMY_CLUSTER_CONFIG)).thenThrow(ioException);

        assertThatThrownBy(() -> deleteSecret.execute(execution)).isInstanceOf(BpmnError.class);
    }

    @Test
    public void testExecute() throws Exception {
        createRulesForMockito();
        putVariablesForExecution();

        when(coreV1Api.deleteNamespacedSecret(anyString(), anyString())).thenReturn(apiDeleteNamespacedSecretRequest);

        deleteSecret.execute(execution);

        verify(apiDeleteNamespacedSecretRequest).execute();
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
