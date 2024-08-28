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

import static com.ericsson.amcommonwfs.util.Constants.CLUSTER_CONFIG_INVALID_FILE_NAME_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.util.Constants.FILE_NULL_EMPTY_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REGEX_FOR_CLUSTER_CONFIG_FILE_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.ericsson.amcommonwfs.model.ClusterConfigFileContext;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.cluster.config.service.models.KubeConfig;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.util.RestPayloadValidationUtils;
import com.ericsson.amcommonwfs.util.Utility;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClusterConfigServiceImpl implements ClusterConfigService {

    private static final Pattern PATTERN_FOR_CLUSTER_CONFIG_NAME = Pattern
            .compile(REGEX_FOR_CLUSTER_CONFIG_FILE_NAME);

    @Autowired
    private KubectlService kubectlService;

    @Autowired
    private FileService fileService;

    @Override
    public void validateConfigFile(final MultipartFile clusterConfig) {
        final String originalFilename = clusterConfig.getOriginalFilename();
        LOGGER.info("Validating {} kube config file", originalFilename);
        RestPayloadValidationUtils.validateFileName(originalFilename, PATTERN_FOR_CLUSTER_CONFIG_NAME,
                String.format(FILE_NULL_EMPTY_ERROR_MESSAGE, "ClusterConfig"),
                CLUSTER_CONFIG_INVALID_FILE_NAME_ERROR_MESSAGE);
        try (InputStream instr1 = RestPayloadValidationUtils.getInputStream(clusterConfig);
             InputStream instr2 = RestPayloadValidationUtils.getInputStream(clusterConfig)) {
            RestPayloadValidationUtils.validateFileTypeAsPlainText(instr1);
            validateClusterConfigFileContent(instr2);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ClusterServerDetailsResponse checkIfConfigFileValid(final MultipartFile configFile) {
        validateConfigFile(configFile);
        Path clusterConfigPath = null;
        try {
            clusterConfigPath = fileService.saveFile(configFile);
            return kubectlService.getClusterServerDetails(clusterConfigPath);
        } finally {
            if (clusterConfigPath != null) {
                Utility.deleteClusterConfigFile(clusterConfigPath);
            }
        }
    }

    @Override
    public String resolveClusterConfig(String clusterName, MultipartFile clusterConfig) {
        if (Objects.nonNull(clusterConfig)) {
            return fileService.saveFile(clusterConfig)
                    .toAbsolutePath().toString();
        }
        return clusterName;
    }

    @Override
    public ClusterConfigFileContext resolveClusterConfigContext(String clusterName, MultipartFile clusterConfig) {
        if (Objects.nonNull(clusterConfig)) {
            String fileContent = fileService.readMultipartFileContent(clusterConfig);
            return new ClusterConfigFileContext(clusterConfig.getOriginalFilename(), fileContent);
        }
        return new ClusterConfigFileContext(clusterName, null);
    }

    @Override
    public String saveClusterConfig(String fileName, String fileContent) {
        if (Objects.nonNull(fileContent)) {
            return fileService.saveFileIfNotExists(fileContent, UUID.randomUUID() + "_" + fileName);
        }
        return fileName;
    }

    private static void validateClusterConfigFileContent(final InputStream kubeConfigInputStream) {
        final KubeConfig kubeConfig = getJsonContentOfClusterConfigFile(kubeConfigInputStream);
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final Validator validator = factory.getValidator();
        final Set<ConstraintViolation<KubeConfig>> violations = validator.validate(kubeConfig);
        if (violations != null && !violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static KubeConfig getJsonContentOfClusterConfigFile(final InputStream clusterConfigFile) {
        try {
            final JSONObject jsonObject = RestPayloadValidationUtils.validateYamlCanBeParsed(clusterConfigFile);
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonObject.toString(), KubeConfig.class);
        } catch (final IOException ioe) {
            LOGGER.error(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, ioe);
            throw new IllegalArgumentException(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE);
        }
    }
}
