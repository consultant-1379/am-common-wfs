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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.camunda.service.WorkflowInstanceServiceCamunda;
import com.ericsson.amcommonwfs.presentation.controllers.v3.ResourceApiControllerImpl;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class NegativeApplicationTimedOutBase {

    @Mock
    WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;
    @InjectMocks
    ResourceApiControllerImpl resourceController;

    @BeforeEach
    public void setup() {
        given(workflowInstanceServiceCamunda.getWorkflowHistoryByReleaseName(anyString(), any()))
                .willReturn(getWorkflowQueryResponseError());

        RestAssuredMockMvc.standaloneSetup(resourceController);
    }

    public WorkflowQueryResponse getWorkflowQueryResponseError() {
        List<ProcessInstance> workflowQueries = new ArrayList<>();

        ResourceProcessInstance resourceProcessInstance = new ResourceProcessInstance();
        resourceProcessInstance.setInstanceId("a4233dd0-cd63-11e9-9fd9-4615cf3f00bd");
        resourceProcessInstance.setDefinitionKey("InstantiateApplication__top");
        resourceProcessInstance.setChartName("stable/mysql");
        resourceProcessInstance.setChartUrl(null);
        resourceProcessInstance.setChartVersion("0.13.0");
        resourceProcessInstance.setReleaseName("release-apptimeout");
        resourceProcessInstance.setNamespace("default");
        resourceProcessInstance.setUserId("UNKNOWN");
        resourceProcessInstance.setWorkflowState(WorkflowState.FAILED);
        resourceProcessInstance.setMessage(
                "{\"detail\":\"Verification of the lifecycle operation failed. Please try increasing the "
                        + "applicationTimeOut.\",\"status\":\"422\"}");
        resourceProcessInstance.setStartTime(new Date(1551890124328L));
        resourceProcessInstance.setAdditionalParams(null);
        resourceProcessInstance.setRevision(null);
        resourceProcessInstance.setRevisionDescription(null);

        workflowQueries.add(resourceProcessInstance);

        WorkFlowQueryMetaData workFlowQueryMetaData = new WorkFlowQueryMetaData();
        workFlowQueryMetaData.setCount(1);

        WorkflowQueryResponse workflowQueryResponse = new WorkflowQueryResponse();
        workflowQueryResponse.setWorkflowQueries(workflowQueries);
        workflowQueryResponse.setMetadata(workFlowQueryMetaData);

        return workflowQueryResponse;
    }
}
