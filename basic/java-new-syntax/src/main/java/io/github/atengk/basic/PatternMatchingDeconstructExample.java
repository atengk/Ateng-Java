package io.github.atengk.basic;

import java.util.List;

/**
 * Pattern Matching + 解构示例（JDK21🔥）
 */
public class PatternMatchingDeconstructExample {

    /**
     * record（天然支持解构🔥）
     */
    public record User(String name, int age) {}

    public record Order(String id, double amount) {}

    /**
     * sealed 类型（推荐组合使用）
     */
    sealed interface Result permits Success, Error {}

    public record Success(String data) implements Result {}

    public record Error(String msg) implements Result {}

    /**
     * 核心方法：模式匹配 + 解构
     */
    public static void patternUsage() {

        // =========================
        // 1. record 解构（核心🔥）
        // =========================
        Object obj = new User("张三", 20);

        if (obj instanceof User(String name, int age)) {
            System.out.println("解构 User：" + name + "," + age);
        }


        // =========================
        // 2. switch 解构（推荐🔥）
        // =========================
        Object input = new Order("ORD001", 99.9);

        String result = switch (input) {
            case Order(String id, double amount) ->
                    "订单：" + id + " 金额：" + amount;
            case User(String name, int age) ->
                    "用户：" + name + " 年龄：" + age;
            default -> "未知";
        };

        System.out.println(result);


        // =========================
        // 3. 条件匹配（when🔥）
        // =========================
        Object obj2 = new User("李四", 17);

        String msg = switch (obj2) {
            case User(String name, int age) when age >= 18 ->
                    "成年用户：" + name;
            case User(String name, int age) ->
                    "未成年：" + name;
            default -> "未知";
        };

        System.out.println(msg);


        // =========================
        // 4. 嵌套解构（复杂对象🔥）
        // =========================
        record Wrapper(User user) {}

        Object obj3 = new Wrapper(new User("王五", 30));

        if (obj3 instanceof Wrapper(User(String name, int age))) {
            System.out.println("嵌套解构：" + name + "," + age);
        }


        // =========================
        // 5. 结合 sealed（最佳实践🔥）
        // =========================
        Result r = new Success("OK");

        String res = switch (r) {
            case Success(String data) -> "成功：" + data;
            case Error(String err) -> "失败：" + err;
        };

        System.out.println(res);


        // =========================
        // 6. List 遍历 + 解构
        // =========================
        List<Object> list = List.of(
                new User("A", 10),
                new Order("B", 20.0)
        );

        for (Object o : list) {
            String s = switch (o) {
                case User(String name, int age) ->
                        "User:" + name;
                case Order(String id, double amount) ->
                        "Order:" + id;
                default -> "Other";
            };
            System.out.println(s);
        }


        // =========================
        // 7. 替代 getter（核心价值🔥）
        // =========================
        User user = new User("赵六", 40);

        if (user instanceof User(String name, int age)) {
            System.out.println("无需 getter：" + name + "," + age);
        }


        // =========================
        // 8. 项目实战：统一返回处理🔥
        // =========================
        System.out.println(handle(new Success("数据")));
        System.out.println(handle(new Error("异常")));
    }

    /**
     * 项目实战：统一处理返回值
     */
    public static String handle(Result result) {
        return switch (result) {
            case Success(String data) -> "成功：" + data;
            case Error(String msg) -> "失败：" + msg;
        };
    }

    public static void main(String[] args) {
        patternUsage();
    }
}
