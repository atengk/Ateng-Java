package local.ateng.java.aop.controller;

import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.aop.annotation.RequestLog;
import local.ateng.java.aop.constants.Module;
import local.ateng.java.aop.constants.Operation;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


@RestController
@RequestMapping("/log")
public class LogController {

    // 测试 GET 请求 - 用于记录请求日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.READ,
            description = "查询用户信息",
            logParams = true,  // 记录请求参数
            logHeaders = true,  // 记录请求头
            logBody = false,    // 不记录请求体
            logResponse = true,  // 记录响应
            logExecutionTime = true  // 记录执行时长
    )
    @GetMapping("/user/{id}")
    public String getUser(@PathVariable("id") String id,
                          @RequestParam(value = "name", required = false) String name) {
        return "用户信息: ID = " + id + ", Name = " + name;
    }

    // 测试 GET 请求 - 用于记录请求日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.READ,
            description = "查询用户信息"
    )
    @GetMapping("/userJson/{id}")
    public Map<String, String> getUserJson(@PathVariable("id") String id,
                                           @RequestParam(value = "name", required = false) String name) {
        return Map.of("id", id, "name", name);
    }

    // 测试 POST 请求 - 用于记录请求日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.CREATE,
            description = "创建新用户",
            logParams = true,  // 记录请求参数
            logHeaders = true,  // 记录请求头
            logBody = true,     // 记录请求体
            logResponse = true,  // 记录响应
            logExecutionTime = true  // 记录执行时长
    )
    @PostMapping("/user")
    public String createUser(@RequestBody User user) {
        return "创建用户: " + user.getName() + "，年龄: " + user.getAge();
    }

    // 测试 PUT 请求 - 用于记录请求日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.UPDATE,
            description = "更新用户信息",
            logParams = true,  // 记录请求参数
            logHeaders = true,  // 记录请求头
            logBody = true,     // 记录请求体
            logResponse = true,  // 记录响应
            logExecutionTime = true  // 记录执行时长
    )
    @PutMapping("/user/{id}")
    public String updateUser(@PathVariable("id") String id, @RequestBody User user) {
        return "更新用户: ID = " + id + "，Name = " + user.getName() + "，Age = " + user.getAge();
    }

    // 测试 DELETE 请求 - 用于记录请求日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.DELETE,
            description = "删除用户",
            logParams = true,  // 记录请求参数
            logHeaders = true,  // 记录请求头
            logBody = false,    // 不记录请求体
            logResponse = true,  // 记录响应
            logExecutionTime = true  // 记录执行时长
    )
    @DeleteMapping("/user/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        return "删除用户: ID = " + id;
    }

    // 测试异常 - 用于记录异常日志
    @RequestLog(
            module = Module.USER,
            operation = Operation.READ,
            description = "测试接口异常记录"
    )
    @PostMapping("/error")
    public String testError(@RequestBody User user) {
        if (user.getAge() < 0) {
            throw new IllegalArgumentException("年龄不能为负");
        }
        return "用户信息: " + user.getName() + ", " + user.getAge();
    }

    // 测试空请求体的 POST 请求 - 用于测试空的请求体日志记录
    @RequestLog(
            module = Module.USER,
            operation = Operation.CREATE,
            description = "创建用户（空请求体）"
    )
    @PostMapping("/user/empty")
    public String createEmptyUser() {
        return "创建用户成功（空请求体）";
    }

    /**
     * 导出简单文本文件
     */
    @RequestLog(
            module = Module.PRODUCT,
            operation = Operation.EXPORT,
            description = "导出简单文本文件"
    )
    @GetMapping("/export")
    public void exportTextFile(HttpServletResponse response) {
        // 简单的文本数据
        String data = "Hello, this is the exported file content.\nLine 2 of the file.\nLine 3 of the file.";

        // 设置响应头
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=sample.txt");

        // 获取输出流
        try (OutputStream out = response.getOutputStream()) {
            out.write(data.getBytes());
            out.flush();  // 确保内容被写入响应
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // User 类，用于演示请求体中的数据
    @Data
    static class User {
        private String name;
        private int age;
    }
}

