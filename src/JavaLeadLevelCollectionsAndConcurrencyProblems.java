import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tough, lead-level Java interview-style problems focused on:
 * 1) Collections
 * 2) Multithreading / concurrency
 * 3) Stream pipelines
 *
 * Each problem includes:
 * - A concise problem statement
 * - A fully-commented solution
 * - A small demo to show behavior/output
 */
public class JavaLeadLevelCollectionsAndConcurrencyProblems {

    public static void main(String[] args) throws Exception {
        System.out.println("=== LEAD-LEVEL JAVA PROBLEMS (Collections + Multithreading + Streams) ===\n");

        problem1LruCacheDesign();
        problem2TopKFrequentUsingHeap();
        problem3ParallelLogAggregation();
        problem4OrderedTaskExecutionWithCompletableFuture();
        problem5StreamBasedFraudSignals();
    }

    // ---------------------------------------------------------------------
    // PROBLEM 1 (Collections): LRU Cache with O(1) get/put
    // ---------------------------------------------------------------------

    /**
     * Problem statement:
     * Design an in-memory LRU cache with O(1) get and put.
     * When size exceeds capacity, remove the least-recently-used key.
     */
    private static void problem1LruCacheDesign() {
        System.out.println("[Problem 1] LRU Cache with O(1) get/put");

        // We build a tiny reusable LRU cache implementation.
        LruCache<String, Integer> cache = new LruCache<>(3);

        // Insert initial values.
        cache.put("A", 10);
        cache.put("B", 20);
        cache.put("C", 30);

        // Access "A" so A becomes most recently used.
        cache.get("A");

        // Insert D; cache size exceeds capacity => least-recently-used key is evicted.
        cache.put("D", 40);

        // Since A was recently used, B should have been evicted.
        System.out.println("Cache content (LRU -> MRU): " + cache.snapshotInAccessOrder());
        System.out.println();
    }

    /**
     * LRU cache implementation using LinkedHashMap(accessOrder=true).
     *
     * Why this works:
     * - LinkedHashMap can maintain entries by access-order (not insertion-order)
     * - get()/put() updates recency when accessOrder=true
     * - removeEldestEntry lets us evict exactly when size exceeds capacity
     */
    static class LruCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> map;

        LruCache(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be > 0");
            }
            this.capacity = capacity;

            // Initial capacity and load-factor are configurable.
            // accessOrder=true is the key point for LRU behavior.
            this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    // Auto-evict the least recently used item after insertion if oversized.
                    return size() > LruCache.this.capacity;
                }
            };
        }

        V get(K key) {
            // O(1) average-time lookup and recency update.
            return map.get(key);
        }

        void put(K key, V value) {
            // O(1) average-time insertion/update.
            map.put(key, value);
        }

        List<Map.Entry<K, V>> snapshotInAccessOrder() {
            // Snapshot to show deterministic state in demos/logging.
            return new ArrayList<>(map.entrySet());
        }
    }

    // ---------------------------------------------------------------------
    // PROBLEM 2 (Collections + Streams): Top-K frequent elements
    // ---------------------------------------------------------------------

    /**
     * Problem statement:
     * Given a large integer list, return top-K most frequent elements.
     * If frequencies tie, prioritize smaller value for deterministic output.
     *
     * Lead-level angle:
     * - Frequency count using HashMap (O(n))
     * - Min-heap of size K to keep memory and sorting under control
     */
    private static void problem2TopKFrequentUsingHeap() {
        System.out.println("[Problem 2] Top-K frequent elements using map + heap");

        List<Integer> input = Arrays.asList(4, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 2, 2, 2);
        int k = 3;

        List<Integer> topK = topKFrequent(input, k);
        System.out.println("Input: " + input);
        System.out.println("Top " + k + " frequent values: " + topK);
        System.out.println();
    }

    private static List<Integer> topKFrequent(List<Integer> values, int k) {
        // 1) Build frequency map in one linear pass.
        Map<Integer, Integer> frequency = new HashMap<>();
        for (int v : values) {
            frequency.merge(v, 1, Integer::sum);
        }

        // 2) Min-heap keeps the "best" K candidates.
        //    Heap root is the weakest candidate among kept entries.
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap = new PriorityQueue<>(
                Comparator
                        .comparingInt((Map.Entry<Integer, Integer> e) -> e.getValue()) // lower freq is weaker
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder())   // for equal freq, larger key weaker
        );

        for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                // Remove weakest to keep heap size bounded by K.
                minHeap.poll();
            }
        }

        // 3) Heap currently has top K, but not final order.
        //    Sort descending by frequency, then ascending by key for deterministic output.
        return minHeap.stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<Integer, Integer> e) -> e.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // PROBLEM 3 (Multithreading): Parallel log aggregation
    // ---------------------------------------------------------------------

    /**
     * Problem statement:
     * Aggregate event-type counts from multiple shards concurrently.
     * Must be thread-safe, scalable, and avoid global synchronized bottlenecks.
     */
    private static void problem3ParallelLogAggregation() throws InterruptedException {
        System.out.println("[Problem 3] Concurrent log aggregation with ExecutorService + LongAdder");

        // Simulated sharded logs (e.g., partitions from different nodes).
        List<List<String>> logShards = List.of(
                List.of("LOGIN", "SEARCH", "SEARCH", "PAYMENT"),
                List.of("LOGIN", "LOGIN", "SEARCH", "LOGOUT"),
                List.of("SEARCH", "PAYMENT", "PAYMENT", "LOGIN")
        );

        Map<String, Long> counts = aggregateEventCounts(logShards);
        System.out.println("Aggregated counts: " + counts);
        System.out.println();
    }

    private static Map<String, Long> aggregateEventCounts(List<List<String>> shards) throws InterruptedException {
        // Use a pool sized by available processors for CPU-bound counting tasks.
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // ConcurrentHashMap + LongAdder is a high-throughput pattern for hot counters.
        ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

        try {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (List<String> shard : shards) {
                tasks.add(() -> {
                    // Each task processes one shard independently.
                    for (String event : shard) {
                        // computeIfAbsent is atomic in ConcurrentHashMap.
                        counters.computeIfAbsent(event, key -> new LongAdder()).increment();
                    }
                    return null;
                });
            }

            // invokeAll blocks until every task completes.
            List<Future<Void>> futures = pool.invokeAll(tasks);

            // Explicitly surface task failures (ExecutionException wrapped by get).
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException ex) {
                    throw new RuntimeException("Log aggregation task failed", ex.getCause());
                }
            }

            // Convert mutable counters to immutable numeric snapshot.
            return counters.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));

        } finally {
            // Always shutdown to avoid leaked threads.
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
    }

    // ---------------------------------------------------------------------
    // PROBLEM 4 (Multithreading): Ordered async pipeline
    // ---------------------------------------------------------------------

    /**
     * Problem statement:
     * Execute asynchronous steps (validate -> enrich -> persist) and ensure
     * order is preserved for each request while still being non-blocking.
     */
    private static void problem4OrderedTaskExecutionWithCompletableFuture() {
        System.out.println("[Problem 4] Ordered async workflow with CompletableFuture");

        Order order = new Order("ORD-9001", 249.99, LocalDateTime.now());

        // Build pipeline; each stage starts only after previous stage finishes.
        CompletableFuture<String> pipeline = CompletableFuture
                .supplyAsync(() -> validate(order))
                .thenApply(JavaLeadLevelCollectionsAndConcurrencyProblems::enrich)
                .thenApply(JavaLeadLevelCollectionsAndConcurrencyProblems::persist);

        // join() rethrows unchecked CompletionException on failure.
        String result = pipeline.join();
        System.out.println("Pipeline result: " + result);
        System.out.println();
    }

    private static String validate(Order order) {
        if (order.amount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return "VALID:" + order.id();
    }

    private static String enrich(String validatedToken) {
        // Simulate deterministic transformation.
        return validatedToken + "|RISK=LOW";
    }

    private static String persist(String enrichedToken) {
        // Simulate persistence result.
        return "PERSISTED:" + enrichedToken;
    }

    record Order(String id, double amount, LocalDateTime createdAt) {}

    // ---------------------------------------------------------------------
    // PROBLEM 5 (Streams): Fraud signal extraction from transactions
    // ---------------------------------------------------------------------

    /**
     * Problem statement:
     * From transaction data, produce a ranked list of suspicious users where:
     * - total amount > threshold
     * - at least one chargeback exists
     * Rank by total descending, then by user id.
     */
    private static void problem5StreamBasedFraudSignals() {
        System.out.println("[Problem 5] Stream-based fraud signal extraction");

        List<Transaction> txns = List.of(
                new Transaction("u1", 400, false),
                new Transaction("u1", 750, true),
                new Transaction("u2", 200, false),
                new Transaction("u2", 950, false),
                new Transaction("u3", 1000, true),
                new Transaction("u3", 250, false)
        );

        List<UserRisk> suspicious = suspiciousUsers(txns, 1000);
        System.out.println("Suspicious users: " + suspicious);
        System.out.println();
    }

    private static List<UserRisk> suspiciousUsers(List<Transaction> txns, double threshold) {
        // Group transactions by user first; this avoids repeated scans per user.
        Map<String, List<Transaction>> byUser = txns.stream()
                .collect(Collectors.groupingBy(Transaction::userId));

        // Transform each user group into a UserRisk aggregate and filter/rank.
        return byUser.entrySet().stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<Transaction> userTxns = entry.getValue();

                    // Sum using primitive stream to avoid boxing overhead.
                    double total = userTxns.stream().mapToDouble(Transaction::amount).sum();

                    // anyMatch short-circuits on first chargeback.
                    boolean hasChargeback = userTxns.stream().anyMatch(Transaction::chargeback);

                    return new UserRisk(userId, total, hasChargeback);
                })
                .filter(risk -> risk.totalAmount() > threshold)
                .filter(UserRisk::hasChargeback)
                .sorted(Comparator
                        .comparingDouble(UserRisk::totalAmount).reversed()
                        .thenComparing(UserRisk::userId))
                .collect(Collectors.toList());
    }

    record Transaction(String userId, double amount, boolean chargeback) {}

    record UserRisk(String userId, double totalAmount, boolean hasChargeback) {}
}
