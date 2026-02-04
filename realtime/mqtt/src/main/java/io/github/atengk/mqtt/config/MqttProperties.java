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
