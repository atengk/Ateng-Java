package local.ateng.java.redis.config;


import lombok.Data;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义Redis配置文件
 *
 * @author 孔余
 * @since 2024-01-18 11:02
 */
@ConfigurationProperties(prefix = "spring.data")
@Configuration
@Data
public class MyRedisProperties {
    private RedisProperties redisDev;
    // private RedisProperties redisTest;
}
