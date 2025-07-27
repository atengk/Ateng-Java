package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

/**
 * MyBatis 类型处理器：将 MySQL 的 binary(16) 字段与 Java 的 UUID 类型进行映射转换。
 * <p>
 * 用于处理数据库中使用 UUID_TO_BIN(uuid()) 生成的顺序 UUID（二进制格式），
 * Java 端字段必须使用 {@link java.util.UUID} 类型。
 * </p>
 *
 * <pre>
 * 数据库字段类型：binary(16)
 * Java 字段类型：java.util.UUID
 * </pre>
 * <p>
 * 示例使用：
 * <pre>
 * &#64;TableField(typeHandler = UUIDTypeHandler.class)
 * private UUID uuid;
 * </pre>
 * <p>
 * 注意：实体类字段必须为 {@code UUID} 类型，不能使用 {@code byte[]} 或 {@code String}。
 *
 * @author 孔余
 * @since 2025-07-27
 */
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

    /**
     * 将 Java UUID 类型参数设置到 PreparedStatement 中，以字节数组形式写入 binary(16) 字段。
     *
     * @param ps       PreparedStatement 对象
     * @param i        参数索引（从1开始）
     * @param uuid     要写入的 UUID 值，不能为空
     * @param jdbcType JDBC 类型（可为空）
     * @throws SQLException SQL 操作异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID uuid, JdbcType jdbcType) throws SQLException {
        try {
            ps.setBytes(i, uuidToBytes(uuid));
        } catch (Exception e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 从结果集中获取 UUID 值（通过列名），并将 binary(16) 转为 UUID 类型。
     *
     * @param rs         结果集对象
     * @param columnName 列名
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 从结果集中获取 UUID 值（通过列索引），并将 binary(16) 转为 UUID 类型。
     *
     * @param rs          结果集对象
     * @param columnIndex 列索引（从1开始）
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 从存储过程中获取 UUID 值（通过列索引），并将 binary(16) 转为 UUID 类型。
     *
     * @param cs          CallableStatement 对象
     * @param columnIndex 列索引（从1开始）
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 将 UUID 对象转换为 16 字节的二进制数组。
     *
     * @param uuid 要转换的 UUID
     * @return 二进制数组表示的 UUID（长度为16）
     */
    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * 将 16 字节的二进制数组转换为 UUID 对象。
     *
     * @param bytes 长度为16的字节数组
     * @return 对应的 UUID 对象
     */
    private UUID bytesToUUID(byte[] bytes) {
        try {
            if (bytes == null || bytes.length != 16) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long high = buffer.getLong();
            long low = buffer.getLong();
            return new UUID(high, low);
        } catch (Exception e) {
            return null;
        }
    }

}


