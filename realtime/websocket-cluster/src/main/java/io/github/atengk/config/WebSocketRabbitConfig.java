package io.github.atengk.config;

import io.github.atengk.constants.WebSocketMqConstants;
import io.github.atengk.util.NodeIdUtil;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketRabbitConfig {

    @Bean
    public DirectExchange wsExchange() {
        return new DirectExchange(WebSocketMqConstants.EXCHANGE_WS_BROADCAST, true, false);
    }

    @Bean
    public Queue wsBroadcastQueue() {
        return new Queue(
                WebSocketMqConstants.QUEUE_WS_BROADCAST + NodeIdUtil.getNodeId(),
                true
        );
    }

    @Bean
    public Binding wsBroadcastBinding(
            Queue wsBroadcastQueue,
            DirectExchange wsExchange
    ) {
        return BindingBuilder
                .bind(wsBroadcastQueue)
                .to(wsExchange)
                .with(WebSocketMqConstants.ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper =
                new DefaultJackson2JavaTypeMapper();

        typeMapper.setTrustedPackages(
                "io.github.atengk.entity"
        );

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

}