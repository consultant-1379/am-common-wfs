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

import static com.ericsson.amcommonwfs.component.VerificationHelper.verifyJobCreatedPod;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TERMINATED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_KUBECTL_FAILURE;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;

import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VerifyTerminationTask implements JavaDelegate {

    @Autowired
    private VerificationHelper verificationHelper;

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timeout = (long) execution.getVariable(APP_TIMEOUT);
        if (currentTime >= timeout) {
            execution.setVariable(APP_TIMED_OUT, true);
        }
        execution.setVariable(APPLICATION_TERMINATED, false);
        String clusterConfig = clusterFileUtils.createClusterConfigForHelm(execution);

        try {
            final String releaseName = (String) execution.getVariable(RELEASE_NAME);
            final String namespace = (String) execution.getVariable(NAMESPACE);

            final int applicationTimeOut = Integer.parseInt(resolveTimeOut(execution));
            final boolean skipJobVerification = (Boolean) execution.getVariable(SKIP_JOB_VERIFICATION);
            V1PodList v1PodList = verificationHelper.getV1PodList(namespace, clusterConfig, releaseName, applicationTimeOut);
            if (v1PodList.getItems().stream().noneMatch(pod -> verifyJobCreatedPod(skipJobVerification, pod))) {
                LOGGER.info("Application successfully terminated");
                execution.setVariable(APP_TIMED_OUT, false);
                execution.setVariable(APPLICATION_TERMINATED, true);
            }
        } catch (Exception e) { // NOSONAR
            handlerErrorFromKubectl(execution, e.getMessage());
        } finally {
            clusterFileUtils.removeClusterConfig(clusterConfig);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage());
        }
    }

    private static void handlerErrorFromKubectl(final DelegateExecution execution, final String message) {
        execution.setVariable(ERROR_MESSAGE, message);
        LOGGER.error("Application termination failed due to {} ", message);
        BusinessProcessExceptionUtils.handleException(BPMN_KUBECTL_FAILURE, message, execution);
    }
}
