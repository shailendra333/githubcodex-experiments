# Java Concurrent Collections: Usage & Evaluation Guidelines

This guide summarizes the core **concurrent data structures** in `java.util.concurrent` and related packages, how to choose between them, and the trade-offs interviewers usually expect senior candidates to understand.

---

## 1) Quick selection guide

Use this fast path first:

- Need a **thread-safe map** with high read/write concurrency → `ConcurrentHashMap`
- Need an **ordered, sorted map/set** with concurrent access → `ConcurrentSkipListMap` / `ConcurrentSkipListSet`
- Need a **lock-free stack** for non-blocking push/pop → `ConcurrentLinkedDeque` (as stack) or `ConcurrentLinkedQueue`
- Need a **bounded producer/consumer queue** with backpressure → `ArrayBlockingQueue` or `LinkedBlockingQueue`
- Need handoff between producer and consumer with **zero buffering** → `SynchronousQueue`
- Need delayed/scheduled item execution → `DelayQueue`
- Need tasks prioritized by comparator/natural order → `PriorityBlockingQueue`
- Need frequent reads, rare writes for a list-like structure → `CopyOnWriteArrayList`
- Need atomic counters/metrics under contention → `LongAdder` / `LongAccumulator`

---

## 2) Available concurrent collections and structures

## 2.1 Maps and sets

### `ConcurrentHashMap<K, V>`

**When to use**
- General-purpose concurrent map.
- High-throughput caches, registries, dedup sets (`newKeySet()`), frequency counts.

**Pros**
- Excellent scalability for mixed read/write workloads.
- Fine-grained synchronization (not whole-map lock).
- Atomic compound APIs: `compute`, `computeIfAbsent`, `merge`, `putIfAbsent`, `replace`.

**Cons / caveats**
- Iterators are **weakly consistent** (not fail-fast snapshot).
- No `null` keys/values.
- Compound operations still require atomic methods; `get` then `put` can race.

### `ConcurrentSkipListMap<K, V>` and `ConcurrentSkipListSet<E>`

**When to use**
- Need sorted keys/elements with concurrent access.
- Range queries (`subMap`, `headMap`, `tailMap`) under concurrency.

**Pros**
- Concurrent + naturally ordered/sorted semantics.
- Non-blocking traversal characteristics are good for read-heavy sorted workloads.

**Cons / caveats**
- Usually slower and more memory-heavy than `ConcurrentHashMap` for plain key lookup.
- Comparator/key ordering cost matters.

### `CopyOnWriteArraySet<E>`

**When to use**
- Very frequent iteration, very rare mutation.
- Listener/subscriber registries.

**Pros**
- Iteration is safe without explicit locking.
- Snapshot-style iteration semantics.

**Cons / caveats**
- Each mutation copies backing array (expensive write path).
- Not suitable for write-heavy workloads.

---

## 2.2 Queues / deques (producer-consumer patterns)

### Non-blocking queues

#### `ConcurrentLinkedQueue<E>`

**When to use**
- High-concurrency FIFO queue without blocking semantics.

**Pros**
- Lock-free algorithm, good throughput under contention.

**Cons / caveats**
- No capacity bound.
- `size()` is O(n) and approximate under concurrency.
- No blocking backpressure; may grow unbounded.

#### `ConcurrentLinkedDeque<E>`

**When to use**
- Need lock-free double-ended operations.

**Pros**
- Lock-free add/remove from both ends.

**Cons / caveats**
- Same caveats as `ConcurrentLinkedQueue` for size/visibility semantics.

### Blocking queues (`BlockingQueue`)

#### `ArrayBlockingQueue<E>`

**When to use**
- Fixed-capacity queue to enforce backpressure.
- Predictable memory footprint.

**Pros**
- Bounded by design.
- Good for stable throughput pipelines.

**Cons / caveats**
- Single lock can become bottleneck at very high contention.
- Capacity fixed at construction.

#### `LinkedBlockingQueue<E>`

**When to use**
- Need optionally bounded queue with typically higher concurrency than `ArrayBlockingQueue`.

**Pros**
- Separate put/take locks can improve producer/consumer parallelism.
- Can be bounded or effectively unbounded.

**Cons / caveats**
- Higher allocation overhead due to linked nodes.
- Unbounded default constructor risks OOM under overload.

#### `LinkedBlockingDeque<E>`

**When to use**
- Blocking, optionally bounded deque for work-stealing-like patterns.

**Pros**
- Double-ended blocking semantics.

**Cons / caveats**
- More complex semantics; ensure consistent producer/consumer policy.

#### `PriorityBlockingQueue<E>`

**When to use**
- Need blocking queue with priority ordering.

**Pros**
- Consumers always get highest-priority available element.

**Cons / caveats**
- Unbounded queue (no natural backpressure).
- Equal-priority ordering is not FIFO unless encoded manually.

#### `DelayQueue<E extends Delayed>`

**When to use**
- Process items only after delay expiration.

**Pros**
- Natural fit for retries, time-based scheduling buffers.

**Cons / caveats**
- Unbounded queue.
- Requires careful `Delayed` implementation.

#### `SynchronousQueue<E>`

**When to use**
- Direct handoff model; each put waits for matching take.

**Pros**
- Zero internal capacity, strong backpressure semantics.
- Useful for thread pool handoff (`Executors.newCachedThreadPool` internals).

**Cons / caveats**
- Throughput depends on producer/consumer rendezvous timing.
- Can surprise teams expecting buffering.

#### `LinkedTransferQueue<E>` (`TransferQueue`)

**When to use**
- Need hybrid behavior: enqueue normally or block until consumer receives.

**Pros**
- Flexible transfer semantics for low-latency handoff.

**Cons / caveats**
- API is richer/more subtle; misuse can cause stalls.

---

## 2.3 List-like and legacy synchronized wrappers

### `CopyOnWriteArrayList<E>`

**When to use**
- Read-mostly lists with infrequent writes.
- Immutable-snapshot iteration needs.

**Pros**
- Iteration without locks; no `ConcurrentModificationException` during traversal.

**Cons / caveats**
- Write operations are O(n) copy and allocation heavy.
- Not suitable for large or frequently changing lists.

### `Collections.synchronizedList/Map/Set(...)`

**When to use**
- Low-concurrency code or easy retrofit for legacy structures.

**Pros**
- Simple migration path.

**Cons / caveats**
- Coarse-grained locking hurts scalability.
- Must manually synchronize during iteration to remain thread-safe.

---

## 2.4 Atomic and contention-friendly primitives (not collections but commonly paired)

### `AtomicInteger`, `AtomicLong`, `AtomicReference`

**When to use**
- Single-variable lock-free state updates via CAS.

**Pros**
- Low overhead, deterministic atomic updates.

**Cons / caveats**
- High contention can cause retries/spin.
- Multi-variable invariants still need locks/transactions.

### `LongAdder`, `LongAccumulator`

**When to use**
- Hot counters/aggregations with many concurrent updates.

**Pros**
- Better throughput than `AtomicLong` under heavy contention.

**Cons / caveats**
- `sum()` is not an atomic snapshot across concurrent updates.

---

## 3) Interview-level evaluation checklist

When evaluating a candidate's collection choice, check whether they reasoned about:

1. **Access pattern**: read-heavy vs write-heavy vs balanced.
2. **Ordering constraints**: none, FIFO, priority, sorted, delayed.
3. **Capacity/backpressure**: bounded vs unbounded memory risk.
4. **Latency vs throughput**: lock-free/low-latency vs simpler blocking semantics.
5. **Iteration semantics**: weakly consistent, snapshot, or externally synchronized.
6. **Atomicity needs**: single operation vs compound update invariants.
7. **Failure mode under load**: queue growth, OOM risk, contention collapse.
8. **Operational simplicity**: maintainability and debugging cost.

---

## 4) Common mistakes and better alternatives

- Using `HashMap` with ad-hoc synchronization in multithreaded code.
  - Prefer `ConcurrentHashMap` with atomic `compute/merge` methods.
- Using unbounded `LinkedBlockingQueue` by default in critical services.
  - Prefer bounded queues + rejection/backpressure policy.
- Using `CopyOnWriteArrayList` in write-heavy paths.
  - Prefer `ConcurrentLinkedQueue` or explicit lock strategy depending on semantics.
- Assuming `size()` on concurrent queues/maps is exact under high churn.
  - Prefer explicit counters/metrics and eventual consistency expectations.
- Choosing data structure before defining SLO/backpressure strategy.
  - Start from load profile, failure policy, and ordering requirements.

---

## 5) Practical decision matrix (short form)

| Requirement | Recommended choice | Why | Main trade-off |
|---|---|---|---|
| Fast concurrent key/value access | `ConcurrentHashMap` | High throughput and atomic map ops | Weakly consistent iteration |
| Sorted concurrent map/set | `ConcurrentSkipListMap/Set` | Concurrent + sorted/range queries | More overhead than hash map |
| Read-mostly list | `CopyOnWriteArrayList` | Lock-free reads/snapshot iteration | Very expensive writes |
| Bounded producer-consumer queue | `ArrayBlockingQueue` | Predictable memory and backpressure | Fixed capacity / lock contention |
| High-throughput blocking queue | `LinkedBlockingQueue` (bounded) | Better producer-consumer parallelism | Node allocation overhead |
| Priority task execution | `PriorityBlockingQueue` | Priority-based retrieval | Unbounded by default |
| Zero-buffer handoff | `SynchronousQueue` | Strong backpressure / direct transfer | Requires matched producer-consumer timing |
| High-contention counter | `LongAdder` | Better update scalability | Non-atomic aggregate snapshot |

---

## 6) Recommendation for documentation and code reviews

- Require explicit justification for:
  - bounded vs unbounded queue,
  - ordering guarantees,
  - expected contention profile,
  - atomicity guarantees.
- Add load-test evidence for high-risk paths (queues/caches/counters).
- Standardize on `ConcurrentHashMap` as default concurrent map unless sorted/range semantics are required.
- Prefer bounded queues in service code unless there is a proven reason not to.

---

## 7) Interview questions with real-world use cases

Use these questions to test whether a candidate can connect Java concurrent collections to production trade-offs, not just API names.

### 1. High-throughput request de-duplication

**Question:** A payment service receives duplicate retry requests from multiple instances. You need to process each idempotency key once while many threads race to register the same key. Which collection and operation would you use?

**Expected answer:** Use `ConcurrentHashMap` or `ConcurrentHashMap.newKeySet()` with atomic operations such as `putIfAbsent`, `computeIfAbsent`, or `add`. Avoid `containsKey` followed by `put` because the check-then-act sequence can race.

**Real-world use case:** API idempotency keys, duplicate message suppression, webhook replay protection, and distributed job submission guards.

**Follow-ups:**
- How would you expire old keys?
- What happens if processing fails after the key is inserted?
- Would you store a status object instead of just a key?

### 2. Concurrent cache population

**Question:** Several request threads need a user profile from a remote service. If the profile is missing locally, only one thread should load it and the rest should reuse the result. How can `ConcurrentHashMap` help?

**Expected answer:** Use `computeIfAbsent` to atomically associate a key with a computed value. For expensive or blocking loads, discuss whether the mapping function should store a `CompletableFuture`, whether failures should remove the entry, and whether a dedicated cache library is more appropriate.

**Real-world use case:** User profile caches, authorization policy caches, feature flag lookups, tenant metadata, and service discovery registries.

**Follow-ups:**
- Why can a slow mapping function be risky?
- How do you prevent cache stampedes across JVMs?
- How would you add TTL, size limits, or refresh behavior?

### 3. Counting events under heavy contention

**Question:** A metrics collector increments counters for thousands of requests per second from many threads. Should you use `AtomicLong`, `LongAdder`, or a synchronized map of counters?

**Expected answer:** Use `LongAdder` for hot counters because it scales better under contention. Store counters in a `ConcurrentHashMap<String, LongAdder>` and initialize with `computeIfAbsent`. Use `AtomicLong` when exact read-modify-write semantics are required for a single value.

**Real-world use case:** Per-endpoint request counts, error counters, rate-limit observations, business event telemetry, and in-memory aggregation before export.

**Follow-ups:**
- Why is `LongAdder.sum()` not a globally atomic snapshot?
- How would you reset counters safely?
- When would approximate metrics be unacceptable?

### 4. Bounded ingestion pipeline

**Question:** A log ingestion service has producer threads parsing network input and consumer threads writing batches to storage. During a storage slowdown, memory must not grow without bound. Which queue should you choose?

**Expected answer:** Use a bounded `BlockingQueue`, commonly `ArrayBlockingQueue` for predictable memory or a bounded `LinkedBlockingQueue` for separate producer/consumer locks. Pair the queue with a clear overload policy: block, timeout, reject, shed low-priority logs, or apply upstream backpressure.

**Real-world use case:** Log pipelines, image processing workers, fraud scoring queues, email sending buffers, and asynchronous audit event writers.

**Follow-ups:**
- What is dangerous about the default `LinkedBlockingQueue` constructor?
- How would queue capacity be selected?
- What metrics would you expose?

### 5. Direct handoff without buffering

**Question:** A service should create work only when a worker is ready to take it; buffering is not allowed because tasks become stale quickly. Which concurrent collection fits this pattern?

**Expected answer:** Use `SynchronousQueue`, where each `put` must rendezvous with a matching `take`. It provides strong backpressure and zero internal capacity, but throughput depends on the timing of producers and consumers.

**Real-world use case:** Thread pool handoff, live market data processing, real-time alert dispatch, and systems where stale work should not be queued.

**Follow-ups:**
- How does fairness mode affect throughput and ordering?
- What happens when no consumer is available?
- How would you add timeouts or fallback behavior?

### 6. Priority-based task scheduling

**Question:** An operations platform must process incident notifications before low-priority report-generation jobs. Which queue can model this, and what issue must you address?

**Expected answer:** Use `PriorityBlockingQueue` with a comparator. Because it is unbounded, it does not provide backpressure by itself. Also ensure equal-priority ordering if FIFO within priority is required, commonly by adding a sequence number.

**Real-world use case:** Incident handling, customer support escalation, workflow engines, print queues, and prioritized background job executors.

**Follow-ups:**
- How do you prevent starvation of low-priority work?
- How would you bound the queue?
- Why might a normal `ThreadPoolExecutor` configuration behave unexpectedly with this queue?

### 7. Retry after delay

**Question:** Failed messages should be retried only after their next retry time. Consumers should not busy-wait or repeatedly poll unavailable work. What structure would you use?

**Expected answer:** Use `DelayQueue` with elements implementing `Delayed`. Consumers block until the head element's delay expires. Discuss unbounded growth, poison messages, max retry count, jitter, and persistence if retries must survive process restarts.

**Real-world use case:** Retry schedulers, token refresh queues, delayed notification delivery, order timeout processing, and temporary ban expiration.

**Follow-ups:**
- How do you implement `compareTo` and `getDelay` correctly?
- What happens if the JVM restarts?
- When would an external broker or scheduler be better?

### 8. Listener registry with frequent reads and rare writes

**Question:** An application maintains a list of listeners. Events are published thousands of times per second, but listeners are added or removed only during configuration changes. Which collection is appropriate?

**Expected answer:** Use `CopyOnWriteArrayList` or `CopyOnWriteArraySet`. Iteration is snapshot-based and safe without explicit locks, making event dispatch simple and predictable. The trade-off is expensive mutation because each write copies the backing array.

**Real-world use case:** Event listeners, plugin hooks, configuration observers, health-check subscribers, and callback registries.

**Follow-ups:**
- Why is this a poor choice for chat room membership?
- What does a listener added during dispatch observe?
- How do you handle slow or failing listeners?

### 9. Sorted concurrent access and range queries

**Question:** A pricing engine stores active orders by price and needs concurrent inserts, removals, and range queries such as all orders between two prices. Why is `ConcurrentHashMap` not enough?

**Expected answer:** Use `ConcurrentSkipListMap` or `ConcurrentSkipListSet` because they maintain sorted order and support range views like `subMap`, `headMap`, and `tailMap`. The trade-off is higher overhead than hash-based lookup.

**Real-world use case:** Order books, leaderboard windows, time-indexed session stores, scheduled task lookup by timestamp, and range-based routing tables.

**Follow-ups:**
- What comparator pitfalls can break correctness?
- Are range view iterations strongly consistent?
- How would you handle multiple orders with the same price?

### 10. Work queue where `size()` drives decisions

**Question:** A developer uses `ConcurrentLinkedQueue.size()` on every request to decide whether to reject new work. What is the problem, and what would you do instead?

**Expected answer:** `ConcurrentLinkedQueue.size()` is O(n) and can be inaccurate while concurrent updates occur. Prefer a bounded `BlockingQueue` for backpressure, maintain a separate counter if approximate observability is enough, or use admission control before enqueueing.

**Real-world use case:** API overload protection, async task submission, telemetry buffering, and in-memory work queues.

**Follow-ups:**
- When is an approximate size acceptable?
- How can a separate counter become inconsistent?
- Why might a bounded queue be simpler and safer?

### 11. Compound updates across multiple keys

**Question:** A wallet service stores balances in a `ConcurrentHashMap<AccountId, Balance>`. A transfer debits one account and credits another. Is `ConcurrentHashMap` enough to make the transfer atomic?

**Expected answer:** No. `ConcurrentHashMap` makes individual map operations thread-safe, but multi-key invariants still require a broader synchronization strategy, such as ordered per-account locks, database transactions, or a single-threaded actor/partition model.

**Real-world use case:** Wallet transfers, inventory reservations, seat booking, resource allocation, and quota movement between tenants.

**Follow-ups:**
- How do you avoid deadlocks with per-account locks?
- Why might database transactions be preferable?
- What invariants must tests verify under concurrency?

### 12. Choosing between synchronized wrappers and concurrent collections

**Question:** A legacy service wraps a `HashMap` with `Collections.synchronizedMap`. Under load, request latency rises sharply. Why might replacing it with `ConcurrentHashMap` help, and what migration risks remain?

**Expected answer:** `Collections.synchronizedMap` serializes access through a coarse-grained lock, while `ConcurrentHashMap` supports much better concurrent access. Migration still requires reviewing iteration behavior, null key/value assumptions, compound operations, and external synchronization that may have been protecting larger invariants.

**Real-world use case:** Legacy session maps, in-memory registries, plugin state, feature flag stores, and application-level caches.

**Follow-ups:**
- Why must synchronized wrapper iteration use external synchronization?
- What changes because `ConcurrentHashMap` rejects nulls?
- Which existing tests might become invalid because iteration is weakly consistent?
