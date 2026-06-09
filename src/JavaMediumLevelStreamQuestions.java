import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  Medium-Level Java Streams – 20 Questions + Answers
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Audience  : Mid-level Java developers (2–5 years experience).
 *  Goal      : Test solid working knowledge of the Stream API without
 *              requiring multi-level grouping or custom collector tricks.
 *
 *  Each question covers ONE primary concept:
 *  ┌────┬──────────────────────────────────────────────────────────────┐
 *  │ Q1 │ filter + collect                                             │
 *  │ Q2 │ map + collect (extract field)                               │
 *  │ Q3 │ count                                                        │
 *  │ Q4 │ reduce for total salary                                      │
 *  │ Q5 │ mapToDouble + sum                                            │
 *  │ Q6 │ mapToDouble + average + Optional                            │
 *  │ Q7 │ max with Comparator                                          │
 *  │ Q8 │ min with Comparator                                          │
 *  │ Q9 │ sorted + limit  (top-N)                                      │
 *  │Q10 │ sorted + skip   (pagination)                                 │
 *  │Q11 │ distinct on mapped values                                    │
 *  │Q12 │ anyMatch / allMatch / noneMatch                              │
 *  │Q13 │ findFirst with filter                                        │
 *  │Q14 │ Collectors.joining                                           │
 *  │Q15 │ flatMap (one-to-many)                                        │
 *  │Q16 │ groupingBy (basic)                                           │
 *  │Q17 │ groupingBy + counting downstream                             │
 *  │Q18 │ partitioningBy                                               │
 *  │Q19 │ Collectors.toMap                                             │
 *  │Q20 │ groupingBy + averagingDouble downstream                      │
 *  └────┴──────────────────────────────────────────────────────────────┘
 *
 *  Domain: Employee records with department, salary, skills, seniority.
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class JavaMediumLevelStreamQuestions {

    // ─────────────────────────── Domain model ────────────────────────────────

    static class Employee {
        final int          id;
        final String       name;
        final String       department;   // plain name, no foreign key needed
        final double       salary;
        final int          experienceYears;
        final String       role;         // "Junior" | "Mid" | "Senior" | "Lead"
        final List<String> skills;
        final boolean      active;

        Employee(int id, String name, String department, double salary,
                 int experienceYears, String role, List<String> skills, boolean active) {
            this.id              = id;
            this.name            = name;
            this.department      = department;
            this.salary          = salary;
            this.experienceYears = experienceYears;
            this.role            = role;
            this.skills          = skills;
            this.active          = active;
        }

        @Override
        public String toString() {
            return name + "(" + department + ", $" + (int) salary + ")";
        }
    }

    // ─────────────────────────── Entry point ─────────────────────────────────

    public static void main(String[] args) {
        List<Employee> employees = employees();

        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("  Medium-Level Java Streams – 20 Questions + Answers");
        System.out.println("══════════════════════════════════════════════════════\n");

        q1FilterActiveEmployees(employees);
        q2ExtractAllNames(employees);
        q3CountSeniorEmployees(employees);
        q4TotalSalaryWithReduce(employees);
        q5TotalSalaryWithMapToDouble(employees);
        q6AverageSalaryOfActive(employees);
        q7HighestPaidEmployee(employees);
        q8LeastExperiencedEmployee(employees);
        q9Top3HighestPaidNames(employees);
        q10PageTwoByExperienceDesc(employees);
        q11DistinctDepartments(employees);
        q12MatchChecks(employees);
        q13FirstSeniorInEngineering(employees);
        q14CommaSeparatedActiveNames(employees);
        q15AllSkillsNoDuplicates(employees);
        q16GroupByDepartment(employees);
        q17CountPerDepartment(employees);
        q18PartitionBySalaryThreshold(employees, 100_000);
        q19NameToSalaryMap(employees);
        q20AvgSalaryPerDepartment(employees);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTIONS & ANSWERS
    // ═══════════════════════════════════════════════════════════════════════

    // ─── Q1 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 1 – filter + collect
     * ─────────────────────────────
     * Return a List of all currently ACTIVE employees.
     *
     * Concept: filter() removes elements that fail the predicate;
     * collect(Collectors.toList()) materialises the stream into a List.
     */
    private static void q1FilterActiveEmployees(List<Employee> employees) {
        System.out.println("Q1) List all active employees.\n");

        List<Employee> active = employees.stream()
                .filter(e -> e.active)               // keep only active
                .collect(Collectors.toList());

        active.forEach(e -> System.out.println("  " + e));
    }

    // ─── Q2 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 2 – map + collect
     * ──────────────────────────
     * Return a List of just the employee NAMES (all employees, not only active).
     *
     * Concept: map() transforms each element; here Employee → String (name).
     */
    private static void q2ExtractAllNames(List<Employee> employees) {
        System.out.println("\nQ2) Extract all employee names.\n");

        List<String> names = employees.stream()
                .map(e -> e.name)                    // Employee → name String
                .collect(Collectors.toList());

        System.out.println("  " + names);
    }

    // ─── Q3 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 3 – count
     * ──────────────────
     * How many employees have the role "Senior"?
     *
     * Concept: count() is a terminal operation that returns a long.
     * Combine with filter() to count a subset.
     */
    private static void q3CountSeniorEmployees(List<Employee> employees) {
        System.out.println("\nQ3) Count employees with role 'Senior'.\n");

        long count = employees.stream()
                .filter(e -> "Senior".equals(e.role))
                .count();                             // terminal: returns long

        System.out.println("  Senior employees: " + count);
    }

    // ─── Q4 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 4 – reduce
     * ───────────────────
     * Compute the total salary of ALL employees using reduce().
     *
     * Concept: reduce(identity, accumulator) folds the stream into a single
     * value. Identity is the neutral element (0 for addition).
     */
    private static void q4TotalSalaryWithReduce(List<Employee> employees) {
        System.out.println("\nQ4) Total salary of all employees using reduce().\n");

        double total = employees.stream()
                .map(e -> e.salary)
                .reduce(0.0, Double::sum);           // identity=0.0, accumulator=+

        System.out.printf("  Total salary: $%,.0f%n", total);
    }

    // ─── Q5 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 5 – mapToDouble + sum
     * ────────────────────────────────
     * Compute the same total salary using mapToDouble().sum().
     *
     * Concept: mapToDouble() returns a DoubleStream which has a built-in
     * sum() terminal. Preferred over reduce() for numeric aggregations.
     */
    private static void q5TotalSalaryWithMapToDouble(List<Employee> employees) {
        System.out.println("\nQ5) Total salary using mapToDouble().sum().\n");

        double total = employees.stream()
                .mapToDouble(e -> e.salary)          // Stream<Employee> → DoubleStream
                .sum();                              // DoubleStream terminal

        System.out.printf("  Total salary: $%,.0f%n", total);
    }

    // ─── Q6 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 6 – mapToDouble + average + Optional
     * ────────────────────────────────────────────────
     * Return the average salary of ACTIVE employees.
     * average() returns OptionalDouble — handle the empty case.
     *
     * Concept: OptionalDouble.orElse() provides a safe default if the stream
     * is empty (e.g. no active employees).
     */
    private static void q6AverageSalaryOfActive(List<Employee> employees) {
        System.out.println("\nQ6) Average salary of active employees.\n");

        OptionalDouble avg = employees.stream()
                .filter(e -> e.active)
                .mapToDouble(e -> e.salary)
                .average();                          // returns OptionalDouble

        System.out.printf("  Average salary: $%,.0f%n", avg.orElse(0));
    }

    // ─── Q7 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 7 – max with Comparator
     * ──────────────────────────────────
     * Find the highest-paid employee.
     *
     * Concept: max(Comparator) returns Optional<T>. Use
     * Comparator.comparingDouble() for type-safe field comparison.
     */
    private static void q7HighestPaidEmployee(List<Employee> employees) {
        System.out.println("\nQ7) Highest-paid employee.\n");

        Optional<Employee> top = employees.stream()
                .max(Comparator.comparingDouble(e -> e.salary));

        top.ifPresent(e -> System.out.println("  " + e));
    }

    // ─── Q8 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 8 – min with Comparator
     * ──────────────────────────────────
     * Find the employee with the least years of experience.
     *
     * Concept: min(Comparator) is symmetric to max — it returns the element
     * that compares smallest according to the provided comparator.
     */
    private static void q8LeastExperiencedEmployee(List<Employee> employees) {
        System.out.println("\nQ8) Least experienced employee.\n");

        Optional<Employee> junior = employees.stream()
                .min(Comparator.comparingInt(e -> e.experienceYears));

        junior.ifPresent(e -> System.out.println("  " + e.name
                + " (" + e.experienceYears + " yrs)"));
    }

    // ─── Q9 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 9 – sorted + limit (top-N)
     * ─────────────────────────────────────
     * Return the names of the top 3 highest-paid employees.
     *
     * Concept: sorted() with a reversed comparator orders descending;
     * limit(n) truncates the stream to at most n elements.
     * Chain map() afterwards to project to names.
     */
    private static void q9Top3HighestPaidNames(List<Employee> employees) {
        System.out.println("\nQ9) Top 3 highest-paid employee names.\n");

        List<String> top3 = employees.stream()
                .sorted(Comparator.comparingDouble((Employee e) -> e.salary).reversed())
                .limit(3)                            // top-3 slice
                .map(e -> e.name)
                .collect(Collectors.toList());

        System.out.println("  " + top3);
    }

    // ─── Q10 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 10 – sorted + skip (pagination / offset)
     * ────────────────────────────────────────────────────
     * Get "page 2" of employees sorted by experience descending,
     * where each page has 3 employees (skip first 3, take next 3).
     *
     * Concept: skip(n) discards the first n elements — combined with limit()
     * this implements offset-based pagination.
     */
    private static void q10PageTwoByExperienceDesc(List<Employee> employees) {
        System.out.println("\nQ10) Page 2 of employees sorted by experience desc (skip 3, take 3).\n");

        List<String> page2 = employees.stream()
                .sorted(Comparator.comparingInt((Employee e) -> e.experienceYears).reversed())
                .skip(3)                             // skip page 1
                .limit(3)                            // page size = 3
                .map(e -> e.name + "(" + e.experienceYears + " yrs)")
                .collect(Collectors.toList());

        System.out.println("  " + page2);
    }

    // ─── Q11 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 11 – distinct on mapped values
     * ────────────────────────────────────────
     * Return a sorted list of DISTINCT department names present in the data.
     *
     * Concept: map() extracts the field, distinct() removes duplicates
     * (based on equals()), sorted() orders the result.
     */
    private static void q11DistinctDepartments(List<Employee> employees) {
        System.out.println("\nQ11) Distinct department names, sorted alphabetically.\n");

        List<String> depts = employees.stream()
                .map(e -> e.department)
                .distinct()                          // deduplicate mapped values
                .sorted()
                .collect(Collectors.toList());

        System.out.println("  " + depts);
    }

    // ─── Q12 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 12 – anyMatch / allMatch / noneMatch
     * ───────────────────────────────────────────────
     * Answer three boolean checks about the dataset:
     *  a) Does ANY employee have a salary above $150 000?
     *  b) Are ALL active employees earning at least $70 000?
     *  c) Is there NO employee with 0 years of experience?
     *
     * Concept: short-circuit terminal operations — they stop processing as
     * soon as the outcome is determined.
     */
    private static void q12MatchChecks(List<Employee> employees) {
        System.out.println("\nQ12) anyMatch / allMatch / noneMatch checks.\n");

        boolean anyAbove150k = employees.stream()
                .anyMatch(e -> e.salary > 150_000);                       // (a)

        boolean allActiveAbove70k = employees.stream()
                .filter(e -> e.active)
                .allMatch(e -> e.salary >= 70_000);                       // (b)

        boolean noneZeroExp = employees.stream()
                .noneMatch(e -> e.experienceYears == 0);                  // (c)

        System.out.println("  Any salary > $150 000?            " + anyAbove150k);
        System.out.println("  All active earning >= $70 000?    " + allActiveAbove70k);
        System.out.println("  None with 0 years experience?     " + noneZeroExp);
    }

    // ─── Q13 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 13 – findFirst with filter
     * ─────────────────────────────────────
     * Find the first Senior employee in the Engineering department
     * (in encounter order).
     *
     * Concept: findFirst() returns Optional<T> containing the first element
     * that survives all upstream filter/map operations, or empty() if none.
     */
    private static void q13FirstSeniorInEngineering(List<Employee> employees) {
        System.out.println("\nQ13) First Senior employee in Engineering.\n");

        Optional<Employee> found = employees.stream()
                .filter(e -> "Engineering".equals(e.department))
                .filter(e -> "Senior".equals(e.role))
                .findFirst();                        // returns Optional<Employee>

        found.ifPresentOrElse(
                e  -> System.out.println("  Found: " + e),
                () -> System.out.println("  (none found)"));
    }

    // ─── Q14 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 14 – Collectors.joining
     * ──────────────────────────────────
     * Produce a comma-separated string of active employee names,
     * sorted alphabetically, wrapped in brackets: "[Alice, Bob, Carol]"
     *
     * Concept: Collectors.joining(delimiter, prefix, suffix) concatenates
     * a stream of CharSequence elements — map to String first.
     */
    private static void q14CommaSeparatedActiveNames(List<Employee> employees) {
        System.out.println("\nQ14) Comma-separated active employee names in alphabetical order.\n");

        String result = employees.stream()
                .filter(e -> e.active)
                .map(e -> e.name)
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));

        System.out.println("  " + result);
    }

    // ─── Q15 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 15 – flatMap (one-to-many explosion)
     * ──────────────────────────────────────────────
     * Return a sorted list of ALL unique skills possessed by active employees.
     *
     * Concept: flatMap(Collection::stream) converts each element's sub-list
     * into a flat stream of individual items. Combine with distinct() + sorted().
     */
    private static void q15AllSkillsNoDuplicates(List<Employee> employees) {
        System.out.println("\nQ15) All unique skills of active employees, sorted.\n");

        List<String> allSkills = employees.stream()
                .filter(e -> e.active)
                .flatMap(e -> e.skills.stream())    // List<String> → stream of Strings
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        System.out.println("  " + allSkills);
    }

    // ─── Q16 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 16 – groupingBy (basic)
     * ──────────────────────────────────
     * Group active employees by department name.
     * Result: Map<String, List<Employee>>
     *
     * Concept: Collectors.groupingBy(classifier) partitions the stream into
     * a Map where the key is produced by the classifier function.
     */
    private static void q16GroupByDepartment(List<Employee> employees) {
        System.out.println("\nQ16) Group active employees by department.\n");

        Map<String, List<Employee>> byDept = employees.stream()
                .filter(e -> e.active)
                .collect(Collectors.groupingBy(e -> e.department));   // key = dept name

        byDept.forEach((dept, emps) -> {
            List<String> names = emps.stream().map(e -> e.name).collect(Collectors.toList());
            System.out.println("  " + dept + " → " + names);
        });
    }

    // ─── Q17 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 17 – groupingBy + counting downstream
     * ─────────────────────────────────────────────────
     * Count how many employees (active + inactive) exist per department.
     * Result: Map<String, Long>
     *
     * Concept: the second argument to groupingBy is a downstream collector.
     * Collectors.counting() counts the elements in each bucket.
     */
    private static void q17CountPerDepartment(List<Employee> employees) {
        System.out.println("\nQ17) Employee count per department (all employees).\n");

        Map<String, Long> countByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.department,
                        Collectors.counting()));     // downstream = count per group

        countByDept.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-14s %d%n", e.getKey(), e.getValue()));
    }

    // ─── Q18 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 18 – partitioningBy
     * ──────────────────────────────
     * Partition ALL employees into two groups:
     *   true  → salary >= threshold
     *   false → salary <  threshold
     * Result: Map<Boolean, List<Employee>>
     *
     * Concept: partitioningBy is a special-case groupingBy with exactly
     * two buckets (true / false). Cleaner than groupingBy when the
     * classifier is a boolean predicate.
     */
    private static void q18PartitionBySalaryThreshold(List<Employee> employees,
                                                       double threshold) {
        System.out.printf("\nQ18) Partition employees by salary >= $%,.0f.%n%n", threshold);

        Map<Boolean, List<String>> partition = employees.stream()
                .collect(Collectors.partitioningBy(
                        e -> e.salary >= threshold,
                        Collectors.mapping(e -> e.name, Collectors.toList())));

        System.out.println("  Above threshold : " + partition.get(true));
        System.out.println("  Below threshold : " + partition.get(false));
    }

    // ─── Q19 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 19 – Collectors.toMap
     * ────────────────────────────────
     * Build a Map<String, Double> of employee name → salary for active employees.
     *
     * Concept: Collectors.toMap(keyMapper, valueMapper) creates a Map from
     * stream elements. Watch out: throws IllegalStateException on duplicate keys.
     * Use the merge-function overload to handle duplicates explicitly.
     */
    private static void q19NameToSalaryMap(List<Employee> employees) {
        System.out.println("\nQ19) Map of name → salary for active employees.\n");

        Map<String, Double> nameToSalary = employees.stream()
                .filter(e -> e.active)
                .collect(Collectors.toMap(
                        e -> e.name,             // key
                        e -> e.salary,           // value
                        (a, b) -> a));           // merge fn (keeps first on dup name)

        nameToSalary.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-10s $%,.0f%n", e.getKey(), e.getValue()));
    }

    // ─── Q20 ─────────────────────────────────────────────────────────────────
    /**
     * QUESTION 20 – groupingBy + averagingDouble downstream
     * ────────────────────────────────────────────────────────
     * Compute the average salary per department (active employees only),
     * sorted by average descending.
     *
     * Concept: Collectors.averagingDouble() is a downstream collector that
     * computes a Double average inside each group produced by groupingBy.
     */
    private static void q20AvgSalaryPerDepartment(List<Employee> employees) {
        System.out.println("\nQ20) Average salary per department (active employees), sorted descending.\n");

        Map<String, Double> avgByDept = employees.stream()
                .filter(e -> e.active)
                .collect(Collectors.groupingBy(
                        e -> e.department,
                        Collectors.averagingDouble(e -> e.salary)));   // downstream avg

        avgByDept.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  %-14s $%,.0f%n", e.getKey(), e.getValue()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DATASET  (12 employees, 4 departments)
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Dataset is intentionally crafted to exercise all 20 questions:
     *  – Salary range  $62 000 – $158 000 (threshold at $100 000 splits ~50/50)
     *  – Mix of active / inactive employees
     *  – Duplicate department names for grouping
     *  – Overlapping skills for flatMap deduplication
     *  – Multiple roles including 2 Seniors in Engineering for findFirst test
     */
    private static List<Employee> employees() {
        return Arrays.asList(
            // ── Engineering ───────────────────────────────────────────────
            new Employee(1,  "Alice",   "Engineering", 135_000, 10, "Senior",
                    Arrays.asList("Java", "Kubernetes", "Go"),         true),
            new Employee(2,  "Bob",     "Engineering", 98_000,   6, "Mid",
                    Arrays.asList("Java", "Spring", "Docker"),         true),
            new Employee(3,  "Carol",   "Engineering", 158_000, 14, "Lead",
                    Arrays.asList("Java", "Distributed Systems", "Rust"), true),
            new Employee(4,  "Dave",    "Engineering", 72_000,   2, "Junior",
                    Arrays.asList("Python", "SQL"),                    true),
            new Employee(5,  "Eve",     "Engineering", 115_000,  8, "Senior",
                    Arrays.asList("Kubernetes", "CI/CD", "Go"),        false), // inactive

            // ── Sales ─────────────────────────────────────────────────────
            new Employee(6,  "Frank",   "Sales",       105_000,  9, "Senior",
                    Arrays.asList("CRM", "Salesforce", "Forecasting"), true),
            new Employee(7,  "Grace",   "Sales",       88_000,   5, "Mid",
                    Arrays.asList("CRM", "Pipeline Management"),       true),
            new Employee(8,  "Hank",    "Sales",       62_000,   1, "Junior",
                    Arrays.asList("Salesforce"),                       true),

            // ── HR ────────────────────────────────────────────────────────
            new Employee(9,  "Iris",    "HR",          92_000,   7, "Senior",
                    Arrays.asList("Hiring", "Org Design", "HRIS"),     true),
            new Employee(10, "Jack",    "HR",          76_000,   4, "Mid",
                    Arrays.asList("Hiring", "Interview Ops"),          false), // inactive

            // ── Finance ───────────────────────────────────────────────────
            new Employee(11, "Karen",   "Finance",     128_000, 11, "Lead",
                    Arrays.asList("FP&A", "Risk", "Compliance"),       true),
            new Employee(12, "Leo",     "Finance",     95_000,   5, "Mid",
                    Arrays.asList("Compliance", "Reporting", "SQL"),   true)
        );
    }
}

