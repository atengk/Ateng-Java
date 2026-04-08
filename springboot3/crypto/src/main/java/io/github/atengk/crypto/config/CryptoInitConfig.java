package io.github.atengk.crypto.config;

import io.github.atengk.crypto.util.CryptoUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 加密初始化配置
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Configuration
public class CryptoInitConfig {

    private final CryptoProperties cryptoProperties;

    public CryptoInitConfig(CryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    @PostConstruct
    public void init() {
        CryptoUtil.init(cryptoProperties);
    }
}
