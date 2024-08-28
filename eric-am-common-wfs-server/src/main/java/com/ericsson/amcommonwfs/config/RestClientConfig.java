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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Value("${health.timeout.connection}")
    private Integer healthCheckConnectionTimeout;

    @Value("${health.timeout.read}")
    private Integer healthCheckReadTimeout;

    @Bean
    public RestTemplateBuilder restTemplateBuilder(RestTemplateAutoConfiguration restTemplateAutoConfiguration,
                                                   ObjectProvider<HttpMessageConverters> messageConverters,
                                                   ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers,
                                                   ObjectProvider<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
        return restTemplateAutoConfiguration
                .restTemplateBuilder(restTemplateAutoConfiguration
                                             .restTemplateBuilderConfigurer(
                                                     messageConverters, restTemplateCustomizers, restTemplateRequestCustomizers));
    }

    @Bean("healthCheckRestTemplate")
    public RestTemplate healthCheckRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.of(healthCheckConnectionTimeout, ChronoUnit.MILLIS))
                .setReadTimeout(Duration.of(healthCheckReadTimeout, ChronoUnit.MILLIS))
                .build();
    }

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
