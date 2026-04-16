package local.ateng.java.mybatisjdk8;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.customutils.entity.MyUser;
import local.ateng.java.customutils.init.InitData;
import local.ateng.java.customutils.utils.FastJson2Util;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Fastjson2 FastJson2Util 测试类
 *
 * @author Ateng
 * @since 2026-04-16
 */
public class FastJson2UtilTests {

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

        String json = FastJson2Util.toJsonString(myUser);
        System.out.println(json);
        // {"ABBCCdd":"aBBCCdd","birthday":"2000-01-01","city":"重庆市","createTime":"2026-04-16 20:17:57.695334100","createTime2":"2026-04-16 20:17:57.7","dateTime":"2026-04-16 20:17:57.693","email":"kongyu2385569970@gmail.com","id":1,"instantTime":"2026-04-16T12:17:57.696330800Z","list":["1","2"],"map":{"name":"ateng","age":26},"name":"ateng","num":0,"offsetDateTime":"2026-04-16T20:17:57.697329800+08:00","phoneNumber":"1762306666","ratio":0.7147,"score":1E+20,"set":["1","2","3"],"zonedDateTime":"2026-04-16T20:17:57.700320500[Asia/Shanghai]"}
    }

    /**
     * 测试：普通对象转 JSON
     */
    @Test
    void test1() {
        MyUser myUser = InitData.getDataList().get(0);
        System.out.println(FastJson2Util.toJsonString(myUser));
        // {"age":0,"birthday":"2026-04-16","city":"鄂尔多斯","createTime":"2026-04-16 20:38:41.838732400","dateTime":"2026-04-16 20:38:41.838","email":"健柏.金@yahoo.com","id":1,"name":"曾炫明","num":0,"phoneNumber":"17530066002","province":"广西省","ratio":0.17693999999999999,"score":15.56}
    }

    /**
     * 测试：格式化 JSON
     */
    @Test
    void test2() {
        MyUser myUser = InitData.getDataList().get(0);
        System.out.println(FastJson2Util.toPrettyJsonString(myUser));
        /*
        {
            "age":0,
            "birthday":"2026-04-16",
            "city":"烟台",
            "createTime":"2026-04-16 20:39:00.509737200",
            "dateTime":"2026-04-16 20:39:00.509",
            "email":"天磊.崔@yahoo.com",
            "id":1,
            "name":"顾烨磊",
            "num":0,
            "phoneNumber":"14725260096",
            "province":"贵州省",
            "ratio":0.86327,
            "score":29.81
        }
         */
    }

    /**
     * 测试：JSON -> 对象
     */
    @Test
    void test3() {
        String json = "{\"id\":1,\"name\":\"ateng\"}";
        MyUser user = FastJson2Util.parseObject(json, MyUser.class);

        System.out.println(user);
        System.out.println(user.getClass());
        // MyUser(id=1, name=ateng, age=null, phoneNumber=null, email=null, score=null, ratio=null, birthday=null, province=null, city=null, dateTime=null, createTime=null, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
        // class local.ateng.java.customutils.entity.MyUser
    }

    /**
     * 测试：JSON -> List
     */
    @Test
    void test4() {
        String json = "[{\"id\":1,\"name\":\"ateng\"}]";

        List<MyUser> list = FastJson2Util.parseObject(json, new TypeReference<List<MyUser>>() {
        });
        System.out.println(list);
        // [MyUser(id=1, name=ateng, age=null, phoneNumber=null, email=null, score=null, ratio=null, birthday=null, province=null, city=null, dateTime=null, createTime=null, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)]
    }

    /**
     * 测试：JSON 合法性校验
     */
    @Test
    void testIsJson() {
        System.out.println(FastJson2Util.isJson("{\"a\":1}"));
        System.out.println(FastJson2Util.isJson("{]"));
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

        MyUser user = FastJson2Util.convert(map, MyUser.class);
        System.out.println(user);
        // MyUser(id=2, name=李四, age=25, phoneNumber=null, email=null, score=null, ratio=null, birthday=null, province=null, city=null, dateTime=null, createTime=null, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
    }

    /**
     * 测试：对象 -> Map
     */
    @Test
    void testConvert_ObjectToMap() {
        MyUser user = InitData.getDataList().get(0);

        Map<String, Object> map = FastJson2Util.convert(user, new TypeReference<Map<String, Object>>() {
        });
        System.out.println(map);
        // {birthday=2026-04-16, dateTime=2026-04-16 20:41:14.45, city=无锡, num=0, score=19.03, phoneNumber=17157217670, province=广西省, createTime=2026-04-16 20:41:14.450080400, name=熊凯瑞, id=1, age=0, email=雨泽.崔@yahoo.com, ratio=0.71064}
    }

    /**
     * 测试：parseMap（泛型）
     */
    @Test
    void testParseMap() {
        String json = "{\"name\":\"ateng\",\"age\":26}";

        Map<String, Object> map = FastJson2Util.parseMap(json);
        System.out.println(map);
        // {name=ateng, age=26}

        Map<String, Integer> map2 = FastJson2Util.parseMap(json, String.class, Integer.class);
        System.out.println(map2);
        // {name=ateng, age=26}
    }

    /**
     * 测试：toMap（对象转 Map）
     */
    @Test
    void testToMap() {
        MyUser user = InitData.getDataList().get(0);

        System.out.println(FastJson2Util.toMap(user));
        // {birthday=2026-04-16, dateTime=2026-04-16 20:41:57.821, city=大同, num=0, score=3.15, phoneNumber=15342008409, province=广西省, createTime=2026-04-16 20:41:57.821640500, name=熊鹏飞, id=1, age=0, email=伟宸.龚@hotmail.com, ratio=0.01609}
        System.out.println(FastJson2Util.toMap(user, String.class, String.class));
        // {birthday=2026-04-16, dateTime=2026-04-16 20:41:57.821, city=大同, num=0, score=3.15, phoneNumber=15342008409, province=广西省, createTime=2026-04-16 20:41:57.821640500, name=熊鹏飞, id=1, age=0, email=伟宸.龚@hotmail.com, ratio=0.01609}
    }

    /**
     * 测试：路径读取
     */
    @Test
    void testGetPath() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26},\"items\":[{\"id\":1}]}";

        System.out.println(FastJson2Util.getString(json, "user.name"));
        // ateng
        System.out.println(FastJson2Util.getInteger(json, "user.age"));
        // 26
        System.out.println(FastJson2Util.getInteger(json, "items[0].id"));
        // 1
    }

    /**
     * 测试：路径写入
     */
    @Test
    void testPutPath() {
        String json = "{}";

        json = FastJson2Util.put(json, "user.name", "ateng");
        json = FastJson2Util.put(json, "items[0].id", 1);

        System.out.println(json);
        // {"user":{"name":"ateng"},"items":[{"id":1}]}
    }

    /**
     * 测试：路径删除
     */
    @Test
    void testRemovePath() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        json = FastJson2Util.remove(json, "user.age");
        System.out.println(json);
        // {"user":{"name":"ateng"}}
    }

    /**
     * 测试：浅合并
     */
    @Test
    void testMerge() {
        String j1 = "{\"name\":\"ateng\"}";
        String j2 = "{\"age\":26}";

        System.out.println(FastJson2Util.merge(j1, j2));
        // {"name":"ateng","age":26}
    }

    /**
     * 测试：深度合并
     */
    @Test
    void testDeepMerge() {
        String j1 = "{\"user\":{\"name\":\"ateng\"}}";
        String j2 = "{\"user\":{\"age\":26}}";

        System.out.println(FastJson2Util.deepMerge(j1, j2));
        // {"user":{"name":"ateng","age":26}}
    }

    /**
     * 测试：扁平化
     */
    @Test
    void testFlatten() {
        String json = "{\"user\":{\"name\":\"ateng\"},\"items\":[{\"id\":1}]}";

        Map<String, Object> flat = FastJson2Util.flatten(json);
        System.out.println(flat);
        // {user.name=ateng, items[0].id=1}
    }

    /**
     * 测试：字段提取
     */
    @Test
    void testExtractFieldList() {
        List<MyUser> list = InitData.getDataList();

        List<Long> ids = FastJson2Util.extractFieldList(list, "id", Long.class);
        System.out.println(ids);
        // [1, 2, 3, 4, 5, 6, 7, ...]
    }

    /**
     * 测试：深拷贝
     */
    @Test
    void testCopy() {
        MyUser user = InitData.getDataList().get(0);

        MyUser copy = FastJson2Util.copy(user, MyUser.class);

        System.out.println(copy);
        // MyUser(id=1, name=傅子轩, age=0, phoneNumber=17565562473, email=鹏飞.贾@gmail.com, score=28.81, ratio=0.53571, birthday=2026-04-16, province=安徽省, city=镇江, dateTime=Thu Apr 16 20:44:59 CST 2026, createTime=2026-04-16T20:44:59.744258700, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
        System.out.println(copy == user);
        // false
    }

    /**
     * 测试：JSON Pointer
     */
    @Test
    void testJsonPointer() {
        String json = "{\"user\":{\"name\":\"ateng\"},\"items\":[{\"id\":1}]}";

        System.out.println(FastJson2Util.getNodeByPointer(json, "/user/name"));
        // ateng
        System.out.println(FastJson2Util.getNodeByPointer(json, "/items/0/id"));
        // 1
    }

    /**
     * 测试：格式化 & 规范化
     */
    @Test
    void testFormat() {
        String json = "{\"name\":\"ateng\",\"age\":26}";

        System.out.println(FastJson2Util.pretty(json));
        /*
        {
            "name":"ateng",
            "age":26
        }
         */
        System.out.println(FastJson2Util.normalize(json));
        // {"name":"ateng","age":26}
    }

    /**
     * 测试：常规推荐序列化配置
     * <p>
     * 适用于绝大多数业务接口返回：
     * - 输出 null 字段
     * - 格式化 JSON（便于调试）
     * - BigDecimal 保持精度（避免科学计数法）
     */
    @Test
    void testWriter_Common() {
        MyUser user = InitData.getDataList().get(0);

        String json = FastJson2Util.toJsonString(
                user,
                FastJson2Util.WRITER_WRITE_NULLS,
                FastJson2Util.WRITER_PRETTY_FORMAT,
                FastJson2Util.WRITER_WRITE_BIG_DECIMAL_AS_PLAIN
        );

        System.out.println(json);
        /*
        {
            "ABBCCdd":null,
            "age":0,
            "birthday":"2026-04-16",
            "city":"日照",
            "createTime":"2026-04-16 20:46:23.878591200",
            "createTime2":null,
            "createTime3":null,
            "dateTime":"2026-04-16 20:46:23.878",
            "email":"风华.张@hotmail.com",
            "id":1,
            "instantTime":null,
            "list":null,
            "map":null,
            "name":"余炎彬",
            "num":0,
            "offsetDateTime":null,
            "phoneNumber":"14521626514",
            "province":"福建省",
            "ratio":0.36025,
            "score":40.67,
            "set":null,
            "zonedDateTime":null
        }
         */
    }

    /**
     * 测试：前端安全序列化配置
     * <p>
     * 适用于前后端交互：
     * - Long 转字符串（避免 JS 精度丢失）
     * - 浏览器兼容模式
     */
    @Test
    void testWriter_FrontendSafe() {
        MyUser user = InitData.getDataList().get(0);

        String json = FastJson2Util.toJsonString(
                user,
                FastJson2Util.WRITER_WRITE_LONG_AS_STRING,
                FastJson2Util.WRITER_BROWSER_COMPATIBLE
        );

        System.out.println(json);
        // {"age":0,"birthday":"2026-04-16","city":"泸州","createTime":"2026-04-16 20:47:12.199250400","dateTime":"2026-04-16 20:47:12.199","email":"鸿涛.尹@hotmail.com","id":"1","name":"覃晓博","num":0,"phoneNumber":"17740159482","province":"广东省","ratio":0.48875999999999997,"score":28.03}
    }

    /**
     * 测试：高性能紧凑序列化配置
     * <p>
     * 适用于日志、缓存（Redis/MQ）：
     * - 不输出默认值
     * - 不输出空数组
     * - 减少 JSON 体积
     */
    @Test
    void testWriter_Performance() {
        MyUser user = InitData.getDataList().get(0);

        String json = FastJson2Util.toJsonString(
                user,
                FastJson2Util.WRITER_NOT_WRITE_DEFAULT_VALUE,
                FastJson2Util.WRITER_NOT_WRITE_EMPTY_ARRAY
        );

        System.out.println(json);
        // {"age":0,"birthday":"2026-04-16","city":"石嘴山","createTime":"2026-04-16 20:48:53.141350600","dateTime":"2026-04-16 20:48:53.141","email":"炫明.韦@gmail.com","id":1,"name":"潘鸿煊","phoneNumber":"17054103442","province":"四川省","ratio":0.26223,"score":59.83}
    }

    /**
     * 测试：Map Key 字符串化
     * <p>
     * 适用于动态 JSON / 通用结构：
     * - 非字符串 Key 自动转字符串
     */
    @Test
    void testWriter_MapKeyString() {
        Map<Object, Object> map = new HashMap<>();
        map.put(1, "a");
        map.put(true, "b");

        String json = FastJson2Util.toJsonString(
                map,
                FastJson2Util.WRITER_WRITE_NON_STRING_KEY_AS_STRING
        );

        System.out.println(json);
        // {"1":"a","true":"b"}
    }

    /**
     * 测试：带类型信息序列化 + 反序列化
     * <p>
     * 适用于缓存 / RPC / 深拷贝：
     * - 写入类信息
     * - 反序列化时自动恢复类型
     */
    @Test
    void testWriter_WithClassName() {
        MyUser user = InitData.getDataList().get(0);

        String json = FastJson2Util.toJsonString(
                user,
                FastJson2Util.WRITER_WRITE_CLASS_NAME
        );

        System.out.println(json);

        MyUser result = FastJson2Util.parseObject(
                json,
                MyUser.class,
                FastJson2Util.READER_SUPPORT_AUTO_TYPE
        );

        System.out.println(result);
        // {"@type":"local.ateng.java.customutils.entity.MyUser","age":0,"birthday":"2026-04-16","city":"连云港","createTime":"2026-04-16 20:49:24.190163600","dateTime":"2026-04-16 20:49:24.19","email":"峻熙.沈@gmail.com","id":1L,"name":"赵振家","num":0,"phoneNumber":"17066835083","province":"广东省","ratio":0.25068,"score":74.47}
        // MyUser(id=1, name=赵振家, age=0, phoneNumber=17066835083, email=峻熙.沈@gmail.com, score=74.47, ratio=0.25068, birthday=2026-04-16, province=广东省, city=连云港, dateTime=Thu Apr 16 20:49:24 CST 2026, createTime=2026-04-16T20:49:24.190163600, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
    }

    /**
     * 测试：宽松解析模式
     * <p>
     * 适用于第三方接口 / 非标准 JSON：
     * - 支持无引号字段
     * - 智能字段匹配
     */
    @Test
    void testReader_LooseMode() {
        String json = "{name:'ateng',age:26}";

        Map<String, Object> map = FastJson2Util.parseObject(
                json,
                new TypeReference<Map<String, Object>>() {
                },
                FastJson2Util.READER_ALLOW_UN_QUOTED_FIELD_NAMES,
                FastJson2Util.READER_SUPPORT_SMART_MATCH
        );

        System.out.println(map);
        // {name=ateng, age=26}
    }

    /**
     * 测试：严格模式解析
     * <p>
     * 适用于核心系统 / 安全敏感场景：
     * - 未知字段报错
     * - 基本类型 null 报错
     */
    @Test
    void testReader_StrictMode() {
        String json = "{\"id\":1,\"name\":\"ateng\"}";

        MyUser user = FastJson2Util.parseObject(
                json,
                MyUser.class,
                FastJson2Util.READER_ERROR_ON_UNKNOWN_PROPERTIES,
                FastJson2Util.READER_ERROR_ON_NULL_FOR_PRIMITIVES
        );

        System.out.println(user);
        // MyUser(id=1, name=ateng, age=null, phoneNumber=null, email=null, score=null, ratio=null, birthday=null, province=null, city=null, dateTime=null, createTime=null, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
    }

    /**
     * 测试：高精度数值解析
     * <p>
     * 适用于金融场景：
     * - float / double 使用 BigDecimal 解析
     * - 避免精度丢失
     */
    @Test
    void testReader_BigDecimal() {
        String json = "{\"score\":1.234567890123456789}";

        Map<String, Object> map = FastJson2Util.parseObject(
                json,
                new TypeReference<Map<String, Object>>() {
                },
                FastJson2Util.READER_USE_BIG_DECIMAL_FOR_FLOATS,
                FastJson2Util.READER_USE_BIG_DECIMAL_FOR_DOUBLES
        );

        System.out.println(map);
        // {score=1.234567890123456789}
    }

    /**
     * 测试：空值处理策略
     * <p>
     * 适用于数据清洗：
     * - 空字符串转 null
     * - 忽略 null 字段
     */
    @Test
    void testReader_NullHandling() {
        String json = "{\"name\":\"\",\"age\":null}";

        Map<String, Object> map = FastJson2Util.parseObject(
                json,
                new TypeReference<Map<String, Object>>() {
                },
                FastJson2Util.READER_EMPTY_STRING_AS_NULL,
                FastJson2Util.READER_IGNORE_NULL_PROPERTY_VALUE
        );

        System.out.println(map);
        // {}
    }

    /*
        ========================
        一、基础路径
        ========================

        $                     // 根节点
        $.name                // 根节点下 name
        $.user.name           // 嵌套对象字段
        $.user.age

        ========================
        二、数组操作
        ========================

        $.items[0]            // 第一个元素
        $.items[1].id         // 第二个元素的 id
        $.items[*]            // 所有元素
        $.items[*].id         // 所有元素的 id

        $.items[-1]           // 最后一个元素（部分实现支持）
        $.items[0,1]          // 多个索引
        $.items[0:2]          // 区间（0 到 1）

        ========================
        三、通配符
        ========================

        $.*                   // 根下所有字段
        $.user.*              // user 下所有字段

        ========================
        四、条件过滤（非常重要）
        ========================

        $.items[?(@.id == 1)]                // id 等于 1
        $.items[?(@.age > 18)]               // age 大于 18
        $.items[?(@.name == 'ateng')]        // 字符串匹配

        $.items[?(@.price >= 100 && @.price <= 200)]  // 区间过滤

        ========================
        五、多条件组合
        ========================

        $.items[?(@.age > 18 && @.city == '重庆')]
        $.items[?(@.age > 18 || @.vip == true)]

        ========================
        六、字段存在判断
        ========================

        $.items[?(@.name)]
        $.items[?(@.age != null)]

        ========================
        七、深度扫描（递归查找）
        ========================

        $..name              // 查找所有 name 字段（全局）
        $..id                // 所有 id

        ========================
        八、长度 / size
        ========================

        $.items.size()       // 数组长度
        $.items.length()     // 同上（部分实现支持）

        ========================
        九、函数（部分支持）
        ========================

        $.items.min()
        $.items.max()
        $.items.avg()

        ========================
        十、复杂组合（实战）
        ========================

        $.items[?(@.status == 1)].id
        // 取 status=1 的所有 id

        $.orders[?(@.amount > 100)].user.name
        // 过滤订单后再取用户名字

        $..items[?(@.price > 100)].name
        // 全局查找价格大于100的商品名称

        ========================
        十一、更新 / 删除常用路径
        ========================

        $.user.name
        $.items[0].id
        $.items[?(@.id == 1)].name   // 注意：复杂路径 set/remove 需谨慎

        ========================
        十二、推荐实践
        ========================

        1. 高频路径建议缓存 JSONPath 对象
        2. 大 JSON 使用 extract（避免全量解析）
        3. set/remove 尽量使用“确定路径”，避免复杂过滤路径
        4. 复杂过滤建议先 eval 再处理（更安全）

        */

    /**
     * 测试：JSONPath 基础提取（对象）
     * <p>
     * 从对象中提取字段值
     */
    @Test
    void testJsonPath_Eval_Object() {
        MyUser user = InitData.getDataList().get(0);

        Object name = FastJson2Util.evalByJsonPath(user, "$.name");
        Object id = FastJson2Util.evalByJsonPath(user, "$.id");

        System.out.println(name);
        // 方鹏
        System.out.println(id);
        // 1
    }

    /**
     * 测试：JSONPath 基础提取（JSON字符串）
     * <p>
     * 从 JSON 字符串中提取字段
     */
    @Test
    void testJsonPath_Eval_Json() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        System.out.println(FastJson2Util.evalByJsonPath(json, "$.user.name"));
        // ateng
        System.out.println(FastJson2Util.evalByJsonPath(json, "$.user.age"));
        // 26
    }

    /**
     * 测试：JSONPath 提取并转类型
     * <p>
     * 常用于避免手动转换
     */
    @Test
    void testJsonPath_Eval_WithType() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        String name = FastJson2Util.evalByJsonPath(json, "$.user.name", String.class);
        Integer age = FastJson2Util.evalByJsonPath(json, "$.user.age", Integer.class);

        System.out.println(name);
        // ateng
        System.out.println(age);
        // 26
    }

    /**
     * 测试：JSONPath contains 判断
     * <p>
     * 判断路径是否存在
     */
    @Test
    void testJsonPath_Contains() {
        String json = "{\"user\":{\"name\":\"ateng\"}}";

        System.out.println(FastJson2Util.containsByJsonPath(json, "$.user.name"));
        // true
        System.out.println(FastJson2Util.containsByJsonPath(json, "$.user.age"));
        // false
    }

    /**
     * 测试：JSONPath set（对象）
     * <p>
     * 对对象进行原地修改
     */
    @Test
    void testJsonPath_Set_Object() {
        MyUser user = InitData.getDataList().get(0);

        FastJson2Util.setByJsonPath(user, "$.name", "修改后的名字");
        FastJson2Util.setByJsonPath(user, "$.age", 99);

        System.out.println(user);
        // MyUser(id=1, name=修改后的名字, age=99, phoneNumber=17070138548, email=擎宇.顾@gmail.com, score=56.17, ratio=0.69194, birthday=2026-04-16, province=宁夏, city=济宁, dateTime=Thu Apr 16 20:59:45 CST 2026, createTime=2026-04-16T20:59:45.355753300, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
    }

    /**
     * 测试：JSONPath set（JSON字符串）
     * <p>
     * 返回修改后的 JSON
     */
    @Test
    void testJsonPath_Set_Json() {
        String json = "{\"user\":{\"name\":\"ateng\"}}";

        json = FastJson2Util.setByJsonPath(json, "$.user.name", "newName");
        json = FastJson2Util.setByJsonPath(json, "$.user.age", 30);

        System.out.println(json);
        // {"user":{"name":"newName","age":30}}
    }

    /**
     * 测试：JSONPath remove（对象）
     * <p>
     * 删除对象字段
     */
    @Test
    void testJsonPath_Remove_Object() {
        MyUser user = InitData.getDataList().get(0);

        FastJson2Util.removeByJsonPath(user, "$.email");

        System.out.println(user);
        // MyUser(id=1, name=沈彬, age=0, phoneNumber=17671866434, email=null, score=16.11, ratio=0.70943, birthday=2026-04-16, province=香港, city=莱州, dateTime=Thu Apr 16 21:00:53 CST 2026, createTime=2026-04-16T21:00:53.197469400, instantTime=null, offsetDateTime=null, zonedDateTime=null, createTime2=null, createTime3=null, num=0, list=null, set=null, map=null, aBBCCdd=null)
    }

    /**
     * 测试：JSONPath remove（JSON字符串）
     * <p>
     * 删除 JSON 字段
     */
    @Test
    void testJsonPath_Remove_Json() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        json = FastJson2Util.removeByJsonPath(json, "$.user.age");

        System.out.println(json);
        // {"user":{"name":"ateng"}}
    }

    /**
     * 测试：JSONPath extract（高性能提取）
     * <p>
     * 适用于大 JSON，只解析需要的部分
     */
    @Test
    void testJsonPath_Extract() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26},\"items\":[{\"id\":1},{\"id\":2}]}";

        Object name = FastJson2Util.extractByJsonPath(json, "$.user.name");
        Object id = FastJson2Util.extractByJsonPath(json, "$.items[0].id");

        System.out.println(name);
        // ateng
        System.out.println(id);
        // 1
    }

    /**
     * 测试：JSONPath extract + 类型转换
     */
    @Test
    void testJsonPath_Extract_WithType() {
        String json = "{\"user\":{\"name\":\"ateng\",\"age\":26}}";

        String name = FastJson2Util.extractByJsonPath(json, "$.user.name", String.class);
        Integer age = FastJson2Util.extractByJsonPath(json, "$.user.age", Integer.class);

        System.out.println(name);
        // ateng
        System.out.println(age);
        // 26
    }

    /**
     * 测试：JSONPath 缓存复用
     * <p>
     * 避免重复解析路径，提高性能
     */
    @Test
    void testJsonPath_Cache() {
        String path = "$.user.name";

        System.out.println(FastJson2Util.getJsonPath(path));
        // $.user.name
        System.out.println(FastJson2Util.getJsonPath(path)); // 第二次走缓存
        // $.user.name
    }

    /**
     * 测试：复杂路径（数组 + 嵌套）
     */
    @Test
    void testJsonPath_Complex() {
        String json = "{\"items\":[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]}";

        System.out.println(FastJson2Util.evalByJsonPath(json, "$.items[0].name"));
        // a
        System.out.println(FastJson2Util.evalByJsonPath(json, "$.items[1].id"));
        // 2
    }

}