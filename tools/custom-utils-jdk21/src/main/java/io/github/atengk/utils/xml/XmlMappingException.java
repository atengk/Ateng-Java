package io.github.atengk.utils.xml;

/**
 * XML 对象映射异常。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class XmlMappingException extends XmlException {

    /**
     * 创建 XML 对象映射异常。
     *
     * @param message 异常信息
     */
    public XmlMappingException(String message) {
        super(message);
    }

    /**
     * 创建 XML 对象映射异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public XmlMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
