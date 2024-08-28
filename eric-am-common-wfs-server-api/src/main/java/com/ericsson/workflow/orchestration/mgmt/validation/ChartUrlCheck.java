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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;

public class ChartUrlCheck implements ConstraintValidator<ValidateChartUrl, String> {

    @Override
    public void initialize(final ValidateChartUrl constraintAnnotation) {
        //Nothing to initialize
    }

    @Override
    public boolean isValid(final String chartUrl, final ConstraintValidatorContext context) {
        try {
            new URL(chartUrl);
        } catch (MalformedURLException ignored) {
            return false;
        }
        return true;
    }
}
