package io.github.atengk.http.constant;

/**
 * 远程 HTTP 调用常量
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class RemoteHttpConstant {

    public static final String CLIENT_DEFAULT = "default";

    public static final String CLIENT_FAST = "fast";

    public static final String CLIENT_LONG = "long";

    public static final String MDC_TRACE_ID = "traceId";

    public static final String DEFAULT_TRACE_HEADER = "X-Trace-Id";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HEADER_ACCEPT = "Accept";

    private RemoteHttpConstant() {
    }

}