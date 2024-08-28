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
package com.ericsson.amcommonwfs;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;

public class ValidateParamsTest {

    private static final List<String> MANDATORY_PARAMS_LIST = List.of(RELEASE_NAME, REVISION_NUMBER);
    private static final String VALID_RELEASE_NAME = "somename-123";
    private static final String VALID_REVISION_NUMBER = "123123";

    private static final String VALID_CHART_URL = "https://charts.helm.sh/stable/mysql.tgz";
    private static final String INVALID_CHART_URL = "helm.sh/mysql.tgz";
    private static final String VALID_CHART_NAME = "somevalidname";
    private static final String VALID_CHART_VERSION = "112233";
    private static final Map<String, String> VALID_ADDITIONAL_PARAMS = Map.of("param1", "value1", "param2", "value2");

    private static final List<String> INVALID_ADDITIONAL_PARAMS = List.of("param1", "param2");
    private final ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testSuccessfullValidateMandatoryParameters() {
        execution.setVariableLocal(RELEASE_NAME, VALID_RELEASE_NAME);
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);

        assertThatNoException()
                .isThrownBy(() -> ValidateParams.validateMandatoryParameters(
                        execution, MANDATORY_PARAMS_LIST));
    }

    @Test
    public void testNegativeValidateMandatoryParameters() {
        final ExecutionImpl execution = new ExecutionImpl();
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams.validateMandatoryParameters(
                        execution, MANDATORY_PARAMS_LIST))
                .withMessage("Required properties are missing/null - provide valid input for the following: releaseName");
    }

    @Test
    public void testGetMissingProperties() {
        execution.setVariableLocal(RELEASE_NAME, VALID_RELEASE_NAME);
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);

        assertTrue(ValidateParams
                           .getMissingProperties
                                   (execution, MANDATORY_PARAMS_LIST).isEmpty());
    }

    @Test
    public void testNegativeGetMissingProperties() {
        final ExecutionImpl execution = new ExecutionImpl();
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);

        String missingReleaseName =
                ValidateParams.getMissingProperties(execution, MANDATORY_PARAMS_LIST);
        assertEquals("releaseName", missingReleaseName);
    }

    @Test
    public void testSuccessVerifyAdditionalParamsIsMap() {
        execution.setVariableLocal(RELEASE_NAME, VALID_RELEASE_NAME);
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);
        execution.setVariableLocal(ADDITIONAL_PARAMS, VALID_ADDITIONAL_PARAMS);

        assertThatNoException().isThrownBy(() -> ValidateParams.verifyAdditionalParamsIsMap(execution));
    }

    @Test
    public void testNegativeVerifyAdditionalParamsIsMap() {
        execution.setVariableLocal(RELEASE_NAME, VALID_RELEASE_NAME);
        execution.setVariableLocal(REVISION_NUMBER, VALID_REVISION_NUMBER);
        execution.setVariableLocal(ADDITIONAL_PARAMS, INVALID_ADDITIONAL_PARAMS);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams.verifyAdditionalParamsIsMap(execution))
                .withMessage("AdditionalParams need to be in MAP format. Please provide valid input");
    }

    @Test
    public void testSuccessValidateChartParameters() {
        execution.setVariableLocal(CHART_URL, VALID_CHART_URL);

        assertThatNoException()
                .isThrownBy(() -> ValidateParams
                        .validateChartParameters(execution));
    }

    @Test
    public void testExtraParameterChartNameValidateChartParameters() {
        execution.setVariableLocal(CHART_NAME, VALID_CHART_NAME);
        execution.setVariableLocal(CHART_URL, VALID_CHART_URL);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams
                        .validateChartParameters(execution))
                .withMessage("chartUrl property has been specified, "
                                     + "chartName or chartVersion properties should not be set. Please see API "
                                     + "documentation for correct usage.")
                .withNoCause();
    }

    @Test
    public void testExtraParameterChartVersionValidateChartParameters() {
        execution.setVariableLocal(CHART_NAME, VALID_CHART_NAME);
        execution.setVariableLocal(CHART_VERSION, VALID_CHART_VERSION);
        execution.setVariableLocal(CHART_URL, VALID_CHART_URL);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams
                        .validateChartParameters(execution))
                .withMessage("chartUrl property has been specified, "
                                     + "chartName or chartVersion properties should not be set. Please see API "
                                     + "documentation for correct usage.")
                .withNoCause();
    }

    @Test
    public void testExtraParametersValidateChartParameters() {
        execution.setVariableLocal(CHART_VERSION, VALID_CHART_VERSION);
        execution.setVariableLocal(CHART_URL, VALID_CHART_URL);

        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams
                        .validateChartParameters(execution))
                .withMessage("chartUrl property has been specified, "
                                     + "chartName or chartVersion properties should not be set. Please see API "
                                     + "documentation for correct usage.")
                .withNoCause();
    }

    @Test
    public void testPositiveValidateUrl() {
        assertThatNoException()
                .isThrownBy(() -> ValidateParams.validateUrl(execution, VALID_CHART_URL));
    }

    @Test
    public void testNegativeValidateUrl() {
        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams.validateUrl(execution, INVALID_CHART_URL))
                .withMessage("chartUrl property validation failed, please provide a valid URL : helm.sh/mysql.tgz")
                .withNoCause();
    }

    @Test
    public void testVerifyClusterConfigPresent() {
        execution.setVariableLocal(ORIGINAL_CLUSTER_NAME, "cluster.config");
        assertThatExceptionOfType(BpmnError.class)
                .isThrownBy(() -> ValidateParams.verifyClusterConfigPresent(execution))
                .withMessage("cluster config not present, please add the config file using "
                                     + "'add cluster config rest api' and then use this parameter");
    }
}