# SpringBoot3配置相关的模块

## 将配置文件加载到属性类中

1. 在配置文件 `application.yml` 中添加以下自定义配置

```yaml
---
# 自定义配置文件
app:
  name: ateng
  port: 12345
  ids:
    - 1
    - 2
    - 3
  ateng:
    name: kongyu
    age: 24
```


2. 创建属性类加载配置

```java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Configuration
@Data
public class AppProperties {
    private String name;
    private int port;
    private List<Integer> ids;
    private Ateng ateng;

    @Data
    public static class Ateng{
        private String name;
        private int age;
    }
}
```

3. 使用属性类

注入属性类后直接使用

```java
import local.ateng.java.config.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyApplicationRunner implements ApplicationRunner {
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("配置文件：{}", appProperties);
    }
}
```
