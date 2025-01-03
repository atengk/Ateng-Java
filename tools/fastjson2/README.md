# FastJson2

https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn

FASTJSON v2是FASTJSON项目的重要升级，目标是为下一个十年提供一个高性能的JSON库。通过同一套API，

支持JSON/JSONB两种协议，JSONPath是一等公民。
支持全量解析和部分解析。
支持Java服务端、客户端Android、大数据场景。
支持Kotlin
支持 JSON Schema https://github.com/alibaba/fastjson2/wiki/json_schema_cn
支持Android (2.0.10.android)
支持Graal Native-Image (2.0.10.graal)



## SpringBoot集成Fastjson2

### 添加依赖

**编辑pom.xml添加依赖**

```xml
        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
        <!-- 在 Spring 中集成 Fastjson2 -->
        <!-- https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring5</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
```

### 配置转换器

**配置WebMvcConfigurer**

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson2
 * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#2-%E5%9C%A8-spring-web-mvc-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
 *
 * @author 孔余
 * @since 2024-02-05 15:06
 */
@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {
    /**
     * Fastjson2转换器配置
     *
     * @return
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(Charset.forName("UTF-8"));
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setWriterFeatures(
                //JSONWriter.Feature.WriteNulls, // 将String类型字段的空值序列化输出为空字符串""
                //JSONWriter.Feature.FieldBased, // 基于字段序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做序列化。
                //JSONWriter.Feature.NullAsDefaultValue, // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.BrowserCompatible, // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.BrowserSecure // 浏览器安全，将会'<' '>' '(' ')'字符做转义输出
        );
        config.setReaderFeatures(
                //JSONReader.Feature.FieldBased, // 基于字段反序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做反序列化。在fieldbase配置下会更安全
                //JSONReader.Feature.InitStringFieldAsEmpty, // 初始化String字段为空字符串""
                JSONReader.Feature.SupportArrayToBean, // 支持数据映射的方式
                JSONReader.Feature.UseBigDecimalForDoubles // 默认配置会使用BigDecimal来parse小数，打开后会使用Double
        );
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = getFastJsonHttpMessageConverter();
        converters.add(0, converter);
    }

}
```

**配置全局JSON**

```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 全局配置fastjson2
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-06-21 11:38:11
 */
@Configuration
public class FastJsonConfig {

    @EventListener
    public void run(ApplicationReadyEvent event) {
        JSON.config(
                //JSONWriter.Feature.WriteNulls, // 将String类型字段的空值序列化输出为空字符串""
                //JSONWriter.Feature.FieldBased, // 基于字段序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做序列化。
                //JSONWriter.Feature.NullAsDefaultValue, // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.BrowserCompatible, // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.BrowserSecure // 浏览器安全，将会'<' '>' '(' ')'字符做转义输出
        );

        JSON.config(
                //JSONReader.Feature.FieldBased, // 基于字段反序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做反序列化。在fieldbase配置下会更安全
                //JSONReader.Feature.InitStringFieldAsEmpty, // 初始化String字段为空字符串""
                JSONReader.Feature.SupportArrayToBean, // 支持数据映射的方式
                JSONReader.Feature.UseBigDecimalForDoubles // 默认配置会使用BigDecimal来parse小数
        );
    }
}
```

### 创建实体类

**UserInfoEntity**

```java
package local.ateng.java.fastjson2.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;

    /**
     * 创建时间
     * 自定义时间格式
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createAt;
}
```

**ClassNameEntity**

```java
@Data
public class ClassNameEntity {
    private String name;
    private List<UserInfoEntity> users;
}
```

### 配置接口

**ClassNameController**

```java
@RestController
@RequestMapping("/class")
public class ClassNameController {

    @PostMapping("/add")
    public ClassNameEntity add(@RequestBody ClassNameEntity className) {
        return className;
    }

    @PostMapping("/user")
    public UserInfoEntity add(@RequestBody UserInfoEntity userInfoEntity) {
        return userInfoEntity;
    }

}
```

