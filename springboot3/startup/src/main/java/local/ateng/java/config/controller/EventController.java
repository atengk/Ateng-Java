package local.ateng.java.config.controller;

import cn.hutool.extra.spring.SpringUtil;
import local.ateng.java.config.event.MyCustomEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推送事件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@RestController
@RequestMapping("/event")
public class EventController {
    @GetMapping("/send")
    public String send( String message) {
        MyCustomEvent event = new MyCustomEvent(this, message);
        ApplicationContext context = SpringUtil.getApplicationContext();
        context.publishEvent(event);
        return "success";
    }
}
