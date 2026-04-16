package local.ateng.java.mybatisjdk8;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.customutils.entity.MyUser;
import local.ateng.java.customutils.init.InitData;
import local.ateng.java.customutils.utils.FastJsonUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Fastjson2 FastJsonUtil 测试类
 *
 * @author Ateng
 * @since 2026-04-16
 */
public class FastFastJsonUtilTests {

    /**
     * 测试：复杂对象转 JSON
     */
    @Test
    void test0() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "ateng");
        map.put("age", 26L);

        List<String> list = Arrays.asList("1", "2");
        Set<String> set = new HashSet<>(Arrays.asList("1", "2", "3"));

        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(null)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("1E+20"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .city("重庆市")
                .dateTime(new Date())
                .createTime(LocalDateTime.now())
                .instantTime(Instant.now())
                .offsetDateTime(OffsetDateTime.now())
                .zonedDateTime(ZonedDateTime.now())
                .createTime2(new Date())
                .list(list)
                .set(set)
                .map(map)
                .aBBCCdd("aBBCCdd")
                .build();

        String json = FastJsonUtil.toJsonString(myUser);
        System.out.println(json);
    }

    /**
     * 测试：普通对象转 JSON
     */
    @Test
    void test1() {
        MyUser myUser = InitData.getDataList().get(0);
        System.out.println(FastJsonUtil.toJsonString(myUser));
    }

    /**
     * 测试：格式化 JSON
     */
    @Test
    void test2() {
        MyUser myUser = InitData.getDataList().get(0);
        System.out.println(FastJsonUtil.toPrettyJsonString(myUser));
    }

    /**
     * 测试：JSON -> 对象
     */
    @Test
    void test3() {
        String json = "{\"id\":1,\"name\":\"ateng\"}";
        MyUser user = FastJsonUtil.parseObject(json, MyUser.class);

        System.out.println(user);
        System.out.println(user.getClass());
    }

    /**
     * 测试：JSON -> List
     */
    @Test
    void test4() {
        String json = "[{\"id\":1,\"name\":\"ateng\"}]";

        List<MyUser> list = FastJsonUtil.parseObject(json, new TypeReference<List<MyUser>>() {});
        System.out.println(list);
    }

    /**
     * 测试：JSON 合法性校验
     */
    @Test
    void testIsJson() {
        System.out.println(FastJsonUtil.isJson("{\"a\":1}"));
        System.out.println(FastJsonUtil.isJson("{]"));
    }

    /**
     * 测试：Map -> 对象
     */
    @Test
    void testConvert_MapToObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 2L);
        map.put("name", "李四");
        map.put("age", 25);

        MyUser user = FastJsonUtil.convert(map, MyUser.class);
        System.out.println(user);
    }

    /**
     * 测试：对象 -> Map
     */
    @Test
    void testConvert_ObjectToMap() {
        MyUser user = InitData.getDataList().get(0);

        Map<String, Object> map = FastJsonUtil.convert(user, new TypeReference<Map<String, Object>>() {});
        System.out.println(map);
    }

    /**
     * 测试：parseMap（泛型）
     */
    @Test
    void testParseMap() {
        String json = "{\"name\":\"ateng\",\"age\":26}";

        Map<String, Object> map = FastJsonUtil.parseMap(json);
        System.out.println(map);

        Map<String, Integer> map2 = FastJsonUtil.parseMap(json, String.class, Integer.class);
        System.out.println(map2);
    }

    /**
     * 测试：toMap（对象转 Map）
     */
    @Test
    void testToMap() {
        MyUser user = InitData.getDataList().get(0);

        System.out.println(FastJsonUtil.toMap(user));
        System.out.println(FastJsonUtil.toMap(user, String.class, String.class));
    }

    /**
     * 测试：路径读取
     */
    @Test
    void testGetPath() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26},\"items\":[{\"id\":1}]}";

        System.out.println(FastJsonUtil.getString(json, "user.name"));
        System.out.println(FastJsonUtil.getInteger(json, "user.age"));
        System.out.println(FastJsonUtil.getInteger(json, "items[0].id"));
    }

    /**
     * 测试：路径写入
     */
    @Test
    void testPutPath() {
        String json = "{}";

        json = FastJsonUtil.put(json, "user.name", "ateng");
        json = FastJsonUtil.put(json, "items[0].id", 1);

        System.out.println(json);
    }

    /**
     * 测试：路径删除
     */
    @Test
    void testRemovePath() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        json = FastJsonUtil.remove(json, "user.age");
        System.out.println(json);
    }

    /**
     * 测试：浅合并
     */
    @Test
    void testMerge() {
        String j1 = "{\"name\":\"ateng\"}";
        String j2 = "{\"age\":26}";

        System.out.println(FastJsonUtil.merge(j1, j2));
    }

    /**
     * 测试：深度合并
     */
    @Test
    void testDeepMerge() {
        String j1 = "{\"user\":{\"name\":\"ateng\"}}";
        String j2 = "{\"user\":{\"age\":26}}";

        System.out.println(FastJsonUtil.deepMerge(j1, j2));
    }

    /**
     * 测试：扁平化
     */
    @Test
    void testFlatten() {
        String json = "{\"user\":{\"name\":\"ateng\"},\"items\":[{\"id\":1}]}";

        Map<String, Object> flat = FastJsonUtil.flatten(json);
        System.out.println(flat);
    }

    /**
     * 测试：字段提取
     */
    @Test
    void testExtractFieldList() {
        List<MyUser> list = InitData.getDataList();

        List<Long> ids = FastJsonUtil.extractFieldList(list, "id", Long.class);
        System.out.println(ids);
    }

    /**
     * 测试：深拷贝
     */
    @Test
    void testCopy() {
        MyUser user = InitData.getDataList().get(0);

        MyUser copy = FastJsonUtil.copy(user, MyUser.class);

        System.out.println(copy);
        System.out.println(copy == user);
    }

    /**
     * 测试：JSON Pointer
     */
    @Test
    void testJsonPointer() {
        String json = "{\"user\":{\"name\":\"ateng\"},\"items\":[{\"id\":1}]}";

        System.out.println(FastJsonUtil.getNodeByPointer(json, "/user/name"));
        System.out.println(FastJsonUtil.getNodeByPointer(json, "/items/0/id"));
    }

    /**
     * 测试：格式化 & 规范化
     */
    @Test
    void testFormat() {
        String json = "{\"name\":\"ateng\",\"age\":26}";

        System.out.println(FastJsonUtil.pretty(json));
        System.out.println(FastJsonUtil.normalize(json));
    }

}