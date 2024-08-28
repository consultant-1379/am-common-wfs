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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetPodsTest {

    @Spy
    private GetPods getPods;

    @Test
    public void getPodsWithNamespaceWithRetryShouldReturnAllPodsInFirstIteration() throws IOException, ApiException {
        V1PodList expected = new V1PodList();
        expected.addItemsItem(new V1Pod());

        doReturn(expected, expected).when(getPods).getPodsWithNamespace(anyString(), anyString(), anyString(), anyInt());

        V1PodList actual = getPods.getPodsWithNamespaceWithRetry(NAMESPACE, CLUSTER_NAME, RELEASE_NAME, 1000);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void getPodsWithNamespaceWithRetryShouldReturnAllPodsInSecondIteration() throws IOException, ApiException {
        V1Pod firstPod = new V1Pod();

        V1PodList firstList = new V1PodList();
        firstList.addItemsItem(firstPod);

        V1PodList secondList = new V1PodList();
        secondList.addItemsItem(firstPod);
        secondList.addItemsItem(new V1Pod());

        doReturn(firstList, secondList, secondList).when(getPods).getPodsWithNamespace(anyString(), anyString(), anyString(), anyInt());

        V1PodList actual = getPods.getPodsWithNamespaceWithRetry(NAMESPACE, CLUSTER_NAME, RELEASE_NAME, 1000);

        assertThat(secondList).isEqualTo(actual);
    }

    @Test
    public void getPodsWithNamespaceWithRetryShouldThrowException() throws IOException, ApiException {
        V1Pod firstPod = new V1Pod();

        V1PodList firstList = new V1PodList();
        firstList.addItemsItem(firstPod);

        V1PodList secondList = new V1PodList();
        secondList.addItemsItem(firstPod);
        secondList.addItemsItem(new V1Pod());

        doReturn(firstList, secondList, firstList).when(getPods).getPodsWithNamespace(anyString(), anyString(), anyString(), anyInt());

        assertThatThrownBy(() -> getPods.getPodsWithNamespaceWithRetry(NAMESPACE, CLUSTER_NAME, RELEASE_NAME, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to retrieve all pods from Kubernetes. All pods are not loaded on Kubernetes");
    }

    @Test
    public void getPodsInAllNamespacesWithRetryShouldReturnAllPodsInFirstIteration() throws IOException, ApiException {
        V1PodList expected = new V1PodList();
        expected.addItemsItem(new V1Pod());

        doReturn(expected, expected).when(getPods).getPodsInAllNamespaces(anyString(), anyString(), anyInt());

        V1PodList actual = getPods.getPodsInAllNamespacesWithRetry(NAMESPACE, CLUSTER_NAME, 1000);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void getPodsInAllNamespacesWithRetryShouldReturnAllPodsInSecondIteration() throws IOException, ApiException {
        V1Pod firstPod = new V1Pod();

        V1PodList firstList = new V1PodList();
        firstList.addItemsItem(firstPod);

        V1PodList secondList = new V1PodList();
        secondList.addItemsItem(firstPod);
        secondList.addItemsItem(new V1Pod());

        doReturn(firstList, secondList, secondList).when(getPods).getPodsInAllNamespaces(anyString(), anyString(), anyInt());

        V1PodList actual = getPods.getPodsInAllNamespacesWithRetry(NAMESPACE, CLUSTER_NAME, 1000);

        assertThat(secondList).isEqualTo(actual);
    }

    @Test
    public void getPodsInAllNamespacesWithRetryShouldThrowException() throws IOException, ApiException {
        V1Pod firstPod = new V1Pod();

        V1PodList firstList = new V1PodList();
        firstList.addItemsItem(firstPod);

        V1PodList secondList = new V1PodList();
        secondList.addItemsItem(firstPod);
        secondList.addItemsItem(new V1Pod());

        doReturn(firstList, secondList, firstList).when(getPods).getPodsInAllNamespaces(anyString(), anyString(), anyInt());

        assertThatThrownBy(() -> getPods.getPodsInAllNamespacesWithRetry(NAMESPACE, CLUSTER_NAME, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to retrieve all pods from Kubernetes. All pods are not loaded on Kubernetes");
    }
}
