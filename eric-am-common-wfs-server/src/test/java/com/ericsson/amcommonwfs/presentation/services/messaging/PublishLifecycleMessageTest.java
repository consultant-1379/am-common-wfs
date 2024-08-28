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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.component.VerifyExecution;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.workflow.orchestration.mgmt.model.ApiVersion;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

@SpringBootTest(classes = PublishLifecycleMessage.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
public class PublishLifecycleMessageTest {

    @MockBean
    private MessagingService messagingService;

    @MockBean
    private MessagingHealth messagingHealth;

    @MockBean
    private VerifyExecution verifyExecution;

    @Autowired
    private PublishLifecycleMessage publishLifecycleMessage;

    @Mock
    private DelegateExecution execution;

    @Mock
    private DelegateExecution superExecution;

    @BeforeEach
    public void setUp() {
        when(messagingHealth.isUp()).thenReturn(true);

        when(execution.getVariable(eq(Constants.API_VERSION))).thenReturn(ApiVersion.API_V3.toString());

        when(execution.getSuperExecution()).thenReturn(superExecution);
        when(superExecution.getCurrentActivityId()).thenReturn("dummy");

        when(verifyExecution.getDefinitionKey(any())).thenReturn(INSTANTIATE_DEFINITION_KEY);
    }

    @Test
    public void shouldNotSetMessageSentWhenSendingFailed() throws Exception {
        // given
        doThrow(new RuntimeException("Sending failed")).when(messagingService).sendMessage(any(), any());

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var inOrder = inOrder(execution);
        inOrder.verify(execution).setVariable(eq(MESSAGE_SENT), eq(false));
        inOrder.verify(execution, never()).setVariable(eq(MESSAGE_SENT), eq(true));
    }

    @Test
    public void shouldNotSetMessageSentWhenMessageServiceIsDownAndRetriesNotCompleted() throws Exception {
        // given
        when(messagingHealth.isUp()).thenReturn(false);
        when(execution.getVariable(eq(MESSAGE_BUS_RETRY_TIME))).thenReturn(LocalDateTime.now().plusSeconds(30).toEpochSecond(ZoneOffset.UTC));

        // when
        publishLifecycleMessage.execute(execution);

        // then
        verify(execution, never()).setVariable(eq(MESSAGE_SENT), eq(true));
        verifyNoInteractions(verifyExecution, messagingService);
    }

    @Test
    public void shouldNotSetMessageSentWhenMessageServiceIsDownAndRetriesCompleted() throws Exception {
        // given
        when(messagingHealth.isUp()).thenReturn(false);
        when(execution.getVariable(eq(MESSAGE_BUS_RETRY_TIME))).thenReturn(LocalDateTime.now().minusSeconds(30).toEpochSecond(ZoneOffset.UTC));

        // when
        publishLifecycleMessage.execute(execution);

        // then
        verify(execution, never()).setVariable(eq(MESSAGE_SENT), eq(true));
        verify(execution).setVariable(eq(MESSAGE_RETRIES_COMPLETED), eq(true));
        verifyNoInteractions(verifyExecution, messagingService);
    }

    @Test
    public void shouldSetMessageSentWhenApiVersion2() throws Exception {
        // given
        when(execution.getVariable(eq(API_VERSION))).thenReturn("not v3");

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var inOrder = inOrder(execution);
        inOrder.verify(execution).setVariable(eq(MESSAGE_SENT), eq(false));
        inOrder.verify(execution).setVariable(eq(MESSAGE_SENT), eq(true));
        verifyNoInteractions(verifyExecution, messagingService);
    }

    @Test
    public void shouldNotSetRevisionInMessageWhenCalledFromErrorEndActivity() throws Exception {
        // given
        when(execution.getVariable(eq(REVISION_NUMBER))).thenReturn("3");
        when(superExecution.getCurrentActivityId()).thenReturn("Activity_Publish_Lifecycle_Message_Error_End");

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(HelmReleaseLifecycleMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getRevisionNumber()).isNull();
    }

    @Test
    public void shouldSetTerminateOperationType() throws Exception {
        // given
        when(verifyExecution.getDefinitionKey(any())).thenReturn(TERMINATE_DEFINITION_KEY);

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(HelmReleaseLifecycleMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getOperationType()).isEqualTo(HelmReleaseOperationType.TERMINATE);
    }

    @Test
    public void shouldSetChangeVnfPkgOperationType() throws Exception {
        // given
        when(verifyExecution.getDefinitionKey(any())).thenReturn(UPGRADE_DEFINITION_KEY);

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(HelmReleaseLifecycleMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getOperationType()).isEqualTo(HelmReleaseOperationType.CHANGE_VNFPKG);
    }

    @Test
    public void shouldSetRollbackOperationType() throws Exception {
        // given
        when(verifyExecution.getDefinitionKey(any())).thenReturn(ROLLBACK_DEFINITION_KEY);

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(HelmReleaseLifecycleMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getOperationType()).isEqualTo(HelmReleaseOperationType.ROLLBACK);
    }

    @Test
    public void shouldSetScaleOperationType() throws Exception {
        // given
        when(verifyExecution.getDefinitionKey(any())).thenReturn(SCALE_DEFINITION_KEY);

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(HelmReleaseLifecycleMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getOperationType()).isEqualTo(HelmReleaseOperationType.SCALE);
    }

    @Test
    public void shouldThrowBpmnErrorOnUnknownDefinitionKey() throws Exception {
        // given
        when(verifyExecution.getDefinitionKey(any())).thenReturn("Unknown");

        // when and then
        assertThatThrownBy(() -> publishLifecycleMessage.execute(execution))
                .isInstanceOf(BpmnError.class);
    }

    @Test
    public void shouldSendGenericEventMessage() throws Exception {
        // given
        when(execution.getVariable(eq(COMMAND_TYPE))).thenReturn("crd");
        when(execution.getVariable(eq(RELEASE_NAME))).thenReturn("releaseName");
        when(execution.getVariable(eq(LIFECYCLE_OPERATION_ID))).thenReturn("lifecycleOperationId");
        when(execution.getVariable(eq(LIFECYCLE_MESSAGE))).thenReturn("lifecycleMessage");

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(WorkflowServiceEventMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message).isEqualTo(new WorkflowServiceEventMessage("lifecycleOperationId",
                                                                      WorkflowServiceEventType.CRD,
                                                                      WorkflowServiceEventStatus.COMPLETED,
                                                                      "lifecycleMessage",
                                                                      "releaseName"));
    }

    @Test
    public void shouldSendGenericEventMessageWithFailedStatus() throws Exception {
        // given
        when(execution.getVariable(eq(COMMAND_TYPE))).thenReturn("crd");
        when(superExecution.getCurrentActivityId()).thenReturn("Activity_Publish_Lifecycle_Message_Error_End");

        // when
        publishLifecycleMessage.execute(execution);

        // then
        final var captor = ArgumentCaptor.forClass(WorkflowServiceEventMessage.class);
        verify(messagingService).sendMessage(captor.capture(), any());
        final var message = captor.getValue();
        assertThat(message.getStatus()).isEqualTo(WorkflowServiceEventStatus.FAILED);
    }
}
