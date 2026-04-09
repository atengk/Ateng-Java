package io.github.atengk.ip.config;

import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * ip2region 配置类
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Configuration
public class Ip2RegionConfig {

    /**
     * IPv4 查询器
     */
    @Bean
    public Searcher ipv4Searcher() {
        return buildSearcher("ip2region/ip2region_v4.xdb", Version.IPv4);
    }

    /**
     * IPv6 查询器
     */
    @Bean
    public Searcher ipv6Searcher() {
        return buildSearcher("ip2region/ip2region_v6.xdb", Version.IPv6);
    }

    /**
     * 构建 Searcher
     */
    private Searcher buildSearcher(String path, Version version) {
        try {
            ClassPathResource resource = new ClassPathResource(path);

            InputStream is = resource.getInputStream();
            File tempFile = File.createTempFile("ip2region", ".xdb");
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return Searcher.newWithFileOnly(version, tempFile);

        } catch (Exception e) {
            throw new RuntimeException("ip2region 初始化失败: " + path, e);
        }
    }
}