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
package com.ericsson.amcommonwfs.camunda.bpm;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.processEngine;

import static com.ericsson.amcommonwfs.TestConstants.CLUSTER_CONFIG;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CHART_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_CLIENT_VERSION;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ORIGINAL_CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.amcommonwfs.util.DefinitionKey;
@Slf4j
final class BpmnTestUtilities {


    private BpmnTestUtilities() {
    }

    public static ProcessInstance getProcessInstance(final HashMap<String, Object> variables, final String workflow) {
        return processEngine().getRuntimeService()
                              .startProcessInstanceByKey(DefinitionKey.getProcessDefinitionKey(workflow), variables);
    }

    public static boolean waitUntilNoActiveJobs(ProcessEngine processEngine, long wait) throws InterruptedException {
        long timeout = System.currentTimeMillis() + wait;
        while (System.currentTimeMillis() < timeout) {
            long jobs = processEngine.getManagementService().createJobQuery().active().count();
            if (jobs == 0) {
                return true;
            }
            LOGGER.info("Waiting for " + jobs + " jobs");
            Thread.sleep(2000);
        }
        return false;
    }

    public static HashMap<String, Object> getCommonVariablesMap() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put(RELEASE_NAME, "my-release");
        variables.put(NAMESPACE, "default");
        variables.put(CHART_NAME, "my-chartName");
        variables.put(HELM_CLIENT_VERSION, "helm");
        variables.put(ORIGINAL_CLUSTER_NAME, CLUSTER_CONFIG);
        variables.put(CLUSTER_CONFIG_CONTENT_KEY, "dummy-config-content-key");
        return variables;
    }

    public static Map<String, Object> getAdditionalParamsWithSecret() {
        Map<String, Object> additionalParameters = new HashMap<>(2);
        additionalParameters.put("secret1", "{\"username\": \"vnfm\", \"password\": \"Ericsson123!\"}");
        additionalParameters.put("secret2", "{\"username\": \"vnfm\", \"password\": \"DefaultP123!\"}");
        return additionalParameters;
    }
}
