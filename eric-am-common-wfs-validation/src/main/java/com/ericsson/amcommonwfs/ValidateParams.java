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

import static com.ericsson.amcommonwfs.TaskConstants.ADDITIONAL_CHART_VALUES_ERROR_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.ADDITIONAL_PARAMS_AS_MAP_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.CLUSTER_CONFIG_NOT_PRESENT_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.TaskConstants.MALFORMED_URL_ERROR_MSG;
import static com.ericsson.amcommonwfs.TaskConstants.REQUIRED_PROPERTIES_ERROR_MSG;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_PARAMS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_URL;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_CLUSTER_CONFIG_NOT_PRESENT;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_INVALID_ARGUMENT_EXCEPTION;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidateParams {

    @VisibleForTesting
    static void validateMandatoryParameters(final DelegateExecution execution, final List<String> mandatoryParams) {
        String missingProperties = getMissingProperties(execution, mandatoryParams);
        if (!missingProperties.isEmpty()) {
            LOGGER.error("{}{}", REQUIRED_PROPERTIES_ERROR_MSG, missingProperties);
            BusinessProcessExceptionUtils
                    .handleException(BPMN_INVALID_ARGUMENT_EXCEPTION, REQUIRED_PROPERTIES_ERROR_MSG + missingProperties,
                            execution);
        } else {
            LOGGER.info("Mandatory parameters have been validated.");
        }
    }

    @VisibleForTesting
    static String getMissingProperties(final DelegateExecution execution, final List<String> mandatoryParams) {
        StringBuilder bld = new StringBuilder();
        for (String property : mandatoryParams) {
            String variableLocal = (String) execution.getVariableLocal(property);
            if (Strings.isNullOrEmpty(variableLocal)) {
                bld.append(" ").append(property);
            }
        }
        return bld.toString().trim();
    }

    @VisibleForTesting
    static void verifyAdditionalParamsIsMap(final DelegateExecution execution) {
        Object additionalParams = execution.getVariableLocal(ADDITIONAL_PARAMS);
        if (additionalParams != null && !(additionalParams instanceof Map)) {
            BusinessProcessExceptionUtils.handleException(BPMN_INVALID_ARGUMENT_EXCEPTION, ADDITIONAL_PARAMS_AS_MAP_MSG, execution);
        }
    }

    @VisibleForTesting
    static void validateChartParameters(final DelegateExecution execution) {
        final String chartUrl = (String) execution.getVariableLocal(CHART_URL);
        final String chartName = (String) execution.getVariableLocal(CHART_NAME);
        final String chartVersion = (String) execution.getVariableLocal(CHART_VERSION);

        if (!Strings.isNullOrEmpty(chartUrl)) {
            LOGGER.info("{} property has been specified: {}", CHART_URL, chartUrl);
            if (!Strings.isNullOrEmpty(chartName) || !Strings.isNullOrEmpty(chartVersion)) {
                LOGGER.error("{} or {} properties should not be specified when {} property is set. " +
                                "Please see API documentation for correct usage.",
                        CHART_NAME, CHART_VERSION, CHART_URL);
                BusinessProcessExceptionUtils.handleException(BPMN_INVALID_ARGUMENT_EXCEPTION,
                        String.format(ADDITIONAL_CHART_VALUES_ERROR_MSG, CHART_NAME, CHART_VERSION), execution);
            } else {
                validateUrl(execution, chartUrl);
            }
        } else {
            LOGGER.info("{} property has not been set, checking {} property is provided", CHART_URL,
                    CHART_NAME);
            if (Strings.isNullOrEmpty(chartName)) {
                LOGGER.error("{} property has not been specified, a value for {} or {} must be specified ",
                        CHART_NAME, CHART_NAME, CHART_URL);
                BusinessProcessExceptionUtils.handleException(BPMN_INVALID_ARGUMENT_EXCEPTION,
                        String.format(REQUIRED_PROPERTIES_ERROR_MSG + "%s or %s ", CHART_NAME, CHART_URL), execution);
            } else {
                LOGGER.info("{} property has been specified: {}", CHART_NAME, chartName);
                LOGGER.info("Checking to see if {} property has been specified also...", CHART_VERSION);
                verifyChartVersionProperty(chartName, chartVersion);
            }
        }
    }

    private static void verifyChartVersionProperty(final String chartName, final String chartVersion) {
        if (!Strings.isNullOrEmpty(chartVersion)) {
            LOGGER.info("{} and {} properties are provided: {} {}", CHART_NAME, CHART_VERSION,
                    chartName, chartVersion);
        } else {
            LOGGER.info("{} property has not been provided, latest version of {} will be used.", CHART_VERSION,
                    chartName);
        }
    }

    @VisibleForTesting
    static void validateUrl(DelegateExecution execution, String chartUrl) {
        try {
            LOGGER.debug("Validating {} property......", CHART_URL);
            new URL(chartUrl);
            LOGGER.debug("{} validation successful", CHART_URL);
        } catch (MalformedURLException e) {
            LOGGER.error("{} is not a valid Chart URL", chartUrl);
            BusinessProcessExceptionUtils
                    .handleException(BPMN_INVALID_ARGUMENT_EXCEPTION, MALFORMED_URL_ERROR_MSG + chartUrl,
                            execution);
        }
    }

    @VisibleForTesting
    static void verifyClusterConfigPresent(final DelegateExecution execution) {
        String clusterConfig = (String) execution.getVariable(ORIGINAL_CLUSTER_NAME);
        LOGGER.debug("Validating cluster config, {}", clusterConfig);
        if (!Strings.isNullOrEmpty(clusterConfig)) {
            String clusterContentKey = (String) execution.getVariable(CLUSTER_CONFIG_CONTENT_KEY);
            if (Strings.isNullOrEmpty(clusterContentKey)) {
                LOGGER.error("cluster config file {} not present", clusterConfig);
                BusinessProcessExceptionUtils
                        .handleException(BPMN_CLUSTER_CONFIG_NOT_PRESENT, CLUSTER_CONFIG_NOT_PRESENT_ERROR_MESSAGE,
                                execution);
            }
            LOGGER.info("Validating done, cluster config present in {}", clusterConfig);
        } else {
            LOGGER.info("Cluster config is not provided, creating application in current cluster");
        }
    }
}
