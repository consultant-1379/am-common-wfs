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
package com.ericsson.amcommonwfs.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.presentation.services.AbstractRequestCommandJobService;
import com.ericsson.amcommonwfs.presentation.services.HelmService;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfo;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfoList;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponseList;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;

@TestPropertySource(properties = { "spring.flyway.enabled=false", "cluster.config.directory=/", "app.command.execute.defaultTimeOut" })
@SpringBootTest(classes = InternalResourceControllerImpl.class)
public class InternalResourceControllerImplTest {

    private static final String RELEASE_NAME_JSON = "[\"spider-app\"]";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    @Autowired
    private InternalResourceControllerImpl internalResourceController;

    @MockBean
    private KubectlService kubectlService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private FileService temporaryFileService;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private AbstractRequestCommandJobService<AsyncDeleteNamespaceRequestDetails> deleteNamespaceCommandJobService;

    @MockBean
    private AbstractRequestCommandJobService<AsyncDeletePvcsRequestDetails> deletePvcsCommandJobService;

    @MockBean
    private IdempotencyServiceImpl idempotencyService;

    @Test
    public void shouldReturnOkWhenGetPodsStatus() {
        String releaseName = "spider-app";
        PodStatusResponse response = new PodStatusResponse();
        response.setClusterName(DEFAULT_CLUSTER_NAME);
        response.setReleaseName(releaseName);

        Mockito.when(kubectlService.getPodStatusByReleaseName(releaseName, DEFAULT_CLUSTER_NAME)).thenReturn(response);

        final ResponseEntity<PodStatusResponse> podStatus = internalResourceController.getPodStatus(releaseName, DEFAULT_CLUSTER_NAME);

        assertThat(podStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(podStatus.getBody().getClusterName()).isEqualTo(DEFAULT_CLUSTER_NAME);
        assertThat(podStatus.getBody().getReleaseName()).isEqualTo(releaseName);
    }

    @Test
    public void shouldReturnOkWhenGetPodsStatusWithReleaseNames() {
        String releaseName = "spider-app";
        PodStatusResponse podStatusResponse = new PodStatusResponse(DEFAULT_CLUSTER_NAME, releaseName, null);

        Mockito.when(clusterConfigService.resolveClusterConfig(DEFAULT_CLUSTER_NAME, null)).thenReturn(DEFAULT_CLUSTER_NAME);
        Mockito.when(kubectlService.getPodStatusByReleaseNames(List.of(releaseName), DEFAULT_CLUSTER_NAME)).thenReturn(List.of(podStatusResponse));

        final ResponseEntity<PodStatusResponseList> podStatus = internalResourceController.getPodStatusWithMultipart(DEFAULT_CLUSTER_NAME,
                                                                                                                     null,
                                                                                                                     RELEASE_NAME_JSON);
        assertThat(podStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldReturnOkWhenGetResourceStatus() {
        String releaseName = "spider-app";
        KubernetesResourceInfo kubernetesResourceInfo = new KubernetesResourceInfo(DEFAULT_CLUSTER_NAME, releaseName, null, null, null);

        Mockito.when(clusterConfigService.resolveClusterConfig(DEFAULT_CLUSTER_NAME, null)).thenReturn(DEFAULT_CLUSTER_NAME);
        Mockito.when(kubectlService.getKubernetesResourceStatusInfoByReleaseNames(List.of(releaseName), DEFAULT_CLUSTER_NAME))
                .thenReturn(List.of(kubernetesResourceInfo));

        final ResponseEntity<KubernetesResourceInfoList> resourceStatus = internalResourceController.getResourceStatus(DEFAULT_CLUSTER_NAME,
                                                                                                                       null,
                                                                                                                       RELEASE_NAME_JSON);

        assertThat(resourceStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldReturnOkWhenClusterConfigFileValidate() {
        MultipartFile clusterConfig = mock(MultipartFile.class);
        ClusterServerDetailsResponse response = new ClusterServerDetailsResponse();

        Mockito.when(clusterConfigService.checkIfConfigFileValid(clusterConfig)).thenReturn(response);

        final ResponseEntity<ClusterServerDetailsResponse> responseEntity =
                internalResourceController.clusterConfigFileValidate(
                clusterConfig);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldReturnAcceptedWhenScaleDown() {
        String nameSpace = "test-ns";
        final InternalScaleInfo internalScaleInfo = new InternalScaleInfo();
        internalScaleInfo.setClusterName(DEFAULT_CLUSTER_NAME);
        internalScaleInfo.setNamespace(nameSpace);

        Mockito.when(kubectlService.doesNamespaceExist(nameSpace, DEFAULT_CLUSTER_NAME)).thenReturn(true);
        Mockito.doNothing().when(kubectlService).scaleDownResources(DEFAULT_CLUSTER_NAME, internalScaleInfo, "dummyKey");
        Mockito.when(idempotencyService.executeTransactionalIdempotentCall(any())).thenCallRealMethod();

        final ResponseEntity<Void> responseEntity = internalResourceController.scaleDown("dummyKey", internalScaleInfo);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

}
