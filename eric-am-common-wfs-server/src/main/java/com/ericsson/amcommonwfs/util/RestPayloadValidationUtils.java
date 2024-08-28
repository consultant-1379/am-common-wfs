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

import static java.util.regex.Pattern.matches;

import static com.ericsson.amcommonwfs.util.Constants.CURRENTLY_SUPPORTED_IS_TEXT_FORMAT;
import static com.ericsson.amcommonwfs.util.Constants.UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME_REGEX;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.google.common.html.HtmlEscapers;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.ericsson.amcommonwfs.services.utils.CommonServicesUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.representer.Representer;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestPayloadValidationUtils {

    public static void validateFileName(final String configFileName, Pattern pattern, String nullOrEmptyErrorMessage,
                                        String patternErrorMessage) {
        if (Strings.isNullOrEmpty(configFileName)) {
            LOGGER.error(nullOrEmptyErrorMessage);
            throw new IllegalArgumentException(nullOrEmptyErrorMessage);
        }
        if (!pattern.matcher(configFileName).matches()) {
            LOGGER.error(patternErrorMessage);
            throw new IllegalArgumentException(patternErrorMessage);
        }
    }

    public static InputStream getInputStream(final MultipartFile packageContents) {
        InputStream clusterConfigContent;
        try {
            clusterConfigContent = packageContents.getInputStream();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return clusterConfigContent;
    }

    public static void validateFileTypeAsPlainText(final InputStream inputStream) {
        String contentType = getFileContentType(inputStream);
        if (!Objects.equals(contentType, MediaType.TEXT_PLAIN.toString())) {
            LOGGER.error("File content type was {}, was expected to be {}", contentType, MediaType.TEXT_PLAIN);
            throw new IllegalArgumentException(CURRENTLY_SUPPORTED_IS_TEXT_FORMAT);
        }
    }

    private static String getFileContentType(final InputStream inputStream) {
        try {
            return new Tika().detect(inputStream);
        } catch (IOException e) {
            LOGGER.error("It was not possible to determine the file type but will continue", e);
        }
        return "";
    }

    public static <T> T validateJson(String instantiateInfo, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            T parsedObject = mapper.readValue(instantiateInfo, valueType);
            final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            final Validator validator = factory.getValidator();
            Set<ConstraintViolation<T>> violations = validator.validate(parsedObject);
            if (violations != null && !violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return parsedObject;
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to parse request json because of %s", e.getMessage()), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static JSONObject validateYamlCanBeParsed(final InputStream inStr) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        final Yaml yaml = new Yaml(new SafeConstructor(loaderOptions),
                new Representer(new DumperOptions()),
                new DumperOptions(),
                loaderOptions);
        try {
            Object config = yaml.load(inStr);
            if (config == null) {
                throw new IllegalArgumentException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Empty file"));
            }
            if (!(config instanceof Map)) {
                throw new IllegalArgumentException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Invalid Yaml file provided"));
            }
            final Map<String, Object> map = (Map) config;
            if (map.get(null) != null) {
                String sanitizedValue = HtmlEscapers.htmlEscaper().escape(map.get(null).toString());
                throw new IllegalArgumentException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Invalid key for " + sanitizedValue));
            }
            return new JSONObject(map);
        } catch (YAMLException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, e.getMessage()),
                                               e);
        }
    }

    public static List<String> validateReleaseNames(String releaseNamesJson) {
        List<String> releaseNames = CommonServicesUtils.parseJsonToGenericType(releaseNamesJson, new TypeReference<>() { });

        List<String> validReleaseNames = new ArrayList<>();
        for (String releaseName : releaseNames) {
            if (!matches(RELEASE_NAME_REGEX, releaseName)) {
                LOGGER.error("Release name {} is invalid", releaseName);
            } else {
                validReleaseNames.add(releaseName);
            }
        }

        return validReleaseNames;
    }
}
