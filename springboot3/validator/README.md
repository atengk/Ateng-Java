# SpringBoot3参数效验相关的模块

Spring Boot Validation 是一个用于简化 Java 应用程序中数据校验的框架，它基于 Java 的 **Bean Validation 规范（JSR 380）**，并通过注解的方式在代码层面提供方便的校验功能。Spring Boot 提供了 `spring-boot-starter-validation` 起步依赖，集成了 Hibernate Validator 作为默认的校验实现。



**注意：**

该模块是基于`全局异常处理模块`



## 基础配置

### 添加依赖

```xml
        <!-- Spring Boot Validation 数据校验框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

### 配置 GlobalExceptionHandler 

在GlobalExceptionHandler添加效验相关的全局异常配置

```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import local.ateng.java.validator.constant.AppCodeEnum;
import local.ateng.java.validator.exception.ServiceException;
import local.ateng.java.validator.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理 POST 请求参数校验失败的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationExceptions(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = error.getField();
                firstErrorMessage = error.getDefaultMessage();
            }
            errors.put(error.getField(), error.getDefaultMessage());
        }
        // 打印异常日志
        log.error("处理 POST 请求参数校验失败的异常 ==> {}", ex.getMessage());
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.error(firstErrorMessage).setData(errors);
    }

    // 处理 GET 请求参数校验失败的异常
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolationException(HttpServletRequest request, HttpServletResponse response, ConstraintViolationException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            // 只保留参数名称
            String fieldName = propertyPath.split("\\.")[1];
            String errorMessage = violation.getMessage();
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = fieldName;
                firstErrorMessage = errorMessage;
            }
            errors.put(fieldName, errorMessage);
        }
        // 打印异常日志
        log.error("处理 GET 请求参数校验失败的异常 ==> {}", ex.getMessage());
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.error(firstErrorMessage).setData(errors);
    }

    // ......
}
```

**配置快速返回**

配置快速返回模式，效验到参数错误立即返回，不进行后续的校验

```java
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 校验框架配置类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Configuration
public class ValidatorConfig {

    /**
     * 配置校验框架 快速返回模式
     */
    @Bean
    public Validator validator() {
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(true)  // 启用快速失败模式
                .buildValidatorFactory();
        return validatorFactory.getValidator();
    }

}
```

## 创建实体类

### User

```java
package local.ateng.java.validator.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class User {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3到20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

}
```

### Account

```java
package local.ateng.java.validator.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import local.ateng.java.validator.validation.AddGroup;
import local.ateng.java.validator.validation.UpdateGroup;
import lombok.Data;

@Data
public class Account {
    @NotBlank(message = "账户名不能为空", groups = AddGroup.class) // 创建时必填
    @Size(min = 3, max = 15, message = "账户名长度必须在3到15个字符之间", groups = AddGroup.class) // 创建时长度限制
    @Size(min = 3, message = "账户名长度至少为3个字符", groups = UpdateGroup.class) // 更新时的最小长度
    private String accountName;

    @NotBlank(message = "密码不能为空", groups = AddGroup.class) // 创建时必填
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符", groups = AddGroup.class) // 创建时长度限制
    private String password;

    @NotBlank(message = "邮箱不能为空", groups = AddGroup.class) // 创建时必填
    private String email;
}
```

### UserClass

**ClassName**

```java
package local.ateng.java.validator.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassName {
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id错误")
    private Long id;
    @NotBlank(message = "name不能为空")
    @Size(min = 3, max = 20, message = "name长度必须在3到20个字符之间")
    private String name;
}
```

**UserClass**

```java
package local.ateng.java.validator.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserClass {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3到20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "班级信息不能为空")
    @Valid
    private ClassName className;

    @NotNull(message = "ids不能为空")
    private List<@Valid @Size(min = 6, max = 100, message = "密码长度至少为6个字符") String> ids;

    @NotNull(message = "custom不能为空")
    private String custom;

    // 自定义校验方法
    @AssertTrue(message = "custom属性不满足要求")
    public boolean isCustomValid() {
        return custom != null && custom.length() > 3 && custom.contains("custom");
    }
}
```



## 创建接口

### UserController

```java
package local.ateng.java.validator.controller;

import jakarta.validation.constraints.Min;
import local.ateng.java.validator.entity.User;
import local.ateng.java.validator.utils.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user") // 基础路径为/user
@Validated // 启用方法参数的校验功能，允许在控制器方法中使用验证注解。
public class UserController {

    /**
     * 注册用户的方法，处理POST请求。
     *
     * @param user 需要注册的用户信息，@RequestBody会将请求体中的JSON映射为User对象。
     * @return Result对象，表示操作结果。
     */
    @PostMapping("/register") // 映射HTTP POST请求到/register路径。
    public Result registerUser(@Validated @RequestBody User user) {
        // @Valid注解用于验证user对象的属性，如果不符合要求会抛出异常。
        // 在这里处理用户注册逻辑，比如保存用户到数据库
        return Result.success(); // 返回成功的结果
    }

    /**
     * 查找用户的方法，处理GET请求。
     *
     * @param id 用户的唯一标识符，必须大于0。
     * @return Result对象，表示操作结果。
     */
    @GetMapping("/findUser") // 映射HTTP GET请求到/findUser路径。
    public Result findUser(
            @RequestParam("id") // 指定id为请求参数，来自URL查询字符串。
            @Min(value = 1, message = "用户ID必须大于0") // 校验id必须大于0，校验失败时返回错误信息。
            Long id
    ) {
        // 假设这里处理查询逻辑
        return Result.success(); // 返回成功的结果
    }
}
```

### AccountController

```java
package local.ateng.java.validator.controller;

import local.ateng.java.validator.entity.Account;
import local.ateng.java.validator.utils.Result;
import local.ateng.java.validator.validation.AddGroup;
import local.ateng.java.validator.validation.UpdateGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account") // 基础路径为/account
public class AccountController {

    /**
     * 创建账户的接口，使用CreateAccountGroup进行校验。
     *
     * @param account 账户信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result createAccount(@Validated(AddGroup.class) @RequestBody Account account) {
        // 在这里处理账户创建逻辑，比如保存账户到数据库
        return Result.success();
    }

    /**
     * 更新账户信息的接口，使用UpdateAccountGroup进行校验。
     *
     * @param account 账户信息
     * @return 更新结果
     */
    @PostMapping("/update")
    public Result updateAccount(@Validated(UpdateGroup.class) @RequestBody Account account) {
        // 在这里处理账户更新逻辑，比如更新账户信息到数据库
        return Result.success();
    }
}
```

**UserClassController**

```java
package local.ateng.java.validator.controller;

import local.ateng.java.validator.entity.UserClass;
import local.ateng.java.validator.utils.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user-class")
public class UserClassController {

    @PostMapping("/add")
    public Result add(@Validated @RequestBody UserClass userClass) {
        return Result.success(userClass);
    }

}
```





## 参数效验总结

### @Validated注解的使用

| 请求方式                         | 参数类型                          | `@Validated` 放置位置                  |
| -------------------------------- | --------------------------------- | -------------------------------------- |
| `@PostMapping` / `@PutMapping`   | `@RequestBody`                    | 方法参数上（`@Validated` 或 `@Valid`） |
| `@GetMapping` / `@DeleteMapping` | `@RequestParam` / `@PathVariable` | 类上（推荐）或方法参数上               |
| `@PatchMapping`                  | `@RequestBody`                    | 方法参数上（通常结合分组校验）         |
| 返回值校验                       | 方法返回值                        | 类上（启用校验）+ 返回值加 `@Valid`    |

### 常见校验注解

| **注解**                    | **适用类型**             | **作用**                                         | **示例**                                                     |
| --------------------------- | ------------------------ | ------------------------------------------------ | ------------------------------------------------------------ |
| `@NotNull`                  | 所有对象类型             | 确保值不为 `null`                                | `@NotNull(message = "值不能为空")`                           |
| `@Null`                     | 所有对象类型             | 确保值为 `null`                                  | `@Null(message = "值必须为null")`                            |
| `@NotBlank`                 | 字符串类型               | 确保字符串不为 `null` 且去除空格后长度大于 0     | `@NotBlank(message = "值不能为空")`                          |
| `@NotEmpty`                 | 字符串、集合、数组等     | 确保字符串、集合、数组等不为 `null` 且非空       | `@NotEmpty(message = "集合不能为空")`                        |
| `@Size`                     | 字符串、集合、数组等     | 确保字符串、集合、数组等的长度或大小在指定范围内 | `@Size(min = 3, max = 10, message = "长度限制")`             |
| `@Pattern`                  | 字符串                   | 确保字符串符合指定的正则表达式                   | `@Pattern(regexp = "\\d{10}", message = "手机号不合法")`     |
| `@Min`                      | 数字类型                 | 确保数字值大于等于指定最小值                     | `@Min(value = 18, message = "年龄必须≥18")`                  |
| `@Max`                      | 数字类型                 | 确保数字值小于等于指定最大值                     | `@Max(value = 100, message = "年龄必须≤100")`                |
| `@Positive`                 | 数字类型                 | 确保数字值为正数（不包含 0）                     | `@Positive(message = "值必须为正数")`                        |
| `@PositiveOrZero`           | 数字类型                 | 确保数字值为正数或 0                             | `@PositiveOrZero(message = "值不能为负数")`                  |
| `@Negative`                 | 数字类型                 | 确保数字值为负数（不包含 0）                     | `@Negative(message = "值必须为负数")`                        |
| `@NegativeOrZero`           | 数字类型                 | 确保数字值为负数或 0                             | `@NegativeOrZero(message = "值不能为正数")`                  |
| `@Digits`                   | 数字类型                 | 确保数字值符合指定的整数位和小数位限制           | `@Digits(integer = 3, fraction = 2, message = "格式不正确")` |
| `@Email`                    | 字符串                   | 确保字符串符合电子邮件格式                       | `@Email(message = "邮箱格式不正确")`                         |
| `@Future`                   | 日期类型                 | 确保值是未来的日期                               | `@Future(message = "日期必须在未来")`                        |
| `@FutureOrPresent`          | 日期类型                 | 确保值是未来或当前的日期                         | `@FutureOrPresent(message = "日期必须是今天或将来")`         |
| `@Past`                     | 日期类型                 | 确保值是过去的日期                               | `@Past(message = "日期必须在过去")`                          |
| `@PastOrPresent`            | 日期类型                 | 确保值是过去或当前的日期                         | `@PastOrPresent(message = "日期必须是今天或过去")`           |
| `@AssertTrue`               | 布尔类型                 | 确保值为 `true`                                  | `@AssertTrue(message = "条件必须为真")`                      |
| `@AssertFalse`              | 布尔类型                 | 确保值为 `false`                                 | `@AssertFalse(message = "条件必须为假")`                     |
| `@Valid`                    | 复合对象类型（嵌套校验） | 对嵌套的对象进行校验                             | 用于嵌套对象属性，如：`@Valid private Address address;`      |
| `@NotEmpty`                 | 字符串、集合、数组等     | 确保字符串、集合或数组不为空                     | `@NotEmpty(message = "集合不能为空")`                        |
| `@Length`（Hibernate 专用） | 字符串                   | 确保字符串长度在指定范围内                       | `@Length(min = 3, max = 20, message = "长度限制")`           |
| `@Range`（Hibernate 专用）  | 数字类型                 | 确保数字在指定范围内                             | `@Range(min = 1, max = 100, message = "值不在范围内")`       |



### 方法校验注解

1. **`@AssertTrue`**

- 用于标注一个方法，其返回值必须为 `true`。

- 常用于自定义逻辑校验。

在实体类中的方法上，实现对字段的自定义逻辑效验

```
@AssertTrue(message = "custom属性不满足要求")
public boolean isCustomValid() {
    return custom != null && custom.length() > 3 && custom.contains("custom");
}
```



### 分组效验

分组校验（Group Validation）是 Java Bean Validation 中的一个重要特性，它允许我们对同一个字段或类的不同校验规则进行分组，以便在不同的情境下应用不同的校验规则。分组校验通常用于区分对象在不同操作下需要满足的校验规则，常见的场景有：**创建时校验**、**更新时校验**等。

#### 1. **分组校验的基本概念**

Java Bean Validation 提供了 `@GroupSequence` 和 `@Validated` 等注解来支持分组校验。你可以将校验分成多个组，在不同的上下文中根据需要应用不同的校验规则。

#### 2. **如何使用分组校验**

##### 2.1 定义校验组接口

首先，你需要定义一个或多个校验组接口。这些接口用于区分不同的校验逻辑。

```java
public interface AddGroup { }
public interface UpdateGroup { }
```

##### 2.2 在属性上使用分组

然后，在需要分组校验的字段或方法上使用这些校验组接口。例如，可以为某些字段在“添加”时要求某些校验，在“更新”时要求其他校验。

```java
@Data
public class User {

    @NotBlank(message = "用户名不能为空", groups = AddGroup.class)
    private String username;

    @NotBlank(message = "密码不能为空", groups = {AddGroup.class, UpdateGroup.class})
    private String password;

    @Email(message = "邮箱格式不正确", groups = AddGroup.class)
    private String email;
}
```

- `@NotBlank(groups = AddGroup.class)`：表示“用户名”在 **添加操作** 时不能为空。
- `@NotBlank(groups = {AddGroup.class, UpdateGroup.class})`：表示“密码”在 **添加和更新操作** 中都不能为空。
- `@Email(groups = AddGroup.class)`：表示“邮箱”在 **添加操作** 时必须符合邮箱格式。

##### 2.3 使用 `@Validated` 或 `@Valid` 注解

然后，在控制器的方法上使用 `@Validated` 来指定校验组。`@Validated` 可以指定一个或多个校验组，或者使用默认校验组。

```java
// 在添加用户时进行校验
@PostMapping("/add")
public Result add(@Validated(AddGroup.class) @RequestBody User user) {
    return Result.success(user);
}

// 在更新用户时进行校验
@PutMapping("/update")
public Result update(@Validated(UpdateGroup.class) @RequestBody User user) {
    return Result.success(user);
}
```

- `@Validated(AddGroup.class)`：在 **添加操作** 时，仅执行 `AddGroup` 校验组。
- `@Validated(UpdateGroup.class)`：在 **更新操作** 时，仅执行 `UpdateGroup` 校验组。

##### 2.4 使用 `@GroupSequence` 自定义校验顺序（可选）

如果你需要自定义校验顺序，可以使用 `@GroupSequence` 注解来指定校验顺序。`@GroupSequence` 注解定义了多个校验组的执行顺序，这样可以根据需要先执行一些校验，再执行其他校验。

```java
@GroupSequence({AddGroup.class, UpdateGroup.class})
public interface DefaultGroup { }
```

然后在 `@Validated` 中使用：

```java
@PostMapping("/add")
public Result add(@Validated(DefaultGroup.class) @RequestBody User user) {
    return Result.success(user);
}
```

`@GroupSequence` 可以确保在进行分组校验时，按照指定的顺序进行校验。

#### 3. **分组校验的常见应用场景**

分组校验在实际开发中非常常见，以下是一些常见的应用场景：

##### 3.1 **创建与更新校验**
- **创建时**：某些字段必须填写（如用户名、邮箱），并且可能有特定的规则。
- **更新时**：有些字段可能可以为空或可以忽略某些校验。

例如：

```java
public class User {

    @NotBlank(message = "用户名不能为空", groups = AddGroup.class)
    private String username;

    @NotBlank(message = "密码不能为空", groups = {AddGroup.class, UpdateGroup.class})
    private String password;

    @Email(message = "邮箱格式不正确", groups = AddGroup.class)
    private String email;
}
```

创建时：
- `username` 和 `email` 必须填，且邮箱格式要正确。
- `password` 必须填。

更新时：
- `password` 必须填，但 `username` 和 `email` 可能不需要重新校验。

##### 3.2 **复杂对象校验**

如果一个对象包含多个嵌套对象，分组校验可以用来分层级地校验这些嵌套对象。

```java
@Data
public class UserAccount {

    @NotNull(groups = AddGroup.class)
    @Valid
    private Address address; // 嵌套对象

    @NotBlank(groups = {AddGroup.class, UpdateGroup.class})
    private String phoneNumber;
}
```

- 在 **创建时**，需要校验 `address` 和 `phoneNumber`。
- 在 **更新时**，只需要校验 `phoneNumber`。

##### 3.3 **多阶段校验**
在一些复杂场景中，可能需要校验多个阶段。例如，首先进行基础字段的校验，然后进行其他属性的校验。可以通过 `@GroupSequence` 配合不同的分组来控制校验顺序。

```java
@GroupSequence({BaseGroup.class, AdvancedGroup.class})
public interface DefaultGroup { }
```

#### 4. **总结**

分组校验允许你根据不同的操作场景，灵活地控制哪些校验规则生效。使用分组校验能够帮助你避免重复的校验逻辑，使得代码更加简洁、可维护。常见的场景包括 **创建**、**更新**、**删除** 操作，或者处理不同的业务流程时需要的不同校验规则。

**关键点总结：**
- 定义不同的校验组接口。
- 在字段上使用 `groups` 属性指定校验组。
- 使用 `@Validated` 指定要触发的校验组。
- `@GroupSequence` 可以定义校验顺序。



### 自定义注解效验

自定义注解校验是 Java Bean Validation（JSR 303/JSR 380）的一种扩展方式，允许开发者根据业务需求编写自己的校验规则。通过自定义注解和校验器，可以实现更加灵活的校验逻辑。

#### 1. **自定义注解校验的基本流程**

自定义校验主要分为两步：
1. **定义校验注解**：创建一个自定义注解，并指定校验规则。
2. **创建校验器（ConstraintValidator）**：实现 `ConstraintValidator` 接口，编写具体的校验逻辑。

#### 2. **自定义注解校验的步骤**

##### 2.1 定义校验注解

自定义注解需要使用 `@Constraint` 注解来标识，这样才会被作为校验注解识别。并且需要指定 `validatedBy` 属性，指定校验器（`ConstraintValidator`）。

```java
import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MyCustomValidator.class)  // 指定校验器
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomConstraint {
    String message() default "自定义校验失败";  // 默认提示消息
    Class<?>[] groups() default {};  // 分组校验
    Class<? extends Payload>[] payload() default {};  // 可选的元数据，用于校验逻辑
}
```

- `@Constraint(validatedBy = MyCustomValidator.class)`：指定校验器，表示实际的校验逻辑将由 `MyCustomValidator` 来完成。
- `message`：自定义校验失败时的消息提示。
- `groups`：分组校验（可选）。
- `payload`：元数据（可选，通常用于扩展，校验器可以使用它来携带额外的信息）。

##### 2.2 创建校验器

创建校验器类并实现 `ConstraintValidator` 接口，负责校验注解应用的字段是否符合要求。校验器需要实现 `initialize` 和 `isValid` 方法。

```java
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MyCustomValidator implements ConstraintValidator<MyCustomConstraint, String> {

    @Override
    public void initialize(MyCustomConstraint constraintAnnotation) {
        // 初始化方法，通常不需要操作，可以忽略
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // 可以根据需求决定 null 是否有效
        }
        // 例如：校验字符串长度是否大于 5
        return value.length() > 5;
    }
}
```

- `initialize()`：可以用来初始化校验器，通常用于获取注解中的配置。对于简单校验，通常不需要特别操作。
- `isValid()`：执行具体的校验逻辑，返回 `true` 表示校验通过，`false` 表示校验失败。

##### 2.3 在实体类中使用自定义注解

自定义的校验注解可以像其他标准注解一样使用在实体类的字段上：

```java
public class User {

    @MyCustomConstraint(message = "用户名长度必须大于 5 个字符")
    private String username;

    // 其他属性和方法
}
```

#### 3. **自定义校验注解的应用场景**

- **复杂逻辑校验**：当现有的标准校验注解（如 `@NotNull`、`@Size`）无法满足需求时，可以使用自定义校验注解实现复杂的业务校验。
- **跨字段校验**：有时需要对多个字段进行联合校验，可以通过自定义注解来实现跨字段的验证。
- **灵活的业务规则**：可以根据具体的业务需求，灵活地设计校验规则，比如某些字段需要符合特定的格式、规则，或者与其他字段存在某种关联。

#### 4. **扩展：跨字段校验**

有时校验规则涉及多个字段的关系，标准的单字段校验注解无法满足需求。这时可以通过自定义注解和校验器来实现跨字段校验。

##### 4.1 自定义注解定义

定义一个跨字段校验注解：

```java
@Constraint(validatedBy = UserFieldsValidator.class)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUser {
    String message() default "用户信息校验失败";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

##### 4.2 校验器（跨字段逻辑）

在校验器中，检查多个字段的关系：

```java
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UserFieldsValidator implements ConstraintValidator<ValidUser, User> {

    @Override
    public void initialize(ValidUser constraintAnnotation) {
    }

    @Override
    public boolean isValid(User user, ConstraintValidatorContext context) {
        // 比如，密码和确认密码要一致
        if (user.getPassword() != null && user.getConfirmPassword() != null) {
            return user.getPassword().equals(user.getConfirmPassword());
        }
        return true;
    }
}
```

##### 4.3 使用跨字段校验注解

```java
@ValidUser
public class User {

    private String password;
    private String confirmPassword;

    // 其他字段和方法
}
```

#### 5. **结合分组校验使用自定义注解**

自定义注解也可以和分组校验一起使用，支持在不同的操作场景下应用不同的校验规则。

```java
@MyCustomConstraint(groups = AddGroup.class)
private String customField;
```

- `groups = AddGroup.class`：只有在添加时才会校验该字段。

#### 6. **总结**

自定义注解校验提供了灵活的校验机制，能够满足复杂业务逻辑的校验需求。通过创建自定义校验注解和校验器，可以解决以下问题：
- 扩展现有校验功能，满足特定的校验需求。
- 实现跨字段或复杂的校验逻辑。
- 灵活控制校验规则，满足不同场景下的要求。

通过自定义注解校验，可以让校验逻辑与业务逻辑解耦，提升代码的可维护性和扩展性。
