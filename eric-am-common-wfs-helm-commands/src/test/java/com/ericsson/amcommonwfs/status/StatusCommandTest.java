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
package com.ericsson.amcommonwfs.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.utils.TestConstants.HELM_COMMAND;
import static com.ericsson.amcommonwfs.utils.TestConstants.HELM_COMMAND_3_10;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

@SpringBootTest(classes = { StatusCommand.class, TemporaryFileServiceImpl.class})
@MockBean(classes = { CryptoService.class, CamundaFileRepository.class})
public class StatusCommandTest {

    private final ExecutionImpl execution = new ExecutionImpl();

    @Autowired
    private StatusCommand statusCommand;

    @MockBean
    private  ClusterFileUtils clusterFileUtils;

    @BeforeEach
    public void setup() {
        execution.setVariable(APP_TIMEOUT, LocalDateTime.now().plusSeconds(4000).toEpochSecond(ZoneOffset.UTC));
        when(clusterFileUtils.createClusterConfigForHelm(any())).thenReturn("test01.config");
    }

    @Test
    public void verifyCommandStructureWithClusterConfig() {
        String clusterConfig = "test01.config";
        execution.setVariable(CLUSTER_NAME, clusterConfig);
        execution.setVariable(RELEASE_NAME, "my-release");
        execution.setVariable(NAMESPACE, "my-namespace");
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND);
        statusCommand.createCommand(execution);
        StringBuilder command = (StringBuilder) execution.getVariable(COMMAND);
        assertThat(command.toString()).matches("helm status my-release --namespace my-namespace --kubeconfig " + clusterConfig);
    }

    @Test
    public void verifyCommandStructureWithClusterConfigFile() {
        String clusterConfig = "test01.config";
        execution.setVariable(CLUSTER_NAME, clusterConfig);
        execution.setVariable(RELEASE_NAME, "my-release");
        execution.setVariable(NAMESPACE, "my-namespace");
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND);
        statusCommand.createCommand(execution);
        StringBuilder command = (StringBuilder) execution.getVariable(COMMAND);
        assertEquals(command.toString(), "helm status my-release --namespace my-namespace --kubeconfig " + execution.getVariable(CLUSTER_NAME));
    }

    @Test
    public void verifyCommandStructureWithNotDefaultHelmClientVersion() {
        execution.setVariable(RELEASE_NAME, "my-release");
        execution.setVariable(NAMESPACE, "my-namespace");
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND_3_10);
        statusCommand.createCommand(execution);
        StringBuilder command = (StringBuilder) execution.getVariable(COMMAND);
        assertThat(command.toString()).matches("helm-3.10 status my-release --namespace my-namespace --kubeconfig " + execution.getVariable(CLUSTER_NAME));
    }
}
