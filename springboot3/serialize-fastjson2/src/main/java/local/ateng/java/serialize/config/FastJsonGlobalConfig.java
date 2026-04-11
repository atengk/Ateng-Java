package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 全局配置fastjson2属性
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class FastJsonGlobalConfig {

    @PostConstruct
    public void run() {
        // 设置全局默认的 JSON 序列化特性
        JSON.config(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 基于字段反序列化
                JSONWriter.Feature.FieldBased,
                // 把 Long 类型转为字符串，避免前端精度丢失
                JSONWriter.Feature.WriteLongAsString,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );
        JSON.config(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch,
                // 允许字段名不带引号
                JSONReader.Feature.AllowUnQuotedFieldNames,
                // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreNoneSerializable,
                // 防止类型不匹配时报错（更安全）
                JSONReader.Feature.IgnoreAutoTypeNotMatch
        );
    }
}
