## 🏛️ 外观模式（Facade Pattern）

### 📘 模式简介

**外观模式（Facade Pattern）** 是一种结构型设计模式，
 它为复杂子系统提供一个统一的高层接口，使得子系统更容易被使用。

在实际项目中，外观模式常用于：

- **封装复杂业务流程**（如用户注册、下单、支付等）
- **统一对多个服务的调用接口**
- **降低模块间的耦合度**

> 简而言之：
>  “外观模式就是帮你一次调用多个服务的中间人，让复杂的事情看起来很简单。”

------

## 💡 实战案例：用户注册一键完成流程

场景说明：
 当用户注册系统账号时，需要完成多项业务：

1. 保存用户信息
2. 发送欢迎邮件
3. 赠送新手积分
4. 记录日志

如果直接在 Controller 中逐个调用，会显得逻辑臃肿。
 外观模式可以优雅地封装这一整套流程。

------

### 🏗️ 项目结构

```
io.github.atengk.design.facade
 ├── controller
 │    └── UserController.java
 ├── facade
 │    └── UserRegisterFacade.java
 ├── service
 │    ├── UserService.java
 │    ├── MailService.java
 │    ├── PointService.java
 │    └── LogService.java
 └── model
      └── User.java
```

------

### 🧩 1. 用户实体类

```java
package io.github.atengk.design.facade.model;

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

### ⚙️ 2. 定义四个基础服务类（子系统）

```java
package io.github.atengk.design.facade.service;

import io.github.atengk.design.facade.model.User;
import org.springframework.stereotype.Service;

/**
 * 用户服务：处理用户注册
 */
@Service
public class UserService {
    public void saveUser(User user) {
        System.out.println("【UserService】用户注册成功：" + user.getUsername());
    }
}
package io.github.atengk.design.facade.service;

import io.github.atengk.design.facade.model.User;
import org.springframework.stereotype.Service;

/**
 * 邮件服务：发送欢迎邮件
 */
@Service
public class MailService {
    public void sendWelcomeEmail(User user) {
        System.out.println("【MailService】发送欢迎邮件至：" + user.getEmail());
    }
}
package io.github.atengk.design.facade.service;

import io.github.atengk.design.facade.model.User;
import org.springframework.stereotype.Service;

/**
 * 积分服务：注册赠送积分
 */
@Service
public class PointService {
    public void addRegisterPoints(User user) {
        System.out.println("【PointService】用户 " + user.getUsername() + " 获得新手积分：100");
    }
}
package io.github.atengk.design.facade.service;

import io.github.atengk.design.facade.model.User;
import org.springframework.stereotype.Service;

/**
 * 日志服务：记录操作日志
 */
@Service
public class LogService {
    public void recordRegisterLog(User user) {
        System.out.println("【LogService】记录注册日志：" + user.getUsername());
    }
}
```

------

### 🧱 3. 外观类（Facade）

```java
package io.github.atengk.design.facade.facade;

import io.github.atengk.design.facade.model.User;
import io.github.atengk.design.facade.service.*;
import org.springframework.stereotype.Component;

/**
 * 外观类：对外暴露统一的注册接口
 */
@Component
public class UserRegisterFacade {

    private final UserService userService;
    private final MailService mailService;
    private final PointService pointService;
    private final LogService logService;

    public UserRegisterFacade(UserService userService,
                              MailService mailService,
                              PointService pointService,
                              LogService logService) {
        this.userService = userService;
        this.mailService = mailService;
        this.pointService = pointService;
        this.logService = logService;
    }

    /**
     * 一键注册：封装整个注册流程
     * @param username 用户名
     * @param email 邮箱
     */
    public void register(String username, String email) {
        User user = new User(username, email);
        userService.saveUser(user);
        mailService.sendWelcomeEmail(user);
        pointService.addRegisterPoints(user);
        logService.recordRegisterLog(user);
        System.out.println("【UserRegisterFacade】注册流程完成！");
    }
}
```

------

### 🧭 4. 控制器调用

```java
package io.github.atengk.design.facade.controller;

import io.github.atengk.design.facade.facade.UserRegisterFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册控制器
 */
@RestController
public class UserController {

    private final UserRegisterFacade userRegisterFacade;

    public UserController(UserRegisterFacade userRegisterFacade) {
        this.userRegisterFacade = userRegisterFacade;
    }

    @GetMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email) {
        userRegisterFacade.register(username, email);
        return "用户注册成功！";
    }
}
```

------

### 🚀 5. 运行结果

访问：

```
http://localhost:8080/register?username=atengk&email=test@example.com
```

控制台输出：

```
【UserService】用户注册成功：atengk
【MailService】发送欢迎邮件至：test@example.com
【PointService】用户 atengk 获得新手积分：100
【LogService】记录注册日志：atengk
【UserRegisterFacade】注册流程完成！
```

------

### 🧭 总结

| 角色                                                       | 说明                             |
| ---------------------------------------------------------- | -------------------------------- |
| `UserService`、`MailService`、`PointService`、`LogService` | 子系统（Subsystem）              |
| `UserRegisterFacade`                                       | 外观（Facade），对外暴露统一接口 |
| `UserController`                                           | 客户端（Client），调用外观接口   |

**优点：**

- 简化复杂系统调用，降低模块耦合。
- 统一外部访问入口，方便维护。
- 可灵活扩展内部子系统，不影响外部接口。

