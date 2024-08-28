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
package com.ericsson.amcommonwfs.tests;

import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.createNamespace;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.createSecret;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.deleteClusterRolesAndClusterRoleBindings;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.deleteCrd;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.deleteHelmRelease;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.deleteNamespaces;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeDeleteNamespaceRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeGetHelmVersions;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeGetPodsRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeInstantiateRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executePostRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executePutRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeTerminateJsonRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.executeTerminateMultipartRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.getInternalScaleInfo;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.teardownStep;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppDeleted;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppInstalled;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppInstalledWithCorrectPodNumber;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppNotUpgraded;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppRollback;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppScaled;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyAppUpgraded;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyErrorOutputOfWorkflowV3;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyFailedInstantiateRequest;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyHelmVersions;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyHistoryQueryByReleaseNameWithInstanceIdV3;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyNamespace;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyOverriddenValueSet;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyPodStatus;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyPodsWithTimeout;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifySecretKeyValue;
import static com.ericsson.amcommonwfs.steps.CommonWfsAcceptanceTestSteps.verifyTerminatePvcWorks;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FAILED_UPGRADE_FAILED_VERIFICATION;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FAILED_UPGRADE_MISSING_PARAMETERS;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SKIP_VERIFICATION_SPIDERAPP;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SPIDERAPP;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SPIDERAPP_ANNOTATIONS;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SPIDERAPP_BAD;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SPIDERAPP_YAML;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_NAME_V2_SQL;
import static com.ericsson.amcommonwfs.utilities.TestConstants.FULL_SCALEOUT_NAME;
import static com.ericsson.amcommonwfs.utilities.TestConstants.GET_PODS_BY_RELEASE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HOST_PATCH_SECRET;
import static com.ericsson.amcommonwfs.utilities.TestConstants.HTTP_CLIENT;
import static com.ericsson.amcommonwfs.utilities.TestConstants.INTERNAL_DOWNSIZING_URI;
import static com.ericsson.amcommonwfs.utilities.TestConstants.UPGRADE;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_COMPLETED_PODS_COMMAND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_READY_COMMAND;
import static com.ericsson.amcommonwfs.utilities.TestConstants.VERIFY_RUNNING_PODS_COMMAND;
import static com.ericsson.amcommonwfs.utilities.Utils.executeCommand;
import static com.ericsson.amcommonwfs.utilities.Utils.getHelmVersionsResponseSuccess;
import static com.ericsson.amcommonwfs.utilities.Utils.getJsonAsString;
import static com.ericsson.amcommonwfs.utilities.Utils.getPodStatusResponseSuccess;
import static com.ericsson.amcommonwfs.utilities.Utils.getV3ResourceResponseSuccess;
import static com.ericsson.amcommonwfs.utilities.Utils.replaceChangeNumber;
import static com.ericsson.amcommonwfs.utilities.Utils.replaceNameSpace;
import static com.ericsson.amcommonwfs.utilities.Utils.writeJsonStringToObject;
import static com.ericsson.amcommonwfs.utilities.Utils.writeObjectToJsonString;
import static com.ericsson.workflow.orchestration.mgmt.model.WorkflowState.COMPLETED;
import static com.ericsson.workflow.orchestration.mgmt.model.WorkflowState.FAILED;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.springframework.core.io.FileSystemResource;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.ericsson.amcommonwfs.utilities.FileUtilities;
import com.ericsson.amcommonwfs.utilities.WfsConfigurationEnum;
import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;

import io.kubernetes.client.openapi.ApiException;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;

@Epic("https://jira-nam.lmera.ericsson.se/browse/SM-3250")
@Slf4j
public class CommonWfsAcceptanceTest {
    public static final String VALUE_ONE_MOVED_TO_FILE = "eric-adp-gs-testapp.ingress.enabled";
    public static final String VALUE_TWO_MOVED_TO_FILE = "eric-pm-server.server.ingress.enabled";
    public static final String TEST_SECRET = "test-secret";
    public static final String UNSEAL_KEY = "unsealKey";
    public static final String PATCH_SECRET_TITLE = "PatchSecret";
    private static final String INSTANTIATE_TITLE = "Instantiate";
    private static final String TERMINATE_TITLE = "Terminate";
    private static final String DELETE_PVC_TITLE = "Delete PVC";
    private static final String UPGRADE_TITLE = "Upgrade";
    private static final String SCALE_TITLE = "Scale";
    private static final String SCALE_DOWN_TITLE = "ScaleDown";
    private static final String ROLLBACK_TITLE = "Rollback";
    private static final String GET_HELM_VERSIONS_TITLE = "Get helm versions";
    private static final String UPGRADE_VNF_INSTANCE = "v3/wfsUpgradeInstance.json";
    private static final String INSTANTIATE_VNF_INSTANCE = "v3/wfsInstantiateInstance.json";
    private static final String INSTANTIATE_TO_EVNFM_NAMESPACE_VNF_INSTANCE = "v3/wfsInstantiateToEvnfmNamespace.json";
    private static final String RUNNING_POD_STATE = "Running";
    private static final String SUCCEEDED_POD_STATE = "Succeeded";
    private static final String CRD_NAMESPACE = "eric-crd-ns";
    private static final String KVDB_CRD_RELEASE_NAME = "eric-data-kvdb-ag-crd";
    private static final String ERIC_DATA_KVDB_AG_CR = "geodeclusters.kvdbag.data.ericsson.com";
    private static final String EVNFM_NAMESPACE_INSTANTIATION_ERROR = "Cannot instantiate in the same namespace which "
            + "EVNFM is deployed in. Use a different Namespace.";

    private final CloseableHttpClient client = HTTP_CLIENT.get();

    @AfterAll
    public static void teardown() {
        teardownStep(FULL_NAME_V2_SQL);
        teardownStep(FULL_NAME_V2_SPIDERAPP);
        teardownStep(FULL_NAME_V2_SPIDERAPP_BAD);
        teardownStep(FULL_SCALEOUT_NAME);
        teardownStep(FULL_NAME_V2_SKIP_VERIFICATION_SPIDERAPP);
        teardownStep(FULL_NAME_V2_SPIDERAPP_YAML);
        teardownStep(FAILED_UPGRADE_MISSING_PARAMETERS);
        teardownStep(FAILED_UPGRADE_FAILED_VERIFICATION);
        teardownStep(FULL_NAME_V2_SPIDERAPP_ANNOTATIONS);
        deleteNamespaces(1, 13);
        deleteClusterRolesAndClusterRoleBindings();

        // delete CRD release manually
        deleteHelmRelease(KVDB_CRD_RELEASE_NAME, CRD_NAMESPACE);
        deleteCrd(ERIC_DATA_KVDB_AG_CR);
    }

    @Test
    @DisplayName("V3 Install with overridden value, verify successful, scale down, upgrade and delete application")
    public void commonWfsTest() throws IOException {
        String namespacePrefix = "test1";

        String json = getJsonAsString(INSTANTIATE_VNF_INSTANCE);
        String formattedReleaseName = replaceChangeNumber("wfs-accept-v2-spider-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, namespacePrefix);
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        verifyAppInstalledWithCorrectPodNumber(3, String.format(VERIFY_RUNNING_PODS_COMMAND, formattedReleaseName));
        verifyOverriddenValueSet(instantiateInfo, formattedReleaseName);

        // Test case for kubernetes java client
        String podResponse = executeGetPodsRequest(INSTANTIATE_TITLE, formattedReleaseName, client);
        PodStatusResponse podStatusResponse = getPodStatusResponseSuccess(podResponse);
        verifyPodStatus(podStatusResponse, 6, RUNNING_POD_STATE);

        String jsonUpgrade = getJsonAsString(UPGRADE_VNF_INSTANCE);
        String formattedUpgradeNamespace = replaceNameSpace(jsonUpgrade, namespacePrefix);
        String upgradeResponseBody = executePostRequest(UPGRADE_TITLE, client, formattedReleaseName, formattedUpgradeNamespace, UPGRADE);

        ResourceResponseSuccess resourceResponseUpgradeSuccess = getV3ResourceResponseSuccess(upgradeResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, resourceResponseUpgradeSuccess,
                COMPLETED);
        verifyAppUpgraded(formattedReleaseName, instantiateInfo.getNamespace());

        String jsonRollback = getJsonAsString("v3/wfsRollbackInstance.json");
        String formattedRollbackNamespace = replaceNameSpace(jsonRollback, namespacePrefix);
        String rollbackResponseBody = executePostRequest(ROLLBACK_TITLE, client, formattedReleaseName,
                                                         formattedRollbackNamespace, "rollback");
        ResourceResponseSuccess resourceResponseRollbackSuccess = getV3ResourceResponseSuccess(rollbackResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(ROLLBACK_TITLE, client, resourceResponseRollbackSuccess,
                COMPLETED);
        verifyAppRollback(jsonRollback, formattedReleaseName, instantiateInfo.getNamespace());

        String deleteResponseBody = executeTerminateMultipartRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                     instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess = getV3ResourceResponseSuccess(deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess, COMPLETED);
        verifyAppDeleted(formattedReleaseName);
        verifyTerminatePvcWorks(instantiateInfo.getNamespace(), client, DELETE_PVC_TITLE, formattedReleaseName);
    }

    @Ignore // Ignored just now as too lengthy in current format
    @Test
    @DisplayName("V3 Install with overridden value, verify successful using annotations, upgrade and delete "
            + "application")
    public void commonWfsTestWithVerificationUsingAnnotations() throws IOException {
        String json = getJsonAsString("v3/wfsInstantiateInstanceVerifyUsingAnnotations.json");
        String formattedReleaseName = replaceChangeNumber("annotations-spiderapp-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, "test9");
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseSuccess =
                getV3ResourceResponseSuccess(
                        deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        verifyAppInstalledWithCorrectPodNumber(6,
                String.format(VERIFY_READY_COMMAND, formattedReleaseName));
        verifyOverriddenValueSet(instantiateInfo, formattedReleaseName);

        String jsonUpgrade = getJsonAsString("v3/wfsUpgradeInstanceVerifyUsingAnnotations.json");
        String upgradeResponseBody = executePostRequest(UPGRADE_TITLE, client, formattedReleaseName, jsonUpgrade, UPGRADE);
        ResourceResponseSuccess resourceResponseUpgradeSuccess = getV3ResourceResponseSuccess(upgradeResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, resourceResponseUpgradeSuccess,
                COMPLETED);
        verifyAppUpgraded(formattedReleaseName, instantiateInfo.getNamespace());

        String jsonRollback = getJsonAsString("v3/wfsRollbackInstance.json");
        String rollbackResponseBody = executePostRequest(ROLLBACK_TITLE, client, formattedReleaseName, jsonRollback, "rollback");
        ResourceResponseSuccess resourceResponseRollbackSuccess = getV3ResourceResponseSuccess(rollbackResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(ROLLBACK_TITLE, client, resourceResponseRollbackSuccess,
                COMPLETED);
        verifyAppRollback(jsonRollback, formattedReleaseName, instantiateInfo.getNamespace());

        String jsonScale = getJsonAsString("v3/wfsScaleInstanceUsingAnnotations.json");
        String scaleResponseBody = executePostRequest(SCALE_TITLE, client, formattedReleaseName, jsonScale, "scale");
        ResourceResponseSuccess resourceResponseScaleSuccess = getV3ResourceResponseSuccess(scaleResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(SCALE_TITLE, client, resourceResponseScaleSuccess, COMPLETED);
        verifyAppScaled(formattedReleaseName, instantiateInfo.getNamespace());

        String deleteResponseBody = executeTerminateJsonRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess = getV3ResourceResponseSuccess(deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess,
                COMPLETED);
        verifyAppDeleted(formattedReleaseName);
    }

    @Test
    @DisplayName("V3 Install with overridden value, verify successful using annotations, scale down and delete application")
    public void commonWfsTestInstantiateAndDeleteWithVerificationUsingAnnotations() {
        String json = getJsonAsString("v3/wfsInstantiateInstanceVerifyUsingAnnotations.json");
        String formattedReleaseName = replaceChangeNumber("annotations-spiderapp-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, "test9");
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        LOGGER.info("Show pods and its information before verification of correctly installed number of pods: {}",
                executeCommand(String.format(GET_PODS_BY_RELEASE, formattedReleaseName)).toString());
        verifyAppInstalledWithCorrectPodNumber(3,
                String.format(VERIFY_READY_COMMAND, formattedReleaseName));
        LOGGER.info("Show pods and its information after verification of correctly installed number of pods: {}",
                executeCommand(String.format(GET_PODS_BY_RELEASE, formattedReleaseName)).toString());
        verifyOverriddenValueSet(instantiateInfo, formattedReleaseName);

        String scaleDownJson = writeObjectToJsonString(getInternalScaleInfo(instantiateInfo, formattedReleaseName));
        executePostRequest(SCALE_DOWN_TITLE, client, scaleDownJson, INTERNAL_DOWNSIZING_URI);

        verifyPodsWithTimeout(SCALE_DOWN_TITLE, formattedReleaseName, client, 500, 0);

        String deleteResponseBody = executeTerminateMultipartRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                     instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess = getV3ResourceResponseSuccess(deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess,
                COMPLETED);
        verifyAppDeleted(formattedReleaseName);
    }

    @Test
    @DisplayName("V3 Install and upgrade with values.yaml file, verify successful and terminate application")
    public void commonWfsValuesYamlTest() {
        FileSystemResource valuesFilePart = FileUtilities.getFile("v3/values.yaml");
        String namespacePrefix = "test2";

        String jsonInstallPart = getJsonAsString("v3/wfsInstantiateYamlCheck.json");
        String formattedReleaseName = replaceChangeNumber("wfs-accept-spider-yaml-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(jsonInstallPart, namespacePrefix);
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        InstantiateInfo strippedOutInstantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        strippedOutInstantiateInfo.getAdditionalParams().remove(VALUE_ONE_MOVED_TO_FILE);
        strippedOutInstantiateInfo.getAdditionalParams().remove(VALUE_TWO_MOVED_TO_FILE);
        String strippedOutInstallJsonPart = writeObjectToJsonString(strippedOutInstantiateInfo);

        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, valuesFilePart,
                                                              strippedOutInstallJsonPart);

        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        verifyAppInstalledWithCorrectPodNumber(3, String.format(VERIFY_RUNNING_PODS_COMMAND, formattedReleaseName));
        verifyOverriddenValueSet(instantiateInfo, formattedReleaseName);

        String jsonUpgradePart = getJsonAsString(UPGRADE_VNF_INSTANCE);
        String formattedUpgradeNamespace = replaceNameSpace(jsonUpgradePart, namespacePrefix);
        UpgradeInfo strippedUpgradeInfo = writeJsonStringToObject(formattedUpgradeNamespace, UpgradeInfo.class);

        strippedUpgradeInfo.getAdditionalParams().remove(VALUE_ONE_MOVED_TO_FILE);
        strippedUpgradeInfo.getAdditionalParams().remove(VALUE_TWO_MOVED_TO_FILE);
        String strippedOutUpgradeJsonPart = writeObjectToJsonString(strippedUpgradeInfo);

        String upgradeResponseBody = executePostRequest(UPGRADE_TITLE, client, formattedReleaseName, valuesFilePart,
                                                        strippedOutUpgradeJsonPart, UPGRADE);

        ResourceResponseSuccess resourceResponseUpgradeSuccess = getV3ResourceResponseSuccess(upgradeResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, resourceResponseUpgradeSuccess,
                COMPLETED);
        verifyAppUpgraded(formattedReleaseName, instantiateInfo.getNamespace());

        String deleteResponseBody = executeTerminateMultipartRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                     instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess = getV3ResourceResponseSuccess(deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess,
                COMPLETED);
        verifyAppDeleted(formattedReleaseName);
        valuesFilePart.getFile().delete(); // NOSONAR
    }

    @Test
    @Ignore("Ignore because need to unblock release flow.")
    @DisplayName("V3 Failed Upgrade attempt, missing parameters")
    public void failedUpgradeTestMissingParameters() {
        String namespacePrefix = "test7";

        String json = getJsonAsString("v3/wfsInstantiateInstanceForFailedUpgradeMissingParameters.json");
        String formattedReleaseName = replaceChangeNumber("wfs-accept-v2-upgrade-missing-parameters-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, namespacePrefix);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);

        String jsonUpgrade = getJsonAsString("v3/wfsUpgradeInstanceMissingParameters.json");
        String formattedUpgradeNamespace = replaceNameSpace(jsonUpgrade, namespacePrefix);

        String upgradeResponseBody = executePostRequest(UPGRADE_TITLE, client, formattedReleaseName, formattedUpgradeNamespace, UPGRADE);

        ResourceResponseSuccess upgradeResourceResponse = getV3ResourceResponseSuccess(upgradeResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, upgradeResourceResponse, FAILED);
        String expectedErrorMessage =
                "failed to create resource: Ingress.extensions \\\"eric-adp-gs-testapp\\\" is " + "invalid";
        verifyErrorOutputOfWorkflowV3(client, upgradeResourceResponse, expectedErrorMessage);

    }

    @Test
    @DisplayName("V3 Instantiate an application and clean up the failed resources")
    public void commonWfsCleanUpTest() {
        String json = getJsonAsString("v3/wfsInstantiateInstanceCleanUp.json");
        String formattedReleaseName = replaceChangeNumber("wfs-accept-v2-spiderapp-bad-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, "test3");
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBodyCleanUp = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseCleanUp = getV3ResourceResponseSuccess(deployResponseBodyCleanUp);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseCleanUp, FAILED);
        String expectedErrorMessage = "An ingress secret is required for helm chart registry service, when deploying "
                + "as stand alone set using --set ingress.tls.secretName=<your-secret>,";
        verifyErrorOutputOfWorkflowV3(client, resourceResponseCleanUp, expectedErrorMessage);
        verifyAppDeleted(FULL_NAME_V2_SPIDERAPP_BAD);
        String namespace = instantiateInfo.getNamespace();
        executeDeleteNamespaceRequest(namespace, client, "default", namespace, formattedReleaseName);
        verifyNamespace(namespace);
    }

    @Test
    @DisplayName("V3 Instantiate an application with deployment validation skipped, verify history status as completed,"
            + " terminate the application")
    public void commonWfsSkipDeploymentVerification() {
        String jsonInstallRequest = getJsonAsString("v3/wfsInstantiateInstanceNoValidation.json");
        String formattedReleaseName = replaceChangeNumber("wfs-accept-v2-spiderapp-skip-validation-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(jsonInstallRequest, "test5");
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);

        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        verifyAppInstalled(FULL_NAME_V2_SKIP_VERIFICATION_SPIDERAPP, 1);
        String deleteResponseBody = executeTerminateMultipartRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                     instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess =
                getV3ResourceResponseSuccess(
                        deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess,
                COMPLETED);
        verifyAppDeleted(formattedReleaseName);
    }

    @Test
    @DisplayName("V3 Instantiate CRD chart and verify successful, upgrade with higher version and verify successful "
            + "upgrade with lower version and verify skipped")
    public void commonWfsCRDTest() {
        String instantiateConfig = "v3/wfsInstantiateInstanceCRD.json";
        String upgradeConfig = "v3/wfsUpgradeInstanceCrd.json";
        String upgradeConfigVersionSubstring = "CRD_VERSION";
        String crdLowerVersion = "4.2.0-1";
        String crdHigherVersion = "4.2.0-2";

        String instantiateJson = getJsonAsString(instantiateConfig);
        String formattedReleaseName = replaceChangeNumber("eric-data-kvdb-ag-crd");
        InstantiateInfo instantiateInfo = writeJsonStringToObject(instantiateJson, InstantiateInfo.class);

        // instantiate and verify successful
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, instantiateJson);
        ResourceResponseSuccess instantiateResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);

        // verify deployment has correct number of pods with helm
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, instantiateResponseSuccess, COMPLETED);
        verifyAppInstalledWithCorrectPodNumber(1, String.format(VERIFY_COMPLETED_PODS_COMMAND, formattedReleaseName));

        // verify deployment has correct number of pods & every pod is in COMPLETED status with interval API
        String podResponse = executeGetPodsRequest(INSTANTIATE_TITLE, formattedReleaseName, client);
        PodStatusResponse podStatusResponse = getPodStatusResponseSuccess(podResponse);
        verifyPodStatus(podStatusResponse, 1, SUCCEEDED_POD_STATE);

        // prepare upgrade configs
        String upgradeJson = getJsonAsString(upgradeConfig);
        String upgradeToHigherVersionConfig = upgradeJson.replace(upgradeConfigVersionSubstring, crdHigherVersion);
        String upgradeToLowerVersionConfig = upgradeJson.replace(upgradeConfigVersionSubstring, crdLowerVersion);

        // upgrade to higher version and verify successful
        ResourceResponseSuccess upgradeResponseSuccess1 =
                performUpgrade(upgradeToHigherVersionConfig, formattedReleaseName);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, upgradeResponseSuccess1, COMPLETED);
        verifyAppUpgraded(formattedReleaseName, instantiateInfo.getNamespace());

        // upgrade to same version and verify skipped
        ResourceResponseSuccess upgradeResponseSuccess2 =
                performUpgrade(upgradeToHigherVersionConfig, formattedReleaseName);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, upgradeResponseSuccess2, COMPLETED);
        verifyAppNotUpgraded(formattedReleaseName, instantiateInfo.getNamespace(), 2);

        // upgrade to lower version and verify skipped
        ResourceResponseSuccess upgradeResponseSuccess3 =
                performUpgrade(upgradeToLowerVersionConfig, formattedReleaseName);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(UPGRADE_TITLE, client, upgradeResponseSuccess3, COMPLETED);
        verifyAppNotUpgraded(formattedReleaseName, instantiateInfo.getNamespace(), 2);
    }

    private ResourceResponseSuccess performUpgrade(final String upgradeJson, final String formattedReleaseName) {
        String upgradeResponseBody = executePostRequest(UPGRADE_TITLE, client, formattedReleaseName, upgradeJson, UPGRADE);
        return getV3ResourceResponseSuccess(upgradeResponseBody);
    }

    @Test
    @DisplayName("Create a new secret with sample data, patch that secret value with new data")
    public void patchSecretWfsTest() throws IOException, ApiException {
        String namespacePrefix = "test12";
        String json = getJsonAsString("v3/wfsPatchSecret.json");

        String formattedNamespace = replaceNameSpace(json, namespacePrefix);
        JSONObject jsonReq = new JSONObject(formattedNamespace);
        String clusterConfigName = jsonReq.get("clusterName").toString();
        String namespace = jsonReq.get("namespace").toString();

        createNamespace(namespace, clusterConfigName);
        createSecret(TEST_SECRET, namespace, clusterConfigName, UNSEAL_KEY, "test-value");
        verifySecretKeyValue(clusterConfigName, namespace, TEST_SECRET, UNSEAL_KEY, "test-value");
        executePutRequest(PATCH_SECRET_TITLE, client, formattedNamespace, HOST_PATCH_SECRET + "/test-secret");
        verifySecretKeyValue(clusterConfigName, namespace, TEST_SECRET, UNSEAL_KEY, "updated-value");
    }

    @Test
    @DisplayName("v3 Install with overridden value, verify successful, scale out, and delete application")
    public void commonWfsScaleOutTest() {
        String namespacePrefix = "test13";

        String json = getJsonAsString("v3/wfsInstantiateInstance2.json");
        String formattedReleaseName = replaceChangeNumber("scaleout-spiderapp-gerritchangenumber");
        String formattedNamespace = replaceNameSpace(json, namespacePrefix);
        InstantiateInfo instantiateInfo = writeJsonStringToObject(formattedNamespace, InstantiateInfo.class);
        String deployResponseBody = executeInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace);
        ResourceResponseSuccess resourceResponseSuccess = getV3ResourceResponseSuccess(deployResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(INSTANTIATE_TITLE, client, resourceResponseSuccess, COMPLETED);
        verifyPodsWithTimeout(INSTANTIATE_TITLE, formattedReleaseName, client, 200, 6);
        verifyOverriddenValueSet(instantiateInfo, formattedReleaseName);

        String jsonScale = getJsonAsString("v3/wfsScaleInstance.json");
        String formattedScaleNamespace = replaceNameSpace(jsonScale, namespacePrefix);
        String scaleResponseBody = executePostRequest(SCALE_TITLE, client, formattedReleaseName, formattedScaleNamespace, "scale");
        ResourceResponseSuccess resourceResponseScaleSuccess = getV3ResourceResponseSuccess(scaleResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(SCALE_TITLE, client, resourceResponseScaleSuccess, COMPLETED);
        verifyAppScaled(formattedReleaseName, instantiateInfo.getNamespace());

        String deleteResponseBody = executeTerminateMultipartRequest(TERMINATE_TITLE, client, formattedReleaseName,
                                                                     instantiateInfo.getNamespace());
        ResourceResponseSuccess resourceResponseDeleteSuccess = getV3ResourceResponseSuccess(deleteResponseBody);
        verifyHistoryQueryByReleaseNameWithInstanceIdV3(TERMINATE_TITLE, client, resourceResponseDeleteSuccess, COMPLETED);
        verifyAppDeleted(formattedReleaseName);
    }

    @Test
    @DisplayName("V3 Install to EVNFM namespace fails")
    public void failedInstantiateToEvnfmNamespace() {
        String json = getJsonAsString(INSTANTIATE_TO_EVNFM_NAMESPACE_VNF_INSTANCE);
        String formattedReleaseName = replaceChangeNumber("wfs-accept-v2-spider-gerritchangenumber");
        String formattedNamespace = json.replaceAll("UNIQUE_NAME", WfsConfigurationEnum.NAMESPACE.getProperty());
        verifyFailedInstantiateRequest(INSTANTIATE_TITLE, client, formattedReleaseName, formattedNamespace,
                                       EVNFM_NAMESPACE_INSTANTIATION_ERROR, 400);
    }

    @Test
    @DisplayName("Verify helm client versions")
    public void getHelmClientVersions() {
        String response = executeGetHelmVersions(GET_HELM_VERSIONS_TITLE, client);
        HelmVersionsResponse helmVersionsResponse = getHelmVersionsResponseSuccess(response);
        verifyHelmVersions(helmVersionsResponse.getHelmVersions());
    }
}
