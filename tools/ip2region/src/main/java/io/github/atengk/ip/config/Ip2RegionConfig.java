package io.github.atengk.ip.config;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * ip2region 配置类
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
@ConditionalOnProperty(prefix = "ip2region", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Ip2RegionConfig {

    private final Ip2RegionProperties properties;

    private Searcher ipv4Searcher;
    private Searcher ipv6Searcher;

    public Ip2RegionConfig(Ip2RegionProperties properties) {
        this.properties = properties;
    }

    @Bean("ipv4Searcher")
    public Searcher ipv4Searcher() {
        this.ipv4Searcher = buildSearcher(properties.getIpv4DbPath(), Version.IPv4);
        return this.ipv4Searcher;
    }

    @Bean("ipv6Searcher")
    public Searcher ipv6Searcher() {
        this.ipv6Searcher = buildSearcher(properties.getIpv6DbPath(), Version.IPv6);
        return this.ipv6Searcher;
    }

    /**
     * 构建 Searcher（支持多模式）
     */
    private Searcher buildSearcher(String path, Version version) {
        try {
            if (!FileUtil.exist(path)) {
                throw new IllegalArgumentException("xdb 文件不存在: " + path);
            }

            File file = new File(path);

            switch (properties.getSearchType()) {
                case "file":
                    return Searcher.newWithFileOnly(version, file);

                case "vector":
                    byte[] vectorIndex = Searcher.loadVectorIndexFromFile(file.getPath());
                    return Searcher.newWithVectorIndex(version, file, vectorIndex);

                case "memory":
                default:
                    LongByteArray content = Searcher.loadContentFromFile(file.getPath());
                    return Searcher.newWithBuffer(version, content);
            }

        } catch (Exception e) {
            throw new RuntimeException("ip2region 初始化失败: " + path, e);
        }
    }

    /**
     * 优雅关闭
     */
    @PreDestroy
    public void destroy() {
        try {
            if (ipv4Searcher != null) {
                ipv4Searcher.close();
            }
            if (ipv6Searcher != null) {
                ipv6Searcher.close();
            }
        } catch (Exception ignored) {
        }
    }
}