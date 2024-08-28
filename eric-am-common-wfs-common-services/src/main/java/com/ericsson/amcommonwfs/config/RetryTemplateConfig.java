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

import com.ericsson.amcommonwfs.model.RetryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryTemplateConfig {

    private RetryProperties retryProperties;

    @Autowired
    public RetryTemplateConfig(final RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Bean
    public RetryTemplate genericWfsRetryTemplate() {
        RetryProperties.Service defaultWfsProp = retryProperties.getGeneric();
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(defaultWfsProp.getInitialBackoff());
        backOffPolicy.setMaxInterval(defaultWfsProp.getMaxBackoff());
        backOffPolicy.setMultiplier(defaultWfsProp.getMultiplier());

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(new WorkflowServiceExceptionClassifierRetryPolicy(defaultWfsProp.getMaxAttempts()));
        return retryTemplate;
    }

    @Bean
    public RetryTemplate kubectlApiRetryTemplate() {
        RetryProperties.Service kubectlApiProp = retryProperties.getKubectlApiRetry();
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(kubectlApiProp.getInitialBackoff());
        backOffPolicy.setMaxInterval(kubectlApiProp.getMaxBackoff());
        backOffPolicy.setMultiplier(kubectlApiProp.getMultiplier());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(new KubectlAPIServiceExceptionClassifierRetryPolicy(kubectlApiProp.getMaxAttempts()));
        return retryTemplate;
    }
}
