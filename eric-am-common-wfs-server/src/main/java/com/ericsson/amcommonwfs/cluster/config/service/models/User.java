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
public class User {

    @NotNull(message = "KUBE_CONFIG_USER_NAME_REQUIRED")
    @Size(min = 1, message = "KUBE_CONFIG_USER_NAME_EMPTY")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "KUBE_CONFIG_USER_DATA_REQUIRED")
    @JsonProperty("user")
    private UserData userData;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
