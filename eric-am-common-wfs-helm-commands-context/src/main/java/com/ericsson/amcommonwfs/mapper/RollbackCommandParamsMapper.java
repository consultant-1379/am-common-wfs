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

import static com.ericsson.amcommonwfs.utils.constants.Constants.MAX_HISTORY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;

import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.model.CommandType;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

@Component
public class RollbackCommandParamsMapper extends BaseCommandParamsMapper {
    @Override
    public CommandType getType() {
        return CommandType.ROLLBACK;
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.BPMN_ROLLBACK_FAILED;
    }

    @Override
    public Map<String, Object> apply(final DelegateExecution execution) {
        Map<String, Object> commandParams = super.apply(execution);

        provideRollbackSpecific(commandParams, execution);

        return commandParams;
    }

    private static void provideRollbackSpecific(final Map<String, Object> commandParams, final DelegateExecution execution) {
        commandParams.put(MAX_HISTORY, 0);
        commandParams.put(REVISION_NUMBER, execution.getVariable(REVISION_NUMBER));
    }
}
