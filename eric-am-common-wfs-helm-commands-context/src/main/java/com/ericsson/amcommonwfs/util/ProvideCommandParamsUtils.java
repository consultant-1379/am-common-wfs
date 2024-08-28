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

import static com.ericsson.amcommonwfs.util.Constant.GLOBAL_PULL_SECRET;
import static com.ericsson.amcommonwfs.util.Constant.GLOBAL_REGISTRY_PULL_SECRET;
import static com.ericsson.amcommonwfs.util.Constant.GLOBAL_REGISTRY_URL;
import static com.ericsson.amcommonwfs.util.Constant.HELM_DEBUG;
import static com.ericsson.amcommonwfs.util.Constant.HELM_REPO;
import static com.ericsson.amcommonwfs.util.Constant.IMAGE_CREDENTIALS_PULL_SECRET;
import static com.ericsson.amcommonwfs.util.Constant.IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET;
import static com.ericsson.amcommonwfs.util.Constant.IMAGE_CREDENTIALS_REGISTRY_URL;
import static com.ericsson.amcommonwfs.util.Constant.TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_NO_HOOKS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_WAIT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ProvideCommandParamsUtils {

    private static final Set<String> BASE_PARAMS_KEYS = Set.of(RELEASE_NAME, NAMESPACE, CLUSTER_CONFIG_CONTENT_KEY);

    private static final Set<String> HELM_PARAMS_SET = Set.of(HELM_WAIT_KEY, DISABLE_OPENAPI_VALIDATION, HELM_NO_HOOKS_KEY);

    private static final Pattern SINGLE_DOUBLE_QUOTE_REGEX = Pattern.compile("[']");

    // Helm special characters needs to be escaped to override format and limitations of `helm --set`
    // https://helm.sh/docs/intro/using_helm/
    private static final Pattern HELM_SPECIAL_CHARACTERS = Pattern.compile("([{},])|(['])");

    public static void provideBaseParams(final DelegateExecution execution, final Map<String, Object> commandParams,
                                         final boolean helmDebug) {
        BASE_PARAMS_KEYS.forEach(key -> commandParams.put(key, execution.getVariable(key)));
        commandParams.put(HELM_DEBUG, helmDebug);
        commandParams.put(TIMEOUT, resolveTimeOut(execution));
    }

    public static void provideChartParams(final DelegateExecution execution, final Map<String, Object> commandParams) {
        final String chartURL = (String) execution.getVariable(CHART_URL);

        if (!Strings.isNullOrEmpty(chartURL)) {
            commandParams.put(CHART_URL, chartURL);
        } else {
            commandParams.put(CHART_NAME, execution.getVariable(CHART_NAME));
            commandParams.put(CHART_VERSION, execution.getVariable(CHART_VERSION));
        }
        commandParams.put(HELM_REPO, execution.getVariable(HELM_REPO));
    }

    public static void provideValuesFilesParams(final DelegateExecution execution, final Map<String, Object> commandParams) {
        if (!Strings.isNullOrEmpty((String) execution.getVariable(VALUES_FILE_CONTENT_KEY))) {
            commandParams.put(VALUES_FILE_CONTENT_KEY, execution.getVariable(VALUES_FILE_CONTENT_KEY));
        }

        if (!Strings.isNullOrEmpty((String) execution.getVariable(ADDITIONAL_VALUES_FILE_CONTENT_KEY))) {
            commandParams.put(ADDITIONAL_VALUES_FILE_CONTENT_KEY, execution.getVariable(ADDITIONAL_VALUES_FILE_CONTENT_KEY));
        }
    }

    public static void provideAdditionalParam(final String key, final String value,
                                              final Map<String, Object> commandParams,
                                              final List<String> setFlagValues) {
        if (HELM_PARAMS_SET.contains(key)) {
            commandParams.put(key, Boolean.parseBoolean(value));
        } else {
            provideSetFlagValueParam(setFlagValues, key, value);
        }
    }

    public static void overrideGlobalRegistry(final String imagePullRegistrySecretName,
                                              final String url, final boolean overrideGlobalRegistry,
                                              final DelegateExecution execution,
                                              final boolean isApplyDeprecatedDesignRules,
                                              final List<String> setFlagValues) {
        String chartURL = (String) execution.getVariable(CHART_URL);
        String releaseName = (String) execution.getVariable(RELEASE_NAME);
        if ((overrideGlobalRegistry || Strings.isNullOrEmpty(chartURL)) && !Strings.isNullOrEmpty(url)) {
            String uniqueImagePullSecretName = overrideGlobalRegistry ? releaseName : imagePullRegistrySecretName;
            provideSetFlagValueParam(setFlagValues, GLOBAL_REGISTRY_URL, url);
            provideSetFlagValueParam(setFlagValues, GLOBAL_PULL_SECRET, uniqueImagePullSecretName);
            if (isApplyDeprecatedDesignRules) {
                provideSetFlagValueParam(setFlagValues, IMAGE_CREDENTIALS_REGISTRY_URL, url);
                provideSetFlagValueParam(setFlagValues, GLOBAL_REGISTRY_PULL_SECRET, uniqueImagePullSecretName);
                provideSetFlagValueParam(setFlagValues, IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET,
                                         uniqueImagePullSecretName);

                provideSetFlagValueParam(setFlagValues, IMAGE_CREDENTIALS_PULL_SECRET,
                                         uniqueImagePullSecretName);
            }
        }
    }

    private static void provideSetFlagValueParam(final List<String> setValuesParams, final String key, final String value) {
        String convertedKey = removeUnwantedCharacters(key);
        String convertedValue = value != null ? escapeHelmSpecialCharacters(value) : value;
        String param = convertedKey + "=" + convertedValue;
        setValuesParams.add(param);
    }

    private static String removeUnwantedCharacters(final String value) {
        return SINGLE_DOUBLE_QUOTE_REGEX.matcher(value).replaceAll("");
    }

    private static String escapeHelmSpecialCharacters(final String value) {
        Matcher matcher = HELM_SPECIAL_CHARACTERS.matcher(value);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                matcher.appendReplacement(stringBuffer, "\\\\" + matcher.group(1));
            }
            if (matcher.group(2) != null) {
                matcher.appendReplacement(stringBuffer, "");
            }
        }
        return matcher.appendTail(stringBuffer).toString();
    }

    private static Long resolveTimeOut(DelegateExecution execution) {
        long appTimeout = (long) execution.getVariable(APP_TIMEOUT);
        Instant instant = Instant.ofEpochSecond(appTimeout);
        LocalDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime currentTime = LocalDateTime.now();
        long remainingAppTime = ChronoUnit.SECONDS.between(currentTime, zonedDateTime);
        if (remainingAppTime < 0) {
            LOGGER.info("Application timeout has elapsed setting remainingAppTime to 0");
            remainingAppTime = 0;
        }
        return remainingAppTime;
    }
}
