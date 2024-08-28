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
package com.ericsson.amcommonwfs.exception;

public class CommandExecutionException extends RuntimeException {
    private static final long serialVersionUID = 7837722433837051755L;

    public CommandExecutionException() {
        super();
    }

    public CommandExecutionException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public CommandExecutionException(String exceptionMessage, Throwable exception) {
        super(exceptionMessage, exception);
    }
}
