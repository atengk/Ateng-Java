package local.ateng.java.serialize.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import local.ateng.java.serialize.serializer.DefaultValueFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson
 * https://github.com/alibaba/fastjson/wiki/%E5%9C%A8-Spring-%E4%B8%AD%E9%9B%86%E6%88%90-Fastjson
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-09-29
 */
@Configuration
public class FastJsonWebMvcConfig implements WebMvcConfigurer {

    /**
     * Fastjson转换器配置
     *
     * @return
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setCharset(StandardCharsets.UTF_8);
        config.setSerializerFeatures(
                // 序列化输出空值字段
                SerializerFeature.WriteMapNullValue,
                // 在大范围超过JavaScript支持的整数，输出为字符串格式
                SerializerFeature.BrowserCompatible,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                SerializerFeature.WriteBigDecimalAsPlain
        );
        config.setSerializeFilters(new DefaultValueFilter());
        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = getFastJsonHttpMessageConverter();
        converters.add(0, converter);
    }

}

