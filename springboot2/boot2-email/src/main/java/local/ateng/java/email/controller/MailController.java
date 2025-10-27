package local.ateng.java.email.controller;

import cn.hutool.json.JSONObject;
import local.ateng.java.email.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailController {
    private final MailService mailService;

    @GetMapping("/async")
    public void async() {
        String html = "<h1>异步 HTML 邮件测试</h1><p>这是异步发送的 HTML 邮件。</p>";
        mailService.sendHtmlMailAsync("2385569970@qq.com", "异步 HTML 邮件测试", html);
    }

    @PostMapping("/sendMail")
    public void sendMail(@RequestBody JSONObject json) {
        mailService.sendMail(
                json.getStr("host"),
                json.getInt("port"),
                json.getStr("from"),
                json.getStr("password"),
                json.getStr("to"),
                null,
                null,
                json.getStr("subject"),
                json.getStr("html"),
                null,
                null
        );
    }

}