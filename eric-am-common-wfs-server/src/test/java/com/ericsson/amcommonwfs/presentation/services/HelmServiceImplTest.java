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
package com.ericsson.amcommonwfs.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.exception.CommandExecutionException;
import com.ericsson.amcommonwfs.util.MapUtils;
import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;
import com.google.common.io.Resources;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class HelmServiceImplTest {

    @Mock
    private ProcessExecutor processExecutor;

    @InjectMocks
    private HelmServiceImpl helmService;

    @Captor
    private ArgumentCaptor<String> commandCaptor;

    @Test
    public void shouldSuccessfullyReturnValuesWithDefaultCluster() throws CommandTimedOutException, IOException, InterruptedException,
            URISyntaxException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(0);
        String valuesString = FileUtils.readFileToString(Paths.get(Resources.getResource("valueFiles/values.yaml").toURI()).toFile(),
                                                         StandardCharsets.UTF_8);
        response.setCmdResult(valuesString);
        when(processExecutor.executeProcess(commandCaptor.capture(), anyInt(), anyBoolean())).thenReturn(response);
        Map<String, Object> values = helmService.getValues("release-1", "default", "my-namespace", "360");

        assertThat(values).isEqualTo(MapUtils.convertYamlToMap(valuesString));
        assertThat(commandCaptor.getValue()).isEqualTo("helm get values release-1 --namespace my-namespace");
    }

    @Test
    public void shouldFailReturnValues() throws CommandTimedOutException, IOException, InterruptedException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(1);
        String errorString = "Error: release: not found";
        response.setCmdResult(errorString);
        when(processExecutor.executeProcess(commandCaptor.capture(), anyInt(), anyBoolean())).thenReturn(response);
        assertThatThrownBy(() -> helmService.getValues("release-1", "/mnt/cluster_config/hahn165.config", "my-namespace", "360"))
                .isInstanceOf(CommandExecutionException.class).hasMessage(errorString);
    }

    @Test
    public void shouldSuccessfullyReturnValues() throws CommandTimedOutException, IOException, InterruptedException, URISyntaxException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(0);
        String valuesString = FileUtils.readFileToString(Paths.get(Resources.getResource("valueFiles/values.yaml").toURI()).toFile(),
                                                         StandardCharsets.UTF_8);
        response.setCmdResult(valuesString);
        when(processExecutor.executeProcess(commandCaptor.capture(), anyInt(), anyBoolean())).thenReturn(response);
        Map<String, Object> values = helmService.getValues("release-1", "/mnt/cluster_config/hahn165.config", "my-namespace", "360");

        assertThat(values).isEqualTo(MapUtils.convertYamlToMap(valuesString));
        assertThat(commandCaptor.getValue()).isEqualTo(
                "helm get values release-1 --namespace my-namespace --kubeconfig /mnt/cluster_config/hahn165.config");
    }

    @Test
    public void shouldSuccessfullyReturnHelmVersions() throws InterruptedException, CommandTimedOutException, IOException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(0);
        String helmVersions = "helm\nhelm-3.8\nhelm-3.10\nhelm-3.12\nhelm-3.13\nhelm-3.14\nhelm-latest";
        response.setCmdResult(helmVersions);

        when(processExecutor.executeProcess(commandCaptor.capture(), anyInt(), anyBoolean())).thenReturn(response);

        HelmVersionsResponse helmVersionsResponse = helmService.getHelmVersions();

        List<String> expectedVersions = Arrays.asList("3.8", "3.10", "3.12", "3.13", "3.14", "latest");

        assertThat(helmVersionsResponse.getHelmVersions()).isNotEmpty();
        assertThat(helmVersionsResponse.getHelmVersions().size()).isEqualTo(expectedVersions.size());
        assertThat(helmVersionsResponse.getHelmVersions()).isEqualTo(expectedVersions);

        assertThat(commandCaptor.getValue()).isEqualTo(
                "ls /usr/local/bin/");
    }

    @Test
    public void shouldFailReturnHelmVersions() throws CommandTimedOutException, IOException, InterruptedException {
        ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(1);
        String errorString = "ls: cannot access /usr/local/bin/: No such file or directory";
        response.setCmdResult(errorString);
        when(processExecutor.executeProcess(commandCaptor.capture(), anyInt(), anyBoolean())).thenReturn(response);
        assertThatThrownBy(() -> helmService.getHelmVersions())
                .isInstanceOf(CommandExecutionException.class).hasMessage(errorString);
    }
}
