package io.github.atengk.service.decorator;

import io.github.atengk.service.MessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 重试增强装饰器
 * 在发送失败时进行重试
 */
@Component
public class RetryMessageDecorator extends AbstractMessageSenderDecorator {

    public RetryMessageDecorator(@Qualifier("defaultMessageSender") MessageSender delegate) {
        super(delegate);
    }

    @Override
    public void send(String message) {
        int retries = 3;
        while (retries-- > 0) {
            try {
                super.send(message);
                System.out.println("【重试机制】发送成功。");
                return;
            } catch (Exception e) {
                System.out.println("【重试机制】发送失败，剩余重试次数：" + retries);
                if (retries == 0) {
                    System.out.println("【重试机制】最终发送失败！");
                }
            }
        }
    }
}

