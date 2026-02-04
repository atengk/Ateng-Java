# MQTT

**MQTT** 是一种轻量级的发布/订阅消息协议，基于 TCP/IP 运行，专为低带宽、不稳定网络环境设计。它通过主题（Topic）实现消息解耦，具备低开销、实时性强、可靠性可配置等特点，广泛用于物联网、移动应用和实时消息场景。

**EMQX** 是一款高性能、分布式的 MQTT 消息服务器，支持百万级并发连接和高吞吐消息处理。它提供集群部署、规则引擎、认证鉴权、数据桥接等能力，常用于构建大规模物联网和实时数据平台。

- [服务端EMQX安装文档](https://atengk.github.io/ops/#/work/docker/service/emqx/)



## 基础配置

**添加依赖**

```xml
<!-- MQTT 支持（基于 Eclipse Paho） -->
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-mqtt</artifactId>
</dependency>

<!-- MQTT Client 实现 -->
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
```

**配置 application.yml**

```yaml
server:
  port: 18009
spring:
  application:
    name: ${project.artifactId}
---
# MQTT 配置
mqtt:
  broker: tcp://192.168.1.12:1883
  client-id: springboot3-mqtt-client
  username: admin
  password: public
  timeout: 10
  keep-alive: 20
  default-topic: test/topic
```

## 配置文件

**配置属性类**

```java
package io.github.atengk.mqtt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String broker;
    private String clientId;
    private String username;
    private String password;
    private int timeout;
    private int keepAlive;
    private String defaultTopic;
}
```

**配置 mqtt 配置文件**

```java
package io.github.atengk.mqtt.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {

    /* ========= 公共 Channel ========= */

    /**
     * MQTT 入站消息通道
     */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 出站消息通道
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /* ========= MQTT Client 工厂 ========= */

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties properties) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getBroker()});
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setConnectionTimeout(properties.getTimeout());
        options.setKeepAliveInterval(properties.getKeepAlive());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        factory.setConnectionOptions(options);
        return factory;
    }

    /* ========= 入站（订阅） ========= */

    @Bean
    public MessageProducer mqttInbound(
            MqttPahoClientFactory factory,
            MqttProperties properties
    ) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        properties.getClientId() + "_in",
                        factory,
                        "test/#"
                );

        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());

        return adapter;
    }

    /* ========= 出站（发布） ========= */

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound(
            MqttPahoClientFactory factory,
            MqttProperties properties
    ) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(
                        properties.getClientId() + "_out",
                        factory
                );

        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultTopic(properties.getDefaultTopic());

        return handler;
    }
}
```

## 消息接收处理

```java
package io.github.atengk.mqtt.handler;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageHandler {

    /**
     * MQTT 消息统一入口
     */
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<String> message) {
        String payload = message.getPayload();
        Object topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        System.out.println("【MQTT 接收】Topic=" + topic + "，消息=" + payload);

        // TODO 业务处理
    }
}
```

## 消息发送服务

```java
package io.github.atengk.mqtt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MqttPublishService {

    private final MessageChannel mqttOutboundChannel;
    private final MessageChannel mqttInboundChannel;

    public void publish(String topic, String payload) {

        // 发给 Broker
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build()
        );

        // 本地模拟一条“接收消息”
        mqttInboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
                        .build()
        );
    }
}
```

## 测试接口

```java
package io.github.atengk.mqtt.controller;

import io.github.atengk.mqtt.service.MqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final MqttPublishService mqttPublishService;

    @GetMapping("/send")
    public String send() {
        mqttPublishService.publish("test/hello", "Hello MQTT from Spring Boot 3");
        return "OK";
    }
}
```

GET: http://localhost:18009/send

日志输出：

【MQTT 接收】Topic=test/hello，消息=Hello MQTT from Spring Boot 3


