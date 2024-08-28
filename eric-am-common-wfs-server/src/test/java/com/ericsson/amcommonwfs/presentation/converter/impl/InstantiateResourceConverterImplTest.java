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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DAY0_CONFIGURATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.google.common.collect.Maps;

public class InstantiateResourceConverterImplTest extends AbstractResourceConverterTest<ResourceInfoDto<InstantiateInfo>> {

    private final InstantiateResourceConverterImpl instantiateResourceConverterImpl =
            new InstantiateResourceConverterImpl(CLUSTER_CONFIG_DIR, DEFAULT_COMMAND_TIME_OUT);

    private ResourceInfoDto<InstantiateInfo> testResourceInfo;

    @BeforeEach
    public void setUp() {
        testResourceInfo = testResourceDtoFactory.createTestInstantiateInfo();
    }

    @Test
    public void shouldNotSetEmptyValues() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();
        instantiateInfo.setAdditionalParams(Maps.newHashMap());
        instantiateInfo.setDay0Configuration(Maps.newHashMap());
        instantiateInfo.setClusterName(null);

        assertEmptyValuesNotIncluded(testResourceInfo, instantiateResourceConverterImpl,
                                     Lists.newArrayList(ADDITIONAL_PARAMS, DAY0_CONFIGURATION, CLUSTER_NAME));
    }

    @Test
    public void shouldSetDefaultParameters() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();
        instantiateInfo.setNamespace(null);
        instantiateInfo.setCleanUpResources(null);
        instantiateInfo.setSkipVerification(null);
        instantiateInfo.setSkipJobVerification(null);
        instantiateInfo.setHelmClientVersion(null);

        Map<String, Object> expectedDefaultValues = new HashMap<>();
        expectedDefaultValues.put(NAMESPACE, "default");
        expectedDefaultValues.put(SKIP_VERIFICATION, false);
        expectedDefaultValues.put(SKIP_JOB_VERIFICATION, false);
        expectedDefaultValues.put(HELM_CLIENT_VERSION, "helm");

        assertDefaultValuesPresent(testResourceInfo, instantiateResourceConverterImpl, expectedDefaultValues);
    }

    @Test
    public void shouldFormatClusterConfig() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();

        assertClusterConfigFormatted(testResourceInfo, instantiateInfo.getClusterName(), instantiateResourceConverterImpl);
    }

    @Test
    public void shouldSetChartParamsWhenEmptyChartUrl() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();
        instantiateInfo.setChartUrl(null);

        Map<String, Object> actualMap = instantiateResourceConverterImpl.convert(testResourceInfo);

        assertNull(actualMap.get(CHART_URL));
        assertEquals(instantiateInfo.getChartName(), actualMap.get(CHART_NAME));
        assertEquals(instantiateInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldSetChartVersionForCRDChartsWithUrl() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();
        instantiateInfo.setChartType(InstantiateInfo.ChartTypeEnum.CRD);
        instantiateInfo.setChartName(null);

        Map<String, Object> actualMap = instantiateResourceConverterImpl.convert(testResourceInfo);
        assertEquals(instantiateInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldSetChartVersionForCRDChartsWithName() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();
        instantiateInfo.setChartType(InstantiateInfo.ChartTypeEnum.CRD);
        instantiateInfo.setChartUrl(null);

        Map<String, Object> actualMap = instantiateResourceConverterImpl.convert(testResourceInfo);
        assertEquals(instantiateInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldFormatHelmClientVersion() {
        InstantiateInfo instantiateInfo = testResourceInfo.getInfo();

        assertHelmClientVersionFormatted(testResourceInfo, instantiateInfo.getHelmClientVersion(), instantiateResourceConverterImpl);
    }
}