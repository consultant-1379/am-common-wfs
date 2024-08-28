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
package com.ericsson.amcommonwfs.steps;

import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.utilities.CompleteErrorDescription;
import com.ericsson.amcommonwfs.utilities.ResourceHttpResponse;
import com.ericsson.amcommonwfs.utilities.WfsConfigurationEnum;
import com.ericsson.workflow.orchestration.mgmt.model.Pod;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.util.Config;
import io.qameta.allure.Step;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.assertj.core.util.Strings;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ericsson.amcommonwfs.utilities.TestConstants.CHANGE_NUMBER;
import static com.ericsson.amcommonwfs.utilities.TestConstants.CONTENT_TYPE_JSON;
import static com.ericsson.amcommonwfs.utilities.TestConstants.DEFAULT_CLUSTER;
import static com.ericsson.amcommonwfs.utilities.TestConstants.DEFAULT_INSTANTIATE_TIME_OUT;
import static com.ericsson.amcommonwfs.utilities.TestConstants.DEFAULT_PVC_DELETE_TIME_OUT;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FAILED_APPLICATION;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SPIDERAPP;
import static com.ericsson.amcommonwfs.utilities.TestConstants.GET_FIRST_PVC_NAME_FROM_NAMESPACE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.GET_LABEL_FROM_PVC;
import static com.ericsson.amcommonwfs.utilities.TestConstants.GET_REQUEST;
import static com.ericsson.amcommonwfs.utilities.TestConstants.GET_REQUEST_COMPLETED;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HELM_DEL_COMMAND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HELM_DEL_EXPECTED_ERROR_RESPONSE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HELM_DEL_EXPECTED_SUCCESS_RESPONSE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HELM_GET_VALUES_BY_NAMESPACE_AND_RELEASE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HOST3;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HOST_V3;
import static com.ericsson.amcommonwfs.utilities.TestConstants.IGNORE_K8S_WARNINGS;
import static com.ericsson.amcommonwfs.utilities.TestConstants.INSTANCE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.INSTANTIATE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.KUBECTL_DEL_CRD_EXPECTED_ERROR_RESPONSE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.KUBECTL_DEL_CRD_EXPECTED_SUCCESS_RESPONSE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.KUBE_GET_PVC_BY_NAME;
import static com.ericsson.amcommonwfs.utilities.TestConstants.MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY;
import static com.ericsson.amcommonwfs.utilities.TestConstants.NOT_FOUND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.NO_RESOURCES_FOUND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.POST_REQUEST;
import static com.ericsson.amcommonwfs.utilities.TestConstants.POST_REQUEST_COMPLETED;
import static com.ericsson.amcommonwfs.utilities.TestConstants.POST_REQUEST_FAILED;
import static com.ericsson.amcommonwfs.utilities.TestConstants.PUT_REQUEST;
import static com.ericsson.amcommonwfs.utilities.TestConstants.PUT_REQUEST_COMPLETED;
import static com.ericsson.amcommonwfs.utilities.TestConstants.UNEXPECTED_ERROR;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_COMMAND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_DELETE_COMMAND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_NAMESPACE_DELETED;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_PVC;
import static com.ericsson.amcommonwfs.utilities.Utils.buildMultiPartBody;
import static com.ericsson.amcommonwfs.utilities.Utils.executeCommand;
import static com.ericsson.amcommonwfs.utilities.Utils.getHttpPost;
import static com.ericsson.amcommonwfs.utilities.Utils.getHttpPut;
import static com.ericsson.amcommonwfs.utilities.Utils.getPmTestAppIngressHostname;
import static com.ericsson.amcommonwfs.utilities.Utils.getPodStatusResponseSuccess;
import static com.ericsson.amcommonwfs.utilities.Utils.handleResponseAndGetResponseObject;
import static com.ericsson.amcommonwfs.utilities.Utils.isValidNumber;
import static com.ericsson.amcommonwfs.utilities.Utils.isValidResult;
import static com.ericsson.amcommonwfs.utilities.Utils.replaceNameSpace;
import static com.ericsson.amcommonwfs.utilities.Utils.writeJsonStringToObject;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.workflow.orchestration.mgmt.model.WorkflowState.COMPLETED;
import static com.ericsson.workflow.orchestration.mgmt.model.WorkflowState.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonWfsAcceptanceTestSteps {
    private static final File CLUSTER_CONFIG = new File(WfsConfigurationEnum.CLUSTER_CONFIG_PATH.getProperty());

    private static final String[] CLUSTER_ROLE_BINDING_PREFIXES = new String[] {
        "eric-pm-server-wfs-accept-spider-yaml-",
        "eric-pm-server-wfs-accept-spider-yaml-",
        "eric-pm-server-wfs-accept-v2-spiderapp-",
        "eric-pm-server-wfs-accept-v2-upgrade-missing-parameters-",
        "eric-pm-server-wfs-accept-v2-upgrade-fail-verify-",
        "eric-pm-server-annotations-spiderapp-",
        "eric-pm-server-scaleout-spiderapp-"
    };

    @Step("Verify {0} application instantiation request failed")
    public static void verifyFailedInstantiateRequest(final String title,
                                                      final CloseableHttpClient client,
                                                      final String releaseName,
                                                      final String json,
                                                      final String errorMessage,
                                                      final int statusCode) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG, json);
        HttpPost request = getHttpPost(releaseName, body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, HOST_V3, INSTANTIATE);
        CompleteErrorDescription errorMessagesFromResponse = getFailedLifecycleOperationResponse(title, client, request, statusCode);
        assertThat(errorMessagesFromResponse).isNotNull();

        assertThat(errorMessagesFromResponse.getErrorDetails()).extracting("message")
                .contains(errorMessage);
        LOGGER.info(POST_REQUEST_FAILED, title);
    }

    @Step("Verify {0} pod count with timeout")
    public static void verifyPodsWithTimeout(final String title, String releaseName, final CloseableHttpClient client, int timeout,
                                             int expectedCount) {
        StopWatch stopwatch = StopWatch.createStarted();
        int podsCount;
        LOGGER.info("Performing query of pods for release {}\n", releaseName);
        while (stopwatch.getTime(TimeUnit.SECONDS) < timeout) {
            String podResponse = executeGetPodsRequest(title, releaseName, client);
            PodStatusResponse podStatusResponse = getPodStatusResponseSuccess(podResponse);
            List<Pod> pods = podStatusResponse.getPods();
            List<Pod> runningPods = pods
                    .stream()
                    .filter(pod -> "Running" .equalsIgnoreCase(pod.getStatus())).collect(Collectors.toList());
            podsCount = runningPods.size();
            LOGGER.info("Pod count is {} and expected is {}", podsCount, expectedCount);
            if (podsCount == expectedCount) {
                return;
            }
            delay(5000);
        }
        fail(String.format("Expected pod count %s was not reached with timeout %s", expectedCount,
                timeout));
    }

    private static String executeLifecycleOperation(final String title, final CloseableHttpClient client,
                                                    HttpRequestBase request, int code) {
        return getResponse(title, client, request, code);
    }

    private static CompleteErrorDescription getFailedLifecycleOperationResponse(final String title, final CloseableHttpClient client,
                                                                                final HttpRequestBase request, final int responseCode) {
        try (CloseableHttpResponse response = client.execute(request)) {
            StatusLine statusLine = response.getStatusLine();
            assertThat(statusLine).as("Empty status").isNotNull();
            int statusCode = statusLine.getStatusCode();
            assertThat(statusCode).isEqualTo(responseCode);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            return writeJsonStringToObject(responseBody, CompleteErrorDescription.class);
        } catch (Exception ex) {
            LOGGER.error(UNEXPECTED_ERROR, ex.getMessage());
            fail(String.format(FAILED_APPLICATION, title));
        }
        return null;
    }
    private static String getResponse(final String title, final CloseableHttpClient client,
                                      final HttpRequestBase request, final int responseCode) {
        try (CloseableHttpResponse response = client.execute(request)) {
            verifyHttpResponse(response, responseCode);
            return new BasicResponseHandler().handleResponse(response);
        } catch (Exception ex) {
            LOGGER.error(UNEXPECTED_ERROR, ex.getMessage());
            fail(String.format(FAILED_APPLICATION, title));
        }
        return null;
    }

    @Step("Verify {0} application instantiation request accepted")
    public static String executeInstantiateRequest(final String title,
                                                   final CloseableHttpClient client,
                                                   final String releaseName,
                                                   final FileSystemResource valuesFilePart,
                                                   final String jsonPart) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity httpEntity = buildMultiPartBody(valuesFilePart, CLUSTER_CONFIG, jsonPart);
        HttpPost request = getHttpPost(releaseName, httpEntity, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, HOST_V3, INSTANTIATE);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application instantiation request accepted")
    public static String executeInstantiateRequest(final String title,
                                                   final CloseableHttpClient client,
                                                   final String releaseName,
                                                   final String jsonPart) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG, jsonPart);
        HttpPost request = getHttpPost(releaseName, body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, HOST_V3, INSTANTIATE);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application instantiation request accepted")
    public static String executeGetPodsRequest(final String title, final String releaseName, final CloseableHttpClient client) {
        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG);
        String host = HOST3 + "/" + releaseName + "/pods?clusterName=" + CLUSTER_CONFIG.getName();
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_OK);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application instantiation request accepted")
    public static String executePostRequest(final String title,
                                            final CloseableHttpClient client,
                                            final String jsonPart,
                                            final String host) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG, jsonPart);
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application {4} request accepted")
    public static String executePostRequest(final String title,
                                            final CloseableHttpClient client,
                                            final String releaseName,
                                            final FileSystemResource valuesFilePart,
                                            final String jsonPart,
                                            final String action) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity httpEntity = buildMultiPartBody(valuesFilePart, CLUSTER_CONFIG, jsonPart);
        HttpPost request = getHttpPost(releaseName, httpEntity, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, HOST_V3, action);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application {4} request accepted")
    public static String executePostRequest(final String title,
                                            final CloseableHttpClient client,
                                            final String releaseName,
                                            final String jsonPart,
                                            final String action) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG, jsonPart);
        HttpPost request = getHttpPost(releaseName, body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, HOST_V3, action);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application {4} request accepted")
    public static String executePutRequest(final String title,
                                           final CloseableHttpClient client,
                                           final String jsonPart,
                                           final String host) {

        LOGGER.info(PUT_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG, jsonPart);
        HttpPut request = getHttpPut(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(PUT_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application {4} request accepted")
    public static String executeTerminateMultipartRequest(final String title,
                                                          final CloseableHttpClient client,
                                                          final String releaseName,
                                                          final String namespace) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG);
        String append = releaseName.contains("?") ? "&" : "?";
        String host = HOST_V3 + "/" + releaseName + "/terminate" + append + "lifecycleOperationId=my-id&state"
                + "=starting&namespace=" + namespace;
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application DELETE request accepted")
    public static String executeDeletePvcRequest(final String title,
                                                 final CloseableHttpClient client,
                                                 final String releaseName,
                                                 final String clusterName,
                                                 final String namespace) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG);
        String host = String
                .format("%s/kubernetes/pvcs/%s/delete?lifecycleOperationId=my-id&state=starting&namespace=%s&clusterName=%s",
                        HOST3, releaseName, namespace, clusterName);
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application DELETE request accepted")
    public static String executeDeletePvcRequestWithLabel(final String title,
                                                          final CloseableHttpClient client,
                                                          final String releaseName,
                                                          final String clusterName,
                                                          final String namespace,
                                                          final String label) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG);
        String host = String
                .format("%s/kubernetes/pvcs/%s/delete?lifecycleOperationId=my-id&state=starting&namespace=%s&clusterName=%s&labels=%s",
                        HOST3, releaseName, namespace, clusterName, label);
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application terminate request accepted")
    public static String executeTerminateJsonRequest(final String title,
                                                     final CloseableHttpClient client,
                                                     final String releaseName,
                                                     final String namespace) {

        LOGGER.info(POST_REQUEST, title);
        String append = releaseName.contains("?") ? "&" : "?";
        String host = HOST_V3 + "/" + releaseName + "/terminate" + append + "lifecycleOperationId=my-id&state"
                + "=starting&namespace=" + namespace;
        HttpPost request = getHttpPost((HttpEntity) null, CONTENT_TYPE_JSON, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify {0} application {4} request accepted")
    public static String executeDeleteNamespaceRequest(final String title,
                                                       final CloseableHttpClient client,
                                                       final String clusterName,
                                                       final String namespace,
                                                       final String releaseName) {

        LOGGER.info(POST_REQUEST, title);
        HttpEntity body = buildMultiPartBody(CLUSTER_CONFIG);
        String host = String
                .format("%s/v2/namespaces/%s/delete?lifecycleOperationId=my-id&releaseName=%s&clusterName=%s",
                        HOST3, namespace, releaseName, clusterName);
        HttpPost request = getHttpPost(body, MULTI_PART_CONTENT_TYPE_WITH_BOUNDARY, host);
        String responseBody = executeLifecycleOperation(title, client, request, HttpStatus.SC_ACCEPTED);
        LOGGER.info(POST_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @SuppressWarnings("unchecked")
    @Step("Verify history query for {0} application by release Name and instanceId")
    public static void verifyHistoryQueryByReleaseNameWithInstanceIdV3(final String title, final CloseableHttpClient client,
                                                                       final ResourceResponseSuccess resourceResponseSuccess,
                                                                       final WorkflowState expectedState) {
        LOGGER.info("Starting Verify history query for {} application by release name", title);
        //Temporary solution until get history v3 has been implemented
        String instanceLink = ((Map<String, String>) resourceResponseSuccess.getLinks()).get(INSTANCE);
        HttpGet request = new HttpGet(instanceLink);
        LOGGER.info("request link {}", instanceLink);
        Stopwatch stopwatch = Stopwatch.createStarted();
        int timeout = Integer.parseInt(DEFAULT_INSTANTIATE_TIME_OUT);
        while (stopwatch.elapsed(TimeUnit.SECONDS) < timeout) {
            try (CloseableHttpResponse response = client.execute(request)) {
                LOGGER.info("request link {}", instanceLink);
                verifyHttpResponse(response, 200);
                ResourceHttpResponse historyResponseObject = handleResponseAndGetResponseObject(
                        response, ResourceHttpResponse.class);
                if (checkWorkflowState(title, historyResponseObject, expectedState)) {
                    return;
                }
            } catch (Exception ex) {
                LOGGER.error(UNEXPECTED_ERROR, ex.getMessage());
                fail(String.format("Failed to get release name %s application with following error %s", title,
                        ex.getMessage()));
            }
            delay(10000);
        }
        fail(String.format("Workflow for release name failed to complete within :: %s ", DEFAULT_INSTANTIATE_TIME_OUT));
    }

    @SuppressWarnings("unchecked")
    @Step("Verify error Output of workflow")
    public static void verifyErrorOutputOfWorkflowV3(final CloseableHttpClient client,
                                                     ResourceResponseSuccess
                                                             resourceResponseSuccess,
                                                     final String errorMessage) {
        String instanceLink = ((Map<String, String>) resourceResponseSuccess.getLinks()).get(INSTANCE);
        HttpGet request = new HttpGet(instanceLink);
        verifyOutput(client, errorMessage, request);
    }

    private static void verifyOutput(final CloseableHttpClient client, final String errorMessage,
                                     final HttpGet request) {
        try (CloseableHttpResponse response = client.execute(request)) {
            verifyHttpResponse(response, HttpStatus.SC_OK);
            ResourceHttpResponse historyResponseObject = handleResponseAndGetResponseObject(response,
                    ResourceHttpResponse.class);
            List<ResourceProcessInstance> workflowQueries = historyResponseObject.getWorkflowQueries();
            ProcessInstance workflowQuery = workflowQueries.get(0);
            LOGGER.info("workflow {} ", workflowQuery);
            assertThat(workflowQuery.getMessage())
                    .withFailMessage("Error message does not match expecting " + workflowQuery.toString() + " to contain " +
                            errorMessage).contains(errorMessage);
        } catch (Exception ex) {
            LOGGER.error(UNEXPECTED_ERROR, ex.getMessage());
            fail(String
                    .format("Failed to get error output for failed workflow instance with error %s", ex.getMessage()));
        }
    }

    @Step("Verify Application is installed")
    public static void verifyAppInstalled(String appName, int expectedReplicas) {
        LOGGER.info("Starting Verify App installed");
        ProcessExecutorResponse processExecutorResponse;
        Stopwatch stopwatch = Stopwatch.createStarted();
        int timeout = Integer.parseInt(DEFAULT_INSTANTIATE_TIME_OUT);
        while (stopwatch.elapsed(TimeUnit.SECONDS) < timeout) {
            processExecutorResponse = executeCommand(String.format(VERIFY_COMMAND, appName));
            String processExecutorResult = processExecutorResponse.getCmdResult();
            LOGGER.info("processExecutorResult: {}", processExecutorResult);
            if (processExecutorResponse.getExitValue() == 0 && !Strings.isNullOrEmpty(processExecutorResult)) {
                LOGGER.info("Command Result: {}", processExecutorResult);
                int availableReplicas = isValidResult(processExecutorResult) ?
                        Ints.tryParse(processExecutorResult) :
                        0;
                assertThat(availableReplicas).isEqualTo(expectedReplicas);
                LOGGER.info("Application launched successfully , deployed instance :: {}", availableReplicas);
                return;
            }
        }
        fail("App has timed out and has not been deployed successfully");
    }

    @Step("Verify Application is installed with correct amount of pods")
    public static void verifyAppInstalledWithCorrectPodNumber(int expectedPods, final String command) {
        LOGGER.info("Starting Verify App installed with correct number of pods");
        ProcessExecutorResponse processExecutorResponse;
        processExecutorResponse = executeCommand(command);
        String processExecutorResult = processExecutorResponse.getCmdResult();
        LOGGER.debug("processExecutorResult: {}", processExecutorResult);
        int availablePods = 0;
        if (processExecutorResponse.getExitValue() == 0 && !Strings.isNullOrEmpty(processExecutorResult)) {
            LOGGER.info("Command Result: {}", processExecutorResult);
            availablePods = isValidNumber(processExecutorResult) ? Ints.tryParse((processExecutorResult)) : 0;
            if (availablePods == expectedPods) {
                LOGGER.info("Application launched successfully , deployed instance :: {}", availablePods);
                return;
            }
        }
        fail(String.format("Number of pods %s, do not match expected pods %s", availablePods, expectedPods));
    }

    @Step("Verify overridden property")
    public static void verifyOverriddenValueSet(InstantiateInfo instantiateInfo, String releaseName) {
        LOGGER.info("Starting verify overridden property");
        String pmTestAppHostname = getPmTestAppIngressHostname(instantiateInfo);
        LOGGER.info("Test app ingress: {}", pmTestAppHostname);
        String getValuesCommand = String.format(
                HELM_GET_VALUES_BY_NAMESPACE_AND_RELEASE, instantiateInfo.getNamespace(), releaseName);
        ProcessExecutorResponse processExecutorResponse = executeCommand(getValuesCommand);
        String helmValues = processExecutorResponse.getCmdResult();
        verifyHelmValuesContainCorrectPmTestAppHostname(helmValues, pmTestAppHostname);
    }

    private static void verifyHelmValuesContainCorrectPmTestAppHostname(
            final String helmValues, final String expectedPmTestAppHostname) {
        try {
            String actualPmTestAppHostname = new JSONObject(helmValues)
                    .getJSONObject("pm-testapp").getJSONObject("ingress").getString("domain");
            assertThat(actualPmTestAppHostname).isEqualTo(expectedPmTestAppHostname);
            LOGGER.info("Property overridden successfully, overridden value :: {} ", helmValues);
        } catch (Exception e) {
            String message = "Failed to verify overridden property";
            LOGGER.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Step("Verify namespace deleted")
    public static void verifyNamespace(String namespace) {
        delay(10000);
        LOGGER.info("Starting verify namespace deleted");
        ProcessExecutorResponse processExecutorResponse = executeCommand(String.format(VERIFY_NAMESPACE_DELETED,
                namespace));
        String commandError = processExecutorResponse.getCmdResult();
        assertThat(commandError).contains("Error from server (NotFound):");
        LOGGER.info("Namespace deleted successfully with result {}", commandError);
    }

    @Step("Verify Application was deleted successfully")
    public static void verifyAppDeleted(String appName) {
        LOGGER.info("Starting verify Application was deleted successfully");
        delay(3000);
        ProcessExecutorResponse processExecutorResponse = executeCommand(String.format(VERIFY_DELETE_COMMAND, appName));
        assertThat(processExecutorResponse.getCmdResult()).contains("No resources found");
        LOGGER.info("Application was Deleted Successfully");
    }

    @Step("Verify application was upgraded successfully")
    public static void verifyAppUpgraded(final String releaseName, final String nameSpace) {
        ProcessExecutorResponse response = executeHistoryCommand(releaseName, nameSpace);
        assertThat(response.getCmdResult()).endsWith("Upgrade complete");
    }

    @Step("Verify application was scaled successfully")
    public static void verifyAppScaled(final String releaseName, final String namespace) {
        verifyAppUpgraded(releaseName, namespace);
    }

    @Step("Verify pod status")
    public static void verifyPodStatus(final PodStatusResponse response, final int expectedPods, final String expectedStatus) {
        List<Pod> pods = response.getPods();
        assertThat(pods.size()).isEqualTo(expectedPods);
        for (Pod pod : pods) {
            assertThat(pod.getStatus()).isEqualTo(expectedStatus);
        }
        LOGGER.info("Application verified with correct number of pods :: {} all in Running phase",
                pods.size());
    }

    @Step("Verify application was rolled back successfully")
    public static void verifyAppRollback(final String json, final String releaseName, final String nameSpace) throws IOException {
        ProcessExecutorResponse response = executeHistoryCommand(releaseName, nameSpace);
        String revisionNumber = getVariable(json, REVISION_NUMBER);
        assertThat(response.getCmdResult()).endsWith("Rollback to " + revisionNumber);
    }

    @Step("Verify CRD application upgrade was skipped")
    public static void verifyAppNotUpgraded(final String releaseName, final String namespace, final int expectedNumberOfRevisions) {
        ProcessExecutorResponse listReleaseCommandResponse = executeHistoryCommand(releaseName, namespace);
        List<String> cmdResultLines = listReleaseCommandResponse.getCmdResult().lines().collect(Collectors.toUnmodifiableList());
        assertThat(cmdResultLines).hasSize(expectedNumberOfRevisions + 1); // add 1 for output header
    }

    @Step("Teardown ensure exporter app has been removed")
    public static void teardownStep(String appName) {
        ProcessExecutorResponse processExecutorResponse = executeCommand(String.format(HELM_DEL_COMMAND, appName));
        String cmdResult = processExecutorResponse.getCmdResult();
        String errorResponse = String.format(HELM_DEL_EXPECTED_ERROR_RESPONSE, appName);
        String successResponse = String.format(HELM_DEL_EXPECTED_SUCCESS_RESPONSE, appName);
        assertThat(cmdResult.split(System.lineSeparator())).containsAnyElementsOf(
                Arrays.asList(errorResponse, successResponse));
    }

    @Step("Delete namespaces")
    public static void deleteNamespaces(int minPrefixIndex, int maxPrefixIndex) {
        for (int prefixIndex = minPrefixIndex; prefixIndex <= maxPrefixIndex; prefixIndex++) {
            final String namespace = replaceNameSpace("wfs-acceptance-v2-UNIQUE_NAME", "test" + prefixIndex);
            LOGGER.info("Deleting namespace: {}", namespace);
            executeCommand("kubectl delete namespace " + namespace);
        }
    }

    @Step("Delete helm release by name from a give namespace")
    public static void deleteHelmRelease(final String releaseName, final String namespace) {
        ProcessExecutorResponse response = executeCommand(String.format("helm3 uninstall %s -n %s", releaseName, namespace));
        String errorMessage = String.format("Failed to uninstall helm release %s in namespace %s", releaseName, namespace);
        String cmdResult = response.getCmdResult();
        String errorResponse = String.format(HELM_DEL_EXPECTED_ERROR_RESPONSE, releaseName);
        String successResponse = String.format(HELM_DEL_EXPECTED_SUCCESS_RESPONSE, releaseName);
        assertThat(cmdResult.split(System.lineSeparator()))
                .withFailMessage(errorMessage)
                .containsAnyElementsOf(Arrays.asList(errorResponse, successResponse));
    }

    @Step("Delete CRD")
    public static void deleteCrd(final String crd) {
        ProcessExecutorResponse response = executeCommand(String.format("kubectl delete crd %s", crd));
        String errorMessage = String.format("Failed to delete crd %s", crd);
        String cmdResult = response.getCmdResult();
        String errorResponse = String.format(KUBECTL_DEL_CRD_EXPECTED_ERROR_RESPONSE, crd);
        String successResponse = String.format(KUBECTL_DEL_CRD_EXPECTED_SUCCESS_RESPONSE, crd);
        assertThat(cmdResult.split(System.lineSeparator()))
                .withFailMessage(errorMessage)
                .containsAnyElementsOf(Arrays.asList(errorResponse, successResponse));
    }

    @Step("Delete clusterroles & clusterrolebindings")
    public static void deleteClusterRolesAndClusterRoleBindings() {
        String clusterRoleBinding;
        for (String clusterRoleBindinPrefix : CLUSTER_ROLE_BINDING_PREFIXES) {
            clusterRoleBinding = clusterRoleBindinPrefix + CHANGE_NUMBER;
            LOGGER.info("Deleting clusterrolebinding: {}", clusterRoleBinding);
            executeCommand("kubectl delete clusterrolebinding " + clusterRoleBinding);
            LOGGER.info("Deleting clusterrole: {}", clusterRoleBinding);
            executeCommand("kubectl delete clusterrole " + clusterRoleBinding);
        }
    }

    @Step("Verify Persistent Volume claims are left behind")
    public static void verifyPvcExist() {
        LOGGER.info("Verify Persistent volume claims exist");
        delay(3000);
        ProcessExecutorResponse processExecutorResponse = executeCommand(
                String.format(VERIFY_PVC, FULL_NAME_V2_SPIDERAPP));
        if (processExecutorResponse.getCmdResult().contains(NO_RESOURCES_FOUND)) {
            fail("PVC not found");
        }
    }

    @Step("Verify Terminate PVC's is working as intended")
    public static void verifyTerminatePvcWorks(String namespace, CloseableHttpClient client, String title, String releaseName) {
        verifyPvcExist();

        String firstPvcName = runCommand(String.format(GET_FIRST_PVC_NAME_FROM_NAMESPACE, namespace, releaseName));
        String pvcLabel = runCommand(String.format(GET_LABEL_FROM_PVC, firstPvcName, namespace));
        executeDeletePvcRequestWithLabel(title, client, releaseName,
                DEFAULT_CLUSTER, namespace, pvcLabel);
        verifyPvcByNameDeleted(firstPvcName, namespace);

        if (releaseContainsMorePvcs(FULL_NAME_V2_SPIDERAPP)) {
            executeDeletePvcRequest(title, client, releaseName, DEFAULT_CLUSTER, namespace);
            verifyAllPvcsDeleted();
        }
    }

    @Step("Verify whether Persistent Volume claim by name was deleted successfully")
    public static void verifyPvcByNameDeleted(String pvcName, String namespace) {
        LOGGER.info("Verify PVC by name deletion");
        ProcessExecutorResponse processExecutorResponse = executeCommand(String.format(KUBE_GET_PVC_BY_NAME, pvcName, namespace));
        Stopwatch stopwatch = Stopwatch.createStarted();
        int timeout = Integer.parseInt(DEFAULT_PVC_DELETE_TIME_OUT);
        while (stopwatch.elapsed(TimeUnit.SECONDS) < timeout) {
            if (processExecutorResponse.getCmdResult().contains(NO_RESOURCES_FOUND) || processExecutorResponse.getCmdResult().contains(NOT_FOUND)) {
                return;
            }
            processExecutorResponse = executeCommand(String.format(VERIFY_PVC, FULL_NAME_V2_SPIDERAPP));
            delay(2000);
        }
        fail("PVC removal failed to complete within :: %s", DEFAULT_PVC_DELETE_TIME_OUT);
    }

    @Step("Verify whether Persistent Volume claims were deleted successfully")
    public static void verifyAllPvcsDeleted() {
        LOGGER.info("Verify PVC deletion");
        ProcessExecutorResponse processExecutorResponse = executeCommand(String.format(VERIFY_PVC, FULL_NAME_V2_SPIDERAPP));
        Stopwatch stopwatch = Stopwatch.createStarted();
        int timeout = Integer.parseInt(DEFAULT_PVC_DELETE_TIME_OUT);
        while (stopwatch.elapsed(TimeUnit.SECONDS) < timeout) {
            if (processExecutorResponse.getCmdResult().contains(NO_RESOURCES_FOUND)) {
                return;
            }
            processExecutorResponse = executeCommand(String.format(VERIFY_PVC, FULL_NAME_V2_SPIDERAPP));
            delay(2000);
        }
        fail("PVC removal failed to complete within :: %s", DEFAULT_PVC_DELETE_TIME_OUT);
    }

    @Step("Create a secret to be used for testing patch Secret call")
    public static void createSecret(String name, String namespace, String clusterConfig, String key, String value) throws IOException, ApiException {
        CoreV1Api coreV1Api = getCoreV1Api(clusterConfig);
        V1SecretBuilder v1SecretBuilder = new V1SecretBuilder();

        V1ObjectMeta v1SecretMeta = new V1ObjectMeta();
        v1SecretMeta.setName(name);

        Map<String, String> secretData = new HashMap<>();
        secretData.put(key, value);
        V1Secret v1Secret = v1SecretBuilder.withType("generic")
                .withMetadata(v1SecretMeta)
                .withStringData(secretData).build();

        coreV1Api.createNamespacedSecret(namespace, v1Secret).execute();
    }

    @Step("Create a namespace")
    public static void createNamespace(String namespace, String clusterConfig) throws ApiException, IOException {
        CoreV1Api coreV1Api = getCoreV1Api(clusterConfig);
        V1NamespaceBuilder v1NamespaceBuilder = new V1NamespaceBuilder();
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(namespace);
        V1Namespace v1Namespace = v1NamespaceBuilder.withMetadata(v1ObjectMeta).build();
        coreV1Api.createNamespace(v1Namespace).execute();
    }

    @Step("Verify Secret key/value")
    public static void verifySecretKeyValue(String clusterConfig, String namespace, String secretName,
                                            String key, String value) throws IOException, ApiException {
        CoreV1Api coreV1Api = getCoreV1Api(clusterConfig);
        V1Secret v1Secret = coreV1Api.readNamespacedSecret(secretName, namespace).execute();
        Map<String, byte[]> secretData = v1Secret.getData();
        assertThat(secretData.containsKey(key)).isTrue();
        List<String> decodedValues = secretData.values().stream()
                .map(byteArr -> new String(byteArr, StandardCharsets.UTF_8))
                .collect(Collectors.toList());
        assertThat(decodedValues).contains(value);
    }

    @Step("Retrieve Helm versions")
    public static String executeGetHelmVersions(String title, final CloseableHttpClient client) {
        LOGGER.info(GET_REQUEST, title);
        String host = HOST3 + "/helm/versions";
        String responseBody = getResponse(title, client, new HttpGet(host), 200);
        LOGGER.info(GET_REQUEST_COMPLETED, title);
        return responseBody;
    }

    @Step("Verify helm versions")
    public static void verifyHelmVersions(List<String> helmVersionsList) {
        assertThat(helmVersionsList).isNotEmpty();
        assertThat(helmVersionsList).containsOnly("3.8", "3.10", "3.12", "3.13", "3.14", "latest");
    }

    private static CoreV1Api getCoreV1Api(String clusterConfig) throws IOException {
        ApiClient client = StringUtils.isNotEmpty(clusterConfig) && !StringUtils.equalsIgnoreCase(clusterConfig, DEFAULT_CLUSTER) ?
                Config.fromConfig(clusterConfig) : Config.defaultClient();
        return new CoreV1Api(client);
    }

    public static String getVariable(final String json, final String variable) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(json);
        return jsonNode.get(variable).asText();
    }

    private static ProcessExecutorResponse executeHistoryCommand(final String releaseName, final String nameSpace) {
        return executeCommand("helm3 history " + releaseName + " -n " + nameSpace + IGNORE_K8S_WARNINGS);
    }

    private static boolean checkWorkflowState(final String title, final ResourceHttpResponse historyResponseObject,
                                              final WorkflowState expectedState) {
        List<ResourceProcessInstance> workflowQueries = historyResponseObject.getWorkflowQueries();
        ProcessInstance workflowQuery = workflowQueries.get(0);
        WorkflowState workflowState = workflowQuery.getWorkflowState();
        LOGGER.info("workflow query {}", workflowQuery);
        if (workflowState.equals(COMPLETED) || workflowState.equals(FAILED)) {
            verifyWorkflowQueries(historyResponseObject, expectedState);
            LOGGER.info("Finished Verify history query for {} application by instance Id", title);
            return true;
        }
        return false;
    }

    private static void verifyWorkflowQueries(final ResourceHttpResponse historyResponseObject,
                                              final WorkflowState expectedState) {
        List<ResourceProcessInstance> workflowQueries = historyResponseObject.getWorkflowQueries();
        ProcessInstance processInstance = workflowQueries.get(0);
        assertThat(processInstance.getWorkflowState()).as(processInstance.getMessage()).isEqualTo(expectedState);
    }

    @Step("Verify request response is successful")
    private static void verifyHttpResponse(final CloseableHttpResponse response, final int responseCode) {
        LOGGER.info("Verifying response");
        StatusLine statusLine = response.getStatusLine();
        assertThat(statusLine).as("Empty status").isNotNull();
        int statusCode = statusLine.getStatusCode();
        assertThat(statusCode).as("ResponseCode failed = " + response.getStatusLine()).isEqualTo(responseCode);
        LOGGER.info("Response from application successful, response code :: {}", responseCode);
    }

    private static void delay(final long timeInMillis) {
        try {
            LOGGER.debug("Sleeping for {} milliseconds", timeInMillis);
            Thread.sleep(timeInMillis);
        } catch (final InterruptedException e) {
            LOGGER.debug("Caught InterruptedException: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static InternalScaleInfo getInternalScaleInfo(InstantiateInfo instantiateInfo, String releaseName) {
        InternalScaleInfo internalScaleInfo = new InternalScaleInfo();
        internalScaleInfo.setReleaseName(releaseName);
        internalScaleInfo.setNamespace(instantiateInfo.getNamespace());
        internalScaleInfo.setClusterName(DEFAULT_CLUSTER);
        internalScaleInfo.setApplicationTimeOut("300");
        internalScaleInfo.setLifecycleOperationId("sfasfasdf-asdfasd-asdf");
        return internalScaleInfo;
    }

    private static boolean releaseContainsMorePvcs(String releaseName) {
        ProcessExecutorResponse processExecutorResponse = executeCommand(
                String.format(VERIFY_PVC, releaseName));
        return processExecutorResponse.getCmdResult().contains(NO_RESOURCES_FOUND);
    }

    private static String runCommand(String command) {
        delay(3000);
        ProcessExecutorResponse processExecutorResponse = executeCommand(command);
        return processExecutorResponse.getCmdResult();
    }
}
