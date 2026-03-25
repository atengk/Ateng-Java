package io.github.atengk.basic;

import java.util.*;

/**
 * 集合工厂方法示例（JDK9+）
 */
public class CollectionFactoryExample {

    /**
     * 核心方法：集合工厂方法使用
     */
    public static void factoryUsage() {

        // =========================
        // 1. List.of（创建不可变 List）
        // =========================
        List<String> list = List.of("A", "B", "C");

        System.out.println("List.of：" + list);

        // ❗ 不可变集合，修改会抛异常
        try {
            list.add("D");
        } catch (Exception e) {
            System.out.println("List 不可修改：" + e);
        }


        // =========================
        // 2. Set.of（创建不可变 Set）
        // =========================
        Set<String> set = Set.of("A", "B", "C");

        System.out.println("Set.of：" + set);

        // ❗ 不允许重复元素
        try {
            Set.of("A", "A");
        } catch (Exception e) {
            System.out.println("Set 不允许重复：" + e);
        }


        // =========================
        // 3. Map.of（创建不可变 Map）
        // =========================
        Map<String, Integer> map = Map.of(
                "A", 1,
                "B", 2,
                "C", 3
        );

        System.out.println("Map.of：" + map);

        // ❗ 不可修改
        try {
            map.put("D", 4);
        } catch (Exception e) {
            System.out.println("Map 不可修改：" + e);
        }


        // =========================
        // 4. Map.ofEntries（超过10个元素推荐）
        // =========================
        Map<String, Integer> bigMap = Map.ofEntries(
                Map.entry("A", 1),
                Map.entry("B", 2),
                Map.entry("C", 3),
                Map.entry("D", 4),
                Map.entry("E", 5)
        );

        System.out.println("Map.ofEntries：" + bigMap);


        // =========================
        // 5. List.copyOf（创建不可变副本）
        // =========================
        List<String> source = new ArrayList<>();
        source.add("X");
        source.add("Y");

        List<String> copyList = List.copyOf(source);

        System.out.println("copyOf：" + copyList);

        // 修改原集合不会影响 copy（结构独立）
        source.add("Z");

        System.out.println("原集合：" + source);
        System.out.println("副本：" + copyList);


        // =========================
        // 6. Set.copyOf（去重 + 不可变）
        // =========================
        List<String> listWithDup = Arrays.asList("A", "A", "B");

        Set<String> copySet = Set.copyOf(listWithDup);

        System.out.println("Set.copyOf（自动去重）：" + copySet);


        // =========================
        // 7. Map.copyOf（不可变 Map 副本）
        // =========================
        Map<String, Integer> mutableMap = new HashMap<>();
        mutableMap.put("K1", 100);
        mutableMap.put("K2", 200);

        Map<String, Integer> copyMap = Map.copyOf(mutableMap);

        System.out.println("Map.copyOf：" + copyMap);

        mutableMap.put("K3", 300);

        System.out.println("原Map：" + mutableMap);
        System.out.println("副本Map：" + copyMap);


        // =========================
        // 8. 空集合（推荐写法）
        // =========================
        List<String> emptyList = List.of();
        Set<String> emptySet = Set.of();
        Map<String, String> emptyMap = Map.of();

        System.out.println("空集合：" + emptyList + " | " + emptySet + " | " + emptyMap);


        // =========================
        // 9. 注意：null 不允许
        // =========================
        try {
            List.of("A", null);
        } catch (Exception e) {
            System.out.println("不允许 null：" + e);
        }


        // =========================
        // 10. 项目推荐用法（初始化常量数据）
        // =========================
        Map<String, String> statusMap = Map.of(
                "0", "禁用",
                "1", "正常"
        );

        System.out.println("状态映射：" + statusMap);
    }

    public static void main(String[] args) {
        factoryUsage();
    }
}
