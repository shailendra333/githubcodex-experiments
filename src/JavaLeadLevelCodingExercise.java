import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Java Lead-Level Coding Exercise
 *
 * Objective:
 * Evaluate mastery of collections, concurrent collections, multithreading, and Java Streams.
 *
 * Scenario:
 * You are implementing a real-time metrics aggregator for an e-commerce platform.
 * Multiple ingestion threads receive transaction events and publish them into a shared store.
 * You must produce ranked analytics safely and efficiently under concurrency.
 *
 * Candidate Tasks:
 * 1) Complete ingest(...) using appropriate concurrent collections.
 * 2) Complete topSpenders(...) using Java streams and deterministic tie-breaking.
 * 3) Complete riskyUsers(...) using stream transformations.
 * 4) Ensure runSimulation(...) runs ingestion in parallel and safely terminates.
 *
 * Constraints:
 * - Assume high write contention.
 * - Avoid global coarse-grained synchronization when not needed.
 * - Keep read methods side-effect free.
 *
 * ------------------------
 * Interviewer Evaluation Notes
 * ------------------------
 * Collections:
 * - Look for correct choices: e.g., ConcurrentHashMap + LongAdder/ConcurrentLinkedQueue.
 * - Penalize non-thread-safe structures (HashMap/ArrayList) in shared mutable paths.
 *
 * Concurrent Collections + Multithreading:
 * - Check whether candidate understands atomic update patterns (computeIfAbsent, merge).
 * - Check thread-pool sizing, shutdown, interruption handling, and failure propagation.
 * - Bonus if candidate discusses backpressure and bounded queues.
 *
 * Streams:
 * - Check grouping/filtering/sorting clarity and complexity awareness.
 * - Verify stable deterministic output under tie cases.
 * - Watch for avoidable repeated scans and poor boxing choices.
 *
 * Code Quality / Lead-Level Signals:
 * - Clear invariants, meaningful method boundaries, and trade-off explanations.
 * - Mentions performance hot spots and memory behavior.
 * - Provides concise tests or example assertions.
 */
public class JavaLeadLevelCodingExercise {

    /**
     * Immutable event input.
     */
    record TransactionEvent(String userId, String category, double amount, boolean chargeback, Instant eventTime) {}

    /**
     * Analytics projection for top spenders.
     */
    record UserSpend(String userId, double totalAmount) {}

    /**
     * Analytics projection for risk scoring.
     */
    record UserRisk(String userId, double totalAmount, long chargebackCount) {}

    /**
     * Shared in-memory state updated by multiple ingestion threads.
     *
     * Implementation guidance for candidate:
     * - Keep this map thread-safe.
     * - Each user should support concurrent increments with low contention.
     */
    private final ConcurrentHashMap<String, UserAccumulator> userMetrics = new ConcurrentHashMap<>();

    /**
     * Candidate TODO #1:
     * Ingest one event into concurrent metrics state.
     *
     * Expected behavior:
     * - Aggregate total spend by user.
     * - Track chargeback count by user.
     * - Track categories seen per user.
     */
    public void ingest(TransactionEvent event) {
        userMetrics.computeIfAbsent(event.userId(), ignored -> new UserAccumulator())
                .accumulate(event);
    }

    /**
     * Candidate TODO #2:
     * Return top N spenders sorted by:
     * 1) totalAmount descending
     * 2) userId ascending (deterministic tie-break)
     */
    public List<UserSpend> topSpenders(int n) {
        return userMetrics.entrySet().stream()
                .map(entry -> new UserSpend(entry.getKey(), entry.getValue().totalAmount()))
                .sorted(Comparator.comparingDouble(UserSpend::totalAmount).reversed()
                        .thenComparing(UserSpend::userId))
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Candidate TODO #3:
     * Return risky users where:
     * - totalAmount >= minTotal
     * - chargebackCount >= minChargebacks
     * Sorted by totalAmount descending then userId.
     */
    public List<UserRisk> riskyUsers(double minTotal, long minChargebacks) {
        return userMetrics.entrySet().stream()
                .map(entry -> new UserRisk(
                        entry.getKey(),
                        entry.getValue().totalAmount(),
                        entry.getValue().chargebackCount()))
                .filter(risk -> risk.totalAmount() >= minTotal)
                .filter(risk -> risk.chargebackCount() >= minChargebacks)
                .sorted(Comparator.comparingDouble(UserRisk::totalAmount).reversed()
                        .thenComparing(UserRisk::userId))
                .collect(Collectors.toList());
    }

    /**
     * Candidate TODO #4:
     * Run parallel ingestion for multiple batches.
     *
     * Interviewer checks:
     * - Proper task submission and wait for completion.
     * - Correct shutdown/awaitTermination semantics.
     * - Thread interruption policy.
     */
    public void runSimulation(List<List<TransactionEvent>> batches) throws InterruptedException {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Void>> tasks = batches.stream()
                    .map(batch -> (Callable<Void>) () -> {
                        for (TransactionEvent event : batch) {
                            ingest(event);
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Batch ingestion failed", e.getCause());
                }
            }
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
    }

    /**
     * Internal mutable accumulator per user.
     *
     * Why these structures:
     * - LongAdder: scalable counters under contention.
     * - ConcurrentHashMap.newKeySet(): lock-free-ish concurrent set of categories.
     */
    static class UserAccumulator {
        private final DoubleAdderAdapter totalAmount = new DoubleAdderAdapter();
        private final LongAdder chargebackCount = new LongAdder();
        private final ConcurrentHashMap.KeySetView<String, Boolean> categories = ConcurrentHashMap.newKeySet();

        void accumulate(TransactionEvent event) {
            totalAmount.add(event.amount());
            if (event.chargeback()) {
                chargebackCount.increment();
            }
            categories.add(event.category());
        }

        double totalAmount() {
            return totalAmount.sum();
        }

        long chargebackCount() {
            return chargebackCount.sum();
        }

        @SuppressWarnings("unused")
        int categoryCount() {
            return categories.size();
        }
    }

    /**
     * Minimal adapter to keep exercise JDK-friendly while still discussing contention.
     * Replace with java.util.concurrent.atomic.DoubleAdder if target JDK guarantees it.
     */
    static class DoubleAdderAdapter {
        private final LongAdder scaled = new LongAdder();

        void add(double value) {
            scaled.add(Math.round(value * 100));
        }

        double sum() {
            return scaled.sum() / 100.0;
        }
    }

    /**
     * Optional smoke run to quickly demo behavior.
     */
    public static void main(String[] args) throws InterruptedException {
        JavaLeadLevelCodingExercise exercise = new JavaLeadLevelCodingExercise();

        List<TransactionEvent> shardA = List.of(
                new TransactionEvent("u1", "BOOKS", 120.50, false, Instant.now()),
                new TransactionEvent("u2", "GAMES", 950.00, true, Instant.now()),
                new TransactionEvent("u1", "BOOKS", 99.50, false, Instant.now())
        );
        List<TransactionEvent> shardB = List.of(
                new TransactionEvent("u3", "TRAVEL", 1300.00, true, Instant.now()),
                new TransactionEvent("u2", "GAMES", 150.00, false, Instant.now()),
                new TransactionEvent("u3", "TRAVEL", 250.00, false, Instant.now())
        );

        exercise.runSimulation(List.of(shardA, shardB));

        System.out.println("Top spenders: " + exercise.topSpenders(2));
        System.out.println("Risky users: " + exercise.riskyUsers(1000, 1));

        // Interviewer sample expected output shape (values may vary only by ordering rules):
        // Top spenders: [UserSpend[userId=u3, totalAmount=1550.0], UserSpend[userId=u2, totalAmount=1100.0]]
        // Risky users: [UserRisk[userId=u3, totalAmount=1550.0, chargebackCount=1], UserRisk[userId=u2, totalAmount=1100.0, chargebackCount=1]]
    }
}
