package io.github.atengk.constants;

/**
 * WebSocket 业务码常量
 *
 * <p>
 * 用于标识 WebSocket 消息对应的具体业务场景，
 * 便于前后端统一识别与处理。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
public final class WebSocketBizCodeConstants {

    private WebSocketBizCodeConstants() {
    }

    /**
     * 订单创建
     */
    public static final String ORDER_CREATE = "ORDER_CREATE";

    /**
     * 订单取消
     */
    public static final String ORDER_CANCEL = "ORDER_CANCEL";

    /**
     * 任务进度通知
     */
    public static final String TASK_PROGRESS = "TASK_PROGRESS";
}
