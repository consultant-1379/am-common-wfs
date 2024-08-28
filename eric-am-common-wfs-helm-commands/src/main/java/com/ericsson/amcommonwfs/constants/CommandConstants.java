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
package com.ericsson.amcommonwfs.constants;

public final class CommandConstants {

    public static final String SPACE = " ";
    public static final String KUBE_CONFIG_ARGUMENT = "--kubeconfig";
    public static final String NAMESPACE_ARGUMENT = "--namespace";
    public static final String GET_COMMAND = "get";
    public static final String STATUS_COMMAND = "status";
    public static final String HISTORY_COMMAND = "history";
    public static final String REVISION_STATUS = "revisionStatus";
    public static final String INVALID_HISTORY_RESPONSE = "No valid history found for release and revision specified";
    public static final String INVALID_REVISION_RESPONSE = "Revision not found";
    public static final String INVALID_REVISION_STATUS = "Revision specified is in incorrect state";
    public static final String INVALID_UNINSTALLING_REVISION_STATUS = "Release is already in uninstalling state";
    public static final String COMMAND_TYPE = "commandType";
    public static final String MAX = "--max";
    public static final String OUTPUT_JSON = "-o json";
    public static final String VALUES = "values";

    private CommandConstants() {
    }
}
