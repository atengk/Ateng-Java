package local.ateng.java.jasypt.runner;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * ApplicationRunner：实现此接口的类会在Spring Boot应用启动后执行其run方法。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@Component
@Slf4j
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        log.info("加密后的配置：{}",SpringUtil.getProperty("user.password", ""));
    }
}

