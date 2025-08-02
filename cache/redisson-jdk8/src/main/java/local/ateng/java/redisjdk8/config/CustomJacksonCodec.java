package local.ateng.java.redisjdk8.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.codec.JsonJacksonCodec;

public class CustomJacksonCodec extends JsonJacksonCodec {

    public CustomJacksonCodec() {
        super(createObjectMapper());
    }

    private static ObjectMapper createObjectMapper() {
        // 创建 ObjectMapper 实例，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 模块，支持 Java 8 日期时间类型（如 LocalDateTime、LocalDate）
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳，改为标准 ISO-8601 字符串格式（如 "2025-08-01T15:30:00"）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用默认类型信息，解决反序列化时无法恢复原始对象类型的问题（类似 fastjson 的 @type）
        objectMapper.activateDefaultTyping(
                // - 使用 LaissezFaireSubTypeValidator：一个宽松的子类型校验器
                LaissezFaireSubTypeValidator.instance,
                // - DefaultTyping.NON_FINAL：仅对非 final 类型（如 Object、List、Map、自定义类）启用类型信息
                ObjectMapper.DefaultTyping.NON_FINAL,
                // - JsonTypeInfo.As.PROPERTY：将类型信息作为 JSON 属性（字段）存储
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

}
