package local.ateng.java.customutils.listener;

import local.ateng.java.customutils.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisterEventListener {

    @EventListener
    public void handle(UserRegisterEvent event) {
        System.out.println("收到用户注册事件，用户名：" + event.getUsername());
    }
}
