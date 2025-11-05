package local.ateng.java.fastjson1;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
    }

}
