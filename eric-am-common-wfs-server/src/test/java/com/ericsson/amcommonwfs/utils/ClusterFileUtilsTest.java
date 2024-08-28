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
package com.ericsson.amcommonwfs.utils;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

@ExtendWith(SpringExtension.class)
@MockBean(classes = { CryptoService.class, CamundaFileRepository.class })
@ContextConfiguration(classes = { ClusterFileUtils.class, TemporaryFileServiceImpl.class })
public class ClusterFileUtilsTest {

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Mock
    private DelegateExecution execution;

    @MockBean
    private CamundaFileRepository camundaFileRepository;

    @Test
    public void returnNullifConfigIsNullTest() {
        when(execution.getVariable(eq(Constants.CLUSTER_NAME))).thenReturn(null);
        when(execution.getVariable(eq(Constants.APP_TIMEOUT))).thenReturn(LocalDateTime.now().plusSeconds(4000).toEpochSecond(ZoneOffset.UTC));
        String configPath = clusterFileUtils.createClusterConfigForHelm(execution);
        assertNull(configPath);
    }

    @Test
    public void returnCreatedClusterConfigTest() {
        String configFile = "cluster01.config";
        when(execution.getVariable(eq(Constants.ORIGINAL_CLUSTER_NAME))).thenReturn(configFile);
        when(execution.getVariable(eq(Constants.APP_TIMEOUT))).thenReturn(LocalDateTime.now().plusSeconds(4000).toEpochSecond(ZoneOffset.UTC));
        when(camundaFileRepository.get(any())).thenReturn(new byte[]{});

        String returnedConfig = clusterFileUtils.createClusterConfigForHelm(execution);
        Pattern pattern = Pattern.compile("^.*cluster01\\d+\\.config");
        assertTrue(pattern.matcher(returnedConfig).matches());
    }
}
