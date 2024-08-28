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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class KubeClientBuilder {

    private static final String KUBERNETES_WFS_MASTER = "KUBERNETES_WFS_MASTER";
    private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";

    @Value("${kubernetes.api.timeout.connection}")
    private int kubernetesApiConnectionTimeout;

    @Value("${kubernetes.api.timeout.read}")
    private int kubernetesApiReadTimeout;

    @Value("${kubernetes.api.timeout.write}")
    private int kubernetesApiWriteTimeout;

    public CoreV1Api getCoreV1Api(String clusterConfig) throws IOException {
        try {
            ApiClient client = getApiClient(clusterConfig);
            return new CoreV1Api(client);
        } catch (Exception e) {
            LOGGER.error(String.format("Error occurs during get API Client: %s ,full details: ", e.getMessage()), e);
            throw new IOException(String.format("Issue in the clusterConfig : %s. Unable to get the ApiClient.", clusterConfig), e);
        }
    }

    public AppsV1Api getAppsV1Api(String clusterConfig) throws IOException {
        try {
            ApiClient client = getApiClient(clusterConfig);
            return new AppsV1Api(client);
        } catch (Exception e) {
            LOGGER.error(String.format("Error occurs during get API Client: %s ,full details: ", e.getMessage()), e);
            throw new IOException(String.format("Issue in the clusterConfig : %s. Unable to get the ApiClient.", clusterConfig), e);
        }
    }

    public ApiClient getApiClient(final String clusterConfig) throws IOException {
        ApiClient client = StringUtils.isNotEmpty(clusterConfig) && !StringUtils.equalsIgnoreCase(clusterConfig, "default") ?
                Config.fromConfig(clusterConfig) : createDefaultApiClient();
        client.setConnectTimeout(kubernetesApiConnectionTimeout);
        client.setReadTimeout(kubernetesApiReadTimeout);
        client.setWriteTimeout(kubernetesApiWriteTimeout);
        if (clusterConfig == null) {
            LOGGER.debug("Cluster to be used : default");
        } else {
            LOGGER.info("Cluster to be used : {} ", clusterConfig);
        }
        return client;
    }

    private ApiClient createDefaultApiClient() throws IOException {
        String host = System.getenv(KUBERNETES_WFS_MASTER);
        String port = System.getenv(KUBERNETES_SERVICE_PORT);

        if (host != null) {
            LOGGER.debug("Using " + KUBERNETES_WFS_MASTER + " host address {}", host);
            return ClientBuilder.standard().setBasePath(setHostPath(host, port)).build();
        } else {
            return Config.defaultClient();
        }
    }

    private String setHostPath(String host, String port) {
        try {
            int iPort = Integer.parseInt(port);
            URI uri = new URI("https", null, host, iPort, null, null, null);

            return uri.toString();
        } catch (URISyntaxException | NumberFormatException var5) {
            throw new IllegalStateException(var5);
        }
    }
}
