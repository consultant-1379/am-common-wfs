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

import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;

public class ScaleInfoCheck extends ChartPropertiesCheck<ScaleInfo> {
    @Override
    public boolean isValid(final ScaleInfo scaleInfo, final ConstraintValidatorContext context) {
        String chartUrl = scaleInfo.getChartUrl();
        String chartName = scaleInfo.getChartName();
        String chartVersion = scaleInfo.getChartVersion();
        return validateChartParams(chartUrl, chartName, chartVersion, context, null);
    }
}
