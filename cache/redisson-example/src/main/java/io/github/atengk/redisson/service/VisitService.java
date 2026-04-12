package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访问统计业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final UvService uvService;

    /**
     * 访问页面
     *
     * @param page 页面标识
     * @param userFlag 用户标识（IP / userId）
     */
    public void visit(String page, String userFlag) {

        uvService.record(page, userFlag);

    }

    /**
     * 获取UV
     */
    public long getUv(String page) {

        return uvService.countToday(page);

    }

}