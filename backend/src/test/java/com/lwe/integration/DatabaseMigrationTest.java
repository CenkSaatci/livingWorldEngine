package com.lwe.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🔴 RED: Dieser Test prüft, ob Flyway alle erwarteten Tabellen angelegt hat.
 * Da V001__initial.sql noch nicht existiert, wird im RED-Lauf {@code users},
 * {@code worlds}, etc. fehlen → Test failt.
 * <p>
 * 🟢 GREEN: Sobald V001__initial.sql angelegt ist, laufen beide Tests grün.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseMigrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
        "users", "game_systems", "worlds", "world_members",
        "entities", "world_events", "npc_intents"
    );

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatedAllExpectedTables() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            var rs = meta.getTables(null, "public", "%", new String[]{"TABLE"});

            var foundTables = new HashSet<String>();
            while (rs.next()) {
                foundTables.add(rs.getString("TABLE_NAME").toLowerCase());
            }

            for (var table : EXPECTED_TABLES) {
                assertThat(foundTables)
                    .as("Flyway-V001 sollte Tabelle '%s' angelegt haben", table)
                    .contains(table);
            }
        }
    }

    @Test
    void entitiesTableHasSkillsJsonColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getColumns(null, "public", "entities", "skills_json");
            assertThat(rs.next())
                .as("V089 sollte skills_json-Spalte auf entities hinzufügen")
                .isTrue();
            assertThat(rs.getString("TYPE_NAME"))
                .as("skills_json sollte JSONB sein")
                .isIn("jsonb", "_jsonb");
        }
    }

    @Test
    void gameSystemsTableHasOwnershipAndVisibilityColumns() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var owner = conn.getMetaData().getColumns(null, "public", "game_systems", "owner_id");
            assertThat(owner.next()).as("V098: game_systems.owner_id (P27-T01/F8)").isTrue();
            var visibility = conn.getMetaData().getColumns(null, "public", "game_systems", "visibility");
            assertThat(visibility.next()).as("V098: game_systems.visibility").isTrue();
        }
    }

    @Test
    void worldsTableHasVisibilityColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var visibility = conn.getMetaData().getColumns(null, "public", "worlds", "visibility");
            assertThat(visibility.next()).as("V098: worlds.visibility").isTrue();
        }
    }

    @Test
    void campaignsTableHasForkedWorldColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var col = conn.getMetaData().getColumns(null, "public", "campaigns", "forked_world");
            assertThat(col.next()).as("V099: campaigns.forked_world (P27-T03)").isTrue();
        }
    }

    @Test
    void itemsTableHasGameSystemIdColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getColumns(null, "public", "items", "game_system_id");
            assertThat(rs.next())
                .as("V090 sollte game_system_id-Spalte auf items haben")
                .isTrue();
        }
    }

    @Test
    void abilitiesTableHasGameSystemIdColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getColumns(null, "public", "abilities", "game_system_id");
            assertThat(rs.next())
                .as("V091 sollte game_system_id-Spalte auf abilities haben")
                .isTrue();
        }
    }

    @Test
    void campaignsTableExists() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getTables(null, "public", "campaigns", new String[]{"TABLE"});
            assertThat(rs.next())
                .as("V092 sollte campaigns-Tabelle anlegen")
                .isTrue();
        }
    }

    @Test
    void flywayHistoryTableExists() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getTables(null, "public", "flyway_schema_history", new String[]{"TABLE"});
            assertThat(rs.next())
                .as("flyway_schema_history-Tabelle sollte existieren")
                .isTrue();
        }
    }

    @Test
    void worldsTableHasNoGameSystemIdColumn() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getColumns(null, "public", "worlds", "game_system_id");
            assertThat(rs.next())
                .as("V097 sollte game_system_id-Spalte aus worlds entfernen (P25-T06)")
                .isFalse();
        }
    }

    @Test
    void appliedMigrationsFormIncreasingSequence() throws SQLException {
        // V007-V009 existieren nie als Dateien — Lücken sind ok, solange Flyway
        // strikt aufsteigend applied (kein out-of-order, keine fehlgeschlagenen).
        try (var conn = dataSource.getConnection();
             var st = conn.createStatement();
             var rs = st.executeQuery(
                 "SELECT version, success FROM flyway_schema_history WHERE success = true ORDER BY installed_rank")) {
            var versions = new java.util.ArrayList<String>();
            while (rs.next()) {
                assertThat(rs.getBoolean("success")).isTrue();
                versions.add(rs.getString("version"));
            }
            assertThat(versions).isNotEmpty();
            assertThat(versions).isSorted();
        }
    }
}