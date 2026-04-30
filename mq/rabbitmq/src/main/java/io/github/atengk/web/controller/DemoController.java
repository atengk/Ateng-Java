package io.github.atengk.web.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot 3 Web 演示 Controller
 *
 * <p>主要用于验证：
 * <ul>
 *     <li>Spring Boot 3 是否能正常启动</li>
 *     <li>Web 模块是否可用</li>
 *     <li>JSON 返回是否正常</li>
 *     <li>参数绑定、路径变量、请求体解析是否正常</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-29
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 基础连通性测试接口
     *
     * <p>访问：
     * http://localhost:12000/api/demo/hello
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot 4, time = " + LocalDateTime.now();
    }

    /**
     * 返回 JSON 对象示例
     *
     * <p>访问：
     * http://localhost:12000/api/demo/info
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> map = new HashMap<>();
        map.put("project", "boot4-web");
        map.put("version", "1.0.0");
        map.put("framework", "Spring Boot 4");
        map.put("time", LocalDateTime.now());
        return map;
    }

    /**
     * 路径变量演示
     *
     * <p>访问：
     * http://localhost:12000/api/demo/user/1001
     */
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable("id") Long id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("username", "user-" + id);
        map.put("createTime", LocalDateTime.now());
        return map;
    }

    /**
     * 请求参数演示
     *
     * <p>访问：
     * http://localhost:12000/api/demo/param?name=atengk&age=18
     */
    @GetMapping("/param")
    public Map<String, Object> param(
            @RequestParam("name") String name,
            @RequestParam(value = "age", required = false) Integer age) {

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        map.put("time", LocalDateTime.now());
        return map;
    }

    /**
     * POST JSON 请求体演示
     *
     * <p>请求示例：
     * <pre>
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     * </pre>
     *
     * <p>POST：
     * http://localhost:12000/api/demo/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestBody", body);
        result.put("login", true);
        result.put("time", LocalDateTime.now());
        return result;
    }

    /**
     * 异常测试接口
     *
     * <p>访问：
     * http://localhost:12000/api/demo/error
     */
    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("Spring Boot 4 异常演示接口");
    }
}
