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
package com.ericsson.amcommonwfs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.amcommonwfs.exception.CommandException;

@Service
public class HelmCommandFactory {

    private static final Map<String, HelmCommand> MY_SERVICE_CACHE = new HashMap<>();
    @Autowired
    private List<HelmCommand> services;

    @PostConstruct
    public void initMyServiceCache() {
        for (HelmCommand service : services) {
            MY_SERVICE_CACHE.put(service.getType().getCommandType(), service);
        }
    }

    public static HelmCommand getService(String type) {
        HelmCommand service = MY_SERVICE_CACHE.get(type);
        if (service == null)
            throw new CommandException("Unknown helm command : " + type);
        return service;
    }

}
