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

import java.io.File;
import java.util.List;

import org.mockito.ArgumentMatcher;

public class CommandLineArgumentsMatcher implements ArgumentMatcher<List<String>> {
    private final List<String> patternArgs;

    public CommandLineArgumentsMatcher(final List<String> patternArgs) {
        this.patternArgs = patternArgs;
    }

    @Override
    public boolean matches(final List<String> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }
        String arg = arguments.get(0);
        String pattern = patternArgs.get(0);
        int executableIndex = arg.lastIndexOf(File.separatorChar) + 1;
        if (!arg.substring(executableIndex).startsWith(pattern)) {
            return false;
        }
        return arguments.subList(1, arguments.size()).containsAll(patternArgs.subList(1, patternArgs.size()));
    }
}
