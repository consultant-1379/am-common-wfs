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
import static java.util.Collections.unmodifiableList;

import static com.ericsson.amcommonwfs.ValidateParams.validateChartParameters;
import static com.ericsson.amcommonwfs.ValidateParams.validateMandatoryParameters;
import static com.ericsson.amcommonwfs.ValidateParams.verifyAdditionalParamsIsMap;
import static com.ericsson.amcommonwfs.ValidateParams.verifyClusterConfigPresent;
import static com.ericsson.amcommonwfs.ValidateReleaseName.checkReleaseName;
import static com.ericsson.amcommonwfs.utils.constants.Constants.NAMESPACE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.amcommonwfs.utils.CamundaStepLogging;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InstantiationValidationTask implements JavaDelegate {

    private static final List<String> MANDATORY_PARAMS = unmodifiableList(
            new ArrayList<>(asList(NAMESPACE, RELEASE_NAME)));

    @Override
    @CamundaStepLogging
    public void execute(final DelegateExecution execution) {
        LOGGER.info("Validating properties...");
        validateMandatoryParameters(execution, MANDATORY_PARAMS);
        checkReleaseName(execution);
        validateChartParameters(execution);
        verifyAdditionalParamsIsMap(execution);
        verifyClusterConfigPresent(execution);
    }
}
