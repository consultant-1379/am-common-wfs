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
package com.ericsson.amcommonwfs.services.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonServicesUtils {

    public static <T> T parseJsonToGenericType(final String jsonString,
                                               final TypeReference<T> typeReference) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to parse json: [%s], because of %s", jsonString, e.getMessage()), e);
        }
    }

    public static String convertObjToJsonString(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Unable to convert object to json: [%s], because of %s", obj, e.getMessage()), e);
        }
    }

    public static boolean isRetryableKubectlException(Throwable e) {
        return e instanceof ApiException;
    }
}
