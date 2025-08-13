# Email



## 基础配置

**添加依赖**

```xml
        <!-- Spring Boot 的邮件启动器依赖，用于发送邮件功能 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
```

**编辑配置文件**

- [QQ邮箱使用说明](https://wx.mail.qq.com/list/readtemplate?name=app_intro.html#/agreement/authorizationCode)
- [163邮箱使用说明](https://help.mail.163.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac2a5feb28b66796d3b)

```yaml
---
# 邮箱配置
spring:
  mail:
    host: smtp.163.com
    port: 465
    username: your_email@163.com
    password: your_email_authorization_code
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
    default-encoding: UTF-8
```



## 发送邮箱

### 简单消息

**创建service**

```java
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailService {
    private final JavaMailSender mailSender;

    // 发送 普通 邮件
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SpringUtil.getProperty("spring.mail.username")); // 发件人邮箱，要和配置一致
        message.setTo(to);                     // 收件人邮箱
        message.setSubject(subject);           // 邮件标题
        message.setText(content);              // 邮件内容
        mailSender.send(message);
    }

}
```

**创建接口**

```java
@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @GetMapping("/send")
    public String sendMail(@RequestParam String to) {
        mailService.sendSimpleMail(to, "测试邮件", "这是一封测试邮件！");
        return "邮件发送成功！";
    }

}
```

**发送消息**

```
curl http://127.0.0.1:12015/mail/send?to=2385569970@qq.com
```

### HTML消息

**创建service**

```java
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailService {
    private final JavaMailSender mailSender;

    // 发送 HTML 邮件
    public void sendHtmlMail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // 第二个参数 true 表示支持 multipart

        helper.setFrom(SpringUtil.getProperty("spring.mail.username"));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true 表示 HTML 格式

        mailSender.send(message);
    }

}
```

**创建接口**

```java
@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @GetMapping("/send-html")
    public String sendHTMLMail(@RequestParam String to) throws MessagingException {
        mailService.sendHtmlMail(to, "HTML 邮件测试",
                "<h2 style='color:blue'>这是一封 HTML 格式的测试邮件</h2><p>内容支持 <b>HTML</b> 标签。</p>");
        return "邮件发送成功！";
    }

}
```

**发送消息**

```
curl http://127.0.0.1:12015/mail/send-html?to=2385569970@qq.com
```

