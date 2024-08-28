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
package com.ericsson.amcommonwfs.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amcommonwfs.VerifyTaskConstants.IS_ANNOTATED;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMED_OUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.APP_TIMEOUT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.service.CryptoService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VerifyApplicationDeployed.class})
public class VerifyApplicationDeployedTest {

    private static final long timeOut = 100L;

    @Autowired
    private VerifyApplicationDeployed verifyApplicationDeployed;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private VerifyExecution verifyExecution;

    @MockBean
    private VerificationHelper verificationHelper;

    private ExecutionImpl execution = new ExecutionImpl();

    @Test
    public void testExecuteWithTimeOut() {
        long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        execution.setVariable(APP_TIMEOUT, currentTime);
        when(verifyExecution.getDefinitionKey(any())).thenReturn(UPGRADE_DEFINITION_KEY);

        verifyApplicationDeployed.execute(execution);

        assertThat(execution.getVariable(APP_TIMED_OUT)).isEqualTo(true);
    }

    @Test
    public void testExecuteWithUpgradeDefinitionKey() {
        execution.setVariable(APP_TIMEOUT, timeOut);
        when(verifyExecution.getDefinitionKey(any())).thenReturn(UPGRADE_DEFINITION_KEY);

        verifyApplicationDeployed.execute(execution);

        verify(verificationHelper, times(1)).verifyApplicationDeployed(any());
    }

    @Test
    public void testExecuteWithScaleDefinitionKey() {
        execution.setVariable(APP_TIMEOUT, timeOut);
        when(verifyExecution.getDefinitionKey(any())).thenReturn(SCALE_DEFINITION_KEY);

        verifyApplicationDeployed.execute(execution);

        verify(verificationHelper, times(1)).verifyApplicationDeployed(any());
    }

    @Test
    public void testExecuteWithRollbackDefinitionKey() {
        execution.setVariable(APP_TIMEOUT, timeOut);
        execution.setVariable(IS_ANNOTATED, true);
        when(verifyExecution.getDefinitionKey(any())).thenReturn(ROLLBACK_DEFINITION_KEY);

        verifyApplicationDeployed.execute(execution);

        verify(verificationHelper, times(1)).verifyApplicationDeployedUsingAnnotation(any());
    }
}