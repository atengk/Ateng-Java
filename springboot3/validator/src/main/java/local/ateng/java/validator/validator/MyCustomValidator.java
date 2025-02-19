package local.ateng.java.validator.validator;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MyCustomValidator implements ConstraintValidator<MyCustomConstraint, String> {

    @Override
    public void initialize(MyCustomConstraint constraintAnnotation) {
        // 初始化方法，通常不需要操作，可以忽略
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // 可以根据需求决定 null 是否有效
        }
        // 例如：校验字符串长度是否大于 5
        return value.length() > 5;
    }
}
