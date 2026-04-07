package io.github.atengk.xss.util;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;

import java.util.regex.Pattern;

/**
 * XSS 工具类（企业级）
 *
 * 特性：
 * 1. 严格模式（默认）：全量转义
 * 2. 宽松模式：允许部分 HTML
 * 3. 防 script / 事件 / 协议注入
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class XssUtil {

    /**
     * script 标签
     */
    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("(?i)<\\s*script[^>]*>(.*?)<\\s*/\\s*script\\s*>");

    /**
     * 事件属性（onclick 等）
     */
    private static final Pattern EVENT_PATTERN =
            Pattern.compile("(?i)on\\w+\\s*=\\s*['\\\"]?[^'\\\"]*['\\\"]?");

    /**
     * javascript: 协议
     */
    private static final Pattern JS_PROTOCOL_PATTERN =
            Pattern.compile("(?i)javascript:");

    /**
     * CSS expression
     */
    private static final Pattern CSS_EXPRESSION_PATTERN =
            Pattern.compile("(?i)expression\\s*\\(");

    /**
     * 默认清理（严格模式）
     */
    public static String clean(String value) {
        return clean(value, true);
    }

    /**
     * 清理 XSS
     *
     * @param value 原始数据
     * @param strict 是否严格模式
     * @return 过滤后数据
     */
    public static String clean(String value, boolean strict) {

        if (StrUtil.isBlank(value)) {
            return value;
        }

        String result = value;

        /*
         * Step1：去除危险标签
         */
        result = ReUtil.replaceAll(result, SCRIPT_PATTERN, "");

        /*
         * Step2：去除事件属性
         */
        result = ReUtil.replaceAll(result, EVENT_PATTERN, "");

        /*
         * Step3：去除 javascript 协议
         */
        result = ReUtil.replaceAll(result, JS_PROTOCOL_PATTERN, "");

        /*
         * Step4：去除 CSS 表达式
         */
        result = ReUtil.replaceAll(result, CSS_EXPRESSION_PATTERN, "");

        /*
         * Step5：HTML 处理策略
         */
        if (strict) {
            /*
             * 严格模式：全部转义
             */
            result = HtmlUtil.escape(result);
        } else {
            /*
             * 宽松模式：去标签（保留文本）
             */
            result = HtmlUtil.cleanHtmlTag(result);
        }

        return result;
    }
}