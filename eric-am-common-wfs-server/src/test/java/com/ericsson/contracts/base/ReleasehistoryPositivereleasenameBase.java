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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureObservability
public class ReleasehistoryPositivereleasenameBase extends GetBaseClass {

    @Mock
    WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;
    @InjectMocks
    ResourceApiControllerImpl resourceController;

    @BeforeEach
    public void setup() {
        given(workflowInstanceServiceCamunda.getWorkflowHistoryByReleaseName(anyString(), isNull())).willReturn(getWorkflowQueryResponseSuccess());

        RestAssuredMockMvc.standaloneSetup(resourceController);
    }

    public WorkflowQueryResponse getWorkflowQueryResponseSuccess() {
        List<ProcessInstance> workflowQueries = getProcessInstances();
        WorkFlowQueryMetaData workFlowQueryMetaData = getWorkFlowQueryMetaData(3);

        WorkflowQueryResponse workflowQueryResponse = new WorkflowQueryResponse();
        workflowQueryResponse.setWorkflowQueries(workflowQueries);
        workflowQueryResponse.setMetadata(workFlowQueryMetaData);

        return workflowQueryResponse;
    }

    private List<ProcessInstance> getProcessInstances() {
        List<ProcessInstance> workflowQueries = getProcessInstance();

        ResourceProcessInstance resourceProcessInstanceUpgraded = new ResourceProcessInstance();
        resourceProcessInstanceUpgraded.setInstanceId("32cd2585-44b7-11e9-806b-645d86898946-dummyId");
        resourceProcessInstanceUpgraded.setDefinitionKey("UpgradeApplication__top");
        resourceProcessInstanceUpgraded.setChartName("adp-am/my-release");
        resourceProcessInstanceUpgraded.setChartUrl(null);
        resourceProcessInstanceUpgraded.setChartVersion("my-release-0.0.1-224");
        resourceProcessInstanceUpgraded.setReleaseName("my-release");
        resourceProcessInstanceUpgraded.setNamespace("default");
        resourceProcessInstanceUpgraded.setUserId("UNKNOWN");
        resourceProcessInstanceUpgraded.setWorkflowState(WorkflowState.COMPLETED);
        resourceProcessInstanceUpgraded.setMessage("Application upgraded with name my-release");
        resourceProcessInstanceUpgraded.setStartTime(new Date(1551890124328L));
        Map<String, Object> additionalParamsForUpgrade = new HashMap<>();
        additionalParamsForUpgrade.put("es.timeout", "56s");
        resourceProcessInstanceUpgraded.setAdditionalParams(additionalParamsForUpgrade);
        resourceProcessInstanceUpgraded.setRevision("2");
        resourceProcessInstanceUpgraded.setRevisionDescription("Upgrade complete");

        ResourceProcessInstance resourceProcessInstanceRollback = new ResourceProcessInstance();
        resourceProcessInstanceRollback.setInstanceId("04a3761b-36a5-11e9-a5ac-96b34b4a5326-dummyId");
        resourceProcessInstanceRollback.setDefinitionKey("RollbackApplication__top");
        resourceProcessInstanceRollback.setChartName("adp-am/my-release");
        resourceProcessInstanceRollback.setChartUrl(null);
        resourceProcessInstanceRollback.setChartVersion("my-release-0.0.1-223");
        resourceProcessInstanceRollback.setReleaseName("my-release");
        resourceProcessInstanceRollback.setNamespace("default");
        resourceProcessInstanceRollback.setUserId("UNKNOWN");
        resourceProcessInstanceRollback.setWorkflowState(WorkflowState.COMPLETED);
        resourceProcessInstanceRollback.setMessage("Application rolled back with name my-release");
        resourceProcessInstanceRollback.setStartTime(new Date(1551880124829L));
        Map<String, Object> additionalParamsForRollback = new HashMap<>();
        additionalParamsForRollback.put("es.timeout", "76s");
        resourceProcessInstanceRollback.setAdditionalParams(additionalParamsForRollback);
        resourceProcessInstanceRollback.setRevision("3");
        resourceProcessInstanceRollback.setRevisionDescription("Rollback complete");

        workflowQueries.add(resourceProcessInstanceUpgraded);
        workflowQueries.add(resourceProcessInstanceRollback);
        return workflowQueries;
    }
}
