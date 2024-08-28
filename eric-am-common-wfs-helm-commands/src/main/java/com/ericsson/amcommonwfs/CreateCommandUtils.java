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

import static com.ericsson.amcommonwfs.constants.CommandConstants.SPACE;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class CreateCommandUtils {

    public static void provideArgument(final StringBuilder command, final String argument, final String argumentValue) {
        if (!Strings.isNullOrEmpty(argumentValue)) {
            command.append(SPACE).append(argument).append(SPACE).append(argumentValue);
        }
    }
}
