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

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.services.crypto.DevCryptoService;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.amcommonwfs.secret.SecretTestUtils.createSecretsResponse;
import static com.ericsson.amcommonwfs.secret.SecretTestUtils.prepareDay0Configuration;
import static com.ericsson.amcommonwfs.services.utils.CommonServicesUtils.convertObjToJsonString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RETRY_CRD_INSTALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateCrdAuxSecretCommandHandlerTest {
    private CreateCrdAuxSecretCommandHandler testedObject;

    private CryptoService testCryptoService = new DevCryptoService();

    private AutoCloseable mocksCloseable;

    @Mock
    private DelegateExecution execution;

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private CoreV1Api.APIcreateNamespacedSecretRequest apiCreateNamespacedSecretRequest;

    @Mock
    private CoreV1Api.APIlistNamespacedSecretRequest apiListNamespacedSecretRequest;

    private RetryTemplate kubectlApiRetryTemplate;

    private Map<String, Object> context;

    public CreateCrdAuxSecretCommandHandlerTest() throws Exception {
    }

    @BeforeEach
    public void setUp() throws ApiException {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        kubectlApiRetryTemplate = new RetryTemplate();
        testedObject = new CreateCrdAuxSecretCommandHandler(kubectlApiRetryTemplate);
        ReflectionTestUtils.setField(testedObject, "cryptoService", testCryptoService);
        context = prepareContext(prepareDay0Configuration());
        when(execution.getVariable(anyString())).thenAnswer(
                invocationOnMock -> context.get(invocationOnMock.getArgument(0, String.class)));
        doAnswer(invocationOnMock -> {
            context.put(invocationOnMock.getArgument(0, String.class), invocationOnMock.getArgument(1));
            return null;
        }).when(execution).setVariable(anyString(), any());

        when(coreV1Api.createNamespacedSecret(anyString(), any())).thenReturn(apiCreateNamespacedSecretRequest);
        when(apiCreateNamespacedSecretRequest.execute()).thenReturn(new V1Secret());
        when(coreV1Api.listNamespacedSecret(anyString())).thenReturn(apiListNamespacedSecretRequest);
    }

    @AfterEach
    public void cleanUp() throws Exception {
        mocksCloseable.close();
    }

    @Test
    public void testRetryRequestedIfSecretExits() throws ApiException {
        when(apiListNamespacedSecretRequest.execute()).thenReturn(createSecretsResponse(true));
        testedObject.invokeKubeApiCall(coreV1Api, execution);
        assertTrue(context.containsKey(RETRY_CRD_INSTALL));
        assertEquals(Boolean.TRUE, context.get(RETRY_CRD_INSTALL));
    }

    @Test
    public void testSecretsCreatedIfNoConflict() throws ApiException {
        when(apiListNamespacedSecretRequest.labelSelector(anyString())).thenReturn(apiListNamespacedSecretRequest);
        when(apiListNamespacedSecretRequest.execute()).thenReturn(createSecretsResponse(false));
        testedObject.invokeKubeApiCall(coreV1Api, execution);
        assertTrue(context.containsKey(RETRY_CRD_INSTALL));
        assertEquals(Boolean.FALSE, context.get(RETRY_CRD_INSTALL));
        verify(apiCreateNamespacedSecretRequest, times(2)).execute();
    }

    private Map<String, Object> prepareContext(Map<String, String> day0Configuration) {
        final Map<String, Object> context = new HashMap<>();
        context.put(Constants.DAY0_CONFIGURATION, List.of(encryptDay0Configuration(day0Configuration)));
        context.put(Constants.NAMESPACE, "test-ns");
        context.put(Constants.CLUSTER_NAME, "haber-haber");
        return context;
    }

    private String encryptDay0Configuration(Map<String, String> day0Configuration) {
        String configurationAsString = convertObjToJsonString(day0Configuration);
        return testCryptoService.encryptString(configurationAsString);
    }
}
