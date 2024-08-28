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

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RETRY_CRD_INSTALL;

@Component
@Slf4j
public class CreateCrdAuxSecretCommandHandler extends CreateAuxSecretCommandHandler {

    public CreateCrdAuxSecretCommandHandler(RetryTemplate kubectlApiRetryTemplate) {
        super(kubectlApiRetryTemplate);
    }

    @Override
    public Optional<Object> invokeKubeApiCall(CoreV1Api coreV1Api, DelegateExecution execution) {
        final Map<String, String> day0Configuration = extractDay0ConfigurationIfNotEmpty(execution);
        final String namespace = (String) execution.getVariable(NAMESPACE);

        try {
            V1SecretList secretList = coreV1Api.listNamespacedSecret(namespace).execute();
            boolean secretAlreadyExist = secretList.getItems().stream()
                    .map(V1Secret::getMetadata).map(V1ObjectMeta::getName)
                    .anyMatch(day0Configuration::containsKey);
            if (secretAlreadyExist) {
                execution.setVariable(RETRY_CRD_INSTALL, Boolean.TRUE);
                return Optional.empty();
            }
        } catch (ApiException ae) {
            handleApiException(execution, ae);
            return Optional.empty();
        }
        execution.setVariable(RETRY_CRD_INSTALL, Boolean.FALSE);
        day0Configuration.entrySet().parallelStream().forEach(entry -> {
            var secretName = entry.getKey();
            Map<String, String> value = getJsonObject(entry.getValue());
            executeSecretApi(coreV1Api, execution, secretName, value);
        });

        return Optional.empty();
    }
}
