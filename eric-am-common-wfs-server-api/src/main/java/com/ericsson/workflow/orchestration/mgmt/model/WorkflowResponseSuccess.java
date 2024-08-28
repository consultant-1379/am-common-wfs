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
package com.ericsson.workflow.orchestration.mgmt.model;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowResponseSuccess extends WorkflowResponse {
    private String businessKey;
    private String definitionId;
    private Map<String, String> links;

    @Setter(value = AccessLevel.NONE)
    private HttpStatus httpStatus = HttpStatus.ACCEPTED;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

