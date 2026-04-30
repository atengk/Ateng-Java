package io.github.atengk.utils.codec;

/**
 * 编解码类型枚举。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum EncodeType {

    /** URL 编码。 */
    URL,

    /** URL 查询参数编码。 */
    QUERY_PARAM,

    /** URL 路径片段编码。 */
    PATH_SEGMENT,

    /** 标准 Base64 编码。 */
    BASE64,

    /** URL Safe Base64 编码。 */
    BASE64_URL,

    /** MIME Base64 编码。 */
    BASE64_MIME,

    /** 十六进制编码。 */
    HEX,

    /** Unicode 转义编码。 */
    UNICODE,

    /** HTML 转义编码。 */
    HTML,

    /** XML 转义编码。 */
    XML,

    /** JSON 字符串转义编码。 */
    JSON
}
