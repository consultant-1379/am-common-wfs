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

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import jakarta.inject.Inject;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class ValuesNegativeBase {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private ProcessExecutor processExecutor;

    @BeforeEach
    public void setup() throws CommandTimedOutException, IOException, InterruptedException {
        RestAssuredMockMvc.webAppContextSetup(context);
        given(processExecutor.executeProcess(anyString(), anyInt(), anyBoolean()))
                .willThrow(new CommandTimedOutException("Unable to get the result in the time specified"));
    }
}
