package com.example.chess.game;

import com.example.chess.common.SchemaUpgrade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaUpgradeTest {

    @TempDir
    Path dir;

    @Test
    void addsNewColumnsToDatabasesFromEarlierVersions() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite:" + dir.resolve("old.db"), true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE games (id VARCHAR(36) PRIMARY KEY, player_color VARCHAR(5) NOT NULL)");
        jdbc.execute("INSERT INTO games (id, player_color) VALUES ('g1', 'WHITE')");
        jdbc.execute("CREATE TABLE moves (id VARCHAR(36) PRIMARY KEY, game_id VARCHAR(36) NOT NULL)");
        jdbc.execute("INSERT INTO moves (id, game_id) VALUES ('m1', 'g1')");

        SchemaUpgrade upgrade = new SchemaUpgrade(jdbc);
        upgrade.afterPropertiesSet();
        upgrade.afterPropertiesSet(); // idempotent

        assertThat(jdbc.queryForObject("SELECT difficulty FROM games WHERE id = 'g1'", String.class)).isEqualTo("MEDIUM");
        assertThat(jdbc.queryForObject("SELECT engine_context FROM moves WHERE id = 'm1'", String.class)).isNull();
        dataSource.destroy();
    }
}
