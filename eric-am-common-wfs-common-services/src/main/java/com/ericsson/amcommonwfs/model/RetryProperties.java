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
package com.ericsson.amcommonwfs.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "retry")
public class RetryProperties {
    private Service kubectlApiRetry;
    private Service defaultWfsRetry;

    public RetryProperties() {
        kubectlApiRetry = new Service();
        defaultWfsRetry = new Service();
    }

    public Service getKubectlApiRetry() {
        return kubectlApiRetry;
    }

    public void setKubectlApiRetry(Service kubectlApiRetry) {
        this.kubectlApiRetry = kubectlApiRetry;
    }

    public Service getGeneric() {
        return defaultWfsRetry;
    }

    public void setGeneric(Service genericWfs) {
        this.defaultWfsRetry = genericWfs;
    }

    @Getter
    @Setter
    public static class Service {
        private int maxAttempts;
        private long initialBackoff;
        private long maxBackoff;
        private double multiplier;
    }
}