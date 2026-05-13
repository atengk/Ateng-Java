# Java 新语法演进（JDK8 → JDK25）

本文围绕 Java 从 JDK8 到 JDK25 的语法演进展开，重点说明新语法在项目开发、老代码重构、版本升级和编码规范中的实际应用。JDK8 是现代 Java 语法体系的重要起点，Lambda 表达式、方法引用、函数式接口、接口默认方法、Optional、Stream API 和新日期时间 API 构成了后续语法演进的基础。

## 文档概述

本章节用于说明文档的编写目标、适用读者和版本范围，帮助读者明确本文的使用边界。本文不是单纯罗列语法点，而是从 Java 项目开发角度说明不同版本语法特性的使用方式、适用场景和注意事项。

### 编写目标

本文的编写目标是帮助 Java 开发人员系统掌握 JDK8 到 JDK25 之间的重要语法变化，并能够在项目开发中合理使用这些语法特性。

对于 Java8 项目，本文重点说明 Lambda 表达式、方法引用、函数式接口、接口默认方法、Optional、Stream API 和新日期时间 API 的使用方式，帮助开发者减少样板代码，提升集合处理、空值处理、接口扩展和日期时间处理能力。

对于准备升级 JDK 的项目，本文用于梳理不同版本之间的语法变化，说明哪些特性适合在生产环境中使用，哪些特性仍需谨慎评估，避免因为盲目引入新语法导致代码可读性下降或团队维护成本增加。

本文主要解决以下问题：

| 目标               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 掌握 JDK8 核心语法 | 理解 Lambda、函数式接口、Stream、Optional 等基础能力         |
| 支持老代码重构     | 将匿名内部类、传统循环、重复空值判断改造为更清晰的写法       |
| 建立版本演进认知   | 理解 JDK8 到 JDK25 的语法发展路线                            |
| 指导项目落地       | 明确新语法在业务代码、DTO 转换、集合处理、接口扩展中的使用方式 |
| 规范团队编码       | 避免滥用 Stream、Optional、var、Preview 特性等问题           |

### 适用读者

本文适用于具备 Java 基础语法和面向对象编程经验的开发人员，尤其适合正在维护 Java8 项目、准备进行代码重构或计划升级 JDK 版本的后端开发人员。

适用读者包括：

| 读者类型              | 适用说明                                                     |
| --------------------- | ------------------------------------------------------------ |
| Java 初中级开发工程师 | 用于系统学习 JDK8 之后的核心语法                             |
| Java 后端开发工程师   | 用于在业务开发中合理使用新语法                               |
| Spring Boot 开发人员  | 用于在 Controller、Service、DTO、VO 转换中使用现代 Java 写法 |
| 老项目维护人员        | 用于重构匿名内部类、传统循环、日期处理和空值判断             |
| 技术负责人            | 用于制定团队 Java 语法使用规范                               |
| 代码评审人员          | 用于判断新语法是否提升了代码质量，而不是只追求写法新         |

阅读本文前，建议读者已经掌握以下基础内容：

- Java 基础语法。
- 类、接口、继承、多态。
- 泛型基础。
- 集合框架基础。
- Maven 或 Gradle 基础。
- 常见后端分层结构，如 Controller、Service、Mapper、DTO、VO。

### 版本范围

本文覆盖 JDK8 到 JDK25 之间的主要语法变化和开发方式变化。其中，JDK8 是现代 Java 语法体系的起点，JDK17、JDK21 和 JDK25 是项目升级中更常被关注的重要版本。

| 阶段           | 版本范围       | 核心内容                                                     |
| -------------- | -------------- | ------------------------------------------------------------ |
| 基础阶段       | JDK8           | Lambda、方法引用、函数式接口、默认方法、Optional、Stream API、java.time |
| 增强阶段       | JDK9 到 JDK11  | 模块化、接口私有方法、try-with-resources 改进、var、集合工厂方法 |
| 成型阶段       | JDK12 到 JDK17 | switch 表达式、文本块、instanceof 模式匹配、Record、Sealed Class |
| 并发升级阶段   | JDK18 到 JDK21 | 虚拟线程、结构化并发、Scoped Values、Sequenced Collections   |
| 新语法演进阶段 | JDK22 到 JDK25 | 未命名变量、构造器改进、模块导入、紧凑源文件、实例 main 方法等 |

在实际项目中，版本采用建议如下：

| 项目情况             | 建议                                                  |
| -------------------- | ----------------------------------------------------- |
| 仍使用 JDK8 的老项目 | 优先掌握 Lambda、Stream、Optional、java.time          |
| 准备升级到 JDK17     | 重点关注 Record、switch 表达式、文本块、模式匹配      |
| 准备升级到 JDK21     | 重点关注虚拟线程、Sequenced Collections、模式匹配增强 |
| 准备升级到 JDK25     | 重点关注正式特性与 Preview 特性的边界                 |
| 生产核心业务系统     | 谨慎使用 Preview 特性，优先使用稳定语法               |

## JDK8 核心语法基础

JDK8 是 Java 语法演进中的关键版本。它引入了函数式编程风格，使 Java 可以用更简洁的方式表达行为传递、集合处理、异步任务、数据转换和空值处理逻辑。

在项目开发中，JDK8 语法最常见的应用场景包括：

- 集合过滤、排序、分组和统计。
- DTO、VO、Entity 之间的数据转换。
- 策略模式、回调逻辑和条件处理。
- 接口扩展和默认行为兼容。
- 空值判断和默认值处理。
- 日期时间计算和格式化。

### Lambda 表达式

Lambda 表达式用于简化函数式接口的实现。它可以将一段行为逻辑作为参数传递，从而减少匿名内部类的样板代码。

Lambda 表达式的基本格式如下：

```java
(参数列表) -> {
    方法体
}
```

在 Java8 之前，排序通常需要使用匿名内部类：

```java
List<String> names = Arrays.asList("Tom", "Jerry", "Alice");

Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
});
```

使用 Lambda 表达式后，可以简化为：

```java
List<String> names = Arrays.asList("Tom", "Jerry", "Alice");

names.sort((o1, o2) -> o1.compareTo(o2));
```

Lambda 常见简化规则如下：

| 规则                          | 示例                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| 参数类型可省略                | `(String name) -> name.trim()` 可写为 `name -> name.trim()`  |
| 单个参数可省略小括号          | `(name) -> name.trim()` 可写为 `name -> name.trim()`         |
| 单行返回可省略大括号和 return | `name -> { return name.trim(); }` 可写为 `name -> name.trim()` |
| 无参数需要保留小括号          | `() -> System.currentTimeMillis()`                           |

以下代码演示在项目中使用 Lambda 完成用户过滤、排序和输出。

文件位置：`src/main/java/io/github/atengk/jdk8/lambda/UserLambdaExample.java`

```java
package io.github.atengk.jdk8.lambda;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lambda 表达式项目示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserLambdaExample {

    public static void main(String[] args) {
        List<User> userList = buildUserList();

        List<User> enabledUsers = userList.stream()
                .filter(user -> user.getEnabled())
                .filter(user -> StrUtil.isNotBlank(user.getName()))
                .sorted(Comparator.comparing(User::getAge))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(enabledUsers)) {
            System.out.println("没有可用用户");
            return;
        }

        enabledUsers.forEach(user -> System.out.println(user.getName() + "：" + user.getAge()));
    }

    private static List<User> buildUserList() {
        List<User> userList = new ArrayList<>();
        userList.add(new User(1L, "Tom", 20, true));
        userList.add(new User(2L, "Jerry", 18, true));
        userList.add(new User(3L, "", 22, true));
        userList.add(new User(4L, "Alice", 25, false));
        return userList;
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class User {

        private Long id;

        private String name;

        private Integer age;

        private Boolean enabled;

        public User(Long id, String name, Integer age, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.enabled = enabled;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Boolean getEnabled() {
            return enabled;
        }
    }
}
```

Lambda 表达式适合用于简短、清晰、无复杂副作用的逻辑。如果业务逻辑较长，或者包含数据库操作、远程调用、事务处理、复杂异常处理，应提取为独立方法，而不是全部写在 Lambda 代码块中。

### 方法引用

方法引用是 Lambda 表达式的简化形式。当 Lambda 表达式只是调用一个已经存在的方法时，可以使用方法引用替代。

方法引用的常见类型如下：

| 类型                 | 写法               | 示例                  |
| -------------------- | ------------------ | --------------------- |
| 静态方法引用         | `类名::静态方法名` | `Integer::parseInt`   |
| 实例对象方法引用     | `对象::实例方法名` | `System.out::println` |
| 特定类型实例方法引用 | `类名::实例方法名` | `String::trim`        |
| 构造方法引用         | `类名::new`        | `ArrayList::new`      |

普通 Lambda 写法：

```java
list.forEach(item -> System.out.println(item));
```

方法引用写法：

```java
list.forEach(System.out::println);
```

以下代码演示静态方法引用、实例方法引用和构造方法引用的组合使用。

文件位置：`src/main/java/io/github/atengk/jdk8/reference/MethodReferenceExample.java`

```java
package io.github.atengk.jdk8.reference;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法引用项目示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MethodReferenceExample {

    public static void main(String[] args) {
        List<String> nameList = Arrays.asList(" tom ", " jerry ", " alice ", "");

        List<String> resultList = nameList.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(UserName::new)
                .map(UserName::getValue)
                .collect(Collectors.toList());

        resultList.forEach(System.out::println);
    }

    /**
     * 用户名称
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class UserName {

        private final String value;

        public UserName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
```

方法引用使用建议如下：

| 建议                           | 说明                                                    |
| ------------------------------ | ------------------------------------------------------- |
| 方法名能表达业务含义时优先使用 | 例如 `User::getName` 比 `user -> user.getName()` 更简洁 |
| 复杂逻辑不要强行使用方法引用   | 如果 Lambda 内有多步处理，应保留 Lambda 或提取方法      |
| 不要只追求代码短               | 方法引用的目标是提升可读性，不是单纯减少字符数          |
| 与 Stream 配合使用较多         | 常用于 `map`、`filter`、`forEach`、`sorted` 等操作      |

### 函数式接口

函数式接口是指只有一个抽象方法的接口。Lambda 表达式和方法引用都必须依赖函数式接口才能使用。

Java8 提供了 `@FunctionalInterface` 注解，用于标识函数式接口。该注解不是必须的，但在项目开发中建议添加，因为它可以让编译器帮助检查接口是否仍然满足函数式接口要求。

JDK 内置常用函数式接口如下：

| 接口                  | 抽象方法              | 典型用途                           |
| --------------------- | --------------------- | ---------------------------------- |
| `Function<T, R>`      | `R apply(T t)`        | 输入一个值，返回另一个值           |
| `Consumer<T>`         | `void accept(T t)`    | 消费一个值，无返回值               |
| `Supplier<T>`         | `T get()`             | 无输入，返回一个值                 |
| `Predicate<T>`        | `boolean test(T t)`   | 条件判断                           |
| `BiFunction<T, U, R>` | `R apply(T t, U u)`   | 输入两个值，返回一个值             |
| `UnaryOperator<T>`    | `T apply(T t)`        | 输入和输出类型一致                 |
| `BinaryOperator<T>`   | `T apply(T t1, T t2)` | 两个相同类型输入，返回相同类型结果 |

以下代码演示使用 JDK 内置函数式接口完成数据过滤和转换。

文件位置：`src/main/java/io/github/atengk/jdk8/functional/FunctionalInterfaceExample.java`

```java
package io.github.atengk.jdk8.functional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 函数式接口项目示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FunctionalInterfaceExample {

    public static void main(String[] args) {
        List<String> nameList = Arrays.asList("Tom", "", "Jerry", "Alice");

        Predicate<String> nameValidPredicate = StrUtil::isNotBlank;
        Function<String, String> nameFormatFunction = name -> "用户：" + name.trim();

        List<String> resultList = nameList.stream()
                .filter(nameValidPredicate)
                .map(nameFormatFunction)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(resultList)) {
            System.out.println("没有有效用户名称");
            return;
        }

        resultList.forEach(System.out::println);
    }
}
```

当业务语义比较明确时，可以自定义函数式接口。例如，项目中经常需要定义通用业务校验逻辑，可以创建一个校验接口。

文件位置：`src/main/java/io/github/atengk/jdk8/functional/BizValidator.java`

```java
package io.github.atengk.jdk8.functional;

/**
 * 业务校验函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface BizValidator<T> {

    boolean validate(T data);
}
```

以下代码演示自定义函数式接口的使用方式。

文件位置：`src/main/java/io/github/atengk/jdk8/functional/BizValidatorExample.java`

```java
package io.github.atengk.jdk8.functional;

import cn.hutool.core.util.StrUtil;

/**
 * 自定义函数式接口使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizValidatorExample {

    public static void main(String[] args) {
        BizValidator<String> mobileValidator = mobile -> StrUtil.isNotBlank(mobile) && mobile.length() == 11;

        String mobile = "13800138000";

        if (!mobileValidator.validate(mobile)) {
            System.out.println("手机号格式不正确");
            return;
        }

        System.out.println("手机号校验通过");
    }
}
```

函数式接口适合以下场景：

| 场景       | 示例                                         |
| ---------- | -------------------------------------------- |
| 策略模式   | 将不同计费规则、折扣规则、校验规则封装为函数 |
| 数据转换   | 将 Entity 转换为 DTO 或 VO                   |
| 条件过滤   | 使用 `Predicate` 表达业务过滤条件            |
| 回调处理   | 在通用流程中传入成功、失败或异常处理逻辑     |
| 默认值生成 | 使用 `Supplier` 延迟生成默认值               |

使用函数式接口时需要注意：

- 优先使用 JDK 内置函数式接口。
- 自定义函数式接口名称应体现业务语义。
- 抽象方法参数不宜过多。
- Lambda 中不建议修改外部共享变量。
- 复杂业务逻辑应提取为普通方法或独立类。

### 接口默认方法与静态方法

JDK8 允许接口中定义默认方法和静态方法。默认方法使用 `default` 修饰，静态方法使用 `static` 修饰。

接口默认方法主要用于接口升级兼容。过去如果给接口新增抽象方法，所有实现类都必须修改。JDK8 之后，可以通过默认方法为接口新增行为，同时不破坏已有实现类。

接口静态方法适合放置与接口强相关的工具逻辑，调用时通过接口名直接调用。

以下代码演示通过接口默认方法增加用户名称校验逻辑，通过接口静态方法增加用户 ID 校验逻辑。

文件位置：`src/main/java/io/github/atengk/jdk8/interfaceplus/UserService.java`

```java
package io.github.atengk.jdk8.interfaceplus;

import cn.hutool.core.util.StrUtil;

/**
 * 用户服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserService {

    UserInfo getById(Long userId);

    default boolean checkUserName(String name) {
        return StrUtil.isNotBlank(name) && name.length() <= 20;
    }

    static boolean checkUserId(Long userId) {
        return userId != null && userId > 0;
    }
}
```

以下代码演示接口实现类如何继承默认方法，并直接调用接口静态方法。

文件位置：`src/main/java/io/github/atengk/jdk8/interfaceplus/UserServiceImpl.java`

```java
package io.github.atengk.jdk8.interfaceplus;

/**
 * 用户服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserServiceImpl implements UserService {

    @Override
    public UserInfo getById(Long userId) {
        if (!UserService.checkUserId(userId)) {
            return null;
        }

        return new UserInfo(userId, "Ateng");
    }
}
```

以下代码是用户信息对象，用于配合接口示例使用。

文件位置：`src/main/java/io/github/atengk/jdk8/interfaceplus/UserInfo.java`

```java
package io.github.atengk.jdk8.interfaceplus;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserInfo {

    private Long id;

    private String name;

    public UserInfo(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

接口默认方法适合以下场景：

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 老接口扩展   | 给已有接口新增能力，不强制所有实现类立即修改 |
| 轻量默认逻辑 | 提供简单校验、转换、兼容处理                 |
| 统一接口行为 | 为多个实现类提供相同的默认行为               |
| 框架扩展点   | 在接口层提供可选实现                         |

接口默认方法不适合以下场景：

| 场景         | 原因                             |
| ------------ | -------------------------------- |
| 复杂业务逻辑 | 接口不应承载过重业务             |
| 数据库访问   | 容易破坏分层职责                 |
| 远程调用     | 不利于异常处理和链路追踪         |
| 状态管理     | 接口不适合管理对象状态           |
| 替代抽象类   | 默认方法不能完全替代抽象类的职责 |

如果一个类实现了多个接口，而多个接口中存在相同签名的默认方法，实现类必须显式重写该方法，否则会产生编译错误。

### Optional 与 Stream API

`Optional` 和 `Stream API` 是 Java8 项目中使用频率非常高的两个特性。

`Optional` 用于表达一个值可能存在，也可能不存在。它适合用于方法返回值，用来减少多层 `null` 判断。不建议将 `Optional` 用作实体类字段、DTO 字段或方法参数。

`Stream API` 用于集合的声明式处理，适合完成过滤、映射、排序、去重、分组、统计和聚合等操作。

Optional 常见写法如下：

```java
String name = Optional.ofNullable(user)
        .map(User::getName)
        .orElse("未知用户");
```

Stream 常见写法如下：

```java
List<String> nameList = userList.stream()
        .filter(User::getEnabled)
        .map(User::getName)
        .collect(Collectors.toList());
```

以下代码演示 Optional 和 Stream 在用户数据处理中的组合使用。

文件位置：`src/main/java/io/github/atengk/jdk8/stream/UserStreamExample.java`

```java
package io.github.atengk.jdk8.stream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Optional 与 Stream API 项目示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserStreamExample {

    public static void main(String[] args) {
        List<User> userList = buildUserList();

        List<UserVO> userVoList = Optional.ofNullable(userList)
                .orElseGet(ArrayList::new)
                .stream()
                .filter(User::getEnabled)
                .filter(user -> StrUtil.isNotBlank(user.getName()))
                .sorted(Comparator.comparing(User::getAge))
                .map(user -> new UserVO(user.getId(), user.getName(), user.getAge()))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(userVoList)) {
            System.out.println("没有可展示的用户数据");
            return;
        }

        userVoList.forEach(System.out::println);

        Map<Integer, List<UserVO>> ageGroupMap = userVoList.stream()
                .collect(Collectors.groupingBy(UserVO::getAge));

        System.out.println("按年龄分组：" + ageGroupMap);
    }

    private static List<User> buildUserList() {
        List<User> userList = new ArrayList<>();
        userList.add(new User(1L, "Tom", 20, true));
        userList.add(new User(2L, "Jerry", 18, true));
        userList.add(new User(3L, "Alice", 20, true));
        userList.add(new User(4L, "", 22, true));
        userList.add(new User(5L, "Bob", 30, false));
        return userList;
    }

    /**
     * 用户实体
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class User {

        private Long id;

        private String name;

        private Integer age;

        private Boolean enabled;

        public User(Long id, String name, Integer age, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.enabled = enabled;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Boolean getEnabled() {
            return enabled;
        }
    }

    /**
     * 用户展示对象
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class UserVO {

        private Long id;

        private String name;

        private Integer age;

        public UserVO(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        @Override
        public String toString() {
            return "UserVO{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}
```

Optional 使用建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 推荐用于方法返回值     | 表达结果可能不存在                   |
| 不建议用于实体字段     | 可能影响序列化、ORM 映射和框架兼容性 |
| 不建议用于方法参数     | 参数校验应由调用方或校验逻辑处理     |
| 不建议直接调用 `get()` | 容易出现 `NoSuchElementException`    |
| 推荐使用 `orElseGet`   | 默认值构造成本较高时优先使用         |

Stream API 使用建议如下：

| 建议                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 适合集合处理                | 过滤、映射、排序、分组、统计             |
| 不适合复杂流程控制          | 多分支、多状态变更代码建议使用普通循环   |
| 避免修改外部变量            | 减少副作用和线程安全问题                 |
| 不要替代数据库查询          | 大数据量过滤、分页、聚合应优先交给数据库 |
| 谨慎使用 `parallelStream()` | 需要评估线程安全、数据量和 CPU 密集程度  |

### 新日期时间 API

JDK8 引入了新的日期时间 API，位于 `java.time` 包下。它解决了旧版 `Date`、`Calendar` 和 `SimpleDateFormat` 存在线程不安全、API 设计复杂、时区处理不清晰等问题。

常用类型如下：

| 类型                | 说明                   | 常见场景                 |
| ------------------- | ---------------------- | ------------------------ |
| `LocalDate`         | 只包含日期             | 生日、统计日期、业务日期 |
| `LocalTime`         | 只包含时间             | 每日固定时间             |
| `LocalDateTime`     | 日期和时间，不包含时区 | 创建时间、更新时间       |
| `ZonedDateTime`     | 带时区的日期时间       | 跨时区业务               |
| `Instant`           | 时间戳                 | 机器时间、日志时间       |
| `Duration`          | 时间间隔，偏时间       | 接口耗时、任务耗时       |
| `Period`            | 日期间隔，偏日期       | 天数、月数、年数计算     |
| `DateTimeFormatter` | 日期格式化器           | 字符串与日期转换         |

以下代码演示 Java8 新日期时间 API 的常见项目用法。

文件位置：`src/main/java/io/github/atengk/jdk8/time/JavaTimeExample.java`

```java
package io.github.atengk.jdk8.time;

import cn.hutool.core.util.StrUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 新日期时间 API 项目示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class JavaTimeExample {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        System.out.println("今天：" + today.format(DATE_FORMATTER));
        System.out.println("七天后：" + nextWeek.format(DATE_FORMATTER));

        String dateTimeText = "2026-05-13 10:30:00";
        LocalDateTime localDateTime = parseDateTime(dateTimeText);
        System.out.println("解析后的时间：" + formatDateTime(localDateTime));

        Date oldDate = toDate(localDateTime);
        LocalDateTime convertedDateTime = toLocalDateTime(oldDate);
        System.out.println("Date 转 LocalDateTime：" + formatDateTime(convertedDateTime));

        Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        System.out.println("任务间隔分钟数：" + duration.toMinutes());
    }

    public static LocalDateTime parseDateTime(String dateTimeText) {
        if (StrUtil.isBlank(dateTimeText)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeText, DATE_TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
```

日期时间 API 使用建议如下：

| 场景             | 推荐类型            |
| ---------------- | ------------------- |
| 只表示日期       | `LocalDate`         |
| 只表示时间       | `LocalTime`         |
| 表示业务日期时间 | `LocalDateTime`     |
| 涉及时区转换     | `ZonedDateTime`     |
| 表示机器时间戳   | `Instant`           |
| 计算时间间隔     | `Duration`          |
| 计算日期间隔     | `Period`            |
| 日期格式化       | `DateTimeFormatter` |

使用新日期时间 API 时需要注意：

- `LocalDateTime` 不包含时区，不适合直接表达跨时区绝对时间。
- `DateTimeFormatter` 是线程安全的，可以定义为静态常量。
- 不建议继续使用全局共享的 `SimpleDateFormat`。
- 数据库存储时间时，应统一数据库时区、服务端时区和日志时区。
- 接口传参建议统一使用明确格式，例如 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss`。
- 与老系统、数据库驱动或第三方 SDK 对接时，可以在边界层进行 `Date` 与 `LocalDateTime` 的转换。



## JDK9 到 JDK11 语法增强

JDK9 到 JDK11 是 Java 从传统类路径机制向模块化、简化语法和更现代化 API 过渡的重要阶段。这个阶段的语法变化不像 JDK8 那样彻底改变编码风格，但对大型项目结构、接口设计、资源关闭、局部变量声明和集合初始化都有明显改进。

对于 Java8 项目升级而言，JDK9 到 JDK11 的重点不是大量重写业务代码，而是理解新语法在工程结构、代码简化和 API 使用上的变化。

### 模块化系统

JDK9 引入了 Java Platform Module System，也称 JPMS。模块化系统的核心目标是将大型 Java 应用拆分为明确边界的模块，每个模块通过 `module-info.java` 声明依赖关系和对外暴露的包。

在传统 Java 项目中，所有依赖通常都放在 classpath 下，模块之间的访问边界不够清晰。模块化系统引入 module path 后，可以显式声明模块依赖，减少无意访问内部包的问题。

模块描述文件通常位于源码根目录下：

文件位置：`src/main/java/module-info.java`

```java
module io.github.atengk.demo {
    // 声明当前模块依赖 java.sql 模块
    requires java.sql;

    // 对外暴露用户相关包，其他模块可以访问该包中的 public 类型
    exports io.github.atengk.user;
}
```

模块化常用关键字如下：

| 关键字                  | 说明                 |
| ----------------------- | -------------------- |
| `module`                | 声明一个模块         |
| `requires`              | 声明依赖的其他模块   |
| `exports`               | 声明对外暴露的包     |
| `opens`                 | 声明允许反射访问的包 |
| `uses`                  | 声明使用某个服务接口 |
| `provides ... with ...` | 声明服务接口的实现类 |

在普通业务项目中，模块化系统更常见于以下场景：

| 场景                 | 说明                           |
| -------------------- | ------------------------------ |
| SDK 或基础组件开发   | 明确暴露 API 包，隐藏内部实现  |
| 大型单体拆分         | 按业务域拆分模块，控制访问边界 |
| 桌面应用或工具类应用 | 结合 jlink 裁剪运行时镜像      |
| 框架或中间件开发     | 管理服务接口与实现类关系       |

需要注意的是，Spring Boot 项目并不强制使用 JPMS。很多 Spring Boot 项目仍然采用传统 classpath 方式运行，因为 Spring 的自动配置、反射、代理和扫描机制与模块强封装之间需要额外适配。

模块化系统适合逐步理解，不建议在老项目升级时立即强行模块化。对于 JDK8 升级到 JDK11 或 JDK17 的普通后端项目，优先处理依赖兼容、编译插件、第三方库版本和运行时参数，再评估是否引入 `module-info.java`。

### 接口私有方法

JDK8 允许接口中定义默认方法和静态方法，但如果多个默认方法中存在重复逻辑，JDK8 无法在接口内部提取私有方法。JDK9 增加了接口私有方法，使接口可以在内部复用通用逻辑。

接口私有方法只能在接口内部被默认方法或静态方法调用，不能被实现类访问，也不能被外部调用。

以下代码演示在接口中使用私有方法复用参数校验逻辑。

文件位置：`src/main/java/io/github/atengk/jdk9/interfaceplus/UserCheckService.java`

```java
package io.github.atengk.jdk9.interfaceplus;

import cn.hutool.core.util.StrUtil;

/**
 * 用户校验服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserCheckService {

    default boolean checkCreateUser(String username, String mobile) {
        return checkUsername(username) && checkMobile(mobile);
    }

    default boolean checkUpdateUser(String username, String mobile) {
        return checkUsername(username) && checkMobile(mobile);
    }

    private boolean checkUsername(String username) {
        return StrUtil.isNotBlank(username) && username.length() <= 20;
    }

    private boolean checkMobile(String mobile) {
        return StrUtil.isNotBlank(mobile) && mobile.length() == 11;
    }
}
```

接口私有方法适合以下场景：

| 场景                     | 说明                     |
| ------------------------ | ------------------------ |
| 多个默认方法存在重复逻辑 | 可以提取为接口私有方法   |
| 接口中有少量通用校验     | 避免实现类重复编写       |
| 需要隐藏接口内部实现细节 | 私有方法不会暴露给实现类 |
| 保持接口默认方法简洁     | 让默认方法只表达主要流程 |

使用接口私有方法时需要注意：

- 私有方法不能被实现类重写。
- 私有方法只能在接口内部使用。
- 不要在接口中堆积复杂业务逻辑。
- 涉及数据库、缓存、远程调用的逻辑仍应放在 Service 实现类中。
- 接口私有方法更适合轻量级通用逻辑。

### 改进的 try-with-resources

JDK7 引入了 try-with-resources，用于自动关闭实现了 `AutoCloseable` 或 `Closeable` 接口的资源。JDK9 对 try-with-resources 做了改进，允许已经声明并且是 final 或等效 final 的资源变量直接放入 `try()` 中使用。

JDK8 写法通常需要在 `try()` 中重新声明资源变量：

```java
try (BufferedReader reader = new BufferedReader(new FileReader("user.txt"))) {
    String line = reader.readLine();
    System.out.println(line);
}
```

JDK9 之后，如果资源变量已经提前声明，并且后续没有被重新赋值，可以直接使用：

```java
BufferedReader reader = new BufferedReader(new FileReader("user.txt"));

try (reader) {
    String line = reader.readLine();
    System.out.println(line);
}
```

以下代码演示 JDK9 改进后的 try-with-resources 写法。

文件位置：`src/main/java/io/github/atengk/jdk9/resource/TryWithResourcesExample.java`

```java
package io.github.atengk.jdk9.resource;

import cn.hutool.core.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 改进的 try-with-resources 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TryWithResourcesExample {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = FileUtil.getReader("data/user.txt", "UTF-8");

        try (reader) {
            String line = reader.readLine();
            System.out.println("读取到的用户数据：" + line);
        }
    }
}
```

这种写法的好处是资源创建逻辑和资源使用逻辑可以拆开，代码更灵活，尤其适合资源对象需要先经过参数配置、包装或条件判断后再进入 `try` 代码块的场景。

使用时需要注意：

| 注意事项                          | 说明                             |
| --------------------------------- | -------------------------------- |
| 资源变量必须是 final 或等效 final | 进入 `try` 之前不能被重新赋值    |
| 资源仍会自动关闭                  | 退出 `try` 后自动调用 `close()`  |
| 不适合重复使用已关闭资源          | try 结束后资源已经关闭           |
| 异常处理仍需保留                  | 自动关闭不代表不需要处理 IO 异常 |

### 局部变量类型推断 var

JDK10 引入了局部变量类型推断 `var`。它允许编译器根据右侧表达式推断局部变量类型，从而减少重复类型声明。

传统写法：

```java
Map<String, List<String>> userRoleMap = new HashMap<String, List<String>>();
```

使用 `var` 后：

```java
var userRoleMap = new HashMap<String, List<String>>();
```

`var` 只能用于局部变量，不能用于成员变量、方法参数、方法返回值、构造方法参数或字段声明。

以下代码演示 `var` 在集合处理中的使用。

文件位置：`src/main/java/io/github/atengk/jdk10/var/VarExample.java`

```java
package io.github.atengk.jdk10.var;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 局部变量类型推断示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class VarExample {

    public static void main(String[] args) {
        var userList = new ArrayList<String>();
        userList.add("Tom");
        userList.add("Jerry");
        userList.add("Alice");

        var userRoleMap = new HashMap<String, List<String>>();
        userRoleMap.put("admin", userList);

        if (CollUtil.isEmpty(userList)) {
            System.out.println("用户列表为空");
            return;
        }

        for (var username : userList) {
            System.out.println("用户名：" + username);
        }

        for (var entry : userRoleMap.entrySet()) {
            System.out.println("角色：" + entry.getKey() + "，用户：" + entry.getValue());
        }
    }
}
```

`var` 使用建议如下：

| 建议                     | 说明                                        |
| ------------------------ | ------------------------------------------- |
| 右侧类型清晰时可以使用   | 例如 `var userList = new ArrayList<User>()` |
| 遍历集合时可以使用       | 例如 `for (var user : userList)`            |
| 泛型较长时可以使用       | 减少重复类型声明                            |
| 业务含义不清晰时不要使用 | 变量名和右侧表达式都不清晰会降低可读性      |
| 不要滥用在简单类型上     | `var count = 1` 不如 `int count = 1` 直观   |

不推荐写法：

```java
var result = service.getData();
```

如果 `getData()` 的返回类型不明显，使用 `var` 会让读者必须跳转到方法定义才能理解变量类型，降低代码可读性。

推荐写法：

```java
List<UserVO> userVoList = userService.getUserVoList();
```

`var` 的使用原则是：减少重复类型声明，而不是隐藏重要类型信息。

### Lambda 参数中的 var

JDK11 允许在 Lambda 表达式的参数中使用 `var`。这个特性主要用于保持 Lambda 参数声明风格一致，并支持在 Lambda 参数上添加注解。

普通 Lambda 写法：

```java
(name) -> name.trim()
```

JDK11 后可以写成：

```java
(var name) -> name.trim()
```

需要注意的是，同一个 Lambda 表达式中的参数要么全部使用 `var`，要么全部不使用，不能混用。

错误写法：

```java
(var name, age) -> name + age
```

正确写法：

```java
(var name, var age) -> name + age
```

以下代码演示 Lambda 参数中使用 `var`。

文件位置：`src/main/java/io/github/atengk/jdk11/lambda/LambdaVarExample.java`

```java
package io.github.atengk.jdk11.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lambda 参数 var 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class LambdaVarExample {

    public static void main(String[] args) {
        List<String> usernameList = Arrays.asList(" Tom ", " Jerry ", "", " Alice ");

        List<String> resultList = usernameList.stream()
                .filter((var username) -> StrUtil.isNotBlank(username))
                .map((var username) -> username.trim())
                .collect(Collectors.toList());

        resultList.forEach(System.out::println);
    }
}
```

在普通项目中，Lambda 参数中的 `var` 使用频率不高，因为大多数情况下省略参数类型更简洁。

更常见写法：

```java
List<String> resultList = usernameList.stream()
        .filter(username -> StrUtil.isNotBlank(username))
        .map(username -> username.trim())
        .collect(Collectors.toList());
```

Lambda 参数 `var` 适合以下场景：

| 场景                       | 说明                       |
| -------------------------- | -------------------------- |
| 需要给 Lambda 参数添加注解 | 例如参数非空注解、校验注解 |
| 希望参数声明风格统一       | 多参数 Lambda 中保持一致   |
| 团队规范明确要求           | 按统一编码规范使用         |

在业务代码中，如果没有注解需求，不建议为了使用新语法而强行给 Lambda 参数加 `var`。

### 集合工厂方法

JDK9 引入了集合工厂方法，用于快速创建不可变集合。常用方法包括 `List.of()`、`Set.of()`、`Map.of()` 和 `Map.ofEntries()`。

传统写法：

```java
List<String> roleList = new ArrayList<>();
roleList.add("admin");
roleList.add("user");
roleList.add("guest");
```

JDK9 后可以写成：

```java
List<String> roleList = List.of("admin", "user", "guest");
```

以下代码演示集合工厂方法的常见用法。

文件位置：`src/main/java/io/github/atengk/jdk9/collection/CollectionFactoryExample.java`

```java
package io.github.atengk.jdk9.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合工厂方法示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CollectionFactoryExample {

    public static void main(String[] args) {
        List<String> roleList = List.of("admin", "user", "guest");

        Set<String> permissionSet = Set.of("user:add", "user:update", "user:delete");

        Map<String, String> statusMap = Map.of(
                "0", "禁用",
                "1", "启用",
                "2", "锁定"
        );

        if (CollUtil.isEmpty(roleList)) {
            System.out.println("角色列表为空");
            return;
        }

        roleList.forEach(role -> System.out.println("角色：" + role));
        permissionSet.forEach(permission -> System.out.println("权限：" + permission));
        statusMap.forEach((code, name) -> System.out.println("状态：" + code + "：" + name));
    }
}
```

集合工厂方法创建的是不可变集合，不能进行新增、删除或修改操作。

以下代码会抛出 `UnsupportedOperationException`：

```java
List<String> roleList = List.of("admin", "user");
roleList.add("guest");
```

集合工厂方法使用建议如下：

| 场景                | 建议                     |
| ------------------- | ------------------------ |
| 固定配置项          | 推荐使用                 |
| 枚举式字符串列表    | 推荐使用                 |
| 单元测试输入数据    | 推荐使用                 |
| 需要动态增删的集合  | 不建议直接使用           |
| 集合元素可能为 null | 不允许，创建时会抛出异常 |

如果需要基于工厂方法创建可变集合，可以再包装一层：

```java
List<String> roleList = new ArrayList<>(List.of("admin", "user", "guest"));
```

集合工厂方法适合替代大量初始化代码，使静态数据、测试数据和固定配置项更加简洁。

## JDK12 到 JDK17 现代语法成型

JDK12 到 JDK17 是现代 Java 语法逐步稳定成型的阶段。这个阶段引入并稳定了 switch 表达式、文本块、instanceof 模式匹配、Record 记录类、Sealed 密封类等重要特性。

这些语法特性对项目代码结构影响明显，尤其适合用于简化条件分支、构造多行字符串、减少 DTO 样板代码、增强类型判断可读性，以及控制继承体系边界。

JDK17 是长期支持版本，也是很多企业从 JDK8 升级后的目标版本。因此，掌握 JDK12 到 JDK17 的语法特性，对老项目升级和新项目开发都有较高价值。

### Switch 表达式

Switch 表达式最早在 JDK12 作为预览特性引入，经过多个版本迭代后在 JDK14 成为正式特性。它允许 `switch` 直接作为表达式返回值，并支持箭头语法 `->`，减少传统 `switch` 中容易遗漏 `break` 的问题。

传统 `switch` 写法：

```java
String statusName;

switch (status) {
    case 0:
        statusName = "禁用";
        break;
    case 1:
        statusName = "启用";
        break;
    default:
        statusName = "未知";
}
```

Switch 表达式写法：

```java
String statusName = switch (status) {
    case 0 -> "禁用";
    case 1 -> "启用";
    default -> "未知";
};
```

以下代码演示 Switch 表达式在订单状态转换中的使用。

文件位置：`src/main/java/io/github/atengk/jdk14/switchcase/SwitchExpressionExample.java`

```java
package io.github.atengk.jdk14.switchcase;

/**
 * Switch 表达式示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class SwitchExpressionExample {

    public static void main(String[] args) {
        Integer orderStatus = 1;

        String statusName = getOrderStatusName(orderStatus);
        System.out.println("订单状态：" + statusName);
    }

    public static String getOrderStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
```

如果分支中需要多行逻辑，可以使用代码块和 `yield` 返回结果。

```java
String statusName = switch (status) {
    case 1 -> {
        System.out.println("订单已支付，可以进入发货流程");
        yield "已支付";
    }
    case 2 -> {
        System.out.println("订单已发货，需要等待用户确认");
        yield "已发货";
    }
    default -> "未知";
};
```

Switch 表达式适合以下场景：

| 场景             | 说明                                     |
| ---------------- | ---------------------------------------- |
| 状态码转换       | 将订单状态、用户状态、审批状态转换为描述 |
| 枚举分支处理     | 根据枚举值返回不同结果                   |
| 简单策略选择     | 根据类型选择处理结果                     |
| 替代简单 if-else | 分支固定且互斥时更清晰                   |

使用建议：

- 分支返回值类型应保持一致。
- 分支逻辑较复杂时，不要把大量业务代码堆在 `switch` 中。
- 对枚举使用 switch 表达式时，应尽量覆盖所有枚举值。
- 复杂策略分支可以考虑使用策略模式，而不是无限扩展 switch。

### 文本块

文本块是 JDK15 正式引入的多行字符串语法。它使用三个双引号 `"""` 表示字符串内容，适合编写 SQL、JSON、HTML、XML 等多行文本。

传统字符串拼接写法：

```java
String sql = "select id, username, mobile " +
        "from sys_user " +
        "where status = 1";
```

文本块写法：

```java
String sql = """
        select id, username, mobile
        from sys_user
        where status = 1
        """;
```

以下代码演示文本块在 SQL 和 JSON 字符串中的使用。

文件位置：`src/main/java/io/github/atengk/jdk15/textblock/TextBlockExample.java`

```java
package io.github.atengk.jdk15.textblock;

/**
 * 文本块示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TextBlockExample {

    public static void main(String[] args) {
        String userQuerySql = """
                select
                    id,
                    username,
                    mobile,
                    status
                from sys_user
                where status = 1
                  and deleted = 0
                order by id desc
                """;

        String userJson = """
                {
                  "id": 1,
                  "username": "Ateng",
                  "mobile": "13800138000",
                  "status": 1
                }
                """;

        System.out.println("用户查询 SQL：");
        System.out.println(userQuerySql);

        System.out.println("用户 JSON：");
        System.out.println(userJson);
    }
}
```

文本块适合以下场景：

| 场景     | 说明                        |
| -------- | --------------------------- |
| SQL      | 多字段查询、复杂 where 条件 |
| JSON     | 接口示例、单元测试数据      |
| XML      | 配置模板、报文模板          |
| HTML     | 邮件模板、简单页面片段      |
| 日志模板 | 多行调试信息                |

使用文本块时需要注意：

- 文本块会保留换行结构。
- 缩进会根据结束的 `"""` 自动处理。
- 不适合存放大量复杂模板，复杂模板仍建议使用模板引擎。
- SQL 拼接仍需注意 SQL 注入问题，不能因为使用文本块就直接拼接用户输入。
- 在 MyBatis XML 中已有 SQL 管理方式时，不必强行将 SQL 放到 Java 文本块中。

### instanceof 模式匹配

JDK16 正式引入 instanceof 模式匹配。它允许在 `instanceof` 判断类型成功后，直接声明并使用目标类型变量，减少强制类型转换代码。

传统写法：

```java
if (obj instanceof String) {
    String text = (String) obj;
    System.out.println(text.trim());
}
```

模式匹配写法：

```java
if (obj instanceof String text) {
    System.out.println(text.trim());
}
```

以下代码演示 instanceof 模式匹配在参数解析中的使用。

文件位置：`src/main/java/io/github/atengk/jdk16/pattern/InstanceofPatternExample.java`

```java
package io.github.atengk.jdk16.pattern;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/**
 * instanceof 模式匹配示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class InstanceofPatternExample {

    public static void main(String[] args) {
        printValue("  Java17  ");
        printValue(100);
        printValue(new BigDecimal("99.99"));
        printValue(null);
    }

    public static void printValue(Object value) {
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            System.out.println("字符串：" + text.trim());
            return;
        }

        if (value instanceof Integer number) {
            System.out.println("整数：" + number);
            return;
        }

        if (value instanceof BigDecimal amount) {
            System.out.println("金额：" + amount);
            return;
        }

        System.out.println("未知类型：" + value);
    }
}
```

instanceof 模式匹配适合以下场景：

| 场景            | 说明                               |
| --------------- | ---------------------------------- |
| Object 参数解析 | 根据实际类型处理不同逻辑           |
| 事件对象处理    | 判断不同事件类型                   |
| 异常对象处理    | 根据异常子类做分支处理             |
| 框架扩展点      | 对外暴露统一接口，内部判断具体实现 |

使用建议：

- 适合替代 `instanceof + 强制类型转换`。
- 不要用大量 instanceof 替代合理的多态设计。
- 类型分支过多时，应考虑策略模式、访问者模式或多态方法。
- 模式变量只在判断成立的作用域内有效。

### Record 记录类

Record 是 JDK16 正式引入的特性，主要用于简化不可变数据载体类。它非常适合表示 DTO、VO、查询条件、接口返回结构、配置项等只承载数据、不包含复杂业务状态的对象。

传统 Java Bean 通常需要编写字段、构造方法、getter、`equals`、`hashCode` 和 `toString`。Record 可以自动生成这些样板代码。

传统写法需要大量样板代码：

```java
public class UserVO {

    private final Long id;

    private final String username;

    public UserVO(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
```

Record 写法：

```java
public record UserVO(Long id, String username) {
}
```

以下代码演示 Record 在接口返回对象中的使用。

文件位置：`src/main/java/io/github/atengk/jdk16/record/UserVO.java`

```java
package io.github.atengk.jdk16.record;

/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserVO(Long id, String username, String mobile) {
}
```

Record 也可以定义紧凑构造方法，用于参数校验和默认值处理。

文件位置：`src/main/java/io/github/atengk/jdk16/record/UserQuery.java`

```java
package io.github.atengk.jdk16.record;

import cn.hutool.core.util.StrUtil;

/**
 * 用户查询条件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserQuery(String username, Integer status) {

    public UserQuery {
        if (StrUtil.isNotBlank(username)) {
            username = username.trim();
        }

        if (status == null) {
            status = 1;
        }
    }
}
```

以下代码演示 Record 的使用方式。

文件位置：`src/main/java/io/github/atengk/jdk16/record/RecordExample.java`

```java
package io.github.atengk.jdk16.record;

/**
 * Record 使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class RecordExample {

    public static void main(String[] args) {
        UserVO userVO = new UserVO(1L, "Ateng", "13800138000");
        UserQuery userQuery = new UserQuery("  Tom  ", null);

        System.out.println("用户 ID：" + userVO.id());
        System.out.println("用户名：" + userVO.username());
        System.out.println("查询名称：" + userQuery.username());
        System.out.println("查询状态：" + userQuery.status());
    }
}
```

Record 适合以下场景：

| 场景     | 说明                   |
| -------- | ---------------------- |
| DTO      | 接口入参、出参对象     |
| VO       | 页面展示对象           |
| 查询条件 | 简单查询参数封装       |
| 配置对象 | 不需要修改的配置项     |
| 聚合结果 | 分组统计、临时结果对象 |

Record 不适合以下场景：

| 场景                   | 原因                                                 |
| ---------------------- | ---------------------------------------------------- |
| JPA 实体类             | ORM 框架通常需要无参构造、可变字段、代理能力         |
| 复杂领域对象           | 领域对象通常包含行为和状态变化                       |
| 需要频繁修改字段的对象 | Record 默认不可变                                    |
| 需要继承其他类的对象   | Record 隐式继承 `java.lang.Record`，不能再继承其他类 |

在 Spring Boot 项目中，Record 可以用于简单 DTO，但在使用 JSON 反序列化、参数校验、框架代理时，需要确认所用框架版本是否完整支持 Record。

### Sealed 密封类

Sealed Class 是 JDK17 正式引入的特性。它允许类或接口明确限制哪些类可以继承或实现自己，从而让继承体系更加可控。

在传统 Java 中，一个非 final 的 public 类可以被任意类继承。对于领域模型、指令类型、事件类型等需要固定子类型集合的场景，这种开放继承可能带来维护风险。Sealed Class 可以显式声明允许的子类。

以下代码演示使用密封接口限制订单指令类型。

文件位置：`src/main/java/io/github/atengk/jdk17/sealed/OrderCommand.java`

```java
package io.github.atengk.jdk17.sealed;

/**
 * 订单指令
 *
 * @author Ateng
 * @since 2026-05-13
 */
public sealed interface OrderCommand permits CreateOrderCommand, CancelOrderCommand {
}
```

以下代码定义创建订单指令。

文件位置：`src/main/java/io/github/atengk/jdk17/sealed/CreateOrderCommand.java`

```java
package io.github.atengk.jdk17.sealed;

import java.math.BigDecimal;

/**
 * 创建订单指令
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CreateOrderCommand(Long userId, BigDecimal amount) implements OrderCommand {
}
```

以下代码定义取消订单指令。

文件位置：`src/main/java/io/github/atengk/jdk17/sealed/CancelOrderCommand.java`

```java
package io.github.atengk.jdk17.sealed;

/**
 * 取消订单指令
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CancelOrderCommand(Long orderId, String reason) implements OrderCommand {
}
```

以下代码演示如何处理受限制的订单指令类型。

文件位置：`src/main/java/io/github/atengk/jdk17/sealed/SealedClassExample.java`

```java
package io.github.atengk.jdk17.sealed;

import java.math.BigDecimal;

/**
 * 密封类使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class SealedClassExample {

    public static void main(String[] args) {
        handleCommand(new CreateOrderCommand(1L, new BigDecimal("99.90")));
        handleCommand(new CancelOrderCommand(1001L, "用户主动取消"));
    }

    public static void handleCommand(OrderCommand command) {
        if (command instanceof CreateOrderCommand createOrderCommand) {
            System.out.println("创建订单，用户ID：" + createOrderCommand.userId()
                    + "，金额：" + createOrderCommand.amount());
            return;
        }

        if (command instanceof CancelOrderCommand cancelOrderCommand) {
            System.out.println("取消订单，订单ID：" + cancelOrderCommand.orderId()
                    + "，原因：" + cancelOrderCommand.reason());
            return;
        }

        throw new IllegalArgumentException("不支持的订单指令类型");
    }
}
```

Sealed Class 的子类必须明确声明自己的继承策略：

| 子类修饰符   | 说明               |
| ------------ | ------------------ |
| `final`      | 不允许继续被继承   |
| `sealed`     | 继续限制自己的子类 |
| `non-sealed` | 重新开放继承       |

Record 默认是 final，因此可以直接作为 sealed interface 的允许实现类型。

Sealed Class 适合以下场景：

| 场景           | 说明                                   |
| -------------- | -------------------------------------- |
| 固定类型集合   | 订单指令、支付结果、事件类型           |
| 领域模型约束   | 限制领域对象的继承边界                 |
| 框架内部扩展点 | 只允许指定实现类                       |
| 状态机建模     | 限定状态类型范围                       |
| 编译期增强检查 | 与 switch 模式匹配配合使用时效果更明显 |

使用建议：

- 子类型集合稳定时适合使用。
- 插件化扩展场景不适合使用 sealed，因为它限制外部扩展。
- 密封类适合表达“我知道所有可能子类型”的模型。
- 普通业务接口如果需要被第三方或其他模块自由实现，不应使用 sealed。

### Helpful NullPointerException

Helpful NullPointerException 是 JDK14 引入的运行时诊断增强。它不是新的语法特性，但对项目排查空指针异常非常有帮助。

在旧版本中，空指针异常通常只提示某一行出现了 `NullPointerException`，但不会明确说明这一行中的哪个变量为 `null`。当一行代码中存在链式调用时，排查成本较高。

示例代码如下：

文件位置：`src/main/java/io/github/atengk/jdk14/npe/HelpfulNullPointerExceptionExample.java`

```java
package io.github.atengk.jdk14.npe;

/**
 * Helpful NullPointerException 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class HelpfulNullPointerExceptionExample {

    public static void main(String[] args) {
        User user = new User(null);

        String cityName = user.getAddress().getCity().getName();

        System.out.println(cityName);
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class User {

        private final Address address;

        public User(Address address) {
            this.address = address;
        }

        public Address getAddress() {
            return address;
        }
    }

    /**
     * 地址信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class Address {

        private final City city;

        public Address(City city) {
            this.city = city;
        }

        public City getCity() {
            return city;
        }
    }

    /**
     * 城市信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class City {

        private final String name;

        public City(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
```

在支持 Helpful NullPointerException 的 JDK 版本中，异常信息会更明确地指出链式调用中哪一部分为 `null`，例如可以定位到 `user.getAddress()` 返回了 `null`。

Helpful NullPointerException 的价值主要体现在以下场景：

| 场景         | 说明                                     |
| ------------ | ---------------------------------------- |
| 链式调用排错 | 明确哪一段调用结果为 null                |
| 生产日志分析 | 降低空指针问题定位成本                   |
| 老项目升级   | 无需修改业务代码即可提升诊断能力         |
| 接口联调     | 快速定位入参、对象字段或返回值为空的位置 |

需要注意的是，Helpful NullPointerException 只能提升异常诊断能力，不能替代空值设计和参数校验。项目中仍应通过以下方式减少空指针问题：

- 对外部请求参数进行统一校验。
- 对数据库查询结果进行空值判断。
- 对远程调用返回值进行防御式处理。
- 对可为空的返回结果使用 Optional 表达。
- 避免过长的链式调用。
- 在关键业务节点补充明确的异常信息。



## JDK18 到 JDK21 语法与并发升级

JDK18 到 JDK21 是 Java 现代语法和并发模型继续增强的重要阶段。这个阶段的重点不只是语法简化，还包括数据解构、模式匹配、集合顺序访问、轻量级线程、结构化并发和线程上下文传递等能力。JDK21 中 Record Patterns、Pattern Matching for switch 成为正式语言特性，Virtual Threads 也成为正式特性；Structured Concurrency 和 Scoped Values 在 JDK21 仍属于 Preview API，生产使用需要谨慎评估。([OpenJDK](https://openjdk.org/jeps/440?utm_source=chatgpt.com))

### Record Pattern

Record Pattern 是 JDK21 正式引入的记录模式特性。它允许在 `instanceof` 或 `switch` 中直接解构 Record 对象，从而减少手动调用访问器方法的样板代码。Record Pattern 可以嵌套使用，适合处理由多个 Record 组成的数据结构。([OpenJDK](https://openjdk.org/jeps/440?utm_source=chatgpt.com))

传统写法通常需要先判断类型，再调用 Record 的访问器方法：

```java
if (obj instanceof UserAddress address) {
    Long userId = address.userId();
    AddressInfo addressInfo = address.addressInfo();
    String city = addressInfo.city();
}
```

使用 Record Pattern 后，可以在模式匹配时直接解构数据：

```java
if (obj instanceof UserAddress(Long userId, AddressInfo(String city, String detail))) {
    System.out.println("用户ID：" + userId + "，城市：" + city + "，地址：" + detail);
}
```

以下代码演示 Record Pattern 在用户地址数据解析中的使用。

文件位置：`src/main/java/io/github/atengk/jdk21/recordpattern/RecordPatternExample.java`

```java
package io.github.atengk.jdk21.recordpattern;

import cn.hutool.core.util.StrUtil;

/**
 * Record Pattern 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class RecordPatternExample {

    public static void main(String[] args) {
        Object data = new UserAddress(1001L, new AddressInfo("杭州", "西湖区文三路"));

        printAddress(data);
    }

    public static void printAddress(Object data) {
        if (data instanceof UserAddress(Long userId, AddressInfo(String city, String detail))) {
            if (StrUtil.isBlank(city) || StrUtil.isBlank(detail)) {
                System.out.println("地址信息不完整");
                return;
            }

            System.out.println("用户ID：" + userId);
            System.out.println("城市：" + city);
            System.out.println("详细地址：" + detail);
            return;
        }

        System.out.println("不支持的数据类型");
    }

    /**
     * 用户地址
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserAddress(Long userId, AddressInfo addressInfo) {
    }

    /**
     * 地址信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record AddressInfo(String city, String detail) {
    }
}
```

Record Pattern 适合以下场景：

| 场景            | 说明                                    |
| --------------- | --------------------------------------- |
| Record 数据解构 | 从 Record 中直接提取组件值              |
| 嵌套数据解析    | 处理 Record 嵌套 Record 的数据结构      |
| 事件对象处理    | 根据事件类型拆解内部字段                |
| DTO 临时转换    | 简化读取不可变数据对象的代码            |
| switch 模式匹配 | 与 Pattern Matching for switch 配合使用 |

使用建议：

- Record Pattern 适合处理不可变数据结构。
- 嵌套层级不宜过深，过深会降低可读性。
- 如果解构后仍需要大量业务判断，应提取为独立方法。
- 与 Record 记录类一起使用效果更好。
- 不建议为了使用 Record Pattern 而强行把所有 Java Bean 改成 Record。

### Pattern Matching for switch

Pattern Matching for switch 是 JDK21 正式引入的模式匹配增强。它允许 `switch` 根据对象类型、Record Pattern、`null` 和守卫条件进行分支匹配，从而让复杂类型判断更加清晰。([OpenJDK](https://openjdk.org/jeps/441?utm_source=chatgpt.com))

传统写法通常需要多层 `if instanceof`：

```java
if (result instanceof SuccessResult successResult) {
    return "成功：" + successResult.message();
}

if (result instanceof FailResult failResult) {
    return "失败：" + failResult.reason();
}
```

使用 Pattern Matching for switch 后，可以统一写成：

```java
String message = switch (result) {
    case SuccessResult success -> "成功：" + success.message();
    case FailResult fail -> "失败：" + fail.reason();
    case null -> "结果为空";
};
```

以下代码演示 Pattern Matching for switch 在支付结果处理中的使用。

文件位置：`src/main/java/io/github/atengk/jdk21/patternswitch/PatternSwitchExample.java`

```java
package io.github.atengk.jdk21.patternswitch;

import java.math.BigDecimal;

/**
 * Pattern Matching for switch 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PatternSwitchExample {

    public static void main(String[] args) {
        PayResult result = new PaySuccess("P202605130001", new BigDecimal("99.90"));

        String message = buildMessage(result);
        System.out.println(message);
    }

    public static String buildMessage(PayResult result) {
        return switch (result) {
            case PaySuccess success when success.amount().compareTo(BigDecimal.ZERO) > 0 ->
                    "支付成功，流水号：" + success.payNo() + "，金额：" + success.amount();
            case PaySuccess success ->
                    "支付成功，但金额异常，流水号：" + success.payNo();
            case PayFail fail ->
                    "支付失败，原因：" + fail.reason();
            case PayProcessing processing ->
                    "支付处理中，请稍后查询，业务号：" + processing.bizNo();
            case null ->
                    "支付结果为空";
        };
    }

    /**
     * 支付结果
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public sealed interface PayResult permits PaySuccess, PayFail, PayProcessing {
    }

    /**
     * 支付成功
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record PaySuccess(String payNo, BigDecimal amount) implements PayResult {
    }

    /**
     * 支付失败
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record PayFail(String reason) implements PayResult {
    }

    /**
     * 支付处理中
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record PayProcessing(String bizNo) implements PayResult {
    }
}
```

Pattern Matching for switch 适合以下场景：

| 场景            | 说明                                 |
| --------------- | ------------------------------------ |
| 类型分支处理    | 根据对象实际类型执行不同逻辑         |
| 密封类分支      | 与 Sealed Class 配合，增强穷尽性检查 |
| Record 数据解构 | 与 Record Pattern 配合读取字段       |
| 状态对象处理    | 处理成功、失败、处理中等有限状态     |
| 事件分发        | 根据事件类型选择不同处理流程         |

使用建议：

- 分支类型固定时，优先结合 sealed interface 使用。
- 分支中业务逻辑较多时，应提取方法。
- `case null` 可用于显式处理空值。
- `when` 守卫条件适合补充简单判断。
- 类型分支过多时，应评估是否需要策略模式或多态设计。

### Sequenced Collections

Sequenced Collections 是 JDK21 引入的集合框架增强。它新增了 `SequencedCollection`、`SequencedSet` 和 `SequencedMap` 等接口，用于统一表达具有明确遍历顺序的集合，并提供访问第一个元素、最后一个元素和反向视图的统一 API。([OpenJDK](https://openjdk.org/jeps/431?utm_source=chatgpt.com))

在 JDK21 之前，不同集合访问首尾元素的方式不统一。例如 `List` 可以用索引访问，`Deque` 有 `getFirst()` 和 `getLast()`，而 `LinkedHashSet` 没有统一的首尾访问方法。

JDK21 后，可以使用统一方法：

```java
String first = userList.getFirst();
String last = userList.getLast();
List<String> reversed = userList.reversed();
```

以下代码演示 Sequenced Collections 的常见用法。

文件位置：`src/main/java/io/github/atengk/jdk21/collection/SequencedCollectionExample.java`

```java
package io.github.atengk.jdk21.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.SequencedSet;

/**
 * Sequenced Collections 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class SequencedCollectionExample {

    public static void main(String[] args) {
        List<String> userList = List.of("Tom", "Jerry", "Alice");

        if (CollUtil.isEmpty(userList)) {
            System.out.println("用户列表为空");
            return;
        }

        System.out.println("第一个用户：" + userList.getFirst());
        System.out.println("最后一个用户：" + userList.getLast());
        System.out.println("反向用户列表：" + userList.reversed());

        SequencedSet<String> roleSet = new LinkedHashSet<>();
        roleSet.addFirst("admin");
        roleSet.addLast("user");
        roleSet.addLast("guest");

        System.out.println("第一个角色：" + roleSet.getFirst());
        System.out.println("最后一个角色：" + roleSet.getLast());
        System.out.println("反向角色：" + roleSet.reversed());

        SequencedMap<String, String> statusMap = new LinkedHashMap<>();
        statusMap.putFirst("0", "禁用");
        statusMap.putLast("1", "启用");
        statusMap.putLast("2", "锁定");

        System.out.println("第一个状态：" + statusMap.firstEntry());
        System.out.println("最后一个状态：" + statusMap.lastEntry());
        System.out.println("反向状态：" + statusMap.reversed());
    }
}
```

Sequenced Collections 适合以下场景：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 有序列表处理 | 统一访问第一个、最后一个元素                           |
| 有序 Set     | 例如菜单权限、角色顺序、标签顺序                       |
| 有序 Map     | 例如状态映射、字段顺序、配置项顺序                     |
| 反向遍历     | 使用 `reversed()` 获取反向视图                         |
| 替代特殊写法 | 减少 `list.get(0)`、`list.get(list.size() - 1)` 等写法 |

使用建议：

- 有明确顺序语义时优先使用。
- 空集合调用 `getFirst()`、`getLast()` 前需要先判断。
- `reversed()` 返回的是反向视图，不一定是新集合副本。
- 对无序集合不要强行使用顺序语义。
- 在接口返回值中可以使用更抽象的 `SequencedCollection` 表达顺序需求。

### Virtual Threads

Virtual Threads 是 JDK21 正式引入的虚拟线程特性。虚拟线程是由 JDK 管理的轻量级线程，适合高并发、IO 密集型任务，可以让传统“一个请求一个线程”的编程模型在更高并发场景下继续保持可读性。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

传统线程池写法：

```java
ExecutorService executorService = Executors.newFixedThreadPool(100);
```

虚拟线程写法：

```java
ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
```

以下代码演示使用虚拟线程并发处理多个用户任务。

文件位置：`src/main/java/io/github/atengk/jdk21/virtualthread/VirtualThreadExample.java`

```java
package io.github.atengk.jdk21.virtualthread;

import cn.hutool.core.collection.CollUtil;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual Threads 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class VirtualThreadExample {

    public static void main(String[] args) {
        List<Long> userIdList = List.of(1001L, 1002L, 1003L);

        if (CollUtil.isEmpty(userIdList)) {
            System.out.println("用户ID列表为空");
            return;
        }

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long userId : userIdList) {
                executorService.submit(() -> queryUser(userId));
            }
        }
    }

    private static void queryUser(Long userId) {
        try {
            Thread.sleep(Duration.ofMillis(300));
            System.out.println("查询用户信息，用户ID：" + userId + "，线程：" + Thread.currentThread());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("用户查询任务被中断，用户ID：" + userId);
        }
    }
}
```

虚拟线程适合以下场景：

| 场景           | 说明                                |
| -------------- | ----------------------------------- |
| IO 密集型接口  | 数据库、Redis、HTTP、RPC 等阻塞调用 |
| 高并发请求处理 | 每个请求使用独立虚拟线程            |
| 批量远程调用   | 并发查询多个外部服务                |
| 任务编排       | 与结构化并发配合组织子任务          |
| 简化异步代码   | 减少复杂 CompletableFuture 链式编排 |

虚拟线程不适合以下场景：

| 场景                              | 原因                                    |
| --------------------------------- | --------------------------------------- |
| CPU 密集型计算                    | CPU 资源有限，虚拟线程不能突破 CPU 上限 |
| 长时间持有锁                      | 可能影响调度和吞吐                      |
| 依赖线程池复用 ThreadLocal 的代码 | 虚拟线程不建议被池化                    |
| 需要严格线程绑定的场景            | 虚拟线程可能在不同平台线程上挂载和卸载  |
| 对底层库兼容性未知的系统          | 需要压测和验证阻塞点是否友好            |

使用建议：

- 虚拟线程适合 IO 密集型业务，不是所有并发问题的通用解法。
- 不要池化虚拟线程，推荐每个任务创建一个虚拟线程。
- 注意数据库连接池、HTTP 连接池等外部资源仍然可能成为瓶颈。
- 避免在虚拟线程中长期持有 `synchronized` 锁。
- 引入虚拟线程前需要配合压测、日志和监控验证效果。

### Structured Concurrency

Structured Concurrency 是结构化并发 API。它把一组相关子任务作为一个整体进行管理，使任务创建、等待、失败处理和取消逻辑都限定在清晰的代码作用域中。JDK21 中该 API 是 Preview API，JDK25 中仍是第五次预览，说明它的 API 设计仍可能继续变化，生产核心代码不建议直接依赖。([OpenJDK](https://openjdk.org/jeps/453?utm_source=chatgpt.com))

传统并发代码中，多个 `Future` 往往分散创建和等待，异常处理也容易分散。结构化并发希望把这些子任务组织成一个明确的任务作用域。

以下示例基于 JDK21 Preview API，编译和运行时需要开启 Preview。

文件位置：`src/main/java/io/github/atengk/jdk21/structured/StructuredConcurrencyExample.java`

```java
package io.github.atengk.jdk21.structured;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

/**
 * Structured Concurrency 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class StructuredConcurrencyExample {

    public static void main(String[] args) throws Exception {
        UserProfile userProfile = queryUserProfile(1001L);
        System.out.println(userProfile);
    }

    public static UserProfile queryUserProfile(Long userId) throws InterruptedException, ExecutionException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            StructuredTaskScope.Subtask<UserInfo> userTask = scope.fork(() -> queryUser(userId));
            StructuredTaskScope.Subtask<AccountInfo> accountTask = scope.fork(() -> queryAccount(userId));

            scope.join();
            scope.throwIfFailed();

            return new UserProfile(userTask.get(), accountTask.get());
        }
    }

    private static UserInfo queryUser(Long userId) {
        return new UserInfo(userId, "Ateng");
    }

    private static AccountInfo queryAccount(Long userId) {
        return new AccountInfo(userId, "正常");
    }

    /**
     * 用户资料
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserProfile(UserInfo userInfo, AccountInfo accountInfo) {
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserInfo(Long userId, String username) {
    }

    /**
     * 账户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record AccountInfo(Long userId, String status) {
    }
}
```

JDK21 编译和运行命令如下：

```bash
javac --release 21 --enable-preview src/main/java/io/github/atengk/jdk21/structured/StructuredConcurrencyExample.java
java --enable-preview io.github.atengk.jdk21.structured.StructuredConcurrencyExample
```

Structured Concurrency 适合以下场景：

| 场景             | 说明                                 |
| ---------------- | ------------------------------------ |
| 并行查询多个服务 | 用户信息、账户信息、权限信息并发查询 |
| 任务整体失败处理 | 任一子任务失败时取消其他任务         |
| 请求级任务编排   | 一个请求下有多个并发子任务           |
| 提升可观测性     | 子任务归属于同一个父任务作用域       |
| 与虚拟线程配合   | 更适合轻量级并发任务管理             |

使用建议：

- JDK21 到 JDK25 期间该 API 仍处于 Preview 状态。
- 生产核心业务不建议直接依赖 Preview API。
- 如果要试用，应限制在实验模块或内部工具中。
- 需要通过 `--enable-preview` 编译和运行。
- 升级 JDK 时应重点检查 API 变化。

### Scoped Values

Scoped Values 用于在线程内以及结构化子线程之间安全、高效地共享不可变上下文数据。它可以理解为一种有明确作用域边界的上下文传递机制。JDK21 中 Scoped Values 是 Preview API，JDK25 中已经成为正式特性。([OpenJDK](https://openjdk.org/jeps/446?utm_source=chatgpt.com))

与 `ThreadLocal` 相比，Scoped Values 更强调不可变、作用域可见和生命周期清晰，尤其适合与虚拟线程、结构化并发配合使用。

以下代码演示使用 Scoped Values 传递请求上下文。

文件位置：`src/main/java/io/github/atengk/jdk21/scoped/ScopedValueExample.java`

```java
package io.github.atengk.jdk21.scoped;

/**
 * Scoped Values 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ScopedValueExample {

    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

    public static void main(String[] args) {
        RequestContext context = new RequestContext("REQ-202605130001", 1001L);

        ScopedValue.where(REQUEST_CONTEXT, context)
                .run(ScopedValueExample::handleRequest);
    }

    private static void handleRequest() {
        RequestContext context = REQUEST_CONTEXT.get();

        System.out.println("请求ID：" + context.requestId());
        System.out.println("用户ID：" + context.userId());

        queryUser();
    }

    private static void queryUser() {
        RequestContext context = REQUEST_CONTEXT.get();
        System.out.println("查询用户数据，用户ID：" + context.userId());
    }

    /**
     * 请求上下文
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record RequestContext(String requestId, Long userId) {
    }
}
```

Scoped Values 适合以下场景：

| 场景             | 说明                            |
| ---------------- | ------------------------------- |
| 请求上下文传递   | 请求ID、用户ID、租户ID          |
| 链路追踪信息     | TraceId、SpanId 等              |
| 虚拟线程上下文   | 避免大量 ThreadLocal 带来的成本 |
| 结构化并发子任务 | 父任务向子任务传递不可变上下文  |
| 框架内部上下文   | 框架内部安全共享只读信息        |

使用建议：

- 上下文对象应设计为不可变对象，例如 Record。
- 不应把可变集合或可变业务对象直接放入 Scoped Value。
- 不应把 Scoped Values 当作全局变量使用。
- 作用域结束后绑定自动失效。
- 在 JDK21 使用时需要开启 Preview；在 JDK25 可作为正式 API 使用。

### Unnamed Patterns and Variables

Unnamed Patterns and Variables 在 JDK21 中作为 Preview 特性引入，用 `_` 表示必须声明但不会使用的变量或模式。该特性在 JDK22 中成为正式特性，标题调整为 Unnamed Variables and Patterns。([OpenJDK](https://openjdk.org/jeps/443?utm_source=chatgpt.com))

它的核心价值是明确表达“这个变量故意不用”，提升代码可读性，减少无意义变量命名。

常见使用场景包括：

```java
try {
    Integer.parseInt("abc");
} catch (NumberFormatException _) {
    System.out.println("数字格式错误");
}
```

在 Record Pattern 中忽略不需要的字段：

```java
if (address instanceof AddressInfo(String city, _)) {
    System.out.println("城市：" + city);
}
```

以下代码演示未命名模式和变量的使用。

文件位置：`src/main/java/io/github/atengk/jdk21/unnamed/UnnamedPatternExample.java`

```java
package io.github.atengk.jdk21.unnamed;

import cn.hutool.core.util.StrUtil;

/**
 * Unnamed Patterns and Variables 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UnnamedPatternExample {

    public static void main(String[] args) {
        Object data = new UserAddress("杭州", "西湖区文三路");

        printCity(data);

        try {
            Integer.parseInt("abc");
        } catch (NumberFormatException _) {
            System.out.println("数字格式错误");
        }
    }

    public static void printCity(Object data) {
        if (data instanceof UserAddress(String city, _)) {
            if (StrUtil.isBlank(city)) {
                System.out.println("城市为空");
                return;
            }

            System.out.println("城市：" + city);
            return;
        }

        System.out.println("不支持的数据类型");
    }

    /**
     * 用户地址
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserAddress(String city, String detail) {
    }
}
```

使用建议：

- 当变量确实不会被使用时，可以使用 `_`。
- 不要用 `_` 隐藏本应使用的重要数据。
- 在 `catch`、lambda、循环、Record Pattern 中较常见。
- JDK21 使用时需要开启 Preview。
- JDK22 及之后可作为正式语法使用。

### String Templates

String Templates 是 JDK21 和 JDK22 中出现过的字符串模板预览特性，用于将字符串字面量、表达式和模板处理器结合起来。它曾用于探索比字符串拼接、`String.format` 和文本块更安全、更结构化的字符串插值方式。JDK22 对应的是第二次预览；从 JDK23 到 JDK25 的正式语言特性列表中，该特性并未继续作为可用特性出现，因此当前不建议在项目文档中把它作为 JDK25 可用语法推广。([OpenJDK](https://openjdk.org/jeps/459?utm_source=chatgpt.com))

历史预览写法大致如下：

```java
String username = "Ateng";
String message = STR."你好，\{username}";
```

由于该特性没有在 JDK25 中成为正式特性，项目开发中建议继续使用以下稳定方案：

| 场景                 | 推荐方案                             |
| -------------------- | ------------------------------------ |
| 简单字符串拼接       | `+` 或 `StringBuilder`               |
| 多变量格式化         | `String.format`                      |
| 多行 SQL、JSON、HTML | 文本块                               |
| 模板邮件、模板页面   | 模板引擎，例如 FreeMarker、Thymeleaf |
| 日志输出             | 使用日志框架占位符                   |

示例：

```java
String username = "Ateng";
Integer age = 18;

String message = String.format("用户名：%s，年龄：%d", username, age);

String json = """
        {
          "username": "%s",
          "age": %d
        }
        """.formatted(username, age);
```

使用建议：

- 不建议在生产代码中使用已停止推进的预览语法。
- 文档中可以作为历史演进说明，但不要作为推荐写法。
- 涉及 SQL 拼接时，不应依赖字符串模板解决注入问题。
- 复杂模板仍应使用成熟模板引擎或参数化 API。

## JDK22 到 JDK25 新语法演进

JDK22 到 JDK25 继续推进 Java 语法简化、模式匹配统一、构造器限制放宽、模块导入简化和小程序入口简化。根据 Java SE 25 语言变化摘要，JDK25 中 Module Import Declarations、Compact Source Files and Instance Main Methods、Flexible Constructor Bodies 已成为正式语言特性；Primitive Types in Patterns, instanceof, and switch 在 JDK25 仍是 Preview 特性。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/language/java-language-changes-summary.html?utm_source=chatgpt.com))

### Unnamed Variables and Patterns

Unnamed Variables and Patterns 是 JDK22 正式引入的特性。它允许在局部变量、异常参数、lambda 参数、循环变量和模式匹配中使用 `_` 表示“变量必须声明，但不会被使用”。([OpenJDK](https://openjdk.org/jeps/456?utm_source=chatgpt.com))

以下代码演示在异常处理、循环和 lambda 中使用未命名变量。

文件位置：`src/main/java/io/github/atengk/jdk22/unnamed/UnnamedVariablesExample.java`

```java
package io.github.atengk.jdk22.unnamed;

import java.util.List;
import java.util.Map;

/**
 * Unnamed Variables and Patterns 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UnnamedVariablesExample {

    public static void main(String[] args) {
        parseNumber("abc");

        List<String> usernameList = List.of("Tom", "Jerry", "Alice");
        int count = 0;

        for (String _ : usernameList) {
            count++;
        }

        System.out.println("用户数量：" + count);

        Map<String, Integer> scoreMap = Map.of("Tom", 90, "Jerry", 88);
        scoreMap.forEach((_, score) -> System.out.println("分数：" + score));
    }

    private static void parseNumber(String text) {
        try {
            Integer.parseInt(text);
        } catch (NumberFormatException _) {
            System.out.println("数字格式错误：" + text);
        }
    }
}
```

以下代码演示在 Record Pattern 中忽略不需要的字段。

文件位置：`src/main/java/io/github/atengk/jdk22/unnamed/UnnamedPatternFinalExample.java`

```java
package io.github.atengk.jdk22.unnamed;

/**
 * Unnamed Pattern 正式特性示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UnnamedPatternFinalExample {

    public static void main(String[] args) {
        Object data = new UserInfo(1001L, "Ateng", "13800138000");

        if (data instanceof UserInfo(Long userId, String username, _)) {
            System.out.println("用户ID：" + userId + "，用户名：" + username);
        }
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserInfo(Long userId, String username, String mobile) {
    }
}
```

使用建议：

| 场景                      | 建议         |
| ------------------------- | ------------ |
| catch 异常参数未使用      | 推荐使用 `_` |
| lambda 某个参数未使用     | 推荐使用 `_` |
| for 循环只关心次数        | 可以使用 `_` |
| Record Pattern 中忽略字段 | 推荐使用 `_` |
| 变量后续可能使用          | 不应使用 `_` |

需要注意的是，`_` 不能被读取，也不能在后续代码中引用。它不是一个普通变量名，而是明确表示“这个位置不需要名称”。

### Flexible Constructor Bodies

Flexible Constructor Bodies 是 JDK25 正式引入的构造器增强。它允许在构造器中，将部分安全语句放在 `super(...)` 或 `this(...)` 之前执行，从而支持在调用父类构造器前进行参数校验、参数转换和字段初始化。([OpenJDK](https://openjdk.org/jeps/513?utm_source=chatgpt.com))

在 JDK25 之前，构造器中如果显式调用 `super(...)` 或 `this(...)`，它必须是第一条语句：

```java
public Employee(String name, int age) {
    super(name, age);
    if (age < 18) {
        throw new IllegalArgumentException("员工年龄不能小于18岁");
    }
}
```

这种写法的问题是：参数校验发生在父类构造器调用之后，无法做到真正的提前失败。

JDK25 后，可以在 `super(...)` 前执行安全校验：

```java
public Employee(String name, int age, String officeId) {
    int checkedAge = checkAge(age);
    this.officeId = StrUtil.blankToDefault(officeId, "UNKNOWN");
    super(name, checkedAge);
}
```

以下代码演示 Flexible Constructor Bodies 的使用。

文件位置：`src/main/java/io/github/atengk/jdk25/constructor/FlexibleConstructorExample.java`

```java
package io.github.atengk.jdk25.constructor;

import cn.hutool.core.util.StrUtil;

/**
 * Flexible Constructor Bodies 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FlexibleConstructorExample {

    public static void main(String[] args) {
        Employee employee = new Employee("Ateng", 28, "");
        System.out.println(employee);
    }

    /**
     * 员工信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class Employee extends Person {

        private final String officeId;

        public Employee(String name, int age, String officeId) {
            int checkedAge = checkAge(age);
            this.officeId = StrUtil.blankToDefault(officeId, "UNKNOWN");
            super(name, checkedAge);
        }

        private static int checkAge(int age) {
            if (age < 18 || age > 67) {
                throw new IllegalArgumentException("员工年龄必须在18到67之间");
            }
            return age;
        }

        @Override
        public String toString() {
            return "Employee{" +
                    "name='" + getName() + '\'' +
                    ", age=" + getAge() +
                    ", officeId='" + officeId + '\'' +
                    '}';
        }
    }

    /**
     * 人员信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    static class Person {

        private final String name;

        private final int age;

        public Person(String name, int age) {
            this.name = StrUtil.blankToDefault(name, "未命名");
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
```

使用建议：

- 适合在调用父类构造器前做参数校验。
- 适合在父类构造器执行前初始化子类关键字段。
- `super(...)` 前的代码不能随意访问尚未完成构造的对象状态。
- 不建议在构造器前置阶段执行复杂业务逻辑、IO 操作或远程调用。
- 对框架实体类、ORM 实体类仍应保持简单构造器设计。

### Module Import Declarations

Module Import Declarations 是 JDK25 正式引入的模块导入声明。它允许通过 `import module 模块名;` 一次性导入某个模块导出的所有公共顶层类型，从而减少多个包级通配导入。该特性不要求当前代码本身必须位于模块中。([OpenJDK](https://openjdk.org/jeps/8344700?utm_source=chatgpt.com))

传统写法：

```java
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.file.Path;
```

JDK25 后可以写成：

```java
import module java.base;
```

以下代码演示模块导入声明的使用。

文件位置：`src/main/java/io/github/atengk/jdk25/moduleimport/ModuleImportExample.java`

```java
package io.github.atengk.jdk25.moduleimport;

import module java.base;

/**
 * Module Import Declarations 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ModuleImportExample {

    public static void main(String[] args) {
        List<String> usernameList = List.of("Tom", "Jerry", "Alice");
        Map<String, BigDecimal> amountMap = Map.of(
                "Tom", new BigDecimal("99.90"),
                "Jerry", new BigDecimal("88.80")
        );
        Path path = Path.of("data/user.txt");

        System.out.println("用户列表：" + usernameList);
        System.out.println("金额映射：" + amountMap);
        System.out.println("文件路径：" + path);
    }
}
```

Module Import Declarations 适合以下场景：

| 场景             | 说明                           |
| ---------------- | ------------------------------ |
| 教学示例         | 减少导入语句干扰               |
| 小工具类         | 快速使用标准库类型             |
| 脚本式 Java 文件 | 配合 compact source files 使用 |
| 模块化 API 使用  | 一次导入模块导出的公共类型     |

使用建议：

- 普通企业项目中不建议无节制使用。
- 大型项目仍建议使用明确的类导入，减少命名冲突。
- 如果多个模块导入后出现同名类型，应改用显式导入。
- 对团队代码规范要求较高的项目，应先统一规范再使用。
- 更适合小程序、教学代码、临时工具和实验代码。

### Compact Source Files

Compact Source Files 是 JDK25 正式引入的紧凑源文件特性。它允许编写简单 Java 程序时省略显式类声明，让开发者可以直接在源文件中声明 `main` 方法、辅助方法和字段。该特性主要用于教学、小工具和脚本式程序，不是为了替代大型项目中的标准类结构。([OpenJDK](https://openjdk.org/jeps/512?utm_source=chatgpt.com))

传统 Hello World 写法：

```java
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

JDK25 紧凑源文件写法：

```java
void main() {
    IO.println("Hello, World!");
}
```

以下代码演示紧凑源文件编写一个简单用户统计工具。

文件位置：`UserCounter.java`

```java
void main() {
    var users = List.of("Tom", "Jerry", "Alice");

    IO.println("用户数量：" + users.size());

    for (var username : users) {
        IO.println("用户名：" + username);
    }
}
```

可以直接运行：

```bash
java UserCounter.java
```

紧凑源文件适合以下场景：

| 场景           | 说明                                  |
| -------------- | ------------------------------------- |
| Java 入门教学  | 减少 class、public、static 等概念干扰 |
| 临时脚本       | 快速编写小工具                        |
| 命令行工具原型 | 快速验证逻辑                          |
| 算法练习       | 聚焦算法本身                          |
| API 示例       | 减少样板结构                          |

使用建议：

- 不建议用于标准 Spring Boot 项目。
- 不建议用于需要包名、访问控制和模块边界的正式业务代码。
- 可用于项目内临时工具、演示代码和学习示例。
- 当代码规模变大时，应改造成普通 Java 类。
- 不要把紧凑源文件当作替代工程结构的方案。

### Instance Main Methods

Instance Main Methods 是 JDK25 中与 Compact Source Files 一起正式引入的入口方法简化能力。它允许 `main` 方法不再必须声明为 `public static void main(String[] args)`，可以使用实例 `main` 方法作为程序入口。([OpenJDK](https://openjdk.org/jeps/512?utm_source=chatgpt.com))

传统入口方法：

```java
public static void main(String[] args) {
    System.out.println("Hello");
}
```

JDK25 后可使用实例入口方法：

```java
class HelloApp {

    void main() {
        IO.println("Hello");
    }
}
```

以下代码演示实例 main 方法的使用。

文件位置：`src/main/java/io/github/atengk/jdk25/main/InstanceMainExample.java`

```java
package io.github.atengk.jdk25.main;

/**
 * Instance Main Methods 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
class InstanceMainExample {

    void main() {
        IO.println("使用实例 main 方法启动程序");

        String message = buildMessage("Ateng");
        IO.println(message);
    }

    private String buildMessage(String username) {
        return "你好，" + username;
    }
}
```

Instance Main Methods 支持更简洁的入口写法，但在企业项目中仍应谨慎使用。

使用建议：

| 场景               | 建议                 |
| ------------------ | -------------------- |
| Java 教学          | 推荐使用             |
| 小工具程序         | 可以使用             |
| 算法练习           | 可以使用             |
| Spring Boot 启动类 | 不建议替代标准写法   |
| 团队业务项目       | 建议保持统一入口规范 |

对于 Spring Boot 项目，仍推荐使用传统启动类结构：

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

原因是标准写法更符合框架约定、团队习惯、构建工具、插件扫描和运维脚本预期。

### Primitive Types in Patterns instanceof and switch

Primitive Types in Patterns, instanceof, and switch 在 JDK25 中仍是第三次预览特性。它允许模式匹配、`instanceof` 和 `switch` 更统一地支持基本类型，例如用 `instanceof byte b` 判断一个数值是否可以安全转换为 byte，或者让 `switch` 支持更多基本类型。([OpenJDK](https://openjdk.org/jeps/507?utm_source=chatgpt.com))

传统写法中，如果要判断 `int` 是否可以安全转成 `byte`，需要手动判断范围：

```java
int value = 100;

if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
    byte result = (byte) value;
    System.out.println(result);
}
```

预览特性写法：

```java
int value = 100;

if (value instanceof byte result) {
    System.out.println(result);
}
```

以下代码演示基本类型模式匹配的使用。该代码属于 JDK25 Preview 示例，生产代码中不建议直接使用。

文件位置：`src/main/java/io/github/atengk/jdk25/primitivepattern/PrimitivePatternExample.java`

```java
package io.github.atengk.jdk25.primitivepattern;

/**
 * Primitive Types in Patterns, instanceof, and switch 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PrimitivePatternExample {

    public static void main(String[] args) {
        printByteValue(100);
        printByteValue(1000);

        String result = checkScore(95L);
        System.out.println(result);
    }

    private static void printByteValue(int value) {
        if (value instanceof byte byteValue) {
            System.out.println("可以安全转换为 byte：" + byteValue);
            return;
        }

        System.out.println("不能安全转换为 byte：" + value);
    }

    private static String checkScore(long score) {
        return switch (score) {
            case 100L -> "满分";
            case 90L -> "优秀";
            case long value when value >= 60L -> "及格：" + value;
            case long value -> "不及格：" + value;
        };
    }
}
```

JDK25 编译和运行 Preview 代码需要开启预览特性：

```bash
javac --release 25 --enable-preview src/main/java/io/github/atengk/jdk25/primitivepattern/PrimitivePatternExample.java
java --enable-preview io.github.atengk.jdk25.primitivepattern.PrimitivePatternExample
```

使用建议：

- 该特性在 JDK25 仍是 Preview，不建议用于生产核心代码。
- 可以在实验项目、技术预研和语法学习中使用。
- 适合数值安全转换、模式匹配统一化和 switch 基本类型分支。
- 升级后需要关注 JDK 后续版本是否继续调整语法。
- 团队编码规范中应明确 Preview 特性默认禁用。

### Scoped Values 正式特性

Scoped Values 在 JDK25 中成为正式特性。它用于在有界作用域内共享不可变数据，可以替代部分 `ThreadLocal` 使用场景，尤其适合虚拟线程和结构化并发场景。([OpenJDK](https://openjdk.org/jeps/506?utm_source=chatgpt.com))

以下代码演示在 JDK25 中使用 Scoped Values 传递请求上下文。

文件位置：`src/main/java/io/github/atengk/jdk25/scoped/ScopedValueFinalExample.java`

```java
package io.github.atengk.jdk25.scoped;

import cn.hutool.core.util.StrUtil;

/**
 * Scoped Values 正式特性示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ScopedValueFinalExample {

    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

    public static void main(String[] args) {
        RequestContext context = new RequestContext("REQ-202605130001", 1001L, "tenant001");

        ScopedValue.where(REQUEST_CONTEXT, context)
                .run(ScopedValueFinalExample::handleRequest);
    }

    private static void handleRequest() {
        RequestContext context = REQUEST_CONTEXT.get();

        if (StrUtil.isBlank(context.requestId())) {
            throw new IllegalArgumentException("请求ID不能为空");
        }

        System.out.println("处理请求，请求ID：" + context.requestId());
        queryOrder();
        queryUser();
    }

    private static void queryOrder() {
        RequestContext context = REQUEST_CONTEXT.get();
        System.out.println("查询订单，租户ID：" + context.tenantId());
    }

    private static void queryUser() {
        RequestContext context = REQUEST_CONTEXT.get();
        System.out.println("查询用户，用户ID：" + context.userId());
    }

    /**
     * 请求上下文
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record RequestContext(String requestId, Long userId, String tenantId) {
    }
}
```

Scoped Values 与 ThreadLocal 的对比如下：

| 对比项       | ThreadLocal            | Scoped Values            |
| ------------ | ---------------------- | ------------------------ |
| 数据可变性   | 通常可变               | 推荐不可变               |
| 生命周期     | 依赖手动 remove        | 绑定在明确作用域内       |
| 虚拟线程场景 | 大量使用可能增加成本   | 更适合虚拟线程           |
| 数据流理解   | 隐式且容易泄漏         | 作用域边界更清晰         |
| 典型用途     | 老项目上下文、框架兼容 | 新并发模型下的上下文传递 |

使用建议：

- 新代码中，如果只是传递请求上下文、租户上下文、TraceId 等不可变数据，可以优先评估 Scoped Values。
- 老项目中已有 ThreadLocal 方案，不建议一次性全量替换，应逐步迁移。
- 上下文对象建议使用 Record。
- 不要在 Scoped Values 中存放可变集合、数据库连接、事务对象等复杂状态。
- 与虚拟线程和结构化并发结合使用时，收益更明显。





## Java 新语法在项目开发中的应用场景

Java 新语法的价值不在于“写法更新”，而在于让项目代码更清晰、更稳定、更容易维护。实际开发中，优先适合落地的场景包括 DTO 简化、条件分支重构、集合处理、并发任务处理、配置类与启动类简化，以及异常定位和日志排查。

对于业务项目，建议按以下原则使用新语法：

| 原则                        | 说明                                                    |
| --------------------------- | ------------------------------------------------------- |
| 优先提升可读性              | 新语法应让代码更容易理解，而不是更难读                  |
| 优先用于边界清晰的代码      | DTO、VO、状态转换、集合处理、并发查询等场景更适合       |
| 避免过度函数式              | 复杂业务流程不应全部压缩进 Stream 或 Lambda             |
| 区分正式特性和 Preview 特性 | 正式特性可以逐步落地，Preview 特性不建议用于核心业务    |
| 保持团队统一规范            | 同一项目内应统一 var、Record、Stream、switch 的使用边界 |

### 实体类与 DTO 简化

实体类与 DTO 简化是新语法最容易落地的场景之一。传统 Java 项目中，Entity、DTO、VO 往往包含大量字段、构造方法、Getter、Setter、`toString`、`equals` 和 `hashCode`。在 JDK17 及以上版本中，可以使用 Record 简化只承载数据的对象。

需要注意的是，Record 更适合 DTO、VO、查询条件、临时聚合结果，不适合直接替代 JPA 或 MyBatis-Plus 实体类。实体类通常需要无参构造、可变字段、框架代理、字段填充和 ORM 映射，仍建议保持普通 class。

推荐使用方式如下：

| 类型              | 推荐写法                                 |
| ----------------- | ---------------------------------------- |
| 数据库实体 Entity | 普通 class                               |
| 接口入参 DTO      | 普通 class 或 Record，视框架支持情况决定 |
| 接口出参 VO       | Record                                   |
| 查询结果聚合对象  | Record                                   |
| 内部不可变值对象  | Record                                   |

以下代码演示普通实体类和 Record VO 的配合使用。

文件位置：`src/main/java/io/github/atengk/application/user/entity/UserEntity.java`

```java
package io.github.atengk.application.user.entity;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserEntity {

    private Long id;

    private String username;

    private String mobile;

    private Integer status;

    private LocalDateTime createTime;

    public UserEntity() {
    }

    public UserEntity(Long id, String username, String mobile, Integer status, LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.mobile = mobile;
        this.status = status;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMobile() {
        return mobile;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

以下代码使用 Record 定义用户接口返回对象，适合 JDK17 及以上项目。

文件位置：`src/main/java/io/github/atengk/application/user/vo/UserVO.java`

```java
package io.github.atengk.application.user.vo;

import java.time.LocalDateTime;

/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserVO(
        Long id,
        String username,
        String mobile,
        String statusName,
        LocalDateTime createTime
) {
}
```

以下代码演示 Entity 到 VO 的转换逻辑。这里保留普通转换方法，避免将过多逻辑隐藏在工具类中。

文件位置：`src/main/java/io/github/atengk/application/user/converter/UserConverter.java`

```java
package io.github.atengk.application.user.converter;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.application.user.entity.UserEntity;
import io.github.atengk.application.user.vo.UserVO;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserConverter {

    private UserConverter() {
    }

    /**
     * 用户实体转换为展示对象
     *
     * @param entity 用户实体
     * @return 用户展示对象
     */
    public static UserVO toVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserVO(
                entity.getId(),
                StrUtil.blankToDefault(entity.getUsername(), "未知用户"),
                DesensitizedUtil.mobilePhone(entity.getMobile()),
                getStatusName(entity.getStatus()),
                entity.getCreateTime()
        );
    }

    /**
     * 获取用户状态名称
     *
     * @param status 用户状态
     * @return 状态名称
     */
    private static String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            case 2 -> "锁定";
            default -> "未知";
        };
    }
}
```

使用建议如下：

| 场景                       | 建议                                   |
| -------------------------- | -------------------------------------- |
| DTO 只有字段，没有复杂行为 | 可以使用 Record                        |
| VO 只用于接口返回          | 推荐使用 Record                        |
| Entity 需要 ORM 映射       | 继续使用普通 class                     |
| 对象需要频繁修改字段       | 不建议使用 Record                      |
| 对象转换逻辑复杂           | 提取 Converter，不要堆在 Controller 中 |

### 条件分支重构

条件分支重构适合使用 switch 表达式、枚举、sealed interface、Record Pattern 和 Pattern Matching for switch。对于 JDK17 项目，可以优先使用 switch 表达式和枚举；对于 JDK21 及以上项目，可以进一步使用 Pattern Matching for switch 处理类型分支。

传统分支代码通常存在以下问题：

- `if-else` 链过长。
- 状态码和状态名称分散在多个地方。
- `switch` 忘记写 `break`。
- 类型判断后需要手动强制转换。
- 分支逻辑扩展后可读性下降。

对于状态码转换，推荐使用 switch 表达式。

文件位置：`src/main/java/io/github/atengk/application/order/enums/OrderStatus.java`

```java
package io.github.atengk.application.order.enums;

/**
 * 订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum OrderStatus {

    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    FINISHED(3, "已完成"),
    CANCELED(4, "已取消");

    private final Integer code;

    private final String name;

    OrderStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据状态码获取状态名称
     *
     * @param code 状态码
     * @return 状态名称
     */
    public static String getNameByCode(Integer code) {
        if (code == null) {
            return "未知";
        }

        return switch (code) {
            case 0 -> WAIT_PAY.getName();
            case 1 -> PAID.getName();
            case 2 -> SHIPPED.getName();
            case 3 -> FINISHED.getName();
            case 4 -> CANCELED.getName();
            default -> "未知";
        };
    }
}
```

对于类型分支，JDK21 及以上可以使用 sealed interface 和 Pattern Matching for switch。Oracle 的 Java 25 语言变更摘要中列明，Pattern Matching for switch 在 Java 21 已成为正式语言特性，Record Patterns 也在 Java 21 成为正式语言特性。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/language/java-language-changes-summary.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/application/pay/model/PayResult.java`

```java
package io.github.atengk.application.pay.model;

import java.math.BigDecimal;

/**
 * 支付结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
public sealed interface PayResult permits PayResult.Success, PayResult.Failure, PayResult.Processing {

    /**
     * 支付成功
     *
     * @author Ateng
     * @since 2026-05-13
     */
    record Success(String payNo, BigDecimal amount) implements PayResult {
    }

    /**
     * 支付失败
     *
     * @author Ateng
     * @since 2026-05-13
     */
    record Failure(String reason) implements PayResult {
    }

    /**
     * 支付处理中
     *
     * @author Ateng
     * @since 2026-05-13
     */
    record Processing(String bizNo) implements PayResult {
    }
}
```

文件位置：`src/main/java/io/github/atengk/application/pay/service/PayResultService.java`

```java
package io.github.atengk.application.pay.service;

import io.github.atengk.application.pay.model.PayResult;

import java.math.BigDecimal;

/**
 * 支付结果服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PayResultService {

    /**
     * 构建支付结果描述
     *
     * @param result 支付结果
     * @return 支付结果描述
     */
    public String buildMessage(PayResult result) {
        return switch (result) {
            case PayResult.Success success when success.amount().compareTo(BigDecimal.ZERO) > 0 ->
                    "支付成功，流水号：" + success.payNo() + "，金额：" + success.amount();
            case PayResult.Success success ->
                    "支付成功，但金额异常，流水号：" + success.payNo();
            case PayResult.Failure failure ->
                    "支付失败，原因：" + failure.reason();
            case PayResult.Processing processing ->
                    "支付处理中，业务号：" + processing.bizNo();
            case null ->
                    "支付结果为空";
        };
    }
}
```

条件分支重构建议如下：

| 场景                 | 推荐方式                              |
| -------------------- | ------------------------------------- |
| 简单状态码转换       | switch 表达式                         |
| 状态和行为绑定       | 枚举方法                              |
| 分支类型有限         | sealed interface                      |
| 类型判断后需要取字段 | Pattern Matching for switch           |
| 分支逻辑复杂         | 策略模式或多态                        |
| 分支包含数据库操作   | 提取 Service 方法，不要写在 switch 中 |

### 集合与流式处理

集合处理是 Lambda、Stream API、方法引用、集合工厂方法和 Sequenced Collections 最常见的落地场景。JDK8 项目可以使用 Stream 完成过滤、映射、排序、分组和聚合；JDK9 及以上可以使用集合工厂方法创建不可变集合；JDK21 及以上可以使用 Sequenced Collections 统一处理有序集合的首尾元素和反向访问。

JDK21 引入的 Sequenced Collections 为具有明确遍历顺序的集合提供统一 API，例如访问首元素、尾元素和反向视图。([Oracle Docs](https://docs.oracle.com/en/java/javase/21/migrate/significant-changes-jdk-release.html?utm_source=chatgpt.com))

以下代码演示订单列表的过滤、分组、金额汇总和展示对象转换。

文件位置：`src/main/java/io/github/atengk/application/order/service/OrderStatisticsService.java`

```java
package io.github.atengk.application.order.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单统计服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderStatisticsService {

    /**
     * 按用户统计已支付订单金额
     *
     * @param orderList 订单列表
     * @return 用户订单统计列表
     */
    public List<UserOrderSummary> summaryPaidOrderByUser(List<OrderInfo> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            return List.of();
        }

        Map<Long, BigDecimal> userAmountMap = orderList.stream()
                .filter(order -> order.status() == 1)
                .filter(order -> order.userId() != null)
                .collect(Collectors.groupingBy(
                        OrderInfo::userId,
                        Collectors.mapping(
                                OrderInfo::amount,
                                Collectors.reducing(BigDecimal.ZERO, amount -> amount == null ? BigDecimal.ZERO : amount, BigDecimal::add)
                        )
                ));

        return userAmountMap.entrySet()
                .stream()
                .map(entry -> new UserOrderSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(UserOrderSummary::totalAmount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取最高消费用户
     *
     * @param summaryList 用户订单统计列表
     * @return 最高消费用户
     */
    public UserOrderSummary getTopUser(List<UserOrderSummary> summaryList) {
        if (CollUtil.isEmpty(summaryList)) {
            return null;
        }

        return summaryList.stream()
                .filter(summary -> NumberUtil.isGreater(summary.totalAmount(), BigDecimal.ZERO))
                .max(Comparator.comparing(UserOrderSummary::totalAmount))
                .orElse(null);
    }

    /**
     * 订单信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record OrderInfo(Long orderId, Long userId, Integer status, BigDecimal amount) {
    }

    /**
     * 用户订单统计
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserOrderSummary(Long userId, BigDecimal totalAmount) {
    }
}
```

如果项目已经升级到 JDK21，可以对有序集合使用更直接的首尾访问方法。

文件位置：`src/main/java/io/github/atengk/application/order/service/OrderSequenceService.java`

```java
package io.github.atengk.application.order.service;

import cn.hutool.core.collection.CollUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单顺序处理服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderSequenceService {

    /**
     * 获取用户最近一笔订单
     *
     * @param orderList 已按时间升序排列的订单列表
     * @return 最近一笔订单
     */
    public OrderInfo getLatestOrder(List<OrderInfo> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            return null;
        }

        return orderList.getLast();
    }

    /**
     * 反向输出订单
     *
     * @param orderList 订单列表
     */
    public void printReverseOrders(List<OrderInfo> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            System.out.println("订单列表为空");
            return;
        }

        orderList.reversed()
                .forEach(order -> System.out.println("订单ID：" + order.orderId()));
    }

    /**
     * 订单信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record OrderInfo(Long orderId, Long userId, LocalDateTime createTime) {
    }
}
```

集合与 Stream 使用建议如下：

| 场景               | 建议                                          |
| ------------------ | --------------------------------------------- |
| 小规模内存集合转换 | 推荐使用 Stream                               |
| 分组、汇总、去重   | 推荐使用 Stream Collectors                    |
| 固定集合初始化     | JDK9 及以上使用 `List.of`、`Set.of`、`Map.of` |
| 有序集合首尾访问   | JDK21 及以上使用 `getFirst()`、`getLast()`    |
| 大数据量过滤和分页 | 优先交给数据库                                |
| 有副作用的业务流程 | 不建议写进 Stream                             |
| 复杂异常处理       | 使用普通循环更清晰                            |

### 并发任务处理

并发任务处理是 JDK21 之后非常重要的升级方向。Virtual Threads 已在 JDK21 成为正式特性，适合 IO 密集型、高并发请求和批量远程调用场景。OpenJDK 对 Virtual Threads 的目标描述是降低编写、维护和观察高吞吐并发应用的成本，并且明确说明虚拟线程不应池化，推荐为每个应用任务创建新的虚拟线程。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

在传统 Java 项目中，并发查询多个外部服务通常会使用固定线程池或 `CompletableFuture`。当业务逻辑以阻塞调用为主时，虚拟线程可以让代码保持同步写法，同时提升并发承载能力。

以下代码演示使用虚拟线程并发查询用户基础信息、账户信息和权限信息。

文件位置：`src/main/java/io/github/atengk/application/user/service/UserProfileQueryService.java`

```java
package io.github.atengk.application.user.service;

import cn.hutool.core.collection.CollUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 用户资料查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserProfileQueryService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileQueryService.class);

    /**
     * 批量查询用户资料
     *
     * @param userIdList 用户ID列表
     */
    public void batchQueryUserProfile(List<Long> userIdList) {
        if (CollUtil.isEmpty(userIdList)) {
            log.info("用户ID列表为空，跳过批量查询");
            return;
        }

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long userId : userIdList) {
                executorService.submit(() -> queryUserProfile(userId));
            }
        }

        log.info("批量用户资料查询任务已完成，用户数量：{}", userIdList.size());
    }

    /**
     * 查询用户完整资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    public UserProfile queryUserProfile(Long userId) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UserInfo> userFuture = executorService.submit(() -> queryUserInfo(userId));
            Future<AccountInfo> accountFuture = executorService.submit(() -> queryAccountInfo(userId));

            UserInfo userInfo = userFuture.get();
            AccountInfo accountInfo = accountFuture.get();

            return new UserProfile(userInfo, accountInfo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("用户资料查询被中断，用户ID：{}", userId);
            throw new IllegalStateException("用户资料查询被中断");
        } catch (ExecutionException e) {
            log.error("用户资料查询失败，用户ID：{}", userId, e);
            throw new IllegalStateException("用户资料查询失败", e);
        }
    }

    private UserInfo queryUserInfo(Long userId) {
        log.info("查询用户基础信息，用户ID：{}", userId);
        return new UserInfo(userId, "Ateng");
    }

    private AccountInfo queryAccountInfo(Long userId) {
        log.info("查询账户信息，用户ID：{}", userId);
        return new AccountInfo(userId, "正常");
    }

    /**
     * 用户资料
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserProfile(UserInfo userInfo, AccountInfo accountInfo) {
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record UserInfo(Long userId, String username) {
    }

    /**
     * 账户信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record AccountInfo(Long userId, String status) {
    }
}
```

并发任务处理建议如下：

| 场景           | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| IO 密集型接口  | 可以评估虚拟线程                                             |
| 批量远程调用   | 可以使用虚拟线程并发处理                                     |
| CPU 密集型计算 | 不应依赖虚拟线程提升吞吐                                     |
| 数据库访问     | 需要同时评估数据库连接池容量                                 |
| HTTP 调用      | 需要同时评估 HTTP 连接池容量                                 |
| 复杂任务编排   | 可关注 Structured Concurrency，但 Preview 阶段不建议核心业务依赖 |
| 上下文传递     | JDK25 及以上可评估 Scoped Values                             |

虚拟线程不能绕过外部资源瓶颈。例如数据库连接池最大连接数是 50，即使创建 5000 个虚拟线程，也不能同时执行 5000 个数据库查询。虚拟线程解决的是线程成本问题，不是数据库、缓存、网络和 CPU 资源限制问题。

### 配置类与启动类简化

配置类与启动类简化主要涉及 var、Record、文本块、Compact Source Files、Instance Main Methods 和 Module Import Declarations。对于标准 Spring Boot 项目，启动类仍建议使用传统写法；对于临时工具、教学示例和单文件脚本，可以使用 JDK25 的紧凑源文件和实例 main 方法。

Spring Boot 3.0 正式版要求 Java 17 或更高版本，这意味着很多从 Spring Boot 2.x 升级到 Spring Boot 3.x 的项目，会同时面对 JDK8 到 JDK17 的升级。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com))

标准 Spring Boot 启动类仍建议保持如下写法。

文件位置：`src/main/java/io/github/atengk/Application.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

配置对象可以使用 Record 表达不可变配置，但需要结合 Spring Boot 版本和绑定方式确认兼容性。对于 Spring Boot 3.x，可以优先考虑构造绑定风格。

文件位置：`src/main/java/io/github/atengk/config/StorageProperties.java`

```java
package io.github.atengk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String bucket,
        String accessKey,
        String secretKey
) {
}
```

文件位置：`src/main/java/io/github/atengk/config/AppConfig.java`

```java
package io.github.atengk.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class AppConfig {
}
```

配置文件示例如下。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  storage:
    # 对象存储访问地址
    endpoint: http://127.0.0.1:9000
    # 默认桶名称
    bucket: demo
    # 访问密钥
    access-key: admin
    # 访问密钥对应的私钥
    secret-key: 12345678
```

JDK25 中 Compact Source Files and Instance Main Methods 已成为正式语言特性，适合教学、临时工具和小型单文件程序；同时，Module Import Declarations 和 Flexible Constructor Bodies 也在 JDK25 成为正式语言特性。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/language/java-language-changes-summary.html?utm_source=chatgpt.com))

例如，简单临时工具可以写成紧凑源文件。

文件位置：`UserExportTool.java`

```java
import module java.base;

void main() {
    var users = List.of("Tom", "Jerry", "Alice");

    IO.println("开始导出用户数据");

    for (var username : users) {
        IO.println("用户：" + username);
    }

    IO.println("用户数据导出完成");
}
```

运行方式如下：

```bash
java UserExportTool.java
```

使用建议如下：

| 场景               | 建议                               |
| ------------------ | ---------------------------------- |
| Spring Boot 启动类 | 保持标准 `public static void main` |
| 配置属性类         | 可评估 Record                      |
| 临时命令行工具     | JDK25 可使用紧凑源文件             |
| 教学示例           | JDK25 可使用实例 main 方法         |
| 企业业务代码       | 不建议大量使用紧凑源文件           |
| 大型项目导入       | 不建议滥用 `import module`         |

### 异常处理与日志定位

异常处理与日志定位是版本升级后收益明显但容易被忽略的场景。JDK14 引入 Helpful NullPointerException 后，空指针异常信息可以更准确指出链式调用中哪个表达式为 `null`。这对老项目排查线上问题非常有价值。

不过，Helpful NullPointerException 只能帮助定位问题，不能替代参数校验、空值防御和业务异常设计。项目中仍应保持清晰的异常边界和日志规范。

以下代码演示业务参数校验、异常抛出和日志输出方式。

文件位置：`src/main/java/io/github/atengk/application/order/service/OrderCreateService.java`

```java
package io.github.atengk.application.order.service;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderCreateService {

    private static final Logger log = LoggerFactory.getLogger(OrderCreateService.class);

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单号
     */
    public String createOrder(CreateOrderRequest request) {
        validateRequest(request);

        String orderNo = "O" + System.currentTimeMillis();

        log.info("创建订单成功，订单号：{}，用户ID：{}，金额：{}", orderNo, request.userId(), request.amount());

        return orderNo;
    }

    /**
     * 校验创建订单请求
     *
     * @param request 创建订单请求
     */
    private void validateRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("创建订单请求不能为空");
        }

        if (request.userId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
    }

    /**
     * 创建订单请求
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record CreateOrderRequest(Long userId, String productName, BigDecimal amount) {
    }
}
```

异常处理建议如下：

| 场景         | 建议                                    |
| ------------ | --------------------------------------- |
| 参数错误     | 使用明确的业务异常或参数异常            |
| 空值问题     | 优先在入口校验，而不是等待 NPE          |
| 链式调用     | 避免过长链式调用                        |
| 关键业务操作 | 记录必要日志                            |
| 异常日志     | 保留异常堆栈，不只记录 message          |
| 可预期失败   | 使用业务异常，不要滥用 RuntimeException |
| 不可恢复异常 | 记录上下文后向上抛出                    |

日志建议如下：

| 日志级别 | 使用场景                         |
| -------- | -------------------------------- |
| `debug`  | 开发调试、详细中间变量           |
| `info`   | 关键业务成功、流程节点           |
| `warn`   | 可恢复异常、降级、参数异常       |
| `error`  | 系统异常、外部服务失败、数据异常 |

## 版本升级与兼容性策略

版本升级不应只看语法特性，还需要同时评估构建工具、框架版本、第三方依赖、运行参数、容器镜像、CI/CD、监控和线上回滚策略。对于 Java 项目，JDK8 到 JDK17、JDK17 到 JDK21、JDK21 到 JDK25 的升级重点并不相同。

JDK17 的迁移文档中特别强调，JDK 内部元素默认强封装，并且不能再像 JDK9 到 JDK16 那样通过单个命令行参数整体放松强封装；这类变化会影响依赖内部 JDK API 的老项目。([Oracle Docs](https://docs.oracle.com/en/java/javase/17/migrate/significant-changes-jdk-release.html?utm_source=chatgpt.com))

### JDK8 到 JDK17 升级重点

JDK8 到 JDK17 是跨度最大的常见升级路径。它不仅包含语法升级，还涉及模块化、强封装、依赖兼容、`javax` 到 `jakarta` 生态迁移、GC 默认行为变化、构建插件升级和运行参数调整。

升级前应重点检查以下内容：

| 检查项       | 说明                                          |
| ------------ | --------------------------------------------- |
| 构建工具     | Maven、Gradle 版本是否支持 JDK17              |
| 编译插件     | `maven-compiler-plugin` 是否配置 `release`    |
| Spring 版本  | Spring Boot 2.x 与 3.x 的 Java 版本要求不同   |
| 第三方依赖   | 是否使用过旧 ASM、CGLIB、Javassist、Lombok    |
| JDK 内部 API | 是否使用 `sun.misc.*`、`com.sun.*` 等内部 API |
| 反射访问     | 是否需要 `--add-opens` 临时兼容               |
| JAXB/JAX-WS  | JDK11 后部分 Java EE 模块不再随 JDK 提供      |
| 日期时间     | 优先使用 `java.time` 替代旧日期 API           |
| 单元测试     | Mockito、Surefire、JaCoCo 是否支持目标 JDK    |

建议升级路线如下：

1. 先在本地和 CI 中引入 JDK17。
2. 升级 Maven、Gradle、编译插件、测试插件。
3. 修复编译错误。
4. 升级不兼容的第三方依赖。
5. 处理非法反射访问和 JDK 内部 API。
6. 补充核心业务回归测试。
7. 在测试环境压测并观察 GC、线程、内存和接口耗时。
8. 灰度发布，保留回滚方案。

JDK8 到 JDK17 语法采用建议如下：

| 特性                | 建议                                    |
| ------------------- | --------------------------------------- |
| var                 | 局部使用，不要滥用                      |
| switch 表达式       | 推荐用于状态转换                        |
| 文本块              | 推荐用于 SQL、JSON、HTML 示例           |
| instanceof 模式匹配 | 推荐替代强制类型转换                    |
| Record              | 推荐用于 DTO、VO，不建议直接替代 Entity |
| Sealed Class        | 适合固定类型体系，不宜滥用              |
| Helpful NPE         | 作为诊断增强，无需主动编码适配          |

### JDK17 到 JDK21 升级重点

JDK17 到 JDK21 的升级重点主要在并发模型、模式匹配和集合 API。相比 JDK8 到 JDK17，这一阶段对传统代码的破坏性通常更小，但对高并发架构和代码表达能力提升明显。

JDK21 的重要变化包括 Record Patterns、Pattern Matching for switch、Virtual Threads 和 Sequenced Collections。Oracle 迁移文档也将 Virtual Threads 和 Sequenced Collections 列为 JDK21 的重要库改进。([Oracle Docs](https://docs.oracle.com/en/java/javase/26/migrate/significant-changes-jdk-21.html?utm_source=chatgpt.com))

升级前重点检查以下内容：

| 检查项           | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 框架兼容性       | Spring Boot、Netty、Tomcat、数据库驱动是否支持 JDK21 |
| 监控工具         | APM、Agent、Profiler 是否支持虚拟线程                |
| 线程模型         | 是否存在大量固定线程池和阻塞调用                     |
| ThreadLocal 使用 | 是否依赖线程池复用 ThreadLocal                       |
| 连接池配置       | 数据库、Redis、HTTP 客户端连接池是否合理             |
| 编译插件         | 是否将 release 设置为 21                             |
| 容器镜像         | 基础镜像是否升级到 JDK21                             |
| 压测脚本         | 是否覆盖高并发 IO 场景                               |

JDK17 到 JDK21 语法采用建议如下：

| 特性                        | 建议                                                |
| --------------------------- | --------------------------------------------------- |
| Record Pattern              | 可用于 Record 数据解构                              |
| Pattern Matching for switch | 可用于类型分支处理                                  |
| Sequenced Collections       | 可用于有序集合首尾访问                              |
| Virtual Threads             | 可在 IO 密集型场景评估                              |
| Structured Concurrency      | Preview，不建议核心业务依赖                         |
| Scoped Values               | JDK21 仍是 Preview，不建议核心业务依赖              |
| String Templates            | 预览阶段且后续未作为 JDK25 正式特性推广，不建议使用 |

虚拟线程改造建议分阶段进行：

1. 先识别 IO 密集型接口。
2. 找出大量阻塞调用的位置，例如 HTTP、JDBC、Redis、RPC。
3. 保持业务代码同步写法，不要同时引入过多异步框架。
4. 使用小范围接口试点虚拟线程。
5. 压测数据库连接池、HTTP 连接池和下游服务容量。
6. 观察吞吐、延迟、错误率、线程数和内存占用。
7. 稳定后再扩大使用范围。

### JDK21 到 JDK25 升级重点

JDK21 到 JDK25 的升级重点是语言简化、构造器增强、模块导入、小程序入口简化和 Scoped Values 正式化。根据 Oracle Java 25 语言变更摘要，JDK25 中 Module Import Declarations、Compact Source Files and Instance main Methods、Flexible Constructor Bodies 是正式特性，而 Primitive Types in Patterns, instanceof, and switch 仍是 Preview 特性。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/language/java-language-changes-summary.html?utm_source=chatgpt.com))

升级前重点检查以下内容：

| 检查项        | 说明                                               |
| ------------- | -------------------------------------------------- |
| 构建工具      | Maven、Gradle 是否支持 JDK25                       |
| 插件兼容性    | 编译、测试、打包、代码扫描插件是否兼容             |
| 运行镜像      | Docker 基础镜像是否提供 JDK25                      |
| 框架支持      | Spring Boot、应用服务器、驱动和 Agent 是否明确支持 |
| Preview 使用  | 是否误用 JDK25 Preview 特性                        |
| 虚拟线程监控  | 继续验证 JDK21 后的线程监控与诊断                  |
| Scoped Values | 评估是否替代部分 ThreadLocal 场景                  |
| 入口简化语法  | 明确只用于工具或教学，不替代标准项目结构           |

JDK21 到 JDK25 语法采用建议如下：

| 特性                           | 建议                                   |
| ------------------------------ | -------------------------------------- |
| Unnamed Variables and Patterns | JDK22 已正式，可用于未使用变量         |
| Flexible Constructor Bodies    | JDK25 正式，可用于构造器前置校验       |
| Module Import Declarations     | JDK25 正式，更适合教学和小工具         |
| Compact Source Files           | JDK25 正式，适合单文件程序             |
| Instance Main Methods          | JDK25 正式，适合教学和工具代码         |
| Scoped Values                  | JDK25 正式，可评估替代部分 ThreadLocal |
| Primitive Types in Patterns    | JDK25 仍是 Preview，不建议生产使用     |

JDK25 适合在以下场景中优先验证：

- 内部工具项目。
- 单文件脚本类工具。
- 高并发 IO 服务。
- 需要优化 ThreadLocal 上下文传递的服务。
- 新项目技术预研。
- 教学、培训和代码示例工程。

对于核心生产业务系统，如果当前已经稳定运行在 JDK21，可以先等待框架、插件、APM、容器镜像和团队规范成熟后，再规划升级到 JDK25。

### Preview 特性使用边界

Preview 特性是已经完整指定并实现、但仍处于临时状态的 Java 语言、虚拟机或 API 特性。Java 语言规范说明，Preview 特性需要开发者反馈，后续可能转正，也可能调整或撤回；并且只有在编译时启用 Preview 后，相关语言特性才会纳入编译。([Oracle Docs](https://docs.oracle.com/javase/specs/jls/se25/html/jls-1.html?utm_source=chatgpt.com))

项目中应明确区分以下几类特性：

| 类型                 | 使用建议               |
| -------------------- | ---------------------- |
| 正式特性             | 可以按团队规范逐步使用 |
| Preview 语言特性     | 不建议进入生产核心代码 |
| Preview API          | 不建议核心业务依赖     |
| Incubator 模块       | 仅建议技术预研         |
| 已停止推进的预览特性 | 不建议使用             |

Preview 特性使用边界建议如下：

| 场景         | 建议                           |
| ------------ | ------------------------------ |
| 生产核心业务 | 禁止使用 Preview               |
| 内部技术预研 | 可以使用                       |
| 培训示例     | 可以使用，但必须标注版本和参数 |
| 临时工具     | 可以谨慎使用                   |
| 公共 SDK     | 禁止使用 Preview               |
| 基础框架     | 禁止依赖 Preview API           |
| 单元测试实验 | 可以使用，但不要影响主构建     |

如果确实需要试用 Preview 特性，应满足以下条件：

1. 代码位于独立实验模块。
2. Maven 或 Gradle 配置中明确启用 Preview。
3. CI 构建明确区分正式模块和实验模块。
4. 文档中标明 JDK 版本和 Preview 状态。
5. 升级 JDK 时必须重新验证语法和 API 是否变化。
6. 不将 Preview 代码打包进核心生产制品。

示例命令如下：

```bash
javac --release 25 --enable-preview src/main/java/io/github/atengk/PreviewExample.java
java --enable-preview io.github.atengk.PreviewExample
```

Maven 配置中也需要显式启用 Preview。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <!-- Java 编译插件，用于指定 JDK 版本和 Preview 参数 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <!-- 指定编译目标版本 -->
                <release>25</release>
                <!-- 启用 Preview 特性，仅实验模块建议使用 -->
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 编译参数与构建配置

JDK 升级必须同步调整编译参数和构建配置。对于 Maven 项目，推荐使用 `maven.compiler.release` 或 `maven-compiler-plugin` 的 `<release>` 配置，而不是只配置 `source` 和 `target`。Maven Compiler Plugin 文档说明，JDK9 之后 `javac` 的 `--release` 可以同时约束语言规则、生成的 class 目标版本，以及可使用的目标版本公共 API；相比旧的 `-source` 和 `-target`，它能在使用目标版本不存在的 API 时直接报错。([maven.apache.org](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html?utm_source=chatgpt.com))

JDK17 项目 Maven 配置如下。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- 编译目标 JDK 版本 -->
    <maven.compiler.release>17</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <!-- Java 编译插件，用于控制源码编译版本 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <!-- 使用 release 而不是 source/target，避免误用高版本 API -->
                <release>${maven.compiler.release}</release>
            </configuration>
        </plugin>

        <plugin>
            <!-- 单元测试插件，升级 JDK 后建议同步升级 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
        </plugin>
    </plugins>
</build>
```

JDK21 项目 Maven 配置如下。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- 编译目标 JDK 版本 -->
    <maven.compiler.release>21</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <!-- Java 编译插件，用于控制 JDK21 编译目标 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <!-- 编译为 Java 21 目标版本 -->
                <release>${maven.compiler.release}</release>
            </configuration>
        </plugin>

        <plugin>
            <!-- 单元测试插件，用于运行 JUnit、Mockito 等测试 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
        </plugin>
    </plugins>
</build>
```

JDK25 项目 Maven 配置如下。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- 编译目标 JDK 版本 -->
    <maven.compiler.release>25</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <!-- Java 编译插件，用于控制 JDK25 编译目标 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <!-- 编译为 Java 25 目标版本 -->
                <release>${maven.compiler.release}</release>
            </configuration>
        </plugin>

        <plugin>
            <!-- 单元测试插件，JDK 升级后需要确保测试插件兼容 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
        </plugin>
    </plugins>
</build>
```

如果使用 Gradle，可以通过 Java Toolchains 固定 JDK 版本，避免开发环境和 CI 环境 JDK 不一致。

文件位置：`build.gradle`

```groovy
plugins {
    id 'java'
}

java {
    toolchain {
        // 指定项目使用 Java 21 工具链
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    // 指定源码编码
    options.encoding = 'UTF-8'
    // 指定 release，避免误用高版本 API
    options.release = 21
}

tasks.withType(Test).configureEach {
    // 使用 JUnit Platform 运行测试
    useJUnitPlatform()
}
```

升级后的验证命令如下：

```bash
# 查看当前 JDK 版本
java -version

# 查看 Maven 使用的 JDK
mvn -version

# 清理并编译项目
mvn clean compile

# 执行单元测试
mvn test

# 打包项目
mvn clean package -DskipTests
```

命令说明：

| 命令                | 说明                      |
| ------------------- | ------------------------- |
| `java -version`     | 确认运行环境 JDK 版本     |
| `mvn -version`      | 确认 Maven 实际使用的 JDK |
| `mvn clean compile` | 验证主代码编译是否通过    |
| `mvn test`          | 验证单元测试是否通过      |
| `mvn clean package` | 验证完整打包流程是否正常  |

构建配置建议如下：

| 配置项       | 建议                                         |
| ------------ | -------------------------------------------- |
| 编译版本     | 使用 `<release>` 或 `maven.compiler.release` |
| Maven 版本   | 升级到兼容目标 JDK 的版本                    |
| Gradle 版本  | 使用支持目标 JDK 的版本                      |
| 测试插件     | 同步升级 Surefire、Failsafe、JaCoCo          |
| Lombok       | 升级到支持目标 JDK 的版本                    |
| Docker 镜像  | 使用明确 JDK 版本标签                        |
| CI 环境      | 固定 JDK，不依赖机器默认版本                 |
| Preview 参数 | 默认禁止，只在实验模块启用                   |





## 编码规范建议

Java 新语法应以提升代码可读性、降低维护成本、减少样板代码为目标。团队在引入新语法时，需要明确哪些场景推荐使用，哪些场景限制使用，哪些场景禁止使用，避免不同开发人员在同一项目中形成风格割裂。

本章节从 `var`、`Stream`、`Record`、`Pattern Matching`、`Virtual Threads` 和 `Preview` 特性几个方面给出编码规范建议。

### var 使用规范

`var` 用于局部变量类型推断，可以减少重复类型声明，但不能牺牲代码可读性。使用 `var` 的核心原则是：右侧表达式能够清晰表达变量类型时可以使用；如果变量类型对理解业务逻辑很重要，则应显式声明类型。

推荐使用场景如下：

| 场景                 | 示例                                               | 建议     |
| -------------------- | -------------------------------------------------- | -------- |
| 右侧构造方法类型明确 | `var userList = new ArrayList<User>();`            | 推荐     |
| for 循环遍历         | `for (var user : userList)`                        | 可以使用 |
| try-with-resources   | `try (var input = Files.newInputStream(path))`     | 可以使用 |
| 泛型声明较长         | `var userMap = new HashMap<Long, List<UserVO>>();` | 可以使用 |
| 临时变量类型明显     | `var count = userList.size();`                     | 可选     |

不推荐使用场景如下：

| 场景               | 示例                                    | 原因                                          |
| ------------------ | --------------------------------------- | --------------------------------------------- |
| 方法返回类型不明显 | `var result = userService.queryData();` | 需要跳转方法才能知道类型                      |
| 基础类型容易混淆   | `var amount = 1;`                       | 无法直观看出是否需要 long、double、BigDecimal |
| 业务核心变量       | `var order = buildOrder();`             | 订单对象类型应明确                            |
| 多态返回值         | `var service = getService();`           | 不利于判断真实抽象层级                        |
| null 初始化        | `var data = null;`                      | 编译不通过                                    |

推荐写法：

```java
List<UserVO> userVoList = userService.listUserVO();

var usernameList = new ArrayList<String>();
var userMap = new HashMap<Long, List<UserVO>>();

for (var userVO : userVoList) {
    System.out.println(userVO.username());
}
```

不推荐写法：

```java
var result = userService.getResult();
var data = remoteClient.query();
var value = calculate();
```

在团队规范中，可以按以下标准约束 `var`：

| 规范项               | 建议                                |
| -------------------- | ----------------------------------- |
| DTO、VO、Entity 变量 | 优先显式声明类型                    |
| 集合构造变量         | 可以使用 `var`                      |
| 循环变量             | 可以使用 `var`                      |
| 方法返回结果         | 返回类型不直观时禁止使用 `var`      |
| 公共示例代码         | 初学者文档中少用 `var`，便于理解    |
| 代码评审             | 重点看 `var` 是否隐藏了重要类型信息 |

### Stream 使用规范

`Stream` 适合处理集合的过滤、映射、排序、去重、分组和聚合。它的优势是让集合处理逻辑更声明式，但如果使用不当，也容易造成调试困难、性能问题和副作用问题。

推荐使用场景如下：

| 场景         | 示例                      |
| ------------ | ------------------------- |
| 集合过滤     | 筛选启用用户              |
| 字段映射     | Entity 转 VO              |
| 排序         | 按创建时间倒序            |
| 分组         | 按用户 ID、状态、类型分组 |
| 聚合         | 金额汇总、数量统计        |
| 去重         | 按 ID 或编码去重          |
| 简单链式转换 | filter + map + collect    |

推荐写法：

```java
List<UserVO> userVoList = userList.stream()
        .filter(user -> user.getStatus() == 1)
        .filter(user -> StrUtil.isNotBlank(user.getUsername()))
        .map(user -> new UserVO(user.getId(), user.getUsername()))
        .collect(Collectors.toList());
```

不推荐写法：

```java
List<UserVO> userVoList = userList.stream()
        .map(user -> {
            updateLoginTime(user);
            sendMessage(user);
            saveLog(user);
            return convert(user);
        })
        .collect(Collectors.toList());
```

上面的写法把数据修改、消息发送、日志保存和对象转换混在 `Stream` 中，副作用过多，不利于排查问题。此类流程应使用普通循环或拆分为明确的业务方法。

Stream 使用规范如下：

| 规范项       | 建议                                    |
| ------------ | --------------------------------------- |
| 链式调用长度 | 不宜过长，超过 5 个主要操作建议拆分     |
| Lambda 内容  | 每个 Lambda 尽量保持一到三行            |
| 副作用操作   | 不建议在 `map`、`filter` 中修改外部状态 |
| 数据库操作   | 不建议在 Stream 中直接执行数据库写入    |
| 远程调用     | 不建议在 Stream 中直接调用 HTTP、RPC    |
| 异常处理     | 复杂异常处理建议使用普通循环            |
| 并行流       | 禁止随意使用 `parallelStream()`         |
| 空集合处理   | 入口处统一处理空集合                    |

对于复杂集合处理，建议拆分中间变量：

```java
List<UserEntity> enabledUserList = userList.stream()
        .filter(user -> user.getStatus() == 1)
        .collect(Collectors.toList());

Map<Long, List<UserEntity>> userGroupMap = enabledUserList.stream()
        .collect(Collectors.groupingBy(UserEntity::getDeptId));
```

相比一条链写到底，拆分变量更方便调试，也更适合代码评审。

### Record 使用规范

`Record` 适合表示不可变数据载体，可以减少 DTO、VO、查询结果对象中的样板代码。它自动生成构造方法、访问器、`equals`、`hashCode` 和 `toString`，适合 JDK16 及以上项目使用。

推荐使用场景如下：

| 场景         | 建议                   |
| ------------ | ---------------------- |
| 接口返回 VO  | 推荐使用               |
| 简单 DTO     | 可以使用               |
| 查询条件对象 | 可以使用               |
| 分组统计结果 | 推荐使用               |
| 内部值对象   | 推荐使用               |
| 配置属性对象 | 可结合框架支持情况使用 |

不推荐使用场景如下：

| 场景                | 原因                                 |
| ------------------- | ------------------------------------ |
| MyBatis-Plus Entity | 通常需要无参构造、Setter、字段填充   |
| JPA Entity          | 需要代理、无参构造、可变字段         |
| 复杂领域对象        | 领域对象通常包含行为和状态变化       |
| 需要频繁修改字段    | Record 默认不可变                    |
| 框架兼容性不明确    | 需要先验证序列化、反序列化和参数绑定 |

推荐写法：

```java
/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserVO(
        Long id,
        String username,
        String mobile,
        String statusName
) {
}
```

带校验和默认值的 Record 可以使用紧凑构造方法：

```java
/**
 * 用户查询条件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserQuery(
        String username,
        Integer status
) {

    public UserQuery {
        if (StrUtil.isNotBlank(username)) {
            username = username.trim();
        }

        if (status == null) {
            status = 1;
        }
    }
}
```

Record 使用规范如下：

| 规范项      | 建议                                               |
| ----------- | -------------------------------------------------- |
| 命名        | 仍按 DTO、VO、Query、Result 等后缀命名             |
| 字段数量    | 不宜过多，字段过多说明对象职责可能不清晰           |
| 业务方法    | 可以有简单派生方法，不应承载复杂业务               |
| 校验逻辑    | 可放在紧凑构造方法中，但不宜过重                   |
| Entity 替换 | 不建议直接将数据库实体改成 Record                  |
| JSON 兼容   | 引入前验证 Jackson、Spring MVC、OpenAPI 等工具支持 |

### Pattern Matching 使用规范

Pattern Matching 包括 `instanceof` 模式匹配、Record Pattern、Pattern Matching for switch 等能力。它适合减少强制类型转换，让类型判断和字段读取更简洁。

推荐使用场景如下：

| 场景                         | 建议                     |
| ---------------------------- | ------------------------ |
| 替代 `instanceof + 强转`     | 推荐                     |
| 处理 sealed interface 子类型 | 推荐                     |
| 处理有限状态类型             | 推荐                     |
| 解构 Record                  | 可以使用                 |
| 复杂类型分发                 | 可以使用，但注意拆分方法 |

推荐写法：

```java
if (value instanceof String text && StrUtil.isNotBlank(text)) {
    return text.trim();
}
```

JDK21 及以上可以使用 `switch` 处理受控类型：

```java
return switch (result) {
    case PayResult.Success success -> "支付成功：" + success.payNo();
    case PayResult.Failure failure -> "支付失败：" + failure.reason();
    case PayResult.Processing processing -> "支付处理中：" + processing.bizNo();
    case null -> "支付结果为空";
};
```

Pattern Matching 使用规范如下：

| 规范项       | 建议                                    |
| ------------ | --------------------------------------- |
| 类型分支数量 | 分支过多时考虑策略模式                  |
| 分支逻辑长度 | 每个 case 不宜包含大量业务逻辑          |
| null 处理    | 需要时显式写 `case null`                |
| sealed 配合  | 固定子类型集合推荐使用 sealed interface |
| Record 解构  | 只在字段数量较少时使用                  |
| 嵌套解构     | 不宜过深，避免可读性下降                |

不推荐写法：

```java
return switch (command) {
    case CreateOrderCommand command -> {
        checkUser(command.userId());
        checkProduct(command.productId());
        lockStock(command.productId());
        createOrder(command);
        sendMessage(command);
        yield "创建成功";
    }
    case CancelOrderCommand command -> {
        queryOrder(command.orderId());
        refund(command.orderId());
        unlockStock(command.orderId());
        updateStatus(command.orderId());
        yield "取消成功";
    }
};
```

这种写法把复杂业务流程压缩进 `switch` 分支，不利于测试和维护。更推荐在分支中调用独立方法：

```java
return switch (command) {
    case CreateOrderCommand createCommand -> handleCreateOrder(createCommand);
    case CancelOrderCommand cancelCommand -> handleCancelOrder(cancelCommand);
};
```

### Virtual Threads 使用规范

`Virtual Threads` 适合 IO 密集型并发任务，可以降低线程创建和阻塞等待的成本。它适合高并发接口、批量远程调用、数据库查询、RPC 调用等阻塞式 IO 场景。

虚拟线程的使用原则是：任务可以很多，但外部资源仍然有限。数据库连接池、Redis 连接池、HTTP 连接池、下游服务限流和 CPU 都不会因为虚拟线程而自动扩容。

推荐使用场景如下：

| 场景           | 建议                       |
| -------------- | -------------------------- |
| IO 密集型接口  | 推荐评估                   |
| 批量 HTTP 调用 | 推荐评估                   |
| 批量 RPC 调用  | 推荐评估                   |
| 数据库查询并发 | 可以评估，但必须关注连接池 |
| 请求级并发任务 | 可以使用                   |
| CPU 密集型计算 | 不推荐依赖虚拟线程提升性能 |

推荐写法：

```java
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<UserInfo> userFuture = executorService.submit(() -> queryUserInfo(userId));
    Future<AccountInfo> accountFuture = executorService.submit(() -> queryAccountInfo(userId));

    UserInfo userInfo = userFuture.get();
    AccountInfo accountInfo = accountFuture.get();

    return new UserProfile(userInfo, accountInfo);
}
```

Virtual Threads 使用规范如下：

| 规范项       | 建议                                 |
| ------------ | ------------------------------------ |
| 线程池       | 不要池化虚拟线程                     |
| 使用方式     | 推荐每个任务一个虚拟线程             |
| 阻塞调用     | 适合传统阻塞式 IO                    |
| 数据库访问   | 必须配合连接池容量评估               |
| ThreadLocal  | 避免大量依赖可变 ThreadLocal         |
| synchronized | 避免长时间持有锁                     |
| 压测         | 必须在上线前压测吞吐、延迟、错误率   |
| 监控         | 确认 APM、日志、线程监控支持虚拟线程 |

不推荐写法：

```java
ExecutorService executorService = Executors.newFixedThreadPool(2000);
```

也不推荐人为构造虚拟线程池复用模型。虚拟线程本身创建成本低，推荐按任务创建，而不是像平台线程一样长期池化。

### Preview 特性禁用规范

Preview 特性是 Java 中尚未最终稳定的语言、API 或 VM 特性。它可能在后续版本中调整、转正或撤回，因此不建议进入生产核心代码。

团队应默认禁用 Preview 特性，只在实验模块、技术预研、培训示例中有限使用。

Preview 特性禁用规范如下：

| 场景                 | 规范                          |
| -------------------- | ----------------------------- |
| 生产核心业务         | 禁止使用                      |
| 公共 SDK             | 禁止使用                      |
| 基础框架             | 禁止使用                      |
| 数据库实体、核心模型 | 禁止使用                      |
| 内部实验模块         | 可以使用，但必须隔离          |
| 技术分享示例         | 可以使用，但必须标注 JDK 版本 |
| 单元测试实验         | 可以使用，但不能影响主构建    |

主项目 Maven 配置中不应开启 Preview：

```xml
<properties>
    <!-- 编译目标 JDK 版本 -->
    <maven.compiler.release>21</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <!-- Java 编译插件，主项目禁止启用 Preview -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <release>${maven.compiler.release}</release>
            </configuration>
        </plugin>
    </plugins>
</build>
```

如果实验模块确实需要启用 Preview，应单独配置，并在模块名称、README 和构建脚本中明确标识：

```xml
<build>
    <plugins>
        <plugin>
            <!-- 实验模块编译插件，仅用于 Preview 特性验证 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.15.0</version>
            <configuration>
                <release>25</release>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>

        <plugin>
            <!-- 测试插件需要同步启用 Preview，否则测试运行会失败 -->
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
            <configuration>
                <argLine>--enable-preview</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Preview 特性使用前必须确认以下内容：

- 是否位于独立实验模块。
- 是否不会被核心业务依赖。
- 是否不会发布到公共 SDK。
- 是否已在文档中标注 JDK 版本。
- 是否已在 CI 中隔离构建。
- 是否有后续 JDK 升级重新验证计划。

## 项目实战章节规划

项目实战章节用于将前文语法点转化为可落地的重构案例。建议以真实开发场景为主线，不按语法点机械堆砌示例，而是围绕“老代码如何改”“为什么这样改”“改完有什么收益”“有哪些风险”展开。

实战章节建议采用统一结构：

| 小节       | 内容                     |
| ---------- | ------------------------ |
| 背景说明   | 当前代码存在的问题       |
| 重构前代码 | 展示传统写法             |
| 重构后代码 | 展示新语法写法           |
| 改造说明   | 说明改动点和收益         |
| 适用边界   | 说明哪些情况不适合这样改 |
| 验证方式   | 提供单元测试或运行方式   |

### 老代码语法重构案例

本节用于展示从 JDK8 老代码或更早写法向现代 Java 写法的迁移过程。重点覆盖匿名内部类、传统循环、空值判断、日期处理和状态分支。

建议案例包括：

| 案例                     | 重构方向                 |
| ------------------------ | ------------------------ |
| 匿名内部类排序           | 改为 Lambda 和方法引用   |
| 传统 for 循环过滤集合    | 改为 Stream              |
| 多层 null 判断           | 改为 Optional 或提前校验 |
| Date 与 SimpleDateFormat | 改为 `java.time`         |
| if-else 状态转换         | 改为 switch 表达式       |
| DTO 手写样板代码         | 改为 Record              |

章节示例结构：

```markdown
### 案例一：匿名内部类排序重构

#### 重构背景

说明旧代码中匿名内部类冗长、阅读成本高。

#### 重构前代码

展示 `Collections.sort` 和 `Comparator` 匿名内部类。

#### 重构后代码

展示 `list.sort(Comparator.comparing(User::getAge))`。

#### 改造说明

说明 Lambda 和方法引用如何减少样板代码。

#### 注意事项

说明复杂排序规则不应过度压缩。
```

### DTO 与 VO 改造案例

本节用于展示 Record 在 DTO、VO 和查询结果对象中的使用方式。重点说明 Record 适合不可变数据载体，不适合直接替代 ORM 实体类。

建议案例包括：

| 案例                  | 说明                          |
| --------------------- | ----------------------------- |
| 用户 VO 改造          | 普通 class 改为 Record        |
| 查询条件对象改造      | Record 紧凑构造方法处理默认值 |
| 统计结果对象改造      | 分组统计结果使用 Record       |
| Entity 保持普通 class | 说明为什么不改成 Record       |
| Controller 返回对象   | 验证 JSON 序列化兼容性        |

章节示例结构：

```markdown
### 案例二：用户 VO 改造为 Record

#### 重构背景

说明原 VO 中存在大量 Getter、构造方法和 toString。

#### 重构前代码

展示普通 UserVO class。

#### 重构后代码

展示 UserVO record。

#### 接口返回验证

通过 curl 请求接口，确认 JSON 正常返回。

#### 注意事项

说明 Entity、复杂领域对象不建议改为 Record。
```

### switch 分支优化案例

本节用于展示从 `if-else`、传统 `switch` 到 switch 表达式、Pattern Matching for switch 的演进。重点说明分支优化不等于把所有业务逻辑塞进 switch，而是让状态转换和类型分派更清晰。

建议案例包括：

| 案例           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 订单状态码转换 | if-else 改为 switch 表达式                     |
| 支付结果处理   | sealed interface + Pattern Matching for switch |
| 审批状态流转   | 枚举 + switch 表达式                           |
| 异常类型映射   | instanceof 模式匹配                            |
| 复杂业务分支   | switch 调用独立方法                            |

章节示例结构：

```markdown
### 案例三：订单状态分支优化

#### 重构背景

说明订单状态判断散落在多个 Service 中。

#### 重构前代码

展示 if-else 状态码判断。

#### 重构后代码

展示 switch 表达式或枚举方法。

#### 改造收益

说明减少遗漏分支、减少 break 问题、集中状态语义。

#### 注意事项

复杂流程应使用策略模式或独立 Service。
```

### Stream 数据处理案例

本节用于展示 Stream 在真实业务集合处理中的使用方式。重点覆盖过滤、映射、分组、去重、排序、聚合和分页前后处理。

建议案例包括：

| 案例           | 说明                               |
| -------------- | ---------------------------------- |
| 用户列表转 VO  | `filter + map + collect`           |
| 订单按用户分组 | `groupingBy`                       |
| 金额汇总       | `reducing` 或 `map + reduce`       |
| 去重处理       | `toMap` 或自定义去重               |
| 排序处理       | `Comparator.comparing`             |
| Stream 反例    | 在 Stream 中写数据库操作或远程调用 |

章节示例结构：

```markdown
### 案例四：订单数据 Stream 统计

#### 重构背景

说明传统循环中包含过滤、分组和金额累加，逻辑较长。

#### 重构前代码

展示普通 for 循环统计。

#### 重构后代码

展示 Stream groupingBy 和 reducing。

#### 性能与可读性说明

说明适合中小规模内存集合，不适合替代数据库聚合。

#### 注意事项

数据量大时应优先 SQL 聚合。
```

### 虚拟线程接口优化案例

本节用于展示 JDK21 虚拟线程在 IO 密集型接口中的使用方式。重点不是简单替换线程池，而是说明虚拟线程适合哪些接口、如何压测、如何评估瓶颈。

建议案例包括：

| 案例             | 说明                     |
| ---------------- | ------------------------ |
| 用户详情聚合接口 | 并发查询用户、账户、权限 |
| 批量远程调用接口 | 每个调用使用一个虚拟线程 |
| 传统线程池对比   | 固定线程池与虚拟线程对比 |
| 连接池瓶颈分析   | 数据库连接池限制说明     |
| 日志与监控验证   | 观察吞吐、延迟、错误率   |

章节示例结构：

```markdown
### 案例五：用户详情接口虚拟线程优化

#### 优化背景

说明接口需要串行调用多个外部服务，整体耗时较长。

#### 优化前代码

展示串行调用用户、账户、权限服务。

#### 优化后代码

展示虚拟线程并发查询。

#### 压测验证

对比平均耗时、P95、P99、错误率和连接池占用。

#### 注意事项

说明虚拟线程不能突破数据库连接池和下游服务限制。
```

### 新语法单元测试案例

本节用于展示新语法改造后的测试方式。语法升级不能只看编译通过，还需要通过单元测试验证行为一致性。尤其是 DTO 改造、switch 分支、Stream 分组聚合、虚拟线程并发处理，都需要对应测试覆盖。

建议案例包括：

| 案例             | 测试重点                   |
| ---------------- | -------------------------- |
| Record VO 测试   | 字段访问、JSON 序列化      |
| switch 分支测试  | 每个状态码覆盖             |
| Stream 统计测试  | 空集合、正常数据、异常数据 |
| Optional 测试    | 空值和非空值               |
| 虚拟线程测试     | 并发执行结果和异常处理     |
| Preview 隔离测试 | 实验模块独立验证           |

以下是 switch 状态转换的单元测试示例。

文件位置：`src/test/java/io/github/atengk/application/order/enums/OrderStatusTest.java`

```java
package io.github.atengk.application.order.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 订单状态测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class OrderStatusTest {

    @Test
    void shouldReturnStatusNameWhenCodeValid() {
        Assertions.assertEquals("待支付", OrderStatus.getNameByCode(0));
        Assertions.assertEquals("已支付", OrderStatus.getNameByCode(1));
        Assertions.assertEquals("已发货", OrderStatus.getNameByCode(2));
        Assertions.assertEquals("已完成", OrderStatus.getNameByCode(3));
        Assertions.assertEquals("已取消", OrderStatus.getNameByCode(4));
    }

    @Test
    void shouldReturnUnknownWhenCodeInvalid() {
        Assertions.assertEquals("未知", OrderStatus.getNameByCode(null));
        Assertions.assertEquals("未知", OrderStatus.getNameByCode(999));
    }
}
```

以下是 Stream 数据统计的单元测试示例。

文件位置：`src/test/java/io/github/atengk/application/order/service/OrderStatisticsServiceTest.java`

```java
package io.github.atengk.application.order.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单统计服务测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class OrderStatisticsServiceTest {

    private final OrderStatisticsService orderStatisticsService = new OrderStatisticsService();

    @Test
    void shouldSummaryPaidOrderByUser() {
        List<OrderStatisticsService.OrderInfo> orderList = List.of(
                new OrderStatisticsService.OrderInfo(1L, 1001L, 1, new BigDecimal("10.00")),
                new OrderStatisticsService.OrderInfo(2L, 1001L, 1, new BigDecimal("20.00")),
                new OrderStatisticsService.OrderInfo(3L, 1002L, 0, new BigDecimal("30.00")),
                new OrderStatisticsService.OrderInfo(4L, 1003L, 1, new BigDecimal("40.00"))
        );

        List<OrderStatisticsService.UserOrderSummary> summaryList =
                orderStatisticsService.summaryPaidOrderByUser(orderList);

        Assertions.assertEquals(2, summaryList.size());
        Assertions.assertEquals(1003L, summaryList.get(0).userId());
        Assertions.assertEquals(new BigDecimal("40.00"), summaryList.get(0).totalAmount());
    }

    @Test
    void shouldReturnEmptyListWhenOrderListEmpty() {
        List<OrderStatisticsService.UserOrderSummary> summaryList =
                orderStatisticsService.summaryPaidOrderByUser(List.of());

        Assertions.assertTrue(summaryList.isEmpty());
    }
}
```

单元测试规划建议如下：

| 测试对象         | 测试重点                   |
| ---------------- | -------------------------- |
| Record           | 构造、访问器、默认值处理   |
| switch 表达式    | 所有分支和 default         |
| Pattern Matching | 每种子类型和 null          |
| Stream           | 空集合、重复数据、异常数据 |
| Optional         | 空值、默认值、异常抛出     |
| Virtual Threads  | 正常结果、中断、异常传播   |
| 构建配置         | 不同 JDK 版本编译验证      |

## 总结

Java 从 JDK8 到 JDK25 的语法演进，可以概括为三个方向：减少样板代码、增强类型表达能力、改进并发编程模型。JDK8 提供函数式编程基础，JDK17 形成现代语法稳定阶段，JDK21 强化模式匹配和虚拟线程，JDK25 继续推进语法简化和上下文传递能力。

对于实际项目，语法升级应服务于工程质量，而不是追求新语法覆盖率。团队应优先在 DTO、VO、集合处理、状态转换、并发查询和工具代码中引入稳定特性，对核心业务流程、ORM 实体、公共 SDK 和基础框架保持谨慎。

### 推荐掌握顺序

建议按照版本稳定性和项目实用性分阶段掌握。

第一阶段：JDK8 必须掌握。

| 特性           | 掌握目标                                       |
| -------------- | ---------------------------------------------- |
| Lambda 表达式  | 能替代匿名内部类和简单行为传递                 |
| 方法引用       | 能简化已有方法调用                             |
| 函数式接口     | 能理解 Predicate、Function、Consumer、Supplier |
| Stream API     | 能完成过滤、映射、排序、分组和聚合             |
| Optional       | 能处理可能为空的返回值                         |
| 新日期时间 API | 能替代 Date、Calendar、SimpleDateFormat        |
| 接口默认方法   | 能理解接口兼容升级方式                         |

第二阶段：JDK9 到 JDK17 优先掌握。

| 特性                         | 掌握目标                        |
| ---------------------------- | ------------------------------- |
| 集合工厂方法                 | 快速创建不可变集合              |
| var                          | 合理使用局部变量类型推断        |
| switch 表达式                | 简化状态分支返回值              |
| 文本块                       | 编写 SQL、JSON、HTML 多行字符串 |
| instanceof 模式匹配          | 减少强制类型转换                |
| Record                       | 简化 DTO、VO、查询结果对象      |
| Sealed Class                 | 控制固定子类型集合              |
| Helpful NullPointerException | 提升空指针排查效率              |

第三阶段：JDK18 到 JDK21 重点掌握。

| 特性                        | 掌握目标                     |
| --------------------------- | ---------------------------- |
| Record Pattern              | 解构 Record 数据             |
| Pattern Matching for switch | 处理类型分支和 sealed 子类型 |
| Sequenced Collections       | 统一访问有序集合首尾元素     |
| Virtual Threads             | 优化 IO 密集型并发任务       |
| Structured Concurrency      | 了解结构化并发思想           |
| Scoped Values               | 了解上下文传递新模型         |

第四阶段：JDK22 到 JDK25 选择性掌握。

| 特性                           | 掌握目标                             |
| ------------------------------ | ------------------------------------ |
| Unnamed Variables and Patterns | 表达故意不使用的变量                 |
| Flexible Constructor Bodies    | 构造器前置校验和初始化               |
| Module Import Declarations     | 理解模块级导入能力                   |
| Compact Source Files           | 编写单文件工具和教学示例             |
| Instance Main Methods          | 简化小程序入口                       |
| Scoped Values 正式特性         | 在 JDK25 中评估替代部分 ThreadLocal  |
| Primitive Types in Patterns    | 了解即可，Preview 阶段不建议生产使用 |

综合学习顺序建议如下：

1. Lambda 表达式。
2. 函数式接口。
3. Stream API。
4. Optional。
5. 新日期时间 API。
6. switch 表达式。
7. 文本块。
8. instanceof 模式匹配。
9. Record。
10. Sealed Class。
11. Pattern Matching for switch。
12. Sequenced Collections。
13. Virtual Threads。
14. Scoped Values。
15. JDK25 小程序语法和构造器增强。

### 生产环境采用建议

生产环境采用新语法时，应按“正式特性优先、局部改造优先、可回滚优先”的原则推进。

推荐采用的稳定特性如下：

| JDK 版本 | 推荐特性                                                     | 采用建议                    |
| -------- | ------------------------------------------------------------ | --------------------------- |
| JDK8     | Lambda、Stream、Optional、java.time                          | 可广泛使用                  |
| JDK9     | 集合工厂方法、接口私有方法                                   | 可局部使用                  |
| JDK10    | var                                                          | 限制性使用                  |
| JDK14    | switch 表达式、Helpful NPE                                   | 推荐使用                    |
| JDK15    | 文本块                                                       | 推荐用于多行字符串          |
| JDK16    | Record、instanceof 模式匹配                                  | 推荐用于 DTO、VO 和类型判断 |
| JDK17    | Sealed Class                                                 | 适合固定类型体系            |
| JDK21    | Virtual Threads、Sequenced Collections、Pattern Matching for switch | 可评估使用                  |
| JDK22    | Unnamed Variables and Patterns                               | 可局部使用                  |
| JDK25    | Scoped Values、Flexible Constructor Bodies                   | 可在新项目中评估使用        |

生产环境不建议采用的内容如下：

| 类型                  | 原因                     |
| --------------------- | ------------------------ |
| Preview 特性          | 后续版本可能变化         |
| 已停止推进的预览特性  | 不具备长期维护价值       |
| 大量紧凑源文件        | 不适合标准工程结构       |
| 滥用 var              | 会隐藏重要类型信息       |
| 滥用 Stream           | 会降低调试和异常处理能力 |
| 滥用 Pattern Matching | 可能替代掉合理的多态设计 |
| 无压测引入虚拟线程    | 可能暴露连接池和下游瓶颈 |

项目采用建议如下：

| 项目类型         | 建议                                             |
| ---------------- | ------------------------------------------------ |
| JDK8 老项目      | 优先规范使用 Lambda、Stream、Optional、java.time |
| JDK17 新项目     | 推荐使用 Record、switch 表达式、文本块、模式匹配 |
| JDK21 高并发项目 | 可评估虚拟线程，但必须压测                       |
| JDK25 工具项目   | 可使用紧凑源文件、实例 main、模块导入            |
| 核心交易系统     | 保守引入新语法，优先稳定性                       |
| 公共 SDK         | 避免 Preview 和过新语法，降低使用方门槛          |
| 内部实验项目     | 可跟进新版本语法，但必须与主项目隔离             |

上线前检查清单如下：

| 检查项       | 说明                                 |
| ------------ | ------------------------------------ |
| 编译版本     | Maven 或 Gradle 已固定 release       |
| 依赖兼容     | 框架、插件、Agent、驱动已验证        |
| 单元测试     | 覆盖新语法改造后的核心逻辑           |
| 回归测试     | 核心业务流程已验证                   |
| 性能压测     | 虚拟线程、集合处理、大对象转换已压测 |
| 日志监控     | 异常、线程、GC、接口耗时可观测       |
| 回滚方案     | 保留旧版本构建和部署方案             |
| Preview 检查 | 主项目未启用 `--enable-preview`      |

### 后续学习方向

完成本文内容后，可以从语言特性、项目重构、并发模型和工程升级四个方向继续深入。

语言特性方向：

| 学习内容                    | 目标                                        |
| --------------------------- | ------------------------------------------- |
| Java Language Specification | 理解语法规则背后的设计                      |
| JEP 文档                    | 跟踪新特性的目标、动机和限制                |
| 模式匹配体系                | 系统理解 instanceof、switch、Record Pattern |
| Record 与不可变对象         | 理解值对象、DTO、领域模型边界               |
| Sealed Class                | 理解受控继承和类型穷尽性                    |

项目重构方向：

| 学习内容     | 目标                                                |
| ------------ | --------------------------------------------------- |
| 老代码重构   | 将匿名内部类、传统循环、旧日期 API 迁移到现代写法   |
| DTO/VO 规范  | 明确 Entity、DTO、VO、Record 的使用边界             |
| 状态模型设计 | 使用枚举、sealed interface 和 switch 表达式优化分支 |
| 集合处理规范 | 控制 Stream 使用复杂度                              |
| 单元测试补强 | 用测试保障语法重构行为不变                          |

并发模型方向：

| 学习内容               | 目标                       |
| ---------------------- | -------------------------- |
| Virtual Threads        | 理解虚拟线程适用场景和限制 |
| Structured Concurrency | 理解结构化任务编排思想     |
| Scoped Values          | 理解上下文传递新方式       |
| ThreadLocal 迁移       | 评估哪些场景可以替代       |
| 连接池与限流           | 理解虚拟线程下外部资源瓶颈 |

工程升级方向：

| 学习内容              | 目标                                      |
| --------------------- | ----------------------------------------- |
| Maven Compiler Plugin | 掌握 release、source、target 的区别       |
| Gradle Toolchains     | 固定项目 JDK 工具链                       |
| Spring Boot 版本升级  | 理解 JDK、Spring Boot、Jakarta 之间的关系 |
| Docker JDK 镜像       | 固定运行环境                              |
| CI/CD JDK 管理        | 保证本地、测试、生产环境一致              |
| APM 与监控适配        | 验证高版本 JDK 和虚拟线程可观测性         |

最终建议是：先把 JDK8 的函数式基础用规范，再把 JDK17 的稳定语法用于业务代码，随后在 JDK21 中重点验证虚拟线程和模式匹配，最后在 JDK25 中选择性评估 Scoped Values、构造器增强和小程序语法。对于团队项目，语法升级必须和编码规范、单元测试、构建配置、依赖治理、性能压测一起推进。
