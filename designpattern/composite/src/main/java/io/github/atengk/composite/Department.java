package io.github.atengk.composite;

import io.github.atengk.component.OrganizationComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门（组合节点）
 */
public class Department extends OrganizationComponent {

    private final List<OrganizationComponent> children = new ArrayList<>();

    public Department(String name) {
        super(name);
    }

    @Override
    public void add(OrganizationComponent component) {
        children.add(component);
    }

    @Override
    public void remove(OrganizationComponent component) {
        children.remove(component);
    }

    @Override
    public void show(int level) {
        String prefix = " ".repeat(level * 2);
        System.out.println(prefix + "🏢 部门：" + name);
        for (OrganizationComponent child : children) {
            child.show(level + 1);
        }
    }
}
