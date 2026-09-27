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
    void addsDifficultyToDatabasesFromEarlierVersions() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite:" + dir.resolve("old.db"), true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE games (id VARCHAR(36) PRIMARY KEY, player_color VARCHAR(5) NOT NULL)");
        jdbc.execute("INSERT INTO games (id, player_color) VALUES ('g1', 'WHITE')");

        SchemaUpgrade upgrade = new SchemaUpgrade(jdbc);
        upgrade.afterPropertiesSet();
        upgrade.afterPropertiesSet(); // idempotent

        assertThat(jdbc.queryForObject("SELECT difficulty FROM games WHERE id = 'g1'", String.class)).isEqualTo("MEDIUM");
        dataSource.destroy();
    }
}
