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
package com.ericsson.amcommonwfs.config;

import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.model.WorkingAsyncRequestDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static java.lang.Boolean.TRUE;

import java.time.Duration;
import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${redis.acl.enabled}")
    private Boolean isRedisACLEnabled;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer(List<Module> jacksonModules) {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    @ConditionalOnProperty(name = "redis.cluster.enabled")
    public LettuceClientConfiguration clientConfiguration() {

        DefaultClientResources clientResources = DefaultClientResources.create();

        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofMillis(10000L))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(refreshOptions)
                .build();

        return LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clientOptions)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "redis.cluster.enabled")
    public RedisConnectionFactory redisConnectionFactory(final LettuceClientConfiguration clientConfiguration) {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.addClusterNode(new RedisServer(redisHost, redisPort));
        if (TRUE == isRedisACLEnabled) {
            clusterConfiguration.setUsername(redisUsername);
            clusterConfiguration.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
    }

    @Bean("redisTemplate")
    public RedisTemplate<String, String> redisTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        return buildRedisTemplate(connectionFactory, String.class, objectMapper);
    }

    @Bean("redisDeleteNamespaceTemplate")
    public RedisTemplate<String, AsyncDeleteNamespaceRequestDetails> redisDeleteNamespaceTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        return buildRedisTemplate(connectionFactory, AsyncDeleteNamespaceRequestDetails.class, objectMapper);
    }

    @Bean("redisDeletePvcsTemplate")
    public RedisTemplate<String, AsyncDeletePvcsRequestDetails> redisDeletePvcsTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        return buildRedisTemplate(connectionFactory, AsyncDeletePvcsRequestDetails.class, objectMapper);
    }

    @Bean("redisWorkingDeleteNamespaceTemplate")
    public RedisTemplate<String, WorkingAsyncRequestDetails<AsyncDeleteNamespaceRequestDetails>> redisWorkingDeleteNamespaceTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        TypeReference<WorkingAsyncRequestDetails<AsyncDeleteNamespaceRequestDetails>> typeReference = new TypeReference<>() { };
        return buildRedisTemplate(connectionFactory, objectMapper.getTypeFactory().constructType(typeReference), objectMapper);
    }

    @Bean("redisWorkingDeletePvcsTemplate")
    public RedisTemplate<String, WorkingAsyncRequestDetails<AsyncDeletePvcsRequestDetails>> redisWorkingDeletePvcsTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        TypeReference<WorkingAsyncRequestDetails<AsyncDeletePvcsRequestDetails>> typeReference = new TypeReference<>() { };
        return buildRedisTemplate(connectionFactory, objectMapper.getTypeFactory().constructType(typeReference), objectMapper);
    }

    @Bean("redisByteTemplate")
    public RedisTemplate<String, byte[]> redisByteTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        return buildRedisTemplate(connectionFactory, byte[].class, objectMapper);
    }

    @Bean
    public RedisHealthIndicator redisHealthIndicator(final RedisConnectionFactory connectionFactory) {
        return new RedisHealthIndicator(connectionFactory);
    }

    private <V> RedisTemplate<String, V> buildRedisTemplate(final RedisConnectionFactory connectionFactory,
                                                            Class<V> valueType, ObjectMapper objectMapper) {
        RedisTemplate<String, V> redisTemplate = new RedisTemplate<>();
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        if (String.class.equals(valueType)) {
            redisTemplate.setDefaultSerializer(keySerializer);
            redisTemplate.setHashValueSerializer(keySerializer);
        } else if (byte[].class.equals(valueType)) {
            redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        } else {
            Jackson2JsonRedisSerializer<V> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, valueType);
            redisTemplate.setValueSerializer(valueSerializer);
            redisTemplate.setHashValueSerializer(valueSerializer);
        }
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    private <V> RedisTemplate<String, V> buildRedisTemplate(final RedisConnectionFactory connectionFactory,
                                                            JavaType valueType, ObjectMapper objectMapper) {
        RedisTemplate<String, V> redisTemplate = new RedisTemplate<>();
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);

        Jackson2JsonRedisSerializer<V> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, valueType);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
