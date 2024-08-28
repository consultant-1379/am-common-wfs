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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.ericsson.amcommonwfs.presentation.services.idempotency.IdempotencyServiceImpl;
import org.camunda.bpm.engine.ProcessEngineException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.amcommonwfs.camunda.service.WorkflowInstanceServiceCamunda;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.presentation.converter.Converter;
import com.ericsson.amcommonwfs.presentation.converter.impl.InstantiateResourceConverterImpl;
import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.amcommonwfs.presentation.dto.TerminateInfoDto;
import com.ericsson.amcommonwfs.util.RestPayloadValidationUtils;
import com.ericsson.amcommonwfs.utils.ValuesFileService;
import com.ericsson.amcommonwfs.utils.repository.CamundaFileRepository;
import com.ericsson.amcommonwfs.utils.repository.FileService;
import com.ericsson.workflow.orchestration.mgmt.model.WorkFlowQueryMetaData;
import com.ericsson.workflow.orchestration.mgmt.model.WorkflowQueryResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;
import com.ericsson.workflow.orchestration.mgmt.model.v3.RollbackInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ScaleInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.UpgradeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {ResourceApiControllerImpl.class, ObjectMapper.class})
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
public class ResourceApiControllerImplTest {

    private static final String UPGRADE_TOP = "UpgradeApplication__top";
    private static final String SCALE_TOP = "ScaleApplication__top";
    private static final String RELEASE_NAME = "spider-app";
    private static final String INSTANCE_ID = "instanceId";
    private static final String IDEMPOTENCY_KEY = "dummyKey";

    @Autowired
    private ResourceApiControllerImpl resourceApiController;
    @MockBean
    private InstantiateResourceConverterImpl instantiateResourceConverter;
    @MockBean
    private WorkflowInstanceServiceCamunda workflowInstanceServiceCamunda;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private Converter<Map<String, Object>, ResourceInfoDto<UpgradeInfo>> upgradeResourceConverterImpl;
    @MockBean
    private Converter<Map<String, Object>, ResourceInfoDto<ScaleInfo>> scaleResourceConverterImpl;
    @MockBean
    private Converter<Map<String, Object>, ResourceInfoDto<RollbackInfo>> rollbackResourceConverterImpl;
    @MockBean
    private Converter<Map<String, Object>, TerminateInfoDto> terminateResourceConverterImpl;
    @MockBean
    private FileService temporaryFileServiceImpl;
    @MockBean
    private ClusterConfigService clusterConfigService;
    @MockBean
    private CryptoService cryptoService;
    @MockBean
    private ValuesFileService valuesFileService;
    @MockBean
    private CamundaFileRepository camundaFileRepository;
    @MockBean
    private IdempotencyServiceImpl  idempotencyService;

    @BeforeEach
    private void setup() {
        when(idempotencyService.executeTransactionalIdempotentCall(any())).thenCallRealMethod();
    }

    @Test
    public void shouldReturnAcceptedWhenInstantiateAppropriateV3Resource() {
        InstantiateInfo instantiateInfo = setupInstantiateInfo();
        Map<String, Object> instantiateVariables = setupInstantiateVariables();
        ResourceResponseSuccess responseSuccess = setupResourceResponseSuccess();

        when(instantiateResourceConverter.convert(any(ResourceInfoDto.class))).thenReturn(instantiateVariables);
        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(anyString(), eq(IDEMPOTENCY_KEY),
                                                                                                        eq(instantiateVariables))).thenReturn(responseSuccess);

        final ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.instantiateV3Resource(IDEMPOTENCY_KEY, RELEASE_NAME,
                                                                                                                   instantiateInfo);

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
    }

    @Test
    public void shouldReturnAcceptedWhenInstantiateV3ResourceWithAdditionalValues() {
        InstantiateInfo instantiateInfo = setupInstantiateInfo();
        Map<String, Object> instantiateVariables = setupInstantiateVariables();

        when(instantiateResourceConverter.convert(any(ResourceInfoDto.class))).thenReturn(instantiateVariables);
        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");
        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(anyString(),
                                                                                                        eq(IDEMPOTENCY_KEY),
                                                                                                        eq(instantiateVariables)))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(InstantiateInfo.class)))
                .thenReturn(instantiateInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.instantiateV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                valuesMultipartFileMock(),
                configMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldReturnAcceptedWhenInstantiateV3ResourceWithoutAdditionalValues() {
        InstantiateInfo instantiateInfo = setupInstantiateInfo();
        Map<String, Object> instantiateVariables = setupInstantiateVariables();

        when(instantiateResourceConverter.convert(any(ResourceInfoDto.class))).thenReturn(instantiateVariables);
        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");
        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(anyString(), eq(IDEMPOTENCY_KEY),
                                                                                                        eq(instantiateVariables)))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(InstantiateInfo.class)))
                .thenReturn(instantiateInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.instantiateV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                null,
                configMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldReturnAcceptedWhenUpgradeWithAppropriateReleaseNameAndUpgradeInfo() {
        UpgradeInfo upgradeInfo = setupUpgradeInfoData();
        Map<String, Object> variables = setupUpgradeVariables();
        ResourceResponseSuccess responseSuccess = setupResourceResponseSuccess();

        when(upgradeResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);
        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(UPGRADE_TOP, IDEMPOTENCY_KEY,
                                                                                                        variables)).thenReturn(responseSuccess);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.upgradeV3Resource(IDEMPOTENCY_KEY, RELEASE_NAME, upgradeInfo);

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
    }

    @Test
    public void shouldThrowErrorWhenUpgradeWithIncorrectUpgradeInfo() {
        UpgradeInfo upgradeInfo = setupUpgradeInfoData();
        Map<String, Object> variables = setupUpgradeVariables();

        when(upgradeResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);
        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(UPGRADE_TOP, IDEMPOTENCY_KEY,
                                                                                                        variables)).thenThrow(new ProcessEngineException());

        Assert.assertThrows(ProcessEngineException.class, () -> resourceApiController.upgradeV3Resource(IDEMPOTENCY_KEY, RELEASE_NAME, upgradeInfo));
    }

    @Test
    public void shouldReturnAcceptedWhenUpgradeV3ResourceWithAdditionalValues() {
        UpgradeInfo upgradeInfo = setupUpgradeInfoData();
        Map<String, Object> variables = setupUpgradeVariables();

        when(upgradeResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);

        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");

        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(UPGRADE_TOP, IDEMPOTENCY_KEY, variables))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(UpgradeInfo.class)))
                .thenReturn(upgradeInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.upgradeV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                valuesMultipartFileMock(),
                configMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldReturnAcceptedWhenUpgradeV3ResourceWithoutAdditionalValues() {
        UpgradeInfo upgradeInfo = setupUpgradeInfoData();
        Map<String, Object> variables = setupUpgradeVariables();

        when(upgradeResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);

        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");

        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(UPGRADE_TOP, IDEMPOTENCY_KEY, variables))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(UpgradeInfo.class)))
                .thenReturn(upgradeInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.upgradeV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                null,
                valuesMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldThrowErrorWhenScaleWithEmptyScaleResources() {
        Assert.assertThrows(IllegalArgumentException.class, () -> resourceApiController.scaleV3Resource(IDEMPOTENCY_KEY, RELEASE_NAME,
                                                                                                        new ScaleInfo()));
    }

    @Test
    public void shouldReturnAcceptedWhenScaleV3ResourceWithAdditionalValues() {
        ScaleInfo scaleInfo = setupScaleInfoData();
        Map<String, Object> variables = setupScaleVariables();

        when(scaleResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);

        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");

        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(SCALE_TOP, IDEMPOTENCY_KEY, variables))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(ScaleInfo.class)))
                .thenReturn(scaleInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.scaleV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                valuesMultipartFileMock(),
                valuesMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldReturnAcceptedWhenScaleV3ResourceWithoutAdditionalValues() {
        ScaleInfo scaleInfo = setupScaleInfoData();
        Map<String, Object> variables = setupScaleVariables();

        when(scaleResourceConverterImpl.convert(any(ResourceInfoDto.class))).thenReturn(variables);

        when(temporaryFileServiceImpl.saveFile(any(MultipartFile.class))).thenReturn(Paths.get("mock-path"));
        when(temporaryFileServiceImpl.readFileContentToString("mock-path")).thenReturn("mock-content");
        when(clusterConfigService.resolveClusterConfig("mock-cluster", configMultipartFileMock())).thenReturn("mock-content");
        when(cryptoService.encryptString(anyString())).thenReturn("mock-content");

        when(workflowInstanceServiceCamunda.startWorkflowInstanceByDefinitionKeyBusinessKeyAndVariables(SCALE_TOP, IDEMPOTENCY_KEY, variables))
                .thenReturn(setupResourceResponseSuccess());

        MockedStatic<RestPayloadValidationUtils> utils = Mockito.mockStatic(RestPayloadValidationUtils.class);

        utils.when(() -> RestPayloadValidationUtils.validateFileTypeAsPlainText(any()))
                .thenAnswer((Answer<Void>) invocation -> null);

        utils.when(() -> RestPayloadValidationUtils.validateJson(anyString(), eq(ScaleInfo.class)))
                .thenReturn(scaleInfo);

        ResponseEntity<ResourceResponseSuccess> responseEntity = resourceApiController.scaleV3Resource(
                IDEMPOTENCY_KEY,
                RELEASE_NAME,
                "mock-json",
                valuesMultipartFileMock(),
                null,
                valuesMultipartFileMock()
        );

        Assert.assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());

        utils.close();
    }

    @Test
    public void shouldThrowErrorWhenGetHistoryV3ResourceForAbsentResource() {
        WorkflowQueryResponse response = new WorkflowQueryResponse();
        response.setMetadata(new WorkFlowQueryMetaData());

        when(workflowInstanceServiceCamunda.getWorkflowHistoryByReleaseName(RELEASE_NAME, INSTANCE_ID)).thenReturn(response);

        Assert.assertThrows(NotFoundException.class, () -> resourceApiController.getHistoryV3Resource(RELEASE_NAME, INSTANCE_ID));
    }

    private static InstantiateInfo setupInstantiateInfo() {
        InstantiateInfo instantiateInfo = new InstantiateInfo();
        instantiateInfo.setNamespace("test-ns");
        instantiateInfo.setCleanUpResources(false);
        instantiateInfo.setChartName("spider-app");
        instantiateInfo.setChartType(InstantiateInfo.ChartTypeEnum.CRD);
        return instantiateInfo;
    }

    private static UpgradeInfo setupUpgradeInfoData() {
        UpgradeInfo upgradeInfo = new UpgradeInfo();
        upgradeInfo.setChartName("spider-app");
        upgradeInfo.setChartType(UpgradeInfo.ChartTypeEnum.CNF);
        upgradeInfo.setNamespace("test-ns");
        return upgradeInfo;
    }

    private static ScaleInfo setupScaleInfoData() {
        ScaleInfo scaleInfo = new ScaleInfo();
        scaleInfo.setChartName("spider-app");
        scaleInfo.setClusterName("mock-cluster");
        scaleInfo.setNamespace("test-ns");
        return scaleInfo;
    }

    private static ResourceResponseSuccess setupResourceResponseSuccess() {
        ResourceResponseSuccess responseSuccess = new ResourceResponseSuccess();
        responseSuccess.setInstanceId(INSTANCE_ID);
        responseSuccess.setReleaseName(RELEASE_NAME);
        return responseSuccess;
    }

    private static Map<String, Object> setupInstantiateVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("instanceId", INSTANCE_ID);
        variables.put("releaseName", RELEASE_NAME);
        return variables;
    }

    private static Map<String, Object> setupUpgradeVariables(){
        return setupInstantiateVariables();
    }

    private static Map<String, Object> setupScaleVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("releaseName", RELEASE_NAME);
        return variables;
    }

    private MultipartFile valuesMultipartFileMock() {
        return new MockMultipartFile("Mock file", "values.yaml", "text", new byte[]{});
    }

    private MultipartFile configMultipartFileMock() {
        return new MockMultipartFile("Mock file", "clusterConfig.config", "text", new byte[]{});
    }

}
