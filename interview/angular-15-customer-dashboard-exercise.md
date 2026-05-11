# Angular 15+ Test Exercise: Customer Dashboard

## Objective
Build a small Angular **15+** application with two screens:
1. **Login**
2. **Dashboard** (customer list with search + pagination)

The goal is to evaluate core Angular skills and practical architecture decisions.

---

## Functional Requirements

### 1) Login Page
- Route: `/login`
- Fields:
  - `username` (required)
  - `password` (required, min length 8)
- Add real-time validation messages.
- Submit button disabled when form is invalid.
- On successful login:
  - Store a token in `localStorage` (mock token is fine).
  - Navigate to `/dashboard`.
- On failed login:
  - Show user-friendly error message.
- Include **logout** support from dashboard.

### 2) Dashboard Page
- Route: `/dashboard`
- Protected by an **Auth Guard**.
- Show customer table/card list with following columns:
  - `id`
  - `name`
  - `age`
  - `gender`
  - `address`
  - `email` (optional but recommended)
- Add a **search box** that filters on:
  - name
  - age
  - gender
  - address
- Add **pagination**:
  - page size selector (e.g., 5, 10, 20)
  - next/previous controls
  - current page indicator
- If no records match, show a friendly empty state.
- Show loading indicator while data is being fetched.

---

## Technical Requirements

### Angular Concepts to Cover
Try to demonstrate as many as possible:
- Standalone components or NgModule-based setup (either accepted).
- Routing with lazy loading.
- Route guards (`canActivate` and/or `canMatch`).
- Reactive Forms (`FormBuilder`, validators, custom validator optional).
- HTTP integration via `HttpClient`.
- Interceptors (attach auth token, handle unauthorized errors).
- RxJS:
  - `BehaviorSubject` for auth/user state
  - `debounceTime`, `distinctUntilChanged`, `switchMap` for search
  - `combineLatest` for search + pagination state
- Reusable components:
  - search input component
  - pagination component
- Custom pipe and/or directive (optional but encouraged).
- Error handling strategy.
- State management approach (service + RxJS is enough; NgRx optional).
- Unit tests for components/services/guards.

### Suggested Architecture
- `core/`: singleton services (auth, api, interceptors)
- `shared/`: reusable components/pipes/directives
- `features/auth`: login feature
- `features/dashboard`: dashboard feature
- `models/`: typed interfaces (Customer, AuthResponse, etc.)

### Data Source
Use either:
- mock JSON + local service, or
- mock API (json-server), or
- in-memory-web-api.

Sample customer model:

```ts
export interface Customer {
  id: number;
  name: string;
  age: number;
  gender: 'Male' | 'Female' | 'Other';
  address: string;
  email?: string;
}
```

---

## Non-Functional Requirements
- Clean folder structure.
- Strong typing (`strict` mode preferred).
- Avoid business logic in templates.
- Responsive layout.
- Basic accessibility (`label`, `aria-*`, keyboard friendly).
- Clear README with run/test instructions.

---

## Expected Deliverables
1. Source code repository.
2. README including:
   - setup and run steps
   - assumptions
   - trade-offs
3. Unit test results.
4. (Optional) short video walkthrough (3-5 minutes).

---

## Evaluation Rubric (100 points)
- Correctness of features: 30
- Angular architecture & code quality: 20
- RxJS and async flow handling: 15
- Forms, validation, and UX behavior: 10
- Routing/guard/interceptor usage: 10
- Testing quality: 10
- Documentation and clarity: 5

---

## Optional Stretch Goals
- Server-side pagination API integration.
- Persist dashboard filters in query params.
- Role-based access (admin vs viewer).
- Sorting by columns.
- Dark/light theme toggle.
- Skeleton loaders.

---

## Timebox
Recommended: **2 to 4 hours** for the core solution.
