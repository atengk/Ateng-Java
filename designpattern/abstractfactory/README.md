# 设计模式：抽象工厂模式

抽象工厂模式用于创建一组相关或相互依赖的对象，而不需要调用方指定具体产品类。在 JDK21 和 Spring Boot 3 项目中，抽象工厂模式常用于多云厂商接入、多支付平台组件族、多消息中间件适配、多数据库方言、多业务渠道套件、多端渲染组件族等场景。

需要注意：抽象工厂模式关注的是“创建一整套产品族”。如果只是根据类型创建一个对象，简单工厂或工厂方法通常更轻；如果需要同时创建同一厂商、同一平台、同一渠道下的一组配套对象，抽象工厂模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证抽象工厂模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法、Getter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

抽象工厂模式的核心目标是让调用方依赖一组抽象产品接口，通过抽象工厂创建同一产品族下的多个具体产品。

常见角色如下：

| 角色            | 说明                                   |
| --------------- | -------------------------------------- |
| AbstractFactory | 抽象工厂，定义创建一组产品的方法       |
| ConcreteFactory | 具体工厂，创建某个产品族的一组具体产品 |
| AbstractProduct | 抽象产品，定义某类产品的统一接口       |
| ConcreteProduct | 具体产品，某个产品族下的具体实现       |
| Client          | 调用方，只依赖抽象工厂和抽象产品       |

以多云厂商为例，阿里云和腾讯云都提供对象存储、短信服务。如果系统中只创建对象存储客户端，可以使用工厂方法；如果系统中需要同时创建“同一云厂商”的对象存储客户端和短信客户端，就更适合抽象工厂模式。

典型结构如下：

```text
CloudServiceFactory
├── createStorageService()
└── createSmsService()

AliyunCloudServiceFactory
├── AliyunStorageService
└── AliyunSmsService

TencentCloudServiceFactory
├── TencentStorageService
└── TencentSmsService
```

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 抽象工厂 > 普通 Java 抽象工厂 > 大量 if else 创建产品对象
```

抽象工厂模式适合产品族稳定、产品等级结构清晰的场景。例如“云厂商”是产品族，“对象存储、短信、消息队列”是产品等级。

## 普通 Java 抽象工厂

普通 Java 抽象工厂适合不依赖 Spring 容器的产品族创建场景。下面以多云服务为例，系统支持阿里云和腾讯云，每个云厂商都提供对象存储服务和短信服务。

整体关系如下：

```text
调用方
    -> CloudServiceFactory
        -> StorageService
        -> SmsService
```

### 文件结构

```text
src/main/java/io/github/atengk/design/abstractfactory/simple/
├── StorageService.java
├── SmsService.java
├── CloudServiceFactory.java
├── AliyunStorageService.java
├── AliyunSmsService.java
├── AliyunCloudServiceFactory.java
├── TencentStorageService.java
├── TencentSmsService.java
└── TencentCloudServiceFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/StorageService.java`

下面是对象存储服务抽象产品接口。

```java
package io.github.atengk.design.abstractfactory.simple;

/**
 * 对象存储服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    String upload(String bucketName, String objectName, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/SmsService.java`

下面是短信服务抽象产品接口。

```java
package io.github.atengk.design.abstractfactory.simple;

/**
 * 短信服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface SmsService {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 发送结果
     */
    String send(String mobile, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/CloudServiceFactory.java`

下面是云服务抽象工厂接口。它负责创建同一云厂商产品族下的对象存储服务和短信服务。

```java
package io.github.atengk.design.abstractfactory.simple;

/**
 * 云服务抽象工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface CloudServiceFactory {

    /**
     * 创建对象存储服务
     *
     * @return 对象存储服务
     */
    StorageService createStorageService();

    /**
     * 创建短信服务
     *
     * @return 短信服务
     */
    SmsService createSmsService();

    /**
     * 获取云厂商名称
     *
     * @return 云厂商名称
     */
    String vendor();
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/AliyunStorageService.java`

下面是阿里云对象存储服务实现。

```java
package io.github.atengk.design.abstractfactory.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云对象存储服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AliyunStorageService implements StorageService {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    @Override
    public String upload(String bucketName, String objectName, String content) {
        validateParam(bucketName, objectName, content);

        String url = StrUtil.format("https://{}.oss-cn-hangzhou.aliyuncs.com/{}?uploadId={}",
                bucketName, objectName, IdUtil.fastSimpleUUID());

        log.info("阿里云OSS上传文件成功，存储桶：{}，对象名称：{}，访问地址：{}", bucketName, objectName, url);
        return url;
    }

    /**
     * 校验上传参数
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     */
    private void validateParam(String bucketName, String objectName, String content) {
        if (StrUtil.hasBlank(bucketName, objectName, content)) {
            log.warn("阿里云OSS上传失败，存储桶、对象名称或内容为空");
            throw new IllegalArgumentException("存储桶、对象名称和内容不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/AliyunSmsService.java`

下面是阿里云短信服务实现。

```java
package io.github.atengk.design.abstractfactory.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云短信服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AliyunSmsService implements SmsService {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 发送结果
     */
    @Override
    public String send(String mobile, String content) {
        if (StrUtil.hasBlank(mobile, content)) {
            log.warn("阿里云短信发送失败，手机号或内容为空");
            throw new IllegalArgumentException("手机号和短信内容不能为空");
        }

        String bizId = "ALI_SMS_" + IdUtil.fastSimpleUUID();
        log.info("阿里云短信发送成功，手机号：{}，业务ID：{}", mobile, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/AliyunCloudServiceFactory.java`

下面是阿里云服务工厂，负责创建阿里云产品族。

```java
package io.github.atengk.design.abstractfactory.simple;

/**
 * 阿里云服务工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class AliyunCloudServiceFactory implements CloudServiceFactory {

    /**
     * 创建对象存储服务
     *
     * @return 对象存储服务
     */
    @Override
    public StorageService createStorageService() {
        return new AliyunStorageService();
    }

    /**
     * 创建短信服务
     *
     * @return 短信服务
     */
    @Override
    public SmsService createSmsService() {
        return new AliyunSmsService();
    }

    /**
     * 获取云厂商名称
     *
     * @return 云厂商名称
     */
    @Override
    public String vendor() {
        return "aliyun";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/TencentStorageService.java`

下面是腾讯云对象存储服务实现。

```java
package io.github.atengk.design.abstractfactory.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云对象存储服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class TencentStorageService implements StorageService {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    @Override
    public String upload(String bucketName, String objectName, String content) {
        if (StrUtil.hasBlank(bucketName, objectName, content)) {
            log.warn("腾讯云COS上传失败，存储桶、对象名称或内容为空");
            throw new IllegalArgumentException("存储桶、对象名称和内容不能为空");
        }

        String url = StrUtil.format("https://{}.cos.ap-shanghai.myqcloud.com/{}?requestId={}",
                bucketName, objectName, IdUtil.fastSimpleUUID());

        log.info("腾讯云COS上传文件成功，存储桶：{}，对象名称：{}，访问地址：{}", bucketName, objectName, url);
        return url;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/TencentSmsService.java`

下面是腾讯云短信服务实现。

```java
package io.github.atengk.design.abstractfactory.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云短信服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class TencentSmsService implements SmsService {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 发送结果
     */
    @Override
    public String send(String mobile, String content) {
        if (StrUtil.hasBlank(mobile, content)) {
            log.warn("腾讯云短信发送失败，手机号或内容为空");
            throw new IllegalArgumentException("手机号和短信内容不能为空");
        }

        String requestId = "TENCENT_SMS_" + IdUtil.fastSimpleUUID();
        log.info("腾讯云短信发送成功，手机号：{}，请求ID：{}", mobile, requestId);
        return requestId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/simple/TencentCloudServiceFactory.java`

下面是腾讯云服务工厂，负责创建腾讯云产品族。

```java
package io.github.atengk.design.abstractfactory.simple;

/**
 * 腾讯云服务工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class TencentCloudServiceFactory implements CloudServiceFactory {

    /**
     * 创建对象存储服务
     *
     * @return 对象存储服务
     */
    @Override
    public StorageService createStorageService() {
        return new TencentStorageService();
    }

    /**
     * 创建短信服务
     *
     * @return 短信服务
     */
    @Override
    public SmsService createSmsService() {
        return new TencentSmsService();
    }

    /**
     * 获取云厂商名称
     *
     * @return 云厂商名称
     */
    @Override
    public String vendor() {
        return "tencent";
    }
}
```

使用方式：

```java
CloudServiceFactory factory = new AliyunCloudServiceFactory();

StorageService storageService = factory.createStorageService();
SmsService smsService = factory.createSmsService();

String fileUrl = storageService.upload("order-bucket", "order.txt", "订单内容");
String smsResult = smsService.send("13800138000", "订单已创建");
```

如果切换为腾讯云，只需要替换工厂：

```java
CloudServiceFactory factory = new TencentCloudServiceFactory();
```

调用方仍然使用 `StorageService` 和 `SmsService` 抽象接口，不直接依赖具体云厂商实现。

## Spring Boot 抽象工厂

Spring Boot 项目中更常见的写法，是把每个具体工厂和具体产品交给 Spring 管理。调用方通过上下文选择某个厂商的抽象工厂，再由工厂返回该厂商下的一组配套产品。

下面以云厂商聚合操作为例，一个接口同时完成文件上传和短信通知，并要求这两个操作必须来自同一个云厂商产品族。

整体流程如下：

```text
Controller
    -> CloudFactoryContext
        -> CloudResourceFactory
            -> CloudStorageClient
            -> CloudSmsClient
```

示例支持两个厂商：

```text
aliyun   阿里云产品族
tencent  腾讯云产品族
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── AbstractFactoryApplication.java
├── client/
│   ├── CloudStorageClient.java
│   ├── CloudSmsClient.java
│   ├── AliyunStorageClient.java
│   ├── AliyunSmsClient.java
│   ├── TencentStorageClient.java
│   └── TencentSmsClient.java
├── controller/
│   └── CloudOperationController.java
├── context/
│   └── CloudFactoryContext.java
├── dto/
│   ├── CloudOperationRequest.java
│   └── CloudOperationResponse.java
└── factory/
    ├── CloudResourceFactory.java
    ├── AliyunCloudResourceFactory.java
    └── TencentCloudResourceFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/AbstractFactoryApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 抽象工厂模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class AbstractFactoryApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AbstractFactoryApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/CloudOperationRequest.java`

下面是云服务操作请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 云服务操作请求
 *
 * @param vendor     云厂商
 * @param bucketName 存储桶名称
 * @param objectName 对象名称
 * @param content    文件内容
 * @param mobile     手机号
 * @param smsContent 短信内容
 * @author Ateng
 * @since 2026-04-30
 */
public record CloudOperationRequest(
        String vendor,
        String bucketName,
        String objectName,
        String content,
        String mobile,
        String smsContent
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/CloudOperationResponse.java`

下面是云服务操作响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 云服务操作响应
 *
 * @param vendor    云厂商
 * @param fileUrl   文件访问地址
 * @param smsBizId  短信业务ID
 * @param message   响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record CloudOperationResponse(
        String vendor,
        String fileUrl,
        String smsBizId,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/client/CloudStorageClient.java`

下面是对象存储客户端抽象产品接口。

```java
package io.github.atengk.design.client;

/**
 * 云对象存储客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface CloudStorageClient {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    String upload(String bucketName, String objectName, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/client/CloudSmsClient.java`

下面是短信客户端抽象产品接口。

```java
package io.github.atengk.design.client;

/**
 * 云短信客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface CloudSmsClient {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 短信业务ID
     */
    String send(String mobile, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/client/AliyunStorageClient.java`

下面是阿里云对象存储客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阿里云对象存储客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AliyunStorageClient implements CloudStorageClient {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    @Override
    public String upload(String bucketName, String objectName, String content) {
        if (StrUtil.hasBlank(bucketName, objectName, content)) {
            log.warn("阿里云OSS上传失败，参数不完整");
            throw new IllegalArgumentException("上传参数不能为空");
        }

        String fileUrl = StrUtil.format("https://{}.oss-cn-hangzhou.aliyuncs.com/{}?id={}",
                bucketName, objectName, IdUtil.fastSimpleUUID());

        log.info("阿里云OSS上传成功，存储桶：{}，对象名称：{}", bucketName, objectName);
        return fileUrl;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/client/AliyunSmsClient.java`

下面是阿里云短信客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AliyunSmsClient implements CloudSmsClient {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 短信业务ID
     */
    @Override
    public String send(String mobile, String content) {
        if (StrUtil.hasBlank(mobile, content)) {
            log.warn("阿里云短信发送失败，手机号或内容为空");
            throw new IllegalArgumentException("手机号和短信内容不能为空");
        }

        String bizId = "ALI" + IdUtil.getSnowflakeNextId();
        log.info("阿里云短信发送成功，手机号：{}，业务ID：{}", mobile, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/client/TencentStorageClient.java`

下面是腾讯云对象存储客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 腾讯云对象存储客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class TencentStorageClient implements CloudStorageClient {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    @Override
    public String upload(String bucketName, String objectName, String content) {
        if (StrUtil.hasBlank(bucketName, objectName, content)) {
            log.warn("腾讯云COS上传失败，参数不完整");
            throw new IllegalArgumentException("上传参数不能为空");
        }

        String fileUrl = StrUtil.format("https://{}.cos.ap-shanghai.myqcloud.com/{}?id={}",
                bucketName, objectName, IdUtil.fastSimpleUUID());

        log.info("腾讯云COS上传成功，存储桶：{}，对象名称：{}", bucketName, objectName);
        return fileUrl;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/client/TencentSmsClient.java`

下面是腾讯云短信客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 腾讯云短信客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class TencentSmsClient implements CloudSmsClient {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 短信业务ID
     */
    @Override
    public String send(String mobile, String content) {
        if (StrUtil.hasBlank(mobile, content)) {
            log.warn("腾讯云短信发送失败，手机号或内容为空");
            throw new IllegalArgumentException("手机号和短信内容不能为空");
        }

        String bizId = "TX" + IdUtil.getSnowflakeNextId();
        log.info("腾讯云短信发送成功，手机号：{}，业务ID：{}", mobile, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/CloudResourceFactory.java`

下面是云资源抽象工厂接口。它负责创建同一厂商下的对象存储客户端和短信客户端。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.client.CloudSmsClient;
import io.github.atengk.design.client.CloudStorageClient;

/**
 * 云资源抽象工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface CloudResourceFactory {

    /**
     * 获取支持的云厂商
     *
     * @return 云厂商
     */
    String supportVendor();

    /**
     * 创建对象存储客户端
     *
     * @return 对象存储客户端
     */
    CloudStorageClient createStorageClient();

    /**
     * 创建短信客户端
     *
     * @return 短信客户端
     */
    CloudSmsClient createSmsClient();
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/AliyunCloudResourceFactory.java`

下面是阿里云资源工厂。它返回阿里云产品族中的对象存储客户端和短信客户端。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.client.AliyunSmsClient;
import io.github.atengk.design.client.AliyunStorageClient;
import io.github.atengk.design.client.CloudSmsClient;
import io.github.atengk.design.client.CloudStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 阿里云资源工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class AliyunCloudResourceFactory implements CloudResourceFactory {

    private final AliyunStorageClient aliyunStorageClient;
    private final AliyunSmsClient aliyunSmsClient;

    /**
     * 获取支持的云厂商
     *
     * @return 云厂商
     */
    @Override
    public String supportVendor() {
        return "aliyun";
    }

    /**
     * 创建对象存储客户端
     *
     * @return 对象存储客户端
     */
    @Override
    public CloudStorageClient createStorageClient() {
        return aliyunStorageClient;
    }

    /**
     * 创建短信客户端
     *
     * @return 短信客户端
     */
    @Override
    public CloudSmsClient createSmsClient() {
        return aliyunSmsClient;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/TencentCloudResourceFactory.java`

下面是腾讯云资源工厂。它返回腾讯云产品族中的对象存储客户端和短信客户端。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.client.CloudSmsClient;
import io.github.atengk.design.client.CloudStorageClient;
import io.github.atengk.design.client.TencentSmsClient;
import io.github.atengk.design.client.TencentStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 腾讯云资源工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class TencentCloudResourceFactory implements CloudResourceFactory {

    private final TencentStorageClient tencentStorageClient;
    private final TencentSmsClient tencentSmsClient;

    /**
     * 获取支持的云厂商
     *
     * @return 云厂商
     */
    @Override
    public String supportVendor() {
        return "tencent";
    }

    /**
     * 创建对象存储客户端
     *
     * @return 对象存储客户端
     */
    @Override
    public CloudStorageClient createStorageClient() {
        return tencentStorageClient;
    }

    /**
     * 创建短信客户端
     *
     * @return 短信客户端
     */
    @Override
    public CloudSmsClient createSmsClient() {
        return tencentSmsClient;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/CloudFactoryContext.java`

下面是云工厂上下文。它根据云厂商选择对应的具体工厂。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.factory.CloudResourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 云工厂上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class CloudFactoryContext {

    private final Map<String, CloudResourceFactory> factoryMap;

    /**
     * 创建云工厂上下文
     *
     * @param factories 云资源工厂列表
     */
    public CloudFactoryContext(List<CloudResourceFactory> factories) {
        if (CollUtil.isEmpty(factories)) {
            log.warn("云资源工厂列表为空");
            this.factoryMap = Map.of();
            return;
        }

        this.factoryMap = factories.stream()
                .collect(Collectors.toUnmodifiableMap(
                        factory -> StrUtil.trim(factory.supportVendor()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化云工厂上下文，支持厂商：{}", factoryMap.keySet());
    }

    /**
     * 获取云资源工厂
     *
     * @param vendor 云厂商
     * @return 云资源工厂
     */
    public CloudResourceFactory getFactory(String vendor) {
        if (StrUtil.isBlank(vendor)) {
            log.warn("获取云资源工厂失败，云厂商为空");
            throw new IllegalArgumentException("云厂商不能为空");
        }

        String key = StrUtil.trim(vendor).toLowerCase();
        CloudResourceFactory factory = factoryMap.get(key);

        if (factory == null) {
            log.warn("获取云资源工厂失败，不支持的云厂商：{}", vendor);
            throw new IllegalArgumentException("不支持的云厂商：" + vendor);
        }

        return factory;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/CloudOperationController.java`

下面是云服务操作接口。它通过抽象工厂获取同一厂商的对象存储客户端和短信客户端。

```java
package io.github.atengk.design.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.client.CloudSmsClient;
import io.github.atengk.design.client.CloudStorageClient;
import io.github.atengk.design.context.CloudFactoryContext;
import io.github.atengk.design.dto.CloudOperationRequest;
import io.github.atengk.design.dto.CloudOperationResponse;
import io.github.atengk.design.factory.CloudResourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 云服务操作控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/abstract-factory/cloud")
public class CloudOperationController {

    private final CloudFactoryContext cloudFactoryContext;

    /**
     * 执行云服务操作
     *
     * @param vendor     云厂商
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @param mobile     手机号
     * @param smsContent 短信内容
     * @return 云服务操作响应
     */
    @PostMapping("/operate")
    public CloudOperationResponse operate(@RequestParam String vendor,
                                          @RequestParam String bucketName,
                                          @RequestParam String objectName,
                                          @RequestParam String content,
                                          @RequestParam String mobile,
                                          @RequestParam String smsContent) {
        CloudOperationRequest request = new CloudOperationRequest(
                vendor,
                bucketName,
                objectName,
                content,
                mobile,
                smsContent
        );

        validateRequest(request);

        CloudResourceFactory factory = cloudFactoryContext.getFactory(request.vendor());
        CloudStorageClient storageClient = factory.createStorageClient();
        CloudSmsClient smsClient = factory.createSmsClient();

        String fileUrl = storageClient.upload(request.bucketName(), request.objectName(), request.content());
        String smsBizId = smsClient.send(request.mobile(), request.smsContent());

        log.info("云服务操作完成，厂商：{}，文件地址：{}，短信业务ID：{}",
                factory.supportVendor(), fileUrl, smsBizId);

        return new CloudOperationResponse(
                factory.supportVendor(),
                fileUrl,
                smsBizId,
                "操作成功"
        );
    }

    /**
     * 校验请求参数
     *
     * @param request 云服务操作请求
     */
    private void validateRequest(CloudOperationRequest request) {
        if (request == null) {
            log.warn("云服务操作失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(
                request.vendor(),
                request.bucketName(),
                request.objectName(),
                request.content(),
                request.mobile(),
                request.smsContent()
        )) {
            log.warn("云服务操作失败，请求参数不完整，厂商：{}", request.vendor());
            throw new IllegalArgumentException("请求参数不能为空");
        }
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=aliyun&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"

curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=tencent&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"
```

阿里云可能返回：

```json
{
  "vendor": "aliyun",
  "fileUrl": "https://order-bucket.oss-cn-hangzhou.aliyuncs.com/order.txt?id=9ecb2a8f0c2744f69eac77d77ad86d12",
  "smsBizId": "ALI2019776866538487808",
  "message": "操作成功"
}
```

腾讯云可能返回：

```json
{
  "vendor": "tencent",
  "fileUrl": "https://order-bucket.cos.ap-shanghai.myqcloud.com/order.txt?id=2ff2f3c016de41f98625f215d7a7f2b8",
  "smsBizId": "TX2019776866538487809",
  "message": "操作成功"
}
```

这种方式的优点是对象存储和短信服务来自同一产品族。调用方只需要选择厂商，不需要分别判断对象存储用哪个实现、短信用哪个实现。

## 扩展一个新产品族

在抽象工厂模式中，扩展新产品族通常比较方便。下面以华为云为例，新增华为云对象存储客户端、华为云短信客户端和华为云资源工厂。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── client/
│   ├── HuaweiStorageClient.java
│   └── HuaweiSmsClient.java
└── factory/
    └── HuaweiCloudResourceFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/client/HuaweiStorageClient.java`

下面是华为云对象存储客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 华为云对象存储客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class HuaweiStorageClient implements CloudStorageClient {

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param content    文件内容
     * @return 文件访问地址
     */
    @Override
    public String upload(String bucketName, String objectName, String content) {
        if (StrUtil.hasBlank(bucketName, objectName, content)) {
            log.warn("华为云OBS上传失败，参数不完整");
            throw new IllegalArgumentException("上传参数不能为空");
        }

        String fileUrl = StrUtil.format("https://{}.obs.cn-east-3.myhuaweicloud.com/{}?id={}",
                bucketName, objectName, IdUtil.fastSimpleUUID());

        log.info("华为云OBS上传成功，存储桶：{}，对象名称：{}", bucketName, objectName);
        return fileUrl;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/client/HuaweiSmsClient.java`

下面是华为云短信客户端实现。

```java
package io.github.atengk.design.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 华为云短信客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class HuaweiSmsClient implements CloudSmsClient {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 短信业务ID
     */
    @Override
    public String send(String mobile, String content) {
        if (StrUtil.hasBlank(mobile, content)) {
            log.warn("华为云短信发送失败，手机号或内容为空");
            throw new IllegalArgumentException("手机号和短信内容不能为空");
        }

        String bizId = "HW" + IdUtil.getSnowflakeNextId();
        log.info("华为云短信发送成功，手机号：{}，业务ID：{}", mobile, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/HuaweiCloudResourceFactory.java`

下面是华为云资源工厂。新增该工厂后会自动加入 `CloudFactoryContext`。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.client.CloudSmsClient;
import io.github.atengk.design.client.CloudStorageClient;
import io.github.atengk.design.client.HuaweiSmsClient;
import io.github.atengk.design.client.HuaweiStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 华为云资源工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class HuaweiCloudResourceFactory implements CloudResourceFactory {

    private final HuaweiStorageClient huaweiStorageClient;
    private final HuaweiSmsClient huaweiSmsClient;

    /**
     * 获取支持的云厂商
     *
     * @return 云厂商
     */
    @Override
    public String supportVendor() {
        return "huawei";
    }

    /**
     * 创建对象存储客户端
     *
     * @return 对象存储客户端
     */
    @Override
    public CloudStorageClient createStorageClient() {
        return huaweiStorageClient;
    }

    /**
     * 创建短信客户端
     *
     * @return 短信客户端
     */
    @Override
    public CloudSmsClient createSmsClient() {
        return huaweiSmsClient;
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=huawei&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"
```

新增华为云产品族后，`CloudOperationController` 和 `CloudFactoryContext` 不需要修改。

## 扩展一个新产品等级

抽象工厂模式扩展新产品族比较方便，但扩展新产品等级比较麻烦。所谓新产品等级，是指在现有产品族中新增一种产品类型，例如除了对象存储和短信服务之外，再新增消息队列服务。

如果新增消息队列产品，需要修改抽象工厂接口：

```java
CloudMqClient createMqClient();
```

然后所有具体工厂都要实现该方法：

```text
AliyunCloudResourceFactory     新增 createMqClient()
TencentCloudResourceFactory    新增 createMqClient()
HuaweiCloudResourceFactory     新增 createMqClient()
```

这就是抽象工厂模式的主要缺点：新增产品族容易，新增产品等级困难。

因此在使用抽象工厂模式前，需要确认产品等级结构相对稳定。例如云厂商产品族中的核心能力固定为对象存储、短信、消息队列时，使用抽象工厂比较合适；如果产品等级经常变化，抽象工厂接口会频繁变动，维护成本会变高。

## 抽象工厂模式和工厂方法模式的区别

抽象工厂模式和工厂方法模式都属于创建型设计模式，但关注点不同。

| 对比项       | 抽象工厂模式                   | 工厂方法模式                       |
| ------------ | ------------------------------ | ---------------------------------- |
| 创建对象数量 | 创建一组相关对象               | 创建一个对象                       |
| 关注点       | 产品族                         | 单个产品                           |
| 工厂接口     | 多个创建方法                   | 通常一个创建方法                   |
| 扩展产品族   | 方便                           | 不强调                             |
| 扩展产品等级 | 较麻烦                         | 相对简单                           |
| 典型场景     | 多云厂商组件族、多数据库组件族 | 支付处理器、文件解析器、消息发送器 |

简单理解：

```text
工厂方法模式：一个工厂创建一种产品。
抽象工厂模式：一个工厂创建一整套产品。
```

如果只根据渠道创建一个支付处理器，使用工厂方法即可。如果需要根据厂商创建一整套对象，例如对象存储、短信、消息队列，就更适合抽象工厂。

## 抽象工厂模式和简单工厂的区别

简单工厂通常通过一个静态方法或普通方法，根据类型返回不同对象。

简单工厂示例：

```java
public StorageService createStorageService(String vendor) {
    if ("aliyun".equals(vendor)) {
        return new AliyunStorageService();
    }
    if ("tencent".equals(vendor)) {
        return new TencentStorageService();
    }
    throw new IllegalArgumentException("不支持的厂商：" + vendor);
}
```

这种方式适合对象少、变化少的场景。如果产品族增多，简单工厂会堆积大量分支。

对比关系如下：

| 对比项           | 简单工厂     | 抽象工厂模式     |
| ---------------- | ------------ | ---------------- |
| 复杂度           | 低           | 中等             |
| 创建对象         | 通常一个对象 | 一组相关对象     |
| 扩展方式         | 修改工厂方法 | 新增具体工厂     |
| 是否符合开闭原则 | 较弱         | 扩展产品族时较好 |
| 典型场景         | 简单类型分发 | 产品族创建       |

简单理解：

```text
简单工厂：我根据类型帮你 new 一个对象。
抽象工厂：我根据产品族帮你创建一整套对象。
```

对象少时不要过度设计。对象族清晰、配套关系强时再考虑抽象工厂。

## 抽象工厂模式和策略模式的关系

抽象工厂模式和策略模式经常组合使用。抽象工厂负责创建同一产品族下的一组对象，策略模式负责选择某一种算法或行为执行。

例如多云场景中：

```text
抽象工厂：根据云厂商创建存储、短信、MQ 一组客户端。
策略模式：根据文件类型选择不同上传策略。
```

也可以理解为：

```text
抽象工厂解决“创建什么一组对象”
策略模式解决“运行时使用哪种行为”
```

两者不是互相替代关系。抽象工厂偏创建型模式，策略模式偏行为型模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行阿里云产品族操作：

```bash
curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=aliyun&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"
```

执行腾讯云产品族操作：

```bash
curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=tencent&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"
```

如果抽象工厂模式正常，可以看到类似日志：

```text
初始化云工厂上下文，支持厂商：[aliyun, tencent, huawei]
阿里云OSS上传成功，存储桶：order-bucket，对象名称：order.txt
阿里云短信发送成功，手机号：13800138000，业务ID：ALI2019776866538487808
云服务操作完成，厂商：aliyun，文件地址：https://order-bucket.oss-cn-hangzhou.aliyuncs.com/order.txt?id=9ecb2a8f0c2744f69eac77d77ad86d12，短信业务ID：ALI2019776866538487808
```

执行不支持的厂商：

```bash
curl -X POST "http://localhost:8080/abstract-factory/cloud/operate?vendor=unknown&bucketName=order-bucket&objectName=order.txt&content=订单内容&mobile=13800138000&smsContent=订单已创建"
```

异常日志示例：

```text
获取云资源工厂失败，不支持的云厂商：unknown
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

抽象工厂模式适合创建产品族，但不要把它用于所有对象创建场景。只有当多个产品之间存在明确的配套关系时，抽象工厂才有明显价值。

适合使用抽象工厂模式的场景：

```text
多云厂商：对象存储、短信、MQ、CDN
多支付平台：支付、退款、查询、回调验签
多数据库方言：分页、字段转义、批量插入
多消息中间件：生产者、消费者、配置解析器
多端渲染：PC组件、移动端组件、小程序组件
```

不太适合使用抽象工厂模式的场景：

```text
只创建一个对象
产品之间没有配套关系
产品等级经常变化
对象创建逻辑非常简单
用普通 Spring 注入即可解决
```

不要把抽象工厂写成上帝工厂。一个工厂接口不应该创建几十种不相关产品。

不推荐：

```java
public interface SystemFactory {

    UserService createUserService();

    OrderService createOrderService();

    PaymentService createPaymentService();

    SmsService createSmsService();

    ReportService createReportService();
}
```

这些产品不一定属于同一产品族，强行放到一个抽象工厂会导致接口臃肿。

推荐围绕明确产品族建模：

```java
public interface CloudResourceFactory {

    CloudStorageClient createStorageClient();

    CloudSmsClient createSmsClient();
}
```

抽象工厂的产品族要保持一致。不要出现阿里云工厂返回阿里云存储，却返回腾讯云短信的情况。

错误示例：

```java
public class AliyunCloudResourceFactory implements CloudResourceFactory {

    public CloudStorageClient createStorageClient() {
        return aliyunStorageClient;
    }

    public CloudSmsClient createSmsClient() {
        return tencentSmsClient;
    }
}
```

这种写法会破坏产品族一致性。抽象工厂的价值正是保证同一工厂创建出来的一组产品属于同一族。

Spring Bean 默认是单例，具体产品和具体工厂中不要保存请求级状态。

错误示例：

```java
private String currentBucketName;
private String currentMobile;
private String currentObjectName;
```

推荐使用方法参数和局部变量：

```java
public String upload(String bucketName, String objectName, String content) {
    String fileUrl = buildFileUrl(bucketName, objectName);
    return fileUrl;
}
```

如果对接真实云厂商 SDK，生产环境中需要考虑配置隔离、密钥管理、超时、重试、限流、熔断、日志脱敏和异常包装。抽象工厂只解决对象族创建问题，不自动保证外部调用可靠性。

常见生产配置包括：

```text
accessKey
secretKey
endpoint
region
bucketName
smsSignName
smsTemplateCode
connectTimeout
readTimeout
retryTimes
```

这些配置建议放在 `application.yml`、配置中心或密钥管理系统中，不要硬编码在具体产品类中。

## 总结

在 JDK21 和 Spring Boot 3 项目中，抽象工厂模式的实践重点是创建一组相关产品对象，并保证这些对象来自同一个产品族。

普通 Java 抽象工厂适合理解产品族创建逻辑。Spring Boot 项目中更推荐使用“抽象产品接口 + 具体产品 Bean + 抽象工厂接口 + 具体工厂 Bean + 工厂上下文”的结构。对于多云厂商、多支付平台、多数据库方言、多消息中间件等场景，抽象工厂模式可以让调用方只面向抽象接口编程，并保证产品族一致性。

抽象工厂模式不是为了替代所有对象创建逻辑。它最适合处理“产品族明确、产品等级相对稳定、调用方需要一整套配套对象”的场景。实际落地时，需要控制工厂接口规模，避免产品等级频繁变动导致所有具体工厂同步修改。
