# 邮件开发使用文档

本文档用于说明 Spring Boot 3 项目中邮件模块的开发、配置和使用方式。当前章节重点介绍邮件模块的功能定位、典型使用场景、基础发送流程，以及开发前需要准备的 JDK、Spring Boot 和 SMTP 邮箱服务环境。

## 模块概述

邮件模块用于在业务系统中统一处理邮件发送能力，将 SMTP 连接、邮件内容组装、模板渲染、附件处理、发送记录和异常日志等逻辑进行封装，避免业务代码直接操作底层邮件 API。

在实际项目中，邮件发送通常不是单一功能，而是和用户注册、账号安全、业务通知、审批提醒、报表推送、异常告警等场景结合使用。因此建议将邮件能力设计为独立模块，对外提供稳定的发送接口，对内统一维护配置、模板、日志和异常处理逻辑。

### 功能定位

邮件模块的核心定位是为业务系统提供统一、可复用、可维护的邮件发送能力。业务模块只需要传入收件人、邮件主题、邮件内容、模板变量或附件信息，不需要关心 SMTP 协议、邮箱授权码、编码格式、MimeMessage 构建等底层细节。

在 Spring Boot 3 项目中，邮件模块通常基于 `spring-boot-starter-mail` 实现基础发送能力，并结合 Hutool、模板引擎、异步线程池和数据库记录完成完整的业务闭环。

邮件模块主要承担以下职责：

| 职责     | 说明                                      |
| ------ | --------------------------------------- |
| 邮件配置管理 | 统一维护 SMTP 地址、端口、账号、授权码、编码和 SSL/TLS 配置   |
| 邮件内容组装 | 支持普通文本、HTML、模板内容和附件内容的统一组装              |
| 邮件发送封装 | 屏蔽 `JavaMailSender` 的底层调用细节，提供业务友好的发送方法 |
| 模板渲染   | 根据模板文件和变量数据生成最终邮件内容                     |
| 异步发送   | 避免邮件发送阻塞主业务流程，提高接口响应速度                  |
| 发送记录   | 记录邮件发送状态、失败原因、发送时间和收件人信息                |
| 异常处理   | 统一捕获发送异常，便于日志排查和失败重试                    |

该模块不建议直接散落在各个业务 Service 中调用 `JavaMailSender`。更推荐通过 `MailClient`、`MailService` 或 `MailSendService` 这类统一入口进行封装，后续扩展短信、站内信、企业微信、钉钉通知等能力时，也可以形成统一的消息通知体系。

### 典型使用场景

邮件模块适用于需要通过邮箱触达用户、管理员或业务人员的系统场景。根据业务重要性和发送频率，可以分为验证码类、通知类、报表类和告警类。

常见使用场景如下：

| 场景   | 示例               | 特点                    |
| ---- | ---------------- | --------------------- |
| 注册验证 | 用户注册时发送邮箱验证码     | 对实时性要求较高，通常需要限制发送频率   |
| 找回密码 | 用户找回密码时发送重置链接    | 对安全性要求较高，链接通常需要设置过期时间 |
| 登录提醒 | 异地登录、异常登录提醒      | 内容较短，通常需要记录发送结果       |
| 业务通知 | 审批结果、订单状态、任务分配通知 | 和业务流程强关联，建议异步发送       |
| 报表推送 | 每日、每周、每月业务报表邮件   | 可能包含附件，发送内容相对固定       |
| 系统告警 | 程序异常、任务失败、资源不足提醒 | 需要尽快触达运维或开发人员         |
| 营销邮件 | 活动通知、产品公告、用户召回   | 发送量可能较大，需要考虑频率控制和退订机制 |

在企业级业务系统中，建议优先保障验证码、找回密码、异常登录、系统告警等高优先级邮件的稳定性。营销类或批量类邮件则应单独进行发送频率控制，避免影响核心业务邮件的发送。

### 邮件发送流程

邮件发送流程通常从业务模块发起，由邮件模块完成参数校验、内容组装、模板渲染、SMTP 发送、状态记录和日志输出。对于耗时较长或非核心链路的邮件，建议使用异步发送方式，避免阻塞主业务接口。

整体流程如下：

```text
业务模块发起发送请求
        |
        v
校验收件人、主题、内容、模板参数
        |
        v
根据邮件类型组装邮件内容
        |
        v
普通文本 / HTML / 模板 / 附件
        |
        v
调用统一邮件发送服务
        |
        v
通过 JavaMailSender 连接 SMTP 服务
        |
        v
发送成功或发送失败
        |
        v
记录发送状态、错误信息和关键日志
```

对于普通文本邮件，流程相对简单，只需要设置收件人、主题和正文内容。对于 HTML 邮件，需要开启 HTML 格式支持，并保证内容编码为 UTF-8。对于附件邮件，需要使用 `MimeMessageHelper` 构建复杂邮件内容。对于模板邮件，则需要先通过模板引擎将变量渲染为最终 HTML 内容，再执行发送。

推荐的调用链路如下：

```text
Controller
    -> MailSendService
        -> MailTemplateService
        -> MailRecordService
        -> JavaMailSender
```

其中 `Controller` 负责接收接口请求，`MailSendService` 负责业务级发送编排，`MailTemplateService` 负责模板渲染，`MailRecordService` 负责发送记录维护，`JavaMailSender` 负责实际 SMTP 发送。

这种分层方式可以降低邮件模块和具体业务模块之间的耦合度，也便于后续增加失败重试、限流、异步队列、发送统计和多邮箱账号切换等功能。

## 环境准备

邮件模块开发前需要先确认基础运行环境是否满足要求。当前文档默认使用 JDK 21、Spring Boot 3 和标准 SMTP 邮箱服务进行开发，后续依赖配置、邮件发送代码和接口设计都会基于该环境展开。

如果项目中已经使用 Spring Boot 3，则通常只需要补充邮件相关依赖和 SMTP 配置。如果项目仍然停留在 Spring Boot 2 或 JDK 8/11 环境，需要先评估升级成本，尤其要注意 Jakarta EE 包名变化、依赖兼容性和构建工具版本。

### JDK 21 环境要求

本邮件模块推荐使用 JDK 21 作为运行环境。Spring Boot 3 最低要求 Java 17，而 JDK 21 是当前较新的长期支持版本，适合作为新项目或升级项目的基础版本。

需要确认本地开发环境、测试环境和生产环境使用的 Java 版本一致，避免出现本地可以编译运行、服务器启动失败的问题。

可以通过以下命令查看当前 JDK 版本。

```bash
# 查看 Java 运行环境版本
java -version

# 查看 Java 编译器版本
javac -version
```

正常情况下，应看到类似以下输出。

```text
java version "21.x.x"
Java(TM) SE Runtime Environment ...
Java HotSpot(TM) 64-Bit Server VM ...
```

如果服务器存在多个 JDK 版本，需要确认 `JAVA_HOME` 和 `PATH` 指向的是 JDK 21。

```bash
# 查看 JAVA_HOME 配置
echo $JAVA_HOME

# 查看当前 java 命令所在路径
which java

# 查看当前 javac 命令所在路径
which javac
```

如果使用 Maven 构建项目，也需要确认 Maven 使用的 Java 版本是否为 JDK 21。

```bash
# 查看 Maven 使用的 Java 版本
mvn -v
```

推荐 Maven 输出中包含如下信息：

```text
Java version: 21.x.x
```

在项目中建议显式声明 Java 版本，避免团队成员或 CI/CD 环境使用错误版本构建。

```xml
<properties>
    <!-- 项目统一使用 JDK 21 编译 -->
    <java.version>21</java.version>

    <!-- 保证源码和目标字节码版本一致 -->
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

需要注意的是，如果项目中存在较旧的三方依赖，升级到 JDK 21 后需要重点检查编译插件、Lombok、MapStruct、MyBatis-Plus、Hutool 等依赖版本是否兼容。

### Spring Boot 3 版本要求

本邮件模块默认基于 Spring Boot 3 开发。Spring Boot 3 已经从 Java EE 迁移到 Jakarta EE，因此部分包名从 `javax.*` 变更为 `jakarta.*`。邮件模块虽然主要使用 Spring 提供的 `JavaMailSender`，但在涉及底层 MIME 消息、校验注解或 Web 接口参数校验时，仍然需要注意相关依赖兼容性。

推荐使用 Spring Boot 3.2.x 或 3.3.x 及以上版本。对于新项目，可以直接使用较新的 Spring Boot 3 稳定版本；对于已有项目，建议先统一 Spring Boot 版本，再引入邮件模块。

可以在 `pom.xml` 中确认项目 Spring Boot 版本。

```xml
<parent>
    <!-- Spring Boot 3 父工程，统一管理依赖版本 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>
```

如果项目没有使用 `spring-boot-starter-parent`，而是使用 `dependencyManagement` 管理版本，也需要确保 Spring Boot 版本为 3.x。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <!-- 统一导入 Spring Boot 3 依赖版本管理 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.3.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

建议同时确认项目中是否已经引入基础 Web 能力。邮件模块本身不强制依赖 Web，但如果需要提供邮件发送接口、邮件预览接口或发送记录查询接口，通常需要引入 Web Starter。

```xml
<dependency>
    <!-- 提供 REST 接口能力，用于暴露邮件发送、预览和查询接口 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <!-- 提供参数校验能力，用于校验邮箱格式、收件人列表和请求参数 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

如果项目已经存在上述依赖，不需要重复添加。后续“依赖配置”章节会统一补充邮件发送、Hutool 和模板引擎相关依赖。

### SMTP 邮箱服务准备

SMTP 是邮件发送的基础服务。应用程序不会直接把邮件发送到用户邮箱，而是通过邮箱服务商提供的 SMTP 服务器完成邮件投递。因此在开发邮件模块前，需要先准备一个可用于系统发送的邮箱账号，并开启 SMTP 服务。

常见邮箱服务商的 SMTP 信息如下：

| 邮箱服务商   | SMTP 地址              | 常用端口          | 说明                 |
| ------- | -------------------- | ------------- | ------------------ |
| QQ 邮箱   | `smtp.qq.com`        | `465` / `587` | 通常需要开启 SMTP 并使用授权码 |
| 163 邮箱  | `smtp.163.com`       | `465` / `994` | 通常需要开启客户端授权码       |
| 企业微信邮箱  | `smtp.exmail.qq.com` | `465` / `587` | 适合企业内部系统通知         |
| Gmail   | `smtp.gmail.com`     | `465` / `587` | 通常需要应用专用密码         |
| Outlook | `smtp.office365.com` | `587`         | 通常使用 STARTTLS      |

不同邮箱服务商对端口、安全协议和授权方式要求不同。一般情况下：

| 端口    | 安全方式     | 说明                    |
| ----- | -------- | --------------------- |
| `25`  | 无加密或明文传输 | 很多云服务器默认禁用，不推荐使用      |
| `465` | SSL      | 常用于国内邮箱服务商            |
| `587` | STARTTLS | 常用于企业邮箱、Gmail、Outlook |
| `994` | SSL      | 部分邮箱服务商支持             |

开发和生产环境建议优先使用 `465` 或 `587` 端口，不建议使用 `25` 端口。很多云服务器为了防止垃圾邮件，会默认封禁 `25` 端口，即使本地开发正常，部署到服务器后也可能发送失败。

准备 SMTP 邮箱服务时，需要完成以下事项：

| 准备项        | 说明                             |
| ---------- | ------------------------------ |
| 发送邮箱账号     | 用于系统发件，例如 `notice@example.com` |
| SMTP 服务    | 在邮箱后台开启 SMTP 或客户端服务            |
| 授权码 / 应用密码 | 不建议直接使用邮箱登录密码                  |
| SMTP 地址    | 邮箱服务商提供的服务器地址                  |
| SMTP 端口    | 根据 SSL 或 STARTTLS 选择对应端口       |
| 发件人名称      | 用于展示邮件来源，例如“系统通知”              |
| 测试收件人      | 用于开发阶段验证邮件是否能正常送达              |

SMTP 账号和授权码属于敏感配置，不应写死在代码中，也不建议提交到 Git 仓库。推荐通过环境变量、配置中心、Kubernetes Secret、服务器启动参数或 CI/CD 密钥变量进行管理。

后续配置文件中可以使用环境变量占位，例如：

```yaml
spring:
  mail:
    # SMTP 服务器地址，例如 smtp.qq.com、smtp.exmail.qq.com
    host: ${MAIL_HOST}

    # SMTP 服务端口，SSL 通常使用 465，STARTTLS 通常使用 587
    port: ${MAIL_PORT:465}

    # 发件邮箱账号
    username: ${MAIL_USERNAME}

    # 邮箱授权码或应用专用密码，不建议使用邮箱登录密码
    password: ${MAIL_PASSWORD}
```

本地开发时，可以临时通过启动参数指定环境变量。

```bash
# Linux 或 macOS 本地启动前设置邮箱配置
export MAIL_HOST=smtp.qq.com
export MAIL_PORT=465
export MAIL_USERNAME=your-email@qq.com
export MAIL_PASSWORD=your-mail-auth-code

# 启动 Spring Boot 项目
mvn spring-boot:run
```

上述命令用于在当前终端会话中临时设置邮件服务配置，并通过 Maven 启动 Spring Boot 项目。`MAIL_PASSWORD` 应填写邮箱服务商生成的授权码或应用专用密码，不应填写个人邮箱登录密码。

如果部署在服务器中，建议将这些配置放入独立环境变量、容器编排平台 Secret 或配置中心中统一管理。生产环境还需要额外关注发送频率限制、失败重试策略、退信处理和发件账号安全策略。

另外，本次对话我会继续按该 skill 的写法处理后续章节；跨新对话固定这个偏好时，需要在新对话里再说明一次。

继续补充“依赖配置”和“邮件基础配置”两部分内容，结构对应你给出的邮件开发使用大纲。

## 依赖配置

邮件模块依赖主要分为三类：邮件发送依赖、工具类依赖和模板引擎依赖。邮件发送依赖负责连接 SMTP 服务并发送邮件；Hutool 负责补充常用字符串、集合、邮箱格式、日期和异常工具能力；模板引擎负责根据模板文件和变量数据生成 HTML 邮件内容。

如果项目已经使用 Spring Boot 3，建议所有依赖版本优先交给 Spring Boot 父工程或 `dependencyManagement` 管理。对于 Spring Boot 没有托管的依赖，例如 Hutool，可以在 `properties` 中统一声明版本，避免后续多个模块重复维护。

### Spring Boot Mail 依赖

Spring Boot Mail 是邮件模块的核心依赖，底层基于 Jakarta Mail 提供邮件发送能力。项目引入该依赖后，可以直接使用 Spring Boot 自动装配的 `JavaMailSender` 完成普通文本、HTML 和附件邮件发送。

文件位置：`pom.xml`

```xml
<dependencies>
    <dependency>
        <!-- Spring Boot 邮件发送依赖，提供 JavaMailSender、MimeMessage 等邮件发送能力 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
</dependencies>
```

如果邮件模块需要对外提供 REST 接口，例如邮件发送接口、邮件预览接口和发送记录查询接口，还需要引入 Web 依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <dependency>
        <!-- 提供 REST 接口能力，用于暴露邮件发送、预览和查询接口 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <!-- 提供参数校验能力，用于校验邮箱、主题、模板参数等请求字段 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <!-- Spring Boot 邮件发送依赖，提供 JavaMailSender 邮件发送客户端 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
</dependencies>
```

如果当前项目已经存在 `spring-boot-starter-web` 和 `spring-boot-starter-validation`，则不需要重复添加。邮件发送的最小必需依赖是 `spring-boot-starter-mail`。

在 Spring Boot 3 项目中，邮件相关底层 API 已经适配 Jakarta 命名空间，不建议继续手动引入老版本的 `javax.mail` 依赖，否则可能出现包冲突或运行时类型不兼容问题。

### Hutool 工具依赖

Hutool 在邮件模块中主要用于增强基础工具能力，例如邮箱格式校验、字符串判空、集合判空、Map 构建、异常堆栈处理和日期格式化等。它不替代 Spring Boot Mail，但可以让业务代码更简洁。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Hutool 工具包版本，建议在父工程中统一管理 -->
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <dependency>
        <!-- Hutool 全量工具包，提供字符串、集合、邮箱校验、日期、异常等常用工具 -->
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

如果项目对依赖体积控制较严格，也可以按需引入 Hutool 子模块。但对于常规后台管理系统或业务中台项目，直接使用 `hutool-all` 更方便，维护成本更低。

邮件模块中常用的 Hutool 工具类如下：

| 工具类          | 常见用途                                 |
| --------------- | ---------------------------------------- |
| `StrUtil`       | 判断主题、内容、模板编号是否为空         |
| `CollUtil`      | 判断收件人、抄送人、附件集合是否为空     |
| `ArrayUtil`     | 处理多个收件人地址                       |
| `MapUtil`       | 构建模板变量 Map                         |
| `Validator`     | 校验邮箱格式                             |
| `ExceptionUtil` | 获取异常堆栈信息并记录发送失败原因       |
| `DateUtil`      | 格式化发送时间、生成邮件内容中的时间文本 |

示例代码中可以优先使用 Hutool 完成邮箱参数校验。

```java
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;

import java.util.List;

public class MailParamCheckExample {

    public void checkMailParam(List<String> receivers, String subject) {
        if (CollUtil.isEmpty(receivers)) {
            throw new IllegalArgumentException("收件人不能为空");
        }

        if (StrUtil.isBlank(subject)) {
            throw new IllegalArgumentException("邮件主题不能为空");
        }

        boolean hasInvalidEmail = receivers.stream()
                .anyMatch(email -> !Validator.isEmail(email));

        if (hasInvalidEmail) {
            throw new IllegalArgumentException("收件人邮箱格式不正确");
        }
    }
}
```

上面的代码只用于说明 Hutool 在邮件参数校验中的典型用法，后续“核心功能实现”章节会给出完整的邮件发送客户端封装。

### 模板引擎依赖

模板邮件适合发送结构固定、内容动态变化的 HTML 邮件，例如验证码邮件、审批通知邮件、订单通知邮件、报表推送邮件等。模板引擎负责把模板文件中的变量替换为业务数据，最终生成完整 HTML 内容。

Spring Boot 项目中常见的模板引擎包括 Thymeleaf、Freemarker 和 Velocity。当前邮件模块推荐使用 Thymeleaf，原因是它与 Spring Boot 集成简单，语法清晰，适合维护 HTML 邮件模板。

文件位置：`pom.xml`

```xml
<dependencies>
    <dependency>
        <!-- Thymeleaf 模板引擎，用于渲染 HTML 邮件模板 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

如果项目中仅需要发送普通文本邮件或由业务代码直接拼接 HTML 内容，可以暂时不引入模板引擎。但在实际业务项目中，不建议长期通过字符串拼接维护 HTML 邮件，因为可读性差、维护成本高，也容易引入样式和转义问题。

推荐模板文件放在以下目录：

```text
src/main/resources/templates/mail/
├── verify-code.html
├── reset-password.html
├── approval-notice.html
└── report-notice.html
```

其中：

| 模板文件               | 用途               |
| ---------------------- | ------------------ |
| `verify-code.html`     | 邮箱验证码         |
| `reset-password.html`  | 找回密码或重置密码 |
| `approval-notice.html` | 审批通知           |
| `report-notice.html`   | 报表推送通知       |

一个简单的验证码邮件模板如下。

文件位置：`src/main/resources/templates/mail/verify-code.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>邮箱验证码</title>
</head>
<body>
<div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8;">
    <h2>邮箱验证码</h2>
    <p>您好，您的验证码为：</p>
    <p style="font-size: 24px; font-weight: bold; color: #1677ff;">
        <span th:text="${code}">123456</span>
    </p>
    <p>
        验证码有效期为
        <span th:text="${expireMinutes}">5</span>
        分钟，请勿泄露给他人。
    </p>
    <p style="color: #999;">
        如果不是您本人操作，请忽略本邮件。
    </p>
</div>
</body>
</html>
```

后续模板渲染时，只需要传入 `code` 和 `expireMinutes` 两个变量，即可生成最终 HTML 邮件内容。

## 邮件基础配置

邮件基础配置用于声明 SMTP 服务器、邮箱账号、授权码、编码、连接超时时间、SSL/TLS 参数等内容。Spring Boot 会根据 `spring.mail` 配置自动创建 `JavaMailSender`，业务代码只需要注入后使用。

建议将邮件配置拆分为两部分：一部分使用 Spring Boot 标准配置 `spring.mail`，用于驱动底层邮件发送；另一部分使用自定义业务配置 `mail.biz`，用于维护发件人名称、默认开关、发送记录、频率控制等业务参数。

### SMTP 服务器配置

SMTP 服务器配置决定系统通过哪个邮箱服务商发送邮件。不同邮箱服务商的 SMTP 地址、端口和安全协议不同，配置时需要以实际邮箱服务商提供的信息为准。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  mail:
    # SMTP 服务器地址，例如 smtp.qq.com、smtp.163.com、smtp.exmail.qq.com
    host: ${MAIL_HOST:smtp.qq.com}

    # SMTP 服务端口，SSL 通常使用 465，STARTTLS 通常使用 587
    port: ${MAIL_PORT:465}

    # 发件邮箱账号
    username: ${MAIL_USERNAME:your-email@qq.com}

    # 邮箱授权码或应用专用密码，不建议使用邮箱登录密码
    password: ${MAIL_PASSWORD:your-mail-auth-code}

    # 默认邮件编码，建议统一使用 UTF-8
    default-encoding: UTF-8

    properties:
      mail:
        smtp:
          # 开启 SMTP 身份认证
          auth: true

          # 使用 465 端口时通常开启 SSL
          ssl:
            enable: true

          # SMTP 连接超时时间，单位毫秒
          connectiontimeout: 10000

          # SMTP 读取超时时间，单位毫秒
          timeout: 10000

          # SMTP 写入超时时间，单位毫秒
          writetimeout: 10000
```

上面配置适合使用 SSL 的 SMTP 服务，例如 QQ 邮箱、163 邮箱或部分企业邮箱。`host`、`port`、`username` 和 `password` 都建议使用环境变量覆盖，避免敏感信息进入代码仓库。

如果邮箱服务商要求使用 STARTTLS，例如部分 Gmail、Outlook 或企业邮箱配置，可以调整为以下形式。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  mail:
    # SMTP 服务器地址，根据邮箱服务商调整
    host: ${MAIL_HOST:smtp.office365.com}

    # STARTTLS 通常使用 587 端口
    port: ${MAIL_PORT:587}

    # 发件邮箱账号
    username: ${MAIL_USERNAME:notice@example.com}

    # 邮箱授权码或应用专用密码
    password: ${MAIL_PASSWORD:your-mail-auth-code}

    # 默认邮件编码
    default-encoding: UTF-8

    properties:
      mail:
        smtp:
          # 开启 SMTP 身份认证
          auth: true

          # 开启 STARTTLS
          starttls:
            enable: true
            required: true

          # SMTP 连接超时时间，单位毫秒
          connectiontimeout: 10000

          # SMTP 读取超时时间，单位毫秒
          timeout: 10000

          # SMTP 写入超时时间，单位毫秒
          writetimeout: 10000
```

SSL 和 STARTTLS 通常不要同时混用。使用 `465` 端口时一般配置 `mail.smtp.ssl.enable=true`；使用 `587` 端口时一般配置 `mail.smtp.starttls.enable=true`。如果配置错误，常见表现是连接超时、认证失败或握手失败。

### 账号与授权码配置

邮件账号和授权码属于敏感配置，应避免写死在 `application.yml` 中。推荐通过环境变量、启动参数、配置中心、Kubernetes Secret 或 CI/CD 密钥变量进行注入。

本地开发时，可以使用环境变量方式启动项目。

```bash
# 设置 SMTP 服务器地址
export MAIL_HOST=smtp.qq.com

# 设置 SMTP 服务端口
export MAIL_PORT=465

# 设置发件邮箱账号
export MAIL_USERNAME=your-email@qq.com

# 设置邮箱授权码或应用专用密码
export MAIL_PASSWORD=your-mail-auth-code

# 启动 Spring Boot 项目
mvn spring-boot:run
```

如果使用 Jar 包方式启动，可以通过启动参数覆盖配置。

```bash
java -jar mail-demo.jar \
  --spring.mail.host=smtp.qq.com \
  --spring.mail.port=465 \
  --spring.mail.username=your-email@qq.com \
  --spring.mail.password=your-mail-auth-code
```

上述方式适合临时验证或测试环境使用。生产环境不建议直接在命令行中暴露明文授权码，因为命令可能被进程列表、运维脚本或日志系统采集。

在生产环境中，更推荐使用环境变量或密钥管理平台，例如：

```bash
# 生产环境建议由服务器环境、容器平台或 CI/CD 系统注入
MAIL_HOST=smtp.exmail.qq.com
MAIL_PORT=465
MAIL_USERNAME=notice@example.com
MAIL_PASSWORD=********
```

账号与授权码管理需要注意以下几点：

| 配置项   | 建议                                 |
| -------- | ------------------------------------ |
| 发件账号 | 使用系统专用邮箱，不建议使用个人邮箱 |
| 授权码   | 使用邮箱服务商生成的授权码或应用密码 |
| 密码保存 | 不提交 Git，不写入代码，不打印日志   |
| 权限控制 | 限制只有部署人员或配置中心管理员可见 |
| 定期更换 | 按安全要求定期轮换授权码             |
| 异常处理 | 授权码失效时需要记录明确错误日志     |

如果授权码配置错误，通常会出现认证失败异常。后续“异常处理与日志”章节中，需要对这类异常进行统一封装，并记录可排查但不泄露敏感信息的日志。

### 邮件发送参数配置

除了 Spring Boot 标准的 `spring.mail` 配置，业务系统通常还需要维护一些邮件发送业务参数，例如默认发件人名称、是否开启邮件发送、是否记录发送日志、默认超时时间、测试收件人白名单等。

建议新增自定义配置项，例如 `mail.biz`。

文件位置：`src/main/resources/application.yml`

```yaml
mail:
  biz:
    # 是否启用邮件发送，测试环境可以关闭真实发送
    enabled: true

    # 默认发件人名称，会展示在收件人邮箱中
    from-name: 系统通知

    # 是否记录邮件发送日志
    record-enabled: true

    # 是否启用异步发送
    async-enabled: true

    # 测试环境收件人白名单，为空表示不限制
    test-receiver-whitelist:
      - test@example.com
      - admin@example.com

    # 单封邮件最大附件大小，单位 MB
    max-attachment-size-mb: 20

    # 单次最多收件人数，避免一次发送过多导致服务商拦截
    max-receiver-count: 50
```

为了让业务代码能够类型安全地读取这些配置，建议定义配置属性类。

文件位置：`src/main/java/io/github/atengk/mail/config/MailBizProperties.java`

```java
package io.github.atengk.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件业务配置属性
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@ConfigurationProperties(prefix = "mail.biz")
public class MailBizProperties {

    /**
     * 是否启用邮件发送
     */
    private Boolean enabled = true;

    /**
     * 默认发件人名称
     */
    private String fromName = "系统通知";

    /**
     * 是否记录邮件发送日志
     */
    private Boolean recordEnabled = true;

    /**
     * 是否启用异步发送
     */
    private Boolean asyncEnabled = true;

    /**
     * 测试环境收件人白名单
     */
    private List<String> testReceiverWhitelist = new ArrayList<>();

    /**
     * 单封邮件最大附件大小，单位 MB
     */
    private Integer maxAttachmentSizeMb = 20;

    /**
     * 单次最多收件人数
     */
    private Integer maxReceiverCount = 50;
}
```

然后在配置类中启用该属性类。

文件位置：`src/main/java/io/github/atengk/mail/config/MailConfig.java`

```java
package io.github.atengk.mail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 邮件模块配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MailBizProperties.class)
public class MailConfig {

    private final MailBizProperties mailBizProperties;

    public MailConfig(MailBizProperties mailBizProperties) {
        this.mailBizProperties = mailBizProperties;
    }

    /**
     * 初始化邮件模块配置
     */
    @PostConstruct
    public void init() {
        log.info("邮件模块初始化完成，启用状态：{}，异步发送：{}，发送记录：{}",
                mailBizProperties.getEnabled(),
                mailBizProperties.getAsyncEnabled(),
                mailBizProperties.getRecordEnabled());
    }
}
```

如果项目已经有统一的配置类扫描机制，也可以直接在启动类上添加 `@ConfigurationPropertiesScan`，这样就不需要在每个配置类中单独使用 `@EnableConfigurationProperties`。

文件位置：`src/main/java/io/github/atengk/Application.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 应用启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class Application {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

配置完成后，可以通过启动日志确认邮件模块是否正常加载。如果项目能够正常启动，并且日志中输出了邮件模块初始化信息，说明自定义业务配置已经被 Spring Boot 正确绑定。

建议后续在邮件发送服务中统一使用 `MailBizProperties` 判断是否允许发送邮件、是否启用异步、是否记录发送记录，以及是否需要限制测试环境收件人范围。这样可以避免业务代码直接读取配置文件，也便于后续扩展更多邮件发送策略。

继续补充“核心功能实现”部分，本节对应邮件发送客户端封装、普通文本邮件、HTML 邮件、附件邮件和模板邮件发送能力。

## 核心功能实现

核心功能实现用于封装底层 `JavaMailSender`，对业务层提供统一的邮件发送入口。业务代码不直接操作 `MimeMessage`、`MimeMessageHelper` 或 SMTP 配置，而是通过邮件发送客户端完成普通文本、HTML、附件和模板邮件发送。

本节只实现邮件发送核心能力，不包含接口层、异步发送、发送记录入库和失败重试逻辑。这些内容会在后续章节中继续展开。

核心文件结构如下：

```text
src/main/java/io/github/atengk/mail/
├── client/
│   └── MailSendClient.java
├── model/
│   ├── MailAttachment.java
│   ├── MailSendRequest.java
│   └── MailSendResult.java
```

### 邮件发送客户端封装

邮件发送客户端是邮件模块的核心入口，主要负责参数校验、发件人构建、收件人设置、正文设置、附件添加、模板渲染和异常捕获。客户端内部统一使用 `MimeMessageHelper` 发送邮件，即使是普通文本邮件，也可以保持后续扩展能力一致。

先定义邮件附件对象，用于封装附件显示名称和本地文件对象。

文件位置：`src/main/java/io/github/atengk/mail/model/MailAttachment.java`

```java
package io.github.atengk.mail.model;

import cn.hutool.core.util.StrUtil;

import java.io.File;

/**
 * 邮件附件对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailAttachment(String fileName, File file) {

    /**
     * 获取附件显示名称
     *
     * @return 附件显示名称
     */
    public String getDisplayFileName() {
        if (file == null) {
            return fileName;
        }
        return StrUtil.blankToDefault(fileName, file.getName());
    }
}
```

再定义邮件发送请求对象，统一承载收件人、抄送人、密送人、主题、正文、模板和附件参数。

文件位置：`src/main/java/io/github/atengk/mail/model/MailSendRequest.java`

```java
package io.github.atengk.mail.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendRequest(
        List<String> receivers,
        List<String> ccList,
        List<String> bccList,
        String subject,
        String content,
        boolean html,
        List<MailAttachment> attachments,
        String templateName,
        Map<String, Object> templateVariables
) {

    /**
     * 创建普通文本邮件请求
     *
     * @param receivers 收件人列表
     * @param subject   邮件主题
     * @param content   邮件正文
     * @return 邮件发送请求
     */
    public static MailSendRequest text(List<String> receivers, String subject, String content) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                false,
                Collections.emptyList(),
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建 HTML 邮件请求
     *
     * @param receivers 收件人列表
     * @param subject   邮件主题
     * @param content   HTML 正文
     * @return 邮件发送请求
     */
    public static MailSendRequest html(List<String> receivers, String subject, String content) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                true,
                Collections.emptyList(),
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建附件邮件请求
     *
     * @param receivers   收件人列表
     * @param subject     邮件主题
     * @param content     邮件正文
     * @param html        是否为 HTML 正文
     * @param attachments 附件列表
     * @return 邮件发送请求
     */
    public static MailSendRequest attachment(List<String> receivers,
                                             String subject,
                                             String content,
                                             boolean html,
                                             List<MailAttachment> attachments) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                html,
                attachments,
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建模板邮件请求
     *
     * @param receivers         收件人列表
     * @param subject           邮件主题
     * @param templateName      模板名称
     * @param templateVariables 模板变量
     * @return 邮件发送请求
     */
    public static MailSendRequest template(List<String> receivers,
                                           String subject,
                                           String templateName,
                                           Map<String, Object> templateVariables) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                null,
                true,
                Collections.emptyList(),
                templateName,
                templateVariables
        );
    }
}
```

定义统一发送结果对象，便于业务层判断邮件是否发送成功、是否跳过发送以及失败原因。

文件位置：`src/main/java/io/github/atengk/mail/model/MailSendResult.java`

```java
package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 邮件发送结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendResult(
        boolean success,
        boolean skipped,
        String message,
        LocalDateTime sendTime,
        String errorStack
) {

    /**
     * 创建发送成功结果
     *
     * @param message 结果说明
     * @return 邮件发送结果
     */
    public static MailSendResult success(String message) {
        return new MailSendResult(true, false, message, LocalDateTime.now(), null);
    }

    /**
     * 创建跳过发送结果
     *
     * @param message 结果说明
     * @return 邮件发送结果
     */
    public static MailSendResult skipped(String message) {
        return new MailSendResult(false, true, message, LocalDateTime.now(), null);
    }

    /**
     * 创建发送失败结果
     *
     * @param message    结果说明
     * @param errorStack 异常堆栈
     * @return 邮件发送结果
     */
    public static MailSendResult failure(String message, String errorStack) {
        return new MailSendResult(false, false, message, LocalDateTime.now(), errorStack);
    }
}
```

下面是邮件发送客户端完整实现，包含普通文本、HTML、附件和模板邮件发送能力。

文件位置：`src/main/java/io/github/atengk/mail/client/MailSendClient.java`

```java
package io.github.atengk.mail.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.config.MailBizProperties;
import io.github.atengk.mail.model.MailAttachment;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 邮件发送客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class MailSendClient {

    private static final Logger log = LoggerFactory.getLogger(MailSendClient.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final MailBizProperties mailBizProperties;
    private final TemplateEngine templateEngine;

    public MailSendClient(JavaMailSender javaMailSender,
                          MailProperties mailProperties,
                          MailBizProperties mailBizProperties,
                          TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.mailBizProperties = mailBizProperties;
        this.templateEngine = templateEngine;
    }

    /**
     * 发送普通文本邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendTextMail(MailSendRequest request) {
        return sendMimeMail(request, false, false);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendHtmlMail(MailSendRequest request) {
        return sendMimeMail(request, true, false);
    }

    /**
     * 发送附件邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendAttachmentMail(MailSendRequest request) {
        return sendMimeMail(request, request != null && request.html(), true);
    }

    /**
     * 发送模板邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendTemplateMail(MailSendRequest request) {
        try {
            MailSendResult skippedResult = checkSendSwitch(request);
            if (skippedResult != null) {
                return skippedResult;
            }

            checkTemplateParams(request);

            Context context = new Context(Locale.CHINA);
            Map<String, Object> variables = MapUtil.emptyIfNull(request.templateVariables());
            context.setVariables(variables);

            String htmlContent = templateEngine.process(request.templateName(), context);
            MailSendRequest htmlRequest = new MailSendRequest(
                    request.receivers(),
                    request.ccList(),
                    request.bccList(),
                    request.subject(),
                    htmlContent,
                    true,
                    request.attachments(),
                    request.templateName(),
                    request.templateVariables()
            );

            return sendMimeMail(htmlRequest, true, CollUtil.isNotEmpty(request.attachments()));
        } catch (Exception exception) {
            String stacktrace = ExceptionUtil.stacktraceToString(exception);
            log.error("模板邮件发送失败，主题：{}，原因：{}", request == null ? null : request.subject(), exception.getMessage(), exception);
            return MailSendResult.failure("模板邮件发送失败：" + exception.getMessage(), stacktrace);
        }
    }

    /**
     * 发送 MIME 邮件
     *
     * @param request   邮件发送请求
     * @param html      是否为 HTML 正文
     * @param multipart 是否为复杂邮件
     * @return 邮件发送结果
     */
    private MailSendResult sendMimeMail(MailSendRequest request, boolean html, boolean multipart) {
        try {
            MailSendResult skippedResult = checkSendSwitch(request);
            if (skippedResult != null) {
                return skippedResult;
            }

            checkBaseParams(request);
            if (multipart) {
                checkAttachments(request.attachments());
            }

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    multipart,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(buildFromAddress());
            helper.setTo(request.receivers().toArray(String[]::new));

            if (CollUtil.isNotEmpty(request.ccList())) {
                helper.setCc(request.ccList().toArray(String[]::new));
            }

            if (CollUtil.isNotEmpty(request.bccList())) {
                helper.setBcc(request.bccList().toArray(String[]::new));
            }

            helper.setSubject(request.subject());
            helper.setText(request.content(), html);

            if (multipart) {
                addAttachments(helper, request.attachments());
            }

            javaMailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，主题：{}", request.receivers(), request.subject());
            return MailSendResult.success("邮件发送成功");
        } catch (Exception exception) {
            String stacktrace = ExceptionUtil.stacktraceToString(exception);
            log.error("邮件发送失败，主题：{}，原因：{}", request == null ? null : request.subject(), exception.getMessage(), exception);
            return MailSendResult.failure("邮件发送失败：" + exception.getMessage(), stacktrace);
        }
    }

    /**
     * 检查发送开关
     *
     * @param request 邮件发送请求
     * @return 跳过发送结果
     */
    private MailSendResult checkSendSwitch(MailSendRequest request) {
        if (!Boolean.TRUE.equals(mailBizProperties.getEnabled())) {
            log.warn("邮件发送开关已关闭，跳过邮件发送，收件人：{}，主题：{}",
                    request == null ? null : request.receivers(),
                    request == null ? null : request.subject());
            return MailSendResult.skipped("邮件发送开关已关闭");
        }
        return null;
    }

    /**
     * 检查基础参数
     *
     * @param request 邮件发送请求
     */
    private void checkBaseParams(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("邮件发送请求不能为空");
        }

        if (CollUtil.isEmpty(request.receivers())) {
            throw new IllegalArgumentException("邮件收件人不能为空");
        }

        if (StrUtil.isBlank(request.subject())) {
            throw new IllegalArgumentException("邮件主题不能为空");
        }

        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("邮件正文不能为空");
        }

        checkReceivers(request);
    }

    /**
     * 检查模板参数
     *
     * @param request 邮件发送请求
     */
    private void checkTemplateParams(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板邮件发送请求不能为空");
        }

        if (CollUtil.isEmpty(request.receivers())) {
            throw new IllegalArgumentException("模板邮件收件人不能为空");
        }

        if (StrUtil.isBlank(request.subject())) {
            throw new IllegalArgumentException("模板邮件主题不能为空");
        }

        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        checkReceivers(request);
    }

    /**
     * 检查收件人参数
     *
     * @param request 邮件发送请求
     */
    private void checkReceivers(MailSendRequest request) {
        List<String> allReceivers = new ArrayList<>();
        allReceivers.addAll(CollUtil.emptyIfNull(request.receivers()));
        allReceivers.addAll(CollUtil.emptyIfNull(request.ccList()));
        allReceivers.addAll(CollUtil.emptyIfNull(request.bccList()));

        Integer maxReceiverCount = mailBizProperties.getMaxReceiverCount();
        if (maxReceiverCount != null && maxReceiverCount > 0 && allReceivers.size() > maxReceiverCount) {
            throw new IllegalArgumentException("邮件收件人数超过限制：" + maxReceiverCount);
        }

        List<String> invalidReceivers = allReceivers.stream()
                .filter(email -> !Validator.isEmail(email))
                .toList();

        if (CollUtil.isNotEmpty(invalidReceivers)) {
            throw new IllegalArgumentException("邮箱格式不正确：" + invalidReceivers);
        }

        List<String> whitelist = mailBizProperties.getTestReceiverWhitelist();
        if (CollUtil.isNotEmpty(whitelist)) {
            List<String> blockedReceivers = allReceivers.stream()
                    .filter(email -> !whitelist.contains(email))
                    .toList();

            if (CollUtil.isNotEmpty(blockedReceivers)) {
                throw new IllegalArgumentException("收件人不在测试白名单中：" + blockedReceivers);
            }
        }
    }

    /**
     * 检查附件参数
     *
     * @param attachments 附件列表
     */
    private void checkAttachments(List<MailAttachment> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            throw new IllegalArgumentException("附件邮件的附件不能为空");
        }

        Integer maxAttachmentSizeMb = mailBizProperties.getMaxAttachmentSizeMb();
        long maxAttachmentSize = maxAttachmentSizeMb == null ? 20L * 1024 * 1024 : maxAttachmentSizeMb * 1024L * 1024L;

        for (MailAttachment attachment : attachments) {
            if (attachment == null || attachment.file() == null) {
                throw new IllegalArgumentException("附件文件不能为空");
            }

            File file = attachment.file();
            if (!FileUtil.exist(file.getAbsolutePath())) {
                throw new IllegalArgumentException("附件文件不存在：" + file.getAbsolutePath());
            }

            if (FileUtil.size(file) > maxAttachmentSize) {
                throw new IllegalArgumentException("附件文件超过大小限制：" + file.getAbsolutePath());
            }
        }
    }

    /**
     * 添加附件
     *
     * @param helper      MIME 消息辅助对象
     * @param attachments 附件列表
     * @throws Exception 附件添加异常
     */
    private void addAttachments(MimeMessageHelper helper, List<MailAttachment> attachments) throws Exception {
        for (MailAttachment attachment : attachments) {
            helper.addAttachment(attachment.getDisplayFileName(), attachment.file());
        }
    }

    /**
     * 构建发件人地址
     *
     * @return 发件人地址
     * @throws Exception 地址构建异常
     */
    private InternetAddress buildFromAddress() throws Exception {
        String username = mailProperties.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("发件邮箱账号不能为空");
        }

        String fromName = StrUtil.blankToDefault(mailBizProperties.getFromName(), username);
        return new InternetAddress(username, fromName, StandardCharsets.UTF_8.name());
    }
}
```

该客户端有几个关键设计点：

| 设计点                       | 说明                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| 统一使用 `MimeMessageHelper` | 普通文本、HTML、附件邮件都走统一构建逻辑                     |
| 使用 `MailBizProperties`     | 读取发送开关、发件人名称、附件大小、收件人数量等业务配置     |
| 使用 Hutool 校验参数         | 通过 `StrUtil`、`CollUtil`、`Validator`、`FileUtil` 简化校验逻辑 |
| 返回 `MailSendResult`        | 业务层可以根据结果判断成功、失败或跳过发送                   |
| 记录中文日志                 | 成功、失败、跳过发送都会输出关键日志                         |
| 模板渲染独立处理             | 先用 Thymeleaf 渲染 HTML，再调用 HTML 邮件发送逻辑           |

### 发送普通文本邮件

普通文本邮件适合发送内容简单、没有样式要求的通知，例如系统提醒、简单验证码、任务处理结果等。普通文本邮件的兼容性最好，几乎所有邮箱客户端都能稳定展示。

调用普通文本邮件时，只需要传入收件人、主题和正文内容。

下面示例演示在业务 Service 中发送普通文本邮件。

文件位置：`src/main/java/io/github/atengk/mail/service/MailTextExampleService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 普通文本邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTextExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailTextExampleService.class);

    private final MailSendClient mailSendClient;

    public MailTextExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送账号登录提醒
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendLoginNotice(String receiver) {
        String subject = "账号登录提醒";
        String content = "您的账号刚刚完成一次登录操作，如非本人操作，请及时修改密码。";

        MailSendRequest request = MailSendRequest.text(
                ListUtil.of(receiver),
                subject,
                content
        );

        MailSendResult result = mailSendClient.sendTextMail(request);
        log.info("登录提醒邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}
```

普通文本邮件的核心调用方法是：

```java
mailSendClient.sendTextMail(request);
```

使用时需要注意：

| 注意项               | 说明                                   |
| -------------------- | -------------------------------------- |
| 正文不支持 HTML 样式 | `<br>`、`<p>` 等标签会按普通文本显示   |
| 适合简单通知         | 不适合复杂排版、按钮、表格和品牌化邮件 |
| 内容建议简短         | 过长文本建议使用 HTML 模板邮件         |

### 发送 HTML 邮件

HTML 邮件适合发送带有样式、标题、段落、按钮、表格或重点内容标识的邮件。和普通文本邮件相比，HTML 邮件展示效果更好，但需要注意不同邮箱客户端对 CSS 的兼容性。

建议 HTML 邮件尽量使用行内样式，避免依赖复杂 CSS、外部样式文件或 JavaScript。大多数邮箱客户端会过滤脚本内容。

下面示例演示发送一个带样式的验证码 HTML 邮件。

文件位置：`src/main/java/io/github/atengk/mail/service/MailHtmlExampleService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HTML 邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailHtmlExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailHtmlExampleService.class);

    private final MailSendClient mailSendClient;

    public MailHtmlExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送验证码 HTML 邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCode(String receiver) {
        String code = RandomUtil.randomNumbers(6);
        String subject = "邮箱验证码";
        String htmlContent = """
                <div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8;">
                    <h2 style="color: #333;">邮箱验证码</h2>
                    <p>您好，您的验证码为：</p>
                    <p style="font-size: 26px; font-weight: bold; color: #1677ff;">%s</p>
                    <p>验证码有效期为 5 分钟，请勿泄露给他人。</p>
                    <p style="font-size: 12px; color: #999;">如果不是您本人操作，请忽略本邮件。</p>
                </div>
                """.formatted(code);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                subject,
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("验证码 HTML 邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}
```

HTML 邮件的核心调用方法是：

```java
mailSendClient.sendHtmlMail(request);
```

HTML 邮件需要注意以下问题：

| 注意项                  | 说明                                |
| ----------------------- | ----------------------------------- |
| 优先使用行内样式        | 邮箱客户端对外部 CSS 支持不稳定     |
| 不要使用 JavaScript     | 大多数邮箱客户端会过滤脚本          |
| 图片建议使用 HTTPS 地址 | 外链图片可能被邮箱客户端默认拦截    |
| 复杂内容建议模板化      | 不建议长期在 Java 字符串中拼接 HTML |

如果邮件样式较复杂，推荐使用模板邮件，而不是直接在 Service 中拼接 HTML 字符串。

### 发送附件邮件

附件邮件适合发送报表、导出文件、合同、对账单、发票、处理结果文件等内容。附件邮件底层必须使用 MIME 复杂邮件格式，因此发送时需要开启 multipart。

当前实现中，附件使用本地 `File` 对象。实际业务中，如果附件来自上传文件、对象存储、临时导出文件或数据库文件记录，需要先将其转换为本地临时文件，发送完成后再按需清理。

下面示例演示发送本地报表附件。

文件位置：`src/main/java/io/github/atengk/mail/service/MailAttachmentExampleService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailAttachment;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 附件邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailAttachmentExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailAttachmentExampleService.class);

    private final MailSendClient mailSendClient;

    public MailAttachmentExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送日报附件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendDailyReport(String receiver) {
        File reportFile = FileUtil.file("/data/report/daily-report.xlsx");
        MailAttachment attachment = new MailAttachment("每日业务报表.xlsx", reportFile);

        MailSendRequest request = MailSendRequest.attachment(
                ListUtil.of(receiver),
                "每日业务报表",
                "您好，附件为今日业务报表，请查收。",
                false,
                ListUtil.of(attachment)
        );

        MailSendResult result = mailSendClient.sendAttachmentMail(request);
        log.info("日报附件邮件发送完成，收件人：{}，附件：{}，结果：{}",
                receiver, reportFile.getAbsolutePath(), result.message());
        return result;
    }
}
```

如果附件邮件正文需要使用 HTML，也可以将 `html` 参数设置为 `true`。

```java
MailSendRequest request = MailSendRequest.attachment(
        ListUtil.of(receiver),
        "每日业务报表",
        "<p>您好，附件为今日业务报表，请查收。</p>",
        true,
        ListUtil.of(attachment)
);
```

附件邮件需要重点关注以下问题：

| 注意项             | 说明                                                |
| ------------------ | --------------------------------------------------- |
| 附件必须存在       | 当前实现会通过 `FileUtil.exist` 校验文件路径        |
| 附件大小需要限制   | 当前默认使用 `mail.biz.max-attachment-size-mb` 控制 |
| 不建议发送超大附件 | 大附件容易被 SMTP 服务商拒绝                        |
| 临时文件需要清理   | 导出型附件发送后应删除临时文件                      |
| 文件名建议带后缀   | 否则部分邮箱客户端无法正确识别文件类型              |

如果附件来自临时生成文件，可以在发送完成后删除。

```java
try {
    MailSendResult result = mailSendClient.sendAttachmentMail(request);
    log.info("临时附件邮件发送完成，结果：{}", result.message());
    return result;
} finally {
    FileUtil.del(reportFile);
}
```

### 发送模板邮件

模板邮件适合发送格式固定但内容动态变化的邮件，例如验证码、找回密码、审批通知、订单通知、报表通知等。模板邮件通过 Thymeleaf 将模板文件和变量数据合并，生成最终 HTML 内容后再发送。

模板文件建议统一放在：

```text
src/main/resources/templates/mail/
```

假设存在验证码模板：

```text
src/main/resources/templates/mail/verify-code.html
```

模板名称传入时通常不需要写 `.html` 后缀，传入 `mail/verify-code` 即可。

下面示例演示发送验证码模板邮件。

文件位置：`src/main/java/io/github/atengk/mail/service/MailTemplateExampleService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 模板邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTemplateExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailTemplateExampleService.class);

    private final MailSendClient mailSendClient;

    public MailTemplateExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送验证码模板邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCodeTemplate(String receiver) {
        String code = RandomUtil.randomNumbers(6);
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("code", code)
                .put("expireMinutes", 5)
                .put("productName", "业务管理系统")
                .build();

        MailSendRequest request = MailSendRequest.template(
                ListUtil.of(receiver),
                "邮箱验证码",
                "mail/verify-code",
                variables
        );

        MailSendResult result = mailSendClient.sendTemplateMail(request);
        log.info("验证码模板邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}
```

模板文件示例：

文件位置：`src/main/resources/templates/mail/verify-code.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>邮箱验证码</title>
</head>
<body>
<div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8;">
    <h2 style="color: #333;" th:text="${productName}">业务管理系统</h2>
    <p>您好，您的邮箱验证码为：</p>
    <p style="font-size: 26px; font-weight: bold; color: #1677ff;">
        <span th:text="${code}">123456</span>
    </p>
    <p>
        验证码有效期为
        <span th:text="${expireMinutes}">5</span>
        分钟，请勿泄露给他人。
    </p>
    <p style="font-size: 12px; color: #999;">
        如果不是您本人操作，请忽略本邮件。
    </p>
</div>
</body>
</html>
```

模板邮件的核心调用方法是：

```java
mailSendClient.sendTemplateMail(request);
```

模板邮件需要注意以下问题：

| 注意项                   | 说明                                                      |
| ------------------------ | --------------------------------------------------------- |
| 模板路径要正确           | `mail/verify-code` 对应 `templates/mail/verify-code.html` |
| 变量名称要一致           | Java 中传入的变量名需要和模板中的 `${}` 名称一致          |
| 模板内容默认是 HTML      | 模板渲染完成后会按 HTML 邮件发送                          |
| 模板适合长期维护         | 比 Java 字符串拼接 HTML 更清晰                            |
| 不建议在模板中写复杂脚本 | 邮箱客户端通常不支持 JavaScript                           |

当前核心实现完成后，业务层已经可以通过 `MailSendClient` 发送四类常见邮件。后续接口设计、异步发送和发送记录章节，可以直接复用该客户端，不需要再重复处理 SMTP、MIME、模板渲染和附件封装细节。

继续补充“邮件模板管理”部分，本节对应模板文件目录规范、模板变量渲染和邮件内容组装。该部分基于前面已经引入的 Thymeleaf 模板引擎和邮件发送客户端实现继续展开。

## 邮件模板管理

邮件模板管理用于统一维护 HTML 邮件模板、模板变量、模板渲染和最终邮件内容组装逻辑。对于验证码、找回密码、审批通知、订单通知、报表推送等格式相对固定的邮件，不建议在 Java 代码中长期拼接 HTML 字符串，而应通过模板文件集中维护。

模板管理的目标是让邮件内容具备较好的可维护性。业务代码只负责传入模板名称和变量数据，模板服务负责渲染最终 HTML 内容，邮件发送客户端负责执行 SMTP 发送。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/mail/
├── model/
│   ├── MailTemplateRenderRequest.java
│   └── MailTemplateRenderResult.java
├── service/
│   └── MailTemplateService.java
src/main/resources/templates/mail/
├── verify-code.html
├── reset-password.html
├── approval-notice.html
└── report-notice.html
```

### 模板文件目录规范

模板文件建议统一放在 `src/main/resources/templates/mail/` 目录下，按照业务场景拆分为多个独立 HTML 文件。模板名称应简洁、稳定，并和业务含义保持一致，避免使用 `template1.html`、`mail.html` 这类不可读名称。

推荐目录结构如下：

```text
src/main/resources/templates/
└── mail/
    ├── verify-code.html          # 邮箱验证码
    ├── reset-password.html       # 找回密码
    ├── approval-notice.html      # 审批通知
    ├── order-notice.html         # 订单通知
    ├── report-notice.html        # 报表推送
    └── system-alert.html         # 系统告警
```

模板命名建议遵循以下规则：

| 规范项   | 建议                                                  |
| -------- | ----------------------------------------------------- |
| 目录位置 | 统一放在 `templates/mail/` 下                         |
| 文件后缀 | 使用 `.html`                                          |
| 文件命名 | 使用小写字母和中划线，例如 `verify-code.html`         |
| 模板名称 | Java 中使用 `mail/verify-code`，不需要写 `.html` 后缀 |
| 样式写法 | 优先使用行内样式，提高邮箱客户端兼容性                |
| 脚本使用 | 不要在邮件模板中使用 JavaScript                       |
| 图片资源 | 优先使用 HTTPS 外链或内嵌资源，不建议使用本地相对路径 |

Thymeleaf 默认会从 `classpath:/templates/` 目录加载模板，因此 `src/main/resources/templates/mail/verify-code.html` 对应的模板名称是：

```text
mail/verify-code
```

如果需要显式配置 Thymeleaf 模板路径，可以在配置文件中增加以下内容。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  thymeleaf:
    # 模板文件所在目录，默认就是 classpath:/templates/
    prefix: classpath:/templates/

    # 模板文件后缀
    suffix: .html

    # 模板编码，建议和邮件编码保持一致
    encoding: UTF-8

    # 模板模式，HTML 邮件使用 HTML
    mode: HTML

    # 开发环境可以关闭缓存，方便修改模板后立即生效
    cache: false
```

生产环境建议开启模板缓存，避免每次发送邮件都重新解析模板文件。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  thymeleaf:
    # 生产环境开启模板缓存，提高模板渲染性能
    cache: true
```

下面是验证码邮件模板示例。

文件位置：`src/main/resources/templates/mail/verify-code.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>邮箱验证码</title>
</head>
<body>
<div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8; color: #333;">
    <h2 style="margin-bottom: 16px;" th:text="${productName}">业务管理系统</h2>

    <p>您好，您的邮箱验证码为：</p>

    <p style="font-size: 28px; font-weight: bold; color: #1677ff; letter-spacing: 4px;">
        <span th:text="${code}">123456</span>
    </p>

    <p>
        验证码有效期为
        <span th:text="${expireMinutes}">5</span>
        分钟，请勿泄露给他人。
    </p>

    <p style="margin-top: 24px; font-size: 12px; color: #999;">
        如果不是您本人操作，请忽略本邮件。
    </p>
</div>
</body>
</html>
```

下面是审批通知邮件模板示例。

文件位置：`src/main/resources/templates/mail/approval-notice.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>审批通知</title>
</head>
<body>
<div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8; color: #333;">
    <h2 style="margin-bottom: 16px;">审批通知</h2>

    <p>
        您有一条新的审批任务：
        <strong th:text="${approvalTitle}">采购申请审批</strong>
    </p>

    <table style="border-collapse: collapse; width: 100%; margin-top: 16px;">
        <tr>
            <td style="border: 1px solid #ddd; padding: 8px; width: 120px;">申请人</td>
            <td style="border: 1px solid #ddd; padding: 8px;" th:text="${applicantName}">张三</td>
        </tr>
        <tr>
            <td style="border: 1px solid #ddd; padding: 8px;">申请时间</td>
            <td style="border: 1px solid #ddd; padding: 8px;" th:text="${applyTime}">2026-04-30 10:30:00</td>
        </tr>
        <tr>
            <td style="border: 1px solid #ddd; padding: 8px;">审批状态</td>
            <td style="border: 1px solid #ddd; padding: 8px;" th:text="${approvalStatus}">待审批</td>
        </tr>
    </table>

    <p style="margin-top: 20px;">
        <a th:href="${approvalUrl}"
           style="display: inline-block; padding: 8px 16px; background: #1677ff; color: #fff; text-decoration: none; border-radius: 4px;">
            查看审批详情
        </a>
    </p>
</div>
</body>
</html>
```

模板文件中变量统一使用 `${变量名}`。Java 代码传入的变量名称必须和模板中的变量名称保持一致，否则渲染结果可能为空或展示默认占位内容。

### 模板变量渲染

模板变量渲染用于将业务数据填充到 HTML 模板中。推荐单独封装模板渲染服务，避免模板处理逻辑散落在邮件发送客户端或业务 Service 中。

先定义模板渲染请求对象，用于封装模板名称和变量数据。

文件位置：`src/main/java/io/github/atengk/mail/model/MailTemplateRenderRequest.java`

```java
package io.github.atengk.mail.model;

import java.util.Map;

/**
 * 邮件模板渲染请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailTemplateRenderRequest(
        String templateName,
        Map<String, Object> variables
) {
}
```

再定义模板渲染结果对象，用于返回最终渲染后的 HTML 内容。

文件位置：`src/main/java/io/github/atengk/mail/model/MailTemplateRenderResult.java`

```java
package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 邮件模板渲染结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailTemplateRenderResult(
        String templateName,
        String htmlContent,
        LocalDateTime renderTime
) {

    /**
     * 创建模板渲染结果
     *
     * @param templateName 模板名称
     * @param htmlContent  HTML 内容
     * @return 模板渲染结果
     */
    public static MailTemplateRenderResult of(String templateName, String htmlContent) {
        return new MailTemplateRenderResult(templateName, htmlContent, LocalDateTime.now());
    }
}
```

下面是模板渲染服务完整实现。

文件位置：`src/main/java/io/github/atengk/mail/service/MailTemplateService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.model.MailTemplateRenderRequest;
import io.github.atengk.mail.model.MailTemplateRenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * 邮件模板服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(MailTemplateService.class);

    private final TemplateEngine templateEngine;

    public MailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 渲染邮件模板
     *
     * @param request 模板渲染请求
     * @return 模板渲染结果
     */
    public MailTemplateRenderResult render(MailTemplateRenderRequest request) {
        checkRenderRequest(request);

        Map<String, Object> variables = MapUtil.emptyIfNull(request.variables());
        Context context = new Context(Locale.CHINA);
        context.setVariables(variables);

        String htmlContent = templateEngine.process(request.templateName(), context);
        log.info("邮件模板渲染完成，模板名称：{}，变量数量：{}", request.templateName(), variables.size());

        return MailTemplateRenderResult.of(request.templateName(), htmlContent);
    }

    /**
     * 根据模板名称和变量渲染 HTML 内容
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return HTML 内容
     */
    public String renderHtml(String templateName, Map<String, Object> variables) {
        MailTemplateRenderRequest request = new MailTemplateRenderRequest(templateName, variables);
        return render(request).htmlContent();
    }

    /**
     * 校验模板渲染请求
     *
     * @param request 模板渲染请求
     */
    private void checkRenderRequest(MailTemplateRenderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板渲染请求不能为空");
        }

        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
    }
}
```

模板变量建议使用 `Map<String, Object>` 承载。对于简单场景，可以直接通过 Hutool 的 `MapUtil.builder()` 构建变量。

```java
Map<String, Object> variables = MapUtil.<String, Object>builder()
        .put("productName", "业务管理系统")
        .put("code", "836291")
        .put("expireMinutes", 5)
        .build();
```

对应模板中的变量写法如下：

```html
<h2 th:text="${productName}">业务管理系统</h2>
<span th:text="${code}">123456</span>
<span th:text="${expireMinutes}">5</span>
```

对于时间类变量，建议在 Java 代码中提前格式化后传入模板，避免模板文件承担过多逻辑。

```java
Map<String, Object> variables = MapUtil.<String, Object>builder()
        .put("approvalTitle", "采购申请审批")
        .put("applicantName", "张三")
        .put("applyTime", "2026-04-30 10:30:00")
        .put("approvalStatus", "待审批")
        .put("approvalUrl", "https://example.com/approval/10001")
        .build();
```

这样模板只负责展示，不负责复杂的数据转换。模板中可以直接使用：

```html
<td th:text="${applyTime}">2026-04-30 10:30:00</td>
```

模板变量管理需要注意以下几点：

| 注意项     | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 变量命名   | 使用清晰的业务名称，例如 `code`、`expireMinutes`、`approvalTitle` |
| 空值处理   | 关键变量应在业务层或模板服务层提前校验                       |
| 时间格式   | 建议 Java 侧格式化后传入模板                                 |
| 链接变量   | URL 应在业务层生成完整地址                                   |
| 敏感信息   | 不要将密码、授权码、Token 等敏感数据写入模板                 |
| HTML 转义  | 普通变量使用 `th:text`，避免 XSS 风险                        |
| 富文本内容 | 只有可信内容才使用 `th:utext`                                |

通常情况下，模板中优先使用 `th:text`。它会对 HTML 特殊字符进行转义，更安全。如果使用 `th:utext`，表示直接输出未转义 HTML，必须确保内容可信。

### 邮件内容组装

邮件内容组装用于把业务数据转换为最终可发送的邮件请求。推荐将“模板变量准备”和“邮件发送请求构建”放在业务服务中，将“模板渲染”放在 `MailTemplateService` 中，将“邮件发送”放在 `MailSendClient` 中。

推荐调用链路如下：

```text
业务 Service
    -> 组装模板变量 Map
    -> MailTemplateService 渲染 HTML 内容
    -> MailSendRequest 构建邮件请求
    -> MailSendClient 发送 HTML 邮件
```

如果沿用上一节 `MailSendClient.sendTemplateMail()` 的方式，也可以让 `MailSendClient` 内部完成模板渲染。但在业务复杂度增加后，推荐将模板渲染职责抽离到 `MailTemplateService`，这样更容易单独测试模板输出。

下面示例演示验证码邮件的内容组装与发送。

文件位置：`src/main/java/io/github/atengk/mail/service/MailContentAssembleService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 邮件内容组装服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailContentAssembleService {

    private static final Logger log = LoggerFactory.getLogger(MailContentAssembleService.class);

    private final MailTemplateService mailTemplateService;
    private final MailSendClient mailSendClient;

    public MailContentAssembleService(MailTemplateService mailTemplateService,
                                      MailSendClient mailSendClient) {
        this.mailTemplateService = mailTemplateService;
        this.mailSendClient = mailSendClient;
    }

    /**
     * 组装并发送验证码邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCodeMail(String receiver) {
        String code = RandomUtil.randomNumbers(6);

        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("productName", "业务管理系统")
                .put("code", code)
                .put("expireMinutes", 5)
                .build();

        String htmlContent = mailTemplateService.renderHtml("mail/verify-code", variables);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                "邮箱验证码",
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("验证码邮件组装并发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }

    /**
     * 组装并发送审批通知邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendApprovalNoticeMail(String receiver) {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("approvalTitle", "采购申请审批")
                .put("applicantName", "张三")
                .put("applyTime", "2026-04-30 10:30:00")
                .put("approvalStatus", "待审批")
                .put("approvalUrl", "https://example.com/approval/10001")
                .build();

        String htmlContent = mailTemplateService.renderHtml("mail/approval-notice", variables);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                "审批通知",
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("审批通知邮件组装并发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}
```

如果业务需要发送“模板邮件 + 附件”，可以先渲染模板内容，再构建附件邮件请求。

```java
File reportFile = FileUtil.file("/data/report/daily-report.xlsx");

Map<String, Object> variables = MapUtil.<String, Object>builder()
        .put("reportName", "每日业务报表")
        .put("reportDate", "2026-04-30")
        .build();

String htmlContent = mailTemplateService.renderHtml("mail/report-notice", variables);

MailSendRequest request = MailSendRequest.attachment(
        ListUtil.of(receiver),
        "每日业务报表",
        htmlContent,
        true,
        ListUtil.of(new MailAttachment("每日业务报表.xlsx", reportFile))
);

MailSendResult result = mailSendClient.sendAttachmentMail(request);
```

邮件内容组装建议遵循以下原则：

| 原则                      | 说明                                                     |
| ------------------------- | -------------------------------------------------------- |
| 业务数据在 Service 中准备 | 例如验证码、审批标题、申请人、详情链接                   |
| 模板只负责展示            | 不在模板中编写复杂业务判断                               |
| HTML 由模板服务渲染       | 避免业务代码拼接大段 HTML                                |
| 发送请求统一构建          | 通过 `MailSendRequest` 承载收件人、主题、正文和附件      |
| 发送动作统一入口          | 通过 `MailSendClient` 发送，便于日志、异常和记录统一处理 |

最终推荐的职责划分如下：

| 组件                         | 职责                                 |
| ---------------------------- | ------------------------------------ |
| `MailTemplateService`        | 根据模板名称和变量渲染 HTML          |
| `MailContentAssembleService` | 根据业务场景组装变量、主题和发送请求 |
| `MailSendClient`             | 执行实际邮件发送                     |
| `MailSendRequest`            | 统一承载邮件发送参数                 |
| `MailSendResult`             | 统一返回发送结果                     |

通过这种拆分，模板文件可以由前端、产品或后端共同维护，业务服务只关注邮件内容所需的数据，邮件发送客户端只关注发送能力本身。后续如果需要增加模板预览接口、模板变量校验、模板多语言支持或数据库化模板管理，也可以在当前结构上继续扩展。

继续补充“接口设计”部分，本节对应邮件发送接口、邮件预览接口和发送记录查询接口。该部分基于前面已经完成的邮件发送客户端、模板渲染服务和邮件内容组装逻辑继续展开。

## 接口设计

接口设计用于将邮件模块能力暴露给业务系统或后台管理端。当前接口主要包含三类：邮件发送接口、邮件预览接口和发送记录查询接口。

邮件发送接口用于触发真实发送；邮件预览接口用于在不发送邮件的情况下查看模板渲染结果；发送记录查询接口用于查看历史发送状态、收件人、主题、发送时间和错误原因。

本节先给出接口层代码和接口契约。发送记录的数据库表结构、状态维护和错误信息入库会在后续“邮件发送记录”章节中继续展开。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/mail/
├── controller/
│   └── MailController.java
├── model/
│   ├── MailSendApiRequest.java
│   ├── MailPreviewRequest.java
│   ├── MailRecordQueryRequest.java
│   ├── MailRecordVO.java
│   ├── MailSendType.java
│   ├── ApiResult.java
│   └── PageResult.java
├── service/
│   └── MailRecordQueryService.java
```

### 邮件发送接口

邮件发送接口用于接收业务系统或后台管理端提交的邮件发送请求，并调用 `MailSendClient` 完成实际发送。接口层不直接操作 SMTP，也不直接拼接 HTML，只负责参数接收、参数校验、日志记录和调用发送客户端。

接口路径建议如下：

| 接口名称 | 请求方式 | 接口路径         | 说明                      |
| -------- | -------- | ---------------- | ------------------------- |
| 发送邮件 | `POST`   | `/api/mail/send` | 发送文本、HTML 或模板邮件 |

先定义邮件类型枚举。

文件位置：`src/main/java/io/github/atengk/mail/model/MailSendType.java`

```java
package io.github.atengk.mail.model;

/**
 * 邮件发送类型
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum MailSendType {

    /**
     * 普通文本邮件
     */
    TEXT,

    /**
     * HTML 邮件
     */
    HTML,

    /**
     * 模板邮件
     */
    TEMPLATE
}
```

再定义统一接口响应对象。实际项目中如果已经存在统一响应结构，可以直接复用项目已有的 `Result`、`R` 或 `ApiResponse`。

文件位置：`src/main/java/io/github/atengk/mail/model/ApiResult.java`

```java
package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record ApiResult<T>(
        Integer code,
        String message,
        T data,
        LocalDateTime timestamp
) {

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 接口响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 返回失败结果
     *
     * @param message 失败信息
     * @return 接口响应
     */
    public static <T> ApiResult<T> failure(String message) {
        return new ApiResult<>(500, message, null, LocalDateTime.now());
    }
}
```

定义邮件发送接口请求对象。该对象支持文本邮件、HTML 邮件和模板邮件。附件邮件通常建议使用内部服务调用或 multipart 接口单独处理，本接口先不直接接收文件流，避免接口复杂度过高。

文件位置：`src/main/java/io/github/atengk/mail/model/MailSendApiRequest.java`

```java
package io.github.atengk.mail.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送接口请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendApiRequest(

        /**
         * 收件人列表
         */
        @NotEmpty(message = "收件人不能为空")
        List<@Email(message = "收件人邮箱格式不正确") String> receivers,

        /**
         * 抄送人列表
         */
        List<@Email(message = "抄送人邮箱格式不正确") String> ccList,

        /**
         * 密送人列表
         */
        List<@Email(message = "密送人邮箱格式不正确") String> bccList,

        /**
         * 邮件主题
         */
        @NotBlank(message = "邮件主题不能为空")
        String subject,

        /**
         * 邮件类型
         */
        @NotNull(message = "邮件类型不能为空")
        MailSendType type,

        /**
         * 邮件正文，TEXT 和 HTML 类型必填
         */
        String content,

        /**
         * 模板名称，TEMPLATE 类型必填
         */
        String templateName,

        /**
         * 模板变量
         */
        Map<String, Object> templateVariables
) {

    /**
     * 获取抄送人列表
     *
     * @return 抄送人列表
     */
    public List<String> safeCcList() {
        return ccList == null ? Collections.emptyList() : ccList;
    }

    /**
     * 获取密送人列表
     *
     * @return 密送人列表
     */
    public List<String> safeBccList() {
        return bccList == null ? Collections.emptyList() : bccList;
    }

    /**
     * 获取模板变量
     *
     * @return 模板变量
     */
    public Map<String, Object> safeTemplateVariables() {
        return templateVariables == null ? Collections.emptyMap() : templateVariables;
    }
}
```

下面是邮件接口 Controller 完整实现。该 Controller 复用前文的 `MailSendClient` 和 `MailTemplateService`。

文件位置：`src/main/java/io/github/atengk/mail/controller/MailController.java`

```java
package io.github.atengk.mail.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.ApiResult;
import io.github.atengk.mail.model.MailPreviewRequest;
import io.github.atengk.mail.model.MailRecordQueryRequest;
import io.github.atengk.mail.model.MailRecordVO;
import io.github.atengk.mail.model.MailSendApiRequest;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import io.github.atengk.mail.model.MailSendType;
import io.github.atengk.mail.model.PageResult;
import io.github.atengk.mail.service.MailRecordQueryService;
import io.github.atengk.mail.service.MailTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件接口控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Validated
@RestController
@RequestMapping("/api/mail")
public class MailController {

    private static final Logger log = LoggerFactory.getLogger(MailController.class);

    private final MailSendClient mailSendClient;
    private final MailTemplateService mailTemplateService;
    private final MailRecordQueryService mailRecordQueryService;

    public MailController(MailSendClient mailSendClient,
                          MailTemplateService mailTemplateService,
                          MailRecordQueryService mailRecordQueryService) {
        this.mailSendClient = mailSendClient;
        this.mailTemplateService = mailTemplateService;
        this.mailRecordQueryService = mailRecordQueryService;
    }

    /**
     * 发送邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send")
    public ApiResult<MailSendResult> send(@Valid @RequestBody MailSendApiRequest request) {
        log.info("收到邮件发送请求，类型：{}，收件人：{}，主题：{}",
                request.type(), request.receivers(), request.subject());

        MailSendResult result = switch (request.type()) {
            case TEXT -> sendTextMail(request);
            case HTML -> sendHtmlMail(request);
            case TEMPLATE -> sendTemplateMail(request);
        };

        if (result.success() || result.skipped()) {
            return ApiResult.success(result);
        }

        return ApiResult.failure(result.message());
    }

    /**
     * 预览邮件内容
     *
     * @param request 邮件预览请求
     * @return HTML 内容
     */
    @PostMapping("/preview")
    public ApiResult<String> preview(@Valid @RequestBody MailPreviewRequest request) {
        log.info("收到邮件预览请求，模板名称：{}", request.templateName());
        String htmlContent = mailTemplateService.renderHtml(request.templateName(), request.safeTemplateVariables());
        return ApiResult.success(htmlContent);
    }

    /**
     * 分页查询邮件发送记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/records/page")
    public ApiResult<PageResult<MailRecordVO>> pageRecords(@Valid @RequestBody MailRecordQueryRequest request) {
        log.info("收到邮件发送记录查询请求，页码：{}，每页条数：{}，收件人：{}，状态：{}",
                request.safePageNum(), request.safePageSize(), request.receiver(), request.status());

        PageResult<MailRecordVO> pageResult = mailRecordQueryService.page(request);
        return ApiResult.success(pageResult);
    }

    /**
     * 发送普通文本邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendTextMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("普通文本邮件正文不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                request.content(),
                false,
                null,
                null,
                null
        );

        return mailSendClient.sendTextMail(sendRequest);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendHtmlMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("HTML 邮件正文不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                request.content(),
                true,
                null,
                null,
                null
        );

        return mailSendClient.sendHtmlMail(sendRequest);
    }

    /**
     * 发送模板邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendTemplateMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                null,
                true,
                null,
                request.templateName(),
                request.safeTemplateVariables()
        );

        return mailSendClient.sendTemplateMail(sendRequest);
    }
}
```

邮件发送接口请求示例：

```json
{
  "receivers": [
    "user@example.com"
  ],
  "ccList": [],
  "bccList": [],
  "subject": "邮箱验证码",
  "type": "TEMPLATE",
  "templateName": "mail/verify-code",
  "templateVariables": {
    "productName": "业务管理系统",
    "code": "836291",
    "expireMinutes": 5
  }
}
```

接口调用示例：

```bash
curl -X POST 'http://localhost:8080/api/mail/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "receivers": ["user@example.com"],
    "subject": "邮箱验证码",
    "type": "TEMPLATE",
    "templateName": "mail/verify-code",
    "templateVariables": {
      "productName": "业务管理系统",
      "code": "836291",
      "expireMinutes": 5
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "skipped": false,
    "message": "邮件发送成功",
    "sendTime": "2026-04-30T10:30:00",
    "errorStack": null
  },
  "timestamp": "2026-04-30T10:30:00"
}
```

邮件发送接口需要注意以下几点：

| 注意项          | 说明                                                      |
| --------------- | --------------------------------------------------------- |
| `type=TEXT`     | `content` 按普通文本发送                                  |
| `type=HTML`     | `content` 按 HTML 内容发送                                |
| `type=TEMPLATE` | 使用 `templateName` 和 `templateVariables` 渲染模板后发送 |
| 附件邮件        | 建议通过内部 Service 调用或单独设计 multipart 接口        |
| 参数校验        | 接口层校验基础格式，发送客户端继续做业务校验              |
| 异常处理        | 后续可以通过全局异常处理统一返回错误结构                  |

### 邮件预览接口

邮件预览接口用于在不发送真实邮件的情况下渲染模板内容，便于后台管理端、测试人员或开发人员检查邮件模板最终展示效果。

接口路径建议如下：

| 接口名称     | 请求方式 | 接口路径            | 说明                             |
| ------------ | -------- | ------------------- | -------------------------------- |
| 预览邮件模板 | `POST`   | `/api/mail/preview` | 根据模板名称和变量渲染 HTML 内容 |

定义邮件预览请求对象。

文件位置：`src/main/java/io/github/atengk/mail/model/MailPreviewRequest.java`

```java
package io.github.atengk.mail.model;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.Map;

/**
 * 邮件预览请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailPreviewRequest(

        /**
         * 模板名称
         */
        @NotBlank(message = "模板名称不能为空")
        String templateName,

        /**
         * 模板变量
         */
        Map<String, Object> templateVariables
) {

    /**
     * 获取模板变量
     *
     * @return 模板变量
     */
    public Map<String, Object> safeTemplateVariables() {
        return templateVariables == null ? Collections.emptyMap() : templateVariables;
    }
}
```

邮件预览接口请求示例：

```json
{
  "templateName": "mail/approval-notice",
  "templateVariables": {
    "approvalTitle": "采购申请审批",
    "applicantName": "张三",
    "applyTime": "2026-04-30 10:30:00",
    "approvalStatus": "待审批",
    "approvalUrl": "https://example.com/approval/10001"
  }
}
```

接口调用示例：

```bash
curl -X POST 'http://localhost:8080/api/mail/preview' \
  -H 'Content-Type: application/json' \
  -d '{
    "templateName": "mail/approval-notice",
    "templateVariables": {
      "approvalTitle": "采购申请审批",
      "applicantName": "张三",
      "applyTime": "2026-04-30 10:30:00",
      "approvalStatus": "待审批",
      "approvalUrl": "https://example.com/approval/10001"
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "<!DOCTYPE html><html lang=\"zh-CN\">...</html>",
  "timestamp": "2026-04-30T10:30:00"
}
```

邮件预览接口通常用于以下场景：

| 场景     | 说明                               |
| -------- | ---------------------------------- |
| 开发调试 | 检查模板变量是否正确渲染           |
| 后台预览 | 管理端发送前预览邮件内容           |
| 测试验证 | 测试人员不发送真实邮件即可验证模板 |
| 模板维护 | 修改模板后快速检查展示效果         |

预览接口只返回渲染后的 HTML，不执行真实发送，也不写入发送记录。生产环境如果开放给后台用户使用，需要增加权限控制，避免任意用户读取或探测模板内容。

### 发送记录查询接口

发送记录查询接口用于分页查看邮件发送历史。常见查询条件包括收件人、邮件主题、发送状态、开始时间和结束时间。

接口路径建议如下：

| 接口名称     | 请求方式 | 接口路径                 | 说明                 |
| ------------ | -------- | ------------------------ | -------------------- |
| 查询发送记录 | `POST`   | `/api/mail/records/page` | 分页查询邮件发送记录 |

先定义分页结果对象。如果项目中已有统一分页对象，可以直接替换为项目已有实现。

文件位置：`src/main/java/io/github/atengk/mail/model/PageResult.java`

```java
package io.github.atengk.mail.model;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record PageResult<T>(
        Long total,
        Long pageNum,
        Long pageSize,
        List<T> records
) {

    /**
     * 返回空分页结果
     *
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(0L, pageNum, pageSize, Collections.emptyList());
    }

    /**
     * 返回分页结果
     *
     * @param total    总条数
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param records  数据列表
     * @return 分页结果
     */
    public static <T> PageResult<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records == null ? Collections.emptyList() : records);
    }
}
```

定义发送记录查询请求对象。

文件位置：`src/main/java/io/github/atengk/mail/model/MailRecordQueryRequest.java`

```java
package io.github.atengk.mail.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

/**
 * 邮件发送记录查询请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailRecordQueryRequest(

        /**
         * 当前页
         */
        @Min(value = 1, message = "页码不能小于 1")
        Long pageNum,

        /**
         * 每页条数
         */
        @Min(value = 1, message = "每页条数不能小于 1")
        @Max(value = 100, message = "每页条数不能大于 100")
        Long pageSize,

        /**
         * 收件人邮箱
         */
        String receiver,

        /**
         * 邮件主题
         */
        String subject,

        /**
         * 发送状态，例如 SUCCESS、FAIL、SKIPPED
         */
        String status,

        /**
         * 开始时间
         */
        LocalDateTime startTime,

        /**
         * 结束时间
         */
        LocalDateTime endTime
) {

    /**
     * 获取安全页码
     *
     * @return 页码
     */
    public Long safePageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    /**
     * 获取安全每页条数
     *
     * @return 每页条数
     */
    public Long safePageSize() {
        return pageSize == null ? 10L : pageSize;
    }
}
```

定义发送记录返回对象。

文件位置：`src/main/java/io/github/atengk/mail/model/MailRecordVO.java`

```java
package io.github.atengk.mail.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件发送记录返回对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailRecordVO(
        Long id,
        List<String> receivers,
        String subject,
        String mailType,
        String status,
        Boolean success,
        Boolean skipped,
        String errorMessage,
        LocalDateTime sendTime,
        LocalDateTime createTime
) {
}
```

下面先提供一个可运行的查询服务边界。该实现暂时返回空分页，用于保证接口层可以先完成联调。后续完成邮件发送记录表结构和 MyBatis-Plus 实现后，再替换该服务中的查询逻辑即可。

文件位置：`src/main/java/io/github/atengk/mail/service/MailRecordQueryService.java`

```java
package io.github.atengk.mail.service;

import io.github.atengk.mail.model.MailRecordQueryRequest;
import io.github.atengk.mail.model.MailRecordVO;
import io.github.atengk.mail.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 邮件发送记录查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailRecordQueryService {

    private static final Logger log = LoggerFactory.getLogger(MailRecordQueryService.class);

    /**
     * 分页查询邮件发送记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    public PageResult<MailRecordVO> page(MailRecordQueryRequest request) {
        log.warn("邮件发送记录持久化暂未接入，当前返回空分页结果，页码：{}，每页条数：{}",
                request.safePageNum(), request.safePageSize());

        return PageResult.empty(request.safePageNum(), request.safePageSize());
    }
}
```

发送记录查询接口请求示例：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "receiver": "user@example.com",
  "subject": "验证码",
  "status": "SUCCESS",
  "startTime": "2026-04-01T00:00:00",
  "endTime": "2026-04-30T23:59:59"
}
```

接口调用示例：

```bash
curl -X POST 'http://localhost:8080/api/mail/records/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "receiver": "user@example.com",
    "subject": "验证码",
    "status": "SUCCESS",
    "startTime": "2026-04-01T00:00:00",
    "endTime": "2026-04-30T23:59:59"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 0,
    "pageNum": 1,
    "pageSize": 10,
    "records": []
  },
  "timestamp": "2026-04-30T10:30:00"
}
```

后续接入数据库后，发送记录查询接口建议支持以下字段：

| 字段           | 说明         |
| -------------- | ------------ |
| `id`           | 发送记录主键 |
| `receivers`    | 收件人列表   |
| `subject`      | 邮件主题     |
| `mailType`     | 邮件类型     |
| `status`       | 发送状态     |
| `success`      | 是否发送成功 |
| `skipped`      | 是否跳过发送 |
| `errorMessage` | 失败原因     |
| `sendTime`     | 实际发送时间 |
| `createTime`   | 记录创建时间 |

接口设计完成后，邮件模块已经具备基础对外访问能力。邮件发送接口负责触发发送，邮件预览接口负责渲染模板内容，发送记录查询接口负责暴露历史记录查询入口。后续在“异步发送”和“邮件发送记录”章节中，可以继续扩展异步处理、失败记录、状态维护和数据库分页查询。

继续补充“异步发送”和“邮件发送记录”两部分内容。本节会把邮件发送从同步调用调整为“先创建发送记录，再异步发送，最后维护发送状态”的完整链路。

## 异步发送

异步发送用于避免邮件发送阻塞主业务流程。SMTP 发送受网络、邮箱服务商、附件大小和服务商限流影响，耗时不可控。如果在用户注册、找回密码、审批提交等接口中同步发送邮件，容易拉长接口响应时间，甚至因为 SMTP 超时导致主业务失败。

推荐做法是：接口或业务服务只负责创建发送任务和发送记录，然后交给异步线程池执行真实发送。发送成功、失败或跳过后，再更新邮件发送记录。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/mail/
├── async/
│   ├── MailAsyncSendService.java
│   └── MailAsyncSendServiceImpl.java
├── config/
│   └── MailAsyncConfig.java
├── model/
│   └── MailRecordStatus.java
├── record/
│   ├── entity/
│   │   └── MailRecord.java
│   ├── mapper/
│   │   └── MailRecordMapper.java
│   └── service/
│       ├── MailRecordService.java
│       └── MailRecordServiceImpl.java
```

### 异步线程池配置

异步线程池用于承载邮件发送任务。邮件发送属于典型的 IO 型任务，主要耗时在网络连接、SMTP 认证、附件上传和服务商响应上，因此线程数可以适当高于 CPU 核心数，但不建议无限制放大。

建议为邮件模块单独配置线程池，不要直接使用 Spring 默认异步线程池。这样可以避免邮件发送任务占满公共线程池，影响其他异步任务。

文件位置：`src/main/java/io/github/atengk/mail/config/MailAsyncConfig.java`

```java
package io.github.atengk.mail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件异步线程池配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@EnableAsync
@Configuration
public class MailAsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MailAsyncConfig.class);

    /**
     * 邮件发送线程池
     *
     * @return 邮件发送线程池
     */
    @Bean("mailTaskExecutor")
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数，保持少量常驻线程处理日常邮件
        executor.setCorePoolSize(4);

        // 最大线程数，邮件发送高峰期最多扩展到 12 个线程
        executor.setMaxPoolSize(12);

        // 队列容量，避免瞬时大量邮件直接打满线程
        executor.setQueueCapacity(500);

        // 线程空闲存活时间，单位秒
        executor.setKeepAliveSeconds(60);

        // 线程名前缀，便于日志排查
        executor.setThreadNamePrefix("mail-send-");

        // 队列满时由提交任务的线程执行，避免任务直接丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 应用关闭时等待异步任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最大等待时间，单位秒
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 默认异步执行器
     *
     * @return 异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return mailTaskExecutor();
    }

    /**
     * 异步异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new MailAsyncExceptionHandler();
    }

    /**
     * 邮件异步异常处理器
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static class MailAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 处理异步异常
         *
         * @param throwable 异常对象
         * @param method    方法对象
         * @param objects   方法参数
         */
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
            log.error("邮件异步任务执行异常，方法：{}，原因：{}", method.getName(), throwable.getMessage(), throwable);
        }
    }
}
```

线程池参数建议根据业务量调整：

| 参数               | 建议                                       |
| ------------------ | ------------------------------------------ |
| `corePoolSize`     | 日常邮件量较小时可设置为 `2` 到 `4`        |
| `maxPoolSize`      | 不建议过大，避免短时间内触发邮箱服务商限流 |
| `queueCapacity`    | 根据系统允许堆积的邮件任务数量设置         |
| `CallerRunsPolicy` | 队列满时由调用线程执行，避免任务直接丢弃   |
| `threadNamePrefix` | 必须设置，便于日志中快速识别邮件发送线程   |

需要注意，`@Async` 方法必须通过 Spring Bean 代理调用才会生效。不要在同一个类内部直接调用自己的异步方法，否则异步代理不会执行。

### 异步发送实现

异步发送实现建议遵循以下流程：

```text
业务请求
    -> 创建邮件发送记录，状态为 PENDING
    -> 提交异步发送任务
    -> 立即返回 recordId 或提交结果
    -> 异步线程更新状态为 SENDING
    -> 调用 MailSendClient 发送邮件
    -> 根据结果更新为 SUCCESS / FAIL / SKIPPED
```

先定义邮件发送状态枚举。

文件位置：`src/main/java/io/github/atengk/mail/model/MailRecordStatus.java`

```java
package io.github.atengk.mail.model;

/**
 * 邮件发送记录状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum MailRecordStatus {

    /**
     * 待发送
     */
    PENDING,

    /**
     * 发送中
     */
    SENDING,

    /**
     * 发送成功
     */
    SUCCESS,

    /**
     * 发送失败
     */
    FAIL,

    /**
     * 跳过发送
     */
    SKIPPED
}
```

异步发送服务接口用于对外提供异步提交能力。

文件位置：`src/main/java/io/github/atengk/mail/async/MailAsyncSendService.java`

```java
package io.github.atengk.mail.async;

import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件异步发送服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MailAsyncSendService {

    /**
     * 异步发送普通文本邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    CompletableFuture<MailSendResult> sendTextAsync(Long recordId, MailSendRequest request);

    /**
     * 异步发送 HTML 邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    CompletableFuture<MailSendResult> sendHtmlAsync(Long recordId, MailSendRequest request);

    /**
     * 异步发送模板邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    CompletableFuture<MailSendResult> sendTemplateAsync(Long recordId, MailSendRequest request);

    /**
     * 异步发送附件邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    CompletableFuture<MailSendResult> sendAttachmentAsync(Long recordId, MailSendRequest request);
}
```

异步发送实现类负责调用 `MailSendClient`，并在发送前后维护发送记录状态。

文件位置：`src/main/java/io/github/atengk/mail/async/MailAsyncSendServiceImpl.java`

```java
package io.github.atengk.mail.async;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import io.github.atengk.mail.record.service.MailRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件异步发送服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailAsyncSendServiceImpl implements MailAsyncSendService {

    private static final Logger log = LoggerFactory.getLogger(MailAsyncSendServiceImpl.class);

    private final MailSendClient mailSendClient;
    private final MailRecordService mailRecordService;

    public MailAsyncSendServiceImpl(MailSendClient mailSendClient,
                                    MailRecordService mailRecordService) {
        this.mailSendClient = mailSendClient;
        this.mailRecordService = mailRecordService;
    }

    /**
     * 异步发送普通文本邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    @Async("mailTaskExecutor")
    @Override
    public CompletableFuture<MailSendResult> sendTextAsync(Long recordId, MailSendRequest request) {
        return executeAsync(recordId, request, () -> mailSendClient.sendTextMail(request));
    }

    /**
     * 异步发送 HTML 邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    @Async("mailTaskExecutor")
    @Override
    public CompletableFuture<MailSendResult> sendHtmlAsync(Long recordId, MailSendRequest request) {
        return executeAsync(recordId, request, () -> mailSendClient.sendHtmlMail(request));
    }

    /**
     * 异步发送模板邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    @Async("mailTaskExecutor")
    @Override
    public CompletableFuture<MailSendResult> sendTemplateAsync(Long recordId, MailSendRequest request) {
        return executeAsync(recordId, request, () -> mailSendClient.sendTemplateMail(request));
    }

    /**
     * 异步发送附件邮件
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @return 异步发送结果
     */
    @Async("mailTaskExecutor")
    @Override
    public CompletableFuture<MailSendResult> sendAttachmentAsync(Long recordId, MailSendRequest request) {
        return executeAsync(recordId, request, () -> mailSendClient.sendAttachmentMail(request));
    }

    /**
     * 执行异步发送
     *
     * @param recordId 发送记录 ID
     * @param request  邮件发送请求
     * @param sender   邮件发送动作
     * @return 异步发送结果
     */
    private CompletableFuture<MailSendResult> executeAsync(Long recordId,
                                                           MailSendRequest request,
                                                           MailSenderAction sender) {
        try {
            mailRecordService.markSending(recordId);
            log.info("邮件异步发送开始，记录ID：{}，收件人：{}，主题：{}",
                    recordId, request.receivers(), request.subject());

            MailSendResult result = sender.send();

            if (result.success()) {
                mailRecordService.markSuccess(recordId, result.message(), result.sendTime());
                log.info("邮件异步发送成功，记录ID：{}，主题：{}", recordId, request.subject());
            } else if (result.skipped()) {
                mailRecordService.markSkipped(recordId, result.message(), result.sendTime());
                log.warn("邮件异步发送跳过，记录ID：{}，原因：{}", recordId, result.message());
            } else {
                mailRecordService.markFail(recordId, result.message(), result.errorStack(), result.sendTime());
                log.error("邮件异步发送失败，记录ID：{}，原因：{}", recordId, result.message());
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception exception) {
            String errorStack = ExceptionUtil.stacktraceToString(exception);
            mailRecordService.markFail(recordId, exception.getMessage(), errorStack, null);
            log.error("邮件异步发送异常，记录ID：{}，原因：{}", recordId, exception.getMessage(), exception);

            MailSendResult result = MailSendResult.failure("邮件异步发送异常：" + exception.getMessage(), errorStack);
            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * 邮件发送动作
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @FunctionalInterface
    private interface MailSenderAction {

        /**
         * 执行发送
         *
         * @return 邮件发送结果
         */
        MailSendResult send();
    }
}
```

业务层提交异步发送时，建议先创建记录，再调用异步服务。

文件位置：`src/main/java/io/github/atengk/mail/service/MailSubmitService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.mail.async.MailAsyncSendService;
import io.github.atengk.mail.model.MailRecordStatus;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.record.entity.MailRecord;
import io.github.atengk.mail.record.service.MailRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 邮件提交服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailSubmitService {

    private static final Logger log = LoggerFactory.getLogger(MailSubmitService.class);

    private final MailRecordService mailRecordService;
    private final MailAsyncSendService mailAsyncSendService;

    public MailSubmitService(MailRecordService mailRecordService,
                             MailAsyncSendService mailAsyncSendService) {
        this.mailRecordService = mailRecordService;
        this.mailAsyncSendService = mailAsyncSendService;
    }

    /**
     * 提交验证码模板邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送记录 ID
     */
    public Long submitVerifyCodeMail(String receiver) {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("productName", "业务管理系统")
                .put("code", "836291")
                .put("expireMinutes", 5)
                .build();

        MailSendRequest request = MailSendRequest.template(
                ListUtil.of(receiver),
                "邮箱验证码",
                "mail/verify-code",
                variables
        );

        MailRecord record = mailRecordService.createPending(request, "TEMPLATE", MailRecordStatus.PENDING);
        mailAsyncSendService.sendTemplateAsync(record.getId(), request);

        log.info("验证码邮件已提交异步发送，记录ID：{}，收件人：{}", record.getId(), receiver);
        return record.getId();
    }
}
```

接口层如果希望立即返回提交结果，而不是等待 SMTP 发送完成，可以改为调用 `MailSubmitService`。

请求成功后返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "recordId": 10001,
    "status": "PENDING",
    "message": "邮件已提交异步发送"
  },
  "timestamp": "2026-04-30T10:30:00"
}
```

这种方式更适合生产环境。前端或调用方可以通过发送记录查询接口查看最终发送状态。

### 发送失败处理

发送失败处理用于保证邮件发送失败时可追踪、可排查、可补偿。失败原因可能来自参数错误、SMTP 认证失败、网络超时、附件不存在、附件过大、邮箱服务商限流、收件人地址不可达等。

失败处理建议分为三层：

| 层级       | 处理方式                                |
| ---------- | --------------------------------------- |
| 发送客户端 | 捕获异常，返回 `MailSendResult.failure` |
| 异步服务   | 根据返回结果更新记录状态为 `FAIL`       |
| 记录服务   | 保存错误摘要和异常堆栈，便于后续排查    |

常见失败类型如下：

| 失败类型      | 常见原因                           | 处理建议               |
| ------------- | ---------------------------------- | ---------------------- |
| 参数校验失败  | 收件人为空、邮箱格式错误、主题为空 | 直接返回失败，不重试   |
| SMTP 认证失败 | 账号或授权码错误                   | 告警并检查配置，不重试 |
| 连接超时      | 网络异常、端口不通、服务商不可用   | 可重试                 |
| 附件异常      | 文件不存在、文件过大、权限不足     | 修复文件后重试         |
| 服务商限流    | 发送频率过高                       | 延迟重试或降低频率     |
| 邮箱不可达    | 收件人不存在、被拒收               | 记录失败，通常不重试   |

失败处理的核心代码已经在 `MailAsyncSendServiceImpl` 中体现：

```java
try {
    mailRecordService.markSending(recordId);
    MailSendResult result = sender.send();

    if (result.success()) {
        mailRecordService.markSuccess(recordId, result.message(), result.sendTime());
    } else if (result.skipped()) {
        mailRecordService.markSkipped(recordId, result.message(), result.sendTime());
    } else {
        mailRecordService.markFail(recordId, result.message(), result.errorStack(), result.sendTime());
    }
} catch (Exception exception) {
    String errorStack = ExceptionUtil.stacktraceToString(exception);
    mailRecordService.markFail(recordId, exception.getMessage(), errorStack, null);
}
```

建议错误信息记录时拆分为两个字段：

| 字段            | 作用                           |
| --------------- | ------------------------------ |
| `error_message` | 保存简短失败原因，适合列表展示 |
| `error_stack`   | 保存完整异常堆栈，适合开发排查 |

不要在日志或数据库中记录邮箱授权码、SMTP 密码、Token 等敏感信息。异常堆栈中如果包含敏感配置，需要在统一异常处理或记录前进行脱敏。

## 邮件发送记录

邮件发送记录用于追踪邮件从提交到最终发送完成的全过程。没有发送记录时，邮件发送失败很难排查，也无法确认某个用户是否收到过某类通知。

发送记录建议至少覆盖以下能力：

| 能力         | 说明                                               |
| ------------ | -------------------------------------------------- |
| 记录提交信息 | 收件人、主题、邮件类型、模板名称、请求参数         |
| 维护发送状态 | `PENDING`、`SENDING`、`SUCCESS`、`FAIL`、`SKIPPED` |
| 记录失败原因 | 保存错误摘要和异常堆栈                             |
| 支持分页查询 | 供后台管理端或运维排查使用                         |
| 支持后续重试 | 失败记录可以作为重试依据                           |

### 表结构设计

邮件发送记录表建议使用单表存储。收件人、抄送人、密送人和模板变量可以使用 JSON 字符串保存，方便排查原始发送内容。对于大规模邮件系统，可以后续拆分为发送批次表和收件人明细表。

下面以 MySQL 为例设计表结构。

文件位置：`sql/mail_record.sql`

```sql
CREATE TABLE mail_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    receivers_json TEXT NOT NULL COMMENT '收件人列表JSON',
    cc_json TEXT NULL COMMENT '抄送人列表JSON',
    bcc_json TEXT NULL COMMENT '密送人列表JSON',

    subject VARCHAR(255) NOT NULL COMMENT '邮件主题',
    mail_type VARCHAR(32) NOT NULL COMMENT '邮件类型：TEXT、HTML、TEMPLATE、ATTACHMENT',
    template_name VARCHAR(255) NULL COMMENT '模板名称',
    template_variables_json TEXT NULL COMMENT '模板变量JSON',

    content_summary VARCHAR(1000) NULL COMMENT '邮件内容摘要',
    status VARCHAR(32) NOT NULL COMMENT '发送状态：PENDING、SENDING、SUCCESS、FAIL、SKIPPED',

    success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否发送成功',
    skipped TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否跳过发送',

    error_message VARCHAR(1000) NULL COMMENT '错误摘要',
    error_stack LONGTEXT NULL COMMENT '异常堆栈',

    submit_time DATETIME NOT NULL COMMENT '提交时间',
    send_time DATETIME NULL COMMENT '发送完成时间',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    INDEX idx_mail_record_status (status),
    INDEX idx_mail_record_send_time (send_time),
    INDEX idx_mail_record_create_time (create_time),
    INDEX idx_mail_record_subject (subject)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '邮件发送记录表';
```

如果使用 MySQL 5.7，`utf8mb4_0900_ai_ci` 可能不支持，可以改为：

```sql
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
```

如果发送量较大，建议增加以下优化：

| 优化项           | 说明                                     |
| ---------------- | ---------------------------------------- |
| 按时间归档       | 定期归档 3 到 6 个月前的发送记录         |
| 拆分收件人明细   | 批量邮件可拆分为批次表和收件人表         |
| 限制内容摘要长度 | 不建议保存完整 HTML 正文到主表           |
| 错误堆栈单独存储 | 大规模系统可将堆栈放到日志平台或对象存储 |
| 增加业务字段     | 可增加 `biz_type`、`biz_id` 关联业务单据 |

下面是邮件发送记录实体。

文件位置：`src/main/java/io/github/atengk/mail/record/entity/MailRecord.java`

```java
package io.github.atengk.mail.record.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件发送记录实体
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@TableName("mail_record")
public class MailRecord {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 收件人列表 JSON
     */
    private String receiversJson;

    /**
     * 抄送人列表 JSON
     */
    private String ccJson;

    /**
     * 密送人列表 JSON
     */
    private String bccJson;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件类型
     */
    private String mailType;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板变量 JSON
     */
    private String templateVariablesJson;

    /**
     * 邮件内容摘要
     */
    private String contentSummary;

    /**
     * 发送状态
     */
    private String status;

    /**
     * 是否发送成功
     */
    private Boolean success;

    /**
     * 是否跳过发送
     */
    private Boolean skipped;

    /**
     * 错误摘要
     */
    private String errorMessage;

    /**
     * 异常堆栈
     */
    private String errorStack;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 发送完成时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

Mapper 文件如下。

文件位置：`src/main/java/io/github/atengk/mail/record/mapper/MailRecordMapper.java`

```java
package io.github.atengk.mail.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.mail.record.entity.MailRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送记录 Mapper
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Mapper
public interface MailRecordMapper extends BaseMapper<MailRecord> {
}
```

如果项目没有配置 MyBatis-Plus Mapper 扫描，需要在启动类或配置类中添加：

文件位置：`src/main/java/io/github/atengk/Application.java`

```java
package io.github.atengk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 应用启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@MapperScan("io.github.atengk.mail.record.mapper")
@ConfigurationPropertiesScan
@SpringBootApplication
public class Application {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 发送状态维护

发送状态维护用于保证每一条邮件记录都能清楚表达当前状态。建议状态流转如下：

```text
PENDING
   |
   v
SENDING
   |
   |----> SUCCESS
   |
   |----> FAIL
   |
   |----> SKIPPED
```

状态含义如下：

| 状态      | 说明                                     |
| --------- | ---------------------------------------- |
| `PENDING` | 已创建发送记录，但尚未开始发送           |
| `SENDING` | 异步线程已开始执行发送                   |
| `SUCCESS` | 邮件发送成功                             |
| `FAIL`    | 邮件发送失败                             |
| `SKIPPED` | 因发送开关关闭、测试白名单等原因跳过发送 |

下面定义发送记录服务接口。

文件位置：`src/main/java/io/github/atengk/mail/record/service/MailRecordService.java`

```java
package io.github.atengk.mail.record.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.mail.model.MailRecordStatus;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.record.entity.MailRecord;

import java.time.LocalDateTime;

/**
 * 邮件发送记录服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MailRecordService extends IService<MailRecord> {

    /**
     * 创建待发送记录
     *
     * @param request 邮件发送请求
     * @param mailType 邮件类型
     * @param status 初始状态
     * @return 邮件发送记录
     */
    MailRecord createPending(MailSendRequest request, String mailType, MailRecordStatus status);

    /**
     * 标记发送中
     *
     * @param recordId 发送记录 ID
     */
    void markSending(Long recordId);

    /**
     * 标记发送成功
     *
     * @param recordId 发送记录 ID
     * @param message  结果信息
     * @param sendTime 发送时间
     */
    void markSuccess(Long recordId, String message, LocalDateTime sendTime);

    /**
     * 标记跳过发送
     *
     * @param recordId 发送记录 ID
     * @param message  结果信息
     * @param sendTime 发送时间
     */
    void markSkipped(Long recordId, String message, LocalDateTime sendTime);

    /**
     * 标记发送失败
     *
     * @param recordId     发送记录 ID
     * @param errorMessage 错误摘要
     * @param errorStack   异常堆栈
     * @param sendTime     发送时间
     */
    void markFail(Long recordId, String errorMessage, String errorStack, LocalDateTime sendTime);
}
```

下面是发送记录服务实现。

文件位置：`src/main/java/io/github/atengk/mail/record/service/MailRecordServiceImpl.java`

```java
package io.github.atengk.mail.record.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.mail.model.MailRecordStatus;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.record.entity.MailRecord;
import io.github.atengk.mail.record.mapper.MailRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 邮件发送记录服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailRecordServiceImpl extends ServiceImpl<MailRecordMapper, MailRecord> implements MailRecordService {

    private static final Logger log = LoggerFactory.getLogger(MailRecordServiceImpl.class);

    /**
     * 创建待发送记录
     *
     * @param request  邮件发送请求
     * @param mailType 邮件类型
     * @param status   初始状态
     * @return 邮件发送记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MailRecord createPending(MailSendRequest request, String mailType, MailRecordStatus status) {
        LocalDateTime now = LocalDateTime.now();

        MailRecord record = new MailRecord();
        record.setReceiversJson(JSONUtil.toJsonStr(CollUtil.emptyIfNull(request.receivers())));
        record.setCcJson(JSONUtil.toJsonStr(CollUtil.emptyIfNull(request.ccList())));
        record.setBccJson(JSONUtil.toJsonStr(CollUtil.emptyIfNull(request.bccList())));
        record.setSubject(request.subject());
        record.setMailType(mailType);
        record.setTemplateName(request.templateName());
        record.setTemplateVariablesJson(JSONUtil.toJsonStr(request.templateVariables()));
        record.setContentSummary(buildContentSummary(request.content()));
        record.setStatus(status.name());
        record.setSuccess(false);
        record.setSkipped(false);
        record.setSubmitTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);

        save(record);
        log.info("邮件发送记录创建完成，记录ID：{}，状态：{}，主题：{}", record.getId(), record.getStatus(), record.getSubject());

        return record;
    }

    /**
     * 标记发送中
     *
     * @param recordId 发送记录 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markSending(Long recordId) {
        MailRecord record = requireRecord(recordId);
        record.setStatus(MailRecordStatus.SENDING.name());
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("邮件发送记录状态更新为发送中，记录ID：{}", recordId);
    }

    /**
     * 标记发送成功
     *
     * @param recordId 发送记录 ID
     * @param message  结果信息
     * @param sendTime 发送时间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markSuccess(Long recordId, String message, LocalDateTime sendTime) {
        MailRecord record = requireRecord(recordId);
        record.setStatus(MailRecordStatus.SUCCESS.name());
        record.setSuccess(true);
        record.setSkipped(false);
        record.setErrorMessage(null);
        record.setErrorStack(null);
        record.setSendTime(sendTime == null ? LocalDateTime.now() : sendTime);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.info("邮件发送记录状态更新为成功，记录ID：{}，结果：{}", recordId, message);
    }

    /**
     * 标记跳过发送
     *
     * @param recordId 发送记录 ID
     * @param message  结果信息
     * @param sendTime 发送时间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markSkipped(Long recordId, String message, LocalDateTime sendTime) {
        MailRecord record = requireRecord(recordId);
        record.setStatus(MailRecordStatus.SKIPPED.name());
        record.setSuccess(false);
        record.setSkipped(true);
        record.setErrorMessage(buildShortText(message, 1000));
        record.setErrorStack(null);
        record.setSendTime(sendTime == null ? LocalDateTime.now() : sendTime);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.warn("邮件发送记录状态更新为跳过，记录ID：{}，原因：{}", recordId, message);
    }

    /**
     * 标记发送失败
     *
     * @param recordId     发送记录 ID
     * @param errorMessage 错误摘要
     * @param errorStack   异常堆栈
     * @param sendTime     发送时间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markFail(Long recordId, String errorMessage, String errorStack, LocalDateTime sendTime) {
        MailRecord record = requireRecord(recordId);
        record.setStatus(MailRecordStatus.FAIL.name());
        record.setSuccess(false);
        record.setSkipped(false);
        record.setErrorMessage(buildShortText(errorMessage, 1000));
        record.setErrorStack(errorStack);
        record.setSendTime(sendTime == null ? LocalDateTime.now() : sendTime);
        record.setUpdateTime(LocalDateTime.now());

        updateById(record);
        log.error("邮件发送记录状态更新为失败，记录ID：{}，原因：{}", recordId, errorMessage);
    }

    /**
     * 获取发送记录
     *
     * @param recordId 发送记录 ID
     * @return 邮件发送记录
     */
    private MailRecord requireRecord(Long recordId) {
        if (recordId == null) {
            throw new IllegalArgumentException("邮件发送记录ID不能为空");
        }

        MailRecord record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("邮件发送记录不存在：" + recordId);
        }

        return record;
    }

    /**
     * 构建邮件内容摘要
     *
     * @param content 邮件正文
     * @return 内容摘要
     */
    private String buildContentSummary(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }

        String cleanContent = StrUtil.cleanBlank(content);
        return buildShortText(cleanContent, 1000);
    }

    /**
     * 构建短文本
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 短文本
     */
    private String buildShortText(String text, int maxLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        return text.length() <= maxLength ? text : StrUtil.subPre(text, maxLength);
    }
}
```

这样，发送记录的状态更新集中在 `MailRecordService` 中完成，异步服务只负责根据发送结果调用对应的状态维护方法。

前面“发送记录查询接口”中的 `MailRecordQueryService` 可以替换为真实分页查询实现。

文件位置：`src/main/java/io/github/atengk/mail/service/MailRecordQueryService.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.mail.model.MailRecordQueryRequest;
import io.github.atengk.mail.model.MailRecordVO;
import io.github.atengk.mail.model.PageResult;
import io.github.atengk.mail.record.entity.MailRecord;
import io.github.atengk.mail.record.service.MailRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 邮件发送记录查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailRecordQueryService {

    private static final Logger log = LoggerFactory.getLogger(MailRecordQueryService.class);

    private final MailRecordService mailRecordService;

    public MailRecordQueryService(MailRecordService mailRecordService) {
        this.mailRecordService = mailRecordService;
    }

    /**
     * 分页查询邮件发送记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    public PageResult<MailRecordVO> page(MailRecordQueryRequest request) {
        Page<MailRecord> page = Page.of(request.safePageNum(), request.safePageSize());

        LambdaQueryWrapper<MailRecord> wrapper = new LambdaQueryWrapper<MailRecord>()
                .like(StrUtil.isNotBlank(request.receiver()), MailRecord::getReceiversJson, request.receiver())
                .like(StrUtil.isNotBlank(request.subject()), MailRecord::getSubject, request.subject())
                .eq(StrUtil.isNotBlank(request.status()), MailRecord::getStatus, request.status())
                .ge(Objects.nonNull(request.startTime()), MailRecord::getCreateTime, request.startTime())
                .le(Objects.nonNull(request.endTime()), MailRecord::getCreateTime, request.endTime())
                .orderByDesc(MailRecord::getCreateTime);

        Page<MailRecord> resultPage = mailRecordService.page(page, wrapper);
        List<MailRecordVO> records = resultPage.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        log.info("邮件发送记录分页查询完成，页码：{}，每页条数：{}，总数：{}",
                request.safePageNum(), request.safePageSize(), resultPage.getTotal());

        return PageResult.of(resultPage.getTotal(), request.safePageNum(), request.safePageSize(), records);
    }

    /**
     * 转换发送记录视图对象
     *
     * @param record 邮件发送记录
     * @return 发送记录视图对象
     */
    private MailRecordVO convertToVO(MailRecord record) {
        return new MailRecordVO(
                record.getId(),
                parseStringList(record.getReceiversJson()),
                record.getSubject(),
                record.getMailType(),
                record.getStatus(),
                record.getSuccess(),
                record.getSkipped(),
                record.getErrorMessage(),
                record.getSendTime(),
                record.getCreateTime()
        );
    }

    /**
     * 解析字符串列表
     *
     * @param json JSON 字符串
     * @return 字符串列表
     */
    private List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return CollUtil.newArrayList();
        }

        return JSONUtil.toList(json, String.class);
    }
}
```

如果项目还没有配置 MyBatis-Plus 分页插件，需要补充分页插件配置。

文件位置：`src/main/java/io/github/atengk/mail/config/MybatisPlusConfig.java`

```java
package io.github.atengk.mail.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // MySQL 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

### 错误信息记录

错误信息记录用于保存邮件发送失败的具体原因。建议错误信息分为“摘要”和“详情”两类：摘要用于列表展示，详情用于开发排查。

表字段设计如下：

| 字段            | 类型            | 说明                       |
| --------------- | --------------- | -------------------------- |
| `error_message` | `VARCHAR(1000)` | 错误摘要，适合页面展示     |
| `error_stack`   | `LONGTEXT`      | 完整异常堆栈，适合开发排查 |

错误记录策略建议如下：

| 场景           | `error_message`    | `error_stack`        |
| -------------- | ------------------ | -------------------- |
| 参数校验失败   | 保存参数错误原因   | 可为空或保存异常堆栈 |
| SMTP 认证失败  | 保存认证失败摘要   | 保存完整异常堆栈     |
| 网络超时       | 保存连接超时摘要   | 保存完整异常堆栈     |
| 附件不存在     | 保存文件不存在路径 | 保存完整异常堆栈     |
| 发送开关关闭   | 保存跳过原因       | 不需要保存堆栈       |
| 测试白名单拦截 | 保存拦截原因       | 不需要保存堆栈       |

错误信息不应直接暴露给普通用户。后台管理端可以展示 `error_message`，完整 `error_stack` 建议只对开发、运维或管理员开放。

下面是发送失败后的记录示例。

```json
{
  "id": 10001,
  "receivers": [
    "user@example.com"
  ],
  "subject": "邮箱验证码",
  "mailType": "TEMPLATE",
  "status": "FAIL",
  "success": false,
  "skipped": false,
  "errorMessage": "邮件发送失败：Authentication failed",
  "sendTime": "2026-04-30T10:30:05",
  "createTime": "2026-04-30T10:30:00"
}
```

后台查询时，只展示摘要即可：

```text
发送状态：FAIL
失败原因：邮件发送失败：Authentication failed
发送时间：2026-04-30 10:30:05
```

日志中则保留完整异常，便于定位：

```text
邮件异步发送失败，记录ID：10001，原因：邮件发送失败：Authentication failed
```

完整异常堆栈已经由 `MailRecordService.markFail()` 写入 `error_stack`，并由 `MailAsyncSendServiceImpl` 输出到日志中。

生产环境建议补充以下安全处理：

| 安全项       | 说明                                              |
| ------------ | ------------------------------------------------- |
| 授权码脱敏   | 不允许在日志和数据库中出现邮箱授权码              |
| 邮箱脱敏     | 面向普通用户展示时可脱敏，例如 `u***@example.com` |
| 堆栈权限控制 | 只有管理员或开发人员可查看完整堆栈                |
| 错误摘要截断 | 避免异常信息过长影响列表查询                      |
| 定期清理     | 定期清理历史失败堆栈，降低数据量和敏感信息风险    |

完成本节后，邮件模块已经具备生产环境中更常用的发送方式：主流程提交任务，异步线程执行真实发送，发送记录表完整保存状态、时间、错误摘要和异常堆栈。后续可以在此基础上继续扩展失败重试、发送频率控制、消息队列投递和定时补偿任务。

继续补充“异常处理与日志”和“安全与配置管理”两部分内容。本节用于完善邮件模块在生产环境中的异常可控性、日志可追踪性和敏感配置安全性。

## 异常处理与日志

异常处理与日志用于保证邮件发送失败时能够快速定位原因，同时避免把底层异常、敏感配置或完整堆栈直接暴露给前端用户。

邮件发送涉及外部 SMTP 服务、网络连接、授权码、附件文件、模板渲染、数据库记录和异步线程池，因此异常来源较多。建议在模块内部使用统一业务异常封装，在接口层通过全局异常处理器返回统一响应，在日志中保留关键排查信息。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/mail/
├── exception/
│   ├── MailErrorCode.java
│   ├── MailException.java
│   └── GlobalExceptionHandler.java
├── util/
│   └── MailSensitiveUtil.java
```

### 常见异常类型

邮件模块常见异常可以按照“参数异常、配置异常、模板异常、附件异常、SMTP 异常、异步异常、数据库异常”进行分类。不同异常的处理方式不同，不能全部简单归为系统错误。

常见异常类型如下：

| 异常类型      | 常见原因                                     | 建议处理方式                     |
| ------------- | -------------------------------------------- | -------------------------------- |
| 参数异常      | 收件人为空、邮箱格式错误、主题为空、正文为空 | 直接返回参数错误，不重试         |
| 配置异常      | SMTP 地址为空、发件账号为空、授权码错误      | 记录错误日志，通知运维检查配置   |
| 模板异常      | 模板文件不存在、模板变量缺失、模板语法错误   | 返回模板处理失败，记录模板名称   |
| 附件异常      | 附件不存在、附件过大、文件无读取权限         | 返回附件处理失败，不建议自动重试 |
| SMTP 认证异常 | 邮箱账号或授权码错误、账号被禁用             | 记录错误并告警，不建议自动重试   |
| SMTP 连接异常 | 网络不通、端口被封、服务商不可用             | 可以按策略重试                   |
| SMTP 限流异常 | 短时间发送过多、服务商拒绝                   | 降低频率或延迟重试               |
| 异步执行异常  | 线程池拒绝、异步方法内部异常                 | 写入失败记录并记录日志           |
| 数据库异常    | 发送记录保存失败、状态更新失败               | 记录日志，必要时进行补偿         |

建议定义统一错误码，便于接口返回和日志检索。

文件位置：`src/main/java/io/github/atengk/mail/exception/MailErrorCode.java`

```java
package io.github.atengk.mail.exception;

/**
 * 邮件错误码
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum MailErrorCode {

    /**
     * 邮件参数错误
     */
    MAIL_PARAM_ERROR("MAIL_PARAM_ERROR", "邮件参数错误"),

    /**
     * 邮件配置错误
     */
    MAIL_CONFIG_ERROR("MAIL_CONFIG_ERROR", "邮件配置错误"),

    /**
     * 邮件模板错误
     */
    MAIL_TEMPLATE_ERROR("MAIL_TEMPLATE_ERROR", "邮件模板错误"),

    /**
     * 邮件附件错误
     */
    MAIL_ATTACHMENT_ERROR("MAIL_ATTACHMENT_ERROR", "邮件附件错误"),

    /**
     * 邮件发送失败
     */
    MAIL_SEND_ERROR("MAIL_SEND_ERROR", "邮件发送失败"),

    /**
     * 邮件记录错误
     */
    MAIL_RECORD_ERROR("MAIL_RECORD_ERROR", "邮件记录错误"),

    /**
     * 邮件异步任务错误
     */
    MAIL_ASYNC_ERROR("MAIL_ASYNC_ERROR", "邮件异步任务错误"),

    /**
     * 系统内部错误
     */
    SYSTEM_ERROR("SYSTEM_ERROR", "系统内部错误");

    private final String code;

    private final String message;

    MailErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getMessage() {
        return message;
    }
}
```

错误码设计建议保持稳定，不要频繁变更。前端、日志平台和监控告警可以基于错误码进行统计和筛选。

### 统一异常封装

统一异常封装用于将邮件模块内部错误转换为可控的业务异常。这样 Controller 不需要到处捕获底层异常，也可以避免直接把 `MailSendException`、`MessagingException`、`TemplateInputException` 等底层异常返回给调用方。

先定义邮件模块业务异常。

文件位置：`src/main/java/io/github/atengk/mail/exception/MailException.java`

```java
package io.github.atengk.mail.exception;

/**
 * 邮件业务异常
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class MailException extends RuntimeException {

    /**
     * 错误码
     */
    private final MailErrorCode errorCode;

    /**
     * 创建邮件业务异常
     *
     * @param errorCode 错误码
     */
    public MailException(MailErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建邮件业务异常
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public MailException(MailErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建邮件业务异常
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原始异常
     */
    public MailException(MailErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public MailErrorCode getErrorCode() {
        return errorCode;
    }
}
```

在邮件模块中，建议用 `MailException` 替代直接抛出 `IllegalArgumentException`。例如参数校验可以调整为：

```java
if (CollUtil.isEmpty(request.receivers())) {
    throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件收件人不能为空");
}

if (StrUtil.isBlank(request.subject())) {
    throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件主题不能为空");
}
```

模板渲染异常可以封装为：

```java
try {
    return templateEngine.process(templateName, context);
} catch (Exception exception) {
    throw new MailException(MailErrorCode.MAIL_TEMPLATE_ERROR, "邮件模板渲染失败：" + templateName, exception);
}
```

发送异常可以封装为：

```java
try {
    javaMailSender.send(mimeMessage);
} catch (Exception exception) {
    throw new MailException(MailErrorCode.MAIL_SEND_ERROR, "邮件发送失败", exception);
}
```

为了让接口返回统一结构，需要增加全局异常处理器。这里复用前面接口设计中定义的 `ApiResult`。

文件位置：`src/main/java/io/github/atengk/mail/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.mail.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.model.ApiResult;
import io.github.atengk.mail.util.MailSensitiveUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理邮件业务异常
     *
     * @param exception 邮件业务异常
     * @return 接口响应
     */
    @ExceptionHandler(MailException.class)
    public ApiResult<Void> handleMailException(MailException exception) {
        String safeMessage = MailSensitiveUtil.maskSensitiveText(exception.getMessage());
        log.warn("邮件业务异常，错误码：{}，原因：{}",
                exception.getErrorCode().getCode(), safeMessage, exception);

        return ApiResult.failure(safeMessage);
    }

    /**
     * 处理请求参数校验异常
     *
     * @param exception 参数校验异常
     * @return 接口响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = CollUtil.isEmpty(fieldErrors)
                ? "请求参数校验失败"
                : fieldErrors.getFirst().getDefaultMessage();

        log.warn("请求参数校验失败，原因：{}", message);
        return ApiResult.failure(message);
    }

    /**
     * 处理绑定异常
     *
     * @param exception 绑定异常
     * @return 接口响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = CollUtil.isEmpty(fieldErrors)
                ? "请求参数绑定失败"
                : fieldErrors.getFirst().getDefaultMessage();

        log.warn("请求参数绑定失败，原因：{}", message);
        return ApiResult.failure(message);
    }

    /**
     * 处理约束校验异常
     *
     * @param exception 约束校验异常
     * @return 接口响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        String message = violations.stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("请求参数校验失败");

        log.warn("请求参数约束校验失败，原因：{}", message);
        return ApiResult.failure(message);
    }

    /**
     * 处理 SMTP 认证异常
     *
     * @param exception SMTP 认证异常
     * @return 接口响应
     */
    @ExceptionHandler(MailAuthenticationException.class)
    public ApiResult<Void> handleMailAuthenticationException(MailAuthenticationException exception) {
        log.error("SMTP 认证失败，请检查邮箱账号或授权码配置，原因：{}", exception.getMessage(), exception);
        return ApiResult.failure("SMTP 认证失败，请检查邮箱账号或授权码配置");
    }

    /**
     * 处理邮件发送异常
     *
     * @param exception 邮件发送异常
     * @return 接口响应
     */
    @ExceptionHandler(MailSendException.class)
    public ApiResult<Void> handleMailSendException(MailSendException exception) {
        String safeMessage = MailSensitiveUtil.maskSensitiveText(exception.getMessage());
        log.error("邮件发送异常，原因：{}", safeMessage, exception);
        return ApiResult.failure("邮件发送失败，请稍后重试");
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 接口响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        String message = StrUtil.blankToDefault(exception.getMessage(), "请求参数错误");
        log.warn("非法参数异常，原因：{}", message);
        return ApiResult.failure(message);
    }

    /**
     * 处理系统未知异常
     *
     * @param exception 异常对象
     * @return 接口响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        String safeMessage = MailSensitiveUtil.maskSensitiveText(exception.getMessage());
        log.error("系统异常，原因：{}", safeMessage, exception);
        return ApiResult.failure("系统繁忙，请稍后重试");
    }
}
```

上面的异常处理器有几个关键点：

| 处理点        | 说明                                   |
| ------------- | -------------------------------------- |
| 业务异常      | 返回具体业务错误信息                   |
| 参数异常      | 返回字段校验失败原因                   |
| SMTP 认证异常 | 提示检查账号或授权码，但不返回敏感内容 |
| 邮件发送异常  | 对前端返回通用失败，对日志保留堆栈     |
| 未知异常      | 统一返回系统繁忙，避免泄露内部实现     |

如果项目已有统一全局异常处理器，不建议重复创建多个 `@RestControllerAdvice`。可以将邮件异常处理逻辑合并到项目已有异常处理器中。

### 关键日志打印

邮件模块日志需要做到“能排查问题，但不泄露敏感信息”。关键日志应该覆盖发送请求、模板渲染、发送结果、状态流转、失败原因和异步执行情况。

建议日志分布如下：

| 位置          | 日志级别                  | 日志内容                    |
| ------------- | ------------------------- | --------------------------- |
| 接口入口      | `info`                    | 邮件类型、收件人、主题      |
| 参数校验失败  | `warn`                    | 参数错误原因                |
| 模板渲染成功  | `info`                    | 模板名称、变量数量          |
| 发送开始      | `info`                    | 记录 ID、收件人、主题       |
| 发送成功      | `info`                    | 记录 ID、主题、发送时间     |
| 跳过发送      | `warn`                    | 跳过原因                    |
| 发送失败      | `error`                   | 记录 ID、失败摘要、异常堆栈 |
| 状态更新      | `info` / `warn` / `error` | 状态流转结果                |
| SMTP 认证失败 | `error`                   | 提示检查配置，不打印授权码  |

为了统一处理日志脱敏，建议提供敏感信息处理工具。

文件位置：`src/main/java/io/github/atengk/mail/util/MailSensitiveUtil.java`

```java
package io.github.atengk.mail.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 邮件敏感信息工具
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class MailSensitiveUtil {

    private static final String EMAIL_PATTERN = "([a-zA-Z0-9._%+-]{1,64})@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})";

    private static final String PASSWORD_PATTERN = "(?i)(password|pwd|secret|token|authorization|auth-code|authCode)\\s*[=:]\\s*([^,;\\s]+)";

    private MailSensitiveUtil() {
    }

    /**
     * 脱敏邮箱地址
     *
     * @param email 邮箱地址
     * @return 脱敏后的邮箱地址
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }

        return DesensitizedUtil.email(email);
    }

    /**
     * 脱敏敏感文本
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public static String maskSensitiveText(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }

        String maskedEmailText = ReUtil.replaceAll(text, EMAIL_PATTERN, matcher -> {
            String fullEmail = matcher.group(0);
            return maskEmail(fullEmail);
        });

        return ReUtil.replaceAll(maskedEmailText, PASSWORD_PATTERN, matcher -> matcher.group(1) + "=******");
    }
}
```

在日志中打印收件人时，可以先脱敏。

```java
String safeReceiver = MailSensitiveUtil.maskEmail(receiver);
log.info("验证码邮件发送完成，收件人：{}，结果：{}", safeReceiver, result.message());
```

如果是收件人列表，可以批量脱敏。

```java
List<String> safeReceivers = request.receivers()
        .stream()
        .map(MailSensitiveUtil::maskEmail)
        .toList();

log.info("邮件发送开始，收件人：{}，主题：{}", safeReceivers, request.subject());
```

关键日志建议遵循以下原则：

| 原则               | 说明                                      |
| ------------------ | ----------------------------------------- |
| 不打印授权码       | `spring.mail.password` 绝不能出现在日志中 |
| 不打印完整敏感参数 | Token、密码、授权码、Cookie 需要脱敏      |
| 邮箱按场景脱敏     | 后台管理可展示完整邮箱，普通日志建议脱敏  |
| 保留 recordId      | 发送链路必须能通过记录 ID 串起来          |
| 错误日志保留堆栈   | `error` 日志应输出异常对象                |
| 成功日志保持简洁   | 不要打印完整 HTML 正文和模板变量          |

推荐日志链路示例：

```text
收到邮件发送请求，类型：TEMPLATE，收件人：[u***@example.com]，主题：邮箱验证码
邮件发送记录创建完成，记录ID：10001，状态：PENDING，主题：邮箱验证码
邮件异步发送开始，记录ID：10001，收件人：[u***@example.com]，主题：邮箱验证码
邮件模板渲染完成，模板名称：mail/verify-code，变量数量：3
邮件发送成功，收件人：[u***@example.com]，主题：邮箱验证码
邮件发送记录状态更新为成功，记录ID：10001，结果：邮件发送成功
```

## 安全与配置管理

安全与配置管理用于保护邮箱账号、授权码、SMTP 配置和收件人数据。邮件模块通常会接触用户邮箱、业务通知内容、系统告警信息和发件账号凭证，因此需要避免敏感信息泄露。

生产环境中，最重要的安全要求是：授权码不入库、不入 Git、不打印日志、不返回前端。

### 邮箱授权码管理

邮箱授权码是系统连接 SMTP 服务的凭证，安全级别等同于密码。大多数邮箱服务商不建议应用系统直接使用邮箱登录密码，而是使用授权码、应用专用密码或客户端密码。

授权码管理建议如下：

| 项目     | 建议                                                    |
| -------- | ------------------------------------------------------- |
| 发件账号 | 使用系统专用邮箱，例如 `notice@example.com`             |
| 授权方式 | 使用授权码或应用专用密码，不使用个人登录密码            |
| 保存位置 | 使用环境变量、配置中心、Kubernetes Secret 或 CI/CD 密钥 |
| 代码仓库 | 禁止提交真实授权码                                      |
| 日志输出 | 禁止打印授权码                                          |
| 权限范围 | 只有部署人员或配置管理员可见                            |
| 轮换策略 | 定期更换授权码，人员变动后立即更换                      |
| 异常处理 | 授权失败时只提示检查配置，不返回授权码内容              |

本地开发可以使用环境变量注入：

```bash
# SMTP 服务器地址
export MAIL_HOST=smtp.qq.com

# SMTP 端口
export MAIL_PORT=465

# 发件邮箱账号
export MAIL_USERNAME=your-email@qq.com

# 邮箱授权码或应用专用密码
export MAIL_PASSWORD=your-mail-auth-code

# 启动项目
mvn spring-boot:run
```

Jar 包启动时可以使用环境变量，而不是直接把密码写在命令行参数中。

```bash
export MAIL_HOST=smtp.exmail.qq.com
export MAIL_PORT=465
export MAIL_USERNAME=notice@example.com
export MAIL_PASSWORD=********

java -jar app.jar
```

配置文件中只保留占位符。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  mail:
    # SMTP 服务器地址，从环境变量读取
    host: ${MAIL_HOST}

    # SMTP 端口，从环境变量读取，默认 465
    port: ${MAIL_PORT:465}

    # 发件邮箱账号，从环境变量读取
    username: ${MAIL_USERNAME}

    # 邮箱授权码，从环境变量读取
    password: ${MAIL_PASSWORD}

    # 默认编码
    default-encoding: UTF-8
```

不建议使用以下写法：

```yaml
spring:
  mail:
    username: real-user@example.com
    password: real-password-or-auth-code
```

这种配置一旦提交到 Git 仓库或被打包进镜像，就会带来明显的凭证泄露风险。

如果使用 Docker 部署，可以通过环境变量传入。

```bash
docker run -d \
  --name mail-service \
  -e MAIL_HOST=smtp.exmail.qq.com \
  -e MAIL_PORT=465 \
  -e MAIL_USERNAME=notice@example.com \
  -e MAIL_PASSWORD=******** \
  -p 8080:8080 \
  mail-service:latest
```

上述命令用于在容器启动时注入 SMTP 配置。实际生产环境中，`MAIL_PASSWORD` 建议由容器平台 Secret 或 CI/CD 密钥变量提供，不建议直接写在脚本文件中。

### 敏感配置隔离

敏感配置隔离用于区分不同环境的配置来源，避免开发、测试、生产环境共用同一个发件账号或授权码。

推荐按照环境拆分配置文件：

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

公共配置放在 `application.yml`，不同环境只覆盖必要差异。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 默认使用 dev 环境，生产环境通过启动参数覆盖
    active: dev

  mail:
    # 默认编码
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          # 开启 SMTP 认证
          auth: true

          # 连接超时时间，单位毫秒
          connectiontimeout: 10000

          # 读取超时时间，单位毫秒
          timeout: 10000

          # 写入超时时间，单位毫秒
          writetimeout: 10000
```

开发环境可以关闭真实发送，或限制测试收件人白名单。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  mail:
    # 开发环境 SMTP 地址
    host: ${MAIL_HOST:smtp.qq.com}

    # 开发环境 SMTP 端口
    port: ${MAIL_PORT:465}

    # 开发环境发件账号
    username: ${MAIL_USERNAME:dev-notice@example.com}

    # 开发环境授权码仍然建议从环境变量读取
    password: ${MAIL_PASSWORD:}

    properties:
      mail:
        smtp:
          ssl:
            # 465 端口通常开启 SSL
            enable: true

mail:
  biz:
    # 开发环境可以关闭真实发送
    enabled: false

    # 开发环境限制收件人，避免误发真实用户
    test-receiver-whitelist:
      - test@example.com
      - admin@example.com
```

测试环境可以开启真实发送，但只允许发送到测试白名单。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          ssl:
            # 测试环境使用 SSL 发送
            enable: true

mail:
  biz:
    # 测试环境开启发送
    enabled: true

    # 测试环境必须配置白名单
    test-receiver-whitelist:
      - qa@example.com
      - developer@example.com
```

生产环境必须从环境变量或配置中心读取敏感配置，不允许配置默认授权码。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  mail:
    # 生产环境必须显式提供 SMTP 地址
    host: ${MAIL_HOST}

    # 生产环境必须显式提供 SMTP 端口
    port: ${MAIL_PORT}

    # 生产环境必须显式提供发件邮箱账号
    username: ${MAIL_USERNAME}

    # 生产环境必须显式提供授权码
    password: ${MAIL_PASSWORD}

    properties:
      mail:
        smtp:
          ssl:
            # 465 端口启用 SSL
            enable: true

mail:
  biz:
    # 生产环境开启真实发送
    enabled: true

    # 生产环境不建议配置测试白名单
    test-receiver-whitelist: []
```

启动生产环境时，使用 profile 指定配置：

```bash
java -jar app.jar --spring.profiles.active=prod
```

如果使用 Kubernetes，可以通过 Secret 注入环境变量。

文件位置：`k8s/mail-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mail-secret
type: Opaque
stringData:
  # SMTP 服务器地址
  MAIL_HOST: smtp.exmail.qq.com

  # SMTP 服务端口
  MAIL_PORT: "465"

  # 发件邮箱账号
  MAIL_USERNAME: notice@example.com

  # 邮箱授权码或应用专用密码
  MAIL_PASSWORD: replace-with-real-secret
```

文件位置：`k8s/mail-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mail-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mail-service
  template:
    metadata:
      labels:
        app: mail-service
    spec:
      containers:
        - name: mail-service
          image: mail-service:latest
          ports:
            - containerPort: 8080
          envFrom:
            # 从 Secret 中注入邮件敏感配置
            - secretRef:
                name: mail-secret
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
```

敏感配置隔离建议遵循以下规则：

| 规则                 | 说明                                |
| -------------------- | ----------------------------------- |
| 默认配置不放真实密码 | `application.yml` 只写占位符        |
| 生产环境无默认密码   | `${MAIL_PASSWORD}` 不设置默认值     |
| 测试环境配置白名单   | 防止测试邮件误发真实用户            |
| 日志不打印配置值     | 启动日志只打印开关和非敏感状态      |
| 密钥由平台管理       | 使用 Secret、配置中心或 CI/CD 密钥  |
| 不把 `.env` 提交仓库 | 本地 `.env` 文件应加入 `.gitignore` |

### 收件人参数校验

收件人参数校验用于防止错误邮箱、空邮箱、超量收件人和非白名单收件人在系统中流转。邮件一旦发送到外部邮箱，就不可撤回，因此参数校验必须尽量前置。

建议校验范围包括：

| 校验项         | 说明                                      |
| -------------- | ----------------------------------------- |
| 收件人不能为空 | `receivers` 至少包含一个邮箱              |
| 邮箱格式正确   | 使用 `Validator.isEmail` 或 `@Email` 校验 |
| 收件人数量限制 | 防止一次发送过多收件人                    |
| 抄送密送校验   | `ccList`、`bccList` 也必须校验邮箱格式    |
| 测试白名单     | 非生产环境限制收件人范围                  |
| 重复邮箱处理   | 可以去重，避免重复发送                    |
| 空格清理       | 邮箱前后空格需要清理                      |
| 黑名单拦截     | 可选，拦截禁止发送的邮箱域名或地址        |

建议在接口层和发送客户端都做校验。接口层负责基础格式校验，发送客户端负责最终业务校验，避免绕过接口直接调用 Service 时出现风险。

下面是一个独立的收件人校验工具。

文件位置：`src/main/java/io/github/atengk/mail/util/MailReceiverValidator.java`

```java
package io.github.atengk.mail.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.config.MailBizProperties;
import io.github.atengk.mail.exception.MailErrorCode;
import io.github.atengk.mail.exception.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 邮件收件人校验工具
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class MailReceiverValidator {

    private static final Logger log = LoggerFactory.getLogger(MailReceiverValidator.class);

    private MailReceiverValidator() {
    }

    /**
     * 校验并规范化收件人
     *
     * @param receivers         收件人列表
     * @param ccList            抄送人列表
     * @param bccList           密送人列表
     * @param mailBizProperties 邮件业务配置
     * @return 所有规范化后的邮箱
     */
    public static List<String> validateAndNormalize(List<String> receivers,
                                                    List<String> ccList,
                                                    List<String> bccList,
                                                    MailBizProperties mailBizProperties) {
        if (CollUtil.isEmpty(receivers)) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件收件人不能为空");
        }

        List<String> normalizedReceivers = normalizeEmails(receivers);
        List<String> normalizedCcList = normalizeEmails(ccList);
        List<String> normalizedBccList = normalizeEmails(bccList);

        List<String> allEmails = new ArrayList<>();
        allEmails.addAll(normalizedReceivers);
        allEmails.addAll(normalizedCcList);
        allEmails.addAll(normalizedBccList);

        checkEmailFormat(allEmails);
        checkReceiverCount(allEmails, mailBizProperties);
        checkWhitelist(allEmails, mailBizProperties);

        log.info("邮件收件人校验完成，收件人数：{}，抄送人数：{}，密送人数：{}",
                normalizedReceivers.size(), normalizedCcList.size(), normalizedBccList.size());

        return allEmails;
    }

    /**
     * 规范化邮箱列表
     *
     * @param emails 邮箱列表
     * @return 规范化后的邮箱列表
     */
    public static List<String> normalizeEmails(List<String> emails) {
        if (CollUtil.isEmpty(emails)) {
            return CollUtil.newArrayList();
        }

        Set<String> emailSet = new LinkedHashSet<>();
        for (String email : emails) {
            String normalizedEmail = StrUtil.trim(email);
            if (StrUtil.isNotBlank(normalizedEmail)) {
                emailSet.add(normalizedEmail);
            }
        }

        return new ArrayList<>(emailSet);
    }

    /**
     * 校验邮箱格式
     *
     * @param emails 邮箱列表
     */
    private static void checkEmailFormat(List<String> emails) {
        List<String> invalidEmails = emails.stream()
                .filter(email -> !Validator.isEmail(email))
                .toList();

        if (CollUtil.isNotEmpty(invalidEmails)) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮箱格式不正确：" + invalidEmails);
        }
    }

    /**
     * 校验收件人数量
     *
     * @param emails            邮箱列表
     * @param mailBizProperties 邮件业务配置
     */
    private static void checkReceiverCount(List<String> emails, MailBizProperties mailBizProperties) {
        Integer maxReceiverCount = mailBizProperties.getMaxReceiverCount();
        if (maxReceiverCount != null && maxReceiverCount > 0 && emails.size() > maxReceiverCount) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件收件人数超过限制：" + maxReceiverCount);
        }
    }

    /**
     * 校验测试白名单
     *
     * @param emails            邮箱列表
     * @param mailBizProperties 邮件业务配置
     */
    private static void checkWhitelist(List<String> emails, MailBizProperties mailBizProperties) {
        List<String> whitelist = mailBizProperties.getTestReceiverWhitelist();
        if (CollUtil.isEmpty(whitelist)) {
            return;
        }

        List<String> blockedEmails = emails.stream()
                .filter(email -> !whitelist.contains(email))
                .toList();

        if (CollUtil.isNotEmpty(blockedEmails)) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "收件人不在测试白名单中：" + blockedEmails);
        }
    }
}
```

在 `MailSendClient` 中可以调用该工具替代原来的收件人校验逻辑。

```java
MailReceiverValidator.validateAndNormalize(
        request.receivers(),
        request.ccList(),
        request.bccList(),
        mailBizProperties
);
```

接口层也应保留 Bean Validation 校验，例如：

```java
@NotEmpty(message = "收件人不能为空")
List<@Email(message = "收件人邮箱格式不正确") String> receivers;
```

这样可以形成两层防护：

| 层级                | 作用                                 |
| ------------------- | ------------------------------------ |
| Controller DTO 校验 | 快速拦截明显错误请求                 |
| MailSendClient 校验 | 防止内部服务绕过接口直接调用导致风险 |

收件人参数校验还可以进一步扩展黑名单能力，例如禁止向某些域名发送邮件。

```java
List<String> blockedDomains = List.of("@example-test.com", "@invalid.local");

boolean hasBlockedDomain = emails.stream()
        .anyMatch(email -> blockedDomains.stream().anyMatch(email::endsWith));

if (hasBlockedDomain) {
    throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "收件人包含禁止发送的邮箱域名");
}
```

生产环境建议至少启用以下安全策略：

| 策略             | 说明                                  |
| ---------------- | ------------------------------------- |
| 最大收件人数限制 | 防止一次请求发送过多邮件              |
| 非生产白名单     | 测试环境只能发送到指定邮箱            |
| 邮箱格式校验     | 收件人、抄送人、密送人都必须校验      |
| 日志邮箱脱敏     | 普通日志不打印完整邮箱                |
| 失败记录可追踪   | 参数错误和发送失败都应进入日志或记录  |
| 敏感内容不返回   | 接口错误信息不包含授权码、密码、Token |

完成本节后，邮件模块已经具备较完整的异常处理和安全配置能力。后续“测试与验证”和“部署与运维”章节可以基于这些能力继续验证接口、SMTP 连通性、模板渲染、异步发送、发送记录和生产环境配置。

继续补充“测试与验证”和“部署与运维”两部分内容。本节用于说明邮件模块如何进行单元测试、接口测试、真实送达验证，以及在不同环境中的部署配置、频率控制和常见问题排查。

## 测试与验证

测试与验证用于确认邮件模块的配置、模板、接口、异步发送和 SMTP 投递链路是否正常。邮件功能依赖外部邮箱服务，不能只依赖代码层面的单元测试，还需要结合接口测试、测试邮箱收件验证和发送记录状态验证。

建议测试分为三层：

| 测试类型 | 目标                                               |
| -------- | -------------------------------------------------- |
| 单元测试 | 验证参数校验、模板渲染、内容组装等内部逻辑         |
| 接口测试 | 验证 Controller 入参、响应结构、异常处理和调用链路 |
| 送达验证 | 验证 SMTP 配置、邮箱授权码、真实投递和垃圾箱情况   |

### 单元测试

单元测试主要用于验证邮件模块内部逻辑，例如收件人校验、模板渲染、发送请求构建和异常分支处理。单元测试不建议直接依赖真实 SMTP 服务，否则测试稳定性会受网络和邮箱服务商影响。

测试依赖建议使用 Spring Boot 默认测试依赖。

文件位置：`pom.xml`

```xml
<dependency>
    <!-- Spring Boot 测试依赖，包含 JUnit 5、Mockito、AssertJ、MockMvc 等测试能力 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

先测试收件人校验逻辑。该测试不需要启动完整 Spring 容器，执行速度快，适合覆盖参数校验分支。

文件位置：`src/test/java/io/github/atengk/mail/util/MailReceiverValidatorTest.java`

```java
package io.github.atengk.mail.util;

import cn.hutool.core.collection.ListUtil;
import io.github.atengk.mail.config.MailBizProperties;
import io.github.atengk.mail.exception.MailException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 邮件收件人校验工具测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class MailReceiverValidatorTest {

    /**
     * 校验合法收件人
     */
    @Test
    void shouldValidateSuccessWhenEmailIsValid() {
        MailBizProperties properties = new MailBizProperties();
        properties.setMaxReceiverCount(10);

        List<String> emails = MailReceiverValidator.validateAndNormalize(
                ListUtil.of(" user@example.com "),
                ListUtil.of("cc@example.com"),
                ListUtil.of("bcc@example.com"),
                properties
        );

        Assertions.assertEquals(3, emails.size());
        Assertions.assertTrue(emails.contains("user@example.com"));
    }

    /**
     * 校验非法邮箱格式
     */
    @Test
    void shouldThrowExceptionWhenEmailIsInvalid() {
        MailBizProperties properties = new MailBizProperties();

        MailException exception = Assertions.assertThrows(
                MailException.class,
                () -> MailReceiverValidator.validateAndNormalize(
                        ListUtil.of("invalid-email"),
                        null,
                        null,
                        properties
                )
        );

        Assertions.assertTrue(exception.getMessage().contains("邮箱格式不正确"));
    }

    /**
     * 校验测试白名单拦截
     */
    @Test
    void shouldThrowExceptionWhenReceiverNotInWhitelist() {
        MailBizProperties properties = new MailBizProperties();
        properties.setTestReceiverWhitelist(ListUtil.of("test@example.com"));

        MailException exception = Assertions.assertThrows(
                MailException.class,
                () -> MailReceiverValidator.validateAndNormalize(
                        ListUtil.of("user@example.com"),
                        null,
                        null,
                        properties
                )
        );

        Assertions.assertTrue(exception.getMessage().contains("收件人不在测试白名单中"));
    }
}
```

再测试模板渲染逻辑。该测试需要加载 Spring 容器和 Thymeleaf 模板引擎。

文件位置：`src/test/java/io/github/atengk/mail/service/MailTemplateServiceTest.java`

```java
package io.github.atengk.mail.service;

import cn.hutool.core.map.MapUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 邮件模板服务测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootTest
class MailTemplateServiceTest {

    @Autowired
    private MailTemplateService mailTemplateService;

    /**
     * 测试验证码模板渲染
     */
    @Test
    void shouldRenderVerifyCodeTemplate() {
        String htmlContent = mailTemplateService.renderHtml(
                "mail/verify-code",
                MapUtil.<String, Object>builder()
                        .put("productName", "业务管理系统")
                        .put("code", "836291")
                        .put("expireMinutes", 5)
                        .build()
        );

        Assertions.assertNotNull(htmlContent);
        Assertions.assertTrue(htmlContent.contains("业务管理系统"));
        Assertions.assertTrue(htmlContent.contains("836291"));
    }
}
```

如果测试环境不希望真实发送邮件，可以在 `src/test/resources/application-test.yml` 中关闭真实发送。

文件位置：`src/test/resources/application-test.yml`

```yaml
mail:
  biz:
    # 单元测试环境关闭真实发送，避免误发邮件
    enabled: false

    # 单元测试环境限制收件人范围
    test-receiver-whitelist:
      - test@example.com
```

运行测试命令如下：

```bash
# 使用 test profile 执行测试
mvn test -Dspring.profiles.active=test
```

该命令会执行项目中的单元测试和 Spring Boot 测试。测试阶段建议关闭真实邮件发送，只验证模板渲染、参数校验和业务逻辑。

### 接口测试

接口测试用于验证邮件模块对外接口是否符合预期，包括请求参数校验、响应结构、异常返回和 Controller 调用逻辑。接口测试可以使用 MockMvc，不需要启动真实 HTTP 端口。

下面是邮件发送接口测试示例。测试中通过 Mock 模拟 `MailSendClient`，避免真实连接 SMTP 服务。

文件位置：`src/test/java/io/github/atengk/mail/controller/MailControllerTest.java`

```java
package io.github.atengk.mail.controller;

import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendResult;
import io.github.atengk.mail.service.MailRecordQueryService;
import io.github.atengk.mail.service.MailTemplateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 邮件接口控制器测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@WebMvcTest(MailController.class)
class MailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailSendClient mailSendClient;

    @MockitoBean
    private MailTemplateService mailTemplateService;

    @MockitoBean
    private MailRecordQueryService mailRecordQueryService;

    /**
     * 测试发送文本邮件接口
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldSendTextMailSuccess() throws Exception {
        Mockito.when(mailSendClient.sendTextMail(any()))
                .thenReturn(MailSendResult.success("邮件发送成功"));

        String requestBody = """
                {
                  "receivers": ["test@example.com"],
                  "subject": "测试文本邮件",
                  "type": "TEXT",
                  "content": "这是一封测试文本邮件"
                }
                """;

        mockMvc.perform(post("/api/mail/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("操作成功")))
                .andExpect(jsonPath("$.data.success", is(true)))
                .andExpect(jsonPath("$.data.message", is("邮件发送成功")));
    }

    /**
     * 测试邮箱格式错误
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnFailureWhenEmailInvalid() throws Exception {
        String requestBody = """
                {
                  "receivers": ["invalid-email"],
                  "subject": "测试文本邮件",
                  "type": "TEXT",
                  "content": "这是一封测试文本邮件"
                }
                """;

        mockMvc.perform(post("/api/mail/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$.code", is(500)));
    }

    /**
     * 测试模板预览接口
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldPreviewTemplateSuccess() throws Exception {
        Mockito.when(mailTemplateService.renderHtml(Mockito.eq("mail/verify-code"), any()))
                .thenReturn("<html><body>836291</body></html>");

        String requestBody = """
                {
                  "templateName": "mail/verify-code",
                  "templateVariables": {
                    "code": "836291",
                    "expireMinutes": 5
                  }
                }
                """;

        mockMvc.perform(post("/api/mail/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is("<html><body>836291</body></html>")));
    }
}
```

如果当前项目使用的 Spring Boot 版本低于 3.4，`@MockitoBean` 不可用，可以改为 `@MockBean`。

```java
@MockBean
private MailSendClient mailSendClient;
```

使用 curl 进行手动接口测试时，可以先测试模板预览接口。预览接口不会真实发送邮件，适合确认模板变量是否正确。

```bash
curl -X POST 'http://localhost:8080/api/mail/preview' \
  -H 'Content-Type: application/json' \
  -d '{
    "templateName": "mail/verify-code",
    "templateVariables": {
      "productName": "业务管理系统",
      "code": "836291",
      "expireMinutes": 5
    }
  }'
```

确认模板正常后，再测试发送接口。

```bash
curl -X POST 'http://localhost:8080/api/mail/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "receivers": ["test@example.com"],
    "subject": "邮箱验证码",
    "type": "TEMPLATE",
    "templateName": "mail/verify-code",
    "templateVariables": {
      "productName": "业务管理系统",
      "code": "836291",
      "expireMinutes": 5
    }
  }'
```

接口测试建议覆盖以下场景：

| 测试场景       | 预期结果                   |
| -------------- | -------------------------- |
| 合法文本邮件   | 返回发送成功或异步提交成功 |
| 合法 HTML 邮件 | 返回发送成功或异步提交成功 |
| 合法模板邮件   | 模板渲染成功并发送         |
| 收件人为空     | 返回参数校验失败           |
| 邮箱格式错误   | 返回参数校验失败           |
| 主题为空       | 返回参数校验失败           |
| 模板名称为空   | 返回参数校验失败           |
| 测试白名单拦截 | 返回收件人不在白名单中     |
| 邮件发送关闭   | 返回跳过发送或提交失败提示 |

### 邮件送达验证

邮件送达验证用于确认 SMTP 配置、邮箱授权码、网络连通性和邮箱服务商投递策略是否正常。代码测试通过不代表邮件一定能送达用户收件箱，因此必须进行真实送达验证。

送达验证建议按以下顺序执行：

```text
确认 SMTP 配置
    -> 验证网络端口连通性
    -> 发送测试邮件
    -> 查看发送记录状态
    -> 检查收件箱和垃圾箱
    -> 检查发件邮箱退信
```

先确认环境变量是否存在。

```bash
# 查看邮件相关环境变量
echo $MAIL_HOST
echo $MAIL_PORT
echo $MAIL_USERNAME

# 不要在公共终端或日志中输出 MAIL_PASSWORD
```

再检查服务器到 SMTP 服务的端口连通性。

```bash
# 检查 465 端口连通性
nc -vz smtp.qq.com 465

# 检查 587 端口连通性
nc -vz smtp.office365.com 587
```

上述命令用于检查服务器是否可以连接 SMTP 端口。`nc -vz` 只做连接探测，不发送邮件。若连接失败，需要检查云服务器安全组、出口网络、防火墙、邮箱服务商端口和本机 DNS。

启动项目后，先观察启动日志中邮件模块配置是否加载成功。

```text
邮件模块初始化完成，启用状态：true，异步发送：true，发送记录：true
```

然后发送测试邮件，并通过发送记录查询接口确认状态。

```bash
curl -X POST 'http://localhost:8080/api/mail/records/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "receiver": "test@example.com",
    "subject": "邮箱验证码"
  }'
```

如果状态为 `SUCCESS`，但收件箱没有收到邮件，需要继续检查：

| 检查项         | 说明                                   |
| -------------- | -------------------------------------- |
| 垃圾箱         | 邮件可能被邮箱客户端归类为垃圾邮件     |
| 退信邮件       | 发件邮箱可能收到退信通知               |
| 发件人信誉     | 新邮箱、大量发送、内容相似可能影响投递 |
| SPF/DKIM/DMARC | 企业域名邮箱建议配置邮件认证记录       |
| 邮件内容       | 过多链接、敏感词、图片异常可能被拦截   |
| 收件人规则     | 用户邮箱可能配置了过滤规则             |

如果状态为 `FAIL`，优先查看 `error_message` 和应用日志。常见错误可以参考后续“常见问题排查”。

## 部署与运维

部署与运维用于保证邮件模块在开发、测试、预发和生产环境中稳定运行。邮件模块依赖外部 SMTP 服务，生产环境需要重点关注配置隔离、授权码安全、发送频率、失败告警和历史记录清理。

### 不同环境配置

不同环境应使用不同的邮箱配置和发送策略。开发环境可以关闭真实发送，测试环境建议启用白名单，生产环境应启用真实发送并使用专用企业邮箱账号。

推荐环境策略如下：

| 环境   | 发送开关 | 收件人白名单 | SMTP 账号    | 说明                 |
| ------ | -------- | ------------ | ------------ | -------------------- |
| `dev`  | 可关闭   | 建议配置     | 开发专用邮箱 | 避免开发阶段误发     |
| `test` | 开启     | 必须配置     | 测试专用邮箱 | 只允许发送给测试人员 |
| `pre`  | 开启     | 按需配置     | 预发专用邮箱 | 验证真实链路         |
| `prod` | 开启     | 不配置       | 生产专用邮箱 | 服务真实用户         |

公共配置放在 `application.yml`。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  mail:
    # 邮件默认编码
    default-encoding: UTF-8

    properties:
      mail:
        smtp:
          # 开启 SMTP 认证
          auth: true

          # SMTP 连接超时时间，单位毫秒
          connectiontimeout: 10000

          # SMTP 读取超时时间，单位毫秒
          timeout: 10000

          # SMTP 写入超时时间，单位毫秒
          writetimeout: 10000

mail:
  biz:
    # 默认启用异步发送
    async-enabled: true

    # 默认记录发送日志
    record-enabled: true

    # 单封邮件最大附件大小，单位 MB
    max-attachment-size-mb: 20

    # 单次最多收件人数
    max-receiver-count: 50
```

开发环境可以关闭真实发送。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  mail:
    # 开发环境 SMTP 地址，可通过环境变量覆盖
    host: ${MAIL_HOST:smtp.qq.com}

    # 开发环境 SMTP 端口
    port: ${MAIL_PORT:465}

    # 开发环境发件账号
    username: ${MAIL_USERNAME:dev-notice@example.com}

    # 开发环境授权码，建议从环境变量读取
    password: ${MAIL_PASSWORD:}

    properties:
      mail:
        smtp:
          ssl:
            # 465 端口开启 SSL
            enable: true

mail:
  biz:
    # 开发环境默认关闭真实发送
    enabled: false

    # 开发环境限制测试收件人
    test-receiver-whitelist:
      - test@example.com
      - developer@example.com
```

测试环境建议开启真实发送，但必须限制白名单。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          ssl:
            # 测试环境使用 SSL
            enable: true

mail:
  biz:
    # 测试环境开启真实发送
    enabled: true

    # 测试环境必须限制收件人，避免误发真实用户
    test-receiver-whitelist:
      - qa@example.com
      - developer@example.com
```

生产环境必须通过环境变量、配置中心或密钥管理平台提供敏感配置。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  mail:
    # 生产环境 SMTP 地址，必须由外部注入
    host: ${MAIL_HOST}

    # 生产环境 SMTP 端口，必须由外部注入
    port: ${MAIL_PORT}

    # 生产环境发件邮箱账号，必须由外部注入
    username: ${MAIL_USERNAME}

    # 生产环境邮箱授权码，必须由外部注入
    password: ${MAIL_PASSWORD}

    properties:
      mail:
        smtp:
          ssl:
            # 使用 465 端口时开启 SSL
            enable: true

mail:
  biz:
    # 生产环境开启真实发送
    enabled: true

    # 生产环境不配置测试白名单
    test-receiver-whitelist: []
```

Jar 包部署示例：

```bash
export SPRING_PROFILES_ACTIVE=prod
export MAIL_HOST=smtp.exmail.qq.com
export MAIL_PORT=465
export MAIL_USERNAME=notice@example.com
export MAIL_PASSWORD=********

java -jar app.jar
```

如果使用 systemd 管理服务，可以将环境变量放到独立环境文件中。

文件位置：`/etc/mail-service/mail-service.env`

```properties
# Spring Boot 运行环境
SPRING_PROFILES_ACTIVE=prod

# SMTP 配置
MAIL_HOST=smtp.exmail.qq.com
MAIL_PORT=465
MAIL_USERNAME=notice@example.com
MAIL_PASSWORD=replace-with-real-secret
```

文件位置：`/etc/systemd/system/mail-service.service`

```ini
[Unit]
Description=Mail Service
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/mail-service
EnvironmentFile=/etc/mail-service/mail-service.env
ExecStart=/usr/bin/java -jar /opt/mail-service/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

加载并启动服务：

```bash
# 重新加载 systemd 配置
systemctl daemon-reload

# 启动邮件服务
systemctl start mail-service

# 设置开机自启
systemctl enable mail-service

# 查看服务状态
systemctl status mail-service
```

这些命令用于将邮件服务注册为 systemd 服务，并通过环境文件注入敏感配置。生产环境需要控制 `/etc/mail-service/mail-service.env` 文件权限，避免普通用户读取邮箱授权码。

### 邮件发送频率控制

邮件发送频率控制用于防止接口被刷、验证码滥发、邮箱服务商限流和发件账号信誉下降。邮件发送属于外部投递行为，必须对用户、邮箱、IP、业务类型和全局发送量进行限制。

常见限流维度如下：

| 限流维度 | 示例                               |
| -------- | ---------------------------------- |
| 单个邮箱 | 同一邮箱 1 分钟最多发送 1 次验证码 |
| 单个 IP  | 同一 IP 1 分钟最多请求 5 次        |
| 单个用户 | 同一用户 1 小时最多发送 10 次      |
| 业务类型 | 找回密码类邮件限制更严格           |
| 全局频率 | 系统每分钟最多发送 300 封          |
| 附件邮件 | 附件邮件单独限制，避免占用带宽     |

如果是单体应用，可以先使用本地缓存实现简单限流。下面示例基于 Hutool 的 timed cache 实现单节点邮箱频率控制。

文件位置：`src/main/java/io/github/atengk/mail/limit/MailSendRateLimiter.java`

```java
package io.github.atengk.mail.limit;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.exception.MailErrorCode;
import io.github.atengk.mail.exception.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 邮件发送频率限制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class MailSendRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(MailSendRateLimiter.class);

    /**
     * 本地限流缓存，默认过期时间 60 秒
     */
    private final TimedCache<String, Long> localLimitCache = CacheUtil.newTimedCache(60_000);

    /**
     * 校验邮箱发送频率
     *
     * @param bizType  业务类型
     * @param receiver 收件人邮箱
     */
    public void checkEmailLimit(String bizType, String receiver) {
        if (StrUtil.isBlank(receiver)) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "收件人邮箱不能为空");
        }

        String cacheKey = buildCacheKey(bizType, receiver);
        Long lastSendTime = localLimitCache.get(cacheKey);
        if (lastSendTime != null) {
            log.warn("邮件发送过于频繁，业务类型：{}，收件人：{}", bizType, receiver);
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件发送过于频繁，请稍后再试");
        }

        localLimitCache.put(cacheKey, System.currentTimeMillis());
    }

    /**
     * 构建限流缓存 Key
     *
     * @param bizType  业务类型
     * @param receiver 收件人邮箱
     * @return 缓存 Key
     */
    private String buildCacheKey(String bizType, String receiver) {
        return "mail:limit:" + StrUtil.blankToDefault(bizType, "default") + ":" + receiver;
    }
}
```

在提交邮件发送前调用限流器。

```java
mailSendRateLimiter.checkEmailLimit("VERIFY_CODE", receiver);
Long recordId = mailSubmitService.submitVerifyCodeMail(receiver);
```

本地缓存限流只适合单节点应用。如果系统是多实例部署，应使用 Redis 实现分布式限流。下面是基于 `StringRedisTemplate` 的简单实现。

文件位置：`src/main/java/io/github/atengk/mail/limit/RedisMailSendRateLimiter.java`

```java
package io.github.atengk.mail.limit;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.exception.MailErrorCode;
import io.github.atengk.mail.exception.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 邮件发送频率限制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class RedisMailSendRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisMailSendRateLimiter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisMailSendRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 校验邮箱发送频率
     *
     * @param bizType        业务类型
     * @param receiver       收件人邮箱
     * @param intervalSecond 间隔秒数
     */
    public void checkEmailLimit(String bizType, String receiver, long intervalSecond) {
        if (StrUtil.isBlank(receiver)) {
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "收件人邮箱不能为空");
        }

        String key = buildKey(bizType, receiver);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(intervalSecond));

        if (!Boolean.TRUE.equals(success)) {
            log.warn("Redis 邮件发送频率限制命中，业务类型：{}，收件人：{}", bizType, receiver);
            throw new MailException(MailErrorCode.MAIL_PARAM_ERROR, "邮件发送过于频繁，请稍后再试");
        }
    }

    /**
     * 构建 Redis 限流 Key
     *
     * @param bizType  业务类型
     * @param receiver 收件人邮箱
     * @return Redis Key
     */
    private String buildKey(String bizType, String receiver) {
        return "mail:limit:" + StrUtil.blankToDefault(bizType, "default") + ":" + receiver;
    }
}
```

Redis 限流需要引入 Redis 依赖。

文件位置：`pom.xml`

```xml
<dependency>
    <!-- Redis 依赖，用于分布式邮件发送频率控制 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

配置 Redis 连接信息。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # Redis 地址
      host: ${REDIS_HOST:localhost}

      # Redis 端口
      port: ${REDIS_PORT:6379}

      # Redis 密码，没有密码可留空
      password: ${REDIS_PASSWORD:}

      # Redis 数据库编号
      database: ${REDIS_DATABASE:0}
```

频率控制建议值如下：

| 邮件类型   | 建议限制                             |
| ---------- | ------------------------------------ |
| 注册验证码 | 同一邮箱 60 秒 1 次                  |
| 找回密码   | 同一邮箱 60 秒 1 次，1 小时最多 5 次 |
| 登录提醒   | 可按用户或 IP 限制                   |
| 审批通知   | 按业务事件去重，避免重复通知         |
| 报表推送   | 由定时任务控制，不允许接口频繁触发   |
| 系统告警   | 增加聚合，避免异常风暴刷屏           |

对于重要邮件，限流命中时应明确提示“发送过于频繁，请稍后再试”。对于内部系统通知，可以记录日志后直接丢弃重复通知，避免骚扰用户。

### 常见问题排查

邮件发送失败时，应优先从发送记录、应用日志、SMTP 配置、网络连通性和邮箱服务商限制几个方向排查。

常见问题如下：

| 问题                 | 可能原因                                 | 处理方式                                 |
| -------------------- | ---------------------------------------- | ---------------------------------------- |
| SMTP 认证失败        | 账号错误、授权码错误、SMTP 服务未开启    | 检查邮箱账号、授权码和邮箱后台 SMTP 开关 |
| 连接超时             | 端口不通、防火墙拦截、云服务器限制       | 使用 `nc -vz` 检查端口连通性             |
| 本地成功，服务器失败 | 服务器出口端口被封、安全组未放行         | 检查云服务器安全组和网络策略             |
| 发送成功但收不到     | 被归入垃圾箱、被服务商拦截、收件规则过滤 | 检查垃圾箱、退信、发件人信誉             |
| HTML 样式异常        | 邮箱客户端不支持复杂 CSS                 | 使用行内样式，减少复杂布局               |
| 附件发送失败         | 文件不存在、文件过大、权限不足           | 检查文件路径、大小和读取权限             |
| 模板渲染失败         | 模板路径错误、变量错误、语法错误         | 检查 `templates/mail` 目录和模板名称     |
| 测试环境发不出去     | 发送开关关闭或白名单拦截                 | 检查 `mail.biz.enabled` 和白名单         |
| 异步方法未生效       | 同类内部调用、未启用 `@EnableAsync`      | 确认通过 Spring Bean 调用异步方法        |
| 发送记录一直 PENDING | 异步任务未提交或线程池异常               | 检查线程池配置和异步服务日志             |
| 发送记录一直 SENDING | 发送线程卡住或 SMTP 超时过长             | 检查超时配置和线程池状态                 |

排查 SMTP 端口连通性：

```bash
# 检查 SSL 端口
nc -vz smtp.exmail.qq.com 465

# 检查 STARTTLS 端口
nc -vz smtp.office365.com 587
```

排查当前服务日志：

```bash
# systemd 查看最近日志
journalctl -u mail-service -n 200 --no-pager

# 按关键字过滤邮件日志
journalctl -u mail-service --no-pager | grep "邮件"
```

如果是 Docker 部署：

```bash
# 查看容器日志
docker logs --tail=200 mail-service

# 进入容器检查环境变量
docker exec -it mail-service env | grep MAIL_
```

注意不要在共享终端中输出 `MAIL_PASSWORD`。如果需要确认密码是否注入，只检查变量是否存在，不打印真实值。

```bash
# 只判断 MAIL_PASSWORD 是否存在
if [ -n "$MAIL_PASSWORD" ]; then
  echo "MAIL_PASSWORD is set"
else
  echo "MAIL_PASSWORD is empty"
fi
```

排查模板文件是否被正确打包：

```bash
# 查看 Jar 包中是否存在邮件模板
jar tf app.jar | grep 'templates/mail'
```

如果看不到模板文件，说明构建产物中没有包含模板，需要检查 `src/main/resources/templates/mail/` 目录是否正确，或者 Maven 构建资源配置是否排除了模板文件。

排查发送记录状态：

```sql
-- 查询最近 20 条邮件发送记录
SELECT
    id,
    subject,
    mail_type,
    status,
    success,
    skipped,
    error_message,
    submit_time,
    send_time
FROM mail_record
ORDER BY create_time DESC
LIMIT 20;

-- 查询最近失败记录
SELECT
    id,
    subject,
    error_message,
    send_time
FROM mail_record
WHERE status = 'FAIL'
ORDER BY create_time DESC
LIMIT 20;
```

如果需要查看完整异常堆栈：

```sql
-- 查看指定邮件发送记录的异常堆栈
SELECT
    id,
    error_message,
    error_stack
FROM mail_record
WHERE id = 10001;
```

生产环境中，`error_stack` 只建议对开发和运维人员开放。普通后台用户只需要看到 `error_message`。

常见异常和处理建议如下：

| 异常关键字                          | 说明           | 处理方式                    |
| ----------------------------------- | -------------- | --------------------------- |
| `Authentication failed`             | SMTP 认证失败  | 检查账号、授权码、SMTP 开关 |
| `Connection timed out`              | 连接超时       | 检查端口、防火墙、安全组    |
| `Could not connect to SMTP host`    | 无法连接 SMTP  | 检查 host、port、网络       |
| `Invalid Addresses`                 | 收件人地址无效 | 检查邮箱格式和收件人列表    |
| `TemplateInputException`            | 模板加载失败   | 检查模板路径和文件名        |
| `FileNotFoundException`             | 附件不存在     | 检查附件路径和文件权限      |
| `Daily user sending quota exceeded` | 邮箱服务商限额 | 降低发送频率或更换服务方案  |

最终上线前建议完成以下检查：

| 检查项                         | 是否必须 |
| ------------------------------ | -------- |
| 生产 SMTP 配置通过环境变量注入 | 必须     |
| 邮箱授权码未提交 Git           | 必须     |
| 发送接口参数校验完整           | 必须     |
| 测试环境收件人白名单已启用     | 必须     |
| 邮件发送记录可查询             | 必须     |
| 异步线程池配置合理             | 必须     |
| SMTP 端口连通性验证通过        | 必须     |
| 真实邮箱送达验证通过           | 必须     |
| 失败日志可定位问题             | 必须     |
| 发送频率控制已启用             | 建议     |
| 历史记录清理策略已制定         | 建议     |

完成本节后，邮件开发使用文档已经覆盖从开发、配置、核心发送、模板管理、接口、异步、记录、异常、安全、测试到部署运维的主要内容。
