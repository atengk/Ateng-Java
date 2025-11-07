# 🧬 原型模式（Prototype Pattern）

## 一、模式简介

**原型模式（Prototype Pattern）** 是一种创建型设计模式，它允许对象通过“克隆（拷贝）”的方式创建新的实例，而不是通过 `new` 操作符。
 这种方式非常适合在对象创建成本较高、或对象包含复杂状态时使用，能够提高性能、简化初始化逻辑。

在 Spring Boot 项目中，原型模式可以用于：

- 深拷贝配置对象、DTO、VO；
- 复制复杂的业务实体；
- 在缓存、模板对象等场景下快速创建对象副本。

------

## 二、实现目标

在本示例中，我们模拟一个“用户模板克隆”的场景：
 系统中定义了一个用户模板对象，克隆出多个不同的用户副本，用于批量生成测试数据或初始化场景。

------

## 三、示例结构

```
io.github.atengk
└── designpattern
    └── prototype
        ├── PrototypeApplication.java              # 启动类
        ├── model
        │   └── UserTemplate.java                  # 原型对象类（实现 Cloneable）
        ├── service
        │   ├── PrototypeService.java              # 克隆服务接口
        │   └── PrototypeServiceImpl.java          # 克隆服务实现
        └── controller
            └── PrototypeController.java            # 测试入口控制器
```

------

## 四、代码实现

### 1. 原型对象类：`UserTemplate.java`

```java
package io.github.atengk.designpattern.prototype.model;

import java.io.Serializable;

/**
 * 用户模板类，支持克隆操作
 * 实现 Cloneable 接口以支持浅拷贝
 */
public class UserTemplate implements Cloneable, Serializable {

    private String username;
    private String role;
    private String email;

    public UserTemplate(String username, String role, String email) {
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    /**
     * 克隆当前对象
     */
    @Override
    public UserTemplate clone() {
        try {
            return (UserTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }

    @Override
    public String toString() {
        return "UserTemplate{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
```

------

### 2. 克隆服务接口：`PrototypeService.java`

```java
package io.github.atengk.designpattern.prototype.service;

import io.github.atengk.designpattern.prototype.model.UserTemplate;

/**
 * 原型克隆服务接口
 */
public interface PrototypeService {

    /**
     * 克隆用户模板并自定义用户名
     *
     * @param username 用户名
     * @return 新的用户对象
     */
    UserTemplate cloneUser(String username);
}
```

------

### 3. 克隆服务实现类：`PrototypeServiceImpl.java`

```java
package io.github.atengk.designpattern.prototype.service;

import io.github.atengk.designpattern.prototype.model.UserTemplate;
import org.springframework.stereotype.Service;

/**
 * 原型克隆服务实现类
 * 模拟从模板克隆多个用户
 */
@Service
public class PrototypeServiceImpl implements PrototypeService {

    /** 模拟系统中的“原型模板对象” */
    private final UserTemplate userTemplate = new UserTemplate("TemplateUser", "Admin", "template@example.com");

    @Override
    public UserTemplate cloneUser(String username) {
        // 克隆模板对象
        UserTemplate clonedUser = userTemplate.clone();
        // 修改克隆体的个别属性
        return new UserTemplate(username, clonedUser.getRole(), username.toLowerCase() + "@example.com");
    }
}
```

------

### 4. 控制层测试：`PrototypeController.java`

```java
package io.github.atengk.designpattern.prototype.controller;

import io.github.atengk.designpattern.prototype.model.UserTemplate;
import io.github.atengk.designpattern.prototype.service.PrototypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 原型模式测试控制器
 */
@RestController
public class PrototypeController {

    private final PrototypeService prototypeService;

    public PrototypeController(PrototypeService prototypeService) {
        this.prototypeService = prototypeService;
    }

    @GetMapping("/prototype/test")
    public Object testPrototype() {
        return Stream.of("Alice", "Bob", "Charlie")
                .map(prototypeService::cloneUser)
                .collect(Collectors.toList());
    }
}
```

------

### 5. 启动类：`PrototypeApplication.java`

```java
package io.github.atengk.designpattern.prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 原型模式示例启动类
 */
@SpringBootApplication
public class PrototypeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrototypeApplication.class, args);
    }
}
```

------

## 五、测试结果

访问接口：

```
GET http://localhost:8080/prototype/test
```

返回结果示例：

```json
[
  {"username":"Alice","role":"Admin","email":"alice@example.com"},
  {"username":"Bob","role":"Admin","email":"bob@example.com"},
  {"username":"Charlie","role":"Admin","email":"charlie@example.com"}
]
```

可以看到，系统从一个模板用户对象中克隆出了多个用户副本，并且只改动了个别属性。

------

## 六、适用场景

- 需要频繁创建相似对象的情况；
- 对象创建成本较高；
- 需要动态复制复杂对象结构；
- 需要避免暴露复杂的构造逻辑。

------

## 七、总结

| 项目         | 内容                                   |
| ------------ | -------------------------------------- |
| **核心思想** | 用复制（克隆）代替 `new` 创建对象      |
| **优点**     | 降低对象创建成本、隐藏复杂构造过程     |
| **缺点**     | 深拷贝实现复杂；容易与引用类型共享问题 |
| **典型应用** | 原型缓存、对象池、DTO 模板、文档复制等 |

