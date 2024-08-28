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
package com.ericsson.amcommonwfs.utils.constants;

import java.util.regex.Pattern;

public final class Constants {
    public static final String AUX_SECRET = "auxSecret";
    public static final String TRACING_CONTEXT = "tracingContext";
    public static final String AUX_SECRET_LABEL = "common_eric_eo_secret_aux";
    public static final String COMMAND_OUTPUT = "commandOutput";
    public static final String COMMAND_EXIT_STATUS = "commandExitStatus";
    public static final String COMMAND_TIME_TAKEN = "commandTimeTaken";
    public static final String RELEASE_NAME = "releaseName";
    public static final String STATE = "state";
    public static final String API_VERSION = "apiVersion";
    public static final String LAST_HISTORY = "lastHistory";
    public static final String MAX_HISTORY = "maxHistory";
    public static final String NAMESPACE = "namespace";
    public static final String CHART_VERSION = "chartVersion";
    public static final String REVISION = "revision";
    public static final String DESCRIPTION = "description";
    public static final String CHART_NAME = "chartName";
    public static final String CHART_URL = "chartUrl";
    public static final String CHART_TYPE = "chartType";
    public static final String CRD_VERSION_IN_CLUSTER = "crdVersionInCluster";
    public static final String PROCEED_WITH_CRD_INSTALL = "proceedWithCRDInstallation";
    public static final String RETRY_CRD_INSTALL = "retryCRDInstallation";
    public static final String CLUSTER_NAME = "clusterName";
    public static final String ORIGINAL_CLUSTER_NAME = "originalClusterName";
    public static final String HELM_EXECUTION_ID_KEY = "helmExecutionId";
    public static final String CLUSTER_CONFIG_CONTENT_KEY = "clusterConfigFileContentKey";
    public static final String CLUSTER_CONFIG_CONTENT_KEY_PREFIX = "CVNFM:WFS:clusterConfigFileContent";
    public static final String OVERRIDE_GLOBAL_REGISTRY = "overrideGlobalRegistry";
    public static final String ADDITIONAL_PARAMS = "additionalParams";
    public static final String DAY0_CONFIGURATION = "day0Configuration";
    public static final String REVISION_NUMBER = "revisionNumber";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_CODE = "errorCode";
    public static final String COMMAND = "command";
    public static final String GLOBAL_REGISTRY_SECRET_PRESENT = "globalRegistrySecretPresent";
    public static final String INSTANTIATE_DEFINITION_KEY = "InstantiateApplication__top";
    public static final String UPGRADE_DEFINITION_KEY = "UpgradeApplication__top";
    public static final String ROLLBACK_DEFINITION_KEY = "RollbackApplication__top";
    public static final String TERMINATE_DEFINITION_KEY = "TerminateApplication__top";
    public static final String SCALE_DEFINITION_KEY = "ScaleApplication__top";
    public static final String CRD_DEFINITION_KEY = "CRDApplication__top";
    public static final String PARENT_WORKFLOW_SUB_STRING = "__top";
    public static final String SKIP_VERIFICATION = "skipVerification";
    public static final String PODS_POLLING_CONTINUE = "podsPollingContinue";
    public static final String SKIP_JOB_VERIFICATION = "skipJobVerification";
    public static final String APPLICATION_TIME_OUT = "applicationTimeOut";
    public static final String IS_APPLICATION_TIMED_OUT = "isAppTimedOut";
    public static final String APP_DEPLOYED = "appDeployed";
    public static final String APPLICATION_TERMINATED = "applicationTerminated";
    public static final String RELEASE_NAME_REGEX = "[a-z]([-a-z0-9]*[a-z0-9])?";
    public static final String VALUES_FILE = "valuesFile";
    public static final String REDIS_KEY_PREFIX = "CVNFM:WFS:";
    public static final String VALUES_FILE_CONTENT_KEY = "valuesFileContentKey";
    public static final String VALUES_FILE_CONTENT_KEY_PREFIX = REDIS_KEY_PREFIX + "valuesFileContent";

    public static final String ADDITIONAL_VALUES_FILE_CONTENT_KEY = "additionalValuesFileContentKey";
    public static final String ADDITIONAL_VALUES_FILE_CONTENT_KEY_PREFIX = REDIS_KEY_PREFIX + "additionalValuesFileContent";
    public static final String HELM_JOBS_CONTEXT = REDIS_KEY_PREFIX + "jobs.Context";
    private static final String CLUSTER_CONFIG_PREFIX_REGEX = "[0-9a-zA-Z][0-9a-zA-Z-_]*";
    public static final String REGEX_FOR_CLUSTER_CONFIG_FILE_NAME = CLUSTER_CONFIG_PREFIX_REGEX + "\\.config$";
    public static final Pattern RESOURCE_ALREADY_EXISTS = Pattern.compile(".*a release named (.*) already exists.*"); // NOSONAR
    public static final Pattern RESOURCE_TYPE_ALREADY_EXISTS = Pattern.compile(".*Error: release (.*) failed: (.*) already exists.*"); // NOSONAR
    public static final String ALREADY_EXISTS_FAILURE_REASON = "AlreadyExists";
    public static final String APPLICATION_INSTANCE_LABEL = "app.kubernetes.io/instance=%s";
    public static final String RETRIES_DELAY = "camundaDelay";
    public static final String APP_TIMEOUT = "camundaAppTimeout";
    public static final String APP_TIMED_OUT = "appTimedOut";
    public static final String MESSAGE_SENT = "messageSent";
    public static final String LIFECYCLE_OPERATION_ID = "lifecycleOperationId";
    public static final String LIFECYCLE_MESSAGE = "message";
    public static final String DISABLE_OPENAPI_VALIDATION = "disableOpenapiValidation";
    public static final String MESSAGE_BUS_RETRY_TIME = "messageBusRetryTime";
    public static final String MESSAGE_BUS_RETRY_INTERVAL = "messageBusRetryInterval";
    public static final String MESSAGE_RETRIES_COMPLETED = "messageRetriesCompleted";
    public static final String DEFAULT_NAMESPACE = "default";
    public static final Pattern VERSION_REGEX = Pattern.compile("((\\d+\\.)+\\d*)((-\\d+)|(\\+\\d+))*"); // semantic version followed by
    // unreleased(-)/released(+) build number
    public static final Pattern BUILD_NUMBER_REGEX = Pattern.compile("(-|\\+)");
    public static final String CHART_VERSION_NEWER = "Chart version is newer than version in cluster, setting flag to proceed with upgrade.";
    public static final String CHART_VERSION_OLDER = "Chart version is older than version in cluster, setting flag to skip upgrade.";
    public static final String HELM_VALUES_HEADER = "USER-SUPPLIED VALUES:";
    public static final String HELM_WAIT_KEY = "helmWait";
    public static final String HELM_NO_HOOKS_KEY = "helmNoHooks";
    public static final String HELM_CLIENT_VERSION = "helmClientVersion";
    public static final String HELM_CLIENT_VERSION_TEMPLATE = "helm-%s";
    public static final String DEFAULT_HELM_CLIENT_VERSION = "helm";

    public static final String LIST_COMMAND = "ls";
    public static final String HELM_BINARIES_LOCATION = "/usr/local/bin/";

    public static final String HELM_EXECUTOR_REDIS_KEY = "helmExecutorRedisKey";

    public static final String HELM_EXECUTOR_REDIS_KEY_PREFIX = REDIS_KEY_PREFIX + "helm-executor:";

    public static final String INSTALL = "install";
    public static final String CREATE_NAMESPACE = "createNamespace";
    public static final String ATOMIC = "atomic";
    public static final String EXECUTION_PROCESS_ID = "execution-process-id";

    private Constants() {
    }
}
