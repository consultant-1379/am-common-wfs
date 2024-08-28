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

import static com.ericsson.amcommonwfs.utilities.Utils.getHost;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public final class TestConstants {

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CHANGE_NUMBER = System.getProperty("GERRIT_CHANGE_NUMBER", "12345");
    public static final String FULL_NAME_V2_SQL = "wfs-accept-v2-mysql-" + CHANGE_NUMBER;
    public static final String RELEASE_NAME_V2_SPIDERAPP_BAD = "wfs-accept-v2-spiderapp-bad-";
    public static final String RELEASE_NAME_V2_SPIDERAPP = "wfs-accept-v2-spider-";
    public static final String RELEASE_NAME_V2_SCALEOUT = "scaleout-spiderapp-";
    public static final String RELEASE_NAME_V2_SKIP_VERIFICATION_SPIDERAPP = "wfs-accept-v2-spiderapp-skip-validation-";
    public static final String FULL_NAME_V2_SPIDERAPP_BAD = RELEASE_NAME_V2_SPIDERAPP_BAD + CHANGE_NUMBER;
    public static final String FULL_SCALEOUT_NAME = RELEASE_NAME_V2_SCALEOUT + CHANGE_NUMBER;
    public static final String FULL_NAME_V2_SPIDERAPP = RELEASE_NAME_V2_SPIDERAPP + CHANGE_NUMBER;
    public static final String FULL_NAME_V2_SPIDERAPP_ANNOTATIONS = "annotations-spiderapp-" + CHANGE_NUMBER;
    public static final String FULL_NAME_V2_SPIDERAPP_YAML = "wfs-accept-spider-yaml-" + CHANGE_NUMBER;
    public static final String FAILED_UPGRADE_MISSING_PARAMETERS = "wfs-accept-v2-upgrade-missing-parameters-" + CHANGE_NUMBER;
    public static final String FAILED_UPGRADE_FAILED_VERIFICATION =
            "wfs-accept-v2-upgrade-fail-verify-" + CHANGE_NUMBER;
    public static final String FULL_NAME_V2_SKIP_VERIFICATION_SPIDERAPP = RELEASE_NAME_V2_SKIP_VERIFICATION_SPIDERAPP + CHANGE_NUMBER;
    public static final String KUBE_GET_DEPLOYMENTS = "kubectl get deployments --all-namespaces ";
    public static final String KUBE_GET_PODS = "kubectl get pods --all-namespaces ";
    public static final String KUBE_GET_PVC = "kubectl get pvc --all-namespaces ";
    public static final String RELEASE = "-l release=%s ";
    public static final String JSON_PATH = "-o jsonpath=";
    public static final String VERIFY_DELETE_COMMAND = KUBE_GET_DEPLOYMENTS + RELEASE;
    public static final String VERIFY_NAMESPACE_DELETED = "kubectl get namespace %s";
    public static final String IGNORE_K8S_WARNINGS = " 2>/dev/null";
    public static final String VERIFY_RUNNING_PODS_COMMAND =
            KUBE_GET_PODS + RELEASE + "--field-selector=status" + ".phase=Running " + IGNORE_K8S_WARNINGS + " |  grep -Eo \"Running\" |wc -l";
    public static final String VERIFY_COMPLETED_PODS_COMMAND =
            KUBE_GET_PODS + RELEASE + IGNORE_K8S_WARNINGS + " | grep -Eo \"Completed\" |wc -l";
    public static final String VERIFY_COMMAND =
            KUBE_GET_DEPLOYMENTS + RELEASE + JSON_PATH + "'{.items[0].status" + ".availableReplicas}'" + IGNORE_K8S_WARNINGS;
    public static final String VERIFY_READY_COMMAND =
            KUBE_GET_PODS + RELEASE + "-o $'jsonpath={range .items[*]}\\n{.status.conditions[1].status}' " + IGNORE_K8S_WARNINGS + " | grep True "
                    + "| wc -l";
    public static final String GET_PODS_BY_RELEASE = KUBE_GET_PODS + RELEASE + IGNORE_K8S_WARNINGS;
    public static final String HELM_GET_VALUES_BY_NAMESPACE_AND_RELEASE = "helm get values -n %s %s -o json " + IGNORE_K8S_WARNINGS;
    public static final String HELM_DEL_COMMAND = "helm3 uninstall %s";
    public static final String VERIFY_PVC = KUBE_GET_PVC + RELEASE;
    public static final String HELM_DEL_EXPECTED_ERROR_RESPONSE = "Error: uninstall: Release not loaded: %s: release: not found";
    public static final String HELM_DEL_EXPECTED_SUCCESS_RESPONSE = "release \"%s\" uninstalled";
    public static final String KUBECTL_DEL_CRD_EXPECTED_ERROR_RESPONSE =
            "Error from server (NotFound): customresourcedefinitions.apiextensions.k8s.io \"%s\" not found";
    public static final String KUBECTL_DEL_CRD_EXPECTED_SUCCESS_RESPONSE = "customresourcedefinition.apiextensions.k8s.io \"%s\" deleted";
    public static final String UNEXPECTED_ERROR = "Unexpected exception :: {}";
    public static final String DEFAULT_INSTANTIATE_TIME_OUT = System.getProperty("deploy.timeout", "400");
    public static final String DEFAULT_PVC_DELETE_TIME_OUT = System.getProperty("pvc_delete.timeout", "600");
    public static final String DEFAULT_EXEC_TIME_OUT = System.getProperty("exec.timeout", "120");
    public static final ThreadLocal<CloseableHttpClient> HTTP_CLIENT = ThreadLocal
            .withInitial(() -> HttpClients.createDefault());
    public static final String HOST_V3 = getHost("/api/lcm/v3/resources");
    public static final String HOST3 = getHost("/api/internal");
    public static final String INTERNAL_DOWNSIZING_URI = getHost("/api/internal/kubernetes/pods/scale/down");
    public static final String HOST_PATCH_SECRET = getHost("/api/internal/kubernetes/secrets");

    public static final String JSON_TO_OBJECT_ERROR_MESSAGE = "Failed to convert json string to object :: {} ";
    public static final String JSON_TO_STRING_ERROR_MESSAGE = "Failed to convert object to json string :: {} ";
    public static final String NO_RESOURCES_FOUND = "No resources found";
    public static final String POST_REQUEST_COMPLETED = "Post request to {} application completed";
    public static final String GET_REQUEST_COMPLETED = "Get request to {} application completed";
    public static final String PUT_REQUEST_COMPLETED = "Put request to {} application completed";
    public static final String POST_REQUEST_FAILED = "Post request to {} application failed as expected";
    public static final String INSTANCE = "instance";
    public static final String INSTANTIATE = "instantiate";
    public static final String UPGRADE = "upgrade";

    public static final String FAILED_APPLICATION = "Failed to %s application";
    public static final String POST_REQUEST = "executing post request to {} application";
    public static final String PUT_REQUEST = "executing put request to {} application";
    public static final String GET_REQUEST = "executing get request to {}";
    public static final String ACCEPT = "Accept";
    public static final String CONTENT_TYPE = "Content-type";
    public static final String MULTI_PART_BOUNDARY = "---vbrhje";
    public static final String MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY = String.format("%s;boundary=%s",
            ContentType.MULTIPART_FORM_DATA, MULTI_PART_BOUNDARY);
    public static final String GET_FIRST_PVC_NAME_FROM_NAMESPACE = "kubectl get pvc -n %s -l release=%s " + IGNORE_K8S_WARNINGS + " | awk '{"
            + " print $1 }' | sed -n 2p";
    public static final String GET_LABEL_FROM_PVC = "kubectl describe pvc %s -n %s " + IGNORE_K8S_WARNINGS + " | grep "
            + "\"Labels:\" | awk '{ print $2 }'";
    public static final String KUBE_GET_PVC_BY_NAME = "kubectl get pvc %s -n %s " + IGNORE_K8S_WARNINGS;
    public static final String NOT_FOUND = "NotFound";
    public static final String DEFAULT_CLUSTER = "default";
    public static final String CLUSTER_CONFIG = "clusterConfig";
    public static final String JSON = "json";

    private TestConstants() {
    }
}
