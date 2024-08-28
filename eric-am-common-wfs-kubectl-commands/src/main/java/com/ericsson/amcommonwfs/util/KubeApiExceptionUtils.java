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

import static com.ericsson.amcommonwfs.utils.constants.Constants.ALREADY_EXISTS_FAILURE_REASON;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import io.kubernetes.client.openapi.ApiException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KubeApiExceptionUtils {

    public static boolean isAlreadyExistError(final ApiException e) {
        return e.getCode() == HttpStatus.CONFLICT.value() && StringUtils.contains(e.getResponseBody(), ALREADY_EXISTS_FAILURE_REASON);
    }
}
