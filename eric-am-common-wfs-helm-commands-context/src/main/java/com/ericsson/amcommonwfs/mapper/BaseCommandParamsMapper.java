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
package com.ericsson.amcommonwfs.mapper;

import static com.ericsson.amcommonwfs.util.ProvideCommandParamsUtils.provideBaseParams;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.service.HelmCommandParamsMapper;
import com.google.common.annotations.VisibleForTesting;

@Component
public abstract class BaseCommandParamsMapper implements HelmCommandParamsMapper {

    private boolean helmDebug;

    @Override
    public Map<String, Object> apply(final DelegateExecution execution) {
        Map<String, Object> commandParams = new HashMap<>();

        provideBaseParams(execution, commandParams, helmDebug);

        return commandParams;
    }

    @Value("${helm.debug.enabled}")
    @VisibleForTesting
    protected void setHelmDebug(boolean helmDebug) {
        this.helmDebug = helmDebug;
    }
}
