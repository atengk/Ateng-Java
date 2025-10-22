package local.ateng.java.oa;

import org.dromara.oa.api.OaSender;
import org.dromara.oa.comm.entity.Request;
import org.dromara.oa.comm.enums.MessageType;
import org.dromara.oa.core.dingTalk.config.DingTalkConfig;
import org.dromara.oa.core.provider.factory.OaFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
public class OADingTalkTests {

    /**
     * DingTalk的Text测试
     */
    @Test
    public void oaDingTalkText() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("dingTalk");

        Request request = new Request();
        ArrayList<String> phones = new ArrayList<>();
        phones.add("17623062936");
        // 支持通过手机号@
        request.setPhoneList(phones);
        // 支持@all
        // request.setIsNoticeAll(true);
        request.setContent("info: 测试消息");

        alarm.sender(request, MessageType.DING_TALK_TEXT);
    }

    /**
     * DingTalk的Markdown测试
     */
    @Test
    public void oaDingTalkMarkdown() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("dingTalk");

        Request request = new Request();
        // 支持@all
        request.setIsNoticeAll(true);
        request.setContent("#### 杭州天气 @150XXXXXXXX \n > 9度，西北风1级，空气良89，相对温度73%\n > ![screenshot](https://img.alicdn.com/tfs/TB1NwmBEL9TBuNjy1zbXXXpepXa-2400-1218.png)\n > ###### 10点20分发布 [天气](https://www.dingtalk.com) \n");
        request.setTitle("info:标题");
        alarm.sender(request, MessageType.DING_TALK_MARKDOWN);

    }

    /**
     * DingTalk的Link测试
     */
    @Test
    public void oaDingTalkLink() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("dingTalk");

        Request request = new Request();
        request.setContent("这个即将发布的新版本，创始人xx称它为红树林。而在此之前，每当面临重大升级，产品经理们都会取一个应景的代号，这一次，为什么是红树林");
        request.setTitle("info:点击跳转到钉钉");
        request.setMessageUrl("https://www.dingtalk.com/s?__biz=MzA4NjMwMTA2Ng==&mid=2650316842&idx=1&sn=60da3ea2b29f1dcc43a7c8e4a7c97a16&scene=2&srcid=09189AnRJEdIiWVaKltFzNTw&from=timeline&isappinstalled=0&key=&ascene=2&uin=&devicetype=android-23&version=26031933&nettype=WIFI");
        request.setPicUrl("https://img.alicdn.com/tfs/TB1NwmBEL9TBuNjy1zbXXXpepXa-2400-1218.png");

        alarm.sender(request, MessageType.DING_TALK_LINK);

    }

    /**
     * DingTalk的动态Text测试
     */
    @Test
    public void oaDynamicDingTalkText() {
        String key = "oaDingTalk";
        DingTalkConfig dingTalkConfig = new DingTalkConfig();
        dingTalkConfig.setConfigId(key);
        dingTalkConfig.setSign("SEC9dbf5a36f16940ede57fd870b2fcb61c61c0b5fb66bb666b7c0c8828ca0f8551");
        dingTalkConfig.setTokenId("1c58688af20ba408585bbb51997532e421b377093f7e876f2ef88333eaabb291");

        // 根据配置创建服务实例并注册
        OaFactory.createAndRegisterOaSender(dingTalkConfig);
        OaSender alarm = OaFactory.getSmsOaBlend(key);

        Request request = new Request();
        ArrayList<String> phones = new ArrayList<>();
        phones.add("17623062936");
        // 支持通过手机号@
        request.setPhoneList(phones);
        // 支持@all
        // request.setIsNoticeAll(true);
        request.setContent("info: 测试消息");

        alarm.sender(request, MessageType.DING_TALK_TEXT);
    }

}
