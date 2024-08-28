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
package com.ericsson.amcommonwfs.exceptions;

import static com.ericsson.amcommonwfs.exception.ParameterErrorMessage.CLUSTER_NAME_ERROR_MSG;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.exception.ParameterErrorMessage;

public class ParameterErrorMessageTest {

    @Test
    public void shouldReturnNullErrorNameNotFound() {
        Assertions.assertNull(ParameterErrorMessage.fromString("not found"));
    }

    @Test
    public void shouldReturnClusterNameErrorMessage() {
        ParameterErrorMessage parameterErrorMessage = ParameterErrorMessage
                .fromString("CLUSTER_NAME_ERROR_MSG");
        Assertions.assertEquals(CLUSTER_NAME_ERROR_MSG, parameterErrorMessage);
    }
}
