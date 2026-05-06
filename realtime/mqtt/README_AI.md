# MQTT

MQTT 是一种基于发布/订阅模型的轻量级消息协议，适合在设备端、边缘网关、业务系统之间传输状态数据、遥测数据、控制指令和事件消息。本文档以 Spring Boot 3 为基础，使用 Spring Integration MQTT 作为 Spring 侧集成框架，并基于 Eclipse Paho Client v3 实现 MQTT v3.1/v3.1.1 协议客户端能力。Spring Integration 官方 MQTT 模块提供入站和出站通道适配器，底层实现使用 Eclipse Paho MQTT Client。

## 项目概述

本项目用于在 Spring Boot 3 应用中集成 MQTT 消息通信能力，支持服务端作为 MQTT 客户端连接 Broker，并完成主题订阅、消息接收、消息发布、连接管理、QoS 控制、异常处理和后续业务处理。整体设计重点是将 MQTT 通信能力封装为 Spring Bean，让业务代码不直接依赖底层 Paho Client 细节。

### 技术选型

本章节说明项目中 MQTT 相关技术组件的职责边界。Spring Boot 负责应用启动、配置加载、Bean 管理和运行时集成；Spring Integration MQTT 负责 MQTT 入站和出站通道适配；Eclipse Paho Client v3 负责和 MQTT Broker 建立真实连接并完成协议通信。

| 技术组件    | 选型                      | 说明                                                         |
| ----------- | ------------------------- | ------------------------------------------------------------ |
| JDK         | JDK 17+                   | Spring Boot 3 的基础运行环境要求。                           |
| 应用框架    | Spring Boot 3.x           | 负责应用配置、自动装配、生命周期管理和 Web/API 能力。        |
| 集成框架    | Spring Integration MQTT   | 提供 MQTT 入站通道适配器和出站通道适配器。                   |
| MQTT 客户端 | Eclipse Paho Client v3    | 使用 `org.eclipse.paho.client.mqttv3` 包连接 MQTT Broker。   |
| MQTT Broker | EMQX / Mosquitto / HiveMQ | 开发环境可优先使用 Docker 快速启动。                         |
| 构建工具    | Maven                     | 管理 Spring Boot、Spring Integration、Paho、Hutool 等依赖。  |
| 工具类库    | Hutool                    | 用于字符串、JSON、集合、日期等常见处理。                     |
| 日志框架    | SLF4J + Logback           | Spring Boot 默认日志体系，用于输出连接、订阅、发布和异常日志。 |

Spring Integration 的 MQTT 配置通常通过 `DefaultMqttPahoClientFactory` 完成，并建议注入 `MqttConnectOptions` 作为连接参数载体，而不是直接使用已经废弃的工厂选项配置方式。

### 功能目标

本章节定义本项目需要完成的 MQTT 核心能力，后续代码实现应围绕这些目标展开。

1. 支持连接指定 MQTT Broker，连接参数包括地址、用户名、密码、客户端 ID、连接超时时间、心跳时间、自动重连、清理会话等。
2. 支持订阅一个或多个 MQTT Topic，并根据业务类型分发消息。
3. 支持向指定 Topic 发布消息，发布时可指定 QoS、Retained 标识和消息内容。
4. 支持 QoS 0、QoS 1、QoS 2 三种消息质量等级，根据业务场景选择不同可靠性策略。
5. 支持连接断开、自动重连、订阅恢复、发布异常等场景的日志记录和异常处理。
6. 支持配置化管理 Topic、Broker 地址、客户端 ID、认证信息和默认 QoS。
7. 支持将 MQTT 接收到的消息转换为业务事件，例如设备上报、状态变更、告警通知、指令回执等。
8. 支持后续扩展鉴权、TLS、Topic 权限隔离、消息幂等、消息落库和监控指标。

MQTT 本身采用发布/订阅模型，客户端通过 Topic 解耦生产者和消费者；Paho v3 客户端包中也明确提供同步客户端 `MqttClient`、异步客户端 `MqttAsyncClient`、连接参数 `MqttConnectOptions`、消息对象 `MqttMessage` 和回调接口等核心类型。

### 应用场景

本章节列出适合使用 Spring Boot 3 集成 MQTT 的典型业务场景。MQTT 更适合事件驱动、设备通信、弱网络、轻量消息和实时状态同步场景，不适合替代完整事务消息系统。

| 场景                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 物联网设备数据上报       | 设备向 `device/{productKey}/{deviceId}/property/post` 等主题上报温度、湿度、电量、位置、运行状态等数据。 |
| 设备控制指令下发         | 平台向设备发布控制指令，例如开关控制、参数设置、模式切换、固件升级通知。 |
| 网关数据转发             | 边缘网关汇聚子设备数据后，通过 MQTT 统一转发到平台服务。     |
| 告警事件通知             | 设备故障、离线、越界、异常传感器数据可通过 MQTT 事件主题上报。 |
| 轻量级服务间事件通信     | 内部服务之间可以通过 MQTT 做简单事件广播，但高一致性事务消息仍建议使用 Kafka、RabbitMQ、RocketMQ 等消息中间件。 |
| 移动端或边缘端长连接通信 | 对低带宽、网络不稳定、长连接消息通信有要求的场景，可以使用 MQTT 降低通信成本。 |

## 环境准备

本章节说明开发前需要准备的 Broker、JDK、Maven、Spring Boot 项目和依赖配置。建议先在本地通过 Docker 启动 MQTT Broker，确认发布和订阅链路正常后，再接入 Spring Boot 应用。

### MQTT Broker 准备

本章节使用 Docker 启动 MQTT Broker。开发环境推荐优先使用 EMQX 或 Eclipse Mosquitto：EMQX 提供管理控制台，适合调试 Topic、客户端连接和认证；Mosquitto 更轻量，适合快速验证基础发布订阅能力。EMQX 官方 Docker 镜像示例会暴露 `1883` MQTT TCP 端口和 `18083` 管理控制台端口；Eclipse Mosquitto 是 Docker 官方镜像，并支持 MQTT 5、3.1.1 和 3.1。

使用 EMQX 启动本地 Broker：

```bash
# 拉取并启动 EMQX
# 1883：MQTT TCP 端口
# 8083：MQTT WebSocket 端口
# 8084：MQTT WebSocket SSL 端口
# 8883：MQTT SSL/TLS 端口
# 18083：EMQX Dashboard 控制台端口
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8084:8084 \
  -p 8883:8883 \
  -p 18083:18083 \
  emqx/emqx:latest

# 查看容器状态
docker ps | grep emqx
```

启动后可访问 EMQX Dashboard，默认开发环境重点关注以下信息：

| 配置项         | 示例值                   | 说明                                      |
| -------------- | ------------------------ | ----------------------------------------- |
| Broker 地址    | `tcp://127.0.0.1:1883`   | Spring Boot 应用连接 MQTT Broker 的地址。 |
| Dashboard 地址 | `http://127.0.0.1:18083` | EMQX 管理控制台地址。                     |
| 默认 MQTT 端口 | `1883`                   | 非 TLS MQTT TCP 端口。                    |
| TLS MQTT 端口  | `8883`                   | 启用证书后使用。                          |
| WebSocket 端口 | `8083`                   | WebSocket MQTT 调试时使用。               |

也可以使用 Mosquitto 启动轻量 Broker：

```bash
# 启动 Eclipse Mosquitto
# 1883：MQTT TCP 端口
docker run -d --name mosquitto \
  -p 1883:1883 \
  eclipse-mosquitto:latest

# 查看容器状态
docker ps | grep mosquitto
```

开发阶段可以使用 MQTTX、MQTT Explorer、mosquitto_pub、mosquitto_sub 等工具验证 Broker 是否可用。验证逻辑是先订阅一个测试主题，再向同一个主题发布消息。

```bash
# 订阅测试主题
mosquitto_sub -h 127.0.0.1 -p 1883 -t "demo/test"

# 另开一个终端发布测试消息
mosquitto_pub -h 127.0.0.1 -p 1883 -t "demo/test" -m "hello mqtt"
```

如果订阅终端能够收到 `hello mqtt`，说明本地 Broker 发布订阅链路正常。

### 项目基础环境

本章节定义 Spring Boot 3 MQTT 项目的基础开发环境。建议团队统一 JDK、Maven、Spring Boot 版本，避免因为依赖版本差异导致 Paho Client 或 Spring Integration 适配器行为不一致。

| 环境项                  | 推荐版本                  | 说明                                                         |
| ----------------------- | ------------------------- | ------------------------------------------------------------ |
| JDK                     | 17 或更高版本             | Spring Boot 3 基线要求。                                     |
| Maven                   | 3.9.x                     | 用于构建和依赖管理。                                         |
| Spring Boot             | 3.5.x                     | Spring Boot 3 当前稳定维护线可选版本。Spring 官方文档当前仍列出 3.5.x 稳定线。([Home](https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html?utm_source=chatgpt.com)) |
| Spring Integration MQTT | 使用 Spring Boot BOM 管理 | 不建议在业务项目中手动强行指定 Spring Integration 版本，优先交给 Spring Boot 依赖管理。 |
| MQTT Broker             | EMQX / Mosquitto          | 本地开发使用 Docker 启动即可。                               |

推荐项目结构如下：

```text
springboot3-mqtt-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── mqtt
│   │   │                   ├── MqttApplication.java
│   │   │                   ├── config
│   │   │                   ├── properties
│   │   │                   ├── handler
│   │   │                   ├── service
│   │   │                   └── controller
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
└── README.md
```

目录职责建议如下：

| 目录                        | 说明                                                      |
| --------------------------- | --------------------------------------------------------- |
| `config`                    | MQTT 连接工厂、入站适配器、出站适配器、消息通道等配置类。 |
| `properties`                | MQTT 配置属性映射类。                                     |
| `handler`                   | MQTT 入站消息处理器，负责解析 Topic 和 Payload。          |
| `service`                   | 业务服务层，封装消息发布、业务分发、设备数据处理等逻辑。  |
| `controller`                | 提供测试 API，例如手动发布 MQTT 消息。                    |
| `resources/application.yml` | 配置 Broker 地址、Topic、QoS、客户端 ID、认证信息等。     |

### Maven 依赖配置

本章节给出 Spring Boot 3 集成 MQTT 所需 Maven 依赖。`spring-integration-mqtt` 是 Spring Integration 的 MQTT 模块；如果使用 Spring Integration 6.5 及以上版本，`org.eclipse.paho:org.eclipse.paho.client.mqttv3` 已变为 optional 依赖，因此使用 MQTT v3 时需要在项目中显式引入 Paho v3 客户端。([docs.springframework.org.cn](https://docs.springframework.org.cn/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
            http://maven.apache.org/POM/4.0.0
            https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程，统一管理 Spring、日志、测试等依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-mqtt-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-mqtt-demo</name>
    <description>Spring Boot 3 MQTT demo based on Spring Integration MQTT and Eclipse Paho Client v3</description>

    <properties>
        <!-- Spring Boot 3 推荐使用 JDK 17+ -->
        <java.version>17</java.version>

        <!-- Eclipse Paho MQTT v3 客户端当前常用稳定版本 -->
        <paho.mqttv3.version>1.2.5</paho.mqttv3.version>

        <!-- Hutool 工具类版本，按团队版本规范统一维护 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 能力：用于提供测试接口，例如通过 HTTP 触发 MQTT 消息发布 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Integration MQTT：提供 MQTT 入站和出站通道适配器 -->
        <dependency>
            <groupId>org.springframework.integration</groupId>
            <artifactId>spring-integration-mqtt</artifactId>
        </dependency>

        <!-- Eclipse Paho Client v3：Spring Integration MQTT v3 底层客户端依赖 -->
        <dependency>
            <groupId>org.eclipse.paho</groupId>
            <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
            <version>${paho.mqttv3.version}</version>
        </dependency>

        <!-- Hutool：用于字符串、JSON、集合、日期等常见工具处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 DTO、配置属性类中的样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖：用于单元测试和集成测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包、运行和构建可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置说明：

| 依赖                             | 是否必须     | 说明                                                         |
| -------------------------------- | ------------ | ------------------------------------------------------------ |
| `spring-boot-starter-web`        | 可选         | 如果只作为后台 MQTT 客户端运行，不暴露 HTTP API，可以去掉。  |
| `spring-integration-mqtt`        | 必须         | 提供 MQTT 入站、出站适配能力。                               |
| `org.eclipse.paho.client.mqttv3` | 建议显式引入 | MQTT v3 客户端依赖，Spring Integration 6.5+ 场景下需要显式添加。 |
| `hutool-all`                     | 推荐         | 后续处理 Payload、Topic、JSON、字符串时使用。                |
| `lombok`                         | 可选         | 简化配置类、DTO、VO 代码。                                   |
| `spring-boot-starter-test`       | 推荐         | 用于测试消息发布、配置加载和业务处理逻辑。                   |

Eclipse Paho Java Client 官方说明中，Paho Java Client 用于 JVM 或 Android 等 Java 兼容平台，并提供 `MqttAsyncClient` 异步 API 和 `MqttClient` 同步封装 API；Maven Central 中 `org.eclipse.paho.client.mqttv3` 当前发布版本停留在 `1.2.5`。([eclipse.dev](https://eclipse.dev/paho/clients/java/?utm_source=chatgpt.com))

如果项目已经有统一父工程或公司内部 BOM，建议只保留业务模块需要的依赖，不在业务模块重复声明 Spring Boot parent：

```xml
<dependencies>
    <!-- Spring Integration MQTT：由公司 BOM 或 Spring Boot BOM 统一管理版本 -->
    <dependency>
        <groupId>org.springframework.integration</groupId>
        <artifactId>spring-integration-mqtt</artifactId>
    </dependency>

    <!-- MQTT v3 客户端：用于支持 Eclipse Paho MQTT v3 协议通信 -->
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>1.2.5</version>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
</dependencies>
```

完成依赖配置后，执行以下命令验证依赖是否可以正常解析：

```bash
# 清理并编译项目
mvn clean compile

# 查看 MQTT 相关依赖树
mvn dependency:tree | grep -E "spring-integration-mqtt|paho|mqtt"
```

如果能看到 `spring-integration-mqtt` 和 `org.eclipse.paho.client.mqttv3`，说明 MQTT 基础依赖已经准备完成。后续即可继续编写 `application.yml`、`MqttConnectOptions`、`DefaultMqttPahoClientFactory`、入站订阅适配器和出站发布网关。



## MQTT 基础配置

本章节给出 Spring Boot 3 集成 MQTT 的基础配置，包括 Broker 连接参数、Paho 客户端工厂、Topic 命名规范和 QoS 使用策略。Spring Integration MQTT 的入站和出站适配器都通过 `DefaultMqttPahoClientFactory` 配置连接能力，并且官方推荐通过 `MqttConnectOptions` 注入连接参数，而不是直接在工厂上设置已废弃的连接选项。([Home](https://docs.spring.vmware.com/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

### 连接参数配置

本章节将 MQTT Broker 地址、认证信息、客户端连接行为和订阅主题放入 `application.yml`，便于不同环境通过配置文件或环境变量调整。开发环境可以先使用本地 `tcp://127.0.0.1:1883`，生产环境建议切换为内网 Broker 地址，并按实际安全要求启用用户名密码、TLS 和 ACL。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-mqtt-demo

mqtt:
  # Broker 地址，支持配置多个地址，Paho 会按 serverURIs 策略连接
  server-uris:
    - tcp://127.0.0.1:1883

  # MQTT 认证信息，开发环境可为空，生产环境建议必须配置
  username:
  password:

  # clean-session=true 表示客户端重连后不保留会话；需要持久订阅时可设置为 false
  clean-session: true

  # 是否开启 Paho 自动重连
  automatic-reconnect: true

  # 建立连接超时时间，单位：秒
  connection-timeout: 30

  # 心跳间隔，单位：秒
  keep-alive-interval: 60

  # 最大未确认消息数量，高并发发布场景可适当调大
  max-inflight: 100

  inbound:
    # 入站订阅客户端 ID，同一个 Broker 下需要保持唯一
    client-id: springboot3-mqtt-inbound-${random.uuid}

    # 默认订阅主题，可使用 MQTT 通配符 + 和 #
    topics:
      - device/+/property/post
      - device/+/event/post
      - device/+/status/post

    # 默认订阅 QoS
    qos: 1

    # 消息发送到 Spring Integration 通道的超时时间，单位：毫秒
    completion-timeout: 5000

    # 连接失败后的恢复间隔，单位：毫秒
    recovery-interval: 10000

    # 是否启用手动 ACK；默认 false，由适配器自动确认
    manual-acks: false
```

`MqttConnectOptions` 支持 `serverURIs`、`userName`、`password`、`cleanSession`、`automaticReconnect`、`connectionTimeout`、`keepAliveInterval`、`maxInflight` 等连接参数。Paho 文档中也明确说明，`cleanSession=false` 适用于需要服务端保留订阅状态和 QoS 消息状态的场景；`automaticReconnect=true` 时，客户端在连接丢失后会自动尝试重连。([eclipse.dev](https://eclipse.dev/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html?utm_source=chatgpt.com))

下面创建配置属性类，用于将 `application.yml` 中的 `mqtt` 配置绑定为 Java 对象。

文件位置：`src/main/java/io/github/atengk/mqtt/properties/MqttProperties.java`

```java
package io.github.atengk.mqtt.properties;

import cn.hutool.core.collection.ListUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * MQTT 配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /**
     * Broker 地址列表
     */
    private List<String> serverUris = ListUtil.of("tcp://127.0.0.1:1883");

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否清理会话
     */
    private Boolean cleanSession = true;

    /**
     * 是否自动重连
     */
    private Boolean automaticReconnect = true;

    /**
     * 连接超时时间，单位：秒
     */
    private Integer connectionTimeout = 30;

    /**
     * 心跳间隔，单位：秒
     */
    private Integer keepAliveInterval = 60;

    /**
     * 最大未确认消息数量
     */
    private Integer maxInflight = 100;

    /**
     * 入站订阅配置
     */
    private Inbound inbound = new Inbound();

    /**
     * MQTT 入站订阅配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Inbound {

        /**
         * 入站客户端 ID
         */
        private String clientId = "springboot3-mqtt-inbound";

        /**
         * 订阅主题列表
         */
        private List<String> topics = ListUtil.of("device/+/property/post");

        /**
         * 默认 QoS
         */
        private Integer qos = 1;

        /**
         * 完成超时时间，单位：毫秒
         */
        private Long completionTimeout = 5000L;

        /**
         * 恢复间隔，单位：毫秒
         */
        private Long recoveryInterval = 10000L;

        /**
         * 是否手动 ACK
         */
        private Boolean manualAcks = false;
    }
}
```

### 客户端工厂配置

本章节创建 MQTT 客户端工厂。`DefaultMqttPahoClientFactory` 是 Spring Integration MQTT 连接 Paho Client v3 的核心工厂，入站订阅适配器和后续出站发布适配器都可以复用它。Spring Integration MQTT 入站消息默认会将接收主题、QoS、Retained 等信息放入消息头；从 Spring Integration 5.0 开始，接收侧字段会映射到 `MqttHeaders.RECEIVED_TOPIC`、`MqttHeaders.RECEIVED_QOS`、`MqttHeaders.RECEIVED_RETAINED`，避免误传给出站消息头。([Home](https://docs.spring.vmware.com/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mqtt/config/MqttIntegrationConfig.java`

```java
package io.github.atengk.mqtt.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mqtt.properties.MqttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EnableIntegration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

import java.util.List;

/**
 * MQTT 集成配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
@EnableConfigurationProperties(MqttProperties.class)
public class MqttIntegrationConfig {

    private final MqttProperties mqttProperties;

    /**
     * 构建 MQTT 连接参数
     *
     * @return MQTT 连接参数
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        if (CollUtil.isEmpty(mqttProperties.getServerUris())) {
            throw new IllegalArgumentException("MQTT Broker 地址不能为空");
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(mqttProperties.getServerUris().toArray(new String[0]));
        options.setCleanSession(Boolean.TRUE.equals(mqttProperties.getCleanSession()));
        options.setAutomaticReconnect(Boolean.TRUE.equals(mqttProperties.getAutomaticReconnect()));
        options.setConnectionTimeout(mqttProperties.getConnectionTimeout());
        options.setKeepAliveInterval(mqttProperties.getKeepAliveInterval());
        options.setMaxInflight(mqttProperties.getMaxInflight());
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        if (CharSequenceUtil.isNotBlank(mqttProperties.getUsername())) {
            options.setUserName(mqttProperties.getUsername());
        }
        if (CharSequenceUtil.isNotBlank(mqttProperties.getPassword())) {
            options.setPassword(mqttProperties.getPassword().toCharArray());
        }

        log.info("初始化MQTT连接参数，Broker数量：{}，cleanSession：{}，自动重连：{}",
                mqttProperties.getServerUris().size(),
                mqttProperties.getCleanSession(),
                mqttProperties.getAutomaticReconnect());

        return options;
    }

    /**
     * 构建 MQTT Paho 客户端工厂
     *
     * @param mqttConnectOptions MQTT 连接参数
     * @return MQTT Paho 客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttPahoClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    /**
     * MQTT 入站消息通道
     *
     * @return MQTT 入站消息通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 错误消息通道
     *
     * @return MQTT 错误消息通道
     */
    @Bean
    public MessageChannel mqttErrorChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 入站适配器
     *
     * @param mqttPahoClientFactory MQTT 客户端工厂
     * @param mqttInputChannel      MQTT 入站消息通道
     * @param mqttErrorChannel      MQTT 错误消息通道
     * @return MQTT 入站适配器
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory mqttPahoClientFactory,
            @Qualifier("mqttInputChannel") MessageChannel mqttInputChannel,
            @Qualifier("mqttErrorChannel") MessageChannel mqttErrorChannel) {

        MqttProperties.Inbound inbound = mqttProperties.getInbound();
        List<String> topics = inbound.getTopics();
        if (CollUtil.isEmpty(topics)) {
            throw new IllegalArgumentException("MQTT 入站订阅主题不能为空");
        }

        String[] topicArray = topics.toArray(new String[0]);
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                inbound.getClientId(),
                mqttPahoClientFactory,
                topicArray
        );

        adapter.setQos(buildQosArray(topicArray.length, inbound.getQos()));
        adapter.setCompletionTimeout(inbound.getCompletionTimeout());
        adapter.setRecoveryInterval(inbound.getRecoveryInterval());
        adapter.setManualAcks(Boolean.TRUE.equals(inbound.getManualAcks()));
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(mqttInputChannel);
        adapter.setErrorChannel(mqttErrorChannel);

        log.info("初始化MQTT入站适配器，clientId：{}，topics：{}，qos：{}",
                inbound.getClientId(),
                JSONUtil.toJsonStr(topics),
                inbound.getQos());

        return adapter;
    }

    /**
     * 构建 QoS 数组
     *
     * @param topicSize 主题数量
     * @param qos       QoS
     * @return QoS 数组
     */
    private int[] buildQosArray(int topicSize, Integer qos) {
        int safeQos = qos == null ? 1 : qos;
        if (safeQos < 0 || safeQos > 2) {
            throw new IllegalArgumentException("MQTT QoS 只能是 0、1、2");
        }

        int[] qosArray = new int[topicSize];
        for (int i = 0; i < topicSize; i++) {
            qosArray[i] = safeQos;
        }
        return qosArray;
    }
}
```

### Topic 规划

本章节定义项目中的 Topic 命名规范。Topic 规划的目标是让设备、产品、消息类型和动作语义保持稳定，方便 Broker 做 ACL 权限控制，也方便服务端根据 Topic 路由到不同业务处理器。

推荐 Topic 采用以下格式：

```text
{domain}/{productKey}/{deviceId}/{messageType}/{action}
```

字段说明如下：

| 字段          | 示例                                     | 说明                                  |
| ------------- | ---------------------------------------- | ------------------------------------- |
| `domain`      | `device`                                 | 业务域，例如设备侧统一使用 `device`。 |
| `productKey`  | `p001`                                   | 产品标识，用于区分设备模型或产品线。  |
| `deviceId`    | `d10001`                                 | 设备唯一标识。                        |
| `messageType` | `property`、`event`、`status`、`command` | 消息类型。                            |
| `action`      | `post`、`set`、`reply`                   | 动作语义。                            |

推荐 Topic 示例：

| Topic                                                    | 方向       | 说明                     |
| -------------------------------------------------------- | ---------- | ------------------------ |
| `device/{productKey}/{deviceId}/property/post`           | 设备到平台 | 设备属性上报。           |
| `device/{productKey}/{deviceId}/event/post`              | 设备到平台 | 设备事件上报。           |
| `device/{productKey}/{deviceId}/status/post`             | 设备到平台 | 设备状态上报。           |
| `device/{productKey}/{deviceId}/command/set`             | 平台到设备 | 平台下发控制命令。       |
| `device/{productKey}/{deviceId}/command/reply`           | 设备到平台 | 设备返回命令执行结果。   |
| `gateway/{gatewayId}/subdevice/{deviceId}/property/post` | 网关到平台 | 网关代理子设备属性上报。 |

订阅时可以使用 MQTT 通配符降低配置数量：

| 订阅 Topic               | 说明                                                       |
| ------------------------ | ---------------------------------------------------------- |
| `device/+/property/post` | 订阅所有产品下的属性上报，不区分设备。                     |
| `device/+/event/post`    | 订阅所有产品下的事件上报。                                 |
| `device/+/status/post`   | 订阅所有产品下的状态上报。                                 |
| `device/#`               | 订阅 `device` 域下所有消息，调试可用，生产环境不建议滥用。 |

Topic 规划建议：

1. 生产环境不要把所有业务都放到 `#` 或 `device/#`，否则消息量增大后很难做权限隔离和业务路由。
2. 服务端订阅建议按业务类型拆分，例如属性、事件、状态、回执分别处理。
3. 设备侧订阅平台命令时，建议订阅到具体设备级别，例如 `device/p001/d10001/command/set`。
4. Topic 中不要放敏感信息，例如手机号、身份证号、Token、密码等。
5. 如果需要跨租户隔离，可以在最前面增加租户维度，例如 `tenant/{tenantId}/device/{productKey}/{deviceId}/property/post`。

### QoS 策略

本章节定义不同业务消息的 QoS 使用策略。MQTT QoS 控制消息交付质量，值只能是 `0`、`1`、`2`。QoS 越高，协议交互越多，可靠性越强，但吞吐和延迟成本也越高。Paho 的 `setWill`、订阅和发布相关 API 均使用 `0`、`1`、`2` 表示 QoS。([eclipse.dev](https://eclipse.dev/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html?utm_source=chatgpt.com))

| QoS   | 语义     | 适用场景                         | 使用建议                                         |
| ----- | -------- | -------------------------------- | ------------------------------------------------ |
| QoS 0 | 最多一次 | 高频遥测、可丢弃数据、实时位置流 | 数据量大且允许少量丢失时使用。                   |
| QoS 1 | 至少一次 | 属性上报、事件上报、命令回执     | 推荐作为大多数业务消息默认值，需要业务侧做幂等。 |
| QoS 2 | 只有一次 | 金额、强一致控制、关键状态变更   | 成本最高，谨慎使用，通常只用于关键消息。         |

推荐策略如下：

| 消息类型       | Topic 示例                                   | 推荐 QoS | 说明                                                   |
| -------------- | -------------------------------------------- | -------- | ------------------------------------------------------ |
| 属性上报       | `device/+/property/post`                     | 1        | 属性变更一般需要可靠到达，但可能重复，需要幂等处理。   |
| 事件上报       | `device/+/event/post`                        | 1        | 告警、故障、业务事件建议至少一次。                     |
| 状态上报       | `device/+/status/post`                       | 0 或 1   | 在线状态可使用 QoS 0；关键状态可使用 QoS 1。           |
| 命令下发       | `device/{productKey}/{deviceId}/command/set` | 1        | 平台下发控制命令一般要求可达，但仍要处理重复命令。     |
| 命令回执       | `device/+/command/reply`                     | 1        | 平台需要知道，但仍要处理重复命令执行结果，建议 QoS 1。 |
| 关键交易类消息 | 按业务定义                                   | 2        | 只有确有必要时使用。                                   |

业务侧必须注意：QoS 1 表示“至少一次”，不是“只处理一次”。如果设备端或服务端发生重试，业务处理器可能收到重复消息。因此建议 Payload 中包含 `messageId`、`timestamp`、`deviceId` 等字段，服务端基于 `messageId` 做幂等。

## 消息订阅实现

本章节实现 MQTT 入站订阅能力。核心流程是：`MqttPahoMessageDrivenChannelAdapter` 连接 Broker 并订阅 Topic，收到消息后写入 `mqttInputChannel`，再由 Spring Integration 的 `@ServiceActivator` 分发到业务处理器。

### 入站通道配置

本章节使用 `DirectChannel` 作为 MQTT 入站消息通道。`DirectChannel` 会在发送线程中直接调用订阅者，适合处理逻辑较轻、希望快速完成链路验证的场景。如果业务处理耗时较长，建议后续改为线程池通道，或在消息处理器中异步投递到业务线程池、队列、Kafka、RabbitMQ 等组件。

入站通道已经在 `MqttIntegrationConfig` 中定义：

```java
@Bean
public MessageChannel mqttInputChannel() {
    return new DirectChannel();
}

@Bean
public MessageChannel mqttErrorChannel() {
    return new DirectChannel();
}
```

配置说明：

| Bean               | 说明                                     |
| ------------------ | ---------------------------------------- |
| `mqttInputChannel` | MQTT 正常消息输入通道。                  |
| `mqttErrorChannel` | MQTT 入站处理异常通道。                  |
| `DirectChannel`    | 同步调用订阅者，简单直接，适合轻量处理。 |

生产环境建议不要在 `DirectChannel` 订阅者中执行长耗时任务，例如大量数据库写入、远程 HTTP 调用、批量文件处理等。如果需要执行这些任务，可以将 MQTT 消息转换为内部事件后异步处理。

### MQTT 入站适配器配置

本章节使用 `MqttPahoMessageDrivenChannelAdapter` 实现 MQTT 订阅。该适配器负责连接 Broker、订阅 Topic、接收消息并转换为 Spring Message。官方 API 中 `MqttPahoMessageDrivenChannelAdapter` 支持构造时传入客户端工厂和 Topic，也提供 `addTopic(String topic, int qos)`、`removeTopic(String... topic)` 用于([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-integration/docs/6.2.13/api/org/springframework/integration/mqtt/inbound/MqttPahoMessageDrivenChannelAdapter.html?utm_source=chatgpt.com))96search1

核心配置已在 `MqttIntegrationConfig` 中完成：

```java
@Bean
public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
        MqttPahoClientFactory mqttPahoClientFactory,
        @Qualifier("mqttInputChannel") MessageChannel mqttInputChannel,
        @Qualifier("mqttErrorChannel") MessageChannel mqttErrorChannel) {

    MqttProperties.Inbound inbound = mqttProperties.getInbound();
    List<String> topics = inbound.getTopics();
    if (CollUtil.isEmpty(topics)) {
        throw new IllegalArgumentException("MQTT 入站订阅主题不能为空");
    }

    String[] topicArray = topics.toArray(new String[0]);
    MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
            inbound.getClientId(),
            mqttPahoClientFactory,
            topicArray
    );

    adapter.setQos(buildQosArray(topicArray.length, inbound.getQos()));
    adapter.setCompletionTimeout(inbound.getCompletionTimeout());
    adapter.setRecoveryInterval(inbound.getRecoveryInterval());
    adapter.setManualAcks(Boolean.TRUE.equals(inbound.getManualAcks()));
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setOutputChannel(mqttInputChannel);
    adapter.setErrorChannel(mqttErrorChannel);

    return adapter;
}
```

关键配置说明：

| 配置项                 | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| `clientId`             | MQTT 客户端 ID，同一个 Broker 下必须唯一。                   |
| `clientFactory`        | 使用前面定义的 `DefaultMqttPahoClientFactory`。              |
| `topics`               | 初始订阅主题列表。                                           |
| `setQos`               | 设置订阅 QoS。可以统一设置，也可以为不同主题设置不同 QoS。   |
| `setCompletionTimeout` | 发送到 Spring Integration 通道的完成超时时间。               |
| `setRecoveryInterval`  | 连接失败后的恢复间隔。                                       |
| `setManualAcks`        | 是否启用手动确认。默认建议自动确认，复杂可靠性场景再开启手动确认。 |
| `setConverter`         | MQTT 消息和 Spring Message 的转换器。                        |
| `setOutputChannel`     | 正常消息输出到 `mqttInputChannel`。                          |
| `setErrorChannel`      | 下游处理异常输出到 `mqttErrorChannel`。                      |

### 消息处理器实现

本章节实现 MQTT 消息处理器。处理器从消息头中读取接收 Topic、QoS、Retained 标识，从 Payload 中读取业务数据，再交给业务服务按 Topic 类型分发处理。

Spring Integration MQTT 入站消息会携带接收侧 MQTT 头信息，常用字段包括 `MqttHeaders.RECEIVED_TOPIC`、`MqttHeaders.RECEIVED_QOS`、`MqttHeaders.RECEIVED_RETAINED`。这些字段用于识别消息来源 Topic、订阅 QoS 和 Re([Home](https://docs.spring.io/spring-integration/docs/6.5.3/api/org/springframework/integration/mqtt/support/MqttHeaders.html?utm_source=chatgpt.com))28search3

文件位置：`src/main/java/io/github/atengk/mqtt/handler/MqttInboundMessageHandler.java`

```java
package io.github.atengk.mqtt.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.mqtt.service.DeviceMqttMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * MQTT 入站消息处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttInboundMessageHandler {

    private final DeviceMqttMessageService deviceMqttMessageService;

    /**
     * 处理 MQTT 入站消息
     *
     * @param message Spring Integration 消息
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topic = Convert.toStr(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        Integer qos = Convert.toInt(message.getHeaders().get(MqttHeaders.RECEIVED_QOS), 0);
        Boolean retained = Convert.toBool(message.getHeaders().get(MqttHeaders.RECEIVED_RETAINED), false);
        String payload = Convert.toStr(message.getPayload(), CharSequenceUtil.EMPTY);

        if (CharSequenceUtil.isBlank(topic)) {
            log.warn("收到MQTT消息但Topic为空，payload：{}", payload);
            return;
        }

        log.info("收到MQTT消息，topic：{}，qos：{}，retained：{}，payload：{}", topic, qos, retained, payload);
        deviceMqttMessageService.handleMessage(topic, payload, qos, retained);
    }

    /**
     * 处理 MQTT 入站异常
     *
     * @param errorMessage 错误消息
     */
    @ServiceActivator(inputChannel = "mqttErrorChannel")
    public void handleError(ErrorMessage errorMessage) {
        Throwable throwable = errorMessage.getPayload();
        log.error("MQTT入站消息处理异常：{}", throwable.getMessage(), throwable);
    }
}
```

下面定义业务服务接口，屏蔽 MQTT 框架细节，让业务层只关注 Topic 和 Payload。

文件位置：`src/main/java/io/github/atengk/mqtt/service/DeviceMqttMessageService.java`

```java
package io.github.atengk.mqtt.service;

/**
 * 设备 MQTT 消息服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DeviceMqttMessageService {

    /**
     * 处理 MQTT 消息
     *
     * @param topic    主题
     * @param payload  消息内容
     * @param qos      QoS
     * @param retained 是否保留消息
     */
    void handleMessage(String topic, String payload, Integer qos, Boolean retained);
}
```

下面是业务服务实现，包含属性上报、事件上报、状态上报三类常见消息处理。实际项目中可以在这里写入数据库、推送 WebSocket、投递 MQ 或触发告警规则。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/DeviceMqttMessageServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mqtt.service.DeviceMqttMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备 MQTT 消息服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class DeviceMqttMessageServiceImpl implements DeviceMqttMessageService {

    /**
     * 处理 MQTT 消息
     *
     * @param topic    主题
     * @param payload  消息内容
     * @param qos      QoS
     * @param retained 是否保留消息
     */
    @Override
    public void handleMessage(String topic, String payload, Integer qos, Boolean retained) {
        if (CharSequenceUtil.isBlank(payload)) {
            log.warn("MQTT消息内容为空，topic：{}", topic);
            return;
        }
        if (!JSONUtil.isTypeJSON(payload)) {
            log.warn("MQTT消息不是JSON格式，topic：{}，payload：{}", topic, payload);
            return;
        }

        List<String> topicLevels = CharSequenceUtil.split(topic, '/');
        if (CollUtil.size(topicLevels) < 5) {
            log.warn("MQTT Topic格式不符合规范，topic：{}", topic);
            return;
        }

        String domain = topicLevels.get(0);
        String productKey = topicLevels.get(1);
        String deviceId = topicLevels.get(2);
        String messageType = topicLevels.get(3);
        String action = topicLevels.get(4);

        if (!CharSequenceUtil.equals("device", domain)) {
            log.warn("忽略非设备域MQTT消息，topic：{}", topic);
            return;
        }

        JSONObject body = JSONUtil.parseObj(payload);
        String messageId = body.getStr("messageId");
        if (CharSequenceUtil.isBlank(messageId)) {
            log.warn("MQTT消息缺少messageId，topic：{}，payload：{}", topic, payload);
            return;
        }

        if (CharSequenceUtil.equals("property", messageType) && CharSequenceUtil.equals("post", action)) {
            handlePropertyPost(productKey, deviceId, messageId, body, qos, retained);
            return;
        }

        if (CharSequenceUtil.equals("event", messageType) && CharSequenceUtil.equals("post", action)) {
            handleEventPost(productKey, deviceId, messageId, body, qos, retained);
            return;
        }

        if (CharSequenceUtil.equals("status", messageType) && CharSequenceUtil.equals("post", action)) {
            handleStatusPost(productKey, deviceId, messageId, body, qos, retained);
            return;
        }

        log.warn("未匹配到MQTT业务处理器，topic：{}，messageType：{}，action：{}", topic, messageType, action);
    }

    /**
     * 处理属性上报
     *
     * @param productKey 产品标识
     * @param deviceId   设备标识
     * @param messageId  消息ID
     * @param body       消息体
     * @param qos        QoS
     * @param retained   是否保留消息
     */
    private void handlePropertyPost(String productKey, String deviceId, String messageId,
                                    JSONObject body, Integer qos, Boolean retained) {
        JSONObject properties = body.getJSONObject("properties");
        if (ObjUtil.isNull(properties) || properties.isEmpty()) {
            log.warn("设备属性上报内容为空，productKey：{}，deviceId：{}，messageId：{}", productKey, deviceId, messageId);
            return;
        }

        log.info("处理设备属性上报，productKey：{}，deviceId：{}，messageId：{}，qos：{}，retained：{}，properties：{}",
                productKey, deviceId, messageId, qos, retained, properties);
    }

    /**
     * 处理事件上报
     *
     * @param productKey 产品标识
     * @param deviceId   设备标识
     * @param messageId  消息ID
     * @param body       消息体
     * @param qos        QoS
     * @param retained   是否保留消息
     */
    private void handleEventPost(String productKey, String deviceId, String messageId,
                                 JSONObject body, Integer qos, Boolean retained) {
        String eventCode = body.getStr("eventCode");
        JSONObject data = body.getJSONObject("data");

        if (CharSequenceUtil.isBlank(eventCode)) {
            log.warn("设备事件上报缺少eventCode，productKey：{}，deviceId：{}，messageId：{}", productKey, deviceId, messageId);
            return;
        }

        log.info("处理设备事件上报，productKey：{}，deviceId：{}，messageId：{}，eventCode：{}，qos：{}，retained：{}，data：{}",
                productKey, deviceId, messageId, eventCode, qos, retained, data);
    }

    /**
     * 处理状态上报
     *
     * @param productKey 产品标识
     * @param deviceId   设备标识
     * @param messageId  消息ID
     * @param body       消息体
     * @param qos        QoS
     * @param retained   是否保留消息
     */
    private void handleStatusPost(String productKey, String deviceId, String messageId,
                                  JSONObject body, Integer qos, Boolean retained) {
        String status = body.getStr("status");
        if (CharSequenceUtil.isBlank(status)) {
            log.warn("设备状态上报缺少status，productKey：{}，deviceId：{}，messageId：{}", productKey, deviceId, messageId);
            return;
        }

        log.info("处理设备状态上报，productKey：{}，deviceId：{}，messageId：{}，status：{}，qos：{}，retained：{}",
                productKey, deviceId, messageId, status, qos, retained);
    }
}
```

可以使用以下测试消息验证属性上报处理逻辑：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/property/post" \
  -q 1 \
  -m '{"messageId":"msg-10001","timestamp":1714960000000,"properties":{"temperature":26.5,"humidity":61}}'
```

可以使用以下测试消息验证事件上报处理逻辑：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/event/post" \
  -q 1 \
  -m '{"messageId":"msg-10002","timestamp":1714960001000,"eventCode":"over_temperature","data":{"temperature":85.2}}'
```

可以使用以下测试消息验证状态上报处理逻辑：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/status/post" \
  -q 1 \
  -m '{"messageId":"msg-10003","timestamp":1714960002000,"status":"online"}'
```

启动应用后，如果控制台能够看到 `处理设备属性上报`、`处理设备事件上报`、`处理设备状态上报` 等日志，说明入站订阅链路已经打通。

### 订阅主题动态扩展

本章节实现运行时动态增加和移除订阅 Topic。该能力适用于设备产品线动态增加、租户隔离 Topic 动态加载、后台管理页面临时订阅调试等场景。`MqttPahoMessageDrivenChannelAdapter` 官方 API 提供 `addTopic(String topic, int qos)` 和 `removeTopic(String... topic)`，可以用于([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-integration/docs/6.2.13/api/org/springframework/integration/mqtt/inbound/MqttPahoMessageDrivenChannelAdapter.html?utm_source=chatgpt.com))96search1

先定义动态订阅服务接口。

文件位置：`src/main/java/io/github/atengk/mqtt/service/MqttTopicService.java`

```java
package io.github.atengk.mqtt.service;

import java.util.Set;

/**
 * MQTT 订阅主题服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MqttTopicService {

    /**
     * 新增订阅主题
     *
     * @param topic 主题
     * @param qos   QoS
     */
    void subscribe(String topic, Integer qos);

    /**
     * 移除订阅主题
     *
     * @param topic 主题
     */
    void unsubscribe(String topic);

    /**
     * 获取当前订阅主题
     *
     * @return 当前订阅主题集合
     */
    Set<String> listTopics();
}
```

下面实现动态订阅服务。这里用内存集合维护运行时主题，适合开发和单实例服务；如果是多实例部署或需要重启后恢复动态订阅，建议将动态 Topic 存入数据库、配置中心或 Redis，并在应用启动后重新加载。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/MqttTopicServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.mqtt.properties.MqttProperties;
import io.github.atengk.mqtt.service.MqttTopicService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 订阅主题服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttTopicServiceImpl implements MqttTopicService {

    private final MqttProperties mqttProperties;
    private final MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter;

    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();

    /**
     * 初始化默认主题
     */
    @PostConstruct
    public void initTopics() {
        if (CollUtil.isNotEmpty(mqttProperties.getInbound().getTopics())) {
            topicSet.addAll(mqttProperties.getInbound().getTopics());
        }
        log.info("初始化MQTT订阅主题缓存，topics：{}", topicSet);
    }

    /**
     * 新增订阅主题
     *
     * @param topic 主题
     * @param qos   QoS
     */
    @Override
    public void subscribe(String topic, Integer qos) {
        validateTopic(topic);
        int safeQos = validateQos(qos);

        if (topicSet.contains(topic)) {
            log.info("MQTT主题已订阅，忽略重复订阅，topic：{}", topic);
            return;
        }

        mqttInboundAdapter.addTopic(topic, safeQos);
        topicSet.add(topic);
        log.info("新增MQTT订阅主题成功，topic：{}，qos：{}", topic, safeQos);
    }

    /**
     * 移除订阅主题
     *
     * @param topic 主题
     */
    @Override
    public void unsubscribe(String topic) {
        validateTopic(topic);

        if (!topicSet.contains(topic)) {
            log.info("MQTT主题未订阅，忽略取消订阅，topic：{}", topic);
            return;
        }

        mqttInboundAdapter.removeTopic(topic);
        topicSet.remove(topic);
        log.info("移除MQTT订阅主题成功，topic：{}", topic);
    }

    /**
     * 获取当前订阅主题
     *
     * @return 当前订阅主题集合
     */
    @Override
    public Set<String> listTopics() {
        return Collections.unmodifiableSet(topicSet);
    }

    /**
     * 校验主题
     *
     * @param topic 主题
     */
    private void validateTopic(String topic) {
        if (CharSequenceUtil.isBlank(topic)) {
            throw new IllegalArgumentException("MQTT Topic 不能为空");
        }
        if (CharSequenceUtil.startWith(topic, "/") || CharSequenceUtil.endWith(topic, "/")) {
            throw new IllegalArgumentException("MQTT Topic 不能以 / 开头或结尾");
        }
        if (CharSequenceUtil.contains(topic, " ")) {
            throw new IllegalArgumentException("MQTT Topic 不能包含空格");
        }
    }

    /**
     * 校验 QoS
     *
     * @param qos QoS
     * @return 安全 QoS
     */
    private int validateQos(Integer qos) {
        int safeQos = qos == null ? mqttProperties.getInbound().getQos() : qos;
        if (safeQos < 0 || safeQos > 2) {
            throw new IllegalArgumentException("MQTT QoS 只能是 0、1、2");
        }
        return safeQos;
    }
}
```

为了便于开发阶段调试，可以提供一个简单的 HTTP 接口管理动态订阅主题。生产环境必须增加权限控制，避免任意用户订阅 `#` 或敏感 Topic。

文件位置：`src/main/java/io/github/atengk/mqtt/controller/MqttTopicController.java`

```java
package io.github.atengk.mqtt.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.mqtt.dto.MqttTopicRequest;
import io.github.atengk.mqtt.service.MqttTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * MQTT 订阅主题控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mqtt/topics")
public class MqttTopicController {

    private final MqttTopicService mqttTopicService;

    /**
     * 新增订阅主题
     *
     * @param request 订阅请求
     * @return 处理结果
     */
    @PostMapping("/subscribe")
    public Dict subscribe(@RequestBody MqttTopicRequest request) {
        mqttTopicService.subscribe(request.getTopic(), request.getQos());
        return Dict.create()
                .set("success", true)
                .set("message", "订阅成功")
                .set("topic", request.getTopic())
                .set("qos", request.getQos());
    }

    /**
     * 移除订阅主题
     *
     * @param request 订阅请求
     * @return 处理结果
     */
    @PostMapping("/unsubscribe")
    public Dict unsubscribe(@RequestBody MqttTopicRequest request) {
        mqttTopicService.unsubscribe(request.getTopic());
        return Dict.create()
                .set("success", true)
                .set("message", "取消订阅成功")
                .set("topic", request.getTopic());
    }

    /**
     * 查询当前订阅主题
     *
     * @return 当前订阅主题
     */
    @GetMapping
    public Dict listTopics() {
        Set<String> topics = mqttTopicService.listTopics();
        return Dict.create()
                .set("success", true)
                .set("topics", topics);
    }
}
```

文件位置：`src/main/java/io/github/atengk/mqtt/dto/MqttTopicRequest.java`

```java
package io.github.atengk.mqtt.dto;

import lombok.Data;

/**
 * MQTT 主题请求
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class MqttTopicRequest {

    /**
     * 主题
     */
    private String topic;

    /**
     * QoS
     */
    private Integer qos;
}
```

接口使用示例：

```bash
# 新增动态订阅主题
curl -X POST "http://127.0.0.1:8080/mqtt/topics/subscribe" \
  -H "Content-Type: application/json" \
  -d '{"topic":"device/+/command/reply","qos":1}'

# 查询当前订阅主题
curl -X GET "http://127.0.0.1:8080/mqtt/topics"

# 移除动态订阅主题
curl -X POST "http://127.0.0.1:8080/mqtt/topics/unsubscribe" \
  -H "Content-Type: application/json" \
  -d '{"topic":"device/+/command/reply"}'
```

动态订阅注意事项：

1. `addTopic` 只会修改当前运行实例的订阅状态，应用重启后仍以 `application.yml` 或持久化配置为准。
2. 多实例部署时，每个实例都有自己的 MQTT 客户端连接，动态订阅需要广播到所有实例，或统一从配置中心加载。
3. 不建议开放任意 `#` 订阅能力给普通用户，容易造成消息泄露和系统压力。
4. 如果 `cleanSession=false`，需要注意 Broker 端会话保留、离线消息堆积和客户端 ID 唯一性。
5. 如果启用 QoS 1 或 QoS 2，业务处理必须按 `messageId` 做幂等，避免重复消息造成重复

库或重复执行指令。



## 消息发布实现

本章节实现 MQTT 出站发布能力。Spring Integration MQTT 的出站通道适配器由 `MqttPahoMessageHandler` 实现，默认转换器 `DefaultPahoMessageConverter` 会识别 `mqtt_topic`、`mqtt_qos`、`mqtt_retained` 这些消息头，用于决定发布 Topic、QoS 和 Retained 标识。出站适配器还支持异步发送和发送/送达事件，便于业务侧记录发布链路日志。([Home](https://docs.spring.vmware.com/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

### 出站通道配置

本章节先扩展 MQTT 配置项和 Spring Integration 出站通道。出站通道用于接收业务服务发送的 Spring Message，再由 `MqttPahoMessageHandler` 转换并发布到 MQTT Broker。

在 `application.yml` 的 `mqtt` 配置下增加 `outbound` 配置：

```yaml
mqtt:
  outbound:
    # 出站发布客户端 ID，同一个 Broker 下需要保持唯一
    client-id: springboot3-mqtt-outbound-${random.uuid}

    # 默认发布主题。当消息头未指定 mqtt_topic 时使用
    default-topic: device/default/default/command/set

    # 默认发布 QoS
    default-qos: 1

    # 默认是否保留消息
    default-retained: false

    # 是否异步发布。false 表示发送线程阻塞等待交付确认
    async: false

    # async=true 时是否发布发送/送达事件
    async-events: true

    # MQTT 出站适配器完成超时时间，单位：毫秒
    completion-timeout: 5000

    # MessageChannel 发送超时时间，单位：毫秒
    send-timeout: 3000
```

在上一章节的 `MqttProperties` 中增加 `outbound` 字段和内部类。

文件位置：`src/main/java/io/github/atengk/mqtt/properties/MqttProperties.java`

```java
/**
 * 出站发布配置
 */
private Outbound outbound = new Outbound();

/**
 * MQTT 出站发布配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public static class Outbound {

    /**
     * 出站客户端 ID
     */
    private String clientId = "springboot3-mqtt-outbound";

    /**
     * 默认发布主题
     */
    private String defaultTopic = "device/default/default/command/set";

    /**
     * 默认 QoS
     */
    private Integer defaultQos = 1;

    /**
     * 默认是否保留消息
     */
    private Boolean defaultRetained = false;

    /**
     * 是否异步发布
     */
    private Boolean async = false;

    /**
     * 是否发布异步事件
     */
    private Boolean asyncEvents = true;

    /**
     * 完成超时时间，单位：毫秒
     */
    private Long completionTimeout = 5000L;

    /**
     * 通道发送超时时间，单位：毫秒
     */
    private Long sendTimeout = 3000L;
}
```

在上一章节的 `MqttIntegrationConfig` 中追加出站通道和出站适配器配置。

文件位置：`src/main/java/io/github/atengk/mqtt/config/MqttIntegrationConfig.java`

```java
/**
 * MQTT 出站消息通道
 *
 * @return MQTT 出站消息通道
 */
@Bean
public MessageChannel mqttOutboundChannel() {
    return new DirectChannel();
}

/**
 * MQTT 出站适配器
 *
 * @param mqttPahoClientFactory MQTT 客户端工厂
 * @return MQTT 出站消息处理器
 */
@Bean
@ServiceActivator(inputChannel = "mqttOutboundChannel")
public MessageHandler mqttOutboundHandler(MqttPahoClientFactory mqttPahoClientFactory) {
    MqttProperties.Outbound outbound = mqttProperties.getOutbound();
    int defaultQos = validateQos(outbound.getDefaultQos());

    MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
            outbound.getClientId(),
            mqttPahoClientFactory
    );
    handler.setAsync(Boolean.TRUE.equals(outbound.getAsync()));
    handler.setAsyncEvents(Boolean.TRUE.equals(outbound.getAsyncEvents()));
    handler.setDefaultTopic(outbound.getDefaultTopic());
    handler.setDefaultQos(defaultQos);
    handler.setDefaultRetained(Boolean.TRUE.equals(outbound.getDefaultRetained()));
    handler.setCompletionTimeout(outbound.getCompletionTimeout());
    handler.setConverter(new DefaultPahoMessageConverter(defaultQos, Boolean.TRUE.equals(outbound.getDefaultRetained())));

    log.info("初始化MQTT出站适配器，clientId：{}，defaultTopic：{}，defaultQos：{}，async：{}",
            outbound.getClientId(),
            outbound.getDefaultTopic(),
            defaultQos,
            outbound.getAsync());

    return handler;
}

/**
 * 校验 QoS
 *
 * @param qos QoS
 * @return 安全 QoS
 */
private int validateQos(Integer qos) {
    int safeQos = qos == null ? 1 : qos;
    if (safeQos < 0 || safeQos > 2) {
        throw new IllegalArgumentException("MQTT QoS 只能是 0、1、2");
    }
    return safeQos;
}
```

如果你的 `MqttIntegrationConfig` 还没有下面这些 import，需要一并补充：

```java
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageHandler;
```

### MQTT 出站适配器配置

本章节说明出站适配器的关键配置。`MqttPahoMessageHandler` 会从 Spring Message 中读取 Topic、QoS、Retained 等头信息；如果消息头没有指定，则使用适配器上的默认配置。Paho 的 `MqttMessage` 本身也包含 payload、QoS、Retained 等发布参数，QoS 只允许取值 `0`、`1`、`2`。([Home](https://docs.spring.vmware.com/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

| 配置项              | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| `clientId`          | 出站发布客户端 ID，不能和入站订阅客户端 ID 重复。            |
| `defaultTopic`      | 默认发布主题，当消息头未设置 `MqttHeaders.TOPIC` 时使用。    |
| `defaultQos`        | 默认 QoS，当消息头未设置 `MqttHeaders.QOS` 时使用。          |
| `defaultRetained`   | 默认 Retained 标识，当消息头未设置 `MqttHeaders.RETAINED` 时使用。 |
| `async`             | 是否异步发送。`false` 时发送线程会等待交付确认；`true` 时不阻塞等待。 |
| `asyncEvents`       | `async=true` 时是否发布 `MqttMessageSentEvent` 和 `MqttMessageDeliveredEvent`。 |
| `completionTimeout` | 异步操作完成超时时间。                                       |
| `converter`         | MQTT 消息转换器，默认使用 `DefaultPahoMessageConverter`。    |

推荐策略：

| 场景         | async             | asyncEvents | 说明                                       |
| ------------ | ----------------- | ----------- | ------------------------------------------ |
| 开发调试     | `false`           | `false`     | 发布失败时更容易在当前调用链暴露异常。     |
| 普通业务发布 | `false`           | `false`     | 调用方能感知发送结果，逻辑简单。           |
| 高吞吐发布   | `true`            | `true`      | 调用方不阻塞，通过事件记录发送和送达状态。 |
| 指令下发     | `false` 或 `true` | `true`      | 根据业务对响应时间和可靠性要求选择。       |

### 消息发送服务封装

本章节封装业务侧使用的 MQTT 发布服务。业务代码不直接依赖 `MqttPahoMessageHandler`，只需要调用 `MqttPublishService` 并传入 Topic、Payload、QoS 和 Retained 标识。

先定义发布请求对象。

文件位置：`src/main/java/io/github/atengk/mqtt/dto/MqttPublishRequest.java`

```java
package io.github.atengk.mqtt.dto;

import lombok.Data;

/**
 * MQTT 发布请求
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class MqttPublishRequest {

    /**
     * 业务消息 ID，可为空；为空时服务端自动生成
     */
    private String messageId;

    /**
     * 发布主题
     */
    private String topic;

    /**
     * 消息内容，可以是 JSON 字符串，也可以是普通对象
     */
    private Object payload;

    /**
     * QoS，取值 0、1、2
     */
    private Integer qos;

    /**
     * 是否保留消息
     */
    private Boolean retained;
}
```

再定义发布结果对象。

文件位置：`src/main/java/io/github/atengk/mqtt/dto/MqttPublishResult.java`

```java
package io.github.atengk.mqtt.dto;

import lombok.Builder;
import lombok.Data;

/**
 * MQTT 发布结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class MqttPublishResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 业务消息 ID
     */
    private String messageId;

    /**
     * 发布主题
     */
    private String topic;

    /**
     * QoS
     */
    private Integer qos;

    /**
     * 是否保留消息
     */
    private Boolean retained;

    /**
     * 结果说明
     */
    private String message;

    /**
     * 创建成功结果
     *
     * @param messageId 业务消息 ID
     * @param topic     主题
     * @param qos       QoS
     * @param retained  是否保留消息
     * @return 发布结果
     */
    public static MqttPublishResult success(String messageId, String topic, Integer qos, Boolean retained) {
        return MqttPublishResult.builder()
                .success(true)
                .messageId(messageId)
                .topic(topic)
                .qos(qos)
                .retained(retained)
                .message("发布成功")
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param messageId 业务消息 ID
     * @param topic     主题
     * @param qos       QoS
     * @param retained  是否保留消息
     * @param message   失败说明
     * @return 发布结果
     */
    public static MqttPublishResult fail(String messageId, String topic, Integer qos, Boolean retained, String message) {
        return MqttPublishResult.builder()
                .success(false)
                .messageId(messageId)
                .topic(topic)
                .qos(qos)
                .retained(retained)
                .message(message)
                .build();
    }
}
```

定义发布服务接口。

文件位置：`src/main/java/io/github/atengk/mqtt/service/MqttPublishService.java`

```java
package io.github.atengk.mqtt.service;

import io.github.atengk.mqtt.dto.MqttPublishRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;

/**
 * MQTT 消息发布服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MqttPublishService {

    /**
     * 发布 MQTT 消息
     *
     * @param request 发布请求
     * @return 发布结果
     */
    MqttPublishResult publish(MqttPublishRequest request);
}
```

下面实现发布服务。这里通过 `MessageChannel` 发送 Spring Message，并在消息头中写入 `MqttHeaders.TOPIC`、`MqttHeaders.QOS`、`MqttHeaders.RETAINED`。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/MqttPublishServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mqtt.dto.MqttPublishRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;
import io.github.atengk.mqtt.properties.MqttProperties;
import io.github.atengk.mqtt.service.MqttPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * MQTT 消息发布服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublishServiceImpl implements MqttPublishService {

    @Qualifier("mqttOutboundChannel")
    private final MessageChannel mqttOutboundChannel;

    private final MqttProperties mqttProperties;

    /**
     * 发布 MQTT 消息
     *
     * @param request 发布请求
     * @return 发布结果
     */
    @Override
    public MqttPublishResult publish(MqttPublishRequest request) {
        validateRequest(request);

        String messageId = CharSequenceUtil.blankToDefault(request.getMessageId(), IdUtil.fastSimpleUUID());
        String topic = request.getTopic();
        int qos = validateQos(request.getQos());
        boolean retained = Boolean.TRUE.equals(request.getRetained());
        String payload = normalizePayload(request.getPayload());

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .setHeader(MqttHeaders.RETAINED, retained)
                .setHeader("messageId", messageId)
                .build();

        try {
            boolean sent = mqttOutboundChannel.send(message, mqttProperties.getOutbound().getSendTimeout());
            if (!sent) {
                log.warn("MQTT消息发布超时，messageId：{}，topic：{}，qos：{}", messageId, topic, qos);
                return MqttPublishResult.fail(messageId, topic, qos, retained, "发布超时");
            }

            log.info("MQTT消息发布成功，messageId：{}，topic：{}，qos：{}，retained：{}，payload：{}",
                    messageId, topic, qos, retained, payload);
            return MqttPublishResult.success(messageId, topic, qos, retained);
        } catch (Exception e) {
            log.error("MQTT消息发布异常，messageId：{}，topic：{}，qos：{}，原因：{}",
                    messageId, topic, qos, e.getMessage(), e);
            return MqttPublishResult.fail(messageId, topic, qos, retained, e.getMessage());
        }
    }

    /**
     * 校验发布请求
     *
     * @param request 发布请求
     */
    private void validateRequest(MqttPublishRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("MQTT 发布请求不能为空");
        }
        if (CharSequenceUtil.isBlank(request.getTopic())) {
            throw new IllegalArgumentException("MQTT 发布 Topic 不能为空");
        }
        if (request.getPayload() == null) {
            throw new IllegalArgumentException("MQTT 发布内容不能为空");
        }
    }

    /**
     * 规范化消息内容
     *
     * @param payload 原始消息内容
     * @return 字符串消息内容
     */
    private String normalizePayload(Object payload) {
        if (payload instanceof CharSequence) {
            String text = Convert.toStr(payload);
            if (CharSequenceUtil.isBlank(text)) {
                throw new IllegalArgumentException("MQTT 发布内容不能为空字符串");
            }
            return text;
        }
        return JSONUtil.toJsonStr(payload);
    }

    /**
     * 校验 QoS
     *
     * @param qos QoS
     * @return 安全 QoS
     */
    private int validateQos(Integer qos) {
        int safeQos = qos == null ? mqttProperties.getOutbound().getDefaultQos() : qos;
        if (safeQos < 0 || safeQos > 2) {
            throw new IllegalArgumentException("MQTT QoS 只能是 0、1、2");
        }
        return safeQos;
    }
}
```

提供一个开发调试接口，用于通过 HTTP 触发 MQTT 发布。

文件位置：`src/main/java/io/github/atengk/mqtt/controller/MqttPublishController.java`

```java
package io.github.atengk.mqtt.controller;

import io.github.atengk.mqtt.dto.MqttPublishRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;
import io.github.atengk.mqtt.service.MqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * MQTT 消息发布控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mqtt/publish")
public class MqttPublishController {

    private final MqttPublishService mqttPublishService;

    /**
     * 发布 MQTT 消息
     *
     * @param request 发布请求
     * @return 发布结果
     */
    @PostMapping
    public MqttPublishResult publish(@RequestBody MqttPublishRequest request) {
        return mqttPublishService.publish(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://127.0.0.1:8080/mqtt/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "device/p001/d10001/command/set",
    "qos": 1,
    "retained": false,
    "payload": {
      "messageId": "cmd-10001",
      "timestamp": 1714961000000,
      "method": "switch.set",
      "params": {
        "switch": true
      }
    }
  }'
```

可以使用 `mosquitto_sub` 订阅命令主题，验证 Spring Boot 是否成功发布消息：

```bash
mosquitto_sub -h 127.0.0.1 -p 1883 -t "device/p001/d10001/command/set" -q 1
```

### 发布结果与异常处理

本章节处理发布链路中的同步异常、通道发送超时、连接失败事件和异步发送事件。Spring Integration MQTT 在异步模式下可以发布 `MqttMessageSentEvent` 和 `MqttMessageDeliveredEvent`，事件中包含 `messageId`、`clientId`、`clientInstance` 等字段，用于关联发送和送达状态；同时连接失败会发布 `MqttConnectionFailedEvent`。([Home](https://docs.spring.vmware.com/spring-integration/reference/mqtt.html?utm_source=chatgpt.com))

创建 MQTT 事件监听器。

文件位置：`src/main/java/io/github/atengk/mqtt/handler/MqttPublishEventHandler.java`

```java
package io.github.atengk.mqtt.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveryEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.stereotype.Component;

/**
 * MQTT 发布事件处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MqttPublishEventHandler {

    /**
     * 处理 MQTT 发布相关事件
     *
     * @param event 发布事件
     */
    @EventListener
    public void handleDeliveryEvent(MqttMessageDeliveryEvent event) {
        if (event instanceof MqttMessageSentEvent sentEvent) {
            log.info("MQTT消息已发送，clientId：{}，clientInstance：{}，mqttMessageId：{}，topic：{}",
                    sentEvent.getClientId(),
                    sentEvent.getClientInstance(),
                    sentEvent.getMessageId(),
                    sentEvent.getTopic());
            return;
        }

        if (event instanceof MqttMessageDeliveredEvent deliveredEvent) {
            log.info("MQTT消息已送达，clientId：{}，clientInstance：{}，mqttMessageId：{}",
                    deliveredEvent.getClientId(),
                    deliveredEvent.getClientInstance(),
                    deliveredEvent.getMessageId());
            return;
        }

        log.info("MQTT发布事件，eventType：{}，clientId：{}，mqttMessageId：{}",
                event.getClass().getSimpleName(),
                event.getClientId(),
                event.getMessageId());
    }

    /**
     * 处理 MQTT 连接失败事件
     *
     * @param event 连接失败事件
     */
    @EventListener
    public void handleConnectionFailedEvent(MqttConnectionFailedEvent event) {
        Throwable cause = event.getCause();
        if (cause == null) {
            log.warn("MQTT连接断开，未返回异常原因");
            return;
        }
        log.error("MQTT连接失败或连接丢失，原因：{}", cause.getMessage(), cause);
    }
}
```

异常处理建议：

| 异常场景       | 处理方式                                                     |
| -------------- | ------------------------------------------------------------ |
| Topic 为空     | 直接参数校验失败，不进入发送通道。                           |
| QoS 非法       | 直接参数校验失败，提示只能为 `0`、`1`、`2`。                 |
| Payload 为空   | 直接参数校验失败，避免发布无意义消息。                       |
| Broker 未连接  | 捕获发送异常，记录日志并返回失败结果。                       |
| 发送超时       | `MessageChannel.send` 返回 `false` 时记录超时。              |
| 异步发送       | 开启 `async-events` 后通过事件记录发送和送达状态。           |
| 业务要求强一致 | 不建议只依赖 MQTT 发布结果，应增加业务回执 Topic，例如 `command/reply`。 |

生产环境建议增加发布记录表，用于记录 `messageId`、`topic`、`payload`、`qos`、`retained`、`sendStatus`、`retryCount`、`createdAt`、`updatedAt`。如果命令下发必须可靠闭环，应由设备端向 `device/{productKey}/{deviceId}/command/reply` 返回回执，平台根据回执更新业务状态。

## 消息模型设计

本章节定义 MQTT 业务消息结构。通信层只负责 Topic、QoS、Retained 和 Payload；业务层需要在 Payload 中定义稳定的 `messageId`、`timestamp`、`messageType`、`action` 和 `data`，以便做幂等、追踪、回执、告警和数据入库。

### 请求消息结构

请求消息用于平台下发命令、设备上报属性、设备上报事件等场景。推荐请求消息统一包含消息 ID、产品标识、设备标识、消息类型、动作、时间戳和业务数据。

通用请求结构：

```json
{
  "messageId": "cmd-10001",
  "productKey": "p001",
  "deviceId": "d10001",
  "messageType": "command",
  "action": "set",
  "timestamp": 1714961000000,
  "data": {
    "method": "switch.set",
    "params": {
      "switch": true
    }
  }
}
```

字段说明：

| 字段          | 类型   | 是否必填 | 说明                                                      |
| ------------- | ------ | -------- | --------------------------------------------------------- |
| `messageId`   | String | 是       | 全局唯一业务消息 ID，用于日志追踪和幂等。                 |
| `productKey`  | String | 是       | 产品标识。                                                |
| `deviceId`    | String | 是       | 设备标识。                                                |
| `messageType` | String | 是       | 消息类型，例如 `property`、`event`、`status`、`command`。 |
| `action`      | String | 是       | 动作，例如 `post`、`set`、`reply`。                       |
| `timestamp`   | Long   | 是       | 消息产生时间戳，单位毫秒。                                |
| `data`        | Object | 是       | 业务数据。                                                |

文件位置：`src/main/java/io/github/atengk/mqtt/model/MqttRequestMessage.java`

```java
package io.github.atengk.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT 请求消息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttRequestMessage<T> {

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 动作
     */
    private String action;

    /**
     * 时间戳，单位毫秒
     */
    private Long timestamp;

    /**
     * 业务数据
     */
    private T data;
}
```

### 响应消息结构

响应消息用于设备返回命令执行结果，或者平台返回业务处理结果。响应消息中应包含原请求消息 ID，便于调用方关联请求和响应。

通用响应结构：

```json
{
  "messageId": "reply-10001",
  "requestMessageId": "cmd-10001",
  "productKey": "p001",
  "deviceId": "d10001",
  "code": 0,
  "message": "success",
  "timestamp": 1714961003000,
  "data": {
    "result": true
  }
}
```

字段说明：

| 字段               | 类型    | 是否必填 | 说明                                    |
| ------------------ | ------- | -------- | --------------------------------------- |
| `messageId`        | String  | 是       | 当前响应消息 ID。                       |
| `requestMessageId` | String  | 是       | 原请求消息 ID。                         |
| `productKey`       | String  | 是       | 产品标识。                              |
| `deviceId`         | String  | 是       | 设备标识。                              |
| `code`             | Integer | 是       | 响应码，`0` 表示成功，非 `0` 表示失败。 |
| `message`          | String  | 是       | 响应说明。                              |
| `timestamp`        | Long    | 是       | 响应时间戳，单位毫秒。                  |
| `data`             | Object  | 否       | 响应数据。                              |

文件位置：`src/main/java/io/github/atengk/mqtt/model/MqttResponseMessage.java`

```java
package io.github.atengk.mqtt.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT 响应消息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttResponseMessage<T> {

    /**
     * 响应消息 ID
     */
    private String messageId;

    /**
     * 请求消息 ID
     */
    private String requestMessageId;

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 响应码，0 表示成功
     */
    private Integer code;

    /**
     * 响应说明
     */
    private String message;

    /**
     * 时间戳，单位毫秒
     */
    private Long timestamp;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 创建成功响应
     *
     * @param requestMessageId 请求消息 ID
     * @param productKey       产品标识
     * @param deviceId         设备标识
     * @param data             响应数据
     * @return 响应消息
     */
    public static <T> MqttResponseMessage<T> success(String requestMessageId, String productKey, String deviceId, T data) {
        return MqttResponseMessage.<T>builder()
                .messageId(IdUtil.fastSimpleUUID())
                .requestMessageId(requestMessageId)
                .productKey(productKey)
                .deviceId(deviceId)
                .code(0)
                .message("success")
                .timestamp(DateUtil.current())
                .data(data)
                .build();
    }

    /**
     * 创建失败响应
     *
     * @param requestMessageId 请求消息 ID
     * @param productKey       产品标识
     * @param deviceId         设备标识
     * @param code             响应码
     * @param message          响应说明
     * @return 响应消息
     */
    public static <T> MqttResponseMessage<T> fail(String requestMessageId, String productKey, String deviceId,
                                                  Integer code, String message) {
        return MqttResponseMessage.<T>builder()
                .messageId(IdUtil.fastSimpleUUID())
                .requestMessageId(requestMessageId)
                .productKey(productKey)
                .deviceId(deviceId)
                .code(code)
                .message(message)
                .timestamp(DateUtil.current())
                .build();
    }
}
```

### 业务消息类型

本章节定义业务消息类型枚举。枚举用于统一 Topic 动作、消息类型和业务含义，避免在代码中散落硬编码字符串。

文件位置：`src/main/java/io/github/atengk/mqtt/enums/MqttMessageType.java`

```java
package io.github.atengk.mqtt.enums;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQTT 业务消息类型
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@AllArgsConstructor
public enum MqttMessageType {

    /**
     * 属性上报
     */
    PROPERTY_POST("property", "post", "属性上报"),

    /**
     * 事件上报
     */
    EVENT_POST("event", "post", "事件上报"),

    /**
     * 状态上报
     */
    STATUS_POST("status", "post", "状态上报"),

    /**
     * 命令下发
     */
    COMMAND_SET("command", "set", "命令下发"),

    /**
     * 命令回执
     */
    COMMAND_REPLY("command", "reply", "命令回执");

    /**
     * 消息类型
     */
    private final String messageType;

    /**
     * 动作
     */
    private final String action;

    /**
     * 描述
     */
    private final String description;

    /**
     * 根据消息类型和动作匹配枚举
     *
     * @param messageType 消息类型
     * @param action      动作
     * @return 消息类型枚举
     */
    public static MqttMessageType match(String messageType, String action) {
        for (MqttMessageType item : values()) {
            if (CharSequenceUtil.equals(item.getMessageType(), messageType)
                    && CharSequenceUtil.equals(item.getAction(), action)) {
                return item;
            }
        }
        return null;
    }
}
```

推荐 Topic 和消息类型的对应关系：

| 业务类型 | Topic                                          | messageType | action  |
| -------- | ---------------------------------------------- | ----------- | ------- |
| 属性上报 | `device/{productKey}/{deviceId}/property/post` | `property`  | `post`  |
| 事件上报 | `device/{productKey}/{deviceId}/event/post`    | `event`     | `post`  |
| 状态上报 | `device/{productKey}/{deviceId}/status/post`   | `status`    | `post`  |
| 命令下发 | `device/{productKey}/{deviceId}/command/set`   | `command`   | `set`   |
| 命令回执 | `device/{productKey}/{deviceId}/command/reply` | `command`   | `reply` |

### 消息序列化与反序列化

本章节封装 MQTT Payload 的 JSON 序列化与反序列化。MQTT Payload 在 Paho 中本质上是 `byte[]`，Spring Integration 的默认转换器会在常见场景中处理字符串和字节数组转换；业务层建议统一使用 JSON 字符串作为 Payload，便于调试、日志记录和跨语言接入。([eclipse.dev](https://eclipse.dev/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttMessage.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mqtt/util/MqttMessageCodec.java`

```java
package io.github.atengk.mqtt.util;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;

/**
 * MQTT 消息编解码工具
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class MqttMessageCodec {

    private MqttMessageCodec() {
    }

    /**
     * 序列化对象为 JSON 字符串
     *
     * @param message 消息对象
     * @return JSON 字符串
     */
    public static String toJson(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("MQTT 消息对象不能为空");
        }
        return JSONUtil.toJsonStr(message);
    }

    /**
     * 反序列化 JSON 字符串为对象
     *
     * @param payload 消息内容
     * @param clazz   目标类型
     * @return 目标对象
     */
    public static <T> T fromJson(String payload, Class<T> clazz) {
        if (CharSequenceUtil.isBlank(payload)) {
            throw new IllegalArgumentException("MQTT 消息内容不能为空");
        }
        if (!JSONUtil.isTypeJSON(payload)) {
            throw new IllegalArgumentException("MQTT 消息内容不是合法 JSON");
        }
        return JSONUtil.toBean(payload, clazz);
    }

    /**
     * 判断 Payload 是否为 JSON
     *
     * @param payload 消息内容
     * @return 是否 JSON
     */
    public static boolean isJson(String payload) {
        return CharSequenceUtil.isNotBlank(payload) && JSONUtil.isTypeJSON(payload);
    }
}
```

使用消息模型发布设备命令示例：

```java
MqttRequestMessage<Object> command = MqttRequestMessage.builder()
        .messageId(IdUtil.fastSimpleUUID())
        .productKey("p001")
        .deviceId("d10001")
        .messageType("command")
        .action("set")
        .timestamp(DateUtil.current())
        .data(Dict.create()
                .set("method", "switch.set")
                .set("params", Dict.create().set("switch", true)))
        .build();

MqttPublishRequest request = new MqttPublishRequest();
request.setTopic("device/p001/d10001/command/set");
request.setQos(1);
request.setRetained(false);
request.setPayload(MqttMessageCodec.toJson(command));

MqttPublishResult result = mqttPublishService.publish(request);
```

消息模型设计注意事项：

1. `messageId` 必须全局唯一，建议服务端统一生成，设备端上报时也必须携带。
2. QoS 1 和 QoS 2 不能替代业务幂等，业务层仍应基于 `messageId` 防重复处理。
3. `timestamp` 使用毫秒时间戳，避免跨语言解析日期格式不一致。
4. Payload 建议统一 JSON，不建议混用纯文本、二进制和 JSON，除非 Topic 已明确区分数据类型。
5. 命令下发必须设计回执 Topic，发布成功只表示消息进入 MQTT 发送链路，不等于设备已经执行成功。
6. Retained 消息适合保存设备最新状态或配置，不适合保存一次性命令，避免新上线设备误执行旧命令。

## 业务集成

本章节说明 MQTT 通信层如何接入实际业务。前面章节已经完成订阅、发布、消息模型和 JSON 编解码，本章节重点将设备上报、服务端指令下发、消息幂等、离线与重连处理串起来，形成可落地的业务闭环。

### 设备上报消息处理

设备上报消息通常包括属性上报、事件上报、状态上报和命令回执。服务端收到 MQTT 消息后，不建议直接在入站处理器中写复杂业务逻辑，而是先解析 Topic 和 Payload，再分发到专门的业务处理器。

推荐处理流程如下：

```text
设备发布 MQTT 消息
  ↓
MqttPahoMessageDrivenChannelAdapter 接收消息
  ↓
mqttInputChannel 入站通道
  ↓
MqttInboundMessageHandler 读取 Topic 和 Payload
  ↓
DeviceMqttMessageService 分发业务类型
  ↓
设备属性、事件、状态、回执等业务处理器
  ↓
幂等校验、数据入库、告警触发、状态更新
```

为了让业务层更清晰，可以先定义设备上报解析结果对象。

文件位置：`src/main/java/io/github/atengk/mqtt/model/DeviceReportMessage.java`

```java
package io.github.atengk.mqtt.model;

import cn.hutool.json.JSONObject;
import lombok.Builder;
import lombok.Data;

/**
 * 设备上报消息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class DeviceReportMessage {

    /**
     * 原始主题
     */
    private String topic;

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 动作
     */
    private String action;

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * QoS
     */
    private Integer qos;

    /**
     * 是否保留消息
     */
    private Boolean retained;

    /**
     * 原始消息内容
     */
    private String payload;

    /**
     * JSON 消息体
     */
    private JSONObject body;
}
```

定义设备上报业务服务接口。

文件位置：`src/main/java/io/github/atengk/mqtt/service/DeviceReportService.java`

```java
package io.github.atengk.mqtt.service;

import io.github.atengk.mqtt.model.DeviceReportMessage;

/**
 * 设备上报业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DeviceReportService {

    /**
     * 处理属性上报
     *
     * @param message 设备上报消息
     */
    void handlePropertyPost(DeviceReportMessage message);

    /**
     * 处理事件上报
     *
     * @param message 设备上报消息
     */
    void handleEventPost(DeviceReportMessage message);

    /**
     * 处理状态上报
     *
     * @param message 设备上报消息
     */
    void handleStatusPost(DeviceReportMessage message);

    /**
     * 处理命令回执
     *
     * @param message 设备上报消息
     */
    void handleCommandReply(DeviceReportMessage message);
}
```

下面是设备上报业务服务实现。示例中只输出关键日志，实际项目中可以在对应方法中接入数据库、规则引擎、WebSocket、告警系统或时序数据库。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/DeviceReportServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import io.github.atengk.mqtt.model.DeviceReportMessage;
import io.github.atengk.mqtt.service.DeviceReportService;
import io.github.atengk.mqtt.service.MqttIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备上报业务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceReportServiceImpl implements DeviceReportService {

    private final MqttIdempotentService mqttIdempotentService;

    /**
     * 处理属性上报
     *
     * @param message 设备上报消息
     */
    @Override
    public void handlePropertyPost(DeviceReportMessage message) {
        if (!mqttIdempotentService.markProcessing(message.getMessageId())) {
            log.warn("忽略重复属性上报消息，messageId：{}，topic：{}", message.getMessageId(), message.getTopic());
            return;
        }

        JSONObject properties = message.getBody().getJSONObject("properties");
        if (ObjUtil.isNull(properties) || properties.isEmpty()) {
            log.warn("设备属性上报内容为空，productKey：{}，deviceId：{}，messageId：{}",
                    message.getProductKey(), message.getDeviceId(), message.getMessageId());
            return;
        }

        log.info("保存设备属性上报，productKey：{}，deviceId：{}，messageId：{}，properties：{}",
                message.getProductKey(), message.getDeviceId(), message.getMessageId(), properties);

        mqttIdempotentService.markSuccess(message.getMessageId());
    }

    /**
     * 处理事件上报
     *
     * @param message 设备上报消息
     */
    @Override
    public void handleEventPost(DeviceReportMessage message) {
        if (!mqttIdempotentService.markProcessing(message.getMessageId())) {
            log.warn("忽略重复事件上报消息，messageId：{}，topic：{}", message.getMessageId(), message.getTopic());
            return;
        }

        String eventCode = message.getBody().getStr("eventCode");
        JSONObject data = message.getBody().getJSONObject("data");

        log.info("处理设备事件上报，productKey：{}，deviceId：{}，messageId：{}，eventCode：{}，data：{}",
                message.getProductKey(), message.getDeviceId(), message.getMessageId(), eventCode, data);

        mqttIdempotentService.markSuccess(message.getMessageId());
    }

    /**
     * 处理状态上报
     *
     * @param message 设备上报消息
     */
    @Override
    public void handleStatusPost(DeviceReportMessage message) {
        if (!mqttIdempotentService.markProcessing(message.getMessageId())) {
            log.warn("忽略重复状态上报消息，messageId：{}，topic：{}", message.getMessageId(), message.getTopic());
            return;
        }

        String status = message.getBody().getStr("status");
        log.info("更新设备在线状态，productKey：{}，deviceId：{}，messageId：{}，status：{}",
                message.getProductKey(), message.getDeviceId(), message.getMessageId(), status);

        mqttIdempotentService.markSuccess(message.getMessageId());
    }

    /**
     * 处理命令回执
     *
     * @param message 设备上报消息
     */
    @Override
    public void handleCommandReply(DeviceReportMessage message) {
        if (!mqttIdempotentService.markProcessing(message.getMessageId())) {
            log.warn("忽略重复命令回执消息，messageId：{}，topic：{}", message.getMessageId(), message.getTopic());
            return;
        }

        String requestMessageId = message.getBody().getStr("requestMessageId");
        Integer code = message.getBody().getInt("code");
        String resultMessage = message.getBody().getStr("message");

        log.info("更新设备命令回执，productKey：{}，deviceId：{}，messageId：{}，requestMessageId：{}，code：{}，message：{}",
                message.getProductKey(), message.getDeviceId(), message.getMessageId(), requestMessageId, code, resultMessage);

        mqttIdempotentService.markSuccess(message.getMessageId());
    }
}
```

然后调整前面章节中的 `DeviceMqttMessageServiceImpl`，将原本的日志处理改为业务分发。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/DeviceMqttMessageServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mqtt.enums.MqttMessageType;
import io.github.atengk.mqtt.model.DeviceReportMessage;
import io.github.atengk.mqtt.service.DeviceMqttMessageService;
import io.github.atengk.mqtt.service.DeviceReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备 MQTT 消息服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMqttMessageServiceImpl implements DeviceMqttMessageService {

    private final DeviceReportService deviceReportService;

    /**
     * 处理 MQTT 消息
     *
     * @param topic    主题
     * @param payload  消息内容
     * @param qos      QoS
     * @param retained 是否保留消息
     */
    @Override
    public void handleMessage(String topic, String payload, Integer qos, Boolean retained) {
        if (CharSequenceUtil.isBlank(payload) || !JSONUtil.isTypeJSON(payload)) {
            log.warn("MQTT消息内容不是合法JSON，topic：{}，payload：{}", topic, payload);
            return;
        }

        List<String> topicLevels = CharSequenceUtil.split(topic, '/');
        if (CollUtil.size(topicLevels) < 5) {
            log.warn("MQTT Topic格式不符合规范，topic：{}", topic);
            return;
        }

        String domain = topicLevels.get(0);
        String productKey = topicLevels.get(1);
        String deviceId = topicLevels.get(2);
        String messageType = topicLevels.get(3);
        String action = topicLevels.get(4);

        if (!CharSequenceUtil.equals("device", domain)) {
            log.warn("忽略非设备域MQTT消息，topic：{}", topic);
            return;
        }

        JSONObject body = JSONUtil.parseObj(payload);
        String messageId = body.getStr("messageId");
        if (CharSequenceUtil.isBlank(messageId)) {
            log.warn("MQTT消息缺少messageId，topic：{}，payload：{}", topic, payload);
            return;
        }

        DeviceReportMessage reportMessage = DeviceReportMessage.builder()
                .topic(topic)
                .productKey(productKey)
                .deviceId(deviceId)
                .messageType(messageType)
                .action(action)
                .messageId(messageId)
                .qos(qos)
                .retained(retained)
                .payload(payload)
                .body(body)
                .build();

        MqttMessageType type = MqttMessageType.match(messageType, action);
        if (type == null) {
            log.warn("未匹配到MQTT业务类型，topic：{}，messageType：{}，action：{}", topic, messageType, action);
            return;
        }

        switch (type) {
            case PROPERTY_POST -> deviceReportService.handlePropertyPost(reportMessage);
            case EVENT_POST -> deviceReportService.handleEventPost(reportMessage);
            case STATUS_POST -> deviceReportService.handleStatusPost(reportMessage);
            case COMMAND_REPLY -> deviceReportService.handleCommandReply(reportMessage);
            default -> log.warn("当前业务类型不属于服务端入站处理范围，topic：{}，type：{}", topic, type);
        }
    }
}
```

### 服务端指令下发

服务端指令下发用于平台控制设备，例如开关控制、参数设置、模式切换和升级通知。指令下发不应只关注 MQTT 发布成功，还应设计命令回执 Topic，由设备执行后返回结果，服务端再更新命令状态。

推荐流程如下：

```text
业务系统发起设备命令
  ↓
生成 command messageId
  ↓
保存命令记录，状态为 SENDING
  ↓
发布到 device/{productKey}/{deviceId}/command/set
  ↓
设备执行命令
  ↓
设备发布回执到 device/{productKey}/{deviceId}/command/reply
  ↓
服务端更新命令状态为 SUCCESS 或 FAILED
```

定义设备命令请求对象。

文件位置：`src/main/java/io/github/atengk/mqtt/dto/DeviceCommandRequest.java`

```java
package io.github.atengk.mqtt.dto;

import lombok.Data;

import java.util.Map;

/**
 * 设备命令请求
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class DeviceCommandRequest {

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 命令方法
     */
    private String method;

    /**
     * 命令参数
     */
    private Map<String, Object> params;

    /**
     * QoS
     */
    private Integer qos;
}
```

定义设备命令服务接口。

文件位置：`src/main/java/io/github/atengk/mqtt/service/DeviceCommandService.java`

```java
package io.github.atengk.mqtt.service;

import io.github.atengk.mqtt.dto.DeviceCommandRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;

/**
 * 设备命令服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DeviceCommandService {

    /**
     * 下发设备命令
     *
     * @param request 设备命令请求
     * @return 发布结果
     */
    MqttPublishResult sendCommand(DeviceCommandRequest request);
}
```

下面封装命令下发实现。示例中使用 `MqttRequestMessage` 统一包装消息体，再调用前面章节的 `MqttPublishService` 发送。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/DeviceCommandServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.mqtt.dto.DeviceCommandRequest;
import io.github.atengk.mqtt.dto.MqttPublishRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;
import io.github.atengk.mqtt.model.MqttRequestMessage;
import io.github.atengk.mqtt.service.DeviceCommandService;
import io.github.atengk.mqtt.service.MqttPublishService;
import io.github.atengk.mqtt.util.MqttMessageCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备命令服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final MqttPublishService mqttPublishService;

    /**
     * 下发设备命令
     *
     * @param request 设备命令请求
     * @return 发布结果
     */
    @Override
    public MqttPublishResult sendCommand(DeviceCommandRequest request) {
        validateRequest(request);

        String messageId = IdUtil.fastSimpleUUID();
        String topic = CharSequenceUtil.format(
                "device/{}/{}/command/set",
                request.getProductKey(),
                request.getDeviceId()
        );

        MqttRequestMessage<Object> commandMessage = MqttRequestMessage.builder()
                .messageId(messageId)
                .productKey(request.getProductKey())
                .deviceId(request.getDeviceId())
                .messageType("command")
                .action("set")
                .timestamp(DateUtil.current())
                .data(Dict.create()
                        .set("method", request.getMethod())
                        .set("params", request.getParams()))
                .build();

        MqttPublishRequest publishRequest = new MqttPublishRequest();
        publishRequest.setMessageId(messageId);
        publishRequest.setTopic(topic);
        publishRequest.setQos(request.getQos());
        publishRequest.setRetained(false);
        publishRequest.setPayload(MqttMessageCodec.toJson(commandMessage));

        log.info("准备下发设备命令，messageId：{}，productKey：{}，deviceId：{}，method：{}",
                messageId, request.getProductKey(), request.getDeviceId(), request.getMethod());

        return mqttPublishService.publish(publishRequest);
    }

    /**
     * 校验设备命令请求
     *
     * @param request 设备命令请求
     */
    private void validateRequest(DeviceCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("设备命令请求不能为空");
        }
        if (CharSequenceUtil.isBlank(request.getProductKey())) {
            throw new IllegalArgumentException("产品标识不能为空");
        }
        if (CharSequenceUtil.isBlank(request.getDeviceId())) {
            throw new IllegalArgumentException("设备标识不能为空");
        }
        if (CharSequenceUtil.isBlank(request.getMethod())) {
            throw new IllegalArgumentException("命令方法不能为空");
        }
    }
}
```

提供一个用于调试的命令下发接口。

文件位置：`src/main/java/io/github/atengk/mqtt/controller/DeviceCommandController.java`

```java
package io.github.atengk.mqtt.controller;

import io.github.atengk.mqtt.dto.DeviceCommandRequest;
import io.github.atengk.mqtt.dto.MqttPublishResult;
import io.github.atengk.mqtt.service.DeviceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备命令控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/device/commands")
public class DeviceCommandController {

    private final DeviceCommandService deviceCommandService;

    /**
     * 下发设备命令
     *
     * @param request 设备命令请求
     * @return 发布结果
     */
    @PostMapping("/send")
    public MqttPublishResult sendCommand(@RequestBody DeviceCommandRequest request) {
        return deviceCommandService.sendCommand(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://127.0.0.1:8080/device/commands/send" \
  -H "Content-Type: application/json" \
  -d '{
    "productKey": "p001",
    "deviceId": "d10001",
    "method": "switch.set",
    "params": {
      "switch": true
    },
    "qos": 1
  }'
```

### 消息幂等处理

MQTT QoS 1 表示“至少一次”，业务侧可能收到重复消息。服务端处理设备上报、事件告警、命令回执时，必须基于 `messageId` 做幂等，避免重复入库、重复告警、重复扣减库存或重复执行状态变更。

开发环境可以使用内存幂等实现，生产环境建议使用 Redis、数据库唯一索引或消息记录表。下面给出内存实现，便于文档示例直接运行。

定义幂等服务接口。

文件位置：`src/main/java/io/github/atengk/mqtt/service/MqttIdempotentService.java`

```java
package io.github.atengk.mqtt.service;

/**
 * MQTT 消息幂等服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MqttIdempotentService {

    /**
     * 标记消息处理中
     *
     * @param messageId 消息 ID
     * @return 是否允许继续处理
     */
    boolean markProcessing(String messageId);

    /**
     * 标记消息处理成功
     *
     * @param messageId 消息 ID
     */
    void markSuccess(String messageId);

    /**
     * 标记消息处理失败
     *
     * @param messageId 消息 ID
     */
    void markFailed(String messageId);
}
```

下面是内存幂等实现。该实现适合单实例开发调试，不适合多实例生产环境。

文件位置：`src/main/java/io/github/atengk/mqtt/service/impl/MemoryMqttIdempotentServiceImpl.java`

```java
package io.github.atengk.mqtt.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.mqtt.service.MqttIdempotentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 MQTT 消息幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class MemoryMqttIdempotentServiceImpl implements MqttIdempotentService {

    private static final String PROCESSING = "PROCESSING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final Map<String, String> messageStatusMap = new ConcurrentHashMap<>();

    /**
     * 标记消息处理中
     *
     * @param messageId 消息 ID
     * @return 是否允许继续处理
     */
    @Override
    public boolean markProcessing(String messageId) {
        if (CharSequenceUtil.isBlank(messageId)) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        String previousStatus = messageStatusMap.putIfAbsent(messageId, PROCESSING);
        if (previousStatus == null) {
            log.info("MQTT消息进入处理流程，messageId：{}，time：{}", messageId, DateUtil.now());
            return true;
        }

        if (CharSequenceUtil.equals(FAILED, previousStatus)) {
            messageStatusMap.put(messageId, PROCESSING);
            log.info("MQTT失败消息重新处理，messageId：{}", messageId);
            return true;
        }

        log.warn("MQTT消息重复投递，messageId：{}，status：{}", messageId, previousStatus);
        return false;
    }

    /**
     * 标记消息处理成功
     *
     * @param messageId 消息 ID
     */
    @Override
    public void markSuccess(String messageId) {
        messageStatusMap.put(messageId, SUCCESS);
        log.info("MQTT消息处理成功，messageId：{}", messageId);
    }

    /**
     * 标记消息处理失败
     *
     * @param messageId 消息 ID
     */
    @Override
    public void markFailed(String messageId) {
        messageStatusMap.put(messageId, FAILED);
        log.warn("MQTT消息处理失败，messageId：{}", messageId);
    }
}
```

生产环境幂等建议：

| 方案           | 适用场景           | 说明                                                        |
| -------------- | ------------------ | ----------------------------------------------------------- |
| Redis `SETNX`  | 高并发、短周期幂等 | 设置过期时间，适合消息重复窗口较短的场景。                  |
| 数据库唯一索引 | 强一致入库         | 对 `message_id` 建唯一索引，重复插入直接失败。              |
| 消息记录表     | 需要追踪状态       | 记录 `PROCESSING`、`SUCCESS`、`FAILED`、`RETRYING` 等状态。 |
| 业务唯一键     | 业务天然唯一       | 例如设备状态使用 `deviceId + timestamp + status`。          |

如果使用数据库唯一索引，建议至少包含以下字段：

```sql
CREATE TABLE mqtt_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    topic VARCHAR(255) NOT NULL COMMENT 'MQTT主题',
    product_key VARCHAR(64) COMMENT '产品标识',
    device_id VARCHAR(64) COMMENT '设备标识',
    message_type VARCHAR(32) COMMENT '消息类型',
    action VARCHAR(32) COMMENT '动作',
    payload TEXT COMMENT '消息内容',
    status VARCHAR(32) NOT NULL COMMENT '处理状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1024) COMMENT '异常信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_device_time (product_key, device_id, created_at),
    KEY idx_status (status)
) COMMENT='MQTT消息处理记录表';
```

### 离线与重连处理

离线与重连处理包括客户端连接失败、Broker 不可用、网络抖动、订阅恢复、设备上下线事件等场景。Paho 的自动重连可以解决客户端到 Broker 的连接恢复，但不能替代业务层的设备在线状态判断和消息补偿机制。

处理建议如下：

| 场景                   | 处理策略                                                     |
| ---------------------- | ------------------------------------------------------------ |
| 服务端 MQTT 客户端断线 | 开启 `automatic-reconnect`，监听连接失败事件，记录告警日志。 |
| Broker 重启            | 依赖 Paho 自动重连；重连成功后确认订阅是否恢复。             |
| 设备离线               | 通过设备状态上报、遗嘱消息或 Broker 事件判断。               |
| 指令下发给离线设备     | 保存命令记录，等待设备上线后补偿，或返回设备离线。           |
| `cleanSession=false`   | 适合需要 Broker 保留订阅和离线消息的场景，但要控制离线堆积。 |
| 动态订阅               | 应用重启后需要从数据库或配置中心重新加载动态 Topic。         |

可以在前面章节的事件监听器中扩展连接异常日志。如果需要记录当前连接状态，可以增加一个简单的状态组件。

文件位置：`src/main/java/io/github/atengk/mqtt/support/MqttConnectionState.java`

```java
package io.github.atengk.mqtt.support;

import cn.hutool.core.date.DateUtil;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * MQTT 连接状态
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
public class MqttConnectionState {

    /**
     * 是否连接异常
     */
    private volatile boolean connectionFailed = false;

    /**
     * 最后异常时间
     */
    private volatile String lastFailedTime;

    /**
     * 最后异常原因
     */
    private volatile String lastFailedReason;

    /**
     * 标记连接失败
     *
     * @param reason 失败原因
     */
    public void markFailed(String reason) {
        this.connectionFailed = true;
        this.lastFailedTime = DateUtil.now();
        this.lastFailedReason = reason;
    }

    /**
     * 标记连接正常
     */
    public void markHealthy() {
        this.connectionFailed = false;
        this.lastFailedTime = null;
        this.lastFailedReason = null;
    }
}
```

在 `MqttPublishEventHandler` 中注入并使用该状态组件：

```java
private final MqttConnectionState mqttConnectionState;

@EventListener
public void handleConnectionFailedEvent(MqttConnectionFailedEvent event) {
    Throwable cause = event.getCause();
    String reason = cause == null ? "未知原因" : cause.getMessage();
    mqttConnectionState.markFailed(reason);
    log.error("MQTT连接失败或连接丢失，原因：{}", reason, cause);
}
```

## 配置管理

本章节说明 MQTT 配置如何在 Spring Boot 中统一管理。开发环境可以把 Topic、Broker 地址和客户端参数放在 `application.yml` 中；生产环境建议通过环境变量、配置中心或数据库外置化管理，避免频繁改包发布。

### application.yml 配置

下面给出完整的 MQTT 配置示例，可以合并前面章节中的入站和出站配置。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-mqtt-demo

mqtt:
  # Broker 地址列表
  server-uris:
    - tcp://127.0.0.1:1883

  # MQTT 用户名，生产环境建议使用环境变量注入
  username: ${MQTT_USERNAME:}

  # MQTT 密码，生产环境建议使用环境变量注入
  password: ${MQTT_PASSWORD:}

  # true：重连后不保留会话；false：Broker 保留会话和离线消息
  clean-session: true

  # 开启 Paho 自动重连
  automatic-reconnect: true

  # 连接超时时间，单位：秒
  connection-timeout: 30

  # 心跳间隔，单位：秒
  keep-alive-interval: 60

  # 最大未确认消息数量
  max-inflight: 100

  inbound:
    # 入站订阅客户端 ID，需要全局唯一
    client-id: ${spring.application.name}-inbound-${random.uuid}

    # 入站订阅主题
    topics:
      - device/+/property/post
      - device/+/event/post
      - device/+/status/post
      - device/+/command/reply

    # 默认订阅 QoS
    qos: 1

    # 入站消息通道完成超时时间，单位：毫秒
    completion-timeout: 5000

    # 连接恢复间隔，单位：毫秒
    recovery-interval: 10000

    # 默认关闭手动确认
    manual-acks: false

  outbound:
    # 出站发布客户端 ID，需要全局唯一
    client-id: ${spring.application.name}-outbound-${random.uuid}

    # 默认发布主题
    default-topic: device/default/default/command/set

    # 默认发布 QoS
    default-qos: 1

    # 默认不保留消息
    default-retained: false

    # 默认同步发布，便于调用方感知异常
    async: false

    # 异步模式下发布发送和送达事件
    async-events: true

    # 出站适配器完成超时时间，单位：毫秒
    completion-timeout: 5000

    # MessageChannel 发送超时时间，单位：毫秒
    send-timeout: 3000
```

### 多环境配置

多环境配置用于区分开发、测试、预发和生产环境。不同环境通常使用不同的 Broker 地址、认证信息、Topic 前缀和客户端 ID 规则。

推荐配置文件结构：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
```

主配置文件只指定激活环境和公共配置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    active: dev

  application:
    name: springboot3-mqtt-demo
```

开发环境配置：

文件位置：`src/main/resources/application-dev.yml`

```yaml
mqtt:
  server-uris:
    - tcp://127.0.0.1:1883
  username:
  password:
  clean-session: true
  automatic-reconnect: true

  inbound:
    client-id: ${spring.application.name}-dev-inbound-${random.uuid}
    topics:
      - device/+/property/post
      - device/+/event/post
      - device/+/status/post
      - device/+/command/reply
    qos: 1

  outbound:
    client-id: ${spring.application.name}-dev-outbound-${random.uuid}
    default-qos: 1
    async: false
```

生产环境配置：

文件位置：`src/main/resources/application-prod.yml`

```yaml
mqtt:
  server-uris:
    - ${MQTT_SERVER_URI_1}
    - ${MQTT_SERVER_URI_2}
  username: ${MQTT_USERNAME}
  password: ${MQTT_PASSWORD}

  # 生产环境是否保留会话需要结合 Broker 离线消息策略确认
  clean-session: false
  automatic-reconnect: true
  connection-timeout: 30
  keep-alive-interval: 60
  max-inflight: 1000

  inbound:
    client-id: ${spring.application.name}-prod-inbound-${HOSTNAME:${random.uuid}}
    topics:
      - device/+/property/post
      - device/+/event/post
      - device/+/status/post
      - device/+/command/reply
    qos: 1
    completion-timeout: 5000
    recovery-interval: 10000

  outbound:
    client-id: ${spring.application.name}-prod-outbound-${HOSTNAME:${random.uuid}}
    default-topic: device/default/default/command/set
    default-qos: 1
    default-retained: false
    async: true
    async-events: true
    completion-timeout: 5000
    send-timeout: 3000
```

启动时指定环境：

```bash
# 使用 dev 环境启动
java -jar springboot3-mqtt-demo.jar --spring.profiles.active=dev

# 使用 prod 环境启动
java -jar springboot3-mqtt-demo.jar --spring.profiles.active=prod
```

### Topic 配置外置化

Topic 配置外置化适用于 Topic 经常变化、设备产品线动态增加、多租户隔离、后台管理页面维护订阅规则等场景。外置化方式可以是配置中心、数据库或 Redis。简单项目使用 `application.yml` 即可，复杂项目建议将 Topic 作为业务数据管理。

推荐外置化表结构：

```sql
CREATE TABLE mqtt_topic_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    topic VARCHAR(255) NOT NULL COMMENT 'MQTT主题',
    direction VARCHAR(16) NOT NULL COMMENT '方向：INBOUND/OUTBOUND',
    qos INT NOT NULL DEFAULT 1 COMMENT 'QoS',
    retained TINYINT NOT NULL DEFAULT 0 COMMENT '是否保留消息',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_topic_direction (topic, direction),
    KEY idx_enabled_direction (enabled, direction)
) COMMENT='MQTT主题配置表';
```

外置化管理建议：

| 配置项         | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| 初始订阅 Topic | 启动时从配置中心或数据库加载。                               |
| 动态新增 Topic | 调用 `MqttPahoMessageDrivenChannelAdapter.addTopic` 增加订阅。 |
| 动态移除 Topic | 调用 `removeTopic` 移除订阅。                                |
| 多实例同步     | 使用配置中心推送、MQ 广播或定时刷新。                        |
| Topic 权限     | 后台页面限制 `#` 和敏感 Topic，避免越权订阅。                |
| 发布 Topic     | 不建议完全由前端传入，应由服务端根据产品、设备和业务类型拼接。 |

如果 Topic 暂时只放在配置文件中，可以按业务域拆分得更清晰：

```yaml
mqtt:
  topic:
    device:
      property-post: device/+/property/post
      event-post: device/+/event/post
      status-post: device/+/status/post
      command-reply: device/+/command/reply
      command-set-template: device/{productKey}/{deviceId}/command/set
```

## 日志与异常处理

本章节定义 MQTT 关键链路日志和异常处理方式。MQTT 是长连接消息通信，排查问题时需要关注连接状态、订阅主题、发布结果、消息 ID、Topic、设备 ID、业务处理结果和异常堆栈。

### 连接异常处理

连接异常主要包括 Broker 不可达、认证失败、网络中断、客户端 ID 冲突、心跳超时和 TLS 配置错误。服务端应监听连接失败事件，并输出足够的排障信息。

连接异常日志建议包含以下字段：

| 字段         | 说明             |
| ------------ | ---------------- |
| `clientId`   | MQTT 客户端 ID。 |
| `serverUris` | Broker 地址。    |
| `eventType`  | 事件类型。       |
| `reason`     | 异常原因。       |
| `time`       | 发生时间。       |

连接异常处理器可以继续使用前面章节的 `MqttPublishEventHandler`，并扩展入站和出站连接异常日志：

```java
@EventListener
public void handleConnectionFailedEvent(MqttConnectionFailedEvent event) {
    Throwable cause = event.getCause();
    String reason = cause == null ? "未知原因" : cause.getMessage();

    log.error("MQTT连接失败，eventType：{}，reason：{}",
            event.getClass().getSimpleName(),
            reason,
            cause);
}
```

常见问题定位：

| 现象                  | 可能原因                 | 处理方式                                         |
| --------------------- | ------------------------ | ------------------------------------------------ |
| 启动后无法连接 Broker | 地址、端口、网络不通     | 检查 `server-uris`、防火墙、Docker 端口映射。    |
| 认证失败              | 用户名或密码错误         | 检查 Broker 用户配置和环境变量。                 |
| 频繁掉线              | 心跳配置不合理、网络抖动 | 调整 `keep-alive-interval`，检查网络链路。       |
| 客户端被踢下线        | clientId 重复            | 确保入站和出站 clientId 全局唯一。               |
| 重连后收不到消息      | Topic 未恢复或权限不足   | 检查订阅日志、Broker ACL 和动态 Topic 加载逻辑。 |

### 消息处理异常处理

消息处理异常包括 Payload 非 JSON、缺少 `messageId`、Topic 不符合规范、业务处理失败、数据库异常、幂等状态异常等。异常处理原则是：通信层不吞异常，业务层记录明确原因；可重试异常进入失败状态，不可重试异常直接记录并丢弃或进入死信处理。

建议定义统一的 MQTT 业务异常。

文件位置：`src/main/java/io/github/atengk/mqtt/exception/MqttBusinessException.java`

```java
package io.github.atengk.mqtt.exception;

/**
 * MQTT 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class MqttBusinessException extends RuntimeException {

    /**
     * 创建 MQTT 业务异常
     *
     * @param message 异常信息
     */
    public MqttBusinessException(String message) {
        super(message);
    }

    /**
     * 创建 MQTT 业务异常
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public MqttBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

在入站处理器中，可以对业务异常和未知异常做区分处理：

```java
@ServiceActivator(inputChannel = "mqttInputChannel")
public void handleMessage(Message<?> message) {
    String topic = Convert.toStr(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
    Integer qos = Convert.toInt(message.getHeaders().get(MqttHeaders.RECEIVED_QOS), 0);
    Boolean retained = Convert.toBool(message.getHeaders().get(MqttHeaders.RECEIVED_RETAINED), false);
    String payload = Convert.toStr(message.getPayload(), CharSequenceUtil.EMPTY);

    try {
        log.info("收到MQTT消息，topic：{}，qos：{}，retained：{}，payload：{}", topic, qos, retained, payload);
        deviceMqttMessageService.handleMessage(topic, payload, qos, retained);
    } catch (MqttBusinessException e) {
        log.warn("MQTT业务处理失败，topic：{}，reason：{}，payload：{}", topic, e.getMessage(), payload);
    } catch (Exception e) {
        log.error("MQTT消息处理异常，topic：{}，payload：{}，reason：{}", topic, payload, e.getMessage(), e);
        throw e;
    }
}
```

异常处理建议：

| 异常类型        | 建议处理                                   |
| --------------- | ------------------------------------------ |
| 非法 Topic      | 记录 warn 日志，丢弃消息。                 |
| 非 JSON Payload | 记录 warn 日志，丢弃消息。                 |
| 缺少 messageId  | 记录 warn 日志，丢弃消息或进入异常消息表。 |
| 重复 messageId  | 记录 warn 日志，跳过处理。                 |
| 数据库异常      | 标记消息失败，后续重试。                   |
| 业务校验失败    | 记录失败原因，不建议无限重试。             |
| 未知系统异常    | 记录 error 日志，必要时告警。              |

### 关键链路日志

关键链路日志用于串联消息从接收到处理、从发布到回执的全过程。日志必须包含 `messageId`、`topic`、`productKey`、`deviceId`、`messageType`、`action`、`qos` 等关键字段，便于生产排障。

推荐日志点如下：

| 链路              | 日志级别   | 日志内容                                                  |
| ----------------- | ---------- | --------------------------------------------------------- |
| MQTT 客户端初始化 | info       | Broker 数量、clientId、cleanSession、automaticReconnect。 |
| 入站适配器初始化  | info       | clientId、topics、qos。                                   |
| 收到消息          | info       | topic、qos、retained、payload。                           |
| Topic 解析失败    | warn       | topic、payload、失败原因。                                |
| Payload 校验失败  | warn       | topic、payload、失败原因。                                |
| 幂等重复          | warn       | messageId、topic、当前状态。                              |
| 业务处理成功      | info       | messageId、productKey、deviceId、业务类型。               |
| 业务处理失败      | warn/error | messageId、topic、异常原因。                              |
| 准备发布消息      | info       | messageId、topic、qos、retained。                         |
| 发布成功          | info       | messageId、topic、qos、retained。                         |
| 发布失败          | error      | messageId、topic、异常原因。                              |
| 收到命令回执      | info       | messageId、requestMessageId、code、message。              |

日志示例：

```text
初始化MQTT连接参数，Broker数量：1，cleanSession：true，自动重连：true
初始化MQTT入站适配器，clientId：springboot3-mqtt-demo-inbound-xxx，topics：["device/+/property/post"]，qos：1
收到MQTT消息，topic：device/p001/d10001/property/post，qos：1，retained：false，payload：{...}
MQTT消息进入处理流程，messageId：msg-10001，time：2026-05-06 10:00:00
保存设备属性上报，productKey：p001，deviceId：d10001，messageId：msg-10001，properties：{...}
MQTT消息处理成功，messageId：msg-10001
准备下发设备命令，messageId：cmd-10001，productKey：p001，deviceId：d10001，method：switch.set
MQTT消息发布成功，messageId：cmd-10001，topic：device/p001/d10001/command/set，qos：1，retained：false
更新设备命令回执，productKey：p001，deviceId：d10001，messageId：reply-10001，requestMessageId：cmd-10001，code：0，message：success
```

生产日志建议：

1. `payload` 内容较大时不要完整打印，可以只打印摘要、长度或关键字段。
2. 涉及密码、Token、密钥、用户隐私的数据必须脱敏。
3. `messageId` 应贯穿发布、接收、处理、回执全过程。
4. 高频遥测数据可以降低日志级别，避免日志量压垮磁盘。
5. 错误日志必须包含异常堆栈，业务失败日志必须包含失败原因。
6. 如果系统接入链路追踪，可以将 `messageId` 写入 MDC，统一关联日志。

## 测试与验证

本章节用于验证 MQTT Broker、Spring Boot 入站订阅、出站发布、重连恢复等核心链路是否正常。建议先验证 Broker，再验证订阅，最后验证发布和重连，避免问题混在一起难以定位。

### 本地 Broker 测试

本地 Broker 测试用于确认 MQTT 服务端口、发布订阅链路和基础网络连通性。开发环境推荐使用 EMQX 或 Mosquitto，前面章节已经给出 Docker 启动方式，这里重点验证 Broker 是否可正常收发消息。

先启动 EMQX：

```bash
# 启动 EMQX Broker
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8084:8084 \
  -p 8883:8883 \
  -p 18083:18083 \
  emqx/emqx:latest

# 查看容器运行状态
docker ps | grep emqx

# 查看 EMQX 日志
docker logs -f emqx
```

`1883` 是 MQTT TCP 端口，Spring Boot 应用通过 `tcp://127.0.0.1:1883` 连接；`18083` 是 EMQX Dashboard 端口，可用于查看客户端连接、订阅主题和消息统计。

使用命令行验证发布订阅链路：

```bash
# 终端 1：订阅测试主题
mosquitto_sub -h 127.0.0.1 -p 1883 -t "demo/test" -q 1

# 终端 2：发布测试消息
mosquitto_pub -h 127.0.0.1 -p 1883 -t "demo/test" -q 1 -m "hello mqtt"
```

如果终端 1 能收到 `hello mqtt`，说明 Broker 的基础发布订阅能力正常。若收不到消息，优先检查容器是否运行、`1883` 端口是否映射、系统防火墙是否拦截。

### 消息订阅验证

消息订阅验证用于确认 Spring Boot 应用能否连接 Broker、订阅 Topic，并将设备上报消息转发到 `MqttInboundMessageHandler` 和业务服务。

启动 Spring Boot 应用：

```bash
# 编译项目
mvn clean compile

# 使用 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，控制台应能看到类似日志：

```text
初始化MQTT连接参数，Broker数量：1，cleanSession：true，自动重连：true
初始化MQTT入站适配器，clientId：springboot3-mqtt-demo-dev-inbound-xxx，topics：[...]，qos：1
```

验证属性上报消息：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/property/post" \
  -q 1 \
  -m '{"messageId":"msg-property-10001","timestamp":1714960000000,"properties":{"temperature":26.5,"humidity":61}}'
```

预期应用日志：

```text
收到MQTT消息，topic：device/p001/d10001/property/post，qos：1，retained：false，payload：{...}
MQTT消息进入处理流程，messageId：msg-property-10001
保存设备属性上报，productKey：p001，deviceId：d10001，messageId：msg-property-10001，properties：{...}
MQTT消息处理成功，messageId：msg-property-10001
```

验证事件上报消息：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/event/post" \
  -q 1 \
  -m '{"messageId":"msg-event-10001","timestamp":1714960001000,"eventCode":"over_temperature","data":{"temperature":85.2}}'
```

验证状态上报消息：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/status/post" \
  -q 1 \
  -m '{"messageId":"msg-status-10001","timestamp":1714960002000,"status":"online"}'
```

验证命令回执消息：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/command/reply" \
  -q 1 \
  -m '{"messageId":"reply-10001","requestMessageId":"cmd-10001","timestamp":1714960003000,"code":0,"message":"success","data":{"result":true}}'
```

如果没有收到应用日志，应检查以下配置：

| 检查项                | 说明                                    |
| --------------------- | --------------------------------------- |
| `mqtt.server-uris`    | 是否指向当前 Broker 地址。              |
| `mqtt.inbound.topics` | 是否包含对应 Topic 或通配符。           |
| `client-id`           | 是否和其他客户端重复。                  |
| Broker ACL            | 是否允许当前客户端订阅对应 Topic。      |
| Payload               | 是否为合法 JSON，是否包含 `messageId`。 |

### 消息发布验证

消息发布验证用于确认 Spring Boot 应用能否通过 `MqttPublishService` 或 HTTP 调试接口向 Broker 发布消息。验证方式是先用命令行工具订阅目标 Topic，再调用 Spring Boot 发布接口。

先订阅命令下发 Topic：

```bash
mosquitto_sub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/command/set" \
  -q 1
```

调用通用 MQTT 发布接口：

```bash
curl -X POST "http://127.0.0.1:8080/mqtt/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "device/p001/d10001/command/set",
    "qos": 1,
    "retained": false,
    "payload": {
      "messageId": "cmd-publish-10001",
      "productKey": "p001",
      "deviceId": "d10001",
      "messageType": "command",
      "action": "set",
      "timestamp": 1714961000000,
      "data": {
        "method": "switch.set",
        "params": {
          "switch": true
        }
      }
    }
  }'
```

调用设备命令下发接口：

```bash
curl -X POST "http://127.0.0.1:8080/device/commands/send" \
  -H "Content-Type: application/json" \
  -d '{
    "productKey": "p001",
    "deviceId": "d10001",
    "method": "switch.set",
    "params": {
      "switch": true
    },
    "qos": 1
  }'
```

预期响应：

```json
{
  "success": true,
  "messageId": "生成的消息ID",
  "topic": "device/p001/d10001/command/set",
  "qos": 1,
  "retained": false,
  "message": "发布成功"
}
```

预期订阅终端可以收到服务端发布的 JSON 消息。如果接口返回成功但订阅终端未收到，应检查发布 Topic 和订阅 Topic 是否完全匹配，QoS 是否符合 Broker 权限策略，以及出站 `client-id` 是否被其他客户端占用。

### 重连场景验证

重连场景验证用于确认 Broker 重启、网络短暂中断后，Spring Boot MQTT 客户端是否能自动恢复连接并继续收发消息。开发环境可以通过停止和启动 Broker 容器模拟故障。

先确认配置开启自动重连：

```yaml
mqtt:
  automatic-reconnect: true
  inbound:
    recovery-interval: 10000
```

模拟 Broker 故障：

```bash
# 停止 Broker
docker stop emqx

# 观察 Spring Boot 应用日志
# 预期看到 MQTT 连接失败、连接丢失或重连相关日志
```

恢复 Broker：

```bash
# 启动 Broker
docker start emqx

# 查看 Broker 是否恢复
docker ps | grep emqx
```

Broker 恢复后，再发布一条设备上报消息：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -t "device/p001/d10001/status/post" \
  -q 1 \
  -m '{"messageId":"msg-reconnect-10001","timestamp":1714962000000,"status":"online"}'
```

预期 Spring Boot 应用能重新收到消息。如果没有恢复，重点检查：

| 检查项                | 说明                                      |
| --------------------- | ----------------------------------------- |
| `automatic-reconnect` | 是否开启 Paho 自动重连。                  |
| `recovery-interval`   | 入站适配器恢复间隔是否配置合理。          |
| `client-id`           | Broker 重启后是否存在客户端 ID 冲突。     |
| `clean-session`       | 是否影响会话和订阅恢复策略。              |
| 动态 Topic            | 动态订阅的 Topic 是否需要重启后重新加载。 |

## 项目结构建议

本章节给出 Spring Boot 3 MQTT 项目的推荐目录结构。目录划分目标是让配置、消息模型、消息处理、业务服务、控制器和工具类职责清晰，避免所有 MQTT 逻辑集中在一个配置类或处理器中。

推荐完整结构如下：

```text
src/main/java/io/github/atengk/mqtt
├── MqttApplication.java
├── config
│   └── MqttIntegrationConfig.java
├── controller
│   ├── DeviceCommandController.java
│   ├── MqttPublishController.java
│   └── MqttTopicController.java
├── dto
│   ├── DeviceCommandRequest.java
│   ├── MqttPublishRequest.java
│   ├── MqttPublishResult.java
│   └── MqttTopicRequest.java
├── enums
│   └── MqttMessageType.java
├── exception
│   └── MqttBusinessException.java
├── handler
│   ├── MqttInboundMessageHandler.java
│   └── MqttPublishEventHandler.java
├── model
│   ├── DeviceReportMessage.java
│   ├── MqttRequestMessage.java
│   └── MqttResponseMessage.java
├── properties
│   └── MqttProperties.java
├── service
│   ├── DeviceCommandService.java
│   ├── DeviceMqttMessageService.java
│   ├── DeviceReportService.java
│   ├── MqttIdempotentService.java
│   ├── MqttPublishService.java
│   ├── MqttTopicService.java
│   └── impl
│       ├── DeviceCommandServiceImpl.java
│       ├── DeviceMqttMessageServiceImpl.java
│       ├── DeviceReportServiceImpl.java
│       ├── MemoryMqttIdempotentServiceImpl.java
│       ├── MqttPublishServiceImpl.java
│       └── MqttTopicServiceImpl.java
├── support
│   └── MqttConnectionState.java
└── util
    └── MqttMessageCodec.java
```

### 配置类目录

配置类目录用于放置 MQTT 连接工厂、入站适配器、出站适配器、消息通道和配置属性绑定相关代码。

推荐目录：

```text
config
└── MqttIntegrationConfig.java

properties
└── MqttProperties.java
```

职责说明：

| 文件                         | 职责                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| `MqttIntegrationConfig.java` | 定义 `MqttConnectOptions`、`MqttPahoClientFactory`、入站通道、出站通道、入站适配器、出站适配器。 |
| `MqttProperties.java`        | 绑定 `application.yml` 中的 `mqtt` 配置。                    |

配置类只处理框架集成，不建议写设备业务逻辑、消息解析逻辑或数据库操作。这样可以避免配置类膨胀，也便于后续替换 Broker、调整 Topic 和扩展 TLS。

### 消息处理目录

消息处理目录用于承接 Spring Integration 入站消息和 MQTT 发布事件。该目录只做通信层处理，例如读取消息头、解析基础字段、记录连接异常、监听发布事件，不直接承载复杂业务规则。

推荐目录：

```text
handler
├── MqttInboundMessageHandler.java
└── MqttPublishEventHandler.java
```

职责说明：

| 文件                             | 职责                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| `MqttInboundMessageHandler.java` | 从 `mqttInputChannel` 接收消息，读取 Topic、QoS、Retained、Payload，转发给业务服务。 |
| `MqttPublishEventHandler.java`   | 监听连接失败、消息发送、消息送达等事件。                     |

处理建议：

1. 入站处理器不直接操作数据库，避免通信层和业务层耦合。
2. 入站处理器应记录 `topic`、`qos`、`retained` 和 `payload` 摘要。
3. 发布事件处理器应记录 `clientId`、`messageId` 和异常原因。
4. 高频消息不建议完整打印 Payload，可以按业务需要打印关键字段。

### 消息模型目录

消息模型目录用于放置 MQTT 业务消息结构、上报解析结构、请求响应结构和枚举。消息模型应保持稳定，避免直接暴露底层 Paho 或 Spring Integration 类型。

推荐目录：

```text
model
├── DeviceReportMessage.java
├── MqttRequestMessage.java
└── MqttResponseMessage.java

dto
├── DeviceCommandRequest.java
├── MqttPublishRequest.java
├── MqttPublishResult.java
└── MqttTopicRequest.java

enums
└── MqttMessageType.java
```

职责说明：

| 类型    | 说明                                            |
| ------- | ----------------------------------------------- |
| `model` | MQTT 业务消息模型，适合服务内部流转。           |
| `dto`   | HTTP 接口、服务调用和控制器使用的请求响应对象。 |
| `enums` | 消息类型、动作、状态、命令类型等枚举。          |

模型设计建议：

1. MQTT Payload 统一使用 JSON，便于跨语言设备接入。
2. 所有业务消息必须包含 `messageId`。
3. 上报消息和响应消息应包含 `timestamp`。
4. 设备命令必须包含可追踪的原始请求 ID 和回执 ID。
5. 枚举值应与 Topic 中的 `messageType` 和 `action` 保持一致。

### 业务服务目录

业务服务目录用于承载设备上报、命令下发、幂等处理、动态订阅、消息发布等业务逻辑。该层可以访问数据库、Redis、规则引擎、告警服务和其他业务系统。

推荐目录：

```text
service
├── DeviceCommandService.java
├── DeviceMqttMessageService.java
├── DeviceReportService.java
├── MqttIdempotentService.java
├── MqttPublishService.java
├── MqttTopicService.java
└── impl
    ├── DeviceCommandServiceImpl.java
    ├── DeviceMqttMessageServiceImpl.java
    ├── DeviceReportServiceImpl.java
    ├── MemoryMqttIdempotentServiceImpl.java
    ├── MqttPublishServiceImpl.java
    └── MqttTopicServiceImpl.java
```

职责说明：

| 服务                       | 职责                                       |
| -------------------------- | ------------------------------------------ |
| `DeviceMqttMessageService` | 统一分发设备侧入站消息。                   |
| `DeviceReportService`      | 处理属性、事件、状态、命令回执等业务消息。 |
| `DeviceCommandService`     | 封装服务端命令下发。                       |
| `MqttPublishService`       | 封装 MQTT 出站发布能力。                   |
| `MqttTopicService`         | 管理动态订阅 Topic。                       |
| `MqttIdempotentService`    | 处理消息幂等。                             |

业务服务层可以继续拆分为更细的处理器，例如 `PropertyReportHandler`、`EventReportHandler`、`StatusReportHandler`、`CommandReplyHandler`。当消息类型持续增加时，推荐使用策略模式替代大段 `switch` 分发。

## 开发注意事项

本章节整理 Spring Boot 3 集成 MQTT 时容易出现的问题，包括客户端 ID、QoS、连接保活、重复消费、Retained 消息和 Topic 权限等。开发阶段这些问题可能不明显，生产环境消息量上来后会直接影响稳定性。

### ClientId 唯一性

`clientId` 是 MQTT 客户端在 Broker 侧的唯一标识。同一个 Broker 下，如果两个客户端使用相同 `clientId` 连接，通常会导致旧连接被新连接踢掉，表现为服务频繁断线、重复重连、订阅不稳定。

推荐规则：

| 场景       | clientId 示例                                                |
| ---------- | ------------------------------------------------------------ |
| 本地开发   | `springboot3-mqtt-demo-dev-inbound-${random.uuid}`           |
| 测试环境   | `springboot3-mqtt-demo-test-inbound-${random.uuid}`          |
| 生产单实例 | `springboot3-mqtt-demo-prod-inbound-${HOSTNAME}`             |
| 生产多实例 | `springboot3-mqtt-demo-prod-inbound-${HOSTNAME}-${random.uuid}` |
| 出站发布   | `springboot3-mqtt-demo-prod-outbound-${HOSTNAME}-${random.uuid}` |

注意事项：

1. 入站订阅客户端和出站发布客户端不要使用同一个 `clientId`。
2. 多实例部署时，每个实例必须有独立 `clientId`。
3. 如果 `cleanSession=false`，`clientId` 更要稳定且唯一，否则 Broker 会话状态会混乱。
4. 如果服务频繁出现连接断开，优先排查 `clientId` 是否冲突。
5. 不建议写死生产环境 `clientId`，应组合应用名、环境、主机名、实例 ID 或随机值。

### QoS 与性能权衡

QoS 决定 MQTT 消息交付质量。QoS 越高，协议交互越复杂，可靠性越强，但吞吐性能和延迟表现会下降。业务设计时不能盲目全部使用 QoS 2。

推荐策略：

| 消息类型       | 推荐 QoS | 原因                                         |
| -------------- | -------- | -------------------------------------------- |
| 高频遥测数据   | 0        | 数据量大，允许少量丢失。                     |
| 属性上报       | 1        | 需要可靠到达，但可以通过幂等处理重复。       |
| 事件告警       | 1        | 告警消息不应轻易丢失。                       |
| 状态上报       | 0 或 1   | 普通在线状态可用 QoS 0，关键状态可用 QoS 1。 |
| 命令下发       | 1        | 需要可靠下发，同时等待设备回执确认执行结果。 |
| 命令回执       | 1        | 服务端需要收到执行结果。                     |
| 强一致关键消息 | 2        | 只在确有必要时使用。                         |

开发建议：

1. 默认业务消息使用 QoS 1。
2. QoS 1 必须配合 `messageId` 幂等处理。
3. 高频数据不建议使用 QoS 2。
4. QoS 2 不能替代业务事务一致性。
5. 服务端发布成功不代表设备执行成功，必须通过回执 Topic 确认。

### 连接保活配置

连接保活用于检测客户端和 Broker 之间的长连接是否仍然有效。保活配置过短会增加网络和 Broker 压力，过长会导致断线检测不及时。

推荐配置：

```yaml
mqtt:
  # 连接超时时间，单位：秒
  connection-timeout: 30

  # 心跳间隔，单位：秒
  keep-alive-interval: 60

  # 开启自动重连
  automatic-reconnect: true

  inbound:
    # 入站适配器连接恢复间隔，单位：毫秒
    recovery-interval: 10000
```

配置建议：

| 参数                  | 建议值                 | 说明                                     |
| --------------------- | ---------------------- | ---------------------------------------- |
| `connection-timeout`  | `10` 到 `30` 秒        | Broker 连接超时时间。                    |
| `keep-alive-interval` | `30` 到 `120` 秒       | 常规服务端应用可使用 `60` 秒。           |
| `automatic-reconnect` | `true`                 | 生产环境建议开启。                       |
| `recovery-interval`   | `5000` 到 `30000` 毫秒 | 不建议太短，避免 Broker 故障时频繁重连。 |
| `max-inflight`        | 按吞吐调整             | 高并发发布场景可适当调大。               |

注意事项：

1. Broker、负载均衡、防火墙和 NAT 网关可能会影响长连接稳定性。
2. 如果网络链路中存在空闲连接回收，应适当缩短 `keep-alive-interval`。
3. 如果 Broker 压力较大，不要把所有客户端心跳设置得过短。
4. 重连恢复后要关注动态 Topic 是否需要重新加载。
5. 如果启用 TLS，还需要关注证书过期、域名匹配和握手超时。

### 消息重复消费处理

消息重复消费主要出现在 QoS 1、客户端重连、Broker 重试、业务处理超时、应用实例重启等场景。服务端必须假设同一个 `messageId` 可能被处理多次，并在业务入口做幂等控制。

推荐处理流程：

```text
收到 MQTT 消息
  ↓
解析 messageId
  ↓
检查幂等记录
  ↓
不存在：标记 PROCESSING，继续处理
  ↓
已成功：跳过重复消息
  ↓
处理中：按策略跳过或稍后重试
  ↓
失败：允许重新处理或进入人工补偿
  ↓
处理成功后标记 SUCCESS
```

幂等状态建议：

| 状态         | 说明                                 |
| ------------ | ------------------------------------ |
| `PROCESSING` | 消息正在处理。                       |
| `SUCCESS`    | 消息已经处理成功，重复消息直接跳过。 |
| `FAILED`     | 消息处理失败，可按策略重试。         |
| `IGNORED`    | 消息非法或业务明确忽略。             |

Redis 幂等示例思路：

```bash
# 使用 SET NX 标记消息正在处理，并设置过期时间
SET mqtt:message:msg-10001 PROCESSING NX EX 86400

# 处理成功后更新状态
SET mqtt:message:msg-10001 SUCCESS EX 86400

# 查询消息状态
GET mqtt:message:msg-10001
```

数据库幂等示例思路：

```sql
-- message_id 建唯一索引，重复插入会失败
INSERT INTO mqtt_message_record (
    message_id,
    topic,
    product_key,
    device_id,
    message_type,
    action,
    payload,
    status,
    retry_count,
    created_at,
    updated_at
) VALUES (
    'msg-10001',
    'device/p001/d10001/property/post',
    'p001',
    'd10001',
    'property',
    'post',
    '{"messageId":"msg-10001"}',
    'PROCESSING',
    0,
    NOW(),
    NOW()
);
```

重复消费处理建议：

1. 所有设备上报、命令回执和关键事件必须包含 `messageId`。
2. 业务处理入口先做幂等校验，再执行数据库写入、告警触发、状态更新等操作。
3. 数据库写入类场景优先使用唯一索引兜底。
4. Redis 幂等需要设置合理过期时间，避免 key 无限增长。
5. 失败消息不要无限重试，应限制次数并记录失败原因。
6. 对于命令下发场景，发布消息和设备回执都应分别做幂等。
