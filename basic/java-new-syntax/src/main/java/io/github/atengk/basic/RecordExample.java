package io.github.atengk.basic;

import java.util.List;

/**
 * record 示例（JDK16+，JDK14 预览）
 */
public class RecordExample {

    /**
     * 1. 定义 record（自动生成构造器、getter、toString、equals、hashCode）
     */
    public record User(String name, int age) {

        // =========================
        // 2. 自定义构造（校验逻辑）
        // =========================
        public User {
            if (age < 0) {
                throw new IllegalArgumentException("年龄不能为负");
            }
        }

        // =========================
        // 3. 自定义方法
        // =========================
        public String display() {
            return name + "(" + age + ")";
        }
    }

    /**
     * 4. 嵌套 record（DTO 场景）
     */
    public record Order(String orderId, double amount) {}

    /**
     * 核心方法：record 使用
     */
    public static void recordUsage() {

        // =========================
        // 5. 创建对象（无需 new getter/setter）
        // =========================
        User user = new User("张三", 20);

        System.out.println("User：" + user);

        // =========================
        // 6. 获取字段（类似 getter，但无 get 前缀）
        // =========================
        System.out.println("姓名：" + user.name());
        System.out.println("年龄：" + user.age());

        // =========================
        // 7. equals / hashCode 自动生成
        // =========================
        User u1 = new User("张三", 20);
        User u2 = new User("张三", 20);

        System.out.println("是否相等：" + u1.equals(u2));


        // =========================
        // 8. 不可变（字段 final）
        // =========================
        // user.name = "李四"; ❌ 编译错误

        // =========================
        // 9. 集合使用（DTO 列表）
        // =========================
        List<User> users = List.of(
                new User("张三", 20),
                new User("李四", 25)
        );

        users.forEach(System.out::println);


        // =========================
        // 10. 结合 Stream 使用（项目常见🔥）
        // =========================
        List<String> names = users.stream()
                .map(User::name)
                .toList();

        System.out.println("姓名列表：" + names);


        // =========================
        // 11. record 作为返回值（推荐）
        // =========================
        User result = buildUser();

        System.out.println("返回对象：" + result);


        // =========================
        // 12. 多 record 组合（接口返回）
        // =========================
        Order order = new Order("ORD001", 99.9);

        System.out.println("订单：" + order);
    }

    /**
     * 模拟接口返回 DTO
     */
    public static User buildUser() {
        return new User("王五", 30);
    }

    public static void main(String[] args) {
        recordUsage();
    }
}
