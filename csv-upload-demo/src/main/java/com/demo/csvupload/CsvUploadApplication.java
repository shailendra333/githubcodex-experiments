package com.demo.csvupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the CSV Upload Demo application.
 *
 * <p>Demonstrates techniques for handling large CSV file uploads:
 * <ul>
 *   <li>Streaming multipart upload (file never fully loaded into heap)</li>
 *   <li>OpenCSV line-by-line streaming parse</li>
 *   <li>Hibernate JDBC batch inserts (configurable batch size)</li>
 *   <li>Asynchronous background processing with @Async</li>
 *   <li>Real-time job progress tracking via polling</li>
 *   <li>Error collection and reporting per row</li>
 * </ul>
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableAsync
public class CsvUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsvUploadApplication.class, args);
    }
}

