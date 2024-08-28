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
package com.ericsson.amcommonwfs.secret;


import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.services.crypto.DevCryptoService;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.openapi.models.V1SecretList;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.amcommonwfs.secret.SecretTestUtils.prepareDay0Configuration;
import static com.ericsson.amcommonwfs.services.utils.CommonServicesUtils.convertObjToJsonString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.EXECUTION_PROCESS_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RetryProperties.class, RetryTemplateConfig.class})
public class CreateAuxSecretCommandHandlerTest {

    private CreateAuxSecretCommandHandler testedObject;

    private CryptoService testCryptoService = new DevCryptoService();

    @Mock
    private DelegateExecution execution;

    @MockBean
    private CoreV1Api coreV1Api;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;

    @Mock
    private CoreV1Api.APIcreateNamespacedSecretRequest apiCreateNamespacedSecretRequest;

    @Mock
    private CoreV1Api.APIlistNamespacedSecretRequest apiListNamespacedSecretRequest;

    public CreateAuxSecretCommandHandlerTest() throws Exception {
    }

    @BeforeEach
    public void setUp() {
        testedObject = new CreateAuxSecretCommandHandler(kubectlApiRetryTemplate);
        ReflectionTestUtils.setField(testedObject, "cryptoService", testCryptoService);
        when(coreV1Api.createNamespacedSecret(any(), any())).thenReturn(apiCreateNamespacedSecretRequest);
        when(coreV1Api.listNamespacedSecret(any())).thenReturn(apiListNamespacedSecretRequest);
        when(apiListNamespacedSecretRequest.labelSelector(any())).thenReturn(apiListNamespacedSecretRequest);
    }

    @Test
    public void shouldNotCreateSecretOnNullDay0Configuration() throws ApiException {
        when(execution.getVariable(Constants.DAY0_CONFIGURATION)).thenReturn(null);
        testedObject.invokeKubeApiCall(coreV1Api, execution);
        verify(apiCreateNamespacedSecretRequest, times(0)).execute();
    }

    @Test
    public void shouldNotCreateExistingSecret() throws ApiException {
        Map<String, String> day0Configuration = prepareDay0Configuration();
        final Map<String, Object> context = prepareContext(day0Configuration);
        String processId = "12355";
        context.put(EXECUTION_PROCESS_ID, processId);

        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(day0Configuration.keySet().stream().findFirst().get());
        objectMeta.setLabels(Map.of(EXECUTION_PROCESS_ID, processId));

        V1Secret secret = new V1Secret();
        secret.setMetadata(objectMeta);

        V1SecretList secretList = new V1SecretList();
        secretList.addItemsItem(secret);

        when(execution.getVariable(anyString())).thenAnswer(
                invocationOnMock -> context.get(invocationOnMock.getArgument(0, String.class)));
        when(execution.getProcessInstanceId()).thenReturn(processId);
        when(apiCreateNamespacedSecretRequest.execute()).thenReturn(new V1Secret());
        when(apiListNamespacedSecretRequest.execute()).thenReturn(secretList);

        testedObject.invokeKubeApiCall(coreV1Api, execution);

        verify(apiCreateNamespacedSecretRequest, times(1)).execute();
    }

    @Test
    public void shouldCreateSecretsBasedOnDay0Configuration() throws ApiException {
        Map<String, String> day0Configuration = prepareDay0Configuration();
        final Map<String, Object> context = prepareContext(day0Configuration);
        when(execution.getVariable(anyString())).thenAnswer(
                invocationOnMock -> context.get(invocationOnMock.getArgument(0, String.class)));
        when(apiCreateNamespacedSecretRequest.execute()).thenReturn(new V1Secret());
        when(apiListNamespacedSecretRequest.execute()).thenReturn(new V1SecretList());

        testedObject.invokeKubeApiCall(coreV1Api, execution);

        verify(apiCreateNamespacedSecretRequest, times(2)).execute();
    }

    @Test
    public void shouldThrowOnInvalidDay0Configuration() {
        Map<String, String> day0Configuration = new HashMap<>();
        day0Configuration.put("secret1", "test-secret");
        final Map<String, Object> context = prepareContext(day0Configuration);
        when(execution.getVariable(anyString())).thenAnswer(
                invocationOnMock -> context.get(invocationOnMock.getArgument(0, String.class)));
        assertThatThrownBy(() -> testedObject.invokeKubeApiCall(coreV1Api, execution))
                .isInstanceOf(BpmnError.class)
                .hasMessageContaining("Unable to parse json: [test-secret], because of Unrecognized token 'test'");
    }

    @Test
    public void shouldThrowOnInvalidDay0ConfigurationArgument() {
        Map<String, String> day0Configuration = prepareDay0Configuration();

        final Map<String, Object> context = prepareContextWithInvalidArgument(day0Configuration);
        when(execution.getVariable(anyString())).thenAnswer(
                invocationOnMock -> context.get(invocationOnMock.getArgument(0, String.class)));

        assertThatThrownBy(() -> testedObject.invokeKubeApiCall(coreV1Api, execution))
                .isInstanceOf(BpmnError.class)
                .hasMessageContaining("Expected List with String element");
    }

    private Map<String, Object> prepareContext(Map<String, String> day0Configuration) {
        final Map<String, Object> context = new HashMap<>();
        context.put(Constants.DAY0_CONFIGURATION, List.of(encryptDay0Configuration(day0Configuration)));
        context.put(Constants.NAMESPACE, "test-ns");
        context.put(Constants.CLUSTER_NAME, "hahn061");
        return context;
    }

    private Map<String, Object> prepareContextWithInvalidArgument(Map<String, String> day0Configuration) {
        final Map<String, Object> context = new HashMap<>();
        context.put(Constants.DAY0_CONFIGURATION, encryptDay0Configuration(day0Configuration));
        context.put(Constants.NAMESPACE, "test-ns");
        context.put(Constants.CLUSTER_NAME, "hahn061");
        return context;
    }

    private String encryptDay0Configuration(Map<String, String> day0Configuration) {
        String configurationAsString = convertObjToJsonString(day0Configuration);
        return testCryptoService.encryptString(configurationAsString);
    }
}
