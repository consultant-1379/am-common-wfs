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
package com.ericsson.amcommonwfs.presentation.controllers;

import static com.ericsson.amcommonwfs.util.Utility.checkClusterFileExists;

import java.util.List;
import java.util.function.Supplier;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.model.ClusterConfigFileContext;
import com.ericsson.amcommonwfs.presentation.services.AbstractRequestCommandJobService;
import com.ericsson.amcommonwfs.presentation.services.HelmService;
import com.ericsson.amcommonwfs.presentation.services.KubectlService;
import com.ericsson.amcommonwfs.util.RestPayloadValidationUtils;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.workflow.orchestration.mgmt.api.v3.InternalApi;
import com.ericsson.workflow.orchestration.mgmt.api.v3.InternalMultipartApi;
import com.ericsson.workflow.orchestration.mgmt.api.v3.InternalResourceControllerApi;
import com.ericsson.workflow.orchestration.mgmt.model.HelmVersionsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfo;
import com.ericsson.workflow.orchestration.mgmt.model.KubernetesResourceInfoList;
import com.ericsson.workflow.orchestration.mgmt.model.NamespaceValidationResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponse;
import com.ericsson.workflow.orchestration.mgmt.model.PodStatusResponseList;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InternalScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.SecretInfo;

@RestController
@RequestMapping("/api")
@Validated
public class InternalResourceControllerImpl implements InternalApi, InternalMultipartApi, InternalResourceControllerApi {

    @Autowired
    private KubectlService kubectlService;

    @Autowired
    private HelmService helmService;

    @Value("${cluster.config.directory}")
    private String clusterConfigDir;

    @Value("${app.command.execute.defaultTimeOut}")
    private String defaultTimeOut;

    @Autowired
    private FileService temporaryFileService;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private AbstractRequestCommandJobService<AsyncDeleteNamespaceRequestDetails> deleteNamespaceCommandJobService;

    @Autowired
    private AbstractRequestCommandJobService<AsyncDeletePvcsRequestDetails> deletePvcsCommandJobService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Override
    public ResponseEntity<PodStatusResponse> getPodStatus(final String releaseName, final String clusterName) {
        String clusterConfig = checkClusterFileExists(clusterName, clusterConfigDir);
        PodStatusResponse podStatusResponse = kubectlService.getPodStatusByReleaseName(releaseName, clusterConfig);
        return new ResponseEntity<>(podStatusResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PodStatusResponse> getPodStatusLegacy(final String releaseName,
                                                                final String clusterName,
                                                                MultipartFile clusterConfig) {
        PodStatusResponse podStatusResponse =
                kubectlService.getPodStatusByReleaseName(releaseName, clusterConfigService.resolveClusterConfig(clusterName, clusterConfig));
        return new ResponseEntity<>(podStatusResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PodStatusResponseList> getPodStatusWithMultipart(String clusterName,
                                                                           MultipartFile clusterConfig,
                                                                           String releaseNamesJson) {
        List<String> releaseNames = RestPayloadValidationUtils.validateReleaseNames(releaseNamesJson);
        final String cluster = clusterConfigService.resolveClusterConfig(clusterName, clusterConfig);
        final List<PodStatusResponse> podStatusResponses = kubectlService.getPodStatusByReleaseNames(releaseNames, cluster);
        podStatusResponses.forEach(podStatusResponse -> podStatusResponse.setClusterName(clusterName));
        PodStatusResponseList podStatusResponseList = new PodStatusResponseList(podStatusResponses);
        return new ResponseEntity<>(podStatusResponseList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<KubernetesResourceInfoList> getResourceStatus(String clusterName,
                                                                        MultipartFile clusterConfig,
                                                                        String releaseNamesJson) {
        List<String> releaseNames = RestPayloadValidationUtils.validateReleaseNames(releaseNamesJson);
        final String cluster = clusterConfigService.resolveClusterConfig(clusterName, clusterConfig);
        List<KubernetesResourceInfo> kubernetesResourceInfos = kubectlService.getKubernetesResourceStatusInfoByReleaseNames(releaseNames, cluster);
        kubernetesResourceInfos.forEach(kubernetesResourceInfo -> kubernetesResourceInfo.setClusterName(clusterName));
        KubernetesResourceInfoList kubernetesResourceInfoList = new KubernetesResourceInfoList(kubernetesResourceInfos);
        return new ResponseEntity<>(kubernetesResourceInfoList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<HelmVersionsResponse> getSupportedHelmVersions() {
        HelmVersionsResponse helmVersionsResponse = helmService.getHelmVersions();
        return new ResponseEntity<>(helmVersionsResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ClusterServerDetailsResponse> clusterConfigFileValidate(final MultipartFile clusterConfig) {
        ClusterServerDetailsResponse response = clusterConfigService.checkIfConfigFileValid(clusterConfig);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<NamespaceValidationResponse> validateNamespace(String namespace,
                                                                         String clusterName,
                                                                         MultipartFile clusterConfig) {
        final String cluster = clusterConfigService.resolveClusterConfig(clusterName, clusterConfig);
        final NamespaceValidationResponse namespaceValidationResponse = kubectlService
                .isNamespaceUsedForEvnfmDeployment(namespace, cluster);
        return new ResponseEntity<>(namespaceValidationResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteNamespace(final String idempotencyKey,
                                                final String namespace,
                                                final String releaseName,
                                                final String clusterName,
                                                final String lifecycleOperationId,
                                                final String applicationTimeOut) {
        Supplier<ResponseEntity<Void>> deleteNamespaceSupplier = () ->
                deleteNamespace(idempotencyKey,
                        namespace,
                        clusterName,
                        releaseName,
                        applicationTimeOut,
                        lifecycleOperationId,
                        null);

        return idempotencyService.executeTransactionalIdempotentCall(deleteNamespaceSupplier);
    }

    @Override
    public ResponseEntity<Void> deleteNamespace(final String idempotencyKey,
                                                final String namespace,
                                                final String clusterName,
                                                final String releaseName,
                                                final String applicationTimeOut,
                                                final String lifecycleOperationId,
                                                MultipartFile clusterConfig) {
        Supplier<ResponseEntity<Void>> deleteNamespaceSupplier = () -> {
            String timeOut = applicationTimeOut != null ? applicationTimeOut : defaultTimeOut;

            AsyncDeleteNamespaceRequestDetails deleteNamespaceRequestDetails = new AsyncDeleteNamespaceRequestDetails(timeOut);
            deleteNamespaceRequestDetails.setNamespace(namespace);
            deleteNamespaceRequestDetails.setLifecycleOperationId(lifecycleOperationId);
            deleteNamespaceRequestDetails.setReleaseName(releaseName);
            deleteNamespaceRequestDetails.setIdempotencyKey(idempotencyKey);

            ClusterConfigFileContext clusterConfigFileContext = clusterConfigService
                    .resolveClusterConfigContext(clusterName, clusterConfig);
            deleteNamespaceRequestDetails.setClusterConfigFileContext(clusterConfigFileContext);

            deleteNamespaceCommandJobService.submitRequest(deleteNamespaceRequestDetails);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deleteNamespaceSupplier);
    }

    @Override
    public ResponseEntity<Void> scaleDown(String idempotencyKey, InternalScaleInfo internalScaleInfo) {
        Supplier<ResponseEntity<Void>> scaleDownSupplier = () -> {
            String clusterName = internalScaleInfo.getClusterName();
            String clusterConfig = checkClusterFileExists(clusterName, clusterConfigDir);
            String namespace = internalScaleInfo.getNamespace();
            if (!kubectlService.doesNamespaceExist(namespace, clusterConfig)) {
                throw new NotFoundException(String.format("Namespace %s not found", namespace));
            }
            kubectlService.scaleDownResources(
                    clusterConfigService.resolveClusterConfig(internalScaleInfo.getClusterName(), null),
                internalScaleInfo, idempotencyKey
            );
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(scaleDownSupplier);
    }

    @Override
    public ResponseEntity<Void> scaleDown(String idempotencyKey,
                                          MultipartFile clusterConfig,
                                          String internalScaleInfoJson) {
        Supplier<ResponseEntity<Void>> scaleDownSupplier = () -> {
            InternalScaleInfo internalScaleInfo = RestPayloadValidationUtils.validateJson(internalScaleInfoJson, InternalScaleInfo.class);
            String clusterConfigFilePath = clusterConfigService.resolveClusterConfig(internalScaleInfo.getClusterName(), clusterConfig);
            kubectlService.scaleDownResources(clusterConfigFilePath, internalScaleInfo, idempotencyKey);

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(scaleDownSupplier);
    }

    @Override
    public ResponseEntity<Void> deletePvcs(final String idempotencyKey,
                                           final String releaseName,
                                           final String lifecycleOperationId,
                                           final String state,
                                           final String namespace,
                                           final String applicationTimeOut,
                                           final String clusterName,
                                           List<String> labels) {
        Supplier<ResponseEntity<Void>> deletePvcsSupplier = () ->
                deletePvcs(idempotencyKey, releaseName, lifecycleOperationId, state, namespace, applicationTimeOut,
                        clusterName, labels, null);

        return idempotencyService.executeTransactionalIdempotentCall(deletePvcsSupplier);
    }

    @Override
    public ResponseEntity<Void> deletePvcs(final String idempotencyKey,
                                           final String releaseName,
                                           final String lifecycleOperationId,
                                           final String state,
                                           final String namespace,
                                           final String applicationTimeOut,
                                           final String clusterName,
                                           List<String> labels,
                                           MultipartFile clusterConfig) {
        Supplier<ResponseEntity<Void>> deletePvcsSupplier = () -> {
            AsyncDeletePvcsRequestDetails asyncDeletePvcsRequestDetails = new AsyncDeletePvcsRequestDetails(applicationTimeOut, labels);

            asyncDeletePvcsRequestDetails.setNamespace(namespace);
            asyncDeletePvcsRequestDetails.setLifecycleOperationId(lifecycleOperationId);
            asyncDeletePvcsRequestDetails.setReleaseName(releaseName);
            asyncDeletePvcsRequestDetails.setIdempotencyKey(idempotencyKey);
            ClusterConfigFileContext clusterConfigFileContext = clusterConfigService
                    .resolveClusterConfigContext(clusterName, clusterConfig);
            asyncDeletePvcsRequestDetails.setClusterConfigFileContext(clusterConfigFileContext);

            deletePvcsCommandJobService.submitRequest(asyncDeletePvcsRequestDetails);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deletePvcsSupplier);
    }

    @Override
    public ResponseEntity<Object> getSecretsInNamespace(String namespace,
                                                        String clusterName,
                                                        String fetchTimeOut) {
        String clusterConfig = checkClusterFileExists(clusterName, clusterConfigDir);
        return new ResponseEntity<>(kubectlService.getAllSecretInTheNamespace(namespace, clusterConfig, fetchTimeOut), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getSecretsInNamespaceWithMultipart(String namespace,
                                                                     String clusterName,
                                                                     String fetchTimeOut,
                                                                     MultipartFile clusterConfig) {
        String clusterConfigFile = clusterConfigService.resolveClusterConfig(clusterName, clusterConfig);
        return new ResponseEntity<>(kubectlService.getAllSecretInTheNamespace(namespace, clusterConfigFile, fetchTimeOut), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getValuesInRelease(final String releaseName, final String namespace,
                                                     final String clusterName, String fetchTimeOut) {
        String clusterConfig = checkClusterFileExists(clusterName, clusterConfigDir);
        return new ResponseEntity<>(helmService.getValues(releaseName, clusterConfig, namespace, fetchTimeOut),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getValuesInReleaseWithMultipart(String releaseName,
                                                                  String namespace,
                                                                  String clusterName,
                                                                  String fetchTimeOut,
                                                                  MultipartFile clusterConfig) {
        String clusterConfigFile = clusterConfigService.resolveClusterConfig(clusterName, clusterConfig);
        return new ResponseEntity<>(helmService.getValues(releaseName, clusterConfigFile, namespace, fetchTimeOut),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> patchSecretInNamespace(final String secretName, final SecretInfo secretInfo) {
        kubectlService.patchSecretInNamespace(secretName, secretInfo);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> patchSecretInNamespace(String secretName,
                                                       MultipartFile clusterConfig,
                                                       String secretInfoJson) {
        SecretInfo secretInfo = RestPayloadValidationUtils.validateJson(secretInfoJson, SecretInfo.class);
        String clusterConfigFile = clusterConfigService.resolveClusterConfig(secretInfo.getClusterName(), clusterConfig);
        secretInfo.setClusterName(clusterConfigFile);
        kubectlService.patchSecretInNamespace(secretName, secretInfo);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
