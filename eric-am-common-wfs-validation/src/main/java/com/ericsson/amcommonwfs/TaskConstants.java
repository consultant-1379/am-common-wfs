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

import java.util.regex.Pattern;

final class TaskConstants {

    static final String REQUIRED_PROPERTIES_ERROR_MSG =
            "Required properties are missing/null - provide valid input for the following: ";

    static final String MALFORMED_URL_ERROR_MSG =
            "chartUrl property validation failed, please provide a valid" + " URL : ";

    static final String ADDITIONAL_CHART_VALUES_ERROR_MSG =
            "chartUrl property has been specified, %s or %s properties should not be set. "
                    + "Please see API documentation for correct usage.";

    static final String ADDITIONAL_PARAMS_AS_MAP_MSG =
            "AdditionalParams need to be in MAP format. Please provide valid input";

    static final String RELEASE_NAME_ERROR_MSG = "releaseName must consist of lower case alphanumeric characters or -"
            + " It must start with an alphabetic character, and end with an alphanumeric character";

    static final Pattern RELEASE_NAME_PATTERN = Pattern
            .compile("[a-z]([-a-z0-9]*[a-z0-9])?");

    static final String REVISION_NUMBER_ERROR_MSG = "revisionNumber must be an integer";

    static final String CLUSTER_CONFIG_NOT_PRESENT_ERROR_MESSAGE = "cluster config not present, please add the " +
            "config file using 'add cluster config rest api' and then use this parameter";

    private TaskConstants() {
    }
}
