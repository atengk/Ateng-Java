package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.Base64;

/**
 * MyBatis TypeHandler：用于将 MySQL 中的二进制字段（如 BLOB、BINARY、VARBINARY）与 Java 字段进行映射。
 *
 * <p>功能说明：
 * <ul>
 *   <li>将数据库中的二进制数据（byte[]）转换为 Base64 字符串，用于 Java 字段是 String 的情况</li>
 *   <li>将 Java 中的 Base64 字符串解码为 byte[] 后写入数据库</li>
 *   <li>支持查询时自动判断字段是否为 null，避免异常</li>
 * </ul>
 *
 * <p>适用数据库字段类型：
 * <ul>
 *   <li>BLOB</li>
 *   <li>BINARY(n)</li>
 *   <li>VARBINARY(n)</li>
 * </ul>
 *
 * <p>适用 Java 字段类型：
 * <ul>
 *   <li>String（Base64 格式）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @TableField(typeHandler = Base64TypeHandler.class)
 * private String binaryData;
 * }</pre>
 *
 * <p>注意事项：
 * <ul>
 *   <li>若字段为 null，查询时将返回 null，不抛出异常</li>
 *   <li>编码格式为标准 Base64，不包含换行</li>
 * </ul>
 *
 * @author 孔余
 * @since 2025-07-27
 */
public class Base64TypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置非空参数：将 Base64 字符串解码为 byte[] 写入数据库
     *
     * @param ps        PreparedStatement 对象
     * @param i         参数索引（从1开始）
     * @param parameter Base64 编码字符串
     * @param jdbcType  JDBC 类型（应为 BLOB）
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            byte[] decoded = Base64.getDecoder().decode(parameter);
            ps.setBytes(i, decoded);
        } catch (Exception e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 通过列名获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            byte[] bytes = rs.getBytes(columnName);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过列索引获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param rs          结果集
     * @param columnIndex 列索引（从1开始）
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            byte[] bytes = rs.getBytes(columnIndex);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过 CallableStatement 获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param cs          CallableStatement 对象
     * @param columnIndex 输出参数索引
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            byte[] bytes = cs.getBytes(columnIndex);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

