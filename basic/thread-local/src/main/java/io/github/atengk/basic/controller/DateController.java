package io.github.atengk.basic.controller;

import io.github.atengk.basic.util.DateFormatUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 日期格式化测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class DateController {

    /**
     * 测试格式化
     *
     * http://localhost:8080/test/date/format
     */
    @GetMapping("/test/date/format")
    public String format() {
        return DateFormatUtil.format(new Date());
    }

    /**
     * 测试解析
     *
     * http://localhost:8080/test/date/parse?date=2026-04-10 12:00:00
     */
    @GetMapping("/test/date/parse")
    public String parse(String date) {
        return DateFormatUtil.parse(date).toString();
    }
}
