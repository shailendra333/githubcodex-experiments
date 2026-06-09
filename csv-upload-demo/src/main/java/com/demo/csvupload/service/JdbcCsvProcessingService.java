package com.demo.csvupload.service;

import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.model.ProcessingJob.Status;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-JDBC CSV processing service.
 *
 * <p>Architecturally identical to {@link CsvProcessingService} (same streaming
 * CSV loop, same batch size, same async executor) but delegates to
 * {@link JdbcBatchUpsertService} instead of {@link BatchUpsertService}.
 *
 * <h2>What is NOT present vs the JPA version</h2>
 * <ul>
 *   <li>No {@code Customer} entity objects — raw {@code String[]} rows go straight
 *       to JDBC parameter arrays.</li>
 *   <li>No {@code entityManager.flush()+clear()} — there is no Hibernate session.</li>
 *   <li>No projection lookup — SQLite's {@code ON CONFLICT DO UPDATE} handles upsert
 *       without a preceding SELECT.</li>
 * </ul>
 *
 * <h2>What IS the same</h2>
 * <ul>
 *   <li>Same 128 KB {@link BufferedReader} streaming parse (Technique 1).</li>
 *   <li>Same 500-row batch size (Technique 4).</li>
 *   <li>Same heap telemetry via {@link MemoryMXBean} (Technique 5).</li>
 *   <li>Same {@code @Async} thread pool.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcCsvProcessingService {

    private final JdbcBatchUpsertService jdbcBatchUpsertService;

    @Value("${csv.processing.batch-size:500}")
    private int batchSize;

    private static final int HEAP_LOG_EVERY_N_BATCHES = 100;
    private final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    @Async("csvProcessingExecutor")
    public void processAsync(InputStream inputStream, ProcessingJob job) {
        job.setStatus(Status.PROCESSING);
        log.info("[Job {}] [JDBC] Starting CSV upsert for: {}", job.getJobId(), job.getFileName());
        logHeap(job, "startup");
        long t0 = System.currentTimeMillis();

        try {
            processCsv(inputStream, job);
            long elapsed = System.currentTimeMillis() - t0;
            job.setDurationMs(elapsed);
            job.setStatus(Status.COMPLETED);
            job.setCompletedAt(java.time.LocalDateTime.now());
            log.info("[Job {}] [JDBC] Completed in {} ms — processed={} failed={} ({} rows/sec)",
                    job.getJobId(), elapsed,
                    job.getProcessedCount(), job.getFailedCount(),
                    job.getThroughputRowsPerSec());
            logHeap(job, "completion");
        } catch (Exception e) {
            job.setDurationMs(System.currentTimeMillis() - t0);
            job.setStatus(Status.FAILED);
            job.setFatalError(e.getMessage());
            log.error("[Job {}] [JDBC] Fatal error", job.getJobId(), e);
        } finally {
            try { inputStream.close(); } catch (IOException ignored) {}
        }
    }

    private void processCsv(InputStream inputStream, ProcessingJob job)
            throws IOException, CsvValidationException {

        RFC4180Parser csvParser = new RFC4180ParserBuilder().build();

        // Same 128 KB streaming BufferedReader as the JPA version (Technique 1)
        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8), 128 * 1024);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(csvParser)
                     .withSkipLines(1)
                     .build()) {

            // Raw String[] rows — NO Customer objects created
            List<String[]> rawBatch = new ArrayList<>(batchSize);
            String[] row;
            long rowNum  = 1;
            long batchNo = 0;

            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                job.setTotalRows(rowNum);

                // Basic validation before queuing (mirrors JPA mapRow validation)
                if (row.length < 8 || col(row, 1).isBlank() || col(row, 2).isBlank()) {
                    job.addError(rowNum, row.length < 8
                            ? "Expected 8 columns, got " + row.length
                            : "firstName or lastName is required");
                    continue;
                }

                rawBatch.add(row);

                if (rawBatch.size() >= batchSize) {
                    batchNo++;
                    jdbcBatchUpsertService.upsertBatch(rawBatch, job, batchNo);
                    rawBatch.clear();

                    if (batchNo % HEAP_LOG_EVERY_N_BATCHES == 0) {
                        logHeap(job, "batch-" + batchNo);
                    }
                }
            }

            if (!rawBatch.isEmpty()) {
                jdbcBatchUpsertService.upsertBatch(rawBatch, job, ++batchNo);
            }
        }
    }

    private void logHeap(ProcessingJob job, String phase) {
        long used      = memBean.getHeapMemoryUsage().getUsed()      / (1024 * 1024);
        long committed = memBean.getHeapMemoryUsage().getCommitted()  / (1024 * 1024);
        long max       = memBean.getHeapMemoryUsage().getMax()        / (1024 * 1024);
        int  pct       = max > 0 ? (int)((used * 100) / max) : 0;
        if (used > job.getPeakHeapMb()) job.setPeakHeapMb(used);
        log.info("[Job {}] [JDBC] [{}] Heap: used={} MB | committed={} MB | max={} MB  ({}% used)",
                job.getJobId(), phase, used, committed, max, pct);
    }

    private String col(String[] cols, int idx) {
        return (idx < cols.length && cols[idx] != null) ? cols[idx].trim() : "";
    }
}

