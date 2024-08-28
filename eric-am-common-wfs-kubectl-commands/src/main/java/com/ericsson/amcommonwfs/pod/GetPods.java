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
package com.ericsson.amcommonwfs.pod;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_INSTANCE_LABEL;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GetPods {

    private static final int SLEEP_INTERVAL_SECONDS = 8;
    private static final int CAMUNDA_ACTIVITY_TIMEOUT = 300;
    private static final String NOT_ALL_PODS_ARE_REGISTERED = "Failed to retrieve all pods from Kubernetes. All pods are not loaded on Kubernetes";

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;

    public V1PodList getPodsWithNamespace(String namespace,
                                   String clusterConfig,
                                   String releaseName,
                                   int applicationTimeOut) throws IOException, ApiException {

        CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
        return kubectlApiRetryTemplate.execute(context -> coreV1Api
                .listNamespacedPod(namespace)
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .timeoutSeconds(applicationTimeOut)
                .execute()
        );
    }

    public V1PodList getPodsInAllNamespaces(String clusterConfig,
                                     String releaseName,
                                     int applicationTimeOut) throws IOException, ApiException {

        CoreV1Api coreV1Api = kubeClientBuilder.getCoreV1Api(clusterConfig);
        return kubectlApiRetryTemplate.execute(context -> coreV1Api
                .listPodForAllNamespaces()
                .labelSelector(String.format(APPLICATION_INSTANCE_LABEL, releaseName))
                .timeoutSeconds(applicationTimeOut)
                .execute()
        );
    }

    public V1PodList getPodsWithNamespaceWithRetry(String namespace,
                                                   String clusterConfig,
                                                   String releaseName,
                                                   int applicationTimeOut) throws IOException, ApiException {

        return pollPods(() -> getPodsWithNamespace(namespace, clusterConfig, releaseName, applicationTimeOut), applicationTimeOut);
    }

    public V1PodList getPodsInAllNamespacesWithRetry(String clusterConfig,
                                                     String releaseName,
                                                     int applicationTimeOut) throws IOException, ApiException {

        return pollPods(() -> getPodsInAllNamespaces(clusterConfig, releaseName, applicationTimeOut), applicationTimeOut);
    }

    private V1PodList pollPods(final PodListSupplier podListSupplier, int applicationTimeOut) throws IOException, ApiException {
        final long timeoutMillis = System.currentTimeMillis() +
                Math.min(applicationTimeOut, CAMUNDA_ACTIVITY_TIMEOUT - SLEEP_INTERVAL_SECONDS - 2) * 1000L;


        int podListSize = podListSupplier.get().getItems().size();
        LOGGER.info("Number of pods from initial call: {}", podListSize);

        for (int i = 0; System.currentTimeMillis() < timeoutMillis; i++) {
            if (!sleepForTime()) {
                throw new IllegalStateException();
            }
            V1PodList currentV1PodList = podListSupplier.get();
            int currentPodListSize = currentV1PodList.getItems().size();
            LOGGER.info("Number of pods from {} additional call: {}", i + 1, currentPodListSize);
            if (podListSize == currentPodListSize) {
                return currentV1PodList;
            }
            podListSize = currentPodListSize;
        }

        throw new ApiException(NOT_ALL_PODS_ARE_REGISTERED);
    }

    private boolean sleepForTime() {
        try {
            TimeUnit.of(ChronoUnit.SECONDS).sleep(SLEEP_INTERVAL_SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread has been interrupted by some other thread during sleeping. Exception details: {}", e.getMessage());
            return false;
        }
    }

    private interface PodListSupplier {
        V1PodList get() throws IOException, ApiException;
    }
}
