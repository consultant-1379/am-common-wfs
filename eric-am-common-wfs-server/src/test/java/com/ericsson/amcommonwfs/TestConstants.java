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
package com.ericsson.amcommonwfs;

public final class TestConstants {
    public static final String INVALID_YAML_EXCEPTION_MESSAGE_PREFIX = "Unable to parse yaml file due to";
    public static final String HELM_HISTORY_COMMAND = "helm history %s --max 1 -o json";
    public static final String CLUSTER_CONFIG = "clusterConfig.config";
    public static final String VALUES_YAML = "evnfmValues.yaml";

    private TestConstants() {
    }
}
