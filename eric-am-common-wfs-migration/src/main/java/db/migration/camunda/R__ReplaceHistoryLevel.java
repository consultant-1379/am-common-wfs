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

import db.utils.HistoryLevel;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static db.utils.CamundaDbMigrationUtils.isTableExists;

public class R__ReplaceHistoryLevel extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(R__ReplaceHistoryLevel.class);
    private static final String REQUIRED_TABLE = "act_ge_property";
    private static final String SELECT_HISTORY_LEVEL = "SELECT value_ FROM act_ge_property WHERE name_ = 'historyLevel'";
    private static final String UPDATE_HISTORY_LEVEL = "UPDATE act_ge_property SET value_ = ? WHERE name_ = 'historyLevel'";
    private static final HistoryLevel HISTORY_LEVEL = parseHistoryLevel();

    @Override
    public void migrate(Context context) throws Exception {
        LOGGER.info("Updating Camunda history level settings to '{}'", HISTORY_LEVEL.getName());
        Connection conn = context.getConnection();
        if (!isTableExists(conn, REQUIRED_TABLE)) {
            LOGGER.info("Table " + REQUIRED_TABLE + " does not exists, skipping Camunda history level migration");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(SELECT_HISTORY_LEVEL);
            if (!rs.next()) {
                LOGGER.info("No history level setting found, looks like fresh installation.");
                return;
            }
            int currentLevel = rs.getInt(1);
            if (currentLevel != HISTORY_LEVEL.getId()) {
                updateHistoryLevel(conn, HISTORY_LEVEL.getId());
            }
            LOGGER.info("Updated Camunda history level to '{}'", HISTORY_LEVEL.getName());
        } catch (SQLException e) {
            LOGGER.error("Unable to update Camunda history level.", e);
        }
    }

    private void updateHistoryLevel(Connection conn, int value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_HISTORY_LEVEL)) {
            stmt.setInt(1, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to update already configured Camunda history level");
            throw e;
        }
    }

    @Override
    public Integer getChecksum() {
        return HISTORY_LEVEL.getId();
    }

    private static HistoryLevel parseHistoryLevel() {
        String historyLevel = System.getenv("CAMUNDA_HISTORY_LEVEL");

        if (HistoryLevel.FULL.getName().equals(historyLevel)) {
            return HistoryLevel.FULL;
        } else if (HistoryLevel.NONE.getName().equals(historyLevel)) {
            return HistoryLevel.NONE;
        } else if (HistoryLevel.AUDIT.getName().equals(historyLevel)) {
            return HistoryLevel.AUDIT;
        } else {
            return HistoryLevel.ACTIVITY;
        }
    }
}
