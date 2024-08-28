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
package com.ericsson.amcommonwfs.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ericsson.amcommonwfs.util.JWTDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MDCLogEnhanceFilter extends OncePerRequestFilter {

    private static final String USERNAME_KEY = "userName";
    private static final String PREFERRED_USERNAME_KEY = "preferred_username";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    @Autowired
    private ObjectMapper objectMapper;

    private String getUsernameFromToken(String token) {
        try {
            return JWTDecoder.decodeToMap(token, objectMapper)
                    .map(item -> item.get(PREFERRED_USERNAME_KEY))
                    .filter(item -> item instanceof String)
                    .map(Object::toString)
                    .orElse(StringUtils.EMPTY);
        } catch (Exception e) {
            LOGGER.warn("Could not get userName from JWT: ", e);
        }
        return StringUtils.EMPTY;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userName = getUsernameFromToken(request.getHeader(AUTHORIZATION_HEADER_NAME));
        try {
            MDC.put(USERNAME_KEY, userName);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(USERNAME_KEY);
        }
    }
}
