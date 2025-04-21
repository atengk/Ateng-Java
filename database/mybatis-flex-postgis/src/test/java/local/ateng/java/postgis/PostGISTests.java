package local.ateng.java.postgis;

import cn.hutool.core.util.CoordinateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.query.QueryColumn;
import local.ateng.java.postgis.entity.PointEntities;
import local.ateng.java.postgis.service.PointEntitiesService;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.geometry.Point;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.List;

import static com.mybatisflex.core.query.QueryMethods.column;
import static local.ateng.java.postgis.entity.table.PointEntitiesTableDef.POINT_ENTITIES;

/**
 * PostGIS 数据处理
 *
 * @author Ateng
 * @email 2385569970@qq.com
 * @since 2025-04-18
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PostGISTests {
    private final PointEntitiesService pointEntitiesService;

    @Test
    public void test() {
        System.out.println(pointEntitiesService.list());
    }


    @Test
    void pointEntitiesSave() {
        PointEntities pois = PointEntities.builder()
                .name("重庆市")
                .category("重庆市")
                .geom(new Point(106.551787, 29.56268))
                .build();
        pointEntitiesService.save(pois);
    }

    @Test
    void pointEntitiesSaveStr() throws SQLException {
        PointEntities pois = PointEntities.builder()
                .name("重庆市")
                .category("重庆市")
                .geom(new Point("106.551787 29.56268"))
                .build();
        pointEntitiesService.save(pois);
    }

    @Test
    void transformCoordinates() {
        // Gcj02 坐标转换成 Wgs84 坐标系再写入数据库
        double lng = 106.560199;
        double lat = 29.471087;
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.gcj02ToWgs84(lng, lat);
        Point point = new Point(coordinate.getLng(), coordinate.getLat());
        PointEntities pois = PointEntities.builder()
                .name("重庆市")
                .category("重庆市")
                .geom(point)
                .build();
        pointEntitiesService.save(pois);
    }

    @Test
    void select() {
        QueryColumn geomColumn = column("ST_AsGeoJSON(geom) AS geojson");
        List<JSONObject> list = pointEntitiesService.queryChain()
                .select(POINT_ENTITIES.ID, POINT_ENTITIES.NAME, POINT_ENTITIES.CATEGORY, POINT_ENTITIES.CREATED_AT, geomColumn)
                .from(POINT_ENTITIES)
                .listAs(JSONObject.class);
        System.out.println(list);
    }

    @Test
    void select2() {
        QueryColumn geomColumn = column("jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson");
        String str = pointEntitiesService.queryChain()
                .select(geomColumn)
                .from(POINT_ENTITIES.as("t"))
                .objAs(String.class);
        System.out.println(str);
    }

    @Test
    void select3() {
        QueryColumn geomColumn = column("ST_AsGeoJSON(geom) AS geojson");
        List<JSONObject> list = pointEntitiesService.queryChain()
                .select(POINT_ENTITIES.ID, POINT_ENTITIES.NAME, POINT_ENTITIES.CATEGORY, POINT_ENTITIES.CREATED_AT, geomColumn)
                .from(POINT_ENTITIES)
                .where("ST_Within(geom,ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326));")
                .listAs(JSONObject.class);
        System.out.println(list);
    }

    @Test
    void select4() {
        QueryColumn geomColumn = column("geoc_wgs84togcj02(geom) AS geom");
        List<PointEntities> list = pointEntitiesService.queryChain()
                .select(POINT_ENTITIES.ID, POINT_ENTITIES.NAME, POINT_ENTITIES.CATEGORY, POINT_ENTITIES.CREATED_AT, geomColumn)
                .from(POINT_ENTITIES)
                .list();
        System.out.println(list);
    }

}
