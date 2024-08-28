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

import static com.ericsson.amcommonwfs.utils.constants.Constants.CRD_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.INSTANTIATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ROLLBACK_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.SCALE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.TERMINATE_DEFINITION_KEY;
import static com.ericsson.amcommonwfs.utils.constants.Constants.UPGRADE_DEFINITION_KEY;

import com.ericsson.amcommonwfs.CommandType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor (access = AccessLevel.PACKAGE)
public enum DefinitionKey {

    INSTALL(INSTANTIATE_DEFINITION_KEY, CommandType.INSTALL.getCommandType()),
    INSTANTIATE(INSTANTIATE_DEFINITION_KEY, CommandType.INSTANTIATE.getCommandType()),
    UPGRADE(UPGRADE_DEFINITION_KEY, CommandType.UPGRADE.getCommandType()),
    ROLLBACK(ROLLBACK_DEFINITION_KEY, CommandType.ROLLBACK.getCommandType()),
    UNINSTALL(TERMINATE_DEFINITION_KEY, CommandType.UNINSTALL.getCommandType()),
    SCALE(SCALE_DEFINITION_KEY, CommandType.SCALE.getCommandType()),
    TERMINATE(TERMINATE_DEFINITION_KEY, CommandType.TERMINATE.getCommandType()),
    CRD(CRD_DEFINITION_KEY, CommandType.CRD.getCommandType());

    private String processDefinitionKey;
    private String simpleDefinitionKey;

    public static String getProcessDefinitionKey(final String definitionKey) {
        for (DefinitionKey key : DefinitionKey.values()) {
            if (key.processDefinitionKey.equalsIgnoreCase(definitionKey) || key.simpleDefinitionKey
                    .equalsIgnoreCase(definitionKey)) {
                return key.processDefinitionKey;
            }
        }
        return definitionKey;
    }
}
