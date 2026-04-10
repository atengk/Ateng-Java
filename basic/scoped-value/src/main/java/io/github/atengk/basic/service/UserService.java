package io.github.atengk.basic.service;

import io.github.atengk.basic.context.RequestContext;
import io.github.atengk.basic.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 用户业务示例
 *
 * 演示如何获取 ScopedValue 中的数据
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserService {

    /**
     * 示例方法
     */
    public void process() {
        LogUtil.info("开始处理用户业务");

        try {
            int a = 1 / 0;
        } catch (Exception e) {
            LogUtil.error("业务异常", e);
        }

        LogUtil.info("结束处理用户业务");
    }
}
