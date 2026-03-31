package io.github.atengk.interceptor.controller;

import io.github.atengk.interceptor.annotation.GrayRelease;
import io.github.atengk.interceptor.interceptor.GrayReleaseInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 灰度测试接口
 */
@RestController
public class GrayController {

    /**
     * 灰度接口（10%用户走新版本）
     */
    @GrayRelease(percent = 10, version = "v2")
    @GetMapping("/api/gray")
    public String gray(HttpServletRequest request) {

        boolean isGray = Boolean.TRUE.equals(
                request.getAttribute(GrayReleaseInterceptor.GRAY_FLAG)
        );

        if (isGray) {
            // 新版本逻辑
            return "v2 新版本逻辑";
        } else {
            // 老版本逻辑
            return "v1 老版本逻辑";
        }
    }
}
