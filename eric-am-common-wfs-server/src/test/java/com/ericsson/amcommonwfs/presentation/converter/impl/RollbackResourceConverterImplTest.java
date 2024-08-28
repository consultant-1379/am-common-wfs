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
package com.ericsson.amcommonwfs.presentation.converter.impl;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_JOB_VERIFICATION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SKIP_VERIFICATION;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.presentation.dto.ResourceInfoDto;
import com.ericsson.workflow.orchestration.mgmt.model.v3.RollbackInfo;

public class RollbackResourceConverterImplTest extends AbstractResourceConverterTest<ResourceInfoDto<RollbackInfo>> {

    private final RollbackResourceConverterImpl rollbackResourceConverterImpl =
            new RollbackResourceConverterImpl(CLUSTER_CONFIG_DIR, DEFAULT_COMMAND_TIME_OUT);

    private ResourceInfoDto<RollbackInfo> testResourceInfo;

    @BeforeEach
    public void setUp() {
        testResourceInfo = testResourceDtoFactory.createTestRollbackInfo();
    }

    @Test
    public void shouldNotSetEmptyValues() {
        RollbackInfo rollbackInfo = testResourceInfo.getInfo();
        rollbackInfo.setClusterName(null);

        assertEmptyValuesNotIncluded(testResourceInfo, rollbackResourceConverterImpl,
                Lists.newArrayList(CLUSTER_NAME));
    }

    @Test
    public void shouldSetDefaultParameters() {
        RollbackInfo rollbackInfo = testResourceInfo.getInfo();

        rollbackInfo.setSkipVerification(null);
        rollbackInfo.setSkipJobVerification(null);
        rollbackInfo.setHelmClientVersion(null);

        Map<String, Object> expectedDefaultValues = new HashMap<>();
        expectedDefaultValues.put(SKIP_VERIFICATION, false);
        expectedDefaultValues.put(SKIP_JOB_VERIFICATION, false);
        expectedDefaultValues.put(HELM_CLIENT_VERSION, "helm");

        assertDefaultValuesPresent(testResourceInfo, rollbackResourceConverterImpl, expectedDefaultValues);
    }

    @Test
    public void shouldFormatClusterConfig() {
        RollbackInfo rollbackInfo = testResourceInfo.getInfo();

        assertClusterConfigFormatted(testResourceInfo, rollbackInfo.getClusterName(), rollbackResourceConverterImpl);
    }

    @Test
    public void shouldFormatHelmClientVersion() {
        RollbackInfo rollbackInfo = testResourceInfo.getInfo();

        assertHelmClientVersionFormatted(testResourceInfo, rollbackInfo.getHelmClientVersion(), rollbackResourceConverterImpl);
    }
}