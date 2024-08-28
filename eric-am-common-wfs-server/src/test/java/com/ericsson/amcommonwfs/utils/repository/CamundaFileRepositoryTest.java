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
package com.ericsson.amcommonwfs.utils.repository;

import com.ericsson.amcommonwfs.config.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

@SpringBootTest(classes = {
        ObjectMapper.class,
        CamundaFileRepository.class,
        RedisConfig.class,
        RedisAutoConfiguration.class
})
@DirtiesContext
@TestPropertySource(properties = { "redis.cluster.enabled=false", "redis.acl.enabled=false" })
@AutoConfigureObservability
public class CamundaFileRepositoryTest {

    @Autowired
    private CamundaFileRepository camundaFileRepository;

    @Autowired
    private RedisTemplate<String, byte[]> byteRedisTemplate;

    @Test
    public void shouldSucceedWhenSavingValue() {
        camundaFileRepository.save("test-key", "test-value".getBytes(StandardCharsets.UTF_8), 10L);
        Assertions.assertEquals("test-value", new String(camundaFileRepository.get("test-key")));
    }

    @Test
    public void shouldDeleteKeyWhenAfterTTL() throws InterruptedException {
        camundaFileRepository.save("test-key", "test-value".getBytes(StandardCharsets.UTF_8), 10L);
        Assertions.assertEquals("test-value", new String(camundaFileRepository.get("test-key")));

        Thread.sleep(15L * 1000);

        Assertions.assertFalse(byteRedisTemplate.hasKey("test-key"));
    }

    @Test
    public void shouldSucceedWhenRetrievingValue() {
        camundaFileRepository.save("test-key", "test-value".getBytes(StandardCharsets.UTF_8), 10L);
        Assertions.assertEquals("test-value", new String(camundaFileRepository.get("test-key")));
    }

    static {
        String image = "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/redis:5.0.3-alpine";
        var redisContainer = new GenericContainer<>(DockerImageName.parse(image)
                .asCompatibleSubstituteFor("redis"))
                .withExposedPorts(6379);
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getContainerIpAddress());
        System.setProperty("spring.data.redis.port", redisContainer.getFirstMappedPort().toString());
    }

}
