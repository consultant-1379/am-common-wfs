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

public final class VerifyTaskConstants {

    public static final String VERIFY_CMD_EXEC_RESULT = "verifyAppResult";

    public static final String APP_TERMINATED = "Application terminated successfully";

    public static final String APP_ERROR = "Application Error.";

    public static final String APP_TIME_OUT = "Verification of the lifecycle operation failed. Please try increasing the applicationTimeOut.";

    public static final String ERROR_UNKNOWN_DEFINITION_KEY = "Unknown workflow definition key";

    public static final String APP_DEPLOYED_SUCCESS = "Application deployed with name ";

    public static final String APP_UPGRADED_SUCCESS = "Application upgraded with name ";

    public static final String APP_ROLLBACK_SUCCESS = "Application rolled back with name ";

    public static final String APP_SCALE_SUCCESS = "Application scaled with name ";

    public static final String PODS_NOT_READY_ERROR_MESSAGE = "Timeout has occurred waiting for Pods to become ready";

    public static final String PODS_NOT_TERMINATED_ERROR_MESSAGE = "Timeout has occurred waiting for Pods to terminate";

    public static final String CONDITION_TIMED_OUT_MESSAGE = "timed out waiting for the condition";

    public static final String NO_MATCHING_RESOURCES_MESSAGE = "no matching resources found";

    public static final String VERIFICATION_ANNOTATION = "evnfm.eo.ericsson.com/post-instantiate-status";

    public static final String CONTAINERS_STATE_ERROR_MESSAGE = "Timeout has occurred waiting for Containers to reach the expected state";

    public static final String INSTANCE_LABEL = "app.kubernetes.io/instance";

    public static final String IS_ANNOTATED = "isAnnotated";

    public static final String NOT_ALL_PODS_ARE_REGISTERED = "Failed to retrieve all pods from Kubernetes. All pods are not loaded on Kubernetes";

    private VerifyTaskConstants() {
    }
}
