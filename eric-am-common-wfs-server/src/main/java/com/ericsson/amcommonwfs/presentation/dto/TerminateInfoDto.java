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
package com.ericsson.amcommonwfs.presentation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminateInfoDto {

    private String releaseName;
    private String lifecycleOperationId;
    private String state;
    private String namespace;
    private String applicationTimeOut;
    private Boolean skipVerification;
    private Boolean cleanUpResources;
    private String clusterName;
    private Boolean skipJobVerification;
    private String helmClientVersion;
}
