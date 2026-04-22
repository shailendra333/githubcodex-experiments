# AWS S3 — Architecture & Design Interview Questions

## 1) Multi-Region Data Strategy
You are designing a platform that stores regulatory documents used by customers in North America, Europe, and APAC.
- How would you decide between single-region buckets, Multi-Region Access Points, and cross-region replication?
- What are the trade-offs in latency, sovereignty, data residency, and operational complexity?
- What would your failure-mode design look like during a region outage?

**Model answer / solution**
- Keep primary buckets per sovereignty zone (e.g., `eu-west-1` for EU-only data, `us-east-1` for US-only data) and avoid cross-border replication where regulation blocks it.
- Use Multi-Region Access Points (MRAP) only for globally shareable datasets where active-active read/write locality is needed.
- Use cross-region replication (CRR) with replication metrics + RTC only for critical durability/availability objectives, and scope replication by prefix/tag for cost control.
- During outage: fail reads to secondary region via MRAP or application routing, and fail writes based on compliance constraints (queue if writes cannot leave region).

**Trade-offs**
- Single-region is simplest and strongest for residency, but highest outage risk and potentially poor global latency.
- MRAP improves global routing and failover experience but increases operational abstraction, potential debugging complexity, and can conflict with strict residency needs.
- CRR improves recovery posture but adds storage + replication charges, eventual consistency windows across regions, and key-management complexity.

## 2) Security Boundary Design
A product team wants to allow direct browser uploads to S3 from untrusted clients.
- How would you define trust boundaries and IAM policies to avoid privilege escalation?
- When would you use pre-signed URLs vs API proxying through backend services?
- How do KMS key strategy and bucket policies align with least privilege and tenant isolation?

**Model answer / solution**
- Treat browser as untrusted: backend issues short-lived pre-signed `PUT` URLs constrained by object key prefix, content length/type, and expiration.
- Enforce tenant isolation with key namespace strategy (`tenant-id/...`) and bucket policy conditions (`s3:prefix`, object tags, TLS required, deny public ACLs).
- Use separate IAM roles for URL-issuing service vs processing workers; block wildcard permissions and deny direct `ListBucket` unless necessary.
- Use SSE-KMS with either per-tenant CMKs (strong isolation) or shared CMK + encryption context (better scale), and include explicit key policy boundaries.

**Trade-offs**
- Pre-signed URLs reduce backend bandwidth and scale better for large uploads; downside is reduced inline inspection/control unless post-upload validation is mandatory.
- API proxying enables deep request inspection and uniform auth logic, but significantly increases backend cost/latency and creates throughput bottlenecks.
- Per-tenant CMKs improve blast-radius isolation and audit clarity, but raise KMS quota/management overhead versus shared-key models.

## 3) Data Lifecycle and Cost Governance
You expect 5 PB growth over 3 years with highly variable access patterns.
- How would you create a storage class and lifecycle strategy (Standard, Intelligent-Tiering, Glacier tiers)?
- What telemetry would you require before making lifecycle transitions?
- How would you enforce cost guardrails and architecture standards across all teams?

**Model answer / solution**
- Start unknown workloads in Intelligent-Tiering; move known hot prefixes to Standard and archive-ready prefixes to Glacier Instant/Flexible/Deep Archive using lifecycle by tag.
- Require data classification tags (`business-unit`, `retention-class`, `rto-rpo`, `pii`) at object or bucket level; lifecycle rules are generated from these tags.
- Collect Storage Lens, CloudWatch request metrics, inventory reports, restore frequency, and access age distribution before adjusting transition thresholds.
- Enforce standards with IaC modules + policy-as-code checks in CI (mandatory encryption, lifecycle, versioning, logging, public access block).

**Trade-offs**
- Intelligent-Tiering minimizes decision risk for uncertain access, but has monitoring/automation overhead and may cost more than Standard for consistently hot data.
- Early archival reduces storage cost quickly but can create restore delays and retrieval fees that hurt user-facing SLAs.
- Centralized guardrails improve consistency/compliance, but overly rigid rules can slow teams unless exception workflows are fast and transparent.

## 4) Event-Driven Processing Architecture
A media company uploads large files and triggers downstream transcoding and metadata extraction.
- How would you architect event flow from S3 to processing services while handling retries, duplicates, and backpressure?
- What service boundaries would you establish between ingestion, validation, transformation, and publication?
- How would you evolve the architecture to support new processing types without destabilizing existing pipelines?

**Model answer / solution**
- Route S3 notifications to EventBridge (or SNS/SQS fanout), then to decoupled queues per processor type; use DLQs, redrive policies, and idempotency keys (`bucket+key+versionId`).
- Split services into: ingestion API (auth + upload contract), validator (format/security checks), transformer workers (transcode/extract), and publisher/indexer.
- Persist workflow state in a job table/state machine (Step Functions or equivalent orchestration) so retries and partial failures are observable and resumable.
- Add new processors via new event rules/queues and contract-versioned events to avoid breaking existing consumers.

**Trade-offs**
- Strong decoupling with queues improves resilience and backpressure handling but increases end-to-end latency and operational overhead.
- Central orchestration improves observability/governance but can become a control-plane bottleneck or single complexity hotspot.
- Fine-grained services improve team autonomy and release safety, but raise distributed tracing, schema evolution, and ownership coordination costs.

## 5) Governance at Scale
Your organization has 200+ buckets across multiple business units.
- How would you define organization-wide standards for naming, encryption, logging, retention, and public access controls?
- What is your approach to policy-as-code, drift detection, and exception management?
- How do you balance central governance with team autonomy and delivery speed?

**Model answer / solution**
- Publish a baseline standard: naming convention, mandatory Block Public Access, SSE-KMS, access logging/CloudTrail data events, versioning, lifecycle minimums.
- Provision buckets only through approved IaC modules; disallow manual creation in production accounts via SCPs except break-glass roles.
- Implement policy-as-code with preventive (CI checks, admission controls) and detective controls (AWS Config + Security Hub + periodic conformance scans).
- Use time-bound exceptions with owner, risk rating, compensating controls, and automatic expiry notifications.

**Trade-offs**
- Strict central templates greatly reduce misconfiguration risk, but can frustrate product teams if module changes are slow.
- Detective-only governance preserves agility, but allows insecure drift windows and delayed remediation.
- Exception workflows improve practicality; without strict expiry/review they can accumulate and silently weaken the control environment.
