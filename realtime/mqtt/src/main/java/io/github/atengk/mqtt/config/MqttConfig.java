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
