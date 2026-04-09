package io.github.atengk.ip.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.ip.model.IpInfo;
import io.github.atengk.ip.service.Ip2RegionService;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * IP 归属地服务实现
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Service
public class Ip2RegionServiceImpl implements Ip2RegionService {

    private final Searcher ipv4Searcher;
    private final Searcher ipv6Searcher;

    public Ip2RegionServiceImpl(
            @Qualifier("ipv4Searcher") Searcher ipv4Searcher,
            @Qualifier("ipv6Searcher") Searcher ipv6Searcher) {
        this.ipv4Searcher = ipv4Searcher;
        this.ipv6Searcher = ipv6Searcher;
    }

    /**
     * 查询结构化信息
     */
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
     * 查询原始字符串
     */
    @Override
    public String searchRaw(String ip) {
        try {
            if (isInnerIp(ip)) {
                return "内网IP|0|0|0|0";
            }

            if (isIpv4(ip)) {
                return ipv4Searcher.search(ip);
            }

            if (isIpv6(ip)) {
                return ipv6Searcher.search(ip);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("IP 查询失败: " + ip, e);
        }
    }


    /**
     * 判断是否 IPv4
     */
    private boolean isIpv4(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address instanceof Inet4Address;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否 IPv6
     */
    private boolean isIpv6(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否内网 IP
     */
    private boolean isInnerIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);

            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress();

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安全获取数组值
     */
    private String get(String[] arr, int index) {
        return arr.length > index ? arr[index] : "";
    }
}