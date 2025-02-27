package local.ateng.java.auth.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.exception.ServiceException;
import local.ateng.java.auth.utils.Result;
import local.ateng.java.auth.utils.SecurityUtils;
import local.ateng.java.auth.vo.SysUserVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {
    // Redis Key的前缀
    private static String REDIS_KEY_PREFIX = SpringUtil.getProperty("jwt.redis.key-prefix", "ateng:springsecurity:");
    // token 名称
    private static String TOKEN_NAME = SpringUtil.getProperty("jwt.token-name", "ateng-token");
    // 续期Token过期时间，单位天
    private static int REFRESH_EXPIRATION_TIME = Integer.parseInt(SpringUtil.getProperty("jwt.refresh-expiration", "30"));
    // 默认过期时间为 24 小时
    private static int EXPIRATION_TIME = Integer.parseInt(SpringUtil.getProperty("jwt.expiration", "24"));

    private final RedisTemplate redisTemplate;

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

    // 查询用户信息
    @GetMapping("/get-info")
    public Result getInfo() {
        SysUserVo userVo = SecurityUtils.getAuthenticatedUserDetails();
        return Result.success(userVo);
    }

    // 获取数据
    @GetMapping("/list")
    public Result list() {
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

    // 新增用户（user.add权限测试）
    @GetMapping("/add")
    public Result add() {
        return Result.success();
    }

    // 注解角色测试
    @GetMapping("/check-role")
    @PreAuthorize("hasRole('admin')")
    public Result checkRole() {
        return Result.success();
    }

    // 注解权限测试
    @PreAuthorize("hasAuthority('user.add')")
    @GetMapping("/check-permission")
    public Result checkPermission() {
        return Result.success();
    }

}
