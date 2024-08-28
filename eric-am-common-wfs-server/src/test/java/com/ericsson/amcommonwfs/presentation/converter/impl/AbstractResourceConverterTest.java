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

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.ericsson.amcommonwfs.presentation.converter.Converter;
import com.ericsson.infrastructure.factory.TestResourceDtoFactory;


public abstract class AbstractResourceConverterTest<T> {

    private static final String CONFIG_FILE_EXTENSION = ".config";

    final static String CLUSTER_CONFIG_DIR = "/mnt/cluster_config";

    final static String DEFAULT_COMMAND_TIME_OUT = "300";

    TestResourceDtoFactory testResourceDtoFactory = new TestResourceDtoFactory();

    protected void assertEmptyValuesNotIncluded(T emptyValuesInfo, Converter<Map<String, Object>, T> testConverter,
                                                List<String> expectedNotIncludedKeys) {
        Map<String, Object> actualConvertedMap = testConverter.convert(emptyValuesInfo);

        for (String expectedNotIncludedKey : expectedNotIncludedKeys) {
            assertNull(actualConvertedMap.get(expectedNotIncludedKey));
        }
    }

    protected void assertDefaultValuesPresent(T emptyDefaultValuesInfo, Converter<Map<String, Object>, T> testConverter,
                                              Map<String, Object> expectedDefaultValues) {
        Map<String, Object> actualConvertedMap = testConverter.convert(emptyDefaultValuesInfo);

        for (Map.Entry<String, Object> expectedDefaultEntry : expectedDefaultValues.entrySet()) {
            assertEquals(expectedDefaultEntry.getValue(), actualConvertedMap.get(expectedDefaultEntry.getKey()));
        }
    }

    protected void assertClusterConfigFormatted(T testInfo, String clusterName, Converter<Map<String, Object>, T> testConverter) {
        String expectedClusterName = CLUSTER_CONFIG_DIR + File.separator +
                clusterName + CONFIG_FILE_EXTENSION;

        Map<String, Object> actualConvertedMap = testConverter.convert(testInfo);

        assertEquals(expectedClusterName, actualConvertedMap.get(CLUSTER_NAME));
    }

    protected void assertHelmClientVersionFormatted(T testInfo, String helmClientVersion, Converter<Map<String, Object>, T> testConverter) {
        String expectedHelmClientVersion = "helm-" + helmClientVersion;

        Map<String, Object> actualConvertedMap = testConverter.convert(testInfo);

        assertEquals(expectedHelmClientVersion, actualConvertedMap.get(HELM_CLIENT_VERSION));
    }
}
