package io.github.atengk.utils.zip.exception;

/**
 * 压缩解压工具异常，统一包装底层 IO、格式、密码、校验等异常。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class ZipUtilException extends RuntimeException {

    /**
     * 创建工具异常。
     *
     * @param message 异常信息
     */
    public ZipUtilException(String message) {
        super(message);
    }

    /**
     * 创建工具异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public ZipUtilException(String message, Throwable cause) {
        super(message, cause);
    }
}
