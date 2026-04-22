# Redis Data Structures: Evaluation Criteria, Use Cases, Pros, and Cons

This guide helps you choose the right Redis data structure by defining practical evaluation criteria and mapping each structure to common use cases with trade-offs.

## 1) Evaluation Criteria

When selecting a Redis data structure, evaluate along these dimensions:

- **Access pattern**
  - Point lookup, range scan, top-N, FIFO/LIFO, pub/sub fanout, geo-radius, time-series windows, etc.
- **Mutation pattern**
  - High write rate, append-only, random updates, increment/decrement-heavy workloads.
- **Query capability**
  - Exact key retrieval vs. ordered/ranked/range queries vs. set operations.
- **Latency requirements**
  - P50/P95/P99 targets; sensitivity to occasional spikes.
- **Memory efficiency**
  - Raw value overhead, metadata overhead, cardinality growth behavior.
- **Cardinality and growth**
  - Expected number of elements per key and total keys; hot-key risk.
- **Durability and recovery needs**
  - Snapshot/AOF implications for data volume and write frequency.
- **Consistency and atomicity**
  - Need for atomic increments/updates/transactions.
- **TTL and lifecycle management**
  - Whole-key expiration vs. per-item expiration patterns.
- **Operational complexity**
  - Simplicity of commands, observability, sharding strategy, and debugging effort.

---

## 2) Data Structure Decision Matrix (Quick View)

| Data Structure | Best For | Avoid When | Core Strength |
|---|---|---|---|
| String | Caching, counters, simple state | You need partial-field updates or complex queries | Fastest and simplest primitive |
| Hash | Object-like records with fields | Need deep nested structures or secondary indexing | Field-level updates without re-writing whole object |
| List | Queues, logs, recent event buffers | Need random lookup or ranking | Efficient push/pop at ends |
| Set | Unique membership, tags, dedupe | Need ordering or score-based ranking | Fast membership and set algebra |
| Sorted Set (ZSET) | Ranking, leaderboards, time-ordered indexes | Very large write-heavy scores with loose ordering needs | Score-based ordering + range queries |
| Stream | Event logs, consumer groups, replay | Need ultra-simple queue semantics only | Durable append log with consumer tracking |
| Bitmap | Boolean flags by numeric ID, DAU tracking | IDs are sparse or non-numeric | Bit-level memory efficiency |
| HyperLogLog | Approximate cardinality | Exact counts required | Very low-memory unique counting |
| Geospatial (GEO) | Nearby search, location-based lookup | Need advanced GIS polygons/projections | Easy radius/proximity search |

---

## 3) Structure-by-Structure Guidance

## 3.1 String

**Typical use cases**
- Page/API response cache
- Session/token storage
- Counters (views, likes, rate-limits)
- Feature flags or simple config values

**Pros**
- Minimal cognitive overhead and excellent performance
- Supports atomic operations (`INCR`, `DECR`, `SETNX` patterns)
- Works naturally with TTL for cache semantics

**Cons**
- Entire value is typically rewritten for non-atomic partial updates
- Poor fit for multi-field objects unless serialized externally
- No built-in secondary query capabilities

---

## 3.2 Hash

**Typical use cases**
- User/profile objects (`user:123` with fields)
- Product metadata
- Mutable object state where only some fields change frequently

**Pros**
- Field-level read/write (`HGET`, `HSET`) avoids whole-object rewrites
- Cleaner object modeling vs. packing JSON into strings
- Efficient for medium-sized flat records

**Cons**
- Not ideal for deeply nested documents
- No native secondary indexes (you must model indexes separately)
- Can become unwieldy with extremely large field counts per key

---

## 3.3 List

**Typical use cases**
- Simple job queues (producer-consumer)
- Activity feeds (most recent N events)
- Rolling buffers/log tails

**Pros**
- Fast push/pop operations from head/tail
- Straightforward queue semantics
- Supports blocking pop for worker patterns

**Cons**
- Random element access is not efficient for large lists
- No built-in scoring/ranking
- Queue reliability patterns can become complex under failure scenarios

---

## 3.4 Set

**Typical use cases**
- Deduplication (seen IDs)
- Tagging and cohort membership
- Relationship modeling (followers/following as membership sets)

**Pros**
- Uniqueness guaranteed
- Fast membership checks (`SISMEMBER`)
- Powerful set operations (`SINTER`, `SUNION`, `SDIFF`)

**Cons**
- Unordered by design
- No weighted ranking
- Large set operations can be costly at scale

---

## 3.5 Sorted Set (ZSET)

**Typical use cases**
- Leaderboards and scoring systems
- Priority queues
- Time-series index by timestamp as score
- Top-N and percentile style queries

**Pros**
- Ordered retrieval by score (`ZRANGE`, `ZREVRANGE`, score windows)
- Great for rank + lookup in one structure
- Supports efficient top-K patterns

**Cons**
- Higher memory and computational overhead than plain set/list
- Tie handling and score precision require careful modeling
- Frequent re-scoring can increase write amplification

---

## 3.6 Stream

**Typical use cases**
- Event sourcing-lite pipelines
- Audit/event logs with replay
- Multi-consumer processing with consumer groups

**Pros**
- Append-only log semantics with IDs
- Consumer groups track pending/unacked messages
- Better reliability patterns than ad hoc list queues

**Cons**
- More operational complexity than lists
- Requires trimming and retention strategy
- Backlog/pending management needs monitoring discipline

---

## 3.7 Bitmap

**Typical use cases**
- Daily active users by numeric user ID
- Feature rollout flags per user index
- Compact on/off state arrays

**Pros**
- Extremely memory efficient for dense numeric ID spaces
- Fast bitwise operations and aggregates
- Great for cohort comparisons over time

**Cons**
- Requires stable integer offset mapping
- Sparse IDs can waste memory
- Harder to inspect/debug manually vs. sets/hashes

---

## 3.8 HyperLogLog

**Typical use cases**
- Unique visitor estimation
- Approximate cardinality of large streams or datasets

**Pros**
- Very low fixed memory footprint
- Good for high-scale approximate unique counting

**Cons**
- Approximate, not exact
- Cannot enumerate members
- Unsuitable for billing/compliance use cases requiring precision

---

## 3.9 Geospatial (GEO)

**Typical use cases**
- Nearby store/driver search
- “Users near me” radius queries
- Region-based discovery features

**Pros**
- Simple commands for geospatial indexing and radius search
- Works well for proximity use cases without external GIS stack

**Cons**
- Limited compared to full GIS databases (polygons, complex geofencing)
- Coordinate quality and update frequency directly affect correctness
- Often needs companion metadata keys for rich filtering

---

## 4) Practical Selection Heuristics

- Use **String** first for simple key-value caching and counters.
- Use **Hash** for mutable object fields.
- Use **List** for simple FIFO/LIFO pipelines.
- Use **Set** for uniqueness and membership logic.
- Use **ZSET** when order/rank/priority matters.
- Use **Stream** for durable event processing with consumer groups.
- Use **Bitmap** for dense boolean states by numeric IDs.
- Use **HyperLogLog** only when approximation is acceptable.
- Use **GEO** for basic radius/proximity search.

If multiple needs overlap, combine structures (e.g., hash for object + zset for ranking + set for membership).

## 5) Common Anti-Patterns

- Storing massive JSON blobs in strings when only a few fields change frequently.
- Using lists for workloads that require ranking or random access.
- Using sets when deterministic ordering is required.
- Using HyperLogLog where exact counts are contractually required.
- Ignoring TTL/retention, causing unbounded memory growth.

---

## 6) Lead-Level Redis Solution Design (Comprehensive Problem)

### Problem Statement

Design a **real-time hyperlocal commerce and delivery platform** (think groceries + quick commerce) that must support:

- User sessions, carts, and product metadata
- Inventory reservation with contention control
- Order placement and lifecycle events
- Driver discovery by proximity
- Dynamic ETA and surge ranking
- Notification fanout
- Fraud/rate-limit protection
- Real-time analytics (approx + exact where needed)

The system has to operate at very high QPS with sub-20ms read latency for hot paths and predictable behavior during traffic spikes.

### Functional and Non-Functional Requirements

**Functional**
- Browse products and prices by store
- Maintain cart state with expiration
- Reserve inventory atomically on checkout
- Dispatch nearest eligible driver
- Track order status updates in real time
- Provide user and merchant dashboards (orders, rankings, active users)

**Non-functional**
- P99 reads < 20ms for hot keys
- Graceful degradation under 10x burst traffic
- Durable order/event history for replay and reconciliation
- Multi-AZ/high availability and fast failover
- Strong observability and operational guardrails

---

### Data Modeling: Redis Features Mapped to Use Cases

#### 1) Strings
- `session:{token}` → serialized auth/session payload with TTL
- `config:surge_multiplier:{zone}` → dynamic config values
- `rate_limit:{user_id}:{minute}` → counter using `INCR` + `EXPIRE`

Why: fastest primitive for ephemeral state, counters, and configuration snapshots.

#### 2) Hashes
- `user:{id}` → profile fields
- `store:{id}:product:{sku}` → price, stock, attributes
- `order:{id}` → mutable order state machine fields

Why: partial field updates avoid rewriting full blobs; good for operational entities.

#### 3) Lists
- `deadletter:dispatch` → failed dispatch attempts for operator triage
- `recent_events:{order_id}` → last N events per order (`LPUSH` + `LTRIM`)

Why: simple append/pop buffers where strict replay semantics are not required.

#### 4) Sets
- `store:{id}:active_couriers` → currently available drivers
- `coupon:{id}:redeemed_users` → dedupe coupon redemption
- `fraud:blocked_devices` → O(1) membership checks

Why: uniqueness + fast membership and set algebra for eligibility filtering.

#### 5) Sorted Sets (ZSET)
- `leaderboard:stores:sales:{day}` score = revenue
- `dispatch_candidates:{zone}` score = composite priority (distance, load, rating)
- `orders_by_time:{store_id}` score = epoch millis

Why: ranking, top-N, and time window queries with one structure.

#### 6) Streams
- `stream:orders` for order lifecycle events
- `stream:inventory` for stock updates
- Consumer groups:
  - `cg:fulfillment`
  - `cg:notifications`
  - `cg:analytics`

Why: durable event log with replay, pending tracking, and independent consumer scaling.

#### 7) Bitmaps
- `bitmap:dau:{yyyyMMdd}` bit offset by numeric user ID
- `bitmap:feature:{flag}:{yyyyMMdd}` rollout exposure tracking

Why: highly memory-efficient daily active/feature participation analytics.

#### 8) HyperLogLog
- `hll:unique_visitors:{store_id}:{yyyyMMdd}`
- `hll:search_unique_queries:{yyyyMMdd}`

Why: low-cost approximate cardinality at high scale.

#### 9) GEO
- `geo:couriers:{city}` with courier coordinates
- Query via radius around merchant/customer location

Why: native nearby lookup for dispatch without external GIS for simple proximity.

#### 10) Pub/Sub
- Channels: `chan:order_updates:{order_id}`, `chan:ops_alerts`

Why: low-latency fanout for transient real-time UI updates (not source of truth).

---

### End-to-End Flow (Checkout to Delivery)

1. **Cart read/write**
   - Cart entries in `hash cart:{user_id}` with TTL refresh.
2. **Checkout validation**
   - Inventory + coupon + risk checks in a Lua script for atomic multi-key rules.
3. **Order creation**
   - Persist `order:{id}` hash + append `XADD stream:orders`.
4. **Inventory reservation**
   - Atomic decrement using Lua; failure triggers compensating event.
5. **Dispatch**
   - GEO radius lookup of drivers, intersect with availability set, rank in ZSET.
6. **Notification**
   - Publish transient update via Pub/Sub; durable status also sent on Stream.
7. **Analytics**
   - Increment strings, set bits in bitmap, update HLL and daily leaderboards.

---

### Atomicity, Consistency, and Concurrency Strategy

- Use **Lua scripts** for multi-step invariants:
  - stock decrement only if `stock >= requested`
  - one-time coupon redemption check + write
  - idempotent order transition enforcement
- Use **WATCH/MULTI/EXEC** only when optimistic concurrency is sufficient and key contention is moderate.
- Design **idempotency keys**:
  - `idempotency:checkout:{request_id}` with TTL, set via `SET NX EX`.
- Guarantee state transition correctness:
  - finite-state-machine rules inside script (`CREATED -> ASSIGNED -> PICKED -> DELIVERED`).

---

### Partitioning, Scalability, and High Availability

- Deploy **Redis Cluster** with hash-tagging for co-location where needed:
  - Example: `{order:123}:state`, `{order:123}:events`
- Split workloads by role:
  - hot cache cluster
  - stream/event cluster
  - analytics cluster
- Use replicas for read scaling where read-after-write strictness is not required.
- Configure Sentinel/managed failover (or cloud managed Redis) for automated recovery.
- Implement client-side circuit breaking and fallback for partial outages.

---

### Persistence and Recovery

- Use **AOF everysec** for event-centric keys (orders/streams).
- Use periodic **RDB snapshots** for faster restart + backup checkpoints.
- Define clear data classes:
  - critical durable: orders, payments linkage, inventory reservations
  - reconstructable: rankings, caches, ephemeral sessions
- Build stream replay jobs to rebuild derived views (leaderboards, aggregates).

---

### Observability and SRE Guardrails

- Track:
  - latency (P50/P95/P99) by command group
  - keyspace hit ratio, eviction counts, fragmentation ratio
  - stream lag and pending message depth per consumer group
  - hot-key detection and top memory keys
- Enforce:
  - TTL standards for ephemeral data
  - maxmemory policies (`allkeys-lru`/`volatile-ttl` by workload)
  - key naming/versioning conventions
- Runbooks:
  - backlog drain procedure
  - dead-letter reprocessing
  - traffic shedding mode (disable non-critical writes/analytics)

---

### Security and Governance

- Enable ACLs by service role (read-only analytics, write-capable fulfillment).
- Enforce TLS in transit and encryption at rest (managed offering preferred).
- Avoid sensitive PII in Redis; keep tokenized references where possible.
- Apply strict TTL for session/security artifacts.

---

### Trade-Offs and Design Rationale

- Streams + Pub/Sub together provide both durable processing and live fanout.
- ZSET-based ranking simplifies dispatch but requires careful score tuning.
- Bitmap/HLL drastically reduce analytics cost, with known precision trade-offs.
- Lua improves correctness under concurrency but increases script governance complexity.
- Multi-cluster separation reduces noisy-neighbor effects at operational cost.

---

### Interview Evaluation Rubric (Lead-Level Signals)

A strong lead-level solution should explicitly cover:
- Data structure fit by access pattern (not by habit)
- Atomic invariants and idempotency strategy
- Failure modes and compensating actions
- Capacity planning (memory + cardinality growth)
- Operational model (monitoring, runbooks, SLOs)
- Security, compliance boundaries, and data lifecycle
- Clear trade-offs with rationale, not just feature listing

## 7) Example Architecture Patterns

- **Leaderboard system**: `ZSET` for scores, `HASH` for player profile.
- **Rate limiter**: `STRING` counters with TTL, or `ZSET` sliding window for precision.
- **Activity feed**: `LIST` or `STREAM` depending on replay/consumer needs.
- **Feature targeting**: `SET` for segment membership + `BITMAP` for fast daily eligibility flags.

---

## 8) Lead-Level Interview Questions and Model Answers

These questions are based on the solution above and are designed to evaluate architecture depth, trade-off clarity, and operational maturity.

### Q1) Why did you choose both Streams and Pub/Sub instead of just one?
**Model answer (what strong looks like):**
- They solve different reliability needs.
- **Streams** are the durable system-of-record event log with replay and consumer-group tracking.
- **Pub/Sub** is for low-latency transient fanout to live clients (e.g., active order screen updates).
- If Pub/Sub messages are missed, the client can recover from Stream-backed current state.
- This separation keeps UX snappy while preserving auditability and recovery guarantees.

### Q2) How do you make inventory reservation safe under high concurrency?
**Model answer:**
- Use a Lua script to atomically:
  1. read stock,
  2. validate `stock >= requested`,
  3. decrement stock,
  4. write reservation marker/idempotency token.
- This prevents oversell from race conditions that occur with multi-round-trip logic.
- Add request idempotency key (`SET NX EX`) so retries do not double-reserve.
- Emit reservation events to Stream for reconciliation and observability.

### Q3) When would you prefer WATCH/MULTI/EXEC over Lua?
**Model answer:**
- Prefer Lua when correctness depends on strict single-step invariants under contention.
- Prefer `WATCH/MULTI/EXEC` when:
  - contention is moderate,
  - logic is simple,
  - optimistic retry cost is acceptable,
  - maintainability is better without scripts.
- In practice, use Lua for critical paths (inventory/order transitions), optimistic transactions for non-critical metadata updates.

### Q4) How do you model driver dispatch ranking in Redis?
**Model answer:**
- Candidate pool from GEO radius query (`geo:couriers:{city}`).
- Eligibility filter via sets (active, not blocked, right vehicle type).
- Final rank in ZSET with a composite score (distance, acceptance rate, current load, rating).
- Keep scoring function versioned (e.g., `dispatch:v2`) to allow controlled rollouts and A/B tests.
- Recompute/expire candidate sets quickly to avoid stale dispatch decisions.

### Q5) What are your hottest keys and how do you protect the system from hot-key collapse?
**Model answer:**
- Likely hot keys: popular store inventory, surge config, zone-level dispatch sets.
- Mitigations:
  - key sharding for aggregate counters,
  - local in-process cache for ultra-hot read-mostly config,
  - request coalescing,
  - shorter TTL with jitter to avoid synchronized expiry,
  - targeted replica reads (where consistency allows).
- Instrument top-key usage and set alerting on per-key QPS thresholds.

### Q6) How do you decide what must be durable vs reconstructable?
**Model answer:**
- **Durable:** order state transitions, reservation/payment linkage, irreversible user actions.
- **Reconstructable:** leaderboards, cache projections, temporary ranking candidates.
- Durable data gets stronger persistence policy (AOF + backups + replay strategy).
- Reconstructable data favors performance and bounded TTL; can be rebuilt from durable streams.

### Q7) How would you design idempotency end-to-end for checkout?
**Model answer:**
- Client sends idempotency key per checkout intent.
- API gateway/service writes `idempotency:checkout:{key}` (`SET NX EX`).
- All side effects bind to that key (order id, reservation result, payment attempt reference).
- Retries return the same canonical outcome instead of re-executing business logic.
- Timeout policy is explicit (e.g., 24h) and monitored for key growth.

### Q8) How do you use Bitmap vs HyperLogLog in analytics, and what mistakes do candidates make?
**Model answer:**
- Bitmap: exact-ish presence by integer ID (e.g., daily active users by user-id bit offset).
- HyperLogLog: approximate unique count at tiny memory footprint when exact identity is unnecessary.
- Common mistakes:
  - using HLL for billing-grade exact counts,
  - using bitmap on sparse/non-numeric IDs causing memory waste,
  - not documenting acceptable error bounds.

### Q9) What SLOs and alerts would you define for this Redis-heavy system?
**Model answer:**
- SLOs:
  - P99 latency by critical command groups,
  - stream lag bounds per consumer group,
  - order state propagation delay.
- Alerts:
  - replication lag spikes,
  - eviction rate > baseline,
  - consumer pending backlog growth,
  - memory fragmentation and hot-key anomalies.
- Include error-budget policy to trigger load shedding for non-critical writes.

### Q10) How do you evolve schema/key design safely in production?
**Model answer:**
- Use versioned keys (`order:v2:{id}`), dual-write during migration, read-fallback to old keys.
- Backfill asynchronously, validate with sampled checksums/consistency reports.
- Cut over with feature flag and rollback plan.
- Declare TTL and ownership for every new keyspace before launch.

### Q11) If a consumer group is down for 30 minutes, what is your recovery plan?
**Model answer:**
- Keep producers writing to Streams (durable buffer absorbs outage).
- On recovery:
  - inspect pending/lag,
  - scale consumer instances horizontally,
  - process oldest-first for correctness-sensitive topics,
  - dead-letter poison messages after bounded retries.
- Track catch-up ETA and degrade non-critical downstream jobs until lag is healthy.

### Q12) What is the biggest trade-off in this design?
**Model answer:**
- Complexity vs capability.
- Combining many Redis primitives enables low latency and rich behavior, but operational burden rises:
  - more keyspaces,
  - stricter observability needs,
  - stronger governance for scripts and TTL hygiene.
- A lead-level design acknowledges this and defines ownership/runbooks/SLOs up front.

---

Use this document as a baseline and tune based on your workload’s real traffic shape, memory budget, and correctness requirements.

---

## 7) Interview Question and Model Answer

### Interview Question
You are building a **Product Service** for a high-traffic e-commerce platform.

Requirements:
- APIs:
  - Get product details (name, description, category)
  - Get product price (frequently updated)
  - Get product inventory (near real-time)
- Traffic:
  - 95% reads, 5% writes
- Problem:
  - Database latency is high due to repeated reads

How would you design Redis caching and data modeling to reduce DB load while keeping price and inventory reasonably fresh?

### Model Answer
I would design this as a **cache-aside + selective write-through** pattern with different Redis structures and TTLs per data type, because details, price, and inventory have very different freshness requirements.

1. **Use separate keys by concern**
   - `product:{id}:details` (Hash or JSON string): name, description, category
   - `product:{id}:price` (String/Hash field): current price, currency, updated_at
   - `product:{id}:inventory` (String/Hash field): available_qty, updated_at

2. **Cache strategy by API**
   - **Product details**: cache-aside with longer TTL (for example 30–120 minutes), since details change infrequently.
   - **Price**: shorter TTL (for example 30–120 seconds) plus write-through/invalidation on price updates so reads are fresh.
   - **Inventory**: very short TTL (for example 5–15 seconds) or event-driven updates from inventory service to Redis for near real-time reads.

3. **Read flow (95% traffic)**
   - Read from Redis first.
   - On miss, fetch from DB, populate Redis, then return.
   - Add **jitter to TTLs** to prevent cache stampedes.
   - Use request coalescing/mutex for hot keys so only one request repopulates on miss.

4. **Write flow (5% traffic)**
   - DB remains source of truth.
   - On successful write:
     - Update/invalidate corresponding Redis key immediately.
     - For price/inventory, prefer publishing update events so all app nodes refresh quickly.

5. **Consistency and reliability**
   - Accept eventual consistency for details.
   - Keep stricter freshness for price/inventory via short TTL + proactive invalidation.
   - Include `updated_at`/version in cached payload to detect stale data.
   - Optionally use stale-while-revalidate for details to protect p99 latency.

6. **Operational safeguards**
   - Monitor cache hit rate, p95 latency, evictions, and hot keys.
   - Pre-warm top products.
   - Use namespaced versioned keys (e.g., `v1:product:{id}:price`) for safe schema evolution.

This approach removes most repeated reads from the database while preserving correctness where it matters most (price and inventory), which is ideal for a 95/5 read-heavy workload.
