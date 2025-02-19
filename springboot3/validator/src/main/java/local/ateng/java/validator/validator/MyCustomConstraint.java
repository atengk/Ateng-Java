package local.ateng.java.validator.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MyCustomValidator.class)  // 指定校验器
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomConstraint {
    String message() default "自定义校验失败";  // 默认提示消息
    Class<?>[] groups() default {};  // 分组校验
    Class<? extends Payload>[] payload() default {};  // 可选的元数据，用于校验逻辑
}
