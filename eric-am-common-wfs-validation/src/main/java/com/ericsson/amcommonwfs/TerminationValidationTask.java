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

import static com.ericsson.amcommonwfs.ValidateParams.verifyClusterConfigPresent;
import static com.ericsson.amcommonwfs.ValidateReleaseName.checkReleaseName;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TerminationValidationTask implements JavaDelegate {

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info("Validating release name...");
        checkReleaseName(execution);
        LOGGER.info("Release name has been validated...");
        verifyClusterConfigPresent(execution);
    }
}
