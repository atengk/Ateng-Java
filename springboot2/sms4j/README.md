# SMS4J 邮件发送

官网：[链接](https://sms4j.com/doc3/springBoot.html)



## 基础配置

### 添加依赖

```xml
<!-- SMS4J 短信发送 -->
<dependency>
    <groupId>org.dromara.sms4j</groupId>
    <artifactId>sms4j-spring-boot-starter</artifactId>
    <version>3.3.5</version>
</dependency>
```

### 添加配置文件

```yaml
---
# 短信配置
sms:
  # 标注从yml读取配置
  config-type: yaml
  blends:
    # 自定义的标识，也就是configId这里可以是任意值（最好不要是中文）
    tx1:
      #厂商标识，标定此配置是哪个厂商，详细请看厂商标识介绍部分
      supplier: danmi
      #您的accessKey
      access-key-id: 68cbd5af666d27a25882c0c06465a750
      #您的accessKeySecret
      access-key-secret: d1c6c8a7c20e7b0aac588498f3eb8e44
      #您的短信签名
      signature: 您的短信签名
      #模板ID 非必须配置，如果使用sendMessage的快速发送需此配置
      template-id: xxxxxxxx
      #您的sdkAppId
      sdk-app-id: 您的sdkAppId
      # 代理
      proxy:
        # 是否启用代理 默认关闭 需手动开启
        enable: false
        host: 127.0.0.1
        port: 8080
```



## 发送短信

```java
package local.ateng.java.sms4j.controller;

import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test")
    public void testSend(){
        SmsBlend smsBlend = SmsFactory.getSmsBlend("tx1");
        SmsResponse smsResponse = smsBlend.sendMessage("17623062936","你好，我是阿腾！");
        System.out.println(smsResponse);
    }
}
```

