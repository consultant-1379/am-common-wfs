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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JWTDecoder {
    private static final String HEADER_SPLIT_PATTERN = ".";

    private static Optional<String> decodeToString(String authToken) {
        if (authToken == null) {
            return Optional.empty();
        }
        // JWT TOKEN = EncodedHeader.EncodedBody.EncodedSignature
        String encodedTokenBody = StringUtils.substringBetween(authToken, HEADER_SPLIT_PATTERN, HEADER_SPLIT_PATTERN);
        if (encodedTokenBody == null) {
            LOGGER.warn("Token is invalid {}", authToken);
            return Optional.empty();
        }
        byte[] decode = Base64.getDecoder().decode(encodedTokenBody);
        if (decode == null) {
            LOGGER.warn("Unable to decode token {}", encodedTokenBody);
            return Optional.empty();
        }
        return Optional.of(new String(decode, StandardCharsets.UTF_8));
    }

    public static Optional<Map<String, Object>> decodeToMap(String token, ObjectMapper objectMapper) {
        return decodeToString(token)
                .map(decodedToken -> convertStringToMap(decodedToken, objectMapper));
    }

    private static Map<String, Object> convertStringToMap(String decodedToken, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(decodedToken, new TokenMapReference());
        } catch (IOException e) {
            LOGGER.warn("Can not convert to map ", e);
            return Collections.emptyMap();
        }
    }

    private static class TokenMapReference extends TypeReference<Map<String, Object>> {
    }
}
