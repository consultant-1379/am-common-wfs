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
import static com.ericsson.amcommonwfs.utils.constants.Constants.ATOMIC;
import static com.ericsson.amcommonwfs.utils.constants.Constants.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_NO_HOOKS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_WAIT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTALL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MAX_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.OVERRIDE_GLOBAL_REGISTRY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_CRD_FAILED;

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
        CRDCommandParamsMapper.class
})
@TestPropertySource(properties = {
        "docker.registry.url=testRegistryUrl",
        "app.command.execute.defaultTimeOut=300",
        "containerRegistry.global.registry.pullSecret=regcred",
        "helm.debug.enabled=false",
        "applyDeprecatedDesignRules=true"
})
public class CRDCommandParamsMapperTest {
    @Spy
    private ExecutionImpl execution;

    @Autowired
    private CRDCommandParamsMapper crdCommandParamsMapper;

    @Test
    public void verifyInstallCommandTypeAndErrorCode() {
        assertThat(crdCommandParamsMapper.getType()).isEqualTo(CommandType.CRD);
        assertThat(crdCommandParamsMapper.getErrorCode()).isEqualTo(BPMN_CRD_FAILED);
    }

    @Test
    public void verifyCRDCommandParamsNoChartUrl() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_NO_CHART_URL, execution);

        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_NO_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(1L));
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat((String) actualCommandParams.get(VALUES_FILE_CONTENT_KEY)).isEqualTo(VALUES_FILE_REDIS_KEY);
        assertThat((String) actualCommandParams.get(ADDITIONAL_VALUES_FILE_CONTENT_KEY))
                .isEqualTo(ADDITIONAL_VALUES_FILE_REDIS_KEY);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_REGCRED);
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(ATOMIC)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(15);
    }

    @Test
    public void verifyCRDCommandParamsNoAdditionalParams() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_NO_ADDITIONAL_PARAMS, execution);

        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(1L));
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(11);
    }

    @Test
    public void verifyCRDCommandParamsWithChartUrl() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);

        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(1L));
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(ATOMIC)).isTrue();
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_ADDITIONAL_PARAMS_ONLY);
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    public void verifyCRDCommandParamsWithOverrideGlobalRegistry() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(OVERRIDE_GLOBAL_REGISTRY, true);
        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_CHART_PARAMS_WITH_CHART_URL);
        assertThat((long) actualCommandParams.get(Constant.TIMEOUT)).isCloseTo(EXPECTED_TIMEOUT, within(2L));
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_RELEASE_NAME);
        assertThat((boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isTrue();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(ATOMIC)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    @DirtiesContext
    public void verifyCRDCommandParamsWithApplyDeprecatedDesignRulesFalse() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(OVERRIDE_GLOBAL_REGISTRY, true);
        crdCommandParamsMapper.setIsApplyDeprecatedDesignRules(false);
        crdCommandParamsMapper.setHelmDebug(true);
        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE);
        List<String> actualSetFlagValues = (List<String>) actualCommandParams.get(Constant.SET_FLAG_VALUES);
        assertThat(actualSetFlagValues).hasSameElementsAs(EXPECTED_SET_FLAG_VALUES_PARAMS_APPLY_DEPRECATED_DR_FALSE);
        assertThat((Boolean) actualCommandParams.get(Constant.HELM_DEBUG)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(12);
    }

    @Test
    public void verifyCRDCommandParamsWithHelmParamsSet() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_WITH_CHART_URL, execution);
        execution.setVariableLocal(ADDITIONAL_PARAMS, ADDITIONAL_PARAMS_WITH_HELM_KEYS);
        Map<String, Object> actualCommandParams = crdCommandParamsMapper.apply(execution);
        assertThat((Boolean) actualCommandParams.get(DISABLE_OPENAPI_VALIDATION)).isFalse();
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((boolean) actualCommandParams.get(INSTALL)).isTrue();
        assertThat((boolean) actualCommandParams.get(ATOMIC)).isTrue();
        assertThat((Boolean) actualCommandParams.get(HELM_NO_HOOKS_KEY)).isTrue();
        assertThat((Boolean) actualCommandParams.get(HELM_WAIT_KEY)).isTrue();
        assertThat(actualCommandParams.size()).isEqualTo(14);
    }
}
