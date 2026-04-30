package io.github.atengk.utils.validate.config;

import io.github.atengk.utils.validate.ValidateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Configuration;

/**
 * 校验工具类配置
 * 将 Spring Boot 容器管理的 Validator 注入到 ValidateUtil，确保消息源和自定义校验器生效。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Configuration
public class ValidateUtilConfig {

    private final Validator validator;

    public ValidateUtilConfig(Validator validator) {
        this.validator = validator;
    }

    /**
     * 初始化 ValidateUtil 使用的 Validator。
     */
    @PostConstruct
    public void init() {
        ValidateUtil.setValidator(validator);
    }

}
