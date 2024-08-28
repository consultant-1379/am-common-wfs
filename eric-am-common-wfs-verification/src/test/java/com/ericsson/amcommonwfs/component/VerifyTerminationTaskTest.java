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

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import io.kubernetes.client.openapi.ApiException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VerifyTerminationTask.class, TemporaryFileServiceImpl.class, VerificationHelper.class,
        ClusterFileUtils.class })
public class VerifyTerminationTaskTest {

    public static final String TEMP_PATH_TO_FILE = "TEMP_PATH_TO_FILE";
    public static final String TEST_RELEASE = "test-release";
    public static final String TEST_NAMESPACE = "test-namespace";
    public static final String TEST_CLUSTER = "test-cluster";
    public static final String TEST_CLUSTER_CONFIG_FILE = "test-cluster-config-file";
    private static final String ORIGINAL_CONFIG_NAME = "cluster.config";

    @MockBean
    private VerificationHelper verificationHelper;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private FileService temporaryFileService;

    @Autowired
    private VerifyTerminationTask verifyTerminationTask;

    @MockBean
    private CamundaFileRepository camundaFileRepository;

    private final ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testTerminationTaskWithException() throws IOException, ApiException {
        String dummyErrorMessage = "dummy_api_exception";
        ApiException apiException = new ApiException(dummyErrorMessage);

        String clusterUUID = CLUSTER_CONFIG_CONTENT_KEY + "-" + UUID.randomUUID();
        execution.setVariable(CLUSTER_CONFIG_CONTENT_KEY, clusterUUID);
        when(camundaFileRepository.get(clusterUUID)).thenReturn(TEST_CLUSTER_CONFIG_FILE.getBytes(StandardCharsets.UTF_8));
        when(cryptoService.decryptString(TEST_CLUSTER_CONFIG_FILE)).thenReturn(TEST_CLUSTER_CONFIG_FILE);

        when(verificationHelper.getV1PodList(anyString(), anyString(), anyString(), any(Integer.class))).thenThrow(apiException);
        when(temporaryFileService.saveFileIfNotExists(anyString(), anyString())).thenReturn(TEMP_PATH_TO_FILE);
        when(cryptoService.decryptString(anyString())).thenReturn(TEST_CLUSTER_CONFIG_FILE);
        createExecution();

        assertThatThrownBy(() -> verifyTerminationTask.execute(execution)).isInstanceOf(BpmnError.class);
        assertThat(execution.getVariable(ERROR_MESSAGE)).isEqualTo(new ApiException(dummyErrorMessage).getMessage());
    }

    private void createExecution() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, toEpochSecond);
        execution.setVariableLocal(RELEASE_NAME, TEST_RELEASE);
        execution.setVariableLocal(NAMESPACE, TEST_NAMESPACE);
        execution.setVariableLocal(CLUSTER_NAME, TEST_CLUSTER);
        execution.setVariableLocal(ORIGINAL_CLUSTER_NAME, ORIGINAL_CONFIG_NAME);
        execution.setVariableLocal(SKIP_JOB_VERIFICATION, true);
    }
}
