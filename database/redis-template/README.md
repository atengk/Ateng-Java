# Spring Data Redis

RedisTemplate 是 Spring Data Redis 提供的一个核心类，用于操作 Redis 数据库，支持对 Redis 各种数据结构的访问，例如字符串（String）、哈希（Hash）、列表（List）、集合（Set）、有序集合（Sorted Set）等。

**主要特点**
通用性强：支持 Redis 的所有数据结构和命令。
扩展性好：可以通过自定义序列化器来适配不同的数据格式。
线程安全：可以在多个线程中安全使用。



## 基础配置

### 添加依赖

添加Spring Boot Redis和Lettuce

```xml
       <!-- Spring Boot Redis 数据库集成，支持多种 Redis 数据结构和操作 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Lettuce 客户端连接池实现，基于 Apache Commons Pool2 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
        <!-- Spring 中集成 Fastjson2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring6</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
```

### 编辑配置

编辑配置文件 `application.yml`

```yaml
---
# Redis的相关配置
spring:
  data:
    redis:
      host: 192.168.1.10 # Redis服务器地址
      database: 102 # Redis数据库索引（默认为0）
      port: 42784 # Redis服务器连接端口
      password: Admin@123 # Redis服务器连接密码（默认为空）
      client-type: lettuce  # 默认使用Lettuce作为Redis客户端
      lettuce:
        pool:
          max-active: 100 # 连接池最大连接数（使用负值表示没有限制）
          max-wait: -1s # 连接池最大阻塞等待时间（使用负值表示没有限制）
          max-idle: 100 # 连接池中的最大空闲连接
          min-idle: 0 # 连接池最小空闲连接数
          time-between-eviction-runs: 1s # 空闲对象逐出器线程的运行间隔时间.空闲连接线程释放周期时间
      timeout: 5000ms # 连接超时时间（毫秒）
```



## 集成 Fastjson2

参考官方文档：[地址](https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#4-%E5%9C%A8-spring-data-redis-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2)

### 创建RedisSerializer

JSONReader.autoTypeFilter修改为包名

```java
package local.ateng.java.redis.config;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * RedisTemplate Fastjson2 Serializer
 * 自定义Redis序列化
 *
 * @author 孔余
 * @since 2024-01-30 17:29
 */
public class MyFastJsonRedisSerializer<T> implements RedisSerializer<T> {
    private final Class<T> type;
    private FastJsonConfig config = new FastJsonConfig();

    public MyFastJsonRedisSerializer(Class<T> type) {
        config.setCharset(Charset.forName("UTF-8"));
        config.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        // 配置 JSONWriter 的特性
        config.setWriterFeatures(
                JSONWriter.Feature.WriteClassName,             // 在 JSON 输出中写入类名
                JSONWriter.Feature.NotWriteNumberClassName,   // 不输出数字类型的类名
                JSONWriter.Feature.NotWriteSetClassName,      // 不输出 Set 类型的类名
                JSONWriter.Feature.WriteNulls,                // 输出 null 值的字段
                JSONWriter.Feature.FieldBased,                // 基于字段访问数据，而不是使用 getter/setter
                JSONWriter.Feature.BrowserCompatible,         // 生成与浏览器兼容的 JSON
                JSONWriter.Feature.WriteMapNullValue         // 输出 Map 中 null 值的键
        );

        // 配置 JSONReader 的特性
        config.setReaderFeatures(
                JSONReader.Feature.FieldBased,                // 基于字段访问数据，而不是使用 getter/setter
                JSONReader.Feature.SupportArrayToBean        // 支持将 JSON 数组解析为 Java Bean
        );


        // 支持自动类型，要读取带"@type"类型信息的JSON数据，需要显式打开SupportAutoType
        config.setReaderFilters(
                JSONReader.autoTypeFilter(
                        // 按需加上需要支持自动类型的类名前缀，范围越小越安全
                        "local.ateng.java."
                )
        );
        this.type = type;
    }

    public FastJsonConfig getFastJsonConfig() {
        return config;
    }

    public void setFastJsonConfig(FastJsonConfig fastJsonConfig) {
        this.config = fastJsonConfig;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            if (config.isJSONB()) {
                return JSONB.toBytes(t, config.getSymbolTable(), config.getWriterFilters(), config.getWriterFeatures());
            } else {
                return JSON.toJSONBytes(t, config.getDateFormat(), config.getWriterFilters(), config.getWriterFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not serialize: " + ex.getMessage(), ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            if (config.isJSONB()) {
                return JSONB.parseObject(bytes, type, config.getSymbolTable(), config.getReaderFilters(), config.getReaderFeatures());
            } else {
                return JSON.parseObject(bytes, type, config.getDateFormat(), config.getReaderFilters(), config.getReaderFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not deserialize: " + ex.getMessage(), ex);
        }
    }
}
```

### 创建RedisTemplateConfig

```java
package local.ateng.java.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return getRedisTemplate(redisConnectionFactory);
    }

    /**
     * 在 Spring Data Redis 中集成 Fastjson2
     * 使用 GenericFastJsonRedisSerializer 作为 RedisTemplate 的 RedisSerializer 来提升JSON序列化和反序列化速度。
     * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#4-%E5%9C%A8-spring-data-redis-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
     *
     * @param redisConnectionFactory
     * @return
     */
    private RedisTemplate getRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用GenericFastJsonRedisSerializer来序列化和反序列化redis的key和value值
         */
        //GenericFastJsonRedisSerializer genericFastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        //redisTemplate.setDefaultSerializer(fastJsonRedisSerializer);//设置默认的Serialize，包含 keySerializer & valueSerializer

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        /**
         * 自定义Serializer来序列化和反序列化redis的value值
         */
        MyFastJsonRedisSerializer myFastJsonRedisSerializer = new MyFastJsonRedisSerializer(Object.class);
        redisTemplate.setValueSerializer(myFastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(myFastJsonRedisSerializer);
        redisTemplate.setStringSerializer(myFastJsonRedisSerializer);
        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```



## 数据准备

### 添加依赖

```xml
        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>
```

### 创建实体类

```java
package local.kongyu.redisTemplate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

### 创建初始数据

```java
package local.kongyu.redisTemplate.init;

import com.github.javafaker.Faker;
import local.kongyu.redisTemplate.entity.UserInfoEntity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @since 2024-01-18 14:17
 */
@Getter
public class InitData {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        // 创建一个包含不少于100条JSON数据的列表
        List<UserInfoEntity> userList = new ArrayList();
        for (int i = 1; i <= 10; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list = userList;
        for (int i = 1; i <= 20; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list2 = userList;
    }

}
```



## 使用String

### 创建测试类

```java
/**
 * Redis String相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisStringTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 写入对象

```java
    //设置key对应的值
    @Test
    void set() {
        String key = "my:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForValue().set(key, user);
        // 并设置过期时间
        redisTemplate.opsForValue().set(key + ":expire", user, Duration.ofHours(1));
    }

    @Test
    void setMany() {
        String key = "my:user:data";
        List<UserInfoEntity> list = new InitData().getList();
        list.forEach(user -> redisTemplate.opsForValue().set(key + ":" + user.getId(), user));
    }

    @Test
    void setList() {
        String key = "my:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForValue().set(key, list);
    }
```

### 读取对象

```java
    //取出key值所对应的值
    @Test
    void get() {
        UserInfoEntity user = (UserInfoEntity) redisTemplate.opsForValue().get("my:user");
        System.out.println(user);
        System.out.println(user.getName());
    }

    @Test
    void getList() {
        List<UserInfoEntity> userList = (List<UserInfoEntity>) redisTemplate.opsForValue().get("my:userList");
        System.out.println(userList);
        System.out.println(userList.get(0).getName());
    }
```

### 判断key是否存在

```java
    //判断是否有key所对应的值，有则返回true，没有则返回false
    @Test
    void hashKey() {
        String key = "my:user";
        Boolean result = redisTemplate.hasKey(key);
        System.out.println(result);
    }
```

### 删除key

```java
    //删除单个key值
    @Test
    void delete() {
        String key = "my:user";
        Boolean result = redisTemplate.delete(key);
        System.out.println(result);
    }

    //删除多个key值
    @Test
    void deletes() {
        List<String> keys = new ArrayList<>();
        keys.add("my:user");
        keys.add("my:userList");
        Long result = redisTemplate.delete(keys);
        System.out.println(result);
    }
```

### 设置过期时间

```java
    //设置过期时间
    @Test
    void expire() {
        String key = "my:user";
        long timeout = 1;
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.HOURS);
        System.out.println(result);
    }

    // 设置指定时间过期
    // 如果超过当前时间则key会被清除
    @Test
    void expireAt() {
        String key = "my:user";
        String dateTime = "2025-12-12 22:22:22";
        Boolean result = redisTemplate.expireAt(key, DateUtil.parse(dateTime));
        System.out.println(result);
    }

    //返回剩余过期时间并且指定时间单位
    @Test
    void getExpire() {
        String key = "my:user";
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println(expire);
    }
```

### 查找匹配的key值

```java
    // 查找匹配的key值，返回一个Set集合类型
    @Test
    void keysAndValues() {
        // 返回所有key，保证这些key的值的数据类型一致
        String pattern = "my:user:data:*";
        Set<String> keys = redisTemplate.keys(pattern);
        keys.forEach(System.out::println);
        System.out.println(keys.size());
        // 返回所有value
        List<UserInfoEntity> values = (List<UserInfoEntity>) redisTemplate.opsForValue().multiGet(keys);
        values.forEach(value-> System.out.println(value.getName()));
    }
```

### 自增值increment

```java
    //以增量的方式将double值存储在变量中
    @Test
    void incrementDouble() {
        String key = "my:double";
        double delta = 0.1;
        Double result = redisTemplate.opsForValue().increment(key, delta);
        System.out.println(result);
    }
    //通过increment(K key, long delta)方法以增量方式存储long值（正值则自增，负值则自减）
    @Test
    void incrementLong() {
        String key = "my:long";
        long delta = 1;
        Long result = redisTemplate.opsForValue().increment(key, delta);
        System.out.println(result);
    }
```



## 使用List

### 创建测试类

```java
/**
 * Redis List相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisListTests {
    private final RedisTemplate redisTemplate;
    
}
```

### rightPush 队列 先进先出

```java
    /**
     * 功能: 将指定的值 value 添加到列表 key 的 右端（尾部）。
     * 使用场景: 通常用于像队列一样的操作，先进先出（FIFO）。例如，添加数据到消息队列的末尾，等待后续的消费
     */
    @Test
    void rightPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().rightPush(key, user);
    }

    @Test
    void rightPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().rightPushAll(key, list);
    }

    /**
     * 用于从Redis的列表中 弹出并移除 右端（尾部）的元素。这个操作类似于从队列的尾部取出元素。
     */
    @Test
    void rightPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().rightPop(key);
        System.out.println(userInfoEntity.getId());
    }
```

### leftPush 栈 先进后出

```java
    /**
     * 功能: 将指定的值 value 添加到列表 key 的 左端（头部）。
     * 使用场景: 通常用于像栈一样的操作，后进先出（LIFO）。例如，添加数据到列表的开头。
     */
    @Test
    void leftPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().leftPush(key, user);
    }

    @Test
    void leftPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().leftPushAll(key, list);
    }

    /**
     * 用于从 Redis 列表中 弹出并移除 左端（头部）元素的方法。这个操作类似于从队列的头部取出元素。
     */
    @Test
    void leftPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().leftPop(key);
        System.out.println(userInfoEntity.getId());
    }
```

### 获取列表元素

```java
    // 获取列表指定范围内的元素(start开始位置, 0是开始位置，end 结束位置, -1返回所有)
    @Test
    void range() {
        String key = "my:list:userList";
        long start = 0;
        long end = -1;
        List<UserInfoEntity> result = redisTemplate.opsForList().range(key, start, end);
        System.out.println(result);
    }
```



## 使用Hash

### 创建测试类

```java
/**
 * Redis HashMap相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisHashMapTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 新增hashMap值
    @Test
    void put() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForHash().put(key, hashKey, user);
    }

    @Test
    void putAll() {
        Map<String, Object> map = new HashMap<>();
        List<UserInfoEntity> list = new InitData().getList();
        list.forEach(user -> map.put("user" + user.getId(), user));
        redisTemplate.opsForHash().putAll("my:hashmap:userList", map);
    }
```

### 获取数据

```java
    // 获取hashMap值，不存在为null
    @Test
    void get() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity result = (UserInfoEntity) redisTemplate.opsForHash().get(key, hashKey);
        System.out.println(result);
        System.out.println(result.getName());
    }

    // 获取Key
    @Test
    void getKey() {
        String key = "my:hashmap:user";
        Set keys = redisTemplate.opsForHash().keys(key);
        String next = (String) keys.iterator().next();
        System.out.println(next);
    }

    // 获取多个hashMap的值
    @Test
    void multiGet() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> list = (List<UserInfoEntity>) redisTemplate.opsForHash().multiGet(key, Arrays.asList("user1", "user2"));
        System.out.println(list);
        list.forEach(user -> System.out.println(user.getName()));
    }

    // 获取所有hashMap的值
    @Test
    void getAll() {
        String key = "my:hashmap:userList";
        Map<String, UserInfoEntity> entries = (Map<String, UserInfoEntity>) redisTemplate.opsForHash().entries(key);
        System.out.println(entries);
        entries.values().forEach(value-> System.out.println(value.getName()));
    }

    // 获取所有hashMap的key值
    @Test
    void keys() {
        String key = "my:hashmap:userList";
        Set<String> keys = (Set<String>) redisTemplate.opsForHash().keys(key);
        System.out.println(keys);
    }

    // 获取所有hashMap的key值
    @Test
    void values() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> values = (List<UserInfoEntity>) redisTemplate.opsForHash().values(key);
        System.out.println(values);
        values.forEach(user -> System.out.println(user.getName()));
    }
```

### 删除数据

```java
    // 删除一个或者多个hash表字段
    @Test
    void delete() {
        String key = "my:hashmap:userList";
        String hashKey1 = "user1";
        String hashKey2 = "user2";
        Long result = redisTemplate.opsForHash().delete(key, hashKey1, hashKey2);
        System.out.println(result);
    }
```

### 匹配获取键值对

```java
    // 匹配获取键值对
    @Test
    void scan() {
        String key = "my:hashmap:userList";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("user1*")
                .build();
        Cursor<Map.Entry<Object, Object>> result = redisTemplate.opsForHash().scan(key, scanOptions);
        while (result.hasNext()) {
            Map.Entry<Object, Object> entry = result.next();
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
    }
```

## 使用Set

### 创建测试类

```java
/**
 * Redis Set集合相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisSetTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 添加数据
    @Test
    void add() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 100));
        }
        redisTemplate.opsForSet().add(key, list.toArray(new Integer[0]));
    }
```

### 获取数据

```java
    // 获取集合中的所有数据
    @Test
    void members() {
        String key = "my:set:number";
        Set<Integer> members = (LinkedHashSet<Integer>) redisTemplate.opsForSet().members(key);
        System.out.println(members);
    }
```

### 删除数据

```java
    // 删除数据
    @Test
    void remove() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 1000));
        }
        redisTemplate.opsForSet().remove(key, list.toArray(new Integer[0]));
    }

    // 删除并且返回一个随机的元素
    @Test
    void pop() {
        String key = "my:set:number";
        Integer result1 = (Integer) redisTemplate.opsForSet().pop(key);
        List<Integer> result2 = (ArrayList<Integer>) redisTemplate.opsForSet().pop(key, 2); // pop多个
        System.out.println(result1);
        System.out.println(result2);
    }
```

### 查询数据

```java
    // 判断集合是否包含value
    @Test
    void isMember() {
        String key = "my:set:number";
        String value = "60";
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        System.out.println(result);
    }

    // 获取两个集合的交集
    @Test
    void intersect() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().intersect(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的交集，并将结果存储到新的key
    @Test
    void intersectAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:intersect";
        redisTemplate.opsForSet().intersectAndStore(key1, key2, destKey);
    }

    // 获取两个集合的并集
    @Test
    void union() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().union(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void unionAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:union";
        redisTemplate.opsForSet().unionAndStore(key1, key2, destKey);
    }

    // 获取两个集合的差集
    @Test
    void difference() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().difference(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void differenceAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:difference";
        redisTemplate.opsForSet().differenceAndStore(key1, key2, destKey);
    }

    // 随机获取集合中的一个元素
    @Test
    void randomMember() {
        String key = "my:set:number";
        Integer result = (Integer) redisTemplate.opsForSet().randomMember(key);
        System.out.println(result);
    }

    // 随机获取集合中count个元素
    @Test
    void randomMembers() {
        String key = "my:set:number";
        List<Integer> strings = (ArrayList<Integer>) redisTemplate.opsForSet().randomMembers(key, 2);
        System.out.println(strings);
    }

    // 模糊匹配数据
    @Test
    void scan() {
        String key = "my:set:number";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("2*")
                .build();
        Cursor result1 = redisTemplate.opsForSet().scan(key, scanOptions);
        while (result1.hasNext()) {
            Object next = result1.next();
            System.out.println(next);
        }
    }
```

## 使用ZSet

### 创建测试类

```java
/**
 * Redis ZSet集合相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisZSetTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 添加元素(有序集合是按照元素的score值由小到大进行排列)
    @Test
    void add() {
        String key = "my:zset:number";
        String value = "value";
        double score = 1.1;
        Boolean result = redisTemplate.opsForZSet().add(key, value, score);
        System.out.println(result);
    }
    @Test
    void addMany() {
        String key = "my:zset:number";
        for (int i = 0; i < 30; i++) {
            String value = "value" + i;
            double score = RandomUtil.randomDouble(0,1,2, RoundingMode.UP);
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            System.out.println(result);
        }
    }
```

### 删除数据

```java
    // 删除元素
    @Test
    void remove() {
        String key = "my:zset:number";
        String value1 = "value1";
        String value2 = "value2";
        Long result = redisTemplate.opsForZSet().remove(key, value1, value2);
        System.out.println(result);
    }

    // 移除指定索引位置处的成员
    @Test
    void removeRange() {
        String key = "my:zset:number";
        long start = 1;
        long end = 3;
        Long result = redisTemplate.opsForZSet().removeRange(key, start, end);
        System.out.println(result);
    }

    // 移除指定score范围的集合成员
    @Test
    void removeRangeByScore() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Long result = redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        System.out.println(result);
    }
```

### 查询数据

```java
    // 返回元素在集合的排名,有序集合是按照元素的score值由小到大排列
    @Test
    void rank() {
        String key = "my:zset:number";
        String value = "value10";
        Long result = redisTemplate.opsForZSet().rank(key, value);
        System.out.println(result);
    }

    // 返回元素在集合的排名,按元素的score值由大到小排列
    @Test
    void reverseRank() {
        String key = "my:zset:number";
        String value = "value10";
        Long result = redisTemplate.opsForZSet().reverseRank(key, value);
        System.out.println(result);
    }

    // 获取集合中给定区间的元素(start 开始位置，end 结束位置, -1查询所有)
    @Test
    void reverseRangeWithScores() {
        String key = "my:zset:number";
        long start = 1;
        long end = 4;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        List<ZSetOperations.TypedTuple<String>> list = new ArrayList<>(typedTuples);
        list.forEach(data -> System.out.println("value=" + data.getValue() + ", score=" + data.getScore()));
    }

    // 按照Score值查询集合中的元素，结果从小到大排序
    @Test
    void reverseRangeByScore() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Set<String> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
        System.out.println(result);
    }

    // 按照Score值查询集合中的元素，结果从小到大排序
    @Test
    void reverseRangeByScoreWithScores() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
        List<ZSetOperations.TypedTuple<String>> list = new ArrayList<>(typedTuples);
        list.forEach(data -> System.out.println("value=" + data.getValue() + ", score=" + data.getScore()));
    }

    // 从高到低的排序集中获取分数在最小和最大值之间的元素
    @Test
    void reverseRangeByScoreOffset() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        long offset = 1;
        long count = 3;
        Set<String> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
        System.out.println(result);
    }

    // 根据score值获取集合元素数量
    @Test
    void count() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Long count = redisTemplate.opsForZSet().count(key, min, max);
        System.out.println(count);
    }

    // 获取集合的大小
    @Test
    void size() {
        String key = "my:zset:number";
        Long size = redisTemplate.opsForZSet().size(key);
        Long zCard = redisTemplate.opsForZSet().zCard(key);
        System.out.println(size);
        System.out.println(zCard);
    }

    // 获取集合中key、value元素对应的score值
    @Test
    void score() {
        String key = "my:zset:number";
        String value = "value";
        Double score = redisTemplate.opsForZSet().score(key, value);
        System.out.println(score);
    }

    // 获取所有元素
    @Test
    void scan() {
        String key = "my:zset:number";
        Cursor<ZSetOperations.TypedTuple<String>> scan = redisTemplate.opsForZSet().scan(key, ScanOptions.NONE);
        while (scan.hasNext()) {
            ZSetOperations.TypedTuple<String> item = scan.next();
            System.out.println(item.getValue() + ":" + item.getScore());
        }
    }
```

### 查询并存储数据

```java
    // 获取key和otherKey的并集并存储在destKey中（其中otherKeys可以为单个字符串或者字符串集合）
    @Test
    void unionAndStore() {
        String key = "my:zset:number";
        String otherKey = "my:zset:number2";
        String destKey = "my:zset:number3";
        Long result = redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
        System.out.println(result);
    }

    // 获取key和otherKey的交集并存储在destKey中（其中otherKeys可以为单个字符串或者字符串集合）
    @Test
    void intersectAndStore() {
        String key = "my:zset:number";
        String otherKey = "my:zset:number2";
        String destKey = "my:zset:number4";
        Long result = redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
        System.out.println(result);
    }
```

