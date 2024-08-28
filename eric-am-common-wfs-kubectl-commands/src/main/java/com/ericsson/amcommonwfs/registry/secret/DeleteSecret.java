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

import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;
import jakarta.annotation.PostConstruct;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeleteSecret implements JavaDelegate {

    @Autowired
    private ClusterFileUtils clusterFileUtils;

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
    public void execute(DelegateExecution execution) throws Exception {

        final String namespace = (String) execution.getVariable(NAMESPACE);
        String secretName = (String) execution.getVariable(RELEASE_NAME);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);

        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            retryTemplate.execute(context ->
                    coreV1Api.deleteNamespacedSecret(secretName, namespace).execute());
        } catch (IOException e) { // NOSONAR
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT, e.getMessage(), execution);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Secret {} doesn't exist", secretName);
            } else {
                LOGGER.error("Unable to delete the secret : {} due to : {} ", secretName, e.getMessage(), e);
            }
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

}
