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
package com.ericsson.amcommonwfs.camunda.service;

import java.util.Map;

import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;

public interface WorkflowInstanceServiceCamunda {

    ResourceResponseSuccess startWorkflowInstanceByDefinitionKeyAndVariables(String definitionKey,
            Map<String, Object> processVariable);

    ResourceResponseSuccess startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(String definitionKey,
                                                                             String businessKey,
                                                                             Map<String, Object> processVariable);

    WorkflowQueryResponse getWorkflowHistoryByReleaseName(String releaseName, String instanceId);
}
