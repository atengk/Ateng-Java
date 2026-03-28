# 属性配置



## 环境配置

### spring.profiles.active

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

### spring.config.activate.on-profile

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

### 环境变量

如果环境变量存在，则取环境变量，否则取默认值
value: ${ENV_VAR:defaultValue}

```
server:
  port: ${SERVER_PORT:8080}
```

如果设置了 `SERVER_PORT` 环境变量，就用它的值；

否则默认用 `8080`。



## 将配置文件加载到属性类中

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
package io.github.atengk.config.runner;

import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.config.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ApplicationRunner：实现此接口的类会在Spring Boot应用启动后执行其run方法。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MyApplicationRunner implements ApplicationRunner {
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = SpringUtil.getActiveProfiles();
        System.out.println(Arrays.toString(profiles));

        log.info("启动环境：{}", String.join(",", SpringUtil.getActiveProfiles()));
        log.info("配置文件：{}", appProperties);
        log.info("配置文件数据：{}", SpringUtil.getProperty("data"));
    }
}


```



## 使用@Value

`@Value` 是 Spring 提供的一个用于 **属性注入** 的注解，主要用于从 **配置文件**（`application.properties` 或 `application.yml`）、**环境变量** 或 **SpEL 表达式** 中获取值。

---

### **1. `@Value` 的基本用法**
`@Value` 通常用于 **从配置文件加载值**，并提供默认值：

#### **🔹（1）从 `application.properties` 或 `application.yml` 读取**
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

#### **🔹（2）使用默认值**
如果 **配置文件中没有该属性**，可以使用 `:` 提供 **默认值**：
```java
@Value("${app.version:1.0.0}") // 读取 app.version，如果不存在，则使用默认值 "1.0.0"
private String appVersion;
```
> **说明**：
> - `app.version` 不存在时，`appVersion` 变量会被赋值 `"1.0.0"`。

---

### **2. `@Value` 的高级用法**
#### **🔹（1）SpEL 表达式（Spring Expression Language）**
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

#### **🔹（2）从环境变量或系统属性读取**
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

#### **🔹（3）注入 List、Map、数组**
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
