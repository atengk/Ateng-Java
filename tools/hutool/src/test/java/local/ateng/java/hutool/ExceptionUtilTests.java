package local.ateng.java.hutool;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.log.StaticLog;
import org.junit.jupiter.api.Test;

/**
 * 针对异常封装，例如包装为RuntimeException。
 * https://www.hutool.cn/docs/#/core/%E5%BC%82%E5%B8%B8/%E5%BC%82%E5%B8%B8%E5%B7%A5%E5%85%B7-ExceptionUtil?id=%e6%96%b9%e6%b3%95
 *
 * @author 孔余
 * @since 2024-01-17 21:27
 */
public class ExceptionUtilTests {
    // 堆栈转为完整字符串
    @Test
    void test01() {
        String str = ExceptionUtil.stacktraceToString(new RuntimeException("1111"));
        StaticLog.error(str);
    }
}
