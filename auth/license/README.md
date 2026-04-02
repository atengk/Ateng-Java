# License 授权

管理端：根据被管理端的机器指纹（CPU + 主网卡MAC + 磁盘序列号 + 主机名） 生成 License 文件。

被管理端：导入 License 文件到Redis中存储，通过过滤器进行接口访问限制，正确的 License 才能放行。



## 基础准备

### RSA 密钥生成

```java
package io.github.atengk.license;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.Test;

/**
 * RSA 密钥生成
 */
public class RsaKeyGeneratorTests {

    @Test
    public void testGenerate() {
        RSA rsa = SecureUtil.rsa();

        String publicKey = rsa.getPublicKeyBase64();
        String privateKey = rsa.getPrivateKeyBase64();

        System.out.println("公钥：");
        System.out.println(publicKey);

        System.out.println("私钥：");
        System.out.println(privateKey);
    }

}
```

生成

```
公钥：
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfNK24uKpliBvKwZAvU0m/Ibd4BOhnKQFCp2Zpc40T+41eoeYqMvu5d2c6NfVYl6mohTgefk5/tCmTcKC1fsNqAoSdZBUTg/Q5UPYLikT3gZ3lZoVqPMf/lX2lPN8RKNB4E1pfMbr1HFLwNEwrrKJGymb7FqDoa/W3Q5KAImqvZQIDAQAB
私钥：
MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ80rbi4qmWIG8rBkC9TSb8ht3gE6GcpAUKnZmlzjRP7jV6h5ioy+7l3Zzo19ViXqaiFOB5+Tn+0KZNwoLV+w2oChJ1kFROD9DlQ9guKRPeBneVmhWo8x/+VfaU83xEo0HgTWl8xuvUcUvA0TCusokbKZvsWoOhr9bdDkoAiaq9lAgMBAAECgYAPU/VQ1lizf0v7tLCi2dA9GmwtXj2Y5woDxpW12+XukVbOUGSWVgPF2sDhyh2lyq5PNwpM50i8A1bIZvzWFI8QQBJYHjvK4cnS2v0OXEYV8QzdpXdUCQTN1zKgeTte9obOLFxVUvUVOOloLv6hXoG5YEZKWx59UnYoKVsvc6CV4QJBALsBi2E0n9rpBWvDG4fAs2s1Rl3iDOa6+Mh/eJkttq+2+9WAFCQYALxBlshoYfee1pYw6BByat7/zHCLM+mbByECQQDZ8XBrX7jNVltUMqEValJNc9yjooXNitYYBa7z49E2J6ByDGmc0cHkw4nJlBG3kNEPUHN3v8/iNBlmjhN1ftPFAkAi1yzSlW1a6aMa6qTMa/iBdtF/WEgzDI6hPC6Jy1yH7D2LD2uxNc+dQ1MGT3xBGBS+sqnptod2uI2sQiMP2NRBAkB+OdTXi3AYFSz+Hfin05VpYHJon6eJPSD0ds42WTsBd3/4rfG5Ls9ytEGoa0a7n3dFvF2z/HblVzFi8WSZx2PVAkB4K/rq2HQsWylcpwUXlgd8G6PjAd2gV0ll9Cvldq5u8ghu4lHkHvM0EtwUORXtKsVFUeMt9QbOQUl/hgbHiB7e
```

### 机器指纹工具类

```java
package io.github.atengk.license.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Locale;

/**
 * 机器指纹工具类
 *
 * <p>组成：
 * CPU + 主网卡MAC + 磁盘序列号 + 主机名
 *
 * <p>特性：
 * 1. 自动容错（字段缺失不影响整体）
 * 2. 多磁盘合并（避免随机性）
 * 3. 统一标准化（保证hash一致）
 * 4. 内置缓存（避免重复计算）
 *
 * @author
 */
public class MachineFingerprintUtil {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();

    /**
     * 指纹缓存（进程级）
     */
    private static volatile String CACHE;

    /**
     * 获取机器唯一指纹（SHA-256）
     */
    public static String getFingerprint() {
        if (StrUtil.isNotBlank(CACHE)) {
            return CACHE;
        }

        synchronized (MachineFingerprintUtil.class) {
            if (StrUtil.isNotBlank(CACHE)) {
                return CACHE;
            }

            String cpu = getCpuId();
            String mac = getMainMac();
            String disk = getDiskSerial();
            String host = getHostName();

            String raw = StrUtil.join("|",
                    safe(cpu),
                    safe(mac),
                    safe(disk),
                    safe(host)
            );

            CACHE = DigestUtil.sha256Hex(raw);
            return CACHE;
        }
    }

    /**
     * 获取 CPU ID
     */
    public static String getCpuId() {
        try {
            CentralProcessor processor = SYSTEM_INFO.getHardware().getProcessor();
            String id = processor.getProcessorIdentifier().getProcessorID();
            return normalize(id);
        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取主网卡 MAC（基于出口IP推断）
     */
    public static String getMainMac() {
        try {
            InetAddress address = getRealLocalAddress();
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);

            if (ni == null) return StrUtil.EMPTY;

            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return StrUtil.EMPTY;

            StringBuilder sb = new StringBuilder();
            for (byte b : mac) {
                sb.append(String.format("%02X", b));
            }
            return normalize(sb.toString());

        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取磁盘序列号（多盘合并）
     */
    public static String getDiskSerial() {
        try {
            List<HWDiskStore> disks = SYSTEM_INFO.getHardware().getDiskStores();

            StringBuilder sb = new StringBuilder();

            for (HWDiskStore disk : disks) {
                String serial = disk.getSerial();
                if (isValid(serial)) {
                    sb.append(serial);
                }
            }

            return normalize(sb.toString());

        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取主机名
     */
    public static String getHostName() {
        try {
            return normalize(NetUtil.getLocalhost().getHostName());
        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    // ===================== 内部方法 =====================

    /**
     * 获取真实出口IP（UDP推断）
     */
    private static InetAddress getRealLocalAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        } catch (Exception e) {
            return NetUtil.getLocalhost();
        }
    }

    /**
     * 判断序列号有效性
     */
    private static boolean isValid(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        String s = str.trim().toLowerCase(Locale.ROOT);
        return !(s.equals("unknown")
                || s.equals("none")
                || s.equals("00000000"));
    }

    /**
     * 标准化（去空格 + 大写）
     */
    private static String normalize(String str) {
        if (StrUtil.isBlank(str)) {
            return StrUtil.EMPTY;
        }
        return StrUtil.trim(str)
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String safe(String str) {
        return StrUtil.nullToEmpty(str);
    }
}

```



## 创建 License 服务

### License 实体

```java
package io.github.atengk.license.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * License 实体
 *
 * 用于描述系统授权信息
 */
@Data
public class License implements Serializable {

    /**
     * License 唯一标识
     */
    private String licenseId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 签发时间
     */
    private LocalDateTime issuedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 绑定机器码
     */
    private String machineCode;

    /**
     * IP 白名单
     */
    private Set<String> allowIps;

    /**
     * 功能权限集合
     */
    private Set<String> features;

    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 数字签名
     */
    private String signature;

}
```

### License 服务接口

```java
package io.github.atengk.license.service;

import io.github.atengk.license.model.License;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * License 服务接口
 *
 * 特点
 * 1 Redis 为唯一数据源
 * 2 文件仅用于导入导出
 * 3 每次校验实时读取 Redis
 */
public interface LicenseService {

    /**
     * 生成 License 字符串
     *
     * @param license License 对象
     * @return License 字符串
     */
    String generate(License license);

    /**
     * 解析 License 字符串
     *
     * @param licenseStr License 字符串
     * @return License 对象
     */
    License parse(String licenseStr);

    /**
     * 写入 Redis
     *
     * @param licenseStr License 字符串
     */
    void save(String licenseStr);

    /**
     * 从 Redis 获取 License 字符串
     *
     * @return License 字符串
     */
    String get();

    /**
     * 获取当前 License（解析后）
     *
     * @return License 对象
     */
    License current();

    /**
     * 导出 License 文件（用于下载）
     *
     * @return License 文件字节
     */
    byte[] exportLicense();

    /**
     * 导入 License 文件
     *
     * 流程
     * 1 读取文件内容
     * 2 解析 License
     * 3 校验合法性
     * 4 写入 Redis
     *
     * @param inputStream License 文件流
     */
    void importLicense(InputStream inputStream);

    /**
     * 校验当前 License（基础校验）
     *
     * @return true 表示合法
     */
    boolean validate();

    /**
     * 校验指定 License
     *
     * @param license License
     * @return true 表示合法
     */
    boolean validate(License license);

    /**
     * 校验签名
     *
     * @param license License
     * @return true 表示合法
     */
    boolean verifySignature(License license);

    /**
     * 判断是否过期
     *
     * @param license License
     * @return true 表示过期
     */
    boolean isExpired(License license);

    /**
     * 刷新 License
     *
     * @param license 原 License
     * @param newExpireAt 新过期时间
     * @return 新 License 字符串
     */
    String refresh(License license, LocalDateTime newExpireAt);

    /**
     * 删除 License（清空 Redis）
     */
    void remove();

    /**
     * 生成机器码
     *
     * @return 机器码
     */
    String generateMachineCode();

    /**
     * 是否匹配当前机器
     *
     * @param license License
     * @return true 表示匹配
     */
    boolean matchMachine(License license);

    /**
     * IP 是否允许
     *
     * @param license License
     * @param clientIp IP
     * @return true 表示允许
     */
    boolean matchIp(License license, String clientIp);

    /**
     * 是否具备功能权限
     *
     * @param license License
     * @param featureCode 功能编码
     * @return true 表示允许
     */
    boolean hasFeature(License license, String featureCode);

}
```

### License 服务实现

```java
package io.github.atengk.license.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.json.JSONUtil;
import io.github.atengk.license.model.License;
import io.github.atengk.license.service.LicenseService;
import io.github.atengk.license.util.MachineFingerprintUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * License 服务实现
 */
@Component
public class LicenseServiceImpl implements LicenseService {

    /**
     * Redis Key
     */
    private static final String LICENSE_KEY = "license:data";

    /**
     * 私钥（用于签名）
     */
    private static final String PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ80rbi4qmWIG8rBkC9TSb8ht3gE6GcpAUKnZmlzjRP7jV6h5ioy+7l3Zzo19ViXqaiFOB5+Tn+0KZNwoLV+w2oChJ1kFROD9DlQ9guKRPeBneVmhWo8x/+VfaU83xEo0HgTWl8xuvUcUvA0TCusokbKZvsWoOhr9bdDkoAiaq9lAgMBAAECgYAPU/VQ1lizf0v7tLCi2dA9GmwtXj2Y5woDxpW12+XukVbOUGSWVgPF2sDhyh2lyq5PNwpM50i8A1bIZvzWFI8QQBJYHjvK4cnS2v0OXEYV8QzdpXdUCQTN1zKgeTte9obOLFxVUvUVOOloLv6hXoG5YEZKWx59UnYoKVsvc6CV4QJBALsBi2E0n9rpBWvDG4fAs2s1Rl3iDOa6+Mh/eJkttq+2+9WAFCQYALxBlshoYfee1pYw6BByat7/zHCLM+mbByECQQDZ8XBrX7jNVltUMqEValJNc9yjooXNitYYBa7z49E2J6ByDGmc0cHkw4nJlBG3kNEPUHN3v8/iNBlmjhN1ftPFAkAi1yzSlW1a6aMa6qTMa/iBdtF/WEgzDI6hPC6Jy1yH7D2LD2uxNc+dQ1MGT3xBGBS+sqnptod2uI2sQiMP2NRBAkB+OdTXi3AYFSz+Hfin05VpYHJon6eJPSD0ds42WTsBd3/4rfG5Ls9ytEGoa0a7n3dFvF2z/HblVzFi8WSZx2PVAkB4K/rq2HQsWylcpwUXlgd8G6PjAd2gV0ll9Cvldq5u8ghu4lHkHvM0EtwUORXtKsVFUeMt9QbOQUl/hgbHiB7e";

    /**
     * 公钥（用于验签）
     */
    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfNK24uKpliBvKwZAvU0m/Ibd4BOhnKQFCp2Zpc40T+41eoeYqMvu5d2c6NfVYl6mohTgefk5/tCmTcKC1fsNqAoSdZBUTg/Q5UPYLikT3gZ3lZoVqPMf/lX2lPN8RKNB4E1pfMbr1HFLwNEwrrKJGymb7FqDoa/W3Q5KAImqvZQIDAQAB";

    private final StringRedisTemplate stringRedisTemplate;

    public LicenseServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String generate(License license) {
        license.setIssuedAt(LocalDateTime.now());
        String data = JSONUtil.toJsonStr(license);
        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA, PRIVATE_KEY, null);
        byte[] signBytes = sign.sign(data.getBytes(StandardCharsets.UTF_8));
        license.setSignature(Base64.encode(signBytes));
        return JSONUtil.toJsonStr(license);
    }

    @Override
    public License parse(String licenseStr) {
        if (StrUtil.isBlank(licenseStr)) {
            return null;
        }
        return JSONUtil.toBean(licenseStr, License.class);
    }

    @Override
    public void save(String licenseStr) {
        stringRedisTemplate.opsForValue().set(LICENSE_KEY, licenseStr);
    }

    @Override
    public String get() {
        return stringRedisTemplate.opsForValue().get(LICENSE_KEY);
    }

    @Override
    public License current() {
        return parse(get());
    }

    @Override
    public byte[] exportLicense() {
        String licenseStr = get();
        if (StrUtil.isBlank(licenseStr)) {
            return new byte[0];
        }
        return licenseStr.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void importLicense(InputStream inputStream) {
        String licenseStr = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        License license = parse(licenseStr);
        if (!validate(license)) {
            throw new RuntimeException("License 非法");
        }
        save(licenseStr);
    }

    @Override
    public boolean validate() {
        License license = current();
        if (!validate(license)) {
            return false;
        }
        if (!matchMachine(license)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(License license) {
        if (license == null) {
            return false;
        }
        if (!verifySignature(license)) {
            return false;
        }
        return !isExpired(license);
    }

    @Override
    public boolean verifySignature(License license) {
        String signature = license.getSignature();
        license.setSignature(null);
        String data = JSONUtil.toJsonStr(license);
        license.setSignature(signature);

        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA, null, PUBLIC_KEY);
        return sign.verify(
                data.getBytes(StandardCharsets.UTF_8),
                Base64.decode(signature)
        );
    }

    @Override
    public boolean isExpired(License license) {
        return license.getExpireAt() != null &&
                license.getExpireAt().isBefore(LocalDateTime.now());
    }

    @Override
    public String refresh(License license, LocalDateTime newExpireAt) {
        license.setExpireAt(newExpireAt);
        return generate(license);
    }

    @Override
    public void remove() {
        stringRedisTemplate.delete(LICENSE_KEY);
    }

    @Override
    public String generateMachineCode() {
        return MachineFingerprintUtil.getFingerprint();
    }

    @Override
    public boolean matchMachine(License license) {
        if (StrUtil.isBlank(license.getMachineCode())) {
            return true;
        }
        return StrUtil.equals(license.getMachineCode(), generateMachineCode());
    }

    @Override
    public boolean matchIp(License license, String clientIp) {
        if (CollUtil.isEmpty(license.getAllowIps())) {
            return true;
        }
        return license.getAllowIps().contains(clientIp);
    }

    @Override
    public boolean hasFeature(License license, String featureCode) {
        if (CollUtil.isEmpty(license.getFeatures())) {
            return true;
        }
        return license.getFeatures().contains(featureCode);
    }
}
```

### License 控制器

```java
package io.github.atengk.license.controller;

import io.github.atengk.license.model.License;
import io.github.atengk.license.service.LicenseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * License 控制器
 *
 * 提供 License 导入、导出、查看、校验等功能
 */
@RestController
@RequestMapping("/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * 生成 License（后台使用）
     *
     * @param license License 参数
     * @return License 字符串
     */
    @PostMapping("/generate")
    public String generate(@RequestBody License license) {
        return licenseService.generate(license);
    }

    /**
     * 生成并导出 License（后台使用）
     *
     * @param license License 参数
     */
    @PostMapping("/generate/download")
    public void generateAndDownload(@RequestBody License license,
                                    HttpServletResponse response) {
        try {
            String licenseStr = licenseService.generate(license);

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=license.lic");

            response.getOutputStream().write(licenseStr.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();

        } catch (Exception e) {
            throw new RuntimeException("生成 License 文件失败", e);
        }
    }

    /**
     * 生成机器码
     *
     * @return 机器码
     */
    @GetMapping("/generateMachineCode")
    public String generateMachineCode() {
        return licenseService.generateMachineCode();
    }

    /**
     * 导入 License 文件
     *
     * @param file License 文件
     * @return 是否成功
     */
    @PostMapping("/import")
    public boolean importLicense(@RequestParam("file") MultipartFile file) {
        try {
            licenseService.importLicense(file.getInputStream());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("License 导入失败", e);
        }
    }

    /**
     * 导出 License 文件
     *
     * @param response 响应
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        try {
            byte[] data = licenseService.exportLicense();

            response.setContentType("application/octet-stream");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename=license.lic");

            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("License 导出失败", e);
        }
    }

    /**
     * 获取当前 License 信息
     *
     * @return License 对象
     */
    @GetMapping("/current")
    public License current() {
        return licenseService.current();
    }

    /**
     * 校验当前 License（基础）
     *
     * @return 是否合法
     */
    @GetMapping("/validate")
    public boolean validate() {
        return licenseService.validate();
    }

    /**
     * 删除 License
     *
     * @return 是否成功
     */
    @DeleteMapping("/remove")
    public boolean remove() {
        licenseService.remove();
        return true;
    }
}

```



## 配置过滤器

### License 全局过滤器

```java
package io.github.atengk.license.filter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.license.service.LicenseService;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * License 全局过滤器（所有接口强制校验）
 */
public class LicenseFilter extends OncePerRequestFilter {

    private final LicenseService licenseService;

    public LicenseFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        /**
         * 放行 License 自身接口（否则无法导入授权）
         */
        if (isIgnore(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * 只做基础校验（签名 + 过期 + 机器码）
         */
        boolean valid = licenseService.validate();

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"LICENSE_INVALID\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 忽略路径（必须）
     */
    private boolean isIgnore(String uri) {

        return uri.startsWith("/license")
                || uri.startsWith("/error")
                || uri.startsWith("/actuator");
    }

}
```

### License 过滤器配置

```java
package io.github.atengk.license.config;

import io.github.atengk.license.filter.LicenseFilter;
import io.github.atengk.license.service.LicenseService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * License 过滤器配置
 */
@Configuration
public class LicenseFilterConfig {

    @Bean
    public FilterRegistrationBean<LicenseFilter> licenseFilter(LicenseService licenseService) {

        FilterRegistrationBean<LicenseFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new LicenseFilter(licenseService));
        bean.addUrlPatterns("/*");
        bean.setOrder(1);

        return bean;
    }
}

```



## 测试使用

### 生成 License 文件

**获取机器码**

```
GET: /license/generateMachineCode
```

返回：

```
0b744704d6d65e2278d082fdffcb03bc8f81e4b647a6752099e7184780fcccaa
```

**生成 License**

```
POST: /license/generate/download
BODY:
{
  "licenseId": "LIC-20260402-001",
  "customerName": "测试公司",
  "expireAt": "2026-12-31T23:59:59",
  "machineCode": "0b744704d6d65e2278d082fdffcb03bc8f81e4b647a6752099e7184780fcccaa",
  "allowIps": [
    "127.0.0.1",
    "192.168.1.10"
  ],
  "features": [
    "user:add",
    "user:delete",
    "order:view"
  ],
  "extra": "企业版授权"
}
```

得到 license.lic 文件，文件内容如下：

```json
{
	"licenseId": "LIC-20260402-001",
	"customerName": "测试公司",
	"issuedAt": 1775121974504,
	"expireAt": 1798732799000,
	"machineCode": "0b744704d6d65e2278d082fdffcb03bc8f81e4b647a6752099e7184780fcccaa",
	"allowIps": [
		"192.168.1.10",
		"127.0.0.1"
	],
	"features": [
		"user:add",
		"user:delete",
		"order:view"
	],
	"extra": "企业版授权",
	"signature": "RgstLWlb3PWEpOY2QvdzI5rXXvkkMkEjcWlC0O4l+7ZL7OQ8RQXDZ6aTL7cMIkoaSz7ylottHfoWzYqx0crTzX9h5oJHpGrv0a58b+83MntXxXPlN5RJfjpy6inDS9HujkGn6KDgmSyEHm0DBU4hJ1KZFKR2+D2gUcK+AhlBBxI="
}
```

### 导入 License

```
POST(form-data): /license/import
BODY: file license.lic
```

### 访问接口测试

**创建接口**

```java
package io.github.atengk.license.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/test")
    public String test() {
        return "ok";
    }

}

```

**调用接口测试**

```
GET: /demo/test
```

返回：ok
