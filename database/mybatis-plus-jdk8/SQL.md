# MySQL

## 数据准备

### 创建表

#### project

创建后下载 [数据](https://github.com/atengk/Ateng-Java/releases/download/v1.0/project_data.sql.zip)，将数据导入表中

```sql
create table if not exists project
(
    id          int auto_increment comment '主键，自增ID'
        primary key,
    category_id int          default 1                        null comment '项目分类ID，关联 project_category(id)',
    uuid        binary(16)   default (uuid_to_bin(uuid(), 1)) not null comment '顺序UUID，用于替代ID，全局唯一',
    name        varchar(100)                                  not null comment '名称，最大100字符',
    code        varchar(50)                                   null comment '项目编号，唯一逻辑标识',
    description text                                          null comment '描述，可以存储较长的文本',
    amount      decimal(10, 2)                                null comment '金额，最多10位，小数2位',
    score       float                                         null comment '浮动分数',
    balance     bigint                                        null comment '账户余额，支持大整数',
    tags        set ('tag1', 'tag2', 'tag3', 'tag4')          null comment '标签集合',
    priority    enum ('low', 'medium', 'high')                null comment '优先级',
    status      tinyint      default 0                        null comment '状态：0=草稿, 1=进行中, 2=已完成, 3=已取消',
    is_active   tinyint(1)   default 1                        null comment '是否激活',
    is_deleted  tinyint(1)   default 0                        null comment '是否已删除，逻辑删除标志',
    version     int          default 1                        null comment '版本号，用于乐观锁',
    user_count  int unsigned default '0'                      null comment '用户数量，正整数',
    birth_date  date                                          null comment '出生日期',
    last_login  time                                          null comment '最后登录时间',
    start_date  datetime                                      null comment '开始日期',
    end_date    datetime                                      null comment '结束日期',
    region      varchar(100)                                  null comment '地区名称',
    file_path   varchar(255)                                  null comment '文件路径，例如上传到OSS的路径',
    json_object json                                          null comment 'JSON对象类型数据',
    json_array  json                                          null comment 'JSON数组类型数据',
    location    geometry                                      null comment '地理坐标（经纬度）',
    ip_address  varbinary(16)                                 null comment 'IP地址，支持IPv6',
    binary_data blob                                          null comment '二进制大数据字段',
    created_at  datetime     default CURRENT_TIMESTAMP        null comment '创建时间',
    updated_at  timestamp    default CURRENT_TIMESTAMP        null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '项目表，包含常用字段类型' charset = utf8mb4;
```

创建project_mini表，方便后续进行数据查询

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

#### project_category

```sql
create table if not exists kongyu.project_category
(
    id          int auto_increment comment '主键，自增ID'
        primary key,
    name        varchar(100)                         not null comment '分类名称，如：政府项目、内部项目',
    code        varchar(50)                          not null comment '分类编码，逻辑唯一标识，如 GOV, INTERNAL',
    description varchar(255)                         null comment '分类描述',
    sort_order  int        default 0                 null comment '排序值，越小越靠前',
    is_enabled  tinyint(1) default 1                 null comment '是否启用：1=启用，0=禁用',
    created_at  datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at  timestamp  default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_project_category_code
        unique (code)
)
    comment '项目分类表，用于给项目做分类管理' charset = utf8mb4;
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
select NOW() + INTERVAL 7 DAY;

-- 当前时间减3个月
SELECT DATE_SUB(NOW(), INTERVAL 3 MONTH);
select NOW() - INTERVAL 7 MONTH;
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

**时间截取（YEAR、MONTH、DAY、HOUR、MINUTE、SECOND、DAYOFWEEK）**

```sql
SELECT
  YEAR(NOW())   AS year,
  MONTH(NOW())  AS month,
  DAY(NOW())    AS day,
  HOUR(NOW())   AS hour;

SELECT DATE(NOW());     -- 将日期时间字段转换成日期（年月日）
SELECT TIME(NOW());     -- 将日期时间字段转换成时间（时分秒）
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



## 字符串函数

------

**字符串拼接**

```sql
SELECT CONCAT('项目名称：', name) AS full_name
FROM kongyu.project_mini;
```

- `CONCAT()`：拼接多个字符串；
- 也可拼接字段值：`CONCAT(name, '（', code, '）')`

------

**子串截取**

```sql
SELECT SUBSTRING(name, 1, 5) AS short_name
FROM kongyu.project_mini;
```

- `SUBSTRING(str, pos, len)`：从第 `pos` 个字符开始，截取 `len` 长度的字符串；
- 也支持负数索引。

------

**字符串替换**

```sql
SELECT REPLACE(name, '智慧', '智能') AS replaced_name
FROM kongyu.project_mini;
```

- 将 `name` 中的 `"智慧"` 替换为 `"智能"`。

------

**字符串查找**

```sql
SELECT INSTR(name, '园区') AS position
FROM kongyu.project_mini;
```

- `INSTR(str, substr)`：返回 `substr` 在 `str` 中首次出现的位置；
- 没找到返回 `0`。

------

**字符串长度**

```sql
SELECT name, CHAR_LENGTH(name) AS char_len, LENGTH(name) AS byte_len
FROM kongyu.project_mini;
```

- `CHAR_LENGTH()`：返回字符数；
- `LENGTH()`：返回字节数（如中文返回 3 个字节/字符）。

------

**大小写转换**

```sql
SELECT UPPER(name) AS upper_name, LOWER(code) AS lower_code
FROM kongyu.project_mini;
```

- `UPPER()`：转为大写；
- `LOWER()`：转为小写。

------

**去除空格**

```sql
SELECT TRIM(name) AS trimmed_name
FROM kongyu.project_mini;
```

- `TRIM()`：去除前后空格；
- 也可使用 `LTRIM()` / `RTRIM()` 单独去除左/右空格。

------

**字段填充**

```sql
SELECT LPAD(code, 12, '0') AS padded_code
FROM kongyu.project_mini;
```

- `LPAD(str, len, pad_str)`：左侧填充；
- `RPAD()`：右侧填充。
- 填充后总计 12 个字符，为 Null 的数据不会填充

------

**格式处理（数字转字符串）**

```sql
SELECT FORMAT(1234567.89123, 2) AS formatted;
```

- 输出结果：1,234,567.89

- 保留两位小数，并自动加上千分位逗号。

- 默认使用 当前语言环境的千位分隔符和小数点格式（通常是英文）。

```sql
SELECT id, name, amount, FORMAT(amount, 3) AS formatted_amount
FROM kongyu.project_mini;
```

- 将 `DECIMAL` 或 `DOUBLE` 类型格式化为字符串，保留两位小数；
- 常用于报表、导出等场景。

------

**正则提取（高级用法）**

```sql
SELECT REGEXP_SUBSTR(name, '[0-9]+') AS first_number
FROM kongyu.project_mini;
```

- 从字符串中提取第一个数字；
- MySQL 8.0+ 支持 `REGEXP_SUBSTR()` / `REGEXP_REPLACE()` 等正则函数。



## 数值函数

------

**ROUND() - 四舍五入**

```sql
SELECT ROUND(123.4567, 2);  -- 结果：123.46
SELECT ROUND(123.4567, 0);  -- 结果：123
SELECT ROUND(123.4567, -1); -- 结果：120
```

说明：

- 第二个参数为保留的小数位数；
- 如果为负数，则对整数部分四舍五入；
- 常用于金额、税率、百分比等展示精度控制。

------

**TRUNCATE() - 截断小数位（不四舍五入）**

```sql
SELECT TRUNCATE(123.4567, 2);  -- 结果：123.45
SELECT TRUNCATE(123.4567, 0);  -- 结果：123
SELECT TRUNCATE(123.4567, -1); -- 结果：120
```

说明：

- 与 `ROUND()` 不同，`TRUNCATE` 会直接截断；
- 多用于数值精度对比、统计分析保守处理场景。

------

**CEIL() / FLOOR() - 向上/向下取整**

```sql
SELECT CEIL(123.001);   -- 结果：124
SELECT CEIL(-123.001);  -- 结果：-123

SELECT FLOOR(123.999);  -- 结果：123
SELECT FLOOR(-123.999); -- 结果：-124
```

说明：

- `CEIL()`：始终向上取整；
- `FLOOR()`：始终向下取整；
- 用于分页计算、商品库存控制、积分系统等。

------

**ABS() - 绝对值**

```sql
SELECT ABS(-123.45);   -- 结果：123.45
SELECT ABS(99);        -- 结果：99
```

说明：

- 返回数值的绝对值；
- 多用于计算差值、偏移量、距离等。

------

**MOD() - 取模（余数）**

```sql
SELECT MOD(10, 3);     -- 结果：1
SELECT 10 % 3;         -- 结果：1
```

说明：

- 结果为 x 除以 y 的余数；
- 常用于分组编号、奇偶判断、定期计算等。

------

**SIGN() - 获取符号**

```sql
SELECT SIGN(10);     -- 结果：1
SELECT SIGN(-5);     -- 结果：-1
SELECT SIGN(0);      -- 结果：0
```

说明：

- 判断数值正负性；
- 可用于排序方向、计算差异趋势等。

------

**RAND() - 生成随机数**

```sql
SELECT RAND();              -- 结果：0~1之间的随机浮点数
SELECT ROUND(RAND()*100);  -- 结果：0~100 的整数
SELECT RAND(100);          -- 指定种子，生成可重复值
```

说明：

- 通常用于抽样、生成验证码、测试数据等；
- `RAND(seed)` 可生成固定种子的伪随机结果。

------

**GREATEST() / LEAST() - 多个值的最大/最小值**

```sql
SELECT GREATEST(10, 20, 5);  -- 结果：20
SELECT LEAST(10, 20, 5);     -- 结果：5
```

说明：

- 计算多列/多值中的最大或最小值；
- 适用于条件判断、值合并、统计分析等。

------

**POWER(x, y) - 幂运算**

```sql
SELECT POWER(2, 3);    -- 结果：8
SELECT POWER(9, 0.5);  -- 结果：3（相当于开平方）
```

说明：

- 返回 `x` 的 `y` 次幂；
- 用于风险模型评分、指数增长等场景。

------

**SQRT(x) - 开平方**

```sql
SELECT SQRT(16);   -- 结果：4
SELECT SQRT(2);    -- 结果：1.4142...
```

说明：

- 求平方根（√x）；
- 常用于几何计算、坐标距离等。

------

**LOG(x) / LOG10(x) / LN(x) - 对数函数**

```sql
SELECT LOG(10);       -- 结果：2.3025（以e为底）
SELECT LOG(100, 10);  -- 结果：2（以10为底）
SELECT LOG10(1000);   -- 结果：3（常用10进制对数）
SELECT LN(10);        -- 等同于 LOG(10)
```

说明：

- `LOG(x)` 默认以自然常数 `e` 为底；
- `LOG(b, x)` 表示以 `b` 为底的对数；
- 用于计算增长率、信息熵、权重等。

------

**EXP(x) - 指数函数**

```sql
SELECT EXP(1);     -- 结果：2.71828（自然常数 e）
SELECT EXP(2);     -- 结果：7.38905
```

说明：

- 返回 `e^x` 的值；
- 常用于复利计算、指数建模。

------

**PI() - 圆周率常量**

```sql
SELECT PI();           -- 结果：3.141593
SELECT ROUND(PI(), 2); -- 结果：3.14
```

说明：

- 返回圆周率常数 π；
- 用于坐标系统、角度计算等。

------

**SIN(x), COS(x), TAN(x) - 三角函数**

```sql
SELECT SIN(PI()/2);   -- 结果：1
SELECT COS(0);        -- 结果：1
SELECT TAN(PI()/4);   -- 结果：1
```

说明：

- 参数为弧度制；
- 可用于地图角度、导航路径、波形建模等。

------

**DEGREES() / RADIANS() - 角度与弧度转换**

```sql
SELECT DEGREES(PI());      -- 结果：180
SELECT RADIANS(180);       -- 结果：3.14159
```

说明：

- `RADIANS()`：角度 → 弧度；
- `DEGREES()`：弧度 → 角度；
- 多用于 `ST_Distance_Sphere()`、`SIN()` 等计算时格式转换。

------

**CONV(x, from_base, to_base) - 进制转换**

```sql
SELECT CONV('A', 16, 10);   -- 结果：10（十六进制转十进制）
SELECT CONV(10, 10, 2);     -- 结果：1010（二进制）
SELECT CONV('1010', 2, 10); -- 结果：10（反向）
```

说明：

- 将字符串形式的数字从一个进制转换为另一个进制；
- 支持 2~36 进制；
- 常用于编码解码、唯一标识转换等。

------

**BIT_LENGTH(x) / CHAR_LENGTH(x) - 位数与字符数**

```sql
SELECT BIT_LENGTH('abc');   -- 结果：24（3字符 × 8位）
SELECT CHAR_LENGTH('abc');  -- 结果：3
```

说明：

- `BIT_LENGTH()`：返回字节位数（byte × 8）；
- `CHAR_LENGTH()`：返回字符长度；
- 多用于字段长度校验、编码问题处理等。



## 类型转换函数

类型转换函数用于将不同数据类型之间进行显式或隐式的转换，例如：字符串转数字、日期转字符串等，常用于数据清洗、格式统一、导入导出等场景。

------

**CAST(expr AS type) - 显式类型转换**

```sql
SELECT CAST('2025-07-30' AS DATE);         -- 字符串转日期
SELECT CAST(1 AS CHAR);                    -- 数字转字符串
SELECT CAST(12.34 AS SIGNED);              -- 转为整数：12
SELECT CAST(99 AS DECIMAL(5, 2));          -- 转为浮点数：99.00
```

说明：

- 支持转换为：`CHAR`、`BINARY`、`SIGNED`、`UNSIGNED`、`DECIMAL(p,s)`、`DATE`、`DATETIME`、`TIME` 等；
- 更加标准、兼容性强，推荐使用。

------

**CONVERT(expr, type) - 与 CAST 等效**

```sql
SELECT CONVERT('123', SIGNED);             -- 结果：123（字符串转整数）
SELECT CONVERT(456, CHAR);                 -- 结果：'456'（整数转字符串）
```

说明：

- 与 `CAST()` 功能相同，仅语法不同；
- 推荐用 `CAST()`，更具可读性；
- `CONVERT(expr USING charset)` 则用于字符集转换（下节详述）。

------

**CHAR() - 数值转 ASCII 字符**

```sql
SELECT CHAR(65);       -- 结果：'A'
SELECT CHAR(65,66,67); -- 结果：'ABC'
```

说明：

- 适用于 ASCII 编码处理、字符构造、协议处理。

------

**BIN(), HEX(), OCT() - 数值转进制字符串**

```sql
SELECT BIN(10);    -- 结果：'1010'（二进制）
SELECT OCT(10);    -- 结果：'12'（八进制）
SELECT HEX(255);   -- 结果：'FF'（十六进制）
```

说明：

- 常用于调试、编码转换、加密、底层数据表示。

------

**UNHEX(str) - 十六进制字符串转字节串**

```sql
SELECT UNHEX('4D7953514C');   -- 结果：'MySQL'（十六进制 → 字符串）
```

说明：

- 与 `HEX()` 相反；
- 多用于存储 BINARY 数据、UUID 字节序转换等。

------

**DATE_FORMAT(date, format) - 日期 → 字符串**

```sql
SELECT DATE_FORMAT(NOW(), '%Y-%m-%d');        -- 结果：'2025-07-30'
SELECT DATE_FORMAT('2025-07-30 15:20:00', '%H:%i');  -- 结果：'15:20'
```

说明：

- 适用于时间字段展示、日志、报表等；
- 格式符如 `%Y/%m/%d %H:%i:%s` 等详见时间函数章节。

------

**STR_TO_DATE(str, format) - 字符串 → 日期**

```sql
SELECT STR_TO_DATE('2025-07-30', '%Y-%m-%d');     -- 结果：2025-07-30
SELECT STR_TO_DATE('30/07/2025 14:00', '%d/%m/%Y %H:%i');
```

说明：

- 与 `DATE_FORMAT` 相反；
- 常用于导入 CSV、用户输入转时间。

------

**IF(expr IS NULL, alt, expr) - NULL 类型处理**

```sql
SELECT IF(NULL IS NULL, '默认值', '原值');   -- 结果：'默认值'
```

说明：

- 对 `NULL` 值进行安全转换；
- 替代空值时常与 `CAST()` 等函数组合使用。

------



## 条件判断函数

MySQL 中常用的条件判断函数适用于字段值判断、分支逻辑处理、数据分组替换、缺失值补充等场景。

------

**IF 条件表达式**

```sql
SELECT 
    name,
    IF(status = 1, '进行中', '其他') AS status_label
FROM kongyu.project_mini;
```

**说明：**

- `IF(condition, true_value, false_value)`：条件成立返回第一个值，否则返回第二个；
- 适用于简单条件逻辑。

------

**IFNULL：为空时返回默认值**

```sql
SELECT 
    name,
    IFNULL(description, '暂无说明') AS description
FROM kongyu.project_mini;
```

**说明：**

- `IFNULL(expr1, expr2)`：若 `expr1` 为 `NULL`，返回 `expr2`；
- 用于字段值缺失的情况。

------

**NULLIF：两个值相等时返回 NULL**

```sql
SELECT 
    name,
    NULLIF(status, 0) AS real_status
FROM kongyu.project_mini;
```

**说明：**

- `NULLIF(a, b)`：若 `a = b`，返回 `NULL`，否则返回 `a`；
- 适用于防止除 0 错误、过滤无效值等场景。

------

**COALESCE：返回第一个非 NULL 值**

```sql
SELECT 
    name,
    COALESCE(short_name, alias, name) AS display_name
FROM kongyu.project_mini;
```

**说明：**

- `COALESCE(a, b, c, ...)`：返回从左到右第一个非 `NULL` 的值；
- 适用于多字段候选取值场景，如显示名称、地址优先级等。

------

**ISNULL(expr)：判断是否为 NULL（返回 1 或 0）**

```sql
SELECT 
    name,
    ISNULL(updated_at) AS is_never_updated
FROM kongyu.project_mini;
```

**说明：**

- `ISNULL(expr)`：为 `NULL` 返回 1，否则返回 0；
- 常用于统计、分组过滤等场景。

---

**CASE 表达式**

```sql
SELECT 
    name,
    CASE status
        WHEN 0 THEN '未开始'
        WHEN 1 THEN '进行中'
        WHEN 2 THEN '已完成'
        ELSE '未知状态'
    END AS status_label
FROM kongyu.project_mini;
```

**说明：**

- 类似 Java 或其他语言的 switch-case；
- 用于多条件分支判断，灵活清晰。

------

**CASE + 范围判断**

```sql
SELECT 
    name,
    CASE 
        WHEN amount >= 10000 THEN '高额'
        WHEN amount >= 5000 THEN '中额'
        ELSE '低额'
    END AS amount_level
FROM kongyu.project_mini;
```

**说明：**

- `CASE WHEN ... THEN ...`：适用于多段范围判断；
- 常见于评分分级、金额分档、等级分类等。

------

**CASE 多条件组合字段**

```sql
SELECT 
    name,
    CASE 
        WHEN status = 1 AND amount > 5000 THEN '重点在建项目'
        WHEN status = 1 THEN '一般在建项目'
        ELSE '非在建项目'
    END AS project_level
FROM kongyu.project_mini;
```

**CASE 与 ISNULL 进行状态判断**

```sql
SELECT 
    name,
    CASE 
        WHEN ISNULL(updated_at) THEN '从未修改'
        ELSE '已更新'
    END AS update_status
FROM kongyu.project_mini;
```



## 窗口函数

窗口函数是一类在**不聚合整组数据的前提下**进行排序、分组、累计等分析操作的函数，常用于排名、累计、分区内排序等操作。

格式一般如下：

```sql
<窗口函数> OVER (
    [PARTITION BY partition_expr]
    [ORDER BY order_expr]
    [ROWS BETWEEN x PRECEDING AND y FOLLOWING]
)
```

1. **`PARTITION BY`**
    将结果集按指定字段分区（类似 `GROUP BY`），每个分区内独立计算窗口函数。
    可选项。
2. **`ORDER BY`**
    指定分区内的排序方式。窗口函数计算通常依赖顺序。
    多数窗口函数需要指定。
3. **`ROWS BETWEEN ... AND ...`**
    定义“窗口”的行范围。常见用法有：
   - `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`
      从分区内第一行到当前行（累积）。
   - `ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING`
      以当前行为中心，前后各取一行（滑动窗口）。

**常见窗口函数分类**

| 类别     | 函数                                                 | 说明                             |
| -------- | ---------------------------------------------------- | -------------------------------- |
| 聚合函数 | `SUM()`, `AVG()`, `MAX()`, `MIN()`, `COUNT()`        | 可作为窗口函数使用，不影响其他列 |
| 排序排名 | `ROW_NUMBER()`, `RANK()`, `DENSE_RANK()`, `NTILE(n)` | 用于排序和排名                   |
| 位移函数 | `LEAD()`, `LAG()`, `FIRST_VALUE()`, `LAST_VALUE()`   | 用于访问窗口内其他行的值         |
| 分析函数 | `CUME_DIST()`, `PERCENT_RANK()`                      | 用于分布计算                     |

------

**排名函数**

ROW_NUMBER()：全表/每组内唯一编号

```sql
SELECT id, name, amount,
       ROW_NUMBER() OVER (ORDER BY amount DESC) AS row_num
FROM kongyu.project;
```

说明：

- 为每行分配一个唯一序号；
- 通常用于分页、去重等场景。

------

RANK() / DENSE_RANK()：排名（支持并列名次）

```sql
SELECT id, name, amount,
       RANK() OVER (ORDER BY amount DESC) AS rank_val,
       DENSE_RANK() OVER (ORDER BY amount DESC) AS dense_rank_val
FROM kongyu.project;
```

说明：

- `RANK()` 遇到并列名次会跳过后续编号；
- `DENSE_RANK()` 则会连续编号；
- 适合排行榜、名次统计等。

------

**累计/聚合窗口**

SUM() / AVG() / COUNT() OVER(...)

```sql
SELECT id, name, region, amount,
       SUM(amount) OVER (PARTITION BY region) AS total_amount_per_region
FROM kongyu.project;
```

说明：

- 可按某字段分区，统计累计值；
- 不影响原始行展示，适合报表分析。

------

累计总和（按时间累计）

```sql
SELECT id, name, created_at, amount,
       SUM(amount) OVER (ORDER BY created_at) AS running_total
FROM kongyu.project;
```

说明：

- 随时间推移的累计值，如日收入、用户增长。

------

**前一行/后一行值**

LAG() / LEAD()

```sql
SELECT id, name, amount,
       LAG(amount) OVER (ORDER BY id) AS prev_amount,
       LEAD(amount) OVER (ORDER BY id) AS next_amount
FROM kongyu.project;
```

说明：

- `LAG()` 获取前一行字段值；
- `LEAD()` 获取后一行字段值；
- 可用于计算环比、变化趋势等。

------

**首尾值**

FIRST_VALUE() / LAST_VALUE()

```sql
SELECT id, name, created_at, amount,
       FIRST_VALUE(amount) OVER (PARTITION BY region ORDER BY created_at) AS first_amt,
       LAST_VALUE(amount) OVER (PARTITION BY region ORDER BY created_at ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_amt
FROM kongyu.project;
```

说明：

- 提取分组内的首行/尾行字段值；
- 注意 `LAST_VALUE()` 需加完整窗口范围，否则仅取当前行。

------

**Top-N 分组取前几名**

> 每个分组中，按照某字段排序，取前几条记录（如：每个地区销售额前 3 的项目）

```sql
SELECT *
FROM (
    SELECT id, name, region, amount,
           ROW_NUMBER() OVER (PARTITION BY region ORDER BY amount DESC) AS rn
    FROM kongyu.project
) t
WHERE t.rn <= 3;
```

说明：

- `PARTITION BY region` 表示按区域分组；
- `ROW_NUMBER()` 给每个分组内编号；
- 外层通过 `WHERE` 过滤出前 N 行；
- 适用于：Top 销量、Top 评论数等。

------

**分组内累计占比（百分比分析）**

> 计算每个项目在其区域内销售额的占比（百分比）

```sql
SELECT id, name, region, amount,
       ROUND(amount / SUM(amount) OVER (PARTITION BY region), 4) AS percent_in_region
FROM kongyu.project;
```

说明：

- 用 `SUM(...) OVER(PARTITION BY ...)` 得到分组总值；
- 单个值除以总值，即得占比；
- 可用于地区占比、品类占比分析等。

------

**按日累计数量 / 金额（带日期窗口）**

> 按天统计用户增长 / 收入累计值

```sql
SELECT DATE(created_at) AS day, user_id,
       COUNT(*) OVER (ORDER BY created_at ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS total_users
FROM kongyu.user_log;
```

说明：

- 每天新增用户统计；
- `ROWS BETWEEN ...` 显式定义窗口范围为前所有行。

------

**与聚合函数混用（累计、平均与总数）**

> 结合普通聚合统计与窗口函数使用

```sql
SELECT name, region, amount,
       AVG(amount) OVER (PARTITION BY region) AS avg_amount,
       COUNT(*) OVER (PARTITION BY region) AS region_project_count
FROM kongyu.project;
```

说明：

- 既保留每行明细，又展示该行所在组的统计信息；
- 适合数据分析场景中 “既看个体，又看整体”。

------

**分组内找增长最快的一行（同比/环比分析）**

```sql
SELECT *
FROM (
    SELECT id, name, created_at, amount,
           amount - LAG(amount) OVER (PARTITION BY name ORDER BY created_at) AS diff
    FROM kongyu.project
) t
WHERE diff IS NOT NULL
ORDER BY diff DESC
LIMIT 5;
```

说明：

- 用 `LAG()` 获取前值，计算差值；
- 筛选增长最多的几行（比如销售暴增）；

------

**复杂分页（每个用户取最新一条记录）**

```sql
SELECT *
FROM (
    SELECT id, user_id, action, created_at,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) AS rn
    FROM kongyu.user_log
) t
WHERE rn = 1;
```

说明：

- `ROW_NUMBER()` 保证每个用户仅保留最新操作；
- 常用于“最新一条登录记录”、“最后一次下单记录”等。



## 分页与子查询

### **分页查询（LIMIT 与 OFFSET）**

> 用于从查询结果中截取部分数据，实现数据分页展示，如常见的列表页。

```sql
SELECT id, name, created_at
FROM kongyu.project
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;
```

说明：

- `LIMIT n`：限制返回最多 n 条数据；

- `OFFSET m`：跳过前 m 条数据，配合 LIMIT 实现分页；

- 通常第 `page` 页的数据是：

  ```sql
  LIMIT pageSize OFFSET (page - 1) * pageSize
  ```

**示例：获取第 3 页，每页 5 条数据**

```sql
SELECT id, name
FROM kongyu.project
ORDER BY created_at DESC
LIMIT 5 OFFSET 10;
```

**简写形式（MySQL 支持）**

```sql
SELECT ... LIMIT 10, 5;
-- 等价于 OFFSET 10 LIMIT 5，即第 3 页
```

------

### **子查询（Subquery）**

子查询是在 SQL 语句中嵌套的另一条查询语句，常用于复杂筛选逻辑。

**1. 子查询作为筛选条件**

```sql
SELECT id, name
FROM kongyu.project
WHERE id IN (
    SELECT project_id FROM kongyu.task WHERE status = 'pending'
);
```

说明：

- 外层查询 `project` 表；
- 子查询返回 `task` 表中处于待办状态的项目 ID。

**2. 子查询作为字段值**

```sql
SELECT id, name,
    (SELECT COUNT(*) FROM kongyu.task t WHERE t.project_id = p.id) AS task_count
FROM kongyu.project p;
```

说明：

- 为每个项目计算其关联的任务数量；
- 子查询用作列字段返回值。

**3. 子查询作为临时表（FROM 子句中）**

```sql
SELECT project_id, COUNT(*) AS count
FROM (
    SELECT project_id FROM kongyu.task WHERE status = 'pending'
) AS t
GROUP BY project_id;
```

说明：

- 子查询结果作为一个临时表参与聚合计算；
- 可与 `JOIN` 等结合构建复杂逻辑。

------

### **相关子查询（Correlated Subquery）**

> 子查询引用了外部查询的列，每次迭代主查询时都要执行一次子查询。

```sql
SELECT id, name
FROM kongyu.project p
WHERE EXISTS (
    SELECT 1 FROM kongyu.task t
    WHERE t.project_id = p.id AND t.status = 'pending'
);
```

说明：

- 每条 `project` 记录执行一次子查询；
- `EXISTS` 检查是否有匹配行返回，常用于高性能存在性判断。

------

### **EXISTS / NOT EXISTS 的使用**

> 用于判断子查询是否返回数据，返回 `TRUE` 则包含该行。通常用于存在性判断，比 `IN` 更适合大数据量过滤。

**1. 使用 EXISTS 判断是否存在关联数据**

```sql
SELECT id, name
FROM kongyu.project p
WHERE EXISTS (
    SELECT 1 FROM kongyu.task t
    WHERE t.project_id = p.id AND t.status = 'done'
);
```

说明：

- 若某个 `project` 存在 `status = 'done'` 的 `task`，则返回；
- `EXISTS` 只关心是否存在，不会返回子查询数据；
- `SELECT 1` 是习惯写法，可省资源。

**2. 使用 NOT EXISTS 判断不存在**

```sql
SELECT id, name
FROM kongyu.project p
WHERE NOT EXISTS (
    SELECT 1 FROM kongyu.task t
    WHERE t.project_id = p.id
);
```

说明：

- 查询**没有任何任务**的项目；
- 相当于反向过滤；
- 效率高于 `NOT IN`，后者在遇到 `NULL` 值时可能出现逻辑问题。

------

### **WITH（CTE）公共表表达式（MySQL 8+）**

> `WITH` 表达式用于给子查询命名，提升可读性，支持递归查询。

**1. 简单 CTE 示例**

```sql
WITH recent_tasks AS (
    SELECT id, name, project_id
    FROM kongyu.task
    WHERE created_at > NOW() - INTERVAL 7 DAY
)
SELECT p.id, p.name, t.name AS task_name
FROM kongyu.project p
JOIN recent_tasks t ON p.id = t.project_id;
```

说明：

- `recent_tasks` 是一个临时命名结果集；
- 可以像普通表一样进行 `JOIN`、筛选；
- 比嵌套子查询更清晰，方便维护。

**2. 多个 CTE**

```sql
WITH done_projects AS (
    SELECT DISTINCT project_id FROM kongyu.task WHERE status = 'done'
),
project_info AS (
    SELECT id, name FROM kongyu.project
)
SELECT pi.id, pi.name
FROM project_info pi
JOIN done_projects dp ON pi.id = dp.project_id;
```

说明：

- 多个 `CTE` 用逗号隔开；
- 可逐层封装查询逻辑。

**3. 递归 CTE（例如构建树形结构）**

适用于组织架构、菜单树等层级数据结构（略高级，视项目需要使用）：

```sql
WITH RECURSIVE task_hierarchy AS (
    SELECT id, name, parent_id
    FROM kongyu.task
    WHERE parent_id IS NULL
    UNION ALL
    SELECT t.id, t.name, t.parent_id
    FROM kongyu.task t
    JOIN task_hierarchy th ON t.parent_id = th.id
)
SELECT * FROM task_hierarchy;
```

说明：

- 递归 CTE 必须加 `RECURSIVE`；
- `UNION ALL` 链接“根节点”与“子节点”；
- 非常适合处理“树状结构”的表。



## 树状/组织架构表

### 创建表

```sql
DROP TABLE IF EXISTS t_org_unit;
CREATE TABLE t_org_unit (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id    BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID（根节点为0）',
    name         VARCHAR(255) NOT NULL COMMENT '名称',
    code         VARCHAR(100) NOT NULL COMMENT '编码，唯一',
    tree_path    VARCHAR(500) NOT NULL COMMENT '完整路径，如 /1/3/8/',
    level        INT NOT NULL DEFAULT 1 COMMENT '树层级，从1开始',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_tree_path (tree_path)
) COMMENT = '组织单元（支持上下级/树结构）';
```

### 插入数据

#### 示例数据（3 层树）

```
集团公司（id=1）
 ├─ 职能总部（id=2）
 │    ├─ 财务部（id=4）
 │    └─ HR（id=5）
 └─ 事业部（id=3）
      ├─ 市场部（id=6）
      └─ 研发部（id=7）
```

#### SQL 插入示例数据

```sql
INSERT INTO t_org_unit (id, parent_id, name, code, tree_path, level) VALUES
(1, 0, '集团公司', '1000', '/1/', 1),
(2, 1, '职能总部', '1010', '/1/2/', 2),
(3, 1, '事业部', '1020', '/1/3/', 2),
(4, 2, '财务部', '1011', '/1/2/4/', 3),
(5, 2, '人力资源部', '1012', '/1/2/5/', 3),
(6, 3, '市场部', '1021', '/1/3/6/', 3),
(7, 3, '研发部', '1022', '/1/3/7/', 3);
```

### 常用查询示例

#### 查询某个部门的所有子级（包含所有层级）

比如查询 **职能总部(id=2)** 下的所有部门：

方式 A：使用 `tree_path`（性能最好）

```sql
SELECT *
FROM t_org_unit
WHERE tree_path LIKE '/1/2/%';
```

------

方式 B：递归 CTE（标准写法）

```sql
WITH RECURSIVE sub AS (
    SELECT * FROM t_org_unit WHERE id = 2
    UNION ALL
    SELECT c.*
    FROM t_org_unit c
    JOIN sub s ON c.parent_id = s.id
)
SELECT * FROM sub;
```

------

#### 查询某个部门的所有上级（从下往上查）

比如查询 **研发部(id=7)** 的所有祖先节点：

方式 A：使用 `tree_path`（性能最好）

```sql
SELECT *
FROM t_org_unit
WHERE '/1/3/7/' LIKE CONCAT(tree_path,'%');
```

方式 B：递归 CTE（标准写法）

```sql
WITH RECURSIVE parent_tree AS (
    SELECT * FROM t_org_unit WHERE id = 7
    UNION ALL
    SELECT p.*
    FROM t_org_unit p
    JOIN parent_tree ct ON p.id = ct.parent_id
)
SELECT * FROM parent_tree;
```

#### 查询完整路径中文名称（类似“集团公司 / 事业部 / 研发部”）

```sql
WITH RECURSIVE pt AS (
    SELECT * FROM t_org_unit WHERE id = 7
    UNION ALL
    SELECT t.*
    FROM t_org_unit t
    JOIN pt ON t.id = pt.parent_id
)
SELECT GROUP_CONCAT(name ORDER BY level SEPARATOR ' / ') AS full_path_name
FROM pt;
```

输出：

```
集团公司 / 事业部 / 研发部
```

------

#### 查询整棵树并带层级缩进（适合前端渲染树）

```sql
WITH RECURSIVE tree AS (
    SELECT id, parent_id, name, level, name AS display_name
    FROM t_org_unit WHERE parent_id = 0
    UNION ALL
    SELECT c.id, c.parent_id, c.name, c.level,
           CONCAT(REPEAT('  ', c.level - 1), '├─ ', c.name)
    FROM t_org_unit c
    JOIN tree t ON c.parent_id = t.id
)
SELECT * FROM tree ORDER BY level;
```

------

#### 查询两个节点是否存在上下级关系

比如：**事业部(id=3)** 是否为 **市场部(id=6)** 的上级？

```sql
SELECT CASE
    WHEN '/1/3/' IN (SELECT tree_path FROM t_org_unit WHERE id = 6)
    THEN 'YES' ELSE 'NO' END AS is_parent;
```

------

#### 批量查询多个部门的所有子部门

```sql
WITH RECURSIVE sub AS (
    SELECT * FROM t_org_unit WHERE id IN (2, 3)
    UNION ALL
    SELECT c.*
    FROM t_org_unit c
    JOIN sub s ON c.parent_id = s.id
)
SELECT * FROM sub;
```

------

####  查询同级部门

```sql
SELECT *
FROM t_org_unit
WHERE parent_id = (SELECT parent_id FROM t_org_unit WHERE id = 4)
  AND id <> 4;
```

------

#### ⭐ 8. 统计每个部门下有多少子部门

```sql
SELECT p.id, p.name,
       COUNT(c.id) AS child_count
FROM t_org_unit p
LEFT JOIN t_org_unit c ON c.tree_path LIKE CONCAT(p.tree_path, '%')
GROUP BY p.id, p.name;
```



## 进阶数据修改操作

### INSERT 与 UPDATE

------

🔹 **1. INSERT ... ON DUPLICATE KEY UPDATE**

当主键或唯一索引冲突时，执行更新操作。

```sql
INSERT INTO user_stats (user_id, login_count)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE login_count = login_count + 1;
```

✅ 用途：实现 **“有则更新，无则插入”** 的幂等写入操作。

------

🔹 **2. INSERT IGNORE**

遇到主键/唯一约束冲突时 **忽略错误**（跳过该行）。

```sql
INSERT IGNORE INTO users (id, name) VALUES (1, 'Alice');
```

✅ 用途：适用于数据全量导入、去重插入等场景。

------

🔹 **3. REPLACE INTO**

功能类似 INSERT，但如果主键或唯一索引冲突，会先 **删除旧记录**，再插入新记录。

```sql
REPLACE INTO users (id, name) VALUES (1, 'Bob');
```

⚠️ 注意：`REPLACE` 实质是 **DELETE + INSERT**，有副作用（如触发器执行两次、自增字段重置等），不推荐频繁使用。

------

🔹 **4. INSERT ... SELECT**

从其他表查询数据，并批量插入。

```sql
INSERT INTO archive_orders (id, user_id, total)
SELECT id, user_id, total FROM orders
WHERE create_time < '2024-01-01';
```

✅ 用途：常用于 **数据归档、数据同步、离线导入** 等。

------

🔹 **5. 多行 INSERT 提高性能**

```sql
INSERT INTO users (id, name) VALUES
(1, 'Alice'),
(2, 'Bob'),
(3, 'Charlie');
```

✅ 用途：相比多次单行插入，性能提升明显（推荐批量导入使用）。

------

🔹 **6. 批量 UPDATE：使用 JOIN 联合更新**

```sql
UPDATE orders o
JOIN users u ON o.user_id = u.id
SET o.region = u.region
WHERE o.region IS NULL;
```

✅ 用途：适用于主表字段需要从副表批量补齐或更新的场景。

------

🔹 **7. 条件 CASE 更新**

```sql
UPDATE orders
SET status = CASE
  WHEN total >= 1000 THEN 'VIP'
  WHEN total >= 500 THEN 'HIGH'
  ELSE 'NORMAL'
END;
```

✅ 用途：对同一字段按不同条件赋值（**批量分类更新**）。

### DELETE

------

🔹 **1. 基于 JOIN 条件删除**

从主表中删除满足某些条件、在子表中存在对应记录的数据。

```sql
DELETE t1
FROM users t1
JOIN blacklist t2 ON t1.email = t2.email;
```

✅ 用途：删除命中黑名单的用户。

------

🔹 **2. 子查询中使用 DELETE**

```sql
DELETE FROM orders
WHERE user_id IN (
  SELECT id FROM users WHERE is_banned = 1
);
```

✅ 用途：删除所有来自被封禁用户的订单。

------

🔹 **3. 删除重复记录，仅保留一条**

假设 `email` 字段有重复：

```sql
DELETE u1
FROM users u1
JOIN users u2
ON u1.email = u2.email AND u1.id > u2.id;
```

✅ 用途：清理表中冗余数据，保留最小 `id` 的一条。

------

🔹 **4. 使用 LIMIT 分批删除**

```sql
DELETE FROM logs
WHERE create_time < NOW() - INTERVAL 90 DAY
LIMIT 1000;
```

✅ 用途：日志、归档类数据的 **按批次清理**，避免大事务。

------

🔹 **5. 配合事务分批清除**

```sql
START TRANSACTION;

DELETE FROM temp_data
WHERE processed = 1
LIMIT 500;

COMMIT;
```

✅ 用途：可与脚本配合，进行定时清理。

------

🔹 **6. TRUNCATE 表清空（高性能删除）**

```sql
TRUNCATE TABLE temp_cache;
```

⚠️ 注意：不可回滚，不触发触发器，适合清空临时表或缓存表。

------

🔹 **7. 使用 CTE 语句批量删除（MySQL 8+）**

```sql
WITH to_delete AS (
  SELECT id FROM messages WHERE is_read = 1 LIMIT 1000
)
DELETE FROM messages WHERE id IN (SELECT id FROM to_delete);
```

✅ 用途：**CTE + 删除**组合优化复杂条件处理。

------





## 索引与性能优化

------

🔹 **索引分类**

索引是提高查询效率的关键结构，MySQL 支持多种索引类型。

**1. 单列索引**

```sql
CREATE INDEX idx_project_name ON project(name);
```

- 仅对单一字段建索引；
- 等值查询、高选择性字段优先。

**2. 联合索引（复合索引）**

```sql
CREATE INDEX idx_project_type_name ON project(type, name);
```

- 多字段组合建立；
- 遵循**最左前缀原则**，例如可用于 `(type)` 或 `(type, name)`，但不支持仅 `(name)`；
- 常用于多条件筛选场景。

**3. 唯一索引（UNIQUE）**

```sql
CREATE UNIQUE INDEX idx_project_code ON project(code);
```

- 保证字段唯一性；
- 避免重复数据写入，常用于业务编码、手机号等。

**4. 覆盖索引**

> 查询所需字段完全被索引覆盖，不再回表，提高查询效率。

```sql
-- 使用覆盖索引的查询
SELECT name FROM project WHERE type = 'internal';
```

- 若 `(type, name)` 是联合索引，以上查询可以直接命中索引，无需访问表数据。

**5. 全文索引（FULLTEXT）**

```sql
CREATE FULLTEXT INDEX idx_description ON project(description);
```

- 用于自然语言文本搜索（MyISAM/InnoDB）；
- 可配合 `MATCH ... AGAINST` 使用。

------

🔹 **如何查看执行计划（EXPLAIN）**

`EXPLAIN` 用于分析 SQL 查询执行路径，判断是否使用了索引、是否出现了全表扫描等。

**基本用法**

```sql
EXPLAIN SELECT * FROM project WHERE type = 'internal';
```

或者

```sql
EXPLAIN FORMAT=JSON SELECT * FROM project WHERE type = 'internal';
```

**常见字段说明**

| 字段          | 含义                                          |
| ------------- | --------------------------------------------- |
| id            | 查询的执行顺序（越大越先执行）                |
| select_type   | 查询类型（SIMPLE、PRIMARY、SUBQUERY 等）      |
| table         | 表名                                          |
| type          | 连接类型（ALL、index、ref、const、eq_ref...） |
| possible_keys | 可用索引                                      |
| key           | 实际使用的索引                                |
| rows          | 估算扫描行数                                  |
| Extra         | 额外信息，如 Using index、Using where 等      |

**示例判断**

```sql
type = ALL       -- 全表扫描（性能差）
type = ref       -- 使用索引字段查找（性能好）
Extra: Using index -- 使用了覆盖索引
```

------

🔹 **慢查询分析**

MySQL 支持记录执行时间较长的 SQL，用于分析优化瓶颈。

**开启慢查询日志（my.cnf）**

```ini
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1  -- 超过 1 秒记录
log_queries_not_using_indexes = 1  -- 未使用索引的也记录
```

**查询慢日志配置**

```sql
SHOW VARIABLES LIKE '%slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
```

**使用 `mysqldumpslow` 或 `pt-query-digest` 分析慢查询**

```bash
mysqldumpslow /var/log/mysql/slow.log
pt-query-digest /var/log/mysql/slow.log
```

- 支持按执行频率、时间分组；
- 可生成慢查询报告。

------

🔹 **常见优化策略**

✅ **避免 SELECT **

```sql
-- ❌ 不推荐
SELECT * FROM project;

-- ✅ 推荐（只查所需字段）
SELECT id, name FROM project;
```

- `SELECT *` 会读取所有字段，增加 I/O；
- 无法利用覆盖索引。

✅ **避免函数操作字段**

```sql
-- ❌ 索引失效
SELECT * FROM project WHERE LEFT(code, 4) = '2025';

-- ✅ 使用范围匹配
SELECT * FROM project WHERE code LIKE '2025%';
```

- 对字段使用函数，索引无法命中；
- 尽量将操作放在等号右侧。

✅ **使用 LIMIT 分页时配合主键条件**

```sql
-- ❌ 大 OFFSET 会产生大量扫描
SELECT * FROM project ORDER BY id LIMIT 100000, 10;

-- ✅ 推荐方式：记住上次最大 ID
SELECT * FROM project WHERE id > 123456 ORDER BY id LIMIT 10;
```

- 大分页推荐“游标式分页”；
- 减少跳过行数，提高效率。

✅ **避免隐式类型转换**

```sql
-- ❌ 假设 id 是 INT，但传字符串
SELECT * FROM project WHERE id = '123';

-- ✅ 明确数据类型
SELECT * FROM project WHERE id = 123;
```

- 类型不一致可能导致索引失效。

------

🔹 **如何定位未使用索引的 SQL**

MySQL 提供几种方式可以发现没有命中索引的 SQL：

**1. 开启日志：记录未使用索引的查询**

```ini
log_queries_not_using_indexes = 1
```

- 开启后会将未使用索引的 SQL 写入慢查询日志；
- 配合 `slow_query_log` 一起使用效果更佳。

**2. 使用 `EXPLAIN` 检查查询执行路径**

```sql
EXPLAIN SELECT * FROM project WHERE YEAR(created_time) = 2024;
```

- 若 `type = ALL` 且 `key = NULL`，说明没有用到任何索引；
- 如果出现 `Using where` 而无 `Using index`，说明存在回表。

**3. `performance_schema` 和 `sys` 库辅助分析**

```sql
-- 找出未命中索引的 top SQL
SELECT * FROM sys.statements_with_full_table_scans LIMIT 10;
```

------

🔹 **实际项目中的优化案例**

✅ 案例 1：分页性能优化

```sql
-- ❌ 低效分页（大 OFFSET）
SELECT * FROM project ORDER BY id LIMIT 100000, 10;

-- ✅ 推荐：基于主键做游标分页
SELECT * FROM project WHERE id > 123456 ORDER BY id LIMIT 10;
```

- 减少扫描行数；
- 提升响应时间（尤其在大数据量下）。

✅ 案例 2：索引失效 due to 函数包裹

```sql
-- ❌ 索引失效：函数包裹字段
SELECT * FROM project WHERE DATE(created_time) = '2025-07-30';

-- ✅ 重写为范围查询
SELECT * FROM project
WHERE created_time >= '2025-07-30 00:00:00'
  AND created_time <  '2025-07-31 00:00:00';
```

- 避免对字段使用函数、表达式；
- 保持字段“裸用”最能命中索引。

✅ 案例 3：覆盖索引优化

```sql
-- 建立联合索引
CREATE INDEX idx_type_name ON project(type, name);

-- 查询命中覆盖索引
SELECT name FROM project WHERE type = 'internal';
```

- 查询字段完全被索引覆盖；
- 可避免回表，大幅提升查询速度。

------

🔹 **查询缓存替代方案（MySQL 8+）**

> MySQL 8.0 **已移除** Query Cache，推荐通过应用层或中间件缓存提升查询效率。

替代方案：

1. **Redis 缓存热点数据**
   - 前端或接口缓存热点查询结果，减少数据库压力；
   - 可设置过期时间，避免数据不一致。
2. **Materialized View（物化视图）**
   - 通过程序周期性刷新热点表到缓存表；
   - 实现类似缓存机制的效果。
3. **Proxy 层缓存**（如 ProxySQL、MyCat）

------

🔹 **数据库结构优化建议**

✅ 1. 避免字段过宽

- `VARCHAR(1000)` 等大字段不宜频繁参与查询；
- 长字段建议独立表存储或异步加载。

✅ 2. 精简索引数量

- 每增加一个索引，`INSERT/UPDATE` 会变慢；
- 审查索引使用频率，避免冗余索引。

✅ 3. 控制表连接数量

- 多表 JOIN 会增加临时表、排序等；
- 拆分为多次查询或使用中间表缓解。

✅ 4. 定期优化表结构

```sql
OPTIMIZE TABLE project;
```

- 释放碎片空间，重建索引。

✅ 5. 分区表 / 分表策略

- 针对大表（如日志表），可考虑按时间、ID 分区；
- 适用于海量数据写入和归档场景。

------



## 事务与锁

MySQL 使用 InnoDB 存储引擎时支持标准的 **ACID** 事务，并且拥有多种锁机制和隔离级别。理解事务控制和锁机制是确保数据一致性与并发性能的关键。

------

🔹 **基础事务控制**

```sql
-- 开启事务
START TRANSACTION;  -- 或者 BEGIN;

-- 执行若干操作
UPDATE account SET balance = balance - 100 WHERE id = 1;
UPDATE account SET balance = balance + 100 WHERE id = 2;

-- 提交事务
COMMIT;

-- 或者遇到异常，回滚
ROLLBACK;
```

说明：

- `BEGIN` 或 `START TRANSACTION` 启动显式事务；
- `COMMIT` 提交所有更改；
- `ROLLBACK` 回滚至事务开始前状态；
- 默认 MySQL 是自动提交的，需手动关闭：`SET autocommit = 0;`

------

🔹 **事务的四大特性（ACID）**

| 特性                  | 描述                                       |
| --------------------- | ------------------------------------------ |
| 原子性（Atomicity）   | 事务中的所有操作要么全部成功，要么全部失败 |
| 一致性（Consistency） | 执行事务前后，数据都必须处于一致状态       |
| 隔离性（Isolation）   | 多个事务互不干扰                           |
| 持久性（Durability）  | 事务一旦提交，数据永久保存                 |

------

🔹 **事务隔离级别**

| 隔离级别         | 脏读 | 不可重复读 | 幻读 | 说明                                 |
| ---------------- | ---- | ---------- | ---- | ------------------------------------ |
| READ UNCOMMITTED | ✅    | ✅          | ✅    | 最低级别，效率高，可能读到未提交数据 |
| READ COMMITTED   | ❌    | ✅          | ✅    | Oracle 默认，避免脏读                |
| REPEATABLE READ  | ❌    | ❌          | ✅    | MySQL 默认，防止不可重复读           |
| SERIALIZABLE     | ❌    | ❌          | ❌    | 最严格，事务串行执行，性能最低       |

查看或设置隔离级别：

```sql
-- 查看当前会话隔离级别
SELECT @@tx_isolation;

-- 设置当前会话为 READ COMMITTED
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

------

🔹 **锁机制**

✅ 表级锁（Table Lock）

```sql
-- 显式加锁
LOCK TABLES project WRITE;

-- 解锁
UNLOCK TABLES;
```

- 读锁（READ）：允许并发读，但不能写；
- 写锁（WRITE）：阻止其他读写；
- 表级锁简单但并发性差，适用于读多写少的场景。

✅ 行级锁（Row Lock）

- InnoDB 默认采用行级锁；
- 加锁基于主键或唯一索引才能生效：

```sql
-- 行锁生效（基于主键）
SELECT * FROM project WHERE id = 123 FOR UPDATE;

-- 行锁失效（无索引）
SELECT * FROM project WHERE name = 'abc' FOR UPDATE;
```

说明：

- `FOR UPDATE`：加排它锁（写锁）；
- `LOCK IN SHARE MODE`：加共享锁（读锁）；
- 行级锁可提升并发，但容易产生死锁。

------

🔹 **死锁与排查**

死锁示例：

两个事务交叉加锁：

```sql
-- 事务 A
BEGIN;
UPDATE project SET status = 'A' WHERE id = 1;
-- 等待事务 B

-- 事务 B
BEGIN;
UPDATE project SET status = 'B' WHERE id = 2;
UPDATE project SET status = 'B' WHERE id = 1;  -- 死锁

-- 事务 A 再更新 id = 2，也死锁
```

排查死锁：

```sql
SHOW ENGINE INNODB STATUS\G
```

输出中包含 **LATEST DETECTED DEADLOCK**，可查看死锁线程、锁信息等。

避免死锁建议：

| 建议           | 说明                             |
| -------------- | -------------------------------- |
| 固定的加锁顺序 | 所有事务按相同顺序加锁，避免交叉 |
| 限定锁范围     | 使用唯一索引精确加锁             |
| 减少锁时间     | 减少事务逻辑、尽快提交           |
| 使用小事务     | 拆分大事务为小事务，降低冲突概率 |

------

🔹 其他事务控制语法

```sql
-- 设置自动提交关闭（不推荐全局关闭）
SET autocommit = 0;

-- 提交并开始新事务
COMMIT AND CHAIN;

-- 回滚并开始新事务
ROLLBACK AND CHAIN;
```

------



## 视图与临时表

视图和临时表是 MySQL 中用于抽象、优化和复用查询逻辑的常用工具，能显著提升 SQL 可维护性与开发效率。

------

🔹 **视图（VIEW）**

视图是基于 SQL 查询语句构建的虚拟表，并不会存储实际数据。

✅ 创建视图

```sql
-- 基本语法
CREATE VIEW view_project_summary AS
SELECT id, name, status
FROM project
WHERE deleted = 0;
```

✅ 使用视图

```sql
SELECT * FROM view_project_summary WHERE status = 'ACTIVE';
```

✅ 修改视图

```sql
-- 重新定义视图
ALTER VIEW view_project_summary AS
SELECT id, name, status, create_time
FROM project
WHERE deleted = 0;
```

✅ 删除视图

```sql
DROP VIEW IF EXISTS view_project_summary;
```

------

📝 应用说明

- 视图不能接收参数；
- 支持连接、聚合等复杂查询；
- 视图不能索引，性能受限于底层表；
- 有些视图不支持更新操作，称为 **不可更新视图**（如聚合、JOIN）。

------

🔹 **临时表（Temporary Table）**

临时表是会话级别的表，只在当前连接中可见，连接断开后自动删除。

✅ 创建临时表

```sql
CREATE TEMPORARY TABLE temp_user_summary (
  id INT,
  name VARCHAR(50),
  total_orders INT
);
```

✅ 使用临时表

```sql
INSERT INTO temp_user_summary
SELECT u.id, u.name, COUNT(o.id)
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name;

SELECT * FROM temp_user_summary;
```

✅ 删除临时表

```sql
DROP TEMPORARY TABLE IF EXISTS temp_user_summary;
```

------

📝 应用说明

- 临时表仅当前连接可见，不影响其他用户；
- 常用于复杂逻辑拆分、中间结果缓存；
- 临时表支持索引、主键、约束；
- 若名称冲突，可与普通表共存，不互相影响。

------

🔹 **物化子查询 vs 临时表**

物化子查询指执行一次后将结果缓存（在内存或磁盘），提升 JOIN 或 IN 操作性能。

```sql
-- 子查询可能被物化（MySQL 8.0+）
SELECT * FROM orders o
JOIN (SELECT user_id FROM users WHERE is_active = 1) AS u
ON o.user_id = u.user_id;
```

⚠️ 注意：

- 物化行为由优化器决定；
- 如加 `STRAIGHT_JOIN` 可以强制顺序，或使用临时表显式缓存；
- `IN`/`EXISTS` 也可能触发物化，但相关子查询一般不会；

------

🔹 ✅ 应用场景对比总结

| 场景/类型      | 视图（VIEW） | 临时表（TEMP TABLE） | 物化子查询       |
| -------------- | ------------ | -------------------- | ---------------- |
| 数据是否持久化 | ❌            | ❌（会话级）          | ❌（自动管理）    |
| 是否可加索引   | ❌            | ✅                    | ❌                |
| 是否自动创建   | 否           | 否                   | 是               |
| 可维护性       | 高           | 中                   | 低               |
| 使用难度       | 简单         | 需要建表             | 无需管理         |
| 推荐用途       | 查询复用     | 多步复杂计算         | 提高性能临时缓存 |

------



## 分区表 和 归档表

### 分区表（Partitioned Table）

💡 作用

将一个大表 **逻辑拆分成多个分区（partition）**，每个分区存储在物理上独立的区段中，从而：

- 提高查询效率（针对分区字段的查询只扫部分分区）
- 管理更灵活（可以单独删除某个分区）

------

📌 分区类型示例（MySQL 支持以下几种）

| 类型  | 说明                          |
| ----- | ----------------------------- |
| RANGE | 根据范围分区（如日期区间）    |
| LIST  | 枚举值分区（如按省份）        |
| HASH  | 按哈希值自动分区              |
| KEY   | 类似 HASH，用于主键或唯一索引 |

------

✅ 示例：按创建时间按年分区

```sql
CREATE TABLE project_log (
  id BIGINT NOT NULL,
  name VARCHAR(100),
  created_at DATE NOT NULL,
  PRIMARY KEY(id, created_at)
)
PARTITION BY RANGE (YEAR(created_at)) (
  PARTITION p2022 VALUES LESS THAN (2023),
  PARTITION p2023 VALUES LESS THAN (2024),
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

> ✅ 查询 `WHERE created_at BETWEEN '2023-01-01' AND '2023-12-31'` 只扫描 `p2023`

------

✅ 注意事项

- 分区字段必须是主键/唯一键的一部分。
- 不支持全文索引、外键等。
- 不适合小表。

------

### 归档表（归档策略）

💡 作用

定期将 **旧数据从主表移动到历史表**，降低主表体积。

------

📌 应用场景

- 项目完成、订单结束、历史日志等
- 查询只针对近一段时间数据
- 只读数据可以放归档表

------

✅ 归档策略示例

1. **建立归档表结构**

```sql
CREATE TABLE project_archive LIKE project;
```

1. **定期归档（如每月一次）**

```sql
INSERT INTO project_archive
SELECT * FROM project
WHERE created_at < CURDATE() - INTERVAL 6 MONTH;

DELETE FROM project
WHERE created_at < CURDATE() - INTERVAL 6 MONTH;
```

> 可使用 **事件（EVENT）或定时任务（如 cron + shell）** 实现自动归档。

------

✅ 归档与分区对比

| 特性           | 分区表             | 归档表              |
| -------------- | ------------------ | ------------------- |
| 数据是否同一表 | ✅ 是               | ❌ 拆分为多个表      |
| 管理便利性     | ✅ 查询统一         | ❌ 需 union 或 merge |
| 查询性能       | ✅ 针对分区字段优化 | ✅ 主表更小更快      |
| 适合读写       | ✅ 读写都适合       | ❌ 归档表只读常见    |

------

✅ 最佳实践建议

- 日志、订单等大数据量表：**优先考虑分区**
- 归档多用于**多年数据归档或历史审计记录**
- 可组合使用：**当前表分区 + 历史归档表**

------

## 触发器（TRIGGER）

------

✅ 1. 基本定义

触发器（Trigger）是一个由数据库自动执行的程序，当对某个表执行 `INSERT`、`UPDATE` 或 `DELETE` 操作时，会自动触发执行预先定义的 SQL 逻辑。

------

✅ 2. 语法格式

```sql
CREATE TRIGGER trigger_name
{BEFORE | AFTER} {INSERT | UPDATE | DELETE}
ON table_name
FOR EACH ROW
BEGIN
    -- SQL 语句
END;
```

- `BEFORE` / `AFTER`：指定触发的时机
- `INSERT` / `UPDATE` / `DELETE`：指定触发的操作类型
- `FOR EACH ROW`：每处理一行数据都会执行一次触发器
- `NEW.column`：新值（用于 INSERT、UPDATE）
- `OLD.column`：旧值（用于 UPDATE、DELETE）

------

✅ 3. 示例讲解

**（1）AFTER INSERT：插入记录后写日志**

```sql
CREATE TRIGGER trg_after_insert_project
AFTER INSERT ON project
FOR EACH ROW
BEGIN
    INSERT INTO project_log(project_id, action, action_time)
    VALUES (NEW.id, 'INSERT', NOW());
END;
```

------

**（2）BEFORE UPDATE：更新前记录修改前后的值**

```sql
CREATE TRIGGER trg_before_update_project
BEFORE UPDATE ON project
FOR EACH ROW
BEGIN
    INSERT INTO project_log(project_id, action, old_name, new_name, action_time)
    VALUES (OLD.id, 'UPDATE', OLD.name, NEW.name, NOW());
END;
```

------

**（3）BEFORE DELETE：删除前备份数据**

```sql
CREATE TRIGGER trg_before_delete_project
BEFORE DELETE ON project
FOR EACH ROW
BEGIN
    INSERT INTO project_backup(id, name, deleted_at)
    VALUES (OLD.id, OLD.name, NOW());
END;
```

------

✅ 4. 查看与删除触发器

- **查看所有触发器**

```sql
SHOW TRIGGERS;
```

- **删除触发器**

```sql
DROP TRIGGER IF EXISTS trg_after_insert_project;
```

------

✅ 5. 注意事项

| 项目                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 触发器限制             | 每种操作（如 INSERT）只能有一个同类型（BEFORE/AFTER）触发器  |
| 禁止事务控制           | 触发器内不能执行 `COMMIT` 或 `ROLLBACK`                      |
| 修改同表限制           | 触发器中不能对其作用的表执行写操作（避免递归）               |
| 必须使用 `BEGIN...END` | 多条语句必须包在 `BEGIN...END` 中，注意使用 `DELIMITER` 分隔符切换 |

------

✅ 6. 实际应用场景

| 应用类型     | 是否推荐使用触发器 | 说明                          |
| ------------ | ------------------ | ----------------------------- |
| 审计日志记录 | ✅ 推荐             | 自动记录变更，无需业务侵入    |
| 更新时间维护 | ✅ 推荐             | 可自动设置 `update_time` 字段 |
| 跨表同步更新 | ⚠️ 谨慎使用         | 注意性能及事务一致性          |
| 复杂业务逻辑 | ❌ 不推荐           | 建议放在业务代码中处理        |

------



## 存储过程与函数（Stored Procedures & Functions）

------

✅ 1. 基本定义

- **存储过程（Procedure）**：一组预编译的 SQL 语句集合，可通过调用名称并传入参数重复执行。
- **存储函数（Function）**：类似过程，但必须有 `RETURN` 返回值，通常用于计算某个值。

------

✅ 2. 创建语法

（1）创建存储过程

```sql
DELIMITER $$

CREATE PROCEDURE procedure_name(
    IN param1 INT,
    OUT param2 VARCHAR(50)
)
BEGIN
    -- SQL语句块
END $$

DELIMITER ;
```

（2）创建函数

```sql
DELIMITER $$

CREATE FUNCTION function_name(param1 INT)
RETURNS VARCHAR(50)
DETERMINISTIC
BEGIN
    DECLARE result VARCHAR(50);
    -- 处理逻辑
    RETURN result;
END $$

DELIMITER ;
```

------

✅ 3. 参数类型说明

| 类型    | 说明                       |
| ------- | -------------------------- |
| `IN`    | 传入参数（调用时提供）     |
| `OUT`   | 输出参数（过程执行后返回） |
| `INOUT` | 即可输入也可输出           |

------

✅ 4. 调用方式

```sql
-- 调用存储过程（带OUT参数）
CALL procedure_name(100, @out_param);
SELECT @out_param;

-- 调用函数
SELECT function_name(100);
```

------

✅ 5. 示例讲解

（1）过程：根据ID获取项目名

```sql
DELIMITER $$

CREATE PROCEDURE get_project_name(
    IN project_id INT,
    OUT project_name VARCHAR(100)
)
BEGIN
    SELECT name INTO project_name
    FROM project
    WHERE id = project_id;
END $$

DELIMITER ;
```

调用：

```sql
CALL get_project_name(1, @name);
SELECT @name;
```

------

（2）函数：计算两个值的加权平均

```sql
DELIMITER $$

CREATE FUNCTION weighted_avg(score1 DOUBLE, score2 DOUBLE)
RETURNS DOUBLE
DETERMINISTIC
BEGIN
    RETURN (score1 * 0.6 + score2 * 0.4);
END $$

DELIMITER ;
```

调用：

```sql
SELECT weighted_avg(80, 90);  -- 返回 84
```

------

✅ 6. 管理操作

- 查看所有过程或函数：

```sql
SHOW PROCEDURE STATUS WHERE Db = 'your_db';
SHOW FUNCTION STATUS WHERE Db = 'your_db';
```

- 查看定义：

```sql
SHOW CREATE PROCEDURE procedure_name;
SHOW CREATE FUNCTION function_name;
```

- 删除：

```sql
DROP PROCEDURE IF EXISTS procedure_name;
DROP FUNCTION IF EXISTS function_name;
```

------

✅ 7. 注意事项与最佳实践

| 建议或限制                | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 参数命名规范              | 避免与表字段或保留关键字冲突                                 |
| 使用 `DETERMINISTIC`      | 函数需标明是否确定性（相同输入是否必定相同输出）             |
| 不可使用事务控制语句      | 如在函数中禁止使用 `START TRANSACTION`, `COMMIT`, `ROLLBACK` |
| 使用 `DELIMITER` 切换符号 | 定义过程或函数中包含 `;`，需用 `DELIMITER` 分隔              |

------



当然，以下是符合你格式要求的 **MySQL 事件调度器（EVENT）** 章节内容：

------

## 事件调度器（EVENT）

------

✅ 1. 概述

- **事件（EVENT）** 是 MySQL 的一种计划任务机制，用于定时执行 SQL 操作，无需借助外部工具（如 crontab）。
- 可用于定时归档、清理数据、统计分析等场景。

------

✅ 2. 启用事件调度器

MySQL 默认关闭事件调度器，需先开启：

```sql
-- 开启事件调度器（临时，重启失效）
SET GLOBAL event_scheduler = ON;

-- 查看当前状态
SHOW VARIABLES LIKE 'event_scheduler';
```

若需永久开启，可在 `my.cnf` 中添加：

```ini
[mysqld]
event_scheduler=ON
```

------

✅ 3. 创建事件语法

```sql
CREATE EVENT event_name
ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 DAY
DO
    SQL语句;
```

或者设置为 **周期性执行**：

```sql
CREATE EVENT event_name
ON SCHEDULE EVERY 1 DAY
STARTS '2025-08-01 00:00:00'
ENDS '2025-08-10 00:00:00'
DO
    SQL语句;
```

------

✅ 4. 示例讲解

（1）每晚清除过期日志：

```sql
CREATE EVENT IF NOT EXISTS delete_old_logs
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP + INTERVAL 1 DAY
DO
    DELETE FROM logs WHERE created_at < NOW() - INTERVAL 30 DAY;
```

（2）定时插入演示数据：

```sql
CREATE EVENT insert_demo_data
ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 5 MINUTE
DO
    INSERT INTO demo_table(name, created_at)
    VALUES ('定时插入', NOW());
```

------

✅ 5. 管理事件

| 操作          | SQL 语句                                                     |
| ------------- | ------------------------------------------------------------ |
| 查看所有事件  | `SHOW EVENTS;`                                               |
| 查看事件定义  | `SHOW CREATE EVENT event_name;`                              |
| 删除事件      | `DROP EVENT IF EXISTS event_name;`                           |
| 禁用/启用事件 | `ALTER EVENT event_name DISABLE;``ALTER EVENT event_name ENABLE;` |
| 修改事件      | 使用 `ALTER EVENT` 重新定义时间或内容                        |

------

✅ 6. 注意事项

| 项目               | 说明                                         |
| ------------------ | -------------------------------------------- |
| 仅限 `InnoDB` 引擎 | 事件任务主要配合 InnoDB 使用                 |
| 权限要求           | 需具备 `EVENT` 权限才能创建/修改事件         |
| 精确度             | MySQL 的事件调度器精度为秒级                 |
| 不支持事务处理     | 事件中通常为单语句或无需事务控制的多语句操作 |
| 影响全局资源       | EVENT 是全局对象，不能基于会话隔离           |

------

