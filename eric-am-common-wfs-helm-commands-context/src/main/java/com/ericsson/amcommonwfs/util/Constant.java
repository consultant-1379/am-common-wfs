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

public final class Constant {
    public static final String COMMAND_TYPE = "commandType";

    public static final String TIMEOUT = "timeout";

    public static final String HELM_REPO = "helmRepoRoot";

    public static final String HELM_DEBUG = "helmDebug";

    public static final String GLOBAL_REGISTRY_URL = "global.registry.url";
    public static final String IMAGE_CREDENTIALS_REGISTRY_URL = "imageCredentials.registry.url";
    public static final String GLOBAL_REGISTRY_PULL_SECRET = "global.registry.pullSecret";
    public static final String GLOBAL_PULL_SECRET = "global.pullSecret";
    public static final String IMAGE_CREDENTIALS_PULL_SECRET = "imageCredentials.pullSecret";
    public static final String IMAGE_CREDENTIALS_REGISTRY_PULL_SECRET = "imageCredentials.registry.pullSecret";

    public static final String SET_FLAG_VALUES = "setFlagValues";

    private Constant() {
    }
}
