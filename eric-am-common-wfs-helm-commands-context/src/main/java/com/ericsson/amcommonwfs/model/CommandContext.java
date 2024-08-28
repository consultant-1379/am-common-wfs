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

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class CommandContext {

    /**
     * API version. Must be increased each time backward incompatible changes introduced.
     * Helm Executor must reject task if 'version' is not in the list of supported versions.
     * Current supported version is "v1"
     */
    private String version;
    private String commandType;
    private String helmClientVersion;
    private Map<String, Object> commandParams;
}
