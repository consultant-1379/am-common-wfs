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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowInstanceResource {

    private String instanceId;
    private String businessKey;
    private String definitionId;

    public WorkflowInstanceResource(final String instanceId, final String businessKey, final String definitionId) {
        super();
        this.instanceId = instanceId;
        this.businessKey = businessKey;
        this.definitionId = definitionId;
    }

    @Override
    public String toString() {
        return "[{instanceId : " + instanceId + ",businessKey :" + businessKey + ",definitionId :" + definitionId
                + "}]";
    }

}
