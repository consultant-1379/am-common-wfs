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
package com.ericsson.amcommonwfs.utilities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WfsConfigurationEnum {
    EVNFM_HOST(System.getProperty("evnfmHost")),
    WFS_HOST(System.getProperty("container.host")),
    CLUSTER_CONFIG_PATH(System.getProperty("cluster.config")),
    NODE_NAME(System.getProperty("node.name")),
    NAMESPACE(System.getProperty("namespace"));

    private final String property;
}
