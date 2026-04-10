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

### 添加配置属性

`application.yml`

```yaml
---
# ip2region 配置
ip2region:
  enabled: true
  ipv4-db-path: D:/My/files/ip2region/ip2region_v4.xdb
  ipv6-db-path: D:/My/files/ip2region/ip2region_v6.xdb
  search-type: memory   # file / vector / memory
  cache-enabled: true

```

### 配置属性类

```java
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
```

### 配置类

```java
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
```

### VO 对象（结构化返回）

```java
package io.github.atengk.ip.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * IP 信息
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String country;
    private String area;
    private String province;
    private String city;
    private String isp;

}
```

### IP 工具类

```java
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
```

## 使用ip2region

### 创建 Controller 测试

```java
package io.github.atengk.ip.controller;

import io.github.atengk.ip.model.IpInfo;
import io.github.atengk.ip.service.Ip2RegionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(Ip2RegionService.class)
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

