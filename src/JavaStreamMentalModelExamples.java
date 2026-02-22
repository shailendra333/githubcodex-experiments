import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mental model for Java Streams:
 *
 * 1) Start from the question: "What final shape do I need?"
 *    - single number? list? map? grouped report?
 *
 * 2) Translate the question into a pipeline:
 *    SOURCE -> FILTER -> TRANSFORM -> ORDER/LIMIT -> COLLECT/REDUCE
 *
 * 3) Keep each step pure and small:
 *    - filter: keep only needed records
 *    - map: convert shape
 *    - flatMap: explode nested collections
 *    - distinct/sorted/limit: refine the result set
 *    - collect/reduce: produce final answer
 *
 * 4) Read the pipeline left-to-right like a sentence.
 *
 * 5) Prefer method extraction for readability when logic grows.
 */
public class JavaStreamMentalModelExamples {

    // A small domain model used by many examples.
    static class Employee {
        String name;
        String department;
        int age;
        double salary;
        List<String> skills;

        Employee(String name, String department, int age, double salary, List<String> skills) {
            this.name = name;
            this.department = department;
            this.age = age;
            this.salary = salary;
            this.skills = skills;
        }

        @Override
        public String toString() {
            return name + "(" + department + ", age=" + age + ", salary=" + salary + ")";
        }
    }

    public static void main(String[] args) {
        List<Employee> employees = sampleEmployees();
        List<String> words = Arrays.asList("stream", "java", "code", "flow", "stream", "api");
        List<List<Integer>> nestedNumbers = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(4, 5, 6)
        );

        System.out.println("=== JAVA STREAM MENTAL MODEL EXAMPLES ===\n");

        example1FilterMapCollect(employees);
        example2CountCondition(employees);
        example3SumWithMapToDouble(employees);
        example4Grouping(employees);
        example5NestedGrouping(employees);
        example6TopNBySorting(employees);
        example7DistinctSortedWords(words);
        example8FlattenNestedList(nestedNumbers);
        example9FirstMatchWithOptional(employees);
        example10Partitioning(employees);
        example11FrequencyMap(words);
        example12CustomReduction(employees);
    }

    /**
     * EXAMPLE 1: Get names of employees from Engineering department.
     * Question -> "I need a List<String> names".
     * Pipeline -> source employees, filter by dept, map to name, collect list.
     */
    private static void example1FilterMapCollect(List<Employee> employees) {
        List<String> engineeringNames = employees.stream()
                .filter(e -> "Engineering".equals(e.department))
                .map(e -> e.name)
                .collect(Collectors.toList());

        print("1) Engineering employee names", engineeringNames);
    }

    /**
     * EXAMPLE 2: Count employees older than 30.
     * Final shape is a long count.
     */
    private static void example2CountCondition(List<Employee> employees) {
        long count = employees.stream()
                .filter(e -> e.age > 30)
                .count();

        print("2) Count of employees age > 30", count);
    }

    /**
     * EXAMPLE 3: Total salary of all employees.
     * Use mapToDouble when you need numeric aggregation.
     */
    private static void example3SumWithMapToDouble(List<Employee> employees) {
        double totalSalary = employees.stream()
                .mapToDouble(e -> e.salary)
                .sum();

        print("3) Total salary", totalSalary);
    }

    /**
     * EXAMPLE 4: Group employees by department.
     * Final shape is Map<Department, List<Employee>>.
     */
    private static void example4Grouping(List<Employee> employees) {
        Map<String, List<Employee>> byDept = employees.stream()
                .collect(Collectors.groupingBy(e -> e.department));

        print("4) Employees grouped by department", byDept);
    }

    /**
     * EXAMPLE 5: Average salary by department.
     * Final shape is Map<Department, AverageSalary>.
     */
    private static void example5NestedGrouping(List<Employee> employees) {
        Map<String, Double> avgSalaryByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.department,
                        Collectors.averagingDouble(e -> e.salary)
                ));

        print("5) Average salary by department", avgSalaryByDept);
    }

    /**
     * EXAMPLE 6: Top 3 highest-paid employees.
     * Pipeline -> sort descending by salary, limit(3), collect list.
     */
    private static void example6TopNBySorting(List<Employee> employees) {
        List<Employee> top3Paid = employees.stream()
                .sorted(Comparator.comparingDouble((Employee e) -> e.salary).reversed())
                .limit(3)
                .collect(Collectors.toList());

        print("6) Top 3 highest-paid employees", top3Paid);
    }

    /**
     * EXAMPLE 7: Distinct words sorted by length then alphabetically.
     * Useful for cleaning and ordering textual data.
     */
    private static void example7DistinctSortedWords(List<String> words) {
        List<String> cleaned = words.stream()
                .distinct()
                .sorted(Comparator.comparingInt(String::length).thenComparing(Function.identity()))
                .collect(Collectors.toList());

        print("7) Distinct words sorted by length then alpha", cleaned);
    }

    /**
     * EXAMPLE 8: Flatten nested lists and keep only even numbers.
     * flatMap changes Stream<List<Integer>> to Stream<Integer>.
     */
    private static void example8FlattenNestedList(List<List<Integer>> nestedNumbers) {
        List<Integer> evens = nestedNumbers.stream()
                .flatMap(List::stream)
                .filter(n -> n % 2 == 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        print("8) Flattened unique even numbers", evens);
    }

    /**
     * EXAMPLE 9: Find first employee in Sales with salary > 70000.
     * Final shape is Optional<Employee> since match may not exist.
     */
    private static void example9FirstMatchWithOptional(List<Employee> employees) {
        Optional<Employee> first = employees.stream()
                .filter(e -> "Sales".equals(e.department))
                .filter(e -> e.salary > 70000)
                .findFirst();

        print("9) First Sales employee with salary > 70000", first.orElse(null));
    }

    /**
     * EXAMPLE 10: Partition employees by salary threshold.
     * partitioningBy gives Map<Boolean, List<Employee>> (true/false buckets).
     */
    private static void example10Partitioning(List<Employee> employees) {
        Map<Boolean, List<Employee>> partitioned = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.salary >= 80000));

        print("10) Partition employees by salary >= 80000", partitioned);
    }

    /**
     * EXAMPLE 11: Build word frequency map.
     * Final shape is Map<Word, Count>.
     */
    private static void example11FrequencyMap(List<String> words) {
        Map<String, Long> freq = words.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        print("11) Word frequency map", freq);
    }

    /**
     * EXAMPLE 12: Custom reduction - concatenate senior employee names.
     * Demonstrates reduce for custom accumulation.
     */
    private static void example12CustomReduction(List<Employee> employees) {
        String seniorNames = employees.stream()
                .filter(e -> e.age >= 35)
                .map(e -> e.name)
                .sorted()
                .reduce("", (acc, name) -> acc.isEmpty() ? name : acc + ", " + name);

        print("12) Senior employee names (age >= 35)", seniorNames);
    }

    private static List<Employee> sampleEmployees() {
        return Arrays.asList(
                new Employee("Aarav", "Engineering", 28, 75000, Arrays.asList("Java", "Spring")),
                new Employee("Isha", "Engineering", 35, 98000, Arrays.asList("Java", "AWS")),
                new Employee("Kabir", "Sales", 32, 72000, Arrays.asList("Negotiation", "CRM")),
                new Employee("Meera", "HR", 30, 65000, Arrays.asList("Hiring", "Communication")),
                new Employee("Rohan", "Sales", 40, 88000, Arrays.asList("Leadership", "CRM")),
                new Employee("Sara", "Engineering", 42, 120000, Arrays.asList("Architecture", "Kubernetes"))
        );
    }

    private static void print(String title, Object value) {
        System.out.println(title + " -> " + value);
    }
}
