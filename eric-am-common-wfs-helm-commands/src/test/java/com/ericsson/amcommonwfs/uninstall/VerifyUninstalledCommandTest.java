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
package com.ericsson.amcommonwfs.uninstall;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

@SpringBootTest(classes = {
        VerifyUninstalledCommand.class,
        RestTemplate.class,
        TemporaryFileServiceImpl.class})
@MockBean(classes = { CryptoService.class, CamundaFileRepository.class})
@TestPropertySource(properties = { "app.command.execute.defaultTimeOut=300" })
public class VerifyUninstalledCommandTest {
    private final String DUMMY_RELEASE_NAME = "dummy_release_name";
    private final String DUMMY_NAMESPACE = "dummy_name_space";
    private final String DUMMY_CLUSTER_NAME = "test01.config";
    private final ExecutionImpl execution = new ExecutionImpl();

    @Autowired
    private VerifyUninstalledCommand verifyUninstalledCommand;

    @MockBean
    private  ClusterFileUtils clusterFileUtils;

    @BeforeEach
    public void setup() {
        execution.setVariable(APP_TIMEOUT, LocalDateTime.now().plusSeconds(4000).toEpochSecond(ZoneOffset.UTC));
        when(clusterFileUtils.createClusterConfigForHelm(any())).thenReturn("test01.config");
    }

    @Test
    public void testVerifyUninstalledCommandWithClusterName() {
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(NAMESPACE, DUMMY_NAMESPACE);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND);

        verifyUninstalledCommand.createCommand(execution);

        assertThat(execution.getVariable(COMMAND).toString())
                .isEqualTo("helm get dummy_release_name --namespace dummy_name_space --kubeconfig test01.config");
    }

    @Test
    public void testVerifyUninstalledCommandWithConfigFile() {
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(NAMESPACE, DUMMY_NAMESPACE);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND);

        verifyUninstalledCommand.createCommand(execution);

        assertThat(execution.getVariable(COMMAND).toString())
                .isEqualTo("helm get dummy_release_name --namespace dummy_name_space --kubeconfig " + execution.getVariable(CLUSTER_NAME));
    }

    @Test
    public void testVerifyUninstalledCommandWithNotDefaultHelmClientVersion() {
        execution.setVariable(RELEASE_NAME, DUMMY_RELEASE_NAME);
        execution.setVariable(NAMESPACE, DUMMY_NAMESPACE);
        execution.setVariableLocal(HELM_CLIENT_VERSION, HELM_COMMAND_3_10);

        verifyUninstalledCommand.createCommand(execution);

        assertThat(execution.getVariable(COMMAND).toString())
                .isEqualTo("helm-3.10 get dummy_release_name --namespace dummy_name_space --kubeconfig " + execution.getVariable(CLUSTER_NAME));
    }
}
