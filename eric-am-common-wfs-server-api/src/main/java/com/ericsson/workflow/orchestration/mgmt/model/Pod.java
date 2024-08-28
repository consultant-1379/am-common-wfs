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
package com.ericsson.workflow.orchestration.mgmt.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.kubernetes.client.openapi.models.V1OwnerReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Pod {
    private String uid;
    private String name;
    private String status;
    private String namespace;
    private String hostname;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    List<V1OwnerReference> ownerReferences;

    public Pod(final String uid, final String name, final String status, final String namespace, final String hostName) {
        this.uid = uid;
        this.name = name;
        this.status = status;
        this.namespace = namespace;
        this.hostname = hostName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
