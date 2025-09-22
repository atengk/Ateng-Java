package local.ateng.java.exception.exception;

import local.ateng.java.exception.enums.AppCodeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 自定义业务异常类
 *
 * <p>用于在业务逻辑处理中主动抛出异常，以区分系统级异常和业务逻辑异常。
 * 该异常继承自 {@link RuntimeException}，支持事务回滚，并包含统一的错误码、
 * 错误提示和详细错误信息。</p>
 *
 * <p>典型使用场景：</p>
 * <ul>
 *   <li>参数符合语法规则，但不满足业务规则时抛出</li>
 *   <li>业务处理失败需要中断流程时抛出</li>
 *   <li>需要向前端返回统一的错误码和错误提示时抛出</li>
 * </ul>
 *
 * <p>本类与 {@link AppCodeEnum} 配合使用，可实现统一的错误码管理。</p>
 *
 * @author 孔余
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     *
     * <p>用于唯一标识错误类型，通常与 {@link AppCodeEnum} 对应。</p>
     */
    private final String code;

    /**
     * 错误提示
     *
     * <p>面向用户或调用方的错误描述，便于前端展示。</p>
     */
    private final String message;

    /**
     * 错误明细
     *
     * <p>通常为内部调试或日志记录使用，避免直接暴露给前端。</p>
     */
    private String detailMessage;

    /**
     * 默认构造函数
     *
     * <p>使用 {@link AppCodeEnum#ERROR} 作为默认错误码和描述。</p>
     */
    public ServiceException() {
        this(AppCodeEnum.ERROR.getCode(), AppCodeEnum.ERROR.getMessage(), null);
    }

    /**
     * 构造函数（仅包含错误提示）
     *
     * @param message 错误提示
     */
    public ServiceException(String message) {
        this(null, message, null);
    }

    /**
     * 构造函数（包含错误码和提示）
     *
     * @param code    错误码
     * @param message 错误提示
     */
    public ServiceException(String code, String message) {
        this(code, message, null);
    }

    /**
     * 全参数构造函数
     *
     * @param code          错误码
     * @param message       错误提示
     * @param detailMessage 错误明细
     */
    public ServiceException(String code, String message, String detailMessage) {
        super(message);
        this.code = code;
        this.message = message;
        this.detailMessage = detailMessage;
    }

    /**
     * 设置错误明细并返回当前对象
     *
     * <p>可用于链式调用，例如：
     * <pre>{@code
     * throw new ServiceException("1001", "业务失败").withDetailMessage("数据库写入失败");
     * }</pre></p>
     *
     * @param detailMessage 错误明细
     * @return 当前 {@link ServiceException} 实例
     */
    public ServiceException withDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    /**
     * 重写父类的 {@code getMessage} 方法
     *
     * <p>保证返回的始终是业务定义的 {@link #message}，
     * 而不是父类的 {@code detailMessage}。</p>
     *
     * @return 错误提示
     */
    @Override
    public String getMessage() {
        return message;
    }

}
