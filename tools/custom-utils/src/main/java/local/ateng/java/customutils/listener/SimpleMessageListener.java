package local.ateng.java.customutils.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleMessageListener {

    @EventListener
    public void onMessage(String message) {
        System.out.println("收到 String 消息事件：" + message);
    }
}

