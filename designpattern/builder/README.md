# 🧱 构建者模式（Builder Pattern）

------

### 📘 模式简介

**构建者模式（Builder Pattern）** 是一种创建型设计模式，
 用于一步步构建一个复杂对象，将对象的**构建过程**与**表示**分离。

相比直接使用构造函数或工厂方法，构建者模式更适合：

- 对象参数较多；
- 部分字段可选；
- 对象组合逻辑复杂；
- 希望保证创建的对象始终处于有效状态的场景。

> 通俗解释：
>  “构建者模式就像点外卖——菜品是固定的，但你可以自由选择加料、加辣、加饭，最后统一制作成成品。”

------

## 💡 实战案例：构建一个复杂的用户对象（带构建流程）

场景说明：
 在用户注册系统中，一个 `UserProfile` 对象包含多个属性：

- 基础信息（用户名、邮箱）
- 可选信息（地址、电话、年龄）
- 状态信息（是否启用、是否管理员）

这些字段不是每次都有，因此用构建者模式来更优雅地创建对象。

------

### 🏗️ 项目结构

```
io.github.atengk.design.builder
 ├── model
 │    └── UserProfile.java
 ├── service
 │    └── UserProfileService.java
 └── controller
      └── UserProfileController.java
```

------

### 🧩 1. 用户实体类（最终对象）

```java
package io.github.atengk.model;

/**
 * 用户档案实体类
 */
public class UserProfile {

    private final String username;
    private final String email;
    private final String address;
    private final String phone;
    private final Integer age;
    private final boolean active;
    private final boolean admin;

    /**
     * 构造函数设为私有，禁止外部直接创建
     */
    private UserProfile(UserProfileBuilder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.address = builder.address;
        this.phone = builder.phone;
        this.age = builder.age;
        this.active = builder.active;
        this.admin = builder.admin;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAge() {
        return age;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", age=" + age +
                ", active=" + active +
                ", admin=" + admin +
                '}';
    }

    public static UserProfile.UserProfileBuilder builder() {
        return new UserProfile.UserProfileBuilder();
    }

    /**
     * 内部构建者类
     */
    public static class UserProfileBuilder {
        private String username;
        private String email;
        private String address;
        private String phone;
        private Integer age;
        private boolean active = true;
        private boolean admin = false;

        public UserProfileBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserProfileBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileBuilder address(String address) {
            this.address = address;
            return this;
        }

        public UserProfileBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserProfileBuilder age(Integer age) {
            this.age = age;
            return this;
        }

        public UserProfileBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public UserProfileBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * 构建最终对象
         */
        public UserProfile build() {
            if (username == null || email == null) {
                throw new IllegalArgumentException("用户名和邮箱不能为空");
            }
            return new UserProfile(this);
        }
    }
}
```

------

### 🧱 2. 业务服务类

```java
package io.github.atengk.design.builder.service;

import io.github.atengk.design.builder.model.UserProfile;
import org.springframework.stereotype.Service;

/**
 * 用户档案业务逻辑
 */
@Service
public class UserProfileService {

    public String register(UserProfile userProfile) {
        System.out.println("【UserProfileService】保存用户信息：" + userProfile);
        return "注册成功：" + userProfile.getUsername();
    }
}
```

------

### 🧭 3. 控制器层示例

```java
package io.github.atengk.design.builder.controller;

import io.github.atengk.design.builder.model.UserProfile;
import io.github.atengk.design.builder.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户档案控制器
 */
@RestController
@RequestMapping("/builder")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) Integer age) {

        // 使用构建者模式创建复杂对象
        UserProfile userProfile = new UserProfile.UserProfileBuilder()
                .username(username)
                .email(email)
                .address(address)
                .phone(phone)
                .age(age)
                .active(true)
                .admin(false)
                .build();

        return userProfileService.register(userProfile);
    }
}
```

------

### 🚀 4. 运行测试

请求：

```bash
POST http://localhost:8080/builder/register?username=atengk&email=test@example.com&age=28&address=Tokyo
```

控制台输出：

```
【UserProfileService】保存用户信息：UserProfile{username='atengk', email='test@example.com', address='Tokyo', phone='null', age=28, active=true, admin=false}
```

返回结果：

```
注册成功：atengk
```

------

### 🧭 总结

| 角色                    | 说明                             |
| ----------------------- | -------------------------------- |
| `UserProfile`           | 产品对象（Product）              |
| `UserProfileBuilder`    | 构建者（Builder），负责对象组装  |
| `UserProfileService`    | 使用者（Director），执行业务逻辑 |
| `UserProfileController` | 客户端（Client）                 |

**优点：**

- 对象构建逻辑清晰、链式调用优雅；
- 支持可选参数与默认值；
- 有效避免构造函数参数爆炸问题；
- 满足开闭原则，方便扩展新字段。

