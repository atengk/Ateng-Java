package local.ateng.java.hutool;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 使用Spring Boot时，通过依赖注入获取bean是非常方便的，但是在工具化的应用场景下，
 * 想要动态获取bean就变得非常困难，于是Hutool封装了Spring中Bean获取的工具类——SpringUtil。
 * https://www.hutool.cn/docs/#/extra/Spring/Spring%E5%B7%A5%E5%85%B7-SpringUtil?id=spring%e5%b7%a5%e5%85%b7-springutil
 *
 * @author 孔余
 * @since 2024-01-17 17:37
 */
@SpringBootTest
public class SpringUtilTests {
    // 获取spring.profiles.active的值
    @Test
    void test01() {
        String activeProfile = SpringUtil.getActiveProfile();
        System.out.println(activeProfile);
    }

    // 获取spring.application.name的值
    @Test
    void test02() {
        String applicationName = SpringUtil.getApplicationName();
        System.out.println(applicationName);
    }

    // 获取指定属性的值
    @Test
    void test03() {
        String property = SpringUtil.getProperty("server.port");
        System.out.println(property);
    }

    // 获取指定Bean
    @Test
    void test04() {
        final String bean = SpringUtil.getBean("myBean");
        System.out.println(bean);
    }
}
