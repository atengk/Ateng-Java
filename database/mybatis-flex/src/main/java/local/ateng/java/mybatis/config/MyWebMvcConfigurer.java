package local.ateng.java.mybatis.config;

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
