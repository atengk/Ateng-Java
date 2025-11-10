package io.github.atengk.service.visitor;

/**
 * 普通员工
 */
public class Staff implements Employee {

    private final String name;
    private final double salary;

    public Staff(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    @Override
    public void accept(EmployeeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getSalary() {
        return salary;
    }
}