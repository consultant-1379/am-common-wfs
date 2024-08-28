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

import static com.ericsson.amcommonwfs.utils.constants.Constants.API_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.LIFECYCLE_OPERATION_ID;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.STATE;
import static com.ericsson.workflow.orchestration.mgmt.model.ApiVersion.API_V3;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.presentation.converter.AbstractV3ResourceConverter;
import com.ericsson.amcommonwfs.presentation.dto.TerminateInfoDto;

@Component
public class TerminateResourceConverterImpl extends AbstractV3ResourceConverter<Map<String, Object>, TerminateInfoDto> {

    @Autowired
    public TerminateResourceConverterImpl(@Value("${cluster.config.directory}") String clusterConfigDir,
                                          @Value("${app.command.execute.defaultTimeOut}") String defaultCommandTimeOut) {
        super(clusterConfigDir, defaultCommandTimeOut);
    }

    @Override
    public Map<String, Object> convert(TerminateInfoDto parameter) {
        Map<String, Object> convertedVariables = new HashMap<>();

        convertedVariables.put(SKIP_VERIFICATION, BooleanUtils.toBooleanDefaultIfNull(parameter.getSkipVerification(), false));
        convertedVariables.put(SKIP_JOB_VERIFICATION, BooleanUtils.toBooleanDefaultIfNull(parameter.getSkipJobVerification(), false));
        convertedVariables.put(NAMESPACE, parameter.getNamespace());

        String clusterName = parameter.getClusterName();
        putIfNotEmptyString(CLUSTER_NAME, clusterName, convertedVariables);

        String applicationTimeOut = parameter.getApplicationTimeOut();
        convertedVariables.put(APPLICATION_TIME_OUT, StringUtils.isNumeric(applicationTimeOut) ? applicationTimeOut : defaultCommandTimeOut);

        convertedVariables.put(RELEASE_NAME, parameter.getReleaseName());
        convertedVariables.put(LIFECYCLE_OPERATION_ID, parameter.getLifecycleOperationId());
        convertedVariables.put(STATE, parameter.getState());
        convertedVariables.put(API_VERSION, API_V3.toString());

        putClusterConfigParam(clusterName, convertedVariables);
        putHelmClientVersionParam(parameter.getHelmClientVersion(), convertedVariables);

        return convertedVariables;
    }

}
