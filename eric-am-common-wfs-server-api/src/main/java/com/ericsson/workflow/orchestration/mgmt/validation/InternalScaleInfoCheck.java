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
package com.ericsson.workflow.orchestration.mgmt.validation;

import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;

import jakarta.validation.ConstraintValidatorContext;

public class InternalScaleInfoCheck extends ChartPropertiesCheck<InternalScaleInfo> {
    @Override
    public boolean isValid(final InternalScaleInfo internalScaleInfo, final ConstraintValidatorContext context) {
        return true;
    }
}
