# Lambda 表达式

## Lambda 表达式概述

Lambda 表达式是 Java 8 引入的一种语法特性，用于简化函数式接口的实现。它可以把一段行为逻辑作为参数进行传递，使代码从“如何创建对象”转向“具体执行什么行为”。

在 Java 中，Lambda 表达式不能独立存在，必须依赖函数式接口。函数式接口是指只有一个抽象方法的接口，例如 `Runnable`、`Comparator`、`Consumer`、`Function`、`Predicate` 等。

Lambda 表达式的基本形式如下：

```java
(参数列表) -> {
    方法体
}
```

如果方法体只有一行代码，可以省略 `{}`；如果只有一个参数，也可以省略参数列表中的 `()`。

### Lambda 的作用

Lambda 的主要作用是简化匿名内部类写法，让代码更加简洁，并且更适合表达“传入一段行为逻辑”的场景。

在传统写法中，如果要给线程传入一个任务，通常需要使用匿名内部类。

下面的代码演示匿名内部类的传统写法。

```java
package io.github.atengk.basic.lambda;

/**
 * 匿名内部类任务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnonymousTaskDemo {

    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("使用匿名内部类执行任务");
            }
        };

        Thread thread = new Thread(task);
        thread.start();
    }
}
```

使用 Lambda 表达式后，可以将代码简化为下面的形式。

下面的代码演示 Lambda 表达式创建线程任务。

```java
package io.github.atengk.basic.lambda;

/**
 * Lambda 任务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaTaskDemo {

    public static void main(String[] args) {
        Runnable task = () -> System.out.println("使用 Lambda 表达式执行任务");

        Thread thread = new Thread(task);
        thread.start();
    }
}
```

可以看到，Lambda 表达式省略了接口实现类、方法重写和多余的结构代码，直接保留了核心执行逻辑。

Lambda 的常见作用包括：

| 作用           | 说明                               |
| -------------- | ---------------------------------- |
| 简化代码       | 减少匿名内部类的模板代码           |
| 传递行为       | 可以把一段逻辑作为参数传递给方法   |
| 提高可读性     | 简短逻辑更容易阅读                 |
| 配合集合操作   | 常用于遍历、排序、过滤、转换       |
| 支持函数式编程 | 可以配合 Stream API 编写声明式代码 |

下面的代码演示 Lambda 在集合遍历、排序和过滤中的常见作用。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lambda 基础作用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaActionDemo {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Tom");
        names.add("Jerry");
        names.add("Alice");
        names.add("Bob");

        if (CollUtil.isEmpty(names)) {
            System.out.println("名称列表为空");
            return;
        }

        names.forEach(name -> System.out.println("遍历名称：" + name));

        names.sort(Comparator.comparingInt(String::length));
        System.out.println("按名称长度排序：" + names);

        List<String> result = names.stream()
                .filter(name -> name.length() > 3)
                .toList();

        System.out.println("名称长度大于 3 的数据：" + result);
    }
}
```

在这个示例中，`forEach` 接收的是遍历逻辑，`sort` 接收的是排序规则，`filter` 接收的是过滤条件。Lambda 表达式让这些行为逻辑可以直接写在方法调用中。

### Lambda 与匿名内部类

Lambda 表达式和匿名内部类都可以用来实现接口，但两者并不完全相同。

匿名内部类可以用于接口、抽象类和普通类，而 Lambda 表达式只能用于函数式接口。函数式接口只能有一个抽象方法，因此 Lambda 表达式实际上是在为这个唯一的抽象方法提供实现。

例如，`Runnable` 接口只有一个 `run` 方法，因此可以使用 Lambda 表达式实现。

下面的代码分别使用匿名内部类和 Lambda 表达式实现同一个 `Runnable` 任务。

```java
package io.github.atengk.basic.lambda;

/**
 * Lambda 与匿名内部类对比示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaAndAnonymousDemo {

    public static void main(String[] args) {
        Runnable anonymousTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类执行任务");
            }
        };

        Runnable lambdaTask = () -> System.out.println("Lambda 表达式执行任务");

        anonymousTask.run();
        lambdaTask.run();
    }
}
```

两种写法执行结果类似，但代码结构不同。匿名内部类更像是创建了一个接口的实现对象，而 Lambda 表达式更像是直接传入了一段方法逻辑。

两者主要区别如下：

| 对比项       | Lambda 表达式              | 匿名内部类                   |
| ------------ | -------------------------- | ---------------------------- |
| 适用范围     | 只能用于函数式接口         | 可以用于接口、抽象类、普通类 |
| 代码结构     | 简洁，关注行为本身         | 结构完整，但代码较长         |
| 抽象方法数量 | 目标接口只能有一个抽象方法 | 没有该限制                   |
| `this` 指向  | 指向外部类对象             | 指向匿名内部类对象           |
| 使用场景     | 简单行为逻辑               | 复杂实现逻辑                 |

需要特别注意 `this` 的指向问题。Lambda 表达式中的 `this` 指向当前外部类对象，而匿名内部类中的 `this` 指向匿名内部类对象本身。

下面的代码演示两者中 `this` 指向的区别。

```java
package io.github.atengk.basic.lambda;

/**
 * Lambda 与匿名内部类 this 指向示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaThisDemo {

    public static void main(String[] args) {
        LambdaThisDemo demo = new LambdaThisDemo();
        demo.testThis();
    }

    /**
     * 测试 this 指向
     */
    public void testThis() {
        Runnable anonymousTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类 this：" + this.getClass().getName());
            }
        };

        Runnable lambdaTask = () -> System.out.println("Lambda this：" + this.getClass().getName());

        anonymousTask.run();
        lambdaTask.run();
    }
}
```

因此，Lambda 表达式不是匿名内部类的完全替代品。对于简单的函数式接口实现，优先使用 Lambda；对于实现逻辑复杂、需要额外成员变量或多个方法的场景，匿名内部类仍然更合适。

### Lambda 的适用场景

Lambda 表达式适合用于逻辑短小、行为明确、目标类型是函数式接口的场景。它常用于集合处理、线程任务、排序规则、条件判断、数据转换和回调处理。

#### 1. 集合遍历

集合遍历是 Lambda 最常见的使用场景之一。

```java
List<String> names = List.of("Tom", "Jerry", "Alice");

names.forEach(name -> System.out.println("名称：" + name));
```

如果只是直接调用已有方法，可以使用方法引用进一步简化。

```java
names.forEach(System.out::println);
```

#### 2. 集合排序

Lambda 可以用于实现自定义排序规则。

```java
List<String> names = new ArrayList<>();
names.add("Tom");
names.add("Jerry");
names.add("Alice");

names.sort((name1, name2) -> name1.length() - name2.length());

System.out.println(names);
```

更推荐下面这种写法，可读性更好。

```java
names.sort(Comparator.comparingInt(String::length));
```

#### 3. 条件过滤

Lambda 经常配合 Stream API 实现数据过滤。

```java
List<String> names = List.of("Tom", "Jerry", "Alice", "Bob");

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .toList();

System.out.println(result);
```

其中 `name -> name.length() > 3` 表示过滤条件：只保留长度大于 `3` 的字符串。

#### 4. 数据转换

Lambda 可以用于将一种数据转换成另一种数据。

```java
List<String> names = List.of("Tom", "Jerry", "Alice");

List<Integer> lengths = names.stream()
        .map(name -> name.length())
        .toList();

System.out.println(lengths);
```

这段代码将字符串集合转换成了字符串长度集合。

也可以使用方法引用简化。

```java
List<Integer> lengths = names.stream()
        .map(String::length)
        .toList();
```

#### 5. 线程任务

`Runnable` 是函数式接口，因此可以直接使用 Lambda 创建线程任务。

```java
Thread thread = new Thread(() -> System.out.println("线程任务执行中"));
thread.start();
```

如果任务逻辑较复杂，不建议将所有代码都写在 Lambda 中，可以提取成独立方法。

下面的代码演示将线程任务逻辑提取到独立方法中。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.date.DateUtil;

/**
 * Lambda 线程任务优化示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaThreadDemo {

    public static void main(String[] args) {
        Thread thread = new Thread(() -> executeTask("数据同步任务"));
        thread.start();
    }

    /**
     * 执行任务
     *
     * @param taskName 任务名称
     */
    private static void executeTask(String taskName) {
        System.out.println("任务开始：" + taskName + "，时间：" + DateUtil.now());

        System.out.println("任务处理中：" + taskName);

        System.out.println("任务结束：" + taskName + "，时间：" + DateUtil.now());
    }
}
```

#### 6. 回调处理

Lambda 也适合用于回调场景。方法本身只负责定义流程，具体处理逻辑由调用方传入。

下面的代码演示通过 `Consumer` 接收外部传入的处理逻辑。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Consumer;

/**
 * Lambda 回调处理示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaCallbackDemo {

    public static void main(String[] args) {
        handleMessage("操作成功", message -> System.out.println("接收到消息：" + message));
        handleMessage("", message -> System.out.println("接收到消息：" + message));
    }

    /**
     * 处理消息
     *
     * @param message 消息内容
     * @param consumer 消息处理逻辑
     */
    public static void handleMessage(String message, Consumer<String> consumer) {
        if (StrUtil.isBlank(message)) {
            System.out.println("消息内容为空，跳过处理");
            return;
        }

        consumer.accept(message);
    }
}
```

这个示例中，`handleMessage` 方法并不关心消息如何处理，只负责在消息合法时执行传入的处理逻辑。调用方可以根据不同业务场景传入不同的 Lambda 表达式。

实际开发中，Lambda 表达式适合短小、清晰的逻辑。如果 Lambda 内部出现大量判断、循环、异常处理或多层嵌套，应优先提取为独立方法，避免影响代码可读性。



## 函数式接口

函数式接口是 Lambda 表达式能够工作的基础。Lambda 表达式本身并不是一种独立的数据类型，它必须依附于某个函数式接口，由接口中的唯一抽象方法决定 Lambda 的参数列表和返回值类型。

### 函数式接口定义

函数式接口指的是只包含一个抽象方法的接口。这个抽象方法也称为函数描述符，它决定了 Lambda 表达式应该接收什么参数、返回什么结果。

需要注意的是，函数式接口中可以包含默认方法、静态方法和 `Object` 类中的公共方法，但只能有一个真正需要实现的抽象方法。

下面的代码定义了一个简单的函数式接口，用于接收两个整数并返回计算结果。

```java
package io.github.atengk.basic.lambda;

/**
 * 计算器函数式接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@FunctionalInterface
public interface Calculator {

    /**
     * 执行计算
     *
     * @param num1 第一个数字
     * @param num2 第二个数字
     * @return 计算结果
     */
    int calculate(int num1, int num2);

    /**
     * 输出接口说明
     */
    default void printInfo() {
        System.out.println("这是一个计算器函数式接口");
    }

    /**
     * 获取接口名称
     *
     * @return 接口名称
     */
    static String getName() {
        return "Calculator";
    }
}
```

在这个接口中，`calculate` 是唯一的抽象方法，因此 `Calculator` 是函数式接口。`printInfo` 是默认方法，`getName` 是静态方法，它们不会影响函数式接口的判断。

可以通过 Lambda 表达式实现这个接口。

```java
package io.github.atengk.basic.lambda;

/**
 * 函数式接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CalculatorDemo {

    public static void main(String[] args) {
        Calculator addCalculator = (num1, num2) -> num1 + num2;
        Calculator multiplyCalculator = (num1, num2) -> num1 * num2;

        System.out.println("加法结果：" + addCalculator.calculate(10, 5));
        System.out.println("乘法结果：" + multiplyCalculator.calculate(10, 5));

        addCalculator.printInfo();
        System.out.println("接口名称：" + Calculator.getName());
    }
}
```

这段代码中，`(num1, num2) -> num1 + num2` 就是对 `calculate` 方法的实现。由于 `calculate` 方法接收两个 `int` 参数并返回一个 `int` 结果，所以 Lambda 表达式也必须符合这个方法签名。

### @FunctionalInterface 注解

`@FunctionalInterface` 是 Java 提供的函数式接口标识注解。它的作用类似于 `@Override`，主要用于让编译器检查当前接口是否满足函数式接口规则。

如果接口使用了 `@FunctionalInterface` 注解，但接口中存在多个抽象方法，编译器会直接报错。

下面的接口是合法的函数式接口。

```java
package io.github.atengk.basic.lambda;

/**
 * 消息处理函数式接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * 处理消息
     *
     * @param message 消息内容
     */
    void handle(String message);
}
```

下面的接口则不是合法的函数式接口，因为它包含了两个抽象方法。

```java
package io.github.atengk.basic.lambda;

/**
 * 错误的函数式接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * 处理消息
     *
     * @param message 消息内容
     */
    void handle(String message);

    /**
     * 保存消息
     *
     * @param message 消息内容
     */
    void save(String message);
}
```

上面的代码会编译失败，因为 `handle` 和 `save` 都是抽象方法。函数式接口只能有一个抽象方法。

实际开发中，建议在自定义函数式接口上添加 `@FunctionalInterface` 注解。这样可以明确接口用途，并且避免后续维护时误加抽象方法导致 Lambda 无法使用。

### Lambda 与函数式接口的关系

Lambda 表达式必须依赖函数式接口才能使用。可以理解为：函数式接口定义方法规范，Lambda 表达式提供方法实现。

例如下面这个函数式接口：

```java
@FunctionalInterface
public interface MessageHandler {

    void handle(String message);
}
```

它的抽象方法是：

```java
void handle(String message);
```

因此对应的 Lambda 表达式必须接收一个 `String` 参数，并且没有返回值。

```java
MessageHandler handler = message -> System.out.println("处理消息：" + message);
```

Lambda 表达式赋值给函数式接口变量后，就可以像普通接口对象一样调用方法。

下面的代码演示 Lambda 表达式与函数式接口之间的完整关系。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

/**
 * Lambda 与函数式接口关系示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaFunctionalInterfaceDemo {

    public static void main(String[] args) {
        MessageHandler handler = message -> {
            if (StrUtil.isBlank(message)) {
                System.out.println("消息为空，跳过处理");
                return;
            }
            System.out.println("处理消息：" + message);
        };

        handler.handle("用户登录成功");
        handler.handle("");
    }
}
```

在这个示例中，`MessageHandler` 决定了 Lambda 表达式的形态，Lambda 表达式则提供了 `handle` 方法的具体实现。

## Lambda 基本语法

Lambda 表达式的基本语法由参数列表、箭头符号和方法体组成。

```java
(参数列表) -> {
    方法体
}
```

其中：

| 组成部分 | 说明                                |
| -------- | ----------------------------------- |
| 参数列表 | 对应函数式接口抽象方法的参数        |
| `->`     | Lambda 操作符，用于分隔参数和方法体 |
| 方法体   | 对应函数式接口抽象方法的实现逻辑    |

Lambda 表达式的写法会根据参数数量、返回值和方法体复杂度进行简化。

### 无参数无返回值

无参数无返回值的 Lambda 表达式，常见于 `Runnable` 这类接口。

`Runnable` 的抽象方法是：

```java
void run();
```

因此对应的 Lambda 表达式不需要参数，也不需要返回值。

```java
package io.github.atengk.basic.lambda;

/**
 * 无参数无返回值 Lambda 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class NoParamNoReturnDemo {

    public static void main(String[] args) {
        Runnable task = () -> System.out.println("执行无参数无返回值任务");

        task.run();
    }
}
```

如果方法体有多行代码，需要使用 `{}` 包裹。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.date.DateUtil;

/**
 * 多行 Lambda 任务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MultiLineTaskDemo {

    public static void main(String[] args) {
        Runnable task = () -> {
            System.out.println("任务开始时间：" + DateUtil.now());
            System.out.println("任务执行中");
            System.out.println("任务执行完成");
        };

        task.run();
    }
}
```

### 有参数无返回值

有参数无返回值的 Lambda 表达式，常见于 `Consumer<T>` 接口。

`Consumer<T>` 的核心抽象方法是：

```java
void accept(T t);
```

它接收一个参数，但不返回结果。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Consumer;

/**
 * 有参数无返回值 Lambda 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ParamNoReturnDemo {

    public static void main(String[] args) {
        Consumer<String> printMessage = message -> {
            if (StrUtil.isBlank(message)) {
                System.out.println("消息为空");
                return;
            }
            System.out.println("消息内容：" + message);
        };

        printMessage.accept("Lambda 表达式");
        printMessage.accept("");
    }
}
```

如果只有一个参数，参数列表中的小括号可以省略。

```java
Consumer<String> printMessage = message -> System.out.println(message);
```

如果显式声明参数类型，则小括号不能省略。

```java
Consumer<String> printMessage = (String message) -> System.out.println(message);
```

### 有参数有返回值

有参数有返回值的 Lambda 表达式，常见于 `Function<T, R>`、`Predicate<T>`、`Comparator<T>` 等接口。

例如 `Function<T, R>` 的核心抽象方法是：

```java
R apply(T t);
```

它接收一个参数，并返回一个结果。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Function;

/**
 * 有参数有返回值 Lambda 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ParamReturnDemo {

    public static void main(String[] args) {
        Function<String, Integer> lengthFunction = text -> {
            if (StrUtil.isBlank(text)) {
                return 0;
            }
            return text.length();
        };

        System.out.println("字符串长度：" + lengthFunction.apply("Lambda"));
        System.out.println("字符串长度：" + lengthFunction.apply(""));
    }
}
```

如果 Lambda 方法体只有一行返回逻辑，可以省略 `{}` 和 `return`。

```java
Function<String, Integer> lengthFunction = text -> text.length();
```

使用 `Predicate<T>` 时，返回值固定为 `boolean`。

```java
Predicate<String> checkName = name -> StrUtil.isNotBlank(name) && name.length() > 3;
```

这段代码表示判断字符串是否非空，并且长度是否大于 `3`。

### 参数类型推断

Lambda 表达式支持参数类型推断。编译器会根据函数式接口中抽象方法的参数类型，自动推断 Lambda 参数类型。

例如：

```java
Consumer<String> consumer = message -> System.out.println(message);
```

这里虽然没有写出 `message` 的类型，但编译器可以根据 `Consumer<String>` 推断出 `message` 是 `String` 类型。

下面的代码演示显式参数类型和省略参数类型的写法。

```java
package io.github.atengk.basic.lambda;

import java.util.function.BiFunction;

/**
 * Lambda 参数类型推断示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TypeInferenceDemo {

    public static void main(String[] args) {
        BiFunction<Integer, Integer, Integer> addFunction1 = (Integer num1, Integer num2) -> num1 + num2;

        BiFunction<Integer, Integer, Integer> addFunction2 = (num1, num2) -> num1 + num2;

        System.out.println("显式声明参数类型：" + addFunction1.apply(10, 20));
        System.out.println("自动推断参数类型：" + addFunction2.apply(10, 20));
    }
}
```

参数类型推断需要注意以下规则：

| 规则                                 | 示例                                              |
| ------------------------------------ | ------------------------------------------------- |
| 参数类型可以全部省略                 | `(num1, num2) -> num1 + num2`                     |
| 参数类型要么全部写，要么全部不写     | `(Integer num1, num2) -> num1 + num2` 是错误写法  |
| 单个参数且不写类型时，可以省略小括号 | `message -> System.out.println(message)`          |
| 单个参数但写了类型时，不能省略小括号 | `(String message) -> System.out.println(message)` |
| 多个参数时，小括号不能省略           | `(num1, num2) -> num1 + num2`                     |

## 常用函数式接口

Java 在 `java.util.function` 包中提供了很多常用函数式接口，开发中一般优先使用这些标准接口，而不是自己定义接口。

常用函数式接口主要包括 `Consumer`、`Supplier`、`Function`、`Predicate`、`UnaryOperator` 和 `BinaryOperator`。

| 接口                | 抽象方法              | 作用                               |
| ------------------- | --------------------- | ---------------------------------- |
| `Consumer<T>`       | `void accept(T t)`    | 接收一个参数，无返回值             |
| `Supplier<T>`       | `T get()`             | 不接收参数，返回一个结果           |
| `Function<T, R>`    | `R apply(T t)`        | 接收一个参数，返回一个结果         |
| `Predicate<T>`      | `boolean test(T t)`   | 接收一个参数，返回布尔值           |
| `UnaryOperator<T>`  | `T apply(T t)`        | 接收一个参数，返回同类型结果       |
| `BinaryOperator<T>` | `T apply(T t1, T t2)` | 接收两个同类型参数，返回同类型结果 |

### Consumer

`Consumer<T>` 表示消费型接口，接收一个参数，但没有返回值。它常用于打印、保存、发送消息、数据处理等场景。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Consumer;

/**
 * Consumer 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConsumerDemo {

    public static void main(String[] args) {
        Consumer<String> messageConsumer = message -> {
            if (StrUtil.isBlank(message)) {
                System.out.println("消息为空，不执行消费逻辑");
                return;
            }
            System.out.println("消费消息：" + message);
        };

        messageConsumer.accept("订单创建成功");
        messageConsumer.accept("");
    }
}
```

`Consumer` 还可以通过 `andThen` 组合多个消费逻辑。

```java
package io.github.atengk.basic.lambda;

import java.util.function.Consumer;

/**
 * Consumer 组合使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConsumerAndThenDemo {

    public static void main(String[] args) {
        Consumer<String> printOriginal = message -> System.out.println("原始消息：" + message);
        Consumer<String> printUpper = message -> System.out.println("大写消息：" + message.toUpperCase());

        Consumer<String> consumer = printOriginal.andThen(printUpper);

        consumer.accept("lambda");
    }
}
```

### Supplier

`Supplier<T>` 表示供给型接口，不接收参数，但会返回一个结果。它常用于对象创建、默认值提供、延迟加载等场景。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.date.DateUtil;

import java.util.function.Supplier;

/**
 * Supplier 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SupplierDemo {

    public static void main(String[] args) {
        Supplier<String> timeSupplier = () -> DateUtil.now();

        System.out.println("当前时间：" + timeSupplier.get());
    }
}
```

下面的示例演示使用 `Supplier` 提供默认值。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/**
 * Supplier 默认值示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SupplierDefaultDemo {

    public static void main(String[] args) {
        String username = "";
        String result = getValueOrDefault(username, () -> "默认用户");

        System.out.println("用户名：" + result);
    }

    /**
     * 获取有效值或默认值
     *
     * @param value 原始值
     * @param supplier 默认值提供者
     * @return 有效值或默认值
     */
    private static String getValueOrDefault(String value, Supplier<String> supplier) {
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return supplier.get();
    }
}
```

### Function

`Function<T, R>` 表示函数型接口，接收一个参数并返回一个结果。它常用于数据转换，例如对象转字符串、字符串转数字、实体转 VO 等。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Function;

/**
 * Function 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FunctionDemo {

    public static void main(String[] args) {
        Function<String, Integer> lengthFunction = text -> {
            if (StrUtil.isBlank(text)) {
                return 0;
            }
            return text.length();
        };

        System.out.println("字符串长度：" + lengthFunction.apply("Lambda"));
        System.out.println("字符串长度：" + lengthFunction.apply(""));
    }
}
```

`Function` 可以通过 `andThen` 进行结果链式处理。

```java
package io.github.atengk.basic.lambda;

import java.util.function.Function;

/**
 * Function 链式处理示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FunctionAndThenDemo {

    public static void main(String[] args) {
        Function<String, String> trimFunction = String::trim;
        Function<String, String> upperFunction = String::toUpperCase;

        Function<String, String> finalFunction = trimFunction.andThen(upperFunction);

        System.out.println("处理结果：" + finalFunction.apply(" lambda "));
    }
}
```

### Predicate

`Predicate<T>` 表示断言型接口，接收一个参数并返回 `boolean`。它常用于条件判断、数据过滤、参数校验等场景。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Predicate;

/**
 * Predicate 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PredicateDemo {

    public static void main(String[] args) {
        List<String> names = List.of("Tom", "Jerry", "Alice", "", "Bob");

        Predicate<String> validNamePredicate = name -> StrUtil.isNotBlank(name) && name.length() > 3;

        List<String> result = names.stream()
                .filter(validNamePredicate)
                .toList();

        System.out.println("符合条件的名称：" + result);
    }
}
```

`Predicate` 支持 `and`、`or`、`negate` 组合条件。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.Predicate;

/**
 * Predicate 条件组合示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PredicateComposeDemo {

    public static void main(String[] args) {
        Predicate<String> notBlank = StrUtil::isNotBlank;
        Predicate<String> lengthGreaterThanThree = name -> name.length() > 3;
        Predicate<String> startsWithA = name -> name.startsWith("A");

        Predicate<String> finalPredicate = notBlank
                .and(lengthGreaterThanThree)
                .and(startsWithA);

        System.out.println("Alice 是否符合条件：" + finalPredicate.test("Alice"));
        System.out.println("Tom 是否符合条件：" + finalPredicate.test("Tom"));
    }
}
```

### UnaryOperator 与 BinaryOperator

`UnaryOperator<T>` 和 `BinaryOperator<T>` 都是 `Function` 的特殊形式。

`UnaryOperator<T>` 接收一个 `T` 类型参数，并返回一个 `T` 类型结果。它适合用于同类型数据转换。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.function.UnaryOperator;

/**
 * UnaryOperator 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UnaryOperatorDemo {

    public static void main(String[] args) {
        UnaryOperator<String> formatName = name -> {
            if (StrUtil.isBlank(name)) {
                return "未知用户";
            }
            return name.trim().toUpperCase();
        };

        System.out.println("格式化结果：" + formatName.apply(" ateng "));
        System.out.println("格式化结果：" + formatName.apply(""));
    }
}
```

`BinaryOperator<T>` 接收两个 `T` 类型参数，并返回一个 `T` 类型结果。它适合用于同类型数据合并、比较、计算等场景。

```java
package io.github.atengk.basic.lambda;

import java.util.function.BinaryOperator;

/**
 * BinaryOperator 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BinaryOperatorDemo {

    public static void main(String[] args) {
        BinaryOperator<Integer> addOperator = (num1, num2) -> num1 + num2;
        BinaryOperator<Integer> maxOperator = Integer::max;

        System.out.println("求和结果：" + addOperator.apply(10, 20));
        System.out.println("最大值：" + maxOperator.apply(10, 20));
    }
}
```

两者与 `Function` 的关系如下：

| 接口                | 等价关系              | 说明                       |
| ------------------- | --------------------- | -------------------------- |
| `UnaryOperator<T>`  | `Function<T, T>`      | 参数类型和返回值类型相同   |
| `BinaryOperator<T>` | `BiFunction<T, T, T>` | 两个参数和返回值类型都相同 |

实际开发中，如果参数类型和返回值类型不同，使用 `Function`；如果参数类型和返回值类型相同，优先使用 `UnaryOperator`；如果两个参数和返回值都是同一种类型，优先使用 `BinaryOperator`。



## 方法引用

方法引用是 Lambda 表达式的一种简化写法。当 Lambda 表达式的方法体只是调用一个已经存在的方法时，就可以使用方法引用让代码更简洁。

方法引用的核心作用是复用已有方法逻辑，减少重复书写 Lambda 表达式。它并不是新的功能，而是 Lambda 表达式的语法简化形式。

方法引用常见格式如下：

| 类型         | 格式                 | 示例                |
| ------------ | -------------------- | ------------------- |
| 静态方法引用 | `类名::静态方法名`   | `Integer::parseInt` |
| 实例方法引用 | `对象名::实例方法名` | `printer::print`    |
| 对象方法引用 | `类名::实例方法名`   | `String::length`    |
| 构造方法引用 | `类名::new`          | `User::new`         |

### 静态方法引用

静态方法引用用于引用某个类中的静态方法。它的格式是：

```java
类名::静态方法名
```

如果 Lambda 表达式只是调用一个静态方法，就可以改写为静态方法引用。

例如：

```java
Function<String, Integer> function = text -> Integer.parseInt(text);
```

可以简化为：

```java
Function<String, Integer> function = Integer::parseInt;
```

下面的代码演示静态方法引用的完整使用方式。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Function;

/**
 * 静态方法引用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StaticMethodReferenceDemo {

    public static void main(String[] args) {
        List<String> numberTextList = List.of("10", "20", "30");

        Function<String, Integer> parseFunction = Integer::parseInt;

        List<Integer> numberList = numberTextList.stream()
                .filter(StrUtil::isNotBlank)
                .map(parseFunction)
                .toList();

        System.out.println("转换结果：" + numberList);
    }
}
```

在这个示例中，`Integer::parseInt` 等价于 `text -> Integer.parseInt(text)`。`StrUtil::isNotBlank` 也是静态方法引用，等价于 `text -> StrUtil.isNotBlank(text)`。

静态方法引用适合用于数据转换、参数校验、工具类方法调用等场景。

### 实例方法引用

实例方法引用用于引用某个具体对象的实例方法。它的格式是：

```java
对象名::实例方法名
```

如果 Lambda 表达式只是调用某个对象的方法，就可以使用实例方法引用。

例如：

```java
Consumer<String> consumer = message -> printer.print(message);
```

可以简化为：

```java
Consumer<String> consumer = printer::print;
```

下面的代码演示实例方法引用的完整使用方式。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * 实例方法引用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InstanceMethodReferenceDemo {

    public static void main(String[] args) {
        MessagePrinter printer = new MessagePrinter();

        Consumer<String> printConsumer = printer::print;

        List<String> messages = List.of("订单创建成功", "支付完成", "");

        messages.stream()
                .filter(StrUtil::isNotBlank)
                .forEach(printConsumer);
    }

    /**
     * 消息打印器
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class MessagePrinter {

        /**
         * 打印消息
         *
         * @param message 消息内容
         */
        public void print(String message) {
            System.out.println("消息内容：" + message);
        }
    }
}
```

在这个示例中，`printer::print` 引用了 `printer` 对象的 `print` 方法。每次消费字符串时，都会调用这个对象的实例方法。

实例方法引用适合用于复用某个已经存在的对象能力，例如消息打印器、格式化器、处理器、校验器等。

### 对象方法引用

对象方法引用通常指“某个类型的任意对象的实例方法引用”。它的格式是：

```java
类名::实例方法名
```

这种写法看起来和静态方法引用相似，但含义不同。静态方法引用调用的是类的静态方法，而对象方法引用调用的是对象自己的实例方法。

例如：

```java
Function<String, Integer> function = text -> text.length();
```

可以简化为：

```java
Function<String, Integer> function = String::length;
```

下面的代码演示对象方法引用的完整使用方式。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.Comparator;
import java.util.List;

/**
 * 对象方法引用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ObjectMethodReferenceDemo {

    public static void main(String[] args) {
        List<String> names = List.of("Tom", "Jerry", "Alice", "Bob", "");

        List<Integer> lengthList = names.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::length)
                .toList();

        System.out.println("名称长度：" + lengthList);

        List<String> sortedList = names.stream()
                .filter(StrUtil::isNotBlank)
                .sorted(Comparator.comparingInt(String::length))
                .toList();

        System.out.println("按长度排序：" + sortedList);
    }
}
```

在这个示例中，`String::length` 等价于 `name -> name.length()`。方法引用中的第一个参数会作为调用方法的对象。

对象方法引用适合用于调用元素自身的方法，例如 `String::length`、`String::trim`、`String::toUpperCase`、`User::getName` 等。

### 构造方法引用

构造方法引用用于引用类的构造方法。它的格式是：

```java
类名::new
```

当 Lambda 表达式只是创建对象时，可以使用构造方法引用简化。

例如：

```java
Supplier<User> supplier = () -> new User();
```

可以简化为：

```java
Supplier<User> supplier = User::new;
```

下面的代码演示无参数构造方法引用。

```java
package io.github.atengk.basic.lambda;

import java.util.function.Supplier;

/**
 * 无参数构造方法引用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorReferenceDemo {

    public static void main(String[] args) {
        Supplier<User> userSupplier = User::new;

        User user = userSupplier.get();
        user.setName("Ateng");

        System.out.println("用户信息：" + user);
    }

    /**
     * 用户实体
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class User {

        private String name;

        public User() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "'}";
        }
    }
}
```

如果构造方法有参数，可以使用 `Function`、`BiFunction` 等函数式接口接收参数。

下面的代码演示有参数构造方法引用。

```java
package io.github.atengk.basic.lambda;

import java.util.function.BiFunction;

/**
 * 有参数构造方法引用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorReferenceWithParamDemo {

    public static void main(String[] args) {
        BiFunction<Long, String, User> userCreator = User::new;

        User user = userCreator.apply(1L, "Ateng");

        System.out.println("用户信息：" + user);
    }

    /**
     * 用户实体
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class User {

        private Long id;

        private String name;

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "'}";
        }
    }
}
```

构造方法引用适合用于对象创建、集合数据转换、工厂方法等场景。

## 变量捕获

变量捕获指 Lambda 表达式访问外部变量的能力。Lambda 表达式可以访问局部变量、成员变量和静态变量，但不同变量的访问规则并不相同。

变量捕获最常见的问题是局部变量必须是 `final` 或者 effectively final。

### 局部变量捕获

Lambda 表达式可以访问所在方法中的局部变量，但这个局部变量不能在后续代码中被修改。

下面的代码演示 Lambda 捕获局部变量。

```java
package io.github.atengk.basic.lambda;

import java.util.function.Consumer;

/**
 * Lambda 局部变量捕获示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalVariableCaptureDemo {

    public static void main(String[] args) {
        String prefix = "消息内容：";

        Consumer<String> consumer = message -> System.out.println(prefix + message);

        consumer.accept("Lambda 表达式");
    }
}
```

在这个示例中，Lambda 表达式访问了外部局部变量 `prefix`。由于 `prefix` 初始化后没有再被修改，因此可以被 Lambda 捕获。

下面这种写法是错误的：

```java
String prefix = "消息内容：";

Consumer<String> consumer = message -> System.out.println(prefix + message);

prefix = "新的消息内容：";
```

这段代码会编译失败，因为 `prefix` 被 Lambda 捕获后又被重新赋值，不再满足 effectively final 的要求。

### effectively final

effectively final 的意思是：变量虽然没有显式使用 `final` 修饰，但从初始化之后就没有再被修改。

下面两种写法对 Lambda 来说效果是一样的。

显式使用 `final`：

```java
final String prefix = "消息内容：";

Consumer<String> consumer = message -> System.out.println(prefix + message);
```

没有使用 `final`，但没有被修改：

```java
String prefix = "消息内容：";

Consumer<String> consumer = message -> System.out.println(prefix + message);
```

第二种写法中的 `prefix` 就是 effectively final。

下面的代码演示 effectively final 的正确使用方式。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Predicate;

/**
 * effectively final 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EffectivelyFinalDemo {

    public static void main(String[] args) {
        String keyword = "a";

        Predicate<String> predicate = name -> StrUtil.isNotBlank(name) && name.contains(keyword);

        List<String> names = List.of("Ateng", "Tom", "Java", "Spring");

        List<String> result = names.stream()
                .filter(predicate)
                .toList();

        System.out.println("匹配结果：" + result);
    }
}
```

这里的 `keyword` 没有被修改，因此可以在 Lambda 中使用。

如果确实需要在 Lambda 中改变外部状态，通常可以使用对象成员变量、集合、原子类或自定义上下文对象。但在集合流式操作中，应尽量避免修改外部状态，否则可能影响代码可读性和线程安全。

下面的代码演示使用 `AtomicInteger` 在 Lambda 中记录计数。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lambda 中计数示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaCounterDemo {

    public static void main(String[] args) {
        List<String> names = List.of("Tom", "Jerry", "Alice");

        if (CollUtil.isEmpty(names)) {
            System.out.println("名称列表为空");
            return;
        }

        AtomicInteger counter = new AtomicInteger();

        names.forEach(name -> {
            int index = counter.incrementAndGet();
            System.out.println("第 " + index + " 个名称：" + name);
        });
    }
}
```

这种方式可以实现计数，但实际开发中要谨慎使用。如果只是需要下标遍历，普通 `for` 循环可能更加直观。

### 成员变量访问

Lambda 表达式可以直接访问和修改成员变量。成员变量不受 effectively final 规则限制。

下面的代码演示 Lambda 访问和修改成员变量。

```java
package io.github.atengk.basic.lambda;

import java.util.function.Consumer;

/**
 * Lambda 成员变量访问示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MemberVariableAccessDemo {

    private int count = 0;

    public static void main(String[] args) {
        MemberVariableAccessDemo demo = new MemberVariableAccessDemo();
        demo.test();
    }

    /**
     * 测试成员变量访问
     */
    public void test() {
        Consumer<String> consumer = message -> {
            count++;
            System.out.println("第 " + count + " 次处理消息：" + message);
        };

        consumer.accept("用户登录");
        consumer.accept("用户下单");
    }
}
```

Lambda 中可以修改 `count`，因为 `count` 是成员变量，不是局部变量。

不过需要注意，如果 Lambda 在多线程环境中修改成员变量，可能会产生线程安全问题。此时应根据业务情况使用同步控制、线程安全集合、原子类或其他并发工具。

## Lambda 实践

Lambda 在实际开发中经常配合集合和 Stream API 使用。常见操作包括集合遍历、集合排序、条件过滤和数据转换。

下面示例使用一个简单的用户类作为数据模型。

```java
package io.github.atengk.basic.lambda;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

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

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + ", enabled=" + enabled + "}";
    }
}
```

### 集合遍历

集合遍历可以使用 `forEach` 配合 Lambda 表达式完成。相比传统 `for` 循环，`forEach` 更适合表达“对每个元素执行某个操作”。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * Lambda 集合遍历示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaForEachDemo {

    public static void main(String[] args) {
        List<User> users = buildUsers();

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        users.forEach(user -> System.out.println("用户信息：" + user));

        users.forEach(System.out::println);
    }

    /**
     * 构建用户列表
     *
     * @return 用户列表
     */
    private static List<User> buildUsers() {
        return List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 20, true),
                new User(3L, "Jerry", 16, false)
        );
    }
}
```

如果只是打印元素，可以使用 `System.out::println`。如果需要执行复杂逻辑，建议提取为独立方法，避免 Lambda 体过长。

### 集合排序

集合排序可以使用 `sort`、`sorted` 和 `Comparator` 完成。Lambda 可以直接描述排序规则。

下面的代码演示按照年龄升序排序。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lambda 集合排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaSortDemo {

    public static void main(String[] args) {
        List<User> users = new ArrayList<>(buildUsers());

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        users.sort((user1, user2) -> user1.getAge().compareTo(user2.getAge()));
        System.out.println("按年龄升序排序：" + users);

        users.sort(Comparator.comparing(User::getAge).reversed());
        System.out.println("按年龄降序排序：" + users);
    }

    /**
     * 构建用户列表
     *
     * @return 用户列表
     */
    private static List<User> buildUsers() {
        return List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 20, true),
                new User(3L, "Jerry", 16, false)
        );
    }
}
```

实际开发中，更推荐使用 `Comparator.comparing` 或 `Comparator.comparingInt`，因为它们语义更清晰。

### 条件过滤

条件过滤通常使用 Stream API 的 `filter` 方法。`filter` 接收一个 `Predicate`，用于判断元素是否保留。

下面的代码演示过滤启用状态的成年用户。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;

import java.util.List;

/**
 * Lambda 条件过滤示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaFilterDemo {

    public static void main(String[] args) {
        List<User> users = buildUsers();

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        List<User> result = users.stream()
                .filter(user -> BooleanUtil.isTrue(user.getEnabled()))
                .filter(user -> user.getAge() >= 18)
                .toList();

        System.out.println("启用状态的成年用户：" + result);
    }

    /**
     * 构建用户列表
     *
     * @return 用户列表
     */
    private static List<User> buildUsers() {
        return List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 20, true),
                new User(3L, "Jerry", 16, false),
                new User(4L, "Alice", 17, true)
        );
    }
}
```

多个 `filter` 可以连续使用，每个 `filter` 表达一个独立条件。这种写法比把所有条件写在一个复杂表达式中更容易阅读。

### 数据转换

数据转换通常使用 Stream API 的 `map` 方法。`map` 接收一个 `Function`，用于将一种对象转换成另一种对象。

下面的代码演示将用户列表转换成用户名列表。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Lambda 数据转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaMapDemo {

    public static void main(String[] args) {
        List<User> users = buildUsers();

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        List<String> names = users.stream()
                .map(User::getName)
                .filter(StrUtil::isNotBlank)
                .toList();

        System.out.println("用户名列表：" + names);
    }

    /**
     * 构建用户列表
     *
     * @return 用户列表
     */
    private static List<User> buildUsers() {
        return List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 20, true),
                new User(3L, "Jerry", 16, false)
        );
    }
}
```

如果需要转换成新的对象，可以使用构造方法或普通 Lambda 表达式。

下面的代码演示将 `User` 转换成 `UserVO`。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * Lambda 对象转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaObjectMapDemo {

    public static void main(String[] args) {
        List<User> users = buildUsers();

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        List<UserVO> userVOList = users.stream()
                .map(user -> new UserVO(user.getId(), user.getName()))
                .toList();

        System.out.println("用户视图列表：" + userVOList);
    }

    /**
     * 构建用户列表
     *
     * @return 用户列表
     */
    private static List<User> buildUsers() {
        return List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 20, true),
                new User(3L, "Jerry", 16, false)
        );
    }

    /**
     * 用户视图对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class UserVO {

        private Long id;

        private String name;

        public UserVO(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "UserVO{id=" + id + ", name='" + name + "'}";
        }
    }
}
```

数据转换是 Lambda 和 Stream API 中最常见的使用方式之一。实际开发中，经常用于实体转 VO、DTO 转实体、集合字段提取、类型转换等场景。

## 常见问题

本节整理 Lambda 表达式在学习和开发中常见的问题，包括与匿名内部类的区别、局部变量限制、可读性影响以及 `this` 指向问题。

### Lambda 与匿名内部类的区别

Lambda 表达式和匿名内部类都可以用于实现接口，但它们有明显区别。

| 对比项       | Lambda 表达式              | 匿名内部类                   |
| ------------ | -------------------------- | ---------------------------- |
| 使用前提     | 只能用于函数式接口         | 可以用于接口、抽象类、普通类 |
| 抽象方法数量 | 目标接口只能有一个抽象方法 | 没有函数式接口限制           |
| 写法         | 简洁，直接描述行为         | 结构完整，但代码较长         |
| `this` 指向  | 指向 Lambda 所在的外部对象 | 指向匿名内部类对象本身       |
| 适合场景     | 简单行为传递               | 复杂对象实现                 |

下面的代码演示两者的基本区别。

```java
package io.github.atengk.basic.lambda;

/**
 * Lambda 与匿名内部类区别示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaAnonymousDiffDemo {

    public static void main(String[] args) {
        Runnable anonymousTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类任务");
            }
        };

        Runnable lambdaTask = () -> System.out.println("Lambda 任务");

        anonymousTask.run();
        lambdaTask.run();
    }
}
```

简单来说，Lambda 更适合函数式接口的简短逻辑，匿名内部类更适合需要完整对象结构的复杂逻辑。

### Lambda 为什么要求局部变量 effectively final

Lambda 要求局部变量是 final 或 effectively final，主要是为了避免变量生命周期和并发语义带来的问题。

局部变量存放在线程栈中，方法执行结束后局部变量就会消失。而 Lambda 表达式可能在方法结束后才被执行。为了保证 Lambda 中访问到的局部变量值是稳定的，Java 要求被捕获的局部变量不能再被修改。

下面的代码是合法的：

```java
String prefix = "消息：";

Consumer<String> consumer = message -> System.out.println(prefix + message);

consumer.accept("Lambda");
```

下面的代码是不合法的：

```java
String prefix = "消息：";

Consumer<String> consumer = message -> System.out.println(prefix + message);

prefix = "新消息：";
```

第二段代码中，`prefix` 被 Lambda 捕获后又被重新赋值，因此不满足 effectively final 的要求。

如果业务确实需要变化的状态，可以使用成员变量、对象包装、集合或原子类。但要注意，这种写法可能增加副作用，尤其在并行流或多线程环境中更要谨慎。

### Lambda 是否会影响代码可读性

Lambda 是否影响代码可读性，取决于使用方式。

对于简短逻辑，Lambda 通常能提升可读性。例如：

```java
names.stream()
        .filter(name -> name.length() > 3)
        .toList();
```

这段代码能清楚表达过滤规则。

但如果 Lambda 内部逻辑过长，可读性会下降。例如：

```java
users.forEach(user -> {
    if (user.getName() != null && user.getAge() != null) {
        if (user.getAge() >= 18) {
            if (Boolean.TRUE.equals(user.getEnabled())) {
                System.out.println("有效用户：" + user);
            }
        }
    }
});
```

这种写法不推荐。可以将判断逻辑提取为独立方法。

```java
package io.github.atengk.basic.lambda;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Lambda 可读性优化示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaReadableDemo {

    public static void main(String[] args) {
        List<User> users = List.of(
                new User(1L, "Ateng", 18, true),
                new User(2L, "Tom", 16, true),
                new User(3L, "", 20, false)
        );

        users.stream()
                .filter(LambdaReadableDemo::isValidAdultUser)
                .forEach(user -> System.out.println("有效成年用户：" + user));
    }

    /**
     * 判断是否为有效成年用户
     *
     * @param user 用户信息
     * @return true 表示有效，false 表示无效
     */
    private static boolean isValidAdultUser(User user) {
        return user != null
                && StrUtil.isNotBlank(user.getName())
                && user.getAge() != null
                && user.getAge() >= 18
                && BooleanUtil.isTrue(user.getEnabled());
    }
}
```

实际开发中可以遵循一个原则：Lambda 负责表达流程，复杂业务逻辑交给独立方法。

### Lambda 中 this 指向谁

Lambda 中的 `this` 指向 Lambda 所在的外部对象，而不是 Lambda 表达式本身。

匿名内部类中的 `this` 指向匿名内部类对象本身。

下面的代码演示两者的区别。

```java
package io.github.atengk.basic.lambda;

/**
 * Lambda 中 this 指向示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LambdaThisDemo {

    public static void main(String[] args) {
        LambdaThisDemo demo = new LambdaThisDemo();
        demo.testThis();
    }

    /**
     * 测试 this 指向
     */
    public void testThis() {
        Runnable anonymousTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类 this：" + this.getClass().getName());
            }
        };

        Runnable lambdaTask = () -> System.out.println("Lambda this：" + this.getClass().getName());

        anonymousTask.run();
        lambdaTask.run();
    }
}
```

执行结果中，匿名内部类的 `this` 会指向匿名内部类对象，而 Lambda 的 `this` 会指向外部类 `LambdaThisDemo` 的对象。

因此，如果在 Lambda 中访问成员方法或成员变量，访问的是外部类对象的成员。这个特性在编写回调、线程任务和集合处理逻辑时需要特别注意。
