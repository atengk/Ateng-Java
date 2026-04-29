package io.github.atengk.utils.filetype;

/**
 * 文件类型处理基础异常。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class FileTypeException extends RuntimeException {

    /**
     * 创建文件类型异常。
     *
     * @param message 异常消息
     */
    public FileTypeException(String message) {
        super(message);
    }

    /**
     * 创建文件类型异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public FileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
