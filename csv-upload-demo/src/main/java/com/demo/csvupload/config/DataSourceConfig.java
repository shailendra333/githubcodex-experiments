package com.demo.csvupload.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures the SQLite DataSource and ensures the data directory exists
 * before Hibernate tries to open the database file.
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Bean
    public DataSource dataSource() throws IOException {
        // Extract the file path from the JDBC URL (jdbc:sqlite:<path>)
        String filePath = jdbcUrl.replace("jdbc:sqlite:", "");
        Path dbPath = Paths.get(filePath).toAbsolutePath().normalize();
        Path dataDir = dbPath.getParent();

        if (dataDir != null && !Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
            log.info("Created SQLite data directory: {}", dataDir);
        }
        log.info("SQLite database file: {}", dbPath);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        // SQLite supports only one writer at a time; keep pool size at 1
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("sqlite-pool");

        return new HikariDataSource(config);
    }
}

