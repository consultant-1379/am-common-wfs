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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import io.qameta.allure.Step;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtilities {

    @Step("Load {0} from classpath")
    public static FileSystemResource getFile(final String filename) {
        ClassLoader classLoader = FileUtilities.class.getClassLoader();
        File file = new File(requireNonNull(classLoader.getResource(filename)).getFile());
        if (!file.exists()) {
            LOGGER.info("{} not found on the filesystem, searching the classpath", filename);
            file = requireNonNull(getFileFromClasspath(filename));
        }
        LOGGER.info("{} loaded from {}, filesize: {}", filename, file.getAbsolutePath(), file.length());
        return new FileSystemResource((file));
    }

    private static File getFileFromClasspath(final String searchString) {
        File tempFile = null;
        ClassPathResource classPathResource = new ClassPathResource(searchString);
        try (InputStream inputStream = classPathResource.getInputStream()) {
            tempFile = new File(System.getProperty("java.io.tmpdir"), searchString);
            FileUtils.copyInputStreamToFile(inputStream, tempFile);
        } catch (final IOException e) {
            LOGGER.error("Failed to write file from classpath to disk", e);
        }
        return tempFile;
    }
}
