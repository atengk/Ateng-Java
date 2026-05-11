# Spring Boot 集成 Fastjson1 开发

本文说明在 Spring Boot 3 项目中集成 Fastjson1 的技术背景与环境准备。需要明确的是，Spring Boot 3 默认 JSON 处理体系以 Jackson 为主，Fastjson1 更适合作为存量系统兼容、特定序列化格式迁移、历史接口保持一致性时的补充方案，而不建议作为新项目的首选 JSON 库。Spring Boot 官方文档明确说明 Spring Boot 集成 Gson、Jackson、JSON-B，其中 Jackson 是默认且首选的 JSON 库；Jackson 位于 classpath 时会自动配置 `ObjectMapper`。([Home](https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/index.html?utm_source=chatgpt.com))

## 技术背景

本节用于说明 Spring Boot 3 默认序列化机制、Fastjson1 的适用边界，以及在 Spring Boot 3 环境下使用 Fastjson1 时需要提前评估的兼容性和安全问题。

### Spring Boot 3 序列化机制

Spring Boot 3 的 Web 场景通常通过 `spring-boot-starter-web` 引入 Spring MVC 与 JSON 序列化能力。默认情况下，Spring MVC 使用 `HttpMessageConverter` 完成 HTTP 请求体与响应体之间的对象转换，JSON 场景主要由 Jackson 的 `MappingJackson2HttpMessageConverter` 负责处理。

`HttpMessageConverter` 的核心职责是将 HTTP 输入输出消息与 Java 对象相互转换。Spring Framework 中的 `MappingJackson2HttpMessageConverter` 基于 Jackson 2.x 的 `ObjectMapper` 读写 JSON，默认支持 `application/json` 与 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

在 Spring Boot 3 中，常见序列化流程如下：

1. Controller 方法接收 `@RequestBody` 参数。
2. Spring MVC 根据请求头 `Content-Type` 选择可读取的 `HttpMessageConverter`。
3. JSON 请求体被转换为 Java 对象。
4. Controller 返回 Java 对象。
5. Spring MVC 根据响应类型和 `Accept` 请求头选择可写出的 `HttpMessageConverter`。
6. Java 对象被序列化为 JSON 响应体。

默认情况下，如果项目未显式调整 JSON Converter，Spring Boot 会使用 Jackson 作为主要 JSON 序列化方案。Spring Boot MVC 自动配置会支持 `HttpMessageConverters`，并且可以通过注册自定义 `HttpMessageConverter` Bean 或使用 `extendMessageConverters` 扩展默认列表。([Home](https://docs.spring.io/spring-boot/docs/3.2.11/reference/htmlsingle/?utm_source=chatgpt.com))

### Fastjson1 使用定位

Fastjson1 是 Alibaba 开源的 JSON 处理库，用于 Java 对象与 JSON 字符串之间的序列化和反序列化。Fastjson 官方 Wiki 说明其目标包括提供 `toJSONString()`、`parseObject()` 等简单 API，并支持复杂 Java 对象、泛型、自定义对象表示等能力。([GitHub](https://github.com/alibaba/fastjson/wiki?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，Fastjson1 的定位建议如下：

| 使用场景           | 是否建议 | 说明                                                         |
| ------------------ | -------- | ------------------------------------------------------------ |
| 新项目默认 JSON 库 | 不建议   | Spring Boot 3 默认生态围绕 Jackson 自动配置，兼容性和维护成本更低。 |
| 存量接口迁移       | 可以使用 | 如果旧系统响应字段格式、日期格式、Null 值策略依赖 Fastjson1，可通过 Converter 方式保持兼容。 |
| 单点工具类序列化   | 可以使用 | 例如日志脱敏、缓存 JSON 字符串、局部对象转换等，不一定需要替换全局 Jackson。 |
| 全局替换 Jackson   | 谨慎使用 | 需要完整验证 Controller 请求体、响应体、异常响应、`LocalDateTime`、泛型对象等场景。 |
| 反序列化不可信输入 | 不建议   | Fastjson1 历史上多次出现 AutoType 相关安全风险，应避免处理不可信多态反序列化输入。 |

实际项目中更推荐将 Fastjson1 控制在明确边界内使用。若只是局部 JSON 字符串处理，可以直接使用 `JSON.toJSONString()`、`JSON.parseObject()`；若必须影响接口请求与响应，再通过 `FastJsonHttpMessageConverter` 接入 Spring MVC。

### 版本兼容注意事项

Spring Boot 3 基于 Spring Framework 6，并要求 Java 17 作为最低运行版本。Spring Boot 3.0.0 官方文档说明其要求 Java 17，并依赖 Spring Framework 6.0.2 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com))

Fastjson1 当前常见安全修复版本为 `1.2.83`。Maven Central 中 `com.alibaba:fastjson:1.2.83` 的描述为 Java JSON parser/generator，并提供标准 Maven 依赖坐标。([Maven Central](https://central.sonatype.com/artifact/com.alibaba/fastjson/1.2.83?utm_source=chatgpt.com)) Fastjson 官方 1.2.83 Release 说明该版本是安全修复版本，并修复了 JDK 17 下 `setAccessible` 报错的问题。([GitHub](https://github.com/alibaba/fastjson/releases?utm_source=chatgpt.com))

需要特别注意以下兼容点：

| 项目          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| JDK 版本      | Spring Boot 3 最低要求 JDK 17，因此 Fastjson1 至少要在 JDK 17 下完成启动和接口回归测试。 |
| Spring 版本   | Spring Boot 3 使用 Spring Framework 6，虽然 Fastjson1 提供 Spring Converter 支持，但它不是 Spring Boot 3 官方默认 JSON 方案。 |
| Jakarta 迁移  | Spring Boot 3 生态已从 Java EE 迁移到 Jakarta EE 命名空间。Fastjson1 的普通 JSON API 不受影响，但涉及 Web、Servlet、JAX-RS 扩展时需要额外验证。 |
| AutoType 安全 | 不建议启用 AutoType。Fastjson 官方说明 1.2.68 之后支持 SafeMode，开启后会完全禁用 AutoType。([GitHub](https://github.com/alibaba/fastjson/wiki/fastjson_safemode?utm_source=chatgpt.com)) |
| 漏洞版本      | GitHub Advisory 记录了 CVE-2022-25845，受影响版本为 `>= 1.2.25, < 1.2.83`，修复版本为 `1.2.83`。([GitHub](https://github.com/advisories/GHSA-pv7h-hx5h-mgfj?utm_source=chatgpt.com)) |
| 长期方案      | Fastjson 官方仓库说明 Fastjson 2.0.x 已发布，并建议升级到更快、更安全的 Fastjson2。([GitHub](https://github.com/alibaba/fastjson?utm_source=chatgpt.com)) |

因此，本文后续示例统一使用 `fastjson:1.2.83`，并默认开启 SafeMode 或使用 JVM 参数禁用 AutoType 能力。

## 环境准备

本节给出 Spring Boot 3 集成 Fastjson1 前需要确认的 JDK、Spring Boot、Maven 依赖和默认 Jackson 配置。这里的配置只完成基础环境准备，具体 Converter 注册会在后续“Fastjson1 集成方式”章节展开。

### JDK 与 Spring Boot 版本

建议使用如下版本组合：

| 组件        | 推荐版本     | 说明                                                         |
| ----------- | ------------ | ------------------------------------------------------------ |
| JDK         | 17 或更高    | Spring Boot 3 的最低要求是 Java 17。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com)) |
| Spring Boot | 3.x          | 本文面向 Spring Boot 3，不适用于 Spring Boot 2 的完整配置方式。 |
| Maven       | 3.6.3 或更高 | Spring Boot 3.x 后续版本通常要求更高 Maven 版本，建议统一使用 3.6.3+。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com)) |
| JSON 默认库 | Jackson      | `spring-boot-starter-web` 默认会带入 Jackson 相关依赖。Spring Boot 文档说明 Jackson 是默认且首选 JSON 库。([Home](https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/index.html?utm_source=chatgpt.com)) |
| Fastjson1   | 1.2.83       | 当前 Fastjson1 常用安全修复版本。低于该版本需要重点评估 CVE-2022-25845。([GitHub](https://github.com/advisories/GHSA-pv7h-hx5h-mgfj?utm_source=chatgpt.com)) |

项目启动后可以先确认 Java 与 Maven 版本。

```bash
# 查看 JDK 版本，Spring Boot 3 要求 Java 17+
java -version

# 查看 Maven 版本，建议使用 Maven 3.6.3+
mvn -version
```

如果项目已经存在，可以通过以下命令检查实际依赖树，确认 Fastjson 与 Jackson 是否同时存在：

```bash
# 查看 JSON 相关依赖
mvn dependency:tree | grep -E "fastjson|jackson|spring-boot-starter-json"
```

### Fastjson1 依赖配置

下面的 Maven 配置放在项目根目录 `pom.xml` 中，用于引入 Spring Boot Web、Fastjson1 和 Hutool。Hutool 不是 Fastjson1 必需依赖，但在后续示例中可用于字符串、集合、日期等常见工具处理。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 Spring MVC、内置 Tomcat、JSON 默认序列化能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Fastjson1：用于兼容存量 Fastjson 序列化和反序列化逻辑 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.83</version>
    </dependency>

    <!-- Hutool：常用 Java 工具类，便于后续示例处理字符串、日期、集合等逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、配置类中的样板代码，按项目规范决定是否使用 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目使用 Gradle，可以使用如下配置：

```gradle
dependencies {
    // Spring Boot Web：提供 Spring MVC 与默认 JSON 能力
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Fastjson1：建议使用 1.2.83 安全修复版本
    implementation 'com.alibaba:fastjson:1.2.83'

    // Hutool：后续示例中可用于常用工具处理
    implementation 'cn.hutool:hutool-all:5.8.36'

    // Lombok：减少样板代码
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

如果项目只是在业务代码中局部使用 Fastjson1，不需要额外配置 Spring MVC Converter。只有当接口请求体或响应体必须由 Fastjson1 接管时，才需要在后续章节注册 `FastJsonHttpMessageConverter`。

### Jackson 默认配置说明

Spring Boot 3 默认会自动配置 Jackson。只要 Jackson 存在于 classpath，Spring Boot 就会自动创建 `ObjectMapper` Bean，并提供配置属性用于定制 `ObjectMapper`。([Home](https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/index.html?utm_source=chatgpt.com))

默认 Jackson 机制主要影响以下场景：

| 场景                    | 默认处理方式                                        |
| ----------------------- | --------------------------------------------------- |
| `@RequestBody` 请求体   | 使用 Jackson 将 JSON 反序列化为 Java 对象。         |
| Controller 返回对象     | 使用 Jackson 将 Java 对象序列化为 JSON。            |
| `application/json` 响应 | 由 `MappingJackson2HttpMessageConverter` 处理。     |
| 日期时间格式            | 可通过 `spring.jackson.*` 或 `ObjectMapper` 定制。  |
| Null 值策略             | 默认按 Jackson 规则输出，可通过全局配置或注解调整。 |

下面是一个常见的 Jackson 默认配置示例，放在 `src/main/resources/application.yml`。即使后续接入 Fastjson1，也建议保留清晰的 Jackson 配置，避免项目中部分接口仍由 Jackson 处理时表现不一致。

```yaml
spring:
  jackson:
    # 日期时间输出格式，主要影响 java.util.Date 等类型
    date-format: yyyy-MM-dd HH:mm:ss

    # 时区配置，避免服务器时区差异导致时间输出偏移
    time-zone: Asia/Shanghai

    serialization:
      # Date、LocalDateTime 等时间类型按格式输出，而不是时间戳
      write-dates-as-timestamps: false

      # 空 Bean 不直接抛出序列化异常，按项目规范决定是否开启
      fail-on-empty-beans: false

    deserialization:
      # JSON 中出现 Java 对象不存在的字段时不报错，便于接口兼容
      fail-on-unknown-properties: false

    default-property-inclusion: non_null
```

需要注意，以上 `spring.jackson.*` 配置只对 Jackson 生效，不会自动影响 Fastjson1。如果后续通过 `FastJsonHttpMessageConverter` 接管接口 JSON 序列化，需要在 Fastjson1 的 `FastJsonConfig` 中单独配置日期格式、Null 值输出、字段命名策略、反序列化特性等参数。

在同一个 Spring Boot 3 项目中，Jackson 与 Fastjson1 可以同时存在。关键在于 `HttpMessageConverter` 的注册顺序和支持的 `MediaType`。如果 Fastjson1 Converter 排在 Jackson Converter 之前，并且支持 `application/json`，则 Controller 的 JSON 响应可能优先由 Fastjson1 输出；反之仍由 Jackson 输出。Spring Framework 文档说明可以通过 `configureMessageConverters()` 替换默认 Converter，也可以通过 `extendMessageConverters()` 在默认列表末尾做扩展；在 Spring Boot 应用中，更推荐通过 Boot 的 `HttpMessageConverters` 机制或 `extendMessageConverters` 修改列表。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/reference/web/webmvc/mvc-config/message-converters.html?utm_source=chatgpt.com))



## Fastjson1 集成方式

本节说明如何在 Spring Boot 3 中通过 `HttpMessageConverter` 接入 Fastjson1。Spring MVC 允许通过 `configureMessageConverters()` 配置消息转换器，也允许通过 `extendMessageConverters()` 在默认转换器列表初始化后进行扩展；官方文档同时强调转换器顺序会影响匹配结果，尤其是客户端使用宽泛 `Accept` 头时，靠前的 Converter 会优先处理。Spring Boot 应用中通常更适合使用 `extendMessageConverters()`，避免直接替换掉默认转换器列表。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-framework/reference/web/webmvc/mvc-config/message-converters.html?utm_source=chatgpt.com))

### HttpMessageConverter 扩展

`HttpMessageConverter` 是 Spring MVC 请求体反序列化和响应体序列化的核心扩展点。Fastjson1 提供了 `FastJsonHttpMessageConverter`，该类位于 `com.alibaba.fastjson.support.spring` 包下，并继承自 Spring 的 `AbstractHttpMessageConverter`。([Javadoc](https://www.javadoc.io/static/com.alibaba/fastjson/1.2.83/com/alibaba/fastjson/support/spring/package-tree.html?utm_source=chatgpt.com))

在 Spring Boot 3 中集成 Fastjson1 时，不建议直接删除所有默认 Converter。更稳妥的做法是保留 Spring Boot 默认 Converter，仅将 Fastjson1 Converter 插入到 Jackson Converter 之前，使 `application/json` 的普通对象响应优先由 Fastjson1 处理，同时保留字符串、字节数组、文件下载、表单等默认能力。

建议的文件结构如下：

```text
src/main/java/io/github/atengk/config/FastjsonWebMvcConfig.java
src/main/java/io/github/atengk/common/ApiResult.java
src/main/java/io/github/atengk/order/dto/OrderCreateRequest.java
src/main/java/io/github/atengk/order/vo/OrderVO.java
src/main/java/io/github/atengk/order/controller/OrderController.java
```

下面的配置类用于注册 Fastjson1 消息转换器，并将其插入到 Jackson JSON Converter 之前。

文件位置：`src/main/java/io/github/atengk/config/FastjsonWebMvcConfig.java`

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fastjson1 Web MVC 配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class FastjsonWebMvcConfig implements WebMvcConfigurer {

    /**
     * 扩展 Spring MVC 消息转换器
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        if (CollUtil.isEmpty(converters)) {
            log.warn("消息转换器列表为空，将直接注册 Fastjson1 转换器");
            converters.add(createFastJsonHttpMessageConverter());
            return;
        }

        // 避免重复注册 Fastjson1 Converter
        converters.removeIf(FastJsonHttpMessageConverter.class::isInstance);

        FastJsonHttpMessageConverter fastJsonConverter = createFastJsonHttpMessageConverter();

        int jacksonIndex = findJacksonConverterIndex(converters);
        if (jacksonIndex >= 0) {
            converters.add(jacksonIndex, fastJsonConverter);
            log.info("Fastjson1 转换器已注册到 Jackson 转换器之前，索引：{}", jacksonIndex);
            return;
        }

        converters.add(0, fastJsonConverter);
        log.info("未找到 Jackson 转换器，Fastjson1 转换器已注册到首位");
    }

    /**
     * 创建 Fastjson1 消息转换器
     *
     * @return Fastjson1 消息转换器
     */
    private FastJsonHttpMessageConverter createFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        converter.setFastJsonConfig(createFastJsonConfig());
        return converter;
    }

    /**
     * 创建 Fastjson1 全局配置
     *
     * @return Fastjson1 配置
     */
    private FastJsonConfig createFastJsonConfig() {
        FastJsonConfig config = new FastJsonConfig();

        // 字符集配置，避免中文响应乱码
        config.setCharset(StandardCharsets.UTF_8);

        // 日期格式配置，主要影响 java.util.Date；LocalDateTime 建议配合 @JSONField 使用
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        // 序列化配置
        config.setSerializeConfig(createSerializeConfig());

        // 反序列化配置
        config.setParserConfig(createParserConfig());

        // 序列化特性配置
        config.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.WriteBigDecimalAsPlain,
                SerializerFeature.DisableCircularReferenceDetect
        );

        // 反序列化特性配置
        config.setFeatures(
                Feature.AllowISO8601DateFormat,
                Feature.IgnoreNotMatch
        );

        return config;
    }

    /**
     * 创建序列化配置
     *
     * @return 序列化配置
     */
    private SerializeConfig createSerializeConfig() {
        SerializeConfig serializeConfig = new SerializeConfig();

        // 字段命名策略：Java userName 输出为 JSON user_name
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        return serializeConfig;
    }

    /**
     * 创建反序列化配置
     *
     * @return 反序列化配置
     */
    private ParserConfig createParserConfig() {
        ParserConfig parserConfig = ParserConfig.getGlobalInstance();

        // 安全配置：关闭 AutoType，并启用 SafeMode
        parserConfig.setAutoTypeSupport(false);
        parserConfig.setSafeMode(true);

        // 字段命名策略：JSON user_name 反序列化为 Java userName
        parserConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        return parserConfig;
    }

    /**
     * 查找 Jackson 转换器位置
     *
     * @param converters 消息转换器列表
     * @return Jackson 转换器索引，未找到返回 -1
     */
    private int findJacksonConverterIndex(List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                return i;
            }
        }
        return -1;
    }

}
```

这段配置保留了默认 Converter，只是调整 JSON 对象的处理优先级。这样做比完全重写 `configureMessageConverters()` 更安全，因为静态资源、字符串响应、文件下载、表单数据等默认转换能力不会被意外移除。

### WebMvcConfigurer 配置

`WebMvcConfigurer` 是 Spring MVC 的配置扩展接口。对于 Fastjson1 集成，常用方式有两种：

| 配置方式                     | 是否推荐 | 说明                                                         |
| ---------------------------- | -------- | ------------------------------------------------------------ |
| `extendMessageConverters`    | 推荐     | 在默认 Converter 初始化后追加或调整顺序，不破坏 Spring Boot 默认能力。 |
| `configureMessageConverters` | 谨慎使用 | 会直接配置 Converter 列表，容易导致默认 Converter 丢失。     |

Spring Framework 文档说明，`configureMessageConverters()` 用于配置读写请求和响应的 Converter；如果使用该方法，会影响默认 Converter 注册，而 `extendMessageConverters()` 用于在默认列表基础上扩展或修改 Converter。([Home](https://docs.spring.io/spring-framework/docs/6.2.8/javadoc-api/org/springframework/web/servlet/config/annotation/WebMvcConfigurer.html?utm_source=chatgpt.com))

实际项目中建议遵循以下规则：

1. 只想让 Fastjson1 接管 JSON 请求和响应时，使用 `extendMessageConverters()`。
2. 不要给 Fastjson1 Converter 配置 `MediaType.ALL`，否则可能误处理非 JSON 响应。
3. Fastjson1 Converter 放在 Jackson Converter 之前即可，不要放在 `StringHttpMessageConverter` 之前。
4. 如果部分接口仍希望使用 Jackson，需要通过接口返回类型、媒体类型或局部序列化方式做隔离。

### 序列化特性配置

Fastjson1 的序列化行为主要通过 `SerializerFeature` 控制。常用特性包括输出 Null 字段、日期格式化、关闭循环引用检测、BigDecimal 普通字符串输出等。Fastjson 的 `SerializerFeature` 枚举中包含 `WriteMapNullValue`、`WriteNullStringAsEmpty`、`WriteNullNumberAsZero`、`WriteDateUseDateFormat`、`DisableCircularReferenceDetect`、`WriteBigDecimalAsPlain` 等常用配置项。([Javadoc](https://www.javadoc.io/static/com.alibaba/fastjson/1.2.78/com/alibaba/fastjson/serializer/SerializerFeature.html?utm_source=chatgpt.com))

常用配置说明如下：

| 配置项                           | 作用                                           | 使用建议                                     |
| -------------------------------- | ---------------------------------------------- | -------------------------------------------- |
| `WriteMapNullValue`              | 输出值为 `null` 的字段                         | 需要前后端字段结构稳定时开启。               |
| `WriteNullStringAsEmpty`         | 字符串 `null` 输出为空字符串                   | 谨慎开启，可能混淆“未传值”和“空字符串”。     |
| `WriteNullListAsEmpty`           | 集合 `null` 输出为空数组                       | 前端列表渲染友好。                           |
| `WriteNullNumberAsZero`          | 数字 `null` 输出为 `0`                         | 谨慎开启，可能造成业务含义错误。             |
| `WriteNullBooleanAsFalse`        | 布尔 `null` 输出为 `false`                     | 谨慎开启，可能造成状态误判。                 |
| `WriteDateUseDateFormat`         | 使用 `FastJsonConfig#setDateFormat` 的日期格式 | 适合统一 `Date` 输出格式。                   |
| `DisableCircularReferenceDetect` | 禁用循环引用检测                               | 响应更简洁，但对象存在循环引用时可能栈溢出。 |
| `WriteBigDecimalAsPlain`         | BigDecimal 使用普通数字格式输出                | 避免科学计数法。                             |

推荐的基础配置如下：

```java
config.setSerializerFeatures(
        SerializerFeature.WriteMapNullValue,
        SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.WriteBigDecimalAsPlain,
        SerializerFeature.DisableCircularReferenceDetect
);
```

如果接口必须兼容前端固定字段结构，可以额外开启 Null 值转换：

```java
config.setSerializerFeatures(
        SerializerFeature.WriteMapNullValue,
        SerializerFeature.WriteNullListAsEmpty,
        SerializerFeature.WriteNullStringAsEmpty,
        SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.WriteBigDecimalAsPlain,
        SerializerFeature.DisableCircularReferenceDetect
);
```

不建议在全局默认开启 `WriteNullNumberAsZero` 和 `WriteNullBooleanAsFalse`。数字和布尔值的 `null` 往往代表“未知”“未设置”或“不适用”，直接转换为 `0` 或 `false` 容易造成业务判断错误。

### 反序列化特性配置

Fastjson1 的反序列化行为主要由 `ParserConfig` 和 `Feature` 控制。`FastJsonConfig` 提供了 `setParserConfig()`、`setFeatures()`、`setSerializeConfig()`、`setSerializerFeatures()`、`setDateFormat()`、`setCharset()` 等配置入口。([Javadoc](https://www.javadoc.io/static/com.alibaba/fastjson/2.0.17.android/com/alibaba/fastjson/support/config/FastJsonConfig.html?utm_source=chatgpt.com))

推荐配置如下：

```java
ParserConfig parserConfig = ParserConfig.getGlobalInstance();
parserConfig.setAutoTypeSupport(false);
parserConfig.setSafeMode(true);

config.setParserConfig(parserConfig);
config.setFeatures(
        Feature.AllowISO8601DateFormat,
        Feature.IgnoreNotMatch
);
```

Fastjson 官方 SafeMode 文档说明，1.2.68 之后支持 SafeMode，开启后会完全禁用 AutoType；官方也给出了 `ParserConfig.getGlobalInstance().setSafeMode(true)`、JVM 参数 `-Dfastjson.parser.safeMode=true`、`fastjson.properties` 三种配置方式。([GitHub](https://github.com/alibaba/fastjson/wiki/fastjson_safemode?utm_source=chatgpt.com))

生产环境建议在 JVM 参数中同时配置 SafeMode：

```bash
# 启动应用时开启 Fastjson1 SafeMode
java -Dfastjson.parser.safeMode=true -jar app.jar
```

或者在容器环境中配置：

```bash
# Docker / Kubernetes 环境中可通过 JAVA_TOOL_OPTIONS 注入
export JAVA_TOOL_OPTIONS="-Dfastjson.parser.safeMode=true"
```

如果项目不依赖 AutoType，不要开启 `SupportAutoType`，也不要在序列化时使用 `SerializerFeature.WriteClassName`。这两个配置会引入 `@type` 类型信息，容易扩大反序列化攻击面。

## JSON 序列化处理

本节说明 Fastjson1 在日期时间、Null 值、字段命名和大数值精度方面的常见处理方式。这些内容直接影响 Controller 的请求体解析和响应体输出格式。

### 日期时间格式化

Fastjson1 的 `FastJsonConfig#setDateFormat` 可以设置全局日期格式，通常配合 `SerializerFeature.WriteDateUseDateFormat` 使用。该方式主要适合 `java.util.Date` 类型；对于 `LocalDateTime`、`LocalDate`、`LocalTime` 等 Java 8 时间类型，更建议在字段上使用 `@JSONField(format = "...")` 明确声明格式。

DTO 和 VO 中的时间字段建议统一使用如下格式：

```java
@JSONField(format = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

这种方式的优点是局部清晰，不依赖全局 Converter 行为，也便于不同字段使用不同时间格式。例如创建时间可以精确到秒，生日字段可以只保留日期。

### Null 值处理

Fastjson1 默认不会输出部分 Null 字段。是否输出 Null 值应根据接口契约决定，而不是单纯为了“看起来完整”。

推荐策略如下：

| 字段类型 | 推荐处理                       | 说明                             |
| -------- | ------------------------------ | -------------------------------- |
| 字符串   | 保留 `null` 或局部转为空字符串 | 全局转空字符串可能影响业务语义。 |
| 集合     | 可转为空数组                   | 前端渲染列表更方便。             |
| 数字     | 保留 `null`                    | 避免把未设置误判为 `0`。         |
| 布尔     | 保留 `null`                    | 避免把未知状态误判为 `false`。   |
| 对象     | 保留 `null`                    | 便于表达关联对象不存在。         |

如果接口要求字段结构稳定，可以开启：

```java
SerializerFeature.WriteMapNullValue
```

如果接口要求空集合统一输出 `[]`，可以额外开启：

```java
SerializerFeature.WriteNullListAsEmpty
```

不建议全局开启以下配置：

```java
SerializerFeature.WriteNullNumberAsZero
SerializerFeature.WriteNullBooleanAsFalse
```

这两个配置会改变业务含义。例如库存字段为 `null` 可能表示“未同步”，而不是库存为 `0`；审核状态为 `null` 可能表示“未进入审核流程”，而不是审核失败。

### 字段命名策略

字段命名策略用于处理 Java 字段名与 JSON 字段名之间的转换。例如 Java 中通常使用 `orderNo`，而 JSON 接口可能要求使用 `order_no`。

Fastjson1 可以通过 `SerializeConfig` 和 `ParserConfig` 配置全局命名策略：

```java
SerializeConfig serializeConfig = new SerializeConfig();
serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

ParserConfig parserConfig = ParserConfig.getGlobalInstance();
parserConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
```

全局配置后，序列化和反序列化会按 `snake_case` 风格处理：

```json
{
  "order_no": "SO202605090001",
  "order_amount": 199.99,
  "create_time": "2026-05-09 10:30:00"
}
```

如果只有少数字段需要特殊名称，建议优先使用 `@JSONField(name = "...")`，不要全局调整命名策略：

```java
@JSONField(name = "order_no")
private String orderNo;
```

全局命名策略适合整个项目接口规范统一为 `snake_case` 的场景；注解适合局部兼容历史字段、第三方接口字段或个别非标准字段。

### 大数值精度处理

JSON 本身不区分整数、长整数和高精度小数，但前端 JavaScript 的 `Number` 存在安全整数范围限制。后端如果直接输出超长 `Long`，前端可能出现精度丢失。

常见处理策略如下：

| 类型              | 风险                 | 推荐处理                                             |
| ----------------- | -------------------- | ---------------------------------------------------- |
| `Long` 主键       | 前端精度丢失         | 输出为字符串。                                       |
| `BigInteger`      | 前端精度丢失         | 输出为字符串。                                       |
| `BigDecimal` 金额 | 科学计数法或精度误读 | 使用 `BigDecimal`，并开启 `WriteBigDecimalAsPlain`。 |
| `Double` 金额     | 二进制浮点误差       | 金额字段不要使用 `Double`。                          |

对于 Long 类型 ID，可以使用 Fastjson1 的 `ToStringSerializer`：

```java
@JSONField(serializeUsing = ToStringSerializer.class)
private Long orderId;
```

对于金额字段，统一使用 `BigDecimal`：

```java
private BigDecimal orderAmount;
```

并在全局序列化配置中开启：

```java
SerializerFeature.WriteBigDecimalAsPlain
```

这样可以避免 `BigDecimal` 输出为科学计数法，便于前端展示和接口联调。

## Controller 接口使用

本节给出一个订单接口示例，覆盖请求参数反序列化、响应结果序列化和统一响应对象处理。示例使用 Spring Boot 3、Fastjson1、Lombok 和 Hutool，代码可以直接放入普通 Web 项目中测试。

### 请求参数反序列化

请求参数反序列化指的是将 HTTP JSON 请求体转换为 Java DTO 对象。Fastjson1 Converter 生效后，`@RequestBody` 的 JSON 解析会由 Fastjson1 处理。

下面的 DTO 用于接收创建订单请求。

文件位置：`src/main/java/io/github/atengk/order/dto/OrderCreateRequest.java`

```java
package io.github.atengk.order.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderCreateRequest {

    /**
     * 订单编号
     */
    @JSONField(name = "order_no")
    private String orderNo;

    /**
     * 订单金额
     */
    @JSONField(name = "order_amount")
    private BigDecimal orderAmount;

    /**
     * 下单时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 买家名称
     */
    @JSONField(name = "buyer_name")
    private String buyerName;

}
```

请求示例：

```json
{
  "order_no": "SO202605090001",
  "order_amount": 199.99,
  "create_time": "2026-05-09 10:30:00",
  "buyer_name": "Ateng"
}
```

这里使用 `@JSONField(name = "...")` 显式绑定 JSON 字段名。即使项目没有配置全局 `SnakeCase` 命名策略，该 DTO 也可以稳定解析 `snake_case` 请求参数。

### 响应结果序列化

响应结果序列化指的是将 Controller 返回的 Java 对象转换为 JSON 响应体。Fastjson1 Converter 生效后，普通对象响应会按照 Fastjson1 的序列化规则输出。

下面的 VO 用于订单响应结果。

文件位置：`src/main/java/io/github/atengk/order/vo/OrderVO.java`

```java
package io.github.atengk.order.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /**
     * 订单ID
     */
    @JSONField(name = "order_id", serializeUsing = ToStringSerializer.class)
    private Long orderId;

    /**
     * 订单编号
     */
    @JSONField(name = "order_no")
    private String orderNo;

    /**
     * 订单金额
     */
    @JSONField(name = "order_amount")
    private BigDecimal orderAmount;

    /**
     * 下单时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 买家名称
     */
    @JSONField(name = "buyer_name")
    private String buyerName;

}
```

序列化后的响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "order_id": "9007199254740993",
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }
}
```

这里的 `order_id` 输出为字符串，是为了避免前端 JavaScript 对超长整数产生精度丢失。金额字段使用 `BigDecimal`，不使用 `Double`。

### 统一响应对象处理

统一响应对象用于固定接口返回结构，避免不同 Controller 返回格式不一致。常见结构包含 `code`、`message`、`data` 三部分。

下面的 `ApiResult` 是一个简洁的统一响应类。

文件位置：`src/main/java/io/github/atengk/common/ApiResult.java`

```java
package io.github.atengk.common;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 状态码
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
     * 构建成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 构建成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, StrUtil.blankToDefault(message, "操作成功"), data);
    }

    /**
     * 构建失败响应
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        Integer resultCode = ObjectUtil.defaultIfNull(code, 500);
        String resultMessage = StrUtil.blankToDefault(message, "操作失败");
        return new ApiResult<>(resultCode, resultMessage, null);
    }

}
```

下面的 Controller 演示请求体反序列化、响应体序列化和统一响应对象使用。

文件位置：`src/main/java/io/github/atengk/order/controller/OrderController.java`

```java
package io.github.atengk.order.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.ApiResult;
import io.github.atengk.order.dto.OrderCreateRequest;
import io.github.atengk.order.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单响应结果
     */
    @PostMapping
    public ApiResult<OrderVO> create(@RequestBody OrderCreateRequest request) {
        if (ObjectUtil.isNull(request)) {
            log.warn("创建订单失败，请求体为空");
            return ApiResult.fail(400, "请求体不能为空");
        }

        if (StrUtil.isBlank(request.getOrderNo())) {
            log.warn("创建订单失败，订单编号为空");
            return ApiResult.fail(400, "订单编号不能为空");
        }

        if (ObjectUtil.isNull(request.getOrderAmount())) {
            log.warn("创建订单失败，订单金额为空，订单编号：{}", request.getOrderNo());
            return ApiResult.fail(400, "订单金额不能为空");
        }

        Long orderId = IdUtil.getSnowflakeNextId();
        OrderVO orderVO = new OrderVO(
                orderId,
                request.getOrderNo(),
                request.getOrderAmount(),
                ObjectUtil.defaultIfNull(request.getCreateTime(), LocalDateTime.now()),
                request.getBuyerName()
        );

        log.info("创建订单成功，订单ID：{}，订单编号：{}", orderId, request.getOrderNo());
        return ApiResult.success("创建订单成功", orderVO);
    }

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单响应结果
     */
    @GetMapping("/{orderId}")
    public ApiResult<OrderVO> detail(@PathVariable Long orderId) {
        if (ObjectUtil.isNull(orderId)) {
            log.warn("查询订单失败，订单ID为空");
            return ApiResult.fail(400, "订单ID不能为空");
        }

        OrderVO orderVO = new OrderVO(
                orderId,
                "SO202605090001",
                new BigDecimal("199.99"),
                LocalDateTime.now(),
                "Ateng"
        );

        log.info("查询订单成功，订单ID：{}", orderId);
        return ApiResult.success(orderVO);
    }

}
```

启动项目后，可以使用以下命令验证请求体反序列化：

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }'
```

可以使用以下命令验证响应体序列化：

```bash
curl -X GET "http://localhost:8080/api/orders/9007199254740993"
```

预期响应结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "order_id": "9007199254740993",
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }
}
```

如果返回字段仍然是 `orderId`、`orderNo`，通常说明 Fastjson1 Converter 没有优先于 Jackson 生效。此时应检查 `extendMessageConverters()` 中 Fastjson1 Converter 的插入位置，以及响应 `Content-Type` 是否为 `application/json`。



## 全局配置示例

本节给出一个相对完整的全局配置示例，用于在 Spring Boot 3 中统一注册 Fastjson1 的 `HttpMessageConverter`。Spring MVC 官方文档说明，`configureMessageConverters()` 会替换默认消息转换器，而 `extendMessageConverters()` 用于在默认转换器列表初始化后进行扩展；在 Spring Boot 应用中，通常更适合使用 `extendMessageConverters()` 或 Boot 的 `HttpMessageConverters` 机制。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/reference/web/webmvc/mvc-config/message-converters.html?utm_source=chatgpt.com))

### FastJsonConfig 配置类

这里的 `FastJsonConfig` 指 Alibaba Fastjson1 提供的配置对象，不建议把自定义配置类也命名为 `FastJsonConfig`，否则容易和 `com.alibaba.fastjson.support.config.FastJsonConfig` 混淆。

下面的配置类完成以下工作：

1. 注册 Fastjson1 的 `FastJsonHttpMessageConverter`。
2. 设置 JSON 响应字符集为 UTF-8。
3. 统一配置日期格式。
4. 配置 Null 值、大数值、循环引用等序列化行为。
5. 配置反序列化安全策略，禁用 AutoType 并开启 SafeMode。
6. 将 Fastjson1 Converter 插入到 Jackson Converter 之前。

文件位置：`src/main/java/io/github/atengk/config/FastjsonMvcConfig.java`

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fastjson1 MVC 全局配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class FastjsonMvcConfig implements WebMvcConfigurer {

    /**
     * 扩展 Spring MVC 消息转换器
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter fastJsonConverter = createFastJsonHttpMessageConverter();

        if (CollUtil.isEmpty(converters)) {
            converters.add(fastJsonConverter);
            log.warn("默认消息转换器列表为空，已直接注册 Fastjson1 转换器");
            return;
        }

        converters.removeIf(FastJsonHttpMessageConverter.class::isInstance);

        int jacksonIndex = findJacksonConverterIndex(converters);
        if (jacksonIndex >= 0) {
            converters.add(jacksonIndex, fastJsonConverter);
            log.info("Fastjson1 转换器已注册到 Jackson 转换器之前，索引：{}", jacksonIndex);
            return;
        }

        converters.add(0, fastJsonConverter);
        log.info("未找到 Jackson 转换器，Fastjson1 转换器已注册到转换器列表首位");
    }

    /**
     * 创建 Fastjson1 HTTP 消息转换器
     *
     * @return Fastjson1 HTTP 消息转换器
     */
    private FastJsonHttpMessageConverter createFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // 只处理 JSON，避免误处理文件、表单、字符串等响应
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));

        // 设置 Fastjson1 全局配置
        converter.setFastJsonConfig(createFastJsonConfig());

        return converter;
    }

    /**
     * 创建 Fastjson1 配置
     *
     * @return Fastjson1 配置
     */
    private FastJsonConfig createFastJsonConfig() {
        FastJsonConfig config = new FastJsonConfig();

        // 设置响应字符集，避免中文乱码
        config.setCharset(StandardCharsets.UTF_8);

        // 设置 Date 类型的默认格式
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        // 设置序列化配置
        config.setSerializeConfig(createSerializeConfig());

        // 设置反序列化配置
        config.setParserConfig(createParserConfig());

        // 设置序列化特性
        config.setSerializerFeatures(
                // 输出 null 字段，保证响应字段结构稳定
                SerializerFeature.WriteMapNullValue,

                // Date 类型按 dateFormat 输出
                SerializerFeature.WriteDateUseDateFormat,

                // BigDecimal 使用普通数字格式，避免科学计数法
                SerializerFeature.WriteBigDecimalAsPlain,

                // 禁用循环引用标识，避免输出 "$ref"
                SerializerFeature.DisableCircularReferenceDetect
        );

        // 设置反序列化特性
        config.setFeatures(
                // 支持 ISO8601 日期格式解析
                Feature.AllowISO8601DateFormat,

                // 忽略 Java 对象中不存在的 JSON 字段
                Feature.IgnoreNotMatch
        );

        return config;
    }

    /**
     * 创建序列化配置
     *
     * @return 序列化配置
     */
    private SerializeConfig createSerializeConfig() {
        SerializeConfig serializeConfig = new SerializeConfig();

        // 全局字段命名策略：orderNo -> order_no
        serializeConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        return serializeConfig;
    }

    /**
     * 创建反序列化配置
     *
     * @return 反序列化配置
     */
    private ParserConfig createParserConfig() {
        ParserConfig parserConfig = ParserConfig.getGlobalInstance();

        // 禁用 AutoType，降低不可信 JSON 反序列化风险
        parserConfig.setAutoTypeSupport(false);

        // 开启 SafeMode，开启后会完全禁用 AutoType
        parserConfig.setSafeMode(true);

        // 全局字段命名策略：order_no -> orderNo
        parserConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;

        return parserConfig;
    }

    /**
     * 查找 Jackson 消息转换器索引
     *
     * @param converters 消息转换器列表
     * @return Jackson 转换器索引，未找到返回 -1
     */
    private int findJacksonConverterIndex(List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                return i;
            }
        }
        return -1;
    }

}
```

如果生产环境需要进一步强化安全配置，可以在 JVM 启动参数中启用 Fastjson1 SafeMode。Fastjson 官方说明，1.2.68 之后支持 SafeMode，开启后会完全禁用 AutoType。([CVE](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-25845&utm_source=chatgpt.com))

```bash
# 通过 JVM 参数开启 Fastjson1 SafeMode
java -Dfastjson.parser.safeMode=true -jar app.jar
```

Docker 或 Kubernetes 环境可以通过环境变量注入：

```bash
# 容器环境统一注入 JVM 参数
export JAVA_TOOL_OPTIONS="-Dfastjson.parser.safeMode=true"
```

### MessageConverter 注册顺序

`HttpMessageConverter` 的注册顺序会直接影响请求和响应由哪个 Converter 处理。Spring Framework Javadoc 明确说明 Converter 注册顺序很重要，特别是在客户端接受 `MediaType.ALL` 时，靠前的 Converter 会被优先使用。([Home](https://docs.spring.io/spring-framework/docs/6.1.8/javadoc-api/org/springframework/web/servlet/config/annotation/WebMvcConfigurer.html?utm_source=chatgpt.com))

推荐的关键顺序如下：

```text
ByteArrayHttpMessageConverter
StringHttpMessageConverter
ResourceHttpMessageConverter
...
FastJsonHttpMessageConverter
MappingJackson2HttpMessageConverter
...
```

这里不建议把 Fastjson1 Converter 放到 `StringHttpMessageConverter` 之前。因为 Controller 如果直接返回 `String`，Spring MVC 通常会交给 `StringHttpMessageConverter` 处理；如果 Fastjson1 Converter 排在过前位置，可能会改变字符串响应行为。

可以在配置类中增加启动日志，输出当前 Converter 顺序，便于排查是否生效。

文件位置：`src/main/java/io/github/atengk/config/MessageConverterLogConfig.java`

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

/**
 * 消息转换器顺序日志配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class MessageConverterLogConfig implements CommandLineRunner {

    private final RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    public MessageConverterLogConfig(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        this.requestMappingHandlerAdapter = requestMappingHandlerAdapter;
    }

    /**
     * 应用启动后输出消息转换器顺序
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        List<HttpMessageConverter<?>> converters = requestMappingHandlerAdapter.getMessageConverters();

        log.info("当前 Spring MVC 消息转换器数量：{}", converters.size());
        for (int i = 0; i < converters.size(); i++) {
            log.info("消息转换器顺序：{}，类型：{}", i, converters.get(i).getClass().getName());
        }
    }

}
```

启动后需要重点观察日志中 `FastJsonHttpMessageConverter` 与 `MappingJackson2HttpMessageConverter` 的相对位置。如果 Fastjson1 在 Jackson 之后，普通 `application/json` 响应仍可能被 Jackson 处理。

### 与默认 Jackson 的关系

Spring Boot 3 默认 JSON 体系以 Jackson 为主。即使项目引入 Fastjson1，Jackson 依然可能在以下场景中继续生效：

| 场景                                  | 是否可能仍由 Jackson 处理 | 说明                                                      |
| ------------------------------------- | ------------------------- | --------------------------------------------------------- |
| Fastjson1 Converter 未注册            | 是                        | 默认由 Jackson JSON Converter 处理。                      |
| Fastjson1 Converter 排在 Jackson 后面 | 是                        | Jackson 可能优先匹配 `application/json`。                 |
| 返回类型是 `String`                   | 不一定                    | 通常由 `StringHttpMessageConverter` 处理。                |
| 异常响应                              | 可能                      | 与异常处理器、返回类型、Converter 顺序有关。              |
| 测试中直接使用 `ObjectMapper`         | 是                        | `ObjectMapper` 是 Jackson 对象，不受 Fastjson1 配置影响。 |
| Redis、MQ、缓存序列化器               | 取决于配置                | 不会因为 MVC Converter 改变而自动使用 Fastjson1。         |

因此，Fastjson1 与 Jackson 的关系可以理解为：

1. Jackson 是 Spring Boot 3 的默认 JSON 基础设施。
2. Fastjson1 通过 `HttpMessageConverter` 可以接管 Spring MVC 的部分 JSON 请求和响应。
3. `spring.jackson.*` 配置不会影响 Fastjson1。
4. `FastJsonConfig` 配置不会影响 Jackson。
5. 如果系统中同时存在两套 JSON 配置，需要通过测试保证接口输出一致。

建议在项目中明确约定：

```text
Controller 请求体和响应体：Fastjson1
内部对象转换或第三方组件默认 JSON：Jackson
局部 JSON 字符串处理：按业务代码显式选择 JSON 工具
```

如果不需要全局接管 Controller JSON 响应，不建议注册 Fastjson1 Converter。可以只在业务代码中使用：

```java
String json = JSON.toJSONString(data);
OrderVO orderVO = JSON.parseObject(json, OrderVO.class);
```

## 常见问题

本节整理 Spring Boot 3 集成 Fastjson1 时常见的问题和处理方式，主要覆盖时间类型、中文乱码、Converter 不生效以及 Fastjson1 安全风险。

### LocalDateTime 序列化问题

`FastJsonConfig#setDateFormat` 主要用于 `java.util.Date` 类型。对于 `LocalDateTime`、`LocalDate`、`LocalTime` 等 Java 8 时间类型，建议优先使用 `@JSONField(format = "...")` 标注字段格式。

示例：

```java
@JSONField(format = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

完整 VO 示例：

```java
package io.github.atengk.order.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单时间响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderTimeVO {

    /**
     * 订单编号
     */
    @JSONField(name = "order_no")
    private String orderNo;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

如果接口返回的时间格式不符合预期，按以下顺序排查：

1. 字段上是否添加了 `@JSONField(format = "...")`。
2. 当前响应是否真的由 Fastjson1 Converter 处理。
3. 是否仍由 Jackson 输出。
4. 是否存在统一响应包装器重新转换 JSON。
5. 是否在测试中使用 Jackson `ObjectMapper` 判断结果。

### 中文乱码问题

中文乱码通常不是 Fastjson1 对象序列化本身的问题，而是响应字符集、HTTP Header、字符串响应或网关代理配置不一致导致的问题。

Fastjson1 Converter 中建议明确设置 UTF-8：

```java
FastJsonConfig config = new FastJsonConfig();
config.setCharset(StandardCharsets.UTF_8);
```

同时建议在 `application.yml` 中配置 Spring Web 编码：

```yaml
server:
  servlet:
    encoding:
      # 统一请求和响应字符集
      charset: UTF-8

      # 强制使用指定字符集
      force: true

      # 开启编码配置
      enabled: true
```

如果 Controller 直接返回 `String`，通常不会由 Fastjson1 Converter 处理，而是由 `StringHttpMessageConverter` 处理。对于 JSON 接口，不建议直接返回 JSON 字符串：

```java
// 不推荐：容易绕过 Fastjson1 Converter，也容易产生 Content-Type 不一致问题
return JSON.toJSONString(orderVO);
```

推荐返回 Java 对象：

```java
// 推荐：交给 HttpMessageConverter 统一处理
return ApiResult.success(orderVO);
```

如果仍然出现乱码，需要检查响应头：

```bash
curl -i "http://localhost:8080/api/orders/9007199254740993"
```

重点观察：

```text
Content-Type: application/json
```

如果响应头不是 JSON，或存在错误的字符集配置，需要继续检查 Controller 返回类型、网关、Nginx、全局过滤器和自定义响应包装逻辑。

### Converter 不生效问题

Converter 不生效通常表现为：字段没有按 `@JSONField` 输出、`Long` 没有转字符串、`LocalDateTime` 格式不对、Null 值策略不生效，或者响应仍然是 Jackson 的字段命名风格。

常见原因如下：

| 问题                       | 现象                    | 处理方式                                  |
| -------------------------- | ----------------------- | ----------------------------------------- |
| Fastjson1 Converter 未注册 | 完全无 Fastjson1 效果   | 检查配置类是否被 Spring 扫描。            |
| Converter 顺序错误         | Jackson 仍然处理 JSON   | 将 Fastjson1 插入到 Jackson 之前。        |
| 返回 `String`              | Fastjson1 不处理响应    | 返回 Java 对象，不返回 JSON 字符串。      |
| `MediaType` 不匹配         | Converter 无法匹配      | 设置 `application/json`。                 |
| 配置类包路径不对           | 配置未加载              | 放到启动类同级或子包。                    |
| 测试方式错误               | 测试结果和接口不一致    | 使用 MockMvc 或 curl 验证真实 HTTP 响应。 |
| 使用 `@EnableWebMvc`       | 默认 MVC 自动配置受影响 | 普通 Boot 项目不建议手动添加。            |

推荐增加一个临时接口，用于判断当前 JSON 响应是否由 Fastjson1 处理。

文件位置：`src/main/java/io/github/atengk/debug/controller/JsonDebugController.java`

```java
package io.github.atengk.debug.controller;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * JSON 调试接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
public class JsonDebugController {

    /**
     * 测试 Fastjson1 是否生效
     *
     * @return JSON 调试结果
     */
    @GetMapping("/debug/json")
    public JsonDebugVO json() {
        return new JsonDebugVO(
                9007199254740993L,
                "测试订单",
                LocalDateTime.of(2026, 5, 9, 10, 30, 0)
        );
    }

    /**
     * JSON 调试响应对象
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    @AllArgsConstructor
    public static class JsonDebugVO {

        /**
         * 订单ID
         */
        @JSONField(name = "order_id", serializeUsing = ToStringSerializer.class)
        private Long orderId;

        /**
         * 订单名称
         */
        @JSONField(name = "order_name")
        private String orderName;

        /**
         * 创建时间
         */
        @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

    }

}
```

验证命令：

```bash
curl -s "http://localhost:8080/debug/json"
```

预期结果：

```json
{
  "order_id": "9007199254740993",
  "order_name": "测试订单",
  "create_time": "2026-05-09 10:30:00"
}
```

如果实际返回为 `orderId`、`orderName`、`createTime`，说明 Fastjson1 Converter 没有生效，或者 Jackson 仍然优先处理响应。

### Fastjson1 安全风险说明

Fastjson1 的核心安全风险主要集中在反序列化和 AutoType 机制。CVE-2022-25845 记录了 Fastjson 1.2.83 之前版本存在不可信数据反序列化问题，攻击者在特定条件下可绕过 AutoType 关闭限制并造成远程攻击风险；修复版本为 1.2.83，无法升级时建议启用 SafeMode。([GitLab Advisory Database](https://advisories.gitlab.com/maven/com.alibaba/fastjson/CVE-2022-25845/?utm_source=chatgpt.com))

生产环境建议遵循以下规则：

1. Fastjson1 版本固定使用 `1.2.83`。
2. 不启用 AutoType。
3. 开启 SafeMode。
4. 不反序列化不可信来源的多态对象。
5. 不使用 `JSON.parseObject(json)` 解析未指定目标类型的外部输入。
6. 不在全局序列化中开启 `WriteClassName`。
7. 对外部请求 DTO 使用明确字段类型，不使用 `Object`、`Map<String, Object>` 承接复杂未知结构。
8. 新项目优先评估 Jackson 或 Fastjson2，不建议默认选用 Fastjson1。

不推荐的写法：

```java
// 不推荐：未指定目标类型，且输入来源不可信时风险较高
Object value = JSON.parseObject(json);
```

推荐的写法：

```java
// 推荐：指定明确 DTO 类型
OrderCreateRequest request = JSON.parseObject(json, OrderCreateRequest.class);
```

如果项目必须兼容历史 Fastjson1 行为，建议将 Fastjson1 的使用范围限制在接口适配层、历史协议兼容层或局部工具类中，不建议让它无边界接管所有 JSON 处理场景。

## 功能验证

本节通过 curl 和 MockMvc 两种方式验证 Fastjson1 是否生效。curl 用于验证真实 HTTP 响应，MockMvc 用于在自动化测试中验证请求体反序列化和响应体序列化行为。

### 接口测试用例

如果项目还没有测试依赖，需要在 `pom.xml` 中加入 `spring-boot-starter-test`。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

下面的测试类用于验证订单接口。测试重点不是业务逻辑，而是确认请求体能被正确反序列化、响应字段能按 Fastjson1 注解和配置输出。

文件位置：`src/test/java/io/github/atengk/order/controller/OrderControllerTest.java`

```java
package io.github.atengk.order.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 订单接口测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试创建订单接口
     *
     * @throws Exception 请求异常
     */
    @Test
    void testCreateOrder() throws Exception {
        String requestBody = """
                {
                  "order_no": "SO202605090001",
                  "order_amount": 199.99,
                  "create_time": "2026-05-09 10:30:00",
                  "buyer_name": "Ateng"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("创建订单响应：{}", responseBody);

        Assertions.assertTrue(StrUtil.isNotBlank(responseBody), "响应内容不能为空");

        JSONObject responseJson = JSON.parseObject(responseBody);
        Assertions.assertEquals(200, responseJson.getInteger("code"));
        Assertions.assertEquals("创建订单成功", responseJson.getString("message"));

        JSONObject data = responseJson.getJSONObject("data");
        Assertions.assertNotNull(data, "响应 data 不能为空");
        Assertions.assertEquals("SO202605090001", data.getString("order_no"));
        Assertions.assertEquals("199.99", data.getBigDecimal("order_amount").toPlainString());
        Assertions.assertEquals("2026-05-09 10:30:00", data.getString("create_time"));
        Assertions.assertEquals("Ateng", data.getString("buyer_name"));

        // Long ID 应输出为字符串，避免前端精度丢失
        Assertions.assertTrue(data.get("order_id") instanceof String, "order_id 应输出为字符串");
    }

    /**
     * 测试查询订单详情接口
     *
     * @throws Exception 请求异常
     */
    @Test
    void testGetOrderDetail() throws Exception {
        String responseBody = mockMvc.perform(get("/api/orders/{orderId}", 9007199254740993L)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("查询订单响应：{}", responseBody);

        JSONObject responseJson = JSON.parseObject(responseBody);
        Assertions.assertEquals(200, responseJson.getInteger("code"));

        JSONObject data = responseJson.getJSONObject("data");
        Assertions.assertNotNull(data, "响应 data 不能为空");
        Assertions.assertEquals("9007199254740993", data.getString("order_id"));
        Assertions.assertTrue(data.containsKey("order_no"), "响应中应包含 order_no 字段");
        Assertions.assertTrue(data.containsKey("create_time"), "响应中应包含 create_time 字段");
    }

}
```

执行测试：

```bash
mvn test -Dtest=OrderControllerTest
```

如果测试失败，并且响应字段为 `orderId`、`orderNo`、`createTime`，通常说明 Fastjson1 没有接管响应序列化。

### 请求体反序列化验证

请求体反序列化验证重点是确认前端提交的 JSON 字段能被正确映射到 Java DTO。尤其是使用 `snake_case` 字段名时，需要确认 `order_no`、`order_amount`、`create_time` 等字段是否能被正确解析。

使用 curl 发送 POST 请求：

```bash
curl -i -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "创建订单成功",
  "data": {
    "order_id": "9007199254740993",
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }
}
```

如果请求失败，需要重点检查以下内容：

| 检查项         | 说明                                                  |
| -------------- | ----------------------------------------------------- |
| `Content-Type` | 必须是 `application/json` 或兼容 JSON 的媒体类型。    |
| DTO 字段注解   | 检查 `@JSONField(name = "...")` 是否正确。            |
| 时间格式       | 检查请求时间是否符合 `yyyy-MM-dd HH:mm:ss`。          |
| Converter 顺序 | 确认 Fastjson1 是否排在 Jackson 之前。                |
| SafeMode       | SafeMode 不影响普通 DTO 反序列化，但会禁用 AutoType。 |

如果请求中的字段名是 `orderNo`，而 DTO 只显式配置了 `@JSONField(name = "order_no")`，需要根据实际接口规范统一字段风格。建议接口层统一使用一种命名方式，避免同一个字段同时支持多种 JSON 名称。

### 响应体序列化验证

响应体序列化验证重点是确认 Controller 返回对象后，最终 HTTP 响应是否符合 Fastjson1 配置和接口规范。

使用 curl 验证 GET 接口：

```bash
curl -i -X GET "http://localhost:8080/api/orders/9007199254740993" \
  -H "Accept: application/json"
```

重点检查响应头：

```text
HTTP/1.1 200
Content-Type: application/json
```

重点检查响应体字段：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "order_id": "9007199254740993",
    "order_no": "SO202605090001",
    "order_amount": 199.99,
    "create_time": "2026-05-09 10:30:00",
    "buyer_name": "Ateng"
  }
}
```

可以从以下几个点判断 Fastjson1 是否生效：

| 验证点            | 预期结果                                                  |
| ----------------- | --------------------------------------------------------- |
| `order_id`        | 字段名为下划线格式。                                      |
| `order_id` 值类型 | 超长 Long 输出为字符串。                                  |
| `create_time`     | 格式为 `yyyy-MM-dd HH:mm:ss`。                            |
| Null 字段         | 如果开启 `WriteMapNullValue`，Null 字段应保留。           |
| BigDecimal        | 不应输出科学计数法。                                      |
| `$ref`            | 开启 `DisableCircularReferenceDetect` 后不应输出 `$ref`。 |

如果响应体不符合预期，建议按以下顺序排查：

```text
1. 确认配置类是否被 Spring Boot 扫描。
2. 查看启动日志中的 Converter 顺序。
3. 确认 FastJsonHttpMessageConverter 是否在 MappingJackson2HttpMessageConverter 之前。
4. 确认 Controller 返回的是 Java 对象，而不是 String。
5. 确认响应 Content-Type 是 application/json。
6. 确认字段上是否使用了正确的 @JSONField 注解。
7. 使用 curl 验证真实 HTTP 响应，不只看单元测试中的对象转换结果。
```

完成以上验证后，可以基本确认 Spring Boot 3 中 Fastjson1 的全局接入、请求体反序列化和响应体序列化是否符合预期。
