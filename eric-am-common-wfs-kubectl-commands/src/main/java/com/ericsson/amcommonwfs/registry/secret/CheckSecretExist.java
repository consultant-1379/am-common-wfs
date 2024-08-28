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

import static com.ericsson.amcommonwfs.MessageConstants.FORBIDDEN_CLUSTER_MESSAGE;
import static com.ericsson.amcommonwfs.registry.secret.CreateSecret.DOCKERCONFIGJSON;
import static com.ericsson.amcommonwfs.util.BpmKubeApiExceptionHandler.handleGenericException;
import static com.ericsson.amcommonwfs.util.KubeApiExceptionUtils.isAlreadyExistError;
import static com.ericsson.amcommonwfs.utils.constants.Constants.GLOBAL_REGISTRY_SECRET_PRESENT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils.handleException;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_IO_EXCEPTION;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.amcommonwfs.secret.PatchRequest;
import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.PatchUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CheckSecretExist implements JavaDelegate {

    private static final String MESSAGE_KEY_IN_API_EXCEPTION = "message";
    public static final String PATCH_REQUEST_ERROR = "Issue patching the secret %s with the following ERROR %s";

    private static RetryTemplate retryTemplate;

    @Value("${autoConfigureDocker.enabled}")
    private boolean autoConfigureDockerEnabled;

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Autowired
    private DockerConfigUtils dockerConfigUtils;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    @PostConstruct
    public void init() {
        setRetryTemplate(kubectlApiRetryTemplate);
    }

    private static void setRetryTemplate(RetryTemplate kubectlApiRetryTemplate) {
        retryTemplate = kubectlApiRetryTemplate;
    }

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        final String namespace = (String) execution.getVariable(NAMESPACE);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            createNamespaceIfDoesntExist(namespace, execution, coreV1Api);

            if (autoConfigureDockerEnabled) {
                checkRegistrySecretInNamespace(namespace, coreV1Api, execution);
            } else {
                LOGGER.info("OverrideGlobalRegistry is set to false skipping secrets");
                execution.setVariable(GLOBAL_REGISTRY_SECRET_PRESENT, true);
            }
        } catch (IOException e) { // NOSONAR
            handleException(ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT, e.getMessage(), execution);
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

    void checkRegistrySecretInNamespace(String namespace, CoreV1Api coreV1Api, DelegateExecution execution) {
        try {
            String secretName = (String) execution.getVariable(RELEASE_NAME);
            V1Secret v1Secret = retryTemplate.execute(context -> coreV1Api.readNamespacedSecret(secretName, namespace).execute());
            if (v1Secret.getMetadata() != null && StringUtils.equals(v1Secret.getMetadata().getName(), secretName)) {
                verifySecretContents(namespace, coreV1Api, execution, v1Secret);
            } else {
                execution.setVariable(GLOBAL_REGISTRY_SECRET_PRESENT, false);
            }
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
                execution.setVariable(GLOBAL_REGISTRY_SECRET_PRESENT, false);
            } else {
                handleGenericException(BPMN_KUBECTL_FAILURE, execution, e);
            }
        } catch (JsonProcessingException e) { // NOSONAR
            handleException(BPMN_IO_EXCEPTION, e.getMessage(), execution);
        }
    }

    private void verifySecretContents(final String namespace, final CoreV1Api coreV1Api,
                                      final DelegateExecution execution, final V1Secret v1Secret) throws JsonProcessingException {

        String secretName = (String) execution.getVariable(RELEASE_NAME);
        Map<String, byte[]> dockerConfigCluster = v1Secret.getData();

        if (!CollectionUtils.isEmpty(dockerConfigCluster)) {

            String dockerConfigExisting = new String(dockerConfigCluster.get(DOCKERCONFIGJSON)); // NOSONAR
            String currentDockerConfig = dockerConfigUtils.constructDockerConfigJson();

            if (!StringUtils.equals(dockerConfigExisting, currentDockerConfig)) {
                patchSecretInNamespace(v1Secret.getMetadata(), DOCKERCONFIGJSON,
                                       currentDockerConfig, execution, coreV1Api);
            }
            execution.setVariable(GLOBAL_REGISTRY_SECRET_PRESENT, true);
        } else {
            deleteRegistrySecretInNamespace(secretName, namespace, coreV1Api, execution);
            execution.setVariable(GLOBAL_REGISTRY_SECRET_PRESENT, false);
        }
    }

    private static void deleteRegistrySecretInNamespace(final String secretName, final String namespace,
                                                        final CoreV1Api coreV1Api, final DelegateExecution execution) {
        try {
            retryTemplate.execute(context -> coreV1Api.deleteNamespacedSecret(secretName, namespace).execute());
        } catch (ApiException e) {
            handleGenericException(BPMN_KUBECTL_FAILURE, execution, e);
        }
    }

    private static void createNamespaceIfDoesntExist(final String namespace, final DelegateExecution execution,
                                                     final CoreV1Api coreV1Api) {
        V1Namespace v1Namespace;
        try {
            v1Namespace = retryTemplate.execute(context -> coreV1Api.readNamespace(namespace).execute());
            if (v1Namespace == null || v1Namespace.getMetadata() == null || v1Namespace.getMetadata().getName() == null) {
                createNamespace(namespace, coreV1Api, execution);
            }
        } catch (ApiException e) {
            LOGGER.error("Unable to get the namespace : {} due to : {} with status code {}", namespace, e.getMessage(), e.getCode());
            if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
                createNamespace(namespace, coreV1Api, execution);
            } else {
                handleGenericException(BPMN_KUBECTL_FAILURE, execution, e);
            }
        }
    }

    private static void createNamespace(final String namespace, final CoreV1Api coreV1Api, final DelegateExecution execution) {
        V1NamespaceBuilder v1NamespaceBuilder = new V1NamespaceBuilder();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(namespace);
        V1Namespace v1Namespace = v1NamespaceBuilder.withMetadata(v1ObjectMeta).build();
        V1Namespace createdNamespace;
        try {
            createdNamespace = retryTemplate.execute(context -> coreV1Api.createNamespace(v1Namespace).execute());
            if (createdNamespace == null || createdNamespace.getMetadata() == null
                    || createdNamespace.getMetadata().getName() == null) {
                handleException(BPMN_KUBECTL_FAILURE,
                                String.format("Failed to create namespace %s", namespace), execution);
            }
        } catch (ApiException e) {
            if (isAlreadyExistError(e)) {
                LOGGER.info("Namespace : {} already exists. Ignoring the Conflict.", namespace);
            } else {
                handleException(BPMN_KUBECTL_FAILURE, getErrorMessage(e), execution);
            }
        }
    }

    private static String getErrorMessage(ApiException e) {
        try {
            if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
                return FORBIDDEN_CLUSTER_MESSAGE;
            } else if (!Strings.isNullOrEmpty(e.getResponseBody())) {
                String errorResponseBody = e.getResponseBody();
                JSONObject errResp = new JSONObject(errorResponseBody);
                if (errResp.has(MESSAGE_KEY_IN_API_EXCEPTION) && errResp.get(MESSAGE_KEY_IN_API_EXCEPTION) instanceof String) {
                    return errResp.getString(MESSAGE_KEY_IN_API_EXCEPTION);
                } else {
                    return e.getMessage();
                }
            } else {
                return e.getMessage();
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred while getting the error message from ApiException:", ex);
            return e.getMessage();
        }
    }

    private static void patchSecretInNamespace(final V1ObjectMeta metadata, final String key, final String value,
                                               final DelegateExecution execution, final CoreV1Api coreV1Api) {
        try {
            if (metadata.getName() != null) {
                patchSecret(metadata.getName(), key, value, coreV1Api, execution);
            } else {
                BusinessProcessExceptionUtils.throwBusinessProcessException(ErrorCode.BPMN_IO_EXCEPTION, "Metadata name attribute is null.");
            }
        } catch (IOException e) {
            String message = String
                    .format(PATCH_REQUEST_ERROR, metadata.getName(), e);
            handleException(BPMN_KUBECTL_FAILURE, message, execution);
        }
    }

    private static void patchSecret(final String name, final String key,
                                    final String value, final CoreV1Api coreV1Api, final DelegateExecution execution) throws JsonProcessingException {
        final String namespace = (String) execution.getVariable(NAMESPACE);
        try {
            V1Patch v1Patch = getV1Path("replace", "/data/" + key,
                                        Base64.getEncoder().encodeToString(value.getBytes())); // NOSONAR
            retryTemplate.execute(context -> PatchUtils.patch(V1Secret.class,
                                                              () -> coreV1Api.patchNamespacedSecret(name, namespace, v1Patch)
                                                                      .buildCall(null),
                                                              V1Patch.PATCH_FORMAT_JSON_PATCH,
                                                              coreV1Api.getApiClient())
            );
        } catch (ApiException e) { // NOSONAR
            String message = String
                    .format(PATCH_REQUEST_ERROR, name, e.getResponseBody());
            handleException(BPMN_KUBECTL_FAILURE, message, execution);
        }
    }

    private static V1Patch getV1Path(final String operation, final String path, final String value) throws JsonProcessingException {
        List<PatchRequest> patchRequests = new ArrayList<>();
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setOperation(operation);
        patchRequest.setPath(path);
        patchRequest.setValue(value);
        patchRequests.add(patchRequest);

        ObjectMapper objectMapper = new ObjectMapper();
        return new V1Patch(objectMapper.writeValueAsString(patchRequests));
    }
}
