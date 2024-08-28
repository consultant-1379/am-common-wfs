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
package com.ericsson.amcommonwfs.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import static com.ericsson.amcommonwfs.util.TestConstants.ADDITIONAL_PARAMS_WITH_HELM_KEYS;
import static com.ericsson.amcommonwfs.util.TestConstants.ADDITIONAL_VALUES_FILE_REDIS_KEY;
import static com.ericsson.amcommonwfs.util.TestConstants.EXECUTION_VARIABLES_NO_ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.util.TestConstants.EXECUTION_VARIABLES_NO_CHART_URL;
import static com.ericsson.amcommonwfs.util.TestConstants.EXECUTION_VARIABLES_WITH_CHART_URL;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_BASE_PARAMS;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_CHART_PARAMS_NO_CHART_URL;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_CHART_PARAMS_WITH_CHART_URL;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_TIMEOUT;
import static com.ericsson.amcommonwfs.util.TestConstants.VALUES_FILE_REDIS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CREATE_NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_NO_HOOKS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_WAIT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTALL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MAX_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.OVERRIDE_GLOBAL_REGISTRY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_UPGRADE_FAILED;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.model.CommandType;
import com.ericsson.amcommonwfs.util.Constant;
import com.ericsson.amcommonwfs.util.MapperUtils;

@SpringBootTest(classes = {
        UpgradeCommandParamsMapper.class
})
@TestPropertySource(properties = {
        "docker.registry.url=testRegistryUrl",
        "app.command.execute.defaultTimeOut=300",
        "containerRegistry.global.registry.pullSecret=regcred",
        "helm.debug.enabled=false",
        "applyDeprecatedDesignRules=true"
})
public class UpgradeCommandParamsMapperTest {
    @Spy
    private ExecutionImpl execution;

    @Autowired
    private UpgradeCommandParamsMapper upgradeCommandParamsMapper;

    @Test
    public void verifyInstallCommandTypeAndErrorCode() {
        assertThat(upgradeCommandParamsMapper.getType()).isEqualTo(CommandType.UPGRADE);
        assertThat(upgradeCommandParamsMapper.getErrorCode()).isEqualTo(BPMN_UPGRADE_FAILED);
    }

    @Test
    public void verifyUpgradeCommandParamsNoChartUrl() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_NO_CHART_URL, execution);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_NO_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(2L));
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat((String) actualCommandParams.get(VALUES_FILE_CONTENT_KEY)).isEqualTo(VALUES_FILE_REDIS_KEY);
        assertThat((String) actualCommandParams.get(ADDITIONAL_VALUES_FILE_CONTENT_KEY))
                .isEqualTo(ADDITIONAL_VALUES_FILE_REDIS_KEY);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED);
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(CREATE_NAMESPACE)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(15);
    }

    @Test
    public void verifyUpgradeCommandParamsNoAdditionalParams() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_NO_ADDITIONAL_PARAMS, execution);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(2L));
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).isNull();
        assertThat(actualCommandParams.size()).isEqualTo(11);
    }

    @Test
    public void verifyUpgradeCommandParamsWithChartUrl() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(2L));
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(CREATE_NAMESPACE)).isTrue();
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY);
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    public void verifyUpgradeCommandParamsWithOverrideGlobalRegistry() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(OVERRIDE_GLOBAL_REGISTRY, true);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(2L));
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME);
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(CREATE_NAMESPACE)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    @DirtiesContext
    public void verifyUpgradeCommandParamsWithApplyDeprecatedDesignRulesFalse() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(OVERRIDE_GLOBAL_REGISTRY, true);
        upgradeCommandParamsMapper.setIsApplyDeprecatedDesignRules(false);
        upgradeCommandParamsMapper.setHelmDebug(true);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE);
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE);
        assertThat((Boolean) actualCommandParams.get(Constant.HELM_DEBUG)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    public void verifyUpgradeCommandParamsWithHelmParamsSet() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(ADDITIONAL_PARAMS, ADDITIONAL_PARAMS_WITH_HELM_KEYS);
        Map<String, Object> actualCommandParams = upgradeCommandParamsMapper.apply(execution);
        assertThat((Boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isFalse();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(CREATE_NAMESPACE)).isTrue();
        assertThat((Boolean) actualCommandParams.get(HELM_NO_HOOKS_KEY)).isTrue();
        assertThat((Boolean) actualCommandParams.get(HELM_WAIT_KEY)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(14);
    }
}
