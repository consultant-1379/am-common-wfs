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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
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
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class PositiveValidateClusterConfigBase {

    @Mock
    private ClusterConfigService clusterConfigService;

    @InjectMocks
    private InternalResourceControllerImpl internalResourceController;

    @BeforeEach
    public void setup() {
        ClusterServerDetailsResponse responseSupportedCluster = new ClusterServerDetailsResponse();
        responseSupportedCluster.setHostUrl("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf");
        responseSupportedCluster.setNamespaces(List.of(new Namespace().name("kube-system").uid("deadbeef-6d62-497d-8de1-ffa0aea9696f"),
                new Namespace().name("cvnfm").uid("4d7be745-4b5e-4361-989b-d8419472edf5")));
        responseSupportedCluster.version("1.26");

        ClusterServerDetailsResponse responseUnsupportedCluster = new ClusterServerDetailsResponse();
        responseUnsupportedCluster.setHostUrl("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf");
        responseUnsupportedCluster.setNamespaces(List.of(new Namespace().name("kube-system").uid("deadbeef-6d62-497d-8de1-ffa0aea9696f"),
                                       new Namespace().name("cvnfm").uid("4d7be745-4b5e-4361-989b-d8419472edf5")));
        responseUnsupportedCluster.version("1.23");
        given(clusterConfigService.checkIfConfigFileValid(any())).willReturn(responseSupportedCluster);
        given(clusterConfigService.checkIfConfigFileValid(ArgumentMatchers.argThat(multipartFile -> {
            String fileName = multipartFile.getOriginalFilename();
            return fileName != null && fileName.equals("cluster01unsupported.config");
        }))).willReturn(responseUnsupportedCluster);
        RestAssuredMockMvc.standaloneSetup(internalResourceController);
    }
}
