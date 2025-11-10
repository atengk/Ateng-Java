package io.github.atengk.command;

import io.github.atengk.receiver.FileSystemReceiver;

/**
 * 创建文件命令
 */
public class CreateFileCommand implements Command {

    private final FileSystemReceiver receiver;
    private final String fileName;

    public CreateFileCommand(FileSystemReceiver receiver, String fileName) {
        this.receiver = receiver;
        this.fileName = fileName;
    }

    @Override
    public void execute() {
        receiver.createFile(fileName);
    }

    @Override
    public void undo() {
        receiver.deleteFile(fileName);
    }
}