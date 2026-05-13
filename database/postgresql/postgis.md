# PostGIS

## 项目概述

PostGIS 是 PostgreSQL 的空间数据库扩展，用于在关系型数据库中存储、索引、查询和分析空间数据。项目中引入 PostGIS 后，可以直接使用 SQL 完成坐标点存储、距离计算、范围检索、区域判断、路线覆盖、空间关系分析等能力，适合需要地理位置能力的业务系统。

### PostGIS 功能定位

PostGIS 的核心定位是为 PostgreSQL 提供空间数据处理能力，使数据库不仅可以存储普通业务数据，还可以存储带有空间含义的数据，例如经纬度点位、道路线路、行政区域、多边形围栏等。

在项目中，PostGIS 通常承担以下职责：

| 功能         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 空间数据存储 | 存储点、线、面、多点、多线、多面等空间对象               |
| 空间索引加速 | 基于 GiST、SP-GiST 等索引提升空间查询性能                |
| 距离计算     | 计算两个坐标点之间的距离，常用于附近搜索                 |
| 范围检索     | 查询某个半径、矩形范围或多边形区域内的数据               |
| 空间关系判断 | 判断点是否在面内、两个区域是否相交、线路是否穿过区域     |
| 坐标系管理   | 通过 SRID 管理空间数据的坐标参考系统                     |
| 空间数据分析 | 支持缓冲区、交集、并集、差集、边界、面积、长度等分析能力 |

在业务系统中，PostGIS 一般不会单独使用，而是作为 PostgreSQL 的增强能力，配合 Spring Boot、MyBatis、JPA、GIS 地图服务或前端地图组件一起完成空间数据的存储、查询和展示。

常见的 SQL 使用方式如下：

```sql
-- 查询指定坐标 1000 米范围内的位置数据
SELECT *
FROM location_info
WHERE ST_DWithin(
    geom,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    1000
);
```

需要注意的是，如果使用 `geometry` 类型并采用 `SRID=4326`，坐标单位通常是“度”，直接用米计算距离可能不准确。实际项目中常见做法是使用 `geography` 类型进行米级距离计算，或者将数据转换到适合距离计算的投影坐标系。

### 项目使用场景

PostGIS 适合用于需要处理地理位置、空间范围、地图检索、空间分析的系统。只要业务数据与“位置”“范围”“距离”“区域关系”相关，就可以考虑使用 PostGIS。

典型使用场景如下：

| 使用场景           | 业务说明                                     | 常用函数                      |
| ------------------ | -------------------------------------------- | ----------------------------- |
| 附近位置查询       | 查询用户当前位置附近的门店、设备、车辆、人员 | `ST_DWithin`、`ST_Distance`   |
| 电子围栏           | 判断用户、车辆、设备是否进入指定区域         | `ST_Contains`、`ST_Within`    |
| 区域数据统计       | 统计某个行政区、网格、园区内的业务数据       | `ST_Intersects`、`ST_Within`  |
| 轨迹存储与分析     | 存储车辆轨迹、人员移动轨迹、物流路线         | `LINESTRING`、`ST_Length`     |
| 地图点位管理       | 管理门店、仓库、充电桩、摄像头、传感器位置   | `POINT`                       |
| 区域边界管理       | 存储行政区、商圈、服务范围、配送范围         | `POLYGON`、`MULTIPOLYGON`     |
| 路线与区域碰撞判断 | 判断路线是否经过某个限制区域                 | `ST_Crosses`、`ST_Intersects` |
| 缓冲区分析         | 生成某个点或线路周边一定距离的影响范围       | `ST_Buffer`                   |
| 空间排序           | 按距离从近到远返回查询结果                   | `ST_Distance`、`ORDER BY`     |

例如，在门店系统中，可以将每个门店的经纬度保存为 `POINT` 类型。当用户打开 App 时，后端根据用户当前位置查询 3 公里范围内的门店，并按距离升序返回。

```sql
-- 查询用户 3 公里范围内的门店，并按距离排序
SELECT
    id,
    store_name,
    ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) AS distance_meter
FROM store_info
WHERE ST_DWithin(
    geog,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    3000
)
ORDER BY distance_meter ASC;
```

在电子围栏场景中，可以将围栏区域保存为 `POLYGON` 或 `MULTIPOLYGON` 类型，然后判断设备当前位置是否位于围栏内部。

```sql
-- 判断指定点是否在围栏范围内
SELECT
    fence_id,
    fence_name
FROM fence_info
WHERE ST_Contains(
    geom,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
);
```

### 空间数据类型说明

PostGIS 中常用的空间数据类型主要包括 `geometry` 和 `geography`。两者都可以表示空间对象，但适用场景不同。项目设计时需要先明确数据主要用于平面空间计算，还是用于真实地球表面的距离、面积计算。

| 类型        | 说明                                     | 适用场景                                         |
| ----------- | ---------------------------------------- | ------------------------------------------------ |
| `geometry`  | 平面空间数据类型，计算基于坐标平面       | 地图展示、区域判断、空间关系判断、高性能空间查询 |
| `geography` | 地理空间数据类型，计算基于地球椭球模型   | 经纬度距离计算、米级半径检索、跨区域距离计算     |
| `raster`    | 栅格数据类型，用于存储影像或网格数据     | 遥感影像、地图瓦片、空间栅格分析                 |
| `topology`  | 拓扑模型，用于表达空间对象之间的拓扑关系 | 行政边界、道路网络、复杂空间关系维护             |

项目中最常用的是 `geometry` 和 `geography`。一般建议如下：

| 选择                             | 建议                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| 需要判断点是否在区域内           | 优先使用 `geometry`                                          |
| 需要查询附近 N 米的数据          | 优先使用 `geography`，或者使用合适投影坐标系下的 `geometry`  |
| 需要存储行政区、围栏、多边形边界 | 使用 `geometry(POLYGON, 4326)` 或 `geometry(MULTIPOLYGON, 4326)` |
| 需要存储用户、门店、设备经纬度   | 使用 `geometry(POINT, 4326)` 或 `geography(POINT, 4326)`     |
| 需要高性能复杂空间关系查询       | 优先使用 `geometry` 并建立 GiST 空间索引                     |

常见几何对象如下：

| 空间对象 | 类型示例             | 说明                                     |
| -------- | -------------------- | ---------------------------------------- |
| 点       | `POINT`              | 表示一个坐标点，例如门店位置、设备位置   |
| 线       | `LINESTRING`         | 表示一条线，例如道路、轨迹、路线         |
| 面       | `POLYGON`            | 表示一个封闭区域，例如围栏、商圈、行政区 |
| 多点     | `MULTIPOINT`         | 表示多个点的集合                         |
| 多线     | `MULTILINESTRING`    | 表示多条线的集合                         |
| 多面     | `MULTIPOLYGON`       | 表示多个多边形区域                       |
| 几何集合 | `GEOMETRYCOLLECTION` | 表示不同几何对象的组合                   |

空间字段定义示例：

```sql
-- 点位表：适合存储门店、设备、人员等经纬度位置
CREATE TABLE location_info (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    -- 使用 geometry 存储经纬度点，SRID 4326 表示 WGS84 坐标系
    geom geometry(POINT, 4326),
    -- 使用 geography 存储经纬度点，适合按米计算距离
    geog geography(POINT, 4326),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 geometry 字段创建空间索引
CREATE INDEX idx_location_info_geom
ON location_info
USING GIST (geom);

-- 为 geography 字段创建空间索引
CREATE INDEX idx_location_info_geog
ON location_info
USING GIST (geog);
```

常见空间数据写入示例：

```sql
-- 写入一个点位数据，经度在前，纬度在后
INSERT INTO location_info (name, geom, geog)
VALUES (
    '北京天安门',
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
);
```

需要特别注意，PostGIS 中经纬度点通常按照 `经度 longitude`、`纬度 latitude` 的顺序写入，也就是 `ST_MakePoint(lng, lat)`。如果顺序写反，数据可以成功入库，但地图展示、距离计算和范围查询都会出现明显偏差。



## 环境准备

本节用于说明项目运行 PostGIS 所需的基础环境，包括 PostgreSQL 数据库安装、PostGIS 扩展安装，以及在具体业务数据库中启用空间扩展。PostGIS 是 PostgreSQL 的扩展能力，安装扩展包后，还需要在每个需要使用空间能力的数据库中执行 `CREATE EXTENSION postgis;` 才能正式使用。([PostGIS](https://postgis.net/docs/en/postgis_installation.html?utm_source=chatgpt.com))

### PostgreSQL 安装

PostgreSQL 是 PostGIS 的基础运行环境。开发环境可以直接使用操作系统包管理器安装，生产环境建议固定 PostgreSQL 主版本，并统一开发、测试、生产环境的版本策略。PostgreSQL 官方说明 Ubuntu 可以直接通过 `apt install postgresql` 安装默认版本；如果需要指定主版本，可以使用 PostgreSQL 官方 Apt 仓库获取受支持版本。([PostgreSQL](https://www.postgresql.org/download/linux/ubuntu/?utm_source=chatgpt.com))

Ubuntu/Debian 环境下安装 PostgreSQL：

```bash
# 更新软件包索引
sudo apt update

# 安装 PostgreSQL 服务端和常用扩展包
sudo apt install -y postgresql postgresql-contrib

# 设置 PostgreSQL 开机启动并立即启动
sudo systemctl enable --now postgresql

# 查看 PostgreSQL 服务状态
sudo systemctl status postgresql
```

上面的命令适合快速安装系统默认版本的 PostgreSQL。`postgresql` 是数据库服务端，`postgresql-contrib` 包含一些常用的 PostgreSQL 扩展工具。生产环境如果要求固定 PostgreSQL 16、17 或 18 等主版本，应优先使用项目统一的软件源和版本规范。

查看 PostgreSQL 版本：

```bash
# 切换到 postgres 系统用户并进入 psql
sudo -iu postgres psql

-- 查看数据库版本
SELECT version();

-- 退出 psql
\q
```

创建项目数据库和项目用户：

```bash
# 进入 PostgreSQL 管理终端
sudo -iu postgres psql
-- 创建项目用户，密码需要替换为实际安全密码
CREATE USER postgis_user WITH PASSWORD 'PostGIS@123456';

-- 创建项目数据库，并指定归属用户
CREATE DATABASE postgis_demo OWNER postgis_user;

-- 授予数据库基础权限
GRANT ALL PRIVILEGES ON DATABASE postgis_demo TO postgis_user;

-- 连接到项目数据库
\c postgis_demo

-- 授予 public schema 权限，便于项目用户创建表、索引和扩展对象
GRANT ALL ON SCHEMA public TO postgis_user;
```

生产环境中不建议在业务系统中直接使用 `postgres` 超级用户连接数据库。应为项目创建独立账号，并按需授予数据库、Schema、表、序列、函数等权限。

### PostGIS 扩展安装

PostGIS 安装分为“操作系统层面的扩展包安装”和“数据库内部的扩展启用”两步。前者让 PostgreSQL 具备加载 PostGIS 的能力，后者让指定数据库真正拥有 `geometry`、`geography`、空间函数、空间索引等能力。PostGIS 官方文档也说明，PostGIS 安装后需要在每个需要使用的数据库中单独启用。([PostGIS](https://postgis.net/docs/en/postgis_installation.html?utm_source=chatgpt.com))

Ubuntu/Debian 环境下可以先搜索当前系统支持的 PostGIS 包：

```bash
# 搜索当前软件源中的 PostGIS 相关包
apt-cache search postgis
```

如果使用系统默认 PostgreSQL 版本，可以安装通用 PostGIS 包：

```bash
# 安装 PostGIS 扩展包
sudo apt install -y postgis
```

如果项目固定 PostgreSQL 主版本，建议安装对应版本的 PostGIS 包。下面以 PostgreSQL 16 为例，实际项目中需要将 `16` 替换为当前使用的 PostgreSQL 主版本：

```bash
# 安装 PostgreSQL 16 对应的 PostGIS 3 扩展包
sudo apt install -y postgresql-16-postgis-3 postgresql-16-postgis-3-scripts
```

安装完成后，可以查看 PostgreSQL 是否识别 PostGIS 扩展：

```bash
# 进入项目数据库
sudo -iu postgres psql -d postgis_demo
-- 查看当前 PostgreSQL 可用扩展中是否包含 postgis
SELECT
    name,
    default_version,
    installed_version,
    comment
FROM pg_available_extensions
WHERE name LIKE 'postgis%';
```

如果能够查询到 `postgis`，说明 PostgreSQL 当前实例已经识别到 PostGIS 扩展文件。此时还不能直接使用空间类型，需要继续在数据库中执行扩展启用命令。

### 数据库扩展启用

PostGIS 扩展需要在具体数据库中启用。一个 PostgreSQL 实例下可以有多个数据库，每个数据库是否启用 PostGIS 是独立的。例如 `postgis_demo` 启用了 PostGIS，不代表另一个业务数据库也自动启用。

进入项目数据库：

```bash
# 使用 postgres 管理用户连接项目数据库
sudo -iu postgres psql -d postgis_demo
```

启用 PostGIS 核心扩展：

```sql
-- 启用 PostGIS 核心扩展，提供 geometry、geography、空间函数、空间索引能力
CREATE EXTENSION IF NOT EXISTS postgis;

-- 可选：启用拓扑扩展，只有需要拓扑模型时才启用
CREATE EXTENSION IF NOT EXISTS postgis_topology;
```

验证 PostGIS 是否启用成功：

```sql
-- 查看 PostGIS 完整版本信息
SELECT PostGIS_Full_Version();

-- 查看当前数据库已安装的 PostGIS 扩展
SELECT
    extname,
    extversion
FROM pg_extension
WHERE extname LIKE 'postgis%';
```

PostGIS 官方入门文档也建议通过 `SELECT PostGIS_Full_Version();` 查看当前数据库中实际启用的 PostGIS 版本。([PostGIS](https://postgis.net/documentation/getting_started/?utm_source=chatgpt.com))

可以执行一个简单的空间函数验证：

```sql
-- 创建一个经纬度点，并输出 WKT 文本
SELECT ST_AsText(
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
) AS point_wkt;
```

预期结果类似：

```text
POINT(116.397128 39.916527)
```

如果能够正常返回 `POINT(...)`，说明当前数据库已经具备基础空间数据处理能力。

## 数据模型设计

本节用于说明 PostGIS 项目中空间表如何设计、`geometry` 与 `geography` 如何选择，以及空间索引如何创建。空间数据模型设计会直接影响查询准确性、距离单位、索引命中率和后续接口封装方式。

### 空间表设计

空间表设计时，建议将普通业务字段和空间字段放在同一张业务表中。普通字段用于业务筛选，例如名称、类型、状态、创建时间；空间字段用于空间计算，例如附近查询、范围判断、区域检索和空间排序。

以门店位置表为例，常见设计如下：

```sql
-- 门店位置表：同时保存业务字段、经纬度字段和空间字段
CREATE TABLE store_location (
    id BIGSERIAL PRIMARY KEY,

    -- 门店名称
    store_name VARCHAR(100) NOT NULL,

    -- 门店编码，用于和外部业务系统关联
    store_code VARCHAR(64) NOT NULL UNIQUE,

    -- 经度，范围为 -180 到 180
    longitude NUMERIC(10, 6) NOT NULL,

    -- 纬度，范围为 -90 到 90
    latitude NUMERIC(10, 6) NOT NULL,

    -- geometry 点位字段，适合空间关系判断、地图展示、区域查询
    geom geometry(POINT, 4326) NOT NULL,

    -- geography 点位字段，适合按照米进行距离计算
    geog geography(POINT, 4326) NOT NULL,

    -- 业务状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 经度范围约束
    CONSTRAINT chk_store_location_longitude
        CHECK (longitude >= -180 AND longitude <= 180),

    -- 纬度范围约束
    CONSTRAINT chk_store_location_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    -- 限制 geometry 字段必须使用 WGS84 坐标系
    CONSTRAINT chk_store_location_geom_srid
        CHECK (ST_SRID(geom) = 4326)
);
```

插入数据时，需要保证 `longitude`、`latitude`、`geom`、`geog` 的值一致。PostGIS 中构造点时通常使用 `ST_MakePoint(longitude, latitude)`，也就是经度在前、纬度在后。

```sql
-- 插入门店点位数据
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog
)
VALUES (
    '北京示例门店',
    'STORE_BJ_001',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
);
```

如果项目希望减少字段冗余，也可以只保存 `geom` 字段，在查询距离时临时转换为 `geography`：

```sql
-- 使用 geometry 字段临时转换为 geography 计算米级距离
SELECT
    id,
    store_name,
    ST_Distance(
        geom::geography,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) AS distance_meter
FROM store_location
WHERE status = 1;
```

这种方式字段更少，但在大量距离查询场景下，需要结合索引和查询计划评估性能。如果附近查询是核心高频接口，建议保留 `geog` 字段并单独建立空间索引。

区域表通常使用 `POLYGON` 或 `MULTIPOLYGON`。例如电子围栏、行政区、配送范围、服务区域等：

```sql
-- 区域围栏表：用于保存多边形或多多边形区域
CREATE TABLE area_fence (
    id BIGSERIAL PRIMARY KEY,

    -- 围栏名称
    fence_name VARCHAR(100) NOT NULL,

    -- 围栏编码
    fence_code VARCHAR(64) NOT NULL UNIQUE,

    -- 围栏区域，使用 MULTIPOLYGON 兼容单区域和多区域
    geom geometry(MULTIPOLYGON, 4326) NOT NULL,

    -- 业务状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 限制围栏字段必须使用 WGS84 坐标系
    CONSTRAINT chk_area_fence_geom_srid
        CHECK (ST_SRID(geom) = 4326)
);
```

区域数据写入示例：

```sql
-- 插入一个示例矩形围栏
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom
)
VALUES (
    '北京示例围栏',
    'FENCE_BJ_001',
    ST_GeomFromText(
        'MULTIPOLYGON(((116.390000 39.910000,116.410000 39.910000,116.410000 39.930000,116.390000 39.930000,116.390000 39.910000)))',
        4326
    )
);
```

区域数据建议使用 `MULTIPOLYGON` 而不是单纯的 `POLYGON`，因为实际业务中的行政区、服务区、商圈可能由多个不连续区域组成，`MULTIPOLYGON` 的兼容性更好。

### Geometry 与 Geography 选择

PostGIS 中最常用的空间类型是 `geometry` 和 `geography`。`geometry` 基于平面坐标系统，函数支持更完整，性能通常更好；`geography` 基于地球曲面，适合全球范围或不想处理投影坐标系的经纬度距离计算，但部分计算会更慢，支持函数范围也比 `geometry` 少。([PostGIS](https://postgis.net/documentation/faq/geometry-or-geography/?utm_source=chatgpt.com))

选择建议如下：

| 对比项   | geometry                                   | geography                          |
| -------- | ------------------------------------------ | ---------------------------------- |
| 计算模型 | 平面坐标                                   | 地球曲面                           |
| 常见坐标 | `geometry(POINT, 4326)`                    | `geography(POINT, 4326)`           |
| 距离单位 | 取决于 SRID，`4326` 下通常是度             | 米                                 |
| 函数支持 | 更完整                                     | 相对较少                           |
| 查询性能 | 通常更快                                   | 通常较慢                           |
| 适合场景 | 区域判断、空间关系、地图展示、本地投影计算 | 米级距离、附近查询、全球经纬度计算 |
| 学习成本 | 需要理解 SRID 和投影                       | 使用经纬度距离更直观               |

`ST_DWithin` 是附近查询中最常用的函数之一。对于 `geometry`，距离单位由空间参考系统决定；对于 `geography`，距离单位是米，并且默认基于椭球体计算。该函数也会自动包含边界框比较，从而利用可用的空间索引。([PostGIS](https://www.postgis.net/docs/manual-3.5/ST_DWithin.html?utm_source=chatgpt.com))

使用 `geography` 进行 3 公里范围查询：

```sql
-- 查询指定坐标 3000 米范围内的门店
SELECT
    id,
    store_name,
    longitude,
    latitude,
    ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC;
```

使用 `geometry` 判断门店是否在围栏内：

```sql
-- 查询位于指定围栏内的门店
SELECT
    s.id,
    s.store_name,
    f.fence_name
FROM store_location s
JOIN area_fence f
    ON ST_Within(s.geom, f.geom)
WHERE s.status = 1
  AND f.status = 1
  AND f.fence_code = 'FENCE_BJ_001';
```

项目中可以按以下原则选择：

| 业务需求                     | 推荐类型                                                    | 说明                              |
| ---------------------------- | ----------------------------------------------------------- | --------------------------------- |
| 保存门店、设备、用户当前位置 | `geometry(POINT, 4326)`                                     | 适合地图展示、空间关系判断        |
| 查询附近 N 米的数据          | `geography(POINT, 4326)`                                    | 距离单位直接使用米                |
| 判断点是否在区域内           | `geometry`                                                  | `ST_Within`、`ST_Contains` 更常见 |
| 保存行政区、围栏、配送范围   | `geometry(POLYGON, 4326)` 或 `geometry(MULTIPOLYGON, 4326)` | 区域数据优先使用 geometry         |
| 大规模本地空间分析           | `geometry` + 合适投影坐标系                                 | 性能和函数支持更好                |
| 跨城市、跨国家距离计算       | `geography`                                                 | 避免直接使用经纬度度数计算距离    |

实际项目中常见组合是：点位表同时保存 `geom` 和 `geog`，区域表只保存 `geom`。`geom` 负责空间关系判断和地图展示，`geog` 负责米级距离计算。

### 空间索引设计

空间索引用于提升范围查询、附近查询、空间关系判断等 SQL 的性能。PostGIS 使用基于 GiST 的 R-Tree 空间索引结构来索引 GIS 数据；当空间表数据量超过几千行后，通常就应考虑为空间字段创建索引。([PostGIS](https://www.postgis.net/docs/manual-3.0/using_postgis_dbmanagement.html?utm_source=chatgpt.com))

为点位表创建空间索引：

```sql
-- 为 geometry 点位字段创建 GiST 空间索引
CREATE INDEX idx_store_location_geom
ON store_location
USING GIST (geom);

-- 为 geography 点位字段创建 GiST 空间索引
CREATE INDEX idx_store_location_geog
ON store_location
USING GIST (geog);
```

为区域围栏表创建空间索引：

```sql
-- 为围栏区域字段创建 GiST 空间索引
CREATE INDEX idx_area_fence_geom
ON area_fence
USING GIST (geom);
```

如果业务中大部分查询只查启用状态的数据，可以创建部分索引，减少索引体积：

```sql
-- 只为启用状态的门店创建 geometry 空间索引
CREATE INDEX idx_store_location_geom_enabled
ON store_location
USING GIST (geom)
WHERE status = 1;

-- 只为启用状态的门店创建 geography 空间索引
CREATE INDEX idx_store_location_geog_enabled
ON store_location
USING GIST (geog)
WHERE status = 1;
```

部分索引适合数据中存在大量无效、禁用、历史记录的场景。需要注意，SQL 查询条件中必须包含与部分索引一致的条件，例如 `status = 1`，否则查询优化器可能不会使用该部分索引。

附近查询建议优先使用 `ST_DWithin` 做范围过滤，再使用 `ST_Distance` 做距离排序：

```sql
-- 推荐写法：先用 ST_DWithin 过滤范围，再用 ST_Distance 排序
SELECT
    id,
    store_name,
    ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC
LIMIT 20;
```

不建议直接对全表使用 `ST_Distance` 过滤：

```sql
-- 不推荐：容易导致大量数据参与距离计算
SELECT
    id,
    store_name
FROM store_location
WHERE ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) <= 3000;
```

原因是 `ST_Distance` 更适合计算距离值和排序，不适合作为大范围数据的第一层过滤条件。`ST_DWithin` 是索引友好的距离判断函数，更适合附近查询的 WHERE 条件。PostGIS 文档也说明 `ST_DWithin` 会使用边界框比较并利用可用索引。([PostGIS](https://www.postgis.net/docs/manual-3.5/ST_DWithin.html?utm_source=chatgpt.com))

空间索引创建后，建议执行统计信息更新，让 PostgreSQL 查询优化器掌握表和索引的数据分布情况：

```sql
-- 更新门店位置表统计信息
VACUUM ANALYZE store_location;

-- 更新区域围栏表统计信息
VACUUM ANALYZE area_fence;
```

PostGIS FAQ 中也强调，创建空间索引后应让 PostgreSQL 收集统计信息，以便查询规划器更合理地判断是否使用索引。([PostGIS](https://postgis.net/docs/manual-2.1/PostGIS_FAQ.html?utm_source=chatgpt.com))

可以使用 `EXPLAIN ANALYZE` 验证索引是否命中：

```sql
-- 查看附近查询的执行计划
EXPLAIN ANALYZE
SELECT
    id,
    store_name
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  );
```

执行计划中如果出现 `Index Scan`、`Bitmap Index Scan` 或对应的 GiST 索引名称，通常说明空间索引已经被使用。如果出现全表扫描，需要检查查询条件、字段类型、索引字段、SRID、类型转换方式是否一致。



## 核心功能使用

本节说明项目中最常用的 PostGIS 操作方式，包括空间数据写入、空间数据查询、距离计算、范围检索和空间关系判断。示例默认沿用前文的数据表：`store_location` 表用于保存门店点位，`area_fence` 表用于保存区域围栏。

### 空间数据写入

空间数据写入的核心是将经纬度、WKT、GeoJSON 等外部数据转换为 PostGIS 可识别的空间对象。对于经纬度点位，常用 `ST_MakePoint` 创建点对象，再通过 `ST_SetSRID` 指定坐标系。PostGIS 官方文档说明，`ST_MakePoint` 创建的是点几何对象，地理坐标中 `X` 表示经度，`Y` 表示纬度，因此项目中应统一使用 `ST_MakePoint(longitude, latitude)`。([PostGIS](https://postgis.net/docs/manual-3.5/en/ST_MakePoint.html?utm_source=chatgpt.com))

写入单个点位数据：

```sql
-- 写入门店点位数据
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog,
    status
)
VALUES (
    '北京示例门店',
    'STORE_BJ_001',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    1
);
```

批量写入点位数据：

```sql
-- 批量写入门店点位数据
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog,
    status
)
VALUES
(
    '北京示例门店',
    'STORE_BJ_001',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    1
),
(
    '上海示例门店',
    'STORE_SH_001',
    121.473701,
    31.230416,
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326),
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326)::geography,
    1
),
(
    '广州示例门店',
    'STORE_GZ_001',
    113.264385,
    23.129112,
    ST_SetSRID(ST_MakePoint(113.264385, 23.129112), 4326),
    ST_SetSRID(ST_MakePoint(113.264385, 23.129112), 4326)::geography,
    1
);
```

更新已有点位坐标：

```sql
-- 根据门店编码更新经纬度和空间字段
UPDATE store_location
SET
    longitude = 116.407526,
    latitude = 39.904030,
    geom = ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326),
    geog = ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326)::geography,
    update_time = CURRENT_TIMESTAMP
WHERE store_code = 'STORE_BJ_001';
```

使用 WKT 写入区域数据：

```sql
-- 使用 WKT 写入一个矩形围栏
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom,
    status
)
VALUES (
    '北京示例围栏',
    'FENCE_BJ_001',
    ST_GeomFromText(
        'MULTIPOLYGON(((116.390000 39.910000,116.410000 39.910000,116.410000 39.930000,116.390000 39.930000,116.390000 39.910000)))',
        4326
    ),
    1
);
```

使用 GeoJSON 写入区域数据时，通常需要传入 GeoJSON 的 `geometry` 片段，而不是完整 Feature 文档。PostGIS 文档说明，`ST_GeomFromGeoJSON` 用于从 GeoJSON 几何表示生成 PostGIS geometry 对象，并且只处理 JSON Geometry 片段。([PostGIS](https://postgis.net/docs/manual-2.0/ST_GeomFromGeoJSON.html?utm_source=chatgpt.com))

```sql
-- 使用 GeoJSON Geometry 片段写入围栏
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom,
    status
)
VALUES (
    'GeoJSON 示例围栏',
    'FENCE_GEOJSON_001',
    ST_SetSRID(
        ST_GeomFromGeoJSON(
            '{
                "type": "MultiPolygon",
                "coordinates": [[[
                    [116.390000, 39.910000],
                    [116.410000, 39.910000],
                    [116.410000, 39.930000],
                    [116.390000, 39.930000],
                    [116.390000, 39.910000]
                ]]]
            }'
        ),
        4326
    ),
    1
);
```

写入空间数据时应重点检查三点：经纬度顺序是否为“经度在前、纬度在后”；空间字段 SRID 是否统一；区域多边形是否闭合，即首尾坐标点是否一致。

### 空间数据查询

空间数据查询通常分为两类：一类是查询业务字段并附带返回经纬度，另一类是将空间字段转换为 WKT、GeoJSON 等格式返回给前端或其他系统。PostGIS 中空间字段本身不适合直接作为接口响应，一般需要使用函数转换为可读格式。

查询点位并返回经纬度：

```sql
-- 查询门店基础信息和经纬度
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    status
FROM store_location
WHERE status = 1
ORDER BY id ASC;
```

从 `geometry` 字段中提取经纬度：

```sql
-- 从 geometry 点位字段中提取经度和纬度
SELECT
    id,
    store_name,
    ST_X(geom) AS longitude,
    ST_Y(geom) AS latitude
FROM store_location
WHERE status = 1;
```

将空间字段转换为 WKT：

```sql
-- 查询门店点位并输出 WKT
SELECT
    id,
    store_name,
    ST_AsText(geom) AS geom_wkt
FROM store_location
WHERE status = 1;
```

返回示例：

```text
POINT(116.397128 39.916527)
```

将空间字段转换为 GeoJSON：

```sql
-- 查询围栏区域并输出 GeoJSON
SELECT
    id,
    fence_name,
    fence_code,
    ST_AsGeoJSON(geom) AS geom_geojson
FROM area_fence
WHERE status = 1;
```

按业务条件和空间字段同时查询：

```sql
-- 查询指定编码列表中的有效门店，并返回 GeoJSON 点位
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ST_AsGeoJSON(geom) AS geom_geojson
FROM store_location
WHERE status = 1
  AND store_code IN ('STORE_BJ_001', 'STORE_SH_001');
```

查询空间字段的 SRID 和几何类型：

```sql
-- 检查空间字段的 SRID 和几何类型
SELECT
    id,
    store_name,
    ST_SRID(geom) AS srid,
    GeometryType(geom) AS geometry_type
FROM store_location
WHERE status = 1;
```

这类查询适合用于开发联调和数据校验。正常业务接口通常不需要返回 `ST_SRID`、`GeometryType` 等调试字段，但在排查空间查询不命中、距离异常、区域判断错误时非常有用。

### 距离计算

距离计算用于附近门店、附近设备、最近人员、最近仓库等业务场景。PostGIS 中 `ST_Distance` 可以计算两个 `geometry` 或 `geography` 对象之间的距离；对于 `geometry`，返回单位取决于空间参考系统；对于 `geography`，默认返回米。([PostGIS](https://www.postgis.net/docs/manual-3.0/ST_Distance.html?utm_source=chatgpt.com))

计算两个指定坐标点之间的距离：

```sql
-- 使用 geography 计算两个经纬度点之间的距离，单位为米
SELECT ST_Distance(
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326)::geography
) AS distance_meter;
```

计算用户当前位置到门店的距离：

```sql
-- 查询所有启用门店到指定坐标的距离
SELECT
    id,
    store_name,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
ORDER BY distance_meter ASC;
```

查询最近的 10 个门店：

```sql
-- 查询距离指定坐标最近的 10 个门店
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
ORDER BY distance_meter ASC
LIMIT 10;
```

如果只是计算距离并排序，`ST_Distance` 可以直接使用。但如果业务要求“查询某个半径范围内的数据”，不建议只用 `ST_Distance <= 半径` 作为过滤条件。PostGIS 官方建议半径查询使用 `ST_DWithin`，因为它可以使用可用的空间索引。([PostGIS](https://postgis.net/documentation/tips/st-dwithin/?utm_source=chatgpt.com))

不推荐写法：

```sql
-- 不推荐：容易让大量数据参与距离计算
SELECT
    id,
    store_name
FROM store_location
WHERE ST_Distance(
    geog,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
) <= 3000;
```

推荐写法：

```sql
-- 推荐：先使用 ST_DWithin 做范围过滤，再使用 ST_Distance 计算和排序
SELECT
    id,
    store_name,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC;
```

### 范围检索

范围检索用于查询某个半径、矩形边界、行政区域或业务围栏内的数据。项目中最常见的是半径范围检索和多边形范围检索。

`ST_DWithin` 用于判断两个空间对象是否在指定距离内。对于 `geometry`，距离单位取决于 SRID；对于 `geography`，距离单位是米，并且该函数会包含边界框比较，从而利用可用索引。([PostGIS](https://www.postgis.net/docs/manual-3.5/ST_DWithin.html?utm_source=chatgpt.com))

查询 3 公里范围内的门店：

```sql
-- 查询指定坐标 3000 米范围内的门店
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC
LIMIT 20;
```

查询矩形范围内的门店：

```sql
-- 查询指定矩形边界内的门店
-- 参数含义：最小经度、最小纬度、最大经度、最大纬度、SRID
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude
FROM store_location
WHERE status = 1
  AND geom && ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326);
```

如果需要严格判断点是否位于矩形内部，可以在边界框过滤后增加 `ST_Within`：

```sql
-- 先用边界框过滤，再用 ST_Within 精确判断
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude
FROM store_location
WHERE status = 1
  AND geom && ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  AND ST_Within(
        geom,
        ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  );
```

查询指定围栏内的门店：

```sql
-- 查询某个围栏范围内的门店
SELECT
    s.id,
    s.store_name,
    s.store_code,
    s.longitude,
    s.latitude,
    f.fence_name
FROM store_location s
JOIN area_fence f
    ON ST_Within(s.geom, f.geom)
WHERE s.status = 1
  AND f.status = 1
  AND f.fence_code = 'FENCE_BJ_001';
```

查询与某个区域相交的围栏：

```sql
-- 查询与指定矩形区域相交的围栏
SELECT
    id,
    fence_name,
    fence_code,
    ST_AsText(geom) AS fence_wkt
FROM area_fence
WHERE status = 1
  AND ST_Intersects(
        geom,
        ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  );
```

矩形范围查询适合地图视野检索，例如前端地图拖动或缩放后，后端根据当前视野的 `minLng`、`minLat`、`maxLng`、`maxLat` 查询当前窗口内的数据。半径范围查询适合“附近 N 米”场景，多边形范围查询适合电子围栏、行政区域和服务区域场景。

### 空间关系判断

空间关系判断用于判断两个空间对象之间的拓扑关系，例如点是否在区域内、两个区域是否相交、线路是否穿过区域。常用函数包括 `ST_Within`、`ST_Contains`、`ST_Intersects`、`ST_Touches`、`ST_Crosses` 等。

常见函数选择如下：

| 函数                  | 判断含义              | 常见场景               |
| --------------------- | --------------------- | ---------------------- |
| `ST_Within(A, B)`     | A 是否在 B 内部       | 判断门店点是否在围栏内 |
| `ST_Contains(A, B)`   | A 是否包含 B          | 判断围栏是否包含某个点 |
| `ST_Intersects(A, B)` | A 和 B 是否有任意交集 | 判断两个区域是否相交   |
| `ST_Touches(A, B)`    | A 和 B 是否边界接触   | 判断区域是否相邻       |
| `ST_Crosses(A, B)`    | A 是否穿过 B          | 判断路线是否穿过区域   |
| `ST_Disjoint(A, B)`   | A 和 B 是否完全不相交 | 排除无关区域           |

`ST_Within` 用于判断 A 是否位于 B 内，要求两个几何对象使用相同 SRID 才有明确意义。([PostGIS](https://postgis.net/docs/ja/ST_Within.html?utm_source=chatgpt.com)) `ST_Contains` 与 `ST_Within` 是相反关系，即 `ST_Contains(A, B)` 等价于 `ST_Within(B, A)`。([PostGIS](https://www.postgis.net/docs/ST_Contains.html?utm_source=chatgpt.com))

判断指定坐标是否位于围栏内：

```sql
-- 判断指定点是否位于围栏内
SELECT
    id,
    fence_name,
    fence_code
FROM area_fence
WHERE status = 1
  AND ST_Contains(
        geom,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
  );
```

使用 `ST_Within` 实现相同判断：

```sql
-- 使用 ST_Within 判断点是否在围栏内
SELECT
    id,
    fence_name,
    fence_code
FROM area_fence
WHERE status = 1
  AND ST_Within(
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
        geom
  );
```

判断门店是否属于某个围栏：

```sql
-- 判断门店点位是否在指定围栏内
SELECT
    s.id,
    s.store_name,
    s.store_code,
    f.fence_name,
    ST_Within(s.geom, f.geom) AS in_fence
FROM store_location s
JOIN area_fence f
    ON f.fence_code = 'FENCE_BJ_001'
WHERE s.store_code = 'STORE_BJ_001';
```

查询所有落入围栏的门店：

```sql
-- 查询所有命中围栏的门店和围栏关系
SELECT
    s.id AS store_id,
    s.store_name,
    s.store_code,
    f.id AS fence_id,
    f.fence_name,
    f.fence_code
FROM store_location s
JOIN area_fence f
    ON ST_Within(s.geom, f.geom)
WHERE s.status = 1
  AND f.status = 1;
```

判断两个区域是否相交：

```sql
-- 判断两个围栏区域是否存在交集
SELECT
    a.fence_name AS fence_name_a,
    b.fence_name AS fence_name_b,
    ST_Intersects(a.geom, b.geom) AS is_intersects
FROM area_fence a
JOIN area_fence b
    ON a.id < b.id
WHERE a.status = 1
  AND b.status = 1;
```

`ST_Intersects` 用于判断两个几何对象是否至少有一个公共点；该函数也会自动包含边界框比较，从而使用可用的空间索引。([PostGIS](https://postgis.net/docs/en/ST_Intersects.html?utm_source=chatgpt.com))

查询与指定围栏相交的其他围栏：

```sql
-- 查询与指定围栏存在交集的其他围栏
SELECT
    target.fence_name AS target_fence_name,
    other_fence.id,
    other_fence.fence_name,
    other_fence.fence_code
FROM area_fence target
JOIN area_fence other_fence
    ON target.id <> other_fence.id
   AND ST_Intersects(target.geom, other_fence.geom)
WHERE target.fence_code = 'FENCE_BJ_001'
  AND target.status = 1
  AND other_fence.status = 1;
```

判断路线是否穿过围栏：

```sql
-- 判断一条路线是否穿过指定围栏
SELECT
    id,
    fence_name,
    fence_code,
    ST_Crosses(
        ST_GeomFromText(
            'LINESTRING(116.380000 39.900000,116.420000 39.940000)',
            4326
        ),
        geom
    ) AS is_crosses
FROM area_fence
WHERE status = 1
  AND fence_code = 'FENCE_BJ_001';
```

空间关系判断时需要注意，`ST_Within`、`ST_Contains`、`ST_Intersects` 等函数对几何对象的边界、内部和拓扑关系有严格定义。对于“点刚好落在边界上”的业务场景，需要提前明确是否算作命中。如果业务要求边界也算命中，通常可以优先使用 `ST_Intersects`；如果要求点必须在区域内部，则使用 `ST_Within` 或结合业务规则进一步处理。



## Spring Boot 项目集成

本节说明 Spring Boot 项目如何集成 PostgreSQL + PostGIS。示例默认使用 Spring Boot 3、PostgreSQL JDBC、MyBatis-Plus 和 Hutool。由于 PostGIS 的空间函数主要通过 SQL 执行，项目中建议由 Mapper XML 统一封装空间 SQL，Java 实体类只维护业务字段、经纬度字段和查询结果字段，避免在业务层直接拼接空间函数。

### 依赖配置

Spring Boot 项目需要引入 Web、PostgreSQL 驱动、MyBatis-Plus、Hutool 和 Lombok。PostGIS 不需要单独的 Java 驱动，它是 PostgreSQL 数据库扩展，Java 项目通过 PostgreSQL JDBC 访问即可。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Web 接口能力，用于提供位置保存、附近查询、范围查询等接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验能力，用于校验经纬度、半径、分页等请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL JDBC 驱动，Spring Boot 通过它连接 PostgreSQL/PostGIS -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter，用于简化 Mapper 和基础 CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- Hutool 工具包，用于对象判断、数字处理、集合处理等通用能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，用于减少 Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖，用于单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

版本可以在 `properties` 中统一管理：

```xml
<properties>
    <java.version>17</java.version>

    <!-- MyBatis-Plus 版本，项目中建议统一由父工程或依赖管理控制 -->
    <mybatis-plus.version>3.5.7</mybatis-plus.version>

    <!-- Hutool 工具包版本 -->
    <hutool.version>5.8.32</hutool.version>
</properties>
```

如果项目已经有统一的父级 BOM 或内部依赖管理平台，可以不在业务模块中直接写死版本，改为由父工程统一控制。

### 数据源配置

数据源配置主要包括 PostgreSQL 连接信息、连接池配置、MyBatis-Plus 配置和 Mapper XML 扫描路径。PostGIS 是数据库扩展能力，不需要在 `application.yml` 中单独声明，但必须保证目标数据库已经执行过 `CREATE EXTENSION postgis;`。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: postgis-demo

  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/postgis_demo
    username: postgis_user
    password: PostGIS@123456

    hikari:
      # 连接池名称，便于日志和监控识别
      pool-name: PostgisHikariPool
      # 最小空闲连接数
      minimum-idle: 5
      # 最大连接数，需结合数据库 max_connections 设置
      maximum-pool-size: 20
      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000

mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml

  # 实体类包路径
  type-aliases-package: io.github.atengk.postgis.entity

  configuration:
    # 开启下划线转驼峰映射，例如 store_name -> storeName
    map-underscore-to-camel-case: true

    # 开发环境可开启 SQL 日志；生产环境建议关闭或接入专业 SQL 监控
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # PostgreSQL 自增主键一般使用 BIGSERIAL 或 IDENTITY
      id-type: auto
```

建议增加一个启动校验 SQL，用于确认当前数据库已经启用 PostGIS。

文件位置：`src/main/java/io/github/atengk/postgis/config/PostgisCheckRunner.java`

这个启动检查类会在项目启动后查询 PostGIS 版本，便于提前发现数据库扩展未启用的问题。

```java
package io.github.atengk.postgis.config;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostGIS 扩展启动检查
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgisCheckRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 项目启动后检查当前数据库是否已启用 PostGIS
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            String version = jdbcTemplate.queryForObject("SELECT PostGIS_Full_Version()", String.class);
            if (CharSequenceUtil.isBlank(version)) {
                log.warn("PostGIS 扩展检查异常：未获取到版本信息");
                return;
            }
            log.info("PostGIS 扩展检查通过：{}", version);
        } catch (Exception e) {
            log.error("PostGIS 扩展检查失败，请确认数据库已执行 CREATE EXTENSION postgis", e);
            throw e;
        }
    }
}
```

该检查适合开发、测试和生产环境保留。若生产环境不希望启动时执行额外 SQL，可以通过配置项控制该检查是否启用。

### 实体字段映射

实体字段映射建议采用“业务字段直接映射，空间字段由 SQL 维护”的方式。原因是 `geometry` 和 `geography` 是 PostgreSQL/PostGIS 的空间类型，Java 实体中直接映射会涉及 PGobject、JTS Geometry 或自定义 TypeHandler，复杂度较高。多数业务系统只需要保存经纬度、查询距离和返回 GeoJSON，因此可以让 Mapper XML 负责空间字段写入和读取转换。

示例表结构如下：

```sql
-- 门店位置表
CREATE TABLE store_location (
    id BIGSERIAL PRIMARY KEY,

    -- 门店名称
    store_name VARCHAR(100) NOT NULL,

    -- 门店编码
    store_code VARCHAR(64) NOT NULL UNIQUE,

    -- 经度
    longitude NUMERIC(10, 6) NOT NULL,

    -- 纬度
    latitude NUMERIC(10, 6) NOT NULL,

    -- geometry 点位字段，用于空间关系判断和地图展示
    geom geometry(POINT, 4326) NOT NULL,

    -- geography 点位字段，用于米级距离计算
    geog geography(POINT, 4326) NOT NULL,

    -- 状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

实体类只映射普通字段，空间字段不直接映射为 Java 对象。查询时如果需要返回 `GeoJSON`、`WKT` 或距离字段，可以通过 `@TableField(exist = false)` 声明非表字段。

文件位置：`src/main/java/io/github/atengk/postgis/entity/StoreLocationEntity.java`

这个实体类映射门店位置表的普通字段，并额外承载距离、WKT 和 GeoJSON 查询结果。

```java
package io.github.atengk.postgis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店位置实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@TableName("store_location")
public class StoreLocationEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 门店名称
     */
    private String storeName;

    /**
     * 门店编码
     */
    private String storeCode;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 距离，单位米，由 SQL 查询计算返回
     */
    @TableField(exist = false)
    private BigDecimal distanceMeter;

    /**
     * geometry 的 WKT 文本，由 SQL 查询转换返回
     */
    @TableField(exist = false)
    private String geomWkt;

    /**
     * geometry 的 GeoJSON 文本，由 SQL 查询转换返回
     */
    @TableField(exist = false)
    private String geomGeoJson;
}
```

新增门店请求对象：

文件位置：`src/main/java/io/github/atengk/postgis/dto/StoreLocationCreateDTO.java`

这个 DTO 用于接收新增门店位置的请求参数，并通过注解校验经纬度范围。

```java
package io.github.atengk.postgis.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店位置新增请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class StoreLocationCreateDTO {

    /**
     * 门店名称
     */
    @NotBlank(message = "门店名称不能为空")
    private String storeName;

    /**
     * 门店编码
     */
    @NotBlank(message = "门店编码不能为空")
    private String storeCode;

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "经度不能大于 180")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "纬度不能大于 90")
    private BigDecimal latitude;
}
```

附近门店查询请求对象：

文件位置：`src/main/java/io/github/atengk/postgis/dto/NearbyStoreQueryDTO.java`

这个 DTO 用于接收附近查询参数，包括当前经纬度、查询半径和返回数量。

```java
package io.github.atengk.postgis.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 附近门店查询请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class NearbyStoreQueryDTO {

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "经度不能大于 180")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "纬度不能大于 90")
    private BigDecimal latitude;

    /**
     * 查询半径，单位米
     */
    @NotNull(message = "查询半径不能为空")
    @Min(value = 1, message = "查询半径不能小于 1 米")
    @Max(value = 100000, message = "查询半径不能大于 100000 米")
    private Integer radiusMeter;

    /**
     * 返回数量
     */
    @NotNull(message = "返回数量不能为空")
    @Min(value = 1, message = "返回数量不能小于 1")
    @Max(value = 100, message = "返回数量不能大于 100")
    private Integer limit;
}
```

返回对象可以和实体解耦，避免直接暴露数据库字段。

文件位置：`src/main/java/io/github/atengk/postgis/vo/StoreLocationVO.java`

这个 VO 用于向前端返回门店位置、距离和 GeoJSON 数据。

```java
package io.github.atengk.postgis.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店位置响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class StoreLocationVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 门店名称
     */
    private String storeName;

    /**
     * 门店编码
     */
    private String storeCode;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 距离，单位米
     */
    private BigDecimal distanceMeter;

    /**
     * GeoJSON 点位
     */
    private String geomGeoJson;
}
```

这种映射方式的优势是简单、稳定、容易排查。空间字段的生成和转换全部在 SQL 层完成，Java 层只处理普通业务字段和查询结果。

### SQL 查询封装

PostGIS 查询建议统一封装在 Mapper XML 中，而不是在 Service 层拼接 SQL。这样可以集中管理 `ST_MakePoint`、`ST_SetSRID`、`ST_DWithin`、`ST_Distance`、`ST_AsGeoJSON` 等空间函数，降低业务代码复杂度。

Mapper 接口定义如下：

文件位置：`src/main/java/io/github/atengk/postgis/mapper/StoreLocationMapper.java`

这个 Mapper 接口封装门店位置新增、附近查询、矩形范围查询和围栏内查询。

```java
package io.github.atengk.postgis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.postgis.dto.StoreLocationCreateDTO;
import io.github.atengk.postgis.entity.StoreLocationEntity;
import io.github.atengk.postgis.vo.StoreLocationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店位置 Mapper
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Mapper
public interface StoreLocationMapper extends BaseMapper<StoreLocationEntity> {

    /**
     * 新增门店位置
     *
     * @param dto 新增请求
     * @return 影响行数
     */
    int insertWithPostgis(@Param("dto") StoreLocationCreateDTO dto);

    /**
     * 查询附近门店
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusMeter 查询半径，单位米
     * @param limit 返回数量
     * @return 附近门店列表
     */
    List<StoreLocationVO> selectNearbyStores(@Param("longitude") BigDecimal longitude,
                                             @Param("latitude") BigDecimal latitude,
                                             @Param("radiusMeter") Integer radiusMeter,
                                             @Param("limit") Integer limit);

    /**
     * 查询矩形范围内的门店
     *
     * @param minLongitude 最小经度
     * @param minLatitude 最小纬度
     * @param maxLongitude 最大经度
     * @param maxLatitude 最大纬度
     * @return 门店列表
     */
    List<StoreLocationVO> selectStoresInEnvelope(@Param("minLongitude") BigDecimal minLongitude,
                                                 @Param("minLatitude") BigDecimal minLatitude,
                                                 @Param("maxLongitude") BigDecimal maxLongitude,
                                                 @Param("maxLatitude") BigDecimal maxLatitude);

    /**
     * 查询指定围栏内的门店
     *
     * @param fenceCode 围栏编码
     * @return 门店列表
     */
    List<StoreLocationVO> selectStoresInFence(@Param("fenceCode") String fenceCode);
}
```

Mapper XML 负责具体 PostGIS SQL。注意 `ST_MakePoint` 的参数顺序是经度在前、纬度在后。

文件位置：`src/main/resources/mapper/StoreLocationMapper.xml`

这个 XML 文件集中封装 PostGIS 写入、附近查询、矩形范围查询和围栏查询 SQL。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.postgis.mapper.StoreLocationMapper">

    <!-- 门店位置返回字段映射 -->
    <resultMap id="StoreLocationVOMap" type="io.github.atengk.postgis.vo.StoreLocationVO">
        <id property="id" column="id"/>
        <result property="storeName" column="store_name"/>
        <result property="storeCode" column="store_code"/>
        <result property="longitude" column="longitude"/>
        <result property="latitude" column="latitude"/>
        <result property="distanceMeter" column="distance_meter"/>
        <result property="geomGeoJson" column="geom_geojson"/>
    </resultMap>

    <!-- 新增门店位置，同时写入普通经纬度字段、geometry 字段和 geography 字段 -->
    <insert id="insertWithPostgis">
        INSERT INTO store_location (
            store_name,
            store_code,
            longitude,
            latitude,
            geom,
            geog,
            status,
            create_time,
            update_time
        )
        VALUES (
            #{dto.storeName},
            #{dto.storeCode},
            #{dto.longitude},
            #{dto.latitude},
            ST_SetSRID(ST_MakePoint(#{dto.longitude}, #{dto.latitude}), 4326),
            ST_SetSRID(ST_MakePoint(#{dto.longitude}, #{dto.latitude}), 4326)::geography,
            1,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
    </insert>

    <!-- 查询指定坐标附近的门店，先用 ST_DWithin 过滤，再用 ST_Distance 排序 -->
    <select id="selectNearbyStores" resultMap="StoreLocationVOMap">
        SELECT
            id,
            store_name,
            store_code,
            longitude,
            latitude,
            ROUND(
                ST_Distance(
                    geog,
                    ST_SetSRID(ST_MakePoint(#{longitude}, #{latitude}), 4326)::geography
                )::numeric,
                2
            ) AS distance_meter,
            ST_AsGeoJSON(geom) AS geom_geojson
        FROM store_location
        WHERE status = 1
          AND ST_DWithin(
                geog,
                ST_SetSRID(ST_MakePoint(#{longitude}, #{latitude}), 4326)::geography,
                #{radiusMeter}
          )
        ORDER BY distance_meter ASC
        LIMIT #{limit}
    </select>

    <!-- 查询矩形边界范围内的门店，适合地图视野范围查询 -->
    <select id="selectStoresInEnvelope" resultMap="StoreLocationVOMap">
        SELECT
            id,
            store_name,
            store_code,
            longitude,
            latitude,
            NULL AS distance_meter,
            ST_AsGeoJSON(geom) AS geom_geojson
        FROM store_location
        WHERE status = 1
          AND geom &amp;&amp; ST_MakeEnvelope(
                #{minLongitude},
                #{minLatitude},
                #{maxLongitude},
                #{maxLatitude},
                4326
          )
          AND ST_Within(
                geom,
                ST_MakeEnvelope(
                    #{minLongitude},
                    #{minLatitude},
                    #{maxLongitude},
                    #{maxLatitude},
                    4326
                )
          )
        ORDER BY id ASC
    </select>

    <!-- 查询指定围栏内的门店，area_fence.geom 使用 MULTIPOLYGON 类型 -->
    <select id="selectStoresInFence" resultMap="StoreLocationVOMap">
        SELECT
            s.id,
            s.store_name,
            s.store_code,
            s.longitude,
            s.latitude,
            NULL AS distance_meter,
            ST_AsGeoJSON(s.geom) AS geom_geojson
        FROM store_location s
        JOIN area_fence f
            ON ST_Within(s.geom, f.geom)
        WHERE s.status = 1
          AND f.status = 1
          AND f.fence_code = #{fenceCode}
        ORDER BY s.id ASC
    </select>

</mapper>
```

Service 层负责参数校验、日志记录和业务编排，不直接拼接 PostGIS SQL。

文件位置：`src/main/java/io/github/atengk/postgis/service/StoreLocationService.java`

这个 Service 接口定义门店位置新增和空间查询能力。

```java
package io.github.atengk.postgis.service;

import io.github.atengk.postgis.dto.NearbyStoreQueryDTO;
import io.github.atengk.postgis.dto.StoreLocationCreateDTO;
import io.github.atengk.postgis.vo.StoreLocationVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店位置服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface StoreLocationService {

    /**
     * 新增门店位置
     *
     * @param dto 新增请求
     */
    void createStoreLocation(StoreLocationCreateDTO dto);

    /**
     * 查询附近门店
     *
     * @param dto 查询请求
     * @return 附近门店列表
     */
    List<StoreLocationVO> listNearbyStores(NearbyStoreQueryDTO dto);

    /**
     * 查询矩形范围内的门店
     *
     * @param minLongitude 最小经度
     * @param minLatitude 最小纬度
     * @param maxLongitude 最大经度
     * @param maxLatitude 最大纬度
     * @return 门店列表
     */
    List<StoreLocationVO> listStoresInEnvelope(BigDecimal minLongitude,
                                               BigDecimal minLatitude,
                                               BigDecimal maxLongitude,
                                               BigDecimal maxLatitude);

    /**
     * 查询围栏内的门店
     *
     * @param fenceCode 围栏编码
     * @return 门店列表
     */
    List<StoreLocationVO> listStoresInFence(String fenceCode);
}
```

Service 实现类通过 Mapper 调用空间 SQL，并使用 Hutool 做基础对象判断。

文件位置：`src/main/java/io/github/atengk/postgis/service/impl/StoreLocationServiceImpl.java`

这个实现类完成门店位置写入和空间查询调用，并记录关键业务日志。

```java
package io.github.atengk.postgis.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.postgis.dto.NearbyStoreQueryDTO;
import io.github.atengk.postgis.dto.StoreLocationCreateDTO;
import io.github.atengk.postgis.mapper.StoreLocationMapper;
import io.github.atengk.postgis.service.StoreLocationService;
import io.github.atengk.postgis.vo.StoreLocationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店位置服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreLocationServiceImpl implements StoreLocationService {

    private final StoreLocationMapper storeLocationMapper;

    /**
     * 新增门店位置
     *
     * @param dto 新增请求
     */
    @Override
    public void createStoreLocation(StoreLocationCreateDTO dto) {
        Assert.notNull(dto, "门店位置新增请求不能为空");

        int rows = storeLocationMapper.insertWithPostgis(dto);
        log.info("新增门店位置完成，门店编码：{}，影响行数：{}", dto.getStoreCode(), rows);
    }

    /**
     * 查询附近门店
     *
     * @param dto 查询请求
     * @return 附近门店列表
     */
    @Override
    public List<StoreLocationVO> listNearbyStores(NearbyStoreQueryDTO dto) {
        Assert.notNull(dto, "附近门店查询请求不能为空");

        List<StoreLocationVO> list = storeLocationMapper.selectNearbyStores(
                dto.getLongitude(),
                dto.getLatitude(),
                dto.getRadiusMeter(),
                dto.getLimit()
        );

        log.info("附近门店查询完成，经度：{}，纬度：{}，半径：{}米，返回数量：{}",
                dto.getLongitude(), dto.getLatitude(), dto.getRadiusMeter(), list.size());
        return list;
    }

    /**
     * 查询矩形范围内的门店
     *
     * @param minLongitude 最小经度
     * @param minLatitude 最小纬度
     * @param maxLongitude 最大经度
     * @param maxLatitude 最大纬度
     * @return 门店列表
     */
    @Override
    public List<StoreLocationVO> listStoresInEnvelope(BigDecimal minLongitude,
                                                      BigDecimal minLatitude,
                                                      BigDecimal maxLongitude,
                                                      BigDecimal maxLatitude) {
        Assert.notNull(minLongitude, "最小经度不能为空");
        Assert.notNull(minLatitude, "最小纬度不能为空");
        Assert.notNull(maxLongitude, "最大经度不能为空");
        Assert.notNull(maxLatitude, "最大纬度不能为空");

        List<StoreLocationVO> list = storeLocationMapper.selectStoresInEnvelope(
                minLongitude,
                minLatitude,
                maxLongitude,
                maxLatitude
        );

        log.info("矩形范围门店查询完成，范围：[{}, {}, {}, {}]，返回数量：{}",
                minLongitude, minLatitude, maxLongitude, maxLatitude, list.size());
        return list;
    }

    /**
     * 查询围栏内的门店
     *
     * @param fenceCode 围栏编码
     * @return 门店列表
     */
    @Override
    public List<StoreLocationVO> listStoresInFence(String fenceCode) {
        Assert.isTrue(CharSequenceUtil.isNotBlank(fenceCode), "围栏编码不能为空");

        List<StoreLocationVO> list = storeLocationMapper.selectStoresInFence(fenceCode);
        log.info("围栏内门店查询完成，围栏编码：{}，返回数量：{}", fenceCode, list.size());
        return list;
    }
}
```

如果需要快速验证 Mapper 是否正常，可以先写一个简单的测试类。

文件位置：`src/test/java/io/github/atengk/postgis/StoreLocationMapperTest.java`

这个测试类用于验证门店新增和附近查询是否可以正常执行。

```java
package io.github.atengk.postgis;

import io.github.atengk.postgis.dto.NearbyStoreQueryDTO;
import io.github.atengk.postgis.dto.StoreLocationCreateDTO;
import io.github.atengk.postgis.service.StoreLocationService;
import io.github.atengk.postgis.vo.StoreLocationVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店位置 Mapper 测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootTest
class StoreLocationMapperTest {

    @Resource
    private StoreLocationService storeLocationService;

    /**
     * 测试新增门店位置
     */
    @Test
    void testCreateStoreLocation() {
        StoreLocationCreateDTO dto = new StoreLocationCreateDTO();
        dto.setStoreName("北京测试门店");
        dto.setStoreCode("STORE_TEST_BJ_001");
        dto.setLongitude(new BigDecimal("116.397128"));
        dto.setLatitude(new BigDecimal("39.916527"));

        storeLocationService.createStoreLocation(dto);
    }

    /**
     * 测试查询附近门店
     */
    @Test
    void testListNearbyStores() {
        NearbyStoreQueryDTO dto = new NearbyStoreQueryDTO();
        dto.setLongitude(new BigDecimal("116.397128"));
        dto.setLatitude(new BigDecimal("39.916527"));
        dto.setRadiusMeter(3000);
        dto.setLimit(10);

        List<StoreLocationVO> list = storeLocationService.listNearbyStores(dto);
        list.forEach(System.out::println);
    }
}
```

SQL 封装时建议遵循以下原则：

| 原则                           | 说明                                                   |
| ------------------------------ | ------------------------------------------------------ |
| 空间 SQL 集中在 Mapper XML     | 避免 Service 层拼接 `ST_DWithin`、`ST_Distance` 等函数 |
| 写入时同步维护经纬度和空间字段 | 保证普通字段、`geom`、`geog` 数据一致                  |
| 附近查询优先使用 `ST_DWithin`  | 先过滤范围，再计算距离和排序                           |
| 距离单位统一使用米             | 建议使用 `geography` 字段做米级距离计算                |
| 坐标顺序统一                   | 始终使用 `ST_MakePoint(longitude, latitude)`           |
| 前端展示返回 GeoJSON           | 使用 `ST_AsGeoJSON(geom)` 转换空间字段                 |
| 排查问题返回 WKT               | 使用 `ST_AsText(geom)` 便于人工阅读和调试              |

完成以上配置后，Spring Boot 项目就可以通过 Mapper XML 直接调用 PostGIS 能力，实现点位写入、附近查询、地图视野查询和围栏内查询等核心业务功能。



## 常用 SQL 示例

本节汇总项目中最常用的 PostGIS SQL，包括创建空间表、创建空间索引、插入空间数据、查询附近数据和查询区域内数据。示例默认使用 `SRID=4326`，即 WGS84 经纬度坐标系；点位写入统一使用 `ST_MakePoint(longitude, latitude)`，也就是经度在前、纬度在后。

### 创建空间表

空间表通常由普通业务字段和空间字段共同组成。普通业务字段用于业务筛选，空间字段用于空间查询、距离计算、范围检索和区域判断。

创建门店位置表：

```sql
-- 删除旧表，开发环境可用；生产环境谨慎执行
DROP TABLE IF EXISTS store_location;

-- 创建门店位置表
CREATE TABLE store_location (
    id BIGSERIAL PRIMARY KEY,

    -- 门店名称
    store_name VARCHAR(100) NOT NULL,

    -- 门店编码，用于业务系统唯一标识门店
    store_code VARCHAR(64) NOT NULL UNIQUE,

    -- 经度，取值范围：-180 到 180
    longitude NUMERIC(10, 6) NOT NULL,

    -- 纬度，取值范围：-90 到 90
    latitude NUMERIC(10, 6) NOT NULL,

    -- geometry 点位字段，适合地图展示、区域判断、空间关系判断
    geom geometry(POINT, 4326) NOT NULL,

    -- geography 点位字段，适合按照米进行距离计算
    geog geography(POINT, 4326) NOT NULL,

    -- 业务状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 经度范围约束
    CONSTRAINT chk_store_location_longitude
        CHECK (longitude >= -180 AND longitude <= 180),

    -- 纬度范围约束
    CONSTRAINT chk_store_location_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    -- geometry 字段坐标系约束
    CONSTRAINT chk_store_location_geom_srid
        CHECK (ST_SRID(geom) = 4326)
);
```

创建区域围栏表：

```sql
-- 删除旧表，开发环境可用；生产环境谨慎执行
DROP TABLE IF EXISTS area_fence;

-- 创建区域围栏表
CREATE TABLE area_fence (
    id BIGSERIAL PRIMARY KEY,

    -- 围栏名称
    fence_name VARCHAR(100) NOT NULL,

    -- 围栏编码，用于业务系统唯一标识围栏
    fence_code VARCHAR(64) NOT NULL UNIQUE,

    -- 围栏区域，使用 MULTIPOLYGON 兼容单区域和多区域
    geom geometry(MULTIPOLYGON, 4326) NOT NULL,

    -- 业务状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- geometry 字段坐标系约束
    CONSTRAINT chk_area_fence_geom_srid
        CHECK (ST_SRID(geom) = 4326)
);
```

如果业务中只需要点位附近查询，可以先创建 `store_location` 表。如果业务涉及电子围栏、行政区、服务范围、配送范围等区域判断，则需要同时创建 `area_fence` 表。

### 创建空间索引

空间索引用于提升 `ST_DWithin`、`ST_Within`、`ST_Contains`、`ST_Intersects`、边界框检索等空间查询的性能。PostGIS 中常用 GiST 索引处理空间字段。

为门店位置表创建索引：

```sql
-- 为 geometry 点位字段创建 GiST 空间索引
CREATE INDEX idx_store_location_geom
ON store_location
USING GIST (geom);

-- 为 geography 点位字段创建 GiST 空间索引
CREATE INDEX idx_store_location_geog
ON store_location
USING GIST (geog);

-- 为门店编码创建普通唯一索引，通常已由 UNIQUE 约束自动创建
CREATE UNIQUE INDEX idx_store_location_store_code
ON store_location (store_code);

-- 为状态字段创建普通索引，适合经常按启用状态过滤的场景
CREATE INDEX idx_store_location_status
ON store_location (status);
```

为区域围栏表创建索引：

```sql
-- 为围栏区域字段创建 GiST 空间索引
CREATE INDEX idx_area_fence_geom
ON area_fence
USING GIST (geom);

-- 为围栏编码创建普通唯一索引，通常已由 UNIQUE 约束自动创建
CREATE UNIQUE INDEX idx_area_fence_fence_code
ON area_fence (fence_code);

-- 为状态字段创建普通索引
CREATE INDEX idx_area_fence_status
ON area_fence (status);
```

如果表中存在大量禁用或历史数据，可以创建部分索引，只索引启用状态的数据：

```sql
-- 只为启用门店创建 geography 空间索引，适合附近查询
CREATE INDEX idx_store_location_geog_enabled
ON store_location
USING GIST (geog)
WHERE status = 1;

-- 只为启用门店创建 geometry 空间索引，适合范围和区域判断
CREATE INDEX idx_store_location_geom_enabled
ON store_location
USING GIST (geom)
WHERE status = 1;

-- 只为启用围栏创建空间索引
CREATE INDEX idx_area_fence_geom_enabled
ON area_fence
USING GIST (geom)
WHERE status = 1;
```

创建索引后建议更新统计信息：

```sql
-- 更新门店位置表统计信息
VACUUM ANALYZE store_location;

-- 更新区域围栏表统计信息
VACUUM ANALYZE area_fence;
```

使用 `EXPLAIN ANALYZE` 可以检查空间索引是否生效：

```sql
-- 查看附近查询执行计划
EXPLAIN ANALYZE
SELECT
    id,
    store_name
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  );
```

执行计划中如果出现 `Index Scan`、`Bitmap Index Scan` 或对应 GiST 索引名称，通常表示空间索引已经被使用。

### 插入空间数据

插入点位数据时，需要同时写入普通经纬度字段、`geometry` 字段和 `geography` 字段。这样既方便普通业务展示，也方便空间查询和米级距离计算。

插入单条门店点位数据：

```sql
-- 插入北京门店点位
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog,
    status
)
VALUES (
    '北京示例门店',
    'STORE_BJ_001',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    1
);
```

批量插入门店点位数据：

```sql
-- 批量插入多个城市的门店点位
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog,
    status
)
VALUES
(
    '北京示例门店',
    'STORE_BJ_001',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    1
),
(
    '上海示例门店',
    'STORE_SH_001',
    121.473701,
    31.230416,
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326),
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326)::geography,
    1
),
(
    '广州示例门店',
    'STORE_GZ_001',
    113.264385,
    23.129112,
    ST_SetSRID(ST_MakePoint(113.264385, 23.129112), 4326),
    ST_SetSRID(ST_MakePoint(113.264385, 23.129112), 4326)::geography,
    1
),
(
    '深圳示例门店',
    'STORE_SZ_001',
    114.057868,
    22.543099,
    ST_SetSRID(ST_MakePoint(114.057868, 22.543099), 4326),
    ST_SetSRID(ST_MakePoint(114.057868, 22.543099), 4326)::geography,
    1
);
```

更新门店点位：

```sql
-- 根据门店编码更新门店经纬度和空间字段
UPDATE store_location
SET
    longitude = 116.407526,
    latitude = 39.904030,
    geom = ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326),
    geog = ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326)::geography,
    update_time = CURRENT_TIMESTAMP
WHERE store_code = 'STORE_BJ_001';
```

插入区域围栏数据：

```sql
-- 插入一个矩形围栏区域
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom,
    status
)
VALUES (
    '北京示例围栏',
    'FENCE_BJ_001',
    ST_GeomFromText(
        'MULTIPOLYGON(((116.390000 39.910000,116.410000 39.910000,116.410000 39.930000,116.390000 39.930000,116.390000 39.910000)))',
        4326
    ),
    1
);
```

使用 GeoJSON 插入区域围栏数据：

```sql
-- 使用 GeoJSON Geometry 写入围栏区域
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom,
    status
)
VALUES (
    'GeoJSON 示例围栏',
    'FENCE_GEOJSON_001',
    ST_SetSRID(
        ST_GeomFromGeoJSON(
            '{
                "type": "MultiPolygon",
                "coordinates": [[[
                    [116.390000, 39.910000],
                    [116.410000, 39.910000],
                    [116.410000, 39.930000],
                    [116.390000, 39.930000],
                    [116.390000, 39.910000]
                ]]]
            }'
        ),
        4326
    ),
    1
);
```

插入数据后可以通过以下 SQL 检查空间字段是否正确：

```sql
-- 检查门店点位数据
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ST_AsText(geom) AS geom_wkt,
    ST_SRID(geom) AS srid
FROM store_location
ORDER BY id ASC;

-- 检查围栏区域数据
SELECT
    id,
    fence_name,
    fence_code,
    ST_AsText(geom) AS geom_wkt,
    ST_SRID(geom) AS srid
FROM area_fence
ORDER BY id ASC;
```

### 查询附近数据

附近查询通常使用 `ST_DWithin` 进行半径过滤，再使用 `ST_Distance` 计算距离并排序。项目中建议使用 `geography` 字段进行米级距离计算，避免 `geometry(POINT, 4326)` 直接按“度”计算距离导致结果不直观。

查询指定坐标 3 公里范围内的门店：

```sql
-- 查询指定坐标 3000 米范围内的门店，并按距离升序排序
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter,
    ST_AsGeoJSON(geom) AS geom_geojson
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC
LIMIT 20;
```

查询最近的 10 个门店：

```sql
-- 查询距离指定坐标最近的 10 个门店
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
ORDER BY distance_meter ASC
LIMIT 10;
```

查询指定范围内的门店，并按距离分段：

```sql
-- 查询 5000 米范围内的门店，并按距离分段显示
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(distance_meter::numeric, 2) AS distance_meter,
    CASE
        WHEN distance_meter <= 1000 THEN '1公里以内'
        WHEN distance_meter <= 3000 THEN '1-3公里'
        WHEN distance_meter <= 5000 THEN '3-5公里'
        ELSE '5公里以上'
    END AS distance_level
FROM (
    SELECT
        id,
        store_name,
        store_code,
        longitude,
        latitude,
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        ) AS distance_meter
    FROM store_location
    WHERE status = 1
      AND ST_DWithin(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
            5000
      )
) t
ORDER BY distance_meter ASC;
```

分页查询附近门店：

```sql
-- 分页查询 3000 米范围内的门店
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC
LIMIT 10 OFFSET 0;
```

附近查询的推荐写法是先用 `ST_DWithin` 限制候选数据范围，再计算 `ST_Distance`。不建议直接在全表上使用 `ST_Distance(...) <= 半径` 作为主要过滤条件。

### 查询区域内数据

区域内查询通常用于电子围栏、行政区、商圈、配送范围、地图视野范围等业务。常见方式包括矩形范围查询、多边形范围查询和围栏内查询。

查询矩形范围内的门店：

```sql
-- 查询指定矩形边界内的门店
-- 参数顺序：最小经度、最小纬度、最大经度、最大纬度、SRID
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude,
    ST_AsGeoJSON(geom) AS geom_geojson
FROM store_location
WHERE status = 1
  AND geom && ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  AND ST_Within(
        geom,
        ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  )
ORDER BY id ASC;
```

查询指定围栏内的门店：

```sql
-- 查询指定围栏内的所有门店
SELECT
    s.id,
    s.store_name,
    s.store_code,
    s.longitude,
    s.latitude,
    f.fence_name,
    f.fence_code,
    ST_AsGeoJSON(s.geom) AS store_geom_geojson
FROM store_location s
JOIN area_fence f
    ON ST_Within(s.geom, f.geom)
WHERE s.status = 1
  AND f.status = 1
  AND f.fence_code = 'FENCE_BJ_001'
ORDER BY s.id ASC;
```

判断指定坐标是否在某个围栏内：

```sql
-- 判断指定经纬度点是否落入围栏
SELECT
    id,
    fence_name,
    fence_code,
    ST_Contains(
        geom,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
    ) AS in_fence
FROM area_fence
WHERE status = 1
  AND fence_code = 'FENCE_BJ_001';
```

查询包含指定坐标的所有围栏：

```sql
-- 查询包含指定点位的所有围栏
SELECT
    id,
    fence_name,
    fence_code,
    ST_AsGeoJSON(geom) AS fence_geom_geojson
FROM area_fence
WHERE status = 1
  AND ST_Contains(
        geom,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
  )
ORDER BY id ASC;
```

查询与指定矩形范围相交的围栏：

```sql
-- 查询与地图视野范围相交的围栏
SELECT
    id,
    fence_name,
    fence_code,
    ST_AsGeoJSON(geom) AS fence_geom_geojson
FROM area_fence
WHERE status = 1
  AND ST_Intersects(
        geom,
        ST_MakeEnvelope(116.390000, 39.910000, 116.410000, 39.930000, 4326)
  )
ORDER BY id ASC;
```

查询两个围栏是否存在交集：

```sql
-- 判断两个围栏区域是否相交
SELECT
    a.fence_name AS fence_name_a,
    b.fence_name AS fence_name_b,
    ST_Intersects(a.geom, b.geom) AS is_intersects
FROM area_fence a
JOIN area_fence b
    ON a.id < b.id
WHERE a.status = 1
  AND b.status = 1
  AND a.fence_code = 'FENCE_BJ_001'
  AND b.fence_code = 'FENCE_GEOJSON_001';
```

查询区域内数据时，常用函数选择如下：

| 查询目标           | 推荐函数                             | 说明                           |
| ------------------ | ------------------------------------ | ------------------------------ |
| 查询矩形视野内点位 | `ST_MakeEnvelope`、`&&`、`ST_Within` | 适合地图拖动、缩放后的视野查询 |
| 查询围栏内点位     | `ST_Within`                          | 判断点是否位于区域内部         |
| 判断区域是否包含点 | `ST_Contains`                        | 判断围栏是否包含指定坐标       |
| 查询相交区域       | `ST_Intersects`                      | 判断两个区域是否存在交集       |
| 查询附近点位       | `ST_DWithin`                         | 按半径范围过滤数据             |

空间查询时应保证参与计算的字段使用相同 SRID。项目中建议统一使用 `4326` 存储经纬度数据，距离查询使用 `geography` 字段，区域判断使用 `geometry` 字段。



## 接口设计

本节说明 Spring Boot 项目中如何设计 PostGIS 相关接口。示例延续前文的 `store_location` 表和 `StoreLocationService` 服务层，主要提供坐标点保存、附近位置查询和区域范围查询三个接口。

接口设计建议遵循以下原则：

| 原则                 | 说明                                                     |
| -------------------- | -------------------------------------------------------- |
| 经纬度参数明确命名   | 使用 `longitude`、`latitude`，避免使用 `x`、`y` 造成误解 |
| 距离单位统一         | 半径、距离统一使用米                                     |
| 查询接口限制返回数量 | 附近查询必须限制 `limit`，避免一次返回过多数据           |
| 空间计算放在 SQL 层  | Controller 和 Service 不直接拼接 PostGIS 函数            |
| 前端展示返回 GeoJSON | 地图展示类接口建议返回 `geomGeoJson`                     |
| 接口参数强校验       | 经纬度、半径、分页参数都需要做边界校验                   |

### 坐标点保存接口

坐标点保存接口用于新增门店、设备、人员、仓库等点位数据。接口接收普通经纬度参数，后端在 Mapper XML 中使用 `ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)` 写入 `geometry` 字段，并同步写入 `geography` 字段用于距离计算。

接口定义如下：

| 项目         | 内容                                         |
| ------------ | -------------------------------------------- |
| 请求路径     | `/api/store-locations`                       |
| 请求方式     | `POST`                                       |
| Content-Type | `application/json`                           |
| 功能说明     | 保存一个门店坐标点                           |
| 核心处理     | 写入普通经纬度字段、`geom` 字段、`geog` 字段 |

请求参数示例：

```json
{
  "storeName": "北京测试门店",
  "storeCode": "STORE_TEST_BJ_001",
  "longitude": 116.397128,
  "latitude": 39.916527
}
```

响应结果示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

### 附近位置查询接口

附近位置查询接口用于根据用户当前位置查询指定半径范围内的数据，例如附近门店、附近设备、附近仓库等。接口接收当前经纬度、查询半径和返回数量，底层使用 `ST_DWithin` 过滤范围，再使用 `ST_Distance` 计算距离并排序。

接口定义如下：

| 项目     | 内容                          |
| -------- | ----------------------------- |
| 请求路径 | `/api/store-locations/nearby` |
| 请求方式 | `GET`                         |
| 功能说明 | 查询指定坐标附近的门店        |
| 距离单位 | 米                            |
| 排序方式 | 按距离从近到远排序            |

请求示例：

```bash
curl -X GET "http://localhost:8080/api/store-locations/nearby?longitude=116.397128&latitude=39.916527&radiusMeter=3000&limit=10"
```

响应结果示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "storeName": "北京测试门店",
      "storeCode": "STORE_TEST_BJ_001",
      "longitude": 116.397128,
      "latitude": 39.916527,
      "distanceMeter": 0.00,
      "geomGeoJson": "{\"type\":\"Point\",\"coordinates\":[116.397128,39.916527]}"
    }
  ]
}
```

### 区域范围查询接口

区域范围查询接口用于查询矩形范围内的数据，适合地图视野范围查询。例如前端地图拖动或缩放后，将当前视野的最小经度、最小纬度、最大经度、最大纬度传给后端，后端返回当前地图窗口内的门店点位。

接口定义如下：

| 项目     | 内容                                                         |
| -------- | ------------------------------------------------------------ |
| 请求路径 | `/api/store-locations/range`                                 |
| 请求方式 | `GET`                                                        |
| 功能说明 | 查询矩形范围内的门店                                         |
| 参数格式 | `minLongitude`、`minLatitude`、`maxLongitude`、`maxLatitude` |
| 常见场景 | 地图视野检索、区域框选、范围内点位加载                       |

请求示例：

```bash
curl -X GET "http://localhost:8080/api/store-locations/range?minLongitude=116.390000&minLatitude=39.910000&maxLongitude=116.410000&maxLatitude=39.930000"
```

响应结果示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "storeName": "北京测试门店",
      "storeCode": "STORE_TEST_BJ_001",
      "longitude": 116.397128,
      "latitude": 39.916527,
      "distanceMeter": null,
      "geomGeoJson": "{\"type\":\"Point\",\"coordinates\":[116.397128,39.916527]}"
    }
  ]
}
```

区域范围查询请求对象：

文件位置：`src/main/java/io/github/atengk/postgis/dto/RegionRangeQueryDTO.java`

这个 DTO 用于接收地图矩形范围查询参数，并校验经纬度边界。

```java
package io.github.atengk.postgis.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 区域范围查询请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class RegionRangeQueryDTO {

    /**
     * 最小经度
     */
    @NotNull(message = "最小经度不能为空")
    @DecimalMin(value = "-180.0", message = "最小经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "最小经度不能大于 180")
    private BigDecimal minLongitude;

    /**
     * 最小纬度
     */
    @NotNull(message = "最小纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最小纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "最小纬度不能大于 90")
    private BigDecimal minLatitude;

    /**
     * 最大经度
     */
    @NotNull(message = "最大经度不能为空")
    @DecimalMin(value = "-180.0", message = "最大经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "最大经度不能大于 180")
    private BigDecimal maxLongitude;

    /**
     * 最大纬度
     */
    @NotNull(message = "最大纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最大纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "最大纬度不能大于 90")
    private BigDecimal maxLatitude;
}
```

通用响应对象：

文件位置：`src/main/java/io/github/atengk/postgis/common/ApiResult.java`

这个通用响应类用于统一接口返回结构，便于前端处理成功和失败结果。

```java
package io.github.atengk.postgis.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 失败消息
     * @param <T> 数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

接口控制器：

文件位置：`src/main/java/io/github/atengk/postgis/controller/StoreLocationController.java`

这个 Controller 提供坐标点保存、附近位置查询和矩形区域范围查询接口。

```java
package io.github.atengk.postgis.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.postgis.common.ApiResult;
import io.github.atengk.postgis.dto.NearbyStoreQueryDTO;
import io.github.atengk.postgis.dto.RegionRangeQueryDTO;
import io.github.atengk.postgis.dto.StoreLocationCreateDTO;
import io.github.atengk.postgis.service.StoreLocationService;
import io.github.atengk.postgis.vo.StoreLocationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 门店位置接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-locations")
public class StoreLocationController {

    private final StoreLocationService storeLocationService;

    /**
     * 保存坐标点
     *
     * @param dto 坐标点保存请求
     * @return 保存结果
     */
    @PostMapping
    public ApiResult<Boolean> createStoreLocation(@Valid @RequestBody StoreLocationCreateDTO dto) {
        storeLocationService.createStoreLocation(dto);
        log.info("坐标点保存接口调用完成，门店编码：{}", dto.getStoreCode());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 查询附近位置
     *
     * @param dto 附近位置查询请求
     * @return 附近位置列表
     */
    @GetMapping("/nearby")
    public ApiResult<List<StoreLocationVO>> listNearbyStores(@Valid @ModelAttribute NearbyStoreQueryDTO dto) {
        List<StoreLocationVO> list = storeLocationService.listNearbyStores(dto);
        log.info("附近位置查询接口调用完成，返回数量：{}", CollUtil.size(list));
        return ApiResult.success(list);
    }

    /**
     * 查询区域范围内的位置
     *
     * @param dto 区域范围查询请求
     * @return 区域范围内的位置列表
     */
    @GetMapping("/range")
    public ApiResult<List<StoreLocationVO>> listStoresInRange(@Valid @ModelAttribute RegionRangeQueryDTO dto) {
        List<StoreLocationVO> list = storeLocationService.listStoresInEnvelope(
                dto.getMinLongitude(),
                dto.getMinLatitude(),
                dto.getMaxLongitude(),
                dto.getMaxLatitude()
        );

        log.info("区域范围查询接口调用完成，返回数量：{}", CollUtil.size(list));
        return ApiResult.success(list);
    }
}
```

为了让参数校验结果更友好，建议补充全局异常处理。

文件位置：`src/main/java/io/github/atengk/postgis/common/GlobalExceptionHandler.java`

这个全局异常处理类统一处理参数校验异常和业务异常，避免接口直接返回 Spring 默认错误结构。

```java
package io.github.atengk.postgis.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 JSON 请求体参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> CharSequenceUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        String message = CollUtil.join(messages, "；");
        log.warn("请求体参数校验失败：{}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理 Query 参数绑定校验异常
     *
     * @param e 参数绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        List<String> messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> CharSequenceUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        String message = CollUtil.join(messages, "；");
        log.warn("请求参数绑定失败：{}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理约束校验异常
     *
     * @param e 约束校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(item -> item.getMessage())
                .collect(Collectors.joining("；"));

        log.warn("请求参数约束校验失败：{}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理其他异常
     *
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail("系统异常，请联系管理员");
    }
}
```

## 功能验证

本节用于验证 PostGIS 数据表、空间索引、接口调用和查询结果是否符合预期。验证顺序建议为：先准备数据库数据，再调用接口，最后使用 SQL 检查空间字段、距离结果和范围命中情况。

### 数据准备

功能验证前，需要确认数据库已经启用 PostGIS，并且空间表和索引已经创建完成。

检查 PostGIS 扩展：

```sql
-- 查看当前数据库是否启用 PostGIS
SELECT PostGIS_Full_Version();

-- 查看已安装扩展
SELECT
    extname,
    extversion
FROM pg_extension
WHERE extname LIKE 'postgis%';
```

清理旧测试数据：

```sql
-- 清理测试数据，开发环境可执行；生产环境禁止直接执行
DELETE FROM store_location
WHERE store_code LIKE 'STORE_TEST_%';

DELETE FROM area_fence
WHERE fence_code LIKE 'FENCE_TEST_%';
```

准备门店点位测试数据：

```sql
-- 插入北京附近测试门店
INSERT INTO store_location (
    store_name,
    store_code,
    longitude,
    latitude,
    geom,
    geog,
    status
)
VALUES
(
    '北京测试门店A',
    'STORE_TEST_BJ_A',
    116.397128,
    39.916527,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326),
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    1
),
(
    '北京测试门店B',
    'STORE_TEST_BJ_B',
    116.407526,
    39.904030,
    ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326),
    ST_SetSRID(ST_MakePoint(116.407526, 39.904030), 4326)::geography,
    1
),
(
    '上海测试门店A',
    'STORE_TEST_SH_A',
    121.473701,
    31.230416,
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326),
    ST_SetSRID(ST_MakePoint(121.473701, 31.230416), 4326)::geography,
    1
);
```

准备区域围栏测试数据：

```sql
-- 插入北京测试围栏
INSERT INTO area_fence (
    fence_name,
    fence_code,
    geom,
    status
)
VALUES (
    '北京测试围栏',
    'FENCE_TEST_BJ',
    ST_GeomFromText(
        'MULTIPOLYGON(((116.390000 39.900000,116.420000 39.900000,116.420000 39.930000,116.390000 39.930000,116.390000 39.900000)))',
        4326
    ),
    1
);
```

检查测试数据：

```sql
-- 检查门店点位
SELECT
    store_name,
    store_code,
    longitude,
    latitude,
    ST_AsText(geom) AS geom_wkt,
    ST_SRID(geom) AS srid
FROM store_location
WHERE store_code LIKE 'STORE_TEST_%'
ORDER BY store_code ASC;

-- 检查测试围栏
SELECT
    fence_name,
    fence_code,
    ST_AsText(geom) AS geom_wkt,
    ST_SRID(geom) AS srid
FROM area_fence
WHERE fence_code = 'FENCE_TEST_BJ';
```

### 接口测试

接口测试可以使用 `curl`、Postman、Apifox 或自动化测试工具。下面以 `curl` 为例。

启动项目：

```bash
# 在项目根目录启动 Spring Boot 项目
mvn spring-boot:run
```

启动成功后，日志中应能看到 PostGIS 检查通过信息，例如：

```text
PostGIS 扩展检查通过：POSTGIS="..."
```

测试坐标点保存接口：

```bash
curl -X POST "http://localhost:8080/api/store-locations" \
  -H "Content-Type: application/json" \
  -d '{
    "storeName": "接口测试门店",
    "storeCode": "STORE_TEST_API_001",
    "longitude": 116.398000,
    "latitude": 39.917000
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

测试附近位置查询接口：

```bash
curl -X GET "http://localhost:8080/api/store-locations/nearby?longitude=116.397128&latitude=39.916527&radiusMeter=3000&limit=10"
```

预期响应中应包含北京附近的测试门店，并返回 `distanceMeter` 字段：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "storeName": "北京测试门店A",
      "storeCode": "STORE_TEST_BJ_A",
      "longitude": 116.397128,
      "latitude": 39.916527,
      "distanceMeter": 0.00,
      "geomGeoJson": "{\"type\":\"Point\",\"coordinates\":[116.397128,39.916527]}"
    }
  ]
}
```

测试区域范围查询接口：

```bash
curl -X GET "http://localhost:8080/api/store-locations/range?minLongitude=116.390000&minLatitude=39.900000&maxLongitude=116.420000&maxLatitude=39.930000"
```

预期响应中应包含北京测试门店，不应包含上海测试门店。

测试参数校验：

```bash
# 经度超出范围，应返回参数校验失败
curl -X GET "http://localhost:8080/api/store-locations/nearby?longitude=200&latitude=39.916527&radiusMeter=3000&limit=10"

# 半径为空，应返回参数校验失败
curl -X GET "http://localhost:8080/api/store-locations/nearby?longitude=116.397128&latitude=39.916527&limit=10"
```

### 查询结果校验

接口返回结果需要通过 SQL 做二次校验，重点确认空间字段是否正确、距离是否合理、区域查询是否命中预期数据、空间索引是否生效。

校验保存接口是否正确写入空间字段：

```sql
-- 校验接口新增的数据
SELECT
    store_name,
    store_code,
    longitude,
    latitude,
    ST_AsText(geom) AS geom_wkt,
    ST_AsGeoJSON(geom) AS geom_geojson,
    ST_SRID(geom) AS srid
FROM store_location
WHERE store_code = 'STORE_TEST_API_001';
```

预期结果中：

| 字段        | 预期                    |
| ----------- | ----------------------- |
| `longitude` | `116.398000`            |
| `latitude`  | `39.917000`             |
| `geom_wkt`  | `POINT(116.398 39.917)` |
| `srid`      | `4326`                  |

校验附近查询距离：

```sql
-- 校验指定坐标到测试门店的距离
SELECT
    store_name,
    store_code,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE store_code LIKE 'STORE_TEST_%'
ORDER BY distance_meter ASC;
```

校验 3 公里范围内数据：

```sql
-- 校验 3000 米范围内门店
SELECT
    store_name,
    store_code
FROM store_location
WHERE store_code LIKE 'STORE_TEST_%'
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY store_code ASC;
```

预期结果中应包含北京测试门店，不应包含上海测试门店。

校验区域范围查询：

```sql
-- 校验矩形范围内门店
SELECT
    store_name,
    store_code,
    ST_AsText(geom) AS geom_wkt
FROM store_location
WHERE store_code LIKE 'STORE_TEST_%'
  AND geom && ST_MakeEnvelope(116.390000, 39.900000, 116.420000, 39.930000, 4326)
  AND ST_Within(
        geom,
        ST_MakeEnvelope(116.390000, 39.900000, 116.420000, 39.930000, 4326)
  )
ORDER BY store_code ASC;
```

校验围栏包含关系：

```sql
-- 校验门店是否在测试围栏内
SELECT
    s.store_name,
    s.store_code,
    f.fence_name,
    f.fence_code,
    ST_Within(s.geom, f.geom) AS in_fence
FROM store_location s
JOIN area_fence f
    ON f.fence_code = 'FENCE_TEST_BJ'
WHERE s.store_code LIKE 'STORE_TEST_%'
ORDER BY s.store_code ASC;
```

校验空间索引是否生效：

```sql
-- 查看附近查询是否使用空间索引
EXPLAIN ANALYZE
SELECT
    id,
    store_name
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  );
```

执行计划中如果出现 `Index Scan`、`Bitmap Index Scan` 或 `idx_store_location_geog`、`idx_store_location_geog_enabled` 等索引名称，说明空间索引通常已经生效。如果出现全表扫描，需要结合数据量、统计信息、SQL 写法和索引条件继续排查。

## 项目注意事项

本节总结 PostGIS 项目落地时最容易出现的问题，包括坐标系选择、空间索引使用、距离单位处理和查询性能优化。实际开发中，大部分空间查询异常都与坐标顺序、SRID、距离单位和索引未命中有关。

### 坐标系选择

坐标系决定空间数据如何解释。项目中最常用的是 `SRID=4326`，也就是 WGS84 经纬度坐标系。互联网地图、GPS 坐标、常见 GeoJSON 数据多数都使用经纬度表达，因此业务系统中通常使用 `geometry(POINT, 4326)`、`geometry(MULTIPOLYGON, 4326)` 或 `geography(POINT, 4326)`。

常见建议如下：

| 场景                     | 建议                                               |
| ------------------------ | -------------------------------------------------- |
| 存储经纬度点位           | 使用 `geometry(POINT, 4326)`                       |
| 米级附近查询             | 使用 `geography(POINT, 4326)`                      |
| 存储围栏、行政区、服务区 | 使用 `geometry(MULTIPOLYGON, 4326)`                |
| 地图视野范围查询         | 使用 `geometry`                                    |
| 本地高精度面积、长度分析 | 使用合适的投影坐标系，不建议直接用 `4326` 计算面积 |

坐标系使用时需要注意：

| 注意点             | 说明                                                   |
| ------------------ | ------------------------------------------------------ |
| SRID 必须统一      | 参与空间计算的字段和参数应使用相同 SRID                |
| 经纬度顺序不能写反 | PostGIS 中通常使用 `ST_MakePoint(longitude, latitude)` |
| 前端坐标来源要确认 | 不同地图服务可能存在坐标系偏移问题                     |
| GeoJSON 默认顺序   | GeoJSON 坐标通常也是 `[longitude, latitude]`           |
| 不要混用坐标系     | 数据来源不同的坐标需要先统一转换后再入库               |

错误示例：

```sql
-- 错误：纬度和经度顺序写反
SELECT ST_AsText(
    ST_SetSRID(ST_MakePoint(39.916527, 116.397128), 4326)
);
```

正确示例：

```sql
-- 正确：经度在前，纬度在后
SELECT ST_AsText(
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
);
```

### 索引使用注意

空间索引不是创建后就一定会被使用。查询优化器是否使用索引，取决于 SQL 写法、数据量、统计信息、字段类型、查询条件和索引条件。

常见建议如下：

| 注意点                      | 说明                                                 |
| --------------------------- | ---------------------------------------------------- |
| 空间字段需要 GiST 索引      | `geometry`、`geography` 字段通常使用 GiST 索引       |
| 高频过滤字段也要建索引      | 如 `status`、`store_code`、`fence_code`              |
| 部分索引条件要匹配 SQL      | 如索引 `WHERE status = 1`，查询也要包含 `status = 1` |
| 创建索引后执行统计更新      | 使用 `VACUUM ANALYZE 表名`                           |
| 使用 `EXPLAIN ANALYZE` 验证 | 不要只凭感觉判断索引是否生效                         |

推荐索引：

```sql
-- geography 字段索引，适合附近查询
CREATE INDEX idx_store_location_geog
ON store_location
USING GIST (geog);

-- geometry 字段索引，适合区域判断和地图范围查询
CREATE INDEX idx_store_location_geom
ON store_location
USING GIST (geom);

-- 围栏 geometry 字段索引，适合点面关系判断
CREATE INDEX idx_area_fence_geom
ON area_fence
USING GIST (geom);
```

部分索引示例：

```sql
-- 只索引启用状态的数据，适合大部分查询都只查 status = 1 的场景
CREATE INDEX idx_store_location_geog_enabled
ON store_location
USING GIST (geog)
WHERE status = 1;
```

对应 SQL 需要包含 `status = 1`：

```sql
-- 查询条件匹配部分索引条件，更容易命中索引
SELECT
    id,
    store_name
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  );
```

不推荐写法：

```sql
-- 不推荐：没有包含 status = 1 时，无法使用 status = 1 的部分索引
SELECT
    id,
    store_name
FROM store_location
WHERE ST_DWithin(
    geog,
    ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
    3000
);
```

### 距离单位处理

PostGIS 中距离单位取决于字段类型和坐标系。项目中最常见的误区是直接对 `geometry(POINT, 4326)` 使用 `ST_Distance`，然后把结果当成米。实际上，`geometry(POINT, 4326)` 的坐标单位是经纬度角度，不是米。

常见规则如下：

| 写法                                             | 返回单位 | 说明                   |
| ------------------------------------------------ | -------- | ---------------------- |
| `ST_Distance(geometry_4326, geometry_4326)`      | 度       | 不适合直接作为米级距离 |
| `ST_Distance(geography, geography)`              | 米       | 适合附近查询和距离展示 |
| `ST_DWithin(geography, geography, 3000)`         | 米       | 表示 3000 米范围内     |
| `ST_DWithin(geometry_4326, geometry_4326, 0.01)` | 度       | 不建议用于业务半径查询 |

不推荐写法：

```sql
-- 不推荐：geometry 4326 下距离单位不是米
SELECT
    id,
    store_name,
    ST_Distance(
        geom,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)
    ) AS distance_value
FROM store_location;
```

推荐写法：

```sql
-- 推荐：使用 geography 计算米级距离
SELECT
    id,
    store_name,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
ORDER BY distance_meter ASC;
```

如果表中没有单独保存 `geog` 字段，也可以临时转换：

```sql
-- 使用 geometry 临时转换为 geography 计算米级距离
SELECT
    id,
    store_name,
    ROUND(
        ST_Distance(
            geom::geography,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
ORDER BY distance_meter ASC;
```

临时转换写法更简单，但高频查询场景下建议单独保存 `geog` 字段并建立 GiST 索引。

### 查询性能优化

PostGIS 查询性能优化的核心思路是减少参与精确空间计算的数据量。空间计算通常比普通字段比较更复杂，因此应优先用索引友好的条件缩小候选集，再执行距离计算、区域判断和排序。

常见优化建议如下：

| 优化点                       | 说明                                     |
| ---------------------------- | ---------------------------------------- |
| 先过滤业务条件               | 如 `status = 1`、城市编码、类型、租户 ID |
| 使用 `ST_DWithin` 做半径过滤 | 不要直接全表 `ST_Distance <= 半径`       |
| 限制返回数量                 | 附近查询必须使用 `LIMIT`                 |
| 结合边界框过滤               | 大范围地图查询可先用 `&&` 粗过滤         |
| 建立合适索引                 | 空间字段、状态字段、编码字段都要考虑     |
| 更新统计信息                 | 大量导入数据后执行 `VACUUM ANALYZE`      |
| 使用执行计划分析             | 通过 `EXPLAIN ANALYZE` 排查瓶颈          |

推荐的附近查询写法：

```sql
-- 推荐：先使用 ST_DWithin 过滤，再计算距离并排序
SELECT
    id,
    store_name,
    ROUND(
        ST_Distance(
            geog,
            ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
        )::numeric,
        2
    ) AS distance_meter
FROM store_location
WHERE status = 1
  AND ST_DWithin(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography,
        3000
  )
ORDER BY distance_meter ASC
LIMIT 20;
```

不推荐的附近查询写法：

```sql
-- 不推荐：直接对全表计算距离后过滤，数据量大时性能较差
SELECT
    id,
    store_name
FROM store_location
WHERE ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) <= 3000
ORDER BY ST_Distance(
        geog,
        ST_SetSRID(ST_MakePoint(116.397128, 39.916527), 4326)::geography
    ) ASC;
```

地图视野查询推荐写法：

```sql
-- 推荐：先使用 && 边界框操作符过滤，再使用 ST_Within 精确判断
SELECT
    id,
    store_name,
    store_code,
    longitude,
    latitude
FROM store_location
WHERE status = 1
  AND geom && ST_MakeEnvelope(116.390000, 39.900000, 116.420000, 39.930000, 4326)
  AND ST_Within(
        geom,
        ST_MakeEnvelope(116.390000, 39.900000, 116.420000, 39.930000, 4326)
  )
ORDER BY id ASC
LIMIT 500;
```

大数据量场景下，还可以进一步优化：

| 场景           | 优化方式                                          |
| -------------- | ------------------------------------------------- |
| 全国门店点位   | 增加省、市、区编码字段，先按行政区过滤            |
| 多租户系统     | 空间表增加 `tenant_id`，并建立联合过滤策略        |
| 高频附近查询   | 保存 `geog` 字段并建立 GiST 索引                  |
| 地图海量点展示 | 后端按网格聚合或聚类返回                          |
| 大批量导入     | 先导入数据，再批量创建索引和执行 `VACUUM ANALYZE` |
| 复杂区域分析   | 预处理区域边界，避免每次查询实时计算复杂几何      |

最终建议是：点位数据用 `geometry + geography` 组合存储，区域数据用 `geometry(MULTIPOLYGON, 4326)` 存储；附近查询走 `geography + ST_DWithin`，区域判断走 `geometry + ST_Within/ST_Contains/ST_Intersects`，并通过 `EXPLAIN ANALYZE` 持续验证索引命中情况。
