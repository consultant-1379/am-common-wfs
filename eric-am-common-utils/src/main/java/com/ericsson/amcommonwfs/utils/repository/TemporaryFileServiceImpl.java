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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TemporaryFileServiceImpl implements FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryFileServiceImpl.class);

    @Override
    public Path saveFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        Path tempFile = null;
        try {
            tempFile = createTempFile(originalFileName);
            file.transferTo(tempFile.toFile());
            Files.setPosixFilePermissions(tempFile,
                                          Set.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ));

            LOGGER.info("File with original filename {} was saved as {}", originalFileName, tempFile.getFileName());
            return tempFile;
        } catch (IOException ex) { // NOSONAR
            removeFile(tempFile);
            LOGGER.error("Error during saving temp file with name {}", originalFileName);
            throw new FileStorageException("Failed to store temp file due to " + ex.getMessage());
        }
    }

    @Override
    public String saveFileIfNotExists(String fileContent, String fileName) {
        if (new File(fileName).exists()) {
            return fileName;
        }
        return String.valueOf(saveFile(fileContent, fileName).toAbsolutePath());
    }

    @Override
    public String readFileContentToString(final String path) {
        if (path != null) {
            try {
                return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
            } catch (IOException e) {
                LOGGER.error("Error during reading temp file to execution variable {}", path, e);
            }
        }
        return null;
    }

    @Override
    public void removeFile(Path filePath) {
        if (filePath != null) {
            try {
                FileUtils.forceDelete(filePath.toFile());
            } catch (IOException ex) {
                LOGGER.error("Failed to remove temp file with name {}", filePath, ex);
            }
        }
    }

    private static Path createTempFile(String originalFileName) throws IOException {
        String fileName = FilenameUtils.getBaseName(originalFileName);
        String fileExtension = FilenameUtils.getExtension(originalFileName);
        return Files.createTempFile(fileName, "." + fileExtension); // NOSONAR
    }

    private Path saveFile(String fileContent, String fileName) {
        Path tempFile = null;
        try {
            tempFile = createTempFile(fileName);
            FileUtils.writeStringToFile(tempFile.toFile(), fileContent, StandardCharsets.UTF_8.name());
            Files.setPosixFilePermissions(tempFile,
                                          Set.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ));
            LOGGER.info("File with original filename {} was saved as {}", fileName, tempFile.getFileName());
            return tempFile;
        } catch (IOException ex) { // NOSONAR
            removeFile(tempFile);
            LOGGER.error("Error during saving temp file with name {}", fileName, ex);
            throw new FileStorageException("Failed to store temp file due to " + ex.getMessage());
        }
    }

    public String readMultipartFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("Error during reading file with name {}", file.getOriginalFilename(), ex);
            throw new FileStorageException("Failed to read temp file due to " + ex.getMessage());
        }
    }
}
