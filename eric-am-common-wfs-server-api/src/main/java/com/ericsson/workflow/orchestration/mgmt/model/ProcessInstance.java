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

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("squid:S1172")
@Getter
@Setter
@NoArgsConstructor
public class ProcessInstance {

    private String instanceId;
    private String definitionKey;
    private String chartName;
    private String chartUrl;
    private String chartVersion;
    private String releaseName;
    private String namespace;
    private String userId;
    private WorkflowState workflowState;
    private String message;
    private Date startTime;
    private Map<String, Object> additionalParams;

    @SuppressWarnings(value = "unchecked")
    public ProcessInstance(Map<String, Object> variablesMap, final String instanceId) {
        setInstanceId(instanceId); // NOSONAR
        setDefinitionKey((String) variablesMap.get("definitionKey")); // NOSONAR
        setChartName((String) variablesMap.get("chartName")); // NOSONAR
        setChartVersion((String) variablesMap.get("chartVersion")); // NOSONAR
        setReleaseName((String) variablesMap.get("releaseName")); // NOSONAR
        setUserId((String) variablesMap.get("userId")); // NOSONAR
        setMessage((String) variablesMap.get("message")); // NOSONAR
        setNamespace((String) variablesMap.get("namespace")); // NOSONAR
        setAdditionalParams((Map<String, Object>) variablesMap.get("additionalParams")); // NOSONAR
        setChartUrl((String) variablesMap.get("chartUrl")); // NOSONAR
    }

    public void setUserId(final String userId) {
        this.userId = "UNKNOWN";
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
