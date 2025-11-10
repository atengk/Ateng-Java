package io.github.atengk.component;


/**
 * 组织结构抽象组件
 */
public abstract class OrganizationComponent {

    /**
     * 组件名称
     */
    protected String name;

    public OrganizationComponent(String name) {
        this.name = name;
    }

    /**
     * 添加子节点
     */
    public void add(OrganizationComponent component) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 移除子节点
     */
    public void remove(OrganizationComponent component) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 显示组织结构（核心抽象方法）
     */
    public abstract void show(int level);
}
