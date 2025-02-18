# JetCache

JetCache是一个基于Java的缓存系统封装，提供统一的API和注解来简化缓存的使用。 JetCache提供了比SpringCache更加强大的注解，可以原生的支持TTL、两级缓存、分布式自动刷新，还提供了`Cache`接口用于手工缓存操作。 当前有四个实现，`RedisCache`、`TairCache`（此部分未在github开源）、`CaffeineCache`(in memory)和一个简易的`LinkedHashMapCache`(in memory)，要添加新的实现也是非常简单的。

全部特性:

- 通过统一的API访问Cache系统
- 通过注解实现声明式的方法缓存，支持TTL和两级缓存
- 通过注解创建并配置`Cache`实例
- 针对所有`Cache`实例和方法缓存的自动统计
- Key的生成策略和Value的序列化策略是可以配置的
- 分布式缓存自动刷新，分布式锁 (2.2+)
- 异步Cache API (2.2+，使用Redis的lettuce客户端时)
- Spring Boot支持

参考：[官网文档](https://github.com/alibaba/jetcache/blob/master/docs/CN/Readme.md)



## 本地缓存

### 添加依赖

```xml
<jetcache.version>2.7.7</jetcache.version>
<!-- JetCache 依赖 -->
<dependency>
    <groupId>com.alicp.jetcache</groupId>
    <artifactId>jetcache-autoconfigure</artifactId>
    <version>${jetcache.version}</version>
</dependency>
```

### 编辑配置文件

配置文件参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/Config.md)

```yaml
# JetCache 配置
jetcache:
  # 本地缓存配置
  local:
    default:
      type: caffeine  # 缓存类型：linkedhashmap 或 caffeine
      limit: 100       # 缓存实例的最大元素数
      keyConvertor: fastjson2 # key 序列化方式，支持：fastjson2/jackson
      expireAfterWriteInMillis: 100000  # 缓存过期时间（毫秒）
```

### 启动缓存

```java
package local.ateng.java.jetcache;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = {"local.ateng.java"})
public class DBJetCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBJetCacheApplication.class, args);
    }

}
```

### 使用方法注解缓存

#### 创建服务

```java
package local.ateng.java.jetcache.local;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LocalService {

    // 使用 @Cached 注解缓存方法的返回值
    @Cached(cacheType = CacheType.LOCAL, name = "myCache", key = "#id", expire = 2, timeUnit = TimeUnit.MINUTES)
    public String getDataById(int id) {
        // 模拟从数据库或其他数据源获取数据
        String data = "Data for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheUpdate 注解更新缓存
    @CacheUpdate(name = "myCache", key = "#id", value = "#result")
    public String updateData(int id, String newData) {
        // 模拟更新数据源
        String data = "newData " + newData + " for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheInvalidate 注解删除缓存
    @CacheInvalidate(name = "myCache", key = "#id")
    public void deleteData(int id) {
        // 模拟删除数据源
        String data = "Data for id " + id;
        log.info(data);
    }

}
```

#### 创建接口

```java
package local.ateng.java.jetcache.local;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/local")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalController {

    private final LocalService localService;

    @GetMapping("/data/{id}")
    public String getData(@PathVariable int id) {
        return localService.getDataById(id);
    }

    @GetMapping("/update/{id}/{newData}")
    public String updateData(@PathVariable int id, @PathVariable String newData) {
        return localService.updateData(id, newData);
    }

    @GetMapping("/delete/{id}")
    public void deleteData(@PathVariable int id) {
        localService.deleteData(id);
    }

}
```

#### 使用缓存

1. 先调用 get 获取数据并写入缓存：
2. 调用 update 更新缓存数据和重置过期时间
3. 调用 delete 删除缓存

![image-20250217165127555](./assets/image-20250217165127555.png)

### 使用手动缓存

#### 使用CacheManager创建Cache实例

```java
package local.ateng.java.jetcache.local;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JetcacheLocalConfig {
    private final CacheManager cacheManager;
    private Cache<Integer, Object> localCache;

    @PostConstruct
    public void init() {
        QuickConfig qc = QuickConfig.newBuilder("localCache:")
                .expire(Duration.ofSeconds(3600))
                .cacheType(CacheType.LOCAL)
                .build();
        localCache = cacheManager.getOrCreateCache(qc);
    }

    @Bean
    public Cache<Integer, Object> localCache() {
        return localCache;
    }

}
```

#### 使用缓存

```java
@RestController
@RequestMapping("/local")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalController {

    private final Cache<Integer, String> manualCache;

    @GetMapping("/manual/put/{id}")
    public void manualPut(@PathVariable int id) {
        manualCache.put(id, "Hello World");
    }

    @GetMapping("/manual/get/{id}")
    public String manualGet(@PathVariable int id) {
        return manualCache.get(id);
    }

    @GetMapping("/manual/list")
    public Map<Integer, String> manualList() {
        return manualCache.getAll(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

}
```



## 远程缓存

### 基础配置

#### Jedis

参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/RedisWithJedis.md)

**添加依赖**

```xml
<jetcache.version>2.7.7</jetcache.version>
<!-- JetCache 依赖 -->
<dependency>
    <groupId>com.alicp.jetcache</groupId>
    <artifactId>jetcache-starter-redis</artifactId>
    <version>${jetcache.version}</version>
</dependency>
```

**编辑配置文件**

配置文件参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/Config.md)

```yaml
---
# JetCache 配置
jetcache:
  statIntervalMinutes: 15
  areaInCacheName: false
  # 本地缓存配置
  local:
    default:
      type: caffeine  # 缓存类型：linkedhashmap 或 caffeine
      limit: 100       # 缓存实例的最大元素数
      keyConvertor: fastjson2 # key 序列化方式，支持：fastjson2/jackson
      expireAfterWriteInMillis: 100000  # 缓存过期时间（毫秒）
  # 远程缓存配置
  remote:
    default:
      type: redis      # 缓存类型
      keyConvertor: fastjson2  # key 序列化方式
      broadcastChannel: ${spring.application.name}  # 缓存广播频道
      valueEncoder: java  # 值序列化方式
      valueDecoder: java  # 值反序列化方式
      poolConfig:
        minIdle: 5       # 连接池最小空闲连接数
        maxIdle: 20      # 连接池最大空闲连接数
        maxTotal: 50     # 连接池最大连接数
      host: 192.168.1.10  # Redis 主机地址
      port: 42784         # Redis 端口
      password: Admin@123 # Redis 密码
      database: 41
      # 通用配置
      timeout: 2000
      connectionTimeout: 2000
      soTimeout: 2000
      maxAttempt: 5
      defaultExpireInMillis: 60000
      keyPrefix: "ateng:jetcache:"
```

#### Lettuce

参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/RedisWithLettuce.md)

**添加依赖**

```xml
<jetcache.version>2.7.7</jetcache.version>
<!-- JetCache 依赖 -->
<dependency>
    <groupId>com.alicp.jetcache</groupId>
    <artifactId>jetcache-starter-redis-lettuce</artifactId>
    <version>${jetcache.version}</version>
</dependency>
```

**编辑配置文件**

lettuce 使用 Netty 建立并复用单个连接实现 redis 的通信，因此无须配置连接池。

配置文件参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/Config.md)

```yaml
---
# JetCache 配置
jetcache:
  statIntervalMinutes: 15
  areaInCacheName: false
  # 本地缓存配置
  local:
    default:
      type: caffeine  # 缓存类型：linkedhashmap 或 caffeine
      limit: 100       # 缓存实例的最大元素数
      keyConvertor: fastjson2 # key 序列化方式，支持：fastjson2/jackson
      expireAfterWriteInMillis: 100000  # 缓存过期时间（毫秒）
  # 远程缓存配置
  remote:
    default:
      type: redis.lettuce      # 缓存类型
      keyConvertor: fastjson2  # key 序列化方式
      broadcastChannel: ${spring.application.name}  # 缓存广播频道
      valueEncoder: java  # 值序列化方式
      valueDecoder: java  # 值反序列化方式
      uri: redis://:Admin%40123@192.168.1.10:42784/41 # Redis URI，格式：redis://[username:password@]host:port[/database]，特殊密码需要encode转码
      # 通用配置
      timeout: 2000
      connectionTimeout: 2000
      soTimeout: 2000
      maxAttempt: 5
      defaultExpireInMillis: 60000
      keyPrefix: "ateng:jetcache:"
```

#### Redisson

参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/RedisWithRedisson.md)

**添加依赖**

```xml
<jetcache.version>2.7.7</jetcache.version>
<redisson.version>3.44.0</redisson.version>
<!-- JetCache 依赖 -->
<dependency>
    <groupId>com.alicp.jetcache</groupId>
    <artifactId>jetcache-starter-redisson</artifactId>
    <version>${jetcache.version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!-- Redisson 依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

**编辑配置文件**

redisson 不支持通过配置连接，而是通过获取 Spring bean 拿到 Client 实例，因此在 Spring Boot 环境下使用 redisson 支持时，客户端特定的参数只需要配置一个 `redissonClient` 即可。

配置文件参考：[官网链接](https://github.com/alibaba/jetcache/blob/master/docs/CN/Config.md)

```yaml
---
# JetCache 配置
jetcache:
  statIntervalMinutes: 15
  areaInCacheName: false
  # 本地缓存配置
  local:
    default:
      type: caffeine  # 缓存类型：linkedhashmap 或 caffeine
      limit: 100       # 缓存实例的最大元素数
      keyConvertor: fastjson2 # key 序列化方式，支持：fastjson2/jackson
      expireAfterWriteInMillis: 100000  # 缓存过期时间（毫秒）
  # 远程缓存配置
  remote:
    default:
      type: redisson      # 缓存类型
      redissonClient: redissonClient
      keyConvertor: fastjson2  # key 序列化方式
      broadcastChannel: ${spring.application.name}  # 缓存广播频道
      valueEncoder: java  # 值序列化方式
      valueDecoder: java  # 值反序列化方式
      defaultExpireInMillis: 60000
      keyPrefix: "ateng:jetcache:"
---
# Redisson 的相关配置
redisson:
  config: |
    singleServerConfig:
      address: redis://192.168.1.10:42784
      password: Admin@123
      database: 41
      clientName: redisson-client
      connectionPoolSize: 64      # 最大连接数
      connectionMinimumIdleSize: 24 # 最小空闲连接
      idleConnectionTimeout: 10000 # 空闲连接超时时间（ms）
      connectTimeout: 5000        # 连接超时时间
      timeout: 3000               # 命令等待超时
      retryAttempts: 3            # 命令重试次数
      retryInterval: 1500         # 命令重试间隔（ms）
    threads: 16                   # 处理Redis事件的线程数
    nettyThreads: 32              # Netty线程数
    codec: !<org.redisson.codec.JsonJacksonCodec> {} # 推荐JSON序列化
```

**创建配置属性**

```java
package local.ateng.java.jetcache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "redisson")
@Configuration
@Data
public class RedissonProperties {
    private String config;
}
```

**创建客户端Bean**

```java
package local.ateng.java.jetcache.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedissonConfig {
    private final RedissonProperties redissonProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonProperties.getConfig());
        return Redisson.create(config);
    }

}
```

### 启动缓存

```java
package local.ateng.java.jetcache;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = {"local.ateng.java"})
public class DBJetCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBJetCacheApplication.class, args);
    }

}
```

### 使用方法注解缓存

#### 创建服务

```java
package local.ateng.java.jetcache.remote;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RemoteService {

    // 使用 @Cached 注解缓存方法的返回值
    @Cached(cacheType = CacheType.REMOTE, name = "my:cache", key = "#id", expire = 2, timeUnit = TimeUnit.MINUTES)
    public String getDataById(int id) {
        // 模拟从数据库或其他数据源获取数据
        String data = "Data for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheUpdate 注解更新缓存
    @CacheUpdate(name = "my:cache", key = "#id", value = "#result")
    public String updateData(int id, String newData) {
        // 模拟更新数据源
        String data = "newData " + newData + " for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheInvalidate 注解删除缓存
    @CacheInvalidate(name = "my:cache", key = "#id")
    public void deleteData(int id) {
        // 模拟删除数据源
        String data = "Data for id " + id;
        log.info(data);
    }

}
```

#### 创建接口

```java
package local.ateng.java.jetcache.remote;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/remote")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RemoteController {

    private final RemoteService remoteService;

    @GetMapping("/data/{id}")
    public String getData(@PathVariable int id) {
        return remoteService.getDataById(id);
    }

    @GetMapping("/update/{id}/{newData}")
    public String updateData(@PathVariable int id, @PathVariable String newData) {
        return remoteService.updateData(id, newData);
    }

    @GetMapping("/delete/{id}")
    public void deleteData(@PathVariable int id) {
        remoteService.deleteData(id);
    }

}
```

#### 使用缓存

1. 先调用 get 获取数据并写入缓存：
2. 调用 update 更新缓存数据和重置过期时间
3. 调用 delete 删除缓存

![image-20250218082543360](./assets/image-20250218082543360.png)

### 使用手动缓存

#### 使用CacheManager创建Cache实例

```java
package local.ateng.java.jetcache.remote;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JetcacheRemoteConfig {
    private final CacheManager cacheManager;
    private Cache<Integer, String> manualRemoteCache;

    @PostConstruct
    public void init() {
        QuickConfig qc = QuickConfig.newBuilder("manualRemoteCache:")
                .expire(Duration.ofSeconds(3600))
                .cacheType(CacheType.REMOTE)
                .build();
        manualRemoteCache = cacheManager.getOrCreateCache(qc);
    }

    @Bean
    public Cache<Integer, String> manualRemoteCache() {
        return manualRemoteCache;
    }

}
```

#### 使用缓存

```java
@RestController
@RequestMapping("/remote")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RemoteController {

    private final Cache<Integer, String> manualRemoteCache;

    @GetMapping("/manual/put/{id}")
    public void manualPut(@PathVariable int id) {
        manualRemoteCache.put(id, "Hello World");
    }

    @GetMapping("/manual/get/{id}")
    public String manualGet(@PathVariable int id) {
        return manualRemoteCache.get(id);
    }

    @GetMapping("/manual/list")
    public Map<Integer, String> manualList() {
        return manualRemoteCache.getAll(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

}
```



## 二级缓存

### 注解

```java
@Cached(cacheType = CacheType.BOTH, name = "my:cache", key = "#id", expire = 2, timeUnit = TimeUnit.MINUTES, syncLocal = true)
public String getDataById(int id) {
    // 模拟从数据库或其他数据源获取数据
    String data = "Data for id " + id;
    log.info(data);
    return data;
}
```

### 实例

```java
@PostConstruct
public void init() {
    QuickConfig qc = QuickConfig.newBuilder("manualRemoteCache:")
            .expire(Duration.ofSeconds(3600))
            .cacheType(CacheType.BOTH)
            .syncLocal(true)
            .build();
    manualRemoteCache = cacheManager.getOrCreateCache(qc);
}
```

