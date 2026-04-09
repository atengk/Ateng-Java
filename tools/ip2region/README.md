# ip2region

[ip2region](https://ip2region.net/) - 是一个离线IP地址定位库和IP定位数据管理框架，同时支持 `IPv4` 和 `IPv6` ，10微秒级别的查询效率，提供了众多主流编程语言的 `xdb` 数据生成和查询客户端实现。



## 基础配置

**下载离线文件**

- [ip2region_v4.xdb](https://github.com/lionsoul2014/ip2region/blob/v3.15.0/data/ip2region_v4.xdb)
- [ip2region_v6.xdb](https://github.com/lionsoul2014/ip2region/blob/v3.15.0/data/ip2region_v6.xdb)

下载后放到项目 `resources\ip2region\` 目录下

**添加依赖**

```xml
<!-- ip2region -->
<dependency>
    <groupId>org.lionsoul</groupId>
    <artifactId>ip2region</artifactId>
    <version>3.3.7</version>
</dependency>
```

## 创建ip2region配置

### 配置类

```java
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
```

### VO 对象（结构化返回）

```java
package io.github.atengk.ip.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP 信息
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpInfo {
    private String country;
    private String area;
    private String province;
    private String city;
    private String isp;
}
```

### Service 接口

```java
package io.github.atengk.ip.service;

import io.github.atengk.ip.model.IpInfo;

/**
 * IP 归属地服务
 *
 * @author Ateng
 * @since 2026-04-09
 */
public interface Ip2RegionService {

    /**
     * 查询 IP 归属地
     *
     * @param ip IP 地址
     * @return 结构化信息
     */
    IpInfo search(String ip);

    /**
     * 查询原始字符串
     */
    String searchRaw(String ip);
}
```

### Service 实现

```java
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
```

## 使用ip2region

### 创建 Controller 测试

```java
package io.github.atengk.ip.controller;

import io.github.atengk.ip.model.IpInfo;
import io.github.atengk.ip.service.Ip2RegionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IP 查询接口
 *
 * @author Ateng
 * @since 2026-04-09
 */
@RestController
public class IpController {

    private final Ip2RegionService ip2RegionService;

    public IpController(Ip2RegionService ip2RegionService) {
        this.ip2RegionService = ip2RegionService;
    }

    @GetMapping("/ip")
    public IpInfo getIp(@RequestParam String ip) {
        return ip2RegionService.search(ip);
    }
}
```

### 调用接口

```
GET: /ip?ip=183.227.91.145
```

返回

```json
{
  "country": "中国",
  "area": "重庆",
  "province": "重庆市",
  "city": "移动",
  "isp": "CN"
}
```

