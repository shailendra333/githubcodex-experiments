# AWS SQS — Architecture & Design Interview Questions

## 1) Queue-Centric Microservices Boundaries
A monolith is being decomposed into microservices with asynchronous communication.
- How would you define which workflows should be queue-based vs synchronous API-based?
- Where would you place boundaries for command processing, orchestration, and state ownership?
- How do you prevent distributed monolith anti-patterns when introducing SQS?

## 2) Throughput, Ordering, and Consistency
A payment platform needs strict ordering for some flows and high parallelism for others.
- How would you decide between Standard and FIFO queues per workflow?
- What architectural trade-offs exist between ordering guarantees and throughput?
- How would you design idempotency, deduplication, and exactly-once-like outcomes at system level?

## 3) Failure Handling and Operational Resilience
Traffic spikes and downstream outages are common.
- How would you design redrive policies, DLQ triage, and replay processes as first-class architectural capabilities?
- What visibility timeout and backoff standards would you enforce across teams?
- How would your design minimize cascading failures and protect critical workflows?

## 4) Multi-Tenant and Security Architecture
A shared platform processes jobs from many tenant applications.
- How would you design queue isolation, encryption, and access control by tenant sensitivity?
- When would you use shared queues with attributes vs dedicated queues per tenant?
- What operating model ensures both governance and cost efficiency?

## 5) End-to-End Platform Roadmap
Today’s workload is batch-heavy, but product strategy moves toward real-time and event choreography.
- How would your SQS architecture evolve to support this roadmap?
- What standards would you define for contracts, observability, and ownership to enable safe evolution?
- How would you sequence migration milestones to minimize delivery risk?
