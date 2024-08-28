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
package com.ericsson.amcommonwfs.camunda.bpm;

import com.ericsson.amcommonwfs.CommandTimedOutException;
import com.ericsson.amcommonwfs.ProcessExecutor;
import com.ericsson.amcommonwfs.ProcessExecutorResponse;
import com.ericsson.amcommonwfs.pod.GetPods;
import com.ericsson.amcommonwfs.registry.secret.DeleteSecret;
import com.ericsson.amcommonwfs.secret.CreateAuxSecretCommandHandler;
import com.ericsson.amcommonwfs.secret.CreateCrdAuxSecretCommandHandler;
import com.ericsson.amcommonwfs.secret.DeleteAuxSecretCommandHandler;
import com.ericsson.amcommonwfs.secret.KubeApiHandler;
import com.ericsson.amcommonwfs.service.HelmCommandJobService;
import com.ericsson.amcommonwfs.util.CommandLineArgumentsMatcher;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Status;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.ericsson.amcommonwfs.TestConstants.HELM_HISTORY_COMMAND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@AutoConfigureObservability
@Testcontainers
@TestPropertySource(properties = {"spring.flyway.enabled=false", "redis.cluster.enabled=false", "redis.acl.enabled=false"})
abstract class AbstractBpmnTest {

    private static final String REDIS_CONTAINER_PATH = "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/redis:7.2.3-alpine";

    @Container
    public static GenericContainer<?> redisContainer = getRedisContainer();

    @DynamicPropertySource
    private static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }

    @Autowired
    private HistoryService historyService;

    @Autowired
    protected RuntimeService runtimeService;

    @MockBean
    protected ProcessExecutor executor;

    @MockBean
    protected GetPods getPods;

    @MockBean
    protected HelmCommandJobService helmCommandJobService;

    @MockBean(name = "createAuxSecretCommandHandler", classes = CreateAuxSecretCommandHandler.class)
    @Qualifier("createAuxSecretCommandHandler")
    protected KubeApiHandler createAuxSecretCommandHandler;

    @MockBean(classes = DeleteAuxSecretCommandHandler.class)
    protected KubeApiHandler deleteAuxSecretCommandHandler;

    @MockBean(name = "createCrdAuxSecretCommandHandler", classes = CreateCrdAuxSecretCommandHandler.class)
    protected KubeApiHandler createCrdAuxSecretCommandHandler;

    @MockBean
    protected DeleteSecret deleteSecret;

    protected String pathToTempDirectory = System.getProperty("java.io.tmpdir");

    public String getErrorMessage(final ProcessInstance processInstance1) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance1.getProcessInstanceId()).variableName("errorMessage").singleResult()
                .getValue().toString();
    }

    public void mockCheckForReleaseName()
            throws IOException, ApiException {
        when(getPods.getPodsWithNamespaceWithRetry(anyString(), eq(null), anyString(), anyInt()))
                .thenReturn(getListOfPods());
    }

    public void mockCheckForReleaseNameWithReleaseLabelNotPresent()
            throws IOException, ApiException {
        when(getPods.getPodsWithNamespaceWithRetry(anyString(), eq(null), anyString(), anyInt()))
                .thenReturn(null);
    }

    private V1PodList getListOfPods() {
        V1PodList podList = new V1PodList();
        V1Pod pod = new V1Pod();
        podList.addItemsItem(pod);
        return podList;
    }

    public void mockJobService() {
        doNothing().when(helmCommandJobService).submit(any(), any(), any());
    }

    public void mockCheckForHistory(final String cmdResult)
            throws IOException, InterruptedException, CommandTimedOutException {
        final ProcessExecutorResponse resultResourcesFound = new ProcessExecutorResponse();
        resultResourcesFound.setCmdResult(cmdResult);
        when(executor.executeProcess(eq(String.format(HELM_HISTORY_COMMAND, "my-release")), anyInt(), false))
                .thenReturn(resultResourcesFound);
    }

    /**
     * Mocks calls for auxiliary secret creation to pass. Use for BPMN tests only.
     */
    public void mockCreateAuxiliarySecretCommand(final String secretName, final String namespace) {
        when(createAuxSecretCommandHandler.invokeKubeApiCall(any(), any())).thenReturn(Optional.of(new V1Secret()));
    }

    /**
     * Mocks calls for auxiliary secret deletion to pass. Use for BPMN tests only.
     */
    public void mockDeleteAuxiliarySecretCommand(final String secretName, final String namespace) {
        when(deleteAuxSecretCommandHandler.invokeKubeApiCall(any(), any())).thenReturn(Optional.of(new V1Status()));
    }

    public void mockDeleteSecretCommand() throws Exception {
        doNothing().when(deleteSecret).execute(any());
    }

    /**
     * Generalized method to make verifications from child BPMN tests
     * that specified command was called given number of times during workflow.
     *
     * @param commandPrefix prefix the command starts with or whole command
     * @param times         expected number of command calls
     */
    public void verifyCommandExecuted(String commandPrefix, VerificationMode times)
            throws InterruptedException, CommandTimedOutException, IOException {
        List<String> patternArgs = Arrays.asList(commandPrefix.split(" "));
        verify(executor, times).executeProcess(argThat(new CommandLineArgumentsMatcher(patternArgs)), anyInt());
    }

    protected boolean fileExistInTmp(String fileName) throws IOException {
        String pathToTempDirectory = System.getProperty("java.io.tmpdir");
        File dir = new File(pathToTempDirectory);
        String [] fileParts = fileName.split("\\.");
        if (fileParts.length < 2) {
            return false;
        }
        Pattern pattern = Pattern.compile(String.format("^%s\\d+\\.%s", fileParts[0], fileParts[1]));
        return anyFileMatch(dir, pattern);
    }

    private static GenericContainer<?> getRedisContainer() {
        DockerImageName redisImage = DockerImageName.parse(REDIS_CONTAINER_PATH);
        try (GenericContainer<?> container = new GenericContainer<>(redisImage)) {
            container.withExposedPorts(6379);
            return container;
        }
    }

    public boolean anyFileMatch(File directoryPath, Pattern pattern) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath.toPath())) {
            for (Path filePath : directoryStream) {
                if (isMatchingFileName(filePath, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMatchingFileName(Path filePath, Pattern pattern) {
        String fileName = filePath.getFileName().toString();
        return pattern.matcher(fileName).matches();
    }
}
