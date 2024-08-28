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
package com.ericsson.amcommonwfs.registry.secret;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class DockerConfigUtils {

    @Value("${docker.registry.url}")
    private String dockerRegistryUrl;

    @Value("${docker.registry.username}")
    private String dockerRegistryUsername;

    @Value("${docker.registry.password}")
    private String dockerRegistryPassword;

    @VisibleForTesting
    String constructDockerConfigJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String auth = Base64.getEncoder()
                .encodeToString((dockerRegistryUsername + ":" + dockerRegistryPassword).getBytes()); // NOSONAR
        DockerConfigCredentials dockerConfigCredentials = new DockerConfigCredentials();
        dockerConfigCredentials.setAuth(auth);
        dockerConfigCredentials.setPassword(dockerRegistryPassword);
        dockerConfigCredentials.setUsername(dockerRegistryUsername);

        DockerConfigHost dockerConfigHost = new DockerConfigHost();
        dockerConfigHost.setDockerConfigCredentials(dockerConfigCredentials);
        dockerConfigHost.setDockerHost(dockerRegistryUrl);

        DockerSecretAuths dockerSecretAuths = new DockerSecretAuths();
        dockerSecretAuths.setDockerConfigHost(dockerConfigHost);
        return objectMapper.writeValueAsString(dockerSecretAuths);
    }
}
