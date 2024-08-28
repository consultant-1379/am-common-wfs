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

import jakarta.validation.ConstraintValidatorContext;

import com.ericsson.workflow.orchestration.mgmt.model.v3.RollbackInfo;

public class RollbackInfoCheck extends ChartPropertiesCheck<RollbackInfo> {
    @Override
    public boolean isValid(final RollbackInfo rollbackInfo, final ConstraintValidatorContext context) {
        return true;
    }
}
