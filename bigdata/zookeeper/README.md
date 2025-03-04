# Zookeeper

Zookeeper 是一个开源的分布式协调服务，用于构建分布式应用程序中的同步和协调机制。它提供了高可用的服务，可以用于分布式锁、配置管理、命名服务、选举机制等场景。Zookeeper 采用类似于文件系统的层次结构来存储数据，所有的数据都会同步到集群中的所有节点，确保一致性。它通常用于大规模分布式系统中，支持高效的协调和故障恢复。

Apache Zookeeper 是一个分布式协调服务，Curator 是 Netflix 开源的 Zookeeper 客户端，它简化了 Zookeeper 的使用，提供了更安全和方便的 API。

- [官网链接](https://zookeeper.apache.org/)



## 基础配置

### 添加依赖

```xml
<properties>
    <curator.version>5.7.1</curator.version>
</properties>
<dependencies>
    <!-- Curator 是 Netflix 开源的 Zookeeper 客户端 -->
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-recipes</artifactId>
        <version>${curator.version}</version>
    </dependency>
</dependencies>
```

### 创建配置属性

```yaml
---
# Zookeeper 配置
zookeeper:
  connect-string: 192.168.1.10:44276
  session-timeout: 60000
  connection-timeout: 15000
```

### 创建配置

```java
package local.ateng.java.bigdata.config;

import lombok.Data;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建 Zookeeper 客户端
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-04
 */
@Configuration
@ConfigurationProperties(prefix = "zookeeper")
@Data
public class ZookeeperConfig {

    private String connectString;
    private int sessionTimeout;
    private int connectionTimeout;

    /**
     * 创建并返回一个CuratorFramework实例。
     *
     * @return CuratorFramework实例
     */
    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        return client;
    }
}
```



## 使用Zookeeper

### 创建Service

```java
package local.ateng.java.bigdata.service;

import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZookeeperService {
    private final CuratorFramework client;

    // 创建节点
    public void createNode(String path, String data) throws Exception {
        client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
    }

    // 获取节点数据
    public String getNodeData(String path) throws Exception {
        return new String(client.getData().forPath(path));
    }

    // 更新节点数据
    public void updateNodeData(String path, String newData) throws Exception {
        client.setData().forPath(path, newData.getBytes());
    }

    // 删除节点
    public void deleteNode(String path) throws Exception {
        client.delete().deletingChildrenIfNeeded().forPath(path);
    }

    // 获取子节点列表
    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    // 监听节点数据变化
    public void watchNode(String path) throws Exception {
        NodeCache nodeCache = new NodeCache(client, path);
        nodeCache.getListenable().addListener(() -> {
            System.out.println("节点数据发生变化: " + new String(nodeCache.getCurrentData().getData()));
        });
        nodeCache.start();
    }

    // 监听子节点变化
    public void watchChildren(String path) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        cache.getListenable().addListener((curatorFramework, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED:
                    System.out.println("子节点新增: " + event.getData().getPath());
                    break;
                case CHILD_UPDATED:
                    System.out.println("子节点更新: " + event.getData().getPath());
                    break;
                case CHILD_REMOVED:
                    System.out.println("子节点删除: " + event.getData().getPath());
                    break;
            }
        });
        cache.start();
    }

    // 分布式锁
    public void distributedLock() throws Exception {
        InterProcessMutex lock = new InterProcessMutex(client, "/distributed-lock");
        try {
            lock.acquire();
            System.out.println("获取到锁，执行任务...");
            Thread.sleep(5000);
        } finally {
            lock.release();
            System.out.println("释放锁");
        }
    }

    // 领导选举
    public void leaderElection() {
        LeaderSelector selector = new LeaderSelector(client, "/leader-election", new LeaderSelectorListenerAdapter() {
            @Override
            public void takeLeadership(CuratorFramework client) throws Exception {
                System.out.println("当前节点成为 Leader");
                Thread.sleep(5000);
                System.out.println("领导权交接");
            }
        });
        selector.autoRequeue();
        selector.start();
    }

}

```

### 创建Controller

```java
package local.ateng.java.bigdata.controller;

import local.ateng.java.bigdata.service.ZookeeperService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zookeeper")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZookeeperController {
    private final ZookeeperService zookeeperService;

    @PostMapping("/node")
    public void createNode(@RequestParam String path, @RequestParam String data) throws Exception {
        zookeeperService.createNode(path, data);
    }

    @GetMapping("/node")
    public String getNodeData(@RequestParam String path) throws Exception {
        return zookeeperService.getNodeData(path);
    }

    @PutMapping("/node")
    public void updateNodeData(@RequestParam String path, @RequestParam String newData) throws Exception {
        zookeeperService.updateNodeData(path, newData);
    }

    @DeleteMapping("/node")
    public void deleteNode(@RequestParam String path) throws Exception {
        zookeeperService.deleteNode(path);
    }

    @GetMapping("/children")
    public List<String> getChildren(@RequestParam String path) throws Exception {
        return zookeeperService.getChildren(path);
    }

    @PostMapping("/watch/node")
    public void watchNode(@RequestParam String path) throws Exception {
        zookeeperService.watchNode(path);
    }

    @PostMapping("/watch/children")
    public void watchChildren(@RequestParam String path) throws Exception {
        zookeeperService.watchChildren(path);
    }

    @PostMapping("/lock")
    public void distributedLock() throws Exception {
        zookeeperService.distributedLock();
    }

    @PostMapping("/leader")
    public void leaderElection() {
        zookeeperService.leaderElection();
    }
}
```

