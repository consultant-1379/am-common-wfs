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

import java.util.regex.Pattern;

public final class Constants {

    public static final String INSTANCE_DETAILS_NOT_FOUND = "Unable to find workflow instance";
    public static final String WITH_ERROR = "with error :: {} ";
    public static final String PROCESS_INSTANCE_NULL = "Unable to start Instance, as ProcessInstance is null.";
    public static final String CHART_NAME = "chartName";
    public static final String CHART_URL = "chartUrl";
    public static final String STATE = "state";
    public static final String LIFECYCLE_OPERATION_ID = "lifecycleOperationId";
    public static final String CLUSTERS = "clusters";
    public static final String CONTEXTS = "contexts";
    public static final String USERS = "users";
    public static final String USER = "user";
    public static final String NAME = "name";
    public static final String CLUSTER = "cluster";
    public static final String NAMESPACE = "namespace";
    public static final String UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE =
            "Unable to parse yaml file due to [%s], Please provide a valid yaml";
    public static final String FILE_NULL_EMPTY_ERROR_MESSAGE = "%s name cannot be empty";
    public static final String CLUSTER_CONFIG_INVALID_FILE_NAME_ERROR_MESSAGE =
            "ClusterConfig name not in correct format";
    public static final String VALUES_FILE_INVALID_FILE_NAME_ERROR_MESSAGE =
            "Values.yaml file name not in correct format. File name cannot contain any of `~!@#$%^&*()_|+-=?;:'\",<>{}[]\\/ and must be .yaml or "
                    + ".yml format.";
    public static final Pattern VALUES_FILE_NAME_PATTERN = Pattern.compile("evnfmvalues.*yaml");
    public static final long VALUES_FILE_AGE_CUTOFF_IN_HOURS = 6;
    public static final String CURRENTLY_SUPPORTED_IS_TEXT_FORMAT =
            "Invalid upload content type. Valid content type that is currently supported is text format";
    public static final String CLUSTER_CONFIG_FILE_EXTENSION = ".config";
    public static final String DIRECTORY_PATH_SEPARATOR = "/";
    public static final String UNABLE_TO_SCALE_DOWN_RESOURCES = "Unable to scale down ReplicaSets or StatefulSets in namespace %s with release "
            + "name %s due to %s ";
    public static final String UNABLE_TO_DELETE_PVCS = "Unable to delete PVCs in namespace %s with release name %s due to %s";
    public static final String UNABLE_TO_PUBLISH_MESSAGE = "Unable to publish the message due to: {}. Retrying to send the message.";
    public static final String DEFAULT = "default";
    public static final String SECRET_KEY = "key";
    public static final String SECRET_VALUE = "value";
    public static final String CONNECTIVITY_TEST_FAILED = "Connectivity test failed, please check your connection to the target cluster: %s";
    public static final String CONNECTIVITY_TEST_KUB_EXCEPTION = "UnknownHostException";
    public static final String DEPLOYMENT_KIND = "Deployment";
    public static final String STATEFULSET_KIND = "Statefulset";
    public static final String FAILED_TO_GET_METADATA_FROM_KUBERNETES_RESOURCE = "Failed to get metadata from %s details";
    public static final String FAILED_TO_GET_STATUS_FROM_KUBERNETES_RESOURCE = "Failed to get status from %s details";
    public static final String FAILED_TO_GET_SPEC_FROM_KUBERNETES_RESOURCE = "Failed to get spec from %s details";
    public static final String FAILED_TO_CHECK_NAMESPACE_IS_EVNFM_NAMESPACE_AND_CLUSTER = "Failed to check if namespace (%s) is used for EVNFM "
            + "deployment and cluster. Details: %s";

    public static final String WFS_STREAM_KEY = "CVNFM:Streams:workflow-events";
    public static final String PAYLOAD = "payload";
    public static final String IDEMPOTENCY_KEY = "Idempotency-key";
    public static final String TYPE_ID = "TypeId";
    public static final String TRACING = "tracing";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-key";

    private Constants() {
    }
}
