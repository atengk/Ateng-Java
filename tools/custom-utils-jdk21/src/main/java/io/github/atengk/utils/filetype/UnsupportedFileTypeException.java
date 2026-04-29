package io.github.atengk.utils.filetype;

/**
 * 不支持的文件类型异常。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class UnsupportedFileTypeException extends FileTypeException {

    /**
     * 创建不支持的文件类型异常。
     *
     * @param message 异常消息
     */
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
