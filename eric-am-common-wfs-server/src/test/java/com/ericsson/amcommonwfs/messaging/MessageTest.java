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
package com.ericsson.amcommonwfs.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.util.Constants.WFS_STREAM_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.API_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.workflow.orchestration.mgmt.model.ApiVersion.API_V3;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.ericsson.amcommonwfs.component.VerifyExecution;
import com.ericsson.amcommonwfs.presentation.services.messaging.GenericMessagingService;
import com.ericsson.amcommonwfs.presentation.services.messaging.MessagingHealth;
import com.ericsson.amcommonwfs.presentation.services.messaging.MessagingService;
import com.ericsson.amcommonwfs.presentation.services.messaging.PublishLifecycleMessage;
import com.ericsson.amcommonwfs.util.Constants;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "redis.cluster.enabled=false", "redis.acl.enabled=false" })
@AutoConfigureObservability
public class MessageTest {

    private static final String IDEMPOTENCY_KEY = "dummyKey";

    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/redis:5.0.3-alpine")
                                           .asCompatibleSubstituteFor("redis"))
                    .withExposedPorts(6379);

    static {
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getContainerIpAddress());
        System.setProperty("spring.data.redis.port", redisContainer.getFirstMappedPort().toString());
    }

    private ExecutionEntity execution;

    @Autowired
    private MessagingService messagingService;

    @MockBean
    private MessagingHealth messagingHealth;

    @MockBean
    private VerifyExecution verifyExecution;

    @Autowired
    private PublishLifecycleMessage publishLifecycleMessage;

    @Autowired
    private GenericMessagingService genericMessagingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    public void init() {
        execution = mock(ExecutionEntity.class);
        ReflectionTestUtils.setField(genericMessagingService, "messageRetryTime", "10");
        ReflectionTestUtils.setField(genericMessagingService, "messageRetryInterval", "5");
        ReflectionTestUtils.setField(messagingService, "tracing", null);
    }

    @Test
    public void sendHelmReleaseLifecycleMessage() throws Exception {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("test-release");
        messagingService.sendMessage(message, IDEMPOTENCY_KEY);
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(30);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        when(messagingHealth.isUp()).thenReturn(true);
        when(execution.getVariable(API_VERSION)).thenReturn(API_V3.toString());
        when(execution.getVariable(APP_TIMEOUT)).thenReturn(toEpochSecond);
        when(execution.getVariable(RELEASE_NAME)).thenReturn("release-instantiate-success");
        when(execution.getVariable(REVISION_NUMBER)).thenReturn("1");
        when(execution.getVariable(LIFECYCLE_MESSAGE)).thenReturn("Application deployed successfully");
        when(execution.getVariable(LIFECYCLE_OPERATION_ID)).thenReturn("b08fcbc8-474f-4673-91ee-761fd83991e8");
        when(execution.getSuperExecution()).thenReturn(execution);
        when(execution.getSuperExecution().getCurrentActivityId()).thenReturn("Activity_Publish_Lifecycle_Message_End");
        when(verifyExecution.getDefinitionKey(execution)).thenReturn(INSTANTIATE_DEFINITION_KEY);
        publishLifecycleMessage.execute(execution);
        verifyHelmReleaseMessage();
    }

    @Test
    public void sendWorkflowServiceEventMessage() throws Exception {
        WorkflowServiceEventMessage workflowServiceEventMessage = getWorkflowServiceEventMessageObject();

        when(messagingHealth.isUp()).thenReturn(true);
        genericMessagingService.prepareAndSend(workflowServiceEventMessage, IDEMPOTENCY_KEY);

        verify(messagingHealth, times(1)).isUp();
        verifyWfsMessage();
    }

    @Test
    public void sendWorkflowServiceEventMessageRecovered() throws Exception {
        WorkflowServiceEventMessage workflowServiceEventMessage = getWorkflowServiceEventMessageObject();

        when(messagingHealth.isUp()).thenReturn(false).thenReturn(true);
        genericMessagingService.prepareAndSend(workflowServiceEventMessage, IDEMPOTENCY_KEY);

        verify(messagingHealth, times(2)).isUp();
        verifyWfsMessage();
    }

    @Test
    public void sendWorkflowServiceEventMessageRetryExceed() throws Exception {
        WorkflowServiceEventMessage workflowServiceEventMessage = getWorkflowServiceEventMessageObject();

        when(messagingHealth.isUp()).thenReturn(false);
        genericMessagingService.prepareAndSend(workflowServiceEventMessage, IDEMPOTENCY_KEY);

        verify(messagingHealth, times(2)).isUp();
        verifyMessageNotSent();
    }

    private void verifyHelmReleaseMessage() {
        List<MapRecord<String, String, String>> messages = getMessage();
        for (MapRecord<String, String, String> message : messages) {
            Optional<Class<?>> valueType = getMessageClass(message.getValue().get(Constants.TYPE_ID));
            if (valueType.isPresent()) {
                HelmReleaseLifecycleMessage helmReleaseLifecycleMessage =
                        (HelmReleaseLifecycleMessage) parseMessage(message.getValue().get(Constants.PAYLOAD),
                                                                   valueType.get());
                if ("release-instantiate-success".equals(helmReleaseLifecycleMessage.getReleaseName())) {
                    assertThat(helmReleaseLifecycleMessage.equals(getReleaseLifecycleObject_success())).isTrue();
                } else {
                    HelmReleaseLifecycleMessage lifecycleMessage = new HelmReleaseLifecycleMessage();
                    lifecycleMessage.setReleaseName("test-release");
                    assertThat(helmReleaseLifecycleMessage.equals(lifecycleMessage)).isTrue();
                }
            } else {
                fail("Message typeId not found.");
            }
        }
    }

    private void verifyWfsMessage() {
        List<MapRecord<String, String, String>> messages = getMessage();
        for (MapRecord<String, String, String> message : messages) {
            Optional<Class<?>> valueType = getMessageClass(message.getValue().get(Constants.TYPE_ID));
            if (valueType.isPresent()) {
                WorkflowServiceEventMessage wfsMessage =
                        (WorkflowServiceEventMessage) parseMessage(message.getValue().get(Constants.PAYLOAD),
                                                                   valueType.get());
                assertThat(wfsMessage.equals(getWorkflowServiceEventMessageObject())).isTrue();
            } else {
                fail("Message typeId not found.");
            }
        }
    }

    private void verifyMessageNotSent() {
        List<MapRecord<String, String, String>> messages = getMessage();
        assertThat(messages.isEmpty()).isTrue();
    }

    private List<MapRecord<String, String, String>> getMessage() {
        StreamOperations<String, String, String> operations = redisTemplate.opsForStream();
        List<MapRecord<String, String, String>> records = operations.read(StreamOffset.create(WFS_STREAM_KEY, ReadOffset.from("0")));
        List<String> recordIds = new ArrayList<>();
        for (MapRecord<String, String, String> record : records) {
            recordIds.add(record.getId().getValue());
        }
        if (!recordIds.isEmpty()) {
            redisTemplate.opsForStream().delete(WFS_STREAM_KEY, recordIds.toArray(String[]::new));
        }
        return records;
    }

    private static <T> T parseMessage(final String jsonString,
                                      final Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, valueType);
        } catch (IOException e) {
            fail("Unable to parse json: [%s], because of %s", jsonString, e.getMessage());
        }
        return null;
    }

    private static Optional<Class<?>> getMessageClass(final String typeName) {
        if (typeName.equals(HelmReleaseLifecycleMessage.class.getName())) {
            return Optional.of(HelmReleaseLifecycleMessage.class);
        } else if (typeName.equals(WorkflowServiceEventMessage.class.getName())) {
            return Optional.of(WorkflowServiceEventMessage.class);
        } else {
            fail("Message type unknown for {}", typeName);
        }
        return Optional.empty();
    }

    private HelmReleaseLifecycleMessage getReleaseLifecycleObject_success() {
        HelmReleaseLifecycleMessage lifecycleMessage = new HelmReleaseLifecycleMessage();
        lifecycleMessage.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        lifecycleMessage.setState(HelmReleaseState.COMPLETED);
        lifecycleMessage.setMessage("Application deployed successfully");
        lifecycleMessage.setLifecycleOperationId("b08fcbc8-474f-4673-91ee-761fd83991e8");
        lifecycleMessage.setReleaseName("release-instantiate-success");
        lifecycleMessage.setRevisionNumber("1");
        return lifecycleMessage;
    }

    private WorkflowServiceEventMessage getWorkflowServiceEventMessageObject() {
        WorkflowServiceEventMessage workflowServiceEventMessage = new WorkflowServiceEventMessage("operationId",
                                                                                                  WorkflowServiceEventType.DOWNSIZE,
                                                                                                  WorkflowServiceEventStatus.COMPLETED,
                                                                                                  "Success", "test-release");
        return workflowServiceEventMessage;
    }
}

