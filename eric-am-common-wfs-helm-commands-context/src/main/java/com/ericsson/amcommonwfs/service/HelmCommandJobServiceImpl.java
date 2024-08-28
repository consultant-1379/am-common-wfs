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
package com.ericsson.amcommonwfs.service;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.inject.Provider;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_JOBS_CONTEXT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REDIS_KEY_PREFIX;

@Service
@Slf4j
public class HelmCommandJobServiceImpl implements HelmCommandJobService {
    public static final String HELM_JOBS_MAIN_QUEUE = REDIS_KEY_PREFIX + "jobs.mainQueue";
    public static final long HELM_JOB_START_TIMEOUT = 2000L;
    public static final int MAX_FAILED_JOBS = 10;
    public static final String JOB_TYPE_LABEL = "eric-evnfm-job-type=helm";
    public static final String WORKING_QUEUE_TIMEOUTS = "jobs.workingQueueTimeouts";
    public static final String JOB_NAME_PREFIX = "eric-eo-vnfm-helm-executor-job-";
    public static final String HELM_JOB_CONTEXT_KEY_ENV = "REDIS_KEY";
    public static final String EXECUTION_ID_ENV = "PROCESS_INSTANCE_ID";

    public static final String TRACE_ID = "TRACE_ID";

    public static final String DELIMITER = ",";


    private final int maxJobs;
    private final String namespace;
    private final RedisTemplate<String, String> redisTemplate;
    private final KubeClientBuilder kubeClientBuilder;
    private Provider<V1Job> jobTemplateProvider;

    @Autowired
    public HelmCommandJobServiceImpl(@Value("${helmExecutor.maxJobsAllowed}") Integer maxJobs,
                                     @Value("${evnfm.namespace}") String evnfmNamespace,
                                     RedisTemplate<String, String> redisTemplate,
                                     KubeClientBuilder kubeClientBuilder) {
        this.redisTemplate = redisTemplate;
        this.namespace = evnfmNamespace;
        this.kubeClientBuilder = kubeClientBuilder;
        this.maxJobs = Optional.ofNullable(maxJobs).orElse(30);
    }

    @Override
    public void submit(String executionId, String contextKey, String traceId) {
        String contextKeyJoinTraceId = String.join(DELIMITER, contextKey, traceId);
        Boolean notExist = redisTemplate.opsForHash().putIfAbsent(HELM_JOBS_CONTEXT, executionId, contextKeyJoinTraceId);
        if (Boolean.TRUE.equals(notExist)) { // prevent job duplication: add to queue only if job not existed yet
            redisTemplate.opsForList().leftPush(HELM_JOBS_MAIN_QUEUE, executionId);
            LOGGER.info("Submitted job for process {}", executionId);
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Observed
    public void clearCompletedJobs() {
        try {
            BatchV1Api batchV1Api = new BatchV1Api(kubeClientBuilder.getApiClient(null));
            V1JobList jobList = batchV1Api.listNamespacedJob(namespace).labelSelector(JOB_TYPE_LABEL).watch(false).execute();
            if (jobList.getItems() == null) {
                return;
            }
            LOGGER.debug("Found {} jobs total", jobList.getItems().size());
            List<V1Job> completedJobs = jobList.getItems().stream().filter(HelmCommandJobServiceImpl::isJobSucceeded)
                    .collect(Collectors.toList());
            if (!completedJobs.isEmpty()) {
                LOGGER.info("Removing {} succeeded jobs", completedJobs.size());
            }
            deleteJobs(batchV1Api, completedJobs);
            List<V1Job> failedJobs = jobList.getItems().stream().filter(HelmCommandJobServiceImpl::isJobFailed)
                    .collect(Collectors.toList());
            if (!failedJobs.isEmpty()) {
                LOGGER.info("Found {} failed jobs", failedJobs.size());
            }
            if (failedJobs.size() > MAX_FAILED_JOBS) {
                final int exceeding = failedJobs.size() - MAX_FAILED_JOBS;
                LOGGER.info("Removing {} failed jobs", exceeding);
                failedJobs.sort(Comparator.comparing(job -> job.getStatus().getStartTime()));
                deleteJobs(batchV1Api, failedJobs.subList(0, exceeding));
            }
        } catch (ApiException e) {
            LOGGER.warn("Failed to clean up completed jobs: {}", e.getResponseBody(), e);
        } catch (IOException e) {
            LOGGER.warn("Failed to clean up completed jobs:", e);
        }
    }

    private void deleteJobs(BatchV1Api batchV1Api, List<V1Job> completedJobs) throws ApiException {
        for (V1Job job : completedJobs) {
            LOGGER.debug("Deleting job {}", job.getMetadata().getName());
            try {
                batchV1Api.deleteNamespacedJob(job.getMetadata().getName(), namespace).propagationPolicy("Foreground").execute();
            } catch (ApiException e) {
                LOGGER.error("Failed to remove job {}: {}", job.getMetadata().getName(), e.getResponseBody(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 500L)
    @Observed
    public void handleQueue() {
        long timestamp = System.currentTimeMillis();
        handleOutdatedWorkitems(timestamp);
        if (redisTemplate.opsForList().size(HELM_JOBS_MAIN_QUEUE) == 0) {
            return;
        }
        try {
            BatchV1Api batchV1Api = new BatchV1Api(kubeClientBuilder.getApiClient(null));
            if (!canStartJob(batchV1Api)) {
                LOGGER.info("Can't start next job right now, sleeping");
                return;
            }
            String executionId = redisTemplate.opsForList().rightPop(HELM_JOBS_MAIN_QUEUE);
            if (executionId != null) { // Successfully get a task to start job
                String timeout = Long.valueOf(timestamp + HELM_JOB_START_TIMEOUT).toString();
                redisTemplate.<String, String>opsForHash().put(WORKING_QUEUE_TIMEOUTS, executionId, timeout);
                if (startJob(batchV1Api, executionId)) {
                    redisTemplate.<String, String>opsForHash().delete(WORKING_QUEUE_TIMEOUTS, executionId);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to obtain k8s batch api client:", e);
        }
    }

    private boolean startJob(BatchV1Api batchV1Api, String executionId) {
        final String name = StringUtils.truncate(JOB_NAME_PREFIX + UUID.randomUUID(), 63);
        try {
            V1Job job = jobTemplateProvider.get();
            final String contextKeyJoinTraceId = redisTemplate.<String, String>opsForHash().get(HELM_JOBS_CONTEXT, executionId);
            int delimiterIndex = contextKeyJoinTraceId.indexOf(DELIMITER);
            String contextKey = contextKeyJoinTraceId.substring(0, delimiterIndex);
            String traceId = contextKeyJoinTraceId.substring(delimiterIndex + 1);
            final V1EnvVar contextEnvVar = new V1EnvVar().name(HELM_JOB_CONTEXT_KEY_ENV).value(contextKey);
            final V1EnvVar executionIdVar = new V1EnvVar().name(EXECUTION_ID_ENV).value(executionId);
            final V1EnvVar traceIdEnvVar = new V1EnvVar().name(TRACE_ID).value(traceId);
            Optional<V1PodTemplateSpec> podTemplateSpec = Optional.of(job).map(V1Job::getSpec).map(V1JobSpec::getTemplate);
            podTemplateSpec.map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getContainers)
                    .map(containers -> containers.get(0))
                    .ifPresent(c -> c.addEnvItem(contextEnvVar).addEnvItem(executionIdVar).addEnvItem(traceIdEnvVar));
            Optional.ofNullable(job.getMetadata()).ifPresent(meta -> meta.name(name));
            LOGGER.info("Starting job {} with context key {} in scope of process {}", name, contextKey, executionId);
            batchV1Api.createNamespacedJob(namespace, job).execute();
            LOGGER.info("Job {} started", name);
            return true;
        } catch (ApiException e) {
            LOGGER.error("Failed to start job {}: {}", name, e.getResponseBody(), e);
            pushbackJobTask(executionId);
        }
        return false;
    }

    private boolean canStartJob(BatchV1Api batchV1Api) {
        try {
            V1JobList jobList = batchV1Api
                .listNamespacedJob(namespace).labelSelector(JOB_TYPE_LABEL).watch(false).execute();
            Predicate<V1Job> isSucceeded = HelmCommandJobServiceImpl::isJobSucceeded;
            final Predicate<V1Job> isJobRunning =  Predicate.not(isSucceeded.or(HelmCommandJobServiceImpl::isJobFailed));
            long runningJobCount = Optional.ofNullable(jobList.getItems()).map(
                items -> items.stream().filter(isJobRunning).count()
            ).orElse(0L);

            return runningJobCount < maxJobs;
        } catch (ApiException e) {
            LOGGER.error("Failed to query jobs on own cluster due to: {}", e.getResponseBody(), e);
            return false;
        }
    }

    private static boolean isJobSucceeded(V1Job job) {
        return job.getStatus() != null && Integer.valueOf(1).equals(job.getStatus().getSucceeded());
    }

    private static boolean isJobFailed(V1Job job) {
        return job.getStatus() != null && Integer.valueOf(1).equals(job.getStatus().getFailed());
    }

    private void handleOutdatedWorkitems(final long timestamp) {
        Map<String, String> workingItems = redisTemplate.<String, String>opsForHash().entries(WORKING_QUEUE_TIMEOUTS);
        workingItems.entrySet().stream()
                .filter(e -> timestamp > Long.parseLong(e.getValue()))
                .map(Map.Entry::getKey)
                .forEach(this::pushbackJobTask);
    }

    private void pushbackJobTask(String executionId) {
        redisTemplate.opsForList().rightPush(HELM_JOBS_MAIN_QUEUE, executionId);
        redisTemplate.opsForHash().delete(WORKING_QUEUE_TIMEOUTS, executionId);
    }

    @Autowired
    public void setJobTemplateProvider(Provider<V1Job> jobTemplateProvider) {
        this.jobTemplateProvider = jobTemplateProvider;
    }
}
