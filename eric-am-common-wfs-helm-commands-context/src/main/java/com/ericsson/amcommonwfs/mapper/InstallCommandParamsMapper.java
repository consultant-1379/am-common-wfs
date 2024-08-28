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
package com.ericsson.amcommonwfs.mapper;

import static com.ericsson.amcommonwfs.model.CommandType.INSTALL;

import org.springframework.stereotype.Component;

import com.ericsson.amcommonwfs.model.CommandType;
import com.ericsson.amcommonwfs.utils.error.ErrorCode;

@Component
public class InstallCommandParamsMapper extends ExtendedBaseCommandParamsMapper {

    @Override
    public CommandType getType() {
        return INSTALL;
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.BPMN_INSTALL_FAILED;
    }
}
