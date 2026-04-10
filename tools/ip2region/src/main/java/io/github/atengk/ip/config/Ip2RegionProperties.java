package io.github.atengk.ip.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ip2region 配置属性
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "ip2region")
public class Ip2RegionProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * IPv4 数据库路径
     */
    private String ipv4DbPath;

    /**
     * IPv6 数据库路径
     */
    private String ipv6DbPath;

    /**
     * 查询模式（file / vector / memory）
     */
    private String searchType = "memory";

    /**
     * 是否开启缓存
     */
    private boolean cacheEnabled = true;
}