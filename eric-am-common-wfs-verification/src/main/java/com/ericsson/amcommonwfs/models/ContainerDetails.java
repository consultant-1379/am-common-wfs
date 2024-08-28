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
package com.ericsson.amcommonwfs.models;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.amcommonwfs.exceptions.InvalidAnnotationException;
import com.ericsson.amcommonwfs.exceptions.InvalidContainerState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContainerDetails implements Serializable {
    private static final long serialVersionUID = -6397207992861416844L;

    public enum ContainerState {
        WAITING("Waiting"),
        RUNNING("Running"),
        TERMINATED("Terminated");

        String state;

        ContainerState(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }

        @JsonCreator
        public static ContainerState fromString(String key) {
            for (ContainerState containerState : ContainerState.values()) {
                if (containerState.name().equalsIgnoreCase(key)) {
                    return containerState;
                }
            }
            throw new InvalidContainerState(String.format("Invalid Container state - %s. " +
                    "Valid states are Waiting, Running, Terminated", key));
        }
    }

    private String containerName;

    private ContainerState state;

    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private Boolean isReady;

    private String podName = "defaultPod";

    private boolean isStatePassed;

    @JsonCreator
    public ContainerDetails(@JsonProperty("containerName") String containerName,
                            @JsonProperty("state") ContainerState containerState,
                            @JsonProperty("ready") Boolean isReady) {
        if (StringUtils.isNotEmpty(containerName) && containerState != null && isReady != null) {
            this.containerName = containerName;
            this.state = containerState;
            this.isReady = isReady;
        } else {
            throw new InvalidAnnotationException("Mandatory fields are missing/empty. " +
                    "Please provide valid inputs for containerName, state and ready");
        }
    }


    public ContainerDetails(String containerName, ContainerState state, Boolean isReady, String podName) {
        this.containerName = containerName;
        this.state = state;
        this.isReady = isReady;
        this.podName = podName;
    }

    public Boolean isReady() {
        return isReady;
    }

    public void setReady(Boolean ready) {
        isReady = ready;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
