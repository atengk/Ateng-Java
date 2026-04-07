package io.github.atengk.xss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * XSS 配置属性
 *
 * @author 孔余
 * @since 2026-04-05
 */
@Component
@ConfigurationProperties(prefix = "xss")
public class XssProperties {

    /**
     * 是否开启
     */
    private boolean enabled = true;

    /**
     * 忽略路径
     */
    private List<String> excludePaths;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
