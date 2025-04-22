# PostGIS SQL



## 几何类型介绍

### 概览

| 类型                 | 中文名称   | 说明                           |
| -------------------- | ---------- | ------------------------------ |
| `POINT`              | 点         | 一个位置                       |
| `LINESTRING`         | 线         | 一系列连接的点                 |
| `POLYGON`            | 多边形     | 闭合区域，边界为直线段         |
| `MULTIPOINT`         | 多点       | 点的集合                       |
| `MULTILINESTRING`    | 多线       | 多个线段集合                   |
| `MULTIPOLYGON`       | 多多边形   | 多个多边形组成的集合           |
| `GEOMETRYCOLLECTION` | 几何集合   | 混合的几何类型集合             |
| `CIRCULARSTRING`     | 圆弧线     | 圆弧曲线                       |
| `COMPOUNDCURVE`      | 复合曲线   | 圆弧与直线的组合               |
| `CURVEPOLYGON`       | 曲线多边形 | 带曲线边的多边形               |
| `MULTICURVE`         | 多曲线     | 多个曲线段的集合               |
| `MULTISURFACE`       | 多曲面     | 多个曲面区域集合（支持曲线边） |

### 基础几何类型

🧱 1. **点（Point）**

- 表示一个地理空间中的单一位置。
- 常用于表示例如：城市、建筑物、水井等的坐标位置。
- 示例：`POINT(30 10)`

------

📏 2. **线（LineString）**

- 表示一个由两个或多个点连接起来的线。
- 常用于表示道路、河流、电缆等。
- 示例：`LINESTRING(30 10, 10 30, 40 40)`

------

🔷 3. **多边形（Polygon）**

- 表示一个闭合区域，边界由一系列线段组成，首尾相连。
- 可用于表示湖泊、建筑区域、行政边界等。
- 示例：`POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))`

------

🧩 4. **多点（MultiPoint）**

- 表示多个点组成的集合。
- 示例：`MULTIPOINT((10 40), (40 30), (20 20), (30 10))`

------

🪡 5. **多线（MultiLineString）**

- 表示多个 LineString 的集合。
- 示例：`MULTILINESTRING((10 10, 20 20), (15 15, 30 15))`

------

🧱 6. **多多边形（MultiPolygon）**

- 表示多个 Polygon 的集合（可以是离散的或相邻的区域）。
- 示例：`MULTIPOLYGON(((30 20, 45 40, 10 40, 30 20)), ((15 5, 40 10, 10 20, 5 10, 15 5)))`

------

🧱 7. **几何集合（GeometryCollection）**

- 一个可以包含不同类型几何对象的集合（例如点、线、面混合）。
- 示例：`GEOMETRYCOLLECTION(POINT(10 10), LINESTRING(20 20, 30 30))`

------

### 高级几何类型（Curved Geometry Types）

1. **CIRCULARSTRING（圆弧线）**

- 描述圆弧或弧形路径的曲线线段。
- 每三个点定义一个弧：第一个点是起点，第二个是弧上的点（不在直线上），第三个是终点。
- 用途：铁路、环形道路、流线型设计等。

🧾 示例：

```sql
ST_GeomFromText('CIRCULARSTRING(0 0, 1 1, 2 0)')
```

------

2. **COMPOUNDCURVE（复合曲线）**

- 是 `CIRCULARSTRING` 和 `LINESTRING` 的组合，表示一条由直线段和圆弧段组成的复合线。
- 用于构建复杂的连续路径。

🧾 示例：

```sql
ST_GeomFromText('COMPOUNDCURVE((0 0, 1 1), CIRCULARSTRING(1 1, 2 2, 3 1))')
```

------

3. **CURVEPOLYGON（曲线多边形）**

- 用 `CIRCULARSTRING` 和/或 `COMPOUNDCURVE` 定义边界的多边形。
- 可以有内外环（外环包围区域，内环表示洞）。

🧾 示例：

```sql
ST_GeomFromText('CURVEPOLYGON(CIRCULARSTRING(0 0, 1 1, 2 0, 0 0))')
```

------

4. **MULTICURVE（多曲线）**

- 一个由 `LineString`、`CircularString` 或 `CompoundCurve` 组成的集合。
- 类似 `MultiLineString`，但支持曲线。

🧾 示例：

```sql
ST_GeomFromText('MULTICURVE((0 0, 1 1), CIRCULARSTRING(1 1, 2 2, 3 1))')
```

------

5. **MULTISURFACE（多曲面）**

- 一个由 `Polygon` 或 `CurvePolygon` 组成的集合。
- 类似 `MultiPolygon`，但支持曲线边界。

🧾 示例：

```sql
ST_GeomFromText('MULTISURFACE(CURVEPOLYGON(CIRCULARSTRING(0 0, 1 1, 2 0, 0 0)))')
```



## 安装扩展

**安装 PostGIS 扩展**

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```



## POINT点表

### 数据准备

**创建表**

```sql
CREATE TABLE point_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(POINT, 4326)
);
CREATE INDEX idx_point_geom ON point_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO point_entities (name, category, geom) VALUES
('解放碑步行街', '商业区', 'POINT(106.5784 29.5628)'),
('重庆北站', '交通枢纽', 'POINT(106.5516 29.6067)'),
('重庆市人民医院', '医疗机构', 'POINT(106.5231 29.5409)'),
('洪崖洞', '景点', 'POINT(106.5764 29.5622)'),
('重庆大学 A 区', '高校', 'POINT(106.4597 29.5647)');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM point_entities;
```

### 使用示例

#### 空间范围查询

**空间范围查询（查找某区域内的点）**

查找落在一个多边形（如区域边界）内的所有点。

```sql
-- 查询某多边形区域内的点
SELECT * FROM point_entities
WHERE ST_Within(geom, ST_GeomFromText(
  'POLYGON((106.56 29.55, 106.58 29.55, 106.58 29.57, 106.56 29.57, 106.56 29.55))', 4326
));
```

**查询矩形范围内的点（bounding box）**

后端使用 `ST_MakeEnvelope` 和 `ST_Within` 查询

```sql
SELECT *
FROM points
WHERE ST_Within(geom, ST_MakeEnvelope(minLng, minLat, maxLng, maxLat, 4326));
```

示例SQL

```sql
SELECT *
FROM point_entities
WHERE ST_Within(
    geom,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
);
```

#### 点与点之间的距离查询

**点与点之间的距离查询**

查找距离指定坐标（如某地标）最近的点，或者在某距离范围内的点。

```sql
-- 距离“解放碑”500 米以内的点
SELECT * FROM point_entities
WHERE ST_DWithin(
  geom::geography,
  ST_SetSRID(ST_Point(106.5784, 29.5628), 4326)::geography,
  500
);
-- 查询离“解放碑”最近的一个点
SELECT * FROM point_entities
ORDER BY geom <-> ST_SetSRID(ST_Point(106.5784, 29.5628), 4326)
LIMIT 1;
```

#### 最近邻查询

**最近邻查询（K 最近点）**

利用 GiST 索引高效查询 K 近邻：

```sql
-- 查询离“南山一棵树”最近的 3 个点
SELECT * FROM point_entities
ORDER BY geom <-> ST_SetSRID(ST_Point(106.6125, 29.5051), 4326)
LIMIT 3;
```

#### 空间连接

**空间连接（与其他图层如多边形、线进行 JOIN）**

将点与区域（如行政区、多边形图层）进行空间匹配。

```sql
-- 假设另有表 polygon_entities，找出点属于哪个区域
SELECT p.id, p.name, r.id, r.name AS region_name
FROM point_entities p
LEFT JOIN polygon_entities r
  ON ST_Within(p.geom, r.geom);
```

#### GeoJSON

**提取每条记录的基本属性及其几何字段的 GeoJSON 表示**

```sql
SELECT id, name, category,
       ST_AsGeoJSON(geom) AS geojson
FROM point_entities;
```

**将整条记录（包含所有属性和几何）序列化为 GeoJSON（Feature 格式）**

```sql
SELECT ST_AsGeoJSON(t) AS geojson
FROM point_entities as t;
```

**将所有记录序列化为 GeoJSON（Feature 格式），并聚合为数组**

```sql
SELECT jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson
FROM point_entities as t;
```

**构建标准的 GeoJSON FeatureCollection 对象，包含所有要素**

```sql
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(ST_AsGeoJSON(t)::jsonb)
) AS geojson_collection
FROM point_entities t;
```

#### 数据简化

**基础示例**

简化几何图形的实际使用示例，它可以在前端地图缩放级别较低时，减少图形点数，提升渲染性能。

```sql
SELECT
    id,
    name,
    category,
    ST_SimplifyPreserveTopology(geom, 0.001) AS simplified_geom
FROM point_entities
WHERE ST_Within(
    geom,
    ST_MakeEnvelope(106.0, 29.0, 107.0, 30.0, 4326)
);
```

**动态根据缩放级别简化**

```sql
WITH zoom AS (
    SELECT 11 AS zoom_level  -- 前端传入的缩放级别
)
SELECT
    id,
    name,
    category,
    CASE
        WHEN z.zoom_level < 5 THEN ST_SimplifyPreserveTopology(p.geom, 0.1)
        WHEN z.zoom_level < 7 THEN ST_SimplifyPreserveTopology(p.geom, 0.05)
        WHEN z.zoom_level < 9 THEN ST_SimplifyPreserveTopology(p.geom, 0.01)
        WHEN z.zoom_level < 11 THEN ST_SimplifyPreserveTopology(p.geom, 0.005)
        WHEN z.zoom_level < 13 THEN ST_SimplifyPreserveTopology(p.geom, 0.001)
        WHEN z.zoom_level < 15 THEN ST_SimplifyPreserveTopology(p.geom, 0.0005)
        WHEN z.zoom_level < 17 THEN ST_SimplifyPreserveTopology(p.geom, 0.0001)
        ELSE p.geom
    END AS display_geom
FROM point_entities p
CROSS JOIN zoom z
WHERE ST_Within(
    p.geom,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
);
```



#### 网格数据聚合

**按固定网格聚合**

按0.1度×0.1度的网格聚合点数据

```sql
SELECT
    ST_SnapToGrid(geom, 0.1, 0.1) AS grid_center,
    COUNT(*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', id,
            'name', name,
            'category', category
        )
    ) AS data_items
FROM
    point_entities
WHERE
    ST_Within(geom, ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326))
GROUP BY
    ST_SnapToGrid(geom, 0.1, 0.1)
ORDER BY
    point_count DESC;
```

返回GeoJSON数据

```sql
WITH aggregated AS (
    SELECT
        ST_SnapToGrid(geom, 0.1, 0.1),
        COUNT(*) AS point_count,
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', id,
                'name', name,
                'category', category
            )
        ) AS data_items
    FROM
        point_entities
    WHERE
        ST_Within(geom, ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326))
    GROUP BY
        ST_SnapToGrid(geom, 0.1, 0.1)
    ORDER BY
        point_count DESC
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(agg)::jsonb)
    ) AS feature
FROM aggregated as agg;
```

返回以下数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.6, 29.6]}, "properties": {"data_items": [{"id": 1, "name": "解放碑步行街", "category": "商业区"}, {"id": 4, "name": "洪崖洞", "category": "景点"}, {"id": 6, "name": "重庆市", "category": "重庆市"}, {"id": 7, "name": "重庆市", "category": "重庆市"}], "point_count": 4}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.5, 29.5]}, "properties": {"data_items": [{"id": 3, "name": "重庆市人民医院", "category": "医疗机构"}], "point_count": 1}}]}
```



**按缩放级别动态聚合**

```sql
WITH zoom_params AS (
    SELECT 11 AS zoom_level  -- 动态传入
),
base AS (
    SELECT
        CASE
            WHEN zp.zoom_level < 5 THEN ST_SnapToGrid(p.geom, 2.0, 2.0)
            WHEN zp.zoom_level < 7 THEN ST_SnapToGrid(p.geom, 1.0, 1.0)
            WHEN zp.zoom_level < 9 THEN ST_SnapToGrid(p.geom, 0.5, 0.5)
            WHEN zp.zoom_level < 11 THEN ST_SnapToGrid(p.geom, 0.2, 0.2)
            WHEN zp.zoom_level = 11 THEN ST_SnapToGrid(p.geom, 0.1, 0.1)
            ELSE p.geom
        END AS display_geom,
        p.id,
        p.name,
        p.category,
        zp.zoom_level
    FROM point_entities p
    CROSS JOIN zoom_params zp
    WHERE ST_Within(
        p.geom,
        ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
    )
),
aggregated AS (
    SELECT
        display_geom,
        COUNT(*) AS point_count,
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', id,
                'name', name,
                'category', category
            )
        ) AS data_items,
        MAX(zoom_level) AS zoom_level
    FROM base
    GROUP BY display_geom
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(agg)::jsonb)
    ) AS feature
FROM aggregated as agg;
```

关键sql说明

- `WITH zoom_params AS (...)`：定义缩放级别参数，这个可以由前端传入。
     用于控制不同的缩放级别返回不同精度的几何信息。
- `ST_SnapToGrid(p.geom, ...)`:
    - 对点进行“网格化”处理，作用是将多个落在同一网格的点聚合为一个；
    - 网格大小根据 `zoom_level` 动态控制（值越小网格越密集）；
    - 这样就实现了缩放越小（视野越大）聚合越粗的效果。
- `display_geom`:
    - 表示聚合后的“代表几何位置”（用于地图展示）；
    - 低缩放级别为聚合中心，高缩放级别为原始点。
- `CROSS JOIN zoom_params zp`: 将缩放级别参数与每条数据绑定，使查询可以使用 zoom_level 来动态调整逻辑
- `ST_Within(..., ST_MakeEnvelope(...))`:
    - 空间过滤，只查询在指定矩形边界内的点；
    - 提高查询效率，减少不必要的几何数据。
- `JSON_AGG(...)`:
    - 将聚合后的点的信息（id、name、category）以数组形式合并；
    - 为了低缩放级别下展示聚合点时仍能展示明细数据（如 hover 弹出列表）。
- `MAX(zoom_level)`:
    - 将 zoom_level 提升到 `aggregated` 层，便于后续判断分支逻辑。
- `JSON_BUILD_OBJECT(...)` in `SELECT`:
    - 最终输出符合 GeoJSON 格式的 `FeatureCollection`；
    - `ST_AsGeoJSON(...)` 将聚合后的 `display_geom` 转换成 GeoJSON 格式；
    - `jsonb_agg(...)` 把所有 feature 聚合成 `features` 字段内容。

返回数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.6, 29.6]}, "properties": {"data_items": [{"id": 1, "name": "解放碑步行街", "category": "商业区"}, {"id": 4, "name": "洪崖洞", "category": "景点"}, {"id": 6, "name": "重庆市", "category": "重庆市"}, {"id": 7, "name": "重庆市", "category": "重庆市"}], "zoom_level": 11, "point_count": 4}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.5, 29.5]}, "properties": {"data_items": [{"id": 3, "name": "重庆市人民医院", "category": "医疗机构"}], "zoom_level": 11, "point_count": 1}}]}
```



#### 网格数据聚合2

**按固定网格聚合**

该 SQL 在指定的地理范围内生成固定大小的网格，并对每个网格单元内的点数据进行空间聚合，输出每个格子的中心点坐标、包含的点数量以及这些点的属性信息列表，便于前端根据地图缩放级别展示聚合后的结果。

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', id,
            'name', name,
            'category', category
        )
    ) AS data_items
  FROM grid g
  JOIN point_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
)
SELECT * FROM aggregated;
```

关键部分说明：

- `ST_MakeEnvelope(...)`: 创建聚合的地理范围（矩形边界框bounding box）。
- `ST_SquareGrid(0.01, ...)`: 生成边长为 0.01 的正方形网格。
- `grid AS (...)`: 定义网格临时表，每行表示一个格子。
- `ST_Intersects(g.geom, p.geom)`: 判断点是否落入网格格子中。
- `ST_Centroid(g.geom)`: 计算每个网格格子的中心点坐标。
- `COUNT(p.*)`: 统计每个格子中包含的点数量。
- `JSON_AGG(JSON_BUILD_OBJECT(...))`: 聚合点的属性为 JSON 数组。
- `GROUP BY g.geom`: 以格子为单位分组聚合。
- `SELECT * FROM aggregated`: 输出聚合结果（中心点、数量、明细）。

返回GeoJSON数据

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', id,
            'name', name,
            'category', category
        )
    ) AS data_items
  FROM grid g
  JOIN point_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(agg)::jsonb)
    ) AS feature
FROM aggregated as agg;
```

返回以下数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 2, "name": "重庆北站", "category": "交通枢纽"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "重庆市", "category": "重庆市"}, {"id": 7, "name": "重庆市", "category": "重庆市"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "解放碑步行街", "category": "商业区"}, {"id": 4, "name": "洪崖洞", "category": "景点"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.545]}, "properties": {"data_items": [{"id": 3, "name": "重庆市人民医院", "category": "医疗机构"}], "point_count": 1}}]}
```



**按缩放级别动态聚合**

根据前端传入的缩放级别与地图边界，动态判断是否对点数据进行网格聚合。低缩放级别时以网格聚合形式返回中心点和属性，高缩放级别（zoom ≥ 16）则返回原始点数据，并统一以 GeoJSON FeatureCollection 输出，适配地图可视化需求。

```sql
WITH
-- 模拟当前视图的边界和缩放级别
params AS (
  SELECT
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326) AS bbox,
    13 AS zoom_level  -- 改这个值模拟不同缩放级别
),

-- 根据 zoom_level 决定网格大小，若 zoom >= 16 则为 NULL 表示不聚合
grid_size AS (
  SELECT
    zoom_level,
    CASE
      WHEN zoom_level < 5 THEN 0.5        -- 世界级
      WHEN zoom_level < 7 THEN 0.2        -- 国家级
      WHEN zoom_level < 9 THEN 0.1        -- 区域级
      WHEN zoom_level < 11 THEN 0.05      -- 城市级
      WHEN zoom_level < 13 THEN 0.02      -- 区县级
      WHEN zoom_level < 14 THEN 0.01      -- 街道级
      WHEN zoom_level < 15 THEN 0.005     -- 社区级
      WHEN zoom_level < 16 THEN 0.002     -- 小区级
      ELSE NULL                           -- >=16 显示原始点
    END AS cell_size,
    bbox
  FROM params
),

-- 构造网格（当 cell_size 非空时才构建）
grid AS (
  SELECT (ST_SquareGrid(gs.cell_size, gs.bbox)).*
  FROM grid_size gs
  WHERE gs.cell_size IS NOT NULL
),

-- 聚合数据（仅当 zoom_level < 16）
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM grid g
  JOIN point_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
),

-- 原始点数据（仅当 zoom_level >= 16）
raw_points AS (
  SELECT
    p.geom AS center_point,
    1 AS point_count,
    JSON_BUILD_ARRAY(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM point_entities p, params pa
  WHERE ST_Intersects(p.geom, pa.bbox)
)
, result AS (
-- 最终输出
SELECT * FROM aggregated WHERE (SELECT zoom_level FROM params) < 16
UNION ALL
SELECT * FROM raw_points WHERE (SELECT zoom_level FROM params) >= 16
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(r)::jsonb)
    ) AS feature
FROM result as r;
```

关键部分说明：

- `params`: 定义地图的可视边界 (`bbox`) 和当前缩放等级 (`zoom_level`)，作为后续逻辑判断的基础。
- `grid_size`: 根据 `zoom_level` 计算网格的边长（`cell_size`），zoom 越小格子越大；当 zoom ≥ 16 时，返回 `NULL` 表示不做聚合。
- `grid`: 在 `cell_size` 非空时，构造指定区域内的正方形网格，作为聚合区域的基础。
- `aggregated`: 针对 zoom < 16 情况，将点数据按网格聚合，返回每个格子中心点、点数量及聚合后的属性数据（以 JSON 数组形式表示）。
- `raw_points`: 针对 zoom ≥ 16 的情况，返回边界范围内的原始点数据，构造为类似聚合结构（每个点当作单独一格），便于前端统一处理。
- `result`: 根据当前 zoom 级别选择性地返回聚合数据或原始点数据（两者通过 `UNION ALL` 合并）。
- `ST_AsGeoJSON(...)`:
     将每条记录转换为 GeoJSON Feature，统一输出为 `FeatureCollection`，方便前端直接加载渲染。

返回数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 2, "name": "重庆北站", "category": "交通枢纽"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "重庆市", "category": "重庆市"}, {"id": 7, "name": "重庆市", "category": "重庆市"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "解放碑步行街", "category": "商业区"}, {"id": 4, "name": "洪崖洞", "category": "景点"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.545]}, "properties": {"data_items": [{"id": 3, "name": "重庆市人民医院", "category": "医疗机构"}], "point_count": 1}}]}
```



## LINESTRING线表

### 数据准备

**创建表**

```sql
CREATE TABLE linestring_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(LINESTRING, 4326)
);
CREATE INDEX idx_linestring_geom ON linestring_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO linestring_entities (name, category, geom) VALUES
('长江滨江路段', '城市道路', 'LINESTRING(106.5587 29.5676, 106.5708 29.5639, 106.5812 29.5615)'),
('轨道交通2号线（解放碑段）', '轨道交通', 'LINESTRING(106.5784 29.5628, 106.5746 29.5582, 106.5701 29.5543)'),
('嘉陵江滨江绿道', '步道', 'LINESTRING(106.5432 29.5891, 106.5486 29.5853, 106.5537 29.5807)'),
('红旗河沟至重庆北站道路', '主干道', 'LINESTRING(106.5327 29.5890, 106.5418 29.5952, 106.5516 29.6067)'),
('南滨路夜景段', '景观道路', 'LINESTRING(106.5861 29.5549, 106.5894 29.5521, 106.5926 29.5498)');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM linestring_entities;
```

### 使用示例

#### 空间分析类查询

查询某点附近的所有线段（比如查找附近道路）

```sql
SELECT *
FROM linestring_entities
WHERE ST_DWithin(
    geom,
    ST_SetSRID(ST_Point(106.5505, 29.5630), 4326),
    0.01 -- 单位为度，约等于 1km
);
```

查询与某条线相交的其他线

```sql
WITH target AS (
  SELECT geom FROM linestring_entities WHERE id = 1
)
SELECT l.*
FROM linestring_entities l, target
WHERE ST_Intersects(l.geom, target.geom)
  AND l.id != 1;
```

查询某线段是否被某区域(完全在区域内部)

```sql
SELECT l.id, l.name AS line_name, p.name AS region_name
FROM linestring_entities l
JOIN polygon_entities p
  ON ST_Within(l.geom, p.geom);

SELECT l.id, l.name AS line_name, p.name AS region_name
FROM linestring_entities l
JOIN polygon_entities p
  ON ST_Contains(p.geom, l.geom);
```

查询某线段是否被某区域(部分在区域内部)

```sql
SELECT l.*, p.name AS region
FROM linestring_entities l
JOIN polygon_entities p
  ON ST_Intersects(l.geom, p.geom);
```

#### 轨迹处理类

计算线段的长度（单位：度）

```sql
SELECT id, ST_Length(geom) AS length_degrees FROM linestring_entities;
```

计算线段的长度（单位：米）

```sql
SELECT id, ST_Length(ST_Transform(geom, 3857)) AS length_meters FROM linestring_entities;
```

计算线段的起点与终点

```sql
SELECT
  id,
  ST_AsText(ST_StartPoint(geom)) AS start_point,
  ST_AsText(ST_EndPoint(geom)) AS end_point
FROM linestring_entities;
```

#### GeoJSON

**提取每条记录的基本属性及其几何字段的 GeoJSON 表示**

```sql
SELECT id, name, category,
       ST_AsGeoJSON(geom) AS geojson
FROM linestring_entities;
```

**将整条记录（包含所有属性和几何）序列化为 GeoJSON（Feature 格式）**

```sql
SELECT ST_AsGeoJSON(t) AS geojson
FROM linestring_entities as t;
```

**将所有记录序列化为 GeoJSON（Feature 格式），并聚合为数组**

```sql
SELECT jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson
FROM linestring_entities as t;
```

**构建标准的 GeoJSON FeatureCollection 对象，包含所有要素**

```sql
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(ST_AsGeoJSON(t)::jsonb)
) AS geojson_collection
FROM linestring_entities t;
```

#### 数据简化

**基础示例**

简化几何图形的实际使用示例，它可以在前端地图缩放级别较低时，减少图形点数，提升渲染性能。

```sql
SELECT
    id,
    name,
    category,
    ST_SimplifyPreserveTopology(geom, 0.001) AS simplified_geom
FROM linestring_entities
WHERE ST_Within(
    geom,
    ST_MakeEnvelope(106.0, 29.0, 107.0, 30.0, 4326)
);
```

**动态根据缩放级别简化**

```sql
WITH zoom AS (
    SELECT 11 AS zoom_level  -- 前端传入的缩放级别
)
SELECT
    id,
    name,
    category,
    CASE
        WHEN z.zoom_level < 5 THEN ST_SimplifyPreserveTopology(p.geom, 0.1)
        WHEN z.zoom_level < 7 THEN ST_SimplifyPreserveTopology(p.geom, 0.05)
        WHEN z.zoom_level < 9 THEN ST_SimplifyPreserveTopology(p.geom, 0.01)
        WHEN z.zoom_level < 11 THEN ST_SimplifyPreserveTopology(p.geom, 0.005)
        WHEN z.zoom_level < 13 THEN ST_SimplifyPreserveTopology(p.geom, 0.001)
        WHEN z.zoom_level < 15 THEN ST_SimplifyPreserveTopology(p.geom, 0.0005)
        WHEN z.zoom_level < 17 THEN ST_SimplifyPreserveTopology(p.geom, 0.0001)
        ELSE p.geom
    END AS display_geom
FROM linestring_entities p
CROSS JOIN zoom z
WHERE ST_Within(
    p.geom,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
);
```

#### 网格数据聚合2

**按固定网格聚合**

将线数据（`linestring_entities`）按照固定大小网格进行空间聚合，统计每个网格中相交的线数量、线段在该格子内的总长度（单位：米）以及相关属性，并以网格中心点作为代表位置输出，适用于中低缩放级别下的地图线聚合展示。

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(l.*) AS line_count,
    SUM(ST_Length(ST_Intersection(g.geom, l.geom)::geography)) AS total_length_meters,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', l.id,
            'name', l.name,
            'category', l.category
        )
    ) AS data_items
  FROM grid g
  JOIN linestring_entities l
    ON ST_Intersects(g.geom, l.geom)
  GROUP BY g.geom
)
SELECT * FROM aggregated;
```

关键部分说明：

- `grid`: 使用 `ST_SquareGrid` 构造一个网格，覆盖指定的地理范围（此处为 `(106.50, 29.50, 106.60, 29.60)`），每个网格单元为 0.01 度大小的正方形。
- `ST_MakeEnvelope(...)`: 定义查询所覆盖的地理边界（bounding box），即当前地图视图区域。
- `ST_Intersects(g.geom, l.geom)`: 判断线是否与当前网格单元相交，作为聚合判断条件。
- `ST_Centroid(g.geom) AS center_point`: 计算每个网格的中心点，用于前端可视化定位。
- `COUNT(l.*) AS line_count`: 统计当前网格内交叉的线数量。
- `ST_Intersection(g.geom, l.geom)`: 提取线与网格单元相交的部分几何形状。
- `ST_Length(...::geography)`: 计算相交线段的实际长度（以米为单位），`::geography` 用于准确的地理距离测量。
- `SUM(...) AS total_length_meters`: 聚合当前格子中所有线段的总长度。
- `JSON_AGG(...) AS data_items`: 将当前格子内所有线条的属性（id、name、category）聚合成一个 JSON 数组，便于前端展示属性详情。

返回GeoJSON数据

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(l.*) AS line_count,
    SUM(ST_Length(ST_Intersection(g.geom, l.geom)::geography)) AS total_length_meters,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', l.id,
            'name', l.name,
            'category', l.category
        )
    ) AS data_items
  FROM grid g
  JOIN linestring_entities l
    ON ST_Intersects(g.geom, l.geom)
  GROUP BY g.geom
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(agg)::jsonb)
    ) AS feature
FROM aggregated as agg;
```

返回以下数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.595]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "line_count": 1, "total_length_meters": 716.4163453553116}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.595]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "line_count": 1, "total_length_meters": 884.5061051889184}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.605]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "line_count": 1, "total_length_meters": 666.4949035224914}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "line_count": 1, "total_length_meters": 259.4872114699714}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.585]}, "properties": {"data_items": [{"id": 3, "name": "嘉陵江滨江绿道", "category": "步道"}], "line_count": 1, "total_length_meters": 515.1228885356956}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.585]}, "properties": {"data_items": [{"id": 3, "name": "嘉陵江滨江绿道", "category": "步道"}], "line_count": 1, "total_length_meters": 866.5683256849777}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.585]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "line_count": 1, "total_length_meters": 180.29555483446813}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "line_count": 1, "total_length_meters": 133.4566188717526}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "line_count": 1, "total_length_meters": 1026.6049140227747}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}, {"id": 2, "name": "轨道交通2号线（解放碑段）", "category": "轨道交通"}], "line_count": 2, "total_length_meters": 1387.0509002394483}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "line_count": 1, "total_length_meters": 120.27199158585553}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.555]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "line_count": 1, "total_length_meters": 291.28655503501346}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.545]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "line_count": 1, "total_length_meters": 34.91093782416542}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.555]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "line_count": 1, "total_length_meters": 520.9349802079854}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.555]}, "properties": {"data_items": [{"id": 2, "name": "轨道交通2号线（解放碑段）", "category": "轨道交通"}], "line_count": 1, "total_length_meters": 860.176449045972}}]}
```



**按缩放级别动态聚合**

根据前端传入的缩放级别与地图边界，动态判断是否对点数据进行网格聚合。低缩放级别时以网格聚合形式返回中心点和属性，高缩放级别（zoom ≥ 16）则返回原始点数据，并统一以 GeoJSON FeatureCollection 输出，适配地图可视化需求。

```sql
WITH
-- 模拟当前视图的边界和缩放级别
params AS (
  SELECT
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326) AS bbox,
    13 AS zoom_level  -- 改这个值模拟不同缩放级别
),

-- 根据 zoom_level 决定网格大小，若 zoom >= 16 则为 NULL 表示不聚合
grid_size AS (
  SELECT
    zoom_level,
    CASE
      WHEN zoom_level < 5 THEN 0.5        -- 世界级
      WHEN zoom_level < 7 THEN 0.2        -- 国家级
      WHEN zoom_level < 9 THEN 0.1        -- 区域级
      WHEN zoom_level < 11 THEN 0.05      -- 城市级
      WHEN zoom_level < 13 THEN 0.02      -- 区县级
      WHEN zoom_level < 14 THEN 0.01      -- 街道级
      WHEN zoom_level < 15 THEN 0.005     -- 社区级
      WHEN zoom_level < 16 THEN 0.002     -- 小区级
      ELSE NULL                           -- >=16 显示原始点
    END AS cell_size,
    bbox
  FROM params
),

-- 构造网格（当 cell_size 非空时才构建）
grid AS (
  SELECT (ST_SquareGrid(gs.cell_size, gs.bbox)).*
  FROM grid_size gs
  WHERE gs.cell_size IS NOT NULL
),

-- 聚合数据（仅当 zoom_level < 16）
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(l.*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', l.id,
            'name', l.name,
            'category', l.category
        )
    ) AS data_items
  FROM grid g
  JOIN linestring_entities l
    ON ST_Intersects(g.geom, l.geom)
  GROUP BY g.geom
),

-- 原始点数据（仅当 zoom_level >= 16）
raw_points AS (
  SELECT
    l.geom AS center_point,
    1 AS point_count,
    JSON_BUILD_ARRAY(
        JSON_BUILD_OBJECT(
            'id', l.id,
            'name', l.name,
            'category', l.category
        )
    ) AS data_items
  FROM linestring_entities l, params pa
  WHERE ST_Intersects(l.geom, pa.bbox)
)
, result AS (
-- 最终输出
SELECT * FROM aggregated WHERE (SELECT zoom_level FROM params) < 16
UNION ALL
SELECT * FROM raw_points WHERE (SELECT zoom_level FROM params) >= 16
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(r)::jsonb)
    ) AS feature
FROM result as r;
```

关键部分说明：

- `params`: 定义地图的可视边界 (`bbox`) 和当前缩放等级 (`zoom_level`)，作为后续逻辑判断的基础。
- `grid_size`: 根据 `zoom_level` 计算网格的边长（`cell_size`），zoom 越小格子越大；当 zoom ≥ 16 时，返回 `NULL` 表示不做聚合。
- `grid`: 在 `cell_size` 非空时，构造指定区域内的正方形网格，作为聚合区域的基础。
- `aggregated`: 针对 zoom < 16 情况，将点数据按网格聚合，返回每个格子中心点、点数量及聚合后的属性数据（以 JSON 数组形式表示）。
- `raw_points`: 针对 zoom ≥ 16 的情况，返回边界范围内的原始点数据，构造为类似聚合结构（每个点当作单独一格），便于前端统一处理。
- `result`: 根据当前 zoom 级别选择性地返回聚合数据或原始点数据（两者通过 `UNION ALL` 合并）。
- `ST_AsGeoJSON(...)`:
     将每条记录转换为 GeoJSON Feature，统一输出为 `FeatureCollection`，方便前端直接加载渲染。

返回数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.595]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.595]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.605]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.585]}, "properties": {"data_items": [{"id": 3, "name": "嘉陵江滨江绿道", "category": "步道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.585]}, "properties": {"data_items": [{"id": 3, "name": "嘉陵江滨江绿道", "category": "步道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.585]}, "properties": {"data_items": [{"id": 4, "name": "红旗河沟至重庆北站道路", "category": "主干道"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}, {"id": 2, "name": "轨道交通2号线（解放碑段）", "category": "轨道交通"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "长江滨江路段", "category": "城市道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.555]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.545]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.555]}, "properties": {"data_items": [{"id": 5, "name": "南滨路夜景段", "category": "景观道路"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.555]}, "properties": {"data_items": [{"id": 2, "name": "轨道交通2号线（解放碑段）", "category": "轨道交通"}], "point_count": 1}}]}
```



## POLYGON多边形表

### 数据准备

**创建表**

```sql
CREATE TABLE polygon_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(POLYGON, 4326)
);
CREATE INDEX idx_polygon_geom ON polygon_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO polygon_entities (name, category, geom) VALUES
('人民大礼堂广场', '地标广场', 'POLYGON((106.5430 29.5614, 106.5446 29.5600, 106.5460 29.5613, 106.5445 29.5625, 106.5430 29.5614))'),
('鹅岭公园区域', '城市公园', 'POLYGON((106.5302 29.5577, 106.5325 29.5562, 106.5340 29.5585, 106.5315 29.5598, 106.5302 29.5577))'),
('重庆大学A区', '高校校区', 'POLYGON((106.4556 29.5612, 106.4589 29.5601, 106.4605 29.5631, 106.4572 29.5644, 106.4556 29.5612))'),
('磁器口古镇核心区', '历史文化保护区', 'POLYGON((106.4273 29.5855, 106.4295 29.5840, 106.4308 29.5862, 106.4284 29.5876, 106.4273 29.5855))'),
('南山植物园一区', '自然保护区', 'POLYGON((106.6195 29.5071, 106.6212 29.5056, 106.6230 29.5078, 106.6208 29.5090, 106.6195 29.5071))'),
('渝中核心区', '城区', 'POLYGON((106.50 29.54, 106.60 29.54, 106.60 29.61, 106.50 29.61, 106.50 29.54))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM polygon_entities;
```

### 使用示例

#### 空间关系查询

查询包含某个点的多边形

```sql
SELECT *
FROM polygon_entities
WHERE ST_Contains(
  geom,
  ST_SetSRID(ST_Point(106.5505, 29.5630), 4326)
);
```

查询与某条线相交的多边形

```sql
WITH line AS (
  SELECT ST_SetSRID(ST_MakeLine(
    ARRAY[
      ST_Point(106.55, 29.56),
      ST_Point(106.57, 29.57)
    ]
  ), 4326) AS geom
)
SELECT p.*
FROM polygon_entities p, line
WHERE ST_Intersects(p.geom, line.geom);
```

查询两个多边形的空间关系

```sql
-- 判断是否相交（任意接触或重叠）
SELECT a.name AS poly_a, b.name AS poly_b
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Intersects(a.geom, b.geom)
WHERE a.id < b.id;
-- 判断是否完全包含（A 包含 B）
SELECT a.name AS container, b.name AS contained
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Contains(a.geom, b.geom)
WHERE a.id <> b.id;
-- 判断是否被包含（A 在 B 内）
SELECT a.name AS inside, b.name AS container
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Within(a.geom, b.geom)
WHERE a.id <> b.id;
-- 判断是否相邻（边界相接但内部不重合）
SELECT a.name AS poly_a, b.name AS poly_b
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Touches(a.geom, b.geom)
WHERE a.id < b.id;
-- 判断是否穿过（交叉而非包含）
SELECT a.name, b.name
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Crosses(a.geom, b.geom)
WHERE a.id < b.id;
-- 更宽容的包含：包含或边界接触
SELECT a.name, b.name
FROM polygon_entities a
JOIN polygon_entities b
  ON ST_Covers(a.geom, b.geom)
WHERE a.id <> b.id;
```

#### 空间分析类查询

计算面积（㎡）

```sql
SELECT id, name, ST_Area(ST_Transform(geom, 3857)) AS area_m2
FROM polygon_entities;
```

获取多边形的中心点（用于打点标注）

```sql
SELECT id, name, ST_AsText(ST_Centroid(geom)) AS center
FROM polygon_entities;
```

查询重叠的多边形

```sql
SELECT a.id, b.id
FROM polygon_entities a, polygon_entities b
WHERE a.id < b.id AND ST_Overlaps(a.geom, b.geom);
```

#### GeoJSON

**提取每条记录的基本属性及其几何字段的 GeoJSON 表示**

```sql
SELECT id, name, category,
       ST_AsGeoJSON(geom) AS geojson
FROM polygon_entities;
```

**将整条记录（包含所有属性和几何）序列化为 GeoJSON（Feature 格式）**

```sql
SELECT ST_AsGeoJSON(t) AS geojson
FROM polygon_entities as t;
```

**将所有记录序列化为 GeoJSON（Feature 格式），并聚合为数组**

```sql
SELECT jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson
FROM polygon_entities as t;
```

**构建标准的 GeoJSON FeatureCollection 对象，包含所有要素**

```sql
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(ST_AsGeoJSON(t)::jsonb)
) AS geojson_collection
FROM polygon_entities t;
```

#### 数据简化

**基础示例**

简化几何图形的实际使用示例，它可以在前端地图缩放级别较低时，减少图形点数，提升渲染性能。

```sql
SELECT
    id,
    name,
    category,
    ST_SimplifyPreserveTopology(geom, 0.001) AS simplified_geom
FROM polygon_entities
WHERE ST_Within(
    geom,
    ST_MakeEnvelope(106.0, 29.0, 107.0, 30.0, 4326)
);
```

**动态根据缩放级别简化**

```sql
WITH zoom AS (
    SELECT 11 AS zoom_level  -- 前端传入的缩放级别
)
SELECT
    id,
    name,
    category,
    CASE
        WHEN z.zoom_level < 5 THEN ST_SimplifyPreserveTopology(p.geom, 0.1)
        WHEN z.zoom_level < 7 THEN ST_SimplifyPreserveTopology(p.geom, 0.05)
        WHEN z.zoom_level < 9 THEN ST_SimplifyPreserveTopology(p.geom, 0.01)
        WHEN z.zoom_level < 11 THEN ST_SimplifyPreserveTopology(p.geom, 0.005)
        WHEN z.zoom_level < 13 THEN ST_SimplifyPreserveTopology(p.geom, 0.001)
        WHEN z.zoom_level < 15 THEN ST_SimplifyPreserveTopology(p.geom, 0.0005)
        WHEN z.zoom_level < 17 THEN ST_SimplifyPreserveTopology(p.geom, 0.0001)
        ELSE p.geom
    END AS display_geom
FROM polygon_entities p
CROSS JOIN zoom z
WHERE ST_Within(
    p.geom,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
);
```

#### 网格数据聚合2

**按固定网格聚合**

按照固定大小网格进行空间聚合，统计每个网格中相交的多边形数量、多边形在该格子内的总面积（单位：米）以及相关属性，并以网格中心点作为代表位置输出，适用于中低缩放级别下的地图线聚合展示。

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS polygon_count,
    SUM(ST_Area(p.geom::geography)) AS total_area,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM grid g
  JOIN polygon_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
)
SELECT * FROM aggregated;
```

关键部分说明：

- `grid`: 使用 `ST_SquareGrid` 构造一个网格，覆盖指定的地理范围（此处为 `(106.50, 29.50, 106.60, 29.60)`），每个网格单元为 0.01 度大小的正方形。
- `ST_MakeEnvelope(...)`: 定义查询所覆盖的地理边界（bounding box），即当前地图视图区域。
- `ST_Intersects(g.geom, l.geom)`: 判断线是否与当前网格单元相交，作为聚合判断条件。
- `ST_Centroid(g.geom) AS center_point`: 计算每个网格的中心点，用于前端可视化定位。
- `COUNT(l.*) AS line_count`: 统计当前网格内交叉的线数量。
- `ST_Intersection(g.geom, l.geom)`: 提取线与网格单元相交的部分几何形状。
- `ST_Length(...::geography)`: 计算相交线段的实际长度（以米为单位），`::geography` 用于准确的地理距离测量。
- `SUM(...) AS total_length_meters`: 聚合当前格子中所有线段的总长度。
- `JSON_AGG(...) AS data_items`: 将当前格子内所有线条的属性（id、name、category）聚合成一个 JSON 数组，便于前端展示属性详情。

返回GeoJSON数据

```sql
WITH
grid AS (
  SELECT (ST_SquareGrid(
    0.01,
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326)
  )).*
),
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS polygon_count,
    SUM(ST_Area(p.geom::geography)) AS total_area,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM grid g
  JOIN polygon_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(agg)::jsonb)
    ) AS feature
FROM aggregated as agg;
```

返回以下数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "人民大礼堂广场", "category": "地标广场"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75222532.97116923, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.565]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.585]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.585]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.575]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.565]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.535]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.545]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 9658396.74378395, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.545]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 84840701.85728645, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.555]}, "properties": {"data_items": [{"id": 2, "name": "鹅岭公园区域", "category": "城市公园"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75260077.84918952, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 1, "name": "人民大礼堂广场", "category": "地标广场"}], "total_area": 75222532.97116923, "polygon_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "total_area": 75182305.1135025, "polygon_count": 1}}]}
```



**按缩放级别动态聚合**

根据前端传入的缩放级别与地图边界，动态判断是否对点数据进行网格聚合。低缩放级别时以网格聚合形式返回中心点和属性，高缩放级别（zoom ≥ 16）则返回原始点数据，并统一以 GeoJSON FeatureCollection 输出，适配地图可视化需求。

```sql
WITH
-- 模拟当前视图的边界和缩放级别
params AS (
  SELECT
    ST_MakeEnvelope(106.50, 29.50, 106.60, 29.60, 4326) AS bbox,
    13 AS zoom_level  -- 改这个值模拟不同缩放级别
),

-- 根据 zoom_level 决定网格大小，若 zoom >= 16 则为 NULL 表示不聚合
grid_size AS (
  SELECT
    zoom_level,
    CASE
      WHEN zoom_level < 5 THEN 0.5        -- 世界级
      WHEN zoom_level < 7 THEN 0.2        -- 国家级
      WHEN zoom_level < 9 THEN 0.1        -- 区域级
      WHEN zoom_level < 11 THEN 0.05      -- 城市级
      WHEN zoom_level < 13 THEN 0.02      -- 区县级
      WHEN zoom_level < 14 THEN 0.01      -- 街道级
      WHEN zoom_level < 15 THEN 0.005     -- 社区级
      WHEN zoom_level < 16 THEN 0.002     -- 小区级
      ELSE NULL                           -- >=16 显示原始点
    END AS cell_size,
    bbox
  FROM params
),

-- 构造网格（当 cell_size 非空时才构建）
grid AS (
  SELECT (ST_SquareGrid(gs.cell_size, gs.bbox)).*
  FROM grid_size gs
  WHERE gs.cell_size IS NOT NULL
),

-- 聚合数据（仅当 zoom_level < 16）
aggregated AS (
  SELECT
    ST_Centroid(g.geom) AS center_point,
    COUNT(p.*) AS point_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM grid g
  JOIN polygon_entities p
    ON ST_Intersects(g.geom, p.geom)
  GROUP BY g.geom
),

-- 原始点数据（仅当 zoom_level >= 16）
raw_points AS (
  SELECT
    p.geom AS center_point,
    1 AS point_count,
    JSON_BUILD_ARRAY(
        JSON_BUILD_OBJECT(
            'id', p.id,
            'name', p.name,
            'category', p.category
        )
    ) AS data_items
  FROM polygon_entities p, params pa
  WHERE ST_Intersects(p.geom, pa.bbox)
)
, result AS (
-- 最终输出
SELECT * FROM aggregated WHERE (SELECT zoom_level FROM params) < 16
UNION ALL
SELECT * FROM raw_points WHERE (SELECT zoom_level FROM params) >= 16
)
SELECT
    JSON_BUILD_OBJECT(
        'type', 'FeatureCollection',
        'features', jsonb_agg(ST_AsGeoJSON(r)::jsonb)
    ) AS feature
FROM result as r;
```

关键部分说明：

- `params`: 定义地图的可视边界 (`bbox`) 和当前缩放等级 (`zoom_level`)，作为后续逻辑判断的基础。
- `grid_size`: 根据 `zoom_level` 计算网格的边长（`cell_size`），zoom 越小格子越大；当 zoom ≥ 16 时，返回 `NULL` 表示不做聚合。
- `grid`: 在 `cell_size` 非空时，构造指定区域内的正方形网格，作为聚合区域的基础。
- `aggregated`: 针对 zoom < 16 情况，将点数据按网格聚合，返回每个格子中心点、点数量及聚合后的属性数据（以 JSON 数组形式表示）。
- `raw_points`: 针对 zoom ≥ 16 的情况，返回边界范围内的原始点数据，构造为类似聚合结构（每个点当作单独一格），便于前端统一处理。
- `result`: 根据当前 zoom 级别选择性地返回聚合数据或原始点数据（两者通过 `UNION ALL` 合并）。
- `ST_AsGeoJSON(...)`:
     将每条记录转换为 GeoJSON Feature，统一输出为 `FeatureCollection`，方便前端直接加载渲染。

返回数据

```json
{"type" : "FeatureCollection", "features" : [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.605]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.595]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.565]}, "properties": {"data_items": [{"id": 1, "name": "人民大礼堂广场", "category": "地标广场"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.565]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.585]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.585]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.585]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.575]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.575]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.565]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.565]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.535]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.605, 29.545]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.595, 29.545]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 13, "name": "渝中区和南岸区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.585, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.575, 29.555]}, "properties": {"data_items": [{"id": 13, "name": "渝中区和南岸区", "category": "城区"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.565, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.555]}, "properties": {"data_items": [{"id": 2, "name": "鹅岭公园区域", "category": "城市公园"}, {"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}, {"id": 1, "name": "人民大礼堂广场", "category": "地标广场"}], "point_count": 2}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.555]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.505, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.515, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.525, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.535, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.545, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.545]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [106.555, 29.535]}, "properties": {"data_items": [{"id": 6, "name": "渝中核心区", "category": "城区"}], "point_count": 1}}]}
```



## GEOMETRY几何混合表

### 数据准备

**创建表**

```sql
CREATE TABLE geometry_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(GEOMETRY, 4326)
);
CREATE INDEX idx_geometry_geom ON geometry_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO geometry_entities (name, category, geom) VALUES
('磁器口古镇入口', '景点-点', 'POINT(106.4285 29.5849)'),
('朝天门到解放碑主路', '主干道-线', 'LINESTRING(106.5867 29.5702, 106.5821 29.5670, 106.5784 29.5628)'),
('重庆动物园区域', '公园-面', 'POLYGON((106.5060 29.4962, 106.5088 29.4945, 106.5107 29.4973, 106.5075 29.4989, 106.5060 29.4962))'),
('渝中半岛多点布控', '监控点组', 'MULTIPOINT((106.5671 29.5591), (106.5743 29.5633), (106.5801 29.5666))'),
('两江交汇地带河岸线', '景观带-多线', 'MULTILINESTRING((106.5775 29.5617, 106.5831 29.5580), (106.5831 29.5580, 106.5888 29.5554))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM geometry_entities;
```

### 使用示例

#### 基础查询

查询某种几何类型的数据

```sql
SELECT *
FROM geometry_entities
WHERE GeometryType(geom) = 'POINT';
```

查询某种几何类型的数据（别名）

```sql
SELECT *
FROM geometry_entities
WHERE ST_GeometryType(geom) = 'ST_LineString';
```

#### 按类型分类统计

统计各种几何类型的数量

```sql
SELECT ST_GeometryType(geom) AS type, COUNT(*) AS count
FROM geometry_entities
GROUP BY type;
```

#### 空间关系查询

查询与某个区域相交的任意几何

```sql
SELECT *
FROM geometry_entities
WHERE ST_Intersects(
  geom,
  ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(106.55 29.55, 106.60 29.55, 106.60 29.60, 106.55 29.60, 106.55 29.55)')), 4326)
);
```

查询包含某点的几何

```sql
SELECT *
FROM geometry_entities
WHERE ST_Contains(
  geom,
  ST_SetSRID(ST_Point(106.5612, 29.5623), 4326)
);
```

#### 数据质量与校验

查询非法或无效几何

```sql
SELECT *
FROM geometry_entities
WHERE NOT ST_IsValid(geom);
```

查询几何是否为空

```sql
SELECT * FROM geometry_entities WHERE ST_IsEmpty(geom);
```

#### 其他常用函数

获取几何的边界框（用于缩放地图）

```sql
SELECT id, ST_Extent(geom) OVER () AS bbox FROM geometry_entities;
```

#### GeoJSON

**提取每条记录的基本属性及其几何字段的 GeoJSON 表示**

```sql
SELECT id, name, category,
       ST_AsGeoJSON(geom) AS geojson
FROM geometry_entities;
```

**将整条记录（包含所有属性和几何）序列化为 GeoJSON（Feature 格式）**

```sql
SELECT ST_AsGeoJSON(t) AS geojson
FROM geometry_entities as t;
```

**将所有记录序列化为 GeoJSON（Feature 格式），并聚合为数组**

```sql
SELECT jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson
FROM geometry_entities as t;
```

**构建标准的 GeoJSON FeatureCollection 对象，包含所有要素**

```sql
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(ST_AsGeoJSON(t)::jsonb)
) AS geojson_collection
FROM geometry_entities t;
```



## MULTIPOINT多点集合表

### 数据准备

**创建表**

```sql
CREATE TABLE multipoint_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(MULTIPOINT, 4326)
);
CREATE INDEX idx_multipoint_geom ON multipoint_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO multipoint_entities (name, category, geom) VALUES
('解放碑周边监控点', '公共安全', 'MULTIPOINT((106.5784 29.5628), (106.5796 29.5635), (106.5773 29.5612))'),
('南岸滨江广场测温点', '公共服务', 'MULTIPOINT((106.5865 29.5541), (106.5892 29.5530), (106.5918 29.5515))'),
('轨道交通2号线车站定位', '交通设施', 'MULTIPOINT((106.5701 29.5543), (106.5746 29.5582), (106.5784 29.5628))'),
('沙坪坝商圈人流传感器', '智能感知', 'MULTIPOINT((106.4542 29.5410), (106.4586 29.5433), (106.4609 29.5456))'),
('长江大桥桥头监测点', '桥梁监测', 'MULTIPOINT((106.5481 29.5620), (106.5510 29.5604), (106.5537 29.5589))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM multipoint_entities;
```

### 使用示例

#### 空间操作

**拆分 MultiPoint 中的每一个点**

💡 `ST_DumpPoints` 会把 `MULTIPOINT` 拆成独立的 `POINT`。

```sql
SELECT
  id,
  name,
  category,
  ST_AsText((ST_DumpPoints(geom)).geom) AS point_geom
FROM multipoint_entities;
```

**统计每个记录包含多少个点**

```sql
SELECT
  id,
  name,
  ST_NPoints(geom) AS point_count
FROM multipoint_entities;
```

**查询包含特定坐标点的记录**

注意：`ST_Contains` 在 `MULTIPOINT` 上不是总是可用，可使用 `ST_Intersects` 更保险。

```sql
SELECT *
FROM multipoint_entities
WHERE ST_Contains(
  geom,
  ST_SetSRID(ST_Point(106.5600, 29.5600), 4326)
);
```

#### 空间范围与筛选

**查找某个区域内的 MultiPoint（至少有一个点落在范围内）**

```sql
SELECT *
FROM multipoint_entities
WHERE ST_Intersects(
  geom,
  ST_SetSRID(ST_MakeEnvelope(106.55, 29.55, 106.60, 29.60), 4326)
);
```

#### GeoJSON

**提取每条记录的基本属性及其几何字段的 GeoJSON 表示**

```sql
SELECT id, name, category,
       ST_AsGeoJSON(geom) AS geojson
FROM multipoint_entities;
```

**将整条记录（包含所有属性和几何）序列化为 GeoJSON（Feature 格式）**

```sql
SELECT ST_AsGeoJSON(t) AS geojson
FROM multipoint_entities as t;
```

**将所有记录序列化为 GeoJSON（Feature 格式），并聚合为数组**

```sql
SELECT jsonb_agg(ST_AsGeoJSON(t)::jsonb) AS geojson
FROM multipoint_entities as t;
```

**构建标准的 GeoJSON FeatureCollection 对象，包含所有要素**

```sql
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(ST_AsGeoJSON(t)::jsonb)
) AS geojson_collection
FROM multipoint_entities t;
```



## MULTILINESTRING多线集合表

### 数据准备

**创建表**

```sql
CREATE TABLE multiline_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(MULTILINESTRING, 4326)
);
CREATE INDEX idx_multiline_geom ON multiline_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO multiline_entities (name, category, geom) VALUES
('渝中区道路网', '城市道路', 'MULTILINESTRING((106.5784 29.5628, 106.5801 29.5615), (106.5746 29.5582, 106.5730 29.5550))'),
('轨道交通1号线', '轨道交通', 'MULTILINESTRING((106.5354 29.5761, 106.5323 29.5732), (106.5249 29.5664, 106.5205 29.5631))'),
('长江滨江绿道', '步道', 'MULTILINESTRING((106.5775 29.5617, 106.5831 29.5580), (106.5863 29.5545, 106.5894 29.5522))'),
('沙坪坝环城快速路', '快速路网', 'MULTILINESTRING((106.4645 29.5473, 106.4678 29.5461), (106.4720 29.5440, 106.4783 29.5409))'),
('渝北区环线道路', '主干道', 'MULTILINESTRING((106.5497 29.6071, 106.5521 29.6052), (106.5576 29.6034, 106.5630 29.6025))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM multiline_entities;
```

### 使用示例



## MULTIPOLYGON多多边形集合表

### 数据准备

**创建表**

```sql
CREATE TABLE multipolygon_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(MULTIPOLYGON, 4326)
);
CREATE INDEX idx_multipolygon_geom ON multipolygon_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO multipolygon_entities (name, category, geom) VALUES
('重庆市主城区公园', '公园组合', 'MULTIPOLYGON(((106.5287 29.5586, 106.5300 29.5569, 106.5321 29.5578, 106.5305 29.5590, 106.5287 29.5586)), ((106.5104 29.4965, 106.5127 29.4951, 106.5140 29.4978, 106.5114 29.4983, 106.5104 29.4965)))'),
('南山-长江生态带', '生态保护区', 'MULTIPOLYGON(((106.6125 29.5051, 106.6145 29.5035, 106.6170 29.5052, 106.6150 29.5070, 106.6125 29.5051)), ((106.5765 29.5557, 106.5798 29.5530, 106.5821 29.5523, 106.5783 29.5547, 106.5765 29.5557)))'),
('渝北区商业区与住宅区', '城市规划区', 'MULTIPOLYGON(((106.5346 29.6274, 106.5378 29.6250, 106.5402 29.6273, 106.5376 29.6292, 106.5346 29.6274)), ((106.5425 29.6098, 106.5459 29.6083, 106.5473 29.6104, 106.5450 29.6117, 106.5425 29.6098)))'),
('重庆大渡口区与沙坪坝区交界', '行政区划', 'MULTIPOLYGON(((106.4550 29.5278, 106.4582 29.5250, 106.4600 29.5280, 106.4565 29.5303, 106.4550 29.5278)), ((106.4678 29.5180, 106.4701 29.5163, 106.4724 29.5188, 106.4700 29.5201, 106.4678 29.5180)))'),
('长江-嘉陵江交汇区域', '水域交界', 'MULTIPOLYGON(((106.5762 29.5634, 106.5791 29.5616, 106.5812 29.5600, 106.5785 29.5584, 106.5762 29.5634)), ((106.5845 29.5585, 106.5868 29.5567, 106.5884 29.5549, 106.5852 29.5536, 106.5845 29.5585)))');
```

**查询数据**

```
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM multipolygon_entities;
```

### 使用示例



## GEOMETRYCOLLECTION几何混合集合表

### 数据准备

**创建表**

```sql
CREATE TABLE geom_collection_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(GEOMETRYCOLLECTION, 4326)
);
CREATE INDEX idx_geom_collection_geom ON geom_collection_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO geom_collection_entities (name, category, geom) VALUES
('解放碑多元化空间数据', '城市空间', 'GEOMETRYCOLLECTION(POINT(106.5784 29.5628), LINESTRING(106.5746 29.5582, 106.5730 29.5550), POLYGON((106.5430 29.5614, 106.5446 29.5600, 106.5460 29.5613, 106.5445 29.5625, 106.5430 29.5614)))'),
('重庆科技园区', '科技园区', 'GEOMETRYCOLLECTION(POINT(106.4876 29.5860), POLYGON((106.4819 29.5855, 106.4850 29.5837, 106.4874 29.5842, 106.4860 29.5860, 106.4819 29.5855)), LINESTRING(106.4882 29.5847, 106.4890 29.5862))'),
('南山风景区', '自然景区', 'GEOMETRYCOLLECTION(POINT(106.6125 29.5051), POLYGON((106.6120 29.5055, 106.6140 29.5037, 106.6160 29.5045, 106.6145 29.5060, 106.6120 29.5055)), LINESTRING(106.6132 29.5044, 106.6152 29.5050))'),
('渝中区交通节点', '交通枢纽', 'GEOMETRYCOLLECTION(POINT(106.5801 29.5615), LINESTRING(106.5790 29.5605, 106.5783 29.5589), POLYGON((106.5730 29.5590, 106.5745 29.5580, 106.5760 29.5605, 106.5740 29.5615, 106.5730 29.5590)))'),
('两江汇流处标志区域', '地标区域', 'GEOMETRYCOLLECTION(POINT(106.5762 29.5634), POLYGON((106.5775 29.5617, 106.5831 29.5580, 106.5855 29.5563, 106.5780 29.5587, 106.5775 29.5617)), LINESTRING(106.5770 29.5632, 106.5790 29.5618))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(geom) AS geom_wkt FROM geom_collection_entities;
```

### 使用示例



## CIRCULARSTRING 表

`CIRCULARSTRING` 用于表示圆弧线段。

### 数据准备

**创建表**

```sql
CREATE TABLE circularstring_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(CIRCULARSTRING, 4326)
);

CREATE INDEX idx_circularstring_geom ON circularstring_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO circularstring_entities (name, category, geom) VALUES
('解放碑周边圆弧', '商业区', 'CIRCULARSTRING(106.5772 29.5585, 106.5788 29.5597, 106.5801 29.5610)'),
('重庆科技园圆形区域', '科技园区', 'CIRCULARSTRING(106.4876 29.5860, 106.4890 29.5875, 106.4905 29.5890)'),
('南山观景点圆弧', '自然景区', 'CIRCULARSTRING(106.6125 29.5051, 106.6145 29.5060, 106.6160 29.5075)'),
('渝中区交通圆弧', '交通枢纽', 'CIRCULARSTRING(106.5795 29.5610, 106.5810 29.5625, 106.5825 29.5635)'),
('两江汇流处圆形区域', '地标区域', 'CIRCULARSTRING(106.5760 29.5620, 106.5775 29.5630, 106.5790 29.5645)');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(ST_CurveToLine(geom)) AS geom_wkt FROM circularstring_entities;
```

### 使用示例



## COMPOUNDCURVE 表

`COMPOUNDCURVE` 是多个曲线（例如线段和圆弧）的组合。

### 数据准备

**创建表**

```sql
CREATE TABLE compoundcurve_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(COMPOUNDCURVE, 4326)
);

CREATE INDEX idx_compoundcurve_geom ON compoundcurve_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO compoundcurve_entities (name, category, geom) VALUES
('解放碑步行街曲线', '商业区', 'COMPOUNDCURVE(LINESTRING(106.5784 29.5628, 106.5800 29.5640), CIRCULARSTRING(106.5800 29.5640, 106.5815 29.5655, 106.5830 29.5640), LINESTRING(106.5830 29.5640, 106.5840 29.5630))'),
('重庆科技园曲线区域', '科技园区', 'COMPOUNDCURVE(LINESTRING(106.4876 29.5860, 106.4888 29.5872), CIRCULARSTRING(106.4888 29.5872, 106.4900 29.5880, 106.4915 29.5870), LINESTRING(106.4915 29.5870, 106.4930 29.5855))'),
('南山观景区域曲线', '自然景区', 'COMPOUNDCURVE(LINESTRING(106.6125 29.5051, 106.6135 29.5060), CIRCULARSTRING(106.6135 29.5060, 106.6145 29.5075, 106.6160 29.5060), LINESTRING(106.6160 29.5060, 106.6175 29.5050))'),
('渝中区交通枢纽曲线', '交通枢纽', 'COMPOUNDCURVE(LINESTRING(106.5795 29.5610, 106.5810 29.5625), CIRCULARSTRING(106.5810 29.5625, 106.5825 29.5635, 106.5840 29.5620), LINESTRING(106.5840 29.5620, 106.5855 29.5615))'),
('两江汇流处曲线区', '地标区域', 'COMPOUNDCURVE(LINESTRING(106.5760 29.5620, 106.5770 29.5630), CIRCULARSTRING(106.5770 29.5630, 106.5785 29.5635, 106.5800 29.5625), LINESTRING(106.5800 29.5625, 106.5815 29.5615))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(ST_CurveToLine(geom)) AS geom_wkt FROM compoundcurve_entities;
```

### 使用示例



## CURVEPOLYGON 表

`CURVEPOLYGON` 是带曲线的多边形，允许其边界由圆弧等曲线组成。

### 数据准备

**创建表**

```sql
CREATE TABLE curvepolygon_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(CURVEPOLYGON, 4326)
);

CREATE INDEX idx_curvepolygon_geom ON curvepolygon_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO curvepolygon_entities (name, category, geom) VALUES
('解放碑圆弧广场', '商业区',
 'CURVEPOLYGON(COMPOUNDCURVE(
     CIRCULARSTRING(106.5780 29.5620, 106.5795 29.5635, 106.5810 29.5620),
     CIRCULARSTRING(106.5810 29.5620, 106.5795 29.5605, 106.5780 29.5620)
 ))'),

('重庆高新区弯曲区域', '科技园区',
 'CURVEPOLYGON(COMPOUNDCURVE(
     LINESTRING(106.4850 29.5860, 106.4865 29.5875),
     CIRCULARSTRING(106.4865 29.5875, 106.4880 29.5860, 106.4850 29.5860)
 ))'),

('南山山脚公园', '自然景区',
 'CURVEPOLYGON(COMPOUNDCURVE(
     CIRCULARSTRING(106.6100 29.5040, 106.6115 29.5055, 106.6130 29.5040),
     CIRCULARSTRING(106.6130 29.5040, 106.6115 29.5025, 106.6100 29.5040)
 ))'),

('较场口曲面广场', '休闲广场',
 'CURVEPOLYGON(COMPOUNDCURVE(
     LINESTRING(106.5700 29.5580, 106.5715 29.5590),
     CIRCULARSTRING(106.5715 29.5590, 106.5730 29.5580, 106.5700 29.5580)
 ))'),

('江北嘴弯形地块', '金融区',
 'CURVEPOLYGON(COMPOUNDCURVE(
     CIRCULARSTRING(106.5320 29.5670, 106.5335 29.5685, 106.5350 29.5670),
     CIRCULARSTRING(106.5350 29.5670, 106.5335 29.5655, 106.5320 29.5670)
 ))');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(ST_CurveToLine(geom)) AS geom_wkt FROM curvepolygon_entities;
```

### 使用示例



## MULTICURVE 表

`MULTICURVE` 用于表示多个曲线（可以是多条直线、圆弧等）的集合。

### 数据准备

**创建表**

```sql
CREATE TABLE multicurve_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(MULTICURVE, 4326)
);

CREATE INDEX idx_multicurve_geom ON multicurve_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO multicurve_entities (name, category, geom) VALUES
('解放碑交通线路', '交通线网',
 'MULTICURVE(
    LINESTRING(106.5770 29.5615, 106.5785 29.5625),
    CIRCULARSTRING(106.5785 29.5625, 106.5800 29.5635, 106.5815 29.5625)
 )'),

('南山景区环线', '景区道路',
 'MULTICURVE(
    CIRCULARSTRING(106.6105 29.5045, 106.6120 29.5055, 106.6135 29.5045),
    LINESTRING(106.6135 29.5045, 106.6150 29.5030)
 )'),

('重庆高新区交通组合', '城市交通',
 'MULTICURVE(
    LINESTRING(106.4840 29.5865, 106.4855 29.5875),
    CIRCULARSTRING(106.4855 29.5875, 106.4870 29.5885, 106.4885 29.5870)
 )'),

('嘉陵江滨河道', '滨江线路',
 'MULTICURVE(
    LINESTRING(106.5450 29.5670, 106.5470 29.5680),
    CIRCULARSTRING(106.5470 29.5680, 106.5485 29.5695, 106.5500 29.5680)
 )'),

('江北区环形公交线路', '公交环线',
 'MULTICURVE(
    CIRCULARSTRING(106.5330 29.5675, 106.5345 29.5685, 106.5360 29.5675),
    LINESTRING(106.5360 29.5675, 106.5330 29.5675)
 )');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(ST_CurveToLine(geom)) AS geom_wkt FROM multicurve_entities;
```

### 使用示例



## MULTISURFACE 表

`MULTISURFACE` 用于表示多个面（如多个多边形、曲面等）的集合。

### 数据准备

**创建表**

```sql
CREATE TABLE multisurface_entities (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    geom GEOMETRY(MULTISURFACE, 4326)
);

CREATE INDEX idx_multisurface_geom ON multisurface_entities USING GIST (geom);
```

**插入数据**

```sql
INSERT INTO multisurface_entities (name, category, geom) VALUES
('解放碑核心商圈', '商业区',
 'MULTISURFACE(
    POLYGON((106.5770 29.5610, 106.5785 29.5615, 106.5780 29.5630, 106.5765 29.5625, 106.5770 29.5610)),
    POLYGON((106.5788 29.5612, 106.5795 29.5618, 106.5790 29.5625, 106.5782 29.5620, 106.5788 29.5612))
 )'),

('南山风景区核心保护地块', '自然保护区',
 'MULTISURFACE(
    POLYGON((106.6100 29.5040, 106.6120 29.5045, 106.6110 29.5060, 106.6095 29.5055, 106.6100 29.5040)),
    POLYGON((106.6125 29.5035, 106.6140 29.5040, 106.6130 29.5050, 106.6118 29.5045, 106.6125 29.5035))
 )'),

('重庆高新区创新园', '科技园区',
 'MULTISURFACE(
    POLYGON((106.4840 29.5850, 106.4855 29.5855, 106.4850 29.5870, 106.4835 29.5865, 106.4840 29.5850)),
    POLYGON((106.4860 29.5860, 106.4875 29.5865, 106.4870 29.5880, 106.4858 29.5875, 106.4860 29.5860))
 )'),

('嘉陵江滨江绿地', '城市绿地',
 'MULTISURFACE(
    POLYGON((106.5440 29.5660, 106.5460 29.5665, 106.5455 29.5680, 106.5435 29.5675, 106.5440 29.5660)),
    POLYGON((106.5465 29.5670, 106.5480 29.5675, 106.5475 29.5690, 106.5460 29.5685, 106.5465 29.5670))
 )'),

('江北嘴商务区', '金融办公区',
 'MULTISURFACE(
    POLYGON((106.5320 29.5665, 106.5335 29.5670, 106.5330 29.5685, 106.5315 29.5680, 106.5320 29.5665)),
    POLYGON((106.5340 29.5672, 106.5355 29.5678, 106.5350 29.5690, 106.5338 29.5685, 106.5340 29.5672))
 )');
```

**查询数据**

```sql
SELECT id, name, category, created_at, ST_AsText(ST_CurveToLine(geom)) AS geom_wkt FROM multisurface_entities;
```

### 使用示例



