package local.ateng.java.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.sign.SaSignUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/user")
public class UserController {

    // 测试登录
    @SaIgnore
    @GetMapping("/login")
    public Result login(String username, String password) {
        // 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
        if ("ateng".equals(username) && "Admin@123".equals(password)) {
            StpUtil.login(1);
            return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription());
        }
        return Result.error(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
    }

    // 查询登录状态
    @GetMapping("/is-login")
    public Result isLogin() {
        boolean result = StpUtil.isLogin();
        if (result) {
            return Result.success();
        }
        return Result.error();
    }

    // 检验当前会话是否已经登录, 如果未登录，则抛出异常：`NotLoginException`
    @GetMapping("/check-login")
    public void checkLogin() {
        StpUtil.checkLogin();
    }

    // 检验当前会话是否已经登录, 如果未登录，则抛出异常：`NotLoginException`
    @GetMapping("/get-token-info")
    public Result getTokenInfo() {
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return Result.success(tokenInfo);
    }

    // 在 Sa-Token的 Session(Account-Session) 中缓存数据
    @PostMapping("/set-session")
    public Result setSession(@RequestBody JSONObject json) {
        SaSession session = StpUtil.getSession();
        session.set("user", json);
        return Result.success();
    }

    // 在 Sa-Token的 Session(Account-Session) 中获取缓存数据
    @GetMapping("/get-session")
    public Result getSession() {
        SaSession session = StpUtil.getSession();
        JSONObject json = (JSONObject) session.get("user");
        return Result.success(json);
    }

    // 获取用户列表数据
    @GetMapping("/list")
    public Result list() {
        // 模拟用户列表数据
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

    // 新增用户
    @PostMapping("/add")
    @SaCheckPermission("user.add")
    public Result add(@RequestBody JSONObject json) {
        // 模拟新增用户
        System.out.println(json);
        return Result.success();
    }

    @PostMapping("/list")
    public Result listJson(@RequestBody JSONObject json) {
        // 模拟用户列表数据
        System.out.println(json);
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

    // 当前会话注销登录
    @GetMapping("/logout")
    public Result logout() {
        StpUtil.logout();
        return Result.success();
    }

    // 获取签名参数，默认15分钟过期
    @GetMapping("/get-sign")
    public String getSign(Long userId, Long money) {
        // 请求参数
        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("money", money);
        // 添加签名参数
        String string = SaSignUtil.addSignParamsAndJoin(paramMap);
        return string;
    }

    @GetMapping("/get-sign-custom")
    public String getSignCustom(Long userId, Long money) {
        // 系统当前时间戳
        long timestamp = System.currentTimeMillis();
        // nonce, 随机32位字符串
        String nonce = RandomUtil.randomString(32);
        // 将请求参数放入Map
        Map<String, Object> paramMap = MapUtil.newHashMap();
        paramMap.put("userId", userId);
        paramMap.put("money", money);
        paramMap.put("timestamp", timestamp);
        paramMap.put("nonce", nonce);
        // 使用 TreeMap 对参数进行排序
        Map<String, Object> paramTreeMap = new TreeMap<>(paramMap);
        StringBuilder sb = new StringBuilder();
        paramTreeMap.forEach((key, value) -> sb.append(key).append("=").append(value).append("&"));
        String queryString = sb + "key=" + SpringUtil.getProperty("sa-token.sign.secret-key");
        // 计算 sign 参数
        String sign = SecureUtil.md5(queryString);
        return "userId=" + userId + "&money=" + money + "&timestamp=" + timestamp + "&nonce=" + nonce + "&sign=" + sign;
    }

    // 需要验证签名
    @GetMapping("/sign")
    public String sign() {
        // 1、校验请求中的签名
        SaSignUtil.checkRequest(SaHolder.getRequest());
        // 2、校验通过，处理业务
        // ...
        // 3、返回
        return "ok";
    }


}

