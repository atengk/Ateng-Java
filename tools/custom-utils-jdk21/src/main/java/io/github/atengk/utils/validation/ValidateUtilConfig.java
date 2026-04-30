package io.github.atengk.utils.validation;

import jakarta.validation.Validator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/**
 * ValidateUtil 初始化配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class ValidateUtilConfig implements ApplicationRunner {

    private final Validator validator;

    public ValidateUtilConfig(Validator validator) {
        this.validator = Objects.requireNonNull(validator, "Validator 不能为空");
    }

    @Override
    public void run(ApplicationArguments args) {
        ValidateUtil.setValidator(validator);
    }

}
