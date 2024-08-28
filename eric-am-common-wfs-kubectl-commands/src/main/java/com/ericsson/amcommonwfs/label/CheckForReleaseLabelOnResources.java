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
package com.ericsson.amcommonwfs.label;

import com.ericsson.amcommonwfs.pod.GetPods;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CheckForReleaseLabelOnResources extends CheckForResourcesBase {

    @Autowired
    public CheckForReleaseLabelOnResources(ClusterFileUtils clusterFileUtils, GetPods getPods) {
        super(clusterFileUtils, getPods);
    }

    @Override
    protected V1PodList getPodsInAllNamespaces(String releaseName, String clusterConfig,
                                               int applicationTimeOut) throws IOException, ApiException {
        return getPods.getPodsInAllNamespacesWithRetry(clusterConfig, releaseName, applicationTimeOut);
    }

    @Override
    protected V1PodList getPodsInNamespace(String releaseName, String namespace, String clusterConfig,
                                           int applicationTimeOut) throws IOException, ApiException {
        return getPods.getPodsWithNamespaceWithRetry(namespace, clusterConfig, releaseName, applicationTimeOut);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
