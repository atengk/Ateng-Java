package local.ateng.java.config.config;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 通用配置类，涵盖常见类型，用于演示 Spring Boot 3 元数据提示
 * <p>
 * prefix: common
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "common")
public class CommonProperties {

    /**
     * 应用名称（必填）
     */
    @NotBlank
    private String name;

    /**
     * 是否启用模块
     */
    private boolean enabled = true;

    /**
     * 服务端口号
     */
    private int port = 8080;

    /**
     * 允许的ID列表
     */
    private List<Integer> ids;

    /**
     * 元信息键值对
     */
    private Map<String, String> metadata;

    /**
     * 运行环境类型
     */
    private EnvType env = EnvType.DEV;

    /**
     * 请求超时时间（如 10s、1m）
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 已弃用字段（示例）
     */
    private String host;

    /**
     * 枚举类型：环境类型
     */
    public enum EnvType {
        DEV, TEST, PROD
    }
}