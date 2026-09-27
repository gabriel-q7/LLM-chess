package com.example.chess.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Brings databases created by earlier versions up to date. {@code schema.sql} only creates
 * missing tables, so columns added later are added here, once, if absent.
 */
@Component
@DependsOnDatabaseInitialization
public class SchemaUpgrade implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final JdbcTemplate jdbc;

    public SchemaUpgrade(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterPropertiesSet() {
        addColumnIfMissing("games", "difficulty", "VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        List<String> columns = jdbc.query("PRAGMA table_info(" + table + ")", (rs, i) -> rs.getString("name"));
        if (!columns.contains(column)) {
            log.info("Adding column {}.{}", table, column);
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
