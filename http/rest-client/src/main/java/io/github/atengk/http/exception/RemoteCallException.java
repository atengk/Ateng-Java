package io.github.atengk.http.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

import java.net.URI;

/**
 * 远程调用异常
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Getter
public class RemoteCallException extends RuntimeException {

    private final URI uri;

    private final HttpStatusCode statusCode;

    private final String responseBody;

    public RemoteCallException(String message, URI uri, HttpStatusCode statusCode, String responseBody) {
        super(message);
        this.uri = uri;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public RemoteCallException(String message, URI uri, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.uri = uri;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}