# Forest

Forest 声明式与编程式双修，让天下没有难以发送的 HTTP 请求

参考：[官方文档](https://forest.dromara.org/pages/1.6.x/spring_boot3_install/)



## 基础配置

### 配置 `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>forest</artifactId>
    <version>v1.0</version>
    <name>forest</name>
    <description>Forest 声明式与编程式双修，让天下没有难以发送的 HTTP 请求</description>

    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.4.1</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <forest.version>1.6.3</forest.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok: 简化Java代码编写的依赖项 -->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Forest: 声明式与编程式双修，让天下没有难以发送的 HTTP 请求 -->
        <dependency>
            <groupId>com.dtflys.forest</groupId>
            <artifactId>forest-spring-boot3-starter</artifactId>
            <version>${forest.version}</version>
        </dependency>
        <!-- XML框架依赖 -->
        <dependency>
            <groupId>com.dtflys.forest</groupId>
            <artifactId>forest-jakarta-xml</artifactId>
            <version>${forest.version}</version>
        </dependency>

    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件仓库配置 -->
    <repositories>
        <!-- Central Repository -->
        <repository>
            <id>central</id>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <!--<name>Maven官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>-->
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <!-- 编译参数 -->
                    <compilerArgs>
                        <!-- 启用Java 8参数名称保留功能 -->
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <!-- 第一个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <!-- 第二个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>application*</include>
                    <include>bootstrap*.yml</include>
                    <include>common*</include>
                    <include>banner*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

</project>
```

### 配置 `application.yml`

Forest的详细配置说明，请参见《[Springboot环境配置项](https://forest.dromara.org/pages/1.6.x/spring_boot_config_items/)》

```yaml
server:
  port: 17003
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# HTTP 请求配置
forest:
  backend: okhttp3             # 后端HTTP框架（默认为 okhttp3）
  max-connections: 1000        # 连接池最大连接数（默认为 500）
  max-route-connections: 500   # 每个路由的最大连接数（默认为 500）
  max-request-queue-size: 100  # [自v1.5.22版本起可用] 最大请求等待队列大小
  max-async-thread-size: 300   # [自v1.5.21版本起可用] 最大异步线程数
  max-async-queue-size: 16     # [自v1.5.22版本起可用] 最大异步线程池队列大小
  timeout: 3000                # [已不推荐使用] 请求超时时间，单位为毫秒（默认为 3000）
  connect-timeout: 3000        # 连接超时时间，单位为毫秒（默认为 timeout）
  read-timeout: 3000           # 数据读取超时时间，单位为毫秒（默认为 timeout）
  max-retry-count: 0           # 请求失败后重试次数（默认为 0 次不重试）
  max-retry-interval: 200      # 最大请重试的时间间隔 (毫秒)
  ssl-protocol: TLS            # 单向验证的HTTPS的默认TLS协议（默认为 TLS）
  log-enabled: false           # 打开或关闭日志（默认为 true）
  log-request: true            # 打开/关闭Forest请求日志（默认为 true）
  log-response-status: true    # 打开/关闭Forest响应状态日志（默认为 true）
  log-response-content: true   # 打开/关闭Forest响应内容日志（默认为 false）
  async-mode: platform         # [自v1.5.27版本起可用] 异步模式（默认为 platform）
```



## 编程式接口

### GET请求

以 String 类型接受数据

```java
    @Test
    void getAsString() {
        // 直接以 String 类型接受数据
        String str = Forest.get("https://www.wanandroid.com/article/list/0/json").executeAsString();
        System.out.println(str);
    }
```

不安全的https

```java
    @Test
    void getHttpsAsString() {
        // 不安全的https
        String str = Forest.get("https://192.168.1.12:11443/").executeAsString();
        System.out.println(str);
    }
```

自定义类型

```java
    @Test
    void getAsJson() {
        // 自定义类型
        JSONObject json = Forest.get("https://www.wanandroid.com/article/list/0/json").execute(JSONObject.class);
        System.out.println(json);
    }
```



### POST请求

post请求1

```java
    @Test
    void getAsString() {
        String str = Forest
                .post("https://api.apiopen.top/api/login")
                .contentTypeJson()        // 指定请求体为JSON格式
                .addBody("account", "309324904@qq.com")
                .addBody("password", "123456")
                .executeAsString();
        System.out.println(str);
    }
```

post请求2

```java
    @Test
    void getAsString2() {
        JSONObject body = JSONObject.of(
                "account", "309324904@qq.com",
                "password", "123456"
        );
        String str = Forest
                .post("https://api.apiopen.top/api/login")
                .contentTypeJson()        // 指定请求体为JSON格式
                .addBody(body)
                .executeAsString();
        System.out.println(str);
    }
```

