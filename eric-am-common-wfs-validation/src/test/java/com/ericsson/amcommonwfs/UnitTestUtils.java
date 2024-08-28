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

import static com.ericsson.amcommonwfs.TaskConstants.*;
import static com.ericsson.amcommonwfs.utils.constants.Constants.*;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;

public final class UnitTestUtils {

    private UnitTestUtils() {
    }

    public static void setReleaseName(ExecutionImpl execution, String releaseName) {
        execution.setVariableLocal(RELEASE_NAME, releaseName);
    }

    public static void expectedException(ExecutionImpl execution, String expectedMessage,
            List<String> mandatoryParams) {
        Assertions.assertThatThrownBy(() -> ValidateParams.validateMandatoryParameters(execution, mandatoryParams))
                .isInstanceOf(BpmnError.class).hasMessage(REQUIRED_PROPERTIES_ERROR_MSG + expectedMessage);
    }

    public static void expectedExceptionReleaseName(ExecutionImpl execution) {
        Assertions.assertThatThrownBy(() -> ValidateReleaseName.checkReleaseName(execution))
                .isInstanceOf(BpmnError.class).hasMessage(RELEASE_NAME_ERROR_MSG);
    }

    public static void expectedExceptionInvalidChartUrl(ExecutionImpl execution, String chartUrl) {
        Assertions.assertThatThrownBy(() -> ValidateParams.validateUrl(execution, chartUrl))
                .isInstanceOf(BpmnError.class).hasMessageContaining(MALFORMED_URL_ERROR_MSG);
    }

    public static void expectedExceptionTooManyChartValues(ExecutionImpl execution) {
        Assertions.assertThatThrownBy(() -> ValidateParams.validateChartParameters(execution))
                .isInstanceOf(BpmnError.class).hasMessage(String.format(ADDITIONAL_CHART_VALUES_ERROR_MSG, CHART_NAME, CHART_VERSION));
    }

    public static void expectedExceptionChartValuesNotSet(ExecutionImpl execution) {
        Assertions.assertThatThrownBy(() -> ValidateParams.validateChartParameters(execution))
                .isInstanceOf(BpmnError.class).hasMessage(String.format(REQUIRED_PROPERTIES_ERROR_MSG + "%s or %s ", CHART_NAME, CHART_URL));
    }

    public static void expectedExceptionClusterConfigFileNotPresent(ExecutionImpl execution) {
        Assertions.assertThatThrownBy(() -> ValidateParams.verifyClusterConfigPresent(execution))
                .isInstanceOf(BpmnError.class).hasMessageContaining(CLUSTER_CONFIG_NOT_PRESENT_ERROR_MESSAGE);
    }

}
