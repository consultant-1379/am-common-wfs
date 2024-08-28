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
package com.ericsson.amcommonwfs.utilities;

import java.util.List;

import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceHttpResponse {

    private List<ResourceProcessInstance> workflowQueries;
    private WorkFlowQueryMetaData metadata;
}
