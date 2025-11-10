package io.github.atengk.service;

import io.github.atengk.command.Command;
import io.github.atengk.command.CreateFileCommand;
import io.github.atengk.command.DeleteFileCommand;
import io.github.atengk.invoker.FileCommandInvoker;
import io.github.atengk.receiver.FileSystemReceiver;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 命令模式示例演示
 */
@Component
public class CommandPatternDemo {

    private final FileCommandInvoker invoker;
    private final FileSystemReceiver receiver;

    public CommandPatternDemo(FileCommandInvoker invoker, FileSystemReceiver receiver) {
        this.invoker = invoker;
        this.receiver = receiver;
    }

    @PostConstruct
    public void runDemo() {
        Command create = new CreateFileCommand(receiver, "demo.txt");
        Command delete = new DeleteFileCommand(receiver, "demo.txt");

        System.out.println("=== 执行命令 ===");
        invoker.executeCommand(create);
        invoker.executeCommand(delete);

        System.out.println("=== 撤销上一步 ===");
        invoker.undoLastCommand();
    }
}
