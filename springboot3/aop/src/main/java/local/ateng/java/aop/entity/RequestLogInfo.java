package local.ateng.java.aop.entity;


import lombok.Data;

/**
 * 存储 HTTP 请求和响应日志的实体类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
@Data
public class RequestLogInfo {
    private String username;  // 用户名
    private String module;  // 模块名
    private String operationType;  // 操作类型
    private String description;  // 操作说明
    private String classMethod;  // 类名和方法名（全路径）
    private boolean success;  // 请求是否成功
    private String url;
    private String method;
    private String ip;
    private String userAgent;
    private String params;  // 请求参数
    private String headers;  // 请求头
    private String body;  // 请求体
    private String response;  // 响应数据
    private String executionTime;  // 执行时间
    private String exceptionMessage;  // 异常信息
}
