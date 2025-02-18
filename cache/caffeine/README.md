# Caffeine

Caffeine 是一个高性能的 Java 缓存库，设计目标是提供快速、低延迟的缓存操作。它支持自动过期、基于容量的限制、异步加载等功能，能够有效提升应用性能。与其他缓存解决方案相比，Caffeine 在性能和内存占用上表现优异，广泛用于需要高频次缓存访问的场景。

- [官网地址](https://github.com/ben-manes/caffeine)

## 基础配置

### 添加依赖

```xml
<!-- Caffeine 依赖 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 配置Caffeine缓存

```java
package local.ateng.java.caffeine.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<Long, String> caffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)                      // 设置缓存的最大数量
                .expireAfterWrite(10, TimeUnit.MINUTES) // 设置写入后 10 分钟过期
                .build(); // 构建 Cache
    }

}
```



## 使用Caffeine缓存

### 创建服务

```java
package local.ateng.java.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Cache<Long, String> cache;

    @Autowired
    public UserService(Cache<Long, String> cache) {
        this.cache = cache;
    }

    public String getUserById(Long userId) {
        // 尝试从缓存中获取数据
        String user = cache.getIfPresent(userId);

        if (user == null) {
            // 缓存未命中，模拟从数据库获取数据
            System.out.println("Cache miss: Loading user from DB...");
            user = "User-" + userId;  // 这里模拟从数据库获取
            // 将数据放入缓存
            cache.put(userId, user);
        } else {
            System.out.println("Cache hit: Returning user from cache...");
        }

        return user;
    }

    // 清空缓存
    public void clearCache(Long userId) {
        cache.invalidate(userId);
    }

    // 清空所有缓存
    public void clearAllCache() {
        cache.invalidateAll();
    }
}
```

### 测试缓存

```java
package local.ateng.java.caffeine;

import local.ateng.java.caffeine.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testCaching() {
        // 第一次调用，会从“数据库”加载
        String user1 = userService.getUserById(1L);
        System.out.println(user1);  // 输出 Cache miss

        // 第二次调用，会从缓存获取
        String user2 = userService.getUserById(1L);
        System.out.println(user2);  // 输出 Cache hit
    }
}
```

![image-20250218153526611](./assets/image-20250218153526611.png)
