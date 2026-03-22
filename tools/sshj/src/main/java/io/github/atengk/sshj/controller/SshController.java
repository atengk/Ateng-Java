package io.github.atengk.sshj.controller;

import io.github.atengk.sshj.core.SshCommandResult;
import io.github.atengk.sshj.core.SshTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SshController {

    @Autowired
    private SshTemplate sshTemplate;

    @GetMapping("/demo")
    public void demo() throws Exception {
        // 1. 执行命令
        SshCommandResult result = sshTemplate.exec("whoami && pwd && ls -la");
        System.out.println("stdout = " + result.getStdout());
        System.out.println("stderr = " + result.getStderr());
        System.out.println("exit = " + result.getExitStatus());

        // 2. 上传
        sshTemplate.upload("D:\\temp\\2026\\demo.png", "/tmp/demo.png");

        // 3. 下载
        sshTemplate.download("/tmp/demo.png", "D:\\temp\\2026\\demo2.png");

        // 4. 自定义操作
        String host = sshTemplate.execute(sshClient -> sshClient.getRemoteHostname());
        System.out.println(host);
    }

}
