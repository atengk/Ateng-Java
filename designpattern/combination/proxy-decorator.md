# 代理模式 + 装饰器模式

代理模式和装饰器模式组合使用时，通常用于处理 **既要控制对象访问，又要在不修改原对象的前提下增强对象能力** 的业务场景。

代理模式负责控制真实对象的访问，例如权限校验、风控校验、访问限制、远程调用代理、缓存代理等；装饰器模式负责在不改变原对象代码的前提下增强功能，例如增加水印、审计、日志、脱敏、加密、压缩等。

```text
代理模式：控制对象访问
装饰器模式：增强对象能力
```

## 适用场景

本示例以“合同文件下载”为业务场景。用户下载合同时，系统既要判断用户是否有权限访问合同文件，又要对下载结果增加水印、审计等增强能力。

| 需求                   | 适合模式          | 说明                             |
| ---------------------- | ----------------- | -------------------------------- |
| 判断用户是否能下载合同 | 代理模式          | 下载前进行权限校验和访问控制     |
| 给合同文件增加水印信息 | 装饰器模式        | 不修改原始下载逻辑，额外增强响应 |
| 记录下载审计流水       | 装饰器模式        | 不影响核心下载逻辑，增加审计信息 |
| 统一对外暴露下载能力   | 代理 + 装饰器组合 | 调用方只依赖同一个接口           |

如果把所有逻辑都写在一个 Service 中，代码容易变成：

```text
Service
  -> 校验用户权限
  -> 校验合同状态
  -> 生成下载地址
  -> 添加水印
  -> 记录审计
  -> 返回下载结果
```

这种写法的问题是：访问控制、核心下载逻辑、增强逻辑全部混在一起。后续如果要新增脱敏、压缩、加密、审计等能力，Service 会越来越重。

使用“代理模式 + 装饰器模式”后，结构变成：

```text
Controller
  -> ApplicationService
      -> ContractFileAccessProxy：控制访问权限
          -> AuditContractFileDecorator：增加审计能力
              -> WatermarkContractFileDecorator：增加水印能力
                  -> DefaultContractFileService：真实合同文件下载对象
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、集合、对象、ID、日期等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/proxydecorator
├── component
│   └── ContractFileGenerator.java
├── config
│   └── ContractFileServiceConfig.java
├── controller
│   └── ContractDownloadController.java
├── decorator
│   ├── AbstractContractFileServiceDecorator.java
│   ├── AuditContractFileDecorator.java
│   └── WatermarkContractFileDecorator.java
├── dto
│   ├── ContractDownloadRequest.java
│   └── ContractDownloadResponse.java
├── handler
│   └── GlobalExceptionHandler.java
├── proxy
│   └── ContractFileAccessProxy.java
├── result
│   └── Result.java
├── service
│   ├── ContractDownloadService.java
│   ├── ContractFileService.java
│   └── impl
│       ├── ContractDownloadServiceImpl.java
│       └── DefaultContractFileService.java
└── support
    └── ContractAccessChecker.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
ContractFileAccessProxy：代理对象，负责访问控制
AbstractContractFileServiceDecorator：装饰器基类，负责扩展增强能力
```

代理对象和装饰器对象都实现同一个 `ContractFileService` 接口，因此调用方不需要关心当前调用的是代理、装饰器还是真实对象。

### 合同下载请求对象

合同下载请求对象用于接收用户下载合同文件时的参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/dto/ContractDownloadRequest.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合同下载请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ContractDownloadRequest {

    /**
     * 合同编号
     */
    @NotBlank(message = "合同编号不能为空")
    private String contractNo;

    /**
     * 当前登录用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 合同所属用户ID
     */
    @NotNull(message = "合同所属用户ID不能为空")
    private Long ownerUserId;

    /**
     * 用户角色：USER、ADMIN
     */
    @NotBlank(message = "用户角色不能为空")
    private String userRole;

    /**
     * 合同状态：ACTIVE、LOCKED
     */
    @NotBlank(message = "合同状态不能为空")
    private String contractStatus;
}
```

### 合同下载响应对象

合同下载响应对象用于返回文件地址、水印信息、审计流水和增强特性。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/dto/ContractDownloadResponse.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同下载响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ContractDownloadResponse {

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件下载地址
     */
    private String downloadUrl;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 水印文本
     */
    private String watermarkText;

    /**
     * 审计流水号
     */
    private String auditNo;

    /**
     * 是否允许下载
     */
    private Boolean allowed;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 已增强功能
     */
    @Builder.Default
    private List<String> enhancedFeatures = new ArrayList<>();
}
```

### 合同文件服务接口

合同文件服务接口是代理对象、装饰器对象和真实对象共同实现的接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/service/ContractFileService.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.service;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;

/**
 * 合同文件服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ContractFileService {

    /**
     * 下载合同文件
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    ContractDownloadResponse download(ContractDownloadRequest request);
}
```

### 合同文件生成组件

合同文件生成组件用于模拟生成合同下载地址。真实项目中这里可以对接文件服务、对象存储、MinIO、OSS 或内部文档系统。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/component/ContractFileGenerator.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.component;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合同文件生成组件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ContractFileGenerator {

    /**
     * 生成合同文件名
     *
     * @param request 合同下载请求
     * @return 合同文件名
     */
    public String generateFileName(ContractDownloadRequest request) {
        return StrUtil.format("contract_{}.pdf", request.getContractNo());
    }

    /**
     * 生成合同下载地址
     *
     * @param request 合同下载请求
     * @return 合同下载地址
     */
    public String generateDownloadUrl(ContractDownloadRequest request) {
        String downloadUrl = StrUtil.format("https://file.example.com/contracts/{}.pdf", request.getContractNo());
        log.info("生成合同下载地址，合同编号：{}，下载地址：{}", request.getContractNo(), downloadUrl);
        return downloadUrl;
    }
}
```

### 真实合同文件服务

真实对象只负责核心下载逻辑，不处理访问控制，也不处理水印、审计等增强能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/service/impl/DefaultContractFileService.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.service.impl;

import io.github.atengk.pattern.combination.proxydecorator.component.ContractFileGenerator;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认合同文件服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultContractFileService implements ContractFileService {

    private final ContractFileGenerator contractFileGenerator;

    /**
     * 下载合同文件
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        String fileName = contractFileGenerator.generateFileName(request);
        String downloadUrl = contractFileGenerator.generateDownloadUrl(request);

        log.info("真实合同文件服务生成下载结果，合同编号：{}，文件名称：{}",
                request.getContractNo(), fileName);

        return ContractDownloadResponse.builder()
                .contractNo(request.getContractNo())
                .fileName(fileName)
                .downloadUrl(downloadUrl)
                .contentType("application/pdf")
                .allowed(true)
                .message("合同文件下载地址生成成功")
                .build();
    }
}
```

### 装饰器抽象类

装饰器抽象类持有一个 `ContractFileService` 委托对象，默认把调用转发给委托对象。具体装饰器在调用前后增加自己的增强逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/decorator/AbstractContractFileServiceDecorator.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.decorator;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;

/**
 * 合同文件服务装饰器抽象类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public abstract class AbstractContractFileServiceDecorator implements ContractFileService {

    protected final ContractFileService delegate;

    /**
     * 初始化合同文件服务装饰器
     *
     * @param delegate 被装饰的合同文件服务
     */
    protected AbstractContractFileServiceDecorator(ContractFileService delegate) {
        this.delegate = delegate;
    }

    /**
     * 下载合同文件
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        return delegate.download(request);
    }
}
```

### 水印装饰器

水印装饰器在不修改真实合同文件服务代码的前提下，为下载结果增加水印信息。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/decorator/WatermarkContractFileDecorator.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.decorator;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 合同文件水印装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class WatermarkContractFileDecorator extends AbstractContractFileServiceDecorator {

    /**
     * 初始化水印装饰器
     *
     * @param delegate 被装饰的合同文件服务
     */
    public WatermarkContractFileDecorator(ContractFileService delegate) {
        super(delegate);
    }

    /**
     * 下载合同文件并增加水印信息
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        ContractDownloadResponse response = super.download(request);
        String watermarkText = StrUtil.format("user:{} time:{}",
                request.getUserId(), DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));

        response.setWatermarkText(watermarkText);
        response.getEnhancedFeatures().add("WATERMARK");

        log.info("合同文件水印增强完成，合同编号：{}，水印：{}",
                request.getContractNo(), watermarkText);

        return response;
    }
}
```

### 审计装饰器

审计装饰器为下载结果增加审计流水号。真实项目中这里可以写入审计表、日志系统或消息队列。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/decorator/AuditContractFileDecorator.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.decorator;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.extern.slf4j.Slf4j;

/**
 * 合同文件审计装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AuditContractFileDecorator extends AbstractContractFileServiceDecorator {

    /**
     * 初始化审计装饰器
     *
     * @param delegate 被装饰的合同文件服务
     */
    public AuditContractFileDecorator(ContractFileService delegate) {
        super(delegate);
    }

    /**
     * 下载合同文件并增加审计信息
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        ContractDownloadResponse response = super.download(request);
        String auditNo = "AUD" + IdUtil.getSnowflakeNextIdStr();

        response.setAuditNo(auditNo);
        response.getEnhancedFeatures().add("AUDIT");

        log.info("合同文件下载审计完成，审计流水号：{}，合同编号：{}，用户ID：{}",
                auditNo, request.getContractNo(), request.getUserId());

        return response;
    }
}
```

### 合同访问校验组件

访问校验组件用于判断用户是否可以下载合同。这里用简单规则模拟权限判断。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/support/ContractAccessChecker.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.support;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合同访问校验组件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ContractAccessChecker {

    /**
     * 校验合同下载权限
     *
     * @param request 合同下载请求
     */
    public void checkDownloadPermission(ContractDownloadRequest request) {
        if (StrUtil.isBlank(request.getContractNo())) {
            throw new IllegalArgumentException("合同编号不能为空");
        }
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (ObjectUtil.isNull(request.getOwnerUserId())) {
            throw new IllegalArgumentException("合同所属用户ID不能为空");
        }
        if (StrUtil.equalsIgnoreCase(request.getContractStatus(), "LOCKED")) {
            throw new IllegalArgumentException("合同已锁定，不允许下载");
        }

        boolean isOwner = ObjectUtil.equal(request.getUserId(), request.getOwnerUserId());
        boolean isAdmin = StrUtil.equalsIgnoreCase(request.getUserRole(), "ADMIN");
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("当前用户无权下载该合同");
        }

        log.info("合同下载权限校验通过，合同编号：{}，用户ID：{}，角色：{}",
                request.getContractNo(), request.getUserId(), request.getUserRole());
    }
}
```

### 合同文件访问代理

访问代理负责在调用真实下载链路前执行权限校验。只有校验通过后，才会继续调用后续装饰器和真实对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/proxy/ContractFileAccessProxy.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.proxy;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import io.github.atengk.pattern.combination.proxydecorator.support.ContractAccessChecker;
import lombok.extern.slf4j.Slf4j;

/**
 * 合同文件访问代理
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ContractFileAccessProxy implements ContractFileService {

    private final ContractFileService delegate;

    private final ContractAccessChecker contractAccessChecker;

    /**
     * 初始化合同文件访问代理
     *
     * @param delegate              被代理的合同文件服务
     * @param contractAccessChecker 合同访问校验组件
     */
    public ContractFileAccessProxy(ContractFileService delegate, ContractAccessChecker contractAccessChecker) {
        this.delegate = delegate;
        this.contractAccessChecker = contractAccessChecker;
    }

    /**
     * 下载合同文件
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        log.info("合同文件访问代理开始处理，合同编号：{}，用户ID：{}",
                request.getContractNo(), request.getUserId());

        contractAccessChecker.checkDownloadPermission(request);

        ContractDownloadResponse response = delegate.download(request);

        log.info("合同文件访问代理处理完成，合同编号：{}，允许下载：{}",
                response.getContractNo(), response.getAllowed());

        return response;
    }
}
```

### 合同文件服务装配配置

配置类负责组装真实对象、装饰器和代理对象。这里的装配顺序是：真实对象先生成下载结果，水印装饰器增加水印，审计装饰器增加审计流水，代理对象在最外层控制访问。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/config/ContractFileServiceConfig.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.config;

import io.github.atengk.pattern.combination.proxydecorator.component.ContractFileGenerator;
import io.github.atengk.pattern.combination.proxydecorator.decorator.AuditContractFileDecorator;
import io.github.atengk.pattern.combination.proxydecorator.decorator.WatermarkContractFileDecorator;
import io.github.atengk.pattern.combination.proxydecorator.proxy.ContractFileAccessProxy;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import io.github.atengk.pattern.combination.proxydecorator.service.impl.DefaultContractFileService;
import io.github.atengk.pattern.combination.proxydecorator.support.ContractAccessChecker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 合同文件服务装配配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
public class ContractFileServiceConfig {

    /**
     * 装配合同文件服务调用链
     *
     * @param contractFileGenerator 合同文件生成组件
     * @param contractAccessChecker 合同访问校验组件
     * @return 合同文件服务
     */
    @Bean
    public ContractFileService contractFileService(ContractFileGenerator contractFileGenerator,
                                                   ContractAccessChecker contractAccessChecker) {
        ContractFileService target = new DefaultContractFileService(contractFileGenerator);
        ContractFileService watermarkDecorator = new WatermarkContractFileDecorator(target);
        ContractFileService auditDecorator = new AuditContractFileDecorator(watermarkDecorator);
        return new ContractFileAccessProxy(auditDecorator, contractAccessChecker);
    }
}
```

这段装配代码体现了代理和装饰器的组合关系：

```text
ContractFileAccessProxy
  -> AuditContractFileDecorator
      -> WatermarkContractFileDecorator
          -> DefaultContractFileService
```

### 应用服务接口

应用服务对外暴露合同下载能力，Controller 不直接依赖代理、装饰器或真实对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/service/ContractDownloadService.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.service;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;

/**
 * 合同下载服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ContractDownloadService {

    /**
     * 下载合同
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    ContractDownloadResponse download(ContractDownloadRequest request);
}
```

### 应用服务实现类

应用服务实现类只依赖 `ContractFileService` 接口，不关心实际注入的是代理对象、装饰器对象还是真实对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/service/impl/ContractDownloadServiceImpl.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.service.impl;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractDownloadService;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 合同下载服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractDownloadServiceImpl implements ContractDownloadService {

    private final ContractFileService contractFileService;

    /**
     * 下载合同
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        ContractDownloadResponse response = contractFileService.download(request);
        log.info("合同下载服务处理完成，合同编号：{}，增强能力：{}",
                response.getContractNo(), response.getEnhancedFeatures());
        return response;
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/result/Result.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    响应编码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

### 全局异常处理器

全局异常处理器用于把访问控制失败、参数异常统一转换成接口响应。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = ObjectUtil.isNotNull(fieldError)
                ? StrUtil.blankToDefault(fieldError.getDefaultMessage(), "请求参数不合法")
                : "请求参数不合法";

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("合同下载业务异常：{}", exception.getMessage());
        return Result.fail(400, exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
```

### Controller

Controller 提供合同下载接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/controller/ContractDownloadController.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.controller;

import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.result.Result;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractDownloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同下载控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
public class ContractDownloadController {

    private final ContractDownloadService contractDownloadService;

    /**
     * 下载合同
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @PostMapping("/download")
    public Result<ContractDownloadResponse> download(@Valid @RequestBody ContractDownloadRequest request) {
        return Result.success(contractDownloadService.download(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过合同下载接口验证代理模式和装饰器模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/contracts/download
Content-Type：application/json
```

合同所属用户下载请求示例：

```bash
curl -X POST "http://localhost:8080/api/contracts/download" \
  -H "Content-Type: application/json" \
  -d '{
    "contractNo": "CT202605130001",
    "userId": 1001,
    "ownerUserId": 1001,
    "userRole": "USER",
    "contractStatus": "ACTIVE"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "contractNo": "CT202605130001",
    "fileName": "contract_CT202605130001.pdf",
    "downloadUrl": "https://file.example.com/contracts/CT202605130001.pdf",
    "contentType": "application/pdf",
    "watermarkText": "user:1001 time:2026-05-13 14:30:00",
    "auditNo": "AUD1988267712300011520",
    "allowed": true,
    "message": "合同文件下载地址生成成功",
    "enhancedFeatures": [
      "WATERMARK",
      "AUDIT"
    ]
  }
}
```

管理员下载其他用户合同请求示例：

```bash
curl -X POST "http://localhost:8080/api/contracts/download" \
  -H "Content-Type: application/json" \
  -d '{
    "contractNo": "CT202605130002",
    "userId": 9001,
    "ownerUserId": 1002,
    "userRole": "ADMIN",
    "contractStatus": "ACTIVE"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "contractNo": "CT202605130002",
    "fileName": "contract_CT202605130002.pdf",
    "downloadUrl": "https://file.example.com/contracts/CT202605130002.pdf",
    "contentType": "application/pdf",
    "watermarkText": "user:9001 time:2026-05-13 14:31:00",
    "auditNo": "AUD1988267712300011521",
    "allowed": true,
    "message": "合同文件下载地址生成成功",
    "enhancedFeatures": [
      "WATERMARK",
      "AUDIT"
    ]
  }
}
```

无权限下载请求示例：

```bash
curl -X POST "http://localhost:8080/api/contracts/download" \
  -H "Content-Type: application/json" \
  -d '{
    "contractNo": "CT202605130003",
    "userId": 1003,
    "ownerUserId": 1004,
    "userRole": "USER",
    "contractStatus": "ACTIVE"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "当前用户无权下载该合同",
  "data": null
}
```

合同锁定请求示例：

```bash
curl -X POST "http://localhost:8080/api/contracts/download" \
  -H "Content-Type: application/json" \
  -d '{
    "contractNo": "CT202605130004",
    "userId": 1001,
    "ownerUserId": 1001,
    "userRole": "USER",
    "contractStatus": "LOCKED"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "合同已锁定，不允许下载",
  "data": null
}
```

## 新增装饰器

当业务需要新增一个增强能力时，例如“下载链接加密”，不需要修改真实合同文件服务，也不需要修改访问代理，只需要新增一个装饰器，并调整装配顺序。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/decorator/EncryptUrlContractFileDecorator.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.decorator;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.extern.slf4j.Slf4j;

/**
 * 合同文件下载地址加密装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class EncryptUrlContractFileDecorator extends AbstractContractFileServiceDecorator {

    /**
     * 初始化下载地址加密装饰器
     *
     * @param delegate 被装饰的合同文件服务
     */
    public EncryptUrlContractFileDecorator(ContractFileService delegate) {
        super(delegate);
    }

    /**
     * 下载合同文件并加密下载地址
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        ContractDownloadResponse response = super.download(request);
        String encryptedUrl = Base64.encode(response.getDownloadUrl());

        response.setDownloadUrl(StrUtil.format("https://file.example.com/secure-download?token={}", encryptedUrl));
        response.getEnhancedFeatures().add("ENCRYPT_URL");

        log.info("合同文件下载地址加密完成，合同编号：{}", request.getContractNo());
        return response;
    }
}
```

调整装配顺序：

```java
@Bean
public ContractFileService contractFileService(ContractFileGenerator contractFileGenerator,
                                               ContractAccessChecker contractAccessChecker) {
    ContractFileService target = new DefaultContractFileService(contractFileGenerator);
    ContractFileService watermarkDecorator = new WatermarkContractFileDecorator(target);
    ContractFileService encryptUrlDecorator = new EncryptUrlContractFileDecorator(watermarkDecorator);
    ContractFileService auditDecorator = new AuditContractFileDecorator(encryptUrlDecorator);
    return new ContractFileAccessProxy(auditDecorator, contractAccessChecker);
}
```

调整后调用链变成：

```text
ContractFileAccessProxy
  -> AuditContractFileDecorator
      -> EncryptUrlContractFileDecorator
          -> WatermarkContractFileDecorator
              -> DefaultContractFileService
```

## 新增代理控制

当业务需要新增访问控制能力时，例如“下载频率限制”，可以在代理中增加控制逻辑，或者新增一个专门的限流代理包裹原代理。

下面示例新增一个限流代理。真实项目中可以使用 Redis、Redisson 或 Sentinel 实现分布式限流。

文件位置：`src/main/java/io/github/atengk/pattern/combination/proxydecorator/proxy/ContractFileRateLimitProxy.java`

```java
package io.github.atengk.pattern.combination.proxydecorator.proxy;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadRequest;
import io.github.atengk.pattern.combination.proxydecorator.dto.ContractDownloadResponse;
import io.github.atengk.pattern.combination.proxydecorator.service.ContractFileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合同文件下载限流代理
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ContractFileRateLimitProxy implements ContractFileService {

    private static final int MAX_DOWNLOAD_COUNT = 3;

    private final ContractFileService delegate;

    private final Map<String, Integer> downloadCountMap = new ConcurrentHashMap<>();

    /**
     * 初始化限流代理
     *
     * @param delegate 被代理的合同文件服务
     */
    public ContractFileRateLimitProxy(ContractFileService delegate) {
        this.delegate = delegate;
    }

    /**
     * 下载合同文件
     *
     * @param request 合同下载请求
     * @return 合同下载响应
     */
    @Override
    public ContractDownloadResponse download(ContractDownloadRequest request) {
        String limitKey = StrUtil.format("{}:{}", request.getUserId(), request.getContractNo());
        int currentCount = downloadCountMap.merge(limitKey, 1, Integer::sum);

        if (currentCount > MAX_DOWNLOAD_COUNT) {
            throw new IllegalArgumentException("合同下载次数超过限制");
        }

        log.info("合同下载限流校验通过，key：{}，当前次数：{}", limitKey, currentCount);
        return delegate.download(request);
    }
}
```

装配时可以把限流代理放在最外层：

```java
@Bean
public ContractFileService contractFileService(ContractFileGenerator contractFileGenerator,
                                               ContractAccessChecker contractAccessChecker) {
    ContractFileService target = new DefaultContractFileService(contractFileGenerator);
    ContractFileService watermarkDecorator = new WatermarkContractFileDecorator(target);
    ContractFileService auditDecorator = new AuditContractFileDecorator(watermarkDecorator);
    ContractFileService accessProxy = new ContractFileAccessProxy(auditDecorator, contractAccessChecker);
    return new ContractFileRateLimitProxy(accessProxy);
}
```

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 合同所属用户下载时，代理校验通过
2. 管理员下载其他用户合同时，代理校验通过
3. 普通用户下载其他用户合同时，代理直接拒绝
4. 合同被锁定时，代理直接拒绝
5. 权限通过后，真实对象生成下载地址
6. 水印装饰器增加 WATERMARK 增强标识
7. 审计装饰器增加 AUDIT 增强标识
8. 新增装饰器时，不需要修改真实对象
9. 新增代理控制时，不需要修改装饰器和真实对象
```

正常请求日志示例：

```text
合同文件访问代理开始处理，合同编号：CT202605130001，用户ID：1001
合同下载权限校验通过，合同编号：CT202605130001，用户ID：1001，角色：USER
生成合同下载地址，合同编号：CT202605130001，下载地址：https://file.example.com/contracts/CT202605130001.pdf
真实合同文件服务生成下载结果，合同编号：CT202605130001，文件名称：contract_CT202605130001.pdf
合同文件水印增强完成，合同编号：CT202605130001，水印：user:1001 time:2026-05-13 14:30:00
合同文件下载审计完成，审计流水号：AUD1988267712300011520，合同编号：CT202605130001，用户ID：1001
合同文件访问代理处理完成，合同编号：CT202605130001，允许下载：true
合同下载服务处理完成，合同编号：CT202605130001，增强能力：[WATERMARK, AUDIT]
```

无权限请求日志示例：

```text
合同文件访问代理开始处理，合同编号：CT202605130003，用户ID：1003
合同下载业务异常：当前用户无权下载该合同
```

这个日志说明代理在最外层拦截了请求，后续装饰器和真实对象不会继续执行。

## 组合效果

代理模式和装饰器模式组合后，各自负责不同变化点：

| 模式       | 职责             | 在示例中的体现                                               |
| ---------- | ---------------- | ------------------------------------------------------------ |
| 代理模式   | 控制对象访问     | `ContractFileAccessProxy` 在下载前进行权限校验               |
| 装饰器模式 | 动态增强对象能力 | `WatermarkContractFileDecorator`、`AuditContractFileDecorator` 增加水印和审计能力 |
| 真实对象   | 处理核心业务     | `DefaultContractFileService` 只负责生成合同下载结果          |

这种组合适合处理下面两类变化：

```text
第一类变化：访问控制规则变化
例如权限校验、合同状态校验、下载频率限制、IP 白名单、风控拦截

第二类变化：功能增强规则变化
例如水印、审计、加密、压缩、脱敏、日志、埋点
```

相比只使用代理模式，这个组合可以避免代理对象中堆积大量增强逻辑。相比只使用装饰器模式，这个组合可以在增强前先进行访问控制，避免无权限请求继续执行核心逻辑和增强逻辑。

## 注意事项

代理模式和装饰器模式组合使用时，要明确两者边界：

```text
代理模式负责访问控制
装饰器模式负责功能增强
```

不要把水印、审计、加密、压缩等增强能力都写进代理对象。代理对象一旦包含太多增强逻辑，就会偏离“控制访问”的职责。

也不要让装饰器承担权限校验、风控拦截、限流等访问控制职责。装饰器更适合在调用前后增加能力，但不应该成为主要访问控制入口。

在 Spring Boot 项目中，如果增强逻辑是横切关注点，也可以使用 Spring AOP；如果增强逻辑需要按对象组合、按顺序灵活叠加，装饰器模式会更清晰。这个组合最适合的判断标准是：**访问控制归代理，能力增强归装饰器**。