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
package com.ericsson.amcommonwfs.presentation.services.messaging;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.ERROR_UNKNOWN_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.constants.CommandConstants.COMMAND_TYPE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.API_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_BUS_RETRY_TIME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_RETRIES_COMPLETED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MESSAGE_SENT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TERMINATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION;
import static com.ericsson.workflow.orchestration.mgmt.model.ApiVersion.API_V3;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.component.VerifyExecution;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PublishLifecycleMessage implements JavaDelegate {

    private static final String ERROR_END_ACTIVITY_ID = "Activity_Publish_Lifecycle_Message_Error_End";
    private static final String CRD_COMMAND = "crd";

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private MessagingHealth messagingHealth;

    @Autowired
    private VerifyExecution verifyExecution;

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) throws Exception {
        execution.setVariable(MESSAGE_SENT, false);

        String apiVersion = (String) execution.getVariable(API_VERSION);
        /* Messages to be published only for V3 apiVersions
         * This condition can be removed once V2 apiVersions are removed */
        if (StringUtils.equalsIgnoreCase(apiVersion, API_V3.toString())) {
            if (messagingHealth.isUp()) {
                sendHelmMessage(execution);
            } else if (isRetriesCompleted(execution)) {
                return;
            }
        } else {
            execution.setVariable(MESSAGE_SENT, true);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("An error occurred: ", e);
        }
    }

    private static boolean isRetriesCompleted(DelegateExecution execution) {
        LOGGER.warn("Unable to publish the message as messaging service is down. Retrying to send the message.");
        long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long retryTime = (Long) execution.getVariable(MESSAGE_BUS_RETRY_TIME);
        if (currentTime >= retryTime) {
            LOGGER.error("Messaging service failed to come up.");
            execution.setVariable(MESSAGE_RETRIES_COMPLETED, true);
            return true;
        }
        return false;
    }

    private void sendHelmMessage(final DelegateExecution execution) {
        Object message = getMessageToPublishBasedOnOperation(execution);
        try {
            LOGGER.info("Message sent: {}", message);
            String idempotencyKey = execution.getProcessBusinessKey();
            messagingService.sendMessage(message, idempotencyKey);
            execution.setVariable(MESSAGE_SENT, true);
        } catch (Exception e) {
            LOGGER.error("Unable to publish the message due to :: ", e);
        }
    }

    private Object getMessageToPublishBasedOnOperation(final DelegateExecution execution) {
        String commandType = (String) execution.getVariable(COMMAND_TYPE);
        if (Objects.equals(commandType, CRD_COMMAND)) {
            return getGenericEventMessage(execution);
        } else {
            return getHelmReleaseLifecycleMessage(execution);
        }
    }

    private static WorkflowServiceEventMessage getGenericEventMessage(final DelegateExecution execution) {
        String releaseName = (String) execution.getVariable(RELEASE_NAME);
        String lifeCycleOperationId = (String) execution.getVariable(LIFECYCLE_OPERATION_ID);
        WorkflowServiceEventStatus state = ERROR_END_ACTIVITY_ID.equalsIgnoreCase(execution.getSuperExecution().getCurrentActivityId()) ?
                WorkflowServiceEventStatus.FAILED :
                WorkflowServiceEventStatus.COMPLETED;
        String message = (String) execution.getVariable(LIFECYCLE_MESSAGE);

        return new WorkflowServiceEventMessage(lifeCycleOperationId, WorkflowServiceEventType.CRD, state, message, releaseName);
    }

    private HelmReleaseLifecycleMessage getHelmReleaseLifecycleMessage(final DelegateExecution execution) {
        HelmReleaseLifecycleMessage helmReleaseLifecycleMessage = new HelmReleaseLifecycleMessage();
        String releaseName = (String) execution.getVariable(RELEASE_NAME);
        String message = (String) execution.getVariable(LIFECYCLE_MESSAGE);
        String lifeCycleOperationId = (String) execution.getVariable(LIFECYCLE_OPERATION_ID);
        HelmReleaseState state = ERROR_END_ACTIVITY_ID.equalsIgnoreCase(execution.getSuperExecution().getCurrentActivityId()) ?
                HelmReleaseState.FAILED :
                HelmReleaseState.COMPLETED;
        String revisionNumber = null;
        if (state.equals(HelmReleaseState.COMPLETED)) {
            revisionNumber = (String) execution.getVariable(REVISION_NUMBER);
        }
        helmReleaseLifecycleMessage.setOperationType(getLifecycleState(execution));
        helmReleaseLifecycleMessage.setLifecycleOperationId(lifeCycleOperationId);
        helmReleaseLifecycleMessage.setReleaseName(releaseName);
        helmReleaseLifecycleMessage.setMessage(message);
        helmReleaseLifecycleMessage.setState(state);
        helmReleaseLifecycleMessage.setRevisionNumber(revisionNumber);

        return helmReleaseLifecycleMessage;
    }

    private HelmReleaseOperationType getLifecycleState(DelegateExecution execution) {
        String definitionKey = verifyExecution.getDefinitionKey(execution);
        switch (definitionKey) {
            case INSTANTIATE_DEFINITION_KEY:
                return HelmReleaseOperationType.INSTANTIATE;
            case TERMINATE_DEFINITION_KEY:
                return HelmReleaseOperationType.TERMINATE;
            case UPGRADE_DEFINITION_KEY:
                return HelmReleaseOperationType.CHANGE_VNFPKG;
            case SCALE_DEFINITION_KEY:
                return HelmReleaseOperationType.SCALE;
            case ROLLBACK_DEFINITION_KEY:
                return HelmReleaseOperationType.ROLLBACK;
            default:
                LOGGER.error(ERROR_UNKNOWN_DEFINITION_KEY + ":" + definitionKey);
                BusinessProcessExceptionUtils
                        .handleException(BPMN_INVALID_ARGUMENT_EXCEPTION, ERROR_UNKNOWN_DEFINITION_KEY, execution);
                return null;
        }
    }
}
