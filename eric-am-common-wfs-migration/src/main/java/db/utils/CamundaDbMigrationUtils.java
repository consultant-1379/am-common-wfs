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
package db.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public final class CamundaDbMigrationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaDbMigrationUtils.class);
    public static final String SELECT_TABLE_INFO = "SELECT * FROM information_schema.tables WHERE table_schema LIKE 'public' AND table_name LIKE ?";

    private CamundaDbMigrationUtils() {
    }

    public static void migrateCamundaDb(final Context context, final String expectedDbVersion, final String migrationScriptPath) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        if (isTableWithDbVersionsPresent(jdbcTemplate)) {
            String currentDbVersion = getCurrentCamundaDbVersion(jdbcTemplate);
            if (expectedDbVersion.equals(currentDbVersion)) {
                LOGGER.info("Current Camunda DB version {} is equal to the expected version {}, executing migration to the next version",
                            currentDbVersion, expectedDbVersion);
                executeCamundaMigration(context, migrationScriptPath);
            } else {
                LOGGER.info("Current Camunda DB version {} is not equal to the expected version {}, skipping migration to the next version",
                            currentDbVersion, expectedDbVersion);
            }
        } else {
            LOGGER.info("Table with Camunda DB versions not present, skipping migration");
        }
    }

    private static void executeCamundaMigration(final Context context, final String migrationScriptPath) {
        Resource resource = new ClassPathResource(migrationScriptPath);
        ScriptUtils.executeSqlScript(context.getConnection(), resource);
    }

    private static String getCurrentCamundaDbVersion(final JdbcTemplate template) {
        return template.query("SELECT * FROM ACT_GE_SCHEMA_LOG ORDER BY id_ DESC LIMIT 1;", rs -> {
            if (rs.next()) {
                return rs.getString(rs.findColumn("version_"));
            }
            return null;
        });
    }

    private static boolean isTableWithDbVersionsPresent(final JdbcTemplate template) {
        return Boolean.TRUE.equals(template.query(
                "SELECT * FROM information_schema.tables WHERE "
                        + "table_schema LIKE 'public' AND table_name LIKE 'act_ge_schema_log';", ResultSet::next));
    }

    public static boolean isTableExists(final Connection conn, final String table) {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_TABLE_INFO)) {
            stmt.setString(1, table);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.error("Error querying for table " + table, e);
        }
        return false;
    }
}
