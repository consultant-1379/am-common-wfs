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

public class CommandException extends RuntimeException {

    private static final long serialVersionUID = 7371719337932763283L;

    public CommandException() {
        super();
    }

    public CommandException(final String exception) {
        super(exception);
    }

}
