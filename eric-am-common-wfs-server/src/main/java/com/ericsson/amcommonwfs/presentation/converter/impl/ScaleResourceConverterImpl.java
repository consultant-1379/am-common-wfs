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
package com.ericsson.amcommonwfs.presentation.converter.impl;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.API_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.OVERRIDE_GLOBAL_REGISTRY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.STATE;
import static com.ericsson.workflow.orchestration.mgmt.model.ApiVersion.API_V3;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.amcommonwfs.presentation.converter.AbstractV3ResourceConverter;
import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;

@Component
public class ScaleResourceConverterImpl extends AbstractV3ResourceConverter<Map<String, Object>, ResourceInfoDto<ScaleInfo>> {

    @Autowired
    public ScaleResourceConverterImpl(@Value("${cluster.config.directory}") String clusterConfigDir,
                                        @Value("${app.command.execute.defaultTimeOut}") String defaultCommandTimeOut) {
        super(clusterConfigDir, defaultCommandTimeOut);
    }

    @Override
    public Map<String, Object> convert(ResourceInfoDto<ScaleInfo> parameter) {
        ScaleInfo scaleInfo = parameter.getInfo();
        final Map<String, Object> convertedVariables = new HashMap<>();

        convertedVariables.put(RELEASE_NAME, parameter.getReleaseName());
        convertedVariables.put(NAMESPACE, scaleInfo.getNamespace());
        convertedVariables.put(LIFECYCLE_OPERATION_ID, scaleInfo.getLifecycleOperationId());
        convertedVariables.put(STATE, scaleInfo.getState());
        convertedVariables.put(API_VERSION, API_V3.toString());
        convertedVariables.put(CHART_URL, scaleInfo.getChartUrl());
        convertedVariables.put(SKIP_VERIFICATION, true);
        convertedVariables.put(OVERRIDE_GLOBAL_REGISTRY, scaleInfo.getOverrideGlobalRegistry());

        putAdditionalParameters(scaleInfo, convertedVariables);
        putMandatoryResourceParams(convertedVariables, scaleInfo.getApplicationTimeOut(), scaleInfo.getClusterName());
        putHelmClientVersionParam(scaleInfo.getHelmClientVersion(), convertedVariables);
        return convertedVariables;
    }

    private static void putAdditionalParameters(ScaleInfo scaleInfo, Map<String, Object> convertedVariables) {
        Map<String, String> additionalParameters = scaleInfo.getAdditionalParams();
        if (CollectionUtils.isEmpty(additionalParameters)) {
            additionalParameters = new HashMap<>();
        }
        additionalParameters.putAll(getReplicaParameters(scaleInfo.getScaleResources()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))));

        convertedVariables.put(ADDITIONAL_PARAMS, additionalParameters);
    }

    private static Map<String, Integer> getReplicaParameters(Map<String, Map<String, Integer>> scaleResources) {
        Map<String, Integer> allScaleParameters = new HashMap<>();

        for (Map.Entry<String, Map<String, Integer>> entry : scaleResources.entrySet()) {
            allScaleParameters.putAll(scaleResources.get(entry.getKey()));
        }
        return allScaleParameters;
    }

}
