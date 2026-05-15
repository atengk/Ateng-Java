# 注解与反射

## 注解概述

注解是 Java 提供的一种元数据机制，用于给类、方法、字段、参数、构造方法、包等程序元素添加说明信息。注解本身通常不直接改变程序逻辑，它的价值主要体现在被编译器、开发工具、框架或运行期反射机制读取并处理。

注解可以理解为“写在代码上的标记”。例如 `@Override` 用于告诉编译器当前方法是重写父类方法，`@Deprecated` 用于标记某个 API 已过时，Spring 中的 `@Controller`、`@Service`、`@Autowired` 则用于让框架识别组件、注入依赖或处理请求。

### 注解的作用

注解的核心作用是为代码提供额外的描述信息，方便编译器检查、工具生成代码、框架读取配置或运行时进行动态处理。

常见作用可以分为以下几类：

| 作用       | 说明                                        | 示例                                         |
| ---------- | ------------------------------------------- | -------------------------------------------- |
| 编译检查   | 让编译器根据注解进行语法或语义检查          | `@Override`、`@FunctionalInterface`          |
| 标记说明   | 标识代码状态或用途，提升可读性              | `@Deprecated`                                |
| 抑制警告   | 告诉编译器忽略特定类型的警告                | `@SuppressWarnings`                          |
| 框架配置   | 替代 XML 或配置文件，让框架读取注解完成配置 | `@Controller`、`@Service`、`@RequestMapping` |
| 代码生成   | 编译期工具根据注解生成辅助代码              | Lombok 的 `@Getter`、`@Setter`               |
| 运行期解析 | 程序运行时通过反射读取注解并执行逻辑        | 自定义校验注解、权限注解、日志注解           |

注解本身只是元数据，真正产生效果的是“读取并处理注解的程序”。例如 `@Override` 是由编译器处理，Spring 注解是由 Spring 框架处理，自定义注解通常需要通过反射或 AOP 主动解析。

下面的代码演示了编译器如何通过 `@Override` 帮助检查方法重写是否正确。

```java
package io.github.atengk.basic.annotation;

/**
 * 注解基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OverrideDemo {

    @Override
    public String toString() {
        return "OverrideDemo{}";
    }
}
```

如果方法名写错，例如写成 `tostring()`，编译器会报错，因为该方法并没有真正重写父类方法。这就是注解参与编译检查的典型场景。

### 注解的使用场景

注解在 Java 开发中使用非常广泛，尤其是在 Spring Boot、MyBatis、JUnit、Lombok、Swagger、Sa-Token 等框架中，大量配置都通过注解完成。

常见使用场景如下：

| 使用场景        | 说明                                | 常见注解                                            |
| --------------- | ----------------------------------- | --------------------------------------------------- |
| 编译期检查      | 提前发现代码问题                    | `@Override`、`@FunctionalInterface`                 |
| API 过时提示    | 标记不推荐继续使用的方法或类        | `@Deprecated`                                       |
| 单元测试        | 标记测试方法、初始化方法、销毁方法  | `@Test`、`@BeforeEach`、`@AfterEach`                |
| Spring 组件扫描 | 标识类交给 Spring 容器管理          | `@Component`、`@Service`、`@Repository`             |
| Web 接口开发    | 声明 Controller、接口路径、请求参数 | `@RestController`、`@RequestMapping`、`@GetMapping` |
| 依赖注入        | 让框架自动装配对象                  | `@Autowired`、`@Resource`                           |
| 数据校验        | 对字段或参数进行约束校验            | `@NotNull`、`@NotBlank`、`@Size`                    |
| ORM 映射        | 将类和数据库表、字段建立映射关系    | `@TableName`、`@TableId`、`@TableField`             |
| 接口文档        | 生成接口说明文档                    | `@Operation`、`@Schema`                             |
| 权限控制        | 标记接口所需权限                    | `@PreAuthorize`、`@SaCheckPermission`               |

在实际开发中，注解通常配合框架使用，用于减少配置文件、提高代码可读性，并让业务代码和配置信息保持在相对接近的位置。

下面的代码展示了 Spring Boot 中常见的注解使用方式，用于声明一个简单的用户接口。

```java
package io.github.atengk.basic.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
public class UserController {

    /**
     * 查询用户名称
     *
     * @return 用户名称
     */
    @GetMapping("/user/name")
    public String getUserName() {
        log.info("查询用户名称");
        return "Ateng";
    }
}
```

在这个示例中，`@RestController` 表示当前类是一个 REST 接口类，`@GetMapping` 表示当前方法处理 HTTP GET 请求，`@Slf4j` 用于生成日志对象。程序运行时，Spring Boot 会扫描这些注解，并根据注解信息注册接口。

### Java 内置注解

Java 内置注解是 JDK 自带的注解，不需要额外引入第三方依赖。它们主要用于编译检查、警告控制、过时标记、函数式接口约束和泛型安全提示。

常见 Java 内置注解如下：

| 注解                   | 作用                                       | 常用位置                                              |
| ---------------------- | ------------------------------------------ | ----------------------------------------------------- |
| `@Override`            | 标记方法重写父类或接口方法                 | 方法                                                  |
| `@Deprecated`          | 标记类、方法、字段等已过时                 | 类、方法、字段、构造方法                              |
| `@SuppressWarnings`    | 抑制编译器警告                             | 类、方法、字段、局部变量                              |
| `@SafeVarargs`         | 抑制泛型可变参数的堆污染警告               | 构造方法、`final` 方法、`static` 方法、`private` 方法 |
| `@FunctionalInterface` | 标记函数式接口，要求接口只能有一个抽象方法 | 接口                                                  |

`@Override` 用于明确声明当前方法是重写方法。它可以提高代码可读性，并让编译器检查方法签名是否正确。

```java
package io.github.atengk.basic.annotation;

/**
 * Override 注解示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OverrideExample {

    @Override
    public String toString() {
        return "OverrideExample{}";
    }
}
```

`@Deprecated` 用于标记某个 API 已经过时，不推荐继续使用。被标记的类、方法或字段仍然可以调用，但编译器会给出警告。

```java
package io.github.atengk.basic.annotation;

/**
 * Deprecated 注解示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DeprecatedExample {

    /**
     * 旧版登录方法，不推荐继续使用
     *
     * @param username 用户名
     * @return 登录结果
     */
    @Deprecated
    public boolean login(String username) {
        return username != null && !username.isBlank();
    }

    /**
     * 新版登录方法
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    public boolean login(String username, String password) {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}
```

`@SuppressWarnings` 用于抑制指定类型的编译器警告。它不建议滥用，只有在明确知道警告原因且风险可控时才使用。

```java
package io.github.atengk.basic.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * SuppressWarnings 注解示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SuppressWarningsExample {

    /**
     * 演示抑制原始类型警告
     *
     * @return 集合数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> buildList() {
        List list = new ArrayList();
        list.add("Java");
        list.add("Annotation");
        return list;
    }
}
```

`@FunctionalInterface` 用于标记函数式接口。函数式接口只能有一个抽象方法，常用于 Lambda 表达式和方法引用。

```java
package io.github.atengk.basic.annotation;

/**
 * 函数式接口示例
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

使用函数式接口时，可以直接通过 Lambda 表达式传入具体逻辑。

```java
package io.github.atengk.basic.annotation;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 函数式接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class MessageHandlerDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        MessageHandler handler = message -> {
            if (StrUtil.isBlank(message)) {
                log.warn("消息内容为空，跳过处理");
                return;
            }
            log.info("处理消息：{}", message);
        };

        handler.handle("Java 注解基础");
    }
}
```

`@SafeVarargs` 用于修饰使用泛型可变参数的方法，表示该方法不会对泛型参数执行不安全操作。它通常用于工具方法中。

```java
package io.github.atengk.basic.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SafeVarargs 注解示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SafeVarargsExample {

    /**
     * 合并多个集合
     *
     * @param lists 多个集合
     * @param <T>   元素类型
     * @return 合并后的集合
     */
    @SafeVarargs
    public static <T> List<T> merge(List<T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<T> list : lists) {
            if (list != null) {
                result.addAll(list);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
```

内置注解的重点不在于“写法复杂”，而在于理解谁会处理它。`@Override`、`@SuppressWarnings`、`@FunctionalInterface` 主要由编译器处理；`@Deprecated` 既能被编译器识别，也能被 IDE、文档工具和开发人员识别；而后续学习自定义注解时，需要重点关注注解的保留策略和解析方式。



## 自定义注解

自定义注解用于根据业务或框架需求定义自己的代码标记。它本身只负责声明元数据，真正的处理逻辑需要由编译器插件、注解处理器、框架、AOP 或反射代码完成。

### 注解定义

自定义注解使用 `@interface` 关键字定义。它的语法类似接口，但不是普通接口，而是一种专门用于声明注解类型的语法结构。

下面定义一个用于描述接口字段信息的注解，可以标记在字段上。

文件位置：`src/main/java/io/github/atengk/basic/annotation/FieldInfo.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 字段说明注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Target(ElementType.FIELD)
public @interface FieldInfo {

    /**
     * 字段名称
     *
     * @return 字段名称
     */
    String name();

    /**
     * 字段描述
     *
     * @return 字段描述
     */
    String description() default "";
}
```

这个注解可以使用在实体类字段上。

文件位置：`src/main/java/io/github/atengk/basic/annotation/UserInfo.java`

```java
package io.github.atengk.basic.annotation;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserInfo {

    @FieldInfo(name = "用户名", description = "系统登录账号")
    private String username;

    @FieldInfo(name = "年龄", description = "用户年龄")
    private Integer age;
}
```

需要注意的是，`@interface` 定义的是注解类型，注解中的方法称为“注解属性”。使用注解时，属性名和属性值以 `key = value` 的形式传入。

### 元注解

元注解是用于修饰注解的注解。简单说，自定义注解本身也可以被注解修饰，这些修饰自定义注解的注解就叫元注解。

例如前面定义的 `@FieldInfo` 中使用了两个元注解：

```java
@Documented
@Target(ElementType.FIELD)
public @interface FieldInfo {
}
```

其中，`@Target(ElementType.FIELD)` 表示 `@FieldInfo` 只能用于字段上；`@Documented` 表示生成 Javadoc 文档时可以包含这个注解信息。

如果一个自定义注解没有使用 `@Target`，理论上它可以被使用在多种程序元素上，但这种写法不够严谨。实际开发中建议明确指定注解的使用范围。

下面定义一个可用于类和方法的日志标记注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/OperationLog.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OperationLog {

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String value();
}
```

使用方式如下：

```java
package io.github.atengk.basic.annotation;

/**
 * 用户服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@OperationLog("用户模块")
public class UserService {

    /**
     * 新增用户
     */
    @OperationLog("新增用户")
    public void createUser() {
        // 业务逻辑
    }
}
```

### 注解属性

注解属性本质上是注解中定义的无参方法。注解属性不能带参数，不能抛出异常，也不能定义普通业务方法。

注解属性支持的类型是有限的，常见类型如下：

| 属性类型       | 示例                                      |
| -------------- | ----------------------------------------- |
| 基本数据类型   | `int`、`long`、`boolean`                  |
| `String`       | `String name()`                           |
| `Class`        | `Class<?> type()`                         |
| 枚举           | `LogType type()`                          |
| 注解           | `FieldInfo field()`                       |
| 以上类型的数组 | `String[] roles()`、`Class<?>[] groups()` |

下面定义一个带有多种属性类型的权限注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/PermissionType.java`

```java
package io.github.atengk.basic.annotation;

/**
 * 权限类型
 *
 * @author Ateng
 * @since 2026-05-15
 */
public enum PermissionType {

    /**
     * 菜单权限
     */
    MENU,

    /**
     * 按钮权限
     */
    BUTTON,

    /**
     * 接口权限
     */
    API
}
```

文件位置：`src/main/java/io/github/atengk/basic/annotation/RequirePermission.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Target(ElementType.METHOD)
public @interface RequirePermission {

    /**
     * 权限编码
     *
     * @return 权限编码
     */
    String value();

    /**
     * 权限类型
     *
     * @return 权限类型
     */
    PermissionType type();

    /**
     * 是否必须登录
     *
     * @return true 表示必须登录
     */
    boolean loginRequired();

    /**
     * 允许的角色
     *
     * @return 角色编码数组
     */
    String[] roles();

    /**
     * 目标处理类
     *
     * @return 处理类类型
     */
    Class<?> handler();
}
```

使用该注解时，需要按照属性定义传入对应类型的值。

```java
package io.github.atengk.basic.annotation;

/**
 * 权限接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PermissionController {

    /**
     * 删除用户
     */
    @RequirePermission(
            value = "user:delete",
            type = PermissionType.API,
            loginRequired = true,
            roles = {"admin", "manager"},
            handler = PermissionController.class
    )
    public void deleteUser() {
        // 删除用户逻辑
    }
}
```

注解属性的命名一般使用简洁语义。例如只有一个核心属性时，通常命名为 `value`，这样使用注解时可以省略属性名。

```java
@OperationLog("新增用户")
public void createUser() {
}
```

等价于：

```java
@OperationLog(value = "新增用户")
public void createUser() {
}
```

### 默认值

注解属性可以使用 `default` 设置默认值。设置默认值后，使用注解时可以不显式传入该属性。

下面定义一个接口限流注解，其中 `limit`、`timeWindowSeconds`、`message` 都设置了默认值。

文件位置：`src/main/java/io/github/atengk/basic/annotation/RateLimit.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Target(ElementType.METHOD)
public @interface RateLimit {

    /**
     * 限流次数
     *
     * @return 限流次数
     */
    int limit() default 10;

    /**
     * 时间窗口，单位：秒
     *
     * @return 时间窗口秒数
     */
    int timeWindowSeconds() default 60;

    /**
     * 限流提示信息
     *
     * @return 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
```

使用默认值时，可以只写注解名。

```java
@RateLimit
public void queryUser() {
}
```

也可以覆盖部分默认值。

```java
@RateLimit(limit = 5, timeWindowSeconds = 30)
public void createOrder() {
}
```

默认值适合用于大多数场景相同、少数场景需要覆盖的属性。这样可以减少注解使用时的重复配置。

## 元注解

元注解用于控制注解的使用范围、生命周期、文档生成、继承行为和重复标注能力。Java 中常见的元注解包括 `@Target`、`@Retention`、`@Documented`、`@Inherited` 和 `@Repeatable`。

### @Target

`@Target` 用于指定注解可以标记在哪些程序元素上。如果使用位置不符合限制，编译器会直接报错。

常见 `ElementType` 如下：

| ElementType       | 说明                 |
| ----------------- | -------------------- |
| `TYPE`            | 类、接口、枚举、注解 |
| `FIELD`           | 字段                 |
| `METHOD`          | 方法                 |
| `PARAMETER`       | 方法参数             |
| `CONSTRUCTOR`     | 构造方法             |
| `LOCAL_VARIABLE`  | 局部变量             |
| `PACKAGE`         | 包                   |
| `ANNOTATION_TYPE` | 注解类型             |
| `TYPE_PARAMETER`  | 类型参数             |
| `TYPE_USE`        | 类型使用位置         |

下面的注解只能用于方法。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 方法标记注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.METHOD)
public @interface MethodMarker {
}
```

下面的注解可以同时用于类和方法。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 模块标记注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ModuleMarker {
}
```

### @Retention

`@Retention` 用于指定注解的保留策略，也就是注解信息保留到哪个阶段。

常见保留策略如下：

| RetentionPolicy | 说明                                          | 是否能通过反射读取 |
| --------------- | --------------------------------------------- | ------------------ |
| `SOURCE`        | 只保留在源代码中，编译后丢弃                  | 否                 |
| `CLASS`         | 保留到 `.class` 文件中，运行时 JVM 默认不读取 | 否                 |
| `RUNTIME`       | 保留到运行期，可以通过反射读取                | 是                 |

如果自定义注解需要在运行时通过反射解析，必须使用 `@Retention(RetentionPolicy.RUNTIME)`。

下面的注解可以在运行期读取。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 运行期标记注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeMarker {
}
```

下面的注解只在源码阶段有效，适合编译器或源码工具使用。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 源码期标记注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Retention(RetentionPolicy.SOURCE)
public @interface SourceMarker {
}
```

实际开发中，如果注解要配合 Spring AOP、反射工具、运行期权限校验、字段校验等功能使用，通常选择 `RetentionPolicy.RUNTIME`。

### @Documented

`@Documented` 表示该注解会被 Javadoc 工具记录到 API 文档中。它不影响程序运行，只影响文档生成结果。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * API 文档标记注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApiDocMarker {

    /**
     * 文档说明
     *
     * @return 文档说明
     */
    String value();
}
```

使用该注解后，生成 Javadoc 时，注解信息会出现在对应类或方法的文档中。

```java
@ApiDocMarker("用户查询接口")
public String queryUser() {
    return "Ateng";
}
```

`@Documented` 适合用于公开 API、框架注解、SDK 注解等需要暴露给调用方阅读的场景。

### @Inherited

`@Inherited` 表示某个注解可以被子类继承。它只对类级别注解有效，对方法、字段、接口实现等场景无效。

下面定义一个类级别的业务模块注解。

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务模块注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BusinessModule {

    /**
     * 模块名称
     *
     * @return 模块名称
     */
    String value();
}
```

父类使用注解。

```java
package io.github.atengk.basic.annotation;

/**
 * 基础服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@BusinessModule("用户模块")
public class BaseUserService {
}
```

子类没有显式标注注解，但可以通过 `Class#getAnnotation` 获取继承来的注解。

```java
package io.github.atengk.basic.annotation;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserServiceImpl extends BaseUserService {
}
```

需要注意，`@Inherited` 不会让接口上的注解被实现类继承，也不会让父类方法上的注解被子类重写方法继承。

### @Repeatable

`@Repeatable` 表示一个注解可以在同一个位置重复使用。使用它时，需要额外定义一个“容器注解”。

下面定义一个角色注解和对应的容器注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/Role.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Repeatable(Roles.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {

    /**
     * 角色编码
     *
     * @return 角色编码
     */
    String value();
}
```

文件位置：`src/main/java/io/github/atengk/basic/annotation/Roles.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色容器注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Roles {

    /**
     * 角色注解数组
     *
     * @return 角色注解数组
     */
    Role[] value();
}
```

使用时可以在同一个方法上多次标记 `@Role`。

```java
package io.github.atengk.basic.annotation;

/**
 * 用户权限服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserPermissionService {

    /**
     * 导出用户数据
     */
    @Role("admin")
    @Role("manager")
    public void exportUser() {
        // 导出用户数据
    }
}
```

读取重复注解时，通常使用 `getAnnotationsByType(Role.class)`，这样可以直接获取多个 `@Role` 注解。

## 注解解析

注解定义完成后，必须被解析才有实际意义。根据解析时机不同，注解解析可以分为编译期注解解析和运行期注解解析。

### 编译期注解

编译期注解一般通过注解处理器完成，常见技术是 JDK 提供的 APT，也就是 Annotation Processing Tool。它可以在编译阶段扫描注解，并生成代码、校验代码或输出编译提示。

常见编译期注解场景包括：

| 场景     | 说明                           | 示例              |
| -------- | ------------------------------ | ----------------- |
| 生成代码 | 根据注解生成 Java 源码         | Lombok、MapStruct |
| 生成配置 | 根据注解生成配置文件或索引文件 | Spring 配置元数据 |
| 编译校验 | 编译时检查注解使用是否符合规则 | 自定义规范检查    |
| 文档生成 | 根据注解生成接口或字段说明     | 部分 API 文档工具 |

编译期注解一般使用 `RetentionPolicy.SOURCE` 或 `RetentionPolicy.CLASS`。它们不依赖程序运行时的反射，适合做代码生成和编译检查。

下面是一个简单的源码期注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/GenerateDescription.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 生成描述信息注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateDescription {

    /**
     * 描述内容
     *
     * @return 描述内容
     */
    String value();
}
```

使用方式如下：

```java
package io.github.atengk.basic.annotation;

/**
 * 订单信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@GenerateDescription("订单基础信息")
public class OrderInfo {
}
```

编译期注解的处理逻辑不在注解本身，而是在注解处理器中。实际项目中，如果要完整实现编译期代码生成，需要继承 `AbstractProcessor`，并在编译阶段注册处理器。

下面是一个简化版注解处理器示例，用于在编译期扫描 `@GenerateDescription`。

文件位置：`src/main/java/io/github/atengk/basic/processor/GenerateDescriptionProcessor.java`

```java
package io.github.atengk.basic.processor;

import io.github.atengk.basic.annotation.GenerateDescription;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * 描述信息注解处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@SupportedAnnotationTypes("io.github.atengk.basic.annotation.GenerateDescription")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class GenerateDescriptionProcessor extends AbstractProcessor {

    /**
     * 处理注解
     *
     * @param annotations 当前处理器支持的注解类型
     * @param roundEnv    当前轮次的编译环境
     * @return true 表示当前注解已由该处理器处理
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(GenerateDescription.class)
                .forEach(element -> processingEnv.getMessager()
                        .printMessage(javax.tools.Diagnostic.Kind.NOTE, "发现描述注解：" + element.getSimpleName()));
        return true;
    }
}
```

编译期注解处理器适合框架和工具开发。普通业务开发中更常见的是使用成熟框架提供的注解，例如 Lombok、MapStruct、Spring、MyBatis-Plus 等。

### 运行期注解

运行期注解指程序运行时仍然存在，并且可以通过反射读取的注解。要实现运行期解析，注解必须使用 `@Retention(RetentionPolicy.RUNTIME)`。

运行期注解常用于以下场景：

| 场景     | 说明                               |
| -------- | ---------------------------------- |
| 字段校验 | 根据字段注解判断值是否合法         |
| 权限控制 | 根据方法注解判断当前用户是否有权限 |
| 日志记录 | 根据方法注解记录操作日志           |
| 对象映射 | 根据字段注解完成字段转换           |
| ORM 框架 | 根据类和字段注解建立表映射关系     |
| Web 框架 | 根据类和方法注解注册接口映射       |

下面定义一个简单的字段必填校验注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/RequiredField.java`

```java
package io.github.atengk.basic.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 必填字段注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequiredField {

    /**
     * 校验失败提示
     *
     * @return 提示信息
     */
    String message() default "字段不能为空";
}
```

实体类中使用该注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/UserRegisterRequest.java`

```java
package io.github.atengk.basic.annotation;

/**
 * 用户注册请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserRegisterRequest {

    @RequiredField(message = "用户名不能为空")
    private String username;

    @RequiredField(message = "手机号不能为空")
    private String phone;

    public UserRegisterRequest(String username, String phone) {
        this.username = username;
        this.phone = phone;
    }
}
```

运行期解析时，可以通过反射获取字段、读取注解，并校验字段值。

文件位置：`src/main/java/io/github/atengk/basic/annotation/RequiredFieldValidator.java`

```java
package io.github.atengk.basic.annotation;

import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 必填字段校验器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RequiredFieldValidator {

    /**
     * 校验对象中的必填字段
     *
     * @param target 待校验对象
     * @return 错误信息列表
     */
    public static List<String> validate(Object target) {
        List<String> errors = new ArrayList<>();
        if (target == null) {
            errors.add("校验对象不能为空");
            return errors;
        }

        Class<?> targetClass = target.getClass();
        Field[] fields = targetClass.getDeclaredFields();

        for (Field field : fields) {
            RequiredField requiredField = field.getAnnotation(RequiredField.class);
            if (requiredField == null) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value == null || value instanceof CharSequence && StrUtil.isBlank(value.toString())) {
                    errors.add(requiredField.message());
                }
            } catch (IllegalAccessException e) {
                errors.add("字段访问失败：" + field.getName());
            }
        }

        return errors;
    }
}
```

测试运行代码如下。

文件位置：`src/main/java/io/github/atengk/basic/annotation/RequiredFieldDemo.java`

```java
package io.github.atengk.basic.annotation;

import java.util.List;

/**
 * 必填字段校验示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RequiredFieldDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        UserRegisterRequest request = new UserRegisterRequest("", "13800138000");
        List<String> errors = RequiredFieldValidator.validate(request);
        System.out.println(errors);
    }
}
```

运行结果如下：

```text
[用户名不能为空]
```

这个示例体现了运行期注解的基本流程：定义运行期注解、在目标类上使用注解、通过反射读取注解、根据注解属性执行对应逻辑。

### 反射读取注解

反射读取注解主要依赖 `Class`、`Field`、`Method`、`Constructor` 等反射对象。只要注解的保留策略是 `RUNTIME`，就可以在运行时读取。

常用方法如下：

| 方法                           | 说明                             |
| ------------------------------ | -------------------------------- |
| `isAnnotationPresent(Class)`   | 判断是否存在指定注解             |
| `getAnnotation(Class)`         | 获取指定注解，不存在返回 `null`  |
| `getAnnotations()`             | 获取当前元素上的所有 public 注解 |
| `getDeclaredAnnotations()`     | 获取当前元素上直接声明的所有注解 |
| `getAnnotationsByType(Class)`  | 获取可重复注解                   |
| `getDeclaredAnnotation(Class)` | 获取当前元素直接声明的指定注解   |

下面演示读取类、字段、方法上的注解。

文件位置：`src/main/java/io/github/atengk/basic/annotation/AnnotationReadDemo.java`

```java
package io.github.atengk.basic.annotation;

import cn.hutool.core.util.ArrayUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 注解读取示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@BusinessModule("注解示例模块")
public class AnnotationReadDemo {

    @FieldInfo(name = "用户名", description = "登录账号")
    private String username;

    /**
     * 查询用户
     */
    @OperationLog("查询用户")
    @Role("admin")
    @Role("manager")
    public void queryUser() {
        // 查询用户
    }

    /**
     * 程序入口
     *
     * @param args 启动参数
     * @throws NoSuchMethodException 方法不存在时抛出异常
     * @throws NoSuchFieldException  字段不存在时抛出异常
     */
    public static void main(String[] args) throws NoSuchMethodException, NoSuchFieldException {
        readClassAnnotation();
        readFieldAnnotation();
        readMethodAnnotation();
        readRepeatableAnnotation();
    }

    /**
     * 读取类注解
     */
    private static void readClassAnnotation() {
        Class<AnnotationReadDemo> clazz = AnnotationReadDemo.class;
        BusinessModule businessModule = clazz.getAnnotation(BusinessModule.class);
        if (businessModule != null) {
            System.out.println("类注解：" + businessModule.value());
        }
    }

    /**
     * 读取字段注解
     *
     * @throws NoSuchFieldException 字段不存在时抛出异常
     */
    private static void readFieldAnnotation() throws NoSuchFieldException {
        Field field = AnnotationReadDemo.class.getDeclaredField("username");
        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (fieldInfo != null) {
            System.out.println("字段名称：" + fieldInfo.name());
            System.out.println("字段描述：" + fieldInfo.description());
        }
    }

    /**
     * 读取方法注解
     *
     * @throws NoSuchMethodException 方法不存在时抛出异常
     */
    private static void readMethodAnnotation() throws NoSuchMethodException {
        Method method = AnnotationReadDemo.class.getDeclaredMethod("queryUser");
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog != null) {
            System.out.println("方法注解：" + operationLog.value());
        }
    }

    /**
     * 读取重复注解
     *
     * @throws NoSuchMethodException 方法不存在时抛出异常
     */
    private static void readRepeatableAnnotation() throws NoSuchMethodException {
        Method method = AnnotationReadDemo.class.getDeclaredMethod("queryUser");
        Role[] roles = method.getAnnotationsByType(Role.class);
        if (ArrayUtil.isNotEmpty(roles)) {
            for (Role role : roles) {
                System.out.println("角色注解：" + role.value());
            }
        }
    }
}
```

运行结果如下：

```text
类注解：注解示例模块
字段名称：用户名
字段描述：登录账号
方法注解：查询用户
角色注解：admin
角色注解：manager
```

反射读取注解时需要注意以下几点：

| 注意点                                  | 说明                                                       |
| --------------------------------------- | ---------------------------------------------------------- |
| 注解必须是 `RUNTIME`                    | 否则运行期无法通过反射读取                                 |
| `getAnnotation` 可能返回 `null`         | 使用前需要判空                                             |
| 读取私有字段需要 `setAccessible(true)`  | 否则可能无法读取字段值                                     |
| `@Inherited` 只影响类注解               | 不影响方法和字段注解                                       |
| 可重复注解建议用 `getAnnotationsByType` | 比直接读取容器注解更方便                                   |
| 反射有性能成本                          | 高频场景建议缓存 `Class`、`Field`、`Method` 和注解解析结果 |

注解解析的关键不是注解本身，而是解析逻辑。注解只提供“标记”和“参数”，具体行为必须由处理器、框架、反射代码或 AOP 逻辑完成。



## 反射概述

反射是 Java 在运行期间动态获取类信息、创建对象、访问字段、调用方法的一套机制。普通代码通常在编译期就确定了要调用哪个类、哪个方法，而反射可以在运行时根据类名、方法名、字段名等字符串信息动态完成操作。

反射的核心入口是 `Class` 对象。通过 `Class` 对象可以进一步获取字段、方法、构造方法、注解、父类、接口等信息，也可以配合 `Constructor`、`Field`、`Method` 完成对象创建和成员访问。

### 反射的作用

反射的主要作用是让程序具备运行时动态处理能力。它可以在不知道具体类型的情况下，获取类的结构信息并执行对应操作。

常见作用如下：

| 作用         | 说明                                             | 常见场景                     |
| ------------ | ------------------------------------------------ | ---------------------------- |
| 获取类结构   | 获取类名、包名、父类、接口、字段、方法、构造方法 | 框架扫描、工具类分析         |
| 创建对象     | 通过类名或构造方法动态创建实例                   | IOC 容器、插件加载           |
| 访问字段     | 动态读取或修改对象字段值                         | ORM 映射、对象复制、字段校验 |
| 调用方法     | 根据方法名动态执行方法                           | RPC 调用、事件分发、测试工具 |
| 读取注解     | 获取类、字段、方法上的注解信息                   | Spring、权限校验、参数校验   |
| 访问私有成员 | 绕过访问修饰符限制访问私有字段或方法             | 框架底层、调试工具           |

反射使代码更灵活，但也会降低可读性，并带来一定性能成本。因此普通业务代码不建议频繁使用反射，更多用于框架、通用工具、基础组件和底层封装。

下面定义一个用户实体，后续反射示例都会基于这个类展开。

文件位置：`src/main/java/io/github/atengk/basic/reflect/UserInfo.java`

```java
package io.github.atengk.basic.reflect;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserInfo {

    private Long id;

    private String username;

    private Integer age;

    public UserInfo() {
    }

    public UserInfo(Long id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }

    private UserInfo(String username) {
        this.username = username;
    }

    public String getUserSummary() {
        return id + "-" + username + "-" + age;
    }

    public String getUserSummary(String prefix) {
        return prefix + ":" + id + "-" + username + "-" + age;
    }

    private String buildSecretInfo(String key) {
        return key + ":" + username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Integer getAge() {
        return age;
    }
}
```

### Class 对象

`Class` 对象是 Java 反射机制的核心。每个被 JVM 加载的类，在内存中都会对应一个 `Class` 对象，用于描述这个类的结构信息。

常见获取 `Class` 对象的方式有三种：

| 获取方式           | 示例                        | 说明                   |
| ------------------ | --------------------------- | ---------------------- |
| 类名 `.class`      | `UserInfo.class`            | 编译期已知类型时使用   |
| 对象 `.getClass()` | `userInfo.getClass()`       | 已有对象实例时使用     |
| `Class.forName()`  | `Class.forName("完整类名")` | 运行时根据类名动态加载 |

下面的示例演示三种方式获取 `Class` 对象。

文件位置：`src/main/java/io/github/atengk/basic/reflect/ClassObjectDemo.java`

```java
package io.github.atengk.basic.reflect;

/**
 * Class 对象示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ClassObjectDemo {

    public static void main(String[] args) throws ClassNotFoundException {
        Class<UserInfo> clazz1 = UserInfo.class;

        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);
        Class<?> clazz2 = userInfo.getClass();

        Class<?> clazz3 = Class.forName("io.github.atengk.basic.reflect.UserInfo");

        System.out.println(clazz1 == clazz2);
        System.out.println(clazz2 == clazz3);
        System.out.println(clazz1.getName());
        System.out.println(clazz1.getSimpleName());
    }
}
```

运行结果如下：

```text
true
true
io.github.atengk.basic.reflect.UserInfo
UserInfo
```

同一个类在同一个类加载器下只会有一个 `Class` 对象。因此上面三种方式获取到的 `Class` 对象是同一个对象。

### 反射的使用场景

反射常用于需要“通用化”和“动态化”的场景。业务代码中直接使用反射的频率不高，但很多框架底层都大量依赖反射。

常见使用场景如下：

| 使用场景               | 说明                                   |
| ---------------------- | -------------------------------------- |
| Spring IOC             | 扫描类、读取注解、创建 Bean、注入字段  |
| Spring MVC             | 根据请求路径找到 Controller 方法并调用 |
| MyBatis / MyBatis-Plus | 根据实体类字段映射数据库字段           |
| JSON 序列化            | 读取对象字段并转换为 JSON              |
| 参数校验               | 扫描字段注解并校验字段值               |
| 对象复制               | 动态读取源对象字段并写入目标对象       |
| 单元测试               | 调用私有方法或设置私有字段             |
| 插件化开发             | 根据类名动态加载实现类                 |

下面是一个简单的“根据类名创建对象”的场景。它模拟框架根据配置动态加载类。

文件位置：`src/main/java/io/github/atengk/basic/reflect/DynamicCreateDemo.java`

```java
package io.github.atengk.basic.reflect;

/**
 * 动态创建对象示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DynamicCreateDemo {

    public static void main(String[] args) throws Exception {
        String className = "io.github.atengk.basic.reflect.UserInfo";

        Class<?> clazz = Class.forName(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();

        System.out.println(instance.getClass().getName());
    }
}
```

这类写法在框架中很常见。例如框架读取配置中的类名后，并不需要在代码中直接 `new` 某个具体类，而是可以通过反射动态创建实例。

## 反射操作

反射操作主要包括获取类信息、字段信息、方法信息和构造方法信息。这些信息分别由 `Class`、`Field`、`Method`、`Constructor` 表示。

### 获取类信息

通过 `Class` 对象可以获取类的基本信息，例如完整类名、简单类名、包名、父类、接口、修饰符等。

下面的代码演示如何读取类的基础结构信息。

文件位置：`src/main/java/io/github/atengk/basic/reflect/ClassInfoDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 类信息读取示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ClassInfoDemo {

    public static void main(String[] args) {
        Class<UserInfo> clazz = UserInfo.class;

        System.out.println("完整类名：" + clazz.getName());
        System.out.println("简单类名：" + clazz.getSimpleName());
        System.out.println("包名：" + clazz.getPackageName());
        System.out.println("父类：" + clazz.getSuperclass().getName());
        System.out.println("修饰符：" + Modifier.toString(clazz.getModifiers()));
        System.out.println("是否接口：" + clazz.isInterface());
        System.out.println("是否枚举：" + clazz.isEnum());
        System.out.println("是否注解：" + clazz.isAnnotation());

        Arrays.stream(clazz.getInterfaces())
                .forEach(interfaceClass -> System.out.println("实现接口：" + interfaceClass.getName()));
    }
}
```

常用类信息方法如下：

| 方法               | 说明                   |
| ------------------ | ---------------------- |
| `getName()`        | 获取完整类名，包含包名 |
| `getSimpleName()`  | 获取简单类名           |
| `getPackageName()` | 获取包名               |
| `getSuperclass()`  | 获取父类               |
| `getInterfaces()`  | 获取实现的接口         |
| `getModifiers()`   | 获取修饰符编码         |
| `isInterface()`    | 判断是否为接口         |
| `isEnum()`         | 判断是否为枚举         |
| `isAnnotation()`   | 判断是否为注解         |

`getModifiers()` 返回的是整数编码，通常需要配合 `Modifier.toString()` 转换为可读的修饰符字符串。

### 获取字段信息

字段信息由 `Field` 表示。通过 `Class` 对象可以获取当前类或父类中的字段。

常用方法如下：

| 方法                            | 说明                                        |
| ------------------------------- | ------------------------------------------- |
| `getFields()`                   | 获取 public 字段，包含父类 public 字段      |
| `getDeclaredFields()`           | 获取当前类声明的所有字段，不包含父类字段    |
| `getField(String name)`         | 获取指定 public 字段                        |
| `getDeclaredField(String name)` | 获取当前类声明的指定字段，包括 private 字段 |

下面的代码演示如何获取字段名称、字段类型和字段修饰符。

文件位置：`src/main/java/io/github/atengk/basic/reflect/FieldInfoDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 字段信息读取示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FieldInfoDemo {

    public static void main(String[] args) throws NoSuchFieldException {
        Class<UserInfo> clazz = UserInfo.class;

        Field[] fields = clazz.getDeclaredFields();
        Arrays.stream(fields).forEach(field -> {
            System.out.println("字段名称：" + field.getName());
            System.out.println("字段类型：" + field.getType().getName());
            System.out.println("字段修饰符：" + Modifier.toString(field.getModifiers()));
            System.out.println("----------");
        });

        Field usernameField = clazz.getDeclaredField("username");
        System.out.println("指定字段：" + usernameField.getName());
    }
}
```

字段对象常用方法如下：

| 方法                            | 说明                   |
| ------------------------------- | ---------------------- |
| `getName()`                     | 获取字段名称           |
| `getType()`                     | 获取字段类型           |
| `getGenericType()`              | 获取字段泛型类型       |
| `getModifiers()`                | 获取字段修饰符         |
| `getAnnotation(Class)`          | 获取字段上的指定注解   |
| `setAccessible(true)`           | 允许访问非 public 字段 |
| `get(Object obj)`               | 获取对象中的字段值     |
| `set(Object obj, Object value)` | 设置对象中的字段值     |

字段反射经常用于对象映射、字段校验、Excel 导入导出、JSON 序列化、ORM 框架等场景。

### 获取方法信息

方法信息由 `Method` 表示。通过 `Class` 对象可以获取当前类或父类中的方法。

常用方法如下：

| 方法                                                         | 说明                                        |
| ------------------------------------------------------------ | ------------------------------------------- |
| `getMethods()`                                               | 获取 public 方法，包含父类 public 方法      |
| `getDeclaredMethods()`                                       | 获取当前类声明的所有方法，不包含父类方法    |
| `getMethod(String name, Class<?>... parameterTypes)`         | 获取指定 public 方法                        |
| `getDeclaredMethod(String name, Class<?>... parameterTypes)` | 获取当前类声明的指定方法，包括 private 方法 |

下面的代码演示如何读取方法名称、返回类型、参数类型和修饰符。

文件位置：`src/main/java/io/github/atengk/basic/reflect/MethodInfoDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 方法信息读取示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodInfoDemo {

    public static void main(String[] args) throws NoSuchMethodException {
        Class<UserInfo> clazz = UserInfo.class;

        Method[] methods = clazz.getDeclaredMethods();
        Arrays.stream(methods).forEach(method -> {
            System.out.println("方法名称：" + method.getName());
            System.out.println("返回类型：" + method.getReturnType().getName());
            System.out.println("参数类型：" + Arrays.toString(method.getParameterTypes()));
            System.out.println("方法修饰符：" + Modifier.toString(method.getModifiers()));
            System.out.println("----------");
        });

        Method method = clazz.getDeclaredMethod("getUserSummary", String.class);
        System.out.println("指定方法：" + method.getName());
    }
}
```

方法对象常用方法如下：

| 方法                                 | 说明                 |
| ------------------------------------ | -------------------- |
| `getName()`                          | 获取方法名称         |
| `getReturnType()`                    | 获取返回值类型       |
| `getParameterTypes()`                | 获取参数类型数组     |
| `getGenericParameterTypes()`         | 获取泛型参数类型     |
| `getExceptionTypes()`                | 获取异常类型         |
| `getModifiers()`                     | 获取方法修饰符       |
| `getAnnotation(Class)`               | 获取方法上的指定注解 |
| `invoke(Object obj, Object... args)` | 调用方法             |

方法重载时，获取方法不能只依赖方法名，还需要传入参数类型。例如 `getUserSummary()` 和 `getUserSummary(String prefix)` 是两个不同方法，反射获取时必须明确参数类型。

### 获取构造方法

构造方法信息由 `Constructor` 表示。通过构造方法可以在运行期动态创建对象。

常用方法如下：

| 方法                                                 | 说明                                                |
| ---------------------------------------------------- | --------------------------------------------------- |
| `getConstructors()`                                  | 获取 public 构造方法                                |
| `getDeclaredConstructors()`                          | 获取当前类声明的所有构造方法，包括 private 构造方法 |
| `getConstructor(Class<?>... parameterTypes)`         | 获取指定 public 构造方法                            |
| `getDeclaredConstructor(Class<?>... parameterTypes)` | 获取当前类声明的指定构造方法，包括 private 构造方法 |

下面的代码演示如何读取构造方法信息。

文件位置：`src/main/java/io/github/atengk/basic/reflect/ConstructorInfoDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 构造方法信息读取示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorInfoDemo {

    public static void main(String[] args) throws NoSuchMethodException {
        Class<UserInfo> clazz = UserInfo.class;

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Arrays.stream(constructors).forEach(constructor -> {
            System.out.println("构造方法名称：" + constructor.getName());
            System.out.println("参数类型：" + Arrays.toString(constructor.getParameterTypes()));
            System.out.println("构造方法修饰符：" + Modifier.toString(constructor.getModifiers()));
            System.out.println("----------");
        });

        Constructor<UserInfo> constructor = clazz.getDeclaredConstructor(Long.class, String.class, Integer.class);
        System.out.println("指定构造方法：" + constructor.getName());
    }
}
```

构造方法对象常用方法如下：

| 方法                          | 说明                       |
| ----------------------------- | -------------------------- |
| `getName()`                   | 获取构造方法名称           |
| `getParameterTypes()`         | 获取参数类型               |
| `getModifiers()`              | 获取构造方法修饰符         |
| `setAccessible(true)`         | 允许访问非 public 构造方法 |
| `newInstance(Object... args)` | 创建对象实例               |

在实际开发中，反射创建对象一般优先使用无参构造方法。如果类没有无参构造方法，则需要明确获取对应参数类型的构造方法。

## 反射调用

反射调用指通过 `Constructor` 创建对象，通过 `Field` 访问字段，通过 `Method` 调用方法。它可以突破编译期类型限制，在运行时动态执行操作。

### 创建对象

反射创建对象推荐使用 `getDeclaredConstructor().newInstance()`。早期的 `Class#newInstance()` 已不推荐使用，因为它只能调用 public 无参构造方法，并且异常表达不够清晰。

下面演示通过无参构造方法和有参构造方法创建对象。

文件位置：`src/main/java/io/github/atengk/basic/reflect/CreateObjectDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Constructor;

/**
 * 反射创建对象示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CreateObjectDemo {

    public static void main(String[] args) throws Exception {
        Class<UserInfo> clazz = UserInfo.class;

        Constructor<UserInfo> defaultConstructor = clazz.getDeclaredConstructor();
        UserInfo emptyUser = defaultConstructor.newInstance();

        Constructor<UserInfo> fullConstructor = clazz.getDeclaredConstructor(Long.class, String.class, Integer.class);
        UserInfo user = fullConstructor.newInstance(1L, "Ateng", 18);

        System.out.println(emptyUser.getUserSummary());
        System.out.println(user.getUserSummary());
    }
}
```

反射创建对象的基本流程如下：

| 步骤               | 说明                                           |
| ------------------ | ---------------------------------------------- |
| 获取 `Class` 对象  | 确定要创建哪个类的对象                         |
| 获取 `Constructor` | 根据参数类型找到构造方法                       |
| 调用 `newInstance` | 传入构造参数并创建对象                         |
| 处理异常           | 处理类不存在、构造方法不存在、实例化失败等异常 |

如果需要调用 private 构造方法，需要先设置访问权限。

文件位置：`src/main/java/io/github/atengk/basic/reflect/PrivateConstructorDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Constructor;

/**
 * 私有构造方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PrivateConstructorDemo {

    public static void main(String[] args) throws Exception {
        Class<UserInfo> clazz = UserInfo.class;

        Constructor<UserInfo> constructor = clazz.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);

        UserInfo userInfo = constructor.newInstance("Ateng");

        System.out.println(userInfo.getUserSummary());
    }
}
```

`setAccessible(true)` 可以绕过 Java 访问检查，但不建议在普通业务逻辑中滥用。它更多用于框架、测试、工具类或底层组件。

### 访问字段

通过 `Field` 可以读取或修改对象字段值。访问 private 字段时，需要调用 `setAccessible(true)`。

下面演示通过反射读取和修改字段。

文件位置：`src/main/java/io/github/atengk/basic/reflect/FieldAccessDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Field;

/**
 * 字段访问示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FieldAccessDemo {

    public static void main(String[] args) throws Exception {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);

        Class<?> clazz = userInfo.getClass();

        Field usernameField = clazz.getDeclaredField("username");
        usernameField.setAccessible(true);

        Object oldValue = usernameField.get(userInfo);
        System.out.println("修改前：" + oldValue);

        usernameField.set(userInfo, "Java");

        Object newValue = usernameField.get(userInfo);
        System.out.println("修改后：" + newValue);
        System.out.println(userInfo.getUserSummary());
    }
}
```

字段访问常用方法如下：

| 方法                        | 说明                   |
| --------------------------- | ---------------------- |
| `field.get(obj)`            | 获取指定对象中的字段值 |
| `field.set(obj, value)`     | 设置指定对象中的字段值 |
| `field.getType()`           | 获取字段类型           |
| `field.setAccessible(true)` | 允许访问 private 字段  |

对于基本类型字段，也可以使用 `getInt`、`setInt`、`getLong`、`setLong` 等专用方法。不过普通业务工具中，使用 `get` 和 `set` 更通用。

如果项目中已经引入 Hutool，可以使用 `ReflectUtil` 简化字段访问。

文件位置：`src/main/java/io/github/atengk/basic/reflect/HutoolFieldAccessDemo.java`

```java
package io.github.atengk.basic.reflect;

import cn.hutool.core.util.ReflectUtil;

/**
 * Hutool 字段访问示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HutoolFieldAccessDemo {

    public static void main(String[] args) {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);

        Object oldValue = ReflectUtil.getFieldValue(userInfo, "username");
        System.out.println("修改前：" + oldValue);

        ReflectUtil.setFieldValue(userInfo, "username", "Hutool");

        Object newValue = ReflectUtil.getFieldValue(userInfo, "username");
        System.out.println("修改后：" + newValue);
    }
}
```

Hutool 的 `ReflectUtil` 会封装部分反射细节，适合在业务工具类中快速完成字段访问。但学习反射基础时，仍然需要理解 JDK 原生反射 API。

### 调用方法

通过 `Method` 可以动态调用对象方法。调用 public 方法时可以使用 `getMethod`，调用当前类中的 private 方法时需要使用 `getDeclaredMethod` 并设置访问权限。

下面演示调用无参方法和有参方法。

文件位置：`src/main/java/io/github/atengk/basic/reflect/MethodInvokeDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Method;

/**
 * 方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodInvokeDemo {

    public static void main(String[] args) throws Exception {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);
        Class<?> clazz = userInfo.getClass();

        Method noArgsMethod = clazz.getDeclaredMethod("getUserSummary");
        Object summary = noArgsMethod.invoke(userInfo);
        System.out.println("无参方法返回值：" + summary);

        Method argsMethod = clazz.getDeclaredMethod("getUserSummary", String.class);
        Object summaryWithPrefix = argsMethod.invoke(userInfo, "USER");
        System.out.println("有参方法返回值：" + summaryWithPrefix);
    }
}
```

`invoke` 的第一个参数是目标对象，后面的参数是方法实参。如果调用的是静态方法，第一个参数可以传 `null`。

方法调用常用流程如下：

| 步骤               | 说明                                   |
| ------------------ | -------------------------------------- |
| 获取 `Class` 对象  | 确定方法所在类                         |
| 获取 `Method` 对象 | 根据方法名和参数类型定位方法           |
| 设置访问权限       | private 方法需要 `setAccessible(true)` |
| 调用 `invoke`      | 传入目标对象和方法参数                 |
| 接收返回值         | 返回值统一为 `Object` 类型             |

如果使用 Hutool，可以通过 `ReflectUtil.invoke` 简化调用。

文件位置：`src/main/java/io/github/atengk/basic/reflect/HutoolMethodInvokeDemo.java`

```java
package io.github.atengk.basic.reflect;

import cn.hutool.core.util.ReflectUtil;

/**
 * Hutool 方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HutoolMethodInvokeDemo {

    public static void main(String[] args) {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);

        Object summary = ReflectUtil.invoke(userInfo, "getUserSummary");
        Object summaryWithPrefix = ReflectUtil.invoke(userInfo, "getUserSummary", "USER");

        System.out.println(summary);
        System.out.println(summaryWithPrefix);
    }
}
```

Hutool 写法更简洁，但底层仍然是基于 Java 反射完成方法查找和调用。

### 访问私有成员

私有成员包括 private 字段、private 方法和 private 构造方法。正常代码不能直接访问私有成员，但反射可以通过 `setAccessible(true)` 关闭访问检查。

下面演示访问 private 字段和 private 方法。

文件位置：`src/main/java/io/github/atengk/basic/reflect/PrivateMemberAccessDemo.java`

```java
package io.github.atengk.basic.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 私有成员访问示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PrivateMemberAccessDemo {

    public static void main(String[] args) throws Exception {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);
        Class<?> clazz = userInfo.getClass();

        Field usernameField = clazz.getDeclaredField("username");
        usernameField.setAccessible(true);
        usernameField.set(userInfo, "PrivateUser");

        Method secretMethod = clazz.getDeclaredMethod("buildSecretInfo", String.class);
        secretMethod.setAccessible(true);
        Object secretInfo = secretMethod.invoke(userInfo, "TOKEN");

        System.out.println("字段修改结果：" + userInfo.getUserSummary());
        System.out.println("私有方法返回值：" + secretInfo);
    }
}
```

访问私有成员虽然强大，但要谨慎使用：

| 注意点         | 说明                                               |
| -------------- | -------------------------------------------------- |
| 破坏封装性     | private 原本是类内部实现细节，反射访问会增加耦合   |
| 影响可维护性   | 字段名、方法名修改后，反射代码无法在编译期发现问题 |
| 存在性能成本   | 高频调用时需要考虑缓存反射对象                     |
| 可能受模块限制 | Java 9 之后模块系统可能限制深度反射访问            |
| 安全风险更高   | 不应随意暴露反射调用入口给外部输入                 |

在实际开发中，可以把反射访问封装为工具类，避免在业务代码中散落大量 `getDeclaredField`、`setAccessible`、`invoke` 等底层代码。

下面给出一个简单的反射工具类示例，用于安全读取字段值。

文件位置：`src/main/java/io/github/atengk/basic/reflect/SimpleReflectUtils.java`

```java
package io.github.atengk.basic.reflect;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * 简单反射工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class SimpleReflectUtils {

    public static Optional<Object> getFieldValue(Object target, String fieldName) {
        if (target == null) {
            log.warn("反射读取字段失败，目标对象为空");
            return Optional.empty();
        }
        if (StrUtil.isBlank(fieldName)) {
            log.warn("反射读取字段失败，字段名称为空");
            return Optional.empty();
        }

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return Optional.ofNullable(field.get(target));
        } catch (NoSuchFieldException e) {
            log.warn("反射读取字段失败，字段不存在：{}", fieldName);
            return Optional.empty();
        } catch (IllegalAccessException e) {
            log.warn("反射读取字段失败，字段不可访问：{}", fieldName);
            return Optional.empty();
        }
    }
}
```

使用工具类读取字段。

文件位置：`src/main/java/io/github/atengk/basic/reflect/SimpleReflectUtilsDemo.java`

```java
package io.github.atengk.basic.reflect;

/**
 * 简单反射工具类使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SimpleReflectUtilsDemo {

    public static void main(String[] args) {
        UserInfo userInfo = new UserInfo(1L, "Ateng", 18);

        Object username = SimpleReflectUtils.getFieldValue(userInfo, "username")
                .orElse("未知用户");

        System.out.println(username);
    }
}
```

反射调用的核心结论是：能不用反射时优先不用；需要通用化、框架化、动态化时再使用。使用反射时，应尽量封装公共逻辑、缓存反射对象、明确异常处理，并避免把外部不可信输入直接作为类名、字段名或方法名执行。



## 注解与反射实践

注解与反射通常会结合使用：注解负责声明规则，反射负责在运行期读取规则并执行逻辑。常见实践包括字段校验、对象映射、通用工具封装、权限判断、日志记录、接口扫描等。

本节通过三个小案例说明注解与反射的典型用法。

### 基于注解实现字段校验

字段校验的基本思路是：先定义校验注解，然后把注解标记在实体类字段上，最后通过反射读取字段上的注解并校验字段值。

下面定义一个必填字段注解，用于标记字段不能为空。

文件位置：`src/main/java/io/github/atengk/basic/practice/Required.java`

```java
package io.github.atengk.basic.practice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 必填字段注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {

    String message() default "字段不能为空";
}
```

下面定义一个长度校验注解，用于限制字符串字段的长度范围。

文件位置：`src/main/java/io/github/atengk/basic/practice/Length.java`

```java
package io.github.atengk.basic.practice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字符串长度校验注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Length {

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String message() default "字段长度不合法";
}
```

下面定义一个用户注册请求类，并在字段上添加校验注解。

文件位置：`src/main/java/io/github/atengk/basic/practice/UserRegisterRequest.java`

```java
package io.github.atengk.basic.practice;

/**
 * 用户注册请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserRegisterRequest {

    @Required(message = "用户名不能为空")
    @Length(min = 2, max = 20, message = "用户名长度必须在 2 到 20 个字符之间")
    private String username;

    @Required(message = "手机号不能为空")
    @Length(min = 11, max = 11, message = "手机号长度必须为 11 位")
    private String phone;

    public UserRegisterRequest(String username, String phone) {
        this.username = username;
        this.phone = phone;
    }
}
```

下面定义字段校验器，通过反射读取字段注解并完成校验。

文件位置：`src/main/java/io/github/atengk/basic/practice/AnnotationValidator.java`

```java
package io.github.atengk.basic.practice;

import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 注解字段校验器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnnotationValidator {

    public static List<String> validate(Object target) {
        List<String> errors = new ArrayList<>();
        if (target == null) {
            errors.add("校验对象不能为空");
            return errors;
        }

        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = getFieldValue(target, field);

            validateRequired(field, value, errors);
            validateLength(field, value, errors);
        }

        return errors;
    }

    private static Object getFieldValue(Object target, Field field) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void validateRequired(Field field, Object value, List<String> errors) {
        Required required = field.getAnnotation(Required.class);
        if (required == null) {
            return;
        }

        if (value == null) {
            errors.add(required.message());
            return;
        }

        if (value instanceof CharSequence text && StrUtil.isBlank(text)) {
            errors.add(required.message());
        }
    }

    private static void validateLength(Field field, Object value, List<String> errors) {
        Length length = field.getAnnotation(Length.class);
        if (length == null || value == null) {
            return;
        }

        if (!(value instanceof CharSequence text)) {
            errors.add(field.getName() + " 不是字符串类型，不能使用 @Length 校验");
            return;
        }

        int size = text.length();
        if (size < length.min() || size > length.max()) {
            errors.add(length.message());
        }
    }
}
```

下面通过一个测试类验证字段校验效果。

文件位置：`src/main/java/io/github/atengk/basic/practice/AnnotationValidatorDemo.java`

```java
package io.github.atengk.basic.practice;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 注解字段校验示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnnotationValidatorDemo {

    public static void main(String[] args) {
        UserRegisterRequest request = new UserRegisterRequest("", "138");

        List<String> errors = AnnotationValidator.validate(request);
        if (CollUtil.isEmpty(errors)) {
            System.out.println("校验通过");
            return;
        }

        errors.forEach(System.out::println);
    }
}
```

运行结果如下：

```text
用户名不能为空
用户名长度必须在 2 到 20 个字符之间
手机号长度必须为 11 位
```

这个案例的核心流程是：字段上写注解，校验器读取注解，根据注解属性执行规则。后续可以继续扩展 `@Range`、`@Email`、`@Pattern`、`@EnumValue` 等注解。

### 基于注解实现对象映射

对象映射的基本思路是：在目标对象字段上声明来源字段名，程序运行时通过反射读取注解，把源对象中的字段值复制到目标对象中。

下面定义一个对象映射注解，用于声明目标字段对应的源字段名称。

文件位置：`src/main/java/io/github/atengk/basic/practice/MappingField.java`

```java
package io.github.atengk.basic.practice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对象字段映射注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MappingField {

    String source();
}
```

下面定义源对象，模拟数据库实体或第三方接口返回对象。

文件位置：`src/main/java/io/github/atengk/basic/practice/UserEntity.java`

```java
package io.github.atengk.basic.practice;

/**
 * 用户实体对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserEntity {

    private Long id;

    private String username;

    private Integer age;

    public UserEntity(Long id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }
}
```

下面定义目标对象，字段名可以和源对象不一致，通过 `@MappingField` 指定来源字段。

文件位置：`src/main/java/io/github/atengk/basic/practice/UserVO.java`

```java
package io.github.atengk.basic.practice;

/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserVO {

    @MappingField(source = "id")
    private Long userId;

    @MappingField(source = "username")
    private String name;

    @MappingField(source = "age")
    private Integer userAge;

    @Override
    public String toString() {
        return "UserVO{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", userAge=" + userAge +
                '}';
    }
}
```

下面实现一个简单对象映射工具，读取目标字段上的 `@MappingField`，再从源对象中取值并写入目标对象。

文件位置：`src/main/java/io/github/atengk/basic/practice/AnnotationMapper.java`

```java
package io.github.atengk.basic.practice;

import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * 注解对象映射工具
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnnotationMapper {

    public static <T> T map(Object source, Class<T> targetClass) {
        if (source == null) {
            throw new IllegalArgumentException("源对象不能为空");
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类型不能为空");
        }

        T target = ReflectUtil.newInstance(targetClass);
        Field[] targetFields = targetClass.getDeclaredFields();

        for (Field targetField : targetFields) {
            MappingField mappingField = targetField.getAnnotation(MappingField.class);
            if (mappingField == null) {
                continue;
            }

            Object sourceValue = ReflectUtil.getFieldValue(source, mappingField.source());
            ReflectUtil.setFieldValue(target, targetField.getName(), sourceValue);
        }

        return target;
    }
}
```

下面通过测试类验证对象映射效果。

文件位置：`src/main/java/io/github/atengk/basic/practice/AnnotationMapperDemo.java`

```java
package io.github.atengk.basic.practice;

/**
 * 注解对象映射示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnnotationMapperDemo {

    public static void main(String[] args) {
        UserEntity userEntity = new UserEntity(1L, "Ateng", 18);

        UserVO userVO = AnnotationMapper.map(userEntity, UserVO.class);

        System.out.println(userVO);
    }
}
```

运行结果如下：

```text
UserVO{userId=1, name='Ateng', userAge=18}
```

这个案例适合理解 MapStruct、BeanUtils、JSON 序列化、ORM 字段映射等框架的基本思想。实际生产项目中，简单对象复制可以使用 Hutool 的 `BeanUtil`，复杂对象映射建议使用 MapStruct，因为它在编译期生成代码，性能和类型安全更好。

### 基于反射实现通用工具

反射适合封装通用工具，例如读取字段、设置字段、调用方法、判断字段是否存在、批量复制字段等。业务代码不建议到处直接写反射 API，最好封装成工具类统一处理异常和边界条件。

下面实现一个简单反射工具类，支持读取字段值、设置字段值、调用无参方法和判断字段是否存在。

文件位置：`src/main/java/io/github/atengk/basic/practice/ReflectHelper.java`

```java
package io.github.atengk.basic.practice;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * 反射辅助工具
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ReflectHelper {

    public static Optional<Object> getFieldValue(Object target, String fieldName) {
        if (target == null || StrUtil.isBlank(fieldName)) {
            log.warn("读取字段失败，目标对象或字段名为空");
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(ReflectUtil.getFieldValue(target, fieldName));
        } catch (Exception e) {
            log.warn("读取字段失败，字段名：{}", fieldName);
            return Optional.empty();
        }
    }

    public static boolean setFieldValue(Object target, String fieldName, Object value) {
        if (target == null || StrUtil.isBlank(fieldName)) {
            log.warn("设置字段失败，目标对象或字段名为空");
            return false;
        }

        try {
            ReflectUtil.setFieldValue(target, fieldName, value);
            return true;
        } catch (Exception e) {
            log.warn("设置字段失败，字段名：{}", fieldName);
            return false;
        }
    }

    public static Optional<Object> invoke(Object target, String methodName, Object... args) {
        if (target == null || StrUtil.isBlank(methodName)) {
            log.warn("调用方法失败，目标对象或方法名为空");
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(ReflectUtil.invoke(target, methodName, args));
        } catch (Exception e) {
            log.warn("调用方法失败，方法名：{}", methodName);
            return Optional.empty();
        }
    }

    public static boolean hasField(Class<?> targetClass, String fieldName) {
        if (targetClass == null || StrUtil.isBlank(fieldName)) {
            return false;
        }

        Field field = ReflectUtil.getField(targetClass, fieldName);
        return field != null;
    }
}
```

下面通过一个测试类验证通用反射工具。

文件位置：`src/main/java/io/github/atengk/basic/practice/ReflectHelperDemo.java`

```java
package io.github.atengk.basic.practice;

/**
 * 反射辅助工具示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ReflectHelperDemo {

    public static void main(String[] args) {
        UserEntity userEntity = new UserEntity(1L, "Ateng", 18);

        Object username = ReflectHelper.getFieldValue(userEntity, "username")
                .orElse("未知用户");
        System.out.println("读取字段：" + username);

        boolean setResult = ReflectHelper.setFieldValue(userEntity, "username", "Java");
        System.out.println("设置字段：" + setResult);

        Object newUsername = ReflectHelper.getFieldValue(userEntity, "username")
                .orElse("未知用户");
        System.out.println("修改后字段：" + newUsername);

        boolean hasAgeField = ReflectHelper.hasField(UserEntity.class, "age");
        System.out.println("是否存在 age 字段：" + hasAgeField);
    }
}
```

运行结果如下：

```text
读取字段：Ateng
设置字段：true
修改后字段：Java
是否存在 age 字段：true
```

这类工具适合用于基础组件、通用转换、测试辅助、低代码配置、动态字段读取等场景。高频调用时建议缓存 `Field`、`Method`、`Constructor`，避免每次重复解析。

## 常见问题

本节整理注解与反射学习和开发中最常见的问题。重点关注注解是否会影响执行、注解保留策略区别、反射方法查找区别，以及反射性能问题。

### 注解是否会影响程序执行

注解本身通常不会直接影响程序执行。注解只是附加在代码元素上的元数据，真正影响程序执行的是读取并处理注解的编译器、框架、工具或反射代码。

例如：

```java
@Override
public String toString() {
    return "demo";
}
```

`@Override` 不会在运行时改变 `toString()` 的执行逻辑，它只是让编译器检查该方法是否真的重写了父类方法。

再看一个自定义注解：

```java
@Required(message = "用户名不能为空")
private String username;
```

如果程序中没有类似 `AnnotationValidator.validate(request)` 这样的解析逻辑，那么 `@Required` 不会自动校验字段。只有当校验器通过反射读取到该注解，并执行校验逻辑时，它才会产生实际效果。

可以按处理者来理解注解的影响：

| 注解类型         | 处理者                 | 是否直接影响运行                     |
| ---------------- | ---------------------- | ------------------------------------ |
| `@Override`      | 编译器                 | 不影响运行，只做编译检查             |
| `@Deprecated`    | 编译器、IDE、文档工具  | 不阻止运行，只提示过时               |
| Lombok 注解      | 编译期工具             | 编译期生成代码，间接影响运行代码     |
| Spring 注解      | Spring 框架            | 框架读取后影响 Bean 创建、请求映射等 |
| 自定义运行期注解 | 自己写的反射逻辑或 AOP | 是否影响运行取决于解析逻辑           |

结论是：注解本身不等于功能，注解加解析器才构成功能。

### SOURCE、CLASS、RUNTIME 的区别

`SOURCE`、`CLASS`、`RUNTIME` 是注解的三种保留策略，由 `@Retention` 指定。它们决定注解信息会保留到哪个阶段。

| 保留策略  | 保留阶段   | 是否进入 `.class` 文件 | 是否能反射读取 | 典型场景                     |
| --------- | ---------- | ---------------------- | -------------- | ---------------------------- |
| `SOURCE`  | 源码阶段   | 否                     | 否             | 编译检查、源码生成、Lombok   |
| `CLASS`   | 字节码阶段 | 是                     | 否             | 字节码工具、编译期框架处理   |
| `RUNTIME` | 运行阶段   | 是                     | 是             | Spring、反射解析、运行期校验 |

`SOURCE` 表示注解只存在于 `.java` 源文件中，编译成 `.class` 文件后会被丢弃。适合只在编译期使用的注解，例如源码检查或代码生成。

```java
@Retention(RetentionPolicy.SOURCE)
public @interface SourceOnly {
}
```

`CLASS` 表示注解会进入 `.class` 文件，但 JVM 运行时默认不会保留给反射读取。它适合字节码增强、编译后处理工具等场景。

```java
@Retention(RetentionPolicy.CLASS)
public @interface ClassOnly {
}
```

`RUNTIME` 表示注解不仅进入 `.class` 文件，还能在程序运行时通过反射读取。只要需要运行时解析注解，就必须使用它。

```java
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeVisible {
}
```

选择策略时可以遵循这个判断：

| 需求                         | 推荐策略  |
| ---------------------------- | --------- |
| 只给编译器或源码工具使用     | `SOURCE`  |
| 给字节码工具或编译后工具使用 | `CLASS`   |
| 程序运行时需要通过反射读取   | `RUNTIME` |

常见误区是定义了自定义注解，却忘记加 `@Retention(RetentionPolicy.RUNTIME)`，导致运行时 `getAnnotation()` 一直返回 `null`。

### getDeclaredMethod 与 getMethod 的区别

`getDeclaredMethod` 和 `getMethod` 都用于反射获取方法，但查找范围不同。

| 方法                | 查找范围         | 是否能获取 private 方法 | 是否查找父类             |
| ------------------- | ---------------- | ----------------------- | ------------------------ |
| `getMethod`         | public 方法      | 否                      | 是，包含父类 public 方法 |
| `getDeclaredMethod` | 当前类声明的方法 | 是                      | 否，不包含父类方法       |

示例类如下。

文件位置：`src/main/java/io/github/atengk/basic/practice/BaseService.java`

```java
package io.github.atengk.basic.practice;

/**
 * 基础服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BaseService {

    public void publicBaseMethod() {
        System.out.println("父类 public 方法");
    }
}
```

文件位置：`src/main/java/io/github/atengk/basic/practice/UserService.java`

```java
package io.github.atengk.basic.practice;

/**
 * 用户服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserService extends BaseService {

    public void publicUserMethod() {
        System.out.println("子类 public 方法");
    }

    private void privateUserMethod() {
        System.out.println("子类 private 方法");
    }
}
```

下面演示 `getMethod` 和 `getDeclaredMethod` 的差异。

文件位置：`src/main/java/io/github/atengk/basic/practice/MethodFindDemo.java`

```java
package io.github.atengk.basic.practice;

import java.lang.reflect.Method;

/**
 * 方法查找示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodFindDemo {

    public static void main(String[] args) throws Exception {
        Class<UserService> clazz = UserService.class;

        Method publicUserMethod = clazz.getMethod("publicUserMethod");
        System.out.println(publicUserMethod.getName());

        Method publicBaseMethod = clazz.getMethod("publicBaseMethod");
        System.out.println(publicBaseMethod.getName());

        Method privateUserMethod = clazz.getDeclaredMethod("privateUserMethod");
        privateUserMethod.setAccessible(true);
        privateUserMethod.invoke(new UserService());
    }
}
```

运行结果如下：

```text
publicUserMethod
publicBaseMethod
子类 private 方法
```

如果使用 `clazz.getMethod("privateUserMethod")`，会抛出 `NoSuchMethodException`，因为 `getMethod` 只能获取 public 方法。

如果使用 `clazz.getDeclaredMethod("publicBaseMethod")`，也会抛出 `NoSuchMethodException`，因为 `getDeclaredMethod` 只查找当前类声明的方法，不查找父类。

选择建议如下：

| 场景                                        | 推荐方法                                                 |
| ------------------------------------------- | -------------------------------------------------------- |
| 获取当前类或父类的 public 方法              | `getMethod`                                              |
| 获取当前类 private、protected、default 方法 | `getDeclaredMethod`                                      |
| 获取当前类声明的 public 方法                | 两者都可以，但 `getDeclaredMethod` 范围更精确            |
| 获取父类 private 方法                       | 需要先拿到父类 `Class`，再调用父类的 `getDeclaredMethod` |

### 反射为什么会影响性能

反射影响性能主要是因为它绕过了普通方法调用的静态绑定路径，需要在运行时动态查找、权限检查、参数封装、类型匹配和方法分派。

主要原因如下：

| 原因                   | 说明                                                  |
| ---------------------- | ----------------------------------------------------- |
| 运行期动态查找         | 需要根据字符串名称查找字段、方法或构造方法            |
| 访问权限检查           | 反射调用前可能需要检查 public、private 等访问权限     |
| 参数装箱和数组封装     | `Method.invoke` 使用 `Object... args`，会涉及参数包装 |
| 无法完全享受编译期优化 | 普通调用更容易被 JIT 内联优化                         |
| 异常包装成本           | 反射调用异常通常会被包装成反射相关异常                |
| 代码可读性降低         | 维护成本变高，间接影响开发效率                        |

普通方法调用是直接的：

```java
userService.publicUserMethod();
```

反射方法调用是动态的：

```java
Method method = UserService.class.getMethod("publicUserMethod");
method.invoke(userService);
```

第二种方式灵活性更高，但 JVM 需要做更多运行期处理。因此在高频场景中，反射通常比直接调用慢。

不过，反射性能问题要结合场景看：

| 场景                                                | 建议                                                 |
| --------------------------------------------------- | ---------------------------------------------------- |
| 程序启动阶段扫描类和注解                            | 可以接受                                             |
| 管理后台少量动态字段读取                            | 可以接受                                             |
| 每秒大量调用反射方法                                | 需要优化                                             |
| 循环中频繁 `getDeclaredField` / `getDeclaredMethod` | 应该缓存                                             |
| 简单属性复制                                        | 优先使用成熟工具或编译期映射                         |
| 极致性能场景                                        | 优先使用直接调用、MapStruct、MethodHandle 或生成代码 |

优化反射性能的常见方式如下：

| 优化方式                   | 说明                                                |
| -------------------------- | --------------------------------------------------- |
| 缓存反射对象               | 缓存 `Field`、`Method`、`Constructor`，避免重复查找 |
| 减少反射调用频率           | 不要在大循环中重复解析反射信息                      |
| 使用编译期生成代码         | 如 MapStruct、Lombok、部分代码生成器                |
| 使用 MethodHandle          | 在特定场景下比传统反射更适合高频调用                |
| 封装工具类                 | 集中处理异常、缓存和访问控制                        |
| 启动期解析，运行期使用缓存 | Spring、ORM 框架常用思路                            |

下面给出一个简单的字段缓存工具示例，避免每次读取字段时都重复调用 `getDeclaredField`。

文件位置：`src/main/java/io/github/atengk/basic/practice/CachedFieldReader.java`

```java
package io.github.atengk.basic.practice;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存字段读取器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CachedFieldReader {

    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    public static Optional<Object> read(Object target, String fieldName) {
        if (target == null || StrUtil.isBlank(fieldName)) {
            log.warn("读取字段失败，目标对象或字段名为空");
            return Optional.empty();
        }

        try {
            Class<?> targetClass = target.getClass();
            Field field = getCachedField(targetClass, fieldName);
            return Optional.ofNullable(field.get(target));
        } catch (Exception e) {
            log.warn("读取字段失败，字段名：{}", fieldName);
            return Optional.empty();
        }
    }

    private static Field getCachedField(Class<?> targetClass, String fieldName) throws NoSuchFieldException {
        String cacheKey = targetClass.getName() + "#" + fieldName;
        Field cachedField = FIELD_CACHE.get(cacheKey);
        if (cachedField != null) {
            return cachedField;
        }

        Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        FIELD_CACHE.put(cacheKey, field);
        return field;
    }
}
```

使用方式如下。

文件位置：`src/main/java/io/github/atengk/basic/practice/CachedFieldReaderDemo.java`

```java
package io.github.atengk.basic.practice;

/**
 * 缓存字段读取器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CachedFieldReaderDemo {

    public static void main(String[] args) {
        UserEntity userEntity = new UserEntity(1L, "Ateng", 18);

        Object username = CachedFieldReader.read(userEntity, "username")
                .orElse("未知用户");

        System.out.println(username);
    }
}
```

反射不是不能用，而是要控制使用边界。对于框架、通用组件、动态映射、注解解析等场景，反射非常有价值；对于普通业务逻辑和高频核心链路，应优先使用直接调用、接口抽象或编译期生成代码。
