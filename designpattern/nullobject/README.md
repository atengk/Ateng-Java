# 空对象模式

空对象模式是 Spring Boot 项目中常用的工程实践模式，属于当前设计模式文档体系中的 **Spring Boot 实战补充模式**。它的核心作用是用一个安全的默认对象替代 `null`，减少空判断、降低空指针风险，并让调用方可以用统一方式处理正常对象和缺省对象。

在实际项目中，空对象模式常用于用户未登录、查询不到配置、缺省会员信息、默认支付策略、默认通知渠道、空权限集合、默认优惠规则、缺省风控结果等场景。

## 基础配置

本示例基于 **JDK 21 + Spring Boot 3**，使用 Spring Web 提供接口，使用 Hutool 简化字符串、对象和金额处理，使用 Lombok 简化日志和构造器代码。

示例业务以“会员权益展示”为场景：

```text
1. 如果用户存在会员信息，返回真实会员权益。
2. 如果用户不存在会员信息，不返回 null，而是返回游客会员对象。
3. Service 和 Controller 不需要反复判断 memberProfile == null。
4. 前端始终能拿到结构稳定的响应。
```

普通写法中，查询用户会员信息经常会出现大量空判断：

```java
MemberProfile memberProfile = repository.findByUserId(userId);
if (memberProfile == null) {
    // 返回游客默认权益
}
```

使用空对象模式后，调用方始终拿到一个 `MemberProfile`：

```java
MemberProfile memberProfile = repository.findByUserIdOrDefault(userId);
```

这样真实会员对象和游客默认对象可以通过同一套接口访问。

### 项目依赖

文件位置：`pom.xml`

下面配置 Web、Validation、Hutool 和 Lombok。当前示例使用内存数据模拟会员信息，暂不引入数据库。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、对象、金额等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少构造器、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 应用配置

文件位置：`src/main/resources/application.yml`

示例只需要基础应用配置。真实项目中可以把会员数据改为来自 MySQL、Redis、远程会员中心或配置中心。

```yaml
spring:
  application:
    name: design-pattern-null-object

server:
  port: 8080
```

### 文件结构

空对象模式建议先定义统一抽象，再分别提供真实对象和空对象实现。业务层只依赖抽象，不直接处理 `null`。

```text
src/main/java/io/github/atengk/nullobject
├── NullObjectApplication.java
├── controller
│   └── MemberBenefitController.java
├── domain
│   └── model
│       ├── MemberProfile.java
│       ├── NullMemberProfile.java
│       └── RealMemberProfile.java
├── repository
│   ├── MemberProfileRepository.java
│   └── impl
│       └── InMemoryMemberProfileRepository.java
├── service
│   ├── MemberBenefitService.java
│   └── impl
│       └── MemberBenefitServiceImpl.java
└── vo
    └── MemberBenefitVO.java
```

## 模式说明

空对象模式的核心思想是：当业务对象不存在时，不返回 `null`，而是返回一个实现了相同接口的默认对象。

普通写法：

```text
Controller
 -> Service
   -> Repository
     -> 查询不到返回 null
   -> Service 判断 null
   -> Controller 可能继续判断 null
```

空对象模式写法：

```text
Controller
 -> Service
   -> Repository
     -> 查询不到返回 NullMemberProfile
   -> Service 统一调用 MemberProfile
   -> Controller 统一返回 VO
```

空对象不是随便创建一个空壳对象，而是一个具备安全默认行为的对象。它应该明确表达“没有真实对象，但可以安全使用”。

例如：

```text
真实会员对象：
- 昵称：张三
- 等级：3
- 折扣：0.85
- 优惠券数量：5
- 是否游客：false

空会员对象：
- 昵称：游客用户
- 等级：0
- 折扣：1.00
- 优惠券数量：0
- 是否游客：true
```

调用方不需要关心对象是否为 `null`，只需要根据 `isGuest()` 判断是否游客即可。

## 核心代码

这一节给出空对象模式的完整关键代码。示例重点体现三点：

```text
1. 使用 MemberProfile 抽象统一真实对象和空对象。
2. NullMemberProfile 提供安全默认值，不返回 null。
3. Service 不再堆叠 memberProfile == null 判断。
```

### 启动类

文件位置：`src/main/java/io/github/atengk/nullobject/NullObjectApplication.java`

启动类用于启动 Spring Boot 应用。

```java
package io.github.atengk.nullobject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 空对象模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class NullObjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(NullObjectApplication.class, args);
    }

}
```

### 会员信息抽象

文件位置：`src/main/java/io/github/atengk/nullobject/domain/model/MemberProfile.java`

该接口定义会员信息的统一访问方式。真实会员对象和空会员对象都实现这个接口。

```java
package io.github.atengk.nullobject.domain.model;

import java.math.BigDecimal;

/**
 * 会员信息抽象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface MemberProfile {

    String userId();

    String nickname();

    Integer memberLevel();

    BigDecimal discountRate();

    Integer couponCount();

    boolean isGuest();

    default String levelName() {
        return switch (memberLevel()) {
            case 5 -> "钻石会员";
            case 4 -> "铂金会员";
            case 3 -> "黄金会员";
            case 2 -> "白银会员";
            case 1 -> "普通会员";
            default -> "游客用户";
        };
    }

}
```

### 真实会员对象

文件位置：`src/main/java/io/github/atengk/nullobject/domain/model/RealMemberProfile.java`

该对象表示真实存在的会员信息。

```java
package io.github.atengk.nullobject.domain.model;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/**
 * 真实会员信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record RealMemberProfile(

        String userId,

        String nickname,

        Integer memberLevel,

        BigDecimal discountRate,

        Integer couponCount

) implements MemberProfile {

    public RealMemberProfile {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(nickname)) {
            throw new IllegalArgumentException("用户昵称不能为空");
        }
        if (memberLevel == null || memberLevel < 1) {
            throw new IllegalArgumentException("会员等级不合法");
        }
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("会员折扣不合法");
        }
        if (couponCount == null || couponCount < 0) {
            throw new IllegalArgumentException("优惠券数量不合法");
        }
    }

    @Override
    public boolean isGuest() {
        return false;
    }

}
```

### 空会员对象

文件位置：`src/main/java/io/github/atengk/nullobject/domain/model/NullMemberProfile.java`

该对象表示查询不到会员信息时的安全默认对象。它不是异常对象，而是一个可正常参与业务展示的默认对象。

```java
package io.github.atengk.nullobject.domain.model;

import java.math.BigDecimal;

/**
 * 空会员信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class NullMemberProfile implements MemberProfile {

    public static final NullMemberProfile INSTANCE = new NullMemberProfile();

    private NullMemberProfile() {
    }

    @Override
    public String userId() {
        return "GUEST";
    }

    @Override
    public String nickname() {
        return "游客用户";
    }

    @Override
    public Integer memberLevel() {
        return 0;
    }

    @Override
    public BigDecimal discountRate() {
        return BigDecimal.ONE;
    }

    @Override
    public Integer couponCount() {
        return 0;
    }

    @Override
    public boolean isGuest() {
        return true;
    }

}
```

## 仓储层代码

这一节使用内存数据模拟会员查询。真实项目中，`InMemoryMemberProfileRepository` 可以替换为 MyBatis-Plus、JPA、Redis 或远程会员服务实现。

### 仓储接口

文件位置：`src/main/java/io/github/atengk/nullobject/repository/MemberProfileRepository.java`

仓储接口直接提供 `findByUserIdOrDefault` 方法，明确表达“查询不到时返回默认对象”。

```java
package io.github.atengk.nullobject.repository;

import io.github.atengk.nullobject.domain.model.MemberProfile;

import java.util.Optional;

/**
 * 会员信息仓储接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface MemberProfileRepository {

    Optional<MemberProfile> findByUserId(String userId);

    MemberProfile findByUserIdOrDefault(String userId);

}
```

### 内存仓储实现

文件位置：`src/main/java/io/github/atengk/nullobject/repository/impl/InMemoryMemberProfileRepository.java`

该实现类模拟从数据源查询会员信息。查询不到时返回 `NullMemberProfile.INSTANCE`。

```java
package io.github.atengk.nullobject.repository.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.nullobject.domain.model.MemberProfile;
import io.github.atengk.nullobject.domain.model.NullMemberProfile;
import io.github.atengk.nullobject.domain.model.RealMemberProfile;
import io.github.atengk.nullobject.repository.MemberProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 基于内存的会员信息仓储实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class InMemoryMemberProfileRepository implements MemberProfileRepository {

    private static final Map<String, MemberProfile> MEMBER_PROFILE_MAP = new HashMap<>();

    static {
        MEMBER_PROFILE_MAP.put("10001", new RealMemberProfile(
                "10001",
                "张三",
                3,
                new BigDecimal("0.85"),
                5
        ));

        MEMBER_PROFILE_MAP.put("10002", new RealMemberProfile(
                "10002",
                "李四",
                1,
                new BigDecimal("0.95"),
                1
        ));

        MEMBER_PROFILE_MAP.put("10003", new RealMemberProfile(
                "10003",
                "王五",
                5,
                new BigDecimal("0.75"),
                12
        ));
    }

    @Override
    public Optional<MemberProfile> findByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(MEMBER_PROFILE_MAP.get(userId));
    }

    @Override
    public MemberProfile findByUserIdOrDefault(String userId) {
        if (StrUtil.isBlank(userId)) {
            log.warn("用户ID为空，返回空会员对象");
            return NullMemberProfile.INSTANCE;
        }

        MemberProfile memberProfile = MEMBER_PROFILE_MAP.get(userId);
        if (memberProfile == null) {
            log.info("会员信息不存在，返回空会员对象，userId={}", userId);
            return NullMemberProfile.INSTANCE;
        }

        return memberProfile;
    }

}
```

## 业务层代码

业务层不再处理 `null`，只面对 `MemberProfile` 抽象。真实会员和游客会员都能走同一段代码。

### 响应 VO

文件位置：`src/main/java/io/github/atengk/nullobject/vo/MemberBenefitVO.java`

该 VO 用于接口返回会员权益信息。

```java
package io.github.atengk.nullobject.vo;

import java.math.BigDecimal;

/**
 * 会员权益响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record MemberBenefitVO(

        String userId,

        String nickname,

        Integer memberLevel,

        String levelName,

        BigDecimal discountRate,

        Integer couponCount,

        Boolean guest,

        String benefitText

) {
}
```

### Service 接口

文件位置：`src/main/java/io/github/atengk/nullobject/service/MemberBenefitService.java`

该接口定义会员权益查询能力。

```java
package io.github.atengk.nullobject.service;

import io.github.atengk.nullobject.vo.MemberBenefitVO;

/**
 * 会员权益业务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface MemberBenefitService {

    MemberBenefitVO getBenefitSummary(String userId);

}
```

### Service 实现

文件位置：`src/main/java/io/github/atengk/nullobject/service/impl/MemberBenefitServiceImpl.java`

该实现类通过空对象模式统一处理真实会员和游客会员。这里没有 `memberProfile == null` 判断。

```java
package io.github.atengk.nullobject.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.nullobject.domain.model.MemberProfile;
import io.github.atengk.nullobject.repository.MemberProfileRepository;
import io.github.atengk.nullobject.service.MemberBenefitService;
import io.github.atengk.nullobject.vo.MemberBenefitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 会员权益业务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBenefitServiceImpl implements MemberBenefitService {

    private final MemberProfileRepository memberProfileRepository;

    @Override
    public MemberBenefitVO getBenefitSummary(String userId) {
        MemberProfile memberProfile = memberProfileRepository.findByUserIdOrDefault(userId);

        String benefitText = buildBenefitText(memberProfile);
        log.info("查询会员权益完成，requestUserId={}，actualUserId={}，guest={}",
                userId, memberProfile.userId(), memberProfile.isGuest());

        return new MemberBenefitVO(
                memberProfile.userId(),
                memberProfile.nickname(),
                memberProfile.memberLevel(),
                memberProfile.levelName(),
                memberProfile.discountRate(),
                memberProfile.couponCount(),
                memberProfile.isGuest(),
                benefitText
        );
    }

    private String buildBenefitText(MemberProfile memberProfile) {
        BigDecimal discount = NumberUtil.mul(memberProfile.discountRate(), new BigDecimal("10"));

        if (memberProfile.isGuest()) {
            return "当前为游客用户，可登录后查看会员权益";
        }

        return StrUtil.format(
                "尊敬的{}，您当前是{}，购物可享{}折，可用优惠券{}张",
                memberProfile.nickname(),
                memberProfile.levelName(),
                discount.stripTrailingZeros().toPlainString(),
                memberProfile.couponCount()
        );
    }

}
```

## 接口层代码

Controller 只负责接收请求并返回结果，不处理会员是否存在的细节。

### Controller 接口

文件位置：`src/main/java/io/github/atengk/nullobject/controller/MemberBenefitController.java`

该 Controller 提供会员权益查询接口。

```java
package io.github.atengk.nullobject.controller;

import io.github.atengk.nullobject.service.MemberBenefitService;
import io.github.atengk.nullobject.vo.MemberBenefitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会员权益接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberBenefitController {

    private final MemberBenefitService memberBenefitService;

    @GetMapping("/{userId}/benefit-summary")
    public MemberBenefitVO getBenefitSummary(@PathVariable String userId) {
        return memberBenefitService.getBenefitSummary(userId);
    }

}
```

## 使用方式

本示例提供一个会员权益查询接口。无论用户是否存在，接口都会返回结构稳定的响应对象。

### 查询真实会员权益

接口信息：

| 项目     | 内容                                |
| -------- | ----------------------------------- |
| 请求路径 | `/members/{userId}/benefit-summary` |
| 请求方法 | `GET`                               |
| 主要作用 | 查询会员权益摘要                    |

请求示例：

```bash
curl -X GET 'http://localhost:8080/members/10001/benefit-summary'
```

响应示例：

```json
{
  "userId": "10001",
  "nickname": "张三",
  "memberLevel": 3,
  "levelName": "黄金会员",
  "discountRate": 0.85,
  "couponCount": 5,
  "guest": false,
  "benefitText": "尊敬的张三，您当前是黄金会员，购物可享8.5折，可用优惠券5张"
}
```

### 查询不存在用户权益

请求示例：

```bash
curl -X GET 'http://localhost:8080/members/99999/benefit-summary'
```

响应示例：

```json
{
  "userId": "GUEST",
  "nickname": "游客用户",
  "memberLevel": 0,
  "levelName": "游客用户",
  "discountRate": 1,
  "couponCount": 0,
  "guest": true,
  "benefitText": "当前为游客用户，可登录后查看会员权益"
}
```

可以看到，用户不存在时接口没有返回 `null`，也没有报错，而是返回了一个安全的游客对象。

## 验证方式

可以通过接口响应和日志验证空对象模式是否生效。

启动项目：

```bash
mvn spring-boot:run
```

查询真实会员：

```bash
curl -X GET 'http://localhost:8080/members/10003/benefit-summary'
```

查询不存在用户：

```bash
curl -X GET 'http://localhost:8080/members/99999/benefit-summary'
```

验证点：

```text
1. 查询不存在用户时，接口仍然返回完整 JSON。
2. Service 中没有 memberProfile == null 判断。
3. Repository 查询不到数据时返回 NullMemberProfile.INSTANCE。
4. NullMemberProfile 中所有方法都有安全默认值。
5. 前端可以通过 guest 字段判断是否游客用户。
```

如果接口异常，重点检查：

```text
1. Controller 路径是否为 /members/{userId}/benefit-summary。
2. InMemoryMemberProfileRepository 是否添加 @Repository。
3. MemberBenefitServiceImpl 是否添加 @Service。
4. NullMemberProfile 的方法是否返回了非 null 的安全默认值。
5. RealMemberProfile 的构造参数是否合法。
```

## 适用场景

空对象模式适合“对象不存在但业务可以继续执行”的场景。

常见适用场景：

```text
用户信息：
- 未登录用户返回游客对象
- 查询不到会员返回默认会员对象
- 查询不到头像返回默认头像对象

配置读取：
- 查询不到租户配置返回默认配置
- 查询不到功能开关返回关闭配置
- 查询不到计费规则返回默认计费规则

权限系统：
- 查询不到角色返回空角色
- 查询不到菜单返回空菜单集合
- 查询不到权限返回无权限对象

通知系统：
- 查询不到通知渠道返回空通知器
- 用户未绑定邮箱时返回 NoOpEmailSender
- 用户未绑定手机号时返回 NoOpSmsSender

优惠和风控：
- 查询不到优惠策略返回无优惠策略
- 查询不到风控规则返回通过或拒绝的默认规则
- 查询不到黑名单记录返回非黑名单对象
```

## 不适用场景

空对象模式不能替代所有空值处理。对于必须显式报错的业务，不应该用空对象掩盖问题。

不建议使用的场景：

```text
1. 数据不存在就是业务异常，例如订单不存在、支付单不存在。
2. 缺少对象会导致资金、库存、权限等关键风险。
3. 调用方必须明确知道数据不存在，并做特殊处理。
4. 空对象默认行为不清晰，容易误导业务结果。
5. 使用空对象只是为了隐藏错误，而不是表达合理默认行为。
```

例如支付业务中，查询不到支付单通常应该报错：

```text
支付单不存在 -> 应该返回异常或失败结果
```

不应该返回一个“空支付单”继续扣款、退款或对账。

## 和 Optional 的区别

空对象模式和 `Optional` 都可以减少空指针风险，但二者表达的业务含义不同。

| 对比项   | 空对象模式                     | Optional                            |
| -------- | ------------------------------ | ----------------------------------- |
| 核心含义 | 不存在时仍可用默认对象继续执行 | 明确表达结果可能不存在              |
| 返回值   | 统一接口对象                   | 容器对象                            |
| 调用方式 | 直接调用对象方法               | 需要 `map`、`orElse`、`orElseThrow` |
| 适合场景 | 有合理默认行为                 | 需要调用方决定如何处理不存在        |
| 风险     | 默认行为不合理时可能掩盖问题   | 调用方仍需处理不存在分支            |

示例：

```java
Optional<MemberProfile> memberProfile = repository.findByUserId(userId);
```

这种写法表达“会员可能不存在，需要调用方决定怎么处理”。

```java
MemberProfile memberProfile = repository.findByUserIdOrDefault(userId);
```

这种写法表达“会员不存在也可以使用默认会员对象继续执行”。

二者可以同时存在。Repository 可以提供两个方法：

```java
Optional<MemberProfile> findByUserId(String userId);

MemberProfile findByUserIdOrDefault(String userId);
```

这样调用方可以按业务语义选择。

## 和默认值的区别

空对象不是简单默认值。默认值通常只处理单个字段，而空对象处理的是一组行为。

简单默认值：

```java
String nickname = StrUtil.blankToDefault(user.getNickname(), "游客用户");
```

空对象：

```java
MemberProfile memberProfile = memberProfileRepository.findByUserIdOrDefault(userId);

memberProfile.nickname();
memberProfile.memberLevel();
memberProfile.discountRate();
memberProfile.couponCount();
memberProfile.isGuest();
```

如果只有一个字段需要默认值，用 Hutool 或简单三元表达式就够了。如果一整个对象都需要默认行为，才更适合空对象模式。

## 和策略模式的结合

空对象模式经常和策略模式结合使用。例如通知发送场景中，用户没有绑定手机号时，可以返回一个 `NoOpSmsSender`，表示什么都不做的短信发送策略。

示意结构：

```text
NotifySender
├── SmsNotifySender
├── EmailNotifySender
└── NoOpNotifySender
```

调用方统一执行：

```java
notifySender.send(message);
```

如果当前没有可用通知渠道，返回 `NoOpNotifySender`，它的 `send` 方法只记录日志，不执行真实外部调用。

这种方式可以避免调用方写成：

```java
if (notifySender != null) {
    notifySender.send(message);
}
```

但要注意，`NoOpNotifySender` 必须有清晰日志，否则容易造成“业务看起来执行了，但实际什么都没发生”的问题。

## 项目落地建议

在 Spring Boot 项目中使用空对象模式时，重点是让默认行为合理、明确、可观察。

建议：

```text
1. 空对象类命名使用 Null、Empty、Default 或 NoOp 前缀。
2. 空对象必须实现和真实对象相同的接口。
3. 空对象的方法返回安全默认值，不要继续返回 null。
4. 空对象适合表达“无数据但可继续”，不适合隐藏异常。
5. 重要空对象行为要记录日志，便于排查。
6. 对资金、库存、权限等关键业务要谨慎使用空对象。
7. 空对象可以做成单例，避免重复创建无状态对象。
8. Repository 可以同时提供 Optional 查询和默认对象查询。
```

推荐命名：

```text
NullMemberProfile
EmptyPermissionSet
DefaultTenantConfig
NoOpNotifySender
EmptyMenuTree
DefaultDiscountPolicy
```

不推荐命名：

```text
FakeUser
TempObject
NoneData
BlankThing
UnknownModel
```

这些命名不够明确，后续维护人员很难判断它们是测试对象、临时对象还是业务默认对象。

## 常见问题

### 空对象是否一定要单例

不一定。如果空对象无状态，可以做成单例，例如：

```java
public static final NullMemberProfile INSTANCE = new NullMemberProfile();
```

如果空对象需要携带上下文信息，例如请求用户 ID、租户 ID、缺省原因，则可以每次创建新对象：

```java
new NullMemberProfile(requestUserId, "会员不存在");
```

但大多数无状态空对象使用单例即可。

### 空对象里面能不能抛异常

一般不建议。空对象的价值在于提供安全默认行为。如果大部分方法都抛异常，它就不再是空对象，而更像异常占位对象。

但在少数危险操作中，可以抛异常。例如空支付账户不允许扣款：

```text
NullPaymentAccount.debit()
 -> 抛出“支付账户不存在，不能扣款”
```

这类行为需要谨慎设计，避免调用方误以为空对象什么都能安全执行。

### 空对象是否会掩盖数据问题

会有这个风险。因此空对象只适合“业务允许默认处理”的场景。

例如：

```text
查询不到会员信息 -> 可以返回游客会员
查询不到租户配置 -> 可以返回默认配置
查询不到通知渠道 -> 可以返回 NoOp 通知器
```

但下面这些场景不应随便使用空对象：

```text
查询不到订单 -> 应该提示订单不存在
查询不到支付单 -> 应该提示支付单不存在
查询不到库存记录 -> 应该阻止扣减库存
查询不到权限记录 -> 应该拒绝访问
```

### 前端如何区分真实对象和空对象

推荐在 VO 中显式返回标识字段，例如：

```json
{
  "guest": true
}
```

不要让前端通过 `userId == "GUEST"`、`memberLevel == 0` 这类隐式规则判断。显式字段更稳定，也更容易扩展。

## 总结

空对象模式适合在 Spring Boot 项目中处理“对象不存在但业务可以继续执行”的场景。它通过安全默认对象替代 `null`，减少空判断和空指针风险，让调用链更加稳定。

推荐落地方式：

```text
Controller
 -> Service
   -> Repository
     -> 查询到真实对象：RealMemberProfile
     -> 查询不到对象：NullMemberProfile
   -> Service 统一调用 MemberProfile
```

当业务需要明确报错时，不要使用空对象模式。当业务存在合理默认行为，例如游客用户、默认配置、空权限集合、无操作通知器时，空对象模式可以让代码更清晰、更稳定，也更容易测试。
