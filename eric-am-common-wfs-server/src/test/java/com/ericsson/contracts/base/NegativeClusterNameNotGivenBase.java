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
import static org.mockito.BDDMockito.given;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureObservability
@SpringBootTest(classes = ApplicationServer.class)
public class NegativeClusterNameNotGivenBase extends PodstatusNegativeBase {

    @Inject
    private WebApplicationContext context;

    @MockBean
    KubectlService kubectlService;

    @BeforeEach
    public void setup() {
        given(kubectlService.getPodStatusByReleaseName(anyString(), anyString())).willThrow(new NotFoundException("Required request parameter "
                                                                                                                    + "'clusterName' for method "
                                                                                                                          + "parameter type "
                                                                                                                          + "String is not present"));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
