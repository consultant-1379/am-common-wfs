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

import com.ericsson.amcommonwfs.presentation.dto.TerminateInfoDto;

public class TerminateResourceConverterImplTest extends AbstractResourceConverterTest<TerminateInfoDto> {

    private final TerminateResourceConverterImpl terminateResourceConverterImpl =
            new TerminateResourceConverterImpl(CLUSTER_CONFIG_DIR, DEFAULT_COMMAND_TIME_OUT);

    private TerminateInfoDto testTerminateInfoDto;

    @BeforeEach
    public void setUp() {
        testTerminateInfoDto = testResourceDtoFactory.createTestTerminateInfoDto();
    }

    @Test
    public void shouldNotSetEmptyValues() {
        testTerminateInfoDto.setClusterName(null);

        assertEmptyValuesNotIncluded(testTerminateInfoDto, terminateResourceConverterImpl,
                Lists.newArrayList(CLUSTER_NAME));
    }

    @Test
    public void shouldSetDefaultParameters() {
        testTerminateInfoDto.setCleanUpResources(null);
        testTerminateInfoDto.setSkipVerification(null);
        testTerminateInfoDto.setSkipJobVerification(null);
        testTerminateInfoDto.setHelmClientVersion(null);

        Map<String, Object> expectedDefaultValues = new HashMap<>();
        expectedDefaultValues.put(SKIP_VERIFICATION, false);
        expectedDefaultValues.put(SKIP_JOB_VERIFICATION, false);
        expectedDefaultValues.put(HELM_CLIENT_VERSION, "helm");

        assertDefaultValuesPresent(testTerminateInfoDto, terminateResourceConverterImpl, expectedDefaultValues);
    }

    @Test
    public void shouldFormatClusterConfig() {
        assertClusterConfigFormatted(testTerminateInfoDto, testTerminateInfoDto.getClusterName(),
                terminateResourceConverterImpl);
    }

    @Test
    public void shouldFormatHelmClientVersion() {
        assertHelmClientVersionFormatted(testTerminateInfoDto, testTerminateInfoDto.getHelmClientVersion(), terminateResourceConverterImpl);
    }
}