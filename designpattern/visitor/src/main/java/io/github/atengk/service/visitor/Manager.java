package io.github.atengk.service.visitor;

/**
 * 经理
 */
public class Manager implements Employee {

    private final String name;
    private final double salary;

    public Manager(String name, double salary) {
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
