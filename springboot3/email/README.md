# Spring Boot 邮箱 开发使用



## 基础配置

### 添加依赖

```xml
<!-- SMTP / JavaMail 邮件发送能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<!-- Jakarta Validation 参数校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<!-- Thymeleaf 邮件模板渲染 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<!-- Actuator 健康检查和运行时观测入口 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 编辑配置

```yaml
---
# 邮箱配置
spring:
  mail:
    # SMTP 服务地址
    host: smtp.qq.com
    # SMTP 服务端口，常见值：25、465、587
    port: 587
    # 发件账号
    username: 2385569970@qq.com
    # 发件密码或授权码，生产环境建议使用环境变量注入
    password: ${MAIL_PASSWORD:change-me}
    # 默认编码
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          # 开启 SMTP 认证
          auth: true
          # 启用 STARTTLS
          starttls:
            enable: true
          # 连接超时时间，单位毫秒
          connectiontimeout: 10000
          # 读超时时间，单位毫秒
          timeout: 10000
          # 写超时时间，单位毫秒
          writetimeout: 10000
```

### 邮件异步线程池配置

```java
package io.github.atengk.mail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件异步线程池配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class MailAsyncConfig {

    /**
     * 邮件发送专用线程池。
     *
     * @return 邮件任务执行器
     */
    @Bean("mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：适合常规项目，避免邮件发送阻塞主业务线程
        executor.setCorePoolSize(4);

        // 最大线程数：突发批量发送时临时扩容
        executor.setMaxPoolSize(12);

        // 队列容量：根据项目邮件峰值调整，避免无限堆积
        executor.setQueueCapacity(500);

        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);

        // 线程名前缀，方便日志排查
        executor.setThreadNamePrefix("mail-send-");

        // 关闭应用时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 队列满时由调用线程执行，形成反压，避免任务静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("邮件异步线程池初始化完成，corePoolSize={}，maxPoolSize={}，queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
```

### 邮件枚举

#### 邮件正文类型

```java
package io.github.atengk.mail.enums;

/**
 * 邮件正文类型。
 *
 * @author Ateng
 * @since 2026-05-06
 */
public enum MailContentType {

    /**
     * 纯文本邮件。
     */
    TEXT,

    /**
     * HTML 邮件。
     */
    HTML,

    /**
     * 模板邮件。
     */
    TEMPLATE
}

```

#### 邮件优先级

```java
package io.github.atengk.mail.enums;

/**
 * 邮件优先级。
 *
 * @author Ateng
 * @since 2026-05-06
 */
public enum MailPriority {

    /**
     * 低优先级。
     */
    LOW,

    /**
     * 普通优先级。
     */
    NORMAL,

    /**
     * 高优先级。
     */
    HIGH
}

```

### 邮件实体类

#### 邮件附件信息

```java
package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件附件信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 附件文件名。
     */
    private String fileName;

    /**
     * 附件内容类型，例如 application/pdf、image/png。
     */
    private String contentType;

    /**
     * 本地文件路径，适用于服务端已有文件的场景。
     */
    private String filePath;

    /**
     * 附件字节内容，适用于动态生成文件或上传文件转发的场景。
     */
    private byte[] content;
}

```

#### 批量邮件发送请求

```java
package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

/**
 * 批量邮件发送请求。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailBatchSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批量任务编号。
     */
    private String batchBizId;

    /**
     * 待发送邮件列表。
     */
    @Valid
    @NotEmpty(message = "批量邮件列表不能为空")
    @Builder.Default
    private List<MailSendRequest> messages = CollUtil.newArrayList();

    /**
     * 单批次大小，用于实现类控制发送节奏。
     */
    @Positive(message = "批次大小必须大于 0")
    @Builder.Default
    private Integer batchSize = 50;

    /**
     * 批次间隔，用于限流或降低 SMTP 压力。
     */
    @Builder.Default
    private Duration batchInterval = Duration.ZERO;

    /**
     * 单封发送失败后是否继续处理剩余邮件。
     */
    @Builder.Default
    private Boolean continueOnError = true;
}

```

#### 邮件联系人信息

```java
package io.github.atengk.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件联系人信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailContact implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 联系人显示名称。
     */
    private String name;

    /**
     * 邮箱地址。
     */
    @Email(message = "邮箱地址格式不正确")
    @NotBlank(message = "邮箱地址不能为空")
    private String address;
}

```

#### 邮件发件人

```java
package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件发件人。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailFrom {

    /**
     * 发件邮箱。
     *
     * <p>
     * 不传时默认使用当前 SMTP 配置的 username。
     * 多数 SMTP 服务商要求该值必须和 username 一致。
     * </p>
     */
    private String address;

    /**
     * 发件人显示名称。
     */
    private String name;

    /**
     * 动态 SMTP 配置。
     *
     * <p>
     * 不传时使用 spring.mail 默认配置。
     * 传入时本次邮件使用该配置发送。
     * </p>
     */
    private MailSmtpConfig mailConfig;
}
```

#### HTML 邮件内嵌资源信息

```java
package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTML 邮件内嵌资源信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailInlineResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * HTML 中引用的 contentId，例如 logo，对应 cid:logo。
     */
    private String contentId;

    /**
     * 资源名称。
     */
    private String name;

    /**
     * 资源内容类型，例如 image/png。
     */
    private String contentType;

    /**
     * 本地资源路径。
     */
    private String filePath;

    /**
     * 资源字节内容。
     */
    private byte[] content;
}

```

#### 单封邮件发送请求

```java
package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import io.github.atengk.mail.enums.MailContentType;
import io.github.atengk.mail.enums.MailPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 单封邮件发送请求。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务编号，用于外部系统追踪邮件发送结果。
     */
    private String bizId;

    /**
     * 发件人。
     *
     * <p>
     * 不传时使用 spring.mail 默认配置。
     * 如果传入 from.mailConfig，则本次邮件使用该动态 SMTP 配置发送。
     * </p>
     */
    private MailFrom from;

    /**
     * 收件人列表。
     */
    @Valid
    @NotEmpty(message = "收件人不能为空")
    @Builder.Default
    private List<MailContact> to = CollUtil.newArrayList();

    /**
     * 抄送人列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> cc = CollUtil.newArrayList();

    /**
     * 密送人列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> bcc = CollUtil.newArrayList();

    /**
     * 回复地址列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> replyTo = CollUtil.newArrayList();

    /**
     * 邮件主题。
     */
    @NotBlank(message = "邮件主题不能为空")
    private String subject;

    /**
     * 邮件正文。文本和 HTML 邮件直接使用该字段，模板邮件可存放渲染后的内容。
     */
    private String content;

    /**
     * 邮件正文类型。
     */
    @Builder.Default
    private MailContentType contentType = MailContentType.TEXT;

    /**
     * 模板名称，例如 notice.ftl、mail/order.html。
     */
    private String templateName;

    /**
     * 模板变量。
     */
    @Builder.Default
    private Map<String, Object> templateVariables = MapUtil.newHashMap();

    /**
     * 邮件附件列表。
     */
    @Valid
    @Builder.Default
    private List<MailAttachment> attachments = CollUtil.newArrayList();

    /**
     * HTML 内嵌资源列表，例如内嵌图片。
     */
    @Valid
    @Builder.Default
    private List<MailInlineResource> inlineResources = CollUtil.newArrayList();

    /**
     * 自定义邮件头。
     */
    @Builder.Default
    private Map<String, String> headers = MapUtil.newHashMap();

    /**
     * 邮件优先级。
     */
    @Builder.Default
    private MailPriority priority = MailPriority.NORMAL;

    /**
     * 字符编码。
     */
    @Builder.Default
    private String charset = CharsetUtil.UTF_8;

    /**
     * 发送失败时是否抛出异常，false 时由结果对象承载失败信息。
     */
    @Builder.Default
    private Boolean throwException = false;
}

```

#### 单封邮件发送结果

```java
package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单封邮件发送结果。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务编号。
     */
    private String bizId;

    /**
     * 邮件服务商或 SMTP 返回的消息编号。
     */
    private String messageId;

    /**
     * 邮件主题。
     */
    private String subject;

    /**
     * 收件人地址列表。
     */
    @Builder.Default
    private List<String> recipients = CollUtil.newArrayList();

    /**
     * 是否发送成功。
     */
    private Boolean success;

    /**
     * 失败编码。
     */
    private String errorCode;

    /**
     * 失败信息。
     */
    private String errorMessage;

    /**
     * 发送完成时间。
     */
    @Builder.Default
    private LocalDateTime sendTime = LocalDateTime.now();
}

```

#### 批量邮件发送汇总结果

```java
package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量邮件发送汇总结果。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批量任务编号。
     */
    private String batchBizId;

    /**
     * 总数量。
     */
    private Integer totalCount;

    /**
     * 成功数量。
     */
    private Integer successCount;

    /**
     * 失败数量。
     */
    private Integer failureCount;

    /**
     * 明细结果。
     */
    @Builder.Default
    private List<MailSendResult> results = CollUtil.newArrayList();

    /**
     * 批量任务完成时间。
     */
    @Builder.Default
    private LocalDateTime finishTime = LocalDateTime.now();
}

```

#### 动态 SMTP 配置

```java
package io.github.atengk.mail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 动态 SMTP 配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSmtpConfig {

    /**
     * SMTP 服务地址。
     */
    private String host;

    /**
     * SMTP 服务端口。
     */
    private Integer port;

    /**
     * SMTP 认证账号。
     */
    private String username;

    /**
     * SMTP 密码或授权码。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 默认编码。
     */
    private String defaultEncoding;

    /**
     * 是否开启 SMTP 认证。
     */
    private Boolean auth;

    /**
     * 是否启用 STARTTLS。
     */
    private Boolean starttlsEnable;

    /**
     * 是否强制 STARTTLS。
     */
    private Boolean starttlsRequired;

    /**
     * 是否启用 SSL。
     */
    private Boolean sslEnable;

    /**
     * 连接超时时间，单位毫秒。
     */
    private Integer connectionTimeout;

    /**
     * 读取超时时间，单位毫秒。
     */
    private Integer timeout;

    /**
     * 写入超时时间，单位毫秒。
     */
    private Integer writeTimeout;

    /**
     * 是否强制 from.address 和 username 一致。
     */
    private Boolean forceFromSameAsUsername;

    /**
     * 额外 JavaMail 属性。
     */
    private Map<String, String> properties;
}
```



## 邮件服务

### 邮件服务接口

```java
package io.github.atengk.mail.service;

import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MailService {

    /**
     * 根据请求中的正文类型发送邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult send(MailSendRequest request);

    /**
     * 发送纯文本邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendText(MailSendRequest request);

    /**
     * 发送 HTML 邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendHtml(MailSendRequest request);

    /**
     * 发送模板邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendTemplate(MailSendRequest request);

    /**
     * 批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    MailSendSummary sendBatch(MailBatchSendRequest request);

    /**
     * 异步发送单封邮件。
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    CompletableFuture<MailSendResult> sendAsync(MailSendRequest request);

    /**
     * 使用指定线程池异步发送单封邮件。
     *
     * @param request  邮件发送请求
     * @param executor 指定线程池
     * @return 异步邮件发送结果
     */
    CompletableFuture<MailSendResult> sendAsync(MailSendRequest request, Executor executor);

    /**
     * 异步批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request);

    /**
     * 使用指定线程池异步批量发送邮件。
     *
     * @param request  批量邮件发送请求
     * @param executor 指定线程池
     * @return 异步批量邮件发送汇总结果
     */
    CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request, Executor executor);

    /**
     * 渲染邮件模板。
     *
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 渲染后的邮件正文
     */
    String renderTemplate(String templateName, Map<String, Object> variables);

    /**
     * 校验邮件发送请求。
     *
     * @param request 邮件发送请求
     */
    void validate(MailSendRequest request);

    /**
     * 检查邮件服务器连接状态。
     *
     * @return true 表示连接正常，false 表示连接异常
     */
    boolean checkConnection();
}

```

### 邮件服务接口实现

```java
package io.github.atengk.mail.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import io.github.atengk.mail.dto.MailAttachment;
import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailContact;
import io.github.atengk.mail.dto.MailInlineResource;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;
import io.github.atengk.mail.enums.MailContentType;
import io.github.atengk.mail.enums.MailPriority;
import io.github.atengk.mail.service.MailService;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务默认实现。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final JavaMailSender javaMailSender;

    private final MailProperties mailProperties;

    private final Validator validator;

    private final TemplateEngine templateEngine = TemplateUtil.createEngine(
            new TemplateConfig("templates", TemplateConfig.ResourceMode.CLASSPATH)
    );

    @Qualifier("mailTaskExecutor")
    private final Executor mailTaskExecutor;

    /**
     * 根据邮件正文类型发送邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult send(MailSendRequest request) {
        validate(request);

        MailContentType contentType = ObjectUtil.defaultIfNull(request.getContentType(), MailContentType.TEXT);
        return switch (contentType) {
            case TEXT -> sendText(request);
            case HTML -> sendHtml(request);
            case TEMPLATE -> sendTemplate(request);
        };
    }

    /**
     * 发送纯文本邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendText(MailSendRequest request) {
        request.setContentType(MailContentType.TEXT);
        return doSend(request);
    }

    /**
     * 发送 HTML 邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendHtml(MailSendRequest request) {
        request.setContentType(MailContentType.HTML);
        return doSend(request);
    }

    /**
     * 发送模板邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendTemplate(MailSendRequest request) {
        request.setContentType(MailContentType.TEMPLATE);
        return doSend(request);
    }

    /**
     * 批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    @Override
    public MailSendSummary sendBatch(MailBatchSendRequest request) {
        validateBatch(request);

        List<MailSendRequest> messages = request.getMessages();
        List<MailSendResult> results = CollUtil.newArrayList();
        int batchSize = Math.max(ObjectUtil.defaultIfNull(request.getBatchSize(), 50), 1);
        Duration batchInterval = ObjectUtil.defaultIfNull(request.getBatchInterval(), Duration.ZERO);
        boolean continueOnError = BooleanUtil.isTrue(request.getContinueOnError());

        log.info("开始批量发送邮件，批量编号：{}，总数量：{}，批次大小：{}",
                request.getBatchBizId(), messages.size(), batchSize);

        for (int start = 0; start < messages.size(); start += batchSize) {
            int end = Math.min(start + batchSize, messages.size());
            List<MailSendRequest> batchMessages = messages.subList(start, end);

            for (MailSendRequest message : batchMessages) {
                MailSendResult result;
                try {
                    result = send(message);
                } catch (Exception ex) {
                    result = buildFailureResult(message, ex);
                    log.error("批量邮件发送异常，业务编号：{}，原因：{}", message.getBizId(), ex.getMessage(), ex);
                }

                results.add(result);

                if (BooleanUtil.isFalse(result.getSuccess()) && !continueOnError) {
                    log.warn("批量邮件发送中断，批量编号：{}，失败业务编号：{}",
                            request.getBatchBizId(), message.getBizId());
                    return buildSummary(request.getBatchBizId(), messages.size(), results);
                }
            }

            if (end < messages.size() && !batchInterval.isZero() && !batchInterval.isNegative()) {
                ThreadUtil.safeSleep(batchInterval.toMillis());
            }
        }

        MailSendSummary summary = buildSummary(request.getBatchBizId(), messages.size(), results);
        log.info("批量邮件发送完成，批量编号：{}，成功：{}，失败：{}",
                summary.getBatchBizId(), summary.getSuccessCount(), summary.getFailureCount());
        return summary;
    }

    /**
     * 异步发送单封邮件。
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    @Override
    public CompletableFuture<MailSendResult> sendAsync(MailSendRequest request) {
        return CompletableFuture.supplyAsync(() -> send(request), mailTaskExecutor);
    }

    /**
     * 使用指定线程池异步发送单封邮件。
     *
     * @param request  邮件发送请求
     * @param executor 指定线程池
     * @return 异步邮件发送结果
     */
    @Override
    public CompletableFuture<MailSendResult> sendAsync(MailSendRequest request, Executor executor) {
        Executor actualExecutor = ObjectUtil.defaultIfNull(executor, mailTaskExecutor);
        return CompletableFuture.supplyAsync(() -> send(request), actualExecutor);
    }

    /**
     * 异步批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @Override
    public CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request) {
        return CompletableFuture.supplyAsync(() -> sendBatch(request), mailTaskExecutor);
    }

    /**
     * 使用指定线程池异步批量发送邮件。
     *
     * @param request  批量邮件发送请求
     * @param executor 指定线程池
     * @return 异步批量邮件发送汇总结果
     */
    @Override
    public CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request, Executor executor) {
        Executor actualExecutor = ObjectUtil.defaultIfNull(executor, mailTaskExecutor);
        return CompletableFuture.supplyAsync(() -> sendBatch(request), actualExecutor);
    }

    /**
     * 渲染邮件模板。
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return 渲染后的邮件正文
     */
    @Override
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        if (StrUtil.isBlank(templateName)) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        Template template = templateEngine.getTemplate(templateName);
        return template.render(MapUtil.emptyIfNull(variables));
    }

    /**
     * 校验邮件发送请求。
     *
     * @param request 邮件发送请求
     */
    @Override
    public void validate(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("邮件发送请求不能为空");
        }

        Set<ConstraintViolation<MailSendRequest>> violations = validator.validate(request);
        if (CollUtil.isNotEmpty(violations)) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("邮件发送请求参数不合法");
            throw new IllegalArgumentException(message);
        }

        MailContentType contentType = ObjectUtil.defaultIfNull(request.getContentType(), MailContentType.TEXT);
        if (contentType == MailContentType.TEMPLATE) {
            if (StrUtil.isBlank(request.getTemplateName())) {
                throw new IllegalArgumentException("模板邮件的模板名称不能为空");
            }
        } else if (StrUtil.isBlank(request.getContent())) {
            throw new IllegalArgumentException("邮件正文不能为空");
        }

        validateAttachments(request.getAttachments());
        validateInlineResources(request.getInlineResources());
    }

    /**
     * 检查邮件服务器连接状态。
     *
     * @return true 表示连接正常，false 表示连接异常
     */
    @Override
    public boolean checkConnection() {
        try {
            if (javaMailSender instanceof JavaMailSenderImpl sender) {
                sender.testConnection();
            } else {
                javaMailSender.createMimeMessage();
            }
            log.info("邮件服务器连接检查通过");
            return true;
        } catch (Exception ex) {
            log.warn("邮件服务器连接检查失败，原因：{}", ex.getMessage());
            return false;
        }
    }

    /**
     * 执行邮件发送。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    private MailSendResult doSend(MailSendRequest request) {
        try {
            validate(request);

            JavaMailSender actualMailSender = resolveMailSender(request);
            MimeMessage mimeMessage = actualMailSender.createMimeMessage();
            String charset = resolveCharset(request);
            boolean multipart = CollUtil.isNotEmpty(request.getAttachments())
                    || CollUtil.isNotEmpty(request.getInlineResources());

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, charset);
            fillBasicMessage(helper, mimeMessage, request, charset);
            addAttachments(helper, request.getAttachments());
            addInlineResources(helper, request.getInlineResources());

            actualMailSender.send(mimeMessage);

            String messageId = ArrayUtil.get(mimeMessage.getHeader("Message-ID"), 0);
            MailSendResult result = MailSendResult.builder()
                    .bizId(request.getBizId())
                    .messageId(messageId)
                    .subject(request.getSubject())
                    .recipients(toAddressList(request.getTo()))
                    .success(true)
                    .sendTime(LocalDateTime.now())
                    .build();

            log.info("邮件发送成功，业务编号：{}，主题：{}，收件人数：{}",
                    request.getBizId(), request.getSubject(), result.getRecipients().size());
            return result;
        } catch (Exception ex) {
            MailSendResult result = buildFailureResult(request, ex);
            log.error("邮件发送失败，业务编号：{}，主题：{}，原因：{}",
                    request == null ? null : request.getBizId(),
                    request == null ? null : request.getSubject(),
                    ex.getMessage(),
                    ex);

            if (request != null && BooleanUtil.isTrue(request.getThrowException())) {
                throw new IllegalStateException("邮件发送失败：" + ex.getMessage(), ex);
            }
            return result;
        }
    }

    /**
     * 填充邮件基础信息。
     *
     * @param helper      邮件助手
     * @param mimeMessage MIME 消息
     * @param request     邮件发送请求
     * @param charset     字符编码
     */
    private void fillBasicMessage(MimeMessageHelper helper,
                                  MimeMessage mimeMessage,
                                  MailSendRequest request,
                                  String charset) throws Exception {
        fillFrom(helper, request, charset);

        helper.setTo(toInternetAddresses(request.getTo(), charset));

        if (CollUtil.isNotEmpty(request.getCc())) {
            helper.setCc(toInternetAddresses(request.getCc(), charset));
        }
        if (CollUtil.isNotEmpty(request.getBcc())) {
            helper.setBcc(toInternetAddresses(request.getBcc(), charset));
        }
        if (CollUtil.isNotEmpty(request.getReplyTo())) {
            mimeMessage.setReplyTo(toInternetAddresses(request.getReplyTo(), charset));
        }

        helper.setSubject(StrUtil.nullToEmpty(request.getSubject()));
        helper.setSentDate(new Date());

        String content = resolveContent(request);
        boolean html = request.getContentType() == MailContentType.HTML
                || request.getContentType() == MailContentType.TEMPLATE;
        helper.setText(StrUtil.nullToEmpty(content), html);

        fillPriorityHeader(mimeMessage, request.getPriority());
        fillCustomHeaders(mimeMessage, request.getHeaders());
    }

    /**
     * 填充发件人。
     *
     * <p>
     * 不传 from.mailConfig 时使用 spring.mail 默认配置；传入 from.mailConfig 时使用本次请求的动态 SMTP 配置。
     * from.address 不传时默认取当前 SMTP 配置的 username。
     * </p>
     *
     * @param helper  邮件助手
     * @param request 邮件发送请求
     * @param charset 字符编码
     */
    private void fillFrom(MimeMessageHelper helper, MailSendRequest request, String charset) throws Exception {
        Object from = request.getFrom();
        Object dynamicConfig = getDynamicMailConfig(request);

        String username = dynamicConfig == null ? mailProperties.getUsername() : getStringProperty(dynamicConfig, "username");
        String actualAddress = from == null ? username : StrUtil.blankToDefault(getStringProperty(from, "address"), username);
        String actualName = from == null ? null : getStringProperty(from, "name");

        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("SMTP 认证账号不能为空，请检查 spring.mail.username 或 from.mailConfig.username");
        }
        if (StrUtil.isBlank(actualAddress)) {
            throw new IllegalArgumentException("发件邮箱不能为空");
        }

        boolean forceSameAsUsername = dynamicConfig == null
                || ObjectUtil.defaultIfNull(getBooleanProperty(dynamicConfig, "forceFromSameAsUsername"), true);
        if (forceSameAsUsername && !StrUtil.equalsIgnoreCase(actualAddress, username)) {
            throw new IllegalArgumentException(
                    StrUtil.format("发件邮箱必须和当前 SMTP 认证账号一致，from.address={}，username={}", actualAddress, username)
            );
        }

        if (StrUtil.isBlank(actualName)) {
            helper.setFrom(new InternetAddress(actualAddress));
            return;
        }

        helper.setFrom(new InternetAddress(actualAddress, actualName, charset));
    }

    /**
     * 解析邮件发送器。
     *
     * @param request 邮件发送请求
     * @return 邮件发送器
     */
    private JavaMailSender resolveMailSender(MailSendRequest request) {
        Object dynamicConfig = getDynamicMailConfig(request);
        if (dynamicConfig == null) {
            return javaMailSender;
        }

        validateDynamicMailConfig(dynamicConfig);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(getStringProperty(dynamicConfig, "host"));
        sender.setPort(ObjectUtil.defaultIfNull(getIntegerProperty(dynamicConfig, "port"), 25));
        sender.setUsername(getStringProperty(dynamicConfig, "username"));
        sender.setPassword(getStringProperty(dynamicConfig, "password"));
        sender.setDefaultEncoding(StrUtil.blankToDefault(getStringProperty(dynamicConfig, "defaultEncoding"), CharsetUtil.UTF_8));
        sender.setJavaMailProperties(buildJavaMailProperties(dynamicConfig));

        log.info("使用动态 SMTP 配置发送邮件，host={}，port={}，username={}",
                getStringProperty(dynamicConfig, "host"),
                getIntegerProperty(dynamicConfig, "port"),
                getStringProperty(dynamicConfig, "username"));
        return sender;
    }

    /**
     * 解析邮件编码。
     *
     * @param request 邮件发送请求
     * @return 邮件编码
     */
    private String resolveCharset(MailSendRequest request) {
        if (StrUtil.isNotBlank(request.getCharset())) {
            return request.getCharset();
        }

        Object dynamicConfig = getDynamicMailConfig(request);
        if (dynamicConfig != null && StrUtil.isNotBlank(getStringProperty(dynamicConfig, "defaultEncoding"))) {
            return getStringProperty(dynamicConfig, "defaultEncoding");
        }

        if (mailProperties.getDefaultEncoding() != null) {
            return mailProperties.getDefaultEncoding().name();
        }
        return CharsetUtil.UTF_8;
    }

    /**
     * 获取动态 SMTP 配置。
     *
     * @param request 邮件发送请求
     * @return 动态 SMTP 配置
     */
    private Object getDynamicMailConfig(MailSendRequest request) {
        if (request == null || request.getFrom() == null) {
            return null;
        }
        return readProperty(request.getFrom(), "mailConfig");
    }

    /**
     * 校验动态 SMTP 配置。
     *
     * @param dynamicConfig 动态 SMTP 配置
     */
    private void validateDynamicMailConfig(Object dynamicConfig) {
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "host"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 host 不能为空");
        }
        if (ObjectUtil.isNull(getIntegerProperty(dynamicConfig, "port"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 port 不能为空");
        }
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "username"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 username 不能为空");
        }
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "password"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 password 不能为空");
        }
    }

    /**
     * 构建 JavaMail 属性。
     *
     * @param dynamicConfig 动态 SMTP 配置
     * @return JavaMail 属性
     */
    private Properties buildJavaMailProperties(Object dynamicConfig) {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", String.valueOf(ObjectUtil.defaultIfNull(getBooleanProperty(dynamicConfig, "auth"), true)));
        putIfNotNull(properties, "mail.smtp.starttls.enable", getBooleanProperty(dynamicConfig, "starttlsEnable"));
        putIfNotNull(properties, "mail.smtp.starttls.required", getBooleanProperty(dynamicConfig, "starttlsRequired"));
        putIfNotNull(properties, "mail.smtp.ssl.enable", getBooleanProperty(dynamicConfig, "sslEnable"));
        putIfNotNull(properties, "mail.smtp.connectiontimeout", getIntegerProperty(dynamicConfig, "connectionTimeout"));
        putIfNotNull(properties, "mail.smtp.timeout", getIntegerProperty(dynamicConfig, "timeout"));
        putIfNotNull(properties, "mail.smtp.writetimeout", getIntegerProperty(dynamicConfig, "writeTimeout"));

        Object extraProperties = readProperty(dynamicConfig, "properties");
        if (extraProperties instanceof Properties mailProperties) {
            properties.putAll(mailProperties);
        } else if (extraProperties instanceof Map<?, ?> mailPropertiesMap) {
            mailPropertiesMap.forEach((key, value) -> {
                if (key != null && value != null) {
                    properties.put(String.valueOf(key), String.valueOf(value));
                }
            });
        }

        return properties;
    }

    /**
     * 设置非空 JavaMail 属性。
     *
     * @param properties 属性集合
     * @param key        属性键
     * @param value      属性值
     */
    private void putIfNotNull(Properties properties, String key, Object value) {
        if (ObjectUtil.isNotNull(value)) {
            properties.put(key, String.valueOf(value));
        }
    }

    /**
     * 读取字符串属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 字符串属性值
     */
    private String getStringProperty(Object target, String property) {
        Object value = readProperty(target, property);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取整数属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 整数属性值
     */
    private Integer getIntegerProperty(Object target, String property) {
        Object value = readProperty(target, property);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? null : Integer.valueOf(text);
    }

    /**
     * 读取布尔属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 布尔属性值
     */
    private Boolean getBooleanProperty(Object target, String property) {
        Object value = readProperty(target, property);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? null : Boolean.valueOf(text);
    }

    /**
     * 读取对象属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 属性值
     */
    private Object readProperty(Object target, String property) {
        if (target == null || StrUtil.isBlank(property)) {
            return null;
        }

        String suffix = StrUtil.upperFirst(property);
        Object value = invokeGetter(target, "get" + suffix);
        if (value != null) {
            return value;
        }

        value = invokeGetter(target, "is" + suffix);
        if (value != null) {
            return value;
        }

        try {
            Field field = target.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 调用 Getter 方法。
     *
     * @param target     目标对象
     * @param methodName 方法名称
     * @return Getter 返回值
     */
    private Object invokeGetter(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析邮件正文。
     *
     * @param request 邮件发送请求
     * @return 邮件正文
     */
    private String resolveContent(MailSendRequest request) {
        if (request.getContentType() == MailContentType.TEMPLATE) {
            return renderTemplate(request.getTemplateName(), request.getTemplateVariables());
        }
        return request.getContent();
    }

    /**
     * 添加附件。
     *
     * @param helper      邮件助手
     * @param attachments 附件列表
     */
    private void addAttachments(MimeMessageHelper helper, List<MailAttachment> attachments) throws Exception {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }

        for (MailAttachment attachment : attachments) {
            String fileName = attachment.getFileName();
            String contentType = StrUtil.blankToDefault(attachment.getContentType(), DEFAULT_CONTENT_TYPE);

            if (StrUtil.isNotBlank(attachment.getFilePath())) {
                helper.addAttachment(fileName, new FileSystemResource(FileUtil.file(attachment.getFilePath())), contentType);
            } else {
                helper.addAttachment(fileName, buildByteArrayResource(attachment.getContent(), fileName), contentType);
            }
        }
    }

    /**
     * 添加 HTML 内嵌资源。
     *
     * @param helper          邮件助手
     * @param inlineResources 内嵌资源列表
     */
    private void addInlineResources(MimeMessageHelper helper, List<MailInlineResource> inlineResources) throws Exception {
        if (CollUtil.isEmpty(inlineResources)) {
            return;
        }

        for (MailInlineResource resource : inlineResources) {
            String contentType = StrUtil.blankToDefault(resource.getContentType(), DEFAULT_CONTENT_TYPE);

            if (StrUtil.isNotBlank(resource.getFilePath())) {
                helper.addInline(resource.getContentId(), new FileSystemResource(FileUtil.file(resource.getFilePath())), contentType);
            } else {
                InputStreamSource source = buildByteArrayResource(resource.getContent(), resource.getName());
                helper.addInline(resource.getContentId(), source, contentType);
            }
        }
    }

    /**
     * 填充邮件优先级请求头。
     *
     * @param mimeMessage MIME 消息
     * @param priority    邮件优先级
     */
    private void fillPriorityHeader(MimeMessage mimeMessage, MailPriority priority) throws Exception {
        MailPriority mailPriority = ObjectUtil.defaultIfNull(priority, MailPriority.NORMAL);
        switch (mailPriority) {
            case HIGH -> {
                mimeMessage.setHeader("X-Priority", "1");
                mimeMessage.setHeader("Importance", "High");
            }
            case LOW -> {
                mimeMessage.setHeader("X-Priority", "5");
                mimeMessage.setHeader("Importance", "Low");
            }
            case NORMAL -> {
                mimeMessage.setHeader("X-Priority", "3");
                mimeMessage.setHeader("Importance", "Normal");
            }
        }
    }

    /**
     * 填充自定义请求头。
     *
     * @param mimeMessage MIME 消息
     * @param headers     自定义请求头
     */
    private void fillCustomHeaders(MimeMessage mimeMessage, Map<String, String> headers) throws Exception {
        if (MapUtil.isEmpty(headers)) {
            return;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (StrUtil.isNotBlank(entry.getKey()) && StrUtil.isNotBlank(entry.getValue())) {
                mimeMessage.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 校验批量请求。
     *
     * @param request 批量邮件发送请求
     */
    private void validateBatch(MailBatchSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("批量邮件发送请求不能为空");
        }

        Set<ConstraintViolation<MailBatchSendRequest>> violations = validator.validate(request);
        if (CollUtil.isNotEmpty(violations)) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("批量邮件发送请求参数不合法");
            throw new IllegalArgumentException(message);
        }

        if (CollUtil.isEmpty(request.getMessages())) {
            throw new IllegalArgumentException("批量邮件列表不能为空");
        }
    }

    /**
     * 校验附件参数。
     *
     * @param attachments 附件列表
     */
    private void validateAttachments(List<MailAttachment> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }

        for (MailAttachment attachment : attachments) {
            if (attachment == null) {
                throw new IllegalArgumentException("附件信息不能为空");
            }
            if (StrUtil.isBlank(attachment.getFileName())) {
                throw new IllegalArgumentException("附件文件名不能为空");
            }
            if (StrUtil.isBlank(attachment.getFilePath()) && ArrayUtil.isEmpty(attachment.getContent())) {
                throw new IllegalArgumentException("附件文件路径和字节内容不能同时为空");
            }
            if (StrUtil.isNotBlank(attachment.getFilePath()) && !FileUtil.exist(attachment.getFilePath())) {
                throw new IllegalArgumentException("附件文件不存在：" + attachment.getFilePath());
            }
        }
    }

    /**
     * 校验内嵌资源参数。
     *
     * @param inlineResources 内嵌资源列表
     */
    private void validateInlineResources(List<MailInlineResource> inlineResources) {
        if (CollUtil.isEmpty(inlineResources)) {
            return;
        }

        for (MailInlineResource resource : inlineResources) {
            if (resource == null) {
                throw new IllegalArgumentException("内嵌资源信息不能为空");
            }
            if (StrUtil.isBlank(resource.getContentId())) {
                throw new IllegalArgumentException("内嵌资源 contentId 不能为空");
            }
            if (StrUtil.isBlank(resource.getFilePath()) && ArrayUtil.isEmpty(resource.getContent())) {
                throw new IllegalArgumentException("内嵌资源文件路径和字节内容不能同时为空");
            }
            if (StrUtil.isNotBlank(resource.getFilePath()) && !FileUtil.exist(resource.getFilePath())) {
                throw new IllegalArgumentException("内嵌资源文件不存在：" + resource.getFilePath());
            }
        }
    }

    /**
     * 构建批量发送汇总结果。
     *
     * @param batchBizId 批量业务编号
     * @param totalCount 总数量
     * @param results    明细结果
     * @return 批量发送汇总结果
     */
    private MailSendSummary buildSummary(String batchBizId, int totalCount, List<MailSendResult> results) {
        int successCount = (int) results.stream()
                .filter(result -> BooleanUtil.isTrue(result.getSuccess()))
                .count();

        return MailSendSummary.builder()
                .batchBizId(batchBizId)
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(results.size() - successCount)
                .results(results)
                .finishTime(LocalDateTime.now())
                .build();
    }

    /**
     * 构建失败结果。
     *
     * @param request 邮件发送请求
     * @param ex      异常
     * @return 失败结果
     */
    private MailSendResult buildFailureResult(MailSendRequest request, Exception ex) {
        return MailSendResult.builder()
                .bizId(request == null ? null : request.getBizId())
                .subject(request == null ? null : request.getSubject())
                .recipients(request == null ? CollUtil.newArrayList() : toAddressList(request.getTo()))
                .success(false)
                .errorCode(ex.getClass().getSimpleName())
                .errorMessage(ExceptionUtil.getRootCauseMessage(ex))
                .sendTime(LocalDateTime.now())
                .build();
    }

    /**
     * 转换联系人地址列表。
     *
     * @param contacts 联系人列表
     * @return 邮箱地址列表
     */
    private List<String> toAddressList(List<MailContact> contacts) {
        if (CollUtil.isEmpty(contacts)) {
            return CollUtil.newArrayList();
        }

        return contacts.stream()
                .filter(ObjectUtil::isNotNull)
                .map(MailContact::getAddress)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 转换联系人为 InternetAddress 数组。
     *
     * @param contacts 联系人列表
     * @param charset  字符编码
     * @return InternetAddress 数组
     */
    private InternetAddress[] toInternetAddresses(List<MailContact> contacts, String charset) {
        if (CollUtil.isEmpty(contacts)) {
            return new InternetAddress[0];
        }

        return contacts.stream()
                .filter(ObjectUtil::isNotNull)
                .filter(contact -> StrUtil.isNotBlank(contact.getAddress()))
                .map(contact -> toInternetAddress(contact, charset))
                .toArray(InternetAddress[]::new);
    }

    /**
     * 转换联系人为 InternetAddress。
     *
     * @param contact 联系人
     * @param charset 字符编码
     * @return InternetAddress
     */
    private InternetAddress toInternetAddress(MailContact contact, String charset) {
        try {
            if (StrUtil.isBlank(contact.getName())) {
                return new InternetAddress(contact.getAddress());
            }
            return new InternetAddress(contact.getAddress(), contact.getName(), charset);
        } catch (AddressException | UnsupportedEncodingException ex) {
            throw new IllegalArgumentException("邮件联系人编码失败：" + contact.getAddress(), ex);
        }
    }

    /**
     * 构建字节资源。
     *
     * @param content  字节内容
     * @param fileName 文件名
     * @return 字节资源
     */
    private ByteArrayResource buildByteArrayResource(byte[] content, String fileName) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }
}
```

### 邮件服务使用示例

```java
package io.github.atengk.mail.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;
import io.github.atengk.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务接口。
 *
 * <p>
 * 邮件发送请求不传 from.mailConfig 时，默认使用 spring.mail 配置发送；
 * 传入 from.mailConfig 时，本次请求使用动态 SMTP 配置发送。
 * </p>
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final MailService mailService;

    /**
     * 默认邮件异步线程池。
     */
    @Qualifier("mailTaskExecutor")
    private final Executor mailTaskExecutor;

    /**
     * 高优先级邮件异步线程池，可选注入。
     */
    @Qualifier("highPriorityMailTaskExecutor")
    private final ObjectProvider<Executor> highPriorityMailTaskExecutorProvider;

    /**
     * 低优先级邮件异步线程池，可选注入。
     */
    @Qualifier("lowPriorityMailTaskExecutor")
    private final ObjectProvider<Executor> lowPriorityMailTaskExecutorProvider;

        /**
     * 发送邮件，根据 contentType 自动选择纯文本、HTML 或模板发送。
     *
     * <pre>
     * # 不传 from.mailConfig，使用 spring.mail 默认配置发送
     * curl -X POST 'http://localhost:8080/api/mail/send' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-001",
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "默认邮箱发送",
     *     "content": "这封邮件使用 spring.mail 默认配置发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # 传入 from.mailConfig，本次请求使用动态 SMTP 配置发送
     * curl -X POST 'http://localhost:8080/api/mail/send' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-DYNAMIC-001",
     *     "from": {
     *       "address": "sender@example.com",
     *       "name": "动态发件人",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "defaultEncoding": "UTF-8",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "starttlsRequired": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "动态邮箱发送",
     *     "content": "这封邮件使用 from.mailConfig 动态 SMTP 配置发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send")
    public ResponseEntity<MailSendResult> send(@RequestBody MailSendRequest request) {
        log.info("接收邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.send(request));
    }

        /**
     * 发送纯文本邮件。
     *
     * <pre>
     * # from 为空时使用 spring.mail 默认配置
     * curl -X POST 'http://localhost:8080/api/mail/send/text' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEXT-001",
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "纯文本邮件",
     *     "content": "这是一封纯文本邮件",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # 如需临时切换 SMTP 账号，可在 from.mailConfig 中传入动态配置
     * curl -X POST 'http://localhost:8080/api/mail/send/text' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEXT-DYNAMIC-001",
     *     "from": {
     *       "name": "文本通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "动态纯文本邮件",
     *     "content": "这是一封使用动态 SMTP 配置发送的纯文本邮件",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/text")
    public ResponseEntity<MailSendResult> sendText(@RequestBody MailSendRequest request) {
        log.info("接收纯文本邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.sendText(request));
    }

        /**
     * 发送 HTML 邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/html' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-HTML-001",
     *     "from": {
     *       "name": "HTML 通知"
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "HTML 邮件",
     *     "content": "<h2>你好</h2><p>这是一封 HTML 邮件</p>",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # from.name 只覆盖发件人显示名；如需切换 SMTP 账号，传入 from.mailConfig
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/html")
    public ResponseEntity<MailSendResult> sendHtml(@RequestBody MailSendRequest request) {
        log.info("接收 HTML 邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.sendHtml(request));
    }

        /**
     * 发送模板邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/template' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEMPLATE-001",
     *     "from": {
     *       "name": "模板通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "defaultEncoding": "UTF-8",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "模板邮件",
     *     "templateName": "mail/welcome.html",
     *     "templateVariables": {
     *       "username": "Ateng",
     *       "loginUrl": "https://example.com/login"
     *     },
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/template")
    public ResponseEntity<MailSendResult> sendTemplate(@RequestBody MailSendRequest request) {
        log.info("接收模板邮件发送请求，业务编号：{}，主题：{}，模板：{}",
                request.getBizId(), request.getSubject(), request.getTemplateName());
        return ResponseEntity.ok(mailService.sendTemplate(request));
    }

        /**
     * 批量发送邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-001-1",
     *         "from": {
     *           "name": "批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "批量邮件 1",
     *         "content": "这是第一封使用动态 SMTP 配置的批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       },
     *       {
     *         "bizId": "MAIL-BATCH-001-2",
     *         "to": [
     *           {
     *             "address": "user2@example.com",
     *             "name": "用户2"
     *           }
     *         ],
     *         "subject": "批量邮件 2",
     *         "content": "这封邮件未传 from.mailConfig，使用 spring.mail 默认配置",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    @PostMapping("/send/batch")
    public ResponseEntity<MailSendSummary> sendBatch(@RequestBody MailBatchSendRequest request) {
        log.info("接收批量邮件发送请求，批量编号：{}，数量：{}",
                request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return ResponseEntity.ok(mailService.sendBatch(request));
    }

        /**
     * 异步发送邮件，使用默认邮件线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/async' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-001",
     *     "from": {
     *       "name": "异步通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "异步邮件",
     *     "content": "这是一封使用动态 SMTP 配置发送的异步邮件",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    @PostMapping("/send/async")
    public CompletableFuture<ResponseEntity<MailSendResult>> sendAsync(@RequestBody MailSendRequest request) {
        log.info("接收异步邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return mailService.sendAsync(request).thenApply(ResponseEntity::ok);
    }

        /**
     * 提交异步邮件发送任务，接口立即返回。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/submit' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-SUBMIT-001",
     *     "from": {
     *       "name": "后台发送通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "异步提交邮件",
     *     "content": "接口立即返回，邮件后台发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 提交结果
     */
    @PostMapping("/send/submit")
    public ResponseEntity<Map<String, Object>> submitSend(@RequestBody MailSendRequest request) {
        log.info("提交异步邮件发送任务，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());

        mailService.sendAsync(request)
                .thenAccept(result -> {
                    if (Boolean.TRUE.equals(result.getSuccess())) {
                        log.info("异步邮件发送成功，业务编号：{}，消息编号：{}", result.getBizId(), result.getMessageId());
                        return;
                    }
                    log.warn("异步邮件发送失败，业务编号：{}，原因：{}", result.getBizId(), result.getErrorMessage());
                })
                .exceptionally(ex -> {
                    log.error("异步邮件发送异常，业务编号：{}，原因：{}", request.getBizId(), ex.getMessage(), ex);
                    return null;
                });

        return ResponseEntity.accepted().body(MapUtil.<String, Object>builder()
                .put("submitted", true)
                .put("bizId", request.getBizId())
                .put("message", "邮件发送任务已提交")
                .build());
    }

        /**
     * 异步发送邮件，使用指定线程池。
     *
     * <pre>
     * # executorType 支持 default、high、low；from.mailConfig 控制本次使用的 SMTP 配置
     * curl -X POST 'http://localhost:8080/api/mail/send/async/executor/default' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-EXECUTOR-001",
     *     "from": {
     *       "name": "指定线程池通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "指定线程池异步邮件",
     *     "content": "这是一封使用指定线程池和动态 SMTP 配置发送的异步邮件",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * curl -X POST 'http://localhost:8080/api/mail/send/async/executor/high' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-HIGH-001",
     *     "to": [
     *       {
     *         "address": "vip@example.com",
     *         "name": "重要用户"
     *       }
     *     ],
     *     "subject": "高优先级异步邮件",
     *     "content": "未传 from.mailConfig 时使用 spring.mail 默认配置",
     *     "contentType": "TEXT",
     *     "priority": "HIGH",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param executorType 线程池类型，支持 default、high、low
     * @param request      邮件发送请求
     * @return 异步邮件发送结果
     */
    @PostMapping("/send/async/executor/{executorType}")
    public CompletableFuture<ResponseEntity<MailSendResult>> sendAsyncWithExecutor(@PathVariable String executorType,
                                                                                   @RequestBody MailSendRequest request) {
        Executor executor = resolveExecutor(executorType);
        log.info("接收指定线程池异步邮件发送请求，线程池类型：{}，业务编号：{}，主题：{}",
                executorType, request.getBizId(), request.getSubject());
        return mailService.sendAsync(request, executor).thenApply(ResponseEntity::ok);
    }

        /**
     * 异步批量发送邮件，使用默认邮件线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-ASYNC-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-ASYNC-001-1",
     *         "from": {
     *           "name": "异步批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "异步批量邮件 1",
     *         "content": "这是第一封使用动态 SMTP 配置的异步批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @PostMapping("/send/batch/async")
    public CompletableFuture<ResponseEntity<MailSendSummary>> sendBatchAsync(@RequestBody MailBatchSendRequest request) {
        log.info("接收异步批量邮件发送请求，批量编号：{}，数量：{}",
                request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return mailService.sendBatchAsync(request).thenApply(ResponseEntity::ok);
    }

        /**
     * 异步批量发送邮件，使用指定线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async/executor/default' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-EXECUTOR-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-EXECUTOR-001-1",
     *         "from": {
     *           "name": "指定线程池批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "指定线程池批量邮件 1",
     *         "content": "这是第一封指定线程池和动态 SMTP 配置的批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     *
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async/executor/low' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-LOW-001",
     *     "batchSize": 100,
     *     "batchInterval": "PT2S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-LOW-001-1",
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "低优先级批量邮件",
     *         "content": "未传 from.mailConfig 时使用 spring.mail 默认配置",
     *         "contentType": "TEXT",
     *         "priority": "LOW",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param executorType 线程池类型，支持 default、high、low
     * @param request      批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @PostMapping("/send/batch/async/executor/{executorType}")
    public CompletableFuture<ResponseEntity<MailSendSummary>> sendBatchAsyncWithExecutor(@PathVariable String executorType,
                                                                                         @RequestBody MailBatchSendRequest request) {
        Executor executor = resolveExecutor(executorType);
        log.info("接收指定线程池异步批量邮件发送请求，线程池类型：{}，批量编号：{}，数量：{}",
                executorType, request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return mailService.sendBatchAsync(request, executor).thenApply(ResponseEntity::ok);
    }

    /**
     * 渲染邮件模板。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/template/render' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "templateName": "mail/welcome.html",
     *     "variables": {
     *       "username": "Ateng",
     *       "loginUrl": "https://example.com/login"
     *     }
     *   }'
     * </pre>
     *
     * @param request 模板渲染请求
     * @return 渲染后的模板内容
     */
    @PostMapping("/template/render")
    public ResponseEntity<String> renderTemplate(@RequestBody TemplateRenderRequest request) {
        log.info("接收邮件模板渲染请求，模板：{}", request.templateName());
        return ResponseEntity.ok(mailService.renderTemplate(request.templateName(), request.variables()));
    }

        /**
     * 校验邮件发送请求。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/validate' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-VALIDATE-001",
     *     "from": {
     *       "name": "校验通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "校验邮件",
     *     "content": "这是需要校验的邮件内容",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 校验结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody MailSendRequest request) {
        mailService.validate(request);
        log.info("邮件发送请求校验通过，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(MapUtil.<String, Object>builder()
                .put("valid", true)
                .put("message", "邮件发送请求校验通过")
                .build());
    }

    /**
     * 检查邮件服务器连接。
     *
     * <pre>
     * curl -X GET 'http://localhost:8080/api/mail/connection'
     * </pre>
     *
     * @return 连接检查结果
     */
    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> checkConnection() {
        boolean connected = mailService.checkConnection();
        log.info("邮件服务器连接检查完成，结果：{}", connected);
        return ResponseEntity.ok(MapUtil.<String, Object>builder()
                .put("connected", connected)
                .put("message", connected ? "邮件服务器连接正常" : "邮件服务器连接异常")
                .build());
    }

    /**
     * 解析异步线程池。
     *
     * @param executorType 线程池类型
     * @return 线程池
     */
    private Executor resolveExecutor(String executorType) {
        String type = StrUtil.blankToDefault(executorType, "default").toLowerCase();

        return switch (type) {
            case "default", "mail" -> mailTaskExecutor;
            case "high" -> highPriorityMailTaskExecutorProvider.getIfAvailable(() -> mailTaskExecutor);
            case "low" -> lowPriorityMailTaskExecutorProvider.getIfAvailable(() -> mailTaskExecutor);
            default -> throw new IllegalArgumentException("不支持的邮件线程池类型：" + executorType);
        };
    }

    /**
     * 邮件模板渲染请求。
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     */
    public record TemplateRenderRequest(String templateName, Map<String, Object> variables) {
    }
}
```

