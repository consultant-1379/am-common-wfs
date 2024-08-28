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
package com.ericsson.amcommonwfs.utils;

import static com.ericsson.amcommonwfs.utils.CommonUtils.convertToJSONString;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;

import java.nio.file.Path;

import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.repository.FileService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ClusterFileUtils {

    public static final String CMD_TIMED_OUT_ERR_MSG = "Unable to get the result in the time specified";

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private FileService temporaryFileService;

    @Autowired
    private CamundaFileRepository camundaFileRepository;

    public String createClusterConfigForHelm(DelegateExecution execution) {
        String fileContentKey = (String) execution.getVariable(CLUSTER_CONFIG_CONTENT_KEY);

        byte[] clusterConfigContent = camundaFileRepository.get(fileContentKey);
        return createClusterFile(clusterConfigContent, execution);
    }

    private String createClusterFile(final byte[] clusterConfigFileAsBytes,
                                    final DelegateExecution execution) {
        if (clusterConfigFileAsBytes != null) {

            String clusterConfigFile = new String(clusterConfigFileAsBytes, UTF_8);
            String decryptedFileContents = null;
            try {
                decryptedFileContents = cryptoService.decryptString(clusterConfigFile);
            } catch (CryptoException ex) { // NOSONAR
                BusinessProcessExceptionUtils.throwBusinessProcessException(ErrorCode.BPMN_DEPENDENCY_SERVICE_UNAVAILABLE, ex.getMessage());
            }

            String originalClusterName = (String) execution.getVariable(ORIGINAL_CLUSTER_NAME);
            return temporaryFileService.saveFileIfNotExists(decryptedFileContents, originalClusterName);

        } else {
            int applicationTimeOut = Integer.parseInt(resolveTimeOut(execution));

            if (applicationTimeOut == 0) {
                LOGGER.error("Command timed out when creating config file");
                BusinessProcessExceptionUtils.handleException(ErrorCode.BPMN_COMMAND_TIMEOUT_EXCEPTION,
                                                              convertToJSONString(CMD_TIMED_OUT_ERR_MSG,
                                                                                  HttpStatus.UNPROCESSABLE_ENTITY.toString()),
                                                              execution);
            }
            return null;
        }
    }

    public void removeClusterConfig(String clusterConfig) {
        LOGGER.info("Removing cluster config {}", clusterConfig);
        temporaryFileService.removeFile(Path.of(clusterConfig));
    }
}
