package local.ateng.java.postgis;

import cn.hutool.core.util.CoordinateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.row.Db;
import local.ateng.java.postgis.entity.PointEntities;
import local.ateng.java.postgis.service.PointEntitiesService;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.geometry.Point;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
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

    @Test
    void select5() {
        String sql = "WITH\n" +
                "-- 模拟当前视图的边界和缩放级别\n" +
                "params AS (\n" +
                "  SELECT\n" +
                "    ST_MakeEnvelope(?,?,?,?,?) AS bbox,\n" +
                "    ? AS zoom_level  -- 改这个值模拟不同缩放级别\n" +
                "),\n" +
                "\n" +
                "-- 根据 zoom_level 决定网格大小，若 zoom >= 16 则为 NULL 表示不聚合\n" +
                "grid_size AS (\n" +
                "  SELECT\n" +
                "    zoom_level,\n" +
                "    CASE\n" +
                "      WHEN zoom_level < 5 THEN 0.5        -- 世界级\n" +
                "      WHEN zoom_level < 7 THEN 0.2        -- 国家级\n" +
                "      WHEN zoom_level < 9 THEN 0.1        -- 区域级\n" +
                "      WHEN zoom_level < 11 THEN 0.05      -- 城市级\n" +
                "      WHEN zoom_level < 13 THEN 0.02      -- 区县级\n" +
                "      WHEN zoom_level < 14 THEN 0.01      -- 街道级\n" +
                "      WHEN zoom_level < 15 THEN 0.005     -- 社区级\n" +
                "      WHEN zoom_level < 16 THEN 0.002     -- 小区级\n" +
                "      ELSE NULL                           -- >=16 显示原始点\n" +
                "    END AS cell_size,\n" +
                "    bbox\n" +
                "  FROM params\n" +
                "),\n" +
                "\n" +
                "-- 构造网格（当 cell_size 非空时才构建）\n" +
                "grid AS (\n" +
                "  SELECT (ST_SquareGrid(gs.cell_size, gs.bbox)).*\n" +
                "  FROM grid_size gs\n" +
                "  WHERE gs.cell_size IS NOT NULL\n" +
                "),\n" +
                "\n" +
                "-- 聚合数据（仅当 zoom_level < 16）\n" +
                "aggregated AS (\n" +
                "  SELECT\n" +
                "    ST_Centroid(g.geom) AS center_point,\n" +
                "    COUNT(p.*) AS point_count,\n" +
                "    JSON_AGG(\n" +
                "        JSON_BUILD_OBJECT(\n" +
                "            'id', p.id,\n" +
                "            'name', p.name,\n" +
                "            'category', p.category\n" +
                "        )\n" +
                "    ) AS data_items\n" +
                "  FROM grid g\n" +
                "  JOIN point_entities p\n" +
                "    ON ST_Intersects(g.geom, p.geom)\n" +
                "  GROUP BY g.geom\n" +
                "),\n" +
                "\n" +
                "-- 原始点数据（仅当 zoom_level >= 16）\n" +
                "raw_points AS (\n" +
                "  SELECT\n" +
                "    p.geom AS center_point,\n" +
                "    1 AS point_count,\n" +
                "    JSON_BUILD_ARRAY(\n" +
                "        JSON_BUILD_OBJECT(\n" +
                "            'id', p.id,\n" +
                "            'name', p.name,\n" +
                "            'category', p.category\n" +
                "        )\n" +
                "    ) AS data_items\n" +
                "  FROM point_entities p, params pa\n" +
                "  WHERE ST_Intersects(p.geom, pa.bbox)\n" +
                ")\n" +
                ", result AS (\n" +
                "-- 最终输出\n" +
                "SELECT * FROM aggregated WHERE (SELECT zoom_level FROM params) < 16\n" +
                "UNION ALL\n" +
                "SELECT * FROM raw_points WHERE (SELECT zoom_level FROM params) >= 16\n" +
                ")\n" +
                "SELECT\n" +
                "    JSON_BUILD_OBJECT(\n" +
                "        'type', 'FeatureCollection',\n" +
                "        'features', jsonb_agg(ST_AsGeoJSON(r)::jsonb)\n" +
                "    ) AS feature\n" +
                "FROM result as r;";
        PGobject pgObject = (PGobject) Db.selectObject(sql, 106.50, 29.50, 106.60, 29.60, 4326, 13);
        System.out.println(pgObject.getType());
        System.out.println(pgObject.getValue());
    }

}
