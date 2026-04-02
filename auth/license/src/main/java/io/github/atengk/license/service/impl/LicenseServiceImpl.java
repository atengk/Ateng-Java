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