package io.github.atengk.util;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * QLExpress 执行工具类
 *
 * @author Ateng
 * @date 2026/04/07
 */
@Component
@RequiredArgsConstructor
public class QLExpressUtil {

    private final Express4Runner runner;

    /**
     * 执行表达式
     */
    public Object execute(String expression, Map<String, Object> params) {

        QLResult result = runner.execute(
                expression,
                params,
                QLOptions.DEFAULT_OPTIONS
        );

        return result.getResult();
    }
}
