package local.ateng.java.oa;

import org.dromara.oa.api.OaSender;
import org.dromara.oa.comm.entity.Request;
import org.dromara.oa.comm.entity.WeTalkRequestArticle;
import org.dromara.oa.comm.enums.MessageType;
import org.dromara.oa.core.provider.factory.OaFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
public class OAWeTalkTests {

    /**
     * WeTalk的Text测试
     */
    @Test
    public void oaWeTalkText() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("weTalk");

        Request request = new Request();
        ArrayList<String> phones = new ArrayList<>();
        phones.add("17623062936");
        // 支持通过手机号@
        request.setPhoneList(phones);
        // 支持@all
        // request.setIsNoticeAll(true);
        request.setContent("info: 测试消息");

        alarm.sender(request, MessageType.WE_TALK_TEXT);
    }

    /**
     * WeTalk的Markdown测试
     */
    @Test
    public void oaWeTalkMarkdown() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("weTalk");

        Request request = new Request();
        // 支持@all
        request.setIsNoticeAll(true);
        request.setContent("#### 杭州天气 @150XXXXXXXX \n > 9度，西北风1级，空气良89，相对温度73%\n > ![screenshot](https://img.alicdn.com/tfs/TB1NwmBEL9TBuNjy1zbXXXpepXa-2400-1218.png)\n > ###### 10点20分发布 [天气](https://www.dingtalk.com) \n");
        request.setTitle("info:标题");
        alarm.sender(request, MessageType.WE_TALK_MARKDOWN);

    }

    /**
     * WeTalk的News测试
     */
    @Test
    public void oaWeTalkLink() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("weTalk");

        Request request = new Request();
        ArrayList<WeTalkRequestArticle> articles = new ArrayList<>();
        articles.add(new WeTalkRequestArticle("中秋节礼品领取", "今年中秋节公司有豪礼相送", "www.qq.com", "http://res.mail.qq.com/node/ww/wwopenmng/images/independent/doc/test_pic_msg1.png"));
        request.setArticleList(articles);

        alarm.sender(request, MessageType.WE_TALK_NEWS);

    }


}
