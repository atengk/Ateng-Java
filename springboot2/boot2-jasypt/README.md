# Jasypt

JASYPT Spring Boot为Spring Boot应用程序中的属性源提供了加密支持。



## 基础配置

### 添加依赖

```xml
<!-- jasypt：配置文件参数值加密 -->
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

### 创建工具类

```java
package local.ateng.java.jasypt.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Jasypt 加密/解密工具类
 * 用于快速生成 ENC(...) 格式的密文，或解密校验。
 * <p>
 * 注意：生产环境不要把加密密钥写死在代码里，建议通过 JVM 参数或环境变量传入。
 *
 * @author 孔余
 * @since 2025-09-03
 */
public class EncryptorUtil {

    /**
     * 默认加密算法，可根据需要调整
     */
    private static final String ALGORITHM = "PBEWITHMD5ANDDES";

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @param password  加密密钥
     * @return 加密后的密文，格式 ENC(xxx)
     */
    public static String encrypt(String plainText, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setPassword(password);
        String encrypted = encryptor.encrypt(plainText);
        return "ENC(" + encrypted + ")";
    }

    /**
     * 解密字符串
     *
     * @param encryptedText 加密后的字符串（可以带 ENC(...)，也可以直接是密文）
     * @param password      加密密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setPassword(password);

        // 去掉 ENC(...) 包裹
        String text = encryptedText;
        if (encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            text = encryptedText.substring(4, encryptedText.length() - 1);
        }

        return encryptor.decrypt(text);
    }

}


```

### 使用加密解密

```java
public class Tests {

    @Test
     void test() {
        String password = "Admin@123"; // 加密密钥（建议用 JVM 参数传入）
        String plain = "root123";           // 待加密的明文

        // 加密
        String enc = EncryptorUtil.encrypt(plain, password);
        System.out.println("加密后: " + enc);

        // 解密
        String dec = EncryptorUtil.decrypt(enc, password);
        System.out.println("解密后: " + dec);
    }
    
}
```

输出：

```
加密后: ENC(SPr30wjbGw6zMewzBxcr/g==)
解密后: root123
```



## 使用Jasypt

### 编辑配置文件

**配置 jasypt**

PBEWithMD5AndDES 不支持 IV，必须用 `NoIvGenerator`

```
---
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
    algorithm: PBEWITHMD5ANDDES
    iv-generator-classname: org.jasypt.iv.NoIvGenerator
```

将需要加密的配置使用 `ENC` 包裹起来，启动项目后会自动解密

```yaml
---
user:
  name: "ateng"
  password: ENC(SPr30wjbGw6zMewzBxcr/g==)
```



### 添加环境变量

系统添加环境变量，注意环境变量不要设置在全局，让其他项目读取到了会有问题

```
JASYPT_ENCRYPTOR_PASSWORD=Admin@123
```



### 测试加密配置

在使用到加密的配置后会自动解密

```java
package local.ateng.java.jasypt.runner;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * ApplicationRunner：实现此接口的类会在Spring Boot应用启动后执行其run方法。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-29
 */
@Component
@Slf4j
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        log.info("加密后的配置：{}",SpringUtil.getProperty("user.password", ""));
    }
}
```

输出：

```
2025-09-04 14:14:53.382  INFO 33328 --- [           main] l.a.j.jasypt.runner.MyApplicationRunner  : 加密后的配置：root123
```

