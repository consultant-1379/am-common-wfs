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
package com.ericsson.amcommonwfs.component;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.IS_ANNOTATED;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFICATION_ANNOTATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.io.IOException;
import java.util.Map;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CheckForAnnotationsTask implements JavaDelegate {

    @Autowired
    private VerificationHelper verificationHelper;

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        final String releaseName = (String) execution.getVariable(RELEASE_NAME);
        final String namespace = (String) execution.getVariable(NAMESPACE);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        try {
            boolean isPodAnnotated;
            V1PodList releasePods = verificationHelper.getV1PodList(namespace, clusterConfig, releaseName, 100);
            isPodAnnotated = releasePods.getItems().stream()
                    .anyMatch(pod -> getAnnotations(pod) != null && getAnnotations(pod)
                            .containsKey(VERIFICATION_ANNOTATION));
            execution.setVariable(IS_ANNOTATED, isPodAnnotated);
        } catch (ApiException | IOException e) {
            execution.setVariable(ERROR_MESSAGE, e.getMessage());
            LOGGER.error("Application failed due to {} ", e.getMessage());
            BusinessProcessExceptionUtils.handleException(BPMN_KUBECTL_FAILURE, e.getMessage(), execution);
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
    }

    private static Map<String, String> getAnnotations(final V1Pod pod) {
        final Map<String, String> annotations = pod.getMetadata().getAnnotations();
        LOGGER.info("Annotations from pod: {}", annotations);
        return annotations;
    }
}
