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
package com.ericsson.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.camunda.service.WorkflowInstanceServiceCamunda;
import com.ericsson.amcommonwfs.presentation.controllers.v3.ResourceApiControllerImpl;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureObservability
public class ReleasehistoryPositiveinstanceidBase extends GetBaseClass {

    @Mock
    WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;
    @InjectMocks
    ResourceApiControllerImpl resourceController;

    @BeforeEach
    public void setup() {
        given(workflowInstanceServiceCamunda.getWorkflowHistoryByReleaseName(anyString(), any()))
                .willReturn(getWorkflowQueryByInstanceIdResponseSuccessInstance());

        RestAssuredMockMvc.standaloneSetup(resourceController);
    }

    private WorkflowQueryResponse getWorkflowQueryByInstanceIdResponseSuccessInstance() {
        List<ProcessInstance> workflowQueries = getProcessInstance();
        WorkFlowQueryMetaData workFlowQueryMetaDataByInstanceId = getWorkFlowQueryMetaData(1);

        WorkflowQueryResponse workflowQueryResponseByInstanceId = new WorkflowQueryResponse();
        workflowQueryResponseByInstanceId.setWorkflowQueries(workflowQueries);
        workflowQueryResponseByInstanceId.setMetadata(workFlowQueryMetaDataByInstanceId);

        return workflowQueryResponseByInstanceId;
    }
}
