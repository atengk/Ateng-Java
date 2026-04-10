package io.github.atengk.ip.service.impl;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.ip.config.Ip2RegionProperties;
import io.github.atengk.ip.model.IpInfo;
import io.github.atengk.ip.service.Ip2RegionService;
import io.github.atengk.ip.util.IpUtil;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * IP 归属地服务实现
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
@ConditionalOnProperty(prefix = "ip2region", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Ip2RegionServiceImpl implements Ip2RegionService {

    private final Searcher ipv4Searcher;
    private final Searcher ipv6Searcher;
    private final Ip2RegionProperties properties;

    /**
     * 本地缓存（LRU）
     */
    private final Cache<String, String> cache = CacheUtil.newLRUCache(10000);

    public Ip2RegionServiceImpl(
            @Qualifier("ipv4Searcher") Searcher ipv4Searcher,
            @Qualifier("ipv6Searcher") Searcher ipv6Searcher,
            Ip2RegionProperties properties) {

        this.ipv4Searcher = ipv4Searcher;
        this.ipv6Searcher = ipv6Searcher;
        this.properties = properties;
    }

    @Override
    public IpInfo search(String ip) {
        String raw = searchRaw(ip);

        if (ObjectUtil.isEmpty(raw)) {
            return null;
        }

        String[] arr = raw.split("\\|");

        return new IpInfo(
                get(arr, 0),
                get(arr, 1),
                get(arr, 2),
                get(arr, 3),
                get(arr, 4)
        );
    }

    /**
     * 查询原始字符串（带缓存）
     */
    @Override
    public String searchRaw(String ip) {
        try {
            if (ObjectUtil.isEmpty(ip)) {
                return null;
            }

            // 开启缓存
            if (properties.isCacheEnabled()) {
                String cacheVal = cache.get(ip);
                if (cacheVal != null) {
                    return cacheVal;
                }
            }

            String result = doSearch(ip);

            if (properties.isCacheEnabled() && result != null) {
                cache.put(ip, result);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("IP 查询失败: " + ip, e);
        }
    }

    /**
     * 实际查询逻辑
     */
    private String doSearch(String ip) throws Exception {

        InetAddress address = IpUtil.parse(ip);

        if (address == null) {
            return null;
        }

        if (IpUtil.isInnerIp(address)) {
            return "内网IP|0|0|0|0";
        }

        if (IpUtil.isIpv4(address)) {
            return ipv4Searcher.search(ip);
        }

        if (IpUtil.isIpv6(address)) {
            return ipv6Searcher.search(ip);
        }

        return null;
    }

    private String get(String[] arr, int index) {
        return arr.length > index ? arr[index] : "";
    }
}