import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * Lead-Level Java Streams – Set 2
 * ─────────────────────────────────────────────────────────────────────────────
 * Domain  : Employee / Department management system (same models as Set 1
 *           but with a richer 14-person dataset and new edge cases).
 *
 * Each method = one interview question.
 * Every solution follows the 5-step mental model documented in comments:
 *   1) define output shape
 *   2) filter scope / guard clauses
 *   3) transform / flatten / join
 *   4) aggregate / sort / rank
 *   5) collect final structure
 *
 * Questions in this set (harder than Set 1):
 *  Q1  – Salary quartile bands per department  (bucketing + rank)
 *  Q2  – Salary-inversion detector             (in-memory join anomaly)
 *  Q3  – Department budget utilisation         (salary cost vs headcount budget)
 *  Q4  – Single-contributor project risk       (flatMap + frequency filter)
 *  Q5  – Rating trend classifier               (list index arithmetic inside stream)
 *  Q6  – Region → Dept → Avg-salary 3-level map (nested groupingBy)
 *  Q7  – Top-N global skills leaderboard       (flatMap + frequency ranking)
 *  Q8  – Succession-plan for inactive managers (correlated join + composite score)
 *  Q9  – Overqualified employees               (per-dept avg skill count comparison)
 *  Q10 – Talent concentration risk             (top-salary vs remaining avg ratio)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class JavaLeadLevelStreamQuestionsAnswers2 {

    // ─── Domain models ────────────────────────────────────────────────────────

    static class Department {
        final int    id;
        final String name;
        final String region;
        final long   annualBudget;   // full department operating budget in USD

        Department(int id, String name, String region, long annualBudget) {
            this.id           = id;
            this.name         = name;
            this.region       = region;
            this.annualBudget = annualBudget;
        }
    }

    static class Employee {
        final int          id;
        final String       name;
        final int          departmentId;
        final double       salary;
        final int          experienceYears;
        final List<String> skills;
        final List<Integer> quarterlyRatings;   // most-recent last  [Q1, Q2, Q3, Q4]
        final List<String> projects;
        final Integer      managerId;           // null  = top-level
        final boolean      active;

        Employee(int id, String name, int departmentId, double salary, int experienceYears,
                 List<String> skills, List<Integer> quarterlyRatings,
                 List<String> projects, Integer managerId, boolean active) {
            this.id               = id;
            this.name             = name;
            this.departmentId     = departmentId;
            this.salary           = salary;
            this.experienceYears  = experienceYears;
            this.skills           = skills;
            this.quarterlyRatings = quarterlyRatings;
            this.projects         = projects;
            this.managerId        = managerId;
            this.active           = active;
        }

        double avgRating() {
            return quarterlyRatings.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
        }
    }

    // ─── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        List<Department> departments = departments();
        List<Employee>   employees   = employees();

        System.out.println("=== Lead-Level Java Streams – Set 2 (10 Questions + Answers) ===\n");

        q1SalaryQuartileBandsPerDepartment(departments, employees);
        q2SalaryInversionDetector(employees);
        q3DepartmentBudgetUtilisation(departments, employees);
        q4SingleContributorProjectRisk(employees);
        q5RatingTrendClassifier(employees);
        q6RegionDeptAvgSalaryNestedMap(departments, employees);
        q7TopNGlobalSkillsLeaderboard(employees, 5);
        q8SuccessionPlanForInactiveManagers(employees);
        q9OverqualifiedEmployees(departments, employees);
        q10TalentConcentrationRisk(departments, employees);
    }

    // ─── Q1 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 1
     * ──────────
     * For each department, assign every employee a salary quartile label
     * (Q1 = bottom 25 %, Q2 = 25–50 %, Q3 = 50–75 %, Q4 = top 25 %).
     * Print: dept name → [ "Alice(Q3)", "Bob(Q1)", … ]
     *
     * Why it's hard: quartile boundary requires ranking WITHIN a grouped partition,
     * i.e. you must compute relative position after sorting.
     */
    private static void q1SalaryQuartileBandsPerDepartment(
            List<Department> departments, List<Employee> employees) {

        System.out.println("Q1) Salary quartile band per employee, grouped by department.\n");

        Map<Integer, List<String>> result = employees.stream()
                // Step 1 (output shape): dept → employee-label list
                .collect(Collectors.groupingBy(
                        e -> e.departmentId,
                        Collectors.collectingAndThen(Collectors.toList(), deptEmps -> {
                            // Step 2 (sort within group): quartile needs ordered salary sequence
                            List<Employee> sorted = deptEmps.stream()
                                    .sorted(Comparator.comparingDouble(e -> e.salary))
                                    .collect(Collectors.toList());
                            int n = sorted.size();

                            // Step 3 (rank → quartile label): map positional index to band
                            return IntStream.range(0, n)
                                    .mapToObj(i -> {
                                        double pct = (double)(i + 1) / n;  // 1-based rank / total
                                        String band = pct <= 0.25 ? "Q1"
                                                    : pct <= 0.50 ? "Q2"
                                                    : pct <= 0.75 ? "Q3"
                                                    : "Q4";
                                        return sorted.get(i).name + "(" + band + ")";
                                    })
                                    .collect(Collectors.toList());
                        })));

        printDeptMap(departments, result);
    }

    // ─── Q2 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 2
     * ──────────
     * Find all active employees whose salary is HIGHER than their direct manager's
     * salary (salary-inversion anomaly).
     * Print: "report=Alice(120 000) > manager=Bob(110 000)"
     *
     * Why it's hard: requires an in-memory join (employee table joined back to itself
     * on managerId = id) before applying the comparison predicate.
     */
    private static void q2SalaryInversionDetector(List<Employee> employees) {
        System.out.println("\nQ2) Salary-inversion detector: active reports earning more than their manager.\n");

        // Step 1 (build lookup): id → Employee, needed for O(1) manager resolution
        Map<Integer, Employee> byId = employees.stream()
                .collect(Collectors.toMap(e -> e.id, Function.identity()));

        List<String> inversions = employees.stream()
                // Step 2 (filter scope): only active employees that have a manager
                .filter(e -> e.active && e.managerId != null)
                // Step 3 (join): resolve manager; guard against absent managers
                .filter(e -> byId.containsKey(e.managerId))
                // Step 4 (anomaly predicate): report earns strictly more than manager
                .filter(e -> e.salary > byId.get(e.managerId).salary)
                // Step 5 (readable output)
                .map(e -> {
                    Employee mgr = byId.get(e.managerId);
                    return String.format("report=%s(%.0f) > manager=%s(%.0f)",
                            e.name, e.salary, mgr.name, mgr.salary);
                })
                .sorted()
                .collect(Collectors.toList());

        if (inversions.isEmpty()) System.out.println("- (none detected)");
        else inversions.forEach(s -> System.out.println("- " + s));
    }

    // ─── Q3 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 3
     * ──────────
     * Calculate salary-cost utilisation per department:
     *   utilisation % = (sum of all employee salaries / department annual budget) * 100
     * Print depts where utilisation > 10 % as "dept → XX.X %".
     *
     * Why it's hard: requires joining two streams (departments + employees),
     * computing an aggregate per group, then correlating with budget metadata.
     */
    private static void q3DepartmentBudgetUtilisation(
            List<Department> departments, List<Employee> employees) {

        System.out.println("\nQ3) Department budget utilisation (salary cost / annual budget).\n");

        // Step 1 (pre-aggregate): total salary per department
        Map<Integer, Double> salaryByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.departmentId,
                        Collectors.summingDouble(e -> e.salary)));

        // Step 2 (join with departments): correlate salary cost to budget
        Map<String, Double> utilisation = departments.stream()
                .filter(d -> d.annualBudget > 0)
                // Step 3 (compute KPI): utilisation ratio
                .collect(Collectors.toMap(
                        d -> d.name,
                        d -> (salaryByDept.getOrDefault(d.id, 0.0) / d.annualBudget) * 100,
                        (a, b) -> a,
                        TreeMap::new));   // Step 4 (sorted output for readability)

        utilisation.entrySet().stream()
                .filter(e -> e.getValue() > 10)
                .forEach(e -> System.out.printf("- %-12s → %.1f %%\n", e.getKey(), e.getValue()));
    }

    // ─── Q4 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 4
     * ──────────
     * Identify projects that have only ONE contributor (single point of failure risk).
     * Print: "ProjectName (contributor: Alice)"
     *
     * Why it's hard: requires flatMap to explode the projects list, then grouping
     * back by project → contributor list, then filtering on size == 1.
     */
    private static void q4SingleContributorProjectRisk(List<Employee> employees) {
        System.out.println("\nQ4) Projects with a single contributor (SPOF risk).\n");

        Map<String, List<String>> projectToContributors = employees.stream()
                // Step 1 (explode one-to-many): each employee × project → pair
                .flatMap(e -> e.projects.stream()
                        .map(p -> Map.entry(p, e.name)))
                // Step 2 (group by project): collect all contributor names per project
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        projectToContributors.entrySet().stream()
                // Step 3 (risk filter): only single-contributor projects
                .filter(e -> e.getValue().size() == 1)
                // Step 4 (stable output order)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("- %-12s (contributor: %s)\n",
                        e.getKey(), e.getValue().get(0)));
    }

    // ─── Q5 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 5
     * ──────────
     * Classify each active employee's performance trajectory by examining
     * their last 3 quarterly ratings (index 1, 2, 3 in a 4-element list):
     *   IMPROVING   – each quarter strictly higher than the previous
     *   DECLINING   – each quarter strictly lower  than the previous
     *   VOLATILE    – not monotone
     * Print: "Alice → IMPROVING", etc.
     *
     * Why it's hard: requires list-index arithmetic inside a stream pipeline —
     * candidates often forget that List operations can be inlined via IntStream.range.
     */
    private static void q5RatingTrendClassifier(List<Employee> employees) {
        System.out.println("\nQ5) Rate-of-change trajectory for the last 3 quarters of each active employee.\n");

        employees.stream()
                .filter(e -> e.active && e.quarterlyRatings.size() >= 4)
                // Step 1 (compute trend from last 3 deltas): index 1→2 and 2→3
                .map(e -> {
                    List<Integer> r = e.quarterlyRatings;
                    int sz = r.size();
                    // last 3 ratings (most-recent 3 from tail)
                    int r1 = r.get(sz - 3);
                    int r2 = r.get(sz - 2);
                    int r3 = r.get(sz - 1);
                    String trend;
                    if (r2 > r1 && r3 > r2)       trend = "IMPROVING";
                    else if (r2 < r1 && r3 < r2)  trend = "DECLINING";
                    else                           trend = "VOLATILE";
                    // Step 2 (pair employee with label)
                    return Map.entry(e.name, trend);
                })
                // Step 3 (sort for deterministic output)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("- %-10s → %s\n", e.getKey(), e.getValue()));
    }

    // ─── Q6 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 6
     * ──────────
     * Produce a 3-level nested map:
     *   Region → DepartmentName → AverageSalary
     * Only include active employees.
     *
     * Why it's hard: nested groupingBy with a downstream that requires a
     * look-up of dept metadata (name, region) from the department list.
     */
    private static void q6RegionDeptAvgSalaryNestedMap(
            List<Department> departments, List<Employee> employees) {

        System.out.println("\nQ6) 3-level nested map: Region → Dept → Average salary (active employees only).\n");

        // Step 1 (pre-join): build id → dept lookup to resolve region and name
        Map<Integer, Department> deptById = departments.stream()
                .collect(Collectors.toMap(d -> d.id, Function.identity()));

        Map<String, Map<String, Double>> nestedMap = employees.stream()
                // Step 2 (filter scope)
                .filter(e -> e.active && deptById.containsKey(e.departmentId))
                // Step 3 (outer group = region): pull region from dept metadata
                .collect(Collectors.groupingBy(
                        e -> deptById.get(e.departmentId).region,
                        // Step 4 (inner group = dept name)
                        Collectors.groupingBy(
                                e -> deptById.get(e.departmentId).name,
                                // Step 5 (aggregate leaf = average salary)
                                Collectors.averagingDouble(e -> e.salary))));

        nestedMap.forEach((region, deptMap) -> {
            System.out.println("  Region: " + region);
            deptMap.forEach((dept, avg) ->
                    System.out.printf("    %-12s → avg salary $%.0f\n", dept, avg));
        });
    }

    // ─── Q7 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 7
     * ──────────
     * Produce a leaderboard of the top-N most common skills across ALL active
     * employees, with frequency counts, sorted descending.
     * Example (N=5): [ "Java(8)", "CRM(4)", "Compliance(3)", … ]
     *
     * Why it's hard: flatMap to the skill level, frequency count, then top-N
     * selection without modifying the original stream order until the very end.
     */
    private static void q7TopNGlobalSkillsLeaderboard(List<Employee> employees, int topN) {
        System.out.printf("\nQ7) Top-%d most common skills across all active employees.\n\n", topN);

        employees.stream()
                // Step 1 (filter scope)
                .filter(e -> e.active)
                // Step 2 (explode): employee → individual skill entries
                .flatMap(e -> e.skills.stream())
                // Step 3 (frequency map): skill → count
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                // Step 4 (sort descending by frequency, break ties alphabetically)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                // Step 5 (top-N slice)
                .limit(topN)
                .forEach(e -> System.out.printf("- %-25s %d employee(s)\n", e.getKey(), e.getValue()));
    }

    // ─── Q8 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 8
     * ──────────
     * Succession planning: for each INACTIVE manager, find their single best
     * active direct report measured by composite score:
     *   score = avgRating * 25 + experienceYears * 2
     * Print: "inactiveManager → bestSuccessor(score=XX)"
     *
     * Why it's hard: requires correlated join (reports → manager check),
     * a per-group max-by operation, and graceful empty-group handling.
     */
    private static void q8SuccessionPlanForInactiveManagers(List<Employee> employees) {
        System.out.println("\nQ8) Succession plan: best active direct report for each inactive manager.\n");

        // Step 1 (compute composite score helper)
        Function<Employee, Double> score =
                e -> e.avgRating() * 25 + e.experienceYears * 2.0;

        // Step 2 (build manager-id → active-reports map)
        Map<Integer, List<Employee>> activeReportsByManager = employees.stream()
                .filter(e -> e.managerId != null && e.active)
                .collect(Collectors.groupingBy(e -> e.managerId));

        Map<Integer, Employee> byId = employees.stream()
                .collect(Collectors.toMap(e -> e.id, Function.identity()));

        employees.stream()
                // Step 3 (filter inactive managers who have at least one active report)
                .filter(e -> !e.active && activeReportsByManager.containsKey(e.id))
                .forEach(inactiveMgr -> {
                    // Step 4 (best successor by composite score inside each group)
                    Optional<Employee> successor = activeReportsByManager.get(inactiveMgr.id).stream()
                            .max(Comparator.comparingDouble(score::apply));

                    successor.ifPresent(s -> System.out.printf(
                            "- %-8s → successor: %s(score=%.1f)\n",
                            inactiveMgr.name, s.name, score.apply(s)));
                });
    }

    // ─── Q9 ───────────────────────────────────────────────────────────────────
    /**
     * QUESTION 9
     * ──────────
     * Find active employees who are "overqualified" — they possess MORE skills
     * than the average skill count for their department.
     * Print: "Alice(dept=Engineering, skills=5, deptAvg=3.2)"
     *
     * Why it's hard: requires computing a per-department average FIRST, then
     * running a second pass to compare each individual against that average —
     * two-pass pattern common in analytical queries.
     */
    private static void q9OverqualifiedEmployees(
            List<Department> departments, List<Employee> employees) {

        System.out.println("\nQ9) Overqualified active employees (skill count > dept average skill count).\n");

        // Step 1 (first pass): compute average skill count per department
        Map<Integer, Double> avgSkillsByDept = employees.stream()
                .filter(e -> e.active)
                .collect(Collectors.groupingBy(
                        e -> e.departmentId,
                        Collectors.averagingDouble(e -> e.skills.size())));

        Map<Integer, String> deptNameById = departments.stream()
                .collect(Collectors.toMap(d -> d.id, d -> d.name));

        // Step 2 (second pass): filter employees above their dept average
        employees.stream()
                .filter(e -> e.active)
                .filter(e -> e.skills.size() > avgSkillsByDept.getOrDefault(e.departmentId, 0.0))
                .sorted(Comparator.comparing(e -> e.name))
                .forEach(e -> System.out.printf(
                        "- %-10s (dept=%-12s skills=%d, deptAvg=%.1f)\n",
                        e.name,
                        deptNameById.getOrDefault(e.departmentId, "?"),
                        e.skills.size(),
                        avgSkillsByDept.getOrDefault(e.departmentId, 0.0)));
    }

    // ─── Q10 ──────────────────────────────────────────────────────────────────
    /**
     * QUESTION 10
     * ───────────
     * Talent concentration risk: for each department, check whether the highest-paid
     * active employee earns more than 1.5× the average salary of ALL OTHER active
     * employees in that department. Such departments have fragile talent distribution.
     * Print: "dept → topSalary=XXX, othersAvg=YYY, ratio=Z.ZZ  ← AT RISK"
     *
     * Why it's hard: the "excluding one element" average requires splitting the
     * sorted list and using a downstream collector on the remainder — a classic
     * partition-then-reduce pattern.
     */
    private static void q10TalentConcentrationRisk(
            List<Department> departments, List<Employee> employees) {

        System.out.println("\nQ10) Talent concentration risk: top earner vs rest-of-department average.\n");

        Map<Integer, String> deptNameById = departments.stream()
                .collect(Collectors.toMap(d -> d.id, d -> d.name));

        employees.stream()
                .filter(e -> e.active)
                // Step 1 (group by dept): need whole dept list to compare top vs rest
                .collect(Collectors.groupingBy(e -> e.departmentId))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)   // need at least 2 employees
                .map(entry -> {
                    List<Employee> deptEmps = entry.getValue().stream()
                            // Step 2 (sort descending by salary)
                            .sorted(Comparator.comparingDouble((Employee e) -> e.salary).reversed())
                            .collect(Collectors.toList());

                    double topSalary  = deptEmps.get(0).salary;
                    // Step 3 (average of REMAINDER): sub-list excludes index 0
                    double othersAvg  = deptEmps.subList(1, deptEmps.size()).stream()
                            .mapToDouble(e -> e.salary)
                            .average()
                            .orElse(0);
                    double ratio      = othersAvg == 0 ? 0 : topSalary / othersAvg;
                    boolean atRisk    = ratio > 1.5;

                    // Step 4 (pair dept metadata with computed signal)
                    return Map.entry(
                            deptNameById.getOrDefault(entry.getKey(), "?"),
                            String.format("topSalary=$%.0f, othersAvg=$%.0f, ratio=%.2f%s",
                                    topSalary, othersAvg, ratio,
                                    atRisk ? "  ← AT RISK" : ""));
                })
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("- " + e.getKey() + " → " + e.getValue()));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static void printDeptMap(List<Department> departments, Map<Integer, ?> map) {
        Map<Integer, String> names = departments.stream()
                .collect(Collectors.toMap(d -> d.id, d -> d.name));
        map.forEach((id, val) ->
                System.out.println("- " + names.getOrDefault(id, "Unknown") + " → " + val));
    }

    // ─── Dataset ──────────────────────────────────────────────────────────────

    /**
     * Richer 14-employee dataset (vs 10 in Set 1).
     * Covers: cross-department contributors, salary inversions, inactive managers,
     * employees with no manager, volatile rating patterns, and budget stress.
     *
     * Intentional edge cases:
     *  - Employee 4  (Liam,  Sales):  inactive manager  → triggers Q8
     *  - Employee 11 (Raj):           earns MORE than manager 1 (Ava) → triggers Q2
     *  - Employee 12 (Chloe, HR):     inactive → should be excluded from live metrics
     *  - Employee 13 (Omar, Finance): declining ratings           → triggers Q5
     *  - Employee 14 (Yuki, Eng):     single project "Pulsar"      → triggers Q4
     */
    private static List<Employee> employees() {
        return Arrays.asList(
            // ── Engineering (dept 10) ──────────────────────────────────────────
            new Employee(1,  "Ava",     10, 145_000, 11,
                    Arrays.asList("Java", "Distributed Systems", "Kubernetes", "Go"),
                    Arrays.asList(5, 4, 5, 4),
                    Arrays.asList("Atlas", "Neptune"),   // Pulsar removed → Yuki is sole contributor
                    null, true),

            new Employee(2,  "Noah",    10, 120_000, 8,
                    Arrays.asList("Java", "Spring", "Observability"),
                    Arrays.asList(2, 3, 4, 5),   // IMPROVING (last 3: 3<4<5)
                    Arrays.asList("Atlas", "Ledger"),
                    1, true),

            new Employee(3,  "Mia",     10,  98_000, 5,
                    Arrays.asList("QA", "Automation", "Java"),
                    Arrays.asList(4, 3, 3, 2),   // DECLINING
                    Arrays.asList("Neptune"),
                    1, true),

            new Employee(11, "Raj",     10, 155_000, 12,   // salary > manager Ava → inversion
                    Arrays.asList("Java", "Distributed Systems", "Platform Engineering", "Rust"),
                    Arrays.asList(5, 5, 4, 5),
                    Arrays.asList("Atlas", "Orbital"),
                    1, true),

            new Employee(14, "Yuki",    10,  88_000, 3,
                    Arrays.asList("DevOps", "CI/CD"),
                    Arrays.asList(3, 3, 4, 4),
                    Arrays.asList("Pulsar"),     // single contributor → SPOF
                    1, true),

            // ── Sales (dept 20) ───────────────────────────────────────────────
            new Employee(4,  "Liam",    20, 160_000, 13,
                    Arrays.asList("Enterprise Negotiation", "CRM", "Forecasting"),
                    Arrays.asList(5, 5, 4, 5),
                    Arrays.asList("Mercury", "Atlas"),
                    null, false),   // INACTIVE → succession trigger

            new Employee(5,  "Emma",    20, 102_000, 7,
                    Arrays.asList("CRM", "Pipeline Management", "Forecasting"),
                    Arrays.asList(2, 3, 4, 5),   // IMPROVING (last 3: 3<4<5)
                    Arrays.asList("Mercury"),
                    4, true),

            new Employee(10, "Isabella",20,  89_000, 6,
                    Arrays.asList("Forecasting", "CRM", "Storytelling"),
                    Arrays.asList(4, 5, 4, 4),
                    Arrays.asList("Mercury", "Pulse"),
                    4, true),

            // ── HR (dept 30) ──────────────────────────────────────────────────
            new Employee(6,  "Sophia",  30,  92_000, 9,
                    Arrays.asList("Hiring", "Workforce Analytics", "Org Design"),
                    Arrays.asList(4, 5, 4, 4),
                    Arrays.asList("Pulse", "Neptune"),
                    null, true),

            new Employee(7,  "James",   30,  76_000, 4,
                    Arrays.asList("Hiring", "Interview Ops"),
                    Arrays.asList(2, 3, 2, 3),   // VOLATILE
                    Arrays.asList("Pulse"),
                    6, true),

            new Employee(12, "Chloe",   30,  82_000, 6,
                    Arrays.asList("Hiring", "Workforce Analytics"),
                    Arrays.asList(3, 4, 4, 4),
                    Arrays.asList("Pulse"),
                    6, false),   // INACTIVE

            // ── Finance (dept 40) ─────────────────────────────────────────────
            new Employee(8,  "Olivia",  40, 135_000, 10,
                    Arrays.asList("Risk", "Compliance", "FP&A", "Audit"),
                    Arrays.asList(5, 4, 5, 5),   // VOLATILE→ (not strictly monotone)
                    Arrays.asList("Ledger", "Mercury"),
                    null, true),

            new Employee(9,  "Ethan",   40,  99_000, 6,
                    Arrays.asList("Compliance", "Reporting"),
                    Arrays.asList(4, 4, 3, 2),   // DECLINING
                    Arrays.asList("Ledger"),
                    8, true),

            new Employee(13, "Omar",    40,  78_000, 3,
                    Arrays.asList("Reporting"),
                    Arrays.asList(5, 4, 3, 2),   // DECLINING
                    Arrays.asList("Ledger", "Orbital"),
                    8, true)
        );
    }

    private static List<Department> departments() {
        return Arrays.asList(
                new Department(10, "Engineering", "US-West",  3_000_000),
                new Department(20, "Sales",        "US-East",  2_000_000),
                new Department(30, "HR",            "US-East",  1_000_000),
                new Department(40, "Finance",       "US-West",  1_500_000)
        );
    }
}

