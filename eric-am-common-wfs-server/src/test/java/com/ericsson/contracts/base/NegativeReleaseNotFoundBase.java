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
import static org.mockito.Mockito.doReturn;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.presentation.services.KubectlAPIService;

import io.kubernetes.client.openapi.models.V1PodList;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(classes = ApplicationServer.class)
@AutoConfigureObservability
public class NegativeReleaseNotFoundBase extends InstantiateNegativeBase {
    @Inject
    private WebApplicationContext context;

    @SpyBean
    private KubectlAPIService kubectlAPIService;

    @BeforeEach
    public void setup() {
        doReturn(new V1PodList()).when(kubectlAPIService).getV1PodList(anyString(), anyString());
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
