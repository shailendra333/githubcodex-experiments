package com.demo.csvupload.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the SQLite schema has the UNIQUE index on {@code customers.external_id}
 * that drives the {@code ON CONFLICT DO UPDATE} upsert.
 *
 * <p>Hibernate's {@code ddl-auto=update} does not retroactively add UNIQUE
 * constraints to existing tables in SQLite (ALTER TABLE … ADD CONSTRAINT is
 * not supported by SQLite).  This runner uses
 * {@code CREATE UNIQUE INDEX IF NOT EXISTS} which is idempotent — safe to
 * run every startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Ensure customers table exists first (Hibernate may not have created it yet
        // if the application starts fresh — this runs after the JPA context is ready).
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    external_id       TEXT,
                    first_name        TEXT NOT NULL,
                    last_name         TEXT NOT NULL,
                    email             TEXT,
                    phone             TEXT,
                    city              TEXT,
                    country           TEXT,
                    registration_date DATE,
                    created_at        DATETIME
                )
                """);

        // UNIQUE index on external_id — required for ON CONFLICT DO UPDATE.
        // IF NOT EXISTS makes this idempotent across restarts.
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_external_id " +
                "ON customers(external_id)");

        log.info("DatabaseInitializer: UNIQUE index on customers.external_id ensured.");
    }
}

