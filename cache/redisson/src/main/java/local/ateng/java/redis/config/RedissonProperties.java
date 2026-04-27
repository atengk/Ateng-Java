package local.ateng.java.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置属性
 * 用于读取配置文件中 redisson 前缀下的配置项。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@ConfigurationProperties(prefix = "redisson")
@Configuration
@Data
public class RedissonProperties {

    /**
     * Redisson YAML 配置内容。
     * <p>
     * 通常在 application.yml 中配置为完整的 Redisson YAML 字符串，
     * 然后通过 {@link org.redisson.config.Config#fromYAML(String)} 解析为 Redisson 配置对象。
     * </p>
     */
    private String config;

}