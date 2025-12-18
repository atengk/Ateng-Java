package io.github.atengk.beetl.enums;

/**
 * 数据库类型
 *
 * @author 孔余
 * @since 2025-12-18
 */
public enum DatabaseType {

    MYSQL,
    POSTGRESQL;

    public static DatabaseType fromProductName(String productName) {
        if (productName == null) {
            throw new IllegalArgumentException("Database product name is null");
        }
        String name = productName.toLowerCase();
        if (name.contains("mysql")) {
            return MYSQL;
        }
        if (name.contains("postgresql")) {
            return POSTGRESQL;
        }
        throw new UnsupportedOperationException("Unsupported database: " + productName);
    }
}
