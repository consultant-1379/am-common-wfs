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

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Status;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DeleteAuxSecretCommandHandlerTest {

    private DeleteAuxSecretCommandHandler testedObject;

    @Mock
    DelegateExecution delegateExecution;

    @MockBean
    private CoreV1Api coreV1Api;

    @Mock
    private CoreV1Api.APIlistNamespacedSecretRequest apiListNamespacedSecretRequest;

    @Mock
    private CoreV1Api.APIdeleteNamespacedSecretRequest apiDeleteNamespacedSecretRequest;

    private RetryTemplate kubectlApiRetryTemplate;

    @BeforeEach
    public void setUp() {
        kubectlApiRetryTemplate = new RetryTemplate();
        testedObject = new DeleteAuxSecretCommandHandler(kubectlApiRetryTemplate);

        when(coreV1Api.listNamespacedSecret(any())).thenReturn(apiListNamespacedSecretRequest);
        when(apiListNamespacedSecretRequest.labelSelector(any())).thenReturn(apiListNamespacedSecretRequest);
    }

    @Test
    public void shouldNotDeleteAuxSecretIfNoLabeledSecretFound() throws ApiException {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);

        given(delegateExecution.getVariable(NAMESPACE)).willReturn(StringUtils.EMPTY);
        given(delegateExecution.getVariable(APP_TIMEOUT)).willReturn(toEpochSecond);
        when(apiListNamespacedSecretRequest.execute()).thenReturn(new V1SecretList());
        verify(apiDeleteNamespacedSecretRequest, never()).execute();
        assertThat(testedObject.invokeKubeApiCall(coreV1Api, delegateExecution)).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldDeleteAuxSecret() throws ApiException {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        //given
        given(delegateExecution.getVariable(NAMESPACE)).willReturn("test-ns");
        given(delegateExecution.getVariable(APP_TIMEOUT)).willReturn(toEpochSecond);

        V1SecretList secretList = new V1SecretList();
        V1Secret secret = new V1Secret();
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName("secret-name");
        secret.setMetadata(metadata);
        secretList.addItemsItem(secret);

        when(apiListNamespacedSecretRequest.execute()).thenReturn(secretList);

        when(coreV1Api.deleteNamespacedSecret(anyString(), anyString())).thenReturn(apiDeleteNamespacedSecretRequest);
        when(apiDeleteNamespacedSecretRequest.gracePeriodSeconds(anyInt())).thenReturn(apiDeleteNamespacedSecretRequest);
        when(apiDeleteNamespacedSecretRequest.execute()).thenReturn(new V1Status());
        V1Status v1Status = (V1Status) testedObject.invokeKubeApiCall(coreV1Api, delegateExecution).get();
        assertThat(v1Status).isNotNull();
    }
}
