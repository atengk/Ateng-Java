package local.ateng.java.mybatisjdk8;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import local.ateng.java.mybatisjdk8.entity.MyData;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKBWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class Tests {

    @Test
    public void test() throws UnknownHostException {
        InetAddress byAddress = InetAddress.getByAddress("127.0.0.1".getBytes());
        System.out.println(byAddress);
    }

    @Test
    public void test2() throws UnknownHostException {
        byte[] x = ipToBytes("192.168.1.1");
        System.out.println(x);
    }

    /**
     * 将 byte[] 转换为 IP 地址字符串（兼容 IPv4 和 IPv6）
     *
     * @param bytes IP地址的字节数组（IPv4为4字节，IPv6为16字节）
     * @return 对应的IP地址字符串（如 "192.168.1.1" 或 "2001:db8::1"）
     * @throws IllegalArgumentException 如果字节数组长度非法
     */
    public static String bytesToIp(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为 null");
        }

        try {
            // 直接通过 InetAddress 解析字节数组
            InetAddress address = InetAddress.getByAddress(bytes);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无效的IP字节数组长度: " + bytes.length);
        }
    }

    /**
     * 将 IP 地址字符串转换为 byte[]（兼容 IPv4 和 IPv6）
     *
     * @param ip IP地址字符串（如 "192.168.1.1" 或 "2001:db8::1"）
     * @return 对应的字节数组
     * @throws UnknownHostException 如果IP格式非法
     */
    public static byte[] ipToBytes(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip).getAddress();
    }

    @Test
    public void test3() {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = geometryFactory.createPoint(new Coordinate(39.908 ,116.397));

        WKBWriter writer = new WKBWriter(2, true);
        System.out.println(bytesToHex(writer.write(point)));
        //0020000001000010E64043F4395810624E405D196872B020C5
        //0x01010000004E62105839F44340C520B07268195D40
        System.out.println(point.toText());
    }

    // 将字节数组转为十六进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Test
    public void geojson() {
        GeometryFactory factory = new GeometryFactory();
        Polygon polygon = factory.createPolygon(
                new Coordinate[]{
                        new Coordinate(0, 0),
                        new Coordinate(0, 1),
                        new Coordinate(1, 1),
                        new Coordinate(1, 0),
                        new Coordinate(0, 0)
                }
        );

    }

    @Test
    public void test4() {
        String json = "{\n" +
                "  \"id\": 1,\n" +
                "  \"name\": \"test\",\n" +
                "  \"@type\": \"local.ateng.java.mybatisjdk8.entity.MyData\",\n" +
                "  \"address\": \"重庆市\",\n" +
                "  \"dateTime\": \"2025-07-27 16:45:21.646\"\n" +
                "}";
        MyData myData = (MyData) JSON.parse(json,JSONReader.Feature.SupportAutoType);
        System.out.println(myData);
        System.out.println(myData.getDateTime());
        MyData jsonObject = (MyData) JSON.parse(json, JSONReader.Feature.SupportAutoType);

        System.out.println(jsonObject);
    }

    @Test
    public void test5() {
        String json = "[{\"id\":0,\"name\":\"test0\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":1,\"name\":\"test1\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市1\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":2,\"name\":\"test2\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市2\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":3,\"name\":\"test3\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市3\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":4,\"name\":\"test4\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市4\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":5,\"name\":\"test5\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市5\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":6,\"name\":\"test6\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市6\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":7,\"name\":\"test7\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市7\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":8,\"name\":\"test8\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市8\",\"dateTime\":\"2025-07-27 21:14:54.599\"},{\"id\":9,\"name\":\"test9\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市9\",\"dateTime\":\"2025-07-27 21:14:54.599\"}]";
        List<Object> parse = JSON.parseArray(json, Object.class,JSONReader.Feature.SupportAutoType);
        System.out.println(parse);

    }

}
