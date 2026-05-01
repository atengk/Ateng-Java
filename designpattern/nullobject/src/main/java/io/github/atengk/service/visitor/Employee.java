package io.github.atengk.service.visitor;

/**
 * 员工抽象类（元素接口）
 */
public interface Employee {

    /**
     * 接受访问者操作
     *
     * @param visitor 访问者
     */
    void accept(EmployeeVisitor visitor);

    /**
     * 获取姓名
     */
    String getName();

    /**
     * 获取工资
     */
    double getSalary();
}
