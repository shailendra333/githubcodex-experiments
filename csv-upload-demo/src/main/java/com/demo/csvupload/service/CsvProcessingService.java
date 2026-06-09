package com.demo.csvupload.service;

import com.demo.csvupload.model.Customer;
import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.model.ProcessingJob.Status;
import com.demo.csvupload.repository.CustomerRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates streaming CSV parsing and delegates each batch to
 * {@link BatchUpsertService} which runs in its own Spring-proxied transaction.
 *
 * <h2>500 MB Heap Techniques</h2>
 * <ol>
 *   <li><b>Streaming CSV parse</b> — 128 KB BufferedReader; the file never enters heap.</li>
 *   <li><b>Projection upsert lookup</b> — in {@link BatchUpsertService}: only
 *       {@code (externalId, id)} proxy pairs are loaded, not full entities.</li>
 *   <li><b>EntityManager flush + clear</b> — in {@link BatchUpsertService}: session cache
 *       is cleared after every batch so entities are GC-eligible immediately.</li>
 *   <li><b>Batch size 500</b> — lower per-TX peak heap (~150 KB vs ~300 KB at 1000).</li>
 *   <li><b>Heap telemetry every 100 batches</b> — observable without a profiler.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvProcessingService {

    private final CustomerRepository customerRepository;

    /**
     * Separate Spring bean so {@code @Transactional(REQUIRES_NEW)} on
     * {@link BatchUpsertService#upsertBatch} goes through the AOP proxy
     * (avoids the Spring self-invocation pitfall).
     */
    private final BatchUpsertService batchUpsertService;

    @Value("${csv.processing.batch-size:500}")
    private int batchSize;

    private static final int HEAP_LOG_EVERY_N_BATCHES = 100;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    // -----------------------------------------------------------------------

    @Async("csvProcessingExecutor")
    public void processAsync(InputStream inputStream, ProcessingJob job) {
        job.setStatus(Status.PROCESSING);
        log.info("[Job {}] [JPA] Starting CSV upsert for: {}", job.getJobId(), job.getFileName());
        logHeap(job, "startup");
        long t0 = System.currentTimeMillis();

        try {
            processCsv(inputStream, job);
            long elapsed = System.currentTimeMillis() - t0;
            job.setDurationMs(elapsed);
            job.setStatus(Status.COMPLETED);
            job.setCompletedAt(java.time.LocalDateTime.now());
            log.info("[Job {}] [JPA] Completed in {} ms — inserted={} updated={} failed={} ({} rows/sec)",
                    job.getJobId(), elapsed, job.getInsertedCount(), job.getUpdatedCount(),
                    job.getFailedCount(), job.getThroughputRowsPerSec());
            logHeap(job, "completion");
        } catch (Exception e) {
            job.setDurationMs(System.currentTimeMillis() - t0);
            job.setStatus(Status.FAILED);
            job.setFatalError(e.getMessage());
            log.error("[Job {}] [JPA] Fatal error", job.getJobId(), e);
        } finally {
            try { inputStream.close(); } catch (IOException ignored) {}
        }
    }

    // -----------------------------------------------------------------------

    private void processCsv(InputStream inputStream, ProcessingJob job)
            throws IOException, CsvValidationException {

        RFC4180Parser csvParser = new RFC4180ParserBuilder().build();

        // TECHNIQUE 1: 128 KB buffer — only one decoded CSV line in heap at a time
        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8), 128 * 1024);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(csvParser)
                     .withSkipLines(1)
                     .build()) {

            List<Customer> batch = new ArrayList<>(batchSize);
            String[] row;
            long rowNum  = 1;
            long batchNo = 0;

            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                job.setTotalRows(rowNum);

                try {
                    batch.add(mapRow(row));
                } catch (IllegalArgumentException e) {
                    job.addError(rowNum, e.getMessage());
                    continue;
                }

                if (batch.size() >= batchSize) {
                    batchNo++;
                    flushBatch(batch, job, batchNo);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                flushBatch(batch, job, ++batchNo);
            }
        }
    }

    /**
     * Delegates to {@link BatchUpsertService#upsertBatch} (a separate Spring bean)
     * so the {@code @Transactional(REQUIRES_NEW)} annotation is honoured by the AOP proxy,
     * and updates the job counters from the returned insert/update counts.
     */
    private void flushBatch(List<Customer> batch, ProcessingJob job, long batchNo) {
        // Call through the Spring proxy → REQUIRES_NEW transaction is created,
        // flush()+clear() inside execute inside that transaction.
        long[] counts = batchUpsertService.upsertBatch(batch, job, batchNo);
        long ins = counts[0], upd = counts[1];

        batch.forEach(c -> job.incrementProcessed());
        for (long i = 0; i < ins; i++) job.incrementInserted();
        for (long i = 0; i < upd; i++) job.incrementUpdated();

        // TECHNIQUE 5: heap telemetry every 100 batches
        if (batchNo % HEAP_LOG_EVERY_N_BATCHES == 0) {
            logHeap(job, "batch-" + batchNo);
        }
    }

    // -----------------------------------------------------------------------

    private Customer mapRow(String[] cols) {
        if (cols.length < 8) {
            throw new IllegalArgumentException("Expected 8 columns, got " + cols.length);
        }
        String firstName = col(cols, 1);
        String lastName  = col(cols, 2);
        if (firstName.isBlank()) throw new IllegalArgumentException("firstName is required");
        if (lastName.isBlank())  throw new IllegalArgumentException("lastName is required");

        LocalDate regDate = null;
        String rawDate = col(cols, 7);
        if (!rawDate.isBlank()) {
            try {
                regDate = LocalDate.parse(rawDate, DATE_FMT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Invalid registrationDate '" + rawDate + "' – expected yyyy-MM-dd");
            }
        }

        return Customer.builder()
                .externalId(col(cols, 0))
                .firstName(firstName)
                .lastName(lastName)
                .email(col(cols, 3))
                .phone(col(cols, 4))
                .city(col(cols, 5))
                .country(col(cols, 6))
                .registrationDate(regDate)
                .build();
    }

    /**
     * TECHNIQUE 5 — Heap telemetry logged every 100 batches.
     * Example: {@code [Job abc] [batch-100] Heap: used=187 MB | committed=256 MB | max=512 MB (36%)}
     */
    private void logHeap(ProcessingJob job, String phase) {
        long used      = memBean.getHeapMemoryUsage().getUsed()      / (1024 * 1024);
        long committed = memBean.getHeapMemoryUsage().getCommitted()  / (1024 * 1024);
        long max       = memBean.getHeapMemoryUsage().getMax()        / (1024 * 1024);
        int  pct       = max > 0 ? (int)((used * 100) / max) : 0;
        // track peak
        if (used > job.getPeakHeapMb()) job.setPeakHeapMb(used);
        log.info("[Job {}] [JPA] [{}] Heap: used={} MB | committed={} MB | max={} MB  ({}% used)",
                job.getJobId(), phase, used, committed, max, pct);
    }

    private String col(String[] cols, int idx) {
        return (idx < cols.length && cols[idx] != null) ? cols[idx].trim() : "";
    }
}
