# 函数式接口（Functional Interface）

**函数式接口（Functional Interface）** 是指：

> 仅包含一个抽象方法（Single Abstract Method，简称 SAM）的接口。

Java 8 引入 **Lambda 表达式** 和 **方法引用**，使函数式接口成为函数式编程的核心基础。

| 接口                | 抽象方法       | 入参 | 返回值  | 核心作用     | 常见使用场景             |
| ------------------- | -------------- | ---- | ------- | ------------ | ------------------------ |
| `Supplier<T>`       | `get()`        | 无   | T       | 提供数据     | 懒加载、默认值、对象工厂 |
| `Consumer<T>`       | `accept(T)`    | T    | 无      | 消费数据     | 遍历、打印、写日志       |
| `BiConsumer<T,U>`   | `accept(T,U)`  | T,U  | 无      | 消费两个参数 | Map遍历                  |
| `Function<T,R>`     | `apply(T)`     | T    | R       | 类型转换     | 数据转换、DTO映射        |
| `BiFunction<T,U,R>` | `apply(T,U)`   | T,U  | R       | 两参转换     | 计算逻辑                 |
| `Predicate<T>`      | `test(T)`      | T    | boolean | 条件判断     | 过滤                     |
| `BiPredicate<T,U>`  | `test(T,U)`    | T,U  | boolean | 两参判断     | 比较逻辑                 |
| `UnaryOperator<T>`  | `apply(T)`     | T    | T       | 同类型转换   | 字符串处理               |
| `BinaryOperator<T>` | `apply(T,T)`   | T,T  | T       | 同类型合并   | reduce 聚合              |
| `Comparator<T>`     | `compare(T,T)` | T,T  | int     | 排序规则     | 排序                     |



## `Supplier<T>`

### 接口作用说明

`Supplier<T>` 是 **数据提供者接口**。
 它的核心价值不是“返回一个值”，而是：

> ✅ **推迟执行（Lazy Execution）**
>  ✅ **按需生成对象**
>  ✅ **避免不必要的计算开销**

在企业开发中常用于：

- 懒加载
- 默认值生成
- Stream 数据生成
- 工厂模式
- 日志优化

------

### 接口定义理解

```
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

**特点**

- 无参数
- 有返回值
- 每次调用 `get()` 才真正执行逻辑
- 典型用途：**延迟执行 / 懒加载 / 默认值生成 / 对象工厂**

### 创建测试类

```java
package io.github.atengk.basic;

import java.time.LocalDateTime;

public class SupplierTests {

    /**
     * 示例实体类
     */
    static class User {

        private String id;
        private LocalDateTime createTime;

        public void setId(String id) {
            this.id = id;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        @Override
        public String toString() {
            return "User{id='" + id + "', createTime=" + createTime + '}';
        }
    }
    
}

```

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicSupplier() {
        Supplier<String> supplier = () -> "Hello Java8";

        System.out.println("基础使用: " + supplier.get());
    }
```

### 延迟执行

```java
    /**
     * 2. 延迟执行
     *
     * 只有调用 get() 时才会真正执行
     */
    @Test
    void testLazyExecution() {

        Supplier<String> timeSupplier = () -> {
            System.out.println("正在生成时间...");
            return LocalDateTime.now().toString();
        };

        System.out.println("Supplier 已创建，但未执行");
        System.out.println("调用结果: " + timeSupplier.get());
    }
```

### Optional默认值

```java
    /**
     * 3. Optional 默认值
     *
     * 推荐使用 orElseGet，而不是 orElse
     * 因为 orElse 会立即执行
     */
    @Test
    void testOptionalDefaultValue() {

        String value = null;

        String result = Optional.ofNullable(value)
                .orElseGet(() -> "默认值");

        System.out.println("Optional默认值: " + result);
    }
```

### Stream生成数据

```java
    /**
     * 4. Stream 生成数据
     *
     * Stream.generate 本质就是接收 Supplier
     */
    @Test
    void testStreamGenerate() {

        Stream<String> stream = Stream.generate(
                () -> UUID.randomUUID().toString()
        );

        stream.limit(3)
                .forEach(s -> System.out.println("生成UUID: " + s));
    }
```

### 工厂模式

```java
    /**
     * 5. 工厂模式
     *
     * 用于对象创建逻辑封装
     */
    @Test
    void testFactoryPattern() {

        Supplier<User> userFactory = () -> {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCreateTime(LocalDateTime.now());
            return user;
        };

        User user = userFactory.get();

        System.out.println("工厂创建对象: " + user);
    }
```



## `Consumer<T>`

### 接口作用说明

`Consumer<T>` 是 **数据消费者接口**。
它的核心价值不是“处理一个参数”，而是：

> ✅ **对数据执行操作（副作用行为）**
> ✅ **专注行为而非返回值**
> ✅ **适用于遍历、日志、事件处理**

在企业开发中常用于：

- 集合遍历（forEach）
- 日志处理
- 事件回调
- 消息消费
- 数据落库

------

### 接口定义理解

```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
```

**特点**

- 有一个参数
- 无返回值
- 关注“做什么”，而不是“返回什么”
- 典型用途：**遍历处理 / 日志输出 / 事件响应**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConsumerTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicConsumer() {

        Consumer<String> consumer = s -> System.out.println("处理数据: " + s);

        consumer.accept("Hello Java8");
    }
```

------

### 集合遍历

```java
    /**
     * 2. 集合遍历
     *
     * forEach 底层接收 Consumer
     */
    @Test
    void testForEachUsage() {

        List<String> list = Arrays.asList("Java", "Spring", "Boot");

        list.forEach(s -> System.out.println("遍历元素: " + s));
    }
```

------

### 方法引用

```java
    /**
     * 3. 方法引用
     */
    @Test
    void testMethodReference() {

        Consumer<String> printer = System.out::println;

        printer.accept("方法引用示例");
    }
```

------

### 链式调用（andThen）

```java
    /**
     * 4. 链式执行
     *
     * Consumer 可以通过 andThen 组合
     */
    @Test
    void testAndThen() {

        Consumer<String> first =
                s -> System.out.println("第一步: " + s);

        Consumer<String> second =
                s -> System.out.println("第二步: " + s.toUpperCase());

        Consumer<String> combined = first.andThen(second);

        combined.accept("java");
    }
```

------

### 日志延迟优化（企业常用）

```java
    /**
     * 5. 日志场景示例
     *
     * 统一处理输出逻辑
     */
    @Test
    void testLoggingScenario() {

        Consumer<String> logger =
                message -> System.out.println("[INFO] " + message);

        logger.accept("系统启动成功");
    }
```



## `Function<T, R>`

### 接口作用说明

`Function<T, R>` 是 **数据转换接口**。
它的核心价值不是“接收一个参数”，而是：

> ✅ **将一种类型转换为另一种类型**
> ✅ **构建可组合的数据处理链**
> ✅ **用于数据加工与映射**

在企业开发中常用于：

- DTO ↔ Entity 转换
- 数据格式转换
- Stream 的 `map`
- 参数预处理
- 业务计算逻辑

------

### 接口定义理解

```java
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
```

**特点**

- 一个输入参数
- 一个返回值
- 允许类型不同（T → R）
- 典型用途：**数据转换 / 数据映射 / 流式处理**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FunctionTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicFunction() {

        Function<String, Integer> lengthFunction =
                s -> s.length();

        Integer result = lengthFunction.apply("Java");

        System.out.println("字符串长度: " + result);
    }
```

------

### 类型转换

```java
    /**
     * 2. 类型转换
     */
    @Test
    void testTypeConversion() {

        Function<String, Integer> parseFunction =
                Integer::parseInt;

        Integer number = parseFunction.apply("100");

        System.out.println("转换结果: " + number);
    }
```

------

### Stream 映射（企业常用）

```java
    /**
     * 3. Stream 映射
     *
     * map 本质接收 Function
     */
    @Test
    void testStreamMap() {

        List<String> list = Arrays.asList("java", "spring", "boot");

        List<Integer> lengths = list.stream()
                .map(String::length)
                .collect(Collectors.toList());

        System.out.println("长度集合: " + lengths);
    }
```

------

### 函数组合（compose / andThen）

```java
    /**
     * 4. 函数组合
     *
     * andThen：先执行当前函数，再执行后续函数
     * compose：先执行传入函数
     */
    @Test
    void testFunctionCompose() {

        Function<String, String> addPrefix =
                s -> "Hello " + s;

        Function<String, String> toUpper =
                String::toUpperCase;

        Function<String, String> combined =
                addPrefix.andThen(toUpper);

        System.out.println(combined.apply("java"));
    }
```

------

### DTO 转换示例（企业实战）

```java
    /**
     * 5. DTO 转换示例
     */
    @Test
    void testDtoMapping() {

        Function<UserEntity, UserDTO> mapper = entity -> {
            UserDTO dto = new UserDTO();
            dto.setUsername(entity.getUsername());
            return dto;
        };

        UserEntity entity = new UserEntity();
        entity.setUsername("admin");

        UserDTO dto = mapper.apply(entity);

        System.out.println("DTO用户名: " + dto.getUsername());
    }

    static class UserEntity {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    static class UserDTO {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
```



## `Predicate<T>`

### 接口作用说明

`Predicate<T>` 是 **条件判断接口**。
它的核心价值不是“返回 boolean”，而是：

> ✅ **表达一个可复用的判断规则**
> ✅ **支持逻辑组合（and / or / negate）**
> ✅ **用于过滤与校验**

在企业开发中常用于：

- 参数校验
- 业务规则判断
- Stream 过滤
- 条件匹配
- 数据筛选

------

### 接口定义理解

```java
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}
```

**特点**

- 一个输入参数
- 返回 boolean
- 专门用于表达“条件”
- 典型用途：**过滤 / 校验 / 规则组合**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PredicateTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicPredicate() {

        Predicate<String> isEmpty =
                s -> s == null || s.isEmpty();

        System.out.println("判断结果: " + isEmpty.test(""));
    }
```

------

### Stream 过滤（企业常用）

```java
    /**
     * 2. Stream 过滤
     *
     * filter 本质接收 Predicate
     */
    @Test
    void testStreamFilter() {

        List<String> list = Arrays.asList("Java", "", "Spring", "Boot");

        List<String> result = list.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        System.out.println("过滤结果: " + result);
    }
```

------

### 逻辑组合（and / or / negate）

```java
    /**
     * 3. 条件组合
     */
    @Test
    void testPredicateCompose() {

        Predicate<String> notEmpty =
                s -> s != null && !s.isEmpty();

        Predicate<String> longerThan3 =
                s -> s.length() > 3;

        Predicate<String> condition =
                notEmpty.and(longerThan3);

        System.out.println("组合判断: " + condition.test("Java"));
    }
```

------

### 参数校验示例（企业实战）

```java
    /**
     * 4. 参数校验示例
     */
    @Test
    void testValidationScenario() {

        Predicate<User> usernameValid =
                user -> user.getUsername() != null && user.getUsername().length() >= 4;

        User user = new User();
        user.setUsername("admin");

        System.out.println("用户名是否合法: " + usernameValid.test(user));
    }

    static class User {

        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
```

------



## `BiFunction<T, U, R>`

### 接口作用说明

`BiFunction<T, U, R>` 是 **双参数转换接口**。
它的核心价值不是“接收两个参数”，而是：

> ✅ **对两个输入进行计算或组合**
> ✅ **用于合并、计算、映射操作**
> ✅ **支持函数式组合（andThen）**

在企业开发中常用于：

- 两个参数的业务计算
- Map.compute / merge
- 金额计算
- 参数拼接
- 组合逻辑处理

------

### 接口定义理解

```java
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
```

**特点**

- 两个输入参数
- 一个返回值
- 允许三种类型不同（T, U → R）
- 典型用途：**双参计算 / 合并逻辑 / Map 操作**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class BiFunctionTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiFunction() {

        BiFunction<Integer, Integer, Integer> add =
                (a, b) -> a + b;

        Integer result = add.apply(10, 20);

        System.out.println("计算结果: " + result);
    }
```

------

### 字符串拼接

```java
    /**
     * 2. 字符串拼接
     */
    @Test
    void testStringConcat() {

        BiFunction<String, String, String> concat =
                (a, b) -> a + "-" + b;

        String result = concat.apply("Java", "Spring");

        System.out.println("拼接结果: " + result);
    }
```

------

### Map.compute 使用（企业常用）

```java
    /**
     * 3. Map.compute 使用
     *
     * compute 本质接收 BiFunction
     */
    @Test
    void testMapCompute() {

        Map<String, Integer> map = new HashMap<>();
        map.put("count", 1);

        map.compute("count", (key, value) ->
                value == null ? 1 : value + 1
        );

        System.out.println("更新结果: " + map.get("count"));
    }
```

------

### 函数组合（andThen）

```java
    /**
     * 4. 函数组合
     *
     * 先执行 BiFunction，再执行后续 Function
     */
    @Test
    void testAndThen() {

        BiFunction<Integer, Integer, Integer> multiply =
                (a, b) -> a * b;

        Integer result = multiply
                .andThen(r -> r + 10)
                .apply(5, 2);

        System.out.println("组合结果: " + result);
    }
```

------

### 金额计算示例（企业实战）

```java
    /**
     * 5. 金额计算示例
     */
    @Test
    void testPriceCalculation() {

        BiFunction<Double, Double, Double> calculateTotal =
                (price, quantity) -> price * quantity;

        Double total = calculateTotal.apply(99.9, 3.0);

        System.out.println("总金额: " + total);
    }
```

------



## `BiPredicate<T, U>`

### 接口作用说明

`BiPredicate<T, U>` 是 **双参数条件判断接口**。
它的核心价值不是“判断两个参数”，而是：

> ✅ **表达两个对象之间的判断规则**
> ✅ **支持逻辑组合（and / or / negate）**
> ✅ **适用于比较、匹配、校验场景**

在企业开发中常用于：

- 两个字段比较
- 数据匹配规则
- 权限判断
- 业务条件校验
- 关联关系校验

------

### 接口定义理解

```java
@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);
}
```

**特点**

- 两个输入参数
- 返回 boolean
- 专门表达“双对象判断规则”
- 典型用途：**比较 / 匹配 / 条件组合**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

public class BiPredicateTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiPredicate() {

        BiPredicate<String, String> equals =
                (a, b) -> a.equals(b);

        System.out.println("判断结果: " + equals.test("Java", "Java"));
    }
```

------

### 数值比较

```java
    /**
     * 2. 数值比较
     */
    @Test
    void testNumberCompare() {

        BiPredicate<Integer, Integer> greaterThan =
                (a, b) -> a > b;

        System.out.println("是否大于: " + greaterThan.test(10, 5));
    }
```

------

### 逻辑组合（and / or / negate）

```java
    /**
     * 3. 条件组合
     */
    @Test
    void testBiPredicateCompose() {

        BiPredicate<String, String> notNull =
                (a, b) -> a != null && b != null;

        BiPredicate<String, String> equals =
                String::equals;

        BiPredicate<String, String> condition =
                notNull.and(equals);

        System.out.println("组合判断: " + condition.test("Spring", "Spring"));
    }
```

------

### 权限校验示例（企业实战）

```java
    /**
     * 4. 权限校验示例
     */
    @Test
    void testPermissionScenario() {

        BiPredicate<User, String> hasRole =
                (user, role) -> user.getRole().equals(role);

        User user = new User();
        user.setRole("ADMIN");

        System.out.println("是否有权限: " + hasRole.test(user, "ADMIN"));
    }

    static class User {

        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
```

------



## `BiConsumer<T, U>`

### 接口作用说明

`BiConsumer<T, U>` 是 **双参数消费接口**。
它的核心价值不是“接收两个参数”，而是：

> ✅ **对两个参数执行操作（无返回值）**
> ✅ **专注副作用行为（修改状态 / 输出 / 持久化）**
> ✅ **适用于键值对处理场景**

在企业开发中常用于：

- `Map.forEach`
- 双参数日志输出
- 数据落库
- 状态更新
- 回调处理

------

### 接口定义理解

```java
@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);
}
```

**特点**

- 两个输入参数
- 无返回值
- 表达“对两个参数执行行为”
- 典型用途：**Map遍历 / 双参处理 / 事件回调**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BiConsumerTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiConsumer() {

        BiConsumer<String, Integer> printer =
                (name, age) ->
                        System.out.println("姓名: " + name + ", 年龄: " + age);

        printer.accept("张三", 20);
    }
```

------

### Map 遍历（企业常用）

```java
    /**
     * 2. Map 遍历
     *
     * Map.forEach 本质接收 BiConsumer
     */
    @Test
    void testMapForEach() {

        Map<String, Integer> map = new HashMap<>();
        map.put("Java", 1);
        map.put("Spring", 2);

        map.forEach((key, value) ->
                System.out.println("Key: " + key + ", Value: " + value));
    }
```

------

### 方法引用

```java
    /**
     * 3. 方法引用
     */
    @Test
    void testMethodReference() {

        BiConsumer<String, String> logger =
                BiConsumerTests::printLog;

        logger.accept("INFO", "系统启动成功");
    }

    static void printLog(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }
```

------

### 链式执行（andThen）

```java
    /**
     * 4. 链式执行
     */
    @Test
    void testAndThen() {

        BiConsumer<String, Integer> first =
                (name, age) ->
                        System.out.println("基础信息: " + name);

        BiConsumer<String, Integer> second =
                (name, age) ->
                        System.out.println("年龄信息: " + age);

        BiConsumer<String, Integer> combined =
                first.andThen(second);

        combined.accept("李四", 25);
    }
```

------

### 数据更新示例（企业实战）

```java
    /**
     * 5. 数据更新示例
     */
    @Test
    void testUpdateScenario() {

        BiConsumer<User, Integer> updateAge =
                (user, newAge) -> user.setAge(newAge);

        User user = new User();
        user.setName("王五");
        user.setAge(18);

        updateAge.accept(user, 30);

        System.out.println("更新后年龄: " + user.getAge());
    }

    static class User {

        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
```

------

## `UnaryOperator<T>`

### 接口作用说明

`UnaryOperator<T>` 是 **单参数同类型转换接口**。
它的核心价值不是“接收一个参数”，而是：

> ✅ **对同类型数据进行转换**
> ✅ **强调“自己变自己”（T → T）**
> ✅ **用于数据加工、修饰、增强**

在企业开发中常用于：

- 字符串处理
- 数值处理
- Stream.map 同类型转换
- 数据标准化
- 字段修正

------

### 接口定义理解

```java
@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
}
```

**特点**

- 一个输入参数
- 返回值类型与输入类型相同
- 本质是 `Function<T, T>`
- 典型用途：**同类型转换 / 数据修饰**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class UnaryOperatorTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicUnaryOperator() {

        UnaryOperator<String> toUpper =
                s -> s.toUpperCase();

        String result = toUpper.apply("java");

        System.out.println("转换结果: " + result);
    }
```

------

### 数值处理

```java
    /**
     * 2. 数值处理
     */
    @Test
    void testNumberProcess() {

        UnaryOperator<Integer> doubleValue =
                n -> n * 2;

        System.out.println("处理结果: " + doubleValue.apply(10));
    }
```

------

### Stream 同类型转换

```java
    /**
     * 3. Stream 使用
     */
    @Test
    void testStreamUsage() {

        List<String> list = Arrays.asList("java", "spring", "boot");

        List<String> result = list.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        System.out.println("转换集合: " + result);
    }
```

---

### 数据标准化示例（企业实战）

```java
    /**
     * 4. 数据标准化示例
     */
    @Test
    void testNormalizeScenario() {

        UnaryOperator<User> normalizeUser =
                user -> {
                    user.setUsername(user.getUsername().trim().toLowerCase());
                    return user;
                };

        User user = new User();
        user.setUsername("  ADMIN  ");

        normalizeUser.apply(user);

        System.out.println("标准化后用户名: " + user.getUsername());
    }

    static class User {

        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
```

------

## `BinaryOperator<T>`

### 接口作用说明

`BinaryOperator<T>` 是 **双参数同类型合并接口**。
它的核心价值不是“接收两个参数”，而是：

> ✅ **将两个同类型对象合并为一个对象**
> ✅ **常用于聚合（reduce）操作**
> ✅ **强调“同类型合并”（T, T → T）**

在企业开发中常用于：

- Stream.reduce 聚合
- 金额累计
- 最大值 / 最小值计算
- Map.merge
- 统计汇总

------

### 接口定义理解

```java
@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T, T, T> {
}
```

**特点**

- 两个输入参数
- 返回值类型与输入类型相同
- 本质是 `BiFunction<T, T, T>`
- 典型用途：**聚合 / 合并 / 累计**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class BinaryOperatorTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBinaryOperator() {

        BinaryOperator<Integer> add =
                (a, b) -> a + b;

        Integer result = add.apply(10, 20);

        System.out.println("计算结果: " + result);
    }
```

------

### Stream.reduce 聚合（企业常用）

```java
    /**
     * 2. Stream 聚合
     *
     * reduce 本质接收 BinaryOperator
     */
    @Test
    void testStreamReduce() {

        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        Integer sum = list.stream()
                .reduce(0, (a, b) -> a + b);

        System.out.println("累计结果: " + sum);
    }
```

------

### 求最大值

```java
    /**
     * 3. 求最大值
     */
    @Test
    void testMaxValue() {

        List<Integer> list = Arrays.asList(5, 9, 3, 7);

        Optional<Integer> max = list.stream()
                .reduce(BinaryOperator.maxBy(Comparator.naturalOrder()));

        System.out.println("最大值: " + max.orElse(null));
    }
```

------

### Map.merge 使用（企业常用）

```java
    /**
     * 4. Map.merge 示例
     */
    @Test
    void testMapMerge() {

        java.util.Map<String, Integer> map = new java.util.HashMap<>();

        map.merge("count", 1, (oldVal, newVal) -> oldVal + newVal);
        map.merge("count", 1, (oldVal, newVal) -> oldVal + newVal);

        System.out.println("合并结果: " + map.get("count"));
    }
```

------

### 金额累计示例（企业实战）

```java
    /**
     * 5. 金额累计示例
     */
    @Test
    void testAmountAggregation() {

        List<Double> prices = Arrays.asList(99.9, 199.9, 299.9);

        Double total = prices.stream()
                .reduce(0.0, Double::sum);

        System.out.println("总金额: " + total);
    }
```

------

## `Comparator<T>`

### 接口作用说明

`Comparator<T>` 是 **排序规则接口**。
它的核心价值不是“比较两个对象”，而是：

> ✅ **定义对象排序规则**
> ✅ **将业务排序逻辑抽象为可复用策略**
> ✅ **支持链式排序与条件排序**

在企业开发中常用于：

- 集合排序
- 多字段排序
- 数据库结果二次排序
- 复杂业务排序规则
- Stream.sorted()

------

### 接口定义理解

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

**特点**

- 两个输入参数
- 返回 int
- 返回值规则：
  - 负数 → o1 小于 o2
  - 0 → 相等
  - 正数 → o1 大于 o2
- 典型用途：**排序 / 多字段排序 / 自定义排序策略**

------

### 创建测试类

```java
package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ComparatorTests {

}
```

------

### 基础使用

```java
    /**
     * 1. 基础排序
     */
    @Test
    void testBasicComparator() {

        List<String> list = Arrays.asList("Java", "Go", "Python");

        list.sort((a, b) -> a.length() - b.length());

        System.out.println("排序结果: " + list);
    }
```

------

### 使用 comparing（推荐写法）

```java
    /**
     * 2. 推荐写法 comparing
     */
    @Test
    void testComparing() {

        List<User> list = Arrays.asList(
                new User("Tom", 30),
                new User("Jack", 20),
                new User("Lucy", 25)
        );

        list.sort(Comparator.comparing(User::getAge));

        System.out.println("按年龄排序: " + list);
    }
```

------

### 多字段排序（企业常用）

```java
    /**
     * 3. 多字段排序
     */
    @Test
    void testMultiSort() {

        List<User> list = Arrays.asList(
                new User("Tom", 30),
                new User("Tom", 20),
                new User("Jack", 25)
        );

        list.sort(
                Comparator.comparing(User::getName)
                          .thenComparing(User::getAge)
        );

        System.out.println("多字段排序: " + list);
    }
```

------

### 倒序排序

```java
    /**
     * 4. 倒序排序
     */
    @Test
    void testReverseOrder() {

        List<Integer> list = Arrays.asList(5, 1, 9, 3);

        list.sort(Comparator.reverseOrder());

        System.out.println("倒序结果: " + list);
    }
```

------

### Stream 排序

```java
    /**
     * 5. Stream.sorted 使用
     */
    @Test
    void testStreamSorted() {

        List<Integer> list = Arrays.asList(3, 1, 4, 2);

        List<Integer> sorted = list.stream()
                .sorted()
                .collect(Collectors.toList());

        System.out.println("Stream排序: " + sorted);
    }
```

------

### 示例实体类

```java
    static class User {

        private String name;
        private Integer age;

        public User(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + '}';
        }
    }
```

------

