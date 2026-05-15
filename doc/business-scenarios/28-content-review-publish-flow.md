# 内容审核与发布流程

本文实现原 README 中第 28 个业务场景“内容审核与发布流程”，核心覆盖用户提交内容、系统自动审核、人工复审、审核通过发布、审核失败退回、下架与重新提交等流程。原始场景要求包含审核状态机、自动审核与人工审核结合、敏感词过滤、图片/文本审核、审核记录留痕、审核超时提醒和重复提交处理等核心难点。

## 业务场景与实现目标

本案例以“文章内容发布”为主线实现内容审核流程，评论、商品、课程、资料等内容审核场景也可以复用同一套状态机和审核记录模型。系统目标不是做一个完整 CMS，而是实现后端项目中最核心、最能体现业务建模能力的审核发布闭环。

### 适用业务场景

该方案适合需要“先审核、后发布”的内容型业务。典型场景如下：

| 场景     | 示例                             | 审核重点                     |
| -------- | -------------------------------- | ---------------------------- |
| 文章发布 | 用户投稿、知识库文章、资讯内容   | 标题、正文、封面图、分类     |
| 评论审核 | 社区评论、商品评价、课程评价     | 敏感词、广告内容、辱骂内容   |
| 商品审核 | 商家提交商品                     | 商品名称、详情图、价格描述   |
| 课程审核 | 讲师提交课程                     | 课程标题、介绍、封面、附件   |
| 资料审核 | 用户上传资料、合同模板、文档资源 | 文件名称、文本摘要、附件安全 |
| 社区内容 | 帖子、动态、问答                 | 文本安全、图片安全、重复发布 |

本案例默认实现“文章内容审核”，其他业务只需要把 `content_type` 扩展为不同类型，并在自动审核规则中增加不同内容类型的校验策略。

### 核心功能范围

本案例只实现内容审核发布的核心闭环，重点放在状态流转、敏感词校验、审核记录、人工复审和下架重提。

| 功能         | 是否实现 | 说明                                                |
| ------------ | -------- | --------------------------------------------------- |
| 用户提交内容 | 是       | 创建内容记录，状态进入待审核                        |
| 重复提交控制 | 是       | 同一用户相同内容摘要短时间内不允许重复提交          |
| 敏感词过滤   | 是       | 使用 DFA 敏感词匹配实现文本自动审核                 |
| 自动审核     | 是       | 系统先进行标题、正文、封面等基础校验                |
| 人工复审     | 是       | 自动审核不确定或命中轻度风险时进入人工审核          |
| 审核通过发布 | 是       | 人工通过或自动通过后发布内容                        |
| 审核失败退回 | 是       | 记录失败原因，内容退回用户修改                      |
| 内容下架     | 是       | 已发布内容支持管理员下架                            |
| 重新提交     | 是       | 被拒绝或已下架内容修改后重新进入待审核              |
| 审核记录留痕 | 是       | 每次审核动作写入审核记录表                          |
| 审核超时提醒 | 是       | XXL-JOB 扫描长时间未处理的人工审核内容              |
| 图片审核     | 预留     | 本案例保留第三方内容安全 API 扩展点，不展开完整对接 |
| 多级复杂审批 | 不实现   | 不引入 Flowable，只实现轻量状态机                   |

核心流程目标如下：

```text
用户提交内容
-> 内容进入待审核
-> 系统自动审核文本和基础字段
-> 自动通过则直接发布
-> 自动不确定则进入人工复审
-> 人工审核通过后发布
-> 人工审核拒绝后退回
-> 已发布内容可下架
-> 被拒绝或下架内容可修改后重新提交
```

### 本案例技术栈

本案例使用 Spring Boot 3 + MyBatis-Plus + Redis + RabbitMQ + XXL-JOB 实现轻量级内容审核流程。敏感词过滤使用本地 DFA 算法，第三方内容安全 API 作为扩展点保留。

| 技术               | 用途                                     |
| ------------------ | ---------------------------------------- |
| Spring Boot 3      | 后端基础框架                             |
| MyBatis-Plus       | 内容、审核记录、敏感词等表的 CRUD        |
| MySQL              | 存储内容主表、审核记录表、敏感词表       |
| Redis              | 重复提交控制、敏感词缓存、审核锁         |
| RabbitMQ           | 提交内容后异步触发自动审核               |
| XXL-JOB            | 扫描审核超时任务，提醒人工审核           |
| Sa-Token           | 获取当前登录用户与接口权限控制           |
| Hutool             | 摘要计算、集合处理、字符串处理、日期处理 |
| MinIO              | 内容封面、附件、图片资源存储扩展         |
| 第三方内容安全 API | 文本、图片审核扩展点                     |
| Lombok             | 简化实体类和 DTO 代码                    |

本案例后续代码默认使用如下工程约定：

```text
JDK 17
Spring Boot 3.x
MyBatis-Plus 3.5.x
MySQL 8.x
Redis 6+
RabbitMQ 3.x
基础包名：io.github.atengk.audit
```

## 审核流程设计

审核流程采用“状态机 + 审核记录 + 异步自动审核”的设计。内容主表只保存当前状态，审核记录表保存每一次审核动作，避免把所有过程字段都堆在内容主表中。

### 内容提交流程

内容提交是审核流程的入口。用户提交文章后，系统不直接发布，而是先生成内容摘要，判断是否重复提交，再创建内容记录并发送自动审核消息。

流程如下：

```text
用户提交标题、正文、封面、分类
-> 参数校验
-> 计算内容摘要 content_hash
-> Redis 判断短时间内是否重复提交
-> 写入内容主表
-> 初始状态设置为 PENDING_REVIEW
-> 写入审核记录 SUBMIT
-> 发送 RabbitMQ 自动审核消息
-> 返回提交成功
```

内容提交后的关键结果：

| 字段             | 值               | 说明                   |
| ---------------- | ---------------- | ---------------------- |
| `audit_status`   | `PENDING_REVIEW` | 等待系统自动审核       |
| `publish_status` | `UNPUBLISHED`    | 尚未发布               |
| `content_hash`   | SHA256 摘要      | 用于重复提交控制       |
| `submit_time`    | 当前时间         | 记录提交时间           |
| `version`        | 初始为 0         | 用于乐观锁控制并发更新 |

重复提交控制建议使用 Redis Key：

```text
content:submit:{userId}:{contentHash}
```

示例过期时间：

```text
10 分钟
```

这样可以避免用户频繁点击提交按钮导致重复进入审核队列。

### 自动审核流程

自动审核由 RabbitMQ 消费者异步执行。提交接口只负责落库和投递消息，不在接口中执行敏感词扫描、第三方 API 调用等耗时逻辑。

流程如下：

```text
RabbitMQ 消费自动审核消息
-> 查询内容记录
-> 校验内容是否仍为 PENDING_REVIEW
-> 使用 DFA 匹配标题和正文敏感词
-> 调用第三方内容安全 API 扩展点
-> 根据审核结果决定流转状态
```

自动审核结果分为三类：

| 自动审核结果 | 状态流转                          | 说明               |
| ------------ | --------------------------------- | ------------------ |
| 通过         | `PENDING_REVIEW -> PUBLISHED`     | 内容安全，直接发布 |
| 疑似风险     | `PENDING_REVIEW -> MANUAL_REVIEW` | 需要人工复审       |
| 拒绝         | `PENDING_REVIEW -> REJECTED`      | 明确命中违规内容   |

自动审核建议策略：

```text
未命中敏感词 + 第三方 API 通过
-> 自动发布

命中普通敏感词 / 第三方 API 返回疑似风险
-> 进入人工复审

命中严重敏感词 / 第三方 API 返回违规
-> 直接拒绝
```

审核动作需要写入审核记录：

```text
AUTO_PASS
AUTO_REJECT
AUTO_TO_MANUAL
```

### 人工复审流程

人工复审主要处理自动审核无法明确判断的内容。管理员在后台查看待复审内容，给出通过或拒绝结论。

流程如下：

```text
管理员查询 MANUAL_REVIEW 内容
-> 打开内容详情和自动审核命中原因
-> 点击通过或拒绝
-> 校验当前状态是否仍为 MANUAL_REVIEW
-> 使用分布式锁防止多人重复审核
-> 更新内容状态
-> 写入人工审核记录
```

人工复审动作如下：

| 审核动作 | 状态流转                     | 说明             |
| -------- | ---------------------------- | ---------------- |
| 人工通过 | `MANUAL_REVIEW -> PUBLISHED` | 内容正式发布     |
| 人工拒绝 | `MANUAL_REVIEW -> REJECTED`  | 内容退回用户修改 |

人工审核时必须记录以下信息：

| 字段           | 说明         |
| -------------- | ------------ |
| `auditor_id`   | 审核人 ID    |
| `audit_action` | 审核动作     |
| `audit_result` | 审核结果     |
| `audit_reason` | 审核原因     |
| `risk_words`   | 命中的敏感词 |
| `audit_time`   | 审核时间     |

人工复审需要防止两个管理员同时审核同一条内容。建议使用 Redisson 分布式锁：

```text
lock:content:audit:{contentId}
```

同时数据库更新时也要带状态条件：

```text
where id = ? and audit_status = 'MANUAL_REVIEW'
```

这样即使锁失效，也能通过状态条件避免重复审核。

### 发布与退回流程

发布不是一个孤立动作，而是审核通过后的状态结果。内容发布后，前台才能查询到该内容。

自动审核通过或人工审核通过后，状态更新如下：

```text
audit_status = APPROVED
publish_status = PUBLISHED
publish_time = 当前时间
```

为了便于业务查询，本案例将审核状态和发布状态分开：

| 状态字段         | 示例值                                                    | 作用             |
| ---------------- | --------------------------------------------------------- | ---------------- |
| `audit_status`   | `PENDING_REVIEW`、`MANUAL_REVIEW`、`APPROVED`、`REJECTED` | 表示审核过程     |
| `publish_status` | `UNPUBLISHED`、`PUBLISHED`、`OFFLINE`                     | 表示前台可见状态 |

退回流程发生在审核拒绝时：

```text
自动审核拒绝 / 人工审核拒绝
-> audit_status = REJECTED
-> publish_status = UNPUBLISHED
-> reject_reason = 审核失败原因
-> 写入审核记录
-> 用户可修改后重新提交
```

前台内容查询只查询：

```text
audit_status = APPROVED
and publish_status = PUBLISHED
```

这样可以避免待审核、已拒绝、已下架内容被用户访问。

### 下架与重新提交流程

下架用于处理已发布但后续发现违规、过期、投诉成立或运营主动撤回的内容。下架不删除数据，只改变发布状态并记录操作原因。

下架流程如下：

```text
管理员选择已发布内容
-> 填写下架原因
-> 校验内容是否已发布
-> publish_status = OFFLINE
-> 写入审核记录 OFFLINE
-> 前台不再展示该内容
```

下架后的内容可以修改并重新提交：

```text
用户修改已下架或被拒绝内容
-> 重新计算 content_hash
-> audit_status = PENDING_REVIEW
-> publish_status = UNPUBLISHED
-> submit_time = 当前时间
-> 写入审核记录 RESUBMIT
-> 发送自动审核消息
```

允许重新提交的状态：

| 当前状态                 | 是否允许重新提交 | 说明                                             |
| ------------------------ | ---------------- | ------------------------------------------------ |
| `REJECTED + UNPUBLISHED` | 是               | 审核失败后修改重提                               |
| `APPROVED + OFFLINE`     | 是               | 下架后修改重提                                   |
| `PENDING_REVIEW`         | 否               | 正在审核中，不能重复提交                         |
| `MANUAL_REVIEW`          | 否               | 正在人工审核，不能重复提交                       |
| `APPROVED + PUBLISHED`   | 否               | 已发布内容不能直接重复提交，需先下架或走编辑审核 |

本案例最终形成的核心状态流转如下：

```text
PENDING_REVIEW
    -> APPROVED + PUBLISHED
    -> MANUAL_REVIEW
    -> REJECTED + UNPUBLISHED

MANUAL_REVIEW
    -> APPROVED + PUBLISHED
    -> REJECTED + UNPUBLISHED

APPROVED + PUBLISHED
    -> APPROVED + OFFLINE

REJECTED + UNPUBLISHED
    -> PENDING_REVIEW

APPROVED + OFFLINE
    -> PENDING_REVIEW
```

## 数据库表设计

本节围绕“内容审核与发布流程”的核心闭环建表，重点支持提交、自动审核、人工复审、发布、退回、下架、重新提交、敏感词维护和附件记录。原始 README 中第 28 个场景明确要求覆盖审核状态机、敏感词过滤、文本/图片审核、审核记录留痕、超时提醒和重复提交处理。

### 内容主表

内容主表保存内容的当前审核状态和发布状态。审核过程的详细动作不放在主表中，而是写入审核记录表。

下面 SQL 创建内容主表，适合 MySQL 8.x 使用。

```sql
CREATE TABLE audit_content (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    content_type VARCHAR(32) NOT NULL DEFAULT 'ARTICLE' COMMENT '内容类型：ARTICLE文章 COMMENT评论 PRODUCT商品 COURSE课程',
    title VARCHAR(200) NOT NULL COMMENT '内容标题',
    content_body LONGTEXT NOT NULL COMMENT '内容正文',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    content_hash VARCHAR(128) NOT NULL COMMENT '内容摘要，用于重复提交控制',

    audit_status VARCHAR(32) NOT NULL COMMENT '审核状态：DRAFT草稿 PENDING_REVIEW待审核 MANUAL_REVIEW人工复审 APPROVED已通过 REJECTED已拒绝',
    publish_status VARCHAR(32) NOT NULL COMMENT '发布状态：UNPUBLISHED未发布 PUBLISHED已发布 OFFLINE已下架',

    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '最近一次拒绝原因',
    offline_reason VARCHAR(500) DEFAULT NULL COMMENT '最近一次下架原因',
    submit_time DATETIME DEFAULT NULL COMMENT '提交审核时间',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    offline_time DATETIME DEFAULT NULL COMMENT '下架时间',

    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_status (user_id, audit_status, publish_status),
    KEY idx_audit_status (audit_status, submit_time),
    KEY idx_publish_status (publish_status, publish_time),
    KEY idx_content_hash (user_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核主表';
```

字段设计重点如下：

| 字段             | 说明                                       |
| ---------------- | ------------------------------------------ |
| `audit_status`   | 表示内容审核流程状态                       |
| `publish_status` | 表示前台是否可见                           |
| `content_hash`   | 防止短时间重复提交相同内容                 |
| `version`        | 配合 MyBatis-Plus 乐观锁，防止并发审核覆盖 |
| `reject_reason`  | 保存最近一次拒绝原因，方便用户修改         |
| `offline_reason` | 保存最近一次下架原因，方便重新提交前查看   |

前台内容查询只应该查询：

```sql
SELECT *
FROM audit_content
WHERE audit_status = 'APPROVED'
  AND publish_status = 'PUBLISHED'
  AND deleted = 0;
```

### 审核记录表

审核记录表用于保存每一次提交、自动审核、人工审核、发布、拒绝、下架、重新提交动作。主表只保留当前状态，记录表保留完整过程。

下面 SQL 创建审核记录表。

```sql
CREATE TABLE audit_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    content_type VARCHAR(32) NOT NULL COMMENT '内容类型',

    audit_action VARCHAR(32) NOT NULL COMMENT '审核动作：SUBMIT提交 AUTO_PASS自动通过 AUTO_REJECT自动拒绝 AUTO_TO_MANUAL转人工 MANUAL_PASS人工通过 MANUAL_REJECT人工拒绝 OFFLINE下架 RESUBMIT重新提交',
    from_audit_status VARCHAR(32) DEFAULT NULL COMMENT '变更前审核状态',
    to_audit_status VARCHAR(32) DEFAULT NULL COMMENT '变更后审核状态',
    from_publish_status VARCHAR(32) DEFAULT NULL COMMENT '变更前发布状态',
    to_publish_status VARCHAR(32) DEFAULT NULL COMMENT '变更后发布状态',

    auditor_id BIGINT DEFAULT NULL COMMENT '审核人ID，系统自动审核时为空',
    auditor_name VARCHAR(100) DEFAULT NULL COMMENT '审核人名称',
    audit_reason VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
    risk_words VARCHAR(1000) DEFAULT NULL COMMENT '命中的敏感词，逗号分隔',
    audit_source VARCHAR(32) NOT NULL COMMENT '审核来源：SYSTEM系统 USER用户 ADMIN管理员 JOB定时任务',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_content_id (content_id),
    KEY idx_action_time (audit_action, create_time),
    KEY idx_auditor_time (auditor_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核记录表';
```

审核记录表可以支撑三个常见需求：

| 需求                   | 查询方式                                                   |
| ---------------------- | ---------------------------------------------------------- |
| 查看某条内容的审核轨迹 | 按 `content_id` 查询并按 `create_time` 升序排序            |
| 查看管理员审核工作量   | 按 `auditor_id` 和 `audit_action` 聚合                     |
| 排查自动审核误杀       | 查询 `AUTO_REJECT`、`AUTO_TO_MANUAL` 记录中的 `risk_words` |

### 敏感词表

敏感词表用于后台维护敏感词，服务启动或定时任务加载到 Redis，再构建 DFA 词库进行文本匹配。

下面 SQL 创建敏感词表。

```sql
CREATE TABLE sensitive_word (
    id BIGINT NOT NULL COMMENT '主键ID',
    word VARCHAR(100) NOT NULL COMMENT '敏感词',
    word_level VARCHAR(32) NOT NULL COMMENT '敏感级别：NORMAL普通 SEVERE严重',
    handle_type VARCHAR(32) NOT NULL COMMENT '处理方式：MANUAL转人工 REJECT直接拒绝',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    remark VARCHAR(300) DEFAULT NULL COMMENT '备注',

    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_word (word),
    KEY idx_enabled_level (enabled, word_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';
```

初始化几条测试数据，后续可以直接验证自动审核流转。

```sql
INSERT INTO sensitive_word
(id, word, word_level, handle_type, enabled, remark)
VALUES
(1001, '测试违规词', 'SEVERE', 'REJECT', 1, '严重违规，自动拒绝'),
(1002, '测试风险词', 'NORMAL', 'MANUAL', 1, '普通风险，进入人工复审'),
(1003, '广告引流', 'NORMAL', 'MANUAL', 1, '疑似营销广告');
```

### 内容附件表

内容附件表用于保存封面、图片、文件等资源。图片审核可以基于此表扩展第三方内容安全 API。

下面 SQL 创建内容附件表。

```sql
CREATE TABLE content_attachment (
    id BIGINT NOT NULL COMMENT '主键ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问地址',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型：COVER封面 IMAGE图片 FILE附件',
    mime_type VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
    file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件摘要',

    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '附件审核状态：PENDING_REVIEW待审核 APPROVED通过 REJECTED拒绝',
    audit_reason VARCHAR(500) DEFAULT NULL COMMENT '附件审核原因',

    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_content_id (content_id),
    KEY idx_file_hash (file_hash),
    KEY idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容附件表';
```

如果当前阶段只做文本审核，可以先不实现图片审核逻辑，但附件表建议保留，后续接入 MinIO 和内容安全 API 时不需要重构主表。

## 审核状态机设计

内容审核不能只靠简单字段赋值，否则很容易出现“已拒绝内容被发布”“已下架内容重复下架”“人工审核覆盖自动审核结果”等问题。本案例使用枚举限定状态和动作，再通过服务层统一判断状态流转是否合法。

### 内容状态枚举

审核状态和发布状态分开建模。审核状态描述内容是否通过审核，发布状态描述内容是否对前台可见。

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditStatusEnum.java`

下面代码定义内容审核状态枚举。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容审核状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AuditStatusEnum {

    DRAFT("DRAFT", "草稿"),

    PENDING_REVIEW("PENDING_REVIEW", "待审核"),

    MANUAL_REVIEW("MANUAL_REVIEW", "人工复审"),

    APPROVED("APPROVED", "审核通过"),

    REJECTED("REJECTED", "审核拒绝");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/audit/enums/PublishStatusEnum.java`

下面代码定义内容发布状态枚举。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容发布状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum PublishStatusEnum {

    UNPUBLISHED("UNPUBLISHED", "未发布"),

    PUBLISHED("PUBLISHED", "已发布"),

    OFFLINE("OFFLINE", "已下架");

    private final String code;

    private final String desc;
}
```

### 审核动作枚举

审核动作枚举用于写入审核记录表，也用于判断状态流转是否合法。

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditActionEnum.java`

下面代码定义审核动作枚举。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容审核动作枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AuditActionEnum {

    SUBMIT("SUBMIT", "提交审核"),

    AUTO_PASS("AUTO_PASS", "自动审核通过"),

    AUTO_REJECT("AUTO_REJECT", "自动审核拒绝"),

    AUTO_TO_MANUAL("AUTO_TO_MANUAL", "自动审核转人工"),

    MANUAL_PASS("MANUAL_PASS", "人工审核通过"),

    MANUAL_REJECT("MANUAL_REJECT", "人工审核拒绝"),

    OFFLINE("OFFLINE", "内容下架"),

    RESUBMIT("RESUBMIT", "重新提交");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditSourceEnum.java`

下面代码定义审核来源枚举。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容审核来源枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AuditSourceEnum {

    USER("USER", "用户"),

    SYSTEM("SYSTEM", "系统"),

    ADMIN("ADMIN", "管理员"),

    JOB("JOB", "定时任务");

    private final String code;

    private final String desc;
}
```

### 状态流转规则

状态流转规则建议集中在一个类中处理，不要散落在 Controller 或多个 Service 方法中。这样后续新增状态时更容易维护。

文件位置：`src/main/java/io/github/atengk/audit/statemachine/ContentAuditStateMachine.java`

下面代码封装审核状态和发布状态的合法流转判断。

```java
package io.github.atengk.audit.statemachine;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.audit.enums.AuditActionEnum;
import io.github.atengk.audit.enums.AuditStatusEnum;
import io.github.atengk.audit.enums.PublishStatusEnum;

import java.util.Map;
import java.util.Set;

/**
 * 内容审核状态机
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ContentAuditStateMachine {

    private static final Map<String, Set<String>> ALLOW_ACTION_MAP = Map.of(
            AuditStatusEnum.DRAFT.getCode() + ":" + PublishStatusEnum.UNPUBLISHED.getCode(),
            Set.of(AuditActionEnum.SUBMIT.getCode()),

            AuditStatusEnum.PENDING_REVIEW.getCode() + ":" + PublishStatusEnum.UNPUBLISHED.getCode(),
            Set.of(
                    AuditActionEnum.AUTO_PASS.getCode(),
                    AuditActionEnum.AUTO_REJECT.getCode(),
                    AuditActionEnum.AUTO_TO_MANUAL.getCode()
            ),

            AuditStatusEnum.MANUAL_REVIEW.getCode() + ":" + PublishStatusEnum.UNPUBLISHED.getCode(),
            Set.of(
                    AuditActionEnum.MANUAL_PASS.getCode(),
                    AuditActionEnum.MANUAL_REJECT.getCode()
            ),

            AuditStatusEnum.REJECTED.getCode() + ":" + PublishStatusEnum.UNPUBLISHED.getCode(),
            Set.of(AuditActionEnum.RESUBMIT.getCode()),

            AuditStatusEnum.APPROVED.getCode() + ":" + PublishStatusEnum.PUBLISHED.getCode(),
            Set.of(AuditActionEnum.OFFLINE.getCode()),

            AuditStatusEnum.APPROVED.getCode() + ":" + PublishStatusEnum.OFFLINE.getCode(),
            Set.of(AuditActionEnum.RESUBMIT.getCode())
    );

    /**
     * 判断当前状态是否允许执行指定动作
     *
     * @param auditStatus 当前审核状态
     * @param publishStatus 当前发布状态
     * @param action 审核动作
     * @return 是否允许执行
     */
    public static boolean allow(String auditStatus, String publishStatus, String action) {
        String key = auditStatus + ":" + publishStatus;
        Set<String> actions = ALLOW_ACTION_MAP.get(key);
        return CollUtil.isNotEmpty(actions) && actions.contains(action);
    }

    /**
     * 校验当前状态是否允许执行指定动作
     *
     * @param auditStatus 当前审核状态
     * @param publishStatus 当前发布状态
     * @param action 审核动作
     */
    public static void check(String auditStatus, String publishStatus, String action) {
        if (!allow(auditStatus, publishStatus, action)) {
            throw new IllegalStateException("当前状态不允许执行该审核动作：" + action);
        }
    }

    private ContentAuditStateMachine() {
    }
}
```

核心流转规则如下：

| 当前审核状态     | 当前发布状态  | 动作             | 新审核状态       | 新发布状态    |
| ---------------- | ------------- | ---------------- | ---------------- | ------------- |
| `DRAFT`          | `UNPUBLISHED` | `SUBMIT`         | `PENDING_REVIEW` | `UNPUBLISHED` |
| `PENDING_REVIEW` | `UNPUBLISHED` | `AUTO_PASS`      | `APPROVED`       | `PUBLISHED`   |
| `PENDING_REVIEW` | `UNPUBLISHED` | `AUTO_REJECT`    | `REJECTED`       | `UNPUBLISHED` |
| `PENDING_REVIEW` | `UNPUBLISHED` | `AUTO_TO_MANUAL` | `MANUAL_REVIEW`  | `UNPUBLISHED` |
| `MANUAL_REVIEW`  | `UNPUBLISHED` | `MANUAL_PASS`    | `APPROVED`       | `PUBLISHED`   |
| `MANUAL_REVIEW`  | `UNPUBLISHED` | `MANUAL_REJECT`  | `REJECTED`       | `UNPUBLISHED` |
| `APPROVED`       | `PUBLISHED`   | `OFFLINE`        | `APPROVED`       | `OFFLINE`     |
| `REJECTED`       | `UNPUBLISHED` | `RESUBMIT`       | `PENDING_REVIEW` | `UNPUBLISHED` |
| `APPROVED`       | `OFFLINE`     | `RESUBMIT`       | `PENDING_REVIEW` | `UNPUBLISHED` |

## 项目基础配置

本节给出项目依赖、配置文件和基础配置类。后续核心代码默认基于这些配置运行。

### Maven 依赖配置

文件位置：`pom.xml`

下面 Maven 配置包含 Spring Boot Web、MyBatis-Plus、MySQL、Redis、Redisson、RabbitMQ、Hutool、Sa-Token、XXL-JOB 和 Lombok。

```xml
<dependencies>
    <!-- Web 接口基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus ORM -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.9</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis 缓存与重复提交控制 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson 分布式锁，用于人工审核防并发 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.37.0</version>
    </dependency>

    <!-- RabbitMQ 异步自动审核 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Sa-Token 登录认证与权限控制 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Hutool 工具类，摘要、字符串、集合、日期处理会用到 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- XXL-JOB 定时任务，用于审核超时提醒 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Lombok 简化实体和 DTO -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果使用 Spring Boot 父工程管理版本，可以只固定 MyBatis-Plus、Redisson、Sa-Token、Hutool、XXL-JOB 等第三方依赖版本。

### application.yml 配置

文件位置：`src/main/resources/application.yml`

下面配置包含数据库、Redis、RabbitMQ、MyBatis-Plus、Sa-Token、XXL-JOB 和业务审核参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: content-audit-demo

  datasource:
    # MySQL 连接配置
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/content_audit?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      # Redis 用于重复提交控制、敏感词缓存、审核锁辅助
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

  rabbitmq:
    # RabbitMQ 用于异步自动审核
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000ms

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.audit.entity
  global-config:
    db-config:
      # 使用雪花算法生成主键
      id-type: assign_id
      # 逻辑删除字段配置
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    # SQL 日志，开发环境可开启
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期，单位秒
  timeout: 86400
  # 是否允许并发登录
  is-concurrent: true
  # 是否从请求头读取 Token
  is-read-header: true

xxl:
  job:
    admin:
      # XXL-JOB 管理端地址
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      # 当前执行器名称
      appname: content-audit-demo
      address:
      ip:
      port: 9999
      logpath: ./logs/xxl-job
      logretentiondays: 7
    accessToken: default_token

audit:
  content:
    # 重复提交限制时间，单位分钟
    duplicate-submit-minutes: 10
    # 人工审核超过多少分钟未处理触发提醒
    manual-timeout-minutes: 30
    # 敏感词 Redis 缓存 Key
    sensitive-word-cache-key: audit:sensitive:word:list
    # 自动审核队列
    auto-audit-queue: content.audit.auto.queue
    # 自动审核交换机
    auto-audit-exchange: content.audit.exchange
    # 自动审核路由键
    auto-audit-routing-key: content.audit.auto
```

对应的业务配置类如下。

文件位置：`src/main/java/io/github/atengk/audit/config/AuditContentProperties.java`

下面代码读取 `audit.content` 下的业务审核配置。

```java
package io.github.atengk.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 内容审核业务配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "audit.content")
public class AuditContentProperties {

    /**
     * 重复提交限制时间，单位分钟
     */
    private Integer duplicateSubmitMinutes = 10;

    /**
     * 人工审核超时时间，单位分钟
     */
    private Integer manualTimeoutMinutes = 30;

    /**
     * 敏感词缓存 Key
     */
    private String sensitiveWordCacheKey = "audit:sensitive:word:list";

    /**
     * 自动审核队列
     */
    private String autoAuditQueue = "content.audit.auto.queue";

    /**
     * 自动审核交换机
     */
    private String autoAuditExchange = "content.audit.exchange";

    /**
     * 自动审核路由键
     */
    private String autoAuditRoutingKey = "content.audit.auto";
}
```

### MyBatis-Plus 配置

MyBatis-Plus 需要配置分页插件和乐观锁插件。分页用于后台审核列表查询，乐观锁用于防止审核状态并发覆盖。

文件位置：`src/main/java/io/github/atengk/audit/config/MybatisPlusConfig.java`

下面代码配置 MyBatis-Plus 分页插件和乐观锁插件。

```java
package io.github.atengk.audit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件，配合实体类 @Version 字段使用
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件，后台审核列表会使用
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        log.info("初始化 MyBatis-Plus 插件完成");
        return interceptor;
    }
}
```

后续内容实体类中的 `version` 字段需要加上：

```java
@Version
private Integer version;
```

逻辑删除字段需要加上：

```java
@TableLogic
private Integer deleted;
```

### Redis 与 RabbitMQ 配置

Redis 用于缓存敏感词、控制重复提交和辅助审核锁；RabbitMQ 用于内容提交后异步触发自动审核。

文件位置：`src/main/java/io/github/atengk/audit/config/RedisConfig.java`

下面代码配置 RedisTemplate 的序列化方式，避免默认 JDK 序列化导致 Redis 中内容不可读。

```java
package io.github.atengk.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate 用于字符串类型缓存
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();

        log.info("初始化 StringRedisTemplate 完成");
        return redisTemplate;
    }
}
```

文件位置：`src/main/java/io/github/atengk/audit/config/RabbitMqConfig.java`

下面代码声明自动审核交换机、队列和绑定关系。

```java
package io.github.atengk.audit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final AuditContentProperties auditContentProperties;

    /**
     * 内容审核交换机
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange contentAuditExchange() {
        return new DirectExchange(
                auditContentProperties.getAutoAuditExchange(),
                true,
                false
        );
    }

    /**
     * 自动审核队列
     *
     * @return Queue
     */
    @Bean
    public Queue autoAuditQueue() {
        return new Queue(
                auditContentProperties.getAutoAuditQueue(),
                true
        );
    }

    /**
     * 自动审核队列绑定
     *
     * @return Binding
     */
    @Bean
    public Binding autoAuditBinding() {
        log.info("初始化内容自动审核 MQ 绑定关系");
        return BindingBuilder
                .bind(autoAuditQueue())
                .to(contentAuditExchange())
                .with(auditContentProperties.getAutoAuditRoutingKey());
    }
}
```

自动审核消息体建议保持简单，只传内容 ID，不传完整内容，避免消息过大和内容变更导致数据不一致。

```json
{
  "contentId": 1900000000000000001
}
```

后续消费者收到消息后，根据 `contentId` 查询数据库中的最新内容，再判断状态是否仍为 `PENDING_REVIEW`。这样可以避免消息延迟导致处理旧数据。

## 核心代码实现

本节实现内容审核与发布流程的核心代码，覆盖提交、敏感词审核、自动审核、人工审核、下架和重新提交。这里按“能跑通核心业务闭环”的标准组织代码，不展开后台字典管理、MinIO 上传、第三方图片审核 SDK 等外围能力。该实现对应 README 中“内容审核与发布流程”的核心要求：审核状态机、自动审核、人工复审、敏感词过滤、审核记录留痕、超时提醒和重复提交处理。

### 实体类与枚举定义

本小节给出内容主表、审核记录表、敏感词表对应的实体类。枚举类在上一节已经给出 `AuditStatusEnum`、`PublishStatusEnum`、`AuditActionEnum`、`AuditSourceEnum`，这里补充敏感词处理方式和自动审核结果枚举。

文件位置：`src/main/java/io/github/atengk/audit/entity/AuditContent.java`

下面代码定义内容主表实体，保存内容当前审核状态和发布状态。

```java
package io.github.atengk.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核主表实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("audit_content")
public class AuditContent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String contentType;

    private String title;

    private String contentBody;

    private String coverUrl;

    private Long categoryId;

    private String contentHash;

    private String auditStatus;

    private String publishStatus;

    private String rejectReason;

    private String offlineReason;

    private LocalDateTime submitTime;

    private LocalDateTime publishTime;

    private LocalDateTime offlineTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/entity/AuditRecord.java`

下面代码定义审核记录实体，每次状态变更都写入一条记录。

```java
package io.github.atengk.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("audit_record")
public class AuditRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long contentId;

    private String contentType;

    private String auditAction;

    private String fromAuditStatus;

    private String toAuditStatus;

    private String fromPublishStatus;

    private String toPublishStatus;

    private Long auditorId;

    private String auditorName;

    private String auditReason;

    private String riskWords;

    private String auditSource;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/entity/SensitiveWord.java`

下面代码定义敏感词实体，用于加载 DFA 词库。

```java
package io.github.atengk.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String word;

    private String wordLevel;

    private String handleType;

    private Integer enabled;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/enums/SensitiveHandleTypeEnum.java`

下面代码定义敏感词命中后的处理方式。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 敏感词处理方式枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum SensitiveHandleTypeEnum {

    MANUAL("MANUAL", "转人工复审"),

    REJECT("REJECT", "直接拒绝");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/audit/enums/AutoAuditDecisionEnum.java`

下面代码定义自动审核结果枚举。

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自动审核结果枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AutoAuditDecisionEnum {

    PASS("PASS", "自动通过"),

    TO_MANUAL("TO_MANUAL", "转人工复审"),

    REJECT("REJECT", "自动拒绝");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/audit/dto/ContentSubmitRequest.java`

下面代码定义内容提交请求对象。

```java
package io.github.atengk.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容提交请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContentSubmitRequest {

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String contentBody;

    private String coverUrl;

    private Long categoryId;
}
```

文件位置：`src/main/java/io/github/atengk/audit/dto/ManualAuditRequest.java`

下面代码定义人工审核请求对象。

```java
package io.github.atengk.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 人工审核请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ManualAuditRequest {

    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String reason;
}
```

文件位置：`src/main/java/io/github/atengk/audit/dto/OfflineRequest.java`

下面代码定义内容下架请求对象。

```java
package io.github.atengk.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 内容下架请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OfflineRequest {

    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    @NotBlank(message = "下架原因不能为空")
    private String reason;
}
```

文件位置：`src/main/java/io/github/atengk/audit/dto/AutoAuditMessage.java`

下面代码定义自动审核 MQ 消息体，只传内容 ID，消费端再查库获取最新数据。

```java
package io.github.atengk.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自动审核消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoAuditMessage {

    private Long contentId;
}
```

文件位置：`src/main/java/io/github/atengk/audit/vo/ContentSubmitVO.java`

下面代码定义提交成功后的响应对象。

```java
package io.github.atengk.audit.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 内容提交响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@AllArgsConstructor
public class ContentSubmitVO {

    private Long contentId;

    private String auditStatus;

    private String publishStatus;
}
```

文件位置：`src/main/java/io/github/atengk/audit/common/R.java`

下面代码定义统一响应对象，便于接口示例直接使用。

```java
package io.github.atengk.audit.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 统一响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @return 统一响应
     */
    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }
}
```

### Mapper 与 Service 结构

本案例使用 MyBatis-Plus 的 `BaseMapper` 完成基础 CRUD。复杂更新逻辑放在 Service 层，并且所有状态变更必须同时写入审核记录。

文件位置：`src/main/java/io/github/atengk/audit/mapper/AuditContentMapper.java`

```java
package io.github.atengk.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.audit.entity.AuditContent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容审核主表 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AuditContentMapper extends BaseMapper<AuditContent> {
}
```

文件位置：`src/main/java/io/github/atengk/audit/mapper/AuditRecordMapper.java`

```java
package io.github.atengk.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.audit.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容审核记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}
```

文件位置：`src/main/java/io/github/atengk/audit/mapper/SensitiveWordMapper.java`

```java
package io.github.atengk.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.audit.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
```

文件位置：`src/main/java/io/github/atengk/audit/service/ContentAuditService.java`

下面代码定义内容审核核心业务接口。

```java
package io.github.atengk.audit.service;

import io.github.atengk.audit.dto.ContentSubmitRequest;
import io.github.atengk.audit.dto.ManualAuditRequest;
import io.github.atengk.audit.dto.OfflineRequest;
import io.github.atengk.audit.vo.ContentSubmitVO;

/**
 * 内容审核服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContentAuditService {

    /**
     * 提交内容
     *
     * @param request 提交请求
     * @return 提交结果
     */
    ContentSubmitVO submit(ContentSubmitRequest request);

    /**
     * 执行自动审核
     *
     * @param contentId 内容ID
     */
    void autoAudit(Long contentId);

    /**
     * 人工审核
     *
     * @param request 人工审核请求
     */
    void manualAudit(ManualAuditRequest request);

    /**
     * 下架内容
     *
     * @param request 下架请求
     */
    void offline(OfflineRequest request);

    /**
     * 重新提交内容
     *
     * @param contentId 内容ID
     */
    void resubmit(Long contentId);
}
```

文件位置：`src/main/java/io/github/atengk/audit/service/SensitiveWordService.java`

下面代码定义敏感词服务接口。

```java
package io.github.atengk.audit.service;

import io.github.atengk.audit.sensitive.SensitiveCheckResult;

import java.util.List;

/**
 * 敏感词服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SensitiveWordService {

    /**
     * 刷新敏感词缓存
     */
    void refreshCache();

    /**
     * 检查文本敏感词
     *
     * @param text 待检测文本
     * @return 检测结果
     */
    SensitiveCheckResult check(String text);

    /**
     * 查询启用的敏感词
     *
     * @return 敏感词列表
     */
    List<String> listEnabledWords();
}
```

### 内容提交接口

内容提交接口完成参数校验、摘要计算、重复提交控制、内容入库、审核记录写入和自动审核消息投递。

文件位置：`src/main/java/io/github/atengk/audit/service/impl/ContentAuditServiceImpl.java`

下面代码是内容审核核心 Service 实现，包含提交、自动审核、人工审核、下架和重新提交逻辑。

```java
package io.github.atengk.audit.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.dto.AutoAuditMessage;
import io.github.atengk.audit.dto.ContentSubmitRequest;
import io.github.atengk.audit.dto.ManualAuditRequest;
import io.github.atengk.audit.dto.OfflineRequest;
import io.github.atengk.audit.entity.AuditContent;
import io.github.atengk.audit.entity.AuditRecord;
import io.github.atengk.audit.enums.*;
import io.github.atengk.audit.mapper.AuditContentMapper;
import io.github.atengk.audit.mapper.AuditRecordMapper;
import io.github.atengk.audit.service.ContentAuditService;
import io.github.atengk.audit.service.SensitiveWordService;
import io.github.atengk.audit.sensitive.SensitiveCheckResult;
import io.github.atengk.audit.statemachine.ContentAuditStateMachine;
import io.github.atengk.audit.vo.ContentSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 内容审核服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAuditServiceImpl implements ContentAuditService {

    private final AuditContentMapper auditContentMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final SensitiveWordService sensitiveWordService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;
    private final AuditContentProperties auditContentProperties;

    /**
     * 提交内容
     *
     * @param request 提交请求
     * @return 提交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentSubmitVO submit(ContentSubmitRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String contentHash = buildContentHash(request);

        checkDuplicateSubmit(userId, contentHash);

        AuditContent content = new AuditContent();
        content.setUserId(userId);
        content.setContentType(request.getContentType());
        content.setTitle(request.getTitle());
        content.setContentBody(request.getContentBody());
        content.setCoverUrl(request.getCoverUrl());
        content.setCategoryId(request.getCategoryId());
        content.setContentHash(contentHash);
        content.setAuditStatus(AuditStatusEnum.PENDING_REVIEW.getCode());
        content.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        content.setSubmitTime(LocalDateTime.now());
        content.setVersion(0);
        content.setDeleted(0);

        auditContentMapper.insert(content);

        saveRecord(
                content,
                AuditActionEnum.SUBMIT.getCode(),
                null,
                content.getAuditStatus(),
                null,
                content.getPublishStatus(),
                userId,
                "用户提交",
                "用户提交内容进入审核",
                null,
                AuditSourceEnum.USER.getCode()
        );

        sendAutoAuditMessage(content.getId());

        log.info("用户提交内容成功，contentId={}，userId={}", content.getId(), userId);
        return new ContentSubmitVO(content.getId(), content.getAuditStatus(), content.getPublishStatus());
    }

    /**
     * 执行自动审核
     *
     * @param contentId 内容ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAudit(Long contentId) {
        AuditContent content = auditContentMapper.selectById(contentId);
        if (content == null) {
            log.warn("自动审核内容不存在，contentId={}", contentId);
            return;
        }

        if (!CharSequenceUtil.equals(content.getAuditStatus(), AuditStatusEnum.PENDING_REVIEW.getCode())) {
            log.info("内容状态已变化，跳过自动审核，contentId={}，auditStatus={}", contentId, content.getAuditStatus());
            return;
        }

        String text = content.getTitle() + "\n" + content.getContentBody();
        SensitiveCheckResult checkResult = sensitiveWordService.check(text);

        if (checkResult.isReject()) {
            autoReject(content, checkResult);
            return;
        }

        if (checkResult.isNeedManual()) {
            autoToManual(content, checkResult);
            return;
        }

        autoPass(content);
    }

    /**
     * 人工审核
     *
     * @param request 人工审核请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAudit(ManualAuditRequest request) {
        Long auditorId = StpUtil.getLoginIdAsLong();
        RLock lock = redissonClient.getLock("lock:content:audit:" + request.getContentId());

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("当前内容正在审核中，请稍后再试");
            }

            AuditContent content = auditContentMapper.selectById(request.getContentId());
            if (content == null) {
                throw new IllegalArgumentException("内容不存在");
            }

            ContentAuditStateMachine.check(
                    content.getAuditStatus(),
                    content.getPublishStatus(),
                    request.getAction()
            );

            if (CharSequenceUtil.equals(request.getAction(), AuditActionEnum.MANUAL_PASS.getCode())) {
                manualPass(content, auditorId, request.getReason());
                return;
            }

            if (CharSequenceUtil.equals(request.getAction(), AuditActionEnum.MANUAL_REJECT.getCode())) {
                manualReject(content, auditorId, request.getReason());
                return;
            }

            throw new IllegalArgumentException("人工审核动作不支持：" + request.getAction());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取审核锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 下架内容
     *
     * @param request 下架请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(OfflineRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();

        AuditContent content = auditContentMapper.selectById(request.getContentId());
        if (content == null) {
            throw new IllegalArgumentException("内容不存在");
        }

        ContentAuditStateMachine.check(
                content.getAuditStatus(),
                content.getPublishStatus(),
                AuditActionEnum.OFFLINE.getCode()
        );

        String fromPublishStatus = content.getPublishStatus();
        content.setPublishStatus(PublishStatusEnum.OFFLINE.getCode());
        content.setOfflineReason(request.getReason());
        content.setOfflineTime(LocalDateTime.now());

        int updated = auditContentMapper.updateById(content);
        if (updated <= 0) {
            throw new IllegalStateException("内容下架失败，请重试");
        }

        saveRecord(
                content,
                AuditActionEnum.OFFLINE.getCode(),
                content.getAuditStatus(),
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                adminId,
                "管理员",
                request.getReason(),
                null,
                AuditSourceEnum.ADMIN.getCode()
        );

        log.info("内容下架成功，contentId={}，adminId={}", content.getId(), adminId);
    }

    /**
     * 重新提交内容
     *
     * @param contentId 内容ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long contentId) {
        Long userId = StpUtil.getLoginIdAsLong();

        AuditContent content = auditContentMapper.selectById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("内容不存在");
        }
        if (!content.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能重新提交自己的内容");
        }

        ContentAuditStateMachine.check(
                content.getAuditStatus(),
                content.getPublishStatus(),
                AuditActionEnum.RESUBMIT.getCode()
        );

        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setContentHash(DigestUtil.sha256Hex(content.getTitle() + "\n" + content.getContentBody()));
        content.setAuditStatus(AuditStatusEnum.PENDING_REVIEW.getCode());
        content.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        content.setRejectReason(null);
        content.setOfflineReason(null);
        content.setSubmitTime(LocalDateTime.now());
        content.setPublishTime(null);
        content.setOfflineTime(null);

        int updated = auditContentMapper.updateById(content);
        if (updated <= 0) {
            throw new IllegalStateException("重新提交失败，请重试");
        }

        saveRecord(
                content,
                AuditActionEnum.RESUBMIT.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                userId,
                "用户重新提交",
                "用户修改后重新提交审核",
                null,
                AuditSourceEnum.USER.getCode()
        );

        sendAutoAuditMessage(content.getId());

        log.info("内容重新提交成功，contentId={}，userId={}", content.getId(), userId);
    }

    /**
     * 构建内容摘要
     *
     * @param request 提交请求
     * @return 内容摘要
     */
    private String buildContentHash(ContentSubmitRequest request) {
        String raw = request.getContentType()
                + "|" + request.getTitle()
                + "|" + request.getContentBody()
                + "|" + CharSequenceUtil.nullToEmpty(request.getCoverUrl());
        return DigestUtil.sha256Hex(raw);
    }

    /**
     * 检查重复提交
     *
     * @param userId 用户ID
     * @param contentHash 内容摘要
     */
    private void checkDuplicateSubmit(Long userId, String contentHash) {
        String key = "content:submit:" + userId + ":" + contentHash;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                auditContentProperties.getDuplicateSubmitMinutes(),
                TimeUnit.MINUTES
        );
        if (!Boolean.TRUE.equals(success)) {
            throw new IllegalStateException("请勿重复提交相同内容");
        }
    }

    /**
     * 发送自动审核消息
     *
     * @param contentId 内容ID
     */
    private void sendAutoAuditMessage(Long contentId) {
        AutoAuditMessage message = new AutoAuditMessage(contentId);
        rabbitTemplate.convertAndSend(
                auditContentProperties.getAutoAuditExchange(),
                auditContentProperties.getAutoAuditRoutingKey(),
                JSONUtil.toJsonStr(message)
        );
        log.info("发送内容自动审核消息成功，contentId={}", contentId);
    }

    /**
     * 自动审核通过
     *
     * @param content 内容
     */
    private void autoPass(AuditContent content) {
        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setAuditStatus(AuditStatusEnum.APPROVED.getCode());
        content.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        content.setPublishTime(LocalDateTime.now());

        updateStatusStrict(content, fromAuditStatus, fromPublishStatus);

        saveRecord(
                content,
                AuditActionEnum.AUTO_PASS.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                null,
                "系统自动审核",
                "自动审核通过",
                null,
                AuditSourceEnum.SYSTEM.getCode()
        );

        log.info("内容自动审核通过，contentId={}", content.getId());
    }

    /**
     * 自动审核拒绝
     *
     * @param content 内容
     * @param checkResult 敏感词检查结果
     */
    private void autoReject(AuditContent content, SensitiveCheckResult checkResult) {
        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setAuditStatus(AuditStatusEnum.REJECTED.getCode());
        content.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        content.setRejectReason("命中严重敏感词");

        updateStatusStrict(content, fromAuditStatus, fromPublishStatus);

        saveRecord(
                content,
                AuditActionEnum.AUTO_REJECT.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                null,
                "系统自动审核",
                "命中严重敏感词，自动拒绝",
                CollUtil.join(checkResult.getRiskWords(), ","),
                AuditSourceEnum.SYSTEM.getCode()
        );

        log.info("内容自动审核拒绝，contentId={}，riskWords={}", content.getId(), checkResult.getRiskWords());
    }

    /**
     * 自动审核转人工
     *
     * @param content 内容
     * @param checkResult 敏感词检查结果
     */
    private void autoToManual(AuditContent content, SensitiveCheckResult checkResult) {
        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setAuditStatus(AuditStatusEnum.MANUAL_REVIEW.getCode());
        content.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());

        updateStatusStrict(content, fromAuditStatus, fromPublishStatus);

        saveRecord(
                content,
                AuditActionEnum.AUTO_TO_MANUAL.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                null,
                "系统自动审核",
                "命中风险词，转人工复审",
                CollUtil.join(checkResult.getRiskWords(), ","),
                AuditSourceEnum.SYSTEM.getCode()
        );

        log.info("内容自动审核转人工，contentId={}，riskWords={}", content.getId(), checkResult.getRiskWords());
    }

    /**
     * 人工审核通过
     *
     * @param content 内容
     * @param auditorId 审核人ID
     * @param reason 审核原因
     */
    private void manualPass(AuditContent content, Long auditorId, String reason) {
        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setAuditStatus(AuditStatusEnum.APPROVED.getCode());
        content.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        content.setPublishTime(LocalDateTime.now());

        updateStatusStrict(content, fromAuditStatus, fromPublishStatus);

        saveRecord(
                content,
                AuditActionEnum.MANUAL_PASS.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                auditorId,
                "管理员",
                CharSequenceUtil.blankToDefault(reason, "人工审核通过"),
                null,
                AuditSourceEnum.ADMIN.getCode()
        );

        log.info("人工审核通过，contentId={}，auditorId={}", content.getId(), auditorId);
    }

    /**
     * 人工审核拒绝
     *
     * @param content 内容
     * @param auditorId 审核人ID
     * @param reason 审核原因
     */
    private void manualReject(AuditContent content, Long auditorId, String reason) {
        String fromAuditStatus = content.getAuditStatus();
        String fromPublishStatus = content.getPublishStatus();

        content.setAuditStatus(AuditStatusEnum.REJECTED.getCode());
        content.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        content.setRejectReason(CharSequenceUtil.blankToDefault(reason, "人工审核拒绝"));

        updateStatusStrict(content, fromAuditStatus, fromPublishStatus);

        saveRecord(
                content,
                AuditActionEnum.MANUAL_REJECT.getCode(),
                fromAuditStatus,
                content.getAuditStatus(),
                fromPublishStatus,
                content.getPublishStatus(),
                auditorId,
                "管理员",
                content.getRejectReason(),
                null,
                AuditSourceEnum.ADMIN.getCode()
        );

        log.info("人工审核拒绝，contentId={}，auditorId={}", content.getId(), auditorId);
    }

    /**
     * 严格按原状态更新，避免并发状态覆盖
     *
     * @param content 内容
     * @param fromAuditStatus 原审核状态
     * @param fromPublishStatus 原发布状态
     */
    private void updateStatusStrict(AuditContent content, String fromAuditStatus, String fromPublishStatus) {
        LambdaUpdateWrapper<AuditContent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuditContent::getId, content.getId())
                .eq(AuditContent::getAuditStatus, fromAuditStatus)
                .eq(AuditContent::getPublishStatus, fromPublishStatus)
                .set(AuditContent::getAuditStatus, content.getAuditStatus())
                .set(AuditContent::getPublishStatus, content.getPublishStatus())
                .set(AuditContent::getRejectReason, content.getRejectReason())
                .set(AuditContent::getOfflineReason, content.getOfflineReason())
                .set(AuditContent::getPublishTime, content.getPublishTime())
                .set(AuditContent::getOfflineTime, content.getOfflineTime());

        int updated = auditContentMapper.update(wrapper);
        if (updated <= 0) {
            throw new IllegalStateException("内容状态已变化，请刷新后重试");
        }
    }

    /**
     * 保存审核记录
     *
     * @param content 内容
     * @param action 审核动作
     * @param fromAuditStatus 原审核状态
     * @param toAuditStatus 新审核状态
     * @param fromPublishStatus 原发布状态
     * @param toPublishStatus 新发布状态
     * @param auditorId 审核人ID
     * @param auditorName 审核人名称
     * @param reason 审核原因
     * @param riskWords 风险词
     * @param source 审核来源
     */
    private void saveRecord(
            AuditContent content,
            String action,
            String fromAuditStatus,
            String toAuditStatus,
            String fromPublishStatus,
            String toPublishStatus,
            Long auditorId,
            String auditorName,
            String reason,
            String riskWords,
            String source
    ) {
        AuditRecord record = new AuditRecord();
        record.setContentId(content.getId());
        record.setContentType(content.getContentType());
        record.setAuditAction(action);
        record.setFromAuditStatus(fromAuditStatus);
        record.setToAuditStatus(toAuditStatus);
        record.setFromPublishStatus(fromPublishStatus);
        record.setToPublishStatus(toPublishStatus);
        record.setAuditorId(auditorId);
        record.setAuditorName(auditorName);
        record.setAuditReason(reason);
        record.setRiskWords(riskWords);
        record.setAuditSource(source);
        record.setCreateTime(LocalDateTime.now());

        auditRecordMapper.insert(record);
    }
}
```

### 敏感词 DFA 过滤

敏感词 DFA 负责快速匹配标题和正文中的风险词。这里给出一个轻量实现，适合中小规模敏感词库。敏感词非常多时可以换成 AC 自动机或专门的内容安全服务。

文件位置：`src/main/java/io/github/atengk/audit/sensitive/SensitiveCheckResult.java`

下面代码定义敏感词检测结果。

```java
package io.github.atengk.audit.sensitive;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词检测结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SensitiveCheckResult {

    private boolean needManual;

    private boolean reject;

    private List<String> riskWords = new ArrayList<>();
}
```

文件位置：`src/main/java/io/github/atengk/audit/sensitive/SensitiveWordDfaMatcher.java`

下面代码实现 DFA 敏感词匹配器。

```java
package io.github.atengk.audit.sensitive;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DFA 敏感词匹配器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class SensitiveWordDfaMatcher {

    private static final String END_FLAG = "isEnd";

    private volatile Map<String, Object> wordTree = new HashMap<>();

    /**
     * 重建敏感词树
     *
     * @param words 敏感词列表
     */
    public void rebuild(Collection<String> words) {
        Map<String, Object> root = new HashMap<>();
        if (CollUtil.isEmpty(words)) {
            this.wordTree = root;
            log.info("敏感词库为空，已清空 DFA 词树");
            return;
        }

        for (String word : words) {
            if (CharSequenceUtil.isBlank(word)) {
                continue;
            }
            Map<String, Object> current = root;
            for (char ch : word.toCharArray()) {
                String key = String.valueOf(ch);
                current = (Map<String, Object>) current.computeIfAbsent(key, item -> new HashMap<String, Object>());
            }
            current.put(END_FLAG, Boolean.TRUE);
        }

        this.wordTree = root;
        log.info("DFA 敏感词树重建完成，wordCount={}", words.size());
    }

    /**
     * 匹配文本中的敏感词
     *
     * @param text 待检测文本
     * @return 命中的敏感词列表
     */
    public List<String> match(String text) {
        if (CharSequenceUtil.isBlank(text) || wordTree.isEmpty()) {
            return List.of();
        }

        Set<String> result = new LinkedHashSet<>();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            Map<String, Object> current = wordTree;
            StringBuilder matched = new StringBuilder();

            for (int j = i; j < chars.length; j++) {
                String key = String.valueOf(chars[j]);
                Object next = current.get(key);
                if (!(next instanceof Map)) {
                    break;
                }

                matched.append(chars[j]);
                current = (Map<String, Object>) next;

                if (Boolean.TRUE.equals(current.get(END_FLAG))) {
                    result.add(matched.toString());
                }
            }
        }

        return new ArrayList<>(result);
    }
}
```

文件位置：`src/main/java/io/github/atengk/audit/service/impl/SensitiveWordServiceImpl.java`

下面代码实现敏感词加载、缓存和检测。

```java
package io.github.atengk.audit.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.entity.SensitiveWord;
import io.github.atengk.audit.enums.SensitiveHandleTypeEnum;
import io.github.atengk.audit.mapper.SensitiveWordMapper;
import io.github.atengk.audit.sensitive.SensitiveCheckResult;
import io.github.atengk.audit.sensitive.SensitiveWordDfaMatcher;
import io.github.atengk.audit.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 敏感词服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordDfaMatcher sensitiveWordDfaMatcher;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuditContentProperties auditContentProperties;

    private volatile Map<String, SensitiveWord> sensitiveWordMap = Map.of();

    /**
     * 应用启动后初始化敏感词词库
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 刷新敏感词缓存
     */
    @Override
    public void refreshCache() {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getEnabled, 1)
        );

        this.sensitiveWordMap = words.stream()
                .collect(Collectors.toMap(SensitiveWord::getWord, Function.identity(), (a, b) -> a));

        List<String> wordList = words.stream()
                .map(SensitiveWord::getWord)
                .toList();

        sensitiveWordDfaMatcher.rebuild(wordList);
        stringRedisTemplate.opsForValue().set(
                auditContentProperties.getSensitiveWordCacheKey(),
                JSONUtil.toJsonStr(wordList)
        );

        log.info("刷新敏感词缓存完成，wordCount={}", wordList.size());
    }

    /**
     * 检查文本敏感词
     *
     * @param text 待检测文本
     * @return 检测结果
     */
    @Override
    public SensitiveCheckResult check(String text) {
        List<String> riskWords = sensitiveWordDfaMatcher.match(text);

        SensitiveCheckResult result = new SensitiveCheckResult();
        result.setRiskWords(riskWords);

        if (CollUtil.isEmpty(riskWords)) {
            return result;
        }

        boolean reject = riskWords.stream()
                .map(sensitiveWordMap::get)
                .anyMatch(item -> item != null && SensitiveHandleTypeEnum.REJECT.getCode().equals(item.getHandleType()));

        if (reject) {
            result.setReject(true);
            return result;
        }

        result.setNeedManual(true);
        return result;
    }

    /**
     * 查询启用的敏感词
     *
     * @return 敏感词列表
     */
    @Override
    public List<String> listEnabledWords() {
        return sensitiveWordMap.keySet().stream().toList();
    }
}
```

### 自动审核处理

自动审核通过 RabbitMQ 异步消费，避免提交接口阻塞。消费者收到消息后只根据 `contentId` 查库处理，并且会二次校验当前状态，避免重复消费导致状态错乱。

文件位置：`src/main/java/io/github/atengk/audit/mq/AutoAuditMessageConsumer.java`

下面代码实现自动审核消息消费者。

```java
package io.github.atengk.audit.mq;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.dto.AutoAuditMessage;
import io.github.atengk.audit.service.ContentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 自动审核消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAuditMessageConsumer {

    private final ContentAuditService contentAuditService;
    private final AuditContentProperties auditContentProperties;

    /**
     * 消费自动审核消息
     *
     * @param body 消息体
     * @param message 原始消息
     * @param channel MQ 通道
     */
    @RabbitListener(queues = "#{auditContentProperties.autoAuditQueue}")
    public void consume(String body, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            AutoAuditMessage auditMessage = JSONUtil.toBean(body, AutoAuditMessage.class);
            contentAuditService.autoAudit(auditMessage.getContentId());

            channel.basicAck(deliveryTag, false);
            log.info("自动审核消息消费成功，contentId={}", auditMessage.getContentId());
        } catch (Exception e) {
            log.error("自动审核消息消费失败，body={}", body, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("自动审核消息 NACK 失败", nackException);
            }
        }
    }
}
```

上面使用了 SpEL 读取队列名，因此 `AuditContentProperties` 需要作为 Bean 存在。前面配置类已经用 `@Component` 注册。

### 人工审核接口

人工审核接口提供管理员通过和拒绝两种动作。实际项目中建议加上权限注解，例如 `@SaCheckRole("admin")` 或 `@SaCheckPermission("content:audit")`。

文件位置：`src/main/java/io/github/atengk/audit/controller/AdminContentAuditController.java`

下面代码提供人工审核和内容下架接口。

```java
package io.github.atengk.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.atengk.audit.common.R;
import io.github.atengk.audit.dto.ManualAuditRequest;
import io.github.atengk.audit.dto.OfflineRequest;
import io.github.atengk.audit.service.ContentAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端内容审核接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/content/audit")
public class AdminContentAuditController {

    private final ContentAuditService contentAuditService;

    /**
     * 人工审核
     *
     * @param request 审核请求
     * @return 处理结果
     */
    @PostMapping("/manual")
    @SaCheckPermission("content:audit:manual")
    public R<Void> manualAudit(@Valid @RequestBody ManualAuditRequest request) {
        contentAuditService.manualAudit(request);
        return R.ok(null);
    }

    /**
     * 下架内容
     *
     * @param request 下架请求
     * @return 处理结果
     */
    @PostMapping("/offline")
    @SaCheckPermission("content:audit:offline")
    public R<Void> offline(@Valid @RequestBody OfflineRequest request) {
        contentAuditService.offline(request);
        return R.ok(null);
    }
}
```

人工审核通过请求示例：

```bash
curl -X POST 'http://localhost:8080/admin/content/audit/manual' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentId": 1900000000000000001,
    "action": "MANUAL_PASS",
    "reason": "内容正常，允许发布"
  }'
```

人工审核拒绝请求示例：

```bash
curl -X POST 'http://localhost:8080/admin/content/audit/manual' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentId": 1900000000000000001,
    "action": "MANUAL_REJECT",
    "reason": "内容包含明显广告引流信息"
  }'
```

### 内容发布接口

本案例不单独开放“强制发布接口”。发布动作由自动审核通过或人工审核通过触发，避免绕过审核流程。用户端只需要提交内容，系统审核通过后自动发布。

文件位置：`src/main/java/io/github/atengk/audit/controller/UserContentController.java`

下面代码提供用户提交和重新提交接口。

```java
package io.github.atengk.audit.controller;

import io.github.atengk.audit.common.R;
import io.github.atengk.audit.dto.ContentSubmitRequest;
import io.github.atengk.audit.service.ContentAuditService;
import io.github.atengk.audit.vo.ContentSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端内容接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/content")
public class UserContentController {

    private final ContentAuditService contentAuditService;

    /**
     * 提交内容
     *
     * @param request 提交请求
     * @return 提交结果
     */
    @PostMapping("/submit")
    public R<ContentSubmitVO> submit(@Valid @RequestBody ContentSubmitRequest request) {
        return R.ok(contentAuditService.submit(request));
    }

    /**
     * 重新提交内容
     *
     * @param contentId 内容ID
     * @return 处理结果
     */
    @PostMapping("/{contentId}/resubmit")
    public R<Void> resubmit(@PathVariable Long contentId) {
        contentAuditService.resubmit(contentId);
        return R.ok(null);
    }
}
```

用户提交内容请求示例：

```bash
curl -X POST 'http://localhost:8080/user/content/submit' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentType": "ARTICLE",
    "title": "Spring Boot 内容审核流程实现",
    "contentBody": "这是一篇正常的技术文章内容",
    "coverUrl": "https://cdn.example.com/cover/audit.png",
    "categoryId": 1001
  }'
```

自动审核通过后的响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "contentId": 1900000000000000001,
    "auditStatus": "PENDING_REVIEW",
    "publishStatus": "UNPUBLISHED"
  }
}
```

提交接口只返回“已进入审核”，不会同步返回最终发布结果。最终结果由自动审核消费者异步更新。

### 内容下架接口

下架接口已经放在 `AdminContentAuditController` 中，对应路径如下：

```text
POST /admin/content/audit/offline
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/admin/content/audit/offline' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentId": 1900000000000000001,
    "reason": "用户投诉成立，内容暂时下架"
  }'
```

下架成功后状态变化如下：

```text
audit_status: APPROVED -> APPROVED
publish_status: PUBLISHED -> OFFLINE
offline_reason: 用户投诉成立，内容暂时下架
```

这里不修改 `audit_status`，因为下架不代表审核失败，只代表当前不再对前台展示。

### 重新提交接口

重新提交用于两类内容：

```text
REJECTED + UNPUBLISHED
APPROVED + OFFLINE
```

接口路径如下：

```text
POST /user/content/{contentId}/resubmit
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/user/content/1900000000000000001/resubmit' \
  -H 'Authorization: your-token'
```

重新提交成功后状态变化如下：

```text
audit_status: REJECTED / APPROVED -> PENDING_REVIEW
publish_status: UNPUBLISHED / OFFLINE -> UNPUBLISHED
```

随后系统会重新发送自动审核 MQ 消息。

## 当前核心代码的调用链

正常发布链路如下：

```text
POST /user/content/submit
-> ContentAuditService.submit()
-> 写入 audit_content
-> 写入 audit_record: SUBMIT
-> 发送 MQ: AutoAuditMessage
-> AutoAuditMessageConsumer.consume()
-> ContentAuditService.autoAudit()
-> SensitiveWordService.check()
-> 自动通过 / 转人工 / 自动拒绝
-> 写入 audit_record
```

人工复审链路如下：

```text
POST /admin/content/audit/manual
-> ContentAuditService.manualAudit()
-> Redisson 加锁
-> 状态机校验
-> MANUAL_PASS / MANUAL_REJECT
-> 更新 audit_content
-> 写入 audit_record
```

下架重提链路如下：

```text
POST /admin/content/audit/offline
-> ContentAuditService.offline()
-> PUBLISHED 改为 OFFLINE
-> 写入 audit_record: OFFLINE

POST /user/content/{contentId}/resubmit
-> ContentAuditService.resubmit()
-> 状态机校验
-> 改为 PENDING_REVIEW
-> 写入 audit_record: RESUBMIT
-> 重新发送自动审核 MQ
```

## 异步审核与超时处理

内容审核不建议全部放在提交接口同步执行。提交接口只负责落库和投递审核消息，真正的敏感词扫描、第三方审核调用、状态流转交给 RabbitMQ 消费端处理。这样可以避免用户提交接口被慢调用拖垮，也方便后续扩展图片审核、视频审核、人工复审提醒等能力。该设计对应原场景中的“自动审核和人工审核结合、审核超时提醒、重复提交处理”等核心点。

### RabbitMQ 审核消息发送

审核消息只传 `contentId`，不要把完整标题、正文、附件列表都塞进 MQ。消费端应重新查库，读取内容的最新状态，避免处理过期数据。

先补充业务配置字段。

文件位置：`src/main/resources/application.yml`

下面配置补充自动审核重试和死信队列参数。

```yaml
audit:
  content:
    # 重复提交限制时间，单位分钟
    duplicate-submit-minutes: 10
    # 人工审核超过多少分钟未处理触发提醒
    manual-timeout-minutes: 30

    # 敏感词 Redis 缓存 Key
    sensitive-word-cache-key: audit:sensitive:word:list

    # 自动审核主交换机、队列、路由键
    auto-audit-exchange: content.audit.exchange
    auto-audit-queue: content.audit.auto.queue
    auto-audit-routing-key: content.audit.auto

    # 自动审核失败后的死信交换机、队列、路由键
    auto-audit-dead-exchange: content.audit.dead.exchange
    auto-audit-dead-queue: content.audit.auto.dead.queue
    auto-audit-dead-routing-key: content.audit.auto.dead

    # 自动审核最大重试次数
    auto-audit-max-retry: 3
```

文件位置：`src/main/java/io/github/atengk/audit/config/AuditContentProperties.java`

下面代码是在前面配置类基础上补充的字段。

```java
package io.github.atengk.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 内容审核业务配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "audit.content")
public class AuditContentProperties {

    /**
     * 重复提交限制时间，单位分钟
     */
    private Integer duplicateSubmitMinutes = 10;

    /**
     * 人工审核超时时间，单位分钟
     */
    private Integer manualTimeoutMinutes = 30;

    /**
     * 敏感词缓存 Key
     */
    private String sensitiveWordCacheKey = "audit:sensitive:word:list";

    /**
     * 自动审核队列
     */
    private String autoAuditQueue = "content.audit.auto.queue";

    /**
     * 自动审核交换机
     */
    private String autoAuditExchange = "content.audit.exchange";

    /**
     * 自动审核路由键
     */
    private String autoAuditRoutingKey = "content.audit.auto";

    /**
     * 自动审核死信交换机
     */
    private String autoAuditDeadExchange = "content.audit.dead.exchange";

    /**
     * 自动审核死信队列
     */
    private String autoAuditDeadQueue = "content.audit.auto.dead.queue";

    /**
     * 自动审核死信路由键
     */
    private String autoAuditDeadRoutingKey = "content.audit.auto.dead";

    /**
     * 自动审核最大重试次数
     */
    private Integer autoAuditMaxRetry = 3;
}
```

文件位置：`src/main/java/io/github/atengk/audit/mq/AutoAuditMessageProducer.java`

下面代码封装自动审核消息发送逻辑，提交和重新提交时都调用它。

```java
package io.github.atengk.audit.mq;

import cn.hutool.json.JSONUtil;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.dto.AutoAuditMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 自动审核消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAuditMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AuditContentProperties auditContentProperties;

    /**
     * 发送自动审核消息
     *
     * @param contentId 内容ID
     */
    public void send(Long contentId) {
        AutoAuditMessage messageBody = new AutoAuditMessage(contentId);

        Message message = MessageBuilder
                .withBody(JSONUtil.toJsonStr(messageBody).getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader("x-retry-count", 0)
                .build();

        rabbitTemplate.convertAndSend(
                auditContentProperties.getAutoAuditExchange(),
                auditContentProperties.getAutoAuditRoutingKey(),
                message
        );

        log.info("发送自动审核消息成功，contentId={}", contentId);
    }
}
```

然后把 `ContentAuditServiceImpl` 中原来的 `sendAutoAuditMessage(content.getId())` 替换为：

```java
autoAuditMessageProducer.send(content.getId());
```

同时在 `ContentAuditServiceImpl` 中注入：

```java
private final AutoAuditMessageProducer autoAuditMessageProducer;
```

这样提交和重新提交都不直接感知 RabbitTemplate，后续换成 Kafka 或 RocketMQ 时只需要改 Producer。

### RabbitMQ 审核消息消费

这里将前面简单版 `RabbitMqConfig` 替换为带死信队列的版本。主队列负责正常自动审核，死信队列负责保存超过最大重试次数仍然失败的消息。

文件位置：`src/main/java/io/github/atengk/audit/config/RabbitMqConfig.java`

下面代码声明自动审核主队列、主交换机、死信交换机和死信队列。

```java
package io.github.atengk.audit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final AuditContentProperties auditContentProperties;

    /**
     * 内容审核主交换机
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange contentAuditExchange() {
        return ExchangeBuilder
                .directExchange(auditContentProperties.getAutoAuditExchange())
                .durable(true)
                .build();
    }

    /**
     * 内容审核死信交换机
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange contentAuditDeadExchange() {
        return ExchangeBuilder
                .directExchange(auditContentProperties.getAutoAuditDeadExchange())
                .durable(true)
                .build();
    }

    /**
     * 自动审核主队列
     *
     * @return Queue
     */
    @Bean
    public Queue autoAuditQueue() {
        return QueueBuilder
                .durable(auditContentProperties.getAutoAuditQueue())
                .withArguments(Map.of(
                        "x-dead-letter-exchange", auditContentProperties.getAutoAuditDeadExchange(),
                        "x-dead-letter-routing-key", auditContentProperties.getAutoAuditDeadRoutingKey()
                ))
                .build();
    }

    /**
     * 自动审核死信队列
     *
     * @return Queue
     */
    @Bean
    public Queue autoAuditDeadQueue() {
        return QueueBuilder
                .durable(auditContentProperties.getAutoAuditDeadQueue())
                .build();
    }

    /**
     * 自动审核主队列绑定
     *
     * @return Binding
     */
    @Bean
    public Binding autoAuditBinding() {
        log.info("初始化自动审核主队列绑定");
        return BindingBuilder
                .bind(autoAuditQueue())
                .to(contentAuditExchange())
                .with(auditContentProperties.getAutoAuditRoutingKey());
    }

    /**
     * 自动审核死信队列绑定
     *
     * @return Binding
     */
    @Bean
    public Binding autoAuditDeadBinding() {
        log.info("初始化自动审核死信队列绑定");
        return BindingBuilder
                .bind(autoAuditDeadQueue())
                .to(contentAuditDeadExchange())
                .with(auditContentProperties.getAutoAuditDeadRoutingKey());
    }
}
```

文件位置：`src/main/java/io/github/atengk/audit/mq/AutoAuditMessageConsumer.java`

下面代码实现自动审核消费、失败重试和超过重试次数后进入死信队列。

```java
package io.github.atengk.audit.mq;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.dto.AutoAuditMessage;
import io.github.atengk.audit.service.ContentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 自动审核消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAuditMessageConsumer {

    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final ContentAuditService contentAuditService;
    private final RabbitTemplate rabbitTemplate;
    private final AuditContentProperties auditContentProperties;

    /**
     * 消费自动审核消息
     *
     * @param body 消息体
     * @param message 原始消息
     * @param channel MQ 通道
     */
    @RabbitListener(queues = "#{auditContentProperties.autoAuditQueue}")
    public void consume(String body, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            AutoAuditMessage auditMessage = JSONUtil.toBean(body, AutoAuditMessage.class);
            contentAuditService.autoAudit(auditMessage.getContentId());

            channel.basicAck(deliveryTag, false);
            log.info("自动审核消息消费成功，contentId={}", auditMessage.getContentId());
        } catch (Exception e) {
            log.error("自动审核消息消费失败，body={}", body, e);
            handleFailure(body, message, channel, deliveryTag);
        }
    }

    /**
     * 处理消费失败
     *
     * @param body 消息体
     * @param message 原始消息
     * @param channel MQ 通道
     * @param deliveryTag 投递标识
     */
    private void handleFailure(String body, Message message, Channel channel, long deliveryTag) {
        try {
            Integer retryCount = Convert.toInt(
                    message.getMessageProperties().getHeader(RETRY_COUNT_HEADER),
                    0
            );

            if (retryCount < auditContentProperties.getAutoAuditMaxRetry()) {
                republishForRetry(body, retryCount + 1);
                channel.basicAck(deliveryTag, false);
                log.warn("自动审核消息进入重试，retryCount={}，body={}", retryCount + 1, body);
                return;
            }

            sendToDeadQueue(body, retryCount);
            channel.basicAck(deliveryTag, false);
            log.error("自动审核消息超过最大重试次数，已发送死信队列，retryCount={}，body={}", retryCount, body);
        } catch (Exception exception) {
            log.error("处理自动审核失败消息异常，body={}", body, exception);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("自动审核消息 NACK 失败", nackException);
            }
        }
    }

    /**
     * 重新投递消息用于重试
     *
     * @param body 消息体
     * @param retryCount 重试次数
     */
    private void republishForRetry(String body, Integer retryCount) {
        Message retryMessage = MessageBuilder
                .withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader(RETRY_COUNT_HEADER, retryCount)
                .build();

        rabbitTemplate.convertAndSend(
                auditContentProperties.getAutoAuditExchange(),
                auditContentProperties.getAutoAuditRoutingKey(),
                retryMessage
        );
    }

    /**
     * 发送到死信队列
     *
     * @param body 消息体
     * @param retryCount 重试次数
     */
    private void sendToDeadQueue(String body, Integer retryCount) {
        Message deadMessage = MessageBuilder
                .withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader(RETRY_COUNT_HEADER, retryCount)
                .build();

        rabbitTemplate.convertAndSend(
                auditContentProperties.getAutoAuditDeadExchange(),
                auditContentProperties.getAutoAuditDeadRoutingKey(),
                deadMessage
        );
    }
}
```

这段消费逻辑的关键点是：

| 场景                     | 处理方式                       |
| ------------------------ | ------------------------------ |
| 消费成功                 | `basicAck`                     |
| 消费失败但未超过重试次数 | 重新投递主队列，并确认当前消息 |
| 超过最大重试次数         | 投递死信队列，并确认当前消息   |
| 处理失败消息时再次异常   | `basicNack`，不重新入队        |

这种方式不会无限重试，也不会因为单条异常消息阻塞整个审核队列。

### XXL-JOB 审核超时扫描

人工复审可能因为审核员未处理而长时间停留在 `MANUAL_REVIEW` 状态。这里使用 XXL-JOB 定时扫描超时内容，并写入提醒记录。当前案例只做日志和审核记录留痕，如果项目已经有消息通知中心，可以在 `sendTimeoutNotice()` 方法中接入站内信、短信或企业微信。

先给审核动作枚举增加一个动作：

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditActionEnum.java`

在枚举中补充：

```java
MANUAL_TIMEOUT_REMIND("MANUAL_TIMEOUT_REMIND", "人工审核超时提醒");
```

文件位置：`src/main/java/io/github/atengk/audit/job/ContentAuditTimeoutJob.java`

下面代码实现人工审核超时扫描任务。

```java
package io.github.atengk.audit.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.audit.config.AuditContentProperties;
import io.github.atengk.audit.entity.AuditContent;
import io.github.atengk.audit.entity.AuditRecord;
import io.github.atengk.audit.enums.AuditActionEnum;
import io.github.atengk.audit.enums.AuditSourceEnum;
import io.github.atengk.audit.enums.AuditStatusEnum;
import io.github.atengk.audit.enums.PublishStatusEnum;
import io.github.atengk.audit.mapper.AuditContentMapper;
import io.github.atengk.audit.mapper.AuditRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容审核超时扫描任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAuditTimeoutJob {

    private final AuditContentMapper auditContentMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final AuditContentProperties auditContentProperties;

    /**
     * 扫描人工审核超时内容
     */
    @XxlJob("contentAuditTimeoutJob")
    public void scanManualReviewTimeout() {
        LocalDateTime timeoutTime = DateUtil
                .offsetMinute(DateUtil.date(), -auditContentProperties.getManualTimeoutMinutes())
                .toLocalDateTime();

        List<AuditContent> timeoutContents = auditContentMapper.selectList(
                new LambdaQueryWrapper<AuditContent>()
                        .eq(AuditContent::getAuditStatus, AuditStatusEnum.MANUAL_REVIEW.getCode())
                        .eq(AuditContent::getPublishStatus, PublishStatusEnum.UNPUBLISHED.getCode())
                        .le(AuditContent::getSubmitTime, timeoutTime)
                        .orderByAsc(AuditContent::getSubmitTime)
                        .last("LIMIT 200")
        );

        if (timeoutContents.isEmpty()) {
            log.info("未发现人工审核超时内容");
            return;
        }

        for (AuditContent content : timeoutContents) {
            saveTimeoutRecord(content);
            sendTimeoutNotice(content);
        }

        log.warn("人工审核超时扫描完成，timeoutCount={}", timeoutContents.size());
    }

    /**
     * 保存超时提醒记录
     *
     * @param content 内容
     */
    private void saveTimeoutRecord(AuditContent content) {
        AuditRecord record = new AuditRecord();
        record.setContentId(content.getId());
        record.setContentType(content.getContentType());
        record.setAuditAction(AuditActionEnum.MANUAL_TIMEOUT_REMIND.getCode());
        record.setFromAuditStatus(content.getAuditStatus());
        record.setToAuditStatus(content.getAuditStatus());
        record.setFromPublishStatus(content.getPublishStatus());
        record.setToPublishStatus(content.getPublishStatus());
        record.setAuditorName("XXL-JOB");
        record.setAuditReason("人工审核超过 " + auditContentProperties.getManualTimeoutMinutes() + " 分钟未处理");
        record.setAuditSource(AuditSourceEnum.JOB.getCode());
        record.setCreateTime(LocalDateTime.now());

        auditRecordMapper.insert(record);
    }

    /**
     * 发送超时提醒
     *
     * @param content 内容
     */
    private void sendTimeoutNotice(AuditContent content) {
        // 实际项目可在这里接入站内信、WebSocket、企业微信、钉钉或短信。
        log.warn("内容人工审核超时，请及时处理，contentId={}，title={}", content.getId(), content.getTitle());
    }
}
```

XXL-JOB 管理端中新增任务：

```text
执行器：content-audit-demo
JobHandler：contentAuditTimeoutJob
Cron：0 */5 * * * ?
说明：每 5 分钟扫描一次人工审核超时内容
```

这类任务不要一次扫描全表，建议每次限制数量，例如 `LIMIT 200`，避免待审核积压时压垮数据库。

### 审核异常重试处理

审核异常主要来自三类问题：数据库短暂异常、第三方内容安全 API 超时、敏感词服务或缓存异常。处理原则是“可恢复异常重试，不可恢复异常落死信，状态流转必须幂等”。

建议策略如下：

| 异常类型        | 示例                            | 处理方式               |
| --------------- | ------------------------------- | ---------------------- |
| 数据库短暂异常  | 连接抖动、锁等待超时            | MQ 重试                |
| 第三方 API 超时 | 图片审核接口超时                | MQ 重试                |
| 消息体错误      | `contentId` 为空、JSON 格式错误 | 直接进入死信           |
| 内容不存在      | 用户删除或非法 ID               | 直接 ACK，记录日志     |
| 状态已变化      | 已被人工审核或重新提交          | 直接 ACK，不再处理     |
| 重试超过上限    | 连续失败 3 次                   | 进入死信队列，人工补偿 |

死信队列中的消息可以通过 RabbitMQ 管理后台查看，也可以后续实现一个“死信补偿接口”。本案例先给出查询队列的命令示例。

```bash
# 查看 RabbitMQ 容器中的队列列表
rabbitmqctl list_queues name messages_ready messages_unacknowledged

# 查看自动审核死信队列消息数量
rabbitmqctl list_queues name messages_ready | grep content.audit.auto.dead.queue
```

`messages_ready` 表示待消费消息数量，`messages_unacknowledged` 表示已经投递但尚未确认的消息数量。如果死信队列持续增长，通常说明自动审核逻辑存在稳定异常，需要优先排查消费者日志。

## 接口设计

接口按角色拆成三类：用户端负责提交和重新提交，管理端负责人工审核和下架，发布管理端负责查询已发布内容。发布动作本身不直接开放给用户调用，而是由自动审核通过或人工审核通过触发。

### 用户端内容接口

用户端接口面向普通用户，主要用于提交内容、重新提交内容、查看自己内容的审核状态。

| 接口             | 方法 | 路径                                 | 说明                   |
| ---------------- | ---- | ------------------------------------ | ---------------------- |
| 提交内容         | POST | `/user/content/submit`               | 创建内容并进入审核     |
| 重新提交         | POST | `/user/content/{contentId}/resubmit` | 被拒绝或下架后重新提交 |
| 查询我的内容详情 | GET  | `/user/content/{contentId}`          | 查看自己的内容状态     |
| 查询我的内容列表 | GET  | `/user/content/my`                   | 分页查看自己的内容     |

前面已经给出提交和重新提交接口，这里补充详情和列表查询。

文件位置：`src/main/java/io/github/atengk/audit/vo/ContentDetailVO.java`

下面代码定义内容详情响应对象。

```java
package io.github.atengk.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容详情响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContentDetailVO {

    private Long id;

    private String contentType;

    private String title;

    private String contentBody;

    private String coverUrl;

    private String auditStatus;

    private String publishStatus;

    private String rejectReason;

    private String offlineReason;

    private LocalDateTime submitTime;

    private LocalDateTime publishTime;

    private LocalDateTime offlineTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/controller/UserContentController.java`

下面代码是在已有用户端 Controller 基础上补充查询接口。

```java
package io.github.atengk.audit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.common.R;
import io.github.atengk.audit.dto.ContentSubmitRequest;
import io.github.atengk.audit.service.ContentAuditService;
import io.github.atengk.audit.vo.ContentDetailVO;
import io.github.atengk.audit.vo.ContentSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端内容接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/content")
public class UserContentController {

    private final ContentAuditService contentAuditService;

    /**
     * 提交内容
     *
     * @param request 提交请求
     * @return 提交结果
     */
    @PostMapping("/submit")
    public R<ContentSubmitVO> submit(@Valid @RequestBody ContentSubmitRequest request) {
        return R.ok(contentAuditService.submit(request));
    }

    /**
     * 重新提交内容
     *
     * @param contentId 内容ID
     * @return 处理结果
     */
    @PostMapping("/{contentId}/resubmit")
    public R<Void> resubmit(@PathVariable Long contentId) {
        contentAuditService.resubmit(contentId);
        return R.ok(null);
    }

    /**
     * 查询我的内容详情
     *
     * @param contentId 内容ID
     * @return 内容详情
     */
    @GetMapping("/{contentId}")
    public R<ContentDetailVO> detail(@PathVariable Long contentId) {
        return R.ok(contentAuditService.getMyContentDetail(contentId));
    }

    /**
     * 分页查询我的内容
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 内容分页
     */
    @GetMapping("/my")
    public R<IPage<ContentDetailVO>> myPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return R.ok(contentAuditService.pageMyContent(pageNum, pageSize));
    }
}
```

对应 Service 接口需要补充：

```java
/**
 * 查询我的内容详情
 *
 * @param contentId 内容ID
 * @return 内容详情
 */
ContentDetailVO getMyContentDetail(Long contentId);

/**
 * 分页查询我的内容
 *
 * @param pageNum 页码
 * @param pageSize 每页数量
 * @return 内容分页
 */
IPage<ContentDetailVO> pageMyContent(Long pageNum, Long pageSize);
```

用户端调用示例：

```bash
# 提交内容
curl -X POST 'http://localhost:8080/user/content/submit' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentType": "ARTICLE",
    "title": "Spring Boot 内容审核实战",
    "contentBody": "这是一篇正常技术文章",
    "coverUrl": "https://cdn.example.com/audit.png",
    "categoryId": 1001
  }'

# 查询我的内容详情
curl -X GET 'http://localhost:8080/user/content/1900000000000000001' \
  -H 'Authorization: your-token'

# 重新提交
curl -X POST 'http://localhost:8080/user/content/1900000000000000001/resubmit' \
  -H 'Authorization: your-token'
```

### 管理端审核接口

管理端接口面向审核员或运营管理员，主要负责查询待审核内容、人工通过、人工拒绝和下架内容。

| 接口             | 方法 | 路径                                       | 说明                      |
| ---------------- | ---- | ------------------------------------------ | ------------------------- |
| 查询人工复审列表 | GET  | `/admin/content/audit/manual/page`         | 查询 `MANUAL_REVIEW` 内容 |
| 人工审核         | POST | `/admin/content/audit/manual`              | 通过或拒绝                |
| 下架内容         | POST | `/admin/content/audit/offline`             | 下架已发布内容            |
| 查询审核记录     | GET  | `/admin/content/audit/{contentId}/records` | 查看审核轨迹              |

文件位置：`src/main/java/io/github/atengk/audit/controller/AdminContentAuditController.java`

下面代码是在已有管理端 Controller 基础上补充列表和审核记录查询接口。

```java
package io.github.atengk.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.common.R;
import io.github.atengk.audit.dto.ManualAuditRequest;
import io.github.atengk.audit.dto.OfflineRequest;
import io.github.atengk.audit.entity.AuditRecord;
import io.github.atengk.audit.service.ContentAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端内容审核接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/content/audit")
public class AdminContentAuditController {

    private final ContentAuditService contentAuditService;

    /**
     * 查询人工复审分页
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 人工复审分页
     */
    @GetMapping("/manual/page")
    @SaCheckPermission("content:audit:query")
    public R<IPage<?>> manualReviewPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return R.ok(contentAuditService.pageManualReview(pageNum, pageSize));
    }

    /**
     * 人工审核
     *
     * @param request 审核请求
     * @return 处理结果
     */
    @PostMapping("/manual")
    @SaCheckPermission("content:audit:manual")
    public R<Void> manualAudit(@Valid @RequestBody ManualAuditRequest request) {
        contentAuditService.manualAudit(request);
        return R.ok(null);
    }

    /**
     * 下架内容
     *
     * @param request 下架请求
     * @return 处理结果
     */
    @PostMapping("/offline")
    @SaCheckPermission("content:audit:offline")
    public R<Void> offline(@Valid @RequestBody OfflineRequest request) {
        contentAuditService.offline(request);
        return R.ok(null);
    }

    /**
     * 查询审核记录
     *
     * @param contentId 内容ID
     * @return 审核记录列表
     */
    @GetMapping("/{contentId}/records")
    @SaCheckPermission("content:audit:records")
    public R<List<AuditRecord>> records(@PathVariable Long contentId) {
        return R.ok(contentAuditService.listAuditRecords(contentId));
    }
}
```

对应 Service 接口需要补充：

```java
/**
 * 查询人工复审分页
 *
 * @param pageNum 页码
 * @param pageSize 每页数量
 * @return 分页结果
 */
IPage<?> pageManualReview(Long pageNum, Long pageSize);

/**
 * 查询审核记录
 *
 * @param contentId 内容ID
 * @return 审核记录列表
 */
List<AuditRecord> listAuditRecords(Long contentId);
```

管理端调用示例：

```bash
# 查询人工复审列表
curl -X GET 'http://localhost:8080/admin/content/audit/manual/page?pageNum=1&pageSize=10' \
  -H 'Authorization: your-token'

# 人工审核通过
curl -X POST 'http://localhost:8080/admin/content/audit/manual' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentId": 1900000000000000001,
    "action": "MANUAL_PASS",
    "reason": "内容正常，允许发布"
  }'

# 人工审核拒绝
curl -X POST 'http://localhost:8080/admin/content/audit/manual' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: your-token' \
  -d '{
    "contentId": 1900000000000000001,
    "action": "MANUAL_REJECT",
    "reason": "内容包含广告引流"
  }'

# 查询审核记录
curl -X GET 'http://localhost:8080/admin/content/audit/1900000000000000001/records' \
  -H 'Authorization: your-token'
```

### 发布管理接口

发布管理接口面向前台或运营后台，用于查询已发布内容。这里不提供“直接发布”接口，因为发布必须由审核通过触发，避免绕过审核链路。

| 接口                 | 方法 | 路径                            | 说明                   |
| -------------------- | ---- | ------------------------------- | ---------------------- |
| 查询已发布内容列表   | GET  | `/open/content/page`            | 前台展示内容           |
| 查询已发布内容详情   | GET  | `/open/content/{contentId}`     | 前台查看详情           |
| 管理端查询已发布内容 | GET  | `/admin/content/published/page` | 运营后台查看已发布内容 |

文件位置：`src/main/java/io/github/atengk/audit/controller/OpenContentController.java`

下面代码提供前台只读内容接口，只返回审核通过并已发布的内容。

```java
package io.github.atengk.audit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.common.R;
import io.github.atengk.audit.service.ContentPublishService;
import io.github.atengk.audit.vo.ContentDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 前台已发布内容接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/open/content")
public class OpenContentController {

    private final ContentPublishService contentPublishService;

    /**
     * 查询已发布内容分页
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 已发布内容分页
     */
    @GetMapping("/page")
    public R<IPage<ContentDetailVO>> publishedPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return R.ok(contentPublishService.pagePublished(pageNum, pageSize));
    }

    /**
     * 查询已发布内容详情
     *
     * @param contentId 内容ID
     * @return 内容详情
     */
    @GetMapping("/{contentId}")
    public R<ContentDetailVO> publishedDetail(@PathVariable Long contentId) {
        return R.ok(contentPublishService.getPublishedDetail(contentId));
    }
}
```

文件位置：`src/main/java/io/github/atengk/audit/service/ContentPublishService.java`

下面代码定义已发布内容查询服务。

```java
package io.github.atengk.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.vo.ContentDetailVO;

/**
 * 内容发布查询服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContentPublishService {

    /**
     * 查询已发布内容分页
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 已发布内容分页
     */
    IPage<ContentDetailVO> pagePublished(Long pageNum, Long pageSize);

    /**
     * 查询已发布内容详情
     *
     * @param contentId 内容ID
     * @return 内容详情
     */
    ContentDetailVO getPublishedDetail(Long contentId);
}
```

文件位置：`src/main/java/io/github/atengk/audit/service/impl/ContentPublishServiceImpl.java`

下面代码只查询 `APPROVED + PUBLISHED` 内容，避免待审核、已拒绝、已下架内容被前台访问。

```java
package io.github.atengk.audit.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.audit.entity.AuditContent;
import io.github.atengk.audit.enums.AuditStatusEnum;
import io.github.atengk.audit.enums.PublishStatusEnum;
import io.github.atengk.audit.mapper.AuditContentMapper;
import io.github.atengk.audit.service.ContentPublishService;
import io.github.atengk.audit.vo.ContentDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 内容发布查询服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPublishServiceImpl implements ContentPublishService {

    private final AuditContentMapper auditContentMapper;

    /**
     * 查询已发布内容分页
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 已发布内容分页
     */
    @Override
    public IPage<ContentDetailVO> pagePublished(Long pageNum, Long pageSize) {
        Page<AuditContent> page = Page.of(pageNum, pageSize);

        IPage<AuditContent> contentPage = auditContentMapper.selectPage(
                page,
                new LambdaQueryWrapper<AuditContent>()
                        .eq(AuditContent::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                        .eq(AuditContent::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode())
                        .orderByDesc(AuditContent::getPublishTime)
        );

        return contentPage.convert(item -> BeanUtil.copyProperties(item, ContentDetailVO.class));
    }

    /**
     * 查询已发布内容详情
     *
     * @param contentId 内容ID
     * @return 内容详情
     */
    @Override
    public ContentDetailVO getPublishedDetail(Long contentId) {
        AuditContent content = auditContentMapper.selectOne(
                new LambdaQueryWrapper<AuditContent>()
                        .eq(AuditContent::getId, contentId)
                        .eq(AuditContent::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                        .eq(AuditContent::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode())
        );

        if (content == null) {
            throw new IllegalArgumentException("内容不存在或未发布");
        }

        log.info("查询已发布内容详情，contentId={}", contentId);
        return BeanUtil.copyProperties(content, ContentDetailVO.class);
    }
}
```

前台调用示例：

```bash
# 查询已发布内容列表
curl -X GET 'http://localhost:8080/open/content/page?pageNum=1&pageSize=10'

# 查询已发布内容详情
curl -X GET 'http://localhost:8080/open/content/1900000000000000001'
```

本阶段完成后，核心链路已经具备：

```text
用户提交
-> RabbitMQ 异步自动审核
-> 敏感词 DFA 过滤
-> 自动通过 / 自动拒绝 / 转人工
-> 管理端人工审核
-> 审核通过后前台可见
-> 管理端下架
-> 用户重新提交
-> XXL-JOB 扫描人工审核超时
-> 失败消息重试与死信兜底
```

## 接口测试示例

本节用 `curl + SQL` 验证内容审核主流程，覆盖提交、自动审核、人工复审、发布、下架和重新提交。测试目标是验证第 28 个业务场景中的核心链路：内容提交、系统自动审核、人工复审、审核失败退回、审核通过发布、下架和重新提交。

测试前默认服务已启动，并准备两个 Token：

```bash
# 用户端 Token，用于提交内容和重新提交
USER_TOKEN="your-user-token"

# 管理端 Token，用于人工审核和下架
ADMIN_TOKEN="your-admin-token"

# 服务地址
BASE_URL="http://localhost:8080"
```

如果你当前还没有接入真实登录，可以先在本地开发环境临时放开 Sa-Token 校验，或者使用已有系统的登录接口获取 Token。

### 提交内容测试

提交内容主要验证两点：第一，接口能创建内容并进入 `PENDING_REVIEW`；第二，重复提交相同内容会被 Redis 拦截。

下面命令提交一篇正常内容。

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "Spring Boot 内容审核流程实现",
    "contentBody": "这是一篇正常的技术文章，主要介绍内容审核、自动审核和人工复审流程。",
    "coverUrl": "https://cdn.example.com/cover/content-audit.png",
    "categoryId": 1001
  }'
```

预期响应如下，接口只表示“提交成功并进入审核”，不会同步返回最终审核结果。

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "contentId": 1900000000000000001,
    "auditStatus": "PENDING_REVIEW",
    "publishStatus": "UNPUBLISHED"
  }
}
```

提交后查询数据库，确认内容主表和审核记录都已写入。

```sql
SELECT
    id,
    user_id,
    title,
    audit_status,
    publish_status,
    content_hash,
    submit_time
FROM audit_content
WHERE id = 1900000000000000001;

SELECT
    content_id,
    audit_action,
    from_audit_status,
    to_audit_status,
    from_publish_status,
    to_publish_status,
    audit_source,
    create_time
FROM audit_record
WHERE content_id = 1900000000000000001
ORDER BY create_time ASC;
```

预期内容主表状态：

```text
audit_status = PENDING_REVIEW
publish_status = UNPUBLISHED
```

预期审核记录中至少存在：

```text
audit_action = SUBMIT
audit_source = USER
```

重复提交同一份内容，可以再次执行上面的 `curl`。预期返回失败：

```json
{
  "code": 500,
  "message": "请勿重复提交相同内容",
  "data": null
}
```

可以用 Redis 查看重复提交 Key 是否存在。

```bash
redis-cli keys "content:submit:*"
```

### 自动审核测试

自动审核由 RabbitMQ 消费端异步执行。测试时可以分别构造三种内容：正常内容自动通过，普通风险词进入人工复审，严重违规词自动拒绝。

先确认敏感词测试数据存在。

```sql
SELECT id, word, word_level, handle_type, enabled
FROM sensitive_word
WHERE word IN ('测试违规词', '测试风险词', '广告引流');
```

如果没有测试数据，执行下面 SQL 初始化。

```sql
INSERT INTO sensitive_word
(id, word, word_level, handle_type, enabled, remark)
VALUES
(1001, '测试违规词', 'SEVERE', 'REJECT', 1, '严重违规，自动拒绝'),
(1002, '测试风险词', 'NORMAL', 'MANUAL', 1, '普通风险，进入人工复审'),
(1003, '广告引流', 'NORMAL', 'MANUAL', 1, '疑似营销广告')
ON DUPLICATE KEY UPDATE
word_level = VALUES(word_level),
handle_type = VALUES(handle_type),
enabled = VALUES(enabled),
remark = VALUES(remark);
```

如果应用已经启动，需要刷新敏感词缓存。当前文档前面只提供了 `SensitiveWordService.refreshCache()`，实际项目可以临时加一个管理端刷新接口，也可以重启服务触发 `@PostConstruct` 重新加载。

测试自动通过内容。

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "Java 后端状态机设计",
    "contentBody": "这是一篇正常内容，不包含任何敏感词。",
    "coverUrl": "https://cdn.example.com/cover/state-machine.png",
    "categoryId": 1001
  }'
```

等待 MQ 消费完成后查询：

```sql
SELECT id, title, audit_status, publish_status, publish_time
FROM audit_content
WHERE title = 'Java 后端状态机设计'
ORDER BY create_time DESC
LIMIT 1;

SELECT audit_action, audit_reason, risk_words, audit_source, create_time
FROM audit_record
WHERE content_id = (
    SELECT id
    FROM audit_content
    WHERE title = 'Java 后端状态机设计'
    ORDER BY create_time DESC
    LIMIT 1
)
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = APPROVED
publish_status = PUBLISHED
audit_record 包含 SUBMIT、AUTO_PASS
```

测试普通风险词进入人工复审。

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "平台运营活动说明",
    "contentBody": "这是一段包含测试风险词的内容，需要进入人工复审。",
    "coverUrl": "https://cdn.example.com/cover/risk.png",
    "categoryId": 1001
  }'
```

查询结果：

```sql
SELECT id, title, audit_status, publish_status
FROM audit_content
WHERE title = '平台运营活动说明'
ORDER BY create_time DESC
LIMIT 1;

SELECT audit_action, audit_reason, risk_words
FROM audit_record
WHERE content_id = (
    SELECT id
    FROM audit_content
    WHERE title = '平台运营活动说明'
    ORDER BY create_time DESC
    LIMIT 1
)
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = MANUAL_REVIEW
publish_status = UNPUBLISHED
audit_record 包含 AUTO_TO_MANUAL
risk_words 包含 测试风险词
```

测试严重敏感词自动拒绝。

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "违规内容测试",
    "contentBody": "这是一段包含测试违规词的内容，应该被系统自动拒绝。",
    "coverUrl": "https://cdn.example.com/cover/reject.png",
    "categoryId": 1001
  }'
```

查询结果：

```sql
SELECT id, title, audit_status, publish_status, reject_reason
FROM audit_content
WHERE title = '违规内容测试'
ORDER BY create_time DESC
LIMIT 1;

SELECT audit_action, audit_reason, risk_words
FROM audit_record
WHERE content_id = (
    SELECT id
    FROM audit_content
    WHERE title = '违规内容测试'
    ORDER BY create_time DESC
    LIMIT 1
)
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = REJECTED
publish_status = UNPUBLISHED
reject_reason = 命中严重敏感词
audit_record 包含 AUTO_REJECT
risk_words 包含 测试违规词
```

### 人工审核测试

人工审核只处理 `MANUAL_REVIEW + UNPUBLISHED` 的内容。先查询需要人工复审的数据。

```bash
curl -X GET "${BASE_URL}/admin/content/audit/manual/page?pageNum=1&pageSize=10" \
  -H "Authorization: ${ADMIN_TOKEN}"
```

也可以直接 SQL 查询：

```sql
SELECT id, title, audit_status, publish_status, submit_time
FROM audit_content
WHERE audit_status = 'MANUAL_REVIEW'
  AND publish_status = 'UNPUBLISHED'
ORDER BY submit_time ASC
LIMIT 10;
```

人工审核通过。

```bash
curl -X POST "${BASE_URL}/admin/content/audit/manual" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${ADMIN_TOKEN}" \
  -d '{
    "contentId": 1900000000000000002,
    "action": "MANUAL_PASS",
    "reason": "人工确认内容可发布"
  }'
```

查询审核结果：

```sql
SELECT id, title, audit_status, publish_status, publish_time
FROM audit_content
WHERE id = 1900000000000000002;

SELECT audit_action, audit_reason, audit_source, auditor_id, create_time
FROM audit_record
WHERE content_id = 1900000000000000002
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = APPROVED
publish_status = PUBLISHED
audit_record 包含 MANUAL_PASS
```

人工审核拒绝。

```bash
curl -X POST "${BASE_URL}/admin/content/audit/manual" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${ADMIN_TOKEN}" \
  -d '{
    "contentId": 1900000000000000003,
    "action": "MANUAL_REJECT",
    "reason": "内容存在广告引流，不允许发布"
  }'
```

查询审核结果：

```sql
SELECT id, title, audit_status, publish_status, reject_reason
FROM audit_content
WHERE id = 1900000000000000003;

SELECT audit_action, audit_reason, audit_source, auditor_id, create_time
FROM audit_record
WHERE content_id = 1900000000000000003
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = REJECTED
publish_status = UNPUBLISHED
reject_reason = 内容存在广告引流，不允许发布
audit_record 包含 MANUAL_REJECT
```

如果对已经发布、已拒绝、待自动审核的内容执行人工审核，预期会被状态机拦截：

```json
{
  "code": 500,
  "message": "当前状态不允许执行该审核动作：MANUAL_PASS",
  "data": null
}
```

### 发布与下架测试

发布不提供单独接口，而是由自动审核通过或人工审核通过触发。前台只能查询 `APPROVED + PUBLISHED` 的内容。

查询已发布内容列表。

```bash
curl -X GET "${BASE_URL}/open/content/page?pageNum=1&pageSize=10"
```

查询已发布内容详情。

```bash
curl -X GET "${BASE_URL}/open/content/1900000000000000002"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1900000000000000002,
    "contentType": "ARTICLE",
    "title": "平台运营活动说明",
    "auditStatus": "APPROVED",
    "publishStatus": "PUBLISHED"
  }
}
```

管理员下架内容。

```bash
curl -X POST "${BASE_URL}/admin/content/audit/offline" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${ADMIN_TOKEN}" \
  -d '{
    "contentId": 1900000000000000002,
    "reason": "用户投诉成立，运营下架处理"
  }'
```

查询下架结果：

```sql
SELECT id, title, audit_status, publish_status, offline_reason, offline_time
FROM audit_content
WHERE id = 1900000000000000002;

SELECT audit_action, audit_reason, audit_source, create_time
FROM audit_record
WHERE content_id = 1900000000000000002
ORDER BY create_time ASC;
```

预期结果：

```text
audit_status = APPROVED
publish_status = OFFLINE
offline_reason = 用户投诉成立，运营下架处理
audit_record 包含 OFFLINE
```

下架后再次访问前台详情接口：

```bash
curl -X GET "${BASE_URL}/open/content/1900000000000000002"
```

预期结果：

```json
{
  "code": 500,
  "message": "内容不存在或未发布",
  "data": null
}
```

## 核心流程验证

本节从业务链路角度验证状态机是否正确。重点看状态是否按预期变化、审核记录是否完整、前台是否只展示已发布内容。

### 正常发布流程验证

正常发布流程适用于未命中敏感词的内容。

流程如下：

```text
提交正常内容
-> PENDING_REVIEW + UNPUBLISHED
-> MQ 自动审核
-> APPROVED + PUBLISHED
-> 前台可查询
```

执行提交：

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "正常发布流程验证",
    "contentBody": "这是一篇用于验证正常发布流程的内容。",
    "coverUrl": "https://cdn.example.com/cover/normal.png",
    "categoryId": 1001
  }'
```

验证 SQL：

```sql
SELECT id, audit_status, publish_status, publish_time
FROM audit_content
WHERE title = '正常发布流程验证'
ORDER BY create_time DESC
LIMIT 1;

SELECT audit_action, from_audit_status, to_audit_status, from_publish_status, to_publish_status
FROM audit_record
WHERE content_id = (
    SELECT id
    FROM audit_content
    WHERE title = '正常发布流程验证'
    ORDER BY create_time DESC
    LIMIT 1
)
ORDER BY create_time ASC;
```

预期审核记录：

```text
SUBMIT
AUTO_PASS
```

预期最终状态：

```text
APPROVED + PUBLISHED
```

前台访问验证：

```bash
curl -X GET "${BASE_URL}/open/content/page?pageNum=1&pageSize=10"
```

列表中应该能看到标题为 `正常发布流程验证` 的内容。

### 敏感词拦截流程验证

敏感词拦截流程用于验证严重违规内容是否能被自动拒绝，并且不会进入前台发布列表。

流程如下：

```text
提交严重敏感词内容
-> PENDING_REVIEW + UNPUBLISHED
-> MQ 自动审核
-> REJECTED + UNPUBLISHED
-> 前台不可查询
```

执行提交：

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "敏感词拦截流程验证",
    "contentBody": "这是一篇包含测试违规词的内容。",
    "coverUrl": "https://cdn.example.com/cover/reject-flow.png",
    "categoryId": 1001
  }'
```

验证 SQL：

```sql
SELECT id, audit_status, publish_status, reject_reason
FROM audit_content
WHERE title = '敏感词拦截流程验证'
ORDER BY create_time DESC
LIMIT 1;

SELECT audit_action, audit_reason, risk_words
FROM audit_record
WHERE content_id = (
    SELECT id
    FROM audit_content
    WHERE title = '敏感词拦截流程验证'
    ORDER BY create_time DESC
    LIMIT 1
)
ORDER BY create_time ASC;
```

预期审核记录：

```text
SUBMIT
AUTO_REJECT
```

预期最终状态：

```text
REJECTED + UNPUBLISHED
```

预期风险词：

```text
测试违规词
```

前台详情访问该内容时，应返回未发布或不存在。

### 人工复审流程验证

人工复审流程用于验证普通风险词不会直接发布，而是进入管理端待审核列表，由管理员人工决定通过或拒绝。

流程如下：

```text
提交普通风险词内容
-> PENDING_REVIEW + UNPUBLISHED
-> MQ 自动审核
-> MANUAL_REVIEW + UNPUBLISHED
-> 管理员人工审核通过
-> APPROVED + PUBLISHED
```

执行提交：

```bash
curl -X POST "${BASE_URL}/user/content/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${USER_TOKEN}" \
  -d '{
    "contentType": "ARTICLE",
    "title": "人工复审流程验证",
    "contentBody": "这是一篇包含测试风险词的内容，需要人工复审。",
    "coverUrl": "https://cdn.example.com/cover/manual-flow.png",
    "categoryId": 1001
  }'
```

查询人工复审内容 ID：

```sql
SELECT id, title, audit_status, publish_status
FROM audit_content
WHERE title = '人工复审流程验证'
ORDER BY create_time DESC
LIMIT 1;
```

预期当前状态：

```text
MANUAL_REVIEW + UNPUBLISHED
```

人工审核通过：

```bash
curl -X POST "${BASE_URL}/admin/content/audit/manual" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${ADMIN_TOKEN}" \
  -d '{
    "contentId": 1900000000000000004,
    "action": "MANUAL_PASS",
    "reason": "风险词上下文正常，允许发布"
  }'
```

验证 SQL：

```sql
SELECT id, audit_status, publish_status, publish_time
FROM audit_content
WHERE id = 1900000000000000004;

SELECT audit_action, audit_reason, risk_words
FROM audit_record
WHERE content_id = 1900000000000000004
ORDER BY create_time ASC;
```

预期审核记录：

```text
SUBMIT
AUTO_TO_MANUAL
MANUAL_PASS
```

预期最终状态：

```text
APPROVED + PUBLISHED
```

如果要验证人工拒绝，把 `action` 改为 `MANUAL_REJECT`：

```bash
curl -X POST "${BASE_URL}/admin/content/audit/manual" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${ADMIN_TOKEN}" \
  -d '{
    "contentId": 1900000000000000004,
    "action": "MANUAL_REJECT",
    "reason": "人工确认存在广告引流"
  }'
```

预期最终状态：

```text
REJECTED + UNPUBLISHED
```

### 退回后重新提交验证

退回后重新提交用于验证被拒绝内容可以修改后再次进入审核流程。当前代码中的重新提交接口只改变状态并重新触发自动审核，如果要支持修改标题和正文，需要额外提供“编辑内容接口”。在本案例中，可以先通过 SQL 模拟用户修改内容，再调用重新提交接口。

流程如下：

```text
内容被拒绝
-> REJECTED + UNPUBLISHED
-> 用户修改内容
-> 调用重新提交
-> PENDING_REVIEW + UNPUBLISHED
-> MQ 自动审核
-> APPROVED + PUBLISHED 或 MANUAL_REVIEW + UNPUBLISHED
```

先找到一条被拒绝内容：

```sql
SELECT id, title, content_body, audit_status, publish_status, reject_reason
FROM audit_content
WHERE audit_status = 'REJECTED'
  AND publish_status = 'UNPUBLISHED'
ORDER BY update_time DESC
LIMIT 1;
```

模拟用户修改内容，把严重敏感词移除：

```sql
UPDATE audit_content
SET content_body = '这是用户修改后的正常内容，已经移除违规描述。',
    title = '退回后重新提交验证',
    update_time = NOW()
WHERE id = 1900000000000000005
  AND audit_status = 'REJECTED'
  AND publish_status = 'UNPUBLISHED';
```

调用重新提交接口：

```bash
curl -X POST "${BASE_URL}/user/content/1900000000000000005/resubmit" \
  -H "Authorization: ${USER_TOKEN}"
```

验证重新提交后的状态：

```sql
SELECT id, title, audit_status, publish_status, reject_reason, submit_time
FROM audit_content
WHERE id = 1900000000000000005;

SELECT audit_action, from_audit_status, to_audit_status, from_publish_status, to_publish_status, audit_reason
FROM audit_record
WHERE content_id = 1900000000000000005
ORDER BY create_time ASC;
```

预期审核记录中新增：

```text
RESUBMIT
AUTO_PASS
```

预期最终状态：

```text
APPROVED + PUBLISHED
```

如果修改后的内容仍然包含 `测试风险词`，则预期最终状态为：

```text
MANUAL_REVIEW + UNPUBLISHED
```

如果修改后的内容仍然包含 `测试违规词`，则预期最终状态为：

```text
REJECTED + UNPUBLISHED
```

为了完整验证下架后的重新提交，也可以对已下架内容执行同样流程：

```sql
SELECT id, title, audit_status, publish_status, offline_reason
FROM audit_content
WHERE audit_status = 'APPROVED'
  AND publish_status = 'OFFLINE'
ORDER BY offline_time DESC
LIMIT 1;
```

调用重新提交：

```bash
curl -X POST "${BASE_URL}/user/content/1900000000000000006/resubmit" \
  -H "Authorization: ${USER_TOKEN}"
```

预期状态变化：

```text
APPROVED + OFFLINE
-> PENDING_REVIEW + UNPUBLISHED
-> APPROVED + PUBLISHED
```

最终用一条 SQL 检查整个内容生命周期轨迹：

```sql
SELECT
    audit_action,
    from_audit_status,
    to_audit_status,
    from_publish_status,
    to_publish_status,
    audit_reason,
    risk_words,
    audit_source,
    create_time
FROM audit_record
WHERE content_id = 1900000000000000005
ORDER BY create_time ASC;
```

一条完整的退回重提发布链路，审核记录应该类似：

```text
SUBMIT
AUTO_REJECT
RESUBMIT
AUTO_PASS
```

如果中间走人工复审，则应该类似：

```text
SUBMIT
AUTO_REJECT
RESUBMIT
AUTO_TO_MANUAL
MANUAL_PASS
```