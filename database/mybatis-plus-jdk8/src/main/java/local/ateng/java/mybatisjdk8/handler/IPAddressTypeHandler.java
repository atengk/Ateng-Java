package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;

/**
 * MyBatis 类型处理器：将 MySQL 的 varbinary(16) 字段与 Java 中的 IP 字符串进行转换。
 * <p>
 * 在数据库中使用 INET6_ATON('127.0.01') | INET6_ATON('::1') 生成的 IPv4或者IPv6地址（二进制格式）
 * 支持 IPv4（4字节）和 IPv6（16字节）地址的互相映射。
 * 如果字段内容非法或解析异常，则返回 null。
 * </p>
 *
 * <pre>
 * 数据库字段类型：varbinary(16)
 * Java 字段类型：String（如 "192.168.1.1" 或 "::1"）
 * </pre>
 * <p>
 * 示例使用：
 * <pre>
 * &#64;TableField(typeHandler = IPAddressTypeHandler.class)
 * private String ipAddress;
 * </pre>
 *
 * @author 孔余
 * @since 2025-07-27
 */
public class IPAddressTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置非空 IP 字符串参数到 PreparedStatement，写入为对应字节数组。
     *
     * @param ps       PreparedStatement 对象
     * @param i        参数索引
     * @param ip       IP 地址字符串（IPv4 或 IPv6）
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String ip, JdbcType jdbcType) throws SQLException {
        try {
            byte[] addressBytes = InetAddress.getByName(ip).getAddress();
            ps.setBytes(i, addressBytes);
        } catch (UnknownHostException e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 通过列名获取 IP 字符串（从结果集）
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toIpString(rs.getBytes(columnName));
    }

    /**
     * 通过列索引获取 IP 字符串（从结果集）
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toIpString(rs.getBytes(columnIndex));
    }

    /**
     * 从存储过程中通过列索引获取 IP 字符串
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toIpString(cs.getBytes(columnIndex));
    }

    /**
     * 将 IP 地址字节数组转换为字符串（IPv4 或 IPv6），非法时返回 null。
     *
     * @param bytes IP 字节数组（应为 4 或 16 字节）
     * @return 字符串形式的 IP 地址，或 null
     */
    private String toIpString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // 自动兼容 IPv4（4字节）和 IPv6（16字节）
            InetAddress byAddress = InetAddress.getByAddress(bytes);
            return byAddress.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

}

