package io.github.atengk.command;

/**
 * 命令接口，定义执行与撤销操作
 */
public interface Command {

    /**
     * 执行命令
     */
    void execute();

    /**
     * 撤销命令（可选实现）
     */
    default void undo() {
        // 默认空实现，可由子类重写
    }
}