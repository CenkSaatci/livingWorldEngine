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
    void flywayHistoryTableExists() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var rs = conn.getMetaData()
                .getTables(null, "public", "flyway_schema_history", new String[]{"TABLE"});
            assertThat(rs.next())
                .as("flyway_schema_history-Tabelle sollte existieren")
                .isTrue();
        }
    }
}