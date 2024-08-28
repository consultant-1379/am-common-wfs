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
package com.ericsson.amcommonwfs.presentation.converter;

import static com.ericsson.amcommonwfs.util.Utility.formatClusterConfigFile;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DEFAULT_HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION_TEMPLATE;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public abstract class AbstractV3ResourceConverter<R, P> implements Converter<R, P> {

    protected final String clusterConfigDir;

    protected final String defaultCommandTimeOut;

    public AbstractV3ResourceConverter(String clusterConfigDir, String defaultCommandTimeOut) {
        this.clusterConfigDir = clusterConfigDir;
        this.defaultCommandTimeOut = defaultCommandTimeOut;
    }

    protected void putMandatoryResourceParams(Map<String, Object> variables, String applicationTimeout, String clusterName) {
        variables.put(APPLICATION_TIME_OUT, StringUtils.isNumeric(applicationTimeout) ? applicationTimeout : defaultCommandTimeOut);
        putClusterConfigParam(clusterName, variables);
    }

    protected void putClusterConfigParam(String clusterName, Map<String, Object> variables) {
        if (!StringUtils.isEmpty(clusterName)) {
            String clusterConfig = formatClusterConfigFile(clusterName, clusterConfigDir);
            variables.put(CLUSTER_NAME, clusterConfig);
        }
    }

    protected void putHelmClientVersionParam(String helmClientVersion, Map<String, Object> variables) {
        String effectiveHelmClientVersion = StringUtils.isNotEmpty(helmClientVersion)
                ? String.format(HELM_CLIENT_VERSION_TEMPLATE, helmClientVersion)
                : DEFAULT_HELM_CLIENT_VERSION;

        variables.put(HELM_CLIENT_VERSION, effectiveHelmClientVersion);
    }

    protected void putIfNotEmptyMap(String key, Map<String, ?> value, Map<String, Object> variables) {
        if (!CollectionUtils.isEmpty(value)) {
            variables.put(key, value);
        }
    }

    protected void putIfNotEmptyString(String key, String value, Map<String, Object> variables) {
        if (!StringUtils.isEmpty(value)) {
            variables.put(key, value);
        }
    }

    protected void putIfNotEmptyList(String key, List<String> value, Map<String, Object> variables) {
        if (!CollectionUtils.isEmpty(value)) {
            variables.put(key, value);
        }
    }
}
