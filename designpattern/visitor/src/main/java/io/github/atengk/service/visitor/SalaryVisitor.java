package io.github.atengk.service.visitor;

import org.springframework.stereotype.Component;

/**
 * 薪资统计访问者
 */
@Component
public class SalaryVisitor implements EmployeeVisitor {

    private double totalSalary = 0;

    @Override
    public void visit(Manager manager) {
        totalSalary += manager.getSalary();
        System.out.println("【统计经理】" + manager.getName() + " 薪资：" + manager.getSalary());
    }

    @Override
    public void visit(Staff staff) {
        totalSalary += staff.getSalary();
        System.out.println("【统计员工】" + staff.getName() + " 薪资：" + staff.getSalary());
    }

    public double getTotalSalary() {
        return totalSalary;
    }
}
