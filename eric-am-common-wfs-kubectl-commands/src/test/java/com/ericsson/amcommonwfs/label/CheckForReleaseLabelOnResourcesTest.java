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
package com.ericsson.amcommonwfs.label;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.amcommonwfs.pod.GetPods;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

@SpringBootTest(classes = { CheckForReleaseLabelOnResources.class, GetPods.class, TemporaryFileServiceImpl.class})
public final class CheckForReleaseLabelOnResourcesTest {

    private ExecutionImpl execution = new ExecutionImpl();

    @Autowired
    private CheckForReleaseLabelOnResources checkForReleaseLabelOnResources;

    @Autowired
    private FileService temporaryFileService;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private CamundaFileRepository camundaFileRepository;
    @MockBean
    private GetPods getPods;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    @BeforeEach
    public void setup() {
        when(clusterFileUtils.createClusterConfigForHelm(any())).thenReturn("clusterConfig");
    }

    @Test
    public void shouldPassThroughOnPresenceOfReleaseLabel() throws Exception {
        when(getPods.getPodsWithNamespaceWithRetry(anyString(), any(), anyString(), anyInt())).thenReturn(getListOfPods());
        execution.setVariable(RELEASE_NAME, "my-release");
        execution.setVariable(NAMESPACE, "default");
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        checkForReleaseLabelOnResources.execute(execution);
    }

    @Test
    public void shouldPassThroughOnPresenceOfReleaseLabelForAllNamespace() throws Exception {
        when(getPods.getPodsInAllNamespacesWithRetry(any(), anyString(), anyInt())).thenReturn(getListOfPods());
        execution.setVariable(RELEASE_NAME, "my-release");
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        checkForReleaseLabelOnResources.execute(execution);
    }

    @Test
    public void shouldThrowExceptionWhenReleaseLabelAbsent()
    throws Exception {
        when(getPods.getPodsWithNamespaceWithRetry(anyString(), eq(null), anyString(), anyInt())).thenReturn(null);
        execution.setVariable(RELEASE_NAME, "my-release");
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        assertThatThrownBy(() -> checkForReleaseLabelOnResources.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessageStartingWith("The label");
    }

    @Test
    public void shouldThrowExceptionWhenReleaseLabelAbsentCheckUsingAllNamespaces()
    throws Exception {
        when(getPods.getPodsInAllNamespacesWithRetry(eq(null), anyString(), anyInt())).thenReturn(null);
        execution.setVariable(RELEASE_NAME, "my-release");
        execution.setVariable(NAMESPACE, "default");
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        assertThatThrownBy(() -> checkForReleaseLabelOnResources.execute(execution)).isInstanceOf(BpmnError.class)
                .hasMessageStartingWith("The label");
    }

    private V1PodList getListOfPods() {
        V1PodList podList = new V1PodList();
        V1Pod pod = new V1Pod();
        podList.addItemsItem(pod);
        return podList;
    }
}
