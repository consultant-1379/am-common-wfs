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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.camunda.service.WorkflowInstanceServiceCamunda;
import com.ericsson.amcommonwfs.presentation.controllers.v3.ResourceApiControllerImpl;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class TerminatePositivev3Base {

    @MockBean
    WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;

    @Mock
    HttpServletRequest httpServletRequest;

    @Autowired
    ResourceApiControllerImpl resourceController;

    @MockBean
    CamundaFileRepository camundaFileRepository;

    @MockBean
    IdempotencyServiceImpl idempotencyService;

    @BeforeEach
    public void setup() {
        given(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(anyString(), anyString(), anyMap()))
                .willReturn(getResourceResponseSuccess());
        given(httpServletRequest.getRequestURL()).willReturn(new StringBuffer("http://localhost/api/lcm/v3/resources"));
        when(idempotencyService.executeTransactionalIdempotentCall(any())).thenCallRealMethod();
        ReflectionTestUtils.setField(resourceController, "httpServletRequest", httpServletRequest);
        RestAssuredMockMvc.standaloneSetup(resourceController);
    }

    public ResourceResponseSuccess getResourceResponseSuccess() {
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/lcm/v3/resources/my-release");
        links.put("instance",
                "/api/lcm/v3/resources/my-release?instanceId=4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        ResourceResponseSuccess resourceResponse = new ResourceResponseSuccess();
        resourceResponse.setInstanceId("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        resourceResponse.setLinks(links);
        resourceResponse.setReleaseName("my-release");
        return resourceResponse;
    }
}
