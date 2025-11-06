# 属性配置和Bean的使用



## 属性配置

### 环境配置

#### spring.profiles.active

一般在配置文件 `application.yml` 或者 启动命令中 `--spring.profiles.active=dev` 设置项目环境

```yaml
spring:
  profiles:
    active: dev
```

然后就生效对应的配置文件 ： `application-dev.yml`

如果同时设置多个环境，可以这样，两种写法都可以

```yaml
spring:
  profiles:
    active: dev, test
--- 
spring:
  profiles:
    active:
      - dev
      - test
```

#### spring.config.activate.on-profile

**`---`** 代表的是 **多文档分隔符**，每一段其实就是一个独立的配置文件片段。

`on-profile` 匹配到当前环境，就生效这一块文档的配置

```yaml
---
# dev 环境生效该文档
spring:
  config:
    activate:
      on-profile: dev
data: "dev 环境"
---
# dev 环境生效该文档
spring:
  config:
    activate:
      on-profile: test
data: "test 环境"
---
```

#### 环境变量

如果环境变量存在，则取环境变量，否则取默认值
value: ${ENV_VAR:defaultValue}

```
server:
  port: ${SERVER_PORT:8080}
```

如果设置了 `SERVER_PORT` 环境变量，就用它的值；

否则默认用 `8080`。



### 将配置文件加载到属性类中

`@ConfigurationProperties` 是 Spring Boot 提供的一个注解，用于将外部配置文件中的属性值绑定到 Java 对象上。这使得在应用程序中访问配置信息变得更加简洁和类型安全。通常，配置文件可以是 `application.properties` 或 `application.yml`，通过 `@ConfigurationProperties` 注解，Spring Boot 会自动将这些配置映射到一个 POJO（Plain Old Java Object）中。

1. 在配置文件 `application.yml` 中添加以下自定义配置

```yaml
---
# 自定义配置文件
app:
  name: ateng
  port: 12345
  ids:
    - 1
    - 2
    - 3
  ateng:
    name: kongyu
    age: 24
```


2. 创建属性类加载配置

```java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Configuration
@Data
public class AppProperties {
    private String name;
    private int port;
    private List<Integer> ids;
    private Ateng ateng;

    @Data
    public static class Ateng{
        private String name;
        private int age;
    }
}
```

3. 使用属性类

注入属性类后直接使用

```java
import local.ateng.java.config.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyApplicationRunner implements ApplicationRunner {
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("配置文件：{}", appProperties);
    }
}
```

### 配置属性校验

**添加依赖**

```xml
        <!-- Spring Boot Validation 数据校验框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

**配置类使用示例**

```java
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import javax.validation.constraints.*;

@Component  // 注册为Spring组件
@ConfigurationProperties(prefix = "mail")  // 绑定前缀为mail的配置
@Validated  // 启用属性校验
public class MailProperties {

    @NotEmpty  // 不能为空
    private String host;  // 邮件服务器地址

    @Min(1025)  // 最小值1025
    @Max(65536)  // 最大值65536
    private int port = 25;  // 邮件服务器端口，默认25

    @Email  // 必须符合邮箱格式
    private String from;  // 发件人邮箱

    private boolean enabled;  // 是否启用邮件功能
}

import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import javax.validation.constraints.*;

@ConfigurationProperties(prefix = "app.connection")
@Validated  // 启用校验
public class ConnectionProperties {

    @NotNull  // 不能为null
    @Min(1000)  // 最小值1000
    @Max(10000)  // 最大值10000
    private Integer timeout;  // 超时时间

    // 必须以http或https开头的URL格式
    @Pattern(regexp = "^(http|https)://.*$")
    private String serviceUrl;  // 服务地址

    @Email  // 必须是合法邮箱格式
    private String supportEmail;  // 支持邮箱
}
```

### 配置元数据

创建配置元数据，提供IDE自动完成和文档。

引入配置处理器依赖（Maven）：

```xml
        <!-- Spring Boot Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
```

配置属性类

```java
package local.ateng.java.config.config;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 通用配置类，涵盖常见类型，用于演示 Spring Boot 3 元数据提示
 * <p>
 * prefix: common
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "common")
public class CommonProperties {

    /**
     * 应用名称（必填）
     */
    @NotBlank
    private String name;

    /**
     * 是否启用模块
     */
    private boolean enabled = true;

    /**
     * 服务端口号
     */
    private int port = 8080;

    /**
     * 允许的ID列表
     */
    private List<Integer> ids;

    /**
     * 元信息键值对
     */
    private Map<String, String> metadata;

    /**
     * 运行环境类型
     */
    private EnvType env = EnvType.DEV;

    /**
     * 请求超时时间（如 10s、1m）
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 已弃用字段（示例）
     */
    private String host;

    /**
     * 枚举类型：环境类型
     */
    public enum EnvType {
        DEV, TEST, PROD
    }
}
```

创建 `META-INF/additional-spring-configuration-metadata.json`文件，提供额外元数据：

```json
{
  "groups": [
    {
      "name": "common",
      "type": "local.ateng.java.config.config.CommonProperties",
      "sourceType": "local.ateng.java.config.config.CommonProperties",
      "description": "通用应用配置（CommonProperties）"
    }
  ],
  "properties": [
    {
      "name": "common.name",
      "type": "java.lang.String",
      "description": "应用名称（必填项）",
      "sourceType": "local.ateng.java.config.config.CommonProperties",
      "defaultValue": "MyApp"
    },
    {
      "name": "common.enabled",
      "type": "java.lang.Boolean",
      "description": "是否启用该功能模块",
      "defaultValue": true,
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.port",
      "type": "java.lang.Integer",
      "description": "服务监听端口号",
      "defaultValue": 8080,
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.max-size",
      "type": "java.lang.Long",
      "description": "最大处理数据大小（单位：MB）",
      "defaultValue": 1024,
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.threshold",
      "type": "java.lang.Double",
      "description": "阈值比例（0~1）",
      "defaultValue": 0.75,
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.tags",
      "type": "java.util.List<java.lang.String>",
      "description": "标签列表",
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.metadata",
      "type": "java.util.Map<java.lang.String,java.lang.String>",
      "description": "键值对元信息",
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.env",
      "type": "local.ateng.java.config.config.CommonProperties$EnvType",
      "description": "运行环境类型",
      "defaultValue": "DEV",
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.timeout",
      "type": "java.time.Duration",
      "description": "请求超时时间（支持 10s、1m 等格式）",
      "defaultValue": "30s",
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.release-date",
      "type": "java.time.LocalDate",
      "description": "发布日期（格式：yyyy-MM-dd）",
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    },
    {
      "name": "common.host",
      "type": "java.lang.String",
      "description": "（已弃用）旧版主机配置，请使用 common.name 替代",
      "deprecation": {
        "level": "warning",
        "reason": "已迁移至 common.name",
        "replacement": "common.name"
      },
      "sourceType": "local.ateng.java.config.config.CommonProperties"
    }
  ],
  "hints": [
    {
      "name": "common.enabled",
      "values": [
        { "value": true, "description": "启用" },
        { "value": false, "description": "禁用" }
      ]
    },
    {
      "name": "common.port",
      "values": [
        { "value": 8080, "description": "默认 HTTP 端口" },
        { "value": 8443, "description": "默认 HTTPS 端口" }
      ]
    },
    {
      "name": "common.tags",
      "values": [
        { "value": "alpha", "description": "Alpha 标签" },
        { "value": "beta", "description": "Beta 标签" },
        { "value": "release", "description": "正式发布标签" }
      ]
    },
    {
      "name": "common.env",
      "values": [
        { "value": "DEV", "description": "开发环境" },
        { "value": "TEST", "description": "测试环境" },
        { "value": "PROD", "description": "生产环境" }
      ]
    }
  ]
}

```



### 使用@Value

`@Value` 是 Spring 提供的一个用于 **属性注入** 的注解，主要用于从 **配置文件**（`application.properties` 或 `application.yml`）、**环境变量** 或 **SpEL 表达式** 中获取值。

---

**1. `@Value` 的基本用法**

`@Value` 通常用于 **从配置文件加载值**，并提供默认值：

**🔹（1）从 `application.properties` 或 `application.yml` 读取**

✅ **`application.properties`**

```properties
app.name=SpringBootApp
app.port=8080
```
✅ **Java 代码**

```java
@Component
public class AppConfig {
    @Value("${app.name}")  // 读取配置文件中的 app.name
    private String appName;

    @Value("${app.port}")
    private int appPort;

    public void printConfig() {
        System.out.println("App Name: " + appName);
        System.out.println("App Port: " + appPort);
    }
}
```
> **说明**：
> - `@Value("${app.name}")` 读取 `application.properties` 里的 `app.name` 并注入 `appName` 变量。
> - `@Value("${app.port}")` 读取 `app.port` 并注入 `appPort` 变量。

---

**🔹（2）使用默认值**

如果 **配置文件中没有该属性**，可以使用 `:` 提供 **默认值**：
```java
@Value("${app.version:1.0.0}") // 读取 app.version，如果不存在，则使用默认值 "1.0.0"
private String appVersion;
```
> **说明**：
> - `app.version` 不存在时，`appVersion` 变量会被赋值 `"1.0.0"`。

---

**2. `@Value` 的高级用法**

**🔹（1）SpEL 表达式（Spring Expression Language）**

`@Value` 支持 **SpEL 表达式**，可以动态计算值。

✅ **使用 `SpEL` 进行计算**

```java
@Value("#{1 + 2}") // 计算 1+2 的结果
private int sum;  // 结果为 3

@Value("#{T(Math).PI}") // 读取 Java Math 类的 PI 值
private double pi; // 结果 3.141592653589793
```

✅ **读取 Bean 的属性**

```java
@Component
public class User {
    private String username = "admin";

    public String getUsername() {
        return username;
    }
}

@Component
public class SpELExample {
    @Autowired
    private User user;

    @Value("#{user.username}") // 读取 user Bean 的 username
    private String username;
}
```
> **说明**：
> - `@Value("#{user.username}")` 直接读取 `User` Bean 的 `username` 值。

---

**🔹（2）从环境变量或系统属性读取**

✅ **获取环境变量**

```java
@Value("${JAVA_HOME}") // 读取系统环境变量 JAVA_HOME
private String javaHome;
```
> **说明**：
> - 这里直接获取了系统环境变量 `JAVA_HOME` 的值。

✅ **获取系统属性**

```java
@Value("#{systemProperties['user.dir']}") // 获取系统属性 user.dir（当前项目路径）
private String userDir;
```
> **说明**：
> - `systemProperties['user.dir']` 读取 **系统属性**，比如当前的用户目录。

---

**🔹（3）注入 List、Map、数组**

✅ **读取数组**

`application.properties`:
```properties
app.servers=server1,server2,server3
```
Java 代码：
```java
@Value("${app.servers}")
private String[] servers;
```
---

✅ **读取 List**

```java
@Value("#{'${app.servers}'.split(',')}")
private List<String> serverList;
```
> **说明**：
> - `split(',')` 把 `app.servers` 转换成 `List<String>`。

---

✅ **读取 Map**

`application.yml`:
```yaml
app:
  config:
    key1: value1
    key2: value2
```
Java 代码：
```java
@Value("#{${app.config}}")
private Map<String, String> configMap;
```

**🔹（4）从 classpath 获取文件**

Spring 的 `@Value` 支持 **资源表达式**（Resource Expression）
 即以 `classpath:`、`file:`、`url:` 开头的路径。

示例：

```java
package io.github.atengk.demo.value;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathFileExample {

    @Value("classpath:data/example.txt")
    private Resource resourceFile;

    public void printContent() throws Exception {
        System.out.println("文件路径: " + resourceFile.getFilename());
        System.out.println("文件内容: " + new String(resourceFile.getInputStream().readAllBytes()));
    }
}
```

**说明：**

- `@Value("classpath:...")` 会自动注入一个 `org.springframework.core.io.Resource` 对象；
- 通过 `resourceFile.getInputStream()` 可读取文件内容；
- 文件放在 `src/main/resources/data/example.txt` 即可。

🧾 **注入外部文件（file:）**

如果你想读取绝对路径文件：

```java
@Value("file:/opt/config/custom.conf")
private Resource externalFile;
```

同样通过 `externalFile.getInputStream()` 读取。

------

🌐 **读取 URL 资源**

Spring 的 `Resource` 抽象支持网络资源：

```java
@Value("https://example.com/data.txt")
private Resource remoteFile;
```

------

⚙️ **读取并转为字符串或属性值**

如果文件内容是文本或配置文件，你还可以直接注入为字符串：

```java
@Value("classpath:data/message.txt")
private String message;
```

> ⚠️ 注意：这种方式只在文件内容较短且能正确转换为字符串时有效，否则会报错。
>  更推荐注入 `Resource` 再读取流。



## Bean的使用

### 🧩 一、@Profile —— 按环境激活 Bean

**作用**：
 `@Profile` 用于指定 Bean 仅在某个或某些 Profile 环境下加载。

**使用示例：**

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ProfileConfig {

    @Bean
    @Profile("dev")
    public String devBean() {
        return "开发环境Bean";
    }

    @Bean
    @Profile("prod")
    public String prodBean() {
        return "生产环境Bean";
    }
}
```

**配置文件：**

```yaml
spring:
  profiles:
    active: dev
```

**说明：**

- 当 `spring.profiles.active=dev` 时，容器只加载 `devBean()`。
- 切换为 `prod` 时，自动加载 `prodBean()`。

------

### 🧠 二、@ConditionalOnXxx —— 条件装配示例

**作用：**
 Spring Boot 的条件装配注解族，用于根据各种条件动态控制 Bean 的创建。

以下给出常用示例：

1️⃣ @ConditionalOnProperty

```java
package io.github.atengk.demo.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertyConditionConfig {

    @Bean
    @ConditionalOnProperty(prefix = "feature", name = "enabled", havingValue = "true")
    public String propertyConditionBean() {
        return "属性开启时加载的Bean";
    }
}
```

**配置文件：**

```yaml
feature:
  enabled: true
```

------

2️⃣ @ConditionalOnClass

```java
package io.github.atengk.demo.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
public class ClassConditionConfig {

    @Bean
    public String classConditionBean() {
        return "当类路径存在 RestTemplate 时生效";
    }
}
```

------

3️⃣ @ConditionalOnMissingBean

```java
package io.github.atengk.demo.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MissingBeanConditionConfig {

    @Bean
    @ConditionalOnMissingBean(name = "customService")
    public String defaultService() {
        return "默认服务Bean";
    }
}
```

------

4️⃣ @ConditionalOnResource

```java
package io.github.atengk.demo.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceConditionConfig {

    @Bean
    @ConditionalOnResource(resources = "classpath:application.yml")
    public String resourceConditionBean() {
        return "当 classpath 下存在 application.yml 时加载";
    }
}
```

------

### ⚙️ 三、@Primary —— 指定首选 Bean

**作用：**
 当容器中存在多个相同类型的 Bean 时，优先使用带 `@Primary` 的那个。

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PrimaryConfig {

    @Bean
    @Primary
    public String mainService() {
        return "主服务实现";
    }

    @Bean
    public String backupService() {
        return "备用服务实现";
    }
}
```

**说明：**

- 如果 `@Autowired String service;`，会注入 `mainService()`。

------

### 🧭 四、@Qualifier —— 指定注入哪个 Bean

**作用：**
 当存在多个同类型 Bean 时，通过 `@Qualifier` 明确注入目标。

```java
package io.github.atengk.demo.condition;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
public class QualifierConfig {

    @Bean("mysqlRepo")
    public String mysqlRepository() {
        return "MySQL 数据源";
    }

    @Bean("oracleRepo")
    public String oracleRepository() {
        return "Oracle 数据源";
    }
}

@Service
class DataService {
    private final String repository;

    public DataService(@Qualifier("mysqlRepo") String repository) {
        this.repository = repository;
    }
}
```

------

### 🧱 五、@DependsOn —— 控制初始化顺序

**作用：**
 声明该 Bean 依赖于某个其他 Bean，先初始化被依赖的 Bean。

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DependsOnConfig {

    @Bean
    public String baseBean() {
        System.out.println("初始化 baseBean");
        return "Base";
    }

    @Bean
    @DependsOn("baseBean")
    public String dependentBean() {
        System.out.println("初始化 dependentBean（依赖 baseBean）");
        return "Dependent";
    }
}
```

**控制台输出顺序：**

```
初始化 baseBean
初始化 dependentBean（依赖 baseBean）
```

------

### 💤 六、@Lazy —— 延迟初始化 Bean

**作用：**
 默认情况下，Spring 在容器启动时创建所有单例 Bean。
 加上 `@Lazy` 可延迟创建，直到首次使用时再初始化。

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class LazyConfig {

    @Bean
    @Lazy
    public String lazyBean() {
        System.out.println("只有在首次调用时才会初始化");
        return "Lazy Bean";
    }
}
```

------

### 🔁 七、@Scope —— 控制 Bean 的作用域

**作用：**
 控制 Bean 的生命周期范围，如单例、多例、请求级、会话级等。

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ScopeConfig {

    @Bean
    @Scope("prototype")
    public String prototypeBean() {
        System.out.println("每次获取都会创建新实例");
        return "Prototype Bean";
    }
}
```

**常见取值：**

- `singleton`：单例（默认）
- `prototype`：多例
- `request`：每个 HTTP 请求创建一个实例（Web 环境）
- `session`：每个会话创建一个实例（Web 环境）

------

### 📦 八、@Import —— 导入其他配置类或 Bean

**作用：**
 将其他配置类、组件、选择器或注册器导入当前容器中。

**示例：**

```java
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalConfig {

    @Bean
    public String externalBean() {
        return "来自外部配置类的 Bean";
    }
}
package io.github.atengk.demo.condition;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ExternalConfig.class)
public class ImportConfig {
}
```

**说明：**

- `ImportConfig` 自动加载 `ExternalConfig` 中定义的所有 Bean。
- 类似 XML 中的 `<import resource="..."/>`。

------



## Bean 注入方式

### 🧩 一、构造器注入（推荐✅）

✅ 特点

- Spring 官方**强烈推荐**使用构造器注入；
- 最安全（字段可声明为 `final`）；
- 易于单元测试；
- 不依赖反射注入（性能较好）。

📘 示例

```java
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Spring 自动识别构造函数并注入依赖
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void printUser() {
        System.out.println("User count: " + userRepository.count());
    }
}
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    public int count() {
        return 10;
    }
}
```

✅ 特点说明

- 推荐方式，尤其在 Spring Boot 3 中；
- 如果类只有一个构造函数，可以省略 `@Autowired`；
- 可与 `@RequiredArgsConstructor`（Lombok）配合使用。

------

### 🧩 二、字段注入（不推荐❌，但仍可用）

📘 示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public void printOrder() {
        System.out.println("Orders: " + orderRepository.count());
    }
}
```

⚠️ 缺点

- 不能声明 `final`；
- 不利于单元测试；
- 不利于依赖管理，违反依赖倒置原则。

------

### 🧩 三、Setter 方法注入（适合可选依赖）

📘 示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private PaymentRepository paymentRepository;

    @Autowired
    public void setPaymentRepository(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void pay() {
        System.out.println("Pay with repo: " + paymentRepository);
    }
}
```

✅ 优点

- 支持可选依赖（使用 `@Autowired(required = false)`）；
- 支持在运行时重新注入（例如 AOP 代理替换）。

------

### 🧩 四、`@Resource` 注入（基于 JSR-250）

📘 示例

```java
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    // 根据 Bean 名称匹配，优先按 name，其次按 type
    @Resource(name = "mailRepository")
    private MailRepository mailRepository;

    public void send() {
        System.out.println("Send mail using: " + mailRepository);
    }
}
```

✅ 特点

- 来源于 **JDK标准（JSR-250）**；
- 按 **名称优先**，其次按类型；
- 常用于与老项目或 JavaEE 兼容的情况；
- Spring Boot 3 中依然完全支持。

------

### 🧩 五、`@Inject` 注入（基于 JSR-330）

📘 示例

```java
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Inject
    private ProductRepository productRepository;

    public void show() {
        System.out.println("Products: " + productRepository);
    }
}
```

✅ 特点

- 与 `@Autowired` 功能几乎一致；
- 按类型注入；
- 无法使用 `required=false`；
- 更多用于与 CDI (Contexts and Dependency Injection) 框架兼容。

------

### 🧩 六、手动注入（通过 `ApplicationContext`）

在某些需要**动态获取 Bean** 的场景下（例如工厂模式、策略模式），可以手动注入。

📘 示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanLocator {

    private final ApplicationContext context;

    @Autowired
    public BeanLocator(ApplicationContext context) {
        this.context = context;
    }

    public <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
}
```

------

### 🧩 七、配置类中定义 Bean（`@Configuration` + `@Bean`）

除了自动扫描，还可以**手动注册 Bean**。

📘 示例

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public MessageService messageService() {
        return new MessageService();
    }
}
public class MessageService {
    public void sayHello() {
        System.out.println("Hello, Spring Boot 3!");
    }
}
```

------

## 🧭 总结对比表

| 注入方式             | 注入时机 | 优点                   | 缺点                  | 推荐度 |
| -------------------- | -------- | ---------------------- | --------------------- | ------ |
| 构造器注入           | 初始化时 | 安全、可测试、推荐     | 无可选依赖            | ⭐⭐⭐⭐⭐  |
| 字段注入             | 反射注入 | 简单直接               | 不可测试、违背原则    | ⭐      |
| Setter 注入          | 初始化后 | 支持可选依赖           | 依赖可变              | ⭐⭐⭐    |
| `@Resource`          | 初始化时 | 按名称优先             | 不灵活                | ⭐⭐     |
| `@Inject`            | 初始化时 | 标准注解               | 不支持 required=false | ⭐⭐     |
| `ApplicationContext` | 手动     | 灵活、动态获取         | 增加耦合              | ⭐⭐     |
| `@Bean`              | 配置类   | 明确控制 Bean 生命周期 | 较繁琐                | ⭐⭐⭐    |

