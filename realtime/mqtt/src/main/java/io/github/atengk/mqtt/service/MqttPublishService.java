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

