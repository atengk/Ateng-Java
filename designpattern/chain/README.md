## 🪢 责任链模式（Chain of Responsibility Pattern）

------

### 📘 模式简介

**责任链模式（Chain of Responsibility Pattern）** 是一种行为型设计模式。
 它允许将多个处理逻辑（Handler）串联成一条“链”，
 请求在链上传递，每个处理器可以决定：

- 是否处理当前请求；
- 是否将请求继续传递给下一个处理器。

在实际项目中非常常见，比如：

- 审批流程（多级审批）；
- 参数校验链；
- 日志过滤；
- 统一请求拦截；
- 支付流程等。

> 简而言之：
>  “责任链模式就是一个请求经过一串处理者，每个处理者各司其职，灵活解耦。”

------

## 💡 实战案例：用户注册多级校验链

场景说明：
 用户注册前，需要进行一系列校验：

1. 用户名不能为空
2. 邮箱格式合法
3. 用户名不能重复

我们希望通过责任链模式来优雅地实现这些校验逻辑，而不是在 Controller 中堆满 if 判断。

------

### 🏗️ 项目结构

```
io.github.atengk.design.chain
 ├── controller
 │    └── RegisterController.java
 ├── chain
 │    ├── AbstractHandler.java
 │    ├── UsernameNotEmptyHandler.java
 │    ├── EmailFormatHandler.java
 │    └── UsernameUniqueHandler.java
 ├── service
 │    └── UserService.java
 ├── model
 │    └── User.java
 └── config
      └── HandlerChainConfig.java
```

------

### 🧩 1. 用户实体类

```java
package io.github.atengk.design.chain.model;

/**
 * 用户实体类
 */
public class User {
    private String username;
    private String email;

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
```

------

### ⚙️ 2. 定义抽象处理器（Handler）

```java
package io.github.atengk.design.chain.chain;

import io.github.atengk.design.chain.model.User;

/**
 * 抽象处理器
 * 定义处理请求的标准方法及责任传递机制
 */
public abstract class AbstractHandler {

    /** 下一个处理器 */
    protected AbstractHandler next;

    /**
     * 设置下一个处理器
     */
    public AbstractHandler setNext(AbstractHandler next) {
        this.next = next;
        return next;
    }

    /**
     * 执行处理逻辑
     * @param user 用户信息
     */
    public void handle(User user) {
        doHandle(user);
        if (next != null) {
            next.handle(user);
        }
    }

    /**
     * 每个处理器的具体逻辑
     */
    protected abstract void doHandle(User user);
}
```

------

### 🧱 3. 具体处理器实现

#### ① 用户名非空校验

```java
package io.github.atengk.design.chain.chain;

import io.github.atengk.design.chain.model.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 校验用户名不能为空
 */
@Component
public class UsernameNotEmptyHandler extends AbstractHandler {

    @Override
    protected void doHandle(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        System.out.println("【校验通过】用户名不为空");
    }
}
```

#### ② 邮箱格式校验

```java
package io.github.atengk.design.chain.chain;

import io.github.atengk.design.chain.model.User;
import org.springframework.stereotype.Component;

/**
 * 校验邮箱格式合法
 */
@Component
public class EmailFormatHandler extends AbstractHandler {

    @Override
    protected void doHandle(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        System.out.println("【校验通过】邮箱格式合法");
    }
}
```

#### ③ 用户名唯一校验

```java
package io.github.atengk.design.chain.chain;

import io.github.atengk.design.chain.model.User;
import io.github.atengk.design.chain.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 校验用户名不能重复
 */
@Component
public class UsernameUniqueHandler extends AbstractHandler {

    private final UserService userService;

    public UsernameUniqueHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doHandle(User user) {
        if (userService.exists(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        System.out.println("【校验通过】用户名未重复");
    }
}
```

------

### 🧰 4. 模拟用户服务

```java
package io.github.atengk.design.chain.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 模拟用户服务
 */
@Service
public class UserService {

    private static final Set<String> EXISTING_USERS = new HashSet<>();

    public boolean exists(String username) {
        return EXISTING_USERS.contains(username);
    }

    public void save(String username) {
        EXISTING_USERS.add(username);
    }
}
```

------

### 🧩 5. 责任链配置类（将处理器按顺序组装）

```java
package io.github.atengk.design.chain.config;

import io.github.atengk.design.chain.chain.EmailFormatHandler;
import io.github.atengk.design.chain.chain.UsernameNotEmptyHandler;
import io.github.atengk.design.chain.chain.UsernameUniqueHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 责任链配置
 */
@Configuration
public class HandlerChainConfig {

    @Bean
    public UsernameNotEmptyHandler handlerChain(UsernameNotEmptyHandler h1,
                                                EmailFormatHandler h2,
                                                UsernameUniqueHandler h3) {
        // 按顺序组装责任链
        h1.setNext(h2).setNext(h3);
        return h1;
    }
}
```

------

### 🧭 6. 控制器调用

```java
package io.github.atengk.design.chain.controller;

import io.github.atengk.design.chain.chain.UsernameNotEmptyHandler;
import io.github.atengk.design.chain.model.User;
import io.github.atengk.design.chain.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户注册控制器
 */
@RestController
@RequestMapping("/chain")
public class RegisterController {

    private final UsernameNotEmptyHandler handlerChain;
    private final UserService userService;

    public RegisterController(UsernameNotEmptyHandler handlerChain, UserService userService) {
        this.handlerChain = handlerChain;
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        handlerChain.handle(user);
        userService.save(user.getUsername());
        return "注册成功：" + user.getUsername();
    }
}
```

------

### 🚀 7. 运行测试

**请求示例：**

```bash
POST http://localhost:8080/chain/register
Content-Type: application/json

{
  "username": "atengk",
  "email": "test@example.com"
}
```

**控制台输出：**

```
【校验通过】用户名不为空
【校验通过】邮箱格式合法
【校验通过】用户名未重复
```

返回结果：

```
注册成功：atengk
```

------

### 🧭 总结

| 角色                                                         | 说明                           |
| ------------------------------------------------------------ | ------------------------------ |
| `AbstractHandler`                                            | 抽象处理器，定义责任链传递机制 |
| `UsernameNotEmptyHandler`、`EmailFormatHandler`、`UsernameUniqueHandler` | 具体处理器                     |
| `HandlerChainConfig`                                         | 责任链组装器                   |
| `RegisterController`                                         | 客户端（Client）调用入口       |

**优点：**

- 各处理逻辑独立、易于维护；
- 可自由组合链条顺序；
- 满足开闭原则，新增规则仅需新增处理器类。
