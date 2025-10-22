package local.ateng.java.oa;

import org.dromara.oa.api.OaSender;
import org.dromara.oa.comm.entity.Request;
import org.dromara.oa.comm.enums.MessageType;
import org.dromara.oa.core.provider.factory.OaFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
public class OAByteTalkTests {

    /**
     * ByteTalk的Text测试
     */
    @Test
    public void oaByteTalkText() {
        // 获取服务实例
        OaSender alarm = OaFactory.getSmsOaBlend("byteTalk");

        Request request = new Request();
        ArrayList<String> phones = new ArrayList<>();
        phones.add("17623062936");
        // 支持通过手机号@
        request.setPhoneList(phones);
        // 支持@all
        // request.setIsNoticeAll(true);
        request.setContent("info: 测试消息");

        alarm.sender(request, MessageType.BYTE_TALK_TEXT);
    }

}
