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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.presentation.controllers.InternalResourceControllerImpl;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class PositivePatchSecretBase {

    @InjectMocks
    private InternalResourceControllerImpl resourceController;

    @Mock
    KubectlService kubectlService;

    @Mock
    TemporaryFileServiceImpl temporaryFileService;

    @Mock
    private ClusterConfigService clusterConfigService;

    @BeforeEach
    public void setup() {
        doReturn(Paths.get("tmp/testClusterConfig.config")).when(temporaryFileService).saveFile(any());
        doNothing().when(kubectlService).patchSecretInNamespace(any(), any());
        RestAssuredMockMvc.standaloneSetup(resourceController);
    }
}
