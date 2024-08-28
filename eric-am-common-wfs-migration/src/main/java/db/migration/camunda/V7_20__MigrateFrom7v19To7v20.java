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
package db.migration.camunda;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import static db.utils.CamundaDbMigrationUtils.migrateCamundaDb;

public class V7_20__MigrateFrom7v19To7v20 extends BaseJavaMigration {
    private static final String EXPECTED_CAMUNDA_DB_VERSION = "7.19.0";
    private static final String MIGRATION_SCRIPT_PATH = "camunda/migration/postgres_engine_7.19_to_7.20.sql";

    public void migrate(final Context context) {
        migrateCamundaDb(context, EXPECTED_CAMUNDA_DB_VERSION, MIGRATION_SCRIPT_PATH);
    }
}
