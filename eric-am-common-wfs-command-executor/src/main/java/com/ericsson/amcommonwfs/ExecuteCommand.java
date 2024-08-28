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
package com.ericsson.amcommonwfs;

import static com.ericsson.amcommonwfs.utility.CommandUtils.hideSensitiveData;
import static com.ericsson.amcommonwfs.utility.CommandUtils.verifyCommand;
import static com.ericsson.amcommonwfs.utils.CommonUtils.resolveTimeOut;
import static com.ericsson.amcommonwfs.utils.constants.Constants.CLUSTER_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.COMMAND_TIME_TAKEN;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.utils.ClusterFileUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExecuteCommand extends ExecuteCommandBase {

    @Autowired
    private ProcessExecutor processExecutor;

    @Autowired
    private ClusterFileUtils clusterFileUtils;

    @Override
    protected Optional<ProcessExecutorResponse> prepareAndExecuteCommand(final DelegateExecution execution) throws IOException,
            InterruptedException, CommandTimedOutException {
        String command = verifyCommand(execution);
        String timeOut = resolveTimeOut(execution);
        LOGGER.info("Command to execute :: {}. TimeOut set is :: {} ", hideSensitiveData(command), timeOut);

        final long startTime = System.currentTimeMillis();
        try {
            ProcessExecutorResponse processExecutorResponse = processExecutor.executeProcess(command, Integer.parseInt(timeOut), false);
            final long endTime = System.currentTimeMillis();
            execution.setVariable(COMMAND_TIME_TAKEN, (endTime - startTime));

            return Optional.ofNullable(processExecutorResponse);
        } finally {
            String clusterConfig = (String) execution.getVariable(CLUSTER_NAME);
            if (StringUtils.isNotEmpty(clusterConfig)) {
                clusterFileUtils.removeClusterConfig(clusterConfig);
            }
        }

    }
}
