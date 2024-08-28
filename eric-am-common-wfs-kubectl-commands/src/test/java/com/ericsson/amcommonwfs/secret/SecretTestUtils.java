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
package com.ericsson.amcommonwfs.secret;

import com.ericsson.amcommonwfs.utils.constants.Constants;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SecretTestUtils {
    private static final Set<String> PASSING_SECRETS = Set.of("userdb", "registry");
    private static final Set<String> FAILING_SECRETS = Set.of("userdb", "secret2");

    public static Map<String, String> prepareDay0Configuration() {
        Map<String, String> day0Configuration = new LinkedHashMap<>();
        day0Configuration.put("secret1", "{\"username\": \"vnfm\", \"password\": \"Ericsson123!\"}");
        day0Configuration.put("secret2", "{\"username\": \"vnfm\", \"password\": \"DefaultP123!\"}");
        return day0Configuration;
    }

    public static V1SecretList createSecretsResponse(boolean exist) {
        Set<String> secretNames = exist ? FAILING_SECRETS : PASSING_SECRETS;
        List<V1Secret> secrets = secretNames.stream().map(name -> new V1ObjectMeta().name(name))
                .map(meta -> new V1Secret().metadata(meta)).collect(Collectors.toList());
        return new V1SecretList().items(secrets);
    }
}
