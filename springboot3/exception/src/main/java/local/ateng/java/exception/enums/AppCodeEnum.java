package local.ateng.java.exception.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 应用统一业务状态码枚举（完整版 - 第一批）
 *
 * <p>说明：
 * <ol>
 *   <li>该枚举用于在后端（Spring Boot）中实现统一的业务错误码与友好提示，便于前后端（Vue）统一处理。</li>
 *   <li>每个枚举项包含：<code>code</code>（字符串，便于前端存储/比较）、<code>httpStatus</code>（建议的 HTTP 状态码）和<code>message</code>（用户/前端可直接显示的提示）。</li>
 *   <li>枚举按业务领域分段，便于扩展与管理；请确保新增枚举时不与现有 code 冲突并在此类注释中登记分段用途。</li>
 *   <li>第一批包含：通用、认证与权限、请求参数与校验、资源操作、业务规则、文件、服务/通信、消息/通知、系统/基础错误。</li>
 * </ol>
 *
 * <p>使用建议：
 * <ul>
 *   <li>后端抛出异常时推荐使用 {@code ServiceException} 并携带本枚举中的 code 与 message。</li>
 *   <li>前端根据 code 做精细化的展示或埋点；同时也可按 httpStatus 做基础的错误分流（如 401 => 强制登录）。</li>
 *   <li>当需要国际化（i18n）时，前端可根据 code 去本地化资源文件获取用户语言对应的提示信息。</li>
 * </ul>
 *
 * <p>分段约定（可根据项目实际调整）：
 * <ul>
 *   <li>90000 ~ 90099：用户认证与权限</li>
 *   <li>90100 ~ 90199：请求参数与校验</li>
 *   <li>90200 ~ 90299：资源（CRUD）操作相关</li>
 *   <li>90300 ~ 90399：业务规则与流程级错误</li>
 *   <li>90400 ~ 90499：文件、媒体相关</li>
 *   <li>90500 ~ 90599：服务、网络与通信（含超时）</li>
 *   <li>90600 ~ 90699：消息、通知与队列</li>
 *   <li>90700 ~ 90799：系统与通用错误（数据库、缓存、配置等）</li>
 *   <li>90800 ~ 90899：订单与支付（下个批次可扩展）</li>
 *   <li>90900 ~ 90999：第三方集成与 API 调用（下个批次可扩展）</li>
 * </ul>
 *
 * <p>注意：请在团队内约定新增规则与归属区间，避免不同模块使用相同 code 导致歧义。</p>
 *
 * @author 孔余
 * @since 2025-09-22
 */
public enum AppCodeEnum {

    // ==================== 通用 ====================

    /**
     * 请求成功（默认，HTTP 200）
     */
    SUCCESS("0", 200, "请求成功"),

    /**
     * 系统内部错误（兜底，HTTP 500）
     */
    ERROR("-1", 500, "服务器异常，请稍后再试"),

    // ==================== 用户认证与权限 (90000 ~ 90099) ====================

    /**
     * 登录成功（业务性提示，HTTP 200）
     */
    AUTH_USER_LOGIN_SUCCESS("90000", 200, "登录成功"),

    /**
     * 用户不存在（登录/查询时）
     */
    AUTH_USER_NOT_FOUND("90001", 401, "用户不存在或凭证无效"),

    /**
     * 密码或凭证错误
     */
    AUTH_PASSWORD_INCORRECT("90002", 401, "用户名或密码错误"),

    /**
     * 无效访问令牌（格式/签名错误）
     */
    AUTH_INVALID_ACCESS_TOKEN("90003", 401, "无效的访问令牌"),

    /**
     * 用户被禁用
     */
    AUTH_USER_DISABLED("90004", 403, "用户被禁用，请联系管理员"),

    /**
     * 用户已存在（注册场景）
     */
    AUTH_USER_ALREADY_EXISTS("90005", 409, "用户已存在"),

    /**
     * 需要二步验证或额外验证
     */
    AUTH_TWO_FACTOR_AUTH_REQUIRED("90006", 401, "需要两步验证"),

    /**
     * 访问令牌过期
     */
    AUTH_ACCESS_TOKEN_EXPIRED("90007", 401, "访问令牌已过期"),

    /**
     * 刷新令牌无效或已被撤销
     */
    AUTH_INVALID_REFRESH_TOKEN("90008", 401, "刷新令牌无效"),

    /**
     * 未提供身份验证信息（无 Token 等）
     */
    AUTH_NO_AUTHENTICATION_PROVIDED("90009", 401, "未提供身份验证信息"),

    /**
     * 身份验证信息无效（例如签名不匹配）
     */
    AUTH_INVALID_AUTHENTICATION("90010", 401, "无效的身份验证信息"),

    /**
     * 用户名或第三方返回用户名不一致（社交登录场景）
     */
    AUTH_USER_NOT_INCONSISTENT("90011", 400, "用户名与预期不一致"),

    /**
     * 权限不足
     */
    AUTH_FORBIDDEN("90012", 403, "权限不足，无法执行该操作"),

    // ==================== 请求参数与校验 (90100 ~ 90199) ====================

    /**
     * 缺少必要的请求参数（前端或调用方未传）
     */
    PARAM_MISSING_REQUIRED("90101", 400, "缺少必要的请求参数"),

    /**
     * 输入数据格式无效（如 JSON 解析失败）
     */
    PARAM_INVALID_DATA_FORMAT("90102", 400, "无效的输入数据格式"),

    /**
     * 数据校验失败（Bean Validation）
     */
    PARAM_DATA_VALIDATION_FAILED("90103", 400, "数据验证失败"),

    /**
     * 唯一性冲突（数据库唯一索引/业务唯一约束）
     */
    PARAM_DUPLICATE_DATA("90104", 409, "资源已存在或数据重复"),

    /**
     * 参数超出可接受范围
     */
    PARAM_OUT_OF_RANGE("90105", 400, "参数值超出允许范围"),

    /**
     * 包含非法字符（安全校验）
     */
    PARAM_ILLEGAL_CHARACTER("90106", 400, "包含非法字符"),

    /**
     * 请求参数类型不匹配（例如 path/param 类型转换失败）
     */
    PARAM_REQUEST_PARAMETER_TYPE_ERROR("90107", 400, "请求参数类型错误"),

    /**
     * 请求参数格式错误（例如日期格式、枚举映射失败）
     */
    PARAM_REQUEST_PARAMETER_FORMAT_ERROR("90108", 400, "请求参数格式错误"),

    /**
     * 请求参数值不在允许集合中
     */
    PARAM_REQUEST_PARAMETER_VALUE_ERROR("90109", 400, "请求参数值错误"),

    /**
     * 数据格式转换异常（例如 JSON -> DTO）
     */
    PARAM_DATA_FORMAT_CONVERSION_ERROR("90110", 400, "数据格式转换错误"),

    // ==================== 资源操作 (90200 ~ 90299) ====================

    /**
     * 资源已存在（创建时冲突）
     */
    RESOURCE_ALREADY_EXISTS("90201", 409, "资源已存在"),

    /**
     * 资源不存在（查询/更新/删除时）
     */
    RESOURCE_NOT_FOUND("90202", 404, "资源不存在"),

    /**
     * 资源不可修改（受限字段或只读）
     */
    RESOURCE_NOT_MODIFIABLE("90203", 403, "资源不可修改"),

    /**
     * 资源已被删除（软删除场景）
     */
    RESOURCE_DELETED("90204", 410, "资源已被删除"),

    /**
     * 资源已过期（时间窗口失效）
     */
    RESOURCE_EXPIRED("90205", 400, "资源已过期"),

    /**
     * 资源冲突（并发或状态冲突）
     */
    RESOURCE_CONFLICT("90206", 409, "资源冲突，请重试"),

    /**
     * 资源被锁定（悲观/乐观锁）
     */
    RESOURCE_LOCKED("90207", 423, "资源被锁定"),

    /**
     * 资源操作被取消
     */
    RESOURCE_OPERATION_CANCELED("90208", 400, "资源操作被取消"),

    /**
     * 资源当前状态不允许此操作
     */
    RESOURCE_STATE_NOT_ALLOWED("90209", 400, "当前资源状态不允许该操作"),

    /**
     * 资源暂不可用（例如维护中）
     */
    RESOURCE_NOT_AVAILABLE("90210", 503, "资源暂不可用"),

    // ==================== 业务规则与流程 (90300 ~ 90399) ====================

    /**
     * 业务规则不允许执行该操作
     */
    BUSINESS_RULE_NOT_ALLOWED("90301", 400, "业务规则不允许执行此操作"),

    /**
     * 操作被拒绝（权限或规则）
     */
    OPERATION_PERMISSION_DENIED("90302", 403, "操作被拒绝，权限不足"),

    /**
     * 操作冲突（业务层面）
     */
    OPERATION_CONFLICT("90303", 409, "操作冲突"),

    /**
     * 操作依赖前置步骤（流程校验未通过）
     */
    OPERATION_REQUIRES_PREVIOUS_STEPS("90304", 400, "操作需要先完成前置步骤"),

    /**
     * 操作需要特定角色或权限
     */
    OPERATION_REQUIRES_SPECIFIC_ROLE("90305", 403, "操作需要特定角色或权限"),

    /**
     * 当前状态下不允许执行此操作
     */
    OPERATION_INVALID_IN_CURRENT_STATE("90306", 400, "当前状态不允许执行该操作"),

    /**
     * 操作过于频繁（可结合限流/熔断）
     */
    OPERATION_TOO_FREQUENT("90307", 429, "操作过于频繁，请稍后重试"),

    /**
     * 操作超时（业务流程超时）
     */
    OPERATION_TIMEOUT("90308", 504, "操作超时，请稍后重试"),

    /**
     * 操作被中止（手动或系统触发）
     */
    OPERATION_ABORTED("90309", 400, "操作已中止"),

    /**
     * 无效的操作类型
     */
    INVALID_OPERATION_TYPE("90310", 400, "无效的操作类型"),

    // ==================== 文件 / 媒体 (90400 ~ 90499) ====================

    /**
     * 不支持的文件类型
     */
    UNSUPPORTED_FILE_TYPE("90401", 415, "文件类型不支持"),

    /**
     * 文件大小超过限制
     */
    FILE_SIZE_EXCEEDED_LIMIT("90402", 413, "文件大小超过限制"),

    /**
     * 文件不存在
     */
    FILE_NOT_FOUND("90403", 404, "文件未找到"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED("90404", 500, "文件上传失败"),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_FAILED("90405", 500, "文件下载失败"),

    /**
     * 文件已损坏或校验失败
     */
    FILE_CORRUPTED("90406", 422, "文件已损坏或校验失败"),

    /**
     * 文件格式错误
     */
    FILE_FORMAT_ERROR("90407", 422, "文件格式错误"),

    /**
     * 文件访问受限（权限/ACL）
     */
    FILE_ACCESS_RESTRICTED("90408", 403, "文件访问受限"),

    // ==================== 服务 / 通信 (90500 ~ 90599) ====================

    /**
     * 服务不可用（下游服务或模块故障）
     */
    SERVICE_UNAVAILABLE("90501", 503, "服务不可用"),

    /**
     * 服务调用超时
     */
    SERVICE_TIMEOUT("90502", 504, "服务调用超时"),

    /**
     * 网络连接失败
     */
    NETWORK_CONNECTION_FAILED("90503", 502, "网络连接失败"),

    /**
     * 无法连接到外部服务
     */
    EXTERNAL_SERVICE_UNREACHABLE("90504", 502, "无法连接到外部服务"),

    /**
     * 服务请求被拒绝（鉴权/ACL）
     */
    SERVICE_REQUEST_DENIED("90505", 403, "服务请求被拒绝"),

    /**
     * 无法解析下游服务响应
     */
    UNABLE_TO_PARSE_SERVICE_RESPONSE("90506", 502, "无法解析服务响应"),

    /**
     * 服务响应格式错误
     */
    SERVICE_RESPONSE_FORMAT_ERROR("90507", 502, "服务响应格式错误"),

    /**
     * 下游端点未找到
     */
    SERVICE_ENDPOINT_NOT_FOUND("90508", 502, "服务端点未找到"),

    /**
     * 下游端点不可用
     */
    SERVICE_ENDPOINT_NOT_AVAILABLE("90509", 502, "服务端点不可用"),

    /**
     * 下游端点需要升级或版本不兼容
     */
    SERVICE_ENDPOINT_REQUIRES_UPGRADE("90510", 502, "服务端点需要升级或不兼容"),

    // ==================== 消息 / 通知 / 队列 (90600 ~ 90699) ====================

    /**
     * 消息发送失败（通知、邮件、短信）
     */
    MESSAGE_SENDING_FAILED("90601", 500, "消息发送失败"),

    /**
     * 无法找到接收者（用户/终端）
     */
    RECEIVER_NOT_FOUND("90602", 404, "无法找到消息接收者"),

    /**
     * 通知已过期或不再有效
     */
    NOTIFICATION_EXPIRED("90603", 410, "通知已过期"),

    /**
     * 无效的消息内容（格式/校验）
     */
    INVALID_MESSAGE_CONTENT("90604", 422, "无效的消息内容"),

    /**
     * 消息格式错误
     */
    MESSAGE_FORMAT_ERROR("90605", 422, "消息格式错误"),

    /**
     * 接收者不可用（离线/禁用）
     */
    RECEIVER_NOT_AVAILABLE("90606", 503, "消息接收者当前不可用"),

    /**
     * 消息发送被阻止（风控/黑名单）
     */
    MESSAGE_SENDING_BLOCKED("90607", 403, "消息发送被阻止"),

    /**
     * 通知频率限制
     */
    NOTIFICATION_FREQUENCY_LIMIT("90608", 429, "通知发送频率受限"),

    // ==================== 系统 / 通用错误 (90700 ~ 90799) ====================

    /**
     * 未知错误
     */
    UNKNOWN_ERROR("90701", 500, "未知错误"),

    /**
     * 操作失败（通用）
     */
    OPERATION_FAILED("90702", 500, "操作失败"),

    /**
     * 系统维护中
     */
    SYSTEM_MAINTENANCE("90703", 503, "系统维护中，暂时不可用"),

    /**
     * 操作被取消（通用）
     */
    OPERATION_CANCELED("90704", 400, "操作已取消"),

    /**
     * 系统资源不足
     */
    INSUFFICIENT_SYSTEM_RESOURCES("90705", 507, "系统资源不足"),

    /**
     * 数据库错误
     */
    DATABASE_ERROR("90706", 500, "数据库错误"),

    /**
     * 缓存读取失败
     */
    CACHE_READ_FAILURE("90707", 500, "缓存读取失败"),

    /**
     * 外部依赖服务错误
     */
    EXTERNAL_SERVICE_ERROR("90708", 502, "外部依赖服务错误"),

    /**
     * 请求方法不支持
     */
    SYSTEM_CONFIGURATION_ERROR("90709", 500, "系统配置错误"),

    /**
     * 系统配置错误
     */
    REQUEST_METHOD_NOT_SUPPORTED("90701", 405, "请求方法不支持"),

    /**
     * 空指针异常
     */
    NULL_POINTER_ERROR("90702", 500, "系统执行出错（NPE）"),


    // ==================== 预留/占位（90800 ~ 90999） ====================

    /**
     * 订单与支付相关（建议放在下一个批次详细定义）
     */
    ORDER_PAYMENT_PLACEHOLDER("90800", 400, "订单/支付相关错误（占位）"),

    /**
     * 第三方与 API 集成相关（建议下一个批次详细定义）
     */
    THIRD_PARTY_PLACEHOLDER("90900", 502, "第三方服务相关错误（占位）");

    // 枚举字段
    private final String code;
    private final int httpStatus;
    private final String message;

    AppCodeEnum(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 返回唯一业务码（字符串形式）
     *
     * @return 业务码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回建议的 HTTP 状态码（用于统一前端分流）
     *
     * @return HTTP 状态码
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 返回提示信息（面向前端或调用方）
     *
     * @return 提示信息
     */
    public String getMessage() {
        return message;
    }

    // 静态映射，便于通过 code 快速查找枚举
    private static final Map<String, AppCodeEnum> CODE_MAP;

    static {
        Map<String, AppCodeEnum> map = new HashMap<>();
        for (AppCodeEnum value : AppCodeEnum.values()) {
            map.put(value.getCode(), value);
        }
        CODE_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 根据 code 返回对应的枚举（包装 Optional，避免 NPE）
     *
     * @param code 枚举 code
     * @return {@link Optional} 包含匹配的枚举或空
     */
    public static Optional<AppCodeEnum> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    /**
     * 根据 code 返回枚举，若找不到则返回默认值（通常为 {@link #ERROR}）
     *
     * @param code         枚举 code
     * @param defaultValue 默认返回值
     * @return 匹配的枚举或默认值
     */
    public static AppCodeEnum fromCodeOrDefault(String code, AppCodeEnum defaultValue) {
        return fromCode(code).orElse(defaultValue == null ? AppCodeEnum.ERROR : defaultValue);
    }

    @Override
    public String toString() {
        return code + ":" + message;
    }
}
