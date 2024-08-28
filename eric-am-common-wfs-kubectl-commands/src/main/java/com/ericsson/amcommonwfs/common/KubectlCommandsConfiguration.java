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
package com.ericsson.amcommonwfs.common;

import com.ericsson.amcommonwfs.secret.AuxSecretApiDelegate;
import com.ericsson.amcommonwfs.secret.CreateAuxSecretCommandHandler;
import com.ericsson.amcommonwfs.secret.CreateCrdAuxSecretCommandHandler;
import com.ericsson.amcommonwfs.secret.DeleteAuxSecretCommandHandler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubectlCommandsConfiguration {

    @Bean
    public AuxSecretApiDelegate createCrdAuxiliarySecret(CreateCrdAuxSecretCommandHandler createCrdAuxSecretCommandHandler) {
        return new AuxSecretApiDelegate(createCrdAuxSecretCommandHandler);
    }

    @Bean
    public AuxSecretApiDelegate createAuxiliarySecret(
            @Qualifier("createAuxSecretCommandHandler") CreateAuxSecretCommandHandler createAuxSecretCommandHandler) {
        return new AuxSecretApiDelegate(createAuxSecretCommandHandler);
    }

    @Bean
    public AuxSecretApiDelegate deleteAuxiliarySecret(DeleteAuxSecretCommandHandler deleteAuxSecretCommandHandler) {
        return new AuxSecretApiDelegate(deleteAuxSecretCommandHandler);
    }

}
