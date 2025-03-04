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

