package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import local.ateng.java.fastjson2.init.InitData;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class JSONObjectTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public JSONObjectTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

    // 字符串转换为JSONObject
    @Test
    void test01() {
        String str = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject = JSONObject.parse(str);
        System.out.println(jsonObject);
    }

    // 字符串转换为java对象
    @Test
    void test02() {
        String str = "{\"id\":null,\"name\":\"John Doe\",\"age\":25,\"score\":0.0,\"birthday\":\"2024-01-18 15:05:10.102\",\"province\":\"\",\"city\":\"Example City\"}";
        UserInfoEntity user = JSONObject.parseObject(str, UserInfoEntity.class);
        System.out.println(user);
    }

    // JSONObject转换为java对象
    @Test
    void test02_2() {
        JSONObject jsonObject = new JSONObject() {{
            put("name", "John Doe");
            put("age", 25);
            put("birthday", "2024-01-18 15:05:10.102");
            put("city", "Example City");
            put("province", "Example province");
            put("score", 85.5);
        }};
        UserInfoEntity user = jsonObject.toJavaObject(UserInfoEntity.class);
        System.out.println(user);
    }

    // 从URL链接获取结果并转JSONObject
    @Test
    void test03() throws MalformedURLException {
        JSONObject jsonObject = JSON.parseObject(new URL("https://api.apiopen.top/api/sentences"));
        System.out.println(jsonObject);
    }

    // java对象转String
    @Test
    void test04() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        // JSONWriter.Feature介绍
        // https://github.com/alibaba/fastjson2/blob/main/docs/features_cn.md
        // JSONWriter.Feature.WriteNulls -> 序列化输出空值字段
        String str1 = JSONObject.toJSONString(user, JSONWriter.Feature.WriteNulls);
        System.out.println(str1);

        // JSONWriter.Feature.PrettyFormat -> 格式化输出
        String str2 = JSONObject.toJSONString(user, JSONWriter.Feature.PrettyFormat);
        System.out.println(str2);
    }

    // java对象转JSONObject
    @Test
    void test05() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        String str = JSONObject.toJSONString(user);
        JSONObject jsonObject = JSONObject.parse(str);
        System.out.println(jsonObject);
    }

    // JSONObject.of创建JSONObject对象
    @Test
    void test06() {
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
