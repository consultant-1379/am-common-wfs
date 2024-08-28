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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.presentation.controllers.InternalResourceControllerImplTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"cluster.config.directory=/tmp", "spring.flyway.enabled=false"})
@AutoConfigureObservability
public class NegativeValidateClusterConfigBase {

    @Mock
    private ProcessExecutor processExecutor;

    @InjectMocks
    private InternalResourceControllerImplTest internalResourceController;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setup() throws InterruptedException, CommandTimedOutException, IOException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(1);
        given(processExecutor.executeProcess(anyString(), anyInt(), anyBoolean())).willReturn(response);
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
