package local.ateng.java.customutils.entity;

import java.util.ArrayList;
import java.util.List;

public class Menu {
    private Integer id;
    private Integer parentId;
    private String name;
    private List<Menu> children;

    public Menu(Integer id, Integer parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.children = new ArrayList<>();
    }

    // Getter & Setter 省略，可用 lombok 替代
    public Integer getId() {
        return id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public List<Menu> getChildren() {
        return children;
    }

    public void setChildren(List<Menu> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "Menu{id=" + id + ", name='" + name + "', children=" + children + "}";
    }
}
