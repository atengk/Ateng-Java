package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * CollectionUtil 第 15 类功能测试：树形结构处理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil15TreeTest {

    @Test
    void buildTree() {
        List<Menu> result = CollectionUtil.buildTree(menus(), 0L, Menu::getId, Menu::getParentId, Menu::setChildren);
        System.out.println(result);
    }

    @Test
    void flattenTree() {
        List<Menu> result = CollectionUtil.flattenTree(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void flattenTreePostOrder() {
        List<Menu> result = CollectionUtil.flattenTreePostOrder(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void flattenTreeBreadthFirst() {
        List<Menu> result = CollectionUtil.flattenTreeBreadthFirst(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void findTreeNode() {
        Menu result = CollectionUtil.findTreeNode(tree(), Menu::getChildren, menu -> menu.getName().contains("后端"));
        System.out.println(result);
    }

    @Test
    void findTreeNodes() {
        List<Menu> result = CollectionUtil.findTreeNodes(tree(), Menu::getChildren, menu -> menu.getId() > 2L);
        System.out.println(result);
    }

    @Test
    void anyTreeNodeMatch() {
        boolean result = CollectionUtil.anyTreeNodeMatch(tree(), Menu::getChildren, menu -> menu.getId() == 5L);
        System.out.println(result);
    }

    @Test
    void countTreeNodes() {
        int result = CollectionUtil.countTreeNodes(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void maxTreeDepth() {
        int result = CollectionUtil.maxTreeDepth(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void leafNodes() {
        List<Menu> result = CollectionUtil.leafNodes(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void nonLeafNodes() {
        List<Menu> result = CollectionUtil.nonLeafNodes(tree(), Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void findTreePath() {
        List<Menu> result = CollectionUtil.findTreePath(tree(), Menu::getChildren, menu -> menu.getId() == 5L);
        System.out.println(result);
    }

    @Test
    void descendants() {
        Menu root = tree().get(0);
        List<Menu> result = CollectionUtil.descendants(root, Menu::getChildren);
        System.out.println(result);
    }

    @Test
    void descendantIds() {
        Menu root = tree().get(0);
        List<Long> result = CollectionUtil.descendantIds(root, Menu::getChildren, Menu::getId);
        System.out.println(result);
    }

    @Test
    void ancestors() {
        List<Menu> menus = menus();
        Menu node = menus.get(4);
        List<Menu> result = CollectionUtil.ancestors(menus, node, Menu::getId, Menu::getParentId);
        System.out.println(result);
    }

    @Test
    void childNodes() {
        List<Menu> menus = menus();
        Menu root = menus.get(0);
        List<Menu> result = CollectionUtil.childNodes(menus, root, Menu::getId, Menu::getParentId);
        System.out.println(result);
    }

    @Test
    void descendantsFromList() {
        List<Menu> menus = menus();
        Menu root = menus.get(0);
        List<Menu> result = CollectionUtil.descendantsFromList(menus, root, Menu::getId, Menu::getParentId);
        System.out.println(result);
    }

    private List<Menu> tree() {
        return CollectionUtil.buildTree(menus(), 0L, Menu::getId, Menu::getParentId, Menu::setChildren);
    }

    private List<Menu> menus() {
        return Arrays.asList(
                new Menu(1L, 0L, "系统管理"),
                new Menu(2L, 1L, "用户管理"),
                new Menu(3L, 1L, "角色管理"),
                new Menu(4L, 0L, "研发管理"),
                new Menu(5L, 4L, "后端任务"),
                new Menu(6L, 4L, "前端任务")
        );
    }

    static class Menu {
        private final Long id;
        private final Long parentId;
        private final String name;
        private List<Menu> children = new ArrayList<>();

        Menu(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        Long getId() {
            return id;
        }

        Long getParentId() {
            return parentId;
        }

        String getName() {
            return name;
        }

        List<Menu> getChildren() {
            return children;
        }

        void setChildren(List<Menu> children) {
            this.children = children;
        }

        @Override
        public String toString() {
            return "Menu{" + "id=" + id + ", parentId=" + parentId + ", name='" + name + '\'' + ", children=" + children + '}';
        }
    }
}
