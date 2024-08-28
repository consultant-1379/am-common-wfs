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
package com.ericsson.amcommonwfs.checknamespace;

import static com.ericsson.amcommonwfs.util.BpmKubeApiExceptionHandler.handleGenericException;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.io.IOException;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.common.EvnfmNamespaceService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;

import io.kubernetes.client.openapi.ApiException;

@Component
public class CheckEvnfmNamespace implements JavaDelegate {

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Autowired
    private EvnfmNamespaceService evnfmNamespaceService;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        String namespaceTarget = (String) execution.getVariable(NAMESPACE);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);

        try {
            if (evnfmNamespaceService.checkEvnfmNamespace(namespaceTarget, clusterConfig)) {
                BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION,
                                                "Cannot instantiate in the same namespace which "
                                                        + "EVNFM is deployed in. Use a different Namespace.", execution);
            }
        } catch (IOException e) { // NOSONAR
            BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT, e.getMessage(), execution);
        } catch (ApiException e) {
            handleGenericException(BPMN_KUBECTL_FAILURE, execution, e);
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }
}
