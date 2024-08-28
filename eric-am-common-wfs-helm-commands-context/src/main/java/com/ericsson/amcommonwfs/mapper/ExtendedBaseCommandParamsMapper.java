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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.BooleanUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

import static com.ericsson.amcommonwfs.util.Constant.SET_FLAG_VALUES;
import static com.ericsson.amcommonwfs.util.ProvideCommandParamsUtils.overrideGlobalRegistry;
import static com.ericsson.amcommonwfs.util.ProvideCommandParamsUtils.provideAdditionalParam;
import static com.ericsson.amcommonwfs.util.ProvideCommandParamsUtils.provideChartParams;
import static com.ericsson.amcommonwfs.util.ProvideCommandParamsUtils.provideValuesFilesParams;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.OVERRIDE_GLOBAL_REGISTRY;

@Component
public abstract class ExtendedBaseCommandParamsMapper extends BaseCommandParamsMapper {

    @Value("${docker.registry.url}")
    private String dockerRegistryUrl;

    @Value("${containerRegistry.global.registry.pullSecret}")
    private String imagePullRegistrySecretName;

    private boolean isApplyDeprecatedDesignRules;

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> apply(final DelegateExecution execution) {
        Map<String, Object> commandParams = super.apply(execution);

        provideChartParams(execution, commandParams);

        provideValuesFilesParams(execution, commandParams);

        commandParams.put(DISABLE_OPENAPI_VALIDATION, true);

        final Map<String, String> additionalParams = (Map<String, String>) execution.getVariable(ADDITIONAL_PARAMS);

        ArrayList<String> setFlagValues = new ArrayList<>();

        if (!CollectionUtils.isEmpty(additionalParams)) {
            additionalParams.forEach((key, value) -> provideAdditionalParam(key, value, commandParams, setFlagValues));
        }

        final boolean overrideGlobalRegistry = BooleanUtils.toBooleanDefaultIfNull(
                (Boolean) execution.getVariable(OVERRIDE_GLOBAL_REGISTRY), false);

        overrideGlobalRegistry(imagePullRegistrySecretName, dockerRegistryUrl, overrideGlobalRegistry,
                execution, isApplyDeprecatedDesignRules, setFlagValues);

        if (!setFlagValues.isEmpty()) {
            commandParams.put(SET_FLAG_VALUES, setFlagValues);
        }

        return commandParams;
    }

    @Value("${applyDeprecatedDesignRules}")
    @VisibleForTesting
    protected void setIsApplyDeprecatedDesignRules(boolean isApplyDeprecatedDesignRules) {
        this.isApplyDeprecatedDesignRules = isApplyDeprecatedDesignRules;
    }
}
