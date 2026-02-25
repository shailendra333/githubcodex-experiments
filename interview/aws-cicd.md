# AWS CI/CD — Architecture & Design Interview Questions

## 1) Enterprise Delivery Architecture
A company with 150 engineering teams wants a unified CI/CD strategy on AWS.
- How would you define platform boundaries between central DevOps tooling and team-owned pipelines?
- What decisions belong to global standards vs local team autonomy?
- How would you structure the architecture for scale, security, and speed simultaneously?

## 2) Multi-Account, Multi-Region Deployment Model
Applications run across many accounts and regions with different compliance requirements.
- How would you design cross-account deployment roles, artifact promotion, and environment segregation?
- What is your strategy for region-specific release orchestration and rollback?
- How would you enforce consistent controls while supporting diverse workload needs?

## 3) Security and Compliance by Design
Regulated workloads require strict change control and traceability.
- How would you architect approvals, policy checks, signing, and audit trails in the pipeline lifecycle?
- Which controls should be preventive vs detective?
- How would you balance compliance requirements with developer productivity?

## 4) Reliability, Quality Gates, and Release Strategy
A critical customer-facing system needs rapid releases with low incident tolerance.
- How would you architect quality gates (unit/integration/perf/security) and progressive delivery (canary/blue-green)?
- How would you define global rollback patterns and incident response integration?
- What reliability standards and ownership model would you establish for the CI/CD platform itself?

## 5) Technology Roadmap and Platform Evolution
Current pipelines are fragmented and team-specific.
- How would you create a roadmap from ad hoc pipelines to a standardized internal developer platform?
- What principles guide build-vs-buy decisions among CodePipeline, GitHub Actions, Jenkins, and Terraform-based workflows?
- How would you phase adoption to reduce migration risk and preserve delivery momentum?
