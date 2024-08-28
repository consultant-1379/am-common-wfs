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

import java.util.Map;

import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;

public interface HelmService {

    Map<String, Object> getValues(String releaseName, String clusterConfig, String namespace, String timeout);

    HelmVersionsResponse getHelmVersions();
}
