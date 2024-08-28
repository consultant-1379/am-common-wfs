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
import com.ericsson.amcommonwfs.model.AsyncDeletePvcsRequestDetails;
import com.ericsson.amcommonwfs.model.WorkingAsyncRequestDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.ericsson.amcommonwfs.utils.constants.Constants.REDIS_KEY_PREFIX;

@Slf4j
@Component
public class DeletePvcsRequestCommandJobService extends AbstractRequestCommandJobService<AsyncDeletePvcsRequestDetails> {

    private final KubectlService kubectlService;

    public DeletePvcsRequestCommandJobService(
            @Value("${asyncRequest.recovery.timeout}") Integer asyncRequestRecoveryTimeout,
            KubectlService kubectlService,
            ClusterConfigService clusterConfigService,
            RedisTemplate<String, AsyncDeletePvcsRequestDetails> redisDeletePvcsTemplate,
            RedisTemplate<String, String> redisTemplate,
            RedisTemplate<String, WorkingAsyncRequestDetails<AsyncDeletePvcsRequestDetails>> redisWorkingDeletePvcsTemplate,
            Tracing tracing) {
        super(REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deletePvcs.mutex",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deletePvcs.mainQueue",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deletePvcs.workingQueue",
                REDIS_KEY_PREFIX + "{am-common-wfs}.requests.deletePvcs.workingQueueTimeout",
                asyncRequestRecoveryTimeout, redisDeletePvcsTemplate, redisTemplate,
                redisWorkingDeletePvcsTemplate, clusterConfigService, tracing);
        this.kubectlService = kubectlService;
    }

    @Override
    protected boolean processCommandAsync(String configFilePath, AsyncDeletePvcsRequestDetails requestObject) {
        try {
            kubectlService.deletePvcs(configFilePath, requestObject);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Error during delete pvcs operation requestObject={}", requestObject, ex);
            return false;
        }
    }
}
