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

import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.AUX_SECRET;
import static com.ericsson.amcommonwfs.utils.constants.Constants.AUX_SECRET_LABEL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_DELETE_SECRET_FAILED;

import java.util.Optional;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Status;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeleteAuxSecretCommandHandler implements KubeApiHandler {

    private final RetryTemplate retryTemplate;

    public DeleteAuxSecretCommandHandler(RetryTemplate kubectlApiRetryTemplate) {
        this.retryTemplate = kubectlApiRetryTemplate;
    }

    @Override
    public Optional<Object> invokeKubeApiCall(final CoreV1Api coreV1Api,
           final DelegateExecution execution) {
        String namespace = (String) execution.getVariable(NAMESPACE);
        String releaseName = (String) execution.getVariable(RELEASE_NAME);
        StringBuilder labelSelector = new StringBuilder()
                .append(RELEASE_NAME)
                .append("=")
                .append(releaseName)
                .append(",")
                .append(AUX_SECRET)
                .append("=")
                .append(AUX_SECRET_LABEL);
        V1Status status = null;
        try {
            int timeOut = Integer.parseInt(resolveTimeOut(execution));
            V1SecretList secretList = coreV1Api.listNamespacedSecret(namespace).labelSelector(labelSelector.toString()).execute();
            for (V1Secret v1Secret: secretList.getItems()) {
                status =
                        retryTemplate.execute(context -> coreV1Api
                                .deleteNamespacedSecret(v1Secret.getMetadata().getName(), namespace)
                                .gracePeriodSeconds(timeOut)
                                .execute()
                        );
                LOGGER.info("Deleted auxiliary secret : {} ", v1Secret.getMetadata().getName());
            }
        } catch (ApiException e) {
            LOGGER.error("Delete aux secrets failed due to : ", e);
            String exceptionMessage = BusinessProcessExceptionUtils.buildApiExceptionMessage(e.getMessage(), e.getResponseBody());
            BusinessProcessExceptionUtils.handleException(BPMN_DELETE_SECRET_FAILED, exceptionMessage, execution);
        }
        return Optional.ofNullable(status);
    }

    @Override
    public void processApiResult(Object response) {
        V1Status v1Status = (V1Status) response;
        if (v1Status != null && "Success".equalsIgnoreCase(v1Status.getStatus())) {
            return;
        }
    }
}
