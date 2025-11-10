package io.github.atengk.command;


import io.github.atengk.receiver.FileSystemReceiver;

/**
 * 删除文件命令
 */
public class DeleteFileCommand implements Command {

    private final FileSystemReceiver receiver;
    private final String fileName;

    public DeleteFileCommand(FileSystemReceiver receiver, String fileName) {
        this.receiver = receiver;
        this.fileName = fileName;
    }

    @Override
    public void execute() {
        receiver.deleteFile(fileName);
    }

    @Override
    public void undo() {
        receiver.createFile(fileName);
    }
}