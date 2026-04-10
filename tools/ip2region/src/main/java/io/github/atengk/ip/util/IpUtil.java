package io.github.atengk.ip.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * IP 工具类
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class IpUtil {

    /**
     * 解析 IP
     */
    public static InetAddress parse(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否 IPv4
     */
    public static boolean isIpv4(InetAddress address) {
        return address instanceof Inet4Address;
    }

    /**
     * 是否 IPv6
     */
    public static boolean isIpv6(InetAddress address) {
        return address instanceof Inet6Address;
    }

    /**
     * 是否内网 IP
     */
    public static boolean isInnerIp(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress();
    }
}