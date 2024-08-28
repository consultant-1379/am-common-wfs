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

import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;

public class UpgradeInfoCheck extends ChartPropertiesCheck<UpgradeInfo> {
    @Override
    public boolean isValid(final UpgradeInfo upgradeInfo, final ConstraintValidatorContext context) {
        String chartUrl = upgradeInfo.getChartUrl();
        String chartName = upgradeInfo.getChartName();
        String chartVersion = upgradeInfo.getChartVersion();
        String chartType = upgradeInfo.getChartType() != null ? upgradeInfo.getChartType().name() : null;
        return validateChartParams(chartUrl, chartName, chartVersion, context, chartType);
    }
}
