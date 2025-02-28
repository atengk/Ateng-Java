# Spring Security6

Spring Security 6与Spring Boot 3无缝集成，提供强大的认证和授权机制。通过灵活配置，它支持多种认证方式（如JWT、OAuth2）以及细粒度的权限控制。Spring Security 6的更新增强了安全性并简化了配置，使开发者可以更容易地保护Web应用程序。

SpringSecurity6通过集成JWT实现无状态认证。用户登录成功后，系统生成一个JWT令牌，该令牌包含用户信息和权限数据，并设置过期时间。客户端在后续请求中携带此Token，服务端通过过滤器解析和验证Token，确认其有效性后，将认证信息存入安全上下文，从而完成权限校验。这种机制无需在服务器端保存会话数据，提升了系统的扩展性和性能。



## 基础配置

基于 [异常处理](/work/Ateng-Java/springboot3/exception/) 模块做开发演示

### 添加依赖

```xml
<!-- Spring Security 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```



## 基本使用

### 创建过滤器

```java
package local.ateng.java.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 自定义的 JwtAuthenticationFilter 过滤器，用于从请求中提取 JWT Token 并进行认证。
 * 该过滤器会在每次请求时执行一次，验证请求中的 Token 是否有效，并将认证信息存储到 SecurityContext 中。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 过滤器的核心逻辑，处理每个请求并进行身份验证。
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链，用于调用下一个过滤器
     * @throws ServletException 如果发生 Servlet 异常
     * @throws IOException      如果发生 I/O 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求中提取 Token
        String token = getTokenFromRequest(request);

        // 用于演示，实际情况需要验证 Jwt Token 的真实性
        if ("1234567890".equals(token)) {
            // 构造一个 UsernamePasswordAuthenticationToken 对象，并将其设置到 SecurityContext 中
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin", "******", new ArrayList<>());
            authentication.setDetails("我是阿腾");
            // 将认证信息存入 Spring Security 的上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 继续执行过滤器链，传递请求和响应
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token，通常是 Bearer Token。
     *
     * @param request 请求对象
     * @return 提取到的 Token，如果没有则返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从请求头中获取 "Authorization" 字段
        String header = request.getHeader("Authorization");

        // 检查 Authorization 头部是否包含 Bearer Token
        if (header != null && header.startsWith("Bearer ")) {
            // 提取 Token 部分
            return header.substring(7);  // 去掉 "Bearer " 前缀
        }

        // 如果没有找到 Token，则返回 null
        return null;
    }
}
```

### 创建配置

```java
package local.ateng.java.auth.config;

import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.filter.JwtAuthenticationFilter;
import local.ateng.java.auth.utils.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;

/**
 * SpringSecurity 配置
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置 Spring Security 的过滤链，定义各种安全策略和规则。
     *
     * @param httpSecurity HttpSecurity 对象，用于配置安全相关的选项
     * @return 配置好的 SecurityFilterChain
     * @throws Exception 可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 配置 URL 路径的权限控制
                .authorizeHttpRequests(router -> {
                    // 开放登录和 actuator 端点
                    router.requestMatchers("/user/login", "/actuator/**").permitAll()
                            // 限制 /system/** 只能被拥有 "admin" 角色的用户访问
                            .requestMatchers("/system/**").hasRole("admin")
                            // 限制 /user/add 只能被拥有 "user:add" 权限的用户访问
                            .requestMatchers("/user/add").hasAuthority("user.add")
                            // 其他请求需要认证
                            .anyRequest().authenticated();
                })
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用默认登出功能
                .logout(config -> config.disable())
                // 禁用默认的 HTTP Basic 认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 设置 session 管理为无状态（适用于 JWT）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 添加自定义 JWT 认证过滤器
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 禁用 CORS 和 CSRF（通常用于无状态认证）
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                // 配置异常处理，未授权和权限不足时的处理
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling.accessDeniedHandler(accessDeniedHandler());  // 权限不足时调用
                    exceptionHandling.authenticationEntryPoint(authenticationEntryPoint());  // 未认证时调用
                });

        return httpSecurity.build();  // 返回配置好的过滤链
    }


    /**
     * 不使用SpringSecurity的账号密码验证，而是自定义验证账号密码信息
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null;
    }

    /**
     * 创建自定义的 AuthenticationEntryPoint，用于处理未登录（401）状态下的请求。
     * 当用户未登录时，返回一个 JSON 格式的错误信息，提示未登录。
     *
     * @return 返回一个实现了 AuthenticationEntryPoint 接口的 Bean，处理未登录的异常。
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.write(Result.error(AppCodeEnum.AUTH_USER_NOT_LOGIN.getCode(), AppCodeEnum.AUTH_USER_NOT_LOGIN.getDescription()).toString());
            printWriter.flush();
        };
    }

    /**
     * 创建自定义的 AccessDeniedHandler，用于处理权限不足（403）状态下的请求。
     * 当用户在登录后，但没有足够权限时，返回一个 JSON 格式的错误信息，提示权限不足。
     *
     * @return 返回一个实现了 AccessDeniedHandler 接口的 Bean，处理权限不足的异常。
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.write(Result.error(AppCodeEnum.AUTH_INVALID_AUTHENTICATION.getCode(), AppCodeEnum.AUTH_INVALID_AUTHENTICATION.getDescription()).toString());
            printWriter.flush();
        };
    }

}
```

### 创建接口

```java
package local.ateng.java.auth.controller;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.utils.Result;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("/user")
public class UserController {


    @GetMapping("/login")
    public Result login(String username, String password, HttpServletRequest request) {
        // 自定义验证用户的账号和密码（这里只是示例，实际应该去数据库或其他存储验证）
        if (!"admin".equals(username)) {
            return Result.error(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
        }
        if (!"Admin@123".equals(password)) {
            return Result.error(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription());
        }

        // 返回成功
        return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription()).setData(JSONObject.of("token", "1234567890"));
    }


    // 查询用户信息
    @GetMapping("/get-info")
    public Result getInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Object credentials = authentication.getCredentials();
        Object details = authentication.getDetails();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return Result.success(JSONObject.of(
                "principal", principal,
                "credentials", credentials,
                "details", details,
                "authorities", authorities
        ));
    }

    // 获取数据
    @GetMapping("/list")
    public Result list() {
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

}
```

### 使用接口

调用登录接口，返回Jwt Token信息，后续访问其他接口将该Token信息放入Header中Authorization=Bearer ${Token}

#### 登录

```bash
curl -Ss -X GET 'http://localhost:12002/user/login?username=admin&password=Admin@123'
```

![image-20250226090011873](./assets/image-20250226090011873.png)

#### 访问其他接口

```bash
curl -Ss -X GET 'http://localhost:12002/user/get-info' \
--header 'Authorization: Bearer 1234567890'
```

![image-20250226090111998](./assets/image-20250226090111998.png)

如果访问不加Token就会提示错误

![image-20250226090137341](./assets/image-20250226090137341.png)



## 权限认证

### 启用方法注解

在SecurityConfig文件中使用@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)启用方法注解

 *   @EnableMethodSecurity：确保在配置类中启用方法级安全，其中 prePostEnabled = true 用于启用 @PreAuthorize 注解的支持。
 *   securedEnabled = true：如果需要支持 @Secured 注解，确保此选项启用。

```java
@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {
    // ...
}
```

### 配置过滤器

修改JwtAuthenticationFilter过滤器，给 `UsernamePasswordAuthenticationToken` 中添加角色（角色需加ROLE_前缀）和权限

```java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求中提取 Token
        String token = getTokenFromRequest(request);

        // 用于演示，实际情况需要验证 Jwt Token 的真实性
        if ("1234567890".equals(token)) {
            // 角色和权限对象集合
            List<GrantedAuthority> authorities = new ArrayList<>();
            // 角色，注意：Spring Security默认角色需加ROLE_前缀
            authorities.add(new SimpleGrantedAuthority("ROLE_" + "admin"));
            // 权限
            authorities.add(new SimpleGrantedAuthority("user.add"));
            // 构造一个 UsernamePasswordAuthenticationToken 对象，并将其设置到 SecurityContext 中
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin", "******", authorities);
            authentication.setDetails("我是阿腾");
            // 将认证信息存入 Spring Security 的上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 继续执行过滤器链，传递请求和响应
        filterChain.doFilter(request, response);
    }
```

### 配置全局异常

对于注解效验 `@PreAuthorize()` 失败抛出的异常会先被全局异常拦截，所以这样在全局异常里面被拦截了然后再抛出去，从而使SpringSecurity的自定义异常处理器public AccessDeniedHandler accessDeniedHandler() {}方法捕获

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // ...
    
    // 将 Spring Security 异常处理
    @ExceptionHandler({AuthorizationDeniedException.class})
    public void handleException(Exception e) throws Exception{
        // 将 Spring Security 异常继续抛出，以便交给自定义处理器处理
        if (e instanceof AuthorizationDeniedException) {
            throw e;
        }
    }

}
```

### 创建接口

**创建接口，验证权限**

system接口

```java
@RestController
@RequestMapping("/system")
public class SystemController {

    // 获取数据
    @GetMapping("/list")
    public Result list() {
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

}
```

user接口，新增的2个

```java
@RestController
@RequestMapping("/user")
public class UserController {

    // ...
	
    // 注解角色测试
    @GetMapping("/check-role")
    @PreAuthorize("hasRole('admin')")
    public Result checkRole() {
        return Result.success();
    }
    // 注解权限测试
    @PreAuthorize("hasAuthority('user.*')")
    @GetMapping("/check-permission")
    public Result checkPermission() {
        return Result.success();
    }

}
```

### 访问接口

依次访问这三个接口

```bash
curl -Ss  --header 'Authorization: Bearer 1234567890' \
  -X GET 'http://localhost:12002/system/list'

curl -Ss  --header 'Authorization: Bearer 1234567890' \
  -X GET 'http://localhost:12002/user/check-role'

curl -Ss  --header 'Authorization: Bearer 1234567890' \
  -X GET 'http://localhost:12002/user/check-role'
```

将过滤器JwtAuthenticationFilter的角色和权限注释了，再来测试这三个接口

![image-20250226103343434](./assets/image-20250226103343434.png)

出现权限效验失败就进入异常处理器AccessDeniedHandler accessDeniedHandler() {}方法中了

![image-20250226103448627](./assets/image-20250226103448627.png)

## 使用JWT

JWT（JSON Web Token）是一种开放标准（RFC 7519），用于在网络应用环境间安全地传递声明（Claims）。JWT 由三部分组成：头部（Header）、载荷（Payload）和签名（Signature）。头部通常指定签名算法，载荷包含声明信息，签名用来验证消息的完整性和认证信息。JWT 常用于身份验证和信息交换，因其自包含性（无需存储会话信息）和跨平台支持而广泛应用于 Web 和移动应用中的身份验证机制。

参考：

- [Hutool-jwt](https://doc.hutool.cn/pages/jwt)

### 创建用户实体类

```java
package local.ateng.java.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * 返回客户端的实体类信息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String userName;
    private String nickName;
    private String sex;
    private String email;
    private String phoneNumber;
    private Timestamp createTime;
    private Timestamp updateTime;
    /**
     * token
     */
    private String token;
    /**
     * 菜单权限
     */
    private List<String> permissionList;

    /**
     * 角色权限
     */
    private List<String> roleList;

}
```

### 创建配置属性

在 resources 目录下修改 application.yml 配置文件，添加 JWT 相关配置

```yaml
---
# JWT 相关配置
jwt:
  token-name: "token"  # 认证的token名称
  secret-key: "yN7IRdIJm84mOMiJ65KX372fbDl9cRJN"  # JWT的密钥
  expiration: 24 # 过期时间，单位小时
```

### 创建工具类

SpringSecurity 工具类

提供对认证信息获取、JWT 创建与验证的操作

```java
package local.ateng.java.auth.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.ConvertException;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.jwt.JWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.vo.SysUserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;

/**
 * SpringSecurity 工具类
 * 提供对认证信息获取、JWT 创建与验证的操作
 *
 * @author 孔余
 * @since 2025-02-26
 */
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
    // token 名称
    private static String TOKEN_NAME = SpringUtil.getProperty("jwt.token-name", "ateng-token");
    // 从配置文件中注入密钥和过期时间（单位：小时）
    // 默认密钥为 "Admin@123"
    private static String SECRET_KEY = SpringUtil.getProperty("jwt.secret-key", "Admin@123");
    // 默认过期时间为 24 小时
    private static int EXPIRATION_TIME = Integer.parseInt(SpringUtil.getProperty("jwt.expiration", "24"));

    /**
     * 获取当前认证用户的详细信息
     *
     * @return 认证信息中的用户详细数据
     */
    public static SysUserVo getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (SysUserVo) authentication.getDetails();
    }

    /**
     * 设置当前认证信息
     *
     * @param authentication 当前认证信息
     */
    public static void setAuthenticatedUser(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 创建 JWT Token
     *
     * @param user 用户信息对象
     * @return 生成的 JWT Token
     */
    public static String generateJwtToken(SysUserVo user) {
        // 当前时间
        DateTime currentTime = DateUtil.date();
        // 计算 Token 的过期时间
        DateTime expirationTime = DateUtil.offsetHour(currentTime, SecurityUtils.EXPIRATION_TIME);

        return JWT.create()
                .setKey(SECRET_KEY.getBytes())  // 设置密钥
                .setPayload("user", user)      // 设置用户信息作为载荷
                .setNotBefore(currentTime)     // 设置 Token 可用时间
                .setExpiresAt(expirationTime) // 设置过期时间
                .sign();                      // 签名生成 Token
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token
     * @return 解析出的用户信息
     */
    public static SysUserVo parseJwtToken(String token) {
        SysUserVo userVo = null;
        try {
            JWT jwt = JWT.of(token);
            userVo = Convert.convert(SysUserVo.class, jwt.getPayload("user"));
        } catch (ConvertException e) {
            log.error("token={}，解析错误：{}", token, e.getMessage());
            throw new RuntimeException("token解析错误");
        }
        return userVo;
    }

    /**
     * 验证 JWT Token 的签名是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public static Boolean verifyJwtToken(String token) {
        Boolean result = false;
        try {
            result = JWT.of(token).setKey(SECRET_KEY.getBytes()).verify();
        } catch (Exception e) {
            log.error("token={}，效验错误：{}", token, e.getMessage());
            throw new RuntimeException("token效验错误");
        }
        return result;
    }

    /**
     * 从请求中获取指定名称的 token。优先级为：
     * 1. 从 "Authorization" 头部获取 Bearer token；
     * 2. 从指定的 header 中获取；
     * 3. 如果指定的 header 中没有，从请求参数中获取；
     * 4. 如果以上都没有，则从 cookies 中获取指定名称的 token。
     *
     * @param request HttpServletRequest 对象，包含请求信息
     * @return 返回 token 的值，如果没有找到对应的 token 则返回 null
     */
    public static String getToken(HttpServletRequest request) {
        // Step 1: 尝试从 "Authorization" 头部获取 Bearer token
        String token = request.getHeader("Authorization");
        if (token != null && !token.isBlank() && token.startsWith("Bearer ")) {
            // 提取 Bearer 后的 token
            token = token.substring(7);
        }

        // Step 2: 如果 token 为空，从指定的 header 中获取 token
        if (token == null || token.isBlank()) {
            token = request.getHeader(TOKEN_NAME);
        }

        // Step 3: 如果 header 中没有，尝试从请求参数中获取 token
        if (token == null || token.isBlank()) {
            token = request.getParameter(TOKEN_NAME);
        }

        // Step 4: 如果请求参数中没有，从 cookies 中获取指定名称的 token
        if (token == null || token.isBlank()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                // 使用流式操作从 cookies 中查找指定名称的 token
                token = Optional.ofNullable(cookies)
                        .stream()
                        .flatMap(Arrays::stream) // 将 Cookie[] 转换成流
                        .filter(cookie -> TOKEN_NAME.equals(cookie.getName())) // 筛选出符合名称的 cookie
                        .map(Cookie::getValue) // 提取 cookie 的值
                        .findFirst() // 返回第一个匹配的 token 值
                        .orElse(null); // 如果未找到，返回 null
            }
        }

        // 返回最终获取到的 token 或 null
        return token;
    }


    /**
     * 发送统一的 JSON 格式响应。
     *
     * @param response   HttpServletResponse 对象，用于向客户端发送响应
     * @param statusCode 响应的 HTTP 状态码
     * @param code       错误或状态码
     * @param msg        错误或状态信息
     * @throws IOException 如果写入响应时发生 I/O 错误
     */
    public static void sendResponse(HttpServletResponse response, int statusCode, String code, String msg) {
        // 设置响应头
        response.setStatus(statusCode);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 使用 try-with-resources 自动关闭 PrintWriter
        try (PrintWriter printWriter = response.getWriter()) {
            // 构造返回的结果并写入响应
            printWriter.write(Result.error(code, msg).toString());
            printWriter.flush();  // 确保内容已发送到客户端
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 token 写入到响应的 Cookie 中，自动判断是否使用 Secure 属性，并设置 HttpOnly 为 false。
     *
     * @param request  HttpServletRequest 对象，用于检查是否通过 HTTPS
     * @param response HttpServletResponse 对象
     * @param token    值
     * @param path     Cookie 的路径
     */
    public static void addTokenCookie(HttpServletRequest request, HttpServletResponse response, String token, String path, boolean httpOnly) {
        // 创建Cookie，并将其值设置为传入的 token 值
        Cookie cookie = new Cookie(TOKEN_NAME, token);

        // 设置 Cookie 的有效期
        cookie.setMaxAge(EXPIRATION_TIME * 3600);  // 单位：秒。如果为负值，表示会话期间有效，默认为-1
        cookie.setPath(path);  // 设置 Cookie 的路径，"/" 表示对所有路径有效

        // 自动判断是否使用 HTTPS 协议
        boolean isSecure = request.isSecure();  // 判断请求是否使用 HTTPS
        cookie.setSecure(isSecure);  // 如果是 HTTPS，则设置 Secure 为 true

        // 明确设置 HttpOnly 为 false，允许 JavaScript 访问
        cookie.setHttpOnly(httpOnly);  // 允许 JavaScript 访问 Cookie

        // 设置 domain 属性
        cookie.setDomain(request.getServerName());

        // 将 Cookie 添加到响应中
        response.addCookie(cookie);
    }

}
```

### 修改登录接口

修改登录接口和获取用户信息接口

```java
@RestController
@RequestMapping("/user")
public class UserController {


    @GetMapping("/login")
    public Result login(String username, String password, HttpServletRequest request) {
        // 自定义验证用户的账号和密码（这里只是示例，实际应该去数据库或其他存储验证）
        if (!"admin".equals(username)) {
            throw new ServiceException(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
        }
        if (!"Admin@123".equals(password)) {
            throw new ServiceException(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription());
        }
        // 返回的用户信息，实际情况在数据库中查询
        SysUserVo userVo = SysUserVo.builder()
                .userId(1)
                .userName("admin")
                .nickName("阿腾")
                .permissionList(Arrays.asList("user.add", "user.get", "user.update", "user.delete"))
                .roleList(Arrays.asList("super-admin", "admin", "user"))
                .build();
        String jwtToken = SecurityUtils.generateJwtToken(userVo);
        userVo.setToken(jwtToken);
        // 返回成功
        return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription()).setData(userVo);
    }


    // 查询用户信息
    @GetMapping("/get-info")
    public Result getInfo() {
        SysUserVo userVo = SecurityUtils.getAuthenticatedUserDetails();
        return Result.success(userVo);
    }
    
    // ...
}
```

### 修改过滤器

```java
package local.ateng.java.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.utils.SecurityUtils;
import local.ateng.java.auth.vo.SysUserVo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 自定义的 JwtAuthenticationFilter 过滤器，用于从请求中提取 JWT Token 并进行认证。
 * 该过滤器会在每次请求时执行一次，验证请求中的 Token 是否有效，并将认证信息存储到 SecurityContext 中。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private Set<String> allowedUrls;  // 存储放行的接口列表

    // 构造方法接收放行的接口列表
    public JwtAuthenticationFilter(Set<String> allowedUrls) {
        this.allowedUrls = allowedUrls;
    }

    /**
     * 过滤器的核心逻辑，处理每个请求并进行身份验证。
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链，用于调用下一个过滤器
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 如果请求的 URL 在放行列表中，直接放行，不进行 Token 验证
        if (isAllowedUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 从请求中提取 Token
            String token = SecurityUtils.getToken(request);
            // 效验Token的有效性
            Boolean verifyJwtToken = null;
            if (token != null && !token.isBlank()) {
                verifyJwtToken = SecurityUtils.verifyJwtToken(token);
            }
            if (verifyJwtToken != null && verifyJwtToken) {
                // 获取用户的基本信息
                SysUserVo userVo = SecurityUtils.parseJwtToken(token);
                List<GrantedAuthority> authorities = new ArrayList<>();
                userVo.getPermissionList().stream().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                userVo.getRoleList().stream().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                String userName = userVo.getUserName();
                // 构造 UsernamePasswordAuthenticationToken 对象，并将其设置到 SecurityContext 中
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userName, "******", authorities);
                authentication.setDetails(userVo);
                // 将认证信息存入 Spring Security 的上下文中
                SecurityUtils.setAuthenticatedUser(authentication);
            }

            // 继续执行过滤器链，传递请求和响应
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 发送错误直接返回给客户端
            SecurityUtils.sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "-1","token验证失败");
        }
    }

    /**
     * 检查当前请求 URL 是否属于放行的 URL
     *
     * @param requestUri 请求 URI
     * @return 是否是放行的 URL
     */
    private boolean isAllowedUrl(String requestUri) {
        return allowedUrls.stream().anyMatch(requestUri::startsWith);  // 匹配路径前缀
    }

}
```

### 修改配置

修改SecurityConfig文件，将放行的接口单独设置，便于JWT过滤器放行

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    // 配置放行的接口列表
    Set<String> allowedUrls = Set.of("/user/login", "/actuator/**", "/public/**");

    httpSecurity
            // 配置 URL 路径的权限控制
            .authorizeHttpRequests(router -> {
                // 开放登录和 actuator 端点
                router
                        .requestMatchers(allowedUrls.toArray(new String[0])).permitAll()  // 放行的接口
                        // 限制 /system/** 只能被拥有 "admin" 角色的用户访问
                        .requestMatchers("/system/**").hasRole("admin")
                        // 限制 /user/add 只能被拥有 "user:add" 权限的用户访问
                        .requestMatchers("/user/add").hasAuthority("user.add")
                        // 其他请求需要认证
                        .anyRequest().authenticated();
            })
            // 禁用表单登录
            .formLogin(form -> form.disable())
            // 禁用默认登出功能
            .logout(config -> config.disable())
            // 禁用默认的 HTTP Basic 认证
            .httpBasic(httpBasic -> httpBasic.disable())
            // 设置 session 管理为无状态（适用于 JWT）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 添加自定义 JWT 认证过滤器
            .addFilterBefore(new JwtAuthenticationFilter(allowedUrls), UsernamePasswordAuthenticationFilter.class)
            // 禁用 CORS 和 CSRF（通常用于无状态认证）
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            // 配置异常处理，未授权和权限不足时的处理
            .exceptionHandling(exceptionHandling -> {
                exceptionHandling.accessDeniedHandler(accessDeniedHandler());  // 权限不足时调用
                exceptionHandling.authenticationEntryPoint(authenticationEntryPoint());  // 未认证时调用
            });

    return httpSecurity.build();  // 返回配置好的过滤链
}
```

### 使用接口

调用登录接口，返回Jwt Token信息，后续访问其他接口将该Token信息放入Header中Authorization=Bearer ${Token}

#### 登录

```bash
curl -Ss -X GET 'http://localhost:12002/user/login?username=admin&password=Admin@123'
```

![image-20250226161527788](./assets/image-20250226161527788.png)

#### 访问其他接口

```bash
curl -Ss -X GET 'http://localhost:12002/user/get-info' \
--header 'token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjp7InVzZXJJZCI6MSwidXNlck5hbWUiOiJhZG1pbiIsIm5pY2tOYW1lIjoi6Zi_6IW-IiwicGVybWlzc2lvbkxpc3QiOlsidXNlci5hZGQiLCJ1c2VyLmdldCIsInVzZXIudXBkYXRlIiwidXNlci5kZWxldGUiXSwicm9sZUxpc3QiOlsic3VwZXItYWRtaW4iLCJhZG1pbiIsInVzZXIiXX0sIm5iZiI6MTc0MDU1NzY3MCwiZXhwIjoxNzQwNjQ0MDcwfQ.PxFNZZmHXXbsiftLGOFrIT8QR_CshgdIQaCYe4o8GWE'
```

![image-20250226161539653](./assets/image-20250226161539653.png)

如果访问不加Token或者输入错误Token就会提示错误

![image-20250226161654230](./assets/image-20250226161654230.png)

### 登录优化

#### 设置Cookie

在登录接口时，登录成功后使用HttpServletResponse往Cookie里面写一份token

```java
SecurityUtils.addTokenCookie(request, response, jwtToken, "/");
```

完整代码如下：

```java
@GetMapping("/login")
public Result login(String username, String password, HttpServletRequest request, HttpServletResponse response) {
    // 自定义验证用户的账号和密码（这里只是示例，实际应该去数据库或其他存储验证）
    if (!"admin".equals(username)) {
        throw new ServiceException(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
    }
    if (!"Admin@123".equals(password)) {
        throw new ServiceException(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription());
    }
    // 返回的用户信息，实际情况在数据库中查询
    SysUserVo userVo = SysUserVo.builder()
            .userId(1)
            .userName("admin")
            .nickName("阿腾")
            .permissionList(Arrays.asList("user.add", "user.get", "user.update", "user.delete"))
            .roleList(Arrays.asList("super-admin", "admin", "user"))
            .build();
    String jwtToken = SecurityUtils.generateJwtToken(userVo);
    userVo.setToken(jwtToken);
    // 设置Cookie并写入response到客户端
    SecurityUtils.addTokenCookie(request, response, jwtToken, "/", false);
    // 返回成功
    return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription()).setData(userVo);
}
```

#### 使用接口

使用浏览器或者API工具访问登录接口，查看Cookie的写入情况

```bash
http://localhost:12002/user/login?username=admin&password=Admin@123
```

![image-20250227080344864](./assets/image-20250227080344864.png)

此时就可以直接访问其他接口

![image-20250227080426449](./assets/image-20250227080426449.png)



## 使用Redis（实现双Token）

双Token（Access Token + Refresh Token）机制的流程：

1. 用户登录，服务器返回 **Access Token** 和 **Refresh Token**。
2. 客户端存储 **Access Token**（例如`localStorage`）和 **Refresh Token**（通常存储在`HTTP-only` Cookie中）。
3. 客户端在每次请求时，携带 **Access Token**（放在请求头 `Authorization: Bearer <Access Token>`）。
4. 如果 **Access Token** 过期，服务器返回 401 错误。
5. 客户端使用 **Refresh Token** 向服务器请求新的 **Access Token**。
6. 服务器验证 **Refresh Token**，返回新的 **Access Token** 和（可选）新的 **Refresh Token**。
7. 客户端更新存储中的 **Access Token** 和 **Refresh Token**。
8. 客户端继续使用新的 **Access Token** 发起请求。

### 基础配置

参考 [RedisTemplate使用文档](/work/Ateng-Java/cache/redis-template/) 完成Redis的配置

### 添加配置属性

在 resources 目录下修改 application.yml 配置文件，添加 JWT 续期Token（Refresh Token）和Redis Key的配置

```yaml
---
# JWT 相关配置
jwt:
  token-name: "token"  # 认证的token名称
  secret-key: "yN7IRdIJm84mOMiJ65KX372fbDl9cRJN"  # JWT的密钥
  expiration: 24 # 过期时间，单位小时
  refresh-expiration: 30 # 续期Token过期时间，单位天
  redis:
    key-prefix: "ateng:springsecurity:"  # Redis Key的前缀
```

### 添加用户实体类字段

在SysUserVo实体类中添加Refresh Token字段

```java
/**
 * Refresh Token
 */
private String refreshToken;
```

### 修改工具类的Cookie方法

修改SecurityUtils工具类，修改addTokenCookie方法，自定义传入参数

```java
public static void addTokenCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int expiry, String path, boolean httpOnly) {
    // 创建Cookie，并将其值设置为传入的 token 值
    Cookie cookie = new Cookie(name, value);

    // 设置 Cookie 的有效期
    cookie.setMaxAge(expiry);  // 单位：秒。如果为负值，表示会话期间有效，默认为-1
    cookie.setPath(path);  // 设置 Cookie 的路径，"/" 表示对所有路径有效

    // 自动判断是否使用 HTTPS 协议
    boolean isSecure = request.isSecure();  // 判断请求是否使用 HTTPS
    cookie.setSecure(isSecure);  // 如果是 HTTPS，则设置 Secure 为 true

    // 明确设置 HttpOnly 为 false，允许 JavaScript 访问
    cookie.setHttpOnly(httpOnly);  // 允许 JavaScript 访问 Cookie

    // 设置 domain 属性
    cookie.setDomain(request.getServerName());

    // 将 Cookie 添加到响应中
    response.addCookie(cookie);
}
```

### 修改登录接口

```java
    @GetMapping("/login")
    public Result login(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        // 自定义验证用户的账号和密码（这里只是示例，实际应该去数据库或其他存储验证）
        if (!"admin".equals(username)) {
            throw new ServiceException(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
        }
        if (!"Admin@123".equals(password)) {
            throw new ServiceException(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription());
        }
        // 返回的用户信息，实际情况在数据库中查询
        SysUserVo userVo = SysUserVo.builder()
                .userId(1)
                .userName("admin")
                .nickName("阿腾")
                .permissionList(Arrays.asList("user.add", "user.get", "user.update", "user.delete"))
                .roleList(Arrays.asList("super-admin", "admin", "user"))
                .build();
        getAndWriteToken(request, response, userVo, true, null);
        // 返回成功
        return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription()).setData(userVo);
    }

    /**
     * 生成JWT并写入Redis
     *
     * @param request
     * @param response
     * @param userVo         传入用户实体类信息
     * @param isRefreshToken 操作是否是刷新token
     *                       如果是登录设置为true，会同时生成Access Token 和 Refresh Token
     *                       如果是刷新Access Token设置为false，这样只会更新Access，而Refresh Token无变化
     * @param refreshToken   当isRefreshToken为false需要设置refreshToken的值
     */
    private void getAndWriteToken(HttpServletRequest request, HttpServletResponse response, SysUserVo userVo, boolean isRefreshToken, String refreshToken) {
        // 判断refreshToken是否正确设置
        if (!isRefreshToken && StrUtil.isBlank(refreshToken)) {
            throw new RuntimeException("refresh token is empty");
        }
        // Redis Key前缀
        String redisSessionPrefixKey = REDIS_KEY_PREFIX + "login:session:";
        String redisRefreshTokenPrefixKey = REDIS_KEY_PREFIX + "login:refreshToken:";
        String userName = userVo.getUserName();
        String redisTokenKey = redisSessionPrefixKey + userName;
        // 首先判断是否已经登录过（防止一直登录导致Redis中一直新增refreshToken）
        SysUserVo existsUser = (SysUserVo) redisTemplate.opsForValue().get(redisTokenKey);
        if (existsUser != null) {
            refreshToken = existsUser.getRefreshToken();
            // 判断refreshToken是否过期
            if (redisTemplate.hasKey(redisRefreshTokenPrefixKey + refreshToken)) {
                isRefreshToken = false;
            } else {
                isRefreshToken = true;
            }
        }
        // 生成JWT
        userVo.setToken(null);
        userVo.setRefreshToken(null);
        String jwtToken = SecurityUtils.generateJwtToken(userVo);
        userVo.setToken(jwtToken);
        // 写入Access Token到Redis的Key
        // 原有Access Token还未过期时刷新Access Token，只会刷新Access Token，其他的不便
        if (!isRefreshToken) {
            userVo.setRefreshToken(refreshToken);
            redisTemplate.opsForValue().set(redisTokenKey, userVo, Duration.ofHours(EXPIRATION_TIME));
            SecurityUtils.addTokenCookie(request, response, TOKEN_NAME, jwtToken, EXPIRATION_TIME * 3600, "/", false);
            return;
        }
        // 生成Refresh Token
        refreshToken = IdUtil.nanoId(32);
        userVo.setRefreshToken(refreshToken);
        String redisRefreshTokenKey = redisRefreshTokenPrefixKey + refreshToken;
        // 写入Redis
        redisTemplate.opsForValue().set(redisTokenKey, userVo, Duration.ofHours(EXPIRATION_TIME));
        redisTemplate.opsForValue().set(redisRefreshTokenKey, userName, Duration.ofDays(REFRESH_EXPIRATION_TIME));
        // 将Access Token写入Cookie中
        SecurityUtils.addTokenCookie(request, response, TOKEN_NAME, jwtToken, EXPIRATION_TIME * 3600, "/", false);
        // 将Refresh Token写入Cookie中
        SecurityUtils.addTokenCookie(request, response, "refreshToken", refreshToken, REFRESH_EXPIRATION_TIME * 3600 * 24, "/", false);
    }
    
    // ...
}
```

登录后可以看到返回了refreshToken的数据，并且Cookie和Redis中都存在token和refreshToken

![image-20250227114752889](./assets/image-20250227114752889.png)

### 创建刷新Token接口

```java
/**
 * 刷新Token，传入Refresh Token后效验是否有效，然后返回Access Token
 */
@GetMapping("/refresh-token")
public Result login(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
    // 从过期的Token中获取用户信息
    String token = SecurityUtils.getToken(request);
    SysUserVo userVo = SecurityUtils.parseJwtToken(token);
    // 判断refreshToken是否过期，过期则提示重新登录
    String redisRefreshTokenPrefixKey = REDIS_KEY_PREFIX + "login:refreshToken:";
    String redisRefreshTokenKey = redisRefreshTokenPrefixKey + refreshToken;
    String userName = (String) redisTemplate.opsForValue().get(redisRefreshTokenKey);
    if (StrUtil.isBlank(userName)) {
        throw new ServiceException(AppCodeEnum.AUTH_ACCESS_TOKEN_EXPIRED.getCode(), AppCodeEnum.AUTH_ACCESS_TOKEN_EXPIRED.getDescription());
    }
    getAndWriteToken(request, response, userVo, false, refreshToken);
    // 返回 Access Token
    return Result.success().setData(userVo.getToken());
}
```

将该接口添加到SpringSecurity的放行接口中

```java
Set<String> allowedUrls = Set.of("/user/login", "/actuator/**", "/public/**", "/user/refresh-token");
```

![image-20250227110614655](./assets/image-20250227110614655.png)

如果前端检查到Token过期了，就调用该接口（需要将旧的Token设置在请求中）获取新的Access Token，如果Refresh Token也过期了就只能重新登录了

![image-20250227150846624](./assets/image-20250227150846624.png)

### 退出登录

要实现退出登录，意思就是Token会失效，可以通过过滤器中在多加一个条件，判断Redis中的Token情况。

#### 修改SecurityConfig

```java
@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityConfig {
    // 配置放行的接口列表
    public static final Set<String> ALLOWED_URLS = Set.of("/user/login", "/actuator/**", "/public/**", "/user/refresh-token");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置 Spring Security 的过滤链，定义各种安全策略和规则。
     *
     * @param httpSecurity HttpSecurity 对象，用于配置安全相关的选项
     * @return 配置好的 SecurityFilterChain
     * @throws Exception 可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 配置 URL 路径的权限控制
                .authorizeHttpRequests(router -> {
                    // 开放登录和 actuator 端点
                    router
                            .requestMatchers(ALLOWED_URLS.toArray(new String[0])).permitAll()  // 放行的接口
                            // 限制 /system/** 只能被拥有 "admin" 角色的用户访问
                            .requestMatchers("/system/**").hasRole("admin")
                            // 限制 /user/add 只能被拥有 "user:add" 权限的用户访问
                            .requestMatchers("/user/add").hasAuthority("user.add")
                            // 其他请求需要认证
                            .anyRequest().authenticated();
                })
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用默认登出功能
                .logout(config -> config.disable())
                // 禁用默认的 HTTP Basic 认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 设置 session 管理为无状态（适用于 JWT）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 添加自定义 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 禁用 CORS 和 CSRF（通常用于无状态认证）
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                // 配置异常处理，未授权和权限不足时的处理
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling.accessDeniedHandler(accessDeniedHandler());  // 权限不足时调用
                    exceptionHandling.authenticationEntryPoint(authenticationEntryPoint());  // 未认证时调用
                });

        return httpSecurity.build();  // 返回配置好的过滤链
    }
    
    // ...
}
```

#### 修改JwtAuthenticationFilter

```java
/**
 * 自定义的 JwtAuthenticationFilter 过滤器，用于从请求中提取 JWT Token 并进行认证。
 * 该过滤器会在每次请求时执行一次，验证请求中的 Token 是否有效，并将认证信息存储到 SecurityContext 中。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Redis Key的前缀
    private static String REDIS_KEY_PREFIX = SpringUtil.getProperty("jwt.redis.key-prefix", "ateng:springsecurity:");
    private final RedisTemplate redisTemplate;

    /**
     * 过滤器的核心逻辑，处理每个请求并进行身份验证。
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链，用于调用下一个过滤器
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 如果请求的 URL 在放行列表中，直接放行，不进行 Token 验证
        if (isAllowedUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 从请求中提取 Token
            String token = SecurityUtils.getToken(request);
            // 效验Token的有效性
            Boolean verifyJwtToken = null;
            if (token != null && !token.isBlank()) {
                verifyJwtToken = SecurityUtils.verifyJwtToken(token);
            }
            if (verifyJwtToken != null && verifyJwtToken) {
                SysUserVo userVo = SecurityUtils.parseJwtToken(token);
                // Redis Key前缀
                String redisSessionPrefixKey = REDIS_KEY_PREFIX + "login:session:";
                String userName = userVo.getUserName();
                String redisTokenKey = redisSessionPrefixKey + userName;
                if (!redisTemplate.hasKey(redisTokenKey)) {
                    throw new ServiceException(AppCodeEnum.AUTH_ACCESS_TOKEN_EXPIRED.getCode(), AppCodeEnum.AUTH_ACCESS_TOKEN_EXPIRED.getDescription());
                }
                // 获取用户的基本信息
                List<GrantedAuthority> authorities = new ArrayList<>();
                userVo.getPermissionList().stream().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                userVo.getRoleList().stream().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                // 构造 UsernamePasswordAuthenticationToken 对象，并将其设置到 SecurityContext 中
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userName, "******", authorities);
                authentication.setDetails(userVo);
                // 将认证信息存入 Spring Security 的上下文中
                SecurityUtils.setAuthenticatedUser(authentication);
            }

            // 继续执行过滤器链，传递请求和响应
            filterChain.doFilter(request, response);
        } catch (ServiceException e) {
            // 发送错误直接返回给客户端
            SecurityUtils.sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getCode(), e.getMessage());
        } catch (Exception e) {
            // 发送错误直接返回给客户端
            SecurityUtils.sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "-1", "token验证失败");
        }
    }

    /**
     * 检查当前请求 URL 是否属于放行的 URL
     *
     * @param requestUri 请求 URI
     * @return 是否是放行的 URL
     */
    private boolean isAllowedUrl(String requestUri) {
        return SecurityConfig.ALLOWED_URLS.stream().anyMatch(requestUri::startsWith);  // 匹配路径前缀
    }

}
```

#### 创建退出登录接口

```java
    // 退出登录
    @GetMapping("/logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        SysUserVo userVo = SecurityUtils.getAuthenticatedUserDetails();
        // 删除 Access Token
        String redisSessionPrefixKey = REDIS_KEY_PREFIX + "login:session:";
        String userName = userVo.getUserName();
        String redisTokenKey = redisSessionPrefixKey + userName;
        SysUserVo redisUserVo = (SysUserVo) redisTemplate.opsForValue().getAndDelete(redisTokenKey);
        // 删除 Refresh Token
        String redisRefreshTokenPrefixKey = REDIS_KEY_PREFIX + "login:refreshToken:";
        String refreshToken = redisUserVo.getRefreshToken();
        String redisRefreshTokenKey = redisRefreshTokenPrefixKey + refreshToken;
        redisTemplate.delete(redisRefreshTokenKey);
        // 清除Cookie
        SecurityUtils.addTokenCookie(request, response, TOKEN_NAME, "", 0, "/", false);
        SecurityUtils.addTokenCookie(request, response, "refreshToken", "", 0, "/", false);
        return Result.success();
    }
```

![image-20250227152129997](./assets/image-20250227152129997.png)



### 其他功能

- 踢人下线：跟退出登录一个逻辑，只是需要指定用户名
- 整合数据库：结合数据库实现真实的用户信息
