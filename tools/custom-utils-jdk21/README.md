# 自定义工具类模块（基于 JDK21）



## SecureUtil

安全工具类，提供摘要、HMAC、对称加解密、RSA/DSA/SM 系列签名加密、密钥与编码转换等能力。

**添加依赖**

```xml
<!-- BouncyCastle 依赖 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
```

**使用方法**

```
io.github.atengk.controller.SecureDemoController
```



## VirtualThreadUtil

虚拟线程工具类

使用方法：io.github.atengk.controller.VirtualThreadDemoController



## ValidateUtil

效验工具类

**添加依赖**

```xml
<!-- Spring Boot 参数校验依赖，提供 Jakarta Bean Validation 支持，用于 @NotNull、@NotBlank、@Size、@Valid 等注解校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**校验工具类配置**

```java
package io.github.atengk.config;

import io.github.atengk.utils.ValidateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Configuration;

/**
 * 校验工具类配置
 * 将 Spring Boot 容器管理的 Validator 注入到 ValidateUtil，确保消息源和自定义校验器生效。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Configuration
public class ValidateUtilConfig {

    private final Validator validator;

    public ValidateUtilConfig(Validator validator) {
        this.validator = validator;
    }

    /**
     * 初始化 ValidateUtil 使用的 Validator。
     */
    @PostConstruct
    public void init() {
        ValidateUtil.setValidator(validator);
    }

}
```

**使用方法**

```
io.github.atengk.controller.ValidateUtilDemoController
```



## SpringUtil

Spring 上下文工具类

使用方法：io.github.atengk.controller.SpringUtilDemoController



## CommonUtil

通用基础工具类（基于 Hutool 工具库）

使用方法（Test包）：io.github.atengk.CommonUtilTest



## CollectionUtil

集合工具类

使用方法（Test包）：io.github.atengk.collection



## StringUtil

字符串工具类

使用方法（Test包）：io.github.atengk.string



## BeanUtil

Java Bean 基础反射工具类

使用方法（Test包）：io.github.atengk.bean



## DateTimeUtil

日期时间工具类

使用方法（Test包）：io.github.atengk.datetime



## ZipUtil

压缩解压工具类

使用方法（Test包）：io.github.atengk.zip

添加依赖

```xml
<properties>
    <!-- 解压压缩依赖版本 -->
    <zip4j.version>2.11.6</zip4j.version>
    <commons-compress.version>1.28.0</commons-compress.version>
    <xz.version>1.12</xz.version>
</properties>
<!-- 项目依赖 -->
<dependencies>
    <dependency>
        <groupId>net.lingala.zip4j</groupId>
        <artifactId>zip4j</artifactId>
        <version>${zip4j.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-compress</artifactId>
        <version>${commons-compress.version}</version>
    </dependency>
    <dependency>
        <groupId>org.tukaani</groupId>
        <artifactId>xz</artifactId>
        <version>${xz.version}</version>
    </dependency>
</dependencies>
```

