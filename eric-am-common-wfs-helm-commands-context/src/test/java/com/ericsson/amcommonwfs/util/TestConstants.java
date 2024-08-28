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
package com.ericsson.amcommonwfs.util;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY_PREFIX;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY_PREFIX;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_NO_HOOKS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_WAIT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY_PREFIX;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public interface TestConstants {
    String DEFAULT_NAMESPACE = "default";

    String REGISTRY_URL = "testRegistryUrl";

    String REGISTRY_PULL_SECRET = "regcred";

    String CLUSTER_CONFIG_FILE_REDIS_KEY = CLUSTER_CONFIG_CONTENT_KEY_PREFIX + "-"
            + "eee9e730-f51e-4c54-b5c2-6554bad69d00";

    String VALUES_FILE_REDIS_KEY = VALUES_FILE_CONTENT_KEY_PREFIX + "-" +
            "24c92201-62b4-41dc-9fa9-f8021a5c57c2";

    String ADDITIONAL_VALUES_FILE_REDIS_KEY = ADDITIONAL_VALUES_FILE_CONTENT_KEY_PREFIX + "-" +
            "3bc6df72-d96d-4aa9-8995-ceca3e666230";

    String MYSQL_RELEASE_NAME = "test-sql";

    String MYSQL_CHART_VERSION = "0.1.0";

    String HELM_REPO_ROOT = "helm_repo_root";

    String MYSQL_CHART_NAME = "mysql";

    String MYSQL_CHART_URL = "https://charts.helm.sh/stable/mysql.tgz";

    String REVISION_NUMBER_VALUE = "123";

    String EXPECTED_REDIS_JSON_STRING = "{\"version\":\"v1\",\"commandType\":\"install\",\"helmClientVersion\":null,"
            + "\"commandParams\":{\"setFlagValues\":[\"foo=bar\",\"temp=\\\\{hello\\\\, world\\\\}\"],\"releaseName\":\"test-sql\","
            + "\"helmDebug\":false,\"namespace\":\"default\","
            + "\"clusterConfigFileContentKey\":\"CVNFM:WFS:clusterConfigFileContent-eee9e730-f51e-4c54-b5c2-6554bad69d00\","
            + "\"disableOpenapiValidation\":true,\"timeout\":299,\"chartUrl\":\"https://charts.helm.sh/stable/mysql.tgz\","
            + "\"helmRepoRoot\":\"helm_repo_root\"}}";

    Map<String, String> ADDITIONAL_PARAMS_NO_HELM_KEYS
            = ImmutableMap.of("foo", "bar", "temp", "{hello, 'world'}");

    Map<String, String> ADDITIONAL_PARAMS_WITH_HELM_KEYS
            = ImmutableMap.of("foo", "bar", "temp", "{hello, 'world'}",
                              HELM_WAIT_KEY, "true",
                              DISABLE_OPENAPI_VALIDATION, "false",
                              HELM_NO_HOOKS_KEY, "true");

    Map<String, Object> EXECUTION_VARIABLES_NO_CHART_URL = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            CHART_NAME, MYSQL_CHART_NAME,
            VALUES_FILE_CONTENT_KEY, VALUES_FILE_REDIS_KEY,
            ADDITIONAL_VALUES_FILE_CONTENT_KEY, ADDITIONAL_VALUES_FILE_REDIS_KEY,
            CHART_VERSION, MYSQL_CHART_VERSION,
            ADDITIONAL_PARAMS, ADDITIONAL_PARAMS_NO_HELM_KEYS,
            Constant.HELM_REPO, HELM_REPO_ROOT);

    Map<String, Object> EXECUTION_VARIABLES_WITH_CHART_URL = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            CHART_URL, MYSQL_CHART_URL,
            CHART_VERSION, MYSQL_CHART_VERSION,
            ADDITIONAL_PARAMS, ADDITIONAL_PARAMS_NO_HELM_KEYS,
            Constant.HELM_REPO, HELM_REPO_ROOT);

    Map<String, Object> EXECUTION_VARIABLES_NO_ADDITIONAL_PARAMS = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            CHART_URL, MYSQL_CHART_URL,
            CHART_VERSION, MYSQL_CHART_VERSION,
            Constant.HELM_REPO, HELM_REPO_ROOT);

    Map<String, Object> EXECUTION_VARIABLES_BASE = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            CHART_NAME, MYSQL_CHART_NAME,
            REVISION_NUMBER, REVISION_NUMBER_VALUE);

    long EXPECTED_TIMEOUT = 300L;

    String EXPECTED_GLOBAL_REGISTRY_URL = "global.registry.url=testRegistryUrl";
    String EXPECTED_GLOBAL_PULL_SECRET_REGCRED = "global.pullSecret=regcred";

    String EXPECTED_GLOBAL_PULL_SECRET_RELEASE_NAME = "global.pullSecret=test-sql";

    String EXPECTED_IMAGE_CREDENTIALS_REGISTRY_URL = "imageCredentials.registry.url=testRegistryUrl";

    String EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_REGCRED = "global.registry.pullSecret=regcred";

    String EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_RELEASE_NAME = "global.registry.pullSecret=test-sql";

    String EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_REGCRED
            = "imageCredentials.registry.pullSecret=regcred";

    String EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_RELEASE_NAME
            = "imageCredentials.registry.pullSecret=test-sql";

    String EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_REGCRED = "imageCredentials.pullSecret=regcred";

    String EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_RELEASE_NAME = "imageCredentials.pullSecret=test-sql";

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED =
            List.of("foo=bar", "temp=\\{hello\\, world\\}",
                    EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_URL,
                    EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_REGCRED);

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED_NO_ADDITIONAL_PARAMS =
            List.of(EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_URL,
                    EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_REGCRED,
                    EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_REGCRED);

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME =
            List.of("foo=bar", "temp=\\{hello\\, world\\}",
                    EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_URL,
                    EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_RELEASE_NAME);

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME_NO_ADDITIONAL_PARAMS =
            List.of(EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_URL,
                    EXPECTED_GLOBAL_REGISTRY_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET_RELEASE_NAME,
                    EXPECTED_IMAGE_CREDENTIALS_PULL_SECRET_RELEASE_NAME);

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE =
            List.of("foo=bar", "temp=\\{hello\\, world\\}",
                    EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_RELEASE_NAME);

    List<String> EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE_NO_ADDITIONAL_PARAMS =
            List.of(EXPECTED_GLOBAL_REGISTRY_URL,
                    EXPECTED_GLOBAL_PULL_SECRET_RELEASE_NAME);
    List<String> EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY =
            List.of("foo=bar", "temp=\\{hello\\, world\\}");

    Map<String, Object> EXPECTED_BASE_PARAMS = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            Constant.HELM_DEBUG, false);

    Map<String, Object> EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE = Map.of(
            NAMESPACE, DEFAULT_NAMESPACE,
            RELEASE_NAME, MYSQL_RELEASE_NAME,
            CLUSTER_CONFIG_CONTENT_KEY, CLUSTER_CONFIG_FILE_REDIS_KEY,
            Constant.HELM_DEBUG, true);

    Map<String, Object> EXPECTED_CHART_PARAMS_NO_CHART_URL = Map.of(
            CHART_NAME, MYSQL_CHART_NAME,
            CHART_VERSION, MYSQL_CHART_VERSION,
            Constant.HELM_REPO, HELM_REPO_ROOT);

    Map<String, Object> EXPECTED_CHART_PARAMS_WITH_CHART_URL = Map.of(
            CHART_URL, MYSQL_CHART_URL,
            Constant.HELM_REPO, HELM_REPO_ROOT);
}
