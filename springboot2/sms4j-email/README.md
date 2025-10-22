# SMS4J 邮件发送

官网：[链接](https://sms4j.com/doc3/plugin/oa.html)



## 基础配置

### 添加依赖

```xml
<!-- SMS4J 邮件发送 -->
<dependency>
    <groupId>org.dromara.sms4j</groupId>
    <artifactId>sms4j-Email-core</artifactId>
    <version>3.3.5</version>
</dependency>
```

### 添加配置文件

```yaml
---
# OA消息通知配置
sms-email:
  smtp-server: "smtp.qq.com"
  port: "465"
  from-address: "2385569970@qq.com"
  username: "2385569970@qq.com"
  password: "nkoojkmbokvweaji"
  is-ssl: "true"
  is-auth: "true"
  retry-interval: "5"
  max-retries: "1"
```

### 创建配置类

```java
package local.ateng.java.email.config;

import lombok.Data;
import org.dromara.email.api.Blacklist;
import org.dromara.email.api.MailClient;
import org.dromara.email.comm.config.MailSmtpConfig;
import org.dromara.email.core.factory.MailFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "sms-email")
@Data
public class DefaultMailSmtpConfig {

    private String port;
    private String fromAddress;
    private String nickName;
    private String smtpServer;
    private String username;
    private String password;
    private String isSSL = "true";
    private String isAuth = "true";
    private int retryInterval = 5;
    private int maxRetries = 1;

    @Bean
    public MailClient mailClient(){
        MailSmtpConfig config = MailSmtpConfig.builder()
                .port(port)
                .fromAddress(fromAddress)
                .smtpServer(smtpServer)
                .username(username)
                .password(password)
                .isSSL(isSSL)
                .isAuth(isAuth)
                .retryInterval(retryInterval)
                .maxRetries(maxRetries)
                .build();
        String key = "qq";
        MailFactory.put(key, config);
       return MailFactory.createMailClient(key, new BlackListImpl());
    }

    /**
     * 黑名单配置
     */
    public class BlackListImpl implements Blacklist {

        @Override
        public List<String> getBlacklist() {
            List<String> blacklist = new ArrayList<>();
            blacklist.add("2385569970@qq.com");
            return blacklist;
        }
    }

}

```



## 发送邮件

### 创建测试类

```java
package local.ateng.java.email;

import org.dromara.email.api.MailClient;
import org.dromara.email.comm.entity.MailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class EMailTests {
    @Autowired
    private MailClient mailClient;
}
```

### 发送邮件

```java
@Test
public void test1() {
    List<String> mailAddress = new ArrayList<>();
    mailAddress.add("2385569970@qq.com");
    mailAddress.add("kongyu@beraising.cn");
    MailMessage message = MailMessage.Builder()
            .mailAddress (mailAddress)
            .title("测试标题")
            .body("测试邮件发送")
            .htmlContent("11111111111")
            .build();
    mailClient.send(message);
}
```

