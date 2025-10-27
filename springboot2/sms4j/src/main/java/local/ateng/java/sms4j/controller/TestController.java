package local.ateng.java.sms4j.controller;

import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test")
    public void testSend(){
        SmsBlend smsBlend = SmsFactory.getSmsBlend("tx1");
        SmsResponse smsResponse = smsBlend.sendMessage("17623062936","你好，我是阿腾！");
        System.out.println(smsResponse);
    }
}
