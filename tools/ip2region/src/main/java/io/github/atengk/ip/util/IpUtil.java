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
     * 判断是否内网 IP（支持 IPv4 + IPv6，全场景）
     */
    public static boolean isInnerIp(InetAddress address) {

        if (address == null) {
            return false;
        }

        // 通用判断（IPv4 + IPv6）
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }

        // IPv6 特殊处理
        if (address instanceof Inet6Address) {

            Inet6Address ipv6 = (Inet6Address) address;

            // 1. 链路本地地址 fe80::/10
            if (ipv6.isLinkLocalAddress()) {
                return true;
            }

            // 2. IPv4-mapped IPv6（如 ::ffff:192.168.1.1）
            byte[] bytes = ipv6.getAddress();
            if (isIpv4MappedIpv6(bytes)) {
                try {
                    byte[] ipv4Bytes = new byte[4];
                    System.arraycopy(bytes, 12, ipv4Bytes, 0, 4);
                    InetAddress ipv4 = InetAddress.getByAddress(ipv4Bytes);
                    return isInnerIp(ipv4);
                } catch (Exception ignored) {
                }
            }

            // 3. ULA 私网 fc00::/7
            int firstByte = bytes[0] & 0xFF;
            if ((firstByte & 0xFE) == 0xFC) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否 IPv4-mapped IPv6
     */
    private static boolean isIpv4MappedIpv6(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return false;
        }

        // 前 10 字节为 0
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }

        // 第 11、12 字节为 0xFF
        return (bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF);
    }

}