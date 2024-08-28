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
package com.ericsson.infrastructure.factory;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.amcommonwfs.presentation.dto.TerminateInfoDto;
import com.ericsson.amcommonwfs.utils.constants.Constants;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.RollbackInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;

public class TestResourceDtoFactory {

    private static final String CLUSTER_NAME = "test-cluster";
    private static final String LIFECYCLE_OPERATION_ID = "test-id";
    private static final String REVISION_NUMBER = "1";
    private static final String STATE = "starting";
    private static final String RELEASE_NAME = "test-release";
    private static final String NAMESPACE = "test-namespace";
    private static final String CHART_VERSION = "0.0.1";
    private static final String CHART_URL = "http://dummyhost/eric-un-notification-service-0.0.1-222.tgz";
    private static final String CHART_NAME = "adp-am/test-chart";
    private static final String DEFAULT_TIMEOUT = "300";

    private static final boolean OVERRIDE_GLOBAL_REGISTRY = true;
    private static final boolean SKIP_VERIFICATION = false;
    private static final boolean SKIP_JOB_VERIFICATION = false;
    private static final boolean CLEAN_UP_RESOURCES = true;

    private static final String HELM_CLIENT_VERSION = "3.10";

    public ResourceInfoDto<InstantiateInfo> createTestInstantiateInfo() {
        InstantiateInfo testInstantiateInfo = new InstantiateInfo();

        testInstantiateInfo.setClusterName(CLUSTER_NAME);
        testInstantiateInfo.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        testInstantiateInfo.setState(STATE);
        testInstantiateInfo.setNamespace(NAMESPACE);
        testInstantiateInfo.setChartVersion(CHART_VERSION);
        testInstantiateInfo.setChartUrl(CHART_URL);
        testInstantiateInfo.setChartName(CHART_NAME);

        testInstantiateInfo.setApplicationTimeOut(DEFAULT_TIMEOUT);

        testInstantiateInfo.setOverrideGlobalRegistry(OVERRIDE_GLOBAL_REGISTRY);
        testInstantiateInfo.setSkipVerification(SKIP_VERIFICATION);
        testInstantiateInfo.setSkipJobVerification(SKIP_JOB_VERIFICATION);
        testInstantiateInfo.setCleanUpResources(CLEAN_UP_RESOURCES);

        testInstantiateInfo.setAdditionalParams(createAdditionalParams());

        testInstantiateInfo.setHelmClientVersion(HELM_CLIENT_VERSION);

        return new ResourceInfoDto<>(testInstantiateInfo, RELEASE_NAME);
    }

    public ResourceInfoDto<RollbackInfo> createTestRollbackInfo() {
        RollbackInfo testRollbackInfo = new RollbackInfo();

        testRollbackInfo.setNamespace(NAMESPACE);
        testRollbackInfo.setClusterName(CLUSTER_NAME);
        testRollbackInfo.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        testRollbackInfo.setState(STATE);
        testRollbackInfo.setRevisionNumber(REVISION_NUMBER);

        testRollbackInfo.setApplicationTimeOut(DEFAULT_TIMEOUT);

        testRollbackInfo.setSkipVerification(SKIP_VERIFICATION);
        testRollbackInfo.setSkipJobVerification(SKIP_JOB_VERIFICATION);

        testRollbackInfo.setHelmClientVersion(HELM_CLIENT_VERSION);

        return new ResourceInfoDto<>(testRollbackInfo, RELEASE_NAME);
    }

    public ResourceInfoDto<ScaleInfo> createScaleInfo() {
        ScaleInfo testScaleInfo = new ScaleInfo();

        testScaleInfo.setNamespace(NAMESPACE);
        testScaleInfo.setClusterName(CLUSTER_NAME);
        testScaleInfo.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        testScaleInfo.setState(STATE);
        testScaleInfo.setChartVersion(CHART_VERSION);
        testScaleInfo.setChartUrl(CHART_URL);
        testScaleInfo.setChartName(CHART_NAME);

        testScaleInfo.setApplicationTimeOut(DEFAULT_TIMEOUT);

        testScaleInfo.setOverrideGlobalRegistry(OVERRIDE_GLOBAL_REGISTRY);

        testScaleInfo.setAdditionalParams(createAdditionalParams());

        testScaleInfo.setHelmClientVersion(HELM_CLIENT_VERSION);

        return new ResourceInfoDto<>(testScaleInfo, RELEASE_NAME);
    }

    public TerminateInfoDto createTestTerminateInfoDto() {
        TerminateInfoDto terminateInfoDto = new TerminateInfoDto();

        terminateInfoDto.setReleaseName(RELEASE_NAME);
        terminateInfoDto.setNamespace(NAMESPACE);
        terminateInfoDto.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        terminateInfoDto.setState(STATE);
        terminateInfoDto.setClusterName(CLUSTER_NAME);

        terminateInfoDto.setApplicationTimeOut(DEFAULT_TIMEOUT);

        terminateInfoDto.setSkipVerification(SKIP_VERIFICATION);
        terminateInfoDto.setSkipJobVerification(SKIP_JOB_VERIFICATION);
        terminateInfoDto.setCleanUpResources(CLEAN_UP_RESOURCES);

        terminateInfoDto.setHelmClientVersion(HELM_CLIENT_VERSION);

        return terminateInfoDto;
    }

    public ResourceInfoDto<UpgradeInfo> createTestUpgradeInfo() {
        UpgradeInfo upgradeInfo = new UpgradeInfo();

        upgradeInfo.setNamespace(NAMESPACE);
        upgradeInfo.setClusterName(CLUSTER_NAME);
        upgradeInfo.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        upgradeInfo.setState(STATE);
        upgradeInfo.setChartVersion(CHART_VERSION);
        upgradeInfo.setChartUrl(CHART_URL);
        upgradeInfo.setChartName(CHART_NAME);

        upgradeInfo.setApplicationTimeOut(DEFAULT_TIMEOUT);

        upgradeInfo.setSkipVerification(SKIP_VERIFICATION);
        upgradeInfo.setSkipJobVerification(SKIP_JOB_VERIFICATION);
        upgradeInfo.setOverrideGlobalRegistry(OVERRIDE_GLOBAL_REGISTRY);

        upgradeInfo.setAdditionalParams(createAdditionalParams());

        upgradeInfo.setHelmClientVersion(HELM_CLIENT_VERSION);

        return new ResourceInfoDto<>(upgradeInfo, RELEASE_NAME);
    }

    private static Map<String, String> createAdditionalParams() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(RELEASE_NAME, "my-release");
        additionalParams.put(Constants.CHART_NAME, "my-chart");
        return additionalParams;
    }
}
