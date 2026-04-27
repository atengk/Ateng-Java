package io.github.atengk.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.utils.ValidateUtil;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/**
 * ValidateUtil 工具类使用示例 Controller
 *
 * @author Ateng
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/demo/validate-util")
public class ValidateUtilDemoController {

    private static final Logger log = LoggerFactory.getLogger(ValidateUtilDemoController.class);

    private static final MethodValidateTarget METHOD_VALIDATE_TARGET = new MethodValidateTarget();

    @GetMapping("/validator/info")
    public Dict validatorInfo() {
        Validator validator = ValidateUtil.getValidator();

        return result("getValidator / getExecutableValidator", Dict.create()
                .set("validatorClass", validator.getClass().getName())
                .set("executableValidatorClass", ValidateUtil.getExecutableValidator().getClass().getName()));
    }

    @PostMapping("/validator/rebind-current")
    public Dict rebindCurrentValidator() {
        Validator validator = ValidateUtil.getValidator();

        /*
         * 这里只是为了演示 ValidateUtil.setValidator 方法的调用方式。
         * 实际项目中更推荐在配置类中注入 Spring 容器管理的 Validator。
         */
        ValidateUtil.setValidator(validator);

        return result("setValidator", "已重新设置当前 Validator");
    }

    @PostMapping("/first")
    public Dict validateFirst(@RequestBody ValidateUserRequest request) {
        String error = ValidateUtil.validateFirst(request);
        return result("validateFirst(Object)", error);
    }

    @PostMapping("/first-with-path")
    public Dict validateFirstWithPath(@RequestBody ValidateUserRequest request) {
        String error = ValidateUtil.validateFirst(request, true);
        return result("validateFirst(Object, boolean)", error);
    }

    @PostMapping("/first-create-group")
    public Dict validateFirstCreateGroup(@RequestBody ValidateUserRequest request) {
        String error = ValidateUtil.validateFirst(request, CreateGroup.class);
        return result("validateFirst(Object, Class<?>...)", error);
    }

    @PostMapping("/first-create-group-with-path")
    public Dict validateFirstCreateGroupWithPath(@RequestBody ValidateUserRequest request) {
        String error = ValidateUtil.validateFirst(request, true, CreateGroup.class);
        return result("validateFirst(Object, boolean, Class<?>...)", error);
    }

    @PostMapping("/all")
    public Dict validateAll(@RequestBody ValidateUserRequest request) {
        List<String> errors = ValidateUtil.validateAll(request);
        return result("validateAll(Object)", errors);
    }

    @PostMapping("/all-with-path")
    public Dict validateAllWithPath(@RequestBody ValidateUserRequest request) {
        List<String> errors = ValidateUtil.validateAll(request, true);
        return result("validateAll(Object, boolean)", errors);
    }

    @PostMapping("/all-create-group")
    public Dict validateAllCreateGroup(@RequestBody ValidateUserRequest request) {
        List<String> errors = ValidateUtil.validateAll(request, CreateGroup.class);
        return result("validateAll(Object, Class<?>...)", errors);
    }

    @PostMapping("/all-create-group-with-path")
    public Dict validateAllCreateGroupWithPath(@RequestBody ValidateUserRequest request) {
        List<String> errors = ValidateUtil.validateAll(request, true, CreateGroup.class);
        return result("validateAll(Object, boolean, Class<?>...)", errors);
    }

    @PostMapping("/errors")
    public Dict validateErrors(@RequestBody ValidateUserRequest request) {
        List<ValidateUtil.ValidateError> errors = ValidateUtil.validateErrors(request);
        return result("validateErrors(Object, Class<?>...)", errors);
    }

    @PostMapping("/throw")
    public Dict validateThrow(@RequestBody ValidateUserRequest request) {
        try {
            ValidateUtil.validateThrow(request);
            return result("validateThrow(Object)", "校验通过");
        } catch (IllegalArgumentException exception) {
            log.warn("参数校验失败：{}", exception.getMessage());
            return failure(exception.getMessage());
        }
    }

    @PostMapping("/throw-with-path")
    public Dict validateThrowWithPath(@RequestBody ValidateUserRequest request) {
        try {
            ValidateUtil.validateThrow(request, true);
            return result("validateThrow(Object, boolean)", "校验通过");
        } catch (IllegalArgumentException exception) {
            log.warn("参数校验失败：{}", exception.getMessage());
            return failure(exception.getMessage());
        }
    }

    @PostMapping("/throw-create-group-with-path")
    public Dict validateThrowCreateGroupWithPath(@RequestBody ValidateUserRequest request) {
        try {
            ValidateUtil.validateThrow(request, true, CreateGroup.class);
            return result("validateThrow(Object, boolean, Class<?>...)", "校验通过");
        } catch (IllegalArgumentException exception) {
            log.warn("分组参数校验失败：{}", exception.getMessage());
            return failure(exception.getMessage());
        }
    }

    @PostMapping("/throw-custom")
    public Dict validateThrowCustom(@RequestBody ValidateUserRequest request) {
        try {
            Function<String, BusinessValidateException> exceptionFunction = BusinessValidateException::new;
            ValidateUtil.validateThrow(request, exceptionFunction);
            return result("validateThrow(Object, Function<String, RuntimeException>)", "校验通过");
        } catch (BusinessValidateException exception) {
            log.warn("自定义异常校验失败：{}", exception.getMessage());
            return failure(exception.getMessage());
        }
    }

    @PostMapping("/throw-custom-create-group-with-path")
    public Dict validateThrowCustomCreateGroupWithPath(@RequestBody ValidateUserRequest request) {
        try {
            Function<String, BusinessValidateException> exceptionFunction = message ->
                    new BusinessValidateException("业务参数错误：" + message);

            ValidateUtil.validateThrow(request, exceptionFunction, true, CreateGroup.class);
            return result("validateThrow(Object, Function<String, RuntimeException>, boolean, Class<?>...)", "校验通过");
        } catch (BusinessValidateException exception) {
            log.warn("自定义异常分组校验失败：{}", exception.getMessage());
            return failure(exception.getMessage());
        }
    }

    @PostMapping("/map")
    public Dict validateToMap(@RequestBody ValidateUserRequest request) {
        return result("validateToMap(Object, Class<?>...)", ValidateUtil.validateToMap(request));
    }

    @PostMapping("/multi-map")
    public Dict validateToMultiMap(@RequestBody ValidateUserRequest request) {
        return result("validateToMultiMap(Object, Class<?>...)", ValidateUtil.validateToMultiMap(request));
    }

    @PostMapping("/property/first/{propertyName}")
    public Dict validatePropertyFirst(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        String error = ValidateUtil.validatePropertyFirst(request, propertyName);
        return result("validatePropertyFirst(T, String)", error);
    }

    @PostMapping("/property/first-with-path/{propertyName}")
    public Dict validatePropertyFirstWithPath(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        String error = ValidateUtil.validatePropertyFirst(request, propertyName, true);
        return result("validatePropertyFirst(T, String, boolean)", error);
    }

    @PostMapping("/property/first-create-group-with-path/{propertyName}")
    public Dict validatePropertyFirstCreateGroupWithPath(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        String error = ValidateUtil.validatePropertyFirst(request, propertyName, true, CreateGroup.class);
        return result("validatePropertyFirst(T, String, boolean, Class<?>...)", error);
    }

    @PostMapping("/property/all/{propertyName}")
    public Dict validateProperty(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        List<String> errors = ValidateUtil.validateProperty(request, propertyName);
        return result("validateProperty(T, String)", errors);
    }

    @PostMapping("/property/all-with-path/{propertyName}")
    public Dict validatePropertyWithPath(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        List<String> errors = ValidateUtil.validateProperty(request, propertyName, true);
        return result("validateProperty(T, String, boolean)", errors);
    }

    @PostMapping("/property/all-create-group-with-path/{propertyName}")
    public Dict validatePropertyCreateGroupWithPath(
            @PathVariable String propertyName,
            @RequestBody ValidateUserRequest request) {

        List<String> errors = ValidateUtil.validateProperty(request, propertyName, true, CreateGroup.class);
        return result("validateProperty(T, String, boolean, Class<?>...)", errors);
    }

    @GetMapping("/value/email")
    public Dict validateValue(@RequestParam(required = false) String email) {
        List<String> errors = ValidateUtil.validateValue(
                ValidateUserRequest.class,
                "email",
                email,
                true
        );

        return result("validateValue(Class<T>, String, Object, boolean, Class<?>...)", errors);
    }

    @PostMapping("/valid")
    public Dict isValid(@RequestBody ValidateUserRequest request) {
        boolean valid = ValidateUtil.isValid(request);
        return result("isValid(Object, Class<?>...)", valid);
    }

    @PostMapping("/invalid")
    public Dict isInvalid(@RequestBody ValidateUserRequest request) {
        boolean invalid = ValidateUtil.isInvalid(request);
        return result("isInvalid(Object, Class<?>...)", invalid);
    }

    @PostMapping("/collection")
    public Dict validateCollection(@RequestBody List<ValidateUserRequest> requestList) {
        return result("validateCollection(Collection<?>, Class<?>...)", ValidateUtil.validateCollection(requestList));
    }

    @PostMapping("/collection-with-path")
    public Dict validateCollectionWithPath(@RequestBody List<ValidateUserRequest> requestList) {
        return result(
                "validateCollection(Collection<?>, boolean, Class<?>...)",
                ValidateUtil.validateCollection(requestList, true, CreateGroup.class)
        );
    }

    @PostMapping("/collection-errors")
    public Dict validateCollectionErrors(@RequestBody List<ValidateUserRequest> requestList) {
        return result(
                "validateCollectionErrors(Collection<?>, Class<?>...)",
                ValidateUtil.validateCollectionErrors(requestList, CreateGroup.class)
        );
    }

    @PostMapping("/or-else")
    public Dict validateOrElse(@RequestBody ValidateUserRequest request) {
        Dict response = Dict.create();

        ValidateUtil.validateOrElse(
                request,
                () -> response.set("message", "校验通过"),
                errors -> response.set("errors", errors)
        );

        return result("validateOrElse(Object, Runnable, Consumer<List<String>>, Class<?>...)", response);
    }

    @PostMapping("/or-else-with-path")
    public Dict validateOrElseWithPath(@RequestBody ValidateUserRequest request) {
        Dict response = Dict.create();

        ValidateUtil.validateOrElse(
                request,
                () -> response.set("message", "校验通过"),
                errors -> response.set("errors", errors),
                true,
                CreateGroup.class
        );

        return result("validateOrElse(Object, Runnable, Consumer<List<String>>, boolean, Class<?>...)", response);
    }

    @PostMapping("/method/parameters")
    public Dict validateMethodParameters(@RequestBody ValidateUserRequest request) {
        Method method = getMethod(MethodValidateTarget.class, "createUser", String.class, Integer.class);

        List<String> errors = ValidateUtil.validateMethodParameters(
                METHOD_VALIDATE_TARGET,
                method,
                new Object[]{request.username(), request.age()},
                true
        );

        return result("validateMethodParameters(T, Method, Object[], boolean, Class<?>...)", errors);
    }

    @GetMapping("/method/return-value")
    public Dict validateMethodReturnValue(@RequestParam(required = false) String nickname) {
        Method method = getMethod(MethodValidateTarget.class, "queryNickname");

        List<String> errors = ValidateUtil.validateMethodReturnValue(
                METHOD_VALIDATE_TARGET,
                method,
                nickname,
                true
        );

        return result("validateMethodReturnValue(T, Method, Object, boolean, Class<?>...)", errors);
    }

    private Dict result(String method, Object data) {
        log.info("执行 ValidateUtil 示例方法：{}", method);
        return Dict.create()
                .set("success", true)
                .set("method", method)
                .set("data", data);
    }

    private Dict failure(String message) {
        return Dict.create()
                .set("success", false)
                .set("message", message);
    }

    private Method getMethod(Class<?> targetClass, String methodName, Class<?>... parameterTypes) {
        try {
            return targetClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            log.error("获取方法反射对象失败，class={}，method={}", targetClass.getName(), methodName, exception);
            throw new IllegalStateException("获取方法反射对象失败：" + methodName, exception);
        }
    }

    /**
     * 用户校验请求参数
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public record ValidateUserRequest(

            @NotBlank(message = "用户ID不能为空", groups = UpdateGroup.class)
            String id,

            @NotBlank(message = "用户名不能为空")
            @Size(min = 2, max = 20, message = "用户名长度必须在 2 到 20 个字符之间")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 20, message = "密码长度必须在 6 到 20 个字符之间")
            String password,

            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,

            @NotNull(message = "年龄不能为空")
            @Min(value = 18, message = "年龄不能小于 18")
            Integer age,

            @NotBlank(message = "创建编码不能为空", groups = CreateGroup.class)
            String createCode
    ) {
    }

    /**
     * 创建分组
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public interface CreateGroup {
    }

    /**
     * 更新分组
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public interface UpdateGroup {
    }

    /**
     * 方法级校验目标对象
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class MethodValidateTarget {

        public void createUser(
                @NotBlank(message = "方法参数 username 不能为空") String username,
                @NotNull(message = "方法参数 age 不能为空")
                @Min(value = 18, message = "方法参数 age 不能小于 18") Integer age) {
            // 这里只作为方法参数校验的目标方法，不需要实际业务逻辑。
        }

        @NotBlank(message = "方法返回值 nickname 不能为空")
        public String queryNickname() {
            return "ateng";
        }
    }

    /**
     * 业务校验异常
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class BusinessValidateException extends RuntimeException {

        public BusinessValidateException(String message) {
            super(message);
        }
    }

}