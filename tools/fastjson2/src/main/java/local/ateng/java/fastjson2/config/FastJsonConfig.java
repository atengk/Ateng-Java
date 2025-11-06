package local.ateng.java.fastjson2.config;

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
 * @since 2025-11-06
 */
@Configuration
public class FastJsonConfig {

    @EventListener
    public void run(ApplicationReadyEvent event) {
        JSON.configReaderDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        JSON.configWriterDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        JSON.config(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.NullAsDefaultValue,
                // 打开循环引用检测
                JSONWriter.Feature.ReferenceDetection,
                // 保证 BigDecimal 精度
                JSONWriter.Feature.WriteBigDecimalAsPlain,
                // 把 Long 类型转为字符串，避免前端精度丢失
                JSONWriter.Feature.WriteLongAsString,
                // 浏览器安全输出（防止前端注入）
                JSONWriter.Feature.BrowserCompatible,
                JSONWriter.Feature.BrowserSecure
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
