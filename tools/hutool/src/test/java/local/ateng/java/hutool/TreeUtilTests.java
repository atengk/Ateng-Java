package local.ateng.java.hutool;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TreeUtilTests {

    @Test
    public void testTreeUtil() {
        // 测试数据 - 可以支持任意层级
        List<MenuItem> menuList = Arrays.asList(
                new MenuItem(1, "系统管理", "用户管理", "新增用户", null),
                new MenuItem(2, "系统管理", "用户管理", "删除用户", null),
                new MenuItem(3, "系统管理", "角色管理", null, null),
                new MenuItem(4, "内容管理", "文章管理", "新增文章", null),
                new MenuItem(5, "内容管理", "文章管理", null, null),
                new MenuItem(6, "内容管理", "文章管理", "发布文章", null),
                new MenuItem(7, "内容管理", "文章管理", "删除文章", null),
                new MenuItem(8, "内容管理", "评论管理", null, null),
                // 测试4级菜单
                new MenuItem(9, "订单管理", "订单处理", "售后管理", "退款处理"),
                new MenuItem(10, "订单管理", "订单处理", "售后管理", "退货处理")
        );

        List<TreeNode<Integer>> nodeList = buildTreeNodes(menuList);
        List<Tree<Integer>> treeList = TreeUtil.build(nodeList, 0);
        System.out.println(JSONUtil.toJsonPrettyStr(treeList));
    }

    /**
     * 通用方法：将菜单项列表转换为树节点列表
     *
     * @param menuItems 菜单项列表
     * @return 树节点列表
     */
    private List<TreeNode<Integer>> buildTreeNodes(List<MenuItem> menuItems) {
        List<TreeNode<Integer>> nodeList = new ArrayList<>();
        Map<String, Integer> pathToId = new HashMap<>();
        AtomicInteger virtualId = new AtomicInteger(10000); // 虚拟ID起始

        for (MenuItem item : menuItems) {
            // 获取所有非空的层级名称
            List<String> levels = item.getNonNullLevels();
            if (levels.isEmpty()) {
                continue;
            }

            // 使用数组来绕过 final 限制
            final Integer[] parentIdHolder = {0}; // 根节点的父ID为0
            String currentPath = "";

            // 处理非叶子节点
            for (int i = 0; i < levels.size() - 1; i++) {
                String levelName = levels.get(i);
                currentPath += (currentPath.isEmpty() ? "" : "/") + levelName;

                // 如果路径不存在，则创建虚拟节点
                parentIdHolder[0] = pathToId.computeIfAbsent(currentPath, k -> {
                    int vid = virtualId.getAndIncrement();
                    nodeList.add(new TreeNode<>(vid, parentIdHolder[0], levelName, null));
                    return vid;
                });
            }

            // 处理叶子节点（最后一级）
            String leafName = levels.get(levels.size() - 1);
            nodeList.add(new TreeNode<>(item.getId(), parentIdHolder[0], leafName, null));
        }

        return nodeList;
    }

    @Data
    @AllArgsConstructor
    class MenuItem {
        private Integer id;
        private String level1;
        private String level2;
        private String level3;
        private String level4; // 添加更多层级

        /**
         * 获取所有非空的层级名称
         *
         * @return 非空层级列表
         */
        public List<String> getNonNullLevels() {
            return Arrays.stream(new String[]{level1, level2, level3, level4})
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}



