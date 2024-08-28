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

public final class KubeConfigValidationException extends RuntimeException {
    private static final long serialVersionUID = 8643343774592867178L;

    public KubeConfigValidationException(final String stdError) {
        super(stdError);
    }

    public KubeConfigValidationException(final String stdError, final Exception cause) {
        super(stdError, cause);
    }

    public KubeConfigValidationException(final Exception e) {
        super(e.getMessage(), e);
    }
}
