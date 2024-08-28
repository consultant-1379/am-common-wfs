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

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public class MapperUtils {

    public static void setVariables(Map<String, Object> executionVariables, DelegateExecution execution) {
        executionVariables.forEach(execution::setVariableLocal);

        LocalDateTime timeout = LocalDateTime.now().plusSeconds(300);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariableLocal(APP_TIMEOUT, toEpochSecond);
    }
}
