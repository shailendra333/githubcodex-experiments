# AWS S3 — Architecture & Design Interview Questions

## 1) Multi-Region Data Strategy
You are designing a platform that stores regulatory documents used by customers in North America, Europe, and APAC.
- How would you decide between single-region buckets, Multi-Region Access Points, and cross-region replication?
- What are the trade-offs in latency, sovereignty, data residency, and operational complexity?
- What would your failure-mode design look like during a region outage?

## 2) Security Boundary Design
A product team wants to allow direct browser uploads to S3 from untrusted clients.
- How would you define trust boundaries and IAM policies to avoid privilege escalation?
- When would you use pre-signed URLs vs API proxying through backend services?
- How do KMS key strategy and bucket policies align with least privilege and tenant isolation?

## 3) Data Lifecycle and Cost Governance
You expect 5 PB growth over 3 years with highly variable access patterns.
- How would you create a storage class and lifecycle strategy (Standard, Intelligent-Tiering, Glacier tiers)?
- What telemetry would you require before making lifecycle transitions?
- How would you enforce cost guardrails and architecture standards across all teams?

## 4) Event-Driven Processing Architecture
A media company uploads large files and triggers downstream transcoding and metadata extraction.
- How would you architect event flow from S3 to processing services while handling retries, duplicates, and backpressure?
- What service boundaries would you establish between ingestion, validation, transformation, and publication?
- How would you evolve the architecture to support new processing types without destabilizing existing pipelines?

## 5) Governance at Scale
Your organization has 200+ buckets across multiple business units.
- How would you define organization-wide standards for naming, encryption, logging, retention, and public access controls?
- What is your approach to policy-as-code, drift detection, and exception management?
- How do you balance central governance with team autonomy and delivery speed?
