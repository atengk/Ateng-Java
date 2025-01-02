package local.ateng.java.hutool;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;
import cn.hutool.log.level.Level;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Logfactory.get方法不再需要（或者不是必须）传入当前类名，会自动解析当前类名
 * log.xxx方法在传入Exception时同时支持模板语法。
 * 不需要桥接包而自动适配引入的日志框架，在无日志框架下依旧可以完美适配JDK Logging。
 * 引入多个日志框架情况下，可以自定义日志框架输出。
 * https://www.hutool.cn/docs/#/log/%E6%A6%82%E8%BF%B0
 *
 * @author 孔余
 * @since 2024-01-17 19:08
 */
@SpringBootTest
public class LogFactoryTests {
    private static final Log log = LogFactory.get();

    @Test
    void test01() {
        log.debug("This is {} log", Level.DEBUG);
        log.info("This is {} log", Level.INFO);
        log.warn("This is {} log", Level.WARN);

        Exception e = new Exception("test Exception");
        log.error(e, "This is {} log", Level.ERROR);
    }

    @Test
    void test02() {
        Exception e = new Exception("test Exception");
        StaticLog.error(e, "This is {} log", Level.ERROR);
    }
}
