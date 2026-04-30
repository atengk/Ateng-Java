package io.github.atengk.utils.xml;

/**
 * XML 解析异常。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class XmlParseException extends XmlException {

    /**
     * 创建 XML 解析异常。
     *
     * @param message 异常信息
     */
    public XmlParseException(String message) {
        super(message);
    }

    /**
     * 创建 XML 解析异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public XmlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
