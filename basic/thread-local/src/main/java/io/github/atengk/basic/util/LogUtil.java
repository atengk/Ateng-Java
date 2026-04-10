package io.github.atengk.basic.util;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.basic.holder.LogContextHolder;

/**
 * 日志工具类（增强日志输出）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class LogUtil {

    /**
     * 打印日志（自动带上下文）
     *
     * @param message 日志内容
     */
    public static void info(String message) {

        StringBuilder sb = new StringBuilder();

        // 拼接上下文
        if (MapUtil.isNotEmpty(LogContextHolder.getAll())) {
            sb.append("[");
            LogContextHolder.getAll().forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            sb.append("] ");
        }

        sb.append(message);

        System.out.println(sb.toString());
    }
}
