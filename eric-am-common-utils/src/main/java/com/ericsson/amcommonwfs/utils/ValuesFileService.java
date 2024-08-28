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

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;
import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.ericsson.amcommonwfs.utils.repository.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class ValuesFileService {

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private FileService fileService;

    public String createValuesFile(String valuesFilePath, final byte[] valuesFileContentAsBytes) {
        if (valuesFilePath != null && valuesFileContentAsBytes != null) {
            if (new File(valuesFilePath).exists()) {
                return valuesFilePath;
            }
            String valuesFileContent = new String(valuesFileContentAsBytes, UTF_8);
            try {
                String decryptedFileContent = cryptoService.decryptString(valuesFileContent);
                return fileService.saveFileIfNotExists(decryptedFileContent, valuesFilePath);
            } catch (CryptoException ex) { // NOSONAR
                BusinessProcessExceptionUtils.throwBusinessProcessException(ErrorCode.BPMN_DEPENDENCY_SERVICE_UNAVAILABLE, ex.getMessage());
            }
        }
        return valuesFilePath;
    }
}
