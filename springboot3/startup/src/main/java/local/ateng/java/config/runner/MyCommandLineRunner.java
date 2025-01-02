package local.ateng.java.config.runner;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CommandLineRunner：类似于ApplicationRunner，但接收命令行参数。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@Component
@Slf4j
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
         /*
        在程序后面添加：run --name=ateng --age=24
         */
        List<String> argList = CollUtil.newArrayList(args);
        log.info("获取到程序所有参数: {}", argList);
    }
}
