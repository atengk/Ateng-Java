package local.ateng.java.aop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存储 HTTP 请求和响应日志的实体类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLogInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 模块名
     */
    private String module;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作说明
     */
    private String description;

    /**
     * 方法全路径（类名+方法名）
     */
    private String classMethod;

    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 请求的 URL
     */
    private String url;

    /**
     * 请求方式（GET、POST、PUT、DELETE 等）
     */
    private String method;

    /**
     * 客户端 IP 地址
     */
    private String ip;

    /**
     * User-Agent 请求头
     */
    private String userAgent;

    /**
     * 请求参数（Query String 或表单参数）
     */
    private String params;

    /**
     * 请求头信息
     */
    private String headers;

    /**
     * 请求体（JSON 或表单）
     */
    private String body;

    /**
     * 响应数据
     */
    private String response;

    /**
     * 执行耗时
     */
    private String executionTime;

    /**
     * 异常信息（如有）
     */
    private String exceptionMessage;
}

