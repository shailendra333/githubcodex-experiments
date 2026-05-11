# Interview Questions: Angular 15+ Customer Dashboard Exercise

Use these questions after the candidate submits the login + dashboard exercise.

## 1) Architecture & Project Structure
1. Why did you choose your project structure (core/shared/features)?
2. What would you refactor if this app scaled from 2 pages to 20 pages?
3. How did you ensure separation of concerns between components and services?
4. If you used standalone components, what are the pros/cons vs NgModules?

## 2) Routing, Guards, and Navigation
1. How did you protect `/dashboard` from unauthenticated users?
2. Why did you choose `canActivate` (or `canMatch`) for the guard?
3. How do you handle redirect loops between `/login` and `/dashboard`?
4. How would you preload lazy-loaded modules for better UX?

## 3) Forms & Validation (Login)
1. Why did you use Reactive Forms here?
2. How do sync and async validators differ?
3. Show how your form prevents submission when invalid.
4. How would you add password strength validation and display helpful errors?
5. How would you unit test form validators?

## 4) Data Flow, RxJS, and State
1. Explain your search pipeline (`debounceTime`, `distinctUntilChanged`, `switchMap`, etc.).
2. Why choose `BehaviorSubject` for state?
3. How do you avoid memory leaks from subscriptions?
4. When would `combineLatest` vs `withLatestFrom` be more appropriate?
5. If API calls are rapid due to typing, how do you cancel stale requests?
6. How would you model loading/error/empty/data states reactively?

## 5) HTTP, Interceptors, and Error Handling
1. What responsibilities did your interceptor handle?
2. How did you attach and refresh auth tokens?
3. How do you distinguish global vs feature-level error handling?
4. What happens when the API returns 401 on dashboard calls?
5. How would you retry transient failures safely?

## 6) Dashboard Search & Pagination
1. Did you implement client-side or server-side pagination? Why?
2. How does the search interact with current page index?
3. How do you avoid performance issues with large client-side datasets?
4. How would you support sorting and filter chips?
5. How would you persist search and pagination in URL query params?

## 7) Component Design & Reusability
1. Which components are reusable in your solution?
2. How did you design input/output contracts for shared components?
3. When do you prefer smart vs dumb components?
4. If a design system is introduced later, what would you change?

## 8) Change Detection & Performance
1. Did you use `OnPush`? Why or why not?
2. What are common pitfalls that break `OnPush` assumptions?
3. How did you optimize list rendering (`trackBy`)?
4. How would Angular signals change your implementation?

## 9) Testing Strategy
1. Which parts did you unit test and why?
2. How did you test guard logic?
3. How did you test async RxJS flows in components/services?
4. What would you cover with integration/E2E tests?
5. How do you keep test suites maintainable as features grow?

## 10) Security & Reliability
1. What are the risks of storing tokens in `localStorage`?
2. How would you mitigate XSS risks in this app?
3. How would you add role-based authorization?
4. How would you audit sensitive actions (login/logout/view data)?

## 11) Accessibility & UX
1. How did you ensure keyboard accessibility for form and pagination?
2. How do screen readers announce validation errors?
3. What loading and empty states did you design for good UX?

## 12) Senior-Level Scenario Questions
1. Product asks for offline support tomorrow. What architecture changes are needed?
2. API latency suddenly increases to 2s. How do you preserve perceived performance?
3. A bug report says pagination resets unexpectedly after search. How do you debug it?
4. How would you instrument this app for observability (logs/metrics/traces)?
5. If migrating from Angular 15 to the latest major version, what risks do you assess first?

---

## Practical Live Follow-Up Tasks (Optional)
1. Add query-param persistence for search text and page.
2. Add a custom validator for password strength.
3. Add a retry strategy for 5xx errors with exponential backoff.
4. Add unit tests for one search stream edge case.
5. Convert one section to use Angular signals while keeping behavior unchanged.

---

## Scoring Guide for Interviewer (Suggested)
- **Strong hire**: Explains trade-offs clearly, writes maintainable/reactive code, demonstrates robust testing and performance awareness.
- **Borderline**: Can build feature but lacks reasoning on architecture, observability, or RxJS correctness.
- **No hire**: Relies on ad-hoc fixes, weak understanding of routing/security/testing fundamentals.
