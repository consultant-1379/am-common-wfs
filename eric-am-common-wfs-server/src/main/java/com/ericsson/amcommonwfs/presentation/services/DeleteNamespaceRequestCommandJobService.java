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
package com.ericsson.amcommonwfs.presentation.services;

import brave.Tracing;
import com.ericsson.amcommonwfs.cluster.config.service.ClusterConfigService;
import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.model.WorkingAsyncRequestDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.ericsson.amcommonwfs.utils.constants.Constants.REDIS_KEY_PREFIX;

@Slf4j
@Component
public class DeleteNamespaceRequestCommandJobService
        extends AbstractRequestCommandJobService<AsyncDeleteNamespaceRequestDetails> {

    private final KubectlService kubectlService;

    public DeleteNamespaceRequestCommandJobService(
            @Value("${asyncRequest.recovery.timeout}") Integer asyncRequestRecoveryTimeout,
            KubectlService kubectlService,
            ClusterConfigService clusterConfigService,
            RedisTemplate<String, AsyncDeleteNamespaceRequestDetails> redisDeleteNamespaceTemplate,
            RedisTemplate<String, String> redisTemplate,
            RedisTemplate<String, WorkingAsyncRequestDetails<AsyncDeleteNamespaceRequestDetails>> redisWorkingDeleteNamespaceTemplate,
            Tracing tracing) {
        super(REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deleteNamespace.mutex",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deleteNamespace.mainQueue",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deleteNamespace.workingQueue",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deleteNamespace.workingQueueTimeout",
                asyncRequestRecoveryTimeout, redisDeleteNamespaceTemplate, redisTemplate,
                redisWorkingDeleteNamespaceTemplate, clusterConfigService, tracing);
        this.kubectlService = kubectlService;
    }

    @Override
    protected boolean processCommandAsync(String configFilePath, AsyncDeleteNamespaceRequestDetails requestObject) {
        try {
            kubectlService.deleteNamespace(requestObject, configFilePath);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Error during delete namespace operation requestObject={}", requestObject, ex);
            return false;
        }
    }
}
