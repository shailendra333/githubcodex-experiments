import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lead-level Java Streams practice using Employee + Department datasets.
 *
 * Each problem includes:
 * - Question statement
 * - Stream-first answer
 * - Step comments that map to a mental model:
 *   1) define output shape
 *   2) filter scope
 *   3) transform/flatten
 *   4) aggregate/sort
 *   5) collect final structure
 */
public class JavaLeadLevelStreamQuestionsAnswers {

    static class Department {
        final int id;
        final String name;
        final String region;
        final int budget;

        Department(int id, String name, String region, int budget) {
            this.id = id;
            this.name = name;
            this.region = region;
            this.budget = budget;
        }
    }

    static class Employee {
        final int id;
        final String name;
        final int departmentId;
        final double salary;
        final int experienceYears;
        final List<String> skills;
        final List<Integer> quarterlyRatings;
        final List<String> projects;
        final Integer managerId;
        final boolean active;

        Employee(int id, String name, int departmentId, double salary, int experienceYears,
                 List<String> skills, List<Integer> quarterlyRatings, List<String> projects,
                 Integer managerId, boolean active) {
            this.id = id;
            this.name = name;
            this.departmentId = departmentId;
            this.salary = salary;
            this.experienceYears = experienceYears;
            this.skills = skills;
            this.quarterlyRatings = quarterlyRatings;
            this.projects = projects;
            this.managerId = managerId;
            this.active = active;
        }

        double avgRating() {
            return quarterlyRatings.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
    }

    public static void main(String[] args) {
        List<Department> departments = departments();
        List<Employee> employees = employees();

        System.out.println("=== 10 Lead-Level Java Streams Questions + Answers ===\n");

        q1TopTwoByWeightedScorePerDepartment(departments, employees);
        q2DepartmentsWithHighSalaryVariance(departments, employees);
        q3StrategicSkillCoverageByDepartment(departments, employees);
        q4ManagersWithTeamSizeAndAvgRating(employees);
        q5ProjectToCrossDepartmentContributors(departments, employees);
        q6DepartmentRiskIndexFromLowRatings(departments, employees);
        q7SkillAdjacencyMapForSeniorEngineers(employees);
        q8DepartmentMedianSalary(departments, employees);
        q9FindInactiveManagersWithActiveReports(employees);
        q10PromotionShortlistByCompositeRule(departments, employees);
    }

    // Q1
    private static void q1TopTwoByWeightedScorePerDepartment(List<Department> departments, List<Employee> employees) {
        System.out.println("Q1) Top 2 active employees per department using weighted score (avgRating*20 + skillCount*3 + experience).");

        Map<Integer, List<String>> result = employees.stream()
                // Step 1 (mental model: scope): keep only active employees because ranking inactive workers is noise.
                .filter(e -> e.active)
                // Step 2 (mental model: group first): rank must happen per department, so build departmental buckets.
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                // Step 3 (mental model: sort by business score): translate profile -> comparable score.
                                .sorted(Comparator.comparingDouble((Employee e) -> (e.avgRating() * 20)
                                                + (e.skills.size() * 3)
                                                + e.experienceYears)
                                        .reversed())
                                // Step 4 (mental model: constrain output): leadership asked top 2 only.
                                .limit(2)
                                // Step 5 (mental model: final shape): return readable labels for decision meetings.
                                .map(e -> e.name + "(score=" + String.format("%.2f", (e.avgRating() * 20)
                                        + (e.skills.size() * 3) + e.experienceYears) + ")")
                                .collect(Collectors.toList()))));

        printDepartmentMap(departments, result);
    }

    // Q2
    private static void q2DepartmentsWithHighSalaryVariance(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ2) Find departments where maxSalary/minSalary >= 1.8 (pay compression risk).\n");

        Map<Integer, DoubleSummaryStatistics> salaryStatsByDept = employees.stream()
                // Step 1 (mental model: target metric): variance needs min and max in one pass.
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.summarizingDouble(e -> e.salary)));

        Map<Integer, Double> riskyDepartments = salaryStatsByDept.entrySet().stream()
                // Step 2 (mental model: derive indicator): compute inequality ratio from pre-aggregated stats.
                .filter(entry -> entry.getValue().getMin() > 0
                        && (entry.getValue().getMax() / entry.getValue().getMin()) >= 1.8)
                // Step 3 (mental model: publish business signal): keep deptId -> ratio only.
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getMax() / entry.getValue().getMin()));

        printDepartmentMap(departments, riskyDepartments);
    }

    // Q3
    private static void q3StrategicSkillCoverageByDepartment(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ3) Department-wise strategic skill coverage (employees matching at least 1 priority skill).\n");

        Map<String, Set<String>> prioritySkillsByDeptName = Map.of(
                "Engineering", Set.of("Java", "Distributed Systems", "Kubernetes"),
                "Sales", Set.of("Enterprise Negotiation", "CRM", "Forecasting"),
                "HR", Set.of("Workforce Analytics", "Hiring", "Org Design"),
                "Finance", Set.of("Risk", "FP&A", "Compliance")
        );

        Map<Integer, String> deptIdToName = departments.stream()
                .collect(Collectors.toMap(d -> d.id, d -> d.name));

        Map<Integer, Long> coverage = employees.stream()
                // Step 1 (mental model: keep relevant population): only active employees are capacity.
                .filter(e -> e.active)
                // Step 2 (mental model: boolean test): does employee intersect with strategic skills?
                .filter(e -> e.skills.stream().anyMatch(skill ->
                        prioritySkillsByDeptName.getOrDefault(deptIdToName.get(e.departmentId), Set.of()).contains(skill)))
                // Step 3 (mental model: count by owner): coverage is a department KPI.
                .collect(Collectors.groupingBy(e -> e.departmentId, Collectors.counting()));

        printDepartmentMap(departments, coverage);
    }

    // Q4
    private static void q4ManagersWithTeamSizeAndAvgRating(List<Employee> employees) {
        System.out.println("\nQ4) Manager analytics: direct team size + team average rating (only active reports).\n");

        Map<Integer, List<Employee>> reportsByManager = employees.stream()
                // Step 1 (mental model: hierarchy edge list): group employees by managerId.
                .filter(e -> e.managerId != null)
                .collect(Collectors.groupingBy(e -> e.managerId));

        Map<String, String> managerInsights = employees.stream()
                // Step 2 (mental model: candidate managers): keep people who actually manage someone.
                .filter(mgr -> reportsByManager.containsKey(mgr.id))
                .collect(Collectors.toMap(mgr -> mgr.name, mgr -> {
                    List<Employee> activeReports = reportsByManager.get(mgr.id).stream()
                            // Step 3 (mental model: quality of current org): inactive reports excluded.
                            .filter(r -> r.active)
                            .collect(Collectors.toList());

                    double avg = activeReports.stream()
                            // Step 4 (mental model: collapse team -> signal): average report rating.
                            .mapToDouble(Employee::avgRating)
                            .average().orElse(0);

                    // Step 5 (mental model: executive-readable output): compact text summary.
                    return "teamSize=" + activeReports.size() + ", avgTeamRating=" + String.format("%.2f", avg);
                }));

        managerInsights.forEach((k, v) -> System.out.println("- " + k + " -> " + v));
    }

    // Q5
    private static void q5ProjectToCrossDepartmentContributors(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ5) Projects with cross-department contributors (project -> distinct departments).\n");

        Map<Integer, String> deptNameById = departments.stream().collect(Collectors.toMap(d -> d.id, d -> d.name));

        Map<String, Set<String>> projectToDepartments = employees.stream()
                // Step 1 (mental model: explode one-to-many): one employee has many projects.
                .flatMap(e -> e.projects.stream().map(project -> Map.entry(project, deptNameById.get(e.departmentId))))
                // Step 2 (mental model: aggregate membership): project key, set of departments as value.
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));

        Map<String, Set<String>> crossDeptOnly = projectToDepartments.entrySet().stream()
                // Step 3 (mental model: apply business condition): keep projects touching 2+ departments.
                .filter(e -> e.getValue().size() >= 2)
                // Step 4 (mental model: preserve deterministic output order): sort by project name.
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        crossDeptOnly.forEach((k, v) -> System.out.println("- " + k + " -> " + v));
    }

    // Q6
    private static void q6DepartmentRiskIndexFromLowRatings(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ6) Risk index by department from low ratings (<3): lowRatingEvents/totalRatingEvents.\n");

        Map<Integer, Double> riskByDept = employees.stream()
                // Step 1 (mental model: group first): risk must be measured per department.
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            long totalEvents = list.stream().mapToLong(e -> e.quarterlyRatings.size()).sum();
                            long lowEvents = list.stream()
                                    // Step 2 (mental model: flatten nested metric): ratings list -> rating events stream.
                                    .flatMap(e -> e.quarterlyRatings.stream())
                                    // Step 3 (mental model: threshold): identify weak performance signals.
                                    .filter(r -> r < 3)
                                    .count();
                            // Step 4 (mental model: normalized KPI): avoid absolute count bias by ratio.
                            return totalEvents == 0 ? 0 : ((double) lowEvents / totalEvents);
                        })));

        printDepartmentMap(departments, riskByDept);
    }

    // Q7
    private static void q7SkillAdjacencyMapForSeniorEngineers(List<Employee> employees) {
        System.out.println("\nQ7) Skill adjacency map from senior employees (which skills co-occur with each skill).\n");

        Map<String, Set<String>> adjacency = employees.stream()
                // Step 1 (mental model: target persona): build knowledge graph from senior people only.
                .filter(e -> e.experienceYears >= 8)
                // Step 2 (mental model: emit all skill-pairs per employee).
                .flatMap(e -> e.skills.stream().flatMap(skillA ->
                        e.skills.stream()
                                .filter(skillB -> !skillB.equals(skillA))
                                .map(skillB -> Map.entry(skillA, skillB))))
                // Step 3 (mental model: graph construction): key skill -> connected skills set.
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));

        adjacency.forEach((k, v) -> System.out.println("- " + k + " -> " + v));
    }

    // Q8
    private static void q8DepartmentMedianSalary(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ8) Median salary per department (custom collector strategy).\n");

        Map<Integer, Double> medianByDept = employees.stream()
                // Step 1 (mental model: partition by owner): median calculated per department.
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(e -> e.salary, Collectors.toList()),
                                salaries -> {
                                    // Step 2 (mental model: order first): median needs sorted sequence.
                                    List<Double> sorted = salaries.stream().sorted().collect(Collectors.toList());
                                    int n = sorted.size();
                                    if (n == 0) return 0.0;
                                    // Step 3 (mental model: odd/even branch): center or average-of-centers.
                                    return (n % 2 == 1)
                                            ? sorted.get(n / 2)
                                            : (sorted.get((n / 2) - 1) + sorted.get(n / 2)) / 2.0;
                                })));

        printDepartmentMap(departments, medianByDept);
    }

    // Q9
    private static void q9FindInactiveManagersWithActiveReports(List<Employee> employees) {
        System.out.println("\nQ9) Detect inactive managers still owning active reports (governance anomaly).\n");

        Map<Integer, Employee> byId = employees.stream().collect(Collectors.toMap(e -> e.id, Function.identity()));

        List<String> anomalies = employees.stream()
                // Step 1 (mental model: employee -> manager edge): keep only employees that have manager.
                .filter(e -> e.managerId != null)
                // Step 2 (mental model: join in-memory): map each report to manager object.
                .map(e -> Map.entry(e, byId.get(e.managerId)))
                // Step 3 (mental model: anomaly predicate): report active, manager inactive.
                .filter(pair -> pair.getKey().active && pair.getValue() != null && !pair.getValue().active)
                // Step 4 (mental model: output readability): produce clear issue statements.
                .map(pair -> "report=" + pair.getKey().name + ", inactiveManager=" + pair.getValue().name)
                .sorted()
                .collect(Collectors.toList());

        anomalies.forEach(s -> System.out.println("- " + s));
    }

    // Q10
    private static void q10PromotionShortlistByCompositeRule(List<Department> departments, List<Employee> employees) {
        System.out.println("\nQ10) Promotion shortlist: active, avgRating>=4, experience>=6, salary below dept median.\n");

        Map<Integer, Double> medianByDept = employees.stream()
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.collectingAndThen(Collectors.mapping(e -> e.salary, Collectors.toList()), salaries -> {
                            List<Double> sorted = salaries.stream().sorted().collect(Collectors.toList());
                            int n = sorted.size();
                            return n % 2 == 1 ? sorted.get(n / 2)
                                    : (sorted.get((n / 2) - 1) + sorted.get(n / 2)) / 2.0;
                        })));

        Map<Integer, List<String>> shortlist = employees.stream()
                // Step 1 (mental model: hard constraints first): active and tenure thresholds.
                .filter(e -> e.active && e.experienceYears >= 6)
                // Step 2 (mental model: quality gate): only consistently strong performers.
                .filter(e -> e.avgRating() >= 4.0)
                // Step 3 (mental model: compa-ratio signal): under-median salary suggests growth headroom.
                .filter(e -> e.salary < medianByDept.getOrDefault(e.departmentId, Double.MAX_VALUE))
                // Step 4 (mental model: presentation by department): leadership reviews by org unit.
                .collect(Collectors.groupingBy(e -> e.departmentId,
                        Collectors.mapping(e -> e.name + "(rating=" + String.format("%.2f", e.avgRating())
                                + ", salary=" + e.salary + ")", Collectors.toList())));

        printDepartmentMap(departments, shortlist);
    }

    private static void printDepartmentMap(List<Department> departments, Map<Integer, ?> map) {
        Map<Integer, String> names = departments.stream().collect(Collectors.toMap(d -> d.id, d -> d.name));
        map.forEach((deptId, value) -> System.out.println("- " + names.getOrDefault(deptId, "Unknown") + " -> " + value));
    }

    private static List<Department> departments() {
        return Arrays.asList(
                new Department(10, "Engineering", "US", 2_000_000),
                new Department(20, "Sales", "US", 1_500_000),
                new Department(30, "HR", "US", 700_000),
                new Department(40, "Finance", "US", 900_000)
        );
    }

    private static List<Employee> employees() {
        return Arrays.asList(
                new Employee(1, "Ava", 10, 145000, 11,
                        Arrays.asList("Java", "Distributed Systems", "Kubernetes"),
                        Arrays.asList(5, 4, 5, 4),
                        Arrays.asList("Atlas", "Neptune"), null, true),
                new Employee(2, "Noah", 10, 120000, 8,
                        Arrays.asList("Java", "Spring", "Observability"),
                        Arrays.asList(4, 4, 4, 5),
                        Arrays.asList("Atlas"), 1, true),
                new Employee(3, "Mia", 10, 98000, 5,
                        Arrays.asList("QA", "Automation", "Java"),
                        Arrays.asList(3, 4, 3, 4),
                        Arrays.asList("Neptune"), 1, true),
                new Employee(4, "Liam", 20, 160000, 13,
                        Arrays.asList("Enterprise Negotiation", "CRM", "Forecasting"),
                        Arrays.asList(5, 5, 4, 5),
                        Arrays.asList("Mercury", "Atlas"), null, false),
                new Employee(5, "Emma", 20, 102000, 7,
                        Arrays.asList("CRM", "Pipeline Management"),
                        Arrays.asList(4, 4, 4, 4),
                        Arrays.asList("Mercury"), 4, true),
                new Employee(6, "Sophia", 30, 92000, 9,
                        Arrays.asList("Hiring", "Workforce Analytics", "Org Design"),
                        Arrays.asList(4, 5, 4, 4),
                        Arrays.asList("Pulse", "Neptune"), null, true),
                new Employee(7, "James", 30, 76000, 4,
                        Arrays.asList("Hiring", "Interview Ops"),
                        Arrays.asList(2, 3, 2, 3),
                        Arrays.asList("Pulse"), 6, true),
                new Employee(8, "Olivia", 40, 135000, 10,
                        Arrays.asList("Risk", "Compliance", "FP&A"),
                        Arrays.asList(5, 4, 5, 5),
                        Arrays.asList("Ledger", "Mercury"), null, true),
                new Employee(9, "Ethan", 40, 99000, 6,
                        Arrays.asList("Compliance", "Reporting"),
                        Arrays.asList(3, 3, 4, 3),
                        Arrays.asList("Ledger"), 8, true),
                new Employee(10, "Isabella", 20, 89000, 6,
                        Arrays.asList("Forecasting", "CRM", "Storytelling"),
                        Arrays.asList(4, 5, 4, 4),
                        Arrays.asList("Mercury", "Pulse"), 4, true)
        );
    }
}
