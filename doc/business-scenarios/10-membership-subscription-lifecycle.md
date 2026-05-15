# 会员订阅与权益生命周期

本文围绕“用户购买套餐、开通会员权益、续费叠加有效期、升级套餐、降级套餐、到期提醒、到期回收权益”这一业务链路实现核心后端功能，适用于 SaaS 订阅、视频会员、知识付费、企业套餐、工具平台等场景。

## 场景目标

本案例重点实现会员订阅的核心闭环，不追求完整支付系统、完整营销系统或复杂计费系统，而是把会员套餐、订阅周期、权益发放、续费、升降级、到期回收这些高频核心逻辑落到代码层面。

最终实现的能力包括：

| 能力     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 套餐购买 | 用户购买指定会员套餐后生成订阅记录                           |
| 会员开通 | 根据套餐周期计算会员有效期                                   |
| 续费叠加 | 未过期会员从当前到期时间继续叠加，已过期会员从当前时间重新计算 |
| 套餐升级 | 用户从低等级套餐升级到高等级套餐，立即生效                   |
| 套餐降级 | 用户从高等级套餐降级到低等级套餐，到期后生效                 |
| 权益发放 | 根据套餐配置给用户发放功能权益                               |
| 权益回收 | 会员到期后自动回收权益                                       |
| 到期补偿 | 通过定时任务扫描即将到期和已到期订阅                         |

### 业务背景

会员订阅系统的本质是“时间周期 + 套餐等级 + 权益集合”的组合管理。

普通订单系统只关心用户是否购买成功，而会员订阅还要持续管理用户在一段时间内拥有什么权限、什么时候失效、续费如何衔接、升级是否立即生效、降级是否延后生效、到期后如何回收权限。

在实际业务中，会员订阅常见于以下场景：

| 场景        | 示例                             |
| ----------- | -------------------------------- |
| SaaS 系统   | 基础版、专业版、企业版           |
| 视频会员    | 月卡、季卡、年卡                 |
| 知识付费    | 专栏会员、训练营会员             |
| 企业工具    | 按账号数、存储空间、功能模块收费 |
| AI 工具平台 | 免费版、Plus 版、团队版          |

本案例采用“套餐表 + 套餐权益表 + 用户订阅表 + 用户权益表 + 订阅变更记录表”的模型实现。业务上重点保证这几件事：

1. 用户同一时间只能有一个当前生效订阅。
2. 续费要正确处理未过期和已过期两种情况。
3. 升级套餐立即变更当前订阅和权益。
4. 降级套餐不立即影响当前权益，而是记录为待生效变更。
5. 到期后必须回收权益，避免用户继续使用过期权限。
6. 所有购买、续费、升降级操作都要支持幂等处理。

### 核心功能边界

本案例只实现会员订阅和权益生命周期的核心功能，不展开完整支付链路。支付成功后的业务入口会抽象成一个 `orderNo`，模拟“支付已成功，开始开通或变更会员”。

包含的功能边界如下：

| 模块       | 本案例是否实现 | 说明                                 |
| ---------- | -------------- | ------------------------------------ |
| 套餐配置   | 实现           | 通过数据库表维护套餐基础信息         |
| 权益配置   | 实现           | 一个套餐可以绑定多个权益编码         |
| 购买开通   | 实现           | 支付成功后创建或刷新用户订阅         |
| 续费叠加   | 实现           | 根据当前会员是否过期计算新的到期时间 |
| 升级套餐   | 实现           | 立即切换套餐并重新发放权益           |
| 升级补差价 | 简化实现       | 只保留补差价计算方法，不接真实支付   |
| 降级套餐   | 实现           | 记录待生效套餐，到期后切换           |
| 到期提醒   | 实现           | XXL-JOB 扫描即将到期订阅并记录日志   |
| 到期回收   | 实现           | XXL-JOB 扫描已过期订阅并回收权益     |
| 自动续费   | 不实现         | 只预留字段，不接支付代扣             |
| 支付系统   | 不实现         | 假设业务方法只在支付成功后调用       |
| 发票、退款 | 不实现         | 不影响订阅主流程                     |

核心流程如下：

```text
用户支付成功
-> 根据套餐计算有效期
-> 创建或更新用户订阅
-> 发放套餐权益
-> 写入订阅变更记录
-> 定时任务扫描到期订阅
-> 到期后回收权益
```

续费处理逻辑如下：

```text
如果用户没有会员或会员已过期：
    新开始时间 = 当前时间
    新到期时间 = 当前时间 + 套餐周期

如果用户会员未过期：
    新开始时间 = 当前会员开始时间
    新到期时间 = 当前到期时间 + 套餐周期
```

升级处理逻辑如下：

```text
校验当前套餐等级 < 目标套餐等级
-> 计算剩余有效期价值
-> 计算目标套餐补差价
-> 支付补差价成功
-> 当前订阅立即切换到目标套餐
-> 回收旧权益
-> 发放新权益
```

降级处理逻辑如下：

```text
校验当前套餐等级 > 目标套餐等级
-> 不立即变更当前套餐
-> 写入 pending_plan_id
-> 当前订阅到期后由定时任务切换到低等级套餐
-> 回收旧权益
-> 发放新权益
```

### 技术栈选型

本案例使用 Spring Boot 3 + MyBatis-Plus + MySQL + Redis + Redisson + XXL-JOB 实现核心订阅能力。技术栈和 README 中推荐方向保持一致，同时补充 Redisson 做用户级业务锁，避免同一用户并发购买、续费、升降级导致有效期计算错误。

| 技术            | 用途                                 |
| --------------- | ------------------------------------ |
| Spring Boot 3   | 提供 Web、IOC、事务等基础能力        |
| MyBatis-Plus    | 简化单表 CRUD 和条件更新             |
| MySQL           | 存储套餐、订阅、权益、变更记录       |
| Redis           | 缓存套餐和用户权益                   |
| Redisson        | 用户订阅变更分布式锁                 |
| XXL-JOB         | 会员到期提醒、权益回收、降级生效任务 |
| Hutool DateUtil | 处理会员有效期计算                   |
| Hutool IdUtil   | 生成业务编号和记录编号               |
| Lombok          | 简化实体和 DTO 代码                  |
| Sa-Token        | 获取当前登录用户，后续接口鉴权使用   |

项目核心依赖如下，放在 `pom.xml` 中。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 MySQL 数据访问 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis：缓存套餐、用户权益和幂等结果 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：实现用户维度分布式锁 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- XXL-JOB：执行到期提醒、到期回收、降级生效任务 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Hutool：日期计算、ID 生成、对象工具、集合工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Sa-Token：登录认证与当前用户获取 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.38.0</version>
    </dependency>

    <!-- Lombok：减少实体类、DTO、VO 样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

基础配置放在 `src/main/resources/application.yml` 中。

```yaml
server:
  port: 8080

spring:
  application:
    name: member-subscription-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/member_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

mybatis-plus:
  configuration:
    # 开发阶段打印 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除字段，后续表结构会统一使用 deleted
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

xxl:
  job:
    admin:
      # XXL-JOB 调度中心地址
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      # 当前执行器名称
      appname: member-subscription-job
      # 执行器日志路径
      logpath: ./logs/xxl-job
      # 日志保留天数
      logretentiondays: 30
    accessToken: default_token

sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期，单位秒
  timeout: 2592000
  # 是否允许同一账号多地登录
  is-concurrent: true
```

本案例后续代码默认包路径如下：

```text
io.github.atengk.member
```

核心目录结构如下：

```text
src/main/java/io/github/atengk/member
├── controller
│   └── MemberSubscriptionController.java
├── service
│   ├── MemberSubscriptionService.java
│   └── impl
│       └── MemberSubscriptionServiceImpl.java
├── mapper
│   ├── MemberPlanMapper.java
│   ├── MemberPlanBenefitMapper.java
│   ├── UserSubscriptionMapper.java
│   ├── UserBenefitMapper.java
│   └── SubscriptionChangeLogMapper.java
├── entity
│   ├── MemberPlan.java
│   ├── MemberPlanBenefit.java
│   ├── UserSubscription.java
│   ├── UserBenefit.java
│   └── SubscriptionChangeLog.java
├── dto
│   ├── SubscribeRequest.java
│   ├── RenewRequest.java
│   ├── UpgradeRequest.java
│   └── DowngradeRequest.java
├── vo
│   └── UserSubscriptionVO.java
├── enums
│   ├── SubscriptionStatusEnum.java
│   ├── ChangeTypeEnum.java
│   └── BenefitStatusEnum.java
├── job
│   └── MemberSubscriptionJob.java
└── common
    └── BizException.java
```

## 业务流程设计

本模块围绕 README 中“用户购买套餐 -> 开通会员权益 -> 计算会员有效期 -> 续费叠加有效期 -> 升级套餐 -> 降级套餐 -> 到期提醒 -> 到期回收权益”的主链路展开，重点处理会员有效期、套餐变更和权益生命周期。

### 购买套餐开通会员

购买套餐用于处理用户首次开通会员，或者会员已经过期后重新购买套餐的场景。

核心处理流程：

```text
用户选择套餐
-> 创建支付订单
-> 支付成功回调
-> 根据 order_no 做幂等校验
-> 查询套餐信息
-> 判断用户是否已有有效订阅
-> 计算会员开始时间和结束时间
-> 创建或更新用户订阅
-> 发放套餐权益
-> 写入订阅变更记录
```

首次购买时，有效期从当前时间开始计算。

```text
start_time = now()
end_time = now() + 套餐周期
```

如果用户之前有订阅但已经过期，也按重新购买处理。

```text
当前订阅 end_time < now()
-> start_time = now()
-> end_time = now() + 套餐周期
```

需要注意的是，购买入口必须由“支付成功”驱动，不建议前端直接调用开通接口。真实项目中一般由支付回调、MQ 消息或订单状态机触发会员开通。

核心落库行为：

| 数据表                    | 操作                   |
| ------------------------- | ---------------------- |
| `user_subscription`       | 新增或更新当前用户订阅 |
| `user_benefit`            | 发放套餐对应权益       |
| `subscription_change_log` | 记录购买开通流水       |

关键控制点：

| 控制点           | 处理方式                                            |
| ---------------- | --------------------------------------------------- |
| 重复支付回调     | `order_no` 唯一索引防重                             |
| 同一用户并发购买 | Redisson 用户维度锁                                 |
| 套餐不存在或下架 | 拒绝开通                                            |
| 权益重复发放     | `user_id + benefit_code + source_order_no` 唯一控制 |

### 续费叠加有效期

续费用于处理用户在当前套餐基础上延长会员有效期的场景。续费一般要求目标套餐和当前套餐一致，避免续费时偷偷变更套餐等级。

核心处理流程：

```text
用户选择当前套餐续费
-> 创建续费支付订单
-> 支付成功回调
-> 根据 order_no 做幂等校验
-> 查询当前用户订阅
-> 校验当前套餐和续费套餐一致
-> 计算新的到期时间
-> 更新用户订阅
-> 延长用户权益有效期
-> 写入订阅变更记录
```

续费有效期计算分两种情况。

用户会员未过期时，从当前到期时间继续叠加：

```text
当前时间：2026-05-15 10:00:00
原到期时间：2026-06-15 23:59:59
续费周期：30 天

新的开始时间：保持不变
新的到期时间：2026-07-15 23:59:59
```

用户会员已过期时，从当前时间重新计算：

```text
当前时间：2026-05-15 10:00:00
原到期时间：2026-04-15 23:59:59
续费周期：30 天

新的开始时间：2026-05-15 10:00:00
新的到期时间：2026-06-14 10:00:00
```

伪逻辑如下：

```text
if subscription.end_time > now:
    new_end_time = subscription.end_time + plan.period_days
else:
    subscription.start_time = now
    new_end_time = now + plan.period_days
```

核心落库行为：

| 数据表                    | 操作                                     |
| ------------------------- | ---------------------------------------- |
| `user_subscription`       | 更新 `end_time`、`status`、`renew_count` |
| `user_benefit`            | 延长当前套餐权益有效期                   |
| `subscription_change_log` | 记录续费流水                             |

关键控制点：

| 控制点         | 处理方式                     |
| -------------- | ---------------------------- |
| 续费套餐不一致 | 拒绝续费，提示使用升级或降级 |
| 过期后续费     | 从当前时间重新开通           |
| 未过期续费     | 从原到期时间叠加             |
| 并发续费       | 用户维度锁 + 数据库事务      |

### 升级套餐即时生效

升级套餐用于用户从低等级套餐切换到高等级套餐，例如从基础版升级为专业版。升级通常立即生效，并重新发放高等级套餐权益。

核心处理流程：

```text
用户选择高等级套餐
-> 查询当前有效订阅
-> 校验目标套餐等级高于当前套餐
-> 计算剩余有效期价值
-> 计算升级补差价
-> 用户支付补差价
-> 支付成功后切换当前套餐
-> 回收旧套餐权益
-> 发放新套餐权益
-> 写入订阅变更记录
```

套餐等级用 `level` 字段控制，数字越大表示套餐等级越高。

```text
基础版 level = 1
专业版 level = 2
企业版 level = 3
```

升级校验规则：

```text
target_plan.level > current_plan.level
```

升级后有效期可以采用两种方案：

| 方案           | 说明                               | 本案例采用 |
| -------------- | ---------------------------------- | ---------- |
| 保持原到期时间 | 只切换权益，不延长周期             | 是         |
| 重新购买周期   | 支付完整新套餐价格，重新计算有效期 | 否         |

本案例采用“保持原到期时间，只切换套餐和权益”的方案。这样实现更清晰，也更符合常见 SaaS 订阅升级逻辑。

补差价简化计算方式：

```text
剩余天数 = 当前订阅到期时间 - 当前时间
当前套餐日单价 = 当前套餐价格 / 当前套餐周期
目标套餐日单价 = 目标套餐价格 / 目标套餐周期
补差价 = (目标套餐日单价 - 当前套餐日单价) * 剩余天数
```

核心落库行为：

| 数据表                    | 操作                                         |
| ------------------------- | -------------------------------------------- |
| `user_subscription`       | 更新当前 `plan_id`、`plan_code`、`plan_name` |
| `user_benefit`            | 旧权益置为失效，新权益重新发放               |
| `subscription_change_log` | 记录升级流水和补差价金额                     |

关键控制点：

| 控制点           | 处理方式                   |
| ---------------- | -------------------------- |
| 当前无有效订阅   | 不允许升级，提示先购买     |
| 目标套餐等级不高 | 拒绝升级                   |
| 旧权益残留       | 升级前先回收旧权益         |
| 新权益重复发放   | 按用户和权益编码做唯一控制 |
| 补差价为负数     | 不允许按升级流程处理       |

### 降级套餐延后生效

降级套餐用于用户从高等级套餐切换到低等级套餐，例如从企业版降级为专业版。降级通常不立即生效，而是在当前订阅周期结束后生效，避免用户已经支付的高等级权益被提前剥夺。

核心处理流程：

```text
用户选择低等级套餐
-> 查询当前有效订阅
-> 校验目标套餐等级低于当前套餐
-> 不立即修改当前 plan_id
-> 写入 pending_plan_id
-> 写入 pending_plan_effect_time
-> 写入订阅变更记录
-> 当前订阅到期后由定时任务执行降级
```

降级校验规则：

```text
target_plan.level < current_plan.level
```

降级预约后的订阅记录示例：

```text
当前套餐：企业版
当前到期时间：2026-06-15 23:59:59
待生效套餐：专业版
待生效时间：2026-06-15 23:59:59
```

到期任务执行时：

```text
扫描 pending_plan_id 不为空
且 pending_plan_effect_time <= now()
且 subscription.status = ACTIVE 的记录

-> 回收当前高等级权益
-> 切换为 pending_plan_id 对应套餐
-> 发放低等级套餐权益
-> 清空 pending_plan_id
-> 清空 pending_plan_effect_time
-> 写入订阅变更记录
```

核心落库行为：

| 数据表                    | 操作                                 |
| ------------------------- | ------------------------------------ |
| `user_subscription`       | 更新待生效套餐字段，不立即改当前套餐 |
| `subscription_change_log` | 记录降级预约流水                     |
| `user_benefit`            | 当前阶段不处理，到期后再回收和发放   |

关键控制点：

| 控制点           | 处理方式                   |
| ---------------- | -------------------------- |
| 当前无有效订阅   | 不允许降级                 |
| 目标套餐等级不低 | 拒绝降级                   |
| 重复降级预约     | 覆盖或拒绝，本案例采用覆盖 |
| 到期任务漏执行   | 定时任务持续扫描补偿       |
| 降级后权益变化   | 到期任务统一处理           |

### 到期提醒与权益回收

到期提醒和权益回收由定时任务完成。业务上需要区分“即将到期”和“已经到期”。

到期提醒流程：

```text
XXL-JOB 每天执行
-> 扫描未来 3 天内到期的有效订阅
-> 判断是否已经提醒过
-> 发送站内信、短信或邮件
-> 记录提醒时间
```

本案例为了聚焦会员订阅核心逻辑，只记录提醒日志，不接入真实短信或站内信。

到期回收流程：

```text
XXL-JOB 每 5 分钟执行
-> 扫描 end_time <= now() 的有效订阅
-> 判断是否存在 pending_plan_id
-> 如果存在待降级套餐，执行降级生效
-> 如果不存在待降级套餐，关闭订阅
-> 回收用户权益
-> 写入订阅变更记录
```

到期后的两种处理分支：

```text
分支一：普通到期
-> subscription.status = EXPIRED
-> user_benefit.status = EXPIRED

分支二：存在降级预约
-> 切换为 pending_plan_id 套餐
-> 重新计算低等级套餐有效期
-> 回收旧权益
-> 发放新权益
```

本案例中，降级套餐生效后默认重新开启一个低等级套餐周期：

```text
new_start_time = now()
new_end_time = now() + target_plan.period_days
```

核心落库行为：

| 数据表                    | 操作                               |
| ------------------------- | ---------------------------------- |
| `user_subscription`       | 更新订阅状态，或执行降级切换       |
| `user_benefit`            | 回收过期权益，或重新发放降级后权益 |
| `subscription_change_log` | 记录到期、回收、降级生效流水       |

关键控制点：

| 控制点       | 处理方式                           |
| ------------ | ---------------------------------- |
| 任务重复执行 | 状态条件控制，只处理 `ACTIVE`      |
| 任务执行失败 | 下次任务继续扫描补偿               |
| 权益回收失败 | 事务回滚，避免订阅过期但权益仍有效 |
| 降级生效失败 | 保持 pending 字段，下次继续补偿    |

## 数据库表设计

数据库设计以“套餐配置”和“用户订阅状态”为核心。套餐表负责定义卖什么，套餐权益表负责定义包含什么，用户订阅表负责记录用户当前会员状态，用户权益表负责记录用户真正拥有的权益，订阅变更记录表负责审计购买、续费、升级、降级、到期等行为。

### 会员套餐表

会员套餐表用于维护可售卖的会员套餐，例如月度基础版、年度专业版、企业版等。后续购买、续费、升降级都会读取这张表。

```sql
CREATE TABLE member_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_code VARCHAR(64) NOT NULL COMMENT '套餐编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '套餐名称',
    level INT NOT NULL COMMENT '套餐等级，数字越大等级越高',
    price DECIMAL(10, 2) NOT NULL COMMENT '套餐价格',
    period_days INT NOT NULL COMMENT '套餐周期天数',
    description VARCHAR(512) DEFAULT NULL COMMENT '套餐描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_code (plan_code),
    KEY idx_status_level (status, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐表';
```

示例数据：

```sql
INSERT INTO member_plan
(plan_code, plan_name, level, price, period_days, description, status, sort)
VALUES
('BASIC_MONTH', '基础版月卡', 1, 29.00, 30, '适合个人轻量使用', 1, 1),
('PRO_MONTH', '专业版月卡', 2, 59.00, 30, '适合高频个人用户', 1, 2),
('TEAM_MONTH', '团队版月卡', 3, 199.00, 30, '适合小团队协作使用', 1, 3);
```

字段说明：

| 字段          | 说明                                        |
| ------------- | ------------------------------------------- |
| `plan_code`   | 套餐业务编码，接口传参建议使用编码而不是 ID |
| `level`       | 用于判断升级和降级                          |
| `price`       | 用于购买、续费、升级补差价                  |
| `period_days` | 用于计算会员有效期                          |
| `status`      | 下架套餐不能购买、续费、升级或降级          |

### 套餐权益表

套餐权益表用于配置某个套餐包含哪些权益。一个套餐可以包含多个权益，例如导出报表、创建项目数量、团队成员数量、AI 调用额度等。

```sql
CREATE TABLE member_plan_benefit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '套餐ID',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码',
    benefit_name VARCHAR(128) NOT NULL COMMENT '权益名称',
    benefit_value VARCHAR(128) NOT NULL COMMENT '权益值，例如次数、容量、开关值',
    unit VARCHAR(32) DEFAULT NULL COMMENT '权益单位，例如次、GB、人',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_benefit (plan_id, benefit_code),
    KEY idx_benefit_code (benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐权益表';
```

示例数据：

```sql
INSERT INTO member_plan_benefit
(plan_id, benefit_code, benefit_name, benefit_value, unit, status)
VALUES
(1, 'PROJECT_LIMIT', '项目数量上限', '3', '个', 1),
(1, 'EXPORT_REPORT', '报表导出', 'false', NULL, 1),
(2, 'PROJECT_LIMIT', '项目数量上限', '20', '个', 1),
(2, 'EXPORT_REPORT', '报表导出', 'true', NULL, 1),
(3, 'PROJECT_LIMIT', '项目数量上限', '100', '个', 1),
(3, 'TEAM_MEMBER_LIMIT', '团队成员上限', '20', '人', 1),
(3, 'EXPORT_REPORT', '报表导出', 'true', NULL, 1);
```

字段说明：

| 字段              | 说明                                       |
| ----------------- | ------------------------------------------ |
| `benefit_code`    | 权益唯一编码，业务鉴权时通常根据它判断     |
| `benefit_value`   | 权益值，建议用字符串保存，业务侧按类型解析 |
| `unit`            | 展示用单位，不参与核心计算                 |
| `uk_plan_benefit` | 防止同一个套餐重复配置同一个权益           |

### 用户会员订阅表

用户会员订阅表用于记录用户当前会员订阅状态。为了简化实现，本案例采用“一个用户一条当前订阅记录”的模型。

```sql
CREATE TABLE user_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_id BIGINT NOT NULL COMMENT '当前套餐ID',
    plan_code VARCHAR(64) NOT NULL COMMENT '当前套餐编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '当前套餐名称',
    status TINYINT NOT NULL COMMENT '订阅状态：1-生效中，2-已过期，3-已取消',
    start_time DATETIME NOT NULL COMMENT '会员开始时间',
    end_time DATETIME NOT NULL COMMENT '会员到期时间',
    renew_count INT NOT NULL DEFAULT 0 COMMENT '续费次数',
    pending_plan_id BIGINT DEFAULT NULL COMMENT '待生效套餐ID，降级时使用',
    pending_plan_code VARCHAR(64) DEFAULT NULL COMMENT '待生效套餐编码',
    pending_plan_name VARCHAR(128) DEFAULT NULL COMMENT '待生效套餐名称',
    pending_plan_effect_time DATETIME DEFAULT NULL COMMENT '待生效时间',
    last_remind_time DATETIME DEFAULT NULL COMMENT '最近一次到期提醒时间',
    last_order_no VARCHAR(64) DEFAULT NULL COMMENT '最近一次业务订单号',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status_end_time (status, end_time),
    KEY idx_pending_effect_time (pending_plan_effect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员订阅表';
```

字段说明：

| 字段                       | 说明                                     |
| -------------------------- | ---------------------------------------- |
| `user_id`                  | 用户唯一订阅记录，简化当前会员判断       |
| `status`                   | 当前订阅状态，定时任务只扫描生效中的订阅 |
| `start_time`               | 当前会员周期开始时间                     |
| `end_time`                 | 当前会员周期结束时间                     |
| `pending_plan_id`          | 降级预约套餐，不立即生效                 |
| `pending_plan_effect_time` | 降级生效时间，通常等于当前到期时间       |
| `last_order_no`            | 最近一次订单号，方便排查问题             |

订阅状态建议定义：

```text
1 = ACTIVE  生效中
2 = EXPIRED 已过期
3 = CANCELED 已取消
```

### 用户权益表

用户权益表用于记录用户实际拥有的权益。业务系统判断用户是否能使用某个功能时，应该优先查这张表或它的 Redis 缓存，而不是每次反查套餐配置。

```sql
CREATE TABLE user_benefit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subscription_id BIGINT NOT NULL COMMENT '用户订阅ID',
    plan_id BIGINT NOT NULL COMMENT '套餐ID',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码',
    benefit_name VARCHAR(128) NOT NULL COMMENT '权益名称',
    benefit_value VARCHAR(128) NOT NULL COMMENT '权益值',
    unit VARCHAR(32) DEFAULT NULL COMMENT '权益单位',
    status TINYINT NOT NULL COMMENT '权益状态：1-生效中，2-已过期，3-已回收',
    start_time DATETIME NOT NULL COMMENT '权益开始时间',
    end_time DATETIME NOT NULL COMMENT '权益结束时间',
    source_order_no VARCHAR(64) NOT NULL COMMENT '来源订单号',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_benefit_order (user_id, benefit_code, source_order_no),
    KEY idx_user_status (user_id, status),
    KEY idx_subscription_id (subscription_id),
    KEY idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益表';
```

字段说明：

| 字段              | 说明                         |
| ----------------- | ---------------------------- |
| `subscription_id` | 关联用户订阅，方便批量回收   |
| `benefit_code`    | 实际鉴权使用的权益编码       |
| `benefit_value`   | 实际鉴权使用的权益值         |
| `status`          | 生效、过期、回收             |
| `source_order_no` | 用于防止同一订单重复发放权益 |

权益状态建议定义：

```text
1 = ACTIVE  生效中
2 = EXPIRED 已过期
3 = REVOKED 已回收
```

### 订阅变更记录表

订阅变更记录表用于记录用户订阅生命周期中的关键动作，包括购买、续费、升级、降级预约、降级生效、到期、取消等。它不直接参与权限判断，但对排查问题和审计非常重要。

```sql
CREATE TABLE subscription_change_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    change_no VARCHAR(64) NOT NULL COMMENT '变更流水号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subscription_id BIGINT DEFAULT NULL COMMENT '用户订阅ID',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '业务订单号',
    change_type VARCHAR(32) NOT NULL COMMENT '变更类型：BUY-购买，RENEW-续费，UPGRADE-升级，DOWNGRADE-降级预约，DOWNGRADE_EFFECT-降级生效，EXPIRE-到期',
    before_plan_id BIGINT DEFAULT NULL COMMENT '变更前套餐ID',
    before_plan_name VARCHAR(128) DEFAULT NULL COMMENT '变更前套餐名称',
    after_plan_id BIGINT DEFAULT NULL COMMENT '变更后套餐ID',
    after_plan_name VARCHAR(128) DEFAULT NULL COMMENT '变更后套餐名称',
    before_end_time DATETIME DEFAULT NULL COMMENT '变更前到期时间',
    after_end_time DATETIME DEFAULT NULL COMMENT '变更后到期时间',
    amount DECIMAL(10, 2) DEFAULT NULL COMMENT '本次变更金额',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_change_no (change_no),
    UNIQUE KEY uk_order_change_type (order_no, change_type),
    KEY idx_user_id (user_id),
    KEY idx_subscription_id (subscription_id),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅变更记录表';
```

字段说明：

| 字段                   | 说明                               |
| ---------------------- | ---------------------------------- |
| `change_no`            | 系统生成的变更流水号               |
| `order_no`             | 支付订单号或业务订单号             |
| `change_type`          | 订阅变更类型                       |
| `before_plan_id`       | 变更前套餐                         |
| `after_plan_id`        | 变更后套餐                         |
| `before_end_time`      | 变更前到期时间                     |
| `after_end_time`       | 变更后到期时间                     |
| `amount`               | 购买金额、续费金额或升级补差价     |
| `uk_order_change_type` | 控制同一订单同一变更类型只处理一次 |

变更类型建议定义：

```text
BUY              购买开通
RENEW            续费
UPGRADE          升级
DOWNGRADE        降级预约
DOWNGRADE_EFFECT 降级生效
EXPIRE           到期
CANCEL           取消
```

这 5 张表之间的核心关系如下：

```text
member_plan 1 -> N member_plan_benefit

user_subscription N -> 1 member_plan

user_subscription 1 -> N user_benefit

user_subscription 1 -> N subscription_change_log
```

后续代码实现时，主要围绕 `user_subscription` 做状态变更，围绕 `user_benefit` 做权益发放和回收，围绕 `subscription_change_log` 做幂等和审计。

## 核心状态与枚举设计

会员订阅系统最容易出问题的地方是状态边界不清晰。本模块把订阅状态、套餐变更类型、权益状态拆成独立枚举，后续 Service 层只围绕这些枚举做状态判断，避免代码里到处散落魔法值。会员订阅场景本身重点关注有效期叠加、升级降级、权益开通、权益回收、订阅取消和到期补偿。

### 订阅状态枚举

订阅状态用于描述用户当前会员订阅的生命周期。定时任务、购买、续费、升级、降级都需要根据订阅状态判断是否允许继续操作。

状态流转建议如下：

```text
未开通
-> ACTIVE 生效中
-> EXPIRED 已过期

ACTIVE 生效中
-> CANCELED 已取消
```

其中：

| 状态     | 编码 | 说明                                     |
| -------- | ---- | ---------------------------------------- |
| ACTIVE   | 1    | 当前订阅正在生效                         |
| EXPIRED  | 2    | 当前订阅已过期，权益应被回收             |
| CANCELED | 3    | 用户主动取消订阅，不再自动续费或继续变更 |

订阅状态枚举类放在 `src/main/java/io/github/atengk/member/enums/SubscriptionStatusEnum.java`。

```java
package io.github.atengk.member.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户会员订阅状态枚举。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum SubscriptionStatusEnum {

    ACTIVE(1, "生效中"),

    EXPIRED(2, "已过期"),

    CANCELED(3, "已取消");

    private final Integer code;

    private final String desc;

    /**
     * 根据编码获取枚举。
     *
     * @param code 状态编码
     * @return 订阅状态枚举
     */
    public static SubscriptionStatusEnum of(Integer code) {
        for (SubscriptionStatusEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知订阅状态：" + code);
    }
}
```

实际业务判断时，不建议直接写 `status == 1`，而是统一使用枚举值：

```java
if (!SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus())) {
    throw new BizException("当前会员订阅不是生效状态");
}
```

### 套餐变更类型枚举

套餐变更类型用于记录会员订阅生命周期中的关键动作，主要写入 `subscription_change_log.change_type` 字段。它不直接控制用户权益，但用于幂等、审计、排查和补偿。

变更类型建议如下：

| 类型             | 说明     | 是否依赖订单号 |
| ---------------- | -------- | -------------- |
| BUY              | 购买开通 | 是             |
| RENEW            | 续费     | 是             |
| UPGRADE          | 升级套餐 | 是             |
| DOWNGRADE        | 降级预约 | 否             |
| DOWNGRADE_EFFECT | 降级生效 | 否             |
| EXPIRE           | 到期回收 | 否             |
| CANCEL           | 取消订阅 | 否             |

套餐变更类型枚举类放在 `src/main/java/io/github/atengk/member/enums/ChangeTypeEnum.java`。

```java
package io.github.atengk.member.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会员订阅变更类型枚举。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum ChangeTypeEnum {

    BUY("BUY", "购买开通"),

    RENEW("RENEW", "续费"),

    UPGRADE("UPGRADE", "升级套餐"),

    DOWNGRADE("DOWNGRADE", "降级预约"),

    DOWNGRADE_EFFECT("DOWNGRADE_EFFECT", "降级生效"),

    EXPIRE("EXPIRE", "到期回收"),

    CANCEL("CANCEL", "取消订阅");

    private final String code;

    private final String desc;

    /**
     * 根据编码获取枚举。
     *
     * @param code 变更类型编码
     * @return 变更类型枚举
     */
    public static ChangeTypeEnum of(String code) {
        for (ChangeTypeEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知会员变更类型：" + code);
    }
}
```

推荐在订阅变更记录表中用 `change_type` + `order_no` 做幂等控制：

```text
同一个 order_no + BUY 只能处理一次
同一个 order_no + RENEW 只能处理一次
同一个 order_no + UPGRADE 只能处理一次
```

这样可以防止支付回调重复通知时，重复开通、重复续费或重复升级。

### 权益状态枚举

权益状态用于描述用户实际拥有的功能权益是否仍然可用。业务鉴权时应以 `user_benefit` 中的生效权益为准，而不是只看用户订阅表。

权益状态建议如下：

| 状态    | 编码 | 说明                                   |
| ------- | ---- | -------------------------------------- |
| ACTIVE  | 1    | 权益生效中，可以使用                   |
| EXPIRED | 2    | 权益因订阅到期而失效                   |
| REVOKED | 3    | 权益因升级、取消、回收等原因被主动回收 |

权益状态枚举类放在 `src/main/java/io/github/atengk/member/enums/BenefitStatusEnum.java`。

```java
package io.github.atengk.member.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户会员权益状态枚举。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum BenefitStatusEnum {

    ACTIVE(1, "生效中"),

    EXPIRED(2, "已过期"),

    REVOKED(3, "已回收");

    private final Integer code;

    private final String desc;

    /**
     * 根据编码获取枚举。
     *
     * @param code 权益状态编码
     * @return 权益状态枚举
     */
    public static BenefitStatusEnum of(Integer code) {
        for (BenefitStatusEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知权益状态：" + code);
    }
}
```

业务系统判断用户是否拥有某个权益时，只查询 `ACTIVE` 状态的权益：

```text
user_id = 当前用户
benefit_code = 目标权益编码
status = ACTIVE
start_time <= now
end_time > now
```

常见权益判断示例：

```text
EXPORT_REPORT = true        允许导出报表
PROJECT_LIMIT = 20          最多创建 20 个项目
TEAM_MEMBER_LIMIT = 20      最多邀请 20 个团队成员
```

## 后端项目结构

本模块采用常规 Spring Boot 分层结构。Controller 只处理接口入参和返回结果，Service 承载核心订阅业务，Mapper 负责数据库访问，Job 负责到期补偿和定时任务。

推荐项目结构如下：

```text
src/main/java/io/github/atengk/member
├── controller
│   └── MemberSubscriptionController.java
├── service
│   ├── MemberSubscriptionService.java
│   └── impl
│       └── MemberSubscriptionServiceImpl.java
├── mapper
│   ├── MemberPlanMapper.java
│   ├── MemberPlanBenefitMapper.java
│   ├── UserSubscriptionMapper.java
│   ├── UserBenefitMapper.java
│   └── SubscriptionChangeLogMapper.java
├── entity
│   ├── MemberPlan.java
│   ├── MemberPlanBenefit.java
│   ├── UserSubscription.java
│   ├── UserBenefit.java
│   └── SubscriptionChangeLog.java
├── dto
│   ├── SubscribeRequest.java
│   ├── RenewRequest.java
│   ├── UpgradeRequest.java
│   └── DowngradeRequest.java
├── vo
│   └── UserSubscriptionVO.java
├── enums
│   ├── SubscriptionStatusEnum.java
│   ├── ChangeTypeEnum.java
│   └── BenefitStatusEnum.java
├── job
│   └── MemberSubscriptionJob.java
└── common
    └── BizException.java
```

### Controller 接口层

Controller 层负责对外暴露会员订阅接口。它只做参数接收、登录用户获取和 Service 调用，不写复杂业务逻辑。

核心接口建议如下：

| 接口                             | 方法 | 说明               |
| -------------------------------- | ---- | ------------------ |
| `/member/plans`                  | GET  | 查询可购买套餐列表 |
| `/member/subscription/current`   | GET  | 查询当前会员订阅   |
| `/member/subscription/subscribe` | POST | 购买并开通会员     |
| `/member/subscription/renew`     | POST | 续费当前套餐       |
| `/member/subscription/upgrade`   | POST | 升级套餐           |
| `/member/subscription/downgrade` | POST | 预约降级套餐       |

Controller 层示例职责：

```text
获取当前登录用户ID
-> 校验请求参数格式
-> 调用 Service 业务方法
-> 返回统一结果
```

不建议在 Controller 中处理这些逻辑：

```text
不计算会员有效期
不判断套餐等级
不发放权益
不写订阅变更记录
不处理分布式锁
```

### Service 业务层

Service 层是本案例的核心，负责会员订阅生命周期的全部状态变更。

接口层建议定义在 `MemberSubscriptionService` 中：

```text
listEnabledPlans()              查询可用套餐
getCurrentSubscription()        查询当前订阅
subscribe()                     购买开通会员
renew()                         续费会员
upgrade()                       升级套餐
downgrade()                     预约降级套餐
expireSubscriptions()           到期回收权益
effectPendingDowngrade()        降级生效
```

Service 实现层需要重点处理这些问题：

| 问题             | 处理方式                                     |
| ---------------- | -------------------------------------------- |
| 同一用户并发操作 | Redisson 分布式锁                            |
| 支付回调重复     | `order_no + change_type` 唯一索引            |
| 续费有效期计算   | 未过期从原到期时间叠加，已过期从当前时间计算 |
| 升级套餐         | 当前周期立即切换套餐和权益                   |
| 降级套餐         | 写入待生效套餐，到期任务处理                 |
| 权益一致性       | 订阅变更和权益变更放在同一个事务中           |
| 异常回滚         | Service 方法加 `@Transactional`              |

Service 层后续会重点实现这些私有方法：

```text
checkOrderProcessed()       校验订单是否已处理
getEnabledPlan()            获取启用套餐
calculateEndTime()          计算到期时间
grantBenefits()             发放权益
revokeBenefits()            回收权益
writeChangeLog()            写入变更记录
```

### Mapper 持久层

Mapper 层使用 MyBatis-Plus。常规 CRUD 通过 `BaseMapper` 完成，复杂条件更新可以在 Service 层使用 `LambdaUpdateWrapper` 实现。

Mapper 类只需要保持简洁：

```text
MemberPlanMapper                  会员套餐表
MemberPlanBenefitMapper           套餐权益表
UserSubscriptionMapper            用户订阅表
UserBenefitMapper                 用户权益表
SubscriptionChangeLogMapper       订阅变更记录表
```

Mapper 接口示例结构如下：

```java
package io.github.atengk.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.member.entity.UserSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户会员订阅 Mapper。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface UserSubscriptionMapper extends BaseMapper<UserSubscription> {
}
```

本案例的查询尽量保持在 Service 层用 MyBatis-Plus Wrapper 完成，例如：

```text
根据用户 ID 查询当前订阅
根据套餐 ID 查询套餐权益
扫描即将到期订阅
扫描已经到期订阅
查询订单是否已经处理
```

### Entity 实体层

Entity 层和数据库表一一对应。建议所有实体统一包含这些基础字段：

```text
id
created_time
updated_time
deleted
```

实体类建议使用 MyBatis-Plus 注解：

| 注解          | 用途               |
| ------------- | ------------------ |
| `@TableName`  | 绑定数据库表名     |
| `@TableId`    | 标识主键           |
| `@TableLogic` | 标识逻辑删除字段   |
| `@TableField` | 字段映射或填充控制 |

实体类不建议写业务逻辑，只保留数据字段。业务判断统一放到 Service 层，避免实体变成贫血模型和充血模型混用。

主要实体说明：

| 实体                    | 对应表                    | 说明         |
| ----------------------- | ------------------------- | ------------ |
| `MemberPlan`            | `member_plan`             | 套餐基础信息 |
| `MemberPlanBenefit`     | `member_plan_benefit`     | 套餐权益配置 |
| `UserSubscription`      | `user_subscription`       | 用户当前订阅 |
| `UserBenefit`           | `user_benefit`            | 用户实际权益 |
| `SubscriptionChangeLog` | `subscription_change_log` | 订阅变更流水 |

后续实现时，`UserSubscription` 是最核心实体，大部分状态流转都会围绕它展开。

### DTO 与 VO 对象

DTO 用于接收前端请求，VO 用于返回前端展示数据。不要直接用 Entity 接收接口参数，也不要直接把 Entity 返回给前端。

DTO 建议如下：

| DTO                | 用途         |
| ------------------ | ------------ |
| `SubscribeRequest` | 购买开通会员 |
| `RenewRequest`     | 续费会员     |
| `UpgradeRequest`   | 升级套餐     |
| `DowngradeRequest` | 预约降级套餐 |

DTO 关键字段建议：

```text
planCode      套餐编码
orderNo       业务订单号
amount        支付金额或补差价金额
```

其中 `orderNo` 是购买、续费、升级的关键幂等字段。

VO 建议如下：

| VO                   | 用途                                             |
| -------------------- | ------------------------------------------------ |
| `UserSubscriptionVO` | 返回用户当前订阅、到期时间、待生效套餐、权益列表 |

`UserSubscriptionVO` 建议包含：

```text
userId
planCode
planName
status
statusName
startTime
endTime
pendingPlanCode
pendingPlanName
pendingPlanEffectTime
benefits
```

其中 `benefits` 可以是当前生效权益列表，用于前端展示会员能力。

### Job 定时任务层

Job 层负责处理异步补偿类任务。会员订阅场景不能只依赖用户请求触发状态变化，因为用户到期、降级生效、权益回收都可能发生在用户不访问系统的时候。

本案例建议拆成三个任务：

| 任务         | 执行频率      | 说明                         |
| ------------ | ------------- | ---------------------------- |
| 到期提醒任务 | 每天一次      | 扫描未来 3 天内到期的会员    |
| 到期回收任务 | 每 5 分钟一次 | 扫描已过期订阅并回收权益     |
| 降级生效任务 | 每 5 分钟一次 | 扫描待生效降级套餐并切换权益 |

Job 层只负责调度入口，不直接写复杂业务逻辑。推荐结构如下：

```text
XXL-JOB 触发
-> MemberSubscriptionJob 接收调度
-> 调用 MemberSubscriptionService
-> Service 内部开启事务并处理业务
```

Job 方法建议如下：

```text
remindExpiringSubscriptions()       到期提醒
expireSubscriptions()               到期回收
effectPendingDowngrade()            降级生效
```

Job 层需要重点记录日志：

```text
任务开始
扫描数量
成功数量
失败数量
异常原因
任务结束
```

这样排查“用户已经到期但权益还在”“降级没有生效”“到期提醒没有发送”等问题时会更直接。

## 核心代码实现

本节代码建立在前面的表结构、枚举和项目分层之上，重点实现会员订阅生命周期的核心业务：购买开通、续费叠加、升级即时生效、降级延后生效、权益发放回收、到期补偿。该场景对应 README 中“会员订阅与权益生命周期”的核心链路。

为了避免代码过于分散，下面只给出关键可落地代码。Entity 和 Mapper 按前面数据库表结构使用 MyBatis-Plus 常规生成即可。

### 套餐购买与开通

购买开通一般由“支付成功回调”触发。这里为了聚焦会员订阅核心逻辑，Controller 直接接收 `orderNo`，表示该订单已经支付成功。

核心规则：

```text
1. orderNo + BUY 防重复开通
2. 同一用户购买时加 Redisson 分布式锁
3. 已有生效会员时，不允许走购买开通，应走续费或升级
4. 会员已过期时，可以重新购买并刷新订阅周期
5. 开通成功后发放套餐权益
```

文件位置：`src/main/java/io/github/atengk/member/service/MemberSubscriptionService.java`

```java
package io.github.atengk.member.service;

import io.github.atengk.member.dto.DowngradeRequest;
import io.github.atengk.member.dto.RenewRequest;
import io.github.atengk.member.dto.SubscribeRequest;
import io.github.atengk.member.dto.UpgradeRequest;
import io.github.atengk.member.entity.MemberPlan;
import io.github.atengk.member.vo.UserSubscriptionVO;

import java.util.List;

/**
 * 会员订阅业务接口。
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MemberSubscriptionService {

    /**
     * 查询启用套餐列表。
     *
     * @return 套餐列表
     */
    List<MemberPlan> listEnabledPlans();

    /**
     * 查询用户当前会员信息。
     *
     * @param userId 用户ID
     * @return 当前会员信息
     */
    UserSubscriptionVO getCurrentSubscription(Long userId);

    /**
     * 购买并开通会员。
     *
     * @param userId 用户ID
     * @param request 购买请求
     */
    void subscribe(Long userId, SubscribeRequest request);

    /**
     * 续费会员。
     *
     * @param userId 用户ID
     * @param request 续费请求
     */
    void renew(Long userId, RenewRequest request);

    /**
     * 升级会员套餐。
     *
     * @param userId 用户ID
     * @param request 升级请求
     */
    void upgrade(Long userId, UpgradeRequest request);

    /**
     * 预约降级会员套餐。
     *
     * @param userId 用户ID
     * @param request 降级请求
     */
    void downgrade(Long userId, DowngradeRequest request);

    /**
     * 处理已到期订阅。
     *
     * @return 处理数量
     */
    int expireSubscriptions();

    /**
     * 处理待生效降级套餐。
     *
     * @return 处理数量
     */
    int effectPendingDowngrade();

    /**
     * 处理即将到期提醒。
     *
     * @return 处理数量
     */
    int remindExpiringSubscriptions();
}
```

下面是会员订阅 Service 的核心实现，包含购买、续费、升级、降级、权益发放回收和到期补偿逻辑。

文件位置：`src/main/java/io/github/atengk/member/service/impl/MemberSubscriptionServiceImpl.java`

```java
package io.github.atengk.member.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.member.common.BizException;
import io.github.atengk.member.dto.DowngradeRequest;
import io.github.atengk.member.dto.RenewRequest;
import io.github.atengk.member.dto.SubscribeRequest;
import io.github.atengk.member.dto.UpgradeRequest;
import io.github.atengk.member.entity.MemberPlan;
import io.github.atengk.member.entity.MemberPlanBenefit;
import io.github.atengk.member.entity.SubscriptionChangeLog;
import io.github.atengk.member.entity.UserBenefit;
import io.github.atengk.member.entity.UserSubscription;
import io.github.atengk.member.enums.BenefitStatusEnum;
import io.github.atengk.member.enums.ChangeTypeEnum;
import io.github.atengk.member.enums.SubscriptionStatusEnum;
import io.github.atengk.member.mapper.MemberPlanBenefitMapper;
import io.github.atengk.member.mapper.MemberPlanMapper;
import io.github.atengk.member.mapper.SubscriptionChangeLogMapper;
import io.github.atengk.member.mapper.UserBenefitMapper;
import io.github.atengk.member.mapper.UserSubscriptionMapper;
import io.github.atengk.member.service.MemberSubscriptionService;
import io.github.atengk.member.vo.UserSubscriptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 会员订阅业务实现。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSubscriptionServiceImpl implements MemberSubscriptionService {

    private final MemberPlanMapper memberPlanMapper;
    private final MemberPlanBenefitMapper memberPlanBenefitMapper;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final UserBenefitMapper userBenefitMapper;
    private final SubscriptionChangeLogMapper changeLogMapper;
    private final RedissonClient redissonClient;

    @Override
    public List<MemberPlan> listEnabledPlans() {
        return memberPlanMapper.selectList(Wrappers.<MemberPlan>lambdaQuery()
                .eq(MemberPlan::getStatus, 1)
                .orderByAsc(MemberPlan::getSort)
                .orderByAsc(MemberPlan::getLevel));
    }

    @Override
    public UserSubscriptionVO getCurrentSubscription(Long userId) {
        UserSubscription subscription = getUserSubscription(userId);
        if (subscription == null) {
            return UserSubscriptionVO.empty(userId);
        }

        List<UserBenefit> benefits = userBenefitMapper.selectList(Wrappers.<UserBenefit>lambdaQuery()
                .eq(UserBenefit::getUserId, userId)
                .eq(UserBenefit::getSubscriptionId, subscription.getId())
                .eq(UserBenefit::getStatus, BenefitStatusEnum.ACTIVE.getCode())
                .le(UserBenefit::getStartTime, DateUtil.date())
                .gt(UserBenefit::getEndTime, DateUtil.date()));

        return UserSubscriptionVO.of(subscription, benefits);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(Long userId, SubscribeRequest request) {
        executeWithUserLock(userId, () -> {
            if (isOrderProcessed(request.getOrderNo(), ChangeTypeEnum.BUY)) {
                log.info("购买开通请求重复，直接忽略，用户ID：{}，订单号：{}", userId, request.getOrderNo());
                return;
            }

            MemberPlan plan = getEnabledPlanByCode(request.getPlanCode());
            checkAmount(plan.getPrice(), request.getAmount(), "支付金额和套餐价格不一致");

            UserSubscription subscription = getUserSubscription(userId);
            Date now = DateUtil.date();

            if (subscription != null
                    && SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus())
                    && subscription.getEndTime().after(now)) {
                throw new BizException("当前会员仍在有效期内，请使用续费或升级");
            }

            Date endTime = DateUtil.offsetDay(now, plan.getPeriodDays());
            UserSubscription savedSubscription = saveOrRefreshSubscription(userId, subscription, plan, now, endTime, request.getOrderNo());

            revokeBenefits(savedSubscription.getId(), BenefitStatusEnum.EXPIRED, "重新购买前回收旧权益");
            grantBenefits(userId, savedSubscription, plan, now, endTime, request.getOrderNo());

            writeChangeLog(userId, savedSubscription.getId(), request.getOrderNo(), ChangeTypeEnum.BUY,
                    null, plan, null, endTime, request.getAmount(), "购买开通会员");

            log.info("会员购买开通成功，用户ID：{}，套餐：{}，到期时间：{}", userId, plan.getPlanName(), DateUtil.formatDateTime(endTime));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renew(Long userId, RenewRequest request) {
        executeWithUserLock(userId, () -> {
            if (isOrderProcessed(request.getOrderNo(), ChangeTypeEnum.RENEW)) {
                log.info("会员续费请求重复，直接忽略，用户ID：{}，订单号：{}", userId, request.getOrderNo());
                return;
            }

            MemberPlan plan = getEnabledPlanByCode(request.getPlanCode());
            checkAmount(plan.getPrice(), request.getAmount(), "支付金额和续费套餐价格不一致");

            UserSubscription subscription = getRequiredSubscription(userId);
            if (!Objects.equals(subscription.getPlanCode(), plan.getPlanCode())) {
                throw new BizException("续费套餐必须和当前套餐一致，如需变更套餐请使用升级或降级");
            }

            Date now = DateUtil.date();
            boolean stillActive = SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus())
                    && subscription.getEndTime().after(now);

            Date beforeEndTime = subscription.getEndTime();
            Date baseTime = stillActive ? subscription.getEndTime() : now;
            Date newEndTime = DateUtil.offsetDay(baseTime, plan.getPeriodDays());

            userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                    .eq(UserSubscription::getId, subscription.getId())
                    .set(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                    .set(!stillActive, UserSubscription::getStartTime, now)
                    .set(UserSubscription::getEndTime, newEndTime)
                    .set(UserSubscription::getRenewCount, subscription.getRenewCount() + 1)
                    .set(UserSubscription::getLastOrderNo, request.getOrderNo()));

            UserSubscription updatedSubscription = userSubscriptionMapper.selectById(subscription.getId());

            if (stillActive) {
                extendBenefits(updatedSubscription.getId(), newEndTime);
            } else {
                revokeBenefits(updatedSubscription.getId(), BenefitStatusEnum.EXPIRED, "过期续费前回收旧权益");
                grantBenefits(userId, updatedSubscription, plan, now, newEndTime, request.getOrderNo());
            }

            writeChangeLog(userId, updatedSubscription.getId(), request.getOrderNo(), ChangeTypeEnum.RENEW,
                    plan, plan, beforeEndTime, newEndTime, request.getAmount(), "会员续费");

            log.info("会员续费成功，用户ID：{}，套餐：{}，新到期时间：{}", userId, plan.getPlanName(), DateUtil.formatDateTime(newEndTime));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upgrade(Long userId, UpgradeRequest request) {
        executeWithUserLock(userId, () -> {
            if (isOrderProcessed(request.getOrderNo(), ChangeTypeEnum.UPGRADE)) {
                log.info("会员升级请求重复，直接忽略，用户ID：{}，订单号：{}", userId, request.getOrderNo());
                return;
            }

            UserSubscription subscription = getRequiredActiveSubscription(userId);
            MemberPlan currentPlan = memberPlanMapper.selectById(subscription.getPlanId());
            MemberPlan targetPlan = getEnabledPlanByCode(request.getTargetPlanCode());

            if (targetPlan.getLevel() <= currentPlan.getLevel()) {
                throw new BizException("目标套餐等级必须高于当前套餐");
            }

            BigDecimal upgradeAmount = calculateUpgradeAmount(currentPlan, targetPlan, subscription.getEndTime());
            checkAmount(upgradeAmount, request.getAmount(), "支付金额和升级补差价不一致");

            Date beforeEndTime = subscription.getEndTime();

            userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                    .eq(UserSubscription::getId, subscription.getId())
                    .set(UserSubscription::getPlanId, targetPlan.getId())
                    .set(UserSubscription::getPlanCode, targetPlan.getPlanCode())
                    .set(UserSubscription::getPlanName, targetPlan.getPlanName())
                    .set(UserSubscription::getLastOrderNo, request.getOrderNo()));

            UserSubscription updatedSubscription = userSubscriptionMapper.selectById(subscription.getId());

            revokeBenefits(subscription.getId(), BenefitStatusEnum.REVOKED, "升级套餐回收旧权益");
            grantBenefits(userId, updatedSubscription, targetPlan, DateUtil.date(), updatedSubscription.getEndTime(), request.getOrderNo());

            writeChangeLog(userId, updatedSubscription.getId(), request.getOrderNo(), ChangeTypeEnum.UPGRADE,
                    currentPlan, targetPlan, beforeEndTime, updatedSubscription.getEndTime(), request.getAmount(), "会员套餐升级");

            log.info("会员升级成功，用户ID：{}，原套餐：{}，目标套餐：{}",
                    userId, currentPlan.getPlanName(), targetPlan.getPlanName());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downgrade(Long userId, DowngradeRequest request) {
        executeWithUserLock(userId, () -> {
            UserSubscription subscription = getRequiredActiveSubscription(userId);
            MemberPlan currentPlan = memberPlanMapper.selectById(subscription.getPlanId());
            MemberPlan targetPlan = getEnabledPlanByCode(request.getTargetPlanCode());

            if (targetPlan.getLevel() >= currentPlan.getLevel()) {
                throw new BizException("目标套餐等级必须低于当前套餐");
            }

            userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                    .eq(UserSubscription::getId, subscription.getId())
                    .set(UserSubscription::getPendingPlanId, targetPlan.getId())
                    .set(UserSubscription::getPendingPlanCode, targetPlan.getPlanCode())
                    .set(UserSubscription::getPendingPlanName, targetPlan.getPlanName())
                    .set(UserSubscription::getPendingPlanEffectTime, subscription.getEndTime()));

            writeChangeLog(userId, subscription.getId(), null, ChangeTypeEnum.DOWNGRADE,
                    currentPlan, targetPlan, subscription.getEndTime(), subscription.getEndTime(), BigDecimal.ZERO, "预约降级会员套餐");

            log.info("会员降级预约成功，用户ID：{}，当前套餐：{}，待生效套餐：{}，生效时间：{}",
                    userId, currentPlan.getPlanName(), targetPlan.getPlanName(), DateUtil.formatDateTime(subscription.getEndTime()));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireSubscriptions() {
        Date now = DateUtil.date();
        List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .le(UserSubscription::getEndTime, now)
                .last("LIMIT 100"));

        if (CollUtil.isEmpty(subscriptions)) {
            return 0;
        }

        int successCount = 0;
        for (UserSubscription subscription : subscriptions) {
            try {
                executeWithUserLock(subscription.getUserId(), () -> expireOneSubscription(subscription.getId()));
                successCount++;
            } catch (Exception ex) {
                log.error("会员到期处理失败，用户ID：{}，订阅ID：{}，原因：{}",
                        subscription.getUserId(), subscription.getId(), ex.getMessage(), ex);
            }
        }

        log.info("会员到期回收任务完成，扫描数量：{}，成功数量：{}", subscriptions.size(), successCount);
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int effectPendingDowngrade() {
        Date now = DateUtil.date();
        List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .isNotNull(UserSubscription::getPendingPlanId)
                .le(UserSubscription::getPendingPlanEffectTime, now)
                .last("LIMIT 100"));

        if (CollUtil.isEmpty(subscriptions)) {
            return 0;
        }

        int successCount = 0;
        for (UserSubscription subscription : subscriptions) {
            try {
                executeWithUserLock(subscription.getUserId(), () -> effectDowngrade(subscription.getId()));
                successCount++;
            } catch (Exception ex) {
                log.error("会员降级生效失败，用户ID：{}，订阅ID：{}，原因：{}",
                        subscription.getUserId(), subscription.getId(), ex.getMessage(), ex);
            }
        }

        log.info("会员降级生效任务完成，扫描数量：{}，成功数量：{}", subscriptions.size(), successCount);
        return successCount;
    }

    @Override
    public int remindExpiringSubscriptions() {
        Date now = DateUtil.date();
        Date remindEndTime = DateUtil.offsetDay(now, 3);

        List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .gt(UserSubscription::getEndTime, now)
                .le(UserSubscription::getEndTime, remindEndTime)
                .and(wrapper -> wrapper.isNull(UserSubscription::getLastRemindTime)
                        .or()
                        .lt(UserSubscription::getLastRemindTime, DateUtil.beginOfDay(now)))
                .last("LIMIT 200"));

        for (UserSubscription subscription : subscriptions) {
            userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                    .eq(UserSubscription::getId, subscription.getId())
                    .set(UserSubscription::getLastRemindTime, now));

            log.info("会员即将到期提醒，用户ID：{}，套餐：{}，到期时间：{}",
                    subscription.getUserId(), subscription.getPlanName(), DateUtil.formatDateTime(subscription.getEndTime()));
        }

        return subscriptions.size();
    }

    /**
     * 保存或刷新用户订阅。
     *
     * @param userId 用户ID
     * @param subscription 当前订阅
     * @param plan 套餐
     * @param startTime 开始时间
     * @param endTime 到期时间
     * @param orderNo 订单号
     * @return 保存后的订阅
     */
    private UserSubscription saveOrRefreshSubscription(Long userId, UserSubscription subscription, MemberPlan plan,
                                                       Date startTime, Date endTime, String orderNo) {
        if (subscription == null) {
            UserSubscription newSubscription = new UserSubscription();
            newSubscription.setUserId(userId);
            newSubscription.setPlanId(plan.getId());
            newSubscription.setPlanCode(plan.getPlanCode());
            newSubscription.setPlanName(plan.getPlanName());
            newSubscription.setStatus(SubscriptionStatusEnum.ACTIVE.getCode());
            newSubscription.setStartTime(startTime);
            newSubscription.setEndTime(endTime);
            newSubscription.setRenewCount(0);
            newSubscription.setLastOrderNo(orderNo);
            userSubscriptionMapper.insert(newSubscription);
            return newSubscription;
        }

        userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                .eq(UserSubscription::getId, subscription.getId())
                .set(UserSubscription::getPlanId, plan.getId())
                .set(UserSubscription::getPlanCode, plan.getPlanCode())
                .set(UserSubscription::getPlanName, plan.getPlanName())
                .set(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .set(UserSubscription::getStartTime, startTime)
                .set(UserSubscription::getEndTime, endTime)
                .set(UserSubscription::getRenewCount, 0)
                .set(UserSubscription::getPendingPlanId, null)
                .set(UserSubscription::getPendingPlanCode, null)
                .set(UserSubscription::getPendingPlanName, null)
                .set(UserSubscription::getPendingPlanEffectTime, null)
                .set(UserSubscription::getLastOrderNo, orderNo));

        return userSubscriptionMapper.selectById(subscription.getId());
    }

    /**
     * 发放套餐权益。
     *
     * @param userId 用户ID
     * @param subscription 用户订阅
     * @param plan 套餐
     * @param startTime 权益开始时间
     * @param endTime 权益结束时间
     * @param sourceOrderNo 来源订单号
     */
    private void grantBenefits(Long userId, UserSubscription subscription, MemberPlan plan,
                               Date startTime, Date endTime, String sourceOrderNo) {
        List<MemberPlanBenefit> planBenefits = memberPlanBenefitMapper.selectList(Wrappers.<MemberPlanBenefit>lambdaQuery()
                .eq(MemberPlanBenefit::getPlanId, plan.getId())
                .eq(MemberPlanBenefit::getStatus, 1));

        if (CollUtil.isEmpty(planBenefits)) {
            log.warn("套餐未配置权益，套餐ID：{}，套餐名称：{}", plan.getId(), plan.getPlanName());
            return;
        }

        for (MemberPlanBenefit planBenefit : planBenefits) {
            UserBenefit userBenefit = new UserBenefit();
            userBenefit.setUserId(userId);
            userBenefit.setSubscriptionId(subscription.getId());
            userBenefit.setPlanId(plan.getId());
            userBenefit.setBenefitCode(planBenefit.getBenefitCode());
            userBenefit.setBenefitName(planBenefit.getBenefitName());
            userBenefit.setBenefitValue(planBenefit.getBenefitValue());
            userBenefit.setUnit(planBenefit.getUnit());
            userBenefit.setStatus(BenefitStatusEnum.ACTIVE.getCode());
            userBenefit.setStartTime(startTime);
            userBenefit.setEndTime(endTime);
            userBenefit.setSourceOrderNo(sourceOrderNo);
            userBenefitMapper.insert(userBenefit);
        }

        log.info("会员权益发放完成，用户ID：{}，套餐：{}，权益数量：{}", userId, plan.getPlanName(), planBenefits.size());
    }

    /**
     * 回收用户权益。
     *
     * @param subscriptionId 订阅ID
     * @param targetStatus 目标状态
     * @param reason 回收原因
     */
    private void revokeBenefits(Long subscriptionId, BenefitStatusEnum targetStatus, String reason) {
        userBenefitMapper.update(null, Wrappers.<UserBenefit>lambdaUpdate()
                .eq(UserBenefit::getSubscriptionId, subscriptionId)
                .eq(UserBenefit::getStatus, BenefitStatusEnum.ACTIVE.getCode())
                .set(UserBenefit::getStatus, targetStatus.getCode()));

        log.info("会员权益回收完成，订阅ID：{}，目标状态：{}，原因：{}", subscriptionId, targetStatus.getDesc(), reason);
    }

    /**
     * 延长当前权益有效期。
     *
     * @param subscriptionId 订阅ID
     * @param newEndTime 新到期时间
     */
    private void extendBenefits(Long subscriptionId, Date newEndTime) {
        userBenefitMapper.update(null, Wrappers.<UserBenefit>lambdaUpdate()
                .eq(UserBenefit::getSubscriptionId, subscriptionId)
                .eq(UserBenefit::getStatus, BenefitStatusEnum.ACTIVE.getCode())
                .set(UserBenefit::getEndTime, newEndTime));

        log.info("会员权益有效期延长完成，订阅ID：{}，新到期时间：{}", subscriptionId, DateUtil.formatDateTime(newEndTime));
    }

    /**
     * 处理单个到期订阅。
     *
     * @param subscriptionId 订阅ID
     */
    private void expireOneSubscription(Long subscriptionId) {
        UserSubscription subscription = userSubscriptionMapper.selectById(subscriptionId);
        if (subscription == null || !SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus())) {
            return;
        }

        if (subscription.getPendingPlanId() != null) {
            effectDowngrade(subscriptionId);
            return;
        }

        revokeBenefits(subscriptionId, BenefitStatusEnum.EXPIRED, "会员到期回收权益");

        userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                .eq(UserSubscription::getId, subscriptionId)
                .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .set(UserSubscription::getStatus, SubscriptionStatusEnum.EXPIRED.getCode()));

        MemberPlan beforePlan = memberPlanMapper.selectById(subscription.getPlanId());
        writeChangeLog(subscription.getUserId(), subscriptionId, null, ChangeTypeEnum.EXPIRE,
                beforePlan, null, subscription.getEndTime(), subscription.getEndTime(), BigDecimal.ZERO, "会员到期回收");

        log.info("会员到期处理完成，用户ID：{}，订阅ID：{}", subscription.getUserId(), subscriptionId);
    }

    /**
     * 处理降级套餐生效。
     *
     * @param subscriptionId 订阅ID
     */
    private void effectDowngrade(Long subscriptionId) {
        UserSubscription subscription = userSubscriptionMapper.selectById(subscriptionId);
        if (subscription == null || subscription.getPendingPlanId() == null) {
            return;
        }

        MemberPlan beforePlan = memberPlanMapper.selectById(subscription.getPlanId());
        MemberPlan targetPlan = memberPlanMapper.selectById(subscription.getPendingPlanId());
        if (targetPlan == null || !Objects.equals(targetPlan.getStatus(), 1)) {
            throw new BizException("待生效套餐不存在或已下架");
        }

        Date now = DateUtil.date();
        Date newEndTime = DateUtil.offsetDay(now, targetPlan.getPeriodDays());
        String changeNo = "DG" + IdUtil.getSnowflakeNextIdStr();

        revokeBenefits(subscriptionId, BenefitStatusEnum.EXPIRED, "降级生效前回收原套餐权益");

        userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
                .eq(UserSubscription::getId, subscriptionId)
                .set(UserSubscription::getPlanId, targetPlan.getId())
                .set(UserSubscription::getPlanCode, targetPlan.getPlanCode())
                .set(UserSubscription::getPlanName, targetPlan.getPlanName())
                .set(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
                .set(UserSubscription::getStartTime, now)
                .set(UserSubscription::getEndTime, newEndTime)
                .set(UserSubscription::getPendingPlanId, null)
                .set(UserSubscription::getPendingPlanCode, null)
                .set(UserSubscription::getPendingPlanName, null)
                .set(UserSubscription::getPendingPlanEffectTime, null));

        UserSubscription updatedSubscription = userSubscriptionMapper.selectById(subscriptionId);
        grantBenefits(subscription.getUserId(), updatedSubscription, targetPlan, now, newEndTime, changeNo);

        writeChangeLog(subscription.getUserId(), subscriptionId, null, ChangeTypeEnum.DOWNGRADE_EFFECT,
                beforePlan, targetPlan, subscription.getEndTime(), newEndTime, BigDecimal.ZERO, "降级套餐生效");

        log.info("会员降级生效完成，用户ID：{}，原套餐：{}，新套餐：{}",
                subscription.getUserId(), beforePlan.getPlanName(), targetPlan.getPlanName());
    }

    /**
     * 计算升级补差价。
     *
     * @param currentPlan 当前套餐
     * @param targetPlan 目标套餐
     * @param currentEndTime 当前到期时间
     * @return 补差价金额
     */
    private BigDecimal calculateUpgradeAmount(MemberPlan currentPlan, MemberPlan targetPlan, Date currentEndTime) {
        Date now = DateUtil.date();
        long remainDays = Math.max(DateUtil.betweenDay(now, currentEndTime, true), 1);

        BigDecimal currentDayPrice = currentPlan.getPrice()
                .divide(BigDecimal.valueOf(currentPlan.getPeriodDays()), 2, RoundingMode.HALF_UP);
        BigDecimal targetDayPrice = targetPlan.getPrice()
                .divide(BigDecimal.valueOf(targetPlan.getPeriodDays()), 2, RoundingMode.HALF_UP);

        BigDecimal amount = targetDayPrice.subtract(currentDayPrice)
                .multiply(BigDecimal.valueOf(remainDays))
                .setScale(2, RoundingMode.HALF_UP);

        return amount.max(BigDecimal.ZERO);
    }

    /**
     * 写入订阅变更记录。
     *
     * @param userId 用户ID
     * @param subscriptionId 订阅ID
     * @param orderNo 订单号
     * @param changeType 变更类型
     * @param beforePlan 变更前套餐
     * @param afterPlan 变更后套餐
     * @param beforeEndTime 变更前到期时间
     * @param afterEndTime 变更后到期时间
     * @param amount 金额
     * @param remark 备注
     */
    private void writeChangeLog(Long userId, Long subscriptionId, String orderNo, ChangeTypeEnum changeType,
                                MemberPlan beforePlan, MemberPlan afterPlan, Date beforeEndTime,
                                Date afterEndTime, BigDecimal amount, String remark) {
        SubscriptionChangeLog changeLog = new SubscriptionChangeLog();
        changeLog.setChangeNo("MS" + IdUtil.getSnowflakeNextIdStr());
        changeLog.setUserId(userId);
        changeLog.setSubscriptionId(subscriptionId);
        changeLog.setOrderNo(orderNo);
        changeLog.setChangeType(changeType.getCode());
        changeLog.setBeforePlanId(beforePlan == null ? null : beforePlan.getId());
        changeLog.setBeforePlanName(beforePlan == null ? null : beforePlan.getPlanName());
        changeLog.setAfterPlanId(afterPlan == null ? null : afterPlan.getId());
        changeLog.setAfterPlanName(afterPlan == null ? null : afterPlan.getPlanName());
        changeLog.setBeforeEndTime(beforeEndTime);
        changeLog.setAfterEndTime(afterEndTime);
        changeLog.setAmount(amount);
        changeLog.setRemark(remark);
        changeLogMapper.insert(changeLog);
    }

    /**
     * 根据套餐编码查询启用套餐。
     *
     * @param planCode 套餐编码
     * @return 套餐信息
     */
    private MemberPlan getEnabledPlanByCode(String planCode) {
        MemberPlan plan = memberPlanMapper.selectOne(Wrappers.<MemberPlan>lambdaQuery()
                .eq(MemberPlan::getPlanCode, planCode)
                .eq(MemberPlan::getStatus, 1)
                .last("LIMIT 1"));

        if (plan == null) {
            throw new BizException("套餐不存在或已下架");
        }
        return plan;
    }

    /**
     * 查询用户订阅。
     *
     * @param userId 用户ID
     * @return 用户订阅
     */
    private UserSubscription getUserSubscription(Long userId) {
        return userSubscriptionMapper.selectOne(Wrappers.<UserSubscription>lambdaQuery()
                .eq(UserSubscription::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 查询必需存在的用户订阅。
     *
     * @param userId 用户ID
     * @return 用户订阅
     */
    private UserSubscription getRequiredSubscription(Long userId) {
        UserSubscription subscription = getUserSubscription(userId);
        if (subscription == null) {
            throw new BizException("当前用户尚未开通会员");
        }
        return subscription;
    }

    /**
     * 查询必需生效的用户订阅。
     *
     * @param userId 用户ID
     * @return 用户订阅
     */
    private UserSubscription getRequiredActiveSubscription(Long userId) {
        UserSubscription subscription = getRequiredSubscription(userId);
        Date now = DateUtil.date();

        if (!SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus()) || !subscription.getEndTime().after(now)) {
            throw new BizException("当前用户没有生效中的会员");
        }
        return subscription;
    }

    /**
     * 判断订单是否已经处理。
     *
     * @param orderNo 订单号
     * @param changeType 变更类型
     * @return 是否已处理
     */
    private boolean isOrderProcessed(String orderNo, ChangeTypeEnum changeType) {
        Long count = changeLogMapper.selectCount(Wrappers.<SubscriptionChangeLog>lambdaQuery()
                .eq(SubscriptionChangeLog::getOrderNo, orderNo)
                .eq(SubscriptionChangeLog::getChangeType, changeType.getCode()));
        return count != null && count > 0;
    }

    /**
     * 校验支付金额。
     *
     * @param expectedAmount 应付金额
     * @param actualAmount 实付金额
     * @param message 异常信息
     */
    private void checkAmount(BigDecimal expectedAmount, BigDecimal actualAmount, String message) {
        if (expectedAmount.compareTo(actualAmount) != 0) {
            throw new BizException(message);
        }
    }

    /**
     * 执行用户维度分布式锁。
     *
     * @param userId 用户ID
     * @param runnable 业务逻辑
     */
    private void executeWithUserLock(Long userId, Runnable runnable) {
        String lockKey = "lock:member:subscription:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException("会员订阅操作处理中，请稍后再试");
            }
            runnable.run();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("会员订阅操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 会员续费处理

续费逻辑在上面的 `renew` 方法中已经实现，核心是根据当前订阅是否仍在有效期内决定叠加方式。

关键代码：

```java
Date now = DateUtil.date();
boolean stillActive = SubscriptionStatusEnum.ACTIVE.getCode().equals(subscription.getStatus())
        && subscription.getEndTime().after(now);

Date baseTime = stillActive ? subscription.getEndTime() : now;
Date newEndTime = DateUtil.offsetDay(baseTime, plan.getPeriodDays());
```

续费场景建议只允许续当前套餐：

```java
if (!Objects.equals(subscription.getPlanCode(), plan.getPlanCode())) {
    throw new BizException("续费套餐必须和当前套餐一致，如需变更套餐请使用升级或降级");
}
```

这样可以避免用户在“续费接口”中绕过升级补差价或降级延后生效规则。

### 套餐升级处理

升级逻辑在 `upgrade` 方法中完成，重点是套餐等级校验和补差价校验。

关键代码：

```java
if (targetPlan.getLevel() <= currentPlan.getLevel()) {
    throw new BizException("目标套餐等级必须高于当前套餐");
}

BigDecimal upgradeAmount = calculateUpgradeAmount(currentPlan, targetPlan, subscription.getEndTime());
checkAmount(upgradeAmount, request.getAmount(), "支付金额和升级补差价不一致");
```

升级成功后立即切换套餐，并重新发放目标套餐权益：

```java
revokeBenefits(subscription.getId(), BenefitStatusEnum.REVOKED, "升级套餐回收旧权益");
grantBenefits(userId, updatedSubscription, targetPlan, DateUtil.date(), updatedSubscription.getEndTime(), request.getOrderNo());
```

这个实现采用“保持原到期时间，只升级权益”的方案。真实项目中如果要支持“升级后重新购买完整周期”，只需要在升级时重新计算 `endTime` 即可。

### 套餐降级预约

降级逻辑在 `downgrade` 方法中完成。降级不会立即修改当前套餐，只写入待生效套餐字段。

关键代码：

```java
userSubscriptionMapper.update(null, Wrappers.<UserSubscription>lambdaUpdate()
        .eq(UserSubscription::getId, subscription.getId())
        .set(UserSubscription::getPendingPlanId, targetPlan.getId())
        .set(UserSubscription::getPendingPlanCode, targetPlan.getPlanCode())
        .set(UserSubscription::getPendingPlanName, targetPlan.getPlanName())
        .set(UserSubscription::getPendingPlanEffectTime, subscription.getEndTime()));
```

降级预约成功后，用户在当前周期内仍然保留原套餐权益。到期后由定时任务执行 `effectPendingDowngrade`，再切换为低等级套餐。

### 权益发放与回收

权益发放和回收是会员订阅系统的关键。业务系统判断用户能否使用某个功能时，应查询 `user_benefit`，而不是只判断 `user_subscription`。

权益发放流程：

```text
查询套餐权益配置
-> 遍历套餐权益
-> 写入 user_benefit
-> 设置权益开始时间和结束时间
-> 设置权益状态为 ACTIVE
```

权益回收流程：

```text
根据 subscription_id 查询生效权益
-> 更新权益状态为 EXPIRED 或 REVOKED
-> 后续鉴权不再命中这些权益
```

实际项目中可以把用户权益缓存到 Redis：

```text
member:benefit:user:{userId}
```

缓存结构示例：

```json
{
  "PROJECT_LIMIT": "20",
  "EXPORT_REPORT": "true",
  "TEAM_MEMBER_LIMIT": "20"
}
```

当购买、续费、升级、降级生效、到期回收时，都需要刷新这个缓存。

### 到期任务补偿

到期补偿通过 XXL-JOB 调度。Job 层只做任务入口，真正业务逻辑仍然交给 Service。

文件位置：`src/main/java/io/github/atengk/member/job/MemberSubscriptionJob.java`

```java
package io.github.atengk.member.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.member.service.MemberSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 会员订阅定时任务。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSubscriptionJob {

    private final MemberSubscriptionService memberSubscriptionService;

    /**
     * 会员到期提醒任务。
     */
    @XxlJob("memberExpireRemindJob")
    public void remindExpiringSubscriptions() {
        log.info("会员到期提醒任务开始");
        int count = memberSubscriptionService.remindExpiringSubscriptions();
        log.info("会员到期提醒任务结束，处理数量：{}", count);
    }

    /**
     * 会员到期回收任务。
     */
    @XxlJob("memberExpireRecoverJob")
    public void expireSubscriptions() {
        log.info("会员到期回收任务开始");
        int count = memberSubscriptionService.expireSubscriptions();
        log.info("会员到期回收任务结束，处理数量：{}", count);
    }

    /**
     * 会员降级生效任务。
     */
    @XxlJob("memberDowngradeEffectJob")
    public void effectPendingDowngrade() {
        log.info("会员降级生效任务开始");
        int count = memberSubscriptionService.effectPendingDowngrade();
        log.info("会员降级生效任务结束，处理数量：{}", count);
    }
}
```

建议调度频率：

| JobHandler                 | Cron             | 说明                        |
| -------------------------- | ---------------- | --------------------------- |
| `memberExpireRemindJob`    | `0 0 10 * * ?`   | 每天 10 点提醒即将到期会员  |
| `memberExpireRecoverJob`   | `0 */5 * * * ?`  | 每 5 分钟回收已到期会员权益 |
| `memberDowngradeEffectJob` | `30 */5 * * * ?` | 每 5 分钟处理降级生效       |

## 接口设计

接口层用于暴露会员套餐查询、购买、续费、升级、降级和当前会员查询能力。这里使用 Sa-Token 获取当前登录用户 ID。

文件位置：`src/main/java/io/github/atengk/member/controller/MemberSubscriptionController.java`

```java
package io.github.atengk.member.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.member.dto.DowngradeRequest;
import io.github.atengk.member.dto.RenewRequest;
import io.github.atengk.member.dto.SubscribeRequest;
import io.github.atengk.member.dto.UpgradeRequest;
import io.github.atengk.member.entity.MemberPlan;
import io.github.atengk.member.service.MemberSubscriptionService;
import io.github.atengk.member.vo.UserSubscriptionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会员订阅接口。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberSubscriptionController {

    private final MemberSubscriptionService memberSubscriptionService;

    /**
     * 查询启用套餐列表。
     *
     * @return 套餐列表
     */
    @GetMapping("/plans")
    public Map<String, Object> listPlans() {
        List<MemberPlan> plans = memberSubscriptionService.listEnabledPlans();
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "success")
                .put("data", plans)
                .build();
    }

    /**
     * 查询当前会员信息。
     *
     * @return 当前会员信息
     */
    @GetMapping("/subscription/current")
    public Map<String, Object> currentSubscription() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserSubscriptionVO vo = memberSubscriptionService.getCurrentSubscription(userId);
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "success")
                .put("data", vo)
                .build();
    }

    /**
     * 购买会员套餐。
     *
     * @param request 购买请求
     * @return 处理结果
     */
    @PostMapping("/subscription/subscribe")
    public Map<String, Object> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        memberSubscriptionService.subscribe(userId, request);
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "会员开通成功")
                .build();
    }

    /**
     * 续费会员套餐。
     *
     * @param request 续费请求
     * @return 处理结果
     */
    @PostMapping("/subscription/renew")
    public Map<String, Object> renew(@Valid @RequestBody RenewRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        memberSubscriptionService.renew(userId, request);
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "会员续费成功")
                .build();
    }

    /**
     * 升级会员套餐。
     *
     * @param request 升级请求
     * @return 处理结果
     */
    @PostMapping("/subscription/upgrade")
    public Map<String, Object> upgrade(@Valid @RequestBody UpgradeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        memberSubscriptionService.upgrade(userId, request);
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "会员升级成功")
                .build();
    }

    /**
     * 降级会员套餐。
     *
     * @param request 降级请求
     * @return 处理结果
     */
    @PostMapping("/subscription/downgrade")
    public Map<String, Object> downgrade(@Valid @RequestBody DowngradeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        memberSubscriptionService.downgrade(userId, request);
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("message", "会员降级预约成功")
                .build();
    }
}
```

请求 DTO 放在 `dto` 包下。

文件位置：`src/main/java/io/github/atengk/member/dto/SubscribeRequest.java`

```java
package io.github.atengk.member.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购买会员请求。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SubscribeRequest {

    @NotBlank(message = "套餐编码不能为空")
    private String planCode;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.00", message = "支付金额不能小于0")
    private BigDecimal amount;
}
```

文件位置：`src/main/java/io/github/atengk/member/dto/RenewRequest.java`

```java
package io.github.atengk.member.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员续费请求。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RenewRequest {

    @NotBlank(message = "套餐编码不能为空")
    private String planCode;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.00", message = "支付金额不能小于0")
    private BigDecimal amount;
}
```

文件位置：`src/main/java/io/github/atengk/member/dto/UpgradeRequest.java`

```java
package io.github.atengk.member.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员升级请求。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class UpgradeRequest {

    @NotBlank(message = "目标套餐编码不能为空")
    private String targetPlanCode;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.00", message = "支付金额不能小于0")
    private BigDecimal amount;
}
```

文件位置：`src/main/java/io/github/atengk/member/dto/DowngradeRequest.java`

```java
package io.github.atengk.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员降级请求。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class DowngradeRequest {

    @NotBlank(message = "目标套餐编码不能为空")
    private String targetPlanCode;
}
```

当前会员信息 VO 放在 `vo` 包下。

文件位置：`src/main/java/io/github/atengk/member/vo/UserSubscriptionVO.java`

```java
package io.github.atengk.member.vo;

import io.github.atengk.member.entity.UserBenefit;
import io.github.atengk.member.entity.UserSubscription;
import io.github.atengk.member.enums.SubscriptionStatusEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用户会员订阅视图对象。
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class UserSubscriptionVO {

    private Long userId;

    private Boolean opened;

    private String planCode;

    private String planName;

    private Integer status;

    private String statusName;

    private Date startTime;

    private Date endTime;

    private String pendingPlanCode;

    private String pendingPlanName;

    private Date pendingPlanEffectTime;

    private List<BenefitVO> benefits;

    /**
     * 构造空会员信息。
     *
     * @param userId 用户ID
     * @return 空会员信息
     */
    public static UserSubscriptionVO empty(Long userId) {
        UserSubscriptionVO vo = new UserSubscriptionVO();
        vo.setUserId(userId);
        vo.setOpened(false);
        return vo;
    }

    /**
     * 根据订阅和权益构造会员信息。
     *
     * @param subscription 用户订阅
     * @param benefits 用户权益
     * @return 会员信息
     */
    public static UserSubscriptionVO of(UserSubscription subscription, List<UserBenefit> benefits) {
        UserSubscriptionVO vo = new UserSubscriptionVO();
        vo.setUserId(subscription.getUserId());
        vo.setOpened(true);
        vo.setPlanCode(subscription.getPlanCode());
        vo.setPlanName(subscription.getPlanName());
        vo.setStatus(subscription.getStatus());
        vo.setStatusName(SubscriptionStatusEnum.of(subscription.getStatus()).getDesc());
        vo.setStartTime(subscription.getStartTime());
        vo.setEndTime(subscription.getEndTime());
        vo.setPendingPlanCode(subscription.getPendingPlanCode());
        vo.setPendingPlanName(subscription.getPendingPlanName());
        vo.setPendingPlanEffectTime(subscription.getPendingPlanEffectTime());
        vo.setBenefits(benefits.stream().map(BenefitVO::of).toList());
        return vo;
    }

    /**
     * 用户权益视图对象。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Data
    public static class BenefitVO {

        private String benefitCode;

        private String benefitName;

        private String benefitValue;

        private String unit;

        /**
         * 根据用户权益构造权益视图。
         *
         * @param benefit 用户权益
         * @return 权益视图
         */
        public static BenefitVO of(UserBenefit benefit) {
            BenefitVO vo = new BenefitVO();
            vo.setBenefitCode(benefit.getBenefitCode());
            vo.setBenefitName(benefit.getBenefitName());
            vo.setBenefitValue(benefit.getBenefitValue());
            vo.setUnit(benefit.getUnit());
            return vo;
        }
    }
}
```

业务异常类用于主动中断业务流程。

文件位置：`src/main/java/io/github/atengk/member/common/BizException.java`

```java
package io.github.atengk.member.common;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
```

### 查询套餐列表

查询所有启用状态的会员套餐，用于前端展示套餐卡片。

接口信息：

| 项目     | 内容            |
| -------- | --------------- |
| 请求方式 | `GET`           |
| 请求路径 | `/member/plans` |
| 是否登录 | 建议不强制      |
| 业务说明 | 查询可购买套餐  |

请求示例：

```bash
curl -X GET 'http://localhost:8080/member/plans'
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "planCode": "BASIC_MONTH",
      "planName": "基础版月卡",
      "level": 1,
      "price": 29.00,
      "periodDays": 30
    },
    {
      "id": 2,
      "planCode": "PRO_MONTH",
      "planName": "专业版月卡",
      "level": 2,
      "price": 59.00,
      "periodDays": 30
    }
  ]
}
```

### 购买会员套餐

购买会员套餐用于首次开通，或者会员过期后重新开通。

接口信息：

| 项目     | 内容                             |
| -------- | -------------------------------- |
| 请求方式 | `POST`                           |
| 请求路径 | `/member/subscription/subscribe` |
| 是否登录 | 是                               |
| 业务说明 | 支付成功后开通会员               |

请求示例：

```bash
curl -X POST 'http://localhost:8080/member/subscription/subscribe' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "planCode": "BASIC_MONTH",
    "orderNo": "PAY202605150001",
    "amount": 29.00
  }'
```

响应示例：

```json
{
  "code": 0,
  "message": "会员开通成功"
}
```

异常示例：

```json
{
  "code": 500,
  "message": "当前会员仍在有效期内，请使用续费或升级"
}
```

### 续费会员套餐

续费会员套餐用于延长当前套餐有效期。续费不允许变更套餐等级。

接口信息：

| 项目     | 内容                         |
| -------- | ---------------------------- |
| 请求方式 | `POST`                       |
| 请求路径 | `/member/subscription/renew` |
| 是否登录 | 是                           |
| 业务说明 | 当前套餐续费并叠加有效期     |

请求示例：

```bash
curl -X POST 'http://localhost:8080/member/subscription/renew' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "planCode": "BASIC_MONTH",
    "orderNo": "PAY202605150002",
    "amount": 29.00
  }'
```

响应示例：

```json
{
  "code": 0,
  "message": "会员续费成功"
}
```

续费后的有效期示例：

```text
原到期时间：2026-06-15 23:59:59
套餐周期：30 天
续费后到期时间：2026-07-15 23:59:59
```

### 升级会员套餐

升级会员套餐用于从低等级套餐切换到高等级套餐。升级立即生效，当前周期内直接使用新套餐权益。

接口信息：

| 项目     | 内容                           |
| -------- | ------------------------------ |
| 请求方式 | `POST`                         |
| 请求路径 | `/member/subscription/upgrade` |
| 是否登录 | 是                             |
| 业务说明 | 支付补差价后立即升级套餐       |

请求示例：

```bash
curl -X POST 'http://localhost:8080/member/subscription/upgrade' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "targetPlanCode": "PRO_MONTH",
    "orderNo": "PAY202605150003",
    "amount": 20.00
  }'
```

响应示例：

```json
{
  "code": 0,
  "message": "会员升级成功"
}
```

升级后的处理结果：

```text
当前套餐：基础版月卡 -> 专业版月卡
会员到期时间：不变
旧套餐权益：REVOKED
新套餐权益：ACTIVE
```

### 降级会员套餐

降级会员套餐用于从高等级套餐切换到低等级套餐。降级不立即生效，而是在当前订阅到期后由定时任务切换。

接口信息：

| 项目     | 内容                             |
| -------- | -------------------------------- |
| 请求方式 | `POST`                           |
| 请求路径 | `/member/subscription/downgrade` |
| 是否登录 | 是                               |
| 业务说明 | 预约降级套餐                     |

请求示例：

```bash
curl -X POST 'http://localhost:8080/member/subscription/downgrade' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "targetPlanCode": "BASIC_MONTH"
  }'
```

响应示例：

```json
{
  "code": 0,
  "message": "会员降级预约成功"
}
```

降级预约后的订阅信息：

```text
当前套餐：专业版月卡
当前权益：继续保留专业版权益
待生效套餐：基础版月卡
待生效时间：当前会员到期时间
```

### 查询当前会员信息

查询当前用户的会员套餐、订阅状态、到期时间、待生效降级套餐和当前权益列表。

接口信息：

| 项目     | 内容                           |
| -------- | ------------------------------ |
| 请求方式 | `GET`                          |
| 请求路径 | `/member/subscription/current` |
| 是否登录 | 是                             |
| 业务说明 | 查询当前会员订阅状态           |

请求示例：

```bash
curl -X GET 'http://localhost:8080/member/subscription/current' \
  -H 'Authorization: Bearer your-token'
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "opened": true,
    "planCode": "PRO_MONTH",
    "planName": "专业版月卡",
    "status": 1,
    "statusName": "生效中",
    "startTime": "2026-05-15 10:00:00",
    "endTime": "2026-06-14 10:00:00",
    "pendingPlanCode": "BASIC_MONTH",
    "pendingPlanName": "基础版月卡",
    "pendingPlanEffectTime": "2026-06-14 10:00:00",
    "benefits": [
      {
        "benefitCode": "PROJECT_LIMIT",
        "benefitName": "项目数量上限",
        "benefitValue": "20",
        "unit": "个"
      },
      {
        "benefitCode": "EXPORT_REPORT",
        "benefitName": "报表导出",
        "benefitValue": "true",
        "unit": null
      }
    ]
  }
}
```

未开通会员时响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "opened": false,
    "planCode": null,
    "planName": null,
    "status": null,
    "statusName": null,
    "startTime": null,
    "endTime": null,
    "benefits": null
  }
}
```

这一版实现的核心闭环已经具备：购买开通、续费叠加、升级即时生效、降级延后生效、权益发放、权益回收和定时补偿。下一步可以继续补充“幂等与并发控制”和“定时任务设计”，把唯一索引、Redisson 锁、订单防重和 XXL-JOB 配置说明补完整。

## 幂等与并发控制

会员订阅场景的核心风险集中在“重复支付回调、同一用户并发续费、权益重复发放”这三类问题上。本案例通过数据库唯一索引、Redisson 用户维度锁、业务状态判断共同控制，避免重复开通、重复叠加有效期、重复发放权益。该部分对应 README 中第 10 个场景提到的有效期叠加、权益开通、权益回收、到期补偿等核心难点。

### 订单号幂等控制

购买、续费、升级都依赖支付成功后的业务订单号 `order_no`。支付平台可能重复通知，MQ 也可能重复投递，所以不能只依赖前端按钮防重复。

本案例使用 `subscription_change_log` 表控制订单幂等。

核心唯一索引如下：

```sql
-- 同一订单号 + 同一变更类型只能处理一次
ALTER TABLE subscription_change_log
ADD UNIQUE KEY uk_order_change_type (order_no, change_type);
```

业务规则如下：

| 场景     | 幂等维度                                           |
| -------- | -------------------------------------------------- |
| 购买开通 | `order_no + BUY`                                   |
| 续费     | `order_no + RENEW`                                 |
| 升级     | `order_no + UPGRADE`                               |
| 降级预约 | 不强制依赖订单号，可以按用户当前订阅覆盖待降级套餐 |
| 到期回收 | 按订阅状态控制，只处理 `ACTIVE`                    |

Service 中先查询是否处理过：

```java
/**
 * 判断订单是否已经处理。
 *
 * @param orderNo 订单号
 * @param changeType 变更类型
 * @return 是否已处理
 */
private boolean isOrderProcessed(String orderNo, ChangeTypeEnum changeType) {
    Long count = changeLogMapper.selectCount(Wrappers.<SubscriptionChangeLog>lambdaQuery()
            .eq(SubscriptionChangeLog::getOrderNo, orderNo)
            .eq(SubscriptionChangeLog::getChangeType, changeType.getCode()));
    return count != null && count > 0;
}
```

在购买、续费、升级入口中使用：

```java
if (isOrderProcessed(request.getOrderNo(), ChangeTypeEnum.RENEW)) {
    log.info("会员续费请求重复，直接忽略，用户ID：{}，订单号：{}", userId, request.getOrderNo());
    return;
}
```

这里采用“先查 + 唯一索引兜底”的方式。先查可以让重复请求快速返回，唯一索引可以兜住极端并发下两次请求同时通过查询的问题。

如果项目中希望重复请求返回第一次处理结果，可以扩展 `subscription_change_log` 表，增加 `result_status`、`result_message`、`result_data` 字段。但本案例中重复请求直接忽略即可。

### 用户订阅更新锁

同一用户的订阅更新必须串行执行。否则会出现以下问题：

```text
用户同时发起两次续费
-> 两个线程同时读取原 end_time
-> 两个线程分别计算 new_end_time
-> 后提交的覆盖先提交的
-> 实际只叠加了一次有效期
```

因此，本案例使用 Redisson 对用户维度加锁：

```text
lock:member:subscription:user:{userId}
```

锁粒度建议按用户控制，不建议对整个套餐或整个会员模块加全局锁。用户维度锁可以保证同一个用户的订阅变更串行，同时不同用户之间不会互相阻塞。

分布式锁核心代码如下：

```java
/**
 * 执行用户维度分布式锁。
 *
 * @param userId 用户ID
 * @param runnable 业务逻辑
 */
private void executeWithUserLock(Long userId, Runnable runnable) {
    String lockKey = "lock:member:subscription:user:" + userId;
    RLock lock = redissonClient.getLock(lockKey);
    boolean locked = false;

    try {
        locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BizException("会员订阅操作处理中，请稍后再试");
        }
        runnable.run();
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new BizException("会员订阅操作被中断");
    } finally {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

购买、续费、升级、降级都统一包裹在锁内：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void renew(Long userId, RenewRequest request) {
    executeWithUserLock(userId, () -> {
        // 续费核心逻辑
    });
}
```

锁参数说明：

| 参数       | 示例     | 说明                             |
| ---------- | -------- | -------------------------------- |
| 等待锁时间 | `3 秒`   | 3 秒内拿不到锁，直接提示稍后再试 |
| 锁租约时间 | `30 秒`  | 防止服务异常导致死锁             |
| 锁粒度     | `userId` | 同一用户串行，不同用户并行       |

实际生产中，如果会员变更链路会调用支付、远程服务或复杂 MQ，建议不要在锁内做长时间外部调用。支付动作应先完成，会员开通只处理支付成功后的本地事务。

### 权益重复发放控制

权益重复发放会导致用户同一个订单拥有多份相同权益。比如同一支付回调重复执行时，如果没有控制，`user_benefit` 可能出现重复数据。

本案例使用 `user_id + benefit_code + source_order_no` 控制重复发放：

```sql
-- 同一用户、同一权益、同一来源订单只能发放一次
ALTER TABLE user_benefit
ADD UNIQUE KEY uk_user_benefit_order (user_id, benefit_code, source_order_no);
```

权益发放时写入来源订单号：

```java
userBenefit.setUserId(userId);
userBenefit.setSubscriptionId(subscription.getId());
userBenefit.setPlanId(plan.getId());
userBenefit.setBenefitCode(planBenefit.getBenefitCode());
userBenefit.setBenefitName(planBenefit.getBenefitName());
userBenefit.setBenefitValue(planBenefit.getBenefitValue());
userBenefit.setUnit(planBenefit.getUnit());
userBenefit.setStatus(BenefitStatusEnum.ACTIVE.getCode());
userBenefit.setStartTime(startTime);
userBenefit.setEndTime(endTime);
userBenefit.setSourceOrderNo(sourceOrderNo);
userBenefitMapper.insert(userBenefit);
```

权益鉴权时只看生效权益：

```sql
-- 判断用户是否拥有某个生效权益
SELECT *
FROM user_benefit
WHERE user_id = 10001
  AND benefit_code = 'EXPORT_REPORT'
  AND status = 1
  AND start_time <= NOW()
  AND end_time > NOW()
  AND deleted = 0
LIMIT 1;
```

如果业务侧需要高频判断权益，建议把当前生效权益缓存到 Redis：

```text
member:benefit:user:{userId}
```

缓存内容示例：

```json
{
  "PROJECT_LIMIT": "20",
  "EXPORT_REPORT": "true",
  "TEAM_MEMBER_LIMIT": "20"
}
```

刷新缓存的时机：

| 操作     | 是否刷新权益缓存 |
| -------- | ---------------- |
| 购买开通 | 是               |
| 续费     | 是               |
| 升级成功 | 是               |
| 降级生效 | 是               |
| 到期回收 | 是               |
| 取消订阅 | 是               |

## 定时任务设计

定时任务用于处理用户不主动访问系统时仍然必须推进的订阅状态，例如到期提醒、到期回收、降级生效。本案例使用 XXL-JOB 作为调度器，Service 负责实际业务处理。

### 会员到期提醒任务

会员到期提醒任务用于扫描未来 3 天内即将到期的会员。提醒动作可以接入站内信、短信、邮件或企业微信，本案例中先用日志模拟。

任务规则：

```text
每天上午 10 点执行
-> 扫描 ACTIVE 状态订阅
-> end_time 在未来 3 天内
-> 当天未提醒过
-> 更新 last_remind_time
-> 记录提醒日志
```

XXL-JOB 配置：

| 配置项       | 值                      |
| ------------ | ----------------------- |
| JobHandler   | `memberExpireRemindJob` |
| Cron         | `0 0 10 * * ?`          |
| 路由策略     | 第一个                  |
| 阻塞处理策略 | 单机串行                |
| 失败重试次数 | `1`                     |

核心查询条件：

```java
Date now = DateUtil.date();
Date remindEndTime = DateUtil.offsetDay(now, 3);

List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
        .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
        .gt(UserSubscription::getEndTime, now)
        .le(UserSubscription::getEndTime, remindEndTime)
        .and(wrapper -> wrapper.isNull(UserSubscription::getLastRemindTime)
                .or()
                .lt(UserSubscription::getLastRemindTime, DateUtil.beginOfDay(now)))
        .last("LIMIT 200"));
```

这里用 `last_remind_time` 防止同一天重复提醒。真实项目中如果有多渠道通知，建议单独设计 `message_notice` 表记录通知投递结果。

### 会员到期回收任务

会员到期回收任务用于扫描已经过期的有效订阅，并回收权益。

任务规则：

```text
每 5 分钟执行
-> 扫描 ACTIVE 状态订阅
-> end_time <= now
-> 如果没有待生效降级套餐，订阅改为 EXPIRED
-> 当前权益改为 EXPIRED
-> 写入 EXPIRE 变更记录
```

XXL-JOB 配置：

| 配置项       | 值                       |
| ------------ | ------------------------ |
| JobHandler   | `memberExpireRecoverJob` |
| Cron         | `0 */5 * * * ?`          |
| 路由策略     | 第一个                   |
| 阻塞处理策略 | 单机串行                 |
| 失败重试次数 | `2`                      |

核心查询条件：

```java
List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
        .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
        .le(UserSubscription::getEndTime, DateUtil.date())
        .last("LIMIT 100"));
```

回收逻辑：

```text
revokeBenefits(subscriptionId, EXPIRED)
-> user_subscription.status = EXPIRED
-> subscription_change_log 写入 EXPIRE
```

为了避免任务一次处理过多数据，建议每次 `LIMIT 100` 或 `LIMIT 500`，由任务多轮补偿处理。

### 降级套餐生效任务

降级套餐生效任务用于处理用户预约降级后的套餐切换。

任务规则：

```text
每 5 分钟执行
-> 扫描 pending_plan_id 不为空的 ACTIVE 订阅
-> pending_plan_effect_time <= now
-> 回收当前套餐权益
-> 切换为待生效套餐
-> 重新计算低等级套餐有效期
-> 发放低等级套餐权益
-> 清空 pending 字段
-> 写入 DOWNGRADE_EFFECT 变更记录
```

XXL-JOB 配置：

| 配置项       | 值                         |
| ------------ | -------------------------- |
| JobHandler   | `memberDowngradeEffectJob` |
| Cron         | `30 */5 * * * ?`           |
| 路由策略     | 第一个                     |
| 阻塞处理策略 | 单机串行                   |
| 失败重试次数 | `2`                        |

核心查询条件：

```java
List<UserSubscription> subscriptions = userSubscriptionMapper.selectList(Wrappers.<UserSubscription>lambdaQuery()
        .eq(UserSubscription::getStatus, SubscriptionStatusEnum.ACTIVE.getCode())
        .isNotNull(UserSubscription::getPendingPlanId)
        .le(UserSubscription::getPendingPlanEffectTime, DateUtil.date())
        .last("LIMIT 100"));
```

降级生效后的关键字段变化：

| 字段                       | 变化                  |
| -------------------------- | --------------------- |
| `plan_id`                  | 改为待生效套餐 ID     |
| `plan_code`                | 改为待生效套餐编码    |
| `plan_name`                | 改为待生效套餐名称    |
| `start_time`               | 改为当前时间          |
| `end_time`                 | 当前时间 + 新套餐周期 |
| `pending_plan_id`          | 清空                  |
| `pending_plan_code`        | 清空                  |
| `pending_plan_name`        | 清空                  |
| `pending_plan_effect_time` | 清空                  |

如果降级套餐已经下架，建议不要直接清空 pending 字段，而是保留原状态并记录异常日志，等待运营处理。

## 功能验证

功能验证建议直接通过接口、数据库和定时任务三层验证。接口验证业务返回，数据库验证状态和权益是否正确，定时任务验证到期补偿是否可靠。

### 购买开通验证

准备套餐和权益数据：

```sql
-- 查询套餐基础数据
SELECT id, plan_code, plan_name, level, price, period_days, status
FROM member_plan
WHERE deleted = 0;

-- 查询套餐权益配置
SELECT plan_id, benefit_code, benefit_name, benefit_value, unit, status
FROM member_plan_benefit
WHERE deleted = 0;
```

调用购买接口：

```bash
curl -X POST 'http://localhost:8080/member/subscription/subscribe' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "planCode": "BASIC_MONTH",
    "orderNo": "PAY202605150001",
    "amount": 29.00
  }'
```

验证订阅是否创建：

```sql
SELECT user_id, plan_code, plan_name, status, start_time, end_time, last_order_no
FROM user_subscription
WHERE user_id = 10001;
```

预期结果：

```text
plan_code = BASIC_MONTH
status = 1
start_time 不为空
end_time = start_time + 30 天
last_order_no = PAY202605150001
```

验证权益是否发放：

```sql
SELECT benefit_code, benefit_name, benefit_value, status, start_time, end_time, source_order_no
FROM user_benefit
WHERE user_id = 10001
ORDER BY id DESC;
```

预期结果：

```text
status = 1
source_order_no = PAY202605150001
权益数量 = BASIC_MONTH 配置的权益数量
```

重复调用同一个购买接口：

```bash
curl -X POST 'http://localhost:8080/member/subscription/subscribe' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "planCode": "BASIC_MONTH",
    "orderNo": "PAY202605150001",
    "amount": 29.00
  }'
```

预期结果：

```text
不会重复创建订阅
不会重复发放权益
subscription_change_log 中 PAY202605150001 + BUY 只有一条
```

验证幂等记录：

```sql
SELECT order_no, change_type, COUNT(*) AS count_value
FROM subscription_change_log
WHERE order_no = 'PAY202605150001'
GROUP BY order_no, change_type;
```

### 续费叠加验证

先查询当前到期时间：

```sql
SELECT user_id, plan_code, status, start_time, end_time, renew_count
FROM user_subscription
WHERE user_id = 10001;
```

调用续费接口：

```bash
curl -X POST 'http://localhost:8080/member/subscription/renew' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "planCode": "BASIC_MONTH",
    "orderNo": "PAY202605150002",
    "amount": 29.00
  }'
```

再次查询订阅：

```sql
SELECT user_id, plan_code, status, start_time, end_time, renew_count, last_order_no
FROM user_subscription
WHERE user_id = 10001;
```

预期结果：

```text
renew_count 增加 1
end_time 在原 end_time 基础上增加 30 天
last_order_no = PAY202605150002
```

验证权益有效期是否同步延长：

```sql
SELECT benefit_code, status, end_time
FROM user_benefit
WHERE user_id = 10001
  AND status = 1;
```

预期结果：

```text
所有 ACTIVE 权益的 end_time 和 user_subscription.end_time 一致
```

### 升级补差验证

先确认当前套餐是基础版：

```sql
SELECT user_id, plan_code, plan_name, end_time
FROM user_subscription
WHERE user_id = 10001;
```

调用升级接口。这里的 `amount` 需要和后端 `calculateUpgradeAmount` 计算结果一致：

```bash
curl -X POST 'http://localhost:8080/member/subscription/upgrade' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "targetPlanCode": "PRO_MONTH",
    "orderNo": "PAY202605150003",
    "amount": 20.00
  }'
```

验证订阅套餐是否切换：

```sql
SELECT user_id, plan_code, plan_name, status, start_time, end_time
FROM user_subscription
WHERE user_id = 10001;
```

预期结果：

```text
plan_code = PRO_MONTH
status = 1
end_time 不变
```

验证旧权益是否回收，新权益是否发放：

```sql
SELECT plan_id, benefit_code, benefit_value, status, source_order_no
FROM user_benefit
WHERE user_id = 10001
ORDER BY id DESC;
```

预期结果：

```text
旧 BASIC_MONTH 权益 status = 3
新 PRO_MONTH 权益 status = 1
新权益 source_order_no = PAY202605150003
```

验证升级流水：

```sql
SELECT order_no, change_type, before_plan_name, after_plan_name, amount
FROM subscription_change_log
WHERE order_no = 'PAY202605150003';
```

预期结果：

```text
change_type = UPGRADE
before_plan_name = 基础版月卡
after_plan_name = 专业版月卡
amount = 本次升级补差价
```

### 降级延后验证

当前套餐为专业版或团队版时，调用降级接口：

```bash
curl -X POST 'http://localhost:8080/member/subscription/downgrade' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token' \
  -d '{
    "targetPlanCode": "BASIC_MONTH"
  }'
```

验证订阅表：

```sql
SELECT user_id,
       plan_code,
       plan_name,
       pending_plan_code,
       pending_plan_name,
       pending_plan_effect_time,
       end_time
FROM user_subscription
WHERE user_id = 10001;
```

预期结果：

```text
plan_code 仍然是当前高等级套餐
pending_plan_code = BASIC_MONTH
pending_plan_effect_time = 当前 end_time
当前权益不变
```

验证权益未立即降级：

```sql
SELECT benefit_code, benefit_value, status
FROM user_benefit
WHERE user_id = 10001
  AND status = 1;
```

预期结果：

```text
仍然是当前高等级套餐权益
不会立即变成 BASIC_MONTH 权益
```

为了验证降级生效，可以临时把 `pending_plan_effect_time` 改到当前时间之前：

```sql
-- 测试环境使用：模拟降级预约已到生效时间
UPDATE user_subscription
SET pending_plan_effect_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
WHERE user_id = 10001;
```

执行 XXL-JOB `memberDowngradeEffectJob` 后查询：

```sql
SELECT user_id,
       plan_code,
       plan_name,
       pending_plan_code,
       pending_plan_name,
       pending_plan_effect_time,
       start_time,
       end_time
FROM user_subscription
WHERE user_id = 10001;
```

预期结果：

```text
plan_code = BASIC_MONTH
pending_plan_code = NULL
pending_plan_effect_time = NULL
start_time 变为降级生效时间
end_time = start_time + BASIC_MONTH 周期
```

### 到期回收验证

为了验证到期回收，可以在测试环境把当前会员到期时间改到当前时间之前：

```sql
-- 测试环境使用：模拟会员已经到期
UPDATE user_subscription
SET end_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
WHERE user_id = 10001
  AND status = 1;
```

执行 XXL-JOB `memberExpireRecoverJob`，然后查询订阅状态：

```sql
SELECT user_id, plan_code, status, start_time, end_time
FROM user_subscription
WHERE user_id = 10001;
```

如果没有待生效降级套餐，预期结果：

```text
status = 2
表示订阅已过期
```

查询权益状态：

```sql
SELECT benefit_code, benefit_value, status, end_time
FROM user_benefit
WHERE user_id = 10001
ORDER BY id DESC;
```

预期结果：

```text
原 ACTIVE 权益变为 EXPIRED
后续权益鉴权不再命中
```

查询到期流水：

```sql
SELECT user_id, change_type, before_plan_name, after_plan_name, remark, created_time
FROM subscription_change_log
WHERE user_id = 10001
  AND change_type = 'EXPIRE'
ORDER BY id DESC;
```

预期结果：

```text
change_type = EXPIRE
remark = 会员到期回收
```

最后验证当前会员接口：

```bash
curl -X GET 'http://localhost:8080/member/subscription/current' \
  -H 'Authorization: Bearer your-token'
```

预期结果：

```text
statusName = 已过期
benefits 为空或没有 ACTIVE 权益
```

到这里，会员订阅与权益生命周期的核心实现已经闭环：接口可调用、有效期可叠加、升级可补差、降级可延后、权益可发放和回收、定时任务可补偿。