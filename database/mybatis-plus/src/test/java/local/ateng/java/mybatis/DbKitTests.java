package local.ateng.java.mybatis;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import local.ateng.java.mybatis.entity.MyUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Db Kit
 * Db Kit 是 Mybatis-Plus 提供的一个工具类，它允许开发者通过静态调用的方式执行 CRUD 操作，从而避免了在 Spring 环境下可能出现的 Service 循环注入问题，简化了代码，提升了开发效率。
 * https://baomidou.com/guides/data-interface/
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-13
 */
@SpringBootTest
@DS("doris")
public class DbKitTests {

    @Test
    void test01() {
        MyUser user = Db.getById(1L, MyUser.class);
        System.out.println(user);
    }


}
