package io.github.atengk.utils.xml;

/**
 * XML 工具类基础异常。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class XmlException extends RuntimeException {

    /**
     * 创建 XML 工具类基础异常。
     *
     * @param message 异常信息
     */
    public XmlException(String message) {
        super(message);
    }

    /**
     * 创建 XML 工具类基础异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public XmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
