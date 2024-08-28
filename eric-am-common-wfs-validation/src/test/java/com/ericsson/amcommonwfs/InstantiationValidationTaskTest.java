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

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.amcommonwfs.UnitTestUtils.*;
import static com.ericsson.amcommonwfs.utils.constants.Constants.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import static org.assertj.core.api.Assertions.assertThat;

public class InstantiationValidationTaskTest {

    static final String NAMESPACE_VALUE = "ns";
    static final String APP_NAME_VALUE = "appName";
    static final String CHART_VERSION_VALUE = "ver: 2.0";
    static final String EMPTY_STRING = "";
    static final String RELEASE_NAME_VALUE = "app-name";
    static final String CHART_URL_VALUE = "https://arm.rnd.ki.sw.ericsson.se/artifactory" +
            "/proj-adp-notification-service-helm/eric-un-notification-service" +
            "/eric-un-notification-service-0.0.1-223.tgz";
    static final String INVALID_CHART_URL_VALUE = "hts:/arm.rnd.ki.sw.ericsson.se/artifactory" +
            "/proj-adp-notification-service-helm/eric-un-notification-service" +
            "/eric-un-notification-service-0.0.1-223.tgz";
    private static final List<String> MANDATORY_PARAMS_INSTANTIATE = unmodifiableList(
            new ArrayList<>(asList(NAMESPACE, CHART_NAME, CHART_VERSION)));
    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void successfulPropertyValidation() {
        setVariables(execution, NAMESPACE_VALUE, APP_NAME_VALUE, CHART_VERSION_VALUE);
        assertThat(ValidateParams.getMissingProperties(execution, MANDATORY_PARAMS_INSTANTIATE)).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void unsuccessfulPropertyValidation() {
        verifyExceptionThrownWhenMissingProperties(execution, NAMESPACE, EMPTY_STRING, APP_NAME_VALUE,
                                                   CHART_VERSION_VALUE, MANDATORY_PARAMS_INSTANTIATE);

        verifyExceptionThrownWhenMissingProperties(execution, CHART_NAME, NAMESPACE_VALUE, EMPTY_STRING,
                                                   CHART_VERSION_VALUE, MANDATORY_PARAMS_INSTANTIATE);

        verifyExceptionThrownWhenMissingProperties(execution, CHART_VERSION, NAMESPACE_VALUE, APP_NAME_VALUE, null,
                                                   MANDATORY_PARAMS_INSTANTIATE);

        verifyExceptionThrownWhenMissingProperties(execution, "namespace chartName chartVersion", null, EMPTY_STRING,
                                                   null, MANDATORY_PARAMS_INSTANTIATE);
    }

    @Test
    public void successfulReleaseNameValidation() {
        setReleaseName(execution, RELEASE_NAME_VALUE);
        ValidateReleaseName.checkReleaseName(execution);
    }

    @Test
    public void verifyExceptionThrownWhenReleaseNameSpecifiedContainsCapitals() {
        setReleaseName(execution, "App-name");
        expectedExceptionReleaseName(execution);
    }

    @Test
    public void verifyExceptionThrownWhenReleaseNameSpecifiedContainsUnderscore() {
        setReleaseName(execution, "app_name");
        expectedExceptionReleaseName(execution);
    }

    @Test
    public void verifyExceptionThrownWhenReleaseNameSpecifiedIsEmpty() {
        setReleaseName(execution, "");
        expectedExceptionReleaseName(execution);
    }

    @Test
    public void verifyExceptionThrownWhenReleaseNameDoesNotEndWithAlphanumericCharacter() {
        setReleaseName(execution, "app.");
        expectedExceptionReleaseName(execution);
    }

    @Test
    public void verifyExceptionThrownWhenConfigFileNotPresent() {
        execution.setVariable(ORIGINAL_CLUSTER_NAME, "test.config");
        expectedExceptionClusterConfigFileNotPresent(execution);
    }

    @Test
    public void successfulChartValidations() {
        setChartUrlRelatedVariables(execution, CHART_URL_VALUE, null, "");
        ValidateParams.validateChartParameters(execution);
        setChartUrlRelatedVariables(execution, "", APP_NAME_VALUE, "");
        ValidateParams.validateChartParameters(execution);
        setChartUrlRelatedVariables(execution, "", APP_NAME_VALUE, CHART_VERSION_VALUE);
        ValidateParams.validateChartParameters(execution);
    }

    @Test
    public void verifyExceptionThrownForInvalidChartUrlValue() {
        setChartUrlRelatedVariables(execution, INVALID_CHART_URL_VALUE, "", "");
        expectedExceptionInvalidChartUrl(execution, INVALID_CHART_URL_VALUE);
    }

    @Test
    public void verifyExceptionWhenChartValuesAreSpecifiedWithChartUrl() {
        setChartUrlRelatedVariables(execution, CHART_URL_VALUE, APP_NAME_VALUE, CHART_VERSION_VALUE);
        expectedExceptionTooManyChartValues(execution);
        setChartUrlRelatedVariables(execution, CHART_URL_VALUE, "", CHART_VERSION_VALUE);
        expectedExceptionTooManyChartValues(execution);
        setChartUrlRelatedVariables(execution, CHART_URL_VALUE, APP_NAME_VALUE, "");
        expectedExceptionTooManyChartValues(execution);
    }

    @Test
    public void verifyExceptionThrownWhenChartNameOrChartUrlNotSet() {
        setChartUrlRelatedVariables(execution, "", "", "");
        expectedExceptionChartValuesNotSet(execution);
        setChartUrlRelatedVariables(execution, "", "", CHART_VERSION_VALUE);
        expectedExceptionChartValuesNotSet(execution);
    }

    @Test
    public void checkReleaseName() {
        assertThat(ValidateReleaseName.isValidReleaseName((RELEASE_NAME_VALUE))).isTrue();
    }

    private void verifyExceptionThrownWhenMissingProperties(ExecutionImpl execution, final String expected,
                                                            final String namespace, final String chartName, final String chartVersion,
                                                            final List<String> mandatoryParams) {
        setVariables(execution, namespace, chartName, chartVersion);
        expectedException(execution, expected, mandatoryParams);
    }

    private void setVariables(ExecutionImpl execution, String namespaceValue, String chartNameValue,
                              String chartVersionValue) {
        execution.setVariableLocal(NAMESPACE, namespaceValue);
        execution.setVariableLocal(CHART_NAME, chartNameValue);
        execution.setVariableLocal(CHART_VERSION, chartVersionValue);
    }

    private void setChartUrlRelatedVariables(ExecutionImpl execution, String chartUrlValue, String chartName, String chartVersion) {
        execution.setVariableLocal(CHART_URL, chartUrlValue);
        execution.setVariableLocal(CHART_NAME, chartName);
        execution.setVariableLocal(CHART_VERSION, chartVersion);
    }
}
