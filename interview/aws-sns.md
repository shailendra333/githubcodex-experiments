# AWS SNS — Architecture & Design Interview Questions

## 1) Enterprise Notification Topology
You need to broadcast domain events to analytics, billing, fraud detection, and external partners.
- How would you design topic strategy: one global topic, domain topics, or bounded-context topics?
- How do you avoid tight coupling and message-contract sprawl across many consumers?
- What governance model would you define for schema evolution and ownership?

## 2) Reliability and Delivery Guarantees
Some subscribers require near-real-time updates; others can tolerate delays.
- How would you design for heterogeneous delivery requirements using SNS, SQS, and other integrations?
- How would you manage retries, DLQs, and replay strategy at system level?
- What SLOs and observability standards would you define for end-to-end event delivery?

## 3) Cross-Account and Multi-Environment Architecture
A central platform account publishes events consumed by apps in many AWS accounts.
- How would you structure IAM, topic policies, and account boundaries for secure multi-account eventing?
- What is your approach for promoting changes safely across dev/stage/prod without breaking subscribers?
- How would you handle incident isolation if one account is misconfigured or compromised?

## 4) Fan-out with Compliance Constraints
A regulated business must deliver notifications while maintaining auditability and data minimization.
- How would you separate sensitive from non-sensitive payloads in an event-driven architecture?
- Where would you place encryption, redaction, and compliance controls?
- What architectural patterns help preserve audit trails without overexposing data?

## 5) Strategic Evolution of Event Backbone
The company may later adopt EventBridge or Kafka for advanced routing.
- How would you design today’s SNS-based architecture to support future migration without high rework?
- What abstractions, contracts, and service boundaries would you define now?
- How would you evaluate migration readiness and business risk over time?
