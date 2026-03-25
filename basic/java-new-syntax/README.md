# Java 新语法

基于 **JDK8 → JDK21** 在实际项目中**高频且核心**的一些语法/功能点列表

------

## 一、集合 & Stream（核心必备）

### Stream 高级用法（分组、收集、聚合）

```java
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stream 高级用法示例（分组、收集、聚合）
 */
public class StreamAdvancedExample {

    /**
     * 员工实体类（模拟项目中的业务对象）
     */
    static class Employee {
        private String name;
        private String dept;
        private int salary;

        public Employee(String name, String dept, int salary) {
            this.name = name;
            this.dept = dept;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public String getDept() {
            return dept;
        }

        public int getSalary() {
            return salary;
        }

        @Override
        public String toString() {
            return name + "(" + salary + ")";
        }
    }

    /**
     * 核心方法：演示分组、收集、聚合操作
     */
    public static void streamAdvancedUsage(List<Employee> list) {

        // =========================
        // 1. 分组（groupingBy）
        // =========================
        // 按部门分组
        Map<String, List<Employee>> groupByDept =
                list.stream()
                        .collect(Collectors.groupingBy(Employee::getDept));

        System.out.println("按部门分组：");
        groupByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 2. 分组 + 聚合（统计人数）
        // =========================
        Map<String, Long> countByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.counting()
                        ));

        System.out.println("\n各部门人数：");
        countByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 3. 分组 + 求平均值
        // =========================
        Map<String, Double> avgSalaryByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.averagingInt(Employee::getSalary)
                        ));

        System.out.println("\n各部门平均薪资：");
        avgSalaryByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 4. 分组 + 最大值
        // =========================
        Map<String, Optional<Employee>> maxSalaryByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.maxBy(Comparator.comparingInt(Employee::getSalary))
                        ));

        System.out.println("\n各部门最高薪资员工：");
        maxSalaryByDept.forEach((k, v) -> System.out.println(k + " -> " + v.orElse(null)));


        // =========================
        // 5. 分组 + 转换（只要姓名）
        // =========================
        Map<String, List<String>> namesByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.mapping(Employee::getName, Collectors.toList())
                        ));

        System.out.println("\n各部门员工姓名：");
        namesByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 6. 汇总（总薪资）
        // =========================
        int totalSalary =
                list.stream()
                        .mapToInt(Employee::getSalary)
                        .sum();

        System.out.println("\n总薪资：" + totalSalary);


        // =========================
        // 7. 多级分组（部门 + 薪资等级）
        // =========================
        Map<String, Map<String, List<Employee>>> multiGroup =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.groupingBy(e -> e.getSalary() > 10000 ? "高薪" : "普通")
                        ));

        System.out.println("\n多级分组：");
        multiGroup.forEach((dept, map) -> {
            System.out.println(dept + " -> " + map);
        });


        // =========================
        // 8. 转 Map（key 冲突处理）
        // =========================
        Map<String, Employee> nameMap =
                list.stream()
                        .collect(Collectors.toMap(
                                Employee::getName,
                                e -> e,
                                (oldVal, newVal) -> newVal // key冲突时取后者
                        ));

        System.out.println("\n转 Map：");
        nameMap.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 9. 分区（partitioningBy）
        // =========================
        Map<Boolean, List<Employee>> partition =
                list.stream()
                        .collect(Collectors.partitioningBy(e -> e.getSalary() > 10000));

        System.out.println("\n高薪/非高薪分区：");
        partition.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 10. 收集为不可变集合（JDK16+）
        // =========================
        List<Employee> immutableList =
                list.stream().toList(); // 不可变集合

        System.out.println("\n不可变集合：" + immutableList);
    }


    public static void main(String[] args) {

        // 构造测试数据
        List<Employee> list = List.of(
                new Employee("张三", "IT", 12000),
                new Employee("李四", "IT", 9000),
                new Employee("王五", "HR", 8000),
                new Employee("赵六", "HR", 15000),
                new Employee("孙七", "Finance", 20000)
        );

        // 调用核心方法
        streamAdvancedUsage(list);
    }
}
```

输出：

```
按部门分组：
Finance -> [孙七(20000)]
HR -> [王五(8000), 赵六(15000)]
IT -> [张三(12000), 李四(9000)]

各部门人数：
Finance -> 1
HR -> 2
IT -> 2

各部门平均薪资：
Finance -> 20000.0
HR -> 11500.0
IT -> 10500.0

各部门最高薪资员工：
Finance -> 孙七(20000)
HR -> 赵六(15000)
IT -> 张三(12000)

各部门员工姓名：
Finance -> [孙七]
HR -> [王五, 赵六]
IT -> [张三, 李四]

总薪资：64000

多级分组：
Finance -> {高薪=[孙七(20000)]}
HR -> {普通=[王五(8000)], 高薪=[赵六(15000)]}
IT -> {普通=[李四(9000)], 高薪=[张三(12000)]}

转 Map：
孙七 -> 孙七(20000)
李四 -> 李四(9000)
张三 -> 张三(12000)
王五 -> 王五(8000)
赵六 -> 赵六(15000)

高薪/非高薪分区：
false -> [李四(9000), 王五(8000)]
true -> [张三(12000), 赵六(15000), 孙七(20000)]

不可变集合：[张三(12000), 李四(9000), 王五(8000), 赵六(15000), 孙七(20000)]

```

---

### Optional 优雅判空（避免 NPE）

```java
import java.util.Optional;

/**
 * Optional 优雅判空示例（避免 NPE）
 */
public class OptionalExample {

    /**
     * 用户实体类（模拟项目中的对象）
     */
    static class User {
        private String name;
        private String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    /**
     * 模拟查询用户（可能返回 null）
     */
    public static User getUser(boolean exists) {
        return exists ? new User("张三", "test@example.com") : null;
    }

    /**
     * 核心方法：Optional 常用用法
     */
    public static void optionalUsage() {

        // =========================
        // 1. ofNullable（安全创建 Optional）
        // =========================
        Optional<User> userOpt = Optional.ofNullable(getUser(true));

        // =========================
        // 2. isPresent / ifPresent（避免 null 判断）
        // =========================
        userOpt.ifPresent(user -> System.out.println("用户存在：" + user.getName()));

        // =========================
        // 3. orElse（提供默认值，立即执行）
        // =========================
        User user1 = Optional.ofNullable(getUser(false))
                .orElse(new User("默认用户", "default@test.com"));

        System.out.println("orElse：" + user1.getName());

        // =========================
        // 4. orElseGet（推荐，懒加载）
        // =========================
        User user2 = Optional.ofNullable(getUser(false))
                .orElseGet(() -> {
                    System.out.println("执行默认逻辑");
                    return new User("懒加载用户", "lazy@test.com");
                });

        System.out.println("orElseGet：" + user2.getName());

        // =========================
        // 5. orElseThrow（为空直接抛异常）
        // =========================
        try {
            User user3 = Optional.ofNullable(getUser(false))
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
        } catch (Exception e) {
            System.out.println("异常：" + e.getMessage());
        }

        // =========================
        // 6. map（链式调用，避免多层判空）
        // =========================
        String email = Optional.ofNullable(getUser(true))
                .map(User::getEmail)
                .map(String::toUpperCase)
                .orElse("无邮箱");

        System.out.println("邮箱：" + email);

        // =========================
        // 7. flatMap（避免 Optional 嵌套）
        // =========================
        Optional<String> emailOpt =
                Optional.ofNullable(getUser(true))
                        .flatMap(user -> Optional.ofNullable(user.getEmail()));

        System.out.println("flatMap：" + emailOpt.orElse("无"));

        // =========================
        // 8. filter（条件过滤）
        // =========================
        Optional<User> filtered =
                Optional.ofNullable(getUser(true))
                        .filter(user -> user.getName().length() > 2);

        System.out.println("filter：" + filtered.map(User::getName).orElse("不满足条件"));

        // =========================
        // 9. 链式防空（项目推荐写法）
        // =========================
        String result =
                Optional.ofNullable(getUser(true))
                        .map(User::getEmail)
                        .filter(e -> e.contains("@"))
                        .orElse("非法邮箱");

        System.out.println("链式结果：" + result);

        // =========================
        // 10. ifPresentOrElse（JDK9+）
        // =========================
        Optional.ofNullable(getUser(false))
                .ifPresentOrElse(
                        user -> System.out.println("存在：" + user.getName()),
                        () -> System.out.println("用户为空")
                );
    }

    public static void main(String[] args) {
        optionalUsage();
    }
}
```

输出

```
用户存在：张三
orElse：默认用户
执行默认逻辑
orElseGet：懒加载用户
异常：用户不存在
邮箱：TEST@EXAMPLE.COM
flatMap：test@example.com
filter：不满足条件
链式结果：test@example.com
用户为空
```



---

### 集合工厂方法（List.of / Map.of）

```java
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
```

输出

```
List.of：[A, B, C]
List 不可修改：java.lang.UnsupportedOperationException
Set.of：[C, A, B]
Set 不允许重复：java.lang.IllegalArgumentException: duplicate element: A
Map.of：{C=3, A=1, B=2}
Map 不可修改：java.lang.UnsupportedOperationException
Map.ofEntries：{A=1, B=2, C=3, D=4, E=5}
copyOf：[X, Y]
原集合：[X, Y, Z]
副本：[X, Y]
Set.copyOf（自动去重）：[B, A]
Map.copyOf：{K1=100, K2=200}
原Map：{K1=100, K2=200, K3=300}
副本Map：{K1=100, K2=200}
空集合：[] | [] | {}
不允许 null：java.lang.NullPointerException
状态映射：{1=正常, 0=禁用}
```

---

### Stream 并行流（parallelStream）

```java
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stream 并行流示例（parallelStream）
 */
public class ParallelStreamExample {

    /**
     * 模拟耗时操作（如：远程调用 / DB 查询）
     */
    public static int slowTask(int num) {
        try {
            Thread.sleep(100); // 模拟耗时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return num * 2;
    }

    /**
     * 核心方法：对比串行流 vs 并行流
     */
    public static void parallelStreamUsage() {

        // 构造测试数据
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().toList();

        // =========================
        // 1. 串行流（默认）
        // =========================
        long start1 = System.currentTimeMillis();

        List<Integer> result1 = list.stream()
                .map(ParallelStreamExample::slowTask)
                .collect(Collectors.toList());

        long end1 = System.currentTimeMillis();

        System.out.println("串行流结果：" + result1);
        System.out.println("串行耗时：" + (end1 - start1) + " ms");


        // =========================
        // 2. 并行流（parallelStream）
        // =========================
        long start2 = System.currentTimeMillis();

        List<Integer> result2 = list.parallelStream()
                .map(ParallelStreamExample::slowTask)
                .collect(Collectors.toList());

        long end2 = System.currentTimeMillis();

        System.out.println("\n并行流结果：" + result2);
        System.out.println("并行耗时：" + (end2 - start2) + " ms");


        // =========================
        // 3. 并行流线程演示
        // =========================
        System.out.println("\n线程执行情况：");

        list.parallelStream().forEach(i -> {
            System.out.println("值：" + i + " -> 线程：" + Thread.currentThread().getName());
        });


        // =========================
        // 4. 注意：并行流顺序问题
        // =========================
        System.out.println("\nforEach（无序）：");
        list.parallelStream().forEach(System.out::print);

        System.out.println("\nforEachOrdered（有序）：");
        list.parallelStream().forEachOrdered(System.out::print);


        // =========================
        // 5. reduce 聚合（并行安全）
        // =========================
        int sum = list.parallelStream()
                .reduce(0, Integer::sum);

        System.out.println("\n\n并行求和：" + sum);


        // =========================
        // 6. 错误示例：共享变量（线程不安全）
        // =========================
        List<Integer> unsafeList = new ArrayList<>();

        try {
            list.parallelStream().forEach(i -> unsafeList.add(i)); // 非线程安全
        } catch (Exception e) {
            System.out.println("\n并发异常：" + e);
        }

        System.out.println("unsafeList size：" + unsafeList.size());


        // =========================
        // 7. 正确写法：使用收集器（线程安全）
        // =========================
        List<Integer> safeList = list.parallelStream()
                .collect(Collectors.toList());

        System.out.println("safeList size：" + safeList.size());


        // =========================
        // 8. 自定义线程池（不推荐直接用 parallelStream）
        // =========================
        // ❗ parallelStream 默认使用 ForkJoinPool.commonPool
        // ❗ 项目中建议使用 CompletableFuture + 自定义线程池替代
        System.out.println("\n默认线程池：" + ForkJoinPool.commonPool());
    }

    public static void main(String[] args) {
        parallelStreamUsage();
    }
}
```

输出

```
串行流结果：[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
串行耗时：1103 ms

并行流结果：[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
并行耗时：223 ms

线程执行情况：
值：2 -> 线程：ForkJoinPool.commonPool-worker-4
值：3 -> 线程：ForkJoinPool.commonPool-worker-2
值：7 -> 线程：main
值：1 -> 线程：ForkJoinPool.commonPool-worker-5
值：6 -> 线程：ForkJoinPool.commonPool-worker-3
值：9 -> 线程：ForkJoinPool.commonPool-worker-7
值：5 -> 线程：ForkJoinPool.commonPool-worker-1
值：8 -> 线程：ForkJoinPool.commonPool-worker-6
值：10 -> 线程：ForkJoinPool.commonPool-worker-4
值：4 -> 线程：ForkJoinPool.commonPool-worker-2

forEach（无序）：
73169852410
forEachOrdered（有序）：
12345678910

并行求和：55
unsafeList size：10
safeList size：10

默认线程池：java.util.concurrent.ForkJoinPool@5197848c[Running, parallelism = 7, size = 7, active = 0, running = 0, steals = 51, tasks = 0, submissions = 0]

```



------

## 二、函数式编程（JDK8+）

### Lambda 表达式 + 方法引用

```java
import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lambda 表达式 + 方法引用示例
 */
public class LambdaExample {

    /**
     * 核心方法：Lambda + 方法引用常用写法
     */
    public static void lambdaUsage() {

        // =========================
        // 1. Lambda 基础写法
        // =========================
        List<String> list = List.of("apple", "banana", "orange");

        list.forEach(s -> System.out.println("Lambda：" + s));


        // =========================
        // 2. 方法引用（替代 Lambda）
        // =========================
        list.forEach(System.out::println);


        // =========================
        // 3. 排序（Comparator）
        // =========================
        List<String> sorted = list.stream()
                .sorted((a, b) -> a.compareTo(b)) // Lambda
                .toList();

        System.out.println("排序：" + sorted);

        // 方法引用写法（更简洁）
        List<String> sorted2 = list.stream()
                .sorted(String::compareTo)
                .toList();

        System.out.println("排序（方法引用）：" + sorted2);


        // =========================
        // 4. Function（函数式接口）
        // =========================
        Function<String, Integer> lengthFunc = s -> s.length();

        int len = lengthFunc.apply("hello");
        System.out.println("长度：" + len);

        // 方法引用
        Function<String, Integer> lengthFunc2 = String::length;
        System.out.println("长度（方法引用）：" + lengthFunc2.apply("world"));


        // =========================
        // 5. Predicate（条件判断）
        // =========================
        Predicate<String> isLong = s -> s.length() > 5;

        list.stream()
                .filter(isLong)
                .forEach(System.out::println);


        // =========================
        // 6. Consumer（消费型接口）
        // =========================
        Consumer<String> printer = s -> System.out.println("消费：" + s);

        list.forEach(printer);


        // =========================
        // 7. 构造方法引用
        // =========================
        List<User> users = list.stream()
                .map(User::new) // 等价于 s -> new User(s)
                .toList();

        System.out.println("构造方法引用：" + users);


        // =========================
        // 8. 静态方法引用
        // =========================
        List<Integer> numbers = List.of(-1, 2, -3, 4);

        List<Integer> absList = numbers.stream()
                .map(Math::abs)
                .toList();

        System.out.println("绝对值：" + absList);


        // =========================
        // 9. 实例方法引用（特定对象）
        // =========================
        String prefix = "前缀-";

        List<String> newList = list.stream()
                .map(prefix::concat) // 等价于 s -> prefix.concat(s)
                .toList();

        System.out.println("拼接：" + newList);


        // =========================
        // 10. 项目常见写法（链式处理）
        // =========================
        List<String> result = list.stream()
                .filter(s -> s.length() > 5)
                .map(String::toUpperCase)
                .sorted()
                .toList();

        System.out.println("链式处理：" + result);
    }

    /**
     * 示例实体类
     */
    static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "'}";
        }
    }

    public static void main(String[] args) {
        lambdaUsage();
    }
}
```

输出

```
Lambda：apple
Lambda：banana
Lambda：orange
apple
banana
orange
排序：[apple, banana, orange]
排序（方法引用）：[apple, banana, orange]
长度：5
长度（方法引用）：5
banana
orange
消费：apple
消费：banana
消费：orange
构造方法引用：[User{name='apple'}, User{name='banana'}, User{name='orange'}]
绝对值：[1, 2, 3, 4]
拼接：[前缀-apple, 前缀-banana, 前缀-orange]
链式处理：[BANANA, ORANGE]
```



### 函数接口（Function / Consumer / Predicate 实战封装）

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * 函数式接口实战封装（Function / Consumer / Predicate）
 */
public class FunctionalInterfaceExample {

    /**
     * 实体类（模拟项目对象）
     */
    static class User {
        private String name;
        private int age;
        private boolean active;

        public User(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }

        @Override
        public String toString() {
            return name + "(" + age + "," + active + ")";
        }
    }

    // =========================
    // 1. Predicate 封装（通用过滤）
    // =========================
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        // 根据条件过滤集合（通用工具方法）
        return list.stream()
                .filter(predicate)
                .toList();
    }

    // =========================
    // 2. Function 封装（通用转换）
    // =========================
    public static <T, R> List<R> map(List<T> list, Function<T, R> function) {
        // 集合转换（如 Entity -> DTO）
        return list.stream()
                .map(function)
                .toList();
    }

    // =========================
    // 3. Consumer 封装（批量处理）
    // =========================
    public static <T> void forEach(List<T> list, Consumer<T> consumer) {
        // 批量执行操作（如日志、发送消息等）
        list.forEach(consumer);
    }

    // =========================
    // 4. Predicate 组合（复杂条件）
    // =========================
    public static Predicate<User> buildUserFilter() {
        // 年龄 > 18 且 active = true
        return u -> u.getAge() > 18 && u.isActive();
    }

    // =========================
    // 5. Function 链式组合
    // =========================
    public static Function<User, String> buildUserMapper() {
        // name -> upper -> 拼接
        return ((Function<User, String>) User::getName)
                .andThen(String::toUpperCase)
                .andThen(name -> "USER:" + name);
    }

    // =========================
    // 6. 批处理模板（项目常用）
    // =========================
    public static <T, R> List<R> process(
            List<T> list,
            Predicate<T> filter,
            Function<T, R> mapper,
            Consumer<R> after) {

        return list.stream()
                .filter(filter)     // 过滤
                .map(mapper)        // 转换
                .peek(after)        // 后置处理（日志/埋点）
                .toList();
    }

    /**
     * 核心方法：演示函数式接口封装使用
     */
    public static void functionalUsage() {

        // 构造数据
        List<User> users = List.of(
                new User("张三", 20, true),
                new User("李四", 16, true),
                new User("王五", 30, false),
                new User("赵六", 25, true)
        );

        // =========================
        // 1. Predicate 过滤
        // =========================
        List<User> adults = filter(users, u -> u.getAge() >= 18);

        System.out.println("成年人：" + adults);


        // =========================
        // 2. Function 转换
        // =========================
        List<String> names = map(users, User::getName);

        System.out.println("姓名列表：" + names);


        // =========================
        // 3. Consumer 批处理
        // =========================
        forEach(users, u -> System.out.println("处理用户：" + u));


        // =========================
        // 4. Predicate 组合
        // =========================
        List<User> validUsers = filter(users, buildUserFilter());

        System.out.println("有效用户：" + validUsers);


        // =========================
        // 5. Function 链式处理
        // =========================
        List<String> result = map(users, buildUserMapper());

        System.out.println("转换结果：" + result);


        // =========================
        // 6. 组合使用（项目核心写法）
        // =========================
        List<String> processed = process(
                users,
                u -> u.getAge() > 18 && u.isActive(), // 条件
                User::getName,                        // 转换
                name -> System.out.println("日志：" + name) // 后处理
        );

        System.out.println("最终结果：" + processed);


        // =========================
        // 7. 分组 + 函数接口（进阶）
        // =========================
        Map<Boolean, List<User>> group =
                users.stream()
                        .collect(Collectors.partitioningBy(User::isActive));

        System.out.println("分组：" + group);
    }

    public static void main(String[] args) {
        functionalUsage();
    }
}
```

输出

```
成年人：[张三(20,true), 王五(30,false), 赵六(25,true)]
姓名列表：[张三, 李四, 王五, 赵六]
处理用户：张三(20,true)
处理用户：李四(16,true)
处理用户：王五(30,false)
处理用户：赵六(25,true)
有效用户：[张三(20,true), 赵六(25,true)]
转换结果：[USER:张三, USER:李四, USER:王五, USER:赵六]
日志：张三
日志：赵六
最终结果：[张三, 赵六]
分组：{false=[王五(30,false)], true=[张三(20,true), 李四(16,true), 赵六(25,true)]}

```



------

## 三、日期时间（强替代旧 Date）

### LocalDate / LocalDateTime 常用操作

```java
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * LocalDate / LocalDateTime 常用操作（替代 Date）
 */
public class DateTimeExample {

    /**
     * 核心方法：日期时间常用操作
     */
    public static void dateTimeUsage() {

        // =========================
        // 1. 获取当前时间
        // =========================
        LocalDate date = LocalDate.now();                 // 当前日期
        LocalDateTime dateTime = LocalDateTime.now();     // 当前日期时间
        LocalTime time = LocalTime.now();                 // 当前时间

        System.out.println("当前日期：" + date);
        System.out.println("当前时间：" + time);
        System.out.println("当前日期时间：" + dateTime);


        // =========================
        // 2. 指定时间创建
        // =========================
        LocalDate customDate = LocalDate.of(2026, 3, 24);
        LocalDateTime customDateTime = LocalDateTime.of(2026, 3, 24, 12, 30, 0);

        System.out.println("\n指定日期：" + customDate);
        System.out.println("指定时间：" + customDateTime);


        // =========================
        // 3. 时间加减
        // =========================
        LocalDate nextDay = date.plusDays(1);
        LocalDate lastMonth = date.minusMonths(1);

        System.out.println("\n明天：" + nextDay);
        System.out.println("上个月：" + lastMonth);

        LocalDateTime plusHours = dateTime.plusHours(2);
        System.out.println("2小时后：" + plusHours);


        // =========================
        // 4. 时间比较
        // =========================
        boolean isAfter = date.isAfter(LocalDate.of(2025, 1, 1));
        boolean isBefore = date.isBefore(LocalDate.of(2030, 1, 1));

        System.out.println("\n是否在2025之后：" + isAfter);
        System.out.println("是否在2030之前：" + isBefore);


        // =========================
        // 5. 时间差计算（ChronoUnit）
        // =========================
        long days = ChronoUnit.DAYS.between(
                LocalDate.of(2026, 1, 1),
                date
        );

        System.out.println("\n相差天数：" + days);

        long hours = ChronoUnit.HOURS.between(
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now()
        );

        System.out.println("相差小时：" + hours);


        // =========================
        // 6. 时间格式化
        // =========================
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String formatted = dateTime.format(formatter);

        System.out.println("\n格式化：" + formatted);


        // =========================
        // 7. 字符串转时间（解析）
        // =========================
        LocalDateTime parsed = LocalDateTime.parse("2026-03-24 10:20:30", formatter);

        System.out.println("解析：" + parsed);


        // =========================
        // 8. 获取时间字段
        // =========================
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        System.out.println("\n年：" + year + " 月：" + month + " 日：" + day);


        // =========================
        // 9. 转换（LocalDate ↔ LocalDateTime）
        // =========================
        LocalDateTime startOfDay = date.atStartOfDay(); // 当天开始
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        System.out.println("\n当天开始：" + startOfDay);
        System.out.println("当天结束：" + endOfDay);


        // =========================
        // 10. 时区（项目常用）
        // =========================
        ZonedDateTime zoned = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));

        System.out.println("\n东京时间：" + zoned);


        // =========================
        // 11. 时间戳转换
        // =========================
        long timestamp = System.currentTimeMillis();

        LocalDateTime fromTimestamp = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        System.out.println("\n时间戳转时间：" + fromTimestamp);


        // =========================
        // 12. 判断闰年
        // =========================
        boolean leapYear = date.isLeapYear();

        System.out.println("\n是否闰年：" + leapYear);
    }

    public static void main(String[] args) {
        dateTimeUsage();
    }
}
```

输出

```
当前日期：2026-03-24
当前时间：21:35:10.183601100
当前日期时间：2026-03-24T21:35:10.183601100

指定日期：2026-03-24
指定时间：2026-03-24T12:30

明天：2026-03-25
上个月：2026-02-24
2小时后：2026-03-24T23:35:10.183601100

是否在2025之后：true
是否在2030之前：true

相差天数：82
相差小时：5

格式化：2026-03-24 21:35:10
解析：2026-03-24T10:20:30

年：2026 月：3 日：24

当天开始：2026-03-24T00:00
当天结束：2026-03-24T23:59:59

东京时间：2026-03-24T22:35:10.215517600+09:00[Asia/Tokyo]

时间戳转时间：2026-03-24T21:35:10.215

是否闰年：false
```

---

### 时间格式化 & 计算（Duration / Period）

```java
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化 & 时间计算（Duration / Period）
 */
public class DurationPeriodExample {

    /**
     * 核心方法：时间格式化 + Duration + Period 使用
     */
    public static void timeCalcUsage() {

        // =========================
        // 1. 时间格式化（DateTimeFormatter）
        // =========================
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = now.format(formatter);

        System.out.println("格式化时间：" + formatted);


        // =========================
        // 2. Duration（时间差：时分秒）
        // =========================
        LocalDateTime start = LocalDateTime.now().minusHours(2).minusMinutes(30);

        Duration duration = Duration.between(start, now);

        long hours = duration.toHours();       // 小时
        long minutes = duration.toMinutes();   // 分钟
        long seconds = duration.getSeconds();  // 秒

        System.out.println("\nDuration：");
        System.out.println("小时：" + hours);
        System.out.println("分钟：" + minutes);
        System.out.println("秒：" + seconds);


        // =========================
        // 3. Period（时间差：年月日）
        // =========================
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.now();

        Period period = Period.between(startDate, endDate);

        System.out.println("\nPeriod：");
        System.out.println("年：" + period.getYears());
        System.out.println("月：" + period.getMonths());
        System.out.println("日：" + period.getDays());


        // =========================
        // 4. Duration 实战（接口耗时统计）
        // =========================
        Instant begin = Instant.now();

        // 模拟接口耗时
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant end = Instant.now();

        long cost = Duration.between(begin, end).toMillis();

        System.out.println("\n接口耗时：" + cost + " ms");


        // =========================
        // 5. Period 实战（年龄计算）
        // =========================
        LocalDate birthday = LocalDate.of(1995, 5, 20);

        int age = Period.between(birthday, LocalDate.now()).getYears();

        System.out.println("\n年龄：" + age);


        // =========================
        // 6. Duration 转换（单位转换）
        // =========================
        Duration d = Duration.ofSeconds(3600);

        System.out.println("\n转换：");
        System.out.println("小时：" + d.toHours());
        System.out.println("分钟：" + d.toMinutes());
        System.out.println("秒：" + d.getSeconds());


        // =========================
        // 7. 格式化 Duration（自定义）
        // =========================
        Duration custom = Duration.ofSeconds(3661);

        long h = custom.toHours();
        long m = custom.toMinutesPart();  // JDK9+
        long s = custom.toSecondsPart();

        String formattedDuration = String.format("%02d:%02d:%02d", h, m, s);

        System.out.println("\n格式化 Duration：" + formattedDuration);


        // =========================
        // 8. 时间加减（结合 Duration / Period）
        // =========================
        LocalDateTime future = now.plus(Duration.ofHours(5));
        LocalDate past = LocalDate.now().minus(Period.ofDays(10));

        System.out.println("\n5小时后：" + future);
        System.out.println("10天前：" + past);


        // =========================
        // 9. 判断时间区间
        // =========================
        LocalDateTime t1 = LocalDateTime.now().minusHours(1);
        LocalDateTime t2 = LocalDateTime.now().plusHours(1);

        boolean inRange = now.isAfter(t1) && now.isBefore(t2);

        System.out.println("\n是否在区间内：" + inRange);


        // =========================
        // 10. 项目常用：过期判断
        // =========================
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);

        boolean expired = Duration.between(expireTime, LocalDateTime.now()).toMinutes() > 5;

        System.out.println("是否过期：" + expired);
    }

    public static void main(String[] args) {
        timeCalcUsage();
    }
}
```

输出

```
格式化时间：2026-03-24 21:39:56

Duration：
小时：2
分钟：149
秒：8999

Period：
年：6
月：2
日：23

接口耗时：507 ms

年龄：30

转换：
小时：1
分钟：60
秒：3600

格式化 Duration：01:01:01

5小时后：2026-03-25T02:39:56.726650900
10天前：2026-03-14

是否在区间内：true
是否过期：true
```



------

## 四、字符串 & 工具增强（JDK11+）

### String 新方法（isBlank / lines / repeat）

```java
import java.util.List;

/**
 * String 新方法示例（JDK11+）
 */
public class StringNewApiExample {

    /**
     * 核心方法：String 常用新方法
     */
    public static void stringUsage() {

        // =========================
        // 1. isBlank（推荐替代 isEmpty）
        // =========================
        String str1 = "   ";
        String str2 = "";

        System.out.println("isBlank：" + str1.isBlank()); // true（空白字符也算）
        System.out.println("isEmpty：" + str2.isEmpty()); // true（仅判断长度）


        // =========================
        // 2. strip / trim（增强去空格）
        // =========================
        String str3 = "  hello  ";

        System.out.println("\ntrim：" + str3.trim());     // 老方法
        System.out.println("strip：" + str3.strip());   // 支持 Unicode 空白

        System.out.println("stripLeading：" + str3.stripLeading());
        System.out.println("stripTrailing：" + str3.stripTrailing());


        // =========================
        // 3. lines（按行拆分）
        // =========================
        String text = "Java\nPython\nGo";

        List<String> lines = text.lines().toList();

        System.out.println("\nlines：" + lines);


        // =========================
        // 4. repeat（重复字符串）
        // =========================
        String repeated = "-".repeat(10);

        System.out.println("\nrepeat：" + repeated);


        // =========================
        // 5. indent（缩进）
        // =========================
        String multiLine = "line1\nline2";

        String indented = multiLine.indent(4);

        System.out.println("\nindent：\n" + indented);


        // =========================
        // 6. transform（链式处理）
        // =========================
        String result = " hello "
                .transform(s -> s.strip().toUpperCase());

        System.out.println("transform：" + result);


        // =========================
        // 7. formatted（格式化字符串）
        // =========================
        String formatted = "姓名：%s，年龄：%d".formatted("张三", 20);

        System.out.println("\nformatted：" + formatted);


        // =========================
        // 8. 实战：过滤空字符串（推荐写法）
        // =========================
        List<String> list = List.of("A", " ", "", "B");

        List<String> filtered = list.stream()
                .filter(s -> !s.isBlank())
                .toList();

        System.out.println("\n过滤空字符串：" + filtered);


        // =========================
        // 9. 实战：多行文本处理
        // =========================
        String config = "key1=value1\nkey2=value2\nkey3=value3";

        config.lines()
                .map(line -> line.split("="))
                .forEach(arr -> System.out.println(arr[0] + " -> " + arr[1]));


        // =========================
        // 10. 实战：生成分隔线
        // =========================
        String line = "=".repeat(20);

        System.out.println("\n分隔线：\n" + line);
    }

    public static void main(String[] args) {
        stringUsage();
    }
}
```

输出：

```
isBlank：true
isEmpty：true

trim：hello
strip：hello
stripLeading：hello  
stripTrailing：  hello

lines：[Java, Python, Go]

repeat：----------

indent：
    line1
    line2

transform：HELLO

formatted：姓名：张三，年龄：20

过滤空字符串：[A, B]
key1 -> value1
key2 -> value2
key3 -> value3

分隔线：
====================
```



### 文本块 Text Blocks（多行字符串）

```java
import java.util.List;

/**
 * Text Blocks 示例（JDK15+）
 */
public class TextBlockExample {

    /**
     * 核心方法：Text Blocks 常用用法
     */
    public static void textBlockUsage() {

        // =========================
        // 1. 基础用法（多行字符串）
        // =========================
        String json = """
                {
                    "name": "张三",
                    "age": 20
                }
                """;

        System.out.println("JSON：\n" + json);


        // =========================
        // 2. SQL（项目常用🔥）
        // =========================
        String sql = """
                SELECT id, name, age
                FROM user
                WHERE age > 18
                ORDER BY age DESC
                """;

        System.out.println("\nSQL：\n" + sql);


        // =========================
        // 3. HTML 模板
        // =========================
        String html = """
                <html>
                    <body>
                        <h1>Hello</h1>
                    </body>
                </html>
                """;

        System.out.println("\nHTML：\n" + html);


        // =========================
        // 4. 自动去缩进（重要特性）
        // =========================
        String text = """
                    line1
                    line2
                """;

        System.out.println("\n自动去缩进：\n" + text);


        // =========================
        // 5. 保留换行（\）
        // =========================
        String noNewLine = """
                line1 \
                line2 \
                line3
                """;

        System.out.println("\n取消换行：" + noNewLine);


        // =========================
        // 6. formatted（动态拼接）
        // =========================
        String name = "张三";
        int age = 20;

        String template = """
                用户信息：
                姓名：%s
                年龄：%d
                """.formatted(name, age);

        System.out.println("\n模板填充：\n" + template);


        // =========================
        // 7. 结合 String API 使用
        // =========================
        String config = """
                A
                B
                C
                """;

        List<String> list = config.lines().toList();

        System.out.println("按行拆分：" + list);


        // =========================
        // 8. 转义字符
        // =========================
        String escape = """
                \"引号\"
                \\反斜杠
                """;

        System.out.println("\n转义：\n" + escape);


        // =========================
        // 9. 实战：日志模板
        // =========================
        String logTemplate = """
                [INFO] 用户登录
                用户：%s
                时间：%s
                """.formatted("admin", java.time.LocalDateTime.now());

        System.out.println("\n日志：\n" + logTemplate);


        // =========================
        // 10. 实战：配置文件
        // =========================
        String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: demo
                """;

        System.out.println("\nYAML：\n" + yaml);
    }

    public static void main(String[] args) {
        textBlockUsage();
    }
}
```

输出

```
JSON：
{
    "name": "张三",
    "age": 20
}


SQL：
SELECT id, name, age
FROM user
WHERE age > 18
ORDER BY age DESC


HTML：
<html>
    <body>
        <h1>Hello</h1>
    </body>
</html>


自动去缩进：
    line1
    line2


取消换行：line1 line2 line3


模板填充：
用户信息：
姓名：张三
年龄：20

按行拆分：[A, B, C]

转义：
"引号"
\反斜杠


日志：
[INFO] 用户登录
用户：admin
时间：2026-03-24T21:46:38.559069300


YAML：
server:
  port: 8080
spring:
  application:
    name: demo

```



------

## 五、IO / 文件操作（JDK11+）

### Files 新 API（读写文件简化）

```java
package io.github.atengk.basic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Files 新 API 示例（JDK11+）
 */
public class FilesExample {

    /**
     * 核心方法：Files 常用操作
     */
    public static void filesUsage() throws IOException {

        // =========================
        // 1. 创建文件路径
        // =========================
        Path path = Path.of("target/test.txt");

        // =========================
        // 2. 写入字符串（最常用🔥）
        // =========================
        Files.writeString(path, "Hello Java21", StandardCharsets.UTF_8);

        System.out.println("写入完成");


        // =========================
        // 3. 读取字符串
        // =========================
        String content = Files.readString(path, StandardCharsets.UTF_8);

        System.out.println("\n读取内容：" + content);


        // =========================
        // 4. 写入多行数据
        // =========================
        List<String> lines = List.of("A", "B", "C");

        Files.write(path, lines, StandardCharsets.UTF_8);

        System.out.println("\n写入多行完成");


        // =========================
        // 5. 读取多行数据
        // =========================
        List<String> readLines = Files.readAllLines(path, StandardCharsets.UTF_8);

        System.out.println("读取多行：" + readLines);


        // =========================
        // 6. 追加写入
        // =========================
        Files.writeString(
                path,
                "\n追加内容",
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
        );

        System.out.println("\n追加完成");


        // =========================
        // 7. 判断文件是否存在
        // =========================
        boolean exists = Files.exists(path);

        System.out.println("\n文件是否存在：" + exists);


        // =========================
        // 8. 创建目录
        // =========================
        Path dir = Path.of("target/tempDir");

        Files.createDirectories(dir);

        System.out.println("目录创建完成");


        // =========================
        // 9. 复制文件
        // =========================
        Path target = Path.of("target/copy.txt");

        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("复制完成");


        // =========================
        // 10. 移动文件
        // =========================
        Path moved = Path.of("target/moved.txt");

        Files.move(target, moved, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("移动完成");


        // =========================
        // 11. 删除文件
        // =========================
        Files.deleteIfExists(moved);

        System.out.println("删除完成");


        // =========================
        // 12. 遍历目录（Stream🔥）
        // =========================
        Files.list(Path.of("target/"))
                .filter(Files::isRegularFile)
                .forEach(p -> System.out.println("文件：" + p));


        // =========================
        // 13. 查找文件（递归）
        // =========================
        Files.walk(Path.of("target/"))
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(p -> System.out.println("txt文件：" + p));


        // =========================
        // 14. 文件大小
        // =========================
        long size = Files.size(path);

        System.out.println("\n文件大小：" + size + " 字节");


        // =========================
        // 15. 临时文件（项目常用）
        // =========================
        Path tempFile = Files.createTempFile("demo", ".txt");

        System.out.println("临时文件：" + tempFile);


        // =========================
        // 16. 权限判断
        // =========================
        System.out.println("可读：" + Files.isReadable(path));
        System.out.println("可写：" + Files.isWritable(path));


        // =========================
        // 17. 读取为流（大文件推荐🔥）
        // =========================
        try (var stream = Files.lines(path)) {
            stream.forEach(System.out::println);
        }


        // =========================
        // 18. 一次性写入（覆盖）
        // =========================
        Files.writeString(path, "覆盖写入");

        System.out.println("\n覆盖写入完成");
    }

    public static void main(String[] args) throws IOException {
        filesUsage();
    }
}
```

输出

```
写入完成

读取内容：Hello Java21

写入多行完成
读取多行：[A, B, C]

追加完成

文件是否存在：true
目录创建完成
复制完成
移动完成
删除完成
文件：target\test.txt
txt文件：target\test.txt

文件大小：22 字节
临时文件：C:\Users\kongyu\AppData\Local\Temp\demo4505755921877871503.txt
可读：true
可写：true
A
B
C

追加内容

覆盖写入完成

```

---

### try-with-resources 增强写法

```java
package io.github.atengk.basic;

import java.io.*;
import java.nio.file.*;

/**
 * try-with-resources 增强写法示例（JDK9+）
 */
public class TryWithResourcesExample {

    /**
     * 模拟资源（实现 AutoCloseable）
     */
    static class MyResource implements AutoCloseable {

        private final String name;

        public MyResource(String name) {
            this.name = name;
        }

        public void doWork() {
            System.out.println(name + " 执行任务");
        }

        @Override
        public void close() {
            System.out.println(name + " 已关闭");
        }
    }

    /**
     * 核心方法：try-with-resources 用法
     */
    public static void tryWithResourcesUsage() throws Exception {

        // =========================
        // 1. 传统写法（JDK7）
        // =========================
        try (BufferedReader br = new BufferedReader(new FileReader("target/test.txt"))) {
            System.out.println("读取：" + br.readLine());
        }


        // =========================
        // 2. JDK9 增强（变量可复用🔥）
        // =========================
        BufferedReader reader = new BufferedReader(new FileReader("target/test.txt"));

        // ❗ 不需要重新声明变量
        try (reader) {
            System.out.println("\nJDK9 读取：" + reader.readLine());
        }


        // =========================
        // 3. 多资源自动关闭
        // =========================
        try (
                BufferedReader br = new BufferedReader(new FileReader("target/test.txt"));
                BufferedWriter bw = new BufferedWriter(new FileWriter("target/out.txt"))
        ) {
            String line = br.readLine();
            bw.write(line);
            System.out.println("\n多资源处理完成");
        }


        // =========================
        // 4. 自定义资源（推荐理解）
        // =========================
        try (MyResource r = new MyResource("资源1")) {
            r.doWork();
        }


        // =========================
        // 5. 异常处理（自动关闭资源）
        // =========================
        try (MyResource r = new MyResource("资源2")) {
            r.doWork();
            throw new RuntimeException("业务异常");
        } catch (Exception e) {
            System.out.println("\n捕获异常：" + e.getMessage());
        }


        // =========================
        // 6. Files + try-with-resources（项目常用🔥）
        // =========================
        Path path = Path.of("target/test.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("Hello");
        }

        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(System.out::println);
        }


        // =========================
        // 7. Stream 资源关闭（重要）
        // =========================
        try (var lines = Files.lines(path)) {
            lines.forEach(System.out::println);
        }


        // =========================
        // 8. 多资源 + 逻辑拆分（推荐写法）
        // =========================
        BufferedReader br = Files.newBufferedReader(path);
        BufferedWriter bw = Files.newBufferedWriter(Path.of("target/copy.txt"));

        try (br; bw) { // JDK9 写法🔥
            br.lines().forEach(line -> {
                try {
                    bw.write(line);
                    bw.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }


        // =========================
        // 9. 注意：资源必须 final / effectively final
        // =========================
        BufferedReader reader2 = Files.newBufferedReader(path);

        // reader2 = null; ❌ 如果修改就不能用于 try-with-resources

        try (reader2) {
            System.out.println("\nreader2 使用成功");
        }


        // =========================
        // 10. 项目最佳实践（模板方法）
        // =========================
        readFile(path, line -> System.out.println("处理：" + line));
    }

    /**
     * 通用读取方法（结合函数式接口）
     */
    public static void readFile(Path path, java.util.function.Consumer<String> consumer) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        tryWithResourcesUsage();
    }
}
```

输出

```
读取：覆盖写入

JDK9 读取：覆盖写入

多资源处理完成
资源1 执行任务
资源1 已关闭
资源2 执行任务
资源2 已关闭

捕获异常：业务异常
Hello
Hello

reader2 使用成功
处理：Hello
```



------

## 六、并发编程（重点）

### CompletableFuture 异步编排（项目核心）

```java
package io.github.atengk.basic;

import java.util.concurrent.*;
import java.util.*;

/**
 * CompletableFuture 异步编排示例（项目核心🔥）
 */
public class CompletableFutureExample {

    /**
     * 自定义线程池（项目必须使用，避免占用公共线程池）
     */
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(4);

    /**
     * 模拟接口调用
     */
    public static String getUser() {
        sleep(100);
        return "张三";
    }

    public static int getOrderCount() {
        sleep(150);
        return 5;
    }

    public static double getBalance() {
        sleep(200);
        return 1000.0;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 核心方法：CompletableFuture 使用
     */
    public static void completableFutureUsage() throws Exception {

        // =========================
        // 1. 异步执行（无返回值）
        // =========================
        CompletableFuture<Void> f1 =
                CompletableFuture.runAsync(() -> {
                    System.out.println("异步任务：" + Thread.currentThread().getName());
                }, EXECUTOR);

        f1.get();


        // =========================
        // 2. 异步执行（有返回值）
        // =========================
        CompletableFuture<String> f2 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        System.out.println("\n用户：" + f2.get());


        // =========================
        // 3. thenApply（链式转换）
        // =========================
        CompletableFuture<String> f3 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenApply(name -> name.toUpperCase());

        System.out.println("转换后：" + f3.get());


        // =========================
        // 4. thenAccept（消费结果）
        // =========================
        CompletableFuture<Void> f4 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenAccept(name -> System.out.println("消费：" + name));

        f4.get();


        // =========================
        // 5. thenRun（不关心结果）
        // =========================
        CompletableFuture<Void> f5 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenRun(() -> System.out.println("执行完成"));

        f5.get();


        // =========================
        // 6. thenCombine（合并两个任务🔥）
        // =========================
        CompletableFuture<String> userFuture =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        CompletableFuture<Integer> orderFuture =
                CompletableFuture.supplyAsync(() -> getOrderCount(), EXECUTOR);

        CompletableFuture<String> combined =
                userFuture.thenCombine(orderFuture,
                        (user, order) -> user + " 订单数：" + order);

        System.out.println("\n合并结果：" + combined.get());


        // =========================
        // 7. allOf（多个任务并行🔥）
        // =========================
        CompletableFuture<String> fUser =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        CompletableFuture<Integer> fOrder =
                CompletableFuture.supplyAsync(() -> getOrderCount(), EXECUTOR);

        CompletableFuture<Double> fBalance =
                CompletableFuture.supplyAsync(() -> getBalance(), EXECUTOR);

        CompletableFuture<Void> all =
                CompletableFuture.allOf(fUser, fOrder, fBalance);

        all.get(); // 等待全部完成

        System.out.println("\n聚合结果："
                + fUser.get() + ", "
                + fOrder.get() + ", "
                + fBalance.get());


        // =========================
        // 8. anyOf（任意一个完成）
        // =========================
        CompletableFuture<Object> any =
                CompletableFuture.anyOf(fUser, fOrder, fBalance);

        System.out.println("\n最快结果：" + any.get());


        // =========================
        // 9. 异常处理（exceptionally）
        // =========================
        CompletableFuture<String> fError =
                CompletableFuture.<String>supplyAsync(() -> {
                            throw new RuntimeException("异常测试");
                        }, EXECUTOR)
                        .exceptionally(ex -> {
                            System.out.println("捕获异常：" + ex.getMessage());
                            return "默认值";
                        });

        System.out.println("异常结果：" + fError.get());


        // =========================
        // 10. handle（统一处理结果+异常）
        // =========================
        CompletableFuture<String> fHandle =
                CompletableFuture.supplyAsync(() -> {
                            if (true) throw new RuntimeException("错误");
                            return "OK";
                        }, EXECUTOR)
                        .handle((res, ex) -> {
                            if (ex != null) {
                                return "兜底";
                            }
                            return res;
                        });

        System.out.println("handle：" + fHandle.get());


        // =========================
        // 11. 超时控制（JDK9+🔥）
        // =========================
        CompletableFuture<String> fTimeout =
                CompletableFuture.supplyAsync(() -> {
                            sleep(3000);
                            return "慢任务";
                        }, EXECUTOR)
                        .orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(ex -> "超时默认");

        System.out.println("超时结果：" + fTimeout.get());


        // =========================
        // 12. 项目实战（聚合接口🔥）
        // =========================
        Map<String, Object> result = buildUserDashboard();

        System.out.println("\n聚合接口返回：" + result);

        EXECUTOR.shutdown();
    }

    /**
     * 项目实战：接口聚合（用户中心）
     */
    public static Map<String, Object> buildUserDashboard() throws Exception {

        CompletableFuture<String> user =
                CompletableFuture.supplyAsync(CompletableFutureExample::getUser, EXECUTOR);

        CompletableFuture<Integer> order =
                CompletableFuture.supplyAsync(CompletableFutureExample::getOrderCount, EXECUTOR);

        CompletableFuture<Double> balance =
                CompletableFuture.supplyAsync(CompletableFutureExample::getBalance, EXECUTOR);

        CompletableFuture.allOf(user, order, balance).join();

        Map<String, Object> map = new HashMap<>();
        map.put("user", user.join());
        map.put("orderCount", order.join());
        map.put("balance", balance.join());

        return map;
    }

    public static void main(String[] args) throws Exception {
        completableFutureUsage();
    }
}
```

输出

```
异步任务：pool-1-thread-1

用户：张三
转换后：张三
消费：张三
执行完成

合并结果：张三 订单数：5

聚合结果：张三, 5, 1000.0

最快结果：张三
捕获异常：java.lang.RuntimeException: 异常测试
异常结果：默认值
handle：兜底
超时结果：超时默认

聚合接口返回：{balance=1000.0, orderCount=5, user=张三}
```



---

### 新线程 API（Thread.ofVirtual → 虚拟线程，JDK21🔥）

```java
package io.github.atengk.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 虚拟线程示例（JDK21）
 */
public class VirtualThreadExample {

    /**
     * 模拟耗时任务（如：远程调用 / IO）
     */
    public static String task(int i) {
        try {
            Thread.sleep(100); // 模拟 IO 阻塞（虚拟线程非常适合）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "任务" + i + " -> " + Thread.currentThread();
    }

    /**
     * 核心方法：虚拟线程使用
     */
    public static void virtualThreadUsage() throws Exception {

        // =========================
        // 1. 创建虚拟线程（最简单🔥）
        // =========================
        Thread.startVirtualThread(() -> {
            System.out.println("虚拟线程：" + Thread.currentThread());
        });


        // =========================
        // 2. Thread.ofVirtual 创建
        // =========================
        Thread t = Thread.ofVirtual()
                .name("vt-", 1)
                .start(() -> System.out.println("自定义虚拟线程：" + Thread.currentThread()));

        t.join();


        // =========================
        // 3. 批量任务（对比传统线程池）
        // =========================
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int index = i;
            Thread vt = Thread.startVirtualThread(() -> {
                String result = task(index);
                System.out.println(result);
            });
            threads.add(vt);
        }

        // 等待完成
        for (Thread thread : threads) {
            thread.join();
        }


        // =========================
        // 4. 使用 Executor（推荐🔥）
        // =========================
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                int index = i;
                futures.add(executor.submit(() -> task(index)));
            }

            for (Future<String> future : futures) {
                System.out.println("结果：" + future.get());
            }
        }


        // =========================
        // 5. 对比：传统线程池（资源重）
        // =========================
        ExecutorService pool = Executors.newFixedThreadPool(3);

        List<Future<String>> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int index = i;
            list.add(pool.submit(() -> task(index)));
        }

        for (Future<String> f : list) {
            System.out.println("传统线程池：" + f.get());
        }

        pool.shutdown();


        // =========================
        // 6. 高并发场景（虚拟线程优势🔥）
        // =========================
        long start = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> tasks = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                tasks.add(executor.submit(() -> task(1)));
            }

            for (Future<?> f : tasks) {
                f.get();
            }
        }

        long end = System.currentTimeMillis();

        System.out.println("1000任务耗时：" + (end - start) + " ms");


        // =========================
        // 7. 注意：虚拟线程适合 IO，不适合 CPU 密集
        // =========================
        System.out.println("\n建议：");
        System.out.println("✔ IO 密集：虚拟线程");
        System.out.println("✔ CPU 密集：传统线程池");


        // =========================
        // 8. 项目实战（接口并发调用🔥）
        // =========================
        List<String> results = callApis();

        System.out.println("\n接口聚合：" + results);
    }

    /**
     * 项目实战：并发调用多个接口
     */
    public static List<String> callApis() throws Exception {

        List<String> result = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = List.of(
                    executor.submit(() -> task(1)),
                    executor.submit(() -> task(2)),
                    executor.submit(() -> task(3))
            );

            for (Future<String> f : futures) {
                result.add(f.get());
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        virtualThreadUsage();
    }
}
```

输出

```
自定义虚拟线程：VirtualThread[#24,vt-1]/runnable@ForkJoinPool-1-worker-2
虚拟线程：VirtualThread[#22]/runnable@ForkJoinPool-1-worker-1
任务0 -> VirtualThread[#27]/runnable@ForkJoinPool-1-worker-5
任务8 -> VirtualThread[#35]/runnable@ForkJoinPool-1-worker-2
任务3 -> VirtualThread[#30]/runnable@ForkJoinPool-1-worker-6
任务7 -> VirtualThread[#34]/runnable@ForkJoinPool-1-worker-8
任务4 -> VirtualThread[#31]/runnable@ForkJoinPool-1-worker-2
任务5 -> VirtualThread[#32]/runnable@ForkJoinPool-1-worker-1
任务1 -> VirtualThread[#28]/runnable@ForkJoinPool-1-worker-4
任务9 -> VirtualThread[#36]/runnable@ForkJoinPool-1-worker-6
任务2 -> VirtualThread[#29]/runnable@ForkJoinPool-1-worker-3
任务6 -> VirtualThread[#33]/runnable@ForkJoinPool-1-worker-7
结果：任务0 -> VirtualThread[#43]/runnable@ForkJoinPool-1-worker-3
结果：任务1 -> VirtualThread[#44]/runnable@ForkJoinPool-1-worker-4
结果：任务2 -> VirtualThread[#45]/runnable@ForkJoinPool-1-worker-2
结果：任务3 -> VirtualThread[#46]/runnable@ForkJoinPool-1-worker-8
结果：任务4 -> VirtualThread[#47]/runnable@ForkJoinPool-1-worker-6
结果：任务5 -> VirtualThread[#48]/runnable@ForkJoinPool-1-worker-4
结果：任务6 -> VirtualThread[#49]/runnable@ForkJoinPool-1-worker-6
结果：任务7 -> VirtualThread[#50]/runnable@ForkJoinPool-1-worker-2
结果：任务8 -> VirtualThread[#51]/runnable@ForkJoinPool-1-worker-8
结果：任务9 -> VirtualThread[#52]/runnable@ForkJoinPool-1-worker-2
传统线程池：任务0 -> Thread[#53,pool-1-thread-1,5,main]
传统线程池：任务1 -> Thread[#54,pool-1-thread-2,5,main]
传统线程池：任务2 -> Thread[#55,pool-1-thread-3,5,main]
传统线程池：任务3 -> Thread[#54,pool-1-thread-2,5,main]
传统线程池：任务4 -> Thread[#55,pool-1-thread-3,5,main]
传统线程池：任务5 -> Thread[#53,pool-1-thread-1,5,main]
传统线程池：任务6 -> Thread[#53,pool-1-thread-1,5,main]
传统线程池：任务7 -> Thread[#54,pool-1-thread-2,5,main]
传统线程池：任务8 -> Thread[#55,pool-1-thread-3,5,main]
传统线程池：任务9 -> Thread[#53,pool-1-thread-1,5,main]
1000任务耗时：133 ms

建议：
✔ IO 密集：虚拟线程
✔ CPU 密集：传统线程池

接口聚合：[任务1 -> VirtualThread[#1056]/runnable@ForkJoinPool-1-worker-5, 任务2 -> VirtualThread[#1057]/runnable@ForkJoinPool-1-worker-4, 任务3 -> VirtualThread[#1058]/runnable@ForkJoinPool-1-worker-4]

```



### Structured Concurrency（结构化并发，JDK21）

```java
package io.github.atengk.basic;

import java.util.concurrent.*;
import java.util.*;

/**
 * Structured Concurrency 示例（JDK21🔥）
 */
public class StructuredConcurrencyExample {

    /**
     * 模拟接口调用
     */
    public static String getUser() {
        sleep(100);
        return "张三";
    }

    public static int getOrder() {
        sleep(150);
        return 5;
    }

    public static double getBalance() {
        sleep(200);
        return 1000.0;
    }

    public static String errorTask() {
        sleep(100);
        throw new RuntimeException("任务异常");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 核心方法：结构化并发使用
     */
    public static void structuredUsage() throws Exception {

        // =========================
        // 1. 基础用法（fork + join🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // fork 子任务（自动使用虚拟线程）
            var userTask = scope.fork(StructuredConcurrencyExample::getUser);
            var orderTask = scope.fork(StructuredConcurrencyExample::getOrder);
            var balanceTask = scope.fork(StructuredConcurrencyExample::getBalance);

            // 等待所有任务完成
            scope.join();

            // 如果有异常则抛出
            scope.throwIfFailed();

            // 获取结果
            System.out.println("结果："
                    + userTask.get() + ", "
                    + orderTask.get() + ", "
                    + balanceTask.get());
        }


        // =========================
        // 2. 失败快速取消（核心🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var t1 = scope.fork(StructuredConcurrencyExample::getUser);
            var t2 = scope.fork(StructuredConcurrencyExample::errorTask); // 异常任务
            var t3 = scope.fork(StructuredConcurrencyExample::getBalance);

            scope.join();

            // 任一失败，其他任务自动取消
            scope.throwIfFailed();

            System.out.println(t1.get() + "," + t2.get() + "," + t3.get());

        } catch (Exception e) {
            System.out.println("\n失败自动取消：" + e.getMessage());
        }


        // =========================
        // 3. 任意成功返回（ShutdownOnSuccess🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {

            scope.fork(() -> {
                sleep(200);
                return "慢任务";
            });

            scope.fork(() -> {
                sleep(100);
                return "快任务";
            });

            scope.join();

            // 返回最先成功的结果
            String result = scope.result();

            System.out.println("\n最快结果：" + result);
        }


        // =========================
        // 4. 超时控制
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var task = scope.fork(() -> {
                sleep(3000);
                return "慢任务";
            });

            try {
                // 最多等待1秒
                scope.joinUntil(java.time.Instant.now().plusSeconds(1));
            } catch (TimeoutException e) {
                System.out.println("\n超时，取消任务");
                scope.shutdown();
            }

            // 等待任务真正结束（很关键）
            scope.join();

            if (task.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                System.out.println(task.get());
            } else {
                System.out.println("任务未成功完成，状态：" + task.state());
            }
        }


        // =========================
        // 5. 项目实战：接口聚合（推荐🔥）
        // =========================
        Map<String, Object> dashboard = buildDashboard();

        System.out.println("\n聚合接口返回：" + dashboard);
    }

    /**
     * 项目实战：结构化并发实现接口聚合
     */
    public static Map<String, Object> buildDashboard() throws Exception {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var user = scope.fork(StructuredConcurrencyExample::getUser);
            var order = scope.fork(StructuredConcurrencyExample::getOrder);
            var balance = scope.fork(StructuredConcurrencyExample::getBalance);

            scope.join();
            scope.throwIfFailed();

            return Map.of(
                    "user", user.get(),
                    "order", order.get(),
                    "balance", balance.get()
            );
        }
    }

    public static void main(String[] args) throws Exception {
        structuredUsage();
    }
}

```

输出

```
结果：张三, 5, 1000.0

失败自动取消：java.lang.RuntimeException: 任务异常

最快结果：快任务

超时，取消任务
任务未成功完成，状态：UNAVAILABLE

聚合接口返回：{user=张三, order=5, balance=1000.0}
```



------

## 七、类 & 语法增强（JDK14+）

### record 数据类（替代 DTO / VO）

### sealed 类（限制继承结构）

------

## 八、Switch & 模式匹配（JDK17+）

### switch 表达式（更简洁）

### instanceof 模式匹配

### switch 模式匹配（JDK21）

------

## 九、集合增强（JDK9+）

### Stream.toList()（替代 collect）

### Map.computeIfAbsent 实战

------

## 十、其他实用增强

### var 局部变量类型推断（JDK10）

### Objects 工具类增强（判空/比较）

### Pattern Matching + 解构（JDK21）

------

