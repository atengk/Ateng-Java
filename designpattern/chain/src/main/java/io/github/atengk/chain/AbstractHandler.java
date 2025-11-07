package io.github.atengk.chain;


import io.github.atengk.model.User;

/**
 * 抽象处理器
 * 定义处理请求的标准方法及责任传递机制
 */
public abstract class AbstractHandler {

    /** 下一个处理器 */
    protected AbstractHandler next;

    /**
     * 设置下一个处理器
     */
    public AbstractHandler setNext(AbstractHandler next) {
        this.next = next;
        return next;
    }

    /**
     * 执行处理逻辑
     * @param user 用户信息
     */
    public void handle(User user) {
        doHandle(user);
        if (next != null) {
            next.handle(user);
        }
    }

    /**
     * 每个处理器的具体逻辑
     */
    protected abstract void doHandle(User user);
}
