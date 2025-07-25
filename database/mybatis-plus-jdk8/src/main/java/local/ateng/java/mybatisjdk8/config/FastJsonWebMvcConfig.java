package local.ateng.java.mybatisjdk8.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson
 * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#2-%E5%9C%A8-spring-web-mvc-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
 *
 * @author 孔余
 * @since 2024-02-05 15:06
 */
@Configuration
public class FastJsonWebMvcConfig implements WebMvcConfigurer {

    /**
     * Fastjson 1.x 转换器配置
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();

        config.setCharset(StandardCharsets.UTF_8);
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,            // 输出空值字段
                SerializerFeature.WriteNullStringAsEmpty,       // String null -> ""
                SerializerFeature.WriteNullNumberAsZero,        // Number null -> 0
                SerializerFeature.WriteNullListAsEmpty,         // List null -> []
                SerializerFeature.BrowserCompatible,            // 大数转字符串
                SerializerFeature.BrowserSecure                 // < > ( ) 转义
        );

        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, getFastJsonHttpMessageConverter()); // 覆盖默认 Jackson
    }
}
