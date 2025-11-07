package io.github.atengk.adapter;


import io.github.atengk.service.MessageSender;
import io.github.atengk.thirdparty.WeChatApi;
import org.springframework.stereotype.Component;

/**
 * 微信消息适配器，实现系统统一接口
 */
@Component("weChatAdapter")
public class WeChatMessageAdapter implements MessageSender {

    private final WeChatApi weChatApi = new WeChatApi();

    @Override
    public void sendMessage(String userId, String content) {
        // 调用第三方接口
        weChatApi.pushToUser(userId, content);
    }
}
