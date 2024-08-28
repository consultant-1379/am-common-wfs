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

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeConfig {

    @NotNull(message = "KUBE_CONFIG_API_VERSION_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_API_VERSION_EMPTY")
    @JsonProperty("apiVersion")
    private String apiVersion;

    @NotNull(message = "KUBE_CONFIG_KIND_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_KIND_EMPTY")
    @JsonProperty("kind")
    private String kind;

    @NotNull(message = "KUBE_CONFIG_CURRENT_CONTEXT_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_CURRENT_CONTEXT_EMPTY")
    @JsonProperty("current-context")
    private String currentContext;

    @NotNull(message = "KUBE_CONFIG_CLUSTERS_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_CLUSTERS_EMPTY")
    @Size(max = 1, message = "KUBE_CONFIG_ONE_CLUSTER_ALLOWED")
    @JsonProperty("clusters")
    private List<Cluster> clusters;

    @NotNull(message = "KUBE_CONFIG_CONTEXT_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_CONTEXT_EMPTY")
    @Size(max = 1, message = "KUBE_CONFIG_ONE_CONTEXT_ALLOWED")
    @JsonProperty("contexts")
    private List<Context> contexts;

    @NotNull(message = "KUBE_CONFIG_USER_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_USER_EMPTY")
    @Size(max = 1, message = "KUBE_CONFIG_ONE_USER_ALLOWED")
    @JsonProperty("users")
    private List<User> users;

    @JsonProperty("preferences")
    private Object preferences;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
