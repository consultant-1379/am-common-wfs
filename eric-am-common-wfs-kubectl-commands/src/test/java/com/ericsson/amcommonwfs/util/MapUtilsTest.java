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
package com.ericsson.amcommonwfs.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_VALUES_HEADER;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;

class MapUtilsTest {

    @Test
    public void shouldExtractMap_withSingleNestedLevel() {
        Map<String, Object> expectedSingleLevelNestedMap = generateSingleLevelMap();

        Map<String, Object> actualMap = MapUtils.extractAsFlatMap(expectedSingleLevelNestedMap);
        assertTrue(Maps.difference(expectedSingleLevelNestedMap, actualMap).areEqual());
    }

    @Test
    public void shouldExtractMap_withMultiNestedLevel() {
        Map<String, Object> multiLevelNestedMap = generateSingleLevelMap();
        multiLevelNestedMap.put("testKey3", generateSingleLevelMap());

        Map<String, Object> expectedMap = generateSingleLevelMap();
        expectedMap.put("testKey3.testKey1", "testValue1");
        expectedMap.put("testKey3.testKey2", "testValue2");

        Map<String, Object> actualMap = MapUtils.extractAsFlatMap(multiLevelNestedMap);
        assertTrue(Maps.difference(expectedMap, actualMap).areEqual());
    }

    @Test
    public void shouldExtractMap_withNullValue() {
        Map<String, Object> inputMap = generateSingleLevelMap();
        inputMap.put("testKey3", null);
        Map<String, Object> expectedMap = generateSingleLevelMap();
        inputMap.put("testKey3", "");

        Map<String, Object> actualMap = MapUtils.extractAsFlatMap(inputMap);
        assertTrue(Maps.difference(inputMap, actualMap).areEqual());
    }

    @Test
    public void shouldExtractMap_withNullKey() {
        Map<String, Object> expectedMap = generateSingleLevelMap();
        expectedMap.put(null, "testValue3");

        Map<String, Object> actualMap = MapUtils.extractAsFlatMap(expectedMap);
        assertTrue(Maps.difference(expectedMap, actualMap).areEqual());
    }

    @Test
    public void shouldSuccessConvertToYaml() {
        String dummyYamlFile = "resources:\n" +
                "  crdjob:\n" +
                "    requests:\n" +
                "      memory: \"50Mi\"\n" +
                "      cpu: \"50m\"";

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("resources.crdjob.requests.memory", "50Mi");
        expectedMap.put("resources.crdjob.requests.cpu", "50m");

        Map<String, Object> actualMap = MapUtils.convertYamlToMap(dummyYamlFile);
        assertTrue(Maps.difference(expectedMap, actualMap).areEqual());
    }

    @Test
    public void shouldSuccessConvertToYamlWithHeader() {
        String dummyYamlFile = "resources:\n" +
                "  crdjob:\n" +
                "    requests:\n" +
                "      memory: \"50Mi\"\n" +
                HELM_VALUES_HEADER + "\n" +
                "  name: eric-sec-certm-crd\n" +
                "  tag: 2.8.0-72";

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("name", "eric-sec-certm-crd");
        expectedMap.put("tag", "2.8.0-72");

        Map<String, Object> actualMap = MapUtils.convertYamlToMap(dummyYamlFile);
        assertTrue(Maps.difference(expectedMap, actualMap).areEqual());
    }

    @Test
    public void shouldSuccessConvertToYamlWithoutValues() {
        String dummyYamlFile = "resources:\n" +
                "  crdjob:\n" +
                "    requests:\n" +
                "      memory: \"50Mi\"\n" +
                HELM_VALUES_HEADER;

        Map<String, Object> expectedMap = new HashMap<>();

        Map<String, Object> actualMap = MapUtils.convertYamlToMap(dummyYamlFile);
        assertTrue(Maps.difference(expectedMap, actualMap).areEqual());
    }

    private Map<String, Object> generateSingleLevelMap() {
        Map<String, Object> singleLevelNestedMap = new LinkedHashMap<>();
        singleLevelNestedMap.put("testKey1", "testValue1");
        singleLevelNestedMap.put("testKey2", "testValue2");
        return singleLevelNestedMap;
    }
}