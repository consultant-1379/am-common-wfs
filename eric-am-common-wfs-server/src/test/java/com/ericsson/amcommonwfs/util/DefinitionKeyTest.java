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
package com.ericsson.amcommonwfs.util;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TERMINATE_DEFINITION_KEY;

import org.junit.jupiter.api.Test;

import com.ericsson.amcommonwfs.CommandType;

public class DefinitionKeyTest {

    public static final String DUMMY_DEFINITION_KEY = "badKey";

    @Test
    public void testFullDefinitionKeySupplied() {
        assertThat(DefinitionKey.getProcessDefinitionKey(INSTANTIATE_DEFINITION_KEY)).isEqualTo(INSTANTIATE_DEFINITION_KEY);
    }

    @Test
    public void testSimpleDefinitionKeySupplied() {
        assertThat(DefinitionKey.getProcessDefinitionKey(CommandType.INSTALL.getCommandType())).isEqualTo(INSTANTIATE_DEFINITION_KEY);
    }

    @Test
    public void testSimpleDefinitionKeyInstantiate() {
        assertThat(DefinitionKey.getProcessDefinitionKey(CommandType.INSTANTIATE.getCommandType())).isEqualTo(INSTANTIATE_DEFINITION_KEY);
    }

    @Test
    public void testSimpleDefinitionKeyTerminate() {
        assertThat( DefinitionKey.getProcessDefinitionKey(CommandType.TERMINATE.getCommandType())).isEqualTo(TERMINATE_DEFINITION_KEY);
    }

    @Test
    public void testDefinitionKeySuppliedNotFoundReturnsSuppliedKey() {
        assertThat(DefinitionKey.getProcessDefinitionKey(DUMMY_DEFINITION_KEY)).isEqualTo(DUMMY_DEFINITION_KEY);
    }

}
