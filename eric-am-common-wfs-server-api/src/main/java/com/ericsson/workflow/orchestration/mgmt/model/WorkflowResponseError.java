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

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;


public class WorkflowResponseError extends WorkflowResponse {
    @Getter
    @Setter
    private String errorMessage;
    private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
