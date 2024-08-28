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

import static java.util.Arrays.asList;

import static com.ericsson.amcommonwfs.ValidateParams.validateMandatoryParameters;
import static com.ericsson.amcommonwfs.ValidateParams.verifyClusterConfigPresent;
import static com.ericsson.amcommonwfs.ValidateReleaseName.checkReleaseName;
import static com.ericsson.amcommonwfs.ValidateRevisionNumber.checkRevisionNumber;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;
import static com.ericsson.amcommonwfs.utils.constants.Constants.REVISION_NUMBER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RollbackValidationTask implements JavaDelegate {

    private static final List<String> MANDATORY_PARAMS = Collections
            .unmodifiableList(new ArrayList<>(asList(RELEASE_NAME, REVISION_NUMBER)));

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info("Validating release name and revision number...");
        validateMandatoryParameters(execution, MANDATORY_PARAMS);
        checkReleaseName(execution);
        checkRevisionNumber(execution);
        verifyClusterConfigPresent(execution);
    }

}

