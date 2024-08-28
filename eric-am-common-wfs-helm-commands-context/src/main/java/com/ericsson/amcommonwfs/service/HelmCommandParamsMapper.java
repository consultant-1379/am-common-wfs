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
package com.ericsson.amcommonwfs.service;

import java.util.Map;
import java.util.function.Function;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.ericsson.amcommonwfs.model.CommandType;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

public interface HelmCommandParamsMapper extends Function<DelegateExecution, Map<String, Object>> {

    CommandType getType();

    ErrorCode getErrorCode();
}
