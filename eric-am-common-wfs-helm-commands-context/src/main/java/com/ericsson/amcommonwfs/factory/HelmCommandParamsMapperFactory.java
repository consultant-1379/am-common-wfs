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
package com.ericsson.amcommonwfs.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.exception.CommandContextException;
import com.ericsson.amcommonwfs.service.HelmCommandParamsMapper;

@Component
public class HelmCommandParamsMapperFactory {

    private static final Map<String, HelmCommandParamsMapper> HELM_COMMANDS_CONTEXTS = new HashMap<>();

    public HelmCommandParamsMapperFactory(List<HelmCommandParamsMapper> helmCommandParamsMappers) {
        for (HelmCommandParamsMapper mapper : helmCommandParamsMappers) {
            HELM_COMMANDS_CONTEXTS.put(mapper.getType().getCommandType(), mapper);
        }
    }

    public HelmCommandParamsMapper getMapper(String type) {
        HelmCommandParamsMapper mapper = HELM_COMMANDS_CONTEXTS.get(type);

        if (mapper == null) {
            throw new CommandContextException("Unknown helm command context : " + type);
        }
        return mapper;
    }
}
