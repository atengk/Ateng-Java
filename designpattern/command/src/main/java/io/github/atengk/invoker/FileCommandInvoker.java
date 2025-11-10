package io.github.atengk.invoker;


import io.github.atengk.command.Command;
import org.springframework.stereotype.Component;

import java.util.Stack;

/**
 * 命令调用者，负责执行命令与管理撤销操作
 */
@Component
public class FileCommandInvoker {

    private final Stack<Command> commandHistory = new Stack<>();

    /**
     * 执行命令
     */
    public void executeCommand(Command command) {
        command.execute();
        commandHistory.push(command);
    }

    /**
     * 撤销上一步命令
     */
    public void undoLastCommand() {
        if (!commandHistory.isEmpty()) {
            Command last = commandHistory.pop();
            last.undo();
        } else {
            System.out.println("没有可撤销的命令。");
        }
    }
}
