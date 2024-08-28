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

import com.ericsson.amcommonwfs.config.RetryTemplateConfig;
import com.ericsson.amcommonwfs.model.RetryProperties;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.KubeClientBuilder;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { EvnfmNamespaceService.class, RetryProperties.class, RetryTemplateConfig.class })
@TestPropertySource(properties = { "evnfm.namespace=evnfm-namespace" })
public class EvnfmNamespaceServiceTest {

    public static final String CLUSTER_NAME = "clusterName";
    private static final String EVNFM_NAMESPACE = "evnfm-namespace";
    @Autowired
    private EvnfmNamespaceService evnfmNamespaceService;

    @Mock
    private CoreV1Api coreV1Api;
    @Mock
    private CoreV1Api.APIlistNamespacedPodRequest namespacedPodRequest;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    @MockBean
    private KubeClientBuilder kubeClientBuilder ;

    @Test
    public void testCheckEvnfmSameNamespaceWithSamePods() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.listNamespacedPod(eq(EVNFM_NAMESPACE))).thenReturn(namespacedPodRequest);
        when(namespacedPodRequest.execute()).thenReturn(buildDefaultPodList());
        boolean result = evnfmNamespaceService.checkEvnfmNamespace(EVNFM_NAMESPACE, CLUSTER_NAME);

        assertTrue(result);
    }

    @Test
    public void testCheckEvnfmSameNamespaceDifferentPods() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.listNamespacedPod(eq(EVNFM_NAMESPACE))).thenReturn(namespacedPodRequest);
        when(namespacedPodRequest.execute())
                .thenReturn(buildDefaultPodList())
                .thenReturn(buildTargetPodList());
        boolean result = evnfmNamespaceService.checkEvnfmNamespace(EVNFM_NAMESPACE, CLUSTER_NAME);

        assertFalse(result);
    }

    @Test
    public void testCheckDifferentNamespace() throws Exception {
        boolean result = evnfmNamespaceService.checkEvnfmNamespace("namespace", CLUSTER_NAME);

        assertFalse(result);
    }

    @Test
    public void testCheckNamespaceFailToReadPodList() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.listNamespacedPod(eq(EVNFM_NAMESPACE))).thenReturn(namespacedPodRequest);
        when(namespacedPodRequest.execute()).thenReturn(null);
        assertThrows(ApiException.class,
                () -> evnfmNamespaceService.checkEvnfmNamespace(EVNFM_NAMESPACE, CLUSTER_NAME));
    }

    @Test
    public void testCheckSameNamespacePodsEmpty() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.listNamespacedPod(eq(EVNFM_NAMESPACE))).thenReturn(namespacedPodRequest);
        when(namespacedPodRequest.execute())
                .thenReturn(buildDefaultPodList())
                .thenReturn(new V1PodList());
        boolean result = evnfmNamespaceService.checkEvnfmNamespace(EVNFM_NAMESPACE, CLUSTER_NAME);

        assertFalse(result);
    }

    @Test
    public void testCheckNamespaceEmptyMetadata() throws Exception {
        when(kubeClientBuilder.getCoreV1Api(anyString())).thenReturn(coreV1Api);
        when(coreV1Api.listNamespacedPod(eq(EVNFM_NAMESPACE))).thenReturn(namespacedPodRequest);
        when(namespacedPodRequest.execute())
                .thenReturn(buildDefaultPodList())
                .thenReturn(buildTargetNullMetadataPodList());

        assertThrows(RuntimeException.class,
                () -> evnfmNamespaceService.checkEvnfmNamespace(EVNFM_NAMESPACE, CLUSTER_NAME));
    }

    private V1PodList buildDefaultPodList() {
        return new V1PodList().items(Arrays.asList(
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-1")),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-2")),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-3"))
                )
        );
    }

    private V1PodList buildTargetPodList() {
        return new V1PodList().items(Arrays.asList(
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-4")),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-5")),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-6"))
                )
        );
    }

    private V1PodList buildTargetNullMetadataPodList() {
        return new V1PodList().items(Arrays.asList(
                        new V1Pod().metadata(null),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-2")),
                        new V1Pod().metadata(new V1ObjectMeta().uid("test-id-3"))
                )
        );
    }
}