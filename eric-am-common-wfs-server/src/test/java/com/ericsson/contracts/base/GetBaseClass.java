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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.workflow.orchestration.mgmt.model.ProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.ResourceProcessInstance;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowState;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public abstract class GetBaseClass {

    protected WorkFlowQueryMetaData getWorkFlowQueryMetaData(long count) {
        WorkFlowQueryMetaData workFlowQueryMetaData = new WorkFlowQueryMetaData();
        workFlowQueryMetaData.setCount(count);
        return workFlowQueryMetaData;
    }

    protected List<ProcessInstance> getProcessInstance() {
        List<ProcessInstance> workflowQueries = new ArrayList<>();

        ResourceProcessInstance resourceProcessInstance = new ResourceProcessInstance();
        resourceProcessInstance.setInstanceId("04a3761b-36a5-11e9-a5ac-96b34b4a5326-dummyId");
        resourceProcessInstance.setDefinitionKey("InstantiateApplication__top");
        resourceProcessInstance.setChartName("adp-am/my-release");
        resourceProcessInstance.setChartUrl(null);
        resourceProcessInstance.setChartVersion("my-release-0.0.1-223");
        resourceProcessInstance.setReleaseName("my-release");
        resourceProcessInstance.setNamespace("default");
        resourceProcessInstance.setUserId("UNKNOWN");
        resourceProcessInstance.setWorkflowState(WorkflowState.COMPLETED);
        resourceProcessInstance.setMessage("Application deployed with name my-release");
        resourceProcessInstance.setStartTime(new Date(1551880124728L));
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("es.timeout", "66s");
        resourceProcessInstance.setAdditionalParams(additionalParams);
        resourceProcessInstance.setRevision("1");
        resourceProcessInstance.setRevisionDescription("Install complete");

        workflowQueries.add(resourceProcessInstance);
        return workflowQueries;
    }
}
