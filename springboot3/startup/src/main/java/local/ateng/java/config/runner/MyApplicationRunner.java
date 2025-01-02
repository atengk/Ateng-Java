package local.ateng.java.config.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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
        /*
        在程序后面添加：run --name=ateng --age=24
         */

        // 选项参数
        List<String> name = args.getOptionValues("name");
        List<String> age = args.getOptionValues("age");
        log.info("获取到参数：--name {} --age {}", name, age); // 获取到参数：--name [ateng] --age [24]

        // 非选项参数
        List<String> nonOptionArgs = args.getNonOptionArgs();
        log.info("获取到非参数：{}", nonOptionArgs); // 获取到非参数：[run]

    }
}

