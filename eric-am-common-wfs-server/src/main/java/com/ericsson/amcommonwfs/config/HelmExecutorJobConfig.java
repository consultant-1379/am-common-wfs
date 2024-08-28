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

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.kubernetes.client.openapi.models.V1Job;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class HelmExecutorJobConfig {

    private ObjectMapper mapper;

    public HelmExecutorJobConfig() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Value("${helmExecutor.job.template.path}")
    private String helmExecutorJobTemplatePath;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public V1Job helmExecutorJobTemplate() throws IOException {

        return mapper.readValue(new File(helmExecutorJobTemplatePath), V1Job.class);
    }
}
