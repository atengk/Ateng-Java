package local.ateng.java.config.runner;

import cn.hutool.extra.spring.SpringUtil;
import local.ateng.java.config.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ApplicationRunner：实现此接口的类会在Spring Boot应用启动后执行其run方法。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyApplicationRunner implements ApplicationRunner {
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = SpringUtil.getActiveProfiles();
        System.out.println(Arrays.toString(profiles));

        log.info("启动环境：{}", String.join(",", SpringUtil.getActiveProfiles()));
        log.info("配置文件：{}", appProperties);
        log.info("配置文件数据：{}", SpringUtil.getProperty("data"));
    }
}

