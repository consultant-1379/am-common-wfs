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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Resources;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { KubeClientBuilder.class })
@TestPropertySource(properties = {
        "kubernetes.api.timeout.connection=30000",
        "kubernetes.api.timeout.read=10000",
        "kubernetes.api.timeout.write=10000" })
public class KubeClientBuilderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeClientBuilderTest.class);

    private static final String BROKEN_CONFIG_PATH = "broken-config.config";
    private static final int PORT_NUMBER_MINIMUM = 5000;
    private static final int PORT_NUMBER_MAXIMUM = 5100;
    private static final Random RANDOM = new Random();

    private static final int KUBERNETES_API_PORT = PORT_NUMBER_MINIMUM + RANDOM.nextInt(PORT_NUMBER_MAXIMUM - PORT_NUMBER_MINIMUM);

    @Value("${kubernetes.api.timeout.read}")
    private int kubernetesApiReadTimeout;

    @Autowired
    private KubeClientBuilder kubeClientBuilder;

    private static MockedStatic<Config> staticConfigMock;

    @BeforeEach
    public void init() {
        staticConfigMock = Mockito.mockStatic(Config.class);
    }

    @AfterEach
    public void after() {
        staticConfigMock.close();
    }

    @Test
    public void testCoreV1APIWithDefaultClientReturned() throws IOException {
        ApiClient apiClient = ClientBuilder.standard().build();
        staticConfigMock.when(Config::defaultClient).thenReturn(apiClient);
        CoreV1Api expected = new CoreV1Api(apiClient);
        CoreV1Api actual = kubeClientBuilder.getCoreV1Api("default");

        assertEquals(expected.getApiClient(), actual.getApiClient());
    }

    @Test
    public void testIOExceptionIsThrown() {
        staticConfigMock.when(Config::defaultClient).thenThrow(new RuntimeException());
        String clusterConfig = "default";

        String expectedMessage = String.format("Issue in the clusterConfig : %s. Unable to get the ApiClient.", clusterConfig);
        String actualMessage = assertThrows(IOException.class, () -> kubeClientBuilder.getCoreV1Api(clusterConfig)).getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    /**
     * This test verifies that network delays do not result in thread freezes
     * when trying to access Kubernetes Api via CoreV1Api objects created by
     * {@link KubeClientBuilder} class. This is done by creating and running 2
     * threads. The 1st thread opens a SocketConnection on {@code KUBERNETES_API_PORT}
     * and waits to receive exactly 1 connection. The 2nd thread accepts a
     * broken kubeconfig path which poits to {@code https://localhost: KUBERNETES_API_PORT}
     * and executes list namespaces request. The main thread waits for the expected
     * timeout and fails the test if the 2nd thread is still alive after the timeout
     * has elapsed.
     */
    @Test
    public void testKubernetesApiCallDoesNotFreeze() throws URISyntaxException, InterruptedException, IOException {
        // given
        int threadSchedulingTimeout = 5000; // 5 seconds
        int connectionFreezeTimeout = kubernetesApiReadTimeout + threadSchedulingTimeout;
        int socketReadTimeout = connectionFreezeTimeout + threadSchedulingTimeout;
        String kubeConfigPath = createBrokenKubeconfigFile();

        CountDownLatch socketHasBeenOpenedLatch = new CountDownLatch(1);

        Thread socketConnectionThread = new Thread(new SocketConnection(
                KUBERNETES_API_PORT, threadSchedulingTimeout, socketReadTimeout, socketHasBeenOpenedLatch), "Socket connection thread");

        Thread kubernetesApiRequestThread = new Thread(new KubernetesApiRequest(kubeConfigPath, kubeClientBuilder), "Kubernetes API thread");

        // when
        socketConnectionThread.start();
        socketHasBeenOpenedLatch.await();
        kubernetesApiRequestThread.start();

        boolean apiRequestThreadFroze;
        try {
            kubernetesApiRequestThread.join(connectionFreezeTimeout);
            apiRequestThreadFroze = kubernetesApiRequestThread.isAlive();
        } catch (InterruptedException e) {
            LOGGER.error("Received exception while waiting for API thread to die", e);
            throw new RuntimeException(e);
        }
        socketConnectionThread.join();

        // then
        assertFalse(apiRequestThreadFroze, "Kubernetes API request froze when trying to use broken kubeconfig");
    }

    private String createBrokenKubeconfigFile() throws URISyntaxException, IOException {
        String originalKubeconfigPath = Paths.get(Resources.getResource(BROKEN_CONFIG_PATH).toURI()).toString();
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        Map<String, Object> kubeconfigYaml;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(originalKubeconfigPath))) {
            kubeconfigYaml = yaml.load(reader);
        }
        setClusterServer(kubeconfigYaml);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(originalKubeconfigPath))) {
            yaml.dump(kubeconfigYaml, writer);
        }
        return originalKubeconfigPath;
    }

    @SuppressWarnings(value = "unchecked")
    private void setClusterServer(final Map<String, Object> kubeconfigYaml) {
        List<Map<String, Object>> clusters = (List<Map<String, Object>>) kubeconfigYaml.get("clusters");
        Map<String, Object> cluster = clusters.get(0);
        Map<String, Object> clusterData = (Map<String, Object>) cluster.get("cluster");
        clusterData.put("server", getKubernetesApiHost());
    }

    private String getKubernetesApiHost() {
        return String.format("https://localhost:%d", KUBERNETES_API_PORT);
    }

    private static class SocketConnection implements Runnable {
        private static final Logger LOGGER = LoggerFactory.getLogger(SocketConnection.class);
        private static final int READ_BUFFER_SIZE = 1024;

        private final int portNumber;
        private final int acceptTimeout;
        private final int readTimeout;
        private final CountDownLatch socketHasBeenOpenedLatch;

        public SocketConnection(int portNumber, int acceptTimeout, int readTimeout, CountDownLatch socketHasBeenOpenedLatch) {
            this.portNumber = portNumber;
            this.acceptTimeout = acceptTimeout;
            this.readTimeout = readTimeout;
            this.socketHasBeenOpenedLatch = socketHasBeenOpenedLatch;
        }

        @Override
        public void run() {
            LOGGER.info("Started socket connection thread");
            BufferedReader reader = null;

            try (ServerSocket serverSocket = new ServerSocket(portNumber, 50, InetAddress.getLocalHost())) {
                LOGGER.info("Opened port {}", portNumber);
                serverSocket.setSoTimeout(acceptTimeout);
                LOGGER.info("Set socket connection timeout to {} milliseconds", acceptTimeout);

                socketHasBeenOpenedLatch.countDown();

                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Accepted a new connection");
                clientSocket.setSoTimeout(readTimeout);
                LOGGER.info("Set connection read timeout to {} milliseconds", readTimeout);

                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                char[] buffer = new char[READ_BUFFER_SIZE];
                boolean proceed = true;
                while (proceed) {
                    int readEndIndex = reader.read(buffer);
                    if (readEndIndex > 0) {
                        logBuffer(buffer, readEndIndex);
                    } else {
                        LOGGER.info("Connection has been terminated by the client");
                        proceed = false;
                    }
                }
            } catch (IOException e) {
                logException(e);
                throw new RuntimeException(e);
            } finally {
                if (this.socketHasBeenOpenedLatch.getCount() > 0) {
                    socketHasBeenOpenedLatch.countDown();
                }
                closeReader(reader);
            }
        }

        private void closeReader(final Reader reader) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logException(e);
                    throw new RuntimeException(e);
                }
            }
        }

        private void logBuffer(char[] buffer, int readEndIndex) {
            String readLine = new String(buffer, 0, readEndIndex);
            LOGGER.info("Read line: {}", readLine);
        }

        private void logException(IOException exception) {
            LOGGER.error("Exception occurred while processing socket connection", exception);
        }
    }

    private static class KubernetesApiRequest implements Runnable {
        private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesApiRequest.class);

        private final String kubeConfigPath;
        private final KubeClientBuilder kubeClientBuilder;

        public KubernetesApiRequest(String kubeConfigPath, KubeClientBuilder kubeClientBuilder) {
            this.kubeConfigPath = kubeConfigPath;
            this.kubeClientBuilder = kubeClientBuilder;
        }

        @Override
        public void run() {
            try {
                listNamespaces(kubeConfigPath);
            } catch (ApiException | IOException e) {
                logException(e);
                throw new RuntimeException(e);
            }
        }

        private void listNamespaces(String kubeConfigPath) throws ApiException, IOException {
            CoreV1Api api = kubeClientBuilder.getCoreV1Api(kubeConfigPath);
            api.listNamespace().pretty(Boolean.FALSE.toString()).allowWatchBookmarks(Boolean.FALSE).watch(Boolean.FALSE).execute();
        }

        private void logException(Exception e) {
            String message = String.format("Error while calling k8s API with %s kubeconfig", kubeConfigPath);
            LOGGER.error(message, e);
        }
    }
}
