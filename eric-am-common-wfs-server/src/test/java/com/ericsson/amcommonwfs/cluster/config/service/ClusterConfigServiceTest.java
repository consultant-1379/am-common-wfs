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
package com.ericsson.amcommonwfs.cluster.config.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.amcommonwfs.TestConstants.INVALID_YAML_EXCEPTION_MESSAGE_PREFIX;
import static com.ericsson.amcommonwfs.util.Constants.CLUSTER_CONFIG_INVALID_FILE_NAME_ERROR_MESSAGE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.util.UnitTestUtils;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class ClusterConfigServiceTest {

    @MockBean
    private KubectlService kubectlService;

    @Autowired
    @InjectMocks
    private ClusterConfigServiceImpl clusterConfigService;

    private InputStream fileContent;

    private String VALID_CONFIG_FILE_NAME = "cluster01.config";
    private String INVALID_CONFIG_FILE_CONTENT = "invalidConfig.config";
    private String INVALID_CONFIG_YAML_FILE_CONTENT = "invalidYaml.config";
    private String INVALID_FILE_NAME = "test+test.config";
    private String INVALID_CONFIG_FILE_WITH_MULTIPLE_ATTRIBUTE = "withMultipleClusterUserContext.config";

    private MultipartFile multipartFile;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(VALID_CONFIG_FILE_NAME);
        multipartFile = new MockMultipartFile(VALID_CONFIG_FILE_NAME, VALID_CONFIG_FILE_NAME, "text/plain",
                                              fileContent);
    }

    @Test
    public void testValidateConfigFile() {
        clusterConfigService.validateConfigFile(multipartFile);
    }

    @Test
    public void testValidateConfigForInvalidFile() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(INVALID_CONFIG_FILE_CONTENT);
        multipartFile = new MockMultipartFile(INVALID_CONFIG_FILE_CONTENT, INVALID_CONFIG_FILE_CONTENT, "text/plain",
                                              fileContent);
        try {
            clusterConfigService.validateConfigFile(multipartFile);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (final IllegalArgumentException iae) {
            assertThat(iae.getMessage()).startsWith(INVALID_YAML_EXCEPTION_MESSAGE_PREFIX);
        }
    }

    @Test
    public void testValidateConfigForInvalidFileContent() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(INVALID_CONFIG_YAML_FILE_CONTENT);
        multipartFile = new MockMultipartFile(INVALID_CONFIG_YAML_FILE_CONTENT, INVALID_CONFIG_YAML_FILE_CONTENT,
                                              "text/plain", fileContent);
        try {
            clusterConfigService.validateConfigFile(multipartFile);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (final IllegalArgumentException iae) {
            assertThat(iae.getMessage()).startsWith(INVALID_YAML_EXCEPTION_MESSAGE_PREFIX);
        }
    }

    @Test
    public void testValidateConfigForInvalidFileName() throws IOException, URISyntaxException {
        fileContent = UnitTestUtils.createInputStream(INVALID_FILE_NAME);
        multipartFile = new MockMultipartFile(INVALID_FILE_NAME, INVALID_FILE_NAME, "text/plain", fileContent);
        try {
            clusterConfigService.validateConfigFile(multipartFile);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (final IllegalArgumentException iae) {
            assertThat(iae.getMessage()).isEqualTo(CLUSTER_CONFIG_INVALID_FILE_NAME_ERROR_MESSAGE);
        }
    }

    @Test()
    public void testValidateConfigForInvalidClusterConfigWithMultipleClusterUserContext() {

        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            fileContent = UnitTestUtils.createInputStream(INVALID_CONFIG_FILE_WITH_MULTIPLE_ATTRIBUTE);
            multipartFile = new MockMultipartFile(INVALID_CONFIG_FILE_WITH_MULTIPLE_ATTRIBUTE,
                                                  INVALID_CONFIG_FILE_WITH_MULTIPLE_ATTRIBUTE, "text/plain", fileContent);

            clusterConfigService.validateConfigFile(multipartFile);
        });
    }

    @Test
    public void testCheckIfConfigFileValidWhenValidConfigFileWithoutNamespace() {

        clusterConfigService.checkIfConfigFileValid(multipartFile);
        verify(kubectlService, times(1)).getClusterServerDetails(any(Path.class));
    }
}
