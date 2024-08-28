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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("squid:S1172")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowProcessInstance extends ProcessInstance {

    private String businessKey;

    @SuppressWarnings(value = "unchecked")
    public WorkflowProcessInstance(Map<String, Object> variablesMap, final String instanceId) {
        super(variablesMap, instanceId);

    }
}
