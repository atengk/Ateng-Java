# 异常处理

异常处理是 Java 程序稳定性设计中的重要组成部分。它用于处理程序运行过程中出现的非正常情况，使程序能够在发生错误时进行合理响应，而不是直接中断或产生不可预期的结果。

## 异常处理概述

异常处理的核心目标不是“避免所有错误”，而是在错误发生时能够识别错误、隔离错误、记录错误，并根据业务场景采取合理的恢复或终止策略。

在 Java 中，异常本质上是一个对象。程序运行过程中如果发生异常，JVM 会创建对应的异常对象，并沿着方法调用链向外抛出，直到被合适的异常处理逻辑捕获。如果异常一直没有被捕获，程序最终会终止当前线程，并输出异常堆栈信息。

### 异常的作用

异常的主要作用是将正常业务流程和异常处理流程分离，避免大量错误判断代码混杂在核心业务逻辑中。

在没有异常机制的情况下，方法通常需要通过返回值表示执行结果，例如返回 `null`、`false`、错误码等。这种方式会导致调用方必须频繁判断返回值，否则容易产生空指针、状态不一致或数据错误等问题。

使用异常机制后，方法可以在无法继续执行时主动抛出异常，由上层调用方统一处理。这样可以让正常逻辑更清晰，异常逻辑也更集中。

异常常见作用包括：

| 作用             | 说明                                                |
| ---------------- | --------------------------------------------------- |
| 表示程序异常状态 | 例如参数错误、文件不存在、网络中断、数据库连接失败  |
| 中断当前执行流程 | 当程序无法继续正确执行时，通过异常终止当前逻辑      |
| 向上传递错误信息 | 将底层错误传递给上层调用方，由上层决定如何处理      |
| 保留错误现场     | 通过异常类型、异常消息、堆栈信息定位问题            |
| 统一错误处理     | 在 Web 项目中可以通过全局异常处理器统一返回错误结果 |

下面的示例演示异常如何用于参数校验和错误中断。

该代码用于演示业务方法中如何通过异常表达非法参数，并使用 Hutool 进行字符串判空。

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class UserService {

    /**
     * 创建用户
     *
     * @param username 用户名
     * @return 创建结果
     */
    public String createUser(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("创建用户失败，用户名为空");
            throw new IllegalArgumentException("用户名不能为空");
        }

        log.info("创建用户成功，用户名：{}", username);
        return "用户创建成功：" + username;
    }
}
```

在这个示例中，`createUser` 方法不再通过 `false` 或错误码表示失败，而是直接抛出 `IllegalArgumentException`。调用方可以捕获该异常并进行统一处理，例如返回提示信息、记录日志或终止当前业务流程。

### 异常体系结构

Java 的异常体系以 `Throwable` 为根类。所有可以被抛出的错误或异常都必须是 `Throwable` 的子类。

整体结构如下：

```text
Throwable
├── Error
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── VirtualMachineError
└── Exception
    ├── IOException
    ├── SQLException
    ├── ClassNotFoundException
    └── RuntimeException
        ├── NullPointerException
        ├── IndexOutOfBoundsException
        ├── IllegalArgumentException
        └── ArithmeticException
```

`Throwable` 是 Java 异常体系的顶层父类，下面主要分为两大分支：`Error` 和 `Exception`。

`Error` 表示严重错误，通常由 JVM 或系统底层环境引发，应用程序一般不应该捕获和处理这类问题。例如内存溢出、栈溢出、虚拟机错误等。

`Exception` 表示程序可以处理的异常情况，也是日常开发中重点关注的部分。根据编译器是否强制处理，`Exception` 又可以分为受检异常和非受检异常。

常见异常类型说明如下：

| 异常类型                   | 所属体系   | 说明                                     |
| -------------------------- | ---------- | ---------------------------------------- |
| `Throwable`                | 顶层父类   | 所有异常和错误的父类                     |
| `Error`                    | 严重错误   | 通常表示 JVM 或系统级问题                |
| `Exception`                | 普通异常   | 程序可以捕获和处理的问题                 |
| `RuntimeException`         | 运行时异常 | 编译器不强制处理，通常由代码逻辑问题引起 |
| `IOException`              | 受检异常   | 文件、网络、输入输出相关异常             |
| `SQLException`             | 受检异常   | 数据库访问相关异常                       |
| `NullPointerException`     | 运行时异常 | 使用了空对象引用                         |
| `IllegalArgumentException` | 运行时异常 | 方法参数不合法                           |

可以使用下面的代码观察异常对象的基本信息。

该代码用于演示异常对象中常见的信息，包括异常类型、异常消息和堆栈信息。

```java
package io.github.atengk.basic.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 异常结构演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionStructureDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            int result = divide(10, 0);
            log.info("计算结果：{}", result);
        } catch (ArithmeticException e) {
            log.error("计算失败，异常类型：{}，异常信息：{}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * 两数相除
     *
     * @param num1 被除数
     * @param num2 除数
     * @return 计算结果
     */
    public static int divide(int num1, int num2) {
        return num1 / num2;
    }
}
```

执行后会出现 `ArithmeticException`，异常信息通常为 `/ by zero`。通过日志中的堆栈信息，可以定位异常发生的类、方法和代码行。

### Error 与 Exception

`Error` 和 `Exception` 都继承自 `Throwable`，但它们的语义和处理方式完全不同。

`Error` 表示严重错误，通常不是应用程序代码能够恢复的问题。比如 JVM 内存不足、线程栈溢出、类定义错误等。这类问题通常需要通过调整 JVM 参数、优化程序结构、修复系统环境或排查底层依赖来解决，而不是在业务代码中捕获后继续运行。

`Exception` 表示程序运行中可以预见或可以处理的异常情况。例如用户输入不合法、文件不存在、网络连接失败、数据库操作失败等。业务代码主要处理的是 `Exception` 及其子类。

两者对比如下：

| 对比项       | Error                                    | Exception                                         |
| ------------ | ---------------------------------------- | ------------------------------------------------- |
| 所属父类     | `Throwable`                              | `Throwable`                                       |
| 含义         | 严重错误                                 | 程序异常                                          |
| 是否建议捕获 | 通常不建议                               | 通常需要根据场景处理                              |
| 是否可恢复   | 多数不可恢复                             | 多数可以通过业务逻辑处理                          |
| 常见来源     | JVM、系统资源、底层环境                  | 业务代码、IO、数据库、网络                        |
| 示例         | `OutOfMemoryError`、`StackOverflowError` | `IOException`、`SQLException`、`RuntimeException` |

示例说明：

```java
try {
    // 业务代码
} catch (Exception e) {
    // 可以处理普通异常
}
```

这段代码只能捕获 `Exception` 及其子类，不能捕获 `Error`。如果希望捕获所有可抛出对象，需要捕获 `Throwable`，但在实际业务开发中通常不推荐这样做。

不推荐写法：

```java
try {
    // 业务代码
} catch (Throwable e) {
    // 不推荐：会把 Error 也捕获掉，可能掩盖严重系统问题
}
```

更推荐的做法是只捕获当前场景能够处理的异常类型。

该代码用于演示只捕获业务可处理异常，而不是直接捕获 `Throwable`。

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常捕获边界演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionBoundaryDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            checkUsername("");
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败：{}", e.getMessage());
        } catch (Exception e) {
            log.error("系统处理异常", e);
        }
    }

    /**
     * 校验用户名
     *
     * @param username 用户名
     */
    public static void checkUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        log.info("用户名校验通过：{}", username);
    }
}
```

在实际开发中，`Error` 一般交给 JVM 或运行环境处理，业务代码主要关注 `Exception`。捕获异常时应尽量捕获明确的异常类型，例如 `IllegalArgumentException`、`IOException`、`SQLException`，而不是一开始就捕获范围过大的 `Exception` 或 `Throwable`。



## 异常分类

异常分类主要用于判断异常是否必须在编译阶段处理，以及异常更适合由当前方法处理，还是继续向上抛出。Java 中常见分类方式包括受检异常、非受检异常和运行时异常。

### 受检异常

受检异常是编译器强制要求处理的异常，也称为 Checked Exception。只要方法中可能产生受检异常，就必须使用 `try-catch` 捕获，或者在方法签名上使用 `throws` 声明继续向上抛出。

受检异常通常表示程序可以预见、可以处理的外部问题，例如文件不存在、网络中断、数据库连接失败等。这类异常不一定是代码逻辑错误，而是运行环境或外部资源导致的问题。

常见受检异常包括：

| 异常类型                 | 说明                           |
| ------------------------ | ------------------------------ |
| `IOException`            | 输入输出异常，例如文件读取失败 |
| `SQLException`           | 数据库操作异常                 |
| `ClassNotFoundException` | 类加载失败                     |
| `InterruptedException`   | 线程中断异常                   |
| `FileNotFoundException`  | 文件不存在异常                 |

该代码用于演示受检异常的处理方式：读取文件时可能发生 `IOException`，因此必须捕获或声明抛出。

文件位置：`src/main/java/io/github/atengk/basic/exception/CheckedExceptionDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * 受检异常演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CheckedExceptionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String filePath = "data/user.txt";

        try {
            String content = readFile(filePath);
            log.info("文件读取成功，内容：{}", content);
        } catch (IOException e) {
            log.error("文件读取失败，文件路径：{}", filePath, e);
        }
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 文件不存在或读取失败时抛出
     */
    public static String readFile(String filePath) throws IOException {
        File file = FileUtil.file(filePath);

        if (!FileUtil.exist(file)) {
            throw new IOException("文件不存在：" + filePath);
        }

        return FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
    }
}
```

受检异常适合用于调用方必须明确处理的场景。例如读取文件失败后，调用方可以提示用户重新选择文件；数据库连接失败后，调用方可以执行重试或返回错误提示。

### 非受检异常

非受检异常是编译器不强制处理的异常，也称为 Unchecked Exception。非受检异常包括 `RuntimeException` 及其子类，也包括 `Error` 及其子类。

日常业务开发中重点关注的是 `RuntimeException`。它通常表示代码逻辑错误、参数错误、状态错误等问题。编译器不会强制要求使用 `try-catch` 或 `throws`，但程序运行时仍然可能抛出这些异常。

常见非受检异常包括：

| 异常类型                    | 说明         |
| --------------------------- | ------------ |
| `NullPointerException`      | 空指针异常   |
| `IllegalArgumentException`  | 参数非法异常 |
| `IndexOutOfBoundsException` | 索引越界异常 |
| `ArithmeticException`       | 算术异常     |
| `IllegalStateException`     | 状态非法异常 |
| `ClassCastException`        | 类型转换异常 |

该代码用于演示非受检异常：参数不合法时直接抛出 `IllegalArgumentException`，调用方不是必须捕获。

文件位置：`src/main/java/io/github/atengk/basic/exception/UncheckedExceptionDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 非受检异常演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class UncheckedExceptionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String ageText = "abc";

        try {
            Integer age = parseAge(ageText);
            log.info("年龄转换成功：{}", age);
        } catch (IllegalArgumentException e) {
            log.warn("年龄转换失败：{}", e.getMessage());
        }
    }

    /**
     * 转换年龄
     *
     * @param ageText 年龄文本
     * @return 年龄
     */
    public static Integer parseAge(String ageText) {
        if (StrUtil.isBlank(ageText)) {
            throw new IllegalArgumentException("年龄不能为空");
        }

        if (!NumberUtil.isInteger(ageText)) {
            throw new IllegalArgumentException("年龄必须是整数");
        }

        Integer age = Integer.valueOf(ageText);
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("年龄范围不合法");
        }

        return age;
    }
}
```

非受检异常虽然不强制捕获，但不代表可以忽略。在业务代码中，参数校验失败、状态非法、业务规则不满足等场景，通常可以使用运行时异常表达错误。

### 运行时异常

运行时异常是 `RuntimeException` 及其子类，属于非受检异常。它的特点是编译器不强制处理，但运行时如果没有被捕获，仍然会导致当前线程终止。

运行时异常通常来自以下几类问题：

| 场景           | 示例                               |
| -------------- | ---------------------------------- |
| 参数错误       | 传入空字符串、负数、不合法状态值   |
| 空对象访问     | 调用 `null` 对象的方法             |
| 数组或集合越界 | 访问不存在的索引                   |
| 类型转换错误   | 强制转换为不兼容类型               |
| 数学计算错误   | 除数为 0                           |
| 业务规则不满足 | 库存不足、余额不足、状态不允许操作 |

运行时异常适合表达“调用方传入了错误参数”或“当前状态不允许继续执行”这类问题。它不要求每一层都声明 `throws`，因此在业务异常设计中使用较多。

该代码用于演示运行时异常的典型使用方式：当订单金额不合法时，直接抛出异常中断流程。

文件位置：`src/main/java/io/github/atengk/basic/exception/RuntimeExceptionDemo.java`

```java
package io.github.atengk.basic.exception;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 运行时异常演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class RuntimeExceptionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            submitOrder(new BigDecimal("-10"));
        } catch (IllegalArgumentException e) {
            log.warn("订单提交失败：{}", e.getMessage());
        }
    }

    /**
     * 提交订单
     *
     * @param amount 订单金额
     */
    public static void submitOrder(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于 0");
        }

        log.info("订单提交成功，金额：{}", amount);
    }
}
```

运行时异常不应该被滥用。对于可以通过参数校验提前避免的问题，应尽早校验；对于外部资源失败的问题，例如文件、网络、数据库，更适合使用受检异常或在业务层转换成统一的业务异常。

## 异常捕获

异常捕获用于处理已经发生的异常。Java 使用 `try-catch-finally` 结构捕获异常，也可以使用 `try-with-resources` 自动释放资源。

异常捕获的重点不是“把异常吞掉”，而是根据场景进行合理处理，包括记录日志、返回提示、释放资源、转换异常或终止流程。

### try-catch

`try-catch` 是最基础的异常捕获方式。`try` 代码块中放置可能出现异常的代码，`catch` 代码块中处理指定类型的异常。

基本语法如下：

```java
try {
    // 可能发生异常的代码
} catch (ExceptionType e) {
    // 异常处理逻辑
}
```

该代码用于演示通过 `try-catch` 捕获数字转换异常，避免程序直接终止。

文件位置：`src/main/java/io/github/atengk/basic/exception/TryCatchDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * try-catch 演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class TryCatchDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String numberText = "100a";

        try {
            Integer number = parseNumber(numberText);
            log.info("数字转换成功：{}", number);
        } catch (IllegalArgumentException e) {
            log.warn("数字转换失败：{}", e.getMessage());
        }
    }

    /**
     * 转换数字
     *
     * @param numberText 数字文本
     * @return 数字
     */
    public static Integer parseNumber(String numberText) {
        if (StrUtil.isBlank(numberText)) {
            throw new IllegalArgumentException("数字文本不能为空");
        }

        if (!NumberUtil.isInteger(numberText)) {
            throw new IllegalArgumentException("数字文本格式不正确");
        }

        return Integer.valueOf(numberText);
    }
}
```

使用 `try-catch` 时，应优先捕获明确的异常类型。不要在所有地方直接捕获 `Exception`，否则容易掩盖真实错误，也会让异常处理逻辑变得粗糙。

推荐写法：

```java
try {
    Integer number = Integer.valueOf("abc");
} catch (NumberFormatException e) {
    log.warn("数字格式错误：{}", e.getMessage());
}
```

不推荐写法：

```java
try {
    Integer number = Integer.valueOf("abc");
} catch (Exception e) {
    log.error("系统异常", e);
}
```

### 多异常捕获

多异常捕获用于处理一个 `try` 代码块中可能出现多种异常的情况。Java 支持两种写法：多个 `catch` 分支，或者使用 `|` 合并多个异常类型。

多个 `catch` 分支适合不同异常需要不同处理逻辑的场景。

```java
try {
    // 可能发生多种异常的代码
} catch (NumberFormatException e) {
    // 处理数字格式异常
} catch (IllegalArgumentException e) {
    // 处理参数异常
}
```

使用 `|` 合并异常适合多种异常处理逻辑完全一致的场景。

```java
try {
    // 可能发生多种异常的代码
} catch (NumberFormatException | NullPointerException e) {
    // 多种异常使用相同处理逻辑
}
```

该代码用于演示多个异常分支的捕获顺序和处理方式。

文件位置：`src/main/java/io/github/atengk/basic/exception/MultiCatchDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 多异常捕获演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class MultiCatchDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<String> numberList = List.of("100", "abc");

        try {
            Integer number = getNumber(numberList, 1);
            log.info("获取数字成功：{}", number);
        } catch (IndexOutOfBoundsException e) {
            log.warn("集合索引越界：{}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("参数处理失败：{}", e.getMessage());
        } catch (Exception e) {
            log.error("未知异常", e);
        }
    }

    /**
     * 获取指定位置的数字
     *
     * @param numberList 数字文本集合
     * @param index      索引
     * @return 数字
     */
    public static Integer getNumber(List<String> numberList, int index) {
        if (CollUtil.isEmpty(numberList)) {
            throw new IllegalArgumentException("数字集合不能为空");
        }

        String numberText = numberList.get(index);
        if (!NumberUtil.isInteger(numberText)) {
            throw new IllegalArgumentException("数字格式不正确：" + numberText);
        }

        return Integer.valueOf(numberText);
    }
}
```

多异常捕获需要注意顺序。子类异常必须写在父类异常前面，否则父类会提前捕获异常，导致子类分支无法执行。

错误写法：

```java
try {
    Integer number = Integer.valueOf("abc");
} catch (Exception e) {
    log.error("系统异常", e);
} catch (NumberFormatException e) {
    log.warn("数字格式错误：{}", e.getMessage());
}
```

上面的代码无法通过编译，因为 `NumberFormatException` 已经被前面的 `Exception` 覆盖。

正确写法：

```java
try {
    Integer number = Integer.valueOf("abc");
} catch (NumberFormatException e) {
    log.warn("数字格式错误：{}", e.getMessage());
} catch (Exception e) {
    log.error("系统异常", e);
}
```

### finally 代码块

`finally` 代码块用于放置无论是否发生异常都需要执行的代码，常见用途是资源释放、状态清理、锁释放等。

基本语法如下：

```java
try {
    // 可能发生异常的代码
} catch (Exception e) {
    // 异常处理
} finally {
    // 最终执行的代码
}
```

`finally` 通常会执行，但不是绝对执行。以下场景可能导致 `finally` 不执行：

| 场景                 | 说明                       |
| -------------------- | -------------------------- |
| JVM 直接退出         | 例如调用 `System.exit(0)`  |
| JVM 崩溃             | 例如严重底层错误           |
| 当前线程被强制终止   | 极端情况下可能无法继续执行 |
| 机器断电或进程被杀死 | 操作系统层面终止进程       |

该代码用于演示 `finally` 在正常执行和异常执行后都会运行。

文件位置：`src/main/java/io/github/atengk/basic/exception/FinallyDemo.java`

```java
package io.github.atengk.basic.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * finally 代码块演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class FinallyDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        Integer result = divide(10, 0);
        log.info("计算结果：{}", result);
    }

    /**
     * 两数相除
     *
     * @param num1 被除数
     * @param num2 除数
     * @return 计算结果
     */
    public static Integer divide(int num1, int num2) {
        try {
            return num1 / num2;
        } catch (ArithmeticException e) {
            log.warn("除法计算失败：{}", e.getMessage());
            return null;
        } finally {
            log.info("除法计算流程结束");
        }
    }
}
```

`finally` 中不建议写 `return`。如果 `try` 或 `catch` 中已经有返回值，`finally` 中再次 `return` 会覆盖前面的返回结果，容易造成排查困难。

不推荐写法：

```java
public static int getValue() {
    try {
        return 1;
    } finally {
        return 2;
    }
}
```

这个方法最终返回 `2`，因为 `finally` 中的 `return` 覆盖了 `try` 中的返回值。

### try-with-resources

`try-with-resources` 是 Java 7 引入的资源管理语法，用于自动关闭资源。只要资源类实现了 `AutoCloseable` 或 `Closeable` 接口，就可以放在 `try` 后面的括号中。

它常用于文件流、网络连接、数据库连接等需要关闭的资源。

基本语法如下：

```java
try (Resource resource = new Resource()) {
    // 使用资源
} catch (Exception e) {
    // 处理异常
}
```

该代码用于演示使用 `try-with-resources` 读取文件，程序执行结束后会自动关闭输入流。

文件位置：`src/main/java/io/github/atengk/basic/exception/TryWithResourcesDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * try-with-resources 演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class TryWithResourcesDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String filePath = "data/user.txt";

        try {
            String content = readFile(filePath);
            log.info("文件读取成功，内容：{}", content);
        } catch (IOException e) {
            log.error("文件读取失败，文件路径：{}", filePath, e);
        }
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 文件读取失败时抛出
     */
    public static String readFile(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        }
    }
}
```

相比手动在 `finally` 中关闭资源，`try-with-resources` 更简洁，也更安全。即使读取过程中发生异常，资源也会自动关闭。

手动关闭资源的写法：

```java
InputStream inputStream = null;
try {
    inputStream = new FileInputStream("data/user.txt");
    String content = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
    log.info("文件内容：{}", content);
} catch (IOException e) {
    log.error("文件读取失败", e);
} finally {
    IoUtil.close(inputStream);
}
```

推荐写法：

```java
try (InputStream inputStream = new FileInputStream("data/user.txt")) {
    String content = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
    log.info("文件内容：{}", content);
} catch (IOException e) {
    log.error("文件读取失败", e);
}
```

在实际开发中，只要资源支持自动关闭，应优先使用 `try-with-resources`。

## 异常抛出

异常抛出用于主动中断当前方法的执行流程，并将异常对象交给调用方处理。Java 中主要通过 `throw` 和 `throws` 实现异常抛出。

`throw` 用于在方法体内部抛出一个具体异常对象；`throws` 用于在方法声明上说明当前方法可能向外抛出哪些异常。

### throw 关键字

`throw` 用于主动抛出异常对象。它后面必须跟一个 `Throwable` 或其子类对象。

基本语法如下：

```java
throw new ExceptionType("异常信息");
```

`throw` 常用于参数校验、状态校验、业务规则校验等场景。当程序发现当前条件不满足继续执行要求时，可以通过 `throw` 直接中断当前流程。

该代码用于演示通过 `throw` 主动抛出参数异常。

文件位置：`src/main/java/io/github/atengk/basic/exception/ThrowDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * throw 关键字演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThrowDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            register("admin", "");
        } catch (IllegalArgumentException e) {
            log.warn("用户注册失败：{}", e.getMessage());
        }
    }

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param password 密码
     */
    public static void register(String username, String password) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        log.info("用户注册成功，用户名：{}", username);
    }
}
```

`throw` 抛出异常后，当前方法会立即终止，后续代码不会继续执行。

示例：

```java
public static void checkName(String name) {
    if (StrUtil.isBlank(name)) {
        throw new IllegalArgumentException("名称不能为空");
    }

    log.info("名称校验通过：{}", name);
}
```

当 `name` 为空时，`throw` 后面的日志不会执行。

### throws 关键字

`throws` 用于声明方法可能抛出的异常。它写在方法参数列表之后、方法体之前。

基本语法如下：

```java
public void methodName() throws IOException {
    // 方法体
}
```

`throws` 本身不会抛出异常，它只是告诉调用方：当前方法内部可能产生这些异常，调用方需要继续捕获或继续声明抛出。

该代码用于演示使用 `throws` 将文件读取异常交给调用方处理。

文件位置：`src/main/java/io/github/atengk/basic/exception/ThrowsDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * throws 关键字演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThrowsDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String filePath = "data/config.txt";

        try {
            String config = loadConfig(filePath);
            log.info("配置加载成功：{}", config);
        } catch (IOException e) {
            log.error("配置加载失败，文件路径：{}", filePath, e);
        }
    }

    /**
     * 加载配置文件
     *
     * @param filePath 文件路径
     * @return 配置内容
     * @throws IOException 文件不存在或读取失败时抛出
     */
    public static String loadConfig(String filePath) throws IOException {
        File file = FileUtil.file(filePath);

        if (!FileUtil.exist(file)) {
            throw new IOException("配置文件不存在：" + filePath);
        }

        return FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
    }
}
```

`throws` 常用于当前方法不适合处理异常的场景。例如工具方法、底层方法、通用组件方法通常只负责完成核心逻辑，不直接决定异常如何响应，而是将异常交给上层业务方法处理。

`throw` 与 `throws` 的区别如下：

| 对比项           | throw                               | throws                             |
| ---------------- | ----------------------------------- | ---------------------------------- |
| 位置             | 方法体内部                          | 方法声明上                         |
| 作用             | 抛出一个具体异常对象                | 声明方法可能抛出的异常             |
| 后面跟的内容     | 异常对象                            | 异常类型                           |
| 数量             | 一次只能抛出一个异常对象            | 可以声明多个异常类型               |
| 是否真正抛出异常 | 是                                  | 否，只是声明                       |
| 示例             | `throw new IOException("读取失败")` | `throws IOException, SQLException` |

### 异常传播机制

异常传播机制指的是异常发生后，如果当前方法没有捕获处理，就会沿着方法调用链向上抛出，直到被某一层捕获。如果一直没有被捕获，异常最终会交给 JVM 处理，当前线程终止并输出堆栈信息。

调用链示例：

```text
main()
  └── controller()
        └── service()
              └── repository()
```

如果 `repository()` 中发生异常，但没有捕获，异常会依次向上传播到 `service()`、`controller()`、`main()`。任何一层都可以选择捕获处理。

该代码用于演示异常从底层方法向上层方法传播，并最终在入口方法中被捕获。

文件位置：`src/main/java/io/github/atengk/basic/exception/ExceptionPropagationDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常传播机制演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionPropagationDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            controller("");
        } catch (IllegalArgumentException e) {
            log.warn("请求处理失败：{}", e.getMessage());
        } catch (Exception e) {
            log.error("请求处理出现未知异常", e);
        }
    }

    /**
     * 模拟控制层
     *
     * @param username 用户名
     */
    public static void controller(String username) {
        log.info("控制层接收到请求");
        service(username);
    }

    /**
     * 模拟业务层
     *
     * @param username 用户名
     */
    public static void service(String username) {
        log.info("业务层开始处理用户数据");
        repository(username);
    }

    /**
     * 模拟数据层
     *
     * @param username 用户名
     */
    public static void repository(String username) {
        log.info("数据层开始校验用户数据");

        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        log.info("数据层用户数据校验通过");
    }
}
```

这段代码中，异常在 `repository` 方法中产生，但该方法没有捕获异常，因此异常会继续传播到 `service`，再传播到 `controller`，最终在 `main` 方法中被捕获。

异常传播需要注意以下几点：

| 规则                                     | 说明                         |
| ---------------------------------------- | ---------------------------- |
| 当前方法不捕获异常时，异常会向调用方传播 | 调用方可以继续捕获或继续抛出 |
| 运行时异常可以不声明 `throws`            | 但运行时仍然会传播           |
| 受检异常必须捕获或声明抛出               | 否则编译失败                 |
| 异常被捕获后，如果不重新抛出，传播终止   | 后续由当前捕获逻辑处理       |
| 异常类型会按 `catch` 顺序匹配            | 先匹配子类，再匹配父类       |

在实际项目中，常见做法是底层方法抛出明确异常，中间层根据需要补充业务上下文，最外层进行统一处理。例如 Web 项目中通常会在全局异常处理器中统一转换成标准响应结果。



## 自定义异常

自定义异常用于表达项目中的特定错误语义。Java 内置异常能够覆盖通用错误场景，例如参数非法、空指针、文件不存在等；但在业务系统中，经常需要表达更明确的业务错误，例如“余额不足”“订单状态不允许取消”“用户不存在”“权限不足”等，这时就适合定义自定义异常。

自定义异常通常分为两类：自定义运行时异常和自定义受检异常。实际业务开发中，自定义运行时异常更常见，尤其是在 Spring Boot 项目中，通常会配合全局异常处理器统一返回错误响应。

### 自定义运行时异常

自定义运行时异常需要继承 `RuntimeException`。它属于非受检异常，编译器不会强制调用方捕获或声明抛出。

运行时业务异常适合用于业务规则校验失败的场景，例如参数不合法、数据不存在、状态不允许、权限不足等。

该代码用于定义统一业务错误码，方便业务异常携带稳定的错误编码和错误信息。

文件位置：`src/main/java/io/github/atengk/basic/exception/ErrorCode.java`

```java
package io.github.atengk.basic.exception;

/**
 * 业务错误码
 *
 * @author Ateng
 * @since 2026-05-15
 */
public enum ErrorCode {

    /**
     * 请求参数错误
     */
    PARAM_ERROR("A0001", "请求参数错误"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND("B0001", "用户不存在"),

    /**
     * 订单状态错误
     */
    ORDER_STATUS_ERROR("B0002", "订单状态错误"),

    /**
     * 系统处理失败
     */
    SYSTEM_ERROR("C0001", "系统处理失败");

    private final String code;

    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误编码
     *
     * @return 错误编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getMessage() {
        return message;
    }
}
```

该代码用于定义业务运行时异常，支持错误码、错误信息和原始异常原因。

文件位置：`src/main/java/io/github/atengk/basic/exception/BusinessException.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    /**
     * 根据错误码创建业务异常
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 根据错误码和自定义信息创建业务异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(StrUtil.blankToDefault(message, errorCode.getMessage()));
        this.code = errorCode.getCode();
    }

    /**
     * 根据错误码和原始异常创建业务异常
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 根据错误码、自定义信息和原始异常创建业务异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误信息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(StrUtil.blankToDefault(message, errorCode.getMessage()), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 获取错误编码
     *
     * @return 错误编码
     */
    public String getCode() {
        return code;
    }
}
```

该代码用于演示业务运行时异常的使用方式，当用户不存在时主动抛出 `BusinessException`。

文件位置：`src/main/java/io/github/atengk/basic/exception/BusinessExceptionDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务运行时异常演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class BusinessExceptionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            getUserName("");
        } catch (BusinessException e) {
            log.warn("业务处理失败，错误码：{}，错误信息：{}", e.getCode(), e.getMessage());
        }
    }

    /**
     * 获取用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    public static String getUserName(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }

        if (!StrUtil.equals(userId, "1001")) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，用户ID：" + userId);
        }

        return "Ateng";
    }
}
```

在业务项目中，`BusinessException` 通常不会在每个方法里立即捕获，而是继续向上传播，最终由统一异常处理器捕获并转换为标准响应结果。

### 自定义受检异常

自定义受检异常需要继承 `Exception`。它属于 Checked Exception，调用方必须捕获或继续声明抛出。

受检异常适合用于调用方必须明确处理的场景，例如导入文件格式错误、外部接口调用失败、业务流程需要强制补偿等。不过在很多 Spring Boot 项目中，为了减少方法签名污染，业务异常更常使用运行时异常。

该代码用于定义文件导入受检异常，调用方必须显式处理该异常。

文件位置：`src/main/java/io/github/atengk/basic/exception/FileImportException.java`

```java
package io.github.atengk.basic.exception;

/**
 * 文件导入异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FileImportException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 根据错误信息创建文件导入异常
     *
     * @param message 错误信息
     */
    public FileImportException(String message) {
        super(message);
    }

    /**
     * 根据错误信息和原始异常创建文件导入异常
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public FileImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

该代码用于演示自定义受检异常的抛出和捕获。

文件位置：`src/main/java/io/github/atengk/basic/exception/CheckedCustomExceptionDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 自定义受检异常演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CheckedCustomExceptionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            importUsers(List.of("admin", "", "test"));
        } catch (FileImportException e) {
            log.error("文件导入失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 导入用户
     *
     * @param usernameList 用户名集合
     * @throws FileImportException 文件数据不合法时抛出
     */
    public static void importUsers(List<String> usernameList) throws FileImportException {
        if (CollUtil.isEmpty(usernameList)) {
            throw new FileImportException("导入用户列表不能为空");
        }

        for (int i = 0; i < usernameList.size(); i++) {
            String username = usernameList.get(i);
            if (StrUtil.isBlank(username)) {
                throw new FileImportException("第 " + (i + 1) + " 行用户名不能为空");
            }
        }

        log.info("用户导入成功，导入数量：{}", usernameList.size());
    }
}
```

受检异常的优点是约束调用方必须处理，缺点是会让方法签名持续向上传递 `throws`。如果异常属于业务规则失败，并且最终由全局异常处理器统一处理，通常更适合使用运行时异常。

### 业务异常设计

业务异常设计的核心是让异常具有明确的业务语义，同时方便统一处理、日志定位和前端展示。

一个较合理的业务异常通常包含以下信息：

| 信息       | 说明                                           |
| ---------- | ---------------------------------------------- |
| 错误码     | 稳定的错误标识，方便前后端约定和问题统计       |
| 错误信息   | 面向调用方或用户的提示信息                     |
| 原始异常   | 保留底层异常原因，方便排查问题                 |
| 业务上下文 | 例如用户ID、订单号、请求参数等关键信息         |
| 异常类型   | 用不同异常类型区分参数错误、业务错误、系统错误 |

业务异常设计建议如下：

| 建议                             | 说明                                     |
| -------------------------------- | ---------------------------------------- |
| 不要只抛出 `RuntimeException`    | 语义不清晰，难以统一处理                 |
| 不要把底层异常信息直接返回给前端 | 可能暴露数据库、文件路径、SQL 等敏感信息 |
| 错误码保持稳定                   | 前端、日志检索、监控告警都可能依赖错误码 |
| 异常信息要可读                   | 日志中应能快速判断失败原因               |
| 保留 cause                       | 包装异常时不要丢失原始异常               |
| 区分用户提示和系统日志           | 用户提示要简洁，系统日志要完整           |

业务异常常见使用方式如下：

```java
throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
```

如果底层发生异常，应保留原始异常：

```java
throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单取消失败", e);
```

## 异常链

异常链用于描述一个异常由另一个异常引起的关系。Java 通过 `cause` 保存原始异常，使上层异常能够保留底层错误现场。

异常链常见于分层项目中。例如 DAO 层发生 `SQLException`，Service 层不希望把数据库异常直接暴露给上层，于是将其包装成 `BusinessException`，同时把原始 `SQLException` 作为 cause 保存下来。

### 异常包装

异常包装是指捕获底层异常后，重新抛出一个更符合当前层语义的新异常。

例如文件读取失败时，底层可能抛出 `IOException`，但业务层可以将它包装成 `BusinessException`，表示“配置加载失败”。

该代码用于演示异常包装：底层 IO 异常被包装为业务异常，同时保留原始 cause。

文件位置：`src/main/java/io/github/atengk/basic/exception/ExceptionWrapDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 异常包装演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionWrapDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            String config = loadConfig("data/app-config.txt");
            log.info("配置内容：{}", config);
        } catch (BusinessException e) {
            log.error("业务处理失败，错误码：{}，错误信息：{}", e.getCode(), e.getMessage(), e);
        }
    }

    /**
     * 加载配置文件
     *
     * @param filePath 文件路径
     * @return 配置内容
     */
    public static String loadConfig(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "配置文件加载失败：" + filePath, e);
        }
    }
}
```

异常包装的重点是“转换语义但不丢失原因”。上层看到的是业务异常，日志中仍然可以通过堆栈找到底层的 `IOException`。

### 原始异常保留

原始异常保留是指在重新抛出异常时，把捕获到的异常作为新异常的 `cause` 传入构造方法。

推荐写法：

```java
try {
    // 调用底层资源
} catch (IOException e) {
    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败", e);
}
```

不推荐写法：

```java
try {
    // 调用底层资源
} catch (IOException e) {
    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败");
}
```

第二种写法丢失了原始异常。日志中只能看到 `BusinessException`，无法直接定位底层到底是文件不存在、权限不足，还是读取过程中断。

原始异常保留的价值包括：

| 价值         | 说明                                     |
| ------------ | ---------------------------------------- |
| 保留完整堆栈 | 可以定位底层异常发生位置                 |
| 方便问题排查 | 能看到真实异常类型和错误信息             |
| 支持异常追踪 | 日志平台可以根据 cause 链路分析问题      |
| 避免误判     | 不会把所有底层问题都误认为同一种业务错误 |

在实际项目中，只要是捕获异常后重新抛出新异常，都应该优先保留原始异常。

### cause 传递

`cause` 是异常对象中的原始原因。通过构造方法传入 cause 后，可以使用 `getCause()` 获取底层异常。

该代码用于演示异常 cause 的传递和读取。

文件位置：`src/main/java/io/github/atengk/basic/exception/ExceptionCauseDemo.java`

```java
package io.github.atengk.basic.exception;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * cause 传递演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionCauseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            service();
        } catch (BusinessException e) {
            log.error("业务异常，错误码：{}，错误信息：{}", e.getCode(), e.getMessage(), e);

            Throwable cause = e.getCause();
            if (cause != null) {
                log.error("原始异常类型：{}，原始异常信息：{}", cause.getClass().getName(), cause.getMessage());
            }
        }
    }

    /**
     * 模拟业务层
     */
    public static void service() {
        try {
            repository();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据读取失败", e);
        }
    }

    /**
     * 模拟数据层
     *
     * @throws IOException 数据读取失败时抛出
     */
    public static void repository() throws IOException {
        throw new IOException("底层文件读取失败");
    }
}
```

异常链打印时，通常会看到 `Caused by`。这表示当前异常是由另一个异常引起的。排查线上问题时，不能只看最外层异常信息，还要继续查看 `Caused by` 后面的原始异常。

## 异常处理实践

异常处理实践关注的是如何在真实项目中写出可维护、可定位、可统一处理的异常逻辑。异常处理不是简单地 `try-catch`，而是要结合业务边界、日志规范、资源释放和异常转换来设计。

### 异常信息设计

异常信息应当清晰、准确、可定位，但不能暴露敏感信息。

较好的异常信息应该包含：

| 内容       | 示例                                     |
| ---------- | ---------------------------------------- |
| 失败动作   | 用户创建失败、订单取消失败、文件导入失败 |
| 关键上下文 | 用户ID、订单号、文件名、业务类型         |
| 明确原因   | 参数为空、状态不允许、数据不存在         |
| 错误码     | `A0001`、`B0002`、`C0001`                |

推荐写法：

```java
throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不允许取消，订单号：" + orderNo);
```

不推荐写法：

```java
throw new RuntimeException("失败");
```

也不推荐直接把底层敏感信息暴露给用户：

```java
throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SQL 执行失败：" + sql);
```

更合适的做法是：对外返回简洁提示，对内日志记录完整上下文。

```java
log.error("订单查询失败，订单号：{}，用户ID：{}", orderNo, userId, e);
throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单查询失败，请稍后重试", e);
```

### 日志记录

异常日志用于问题排查。日志记录的核心原则是：在合适的边界记录一次完整异常，不要层层重复打印。

推荐记录异常日志的场景包括：

| 场景             | 建议                                                  |
| ---------------- | ----------------------------------------------------- |
| 系统入口层       | 例如 Controller、任务入口、消息消费入口，适合统一记录 |
| 外部资源调用失败 | 例如数据库、Redis、MQ、HTTP、文件系统                 |
| 关键业务失败     | 例如支付、下单、扣减库存、权限变更                    |
| 异常被吞掉时     | 如果捕获后不再抛出，必须说明原因                      |
| 重试失败后       | 记录最终失败原因和重试次数                            |

该代码用于演示异常日志记录方式：日志中包含业务上下文，并打印完整异常堆栈。

```java
try {
    cancelOrder(orderNo, userId);
} catch (BusinessException e) {
    log.warn("订单取消失败，订单号：{}，用户ID：{}，错误码：{}，错误信息：{}",
            orderNo, userId, e.getCode(), e.getMessage());
} catch (Exception e) {
    log.error("订单取消出现系统异常，订单号：{}，用户ID：{}", orderNo, userId, e);
}
```

日志记录需要避免以下问题：

| 问题                     | 说明                                         |
| ------------------------ | -------------------------------------------- |
| 只打印 `e.getMessage()`  | 会丢失堆栈，难以定位代码行                   |
| 多层重复打印异常         | 同一个异常被打印多次，干扰排查               |
| 日志没有业务上下文       | 只有异常堆栈，没有订单号、用户ID等关键信息   |
| 把正常业务失败打成 error | 例如参数错误、用户不存在，通常用 warn 更合理 |
| 捕获异常后什么都不做     | 会导致问题被吞掉                             |

一般来说，业务可预期异常使用 `warn`，系统不可预期异常使用 `error`。

### 资源释放

资源释放用于避免文件句柄、网络连接、数据库连接等资源泄漏。Java 中推荐优先使用 `try-with-resources`，让实现了 `AutoCloseable` 的资源自动关闭。

适合释放的资源包括：

| 资源类型   | 示例                                   |
| ---------- | -------------------------------------- |
| 文件资源   | `FileInputStream`、`FileOutputStream`  |
| 网络资源   | `Socket`、HTTP 连接                    |
| 数据库资源 | `Connection`、`Statement`、`ResultSet` |
| IO 包装流  | `BufferedReader`、`BufferedWriter`     |
| 自定义资源 | 实现了 `AutoCloseable` 的对象          |

该代码用于演示资源释放的推荐方式：使用 `try-with-resources` 自动关闭输入流。

```java
try (InputStream inputStream = new FileInputStream("data/user.txt")) {
    String content = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
    log.info("文件读取成功，内容长度：{}", content.length());
} catch (IOException e) {
    log.error("文件读取失败", e);
}
```

如果资源不支持 `try-with-resources`，可以在 `finally` 中手动释放：

```java
InputStream inputStream = null;
try {
    inputStream = new FileInputStream("data/user.txt");
    String content = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
    log.info("文件读取成功，内容长度：{}", content.length());
} catch (IOException e) {
    log.error("文件读取失败", e);
} finally {
    IoUtil.close(inputStream);
}
```

实际开发中，只要资源对象实现了 `AutoCloseable`，应优先使用 `try-with-resources`，减少忘记关闭资源的风险。

### 异常转换

异常转换是指把底层异常转换成更适合当前层表达的异常。

例如：

| 底层异常                | 转换后异常                 | 场景                             |
| ----------------------- | -------------------------- | -------------------------------- |
| `IOException`           | `BusinessException`        | 文件读取失败，转换成业务错误     |
| `SQLException`          | `BusinessException`        | 数据访问失败，转换成系统处理失败 |
| `NumberFormatException` | `IllegalArgumentException` | 参数格式不正确                   |
| 第三方接口异常          | `BusinessException`        | 外部服务调用失败                 |

异常转换常见于分层架构中：

```text
Controller 层：统一处理异常，返回响应结果
Service 层：抛出业务异常，表达业务失败原因
Repository 层：处理数据访问异常，必要时向上抛出
外部接口层：将第三方异常转换为系统内部异常
```

该代码用于演示将底层参数转换异常转换为更明确的业务异常。

文件位置：`src/main/java/io/github/atengk/basic/exception/ExceptionConvertDemo.java`

```java
package io.github.atengk.basic.exception;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常转换演示
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExceptionConvertDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            Integer userId = parseUserId("abc");
            log.info("用户ID转换成功：{}", userId);
        } catch (BusinessException e) {
            log.warn("业务处理失败，错误码：{}，错误信息：{}", e.getCode(), e.getMessage());
        }
    }

    /**
     * 转换用户ID
     *
     * @param userIdText 用户ID文本
     * @return 用户ID
     */
    public static Integer parseUserId(String userIdText) {
        if (StrUtil.isBlank(userIdText)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }

        try {
            if (!NumberUtil.isInteger(userIdText)) {
                throw new NumberFormatException("用户ID不是整数：" + userIdText);
            }

            return Integer.valueOf(userIdText);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID格式不正确", e);
        }
    }
}
```

异常转换需要避免两个问题：一是只转换不保留 cause，导致底层原因丢失；二是所有异常都转换成同一个异常，导致错误语义过于模糊。

## 常见问题

常见问题主要围绕 `throw`、`throws`、`finally`、`return` 和异常捕获范围展开。这些问题也是 Java 面试和实际开发中经常遇到的异常处理细节。

### throw 与 throws 的区别

`throw` 和 `throws` 都与异常抛出有关，但它们的作用完全不同。

| 对比项           | throw                               | throws                   |
| ---------------- | ----------------------------------- | ------------------------ |
| 使用位置         | 方法体内部                          | 方法声明上               |
| 作用             | 主动抛出一个异常对象                | 声明当前方法可能抛出异常 |
| 后面跟的内容     | 异常对象                            | 异常类型                 |
| 数量             | 一次只能抛出一个异常对象            | 可以声明多个异常类型     |
| 是否真正抛出异常 | 是                                  | 否，只是声明             |
| 示例             | `throw new IOException("读取失败")` | `throws IOException`     |

示例：

```java
public static void checkName(String name) {
    if (StrUtil.isBlank(name)) {
        throw new IllegalArgumentException("名称不能为空");
    }
}
```

这里的 `throw` 会真正创建并抛出异常对象。

```java
public static String readFile(String filePath) throws IOException {
    return FileUtil.readUtf8String(filePath);
}
```

这里的 `throws IOException` 只是声明 `readFile` 方法可能抛出 `IOException`，提醒调用方处理。

### finally 一定会执行吗

`finally` 通常会执行，但不是绝对一定执行。

正常情况下，无论 `try` 中是否发生异常，也无论 `catch` 中是否处理异常，`finally` 都会执行。

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    log.warn("计算失败：{}", e.getMessage());
} finally {
    log.info("计算流程结束");
}
```

但是以下场景可能导致 `finally` 不执行：

| 场景                 | 说明                     |
| -------------------- | ------------------------ |
| 调用 `System.exit()` | JVM 直接退出             |
| JVM 崩溃             | 进程异常终止             |
| 机器断电             | 操作系统无法继续调度程序 |
| 进程被强制杀死       | 例如 `kill -9`           |
| 当前线程长期阻塞     | 程序无法执行到 `finally` |

示例：

```java
try {
    System.exit(0);
} finally {
    log.info("这行日志通常不会执行");
}
```

因此，`finally` 适合做常规资源清理，但不能把它理解成任何情况下都必然执行。

### return 与 finally 的执行顺序

当 `try` 或 `catch` 中存在 `return` 时，`finally` 会在方法真正返回之前执行。

示例：

```java
public static int getValue() {
    try {
        return 1;
    } finally {
        log.info("finally 执行");
    }
}
```

这个方法会先准备返回 `1`，然后执行 `finally`，最后返回 `1`。

如果 `finally` 中也写了 `return`，会覆盖 `try` 或 `catch` 中的返回值。

```java
public static int getValue() {
    try {
        return 1;
    } finally {
        return 2;
    }
}
```

这个方法最终返回 `2`。原因是 `finally` 中的 `return` 覆盖了前面已经准备好的返回结果。

更复杂的情况是返回引用对象。如果 `try` 中准备返回一个对象，`finally` 中修改了这个对象的属性，那么最终返回的对象可能会包含修改后的值。

实际开发建议如下：

| 建议                                    | 说明                               |
| --------------------------------------- | ---------------------------------- |
| 不要在 `finally` 中写 `return`          | 会覆盖正常返回值或异常             |
| 不要在 `finally` 中抛出新异常           | 可能覆盖原始异常                   |
| `finally` 只做清理动作                  | 例如释放资源、关闭连接、清理上下文 |
| 复杂资源释放优先用 `try-with-resources` | 更安全、更简洁                     |

### 捕获 Exception 是否合理

捕获 `Exception` 是否合理，要看代码所在层级和处理目的。

在普通业务方法中，不建议随意捕获 `Exception`。因为 `Exception` 范围较大，容易把本该暴露的问题隐藏起来，也可能导致调用方误以为方法执行成功。

不推荐写法：

```java
public static void updateUser() {
    try {
        // 更新用户
    } catch (Exception e) {
        log.error("更新用户失败", e);
    }
}
```

这段代码的问题是：异常被捕获后没有继续抛出，调用方可能不知道更新失败。

更推荐的写法：

```java
public static void updateUser() {
    try {
        // 更新用户
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户失败", e);
    }
}
```

在系统入口边界，捕获 `Exception` 是合理的。例如 Web 全局异常处理器、定时任务入口、消息消费入口、命令行程序入口等。这些地方需要兜底处理异常，避免异常直接暴露给用户或导致任务静默失败。

合理场景包括：

| 场景                    | 是否合理 | 说明                       |
| ----------------------- | -------- | -------------------------- |
| Controller 全局异常处理 | 合理     | 统一响应错误结果           |
| 定时任务入口            | 合理     | 避免任务异常后无日志       |
| MQ 消费入口             | 合理     | 记录失败消息并决定是否重试 |
| 普通业务方法            | 不建议   | 应优先捕获明确异常         |
| 工具方法                | 不建议   | 通常应抛出给调用方处理     |
| 大范围吞掉异常          | 不合理   | 会掩盖真实问题             |

总结来说，捕获 `Exception` 可以作为系统边界的兜底策略，但不应该成为普通业务代码中的默认写法。业务代码应优先捕获明确异常；如果确实需要捕获 `Exception`，通常应记录日志后重新抛出或转换为业务异常。
