# 国际化 i18n

本文内容用于补充你提供的 Spring Boot 3 国际化 i18n 开发大纲中的前置章节，重点覆盖“功能概述”和“环境与依赖”部分，后续可继续承接 `MessageSource`、`LocaleResolver`、异常国际化、参数校验国际化等章节。

## 功能概述

本章节用于说明系统为什么需要国际化、国际化覆盖哪些业务范围，以及当前项目默认支持哪些语言。Spring Boot 本身支持本地化消息加载，默认会从 classpath 根路径查找 `messages` 资源包，也可以通过 `spring.messages` 配置自定义资源文件位置。([Home](https://docs.spring.io/spring-boot/reference/features/internationalization.html?utm_source=chatgpt.com))

### 国际化目标

国际化 i18n 的目标是将系统中的用户可见文本从业务代码中剥离出来，通过统一的资源文件、语言解析规则和消息转换流程，根据用户当前语言环境返回对应语言内容。

在 Spring Boot 3 项目中，国际化建设主要解决以下问题：

1. **统一接口返回消息**

   接口响应中的成功提示、失败提示、业务提醒等内容不直接写死在 Java 代码中，而是通过消息 Key 从资源文件中获取。

   示例：

   ```json
   {
     "code": "200",
     "message": "操作成功",
     "data": {}
   }
   ```

   在英文语言环境下返回：

   ```json
   {
     "code": "200",
     "message": "Operation successful",
     "data": {}
   }
   ```

2. **统一异常提示**

   业务异常、系统异常、参数校验异常等错误信息统一使用错误码或消息 Key 映射，避免不同模块自行拼接错误文本。

   例如：

   ```text
   user.not_found=用户不存在
   user.not_found=User does not exist
   ```

3. **统一参数校验消息**

   对 `@NotBlank`、`@NotNull`、`@Size`、`@Pattern` 等 Bean Validation 校验提示进行国际化处理，使前端或客户端能根据用户语言展示对应提示。

4. **支持多语言扩展**

   新增语言时，只需要增加对应语言资源文件，不需要修改 Controller、Service 或异常处理逻辑。

5. **保证默认语言兜底**

   当请求未携带语言标识、语言标识不合法，或目标语言资源缺失时，系统应回退到默认语言，避免返回空消息或消息 Key。

### 适用业务场景

国际化适用于存在多语言用户、跨地区部署、海外业务或前后端分离接口服务的系统。对于 Spring Boot 3 后端项目，建议优先覆盖接口响应、参数校验、异常信息和业务提示。

常见适用场景如下：

| 场景         | 说明                                       | 示例                                   |
| ------------ | ------------------------------------------ | -------------------------------------- |
| 登录注册     | 用户直接感知的提示信息需要支持多语言       | 用户名不能为空、密码错误、验证码已过期 |
| 用户中心     | 个人资料、账号状态、权限提示需要按语言展示 | 账号已禁用、邮箱已绑定                 |
| 后台管理系统 | 面向不同地区运营或管理员                   | 新增成功、删除失败、数据不存在         |
| 参数校验     | 请求参数不合法时返回本地化提示             | 手机号格式不正确、名称长度不能超过 50  |
| 业务异常     | 业务规则失败时返回本地化错误信息           | 余额不足、订单已取消、库存不足         |
| 消息通知     | 邮件、站内信、推送消息需要多语言           | 订单支付成功、任务审批通过             |
| Open API     | 对外接口需要根据调用方语言返回消息         | API 参数错误、签名验证失败             |

不建议在第一阶段对所有数据库业务数据做国际化，例如商品名称、文章内容、菜单名称等。这类内容属于“业务数据多语言”，通常需要单独设计数据表、语言字段或内容管理流程，不应与后端接口提示消息混在一起处理。

### 支持语言范围

当前项目建议采用“中文默认、英文扩展”的基础方案，先覆盖系统提示类文本，后续再按业务需要扩展其他语言。

建议语言范围如下：

| 语言     | Locale 标识    | 资源文件                    | 说明           |
| -------- | -------------- | --------------------------- | -------------- |
| 简体中文 | `zh_CN`        | `messages_zh_CN.properties` | 默认业务语言   |
| 英文     | `en_US`        | `messages_en_US.properties` | 国际化基础语言 |
| 默认资源 | 无 Locale 后缀 | `messages.properties`       | 兜底资源文件   |

Spring Boot 国际化自动配置要求默认资源文件存在，例如默认 basename 为 `messages` 时，应提供 `messages.properties`；如果只提供 `messages_zh_CN.properties`、`messages_en_US.properties` 等语言文件，而没有默认文件，可能不会自动配置 `MessageSource`。([Home](https://docs.spring.io/spring-boot/reference/features/internationalization.html?utm_source=chatgpt.com))

建议默认语言策略如下：

```text
默认语言：zh_CN
兜底资源：messages.properties
扩展语言：messages_en_US.properties
语言标识来源优先级：请求参数 > 请求头 > Cookie/Session > 系统默认语言
```

前后端交互时，推荐统一使用标准语言标识：

```text
zh_CN
en_US
```

如需兼容浏览器标准请求头，也可以支持：

```text
zh-CN
en-US
```

后端在解析时统一转换为 Java `Locale` 对象即可。

## 环境与依赖

本章节用于定义 Spring Boot 3 国际化开发所需的基础版本、依赖、资源文件命名规范和目录结构。Spring 的 `MessageSource` 是消息解析的核心接口，支持按消息编码、参数和 `Locale` 解析本地化文本；Spring Framework 也提供了 `ResourceBundleMessageSource` 和 `ReloadableResourceBundleMessageSource` 等常用实现。([Home](https://docs.spring.io/spring-framework/docs/6.2.15/javadoc-api/org/springframework/context/MessageSource.html?utm_source=chatgpt.com))

### Spring Boot 版本说明

本项目基于 Spring Boot 3 开发，建议使用 Java 17 或以上版本。Spring Boot 3.3.x 官方系统要求至少 Java 17，并明确支持 Maven 3.6.3+、Gradle 7.5+ 或 8.x；具体版本应以项目统一父工程或脚手架版本为准。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐基础环境如下：

| 项目             | 推荐版本     | 说明                                            |
| ---------------- | ------------ | ----------------------------------------------- |
| JDK              | 17+          | Spring Boot 3 最低要求 Java 17                  |
| Spring Boot      | 3.x          | 建议项目内统一版本，不同模块不要混用            |
| Spring Framework | 6.x          | Spring Boot 3 基于 Spring Framework 6           |
| Maven            | 3.6.3+       | 推荐用于企业后端项目构建                        |
| Gradle           | 7.5+ / 8.x   | 如果项目使用 Gradle，则按 Boot 插件兼容版本选择 |
| Servlet 容器     | Tomcat 10.1+ | Spring Boot 3 使用 Jakarta EE 命名空间          |

需要注意，Spring Boot 3 已从 Java EE 迁移到 Jakarta EE，相关包名从 `javax.*` 迁移为 `jakarta.*`。如果项目中使用参数校验，需要使用 `jakarta.validation.*` 相关 API，而不是旧版 `javax.validation.*`。

Maven 依赖建议如下，放在项目根目录的 `pom.xml` 中。

```xml
<dependencies>
    <!-- Web 接口支持，包含 Spring MVC、Jackson、内置 Tomcat 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验支持，用于 @NotBlank、@NotNull、@Valid 等校验注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok 简化实体类、配置类、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类，便于字符串、集合、对象、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>
</dependencies>
```

如果项目只做最基础的接口消息国际化，`spring-boot-starter-web` 已经可以满足 `MessageSource` 的基础使用；如果需要参数校验消息国际化，则必须引入 `spring-boot-starter-validation`。

### 国际化资源文件规范

国际化资源文件统一放在 `src/main/resources/i18n/` 目录下，文件使用 `.properties` 格式。Spring Boot 默认 basename 是 `messages`，也可以通过 `spring.messages.basename` 指定多个资源位置；该配置支持从 classpath 根路径解析资源路径。([Home](https://docs.spring.io/spring-boot/reference/features/internationalization.html?utm_source=chatgpt.com))

推荐命名规范如下：

```text
messages.properties
messages_zh_CN.properties
messages_en_US.properties
validation.properties
validation_zh_CN.properties
validation_en_US.properties
error.properties
error_zh_CN.properties
error_en_US.properties
```

资源文件职责建议如下：

| 文件类型                 | 用途                   | 示例 Key                         |
| ------------------------ | ---------------------- | -------------------------------- |
| `messages*.properties`   | 通用接口提示、业务提示 | `common.success`                 |
| `validation*.properties` | 参数校验提示           | `validation.user.name.not_blank` |
| `error*.properties`      | 异常、错误码提示       | `error.user.not_found`           |

Key 命名建议采用“模块 + 场景 + 语义”的结构：

```text
common.success=操作成功
common.fail=操作失败
user.not_found=用户不存在
user.name.not_blank=用户名不能为空
order.status.invalid=订单状态不合法
validation.page.size.min=每页条数不能小于 {0}
```

资源值支持参数占位符，底层通常通过 `MessageFormat` 处理，例如 `{0}`、`{1}`。`ResourceBundleMessageSource` 会基于资源包 basename 加载消息，并缓存资源包和 `MessageFormat`，适合常规生产场景。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/support/ResourceBundleMessageSource.html?utm_source=chatgpt.com))

示例资源文件如下。

文件位置：`src/main/resources/i18n/messages.properties`

```properties
# 默认兜底语言，建议与系统默认语言保持一致
common.success=操作成功
common.fail=操作失败
common.data.not_found=数据不存在

# 用户模块通用提示
user.create.success=用户创建成功
user.update.success=用户更新成功
user.delete.success=用户删除成功
```

文件位置：`src/main/resources/i18n/messages_zh_CN.properties`

```properties
# 简体中文资源
common.success=操作成功
common.fail=操作失败
common.data.not_found=数据不存在

user.create.success=用户创建成功
user.update.success=用户更新成功
user.delete.success=用户删除成功
```

文件位置：`src/main/resources/i18n/messages_en_US.properties`

```properties
# English resources
common.success=Operation successful
common.fail=Operation failed
common.data.not_found=Data does not exist

user.create.success=User created successfully
user.update.success=User updated successfully
user.delete.success=User deleted successfully
```

文件位置：`src/main/resources/i18n/error_zh_CN.properties`

```properties
# 业务异常消息
error.user.not_found=用户不存在
error.user.disabled=用户已被禁用
error.order.not_found=订单不存在
error.order.status_invalid=订单状态不合法
```

文件位置：`src/main/resources/i18n/error_en_US.properties`

```properties
# Business exception messages
error.user.not_found=User does not exist
error.user.disabled=User has been disabled
error.order.not_found=Order does not exist
error.order.status_invalid=Invalid order status
```

### 配置文件目录结构

国际化相关文件建议集中放在 `i18n` 目录下，避免散落在根目录或不同模块中。对于中大型项目，应按通用消息、校验消息、异常消息拆分资源文件，便于多人协作和后续扩展。

推荐目录结构如下：

```text
springboot3-i18n-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               ├── I18nApplication.java
        │               ├── config
        │               │   └── I18nConfig.java
        │               ├── controller
        │               │   └── UserController.java
        │               ├── exception
        │               │   ├── BusinessException.java
        │               │   └── GlobalExceptionHandler.java
        │               └── util
        │                   └── MessageUtils.java
        └── resources
            ├── application.yml
            └── i18n
                ├── messages.properties
                ├── messages_zh_CN.properties
                ├── messages_en_US.properties
                ├── validation.properties
                ├── validation_zh_CN.properties
                ├── validation_en_US.properties
                ├── error.properties
                ├── error_zh_CN.properties
                └── error_en_US.properties
```

`application.yml` 中配置国际化资源路径。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  messages:
    # 国际化资源基础路径，多个 basename 使用英文逗号分隔
    basename: i18n/messages,i18n/validation,i18n/error

    # 资源文件编码，统一使用 UTF-8，避免中文乱码
    encoding: UTF-8

    # 未找到当前语言资源时，不回退到操作系统语言，避免部署环境影响返回结果
    fallback-to-system-locale: false

    # 找不到消息 Key 时，是否直接返回 Key；开发环境可设为 true，生产环境建议统一兜底处理
    use-code-as-default-message: true
```

该目录结构的设计重点是：

1. `application.yml` 只负责声明资源加载规则，不放具体多语言文本。
2. `i18n/messages*` 存放通用提示。
3. `i18n/validation*` 存放参数校验提示。
4. `i18n/error*` 存放异常和错误码提示。
5. Java 代码只引用消息 Key，不直接硬编码中文或英文提示。
6. 新增语言时，只需要增加同名 Locale 后缀文件，例如 `messages_ja_JP.properties`。

## 核心配置

核心配置用于定义国际化消息从哪里加载、如何识别当前请求语言、语言如何切换，以及当语言缺失或资源缺失时如何兜底。本章节承接前文 `src/main/resources/i18n/` 目录结构，默认使用 `zh_CN` 作为系统默认语言。

### MessageSource 配置

`MessageSource` 是 Spring 国际化消息解析的核心组件，负责根据消息 Key、参数和当前 `Locale` 从资源文件中读取对应语言文本。项目中建议显式配置 `MessageSource`，便于统一控制资源路径、编码、缓存时间和找不到 Key 时的处理策略。

推荐配置方式如下：

| 配置项                  | 推荐值                                                       | 说明                               |
| ----------------------- | ------------------------------------------------------------ | ---------------------------------- |
| basename                | `classpath:i18n/messages`、`classpath:i18n/validation`、`classpath:i18n/error` | 多个资源文件按职责拆分             |
| encoding                | `UTF-8`                                                      | 统一编码，避免中文乱码             |
| defaultLocale           | `zh_CN`                                                      | 系统默认语言                       |
| fallbackToSystemLocale  | `false`                                                      | 避免服务器操作系统语言影响接口返回 |
| useCodeAsDefaultMessage | `true`                                                       | 开发阶段便于定位缺失 Key           |

文件位置：`src/main/java/io/github/atengk/config/I18nConfig.java`

下面的配置类用于注册 `MessageSource`、`LocaleResolver` 和语言切换拦截器。

```java
package io.github.atengk.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * 国际化配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置国际化消息源。
     *
     * @return 国际化消息源
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // 多个资源文件按职责拆分：通用消息、参数校验消息、异常消息
        messageSource.setBasenames(
                "classpath:i18n/messages",
                "classpath:i18n/validation",
                "classpath:i18n/error"
        );

        // 统一使用 UTF-8，避免中文乱码
        messageSource.setDefaultEncoding("UTF-8");

        // 找不到当前语言资源时，不回退到操作系统语言
        messageSource.setFallbackToSystemLocale(false);

        // 找不到消息 Key 时直接返回 Key，便于开发阶段定位缺失资源
        messageSource.setUseCodeAsDefaultMessage(true);

        // 生产环境可适当调大缓存时间；开发环境可设置较短时间便于调试
        messageSource.setCacheSeconds(300);

        return messageSource;
    }

    /**
     * 配置语言解析器。
     *
     * @return 语言解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver("language");

        // 默认使用简体中文
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

        // Cookie 有效期 30 天
        localeResolver.setCookieMaxAge(Duration.ofDays(30));

        // Cookie 对整个站点生效
        localeResolver.setCookiePath("/");

        return localeResolver;
    }

    /**
     * 配置语言切换拦截器。
     *
     * @return 语言切换拦截器
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();

        // 前端通过 ?lang=zh_CN 或 ?lang=en_US 切换语言
        interceptor.setParamName("lang");

        // 忽略非法 Locale，避免错误参数导致请求失败
        interceptor.setIgnoreInvalidLocale(true);

        return interceptor;
    }

    /**
     * 注册 Spring MVC 拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
```

如果项目不需要动态刷新资源文件，也可以只使用 `application.yml` 进行自动配置；但显式注册 `ReloadableResourceBundleMessageSource` 更适合开发文档示例，配置边界更清晰。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  messages:
    # 国际化资源基础路径，多个 basename 使用英文逗号分隔
    basename: i18n/messages,i18n/validation,i18n/error

    # 资源文件编码
    encoding: UTF-8

    # 禁止回退到操作系统语言
    fallback-to-system-locale: false

    # 找不到 Key 时返回 Key，便于开发调试
    use-code-as-default-message: true
```

### LocaleResolver 配置

`LocaleResolver` 用于解析当前请求使用哪一种语言。Spring MVC 常用的语言解析方式包括请求头解析、Cookie 解析和 Session 解析。对于前后端分离项目，推荐使用 Cookie 或请求头方案。

常见方案对比如下：

| 方案         | 类                           | 是否支持切换 | 适用场景                                        |
| ------------ | ---------------------------- | ------------ | ----------------------------------------------- |
| 请求头解析   | `AcceptHeaderLocaleResolver` | 否           | 只根据浏览器或客户端 `Accept-Language` 判断语言 |
| Cookie 解析  | `CookieLocaleResolver`       | 是           | 前后端分离、用户切换语言后需要保持状态          |
| Session 解析 | `SessionLocaleResolver`      | 是           | 传统 Web 项目，依赖服务端 Session               |

本项目推荐使用 `CookieLocaleResolver`，原因是语言切换后可以通过 Cookie 保持用户语言偏好，不需要每个接口都显式传递语言参数。

语言解析流程建议如下：

```text
请求进入系统
  ↓
判断 URL 是否携带 lang 参数
  ↓
如果携带 lang，则 LocaleChangeInterceptor 更新当前语言
  ↓
CookieLocaleResolver 将语言写入 Cookie
  ↓
后续请求优先从 Cookie 中读取语言
  ↓
如果没有 Cookie，则使用默认语言 zh_CN
```

如果项目更偏向纯 API 服务，也可以由前端每次通过请求头传递语言，例如：

```http
Accept-Language: zh-CN
```

或自定义请求头：

```http
X-Lang: zh_CN
```

需要注意，如果使用 `AcceptHeaderLocaleResolver`，通常不适合搭配 `LocaleChangeInterceptor` 做语言切换，因为请求头由客户端控制，后端无法直接修改请求头中的语言状态。

### 语言切换参数设计

语言切换参数需要在前后端之间保持一致，避免不同接口、不同模块使用不同参数名。建议统一使用 `lang` 作为语言切换参数。

推荐参数规范如下：

| 参数              | 示例             | 说明               |
| ----------------- | ---------------- | ------------------ |
| `lang`            | `zh_CN`          | 简体中文           |
| `lang`            | `en_US`          | 英文               |
| `Accept-Language` | `zh-CN,zh;q=0.9` | 浏览器标准语言头   |
| `X-Lang`          | `zh_CN`          | 可选的自定义语言头 |

推荐优先级如下：

```text
URL 参数 lang > Cookie language > 请求头 Accept-Language > 系统默认语言 zh_CN
```

前端切换语言时，可以请求任意后端接口并携带 `lang` 参数：

```http
GET /api/user/current?lang=en_US
```

后端接收到 `lang=en_US` 后，`LocaleChangeInterceptor` 会将当前请求语言切换为英文，并由 `CookieLocaleResolver` 写入 Cookie。之后前端继续请求接口时，即使不再携带 `lang` 参数，也可以从 Cookie 中读取语言。

建议语言值统一使用 Java Locale 风格：

```text
zh_CN
en_US
```

如需兼容浏览器标准，也可以在网关、前端或后端工具类中进行转换：

| 浏览器格式 | 后端统一格式 |
| ---------- | ------------ |
| `zh-CN`    | `zh_CN`      |
| `en-US`    | `en_US`      |

### 默认语言设置

默认语言用于处理请求未传递语言、Cookie 不存在、语言值非法、资源文件缺失等兜底场景。项目建议统一使用简体中文作为默认语言。

默认语言策略如下：

| 场景               | 处理方式                                       |
| ------------------ | ---------------------------------------------- |
| 请求未携带语言参数 | 使用 Cookie 中保存的语言                       |
| Cookie 不存在      | 使用系统默认语言 `zh_CN`                       |
| 请求传入非法语言   | 忽略非法语言，继续使用当前语言或默认语言       |
| 指定语言资源缺失   | 使用默认资源文件 `messages.properties`         |
| 消息 Key 不存在    | 开发环境返回 Key，生产环境建议统一返回兜底提示 |

为了便于业务代码获取国际化消息，建议封装统一工具类，不在 Controller 或 Service 中直接操作 `MessageSource`。

文件位置：`src/main/java/io/github/atengk/util/MessageUtils.java`

下面的工具类用于根据当前请求语言解析国际化消息，并支持占位符参数格式化。

```java
package io.github.atengk.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;

    /**
     * 获取当前语言环境。
     *
     * @return 当前语言环境
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * 根据消息 Key 获取国际化消息。
     *
     * @param code 消息 Key
     * @return 国际化消息
     */
    public String getMessage(String code) {
        return getMessage(code, ArrayUtil.empty(Object.class));
    }

    /**
     * 根据消息 Key 和参数获取国际化消息。
     *
     * @param code 消息 Key
     * @param args 占位符参数
     * @return 国际化消息
     */
    public String getMessage(String code, Object... args) {
        if (StrUtil.isBlank(code)) {
            log.warn("国际化消息 Key 为空，返回默认提示");
            return "消息未配置";
        }

        Locale locale = getCurrentLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 根据消息 Key、默认消息和参数获取国际化消息。
     *
     * @param code           消息 Key
     * @param defaultMessage 默认消息
     * @param args           占位符参数
     * @return 国际化消息
     */
    public String getMessage(String code, String defaultMessage, Object... args) {
        if (StrUtil.isBlank(code)) {
            log.warn("国际化消息 Key 为空，返回指定默认消息");
            return StrUtil.blankToDefault(defaultMessage, "消息未配置");
        }

        Locale locale = getCurrentLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

}
```

使用示例：

```java
String message = messageUtils.getMessage("common.success");
String messageWithArgs = messageUtils.getMessage("user.name.length", 2, 20);
```

对应资源文件：

```properties
common.success=操作成功
user.name.length=用户名长度必须在 {0} 到 {1} 个字符之间
```

## 国际化资源管理

国际化资源管理用于规范资源文件如何拆分、Key 如何命名、中文和英文如何保持一致，以及占位符如何在不同语言中正确使用。资源文件管理不规范时，容易出现 Key 冲突、翻译缺失、参数顺序错误和线上返回 Key 等问题。

### messages 资源文件拆分

资源文件建议按“用途”拆分，而不是按“业务模块”无限拆分。中小型项目可以使用 `messages`、`validation`、`error` 三类资源文件；大型项目可以在此基础上增加模块前缀，而不是创建过多文件。

推荐拆分方式如下：

| 文件                     | 作用                       | 示例                             |
| ------------------------ | -------------------------- | -------------------------------- |
| `messages*.properties`   | 通用接口消息、业务成功提示 | `common.success`                 |
| `validation*.properties` | 参数校验消息               | `validation.user.name.not_blank` |
| `error*.properties`      | 业务异常和错误码消息       | `error.user.not_found`           |

目录示例：

```text
src/main/resources/i18n
├── messages.properties
├── messages_zh_CN.properties
├── messages_en_US.properties
├── validation.properties
├── validation_zh_CN.properties
├── validation_en_US.properties
├── error.properties
├── error_zh_CN.properties
└── error_en_US.properties
```

Key 命名建议使用小写字母、数字和点号分隔：

```text
模块.业务.语义
```

推荐示例：

```properties
common.success=操作成功
common.fail=操作失败
user.create.success=用户创建成功
user.update.success=用户更新成功
user.delete.success=用户删除成功
order.pay.success=订单支付成功
order.cancel.success=订单取消成功
```

不推荐示例：

```properties
success=操作成功
msg1=用户不存在
USER_NOT_FOUND=用户不存在
createUserSuccess=用户创建成功
```

原因是这些 Key 缺少模块边界，不利于长期维护，也容易在多人协作时发生冲突。

### 中文资源配置

中文资源一般作为系统默认语言资源，建议同时维护 `messages.properties` 和 `messages_zh_CN.properties`。其中 `messages.properties` 作为兜底文件，内容可以与 `messages_zh_CN.properties` 保持一致。

文件位置：`src/main/resources/i18n/messages.properties`

```properties
# 默认兜底资源，建议与 messages_zh_CN.properties 保持一致
common.success=操作成功
common.fail=操作失败
common.request.success=请求成功
common.request.fail=请求失败
common.data.not_found=数据不存在
common.param.invalid=请求参数不合法

# 用户模块
user.create.success=用户创建成功
user.update.success=用户更新成功
user.delete.success=用户删除成功
user.query.success=用户查询成功
user.not_found=用户不存在
user.disabled=用户已被禁用
user.name.length=用户名长度必须在 {0} 到 {1} 个字符之间

# 订单模块
order.create.success=订单创建成功
order.pay.success=订单支付成功
order.cancel.success=订单取消成功
order.not_found=订单不存在
order.amount.invalid=订单金额必须大于 {0}
```

文件位置：`src/main/resources/i18n/validation_zh_CN.properties`

```properties
# 通用校验
validation.not_null=参数不能为空
validation.not_blank=参数不能为空白
validation.size=参数长度必须在 {0} 到 {1} 个字符之间
validation.email=邮箱格式不正确
validation.mobile=手机号格式不正确

# 用户模块校验
validation.user.name.not_blank=用户名不能为空
validation.user.name.size=用户名长度必须在 {0} 到 {1} 个字符之间
validation.user.email.not_blank=邮箱不能为空
validation.user.email.invalid=邮箱格式不正确

# 订单模块校验
validation.order.id.not_null=订单 ID 不能为空
validation.order.amount.not_null=订单金额不能为空
validation.order.amount.min=订单金额不能小于 {0}
```

文件位置：`src/main/resources/i18n/error_zh_CN.properties`

```properties
# 通用异常
error.system=系统异常，请稍后重试
error.unauthorized=用户未登录
error.forbidden=没有操作权限
error.param.invalid=请求参数不合法
error.data.not_found=数据不存在

# 用户异常
error.user.not_found=用户不存在
error.user.disabled=用户已被禁用
error.user.password_error=用户名或密码错误

# 订单异常
error.order.not_found=订单不存在
error.order.status_invalid=订单状态不合法
error.order.stock_not_enough=商品库存不足
```

中文资源配置注意事项：

1. 默认资源文件 `messages.properties` 必须存在，用于兜底。
2. 中文内容统一使用 UTF-8 编码。
3. 同一类消息使用统一句式，例如成功类统一使用“xxx成功”，失败类统一使用“xxx失败”。
4. 不要在资源值中拼接业务变量，应使用 `{0}`、`{1}` 占位符。
5. 不要把日志内容、数据库字段名、内部错误堆栈放入国际化资源文件。

### 英文资源配置

英文资源应与中文资源保持 Key 完全一致，只修改资源值。新增中文 Key 时，必须同步补充英文资源，否则英文环境下可能返回 Key 或兜底消息。

文件位置：`src/main/resources/i18n/messages_en_US.properties`

```properties
# Common messages
common.success=Operation successful
common.fail=Operation failed
common.request.success=Request successful
common.request.fail=Request failed
common.data.not_found=Data does not exist
common.param.invalid=Invalid request parameters

# User module
user.create.success=User created successfully
user.update.success=User updated successfully
user.delete.success=User deleted successfully
user.query.success=User queried successfully
user.not_found=User does not exist
user.disabled=User has been disabled
user.name.length=Username length must be between {0} and {1} characters

# Order module
order.create.success=Order created successfully
order.pay.success=Order paid successfully
order.cancel.success=Order canceled successfully
order.not_found=Order does not exist
order.amount.invalid=Order amount must be greater than {0}
```

文件位置：`src/main/resources/i18n/validation_en_US.properties`

```properties
# Common validation messages
validation.not_null=Parameter must not be null
validation.not_blank=Parameter must not be blank
validation.size=Parameter length must be between {0} and {1} characters
validation.email=Invalid email format
validation.mobile=Invalid mobile number format

# User validation messages
validation.user.name.not_blank=Username must not be blank
validation.user.name.size=Username length must be between {0} and {1} characters
validation.user.email.not_blank=Email must not be blank
validation.user.email.invalid=Invalid email format

# Order validation messages
validation.order.id.not_null=Order ID must not be null
validation.order.amount.not_null=Order amount must not be null
validation.order.amount.min=Order amount must not be less than {0}
```

文件位置：`src/main/resources/i18n/error_en_US.properties`

```properties
# Common errors
error.system=System error, please try again later
error.unauthorized=User is not logged in
error.forbidden=Permission denied
error.param.invalid=Invalid request parameters
error.data.not_found=Data does not exist

# User errors
error.user.not_found=User does not exist
error.user.disabled=User has been disabled
error.user.password_error=Invalid username or password

# Order errors
error.order.not_found=Order does not exist
error.order.status_invalid=Invalid order status
error.order.stock_not_enough=Insufficient product stock
```

英文资源配置注意事项：

1. 英文 Key 必须与中文 Key 完全一致。
2. `{0}`、`{1}` 等参数占位符必须完整保留。
3. 不要直接逐字翻译中文，应保证英文表达符合接口提示语习惯。
4. 业务名词需要统一，例如订单统一使用 `Order`，用户统一使用 `User`。
5. 缺失翻译应在开发阶段暴露，不建议长期依赖默认 Key 返回。

### 占位符与参数格式化

占位符用于处理包含动态变量的消息，例如用户名、订单号、金额、长度范围等。资源文件中使用 `{0}`、`{1}`、`{2}` 表示参数位置，业务代码调用 `MessageSource` 时按顺序传入参数。

资源文件示例：

文件位置：`src/main/resources/i18n/messages_zh_CN.properties`

```properties
user.welcome=欢迎你，{0}
user.name.length=用户名长度必须在 {0} 到 {1} 个字符之间
order.pay.amount=订单 {0} 支付成功，支付金额为 {1} 元
```

文件位置：`src/main/resources/i18n/messages_en_US.properties`

```properties
user.welcome=Welcome, {0}
user.name.length=Username length must be between {0} and {1} characters
order.pay.amount=Order {0} paid successfully, amount: {1}
```

业务代码中通过 `MessageUtils` 传入参数：

```java
String welcome = messageUtils.getMessage("user.welcome", "Ateng");
String nameLength = messageUtils.getMessage("user.name.length", 2, 20);
String payAmount = messageUtils.getMessage("order.pay.amount", "NO202605060001", "99.90");
```

返回结果示例：

```text
欢迎你，Ateng
用户名长度必须在 2 到 20 个字符之间
订单 NO202605060001 支付成功，支付金额为 99.90 元
```

英文环境下返回：

```text
Welcome, Ateng
Username length must be between 2 and 20 characters
Order NO202605060001 paid successfully, amount: 99.90
```

占位符使用规范如下：

| 规范                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 使用数字占位符             | 统一使用 `{0}`、`{1}`、`{2}`                 |
| 参数顺序保持稳定           | 中文和英文可以调整语序，但参数含义不能改变   |
| 不在代码中拼接文本         | 避免 `"用户" + name + "不存在"` 这类写法     |
| 金额、日期、数量提前格式化 | 复杂格式建议在业务代码或工具类中处理后再传入 |
| 缺失参数要避免             | 资源中有 `{1}` 时，代码至少传入两个参数      |

如果涉及金额、日期等格式，建议先在 Java 代码中格式化为确定字符串，再传入国际化消息，避免不同语言环境下格式不可控。

```java
String amount = "99.90";
String orderNo = "NO202605060001";
String message = messageUtils.getMessage("order.pay.amount", orderNo, amount);
```

资源文件：

```properties
order.pay.amount=订单 {0} 支付成功，支付金额为 {1} 元
```

这种方式更适合接口返回消息，因为接口提示语通常要求稳定、可测试、可对齐前端展示。



## 后端接口国际化

后端接口国际化的核心目标是：Controller、Service、异常处理器都不直接硬编码中文或英文提示，而是统一通过消息 Key 获取当前语言环境下的文本。接口返回给前端的 `message` 字段应是已经转换后的用户可读内容。

### Controller 返回消息国际化

Controller 层负责接收请求、调用业务服务并返回统一响应结果。接口返回消息建议通过 `MessageUtils` 获取，不建议在 Controller 中写死 `操作成功`、`保存成功`、`删除成功` 等固定文本。

示例文件结构如下：

```text
src/main/java/io/github/atengk
├── common
│   └── ApiResult.java
├── controller
│   └── UserController.java
├── dto
│   └── UserCreateDTO.java
├── service
│   └── UserService.java
└── util
    └── MessageUtils.java
```

文件位置：`src/main/java/io/github/atengk/common/ApiResult.java`

下面的类用于封装统一接口返回结构，所有接口都通过该结构返回 `code`、`message` 和 `data`。

```java
package io.github.atengk.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>("200", message, data);
    }

    /**
     * 成功响应，无数据。
     *
     * @param message 响应消息
     * @return 统一响应结果
     */
    public static ApiResult<Void> success(String message) {
        return new ApiResult<>("200", message, null);
    }

    /**
     * 失败响应。
     *
     * @param code    响应编码
     * @param message 响应消息
     * @return 统一响应结果
     */
    public static ApiResult<Void> fail(String code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

文件位置：`src/main/java/io/github/atengk/dto/UserCreateDTO.java`

下面的 DTO 用于接收用户创建参数，校验消息使用国际化资源 Key。

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "{validation.user.name.not_blank}")
    @Size(min = 2, max = 20, message = "{validation.user.name.size}")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "{validation.user.email.not_blank}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;

}
```

文件位置：`src/main/java/io/github/atengk/controller/UserController.java`

下面的 Controller 示例展示查询和创建两个接口，返回消息均通过国际化 Key 转换。

```java
package io.github.atengk.controller;

import io.github.atengk.common.ApiResult;
import io.github.atengk.dto.UserCreateDTO;
import io.github.atengk.service.UserService;
import io.github.atengk.util.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MessageUtils messageUtils;

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public ApiResult<String> getUser(@PathVariable Long id) {
        String username = userService.getUsernameById(id);
        String message = messageUtils.getMessage("user.query.success");
        return ApiResult.success(message, username);
    }

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     * @return 创建结果
     */
    @PostMapping
    public ApiResult<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        String message = messageUtils.getMessage("user.create.success");
        return ApiResult.success(message);
    }

}
```

对应资源文件配置：

```properties
# messages_zh_CN.properties
user.query.success=用户查询成功
user.create.success=用户创建成功
# messages_en_US.properties
user.query.success=User queried successfully
user.create.success=User created successfully
```

接口调用示例：

```bash
curl -X GET "http://localhost:8080/api/users/1?lang=zh_CN"
curl -X GET "http://localhost:8080/api/users/1?lang=en_US"
```

返回示例：

```json
{
  "code": "200",
  "message": "用户查询成功",
  "data": "Ateng"
}
```

英文环境下返回：

```json
{
  "code": "200",
  "message": "User queried successfully",
  "data": "Ateng"
}
```

### Service 业务消息国际化

Service 层建议只处理业务规则，不建议直接决定最终接口响应格式。对于普通成功消息，可以由 Controller 根据业务结果返回国际化消息；对于业务失败场景，Service 层建议抛出带错误码和消息 Key 的业务异常，由全局异常处理器统一转换为当前语言消息。

文件位置：`src/main/java/io/github/atengk/service/UserService.java`

下面的接口定义用户业务操作，Service 层不暴露中文或英文提示。

```java
package io.github.atengk.service;

import io.github.atengk.dto.UserCreateDTO;

/**
 * 用户业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户名。
     *
     * @param id 用户 ID
     * @return 用户名
     */
    String getUsernameById(Long id);

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     */
    void createUser(UserCreateDTO dto);

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/UserServiceImpl.java`

下面的实现类展示业务异常国际化的推荐方式：Service 抛出消息 Key，最终文本由全局异常处理器转换。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.dto.UserCreateDTO;
import io.github.atengk.exception.BusinessException;
import io.github.atengk.exception.ErrorCode;
import io.github.atengk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /**
     * 根据用户 ID 查询用户名。
     *
     * @param id 用户 ID
     * @return 用户名
     */
    @Override
    public String getUsernameById(Long id) {
        if (id == null || id <= 0) {
            log.warn("查询用户失败，用户ID不合法：{}", id);
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        if (id != 1L) {
            log.warn("查询用户失败，用户不存在：{}", id);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, id);
        }

        return "Ateng";
    }

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     */
    @Override
    public void createUser(UserCreateDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getUsername())) {
            log.warn("创建用户失败，用户参数为空");
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        if (StrUtil.equalsIgnoreCase(dto.getUsername(), "admin")) {
            log.warn("创建用户失败，用户名已存在：{}", dto.getUsername());
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, dto.getUsername());
        }

        log.info("创建用户成功，用户名：{}", dto.getUsername());
    }

}
```

对应异常资源文件：

```properties
# error_zh_CN.properties
error.param.invalid=请求参数不合法
error.user.not_found=用户不存在，用户ID：{0}
error.user.already_exists=用户名已存在：{0}
# error_en_US.properties
error.param.invalid=Invalid request parameters
error.user.not_found=User does not exist, user ID: {0}
error.user.already_exists=Username already exists: {0}
```

这种设计可以保证 Service 层只抛出稳定的业务错误语义，不关心当前请求语言，也不会污染业务逻辑。

### 统一响应结果国际化

统一响应结果国际化的关键是所有正常响应、校验失败响应、业务异常响应、系统异常响应都经过同一套消息转换规则。建议响应结构固定，`message` 字段始终返回当前语言的最终文本。

推荐统一响应格式如下：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {}
}
```

失败响应格式如下：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

统一响应处理建议如下：

| 场景         | 消息来源                 | 处理位置       |
| ------------ | ------------------------ | -------------- |
| 正常成功     | `messages*.properties`   | Controller     |
| 业务异常     | `error*.properties`      | 全局异常处理器 |
| 参数校验异常 | `validation*.properties` | 全局异常处理器 |
| 系统异常     | `error.system`           | 全局异常处理器 |

推荐在业务代码中只使用消息 Key：

```java
messageUtils.getMessage("common.success");
messageUtils.getMessage("user.create.success");
throw new BusinessException(ErrorCode.USER_NOT_FOUND, id);
```

不推荐写法：

```java
return ApiResult.success("操作成功", data);
throw new RuntimeException("用户不存在");
```

## 参数校验国际化

参数校验国际化用于处理 `@Valid`、`@Validated`、`@NotBlank`、`@NotNull`、`@Size`、`@Email` 等校验注解的错误提示。校验消息应从国际化资源文件中读取，而不是直接写中文或英文。

### Validation 校验消息配置

Spring Boot 3 使用 Jakarta Validation，相关包名为 `jakarta.validation.*`。为了让校验注解中的 `{validation.xxx}` 能够从 `MessageSource` 中解析，需要配置 `LocalValidatorFactoryBean`。

文件位置：`src/main/java/io/github/atengk/config/ValidatorConfig.java`

下面的配置类用于将参数校验消息源绑定到 Spring 的 `MessageSource`。

```java
package io.github.atengk.config;

import jakarta.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 参数校验配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class ValidatorConfig {

    /**
     * 配置参数校验消息源。
     *
     * @param messageSource 国际化消息源
     * @return 参数校验器
     */
    @Bean
    public Validator validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // 使用统一 MessageSource 解析 Validation 国际化消息
        validatorFactoryBean.setValidationMessageSource(messageSource);

        return validatorFactoryBean;
    }

}
```

参数 DTO 示例：

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "{validation.user.name.not_blank}")
    @Size(min = 2, max = 20, message = "{validation.user.name.size}")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "{validation.user.email.not_blank}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;

}
```

对应资源文件：

```properties
# validation_zh_CN.properties
validation.user.name.not_blank=用户名不能为空
validation.user.name.size=用户名长度必须在 {min} 到 {max} 个字符之间
validation.user.email.not_blank=邮箱不能为空
validation.user.email.invalid=邮箱格式不正确
# validation_en_US.properties
validation.user.name.not_blank=Username must not be blank
validation.user.name.size=Username length must be between {min} and {max} characters
validation.user.email.not_blank=Email must not be blank
validation.user.email.invalid=Invalid email format
```

Bean Validation 注解中的内置属性可以直接使用，例如 `@Size` 支持 `{min}` 和 `{max}`，这类占位符由校验框架处理，不需要业务代码手动传参。

### 自定义校验注解消息

当内置校验注解无法满足业务规则时，可以自定义校验注解。例如手机号、身份证号、业务编码、枚举值等。

文件位置：`src/main/java/io/github/atengk/validation/Mobile.java`

下面的注解用于校验手机号格式，默认消息使用国际化资源 Key。

```java
package io.github.atengk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 手机号校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

    /**
     * 校验失败消息。
     *
     * @return 消息 Key
     */
    String message() default "{validation.mobile.invalid}";

    /**
     * 校验分组。
     *
     * @return 分组数组
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息。
     *
     * @return 负载数组
     */
    Class<? extends Payload>[] payload() default {};

}
```

文件位置：`src/main/java/io/github/atengk/validation/MobileValidator.java`

下面的校验器使用 Hutool 处理字符串空值和正则校验。

```java
package io.github.atengk.validation;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 手机号校验器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    private static final String MOBILE_PATTERN = "^1[3-9]\\d{9}$";

    /**
     * 校验手机号。
     *
     * @param value   手机号
     * @param context 校验上下文
     * @return 是否校验通过
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return true;
        }
        return ReUtil.isMatch(MOBILE_PATTERN, value);
    }

}
```

DTO 使用示例：

```java
package io.github.atengk.dto;

import io.github.atengk.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户手机号绑定参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserMobileBindDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "{validation.mobile.not_blank}")
    @Mobile(message = "{validation.mobile.invalid}")
    private String mobile;

}
```

对应资源文件：

```properties
# validation_zh_CN.properties
validation.mobile.not_blank=手机号不能为空
validation.mobile.invalid=手机号格式不正确
# validation_en_US.properties
validation.mobile.not_blank=Mobile number must not be blank
validation.mobile.invalid=Invalid mobile number format
```

### 参数校验异常处理

参数校验失败后，需要在全局异常处理器中捕获异常，并转换成统一响应结构。常见异常包括：

| 异常类型                          | 触发场景                                      |
| --------------------------------- | --------------------------------------------- |
| `MethodArgumentNotValidException` | `@RequestBody` 参数校验失败                   |
| `BindException`                   | 表单对象、查询对象绑定校验失败                |
| `ConstraintViolationException`    | `@RequestParam`、`@PathVariable` 参数校验失败 |

文件位置：`src/main/java/io/github/atengk/exception/GlobalExceptionHandler.java`

下面的全局异常处理器统一处理参数校验异常、业务异常和系统异常。

```java
package io.github.atengk.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.ApiResult;
import io.github.atengk.util.MessageUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtils messageUtils;

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::resolveFieldErrorMessage)
                .orElseGet(() -> messageUtils.getMessage("error.param.invalid"));

        log.warn("请求体参数校验失败：{}", message);
        return ApiResult.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理表单参数绑定异常。
     *
     * @param exception 参数绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::resolveFieldErrorMessage)
                .orElseGet(() -> messageUtils.getMessage("error.param.invalid"));

        log.warn("表单参数校验失败：{}", message);
        return ApiResult.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理单参数校验异常。
     *
     * @param exception 约束校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        String message = CollUtil.emptyIfNull(violations)
                .stream()
                .map(ConstraintViolation::getMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElseGet(() -> messageUtils.getMessage("error.param.invalid"));

        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException exception) {
        String message = messageUtils.getMessage(
                exception.getMessageKey(),
                exception.getDefaultMessage(),
                exception.getArgs()
        );

        log.warn("业务异常：code={}, message={}", exception.getCode(), message);
        return ApiResult.fail(exception.getCode(), message);
    }

    /**
     * 处理系统异常。
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        String message = messageUtils.getMessage("error.system");
        log.error("系统异常：{}", exception.getMessage(), exception);
        return ApiResult.fail(ErrorCode.SYSTEM_ERROR.getCode(), message);
    }

    /**
     * 解析字段校验错误消息。
     *
     * @param fieldError 字段错误
     * @return 国际化错误消息
     */
    private String resolveFieldErrorMessage(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();

        if (StrUtil.isBlank(defaultMessage)) {
            return messageUtils.getMessage("error.param.invalid");
        }

        try {
            return messageUtils.getMessage(defaultMessage);
        } catch (NoSuchMessageException exception) {
            return defaultMessage;
        }
    }

}
```

注意：如果 `@NotBlank(message = "{validation.user.name.not_blank}")` 已经被 `LocalValidatorFactoryBean` 正确解析，那么 `fieldError.getDefaultMessage()` 通常已经是最终文本；如果项目中存在未解析的 Key，也可以通过 `MessageUtils` 再做一次兜底解析。

## 异常信息国际化

异常信息国际化用于统一处理业务失败、权限失败、参数错误和系统异常等场景。推荐设计方式是：业务代码抛出异常编码和消息 Key，全局异常处理器根据当前 Locale 转换成最终消息。

### 业务异常编码设计

业务异常编码建议分为“响应码”和“消息 Key”两个部分。响应码用于前端判断错误类型，消息 Key 用于后端国际化转换。

推荐错误码结构如下：

| 字段           | 示例                   | 说明                             |
| -------------- | ---------------------- | -------------------------------- |
| code           | `USER_NOT_FOUND`       | 稳定错误编码，前端可用于分支处理 |
| messageKey     | `error.user.not_found` | 国际化资源 Key                   |
| defaultMessage | `用户不存在`           | 兜底消息                         |
| args           | `1`                    | 占位符参数                       |

文件位置：`src/main/java/io/github/atengk/exception/ErrorCode.java`

下面的枚举用于统一维护业务错误编码和国际化消息 Key。

```java
package io.github.atengk.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 错误码枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * 系统异常
     */
    SYSTEM_ERROR("SYSTEM_ERROR", "error.system", "系统异常，请稍后重试"),

    /**
     * 请求参数不合法
     */
    PARAM_INVALID("PARAM_INVALID", "error.param.invalid", "请求参数不合法"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND("USER_NOT_FOUND", "error.user.not_found", "用户不存在"),

    /**
     * 用户名已存在
     */
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "error.user.already_exists", "用户名已存在"),

    /**
     * 用户已禁用
     */
    USER_DISABLED("USER_DISABLED", "error.user.disabled", "用户已被禁用"),

    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "error.order.not_found", "订单不存在"),

    /**
     * 订单状态不合法
     */
    ORDER_STATUS_INVALID("ORDER_STATUS_INVALID", "error.order.status_invalid", "订单状态不合法");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

}
```

文件位置：`src/main/java/io/github/atengk/exception/BusinessException.java`

下面的异常类用于承载错误码、消息 Key 和占位符参数。

```java
package io.github.atengk.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误编码
     */
    private final String code;

    /**
     * 国际化消息 Key
     */
    private final String messageKey;

    /**
     * 默认消息
     */
    private final String defaultMessage;

    /**
     * 占位符参数
     */
    private final Object[] args;

    /**
     * 创建业务异常。
     *
     * @param errorCode 错误码
     * @param args      占位符参数
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
        this.messageKey = errorCode.getMessageKey();
        this.defaultMessage = errorCode.getDefaultMessage();
        this.args = args;
    }

}
```

业务代码使用示例：

```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND, 10001L);
throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Ateng");
throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
```

对应资源文件：

```properties
# error_zh_CN.properties
error.system=系统异常，请稍后重试
error.param.invalid=请求参数不合法
error.user.not_found=用户不存在，用户ID：{0}
error.user.already_exists=用户名已存在：{0}
error.user.disabled=用户已被禁用
error.order.not_found=订单不存在
error.order.status_invalid=订单状态不合法
# error_en_US.properties
error.system=System error, please try again later
error.param.invalid=Invalid request parameters
error.user.not_found=User does not exist, user ID: {0}
error.user.already_exists=Username already exists: {0}
error.user.disabled=User has been disabled
error.order.not_found=Order does not exist
error.order.status_invalid=Invalid order status
```

### 全局异常处理

全局异常处理器负责将不同异常类型转换成统一响应结构。它不应该直接写死中文提示，而应该通过 `MessageUtils` 获取当前语言消息。

异常处理优先级建议如下：

```text
参数校验异常
  ↓
业务异常
  ↓
权限异常
  ↓
系统异常
```

核心处理逻辑如下：

```java
@ExceptionHandler(BusinessException.class)
public ApiResult<Void> handleBusinessException(BusinessException exception) {
    String message = messageUtils.getMessage(
            exception.getMessageKey(),
            exception.getDefaultMessage(),
            exception.getArgs()
    );

    log.warn("业务异常：code={}, message={}", exception.getCode(), message);
    return ApiResult.fail(exception.getCode(), message);
}
```

处理结果示例：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在，用户ID：10001",
  "data": null
}
```

英文环境下返回：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User does not exist, user ID: 10001",
  "data": null
}
```

系统异常建议统一返回通用提示，不要将异常堆栈、SQL、服务器路径等内部信息返回给前端。

```java
@ExceptionHandler(Exception.class)
public ApiResult<Void> handleException(Exception exception) {
    String message = messageUtils.getMessage("error.system");
    log.error("系统异常：{}", exception.getMessage(), exception);
    return ApiResult.fail(ErrorCode.SYSTEM_ERROR.getCode(), message);
}
```

### 异常消息转换流程

异常消息转换流程用于保证业务异常、校验异常和系统异常都按统一规则返回当前语言文本。推荐流程如下：

```text
业务代码执行
  ↓
发现业务规则不满足
  ↓
抛出 BusinessException(ErrorCode.USER_NOT_FOUND, args)
  ↓
GlobalExceptionHandler 捕获 BusinessException
  ↓
读取 exception.messageKey 和 exception.args
  ↓
MessageUtils 根据 LocaleContextHolder 获取当前 Locale
  ↓
MessageSource 从 error_zh_CN.properties 或 error_en_US.properties 中读取消息
  ↓
使用 args 替换 {0}、{1} 等占位符
  ↓
封装 ApiResult 返回前端
```

接口验证示例：

```bash
curl -X GET "http://localhost:8080/api/users/999?lang=zh_CN"
```

中文返回：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在，用户ID：999",
  "data": null
}
```

英文请求：

```bash
curl -X GET "http://localhost:8080/api/users/999?lang=en_US"
```

英文返回：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User does not exist, user ID: 999",
  "data": null
}
```

参数校验验证示例：

```bash
curl -X POST "http://localhost:8080/api/users?lang=zh_CN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "invalid-email"
  }'
```

可能返回：

```json
{
  "code": "PARAM_INVALID",
  "message": "用户名不能为空",
  "data": null
}
```

英文环境下：

```bash
curl -X POST "http://localhost:8080/api/users?lang=en_US" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "invalid-email"
  }'
```

可能返回：

```json
{
  "code": "PARAM_INVALID",
  "message": "Username must not be blank",
  "data": null
}
```

最终需要保证三类消息来源清晰：

| 消息类型     | 资源文件                 | 示例 Key                         |
| ------------ | ------------------------ | -------------------------------- |
| 正常业务提示 | `messages*.properties`   | `user.create.success`            |
| 参数校验提示 | `validation*.properties` | `validation.user.name.not_blank` |
| 异常错误提示 | `error*.properties`      | `error.user.not_found`           |

这样可以避免 Controller、Service、异常处理器各自维护提示语，降低多语言扩展成本。



## 用户语言识别

用户语言识别用于确定当前请求应该使用哪一种语言。后端可以从请求参数、请求头、Cookie 或 Session 中读取语言标识，并最终转换为 Java `Locale` 对象。对于前后端分离项目，推荐优先使用“请求参数切换语言 + Cookie 保存语言偏好 + 请求头兜底识别”的组合方案。

推荐语言识别优先级如下：

```text
请求参数 lang
  ↓
Cookie language
  ↓
自定义请求头 X-Lang
  ↓
标准请求头 Accept-Language
  ↓
系统默认语言 zh_CN
```

### 请求头语言识别

请求头语言识别适合浏览器、移动端、第三方调用方自动传递语言偏好的场景。后端可以读取标准请求头 `Accept-Language`，也可以约定自定义请求头 `X-Lang`。

常见请求头示例：

```http
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
X-Lang: zh_CN
```

推荐后端同时兼容两种格式：

| 来源              | 示例             | 说明                       |
| ----------------- | ---------------- | -------------------------- |
| `X-Lang`          | `zh_CN`          | 前后端明确约定，解析简单   |
| `Accept-Language` | `zh-CN,zh;q=0.9` | 浏览器标准语言头           |
| 默认语言          | `zh_CN`          | 请求头不存在或不支持时兜底 |

如果系统主要面向 Web 前端，建议前端优先传递 `X-Lang`；如果系统同时提供 Open API，可以兼容 `Accept-Language`，降低第三方调用方接入成本。

文件位置：`src/main/java/io/github/atengk/util/LocaleUtils.java`

下面的工具类用于将前端传入的语言字符串统一转换为 `Locale`，同时兼容 `zh_CN` 和 `zh-CN` 格式。

```java
package io.github.atengk.util;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;
import java.util.Set;

/**
 * 语言环境工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class LocaleUtils {

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private static final Set<String> SUPPORT_LANGUAGE_SET = Set.of(
            "zh_CN",
            "en_US"
    );

    private LocaleUtils() {
    }

    /**
     * 解析语言标识。
     *
     * @param language 语言标识
     * @return 语言环境
     */
    public static Locale parseLocale(String language) {
        if (StrUtil.isBlank(language)) {
            return DEFAULT_LOCALE;
        }

        String normalizedLanguage = normalizeLanguage(language);
        if (!SUPPORT_LANGUAGE_SET.contains(normalizedLanguage)) {
            return DEFAULT_LOCALE;
        }

        String[] parts = normalizedLanguage.split("_");
        return Locale.of(parts[0], parts[1]);
    }

    /**
     * 解析请求头 Accept-Language。
     *
     * @param acceptLanguage 请求头语言
     * @return 语言环境
     */
    public static Locale parseAcceptLanguage(String acceptLanguage) {
        if (StrUtil.isBlank(acceptLanguage)) {
            return DEFAULT_LOCALE;
        }

        String firstLanguage = StrUtil.subBefore(acceptLanguage, ",", false);
        return parseLocale(firstLanguage);
    }

    /**
     * 规范化语言标识。
     *
     * @param language 原始语言标识
     * @return 规范化语言标识
     */
    public static String normalizeLanguage(String language) {
        if (StrUtil.isBlank(language)) {
            return "zh_CN";
        }

        return StrUtil.trim(language).replace("-", "_");
    }

    /**
     * 获取默认语言环境。
     *
     * @return 默认语言环境
     */
    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

}
```

请求头识别示例：

```text
zh-CN        -> zh_CN
zh_CN        -> zh_CN
en-US        -> en_US
en_US        -> en_US
fr-FR        -> zh_CN
空值          -> zh_CN
```

### 请求参数语言识别

请求参数语言识别适合用户主动切换语言的场景。前端切换语言时，可以在任意接口 URL 上追加 `lang` 参数，后端读取该参数后更新当前请求语言，并保存到 Cookie 或 Session 中。

请求示例：

```http
GET /api/users/1?lang=zh_CN
GET /api/users/1?lang=en_US
```

推荐参数规范如下：

| 参数名 | 示例值  | 说明                       |
| ------ | ------- | -------------------------- |
| `lang` | `zh_CN` | 简体中文                   |
| `lang` | `en_US` | 英文                       |
| `lang` | `zh-CN` | 可兼容，后端转换为 `zh_CN` |
| `lang` | `en-US` | 可兼容，后端转换为 `en_US` |

对于前后端分离项目，建议使用自定义 `LocaleResolver` 统一处理请求参数、请求头和 Cookie。这样可以避免多个解析器互相覆盖，也能明确语言识别优先级。

文件位置：`src/main/java/io/github/atengk/config/CompositeLocaleResolver.java`

下面的解析器按 `lang` 参数、Cookie、自定义请求头、标准请求头、默认语言的顺序识别当前请求语言。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.util.LocaleUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 组合语言解析器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class CompositeLocaleResolver implements LocaleResolver {

    private static final String LANG_PARAM = "lang";
    private static final String LANG_COOKIE = "language";
    private static final String LANG_HEADER = "X-Lang";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final int COOKIE_MAX_AGE_SECONDS = Math.toIntExact(Duration.ofDays(30).toSeconds());

    /**
     * 解析当前请求语言。
     *
     * @param request HTTP 请求
     * @return 语言环境
     */
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String paramLanguage = request.getParameter(LANG_PARAM);
        if (StrUtil.isNotBlank(paramLanguage)) {
            return LocaleUtils.parseLocale(paramLanguage);
        }

        String cookieLanguage = getCookieValue(request, LANG_COOKIE).orElse(null);
        if (StrUtil.isNotBlank(cookieLanguage)) {
            return LocaleUtils.parseLocale(cookieLanguage);
        }

        String headerLanguage = request.getHeader(LANG_HEADER);
        if (StrUtil.isNotBlank(headerLanguage)) {
            return LocaleUtils.parseLocale(headerLanguage);
        }

        String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER);
        if (StrUtil.isNotBlank(acceptLanguage)) {
            return LocaleUtils.parseAcceptLanguage(acceptLanguage);
        }

        return LocaleUtils.getDefaultLocale();
    }

    /**
     * 设置当前请求语言。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param locale   语言环境
     */
    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (response == null || locale == null) {
            return;
        }

        String language = locale.toString();
        Cookie cookie = new Cookie(LANG_COOKIE, language);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    /**
     * 获取 Cookie 值。
     *
     * @param request    HTTP 请求
     * @param cookieName Cookie 名称
     * @return Cookie 值
     */
    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || StrUtil.isBlank(cookieName)) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> StrUtil.equals(cookieName, cookie.getName()))
                .map(Cookie::getValue)
                .filter(StrUtil::isNotBlank)
                .findFirst();
    }

}
```

文件位置：`src/main/java/io/github/atengk/config/I18nConfig.java`

如果采用上面的组合解析器，可以替换上一节中的 `CookieLocaleResolver` 配置。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 国际化 MVC 配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置语言解析器。
     *
     * @return 语言解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new CompositeLocaleResolver();
    }

    /**
     * 配置语言切换拦截器。
     *
     * @return 语言切换拦截器
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();

        // 前端通过 ?lang=zh_CN 或 ?lang=en_US 切换语言
        interceptor.setParamName("lang");

        // 忽略非法 Locale，避免非法 lang 参数导致请求失败
        interceptor.setIgnoreInvalidLocale(true);

        return interceptor;
    }

    /**
     * 注册语言切换拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
```

需要注意：如果项目已经注册了 `CookieLocaleResolver`，就不要同时注册 `CompositeLocaleResolver`。一个 Spring MVC 应用中通常只保留一个 `LocaleResolver` Bean，否则会出现 Bean 冲突或语言解析行为不明确。

### Cookie 或 Session 语言存储

Cookie 和 Session 都可以用于保存用户语言偏好。前后端分离项目建议优先使用 Cookie；传统服务端渲染项目可以使用 Session。

两种方案对比如下：

| 存储方式 | 优点                           | 缺点                             | 推荐场景           |
| -------- | ------------------------------ | -------------------------------- | ------------------ |
| Cookie   | 前后端分离友好，浏览器自动携带 | 客户端可见，需要注意安全属性     | Web 前端、管理后台 |
| Session  | 服务端维护，客户端不可直接修改 | 依赖服务端会话，不适合无状态 API | 传统 MVC 项目      |
| 请求头   | 无状态，适合 Open API          | 每次请求都要传递                 | App、第三方接口    |
| 请求参数 | 切换语言直观                   | 不适合每次都传                   | 语言切换动作       |

Cookie 存储建议如下：

```text
Cookie 名称：language
Cookie 值：zh_CN / en_US
Cookie Path：/
Cookie Max-Age：30 天
HttpOnly：false
SameSite：Lax
```

如果语言 Cookie 需要被前端 JavaScript 读取，`HttpOnly` 应设置为 `false`。如果只由后端读写，并且前端不需要直接访问，可以设置为 `true`。

对于 Session 方案，可以使用 Spring MVC 提供的 `SessionLocaleResolver`：

文件位置：`src/main/java/io/github/atengk/config/SessionLocaleConfig.java`

下面的配置适合传统服务端 Web 项目，不建议与前后端分离的 Cookie 方案混用。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Session 语言配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class SessionLocaleConfig {

    /**
     * 配置 Session 语言解析器。
     *
     * @return 语言解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();

        // 默认使用简体中文
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

        return localeResolver;
    }

}
```

使用建议：

1. 前后端分离项目使用 `Cookie + 请求参数 + 请求头`。
2. App 或第三方 Open API 使用 `X-Lang` 或 `Accept-Language`。
3. 传统 MVC 项目可以使用 `SessionLocaleResolver`。
4. 不要在同一个应用中同时注册多个 `LocaleResolver`。
5. 语言值必须限制在系统支持范围内，不支持的语言统一回退到 `zh_CN`。

## 前后端协作

前后端协作的目标是统一语言标识传递方式、接口响应格式和多语言切换流程。后端负责返回当前语言的接口消息，前端负责保存用户语言偏好并在请求中传递语言标识。

### 前端传递语言标识

前端传递语言标识有三种常用方式：请求参数、请求头和 Cookie。建议语言切换动作使用请求参数，日常接口请求使用请求头或 Cookie。

推荐策略如下：

| 场景               | 推荐方式          | 示例                            |
| ------------------ | ----------------- | ------------------------------- |
| 用户点击语言切换   | 请求参数 `lang`   | `/api/users/current?lang=en_US` |
| 普通接口请求       | 请求头 `X-Lang`   | `X-Lang: en_US`                 |
| 浏览器自动保持语言 | Cookie `language` | `language=en_US`                |
| 第三方 API 调用    | `Accept-Language` | `Accept-Language: en-US`        |

前端语言值建议统一维护为枚举或常量，不要在页面中散落字符串。

文件位置：`src/utils/language.ts`

下面的 TypeScript 工具文件用于统一管理前端语言标识。

```typescript
export type Language = 'zh_CN' | 'en_US'

const LANGUAGE_KEY = 'language'

export const getLanguage = (): Language => {
  const language = localStorage.getItem(LANGUAGE_KEY)
  if (language === 'en_US' || language === 'zh_CN') {
    return language
  }
  return 'zh_CN'
}

export const setLanguage = (language: Language): void => {
  localStorage.setItem(LANGUAGE_KEY, language)
}

export const normalizeLanguage = (language: string): Language => {
  const normalized = language.replace('-', '_')
  if (normalized === 'en_US' || normalized === 'zh_CN') {
    return normalized
  }
  return 'zh_CN'
}
```

文件位置：`src/utils/request.ts`

下面的 Axios 配置用于在每次请求时自动携带 `X-Lang` 请求头。

```typescript
import axios from 'axios'
import { getLanguage } from './language'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  // 每次请求自动传递当前语言
  config.headers['X-Lang'] = getLanguage()
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 接口错误消息由后端按当前语言返回，前端直接展示 message
    return Promise.reject(error)
  }
)

export default request
```

语言切换时，前端可以先保存语言，再调用一个轻量接口让后端写入 Cookie。

```typescript
import request from '@/utils/request'
import { setLanguage, type Language } from '@/utils/language'

export const changeLanguage = async (language: Language): Promise<void> => {
  setLanguage(language)

  // 携带 lang 参数通知后端切换语言，并让后端写入 language Cookie
  await request.get('/users/current', {
    params: {
      lang: language
    }
  })
}
```

如果项目完全依赖请求头 `X-Lang`，可以不让后端写 Cookie；如果项目希望刷新页面后仍保持语言，建议同时使用 `localStorage` 和 Cookie。

### 接口返回消息规范

接口返回消息规范用于保证前端可以稳定消费后端返回结果。后端应保证所有接口都返回统一结构，`message` 字段始终是当前语言下可直接展示的文本。

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "Ateng"
  }
}
```

英文成功响应：

```json
{
  "code": "200",
  "message": "Operation successful",
  "data": {
    "id": 1,
    "username": "Ateng"
  }
}
```

失败响应示例：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

英文失败响应：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User does not exist",
  "data": null
}
```

推荐字段规范如下：

| 字段      | 类型     | 说明                                   |
| --------- | -------- | -------------------------------------- |
| `code`    | `String` | 业务编码或响应编码，前端可用于逻辑判断 |
| `message` | `String` | 当前语言下的用户提示语                 |
| `data`    | `Object` | 业务数据                               |
| `traceId` | `String` | 可选，链路追踪 ID，便于排查问题        |

如果项目需要排查线上问题，可以扩展响应结构，增加 `traceId` 字段。

文件位置：`src/main/java/io/github/atengk/common/ApiResult.java`

下面的响应结构在原有 `code`、`message`、`data` 基础上增加 `traceId`，适合生产环境排查问题。

```java
package io.github.atengk.common;

import cn.hutool.core.util.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 成功响应。
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>("200", message, data, IdUtil.fastSimpleUUID());
    }

    /**
     * 成功响应，无数据。
     *
     * @param message 响应消息
     * @return 统一响应结果
     */
    public static ApiResult<Void> success(String message) {
        return new ApiResult<>("200", message, null, IdUtil.fastSimpleUUID());
    }

    /**
     * 失败响应。
     *
     * @param code    响应编码
     * @param message 响应消息
     * @return 统一响应结果
     */
    public static ApiResult<Void> fail(String code, String message) {
        return new ApiResult<>(code, message, null, IdUtil.fastSimpleUUID());
    }

}
```

接口返回消息约定：

1. `message` 由后端根据当前语言生成，前端不再二次翻译。
2. `code` 保持稳定，不因语言变化而变化。
3. 前端业务判断只能依赖 `code`，不能依赖 `message`。
4. 参数校验、业务异常、系统异常都必须返回统一结构。
5. 后端日志可以记录中文内部信息，但接口返回消息必须走国际化资源。

### 多语言切换流程

多语言切换流程用于描述用户在前端切换语言后，前端、后端、Cookie 和接口返回之间的协作关系。推荐流程是：前端保存语言偏好，后端更新语言上下文，后续接口自动按新语言返回消息。

推荐切换流程如下：

```text
用户点击语言切换按钮
  ↓
前端保存语言到 localStorage
  ↓
前端调用接口并携带 ?lang=en_US
  ↓
后端 LocaleChangeInterceptor 识别 lang 参数
  ↓
LocaleResolver 更新当前 Locale
  ↓
后端写入 language Cookie
  ↓
前端刷新当前页面或重新请求接口
  ↓
后端按新语言返回 message
```

Vue 页面中可以这样触发语言切换：

文件位置：`src/views/system/LanguageSwitch.vue`

下面的组件展示一个简单的语言切换入口，切换后重新拉取当前用户信息验证后端消息语言。

```vue
<template>
  <div class="language-switch">
    <button :disabled="currentLanguage === 'zh_CN'" @click="handleChangeLanguage('zh_CN')">
      简体中文
    </button>
    <button :disabled="currentLanguage === 'en_US'" @click="handleChangeLanguage('en_US')">
      English
    </button>

    <p v-if="message">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '@/utils/request'
import { getLanguage, setLanguage, type Language } from '@/utils/language'

const currentLanguage = ref<Language>(getLanguage())
const message = ref('')

const handleChangeLanguage = async (language: Language): Promise<void> => {
  // 保存前端语言偏好
  setLanguage(language)
  currentLanguage.value = language

  // 通过 lang 参数通知后端切换语言
  const response = await request.get('/users/current', {
    params: {
      lang: language
    }
  })

  // 后端 message 已经是当前语言文本，前端直接展示
  message.value = response.message
}
</script>

<style scoped lang="scss">
.language-switch {
  display: flex;
  gap: 12px;
  align-items: center;

  button {
    padding: 6px 12px;
    cursor: pointer;

    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }
}
</style>
```

后端验证接口示例：

文件位置：`src/main/java/io/github/atengk/controller/UserCurrentController.java`

下面的接口用于验证当前语言环境下的接口返回消息。

```java
package io.github.atengk.controller;

import io.github.atengk.common.ApiResult;
import io.github.atengk.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 当前用户接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
public class UserCurrentController {

    private final MessageUtils messageUtils;

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息
     */
    @GetMapping("/api/users/current")
    public ApiResult<Map<String, Object>> currentUser() {
        String message = messageUtils.getMessage("user.query.success");

        Map<String, Object> data = Map.of(
                "id", 1L,
                "username", "Ateng"
        );

        return ApiResult.success(message, data);
    }

}
```

验证命令如下：

```bash
curl -X GET "http://localhost:8080/api/users/current?lang=zh_CN"
curl -X GET "http://localhost:8080/api/users/current?lang=en_US"
curl -X GET "http://localhost:8080/api/users/current" -H "X-Lang: en_US"
curl -X GET "http://localhost:8080/api/users/current" -H "Accept-Language: en-US,en;q=0.9"
```

预期中文响应：

```json
{
  "code": "200",
  "message": "用户查询成功",
  "data": {
    "id": 1,
    "username": "Ateng"
  },
  "traceId": "b6f6e7e7e6a74c50a29b8d1a6b19d601"
}
```

预期英文响应：

```json
{
  "code": "200",
  "message": "User queried successfully",
  "data": {
    "id": 1,
    "username": "Ateng"
  },
  "traceId": "b6f6e7e7e6a74c50a29b8d1a6b19d601"
}
```

多语言切换注意事项：

1. 前端切换语言后，应重新请求接口，不能复用旧接口返回的 `message`。
2. 前端页面自己的菜单、按钮、表单标签由前端 i18n 管理；后端只负责接口消息、异常消息、校验消息。
3. 后端返回的 `code` 不随语言变化，前端业务判断必须依赖 `code`。
4. 后端返回的 `message` 可以直接展示，不建议前端再根据 `code` 二次翻译。
5. 新增语言时，前后端要同时补充对应语言包，避免出现页面文本已切换但接口提示未切换的问题。

## 测试与验证

测试与验证用于确认国际化资源文件是否能正常加载、接口是否能按语言返回消息、参数校验消息是否能被正确转换，以及业务异常是否能根据当前语言环境返回对应提示。本章节承接前文的 `MessageSource`、`MessageUtils`、`ApiResult`、`BusinessException` 和全局异常处理设计。

### 资源文件加载验证

资源文件加载验证主要检查 `messages`、`validation`、`error` 三类资源文件是否能被 `MessageSource` 正确读取。建议在单元测试中分别验证中文、英文、默认兜底资源。

测试前应确认目录结构如下：

```text
src/main/resources/i18n
├── messages.properties
├── messages_zh_CN.properties
├── messages_en_US.properties
├── validation.properties
├── validation_zh_CN.properties
├── validation_en_US.properties
├── error.properties
├── error_zh_CN.properties
└── error_en_US.properties
```

文件位置：`src/test/java/io/github/atengk/I18nResourceLoadTest.java`

下面的测试类用于验证国际化资源文件是否可以按 Locale 正常加载。

```java
package io.github.atengk;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 国际化资源文件加载测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class I18nResourceLoadTest {

    @Autowired
    private MessageSource messageSource;

    /**
     * 验证中文资源加载。
     */
    @Test
    void testLoadChineseMessage() {
        String message = messageSource.getMessage("common.success", null, Locale.SIMPLIFIED_CHINESE);

        assertThat(StrUtil.isNotBlank(message)).isTrue();
        assertThat(message).isEqualTo("操作成功");
    }

    /**
     * 验证英文资源加载。
     */
    @Test
    void testLoadEnglishMessage() {
        String message = messageSource.getMessage("common.success", null, Locale.US);

        assertThat(StrUtil.isNotBlank(message)).isTrue();
        assertThat(message).isEqualTo("Operation successful");
    }

    /**
     * 验证占位符资源加载。
     */
    @Test
    void testLoadMessageWithArgs() {
        String message = messageSource.getMessage(
                "user.name.length",
                new Object[]{2, 20},
                Locale.SIMPLIFIED_CHINESE
        );

        assertThat(message).isEqualTo("用户名长度必须在 2 到 20 个字符之间");
    }

}
```

执行测试命令：

```bash
mvn test -Dtest=I18nResourceLoadTest
```

命令说明：

| 命令                          | 说明             |
| ----------------------------- | ---------------- |
| `mvn test`                    | 执行测试阶段     |
| `-Dtest=I18nResourceLoadTest` | 只执行指定测试类 |

如果测试失败，优先检查以下内容：

1. `application.yml` 中 `spring.messages.basename` 是否包含 `i18n/messages,i18n/validation,i18n/error`。
2. 资源文件是否放在 `src/main/resources/i18n/` 目录下。
3. Key 是否拼写一致。
4. 文件编码是否为 UTF-8。
5. 是否缺少默认兜底文件 `messages.properties`。

### 接口多语言验证

接口多语言验证用于确认同一个接口在不同语言环境下返回不同 `message`。建议分别通过请求参数、请求头和 Cookie 验证语言识别结果。

验证接口示例：

```http
GET /api/users/current?lang=zh_CN
GET /api/users/current?lang=en_US
```

中文验证命令：

```bash
curl -X GET "http://localhost:8080/api/users/current?lang=zh_CN"
```

预期响应：

```json
{
  "code": "200",
  "message": "用户查询成功",
  "data": {
    "id": 1,
    "username": "Ateng"
  }
}
```

英文验证命令：

```bash
curl -X GET "http://localhost:8080/api/users/current?lang=en_US"
```

预期响应：

```json
{
  "code": "200",
  "message": "User queried successfully",
  "data": {
    "id": 1,
    "username": "Ateng"
  }
}
```

使用请求头验证：

```bash
curl -X GET "http://localhost:8080/api/users/current" \
  -H "X-Lang: en_US"
```

使用标准请求头验证：

```bash
curl -X GET "http://localhost:8080/api/users/current" \
  -H "Accept-Language: en-US,en;q=0.9"
```

文件位置：`src/test/java/io/github/atengk/I18nApiTest.java`

下面的测试类用于通过 MockMvc 验证接口在不同语言下的响应消息。

```java
package io.github.atengk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 接口多语言测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class I18nApiTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证中文接口消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testChineseApiMessage() throws Exception {
        mockMvc.perform(get("/api/users/current").param("lang", "zh_CN"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("用户查询成功"));
    }

    /**
     * 验证英文接口消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testEnglishApiMessage() throws Exception {
        mockMvc.perform(get("/api/users/current").param("lang", "en_US"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("User queried successfully"));
    }

    /**
     * 验证请求头语言消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testHeaderLanguageMessage() throws Exception {
        mockMvc.perform(get("/api/users/current").header("X-Lang", "en_US"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("User queried successfully"));
    }

}
```

执行测试命令：

```bash
mvn test -Dtest=I18nApiTest
```

### 参数校验消息验证

参数校验消息验证用于确认 `@Valid`、`@Validated`、`@NotBlank`、`@Size`、`@Email` 等校验注解返回的消息是否能按当前语言解析。重点验证 `validation_zh_CN.properties` 和 `validation_en_US.properties` 是否生效。

验证请求示例：

```bash
curl -X POST "http://localhost:8080/api/users?lang=zh_CN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "invalid-email"
  }'
```

预期中文响应：

```json
{
  "code": "PARAM_INVALID",
  "message": "用户名不能为空",
  "data": null
}
```

英文验证请求：

```bash
curl -X POST "http://localhost:8080/api/users?lang=en_US" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "invalid-email"
  }'
```

预期英文响应：

```json
{
  "code": "PARAM_INVALID",
  "message": "Username must not be blank",
  "data": null
}
```

文件位置：`src/test/java/io/github/atengk/I18nValidationTest.java`

下面的测试类用于验证请求体参数校验消息是否能够按语言返回。

```java
package io.github.atengk;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 参数校验国际化测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class I18nValidationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证中文参数校验消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testChineseValidationMessage() throws Exception {
        String body = JSONUtil.createObj()
                .set("username", "")
                .set("email", "invalid-email")
                .toString();

        mockMvc.perform(post("/api/users")
                        .param("lang", "zh_CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    /**
     * 验证英文参数校验消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testEnglishValidationMessage() throws Exception {
        String body = JSONUtil.createObj()
                .set("username", "")
                .set("email", "invalid-email")
                .toString();

        mockMvc.perform(post("/api/users")
                        .param("lang", "en_US")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                .andExpect(jsonPath("$.message").value("Username must not be blank"));
    }

}
```

参数校验验证注意事项：

1. DTO 中的 `message` 必须使用 `{validation.xxx}` 格式。
2. `ValidatorConfig` 中必须将 `MessageSource` 设置给 `LocalValidatorFactoryBean`。
3. `validation_zh_CN.properties` 和 `validation_en_US.properties` 中必须存在对应 Key。
4. 如果返回的是 `{validation.user.name.not_blank}`，说明校验消息没有被正确解析。
5. 如果返回的是英文默认提示，说明当前 Locale 没有正确识别或资源文件缺失。

### 异常消息验证

异常消息验证用于确认业务异常和系统异常是否通过 `error*.properties` 资源文件转换为当前语言消息。重点验证 `BusinessException` 中的 `ErrorCode`、`messageKey` 和占位符参数是否能被正确处理。

验证请求示例：

```bash
curl -X GET "http://localhost:8080/api/users/999?lang=zh_CN"
```

预期中文响应：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在，用户ID：999",
  "data": null
}
```

英文验证请求：

```bash
curl -X GET "http://localhost:8080/api/users/999?lang=en_US"
```

预期英文响应：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User does not exist, user ID: 999",
  "data": null
}
```

文件位置：`src/test/java/io/github/atengk/I18nExceptionTest.java`

下面的测试类用于验证业务异常消息是否能按语言和占位符参数正确返回。

```java
package io.github.atengk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 异常消息国际化测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class I18nExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证中文业务异常消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testChineseBusinessExceptionMessage() throws Exception {
        mockMvc.perform(get("/api/users/999").param("lang", "zh_CN"))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("用户不存在，用户ID：999"));
    }

    /**
     * 验证英文业务异常消息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void testEnglishBusinessExceptionMessage() throws Exception {
        mockMvc.perform(get("/api/users/999").param("lang", "en_US"))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User does not exist, user ID: 999"));
    }

}
```

完整验证建议按以下顺序执行：

```bash
mvn test -Dtest=I18nResourceLoadTest
mvn test -Dtest=I18nApiTest
mvn test -Dtest=I18nValidationTest
mvn test -Dtest=I18nExceptionTest
```

如果需要一次性执行全部国际化测试，可以使用命名通配：

```bash
mvn test -Dtest=I18n*Test
```

## 开发规范

开发规范用于统一国际化 Key、资源文件、默认语言兜底和编码处理方式。多人协作时，国际化规范比代码实现更重要，否则容易出现 Key 重复、资源缺失、语言不一致、线上返回 Key 等问题。

### 资源 Key 命名规范

资源 Key 应保持稳定、清晰、可分组、可维护。建议使用小写字母、数字和点号，不建议使用驼峰、下划线混用或无意义编号。

推荐命名格式：

```text
业务域.模块.动作.结果
业务域.模块.字段.规则
error.模块.错误语义
validation.模块.字段.校验规则
```

推荐示例：

```properties
common.success=操作成功
common.fail=操作失败

user.query.success=用户查询成功
user.create.success=用户创建成功
user.update.success=用户更新成功
user.delete.success=用户删除成功

validation.user.name.not_blank=用户名不能为空
validation.user.name.size=用户名长度必须在 {min} 到 {max} 个字符之间
validation.user.email.invalid=邮箱格式不正确

error.user.not_found=用户不存在
error.user.disabled=用户已被禁用
error.order.status_invalid=订单状态不合法
```

不推荐示例：

```properties
success=操作成功
msg001=用户不存在
USER_NOT_FOUND=用户不存在
userNameError=用户名错误
error.user=用户异常
```

Key 命名规则建议如下：

| 规则                           | 说明                           |
| ------------------------------ | ------------------------------ |
| 使用小写字母和点号             | 例如 `user.create.success`     |
| Key 表达稳定语义               | 不要把临时业务描述写进 Key     |
| 模块前缀必须明确               | 例如 `user`、`order`、`common` |
| 校验消息统一 `validation` 前缀 | 便于与普通消息区分             |
| 异常消息统一 `error` 前缀      | 便于全局异常处理维护           |
| 不使用中文 Key                 | 避免编码、搜索和跨团队维护问题 |
| 不使用无意义编号               | 例如 `msg001` 不利于定位含义   |

资源文件新增规则：

1. 新增 Key 时必须同时补充默认、中文、英文资源。
2. 删除 Key 前必须确认代码中没有引用。
3. 修改 Key 名称需要同步修改 Java 代码、DTO 注解和测试用例。
4. 修改资源值时不应影响 Key 稳定性。
5. 所有占位符数量和含义必须在不同语言中保持一致。

### 默认语言兜底策略

默认语言兜底用于保证资源缺失、语言非法、请求未传语言时系统仍能返回可读消息。建议默认语言统一为 `zh_CN`，默认资源文件为 `messages.properties`、`validation.properties`、`error.properties`。

推荐兜底顺序如下：

```text
当前请求 Locale 对应资源
  ↓
默认资源文件 messages.properties / validation.properties / error.properties
  ↓
ErrorCode.defaultMessage 或业务默认消息
  ↓
统一兜底提示
```

具体策略如下：

| 场景                    | 处理策略                               |
| ----------------------- | -------------------------------------- |
| 未传语言参数            | 使用 Cookie 或请求头中的语言           |
| Cookie 和请求头均不存在 | 使用 `zh_CN`                           |
| 传入不支持的语言        | 回退到 `zh_CN`                         |
| 当前语言资源缺失        | 使用默认资源文件                       |
| 消息 Key 不存在         | 开发环境返回 Key，生产环境返回默认消息 |
| 系统异常                | 统一返回 `error.system` 对应消息       |

生产环境建议关闭直接返回 Key 的行为，避免用户看到类似 `error.user.not_found` 的内部资源标识。可以通过统一工具类控制兜底行为。

文件位置：`src/main/java/io/github/atengk/util/MessageUtils.java`

下面的工具方法适合生产环境使用：优先读取国际化消息，缺失时返回默认消息，再缺失时返回统一兜底提示。

```java
package io.github.atengk.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageUtils {

    private static final String DEFAULT_MESSAGE = "操作失败，请稍后重试";

    private final MessageSource messageSource;

    /**
     * 获取国际化消息。
     *
     * @param code 消息 Key
     * @return 国际化消息
     */
    public String getMessage(String code) {
        return getMessage(code, DEFAULT_MESSAGE, ArrayUtil.empty(Object.class));
    }

    /**
     * 获取国际化消息。
     *
     * @param code           消息 Key
     * @param defaultMessage 默认消息
     * @param args           占位符参数
     * @return 国际化消息
     */
    public String getMessage(String code, String defaultMessage, Object... args) {
        if (StrUtil.isBlank(code)) {
            log.warn("国际化消息 Key 为空，返回默认消息");
            return StrUtil.blankToDefault(defaultMessage, DEFAULT_MESSAGE);
        }

        Locale locale = LocaleContextHolder.getLocale();
        String fallbackMessage = StrUtil.blankToDefault(defaultMessage, DEFAULT_MESSAGE);
        return messageSource.getMessage(code, args, fallbackMessage, locale);
    }

}
```

开发环境可以保留以下配置，方便定位缺失 Key：

```yaml
spring:
  messages:
    # 开发环境可以返回 Key，便于发现资源缺失
    use-code-as-default-message: true
```

生产环境建议使用：

```yaml
spring:
  messages:
    # 生产环境不建议直接把 Key 暴露给用户
    use-code-as-default-message: false
```

### 国际化编码注意事项

国际化编码注意事项主要用于规避中文乱码、占位符错误、Locale 不一致、前后端重复翻译等常见问题。

资源文件编码要求如下：

| 项目       | 要求                                                |
| ---------- | --------------------------------------------------- |
| 文件格式   | `.properties`                                       |
| 文件编码   | UTF-8                                               |
| 文件位置   | `src/main/resources/i18n/`                          |
| Key 编码   | 英文小写、数字、点号                                |
| Value 编码 | 中文、英文或其他目标语言                            |
| 占位符     | `{0}`、`{1}` 或 Bean Validation 的 `{min}`、`{max}` |

`application.yml` 必须明确指定编码：

```yaml
spring:
  messages:
    # 国际化资源文件统一使用 UTF-8
    encoding: UTF-8
```

开发注意事项：

1. 不要在 Controller、Service、Exception 中硬编码中文或英文用户提示。
2. 不要在代码中拼接国际化消息，例如 `"用户" + username + "不存在"`。
3. 动态变量必须通过占位符传入，例如 `error.user.not_found=用户不存在，用户ID：{0}`。
4. 中文和英文资源的 Key 必须完全一致。
5. 不同语言中的占位符数量必须一致。
6. 前端页面文案由前端 i18n 管理，后端只负责接口消息、校验消息、异常消息。
7. 前端业务判断必须依赖 `code`，不能依赖 `message`。
8. 日志可以使用中文内部描述，但接口返回消息必须通过国际化资源转换。
9. 测试用例应覆盖中文、英文、非法语言和资源缺失场景。
10. 新增业务模块时，应同步新增 `messages`、`validation`、`error` 中对应资源。

推荐写法：

```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND, userId);
```

不推荐写法：

```java
throw new RuntimeException("用户不存在：" + userId);
```

推荐资源配置：

```properties
error.user.not_found=用户不存在，用户ID：{0}
```

英文资源配置：

```properties
error.user.not_found=User does not exist, user ID: {0}
```

最终落地时，应把国际化检查纳入代码评审范围。重点检查接口返回、异常抛出、参数校验、资源文件和测试用例是否同步更新，避免功能完成但多语言资源缺失。