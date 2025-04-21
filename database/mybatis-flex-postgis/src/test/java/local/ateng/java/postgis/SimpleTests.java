package local.ateng.java.postgis;

import net.postgis.jdbc.geometry.Point;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

public class SimpleTests {

    @Test
    public void test() throws SQLException {
        Point point = new Point("106.551787 29.56268");
        System.out.println(point);
    }

}
