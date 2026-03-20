package io.github.atengk.sshj.controller;

import io.github.atengk.sshj.service.SshTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SshController {

    @Autowired
    private SshTemplate sshTemplate;

    @GetMapping("/exec")
    public String checkDisk() {
        // 执行命令
        return sshTemplate.execute("df -h");
    }

}
