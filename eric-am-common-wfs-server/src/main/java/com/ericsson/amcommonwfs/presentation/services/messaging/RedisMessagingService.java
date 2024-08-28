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

import static com.ericsson.amcommonwfs.util.Constants.IDEMPOTENCY_KEY;
import static com.ericsson.amcommonwfs.util.Constants.PAYLOAD;
import static com.ericsson.amcommonwfs.util.Constants.TRACING;
import static com.ericsson.amcommonwfs.util.Constants.TYPE_ID;
import static com.ericsson.amcommonwfs.util.Constants.WFS_STREAM_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisMessagingService implements MessagingService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Tracing tracing;

    @Override
    public <T> void sendMessage(final T message, String idempotencyKey) throws JsonProcessingException {

        Map<String, String> messageBody = prepareMessage(message, idempotencyKey);

        StringRecord record = StreamRecords
                .string(messageBody).withStreamKey(WFS_STREAM_KEY);

        RecordId id = redisTemplate.opsForStream().add(record);
        LOGGER.info("Message sent, ID: {}, content: {}", id.getValue(), messageBody.get(PAYLOAD));
    }

    private <T> Map<String, String> prepareMessage(final T message, String idempotencyKey) throws JsonProcessingException {
        HashMap<String, String> messageBody = new HashMap<>();
        String payload = mapper.writeValueAsString(message);
        messageBody.put(PAYLOAD, payload);
        messageBody.put(TYPE_ID, message.getClass().getName());
        messageBody.put(IDEMPOTENCY_KEY, idempotencyKey);
        if (tracing != null) {
            serializeTracingContext().ifPresent(tc -> messageBody.put(TRACING, tc));
        }
        return messageBody;
    }

    private Optional<String> serializeTracingContext() {
        Span span = tracing.tracer().currentSpan();
        Map<String, String> tracingContextSerialized = new HashMap<>();
        TraceContext.Injector<Map<String, String>> injector = tracing.propagation().injector(Map<String, String>::put);
        injector.inject(span.context(), tracingContextSerialized);
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(tracingContextSerialized);
            return Optional.of(jsonString);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to serialize tracing context due to {}", e.getMessage());
            return Optional.empty();
        }
    }
}
