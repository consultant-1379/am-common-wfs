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
package com.ericsson.amcommonwfs.exception;

import static com.ericsson.amcommonwfs.util.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.util.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.util.Constants.CLUSTER;
import static com.ericsson.amcommonwfs.util.Constants.CLUSTERS;
import static com.ericsson.amcommonwfs.util.Constants.CONTEXTS;
import static com.ericsson.amcommonwfs.util.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.util.Constants.NAME;
import static com.ericsson.amcommonwfs.util.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.util.Constants.SECRET_KEY;
import static com.ericsson.amcommonwfs.util.Constants.SECRET_VALUE;
import static com.ericsson.amcommonwfs.util.Constants.STATE;
import static com.ericsson.amcommonwfs.util.Constants.USER;
import static com.ericsson.amcommonwfs.util.Constants.USERS;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public enum ParameterErrorMessage {

    DEFINITION_KEY_REQUIRED("definitionKey", "Required field, cannot be left blank"),
    DEFINITION_KEY_EMPTY("definitionKey", "definitionKey field cannot be empty"),
    BUSINESS_KEY_EMPTY("businessKey", "businessKey field cannot be empty"),
    INSTANCE_ID_REQUIRED("instanceId", "Required field, cannot be left blank"),
    INSTANCE_ID_EMPTY("instanceId", "instanceId field cannot be empty"),
    RELEASE_NAME_ERROR_MSG("releaseName", "releaseName must consist of lower case alphanumeric characters or -."
            + " It must start with an alphabetic character, and end with an alphanumeric character"),
    STATE_NULL(STATE, "state cannot be null"),
    LIFECYCLE_OPERATION_ID_NULL(LIFECYCLE_OPERATION_ID, "lifecycleOperationId cannot be null"),
    CHART_OPTIONAL_MANDATORY_VARIABLES("chartName, chartUrl, chartVersion", "If chartUrl property has "
            + "been specified, chartName and chartVersion properties should not be set. Please see API documentation "
            + "for correct usage."),
    CHART_VERSION_PARAM_REQUIRED("chartVersion", "chartVersion is required for CRD chartType"),
    CHART_PARAM_REQUIRED("chartName, chartUrl", "Either chartUrl or chartName is required"),
    MALFORMED_URL(CHART_URL, "chartUrl is not a valid format"),
    CHART_NAME_BLANK_EMPTY(CHART_NAME, "chartName cannot be empty"),
    STATE_BLANK_EMPTY(STATE, "state cannot be empty"),
    LIFECYCLE_OPERATION_ID_BLANK_EMPTY(LIFECYCLE_OPERATION_ID, "lifecycleOperationId cannot be empty"),
    CHART_NAME_BLANK(CHART_NAME, "chartName cannot be blank"),
    CHART_NAME_URL(CHART_NAME, "chartName cannot be a url link"),
    CLUSTER_NAME_ERROR_MSG(Constants.CLUSTER_NAME, "clusterName must consist of alphanumeric characters. " +
            "It can be given as just the name or ending with .config"),
    CLUSTER_NAME_EMPTY(Constants.CLUSTER_NAME, "clusterName cannot be empty"),
    CLUSTER_NAME_NULL(Constants.CLUSTER_NAME, "clusterName cannot be null"),
    REVISION_NUMBER_REQUIRED("revisionNumber", "revisionNumber field cannot be null or blank"),
    REVISION_NUMBER_NUMERIC("revisionNumber", "revisionNumber field must be numeric"),
    KUBE_CONFIG_API_VERSION_REQUIRED("apiVersion", "kube config api version cannot be null"),
    KUBE_CONFIG_API_VERSION_EMPTY("apiVersion", "kube config api version cannot be empty"),
    KUBE_CONFIG_KIND_REQUIRED("kind", "kube config kind cannot be null"),
    KUBE_CONFIG_KIND_EMPTY("kind", "kube config cannot be empty"),
    KUBE_CONFIG_CURRENT_CONTEXT_REQUIRED("current-context", "kube config current-context cannot be null"),
    KUBE_CONFIG_CURRENT_CONTEXT_EMPTY("current-context", "kube config current-context be empty"),
    KUBE_CONFIG_CLUSTERS_REQUIRED(CLUSTERS, "kube config clusters cannot be null"),
    KUBE_CONFIG_CLUSTERS_EMPTY(CLUSTERS, "kube config clusters cannot be empty"),
    KUBE_CONFIG_ONE_CLUSTER_ALLOWED(CLUSTERS, "only one cluster is allowed in kube config file"),
    KUBE_CONFIG_CONTEXT_REQUIRED(CONTEXTS, "kube config contexts cannot be null"),
    KUBE_CONFIG_CONTEXT_EMPTY(CONTEXTS, "kube config contexts cannot be empty"),
    KUBE_CONFIG_ONE_CONTEXT_ALLOWED(CONTEXTS, "only one context is allowed in kube config file"),
    KUBE_CONFIG_USER_REQUIRED(USERS, "kube config users cannot be null"),
    KUBE_CONFIG_USER_EMPTY(USERS, "kube config users cannot be empty"),
    KUBE_CONFIG_ONE_USER_ALLOWED(USERS, "only one user is allowed in kube config file"),
    KUBE_CONFIG_CLUSTER_NAME_REQUIRED(NAME, "kube config cluster name cannot be null"),
    KUBE_CONFIG_CLUSTER_NAME_EMPTY(NAME, "kube config cluster name cannot be empty"),
    KUBE_CONFIG_CLUSTER_DATA_REQUIRED(CLUSTER, "kube config cluster data cannot be null"),
    KUBE_CONFIG_SERVER_DETAILS_REQUIRED("server", "kube config server url cannot be null"),
    KUBE_CONFIG_SERVER_DETAILS_EMPTY("server", "kube config server url cannot be empty"),
    KUBE_CONFIG_CONTEXT_NAME_REQUIRED(NAME, "kube config context name cannot be null"),
    KUBE_CONFIG_CONTEXT_NAME_EMPTY(NAME, "kube config context name cannot be empty"),
    KUBE_CONFIG_CONTEXT_DATA_REQUIRED("context", "kube config context data cannot be null"),
    KUBE_CONFIG_CLUSTER_NAME_REQUIRED_IN_CONTEXT_DATA(CLUSTER, "kube config cluster name cannot be null in context"),
    KUBE_CONFIG_CLUSTER_NAME_EMPTY_IN_CONTEXT_DATA(CLUSTER, "kube config cluster name cannot be empty in context"),
    KUBE_CONFIG_USER_NAME_REQUIRED_IN_CONTEXT(USER, "kube config user name cannot be null in context"),
    KUBE_CONFIG_USER_NAME_EMPTY_IN_CONTEXT(USER, "kube config user name cannot be empty in context"),
    KUBE_CONFIG_USER_NAME_REQUIRED(NAME, "kube config user cannot be null"),
    KUBE_CONFIG_USER_NAME_EMPTY(NAME, "kube config user name cannot be empty"),
    KUBE_CONFIG_USER_DATA_REQUIRED(USER, "kube config user data cannot be null"),
    CHART_URL_CANT_BE_BLANK(CHART_URL, "chart url can't be null or empty"),
    SCALE_RESOURCES_CANT_BE_NULL("scaleResources", "scale resources can't be null"),
    SCALE_RESOURCES_CANT_BE_EMPTY("scaleResources", "scale resources can't be empty"),
    INVALID_CHART_URL_FORMAT(CHART_URL, "chartUrl is not a valid format"),
    NAMESPACE_EMPTY(NAMESPACE, "namespace cannot be empty"),
    NAMESPACE_NULL(NAMESPACE, "namespace cannot be null"),
    SECRET_KEY_EMPTY(SECRET_KEY, "Secret key cannot be empty."),
    SECRET_KEY_NULL(SECRET_KEY, "Secret key cannot be null."),
    SECRET_VALUE_NULL(SECRET_VALUE, "Secret value cannot be null.");

    private final String parameterName;
    private final String message;

    public static ParameterErrorMessage fromString(final String errorName) {
        for (ParameterErrorMessage errorMessage : ParameterErrorMessage.values()) {
            if (errorMessage.name().equalsIgnoreCase(errorName)) {
                return errorMessage;
            }
        }
        return null;
    }

    private static class Constants {
        public static final String CLUSTER_NAME = "clusterName";
    }
}
