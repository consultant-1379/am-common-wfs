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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DEFAULT_NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_NO_HOOKS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_WAIT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ProvideCommandParamsUtilsTest {

    @Spy
    private ExecutionImpl execution;

    private static final HashMap<String, Object> commandParams = new HashMap<>();

    private static final ArrayList<String> setFlagValues = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        commandParams.clear();
        setFlagValues.clear();
    }

    @Test
    public void testProvideBaseParamsWithHelmDebugFalse() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(300);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariableLocal(APP_TIMEOUT, toEpochSecond);
        execution.setVariableLocal(NAMESPACE, DEFAULT_NAMESPACE);
        execution.setVariableLocal(RELEASE_NAME, TestConstants.MYSQL_RELEASE_NAME);
        execution.setVariableLocal(CLUSTER_CONFIG_CONTENT_KEY, TestConstants.CLUSTER_CONFIG_FILE_REDIS_KEY);

        ProvideCommandParamsUtils.provideBaseParams(execution, commandParams, false);
        assertThat(commandParams).containsAllEntriesOf(TestConstants.EXPECTED_BASE_PARAMS);
        assertThat(commandParams.containsKey(Constant.TIMEOUT)).isTrue();
    }

    @Test
    public void testProvideBaseParamsWithHelmDebugTrue() {
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(300);
        long toEpochSecond = timeout.toEpochSecond(ZoneOffset.UTC);
        execution.setVariableLocal(APP_TIMEOUT, toEpochSecond);
        execution.setVariableLocal(NAMESPACE, DEFAULT_NAMESPACE);
        execution.setVariableLocal(RELEASE_NAME, TestConstants.MYSQL_RELEASE_NAME);
        execution.setVariableLocal(CLUSTER_CONFIG_CONTENT_KEY, TestConstants.CLUSTER_CONFIG_FILE_REDIS_KEY);

        ProvideCommandParamsUtils.provideBaseParams(execution, commandParams, true);
        assertThat(commandParams).containsAllEntriesOf(TestConstants.EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE);
        assertThat(commandParams.containsKey(Constant.TIMEOUT)).isTrue();
    }

    @Test
    public void testProvideChartParamsNoChartUrl() {
        execution.setVariableLocal(CHART_NAME, TestConstants.MYSQL_CHART_NAME);
        execution.setVariableLocal(CHART_VERSION, TestConstants.MYSQL_CHART_VERSION);
        execution.setVariableLocal(Constant.HELM_REPO, TestConstants.HELM_REPO_ROOT);

        ProvideCommandParamsUtils.provideChartParams(execution, commandParams);
        assertThat(commandParams).containsAllEntriesOf(TestConstants.EXPECTED_CHART_PARAMS_NO_CHART_URL);
    }

    @Test
    public void testProvideChartParamsWithChartUrl() {
        execution.setVariableLocal(CHART_NAME, TestConstants.MYSQL_CHART_NAME);
        execution.setVariableLocal(CHART_VERSION, TestConstants.MYSQL_CHART_VERSION);
        execution.setVariableLocal(Constant.HELM_REPO, TestConstants.HELM_REPO_ROOT);
        execution.setVariableLocal(CHART_URL, TestConstants.MYSQL_CHART_URL);

        ProvideCommandParamsUtils.provideChartParams(execution, commandParams);
        assertThat(commandParams).containsAllEntriesOf(TestConstants.EXPECTED_CHART_PARAMS_WITH_CHART_URL);
    }

    @Test
    public void testProvideValuesFilesParams() {
        execution.setVariableLocal(VALUES_FILE_CONTENT_KEY, TestConstants.VALUES_FILE_REDIS_KEY);

        ProvideCommandParamsUtils.provideValuesFilesParams(execution, commandParams);
        assertThat(commandParams.get(VALUES_FILE_CONTENT_KEY)).isEqualTo(TestConstants.VALUES_FILE_REDIS_KEY);
        assertThat(commandParams.get(ADDITIONAL_VALUES_FILE_CONTENT_KEY)).isNull();

        execution.setVariableLocal(ADDITIONAL_VALUES_FILE_CONTENT_KEY, TestConstants.ADDITIONAL_VALUES_FILE_REDIS_KEY);

        ProvideCommandParamsUtils.provideValuesFilesParams(execution, commandParams);
        assertThat(commandParams.get(ADDITIONAL_VALUES_FILE_CONTENT_KEY)).isEqualTo(TestConstants.ADDITIONAL_VALUES_FILE_REDIS_KEY);
    }

    @Test
    public void testProvideAdditionalParamNoHelmKeys() {
        execution.setVariableLocal(ADDITIONAL_PARAMS, TestConstants.ADDITIONAL_PARAMS_NO_HELM_KEYS);
        TestConstants.ADDITIONAL_PARAMS_NO_HELM_KEYS.forEach(
                (key, value) -> ProvideCommandParamsUtils.provideAdditionalParam(key, value, commandParams, setFlagValues));

        assertThat(setFlagValues).hasSameElementsAs(TestConstants.EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY);
        assertThat(commandParams.isEmpty()).isTrue();
    }

    @Test
    public void testProvideAdditionalParamWithHelmKeys() {
        execution.setVariableLocal(ADDITIONAL_PARAMS, TestConstants.ADDITIONAL_PARAMS_WITH_HELM_KEYS);
        TestConstants.ADDITIONAL_PARAMS_WITH_HELM_KEYS.forEach(
                (key, value) -> ProvideCommandParamsUtils.provideAdditionalParam(key, value, commandParams, setFlagValues));

        assertThat(setFlagValues).hasSameElementsAs(TestConstants.EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY);
        assertThat((Boolean) commandParams.get(DISABLE_OPENAPI_VALIDATION)).isFalse();
        assertThat((Boolean) commandParams.get(HELM_NO_HOOKS_KEY)).isTrue();
        assertThat((Boolean) commandParams.get(HELM_WAIT_KEY)).isTrue();
    }

    @Test
    public void testOverrideGlobalRegistryWithOverrideGlobalRegistryFalseNoChartUrl() {
        execution.setVariableLocal(RELEASE_NAME, TestConstants.MYSQL_RELEASE_NAME);
        ProvideCommandParamsUtils.overrideGlobalRegistry(TestConstants.REGISTRY_PULL_SECRET, TestConstants.REGISTRY_URL,
                                                         false, execution, true, setFlagValues);

        assertThat(setFlagValues).hasSameElementsAs(TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED_NO_ADDITIONAL_PARAMS);
    }

    @Test
    public void testOverrideGlobalRegistryWithOverrideGlobalRegistryTrue() {
        execution.setVariableLocal(RELEASE_NAME, TestConstants.MYSQL_RELEASE_NAME);
        ProvideCommandParamsUtils.overrideGlobalRegistry(TestConstants.REGISTRY_PULL_SECRET, TestConstants.REGISTRY_URL,
                                                         true, execution, true, setFlagValues);

        assertThat(setFlagValues).hasSameElementsAs(TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME_NO_ADDITIONAL_PARAMS);
    }

    @Test
    public void testOverrideGlobalRegistryWithApplyDeprecatedDRFalse() {
        execution.setVariableLocal(RELEASE_NAME, TestConstants.MYSQL_RELEASE_NAME);
        ProvideCommandParamsUtils.overrideGlobalRegistry(TestConstants.REGISTRY_PULL_SECRET, TestConstants.REGISTRY_URL,
                                                         true, execution, false, setFlagValues);

        assertThat(setFlagValues).hasSameElementsAs(TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE_NO_ADDITIONAL_PARAMS);
    }
}