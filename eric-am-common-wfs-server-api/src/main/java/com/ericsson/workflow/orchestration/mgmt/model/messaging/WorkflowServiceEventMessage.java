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
package com.ericsson.workflow.orchestration.mgmt.model.messaging;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class WorkflowServiceEventMessage implements Serializable {
    private static final long serialVersionUID = 1947987758535505652L;

    private String lifecycleOperationId;
    private String releaseName;
    private String message;
    private WorkflowServiceEventType type;
    private WorkflowServiceEventStatus status;
    private Map<String, String> additionalParams = new HashMap<>();

    public WorkflowServiceEventMessage(final String lifecycleOperationId, final WorkflowServiceEventType type,
            final WorkflowServiceEventStatus status, final String message, final String releaseName) {
        this.lifecycleOperationId = lifecycleOperationId;
        this.type = type;
        this.status = status;
        this.message = message;
        this.releaseName = releaseName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
