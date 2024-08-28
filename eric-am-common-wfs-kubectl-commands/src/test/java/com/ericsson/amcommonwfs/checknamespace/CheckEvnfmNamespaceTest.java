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
package com.ericsson.amcommonwfs.checknamespace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatNoException;

import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;

import java.io.IOException;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.amcommonwfs.common.EvnfmNamespaceService;
import com.ericsson.amcommonwfs.utils.ClusterFileUtils;

@SpringBootTest(classes = { CheckEvnfmNamespace.class })
public class CheckEvnfmNamespaceTest {

    private static final String EVNFM_NAMESPACE = "evnfm-namespace";
    private static final String DUMMY_NAME_SPACE = "dummy_name_space";
    private static final String DUMMY_CLUSTER_NAME = "test01.config";
    private static final String DUMMY_CLUSTER_CONFIG = "dummy_cluster_config.config";

    @Autowired
    private CheckEvnfmNamespace checkEvnfmNamespace;

    @MockBean
    private ClusterFileUtils clusterFileUtils;

    @MockBean
    private EvnfmNamespaceService evnfmNamespaceService;

    private ExecutionImpl execution = new ExecutionImpl();


    @BeforeEach
    public void init() throws IOException {
        putVariablesForExecution();
        when(clusterFileUtils
                     .createClusterConfigForHelm(execution))
                .thenReturn(DUMMY_CLUSTER_CONFIG);
    }

    @Test
    public void checkEvnfmNamespaceOnAnotherClusterSuccess() throws Exception {
            execution.setVariable(NAMESPACE, EVNFM_NAMESPACE);
            when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(false);
            assertThatNoException().isThrownBy(() -> checkEvnfmNamespace.execute(execution));
    }

    @Test
    public void checkEvnfmNamespaceFailed() throws Exception {
        execution.setVariable(NAMESPACE, EVNFM_NAMESPACE);
        when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> checkEvnfmNamespace.execute(execution)).isInstanceOf(BpmnError.class);
    }

    @Test
    public void checkNamespaceSuccess () throws Exception {
        when(evnfmNamespaceService.checkEvnfmNamespace(any(), any())).thenReturn(false);
        assertThatNoException().isThrownBy(() -> checkEvnfmNamespace.execute(execution));
    }

    private void putVariablesForExecution() {
        execution.setVariable(NAMESPACE, DUMMY_NAME_SPACE);
        execution.setVariable(CLUSTER_NAME, DUMMY_CLUSTER_NAME);
    }
}