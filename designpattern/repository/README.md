# 仓储模式

仓储模式是 Spring Boot 项目中常用的工程实践模式，属于当前设计模式文档体系中的 **Spring Boot 实战补充模式**。它的核心作用是隔离业务层和持久化层，让业务层面向领域对象编程，而不是直接依赖数据库表、Mapper、SQL 或 ORM 细节。

在实际项目中，仓储模式常用于用户账户、订单、商品、库存、会员、支付流水等核心业务对象。它并不等同于 MyBatis Mapper，也不等同于 Spring Data Repository，而是业务层访问领域对象的一层抽象。

## 基础配置

本示例基于 **JDK 21 + Spring Boot 3 + MyBatis-Plus + MySQL**。MyBatis-Plus 在 Spring Boot 3 项目中应使用 `mybatis-plus-spring-boot3-starter`，Maven Central 当前可查询到该 Spring Boot 3 Starter 版本，例如 `3.5.16`。([Maven Central](https://central.sonatype.com/artifact/com.baomidou/mybatis-plus-spring-boot3-starter?utm_source=chatgpt.com))

示例业务使用“用户账户”作为领域对象。业务层只依赖 `UserAccountRepository`，不直接依赖 `UserAccountMapper`，从而把业务规则和持久化实现隔离开。

### 项目依赖

文件位置：`pom.xml`

下面配置 Web、Validation、MyBatis-Plus、MySQL、Hutool 和 Lombok。MyBatis-Plus 用于简化 CRUD，Hutool 用于字符串、ID、对象判断等常用处理。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于接口请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter：简化 MyBatis 持久化开发 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.16</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、ID、对象、集合等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 应用配置

文件位置：`src/main/resources/application.yml`

下面配置 MySQL 数据源、MyBatis-Plus 日志和下划线映射。真实项目中应把数据库账号密码放到环境变量或配置中心。

```yaml
spring:
  application:
    name: design-pattern-repository

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/design_pattern?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

server:
  port: 8080

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true  # 开启下划线字段到驼峰属性的自动映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境打印 SQL，生产环境建议关闭
  global-config:
    db-config:
      id-type: input  # 示例中使用业务 ID，由代码生成后写入数据库
```

### 数据库表

文件位置：`sql/user_account.sql`

该表用于保存用户账户数据。领域对象 `UserAccount` 不直接绑定数据库注解，数据库映射由基础设施层的 `UserAccountDO` 承担。

```sql
CREATE TABLE user_account (
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    status VARCHAR(32) NOT NULL COMMENT '账户状态：NORMAL-正常，FROZEN-冻结',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_user_account_user_id (user_id)
) COMMENT = '用户账户表';
```

### 文件结构

仓储模式建议把领域对象、仓储接口和持久化实现分开。业务层依赖 `domain.repository` 中的接口，基础设施层负责使用 MyBatis-Plus 实现接口。

```text
src/main/java/io/github/atengk/repository
├── RepositoryApplication.java
├── controller
│   └── UserAccountController.java
├── domain
│   ├── model
│   │   └── UserAccount.java
│   └── repository
│       └── UserAccountRepository.java
├── dto
│   ├── CreateUserAccountRequest.java
│   └── ChangeBalanceRequest.java
├── infrastructure
│   ├── converter
│   │   └── UserAccountConverter.java
│   ├── entity
│   │   └── UserAccountDO.java
│   ├── mapper
│   │   └── UserAccountMapper.java
│   └── repository
│       └── MybatisUserAccountRepository.java
├── service
│   ├── UserAccountService.java
│   └── impl
│       └── UserAccountServiceImpl.java
└── vo
    └── UserAccountVO.java
```

## 模式说明

仓储模式的重点不是“写一个 Mapper”，而是为业务层提供一个类似集合的对象访问入口。

普通分层中，Service 往往直接依赖 Mapper：

```text
Controller
 -> Service
   -> Mapper
     -> Database
```

使用仓储模式后，Service 依赖仓储接口，Mapper 被隐藏在仓储实现内部：

```text
Controller
 -> Service
   -> Repository Interface
     -> Repository Implementation
       -> Mapper
         -> Database
```

这样做的好处是业务层不需要关心数据来自 MySQL、Redis、ES、远程接口还是本地缓存。后续即使持久化方式变化，业务层代码也可以保持稳定。

## 核心代码

这一节给出仓储模式的完整关键代码。示例重点体现三点：

```text
1. 领域对象 UserAccount 不直接依赖 MyBatis-Plus 注解。
2. 业务层 UserAccountService 只依赖 UserAccountRepository 接口。
3. 基础设施层 MybatisUserAccountRepository 负责调用 Mapper 并完成 DO 与领域对象转换。
```

### 启动类

文件位置：`src/main/java/io/github/atengk/repository/RepositoryApplication.java`

启动类用于启动 Spring Boot 项目，并扫描 MyBatis Mapper。

```java
package io.github.atengk.repository;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 仓储模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@MapperScan("io.github.atengk.repository.infrastructure.mapper")
@SpringBootApplication
public class RepositoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepositoryApplication.class, args);
    }

}
```

### 领域对象

文件位置：`src/main/java/io/github/atengk/repository/domain/model/UserAccount.java`

该类是业务层使用的领域对象，包含账户创建、余额变更、冻结和解冻等业务行为。它不直接使用 `@TableName`、`@TableId` 等持久化注解。

```java
package io.github.atengk.repository.domain.model;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户账户领域对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public class UserAccount {

    private final String accountId;

    private final String userId;

    private String username;

    private BigDecimal balance;

    private String status;

    private final LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserAccount(String accountId,
                        String userId,
                        String username,
                        BigDecimal balance,
                        String status,
                        LocalDateTime createTime,
                        LocalDateTime updateTime) {
        this.accountId = accountId;
        this.userId = userId;
        this.username = username;
        this.balance = balance;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 创建新用户账户
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return 用户账户领域对象
     */
    public static UserAccount create(String userId, String username) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        return new UserAccount(
                IdUtil.fastSimpleUUID(),
                userId,
                username,
                BigDecimal.ZERO,
                "NORMAL",
                now,
                now
        );
    }

    /**
     * 从持久化数据还原领域对象
     *
     * @param accountId 账户ID
     * @param userId 用户ID
     * @param username 用户名
     * @param balance 账户余额
     * @param status 账户状态
     * @param createTime 创建时间
     * @param updateTime 更新时间
     * @return 用户账户领域对象
     */
    public static UserAccount restore(String accountId,
                                      String userId,
                                      String username,
                                      BigDecimal balance,
                                      String status,
                                      LocalDateTime createTime,
                                      LocalDateTime updateTime) {
        return new UserAccount(accountId, userId, username, balance, status, createTime, updateTime);
    }

    /**
     * 变更账户余额
     *
     * @param amount 变更金额，正数表示增加，负数表示扣减
     */
    public void changeBalance(BigDecimal amount) {
        if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) {
            throw new IllegalArgumentException("变更金额不能为空或0");
        }
        if (isFrozen()) {
            throw new IllegalStateException("账户已冻结，不能变更余额");
        }

        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("账户余额不足");
        }

        this.balance = newBalance;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 冻结账户
     */
    public void freeze() {
        if (isFrozen()) {
            return;
        }
        this.status = "FROZEN";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 解冻账户
     */
    public void unfreeze() {
        if (!isFrozen()) {
            return;
        }
        this.status = "NORMAL";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 判断账户是否冻结
     *
     * @return true 表示冻结
     */
    public boolean isFrozen() {
        return StrUtil.equals("FROZEN", this.status);
    }

}
```

### 仓储接口

文件位置：`src/main/java/io/github/atengk/repository/domain/repository/UserAccountRepository.java`

仓储接口属于领域层或业务抽象层。它描述业务需要什么数据访问能力，不描述 SQL 如何写。

```java
package io.github.atengk.repository.domain.repository;

import io.github.atengk.repository.domain.model.UserAccount;

import java.util.Optional;

/**
 * 用户账户仓储接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserAccountRepository {

    /**
     * 保存用户账户
     *
     * @param userAccount 用户账户领域对象
     * @return 保存后的用户账户领域对象
     */
    UserAccount save(UserAccount userAccount);

    /**
     * 根据账户ID查询账户
     *
     * @param accountId 账户ID
     * @return 用户账户
     */
    Optional<UserAccount> findByAccountId(String accountId);

    /**
     * 根据用户ID查询账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    Optional<UserAccount> findByUserId(String userId);

    /**
     * 判断用户是否已经存在账户
     *
     * @param userId 用户ID
     * @return true 表示已存在
     */
    boolean existsByUserId(String userId);

}
```

### 持久化对象 DO

文件位置：`src/main/java/io/github/atengk/repository/infrastructure/entity/UserAccountDO.java`

该类负责和数据库表映射。它属于基础设施层，不应该泄露到 Controller 或业务规则代码中。

```java
package io.github.atengk.repository.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户账户持久化对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@TableName("user_account")
public class UserAccountDO {

    @TableId(type = IdType.INPUT)
    private String accountId;

    private String userId;

    private String username;

    private BigDecimal balance;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
```

### MyBatis-Plus Mapper

文件位置：`src/main/java/io/github/atengk/repository/infrastructure/mapper/UserAccountMapper.java`

Mapper 只负责数据库 CRUD，不承载业务规则。业务层不直接注入这个 Mapper。

```java
package io.github.atengk.repository.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.repository.infrastructure.entity.UserAccountDO;

/**
 * 用户账户 Mapper
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserAccountMapper extends BaseMapper<UserAccountDO> {
}
```

### 转换器

文件位置：`src/main/java/io/github/atengk/repository/infrastructure/converter/UserAccountConverter.java`

转换器负责领域对象和持久化对象之间的转换，避免转换逻辑散落在 Service 或 Repository 实现中。

```java
package io.github.atengk.repository.infrastructure.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.repository.domain.model.UserAccount;
import io.github.atengk.repository.infrastructure.entity.UserAccountDO;

/**
 * 用户账户对象转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class UserAccountConverter {

    private UserAccountConverter() {
    }

    /**
     * 领域对象转换为持久化对象
     *
     * @param userAccount 用户账户领域对象
     * @return 用户账户持久化对象
     */
    public static UserAccountDO toDO(UserAccount userAccount) {
        if (ObjectUtil.isNull(userAccount)) {
            return null;
        }
        return BeanUtil.copyProperties(userAccount, UserAccountDO.class);
    }

    /**
     * 持久化对象转换为领域对象
     *
     * @param userAccountDO 用户账户持久化对象
     * @return 用户账户领域对象
     */
    public static UserAccount toDomain(UserAccountDO userAccountDO) {
        if (ObjectUtil.isNull(userAccountDO)) {
            return null;
        }

        return UserAccount.restore(
                userAccountDO.getAccountId(),
                userAccountDO.getUserId(),
                userAccountDO.getUsername(),
                userAccountDO.getBalance(),
                userAccountDO.getStatus(),
                userAccountDO.getCreateTime(),
                userAccountDO.getUpdateTime()
        );
    }

}
```

### 仓储实现

文件位置：`src/main/java/io/github/atengk/repository/infrastructure/repository/MybatisUserAccountRepository.java`

该类是仓储接口的 MyBatis-Plus 实现。它屏蔽了 Mapper、查询条件和 DO 转换细节。

```java
package io.github.atengk.repository.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.repository.domain.model.UserAccount;
import io.github.atengk.repository.domain.repository.UserAccountRepository;
import io.github.atengk.repository.infrastructure.converter.UserAccountConverter;
import io.github.atengk.repository.infrastructure.entity.UserAccountDO;
import io.github.atengk.repository.infrastructure.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的用户账户仓储实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MybatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;

    /**
     * 保存用户账户
     *
     * @param userAccount 用户账户领域对象
     * @return 保存后的用户账户领域对象
     */
    @Override
    public UserAccount save(UserAccount userAccount) {
        if (ObjectUtil.isNull(userAccount)) {
            throw new IllegalArgumentException("用户账户不能为空");
        }

        UserAccountDO userAccountDO = UserAccountConverter.toDO(userAccount);
        UserAccountDO exists = userAccountMapper.selectById(userAccount.getAccountId());

        if (ObjectUtil.isNull(exists)) {
            userAccountMapper.insert(userAccountDO);
            log.info("新增用户账户成功，accountId={}，userId={}", userAccount.getAccountId(), userAccount.getUserId());
        } else {
            userAccountMapper.updateById(userAccountDO);
            log.info("更新用户账户成功，accountId={}，userId={}", userAccount.getAccountId(), userAccount.getUserId());
        }

        return userAccount;
    }

    /**
     * 根据账户ID查询账户
     *
     * @param accountId 账户ID
     * @return 用户账户
     */
    @Override
    public Optional<UserAccount> findByAccountId(String accountId) {
        if (StrUtil.isBlank(accountId)) {
            return Optional.empty();
        }

        UserAccountDO userAccountDO = userAccountMapper.selectById(accountId);
        return Optional.ofNullable(UserAccountConverter.toDomain(userAccountDO));
    }

    /**
     * 根据用户ID查询账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Optional.empty();
        }

        UserAccountDO userAccountDO = userAccountMapper.selectOne(
                Wrappers.<UserAccountDO>lambdaQuery()
                        .eq(UserAccountDO::getUserId, userId)
                        .last("limit 1")
        );

        return Optional.ofNullable(UserAccountConverter.toDomain(userAccountDO));
    }

    /**
     * 判断用户是否已经存在账户
     *
     * @param userId 用户ID
     * @return true 表示已存在
     */
    @Override
    public boolean existsByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }

        Long count = userAccountMapper.selectCount(
                Wrappers.<UserAccountDO>lambdaQuery()
                        .eq(UserAccountDO::getUserId, userId)
        );

        return count != null && count > 0;
    }

}
```

### 请求 DTO

文件位置：`src/main/java/io/github/atengk/repository/dto/CreateUserAccountRequest.java`

该 DTO 用于创建用户账户。

```java
package io.github.atengk.repository.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建用户账户请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CreateUserAccountRequest(

        @NotBlank(message = "用户ID不能为空")
        String userId,

        @NotBlank(message = "用户名不能为空")
        String username

) {
}
```

文件位置：`src/main/java/io/github/atengk/repository/dto/ChangeBalanceRequest.java`

该 DTO 用于账户余额变更。`amount` 为正数表示增加余额，为负数表示扣减余额。

```java
package io.github.atengk.repository.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 账户余额变更请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record ChangeBalanceRequest(

        @NotNull(message = "变更金额不能为空")
        BigDecimal amount

) {
}
```

### 响应 VO

文件位置：`src/main/java/io/github/atengk/repository/vo/UserAccountVO.java`

该 VO 用于接口返回，避免直接把领域对象暴露给外部调用方。

```java
package io.github.atengk.repository.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户账户响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserAccountVO(

        String accountId,

        String userId,

        String username,

        BigDecimal balance,

        String status,

        LocalDateTime createTime,

        LocalDateTime updateTime

) {
}
```

### Service 接口

文件位置：`src/main/java/io/github/atengk/repository/service/UserAccountService.java`

该接口定义账户业务能力。外部接口层不直接感知仓储实现。

```java
package io.github.atengk.repository.service;

import io.github.atengk.repository.dto.ChangeBalanceRequest;
import io.github.atengk.repository.dto.CreateUserAccountRequest;
import io.github.atengk.repository.vo.UserAccountVO;

/**
 * 用户账户业务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserAccountService {

    /**
     * 创建用户账户
     *
     * @param request 创建用户账户请求
     * @return 用户账户响应
     */
    UserAccountVO createAccount(CreateUserAccountRequest request);

    /**
     * 查询用户账户
     *
     * @param accountId 账户ID
     * @return 用户账户响应
     */
    UserAccountVO getAccount(String accountId);

    /**
     * 变更账户余额
     *
     * @param accountId 账户ID
     * @param request 余额变更请求
     * @return 用户账户响应
     */
    UserAccountVO changeBalance(String accountId, ChangeBalanceRequest request);

}
```

### Service 实现

文件位置：`src/main/java/io/github/atengk/repository/service/impl/UserAccountServiceImpl.java`

该实现类只依赖 `UserAccountRepository`，不直接依赖 `UserAccountMapper`。这就是仓储模式在业务层的关键体现。

```java
package io.github.atengk.repository.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.repository.domain.model.UserAccount;
import io.github.atengk.repository.domain.repository.UserAccountRepository;
import io.github.atengk.repository.dto.ChangeBalanceRequest;
import io.github.atengk.repository.dto.CreateUserAccountRequest;
import io.github.atengk.repository.service.UserAccountService;
import io.github.atengk.repository.vo.UserAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户账户业务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;

    /**
     * 创建用户账户
     *
     * @param request 创建用户账户请求
     * @return 用户账户响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccountVO createAccount(CreateUserAccountRequest request) {
        if (userAccountRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("该用户已经存在账户");
        }

        UserAccount userAccount = UserAccount.create(request.userId(), request.username());
        UserAccount savedAccount = userAccountRepository.save(userAccount);

        log.info("创建用户账户完成，accountId={}，userId={}", savedAccount.getAccountId(), savedAccount.getUserId());
        return toVO(savedAccount);
    }

    /**
     * 查询用户账户
     *
     * @param accountId 账户ID
     * @return 用户账户响应
     */
    @Override
    public UserAccountVO getAccount(String accountId) {
        UserAccount userAccount = userAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("用户账户不存在"));

        return toVO(userAccount);
    }

    /**
     * 变更账户余额
     *
     * @param accountId 账户ID
     * @param request 余额变更请求
     * @return 用户账户响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccountVO changeBalance(String accountId, ChangeBalanceRequest request) {
        UserAccount userAccount = userAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("用户账户不存在"));

        userAccount.changeBalance(request.amount());
        UserAccount savedAccount = userAccountRepository.save(userAccount);

        log.info("账户余额变更完成，accountId={}，amount={}，balance={}",
                savedAccount.getAccountId(), request.amount(), savedAccount.getBalance());

        return toVO(savedAccount);
    }

    /**
     * 领域对象转换为响应对象
     *
     * @param userAccount 用户账户领域对象
     * @return 用户账户响应对象
     */
    private UserAccountVO toVO(UserAccount userAccount) {
        if (ObjectUtil.isNull(userAccount)) {
            return null;
        }

        return new UserAccountVO(
                userAccount.getAccountId(),
                userAccount.getUserId(),
                userAccount.getUsername(),
                userAccount.getBalance(),
                userAccount.getStatus(),
                userAccount.getCreateTime(),
                userAccount.getUpdateTime()
        );
    }

}
```

### Controller 接口

文件位置：`src/main/java/io/github/atengk/repository/controller/UserAccountController.java`

该 Controller 提供账户创建、查询和余额变更接口。

```java
package io.github.atengk.repository.controller;

import io.github.atengk.repository.dto.ChangeBalanceRequest;
import io.github.atengk.repository.dto.CreateUserAccountRequest;
import io.github.atengk.repository.service.UserAccountService;
import io.github.atengk.repository.vo.UserAccountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户账户接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    /**
     * 创建用户账户
     *
     * @param request 创建用户账户请求
     * @return 用户账户响应
     */
    @PostMapping
    public UserAccountVO createAccount(@Valid @RequestBody CreateUserAccountRequest request) {
        return userAccountService.createAccount(request);
    }

    /**
     * 查询用户账户
     *
     * @param accountId 账户ID
     * @return 用户账户响应
     */
    @GetMapping("/{accountId}")
    public UserAccountVO getAccount(@PathVariable String accountId) {
        return userAccountService.getAccount(accountId);
    }

    /**
     * 变更账户余额
     *
     * @param accountId 账户ID
     * @param request 余额变更请求
     * @return 用户账户响应
     */
    @PostMapping("/{accountId}/balance")
    public UserAccountVO changeBalance(@PathVariable String accountId,
                                        @Valid @RequestBody ChangeBalanceRequest request) {
        return userAccountService.changeBalance(accountId, request);
    }

}
```

## 使用方式

本示例提供三个接口：创建账户、查询账户、变更余额。调用链中，Controller 调用 Service，Service 调用 Repository 接口，Repository 实现再调用 MyBatis-Plus Mapper。

### 创建账户

接口信息：

| 项目     | 内容         |
| -------- | ------------ |
| 请求路径 | `/accounts`  |
| 请求方法 | `POST`       |
| 主要作用 | 创建用户账户 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/accounts' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "10001",
    "username": "张三"
  }'
```

响应示例：

```json
{
  "accountId": "5f4dcc3b5aa765d61d8327deb882cf99",
  "userId": "10001",
  "username": "张三",
  "balance": 0.00,
  "status": "NORMAL",
  "createTime": "2026-05-13T10:30:00",
  "updateTime": "2026-05-13T10:30:00"
}
```

### 查询账户

接口信息：

| 项目     | 内容                    |
| -------- | ----------------------- |
| 请求路径 | `/accounts/{accountId}` |
| 请求方法 | `GET`                   |
| 主要作用 | 根据账户ID查询账户      |

请求示例：

```bash
curl -X GET 'http://localhost:8080/accounts/5f4dcc3b5aa765d61d8327deb882cf99'
```

### 增加余额

接口信息：

| 项目     | 内容                            |
| -------- | ------------------------------- |
| 请求路径 | `/accounts/{accountId}/balance` |
| 请求方法 | `POST`                          |
| 主要作用 | 增加或扣减账户余额              |

增加余额请求示例：

```bash
curl -X POST 'http://localhost:8080/accounts/5f4dcc3b5aa765d61d8327deb882cf99/balance' \
  -H 'Content-Type: application/json' \
  -d '{
    "amount": 100.00
  }'
```

扣减余额请求示例：

```bash
curl -X POST 'http://localhost:8080/accounts/5f4dcc3b5aa765d61d8327deb882cf99/balance' \
  -H 'Content-Type: application/json' \
  -d '{
    "amount": -20.00
  }'
```

## 验证方式

可以通过数据库记录、接口响应和日志验证仓储模式是否生效。

启动项目：

```bash
mvn spring-boot:run
```

创建账户后查询数据库：

```sql
SELECT
    account_id,
    user_id,
    username,
    balance,
    status,
    create_time,
    update_time
FROM user_account
WHERE user_id = '10001';
```

验证点：

```text
1. ServiceImpl 中没有注入 UserAccountMapper。
2. ServiceImpl 只依赖 UserAccountRepository 接口。
3. MybatisUserAccountRepository 内部负责调用 UserAccountMapper。
4. Controller 返回的是 UserAccountVO，而不是 UserAccountDO。
5. UserAccount 领域对象不包含 @TableName、@TableId 等数据库映射注解。
```

如果启动失败，重点检查：

```text
1. 数据库 design_pattern 是否已经创建。
2. user_account 表是否已经执行建表 SQL。
3. application.yml 中数据库账号、密码、地址是否正确。
4. @MapperScan 路径是否和 UserAccountMapper 包路径一致。
5. MyBatis-Plus Starter 是否使用 Spring Boot 3 对应的 artifactId。
```

## 适用场景

仓储模式适合业务对象较重要、业务规则较多、持久化细节容易变化的场景。

常见适用场景：

```text
订单领域：
- 根据订单ID查询订单聚合
- 保存订单主表和订单明细
- 隐藏订单表、明细表、支付表的组合查询细节

用户账户领域：
- 查询账户
- 冻结账户
- 变更余额
- 隐藏数据库表结构和缓存读取逻辑

商品库存领域：
- 查询库存
- 扣减库存
- 回滚库存
- 屏蔽 MySQL、Redis、库存流水表的组合操作

支付流水领域：
- 根据支付单号查询流水
- 保存支付结果
- 防止业务层直接操作支付流水表
```

## 不适用场景

仓储模式会增加一层抽象。对于非常简单的 CRUD 后台管理功能，直接使用 Service + Mapper 可能更直接。

不建议过度使用的场景：

```text
1. 只有简单单表 CRUD，没有明显业务规则。
2. 项目不是领域模型驱动，只是普通数据管理系统。
3. 团队对分层边界没有共识，强行引入会增加理解成本。
4. Repository 只是机械转发 Mapper 方法，没有任何抽象价值。
5. 每张表都无脑创建 Repository，导致类数量膨胀。
```

## 和 Mapper 的区别

仓储模式最容易和 MyBatis Mapper 混淆。二者职责不同。

| 对比项             | Repository                          | Mapper                                  |
| ------------------ | ----------------------------------- | --------------------------------------- |
| 所属层次           | 领域层接口或业务抽象层              | 基础设施层                              |
| 面向对象           | 领域对象、聚合对象                  | 数据库表、SQL 结果                      |
| 关注点             | 业务需要什么数据访问能力            | SQL 如何执行                            |
| 是否暴露给 Service | 推荐暴露 Repository 接口            | 不推荐核心业务直接依赖                  |
| 返回对象           | `UserAccount`、`Order` 等领域对象   | `UserAccountDO`、`OrderDO` 等持久化对象 |
| 是否包含业务语义   | 可以包含业务语义，如 `findByUserId` | 更偏数据库操作                          |

简单理解：

```text
Mapper 是数据库访问工具。
Repository 是业务层访问领域对象的入口。
```

## 和 DAO 的区别

DAO 通常更偏数据访问对象，直接围绕表和 SQL 组织。Repository 更偏领域对象访问，强调屏蔽持久化细节。

| 对比项   | DAO               | Repository                      |
| -------- | ----------------- | ------------------------------- |
| 关注点   | 数据库访问        | 领域对象持久化                  |
| 常见命名 | UserDao、OrderDao | UserRepository、OrderRepository |
| 返回对象 | Entity、DO、Map   | Domain Model、Aggregate         |
| 抽象程度 | 较低              | 较高                            |
| 适合场景 | 数据密集型 CRUD   | 业务规则较多的领域对象          |

在普通 Spring Boot 项目中，如果没有 DDD 分层，也可以把 Repository 理解为比 Mapper 更靠近业务语义的一层数据访问抽象。

## 项目落地建议

在真实项目中使用仓储模式时，应避免把它写成“Mapper 的壳”。仓储接口应该围绕业务对象设计，而不是照搬数据库表操作。

建议：

```text
1. Repository 接口放在 domain.repository 或业务抽象层中。
2. Repository 实现放在 infrastructure.repository 中。
3. Service 依赖 Repository 接口，不直接依赖 Mapper。
4. Mapper、DO、SQL、缓存实现都放在基础设施层。
5. Repository 返回领域对象，不返回数据库 DO。
6. 复杂聚合查询可以在 Repository 内部组合多个 Mapper。
7. 简单后台 CRUD 不必强行套用 Repository。
8. Repository 方法命名应体现业务语义，而不是 SQL 语义。
```

推荐命名：

```text
findByAccountId
findByUserId
save
remove
existsByUserId
findEnabledAccounts
findPendingOrders
```

不推荐命名：

```text
selectOne
selectList
insertDO
updateByWrapper
queryByMap
```

## 常见问题

### Repository 是否可以调用多个 Mapper

可以。Repository 的职责就是屏蔽持久化细节。如果一个订单聚合需要读取订单主表、订单明细表、支付表和物流表，可以由 `OrderRepository` 在内部组合多个 Mapper，然后返回完整的订单领域对象。

### Repository 是否可以使用 Redis

可以。Repository 不限定数据来源。它可以内部先查 Redis，未命中再查 MySQL，也可以保存时同时更新缓存。

例如：

```text
UserAccountRepository
 -> 先查 Redis
 -> 未命中查 MySQL
 -> 转换为 UserAccount
 -> 回写 Redis
 -> 返回领域对象
```

业务层不需要知道数据到底来自 Redis 还是 MySQL。

### 每张表都需要一个 Repository 吗

不需要。Repository 应该围绕业务对象或聚合设计，而不是围绕数据库表设计。

例如订单业务中可能有以下表：

```text
order_main
order_item
order_payment
order_delivery
```

但不一定需要四个 Repository。更常见的是提供一个 `OrderRepository`，内部组合多个 Mapper，向业务层返回完整订单对象。

### Repository 里面能不能写业务规则

一般不建议写核心业务规则。核心业务规则应放在领域对象或 Service 中。Repository 可以包含和数据访问相关的规则，例如是否过滤逻辑删除数据、是否只查询启用状态数据、是否组合缓存和数据库等。

## 总结

仓储模式在 Spring Boot 项目中的核心价值是隔离业务层和持久化层。业务层通过 Repository 操作领域对象，不直接依赖 Mapper、DO、SQL 或数据库表结构。

推荐落地方式：

```text
业务层：
Service -> UserAccountRepository -> UserAccount

基础设施层：
MybatisUserAccountRepository -> UserAccountMapper -> user_account 表
```

当项目只是简单 CRUD 时，直接使用 Service + Mapper 更简单。当项目存在核心业务对象、复杂聚合、缓存组合、持久化实现变化或 DDD 分层诉求时，仓储模式可以明显提升代码边界清晰度、可维护性和可测试性。
