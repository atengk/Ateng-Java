package local.ateng.java.exception.exception;

import local.ateng.java.exception.constant.AppCodeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务异常
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示
     */
    private final String message;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    // 默认构造器使用成功的状态码
    public ServiceException() {
        this(AppCodeEnum.ERROR.getCode(), AppCodeEnum.ERROR.getDescription(), null);
    }

    // 构造器仅包含消息
    public ServiceException(String message) {
        this(null, message, null);
    }

    // 构造器包含消息和错误码
    public ServiceException(String code, String message) {
        this(code, message, null);
    }

    // 全参数构造器
    public ServiceException(String code, String message, String detailMessage) {
        super(message); // 调用父类的构造方法设置 message
        this.code = code;
        this.message = message;
        this.detailMessage = detailMessage;
    }

    // 连贯调用设置错误明细
    public ServiceException withDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    // 重写父类的 getMessage 方法
    @Override
    public String getMessage() {
        return message;
    }

}

