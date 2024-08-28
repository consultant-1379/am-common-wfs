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
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;

public class UpgradeResourceConverterImplTest extends AbstractResourceConverterTest<ResourceInfoDto<UpgradeInfo>> {

    private final UpgradeResourceConverterImpl upgradeResourceConverterImpl =
            new UpgradeResourceConverterImpl(CLUSTER_CONFIG_DIR, DEFAULT_COMMAND_TIME_OUT);

    private ResourceInfoDto<UpgradeInfo> testResourceInfo;

    @BeforeEach
    public void setUp() {
        testResourceInfo = testResourceDtoFactory.createTestUpgradeInfo();
    }

    @Test
    public void shouldNotSetEmptyValues() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();
        upgradeInfo.setAdditionalParams(null);
        upgradeInfo.setClusterName(null);

        assertEmptyValuesNotIncluded(testResourceInfo, upgradeResourceConverterImpl,
                Lists.newArrayList(ADDITIONAL_PARAMS, CLUSTER_NAME));
    }

    @Test
    public void shouldSetDefaultParameters() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();

        upgradeInfo.setSkipVerification(null);
        upgradeInfo.setSkipJobVerification(null);
        upgradeInfo.setHelmClientVersion(null);

        Map<String, Object> expectedDefaultValues = new HashMap<>();
        expectedDefaultValues.put(SKIP_VERIFICATION, false);
        expectedDefaultValues.put(SKIP_JOB_VERIFICATION, false);
        expectedDefaultValues.put(HELM_CLIENT_VERSION, "helm");

        assertDefaultValuesPresent(testResourceInfo, upgradeResourceConverterImpl, expectedDefaultValues);
    }

    @Test
    public void shouldFormatClusterConfig() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();
        assertClusterConfigFormatted(testResourceInfo, upgradeInfo.getClusterName(), upgradeResourceConverterImpl);
    }

    @Test
    public void shouldSetChartParamsWhenEmptyChartUrl() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();
        upgradeInfo.setChartUrl(null);

        Map<String, Object> actualMap = upgradeResourceConverterImpl.convert(testResourceInfo);

        assertNull(actualMap.get(CHART_URL));
        assertEquals(upgradeInfo.getChartName(), actualMap.get(CHART_NAME));
        assertEquals(upgradeInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldSetChartVersionForCRDChartsWithUrl() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();
        upgradeInfo.setChartType(UpgradeInfo.ChartTypeEnum.CRD);
        upgradeInfo.setChartName(null);

        Map<String, Object> actualMap = upgradeResourceConverterImpl.convert(testResourceInfo);
        assertEquals(upgradeInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldSetChartVersionForCRDChartsWithName() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();
        upgradeInfo.setChartType(UpgradeInfo.ChartTypeEnum.CRD);
        upgradeInfo.setChartUrl(null);

        Map<String, Object> actualMap = upgradeResourceConverterImpl.convert(testResourceInfo);
        assertEquals(upgradeInfo.getChartVersion(), actualMap.get(CHART_VERSION));
    }

    @Test
    public void shouldFormatHelmClientVersion() {
        UpgradeInfo upgradeInfo = testResourceInfo.getInfo();

        assertHelmClientVersionFormatted(testResourceInfo, upgradeInfo.getHelmClientVersion(), upgradeResourceConverterImpl);
    }
}