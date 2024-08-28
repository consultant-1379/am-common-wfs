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
package com.ericsson.amcommonwfs.presentation.services.mapper;

import com.ericsson.workflow.orchestration.mgmt.model.SecretAttribute;
import io.kubernetes.client.openapi.models.V1Secret;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecretResponseMapper {

    SecretResponseMapper INSTANCE = Mappers.getMapper(SecretResponseMapper.class);

    @Mapping(source = "data", target = "data", qualifiedByName = "mapData")
    SecretAttribute mapV1SecretToSecretAttribute(V1Secret source);

    @Named("mapData")
    default Map<String, String> mapData(Map<String, byte[]> data) {
        if (data != null && !data.isEmpty()) {
            Map<String, String> secretData = new HashMap<>();
            data.forEach((key, value) -> secretData.put(key, new String(value, UTF_8)));
            return secretData;
        }
        return Collections.emptyMap();
    }
}
