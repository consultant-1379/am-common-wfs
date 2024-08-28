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
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class PositiveScaleDownBase {

    @MockBean
    KubectlService kubectlService;

    @MockBean
    private IdempotencyServiceImpl idempotencyService;

    @Inject
    private WebApplicationContext context;

    @Mock
    ClusterConfigService clusterConfigService;

    @BeforeEach
    public void setup() {
        when(kubectlService.doesNamespaceExist(any(), any())).thenReturn(true);
        when(idempotencyService.executeTransactionalIdempotentCall(any())).thenCallRealMethod();
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
