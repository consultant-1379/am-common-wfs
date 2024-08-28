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
package com.ericsson.amcommonwfs.presentation.controllers.v3;

import static com.ericsson.amcommonwfs.CommandType.CRD;
import static com.ericsson.amcommonwfs.CommandType.INSTANTIATE;
import static com.ericsson.amcommonwfs.CommandType.ROLLBACK;
import static com.ericsson.amcommonwfs.CommandType.SCALE;
import static com.ericsson.amcommonwfs.CommandType.TERMINATE;
import static com.ericsson.amcommonwfs.CommandType.UPGRADE;
import static com.ericsson.amcommonwfs.util.DefinitionKey.getProcessDefinitionKey;
import static com.ericsson.amcommonwfs.util.v3.ControllerUtilities.validateValuesFile;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY_PREFIX;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APPLICATION_TIME_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY_PREFIX;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY_PREFIX;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyService;
import com.ericsson.amcommonwfs.util.v3.ControllerUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.camunda.service.WorkflowInstanceServiceCamunda;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.exception.InvalidRequestParametersException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.exception.ParameterExceptionDetail;
import com.ericsson.amcommonwfs.infrastructure.DocumentController;
import com.ericsson.amcommonwfs.presentation.converter.Converter;
import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.amcommonwfs.presentation.dto.TerminateInfoDto;
import com.ericsson.amcommonwfs.util.RestPayloadValidationUtils;
import com.ericsson.amcommonwfs.utils.ValuesFileService;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.workflow.orchestration.mgmt.api.v3.LcmApi;
import com.ericsson.workflow.orchestration.mgmt.api.v3.LcmMultipartApi;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.ericsson.workflow.orchestration.mgmt.model.v3.RollbackInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;

@Validated
@DocumentController
@RestController
@RequestMapping("/api")
public class ResourceApiControllerImpl implements LcmApi, LcmMultipartApi {

    private static final String SELF_LINK = "self";
    private static final String INSTANCE_LINK = "instance";

    //Cluster config will be saved in redis two minutes after the timeout to finish timeout actions
    private static final long TIMEOUT_CONTINGENCY = 120L;

    @Autowired
    private Converter<Map<String, Object>, ResourceInfoDto<InstantiateInfo>> instantiateResourceConverterImpl;

    @Autowired
    private Converter<Map<String, Object>, ResourceInfoDto<UpgradeInfo>> upgradeResourceConverterImpl;

    @Autowired
    private Converter<Map<String, Object>, ResourceInfoDto<ScaleInfo>> scaleResourceConverterImpl;

    @Autowired
    private Converter<Map<String, Object>, ResourceInfoDto<RollbackInfo>> rollbackResourceConverterImpl;

    @Autowired
    private Converter<Map<String, Object>, TerminateInfoDto> terminateResourceConverterImpl;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;

    @Autowired
    private FileService temporaryFileServiceImpl;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private ValuesFileService valuesFileService;

    @Autowired
    private CamundaFileRepository camundaFileRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @Override
    public ResponseEntity<ResourceResponseSuccess> instantiateV3Resource(String idempotencyKey,
                                                                         String releaseName,
                                                                         final InstantiateInfo instantiateInfo) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> instantiateSupplier = () -> {
            final Map<String, Object> variables = instantiateResourceConverterImpl.convert(new ResourceInfoDto<>(instantiateInfo, releaseName));
            String commandType = InstantiateInfo.ChartTypeEnum.CRD.equals(instantiateInfo.getChartType())
                    ? CRD.getCommandType() : INSTANTIATE.getCommandType();
            return executeOperation(commandType, idempotencyKey, variables, httpServletRequest);
        };

        return idempotencyService.executeTransactionalIdempotentCall(instantiateSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> instantiateV3Resource(String idempotencyKey,
                                                                         String releaseName,
                                                                         String instantiateInfoJson,
                                                                         MultipartFile values,
                                                                         MultipartFile additionalValues,
                                                                         MultipartFile clusterConfig) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> instantiateSupplier = () -> {
            InstantiateInfo instantiateInfoObject = validateOperationParams(instantiateInfoJson, InstantiateInfo.class,
                    values, additionalValues, clusterConfig);
            Map<String, Object> variables = instantiateResourceConverterImpl.convert(new ResourceInfoDto<>(instantiateInfoObject, releaseName));

            String commandType = InstantiateInfo.ChartTypeEnum.CRD.equals(instantiateInfoObject.getChartType())
                    ? CRD.getCommandType() : INSTANTIATE.getCommandType();

            return executeOperationWithFiles(commandType, idempotencyKey, variables, httpServletRequest, clusterConfig,
                    values, additionalValues, instantiateInfoObject.getClusterName());
        };

        return idempotencyService.executeTransactionalIdempotentCall(instantiateSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> upgradeV3Resource(String idempotencyKey,
                                                                     final String releaseName,
                                                                     final UpgradeInfo upgradeInfo) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> upgradeSupplier = () -> {
            final Map<String, Object> variables = upgradeResourceConverterImpl.convert(new ResourceInfoDto<>(upgradeInfo, releaseName));
            String commandType = UpgradeInfo.ChartTypeEnum.CRD.equals(upgradeInfo.getChartType())
                    ? CRD.getCommandType() : UPGRADE.getCommandType();

            return executeOperation(commandType, idempotencyKey, variables, httpServletRequest);
        };

        return idempotencyService.executeTransactionalIdempotentCall(upgradeSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> upgradeV3Resource(String idempotencyKey,
                                                                     String releaseName,
                                                                     String upgradeInfoJson,
                                                                     MultipartFile values,
                                                                     MultipartFile additionalValues,
                                                                     MultipartFile clusterConfig) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> upgradeSupplier = () -> {
            UpgradeInfo upgradeInfo = validateOperationParams(upgradeInfoJson, UpgradeInfo.class, values, additionalValues, clusterConfig);
            Map<String, Object> variables = upgradeResourceConverterImpl.convert(new ResourceInfoDto<>(upgradeInfo, releaseName));

            String commandType = UpgradeInfo.ChartTypeEnum.CRD.equals(upgradeInfo.getChartType())
                    ? CRD.getCommandType() : UPGRADE.getCommandType();

            return executeOperationWithFiles(commandType, idempotencyKey, variables, httpServletRequest,
                    clusterConfig, values, additionalValues, upgradeInfo.getClusterName());
        };

        return idempotencyService.executeTransactionalIdempotentCall(upgradeSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> scaleV3Resource(String idempotencyKey,
                                                                   final String releaseName,
                                                                   final ScaleInfo scaleInfo) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> scaleSupplier = () -> {
            if (CollectionUtils.isEmpty(scaleInfo.getScaleResources())) {
                throw new IllegalArgumentException("ScaleResources can not be empty");
            }
            final Map<String, Object> variables = scaleResourceConverterImpl.convert(new ResourceInfoDto<>(scaleInfo, releaseName));
            return executeOperation(SCALE.getCommandType(), idempotencyKey, variables, httpServletRequest);
        };

        return idempotencyService.executeTransactionalIdempotentCall(scaleSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> scaleV3Resource(String idempotencyKey,
                                                                   String releaseName,
                                                                   String scaleInfoJson,
                                                                   MultipartFile values,
                                                                   MultipartFile additionalValues,
                                                                   MultipartFile clusterConfig) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> scaleSupplier = () -> {
            ScaleInfo scaleInfo = validateOperationParams(scaleInfoJson, ScaleInfo.class, values, additionalValues, clusterConfig);
            Map<String, Object> variables = scaleResourceConverterImpl.convert(new ResourceInfoDto<>(scaleInfo, releaseName));

            return executeOperationWithFiles(SCALE.getCommandType(), idempotencyKey, variables, httpServletRequest, clusterConfig,
                    values, additionalValues, scaleInfo.getClusterName());
        };
        return idempotencyService.executeTransactionalIdempotentCall(scaleSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> rollbackV3Resource(String idempotencyKey,
                                                                      final String releaseName,
                                                                      final RollbackInfo rollbackInfo) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> rollbackSupplier = () -> {
            final Map<String, Object> variables = rollbackResourceConverterImpl.convert(new ResourceInfoDto<>(rollbackInfo, releaseName));
            return executeOperation(ROLLBACK.getCommandType(), idempotencyKey, variables, httpServletRequest);
        };

        return idempotencyService.executeTransactionalIdempotentCall(rollbackSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> rollbackV3Resource(String idempotencyKey,
                                                                      final String releaseName,
                                                                      String rollbackInfoJson,
                                                                      MultipartFile clusterConfig) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> rollbackSupplier = () -> {
            RollbackInfo rollbackInfo = RestPayloadValidationUtils.validateJson(rollbackInfoJson, RollbackInfo.class);
            final Map<String, Object> variables = rollbackResourceConverterImpl.convert(new ResourceInfoDto<>(rollbackInfo, releaseName));
            return executeOperationWithFiles(ROLLBACK.getCommandType(), idempotencyKey, variables, httpServletRequest,
                    clusterConfig, null, null, rollbackInfo.getClusterName());
        };

        return idempotencyService.executeTransactionalIdempotentCall(rollbackSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> terminateV3Resource(String idempotencyKey,
                                                                       final String releaseName,
                                                                       final String lifecycleOperationId,
                                                                       final String state,
                                                                       final String namespace,
                                                                       final String applicationTimeOut,
                                                                       final Boolean skipVerification,
                                                                       final Boolean cleanUpResources,
                                                                       final String clusterName,
                                                                       final Boolean skipJobVerification,
                                                                       final String helmClientVersion) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> terminateSupplier = () -> {
            TerminateInfoDto terminateInfoDto = new TerminateInfoDto();
            terminateInfoDto.setReleaseName(releaseName);
            terminateInfoDto.setLifecycleOperationId(lifecycleOperationId);
            terminateInfoDto.setState(state);
            terminateInfoDto.setNamespace(namespace);
            terminateInfoDto.setApplicationTimeOut(applicationTimeOut);
            terminateInfoDto.setSkipVerification(skipVerification);
            terminateInfoDto.setCleanUpResources(cleanUpResources);
            terminateInfoDto.setClusterName(clusterName);
            terminateInfoDto.setSkipJobVerification(skipJobVerification);
            terminateInfoDto.setHelmClientVersion(helmClientVersion);

            final Map<String, Object> variables = terminateResourceConverterImpl.convert(terminateInfoDto);
            return executeOperation(TERMINATE.getCommandType(), idempotencyKey, variables, httpServletRequest);
        };

        return idempotencyService.executeTransactionalIdempotentCall(terminateSupplier);
    }

    @Override
    public ResponseEntity<ResourceResponseSuccess> terminateV3Resource(String idempotencyKey,
                                                                       final String releaseName,
                                                                       final String lifecycleOperationId,
                                                                       final String state,
                                                                       final String namespace,
                                                                       final String applicationTimeOut,
                                                                       final Boolean skipVerification,
                                                                       final Boolean cleanUpResources,
                                                                       final String clusterName,
                                                                       final Boolean skipJobVerification,
                                                                       final String helmClientVersion,
                                                                       MultipartFile clusterConfig) {
        Supplier<ResponseEntity<ResourceResponseSuccess>> terminateSupplier = () -> {
            TerminateInfoDto terminateInfoDto = new TerminateInfoDto();
            terminateInfoDto.setReleaseName(releaseName);
            terminateInfoDto.setLifecycleOperationId(lifecycleOperationId);
            terminateInfoDto.setState(state);
            terminateInfoDto.setNamespace(namespace);
            terminateInfoDto.setApplicationTimeOut(applicationTimeOut);
            terminateInfoDto.setSkipVerification(skipVerification);
            terminateInfoDto.setCleanUpResources(cleanUpResources);
            terminateInfoDto.setClusterName(clusterName);
            terminateInfoDto.setSkipJobVerification(skipJobVerification);
            terminateInfoDto.setHelmClientVersion(helmClientVersion);

            final Map<String, Object> variables = terminateResourceConverterImpl.convert(terminateInfoDto);
            return executeOperationWithFiles(TERMINATE.getCommandType(), idempotencyKey, variables, httpServletRequest, clusterConfig,
                    null, null, clusterName);
        };

        return idempotencyService.executeTransactionalIdempotentCall(terminateSupplier);
    }

    @Override
    public ResponseEntity<Object> getHistoryV3Resource(final String releaseName, final String instanceId) {
        WorkflowQueryResponse resourceHistoryByReleaseName = workflowInstanceServiceCamunda
                .getWorkflowHistoryByReleaseName(releaseName, instanceId);
        if (resourceHistoryByReleaseName.getMetadata().getCount() == 0) {
            throw new NotFoundException("Resource not found");
        }
        return new ResponseEntity<>(resourceHistoryByReleaseName, HttpStatus.OK);
    }

    private static <T> T validateOperationParams(String json, Class<T> infoClass,
                                                 MultipartFile values, MultipartFile additionalValues, MultipartFile clusterConfig) {
        if (values == null && clusterConfig == null) {
            ParameterExceptionDetail valuesExceptionDetail = new ParameterExceptionDetail();
            valuesExceptionDetail.setParameterName("values");
            valuesExceptionDetail.setMessage("Required request part 'values' is not present");

            ParameterExceptionDetail clusterConfigExceptionDetail = new ParameterExceptionDetail();
            clusterConfigExceptionDetail.setParameterName("clusterConfig");
            clusterConfigExceptionDetail.setMessage("Required request part 'clusterConfig' is not present");

            throw new InvalidRequestParametersException(List.of(valuesExceptionDetail, clusterConfigExceptionDetail));
        }

        if (values != null) {
            validateValuesFile(values);
        }

        if (additionalValues != null) {
            validateValuesFile(additionalValues);
        }

        return RestPayloadValidationUtils.validateJson(json, infoClass);
    }

    private ResponseEntity<ResourceResponseSuccess> executeOperationWithFiles(String definitionKey, String businessKey, Map<String, Object> variables,
                                                                              HttpServletRequest httpServletRequest, MultipartFile clusterConfig,
                                                                              MultipartFile valuesFile, MultipartFile additionalValuesFile,
                                                                              String clusterName) {
        setValuesFileParameters(valuesFile, false, variables);
        setValuesFileParameters(additionalValuesFile, true, variables);
        setConfigFileParameter(clusterName, clusterConfig, variables);

        return executeOperation(definitionKey, businessKey, variables, httpServletRequest);
    }

    private void setValuesFileParameters(MultipartFile valuesFile, boolean isAdditional, Map<String, Object> variables) {

        String valuesContentVarKey = isAdditional ? ADDITIONAL_VALUES_FILE_CONTENT_KEY : VALUES_FILE_CONTENT_KEY;
        String valuesContentVarKeyPrefix = isAdditional ? ADDITIONAL_VALUES_FILE_CONTENT_KEY_PREFIX : VALUES_FILE_CONTENT_KEY_PREFIX;

        if (valuesFile != null) {
            String valuesFileContent = temporaryFileServiceImpl.readMultipartFileContent(valuesFile);
            if (valuesFileContent != null) {
                String encryptValuesFileContent = cryptoService.encryptString(valuesFileContent);
                String valuesUUID = valuesContentVarKeyPrefix + "-" + UUID.randomUUID();
                variables.put(valuesContentVarKey, valuesUUID); // NOSONAR
                long timeoutInSec = Long.parseLong(variables.get(APPLICATION_TIME_OUT).toString());
                camundaFileRepository.save(valuesUUID, encryptValuesFileContent.getBytes(StandardCharsets.UTF_8), timeoutInSec);
            }
        }
    }

    private void setConfigFileParameter(String clusterName, MultipartFile clusterConfig, Map<String, Object> variables) {
        variables.put(ORIGINAL_CLUSTER_NAME, clusterConfig == null ? clusterName : clusterConfig.getOriginalFilename());
        if (clusterConfig != null) {
            String clusterFileContent = temporaryFileServiceImpl.readMultipartFileContent(clusterConfig);
            if (clusterFileContent != null) {
                String encryptCusterConfigContent = cryptoService.encryptString(clusterFileContent);
                String clusterUUID = CLUSTER_CONFIG_CONTENT_KEY_PREFIX + "-" + UUID.randomUUID();
                variables.put(CLUSTER_CONFIG_CONTENT_KEY, clusterUUID); // NOSONAR
                long timeoutInSec = Long.parseLong(variables.get(APPLICATION_TIME_OUT).toString()) + TIMEOUT_CONTINGENCY;
                camundaFileRepository.save(clusterUUID, encryptCusterConfigContent.getBytes(StandardCharsets.UTF_8), timeoutInSec);
            }
        }
    }

    private ResponseEntity<ResourceResponseSuccess> executeOperation(String definitionKey,
                                                                     String businessKey,
                                                                     Map<String, Object> variables, HttpServletRequest httpServletRequest) {
        ResourceResponseSuccess resourceResponseSuccess = workflowInstanceServiceCamunda
                .startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(getProcessDefinitionKey(definitionKey),
                                                                             businessKey,
                                                                             variables);
        return ControllerUtilities.buildOperationResponse(resourceResponseSuccess, httpServletRequest);
    }
}
