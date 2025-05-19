package local.ateng.java.postgis;

import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Geometry;
import net.postgis.jdbc.geometry.Point;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

public class SimpleTests {

    @Test
    public void test() throws SQLException {
        Point point = new Point("106.551787 29.56268");
        System.out.println(point);
    }

    @Test
    public void test2() throws SQLException {
        PGgeometry pgeometry = new PGgeometry("POINT(106.551787 29.56268)");
        Geometry geometry = pgeometry.getGeometry();
        System.out.println(geometry);
    }

    @Test
    public void test3() throws SQLException {
        PGgeometry pgeometry = new PGgeometry("POINT Z (106.551787 29.56268 1)");
        Geometry geometry = pgeometry.getGeometry();
        pgeometry.getGeoType()
        System.out.println(geometry);
    }

}
