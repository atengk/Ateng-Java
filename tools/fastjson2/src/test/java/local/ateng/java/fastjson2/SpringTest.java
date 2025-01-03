package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 在Spring上使用
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-06-21 11:38:54
 */
@SpringBootTest
public class SpringTest {
    // 字符串转换为JSONObject
    @Test
    void test01() {
        String str = "{\"id\":null,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject = JSONObject.parse(str);
        System.out.println(jsonObject);
    }

    // JSONObject.of创建JSONObject对象
    @Test
    void test02() {
        JSONObject object = JSONObject.of(
                "id", 1001,
                "name", "DataWorks",
                "date", "2017-07-14",
                "createAt1", LocalDateTime.now(),
                "createAt2", new Date()
        );
        System.out.println(object);
    }
}
