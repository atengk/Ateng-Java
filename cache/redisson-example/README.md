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

## 地理位置服务（LBS）

用于“附近的人 / 门店 / 外卖范围 / 距离计算”等功能

## 延迟队列（定时任务）

用于订单超时关闭、定时提醒、延迟执行任务

## 消息队列（异步解耦）

用于系统解耦、削峰填谷、异步处理（简单 MQ 或 Stream）

## 布隆过滤器（防缓存穿透）

用于拦截不存在的数据请求，保护数据库

## 黑白名单（访问控制）

用于 IP 封禁、用户权限控制、灰度发布名单等

## 购物车（结构化缓存）

用于存储用户购物车数据（商品 + 数量）

## 发布订阅（实时通知）

用于配置刷新、事件广播、实时消息推送

## 秒杀库存预扣（高并发库存控制）

用于秒杀场景库存快速扣减，降低数据库压力

## 滑动窗口统计（精准限流）

用于更精确的限流控制（比固定窗口更平滑）

