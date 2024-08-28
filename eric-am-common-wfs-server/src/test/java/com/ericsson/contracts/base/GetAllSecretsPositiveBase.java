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
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.utility.DataParser;
import com.ericsson.workflow.orchestration.mgmt.model.Secrets;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class GetAllSecretsPositiveBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetAllSecretsPositiveBase.class);

    @MockBean
    KubectlService kubectlService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        given(kubectlService.getAllSecretInTheNamespace(any(), any(), any())).willAnswer(invocationOnMock -> {
            LOGGER.info("in mock");
            String responseTemplate = DataParser.readFile("contracts/api/internal/get/getAllSecrets/positive/validGetAllSecret.json");
            LOGGER.info("JSON IS {}", responseTemplate);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, Secrets.class);
        });
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
