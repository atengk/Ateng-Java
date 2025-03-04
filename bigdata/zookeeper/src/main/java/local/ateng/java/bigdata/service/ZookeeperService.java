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
