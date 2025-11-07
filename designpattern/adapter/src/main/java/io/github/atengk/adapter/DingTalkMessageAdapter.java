package io.github.atengk.adapter;


import io.github.atengk.service.MessageSender;
import io.github.atengk.thirdparty.DingTalkApi;
import org.springframework.stereotype.Component;

/**
 * 钉钉消息适配器，实现系统统一接口
 */
@Component("dingTalkAdapter")
public class DingTalkMessageAdapter implements MessageSender {

    private final DingTalkApi dingTalkApi = new DingTalkApi();

    @Override
    public void sendMessage(String userId, String content) {
        // 调用第三方接口
        dingTalkApi.sendMsg(userId, content);
    }
}
