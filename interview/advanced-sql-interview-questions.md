# Advanced SQL Interview Questions with Sample Data and Solutions

This set includes:
- A sample schema
- Sample data
- 10 advanced SQL interview problems
- Reference SQL solutions

> Notes:
> - SQL syntax is written in a PostgreSQL-friendly style.
> - Equivalent alternatives may be needed for MySQL/SQL Server/Oracle.

## 1) Sample Schema

```sql
CREATE TABLE Department (
  dept_id      INT PRIMARY KEY,
  dept_name    VARCHAR(100) NOT NULL,
  location     VARCHAR(100)
);

CREATE TABLE Employee (
  emp_id       INT PRIMARY KEY,
  emp_name     VARCHAR(100) NOT NULL,
  dept_id      INT NOT NULL REFERENCES Department(dept_id),
  manager_id   INT NULL REFERENCES Employee(emp_id),
  hire_date    DATE NOT NULL,
  salary       DECIMAL(12,2) NOT NULL
);

CREATE TABLE Project (
  project_id   INT PRIMARY KEY,
  project_name VARCHAR(120) NOT NULL,
  dept_id      INT NOT NULL REFERENCES Department(dept_id),
  start_date   DATE NOT NULL,
  end_date     DATE NULL
);

CREATE TABLE Employee_Project (
  emp_id         INT NOT NULL REFERENCES Employee(emp_id),
  project_id     INT NOT NULL REFERENCES Project(project_id),
  allocation_pct DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (emp_id, project_id)
);
```

## 2) Sample Data

```sql
INSERT INTO Department (dept_id, dept_name, location) VALUES
(10, 'Engineering', 'New York'),
(20, 'Finance', 'Chicago'),
(30, 'HR', 'Boston'),
(40, 'Marketing', 'San Francisco');

INSERT INTO Employee (emp_id, emp_name, dept_id, manager_id, hire_date, salary) VALUES
(101, 'Alice', 10, NULL, '2018-01-10', 180000),
(102, 'Bob',   10, 101,  '2019-03-15', 140000),
(103, 'Carol', 10, 101,  '2020-06-20', 140000),
(104, 'David', 10, 102,  '2021-08-01', 110000),
(105, 'Eve',   20, NULL, '2017-11-05', 175000),
(106, 'Frank', 20, 105,  '2020-02-01', 120000),
(107, 'Grace', 20, 105,  '2022-04-17',  95000),
(108, 'Heidi', 30, NULL, '2019-07-30', 130000),
(109, 'Ivan',  30, 108,  '2021-01-12',  90000),
(110, 'Judy',  10, 102,  '2022-09-22', 115000),
(111, 'Mallory', 20, 105, '2023-03-10', 120000),
(112, 'Niaj',  40, NULL, '2021-12-01', 125000);

INSERT INTO Project (project_id, project_name, dept_id, start_date, end_date) VALUES
(1001, 'Payroll Modernization', 20, '2023-01-01', NULL),
(1002, 'Hiring Platform',       30, '2023-06-01', NULL),
(1003, 'Ad Analytics',          40, '2022-09-15', '2025-12-31'),
(1004, 'Recommendation Engine', 10, '2024-01-10', NULL),
(1005, 'Cost Optimizer',        20, '2024-03-01', NULL);

INSERT INTO Employee_Project (emp_id, project_id, allocation_pct) VALUES
(101, 1004, 40),
(102, 1004, 60),
(103, 1002, 20), -- Engineering employee on HR project
(104, 1001, 30), -- Engineering employee on Finance project
(105, 1001, 40),
(106, 1005, 70),
(107, 1005, 30),
(108, 1002, 80),
(109, 1002, 50),
(110, 1003, 20), -- Engineering employee on Marketing project
(111, 1001, 60),
(112, 1003, 90);
```

---

## 3) Interview Questions + Solutions

### Q1) Find the second highest distinct salary in each department

**Question:** Return each department and its second highest **distinct** salary.

```sql
SELECT dept_id, dept_name, salary AS second_highest_salary
FROM (
  SELECT
    d.dept_id,
    d.dept_name,
    e.salary,
    DENSE_RANK() OVER (PARTITION BY d.dept_id ORDER BY e.salary DESC) AS sal_rank
  FROM Department d
  JOIN Employee e ON e.dept_id = d.dept_id
) x
WHERE sal_rank = 2;
```

### Q2) Employees earning above their department average

**Question:** List employees whose salary is above the average salary of their own department.

```sql
SELECT *
FROM (
  SELECT
    e.emp_id,
    e.emp_name,
    e.dept_id,
    e.salary,
    AVG(e.salary) OVER (PARTITION BY e.dept_id) AS dept_avg_salary
  FROM Employee e
) t
WHERE salary > dept_avg_salary;
```

### Q3) Department salary contribution percentage of total payroll

**Question:** For each department, calculate its payroll share as a percentage of company total salary.

```sql
SELECT
  d.dept_id,
  d.dept_name,
  SUM(e.salary) AS dept_payroll,
  ROUND(
    100.0 * SUM(e.salary) / SUM(SUM(e.salary)) OVER (),
    2
  ) AS payroll_pct
FROM Department d
JOIN Employee e ON e.dept_id = d.dept_id
GROUP BY d.dept_id, d.dept_name
ORDER BY payroll_pct DESC;
```

### Q4) Employees whose salary is higher than their manager

**Question:** Return employees earning more than their direct manager.

```sql
SELECT
  e.emp_id,
  e.emp_name,
  e.salary AS emp_salary,
  m.emp_id AS manager_id,
  m.emp_name AS manager_name,
  m.salary AS manager_salary
FROM Employee e
JOIN Employee m ON e.manager_id = m.emp_id
WHERE e.salary > m.salary;
```

### Q5) Running total of salaries by department ordered by hire date

**Question:** For each employee, show running cumulative salary in their department by `hire_date`.

```sql
SELECT
  e.dept_id,
  e.emp_id,
  e.emp_name,
  e.hire_date,
  e.salary,
  SUM(e.salary) OVER (
    PARTITION BY e.dept_id
    ORDER BY e.hire_date, e.emp_id
    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
  ) AS running_salary_total
FROM Employee e
ORDER BY e.dept_id, e.hire_date, e.emp_id;
```

### Q6) Top 3 highest paid employees per department (including ties)

**Question:** Return top-3 salaries per department including ties.

```sql
SELECT dept_id, emp_id, emp_name, salary, salary_rank
FROM (
  SELECT
    e.dept_id,
    e.emp_id,
    e.emp_name,
    e.salary,
    DENSE_RANK() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC) AS salary_rank
  FROM Employee e
) r
WHERE salary_rank <= 3
ORDER BY dept_id, salary_rank, emp_id;
```

### Q7) Departments with no employees but at least one active project

**Question:** Find departments that have zero employees but one or more active projects.

```sql
SELECT d.dept_id, d.dept_name
FROM Department d
WHERE NOT EXISTS (
  SELECT 1 FROM Employee e WHERE e.dept_id = d.dept_id
)
AND EXISTS (
  SELECT 1
  FROM Project p
  WHERE p.dept_id = d.dept_id
    AND (p.end_date IS NULL OR p.end_date >= CURRENT_DATE)
);
```

### Q8) Employees working on projects outside their home department

**Question:** Employees have a home department in `Employee`; projects belong to a department in `Project`.
Return employees assigned to projects outside their home department.

```sql
SELECT DISTINCT
  e.emp_id,
  e.emp_name,
  e.dept_id       AS home_dept_id,
  p.project_id,
  p.project_name,
  p.dept_id       AS project_dept_id
FROM Employee e
JOIN Employee_Project ep ON ep.emp_id = e.emp_id
JOIN Project p ON p.project_id = ep.project_id
WHERE e.dept_id <> p.dept_id
ORDER BY e.emp_id, p.project_id;
```

### Q9) Detect cycles in the employee-manager hierarchy (recursive SQL)

**Question:** Identify whether any reporting chain loops back to the starting employee.

```sql
WITH RECURSIVE hierarchy AS (
  SELECT
    e.emp_id AS start_emp,
    e.emp_id,
    e.manager_id,
    CAST(e.emp_id AS VARCHAR(500)) AS path,
    false AS has_cycle
  FROM Employee e

  UNION ALL

  SELECT
    h.start_emp,
    m.emp_id,
    m.manager_id,
    h.path || '->' || m.emp_id,
    POSITION('->' || m.emp_id || '->' IN '->' || h.path || '->') > 0 AS has_cycle
  FROM hierarchy h
  JOIN Employee m ON h.manager_id = m.emp_id
  WHERE h.has_cycle = false
)
SELECT DISTINCT start_emp, path
FROM hierarchy
WHERE has_cycle = true;
```

### Q10) Median salary by department

**Question:** Compute median salary in each department.

```sql
SELECT
  dept_id,
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary) AS median_salary
FROM Employee
GROUP BY dept_id
ORDER BY dept_id;
```

> Alternative (portable) approach: use row numbers and counts to average middle value(s).

### Q11) Stored procedure + function for bonus calculation (same domain)

**Question:**  
Using the same `Department` and `Employee` domain:
1. Create a **function** that returns a department's average salary.  
2. Create a **stored procedure** that gives a bonus to employees in a department whose salary is below that department average.  
3. Execute the procedure for a department and verify updated salaries.

```sql
-- 1) Function: return average salary for a department
CREATE OR REPLACE FUNCTION fn_dept_avg_salary(p_dept_id INT)
RETURNS DECIMAL(12,2)
LANGUAGE SQL
AS $$
  SELECT COALESCE(AVG(e.salary), 0)::DECIMAL(12,2)
  FROM Employee e
  WHERE e.dept_id = p_dept_id;
$$;

-- 2) Stored procedure: apply bonus (%) to employees below department average
CREATE OR REPLACE PROCEDURE sp_apply_bonus_below_avg(
  p_dept_id INT,
  p_bonus_pct DECIMAL(5,2)
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_avg_salary DECIMAL(12,2);
BEGIN
  v_avg_salary := fn_dept_avg_salary(p_dept_id);

  UPDATE Employee
  SET salary = ROUND(salary * (1 + p_bonus_pct / 100.0), 2)
  WHERE dept_id = p_dept_id
    AND salary < v_avg_salary;
END;
$$;

-- 3) Example execution + verification
CALL sp_apply_bonus_below_avg(10, 10.00); -- 10% bonus for dept 10 employees below avg

SELECT emp_id, emp_name, dept_id, salary
FROM Employee
WHERE dept_id = 10
ORDER BY salary DESC, emp_id;
```

> Interview extension: Ask the candidate how they would add an audit table and transaction-safe rollback behavior when the update affects more rows than expected.
