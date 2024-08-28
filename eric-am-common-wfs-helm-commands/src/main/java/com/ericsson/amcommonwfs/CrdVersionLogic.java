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

import static com.ericsson.amcommonwfs.constants.CommandConstants.COMMAND_TYPE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.BUILD_NUMBER_REGEX;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION_NEWER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_VERSION_OLDER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_VERSION_IN_CLUSTER;
import static com.ericsson.amcommonwfs.utils.constants.Constants.PROCEED_WITH_CRD_INSTALL;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_CRD_FAILED;

import java.util.regex.Matcher;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.error.BusinessProcessExceptionUtils;
import com.fasterxml.jackson.core.Version;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CrdVersionLogic implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(DelegateExecution execution) {
        boolean proceedWithCRDInstallation = false;
        String crdVersionInChart = (String) execution.getVariable(CHART_VERSION);
        String crdVersionInCluster = (String) execution.getVariable(CRD_VERSION_IN_CLUSTER);

        if (Strings.isNullOrEmpty(crdVersionInCluster)) {
            BusinessProcessExceptionUtils.handleException(BPMN_CRD_FAILED, "Invalid chartVersion", execution);
        } else if (StringUtils.equals(crdVersionInChart, crdVersionInCluster)) {
            LOGGER.info("CRD Version in cluster : {} is equal to CRD version in chart to be installed : {}. ",
                    crdVersionInCluster, crdVersionInChart);
            LOGGER.info("Skipping install/upgrade of CRD");
            proceedWithCRDInstallation = false;
        } else {
            LOGGER.info("Chart found in cluster, comparing new chart version {} to existing chart version {}",
                                      crdVersionInChart, crdVersionInCluster);
            proceedWithCRDInstallation = shouldOperationContinue(crdVersionInChart, crdVersionInCluster);
        }
        LOGGER.info("Version comparison complete, flag to continue set to: {}.", proceedWithCRDInstallation);
        execution.setVariable(PROCEED_WITH_CRD_INSTALL, proceedWithCRDInstallation);
        execution.setVariable(COMMAND_TYPE, "crd");
    }

    private static boolean shouldOperationContinue(final String versionInChart, final String versionInCluster) {
        Matcher chartMatcher = BUILD_NUMBER_REGEX.matcher(versionInChart);
        Matcher clusterMatcher = BUILD_NUMBER_REGEX.matcher(versionInCluster);
        Version chartSemanticVersion = chartMatcher.find()
                ? convertStringToVersion(versionInChart.substring(0, versionInChart.indexOf(chartMatcher.group(0))))
                : convertStringToVersion(versionInChart);
        Version clusterSemanticVersion = clusterMatcher.find()
                ? convertStringToVersion(versionInCluster.substring(0, versionInCluster.indexOf(clusterMatcher.group(0))))
                : convertStringToVersion(versionInCluster);
        int versionDifference = chartSemanticVersion.compareTo(clusterSemanticVersion);
        if (versionDifference > 0) {
            LOGGER.info(CHART_VERSION_NEWER);
            return true;
        } else if (versionDifference == 0) {
            return isChartBuildNumberNewer(versionInChart, versionInCluster);
        } else {
            LOGGER.info(CHART_VERSION_OLDER);
            return false;
        }
    }

    private static Version convertStringToVersion(final String versionAsString) {
        String[] versionParts = versionAsString.split("\\.", 3);
        return new Version(Integer.parseInt(versionParts[0]), Integer.parseInt(versionParts[1]), Integer.parseInt(versionParts[2]),
                                      null, null, null);
    }

    private static boolean isChartBuildNumberNewer(String chartVersion, String clusterVersion) {
        Matcher chartBuildNumberMatcher = BUILD_NUMBER_REGEX.matcher(chartVersion);
        Matcher clusterBuildNumberMatcher = BUILD_NUMBER_REGEX.matcher(clusterVersion);
        boolean foundInChart = chartBuildNumberMatcher.find();
        boolean foundInCluster = clusterBuildNumberMatcher.find();
        if (foundInChart && foundInCluster) {
            int chartBuildNumber = Integer.parseInt(chartVersion.substring(chartBuildNumberMatcher.end()).trim());
            int clusterBuildNumber =  Integer.parseInt(clusterVersion.substring(clusterBuildNumberMatcher.end()).trim());
            LOGGER.info(String.format("Semantic versions are the same, comparing chart build number %d to existing chart build number %d and "
                                              + "setting flag accordingly.", chartBuildNumber, clusterBuildNumber));
            return chartBuildNumber > clusterBuildNumber;
        } else if (foundInChart) {
            LOGGER.info(String.format("Semantic versions are the same. Did not find build number in existing chart %s, setting flag to continue to: "
                    + "true.", clusterVersion));
            return true;
        }
        LOGGER.info(String.format("Semantic versions are the same. Did not find build number in chart %s, setting flag to continue to: false.",
                                  chartVersion));
        return false;
    }
}
