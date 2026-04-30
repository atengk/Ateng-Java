package io.github.atengk.utils.xml;

/**
 * XML 校验异常。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class XmlValidationException extends XmlException {

    /**
     * 创建 XML 校验异常。
     *
     * @param message 异常信息
     */
    public XmlValidationException(String message) {
        super(message);
    }

    /**
     * 创建 XML 校验异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
