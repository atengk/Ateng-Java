package io.github.atengk.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用工具
 */
@Component
@Slf4j
public class CommonTools {

    @Tool(description = "获取当前系统时间")
    public String currentTime() {
        log.info("调用了 [{}] 的方法", "获取当前系统时间");
        return LocalDateTime.now().toString();
    }

    @Tool(description = "计算两个整数的和")
    public int sum(int a, int b) {
        log.info("调用了 [{}] 的方法", "计算两个整数的和");
        return a + b;
    }

    @Tool(description = "根据用户ID查询用户名称")
    public String findUserName(Long userId) {
        log.info("调用了 [{}] 的方法", "根据用户ID查询用户名称");
        return "ateng";
    }

    @Tool(description = "判断用户是否成年")
    public boolean isAdult(int age) {
        log.info("调用了 [{}] 的方法", "判断用户是否成年");
        return age >= 18;
    }

}