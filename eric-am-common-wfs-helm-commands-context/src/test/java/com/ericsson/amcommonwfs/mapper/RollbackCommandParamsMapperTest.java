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
package com.ericsson.amcommonwfs.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amcommonwfs.util.TestConstants.EXECUTION_VARIABLES_BASE;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_BASE_PARAMS;
import static com.ericsson.amcommonwfs.util.TestConstants.EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE;
import static com.ericsson.amcommonwfs.util.TestConstants.REVISION_NUMBER_VALUE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.MAX_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;
import static com.ericsson.amcommonwfs.utils.error.ErrorCode.BPMN_ROLLBACK_FAILED;

import java.util.Map;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amcommonwfs.model.CommandType;
import com.ericsson.amcommonwfs.util.MapperUtils;

@SpringBootTest(classes = {
        RollbackCommandParamsMapper.class
})
@TestPropertySource(properties = {
        "docker.registry.url=testRegistryUrl",
        "app.command.execute.defaultTimeOut=300",
        "containerRegistry.global.registry.pullSecret=regcred",
        "helm.debug.enabled=false",
        "applyDeprecatedDesignRules=true",
        "helmExecutor.caFilePath=mnt/cacert/ca.crt"
})
public class RollbackCommandParamsMapperTest {

    @Spy
    private ExecutionImpl execution;

    @Autowired
    private RollbackCommandParamsMapper rollbackCommandParamsMapper;

    @Test
    public void verifyInstallCommandTypeAndErrorCode() {
        assertThat(rollbackCommandParamsMapper.getType()).isEqualTo(CommandType.ROLLBACK);
        assertThat(rollbackCommandParamsMapper.getErrorCode()).isEqualTo(BPMN_ROLLBACK_FAILED);
    }

    @Test
    public void verifyRollbackCommandBase() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_BASE, execution);

        Map<String, Object> actualCommandParams = rollbackCommandParamsMapper.apply(execution);

        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS);
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((String) actualCommandParams.get(REVISION_NUMBER)).isEqualTo(REVISION_NUMBER_VALUE);
        assertThat(actualCommandParams.size()).isEqualTo(7);
    }

    @Test
    public void verifyRollbackCommandParamsBaseHelmDebugTrue() {
        MapperUtils.setVariables(EXECUTION_VARIABLES_BASE, execution);
        rollbackCommandParamsMapper.setHelmDebug(true);

        Map<String, Object> actualCommandParams = rollbackCommandParamsMapper.apply(execution);

        assertThat(actualCommandParams).containsAllEntriesOf(EXPECTED_BASE_PARAMS_HELM_DEBUG_TRUE);
        assertThat((int) actualCommandParams.get(MAX_HISTORY)).isZero();
        assertThat((String) actualCommandParams.get(REVISION_NUMBER)).isEqualTo(REVISION_NUMBER_VALUE);
        assertThat(actualCommandParams.size()).isEqualTo(7);
    }
}
