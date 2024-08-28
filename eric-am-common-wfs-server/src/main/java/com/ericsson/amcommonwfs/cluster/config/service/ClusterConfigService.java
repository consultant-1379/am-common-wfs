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
package com.ericsson.amcommonwfs.cluster.config.service;

import com.ericsson.amcommonwfs.model.ClusterConfigFileContext;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;

/**
 * Service responsible for any operations with k8s configuration file, such as validation, connectivity test, storing file to drive.
 */
public interface ClusterConfigService {

    void validateConfigFile(MultipartFile clusterConfig);

    /**
     * Check cluster config file for errors in file structure and test cluster connectivity.
     *
     * @param configFile - k8s configuration file
     * @return cluster config server details
     */
    ClusterServerDetailsResponse checkIfConfigFileValid(MultipartFile configFile);

    /**
     * Resolves path to cluster config file specified in parameters.
     * {@link NotFoundException} - if appropriate cluster config file not found.
     *
     * @param clusterName   - name of cluster.
     * @param clusterConfig - cluster config file.
     * @return - path to cluster config file.
     */
    String resolveClusterConfig(String clusterName, MultipartFile clusterConfig);

    String saveClusterConfig(String fileName, String fileContent);

    ClusterConfigFileContext resolveClusterConfigContext(String clusterName, MultipartFile clusterConfig);
}