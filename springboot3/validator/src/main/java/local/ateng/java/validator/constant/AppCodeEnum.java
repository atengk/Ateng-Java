package local.ateng.java.validator.constant;

public enum AppCodeEnum {
    // 常用 Request successful failed
    SUCCESS("0", "请求成功"),
    ERROR("-1", "服务器异常, 请稍后再试"),

    // 用户认证和权限,
    AUTH_USER_LOGIN_SUCCESS("90000", "登录成功"),
    AUTH_USER_NOT_FOUND("90001", "用户不存在"),
    AUTH_PASSWORD_INCORRECT("90002", "密码错误"),
    AUTH_INVALID_ACCESS_TOKEN("90003", "无效的访问令牌"),
    AUTH_USER_DISABLED("90004", "用户已被禁用"),
    AUTH_USER_ALREADY_EXISTS("90005", "用户已存在"),
    AUTH_TWO_FACTOR_AUTH_REQUIRED("90006", "需要两步验证"),
    AUTH_ACCESS_TOKEN_EXPIRED("90007", "访问令牌过期"),
    AUTH_INVALID_REFRESH_TOKEN("90008", "刷新令牌无效"),
    AUTH_NO_AUTHENTICATION_PROVIDED("90009", "未提供身份验证信息"),
    AUTH_INVALID_AUTHENTICATION("90010", "无效的身份验证信息"),
    AUTH_USER_NOT_INCONSISTENT("90011", "用户名不一致"),

    // 请求参数和数据验证
    PARAM_MISSING_REQUIRED("90101", "缺少必要的请求参数"),
    PARAM_INVALID_DATA_FORMAT("90102", "无效的输入数据格式"),
    PARAM_DATA_VALIDATION_FAILED("90103", "数据验证失败"),
    PARAM_DUPLICATE_DATA("90104", "重复的数据"),
    PARAM_OUT_OF_RANGE("90105", "参数范围超出限制"),
    PARAM_ILLEGAL_CHARACTER("90106", "非法字符"),
    PARAM_REQUEST_PARAMETER_TYPE_ERROR("90107", "请求参数类型错误"),
    PARAM_REQUEST_PARAMETER_FORMAT_ERROR("90108", "请求参数格式错误"),
    PARAM_REQUEST_PARAMETER_VALUE_ERROR("90109", "请求参数值错误"),
    PARAM_DATA_FORMAT_CONVERSION_ERROR("90110", "数据格式转换错误"),

    // 资源操作
    RESOURCE_ALREADY_EXISTS("90201", "资源已存在"),
    RESOURCE_NOT_FOUND("90202", "资源不存在"),
    RESOURCE_NOT_MODIFIABLE("90203", "资源不可修改"),
    RESOURCE_DELETED("90204", "资源已被删除"),
    RESOURCE_EXPIRED("90205", "资源已过期"),
    RESOURCE_CONFLICT("90206", "资源冲突"),
    RESOURCE_LOCKED("90207", "资源被锁定"),
    RESOURCE_OPERATION_CANCELED("90208", "资源操作被取消"),
    RESOURCE_STATE_NOT_ALLOWED("90209", "资源状态不允许当前操作"),
    RESOURCE_NOT_AVAILABLE("90210", "资源不可用"),

    // 业务规则和逻辑
    BUSINESS_RULE_NOT_ALLOWED("90301", "业务规则不允许执行此操作"),
    OPERATION_PERMISSION_DENIED("90302", "操作被拒绝，权限不足"),
    OPERATION_CONFLICT("90303", "操作冲突"),
    OPERATION_REQUIRES_PREVIOUS_STEPS("90304", "操作要求先完成其他步骤"),
    OPERATION_REQUIRES_SPECIFIC_ROLE("90305", "操作需要特定角色或权限"),
    OPERATION_INVALID_IN_CURRENT_STATE("90306", "操作无效，当前状态下不允许执行"),
    OPERATION_TOO_FREQUENT("90307", "操作过于频繁"),
    OPERATION_TIMEOUT("90308", "操作超时"),
    OPERATION_ABORTED("90309", "操作被中止"),
    INVALID_OPERATION_TYPE("90310", "无效的操作类型"),

    // 文件上传/下载
    UNSUPPORTED_FILE_TYPE("90401", "文件类型不支持"),
    FILE_SIZE_EXCEEDED_LIMIT("90402", "文件大小超过限制"),
    FILE_NOT_FOUND("90403", "文件不存在"),
    FILE_UPLOAD_FAILED("90404", "文件上传失败"),
    FILE_DOWNLOAD_FAILED("90405", "文件下载失败"),
    FILE_CORRUPTED("90406", "文件已损坏"),
    FILE_FORMAT_ERROR("90407", "文件格式错误"),
    FILE_ACCESS_RESTRICTED("90408", "文件访问受限"),

    // 服务和通信
    SERVICE_UNAVAILABLE("90501", "服务不可用"),
    SERVICE_TIMEOUT("90502", "服务超时"),
    NETWORK_CONNECTION_FAILED("90503", "网络连接失败"),
    EXTERNAL_SERVICE_UNREACHABLE("90504", "无法连接到外部服务"),
    SERVICE_REQUEST_DENIED("90505", "服务请求被拒绝"),
    UNABLE_TO_PARSE_SERVICE_RESPONSE("90506", "无法解析服务响应"),
    SERVICE_RESPONSE_FORMAT_ERROR("90507", "服务响应格式错误"),
    SERVICE_ENDPOINT_NOT_FOUND("90508", "服务端点未找到"),
    SERVICE_ENDPOINT_NOT_AVAILABLE("90509", "服务端点不可用"),
    SERVICE_ENDPOINT_REQUIRES_UPGRADE("90510", "服务端点需要升级"),

    // 通知和消息
    MESSAGE_SENDING_FAILED("90601", "消息发送失败"),
    RECEIVER_NOT_FOUND("90602", "无法找到通知接收者"),
    NOTIFICATION_EXPIRED("90603", "通知已过期"),
    INVALID_MESSAGE_CONTENT("90604", "无效的消息内容"),
    MESSAGE_FORMAT_ERROR("90605", "消息格式错误"),
    RECEIVER_NOT_AVAILABLE("90606", "消息接收者不可用"),
    MESSAGE_SENDING_BLOCKED("90607", "消息发送被阻止"),
    NOTIFICATION_FREQUENCY_LIMIT("90608", "通知频率限制"),

    // 其他通用状态码
    UNKNOWN_ERROR("90701", "未知错误"),
    OPERATION_FAILED("90702", "操作失败"),
    SYSTEM_MAINTENANCE("90703", "系统维护中"),
    OPERATION_CANCELED("90704", "操作被取消"),
    INSUFFICIENT_SYSTEM_RESOURCES("90705", "系统资源不足"),
    DATABASE_ERROR("90706", "数据库错误"),
    CACHE_READ_FAILURE("90707", "缓存读取失败"),
    EXTERNAL_SERVICE_ERROR("90708", "外部依赖服务错误"),
    SYSTEM_CONFIGURATION_ERROR("90709", "系统配置错误");

    private final String code;
    private final String description;

    AppCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

