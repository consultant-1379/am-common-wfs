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
package com.ericsson.contracts.base;

import com.ericsson.amcommonwfs.model.AsyncDeleteNamespaceRequestDetails;
import com.ericsson.amcommonwfs.presentation.services.AbstractRequestCommandJobService;
import org.springframework.boot.test.mock.mockito.MockBean;

public class NegativeNamespaceBase extends DeleteNegativeBase {

    @MockBean
    private AbstractRequestCommandJobService<AsyncDeleteNamespaceRequestDetails> deleteNamespaceCommandJobService;
}
