package local.ateng.java.hutool;

import cn.hutool.core.net.NetUtil;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.LinkedHashSet;

/**
 * 网络工具-NetUtil
 * https://www.hutool.cn/docs/#/core/%E7%BD%91%E7%BB%9C/%E7%BD%91%E7%BB%9C%E5%B7%A5%E5%85%B7-NetUtil
 *
 * @author 孔余
 * @since 2024-01-17 18:15
 */
public class NetUtilTests {
    // ipv4ToLong 根据ip地址计算出long型的数据
    @Test
    void test01() {
        long ipv4ToLong = NetUtil.ipv4ToLong("114.114.114.114");
        System.out.println(ipv4ToLong);
    }

    // longToIpv4 根据long值获取ip v4地址
    @Test
    void test02() {
        String longToIpv4 = NetUtil.longToIpv4(1920103026L);
        System.out.println(longToIpv4);
    }

    // isUsableLocalPort 检测本地端口可用性
    @Test
    void test03() {
        boolean usableLocalPort = NetUtil.isUsableLocalPort(23212);
        System.out.println(usableLocalPort);
    }

    // isValidPort 是否为有效的端口
    @Test
    void test04() {
        boolean isValidPort = NetUtil.isValidPort(65536);
        System.out.println(isValidPort);
    }

    // isInnerIP 判定是否为内网IP
    @Test
    void test05() {
        boolean isInnerIP = NetUtil.isInnerIP("10.0.0.0");
        System.out.println(isInnerIP);
    }

    // localIpv4s 获得本机的IP地址列表
    @Test
    void test06() {
        LinkedHashSet<String> localIpv4s = NetUtil.localIpv4s();
        System.out.println(localIpv4s);
//        LinkedHashSet<String> localIps = NetUtil.localIps();
//        System.out.println(localIps);
    }

    // getLocalHostName 获得本机的主机名
    @Test
    void test07() {
        String localhostName = NetUtil.getLocalHostName();
        System.out.println(localhostName);
    }

    // toAbsoluteUrl 相对URL转换为绝对URL
    @Test
    void test08() {
        String url = NetUtil.toAbsoluteUrl("http://192.168.1.1:8011/", "/demo/list");
        System.out.println(url);
    }

    // hideIpPart 隐藏掉IP地址的最后一部分为 * 代替
    @Test
    void test09() {
        String hideIpPart = NetUtil.hideIpPart("192.168.1.1");
        System.out.println(hideIpPart);
    }

    // buildInetSocketAddress 构建InetSocketAddress
    @Test
    void test10() {
        InetSocketAddress inetSocketAddress = NetUtil.buildInetSocketAddress("192.168.1.1", 1111);
        System.out.println(inetSocketAddress);
    }

    // getIpByHost 通过域名得到IP
    @Test
    void test11() {
        String ipByHost = NetUtil.getIpByHost("www.baidu.com");
        System.out.println(ipByHost);
    }

    // isInner 指定IP的long是否在指定范围内
    @Test
    void test12() {
        boolean inRange = NetUtil.isInRange("192.168.2.1", "192.168.1.0/24");
        System.out.println(inRange);
    }

}
