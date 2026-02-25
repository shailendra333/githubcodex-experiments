# JAX-RS — Architecture & Design Interview Questions

## 1) API Domain and Service Boundary Design
You are defining APIs for an enterprise platform with multiple product teams.
- How would you design JAX-RS resource boundaries to reflect domain ownership and avoid overlap?
- What criteria would you use to split services vs keep capabilities in one service?
- How does API design influence long-term team topology and delivery velocity?

## 2) Versioning and Backward Compatibility Strategy
Several client applications consume your APIs with different release cadences.
- What versioning strategy would you establish (URI, header, media type), and why?
- How would you govern deprecation timelines and compatibility guarantees across teams?
- What architectural controls prevent breaking changes from leaking to production?

## 3) Cross-Cutting Standards in a Microservices Estate
You have 40+ JAX-RS services built by different squads.
- How would you standardize authentication/authorization, error models, correlation IDs, and observability?
- What belongs in shared libraries vs platform sidecars/gateways?
- How would you avoid over-centralization while enforcing engineering standards?

## 4) Performance and Resilience by Design
Critical APIs face bursty traffic and strict latency SLOs.
- How would you architect request handling, caching, timeout budgets, and fallback policies at platform level?
- How would you define dependency isolation patterns for downstream calls?
- What metrics and architecture review gates would you require before launch?

## 5) API Platform Roadmap and Governance
The organization is moving toward API-first product development.
- How would you define an API governance model including review boards, standards, and exception handling?
- How would you align JAX-RS implementation choices with future goals like GraphQL or service mesh adoption?
- What long-term architecture principles would you document to guide teams consistently?
