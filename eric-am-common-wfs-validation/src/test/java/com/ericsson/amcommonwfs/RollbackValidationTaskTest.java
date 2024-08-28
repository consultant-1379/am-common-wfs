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

import static com.ericsson.amcommonwfs.TaskConstants.RELEASE_NAME_ERROR_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.REQUIRED_PROPERTIES_ERROR_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.REVISION_NUMBER_ERROR_MSG;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;

public class RollbackValidationTaskTest {

    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_RELEASE_NAME = "app-name";
    private static final String INVALID_RELEASE_NAME = "1somename-123";
    private static final String VALID_RELEASE_NAME = "somename-123";
    private static final String DEFAULT_REVISION_NUMBER = "22";
    private static final String INVALID_REVISION_NUMBER = "a12";
    private static final List<String> MANDATORY_PARAMS_ROLLBACK = unmodifiableList(
            new ArrayList<>(asList(RELEASE_NAME, REVISION_NUMBER)));
    private final ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void successfulPropertyValidation() {
        setVariables(DEFAULT_RELEASE_NAME, DEFAULT_REVISION_NUMBER);
        assertThat(ValidateParams.getMissingProperties(execution, MANDATORY_PARAMS_ROLLBACK)).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void unsuccessfulPropertyValidation() {
        verifyExceptionThrownWhenMissingProperties(RELEASE_NAME, null, DEFAULT_REVISION_NUMBER);
        verifyExceptionThrownWhenMissingProperties(REVISION_NUMBER, DEFAULT_RELEASE_NAME, null);
    }

    @Test
    public void successfulRevisionNumberValidation() {
        setRevisionNumber(DEFAULT_REVISION_NUMBER);
        ValidateRevisionNumber.checkRevisionNumber(execution);
    }

    @Test
    public void checkRevisionNumber() {
        assertThat(ValidateRevisionNumber.isValidRevisionNumber(DEFAULT_REVISION_NUMBER)).isTrue();
    }

    @Test
    public void checkNullRevisionNumber() {
        assertThat(ValidateRevisionNumber.isValidRevisionNumber(null)).isFalse();
    }

    @Test
    public void checkInvalidRevisionNumber() {
        assertThat(ValidateRevisionNumber.isValidRevisionNumber(INVALID_REVISION_NUMBER)).isFalse();
    }

    @Test
    public void verifyExceptionThrownWhenRevisionNumberNotNumeric() {
        setRevisionNumber(INVALID_REVISION_NUMBER);
        expectedExceptionRevisionNumber();
    }

    @Test
    public void verifyExceptionThrownWhenRevisionNumberNull() {
        setRevisionNumber(null);
        expectedExceptionRevisionNumber();
    }

    @Test
    public void successfulValidateReleaseName() {
        setReleaseName(VALID_RELEASE_NAME);
        assertThat(ValidateReleaseName.isValidReleaseName(VALID_RELEASE_NAME)).isTrue();
    }

    @Test
    public void unsuccessfulValidateReleaseName() {
        setReleaseName(INVALID_RELEASE_NAME);
        assertThat(ValidateReleaseName.isValidReleaseName(INVALID_RELEASE_NAME)).isFalse();
    }

    @Test
    public void verifyExceptionThrownWhenReleaseNameNotWellFormed() {
        setReleaseName(INVALID_RELEASE_NAME);
        expectedExceptionReleaseName();
    }

    private void expectedExceptionRevisionNumber() {
        Assertions.assertThatThrownBy(() -> ValidateRevisionNumber.checkRevisionNumber(execution))
                .isInstanceOf(BpmnError.class).hasMessage(REVISION_NUMBER_ERROR_MSG);
    }

    private void verifyExceptionThrownWhenMissingProperties(final String expected, final String releaseName,
                                                            final String revisionNumber) {
        setVariables(releaseName, revisionNumber);
        expectedException(expected);
    }

    private void setVariables(String releaseName, String revisionNumber) {
        execution.setVariableLocal(REVISION_NUMBER, revisionNumber);
        execution.setVariableLocal(RELEASE_NAME, releaseName);
    }

    private void setReleaseName(String releaseName) {
        execution.setVariableLocal(RELEASE_NAME, releaseName);
    }

    private void setRevisionNumber(String revisionNumber) {
        execution.setVariableLocal(REVISION_NUMBER, revisionNumber);
    }

    private void expectedException(String expectedMessage) {
        Assertions.assertThatThrownBy(() -> ValidateParams.validateMandatoryParameters(execution,
                                                                                       MANDATORY_PARAMS_ROLLBACK))
                .isInstanceOf(BpmnError.class).hasMessage(REQUIRED_PROPERTIES_ERROR_MSG + expectedMessage);
    }

    private void expectedExceptionReleaseName() {
        Assertions.assertThatThrownBy(() -> ValidateReleaseName.checkReleaseName(execution))
                .isInstanceOf(BpmnError.class).hasMessage(RELEASE_NAME_ERROR_MSG);
    }
}
