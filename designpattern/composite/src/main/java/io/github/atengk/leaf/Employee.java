package io.github.atengk.leaf;


import io.github.atengk.component.OrganizationComponent;

/**
 * 员工（叶子节点）
 */
public class Employee extends OrganizationComponent {

    public Employee(String name) {
        super(name);
    }

    @Override
    public void show(int level) {
        String prefix = " ".repeat(level * 2);
        System.out.println(prefix + "👤 员工：" + name);
    }
}
