package io.github.atengk.basic.service;

import io.github.atengk.basic.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（日志增强）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class LogService {

    public String process() {

        LogUtil.info("开始处理业务");

        // 模拟业务逻辑
        LogUtil.info("处理中...");

        LogUtil.info("处理完成");

        return "ok";
    }
}
