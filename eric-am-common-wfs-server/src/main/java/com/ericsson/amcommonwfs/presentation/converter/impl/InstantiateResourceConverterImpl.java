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

import static com.ericsson.amcommonwfs.services.utils.CommonServicesUtils.convertObjToJsonString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.API_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_TYPE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DEFAULT_NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.OVERRIDE_GLOBAL_REGISTRY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.STATE;
import static com.ericsson.workflow.orchestration.mgmt.model.ApiVersion.API_V3;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.presentation.converter.AbstractV3ResourceConverter;
import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;

@Component
public class InstantiateResourceConverterImpl extends AbstractV3ResourceConverter<Map<String, Object>, ResourceInfoDto<InstantiateInfo>> {

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    public InstantiateResourceConverterImpl(@Value("${cluster.config.directory}") String clusterConfigDir,
                                            @Value("${app.command.execute.defaultTimeOut}") String defaultCommandTimeOut) {
        super(clusterConfigDir, defaultCommandTimeOut);
    }

    @Override
    public Map<String, Object> convert(ResourceInfoDto<InstantiateInfo> parameter) {
        InstantiateInfo instantiateInfo = parameter.getInfo();
        Map<String, Object> convertedVariables = new HashMap<>();

        convertedVariables.put(NAMESPACE, StringUtils.defaultIfEmpty(instantiateInfo.getNamespace(), DEFAULT_NAMESPACE));
        convertedVariables.put(RELEASE_NAME, parameter.getReleaseName());
        convertedVariables.put(LIFECYCLE_OPERATION_ID, instantiateInfo.getLifecycleOperationId());
        convertedVariables.put(STATE, instantiateInfo.getState());
        convertedVariables.put(API_VERSION, API_V3.toString());
        convertedVariables.put(SKIP_VERIFICATION, BooleanUtils.toBooleanDefaultIfNull(instantiateInfo.getSkipVerification(), false));
        convertedVariables.put(SKIP_JOB_VERIFICATION, BooleanUtils.toBooleanDefaultIfNull(instantiateInfo.getSkipJobVerification(), false));
        convertedVariables.put(CHART_TYPE, instantiateInfo.getChartType() != null ?
                instantiateInfo.getChartType().toString() : InstantiateInfo.ChartTypeEnum.CNF.toString());
        return convertVariablesMap(instantiateInfo, convertedVariables);
    }

    private Map<String, Object> convertVariablesMap(InstantiateInfo instantiateInfo, Map<String, Object> variables) {
        variables.put(OVERRIDE_GLOBAL_REGISTRY, instantiateInfo.getOverrideGlobalRegistry());
        putIfNotEmptyMap(ADDITIONAL_PARAMS, instantiateInfo.getAdditionalParams(), variables);
        if (!CollectionUtils.isEmpty(instantiateInfo.getDay0Configuration())) {
            putIfNotEmptyList(DAY0_CONFIGURATION, encryptDay0Configuration(instantiateInfo.getDay0Configuration()), variables);
        }
        putChartParams(instantiateInfo, variables);
        putMandatoryResourceParams(variables, instantiateInfo.getApplicationTimeOut(),
                instantiateInfo.getClusterName());
        putHelmClientVersionParam(instantiateInfo.getHelmClientVersion(), variables);
        return variables;
    }


    private List<String> encryptDay0Configuration(Map<String, Object> day0Configuration) {
        String configurationAsString = convertObjToJsonString(day0Configuration);
        String encryptedDay0Configuration = cryptoService.encryptString(configurationAsString);
        if (StringUtils.isNotEmpty(encryptedDay0Configuration)) {
            return List.of(encryptedDay0Configuration);
        }
        return Collections.emptyList();
    }

    private static void putChartParams(InstantiateInfo instantiateInfo, Map<String, Object> variables) {
        String chartUrl = instantiateInfo.getChartUrl();

        if (StringUtils.isEmpty(chartUrl)) {
            variables.put(CHART_NAME, instantiateInfo.getChartName());
            variables.put(CHART_VERSION, instantiateInfo.getChartVersion());
        } else {
            variables.put(CHART_URL, chartUrl);
            if (instantiateInfo.getChartType() != null && instantiateInfo.getChartType() == InstantiateInfo.ChartTypeEnum.CRD) {
                variables.put(CHART_VERSION, instantiateInfo.getChartVersion());
            }
        }
    }
}
