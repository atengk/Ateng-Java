package local.ateng.java.bigdata;

import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZookeeperTests {
    private final CuratorFramework client;

    @Test
    void createNode() throws Exception {
        String path = "/ateng/data";
        String data = "我是阿腾";
        client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
    }

    @Test
    void getNodeData() throws Exception {
        String path = "/ateng/data";
        String data = new String(client.getData().forPath(path));
        System.out.println(data);
    }

}
