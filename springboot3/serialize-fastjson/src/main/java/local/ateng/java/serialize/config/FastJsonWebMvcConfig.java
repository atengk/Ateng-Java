package local.ateng.java.serialize.config;

import com.alibaba.fastjson.parser.Feature;
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
     * 获取自定义的 FastJson HttpMessageConverter 配置
     * 主要用于 Spring Boot WebMVC 的序列化与反序列化
     *
     * @return FastJson HttpMessageConverter
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setCharset(StandardCharsets.UTF_8);
        // 序列化配置
        config.setSerializerFeatures(
                // 输出为 null 的字段，否则默认会被忽略
                SerializerFeature.WriteMapNullValue,
                // String 类型为 null 时输出 ""
                SerializerFeature.WriteNullStringAsEmpty,
                // Number 类型为 null 时输出 0
                SerializerFeature.WriteNullNumberAsZero,
                // Boolean 类型为 null 时输出 false
                SerializerFeature.WriteNullBooleanAsFalse,
                // 集合类型为 null 时输出 []
                SerializerFeature.WriteNullListAsEmpty,
                // 禁用循环引用检测，避免出现 "$ref" 结构
                SerializerFeature.DisableCircularReferenceDetect,
                // BigDecimal 输出为纯字符串（不使用科学计数法）
                SerializerFeature.WriteBigDecimalAsPlain,
                // 浏览器安全输出，防止特殊字符被浏览器误解析
                SerializerFeature.BrowserCompatible
        );
        // 反序列化配置
        config.setFeatures(
                // 允许 JSON 中包含注释（// 或 /* */）
                Feature.AllowComment,
                // 允许字段名不加双引号
                Feature.AllowUnQuotedFieldNames,
                // 允许单引号作为字符串定界符
                Feature.AllowSingleQuotes,
                // 字段名使用常量池优化内存
                Feature.InternFieldNames,
                // 允许多余的逗号
                Feature.AllowArbitraryCommas,
                // 忽略 JSON 中不存在的字段
                Feature.IgnoreNotMatch,
                // 使用 BigDecimal 处理浮动精度，避免科学计数法的输出
                Feature.UseBigDecimal,
                // 允许 ISO 8601 日期格式（例如：2023-10-11T14:30:00Z）
                Feature.AllowISO8601DateFormat
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

