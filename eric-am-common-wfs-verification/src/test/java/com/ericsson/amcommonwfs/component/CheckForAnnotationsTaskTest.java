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
package com.ericsson.amcommonwfs.component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.IS_ANNOTATED;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFICATION_ANNOTATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { CheckForAnnotationsTask.class, TemporaryFileServiceImpl.class })
public class CheckForAnnotationsTaskTest {

    @InjectMocks
    @Autowired
    CheckForAnnotationsTask checkForAnnotationsTask;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    VerificationHelper verificationHelper;

    @MockBean
    private CamundaFileRepository camundaFileRepository;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    private ExecutionImpl execution = new ExecutionImpl();

    @BeforeEach
    public void init() {
        execution.setVariableLocal(NAMESPACE, "default");
        execution.setVariableLocal(RELEASE_NAME, "release");
        execution.setVariableLocal(ORIGINAL_CLUSTER_NAME, "cluster.config");
        when(clusterFileUtils.createClusterConfigForHelm(any())).thenReturn("cluster.config");
    }

    @Test
    public void testV1PodFoundWithAnnotationInList() throws Exception {
        V1Pod annotatedPod = getAnnotatedPod();
        V1Pod unannotatedPod1 = getNonAnnotatedPod();
        V1Pod unannotatedPod2 = getNonAnnotatedPod();
        V1PodList pods = new V1PodList();
        pods.addItemsItem(annotatedPod);
        pods.addItemsItem(unannotatedPod1);
        pods.addItemsItem(unannotatedPod2);
        when(verificationHelper.getV1PodList(anyString(), anyString(), anyString(), anyInt())).thenReturn(pods);
        checkForAnnotationsTask.execute(execution);
        boolean isAnnotated = (boolean) execution.getVariable(IS_ANNOTATED);
        assertThat(isAnnotated).isTrue();
    }

    @Test
    public void testV1PodFoundWithAnnotation() throws Exception {
        V1Pod annotatedPod = getAnnotatedPod();
        V1PodList pods = new V1PodList();
        pods.addItemsItem(annotatedPod);
        when(verificationHelper.getV1PodList(anyString(), anyString(), anyString(), anyInt())).thenReturn(pods);
        checkForAnnotationsTask.execute(execution);
        boolean isAnnotated = (boolean) execution.getVariable(IS_ANNOTATED);
        assertThat(isAnnotated).isTrue();
    }

    @Test
    public void testV1PodNotFoundWithoutAnnotation() throws Exception {
        V1Pod unannotatedPod = getNonAnnotatedPod();
        V1PodList pods = new V1PodList();
        pods.addItemsItem(unannotatedPod);
        when(verificationHelper.getV1PodList(anyString(), anyString(), anyString(), anyInt())).thenReturn(pods);
        checkForAnnotationsTask.execute(execution);
        boolean isAnnotated = (boolean) execution.getVariable(IS_ANNOTATED);
        assertThat(isAnnotated).isFalse();
    }

    @Test
    public void testV1PodFoundWithApiException() throws Exception {
        ApiException apiException = new ApiException("dummy_api_exception");
        when(verificationHelper.getV1PodList(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(apiException);
        assertThatThrownBy(() -> checkForAnnotationsTask.execute(execution)).isInstanceOf(BpmnError.class);
    }

    private V1Pod getNonAnnotatedPod() {
        V1Pod unannotatedPod = new V1Pod();
        V1ObjectMeta metadata = new V1ObjectMeta();
        unannotatedPod.setMetadata(metadata);
        return unannotatedPod;
    }

    private V1Pod getAnnotatedPod() {
        V1Pod annotatedPod = getNonAnnotatedPod();
        V1ObjectMeta metadata = new V1ObjectMeta();
        Map<String, String> verificationAnnotations = new HashMap<>();
        String value = "[{'containerName':'container-1','state':'Terminated','ready':'True'}";
        verificationAnnotations.put(VERIFICATION_ANNOTATION, value);
        metadata.setAnnotations(verificationAnnotations);
        annotatedPod.setMetadata(metadata);
        return annotatedPod;
    }
}
