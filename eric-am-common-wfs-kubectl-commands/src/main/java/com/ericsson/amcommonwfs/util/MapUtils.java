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

import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_VALUES_HEADER;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlProcessor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapUtils {

    private static final YamlMapFlatteningProcessor YAML_MAP_FLATTENING_PROCESSOR = new YamlMapFlatteningProcessor();

    public static Map<String, Object> extractAsFlatMap(Map<String, Object> multiLevelMap) {
        return YAML_MAP_FLATTENING_PROCESSOR.flattenMap(multiLevelMap);
    }

    public static Map<String, Object> convertYamlToMap(final String yamlFile) {
        final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        String formattedValues = yamlFile.contains(HELM_VALUES_HEADER) ?
                yamlFile.substring(yamlFile.indexOf(HELM_VALUES_HEADER) + HELM_VALUES_HEADER.length()) : yamlFile;
        Map<String, Object> config = new HashMap<>();
        if (StringUtils.isNotEmpty(formattedValues)) {
            config = yaml.load(formattedValues);
        }
        return MapUtils.extractAsFlatMap(config);
    }

    private static class YamlMapFlatteningProcessor extends YamlProcessor {
        public Map<String, Object> flattenMap(Map<String, Object> multiLevelMap) {
            return super.getFlattenedMap(multiLevelMap);
        }
    }
}