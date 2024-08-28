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
package com.ericsson.amcommonwfs.model;

public enum CommandType {
    INSTALL("install"),
    UPGRADE("upgrade"),
    ROLLBACK("rollback"),
    UNINSTALL("uninstall"),
    CRD("crd");

    private String type;

    CommandType(final String type) {
        this.type = type;
    }

    public String getCommandType() {
        return type;
    }
}
