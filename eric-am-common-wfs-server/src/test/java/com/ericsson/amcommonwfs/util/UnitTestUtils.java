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
package com.ericsson.amcommonwfs.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import com.google.common.io.Resources;

import static java.lang.String.format;

public class UnitTestUtils {

    public static String readDataFromFile(final Class<?> testClass, String fileName) {
        try {
            return Files.lines(getResource(testClass, fileName))
                    .collect(Collectors.joining("\n"));
        } catch (final IOException e) {
            throw new IllegalArgumentException(format("Could not read file %s", fileName), e);
        }
    }

    public static Path getResource(final Class<?> testClass, final String fileName) {
        final var resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException(format("Could not find file %s", fileName));
        }
        return Paths.get(resource.getPath());
    }

    public static InputStream createInputStream(String fileName) throws URISyntaxException, IOException {
        return Files.newInputStream(getResource(fileName));
    }

    public static Path getResource(String resourceName) throws URISyntaxException {
        return Paths.get(Resources.getResource(resourceName).toURI());
    }
}
