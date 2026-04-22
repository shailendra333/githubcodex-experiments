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
