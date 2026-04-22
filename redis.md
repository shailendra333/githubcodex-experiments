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

## 6) Example Architecture Patterns

- **Leaderboard system**: `ZSET` for scores, `HASH` for player profile.
- **Rate limiter**: `STRING` counters with TTL, or `ZSET` sliding window for precision.
- **Activity feed**: `LIST` or `STREAM` depending on replay/consumer needs.
- **Feature targeting**: `SET` for segment membership + `BITMAP` for fast daily eligibility flags.

---

Use this document as a baseline and tune based on your workload’s real traffic shape, memory budget, and correctness requirements.
