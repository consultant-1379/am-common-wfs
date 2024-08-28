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
package com.ericsson.amcommonwfs.registry.secret;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

@JsonSerialize(using = DockerConfigHost.CustomSerializer.class)
@Getter
@Setter
public class DockerConfigHost {

    @JsonProperty("dockerHost")
    private DockerConfigCredentials dockerConfigCredentials;

    @JsonIgnore
    private String dockerHost;

    static class CustomSerializer extends JsonSerializer<DockerConfigHost> {

        @Override
        public void serialize(DockerConfigHost dockerConfigHost, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectFieldStart(dockerConfigHost.getDockerHost());
            jsonGenerator.writeObjectField("username", dockerConfigHost.getDockerConfigCredentials().getUsername());
            jsonGenerator.writeObjectField("password", dockerConfigHost.getDockerConfigCredentials().getPassword());
            jsonGenerator.writeObjectField("auth", dockerConfigHost.getDockerConfigCredentials().getAuth());
            jsonGenerator.writeEndObject();
        }
    }
}
