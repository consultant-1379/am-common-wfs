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
package com.ericsson.amcommonwfs;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TestUtils {

    private static final ObjectMapper mapper = createObjectMapper();

    public static <T> T readTypedDataFromFile(Class<?> testClass, String fileName, Class<T> valueType) {
        try {
            return mapper.readValue(readDataFromFile(testClass, fileName), valueType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(format("Could not process file %s", fileName), e);
        }
    }

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

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
