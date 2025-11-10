package io.github.atengk.service.interpreter;

/**
 * 抽象表达式接口
 */
public interface Expression {

    /**
     * 解释输入的上下文
     *
     * @param context 输入上下文
     * @return 是否匹配
     */
    boolean interpret(String context);
}
