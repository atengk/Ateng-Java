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
