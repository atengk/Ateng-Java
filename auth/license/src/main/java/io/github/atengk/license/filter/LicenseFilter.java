package io.github.atengk.license.filter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.license.service.LicenseService;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * License 全局过滤器（所有接口强制校验）
 */
public class LicenseFilter extends OncePerRequestFilter {

    private final LicenseService licenseService;

    public LicenseFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        /**
         * 放行 License 自身接口（否则无法导入授权）
         */
        if (isIgnore(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * 只做基础校验（签名 + 过期 + 机器码）
         */
        boolean valid = licenseService.validate();

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"LICENSE_INVALID\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 忽略路径（必须）
     */
    private boolean isIgnore(String uri) {

        return uri.startsWith("/license")
                || uri.startsWith("/error")
                || uri.startsWith("/actuator");
    }

}