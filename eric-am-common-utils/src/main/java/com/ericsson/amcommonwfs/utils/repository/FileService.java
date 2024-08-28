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
package com.ericsson.amcommonwfs.utils.repository;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

/**
 * API for storing files of storage.
 */
public interface FileService {

    /**
     * Saves file to storage. Method is not required to support a null parameter.
     *
     * @param file - object representation of file content and any details connected with it.
     * @return - file identifier.
     */

    Path saveFile(MultipartFile file);

    /**
     * Saves file to storage if one does not exist
     *
     * @param fileContent File content as String
     * @param fileName    Absolute path to file as String
     * @return new absolute path to file
     */

    String saveFileIfNotExists(String fileContent, String fileName);

    /**
     * Removes file from storage by identifier.
     *
     * @param fileId - identifier of the file to be deleted.
     */
    void removeFile(Path fileId);

    /**
     * Reads file content from storage to String
     *
     * @param tempClusterConfig absolute path to file
     * @return file content as String
     */

    String readFileContentToString(String tempClusterConfig);

    String readMultipartFileContent(MultipartFile file);
}
