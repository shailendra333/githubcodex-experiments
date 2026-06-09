import java.util.Arrays;
import java.util.List;

public class Test {

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
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
