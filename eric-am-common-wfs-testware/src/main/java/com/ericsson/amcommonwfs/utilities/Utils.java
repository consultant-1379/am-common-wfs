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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import static com.ericsson.amcommonwfs.utilities.TestConstants.ACCEPT;
import static com.ericsson.amcommonwfs.utilities.TestConstants.CHANGE_NUMBER;
import static com.ericsson.amcommonwfs.utilities.TestConstants.CONTENT_TYPE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.DEFAULT_EXEC_TIME_OUT;
import static com.ericsson.amcommonwfs.utilities.TestConstants.JSON_TO_OBJECT_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.JSON_TO_STRING_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.MULTI_PART_BOUNDARY;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.utility.DataParser;
import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static final String RESPONSE_BODY_IS_NULL_FAILURE_MESSAGES = "ResponseBody is null";

    private Utils() {
    }

    public static <T> T writeJsonStringToObject(final String json, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            LOGGER.error(JSON_TO_OBJECT_ERROR_MESSAGE, e.getMessage());
        }
        return null;
    }

    public static <T> String writeObjectToJsonString(final T resourceInfo) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;
        try {
            json = objectMapper.writeValueAsString(resourceInfo);
        } catch (JsonProcessingException e) {
            LOGGER.error(JSON_TO_STRING_ERROR_MESSAGE, e.getMessage());
        }
        return json;
    }



    @SuppressWarnings(value = "unchecked")
    public static String getPmTestAppIngressHostname(
            final com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo instantiateInfo) {
        Map<String, String> additionalParams = instantiateInfo.getAdditionalParams();
        return additionalParams.get("pm-testapp.ingress.domain");
    }

    public static com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess getV3ResourceResponseSuccess(final String responseBody) {
        assertNotNull(RESPONSE_BODY_IS_NULL_FAILURE_MESSAGES, responseBody);
        com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess workflowResponse =
                writeJsonStringToObject(responseBody, com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess.class);
        assertNotNull("workflowInstanceResource is null", workflowResponse);
        return workflowResponse;
    }


    public static PodStatusResponse getPodStatusResponseSuccess(final String responseBody) {
        assertNotNull(RESPONSE_BODY_IS_NULL_FAILURE_MESSAGES, responseBody);
        PodStatusResponse podStatusResponse = writeJsonStringToObject(responseBody, PodStatusResponse.class);
        assertNotNull("PodStatusResponse is null", podStatusResponse);
        return podStatusResponse;
    }

    public static HelmVersionsResponse getHelmVersionsResponseSuccess(final String responseBody) {
        assertNotNull(RESPONSE_BODY_IS_NULL_FAILURE_MESSAGES, responseBody);
        HelmVersionsResponse podStatusResponse = writeJsonStringToObject(responseBody, HelmVersionsResponse.class);
        assertNotNull("HelmVersionsResponse is null", podStatusResponse);
        return podStatusResponse;
    }


    public static <T> T handleResponseAndGetResponseObject(final CloseableHttpResponse response, Class<T> clazz)
            throws IOException {
        String historyResponseBody = new BasicResponseHandler().handleResponse(response);
        assertNotNull(RESPONSE_BODY_IS_NULL_FAILURE_MESSAGES, historyResponseBody);
        return writeJsonStringToObject(historyResponseBody, clazz);
    }

    public static boolean isValidResult(String result) {
        return !Strings.isNullOrEmpty(result);
    }

    public static boolean isValidNumber(String result) {
        return !Strings.isNullOrEmpty(result) && StringUtils.isNumeric(result);
    }

    public static ProcessExecutorResponse executeCommand(final String command) {
        ProcessExecutor processExecutor = new ProcessExecutor();
        ProcessExecutorResponse processExecutorResponse = new ProcessExecutorResponse();
        int execTimeout = Integer.parseInt(DEFAULT_EXEC_TIME_OUT);
        try {
            processExecutorResponse = processExecutor.executeProcess(command, execTimeout, false);
        } catch (IOException e) {
            failDueToException("unexpected IOException :: {}", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failDueToException("unexpected InterruptedException :: {}", e);
        } catch (CommandTimedOutException e) {
            failDueToException("unexpected CommandTimedOutException :: {}", e);
        }
        return processExecutorResponse;
    }

    public static HttpPost getHttpPost(final String json, final String contentType, String host) {
        HttpPost request = new HttpPost(host);
        createRequest(json, contentType, request);
        return request;
    }

    public static HttpPost getHttpPost(final HttpEntity requestEntity, final String contentType, String host) {
        HttpPost request = new HttpPost(host);
        createRequest(requestEntity, contentType, request);
        return request;
    }

    public static HttpPost getHttpPost(final String releaseName,
                                       final HttpEntity requestEntity,
                                       final String contentType,
                                       final String host,
                                       final String action) {

        HttpPost request = new HttpPost(host + "/" + releaseName + "/" + action);
        createRequest(requestEntity, contentType, request);
        return request;
    }

    public static HttpPut getHttpPut(final String releaseName, final String json, final String contentType,
            final String host, final String action) {
        HttpPut request = new HttpPut(host + "/" + releaseName + "/" + action);
        createRequest(json, contentType, request);
        return request;
    }

    public static HttpPut getHttpPut(final String releaseName,
                                     final HttpEntity requestEntity,
                                     final String contentType,
                                     final String host,
                                     final String action) {

        HttpPut request = new HttpPut(host + "/" + releaseName + "/" + action);
        createRequest(requestEntity, contentType, request);
        return request;
    }
    public static HttpPut getHttpPut(final HttpEntity requestEntity, final String contentType, final String host) {
        HttpPut request = new HttpPut(host);
        createRequest(requestEntity, contentType, request);
        return request;
    }

    public static HttpPut getHttpPut(final String host, final String json, final String contentType) {
        HttpPut request = new HttpPut(host);
        createRequest(json, contentType, request);
        return request;
    }

    public static String getHost(String url) {
        String property = System.getProperty("container.host");
        assertFalse("Host is null/Empty please provide property \"container.host\"", Strings.isNullOrEmpty(property));
        return property + url;
    }

    public static String getJsonAsString(final String resourceName) {
        return DataParser.readFile(resourceName);
    }

    public static String replaceChangeNumber(final String json) {
        return json.replaceAll("gerritchangenumber", CHANGE_NUMBER); //NOSONAR
    }

    public static String replaceNameSpace(final String json, final String uniqueEnding) {
        String nodeName = WfsConfigurationEnum.NODE_NAME.getProperty();
        String name = nodeName == null ? uniqueEnding : uniqueEnding + "-" + nodeName;
        return json.replaceAll("UNIQUE_NAME", name); //NOSONAR
    }

    public static HttpEntity buildMultiPartBody(final FileSystemResource valuesFilePart,
                                                final File clusterConfig,
                                                final String jsonPart) {
        FileBody fileBody = new FileBody(valuesFilePart.getFile());
        StringBody jsonBody = new StringBody(jsonPart, ContentType.TEXT_PLAIN);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setBoundary(MULTI_PART_BOUNDARY);
        builder.addPart(TestConstants.JSON, jsonBody);
        builder.addPart("values", fileBody);
        builder.addPart(TestConstants.CLUSTER_CONFIG, new FileBody(clusterConfig));
        return builder.build();
    }

    public static HttpEntity buildMultiPartBody(final File clusterConfig, final String jsonPart) {
        StringBody jsonBody = new StringBody(jsonPart, ContentType.TEXT_PLAIN);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setBoundary(MULTI_PART_BOUNDARY);
        builder.addPart(TestConstants.JSON, jsonBody);
        builder.addPart(TestConstants.CLUSTER_CONFIG, new FileBody(clusterConfig));
        return builder.build();
    }

    public static HttpEntity buildMultiPartBody(final File clusterConfig) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setBoundary(MULTI_PART_BOUNDARY);
        builder.addPart(TestConstants.CLUSTER_CONFIG, new FileBody(clusterConfig));
        return builder.build();
    }

    private static void createRequest(final String json, final String contentType,
            final HttpEntityEnclosingRequestBase request) {
        String formattedJson = replaceChangeNumber(json);
        try {
            StringEntity entity = new StringEntity(formattedJson);
            request.setEntity(entity);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Unexpected exception setting request Entity :: {}", ex.getMessage());
            fail();
        }
        request.setHeader(ACCEPT, contentType);
        request.setHeader(CONTENT_TYPE, contentType);
    }

    private static void createRequest(final HttpEntity requestEntity, final String contentType, final HttpEntityEnclosingRequestBase request) {
        request.setEntity(requestEntity);
        request.setHeader(ACCEPT, "application/json");
        request.setHeader(CONTENT_TYPE, contentType);
        request.setHeader("Idempotency-key", UUID.randomUUID().toString());
    }

    private static void failDueToException(String errorMessage, final Exception e) {
        LOGGER.error(errorMessage, e.getMessage());
        fail(errorMessage);
    }
}
