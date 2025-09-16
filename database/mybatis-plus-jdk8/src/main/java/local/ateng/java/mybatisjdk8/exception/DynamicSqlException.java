package local.ateng.java.mybatisjdk8.exception;

/**
 * 动态 SQL 相关异常
 *
 * @author 孔余
 * @since 2025-09-16
 */
public class DynamicSqlException extends RuntimeException {

    public DynamicSqlException(String message) {
        super(message);
    }

    public DynamicSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}
