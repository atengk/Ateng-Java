# Redisson 项目实战案例

Redisson 是一个基于 Redis 的 Java 客户端，提供了丰富的分布式数据结构和服务，如分布式锁、集合、队列、Map 等。它简化了与 Redis 的交互，并且支持高可用性、分布式事务、监控等特性，非常适合构建高性能和高可扩展性的应用。

- [官网链接](https://redisson.org)



## 基础配置

### 添加依赖

```xml
<!-- 项目属性 -->
<properties>
    <redisson.version>3.52.0</redisson.version>
</properties>
<!-- Redisson 依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

### 编辑配置文件

#### 单机配置

```yaml
---
# Redisson 的相关配置
redisson:
  config: |
    singleServerConfig:
      address: redis://192.168.1.12:40003
      password: Admin@123
      database: 0
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

#### 集群配置

```yaml
---
# Redisson 的相关配置
redisson:
  config: |
    clusterServersConfig:
      nodeAddresses:
        - "redis://192.168.1.41:6379"
        - "redis://192.168.1.42:6379"
        - "redis://192.168.1.43:6379"
        - "redis://192.168.1.44:6379"
        - "redis://192.168.1.45:6379"
        - "redis://192.168.1.46:6379"
      password: "Admin@123"       # 集群密码（如果集群有密码）
      scanInterval: 2000          # 集群状态扫描间隔（ms）
      readMode: "SLAVE"           # 读取模式（MASTER/SLAVE/MASTER_SLAVE）
      subscriptionMode: "SLAVE"  # 订阅模式（MASTER/SLAVE/MASTER_SLAVE）
      loadBalancer: !<org.redisson.connection.balancer.RoundRobinLoadBalancer> {} # 负载均衡策略
      masterConnectionPoolSize: 64      # 主节点连接池大小
      slaveConnectionPoolSize: 64       # 从节点连接池大小
      masterConnectionMinimumIdleSize: 24 # 主节点最小空闲连接
      slaveConnectionMinimumIdleSize: 24  # 从节点最小空闲连接
      idleConnectionTimeout: 10000      # 空闲连接超时时间（ms）
      connectTimeout: 5000              # 连接超时时间
      timeout: 3000                     # 命令等待超时
      retryAttempts: 3                  # 命令重试次数
      retryInterval: 1500               # 命令重试间隔（ms）
      failedSlaveReconnectionInterval: 3000 # 从节点重连间隔（ms）
      failedSlaveCheckInterval: 60000   # 从节点健康检查间隔（ms）
    threads: 16                         # 处理Redis事件的线程数
    nettyThreads: 32                    # Netty线程数
    codec: !<org.redisson.codec.JsonJacksonCodec> {} # 推荐JSON序列化
```

### 创建配置属性

```java
package io.github.atengk.redisson.config;

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

### 创建客户端Bean

```java
package io.github.atengk.redisson.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class RedissonConfig {
    
    private final RedissonProperties redissonProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonProperties.getConfig());
        return Redisson.create(config);
    }

}
```



## 分布式锁（防并发超卖 / 资源互斥控制）

用于秒杀下单、库存扣减等场景，保证同一资源在分布式环境下只能被一个请求操作

实现基于 Redisson 的分布式锁，支持自动续期（watchdog）、可重入、异常安全释放

```java
package io.github.atengk.lock;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 执行业务（带分布式锁）
     *
     * @param lockKey 锁Key
     * @param waitTime 最大等待时间（秒）
     * @param leaseTime 锁持有时间（秒，-1 表示自动续期）
     * @param business 业务逻辑
     */
    public void executeWithLock(String lockKey,
                                long waitTime,
                                long leaseTime,
                                Runnable business) {

        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.info("获取分布式锁成功，lockKey={}", lockKey);

            business.run();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey={}", lockKey, e);
            throw new RuntimeException("系统异常");
        } finally {

            if (isLocked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.info("释放分布式锁成功，lockKey={}", lockKey);
                } catch (Exception e) {
                    log.error("释放分布式锁异常，lockKey={}", lockKey, e);
                }
            }

        }
    }

}
```

---

模拟秒杀扣减库存（防超卖）

```java
package io.github.atengk.service;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final DistributedLockService distributedLockService;

    private int stock = 10;

    /**
     * 秒杀下单
     *
     * @param userId 用户ID
     */
    public void seckill(Long userId) {

        String lockKey = "lock:seckill:stock";

        distributedLockService.executeWithLock(lockKey, 3, -1, () -> {

            if (stock <= 0) {
                log.warn("库存不足，userId={}", userId);
                throw new RuntimeException("库存不足");
            }

            stock--;

            log.info("扣减库存成功，userId={}，剩余库存={}", userId, stock);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        });

    }

}
```

提供一个简单的 Controller，用于模拟并发请求触发秒杀接口，验证分布式锁效果

```java id="c1k9dp"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀接口
     * 
     * curl "http://localhost:8080/seckill/do?userId=1"
     *
     * @param userId 用户ID
     * @return 执行结果
     */
    @PostMapping("/do")
    public String seckill(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        try {
            seckillService.seckill(userId);
            return "秒杀成功";
        } catch (Exception e) {
            log.error("秒杀失败，userId={}", userId, e);
            return e.getMessage();
        }

    }

}
```

## 接口限流（防刷 / 防滥用）

用于短信验证码、登录接口、核心 API 防止高频请求冲击系统

基于 Redisson 的 RateLimiter 实现分布式限流（令牌桶算法）

```java id="rls9d2"
package io.github.atengk.limit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 分布式限流服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取令牌
     *
     * @param key 限流Key
     * @param permits 每秒允许的请求数
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, long permits) {

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, permits, 1, RateIntervalUnit.SECONDS);
        }

        boolean result = rateLimiter.tryAcquire(1);

        if (!result) {
            log.warn("触发限流，key={}", key);
        }

        return result;
    }

}
```

---

模拟短信接口限流（每秒最多 5 次）

```java id="sms8k3"
package io.github.atengk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RateLimitService rateLimitService;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     */
    public void sendSms(String phone) {

        String key = "rate:sms:" + phone;

        boolean allowed = rateLimitService.tryAcquire(key, 5);

        if (!allowed) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }

        log.info("发送短信成功，phone={}", phone);

    }

}
```

---

Controller 测试接口

```java id="ctl9x1"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 限流测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送短信验证码
     *
     * curl -X POST "http://localhost:8080/sms/send?phone=13800000000"
     *
     * @param phone 手机号
     * @return 执行结果
     */
    @PostMapping("/send")
    public String send(@RequestParam String phone) {

        if (ObjectUtil.isEmpty(phone)) {
            return "phone不能为空";
        }

        try {
            smsService.sendSms(phone);
            return "发送成功";
        } catch (Exception e) {
            log.error("发送短信失败，phone={}", phone, e);
            return e.getMessage();
        }

    }

}
```

## 排行榜（实时排序）

用于积分榜、打赏榜、热度榜等按分数实时排序的场景

基于 Redisson 的 RScoredSortedSet（ZSet）实现排行榜，支持加分、查询排名、获取 TopN

```java id="rank1a2"
package io.github.atengk.rank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 排行榜服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedissonClient redissonClient;

    private static final String RANK_KEY = "rank:score";

    /**
     * 增加分数
     *
     * @param userId 用户ID
     * @param score 分数
     */
    public void addScore(Long userId, double score) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        zSet.addScore(userId, score);

        log.info("增加分数成功，userId={}，score={}", userId, score);
    }

    /**
     * 获取用户排名（从1开始）
     *
     * @param userId 用户ID
     * @return 排名
     */
    public Integer getRank(Long userId) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        Integer rank = zSet.revRank(userId);

        if (rank == null) {
            return null;
        }

        return rank + 1;
    }

    /**
     * 获取 TopN 用户
     *
     * @param n 数量
     * @return 用户列表
     */
    public Collection<Long> topN(int n) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        return zSet.valueRangeReversed(0, n - 1);
    }

}
```

---

模拟打分业务（积分榜）

```java id="rank2b3"
package io.github.atengk.service;

import io.github.atengk.rank.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 积分业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final RankingService rankingService;

    /**
     * 增加积分
     *
     * @param userId 用户ID
     * @param score 分数
     */
    public void addScore(Long userId, double score) {

        rankingService.addScore(userId, score);

    }

    /**
     * 查询排名
     *
     * @param userId 用户ID
     * @return 排名
     */
    public Integer getRank(Long userId) {

        return rankingService.getRank(userId);

    }

}
```

---

Controller 测试接口

```java id="rank3c4"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.rank.RankingService;
import io.github.atengk.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * 排行榜测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankingController {

    private final ScoreService scoreService;
    private final RankingService rankingService;

    /**
     * 增加积分
     *
     * curl -X POST "http://localhost:8080/rank/add?userId=1&score=10"
     */
    @PostMapping("/add")
    public String add(@RequestParam Long userId,
                      @RequestParam Double score) {

        if (ObjectUtil.hasEmpty(userId, score)) {
            return "参数不能为空";
        }

        scoreService.addScore(userId, score);

        return "操作成功";
    }

    /**
     * 查询排名
     *
     * curl "http://localhost:8080/rank/get?userId=1"
     */
    @GetMapping("/get")
    public Object get(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return scoreService.getRank(userId);
    }

    /**
     * 获取TopN
     *
     * curl "http://localhost:8080/rank/top?n=5"
     */
    @GetMapping("/top")
    public Object top(@RequestParam Integer n) {

        if (ObjectUtil.isEmpty(n)) {
            return "n不能为空";
        }

        Collection<Long> result = rankingService.topN(n);

        return result;
    }

}
```

## 计数器（高并发计数）

用于点赞数、浏览量、访问次数等实时增长统计

基于 Redisson 的 RAtomicLong 实现分布式原子计数器，保证并发安全

```java id="cnt1a1"
package io.github.atengk.counter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 计数器服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CounterService {

    private final RedissonClient redissonClient;

    /**
     * 自增计数
     *
     * @param key 计数Key
     * @return 当前值
     */
    public long increment(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        long value = atomicLong.incrementAndGet();

        log.info("计数器自增，key={}，value={}", key, value);

        return value;
    }

    /**
     * 获取当前值
     *
     * @param key 计数Key
     * @return 当前值
     */
    public long get(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        return atomicLong.get();
    }

    /**
     * 重置计数
     *
     * @param key 计数Key
     */
    public void reset(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        atomicLong.set(0);

        log.info("计数器已重置，key={}", key);
    }

}
```

---

模拟点赞/浏览业务

```java id="cnt2b2"
package io.github.atengk.service;

import io.github.atengk.counter.CounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统计业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final CounterService counterService;

    /**
     * 点赞
     *
     * @param postId 帖子ID
     * @return 当前点赞数
     */
    public long like(Long postId) {

        String key = "like:post:" + postId;

        return counterService.increment(key);
    }

    /**
     * 浏览
     *
     * @param postId 帖子ID
     * @return 当前浏览量
     */
    public long view(Long postId) {

        String key = "view:post:" + postId;

        return counterService.increment(key);
    }

    /**
     * 获取点赞数
     */
    public long getLikeCount(Long postId) {
        return counterService.get("like:post:" + postId);
    }

}
```

---

Controller 测试接口

```java id="cnt3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 计数器测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 点赞
     *
     * curl -X POST "http://localhost:8080/stats/like?postId=1"
     */
    @PostMapping("/like")
    public Object like(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.like(postId);
    }

    /**
     * 浏览
     *
     * curl -X POST "http://localhost:8080/stats/view?postId=1"
     */
    @PostMapping("/view")
    public Object view(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.view(postId);
    }

    /**
     * 获取点赞数
     *
     * curl "http://localhost:8080/stats/like/count?postId=1"
     */
    @GetMapping("/like/count")
    public Object likeCount(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.getLikeCount(postId);
    }

}
```

## 分布式会话（Session 共享）

用于多节点部署下用户登录态共享，避免重复登录

基于 Redisson 的 RBucket 实现 Session 存储，结合 TTL 控制登录态过期

```java id="sess1a1"
package io.github.atengk.session;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 分布式会话服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedissonClient redissonClient;

    private static final String SESSION_KEY_PREFIX = "session:";

    /**
     * 创建会话
     *
     * @param token token
     * @param session 会话信息
     * @param ttl 过期时间（秒）
     */
    public void createSession(String token, UserSession session, long ttl) {

        RBucket<UserSession> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        bucket.set(session, ttl, TimeUnit.SECONDS);

        log.info("创建会话成功，token={}", token);
    }

    /**
     * 获取会话
     *
     * @param token token
     * @return 会话信息
     */
    public UserSession getSession(String token) {

        if (ObjectUtil.isEmpty(token)) {
            return null;
        }

        RBucket<UserSession> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        return bucket.get();
    }

    /**
     * 删除会话（登出）
     *
     * @param token token
     */
    public void deleteSession(String token) {

        RBucket<Object> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        bucket.delete();

        log.info("删除会话成功，token={}", token);
    }

    /**
     * 刷新过期时间
     *
     * @param token token
     * @param ttl 过期时间（秒）
     */
    public void refreshSession(String token, long ttl) {

        RBucket<Object> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        boolean success = bucket.expire(ttl, TimeUnit.SECONDS);

        if (success) {
            log.info("刷新会话成功，token={}", token);
        }

    }

    /**
     * 会话对象
     */
    @Data
    public static class UserSession implements Serializable {

        private Long userId;

        private String username;

    }

}
```

---

模拟登录/登出业务

```java id="sess2b2"
package io.github.atengk.service;

import cn.hutool.core.lang.UUID;
import io.github.atengk.session.SessionService;
import io.github.atengk.session.SessionService.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SessionService sessionService;

    /**
     * 登录
     *
     * @param userId 用户ID
     * @return token
     */
    public String login(Long userId) {

        String token = UUID.fastUUID().toString(true);

        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setUsername("user_" + userId);

        sessionService.createSession(token, session, 1800);

        log.info("用户登录成功，userId={}，token={}", userId, token);

        return token;
    }

    /**
     * 获取当前用户
     *
     * @param token token
     * @return 用户信息
     */
    public UserSession getCurrentUser(String token) {

        return sessionService.getSession(token);
    }

    /**
     * 登出
     *
     * @param token token
     */
    public void logout(String token) {

        sessionService.deleteSession(token);

        log.info("用户登出成功，token={}", token);
    }

}
```

---

Controller 测试接口

```java id="sess3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.AuthService;
import io.github.atengk.session.SessionService.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 会话测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     *
     * curl -X POST "http://localhost:8080/auth/login?userId=1"
     */
    @PostMapping("/login")
    public Object login(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return authService.login(userId);
    }

    /**
     * 获取当前用户
     *
     * curl "http://localhost:8080/auth/me?token=xxx"
     */
    @GetMapping("/me")
    public Object me(@RequestParam String token) {

        if (ObjectUtil.isEmpty(token)) {
            return "token不能为空";
        }

        UserSession session = authService.getCurrentUser(token);

        if (ObjectUtil.isEmpty(session)) {
            return "未登录或已过期";
        }

        return session;
    }

    /**
     * 登出
     *
     * curl -X POST "http://localhost:8080/auth/logout?token=xxx"
     */
    @PostMapping("/logout")
    public Object logout(@RequestParam String token) {

        if (ObjectUtil.isEmpty(token)) {
            return "token不能为空";
        }

        authService.logout(token);

        return "登出成功";
    }

}
```

## 用户签到（位图统计）

用于每日签到、连续签到、签到天数统计等场景

基于 Redisson 的 RBitSet 实现签到功能，按“年维度 + 天偏移量”存储

```java id="sign1a1"
package io.github.atengk.sign;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户签到服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {

    private final RedissonClient redissonClient;

    private static final String SIGN_KEY_PREFIX = "sign:";

    /**
     * 签到
     *
     * @param userId 用户ID
     */
    public void sign(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        bitSet.set(dayOfYear);

        log.info("签到成功，userId={}，dayOfYear={}", userId, dayOfYear);
    }

    /**
     * 判断当天是否签到
     *
     * @param userId 用户ID
     * @return 是否签到
     */
    public boolean isSigned(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        return bitSet.get(dayOfYear);
    }

    /**
     * 获取当年签到天数
     *
     * @param userId 用户ID
     * @return 签到天数
     */
    public long getSignCount(Long userId) {

        int year = DateUtil.year(new Date());

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        return bitSet.cardinality();
    }

    /**
     * 获取连续签到天数（从今天往前）
     *
     * @param userId 用户ID
     * @return 连续天数
     */
    public int getContinuousSignCount(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        int count = 0;

        for (int i = dayOfYear; i > 0; i--) {

            if (bitSet.get(i)) {
                count++;
            } else {
                break;
            }

        }

        return count;
    }

}
```

---

模拟签到业务

```java id="sign2b2"
package io.github.atengk.service;

import io.github.atengk.sign.SignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 签到业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignService {

    private final SignService signService;

    /**
     * 用户签到
     */
    public void sign(Long userId) {
        signService.sign(userId);
    }

    /**
     * 查询是否签到
     */
    public boolean isSigned(Long userId) {
        return signService.isSigned(userId);
    }

    /**
     * 查询签到总天数
     */
    public long count(Long userId) {
        return signService.getSignCount(userId);
    }

    /**
     * 查询连续签到
     */
    public int continuous(Long userId) {
        return signService.getContinuousSignCount(userId);
    }

}
```

---

Controller 测试接口

```java id="sign3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.UserSignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 签到测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
public class SignController {

    private final UserSignService userSignService;

    /**
     * 签到
     *
     * curl -X POST "http://localhost:8080/sign/do?userId=1"
     */
    @PostMapping("/do")
    public Object sign(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        userSignService.sign(userId);

        return "签到成功";
    }

    /**
     * 是否签到
     *
     * curl "http://localhost:8080/sign/check?userId=1"
     */
    @GetMapping("/check")
    public Object check(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.isSigned(userId);
    }

    /**
     * 签到总天数
     *
     * curl "http://localhost:8080/sign/count?userId=1"
     */
    @GetMapping("/count")
    public Object count(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.count(userId);
    }

    /**
     * 连续签到天数
     *
     * curl "http://localhost:8080/sign/continuous?userId=1"
     */
    @GetMapping("/continuous")
    public Object continuous(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.continuous(userId);
    }

}
```

## UV 统计（独立访客去重）

用于网站访问 UV、日活统计等大规模去重场景

基于 Redisson 的 RHyperLogLog 实现大规模去重统计，内存占用极低

```java id="uv1a1"
package io.github.atengk.uv;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * UV统计服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UvService {

    private final RedissonClient redissonClient;

    private static final String UV_KEY_PREFIX = "uv:";

    /**
     * 记录访问（按天）
     *
     * @param bizKey 业务标识（如：首页、商品页）
     * @param userFlag 用户标识（如IP、userId）
     */
    public void record(String bizKey, String userFlag) {

        String date = DateUtil.formatDate(new Date());

        String key = UV_KEY_PREFIX + bizKey + ":" + date;

        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);

        hyperLogLog.add(userFlag);

        log.info("记录UV成功，key={}，userFlag={}", key, userFlag);
    }

    /**
     * 获取当日UV
     *
     * @param bizKey 业务标识
     * @return UV数量
     */
    public long countToday(String bizKey) {

        String date = DateUtil.formatDate(new Date());

        String key = UV_KEY_PREFIX + bizKey + ":" + date;

        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);

        return hyperLogLog.count();
    }

}
```

---

模拟访问统计业务

```java id="uv2b2"
package io.github.atengk.service;

import io.github.atengk.uv.UvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访问统计业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final UvService uvService;

    /**
     * 访问页面
     *
     * @param page 页面标识
     * @param userFlag 用户标识（IP / userId）
     */
    public void visit(String page, String userFlag) {

        uvService.record(page, userFlag);

    }

    /**
     * 获取UV
     */
    public long getUv(String page) {

        return uvService.countToday(page);

    }

}
```

---

Controller 测试接口

```java id="uv3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * UV统计测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/uv")
@RequiredArgsConstructor
public class UvController {

    private final VisitService visitService;

    /**
     * 记录访问
     *
     * curl -X POST "http://localhost:8080/uv/visit?page=home&userFlag=ip_127.0.0.1"
     */
    @PostMapping("/visit")
    public Object visit(@RequestParam String page,
                        @RequestParam String userFlag) {

        if (ObjectUtil.hasEmpty(page, userFlag)) {
            return "参数不能为空";
        }

        visitService.visit(page, userFlag);

        return "记录成功";
    }

    /**
     * 获取今日UV
     *
     * curl "http://localhost:8080/uv/count?page=home"
     */
    @GetMapping("/count")
    public Object count(@RequestParam String page) {

        if (ObjectUtil.isEmpty(page)) {
            return "page不能为空";
        }

        return visitService.getUv(page);
    }

}
```

## 地理位置服务（LBS）

用于“附近的人 / 门店 / 外卖范围 / 距离计算”等功能

基于 Redisson 的 RGeo 实现地理位置存储与范围查询

```java id="geo1a1"
package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoEntry;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.api.geo.GeoSearchArgs;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地理位置服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private final RedissonClient redissonClient;

    private static final String GEO_KEY = "geo:store";

    /**
     * 添加位置（门店/用户）
     *
     * @param name 名称
     * @param longitude 经度
     * @param latitude 纬度
     */
    public void add(String name, double longitude, double latitude) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        geo.add(longitude, latitude, name);

        log.info("添加地理位置成功，name={}，lng={}，lat={}", name, longitude, latitude);
    }

    /**
     * 查询附近（指定坐标）
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param radius 半径（km）
     * @return 结果
     */
    public List<String> radius(double longitude, double latitude, double radius) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        return geo.search(
                GeoSearchArgs.from(longitude, latitude)
                        .radius(radius, GeoUnit.KILOMETERS)
        );
    }

    /**
     * 计算两点距离（米）
     *
     * @param name1 点1
     * @param name2 点2
     * @return 距离
     */
    public Double distance(String name1, String name2) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        return geo.dist(name1, name2, GeoUnit.METERS);
    }

    /**
     * 批量添加
     *
     * @param entries 坐标集合
     */
    public void batchAdd(List<GeoEntry> entries) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        geo.add(entries.toArray(new GeoEntry[0]));

        log.info("批量添加地理位置成功，size={}", entries.size());
    }

}
```

---

模拟门店位置业务

```java id="geo2b2"
package io.github.atengk.service;

import io.github.atengk.geo.GeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final GeoService geoService;

    /**
     * 添加门店
     */
    public void addStore(String name, double lng, double lat) {
        geoService.add(name, lng, lat);
    }

    /**
     * 查询附近门店
     */
    public List<String> nearby(double lng, double lat, double radius) {
        return geoService.radius(lng, lat, radius);
    }

    /**
     * 计算距离
     */
    public Double distance(String a, String b) {
        return geoService.distance(a, b);
    }

}
```

---

Controller 测试接口

```java id="geo3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地理位置测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {

    private final StoreService storeService;

    /**
     * 添加门店
     *
     * curl -X POST "http://localhost:8080/geo/add?name=store1&lng=116.4&lat=39.9"
     */
    @PostMapping("/add")
    public Object add(@RequestParam String name,
                      @RequestParam Double lng,
                      @RequestParam Double lat) {

        if (ObjectUtil.hasEmpty(name, lng, lat)) {
            return "参数不能为空";
        }

        storeService.addStore(name, lng, lat);

        return "添加成功";
    }

    /**
     * 查询附近门店
     *
     * curl "http://localhost:8080/geo/nearby?lng=116.4&lat=39.9&radius=3"
     */
    @GetMapping("/nearby")
    public Object nearby(@RequestParam Double lng,
                         @RequestParam Double lat,
                         @RequestParam Double radius) {

        if (ObjectUtil.hasEmpty(lng, lat, radius)) {
            return "参数不能为空";
        }

        List<String> result = storeService.nearby(lng, lat, radius);

        return result;
    }

    /**
     * 计算距离
     *
     * curl "http://localhost:8080/geo/distance?a=store1&b=store2"
     */
    @GetMapping("/distance")
    public Object distance(@RequestParam String a,
                           @RequestParam String b) {

        if (ObjectUtil.hasEmpty(a, b)) {
            return "参数不能为空";
        }

        return storeService.distance(a, b);
    }

}
```

## 延迟队列（定时任务）

用于订单超时关闭、定时提醒、延迟执行任务

基于 Redisson 的 RDelayedQueue + RBlockingQueue 实现延迟队列（到期自动投递到阻塞队列消费）

```java id="delay1a1"
package io.github.atengk.delay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueService {

    private final RedissonClient redissonClient;

    private static final String QUEUE_KEY = "delay:order";

    private RBlockingQueue<String> blockingQueue;

    private RDelayedQueue<String> delayedQueue;

    /**
     * 初始化队列
     */
    @PostConstruct
    public void init() {

        blockingQueue = redissonClient.getBlockingQueue(QUEUE_KEY);

        delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        startConsumer();

        log.info("延迟队列初始化完成");
    }

    /**
     * 添加延迟任务
     *
     * @param data 数据
     * @param delay 延迟时间
     * @param unit 时间单位
     */
    public void addTask(String data, long delay, TimeUnit unit) {

        delayedQueue.offer(data, delay, unit);

        log.info("添加延迟任务成功，data={}，delay={}", data, delay);
    }

    /**
     * 启动消费者（单线程示例）
     */
    private void startConsumer() {

        Thread thread = new Thread(() -> {

            while (true) {

                try {

                    String data = blockingQueue.take();

                    log.info("消费延迟任务，data={}", data);

                    handle(data);

                } catch (Exception e) {
                    log.error("延迟队列消费异常", e);
                }

            }

        });

        thread.setDaemon(true);
        thread.setName("delay-queue-consumer");
        thread.start();
    }

    /**
     * 任务处理（模拟订单超时关闭）
     */
    private void handle(String orderId) {

        log.info("处理订单超时关闭，orderId={}", orderId);

    }

}
```

---

模拟订单业务（延迟关闭）

```java id="delay2b2"
package io.github.atengk.service;

import io.github.atengk.delay.DelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 订单业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DelayQueueService delayQueueService;

    /**
     * 创建订单（30分钟未支付自动关闭）
     *
     * @param orderId 订单ID
     */
    public void createOrder(String orderId) {

        log.info("创建订单成功，orderId={}", orderId);

        delayQueueService.addTask(orderId, 30, TimeUnit.MINUTES);

    }

}
```

---

Controller 测试接口

```java id="delay3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 延迟队列测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（模拟延迟关闭）
     *
     * curl -X POST "http://localhost:8080/order/create?orderId=1001"
     */
    @PostMapping("/create")
    public Object create(@RequestParam String orderId) {

        if (ObjectUtil.isEmpty(orderId)) {
            return "orderId不能为空";
        }

        orderService.createOrder(orderId);

        return "订单创建成功（30分钟后自动关闭）";
    }

}
```

## 延迟队列（定时任务）（使用 RReliableQueue）

**注意：需要 Redisson PRO 版本才可以使用**

用于订单超时关闭、定时提醒、延迟执行任务

使用 `RReliableQueue` 实现延迟消息 + ACK 消费，失败自动重投（不ACK即可）

```java id="delay1a1"
package io.github.atengk.redisson.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.Message;
import org.redisson.api.MessageArgs;
import org.redisson.api.RReliableQueue;
import org.redisson.api.RedissonClient;
import org.redisson.api.queue.*;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 可靠延迟队列服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueService {

    private final RedissonClient redissonClient;

    private static final String QUEUE_KEY = "delay:order";

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration VISIBILITY_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);

    private RReliableQueue<String> reliableQueue;

    /**
     * 初始化队列
     */
    @PostConstruct
    public void init() {
        reliableQueue = redissonClient.getReliableQueue(QUEUE_KEY);
        startConsumer();
        log.info("可靠队列初始化完成，queueKey={}", QUEUE_KEY);
    }

    /**
     * 添加延迟任务
     *
     * @param data          数据
     * @param delayDuration 延迟时间
     */
    public void addTask(String data, Duration delayDuration) {
        reliableQueue.add(QueueAddArgs.messages(
                MessageArgs.payload(data).delay(delayDuration)
        ));

        log.info("添加延迟任务成功，data={}，delayDuration={} {}", data);
    }

    /**
     * 启动消费者（单线程示例）
     */
    private void startConsumer() {
        Thread thread = new Thread(() -> {
            while (true) {
                Message<String> message = null;
                try {
                    message = reliableQueue.poll(QueuePollArgs.defaults()
                            .visibility(VISIBILITY_TIMEOUT)
                            .acknowledgeMode(AcknowledgeMode.MANUAL)
                            .timeout(POLL_TIMEOUT));

                    if (message == null) {
                        continue;
                    }

                    String data = message.getPayload();

                    log.info("消费可靠队列消息，messageId={}，data={}", message.getId(), data);

                    try {
                        handle(data);
                    } catch (Exception ex) {
                        log.error("消息处理失败，准备重试，messageId={}", message.getId(), ex);
                        reliableQueue.negativeAcknowledge(QueueNegativeAckArgs.failed(message.getId())
                                .delay(RETRY_DELAY));
                        continue;
                    }

                    reliableQueue.acknowledge(QueueAckArgs.ids(message.getId()));

                    log.info("消息确认成功，messageId={}", message.getId());
                } catch (Exception e) {
                    log.error("可靠队列消费异常", e);
                }
            }
        });

        thread.setDaemon(true);
        thread.setName("delay-queue-consumer");
        thread.start();
    }

    /**
     * 任务处理（模拟订单超时关闭）
     *
     * @param orderId 订单ID
     */
    private void handle(String orderId) {
        log.info("处理订单超时关闭，orderId={}", orderId);
    }
}
```

---

模拟订单业务（延迟关闭）

```java id="delay2b2"
package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 订单业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DelayQueueService delayQueueService;

    /**
     * 创建订单（30分钟未支付自动关闭）
     *
     * @param orderId 订单ID
     */
    public void createOrder(String orderId) {

        log.info("创建订单成功，orderId={}", orderId);

        delayQueueService.addTask(orderId, Duration.ofMillis(1));

    }

}
```

---

Controller 测试接口

```java id="delay3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 延迟队列测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（模拟延迟关闭）
     *
     * curl -X POST "http://localhost:8080/order/create?orderId=1001"
     */
    @PostMapping("/create")
    public Object create(@RequestParam String orderId) {

        if (ObjectUtil.isEmpty(orderId)) {
            return "orderId不能为空";
        }

        orderService.createOrder(orderId);

        return "订单创建成功（30分钟后自动关闭）";
    }

}
```

## 消息队列（异步解耦）

用于系统解耦、削峰填谷、异步处理（简单 MQ）

基于 Redisson 的 RBlockingQueue 实现简单消息队列（生产者 + 阻塞消费者）

```java id="mq1a1"
package io.github.atengk.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 简单消息队列服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final RedissonClient redissonClient;

    private static final String QUEUE_KEY = "mq:demo";

    private RBlockingQueue<String> queue;

    /**
     * 初始化队列并启动消费者
     */
    @PostConstruct
    public void init() {

        queue = redissonClient.getBlockingQueue(QUEUE_KEY);

        startConsumer();

        log.info("消息队列初始化完成");
    }

    /**
     * 发送消息（生产者）
     *
     * @param message 消息内容
     */
    public void send(String message) {

        queue.offer(message);

        log.info("发送消息成功，message={}", message);
    }

    /**
     * 启动消费者（单线程示例）
     */
    private void startConsumer() {

        Thread thread = new Thread(() -> {

            while (true) {

                try {

                    String message = queue.take();

                    log.info("消费消息，message={}", message);

                    handle(message);

                } catch (Exception e) {
                    log.error("消息消费异常", e);
                }

            }

        });

        thread.setDaemon(true);
        thread.setName("mq-consumer");
        thread.start();
    }

    /**
     * 消息处理（模拟业务）
     */
    private void handle(String message) {

        log.info("处理消息逻辑，message={}", message);

    }

}
```

---

模拟异步业务（如下单后发通知）

```java id="mq2b2"
package io.github.atengk.service;

import io.github.atengk.mq.MessageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 异步业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {

    private final MessageQueueService messageQueueService;

    /**
     * 下单（异步发送通知）
     *
     * @param orderId 订单ID
     */
    public void createOrder(String orderId) {

        log.info("创建订单成功，orderId={}", orderId);

        messageQueueService.send("订单创建成功：" + orderId);

    }

}
```

---

Controller 测试接口

```java id="mq3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.AsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息队列测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/mq")
@RequiredArgsConstructor
public class MqController {

    private final AsyncService asyncService;

    /**
     * 创建订单（触发异步消息）
     *
     * curl -X POST "http://localhost:8080/mq/order?orderId=1001"
     */
    @PostMapping("/order")
    public Object order(@RequestParam String orderId) {

        if (ObjectUtil.isEmpty(orderId)) {
            return "orderId不能为空";
        }

        asyncService.createOrder(orderId);

        return "订单已创建（异步处理）";
    }

}
```

## 布隆过滤器（防缓存穿透）

用于拦截不存在的数据请求，保护数据库

基于 Redisson 的 RBloomFilter 实现数据存在性快速判断

```java id="bf1a1"
package io.github.atengk.bloom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloomFilterService {

    private final RedissonClient redissonClient;

    private static final String BLOOM_KEY = "bf:user";

    /**
     * 初始化布隆过滤器
     *
     * @param expectedInsertions 预计元素数量
     * @param falseProbability 误判率
     */
    public void init(long expectedInsertions, double falseProbability) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falseProbability);
            log.info("初始化布隆过滤器成功");
        }

    }

    /**
     * 添加元素
     */
    public void add(Long userId) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        bloomFilter.add(userId);

        log.info("添加元素到布隆过滤器，userId={}", userId);
    }

    /**
     * 判断是否存在
     */
    public boolean contains(Long userId) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        return bloomFilter.contains(userId);
    }

}
```

---

模拟查询用户（防穿透）

```java id="bf2b2"
package io.github.atengk.service;

import io.github.atengk.bloom.BloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户查询示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final BloomFilterService bloomFilterService;

    /**
     * 查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public String query(Long userId) {

        if (!bloomFilterService.contains(userId)) {
            log.warn("布隆过滤器拦截，userId={}", userId);
            return "用户不存在";
        }

        log.info("查询数据库，userId={}", userId);

        return "用户信息：" + userId;
    }

}
```

---

Controller 测试接口

```java id="bf3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.bloom.BloomFilterService;
import io.github.atengk.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 布隆过滤器测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/bf")
@RequiredArgsConstructor
public class BloomController {

    private final BloomFilterService bloomFilterService;
    private final UserQueryService userQueryService;

    /**
     * 初始化布隆过滤器
     *
     * curl -X POST "http://localhost:8080/bf/init?size=1000000&fpp=0.01"
     */
    @PostMapping("/init")
    public Object init(@RequestParam Long size,
                       @RequestParam Double fpp) {

        if (ObjectUtil.hasEmpty(size, fpp)) {
            return "参数不能为空";
        }

        bloomFilterService.init(size, fpp);

        return "初始化成功";
    }

    /**
     * 添加用户
     *
     * curl -X POST "http://localhost:8080/bf/add?userId=1"
     */
    @PostMapping("/add")
    public Object add(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        bloomFilterService.add(userId);

        return "添加成功";
    }

    /**
     * 查询用户
     *
     * curl "http://localhost:8080/bf/query?userId=1"
     */
    @GetMapping("/query")
    public Object query(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userQueryService.query(userId);
    }

}
```

## 黑白名单（访问控制）

用于 IP 封禁、用户权限控制、灰度发布名单等

基于 Redisson 的 RSet 实现黑名单 / 白名单快速判断

```java id="bw1a1"
package io.github.atengk.blackwhite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 黑白名单服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlackWhiteListService {

    private final RedissonClient redissonClient;

    private static final String BLACK_KEY = "black:list";
    private static final String WHITE_KEY = "white:list";

    /**
     * 添加黑名单
     */
    public void addBlack(String value) {

        RSet<String> set = redissonClient.getSet(BLACK_KEY);

        set.add(value);

        log.info("加入黑名单，value={}", value);
    }

    /**
     * 添加白名单
     */
    public void addWhite(String value) {

        RSet<String> set = redissonClient.getSet(WHITE_KEY);

        set.add(value);

        log.info("加入白名单，value={}", value);
    }

    /**
     * 是否在黑名单
     */
    public boolean isBlack(String value) {

        RSet<String> set = redissonClient.getSet(BLACK_KEY);

        return set.contains(value);
    }

    /**
     * 是否在白名单
     */
    public boolean isWhite(String value) {

        RSet<String> set = redissonClient.getSet(WHITE_KEY);

        return set.contains(value);
    }

}
```

---

模拟访问控制业务

```java id="bw2b2"
package io.github.atengk.service;

import io.github.atengk.blackwhite.BlackWhiteListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访问控制示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {

    private final BlackWhiteListService blackWhiteListService;

    /**
     * 校验访问
     *
     * @param ip IP地址
     * @return 是否允许
     */
    public String check(String ip) {

        if (blackWhiteListService.isBlack(ip)) {
            log.warn("黑名单拦截，ip={}", ip);
            return "访问被拒绝（黑名单）";
        }

        if (blackWhiteListService.isWhite(ip)) {
            log.info("白名单放行，ip={}", ip);
            return "访问通过（白名单）";
        }

        log.info("普通访问，ip={}", ip);

        return "访问通过";
    }

}
```

---

Controller 测试接口

```java id="bw3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.blackwhite.BlackWhiteListService;
import io.github.atengk.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 黑白名单测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/bw")
@RequiredArgsConstructor
public class BlackWhiteController {

    private final BlackWhiteListService blackWhiteListService;
    private final AccessService accessService;

    /**
     * 加入黑名单
     *
     * curl -X POST "http://localhost:8080/bw/black/add?value=127.0.0.1"
     */
    @PostMapping("/black/add")
    public Object addBlack(@RequestParam String value) {

        if (ObjectUtil.isEmpty(value)) {
            return "value不能为空";
        }

        blackWhiteListService.addBlack(value);

        return "加入黑名单成功";
    }

    /**
     * 加入白名单
     *
     * curl -X POST "http://localhost:8080/bw/white/add?value=127.0.0.1"
     */
    @PostMapping("/white/add")
    public Object addWhite(@RequestParam String value) {

        if (ObjectUtil.isEmpty(value)) {
            return "value不能为空";
        }

        blackWhiteListService.addWhite(value);

        return "加入白名单成功";
    }

    /**
     * 访问校验
     *
     * curl "http://localhost:8080/bw/check?ip=127.0.0.1"
     */
    @GetMapping("/check")
    public Object check(@RequestParam String ip) {

        if (ObjectUtil.isEmpty(ip)) {
            return "ip不能为空";
        }

        return accessService.check(ip);
    }

}
```

## 购物车（结构化缓存）

用于存储用户购物车数据（商品 + 数量）

基于 Redisson 的 RMap 实现购物车（key=用户，field=商品ID，value=数量）

```java id="cart1a1"
package io.github.atengk.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 购物车服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedissonClient redissonClient;

    private static final String CART_KEY_PREFIX = "cart:";

    /**
     * 加入购物车（增加数量）
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param count 数量
     */
    public void add(Long userId, Long productId, int count) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        map.addAndGet(productId, count);

        log.info("加入购物车，userId={}，productId={}，count={}", userId, productId, count);
    }

    /**
     * 减少数量
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param count 数量
     */
    public void decrease(Long userId, Long productId, int count) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        long result = map.addAndGet(productId, -count);

        if (result <= 0) {
            map.remove(productId);
        }

        log.info("减少购物车商品，userId={}，productId={}，count={}", userId, productId, count);
    }

    /**
     * 查询购物车
     *
     * @param userId 用户ID
     * @return 商品列表
     */
    public Map<Long, Integer> list(Long userId) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        return map.readAllMap();
    }

    /**
     * 清空购物车
     *
     * @param userId 用户ID
     */
    public void clear(Long userId) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        map.delete();

        log.info("清空购物车，userId={}", userId);
    }

}
```

---

模拟购物车业务

```java id="cart2b2"
package io.github.atengk.service;

import io.github.atengk.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 购物车业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Service
@RequiredArgsConstructor
public class CartBizService {

    private final CartService cartService;

    public void add(Long userId, Long productId, int count) {
        cartService.add(userId, productId, count);
    }

    public void decrease(Long userId, Long productId, int count) {
        cartService.decrease(userId, productId, count);
    }

    public Map<Long, Integer> list(Long userId) {
        return cartService.list(userId);
    }

    public void clear(Long userId) {
        cartService.clear(userId);
    }

}
```

---

Controller 测试接口

```java id="cart3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.CartBizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartBizService cartBizService;

    /**
     * 加入购物车
     *
     * curl -X POST "http://localhost:8080/cart/add?userId=1&productId=100&count=2"
     */
    @PostMapping("/add")
    public Object add(@RequestParam Long userId,
                      @RequestParam Long productId,
                      @RequestParam Integer count) {

        if (ObjectUtil.hasEmpty(userId, productId, count)) {
            return "参数不能为空";
        }

        cartBizService.add(userId, productId, count);

        return "加入成功";
    }

    /**
     * 减少数量
     *
     * curl -X POST "http://localhost:8080/cart/decrease?userId=1&productId=100&count=1"
     */
    @PostMapping("/decrease")
    public Object decrease(@RequestParam Long userId,
                           @RequestParam Long productId,
                           @RequestParam Integer count) {

        if (ObjectUtil.hasEmpty(userId, productId, count)) {
            return "参数不能为空";
        }

        cartBizService.decrease(userId, productId, count);

        return "操作成功";
    }

    /**
     * 查询购物车
     *
     * curl "http://localhost:8080/cart/list?userId=1"
     */
    @GetMapping("/list")
    public Object list(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        Map<Long, Integer> result = cartBizService.list(userId);

        return result;
    }

    /**
     * 清空购物车
     *
     * curl -X POST "http://localhost:8080/cart/clear?userId=1"
     */
    @PostMapping("/clear")
    public Object clear(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        cartBizService.clear(userId);

        return "清空成功";
    }

}
```

## 发布订阅（实时通知）

用于配置刷新、事件广播、实时消息推送

基于 Redisson 的 RTopic 实现发布订阅（广播消息，所有订阅者都会收到）

```java id="pub1a1"
package io.github.atengk.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 发布订阅服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubService {

    private final RedissonClient redissonClient;

    private static final String TOPIC_KEY = "topic:demo";

    /**
     * 初始化订阅者
     */
    @PostConstruct
    public void init() {

        RTopic topic = redissonClient.getTopic(TOPIC_KEY);

        topic.addListener(String.class, (channel, msg) -> {
            log.info("收到消息，channel={}，msg={}", channel, msg);
            handle(msg);
        });

        log.info("订阅初始化完成");
    }

    /**
     * 发布消息
     *
     * @param message 消息内容
     */
    public void publish(String message) {

        RTopic topic = redissonClient.getTopic(TOPIC_KEY);

        topic.publish(message);

        log.info("发布消息成功，message={}", message);
    }

    /**
     * 消息处理
     */
    private void handle(String message) {

        log.info("处理消息逻辑，message={}", message);

    }

}
```

---

模拟配置刷新 / 通知广播

```java id="pub2b2"
package io.github.atengk.service;

import io.github.atengk.pubsub.PubSubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通知业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final PubSubService pubSubService;

    /**
     * 发送通知
     */
    public void send(String msg) {
        pubSubService.publish(msg);
    }

}
```

---

Controller 测试接口

```java id="pub3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发布订阅测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/pub")
@RequiredArgsConstructor
public class PubSubController {

    private final NotifyService notifyService;

    /**
     * 发布消息
     *
     * curl -X POST "http://localhost:8080/pub/send?msg=hello"
     */
    @PostMapping("/send")
    public Object send(@RequestParam String msg) {

        if (ObjectUtil.isEmpty(msg)) {
            return "msg不能为空";
        }

        notifyService.send(msg);

        return "发送成功";
    }

}
```

## 秒杀库存预扣（高并发库存控制）

用于秒杀场景库存快速扣减，降低数据库压力

基于 Redisson 的 RAtomicLong 实现库存预扣（原子扣减 + 防超卖）

```java id="stock1a1"
package io.github.atengk.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 秒杀库存服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillStockService {

    private final RedissonClient redissonClient;

    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * 初始化库存
     *
     * @param productId 商品ID
     * @param stock 库存
     */
    public void initStock(Long productId, long stock) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        atomicLong.set(stock);

        log.info("初始化库存成功，productId={}，stock={}", productId, stock);
    }

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @return 是否成功
     */
    public boolean deduct(Long productId) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        long remain = atomicLong.decrementAndGet();

        if (remain < 0) {
            atomicLong.incrementAndGet();
            log.warn("库存不足，productId={}", productId);
            return false;
        }

        log.info("扣减库存成功，productId={}，剩余库存={}", productId, remain);

        return true;
    }

    /**
     * 查询库存
     */
    public long getStock(Long productId) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        return atomicLong.get();
    }

}
```

---

模拟秒杀业务

```java id="stock2b2"
package io.github.atengk.service;

import io.github.atengk.stock.SeckillStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillBizService {

    private final SeckillStockService seckillStockService;

    /**
     * 秒杀下单
     *
     * @param userId 用户ID
     * @param productId 商品ID
     */
    public String seckill(Long userId, Long productId) {

        boolean success = seckillStockService.deduct(productId);

        if (!success) {
            return "已售罄";
        }

        log.info("秒杀成功，userId={}，productId={}", userId, productId);

        return "秒杀成功";
    }

}
```

---

Controller 测试接口

```java id="stock3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.SeckillBizService;
import io.github.atengk.stock.SeckillStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀库存测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final SeckillStockService seckillStockService;
    private final SeckillBizService seckillBizService;

    /**
     * 初始化库存
     *
     * curl -X POST "http://localhost:8080/stock/init?productId=1&stock=10"
     */
    @PostMapping("/init")
    public Object init(@RequestParam Long productId,
                       @RequestParam Long stock) {

        if (ObjectUtil.hasEmpty(productId, stock)) {
            return "参数不能为空";
        }

        seckillStockService.initStock(productId, stock);

        return "初始化成功";
    }

    /**
     * 秒杀
     *
     * curl -X POST "http://localhost:8080/stock/do?userId=1&productId=1"
     */
    @PostMapping("/do")
    public Object seckill(@RequestParam Long userId,
                          @RequestParam Long productId) {

        if (ObjectUtil.hasEmpty(userId, productId)) {
            return "参数不能为空";
        }

        return seckillBizService.seckill(userId, productId);
    }

    /**
     * 查询库存
     *
     * curl "http://localhost:8080/stock/get?productId=1"
     */
    @GetMapping("/get")
    public Object get(@RequestParam Long productId) {

        if (ObjectUtil.isEmpty(productId)) {
            return "productId不能为空";
        }

        return seckillStockService.getStock(productId);
    }

}
```

## 滑动窗口统计（精准限流）

用于更精确的限流控制（比固定窗口更平滑）

基于 Redisson 的 RScoredSortedSet（ZSet）实现滑动窗口限流（按时间戳打分）

```java id="sw1a1"
package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 滑动窗口限流服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlidingWindowRateLimiterService {

    private final RedissonClient redissonClient;

    /**
     * 是否允许请求
     *
     * @param key        限流Key
     * @param windowSize 窗口大小（毫秒）
     * @param maxCount   窗口内最大请求数
     * @return 是否允许
     */
    public boolean allow(String key, long windowSize, int maxCount) {

        long now = System.currentTimeMillis();

        long windowStart = now - windowSize;

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(key);

        zSet.removeRangeByScore(0, true, windowStart, true);

        int current = zSet.size();

        if (current >= maxCount) {
            log.warn("触发滑动窗口限流，key={}", key);
            return false;
        }

        zSet.add(now, now);

        zSet.expire(Duration.ofMillis(windowSize));

        return true;
    }

}
```

---

模拟接口限流业务

```java id="sw2b2"
package io.github.atengk.service;

import io.github.atengk.limit.SlidingWindowRateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 精准限流业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Service
@RequiredArgsConstructor
public class ApiService {

    private final SlidingWindowRateLimiterService rateLimiterService;

    /**
     * 接口调用
     */
    public String call(String userId) {

        String key = "sliding:api:" + userId;

        boolean allowed = rateLimiterService.allow(key, 60000, 10);

        if (!allowed) {
            return "请求过于频繁";
        }

        return "调用成功";
    }

}
```

---

Controller 测试接口

```java id="sw3c3"
package io.github.atengk.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 滑动窗口限流测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    /**
     * 接口调用（1分钟最多10次）
     *
     * curl "http://localhost:8080/api/call?userId=1"
     */
    @GetMapping("/call")
    public Object call(@RequestParam String userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return apiService.call(userId);
    }

}
```
