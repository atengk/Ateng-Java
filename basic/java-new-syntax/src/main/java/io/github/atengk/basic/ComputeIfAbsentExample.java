package io.github.atengk.basic;

import java.util.*;

/**
 * Map.computeIfAbsent 示例（JDK8+）
 */
public class ComputeIfAbsentExample {

    /**
     * 核心方法：computeIfAbsent 使用
     */
    public static void computeUsage() {

        // =========================
        // 1. 基础用法（不存在才创建🔥）
        // =========================
        Map<String, List<String>> map = new HashMap<>();

        map.computeIfAbsent("A", k -> new ArrayList<>()).add("1");
        map.computeIfAbsent("A", k -> new ArrayList<>()).add("2");

        System.out.println("基础用法：" + map);


        // =========================
        // 2. 替代传统写法（对比）
        // =========================
        Map<String, List<String>> oldMap = new HashMap<>();

        if (!oldMap.containsKey("A")) {
            oldMap.put("A", new ArrayList<>());
        }
        oldMap.get("A").add("1");

        System.out.println("传统写法：" + oldMap);


        // =========================
        // 3. 分组场景（项目常用🔥）
        // =========================
        List<String> list = List.of("A-1", "A-2", "B-1");

        Map<String, List<String>> group = new HashMap<>();

        for (String item : list) {
            String key = item.split("-")[0];

            group.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        System.out.println("分组：" + group);


        // =========================
        // 4. 嵌套 Map（多级分组🔥）
        // =========================
        Map<String, Map<String, List<String>>> multiMap = new HashMap<>();

        for (String item : list) {
            String[] arr = item.split("-");
            String k1 = arr[0];
            String k2 = arr[1];

            multiMap
                    .computeIfAbsent(k1, k -> new HashMap<>())
                    .computeIfAbsent(k2, k -> new ArrayList<>())
                    .add(item);
        }

        System.out.println("多级分组：" + multiMap);


        // =========================
        // 5. 计数统计
        // =========================
        Map<String, Integer> countMap = new HashMap<>();

        for (String item : list) {
            String key = item.split("-")[0];

            countMap.compute(key, (k, v) -> v == null ? 1 : v + 1);
        }

        System.out.println("计数：" + countMap);


        // =========================
        // 6. 缓存场景（懒加载🔥）
        // =========================
        Map<String, String> cache = new HashMap<>();

        String value = cache.computeIfAbsent("key", k -> {
            System.out.println("执行查询DB...");
            return "数据";
        });

        System.out.println("缓存结果：" + value);

        // 第二次不会执行
        cache.computeIfAbsent("key", k -> {
            System.out.println("不会执行");
            return "新数据";
        });


        // =========================
        // 7. 避免 null（注意⚠️）
        // =========================
        Map<String, String> nullMap = new HashMap<>();

        nullMap.computeIfAbsent("A", k -> null); // 不会存入

        System.out.println("null值：" + nullMap);


        // =========================
        // 8. 结合 Stream 使用
        // =========================
        Map<String, List<String>> streamGroup = new HashMap<>();

        list.forEach(item -> {
            String key = item.split("-")[0];
            streamGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        });

        System.out.println("Stream分组：" + streamGroup);


        // =========================
        // 9. 项目实战：构建索引🔥
        // =========================
        List<User> users = List.of(
                new User("张三", "IT"),
                new User("李四", "IT"),
                new User("王五", "HR")
        );

        Map<String, List<User>> deptMap = new HashMap<>();

        users.forEach(u ->
                deptMap.computeIfAbsent(u.dept(), k -> new ArrayList<>()).add(u)
        );

        System.out.println("部门索引：" + deptMap);


        // =========================
        // 10. 推荐总结
        // =========================
        System.out.println("\n推荐场景：");
        System.out.println("✔ 分组");
        System.out.println("✔ 缓存");
        System.out.println("✔ 初始化集合");
    }

    /**
     * 示例 record
     */
    public record User(String name, String dept) {}

    public static void main(String[] args) {
        computeUsage();
    }
}
