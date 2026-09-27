package com.example.chess;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SQLite creates the database file but not its directory; create the directory before the
 * datasource starts.
 */
class DatabaseDirectoryInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String url = event.getEnvironment().getProperty("spring.datasource.url", "");
        if (!url.startsWith(SQLITE_PREFIX)) {
            return;
        }
        String file = url.substring(SQLITE_PREFIX.length()).split("\\?")[0];
        if (file.isBlank() || file.startsWith(":memory:")) {
            return;
        }
        Path parent = Path.of(file).toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create database directory " + parent, e);
        }
    }
}
