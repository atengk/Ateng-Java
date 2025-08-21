package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.Arrays;

/**
 * MyBatis Plus 类型处理器：用于映射 MySQL 中的 Geometry 字段与 JTS 的 Geometry 对象
 *
 * <p>注意：</p>
 * <ul>
 *   <li>写入时使用 WKB（带 SRID）格式</li>
 *   <li>读取时自动跳过前4字节的 SRID 并返回 Geometry 对象</li>
 *   <li>如解析失败，返回 null，不抛出异常</li>
 * </ul>
 *
 * <p>建议 MySQL 字段类型为 <code>geometry SRID 4326</code></p>
 *
 * @author 孔余
 * @since 2025-07-27
 */
@MappedJdbcTypes({JdbcType.OTHER}) // 数据库字段类型
@MappedTypes(Geometry.class)           // Java 类型
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

    /**
     * 设置非空参数到 PreparedStatement 中，使用带 SRID 的 WKB 格式。
     *
     * @param ps        预编译 SQL 语句
     * @param i         参数索引（从1开始）
     * @param parameter Geometry 参数
     * @param jdbcType  JDBC 类型（可为空）
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.BINARY);
            return;
        }

        try {
            // 获取 SRID，默认 4326
            int srid = parameter.getSRID() > 0 ? parameter.getSRID() : 4326;

            // 使用 WKBWriter 生成 2D 小端 WKB，禁用 EWKB 扩展（Z/M/SRID）
            WKBWriter wkbWriter = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false);
            byte[] wkb = wkbWriter.write(parameter);

            // 拼接 SRID（4 字节小端序）和 WKB
            ByteBuffer buffer = ByteBuffer.allocate(4 + wkb.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(srid);
            buffer.put(wkb);

            // 设置参数值为 MySQL 支持的 EWKB 格式二进制
            ps.setBytes(i, buffer.array());
        } catch (Exception e) {
            // 保证接口契约，设置为 SQL NULL，避免报错
            ps.setNull(i, Types.BINARY);
        }
    }

    /**
     * 从 ResultSet 中获取 Geometry 对象（按列名）
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseGeometry(rs.getBytes(columnName));
    }

    /**
     * 从 ResultSet 中获取 Geometry 对象（按列索引）
     *
     * @param rs          结果集
     * @param columnIndex 列索引（从1开始）
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseGeometry(rs.getBytes(columnIndex));
    }

    /**
     * 从 CallableStatement 中获取 Geometry 对象
     *
     * @param cs          存储过程调用
     * @param columnIndex 列索引（从1开始）
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseGeometry(cs.getBytes(columnIndex));
    }

    /**
     * 解析 MySQL 返回的 GEOMETRY 字节流（包含 SRID 前缀）
     *
     * @param bytes GEOMETRY 字节流
     * @return Geometry 对象或 null（如果失败）
     */
    private Geometry parseGeometry(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            return null;
        }

        try {
            // 提取 SRID（前4字节）
            ByteBuffer sridBuffer = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
            int srid = sridBuffer.getInt();

            // 提取 WKB 并解析
            byte[] wkb = Arrays.copyOfRange(bytes, 4, bytes.length);
            WKBReader reader = new WKBReader();
            Geometry geometry = reader.read(wkb);
            geometry.setSRID(srid);

            return geometry;
        } catch (Exception e) {
            return null; // 解析失败返回 null
        }
    }
}
