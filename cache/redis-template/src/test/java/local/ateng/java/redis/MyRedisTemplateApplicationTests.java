package local.ateng.java.redis;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.redis.entity.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;
import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MyRedisTemplateApplicationTests {
    private final RedisTemplate redisTemplate;

    @Test
    void test02() {
        UserInfoEntity user = UserInfoEntity.builder()
                .id(100L)
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        redisTemplate.opsForValue().set("test:user", user);
    }

    @Test
    void test02_1() {
        UserInfoEntity user = (UserInfoEntity) redisTemplate.opsForValue().get("test:user");
        System.out.println(user);
        System.out.println(user.getName());
    }

    @Test
    void test03() {
        String str = "[{\"age\":57,\"birthday\":\"1976-02-28 20:53:21.368\",\"city\":\"东莞\",\"id\":103,\"name\":\"钟明轩\",\"province\":\"广东省\",\"score\":7.2},{\"age\":58,\"birthday\":\"1997-08-24 22:30:32.127\",\"city\":\"东莞\",\"id\":865,\"name\":\"汪风华\",\"province\":\"辽宁省\",\"score\":51.52},{\"age\":55,\"birthday\":\"2000-10-21 00:37:55.746\",\"city\":\"东莞\",\"id\":1216,\"name\":\"吴煜祺\",\"province\":\"吉林省\",\"score\":23.091},{\"age\":82,\"birthday\":\"1962-01-07 14:01:52.73\",\"city\":\"东莞\",\"id\":1698,\"name\":\"田炎彬\",\"province\":\"黑龙江省\",\"score\":41.216},{\"age\":87,\"birthday\":\"1967-09-10 10:49:19.197\",\"city\":\"东莞\",\"id\":1779,\"name\":\"王建辉\",\"province\":\"宁夏\",\"score\":77.111},{\"age\":63,\"birthday\":\"2000-02-06 03:19:10.931\",\"city\":\"东莞\",\"id\":1936,\"name\":\"廖展鹏\",\"province\":\"黑龙江省\",\"score\":46.459},{\"age\":72,\"birthday\":\"1988-04-14 07:36:20.265\",\"city\":\"东莞\",\"id\":2224,\"name\":\"白笑愚\",\"province\":\"湖北省\",\"score\":21.948},{\"age\":55,\"birthday\":\"1976-09-03 18:45:45.393\",\"city\":\"东莞\",\"id\":2807,\"name\":\"魏熠彤\",\"province\":\"澳门\",\"score\":49.405},{\"age\":88,\"birthday\":\"1999-04-01 19:44:01.136\",\"city\":\"东莞\",\"id\":2968,\"name\":\"高航\",\"province\":\"重庆市\",\"score\":40.489},{\"age\":93,\"birthday\":\"1998-09-18 09:38:58.939\",\"city\":\"东莞\",\"id\":2974,\"name\":\"贺明哲\",\"province\":\"内蒙古\",\"score\":64.759},{\"age\":82,\"birthday\":\"1995-12-23 18:17:19.873\",\"city\":\"东莞\",\"id\":102,\"name\":\"罗弘文\",\"province\":\"澳门\",\"score\":43.434},{\"age\":81,\"birthday\":\"1982-09-18 14:03:25.715\",\"city\":\"东莞\",\"id\":1198,\"name\":\"范耀杰\",\"province\":\"山东省\",\"score\":38.969},{\"age\":86,\"birthday\":\"1965-08-04 17:25:06.653\",\"city\":\"东莞\",\"id\":1750,\"name\":\"邹凯瑞\",\"province\":\"黑龙江省\",\"score\":20.792},{\"age\":68,\"birthday\":\"2005-01-12 01:52:02.634\",\"city\":\"东莞\",\"id\":1890,\"name\":\"赵智渊\",\"province\":\"新疆\",\"score\":7.497},{\"age\":92,\"birthday\":\"1979-09-30 21:55:53.382\",\"city\":\"东莞\",\"id\":2226,\"name\":\"于瑾瑜\",\"province\":\"宁夏\",\"score\":44.407},{\"age\":85,\"birthday\":\"1977-05-30 02:51:46.774\",\"city\":\"东莞\",\"id\":2294,\"name\":\"郭旭尧\",\"province\":\"山东省\",\"score\":89.327},{\"age\":53,\"birthday\":\"1975-07-01 02:57:52.664\",\"city\":\"东莞\",\"id\":2542,\"name\":\"白浩\",\"province\":\"江西省\",\"score\":24.3}]";
        List<UserInfoEntity> list = JSONArray.parseArray(str, UserInfoEntity.class);
        System.out.println(list);
        redisTemplate.opsForList().rightPushAll("test:userList", list);
    }

    @Test
    void test03_2() {
        List<UserInfoEntity> list = redisTemplate.opsForList().range("test:userList", 0, -1);
        System.out.println(list);
    }

    @Test
    void test04_1() {
        JSONObject object = JSONObject.of("id", 1001L, "name", "DataWorks", "date", "2017-07-14");
        redisTemplate.opsForValue().set("test:jsonObject", object);
    }

    @Test
    void test04_2() {
        JSONObject jsonObject = (JSONObject) redisTemplate.opsForValue().get("test:jsonObject");
        System.out.println(jsonObject);
    }

}
