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
