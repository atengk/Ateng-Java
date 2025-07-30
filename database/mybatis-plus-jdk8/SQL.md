# MySQL

## 数据准备

```sql
INSERT INTO kongyu.project_mini (
    uuid,
    category_id,
    name,
    code,
    description,
    amount,
    score,
    balance,
    tags,
    priority,
    status,
    is_active,
    is_deleted,
    version,
    user_count,
    birth_date,
    last_login,
    start_date,
    end_date,
    region,
    file_path,
    json_object,
    json_array,
    location,
    ip_address,
    binary_data,
    created_at,
    updated_at
)
SELECT
    uuid_to_bin(uuid(), 1),       -- 新生成顺序UUID
    category_id,
    name,
    code,
    description,
    amount,
    score,
    balance,
    tags,
    priority,
    status,
    is_active,
    is_deleted,
    version,
    user_count,
    birth_date,
    last_login,
    start_date,
    end_date,
    region,
    file_path,
    json_object,
    json_array,
    location,
    ip_address,
    binary_data,
    created_at,
    updated_at
FROM kongyu.project
ORDER BY id DESC
LIMIT 100;
```

### 特殊字段数据设置

------

#### UUID（BINARY）

MySQL 中 UUID 通常存储为 `BINARY(16)` 类型，提高查询性能。

**插入 UUID 数据**

```sql
INSERT INTO kongyu.project_mini (id, name, uuid)
VALUES (0, '智慧项目', UUID_TO_BIN(uuid(), 1));
```

**更新 UUID**

```sql
UPDATE kongyu.project_mini
SET uuid = UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000', 1)
WHERE id = 1;
```

#### IP 地址（VARBINARY）

IP 地址（IPv4 或 IPv6）可存储为 `VARBINARY(16)`，MySQL 提供了转换函数 `INET6_ATON()` / `INET6_NTOA()` 进行编解码。

**插入 IP 地址**

```sql
INSERT INTO kongyu.project_mini (id, name, ip_address)
VALUES (-1, '园区项目', INET6_ATON('192.168.1.10'));
```

**更新 IP 地址**

```sql
UPDATE kongyu.project_mini
SET ip_address = INET6_ATON('10.0.0.1')
WHERE id = -1;
```

> 🔍 `INET6_ATON()` 适用于 IPv4 和 IPv6，建议统一使用。

------

#### 坐标位置（GEOMETRY）

通常存储为 `POINT(纬度 经度)`，字段类型为 `GEOMETRY` 或 `POINT`。需使用 `ST_GeomFromText()` 或 `ST_PointFromText()`。✅ 纬度在前，经度在后（WKT 标准格式）。

**插入坐标**

```sql
INSERT INTO kongyu.project_mini (id, name, location)
VALUES (-2, '滨江项目', ST_GeomFromText('POINT(30.654321 120.123456)', 4326));
```

**更新坐标**

```sql
UPDATE kongyu.project_mini
SET location = ST_PointFromText('POINT(31.0 121.0)', 4326)
WHERE id = -2;
```

**插入坐标 GeoJSON **

```sql
INSERT INTO kongyu.project_mini (id, name, location)
VALUES (
    -3,
    'GeoJSON项目',
    ST_GeomFromGeoJSON('{
        "type": "Point",
        "coordinates": [121.0, 31.0]
    }', 1, 4326);
);
```

- `1` 表示允许 `GeoJSON` 中带有额外属性；
- `4326` 是标准 WGS84 地理坐标系统的 SRID；

**更新坐标 GeoJSON **

```sql
UPDATE kongyu.project_mini
SET location = ST_GeomFromGeoJSON('{
        "type": "Point",
        "coordinates": [121.005, 31.005]
    }', 1, 4326);
WHERE id = -3;
```

#### JSON 对象/数组

JSON 字段可为对象或数组，支持直接插入 JSON 字符串或使用 `JSON_OBJECT()` / `JSON_ARRAY()` 构建。

**插入 JSON 对象**

```sql
INSERT INTO kongyu.project_mini (id, name, json_object)
VALUES (-4, '综合体项目', JSON_OBJECT('type', 'A', 'level', 5));
-- 等价于
INSERT INTO kongyu.project_mini (id, name, json_object)
VALUES (-44, '综合体项目', '{"type": "A", "level": 5}');
```

**插入 JSON 数组**

```sql
INSERT INTO kongyu.project_mini (id, name, json_array)
VALUES (-5, '智能园区', JSON_ARRAY('AI', 'IoT', 'BigData'));
-- 等价于
INSERT INTO kongyu.project_mini (id, name, json_array)
VALUES (-5, '智能园区', '["AI", "IoT", "BigData"]');
```

**更新 JSON 中的字段值**

如果不存在改值则会添加

```sql
UPDATE kongyu.project_mini
SET json_object = JSON_SET(json_object, '$.level', 10)
WHERE id = -4;
```

**更新 JSON 数组中的数据**

向 JSON 数组中追加元素（JSON_ARRAY_APPEND）

> 注意：该字段必须是 JSON 数组，否则报错。

```sql
UPDATE kongyu.project_mini
SET json_array = JSON_ARRAY_APPEND(json_array, '$', 'tag4')
WHERE id = -5;
```

替换数组中某个位置的值（JSON_REPLACE）

```sql
UPDATE kongyu.project_mini
SET json_array = JSON_REPLACE(json_array, '$[0]', 'tagX')
WHERE id = 102;
```

**删除对象字段或数组元素**

```sql
-- 删除对象中的键
UPDATE kongyu.project_mini
SET json_object = JSON_REMOVE(json_object, '$.level')
WHERE id = -4;

-- 删除数组中第一个元素
UPDATE kongyu.project_mini
SET json_array = JSON_REMOVE(json_array, '$[0]')
WHERE id = -5;
```



## 基础查询

**查询所有字段**

```sql
SELECT * FROM project_mini;
```

**指定字段查询**

```sql
SELECT id, name, code, amount, priority FROM project_mini;
```

**条件查询**：状态为“进行中”的项目

```sql
SELECT id, name, status
FROM project_mini
WHERE status = 1;
```

**范围查询**

金额在 1000 到 10000 之间（左右包含）

```sql
SELECT id, name, amount
FROM project_mini
WHERE amount BETWEEN 1000 AND 10000;
```

出身日期在 1990-01-01 到 2000-01-01 之间（左右包含）

```sql
SELECT id, name, birth_date
FROM project_mini
WHERE birth_date BETWEEN '1990-01-01' AND '2000-01-01';
```

**模糊查询**：名称包含“钱”的项目

```sql
SELECT id, name
FROM project_mini
WHERE name LIKE '%钱%';

SELECT id, name
FROM project_mini
WHERE name LIKE CONCAT('%', '钱' , '%');
```

**排序查询**：按创建时间倒序

```sql
SELECT id, name, created_at
FROM project_mini
ORDER BY created_at DESC;
```

**去重查询**：查询所有地区名

```sql
SELECT DISTINCT region FROM project_mini;
```

**聚合查询**：统计总数、金额总和、平均分数

```sql
SELECT
    COUNT(*) AS total_count,
    SUM(amount) AS total_amount,
    AVG(score) AS avg_score
FROM project_mini;
```

**分组查询**：按优先级分组统计项目数量

```sql
SELECT priority, COUNT(*) AS count
FROM project_mini
GROUP BY priority;
```

**分页查询**：每页 10 条，查询第 2 页

```sql
SELECT id, name
FROM project_mini
ORDER BY id
LIMIT 10 OFFSET 10;
```

**布尔条件查询**：查询已激活的项目

```sql
SELECT id, name, is_active
FROM project_mini
WHERE is_active = 1;
```

**集合查询**

包含 tag1 标签的项目

```sql
SELECT id, name, tags
FROM project_mini
WHERE FIND_IN_SET('tag1', tags);
```

包含 tag1 或 tag2 标签的项目

```sql
SELECT id, name, tags
FROM project_mini
WHERE FIND_IN_SET('tag1', tags) OR FIND_IN_SET('tag2', tags);
```



## 联表查询（JOIN）

**内连接**：查询项目及其对应的分类名称

仅返回在两张表中均有匹配的记录。

```sql
SELECT p.id, p.name, c.name AS category_name
FROM kongyu.project_mini p
JOIN kongyu.project_category c ON p.category_id = c.id;
```

说明：只有当 `project.category_id` 与 `project_category.id` 匹配时，该项目才会被返回。

------

**左连接**：查询所有项目及其分类名称（无分类则为 NULL）

返回左表（项目表）的所有记录，即使右表无匹配项。

```sql
SELECT p.id, p.name, c.name AS category_name
FROM kongyu.project_mini p
LEFT JOIN kongyu.project_category c ON p.category_id = c.id;
```

说明：当项目未设置分类时，`category_name` 为 `NULL`，但项目仍会被返回。

------

**右连接**：查询所有分类及其下的项目（无项目则为 NULL）

返回右表（分类表）的所有记录，即使左表无匹配项。

```sql
SELECT c.id AS category_id, c.name AS category_name, p.name AS project_name
FROM kongyu.project_mini p
RIGHT JOIN kongyu.project_category c ON p.category_id = c.id;
```

说明：如果某个分类没有关联任何项目，也会被返回，`project_name` 为 `NULL`。

------

**联表筛选**：查询所有“已启用分类”下的项目

只筛选分类表中 `is_enabled = 1` 的记录参与关联。

```sql
SELECT p.id, p.name, c.name AS category_name
FROM kongyu.project_mini p
JOIN kongyu.project_category c ON p.category_id = c.id AND c.is_enabled = 1;
```

说明：只返回“启用的分类”下的项目。

------

**联表分组**：每个分类下项目的数量与总金额

聚合结果基于联表字段。

```sql
SELECT c.name AS category_name, COUNT(p.id) AS project_count, SUM(p.amount) AS total_amount
FROM kongyu.project_mini p
JOIN kongyu.project_category c ON p.category_id = c.id
GROUP BY c.id, c.name;
```

说明：统计每个分类下有多少项目，以及这些项目的金额总和。

------

**子查询联表**：查出“项目最多”的分类及其项目数

通过子查询得到排序后的结果再取第一条。

```sql
SELECT category_name, project_count
FROM (
    SELECT c.name AS category_name, COUNT(p.id) AS project_count
    FROM kongyu.project_mini p
    JOIN kongyu.project_category c ON p.category_id = c.id
    GROUP BY c.id
) AS t
ORDER BY project_count DESC
LIMIT 1;
```

说明：先按分类分组统计项目数，再取数量最多的分类。

------

**多条件连接**：确保项目未删除、分类已启用

```sql
SELECT p.id, p.name, c.name AS category_name
FROM kongyu.project_mini p
JOIN kongyu.project_category c 
  ON p.category_id = c.id AND p.is_deleted = 0 AND c.is_enabled = 1;
```

说明：把多个条件放到 `ON` 子句中，使得只连接符合条件的记录，效率更高。



## 聚合与分组

**多字段分组**：按优先级和状态统计项目数量

```sql
SELECT priority, status, COUNT(*) AS count
FROM kongyu.project
GROUP BY priority, status
ORDER BY priority, status;
```

**时间分组**：按月份统计每月创建的项目数量

```sql
SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count
FROM kongyu.project
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month;
```

**按分类分组统计**：每个分类下项目数量和总金额（联表）

```sql
SELECT c.name AS category_name, COUNT(p.id) AS project_count, SUM(p.amount) AS total_amount
FROM kongyu.project p
JOIN kongyu.project_category c ON p.category_id = c.id
GROUP BY c.id, c.name
ORDER BY total_amount DESC;
```

**复杂聚合**：查询每个状态下的最大金额、平均得分、项目数量

```sql
SELECT status,
       MAX(amount) AS max_amount,
       AVG(score) AS avg_score,
       COUNT(*) AS count
FROM kongyu.project
GROUP BY status;
```

**带条件的分组**：只统计“激活项目”中每个优先级下的项目数量和平均金额

```sql
SELECT priority, COUNT(*) AS count, AVG(amount) AS avg_amount
FROM kongyu.project
WHERE is_active = 1
GROUP BY priority
ORDER BY avg_amount DESC;
```

**分组后筛选（HAVING）**：筛选出平均金额大于 5000 的状态组

```sql
SELECT status, AVG(amount) AS avg_amount
FROM kongyu.project
GROUP BY status
HAVING AVG(amount) > 5000;
```

**子查询聚合**：找出项目数最多的分类名称

```sql
SELECT category_name, project_count
FROM (
    SELECT c.name AS category_name, COUNT(p.id) AS project_count
    FROM kongyu.project p
    JOIN kongyu.project_category c ON p.category_id = c.id
    GROUP BY c.id
) AS t
ORDER BY project_count DESC
LIMIT 1;
```



## 特殊字段查询

### UUID（BINARY）

项目中的 `uuid` 字段为 `BINARY(16)` 类型，用于存储顺序 UUID，无法直接阅读，可通过 `BIN_TO_UUID()` 函数转换。

**查看可读 UUID**

```sql
SELECT id, BIN_TO_UUID(uuid, 1) AS uuid_str
FROM kongyu.project_mini;
```

说明：

- `BIN_TO_UUID(uuid, 1)`：将顺序 UUID 转为标准格式字符串；
- 参数 `1` 表示转换时使用顺序 UUID 模式（兼容 MySQL 的 `UUID_TO_BIN(uuid(), 1)`）；
- 如果未使用顺序 UUID，参数应设为 `0`。

**按 UUID 精确查询**

```sql
SELECT id, name
FROM kongyu.project_mini
WHERE uuid = UUID_TO_BIN('f2090f0e-6ced-11f0-9f6c-d094666ee2ee', 1);
```

说明：必须先使用 `UUID_TO_BIN()` 将字符串形式的 UUID 转为二进制，再进行匹配。

------

### IP 地址（VARBINARY）

`ip_address` 字段为 `VARBINARY(16)`，支持存储 **IPv4 与 IPv6 地址**。MySQL 提供专用函数用于**查询、显示、比较和计算**。

------

**显示为标准 IP 字符串**

```sql
SELECT id, INET6_NTOA(ip_address) AS ip_text
FROM kongyu.project_mini;
```

说明：

- `INET6_NTOA()`：将 `VARBINARY(16)` 格式的地址转为字符串形式（如 `192.168.1.1` 或 `ccd2:5069:1a06:e7e3:e150:ad23:6de8:70ae`）；
- 推荐用于结果展示，调试查看。

------

**查询指定 IP 的项目**

```sql
SELECT id, name
FROM kongyu.project_mini
WHERE ip_address = INET6_ATON('86.176.119.4');
```

说明：

- `INET6_ATON()`：将字符串 IP 地址转为 `VARBINARY(16)`；
- 适用于 IPv4 或 IPv6；
- `= INET6_ATON(...)` 用于匹配精确 IP 地址。

------

### 坐标位置（GEOMETRY）

`location` 字段为 `GEOMETRY` 类型，实际使用中常为 `POINT`（经纬度坐标），MySQL 提供相关函数用于读取。

**读取经纬度坐标**

```sql
SELECT id,
       ST_X(location) AS longitude,
       ST_Y(location) AS latitude
FROM kongyu.project_mini
WHERE location IS NOT NULL;
```

说明：

- `ST_X()` 获取经度（X 坐标）；
- `ST_Y()` 获取纬度（Y 坐标）；
- 可用于地图展示或距离计算等地理功能。

**输出为 GeoJSON 格式**

```sql
SELECT id, ST_AsGeoJSON(location) AS geojson
FROM kongyu.project_mini
WHERE location IS NOT NULL;
```

说明：

- `ST_AsGeoJSON()`：将 `GEOMETRY` 类型转换为标准 [GeoJSON](https://geojson.org/) 格式；

- 返回结果例如：

  ```json
  { "type": "Point", "coordinates": [120.123, 30.456] }
  ```

- 适用于地图系统、前端可视化（如 Leaflet、Mapbox、OpenLayers）。

------

**输出为 WKT 格式**

```sql
SELECT id, ST_AsText(location) AS wkt
FROM kongyu.project_mini
WHERE location IS NOT NULL;
```

**说明：**

- `ST_AsText()`：将 `GEOMETRY` 类型转换为标准 [WKT](https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry)（Well-Known Text）格式；

- 返回结果例如：

  ```text
  POINT(31.001 121.001)
  ```

- ⚠️ 注意坐标顺序：WKT 中为 **纬度 经度**（即 POINT(**纬度** **经度**)），与 GeoJSON 相反；

- 常用于后端代码中生成 `ST_GeomFromText()` 查询、空间分析、GIS 工具（如 QGIS、PostGIS）等。

------

**筛选存在地理坐标的记录**

```sql
SELECT id, name
FROM kongyu.project_mini
WHERE MBRContains(
    ST_GeomFromText(
        'POLYGON((5.1510527 -103.74596, 30 121, 31 121, 31 120, 5.1510527 -103.74596))',
        4326
    ),
    location
);
```

说明：在特定经纬度范围（矩形）内查找项目。

------

**查询某点周围 5 公里范围内的项目**

```sql
SELECT id, name,
       ST_Distance_Sphere(location, ST_GeomFromText('POINT(5.1510527 -103.74596)', 4326)) AS distance_meters
FROM kongyu.project_mini
WHERE ST_Distance_Sphere(location, ST_GeomFromText('POINT(5.1510527 -103.74596)', 4326)) <= 5000000
ORDER BY distance_meters ASC;
```

说明：

- `ST_Distance_Sphere(a, b)`：计算两个点之间的球面距离（单位：米）；
- `ST_GeomFromText('POINT(...)')`：创建用于查询的点（经度在前）；
- 本例中是查找以 `POINT(120.123 30.456)` 为中心 **5000000 公里内的项目**。

------

### JSON 对象/数组

项目中存在两个字段 `json_object` 和 `json_array`，均为 MySQL 原生 JSON 类型，可用 `->`、`->>`、`JSON_CONTAINS()` 等函数解析。

**提取对象中的字段值**

```sql
SELECT id, json_object->>'$.name' AS name
FROM kongyu.project_mini;
```

说明：

- `->>` 提取字符串值（如 `"张三"`）；
- `->` 提取 JSON 值（仍为 JSON 类型）。

**提取嵌套对象字段**

```sql
SELECT json_object->'$.user.info.name' AS user_name
FROM kongyu.project_mini;
```

说明：支持多级路径访问嵌套字段。

**判断数组是否包含某个值**

```sql
-- 查询 List<String> 的情况
SELECT id, name
FROM kongyu.project_mini
WHERE JSON_CONTAINS(json_array, '\"admin\"');

-- 查询对象的情况
SELECT id, name
FROM kongyu.project_mini
WHERE JSON_CONTAINS(json_array, '{"@type":"local.ateng.java.mybatisjdk8.entity.MyData","address":"Suite 300 毛中心01号, 温州, 陕 282479","dateTime":"2024-11-12 14:49:16.309815200","id":9,"name":"萧博文","salary":17675.94,"score":72.11}');
```

说明：

- 用于判断 JSON 数组中是否包含指定值；
- 参数需为有效 JSON 格式字符串（字符串元素要加 `\"` 转义）。

**读取数组中的某个位置元素**

```sql
SELECT json_array->'$[0]' AS first_element
FROM kongyu.project_mini;
```

说明：可按索引读取数组元素。

**读取数组中的全部位置元素的值**

```sql
SELECT json_array->>'$[*].name' AS first_element
FROM kongyu.project_mini;
```



## 时间函数

**获取当前时间**

```sql
SELECT NOW();         -- 当前日期+时间（带时分秒）
SELECT CURDATE();     -- 当前日期（仅年月日）
SELECT CURTIME();     -- 当前时间（仅时分秒）
```

> 说明：适用于插入创建时间、更新时间字段等。

------

**日期格式化（DATE_FORMAT）**

```sql
SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') AS formatted_time;
```

- `%Y` 年，`%m` 月，`%d` 日
- `%H` 时（24小时制），`%i` 分，`%s` 秒

> 说明：用于格式化日期显示，适用于日志展示、报表导出等场景。

------

**日期加减（DATE_ADD / DATE_SUB）**

```sql
-- 当前时间加7天
SELECT DATE_ADD(NOW(), INTERVAL 7 DAY);

-- 当前时间减3个月
SELECT DATE_SUB(NOW(), INTERVAL 3 MONTH);
```

> 说明：用于查询某一时间段内的数据、过期时间计算等。

------

**计算两个时间的差值（TIMESTAMPDIFF）**

```sql
-- 单位为天
SELECT TIMESTAMPDIFF(DAY, '2025-01-01', NOW()) AS days_diff;

-- 单位为分钟
SELECT TIMESTAMPDIFF(MINUTE, '2025-07-30 08:00:00', '2025-07-30 10:30:00') AS minutes_diff;
```

> 说明：常用于计算任务耗时、用户注册时间距今多少天等场景。

------

**时间截取（YEAR、MONTH、DAY、HOUR、MINUTE、SECOND）**

```sql
SELECT
  YEAR(NOW())   AS year,
  MONTH(NOW())  AS month,
  DAY(NOW())    AS day,
  HOUR(NOW())   AS hour;
```

> 说明：用于数据分组、报表分年/季度/月统计等。

------

**获取某月的第一天 / 最后一天**

```sql
-- 当前月第一天
SELECT DATE_FORMAT(CURDATE(), '%Y-%m-01') AS first_day;

-- 当前月最后一天
SELECT LAST_DAY(NOW()) AS last_day;
```

> 说明：适用于月度数据查询、月报汇总等。

------

**判断某时间是否在范围内（BETWEEN）**

```sql
SELECT id, name, created_at
FROM kongyu.project
WHERE created_at BETWEEN '2025-07-01' AND '2025-07-30';
```

> 说明：常用于筛选时间段内创建的数据。

------

**将字符串转为日期（STR_TO_DATE）**

```sql
SELECT STR_TO_DATE('2025-07-30 15:30:00', '%Y-%m-%d %H:%i:%s');
```

> 说明：用于导入数据时解析字符串时间。

