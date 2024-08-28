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

import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_ERROR;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.APP_TIME_OUT;
import static com.ericsson.amcommonwfs.VerifyTaskConstants.VERIFY_CMD_EXEC_RESULT;
import static com.ericsson.amcommonwfs.utils.CommonUtils.convertToJSONString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.IS_APPLICATION_TIMED_OUT;

import java.util.regex.Pattern;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.apache.commons.lang3.BooleanUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationErrorTask implements JavaDelegate {

    static final Pattern HELM_REPO_UPDATE_OUTPUT = Pattern.compile("(.|\\n)*Helming!.*Error:"); // NOSONAR

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        String errorMsg = (String) execution.getVariable(ERROR_MESSAGE);
        if (!Strings.isNullOrEmpty(errorMsg)) {
            errorMsg = HELM_REPO_UPDATE_OUTPUT.matcher(errorMsg).replaceAll("Error:");
        } else {
            boolean appTimedOut = BooleanUtils.
                    toBooleanDefaultIfNull((Boolean) execution.getVariable(IS_APPLICATION_TIMED_OUT), false);
            errorMsg = appTimedOut ? convertToJSONString(APP_TIME_OUT, HttpStatus.UNPROCESSABLE_ENTITY.toString())
                    : APP_ERROR;
        }
        LOGGER.error(errorMsg);
        execution.setVariable(ERROR_MESSAGE, convertToJSONString(errorMsg, HttpStatus.UNPROCESSABLE_ENTITY.toString()));
        execution.setVariable(VERIFY_CMD_EXEC_RESULT, errorMsg);
    }
}
