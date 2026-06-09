package com.demo.csvupload.service;

import com.demo.csvupload.model.ProcessingJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Pure JDBC batch upsert — the JPA-free alternative to {@link BatchUpsertService}.
 *
 * <h2>Key differences vs JPA</h2>
 * <ul>
 *   <li><b>One SQL round-trip per batch</b> — SQLite's {@code ON CONFLICT DO UPDATE}
 *       handles both INSERT and UPDATE in a single statement.
 *       JPA needs a SELECT first to determine whether to insert or update.</li>
 *   <li><b>No entity objects</b> — raw CSV {@code String[]} rows are converted directly
 *       to {@code Object[][]} JDBC parameter arrays (~10 KB per 500-row batch vs
 *       ~150 KB for 500 Customer entities).</li>
 *   <li><b>No first-level cache</b> — there is no Hibernate session, no dirty checking,
 *       no snapshot copies. {@code flush()+clear()} is not needed.</li>
 *   <li><b>No AOP proxy concern</b> — {@code @Transactional} here works normally because
 *       {@link JdbcCsvProcessingService} calls this separate bean through the proxy.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcBatchUpsertService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * SQLite 3.24+ UPSERT syntax.
     * {@code excluded.col} refers to the value that was proposed for insertion.
     * When {@code external_id} conflicts with an existing row the SET clause fires;
     * otherwise a new row is inserted. Either way: one PreparedStatement execution.
     */
    private static final String UPSERT_SQL = """
            INSERT INTO customers
              (external_id, first_name, last_name, email, phone,
               city, country, registration_date, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, DATETIME('now'))
            ON CONFLICT(external_id) DO UPDATE SET
              first_name        = excluded.first_name,
              last_name         = excluded.last_name,
              email             = excluded.email,
              phone             = excluded.phone,
              city              = excluded.city,
              country           = excluded.country,
              registration_date = excluded.registration_date
            """;

    /**
     * Upserts one batch in its own {@code REQUIRES_NEW} transaction.
     *
     * <p>Each element of {@code rawBatch} is the raw {@code String[]} from OpenCSV —
     * the same array that JPA would parse into a Customer entity.
     * Here we skip entity creation entirely and map directly to JDBC parameters.
     *
     * @return number of rows successfully upserted
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int upsertBatch(List<String[]> rawBatch, ProcessingJob job, long batchNo) {
        try {
            /*
             * Build JDBC parameter matrix.
             * Object[batchSize][8] — total ~10-15 KB for 500 rows.
             * Compare to JPA: List<Customer>[500] — ~150 KB of entity objects
             * plus Hibernate snapshot copies.
             */
            Object[][] params = new Object[rawBatch.size()][];
            for (int i = 0; i < rawBatch.size(); i++) {
                String[] c = rawBatch.get(i);
                params[i] = new Object[]{
                        col(c, 0),           // external_id
                        col(c, 1),           // first_name
                        col(c, 2),           // last_name
                        col(c, 3),           // email
                        col(c, 4),           // phone
                        col(c, 5),           // city
                        col(c, 6),           // country
                        parseDate(col(c, 7)) // registration_date (sql.Date or null)
                };
            }

            /*
             * One batchUpdate call → one PreparedStatement.executeBatch() call.
             * No SELECT, no entity merge, no flush/clear needed.
             *
             * Note: SQLite's ON CONFLICT DO UPDATE returns rowsAffected=1 for both
             * new inserts AND updates, so we cannot distinguish them from JDBC alone.
             * We count all as "processed" — the benchmark page shows "N/A" for
             * insert/update breakdown on the JDBC side.
             */
            int[] results = jdbcTemplate.batchUpdate(UPSERT_SQL, List.of(params));

            for (int ignored : results) {
                job.incrementProcessed();
                job.incrementInserted(); // best-effort; see note above
            }

            log.debug("[Job {}] [JDBC] Batch {}: {} rows upserted (total={})",
                    job.getJobId(), batchNo, results.length, job.getProcessedCount());
            return results.length;

        } catch (Exception e) {
            log.warn("[Job {}] [JDBC] Batch {} failed: {}",
                    job.getJobId(), batchNo, e.getMessage());
            rawBatch.forEach(r -> job.addError(-1, "JDBC batch failed: " + e.getMessage()));
            return 0;
        }
    }

    private String col(String[] cols, int idx) {
        return (idx < cols.length && cols[idx] != null) ? cols[idx].trim() : "";
    }

    private Date parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Date.valueOf(LocalDate.parse(raw)); }
        catch (Exception e) { return null; }
    }
}
