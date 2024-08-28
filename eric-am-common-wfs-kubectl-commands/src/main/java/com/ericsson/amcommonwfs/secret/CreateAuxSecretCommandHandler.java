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

import static com.ericsson.amcommonwfs.services.utils.CommonServicesUtils.parseJsonToGenericType;
import static com.ericsson.amcommonwfs.utils.constants.Constants.AUX_SECRET;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.EXECUTION_PROCESS_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_CREATE_SECRET_FAILED;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.kubernetes.client.openapi.models.V1SecretList;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * This class generates a k8s secret with additional parameters
 * <p>
 * e.g. kubectl create secret generic <secret_name> --namespace=<NS_name> --from-literal=userid=root --from-literal=password=aSecret
 */
@Component
@Slf4j
public class CreateAuxSecretCommandHandler implements KubeApiHandler {

    private static RetryTemplate retryTemplate;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    public CreateAuxSecretCommandHandler(RetryTemplate kubectlApiRetryTemplate) {
        CreateAuxSecretCommandHandler.retryTemplate = kubectlApiRetryTemplate;
    }

    @Override
    public Optional<Object> invokeKubeApiCall(final CoreV1Api coreV1Api, final DelegateExecution execution) {
        final Map<String, String> day0Configuration = extractDay0ConfigurationIfNotEmpty(execution);

        day0Configuration.entrySet().parallelStream().forEach(entry -> {
            var secretName = entry.getKey();
            Map<String, String> value = getJsonObject(entry.getValue());
            executeSecretApi(coreV1Api, execution, secretName, value);
        });

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> extractDay0ConfigurationIfNotEmpty(final DelegateExecution execution) {
        return Optional
                .ofNullable(execution.getVariable(DAY0_CONFIGURATION))
                .map(this::deCryptDay0Configuration)
                .orElse(Collections.emptyMap());
    }

    protected Map<String, String> deCryptDay0Configuration(Object encryptedDay0Configuration) {
        String encryptedDay0String = getEncryptedDay0String(encryptedDay0Configuration);
        String decryptedConfig = "";
        try {
            decryptedConfig = cryptoService.decryptString(encryptedDay0String);

            return getJsonObject(decryptedConfig);
        } catch (CryptoException ex) { // NOSONAR
            //Need to catch the CryptoException and wrap in a BPM exception so the camunda workflow exits correctly
            BusinessProcessExceptionUtils.throwBusinessProcessException(ErrorCode.BPMN_DEPENDENCY_SERVICE_UNAVAILABLE, ex.getMessage());
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("rawtypes")
    private static String getEncryptedDay0String(Object encryptedDay0Configuration) {
        if (!(encryptedDay0Configuration instanceof List)) {
            BusinessProcessExceptionUtils.throwBusinessProcessException(BPMN_CREATE_SECRET_FAILED, "Expected List with String element");
        }
        List list = (List) encryptedDay0Configuration;
        if (list.size() != 1 || !(list.get(0) instanceof String)) {
            BusinessProcessExceptionUtils.throwBusinessProcessException(BPMN_CREATE_SECRET_FAILED, "Expected List with String element");
        }
        return (String) list.get(0);
    }

    protected static Object executeSecretApi(final CoreV1Api coreV1Api, final DelegateExecution execution, final String secretName,
                                           final Map<String, String> secretData) {
        String namespace = (String) execution.getVariable(NAMESPACE);
        String releaseName = (String) execution.getVariable(RELEASE_NAME);

        V1SecretBuilder v1SecretBuilder = new V1SecretBuilder();

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(secretName);
        Map<String, String> labels = new HashMap<>();
        labels.put(RELEASE_NAME, releaseName);
        labels.put(AUX_SECRET, Constants.AUX_SECRET_LABEL);
        labels.put(EXECUTION_PROCESS_ID, execution.getProcessInstanceId());
        v1ObjectMeta.setLabels(labels);

        V1Secret v1Secret = v1SecretBuilder.withType("generic")
                .withMetadata(v1ObjectMeta)
                .withStringData(secretData).build();
        try {
            String labelSelector = String.format("%s=%s", EXECUTION_PROCESS_ID, execution.getProcessInstanceId());
            V1SecretList secretList = coreV1Api.listNamespacedSecret(namespace).labelSelector(labelSelector).execute();
            boolean hasMatchingSecret = secretList.getItems().stream()
                    .anyMatch(secret -> Objects.equals(secret.getMetadata().getName(),
                            v1Secret.getMetadata().getName()));

            if (!hasMatchingSecret) {
                LOGGER.info("Creating secret : {} in namespace : {}", secretName, namespace);
                return retryTemplate.execute(context -> coreV1Api.createNamespacedSecret(namespace, v1Secret).execute());
            }
        } catch (ApiException e) {
            handleApiException(execution, e);
        }
        return null;
    }

    protected static void handleApiException(DelegateExecution execution, ApiException e) {
        LOGGER.error("Create aux secret failed due to :", e);
        String exceptionMessage = BusinessProcessExceptionUtils.buildApiExceptionMessage(e.getMessage(), e.getResponseBody());
        BusinessProcessExceptionUtils.handleException(BPMN_CREATE_SECRET_FAILED, exceptionMessage, execution);
    }

    protected static Map<String, String> getJsonObject(String decryptedConfig) {
        try {
            return parseJsonToGenericType(decryptedConfig, new TypeReference<HashMap<String, String>>() {
            });
        } catch (RuntimeException e) { // NOSONAR
            BusinessProcessExceptionUtils.throwBusinessProcessException(BPMN_CREATE_SECRET_FAILED, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("squid:S1186")
    @Override
    public void processApiResult(Object response) {

    }

}