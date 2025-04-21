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

当前端地图移动或缩放时，只加载当前视图范围内的点，前端传入地图视图边界（bounding box）

```
{
  "minLat": ...,
  "minLng": ...,
  "maxLat": ...,
  "maxLng": ...
}
```

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

**地图视图变化频繁问题解决**

用户滑动地图很快时会触发大量请求，造成后端压力大 + 前端性能差 + 数据抖动

地图视图变化频繁时只监听 `moveend` 而不是 `move`

```
map.on('moveend', () => {
  // 获取地图范围然后请求数据
});
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



