package io.github.atengk.thirdparty;

/**
 * 模拟第三方钉钉SDK
 */
public class DingTalkApi {
    public void sendMsg(String userCode, String msg) {
        System.out.println("【DingTalk】发送给用户：" + userCode + "，消息：" + msg);
    }
}