package io.github.atengk.service.factory;

import io.github.atengk.service.product.Message;
import io.github.atengk.service.product.SmsMessage;
import org.springframework.stereotype.Service;

/**
 * 短信消息工厂
 */
@Service
public class SmsMessageFactory implements MessageFactory {

    @Override
    public Message createMessage() {
        return new SmsMessage();
    }
}
