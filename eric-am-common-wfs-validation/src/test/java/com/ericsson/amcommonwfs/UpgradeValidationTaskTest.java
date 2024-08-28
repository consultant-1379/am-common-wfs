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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import static com.ericsson.amcommonwfs.InstantiationValidationTaskTest.EMPTY_STRING;
import static com.ericsson.amcommonwfs.InstantiationValidationTaskTest.RELEASE_NAME_VALUE;
import static com.ericsson.amcommonwfs.UnitTestUtils.expectedException;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;

public class UpgradeValidationTaskTest {

    public static final String CHART_NAME_VALUE = "ver: 2.0";
    private static final List<String> MANDATORY_PARAMS_UPGRADE = unmodifiableList(
            new ArrayList<>(asList(RELEASE_NAME, CHART_NAME)));
    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void successfulUpgradePropertyValidation() {
        setVariables(RELEASE_NAME_VALUE, CHART_NAME_VALUE);
        assertThat(ValidateParams.getMissingProperties(execution, MANDATORY_PARAMS_UPGRADE)).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void unsuccessfulPropertyValidation() {
        verifyExceptionThrownWhenMissingParams(RELEASE_NAME, EMPTY_STRING, CHART_NAME_VALUE);
        verifyExceptionThrownWhenMissingParams(CHART_NAME, RELEASE_NAME_VALUE, EMPTY_STRING);
    }

    private void setVariables(String releaseNameValue, String chartNameValue) {
        execution.setVariableLocal(RELEASE_NAME, releaseNameValue);
        execution.setVariableLocal(CHART_NAME, chartNameValue);
    }

    private void verifyExceptionThrownWhenMissingParams(final String expected, final String releaseName,
            final String chartName) {
        setVariables(releaseName, chartName);
        expectedException(execution, expected, MANDATORY_PARAMS_UPGRADE);
    }

}
