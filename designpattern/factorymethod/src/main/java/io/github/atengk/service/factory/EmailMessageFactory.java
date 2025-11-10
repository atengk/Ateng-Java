package io.github.atengk.service.factory;

import io.github.atengk.service.product.EmailMessage;
import io.github.atengk.service.product.Message;
import org.springframework.stereotype.Service;

/**
 * 邮件消息工厂
 */
@Service
public class EmailMessageFactory implements MessageFactory {

    @Override
    public Message createMessage() {
        return new EmailMessage();
    }
}