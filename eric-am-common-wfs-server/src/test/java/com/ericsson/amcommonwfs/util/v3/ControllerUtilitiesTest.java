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
package com.ericsson.amcommonwfs.util.v3;

import static com.ericsson.amcommonwfs.util.v3.ControllerUtilities.extractResourceResponse;
import static com.ericsson.amcommonwfs.util.v3.ControllerUtilities.formatLink;
import static com.ericsson.amcommonwfs.util.v3.ControllerUtilities.validateValuesFile;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import static com.ericsson.amcommonwfs.TestConstants.INVALID_YAML_EXCEPTION_MESSAGE_PREFIX;
import static com.ericsson.amcommonwfs.util.Constants.CURRENTLY_SUPPORTED_IS_TEXT_FORMAT;
import static com.ericsson.amcommonwfs.util.Constants.VALUES_FILE_INVALID_FILE_NAME_ERROR_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.amcommonwfs.exception.InstanceServiceException;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.variable.VariableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.util.UnitTestUtils;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(SpringExtension.class)
public class ControllerUtilitiesTest {

    private InputStream fileContent;
    private String VALID_VALUES_FILE_NAME = "values.yaml";
    private String VALID_VALUES_FILE = "valueFiles/values.yaml";
    private String INVALID_VALUES_FILE_NAME = "test+test.config";
    private String INVALID_VALUES_YAML_FILE_CONTENT = "invalidConfig.config";
    private String EMPTY_VALUES_YAML_FILE_NAME = "emptyValues.yaml";
    private String EMPTY_VALUES_YAML_FILE = "valueFiles/emptyValues.yaml";
    private static final String DUMMY_INSTANCE_ID = "dummy_instance_id";
    private static final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private static final String DUMMY_BUSINESS_KEY = "dummy_business_key";
    private static final String DUMMY_URL = "some_website/domain.com";
    private static final String ACTUAL_RESPONSE = DUMMY_URL + "/" + DUMMY_RELEASE_NAME;
    private MultipartFile multipartFile;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private ProcessInstanceWithVariables processInstanceWithVariables;

    @Mock
    private VariableMap variableMap;

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    ResourceResponseSuccess resourceResponseSuccess;

    private List<ProcessInstance> processInstances;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(VALID_VALUES_FILE);
        multipartFile = new MockMultipartFile(VALID_VALUES_FILE_NAME, VALID_VALUES_FILE_NAME, "text/plain",
                fileContent);
    }

    @BeforeEach
    public void init() {
        processInstances = new ArrayList<>();
        processInstances.add(processInstance);
    }

    @Test
    public void validateValuesFilePositivePath() {
        validateValuesFile(multipartFile);
    }

    @Test
    public void validateValuesFileYmlExtension() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(VALID_VALUES_FILE);
        multipartFile = new MockMultipartFile(VALID_VALUES_FILE_NAME, VALID_VALUES_FILE_NAME, "text/plain", fileContent);
        validateValuesFile(multipartFile);
    }

    @Test
    public void validateValuesFileForInvalidFileName() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(INVALID_VALUES_FILE_NAME);
        multipartFile = new MockMultipartFile(INVALID_VALUES_FILE_NAME, INVALID_VALUES_FILE_NAME, "text/plain",
                fileContent);
        assertThatThrownBy(() -> validateValuesFile(multipartFile)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(VALUES_FILE_INVALID_FILE_NAME_ERROR_MESSAGE);
    }

    @Test
    public void validateValuesFileForInvalidFileContent() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(INVALID_VALUES_YAML_FILE_CONTENT);
        multipartFile = new MockMultipartFile(INVALID_VALUES_YAML_FILE_CONTENT, "invalidYaml.yaml", "text/plain",
                fileContent);
        assertThatThrownBy(() -> validateValuesFile(multipartFile)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith(INVALID_YAML_EXCEPTION_MESSAGE_PREFIX);
    }

    @Test
    public void validateValuesFileForInvalidFileContentType() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(EMPTY_VALUES_YAML_FILE);
        multipartFile = new MockMultipartFile(EMPTY_VALUES_YAML_FILE, EMPTY_VALUES_YAML_FILE_NAME, "text/plain",
                fileContent);
        assertThatThrownBy(() -> validateValuesFile(multipartFile)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(CURRENTLY_SUPPORTED_IS_TEXT_FORMAT);
    }

    @Test
    public void testExtractResourceResponseWithNull() {
        ProcessInstanceWithVariables dummyInstance = null;
        assertThatExceptionOfType(InstanceServiceException.class)
                .isThrownBy(() -> {
                    extractResourceResponse(dummyInstance);
                });
    }

    @Test
    public void testExtractResourceResponseWithError() {
        Mockito.when(processInstanceWithVariables.getId()).thenReturn(DUMMY_INSTANCE_ID);
        Mockito.when(processInstanceWithVariables.getVariables()).thenReturn(variableMap);
        Mockito.when(processInstanceWithVariables.getVariables().containsKey(ERROR_MESSAGE)).thenReturn(true);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    extractResourceResponse(processInstanceWithVariables);
                });
    }

    @Test
    public void testExtractResourceResponseWithInstance() {
        Mockito.when(processInstanceWithVariables.getId()).thenReturn(DUMMY_INSTANCE_ID);
        Mockito.when(processInstanceWithVariables.getVariables()).thenReturn(variableMap);
        Mockito.when(processInstanceWithVariables.getVariables().containsKey(DUMMY_BUSINESS_KEY)).thenReturn(true);
        Mockito.when(processInstanceWithVariables.getVariables().get(RELEASE_NAME)).thenReturn(DUMMY_RELEASE_NAME);
        ResourceResponseSuccess resourceResponseSuccess = extractResourceResponse(processInstanceWithVariables);
        assertThat(resourceResponseSuccess.getInstanceId()).isEqualTo(DUMMY_INSTANCE_ID);
        assertThat(resourceResponseSuccess.getReleaseName()).isEqualTo(DUMMY_RELEASE_NAME);
    }

    @Test
    public void testFormatLink() {
        Mockito.when(resourceResponseSuccess.getReleaseName()).thenReturn(DUMMY_RELEASE_NAME);
        StringBuffer stringBuffer = new StringBuffer(DUMMY_URL);
        Mockito.when(httpServletRequest.getRequestURL()).thenReturn(stringBuffer);
        assertThat(formatLink(httpServletRequest, resourceResponseSuccess)).isEqualTo(ACTUAL_RESPONSE);
    }

    @Test
    public void testFormatLinkWithUrlContainsReleaseName() {
        Mockito.when(resourceResponseSuccess.getReleaseName()).thenReturn(DUMMY_RELEASE_NAME);
        StringBuffer stringBuffer = new StringBuffer(ACTUAL_RESPONSE + ".yaml");
        Mockito.when(httpServletRequest.getRequestURL()).thenReturn(stringBuffer);
        assertThat(formatLink(httpServletRequest, resourceResponseSuccess)).isEqualTo(ACTUAL_RESPONSE);
    }
}
