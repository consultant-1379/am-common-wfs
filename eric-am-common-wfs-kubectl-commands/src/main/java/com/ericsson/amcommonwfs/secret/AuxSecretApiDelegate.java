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


import java.io.IOException;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuxSecretApiDelegate implements JavaDelegate {

    private final KubeApiHandler kubeApiHandler;

    @Autowired
    private ClusterFileUtils clusterFileUtils;
    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    public AuxSecretApiDelegate(KubeApiHandler apiHandler) {
        this.kubeApiHandler = apiHandler;
    }

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) throws Exception {
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);

        try {
            CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
            LOGGER.info("Delegating to the class : {} ", kubeApiHandler.getClass());
            kubeApiHandler.invokeKubeApiCall(coreV1Api, execution);
        } catch (IOException e) { // NOSONAR
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT, e.getMessage(), execution);
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }
}
