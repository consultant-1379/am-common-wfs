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
package com.ericsson.amcommonwfs.cluster.config.service.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContextData {

    @NotNull(message = "KUBE_CONFIG_CLUSTER_NAME_REQUIRED_IN_CONTEXT_DATA")
    @Size(min = 1, message = "KUBE_CONFIG_CLUSTER_NAME_EMPTY_IN_CONTEXT_DATA")
    @JsonProperty("cluster")
    private String cluster;

    @NotNull(message = "KUBE_CONFIG_USER_NAME_REQUIRED_IN_CONTEXT")
    @Size(min = 1, message = "KUBE_CONFIG_USER_NAME_EMPTY_IN_CONTEXT")
    @JsonProperty("user")
    private String user;

    @JsonProperty("namespace")
    private String namespace;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
