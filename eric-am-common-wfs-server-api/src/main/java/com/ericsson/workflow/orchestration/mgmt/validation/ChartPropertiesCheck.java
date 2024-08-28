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

import java.net.MalformedURLException;
import java.net.URL;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ChartPropertiesCheck<T> implements ConstraintValidator<ValidateChartProperties, T> {

    private static final String CHART_NAME_URL = "CHART_NAME_URL";

    public static boolean validateChartParams(final String chartUrl, final String chartName, final String chartVersion,
            final ConstraintValidatorContext context, final String chartType) {
        boolean validated = true;
        LOGGER.info("validateChartParams --> Current chartType: {}. If chartType is null will be set default to CNF.", chartType);
        String chartTypeInitialized = (chartType == null) ? InstantiateInfo.ChartTypeEnum.CNF.name() : chartType;
        if (!checkChartParamsNotNull(chartUrl, chartName, context)) {
            validated = false;
        } else if (chartUrl != null) {
            validated = isValidated(chartUrl, chartName, chartVersion, context, chartTypeInitialized);
        } else if (chartName != null && validateUrl(chartName)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(CHART_NAME_URL).addConstraintViolation();
            validated = false;
        }
        return validated;
    }

    private static boolean isValidated(String chartUrl, String chartName,
                                       String chartVersion, ConstraintValidatorContext context, String chartTypeInitialized) {
        boolean validated = true;
        if (!validateUrl(chartUrl)) {
            validated = false;
        } else {
            context.disableDefaultConstraintViolation();
        }
        if (chartTypeInitialized.equals(InstantiateInfo.ChartTypeEnum.CNF.name()) && (chartName != null || chartVersion != null)) {
            context.buildConstraintViolationWithTemplate("CHART_OPTIONAL_MANDATORY_VARIABLES")
                    .addConstraintViolation();
            checkChartNameUrl(chartName, context);
            validated = false;
        } else if (chartTypeInitialized.equals(InstantiateInfo.ChartTypeEnum.CRD.name()) && chartVersion == null) {
            context.buildConstraintViolationWithTemplate("CHART_VERSION_PARAM_REQUIRED")
                    .addConstraintViolation();
            validated = false;
        }
        return validated;
    }

    private static void checkChartNameUrl(final String chartName, final ConstraintValidatorContext context) {
        if (validateUrl(chartName)) {
            context.buildConstraintViolationWithTemplate(CHART_NAME_URL).addConstraintViolation();
        }
    }

    private static boolean validateUrl(final String urlToCheck) {
        try {
            new URL(urlToCheck);
        } catch (MalformedURLException ignored) {
            return false;
        }
        return true;
    }

    private static boolean checkChartParamsNotNull(final String chartUrl, final String chartName,
            final ConstraintValidatorContext context) {
        if (chartName == null && chartUrl == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("CHART_PARAM_REQUIRED").addConstraintViolation();
            return false;
        }
        return true;
    }

    @Override
    public void initialize(final ValidateChartProperties constraintAnnotation) {
        //Nothing to initialize
    }

}
