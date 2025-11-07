package io.github.atengk.thirdparty;

/**
 * 模拟第三方微信SDK
 */
public class WeChatApi {
    public void pushToUser(String openId, String text) {
        System.out.println("【WeChat】推送给用户：" + openId + "，内容：" + text);
    }
}
