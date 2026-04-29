package io.github.atengk.utils.filetype;

/**
 * 危险文件类型异常。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class DangerousFileTypeException extends FileTypeException {

    /**
     * 创建危险文件类型异常。
     *
     * @param message 异常消息
     */
    public DangerousFileTypeException(String message) {
        super(message);
    }
}
