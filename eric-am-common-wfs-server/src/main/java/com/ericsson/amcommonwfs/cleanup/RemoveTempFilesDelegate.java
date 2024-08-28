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
package com.ericsson.amcommonwfs.cleanup;

import static com.ericsson.amcommonwfs.utils.constants.Constants.ADDITIONAL_VALUES_FILE_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_CONFIG_CONTENT_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTION_ID_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_EXECUTOR_REDIS_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.HELM_JOBS_CONTEXT;
import static com.ericsson.amcommonwfs.utils.constants.Constants.VALUES_FILE_CONTENT_KEY;

import java.io.File;
import java.util.List;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RemoveTempFilesDelegate implements JavaDelegate {

    private static final List<String> TEMP_RECORD_VARS = List.of(
            VALUES_FILE_CONTENT_KEY, ADDITIONAL_VALUES_FILE_CONTENT_KEY, CLUSTER_CONFIG_CONTENT_KEY, HELM_EXECUTOR_REDIS_KEY);

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RemoveTempFilesDelegate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info("Cleaning up temporary resources from filesystem");
        TEMP_RECORD_VARS.forEach(v -> deleteRecord(execution, v));
        // Clean Helm job hash entry that was kept to prevent job duplication
        final String helmExecutionId = (String) execution.getVariable(HELM_EXECUTION_ID_KEY);
        if (helmExecutionId != null) {
            redisTemplate.opsForHash().delete(HELM_JOBS_CONTEXT, helmExecutionId);
        }
    }

    private void deleteFile(final DelegateExecution execution, final String name) {
        final String fileName = (String) execution.getVariable(name);
        if (fileName == null) {
            return;
        }
        if (new File(fileName).delete()) {
            LOGGER.info(String.format("File %s was removed", fileName));
        } else {
            LOGGER.info(String.format("File %s not found", fileName));
        }
    }

    private void deleteRecord(final DelegateExecution execution, final String name) {
        final String key = (String) execution.getVariable(name);
        if (key != null) {
            redisTemplate.delete(key);
        }
    }
}


