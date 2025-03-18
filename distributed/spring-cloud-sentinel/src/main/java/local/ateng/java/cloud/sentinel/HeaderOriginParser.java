package local.ateng.java.cloud.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义 RequestOriginParser 实现，用于解析 HTTP 请求中的 "origin" 请求头。
 * 该类可用于结合 Sentinel 授权规则，根据请求来源进行访问控制。
 * 如果 "origin" 请求头为空，返回默认值 "blank"。
 *
 * @author Ateng
 * @email 2385569970@qq.com
 * @since 2025-03-18
 */
@Component
public class HeaderOriginParser implements RequestOriginParser {

    /**
     * 解析请求头中的 "origin" 字段，返回其值；若为空，则返回 "blank"。
     *
     * @param request 当前 HTTP 请求
     * @return 请求来源标识
     */
    @Override
    public String parseOrigin(HttpServletRequest request) {
        String origin = request.getHeader("origin");
        if (StringUtils.isEmpty(origin)) {
            origin = "blank";
        }
        return origin;
    }
}
