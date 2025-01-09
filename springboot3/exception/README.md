# SpringBoot3全局异常处理相关的模块

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



## 创建GlobalExceptionHandler

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
        return Result.error(firstErrorMessage).setData(errors);
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
        return Result.error(code, message).setData(data);
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
        return Result.error(message);
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
        return Result.success(AppCodeEnum.SUCCESS.getCode(), AppCodeEnum.SUCCESS.getDescription()).setData(map);
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

