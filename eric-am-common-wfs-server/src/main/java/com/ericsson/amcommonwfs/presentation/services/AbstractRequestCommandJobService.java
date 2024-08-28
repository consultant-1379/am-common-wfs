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
package com.ericsson.amcommonwfs.presentation.services;

import brave.Span;
import brave.Tracing;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.model.ClusterConfigFileContext;
import com.ericsson.amcommonwfs.model.AsyncRequestDetails;
import com.ericsson.amcommonwfs.model.WorkingAsyncRequestDetails;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ericsson.amcommonwfs.util.Utility.serializeCurrentTracingContext;

@Slf4j
public abstract class AbstractRequestCommandJobService<T extends AsyncRequestDetails> {

    private static final String WORKING_QUEUE_HASH_KEY_DELIMITER = "_";

    private final String mutexQueue;
    private final String mainQueueName;
    private final String workingQueueName;
    private final String workingQueueTimeoutsName;
    private final Integer asyncRequestRecoveryTimeout;
    private final RedisTemplate<String, T> requestRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, WorkingAsyncRequestDetails<T>> workingQueueRedisTemplate;
    private final ClusterConfigService clusterConfigService;
    private final Tracing tracing;


    protected AbstractRequestCommandJobService(String mutexQueue, String mainQueueName, String workingQueueName,
                                               String workingQueueTimeoutsName, Integer asyncRequestRecoveryTimeout,
                                               RedisTemplate<String, T> requestRedisTemplate,
                                               RedisTemplate<String, String> redisTemplate,
                                               RedisTemplate<String, WorkingAsyncRequestDetails<T>> workingQueueRedisTemplate,
                                               ClusterConfigService clusterConfigService,
                                               Tracing tracing) {
        this.mutexQueue = mutexQueue;
        this.requestRedisTemplate = requestRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.mainQueueName = mainQueueName;
        this.workingQueueName = workingQueueName;
        this.workingQueueTimeoutsName = workingQueueTimeoutsName;
        this.asyncRequestRecoveryTimeout = asyncRequestRecoveryTimeout;
        this.workingQueueRedisTemplate = workingQueueRedisTemplate;
        this.clusterConfigService = clusterConfigService;
        this.tracing = tracing;
    }

    public void submitRequest(T requestObject) {
        requestObject.setTracingContext(serializeCurrentTracingContext(tracing));
        requestRedisTemplate.opsForList().leftPush(mainQueueName, requestObject);
        LOGGER.info("Submitted async request for process {}", requestObject);
    }

    @Scheduled(fixedDelay = 500L)
    @Observed
    public void handleQueue() {
        handleOutdatedWorkingItems();
        handleOutdatedWorkingHashItems();

        try {
            T requestObject = requestRedisTemplate.opsForList().move(mainQueueName, RedisListCommands.Direction.RIGHT,
                    workingQueueName, RedisListCommands.Direction.LEFT);

            if (requestObject != null) {
                String workingRequestHashKey = putWorkingAsyncRequest(requestObject);

                submitTraceContext(requestObject);
                LOGGER.info("Processing async request {}", requestObject);

                ClusterConfigFileContext configFileContext = requestObject.getClusterConfigFileContext();
                String configFilePath = clusterConfigService.saveClusterConfig(configFileContext.getOriginalFileName(),
                        configFileContext.getFileContent());

                if (processCommandAsync(configFilePath, requestObject)) {
                    LOGGER.info("Cleaning async request {}", requestObject);
                    requestRedisTemplate.opsForList().remove(workingQueueName, 1, requestObject);
                    workingQueueRedisTemplate.opsForHash().delete(workingQueueTimeoutsName, workingRequestHashKey);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception during request handling", ex);
        }
    }


    protected abstract boolean processCommandAsync(String configFilePath, T requestObject);


    private void handleOutdatedWorkingHashItems() {
        Instant currentInstant = Instant.now();
        boolean isHandlingOutdatedAllowedForLock = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(mutexQueue, "", asyncRequestRecoveryTimeout, TimeUnit.MINUTES));

        if (isHandlingOutdatedAllowedForLock) {
            try {
                workingQueueRedisTemplate.<String, WorkingAsyncRequestDetails<T>>opsForHash()
                        .entries(workingQueueTimeoutsName).entrySet()
                        .stream()
                        .filter(e -> currentInstant.isAfter(Instant.ofEpochSecond(e.getValue().getRequestTimeout())))
                        .forEach(this::pushbackJobTask);
            } finally {
                LOGGER.debug("Unlock handling outdated mutex queue - {}", mutexQueue);
                redisTemplate.opsForValue().getAndDelete(mutexQueue);
            }
        } else {
            LOGGER.debug("Handling outdated`s for {} queue is already being performed by other service",
                    workingQueueName);
        }
    }

    public void handleOutdatedWorkingItems() {
        List<T> workingQueueRequests =  requestRedisTemplate.opsForList().range(workingQueueName, 0, -1);

        if (workingQueueRequests != null) {
            for (T workingRequest : workingQueueRequests) {
                String workingRequestHash = generateHash(workingRequest);
                LOGGER.info("workingQueueRequestsLength={}, workingRequest={}", workingQueueRequests.size(), workingRequest);
                if (!workingQueueRedisTemplate.opsForHash().hasKey(workingQueueTimeoutsName, workingRequestHash)) {
                    LOGGER.info("Add hash to hash working queue {}", workingRequestHash);
                    putWorkingAsyncRequest(workingRequest);
                }
            }
        }
    }

    private String putWorkingAsyncRequest(T requestObject) {
        Long requestExpirationTimestamp = Instant.now().plus(asyncRequestRecoveryTimeout, ChronoUnit.MINUTES).getEpochSecond();
        String hashKey = generateHash(requestObject);

        WorkingAsyncRequestDetails<T> workingAsyncRequestDetails = new WorkingAsyncRequestDetails<>(requestExpirationTimestamp,
                requestObject);
        workingQueueRedisTemplate.opsForHash().putIfAbsent(workingQueueTimeoutsName, hashKey, workingAsyncRequestDetails);
        return hashKey;
    }

    private String generateHash(T requestObject) {
        return requestObject.getNamespace() + WORKING_QUEUE_HASH_KEY_DELIMITER + requestObject.getReleaseName()
                + WORKING_QUEUE_HASH_KEY_DELIMITER + requestObject.getLifecycleOperationId();
    }

    private void pushbackJobTask(Map.Entry<String, WorkingAsyncRequestDetails<T>> requestObjectEntry) {
        LOGGER.info("Pushing back async request object requestObject={}", requestObjectEntry);
        T asyncRequestDetails = requestObjectEntry.getValue().getAsyncRequestDetails();
        requestRedisTemplate.opsForList().rightPush(mainQueueName, asyncRequestDetails);
        requestRedisTemplate.opsForList().remove(workingQueueName, 1, asyncRequestDetails);
        workingQueueRedisTemplate.opsForHash().delete(workingQueueTimeoutsName, requestObjectEntry.getKey());
    }

    private void submitTraceContext(T requestObject) {
        Map<String, String> tracingContextSerialized = requestObject.getTracingContext();
        if (tracingContextSerialized == null || tracingContextSerialized.isEmpty()) {
            LOGGER.warn("Tracing was lost for request in scope of operation {}", requestObject.getLifecycleOperationId());
            tracing.tracer().withSpanInScope(tracing.tracer().nextSpan());
            return;
        }

        Span span = tracing.tracer()
                .toSpan(tracing.propagation().extractor(Map<String, String>::get).extract(tracingContextSerialized).context());
        tracing.tracer().withSpanInScope(span);
    }

}
