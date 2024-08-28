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

import static com.ericsson.amcommonwfs.util.KubeApiExceptionUtils.isAlreadyExistError;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CreateSecret implements JavaDelegate {

    public static final String REGISTRY_SECRET = "kubernetes.io/dockerconfigjson";
    public static final String DOCKERCONFIGJSON = ".dockerconfigjson";

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Autowired
    private DockerConfigUtils dockerConfigUtils;

    private RetryTemplate retryTemplate;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;
    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    @PostConstruct
    public void init() {
        this.retryTemplate = kubectlApiRetryTemplate;
    }

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        final String namespace = (String) execution.getVariable(NAMESPACE);
        String secretName = (String) execution.getVariable(RELEASE_NAME);

        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            V1Secret v1Secret = buildDockerRegistrySecret(secretName);
            retryTemplate.execute(context -> coreV1Api.createNamespacedSecret(namespace, v1Secret).execute());
        } catch (IOException e) { // NOSONAR
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT, e.getMessage(), execution);
        } catch (ApiException e) { // NOSONAR
            if (isAlreadyExistError(e)) {
                LOGGER.info("Secret : {} already exists. Ignoring the Conflict.", secretName);
            } else {
                BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_CREATE_SECRET_FAILED, e.getResponseBody(), execution);
            }
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

    V1Secret buildDockerRegistrySecret(final String secretName) throws JsonProcessingException {
        V1SecretBuilder v1SecretBuilder = new V1SecretBuilder();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(secretName);

        Map<String, String> secretData = new HashMap<>();
        secretData.put(DOCKERCONFIGJSON, dockerConfigUtils.constructDockerConfigJson());

        return v1SecretBuilder.withType(REGISTRY_SECRET)
                .withMetadata(v1ObjectMeta)
                .withStringData(secretData).build();
    }
}
