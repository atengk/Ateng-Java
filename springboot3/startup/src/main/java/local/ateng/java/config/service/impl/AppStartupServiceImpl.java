package local.ateng.java.config.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import local.ateng.java.config.event.MyCustomEvent;
import local.ateng.java.config.service.AppStartupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppStartupServiceImpl implements AppStartupService {

    @Override
    @PostConstruct
    public void myBean() {
        String myBean = SpringUtil.getApplicationContext().getBean("myBean", String.class);
        log.info("myBean={}", myBean);
    }

    /**
     * @EventListener(ApplicationReadyEvent.class)：在应用完全启动后执行的方法。
     */
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void event1() {
        log.info("由{}启动...", "@EventListener(ApplicationReadyEvent.class)");
    }

    @Override
    @EventListener
    public void event2(MyCustomEvent myCustomEvent) {
        log.info("myCustomEvent={}", myCustomEvent.getMessage());
    }
}
