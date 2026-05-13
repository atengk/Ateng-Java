# 函数式接口

函数式接口是 Java 8 引入函数式编程能力的重要基础。它主要用于承载 Lambda 表达式、方法引用以及 Stream API 中的行为参数，使业务代码能够以“传递行为”的方式进行抽象。

## 函数式接口概述

本节说明函数式接口的基本概念、识别方式以及它与传统普通接口之间的差异。理解函数式接口后，才能正确使用 Lambda 表达式、方法引用和 Java 8 内置函数式接口。

### 基本定义

函数式接口是指只包含一个抽象方法的接口。这个抽象方法也称为函数式方法或 SAM 方法，SAM 是 Single Abstract Method 的缩写。

函数式接口可以使用 `@FunctionalInterface` 注解进行标记。该注解不是必须的，但建议在自定义函数式接口时添加，因为它可以让编译器检查接口是否符合函数式接口规范。

下面定义了一个简单的函数式接口，用于处理字符串并返回处理后的结果。

文件位置：`src/main/java/io/github/atengk/functional/StringProcessor.java`

```java
package io.github.atengk.functional;

/**
 * 字符串处理函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface StringProcessor {

    /**
     * 处理字符串
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    String process(String value);
}
```

使用时，可以通过 Lambda 表达式直接传入具体处理逻辑。

文件位置：`src/main/java/io/github/atengk/functional/StringProcessorDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串处理函数式接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class StringProcessorDemo {

    public static void main(String[] args) {
        StringProcessor trimProcessor = value -> StrUtil.trim(value);
        StringProcessor upperProcessor = value -> StrUtil.upperFirst(value);

        System.out.println(trimProcessor.process("  hello java8  "));
        System.out.println(upperProcessor.process("java8"));
    }
}
```

函数式接口本质上仍然是接口，只是它满足“只有一个抽象方法”的结构要求，因此可以作为 Lambda 表达式和方法引用的目标类型。

### 核心特征

函数式接口的核心特征是接口中只能有一个抽象方法，但可以包含多个默认方法、静态方法，以及来自 `Object` 类的公共方法声明。

常见特征如下：

| 特征                            | 说明                                            |
| ------------------------------- | ----------------------------------------------- |
| 只有一个抽象方法                | 这是函数式接口最核心的规则                      |
| 可以使用 `@FunctionalInterface` | 用于让编译器校验接口结构                        |
| 可以包含默认方法                | `default` 方法不影响函数式接口判定              |
| 可以包含静态方法                | `static` 方法不影响函数式接口判定               |
| 可以声明 `Object` 中的方法      | 如 `toString()`、`equals()`，不计入抽象方法数量 |
| 可以被 Lambda 表达式实现        | Lambda 表达式会实现唯一的抽象方法               |

下面这个接口仍然是合法的函数式接口，因为真正需要子类实现的抽象方法只有 `calculate` 一个。

文件位置：`src/main/java/io/github/atengk/functional/NumberCalculator.java`

```java
package io.github.atengk.functional;

/**
 * 数字计算函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface NumberCalculator {

    /**
     * 执行计算
     *
     * @param left 左值
     * @param right 右值
     * @return 计算结果
     */
    Integer calculate(Integer left, Integer right);

    /**
     * 判断计算结果是否为正数
     *
     * @param result 计算结果
     * @return 是否为正数
     */
    default boolean isPositive(Integer result) {
        return result != null && result > 0;
    }

    /**
     * 获取默认结果
     *
     * @return 默认结果
     */
    static Integer defaultResult() {
        return 0;
    }

    @Override
    String toString();
}
```

需要注意的是，如果接口中存在两个普通抽象方法，编译器会认为它不是函数式接口。此时如果加了 `@FunctionalInterface` 注解，代码会直接编译失败。

### 与普通接口的区别

函数式接口和普通接口在语法上都属于 Java 接口，但设计目标不同。普通接口通常用于定义一组能力或行为规范，而函数式接口通常用于抽象一段可传递的行为逻辑。

| 对比项       | 函数式接口                                           | 普通接口                                     |
| ------------ | ---------------------------------------------------- | -------------------------------------------- |
| 抽象方法数量 | 只能有一个抽象方法                                   | 可以有多个抽象方法                           |
| 主要用途     | 配合 Lambda、方法引用、Stream 使用                   | 定义对象能力、模块规范、服务契约             |
| 实现方式     | 可以使用 Lambda 表达式、方法引用、匿名内部类、普通类 | 通常使用普通实现类或匿名内部类               |
| 代码风格     | 更适合行为参数化                                     | 更适合面向对象建模                           |
| 典型示例     | `Consumer`、`Supplier`、`Function`、`Predicate`      | `List`、`Map`、`Runnable`、业务 Service 接口 |

普通接口示例：

```java
public interface UserService {

    UserInfo getById(Long id);

    Boolean save(UserInfo userInfo);

    Boolean removeById(Long id);
}
```

这个接口不是函数式接口，因为它包含多个抽象方法。它更适合作为业务服务契约。

函数式接口示例：

```java
@FunctionalInterface
public interface UserValidator {

    Boolean validate(UserInfo userInfo);
}
```

这个接口只有一个抽象方法，更适合用于参数校验、业务规则判断、条件过滤等场景。

在项目开发中，应避免为了使用 Lambda 而强行把复杂业务接口设计成函数式接口。函数式接口更适合表达单一、清晰、可组合的行为。

## Java8 函数式编程基础

本节说明 Java 8 中函数式编程的三项基础能力：Lambda 表达式、方法引用、默认方法与静态方法。函数式接口是这些能力的类型基础，Lambda 和方法引用则是函数式接口的常用实现方式。

### Lambda 表达式

Lambda 表达式用于简化函数式接口的实现。它可以把一段行为作为参数传递给方法，从而减少匿名内部类带来的样板代码。

Lambda 表达式的基本语法如下：

```java
(参数列表) -> {
    方法体
}
```

当方法体只有一行表达式时，可以省略大括号和 `return`。

```java
(value) -> value.trim()
```

当只有一个参数时，还可以省略参数的小括号。

```java
value -> value.trim()
```

下面通过一个订单金额计算场景说明 Lambda 的基本使用方式。

文件位置：`src/main/java/io/github/atengk/functional/DiscountCalculator.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;

/**
 * 折扣计算函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface DiscountCalculator {

    /**
     * 计算折扣金额
     *
     * @param amount 原始金额
     * @return 折扣后金额
     */
    BigDecimal calculate(BigDecimal amount);
}
```

下面的代码演示不同折扣策略如何通过 Lambda 表达式传入。

文件位置：`src/main/java/io/github/atengk/functional/DiscountCalculatorDemo.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;

/**
 * 折扣计算 Lambda 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class DiscountCalculatorDemo {

    public static void main(String[] args) {
        BigDecimal amount = new BigDecimal("100.00");

        DiscountCalculator noDiscount = value -> value;
        DiscountCalculator vipDiscount = value -> value.multiply(new BigDecimal("0.80"));
        DiscountCalculator newUserDiscount = value -> value.subtract(new BigDecimal("10.00"));

        System.out.println("无折扣金额：" + noDiscount.calculate(amount));
        System.out.println("会员折扣金额：" + vipDiscount.calculate(amount));
        System.out.println("新用户折扣金额：" + newUserDiscount.calculate(amount));
    }
}
```

Lambda 表达式常用于以下场景：

| 场景     | 示例                         |
| -------- | ---------------------------- |
| 集合遍历 | `list.forEach(item -> ...)`  |
| 条件过滤 | `stream.filter(item -> ...)` |
| 数据转换 | `stream.map(item -> ...)`    |
| 业务回调 | `execute(() -> ...)`         |
| 策略选择 | `calculate(amount -> ...)`   |

在实际开发中，Lambda 表达式应该保持简短。如果 Lambda 内部包含大量业务逻辑，应抽取为独立方法，再通过方法引用或普通方法调用提升可读性。

### 方法引用

方法引用是 Lambda 表达式的进一步简化形式。当前 Lambda 表达式只是调用一个已有方法时，可以使用方法引用替代。

方法引用的基本语法是：

```java
类名或对象名::方法名
```

常见方法引用类型如下：

| 类型                 | 语法                 | 示例                |
| -------------------- | -------------------- | ------------------- |
| 静态方法引用         | `类名::静态方法名`   | `Integer::parseInt` |
| 实例方法引用         | `对象名::实例方法名` | `printer::print`    |
| 特定类型实例方法引用 | `类名::实例方法名`   | `String::trim`      |
| 构造方法引用         | `类名::new`          | `ArrayList::new`    |

下面示例演示静态方法引用和实例方法引用的使用方式。

文件位置：`src/main/java/io/github/atengk/functional/MethodReferenceDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法引用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MethodReferenceDemo {

    public static void main(String[] args) {
        List<String> names = CollUtil.newArrayList("  zhangsan  ", "  lisi  ", "  wangwu  ");

        List<String> trimNames = names.stream()
                .map(StrUtil::trim)
                .collect(Collectors.toList());

        trimNames.forEach(System.out::println);
    }
}
```

上面的 `StrUtil::trim` 等价于：

```java
value -> StrUtil.trim(value)
```

`System.out::println` 等价于：

```java
value -> System.out.println(value)
```

构造方法引用适合用于对象创建场景。下面示例将用户名列表转换为用户对象列表。

文件位置：`src/main/java/io/github/atengk/functional/UserInfo.java`

```java
package io.github.atengk.functional;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserInfo {

    private String username;

    public UserInfo() {
    }

    public UserInfo(String username) {
        this.username = username;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
}
```

文件位置：`src/main/java/io/github/atengk/functional/ConstructorReferenceDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 构造方法引用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ConstructorReferenceDemo {

    public static void main(String[] args) {
        List<String> usernames = CollUtil.newArrayList("admin", "ateng", "test");

        List<UserInfo> userList = usernames.stream()
                .map(UserInfo::new)
                .collect(Collectors.toList());

        userList.forEach(user -> System.out.println(user.getUsername()));
    }
}
```

方法引用适合替代简单、直接的 Lambda 表达式。如果方法引用会降低代码理解成本，可以优先使用；如果业务逻辑需要额外判断、组合或异常处理，则直接使用 Lambda 更清晰。

### 默认方法与静态方法

Java 8 允许接口中定义默认方法和静态方法。默认方法使用 `default` 修饰，静态方法使用 `static` 修饰。它们都不会破坏函数式接口的定义，因为它们不是抽象方法。

默认方法主要用于为接口提供通用逻辑，避免每个实现方重复编写相同代码。静态方法主要用于提供与接口强相关的工具方法或默认实例。

下面定义一个参数校验函数式接口，并提供默认方法和静态方法。

文件位置：`src/main/java/io/github/atengk/functional/Validator.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

/**
 * 参数校验函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * 校验数据
     *
     * @param data 待校验数据
     * @return 是否通过校验
     */
    boolean validate(T data);

    /**
     * 与另一个校验器组合，两个校验器都通过时返回 true
     *
     * @param other 另一个校验器
     * @return 组合后的校验器
     */
    default Validator<T> and(Validator<T> other) {
        return data -> this.validate(data) && other.validate(data);
    }

    /**
     * 与另一个校验器组合，任意一个校验器通过时返回 true
     *
     * @param other 另一个校验器
     * @return 组合后的校验器
     */
    default Validator<T> or(Validator<T> other) {
        return data -> this.validate(data) || other.validate(data);
    }

    /**
     * 对当前校验结果取反
     *
     * @return 取反后的校验器
     */
    default Validator<T> negate() {
        return data -> !this.validate(data);
    }

    /**
     * 创建非空字符串校验器
     *
     * @return 非空字符串校验器
     */
    static Validator<String> notBlank() {
        return StrUtil::isNotBlank;
    }

    /**
     * 创建最小长度校验器
     *
     * @param minLength 最小长度
     * @return 最小长度校验器
     */
    static Validator<String> minLength(int minLength) {
        return value -> StrUtil.length(value) >= minLength;
    }
}
```

下面演示如何使用默认方法组合多个校验规则。

文件位置：`src/main/java/io/github/atengk/functional/ValidatorDemo.java`

```java
package io.github.atengk.functional;

/**
 * 参数校验函数式接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ValidatorDemo {

    public static void main(String[] args) {
        Validator<String> usernameValidator = Validator.notBlank()
                .and(Validator.minLength(5));

        System.out.println(usernameValidator.validate("ateng"));
        System.out.println(usernameValidator.validate("java8"));
        System.out.println(usernameValidator.validate(""));

        Validator<String> blankValidator = Validator.notBlank().negate();
        System.out.println(blankValidator.validate(""));
    }
}
```

默认方法和静态方法在函数式接口中的常见用途如下：

| 方法类型 | 适用场景                   | 示例                                |
| -------- | -------------------------- | ----------------------------------- |
| 默认方法 | 组合、增强、复用接口行为   | `and()`、`or()`、`negate()`         |
| 静态方法 | 创建默认实现、提供工具能力 | `notBlank()`、`minLength()`         |
| 抽象方法 | 定义唯一核心行为           | `validate()`、`apply()`、`accept()` |

在设计函数式接口时，默认方法应服务于抽象方法的组合和增强，不应引入过多与核心语义无关的逻辑。静态方法适合放置工厂方法或通用辅助方法，但如果工具逻辑较复杂，建议拆分到独立工具类中。



## 内置函数式接口

Java 8 在 `java.util.function` 包中提供了一组常用函数式接口，主要用于消费数据、提供数据、转换数据和判断条件。项目开发中应优先使用这些标准接口，只有当语义不够清晰或需要特殊异常处理时，再自定义函数式接口。

### Consumer 消费型接口

`Consumer<T>` 表示消费一个参数但不返回结果的操作。它的核心方法是 `accept(T t)`，常用于遍历集合、打印日志、执行回调、处理消息等场景。

接口定义简化如下：

```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
```

下面示例演示通过 `Consumer` 统一处理用户名称列表。

文件位置：`src/main/java/io/github/atengk/functional/BuiltInConsumerDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Consumer 消费型接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BuiltInConsumerDemo {

    public static void main(String[] args) {
        List<String> usernames = CollUtil.newArrayList("admin", " ateng ", "", "test");

        Consumer<String> printValidUsername = username -> {
            if (StrUtil.isBlank(username)) {
                System.out.println("用户名为空，跳过处理");
                return;
            }
            System.out.println("处理用户名：" + StrUtil.trim(username));
        };

        usernames.forEach(printValidUsername);
    }
}
```

`Consumer` 还可以通过 `andThen` 进行链式组合，表示先执行当前消费逻辑，再执行下一个消费逻辑。

下面示例演示先清洗数据，再输出数据的处理流程。

文件位置：`src/main/java/io/github/atengk/functional/ConsumerChainDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

import java.util.function.Consumer;

/**
 * Consumer 链式调用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ConsumerChainDemo {

    public static void main(String[] args) {
        Consumer<String> checkUsername = username -> {
            if (StrUtil.isBlank(username)) {
                System.out.println("用户名为空");
                return;
            }
            System.out.println("用户名校验通过");
        };

        Consumer<String> printUsername = username -> System.out.println("用户名：" + StrUtil.trim(username));

        checkUsername.andThen(printUsername).accept(" ateng ");
    }
}
```

`Consumer` 适合只执行动作、不关心返回值的场景。如果业务逻辑需要返回处理结果，应使用 `Function`；如果需要返回布尔判断结果，应使用 `Predicate`。

### Supplier 供给型接口

`Supplier<T>` 表示不接收参数但返回一个结果的操作。它的核心方法是 `get()`，常用于延迟加载、默认值生成、对象创建、配置读取等场景。

接口定义简化如下：

```java
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

下面示例演示使用 `Supplier` 提供默认订单号。

文件位置：`src/main/java/io/github/atengk/functional/BuiltInSupplierDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;

import java.util.function.Supplier;

/**
 * Supplier 供给型接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BuiltInSupplierDemo {

    public static void main(String[] args) {
        Supplier<String> orderNoSupplier = () -> "ORDER-" + DateUtil.today() + "-" + IdUtil.fastSimpleUUID();

        String orderNo = orderNoSupplier.get();

        System.out.println("生成订单号：" + orderNo);
    }
}
```

`Supplier` 常用于需要延迟执行的场景。只有调用 `get()` 方法时，内部逻辑才会真正执行。

下面示例演示当用户名为空时，通过 `Supplier` 获取默认用户名。

文件位置：`src/main/java/io/github/atengk/functional/SupplierDefaultDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/**
 * Supplier 默认值示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class SupplierDefaultDemo {

    public static void main(String[] args) {
        String username = "";
        String result = getOrDefault(username, () -> "guest");

        System.out.println("最终用户名：" + result);
    }

    private static String getOrDefault(String value, Supplier<String> defaultSupplier) {
        if (StrUtil.isBlank(value)) {
            return defaultSupplier.get();
        }
        return value;
    }
}
```

`Supplier` 不适合承载复杂业务查询。如果内部逻辑涉及数据库、远程接口或大量计算，应注意异常处理、执行耗时和调用次数。

### Function 函数型接口

`Function<T, R>` 表示接收一个参数并返回一个结果的操作。它的核心方法是 `apply(T t)`，常用于数据转换、对象映射、字段提取、格式化处理等场景。

接口定义简化如下：

```java
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
```

下面示例演示将用户名转换为展示名称。

文件位置：`src/main/java/io/github/atengk/functional/BuiltInFunctionDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Function 函数型接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BuiltInFunctionDemo {

    public static void main(String[] args) {
        List<String> usernames = CollUtil.newArrayList(" admin ", " ateng ", " test ");

        Function<String, String> usernameFormatter = username -> "用户：" + StrUtil.trim(username);

        List<String> displayNames = usernames.stream()
                .map(usernameFormatter)
                .collect(Collectors.toList());

        displayNames.forEach(System.out::println);
    }
}
```

`Function` 支持 `compose` 和 `andThen` 进行组合。

`compose` 表示先执行传入的函数，再执行当前函数；`andThen` 表示先执行当前函数，再执行传入的函数。

下面示例演示先清理字符串，再转换为展示格式。

文件位置：`src/main/java/io/github/atengk/functional/FunctionChainDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

import java.util.function.Function;

/**
 * Function 组合调用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FunctionChainDemo {

    public static void main(String[] args) {
        Function<String, String> trimFunction = StrUtil::trim;
        Function<String, String> displayFunction = value -> "用户：" + value;

        String result = trimFunction.andThen(displayFunction).apply(" ateng ");

        System.out.println(result);
    }
}
```

`Function` 适合明确的输入输出转换。如果输入和输出类型一致，可以优先考虑 `UnaryOperator<T>`；如果需要两个输入参数，可以使用 `BiFunction<T, U, R>`。

### Predicate 断言型接口

`Predicate<T>` 表示接收一个参数并返回布尔结果的判断逻辑。它的核心方法是 `test(T t)`，常用于参数校验、条件过滤、权限判断、业务规则判断等场景。

接口定义简化如下：

```java
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}
```

下面示例演示使用 `Predicate` 过滤有效用户名。

文件位置：`src/main/java/io/github/atengk/functional/BuiltInPredicateDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Predicate 断言型接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BuiltInPredicateDemo {

    public static void main(String[] args) {
        List<String> usernames = CollUtil.newArrayList("admin", "", " ateng ", null, "test");

        Predicate<String> notBlankPredicate = StrUtil::isNotBlank;

        List<String> validUsernames = usernames.stream()
                .filter(notBlankPredicate)
                .map(StrUtil::trim)
                .collect(Collectors.toList());

        validUsernames.forEach(System.out::println);
    }
}
```

`Predicate` 支持 `and`、`or`、`negate` 进行条件组合。

下面示例演示组合多个用户名称校验条件。

文件位置：`src/main/java/io/github/atengk/functional/PredicateChainDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

import java.util.function.Predicate;

/**
 * Predicate 条件组合示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PredicateChainDemo {

    public static void main(String[] args) {
        Predicate<String> notBlank = StrUtil::isNotBlank;
        Predicate<String> minLength = value -> StrUtil.length(value) >= 5;
        Predicate<String> notAdmin = value -> !StrUtil.equalsIgnoreCase(value, "admin");

        Predicate<String> usernameRule = notBlank.and(minLength).and(notAdmin);

        System.out.println(usernameRule.test("ateng"));
        System.out.println(usernameRule.test("admin"));
        System.out.println(usernameRule.test("java8"));
    }
}
```

`Predicate` 适合表达返回 `true` 或 `false` 的业务规则。复杂规则建议拆成多个小的 `Predicate` 后再组合，避免把大量判断堆在一个 Lambda 表达式中。

### 常用扩展接口

除了 `Consumer`、`Supplier`、`Function`、`Predicate` 之外，`java.util.function` 还提供了许多扩展接口，用于支持双参数、同类型输入输出、基础类型优化等场景。

常用扩展接口如下：

| 接口                  | 方法                  | 说明                               | 示例场景                 |
| --------------------- | --------------------- | ---------------------------------- | ------------------------ |
| `BiConsumer<T, U>`    | `accept(T t, U u)`    | 接收两个参数，无返回值             | 处理键值对、双参数回调   |
| `BiFunction<T, U, R>` | `apply(T t, U u)`     | 接收两个参数，返回一个结果         | 两个对象合并、金额计算   |
| `BiPredicate<T, U>`   | `test(T t, U u)`      | 接收两个参数，返回布尔值           | 匹配两个对象、比较字段   |
| `UnaryOperator<T>`    | `apply(T t)`          | 输入和输出类型一致的 `Function`    | 字符串清理、金额修正     |
| `BinaryOperator<T>`   | `apply(T t1, T t2)`   | 两个相同类型入参，返回相同类型结果 | 求最大值、合并结果       |
| `IntFunction<R>`      | `apply(int value)`    | 接收 `int`，返回结果               | 按数量创建对象           |
| `IntPredicate`        | `test(int value)`     | 接收 `int`，返回布尔值             | 数值过滤                 |
| `IntConsumer`         | `accept(int value)`   | 接收 `int`，无返回值               | 数值遍历处理             |
| `ToIntFunction<T>`    | `applyAsInt(T value)` | 接收对象，返回 `int`               | 提取数量、年龄、排序字段 |

下面示例演示 `BiFunction` 和 `BinaryOperator` 的使用。

文件位置：`src/main/java/io/github/atengk/functional/ExtendedFunctionDemo.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/**
 * 常用扩展函数式接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ExtendedFunctionDemo {

    public static void main(String[] args) {
        BiFunction<BigDecimal, BigDecimal, BigDecimal> discountFunction = (amount, discountRate) -> amount.multiply(discountRate);

        BinaryOperator<BigDecimal> maxAmountOperator = BigDecimal::max;

        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal discountRate = new BigDecimal("0.80");

        BigDecimal discountAmount = discountFunction.apply(amount, discountRate);
        BigDecimal maxAmount = maxAmountOperator.apply(new BigDecimal("80.00"), new BigDecimal("120.00"));

        System.out.println("折扣后金额：" + discountAmount);
        System.out.println("最大金额：" + maxAmount);
    }
}
```

下面示例演示 `UnaryOperator` 对字符串进行统一清洗。

文件位置：`src/main/java/io/github/atengk/functional/UnaryOperatorDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

import java.util.function.UnaryOperator;

/**
 * UnaryOperator 一元操作接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UnaryOperatorDemo {

    public static void main(String[] args) {
        UnaryOperator<String> cleanUsername = username -> StrUtil.lowerFirst(StrUtil.trim(username));

        String result = cleanUsername.apply(" Ateng ");

        System.out.println("清洗后用户名：" + result);
    }
}
```

选择内置函数式接口时，可以按以下规则判断：

| 需求                           | 推荐接口              |
| ------------------------------ | --------------------- |
| 有入参，无返回值               | `Consumer<T>`         |
| 无入参，有返回值               | `Supplier<T>`         |
| 有入参，有返回值               | `Function<T, R>`      |
| 有入参，返回布尔值             | `Predicate<T>`        |
| 两个入参，无返回值             | `BiConsumer<T, U>`    |
| 两个入参，有返回值             | `BiFunction<T, U, R>` |
| 输入输出类型一致               | `UnaryOperator<T>`    |
| 两个相同类型入参，返回相同类型 | `BinaryOperator<T>`   |

## 自定义函数式接口

当 Java 8 内置函数式接口不能准确表达业务语义，或者需要处理受检异常、封装上下文参数、约束泛型类型时，可以自定义函数式接口。自定义时应保持接口语义单一，避免把多个业务动作放进一个接口中。

### @FunctionalInterface 注解

`@FunctionalInterface` 用于标识函数式接口。添加该注解后，编译器会检查接口是否只有一个抽象方法。如果接口中出现多个抽象方法，编译阶段会直接报错。

下面定义一个订单校验接口，用于表达订单对象是否满足某个业务规则。

文件位置：`src/main/java/io/github/atengk/functional/OrderValidator.java`

```java
package io.github.atengk.functional;

/**
 * 订单校验函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface OrderValidator<T> {

    boolean validate(T order);
}
```

使用 `@FunctionalInterface` 的主要价值不是运行时增强，而是编译期约束。它可以防止接口在后续维护中被误加第二个抽象方法，从而破坏 Lambda 表达式调用。

下面这种写法是错误示例，因为接口中有两个抽象方法。

```java
@FunctionalInterface
public interface ErrorValidator<T> {

    boolean validate(T data);

    String message();
}
```

如果需要补充错误信息，不应直接增加第二个抽象方法，可以通过默认方法提供通用能力。

```java
@FunctionalInterface
public interface CorrectValidator<T> {

    boolean validate(T data);

    default String message() {
        return "数据校验失败";
    }
}
```

在项目开发中，只要接口设计目标是被 Lambda 表达式实现，就建议添加 `@FunctionalInterface` 注解。

### 单一抽象方法设计

函数式接口设计的关键是只暴露一个核心行为。这个行为应该具有明确、稳定、单一的业务语义。

例如，下面接口用于执行一个业务动作，不关心返回值。

文件位置：`src/main/java/io/github/atengk/functional/BizHandler.java`

```java
package io.github.atengk.functional;

/**
 * 业务处理函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface BizHandler<T> {

    void handle(T data);
}
```

下面接口用于执行一个业务转换，接收一个对象并返回另一个对象。

文件位置：`src/main/java/io/github/atengk/functional/BizConverter.java`

```java
package io.github.atengk.functional;

/**
 * 业务转换函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface BizConverter<T, R> {

    R convert(T source);
}
```

使用示例：

文件位置：`src/main/java/io/github/atengk/functional/CustomFunctionalDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.util.StrUtil;

/**
 * 自定义函数式接口使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CustomFunctionalDemo {

    public static void main(String[] args) {
        BizHandler<String> usernameHandler = username -> System.out.println("处理用户名：" + StrUtil.trim(username));

        BizConverter<String, String> usernameConverter = username -> "用户：" + StrUtil.trim(username);

        usernameHandler.handle(" ateng ");

        String displayName = usernameConverter.convert(" ateng ");
        System.out.println(displayName);
    }
}
```

单一抽象方法设计时应避免以下问题：

| 问题           | 说明                                               |
| -------------- | -------------------------------------------------- |
| 方法职责过大   | 一个方法中同时完成校验、转换、保存、通知等多个动作 |
| 方法命名模糊   | 如 `doSomething`、`execute`，无法体现业务语义      |
| 参数过多       | Lambda 表达式可读性下降                            |
| 返回值不明确   | 调用方无法判断返回结果代表什么                     |
| 与内置接口重复 | 能用 `Function`、`Predicate` 表达时却重复造接口    |

如果接口只是普通数据转换，优先使用 `Function<T, R>`；如果接口名称能明显增强业务表达，例如 `OrderValidator`、`RuleMatcher`、`MessageHandler`，可以自定义函数式接口。

### 泛型参数设计

泛型可以提升函数式接口的复用性，使同一个接口适配不同数据类型。设计泛型参数时，应保证参数含义清晰，避免过度抽象。

常见泛型命名如下：

| 泛型 | 含义           |
| ---- | -------------- |
| `T`  | 输入类型       |
| `R`  | 返回类型       |
| `U`  | 第二个输入类型 |
| `E`  | 异常类型       |
| `C`  | 上下文类型     |

下面定义一个带上下文的业务规则接口，适用于规则判断时需要额外上下文参数的场景。

文件位置：`src/main/java/io/github/atengk/functional/ContextRule.java`

```java
package io.github.atengk.functional;

/**
 * 上下文规则函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface ContextRule<T, C> {

    boolean match(T data, C context);
}
```

下面定义简单的订单对象和规则上下文对象。

文件位置：`src/main/java/io/github/atengk/functional/OrderInfo.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;

/**
 * 订单信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderInfo {

    private String orderNo;

    private BigDecimal amount;

    public OrderInfo(String orderNo, BigDecimal amount) {
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
```

文件位置：`src/main/java/io/github/atengk/functional/RuleContext.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;

/**
 * 规则上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class RuleContext {

    private BigDecimal minAmount;

    public RuleContext(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }
}
```

下面示例演示通过泛型函数式接口处理不同订单规则。

文件位置：`src/main/java/io/github/atengk/functional/ContextRuleDemo.java`

```java
package io.github.atengk.functional;

import java.math.BigDecimal;

/**
 * 上下文规则函数式接口示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ContextRuleDemo {

    public static void main(String[] args) {
        OrderInfo orderInfo = new OrderInfo("ORDER-1001", new BigDecimal("199.00"));
        RuleContext ruleContext = new RuleContext(new BigDecimal("100.00"));

        ContextRule<OrderInfo, RuleContext> amountRule = (order, context) -> order.getAmount().compareTo(context.getMinAmount()) >= 0;

        boolean matched = amountRule.match(orderInfo, ruleContext);

        System.out.println("订单是否满足金额规则：" + matched);
    }
}
```

泛型设计建议如下：

| 建议                             | 说明                         |
| -------------------------------- | ---------------------------- |
| 输入和输出明确时使用 `T`、`R`    | 如 `Converter<T, R>`         |
| 存在上下文参数时使用 `C`         | 如 `ContextRule<T, C>`       |
| 不要滥用过多泛型参数             | 泛型过多会降低接口可读性     |
| 避免泛型与业务语义脱节           | 必要时通过接口名补充业务含义 |
| 能用内置接口表达时优先用内置接口 | 减少重复接口定义             |

### 异常处理设计

Java 8 内置函数式接口的方法通常没有声明受检异常。例如 `Function.apply`、`Consumer.accept` 都不能直接抛出 `IOException`、`SQLException` 等受检异常。因此在需要处理受检异常的场景中，应明确异常处理策略。

常见策略有三种：

| 策略                       | 说明                                            | 适用场景                     |
| -------------------------- | ----------------------------------------------- | ---------------------------- |
| Lambda 内部捕获异常        | 在 Lambda 中直接 `try-catch`                    | 异常处理逻辑简单             |
| 自定义可抛异常的函数式接口 | 接口方法声明 `throws Exception`                 | 文件处理、远程调用、反射处理 |
| 包装为运行时异常           | 捕获受检异常后转成业务异常或 `RuntimeException` | 统一交给上层异常处理器       |

下面定义一个允许抛出异常的转换接口。

文件位置：`src/main/java/io/github/atengk/functional/ThrowingFunction.java`

```java
package io.github.atengk.functional;

/**
 * 可抛异常的函数型接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {

    R apply(T data) throws Exception;
}
```

为了方便在 Stream 或普通业务代码中使用，可以提供一个包装工具类，将受检异常转换为运行时异常。

文件位置：`src/main/java/io/github/atengk/functional/FunctionWrapper.java`

```java
package io.github.atengk.functional;

import java.util.function.Function;

/**
 * 函数式接口异常包装工具
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FunctionWrapper {

    private FunctionWrapper() {
    }

    public static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> throwingFunction) {
        return data -> {
            try {
                return throwingFunction.apply(data);
            } catch (Exception e) {
                throw new IllegalStateException("函数式接口执行失败", e);
            }
        };
    }
}
```

下面示例演示将字符串转换为数字，并统一包装异常。

文件位置：`src/main/java/io/github/atengk/functional/ThrowingFunctionDemo.java`

```java
package io.github.atengk.functional;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 函数式接口异常处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ThrowingFunctionDemo {

    public static void main(String[] args) {
        List<String> values = CollUtil.newArrayList("1", "2", "3");

        List<Integer> numbers = values.stream()
                .map(FunctionWrapper.unchecked(Integer::parseInt))
                .collect(Collectors.toList());

        numbers.forEach(System.out::println);
    }
}
```

如果项目中有统一业务异常类，建议不要直接抛出 `IllegalStateException`，而是转换为项目自定义异常，例如 `BizException`。

下面是业务异常包装版本。

文件位置：`src/main/java/io/github/atengk/functional/BizException.java`

```java
package io.github.atengk.functional;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizException extends RuntimeException {

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/functional/BizFunctionWrapper.java`

```java
package io.github.atengk.functional;

import java.util.function.Function;

/**
 * 业务函数式接口异常包装工具
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizFunctionWrapper {

    private BizFunctionWrapper() {
    }

    public static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> throwingFunction) {
        return data -> {
            try {
                return throwingFunction.apply(data);
            } catch (Exception e) {
                throw new BizException("业务函数执行失败", e);
            }
        };
    }
}
```

异常处理设计时需要注意以下几点：

| 注意事项                           | 说明                                             |
| ---------------------------------- | ------------------------------------------------ |
| 不要吞掉异常                       | 捕获异常后只打印日志、不抛出会导致调用方误判成功 |
| 不要在 Lambda 中写大量 `try-catch` | 会降低可读性，应抽取包装方法                     |
| 明确异常边界                       | 底层异常可包装为业务异常向上抛出                 |
| Stream 中慎用复杂异常逻辑          | 复杂逻辑建议抽成普通方法                         |
| 保留原始异常                       | 包装异常时应把原异常作为 `cause` 传入            |

在普通项目开发中，优先使用内置函数式接口；当需要更强业务语义或特殊异常处理时，再定义自定义函数式接口。自定义接口应保持职责单一、命名清晰、泛型适度，并通过 `@FunctionalInterface` 保证接口结构稳定。



## 函数式接口开发实践

函数式接口在项目中的价值不只是简化语法，更重要的是把可变化的业务行为抽象出来。常见实践包括参数校验、数据转换、条件过滤、回调处理和策略模式简化。

### 参数校验场景

参数校验是函数式接口最常见的应用场景之一。通过 `Predicate<T>` 或自定义校验接口，可以把不同校验规则拆成独立逻辑，再按业务需要组合使用。

下面示例使用 `Predicate` 定义用户名校验规则，并通过 `and` 组合多个条件。

文件位置：`src/main/java/io/github/atengk/functional/practice/ParamValidateDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.util.StrUtil;

import java.util.function.Predicate;

/**
 * 参数校验场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ParamValidateDemo {

    public static void main(String[] args) {
        Predicate<String> notBlank = StrUtil::isNotBlank;
        Predicate<String> lengthValid = username -> StrUtil.length(username) >= 5;
        Predicate<String> notAdmin = username -> !StrUtil.equalsIgnoreCase(username, "admin");

        Predicate<String> usernameRule = notBlank.and(lengthValid).and(notAdmin);

        System.out.println(usernameRule.test("ateng"));
        System.out.println(usernameRule.test("admin"));
        System.out.println(usernameRule.test("java8"));
    }
}
```

如果校验场景需要返回错误信息，单纯使用 `Predicate` 不够直观，可以定义一个带错误提示的校验结果对象。

文件位置：`src/main/java/io/github/atengk/functional/practice/ValidateResult.java`

```java
package io.github.atengk.functional.practice;

/**
 * 校验结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ValidateResult {

    private final boolean success;

    private final String message;

    private ValidateResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ValidateResult success() {
        return new ValidateResult(true, "校验通过");
    }

    public static ValidateResult fail(String message) {
        return new ValidateResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
```

下面定义一个返回 `ValidateResult` 的函数式接口，适合业务参数校验。

文件位置：`src/main/java/io/github/atengk/functional/practice/BizValidator.java`

```java
package io.github.atengk.functional.practice;

/**
 * 业务校验函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface BizValidator<T> {

    ValidateResult validate(T data);
}
```

下面示例演示如何使用自定义校验接口处理用户注册参数。

文件位置：`src/main/java/io/github/atengk/functional/practice/BizValidateDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 业务参数校验示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizValidateDemo {

    public static void main(String[] args) {
        List<BizValidator<String>> validators = CollUtil.newArrayList(
                username -> StrUtil.isBlank(username) ? ValidateResult.fail("用户名不能为空") : ValidateResult.success(),
                username -> StrUtil.length(username) < 5 ? ValidateResult.fail("用户名长度不能小于5") : ValidateResult.success(),
                username -> StrUtil.equalsIgnoreCase(username, "admin") ? ValidateResult.fail("用户名不能为admin") : ValidateResult.success()
        );

        ValidateResult result = validate("ateng", validators);

        System.out.println(result.isSuccess());
        System.out.println(result.getMessage());
    }

    private static ValidateResult validate(String username, List<BizValidator<String>> validators) {
        for (BizValidator<String> validator : validators) {
            ValidateResult result = validator.validate(username);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return ValidateResult.success();
    }
}
```

参数校验场景中，简单布尔判断优先使用 `Predicate`；如果需要错误码、错误消息或上下文信息，可以自定义函数式接口。

### 数据转换场景

数据转换适合使用 `Function<T, R>`。它可以把实体对象转换为 VO、把请求 DTO 转换为实体、把原始字段转换为展示字段。

下面定义订单实体和订单展示对象。

文件位置：`src/main/java/io/github/atengk/functional/practice/OrderInfo.java`

```java
package io.github.atengk.functional.practice;

import java.math.BigDecimal;

/**
 * 订单信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderInfo {

    private final String orderNo;

    private final BigDecimal amount;

    private final Integer status;

    public OrderInfo(String orderNo, BigDecimal amount, Integer status) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getStatus() {
        return status;
    }
}
```

文件位置：`src/main/java/io/github/atengk/functional/practice/OrderVo.java`

```java
package io.github.atengk.functional.practice;

import java.math.BigDecimal;

/**
 * 订单展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderVo {

    private String orderNo;

    private BigDecimal amount;

    private String statusName;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}
```

下面示例使用 `Function<OrderInfo, OrderVo>` 完成订单实体到展示对象的转换。

文件位置：`src/main/java/io/github/atengk/functional/practice/DataConvertDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据转换场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class DataConvertDemo {

    public static void main(String[] args) {
        List<OrderInfo> orderList = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2),
                new OrderInfo("ORDER-1003", new BigDecimal("59.90"), 3)
        );

        Function<OrderInfo, OrderVo> orderConverter = order -> {
            OrderVo orderVo = new OrderVo();
            orderVo.setOrderNo(order.getOrderNo());
            orderVo.setAmount(order.getAmount());
            orderVo.setStatusName(convertStatusName(order.getStatus()));
            return orderVo;
        };

        List<OrderVo> voList = orderList.stream()
                .map(orderConverter)
                .collect(Collectors.toList());

        voList.forEach(orderVo -> System.out.println(Convert.toStr(orderVo.getOrderNo()) + "：" + orderVo.getStatusName()));
    }

    private static String convertStatusName(Integer status) {
        if (Integer.valueOf(1).equals(status)) {
            return "待支付";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已支付";
        }
        if (Integer.valueOf(3).equals(status)) {
            return "已取消";
        }
        return "未知状态";
    }
}
```

数据转换场景中，`Function` 的优势是可以和 `Stream.map` 直接配合。如果转换逻辑较复杂，应抽取成独立方法或独立转换类，避免在 Lambda 中堆积大量字段处理代码。

### 条件过滤场景

条件过滤适合使用 `Predicate<T>`。它可以将过滤条件拆分为多个小规则，再根据业务需要组合。

下面示例按订单状态和订单金额过滤订单。

文件位置：`src/main/java/io/github/atengk/functional/practice/ConditionFilterDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.collection.CollUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 条件过滤场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ConditionFilterDemo {

    public static void main(String[] args) {
        List<OrderInfo> orderList = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2),
                new OrderInfo("ORDER-1003", new BigDecimal("59.90"), 2),
                new OrderInfo("ORDER-1004", new BigDecimal("299.00"), 3)
        );

        Predicate<OrderInfo> paidOrder = order -> Integer.valueOf(2).equals(order.getStatus());
        Predicate<OrderInfo> highAmount = order -> order.getAmount().compareTo(new BigDecimal("100.00")) >= 0;

        List<OrderInfo> result = orderList.stream()
                .filter(paidOrder.and(highAmount))
                .collect(Collectors.toList());

        result.forEach(order -> System.out.println(order.getOrderNo()));
    }
}
```

如果过滤条件来自外部参数，可以按条件动态拼接 `Predicate`。

下面示例演示根据查询参数动态组合过滤规则。

文件位置：`src/main/java/io/github/atengk/functional/practice/OrderQuery.java`

```java
package io.github.atengk.functional.practice;

import java.math.BigDecimal;

/**
 * 订单查询条件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderQuery {

    private Integer status;

    private BigDecimal minAmount;

    public OrderQuery(Integer status, BigDecimal minAmount) {
        this.status = status;
        this.minAmount = minAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }
}
```

文件位置：`src/main/java/io/github/atengk/functional/practice/DynamicConditionFilterDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 动态条件过滤示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class DynamicConditionFilterDemo {

    public static void main(String[] args) {
        List<OrderInfo> orderList = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2),
                new OrderInfo("ORDER-1003", new BigDecimal("59.90"), 2)
        );

        OrderQuery query = new OrderQuery(2, new BigDecimal("100.00"));

        Predicate<OrderInfo> predicate = buildPredicate(query);

        List<OrderInfo> result = orderList.stream()
                .filter(predicate)
                .collect(Collectors.toList());

        result.forEach(order -> System.out.println(order.getOrderNo()));
    }

    private static Predicate<OrderInfo> buildPredicate(OrderQuery query) {
        Predicate<OrderInfo> predicate = order -> true;

        if (ObjectUtil.isNotNull(query.getStatus())) {
            predicate = predicate.and(order -> query.getStatus().equals(order.getStatus()));
        }

        if (ObjectUtil.isNotNull(query.getMinAmount())) {
            predicate = predicate.and(order -> order.getAmount().compareTo(query.getMinAmount()) >= 0);
        }

        return predicate;
    }
}
```

条件过滤适合处理内存集合中的规则筛选。对于数据库查询，不建议把大量数据查到内存后再使用 `Predicate` 过滤，应优先通过 SQL、MyBatis-Plus 条件构造器或数据库索引完成过滤。

### 回调处理场景

回调处理适合使用 `Consumer<T>`、`Function<T, R>` 或自定义函数式接口。典型场景包括业务执行前后处理、任务执行结果处理、异常统一包装、模板方法简化等。

下面示例定义一个任务执行模板，使用 `Runnable` 表示无参无返回值的任务。

文件位置：`src/main/java/io/github/atengk/functional/practice/TaskTemplate.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务执行模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TaskTemplate {

    private static final Logger log = LoggerFactory.getLogger(TaskTemplate.class);

    public void execute(String taskName, Runnable runnable) {
        TimeInterval timer = DateUtil.timer();

        try {
            log.info("开始执行任务：{}", taskName);
            runnable.run();
            log.info("任务执行成功：{}，耗时：{}ms", taskName, timer.interval());
        } catch (Exception e) {
            log.error("任务执行失败：{}", taskName, e);
            throw e;
        }
    }
}
```

下面示例演示调用任务模板，把具体业务处理作为回调逻辑传入。

文件位置：`src/main/java/io/github/atengk/functional/practice/CallbackDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.thread.ThreadUtil;

/**
 * 回调处理场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CallbackDemo {

    public static void main(String[] args) {
        TaskTemplate taskTemplate = new TaskTemplate();

        taskTemplate.execute("同步订单数据", () -> {
            ThreadUtil.sleep(500);
            System.out.println("正在同步订单数据");
        });
    }
}
```

如果回调逻辑需要接收参数并返回结果，可以使用 `Function<T, R>` 定义通用执行模板。

文件位置：`src/main/java/io/github/atengk/functional/practice/ResultTemplate.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * 带结果的执行模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ResultTemplate {

    private static final Logger log = LoggerFactory.getLogger(ResultTemplate.class);

    public <T, R> R execute(String bizName, T param, Function<T, R> function) {
        TimeInterval timer = DateUtil.timer();

        try {
            log.info("开始处理业务：{}", bizName);
            R result = function.apply(param);
            log.info("业务处理成功：{}，耗时：{}ms", bizName, timer.interval());
            return result;
        } catch (Exception e) {
            log.error("业务处理失败：{}", bizName, e);
            throw e;
        }
    }
}
```

下面示例演示带参数和返回值的回调处理。

文件位置：`src/main/java/io/github/atengk/functional/practice/ResultCallbackDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.util.StrUtil;

/**
 * 带结果回调处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ResultCallbackDemo {

    public static void main(String[] args) {
        ResultTemplate resultTemplate = new ResultTemplate();

        String result = resultTemplate.execute("用户名格式化", " ateng ", username -> "用户：" + StrUtil.trim(username));

        System.out.println(result);
    }
}
```

回调处理场景中，函数式接口适合用于封装固定流程中的可变步骤。常见固定流程包括日志记录、耗时统计、异常包装、事务边界、权限判断和资源释放。

### 策略模式简化

传统策略模式通常需要定义策略接口、多个策略实现类和策略上下文。在策略数量较少、逻辑较短时，可以使用函数式接口减少类数量。

下面示例使用 `Function` 实现不同订单类型的金额计算策略。

文件位置：`src/main/java/io/github/atengk/functional/practice/StrategyDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.map.MapUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

/**
 * 策略模式简化示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class StrategyDemo {

    public static void main(String[] args) {
        Map<String, Function<BigDecimal, BigDecimal>> strategyMap = MapUtil.newHashMap();

        strategyMap.put("NORMAL", amount -> amount);
        strategyMap.put("VIP", amount -> amount.multiply(new BigDecimal("0.80")));
        strategyMap.put("NEW_USER", amount -> amount.subtract(new BigDecimal("10.00")));

        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal result = calculate("VIP", amount, strategyMap);

        System.out.println("最终金额：" + result);
    }

    private static BigDecimal calculate(String type, BigDecimal amount, Map<String, Function<BigDecimal, BigDecimal>> strategyMap) {
        Function<BigDecimal, BigDecimal> strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的订单类型：" + type);
        }
        return strategy.apply(amount);
    }
}
```

如果策略需要多个参数，可以使用 `BiFunction<T, U, R>`，或自定义更具业务语义的函数式接口。

下面示例定义一个优惠计算策略接口，使代码语义更清晰。

文件位置：`src/main/java/io/github/atengk/functional/practice/DiscountStrategy.java`

```java
package io.github.atengk.functional.practice;

import java.math.BigDecimal;

/**
 * 优惠计算策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface DiscountStrategy {

    BigDecimal calculate(BigDecimal amount, BigDecimal discountValue);
}
```

下面示例使用自定义策略接口管理不同优惠计算规则。

文件位置：`src/main/java/io/github/atengk/functional/practice/CustomStrategyDemo.java`

```java
package io.github.atengk.functional.practice;

import cn.hutool.core.map.MapUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 自定义函数式策略示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CustomStrategyDemo {

    public static void main(String[] args) {
        Map<String, DiscountStrategy> strategyMap = MapUtil.newHashMap();

        strategyMap.put("RATE", (amount, discountValue) -> amount.multiply(discountValue));
        strategyMap.put("REDUCE", (amount, discountValue) -> amount.subtract(discountValue));

        BigDecimal amount = new BigDecimal("100.00");

        BigDecimal rateResult = strategyMap.get("RATE").calculate(amount, new BigDecimal("0.80"));
        BigDecimal reduceResult = strategyMap.get("REDUCE").calculate(amount, new BigDecimal("10.00"));

        System.out.println("折扣结果：" + rateResult);
        System.out.println("满减结果：" + reduceResult);
    }
}
```

函数式接口简化策略模式时，应控制策略逻辑复杂度。如果每个策略都包含大量依赖注入、数据库访问、远程调用或复杂流程，仍建议使用传统策略类，并交给 Spring 容器管理。

## 在项目中的应用方式

函数式接口在项目中通常不会单独存在，而是与 Service 层封装、工具类抽象、集合处理、业务规则配置结合使用。合理使用可以减少重复代码，提高扩展性和可读性。

### Service 层业务封装

Service 层常见问题是多个方法存在相同的前置校验、日志记录、异常处理和结果转换。可以使用函数式接口把通用流程封装起来，把具体业务逻辑作为参数传入。

下面示例定义一个通用业务执行器。

文件位置：`src/main/java/io/github/atengk/functional/project/BizExecutor.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 业务执行器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizExecutor {

    private static final Logger log = LoggerFactory.getLogger(BizExecutor.class);

    public <T> T execute(String bizName, Supplier<T> supplier) {
        if (StrUtil.isBlank(bizName)) {
            throw new IllegalArgumentException("业务名称不能为空");
        }

        TimeInterval timer = DateUtil.timer();

        try {
            log.info("开始执行业务：{}", bizName);
            T result = supplier.get();
            log.info("业务执行完成：{}，耗时：{}ms", bizName, timer.interval());
            return result;
        } catch (Exception e) {
            log.error("业务执行异常：{}", bizName, e);
            throw e;
        }
    }
}
```

下面示例模拟 Service 层调用，通过 `Supplier` 传入具体查询逻辑。

文件位置：`src/main/java/io/github/atengk/functional/project/OrderServiceDemo.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * Service 层业务封装示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderServiceDemo {

    private final BizExecutor bizExecutor = new BizExecutor();

    public List<String> listOrderNo() {
        return bizExecutor.execute("查询订单编号列表", () -> CollUtil.newArrayList("ORDER-1001", "ORDER-1002", "ORDER-1003"));
    }

    public static void main(String[] args) {
        OrderServiceDemo orderServiceDemo = new OrderServiceDemo();

        List<String> orderNoList = orderServiceDemo.listOrderNo();

        orderNoList.forEach(System.out::println);
    }
}
```

Service 层使用函数式接口时，不应为了减少代码行数而牺牲业务表达。适合抽取的是固定流程，不适合把核心业务逻辑隐藏在过长的 Lambda 表达式中。

### 工具类方法抽象

工具类中经常会出现类似的空值判断、异常包装、默认值处理和集合转换逻辑。函数式接口可以让工具方法更加通用。

下面定义一个对象工具类，用于处理空值默认值和安全转换。

文件位置：`src/main/java/io/github/atengk/functional/project/FuncUtils.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.util.ObjectUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 函数式工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FuncUtils {

    private FuncUtils() {
    }

    public static <T> T getOrDefault(T value, Supplier<T> defaultSupplier) {
        if (ObjectUtil.isNotNull(value)) {
            return value;
        }
        return defaultSupplier.get();
    }

    public static <T, R> R mapIfNotNull(T value, Function<T, R> function, Supplier<R> defaultSupplier) {
        if (ObjectUtil.isNotNull(value)) {
            return function.apply(value);
        }
        return defaultSupplier.get();
    }
}
```

下面示例演示工具类在空值处理和字段转换中的使用方式。

文件位置：`src/main/java/io/github/atengk/functional/project/FuncUtilsDemo.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.util.StrUtil;

/**
 * 工具类方法抽象示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FuncUtilsDemo {

    public static void main(String[] args) {
        String username = null;

        String safeUsername = FuncUtils.getOrDefault(username, () -> "guest");
        String displayName = FuncUtils.mapIfNotNull(safeUsername, value -> "用户：" + StrUtil.trim(value), () -> "未知用户");

        System.out.println(safeUsername);
        System.out.println(displayName);
    }
}
```

工具类方法抽象时要避免过度封装。只有当一个函数式工具方法能被多个业务场景复用，并且能提升代码可读性时，才建议沉淀到工具类中。

### 集合处理与 Stream 配合

函数式接口与 Stream API 是 Java 8 中最常见的组合。`filter` 接收 `Predicate`，`map` 接收 `Function`，`forEach` 接收 `Consumer`，`reduce` 接收 `BinaryOperator`。

下面示例演示订单列表的过滤、转换和统计。

文件位置：`src/main/java/io/github/atengk/functional/project/StreamFunctionDemo.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.functional.practice.OrderInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合处理与 Stream 配合示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class StreamFunctionDemo {

    public static void main(String[] args) {
        List<OrderInfo> orderList = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2),
                new OrderInfo("ORDER-1003", new BigDecimal("59.90"), 2),
                new OrderInfo("ORDER-1004", new BigDecimal("299.00"), 3)
        );

        Predicate<OrderInfo> paidOrder = order -> Integer.valueOf(2).equals(order.getStatus());
        Function<OrderInfo, String> orderNoMapper = OrderInfo::getOrderNo;

        List<String> paidOrderNoList = orderList.stream()
                .filter(paidOrder)
                .map(orderNoMapper)
                .collect(Collectors.toList());

        BigDecimal totalAmount = orderList.stream()
                .filter(paidOrder)
                .map(OrderInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("已支付订单编号：" + paidOrderNoList);
        System.out.println("已支付订单总金额：" + totalAmount);
    }
}
```

如果 Stream 链路较长，可以把关键函数式接口提前定义为变量，提升可读性。

下面示例把过滤、转换、输出逻辑分别抽取为具名变量。

文件位置：`src/main/java/io/github/atengk/functional/project/ReadableStreamDemo.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.functional.practice.OrderInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 可读性更好的 Stream 示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ReadableStreamDemo {

    public static void main(String[] args) {
        List<OrderInfo> orderList = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2),
                new OrderInfo("ORDER-1003", new BigDecimal("59.90"), 2)
        );

        Predicate<OrderInfo> paidOrderPredicate = order -> Integer.valueOf(2).equals(order.getStatus());
        Function<OrderInfo, String> orderNoFunction = OrderInfo::getOrderNo;
        Consumer<String> printOrderNoConsumer = orderNo -> System.out.println("订单编号：" + orderNo);

        List<String> orderNoList = orderList.stream()
                .filter(paidOrderPredicate)
                .map(orderNoFunction)
                .collect(Collectors.toList());

        orderNoList.forEach(printOrderNoConsumer);
    }
}
```

集合处理建议遵循以下原则：

| 建议                            | 说明                               |
| ------------------------------- | ---------------------------------- |
| 简单集合处理可以直接使用 Stream | 如过滤、映射、分组、求和           |
| 复杂业务逻辑应抽取方法          | 避免 Lambda 过长                   |
| 避免 Stream 中执行有副作用操作  | 如修改外部变量、执行复杂更新       |
| 数据量大时关注性能              | 必要时使用数据库聚合或分页处理     |
| 空集合优先返回空集合            | 避免返回 `null` 导致调用方额外判断 |

### 配置化业务规则处理

当业务规则经常变化时，可以把规则标识、规则参数和函数式接口结合起来，实现轻量级规则配置。适合处理简单规则，不适合替代完整规则引擎。

下面定义规则配置对象。

文件位置：`src/main/java/io/github/atengk/functional/project/RuleConfig.java`

```java
package io.github.atengk.functional.project;

/**
 * 规则配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class RuleConfig {

    private final String ruleCode;

    private final String ruleValue;

    public RuleConfig(String ruleCode, String ruleValue) {
        this.ruleCode = ruleCode;
        this.ruleValue = ruleValue;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleValue() {
        return ruleValue;
    }
}
```

下面定义规则执行接口，接收订单对象和规则配置，返回是否命中规则。

文件位置：`src/main/java/io/github/atengk/functional/project/RuleExecutor.java`

```java
package io.github.atengk.functional.project;

import io.github.atengk.functional.practice.OrderInfo;

/**
 * 规则执行函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface RuleExecutor {

    boolean execute(OrderInfo orderInfo, RuleConfig ruleConfig);
}
```

下面示例使用规则编码映射不同规则执行逻辑。

文件位置：`src/main/java/io/github/atengk/functional/project/ConfigRuleDemo.java`

```java
package io.github.atengk.functional.project;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.functional.practice.OrderInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 配置化业务规则处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ConfigRuleDemo {

    public static void main(String[] args) {
        OrderInfo orderInfo = new OrderInfo("ORDER-1001", new BigDecimal("199.00"), 2);

        List<RuleConfig> ruleConfigList = CollUtil.newArrayList(
                new RuleConfig("ORDER_STATUS", "2"),
                new RuleConfig("MIN_AMOUNT", "100.00"),
                new RuleConfig("ORDER_NO_PREFIX", "ORDER")
        );

        Map<String, RuleExecutor> executorMap = buildExecutorMap();

        boolean matched = ruleConfigList.stream()
                .allMatch(ruleConfig -> matchRule(orderInfo, ruleConfig, executorMap));

        System.out.println("订单是否满足全部规则：" + matched);
    }

    private static Map<String, RuleExecutor> buildExecutorMap() {
        Map<String, RuleExecutor> executorMap = MapUtil.newHashMap();

        executorMap.put("ORDER_STATUS", (order, config) -> order.getStatus().equals(Convert.toInt(config.getRuleValue())));
        executorMap.put("MIN_AMOUNT", (order, config) -> order.getAmount().compareTo(Convert.toBigDecimal(config.getRuleValue())) >= 0);
        executorMap.put("ORDER_NO_PREFIX", (order, config) -> StrUtil.startWith(order.getOrderNo(), config.getRuleValue()));

        return executorMap;
    }

    private static boolean matchRule(OrderInfo orderInfo, RuleConfig ruleConfig, Map<String, RuleExecutor> executorMap) {
        RuleExecutor executor = executorMap.get(ruleConfig.getRuleCode());
        if (executor == null) {
            throw new IllegalArgumentException("未配置规则执行器：" + ruleConfig.getRuleCode());
        }
        return executor.execute(orderInfo, ruleConfig);
    }
}
```

配置化规则适合以下场景：

| 场景             | 说明                               |
| ---------------- | ---------------------------------- |
| 规则数量较少     | 如状态、金额、类型、前缀等简单判断 |
| 规则逻辑相对稳定 | 变化的是规则值，不是执行逻辑       |
| 需要动态启停规则 | 可通过配置列表控制启用哪些规则     |
| 不想引入规则引擎 | 轻量级业务规则可以用函数式接口处理 |

如果规则之间存在复杂依赖、优先级、分支流程、回溯计算或规则编排，应考虑使用更明确的规则模型、策略类体系，或者引入专业规则引擎。



## 开发规范与注意事项

函数式接口虽然可以减少样板代码，但也容易被滥用。项目中应重点控制接口命名、方法语义、Lambda 可读性、空值处理、日志与异常边界，避免代码变得隐晦。

### 接口命名规范

函数式接口命名应体现业务语义，而不是单纯描述技术动作。命名清晰后，调用方在阅读 Lambda 表达式时可以直接理解这段行为的业务含义。

推荐命名方式如下：

| 场景     | 推荐命名                              | 不推荐命名                          |
| -------- | ------------------------------------- | ----------------------------------- |
| 校验规则 | `OrderValidator`、`UserValidator`     | `CheckFunction`、`MyPredicate`      |
| 数据转换 | `OrderConverter`、`UserMapper`        | `ConvertFunction`、`DataFunction`   |
| 规则匹配 | `RuleMatcher`、`PermissionMatcher`    | `RuleFunction`、`BooleanHandler`    |
| 回调处理 | `TaskCallback`、`MessageHandler`      | `CallbackFunction`、`DoHandler`     |
| 策略计算 | `DiscountStrategy`、`PriceCalculator` | `CalculateFunction`、`StrategyFunc` |

接口命名建议遵循以下规则：

| 规范                             | 说明                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| 优先体现业务含义                 | 如 `OrderValidator` 比 `PredicateFunction` 更清晰            |
| 避免过度技术化                   | 不要把 `Function`、`Predicate` 强行放入业务接口名中          |
| 动词和名词搭配清晰               | 如 `Validator`、`Converter`、`Matcher`、`Handler`、`Calculator` |
| 一个接口只描述一种行为           | 不要使用 `OrderProcessor` 承载校验、转换、保存等多种语义     |
| 能用内置接口表达时优先用内置接口 | 普通转换使用 `Function<T, R>` 即可                           |

下面是较推荐的接口命名示例。

文件位置：`src/main/java/io/github/atengk/functional/spec/OrderAmountValidator.java`

```java
package io.github.atengk.functional.spec;

import java.math.BigDecimal;

/**
 * 订单金额校验器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface OrderAmountValidator {

    /**
     * 校验订单金额
     *
     * @param amount 订单金额
     * @return 是否校验通过
     */
    boolean validate(BigDecimal amount);
}
```

不推荐下面这种命名，因为它只描述了技术形态，没有表达业务语义。

```java
@FunctionalInterface
public interface BooleanFunction<T> {

    boolean apply(T data);
}
```

如果接口名称不能帮助调用方理解业务含义，通常说明该接口不应该自定义，应直接使用 JDK 内置函数式接口。

### 方法语义设计

函数式接口的抽象方法应具备明确的输入、输出和动作语义。方法名不要过于宽泛，否则 Lambda 表达式会变得难以理解。

常见方法命名建议如下：

| 接口语义 | 推荐方法名  | 返回值                     |
| -------- | ----------- | -------------------------- |
| 校验     | `validate`  | `boolean` 或校验结果对象   |
| 匹配     | `match`     | `boolean`                  |
| 转换     | `convert`   | 目标对象                   |
| 处理     | `handle`    | `void`                     |
| 计算     | `calculate` | 计算结果                   |
| 执行     | `execute`   | 根据场景返回结果或无返回值 |
| 解析     | `parse`     | 解析结果                   |

下面示例定义一个语义清晰的订单转换接口。

文件位置：`src/main/java/io/github/atengk/functional/spec/OrderConverter.java`

```java
package io.github.atengk.functional.spec;

/**
 * 订单转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface OrderConverter<S, T> {

    /**
     * 转换订单对象
     *
     * @param source 原始订单对象
     * @return 目标订单对象
     */
    T convert(S source);
}
```

不推荐下面这种抽象方法设计：

```java
@FunctionalInterface
public interface OrderFunction<T, R> {

    R doSomething(T value);
}
```

`doSomething` 无法表达具体动作，调用方必须进入实现细节才能理解逻辑。函数式接口本身就是行为抽象，因此方法语义必须尽量精确。

方法语义设计时还需要注意：

| 注意点                         | 说明                                                      |
| ------------------------------ | --------------------------------------------------------- |
| 不要混合多个动作               | 如 `validateAndSave` 不适合作为函数式接口方法             |
| 不要让返回值含义模糊           | `String execute(T data)` 不如 `String parse(T data)` 清晰 |
| 不要过度依赖注释解释语义       | 方法名本身应尽量自解释                                    |
| 避免参数过多                   | 参数过多时优先封装为上下文对象                            |
| 布尔返回值方法建议使用判断动词 | 如 `match`、`validate`、`support`                         |

### 可读性控制

Lambda 表达式适合表达短小、明确的逻辑。如果 Lambda 代码过长，或者包含多层判断、异常处理、循环、远程调用，就会明显降低可读性。

推荐控制方式如下：

| 场景                 | 建议                          |
| -------------------- | ----------------------------- |
| 单行表达式           | 可以直接写 Lambda             |
| 2 到 5 行简单逻辑    | 可以使用代码块 Lambda         |
| 多个条件判断         | 拆分为多个 `Predicate` 后组合 |
| 复杂转换逻辑         | 抽取为独立方法                |
| 涉及数据库或远程接口 | 优先使用普通方法或策略类      |
| 多处复用             | 抽取为具名变量、方法或类      |

下面示例展示可读性较差的写法。

```java
orders.stream()
        .filter(order -> order != null && order.getAmount() != null && order.getAmount().compareTo(new BigDecimal("100")) > 0 && Integer.valueOf(2).equals(order.getStatus()))
        .map(order -> order.getOrderNo() + ":" + order.getAmount() + ":" + order.getStatus())
        .collect(Collectors.toList());
```

建议拆分为具名函数式变量或独立方法。

文件位置：`src/main/java/io/github/atengk/functional/spec/ReadableLambdaDemo.java`

```java
package io.github.atengk.functional.spec;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Lambda 可读性控制示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ReadableLambdaDemo {

    public static void main(String[] args) {
        List<OrderInfo> orders = CollUtil.newArrayList(
                new OrderInfo("ORDER-1001", new BigDecimal("99.90"), 1),
                new OrderInfo("ORDER-1002", new BigDecimal("199.00"), 2)
        );

        Predicate<OrderInfo> validOrder = ReadableLambdaDemo::isValidOrder;
        Predicate<OrderInfo> paidOrder = order -> Integer.valueOf(2).equals(order.getStatus());
        Predicate<OrderInfo> highAmountOrder = order -> order.getAmount().compareTo(new BigDecimal("100.00")) > 0;
        Function<OrderInfo, String> orderSummaryMapper = ReadableLambdaDemo::buildOrderSummary;

        List<String> result = orders.stream()
                .filter(validOrder.and(paidOrder).and(highAmountOrder))
                .map(orderSummaryMapper)
                .collect(Collectors.toList());

        result.forEach(System.out::println);
    }

    /**
     * 判断订单是否有效
     *
     * @param order 订单信息
     * @return 是否有效
     */
    private static boolean isValidOrder(OrderInfo order) {
        return ObjectUtil.isNotNull(order) && ObjectUtil.isNotNull(order.getAmount());
    }

    /**
     * 构建订单摘要
     *
     * @param order 订单信息
     * @return 订单摘要
     */
    private static String buildOrderSummary(OrderInfo order) {
        return order.getOrderNo() + ":" + order.getAmount() + ":" + order.getStatus();
    }
}
```

上面示例依赖的订单对象如下。

文件位置：`src/main/java/io/github/atengk/functional/spec/OrderInfo.java`

```java
package io.github.atengk.functional.spec;

import java.math.BigDecimal;

/**
 * 订单信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderInfo {

    private final String orderNo;

    private final BigDecimal amount;

    private final Integer status;

    public OrderInfo(String orderNo, BigDecimal amount, Integer status) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getStatus() {
        return status;
    }
}
```

可读性控制的核心原则是：Lambda 表达式负责表达行为，不负责承载大段业务流程。

### 空值处理

函数式接口与 Stream 配合使用时，空值问题比较常见。空集合、空对象、空字段、空函数参数都需要明确处理，否则容易出现 `NullPointerException`。

常见空值处理建议如下：

| 场景                  | 建议                                                |
| --------------------- | --------------------------------------------------- |
| 集合可能为 `null`     | 使用 Hutool `CollUtil.emptyIfNull` 或提前返回空集合 |
| 字符串可能为空        | 使用 `StrUtil.isBlank`、`StrUtil.isNotBlank`        |
| 对象可能为空          | 使用 `ObjectUtil.isNull`、`ObjectUtil.isNotNull`    |
| 函数参数可能为空      | 在公共工具方法中主动校验                            |
| Lambda 返回值可能为空 | 明确调用方是否允许空值                              |
| Stream 中存在空元素   | 先 `filter(ObjectUtil::isNotNull)`                  |

下面示例演示集合和元素的空值处理。

文件位置：`src/main/java/io/github/atengk/functional/spec/NullSafeStreamDemo.java`

```java
package io.github.atengk.functional.spec;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 空值安全处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NullSafeStreamDemo {

    public static void main(String[] args) {
        List<String> usernames = CollUtil.newArrayList("admin", null, " ateng ", "", "test");

        List<String> result = CollUtil.emptyIfNull(usernames).stream()
                .filter(ObjectUtil::isNotNull)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .collect(Collectors.toList());

        result.forEach(System.out::println);
    }
}
```

对于工具类中接收函数式接口参数的方法，应主动判断函数式接口对象是否为空。

文件位置：`src/main/java/io/github/atengk/functional/spec/NullSafeFunctionUtils.java`

```java
package io.github.atengk.functional.spec;

import cn.hutool.core.util.ObjectUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 空值安全函数式工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NullSafeFunctionUtils {

    private NullSafeFunctionUtils() {
    }

    /**
     * 当对象非空时执行转换，否则返回默认值
     *
     * @param value 原始对象
     * @param function 转换函数
     * @param defaultSupplier 默认值提供器
     * @param <T> 原始对象类型
     * @param <R> 返回对象类型
     * @return 转换结果或默认值
     */
    public static <T, R> R mapIfNotNull(T value, Function<T, R> function, Supplier<R> defaultSupplier) {
        if (ObjectUtil.isNull(function)) {
            throw new IllegalArgumentException("转换函数不能为空");
        }
        if (ObjectUtil.isNull(defaultSupplier)) {
            throw new IllegalArgumentException("默认值提供器不能为空");
        }
        if (ObjectUtil.isNull(value)) {
            return defaultSupplier.get();
        }
        return function.apply(value);
    }
}
```

空值处理不建议全部依赖 `Optional`。`Optional` 更适合作为返回值表达“可能不存在”，不建议作为方法参数或实体类字段滥用。

### 日志与异常规范

函数式接口中的异常处理应保持边界清晰。不要在 Lambda 中随意吞掉异常，也不要在 Stream 链路里写大量 `try-catch`。公共执行模板、回调模板、异常包装工具中应添加必要日志，业务 Lambda 内部则保持简洁。

日志建议如下：

| 场景                | 是否建议记录日志 | 说明                 |
| ------------------- | ---------------- | -------------------- |
| 执行业务模板        | 建议             | 记录开始、结束、耗时 |
| 捕获并包装异常      | 建议             | 保留原始异常堆栈     |
| 简单数据转换        | 不建议           | 避免日志噪声         |
| Stream 每个元素处理 | 谨慎             | 数据量大时可能刷屏   |
| 规则命中关键分支    | 可记录           | 适合排查业务规则问题 |

下面示例定义一个统一异常包装工具，用于把受检异常转换为运行时异常，并保留异常堆栈。

文件位置：`src/main/java/io/github/atengk/functional/spec/ThrowingConsumer.java`

```java
package io.github.atengk.functional.spec;

/**
 * 可抛异常的消费型接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    /**
     * 消费数据
     *
     * @param data 数据
     * @throws Exception 执行异常
     */
    void accept(T data) throws Exception;
}
```

下面工具类用于统一包装 `ThrowingConsumer`，避免在每个 Lambda 中重复写异常处理。

文件位置：`src/main/java/io/github/atengk/functional/spec/ExceptionFunctionUtils.java`

```java
package io.github.atengk.functional.spec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 函数式接口异常工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ExceptionFunctionUtils {

    private static final Logger log = LoggerFactory.getLogger(ExceptionFunctionUtils.class);

    private ExceptionFunctionUtils() {
    }

    /**
     * 将可抛异常的消费逻辑包装为普通 Consumer
     *
     * @param throwingConsumer 可抛异常的消费逻辑
     * @param <T> 参数类型
     * @return 普通 Consumer
     */
    public static <T> Consumer<T> uncheckedConsumer(ThrowingConsumer<T> throwingConsumer) {
        return data -> {
            try {
                throwingConsumer.accept(data);
            } catch (Exception e) {
                log.error("函数式消费逻辑执行失败，data={}", data, e);
                throw new IllegalStateException("函数式消费逻辑执行失败", e);
            }
        };
    }
}
```

下面示例演示异常包装工具的使用方式。

文件位置：`src/main/java/io/github/atengk/functional/spec/ExceptionFunctionDemo.java`

```java
package io.github.atengk.functional.spec;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 函数式接口异常处理规范示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ExceptionFunctionDemo {

    public static void main(String[] args) {
        List<String> values = CollUtil.newArrayList("1", "2", "error", "3");

        values.forEach(ExceptionFunctionUtils.uncheckedConsumer(value -> {
            if ("error".equals(value)) {
                throw new IllegalArgumentException("非法数据");
            }
            System.out.println("处理数据：" + value);
        }));
    }
}
```

异常处理规范如下：

| 规范                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 不吞异常                   | 捕获异常后必须返回明确失败结果或继续抛出     |
| 保留原始异常               | 包装异常时传入 `cause`                       |
| 不在 Lambda 中堆积异常逻辑 | 抽取为包装工具或普通方法                     |
| 日志信息应包含业务上下文   | 如任务名、规则编码、订单号等                 |
| 避免重复打印异常           | 底层和全局异常处理器不要同时大量打印同一异常 |

## 测试与验证

函数式接口的测试重点不是语法是否可用，而是行为是否符合预期。测试时应覆盖正常输入、异常输入、边界输入、组合逻辑和异常包装逻辑。

### 单元测试设计

函数式接口单元测试建议围绕“输入是什么、行为是什么、输出是什么”进行设计。对于自定义函数式接口，需要重点验证核心抽象方法和默认方法组合逻辑。

下面示例定义一个可组合的金额规则接口。

文件位置：`src/main/java/io/github/atengk/functional/test/AmountRule.java`

```java
package io.github.atengk.functional.test;

import java.math.BigDecimal;

/**
 * 金额规则函数式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@FunctionalInterface
public interface AmountRule {

    /**
     * 判断金额是否满足规则
     *
     * @param amount 金额
     * @return 是否满足
     */
    boolean match(BigDecimal amount);

    /**
     * 与另一个金额规则组合
     *
     * @param other 另一个金额规则
     * @return 组合后的金额规则
     */
    default AmountRule and(AmountRule other) {
        return amount -> this.match(amount) && other.match(amount);
    }
}
```

下面使用 JUnit 5 测试金额规则的正常匹配和组合行为。

文件位置：`src/test/java/io/github/atengk/functional/test/AmountRuleTest.java`

```java
package io.github.atengk.functional.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * 金额规则单元测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class AmountRuleTest {

    @Test
    void shouldMatchWhenAmountGreaterThanMinAmount() {
        AmountRule minAmountRule = amount -> amount.compareTo(new BigDecimal("100.00")) >= 0;

        boolean matched = minAmountRule.match(new BigDecimal("199.00"));

        Assertions.assertTrue(matched);
    }

    @Test
    void shouldMatchWhenAllRulesPassed() {
        AmountRule minAmountRule = amount -> amount.compareTo(new BigDecimal("100.00")) >= 0;
        AmountRule maxAmountRule = amount -> amount.compareTo(new BigDecimal("500.00")) <= 0;

        AmountRule combinedRule = minAmountRule.and(maxAmountRule);

        boolean matched = combinedRule.match(new BigDecimal("199.00"));

        Assertions.assertTrue(matched);
    }

    @Test
    void shouldNotMatchWhenAnyRuleFailed() {
        AmountRule minAmountRule = amount -> amount.compareTo(new BigDecimal("100.00")) >= 0;
        AmountRule maxAmountRule = amount -> amount.compareTo(new BigDecimal("500.00")) <= 0;

        AmountRule combinedRule = minAmountRule.and(maxAmountRule);

        boolean matched = combinedRule.match(new BigDecimal("999.00"));

        Assertions.assertFalse(matched);
    }
}
```

如果 Java 8 项目还没有引入 JUnit 5，可以添加以下测试依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Hutool 工具类，用于字符串、集合、对象、日期等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- JUnit 5 单元测试框架，支持 Java 8 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

测试函数式接口时，建议覆盖以下内容：

| 测试点   | 说明                                  |
| -------- | ------------------------------------- |
| 正常输入 | 验证 Lambda 行为是否符合预期          |
| 非法输入 | 验证异常或失败结果是否明确            |
| 默认方法 | 验证 `and`、`or`、`negate` 等组合行为 |
| 静态方法 | 验证工厂方法返回的函数行为            |
| 空值输入 | 验证是否符合空值处理约定              |
| 异常包装 | 验证原始异常是否被保留                |

### Lambda 行为验证

Lambda 行为验证应关注函数式接口传入后的实际执行效果。尤其是当 Lambda 作为方法参数传递时，需要验证调用方是否按预期执行了传入行为。

下面定义一个用户名称处理器，接收 `Function<String, String>` 完成格式化。

文件位置：`src/main/java/io/github/atengk/functional/test/UsernameService.java`

```java
package io.github.atengk.functional.test;

import cn.hutool.core.util.StrUtil;

import java.util.function.Function;

/**
 * 用户名称服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UsernameService {

    /**
     * 格式化用户名
     *
     * @param username 用户名
     * @param formatter 格式化函数
     * @return 格式化后的用户名
     */
    public String format(String username, Function<String, String> formatter) {
        if (StrUtil.isBlank(username)) {
            return "guest";
        }
        return formatter.apply(username);
    }
}
```

下面测试 Lambda 是否被正确执行。

文件位置：`src/test/java/io/github/atengk/functional/test/UsernameServiceTest.java`

```java
package io.github.atengk.functional.test;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 用户名称服务测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class UsernameServiceTest {

    @Test
    void shouldApplyFormatterWhenUsernameIsNotBlank() {
        UsernameService usernameService = new UsernameService();

        String result = usernameService.format(" ateng ", username -> "用户：" + StrUtil.trim(username));

        Assertions.assertEquals("用户：ateng", result);
    }

    @Test
    void shouldReturnGuestWhenUsernameIsBlank() {
        UsernameService usernameService = new UsernameService();

        String result = usernameService.format("", username -> "用户：" + StrUtil.trim(username));

        Assertions.assertEquals("guest", result);
    }
}
```

对于行为较复杂的 Lambda，不建议只在调用处测试最终结果。可以把 Lambda 对应逻辑抽取为独立方法，然后分别测试独立方法和集成调用。

Lambda 行为验证建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 验证传入行为是否被执行 | 如格式化函数、回调函数、策略函数     |
| 验证短路逻辑           | 如参数非法时是否跳过 Lambda          |
| 验证组合顺序           | 如 `compose` 和 `andThen` 的执行顺序 |
| 验证异常传播           | Lambda 抛异常后是否被正确包装        |
| 避免测试实现细节       | 重点测试输入输出和行为结果           |

### 边界场景验证

边界场景是函数式接口测试中最容易遗漏的部分。尤其在 Stream 链路、动态规则和异常包装工具中，边界数据可能导致空指针、规则误判或异常被吞掉。

常见边界场景如下：

| 场景              | 应验证内容                     |
| ----------------- | ------------------------------ |
| 空集合            | 是否返回空集合而不是 `null`    |
| 集合元素为 `null` | 是否过滤或明确抛出异常         |
| 字符串为空        | 是否按约定返回默认值或失败结果 |
| 数值为边界值      | 如等于最小金额、等于最大金额   |
| 未配置策略        | 是否抛出明确异常               |
| Lambda 参数为空   | 是否主动校验函数参数           |
| Lambda 内部抛异常 | 是否保留原异常信息             |

下面定义一个订单过滤服务，用于演示边界测试。

文件位置：`src/main/java/io/github/atengk/functional/test/OrderFilterService.java`

```java
package io.github.atengk.functional.test;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 订单过滤服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderFilterService {

    /**
     * 根据条件过滤订单
     *
     * @param orders 订单列表
     * @param predicate 过滤条件
     * @return 过滤后的订单列表
     */
    public List<TestOrderInfo> filter(List<TestOrderInfo> orders, Predicate<TestOrderInfo> predicate) {
        if (ObjectUtil.isNull(predicate)) {
            throw new IllegalArgumentException("过滤条件不能为空");
        }

        return CollUtil.emptyIfNull(orders).stream()
                .filter(ObjectUtil::isNotNull)
                .filter(predicate)
                .collect(Collectors.toList());
    }
}
```

下面定义测试用订单对象。

文件位置：`src/main/java/io/github/atengk/functional/test/TestOrderInfo.java`

```java
package io.github.atengk.functional.test;

import java.math.BigDecimal;

/**
 * 测试订单信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TestOrderInfo {

    private final String orderNo;

    private final BigDecimal amount;

    public TestOrderInfo(String orderNo, BigDecimal amount) {
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
```

下面测试空集合、空元素、边界金额和空 Predicate。

文件位置：`src/test/java/io/github/atengk/functional/test/OrderFilterServiceTest.java`

```java
package io.github.atengk.functional.test;

import cn.hutool.core.collection.CollUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/**
 * 订单过滤服务边界测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class OrderFilterServiceTest {

    private final OrderFilterService orderFilterService = new OrderFilterService();

    @Test
    void shouldReturnEmptyListWhenOrdersIsNull() {
        List<TestOrderInfo> result = orderFilterService.filter(null, order -> true);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreNullElementWhenOrdersContainsNull() {
        List<TestOrderInfo> orders = CollUtil.newArrayList(
                new TestOrderInfo("ORDER-1001", new BigDecimal("100.00")),
                null
        );

        List<TestOrderInfo> result = orderFilterService.filter(orders, order -> true);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void shouldMatchWhenAmountEqualsMinAmount() {
        List<TestOrderInfo> orders = CollUtil.newArrayList(
                new TestOrderInfo("ORDER-1001", new BigDecimal("100.00"))
        );

        Predicate<TestOrderInfo> minAmountRule = order -> order.getAmount().compareTo(new BigDecimal("100.00")) >= 0;

        List<TestOrderInfo> result = orderFilterService.filter(orders, minAmountRule);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void shouldThrowExceptionWhenPredicateIsNull() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> orderFilterService.filter(CollUtil.newArrayList(), null)
        );

        Assertions.assertEquals("过滤条件不能为空", exception.getMessage());
    }
}
```

边界测试的目标是把函数式接口的行为约定固定下来，避免后续维护中因为调整 Lambda、Stream 链路或工具方法导致行为变化。

## 总结

函数式接口是 Java 8 函数式编程的基础能力。它通过单一抽象方法承载一段可传递的行为，使代码可以用更轻量的方式完成参数校验、数据转换、条件过滤、回调处理和策略扩展。

### 适用场景

函数式接口适合以下场景：

| 场景                           | 推荐接口              |
| ------------------------------ | --------------------- |
| 遍历处理、回调执行、消息消费   | `Consumer<T>`         |
| 延迟获取、默认值生成、对象创建 | `Supplier<T>`         |
| 对象转换、字段映射、格式化     | `Function<T, R>`      |
| 参数校验、条件过滤、规则判断   | `Predicate<T>`        |
| 两个参数参与计算               | `BiFunction<T, U, R>` |
| 同类型输入输出转换             | `UnaryOperator<T>`    |
| 两个同类型参数合并             | `BinaryOperator<T>`   |
| 业务语义强于通用接口           | 自定义函数式接口      |

在项目中，函数式接口更适合用于“行为可变、流程稳定”的场景。例如固定的执行模板中传入不同回调逻辑，固定的过滤流程中传入不同判断规则，固定的策略容器中注册不同计算方式。

不适合使用函数式接口的场景如下：

| 场景                   | 原因                        |
| ---------------------- | --------------------------- |
| 业务流程很长           | Lambda 会降低可读性         |
| 逻辑依赖多个外部组件   | 普通类或 Spring Bean 更清晰 |
| 需要复杂状态维护       | 函数式接口不适合承载状态    |
| 策略逻辑复杂且需要扩展 | 传统策略类更利于维护        |
| 异常处理路径复杂       | 普通方法更容易表达异常边界  |

### 使用建议

函数式接口使用建议如下：

| 建议                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 优先使用 JDK 内置接口  | `Consumer`、`Supplier`、`Function`、`Predicate` 能覆盖大多数场景 |
| 自定义接口要有业务语义 | 如 `OrderValidator`、`DiscountStrategy`                      |
| Lambda 保持短小        | 复杂逻辑抽取为方法或类                                       |
| 注意空值处理           | 集合、元素、函数参数都需要明确约定                           |
| 异常不要吞掉           | 保留原始异常并给出业务上下文                                 |
| Stream 不要滥用        | 数据库过滤、分页、聚合应优先交给数据库                       |
| 默认方法用于组合增强   | 如 `and`、`or`、`negate`                                     |
| 测试覆盖边界行为       | 包括空值、异常、组合规则和边界数值                           |

最终原则是：函数式接口用于提升表达力，而不是单纯压缩代码行数。能让业务行为更清晰、更容易组合、更容易复用时，使用函数式接口；如果会让代码变得隐晦，就应回到普通方法、普通类或传统面向对象设计。
