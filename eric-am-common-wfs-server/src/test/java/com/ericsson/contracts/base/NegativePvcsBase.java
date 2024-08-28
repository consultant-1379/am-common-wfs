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
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doThrow;

import jakarta.inject.Inject;

import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.presentation.services.AbstractRequestCommandJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class NegativePvcsBase {

    @MockBean
    KubectlService kubectlService;

    @Inject
    private WebApplicationContext context;

    @MockBean
    private AbstractRequestCommandJobService<AsyncDeletePvcsRequestDetails> deletePvcsCommandJobService;

    @MockBean
    private IdempotencyService idempotencyService;

    @BeforeEach
    public void setup() {
        doThrow(new NotFoundException("Cluster not found"))
                .when(kubectlService).scaleDownResources(matches("cluster-not-found"), any(), any());
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
