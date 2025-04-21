package local.ateng.java.postgis.handler;

import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Geometry;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义TypeHandler映射Geometry空间几何数据
 *
 * @author 孔余
 * @since 2023-08-30 15:58:37
 */
@MappedTypes(Geometry.class)
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        PGgeometry geometry = new PGgeometry();
        geometry.setGeometry(parameter);
        ps.setObject(i, geometry);
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        PGgeometry pGgeometry = (PGgeometry) rs.getObject(columnName);
        if (pGgeometry == null) {
            return null;
        }
        return pGgeometry.getGeometry();
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        PGgeometry pGgeometry = (PGgeometry) rs.getObject(columnIndex);
        if (pGgeometry == null) {
            return null;
        }
        return pGgeometry.getGeometry();
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        PGgeometry pGgeometry = (PGgeometry) cs.getObject(columnIndex);
        if (pGgeometry == null) {
            return null;
        }
        return pGgeometry.getGeometry();
    }
}
