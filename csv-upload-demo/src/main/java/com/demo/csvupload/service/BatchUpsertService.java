package com.demo.csvupload.service;

import com.demo.csvupload.model.Customer;
import com.demo.csvupload.model.CustomerIdProjection;
import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dedicated Spring bean for batch upsert operations.
 *
 * <p><strong>Why a separate bean?</strong> Spring's {@code @Transactional} works via
 * AOP proxies.  When a method calls another {@code @Transactional} method <em>on the
 * same bean</em> ({@code this.upsertBatch(...)}) the call bypasses the proxy and no
 * transaction is started — a classic Spring self-invocation pitfall.  By placing the
 * upsert logic here, {@link CsvProcessingService} calls through the proxy and the
 * {@code REQUIRES_NEW} transaction is correctly created for every batch.
 *
 * <p>This also means {@link EntityManager#flush()} and {@link EntityManager#clear()}
 * execute inside an active transaction, which is required.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchUpsertService {

    private final CustomerRepository customerRepository;

    /**
     * TECHNIQUE 3 — Transaction-scoped EntityManager.
     * {@code flush()} writes any dirty state; {@code clear()} releases all entity
     * references from the first-level cache so they become eligible for GC immediately
     * after the batch commits — preventing heap accumulation across millions of rows.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Upserts one batch of customers inside its own {@code REQUIRES_NEW} transaction.
     *
     * <h3>Memory technique summary</h3>
     * <ol>
     *   <li>TECHNIQUE 2 — projection lookup: loads only {@code (externalId, id)} proxy
     *       pairs (~200 B each), not full Customer entities (~500 B each).</li>
     *   <li>Merges existing DB ids onto incoming entities → Hibernate issues UPDATE
     *       for existing rows and INSERT for new ones in a single {@code saveAll()} call.</li>
     *   <li>TECHNIQUE 3 — {@code entityManager.flush()} + {@code clear()} after
     *       {@code saveAll()}: commits remaining dirty state and detaches all entities
     *       from the session cache, making them immediately GC-eligible.</li>
     * </ol>
     *
     * @return long[]{inserts, updates} counts for telemetry
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long[] upsertBatch(List<Customer> batch, ProcessingJob job, long batchNo) {
        long inserts = 0, updates = 0;
        try {
            // ── Step 1: projection-based lookup (TECHNIQUE 2) ──────────────
            Set<String> externalIds = batch.stream()
                    .map(Customer::getExternalId)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toSet());

            Map<String, Long> existingIdMap = customerRepository
                    .findIdsByExternalIdIn(externalIds)
                    .stream()
                    .collect(Collectors.toMap(
                            CustomerIdProjection::getExternalId,
                            CustomerIdProjection::getId));

            // ── Step 2: stamp DB ids onto existing entities → UPDATE not INSERT
            for (Customer customer : batch) {
                String extId = customer.getExternalId();
                if (extId != null && existingIdMap.containsKey(extId)) {
                    customer.setId(existingIdMap.get(extId));
                    updates++;
                } else {
                    inserts++;
                }
            }

            // ── Step 3: single JDBC batch (INSERT for new, UPDATE for existing)
            customerRepository.saveAll(batch);

            // ── Step 4: flush + clear (TECHNIQUE 3) ────────────────────────
            // IMPORTANT: called inside an active @Transactional context (this bean is
            // a separate Spring proxy, so REQUIRES_NEW is honoured).
            // flush() → flushes any remaining dirty SQL to JDBC
            // clear() → detaches ALL session entities → eligible for GC immediately
            entityManager.flush();
            entityManager.clear();

            log.debug("[Job {}] Batch {}: ins={} upd={} running={}",
                    job.getJobId(), batchNo, inserts, updates,
                    job.getProcessedCount() + inserts + updates);

        } catch (Exception e) {
            log.warn("[Job {}] Batch {} failed: {}", job.getJobId(), batchNo, e.getMessage());
            batch.forEach(c -> job.addError(-1, "Batch upsert failed: " + e.getMessage()));
            return new long[]{0, 0};
        }
        return new long[]{inserts, updates};
    }
}

