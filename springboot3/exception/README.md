# 全局异常处理

全局异常处理可以通过使用 **`@ControllerAdvice`** 或 **`@RestControllerAdvice`** 结合 **`@ExceptionHandler`** 实现。它是一个优雅的方式，用来集中处理应用程序中的异常，避免在每个控制器中重复处理异常逻辑。

------

**核心注解介绍**

1. **`@ControllerAdvice`**：
    - 作用：定义全局范围的异常处理器，适用于所有的 `Controller`。
    - 处理异常后可以返回视图或 JSON，取决于控制器的返回类型。
2. **`@RestControllerAdvice`**：
    - 是 `@ControllerAdvice` 的扩展，默认将异常处理结果返回为 JSON 或其他对象格式（基于 `@ResponseBody`）。
3. **`@ExceptionHandler`**：
    - 作用：指定要处理的异常类型，可以用于单个控制器或全局异常处理器中。



## 统一返回实体

```java
package local.ateng.java.exception.utils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一 API 响应结果封装类，支持链式调用和额外数据存储。
 * <p>
 * 该类是 **不可变对象（Immutable Object）**，以保证线程安全。
 * 每次修改操作都会创建一个新的 `Result<T>` 实例，避免并发环境下的数据竞争问题。
 * </p>
 *
 * @param <T> 业务数据的类型
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-05
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认成功状态码
     */
    private static final String DEFAULT_SUCCESS_CODE = "0";
    /**
     * 默认成功消息
     */
    private static final String DEFAULT_SUCCESS_MESSAGE = "请求成功";
    /**
     * 默认失败状态码
     */
    private static final String DEFAULT_ERROR_CODE = "-1";
    /**
     * 默认失败消息
     */
    private static final String DEFAULT_ERROR_MESSAGE = "服务器异常，请稍后再试";

    /**
     * 状态码
     */
    private final String code;
    /**
     * 提示信息
     */
    private final String msg;
    /**
     * 业务数据
     */
    private final T data;
    /**
     * 响应时间戳（不可变）
     */
    private final LocalDateTime timestamp;
    /**
     * 额外信息存储（使用不可变 Map，保证线程安全）
     */
    private final Map<String, Object> extra;

    /**
     * 私有构造方法，默认初始化成功状态，extra 为空 Map。
     */
    private Result() {
        this.code = DEFAULT_SUCCESS_CODE;
        this.msg = DEFAULT_SUCCESS_MESSAGE;
        this.data = null;
        this.timestamp = LocalDateTime.now();
        this.extra = Collections.emptyMap();
    }

    /**
     * 统一构造方法，确保 extra 是不可变的，防止并发修改问题。
     *
     * @param code  状态码
     * @param msg   提示信息
     * @param data  业务数据
     * @param extra 额外信息 Map
     */
    private Result(String code, String msg, T data, Map<String, Object> extra) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        // 使用不可变 Map 防止外部修改
        this.extra = (extra == null) ? Collections.emptyMap() : Collections.unmodifiableMap(extra);
    }

    // ==============================
    // 静态工厂方法（标准 API 响应）
    // ==============================

    /**
     * 生成一个成功的响应（无数据）。
     *
     * @param <T> 泛型类型
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success() {
        return new Result<>(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, null, null);
    }

    /**
     * 生成一个成功的响应，包含数据。
     *
     * @param <T>  业务数据类型
     * @param data 业务数据
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, data, null);
    }

    /**
     * 生成一个成功的响应，包含自定义消息和数据。
     *
     * @param <T>  业务数据类型
     * @param msg  自定义成功消息
     * @param data 业务数据
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(DEFAULT_SUCCESS_CODE, msg, data, null);
    }

    /**
     * 生成一个成功的响应，包含自定义状态码、消息和数据。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义成功消息
     * @param data 业务数据
     * @return 返回标准成功响应
     */
    public static <T> Result<T> success(String code, String msg, T data) {
        return new Result<>(code, msg, data, null);
    }

    /**
     * 生成一个失败的响应（无数据）。
     *
     * @param <T> 业务数据类型
     * @return 失败的 Result 实例
     */
    public static <T> Result<T> failure() {
        return new Result<>(DEFAULT_ERROR_CODE, DEFAULT_ERROR_MESSAGE, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义错误消息。
     *
     * @param <T> 业务数据类型
     * @param msg 自定义失败消息
     * @return 失败的 Result 实例
     */
    public static <T> Result<T> failure(String msg) {
        return new Result<>(DEFAULT_ERROR_CODE, msg, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义状态码和消息（无业务数据）。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义失败消息
     * @return 返回标准失败响应
     */
    public static <T> Result<T> failure(String code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义状态码、消息和数据。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义失败消息
     * @param data 业务数据
     * @return 返回标准失败响应
     */
    public static <T> Result<T> failure(String code, String msg, T data) {
        return new Result<>(code, msg, data, null);
    }

    // ==============================
    // 生成新实例的方法（不可变对象）
    // ==============================

    /**
     * 生成新的 `Result<T>`，修改状态码。
     *
     * @param code 新的状态码
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withCode(String code) {
        return new Result<>(code, this.msg, this.data, this.extra);
    }

    /**
     * 生成新的 `Result<T>`，修改提示信息。
     *
     * @param msg 新的提示信息
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withMsg(String msg) {
        return new Result<>(this.code, msg, this.data, this.extra);
    }

    /**
     * 生成新的 `Result<U>`，修改业务数据类型。
     *
     * @param <U>  新的泛型类型
     * @param data 业务数据
     * @return 新的 `Result<U>` 实例
     */
    public <U> Result<U> withData(U data) {
        return new Result<>(this.code, this.msg, data, this.extra);
    }

    /**
     * 生成新的 `Result<T>`，添加单个额外信息。
     *
     * @param key   额外信息的键
     * @param value 额外信息的值
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withExtra(String key, Object value) {
        Map<String, Object> newExtra = new ConcurrentHashMap<>(this.extra);
        newExtra.put(key, value);
        return new Result<>(this.code, this.msg, this.data, newExtra);
    }

    /**
     * 生成新的 `Result<T>`，添加多个额外信息。
     *
     * @param extraData 额外信息的 Map
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withExtra(Map<String, Object> extraData) {
        if (extraData == null || extraData.isEmpty()) {
            return this;
        }
        Map<String, Object> newExtra = new ConcurrentHashMap<>(this.extra);
        newExtra.putAll(extraData);
        return new Result<>(this.code, this.msg, this.data, newExtra);
    }

    // ==============================
    // Getter 方法（保持不可变性）
    // ==============================

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    // ==============================
    // toString 方法
    // ==============================

    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", extra=" + extra +
                '}';
    }
}
```



## 全局异常拦截器

这里只给出关键的ExceptionHandler类，其他涉及到的代码自行去源码中查找

```java
package local.ateng.java.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.exception.constant.AppCodeEnum;
import local.ateng.java.exception.exception.ServiceException;
import local.ateng.java.exception.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理 POST 请求参数校验失败的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationExceptions(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = error.getField();
                firstErrorMessage = error.getDefaultMessage();
            }
            errors.put(error.getField(), error.getDefaultMessage());
        }
        // 打印异常日志
        log.error("处理 POST 请求参数校验失败的异常 ==> {}", ex.getMessage());
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(firstErrorMessage).withData(errors);
    }

    // 处理 GET 请求参数校验失败的异常
    /*@ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolationException(HttpServletRequest request, HttpServletResponse response, ConstraintViolationException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            // 只保留参数名称
            String fieldName = propertyPath.split("\\.")[1];
            String errorMessage = violation.getMessage();
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = fieldName;
                firstErrorMessage = errorMessage;
            }
            errors.put(fieldName, errorMessage);
        }
        // 打印异常日志
        log.error("处理 GET 请求参数校验失败的异常 ==> {}", ex.getMessage());
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.error(firstErrorMessage).setData(errors);
    }*/

    // 处理业务的异常
    @ExceptionHandler(ServiceException.class)
    public final Result handleServiceException(HttpServletRequest request, HttpServletResponse response, ServiceException ex) {
        String message = ex.getMessage();
        String code = ex.getCode();
        HashMap<String, String> data = new HashMap<>();
        data.put("detailMessage", ex.getDetailMessage());
        // 打印异常日志
        log.error("处理业务的异常 ==> {}", message);
        // 设置状态码
        ex.printStackTrace();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(code, message).withData(data);
    }

    // 处理其他的异常
    @ExceptionHandler(Exception.class)
    public final Result handleAllExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        // 定义异常码和消息
        String message;
        // 分批处理异常类型
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            message = "请求方式错误";
        } else if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException || ex instanceof HttpMessageNotReadableException) {
            message = "资源未找到";
        } else if (ex instanceof MissingServletRequestParameterException) {
            message = "请求参数缺失";
        } else if (ex instanceof IllegalArgumentException) {
            message = "非法参数异常";
        } else if (ex instanceof ClassCastException) {
            message = "类型转换错误";
        } else if (ex instanceof ArithmeticException) {
            message = "数据计算异常";
        } else if (ex instanceof IndexOutOfBoundsException) {
            message = "数组越界异常";
        } else if (ex instanceof FileNotFoundException || ex instanceof IOException) {
            message = "文件操作异常";
        } else if (ex instanceof NullPointerException) {
            message = "空指针异常";
        } else if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof NumberFormatException) {
            message = "数据类型不匹配异常";
        } else if (ex instanceof UnsupportedOperationException) {
            message = "不支持的操作异常";
        } else {
            // 默认处理
            message = AppCodeEnum.ERROR.getDescription();
        }
        // 打印异常日志
        log.error("处理其他的异常 ==> {}", message);
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(message);
    }


}
```



## 接口测试

**创建接口**

创建一个测试接口，查看全局异常处理的结果

```java
package local.ateng.java.exception.controller;

import local.ateng.java.exception.constant.AppCodeEnum;
import local.ateng.java.exception.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * 测试接口
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/exception")
    public Result exception(@RequestParam(name = "id", required = false, defaultValue = "0") Long id) {
        long result = 1 / id;
        HashMap<String, Long> map = new HashMap<>() {{
            put("result", result);
            put("null", null);
        }};
        return Result.success(AppCodeEnum.SUCCESS.getCode(), AppCodeEnum.SUCCESS.getDescription()).withData(map);
    }

}
```

**正确使用**

```
C:\Users\admin>curl http://localhost:12006/test/exception?id=1
{"code":"0","msg":"请求成功","data":{"result":1,"null":null}}
```

**异常使用**

由此可以看到全局异常生效

```
C:\Users\admin>curl http://localhost:12006/test/exception?id=0
{"code":"-1","msg":"数据计算异常","data":null}
C:\Users\admin>curl http://localhost:12006/test/exception
{"code":"-1","msg":"数据计算异常","data":null}
C:\Users\admin>curl http://localhost:12006/test/exception?id=null
{"code":"-1","msg":"数据类型不匹配异常","data":null}
```



## 关于统一返回的使用

### **使用 `Result<T>`**

```java
package local.ateng.java.controller;

import local.ateng.java.serialize.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例 Controller，展示如何在 Spring Boot 中使用 Result<T> 封装统一的 API 响应。
 */
@RestController
@RequestMapping("/api")
public class ExampleController {

    /**
     * 获取成功响应的示例，无业务数据。
     *
     * @return 返回标准成功响应
     */
    @GetMapping("/success/no-data")
    public Result<String> successNoData() {
        return Result.success();
    }

    /**
     * 获取成功响应的示例，包含业务数据。
     *
     * @return 返回成功响应，包含业务数据
     */
    @GetMapping("/success/with-data")
    public Result<Map<String, String>> successWithData() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "Hello, World!");
        data.put("status", "success");

        return Result.success(data);
    }

    /**
     * 获取成功响应的示例，包含自定义消息和数据。
     *
     * @return 返回成功响应，包含自定义消息和数据
     */
    @GetMapping("/success/custom-msg")
    public Result<String> successCustomMsg() {
        return Result.success("Custom success message", "This is some business data");
    }

    /**
     * 获取失败响应的示例，包含自定义错误消息。
     *
     * @return 返回失败响应，包含自定义错误消息
     */
    @GetMapping("/failure/custom-msg")
    public Result<String> failureCustomMsg() {
        return Result.failure("Custom failure message");
    }

    /**
     * 获取失败响应的示例，包含自定义错误码和消息。
     *
     * @return 返回失败响应，包含自定义错误码和消息
     */
    @GetMapping("/failure/custom-code-msg")
    public Result<String> failureCustomCodeMsg() {
        return Result.failure("1001", "Custom error code and message");
    }

    /**
     * 获取包含额外信息的成功响应示例。
     *
     * @return 返回成功响应，包含额外信息
     */
    @GetMapping("/success/with-extra")
    public Result<Map<String, String>> successWithExtra() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "Operation was successful");

        return Result.success(data)
                .extra("timestamp", System.currentTimeMillis())  // 添加额外信息
                .extra("source", "example-controller");
    }

    /**
     * 获取失败响应的示例，包含自定义状态码、消息和数据。
     *
     * @return 返回失败响应，包含自定义状态码、消息和数据
     */
    @GetMapping("/failure/with-data")
    public Result<String> failureWithData() {
        return Result.failure("500", "Server error", "Failed to process the request");
    }
}
```

### **解释：**

1. **`successNoData()`**：示范返回一个**成功**的响应，且没有业务数据。
2. **`successWithData()`**：示范返回一个**成功**的响应，包含一些业务数据（如 `Map<String, String>`）。
3. **`successCustomMsg()`**：示范返回一个**成功**的响应，包含自定义消息和数据。
4. **`failureCustomMsg()`**：示范返回一个**失败**的响应，包含自定义失败消息（没有业务数据）。
5. **`failureCustomCodeMsg()`**：示范返回一个**失败**的响应，包含自定义状态码和失败消息。
6. **`successWithExtra()`**：示范返回一个**成功**的响应，并且在返回中包含**额外信息**（通过 `extra()` 方法添加的）。
7. **`failureWithData()`**：示范返回一个**失败**的响应，包含自定义状态码、消息以及业务数据。

------

### **API 响应示例**

#### **成功响应：**

- `/api/success/no-data`

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

- `/api/success/with-data`

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": {
        "message": "Hello, World!",
        "status": "success"
      },
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

#### **失败响应：**

- `/api/failure/custom-msg`

    ```json
    {
      "code": "-1",
      "msg": "Custom failure message",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

- `/api/failure/custom-code-msg`

    ```json
    {
      "code": "1001",
      "msg": "Custom error code and message",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

#### **带额外信息的成功响应：**

- ```
    /api/success/with-extra
    ```

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": {
        "message": "Operation was successful"
      },
      "timestamp": "2025-03-05T10:20:30",
      "extra": {
        "timestamp": 1678013430000,
        "source": "example-controller"
      }
    }
    ```

