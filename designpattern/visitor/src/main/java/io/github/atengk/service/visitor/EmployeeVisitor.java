package io.github.atengk.service.visitor;

/**
 * 访问者接口
 */
public interface EmployeeVisitor {

    void visit(Manager manager);

    void visit(Staff staff);
}
