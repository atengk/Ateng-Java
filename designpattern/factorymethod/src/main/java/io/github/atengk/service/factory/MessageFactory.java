package io.github.atengk.service.factory;

import io.github.atengk.service.product.Message;

/**
 * 消息工厂接口
 */
public interface MessageFactory {

    /**
     * 创建消息对象
     *
     * @return Message 实例
     */
    Message createMessage();
}