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
package com.ericsson.amcommonwfs.common;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ericsson.amcommonwfs.utils.KubeClientBuilder;

@Slf4j
@Service
public class EvnfmNamespaceService {

    private static final String DEFAULT = "default";
    @Value("${evnfm.namespace}")
    private String evnfmNamespace;

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    @Autowired
    private RetryTemplate kubectlApiRetryTemplate;

    public boolean checkEvnfmNamespace(String namespaceTarget, String clusterConfig) throws IOException, ApiException {
        if (namespaceTarget.equals(evnfmNamespace)) {
            CoreV1Api currentV1ApiDefaultClient = kubeClientBuilder.getCoreV1Api(DEFAULT);
            Set<String> evnfmNamespacePodIds = getNamespacePods(currentV1ApiDefaultClient, evnfmNamespace);
            CoreV1Api currentV1ApiExternalClient = kubeClientBuilder.getCoreV1Api(clusterConfig);
            Set<String> targetNamespacePodIds = getNamespacePods(currentV1ApiExternalClient, namespaceTarget);
            return evnfmNamespacePodIds.stream().anyMatch(targetNamespacePodIds::contains);
        }
        return false;
    }

    private static String getUid(V1Pod pod) {
        V1ObjectMeta metadata = pod.getMetadata();
        if (metadata == null) {
            throw new RuntimeException("Cannot read pod metadata"); // NOSONAR
        }
        return metadata.getUid();
    }

    private Set<String> getNamespacePods(CoreV1Api currentV1Api, String namespace) throws ApiException {
        return getItems(currentV1Api, namespace).stream()
                .map(EvnfmNamespaceService::getUid).collect(Collectors.toSet());
    }

    private List<V1Pod> getItems(CoreV1Api currentV1Api, String namespace) throws ApiException {
        return getPodList(currentV1Api, namespace)
                .orElseThrow(() -> new ApiException(String.format("Failed to read pod list for namespace %s", namespace)))
                .getItems();
    }

    private Optional<V1PodList> getPodList(CoreV1Api currentV1Api, String namespace) throws ApiException {
        return Optional.ofNullable(kubectlApiRetryTemplate
                .execute(context -> currentV1Api.listNamespacedPod(namespace).execute()));
    }
}
