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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;

public class ScaleResourceConverterImplTest extends AbstractResourceConverterTest<ResourceInfoDto<ScaleInfo>> {

    private static final int SCALE_MAP_LENGTH = 3;

    private final ScaleResourceConverterImpl scaleResourceConverterImpl =
            new ScaleResourceConverterImpl(CLUSTER_CONFIG_DIR, DEFAULT_COMMAND_TIME_OUT);

    private ResourceInfoDto<ScaleInfo> testResourceInfo;

    @BeforeEach
    public void setUp() {
        testResourceInfo = testResourceDtoFactory.createScaleInfo();
    }

    @Test
    public void shouldNotSetEmptyValues() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();
        testScaleInfo.setClusterName(null);

        assertEmptyValuesNotIncluded(testResourceInfo, scaleResourceConverterImpl,
                Lists.newArrayList(CLUSTER_NAME));
    }

    @Test
    public void shouldSetDefaultParameters() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();

        testScaleInfo.setHelmClientVersion(null);

        Map<String, Object> expectedDefaultValues = new HashMap<>();
        expectedDefaultValues.put(SKIP_VERIFICATION, true);
        expectedDefaultValues.put(HELM_CLIENT_VERSION, "helm");

        assertDefaultValuesPresent(testResourceInfo, scaleResourceConverterImpl, expectedDefaultValues);
    }

    @Test
    public void shouldFormatClusterConfig() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();

        assertClusterConfigFormatted(testResourceInfo, testScaleInfo.getClusterName(), scaleResourceConverterImpl);
    }

    @Test

    public void shouldReplaceAdditionalParamsByScaleParamsWhenNullAdditionalParams() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();
        Map<String, Map<String, Integer>> expectedScaleResources = createScaleResources();
        testScaleInfo.setAdditionalParams(null);
        testScaleInfo.setScaleResources(new HashMap<>(expectedScaleResources));

        Map<String, Object> actualMap = scaleResourceConverterImpl.convert(testResourceInfo);

        assertAdditionalParamsMatch(expectedScaleResources, Collections.emptyMap(),
                extractAdditionalParameters(actualMap));
    }

    @Test
    public void shouldAddScaleParamsToAdditionalParams() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();
        Map<String, Map<String, Integer>> expectedScaleResources = createScaleResources();
        Map<String, String> expectedAdditionalParams = new HashMap<>(testScaleInfo.getAdditionalParams());
        testScaleInfo.setScaleResources(new HashMap<>(expectedScaleResources));

        Map<String, Object> actualMap = scaleResourceConverterImpl.convert(testResourceInfo);

        assertAdditionalParamsMatch(expectedScaleResources, expectedAdditionalParams,
                extractAdditionalParameters(actualMap));
    }

    @Test
    public void shouldFormatHelmClientVersion() {
        ScaleInfo testScaleInfo = testResourceInfo.getInfo();

        assertHelmClientVersionFormatted(testResourceInfo, testScaleInfo.getHelmClientVersion(), scaleResourceConverterImpl);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractAdditionalParameters(Map<String, Object> actualMap) {
        return (Map<String, String>) actualMap.get(ADDITIONAL_PARAMS);
    }

    private void assertAdditionalParamsMatch(Map<String, Map<String, Integer>> expectedScaleResources,
                                             Map<String, String> expectedAdditionalParams,
                                             Map<String, String> actualAdditionalParams) {
        int expectedAdditionalParamsSize = expectedAdditionalParams.size() +
                expectedScaleResources.values().stream()
                        .mapToInt(Map::size).sum();

        assertEquals(expectedAdditionalParamsSize, actualAdditionalParams.size());

        for (Map.Entry<String, String> expectedEntry : expectedAdditionalParams.entrySet()) {
            assertThat(actualAdditionalParams, hasEntry(expectedEntry.getKey(), expectedEntry.getValue()));
        }

        for (Map<String, Integer> expectedEntryMap : expectedScaleResources.values()) {
            for (Map.Entry<String, Integer> expectedEntry : expectedEntryMap.entrySet()) {
                Integer value = expectedEntry.getValue();
                assertThat(actualAdditionalParams, hasEntry(expectedEntry.getKey(), String.valueOf(value)));
            }
        }
    }

    private Map<String, Map<String, Integer>> createScaleResources() {
        Map<String, Map<String, Integer>> scaleResources = new HashMap<>();

        for (int i = 0; i < SCALE_MAP_LENGTH; i++) {
            scaleResources.put(RandomStringUtils.randomAlphabetic(3), createIntegerMap());
        }
        return scaleResources;
    }

    private Map<String, Integer> createIntegerMap() {
        Map<String, Integer> integerMap = new HashMap<>();

        for (int i = 0; i < SCALE_MAP_LENGTH; i++) {
            integerMap.put(RandomStringUtils.randomAlphabetic(3), RandomUtils.nextInt());
        }
        return integerMap;
    }

}