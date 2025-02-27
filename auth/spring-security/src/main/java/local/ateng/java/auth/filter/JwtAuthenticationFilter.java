package local.ateng.java.auth.filter;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.config.SecurityConfig;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.exception.ServiceException;
import local.ateng.java.auth.utils.SecurityUtils;
import local.ateng.java.auth.vo.SysUserVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

