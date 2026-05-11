以下内容按你上传的大纲中 `MySQL 8 基础概述` 与 `安装与环境配置` 两部分展开。

# MySQL 8

MySQL 8 是 MySQL 数据库的重要主版本系列，常用于业务系统的数据持久化、事务处理、报表查询、后台管理系统、互联网应用、数据同步与高可用架构。MySQL 8 相比 MySQL 5.7 在 SQL 能力、优化器、权限模型、JSON 支持、索引能力、事务与诊断能力方面都有明显增强，适合作为现代 Java、Spring Boot、微服务和中后台系统的主流关系型数据库方案。

## MySQL 8 基础概述

本节用于建立 MySQL 8 的基础认知，包括核心特性、适用场景、版本选择、客户端与服务端结构。学习 MySQL 8 时，应先理解它不仅是一个 SQL 执行工具，而是由服务端进程、存储引擎、权限系统、日志系统、客户端工具和连接协议共同组成的数据库系统。

### MySQL 8 核心特性

MySQL 8 在 SQL 表达能力、查询优化、权限管理、JSON 支持和运维诊断方面进行了大量增强。官方文档明确列出 MySQL 8.0 支持 CTE、公用表表达式、递归 CTE、窗口函数等能力；窗口函数可以基于当前行关联的一组行进行计算，适合排名、分组序号、同比环比等分析类查询。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/mysql-nutshell.html?ff=nopfpls&utm_source=chatgpt.com))

常见核心特性如下：

| 特性                | 说明                                  | 常见用途                     |
| ------------------- | ------------------------------------- | ---------------------------- |
| InnoDB 默认存储引擎 | 支持事务、行级锁、MVCC、崩溃恢复      | 绝大多数业务表               |
| CTE 公用表表达式    | 使用 `WITH` 定义临时结果集            | 拆分复杂 SQL、递归查询       |
| 窗口函数            | 使用 `OVER()` 对分区内数据计算        | 排名、Top N、累计统计        |
| JSON 增强           | 支持 JSON 字段、JSON 函数、JSON_TABLE | 半结构化扩展字段             |
| 函数索引            | 支持基于表达式创建索引                | 日期截断、大小写转换查询优化 |
| 降序索引            | 支持索引字段按降序存储                | 倒序分页、时间倒序查询       |
| 隐藏索引            | 可临时让优化器忽略索引                | 评估删除索引风险             |
| 角色权限            | 支持通过角色管理权限集合              | 统一授权、最小权限管理       |
| `EXPLAIN ANALYZE`   | 实际执行并返回执行耗时信息            | 慢 SQL 分析                  |
| 原子 DDL            | DDL 操作具备更好的原子性              | 降低结构变更中间态风险       |

MySQL 8 的核心价值不是单一语法能力，而是将查询表达能力、事务安全、索引优化、权限治理和运维诊断能力组合到一起。对于业务系统，最常用的是 InnoDB、事务、索引、JSON、窗口函数、CTE、执行计划和角色权限；对于 DBA 或运维场景，还需要重点掌握 redo log、undo log、binlog、慢查询日志、主从复制与备份恢复。

### MySQL 8 适用场景

MySQL 8 适合以结构化数据为核心、需要事务一致性、需要稳定 SQL 查询能力的业务系统。它支持常见数据类型、聚合、排序、分组、连接查询、权限控制和大规模数据存储，是典型 OLTP 系统的主流选择。官方文档也强调 MySQL 支持丰富数据类型、SQL 查询、分组聚合、连接查询、权限系统和较大规模数据库。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/features.html?utm_source=chatgpt.com))

适用场景包括：

| 场景             | 说明                         | 示例                         |
| ---------------- | ---------------------------- | ---------------------------- |
| 业务交易系统     | 强调事务、主键查询、状态变更 | 订单、支付、库存、账户       |
| 中后台管理系统   | CRUD 多、查询条件稳定        | 用户管理、权限管理、配置管理 |
| 内容管理系统     | 结构化内容与部分扩展字段并存 | 文章、标签、分类、评论       |
| 报表统计         | 使用聚合、分组、窗口函数     | 销售统计、访问统计、排名查询 |
| 微服务持久化     | 每个服务维护独立业务库       | 用户服务、订单服务、商品服务 |
| 数据同步与复制   | 使用 binlog、主从复制        | 读写分离、备库、数据订阅     |
| 轻量数据仓库辅助 | 适合中小规模结构化分析       | 运营报表、管理看板           |

不适合优先选择 MySQL 的场景包括：超大规模离线分析、复杂图关系遍历、全文搜索强依赖场景、非结构化海量日志检索、高吞吐时序数据写入。此类需求通常会结合 ClickHouse、Elasticsearch、Redis、MongoDB、Prometheus、Doris、StarRocks 等组件一起设计。

### MySQL 版本选择与兼容性

MySQL 版本选择应优先考虑稳定性、生命周期、兼容性、团队运维能力和业务系统依赖。MySQL 官方当前将版本分为 LTS 和 Innovation 两类：LTS 适合需要稳定特性和较长支持周期的环境，Innovation 适合希望更快使用新功能并能够接受更频繁升级的团队。官方说明中，LTS 版本以降低行为变化风险为目标，Innovation 版本则更强调新功能和变化。([MySQL开发者专区](https://dev.mysql.com/doc/mysql/en/mysql-releases.html?utm_source=chatgpt.com))

推荐选择策略如下：

| 使用场景            | 推荐版本                       | 说明                                 |
| ------------------- | ------------------------------ | ------------------------------------ |
| 新生产项目          | MySQL 8.4 LTS                  | 更适合长期维护和稳定升级             |
| 已有 MySQL 8.0 项目 | 继续维护 8.0，规划升级 8.4 LTS | 先完成兼容性测试再升级               |
| 学习 MySQL 8        | MySQL 8.0 或 8.4               | 基础 SQL、InnoDB、索引、事务差异不大 |
| 需要最新功能        | Innovation 版本                | 需要更强自动化测试和升级能力         |
| 企业生产环境        | LTS + 明确补丁策略             | 避免无计划跨版本升级                 |

兼容性注意事项：

1. 从 MySQL 5.7 升级到 MySQL 8 时，需要重点检查字符集、排序规则、保留关键字、SQL 模式、认证插件、索引长度、JSON 字段、时间字段默认值等问题。
2. MySQL 8 默认字符集通常使用 `utf8mb4`，默认排序规则为 `utf8mb4_0900_ai_ci`，这与旧版本常见的 `utf8mb4_general_ci`、`utf8mb4_unicode_ci` 在排序和比较行为上可能不同。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/charset-server.html?utm_source=chatgpt.com))
3. Java 项目需要确认 JDBC 驱动版本。Spring Boot 3 项目通常使用 MySQL Connector/J 8.x 或更高版本。
4. 升级前必须在测试环境执行完整回归测试，重点覆盖复杂 SQL、分页排序、唯一索引、字符比较、时间字段、批量写入和事务逻辑。
5. 生产环境升级前应先备份，并准备回滚方案。大版本升级不应直接在主库上无验证执行。

查看当前 MySQL 服务端版本：

```sql
-- 查看 MySQL 服务端版本
SELECT VERSION();

-- 查看版本相关变量
SHOW VARIABLES LIKE 'version%';
```

查看字符集、排序规则和 SQL 模式：

```sql
-- 查看服务端字符集
SHOW VARIABLES LIKE 'character_set_server';

-- 查看服务端排序规则
SHOW VARIABLES LIKE 'collation_server';

-- 查看 SQL 模式
SHOW VARIABLES LIKE 'sql_mode';
```

### MySQL 客户端与服务端结构

MySQL 采用典型客户端 / 服务端结构。服务端由 `mysqld` 进程提供数据库服务，客户端通过 TCP、Unix Socket、Named Pipe 或本地连接方式访问服务端。官方文档中，命令行客户端通常通过 `mysql -h host -u user -p` 指定主机、用户和密码连接服务端；本机连接时可以省略主机参数。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/connecting-disconnecting.html?utm_source=chatgpt.com))

基本结构如下：

```text
应用程序 / 命令行客户端
        |
        |  TCP/IP、Socket、Named Pipe
        v
MySQL Server: mysqld
        |
        |  SQL 解析、权限校验、优化器、执行器
        v
存储引擎层: InnoDB / MyISAM / Memory
        |
        |  数据文件、索引文件、redo log、undo log、binlog
        v
操作系统文件系统 / 磁盘
```

常见组件说明：

| 组件            | 说明                                               |
| --------------- | -------------------------------------------------- |
| `mysqld`        | MySQL 服务端进程，负责接收连接、执行 SQL、管理数据 |
| `mysql`         | 官方命令行客户端，用于连接数据库并执行 SQL         |
| `mysqladmin`    | 管理工具，可查看状态、关闭服务、刷新配置等         |
| `mysqldump`     | 逻辑备份工具，用于导出库表结构和数据               |
| MySQL Workbench | 图形化客户端，适合建模、查询、管理                 |
| JDBC Driver     | Java 应用访问 MySQL 的驱动                         |
| InnoDB          | 默认存储引擎，支持事务、行锁、MVCC                 |
| Binlog          | 二进制日志，用于复制和数据恢复                     |
| Redo Log        | InnoDB 重做日志，用于崩溃恢复                      |
| Undo Log        | InnoDB 回滚日志，用于事务回滚和 MVCC               |

客户端连接并查看当前连接信息：

```sql
-- 查看当前连接用户
SELECT USER();

-- 查看当前实际认证用户
SELECT CURRENT_USER();

-- 查看当前数据库
SELECT DATABASE();

-- 查看当前连接 ID
SELECT CONNECTION_ID();
```

## 安装与环境配置

本节用于说明 MySQL 8 在 Windows、Linux 和 Docker 环境中的安装方式，以及服务启停、客户端连接、配置文件、字符集、时区、端口与连接参数。生产环境建议优先使用 Linux 包管理器或标准化 Docker Compose 部署，开发环境可以使用 Windows Installer、Docker 或本地 Linux 虚拟机。

### Windows 环境安装

Windows 环境适合本地开发、学习和简单测试。官方推荐使用 MySQL Installer 安装 MySQL Server，并可同时安装 MySQL Workbench 等工具；MySQL 8.0 在 Windows 上要求 64 位系统，并需要 Microsoft Visual C++ 2019 Redistributable。([MySQL开发者专区](https://dev.mysql.com/doc/mysql/8.0/en/windows-installation.html?utm_source=chatgpt.com))

安装步骤：

1. 下载 MySQL Installer。
2. 选择安装类型：
   - `Developer Default`：安装服务端、Workbench、Shell 等开发工具。
   - `Server Only`：只安装 MySQL Server。
   - `Custom`：自定义选择组件。
3. 选择 MySQL Server 版本。
4. 配置服务端类型，开发环境可选择默认配置。
5. 设置端口，默认 `3306`。
6. 设置 root 密码。
7. 根据需要创建普通用户。
8. 配置 Windows 服务，建议服务名使用 `MySQL80` 或明确业务名称。
9. 完成安装后使用 Workbench 或命令行连接验证。

Windows 常用目录示例：

| 类型       | 常见路径                                                |
| ---------- | ------------------------------------------------------- |
| 安装目录   | `C:\Program Files\MySQL\MySQL Server 8.0`               |
| 数据目录   | `C:\ProgramData\MySQL\MySQL Server 8.0\Data`            |
| 配置文件   | `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`          |
| 客户端程序 | `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` |

将 MySQL 客户端加入环境变量后，可以在 PowerShell 或 CMD 中执行：

```powershell
# 查看 mysql 客户端版本
mysql --version

# 使用 root 用户连接本机 MySQL
mysql -h 127.0.0.1 -P 3306 -u root -p

# 查看 Windows 服务状态
sc query MySQL80
```

如果提示 `mysql` 不是内部或外部命令，通常是因为 MySQL 的 `bin` 目录没有加入系统环境变量 `Path`。

### Linux 环境安装

Linux 是生产环境部署 MySQL 的常用选择。官方文档提供了通过 Yum、APT、RPM、Debian 包、通用二进制包、Docker 等多种方式安装 MySQL 的说明。([MySQL开发者专区](https://dev.mysql.com/doc/mysql/8.0/en/installing.html?utm_source=chatgpt.com))

在 Ubuntu / Debian 环境中，可以使用 APT 安装。不同发行版的软件源版本可能不同，生产环境建议使用官方 MySQL APT Repository 或企业内部标准源。

下面命令用于在 Ubuntu / Debian 系统中安装并启动 MySQL：

```bash
# 更新软件包索引
sudo apt update

# 安装 MySQL 服务端
sudo apt install -y mysql-server

# 启动 MySQL 服务
sudo systemctl start mysql

# 设置开机自启
sudo systemctl enable mysql

# 查看服务状态
sudo systemctl status mysql
```

命令说明：

- `apt update` 用于刷新软件包索引。
- `apt install mysql-server` 用于安装 MySQL 服务端。
- `systemctl start mysql` 用于启动 MySQL 服务。
- `systemctl enable mysql` 用于设置开机自启。
- Debian / Ubuntu 平台服务名通常是 `mysql`，而部分 RPM 平台服务名可能是 `mysqld`。官方 systemd 文档也说明，不同平台服务名可能不同，需要按实际服务名执行。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/using-systemd.html?utm_source=chatgpt.com))

在 CentOS / RHEL / Rocky Linux / AlmaLinux 环境中，通常使用 Yum 或 DNF：

```bash
# 安装 MySQL 服务端，具体包名取决于系统源或官方 MySQL 源
sudo dnf install -y mysql-server

# 启动 MySQL 服务
sudo systemctl start mysqld

# 设置开机自启
sudo systemctl enable mysqld

# 查看服务状态
sudo systemctl status mysqld
```

安装后建议执行安全初始化：

```bash
# 执行安全初始化，根据提示设置 root 密码、移除匿名用户、禁止远程 root 等
sudo mysql_secure_installation
```

验证安装结果：

```bash
# 查看客户端版本
mysql --version

# 连接本机 MySQL
mysql -u root -p

# 查看服务端版本
mysql -u root -p -e "SELECT VERSION();"
```

### Docker 环境安装

Docker 适合开发环境、测试环境、CI 环境和标准化部署。MySQL 官方文档说明可以使用 Docker 容器部署 MySQL Server，但同时提醒部署前需要理解并处理容器运行的安全风险。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/linux-installation-docker.html?utm_source=chatgpt.com))

下面命令用于快速启动一个 MySQL 8 容器，并将数据持久化到 Docker Volume：

```bash
# 拉取 MySQL 8 镜像
docker pull mysql:8.0

# 创建数据卷
docker volume create mysql8_data

# 启动 MySQL 8 容器
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=Root_123456 \
  -e MYSQL_DATABASE=demo_db \
  -v mysql8_data:/var/lib/mysql \
  mysql:8.0

# 查看容器状态
docker ps

# 查看 MySQL 启动日志
docker logs -f mysql8
```

命令说明：

- `-p 3306:3306` 表示将宿主机 `3306` 端口映射到容器 `3306` 端口。
- `MYSQL_ROOT_PASSWORD` 用于设置 root 用户密码。
- `MYSQL_DATABASE` 用于初始化创建数据库。
- `-v mysql8_data:/var/lib/mysql` 用于持久化 MySQL 数据目录。
- 生产环境不建议直接暴露 root 用户，也不建议使用简单密码。

使用 Docker Compose 更适合项目开发环境。

文件位置：`docker-compose.yml`

```yaml
services:
  mysql8:
    image: mysql:8.0
    container_name: mysql8
    restart: unless-stopped
    ports:
      - "3306:3306"
    environment:
      # root 用户密码，生产环境应通过密钥或环境变量注入
      MYSQL_ROOT_PASSWORD: Root_123456
      # 初始化数据库
      MYSQL_DATABASE: demo_db
      # 设置容器时区
      TZ: Asia/Shanghai
    volumes:
      # 持久化数据目录
      - mysql8_data:/var/lib/mysql
      # 挂载自定义配置文件
      - ./mysql/conf.d:/etc/mysql/conf.d
      # 初始化 SQL，只在首次初始化数据目录时执行
      - ./mysql/init:/docker-entrypoint-initdb.d
    command:
      # 设置服务端字符集和排序规则
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      # 设置默认时区
      - --default-time-zone=+08:00

volumes:
  mysql8_data:
```

启动与验证：

```bash
# 启动服务
docker compose up -d

# 查看服务状态
docker compose ps

# 进入容器执行 mysql 客户端
docker exec -it mysql8 mysql -uroot -p

# 停止服务
docker compose down
```

### MySQL 服务启动与停止

MySQL 服务启动与停止取决于安装方式和操作系统。官方文档说明 MySQL 可以通过直接启动 `mysqld`、Windows 服务、systemd、System V 脚本、macOS launchd 等方式管理；在支持 systemd 的 Linux 平台上，通常使用 `systemctl` 管理服务。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/automatic-start.html?utm_source=chatgpt.com))

Linux systemd 常用命令：

```bash
# 启动 MySQL，Debian / Ubuntu 常见服务名
sudo systemctl start mysql

# 停止 MySQL
sudo systemctl stop mysql

# 重启 MySQL
sudo systemctl restart mysql

# 查看 MySQL 状态
sudo systemctl status mysql

# 设置开机自启
sudo systemctl enable mysql

# 取消开机自启
sudo systemctl disable mysql
```

RPM 系系统常见服务名为 `mysqld`：

```bash
# 启动 MySQL，RHEL / CentOS / Rocky Linux 常见服务名
sudo systemctl start mysqld

# 停止 MySQL
sudo systemctl stop mysqld

# 重启 MySQL
sudo systemctl restart mysqld

# 查看 MySQL 状态
sudo systemctl status mysqld
```

Windows 服务常用命令：

```powershell
# 启动 MySQL 服务
net start MySQL80

# 停止 MySQL 服务
net stop MySQL80

# 查看服务状态
sc query MySQL80
```

Docker 常用命令：

```bash
# 启动容器
docker start mysql8

# 停止容器
docker stop mysql8

# 重启容器
docker restart mysql8

# 查看容器日志
docker logs -f mysql8
```

服务启动失败时，优先检查错误日志、配置文件语法、端口占用、数据目录权限、磁盘空间和初始化状态。

### 客户端连接方式

MySQL 常见客户端包括命令行客户端 `mysql`、图形化工具 MySQL Workbench、DataGrip、DBeaver、Navicat，以及 Java 程序通过 JDBC 连接。官方文档中命令行连接的基本形式是 `mysql -h host -u user -p`，如果连接本机服务端，可以省略 `-h`。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/connecting-disconnecting.html?utm_source=chatgpt.com))

本机连接：

```bash
# 使用 root 用户连接本机 MySQL
mysql -u root -p
```

指定主机和端口连接：

```bash
# 连接指定主机和端口
mysql -h 127.0.0.1 -P 3306 -u root -p
```

连接指定数据库：

```bash
# 登录后默认进入 demo_db 数据库
mysql -h 127.0.0.1 -P 3306 -u root -p demo_db
```

执行单条 SQL：

```bash
# 直接执行 SQL 并返回结果
mysql -h 127.0.0.1 -P 3306 -u root -p -e "SELECT VERSION();"
```

使用 Socket 连接：

```bash
# 使用 Unix Socket 连接本机 MySQL
mysql -u root -p --socket=/var/run/mysqld/mysqld.sock
```

常用连接参数：

| 参数                      | 说明             | 示例                              |
| ------------------------- | ---------------- | --------------------------------- |
| `-h`                      | 主机地址         | `-h 127.0.0.1`                    |
| `-P`                      | 端口号           | `-P 3306`                         |
| `-u`                      | 用户名           | `-u root`                         |
| `-p`                      | 提示输入密码     | `-p`                              |
| `--socket`                | Unix Socket 文件 | `--socket=/tmp/mysql.sock`        |
| `--default-character-set` | 客户端字符集     | `--default-character-set=utf8mb4` |

生产环境不建议在命令中直接写明文密码，例如不建议使用 `-pRoot_123456`。更安全的方式是交互式输入密码，或使用受控的密钥管理方式。

### 配置文件 my.cnf 与 my.ini

MySQL 配置文件用于定义服务端、客户端和工具程序的启动参数。官方文档说明，大多数 MySQL 程序都可以从 option files 读取启动参数；Windows 常见配置文件为 `my.ini`，Unix / Linux 常见配置文件为 `my.cnf`。MySQL 会按一定顺序读取多个配置文件，后读取的配置可能覆盖先读取的配置。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/option-files.html?utm_source=chatgpt.com))

常见配置文件路径：

| 系统                  | 常见配置文件                                              |
| --------------------- | --------------------------------------------------------- |
| Windows               | `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`            |
| Linux 全局            | `/etc/my.cnf`                                             |
| Linux Debian / Ubuntu | `/etc/mysql/my.cnf`、`/etc/mysql/mysql.conf.d/mysqld.cnf` |
| 用户级配置            | `~/.my.cnf`                                               |
| Docker 自定义配置     | `/etc/mysql/conf.d/*.cnf`                                 |

典型 `my.cnf` 配置如下：

```ini
[client]
# 客户端默认端口
port=3306

# 客户端默认字符集
default-character-set=utf8mb4

[mysqld]
# 服务端监听端口
port=3306

# 只监听所有网卡，生产环境应结合防火墙和授权控制
bind-address=0.0.0.0

# 数据目录
datadir=/var/lib/mysql

# 默认字符集
character-set-server=utf8mb4

# 默认排序规则
collation-server=utf8mb4_0900_ai_ci

# 默认时区
default-time-zone='+08:00'

# 最大连接数，根据业务和机器资源调整
max_connections=500

# 单个数据包最大大小，影响大字段、批量写入和导入
max_allowed_packet=64M

# 开启慢查询日志
slow_query_log=ON

# 慢查询阈值，单位秒
long_query_time=1

# 慢查询日志路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 错误日志路径
log_error=/var/log/mysql/error.log

# InnoDB Buffer Pool，生产环境通常按内存比例调整
innodb_buffer_pool_size=1G
```

修改配置后需要重启服务：

```bash
# 检查配置文件是否存在
ls -l /etc/mysql/my.cnf /etc/my.cnf

# 重启 MySQL 服务
sudo systemctl restart mysql

# 查看服务状态
sudo systemctl status mysql
```

验证配置是否生效：

```sql
-- 查看端口
SHOW VARIABLES LIKE 'port';

-- 查看字符集
SHOW VARIABLES LIKE 'character_set_server';

-- 查看排序规则
SHOW VARIABLES LIKE 'collation_server';

-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看默认时区
SHOW VARIABLES LIKE 'time_zone';
```

### 字符集与排序规则配置

字符集决定数据如何编码存储，排序规则决定字符串如何比较和排序。MySQL 8 默认服务端字符集和排序规则通常是 `utf8mb4` 与 `utf8mb4_0900_ai_ci`；官方文档说明也可以通过 `character-set-server` 和 `collation-server` 在启动参数或配置文件中显式设置。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/en/charset-server.html?utm_source=chatgpt.com))

推荐原则：

1. 新项目统一使用 `utf8mb4`，不要使用 `utf8`，因为 MySQL 中的 `utf8` 历史上并不等价于完整 UTF-8。
2. MySQL 8 新项目可使用 `utf8mb4_0900_ai_ci`。
3. 如果需要兼容旧系统或跨库比较行为，常见选择是 `utf8mb4_unicode_ci` 或 `utf8mb4_general_ci`，但应统一全库策略。
4. 表、字段、连接、客户端字符集应尽量保持一致，避免乱码和索引比较异常。

配置服务端字符集：

```ini
[mysqld]
# 设置服务端默认字符集
character-set-server=utf8mb4

# 设置服务端默认排序规则
collation-server=utf8mb4_0900_ai_ci

[client]
# 设置客户端默认字符集
default-character-set=utf8mb4
```

创建数据库时指定字符集和排序规则：

```sql
-- 创建数据库并指定字符集与排序规则
CREATE DATABASE demo_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

创建表时指定字符集和排序规则：

```sql
-- 创建用户表并指定表级字符集与排序规则
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';
```

查看字符集配置：

```sql
-- 查看当前连接、数据库、服务端字符集
SHOW VARIABLES LIKE 'character_set%';

-- 查看当前排序规则配置
SHOW VARIABLES LIKE 'collation%';

-- 查看数据库建库语句
SHOW CREATE DATABASE demo_db;

-- 查看表建表语句
SHOW CREATE TABLE sys_user;
```

### 时区配置

MySQL 时区会影响 `TIMESTAMP`、`NOW()`、`CURRENT_TIMESTAMP`、日志时间、业务时间字段和 Java 应用读取结果。官方文档说明，全局服务端时区可以通过启动参数 `--default-time-zone` 或配置文件中的 `default-time-zone` 设置，也可以在运行时通过 `SET GLOBAL time_zone = timezone` 修改；每个客户端连接也有自己的会话时区。([MySQL开发者专区](https://dev.mysql.com/doc/refman/8.0/ja/time-zone-support.html?utm_source=chatgpt.com))

推荐策略：

1. 单体国内业务系统可以统一使用 `+08:00` 或 `Asia/Shanghai`。
2. 跨时区系统建议数据库使用 UTC，应用层按用户时区展示。
3. Java 应用连接串应明确 `serverTimezone` 或使用驱动推荐配置，避免时区推断异常。
4. 不建议生产环境中不同应用使用不同会话时区写入同一业务时间字段。

配置文件方式：

```ini
[mysqld]
# 设置 MySQL 服务端默认时区
default-time-zone='+08:00'
```

运行时查看时区：

```sql
-- 查看系统时区、全局时区、当前会话时区
SELECT
    @@system_time_zone AS system_time_zone,
    @@global.time_zone AS global_time_zone,
    @@session.time_zone AS session_time_zone;

-- 查看当前时间
SELECT NOW(), UTC_TIMESTAMP();
```

临时修改当前会话时区：

```sql
-- 当前连接使用东八区
SET time_zone = '+08:00';

-- 当前连接使用 UTC
SET time_zone = '+00:00';
```

修改全局时区：

```sql
-- 修改全局时区，新连接生效；重启后是否保留取决于配置文件
SET GLOBAL time_zone = '+08:00';
```

Docker Compose 中配置时区：

```yaml
services:
  mysql8:
    image: mysql:8.0
    environment:
      # 容器系统时区
      TZ: Asia/Shanghai
    command:
      # MySQL 服务端默认时区
      - --default-time-zone=+08:00
```

### 端口与连接配置

MySQL 默认监听端口是 `3306`。端口、监听地址、最大连接数、连接超时时间和防火墙策略共同决定客户端能否访问 MySQL。配置远程访问时，不应只修改 `bind-address`，还需要检查用户授权、网络策略、防火墙、安全组和密码策略。

常见配置项：

| 配置项                | 说明                 | 示例                    |
| --------------------- | -------------------- | ----------------------- |
| `port`                | MySQL 服务端监听端口 | `3306`                  |
| `bind-address`        | 服务端监听地址       | `127.0.0.1` / `0.0.0.0` |
| `max_connections`     | 最大连接数           | `500`                   |
| `connect_timeout`     | 建立连接超时时间     | `10`                    |
| `wait_timeout`        | 非交互连接空闲超时   | `28800`                 |
| `interactive_timeout` | 交互连接空闲超时     | `28800`                 |
| `max_allowed_packet`  | 单个通信包最大值     | `64M`                   |

配置示例：

```ini
[mysqld]
# MySQL 监听端口
port=3306

# 监听所有网卡；生产环境需配合防火墙和授权控制
bind-address=0.0.0.0

# 最大连接数
max_connections=500

# 连接建立超时时间，单位秒
connect_timeout=10

# 非交互连接空闲超时时间，单位秒
wait_timeout=28800

# 交互式连接空闲超时时间，单位秒
interactive_timeout=28800

# 单个通信包最大值
max_allowed_packet=64M
```

查看监听端口：

```bash
# 查看 MySQL 监听端口
sudo ss -lntp | grep 3306

# 查看 MySQL 服务进程
ps -ef | grep mysqld
```

查看 MySQL 连接配置：

```sql
-- 查看端口
SHOW VARIABLES LIKE 'port';

-- 查看监听地址
SHOW VARIABLES LIKE 'bind_address';

-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看当前连接情况
SHOW STATUS LIKE 'Threads_connected';

-- 查看最大历史连接数
SHOW STATUS LIKE 'Max_used_connections';
```

创建远程访问用户示例：

```sql
-- 创建允许从指定网段访问的业务用户
CREATE USER 'app_user'@'192.168.1.%' IDENTIFIED BY 'App_123456';

-- 授权访问指定数据库
GRANT SELECT, INSERT, UPDATE, DELETE ON demo_db.* TO 'app_user'@'192.168.1.%';

-- 刷新权限
FLUSH PRIVILEGES;
```

不建议使用下面这种过宽授权：

```sql
-- 不推荐：允许任意来源使用 root 访问所有库
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
```

远程连接验证：

```bash
# 从应用服务器验证数据库连接
mysql -h 192.168.1.10 -P 3306 -u app_user -p demo_db

# 测试端口连通性
nc -vz 192.168.1.10 3306
```

生产环境端口与连接配置建议：

1. 数据库端口不要暴露到公网。
2. 使用安全组、防火墙或内网访问控制限制来源 IP。
3. 禁止 root 用户远程登录。
4. 为应用创建独立账号，并只授予必要库表权限。
5. 根据连接池规模设置 `max_connections`，避免连接池总和超过数据库承载能力。
6. 开启监控，持续观察连接数、慢查询、锁等待和错误日志。


## 数据库基础操作

数据库是 MySQL 中用于组织数据表、视图、存储过程、函数、触发器等对象的逻辑容器。项目开发中通常会按业务系统、微服务模块或环境隔离数据库，例如 `mall_user`、`mall_order`、`dev_demo`、`test_demo` 等。

### 创建数据库

创建数据库用于初始化一个新的业务数据空间。创建时建议显式指定字符集和排序规则，避免不同环境默认配置不一致导致字符乱码、排序差异或索引比较异常。

创建普通业务数据库：

```sql
-- 创建数据库，指定字符集和排序规则
CREATE DATABASE demo_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

如果数据库可能已经存在，可以使用 `IF NOT EXISTS` 避免重复创建时报错：

```sql
-- 如果数据库不存在，则创建数据库
CREATE DATABASE IF NOT EXISTS demo_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

创建完成后可以查看数据库定义：

```sql
-- 查看数据库创建语句
SHOW CREATE DATABASE demo_db;
```

项目初始化脚本中推荐使用完整写法：

```sql
-- 创建业务数据库
CREATE DATABASE IF NOT EXISTS mall_order
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 切换到业务数据库
USE mall_order;
```

### 查看数据库

查看数据库用于确认当前 MySQL 实例中有哪些数据库，以及当前连接正在使用哪个数据库。开发和运维过程中，执行 DDL 或 DML 前应先确认当前库，避免误操作到其他环境或其他业务库。

查看所有数据库：

```sql
-- 查看当前实例中的所有数据库
SHOW DATABASES;
```

按名称模糊查询数据库：

```sql
-- 查看名称以 demo 开头的数据库
SHOW DATABASES LIKE 'demo%';
```

查看当前正在使用的数据库：

```sql
-- 查看当前连接选择的数据库
SELECT DATABASE();
```

查看数据库创建语句：

```sql
-- 查看指定数据库的字符集、排序规则等信息
SHOW CREATE DATABASE demo_db;
```

通过 `information_schema` 查看数据库信息：

```sql
-- 查询数据库的默认字符集和排序规则
SELECT
    SCHEMA_NAME AS database_name,
    DEFAULT_CHARACTER_SET_NAME AS charset_name,
    DEFAULT_COLLATION_NAME AS collation_name
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'demo_db';
```

### 选择数据库

选择数据库用于指定后续 SQL 默认操作的数据库。执行建表、查询、插入、更新、删除等操作前，应先使用 `USE` 切换到目标数据库。

切换数据库：

```sql
-- 切换到 demo_db 数据库
USE demo_db;
```

验证当前数据库：

```sql
-- 确认当前连接正在使用的数据库
SELECT DATABASE();
```

也可以在 SQL 中使用完整库表名，避免依赖当前数据库：

```sql
-- 使用完整库名和表名查询数据
SELECT *
FROM demo_db.sys_user;
```

在脚本中推荐先切换数据库，再执行后续表结构和数据初始化：

```sql
-- 选择数据库
USE demo_db;

-- 创建业务表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### 修改数据库

修改数据库通常用于调整默认字符集或排序规则。注意，修改数据库默认字符集不会自动修改已有表和已有字段的字符集，只会影响后续新建对象的默认配置。

修改数据库默认字符集和排序规则：

```sql
-- 修改数据库默认字符集和排序规则
ALTER DATABASE demo_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

查看修改结果：

```sql
-- 查看数据库定义
SHOW CREATE DATABASE demo_db;
```

如果需要修改已有表的字符集，需要对表单独执行转换：

```sql
-- 修改已有表的字符集和排序规则，并转换已有字符字段
ALTER TABLE sys_user
  CONVERT TO CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

使用 `ALTER DATABASE` 时需要注意，它只修改数据库层面的默认属性，不代表历史表、历史字段全部同步变更。生产环境修改字符集前应先备份，并在测试环境验证索引长度、排序结果和应用兼容性。

### 删除数据库

删除数据库会删除该数据库下的所有表、视图、存储过程、函数、触发器和数据。该操作风险很高，生产环境必须经过审批、备份和确认。

删除数据库：

```sql
-- 删除数据库，数据库不存在时会报错
DROP DATABASE demo_db;
```

使用 `IF EXISTS` 避免数据库不存在时报错：

```sql
-- 如果数据库存在，则删除数据库
DROP DATABASE IF EXISTS demo_db;
```

删除前建议先确认当前数据库和目标数据库：

```sql
-- 查看当前数据库
SELECT DATABASE();

-- 查看目标数据库是否存在
SHOW DATABASES LIKE 'demo_db';

-- 查看目标数据库下的表
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ENGINE,
    TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'demo_db';
```

生产环境不建议直接执行 `DROP DATABASE`。如果确实需要删除，推荐流程是：先停止业务写入，再完成全量备份，然后导出对象清单，最后由具备权限的人员执行删除。

### 数据库命名规范

数据库命名应稳定、清晰、可识别业务边界。命名不规范会导致多环境混淆、权限授权混乱、备份恢复困难，也会增加脚本维护成本。

推荐规范：

| 规范              | 说明                               | 示例                |
| ----------------- | ---------------------------------- | ------------------- |
| 使用小写字母      | 避免不同操作系统大小写敏感差异     | `mall_order`        |
| 使用下划线分隔    | 提高可读性                         | `user_center`       |
| 体现业务含义      | 不使用无意义缩写                   | `payment_center`    |
| 避免 MySQL 关键字 | 减少 SQL 转义成本                  | 不建议 `order`      |
| 区分环境          | 开发、测试、预发可加环境前缀或后缀 | `dev_mall_order`    |
| 不使用特殊字符    | 避免脚本和工具兼容问题             | 不建议 `mall-order` |
| 名称长度适中      | 简洁但可理解                       | `mall_product`      |

推荐命名示例：

```text
mall_user
mall_order
mall_product
mall_payment
sys_config
report_center
```

不推荐命名示例：

```text
test
db1
my-database
Order
user
prod
```

对于微服务项目，可以按服务边界建库：

```text
mall_user      -- 用户服务数据库
mall_order     -- 订单服务数据库
mall_product   -- 商品服务数据库
mall_payment   -- 支付服务数据库
```

### 字符集与排序规则选择

字符集决定字符串如何存储，排序规则决定字符串如何比较、排序和分组。MySQL 8 项目中建议统一使用 `utf8mb4`，因为它能完整支持中文、英文、Emoji 和更多 Unicode 字符。

常用选择如下：

| 字符集 / 排序规则    | 说明                                         | 使用建议                 |
| -------------------- | -------------------------------------------- | ------------------------ |
| `utf8mb4`            | 完整 Unicode 字符集                          | 新项目推荐               |
| `utf8mb4_0900_ai_ci` | MySQL 8 默认常见排序规则，不区分重音和大小写 | MySQL 8 新项目推荐       |
| `utf8mb4_general_ci` | 旧项目常见排序规则                           | 兼容历史系统时使用       |
| `utf8mb4_unicode_ci` | Unicode 排序规则，旧版本常见                 | 兼容旧 MySQL 时使用      |
| `utf8mb4_bin`        | 按二进制比较，区分大小写                     | 需要严格区分大小写时使用 |

新项目推荐：

```sql
-- 新项目推荐使用 utf8mb4 和 utf8mb4_0900_ai_ci
CREATE DATABASE app_demo
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

需要区分大小写的字段可以单独指定二进制排序规则：

```sql
-- token 字段使用 utf8mb4_bin，比较时区分大小写
CREATE TABLE api_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    token VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '访问令牌',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口令牌表';
```

查看当前库和表的字符集：

```sql
-- 查看数据库字符集和排序规则
SELECT
    SCHEMA_NAME,
    DEFAULT_CHARACTER_SET_NAME,
    DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'demo_db';

-- 查看表字符集和排序规则
SELECT
    TABLE_NAME,
    TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'demo_db';
```

## 数据表基础操作

数据表是 MySQL 中存储业务数据的核心对象。表结构设计直接影响数据一致性、查询性能、索引效率和后续扩展成本。项目开发中应先明确业务实体、字段类型、主键策略、索引设计、约束规则和字段注释，再创建数据表。

### 创建表

创建表用于定义业务数据结构。建表时建议明确存储引擎、字符集、主键、字段类型、默认值、非空约束、索引和字段注释。

创建一张用户表：

```sql
-- 创建系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';
```

常见建表要点：

| 要点            | 说明                                       |
| --------------- | ------------------------------------------ |
| `ENGINE=InnoDB` | 默认使用 InnoDB，支持事务、行锁、MVCC      |
| `id BIGINT`     | 业务增长空间更大，适合后台系统和分布式场景 |
| `NOT NULL`      | 能明确字段语义，减少空值判断成本           |
| `DEFAULT`       | 为状态、逻辑删除、时间字段设置默认值       |
| `COMMENT`       | 表和字段都应添加中文注释                   |
| `UNIQUE KEY`    | 用数据库约束保证唯一性                     |
| 普通索引        | 为高频查询条件建立合适索引                 |

创建表后验证结构：

```sql
-- 查看表结构
DESC sys_user;

-- 查看完整建表语句
SHOW CREATE TABLE sys_user;
```

### 查看表结构

查看表结构用于确认字段、类型、索引、约束、字符集和表注释。开发联调、问题排查、SQL 优化和数据库变更前都需要先查看表结构。

查看当前数据库中的所有表：

```sql
-- 查看当前数据库下的所有表
SHOW TABLES;
```

查看表字段结构：

```sql
-- 查看表字段简要结构
DESC sys_user;

-- 等价写法
DESCRIBE sys_user;
```

查看完整建表语句：

```sql
-- 查看完整建表语句，包括索引、字符集、表注释等
SHOW CREATE TABLE sys_user;
```

通过 `information_schema` 查看字段明细：

```sql
-- 查看表字段详细信息
SELECT
    COLUMN_NAME AS column_name,
    COLUMN_TYPE AS column_type,
    IS_NULLABLE AS is_nullable,
    COLUMN_DEFAULT AS column_default,
    COLUMN_KEY AS column_key,
    EXTRA AS extra,
    COLUMN_COMMENT AS column_comment
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'demo_db'
  AND TABLE_NAME = 'sys_user'
ORDER BY ORDINAL_POSITION;
```

查看表索引：

```sql
-- 查看表索引信息
SHOW INDEX FROM sys_user;
```

查看表基础信息：

```sql
-- 查看表的引擎、行数估算、字符集、创建时间等信息
SELECT
    TABLE_NAME,
    ENGINE,
    TABLE_ROWS,
    TABLE_COLLATION,
    CREATE_TIME,
    UPDATE_TIME,
    TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'demo_db'
  AND TABLE_NAME = 'sys_user';
```

### 修改表结构

修改表结构用于新增字段、修改字段类型、调整默认值、增加索引、删除字段等。生产环境执行表结构变更时需要评估数据量、锁表风险、执行时间、回滚方案和应用兼容性。

新增字段：

```sql
-- 新增最后登录时间字段
ALTER TABLE sys_user
  ADD COLUMN last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间';
```

在指定字段后新增字段：

```sql
-- 在 email 字段后新增头像字段
ALTER TABLE sys_user
  ADD COLUMN avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址'
  AFTER email;
```

修改字段类型和注释：

```sql
-- 修改昵称字段长度
ALTER TABLE sys_user
  MODIFY COLUMN nickname VARCHAR(128) DEFAULT NULL COMMENT '用户昵称';
```

修改字段名称：

```sql
-- 将 mobile 字段重命名为 phone
ALTER TABLE sys_user
  CHANGE COLUMN mobile phone VARCHAR(20) DEFAULT NULL COMMENT '手机号';
```

新增索引：

```sql
-- 为邮箱字段增加普通索引
ALTER TABLE sys_user
  ADD KEY idx_email (email);
```

新增唯一索引：

```sql
-- 为手机号字段增加唯一索引
ALTER TABLE sys_user
  ADD UNIQUE KEY uk_phone (phone);
```

删除索引：

```sql
-- 删除邮箱索引
ALTER TABLE sys_user
  DROP INDEX idx_email;
```

删除字段：

```sql
-- 删除头像字段
ALTER TABLE sys_user
  DROP COLUMN avatar_url;
```

修改表注释：

```sql
-- 修改表注释
ALTER TABLE sys_user COMMENT='系统用户信息表';
```

生产环境变更建议：

1. 大表新增字段应优先评估是否支持在线 DDL。
2. 不建议在业务高峰期修改大表结构。
3. 删除字段前应先确认应用代码、报表、任务脚本都不再使用。
4. 修改字段类型前应先检查历史数据是否兼容。
5. 增加唯一索引前应先检查是否存在重复数据。

检查重复数据示例：

```sql
-- 检查手机号是否存在重复值
SELECT
    phone,
    COUNT(*) AS count_num
FROM sys_user
WHERE phone IS NOT NULL
GROUP BY phone
HAVING COUNT(*) > 1;
```

### 删除表

删除表会删除表结构、表数据、索引和相关元数据。该操作不可直接恢复，除非有备份、binlog 或快照。生产环境删除表前必须确认依赖关系和备份状态。

删除表：

```sql
-- 删除用户表，表不存在时会报错
DROP TABLE sys_user;
```

避免表不存在时报错：

```sql
-- 如果表存在，则删除表
DROP TABLE IF EXISTS sys_user;
```

删除多张表：

```sql
-- 同时删除多张表
DROP TABLE IF EXISTS sys_user, sys_role, sys_user_role;
```

删除前建议先确认表数据量和表结构：

```sql
-- 查看表数据量
SELECT COUNT(*) AS total FROM sys_user;

-- 查看建表语句
SHOW CREATE TABLE sys_user;

-- 查看表是否存在
SHOW TABLES LIKE 'sys_user';
```

生产环境中，如果只是废弃表，建议先改名归档观察一段时间，而不是立即删除：

```sql
-- 将旧表重命名为归档表
RENAME TABLE sys_user TO sys_user_bak_20260509;
```

### 清空表数据

清空表数据用于删除表内所有数据，但保留表结构。MySQL 中常见方式有 `DELETE` 和 `TRUNCATE`。两者效果相似，但语义和执行方式不同。

使用 `DELETE` 清空数据：

```sql
-- 删除表中所有数据，属于 DML 操作
DELETE FROM sys_user;
```

使用 `TRUNCATE` 清空数据：

```sql
-- 清空整张表，通常会重置自增值
TRUNCATE TABLE sys_user;
```

两者对比：

| 操作                   | 特点                                   | 适用场景                   |
| ---------------------- | -------------------------------------- | -------------------------- |
| `DELETE FROM table`    | 可带 `WHERE`，逐行删除，可在事务中回滚 | 条件删除、需要事务控制     |
| `TRUNCATE TABLE table` | 清空整表，通常更快，会重置自增值       | 初始化测试表、清理临时数据 |
| `DROP TABLE table`     | 删除表结构和数据                       | 表彻底废弃                 |

按条件删除数据：

```sql
-- 删除逻辑删除状态的数据
DELETE FROM sys_user
WHERE deleted = 1;
```

清空前备份数据：

```sql
-- 创建备份表并保存当前数据
CREATE TABLE sys_user_bak_20260509 AS
SELECT * FROM sys_user;

-- 清空原表
TRUNCATE TABLE sys_user;
```

生产环境不建议直接执行无条件 `DELETE` 或 `TRUNCATE`。如果确实需要执行，应先确认当前数据库、目标表、数据量和备份状态。

### 复制表结构

复制表结构用于基于已有表快速创建一张结构相同的新表。它常用于开发测试、临时验证、归档准备和结构备份。

复制表结构，不复制数据：

```sql
-- 复制 sys_user 的表结构到 sys_user_template
CREATE TABLE sys_user_template LIKE sys_user;
```

查看复制后的表结构：

```sql
-- 查看新表结构
SHOW CREATE TABLE sys_user_template;
```

`CREATE TABLE ... LIKE ...` 会复制字段、索引、默认值、存储引擎和部分表属性，但不会复制数据、外键约束相关依赖和触发器。用于结构备份时，应额外确认索引、字符集和表注释是否符合预期。

使用查询结果创建空表：

```sql
-- 使用查询结果创建空表，只复制查询字段结构，不完整复制索引
CREATE TABLE sys_user_empty AS
SELECT *
FROM sys_user
WHERE 1 = 0;
```

两种方式区别：

| 方式                                       | 是否复制数据 | 是否复制索引 | 说明                       |
| ------------------------------------------ | ------------ | ------------ | -------------------------- |
| `CREATE TABLE new LIKE old`                | 否           | 是           | 推荐用于复制完整表结构     |
| `CREATE TABLE new AS SELECT ... WHERE 1=0` | 否           | 否           | 适合按查询字段生成临时结构 |

### 复制表结构与数据

复制表结构与数据用于快速生成备份表、测试表或临时分析表。复制前需要确认数据量，避免在生产库上创建大表副本导致磁盘空间、IO 和锁竞争问题。

先复制结构，再复制数据：

```sql
-- 复制表结构
CREATE TABLE sys_user_bak LIKE sys_user;

-- 复制表数据
INSERT INTO sys_user_bak
SELECT *
FROM sys_user;
```

按条件复制部分数据：

```sql
-- 复制启用状态的用户数据
CREATE TABLE sys_user_enabled LIKE sys_user;

INSERT INTO sys_user_enabled
SELECT *
FROM sys_user
WHERE status = 1;
```

使用 `CREATE TABLE ... AS SELECT ...` 复制结构和数据：

```sql
-- 复制表结构和数据，但不会完整复制原表索引
CREATE TABLE sys_user_bak_20260509 AS
SELECT *
FROM sys_user;
```

复制指定字段：

```sql
-- 只复制部分字段，用于临时分析
CREATE TABLE sys_user_report AS
SELECT
    id,
    username,
    nickname,
    status,
    created_at
FROM sys_user;
```

大表复制建议：

1. 优先在低峰期执行。
2. 先评估源表数据量和目标磁盘空间。
3. 大表复制建议分批插入，避免长事务。
4. 复制后检查目标表行数。
5. 如果需要保留索引，优先使用 `CREATE TABLE new LIKE old` 再 `INSERT INTO ... SELECT ...`。

行数校验：

```sql
-- 校验源表和备份表行数
SELECT 'sys_user' AS table_name, COUNT(*) AS total FROM sys_user
UNION ALL
SELECT 'sys_user_bak' AS table_name, COUNT(*) AS total FROM sys_user_bak;
```

### 表命名规范

表命名应准确表达业务含义，并保持全项目一致。表名是 SQL、代码实体、Mapper、接口、报表和运维脚本共同依赖的基础标识，命名不稳定会长期增加维护成本。

推荐规范：

| 规范               | 说明                               | 示例                     |
| ------------------ | ---------------------------------- | ------------------------ |
| 使用小写字母       | 避免大小写兼容问题                 | `sys_user`               |
| 使用下划线分隔     | 提高可读性                         | `order_item`             |
| 使用业务前缀       | 区分业务域或模块                   | `sys_`、`biz_`、`log_`   |
| 避免复数形式混乱   | 项目内统一单数或统一复数，推荐单数 | `sys_user`               |
| 避免关键字         | 不直接使用 `order`、`user` 等      | `order_info`、`sys_user` |
| 表名表达实体       | 不使用无意义名称                   | 不建议 `table1`          |
| 中间表清晰表达关系 | 使用两个实体名组合                 | `sys_user_role`          |
| 日志表单独标识     | 使用 `log_` 前缀或 `_log` 后缀     | `login_log`              |

常见表名前缀：

| 前缀   | 含义       | 示例                         |
| ------ | ---------- | ---------------------------- |
| `sys_` | 系统基础表 | `sys_user`、`sys_role`       |
| `biz_` | 业务主表   | `biz_order`、`biz_contract`  |
| `rel_` | 关系表     | `rel_user_role`              |
| `log_` | 日志表     | `log_login`、`log_operation` |
| `cfg_` | 配置表     | `cfg_dict`、`cfg_param`      |
| `tmp_` | 临时表     | `tmp_import_user`            |
| `his_` | 历史表     | `his_order_status`           |

推荐命名示例：

```text
sys_user
sys_role
sys_user_role
biz_order
biz_order_item
payment_record
login_log
operation_log
file_info
```

不推荐命名示例：

```text
user
order
UserInfo
tbl_user
t_user
table_001
sys-user
```

项目中常见表命名建议：

```text
sys_user              -- 系统用户表
sys_role              -- 系统角色表
sys_menu              -- 系统菜单表
sys_user_role         -- 用户角色关系表
biz_order             -- 订单主表
biz_order_item        -- 订单明细表
payment_record        -- 支付记录表
login_log             -- 登录日志表
operation_log         -- 操作日志表
```

表命名应和 Java 实体、Mapper、Service 保持清晰映射关系。例如 `sys_user` 对应 `SysUser`，`biz_order_item` 对应 `BizOrderItem`。这样可以降低 MyBatis、MyBatis-Plus、JPA 或代码生成器的适配成本。


## MySQL 数据类型

MySQL 数据类型决定字段的存储方式、取值范围、索引效率、比较规则和业务表达能力。表结构设计时，不应只考虑“能不能存”，还要考虑取值边界、查询条件、排序规则、索引长度、空间占用和后续扩展。

### 整数类型

整数类型用于存储不带小数的数值，例如主键 ID、状态值、数量、排序号、版本号、计数字段等。选择整数类型时，应根据业务范围选择合适大小，避免盲目使用 `BIGINT` 或过小类型导致溢出。

常见整数类型如下：

| 类型              | 存储空间 | 有符号范围                | 无符号范围        | 常见用途               |
| ----------------- | -------- | ------------------------- | ----------------- | ---------------------- |
| `TINYINT`         | 1 字节   | -128 到 127               | 0 到 255          | 状态、布尔值、类型枚举 |
| `SMALLINT`        | 2 字节   | -32768 到 32767           | 0 到 65535        | 小范围数量、等级       |
| `MEDIUMINT`       | 3 字节   | -8388608 到 8388607       | 0 到 16777215     | 中等范围数值           |
| `INT` / `INTEGER` | 4 字节   | -2147483648 到 2147483647 | 0 到 4294967295   | 普通计数、业务编号     |
| `BIGINT`          | 8 字节   | 约 ±922 亿亿              | 约 0 到 1844 亿亿 | 主键、雪花 ID、大数量  |

状态字段示例：

```sql
-- 使用 TINYINT 存储状态值
CREATE TABLE sys_user_status_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户状态示例表';
```

计数字段示例：

```sql
-- 使用 INT UNSIGNED 存储非负计数
CREATE TABLE article_counter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
    like_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章计数表';
```

整数类型设计建议：

1. 主键常用 `BIGINT`，便于长期增长和分布式 ID 扩展。
2. 状态字段、类型字段优先使用 `TINYINT`。
3. 数量、库存、次数等非负字段可以使用 `UNSIGNED`。
4. 不要使用 `INT(11)` 误以为限制 11 位数字，括号中的显示宽度不等于取值范围。
5. 金额不要使用整数直接存元，除非明确使用“分”为单位并在业务层统一处理。

### 小数类型

小数类型用于存储金额、比例、评分、重量、面积、经纬度等带小数的数据。MySQL 常用小数类型包括 `DECIMAL`、`FLOAT`、`DOUBLE`。业务金额必须优先使用 `DECIMAL`，避免浮点数精度误差。

常见小数类型如下：

| 类型           | 特点                                       | 适用场景                     |
| -------------- | ------------------------------------------ | ---------------------------- |
| `DECIMAL(M,D)` | 精确小数，按十进制存储                     | 金额、费率、结算、财务       |
| `FLOAT`        | 单精度浮点数，存在近似误差                 | 对精度要求低的测量值         |
| `DOUBLE`       | 双精度浮点数，精度高于 FLOAT，但仍是近似值 | 科学计算、地理坐标、统计指标 |

金额字段示例：

```sql
-- 使用 DECIMAL 存储金额，避免浮点数精度问题
CREATE TABLE payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1已支付，2已退款',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
```

比例字段示例：

```sql
-- 使用 DECIMAL 存储折扣率和税率
CREATE TABLE product_price_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    discount_rate DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '折扣率，例如0.8500表示85折',
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '税率',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格规则表';
```

小数类型设计建议：

1. 金额统一使用 `DECIMAL(18,2)` 或根据业务调整精度。
2. 汇率、费率、折扣率可使用 `DECIMAL(10,4)`、`DECIMAL(10,6)` 等。
3. `FLOAT` 和 `DOUBLE` 不适合用于金额等需要精确比较的数据。
4. `DECIMAL(M,D)` 中，`M` 表示总位数，`D` 表示小数位数。
5. 对金额字段建议设置 `NOT NULL DEFAULT 0.00`，减少空值处理成本。

### 字符串类型

字符串类型用于存储名称、编号、手机号、邮箱、地址、描述、备注、正文等文本数据。选择字符串类型时，应重点考虑最大长度、是否固定长度、是否需要索引、是否参与排序和比较。

常见字符串类型如下：

| 类型         | 说明           | 常见用途                   |
| ------------ | -------------- | -------------------------- |
| `CHAR(n)`    | 固定长度字符串 | 固定长度编码、性别、状态码 |
| `VARCHAR(n)` | 可变长度字符串 | 名称、手机号、邮箱、标题   |
| `TEXT`       | 长文本         | 文章正文、备注、描述       |
| `TINYTEXT`   | 小文本         | 简短说明                   |
| `MEDIUMTEXT` | 中等文本       | 较长内容                   |
| `LONGTEXT`   | 超长文本       | 大文本内容、文档内容       |

常规字符串字段示例：

```sql
-- 使用 VARCHAR 存储常见业务字符串
CREATE TABLE sys_user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    address VARCHAR(255) DEFAULT NULL COMMENT '联系地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';
```

长文本字段示例：

```sql
-- 使用 TEXT 存储文章正文
CREATE TABLE article_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT NOT NULL COMMENT '文章正文',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章内容表';
```

字符串类型设计建议：

1. 普通短文本优先使用 `VARCHAR`。
2. 固定长度编码可以使用 `CHAR`，例如固定长度业务码、国家编码等。
3. 长正文、长描述使用 `TEXT`，但不建议频繁作为查询条件。
4. `VARCHAR` 字段建立索引时要控制长度，过长字段可考虑前缀索引。
5. 手机号、身份证号、订单号等即使由数字组成，也通常应使用字符串类型，因为它们不是用于数学计算的数值。
6. 字符串字段应统一字符集和排序规则，避免跨表关联时字符集不一致。

### 日期时间类型

日期时间类型用于存储业务发生时间、创建时间、更新时间、过期时间、生日、统计日期等。常见类型包括 `DATE`、`TIME`、`DATETIME`、`TIMESTAMP`、`YEAR`。

常见日期时间类型如下：

| 类型        | 说明                       | 常见用途                       |
| ----------- | -------------------------- | ------------------------------ |
| `DATE`      | 日期，格式为 `YYYY-MM-DD`  | 生日、统计日期、业务日期       |
| `TIME`      | 时间，格式为 `HH:MM:SS`    | 时长、每天固定时间             |
| `DATETIME`  | 日期时间，不随时区自动转换 | 创建时间、更新时间、业务时间   |
| `TIMESTAMP` | 时间戳，受会话时区影响     | 记录系统时间、跨时区场景需谨慎 |
| `YEAR`      | 年份                       | 年度统计、生产年份             |

常用时间字段示例：

```sql
-- 使用 DATETIME 存储业务时间
CREATE TABLE order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

日期字段示例：

```sql
-- 使用 DATE 存储统计日期
CREATE TABLE daily_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_date DATE NOT NULL COMMENT '统计日期',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    order_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日报统计表';
```

日期时间类型设计建议：

1. 业务表常用 `DATETIME` 保存创建时间、更新时间、支付时间等。
2. 只需要日期时使用 `DATE`，不要用 `VARCHAR` 存日期。
3. `created_at` 建议使用 `DEFAULT CURRENT_TIMESTAMP`。
4. `updated_at` 建议使用 `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`。
5. 跨时区系统应统一数据库、应用和接口的时间规范。
6. 时间字段参与范围查询时，建议建立合适索引，例如 `(user_id, created_at)`、`(status, created_at)`。

### JSON 类型

JSON 类型用于存储结构可能变化、字段不固定或扩展属性较多的数据。MySQL 8 对 JSON 提供了字段类型、查询函数、修改函数和 `JSON_TABLE` 等能力，但 JSON 不应替代表结构设计。

JSON 字段示例：

```sql
-- 使用 JSON 存储扩展属性
CREATE TABLE product_extend_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    extend_attr JSON NOT NULL COMMENT '扩展属性JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品扩展信息表';
```

插入 JSON 数据：

```sql
-- 插入商品扩展属性
INSERT INTO product_extend_info (product_id, extend_attr)
VALUES
(
    10001,
    JSON_OBJECT(
        'color', '黑色',
        'size', 'XL',
        'material', '棉',
        'tags', JSON_ARRAY('热销', '新品')
    )
);
```

查询 JSON 字段：

```sql
-- 查询 JSON 中的颜色属性
SELECT
    product_id,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.color')) AS color
FROM product_extend_info
WHERE product_id = 10001;
```

修改 JSON 字段：

```sql
-- 修改 JSON 中的 size 属性
UPDATE product_extend_info
SET extend_attr = JSON_SET(extend_attr, '$.size', 'L')
WHERE product_id = 10001;
```

JSON 类型设计建议：

1. 核心查询字段不要放进 JSON，应设计为普通字段并建立索引。
2. JSON 适合存储扩展属性、第三方回调原文、动态配置等。
3. JSON 字段应保持结构相对稳定，不建议随意混用数组、对象和字符串。
4. 需要频繁按 JSON 内部字段过滤时，应考虑生成列或函数索引。
5. JSON 字段内容较大时，会增加读取、解析和网络传输成本。

### 枚举类型

枚举类型 `ENUM` 用于字段取值集合固定且变化较少的场景。它可以限制字段只能取指定值，但在项目开发中需要谨慎使用，因为枚举值变更需要修改表结构。

枚举字段示例：

```sql
-- 使用 ENUM 存储性别
CREATE TABLE user_base_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    gender ENUM('UNKNOWN', 'MALE', 'FEMALE') NOT NULL DEFAULT 'UNKNOWN' COMMENT '性别',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';
```

使用 `TINYINT` 替代枚举的示例：

```sql
-- 使用 TINYINT 存储枚举值，更适合 Java 项目维护
CREATE TABLE user_base_info_int_enum (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';
```

枚举类型设计建议：

1. 数据库层面强限制取值时，可以使用 `ENUM`。
2. Java 项目中更常见的做法是使用 `TINYINT` 或 `VARCHAR` 存储枚举编码。
3. 取值经常变化的字段不建议使用 `ENUM`，否则每次新增枚举都需要 DDL。
4. 使用 `ENUM` 时，应避免依赖枚举内部排序和值位置。
5. 业务状态字段推荐使用 `TINYINT` 并在代码中定义枚举类。

### 二进制类型

二进制类型用于存储字节数据，例如文件内容、加密结果、哈希值、图片、小型附件等。常见类型包括 `BINARY`、`VARBINARY`、`BLOB`、`MEDIUMBLOB`、`LONGBLOB`。

常见二进制类型如下：

| 类型           | 说明               | 常见用途                       |
| -------------- | ------------------ | ------------------------------ |
| `BINARY(n)`    | 固定长度二进制数据 | 固定长度摘要                   |
| `VARBINARY(n)` | 可变长度二进制数据 | token、加密结果                |
| `TINYBLOB`     | 小二进制对象       | 小文件片段                     |
| `BLOB`         | 二进制对象         | 小附件、图片                   |
| `MEDIUMBLOB`   | 中等二进制对象     | 中等文件                       |
| `LONGBLOB`     | 大二进制对象       | 大文件，不建议常规业务直接使用 |

存储哈希值示例：

```sql
-- 使用 VARBINARY 存储哈希摘要
CREATE TABLE file_hash_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    sha256_hash VARBINARY(32) NOT NULL COMMENT 'SHA-256哈希值',
    file_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sha256_hash (sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件哈希记录表';
```

存储小文件示例：

```sql
-- 使用 BLOB 存储小型二进制内容
CREATE TABLE small_file_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    content_type VARCHAR(100) NOT NULL COMMENT '文件类型',
    file_content BLOB NOT NULL COMMENT '文件内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小文件内容表';
```

二进制类型设计建议：

1. 普通业务系统不建议将大文件直接存入 MySQL。
2. 文件通常应存储到 MinIO、OSS、S3、NAS 等对象存储，MySQL 只保存文件元数据和访问地址。
3. 哈希、加密结果、二进制 token 可以使用 `BINARY` 或 `VARBINARY`。
4. `BLOB` 字段不适合频繁查询、排序和索引。
5. 大二进制字段会增加备份、复制、网络传输和 Buffer Pool 压力。

### 数据类型选择原则

数据类型选择的目标是：准确表达业务含义、控制存储空间、减少类型转换、提高索引效率，并为后续扩展预留合理空间。不要为了“省事”全部使用 `VARCHAR`，也不要为了“保险”全部使用最大类型。

常用选择建议如下：

| 业务字段 | 推荐类型                 | 示例                          |
| -------- | ------------------------ | ----------------------------- |
| 主键 ID  | `BIGINT`                 | `id BIGINT`                   |
| 状态     | `TINYINT`                | `status TINYINT`              |
| 是否删除 | `TINYINT`                | `deleted TINYINT`             |
| 数量     | `INT UNSIGNED`           | `stock INT UNSIGNED`          |
| 金额     | `DECIMAL(18,2)`          | `pay_amount DECIMAL(18,2)`    |
| 比率     | `DECIMAL(10,4)`          | `discount_rate DECIMAL(10,4)` |
| 手机号   | `VARCHAR(20)`            | `mobile VARCHAR(20)`          |
| 邮箱     | `VARCHAR(128)`           | `email VARCHAR(128)`          |
| 订单号   | `VARCHAR(64)`            | `order_no VARCHAR(64)`        |
| 创建时间 | `DATETIME`               | `created_at DATETIME`         |
| 扩展属性 | `JSON`                   | `extend_attr JSON`            |
| 备注     | `VARCHAR(500)` 或 `TEXT` | `remark VARCHAR(500)`         |

综合示例：

```sql
-- 综合使用常见数据类型设计订单表
CREATE TABLE order_info_type_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    item_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '商品数量',
    receiver_mobile VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    receiver_address VARCHAR(255) NOT NULL COMMENT '收货地址',
    extend_attr JSON DEFAULT NULL COMMENT '扩展属性',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息类型示例表';
```

设计原则总结：

1. 能用数字表达的状态，不要用长字符串。
2. 能用 `DATE`、`DATETIME` 表达的时间，不要用字符串。
3. 金额使用 `DECIMAL`，不要使用 `FLOAT` 或 `DOUBLE`。
4. 查询条件字段要控制长度，便于建立索引。
5. 字段类型要与 Java 类型保持一致，避免隐式转换。
6. 字段要留有适度扩展空间，但不要无意义放大。
7. 表结构设计前应先明确字段是否必填、是否唯一、是否参与查询、是否参与排序。

## 字段与约束设计

字段与约束用于保证数据结构清晰、业务规则稳定和数据质量可控。合理的约束设计可以把部分数据错误阻挡在数据库层，减少脏数据进入系统的概率。常见约束包括主键、非空、默认值、唯一、外键和检查约束。

### 主键约束

主键用于唯一标识表中的每一行数据。一个表只能有一个主键，主键字段不能为 `NULL`，并且值必须唯一。InnoDB 表会围绕主键组织数据，因此主键设计会影响聚簇索引、二级索引和写入性能。

创建主键示例：

```sql
-- 创建表时定义主键
CREATE TABLE sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

后续添加主键：

```sql
-- 为已有表添加主键
ALTER TABLE sys_dept
  ADD PRIMARY KEY (id);
```

主键设计建议：

1. 每张业务表都应有主键。
2. 推荐使用无业务含义的代理主键，例如 `BIGINT id`。
3. 不建议使用手机号、身份证号、订单号等业务字段作为主键。
4. 主键值不应频繁修改。
5. InnoDB 表建议使用较短、稳定、递增或趋势递增的主键。
6. 分布式系统可使用雪花 ID、号段模式或数据库自增方案。

### 自增字段

自增字段 `AUTO_INCREMENT` 用于在插入数据时自动生成递增数值，通常配合整数主键使用。它适合单库单表或中小规模系统，使用简单，查询友好。

自增主键示例：

```sql
-- 使用 AUTO_INCREMENT 定义自增主键
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

插入数据时不需要指定自增字段：

```sql
-- 插入角色数据，id 由数据库自动生成
INSERT INTO sys_role (role_code, role_name, status)
VALUES ('admin', '管理员', 1);
```

查看当前自增值：

```sql
-- 查看表的自增值
SHOW TABLE STATUS LIKE 'sys_role';
```

修改自增起始值：

```sql
-- 修改下一次自增起始值
ALTER TABLE sys_role AUTO_INCREMENT = 1000;
```

自增字段设计建议：

1. 自增字段必须建立索引，通常作为主键。
2. 一个表只能有一个 `AUTO_INCREMENT` 字段。
3. 不要依赖自增 ID 连续，删除、回滚、插入失败都可能造成间隙。
4. 分库分表场景不建议直接依赖单表自增 ID 作为全局唯一标识。
5. 对外暴露业务编号时，不建议直接使用数据库自增 ID。

### 非空约束

非空约束 `NOT NULL` 用于限制字段必须有值。它可以让字段语义更明确，也能减少应用层处理 `NULL` 的复杂度。对于业务必填字段、状态字段、金额字段、时间字段，应优先设置为非空。

非空字段示例：

```sql
-- 使用 NOT NULL 约束必填字段
CREATE TABLE product_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    stock_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';
```

修改字段为非空：

```sql
-- 修改商品名称为非空字段
ALTER TABLE product_info
  MODIFY COLUMN product_name VARCHAR(200) NOT NULL COMMENT '商品名称';
```

非空约束设计建议：

1. 必填字段使用 `NOT NULL`。
2. 状态、逻辑删除、版本号、创建时间、更新时间建议 `NOT NULL`。
3. 金额、数量字段建议 `NOT NULL DEFAULT 0`。
4. 可选字段可以允许 `NULL`，例如备注、头像、取消时间、支付时间。
5. 修改已有字段为 `NOT NULL` 前，应先清理历史空值。

清理历史空值示例：

```sql
-- 将空库存修正为 0
UPDATE product_info
SET stock_count = 0
WHERE stock_count IS NULL;
```

### 默认值约束

默认值约束 `DEFAULT` 用于在插入数据未指定字段值时自动填充默认值。合理的默认值可以简化插入 SQL，并减少空值导致的业务判断。

默认值示例：

```sql
-- 使用 DEFAULT 定义状态、金额、时间默认值
CREATE TABLE account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    available_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额表';
```

修改默认值：

```sql
-- 修改状态字段默认值
ALTER TABLE account_balance
  ALTER COLUMN status SET DEFAULT 1;
```

删除默认值：

```sql
-- 删除状态字段默认值
ALTER TABLE account_balance
  ALTER COLUMN status DROP DEFAULT;
```

默认值设计建议：

1. 状态字段一般设置默认值，例如 `status TINYINT NOT NULL DEFAULT 1`。
2. 逻辑删除字段一般使用 `deleted TINYINT NOT NULL DEFAULT 0`。
3. 金额和数量字段一般使用 `0` 作为默认值。
4. 创建时间一般使用 `DEFAULT CURRENT_TIMESTAMP`。
5. 更新时间一般使用 `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`。
6. 不要给无明确业务含义的字段设置随意默认值。

### 唯一约束

唯一约束用于保证一个或多个字段的值不能重复。它常用于用户名、手机号、邮箱、订单号、支付流水号、业务编码等字段。唯一约束是数据库层防重复的重要手段，比单纯依赖应用层判断更可靠。

单字段唯一约束示例：

```sql
-- 用户名唯一
CREATE TABLE sys_user_unique_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户唯一约束示例表';
```

联合唯一约束示例：

```sql
-- 同一租户下用户名唯一
CREATE TABLE tenant_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户表';
```

为已有表添加唯一索引：

```sql
-- 为订单号添加唯一约束
ALTER TABLE order_info
  ADD UNIQUE KEY uk_order_no (order_no);
```

唯一约束设计建议：

1. 有天然唯一性的业务字段，应建立唯一约束。
2. 分租户系统中，通常使用 `(tenant_id, biz_code)` 作为联合唯一约束。
3. 增加唯一约束前必须先检查历史重复数据。
4. 唯一约束可以作为幂等写入的重要基础。
5. 对允许 `NULL` 的唯一字段要特别注意，MySQL 中唯一索引允许多个 `NULL` 值。

检查重复数据示例：

```sql
-- 检查同一租户下用户名是否重复
SELECT
    tenant_id,
    username,
    COUNT(*) AS count_num
FROM tenant_user
GROUP BY tenant_id, username
HAVING COUNT(*) > 1;
```

### 外键约束

外键约束用于维护表与表之间的引用完整性。它可以防止子表引用不存在的父表数据，也可以定义父表删除或更新时子表的处理规则。外键在强一致模型中有价值，但在高并发互联网业务中也可能增加写入成本、锁等待和变更复杂度。

外键约束示例：

```sql
-- 使用外键约束维护用户与订单关系
CREATE TABLE fk_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外键用户表';

CREATE TABLE fk_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_order_user_id
        FOREIGN KEY (user_id)
        REFERENCES fk_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外键订单表';
```

带删除规则的外键示例：

```sql
-- 删除用户时，自动删除用户角色关系
CREATE TABLE sys_user_role_fk_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    CONSTRAINT fk_user_role_user_id
        FOREIGN KEY (user_id)
        REFERENCES fk_user (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色外键示例表';
```

删除外键：

```sql
-- 删除外键约束
ALTER TABLE fk_order
  DROP FOREIGN KEY fk_order_user_id;
```

外键设计建议：

1. 外键字段和被引用字段的数据类型必须一致或兼容。
2. 被引用字段必须是索引字段，通常是主键或唯一键。
3. 外键会增加写入和删除时的约束检查成本。
4. 分库分表、微服务拆库场景通常不使用数据库外键，而由应用层保证一致性。
5. 中后台、单体系统、强一致小规模业务可以使用外键增强数据完整性。
6. 生产项目中是否使用外键，应根据团队规范统一决定，不建议混用。

### 检查约束

检查约束 `CHECK` 用于限制字段必须满足指定条件，例如金额不能小于 0、状态只能在指定范围内、结束时间必须大于开始时间等。MySQL 8.0.16 及之后版本会实际检查 `CHECK` 约束。

金额检查约束示例：

```sql
-- 使用 CHECK 限制金额不能小于 0
CREATE TABLE wallet_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    frozen_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT chk_balance_amount CHECK (balance_amount >= 0),
    CONSTRAINT chk_frozen_amount CHECK (frozen_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包账户表';
```

状态检查约束示例：

```sql
-- 使用 CHECK 限制状态取值范围
CREATE TABLE coupon_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未开始，1进行中，2已结束，3已作废',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT chk_coupon_status CHECK (coupon_status IN (0, 1, 2, 3)),
    CONSTRAINT chk_coupon_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券信息表';
```

新增检查约束：

```sql
-- 为已有表新增检查约束
ALTER TABLE wallet_account
  ADD CONSTRAINT chk_balance_frozen
  CHECK (balance_amount >= frozen_amount);
```

删除检查约束：

```sql
-- 删除检查约束
ALTER TABLE wallet_account
  DROP CHECK chk_balance_frozen;
```

检查约束设计建议：

1. 适合表达简单、稳定、不会频繁变化的数据规则。
2. 不适合表达复杂业务流程，复杂规则应放在业务代码中。
3. 状态取值范围、金额非负、时间先后关系适合使用 `CHECK`。
4. 修改历史表添加 `CHECK` 前，需要先检查已有数据是否满足条件。
5. 如果项目需要兼容旧版本 MySQL，应确认目标版本是否真正执行 `CHECK`。

### 字段注释

字段注释用于说明字段含义、枚举值、单位、来源和特殊规则。规范的字段注释可以降低维护成本，也能让数据库工具、代码生成器、接口文档和开发人员更容易理解表结构。

字段注释示例：

```sql
-- 字段注释说明业务含义、枚举值和单位
CREATE TABLE inventory_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    change_type TINYINT NOT NULL COMMENT '变动类型：1入库，2出库，3锁定，4释放',
    change_count INT NOT NULL COMMENT '变动数量，正数增加，负数减少',
    before_count INT NOT NULL COMMENT '变动前库存数量',
    after_count INT NOT NULL COMMENT '变动后库存数量',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_product_id_created_at (product_id, created_at),
    KEY idx_biz_no (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动记录表';
```

修改字段注释：

```sql
-- 修改字段注释时需要完整写出字段类型和约束
ALTER TABLE inventory_record
  MODIFY COLUMN change_type TINYINT NOT NULL COMMENT '变动类型：1入库，2出库，3锁定，4释放，5盘点';
```

查看字段注释：

```sql
-- 查询字段注释
SELECT
    COLUMN_NAME AS column_name,
    COLUMN_TYPE AS column_type,
    IS_NULLABLE AS is_nullable,
    COLUMN_DEFAULT AS column_default,
    COLUMN_COMMENT AS column_comment
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'demo_db'
  AND TABLE_NAME = 'inventory_record'
ORDER BY ORDINAL_POSITION;
```

字段注释建议：

1. 所有业务字段都应添加注释。
2. 枚举字段必须说明枚举值含义。
3. 金额、数量、时间字段应说明单位或口径。
4. 外部系统字段应说明来源。
5. 逻辑删除、状态、版本号等通用字段应保持注释统一。
6. 修改字段注释时要保留原字段类型、非空约束和默认值，避免误改字段定义。

### 字段命名规范

字段命名应清晰表达业务含义，并保持全项目一致。字段名会出现在 SQL、实体类、Mapper、接口响应、报表和数据同步任务中，一旦混乱会持续影响开发效率和数据治理。

推荐规范：

| 规范           | 说明                       | 示例                     |
| -------------- | -------------------------- | ------------------------ |
| 使用小写字母   | 避免大小写兼容问题         | `user_id`                |
| 使用下划线分隔 | 与数据库命名风格一致       | `created_at`             |
| 避免关键字     | 减少反引号转义             | 不建议 `order`、`desc`   |
| 表达业务含义   | 不使用无意义缩写           | `payment_amount`         |
| 布尔状态统一   | 使用 `is_` 或明确状态字段  | `is_default`、`status`   |
| 时间字段统一   | 使用 `_at` 或 `_time` 结尾 | `created_at`、`pay_time` |
| 金额字段统一   | 使用 `_amount` 结尾        | `pay_amount`             |
| 数量字段统一   | 使用 `_count` 结尾         | `stock_count`            |
| 编码字段统一   | 使用 `_code` 结尾          | `role_code`              |
| 编号字段统一   | 使用 `_no` 结尾            | `order_no`               |

常见通用字段：

```sql
-- 常见通用字段示例
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
version INT NOT NULL DEFAULT 0 COMMENT '版本号'
```

字段命名示例：

| 业务含义 | 推荐字段名   | 不推荐字段名         |
| -------- | ------------ | -------------------- |
| 用户 ID  | `user_id`    | `uid`、`userid`      |
| 订单号   | `order_no`   | `orderNo`、`no`      |
| 支付金额 | `pay_amount` | `money`、`amount1`   |
| 创建时间 | `created_at` | `create_time1`       |
| 更新时间 | `updated_at` | `modify_time`        |
| 删除标记 | `deleted`    | `is_del`、`del_flag` |
| 状态     | `status`     | `state1`             |
| 备注     | `remark`     | `memo1`、`desc`      |

完整字段命名示例：

```sql
-- 使用规范字段命名设计订单支付表
CREATE TABLE order_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_channel TINYINT NOT NULL COMMENT '支付渠道：1支付宝，2微信，3银行卡',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败，3已退款',
    paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_id (order_id),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_pay_status_created_at (pay_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单支付表';
```

字段命名设计建议：

1. 项目内统一使用一种命名风格，不要混用 `created_at`、`create_time`、`gmt_create`。
2. Java 实体字段可以使用驼峰命名，通过 MyBatis、MyBatis-Plus 或 JPA 映射到下划线字段。
3. 字段名不要过短，避免含义不清。
4. 字段名也不要过长，避免 SQL 可读性下降。
5. 禁止使用 `select`、`order`、`group`、`desc` 等关键字作为字段名。
6. 对通用字段建立团队规范，所有业务表保持一致。


## 数据增删改查

数据增删改查是 MySQL 最基础、最高频的操作，分别对应 `INSERT`、`SELECT`、`UPDATE`、`DELETE`。项目开发中应重点关注 SQL 的安全性、条件准确性、索引命中、事务边界和批量操作性能，避免无条件更新、无条件删除、深分页和循环单条写入。

本节示例默认使用如下用户表：

```sql
-- 用户表示例，后续增删改查基于该表演示
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### INSERT 新增数据

`INSERT` 用于向表中新增数据。新增数据时建议显式指定字段名，不建议依赖表字段顺序插入，因为后续表结构调整可能导致 SQL 语义错误。

新增单条用户数据：

```sql
-- 新增一条用户数据，id、created_at、updated_at 使用默认值
INSERT INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
) VALUES (
    'zhangsan',
    '张三',
    '13800000001',
    'zhangsan@example.com',
    1
);
```

只插入必要字段：

```sql
-- 只插入必填字段和核心字段，其余字段使用默认值
INSERT INTO sys_user (
    username,
    nickname
) VALUES (
    'lisi',
    '李四'
);
```

插入后查看新增数据：

```sql
-- 按用户名查询新增数据
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status,
    created_at
FROM sys_user
WHERE username = 'zhangsan';
```

不推荐省略字段名的写法：

```sql
-- 不推荐：依赖表字段顺序，后续字段变化后容易出错
INSERT INTO sys_user
VALUES (
    NULL,
    'wangwu',
    '王五',
    '13800000003',
    'wangwu@example.com',
    1,
    0,
    0,
    NOW(),
    NOW()
);
```

`INSERT` 使用建议：

| 建议                 | 说明                     |
| -------------------- | ------------------------ |
| 显式指定字段名       | 提高 SQL 可读性和兼容性  |
| 不直接插入自增主键   | 让数据库自动生成主键     |
| 必填字段必须传值     | 避免违反 `NOT NULL` 约束 |
| 状态字段使用默认值   | 减少重复 SQL             |
| 唯一字段提前校验     | 避免唯一索引冲突         |
| 批量写入优先合并 SQL | 减少网络往返和事务开销   |

### 批量新增数据

批量新增用于一次插入多行数据，适合初始化字典、导入数据、批量创建业务记录等场景。相比循环执行多条单行 `INSERT`，批量新增通常性能更好。

批量插入用户数据：

```sql
-- 一次插入多条用户数据
INSERT INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
) VALUES
    ('user001', '用户001', '13800001001', 'user001@example.com', 1),
    ('user002', '用户002', '13800001002', 'user002@example.com', 1),
    ('user003', '用户003', '13800001003', 'user003@example.com', 0);
```

批量插入部分字段：

```sql
-- 批量插入时可以只写必要字段
INSERT INTO sys_user (
    username,
    nickname
) VALUES
    ('user004', '用户004'),
    ('user005', '用户005'),
    ('user006', '用户006');
```

从其他表批量插入：

```sql
-- 从临时导入表中批量写入正式用户表
INSERT INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
)
SELECT
    import_username,
    import_nickname,
    import_mobile,
    import_email,
    1 AS status
FROM tmp_import_user
WHERE import_username IS NOT NULL;
```

批量新增后校验：

```sql
-- 校验批量插入结果
SELECT
    COUNT(*) AS total
FROM sys_user
WHERE username IN ('user001', 'user002', 'user003');
```

批量新增注意事项：

1. 单条批量 SQL 不宜过大，过大会受 `max_allowed_packet` 限制。
2. 大批量导入时建议分批提交，例如每批 500 到 5000 行，具体取决于字段数量和数据库性能。
3. 批量插入涉及唯一索引时，应先清洗重复数据。
4. 导入历史数据时应明确是否写入 `created_at`、`updated_at`。
5. 大批量写入前应评估索引维护成本，必要时采用离线导入方案。

### SELECT 查询数据

`SELECT` 用于查询表中的数据。项目开发中不建议直接使用 `SELECT *`，应明确查询字段，减少网络传输、避免字段变更影响接口语义，也有利于覆盖索引优化。

查询指定字段：

```sql
-- 查询启用状态的用户列表
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status,
    created_at
FROM sys_user
WHERE status = 1
  AND deleted = 0;
```

按主键查询单条数据：

```sql
-- 根据主键查询用户
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status,
    deleted,
    created_at,
    updated_at
FROM sys_user
WHERE id = 1;
```

按唯一字段查询：

```sql
-- 根据用户名查询用户
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status
FROM sys_user
WHERE username = 'zhangsan'
  AND deleted = 0;
```

统计数据：

```sql
-- 统计启用用户数量
SELECT
    COUNT(*) AS enabled_user_count
FROM sys_user
WHERE status = 1
  AND deleted = 0;
```

查询建议：

| 建议                         | 说明                       |
| ---------------------------- | -------------------------- |
| 避免 `SELECT *`              | 明确字段，减少无效数据传输 |
| 条件字段尽量有索引           | 提高查询效率               |
| 高频查询控制返回字段         | 减少回表和网络成本         |
| 查询逻辑删除表时带 `deleted` | 避免查出已删除数据         |
| 分页查询必须有稳定排序       | 避免翻页数据重复或遗漏     |

### UPDATE 修改数据

`UPDATE` 用于修改表中已有数据。执行更新时必须带明确的 `WHERE` 条件，生产环境禁止无条件更新。涉及并发修改时，应考虑版本号、状态条件或事务锁。

按主键修改用户信息：

```sql
-- 根据主键修改用户昵称和邮箱
UPDATE sys_user
SET
    nickname = '张三丰',
    email = 'zhangsanfeng@example.com'
WHERE id = 1;
```

按唯一字段修改状态：

```sql
-- 根据用户名禁用用户
UPDATE sys_user
SET
    status = 0
WHERE username = 'zhangsan'
  AND deleted = 0;
```

使用版本号实现乐观锁更新：

```sql
-- 使用 version 字段控制并发更新
UPDATE sys_user
SET
    nickname = '张三更新',
    version = version + 1
WHERE id = 1
  AND version = 0
  AND deleted = 0;
```

更新时间字段通常会通过 `ON UPDATE CURRENT_TIMESTAMP` 自动更新，也可以显式指定：

```sql
-- 显式修改更新时间
UPDATE sys_user
SET
    mobile = '13800009999',
    updated_at = NOW()
WHERE id = 1;
```

更新前建议先查询确认影响范围：

```sql
-- 更新前先确认目标数据
SELECT
    id,
    username,
    nickname,
    status,
    deleted
FROM sys_user
WHERE status = 0
  AND deleted = 0;
```

`UPDATE` 使用建议：

1. 必须带 `WHERE` 条件，除非明确需要全表更新。
2. 条件字段尽量命中索引。
3. 修改状态时建议带当前状态条件，例如 `WHERE status = 0`，避免重复更新。
4. 并发场景建议使用版本号或状态机控制。
5. 大批量更新应分批执行，避免长事务和大量锁等待。

### DELETE 删除数据

`DELETE` 用于删除表中符合条件的数据。业务系统中通常更推荐逻辑删除，即使用 `deleted` 字段标记删除，而不是直接物理删除。物理删除适合临时表、日志清理、归档后清理等场景。

物理删除单条数据：

```sql
-- 根据主键物理删除用户
DELETE FROM sys_user
WHERE id = 1;
```

按条件删除数据：

```sql
-- 删除已禁用且已逻辑删除的数据
DELETE FROM sys_user
WHERE status = 0
  AND deleted = 1;
```

逻辑删除示例：

```sql
-- 逻辑删除用户，保留历史数据
UPDATE sys_user
SET
    deleted = 1,
    updated_at = NOW()
WHERE id = 1
  AND deleted = 0;
```

删除前确认影响范围：

```sql
-- 删除前先确认待删除数据数量
SELECT
    COUNT(*) AS delete_count
FROM sys_user
WHERE status = 0
  AND deleted = 1;
```

大批量删除建议分批执行：

```sql
-- 分批删除历史逻辑删除数据，避免单次删除过多造成长事务
DELETE FROM sys_user
WHERE deleted = 1
  AND updated_at < '2025-01-01 00:00:00'
ORDER BY id
LIMIT 1000;
```

`DELETE` 使用建议：

| 建议                       | 说明                       |
| -------------------------- | -------------------------- |
| 删除前先 `SELECT COUNT(*)` | 确认影响范围               |
| 优先逻辑删除               | 保留审计和恢复能力         |
| 大批量删除分批执行         | 降低锁等待和事务日志压力   |
| 条件字段建立索引           | 避免全表扫描删除           |
| 生产环境谨慎物理删除       | 删除前应确认备份和恢复方案 |

### TRUNCATE 清空数据

`TRUNCATE` 用于快速清空整张表的数据，并保留表结构。它通常会重置自增值，适合清空临时表、测试表或初始化环境数据，不适合随意用于生产业务表。

清空用户表：

```sql
-- 清空整张表数据，通常会重置自增值
TRUNCATE TABLE sys_user;
```

清空前备份数据：

```sql
-- 清空前备份数据
CREATE TABLE sys_user_bak_20260509 AS
SELECT *
FROM sys_user;

-- 确认备份行数
SELECT COUNT(*) AS bak_count FROM sys_user_bak_20260509;

-- 清空原表
TRUNCATE TABLE sys_user;
```

`DELETE`、`TRUNCATE`、`DROP` 对比：

| 操作         | 是否保留表结构 | 是否删除数据 | 常见用途           |
| ------------ | -------------- | ------------ | ------------------ |
| `DELETE`     | 是             | 删除指定数据 | 条件删除、业务删除 |
| `TRUNCATE`   | 是             | 清空整表     | 清空临时表、测试表 |
| `DROP TABLE` | 否             | 删除表和数据 | 废弃表结构         |

`TRUNCATE` 注意事项：

1. 会清空整张表，不支持 `WHERE` 条件。
2. 通常会重置自增值。
3. 不适合清理部分数据。
4. 生产环境执行前必须确认当前数据库和目标表。
5. 有外键引用时可能无法直接执行。

### UPSERT 插入或更新

UPSERT 表示“存在则更新，不存在则插入”。MySQL 常用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现 UPSERT，它依赖主键或唯一索引判断是否冲突。

先创建一张账户余额表：

```sql
-- 账户余额表，user_id 唯一
CREATE TABLE IF NOT EXISTS account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    available_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额表';
```

存在则更新，不存在则插入：

```sql
-- user_id 存在时更新余额，不存在时插入新记录
INSERT INTO account_balance (
    user_id,
    available_amount,
    frozen_amount
) VALUES (
    10001,
    200.00,
    0.00
)
ON DUPLICATE KEY UPDATE
    available_amount = available_amount + VALUES(available_amount),
    frozen_amount = frozen_amount + VALUES(frozen_amount),
    version = version + 1,
    updated_at = NOW();
```

按业务编码 UPSERT 示例：

```sql
-- 根据唯一用户名实现插入或更新
INSERT INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
) VALUES (
    'zhangsan',
    '张三',
    '13800000001',
    'zhangsan@example.com',
    1
)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    mobile = VALUES(mobile),
    email = VALUES(email),
    status = VALUES(status),
    updated_at = NOW();
```

UPSERT 使用建议：

1. 必须依赖主键或唯一索引触发冲突判断。
2. 适合幂等写入、配置刷新、统计累加、导入更新。
3. 更新表达式要避免误覆盖重要字段。
4. 不建议对复杂业务流程过度依赖 UPSERT，应结合业务状态判断。
5. 高并发累加场景需要关注锁竞争。

### REPLACE 替换数据

`REPLACE` 是 MySQL 提供的替换写入语法。当主键或唯一键冲突时，MySQL 会先删除旧记录，再插入新记录；如果不冲突，则直接插入。它不是普通意义上的更新，因此要谨慎使用。

`REPLACE` 示例：

```sql
-- 如果 username 唯一键冲突，会删除旧记录再插入新记录
REPLACE INTO sys_user (
    id,
    username,
    nickname,
    mobile,
    email,
    status
) VALUES (
    1,
    'zhangsan',
    '张三替换',
    '13800008888',
    'replace@example.com',
    1
);
```

按唯一字段替换：

```sql
-- username 冲突时触发替换
REPLACE INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
) VALUES (
    'lisi',
    '李四替换',
    '13800007777',
    'lisi_replace@example.com',
    1
);
```

`REPLACE` 风险说明：

| 风险                 | 说明                                     |
| -------------------- | ---------------------------------------- |
| 先删再插             | 可能导致原记录主键、自增值、创建时间变化 |
| 触发删除逻辑         | 可能影响触发器、外键或审计逻辑           |
| 未提供字段会变默认值 | 原有字段可能丢失                         |
| 不适合复杂业务更新   | 容易造成数据覆盖                         |

更推荐使用 UPSERT：

```sql
-- 更推荐：保留原记录，仅更新指定字段
INSERT INTO sys_user (
    username,
    nickname,
    mobile,
    email,
    status
) VALUES (
    'lisi',
    '李四更新',
    '13800007777',
    'lisi_update@example.com',
    1
)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    mobile = VALUES(mobile),
    email = VALUES(email),
    status = VALUES(status),
    updated_at = NOW();
```

项目开发中，除非明确需要“删除旧记录再插入新记录”的语义，否则不建议使用 `REPLACE`。

## 查询条件与结果处理

查询条件用于过滤数据，结果处理用于控制返回顺序、数量、唯一性和展示字段。写查询 SQL 时，应优先考虑业务条件是否准确、索引是否可用、排序是否稳定、分页是否合理，以及返回字段是否必要。

### WHERE 条件查询

`WHERE` 用于过滤符合条件的数据，是查询、更新、删除中最重要的条件部分。条件字段如果能命中索引，查询性能通常会更好。

按状态查询：

```sql
-- 查询启用且未删除的用户
SELECT
    id,
    username,
    nickname,
    mobile,
    status,
    created_at
FROM sys_user
WHERE status = 1
  AND deleted = 0;
```

按主键查询：

```sql
-- 主键查询通常性能最好
SELECT
    id,
    username,
    nickname,
    email
FROM sys_user
WHERE id = 10001;
```

按时间范围查询：

```sql
-- 查询指定时间之后创建的用户
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE created_at >= '2026-01-01 00:00:00';
```

`WHERE` 使用建议：

1. 高频条件字段应考虑建立索引。
2. 不要在索引字段上随意使用函数，否则可能导致索引失效。
3. 字符串条件应使用正确类型，避免隐式类型转换。
4. 逻辑删除表应默认带 `deleted = 0` 条件。
5. 更新和删除必须带明确 `WHERE` 条件。

### AND 与 OR 条件组合

`AND` 表示多个条件同时满足，`OR` 表示满足任一条件。组合使用时要注意优先级，复杂条件建议使用括号明确逻辑。

使用 `AND`：

```sql
-- 查询启用、未删除、手机号不为空的用户
SELECT
    id,
    username,
    nickname,
    mobile
FROM sys_user
WHERE status = 1
  AND deleted = 0
  AND mobile IS NOT NULL;
```

使用 `OR`：

```sql
-- 查询手机号或邮箱匹配的用户
SELECT
    id,
    username,
    nickname,
    mobile,
    email
FROM sys_user
WHERE mobile = '13800000001'
   OR email = 'zhangsan@example.com';
```

使用括号明确组合逻辑：

```sql
-- 查询未删除，并且手机号或邮箱匹配的用户
SELECT
    id,
    username,
    nickname,
    mobile,
    email
FROM sys_user
WHERE deleted = 0
  AND (
      mobile = '13800000001'
      OR email = 'zhangsan@example.com'
  );
```

不推荐逻辑不清晰的写法：

```sql
-- 不推荐：AND 和 OR 混用但没有括号，容易产生理解偏差
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE deleted = 0
  AND mobile = '13800000001'
  OR email = 'zhangsan@example.com';
```

条件组合建议：

1. `AND` 和 `OR` 混用时必须使用括号。
2. `OR` 条件较多时可以考虑 `IN`。
3. 不同字段之间大量 `OR` 可能影响索引使用，需要结合执行计划分析。
4. 对复杂筛选条件，建议先保证语义正确，再优化性能。

### IN 与 NOT IN

`IN` 用于判断字段值是否在指定集合内，常用于按多个 ID、多个状态、多个编码查询。`NOT IN` 用于排除指定集合，但如果集合中包含 `NULL`，结果可能不符合预期，需要谨慎使用。

使用 `IN` 查询多个用户：

```sql
-- 查询指定 ID 集合的用户
SELECT
    id,
    username,
    nickname,
    status
FROM sys_user
WHERE id IN (1, 2, 3, 4, 5);
```

使用 `IN` 查询多个状态：

```sql
-- 查询启用或禁用状态的用户
SELECT
    id,
    username,
    nickname,
    status
FROM sys_user
WHERE status IN (0, 1)
  AND deleted = 0;
```

使用 `NOT IN` 排除数据：

```sql
-- 查询不在指定用户名集合中的用户
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username NOT IN ('admin', 'root')
  AND deleted = 0;
```

子查询中使用 `IN`：

```sql
-- 查询拥有角色的用户
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE id IN (
    SELECT user_id
    FROM sys_user_role
    WHERE role_id = 1
);
```

`IN` 使用建议：

1. `IN` 集合不宜过大，过大时建议使用临时表或关联查询。
2. `IN` 字段应尽量有索引。
3. `NOT IN` 要注意 `NULL`，子查询结果中如果包含 `NULL`，可能导致无结果。
4. 排除逻辑复杂时，可考虑使用 `NOT EXISTS`。

避免 `NOT IN` 子查询包含 `NULL`：

```sql
-- 子查询中排除 NULL，降低 NOT IN 异常结果风险
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE id NOT IN (
    SELECT user_id
    FROM sys_user_blacklist
    WHERE user_id IS NOT NULL
);
```

### BETWEEN 范围查询

`BETWEEN` 用于范围查询，包含左右边界。它常用于数字范围、日期范围、金额范围等条件。

按 ID 范围查询：

```sql
-- 查询 ID 在 100 到 200 之间的用户，包含 100 和 200
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE id BETWEEN 100 AND 200;
```

按创建时间范围查询：

```sql
-- 查询指定日期范围内创建的用户
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE created_at BETWEEN '2026-01-01 00:00:00'
                     AND '2026-01-31 23:59:59';
```

更推荐的时间范围写法：

```sql
-- 推荐：左闭右开，避免毫秒、微秒精度造成边界遗漏
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

金额范围查询示例：

```sql
-- 查询支付金额在指定范围内的订单
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_payment
WHERE pay_amount BETWEEN 100.00 AND 500.00;
```

`BETWEEN` 使用建议：

1. `BETWEEN` 包含边界值。
2. 日期时间范围更推荐使用左闭右开写法。
3. 范围查询字段如果参与高频查询，应建立索引。
4. 联合索引中，范围条件后面的字段通常难以继续充分利用索引，需要关注字段顺序。

### LIKE 模糊查询

`LIKE` 用于字符串模糊匹配，常与 `%` 和 `_` 通配符配合使用。`%` 表示任意长度字符，`_` 表示单个字符。

前缀匹配：

```sql
-- 查询用户名以 user 开头的用户
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username LIKE 'user%';
```

包含匹配：

```sql
-- 查询昵称中包含“三”的用户
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE nickname LIKE '%三%';
```

单字符匹配：

```sql
-- 查询 username 类似 user001、user002 的用户
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username LIKE 'user00_';
```

转义特殊字符：

```sql
-- 查询包含下划线的用户名，使用 ESCAPE 指定转义字符
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username LIKE '%\_%' ESCAPE '\';
```

`LIKE` 使用建议：

| 写法           | 索引利用情况         | 说明           |
| -------------- | -------------------- | -------------- |
| `LIKE 'abc%'`  | 通常可以利用索引     | 前缀匹配较友好 |
| `LIKE '%abc'`  | 通常难以利用普通索引 | 后缀匹配       |
| `LIKE '%abc%'` | 通常难以利用普通索引 | 包含匹配       |
| `LIKE 'a_c%'`  | 视情况利用索引       | `_` 匹配单字符 |

如果需要大量全文搜索、中文分词、高亮、相关度排序，不建议依赖普通 `LIKE '%关键词%'`，应考虑 MySQL 全文索引、Elasticsearch、OpenSearch 或其他搜索引擎方案。

### IS NULL 空值判断

`IS NULL` 用于判断字段是否为 `NULL`，`IS NOT NULL` 用于判断字段不为空。SQL 中不能使用 `= NULL` 或 `!= NULL` 判断空值。

查询手机号为空的用户：

```sql
-- 查询未填写手机号的用户
SELECT
    id,
    username,
    nickname,
    mobile
FROM sys_user
WHERE mobile IS NULL;
```

查询手机号不为空的用户：

```sql
-- 查询已填写手机号的用户
SELECT
    id,
    username,
    nickname,
    mobile
FROM sys_user
WHERE mobile IS NOT NULL;
```

错误写法：

```sql
-- 错误：不能使用 = NULL 判断空值
SELECT
    id,
    username
FROM sys_user
WHERE mobile = NULL;
```

区分 `NULL` 和空字符串：

```sql
-- 查询手机号为 NULL 或空字符串的数据
SELECT
    id,
    username,
    nickname,
    mobile
FROM sys_user
WHERE mobile IS NULL
   OR mobile = '';
```

空值设计建议：

1. 必填字段应使用 `NOT NULL`。
2. 可选字段可以使用 `NULL` 表示未知或未填写。
3. 字符串字段不要混用 `NULL` 和空字符串表达同一含义。
4. 金额、数量、状态字段通常不建议允许 `NULL`。
5. 查询空值时必须使用 `IS NULL` 或 `IS NOT NULL`。

### ORDER BY 排序

`ORDER BY` 用于对查询结果排序。排序可以按单字段或多字段，支持升序 `ASC` 和降序 `DESC`。分页查询必须配合稳定排序，否则翻页结果可能重复或遗漏。

按创建时间倒序：

```sql
-- 查询最新创建的用户
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC;
```

按状态和时间排序：

```sql
-- 先按状态升序，再按创建时间倒序
SELECT
    id,
    username,
    nickname,
    status,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY status ASC, created_at DESC;
```

分页时增加主键排序保证稳定：

```sql
-- 分页查询时，增加 id 排序保证顺序稳定
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 10 OFFSET 0;
```

按表达式排序：

```sql
-- 将启用用户排在前面，再按创建时间倒序
SELECT
    id,
    username,
    nickname,
    status,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY
    CASE WHEN status = 1 THEN 0 ELSE 1 END,
    created_at DESC;
```

`ORDER BY` 使用建议：

1. 排序字段和过滤字段应结合索引设计。
2. 分页排序必须稳定，建议追加主键排序。
3. 大结果集排序可能产生 `Using filesort`，需要结合执行计划优化。
4. 不建议对大字段、函数表达式进行高频排序。
5. 业务列表通常使用 `created_at DESC, id DESC`。

### LIMIT 分页

`LIMIT` 用于限制返回行数，常用于分页查询。MySQL 支持 `LIMIT offset, size` 或 `LIMIT size OFFSET offset` 两种写法。

查询第一页：

```sql
-- 查询第 1 页，每页 10 条
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 0, 10;
```

使用 `OFFSET` 写法：

```sql
-- 查询第 2 页，每页 10 条
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 10 OFFSET 10;
```

分页参数计算：

```text
offset = (pageNum - 1) * pageSize
size = pageSize
```

深分页问题示例：

```sql
-- 深分页性能较差，数据库需要跳过大量数据
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 100000, 10;
```

基于游标的分页优化：

```sql
-- 第一页
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY id DESC
LIMIT 10;

-- 下一页：使用上一页最后一条记录的 id 作为游标
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user
WHERE deleted = 0
  AND id < 100000
ORDER BY id DESC
LIMIT 10;
```

延迟关联优化深分页：

```sql
-- 先通过索引查出主键，再回表查询完整字段
SELECT
    u.id,
    u.username,
    u.nickname,
    u.created_at
FROM sys_user u
JOIN (
    SELECT id
    FROM sys_user
    WHERE deleted = 0
    ORDER BY id DESC
    LIMIT 100000, 10
) t ON u.id = t.id
ORDER BY u.id DESC;
```

分页查询建议：

1. 必须配合 `ORDER BY`，避免结果顺序不稳定。
2. 页码越深，传统 `LIMIT offset, size` 性能越差。
3. 移动端、滚动加载、后台列表可优先使用游标分页。
4. 查询字段尽量少，必要时采用延迟关联。
5. 最大页码和最大每页数量应在应用层限制。

### DISTINCT 去重

`DISTINCT` 用于去除重复结果。它作用于查询结果中的所有选择字段，而不是只作用于某一个字段。字段越多，去重粒度越细。

查询所有不重复状态：

```sql
-- 查询用户表中出现过的状态
SELECT DISTINCT
    status
FROM sys_user;
```

查询不重复手机号：

```sql
-- 查询不重复的手机号
SELECT DISTINCT
    mobile
FROM sys_user
WHERE mobile IS NOT NULL;
```

多字段去重：

```sql
-- 按 status 和 deleted 的组合去重
SELECT DISTINCT
    status,
    deleted
FROM sys_user;
```

错误理解示例：

```sql
-- DISTINCT 作用于 username 和 mobile 的组合，不是只对 username 去重
SELECT DISTINCT
    username,
    mobile
FROM sys_user;
```

如果要按某个字段分组并统计，可以使用 `GROUP BY`：

```sql
-- 按状态统计用户数量
SELECT
    status,
    COUNT(*) AS user_count
FROM sys_user
WHERE deleted = 0
GROUP BY status;
```

`DISTINCT` 使用建议：

1. `DISTINCT` 会增加去重成本，大数据量查询需谨慎。
2. 多字段 `DISTINCT` 是按字段组合去重。
3. 如果需要分组统计，优先使用 `GROUP BY`。
4. 如果重复来自错误的关联查询，应优先修正 JOIN 条件，而不是盲目加 `DISTINCT`。
5. 高频去重查询应结合索引和执行计划分析。

### 字段别名

字段别名用于调整查询结果中的字段名称，提高 SQL 可读性，也便于接口返回、报表展示和聚合字段命名。字段别名使用 `AS` 定义，`AS` 可以省略，但建议保留以增强可读性。

普通字段别名：

```sql
-- 使用字段别名展示结果
SELECT
    id AS user_id,
    username AS user_name,
    nickname AS nick_name,
    created_at AS register_time
FROM sys_user
WHERE deleted = 0;
```

聚合字段别名：

```sql
-- 统计用户数量并设置别名
SELECT
    status,
    COUNT(*) AS user_count
FROM sys_user
WHERE deleted = 0
GROUP BY status;
```

表达式别名：

```sql
-- 使用表达式生成状态名称
SELECT
    id,
    username,
    status,
    CASE status
        WHEN 0 THEN '禁用'
        WHEN 1 THEN '启用'
        ELSE '未知'
    END AS status_name
FROM sys_user
WHERE deleted = 0;
```

表别名：

```sql
-- 使用表别名简化 SQL
SELECT
    u.id,
    u.username,
    u.nickname,
    u.created_at
FROM sys_user AS u
WHERE u.deleted = 0
ORDER BY u.created_at DESC;
```

多表查询中的别名示例：

```sql
-- 用户和角色关系查询中使用表别名
SELECT
    u.id AS user_id,
    u.username AS user_name,
    r.id AS role_id,
    r.role_name AS role_name
FROM sys_user AS u
JOIN sys_user_role AS ur ON u.id = ur.user_id
JOIN sys_role AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND r.status = 1;
```

字段别名使用建议：

1. 聚合字段必须设置清晰别名，例如 `COUNT(*) AS total`。
2. 多表查询中应使用表别名，提高 SQL 可读性。
3. 别名不要使用关键字。
4. 接口查询可以将数据库字段别名转换为业务语义字段。
5. 复杂表达式必须设置别名，便于应用层读取结果。


## 聚合与分组查询

聚合查询用于对多行数据进行统计计算，例如统计数量、求和、平均值、最大值和最小值。分组查询用于按业务维度汇总数据，例如按状态统计用户数、按日期统计订单数、按用户统计消费金额。项目中常见的报表、看板、运营统计和后台列表汇总都依赖聚合与分组查询。

本节示例默认使用以下订单表：

```sql
-- 订单表示例，后续聚合与分组查询基于该表演示
CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_channel TINYINT DEFAULT NULL COMMENT '支付渠道：1支付宝，2微信，3银行卡',
    province_code VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
    city_code VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status_created_at (order_status, created_at),
    KEY idx_paid_at (paid_at),
    KEY idx_region_created_at (province_code, city_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

### COUNT 统计

`COUNT` 用于统计行数，是最常见的聚合函数。它可以统计总记录数、符合条件的记录数，也可以配合 `GROUP BY` 按维度统计数量。

统计订单总数：

```sql
-- 统计订单总记录数
SELECT
    COUNT(*) AS total_count
FROM order_info;
```

统计已支付订单数：

```sql
-- 统计已支付订单数量
SELECT
    COUNT(*) AS paid_order_count
FROM order_info
WHERE order_status = 1;
```

统计某个用户的订单数：

```sql
-- 统计指定用户的订单数量
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_info
WHERE user_id = 10001
GROUP BY user_id;
```

`COUNT(*)`、`COUNT(1)` 和 `COUNT(column)` 的区别：

| 写法            | 说明                     | 是否忽略 NULL |
| --------------- | ------------------------ | ------------- |
| `COUNT(*)`      | 统计结果集行数           | 否            |
| `COUNT(1)`      | 统计常量表达式行数       | 否            |
| `COUNT(column)` | 统计指定字段非 NULL 行数 | 是            |

示例：

```sql
-- 统计总订单数与已支付时间不为空的订单数
SELECT
    COUNT(*) AS total_count,
    COUNT(paid_at) AS paid_time_count
FROM order_info;
```

`COUNT` 使用建议：

1. 统计行数优先使用 `COUNT(*)`，语义最清晰。
2. 统计非空字段数量时使用 `COUNT(column)`。
3. 大表实时 `COUNT(*)` 可能成本较高，后台看板可考虑汇总表或异步统计。
4. 带 `WHERE` 的统计应尽量让条件字段命中索引。
5. 分页总数统计和列表查询应分别优化，避免一个复杂 SQL 同时承担过多职责。

### SUM 求和

`SUM` 用于对数值字段求和，常用于统计金额、库存、数量、积分、访问量等。求和字段应是数值类型，金额字段推荐使用 `DECIMAL`，避免浮点误差。

统计已支付订单总金额：

```sql
-- 统计已支付订单的实付金额总和
SELECT
    SUM(pay_amount) AS total_pay_amount
FROM order_info
WHERE order_status = 1;
```

统计某个用户的消费金额：

```sql
-- 统计指定用户的累计支付金额
SELECT
    user_id,
    SUM(pay_amount) AS user_pay_amount
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
GROUP BY user_id;
```

按支付渠道统计金额：

```sql
-- 按支付渠道统计支付金额
SELECT
    pay_channel,
    SUM(pay_amount) AS channel_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY pay_channel;
```

处理空结果：

```sql
-- 使用 COALESCE 避免没有匹配数据时返回 NULL
SELECT
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

`SUM` 使用建议：

1. 金额求和字段应使用 `DECIMAL`。
2. 没有匹配行时，`SUM` 可能返回 `NULL`，可使用 `COALESCE` 转为 `0`。
3. 求和字段不要使用字符串类型，避免隐式类型转换。
4. 大表高频金额统计建议使用汇总表、日统计表或异步任务。
5. 涉及退款、取消、冲正时，应明确统计口径。

### AVG 平均值

`AVG` 用于计算平均值，常用于平均订单金额、平均评分、平均耗时、平均库存等。它会忽略 `NULL` 值，因此需要明确字段是否允许为空。

统计平均订单金额：

```sql
-- 统计已支付订单的平均实付金额
SELECT
    AVG(pay_amount) AS avg_pay_amount
FROM order_info
WHERE order_status = 1;
```

按支付渠道统计平均金额：

```sql
-- 按支付渠道统计平均支付金额
SELECT
    pay_channel,
    AVG(pay_amount) AS avg_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY pay_channel;
```

格式化平均值小数位：

```sql
-- 将平均金额保留两位小数
SELECT
    pay_channel,
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY pay_channel;
```

同时统计订单数和平均金额：

```sql
-- 统计每个用户的订单数和平均支付金额
SELECT
    user_id,
    COUNT(*) AS order_count,
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY user_id;
```

`AVG` 使用建议：

1. `AVG` 会忽略 `NULL` 值，统计前应确认字段含义。
2. 金额平均值通常需要 `ROUND` 控制小数位。
3. 平均值容易被极端值影响，报表中可结合最大值、最小值和中位数分析。
4. 没有匹配数据时，`AVG` 返回 `NULL`。
5. 平均金额统计应明确是否包含退款、取消、优惠前金额或实付金额。

### MAX 最大值

`MAX` 用于获取字段最大值，常用于最大金额、最新时间、最大排序号、最大版本号等场景。

查询最大支付金额：

```sql
-- 查询已支付订单中的最大支付金额
SELECT
    MAX(pay_amount) AS max_pay_amount
FROM order_info
WHERE order_status = 1;
```

查询最新订单时间：

```sql
-- 查询最新创建订单时间
SELECT
    MAX(created_at) AS latest_order_time
FROM order_info;
```

按用户查询最近下单时间：

```sql
-- 查询每个用户最近一次下单时间
SELECT
    user_id,
    MAX(created_at) AS latest_order_time
FROM order_info
GROUP BY user_id;
```

查询最大金额对应的订单：

```sql
-- 查询支付金额最高的订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info
WHERE order_status = 1
ORDER BY pay_amount DESC, id DESC
LIMIT 1;
```

`MAX` 使用建议：

1. 获取最大值本身使用 `MAX(column)`。
2. 获取最大值对应的整行数据时，通常使用 `ORDER BY column DESC LIMIT 1`。
3. 最大时间字段常用于增量同步、最近更新时间、最后登录时间。
4. 如果最大值查询很频繁，应考虑给目标字段建立索引。
5. 分组最大值对应整行数据时，不能简单 `SELECT 非分组字段, MAX(...)`，应使用窗口函数或关联查询。

### MIN 最小值

`MIN` 用于获取字段最小值，常用于最小金额、最早时间、最小排序号等场景。

查询最小支付金额：

```sql
-- 查询已支付订单中的最小支付金额
SELECT
    MIN(pay_amount) AS min_pay_amount
FROM order_info
WHERE order_status = 1;
```

查询最早下单时间：

```sql
-- 查询最早创建订单时间
SELECT
    MIN(created_at) AS first_order_time
FROM order_info;
```

按用户查询首次下单时间：

```sql
-- 查询每个用户首次下单时间
SELECT
    user_id,
    MIN(created_at) AS first_order_time
FROM order_info
GROUP BY user_id;
```

查询最小金额对应的订单：

```sql
-- 查询支付金额最低的订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info
WHERE order_status = 1
ORDER BY pay_amount ASC, id ASC
LIMIT 1;
```

`MIN` 使用建议：

1. 获取最小值使用 `MIN(column)`。
2. 获取最小值对应的整行数据时，使用排序加 `LIMIT` 更直观。
3. 时间字段的最小值常用于首次行为分析。
4. 如果查询条件包含状态和时间，可考虑联合索引。
5. 统计最小值前应确认是否需要排除 0、空值或异常数据。

### GROUP BY 分组

`GROUP BY` 用于按一个或多个字段对数据分组，并对每组数据执行聚合计算。它常用于统计每个用户、每个状态、每天、每个地区、每个渠道的数据。

按订单状态统计数量：

```sql
-- 按订单状态统计订单数量
SELECT
    order_status,
    COUNT(*) AS order_count
FROM order_info
GROUP BY order_status;
```

按用户统计订单数和金额：

```sql
-- 按用户统计订单数量和支付金额
SELECT
    user_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY user_id;
```

按日期统计订单数据：

```sql
-- 按日期统计订单数量和支付金额
SELECT
    DATE(created_at) AS order_date,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
GROUP BY DATE(created_at)
ORDER BY order_date ASC;
```

按支付渠道统计：

```sql
-- 按支付渠道统计已支付订单
SELECT
    pay_channel,
    COUNT(*) AS order_count,
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY pay_channel;
```

`GROUP BY` 使用建议：

1. `SELECT` 中非聚合字段应出现在 `GROUP BY` 中。
2. 分组字段应尽量是维度字段，例如状态、用户、日期、地区、渠道。
3. 大表按函数结果分组可能无法充分利用索引，例如 `GROUP BY DATE(created_at)`。
4. 高频分组统计建议使用汇总表。
5. 分组后排序要注意是否产生临时表或文件排序。

### HAVING 分组过滤

`HAVING` 用于对分组后的结果进行过滤。`WHERE` 是分组前过滤原始数据，`HAVING` 是分组后过滤聚合结果。

查询订单数大于 10 的用户：

```sql
-- 筛选订单数量大于 10 的用户
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_info
GROUP BY user_id
HAVING COUNT(*) > 10;
```

查询累计支付金额大于 1000 的用户：

```sql
-- 筛选累计支付金额大于 1000 的用户
SELECT
    user_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY user_id
HAVING SUM(pay_amount) > 1000.00;
```

同时使用 `WHERE` 和 `HAVING`：

```sql
-- 先筛选已支付订单，再筛选累计金额大于 1000 的用户
SELECT
    user_id,
    COUNT(*) AS paid_order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
GROUP BY user_id
HAVING SUM(pay_amount) > 1000.00
ORDER BY total_pay_amount DESC;
```

`WHERE` 与 `HAVING` 对比：

| 关键字                   | 执行阶段             | 常见用途             |
| ------------------------ | -------------------- | -------------------- |
| `WHERE`                  | 分组前               | 过滤原始行           |
| `HAVING`                 | 分组后               | 过滤聚合结果         |
| `WHERE order_status = 1` | 分组前过滤已支付订单 | 能减少参与分组的数据 |
| `HAVING COUNT(*) > 10`   | 分组后过滤订单数     | 依赖聚合结果         |

`HAVING` 使用建议：

1. 能放在 `WHERE` 中的条件不要放到 `HAVING`。
2. `HAVING` 适合过滤 `COUNT`、`SUM`、`AVG` 等聚合结果。
3. 大表统计时应先用 `WHERE` 缩小数据范围。
4. `HAVING` 中可以使用聚合函数，也可以使用聚合字段别名，但为了兼容和清晰，建议显式写聚合表达式。
5. 报表查询中常见组合是 `WHERE + GROUP BY + HAVING + ORDER BY`。

### 多字段分组

多字段分组用于按多个维度组合统计，例如按省份和城市统计订单、按日期和渠道统计金额、按用户和状态统计数量。

按省份和城市统计订单：

```sql
-- 按省份和城市统计订单数量
SELECT
    province_code,
    city_code,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY province_code, city_code
ORDER BY province_code ASC, city_code ASC;
```

按日期和支付渠道统计：

```sql
-- 按日期和支付渠道统计支付金额
SELECT
    DATE(paid_at) AS pay_date,
    pay_channel,
    COUNT(*) AS paid_order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info
WHERE order_status = 1
  AND paid_at >= '2026-01-01 00:00:00'
  AND paid_at <  '2026-02-01 00:00:00'
GROUP BY DATE(paid_at), pay_channel
ORDER BY pay_date ASC, pay_channel ASC;
```

按用户和状态统计：

```sql
-- 按用户和订单状态统计订单数量
SELECT
    user_id,
    order_status,
    COUNT(*) AS order_count
FROM order_info
GROUP BY user_id, order_status
ORDER BY user_id ASC, order_status ASC;
```

多字段分组结果的粒度由所有分组字段共同决定。例如 `GROUP BY province_code, city_code` 表示每个省份和城市组合一组，而不是分别按省份和城市单独统计。

多字段分组建议：

1. 分组字段顺序应符合业务阅读习惯。
2. 高频多字段分组可考虑建立匹配的联合索引。
3. 多字段分组字段越多，分组粒度越细，结果行数可能越多。
4. 时间分组建议优先在写入时维护统计日期字段，减少查询时函数计算。
5. 报表类多维统计建议使用专门的汇总表或 OLAP 组件。

### 分组统计优化

分组统计在大数据量场景下容易产生临时表、文件排序和大量扫描。优化思路通常包括缩小数据范围、设计合适索引、避免函数破坏索引、使用汇总表、异步预计算等。

低效示例：

```sql
-- 低效示例：对 created_at 使用 DATE 函数，可能无法充分利用索引
SELECT
    DATE(created_at) AS order_date,
    COUNT(*) AS order_count
FROM order_info
WHERE DATE(created_at) = '2026-01-01'
GROUP BY DATE(created_at);
```

优化为时间范围查询：

```sql
-- 推荐：使用时间范围，便于利用 created_at 索引
SELECT
    COUNT(*) AS order_count
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00';
```

为高频统计增加冗余统计日期字段：

```sql
-- 高频按日统计时，可以维护 order_date 字段
CREATE TABLE IF NOT EXISTS order_info_report_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    order_date DATE NOT NULL COMMENT '订单日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_date_status (order_date, order_status),
    KEY idx_user_id_order_date (user_id, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单报表优化示例表';
```

基于统计日期分组：

```sql
-- 使用冗余日期字段进行分组统计
SELECT
    order_date,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
FROM order_info_report_demo
WHERE order_date >= '2026-01-01'
  AND order_date <  '2026-02-01'
  AND order_status = 1
GROUP BY order_date
ORDER BY order_date ASC;
```

使用汇总表：

```sql
-- 日订单汇总表，用于承载高频报表查询
CREATE TABLE IF NOT EXISTS order_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    summary_date DATE NOT NULL COMMENT '统计日期',
    order_status TINYINT NOT NULL COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    total_pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付总金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_status (summary_date, order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单日汇总表';
```

分组统计优化建议：

1. 先用 `WHERE` 缩小数据范围，再 `GROUP BY`。
2. 避免在索引字段上使用函数参与过滤，例如 `DATE(created_at)`。
3. 高频报表不要每次扫描明细表，应使用汇总表。
4. 分组字段、过滤字段、排序字段应结合联合索引设计。
5. 使用 `EXPLAIN` 检查是否出现 `Using temporary`、`Using filesort`。
6. 明细数据量很大、统计维度复杂时，应考虑 ClickHouse、Doris、StarRocks 等分析型数据库。

## 多表关联查询

多表关联查询用于把多个表中的相关数据组合成一个结果集。典型场景包括用户和角色、订单和订单明细、订单和支付记录、商品和分类等。关联查询的核心是设计正确的关联条件，并确保关联字段具备合适索引。

本节示例默认使用以下表结构：

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS sys_user_join_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关联查询用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role_join_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关联查询角色表';

-- 用户角色关系表
CREATE TABLE IF NOT EXISTS sys_user_role_join_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关联查询用户角色关系表';
```

### INNER JOIN 内连接

`INNER JOIN` 用于查询两张表中满足关联条件的数据。只有两边都匹配的数据才会出现在结果中。它适合查询“存在关联关系”的数据，例如查询已分配角色的用户。

查询用户及其角色：

```sql
-- 查询已分配角色的用户和角色信息
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    r.id AS role_id,
    r.role_code AS role_code,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
INNER JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
INNER JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND u.status = 1
  AND r.status = 1;
```

查询指定角色下的用户：

```sql
-- 查询拥有 admin 角色的用户
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    r.role_name AS role_name
FROM sys_role_join_demo AS r
INNER JOIN sys_user_role_join_demo AS ur ON r.id = ur.role_id
INNER JOIN sys_user_join_demo AS u ON ur.user_id = u.id
WHERE r.role_code = 'admin'
  AND r.status = 1
  AND u.deleted = 0;
```

`INNER JOIN` 使用建议：

1. 只需要匹配双方都存在的数据时使用内连接。
2. 关联字段必须类型一致，例如都使用 `BIGINT`。
3. 关联字段应建立索引。
4. 多表查询必须使用表别名，提高可读性。
5. 多对多关系通常通过中间表连接。

### LEFT JOIN 左连接

`LEFT JOIN` 用于保留左表全部数据，并关联右表中匹配的数据。如果右表没有匹配记录，右表字段返回 `NULL`。它适合查询主表列表，并附带可选的关联信息。

查询所有用户及其角色：

```sql
-- 查询所有未删除用户，即使用户没有角色也返回
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    r.id AS role_id,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
LEFT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
LEFT JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0;
```

查询没有分配角色的用户：

```sql
-- 查询未分配角色的用户
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname
FROM sys_user_join_demo AS u
LEFT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
WHERE u.deleted = 0
  AND ur.user_id IS NULL;
```

注意右表过滤条件的位置：

```sql
-- 推荐：右表状态条件放在 ON 中，保留没有有效角色的用户
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
LEFT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
LEFT JOIN sys_role_join_demo AS r
    ON ur.role_id = r.id
   AND r.status = 1
WHERE u.deleted = 0;
```

如果把右表过滤条件放在 `WHERE` 中，可能把左连接变成类似内连接的效果：

```sql
-- 谨慎：WHERE r.status = 1 会过滤掉没有角色的用户
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
LEFT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
LEFT JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND r.status = 1;
```

`LEFT JOIN` 使用建议：

1. 需要保留左表全部数据时使用左连接。
2. 判断右表不存在时使用 `右表字段 IS NULL`。
3. 右表过滤条件应谨慎放置，避免破坏左连接语义。
4. 列表查询中常用左连接补充字典、角色、配置等信息。
5. 左连接结果可能因一对多关系导致主表数据重复。

### RIGHT JOIN 右连接

`RIGHT JOIN` 用于保留右表全部数据，并关联左表中匹配的数据。如果左表没有匹配记录，左表字段返回 `NULL`。在实际开发中，`RIGHT JOIN` 使用较少，因为通常可以通过调整表顺序改写为 `LEFT JOIN`，可读性更好。

右连接示例：

```sql
-- 查询所有角色及其关联用户，即使角色没有用户也返回
SELECT
    u.id AS user_id,
    u.username AS username,
    r.id AS role_id,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
RIGHT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
RIGHT JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE r.status = 1;
```

推荐改写为 `LEFT JOIN`：

```sql
-- 推荐：使用 LEFT JOIN 表达所有角色及其用户
SELECT
    r.id AS role_id,
    r.role_name AS role_name,
    u.id AS user_id,
    u.username AS username
FROM sys_role_join_demo AS r
LEFT JOIN sys_user_role_join_demo AS ur ON r.id = ur.role_id
LEFT JOIN sys_user_join_demo AS u
    ON ur.user_id = u.id
   AND u.deleted = 0
WHERE r.status = 1;
```

查询没有分配用户的角色：

```sql
-- 查询没有任何用户关联的角色
SELECT
    r.id AS role_id,
    r.role_code AS role_code,
    r.role_name AS role_name
FROM sys_role_join_demo AS r
LEFT JOIN sys_user_role_join_demo AS ur ON r.id = ur.role_id
WHERE r.status = 1
  AND ur.role_id IS NULL;
```

`RIGHT JOIN` 使用建议：

1. 项目中优先使用 `LEFT JOIN`，减少阅读成本。
2. 如果遇到 `RIGHT JOIN`，通常可以通过调换表顺序改写。
3. 团队 SQL 规范中可约定禁止或少用 `RIGHT JOIN`。
4. 复杂多表查询中，统一左连接方向更容易维护。
5. 右连接语义本身没有问题，但实际项目可读性通常不如左连接。

### CROSS JOIN 交叉连接

`CROSS JOIN` 用于生成笛卡尔积，即左表每一行与右表每一行组合。它通常用于生成组合数据、日期维度补齐、测试数据构造等特殊场景。普通业务查询中应谨慎使用，因为结果行数可能急剧膨胀。

简单交叉连接：

```sql
-- 生成用户和角色的所有组合
SELECT
    u.id AS user_id,
    u.username AS username,
    r.id AS role_id,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
CROSS JOIN sys_role_join_demo AS r
WHERE u.deleted = 0
  AND r.status = 1;
```

生成报表维度组合：

```sql
-- 示例：生成支付渠道和订单状态的组合
SELECT
    c.pay_channel,
    s.order_status
FROM (
    SELECT 1 AS pay_channel UNION ALL
    SELECT 2 AS pay_channel UNION ALL
    SELECT 3 AS pay_channel
) AS c
CROSS JOIN (
    SELECT 0 AS order_status UNION ALL
    SELECT 1 AS order_status UNION ALL
    SELECT 2 AS order_status UNION ALL
    SELECT 3 AS order_status
) AS s;
```

用于补齐统计维度：

```sql
-- 生成渠道和状态组合，再左连接实际统计结果
SELECT
    dim.pay_channel,
    dim.order_status,
    COALESCE(stat.order_count, 0) AS order_count
FROM (
    SELECT
        c.pay_channel,
        s.order_status
    FROM (
        SELECT 1 AS pay_channel UNION ALL
        SELECT 2 AS pay_channel UNION ALL
        SELECT 3 AS pay_channel
    ) AS c
    CROSS JOIN (
        SELECT 0 AS order_status UNION ALL
        SELECT 1 AS order_status UNION ALL
        SELECT 2 AS order_status UNION ALL
        SELECT 3 AS order_status
    ) AS s
) AS dim
LEFT JOIN (
    SELECT
        pay_channel,
        order_status,
        COUNT(*) AS order_count
    FROM order_info
    WHERE created_at >= '2026-01-01 00:00:00'
      AND created_at <  '2026-02-01 00:00:00'
    GROUP BY pay_channel, order_status
) AS stat
    ON dim.pay_channel = stat.pay_channel
   AND dim.order_status = stat.order_status
ORDER BY dim.pay_channel, dim.order_status;
```

`CROSS JOIN` 使用建议：

1. 使用前必须估算结果行数。
2. 普通业务关联查询不要使用交叉连接。
3. 适合生成维度组合、测试数据和报表补齐。
4. 如果 `JOIN` 忘记写 `ON` 条件，可能意外产生笛卡尔积。
5. 大表之间禁止直接交叉连接。

### 自关联查询

自关联查询是指同一张表自己和自己关联，常用于树形结构、上下级关系、组织架构、分类层级、评论回复等场景。

部门表示例：

```sql
-- 部门表，自身通过 parent_id 表达上下级关系
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示顶级部门',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_parent_id (parent_id),
    KEY idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

查询部门及父部门名称：

```sql
-- 自关联查询部门和父部门
SELECT
    child.id AS dept_id,
    child.dept_name AS dept_name,
    child.parent_id AS parent_id,
    parent.dept_name AS parent_dept_name
FROM sys_dept AS child
LEFT JOIN sys_dept AS parent ON child.parent_id = parent.id
WHERE child.status = 1
ORDER BY child.parent_id ASC, child.sort_order ASC;
```

查询某个父部门下的直接子部门：

```sql
-- 查询指定部门的直接子部门
SELECT
    id,
    dept_name,
    parent_id,
    sort_order
FROM sys_dept
WHERE parent_id = 100
  AND status = 1
ORDER BY sort_order ASC, id ASC;
```

查询二级部门结构：

```sql
-- 查询一级部门和二级部门
SELECT
    p.id AS parent_dept_id,
    p.dept_name AS parent_dept_name,
    c.id AS child_dept_id,
    c.dept_name AS child_dept_name
FROM sys_dept AS p
LEFT JOIN sys_dept AS c ON p.id = c.parent_id
WHERE p.parent_id = 0
  AND p.status = 1
ORDER BY p.sort_order ASC, c.sort_order ASC;
```

自关联查询建议：

1. 表别名必须清晰，例如 `parent`、`child`。
2. 上下级字段应建立索引，例如 `parent_id`。
3. 层级不固定时，可以使用递归 CTE。
4. 大规模树结构可以考虑路径枚举、闭包表、嵌套集等模型。
5. 自关联查询要避免层级过深导致 SQL 复杂度过高。

### 多表联合查询

多表联合查询是指一个 SQL 同时连接三张或更多表，常见于用户权限、订单详情、商品分类、支付记录、物流信息等场景。此类查询应特别注意关联条件、结果重复、索引设计和返回字段控制。

订单相关表示例：

```sql
-- 订单明细表示例
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    buy_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买数量',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售单价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 支付记录表示例
CREATE TABLE IF NOT EXISTS payment_record_join_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败，3已退款',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_id (order_id),
    KEY idx_pay_status_paid_at (pay_status, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录关联示例表';
```

查询订单、明细和支付记录：

```sql
-- 多表查询订单、订单明细和支付信息
SELECT
    o.id AS order_id,
    o.order_no AS order_no,
    o.user_id AS user_id,
    o.order_status AS order_status,
    i.product_id AS product_id,
    i.product_name AS product_name,
    i.buy_count AS buy_count,
    i.sale_price AS sale_price,
    p.payment_no AS payment_no,
    p.pay_status AS pay_status,
    p.paid_at AS paid_at
FROM order_info AS o
INNER JOIN order_item AS i ON o.id = i.order_id
LEFT JOIN payment_record_join_demo AS p ON o.id = p.order_id
WHERE o.user_id = 10001
  AND o.created_at >= '2026-01-01 00:00:00'
  AND o.created_at <  '2026-02-01 00:00:00'
ORDER BY o.created_at DESC, o.id DESC;
```

查询用户角色权限：

```sql
-- 查询用户拥有的角色
SELECT
    u.id AS user_id,
    u.username AS username,
    r.id AS role_id,
    r.role_code AS role_code,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
INNER JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
INNER JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.username = 'zhangsan'
  AND u.deleted = 0
  AND u.status = 1
  AND r.status = 1;
```

多表联合查询建议：

1. 每张表都使用清晰别名。
2. `SELECT` 中明确字段，不使用 `SELECT *`。
3. 每个 `JOIN` 都必须有明确 `ON` 条件。
4. 一对多连接会放大结果行数，需要确认是否符合业务预期。
5. 多表查询尽量先从过滤性强的表开始理解，但最终执行顺序由优化器决定。
6. 复杂报表查询可以拆分为临时表、CTE 或汇总表。

### JOIN 条件设计

`JOIN` 条件决定表之间如何匹配，是关联查询正确性的核心。错误的关联条件会导致数据丢失、重复、笛卡尔积或统计结果错误。

正确的主外键关联：

```sql
-- 用户角色关系表通过 user_id 和 role_id 分别关联用户表、角色表
SELECT
    u.id AS user_id,
    u.username AS username,
    r.id AS role_id,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0;
```

错误示例：缺少关联条件：

```sql
-- 错误示例：缺少 ON 条件会产生笛卡尔积
SELECT
    u.username,
    r.role_name
FROM sys_user_join_demo AS u
JOIN sys_role_join_demo AS r;
```

错误示例：关联字段不正确：

```sql
-- 错误示例：username 和 role_code 没有业务关联关系
SELECT
    u.username,
    r.role_name
FROM sys_user_join_demo AS u
JOIN sys_role_join_demo AS r ON u.username = r.role_code;
```

在 `ON` 中放关联条件，在 `WHERE` 中放主过滤条件：

```sql
-- 推荐：ON 表达表关系，WHERE 表达业务过滤
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND u.status = 1
  AND r.status = 1;
```

左连接中右表条件放置示例：

```sql
-- 保留所有用户，只关联启用角色
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
LEFT JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
LEFT JOIN sys_role_join_demo AS r
    ON ur.role_id = r.id
   AND r.status = 1
WHERE u.deleted = 0;
```

`JOIN` 条件设计建议：

1. 关联字段应有明确业务关系。
2. 关联字段类型必须一致，例如 `BIGINT` 对 `BIGINT`。
3. 关联字段字符集和排序规则也应一致，特别是字符串关联。
4. 中间表应建立联合唯一索引，例如 `(user_id, role_id)`。
5. 左连接右表过滤条件要谨慎放在 `ON` 或 `WHERE` 中。
6. 不要为了“查出来”随意增加不可靠关联条件。

### 关联查询优化

关联查询优化的核心是减少参与连接的数据量、保证关联字段有索引、控制返回字段、避免不必要的一对多放大，并通过执行计划验证优化效果。

为关联字段建立索引：

```sql
-- 用户角色关系表常用索引
ALTER TABLE sys_user_role_join_demo
  ADD UNIQUE KEY uk_user_role (user_id, role_id),
  ADD KEY idx_role_id (role_id);
```

先过滤再关联：

```sql
-- 先在 WHERE 中缩小用户范围，再关联角色
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND u.status = 1
  AND u.created_at >= '2026-01-01 00:00:00';
```

避免返回不必要字段：

```sql
-- 推荐：只返回业务需要的字段
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0;
```

不推荐：

```sql
-- 不推荐：返回所有字段，增加网络和内存成本
SELECT *
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id;
```

使用 `EXPLAIN` 分析关联查询：

```sql
-- 查看关联查询执行计划
EXPLAIN
SELECT
    u.id AS user_id,
    u.username AS username,
    r.role_name AS role_name
FROM sys_user_join_demo AS u
JOIN sys_user_role_join_demo AS ur ON u.id = ur.user_id
JOIN sys_role_join_demo AS r ON ur.role_id = r.id
WHERE u.deleted = 0
  AND u.status = 1;
```

常见优化方向：

| 问题                 | 优化方向                               |
| -------------------- | -------------------------------------- |
| 关联字段无索引       | 为 `ON` 条件字段建立索引               |
| 返回字段过多         | 明确字段，避免 `SELECT *`              |
| 结果重复过多         | 确认一对多关系，必要时先聚合再关联     |
| 过滤条件太少         | 增加业务过滤条件，减少参与关联的数据量 |
| 深分页关联慢         | 先分页主键，再关联查询详情             |
| 出现临时表和文件排序 | 优化索引、排序字段和分组方式           |

先分页主表再关联详情：

```sql
-- 先分页查询订单主键，再关联明细，适合大表分页详情
SELECT
    o.id AS order_id,
    o.order_no AS order_no,
    o.user_id AS user_id,
    i.product_name AS product_name,
    i.buy_count AS buy_count
FROM (
    SELECT
        id,
        order_no,
        user_id
    FROM order_info
    WHERE user_id = 10001
    ORDER BY created_at DESC, id DESC
    LIMIT 0, 10
) AS o
LEFT JOIN order_item AS i ON o.id = i.order_id
ORDER BY o.id DESC;
```

关联查询优化建议：

1. `ON` 条件字段必须重点检查索引。
2. 大表关联前尽量先缩小数据范围。
3. 关联查询不要返回无用字段。
4. 一对多关联用于分页时要特别谨慎，可能导致分页条数异常。
5. 复杂统计查询可以先聚合再关联，避免明细数据反复放大。
6. 使用 `EXPLAIN` 观察 `type`、`key`、`rows`、`Extra` 等字段，确认优化是否生效。


## 子查询与派生表

子查询是嵌套在其他 SQL 中的查询语句，常用于条件过滤、临时结果集、聚合后再筛选、判断数据是否存在等场景。派生表是出现在 `FROM` 后面的子查询结果，可以像普通表一样参与关联、过滤和排序。

子查询可以提升 SQL 表达能力，但也容易带来性能问题。实际开发中应结合数据量、索引、执行计划和可读性选择子查询、JOIN、CTE 或临时表。

本节示例默认使用以下表：

```sql
-- 用户表示例
CREATE TABLE IF NOT EXISTS sys_user_sub_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子查询用户表';

-- 订单表示例
CREATE TABLE IF NOT EXISTS order_info_sub_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子查询订单表';

-- 用户角色关系表示例
CREATE TABLE IF NOT EXISTS sys_user_role_sub_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子查询用户角色关系表';
```

### WHERE 子查询

`WHERE` 子查询用于在条件中嵌套查询结果，常见写法包括 `IN`、`NOT IN`、比较运算符、`EXISTS` 等。它适合根据另一张表的查询结果过滤当前表。

查询有订单的用户：

```sql
-- 查询至少有一笔订单的用户
SELECT
    id,
    username,
    nickname,
    status
FROM sys_user_sub_demo
WHERE id IN (
    SELECT user_id
    FROM order_info_sub_demo
);
```

查询有已支付订单的用户：

```sql
-- 查询存在已支付订单的用户
SELECT
    id,
    username,
    nickname
FROM sys_user_sub_demo
WHERE deleted = 0
  AND id IN (
      SELECT user_id
      FROM order_info_sub_demo
      WHERE order_status = 1
  );
```

查询订单金额大于平均订单金额的订单：

```sql
-- 查询支付金额大于全表平均支付金额的订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info_sub_demo
WHERE pay_amount > (
    SELECT AVG(pay_amount)
    FROM order_info_sub_demo
    WHERE order_status = 1
);
```

查询没有角色的用户：

```sql
-- 使用 NOT IN 查询没有角色的用户，子查询中需要排除 NULL
SELECT
    id,
    username,
    nickname
FROM sys_user_sub_demo
WHERE deleted = 0
  AND id NOT IN (
      SELECT user_id
      FROM sys_user_role_sub_demo
      WHERE user_id IS NOT NULL
  );
```

`WHERE` 子查询使用建议：

| 场景                   | 推荐写法     | 说明                             |
| ---------------------- | ------------ | -------------------------------- |
| 判断是否在集合中       | `IN`         | 集合较小时较直观                 |
| 判断是否存在关联数据   | `EXISTS`     | 大表关联时通常更稳妥             |
| 判断是否不存在关联数据 | `NOT EXISTS` | 比 `NOT IN` 更能规避 `NULL` 问题 |
| 与聚合值比较           | 标量子查询   | 子查询必须返回单值               |
| 复杂过滤               | JOIN 或 CTE  | 可读性和优化空间更好             |

### FROM 子查询

`FROM` 子查询会先生成一个临时结果集，然后外层查询再基于该结果集继续查询、过滤、排序或关联。这个临时结果集也称为派生表。

查询每个用户的订单统计，再筛选订单数大于 3 的用户：

```sql
-- FROM 子查询先统计每个用户订单，再由外层查询过滤
SELECT
    t.user_id,
    t.order_count,
    t.total_pay_amount
FROM (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
    FROM order_info_sub_demo
    WHERE order_status = 1
    GROUP BY user_id
) AS t
WHERE t.order_count > 3
ORDER BY t.total_pay_amount DESC;
```

派生表参与关联查询：

```sql
-- 查询用户信息和用户支付统计
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    stat.order_count AS order_count,
    stat.total_pay_amount AS total_pay_amount
FROM sys_user_sub_demo AS u
JOIN (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount
    FROM order_info_sub_demo
    WHERE order_status = 1
    GROUP BY user_id
) AS stat ON u.id = stat.user_id
WHERE u.deleted = 0
ORDER BY stat.total_pay_amount DESC;
```

按用户最近订单时间排序：

```sql
-- 先计算每个用户最近下单时间，再关联用户表
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    latest.latest_order_time AS latest_order_time
FROM sys_user_sub_demo AS u
JOIN (
    SELECT
        user_id,
        MAX(created_at) AS latest_order_time
    FROM order_info_sub_demo
    GROUP BY user_id
) AS latest ON u.id = latest.user_id
WHERE u.deleted = 0
ORDER BY latest.latest_order_time DESC;
```

`FROM` 子查询使用建议：

1. 派生表必须设置别名，例如 `AS t`、`AS stat`。
2. 派生表适合承载“先聚合，再关联”或“先过滤，再分页”的逻辑。
3. 派生表字段也应设置清晰别名，便于外层查询引用。
4. 派生表结果过大时可能产生临时表，需要结合 `EXPLAIN` 分析。
5. MySQL 8 中可用 CTE 改写复杂派生表，提高可读性。

### SELECT 子查询

`SELECT` 子查询出现在查询字段列表中，通常用于为每行结果计算一个关联值。它适合查询简单的附加统计字段，但如果外层结果行数较多，可能造成子查询重复执行或执行成本过高。

查询用户及其订单数量：

```sql
-- 在 SELECT 字段中使用子查询统计每个用户订单数
SELECT
    u.id,
    u.username,
    u.nickname,
    (
        SELECT COUNT(*)
        FROM order_info_sub_demo AS o
        WHERE o.user_id = u.id
    ) AS order_count
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0;
```

查询用户最近下单时间：

```sql
-- 在 SELECT 字段中查询用户最近下单时间
SELECT
    u.id,
    u.username,
    u.nickname,
    (
        SELECT MAX(o.created_at)
        FROM order_info_sub_demo AS o
        WHERE o.user_id = u.id
    ) AS latest_order_time
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0;
```

查询用户累计支付金额：

```sql
-- 查询用户基础信息，并附带累计支付金额
SELECT
    u.id,
    u.username,
    u.nickname,
    (
        SELECT COALESCE(SUM(o.pay_amount), 0.00)
        FROM order_info_sub_demo AS o
        WHERE o.user_id = u.id
          AND o.order_status = 1
    ) AS total_pay_amount
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0;
```

对于大数据量列表，更推荐先聚合再关联：

```sql
-- 推荐：先聚合订单，再关联用户，减少 SELECT 子查询重复计算
SELECT
    u.id,
    u.username,
    u.nickname,
    COALESCE(stat.total_pay_amount, 0.00) AS total_pay_amount
FROM sys_user_sub_demo AS u
LEFT JOIN (
    SELECT
        user_id,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_sub_demo
    WHERE order_status = 1
    GROUP BY user_id
) AS stat ON u.id = stat.user_id
WHERE u.deleted = 0;
```

`SELECT` 子查询使用建议：

1. 适合小结果集、简单附加字段查询。
2. 子查询应尽量通过索引快速定位，例如 `o.user_id = u.id`。
3. 外层数据量大时，优先考虑派生表、JOIN 或 CTE。
4. 子查询返回值必须是单行单列，否则会报错。
5. 不建议在后台分页列表中无节制使用多个 `SELECT` 子查询。

### EXISTS 子查询

`EXISTS` 用于判断子查询是否至少返回一行数据。它更关注“是否存在”，不关心子查询返回的具体字段。常用于判断是否存在关联记录。

查询有订单的用户：

```sql
-- 使用 EXISTS 查询至少有一笔订单的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND EXISTS (
      SELECT 1
      FROM order_info_sub_demo AS o
      WHERE o.user_id = u.id
  );
```

查询有已支付订单的用户：

```sql
-- 使用 EXISTS 查询存在已支付订单的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND EXISTS (
      SELECT 1
      FROM order_info_sub_demo AS o
      WHERE o.user_id = u.id
        AND o.order_status = 1
  );
```

查询拥有指定角色的用户：

```sql
-- 查询拥有指定角色ID的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND EXISTS (
      SELECT 1
      FROM sys_user_role_sub_demo AS ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 1
  );
```

`EXISTS` 使用建议：

1. 只判断存在性时，优先考虑 `EXISTS`。
2. `EXISTS` 子查询中通常写 `SELECT 1`，表达不关心返回字段。
3. 子查询关联字段必须有索引，例如 `order_info_sub_demo(user_id)`。
4. 大表存在性判断通常比 `IN` 更可控。
5. 是否一定比 `IN` 快取决于执行计划，最终应以 `EXPLAIN` 为准。

### NOT EXISTS 子查询

`NOT EXISTS` 用于判断子查询没有返回任何记录。它常用于查询没有关联数据的主表记录，例如没有订单的用户、没有角色的用户、没有明细的订单。

查询没有订单的用户：

```sql
-- 查询没有任何订单的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM order_info_sub_demo AS o
      WHERE o.user_id = u.id
  );
```

查询没有已支付订单的用户：

```sql
-- 查询没有已支付订单的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM order_info_sub_demo AS o
      WHERE o.user_id = u.id
        AND o.order_status = 1
  );
```

查询没有角色的用户：

```sql
-- 使用 NOT EXISTS 查询未分配角色的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role_sub_demo AS ur
      WHERE ur.user_id = u.id
  );
```

`NOT EXISTS` 与 `LEFT JOIN IS NULL` 等价写法：

```sql
-- 使用 LEFT JOIN 查询没有角色的用户
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
LEFT JOIN sys_user_role_sub_demo AS ur ON u.id = ur.user_id
WHERE u.deleted = 0
  AND ur.user_id IS NULL;
```

`NOT EXISTS` 使用建议：

1. 反向存在性判断优先考虑 `NOT EXISTS`。
2. 相比 `NOT IN`，`NOT EXISTS` 不容易受 `NULL` 值影响。
3. 子查询中的关联字段应建立索引。
4. 可与 `LEFT JOIN ... IS NULL` 互相改写，最终以可读性和执行计划为准。
5. 用于数据质量检查、缺失关联检查、权限缺口检查时非常常见。

### 相关子查询

相关子查询是指子查询依赖外层查询字段，每处理外层一行数据，子查询都需要基于外层字段进行判断。`EXISTS`、`SELECT` 字段子查询中经常出现相关子查询。

查询每个用户是否有订单：

```sql
-- 子查询依赖外层 u.id，因此是相关子查询
SELECT
    u.id,
    u.username,
    u.nickname,
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM order_info_sub_demo AS o
            WHERE o.user_id = u.id
        ) THEN '有订单'
        ELSE '无订单'
    END AS order_flag
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0;
```

查询每个用户最近一笔订单：

```sql
-- 查询每个用户最近一笔订单
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.pay_amount,
    o.created_at
FROM order_info_sub_demo AS o
WHERE o.created_at = (
    SELECT MAX(o2.created_at)
    FROM order_info_sub_demo AS o2
    WHERE o2.user_id = o.user_id
);
```

如果同一用户存在多笔相同创建时间的订单，上面 SQL 可能返回多行。可以用窗口函数改写：

```sql
-- 使用窗口函数查询每个用户最近一笔订单
SELECT
    t.id,
    t.order_no,
    t.user_id,
    t.pay_amount,
    t.created_at
FROM (
    SELECT
        o.id,
        o.order_no,
        o.user_id,
        o.pay_amount,
        o.created_at,
        ROW_NUMBER() OVER (
            PARTITION BY o.user_id
            ORDER BY o.created_at DESC, o.id DESC
        ) AS row_num
    FROM order_info_sub_demo AS o
) AS t
WHERE t.row_num = 1;
```

相关子查询使用建议：

1. 相关子查询表达能力强，但要关注外层行数。
2. 外层结果集越大，相关子查询的性能风险越高。
3. 相关字段必须有索引，例如 `o2.user_id`。
4. 查询每组 Top 1、Top N 时，MySQL 8 更推荐窗口函数。
5. 对复杂相关子查询，应优先尝试改写为 JOIN、派生表或 CTE。

### 派生表查询

派生表是 `FROM` 子查询产生的临时结果集。它适合承载中间计算逻辑，例如先聚合、先排名、先过滤、先分页，然后外层继续关联或筛选。

先聚合再筛选：

```sql
-- 派生表先统计每个用户的支付金额，外层筛选高价值用户
SELECT
    stat.user_id,
    stat.order_count,
    stat.total_pay_amount
FROM (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_sub_demo
    WHERE order_status = 1
    GROUP BY user_id
) AS stat
WHERE stat.total_pay_amount >= 1000.00
ORDER BY stat.total_pay_amount DESC;
```

先分页主表再关联详情：

```sql
-- 先分页用户主键，再关联订单统计，避免大范围关联后分页
SELECT
    u.id,
    u.username,
    u.nickname,
    COALESCE(stat.order_count, 0) AS order_count
FROM (
    SELECT
        id,
        username,
        nickname
    FROM sys_user_sub_demo
    WHERE deleted = 0
    ORDER BY id DESC
    LIMIT 0, 10
) AS u
LEFT JOIN (
    SELECT
        user_id,
        COUNT(*) AS order_count
    FROM order_info_sub_demo
    GROUP BY user_id
) AS stat ON u.id = stat.user_id
ORDER BY u.id DESC;
```

先聚合订单再关联用户：

```sql
-- 查询有支付订单的用户及其累计支付信息
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    pay_stat.order_count AS paid_order_count,
    pay_stat.total_pay_amount AS total_pay_amount
FROM (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_sub_demo
    WHERE order_status = 1
    GROUP BY user_id
) AS pay_stat
JOIN sys_user_sub_demo AS u ON pay_stat.user_id = u.id
WHERE u.deleted = 0
ORDER BY pay_stat.total_pay_amount DESC;
```

派生表查询建议：

1. 每个派生表必须有别名。
2. 派生表字段应设置清晰别名。
3. 派生表适合拆解复杂 SQL，提高可读性。
4. 对大结果派生表要关注临时表和排序成本。
5. MySQL 8 中复杂派生表可考虑改写为 CTE。

### 子查询优化

子查询优化的目标是减少重复计算、减少扫描行数、充分利用索引，并让 SQL 更容易被优化器处理。优化时不要只看语法形式，应结合 `EXPLAIN` 判断实际执行计划。

使用 `EXPLAIN` 查看子查询执行计划：

```sql
-- 分析子查询执行计划
EXPLAIN
SELECT
    u.id,
    u.username,
    u.nickname
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0
  AND EXISTS (
      SELECT 1
      FROM order_info_sub_demo AS o
      WHERE o.user_id = u.id
        AND o.order_status = 1
  );
```

为子查询关联字段增加索引：

```sql
-- 为订单表增加用户和状态联合索引
ALTER TABLE order_info_sub_demo
  ADD KEY idx_user_id_status (user_id, order_status);
```

将 `IN` 子查询改写为 `EXISTS`：

```sql
-- 原写法：IN 子查询
SELECT
    u.id,
    u.username
FROM sys_user_sub_demo AS u
WHERE u.id IN (
    SELECT o.user_id
    FROM order_info_sub_demo AS o
    WHERE o.order_status = 1
);

-- 改写：EXISTS 子查询
SELECT
    u.id,
    u.username
FROM sys_user_sub_demo AS u
WHERE EXISTS (
    SELECT 1
    FROM order_info_sub_demo AS o
    WHERE o.user_id = u.id
      AND o.order_status = 1
);
```

将 `SELECT` 子查询改写为派生表关联：

```sql
-- 原写法：每个用户执行订单统计子查询
SELECT
    u.id,
    u.username,
    (
        SELECT COUNT(*)
        FROM order_info_sub_demo AS o
        WHERE o.user_id = u.id
    ) AS order_count
FROM sys_user_sub_demo AS u
WHERE u.deleted = 0;

-- 优化写法：先聚合订单，再关联用户
SELECT
    u.id,
    u.username,
    COALESCE(stat.order_count, 0) AS order_count
FROM sys_user_sub_demo AS u
LEFT JOIN (
    SELECT
        user_id,
        COUNT(*) AS order_count
    FROM order_info_sub_demo
    GROUP BY user_id
) AS stat ON u.id = stat.user_id
WHERE u.deleted = 0;
```

避免 `NOT IN` 受 `NULL` 影响：

```sql
-- 不推荐：子查询中如果 user_id 存在 NULL，结果可能异常
SELECT
    u.id,
    u.username
FROM sys_user_sub_demo AS u
WHERE u.id NOT IN (
    SELECT user_id
    FROM sys_user_role_sub_demo
);

-- 推荐：使用 NOT EXISTS
SELECT
    u.id,
    u.username
FROM sys_user_sub_demo AS u
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role_sub_demo AS ur
    WHERE ur.user_id = u.id
);
```

子查询优化建议：

| 问题              | 优化方式                  |
| ----------------- | ------------------------- |
| 子查询重复执行    | 改写为 JOIN、派生表或 CTE |
| 子查询结果很大    | 先过滤、加索引、减少字段  |
| `NOT IN` 结果异常 | 改为 `NOT EXISTS`         |
| 分组后再关联慢    | 先缩小时间范围，再分组    |
| 每行计算统计值    | 先聚合再关联              |
| 查询难读          | 使用 CTE 拆解逻辑         |

常用原则：

1. 子查询字段和关联字段必须建立合适索引。
2. 能提前过滤的数据，尽量在子查询内部过滤。
3. 只判断存在性时使用 `EXISTS` 或 `NOT EXISTS`。
4. 外层结果集很大时，谨慎使用 `SELECT` 子查询。
5. 大表复杂统计建议使用汇总表、临时表或异步预计算。
6. 最终性能判断以 `EXPLAIN` 和真实数据测试为准。

## 联合查询

联合查询用于把多个查询结果纵向合并为一个结果集。MySQL 支持 `UNION` 和 `UNION ALL`。二者的核心区别是：`UNION` 会去重，`UNION ALL` 不去重。实际项目中，如果没有明确去重需求，应优先使用 `UNION ALL`。

本节示例默认使用以下表：

```sql
-- 系统消息表
CREATE TABLE IF NOT EXISTS sys_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    message_title VARCHAR(200) NOT NULL COMMENT '消息标题',
    message_content VARCHAR(1000) DEFAULT NULL COMMENT '消息内容',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_receiver_created_at (receiver_id, created_at),
    KEY idx_read_status (read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log_union_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    operation_name VARCHAR(200) NOT NULL COMMENT '操作名称',
    operation_result TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果：0失败，1成功',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志联合查询示例表';
```

### UNION 查询

`UNION` 用于合并多个查询结果，并自动去除完全重复的行。它适合需要最终结果去重的场景，例如合并多个来源的用户 ID、多个渠道的编码集合等。

合并两个用户 ID 来源并去重：

```sql
-- 合并消息接收人和操作日志用户，并去重
SELECT
    receiver_id AS user_id
FROM sys_message
WHERE created_at >= '2026-01-01 00:00:00'

UNION

SELECT
    user_id AS user_id
FROM operation_log_union_demo
WHERE created_at >= '2026-01-01 00:00:00';
```

合并不同状态来源的数据：

```sql
-- 合并未读消息用户和操作失败用户，并去重
SELECT
    receiver_id AS user_id
FROM sys_message
WHERE read_status = 0

UNION

SELECT
    user_id AS user_id
FROM operation_log_union_demo
WHERE operation_result = 0;
```

为联合结果增加来源字段：

```sql
-- 注意：source_type 不同会影响 UNION 去重结果
SELECT
    receiver_id AS user_id,
    'MESSAGE' AS source_type
FROM sys_message
WHERE read_status = 0

UNION

SELECT
    user_id AS user_id,
    'OPERATION_LOG' AS source_type
FROM operation_log_union_demo
WHERE operation_result = 0;
```

`UNION` 使用建议：

1. `UNION` 会执行去重，成本通常高于 `UNION ALL`。
2. 只有明确需要去重时才使用 `UNION`。
3. 去重基于所有返回字段的组合，而不是某一个字段。
4. 如果增加来源字段，原本相同的用户 ID 也可能不再被去重。
5. 大数据量去重应关注临时表、排序和内存消耗。

### UNION ALL 查询

`UNION ALL` 用于合并多个查询结果，不进行去重。它保留所有数据行，性能通常优于 `UNION`，适合日志汇总、流水合并、多个业务来源统一展示等场景。

合并消息和操作日志为用户动态流：

```sql
-- 合并用户消息和操作日志，不去重
SELECT
    id AS biz_id,
    receiver_id AS user_id,
    message_title AS title,
    'MESSAGE' AS biz_type,
    created_at
FROM sys_message
WHERE receiver_id = 10001

UNION ALL

SELECT
    id AS biz_id,
    user_id AS user_id,
    operation_name AS title,
    'OPERATION_LOG' AS biz_type,
    created_at
FROM operation_log_union_demo
WHERE user_id = 10001;
```

合并多个业务表的待处理事项：

```sql
-- 合并不同来源的待处理数据
SELECT
    id AS biz_id,
    receiver_id AS user_id,
    message_title AS biz_title,
    'UNREAD_MESSAGE' AS biz_type,
    created_at
FROM sys_message
WHERE read_status = 0

UNION ALL

SELECT
    id AS biz_id,
    user_id AS user_id,
    operation_name AS biz_title,
    'FAILED_OPERATION' AS biz_type,
    created_at
FROM operation_log_union_demo
WHERE operation_result = 0;
```

对 `UNION ALL` 结果再统计：

```sql
-- 统计每个用户的待处理数量
SELECT
    t.user_id,
    COUNT(*) AS pending_count
FROM (
    SELECT
        receiver_id AS user_id
    FROM sys_message
    WHERE read_status = 0

    UNION ALL

    SELECT
        user_id AS user_id
    FROM operation_log_union_demo
    WHERE operation_result = 0
) AS t
GROUP BY t.user_id
ORDER BY pending_count DESC;
```

`UNION ALL` 使用建议：

1. 没有去重需求时优先使用 `UNION ALL`。
2. 适合合并日志、消息、流水、动态流等来源。
3. 合并后如果要排序或分页，应在外层统一处理。
4. 结果来源需要区分时，应增加 `biz_type` 或 `source_type` 字段。
5. 多个子查询的字段含义必须对齐，避免前端或应用层解析错误。

### 联合查询字段规则

联合查询要求每个 `SELECT` 返回的字段数量一致，并且字段顺序、语义和类型应尽量一致。最终结果的字段名通常由第一个 `SELECT` 决定。

正确示例：

```sql
-- 两个 SELECT 字段数量一致，字段语义对齐
SELECT
    id AS biz_id,
    receiver_id AS user_id,
    message_title AS title,
    'MESSAGE' AS source_type,
    created_at
FROM sys_message

UNION ALL

SELECT
    id AS biz_id,
    user_id AS user_id,
    operation_name AS title,
    'OPERATION_LOG' AS source_type,
    created_at
FROM operation_log_union_demo;
```

错误示例：字段数量不一致：

```sql
-- 错误：第一个 SELECT 返回 4 个字段，第二个 SELECT 返回 3 个字段
SELECT
    id,
    receiver_id,
    message_title,
    created_at
FROM sys_message

UNION ALL

SELECT
    id,
    user_id,
    operation_name
FROM operation_log_union_demo;
```

字段类型应尽量兼容：

```sql
-- 使用 CAST 显式统一字段类型
SELECT
    CAST(id AS CHAR) AS biz_id,
    receiver_id AS user_id,
    message_title AS title,
    'MESSAGE' AS source_type,
    created_at
FROM sys_message

UNION ALL

SELECT
    CAST(id AS CHAR) AS biz_id,
    user_id AS user_id,
    operation_name AS title,
    'OPERATION_LOG' AS source_type,
    created_at
FROM operation_log_union_demo;
```

字段缺失时使用默认值补齐：

```sql
-- 某个来源没有内容字段时，用 NULL 或默认值补齐
SELECT
    id AS biz_id,
    receiver_id AS user_id,
    message_title AS title,
    message_content AS content,
    'MESSAGE' AS source_type,
    created_at
FROM sys_message

UNION ALL

SELECT
    id AS biz_id,
    user_id AS user_id,
    operation_name AS title,
    NULL AS content,
    'OPERATION_LOG' AS source_type,
    created_at
FROM operation_log_union_demo;
```

字段规则总结：

| 规则                 | 说明                                                   |
| -------------------- | ------------------------------------------------------ |
| 字段数量一致         | 每个 `SELECT` 返回字段数必须相同                       |
| 字段顺序一致         | 第 1 个字段对应第 1 个字段，第 2 个字段对应第 2 个字段 |
| 字段类型兼容         | 数值、字符串、时间类型尽量统一                         |
| 字段语义一致         | 不要把标题和状态放在同一列                             |
| 字段名由首个查询决定 | 后续查询别名通常不影响最终字段名                       |
| 缺失字段可补 `NULL`  | 使用 `NULL AS xxx` 或默认值补齐                        |

### 联合查询排序

联合查询的排序通常应放在整个联合查询的最外层，而不是分别对每个子查询排序。外层排序可以对合并后的完整结果统一排序和分页。

合并后统一排序：

```sql
-- 合并用户动态后按时间倒序排序
SELECT
    t.biz_id,
    t.user_id,
    t.title,
    t.source_type,
    t.created_at
FROM (
    SELECT
        id AS biz_id,
        receiver_id AS user_id,
        message_title AS title,
        'MESSAGE' AS source_type,
        created_at
    FROM sys_message
    WHERE receiver_id = 10001

    UNION ALL

    SELECT
        id AS biz_id,
        user_id AS user_id,
        operation_name AS title,
        'OPERATION_LOG' AS source_type,
        created_at
    FROM operation_log_union_demo
    WHERE user_id = 10001
) AS t
ORDER BY t.created_at DESC, t.biz_id DESC;
```

合并后统一分页：

```sql
-- 对联合查询结果统一分页
SELECT
    t.biz_id,
    t.user_id,
    t.title,
    t.source_type,
    t.created_at
FROM (
    SELECT
        id AS biz_id,
        receiver_id AS user_id,
        message_title AS title,
        'MESSAGE' AS source_type,
        created_at
    FROM sys_message
    WHERE receiver_id = 10001

    UNION ALL

    SELECT
        id AS biz_id,
        user_id AS user_id,
        operation_name AS title,
        'OPERATION_LOG' AS source_type,
        created_at
    FROM operation_log_union_demo
    WHERE user_id = 10001
) AS t
ORDER BY t.created_at DESC, t.biz_id DESC
LIMIT 0, 20;
```

如果确实需要每个来源先限制数量，可以对子查询单独包一层：

```sql
-- 每个来源先取最近 100 条，再合并排序分页
SELECT
    t.biz_id,
    t.user_id,
    t.title,
    t.source_type,
    t.created_at
FROM (
    SELECT *
    FROM (
        SELECT
            id AS biz_id,
            receiver_id AS user_id,
            message_title AS title,
            'MESSAGE' AS source_type,
            created_at
        FROM sys_message
        WHERE receiver_id = 10001
        ORDER BY created_at DESC, id DESC
        LIMIT 100
    ) AS m

    UNION ALL

    SELECT *
    FROM (
        SELECT
            id AS biz_id,
            user_id AS user_id,
            operation_name AS title,
            'OPERATION_LOG' AS source_type,
            created_at
        FROM operation_log_union_demo
        WHERE user_id = 10001
        ORDER BY created_at DESC, id DESC
        LIMIT 100
    ) AS l
) AS t
ORDER BY t.created_at DESC, t.biz_id DESC
LIMIT 0, 20;
```

联合查询排序建议：

1. 合并后排序应放在外层统一处理。
2. 分页也应放在外层，保证分页基于完整合并结果。
3. 排序字段必须出现在外层可访问字段中。
4. 多来源数据建议增加 `source_type`，便于前端展示和后续排查。
5. 每个子查询单独 `ORDER BY` 通常不会决定最终合并结果顺序，除非配合子查询 `LIMIT` 使用。

### 联合查询使用场景

联合查询适合把结构相似但来源不同的数据合并展示或统计。它不是 JOIN 的替代品，JOIN 是横向扩展字段，UNION 是纵向合并行。

常见场景如下：

| 场景                  | 推荐写法                  | 示例                           |
| --------------------- | ------------------------- | ------------------------------ |
| 合并多个来源的动态    | `UNION ALL`               | 消息、日志、通知合并为动态流   |
| 合并多个来源的用户 ID | `UNION`                   | 多个行为表中提取用户并去重     |
| 合并冷热表数据        | `UNION ALL`               | 当前订单表与历史订单表合并查询 |
| 合并待处理事项        | `UNION ALL`               | 未读消息、失败任务、审批待办   |
| 合并相同结构分表      | `UNION ALL`               | 按月分表数据汇总               |
| 合并后去重统计        | `UNION` 或外层 `GROUP BY` | 多渠道用户去重                 |

合并冷热订单表：

```sql
-- 查询当前订单和历史订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    order_status,
    created_at,
    'CURRENT' AS data_source
FROM order_info_sub_demo
WHERE user_id = 10001

UNION ALL

SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    order_status,
    created_at,
    'HISTORY' AS data_source
FROM order_info_history
WHERE user_id = 10001;
```

合并不同来源待办：

```sql
-- 合并消息待办和失败操作待办
SELECT
    id AS biz_id,
    receiver_id AS user_id,
    message_title AS title,
    'UNREAD_MESSAGE' AS todo_type,
    created_at
FROM sys_message
WHERE read_status = 0

UNION ALL

SELECT
    id AS biz_id,
    user_id AS user_id,
    operation_name AS title,
    'FAILED_OPERATION' AS todo_type,
    created_at
FROM operation_log_union_demo
WHERE operation_result = 0;
```

合并后按用户去重：

```sql
-- 从多个来源中提取活跃用户并去重
SELECT
    user_id
FROM (
    SELECT
        receiver_id AS user_id
    FROM sys_message
    WHERE created_at >= '2026-01-01 00:00:00'

    UNION ALL

    SELECT
        user_id AS user_id
    FROM operation_log_union_demo
    WHERE created_at >= '2026-01-01 00:00:00'

    UNION ALL

    SELECT
        user_id AS user_id
    FROM order_info_sub_demo
    WHERE created_at >= '2026-01-01 00:00:00'
) AS t
GROUP BY user_id;
```

`UNION` 与 `JOIN` 的区别：

| 对比项       | `UNION` / `UNION ALL`      | `JOIN`                       |
| ------------ | -------------------------- | ---------------------------- |
| 合并方向     | 纵向合并行                 | 横向关联列                   |
| 字段要求     | 多个查询字段数量一致       | 按关联条件连接表             |
| 典型用途     | 多来源列表、日志流、冷热表 | 用户角色、订单明细、商品分类 |
| 是否增加行数 | 会叠加多个查询结果行       | 可能因一对多放大结果         |
| 是否补充字段 | 不适合补充关联字段         | 适合补充关联字段             |

联合查询使用建议：

1. 没有去重需求时优先使用 `UNION ALL`。
2. 需要去重时使用 `UNION`，或使用 `UNION ALL` 后外层 `GROUP BY`。
3. 多来源结果必须统一字段语义和字段类型。
4. 合并后的排序和分页放在外层处理。
5. 大数据量联合查询应确保每个子查询都能利用索引。
6. 联合查询适合合并同类结果，不适合替代正常表关联。


## MySQL 8 高级查询

MySQL 8 高级查询主要包括 CTE、公用表表达式、递归查询和窗口函数。这些能力可以显著提升复杂 SQL 的可读性，适合处理层级数据、排名统计、分组 Top N、环比分析、去重取最新记录等业务场景。

本节示例默认使用以下表结构：

```sql
-- 订单表示例，用于窗口函数、排名、分区统计和 Top N 查询
CREATE TABLE IF NOT EXISTS order_info_advanced_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_channel TINYINT DEFAULT NULL COMMENT '支付渠道：1支付宝，2微信，3银行卡',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_category_amount (category_id, pay_amount),
    KEY idx_order_status_paid_at (order_status, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高级查询订单示例表';

-- 部门表示例，用于递归 CTE 查询树形结构
CREATE TABLE IF NOT EXISTS sys_dept_advanced_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示顶级部门',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_parent_id (parent_id),
    KEY idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高级查询部门示例表';
```

### 公用表表达式 CTE

CTE 是 Common Table Expression 的缩写，使用 `WITH` 定义一个临时结果集，然后在后续 SQL 中引用。CTE 可以让复杂 SQL 分层表达，避免把所有逻辑都写在一个很长的子查询中。

CTE 基础语法：

```sql
-- 使用 CTE 定义临时结果集
WITH paid_order AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        paid_at
    FROM order_info_advanced_demo
    WHERE order_status = 1
)
SELECT
    user_id,
    COUNT(*) AS paid_order_count,
    SUM(pay_amount) AS total_pay_amount
FROM paid_order
GROUP BY user_id;
```

上面 SQL 中，`paid_order` 是一个临时结果集，只在当前 SQL 中有效。外层查询基于 `paid_order` 做用户维度的订单数量和金额统计。

多个 CTE 可以连续定义：

```sql
-- 使用多个 CTE 分步处理统计逻辑
WITH paid_order AS (
    SELECT
        user_id,
        category_id,
        pay_amount,
        paid_at
    FROM order_info_advanced_demo
    WHERE order_status = 1
),
user_pay_summary AS (
    SELECT
        user_id,
        COUNT(*) AS paid_order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM paid_order
    GROUP BY user_id
)
SELECT
    user_id,
    paid_order_count,
    total_pay_amount
FROM user_pay_summary
WHERE total_pay_amount >= 1000.00
ORDER BY total_pay_amount DESC;
```

CTE 与派生表类似，但 CTE 更适合拆解复杂 SQL：

```sql
-- 使用 CTE 查询每个分类的支付金额统计
WITH category_summary AS (
    SELECT
        category_id,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount,
        AVG(pay_amount) AS avg_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
    GROUP BY category_id
)
SELECT
    category_id,
    order_count,
    total_pay_amount,
    ROUND(avg_pay_amount, 2) AS avg_pay_amount
FROM category_summary
WHERE order_count >= 10
ORDER BY total_pay_amount DESC;
```

CTE 使用建议：

| 场景                 | 说明                                      |
| -------------------- | ----------------------------------------- |
| SQL 逻辑较长         | 使用 CTE 分段表达，提高可读性             |
| 先过滤再统计         | CTE 中先筛选目标数据，外层再聚合          |
| 先聚合再筛选         | CTE 中完成分组，外层使用普通 `WHERE` 过滤 |
| 需要多次引用中间结果 | 可使用 CTE 避免重复书写                   |
| 复杂报表查询         | CTE 能让报表 SQL 更容易维护               |

注意事项：

1. CTE 只在当前 SQL 语句中有效，不会创建真实表。
2. CTE 不是一定比子查询快，最终性能应以 `EXPLAIN` 为准。
3. CTE 名称应表达业务含义，例如 `paid_order`、`user_pay_summary`。
4. CTE 字段建议设置清晰别名，便于后续引用。
5. 大数据量场景下，CTE 仍然需要结合索引、过滤条件和执行计划优化。

### 递归 CTE

递归 CTE 使用 `WITH RECURSIVE` 定义，可以处理树形结构、层级关系、连续日期生成、路径展开等场景。典型业务包括部门树、菜单树、分类树、区域树、评论楼中楼等。

递归 CTE 通常包含两部分：

| 部分     | 说明                           |
| -------- | ------------------------------ |
| 锚点查询 | 查询递归的起始数据             |
| 递归查询 | 基于上一轮结果继续查下一层数据 |

查询部门树：

```sql
-- 使用递归 CTE 查询启用状态的部门树
WITH RECURSIVE dept_tree AS (
    -- 锚点查询：查询顶级部门
    SELECT
        id,
        dept_name,
        parent_id,
        sort_order,
        1 AS dept_level,
        CAST(dept_name AS CHAR(1000)) AS dept_path
    FROM sys_dept_advanced_demo
    WHERE parent_id = 0
      AND status = 1

    UNION ALL

    -- 递归查询：查询子部门
    SELECT
        child.id,
        child.dept_name,
        child.parent_id,
        child.sort_order,
        parent.dept_level + 1 AS dept_level,
        CONCAT(parent.dept_path, '/', child.dept_name) AS dept_path
    FROM sys_dept_advanced_demo AS child
    JOIN dept_tree AS parent ON child.parent_id = parent.id
    WHERE child.status = 1
)
SELECT
    id,
    dept_name,
    parent_id,
    dept_level,
    dept_path
FROM dept_tree
ORDER BY dept_path ASC, sort_order ASC;
```

查询指定部门及其所有子部门：

```sql
-- 查询指定部门及其下级部门
WITH RECURSIVE dept_children AS (
    -- 起始部门
    SELECT
        id,
        dept_name,
        parent_id,
        1 AS dept_level
    FROM sys_dept_advanced_demo
    WHERE id = 100

    UNION ALL

    -- 递归查询子部门
    SELECT
        child.id,
        child.dept_name,
        child.parent_id,
        parent.dept_level + 1 AS dept_level
    FROM sys_dept_advanced_demo AS child
    JOIN dept_children AS parent ON child.parent_id = parent.id
)
SELECT
    id,
    dept_name,
    parent_id,
    dept_level
FROM dept_children
ORDER BY dept_level ASC, id ASC;
```

生成连续日期：

```sql
-- 使用递归 CTE 生成 2026-01-01 到 2026-01-07 的日期序列
WITH RECURSIVE date_range AS (
    SELECT DATE('2026-01-01') AS stat_date

    UNION ALL

    SELECT DATE_ADD(stat_date, INTERVAL 1 DAY)
    FROM date_range
    WHERE stat_date < '2026-01-07'
)
SELECT
    stat_date
FROM date_range;
```

补齐每日订单统计：

```sql
-- 生成日期序列并左连接订单统计，补齐没有订单的日期
WITH RECURSIVE date_range AS (
    SELECT DATE('2026-01-01') AS stat_date

    UNION ALL

    SELECT DATE_ADD(stat_date, INTERVAL 1 DAY)
    FROM date_range
    WHERE stat_date < '2026-01-07'
),
daily_order AS (
    SELECT
        DATE(paid_at) AS stat_date,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
      AND paid_at >= '2026-01-01 00:00:00'
      AND paid_at <  '2026-01-08 00:00:00'
    GROUP BY DATE(paid_at)
)
SELECT
    d.stat_date,
    COALESCE(o.order_count, 0) AS order_count,
    COALESCE(o.total_pay_amount, 0.00) AS total_pay_amount
FROM date_range AS d
LEFT JOIN daily_order AS o ON d.stat_date = o.stat_date
ORDER BY d.stat_date ASC;
```

递归 CTE 使用建议：

1. 必须有明确的终止条件，避免无限递归。
2. 树形表的父级字段应建立索引，例如 `parent_id`。
3. 层级过深时要关注递归深度限制和查询耗时。
4. 递归 CTE 适合中小规模层级查询，大规模复杂树可考虑闭包表、路径枚举等模型。
5. 生成日期序列时，范围不宜过大；大范围日期应使用日历维度表。

### 窗口函数

窗口函数用于在不减少结果行数的情况下，对当前行所在的窗口范围进行计算。它常用于排名、分组序号、累计求和、前后行对比、分组统计值展示等场景。

窗口函数基础语法：

```sql
-- 窗口函数基础结构
窗口函数() OVER (
    PARTITION BY 分区字段
    ORDER BY 排序字段
)
```

查询订单并附带用户维度的订单序号：

```sql
-- 按用户分区，对每个用户的订单按创建时间排序编号
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at,
    ROW_NUMBER() OVER (
        PARTITION BY user_id
        ORDER BY created_at DESC, id DESC
    ) AS user_order_seq
FROM order_info_advanced_demo
WHERE order_status = 1;
```

查询订单并附带用户累计消费金额：

```sql
-- 按用户分区，按支付时间累计计算支付金额
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    paid_at,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
        ORDER BY paid_at ASC, id ASC
    ) AS user_running_pay_amount
FROM order_info_advanced_demo
WHERE order_status = 1
  AND paid_at IS NOT NULL;
```

查询订单并附带用户总支付金额：

```sql
-- 每行订单都展示所属用户的总支付金额
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
    ) AS user_total_pay_amount
FROM order_info_advanced_demo
WHERE order_status = 1;
```

常见窗口函数：

| 函数           | 说明                           | 常见用途                |
| -------------- | ------------------------------ | ----------------------- |
| `ROW_NUMBER()` | 分区内连续编号，不考虑并列     | 去重、取最新一条、Top N |
| `RANK()`       | 并列时排名相同，后续排名跳号   | 竞赛排名                |
| `DENSE_RANK()` | 并列时排名相同，后续排名不跳号 | 连续排名                |
| `LAG()`        | 获取当前行前 N 行数据          | 环比、前一次记录        |
| `LEAD()`       | 获取当前行后 N 行数据          | 后一次记录、趋势分析    |
| `SUM() OVER`   | 窗口求和                       | 累计金额、分区总额      |
| `COUNT() OVER` | 窗口计数                       | 分区记录数              |
| `AVG() OVER`   | 窗口平均值                     | 分区平均金额            |

窗口函数使用建议：

1. 窗口函数不会像 `GROUP BY` 一样压缩结果行数。
2. `PARTITION BY` 定义分组窗口，不写则表示整个结果集。
3. `ORDER BY` 定义窗口内顺序，对排名和前后行函数很重要。
4. 分组统计需要减少行数时用 `GROUP BY`，需要保留明细行时用窗口函数。
5. 大数据量窗口排序可能成本较高，应结合索引和时间范围过滤。

### ROW_NUMBER

`ROW_NUMBER()` 用于为窗口内的每一行生成连续序号。即使排序字段值相同，也会分配不同序号。它常用于分组取第一条、去重保留最新记录、查询每组 Top N。

查询每个用户最近一笔订单：

```sql
-- 使用 ROW_NUMBER 查询每个用户最近一笔已支付订单
WITH ranked_order AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        paid_at,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY paid_at DESC, id DESC
        ) AS row_num
    FROM order_info_advanced_demo
    WHERE order_status = 1
      AND paid_at IS NOT NULL
)
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    paid_at
FROM ranked_order
WHERE row_num = 1;
```

查询每个分类支付金额最高的 3 笔订单：

```sql
-- 使用 ROW_NUMBER 查询每个分类金额最高的 3 笔订单
WITH category_order_rank AS (
    SELECT
        id,
        order_no,
        category_id,
        product_name,
        pay_amount,
        ROW_NUMBER() OVER (
            PARTITION BY category_id
            ORDER BY pay_amount DESC, id DESC
        ) AS row_num
    FROM order_info_advanced_demo
    WHERE order_status = 1
)
SELECT
    category_id,
    order_no,
    product_name,
    pay_amount,
    row_num
FROM category_order_rank
WHERE row_num <= 3
ORDER BY category_id ASC, row_num ASC;
```

对重复订单号保留最新一条：

```sql
-- 按订单号去重，保留创建时间最新的一条
WITH duplicate_order AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        created_at,
        ROW_NUMBER() OVER (
            PARTITION BY order_no
            ORDER BY created_at DESC, id DESC
        ) AS row_num
    FROM order_info_advanced_demo
)
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM duplicate_order
WHERE row_num = 1;
```

`ROW_NUMBER` 使用建议：

1. 适合“每组只取一条”或“每组 Top N”。
2. `ORDER BY` 中建议追加主键，保证排序稳定。
3. 不处理并列排名；即使金额相同，也会分配不同序号。
4. 去重查询时，需要明确保留哪条记录。
5. 大表使用时应先用 `WHERE` 缩小数据范围。

### RANK 与 DENSE_RANK

`RANK()` 和 `DENSE_RANK()` 都用于排名，并且都支持并列排名。区别是：`RANK()` 并列后会跳号，`DENSE_RANK()` 并列后不会跳号。

示例数据排名逻辑：

| 金额 | `RANK()` | `DENSE_RANK()` |
| ---- | -------- | -------------- |
| 1000 | 1        | 1              |
| 900  | 2        | 2              |
| 900  | 2        | 2              |
| 800  | 4        | 3              |

按支付金额进行全局排名：

```sql
-- 对已支付订单按金额排名，展示 RANK 和 DENSE_RANK 的区别
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    RANK() OVER (
        ORDER BY pay_amount DESC
    ) AS amount_rank,
    DENSE_RANK() OVER (
        ORDER BY pay_amount DESC
    ) AS amount_dense_rank
FROM order_info_advanced_demo
WHERE order_status = 1
ORDER BY pay_amount DESC, id DESC;
```

按分类内金额排名：

```sql
-- 每个分类内按支付金额排名
SELECT
    id,
    order_no,
    category_id,
    product_name,
    pay_amount,
    RANK() OVER (
        PARTITION BY category_id
        ORDER BY pay_amount DESC
    ) AS category_rank,
    DENSE_RANK() OVER (
        PARTITION BY category_id
        ORDER BY pay_amount DESC
    ) AS category_dense_rank
FROM order_info_advanced_demo
WHERE order_status = 1
ORDER BY category_id ASC, category_rank ASC;
```

查询每个分类金额排名前 3 的订单，包含并列：

```sql
-- 使用 DENSE_RANK 查询每个分类金额排名前 3 的订单，保留并列名次
WITH category_rank AS (
    SELECT
        id,
        order_no,
        category_id,
        product_name,
        pay_amount,
        DENSE_RANK() OVER (
            PARTITION BY category_id
            ORDER BY pay_amount DESC
        ) AS dense_rank_num
    FROM order_info_advanced_demo
    WHERE order_status = 1
)
SELECT
    category_id,
    order_no,
    product_name,
    pay_amount,
    dense_rank_num
FROM category_rank
WHERE dense_rank_num <= 3
ORDER BY category_id ASC, dense_rank_num ASC, pay_amount DESC;
```

使用建议：

1. 需要竞赛式排名时使用 `RANK()`，例如 1、2、2、4。
2. 需要连续排名时使用 `DENSE_RANK()`，例如 1、2、2、3。
3. 不允许并列、只需要固定条数时使用 `ROW_NUMBER()`。
4. 排名字段相同会产生并列，是否并列要符合业务规则。
5. 排名查询通常需要在外层过滤名次，因为窗口函数不能直接写在同层 `WHERE` 中。

### LAG 与 LEAD

`LAG()` 用于获取当前行前面的数据，`LEAD()` 用于获取当前行后面的数据。它们常用于计算环比、前后状态变化、相邻记录时间差、上一次支付金额和下一次订单时间。

查询用户订单与上一笔订单金额：

```sql
-- 查询每个用户订单及其上一笔订单金额
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    paid_at,
    LAG(pay_amount, 1) OVER (
        PARTITION BY user_id
        ORDER BY paid_at ASC, id ASC
    ) AS previous_pay_amount
FROM order_info_advanced_demo
WHERE order_status = 1
  AND paid_at IS NOT NULL;
```

计算与上一笔订单的金额差：

```sql
-- 计算当前订单与上一笔订单的支付金额差
WITH user_order AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        paid_at,
        LAG(pay_amount, 1) OVER (
            PARTITION BY user_id
            ORDER BY paid_at ASC, id ASC
        ) AS previous_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
      AND paid_at IS NOT NULL
)
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    previous_pay_amount,
    pay_amount - previous_pay_amount AS diff_amount,
    paid_at
FROM user_order;
```

查询下一笔订单时间：

```sql
-- 查询每个用户当前订单的下一笔订单时间
SELECT
    id,
    order_no,
    user_id,
    paid_at,
    LEAD(paid_at, 1) OVER (
        PARTITION BY user_id
        ORDER BY paid_at ASC, id ASC
    ) AS next_paid_at
FROM order_info_advanced_demo
WHERE order_status = 1
  AND paid_at IS NOT NULL;
```

计算两次下单间隔天数：

```sql
-- 计算当前订单与下一笔订单的间隔天数
WITH user_order AS (
    SELECT
        id,
        order_no,
        user_id,
        paid_at,
        LEAD(paid_at, 1) OVER (
            PARTITION BY user_id
            ORDER BY paid_at ASC, id ASC
        ) AS next_paid_at
    FROM order_info_advanced_demo
    WHERE order_status = 1
      AND paid_at IS NOT NULL
)
SELECT
    id,
    order_no,
    user_id,
    paid_at,
    next_paid_at,
    TIMESTAMPDIFF(DAY, paid_at, next_paid_at) AS next_order_interval_days
FROM user_order;
```

`LAG` 与 `LEAD` 使用建议：

1. 必须明确 `ORDER BY`，否则前后行没有稳定业务含义。
2. `PARTITION BY` 通常按用户、商品、分类等业务主体分区。
3. 第一行的 `LAG` 结果通常为 `NULL`，最后一行的 `LEAD` 结果通常为 `NULL`。
4. 可使用第三个参数设置默认值，例如 `LAG(pay_amount, 1, 0)`。
5. 环比、同比、状态流转分析中非常常用。

### 分区统计

分区统计是指使用窗口函数的 `PARTITION BY` 按某个维度划分窗口，并在每个窗口内执行统计计算。与 `GROUP BY` 不同，分区统计不会减少明细行数。

查询订单明细并展示用户总支付金额：

```sql
-- 每行订单展示所属用户的总支付金额
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
    ) AS user_total_pay_amount
FROM order_info_advanced_demo
WHERE order_status = 1;
```

查询订单明细并展示分类平均支付金额：

```sql
-- 每行订单展示所属分类的平均支付金额
SELECT
    id,
    order_no,
    category_id,
    product_name,
    pay_amount,
    ROUND(AVG(pay_amount) OVER (
        PARTITION BY category_id
    ), 2) AS category_avg_pay_amount
FROM order_info_advanced_demo
WHERE order_status = 1;
```

计算订单金额占用户总金额比例：

```sql
-- 计算每笔订单金额占该用户累计支付金额的比例
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
    ) AS user_total_pay_amount,
    ROUND(
        pay_amount / NULLIF(SUM(pay_amount) OVER (PARTITION BY user_id), 0) * 100,
        2
    ) AS amount_percent
FROM order_info_advanced_demo
WHERE order_status = 1;
```

计算用户内累计支付金额：

```sql
-- 按用户分区，按支付时间累计求和
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    paid_at,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
        ORDER BY paid_at ASC, id ASC
    ) AS running_total_amount
FROM order_info_advanced_demo
WHERE order_status = 1
  AND paid_at IS NOT NULL
ORDER BY user_id ASC, paid_at ASC;
```

分区统计使用建议：

1. 需要保留明细行并展示分组统计值时，使用窗口函数。
2. 需要压缩成一行统计结果时，使用 `GROUP BY`。
3. 分区字段通常是用户、分类、渠道、地区、日期等维度。
4. 累计统计需要在窗口中增加 `ORDER BY`。
5. 分区统计结果可继续在外层 SQL 中过滤或排序。

### 排名查询

排名查询用于对数据按金额、数量、时间、分数等指标排序并生成名次。MySQL 8 中推荐使用窗口函数实现排名，避免复杂自关联或变量写法。

按用户累计支付金额排名：

```sql
-- 先按用户聚合支付金额，再使用窗口函数排名
WITH user_pay_summary AS (
    SELECT
        user_id,
        COUNT(*) AS paid_order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
    GROUP BY user_id
)
SELECT
    user_id,
    paid_order_count,
    total_pay_amount,
    RANK() OVER (
        ORDER BY total_pay_amount DESC
    ) AS pay_rank
FROM user_pay_summary
ORDER BY pay_rank ASC, user_id ASC;
```

按分类内商品销售金额排名：

```sql
-- 先按分类和商品聚合，再在分类内排名
WITH product_sales AS (
    SELECT
        category_id,
        product_id,
        product_name,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
    GROUP BY category_id, product_id, product_name
)
SELECT
    category_id,
    product_id,
    product_name,
    order_count,
    total_pay_amount,
    DENSE_RANK() OVER (
        PARTITION BY category_id
        ORDER BY total_pay_amount DESC
    ) AS category_sales_rank
FROM product_sales
ORDER BY category_id ASC, category_sales_rank ASC;
```

查询支付金额排名前 10 的用户：

```sql
-- 查询全局支付金额排名前 10 的用户
WITH user_pay_summary AS (
    SELECT
        user_id,
        COUNT(*) AS paid_order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
    GROUP BY user_id
),
user_rank AS (
    SELECT
        user_id,
        paid_order_count,
        total_pay_amount,
        ROW_NUMBER() OVER (
            ORDER BY total_pay_amount DESC, user_id ASC
        ) AS row_num
    FROM user_pay_summary
)
SELECT
    user_id,
    paid_order_count,
    total_pay_amount,
    row_num
FROM user_rank
WHERE row_num <= 10
ORDER BY row_num ASC;
```

排名查询使用建议：

1. 先聚合，再排名，是报表排名的常见模式。
2. 排名指标必须明确，例如支付金额、订单数、退款率等。
3. 是否允许并列决定使用 `ROW_NUMBER`、`RANK` 还是 `DENSE_RANK`。
4. 排名字段相同时，建议追加稳定排序字段，例如 `user_id`。
5. 排名结果通常需要外层查询过滤，因为窗口函数不能直接写在同层 `WHERE` 中。

### Top N 查询

Top N 查询用于获取全局前 N 条或每组前 N 条数据。MySQL 8 中，分组 Top N 推荐使用 `ROW_NUMBER()`、`RANK()` 或 `DENSE_RANK()`。

查询全局支付金额最高的 10 笔订单：

```sql
-- 查询全局金额最高的 10 笔订单
SELECT
    id,
    order_no,
    user_id,
    product_name,
    pay_amount,
    paid_at
FROM order_info_advanced_demo
WHERE order_status = 1
ORDER BY pay_amount DESC, id DESC
LIMIT 10;
```

查询每个分类销售金额最高的 3 个商品：

```sql
-- 查询每个分类销售金额最高的 3 个商品
WITH product_sales AS (
    SELECT
        category_id,
        product_id,
        product_name,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM order_info_advanced_demo
    WHERE order_status = 1
    GROUP BY category_id, product_id, product_name
),
product_rank AS (
    SELECT
        category_id,
        product_id,
        product_name,
        order_count,
        total_pay_amount,
        ROW_NUMBER() OVER (
            PARTITION BY category_id
            ORDER BY total_pay_amount DESC, product_id ASC
        ) AS row_num
    FROM product_sales
)
SELECT
    category_id,
    product_id,
    product_name,
    order_count,
    total_pay_amount,
    row_num
FROM product_rank
WHERE row_num <= 3
ORDER BY category_id ASC, row_num ASC;
```

查询每个用户最近 3 笔订单：

```sql
-- 查询每个用户最近 3 笔已支付订单
WITH user_order_rank AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        paid_at,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY paid_at DESC, id DESC
        ) AS row_num
    FROM order_info_advanced_demo
    WHERE order_status = 1
      AND paid_at IS NOT NULL
)
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    paid_at,
    row_num
FROM user_order_rank
WHERE row_num <= 3
ORDER BY user_id ASC, row_num ASC;
```

查询每个分类金额排名前 3，允许并列：

```sql
-- 使用 DENSE_RANK 查询每个分类金额排名前 3，保留并列数据
WITH category_order_rank AS (
    SELECT
        id,
        order_no,
        category_id,
        product_name,
        pay_amount,
        DENSE_RANK() OVER (
            PARTITION BY category_id
            ORDER BY pay_amount DESC
        ) AS dense_rank_num
    FROM order_info_advanced_demo
    WHERE order_status = 1
)
SELECT
    category_id,
    order_no,
    product_name,
    pay_amount,
    dense_rank_num
FROM category_order_rank
WHERE dense_rank_num <= 3
ORDER BY category_id ASC, dense_rank_num ASC, pay_amount DESC;
```

Top N 查询选择建议：

| 场景                          | 推荐写法                                            |
| ----------------------------- | --------------------------------------------------- |
| 全局前 N 条                   | `ORDER BY ... LIMIT N`                              |
| 每组固定 N 条                 | `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)` |
| 每组前 N 名，保留并列但跳号   | `RANK()`                                            |
| 每组前 N 名，保留并列且不跳号 | `DENSE_RANK()`                                      |
| 每个用户最新一条              | `ROW_NUMBER() = 1`                                  |
| 每个分类销量前三              | 聚合后 `ROW_NUMBER() <= 3`                          |

Top N 查询使用建议：

1. 全局 Top N 使用 `ORDER BY + LIMIT` 即可。
2. 分组 Top N 推荐使用窗口函数。
3. 排序字段要明确，最好追加主键保证稳定排序。
4. 大表 Top N 查询应确保过滤条件和排序字段有合适索引。
5. 如果 Top N 查询用于高频榜单，建议使用定时任务生成榜单汇总表。


## JSON 数据处理

MySQL 8 支持原生 `JSON` 数据类型和一组 JSON 函数，适合存储结构灵活、字段不固定、扩展属性较多的数据。常见场景包括商品扩展属性、第三方接口原始报文、订单快照、动态配置、用户偏好、标签数组等。

JSON 字段不能替代表结构设计。高频查询字段、强约束字段、关联字段、排序字段和统计字段应优先设计为普通列；JSON 更适合存储扩展信息、低频查询信息或结构变化较快的数据。

本节示例默认使用以下表：

```sql
-- 商品扩展信息表，用于演示 JSON 对象、数组、查询、更新和索引优化
CREATE TABLE IF NOT EXISTS product_json_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    extend_attr JSON DEFAULT NULL COMMENT '扩展属性JSON',
    tag_list JSON DEFAULT NULL COMMENT '标签数组JSON',
    snapshot_info JSON DEFAULT NULL COMMENT '商品快照JSON',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category_id (category_id),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品JSON示例表';
```

### JSON 字段设计

JSON 字段设计用于解决结构灵活性问题，但应控制使用边界。设计 JSON 字段前，应先判断字段是否需要频繁查询、排序、统计、唯一约束或关联。如果需要，应设计为普通字段；如果只是扩展展示、原始报文、低频筛选，则可以使用 JSON。

适合放入 JSON 的数据：

| 数据类型       | 示例                       | 说明                 |
| -------------- | -------------------------- | -------------------- |
| 扩展属性       | 颜色、尺码、材质、保质期   | 不同商品类目属性不同 |
| 第三方原始报文 | 支付回调、物流回调         | 方便审计和排查问题   |
| 用户偏好       | 主题、语言、通知开关       | 字段变化较灵活       |
| 标签数组       | 新品、热销、推荐           | 标签数量不固定       |
| 快照信息       | 下单时商品名称、价格、属性 | 保留当时状态         |

不建议放入 JSON 的数据：

| 数据类型           | 原因               | 推荐方式         |
| ------------------ | ------------------ | ---------------- |
| 主键、外键         | 需要关联和索引     | 普通字段         |
| 状态字段           | 高频过滤和统计     | `TINYINT`        |
| 金额字段           | 高频计算和精确统计 | `DECIMAL`        |
| 创建时间、更新时间 | 高频范围查询和排序 | `DATETIME`       |
| 用户 ID、订单号    | 高频查询和唯一约束 | 普通字段         |
| 高频筛选属性       | JSON 查询成本较高  | 普通字段或生成列 |

推荐的 JSON 结构示例：

```json
{
  "color": "黑色",
  "size": "XL",
  "material": "纯棉",
  "origin": {
    "country": "中国",
    "province": "浙江"
  },
  "spec": {
    "weight": 0.35,
    "unit": "kg"
  }
}
```

标签数组示例：

```json
[
  "新品",
  "热销",
  "推荐"
]
```

订单商品快照示例：

```json
{
  "productCode": "P10001",
  "productName": "基础款T恤",
  "salePrice": 99.90,
  "attr": {
    "color": "黑色",
    "size": "XL"
  }
}
```

JSON 字段设计建议：

1. JSON 字段命名建议使用 `_json`、`_attr`、`_info`、`_snapshot` 等后缀表达用途。
2. JSON 内部 key 命名应统一，避免同一含义混用 `productName`、`product_name`、`name`。
3. 高频查询字段应冗余为普通字段，必要时通过生成列或函数索引优化。
4. JSON 不适合承载强事务约束、唯一约束和复杂关联关系。
5. JSON 字段内容应有版本意识，结构变化较大时可增加 `schemaVersion` 字段。
6. 应避免一个 JSON 字段无限膨胀，过大的 JSON 会增加存储、备份、复制和网络传输成本。

### JSON 数据插入

JSON 数据可以通过标准 JSON 字符串插入，也可以通过 `JSON_OBJECT`、`JSON_ARRAY` 等函数构造。项目中推荐使用函数构造简单 JSON，或由应用层序列化为合法 JSON 后写入数据库。

使用 JSON 字符串插入：

```sql
-- 直接插入 JSON 对象和 JSON 数组
INSERT INTO product_json_demo (
    product_code,
    product_name,
    category_id,
    sale_price,
    extend_attr,
    tag_list,
    snapshot_info
) VALUES (
    'P10001',
    '基础款T恤',
    10,
    99.90,
    '{"color":"黑色","size":"XL","material":"纯棉","origin":{"country":"中国","province":"浙江"}}',
    '["新品","热销","推荐"]',
    '{"productCode":"P10001","productName":"基础款T恤","salePrice":99.90,"attr":{"color":"黑色","size":"XL"}}'
);
```

使用 `JSON_OBJECT` 和 `JSON_ARRAY` 插入：

```sql
-- 使用 JSON_OBJECT 和 JSON_ARRAY 构造 JSON 数据
INSERT INTO product_json_demo (
    product_code,
    product_name,
    category_id,
    sale_price,
    extend_attr,
    tag_list,
    snapshot_info
) VALUES (
    'P10002',
    '运动鞋',
    20,
    299.00,
    JSON_OBJECT(
        'color', '白色',
        'size', '42',
        'material', '织物',
        'origin', JSON_OBJECT(
            'country', '中国',
            'province', '福建'
        )
    ),
    JSON_ARRAY('运动', '热销', '男款'),
    JSON_OBJECT(
        'productCode', 'P10002',
        'productName', '运动鞋',
        'salePrice', 299.00,
        'attr', JSON_OBJECT(
            'color', '白色',
            'size', '42'
        )
    )
);
```

批量插入 JSON 数据：

```sql
-- 批量插入多条 JSON 数据
INSERT INTO product_json_demo (
    product_code,
    product_name,
    category_id,
    sale_price,
    extend_attr,
    tag_list
) VALUES
(
    'P10003',
    '保温杯',
    30,
    59.90,
    JSON_OBJECT('color', '银色', 'capacity', '500ml', 'material', '不锈钢'),
    JSON_ARRAY('居家', '热销')
),
(
    'P10004',
    '蓝牙耳机',
    40,
    199.00,
    JSON_OBJECT('color', '黑色', 'batteryLife', '24h', 'version', '5.3'),
    JSON_ARRAY('数码', '新品')
);
```

插入前验证 JSON 是否合法：

```sql
-- 验证字符串是否为合法 JSON
SELECT
    JSON_VALID('{"color":"黑色","size":"XL"}') AS valid_json,
    JSON_VALID('{"color":"黑色","size":}') AS invalid_json;
```

使用 `CHECK` 约束限制 JSON 合法性和结构：

```sql
-- 创建配置表，限制 config_value 必须是合法 JSON
CREATE TABLE IF NOT EXISTS sys_json_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_code VARCHAR(64) NOT NULL COMMENT '配置编码',
    config_value JSON NOT NULL COMMENT '配置值JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_config_code (config_code),
    CONSTRAINT chk_config_value_json CHECK (JSON_VALID(config_value))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JSON配置表';
```

插入 JSON 数据建议：

1. JSON 字符串必须是合法 JSON，key 和字符串值必须使用双引号。
2. 推荐使用 `JSON_OBJECT`、`JSON_ARRAY` 构造 JSON，减少格式错误。
3. 应用层写入 JSON 前应完成结构校验，避免脏数据进入数据库。
4. 大 JSON 字段不建议频繁更新，更新成本较高。
5. 批量写入 JSON 时仍需关注 `max_allowed_packet` 限制。

### JSON 数据查询

JSON 查询用于读取 JSON 内部的属性、数组元素或嵌套对象。MySQL 支持 `JSON_EXTRACT`、`->`、`->>` 等方式读取 JSON 路径。

查询完整 JSON 字段：

```sql
-- 查询商品基础信息和完整扩展属性
SELECT
    id,
    product_code,
    product_name,
    extend_attr,
    tag_list
FROM product_json_demo
WHERE product_code = 'P10001';
```

查询 JSON 对象中的属性：

```sql
-- 查询 extend_attr 中的 color 和 size
SELECT
    product_code,
    product_name,
    JSON_EXTRACT(extend_attr, '$.color') AS color_json,
    JSON_EXTRACT(extend_attr, '$.size') AS size_json
FROM product_json_demo;
```

去除 JSON 字符串结果中的双引号：

```sql
-- 使用 JSON_UNQUOTE 去除 JSON 字符串外层引号
SELECT
    product_code,
    product_name,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.color')) AS color,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.size')) AS size
FROM product_json_demo;
```

使用 `->` 和 `->>` 简化查询：

```sql
-- -> 返回 JSON 值，->> 返回去引号后的字符串值
SELECT
    product_code,
    product_name,
    extend_attr -> '$.color' AS color_json,
    extend_attr ->> '$.color' AS color_text,
    extend_attr ->> '$.origin.province' AS origin_province
FROM product_json_demo;
```

查询数组元素：

```sql
-- 查询标签数组中的第一个标签
SELECT
    product_code,
    product_name,
    tag_list ->> '$[0]' AS first_tag
FROM product_json_demo;
```

按 JSON 属性过滤：

```sql
-- 查询颜色为黑色的商品
SELECT
    id,
    product_code,
    product_name,
    extend_attr ->> '$.color' AS color
FROM product_json_demo
WHERE extend_attr ->> '$.color' = '黑色';
```

查询嵌套属性：

```sql
-- 查询产地省份为浙江的商品
SELECT
    id,
    product_code,
    product_name,
    extend_attr ->> '$.origin.province' AS province
FROM product_json_demo
WHERE extend_attr ->> '$.origin.province' = '浙江';
```

JSON 查询建议：

1. `->` 返回 JSON 类型结果，字符串值会保留双引号。
2. `->>` 返回文本结果，适合直接展示、比较和过滤。
3. JSON 路径使用 `$` 表示根节点，例如 `$.color`、`$.origin.province`、`$[0]`。
4. 频繁按 JSON 属性过滤时，应考虑生成列或函数索引。
5. 不建议在大表上大量使用未索引的 JSON 路径过滤。

### JSON_EXTRACT 函数

`JSON_EXTRACT` 用于从 JSON 文档中提取指定路径的值。它是 JSON 查询中最基础、最常用的函数之一。

基础语法：

```sql
-- JSON_EXTRACT 基础语法
JSON_EXTRACT(json_doc, json_path)
```

提取普通属性：

```sql
-- 提取商品颜色和尺码
SELECT
    product_code,
    product_name,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.color')) AS color,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.size')) AS size
FROM product_json_demo;
```

提取嵌套属性：

```sql
-- 提取嵌套对象中的产地信息
SELECT
    product_code,
    product_name,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.origin.country')) AS origin_country,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attr, '$.origin.province')) AS origin_province
FROM product_json_demo;
```

提取数组元素：

```sql
-- 提取标签数组中的第一个和第二个标签
SELECT
    product_code,
    product_name,
    JSON_UNQUOTE(JSON_EXTRACT(tag_list, '$[0]')) AS first_tag,
    JSON_UNQUOTE(JSON_EXTRACT(tag_list, '$[1]')) AS second_tag
FROM product_json_demo;
```

提取多个路径：

```sql
-- 一次提取多个 JSON 路径，返回 JSON 数组
SELECT
    product_code,
    JSON_EXTRACT(extend_attr, '$.color', '$.size') AS color_and_size
FROM product_json_demo;
```

判断路径是否存在：

```sql
-- 判断 extend_attr 中是否存在 batteryLife 属性
SELECT
    product_code,
    product_name,
    JSON_CONTAINS_PATH(extend_attr, 'one', '$.batteryLife') AS has_battery_life
FROM product_json_demo;
```

`JSON_EXTRACT` 使用建议：

| 写法                              | 返回结果 | 适用场景           |
| --------------------------------- | -------- | ------------------ |
| `JSON_EXTRACT(json_col, '$.key')` | JSON 值  | 通用提取           |
| `JSON_UNQUOTE(JSON_EXTRACT(...))` | 文本值   | 展示和比较         |
| `json_col -> '$.key'`             | JSON 值  | 简化写法           |
| `json_col ->> '$.key'`            | 文本值   | 推荐用于字符串比较 |

注意事项：

1. 路径不存在时返回 `NULL`。
2. 提取字符串时，`JSON_EXTRACT` 返回的是 JSON 字符串，通常需要 `JSON_UNQUOTE`。
3. 查询数组时下标从 `0` 开始。
4. JSON 路径大小写敏感，应统一 key 命名。
5. 复杂路径提取应配合字段别名，提高 SQL 可读性。

### JSON_SET 函数

`JSON_SET` 用于新增或修改 JSON 路径对应的值。如果路径存在，则更新；如果路径不存在，则尝试新增。它适合对 JSON 中的部分属性进行局部更新。

基础语法：

```sql
-- JSON_SET 基础语法
JSON_SET(json_doc, path, value[, path, value] ...)
```

修改商品颜色：

```sql
-- 修改 extend_attr 中的 color 属性
UPDATE product_json_demo
SET extend_attr = JSON_SET(extend_attr, '$.color', '白色')
WHERE product_code = 'P10001';
```

同时修改多个属性：

```sql
-- 同时修改颜色和尺码
UPDATE product_json_demo
SET extend_attr = JSON_SET(
    extend_attr,
    '$.color', '蓝色',
    '$.size', 'L'
)
WHERE product_code = 'P10001';
```

新增嵌套属性：

```sql
-- 新增或修改商品售后信息
UPDATE product_json_demo
SET extend_attr = JSON_SET(
    extend_attr,
    '$.afterSale.supportReturn', true,
    '$.afterSale.days', 7
)
WHERE product_code = 'P10001';
```

更新数组指定位置：

```sql
-- 修改标签数组中的第一个标签
UPDATE product_json_demo
SET tag_list = JSON_SET(tag_list, '$[0]', '爆款')
WHERE product_code = 'P10001';
```

向数组尾部追加元素可以使用 `JSON_ARRAY_APPEND`：

```sql
-- 向标签数组追加新标签
UPDATE product_json_demo
SET tag_list = JSON_ARRAY_APPEND(tag_list, '$', '限时优惠')
WHERE product_code = 'P10001';
```

查询修改结果：

```sql
-- 查看 JSON 修改后的结果
SELECT
    product_code,
    product_name,
    extend_attr,
    tag_list
FROM product_json_demo
WHERE product_code = 'P10001';
```

`JSON_SET` 使用建议：

1. `JSON_SET` 适合修改局部 JSON 属性，不需要整段 JSON 替换。
2. 更新多个路径时，可以在一次 `JSON_SET` 中完成。
3. 更新数组时要明确数组下标，数组下标从 `0` 开始。
4. 对大 JSON 字段频繁执行局部更新仍然会有较高成本。
5. 对关键业务字段，不建议只存放在 JSON 中再频繁更新。

### JSON_REMOVE 函数

`JSON_REMOVE` 用于从 JSON 文档中删除指定路径的属性或数组元素。它适合清理废弃字段、删除无效标签、移除敏感属性等场景。

基础语法：

```sql
-- JSON_REMOVE 基础语法
JSON_REMOVE(json_doc, path[, path] ...)
```

删除普通属性：

```sql
-- 删除 extend_attr 中的 material 属性
UPDATE product_json_demo
SET extend_attr = JSON_REMOVE(extend_attr, '$.material')
WHERE product_code = 'P10001';
```

删除嵌套属性：

```sql
-- 删除 afterSale 中的 days 属性
UPDATE product_json_demo
SET extend_attr = JSON_REMOVE(extend_attr, '$.afterSale.days')
WHERE product_code = 'P10001';
```

一次删除多个属性：

```sql
-- 同时删除多个 JSON 路径
UPDATE product_json_demo
SET extend_attr = JSON_REMOVE(
    extend_attr,
    '$.material',
    '$.origin.province'
)
WHERE product_code = 'P10001';
```

删除数组元素：

```sql
-- 删除标签数组中的第一个元素
UPDATE product_json_demo
SET tag_list = JSON_REMOVE(tag_list, '$[0]')
WHERE product_code = 'P10001';
```

删除数组元素时要注意，删除后数组会重新排列。例如删除 `$[0]` 后，原来的 `$[1]` 会变成新的 `$[0]`。如果要根据标签值删除数组元素，通常需要先查询位置或在应用层处理后整体更新。

查看删除后的结果：

```sql
-- 查看删除 JSON 路径后的结果
SELECT
    product_code,
    product_name,
    extend_attr,
    tag_list
FROM product_json_demo
WHERE product_code = 'P10001';
```

`JSON_REMOVE` 使用建议：

1. 删除不存在的路径通常不会报错，会返回原 JSON。
2. 删除数组元素时要注意下标变化。
3. 清理废弃 JSON key 前应确认应用代码不再读取该字段。
4. 删除敏感属性时，应确认历史备份、日志和同步链路中是否仍有数据。
5. 大批量 JSON 字段清理建议分批执行，避免长事务。

### JSON_CONTAINS 函数

`JSON_CONTAINS` 用于判断一个 JSON 文档是否包含指定 JSON 值。它常用于判断数组中是否包含某个标签、对象中是否包含某组属性等。

基础语法：

```sql
-- JSON_CONTAINS 基础语法
JSON_CONTAINS(target_json, candidate_json[, path])
```

判断标签数组是否包含某个值：

```sql
-- 查询包含“热销”标签的商品
SELECT
    id,
    product_code,
    product_name,
    tag_list
FROM product_json_demo
WHERE JSON_CONTAINS(tag_list, JSON_QUOTE('热销'));
```

也可以直接传入 JSON 字符串值：

```sql
-- 查询包含“新品”标签的商品
SELECT
    id,
    product_code,
    product_name,
    tag_list
FROM product_json_demo
WHERE JSON_CONTAINS(tag_list, '"新品"');
```

判断对象是否包含指定属性和值：

```sql
-- 查询 extend_attr 中包含 color=黑色 的商品
SELECT
    id,
    product_code,
    product_name,
    extend_attr
FROM product_json_demo
WHERE JSON_CONTAINS(extend_attr, '{"color":"黑色"}');
```

在指定路径下判断是否包含值：

```sql
-- 判断 origin 对象中是否包含 province=浙江
SELECT
    id,
    product_code,
    product_name,
    extend_attr
FROM product_json_demo
WHERE JSON_CONTAINS(
    extend_attr,
    '{"province":"浙江"}',
    '$.origin'
);
```

使用 `JSON_OVERLAPS` 判断两个 JSON 是否有交集：

```sql
-- 查询标签数组中包含“热销”或“新品”的商品
SELECT
    id,
    product_code,
    product_name,
    tag_list
FROM product_json_demo
WHERE JSON_OVERLAPS(tag_list, JSON_ARRAY('热销', '新品'));
```

`JSON_CONTAINS` 使用建议：

1. 判断数组中是否包含字符串时，要传入合法 JSON 字符串，例如 `'"热销"'`。
2. 使用 `JSON_QUOTE('热销')` 可以减少手写双引号出错。
3. 判断对象包含关系时，候选值必须是合法 JSON 对象。
4. 大表中频繁使用 `JSON_CONTAINS` 可能性能较差，应考虑多值索引或拆分关系表。
5. 标签、属性筛选如果是核心查询条件，不建议长期只依赖 JSON 扫描。

### JSON_TABLE 使用

`JSON_TABLE` 可以把 JSON 数据展开成关系型表结构，适合把 JSON 数组拆成多行，或把 JSON 对象映射成多列。它常用于报表查询、数据清洗、JSON 数组展开、第三方报文解析等场景。

基础示例：把标签数组展开成多行：

```sql
-- 使用 JSON_TABLE 将 tag_list 数组展开成多行
SELECT
    p.product_code,
    p.product_name,
    jt.tag_name
FROM product_json_demo AS p
JOIN JSON_TABLE(
    p.tag_list,
    '$[*]' COLUMNS (
        tag_name VARCHAR(100) PATH '$'
    )
) AS jt;
```

查询包含指定标签的商品，也可以通过展开后过滤：

```sql
-- 展开标签数组后筛选热销商品
SELECT
    p.id,
    p.product_code,
    p.product_name,
    jt.tag_name
FROM product_json_demo AS p
JOIN JSON_TABLE(
    p.tag_list,
    '$[*]' COLUMNS (
        tag_name VARCHAR(100) PATH '$'
    )
) AS jt
WHERE jt.tag_name = '热销';
```

解析对象字段为列：

```sql
-- 使用 JSON_TABLE 将 extend_attr 中的字段映射为列
SELECT
    p.product_code,
    p.product_name,
    jt.color,
    jt.size,
    jt.material
FROM product_json_demo AS p
JOIN JSON_TABLE(
    p.extend_attr,
    '$' COLUMNS (
        color VARCHAR(50) PATH '$.color',
        size VARCHAR(50) PATH '$.size',
        material VARCHAR(100) PATH '$.material'
    )
) AS jt;
```

解析嵌套对象：

```sql
-- 解析 origin 嵌套对象中的国家和省份
SELECT
    p.product_code,
    p.product_name,
    jt.color,
    jt.origin_country,
    jt.origin_province
FROM product_json_demo AS p
JOIN JSON_TABLE(
    p.extend_attr,
    '$' COLUMNS (
        color VARCHAR(50) PATH '$.color',
        origin_country VARCHAR(100) PATH '$.origin.country',
        origin_province VARCHAR(100) PATH '$.origin.province'
    )
) AS jt;
```

订单快照数组示例表：

```sql
-- 订单快照表，items_json 存储订单商品明细数组
CREATE TABLE IF NOT EXISTS order_snapshot_json_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    items_json JSON NOT NULL COMMENT '订单商品明细JSON数组',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单快照JSON示例表';
```

插入订单商品数组：

```sql
-- 插入订单商品明细数组
INSERT INTO order_snapshot_json_demo (
    order_no,
    user_id,
    items_json
) VALUES (
    'O202601010001',
    10001,
    JSON_ARRAY(
        JSON_OBJECT('productCode', 'P10001', 'productName', '基础款T恤', 'buyCount', 2, 'salePrice', 99.90),
        JSON_OBJECT('productCode', 'P10002', 'productName', '运动鞋', 'buyCount', 1, 'salePrice', 299.00)
    )
);
```

展开订单商品明细：

```sql
-- 使用 JSON_TABLE 将订单商品明细数组展开成多行
SELECT
    o.order_no,
    o.user_id,
    jt.product_code,
    jt.product_name,
    jt.buy_count,
    jt.sale_price,
    jt.buy_count * jt.sale_price AS item_amount
FROM order_snapshot_json_demo AS o
JOIN JSON_TABLE(
    o.items_json,
    '$[*]' COLUMNS (
        product_code VARCHAR(64) PATH '$.productCode',
        product_name VARCHAR(200) PATH '$.productName',
        buy_count INT PATH '$.buyCount',
        sale_price DECIMAL(18,2) PATH '$.salePrice'
    )
) AS jt;
```

`JSON_TABLE` 使用建议：

1. 适合把 JSON 数组展开为多行，把 JSON 对象映射为多列。
2. JSON 路径必须与实际结构一致，路径错误会导致结果为空或字段为空。
3. 展开大 JSON 数组会放大结果集，应控制数据量。
4. 报表查询中可以用 `JSON_TABLE` 临时解析，但高频查询建议拆表存储。
5. 对订单明细、商品属性、标签关系等核心业务数据，长期高频查询时更推荐关系表建模。

### JSON 索引优化

MySQL 不能像普通字段一样直接对 JSON 内部任意路径建立传统索引。常见优化方式包括：生成列索引、函数索引、多值索引、冗余普通字段和拆分关系表。选择哪种方式取决于查询频率、数据规模和业务稳定性。

为 JSON 路径创建生成列并建立索引：

```sql
-- 新增生成列，将 JSON 中的 color 提取为普通列，并建立索引
ALTER TABLE product_json_demo
  ADD COLUMN attr_color VARCHAR(50)
    GENERATED ALWAYS AS (extend_attr ->> '$.color') STORED COMMENT '扩展属性颜色',
  ADD KEY idx_attr_color (attr_color);
```

使用生成列查询：

```sql
-- 使用生成列查询颜色，便于命中索引
SELECT
    id,
    product_code,
    product_name,
    attr_color
FROM product_json_demo
WHERE attr_color = '黑色';
```

为嵌套路径创建生成列：

```sql
-- 将嵌套 JSON 路径提取为生成列
ALTER TABLE product_json_demo
  ADD COLUMN origin_province VARCHAR(100)
    GENERATED ALWAYS AS (extend_attr ->> '$.origin.province') STORED COMMENT '产地省份',
  ADD KEY idx_origin_province (origin_province);
```

MySQL 8 支持函数索引，可以基于 JSON 表达式建立索引。表达式索引适合不想显式增加生成列字段，但仍希望优化 JSON 路径过滤的场景：

```sql
-- 为 JSON 表达式创建函数索引
CREATE INDEX idx_json_color
ON product_json_demo ((CAST(extend_attr ->> '$.color' AS CHAR(50))));
```

使用函数索引对应的表达式查询：

```sql
-- 查询表达式应与函数索引表达式保持一致
SELECT
    id,
    product_code,
    product_name
FROM product_json_demo
WHERE CAST(extend_attr ->> '$.color' AS CHAR(50)) = '黑色';
```

为 JSON 数组创建多值索引的示例：

```sql
-- 为标签数组创建多值索引，适合数组成员查询
CREATE INDEX idx_tag_list
ON product_json_demo (
    (CAST(tag_list AS CHAR(100) ARRAY))
);
```

使用 `MEMBER OF` 查询数组成员：

```sql
-- 查询包含“热销”标签的商品
SELECT
    id,
    product_code,
    product_name,
    tag_list
FROM product_json_demo
WHERE '热销' MEMBER OF(tag_list);
```

如果标签是核心业务查询条件，更推荐拆成关系表：

```sql
-- 商品标签关系表，适合高频标签筛选和统计
CREATE TABLE IF NOT EXISTS product_tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_product_tag (product_id, tag_name),
    KEY idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签关系表';
```

基于关系表查询标签商品：

```sql
-- 通过关系表查询热销商品
SELECT
    p.id,
    p.product_code,
    p.product_name,
    p.sale_price
FROM product_json_demo AS p
JOIN product_tag_relation AS r ON p.id = r.product_id
WHERE r.tag_name = '热销'
  AND p.status = 1;
```

查看 JSON 查询是否使用索引：

```sql
-- 查看生成列索引查询执行计划
EXPLAIN
SELECT
    id,
    product_code,
    product_name
FROM product_json_demo
WHERE attr_color = '黑色';

-- 查看函数索引查询执行计划
EXPLAIN
SELECT
    id,
    product_code,
    product_name
FROM product_json_demo
WHERE CAST(extend_attr ->> '$.color' AS CHAR(50)) = '黑色';
```

JSON 索引优化建议：

| 场景                     | 推荐方案                    |
| ------------------------ | --------------------------- |
| 高频按 JSON 单值属性过滤 | 生成列 + 普通索引           |
| 不想暴露生成列           | 函数索引                    |
| 高频按 JSON 数组成员过滤 | 多值索引或拆分关系表        |
| 标签、分类、属性需要统计 | 拆分关系表                  |
| JSON 只是展示扩展信息    | 不必建立索引                |
| 查询条件不稳定           | 保留 JSON，按需优化核心路径 |

综合建议：

1. 高频查询字段不要长期只放在 JSON 中。
2. JSON 路径过滤没有索引时，大表性能风险较高。
3. 生成列适合稳定 JSON key，例如 `color`、`brand`、`province`。
4. JSON 数组频繁筛选和统计时，更推荐关系表建模。
5. 建立 JSON 相关索引后，应使用 `EXPLAIN` 验证是否命中。
6. JSON 的灵活性和关系模型的性能之间要做取舍，不要把 JSON 当成万能字段。


## 常用函数

MySQL 函数用于在 SQL 中完成字符串处理、数值计算、日期时间处理、条件判断、类型转换、聚合统计、JSON 操作、哈希计算和系统信息查询。函数可以提升 SQL 表达能力，但在查询条件中滥用函数可能导致索引失效，因此应区分“查询结果处理”和“查询条件过滤”的使用场景。

### 字符串函数

字符串函数用于处理文本字段，例如拼接、截取、替换、大小写转换、去除空格、计算长度、格式化输出等。常见场景包括用户名处理、编码拼接、手机号脱敏、关键字替换、报表字段格式化等。

常用字符串函数如下：

| 函数          | 说明                 | 示例                        |
| ------------- | -------------------- | --------------------------- |
| `CONCAT`      | 拼接字符串           | `CONCAT('A', 'B')`          |
| `CONCAT_WS`   | 使用分隔符拼接字符串 | `CONCAT_WS('-', 'A', 'B')`  |
| `LENGTH`      | 返回字节长度         | `LENGTH('中文')`            |
| `CHAR_LENGTH` | 返回字符长度         | `CHAR_LENGTH('中文')`       |
| `SUBSTRING`   | 截取字符串           | `SUBSTRING('abcdef', 1, 3)` |
| `LEFT`        | 从左侧截取           | `LEFT('abcdef', 3)`         |
| `RIGHT`       | 从右侧截取           | `RIGHT('abcdef', 3)`        |
| `REPLACE`     | 替换字符串           | `REPLACE('a-b-c', '-', '')` |
| `TRIM`        | 去除首尾空格         | `TRIM(' abc ')`             |
| `LOWER`       | 转小写               | `LOWER('ABC')`              |
| `UPPER`       | 转大写               | `UPPER('abc')`              |
| `LOCATE`      | 查找子串位置         | `LOCATE('b', 'abc')`        |
| `LPAD`        | 左侧补齐             | `LPAD('8', 3, '0')`         |
| `RPAD`        | 右侧补齐             | `RPAD('8', 3, '0')`         |

基础示例：

```sql
-- 字符串拼接
SELECT
    CONCAT('用户：', 'zhangsan') AS username_text,
    CONCAT_WS('-', 'ORDER', '20260101', '0001') AS order_no_text;

-- 字符串长度
SELECT
    LENGTH('中文') AS byte_length,
    CHAR_LENGTH('中文') AS char_length;

-- 字符串截取
SELECT
    SUBSTRING('abcdef', 1, 3) AS sub_text,
    LEFT('abcdef', 3) AS left_text,
    RIGHT('abcdef', 3) AS right_text;
```

手机号脱敏示例：

```sql
-- 手机号脱敏：138****0001
SELECT
    mobile,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mask_mobile
FROM sys_user
WHERE mobile IS NOT NULL;
```

订单号格式化示例：

```sql
-- 使用 LPAD 补齐数字编号
SELECT
    CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(18, 6, '0')) AS order_no;
```

替换字符串示例：

```sql
-- 去除手机号中的横线
SELECT
    REPLACE('138-0000-0001', '-', '') AS clean_mobile;
```

字符串函数使用建议：

1. 查询结果展示时可以使用字符串函数，例如脱敏、拼接、格式化。
2. 查询条件中不建议对索引字段使用函数，例如 `WHERE LEFT(username, 3) = 'abc'`，可能导致索引失效。
3. 需要按前缀查询时，优先使用 `LIKE 'abc%'`，而不是 `LEFT(column, 3) = 'abc'`。
4. 中文长度判断应区分 `LENGTH` 和 `CHAR_LENGTH`，前者统计字节，后者统计字符。
5. 字符串清洗类操作如果频繁执行，应考虑在写入前由应用层处理。

不推荐写法：

```sql
-- 不推荐：对索引字段 username 使用函数，可能导致索引失效
SELECT
    id,
    username
FROM sys_user
WHERE LEFT(username, 4) = 'user';
```

推荐写法：

```sql
-- 推荐：前缀匹配更容易利用索引
SELECT
    id,
    username
FROM sys_user
WHERE username LIKE 'user%';
```

### 数值函数

数值函数用于处理金额、数量、比例、评分、随机数、四舍五入等数值计算。业务系统中常用于金额格式化、折扣计算、库存统计、报表指标计算等场景。

常用数值函数如下：

| 函数               | 说明     | 示例                  |
| ------------------ | -------- | --------------------- |
| `ROUND`            | 四舍五入 | `ROUND(3.1415, 2)`    |
| `CEIL` / `CEILING` | 向上取整 | `CEIL(3.1)`           |
| `FLOOR`            | 向下取整 | `FLOOR(3.9)`          |
| `ABS`              | 绝对值   | `ABS(-10)`            |
| `MOD`              | 取模     | `MOD(10, 3)`          |
| `POW` / `POWER`    | 幂运算   | `POW(2, 3)`           |
| `SQRT`             | 平方根   | `SQRT(16)`            |
| `RAND`             | 随机数   | `RAND()`              |
| `TRUNCATE`         | 截断小数 | `TRUNCATE(3.1415, 2)` |

基础示例：

```sql
-- 常用数值函数
SELECT
    ROUND(3.14159, 2) AS round_value,
    CEIL(3.1) AS ceil_value,
    FLOOR(3.9) AS floor_value,
    ABS(-100) AS abs_value,
    MOD(10, 3) AS mod_value,
    POW(2, 3) AS pow_value,
    SQRT(16) AS sqrt_value;
```

金额保留两位小数：

```sql
-- 统计平均支付金额并保留两位小数
SELECT
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount
FROM order_info
WHERE order_status = 1;
```

计算折扣金额：

```sql
-- 计算折扣后金额
SELECT
    id,
    product_name,
    sale_price,
    discount_rate,
    ROUND(sale_price * discount_rate, 2) AS discount_price
FROM product_price_rule;
```

向上取整计算分页总页数：

```sql
-- 根据总记录数和每页条数计算总页数
SELECT
    CEIL(125 / 10) AS total_page;
```

随机排序示例：

```sql
-- 随机查询 5 条商品数据，小表可用，大表慎用
SELECT
    id,
    product_name,
    sale_price
FROM product_info
ORDER BY RAND()
LIMIT 5;
```

数值函数使用建议：

1. 金额字段应使用 `DECIMAL`，计算后可用 `ROUND` 控制小数位。
2. `TRUNCATE` 是直接截断，不等于四舍五入。
3. 大表中不建议使用 `ORDER BY RAND()`，性能较差。
4. 查询条件中不要对索引字段随意使用数值函数。
5. 报表指标计算应明确口径，例如是否包含退款、取消、优惠前金额。

### 日期时间函数

日期时间函数用于处理当前时间、日期格式化、时间加减、时间差计算、年月日提取等。常见场景包括按天统计、订单超时、会员到期、时间范围筛选、报表日期维度处理等。

常用日期时间函数如下：

| 函数              | 说明             | 示例                                   |
| ----------------- | ---------------- | -------------------------------------- |
| `NOW()`           | 当前日期时间     | `2026-05-09 10:30:00`                  |
| `CURDATE()`       | 当前日期         | `2026-05-09`                           |
| `CURTIME()`       | 当前时间         | `10:30:00`                             |
| `UTC_TIMESTAMP()` | UTC 时间         | UTC 日期时间                           |
| `DATE()`          | 提取日期         | `DATE(NOW())`                          |
| `YEAR()`          | 提取年份         | `YEAR(NOW())`                          |
| `MONTH()`         | 提取月份         | `MONTH(NOW())`                         |
| `DAY()`           | 提取日           | `DAY(NOW())`                           |
| `DATE_FORMAT()`   | 格式化日期       | `DATE_FORMAT(NOW(), '%Y-%m-%d')`       |
| `DATE_ADD()`      | 增加时间         | `DATE_ADD(NOW(), INTERVAL 1 DAY)`      |
| `DATE_SUB()`      | 减少时间         | `DATE_SUB(NOW(), INTERVAL 1 DAY)`      |
| `DATEDIFF()`      | 日期差，单位天   | `DATEDIFF('2026-01-02', '2026-01-01')` |
| `TIMESTAMPDIFF()` | 时间差，指定单位 | `TIMESTAMPDIFF(HOUR, t1, t2)`          |

基础示例：

```sql
-- 当前日期时间
SELECT
    NOW() AS now_time,
    CURDATE() AS current_date,
    CURTIME() AS current_time,
    UTC_TIMESTAMP() AS utc_time;

-- 日期提取
SELECT
    YEAR(NOW()) AS year_value,
    MONTH(NOW()) AS month_value,
    DAY(NOW()) AS day_value,
    DATE(NOW()) AS date_value;
```

日期格式化：

```sql
-- 日期格式化
SELECT
    DATE_FORMAT(NOW(), '%Y-%m-%d') AS date_text,
    DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') AS datetime_text,
    DATE_FORMAT(NOW(), '%Y%m%d') AS yyyyMMdd;
```

时间加减：

```sql
-- 时间加减
SELECT
    DATE_ADD(NOW(), INTERVAL 7 DAY) AS after_7_days,
    DATE_SUB(NOW(), INTERVAL 30 DAY) AS before_30_days,
    DATE_ADD(NOW(), INTERVAL 2 HOUR) AS after_2_hours;
```

计算订单支付耗时：

```sql
-- 计算订单从创建到支付的耗时分钟数
SELECT
    id,
    order_no,
    created_at,
    paid_at,
    TIMESTAMPDIFF(MINUTE, created_at, paid_at) AS pay_cost_minutes
FROM order_info
WHERE order_status = 1
  AND paid_at IS NOT NULL;
```

按天统计订单：

```sql
-- 按日期统计订单数量和支付金额
SELECT
    DATE(created_at) AS order_date,
    COUNT(*) AS order_count,
    SUM(pay_amount) AS total_pay_amount
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
GROUP BY DATE(created_at)
ORDER BY order_date ASC;
```

更推荐的时间范围过滤写法：

```sql
-- 推荐：使用左闭右开时间范围，便于利用 created_at 索引
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

不推荐写法：

```sql
-- 不推荐：对索引字段 created_at 使用 DATE 函数，可能导致索引失效
SELECT
    id,
    order_no,
    created_at
FROM order_info
WHERE DATE(created_at) = '2026-01-01';
```

日期时间函数使用建议：

1. 时间范围查询优先使用左闭右开：`>= start_time AND < end_time`。
2. 避免在 `WHERE` 中对索引时间字段使用 `DATE()`、`YEAR()`、`DATE_FORMAT()` 等函数。
3. 报表按天统计时，可以冗余 `order_date DATE` 字段提升性能。
4. 跨时区系统应统一数据库、应用和接口时间规范。
5. 创建时间和更新时间建议使用 `DATETIME`，并统一字段命名。

### 条件判断函数

条件判断函数用于在 SQL 中实现分支逻辑，常见函数包括 `IF`、`IFNULL`、`NULLIF`、`COALESCE` 和 `CASE WHEN`。它们常用于空值处理、状态翻译、报表展示、条件聚合等场景。

常用条件函数如下：

| 函数                                | 说明                                | 示例                                   |
| ----------------------------------- | ----------------------------------- | -------------------------------------- |
| `IF(expr, true_value, false_value)` | 条件判断                            | `IF(status = 1, '启用', '禁用')`       |
| `IFNULL(expr, value)`               | 如果为 NULL 则返回默认值            | `IFNULL(nickname, username)`           |
| `NULLIF(expr1, expr2)`              | 两值相等返回 NULL，否则返回第一个值 | `NULLIF(total, 0)`                     |
| `COALESCE(v1, v2, ...)`             | 返回第一个非 NULL 值                | `COALESCE(nickname, username, '未知')` |
| `CASE WHEN`                         | 多分支条件判断                      | 状态翻译                               |

`IF` 示例：

```sql
-- 使用 IF 将状态转换为文字
SELECT
    id,
    username,
    status,
    IF(status = 1, '启用', '禁用') AS status_name
FROM sys_user;
```

`IFNULL` 和 `COALESCE` 示例：

```sql
-- 空值处理
SELECT
    id,
    username,
    nickname,
    IFNULL(nickname, username) AS display_name,
    COALESCE(nickname, username, '未知用户') AS safe_display_name
FROM sys_user;
```

`CASE WHEN` 状态翻译：

```sql
-- 使用 CASE WHEN 处理多状态展示
SELECT
    id,
    order_no,
    order_status,
    CASE order_status
        WHEN 0 THEN '待支付'
        WHEN 1 THEN '已支付'
        WHEN 2 THEN '已取消'
        WHEN 3 THEN '已退款'
        ELSE '未知状态'
    END AS order_status_name
FROM order_info;
```

条件聚合：

```sql
-- 使用 CASE WHEN 统计不同状态订单数量
SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN order_status = 0 THEN 1 ELSE 0 END) AS wait_pay_count,
    SUM(CASE WHEN order_status = 1 THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN order_status = 2 THEN 1 ELSE 0 END) AS canceled_count,
    SUM(CASE WHEN order_status = 3 THEN 1 ELSE 0 END) AS refunded_count
FROM order_info;
```

避免除零错误：

```sql
-- 使用 NULLIF 避免除以 0
SELECT
    user_id,
    SUM(pay_amount) AS total_pay_amount,
    COUNT(*) AS order_count,
    ROUND(SUM(pay_amount) / NULLIF(COUNT(*), 0), 2) AS avg_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY user_id;
```

条件判断函数使用建议：

1. 简单二选一可以使用 `IF`。
2. 多状态翻译优先使用 `CASE WHEN`。
3. 空值兜底可用 `IFNULL` 或 `COALESCE`。
4. 多字段兜底优先使用 `COALESCE`。
5. 复杂业务判断不建议全部写在 SQL 中，应放在业务代码或字典表中维护。
6. 高频报表中的状态翻译可以在应用层处理，减少数据库计算压力。

### 类型转换函数

类型转换函数用于在 SQL 中显式转换数据类型，常见函数包括 `CAST` 和 `CONVERT`。类型转换常用于字符串转数字、数字转字符串、日期格式转换、JSON 表达式索引等场景。

常用类型转换函数如下：

| 函数                  | 说明           | 示例                          |
| --------------------- | -------------- | ----------------------------- |
| `CAST(expr AS type)`  | 转换类型       | `CAST('123' AS UNSIGNED)`     |
| `CONVERT(expr, type)` | 转换类型       | `CONVERT('123', UNSIGNED)`    |
| `DATE(expr)`          | 转换或提取日期 | `DATE('2026-01-01 10:00:00')` |
| `TIME(expr)`          | 转换或提取时间 | `TIME('2026-01-01 10:00:00')` |

基础示例：

```sql
-- 字符串与数字转换
SELECT
    CAST('123' AS UNSIGNED) AS unsigned_value,
    CAST('123.45' AS DECIMAL(10,2)) AS decimal_value,
    CAST(123 AS CHAR) AS char_value;
```

日期转换：

```sql
-- 日期时间转换
SELECT
    CAST('2026-01-01 10:20:30' AS DATETIME) AS datetime_value,
    CAST('2026-01-01' AS DATE) AS date_value,
    DATE('2026-01-01 10:20:30') AS only_date,
    TIME('2026-01-01 10:20:30') AS only_time;
```

JSON 提取值转数值：

```sql
-- 将 JSON 中的数值属性转为 DECIMAL 后参与计算
SELECT
    product_code,
    product_name,
    CAST(extend_attr ->> '$.weight' AS DECIMAL(10,2)) AS weight_value
FROM product_json_demo
WHERE extend_attr ->> '$.weight' IS NOT NULL;
```

表达式索引中的类型转换示例：

```sql
-- 为 JSON 中的颜色字段创建函数索引时显式转换为 CHAR
CREATE INDEX idx_json_color
ON product_json_demo ((CAST(extend_attr ->> '$.color' AS CHAR(50))));
```

隐式类型转换风险示例：

```sql
-- 不推荐：mobile 是字符串字段，条件使用数字可能触发隐式转换
SELECT
    id,
    username,
    mobile
FROM sys_user
WHERE mobile = 13800000001;
```

推荐写法：

```sql
-- 推荐：字符串字段使用字符串条件
SELECT
    id,
    username,
    mobile
FROM sys_user
WHERE mobile = '13800000001';
```

类型转换使用建议：

1. 避免依赖隐式类型转换，条件值类型应与字段类型一致。
2. 手机号、订单号、编码类字段即使看起来是数字，也应按字符串处理。
3. 在索引字段上使用类型转换可能导致索引失效。
4. JSON 中提取出的文本值参与数值计算时，应显式 `CAST`。
5. 数据清洗脚本中可以使用类型转换，但生产查询条件应尽量避免转换索引字段。

### 聚合函数

聚合函数用于对多行数据进行统计，常见函数包括 `COUNT`、`SUM`、`AVG`、`MAX`、`MIN`、`GROUP_CONCAT` 等。它们通常与 `GROUP BY` 配合使用。

常用聚合函数如下：

| 函数           | 说明     | 示例                      |
| -------------- | -------- | ------------------------- |
| `COUNT`        | 统计行数 | `COUNT(*)`                |
| `SUM`          | 求和     | `SUM(pay_amount)`         |
| `AVG`          | 平均值   | `AVG(pay_amount)`         |
| `MAX`          | 最大值   | `MAX(pay_amount)`         |
| `MIN`          | 最小值   | `MIN(pay_amount)`         |
| `GROUP_CONCAT` | 分组拼接 | `GROUP_CONCAT(role_name)` |

基础聚合示例：

```sql
-- 统计订单数量、总金额、平均金额、最大金额、最小金额
SELECT
    COUNT(*) AS order_count,
    SUM(pay_amount) AS total_pay_amount,
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount,
    MAX(pay_amount) AS max_pay_amount,
    MIN(pay_amount) AS min_pay_amount
FROM order_info
WHERE order_status = 1;
```

按用户聚合：

```sql
-- 按用户统计订单数据
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(pay_amount) AS total_pay_amount,
    ROUND(AVG(pay_amount), 2) AS avg_pay_amount,
    MAX(created_at) AS latest_order_time,
    MIN(created_at) AS first_order_time
FROM order_info
WHERE order_status = 1
GROUP BY user_id;
```

`GROUP_CONCAT` 拼接示例：

```sql
-- 将用户拥有的角色名称拼接为一个字符串
SELECT
    u.id AS user_id,
    u.username AS username,
    GROUP_CONCAT(r.role_name ORDER BY r.role_name ASC SEPARATOR ',') AS role_names
FROM sys_user AS u
JOIN sys_user_role AS ur ON u.id = ur.user_id
JOIN sys_role AS r ON ur.role_id = r.id
WHERE u.deleted = 0
GROUP BY u.id, u.username;
```

条件聚合：

```sql
-- 按用户统计不同状态订单数量
SELECT
    user_id,
    COUNT(*) AS total_count,
    SUM(CASE WHEN order_status = 0 THEN 1 ELSE 0 END) AS wait_pay_count,
    SUM(CASE WHEN order_status = 1 THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN order_status = 2 THEN 1 ELSE 0 END) AS canceled_count
FROM order_info
GROUP BY user_id;
```

聚合函数使用建议：

1. 统计行数优先使用 `COUNT(*)`。
2. `SUM`、`AVG` 在没有匹配数据时可能返回 `NULL`，可用 `COALESCE` 兜底。
3. `COUNT(column)` 只统计非 `NULL` 值。
4. `GROUP_CONCAT` 结果长度受系统参数限制，拼接大量数据时要谨慎。
5. 报表统计应明确口径，例如状态范围、时间范围、是否排除逻辑删除数据。
6. 大表高频聚合建议使用汇总表或异步统计任务。

### JSON 函数

JSON 函数用于构造、查询、修改、删除和判断 JSON 数据。MySQL 8 中 JSON 函数适合处理扩展属性、快照数据、配置数据和第三方接口报文。

常用 JSON 函数如下：

| 函数                 | 说明                 | 示例                                         |
| -------------------- | -------------------- | -------------------------------------------- |
| `JSON_OBJECT`        | 构造 JSON 对象       | `JSON_OBJECT('name', '张三')`                |
| `JSON_ARRAY`         | 构造 JSON 数组       | `JSON_ARRAY('A', 'B')`                       |
| `JSON_EXTRACT`       | 提取 JSON 路径       | `JSON_EXTRACT(attr, '$.color')`              |
| `JSON_UNQUOTE`       | 去除 JSON 字符串引号 | `JSON_UNQUOTE(JSON_EXTRACT(...))`            |
| `JSON_SET`           | 设置或新增路径值     | `JSON_SET(attr, '$.color', '黑色')`          |
| `JSON_REMOVE`        | 删除路径             | `JSON_REMOVE(attr, '$.color')`               |
| `JSON_CONTAINS`      | 判断是否包含 JSON 值 | `JSON_CONTAINS(tags, '"热销"')`              |
| `JSON_CONTAINS_PATH` | 判断路径是否存在     | `JSON_CONTAINS_PATH(attr, 'one', '$.color')` |
| `JSON_LENGTH`        | 返回 JSON 长度       | `JSON_LENGTH(tag_list)`                      |
| `JSON_VALID`         | 判断 JSON 是否合法   | `JSON_VALID(json_text)`                      |

构造 JSON：

```sql
-- 构造 JSON 对象和数组
SELECT
    JSON_OBJECT(
        'productCode', 'P10001',
        'productName', '基础款T恤',
        'salePrice', 99.90
    ) AS product_json,
    JSON_ARRAY('新品', '热销', '推荐') AS tag_json;
```

提取 JSON：

```sql
-- 提取商品扩展属性
SELECT
    product_code,
    product_name,
    extend_attr ->> '$.color' AS color,
    extend_attr ->> '$.size' AS size
FROM product_json_demo;
```

修改 JSON：

```sql
-- 修改 JSON 中的颜色属性
UPDATE product_json_demo
SET extend_attr = JSON_SET(extend_attr, '$.color', '黑色')
WHERE product_code = 'P10001';
```

删除 JSON 属性：

```sql
-- 删除 JSON 中的 material 属性
UPDATE product_json_demo
SET extend_attr = JSON_REMOVE(extend_attr, '$.material')
WHERE product_code = 'P10001';
```

判断标签是否存在：

```sql
-- 查询包含“热销”标签的商品
SELECT
    id,
    product_code,
    product_name,
    tag_list
FROM product_json_demo
WHERE JSON_CONTAINS(tag_list, JSON_QUOTE('热销'));
```

判断路径是否存在：

```sql
-- 判断是否存在 origin.province 属性
SELECT
    product_code,
    product_name,
    JSON_CONTAINS_PATH(extend_attr, 'one', '$.origin.province') AS has_origin_province
FROM product_json_demo;
```

JSON 函数使用建议：

1. `->>` 更适合提取字符串并直接比较。
2. `JSON_CONTAINS` 的候选值必须是合法 JSON。
3. 高频 JSON 路径过滤应考虑生成列、函数索引或拆表。
4. JSON 适合扩展属性，不适合替代核心业务字段。
5. JSON 字段更新仍会带来存储和复制成本，大 JSON 不适合频繁局部更新。

### 加密与哈希函数

加密与哈希函数用于摘要计算、校验、脱敏、简单编码等场景。常见函数包括 `MD5`、`SHA1`、`SHA2`、`TO_BASE64`、`FROM_BASE64`、`UUID` 等。需要注意，哈希函数不是完整的安全加密方案，密码存储不应直接使用简单 MD5 或 SHA。

常用函数如下：

| 函数          | 说明             | 示例                  |
| ------------- | ---------------- | --------------------- |
| `MD5`         | 计算 MD5 摘要    | `MD5('abc')`          |
| `SHA1`        | 计算 SHA-1 摘要  | `SHA1('abc')`         |
| `SHA2`        | 计算 SHA-2 摘要  | `SHA2('abc', 256)`    |
| `TO_BASE64`   | Base64 编码      | `TO_BASE64('abc')`    |
| `FROM_BASE64` | Base64 解码      | `FROM_BASE64('YWJj')` |
| `UUID`        | 生成 UUID        | `UUID()`              |
| `UUID_SHORT`  | 生成短 UUID 数值 | `UUID_SHORT()`        |

哈希示例：

```sql
-- 计算常见哈希值
SELECT
    MD5('hello') AS md5_value,
    SHA1('hello') AS sha1_value,
    SHA2('hello', 256) AS sha256_value,
    SHA2('hello', 512) AS sha512_value;
```

Base64 编码和解码：

```sql
-- Base64 编码和解码
SELECT
    TO_BASE64('hello mysql') AS base64_text,
    FROM_BASE64(TO_BASE64('hello mysql')) AS source_text;
```

生成 UUID：

```sql
-- 生成 UUID
SELECT
    UUID() AS uuid_value,
    REPLACE(UUID(), '-', '') AS uuid_without_dash;
```

生成业务请求号示例：

```sql
-- 生成简单请求号：REQ + 日期 + UUID片段
SELECT
    CONCAT(
        'REQ',
        DATE_FORMAT(NOW(), '%Y%m%d'),
        UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16))
    ) AS request_no;
```

文件哈希表示例：

```sql
-- 文件哈希记录表
CREATE TABLE IF NOT EXISTS file_hash_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    sha256_hash CHAR(64) NOT NULL COMMENT 'SHA-256哈希值',
    file_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sha256_hash (sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件哈希记录表';
```

插入文件哈希示例：

```sql
-- 插入文件哈希记录
INSERT INTO file_hash_record (
    file_name,
    sha256_hash,
    file_size
) VALUES (
    'demo.txt',
    SHA2('demo-file-content', 256),
    1024
);
```

安全使用建议：

1. 密码不要直接使用 `MD5(password)` 或 `SHA2(password, 256)` 存储。
2. 密码存储应由应用层使用专业密码哈希算法，例如 BCrypt、Argon2、PBKDF2。
3. `MD5` 和 `SHA1` 不适合安全敏感场景。
4. `SHA2` 可用于文件摘要、数据校验、幂等键生成等非密码场景。
5. Base64 是编码，不是加密，不能用于保护敏感信息。
6. 敏感字段加密建议在应用层或专用加密组件中完成，并做好密钥管理。

### 系统信息函数

系统信息函数用于查看当前连接、当前用户、当前数据库、当前版本、连接 ID、服务端配置等信息。它们常用于排查连接问题、权限问题、环境问题和 SQL 执行上下文。

常用系统信息函数如下：

| 函数               | 说明                               |
| ------------------ | ---------------------------------- |
| `VERSION()`        | MySQL 服务端版本                   |
| `DATABASE()`       | 当前数据库                         |
| `USER()`           | 当前连接用户                       |
| `CURRENT_USER()`   | 当前认证用户                       |
| `CONNECTION_ID()`  | 当前连接 ID                        |
| `LAST_INSERT_ID()` | 当前连接最近生成的自增 ID          |
| `ROW_COUNT()`      | 上一条 DML 影响行数                |
| `FOUND_ROWS()`     | 配合特定语法使用，现代项目较少推荐 |
| `@@变量名`         | 查看系统变量或会话变量             |

查看基础环境信息：

```sql
-- 查看当前连接和服务端基础信息
SELECT
    VERSION() AS mysql_version,
    DATABASE() AS current_database,
    USER() AS login_user,
    CURRENT_USER() AS auth_user,
    CONNECTION_ID() AS connection_id;
```

查看最近插入的自增 ID：

```sql
-- 插入数据后查看当前连接最近生成的自增ID
INSERT INTO sys_user (
    username,
    nickname,
    status
) VALUES (
    'system_func_user',
    '系统函数用户',
    1
);

SELECT LAST_INSERT_ID() AS last_insert_id;
```

查看上一条 DML 影响行数：

```sql
-- 更新后查看影响行数
UPDATE sys_user
SET status = 0
WHERE username = 'system_func_user';

SELECT ROW_COUNT() AS affected_rows;
```

查看系统变量：

```sql
-- 查看字符集、时区、最大连接数等变量
SELECT
    @@version AS version_value,
    @@character_set_server AS character_set_server,
    @@collation_server AS collation_server,
    @@global.time_zone AS global_time_zone,
    @@session.time_zone AS session_time_zone,
    @@max_connections AS max_connections;
```

查看当前事务隔离级别：

```sql
-- 查看当前会话事务隔离级别
SELECT
    @@transaction_isolation AS transaction_isolation;
```

查看当前 SQL 模式：

```sql
-- 查看当前 SQL 模式
SELECT
    @@sql_mode AS sql_mode;
```

系统信息函数使用建议：

1. 排查环境问题时，先查看 `VERSION()`、`DATABASE()`、`USER()`、`CURRENT_USER()`。
2. 权限异常时，重点对比 `USER()` 和 `CURRENT_USER()`。
3. 插入后获取自增 ID 使用 `LAST_INSERT_ID()`，它基于当前连接，通常不会被其他连接影响。
4. 执行更新或删除后可用 `ROW_COUNT()` 判断影响行数。
5. 配置排查可通过 `@@变量名` 或 `SHOW VARIABLES` 查看。
6. 生产问题排查时，应记录连接用户、数据库名、版本、时区和 SQL 模式，便于复现。


## 索引基础

索引用于提升查询效率，本质上是数据库为字段或表达式维护的一种数据结构。MySQL InnoDB 中常见索引基于 B+Tree 实现，适合等值查询、范围查询、排序、分组和关联查询。索引并不是越多越好，它会提升查询性能，但也会增加写入、更新、删除和存储成本。

本节示例默认使用以下订单表：

```sql
-- 订单表示例，用于演示不同类型索引
CREATE TABLE IF NOT EXISTS order_index_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    receiver_mobile VARCHAR(20) DEFAULT NULL COMMENT '收货人手机号',
    receiver_address VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_status_created_at (order_status, created_at),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单索引示例表';
```

### 主键索引

主键索引用于唯一标识表中的每一行数据。InnoDB 表中，主键索引也是聚簇索引，数据行会按照主键索引组织存储。每张表只能有一个主键，主键字段必须唯一且不能为 `NULL`。

创建主键索引：

```sql
-- 创建表时定义主键索引
CREATE TABLE IF NOT EXISTS sys_user_pk_demo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主键索引示例表';
```

为已有表添加主键：

```sql
-- 为已有表添加主键索引
ALTER TABLE sys_user_pk_demo
  ADD PRIMARY KEY (id);
```

按主键查询：

```sql
-- 主键查询，通常是最高效的单行查询方式
SELECT
    id,
    username,
    nickname,
    created_at
FROM sys_user_pk_demo
WHERE id = 10001;
```

主键索引设计建议：

1. 每张业务表都应设计主键。
2. InnoDB 表推荐使用短、稳定、递增或趋势递增的主键。
3. 常见选择是 `BIGINT AUTO_INCREMENT` 或分布式雪花 ID。
4. 不建议使用手机号、身份证号、订单号等业务字段作为主键。
5. 主键字段不应频繁更新。
6. 主键过大会导致所有二级索引变大，因为 InnoDB 二级索引叶子节点会存储主键值。

### 唯一索引

唯一索引用于保证字段或字段组合的值唯一。它既能提升查询性能，也能在数据库层防止重复数据。常用于订单号、用户名、手机号、邮箱、支付流水号、业务编码等字段。

创建唯一索引：

```sql
-- 创建订单表时定义订单号唯一索引
CREATE TABLE IF NOT EXISTS order_unique_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单唯一索引示例表';
```

为已有表添加唯一索引：

```sql
-- 为订单号添加唯一索引
ALTER TABLE order_unique_demo
  ADD UNIQUE KEY uk_order_no (order_no);
```

创建联合唯一索引：

```sql
-- 同一租户下订单号唯一
ALTER TABLE order_unique_demo
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  ADD UNIQUE KEY uk_tenant_order_no (tenant_id, order_no);
```

按唯一索引查询：

```sql
-- 根据订单号查询订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_unique_demo
WHERE order_no = 'O202601010001';
```

唯一索引使用建议：

1. 有天然唯一约束的业务字段应建立唯一索引。
2. 分租户系统通常使用 `(tenant_id, biz_code)` 作为联合唯一索引。
3. 添加唯一索引前必须先检查历史重复数据。
4. 唯一索引允许多个 `NULL` 值，需要根据业务决定字段是否 `NOT NULL`。
5. 唯一索引可用于幂等写入，例如 `INSERT ... ON DUPLICATE KEY UPDATE`。

检查重复数据：

```sql
-- 添加唯一索引前检查订单号是否重复
SELECT
    order_no,
    COUNT(*) AS count_num
FROM order_unique_demo
GROUP BY order_no
HAVING COUNT(*) > 1;
```

### 普通索引

普通索引用于提升查询性能，不限制字段值是否重复。它适合高频查询条件、排序字段、关联字段等场景。

创建普通索引：

```sql
-- 为用户ID创建普通索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_id (user_id);
```

按普通索引字段查询：

```sql
-- 根据用户ID查询订单
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001;
```

为状态字段创建普通索引：

```sql
-- 为订单状态创建普通索引
ALTER TABLE order_index_demo
  ADD KEY idx_order_status (order_status);
```

普通索引使用建议：

1. 高频查询字段适合创建普通索引。
2. 关联字段适合创建普通索引，例如 `user_id`、`order_id`、`product_id`。
3. 选择性很低的字段单独建索引效果可能有限，例如只有 0 和 1 的 `deleted` 字段。
4. 普通索引不保证唯一性，防重复应使用唯一索引。
5. 不要为所有字段都建立索引，索引会增加写入和维护成本。

### 联合索引

联合索引是由多个字段组成的索引，适合多条件查询、联合排序、覆盖索引等场景。联合索引设计要特别关注字段顺序，因为 MySQL 使用联合索引时遵循最左前缀原则。

创建联合索引：

```sql
-- 为用户ID和创建时间创建联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_id_created_at (user_id, created_at);
```

适合该联合索引的查询：

```sql
-- 命中联合索引 user_id + created_at
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
ORDER BY created_at DESC;
```

创建状态和时间联合索引：

```sql
-- 为订单状态和创建时间创建联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_status_created_at (order_status, created_at);
```

适合后台列表查询：

```sql
-- 查询指定状态下的订单列表
SELECT
    id,
    order_no,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
ORDER BY created_at DESC
LIMIT 20;
```

联合索引使用建议：

1. 多条件查询优先考虑联合索引，而不是多个单列索引堆叠。
2. 联合索引字段顺序应结合等值条件、范围条件、排序字段设计。
3. 常见顺序是：等值条件字段在前，范围字段和排序字段在后。
4. 联合索引可以支持最左前缀查询。
5. 联合索引字段过多会增加维护成本和存储空间，应控制长度。

### 前缀索引

前缀索引用于只对字符串字段的前 N 个字符建立索引，适合较长的 `VARCHAR`、`TEXT` 字段。它可以减少索引长度和存储空间，但不能完全替代完整字段索引。

创建前缀索引：

```sql
-- 对收货地址前 32 个字符创建前缀索引
ALTER TABLE order_index_demo
  ADD KEY idx_receiver_address_prefix (receiver_address(32));
```

按地址前缀查询：

```sql
-- 地址前缀匹配更容易利用前缀索引
SELECT
    id,
    order_no,
    receiver_address
FROM order_index_demo
WHERE receiver_address LIKE '浙江省杭州市%';
```

计算合适的前缀长度：

```sql
-- 对比不同前缀长度的区分度
SELECT
    COUNT(DISTINCT receiver_address) AS full_distinct_count,
    COUNT(DISTINCT LEFT(receiver_address, 8)) AS prefix_8_distinct_count,
    COUNT(DISTINCT LEFT(receiver_address, 16)) AS prefix_16_distinct_count,
    COUNT(DISTINCT LEFT(receiver_address, 32)) AS prefix_32_distinct_count
FROM order_index_demo
WHERE receiver_address IS NOT NULL;
```

前缀索引使用建议：

1. 适合较长字符串字段，例如地址、URL、长编码、长备注的前缀查询。
2. 前缀长度要兼顾区分度和索引大小。
3. 前缀索引不能用于完整字段的精确覆盖。
4. 前缀索引对 `ORDER BY 完整字段` 的支持有限。
5. 如果字段需要唯一约束，应谨慎使用前缀唯一索引，避免前缀相同导致误判冲突。

### 全文索引

全文索引用于文本检索，适合对文章标题、正文、描述等字段进行关键词查询。MySQL 支持 `FULLTEXT` 索引和 `MATCH ... AGAINST` 查询，但中文分词能力有限，复杂搜索场景通常更适合 Elasticsearch、OpenSearch 等搜索引擎。

创建全文索引：

```sql
-- 创建文章表并添加全文索引
CREATE TABLE IF NOT EXISTS article_fulltext_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT NOT NULL COMMENT '文章正文',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1发布',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FULLTEXT KEY ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章全文索引示例表';
```

全文检索查询：

```sql
-- 使用自然语言模式全文检索
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('MySQL 索引') AS score
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('MySQL 索引')
ORDER BY score DESC;
```

布尔模式查询：

```sql
-- 使用布尔模式检索，要求包含 MySQL，最好包含 索引
SELECT
    id,
    title,
    content
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('+MySQL 索引' IN BOOLEAN MODE);
```

全文索引使用建议：

1. 适合文本检索，不适合替代普通 B+Tree 索引。
2. 中文全文检索效果受分词能力影响，需要测试验证。
3. 简单关键词检索可以使用 MySQL 全文索引。
4. 复杂搜索、相关性排序、高亮、拼音、同义词建议使用搜索引擎。
5. 全文索引会增加写入成本，文章类大表应评估维护成本。

### 空间索引

空间索引用于地理空间数据类型，例如 `POINT`、`LINESTRING`、`POLYGON` 等。常见场景包括门店位置、配送范围、地理围栏、地图检索等。

创建空间字段和空间索引：

```sql
-- 门店位置表，使用 POINT 存储经纬度
CREATE TABLE IF NOT EXISTS store_location_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    store_name VARCHAR(100) NOT NULL COMMENT '门店名称',
    location POINT NOT NULL SRID 4326 COMMENT '门店坐标，经纬度',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    SPATIAL INDEX idx_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店位置空间索引示例表';
```

插入空间数据：

```sql
-- 插入门店经纬度，POINT 参数顺序通常为经度、纬度
INSERT INTO store_location_demo (
    store_name,
    location
) VALUES (
    '杭州西湖店',
    ST_SRID(POINT(120.1551, 30.2741), 4326)
);
```

查询坐标信息：

```sql
-- 查询经纬度
SELECT
    id,
    store_name,
    ST_X(location) AS longitude,
    ST_Y(location) AS latitude
FROM store_location_demo;
```

空间索引使用建议：

1. 适合地理位置和空间范围查询。
2. 坐标字段建议使用合适的 SRID，例如 WGS84 常见为 `4326`。
3. 经纬度顺序要统一，通常 `POINT(longitude, latitude)`。
4. 简单距离排序也可以使用经纬度字段配合业务计算，但空间索引更适合空间关系查询。
5. 地图业务复杂时，应结合专业 GIS 能力或地图服务。

### 函数索引

函数索引是 MySQL 8 支持的表达式索引，允许基于表达式或函数结果建立索引。它适合优化函数计算结果查询，例如小写邮箱、JSON 路径、日期表达式等。

为小写邮箱创建函数索引：

```sql
-- 用户邮箱表
CREATE TABLE IF NOT EXISTS user_email_func_index_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    email VARCHAR(128) NOT NULL COMMENT '邮箱',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数索引邮箱示例表';

-- 创建基于 LOWER(email) 的函数索引
CREATE INDEX idx_lower_email
ON user_email_func_index_demo ((LOWER(email)));
```

使用函数索引查询：

```sql
-- 查询表达式应与函数索引表达式一致
SELECT
    id,
    email,
    nickname
FROM user_email_func_index_demo
WHERE LOWER(email) = LOWER('Admin@Example.com');
```

为 JSON 字段路径创建函数索引：

```sql
-- 为 JSON 中的 color 属性创建函数索引
CREATE INDEX idx_json_color
ON product_json_demo ((CAST(extend_attr ->> '$.color' AS CHAR(50))));
```

使用 JSON 函数索引查询：

```sql
-- 使用与函数索引一致的表达式查询
SELECT
    id,
    product_code,
    product_name
FROM product_json_demo
WHERE CAST(extend_attr ->> '$.color' AS CHAR(50)) = '黑色';
```

函数索引使用建议：

1. 适合无法直接使用普通索引的表达式查询。
2. 查询条件表达式应与函数索引定义保持一致。
3. 函数索引会增加写入维护成本。
4. 对稳定、高频的表达式查询再考虑函数索引。
5. JSON 高频路径查询也可以使用生成列索引，通常更直观。

### 降序索引

降序索引用于按照降序存储索引字段，适合优化倒序排序查询。MySQL 8 支持真正的降序索引，在时间倒序列表、排行榜、最新记录查询中较常见。

创建降序索引：

```sql
-- 创建用户ID和创建时间降序联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_created_desc (user_id, created_at DESC, id DESC);
```

适合查询用户最新订单：

```sql
-- 查询用户最新订单列表
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

创建状态和时间降序索引：

```sql
-- 创建订单状态和创建时间降序索引
ALTER TABLE order_index_demo
  ADD KEY idx_status_created_desc (order_status, created_at DESC, id DESC);
```

适合后台订单列表：

```sql
-- 查询指定状态下的最新订单
SELECT
    id,
    order_no,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE order_status = 1
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

降序索引使用建议：

1. 适合高频倒序分页列表。
2. 排序方向应与查询中的 `ORDER BY` 保持一致。
3. 常见组合是等值过滤字段在前，倒序时间和主键在后。
4. 如果排序字段混合升序和降序，降序索引价值更明显。
5. 是否消除 `Using filesort` 应通过 `EXPLAIN` 验证。

### 隐藏索引

隐藏索引是 MySQL 8 提供的索引可见性控制能力。被隐藏的索引仍然存在并继续维护，但优化器不会使用它。它适合在删除索引前进行风险评估。

创建隐藏索引：

```sql
-- 创建隐藏索引
ALTER TABLE order_index_demo
  ADD INDEX idx_receiver_mobile_hidden (receiver_mobile) INVISIBLE;
```

将已有索引设置为隐藏：

```sql
-- 将索引设置为隐藏，优化器默认不再使用该索引
ALTER TABLE order_index_demo
  ALTER INDEX idx_product_id INVISIBLE;
```

恢复索引可见：

```sql
-- 将隐藏索引恢复为可见
ALTER TABLE order_index_demo
  ALTER INDEX idx_product_id VISIBLE;
```

查看索引可见性：

```sql
-- 查看表索引可见性
SELECT
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE,
    IS_VISIBLE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_index_demo'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

隐藏索引使用建议：

1. 删除索引前，可以先设置为隐藏观察影响。
2. 隐藏索引仍然会被写入维护，不会降低写入维护成本。
3. 如果隐藏后业务 SQL 性能无异常，再考虑真正删除。
4. 主键索引不能隐藏。
5. 隐藏索引适合生产环境索引治理和风险控制。

## 索引设计与优化

索引设计的目标不是“给所有查询都加索引”，而是用最少、最有效的索引覆盖核心查询路径。设计索引时应从业务 SQL 出发，分析查询条件、排序字段、分组字段、关联字段、返回字段和数据分布，再结合 `EXPLAIN` 验证效果。

### 最左前缀原则

最左前缀原则是联合索引使用的核心规则。对于联合索引 `(a, b, c)`，查询必须从最左侧字段 `a` 开始连续使用，才能较好利用索引。

创建联合索引：

```sql
-- 创建联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_status_created (user_id, order_status, created_at);
```

可以较好利用索引的查询：

```sql
-- 使用 user_id
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE user_id = 10001;

-- 使用 user_id + order_status
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1;

-- 使用 user_id + order_status + created_at
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1
  AND created_at >= '2026-01-01 00:00:00';
```

不能充分利用该联合索引的查询：

```sql
-- 缺少最左字段 user_id，无法充分利用 idx_user_status_created
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE order_status = 1;

-- 缺少 user_id 和 order_status，无法充分利用 idx_user_status_created
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE created_at >= '2026-01-01 00:00:00';
```

最左前缀使用建议：

1. 联合索引字段顺序非常重要。
2. 查询条件应尽量从联合索引最左字段开始。
3. `(a, b, c)` 可以支持 `a`、`a,b`、`a,b,c` 的前缀查询。
4. 如果常按 `b` 单独查询，需要为 `b` 单独建索引或调整索引顺序。
5. 联合索引不是字段越多越好，要基于核心查询路径设计。

### 覆盖索引

覆盖索引指查询需要的字段都能从索引中直接获取，不需要再回表读取完整数据行。覆盖索引可以减少随机 IO，提升查询性能。

创建覆盖索引：

```sql
-- 为用户订单列表创建覆盖索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_created_cover (user_id, created_at, id, order_no, pay_amount);
```

覆盖索引查询：

```sql
-- 查询字段都包含在 idx_user_created_cover 中，有机会形成覆盖索引
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

非覆盖索引查询：

```sql
-- receiver_address 不在索引中，需要回表
SELECT
    id,
    order_no,
    pay_amount,
    receiver_address,
    created_at
FROM order_index_demo
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

查看是否覆盖索引：

```sql
-- Extra 中如果出现 Using index，通常表示使用了覆盖索引
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

覆盖索引设计建议：

1. 高频列表查询可以考虑覆盖索引。
2. 覆盖索引不是把所有返回字段都塞进索引，字段过多会让索引膨胀。
3. 大字段不适合放入覆盖索引，例如 `TEXT`、长 `VARCHAR`。
4. 优先覆盖高频、轻量、分页列表查询。
5. 是否命中覆盖索引应通过 `EXPLAIN` 的 `Extra` 验证。

### 回表查询

回表查询是指使用二级索引定位到主键后，再根据主键回到聚簇索引读取完整数据行。回表本身不是错误，但大量回表会影响性能。

基于二级索引查询：

```sql
-- 根据用户ID查询订单，可能先走 user_id 索引，再回表读取其他字段
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    receiver_address,
    created_at
FROM order_index_demo
WHERE user_id = 10001;
```

减少回表的方式之一是只查询索引字段：

```sql
-- 只查询索引中包含的字段，有机会减少回表
SELECT
    id,
    user_id,
    created_at
FROM order_index_demo
WHERE user_id = 10001;
```

使用覆盖索引减少回表：

```sql
-- 增加覆盖索引，减少高频列表回表
ALTER TABLE order_index_demo
  ADD KEY idx_user_order_cover (user_id, created_at, id, order_no, pay_amount);
```

回表查询优化建议：

1. 回表不是一定需要优化，只有在高频、大量回表时才需要重点处理。
2. 高频列表查询应避免 `SELECT *`。
3. 可以通过覆盖索引减少回表。
4. 查询字段越多，越难做到覆盖索引。
5. 大字段回表成本更高，应避免在列表页直接查询大字段。

### 索引下推

索引下推是 MySQL 优化器的一种能力，英文是 Index Condition Pushdown，简称 ICP。它可以在存储引擎层利用索引中的字段先过滤一部分数据，减少回表次数。

创建联合索引：

```sql
-- 创建收货手机号和订单号联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_mobile_order_no (receiver_mobile, order_no);
```

可能触发索引下推的查询：

```sql
-- receiver_mobile 使用索引定位，order_no 条件可在索引层继续过滤
EXPLAIN
SELECT
    id,
    order_no,
    receiver_mobile,
    pay_amount
FROM order_index_demo
WHERE receiver_mobile LIKE '138%'
  AND order_no LIKE 'O2026%';
```

查看执行计划：

```sql
-- Extra 中出现 Using index condition，表示可能使用索引下推
EXPLAIN
SELECT
    id,
    order_no,
    receiver_mobile,
    pay_amount
FROM order_index_demo
WHERE receiver_mobile LIKE '138%'
  AND order_no LIKE 'O2026%';
```

索引下推理解要点：

1. 索引下推可以减少回表次数。
2. 它依赖索引中已有字段做进一步过滤。
3. `Extra` 中可能显示 `Using index condition`。
4. 索引下推不能替代合理索引设计。
5. 能否使用由优化器判断，不需要手动开启 SQL 语法。

### 索引选择性

索引选择性表示某个字段的不同值数量与总行数的比例。选择性越高，索引过滤效果通常越好。唯一字段选择性最高，布尔字段选择性通常很低。

计算字段选择性：

```sql
-- 计算订单状态、用户ID、订单号的选择性
SELECT
    COUNT(*) AS total_count,
    COUNT(DISTINCT order_status) AS status_distinct_count,
    COUNT(DISTINCT user_id) AS user_distinct_count,
    COUNT(DISTINCT order_no) AS order_no_distinct_count,
    ROUND(COUNT(DISTINCT order_status) / COUNT(*), 6) AS status_selectivity,
    ROUND(COUNT(DISTINCT user_id) / COUNT(*), 6) AS user_selectivity,
    ROUND(COUNT(DISTINCT order_no) / COUNT(*), 6) AS order_no_selectivity
FROM order_index_demo;
```

低选择性字段示例：

```sql
-- deleted 字段通常只有 0 和 1，单独建索引选择性较低
SELECT
    id,
    order_no
FROM order_index_demo
WHERE deleted = 0;
```

将低选择性字段放入联合索引：

```sql
-- deleted 可与其他高频条件组成联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_deleted_created (user_id, deleted, created_at);
```

适合查询：

```sql
-- user_id 选择性较高，deleted 辅助过滤，created_at 支持范围和排序
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

索引选择性建议：

1. 唯一字段、订单号、用户 ID 等选择性通常较高。
2. 状态、性别、逻辑删除等字段选择性通常较低。
3. 低选择性字段不一定不能建索引，但单独建索引价值有限。
4. 低选择性字段可放入联合索引中辅助过滤。
5. 索引是否有效最终要结合数据分布和执行计划判断。

### 联合索引字段顺序

联合索引字段顺序决定了索引能支持哪些查询。常见设计原则是：等值查询字段在前，范围查询字段在后，排序字段尽量与索引顺序一致。

后台订单列表查询：

```sql
-- 典型查询：按用户、状态、时间范围查询订单
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
ORDER BY created_at DESC
LIMIT 20;
```

推荐索引：

```sql
-- 等值字段 user_id、order_status 在前，范围和排序字段 created_at 在后
ALTER TABLE order_index_demo
  ADD KEY idx_user_status_created (user_id, order_status, created_at DESC);
```

按状态查询最新订单：

```sql
-- 典型查询：按状态查询最新订单
SELECT
    id,
    order_no,
    order_status,
    created_at
FROM order_index_demo
WHERE order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

推荐索引：

```sql
-- 状态字段在前，时间倒序字段在后
ALTER TABLE order_index_demo
  ADD KEY idx_status_created_desc (order_status, created_at DESC, id DESC);
```

字段顺序设计建议：

| 查询模式                                                    | 推荐索引                             |
| ----------------------------------------------------------- | ------------------------------------ |
| `WHERE user_id = ? ORDER BY created_at DESC`                | `(user_id, created_at DESC)`         |
| `WHERE user_id = ? AND status = ? ORDER BY created_at DESC` | `(user_id, status, created_at DESC)` |
| `WHERE status = ? AND created_at BETWEEN ? AND ?`           | `(status, created_at)`               |
| `WHERE order_no = ?`                                        | 唯一索引 `(order_no)`                |
| `WHERE tenant_id = ? AND order_no = ?`                      | 唯一索引 `(tenant_id, order_no)`     |
| `JOIN ON order_id`                                          | `(order_id)`                         |

设计原则：

1. 等值条件字段通常放前面。
2. 范围条件字段通常放后面。
3. 排序字段尽量接在过滤字段后面。
4. 高频查询优先，低频查询不应过度牺牲索引结构。
5. 不同查询差异较大时，可能需要多个索引，但要控制数量。

### 索引失效场景

索引失效是指 SQL 虽然存在索引，但优化器没有使用，或无法充分使用。常见原因包括对索引字段使用函数、隐式类型转换、左模糊匹配、联合索引不满足最左前缀、范围条件后字段无法继续充分利用等。

对索引字段使用函数：

```sql
-- 不推荐：对 created_at 使用 DATE 函数，可能导致索引失效
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE DATE(created_at) = '2026-01-01';
```

推荐写法：

```sql
-- 推荐：使用时间范围查询
SELECT
    id,
    order_no,
    created_at
FROM order_index_demo
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00';
```

隐式类型转换：

```sql
-- 不推荐：order_no 是字符串字段，条件使用数字可能触发隐式转换
SELECT
    id,
    order_no
FROM order_index_demo
WHERE order_no = 202601010001;
```

推荐写法：

```sql
-- 推荐：字符串字段使用字符串条件
SELECT
    id,
    order_no
FROM order_index_demo
WHERE order_no = '202601010001';
```

左模糊匹配：

```sql
-- 不推荐：左侧通配符通常无法利用普通 B+Tree 索引
SELECT
    id,
    order_no,
    receiver_address
FROM order_index_demo
WHERE receiver_address LIKE '%杭州市%';
```

推荐写法：

```sql
-- 推荐：前缀匹配更容易利用索引
SELECT
    id,
    order_no,
    receiver_address
FROM order_index_demo
WHERE receiver_address LIKE '浙江省杭州市%';
```

联合索引不满足最左前缀：

```sql
-- 假设索引为 (user_id, order_status, created_at)，该查询缺少 user_id
SELECT
    id,
    order_no
FROM order_index_demo
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00';
```

常见索引失效场景：

| 场景                 | 示例                  | 优化方式                   |
| -------------------- | --------------------- | -------------------------- |
| 函数作用于索引字段   | `DATE(created_at)`    | 改为范围查询               |
| 隐式类型转换         | 字符串字段用数字比较  | 条件类型与字段一致         |
| 左模糊匹配           | `LIKE '%abc'`         | 改前缀匹配或全文索引       |
| 联合索引缺少最左字段 | 索引 `(a,b)` 查询 `b` | 调整索引或补充条件         |
| `OR` 条件复杂        | 不同字段 OR           | 拆分 UNION 或优化索引      |
| 不等于查询           | `status <> 1`         | 改写业务条件或接受扫描     |
| 低选择性字段         | `deleted = 0`         | 与高选择性字段组成联合索引 |
| 排序方向不匹配       | 混合排序              | 设计匹配排序方向的索引     |

索引失效排查建议：

1. 使用 `EXPLAIN` 查看实际执行计划。
2. 重点查看 `type`、`possible_keys`、`key`、`rows`、`Extra`。
3. 检查字段类型和条件值类型是否一致。
4. 检查联合索引是否满足最左前缀原则。
5. 检查是否对索引字段使用了函数或表达式。
6. 检查数据分布，低选择性字段可能被优化器放弃。

### 慢查询索引优化

慢查询索引优化应从真实慢 SQL 出发，不应凭感觉加索引。标准流程是：获取慢 SQL、确认执行频率、查看执行计划、分析过滤条件和排序字段、设计索引、验证效果、评估写入成本。

慢查询示例：

```sql
-- 慢查询示例：按用户、状态、时间排序分页
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

查看执行计划：

```sql
-- 查看慢 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

设计联合索引：

```sql
-- 针对查询条件和排序创建联合索引
ALTER TABLE order_index_demo
  ADD KEY idx_user_status_deleted_created (
      user_id,
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

优化后的查询：

```sql
-- 排序字段追加 id，保证分页顺序稳定
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

深分页慢查询优化：

```sql
-- 不推荐：深分页需要跳过大量数据
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 100000, 20;
```

使用游标分页优化：

```sql
-- 推荐：使用上一页最后一条记录的 created_at 和 id 作为游标
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_index_demo
WHERE user_id = 10001
  AND (
      created_at < '2026-01-10 12:00:00'
      OR (
          created_at = '2026-01-10 12:00:00'
          AND id < 900000
      )
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

慢查询索引优化流程：

1. 从慢查询日志、APM 或数据库监控中获取真实 SQL。
2. 确认 SQL 的调用频率、业务场景和数据量。
3. 使用 `EXPLAIN` 查看是否全表扫描、是否文件排序、扫描行数是否过大。
4. 根据 `WHERE`、`JOIN`、`ORDER BY`、`GROUP BY` 设计索引。
5. 在测试环境或影子环境验证执行计划和耗时。
6. 评估新增索引对写入、磁盘、备份和复制的影响。
7. 上线后观察慢查询、CPU、IO、锁等待和索引使用情况。

### 索引维护成本

索引会提升查询性能，但也会增加维护成本。每次插入、更新、删除数据时，MySQL 都需要维护相关索引；索引越多，写入成本越高，占用的磁盘和内存也越多。

查看表索引：

```sql
-- 查看指定表的索引
SHOW INDEX FROM order_index_demo;
```

查看索引大小：

```sql
-- 查看表数据和索引大小
SELECT
    TABLE_NAME,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_mb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS total_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_index_demo';
```

删除无用索引：

```sql
-- 删除确认无用的索引
ALTER TABLE order_index_demo
  DROP INDEX idx_receiver_address_prefix;
```

先隐藏索引观察：

```sql
-- 删除前先隐藏索引，降低直接删除风险
ALTER TABLE order_index_demo
  ALTER INDEX idx_product_id INVISIBLE;

-- 如果确认无影响，再删除索引
ALTER TABLE order_index_demo
  DROP INDEX idx_product_id;
```

索引维护成本主要体现在：

| 成本       | 说明                                     |
| ---------- | ---------------------------------------- |
| 写入成本   | 插入数据时需要写入所有相关索引           |
| 更新成本   | 更新索引字段时需要维护旧索引值和新索引值 |
| 删除成本   | 删除数据时需要删除索引记录               |
| 存储成本   | 索引占用磁盘空间                         |
| 内存成本   | 热点索引会占用 Buffer Pool               |
| 优化器成本 | 索引过多会增加优化器选择成本             |
| 变更成本   | 大表新增、删除索引可能耗时较长           |

索引维护建议：

1. 不要为低频查询随意创建索引。
2. 不要为每个字段都建单列索引。
3. 定期治理重复索引和无用索引。
4. 高频写入表应控制索引数量。
5. 大表新增索引前应评估执行时间、锁影响和磁盘空间。
6. 删除索引前优先使用隐藏索引观察一段时间。
7. 索引设计应服务核心 SQL，而不是追求数量。


## 执行计划分析

执行计划用于分析 MySQL 如何执行一条 SQL。通过 `EXPLAIN` 可以查看访问表的顺序、使用的索引、扫描行数、连接类型、过滤方式、是否使用临时表、是否发生文件排序等信息。SQL 优化不应只凭经验判断，应优先通过执行计划验证。

本节示例默认使用以下订单表：

```sql
-- 订单执行计划示例表
CREATE TABLE IF NOT EXISTS order_explain_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status_created (user_id, order_status, created_at),
    KEY idx_status_created (order_status, created_at),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单执行计划示例表';
```

### EXPLAIN 基础使用

`EXPLAIN` 用于查看 SQL 的执行计划。它不会真正返回业务查询结果，而是返回优化器选择的执行方式。常用于分析 `SELECT`，也可以分析部分 `UPDATE`、`DELETE`、`INSERT ... SELECT` 等语句。

基础用法：

```sql
-- 查看查询执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

常见输出字段：

| 字段            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| `id`            | 查询编号，复杂 SQL 中用于表示执行层级                        |
| `select_type`   | 查询类型，例如 SIMPLE、PRIMARY、SUBQUERY、DERIVED            |
| `table`         | 当前访问的表                                                 |
| `partitions`    | 使用的分区，未分区时通常为 NULL                              |
| `type`          | 访问类型，判断性能的重要字段                                 |
| `possible_keys` | 理论上可能使用的索引                                         |
| `key`           | 实际使用的索引                                               |
| `key_len`       | 使用索引的长度                                               |
| `ref`           | 与索引比较的列或常量                                         |
| `rows`          | 预估扫描行数                                                 |
| `filtered`      | 过滤后剩余比例估算                                           |
| `Extra`         | 额外执行信息，例如 Using where、Using index、Using temporary |

分析执行计划时，不要只看是否使用索引，还要结合 `type`、`key`、`rows`、`filtered` 和 `Extra` 综合判断。

常见分析顺序：

1. 看 `type`，判断访问方式是否合理。
2. 看 `possible_keys` 和 `key`，确认是否命中预期索引。
3. 看 `rows`，判断扫描行数是否过大。
4. 看 `Extra`，确认是否出现临时表、文件排序、回表、索引覆盖等情况。
5. 根据业务 SQL 调整索引、查询条件、排序字段或 SQL 结构。

### EXPLAIN ANALYZE

`EXPLAIN ANALYZE` 会实际执行 SQL，并返回执行过程中的真实耗时、循环次数、实际行数等信息。相比普通 `EXPLAIN` 的估算结果，`EXPLAIN ANALYZE` 更接近真实执行情况，适合定位复杂慢 SQL。

基础用法：

```sql
-- 实际执行 SQL 并分析执行耗时
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

关联查询分析示例：

```sql
-- 分析关联查询真实执行过程
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.pay_amount,
    o.created_at
FROM order_explain_demo AS o
JOIN product_info AS p ON o.product_id = p.id
WHERE o.order_status = 1
  AND p.status = 1
ORDER BY o.created_at DESC
LIMIT 20;
```

`EXPLAIN ANALYZE` 使用建议：

1. 它会真实执行 SQL，因此不要随意对生产库中的大查询使用。
2. 对 `UPDATE`、`DELETE` 等写操作要特别谨慎，避免实际修改数据。
3. 更适合在测试环境、预发环境或只读副本中使用。
4. 可用于验证普通 `EXPLAIN` 的估算是否偏差较大。
5. 当 `rows` 估算与实际行数差距很大时，可能需要更新统计信息或重新评估索引设计。

### type 字段分析

`type` 表示表的访问方式，是执行计划中最重要的字段之一。一般来说，从好到差大致为：`system`、`const`、`eq_ref`、`ref`、`range`、`index`、`ALL`。

常见 `type` 说明：

| type     | 说明                                       | 常见场景                  |
| -------- | ------------------------------------------ | ------------------------- |
| `system` | 表只有一行，是 `const` 的特殊情况          | 系统表或极小表            |
| `const`  | 通过主键或唯一索引一次定位一行             | `WHERE id = ?`            |
| `eq_ref` | 多表关联中，每次通过主键或唯一索引匹配一行 | 主键关联                  |
| `ref`    | 使用非唯一索引等值查询                     | `WHERE user_id = ?`       |
| `range`  | 使用索引范围扫描                           | `BETWEEN`、`>`、`<`、`IN` |
| `index`  | 扫描整个索引                               | 全索引扫描                |
| `ALL`    | 全表扫描                                   | 无合适索引或数据量较小    |

主键查询通常是 `const`：

```sql
-- 主键等值查询，通常 type 为 const
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount
FROM order_explain_demo
WHERE id = 10001;
```

唯一索引查询通常也是 `const`：

```sql
-- 唯一索引等值查询，通常 type 为 const
EXPLAIN
SELECT
    id,
    order_no,
    user_id
FROM order_explain_demo
WHERE order_no = 'O202601010001';
```

普通索引等值查询通常是 `ref`：

```sql
-- 普通索引等值查询，通常 type 为 ref
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    created_at
FROM order_explain_demo
WHERE user_id = 10001;
```

范围查询通常是 `range`：

```sql
-- 时间范围查询，通常 type 为 range
EXPLAIN
SELECT
    id,
    order_no,
    created_at
FROM order_explain_demo
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

全表扫描通常是 `ALL`：

```sql
-- 如果没有合适索引，可能出现 type = ALL
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount
FROM order_explain_demo
WHERE pay_amount > 1000.00;
```

`type` 分析建议：

1. 高频查询应尽量避免 `ALL`。
2. `index` 不一定好，它可能表示扫描整个索引。
3. `range` 对范围查询是正常结果。
4. `ref` 对普通索引等值查询通常可以接受。
5. 主键、唯一索引查询应尽量达到 `const`。
6. 不能只看 `type`，还要结合 `rows` 和 `Extra` 判断。

### key 字段分析

`key` 表示优化器实际选择使用的索引。`possible_keys` 表示理论上可能使用的索引，`key` 表示最终实际使用的索引。如果 `possible_keys` 有值但 `key` 为 `NULL`，说明优化器认为不使用索引更合适，或 SQL 写法导致索引不可用。

查看索引选择：

```sql
-- 查看优化器实际使用哪个索引
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

预期命中联合索引：

```sql
-- 该查询通常适合 idx_user_status_created
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
ORDER BY created_at DESC
LIMIT 20;
```

未命中预期索引的常见原因：

| 原因               | 示例                                   | 处理方式                   |
| ------------------ | -------------------------------------- | -------------------------- |
| 条件不满足最左前缀 | 索引 `(user_id,status)`，只查 `status` | 调整索引或补充条件         |
| 索引选择性低       | `deleted = 0`                          | 与高选择性字段组成联合索引 |
| 条件中使用函数     | `DATE(created_at)`                     | 改为范围查询               |
| 隐式类型转换       | 字符串字段用数字比较                   | 条件值类型与字段一致       |
| 返回数据比例过高   | 命中大量数据                           | 优化条件或接受扫描         |
| 统计信息偏差       | rows 估算异常                          | 更新统计信息               |

使用 `SHOW INDEX` 查看表索引：

```sql
-- 查看表中的索引
SHOW INDEX FROM order_explain_demo;
```

`key` 分析建议：

1. `key` 为 `NULL` 时，要重点检查是否全表扫描。
2. `key` 不一定是你预期的索引，优化器会根据成本选择。
3. 如果优化器选择不合理，先检查统计信息和索引设计。
4. 不建议长期依赖 `FORCE INDEX`，应优先调整索引和 SQL。
5. `possible_keys` 很多而 `key` 选择异常时，可能存在冗余索引或索引设计混乱。

### rows 字段分析

`rows` 表示优化器预估需要扫描的行数。它不是实际扫描行数，而是估算值。`rows` 越大，通常表示查询成本越高。优化慢 SQL 时，应重点关注 `rows` 是否远大于实际需要返回的行数。

查看扫描行数估算：

```sql
-- 查看 rows 估算值
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

如果 `rows` 很大，说明 MySQL 可能需要扫描大量满足或可能满足条件的数据。对于后台列表查询，返回 20 行但扫描几十万行通常需要优化。

优化前示例：

```sql
-- 如果只有 order_status 条件，低选择性字段可能导致扫描行数较大
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

增加匹配排序的联合索引：

```sql
-- 创建状态和时间倒序索引，降低排序和扫描成本
ALTER TABLE order_explain_demo
  ADD KEY idx_status_created_desc (order_status, created_at DESC, id DESC);
```

优化后查看：

```sql
-- 再次查看 rows 和 Extra 是否改善
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE order_status = 1
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

`rows` 分析建议：

1. `rows` 是估算值，不是绝对真实值。
2. `rows` 很大时，需要检查过滤条件和索引设计。
3. `rows × filtered` 可以粗略理解过滤后的数据规模。
4. `LIMIT 20` 不代表只扫描 20 行，特别是排序和过滤条件不匹配索引时。
5. 如果估算严重不准，可以考虑更新表统计信息。

更新统计信息：

```sql
-- 更新表统计信息，帮助优化器做出更准确判断
ANALYZE TABLE order_explain_demo;
```

### Extra 字段分析

`Extra` 表示执行计划中的额外信息，常用于判断是否使用覆盖索引、是否使用临时表、是否文件排序、是否使用索引条件下推、是否使用 where 过滤等。

常见 `Extra` 信息：

| Extra                   | 说明                                                   |
| ----------------------- | ------------------------------------------------------ |
| `Using where`           | 在服务层或存储引擎层进行条件过滤                       |
| `Using index`           | 使用覆盖索引，不需要回表                               |
| `Using index condition` | 使用索引条件下推                                       |
| `Using temporary`       | 使用临时表，常见于分组、排序、去重                     |
| `Using filesort`        | 使用文件排序，不一定真的落盘，但表示未完全利用索引排序 |
| `Using join buffer`     | 关联查询中使用 join buffer，可能关联字段缺少索引       |
| `Impossible WHERE`      | WHERE 条件不可能成立                                   |
| `Using MRR`             | 使用 Multi-Range Read 优化                             |

查看 `Extra`：

```sql
-- 查看 Extra 信息
EXPLAIN
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_explain_demo
WHERE order_status = 1
GROUP BY user_id
ORDER BY order_count DESC;
```

可能出现 `Using temporary` 和 `Using filesort`：

```sql
-- 分组后按聚合结果排序，可能使用临时表和文件排序
EXPLAIN
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_explain_demo
GROUP BY user_id
ORDER BY order_count DESC;
```

`Extra` 分析建议：

1. `Using index` 通常是好信号，表示覆盖索引。
2. `Using where` 很常见，不一定是问题。
3. `Using temporary` 和 `Using filesort` 在大数据量场景要重点关注。
4. `Using index condition` 表示可能使用索引下推，是优化行为。
5. `Using join buffer` 可能说明关联字段缺少合适索引。

### Using Index

`Using index` 表示查询使用了覆盖索引，所需字段都可以从索引中获取，不需要回表读取完整数据行。它通常能减少 IO，是较理想的执行方式之一。

创建覆盖索引：

```sql
-- 创建覆盖用户订单列表的索引
ALTER TABLE order_explain_demo
  ADD KEY idx_user_created_cover (user_id, created_at, id, order_no, pay_amount);
```

覆盖索引查询：

```sql
-- 查询字段都在索引中，可能出现 Using index
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

非覆盖查询：

```sql
-- deleted 不在该覆盖索引中时，可能需要回表
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount,
    deleted,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

`Using index` 使用建议：

1. 高频列表查询可以针对返回字段设计覆盖索引。
2. 覆盖索引不要包含过多字段，否则索引会膨胀。
3. 大字段不适合放入覆盖索引，例如长备注、正文、JSON 大字段。
4. 覆盖索引适合分页列表、轻量查询、只读统计。
5. 是否真的覆盖，应以 `EXPLAIN Extra` 中的 `Using index` 为准。

### Using Where

`Using where` 表示 MySQL 需要根据 `WHERE` 条件进一步过滤数据。它很常见，并不一定代表 SQL 有问题。需要结合 `type`、`key`、`rows` 判断过滤成本。

常见 `Using where` 示例：

```sql
-- 使用索引定位后仍然需要 WHERE 条件过滤
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount
FROM order_explain_demo
WHERE user_id = 10001
  AND deleted = 0;
```

如果 `type` 较好、`rows` 较小，`Using where` 可以接受。如果 `type = ALL` 且 `rows` 很大，同时出现 `Using where`，则可能是全表扫描后过滤。

全表扫描过滤示例：

```sql
-- 如果 pay_amount 没有索引，可能全表扫描后过滤
EXPLAIN
SELECT
    id,
    order_no,
    pay_amount
FROM order_explain_demo
WHERE pay_amount > 1000.00;
```

优化方式：

```sql
-- 如果该条件高频使用，可考虑增加索引
ALTER TABLE order_explain_demo
  ADD KEY idx_pay_amount (pay_amount);
```

`Using where` 分析建议：

1. `Using where` 本身不是坏结果。
2. 如果搭配合适索引和较小 `rows`，通常可以接受。
3. 如果搭配 `type = ALL` 和大 `rows`，需要重点优化。
4. 应优先优化高频、慢、扫描行数大的 SQL。
5. 不要为了消除 `Using where` 盲目增加索引。

### Using Temporary

`Using temporary` 表示 MySQL 执行过程中使用了临时表。它常见于 `GROUP BY`、`DISTINCT`、复杂排序、联合查询、派生表等场景。小数据量临时表可以接受，大数据量临时表可能造成明显性能问题。

可能出现临时表的分组查询：

```sql
-- 按用户分组并按订单数排序，可能使用临时表
EXPLAIN
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_explain_demo
WHERE order_status = 1
GROUP BY user_id
ORDER BY order_count DESC;
```

可能出现临时表的去重查询：

```sql
-- DISTINCT 去重可能使用临时表
EXPLAIN
SELECT DISTINCT
    user_id
FROM order_explain_demo
WHERE order_status = 1;
```

优化思路之一：让过滤和分组更贴近索引：

```sql
-- 为状态和用户创建联合索引，改善按状态过滤后按用户分组
ALTER TABLE order_explain_demo
  ADD KEY idx_status_user (order_status, user_id);
```

重新查看：

```sql
-- 查看是否减少扫描和临时表成本
EXPLAIN
SELECT
    user_id,
    COUNT(*) AS order_count
FROM order_explain_demo
WHERE order_status = 1
GROUP BY user_id;
```

`Using temporary` 使用建议：

1. 不是所有临时表都必须消除，小结果集可以接受。
2. 大表高频统计中出现临时表，应重点优化。
3. 可通过联合索引、汇总表、缩小时间范围减少临时表成本。
4. 按聚合结果排序通常较难完全避免临时表。
5. 报表类复杂统计建议使用预聚合表或 OLAP 组件。

### Using Filesort

`Using filesort` 表示 MySQL 需要额外排序，排序不一定真的使用磁盘文件，但说明没有完全通过索引顺序返回结果。大数据量排序时，`Using filesort` 可能成为性能瓶颈。

可能出现文件排序的查询：

```sql
-- 如果没有合适的 order_status + created_at 索引，可能出现 Using filesort
EXPLAIN
SELECT
    id,
    order_no,
    order_status,
    created_at
FROM order_explain_demo
WHERE order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

优化排序索引：

```sql
-- 创建匹配过滤和排序的联合索引
ALTER TABLE order_explain_demo
  ADD KEY idx_status_created_desc (order_status, created_at DESC, id DESC);
```

优化后查询：

```sql
-- ORDER BY 与索引顺序匹配，可能避免 filesort
EXPLAIN
SELECT
    id,
    order_no,
    order_status,
    created_at
FROM order_explain_demo
WHERE order_status = 1
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

多字段排序示例：

```sql
-- 查询用户订单并按创建时间倒序、主键倒序排序
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

推荐索引：

```sql
-- 匹配 user_id 过滤和 created_at、id 排序
ALTER TABLE order_explain_demo
  ADD KEY idx_user_created_id_desc (user_id, created_at DESC, id DESC);
```

`Using filesort` 使用建议：

1. 小数据量排序可以接受。
2. 大数据量分页排序应尽量通过索引优化。
3. `WHERE` 等值字段在前，`ORDER BY` 字段在后，是常见索引设计方式。
4. 分页查询建议追加主键排序，保证结果稳定。
5. 如果按聚合结果排序，通常难以完全避免文件排序，应考虑汇总表。

### 执行计划优化思路

执行计划优化应从业务 SQL 出发，不应机械追求某个字段“看起来好”。优化目标是降低扫描行数、减少回表、减少临时表和文件排序、提升响应时间，同时控制索引维护成本。

通用优化流程：

1. 获取慢 SQL，确认调用频率和业务场景。
2. 使用 `EXPLAIN` 查看执行计划。
3. 判断 `type` 是否过差，例如 `ALL`。
4. 判断 `key` 是否命中预期索引。
5. 判断 `rows` 是否过大。
6. 判断 `Extra` 是否存在高成本操作，例如 `Using temporary`、`Using filesort`。
7. 根据 `WHERE`、`JOIN`、`ORDER BY`、`GROUP BY` 设计或调整索引。
8. 使用测试数据验证执行时间和执行计划。
9. 评估新增索引对写入和存储的影响。
10. 上线后持续观察慢查询和数据库指标。

典型慢 SQL：

```sql
-- 慢 SQL：按用户、状态查询订单并分页
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

分析执行计划：

```sql
-- 查看执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_explain_demo
WHERE user_id = 10001
  AND order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

设计索引：

```sql
-- 根据等值条件、逻辑删除、排序字段创建联合索引
ALTER TABLE order_explain_demo
  ADD KEY idx_user_status_deleted_created (
      user_id,
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

如果需要进一步减少回表，可以设计覆盖索引：

```sql
-- 覆盖列表页常用字段，但要控制索引宽度
ALTER TABLE order_explain_demo
  ADD KEY idx_user_status_deleted_created_cover (
      user_id,
      order_status,
      deleted,
      created_at DESC,
      id DESC,
      order_no,
      pay_amount
  );
```

优化注意事项：

1. 不要为了一个低频 SQL 创建很重的索引。
2. 不要只优化单条 SQL，而忽略写入压力和整体索引数量。
3. 不要滥用 `FORCE INDEX`，它可能在数据分布变化后变成负优化。
4. 不要用 `SELECT *` 做列表查询。
5. 大表深分页优先考虑游标分页。
6. 复杂报表统计优先考虑汇总表或异步计算。

## 事务管理

事务用于保证一组数据库操作要么全部成功，要么全部失败。MySQL 中 InnoDB 存储引擎支持事务，常用于订单创建、库存扣减、账户余额变更、支付状态更新、审批流转等需要一致性的场景。

事务管理的核心不是简单地写 `START TRANSACTION` 和 `COMMIT`，而是合理设计事务边界、控制事务时长、处理异常回滚、避免大事务、减少锁冲突，并与业务幂等、状态机和唯一约束配合使用。

### 事务基本概念

事务是一组逻辑操作单元。事务中的多条 SQL 要么全部提交成功，要么全部回滚失败。它用于解决多个数据变更之间的一致性问题。

典型场景：创建订单和订单明细必须同时成功。

```sql
-- 订单主表
CREATE TABLE IF NOT EXISTS tx_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS tx_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    buy_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买数量',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售单价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务订单明细表';
```

事务插入示例：

```sql
-- 开启事务
START TRANSACTION;

-- 插入订单主表
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010001',
    10001,
    0,
    199.80
);

-- 获取当前连接最近插入的自增ID
SET @order_id = LAST_INSERT_ID();

-- 插入订单明细
INSERT INTO tx_order_item (
    order_id,
    product_id,
    product_name,
    buy_count,
    sale_price
) VALUES
    (@order_id, 20001, '基础款T恤', 2, 99.90);

-- 提交事务
COMMIT;
```

如果订单主表插入成功，但订单明细插入失败，就应回滚整个事务，避免出现没有明细的异常订单。

### ACID 特性

ACID 是事务的四个基本特性，分别是原子性、一致性、隔离性和持久性。

| 特性   | 英文        | 说明                                                   |
| ------ | ----------- | ------------------------------------------------------ |
| 原子性 | Atomicity   | 事务中的操作要么全部成功，要么全部失败                 |
| 一致性 | Consistency | 事务执行前后，数据必须从一个一致状态变为另一个一致状态 |
| 隔离性 | Isolation   | 多个事务并发执行时，彼此影响受隔离级别控制             |
| 持久性 | Durability  | 事务提交后，数据修改应持久保存                         |

订单支付场景示例：

```text
支付成功事务应保证：
1. 支付记录状态更新为成功
2. 订单状态更新为已支付
3. 账户流水写入成功
4. 库存锁定或扣减状态正确

以上操作必须保持一致，不能只成功一部分。
```

支付状态更新示例表：

```sql
-- 支付记录表
CREATE TABLE IF NOT EXISTS tx_payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务支付记录表';
```

支付事务示例：

```sql
-- 支付成功后更新订单和支付记录
START TRANSACTION;

UPDATE tx_payment_record
SET
    pay_status = 1,
    paid_at = NOW()
WHERE payment_no = 'P202601010001'
  AND pay_status = 0;

UPDATE tx_order_info
SET
    order_status = 1
WHERE order_no = 'T202601010001'
  AND order_status = 0;

COMMIT;
```

ACID 理解建议：

1. 原子性依赖提交和回滚机制。
2. 一致性依赖数据库约束、事务逻辑和业务规则共同保证。
3. 隔离性依赖事务隔离级别和锁机制。
4. 持久性依赖 redo log、刷盘策略和存储系统。
5. 应用代码中要正确处理异常，否则事务能力无法发挥作用。

### START TRANSACTION

`START TRANSACTION` 用于显式开启一个事务。开启后，后续 SQL 修改不会立即永久提交，直到执行 `COMMIT` 或 `ROLLBACK`。

基础语法：

```sql
-- 开启事务
START TRANSACTION;
```

等价写法：

```sql
-- BEGIN 也可以开启事务
BEGIN;
```

完整事务示例：

```sql
-- 开启事务
START TRANSACTION;

-- 插入订单
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010002',
    10002,
    0,
    299.00
);

-- 提交事务
COMMIT;
```

如果事务中途需要放弃变更：

```sql
-- 开启事务
START TRANSACTION;

-- 插入订单
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010003',
    10003,
    0,
    399.00
);

-- 放弃本事务中的修改
ROLLBACK;
```

开启事务注意事项：

1. 事务只对支持事务的存储引擎有效，InnoDB 支持事务。
2. 事务开启后应尽快提交或回滚，避免长事务。
3. 事务中不要执行耗时外部调用，例如 HTTP、RPC、文件上传等。
4. 事务中修改的数据可能持有锁，影响其他事务。
5. DDL 语句可能导致隐式提交，应避免与普通业务事务混用。

### COMMIT 提交事务

`COMMIT` 用于提交当前事务，使事务中的数据修改正式生效。提交后，其他事务可以根据隔离级别看到这些修改。

提交事务示例：

```sql
-- 开启事务
START TRANSACTION;

-- 修改订单状态
UPDATE tx_order_info
SET order_status = 1
WHERE order_no = 'T202601010001'
  AND order_status = 0;

-- 插入支付记录
INSERT INTO tx_payment_record (
    payment_no,
    order_no,
    pay_amount,
    pay_status,
    paid_at
) VALUES (
    'P202601010001',
    'T202601010001',
    199.80,
    1,
    NOW()
);

-- 提交事务
COMMIT;
```

提交后验证：

```sql
-- 查询订单状态
SELECT
    order_no,
    order_status
FROM tx_order_info
WHERE order_no = 'T202601010001';

-- 查询支付记录
SELECT
    payment_no,
    order_no,
    pay_status,
    paid_at
FROM tx_payment_record
WHERE payment_no = 'P202601010001';
```

`COMMIT` 使用建议：

1. 所有必要 SQL 执行成功后再提交。
2. 提交前应确认影响行数是否符合预期。
3. 如果状态更新影响行数为 0，可能表示重复处理或状态不匹配，应回滚或做幂等处理。
4. 事务不应保持太久，避免锁等待和 undo 日志膨胀。
5. 应用层通常由框架事务管理提交，例如 Spring `@Transactional`。

### ROLLBACK 回滚事务

`ROLLBACK` 用于回滚当前事务中尚未提交的修改。它常用于异常处理、业务校验失败、影响行数不符合预期等场景。

回滚示例：

```sql
-- 开启事务
START TRANSACTION;

-- 修改订单状态
UPDATE tx_order_info
SET order_status = 1
WHERE order_no = 'T202601010004'
  AND order_status = 0;

-- 发现业务校验失败，回滚事务
ROLLBACK;
```

库存扣减失败时回滚：

```sql
-- 库存表示例
CREATE TABLE IF NOT EXISTS tx_product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    stock_count INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务商品库存表';
-- 开启事务
START TRANSACTION;

-- 扣减库存，要求库存充足
UPDATE tx_product_stock
SET stock_count = stock_count - 2
WHERE product_id = 20001
  AND stock_count >= 2;

-- 应用层应检查 ROW_COUNT() 是否为 1
SELECT ROW_COUNT() AS affected_rows;

-- 如果 affected_rows = 0，表示库存不足，应执行 ROLLBACK
ROLLBACK;
```

正确的成功路径：

```sql
-- 开启事务
START TRANSACTION;

-- 扣减库存
UPDATE tx_product_stock
SET stock_count = stock_count - 2
WHERE product_id = 20001
  AND stock_count >= 2;

-- 插入订单
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010005',
    10005,
    0,
    199.80
);

-- 如果所有操作成功，提交事务
COMMIT;
```

`ROLLBACK` 使用建议：

1. 只会回滚当前事务中尚未提交的修改。
2. 已经 `COMMIT` 的事务不能通过普通 `ROLLBACK` 撤销。
3. 应用层捕获异常后应确保事务能回滚。
4. 业务校验失败、影响行数不符合预期时应回滚。
5. 回滚不能替代备份恢复，误提交的数据需要通过补偿、备份或 binlog 恢复。

### SAVEPOINT 保存点

`SAVEPOINT` 用于在事务中创建保存点，可以回滚到事务中的某个中间位置，而不是回滚整个事务。它适合复杂事务中部分步骤允许失败、但前置步骤需要保留的场景。

基础语法：

```sql
-- 创建保存点
SAVEPOINT savepoint_name;

-- 回滚到保存点
ROLLBACK TO SAVEPOINT savepoint_name;

-- 删除保存点
RELEASE SAVEPOINT savepoint_name;
```

保存点示例：

```sql
-- 开启事务
START TRANSACTION;

-- 插入订单主表
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010006',
    10006,
    0,
    299.00
);

-- 创建保存点
SAVEPOINT sp_order_created;

-- 插入支付记录
INSERT INTO tx_payment_record (
    payment_no,
    order_no,
    pay_amount,
    pay_status
) VALUES (
    'P202601010006',
    'T202601010006',
    299.00,
    0
);

-- 如果支付记录插入有问题，可回滚到保存点
ROLLBACK TO SAVEPOINT sp_order_created;

-- 提交事务，此时订单保留，支付记录回滚
COMMIT;
```

释放保存点：

```sql
-- 开启事务
START TRANSACTION;

-- 执行部分业务SQL
INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010007',
    10007,
    0,
    99.00
);

-- 创建保存点
SAVEPOINT sp_after_order;

-- 确认不再需要保存点后释放
RELEASE SAVEPOINT sp_after_order;

-- 提交事务
COMMIT;
```

保存点使用建议：

1. 保存点适合复杂事务中的局部回滚。
2. 普通业务事务通常不需要保存点，直接提交或整体回滚即可。
3. 回滚到保存点不会结束事务，仍需最终 `COMMIT` 或 `ROLLBACK`。
4. 保存点名称应表达业务含义。
5. 过度使用保存点会增加事务理解成本，应谨慎使用。

### 自动提交机制

MySQL 默认通常开启自动提交模式，即每条 SQL 都会作为一个独立事务自动提交。可以通过 `autocommit` 查看和修改当前会话的自动提交行为。

查看自动提交状态：

```sql
-- 查看当前会话自动提交状态
SELECT @@autocommit AS autocommit_value;
```

关闭当前会话自动提交：

```sql
-- 关闭自动提交，后续 DML 需要手动 COMMIT 或 ROLLBACK
SET autocommit = 0;
```

手动提交：

```sql
-- 修改数据
UPDATE tx_order_info
SET order_status = 2
WHERE order_no = 'T202601010001';

-- 手动提交
COMMIT;
```

手动回滚：

```sql
-- 修改数据
UPDATE tx_order_info
SET order_status = 2
WHERE order_no = 'T202601010002';

-- 放弃修改
ROLLBACK;
```

恢复自动提交：

```sql
-- 恢复自动提交
SET autocommit = 1;
```

自动提交与显式事务：

```sql
-- 即使 autocommit = 1，显式事务中仍由 START TRANSACTION 到 COMMIT 控制
START TRANSACTION;

UPDATE tx_order_info
SET order_status = 1
WHERE order_no = 'T202601010001';

COMMIT;
```

自动提交使用建议：

1. 开发和生产环境中不建议长期关闭自动提交而忘记提交。
2. 显式事务应使用 `START TRANSACTION`、`COMMIT`、`ROLLBACK` 管理。
3. 客户端工具中关闭自动提交后，要特别注意锁和长事务。
4. Spring 项目通常由事务管理器控制提交和回滚。
5. 排查锁等待时，应检查是否存在未提交事务。

查看当前运行事务：

```sql
-- 查看 InnoDB 当前事务信息
SELECT
    trx_id,
    trx_state,
    trx_started,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX;
```

### 事务边界设计

事务边界决定哪些操作应该放在同一个事务中。事务边界过小可能导致数据不一致，事务边界过大则可能造成长事务、锁等待、死锁、undo 日志膨胀和吞吐下降。

适合放在同一事务中的操作：

| 场景                    | 说明                       |
| ----------------------- | -------------------------- |
| 创建订单 + 订单明细     | 主表和明细必须一致         |
| 支付成功 + 订单状态更新 | 支付记录和订单状态必须一致 |
| 扣减库存 + 创建库存流水 | 库存数量和流水必须一致     |
| 账户余额变更 + 资金流水 | 余额和流水必须一致         |
| 审批状态变更 + 审批记录 | 状态和日志必须一致         |

不适合放在同一事务中的操作：

| 操作            | 原因                        |
| --------------- | --------------------------- |
| HTTP 接口调用   | 外部调用耗时不可控          |
| MQ 发送等待确认 | 可能拉长事务时间            |
| 文件上传        | IO 耗时长且不可由数据库回滚 |
| 大批量数据处理  | 容易形成大事务              |
| 用户交互等待    | 事务不能跨用户等待过程      |
| 复杂报表计算    | 长时间占用资源              |

合理事务示例：订单创建。

```sql
-- 订单创建事务：只包含必须一致的数据库写入
START TRANSACTION;

INSERT INTO tx_order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'T202601010008',
    10008,
    0,
    199.80
);

SET @order_id = LAST_INSERT_ID();

INSERT INTO tx_order_item (
    order_id,
    product_id,
    product_name,
    buy_count,
    sale_price
) VALUES
    (@order_id, 20001, '基础款T恤', 2, 99.90);

COMMIT;
```

不推荐事务示例：

```text
不推荐事务流程：

1. START TRANSACTION
2. 插入订单
3. 调用第三方支付接口
4. 上传文件
5. 发送 MQ
6. 更新订单状态
7. COMMIT

问题：
事务持有时间过长，外部调用不可控，容易造成锁等待和连接占用。
```

推荐拆分方式：

```text
推荐流程：

1. 本地事务中创建订单，状态为待支付
2. 提交事务
3. 事务外调用第三方支付
4. 支付回调后使用新的本地事务更新支付状态
5. 使用本地消息表或可靠消息机制发送后续事件
```

事务边界设计建议：

1. 一个事务只包含必须保持强一致的数据库操作。
2. 事务中不要做外部 RPC、HTTP、文件上传、长时间计算。
3. 事务应尽量短，减少锁持有时间。
4. 更新语句应使用明确条件，避免扩大锁范围。
5. 并发更新时应使用状态条件或版本号控制。
6. 大批量更新、删除应分批提交，避免大事务。
7. 跨系统一致性不应依赖单个数据库事务，应考虑本地消息表、最终一致性、幂等和补偿机制。

库存扣减事务设计示例：

```sql
-- 使用条件更新保证库存不扣成负数
START TRANSACTION;

UPDATE tx_product_stock
SET stock_count = stock_count - 1
WHERE product_id = 20001
  AND stock_count >= 1;

-- 应用层检查 ROW_COUNT()，如果为 0 则回滚
SELECT ROW_COUNT() AS affected_rows;

-- 插入库存流水
INSERT INTO inventory_record (
    product_id,
    change_type,
    change_count,
    before_count,
    after_count,
    biz_no,
    remark
) VALUES (
    20001,
    2,
    -1,
    100,
    99,
    'T202601010008',
    '订单扣减库存'
);

COMMIT;
```

应用层处理时，应在扣减库存后检查影响行数。如果影响行数为 `0`，说明库存不足或商品不存在，应立即回滚并返回业务失败。对于高并发库存场景，还需要结合唯一约束、幂等号、库存流水和重试策略。


## 隔离级别与锁

事务隔离级别用于控制多个事务并发执行时彼此可见的数据范围。锁用于控制并发读写冲突，保证数据一致性。MySQL InnoDB 同时依赖 MVCC 和锁机制：普通 `SELECT` 通常走 MVCC 快照读，`UPDATE`、`DELETE`、`SELECT ... FOR UPDATE`、`SELECT ... LOCK IN SHARE MODE` 等属于当前读，可能加锁。

本节示例默认使用以下账户表：

```sql
-- 账户余额表，用于演示隔离级别、锁和并发更新
CREATE TABLE IF NOT EXISTS account_lock_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    frozen_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    account_status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：0冻结，1正常',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户锁示例表';
```

### READ UNCOMMITTED

`READ UNCOMMITTED` 是最低的事务隔离级别。一个事务可以读取到另一个事务尚未提交的数据，因此可能发生脏读。该隔离级别一致性较弱，实际业务系统中很少使用。

查看当前隔离级别：

```sql
-- 查看当前会话事务隔离级别
SELECT @@transaction_isolation AS transaction_isolation;
```

设置当前会话为 `READ UNCOMMITTED`：

```sql
-- 设置当前会话事务隔离级别为 READ UNCOMMITTED
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
```

事务示例：

```sql
-- 会话A：开启事务并修改余额，但暂不提交
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount + 100.00
WHERE user_id = 10001;

-- 此时不要 COMMIT，保持事务未提交
```

另一个会话在 `READ UNCOMMITTED` 下可能读到未提交数据：

```sql
-- 会话B：READ UNCOMMITTED 下读取账户余额，可能读到会话A未提交的数据
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

`READ UNCOMMITTED` 使用建议：

1. 不建议用于核心业务系统。
2. 可能读取到未提交数据，导致业务判断错误。
3. 适合极少数只看趋势、不要求准确性的临时诊断场景。
4. 一般项目中更常用 `READ COMMITTED` 或 `REPEATABLE READ`。
5. 生产环境不建议为了减少锁等待而降低到该级别。

### READ COMMITTED

`READ COMMITTED` 表示一个事务只能读取到其他事务已经提交的数据。它可以避免脏读，但同一个事务中多次读取同一行数据，可能因为其他事务提交了修改而读到不同结果，因此可能发生不可重复读。

设置当前会话为 `READ COMMITTED`：

```sql
-- 设置当前会话事务隔离级别为 READ COMMITTED
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

读取已提交数据示例：

```sql
-- 会话A：开启事务并第一次查询余额
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

-- 保持事务不提交，等待会话B提交修改后再次查询
```

另一个会话提交修改：

```sql
-- 会话B：修改余额并提交
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount + 100.00
WHERE user_id = 10001;

COMMIT;
```

会话 A 再次查询，可能读到新提交数据：

```sql
-- 会话A：同一事务内再次查询，READ COMMITTED 下可能读到会话B已提交的新值
SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

`READ COMMITTED` 使用建议：

1. 能避免脏读。
2. 可能发生不可重复读。
3. 每次普通查询通常读取最新已提交版本。
4. 适合部分对并发可见性要求更实时的业务。
5. 使用该隔离级别时，应通过状态条件、唯一约束、乐观锁等方式保证业务一致性。

### REPEATABLE READ

`REPEATABLE READ` 表示同一事务内多次普通查询同一数据时，结果保持一致。它可以避免脏读和不可重复读。MySQL InnoDB 默认隔离级别通常是 `REPEATABLE READ`，并结合 MVCC、间隙锁和临键锁处理并发一致性问题。

设置当前会话为 `REPEATABLE READ`：

```sql
-- 设置当前会话事务隔离级别为 REPEATABLE READ
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

可重复读示例：

```sql
-- 会话A：开启事务并第一次查询余额
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

-- 保持事务不提交，等待会话B提交修改后再次查询
```

会话 B 修改并提交：

```sql
-- 会话B：修改余额并提交
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount + 100.00
WHERE user_id = 10001;

COMMIT;
```

会话 A 再次普通查询，通常仍看到事务开始后的快照数据：

```sql
-- 会话A：REPEATABLE READ 下普通 SELECT 读取事务快照
SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

但当前读会读取最新已提交数据并加锁：

```sql
-- 当前读：读取最新已提交数据，并尝试加排他锁
START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
FOR UPDATE;

COMMIT;
```

`REPEATABLE READ` 使用建议：

1. 适合大多数 MySQL InnoDB 业务系统。
2. 普通 `SELECT` 通常是快照读。
3. `SELECT ... FOR UPDATE`、`UPDATE`、`DELETE` 是当前读。
4. 当前读可能触发行锁、间隙锁或临键锁。
5. 在高并发写入场景下，应关注锁等待和死锁问题。

### SERIALIZABLE

`SERIALIZABLE` 是最高隔离级别。它会让事务并发行为更接近串行执行，可以避免脏读、不可重复读和幻读，但并发性能最低，锁冲突最严重。

设置当前会话为 `SERIALIZABLE`：

```sql
-- 设置当前会话事务隔离级别为 SERIALIZABLE
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

事务查询示例：

```sql
-- SERIALIZABLE 下执行事务查询
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE account_status = 1;

COMMIT;
```

`SERIALIZABLE` 使用建议：

1. 一般业务系统不建议默认使用。
2. 并发性能较低，容易产生锁等待。
3. 适合极少数强一致、低并发、可接受串行化成本的场景。
4. 多数业务可以通过 `REPEATABLE READ`、唯一约束、状态机、乐观锁和显式锁满足一致性要求。
5. 如果为了修复并发问题直接升到 `SERIALIZABLE`，通常不是最优方案，应先检查业务建模和锁范围。

### 脏读

脏读是指一个事务读取到了另一个事务尚未提交的数据。如果另一个事务最终回滚，那么前一个事务读到的数据就是无效数据。

脏读示例流程：

```text
1. 会话A开启事务，修改账户余额，但未提交
2. 会话B在 READ UNCOMMITTED 下读取到账户新余额
3. 会话A执行 ROLLBACK
4. 会话B之前读取到的数据实际不存在
```

会话 A 修改后回滚：

```sql
-- 会话A：修改余额但不提交
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount + 1000.00
WHERE user_id = 10001;

-- 最终回滚
ROLLBACK;
```

会话 B 在低隔离级别下可能读到未提交数据：

```sql
-- 会话B：READ UNCOMMITTED 下可能发生脏读
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

避免脏读建议：

1. 不使用 `READ UNCOMMITTED` 处理核心业务。
2. 使用 `READ COMMITTED` 或更高隔离级别。
3. 对余额、库存、支付状态等核心数据，不允许读取未提交数据。
4. 查询和写入逻辑应由事务边界清晰控制。
5. 不要用降低隔离级别的方式掩盖锁设计问题。

### 不可重复读

不可重复读是指同一个事务中，两次读取同一行数据，结果不一致。通常是因为另一个事务在两次读取之间提交了更新。

不可重复读示例流程：

```text
1. 会话A开启事务，第一次读取余额为 100
2. 会话B修改余额为 200 并提交
3. 会话A第二次读取余额为 200
4. 会话A在同一事务内两次读取结果不一致
```

在 `READ COMMITTED` 下可能发生：

```sql
-- 会话A：READ COMMITTED 下同一事务内多次读取可能不同
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

-- 等待会话B更新并提交后，再次查询
SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

使用 `REPEATABLE READ` 可避免普通快照读下的不可重复读：

```sql
-- 会话A：REPEATABLE READ 下普通 SELECT 读取一致性快照
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

不可重复读处理建议：

1. 对同一事务内需要一致快照的数据，使用 `REPEATABLE READ`。
2. 对需要读取最新提交数据的场景，可以接受 `READ COMMITTED`。
3. 对需要防止并发修改的数据，应使用当前读，例如 `FOR UPDATE`。
4. 余额扣减、库存扣减不要只依赖普通查询结果，应使用条件更新。
5. 事务中多次读取同一数据时，要明确是否需要一致快照还是最新数据。

### 幻读

幻读是指同一个事务中，两次按同一范围查询数据，第二次出现了第一次没有的数据行，像“幻影”一样。它通常与范围查询和其他事务插入新记录有关。

幻读示例流程：

```text
1. 会话A开启事务，查询余额大于 100 的账户，返回 10 行
2. 会话B插入一条余额大于 100 的账户并提交
3. 会话A再次执行同样范围查询，可能看到 11 行
```

普通快照读下的范围查询：

```sql
-- 会话A：普通 SELECT 是快照读
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE balance_amount > 100.00;

-- 再次普通查询通常仍读取一致性快照
SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE balance_amount > 100.00;

COMMIT;
```

当前读下的范围锁示例：

```sql
-- 当前读：范围查询并加锁，可能产生临键锁，阻止其他事务在范围内插入
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id BETWEEN 10001 AND 10010
FOR UPDATE;

COMMIT;
```

幻读处理建议：

1. 普通快照读依赖 MVCC 保证一致性视图。
2. 当前读需要通过锁控制并发插入。
3. 范围当前读可能产生间隙锁或临键锁。
4. 防止重复插入优先使用唯一索引，不要只依赖查询后判断。
5. 高并发业务中，唯一约束通常比范围加锁更稳健。

### 行锁

行锁是 InnoDB 中常见的锁粒度，用于锁定满足条件的索引记录。行锁能够提升并发性能，但前提是 SQL 能通过索引精确定位记录。如果没有合适索引，可能导致锁范围扩大。

按唯一索引加行锁：

```sql
-- 根据唯一索引 user_id 查询并加排他锁
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
FOR UPDATE;

COMMIT;
```

更新单行数据：

```sql
-- 根据唯一索引更新账户余额，通常只锁定匹配行
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount - 10.00
WHERE user_id = 10001
  AND balance_amount >= 10.00;

COMMIT;
```

共享锁查询：

```sql
-- 加共享锁，允许其他事务读，但阻止其他事务修改锁定行
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
LOCK IN SHARE MODE;

COMMIT;
```

行锁使用建议：

1. 行锁依赖索引，条件字段应命中索引。
2. 通过主键或唯一索引加锁，锁范围最清晰。
3. 避免无索引条件更新，否则可能扫描并锁定大量记录。
4. 加锁后应尽快提交或回滚，减少锁等待。
5. 高并发扣减库存、余额时，推荐使用条件更新控制并发。

### 表锁

表锁会锁定整张表，锁粒度较大，并发性能较低。InnoDB 常规 DML 通常使用行锁，但某些 DDL、显式锁表、元数据锁等待、无索引更新等场景可能表现出接近表级阻塞的效果。

显式锁表：

```sql
-- 显式给表加写锁，阻塞其他会话读写，业务系统中谨慎使用
LOCK TABLES account_lock_demo WRITE;

-- 执行相关操作
UPDATE account_lock_demo
SET account_status = 0
WHERE user_id = 10001;

-- 释放表锁
UNLOCK TABLES;
```

读锁示例：

```sql
-- 显式给表加读锁，其他会话可读但不能写
LOCK TABLES account_lock_demo READ;

SELECT
    COUNT(*) AS total_count
FROM account_lock_demo;

UNLOCK TABLES;
```

表锁使用建议：

1. 业务系统中很少需要显式 `LOCK TABLES`。
2. 表锁会严重影响并发读写。
3. 大多数一致性需求应通过事务、行锁、唯一约束和状态条件解决。
4. DDL 操作可能引发元数据锁等待，应避开业务高峰。
5. 如果简单更新造成大面积阻塞，应检查 SQL 是否命中索引。

### 间隙锁

间隙锁用于锁定索引记录之间的间隙，防止其他事务在该间隙中插入新记录。它通常出现在 `REPEATABLE READ` 隔离级别下的范围当前读中，用于防止幻读。

间隙锁示例：

```sql
-- 对 user_id 范围加锁，可能锁定范围内记录及记录间间隙
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id BETWEEN 10001 AND 10010
FOR UPDATE;

-- 事务未提交前，其他事务在该范围内插入 user_id 可能被阻塞
COMMIT;
```

另一个会话插入范围内数据可能等待：

```sql
-- 会话B：如果 user_id 在会话A锁定范围内，可能等待锁释放
INSERT INTO account_lock_demo (
    user_id,
    balance_amount,
    account_status
) VALUES (
    10005,
    100.00,
    1
);
```

减少间隙锁影响的建议：

1. 尽量使用唯一索引等值查询加锁，减少范围锁。
2. 范围当前读要控制范围大小。
3. 避免在事务中长时间持有范围锁。
4. 高并发插入场景要谨慎使用范围 `FOR UPDATE`。
5. 能用唯一约束防重时，优先使用唯一约束，而不是范围加锁防重。

### 临键锁

临键锁是 Next-Key Lock，由记录锁和间隙锁组合而成。它不仅锁定索引记录本身，也锁定记录前面的间隙，用于防止其他事务插入满足范围条件的新记录。

范围当前读可能产生临键锁：

```sql
-- 范围查询并加排他锁，可能产生临键锁
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id > 10001
  AND user_id < 10010
FOR UPDATE;

COMMIT;
```

当前读更新范围数据：

```sql
-- 范围更新可能锁定匹配记录和相关间隙
START TRANSACTION;

UPDATE account_lock_demo
SET account_status = 0
WHERE user_id > 10001
  AND user_id < 10010;

COMMIT;
```

临键锁使用理解：

1. 临键锁用于解决范围当前读中的幻读问题。
2. 它锁定的是索引范围，不是逻辑上的业务范围。
3. 没有合适索引时，锁范围可能扩大。
4. 唯一索引等值查询命中存在记录时，通常可以退化为记录锁。
5. 范围查询加锁越宽，对并发插入影响越大。

### 意向锁

意向锁是表级锁，用于表示事务接下来想在表中的某些行上加共享锁或排他锁。InnoDB 使用意向锁协调表锁和行锁之间的兼容关系。常见类型包括意向共享锁和意向排他锁。

常见意向锁类型：

| 锁类型     | 说明                         |
| ---------- | ---------------------------- |
| 意向共享锁 | 表示事务准备对某些行加共享锁 |
| 意向排他锁 | 表示事务准备对某些行加排他锁 |

加排他行锁时，InnoDB 会自动加意向排他锁：

```sql
-- 对某行加 FOR UPDATE 锁时，InnoDB 会在表级别加意向排他锁
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
FOR UPDATE;

COMMIT;
```

加共享行锁时，InnoDB 会自动加意向共享锁：

```sql
-- 对某行加共享锁时，InnoDB 会在表级别加意向共享锁
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
LOCK IN SHARE MODE;

COMMIT;
```

意向锁理解建议：

1. 意向锁由 InnoDB 自动管理，应用层通常不需要直接操作。
2. 它是表级锁，但不代表锁住整张表的数据行。
3. 它用于协调表级锁和行级锁。
4. 排查锁等待时可能在系统视图中看到意向锁。
5. 不应把意向锁误解为业务 SQL 显式加了整表锁。

### 死锁分析

死锁是指两个或多个事务相互等待对方持有的锁，导致都无法继续执行。InnoDB 会自动检测死锁，并回滚其中一个事务，让另一个事务继续执行。死锁不是数据库故障，而是并发写入场景中需要治理的典型问题。

典型死锁流程：

```text
1. 事务A锁定 user_id = 10001
2. 事务B锁定 user_id = 10002
3. 事务A尝试锁定 user_id = 10002，等待事务B
4. 事务B尝试锁定 user_id = 10001，等待事务A
5. 形成死锁，InnoDB 回滚其中一个事务
```

会话 A 示例：

```sql
-- 会话A：先锁 10001，再尝试锁 10002
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount - 10.00
WHERE user_id = 10001;

-- 等待会话B锁定 10002 后执行
UPDATE account_lock_demo
SET balance_amount = balance_amount + 10.00
WHERE user_id = 10002;

COMMIT;
```

会话 B 示例：

```sql
-- 会话B：先锁 10002，再尝试锁 10001，可能与会话A形成死锁
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount - 20.00
WHERE user_id = 10002;

-- 等待会话A锁定 10001 后执行
UPDATE account_lock_demo
SET balance_amount = balance_amount + 20.00
WHERE user_id = 10001;

COMMIT;
```

查看最近一次死锁信息：

```sql
-- 查看 InnoDB 状态，包括最近一次死锁信息
SHOW ENGINE INNODB STATUS;
```

查看当前事务和锁等待：

```sql
-- 查看当前 InnoDB 事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX;

-- 查看锁等待关系，具体字段取决于 MySQL 版本
SELECT
    *
FROM performance_schema.data_lock_waits;
```

死锁优化建议：

1. 多个事务更新多行数据时，保持固定的访问顺序。
2. 尽量通过主键或唯一索引精确更新，减少锁范围。
3. 控制事务时长，不在事务中执行外部调用。
4. 大批量更新拆分为小批次。
5. 建立合适索引，避免无索引更新扩大锁范围。
6. 应用层应捕获死锁异常，并对幂等操作进行有限重试。
7. 对账户转账、库存扣减等高并发场景，应设计清晰的锁顺序和状态条件。

## InnoDB 存储引擎

InnoDB 是 MySQL 最常用的事务型存储引擎，支持事务、行级锁、MVCC、外键、崩溃恢复和高并发读写。理解 InnoDB 的架构和核心机制，有助于解释索引、锁、事务、redo log、undo log、Buffer Pool 和崩溃恢复等行为。

### InnoDB 架构

InnoDB 架构可以粗略分为内存结构和磁盘结构。内存结构用于缓存数据、索引和变更，磁盘结构用于持久化数据文件、日志文件和系统表空间。

简化结构如下：

```text
MySQL Server 层
    |
    | SQL 解析、优化器、执行器
    v
InnoDB 存储引擎
    |
    |-- 内存结构
    |     |-- Buffer Pool
    |     |-- Change Buffer
    |     |-- Log Buffer
    |     |-- Adaptive Hash Index
    |
    |-- 磁盘结构
          |-- 表空间文件
          |-- Redo Log
          |-- Undo Log
          |-- Doublewrite Buffer
```

常见组件说明：

| 组件               | 作用                                 |
| ------------------ | ------------------------------------ |
| Buffer Pool        | 缓存数据页和索引页，减少磁盘 IO      |
| Redo Log           | 记录物理修改，用于崩溃恢复           |
| Undo Log           | 记录回滚信息，用于事务回滚和 MVCC    |
| Change Buffer      | 缓存非唯一二级索引变更，减少随机 IO  |
| Doublewrite Buffer | 防止页写入过程中损坏导致数据页不完整 |
| 表空间             | 存储表数据、索引和部分内部结构       |
| MVCC               | 多版本并发控制，支持快照读           |

查看 InnoDB 相关配置：

```sql
-- 查看 InnoDB 常见配置
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';
SHOW VARIABLES LIKE 'innodb_log_file_size';
SHOW VARIABLES LIKE 'innodb_file_per_table';
```

查看 InnoDB 运行状态：

```sql
-- 查看 InnoDB 运行状态、锁、死锁、事务等信息
SHOW ENGINE INNODB STATUS;
```

### 聚簇索引

聚簇索引是 InnoDB 表中按照主键组织数据的索引结构。InnoDB 的主键索引叶子节点存储完整数据行，因此主键索引就是聚簇索引。

主键表示例：

```sql
-- InnoDB 表中主键索引就是聚簇索引
CREATE TABLE IF NOT EXISTS user_cluster_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚簇索引示例表';
```

按主键查询：

```sql
-- 按主键查询可以直接在聚簇索引中定位完整数据行
SELECT
    id,
    username,
    nickname,
    created_at
FROM user_cluster_demo
WHERE id = 10001;
```

聚簇索引设计建议：

1. 每张 InnoDB 表都应显式定义主键。
2. 主键应短、稳定、唯一。
3. 自增主键或趋势递增主键对插入更友好。
4. 主键过大会增加二级索引体积。
5. 不建议使用频繁变更的业务字段作为主键。
6. 如果没有显式主键，InnoDB 会选择唯一非空索引或内部隐藏行 ID，通常不利于维护。

### 二级索引

二级索引也称辅助索引，是除聚簇索引之外的索引。InnoDB 二级索引的叶子节点存储的是索引字段值和主键值，而不是完整数据行。因此通过二级索引查询非索引字段时，通常需要回表。

创建二级索引：

```sql
-- 为用户名创建唯一二级索引，为创建时间创建普通二级索引
CREATE TABLE IF NOT EXISTS user_secondary_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级索引示例表';
```

按二级索引查询：

```sql
-- 通过 mobile 二级索引定位主键，再回表读取完整行
SELECT
    id,
    username,
    nickname,
    mobile,
    created_at
FROM user_secondary_demo
WHERE mobile = '13800000001';
```

覆盖索引减少回表：

```sql
-- 查询字段只包含二级索引字段和主键时，有机会使用覆盖索引
SELECT
    id,
    mobile
FROM user_secondary_demo
WHERE mobile = '13800000001';
```

二级索引设计建议：

1. 二级索引用于优化非主键字段查询。
2. 二级索引叶子节点存储主键值，因此主键越大，二级索引越大。
3. 查询二级索引未覆盖的字段时，需要回表。
4. 高频列表查询可以设计覆盖索引减少回表。
5. 二级索引越多，写入维护成本越高。

### Buffer Pool

Buffer Pool 是 InnoDB 最核心的内存区域，用于缓存数据页和索引页。读取数据时，如果页已经在 Buffer Pool 中，就可以避免磁盘 IO；写入数据时，数据页通常先在 Buffer Pool 中修改为脏页，后续再刷盘。

查看 Buffer Pool 大小：

```sql
-- 查看 Buffer Pool 大小
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
```

查看 Buffer Pool 相关状态：

```sql
-- 查看 Buffer Pool 状态指标
SHOW STATUS LIKE 'Innodb_buffer_pool%';
```

常见指标含义：

| 指标                               | 说明                 |
| ---------------------------------- | -------------------- |
| `Innodb_buffer_pool_read_requests` | 逻辑读请求次数       |
| `Innodb_buffer_pool_reads`         | 需要从磁盘读取的次数 |
| `Innodb_buffer_pool_pages_dirty`   | 当前脏页数量         |
| `Innodb_buffer_pool_pages_free`    | 当前空闲页数量       |
| `Innodb_buffer_pool_pages_total`   | Buffer Pool 总页数   |

粗略计算 Buffer Pool 命中率：

```sql
-- 查看逻辑读和物理读，用于粗略判断 Buffer Pool 命中情况
SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW STATUS LIKE 'Innodb_buffer_pool_reads';
```

Buffer Pool 配置示例：

```ini
[mysqld]
# InnoDB 缓冲池大小，生产环境应结合物理内存、实例数量和业务负载设置
innodb_buffer_pool_size=4G

# Buffer Pool 实例数量，大内存场景可适当增加
innodb_buffer_pool_instances=4
```

Buffer Pool 使用建议：

1. Buffer Pool 越能容纳热点数据和索引，读性能越稳定。
2. 数据库专用服务器上，Buffer Pool 通常会配置为较大比例内存。
3. 配置过大会挤压操作系统和其他进程内存。
4. 高频 SQL 应尽量命中索引，减少无效页进入 Buffer Pool。
5. 大批量扫描可能污染 Buffer Pool，应在低峰期执行。

### Redo Log

Redo Log 是 InnoDB 的重做日志，用于保证事务持久性和崩溃恢复。事务提交时，数据页不一定立即刷盘，但相关修改会记录到 redo log 中。数据库崩溃后，InnoDB 可以根据 redo log 重放已提交事务的修改。

Redo Log 的作用可以概括为：

```text
1. 修改数据页时，先修改内存中的 Buffer Pool
2. 同时记录 redo log
3. 提交事务时，按配置保证 redo log 落盘
4. 后台线程再将脏页刷入数据文件
5. 崩溃恢复时，通过 redo log 恢复已提交修改
```

查看 redo 相关配置：

```sql
-- 查看 redo log 和事务提交刷盘策略
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';
SHOW VARIABLES LIKE 'innodb_log_file_size';
SHOW VARIABLES LIKE 'innodb_log_files_in_group';
```

关键参数说明：

| 参数                             | 说明                                        |
| -------------------------------- | ------------------------------------------- |
| `innodb_flush_log_at_trx_commit` | 控制事务提交时 redo log 刷盘策略            |
| `innodb_log_file_size`           | redo log 文件大小                           |
| `innodb_log_files_in_group`      | redo log 文件组数量，部分版本中配置方式不同 |

`innodb_flush_log_at_trx_commit` 常见取值：

| 值   | 说明                           | 特点                               |
| ---- | ------------------------------ | ---------------------------------- |
| `1`  | 每次提交都写入并刷盘           | 持久性最好，性能成本较高           |
| `2`  | 每次提交写入 OS 缓存，周期刷盘 | 性能较好，系统崩溃可能丢失少量数据 |
| `0`  | 周期写入和刷盘                 | 性能较好，崩溃风险更高             |

Redo Log 使用建议：

1. 核心交易系统通常使用 `innodb_flush_log_at_trx_commit=1`。
2. 写入压力很高但可接受少量丢失的场景，可以评估其他取值。
3. redo log 太小可能导致频繁 checkpoint，影响写入性能。
4. redo log 是崩溃恢复关键组件，不应随意删除。
5. 调整 redo 相关参数前，应结合业务可靠性要求和压测结果。

### Undo Log

Undo Log 是回滚日志，用于事务回滚和 MVCC。事务修改数据前，InnoDB 会记录旧版本数据到 undo log。如果事务回滚，可以根据 undo log 恢复旧值；普通快照读也依赖 undo log 读取历史版本。

Undo Log 的作用：

| 作用         | 说明                                 |
| ------------ | ------------------------------------ |
| 事务回滚     | 回滚未提交事务的修改                 |
| MVCC 快照读  | 为旧事务提供历史版本数据             |
| 一致性读     | 普通 SELECT 可以读取事务开始时的版本 |
| 清理历史版本 | 事务结束后，旧版本由 purge 线程清理  |

事务回滚示例：

```sql
-- 修改数据后回滚，依赖 undo log 恢复旧值
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount - 100.00
WHERE user_id = 10001;

ROLLBACK;
```

长事务影响示例：

```sql
-- 长事务开启后长时间不提交，会导致旧版本无法及时清理
START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

-- 长时间保持事务不提交，会影响 undo 历史版本清理
```

查看当前长事务：

```sql
-- 查看当前运行中的 InnoDB 事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

Undo Log 使用建议：

1. 不要长时间开启事务不提交。
2. 长事务会导致 undo 历史版本无法及时清理。
3. 大批量更新、删除会产生大量 undo log。
4. 批量变更建议分批提交。
5. 普通快照读依赖 undo log，因此长事务可能影响存储和性能。

### Change Buffer

Change Buffer 用于缓存对非唯一二级索引页的变更。当目标索引页不在 Buffer Pool 中时，InnoDB 可以先把变更记录到 Change Buffer，后续读取该页或后台合并时再应用变更，从而减少随机 IO。

Change Buffer 适用条件：

| 条件                 | 说明                                         |
| -------------------- | -------------------------------------------- |
| 非唯一二级索引       | 唯一索引需要立即检查唯一性，通常不能这样延迟 |
| 目标索引页不在内存中 | 如果页已经在 Buffer Pool 中，直接修改即可    |
| 写多读少场景         | 可以减少随机读写                             |
| 后续由 merge 合并    | 读取或后台线程会合并变更                     |

查看 Change Buffer 配置：

```sql
-- 查看 Change Buffer 配置
SHOW VARIABLES LIKE 'innodb_change_buffering';
SHOW VARIABLES LIKE 'innodb_change_buffer_max_size';
```

查看相关状态：

```sql
-- 查看 Change Buffer 相关状态，具体指标名称可能因版本不同而不同
SHOW STATUS LIKE 'Innodb_ibuf%';
```

Change Buffer 使用建议：

1. 它主要优化非唯一二级索引的写入。
2. 唯一索引需要唯一性检查，不能简单延迟合并。
3. 写多读少场景收益更明显。
4. 如果业务读写都很频繁，Change Buffer 收益可能有限。
5. 过多二级索引仍会增加写入成本，不能依赖 Change Buffer 掩盖索引过多问题。

### Doublewrite Buffer

Doublewrite Buffer 用于防止数据页写入过程中发生部分写问题。InnoDB 页通常较大，如果操作系统或硬件在写入数据页时崩溃，可能导致页只写了一部分。Doublewrite Buffer 可以帮助恢复完整页。

Doublewrite 简化流程：

```text
1. InnoDB 准备将脏页刷盘
2. 先将脏页写入 Doublewrite Buffer
3. 再写入真正的数据文件位置
4. 如果写数据文件过程中崩溃，可从 Doublewrite Buffer 找到完整页
5. 再结合 Redo Log 完成恢复
```

查看 Doublewrite 配置：

```sql
-- 查看 Doublewrite Buffer 是否开启
SHOW VARIABLES LIKE 'innodb_doublewrite';
```

Doublewrite 使用建议：

1. 它用于提升数据页写入安全性。
2. 关闭可能提升部分写入性能，但会增加崩溃后数据页损坏风险。
3. 生产环境通常不建议随意关闭。
4. 它和 redo log 共同参与崩溃恢复。
5. 存储硬件、文件系统和数据库配置都可靠时，也应谨慎评估是否调整。

### MVCC 机制

MVCC 是 Multi-Version Concurrency Control，即多版本并发控制。InnoDB 通过 MVCC 让普通读和写可以更好地并发执行。普通 `SELECT` 读取的是一致性快照，不需要阻塞正在修改数据的事务。

MVCC 的核心组件：

| 组件        | 说明                       |
| ----------- | -------------------------- |
| 隐藏事务 ID | 记录最后修改该行的事务 ID  |
| 回滚指针    | 指向 undo log 中的历史版本 |
| Undo Log    | 保存旧版本数据             |
| Read View   | 定义当前事务能看到哪些版本 |

快照读示例：

```sql
-- 普通 SELECT 通常是快照读，不加锁
START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001;

COMMIT;
```

当前读示例：

```sql
-- SELECT ... FOR UPDATE 是当前读，会读取最新已提交版本并加锁
START TRANSACTION;

SELECT
    user_id,
    balance_amount
FROM account_lock_demo
WHERE user_id = 10001
FOR UPDATE;

COMMIT;
```

更新属于当前读：

```sql
-- UPDATE 会读取最新可更新版本并加锁
START TRANSACTION;

UPDATE account_lock_demo
SET balance_amount = balance_amount - 10.00
WHERE user_id = 10001
  AND balance_amount >= 10.00;

COMMIT;
```

快照读与当前读对比：

| 类型   | 示例                            | 是否加锁   | 读取内容               |
| ------ | ------------------------------- | ---------- | ---------------------- |
| 快照读 | 普通 `SELECT`                   | 通常不加锁 | 一致性快照             |
| 当前读 | `SELECT ... FOR UPDATE`         | 加排他锁   | 最新已提交并可加锁版本 |
| 当前读 | `SELECT ... LOCK IN SHARE MODE` | 加共享锁   | 最新已提交并可加锁版本 |
| 当前读 | `UPDATE` / `DELETE`             | 加排他锁   | 最新可修改版本         |

MVCC 使用建议：

1. 普通查询通常不阻塞写入，依赖 MVCC 提供一致性读。
2. 需要锁定数据防止并发修改时，使用当前读。
3. 长事务会持有旧 Read View，导致历史版本无法及时清理。
4. 在 `REPEATABLE READ` 下，同一事务普通查询通常看到一致快照。
5. 当前读和快照读结果可能不同，业务设计时要明确使用哪种读。

### 崩溃恢复

崩溃恢复是 InnoDB 在数据库异常宕机后恢复到一致状态的过程。它依赖 redo log、undo log、Doublewrite Buffer 和 checkpoint 等机制。目标是保证已提交事务的修改不丢失，未提交事务的修改被回滚。

崩溃恢复简化流程：

```text
1. MySQL 异常宕机
2. 重启时 InnoDB 检查数据页和日志状态
3. 使用 Doublewrite Buffer 修复可能损坏的数据页
4. 使用 Redo Log 重放已提交事务的修改
5. 使用 Undo Log 回滚未提交事务的修改
6. 数据库恢复到一致状态
```

模拟事务提交和崩溃恢复逻辑：

```text
事务A已提交：
- 数据页可能还没完全刷盘
- redo log 已记录提交修改
- 崩溃恢复时重放 redo log，保证修改不丢失

事务B未提交：
- 数据页可能已经部分修改
- undo log 保存旧版本
- 崩溃恢复时通过 undo log 回滚未提交修改
```

查看恢复相关错误日志通常在 MySQL 错误日志中完成：

```bash
# Linux 环境查看 MySQL 错误日志，路径以实际配置为准
sudo tail -n 200 /var/log/mysql/error.log

# 如果使用 systemd 管理 MySQL，也可以查看服务日志
sudo journalctl -u mysql -n 200 --no-pager
```

命令说明：

- `tail -n 200` 查看最近 200 行错误日志。
- `/var/log/mysql/error.log` 是常见路径，实际路径应以 `log_error` 配置为准。
- `journalctl -u mysql` 用于查看 systemd 中 MySQL 服务日志。
- RPM 系系统服务名可能是 `mysqld`，需要改为 `journalctl -u mysqld`。

查看错误日志配置：

```sql
-- 查看错误日志路径
SHOW VARIABLES LIKE 'log_error';
```

崩溃恢复相关建议：

1. 不要随意删除 redo log、undo log 或数据文件。
2. 生产环境应配置可靠存储和定期备份。
3. `innodb_flush_log_at_trx_commit` 会影响事务持久性策略。
4. 崩溃恢复不是备份替代方案，误删和误更新仍需要备份或 binlog 恢复。
5. 异常宕机后应检查错误日志、表状态、主从复制状态和业务数据一致性。
6. 大事务、长事务和大量脏页可能导致恢复时间变长。


## 用户与权限管理

用户与权限管理用于控制谁可以连接 MySQL、可以访问哪些数据库、可以执行哪些操作。生产环境中，权限设计应遵循最小权限原则，避免使用 root 账号连接业务系统，避免给应用账号授予 `ALL PRIVILEGES`、`SUPER`、`GRANT OPTION` 等高危权限。

MySQL 用户由用户名和主机两部分共同标识，格式通常为：

```text
'用户名'@'主机'
```

例如：

```text
'app_user'@'localhost'       -- 只允许本机连接
'app_user'@'192.168.1.%'     -- 只允许指定内网网段连接
'app_user'@'%'               -- 允许任意主机连接，不推荐生产环境使用
```

### 创建用户

创建用户用于为应用系统、开发人员、运维人员、只读查询、备份任务等分配独立账号。生产环境不应多个系统共用一个账号，也不应使用 root 账号作为应用连接账号。

创建本地用户：

```sql
-- 创建只允许本机连接的用户
CREATE USER 'app_user'@'localhost'
IDENTIFIED BY 'App_123456';
```

创建指定网段访问用户：

```sql
-- 创建只允许指定内网网段连接的用户
CREATE USER 'app_user'@'192.168.1.%'
IDENTIFIED BY 'App_123456';
```

创建只读用户：

```sql
-- 创建报表只读用户
CREATE USER 'report_user'@'192.168.1.%'
IDENTIFIED BY 'Report_123456';
```

创建备份用户：

```sql
-- 创建备份任务用户
CREATE USER 'backup_user'@'192.168.1.%'
IDENTIFIED BY 'Backup_123456';
```

查看用户：

```sql
-- 查看 MySQL 用户列表
SELECT
    user,
    host,
    plugin,
    account_locked,
    password_expired
FROM mysql.user
ORDER BY user, host;
```

创建用户建议：

1. 每个应用系统使用独立数据库账号。
2. 读写账号、只读账号、备份账号、运维账号应分开。
3. 用户名应表达用途，例如 `mall_app`、`mall_readonly`、`backup_user`。
4. 主机范围应尽量收窄，不建议生产环境使用 `'%'`。
5. 密码应符合复杂度要求，并通过密钥系统或环境变量注入应用。

### 修改用户

修改用户通常包括修改密码、锁定账号、解锁账号、修改认证插件、设置密码过期策略等。生产环境修改用户前应确认该账号是否被应用、定时任务或运维脚本使用。

修改用户密码：

```sql
-- 修改指定用户密码
ALTER USER 'app_user'@'192.168.1.%'
IDENTIFIED BY 'New_App_123456';
```

锁定用户：

```sql
-- 锁定用户，禁止继续登录
ALTER USER 'app_user'@'192.168.1.%' ACCOUNT LOCK;
```

解锁用户：

```sql
-- 解锁用户
ALTER USER 'app_user'@'192.168.1.%' ACCOUNT UNLOCK;
```

设置用户密码过期：

```sql
-- 设置用户密码立即过期，下次登录需要修改密码
ALTER USER 'app_user'@'192.168.1.%' PASSWORD EXPIRE;
```

设置密码永不过期：

```sql
-- 设置用户密码不过期，生产环境应结合公司安全规范谨慎使用
ALTER USER 'app_user'@'192.168.1.%' PASSWORD EXPIRE NEVER;
```

修改认证插件示例：

```sql
-- 修改用户认证插件，需确认客户端驱动兼容性
ALTER USER 'app_user'@'192.168.1.%'
IDENTIFIED WITH caching_sha2_password BY 'New_App_123456';
```

修改用户建议：

1. 修改应用账号密码前，应先准备应用配置变更和发布窗口。
2. 锁定账号适合禁用离职人员、废弃系统、异常连接来源。
3. 修改认证插件前必须确认客户端驱动版本兼容。
4. 重要账号密码变更后，应验证应用连接、定时任务和备份任务是否正常。
5. 生产环境不建议直接修改 root 认证方式，除非有完整回滚方案。

### 删除用户

删除用户用于清理废弃账号、离职人员账号、历史测试账号或不再使用的系统账号。删除用户前应确认该账号没有被应用、脚本、备份任务、监控系统继续使用。

删除用户：

```sql
-- 删除指定用户
DROP USER 'app_user'@'192.168.1.%';
```

删除多个用户：

```sql
-- 删除多个用户
DROP USER
    'old_user'@'192.168.1.%',
    'test_user'@'%';
```

删除前查看用户权限：

```sql
-- 查看用户权限，确认是否仍有业务使用痕迹
SHOW GRANTS FOR 'app_user'@'192.168.1.%';
```

删除前查看连接情况：

```sql
-- 查看当前连接中是否有该用户
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE user = 'app_user';
```

删除用户建议：

1. 删除前先锁定账号观察一段时间，比直接删除更安全。
2. 确认应用、定时任务、备份脚本、监控系统不再使用该账号。
3. 删除生产账号前应保留变更记录。
4. 删除用户会同时删除该用户的权限。
5. 不要删除系统内置账号，除非明确知道用途和影响。

推荐流程：

```text
1. 查询用户权限
2. 查询当前连接
3. 锁定用户
4. 观察应用和任务是否异常
5. 确认无影响后删除用户
```

### 用户授权

用户授权用于给账号分配数据库、表、字段或全局级别的权限。生产环境中，授权范围应尽量小，优先按库授权，其次按表授权，避免全局授权。

给应用用户授予业务库读写权限：

```sql
-- 授予应用用户对 mall_order 库中所有表的常规读写权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'mall_app'@'192.168.1.%';
```

给只读用户授权：

```sql
-- 授予报表用户只读权限
GRANT SELECT
ON mall_order.*
TO 'report_user'@'192.168.1.%';
```

给备份用户授权：

```sql
-- 授予备份用户常见备份所需权限，具体权限应结合备份工具要求调整
GRANT SELECT, SHOW VIEW, TRIGGER, LOCK TABLES
ON mall_order.*
TO 'backup_user'@'192.168.1.%';
```

授予指定表权限：

```sql
-- 只允许用户查询订单表
GRANT SELECT
ON mall_order.order_info
TO 'report_user'@'192.168.1.%';
```

授予字段级权限：

```sql
-- 只允许查询部分字段
GRANT SELECT (id, order_no, user_id, order_status, created_at)
ON mall_order.order_info
TO 'report_user'@'192.168.1.%';
```

刷新权限：

```sql
-- 使用 GRANT 语句通常会自动生效，一般不需要 FLUSH PRIVILEGES
-- 如果直接修改 mysql 系统表，则需要刷新权限，不建议直接改系统表
FLUSH PRIVILEGES;
```

常见权限说明：

| 权限           | 说明               | 常见账号               |
| -------------- | ------------------ | ---------------------- |
| `SELECT`       | 查询数据           | 只读账号、应用账号     |
| `INSERT`       | 插入数据           | 应用账号               |
| `UPDATE`       | 更新数据           | 应用账号               |
| `DELETE`       | 删除数据           | 应用账号               |
| `CREATE`       | 创建对象           | 变更账号               |
| `ALTER`        | 修改表结构         | 变更账号               |
| `DROP`         | 删除对象           | 高危权限               |
| `INDEX`        | 创建或删除索引     | 变更账号               |
| `SHOW VIEW`    | 查看视图定义       | 备份账号、只读账号     |
| `TRIGGER`      | 触发器相关权限     | 备份账号、变更账号     |
| `LOCK TABLES`  | 锁表权限           | 备份账号               |
| `GRANT OPTION` | 允许继续授权给别人 | 高危权限，不给应用账号 |

授权建议：

1. 应用账号通常只需要 `SELECT`、`INSERT`、`UPDATE`、`DELETE`。
2. 应用账号不应拥有 `DROP`、`GRANT OPTION`、`SUPER` 等高危权限。
3. DDL 变更应使用专门的变更账号或发布平台执行。
4. 报表账号优先只授予 `SELECT`。
5. 备份账号按备份工具需要授予权限，不要直接给全局 `ALL PRIVILEGES`。

### 权限回收

权限回收用于移除用户已经不再需要的权限。权限治理中，回收权限和授权同样重要。生产环境应定期审计账号权限，移除不必要的 DDL、全局权限和过宽权限。

回收库级权限：

```sql
-- 回收应用用户的删除权限
REVOKE DELETE
ON mall_order.*
FROM 'mall_app'@'192.168.1.%';
```

回收表级权限：

```sql
-- 回收报表用户对订单表的查询权限
REVOKE SELECT
ON mall_order.order_info
FROM 'report_user'@'192.168.1.%';
```

回收所有权限：

```sql
-- 回收用户的所有权限
REVOKE ALL PRIVILEGES, GRANT OPTION
FROM 'app_user'@'192.168.1.%';
```

回收后查看权限：

```sql
-- 查看回收后的权限
SHOW GRANTS FOR 'app_user'@'192.168.1.%';
```

权限回收建议：

1. 权限回收前应确认业务影响。
2. 对废弃账号可以先锁定，再回收权限，最后删除账号。
3. 发现应用账号拥有 DDL 权限时，应优先评估并回收。
4. 回收权限后应验证应用功能和定时任务是否正常。
5. 权限变更应纳入生产变更记录。

### 查看权限

查看权限用于确认账号当前拥有的权限范围，常用于权限排查、安全审计、上线前检查和问题定位。MySQL 中常用 `SHOW GRANTS` 查看指定用户授权。

查看当前用户权限：

```sql
-- 查看当前登录用户权限
SHOW GRANTS;
```

查看指定用户权限：

```sql
-- 查看指定用户权限
SHOW GRANTS FOR 'app_user'@'192.168.1.%';
```

查看所有用户基础信息：

```sql
-- 查看用户、主机、认证插件和账号状态
SELECT
    user,
    host,
    plugin,
    account_locked,
    password_expired
FROM mysql.user
ORDER BY user, host;
```

查看用户在系统权限表中的授权信息：

```sql
-- 查看库级权限
SELECT
    user,
    host,
    db,
    select_priv,
    insert_priv,
    update_priv,
    delete_priv,
    create_priv,
    drop_priv,
    alter_priv
FROM mysql.db
WHERE user = 'app_user';
```

查看当前连接用户：

```sql
-- 查看当前连接用户和实际认证用户
SELECT
    USER() AS login_user,
    CURRENT_USER() AS auth_user;
```

权限排查建议：

1. 先确认用户完整标识，即 `'user'@'host'`。
2. 同名用户在不同 host 下是不同账号，例如 `'app'@'%'` 和 `'app'@'localhost'`。
3. 权限不足时，先执行 `SHOW GRANTS`，不要盲目追加高权限。
4. 连接异常时，检查账号是否锁定、密码是否过期、host 是否匹配。
5. 定期导出权限清单，便于审计和回滚。

### 角色管理

MySQL 8 支持角色管理。角色可以理解为一组权限集合，先把权限授予角色，再把角色授予用户。角色适合统一管理开发、只读、运维、应用等权限模板。

创建角色：

```sql
-- 创建只读角色和应用读写角色
CREATE ROLE 'role_readonly';
CREATE ROLE 'role_app_rw';
```

给角色授权：

```sql
-- 给只读角色授予查询权限
GRANT SELECT
ON mall_order.*
TO 'role_readonly';

-- 给应用读写角色授予常规DML权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'role_app_rw';
```

把角色授予用户：

```sql
-- 将角色授予用户
GRANT 'role_readonly'
TO 'report_user'@'192.168.1.%';

GRANT 'role_app_rw'
TO 'mall_app'@'192.168.1.%';
```

设置默认角色：

```sql
-- 设置用户登录后默认启用角色
SET DEFAULT ROLE 'role_readonly'
TO 'report_user'@'192.168.1.%';

SET DEFAULT ROLE 'role_app_rw'
TO 'mall_app'@'192.168.1.%';
```

查看角色权限：

```sql
-- 查看角色拥有的权限
SHOW GRANTS FOR 'role_readonly';
SHOW GRANTS FOR 'role_app_rw';
```

查看用户权限和角色：

```sql
-- 查看用户授权，包括角色
SHOW GRANTS FOR 'report_user'@'192.168.1.%';
```

回收角色：

```sql
-- 从用户回收角色
REVOKE 'role_readonly'
FROM 'report_user'@'192.168.1.%';
```

删除角色：

```sql
-- 删除角色
DROP ROLE 'role_readonly';
```

角色管理建议：

1. 权限模板稳定时使用角色，减少重复授权。
2. 常见角色包括只读角色、应用读写角色、DDL 变更角色、备份角色。
3. 用户被授予角色后，应设置默认角色，避免登录后角色未启用。
4. 角色变更会影响所有被授予该角色的用户，生产环境要谨慎。
5. 角色权限也应遵循最小权限原则。

### 最小权限原则

最小权限原则是指用户只拥有完成当前职责所需的最少权限。它是数据库安全的基础原则，可以降低误操作、攻击面扩大和数据泄露风险。

常见账号权限建议：

| 账号类型     | 推荐权限                                     | 不建议权限                  |
| ------------ | -------------------------------------------- | --------------------------- |
| 应用读写账号 | `SELECT, INSERT, UPDATE, DELETE` 指定业务库  | `DROP, GRANT OPTION, SUPER` |
| 只读账号     | `SELECT` 指定库表                            | `INSERT, UPDATE, DELETE`    |
| 备份账号     | `SELECT, SHOW VIEW, TRIGGER, LOCK TABLES` 等 | 全局 `ALL PRIVILEGES`       |
| DDL 变更账号 | `CREATE, ALTER, INDEX` 指定库                | 长期开放给应用              |
| 运维管理员   | 按职责分配高权限                             | 多人共用 root               |
| 临时排查账号 | 限时只读或指定表权限                         | 永久高权限                  |

应用账号授权示例：

```sql
-- 应用账号只授予业务库 DML 权限
CREATE USER 'mall_app'@'192.168.1.%'
IDENTIFIED BY 'Mall_App_123456';

GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'mall_app'@'192.168.1.%';
```

只读账号授权示例：

```sql
-- 报表账号只授予查询权限
CREATE USER 'mall_report'@'192.168.1.%'
IDENTIFIED BY 'Mall_Report_123456';

GRANT SELECT
ON mall_order.*
TO 'mall_report'@'192.168.1.%';
```

不推荐授权：

```sql
-- 不推荐：给应用账号授予所有库所有权限
-- GRANT ALL PRIVILEGES ON *.* TO 'mall_app'@'%' WITH GRANT OPTION;
```

最小权限落地建议：

1. 应用账号不使用 root。
2. 应用账号不授予 DDL 权限。
3. 只读和读写账号分离。
4. 不同系统使用不同账号。
5. 不同环境使用不同账号和密码。
6. 临时账号设置有效期或变更后删除。
7. 定期审计权限，回收不再需要的权限。

### 密码策略

密码策略用于保证数据库账号密码具备足够复杂度，并降低密码泄露、弱口令和长期不轮换带来的风险。MySQL 可以通过密码验证组件、密码过期策略、账号锁定等方式增强安全性。

查看密码相关变量：

```sql
-- 查看密码验证相关变量，实际变量取决于是否安装 validate_password 组件
SHOW VARIABLES LIKE 'validate_password%';
```

安装密码验证组件示例：

```sql
-- 安装密码验证组件，具体组件名称取决于 MySQL 版本和安装方式
INSTALL COMPONENT 'file://component_validate_password';
```

设置密码复杂度策略：

```sql
-- 设置密码策略等级，具体可用值取决于版本和组件
SET GLOBAL validate_password.policy = 'MEDIUM';

-- 设置密码最小长度
SET GLOBAL validate_password.length = 12;
```

修改用户密码：

```sql
-- 修改用户密码
ALTER USER 'mall_app'@'192.168.1.%'
IDENTIFIED BY 'Mall_App_Strong_123456';
```

设置密码过期：

```sql
-- 设置用户密码 90 天过期
ALTER USER 'mall_app'@'192.168.1.%'
PASSWORD EXPIRE INTERVAL 90 DAY;
```

密码策略建议：

1. 密码长度建议不少于 12 位。
2. 密码应包含大小写字母、数字和特殊字符。
3. 不同环境、不同系统不要共用密码。
4. 密码不应写死在代码仓库中。
5. 应使用配置中心、密钥管理系统或环境变量管理密码。
6. 定期轮换生产数据库密码。
7. 离职、系统下线、权限调整时及时锁定或删除账号。

### 远程访问控制

远程访问控制用于限制哪些客户端可以连接 MySQL。它由 MySQL 用户 host、服务端监听地址、防火墙、安全组和网络隔离共同控制。

查看 MySQL 监听地址：

```sql
-- 查看 bind_address 配置
SHOW VARIABLES LIKE 'bind_address';

-- 查看端口
SHOW VARIABLES LIKE 'port';
```

配置文件限制监听地址：

```ini
[mysqld]
# 只监听本机，适合单机本地访问
bind-address=127.0.0.1

# 监听所有网卡，生产环境必须配合防火墙、安全组和用户host限制
# bind-address=0.0.0.0
```

创建指定来源用户：

```sql
-- 只允许应用服务器网段连接
CREATE USER 'mall_app'@'10.10.20.%'
IDENTIFIED BY 'Mall_App_123456';

GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'mall_app'@'10.10.20.%';
```

不推荐任意来源访问：

```sql
-- 不推荐：允许任意来源连接
-- CREATE USER 'mall_app'@'%' IDENTIFIED BY 'Mall_App_123456';
```

Linux 防火墙示例：

```bash
# 只允许指定应用服务器访问 MySQL 3306 端口，示例以 firewalld 为准
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="10.10.20.15" port protocol="tcp" port="3306" accept'

# 重新加载防火墙规则
sudo firewall-cmd --reload

# 查看防火墙规则
sudo firewall-cmd --list-all
```

命令说明：

- `source address` 表示允许访问的客户端 IP。
- `port="3306"` 表示 MySQL 默认端口。
- 实际生产环境通常还会结合云安全组、VPC、堡垒机和数据库审计系统控制访问。

远程访问建议：

1. 数据库不应暴露到公网。
2. MySQL 用户 host 不建议使用 `%`。
3. 优先只允许应用服务器、堡垒机、备份服务器访问。
4. root 用户禁止远程登录。
5. 防火墙、安全组和 MySQL 授权应同时收紧。
6. 开放远程访问后必须验证账号权限和来源 IP。

## 安全配置

MySQL 安全配置包括账号安全、网络访问、SSL 加密、SQL 注入防护、敏感数据保护、数据脱敏、审计日志和安全基线。数据库安全不只是 MySQL 配置本身，也需要应用代码、网络环境、权限流程、备份策略和审计体系共同保证。

### root 用户安全

root 用户拥有最高权限，必须严格限制使用范围。生产环境中，root 账号应只用于数据库初始化、紧急维护和受控变更，不应被应用系统、普通开发人员或定时任务使用。

查看 root 用户：

```sql
-- 查看 root 用户允许的登录来源
SELECT
    user,
    host,
    account_locked,
    password_expired
FROM mysql.user
WHERE user = 'root';
```

禁止 root 远程访问：

```sql
-- 删除允许远程登录的 root 用户，执行前必须确认不会影响运维方式
DROP USER IF EXISTS 'root'@'%';
```

修改 root 密码：

```sql
-- 修改 root 本地用户密码
ALTER USER 'root'@'localhost'
IDENTIFIED BY 'Root_Strong_123456';
```

锁定不需要的 root 账号：

```sql
-- 锁定指定 root 账号
ALTER USER 'root'@'127.0.0.1' ACCOUNT LOCK;
```

root 用户安全建议：

1. 禁止应用系统使用 root 连接数据库。
2. 禁止 root 从任意远程主机登录。
3. root 密码必须高强度并定期轮换。
4. root 操作应通过堡垒机、审计平台或变更系统记录。
5. 不同环境 root 密码不能相同。
6. 日常查询、发布、备份都应使用专门账号。

### 远程连接限制

远程连接限制用于减少数据库暴露面。即使账号密码泄露，如果连接来源被限制，攻击风险也会明显降低。远程连接控制应同时在 MySQL 层、操作系统层和网络层执行。

MySQL 用户层限制：

```sql
-- 只允许应用服务器网段访问
CREATE USER 'secure_app'@'10.20.30.%'
IDENTIFIED BY 'Secure_App_123456';

GRANT SELECT, INSERT, UPDATE, DELETE
ON secure_db.*
TO 'secure_app'@'10.20.30.%';
```

服务监听限制：

```ini
[mysqld]
# 只监听内网地址，避免公网网卡暴露
bind-address=10.20.30.10
```

查看当前连接来源：

```sql
-- 查看当前连接来源
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state
FROM information_schema.PROCESSLIST
ORDER BY time DESC;
```

排查异常来源：

```sql
-- 查看指定用户的连接来源
SELECT
    user,
    host,
    COUNT(*) AS connection_count
FROM information_schema.PROCESSLIST
GROUP BY user, host
ORDER BY connection_count DESC;
```

远程连接安全建议：

1. 数据库端口只在内网开放。
2. 云服务器应通过安全组限制来源 IP。
3. 本地服务器应通过防火墙限制来源 IP。
4. MySQL 用户 host 不要无脑使用 `%`。
5. 管理连接应通过堡垒机或 VPN。
6. 发现异常来源连接时，应立即锁定账号并排查泄露风险。

### SSL 连接配置

SSL 连接用于加密客户端与 MySQL 服务端之间的网络传输，防止账号密码和业务数据在网络中被明文窃听。跨主机、跨机房、云环境或不可信网络中建议启用 SSL。

查看 SSL 支持情况：

```sql
-- 查看服务端 SSL 配置
SHOW VARIABLES LIKE 'have_ssl';
SHOW VARIABLES LIKE 'ssl_ca';
SHOW VARIABLES LIKE 'ssl_cert';
SHOW VARIABLES LIKE 'ssl_key';
```

查看当前连接是否使用 SSL：

```sql
-- 查看当前连接是否启用 SSL
SHOW STATUS LIKE 'Ssl_cipher';
```

创建要求 SSL 的用户：

```sql
-- 创建必须使用 SSL 连接的用户
CREATE USER 'ssl_app'@'10.20.30.%'
IDENTIFIED BY 'Ssl_App_123456'
REQUIRE SSL;

GRANT SELECT, INSERT, UPDATE, DELETE
ON secure_db.*
TO 'ssl_app'@'10.20.30.%';
```

修改已有用户要求 SSL：

```sql
-- 修改已有用户，要求必须使用 SSL 连接
ALTER USER 'ssl_app'@'10.20.30.%'
REQUIRE SSL;
```

客户端 SSL 连接示例：

```bash
# 使用 SSL 连接 MySQL，证书路径按实际环境调整
mysql \
  -h 10.20.30.10 \
  -P 3306 \
  -u ssl_app \
  -p \
  --ssl-ca=/etc/mysql/certs/ca.pem \
  --ssl-cert=/etc/mysql/certs/client-cert.pem \
  --ssl-key=/etc/mysql/certs/client-key.pem
```

命令说明：

- `--ssl-ca` 指定 CA 证书。
- `--ssl-cert` 指定客户端证书。
- `--ssl-key` 指定客户端私钥。
- 如果用户被设置为 `REQUIRE SSL`，未使用 SSL 的连接会被拒绝。

SSL 使用建议：

1. 不可信网络中应启用 SSL。
2. 云数据库通常提供 SSL 配置，应按云厂商文档接入。
3. 证书和私钥文件应限制访问权限。
4. 应定期轮换证书。
5. 应验证应用连接池是否正确启用 SSL。
6. SSL 只能保护传输链路，不能替代账号权限和数据加密。

### SQL 注入防护

SQL 注入是指攻击者通过构造特殊输入改变 SQL 语义，从而绕过权限、读取敏感数据、篡改数据甚至执行危险操作。SQL 注入防护应主要在应用层完成，数据库层权限收敛可以降低注入后的破坏范围。

危险拼接示例：

```sql
-- 危险示例：如果 username 来自用户输入，拼接 SQL 可能导致注入
-- SELECT id, username, nickname
-- FROM sys_user
-- WHERE username = '" + username + "'
--   AND password = '" + password + "';
```

攻击输入示例：

```text
username = admin' OR '1'='1
password = anything
```

安全做法是使用预编译参数，不拼接用户输入。以 SQL 语义表达如下：

```sql
-- 安全原则：使用参数绑定，而不是字符串拼接
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username = ?
  AND password_hash = ?;
```

Java MyBatis 示例：

```xml
<!-- 推荐：使用 #{username} 参数绑定，避免 SQL 注入 -->
<select id="selectByUsername" resultType="io.github.atengk.user.entity.SysUser">
    SELECT
        id,
        username,
        nickname,
        status,
        created_at
    FROM sys_user
    WHERE username = #{username}
      AND deleted = 0
</select>
```

MyBatis 中不推荐 `${}` 接收用户输入：

```xml
<!-- 不推荐：${} 是字符串替换，用户输入可能改变 SQL 结构 -->
<select id="unsafeQuery" resultType="io.github.atengk.user.entity.SysUser">
    SELECT
        id,
        username,
        nickname
    FROM sys_user
    WHERE username = '${username}'
</select>
```

如果必须动态排序，应使用白名单：

```text
允许排序字段白名单：
- created_at
- updated_at
- username

允许排序方向白名单：
- ASC
- DESC

用户传入其他值时直接拒绝。
```

SQL 注入防护建议：

1. 应用层必须使用预编译参数绑定。
2. MyBatis 中优先使用 `#{}`，不要用 `${}` 拼接用户输入。
3. 动态表名、字段名、排序字段必须使用白名单校验。
4. 应用账号遵循最小权限原则，降低注入后的影响范围。
5. 禁止应用账号拥有 `DROP`、`ALTER`、`GRANT OPTION` 等高危权限。
6. 对登录、搜索、导出、报表、自定义查询等接口重点做安全测试。

### 敏感数据存储

敏感数据包括密码、手机号、身份证号、银行卡号、邮箱、地址、访问令牌、密钥、支付信息等。敏感数据存储应根据数据类型选择哈希、加密、脱敏、权限隔离和审计策略。

常见敏感数据处理方式：

| 数据类型 | 推荐处理方式           | 说明                 |
| -------- | ---------------------- | -------------------- |
| 密码     | BCrypt、Argon2、PBKDF2 | 只能校验，不能解密   |
| 手机号   | 加密存储 + 脱敏展示    | 支持有限查询         |
| 身份证号 | 加密存储 + 严格授权    | 高敏感数据           |
| 银行卡号 | 加密或托管支付机构     | 避免直接存储完整卡号 |
| 访问令牌 | 哈希或加密             | 避免明文泄露         |
| API 密钥 | 加密存储 + 权限隔离    | 不写入代码仓库       |
| 地址     | 视业务敏感等级加密     | 查询和展示分权       |

密码字段设计示例：

```sql
-- 密码只存储哈希值，不存储明文密码
CREATE TABLE IF NOT EXISTS secure_user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    mobile_cipher VARCHAR(512) DEFAULT NULL COMMENT '手机号密文',
    mobile_mask VARCHAR(20) DEFAULT NULL COMMENT '手机号脱敏值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全用户账户表';
```

手机号脱敏值示例：

```sql
-- 查询时展示脱敏手机号
SELECT
    id,
    username,
    mobile_mask,
    status
FROM secure_user_account
WHERE status = 1;
```

不推荐直接存储明文密码：

```sql
-- 不推荐：不要存储明文密码
-- password VARCHAR(100) NOT NULL COMMENT '明文密码'
```

敏感数据存储建议：

1. 密码必须使用专业密码哈希算法，不可明文存储。
2. 手机号、身份证号、银行卡号等应按敏感等级加密或脱敏。
3. 加密密钥不应存放在数据库同一位置。
4. 敏感字段查询权限应严格控制。
5. 日志、审计、导出文件中不能输出完整敏感数据。
6. 测试环境不应使用未脱敏的生产数据。

### 数据脱敏

数据脱敏用于在查询、展示、导出、日志和测试数据中隐藏敏感信息。脱敏不等于加密，脱敏后的数据通常不能还原，适合展示和低敏场景。

手机号脱敏：

```sql
-- 手机号脱敏：138****0001
SELECT
    id,
    username,
    mobile,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask
FROM sys_user
WHERE mobile IS NOT NULL;
```

邮箱脱敏：

```sql
-- 邮箱脱敏：保留前2位和域名
SELECT
    email,
    CONCAT(
        LEFT(email, 2),
        '****',
        SUBSTRING(email, LOCATE('@', email))
    ) AS email_mask
FROM sys_user
WHERE email IS NOT NULL
  AND LOCATE('@', email) > 0;
```

身份证号脱敏：

```sql
-- 身份证号脱敏：保留前6位和后4位
SELECT
    id_card,
    CONCAT(LEFT(id_card, 6), '********', RIGHT(id_card, 4)) AS id_card_mask
FROM user_identity
WHERE id_card IS NOT NULL;
```

银行卡号脱敏：

```sql
-- 银行卡号脱敏：只保留后4位
SELECT
    bank_card_no,
    CONCAT('**** **** **** ', RIGHT(bank_card_no, 4)) AS bank_card_mask
FROM user_bank_card
WHERE bank_card_no IS NOT NULL;
```

创建脱敏视图：

```sql
-- 创建脱敏视图，只暴露脱敏后的手机号和邮箱
CREATE OR REPLACE VIEW v_secure_user_account AS
SELECT
    id,
    username,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask,
    CONCAT(
        LEFT(email, 2),
        '****',
        SUBSTRING(email, LOCATE('@', email))
    ) AS email_mask,
    status,
    created_at
FROM sys_user
WHERE deleted = 0;
```

只给报表用户授权访问脱敏视图：

```sql
-- 授予报表用户查询脱敏视图的权限
GRANT SELECT
ON mall_order.v_secure_user_account
TO 'report_user'@'192.168.1.%';
```

脱敏建议：

1. 展示层、导出层、日志层都应脱敏。
2. 不同角色看到的数据脱敏程度可以不同。
3. 报表和运营查询优先访问脱敏视图。
4. 测试环境使用生产数据前必须脱敏。
5. 脱敏不能替代加密，敏感原文仍需安全存储。
6. 数据导出应记录审计日志。

### 审计日志

审计日志用于记录数据库访问和操作行为，便于追踪异常操作、权限滥用、误操作、数据泄露和安全事件。MySQL 社区版能力有限，企业版、云数据库或第三方审计系统通常提供更完整的审计能力。

常见审计内容：

| 审计项         | 说明                        |
| -------------- | --------------------------- |
| 登录成功和失败 | 账号、来源 IP、时间         |
| DDL 操作       | `CREATE`、`ALTER`、`DROP`   |
| DCL 操作       | `GRANT`、`REVOKE`、用户管理 |
| 高危 DML       | 无条件 `UPDATE`、`DELETE`   |
| 敏感表查询     | 用户、订单、支付、身份信息  |
| 数据导出       | 大批量查询、导出任务        |
| 权限变更       | 高权限授权和回收            |

查看通用日志配置：

```sql
-- 查看通用日志配置，不建议生产环境长期全量开启
SHOW VARIABLES LIKE 'general_log';
SHOW VARIABLES LIKE 'general_log_file';
```

临时开启通用日志：

```sql
-- 临时开启通用日志，排查结束后应关闭
SET GLOBAL general_log = 'ON';

-- 查看日志文件路径
SHOW VARIABLES LIKE 'general_log_file';
```

关闭通用日志：

```sql
-- 关闭通用日志，避免大量日志影响性能和磁盘
SET GLOBAL general_log = 'OFF';
```

查看二进制日志配置：

```sql
-- 查看 binlog 是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看 binlog 格式
SHOW VARIABLES LIKE 'binlog_format';
```

审计建议：

1. 生产环境不建议长期全量开启 general log，性能和磁盘成本较高。
2. 应优先使用云数据库审计、企业版审计或专门审计系统。
3. 高危操作应通过变更系统执行并留痕。
4. 敏感表查询和导出应记录操作人、来源、时间和条件。
5. 审计日志应防篡改，并设置合理保存周期。
6. 发现异常访问时，应能快速定位账号、IP、SQL 和影响数据范围。

### 安全基线配置

安全基线配置是生产 MySQL 的最低安全要求集合。它覆盖账号、权限、网络、密码、日志、备份、加密和运维流程。安全基线应在数据库初始化、上线前检查和定期巡检中执行。

账号与权限基线：

```text
1. 禁止应用使用 root 账号
2. 禁止 root 远程登录
3. 删除匿名用户
4. 删除无用测试账号
5. 应用账号只授予必要 DML 权限
6. 只读账号和读写账号分离
7. 高危权限需要审批
8. 定期审计 SHOW GRANTS
```

网络访问基线：

```text
1. MySQL 不暴露公网
2. 只允许应用服务器、堡垒机、备份服务器访问
3. 安全组和防火墙限制来源 IP
4. MySQL 用户 host 不使用 %
5. 管理连接走 VPN 或堡垒机
6. 跨网络访问启用 SSL
```

密码与认证基线：

```text
1. 密码长度不少于 12 位
2. 密码包含大小写字母、数字和特殊字符
3. 不同环境密码不同
4. 密码不进入代码仓库
5. 定期轮换生产密码
6. 离职和系统下线后及时锁定或删除账号
```

日志与审计基线：

```text
1. 开启错误日志
2. 开启慢查询日志
3. 关键系统启用审计能力
4. 高危操作必须走变更流程
5. 敏感表查询和导出应留痕
6. 审计日志应有保存周期和访问控制
```

数据保护基线：

```text
1. 密码只存储哈希值
2. 手机号、身份证号、银行卡号按敏感级别加密或脱敏
3. 测试环境不使用未脱敏生产数据
4. 备份文件加密存储
5. 备份文件访问受控
6. 定期验证备份可恢复
```

常用安全检查 SQL：

```sql
-- 检查 root 远程登录账号
SELECT
    user,
    host
FROM mysql.user
WHERE user = 'root'
  AND host NOT IN ('localhost', '127.0.0.1', '::1');

-- 检查匿名用户
SELECT
    user,
    host
FROM mysql.user
WHERE user = '';

-- 检查允许任意来源连接的账号
SELECT
    user,
    host
FROM mysql.user
WHERE host = '%';

-- 查看用户权限
SHOW GRANTS FOR 'mall_app'@'192.168.1.%';

-- 查看当前连接来源
SELECT
    user,
    host,
    COUNT(*) AS connection_count
FROM information_schema.PROCESSLIST
GROUP BY user, host
ORDER BY connection_count DESC;
```

常用安全配置示例：

```ini
[mysqld]
# 只监听内网地址，避免公网暴露
bind-address=10.20.30.10

# 开启错误日志
log_error=/var/log/mysql/error.log

# 开启慢查询日志
slow_query_log=ON

# 慢查询阈值，单位秒
long_query_time=1

# 慢查询日志路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 禁止本地文件导入导出能力，降低 LOAD DATA LOCAL 风险
local_infile=0

# 限制单个通信包大小，按业务需要调整
max_allowed_packet=64M
```

安全基线落地建议：

1. 初始化数据库时执行安全检查。
2. 上线前检查账号、权限、网络和日志配置。
3. 定期导出权限清单并审计。
4. 高危 SQL 和权限变更必须走审批。
5. 开发、测试、生产账号和数据严格隔离。
6. 备份、审计、监控和告警必须纳入安全体系。
7. 安全配置不能只依赖数据库，还要结合应用、网络、主机和运维流程。


## 视图

视图是基于 SQL 查询结果定义的虚拟表。它本身通常不直接存储数据，而是保存查询定义。使用视图可以封装复杂查询、统一字段口径、控制字段访问权限、提供脱敏数据出口，也可以降低应用层 SQL 的复杂度。

本节示例默认使用以下表：

```sql
-- 用户表，用于演示视图查询、脱敏视图和权限控制
CREATE TABLE IF NOT EXISTS view_sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视图用户表示例';

-- 订单表，用于演示聚合视图
CREATE TABLE IF NOT EXISTS view_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视图订单表示例';
```

### 创建视图

创建视图使用 `CREATE VIEW`。视图可以基于单表查询，也可以基于多表关联、聚合统计或字段表达式。创建视图时建议显式指定字段名，并给表达式字段设置清晰别名。

创建普通用户视图：

```sql
-- 创建用户基础信息视图，只暴露未删除用户
CREATE OR REPLACE VIEW v_user_base AS
SELECT
    id,
    username,
    nickname,
    status,
    created_at
FROM view_sys_user
WHERE deleted = 0;
```

创建脱敏视图：

```sql
-- 创建用户脱敏视图，隐藏完整手机号和邮箱
CREATE OR REPLACE VIEW v_user_mask AS
SELECT
    id,
    username,
    nickname,
    CASE
        WHEN mobile IS NULL OR CHAR_LENGTH(mobile) < 7 THEN NULL
        ELSE CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4))
    END AS mobile_mask,
    CASE
        WHEN email IS NULL OR LOCATE('@', email) <= 1 THEN NULL
        ELSE CONCAT(LEFT(email, 2), '****', SUBSTRING(email, LOCATE('@', email)))
    END AS email_mask,
    status,
    created_at
FROM view_sys_user
WHERE deleted = 0;
```

创建用户订单统计视图：

```sql
-- 创建用户订单统计视图
CREATE OR REPLACE VIEW v_user_order_summary AS
SELECT
    u.id AS user_id,
    u.username AS username,
    u.nickname AS nickname,
    COUNT(o.id) AS order_count,
    COALESCE(SUM(CASE WHEN o.order_status = 1 THEN o.pay_amount ELSE 0 END), 0.00) AS paid_amount,
    MAX(o.created_at) AS latest_order_time
FROM view_sys_user AS u
LEFT JOIN view_order_info AS o ON u.id = o.user_id
WHERE u.deleted = 0
GROUP BY
    u.id,
    u.username,
    u.nickname;
```

查看视图创建语句：

```sql
-- 查看视图定义
SHOW CREATE VIEW v_user_base;
```

创建视图建议：

1. 视图名称建议使用 `v_` 前缀，例如 `v_user_base`、`v_order_summary`。
2. 视图字段应明确列出，不建议使用 `SELECT *`。
3. 视图中的表达式字段必须设置清晰别名。
4. 脱敏视图适合提供给报表、运营、只读账号使用。
5. 聚合视图适合统一统计口径，但不适合替代高性能汇总表。
6. 复杂视图嵌套过多会影响可读性和性能，应谨慎使用。

### 查询视图

查询视图与查询普通表类似，可以使用 `SELECT`、`WHERE`、`ORDER BY`、`LIMIT` 等语法。视图只是封装了底层查询逻辑，最终仍然需要访问底层表。

查询用户基础视图：

```sql
-- 查询启用状态用户
SELECT
    id,
    username,
    nickname,
    status,
    created_at
FROM v_user_base
WHERE status = 1
ORDER BY created_at DESC
LIMIT 20;
```

查询脱敏视图：

```sql
-- 查询用户脱敏信息
SELECT
    id,
    username,
    nickname,
    mobile_mask,
    email_mask,
    status
FROM v_user_mask
WHERE status = 1;
```

查询用户订单统计视图：

```sql
-- 查询累计支付金额大于 1000 的用户
SELECT
    user_id,
    username,
    nickname,
    order_count,
    paid_amount,
    latest_order_time
FROM v_user_order_summary
WHERE paid_amount >= 1000.00
ORDER BY paid_amount DESC;
```

查看当前数据库中的视图：

```sql
-- 查看当前数据库中的视图
SHOW FULL TABLES
WHERE Table_type = 'VIEW';
```

通过 `information_schema` 查看视图：

```sql
-- 查看视图定义信息
SELECT
    TABLE_SCHEMA,
    TABLE_NAME,
    VIEW_DEFINITION,
    DEFINER,
    SECURITY_TYPE
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = DATABASE();
```

查询视图建议：

1. 查询视图时仍应关注底层表索引。
2. 视图不会自动提升查询性能，复杂视图可能比直接 SQL 更难优化。
3. 聚合视图在大表上查询可能较慢，应结合执行计划分析。
4. 权限隔离场景中，应用或报表用户可以只访问视图，不直接访问底层表。
5. 不建议在视图上再叠加过多复杂视图，避免 SQL 难以维护。

### 修改视图

修改视图可以使用 `CREATE OR REPLACE VIEW` 或 `ALTER VIEW`。实际项目中更常用 `CREATE OR REPLACE VIEW`，因为它既能创建新视图，也能替换已有视图。

使用 `CREATE OR REPLACE VIEW` 修改视图：

```sql
-- 修改用户基础视图，增加 updated_at 字段
CREATE OR REPLACE VIEW v_user_base AS
SELECT
    id,
    username,
    nickname,
    status,
    created_at,
    updated_at
FROM view_sys_user
WHERE deleted = 0;
```

使用 `ALTER VIEW` 修改视图：

```sql
-- 使用 ALTER VIEW 修改视图定义
ALTER VIEW v_user_base AS
SELECT
    id,
    username,
    nickname,
    mobile,
    status,
    created_at,
    updated_at
FROM view_sys_user
WHERE deleted = 0;
```

修改脱敏规则：

```sql
-- 修改手机号脱敏规则，只保留后四位
CREATE OR REPLACE VIEW v_user_mask AS
SELECT
    id,
    username,
    nickname,
    CONCAT('****', RIGHT(mobile, 4)) AS mobile_mask,
    CASE
        WHEN email IS NULL OR LOCATE('@', email) <= 1 THEN NULL
        ELSE CONCAT(LEFT(email, 1), '****', SUBSTRING(email, LOCATE('@', email)))
    END AS email_mask,
    status,
    created_at
FROM view_sys_user
WHERE deleted = 0;
```

修改后验证：

```sql
-- 验证视图字段和数据
SELECT
    *
FROM v_user_base
LIMIT 5;

-- 查看修改后的视图定义
SHOW CREATE VIEW v_user_base;
```

修改视图建议：

1. 修改视图前应确认是否有应用、报表、任务依赖该视图字段。
2. 删除或重命名视图字段属于破坏性变更，应提前评估影响。
3. 修改脱敏规则后，应验证不同角色查询结果。
4. 视图变更应纳入数据库变更脚本管理。
5. 生产环境修改复杂视图后，应检查执行计划和查询耗时。

### 删除视图

删除视图使用 `DROP VIEW`。删除视图不会删除底层表数据，但会影响依赖该视图的应用、报表、存储过程或其他视图。

删除视图：

```sql
-- 删除用户基础视图
DROP VIEW v_user_base;
```

如果存在则删除：

```sql
-- 如果视图存在，则删除
DROP VIEW IF EXISTS v_user_base;
```

同时删除多个视图：

```sql
-- 删除多个视图
DROP VIEW IF EXISTS v_user_base, v_user_mask, v_user_order_summary;
```

删除前检查视图：

```sql
-- 查看当前数据库中的视图
SHOW FULL TABLES
WHERE Table_type = 'VIEW';

-- 查看视图定义
SHOW CREATE VIEW v_user_order_summary;
```

删除视图建议：

1. 删除前确认没有应用、报表和任务依赖。
2. 生产环境建议先下线调用方，再删除视图。
3. 删除视图不会删除底层表。
4. 如果视图用于权限隔离，删除后应同步调整用户权限。
5. 删除操作应纳入变更记录。

### 可更新视图

可更新视图是指可以通过视图执行 `INSERT`、`UPDATE`、`DELETE`，并影响底层表。并不是所有视图都可更新。一般来说，基于单表、没有聚合、没有分组、没有 `DISTINCT`、没有复杂表达式的简单视图更可能可更新。

创建可更新视图：

```sql
-- 创建可更新视图，只暴露部分字段
CREATE OR REPLACE VIEW v_user_update AS
SELECT
    id,
    username,
    nickname,
    status
FROM view_sys_user
WHERE deleted = 0;
```

通过视图更新数据：

```sql
-- 通过视图修改用户昵称
UPDATE v_user_update
SET nickname = '张三更新'
WHERE id = 1;
```

通过视图插入数据：

```sql
-- 通过视图插入数据，底层表中未暴露字段必须有默认值或允许为空
INSERT INTO v_user_update (
    username,
    nickname,
    status
) VALUES (
    'view_insert_user',
    '视图插入用户',
    1
);
```

不可更新视图示例：

```sql
-- 聚合视图通常不可更新
CREATE OR REPLACE VIEW v_user_order_count AS
SELECT
    u.id AS user_id,
    u.username AS username,
    COUNT(o.id) AS order_count
FROM view_sys_user AS u
LEFT JOIN view_order_info AS o ON u.id = o.user_id
GROUP BY
    u.id,
    u.username;
```

以下更新通常不可执行：

```sql
-- 不可更新：聚合结果不是底层表的单行字段
UPDATE v_user_order_count
SET order_count = 10
WHERE user_id = 1;
```

可更新视图限制：

| 视图特征           | 是否通常可更新         |
| ------------------ | ---------------------- |
| 单表简单字段视图   | 通常可以               |
| 包含 `GROUP BY`    | 通常不可以             |
| 包含聚合函数       | 通常不可以             |
| 包含 `DISTINCT`    | 通常不可以             |
| 包含复杂表达式字段 | 表达式字段不可直接更新 |
| 多表关联视图       | 限制较多，谨慎使用     |
| 包含 `UNION`       | 通常不可以             |

可更新视图建议：

1. 业务系统中不建议大量通过视图写数据，容易隐藏真实写入表。
2. 可更新视图适合少量权限隔离和字段限制场景。
3. 写操作更推荐直接访问明确的业务表。
4. 如果使用可更新视图，应明确底层表字段默认值和约束。
5. 对聚合视图、关联视图、脱敏视图，不应设计写入操作。

### 视图权限控制

视图可以用于权限隔离。可以不给用户底层表权限，只授予视图查询权限，让用户只能看到视图暴露的字段和数据范围。该方式常用于报表账号、运营账号、外部查询账号和脱敏数据访问。

创建只读用户：

```sql
-- 创建报表只读用户
CREATE USER 'view_report'@'192.168.1.%'
IDENTIFIED BY 'View_Report_123456';
```

只授予视图查询权限：

```sql
-- 只允许查询脱敏视图，不允许直接查询底层表
GRANT SELECT
ON mall_order.v_user_mask
TO 'view_report'@'192.168.1.%';
```

不授予底层表权限：

```sql
-- 不给 view_sys_user 底层表权限，避免查询完整手机号和邮箱
-- GRANT SELECT ON mall_order.view_sys_user TO 'view_report'@'192.168.1.%';
```

查看权限：

```sql
-- 查看报表用户权限
SHOW GRANTS FOR 'view_report'@'192.168.1.%';
```

回收视图权限：

```sql
-- 回收视图查询权限
REVOKE SELECT
ON mall_order.v_user_mask
FROM 'view_report'@'192.168.1.%';
```

创建带安全上下文的视图：

```sql
-- 使用 SQL SECURITY DEFINER，视图按定义者权限执行
CREATE OR REPLACE
SQL SECURITY DEFINER
VIEW v_user_mask AS
SELECT
    id,
    username,
    nickname,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask,
    status,
    created_at
FROM view_sys_user
WHERE deleted = 0;
```

视图权限控制建议：

1. 报表、运营、外部查询账号优先访问脱敏视图。
2. 不要同时授予视图权限和底层敏感表权限，否则视图脱敏意义下降。
3. 视图字段应只暴露必要字段。
4. `SQL SECURITY DEFINER` 涉及定义者权限，应谨慎配置。
5. 删除或修改视图前，应确认依赖该视图的账号和业务。

### 视图使用场景

视图适合封装稳定查询逻辑、统一数据口径和控制访问范围。视图不是性能优化工具，不能替代索引、汇总表和缓存。

常见使用场景：

| 场景         | 说明                                 |
| ------------ | ------------------------------------ |
| 字段脱敏     | 只暴露手机号、邮箱、身份证号的脱敏值 |
| 权限隔离     | 用户只能访问视图，不能访问底层表     |
| 复杂查询封装 | 多表关联字段统一封装                 |
| 报表口径统一 | 统一统计字段和过滤条件               |
| 兼容旧系统   | 底层表结构变更后，通过视图兼容旧字段 |
| 数据范围限制 | 只暴露启用、未删除、指定状态数据     |

脱敏视图场景：

```sql
-- 给运营系统提供脱敏用户数据
CREATE OR REPLACE VIEW v_operation_user AS
SELECT
    id,
    username,
    nickname,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask,
    status,
    created_at
FROM view_sys_user
WHERE deleted = 0;
```

报表统计视图场景：

```sql
-- 给报表系统提供用户订单统计口径
CREATE OR REPLACE VIEW v_report_user_order AS
SELECT
    u.id AS user_id,
    u.username AS username,
    COUNT(o.id) AS order_count,
    SUM(CASE WHEN o.order_status = 1 THEN o.pay_amount ELSE 0 END) AS paid_amount
FROM view_sys_user AS u
LEFT JOIN view_order_info AS o ON u.id = o.user_id
WHERE u.deleted = 0
GROUP BY
    u.id,
    u.username;
```

视图使用建议：

1. 视图适合封装稳定逻辑，不适合承载频繁变化的复杂业务。
2. 性能敏感场景应优先考虑索引、汇总表、缓存或物化数据。
3. 视图中不要写过度复杂的嵌套查询。
4. 视图字段变更要考虑调用方兼容性。
5. 权限隔离场景中，视图非常适合提供受控数据出口。

## 存储过程

存储过程是一组保存在 MySQL 服务端的 SQL 语句集合，可以接收参数、声明变量、执行条件判断、循环、游标和异常处理。它适合封装批处理、数据修复、定时统计、复杂迁移和少量靠近数据端的逻辑。

在普通 Java 或 Spring Boot 业务系统中，不建议把核心业务逻辑大量写入存储过程。核心业务逻辑应优先放在应用层，便于测试、发布、版本管理和日志追踪。存储过程更适合数据库侧批处理、数据治理和管理任务。

本节示例默认使用以下表：

```sql
-- 用户统计表，用于演示存储过程写入统计结果
CREATE TABLE IF NOT EXISTS proc_user_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    summary_date DATE NOT NULL COMMENT '统计日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储过程用户统计表';

-- 订单表，用于演示存储过程统计
CREATE TABLE IF NOT EXISTS proc_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储过程订单表';
```

### 创建存储过程

创建存储过程使用 `CREATE PROCEDURE`。由于存储过程内部会包含多条 SQL，并且每条 SQL 以分号结束，因此在命令行中通常需要使用 `DELIMITER` 临时修改语句结束符。

创建一个简单存储过程：

```sql
-- 修改结束符，避免过程体中的分号提前结束 CREATE PROCEDURE
DELIMITER //

CREATE PROCEDURE proc_select_current_time()
BEGIN
    SELECT
        NOW() AS current_time;
END//

-- 恢复默认结束符
DELIMITER ;
```

创建用户订单统计存储过程：

```sql
-- 创建按日期统计用户订单数据的存储过程
DELIMITER //

CREATE PROCEDURE proc_refresh_user_summary(IN p_summary_date DATE)
BEGIN
    INSERT INTO proc_user_summary (
        user_id,
        order_count,
        paid_amount,
        summary_date
    )
    SELECT
        user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS paid_amount,
        p_summary_date AS summary_date
    FROM proc_order_info
    WHERE order_status = 1
      AND created_at >= p_summary_date
      AND created_at < DATE_ADD(p_summary_date, INTERVAL 1 DAY)
    GROUP BY user_id
    ON DUPLICATE KEY UPDATE
        order_count = VALUES(order_count),
        paid_amount = VALUES(paid_amount),
        updated_at = NOW();
END//

DELIMITER ;
```

查看存储过程：

```sql
-- 查看当前数据库中的存储过程
SHOW PROCEDURE STATUS
WHERE Db = DATABASE();

-- 查看存储过程创建语句
SHOW CREATE PROCEDURE proc_refresh_user_summary;
```

创建存储过程建议：

1. 存储过程名称建议使用 `proc_` 前缀。
2. 参数名称建议使用 `p_` 前缀。
3. 局部变量建议使用 `v_` 前缀。
4. 过程体内部 SQL 应保持清晰，不要写过长逻辑。
5. 创建和修改存储过程应纳入数据库版本管理。
6. 命令行执行时注意 `DELIMITER` 设置。

### 调用存储过程

调用存储过程使用 `CALL`。如果过程有输入参数，需要传入对应参数；如果有输出参数，需要使用用户变量接收。

调用无参数存储过程：

```sql
-- 调用无参数存储过程
CALL proc_select_current_time();
```

调用带输入参数的存储过程：

```sql
-- 刷新指定日期的用户订单统计
CALL proc_refresh_user_summary('2026-01-01');
```

查询统计结果：

```sql
-- 查看统计结果
SELECT
    user_id,
    order_count,
    paid_amount,
    summary_date,
    updated_at
FROM proc_user_summary
WHERE summary_date = '2026-01-01'
ORDER BY paid_amount DESC;
```

在事务中调用存储过程：

```sql
-- 在事务中调用存储过程，是否需要事务取决于过程内部逻辑
START TRANSACTION;

CALL proc_refresh_user_summary('2026-01-01');

COMMIT;
```

调用存储过程建议：

1. 调用前确认参数类型和顺序。
2. 如果过程内部包含写操作，应明确事务边界。
3. 批处理过程建议记录执行日志、影响行数和异常信息。
4. 调用耗时较长的过程时，应避开业务高峰。
5. Java 应用调用存储过程时，应控制超时时间和异常处理。

### 输入参数

输入参数使用 `IN` 定义，调用方传入值，过程内部读取该值。输入参数适合传入日期、用户 ID、状态、批次号等条件。

创建带输入参数的过程：

```sql
-- 根据用户ID和日期范围统计订单金额
DELIMITER //

CREATE PROCEDURE proc_get_user_order_amount(
    IN p_user_id BIGINT,
    IN p_start_time DATETIME,
    IN p_end_time DATETIME
)
BEGIN
    SELECT
        p_user_id AS user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS paid_amount
    FROM proc_order_info
    WHERE user_id = p_user_id
      AND order_status = 1
      AND created_at >= p_start_time
      AND created_at < p_end_time;
END//

DELIMITER ;
```

调用：

```sql
-- 查询指定用户在指定时间范围内的订单统计
CALL proc_get_user_order_amount(
    10001,
    '2026-01-01 00:00:00',
    '2026-02-01 00:00:00'
);
```

输入参数校验示例：

```sql
-- 带参数校验的统计过程
DELIMITER //

CREATE PROCEDURE proc_get_user_order_amount_safe(
    IN p_user_id BIGINT,
    IN p_start_time DATETIME,
    IN p_end_time DATETIME
)
BEGIN
    IF p_user_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '用户ID不能为空';
    END IF;

    IF p_start_time IS NULL OR p_end_time IS NULL OR p_start_time >= p_end_time THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '时间范围不合法';
    END IF;

    SELECT
        p_user_id AS user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS paid_amount
    FROM proc_order_info
    WHERE user_id = p_user_id
      AND order_status = 1
      AND created_at >= p_start_time
      AND created_at < p_end_time;
END//

DELIMITER ;
```

输入参数建议：

1. 输入参数名称使用 `p_` 前缀，避免与字段名混淆。
2. 参数类型应与表字段类型一致。
3. 对关键参数做合法性校验。
4. 日期范围建议使用左闭右开。
5. 不建议把复杂 JSON 或超长字符串作为存储过程参数进行大量业务处理。

### 输出参数

输出参数使用 `OUT` 定义，过程内部赋值，调用方通过用户变量接收。输出参数适合返回计算结果、状态码、错误信息、影响行数等。

创建带输出参数的过程：

```sql
-- 查询用户累计支付金额，并通过 OUT 参数返回
DELIMITER //

CREATE PROCEDURE proc_calc_user_paid_amount(
    IN p_user_id BIGINT,
    OUT p_order_count INT,
    OUT p_paid_amount DECIMAL(18,2)
)
BEGIN
    SELECT
        COUNT(*),
        COALESCE(SUM(pay_amount), 0.00)
    INTO
        p_order_count,
        p_paid_amount
    FROM proc_order_info
    WHERE user_id = p_user_id
      AND order_status = 1;
END//

DELIMITER ;
```

调用并读取输出参数：

```sql
-- 调用过程并接收输出参数
CALL proc_calc_user_paid_amount(10001, @order_count, @paid_amount);

-- 查看输出参数
SELECT
    @order_count AS order_count,
    @paid_amount AS paid_amount;
```

输出状态码和消息：

```sql
-- 返回执行状态和消息
DELIMITER //

CREATE PROCEDURE proc_check_user_order(
    IN p_user_id BIGINT,
    OUT p_result_code VARCHAR(20),
    OUT p_result_msg VARCHAR(200)
)
BEGIN
    DECLARE v_order_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_order_count
    FROM proc_order_info
    WHERE user_id = p_user_id;

    IF v_order_count > 0 THEN
        SET p_result_code = 'SUCCESS';
        SET p_result_msg = '用户存在订单';
    ELSE
        SET p_result_code = 'EMPTY';
        SET p_result_msg = '用户暂无订单';
    END IF;
END//

DELIMITER ;
```

调用：

```sql
-- 调用并查看返回状态
CALL proc_check_user_order(10001, @result_code, @result_msg);

SELECT
    @result_code AS result_code,
    @result_msg AS result_msg;
```

输出参数建议：

1. 输出参数适合返回少量结果。
2. 多行结果更适合直接 `SELECT` 返回结果集。
3. 输出参数名称使用 `p_` 前缀即可，也可以使用 `out_` 前缀保持区分。
4. 输出参数赋值前应考虑空结果场景。
5. 应用层调用时要正确注册输出参数类型。

### 变量声明

存储过程中可以使用 `DECLARE` 声明局部变量。变量必须声明在过程块的开头位置，通常在游标和异常处理声明之前或附近按语法要求组织。

声明变量基础语法：

```sql
-- 变量声明语法
DECLARE v_variable_name data_type DEFAULT default_value;
```

变量使用示例：

```sql
-- 使用局部变量计算用户订单统计
DELIMITER //

CREATE PROCEDURE proc_variable_demo(IN p_user_id BIGINT)
BEGIN
    DECLARE v_order_count INT DEFAULT 0;
    DECLARE v_paid_amount DECIMAL(18,2) DEFAULT 0.00;
    DECLARE v_user_level VARCHAR(20) DEFAULT 'NORMAL';

    SELECT
        COUNT(*),
        COALESCE(SUM(pay_amount), 0.00)
    INTO
        v_order_count,
        v_paid_amount
    FROM proc_order_info
    WHERE user_id = p_user_id
      AND order_status = 1;

    IF v_paid_amount >= 10000.00 THEN
        SET v_user_level = 'VIP';
    ELSEIF v_paid_amount >= 1000.00 THEN
        SET v_user_level = 'ACTIVE';
    ELSE
        SET v_user_level = 'NORMAL';
    END IF;

    SELECT
        p_user_id AS user_id,
        v_order_count AS order_count,
        v_paid_amount AS paid_amount,
        v_user_level AS user_level;
END//

DELIMITER ;
```

调用：

```sql
-- 调用变量示例过程
CALL proc_variable_demo(10001);
```

变量赋值方式：

```sql
-- 使用 SET 赋值
SET v_order_count = 0;

-- 使用 SELECT ... INTO 赋值
SELECT COUNT(*)
INTO v_order_count
FROM proc_order_info
WHERE user_id = p_user_id;
```

变量使用建议：

1. 局部变量使用 `v_` 前缀。
2. 参数使用 `p_` 前缀。
3. 避免变量名与字段名相同。
4. 使用 `SELECT ... INTO` 时要确保返回单行结果。
5. 如果查询可能没有结果，应考虑异常处理或聚合函数兜底。

### 条件判断

条件判断用于在存储过程中根据参数、查询结果或业务状态执行不同逻辑。MySQL 存储过程常用 `IF ... THEN ... ELSEIF ... ELSE ... END IF` 和 `CASE`。

使用 `IF` 判断：

```sql
-- 根据用户支付金额判断用户等级
DELIMITER //

CREATE PROCEDURE proc_if_demo(IN p_user_id BIGINT)
BEGIN
    DECLARE v_paid_amount DECIMAL(18,2) DEFAULT 0.00;
    DECLARE v_user_level VARCHAR(20) DEFAULT 'NORMAL';

    SELECT COALESCE(SUM(pay_amount), 0.00)
    INTO v_paid_amount
    FROM proc_order_info
    WHERE user_id = p_user_id
      AND order_status = 1;

    IF v_paid_amount >= 10000.00 THEN
        SET v_user_level = 'VIP';
    ELSEIF v_paid_amount >= 1000.00 THEN
        SET v_user_level = 'ACTIVE';
    ELSE
        SET v_user_level = 'NORMAL';
    END IF;

    SELECT
        p_user_id AS user_id,
        v_paid_amount AS paid_amount,
        v_user_level AS user_level;
END//

DELIMITER ;
```

使用 `CASE` 判断：

```sql
-- 使用 CASE 根据订单状态返回状态名称
DELIMITER //

CREATE PROCEDURE proc_case_demo(IN p_order_no VARCHAR(64))
BEGIN
    DECLARE v_order_status TINYINT DEFAULT NULL;
    DECLARE v_status_name VARCHAR(50) DEFAULT '未知状态';

    SELECT order_status
    INTO v_order_status
    FROM proc_order_info
    WHERE order_no = p_order_no;

    SET v_status_name = CASE v_order_status
        WHEN 0 THEN '待支付'
        WHEN 1 THEN '已支付'
        WHEN 2 THEN '已取消'
        WHEN 3 THEN '已退款'
        ELSE '未知状态'
    END;

    SELECT
        p_order_no AS order_no,
        v_order_status AS order_status,
        v_status_name AS status_name;
END//

DELIMITER ;
```

参数校验并抛出异常：

```sql
-- 参数为空时主动抛出业务异常
DELIMITER //

CREATE PROCEDURE proc_validate_user_id(IN p_user_id BIGINT)
BEGIN
    IF p_user_id IS NULL OR p_user_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '用户ID不合法';
    END IF;

    SELECT
        p_user_id AS user_id,
        '参数校验通过' AS message;
END//

DELIMITER ;
```

条件判断建议：

1. 简单分支使用 `IF`。
2. 多状态值映射可以使用 `CASE`。
3. 参数非法时可以使用 `SIGNAL SQLSTATE '45000'` 主动抛错。
4. 不要在存储过程中堆叠过多复杂业务分支。
5. 状态判断应与业务状态机保持一致。

### 循环控制

循环控制用于重复执行一段逻辑。MySQL 存储过程支持 `LOOP`、`WHILE`、`REPEAT` 等循环结构。循环常用于批量处理、分批迁移、生成日期数据等场景。

使用 `WHILE` 循环生成日期统计：

```sql
-- 使用 WHILE 循环按天刷新用户订单统计
DELIMITER //

CREATE PROCEDURE proc_refresh_summary_range(
    IN p_start_date DATE,
    IN p_end_date DATE
)
BEGIN
    DECLARE v_current_date DATE;

    IF p_start_date IS NULL OR p_end_date IS NULL OR p_start_date > p_end_date THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '日期范围不合法';
    END IF;

    SET v_current_date = p_start_date;

    WHILE v_current_date <= p_end_date DO
        CALL proc_refresh_user_summary(v_current_date);

        SET v_current_date = DATE_ADD(v_current_date, INTERVAL 1 DAY);
    END WHILE;
END//

DELIMITER ;
```

调用：

```sql
-- 刷新 2026-01-01 到 2026-01-07 的统计数据
CALL proc_refresh_summary_range('2026-01-01', '2026-01-07');
```

使用 `LOOP` 循环：

```sql
-- 使用 LOOP 生成简单序列
DELIMITER //

CREATE PROCEDURE proc_loop_demo(IN p_max_num INT)
BEGIN
    DECLARE v_num INT DEFAULT 1;

    loop_label: LOOP
        IF v_num > p_max_num THEN
            LEAVE loop_label;
        END IF;

        SELECT v_num AS current_num;

        SET v_num = v_num + 1;
    END LOOP;
END//

DELIMITER ;
```

使用 `REPEAT` 循环：

```sql
-- 使用 REPEAT 循环，至少执行一次
DELIMITER //

CREATE PROCEDURE proc_repeat_demo(IN p_max_num INT)
BEGIN
    DECLARE v_num INT DEFAULT 1;

    REPEAT
        SELECT v_num AS current_num;

        SET v_num = v_num + 1;
    UNTIL v_num > p_max_num
    END REPEAT;
END//

DELIMITER ;
```

循环控制建议：

1. 必须有明确退出条件，避免死循环。
2. 大批量处理应分批提交，避免大事务。
3. 循环内不要执行高成本单行 SQL，能集合处理时优先集合处理。
4. 循环适合小批量管理任务，不适合替代应用层批处理框架。
5. 循环执行过程建议记录批次、执行时间和影响行数。

### 游标使用

游标用于逐行读取查询结果，并在存储过程中逐行处理。游标适合结果集较小、必须逐行处理的数据库侧任务。对于大数据量处理，应优先使用集合 SQL 或应用层批处理。

游标使用通常包括：

```text
1. 声明变量
2. 声明游标
3. 声明结束处理器
4. 打开游标
5. 循环读取游标
6. 处理当前行
7. 关闭游标
```

使用游标遍历用户订单统计：

```sql
-- 使用游标逐个处理用户订单统计
DELIMITER //

CREATE PROCEDURE proc_cursor_user_summary(IN p_summary_date DATE)
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE v_order_count INT DEFAULT 0;
    DECLARE v_paid_amount DECIMAL(18,2) DEFAULT 0.00;

    -- 声明游标，查询指定日期内有已支付订单的用户统计
    DECLARE cur_user_summary CURSOR FOR
        SELECT
            user_id,
            COUNT(*) AS order_count,
            COALESCE(SUM(pay_amount), 0.00) AS paid_amount
        FROM proc_order_info
        WHERE order_status = 1
          AND created_at >= p_summary_date
          AND created_at < DATE_ADD(p_summary_date, INTERVAL 1 DAY)
        GROUP BY user_id;

    -- 游标读取结束时设置标记
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur_user_summary;

    read_loop: LOOP
        FETCH cur_user_summary
        INTO v_user_id, v_order_count, v_paid_amount;

        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO proc_user_summary (
            user_id,
            order_count,
            paid_amount,
            summary_date
        ) VALUES (
            v_user_id,
            v_order_count,
            v_paid_amount,
            p_summary_date
        )
        ON DUPLICATE KEY UPDATE
            order_count = VALUES(order_count),
            paid_amount = VALUES(paid_amount),
            updated_at = NOW();
    END LOOP;

    CLOSE cur_user_summary;
END//

DELIMITER ;
```

调用：

```sql
-- 使用游标方式刷新用户统计
CALL proc_cursor_user_summary('2026-01-01');
```

同样逻辑的集合 SQL 更简单：

```sql
-- 推荐：能用集合 SQL 完成时，优先使用集合 SQL
INSERT INTO proc_user_summary (
    user_id,
    order_count,
    paid_amount,
    summary_date
)
SELECT
    user_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS paid_amount,
    '2026-01-01' AS summary_date
FROM proc_order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01'
  AND created_at <  '2026-01-02'
GROUP BY user_id
ON DUPLICATE KEY UPDATE
    order_count = VALUES(order_count),
    paid_amount = VALUES(paid_amount),
    updated_at = NOW();
```

游标使用建议：

1. 能用集合 SQL 解决时，不使用游标。
2. 游标逐行处理性能较差，不适合大结果集。
3. 游标必须正确关闭。
4. 必须声明 `NOT FOUND` 处理器，避免读取结束时报错。
5. 游标逻辑复杂时，应考虑应用层批处理或任务系统。

### 异常处理

异常处理用于在存储过程中捕获 SQL 异常、回滚事务、返回错误信息或记录日志。MySQL 存储过程可以使用 `DECLARE HANDLER` 定义异常处理逻辑。

创建执行日志表：

```sql
-- 存储过程执行日志表
CREATE TABLE IF NOT EXISTS proc_execute_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    proc_name VARCHAR(100) NOT NULL COMMENT '存储过程名称',
    execute_status VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS成功，FAIL失败',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_proc_name_created_at (proc_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储过程执行日志表';
```

异常处理示例：

```sql
-- 带异常处理的统计刷新过程
DELIMITER //

CREATE PROCEDURE proc_refresh_user_summary_safe(IN p_summary_date DATE)
BEGIN
    DECLARE v_error_message VARCHAR(1000) DEFAULT NULL;

    -- 捕获 SQL 异常
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1
            v_error_message = MESSAGE_TEXT;

        ROLLBACK;

        INSERT INTO proc_execute_log (
            proc_name,
            execute_status,
            error_message
        ) VALUES (
            'proc_refresh_user_summary_safe',
            'FAIL',
            v_error_message
        );

        RESIGNAL;
    END;

    IF p_summary_date IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '统计日期不能为空';
    END IF;

    START TRANSACTION;

    INSERT INTO proc_user_summary (
        user_id,
        order_count,
        paid_amount,
        summary_date
    )
    SELECT
        user_id,
        COUNT(*) AS order_count,
        COALESCE(SUM(pay_amount), 0.00) AS paid_amount,
        p_summary_date AS summary_date
    FROM proc_order_info
    WHERE order_status = 1
      AND created_at >= p_summary_date
      AND created_at < DATE_ADD(p_summary_date, INTERVAL 1 DAY)
    GROUP BY user_id
    ON DUPLICATE KEY UPDATE
        order_count = VALUES(order_count),
        paid_amount = VALUES(paid_amount),
        updated_at = NOW();

    INSERT INTO proc_execute_log (
        proc_name,
        execute_status,
        error_message
    ) VALUES (
        'proc_refresh_user_summary_safe',
        'SUCCESS',
        NULL
    );

    COMMIT;
END//

DELIMITER ;
```

调用：

```sql
-- 调用带异常处理的过程
CALL proc_refresh_user_summary_safe('2026-01-01');
```

查看执行日志：

```sql
-- 查看过程执行日志
SELECT
    proc_name,
    execute_status,
    error_message,
    created_at
FROM proc_execute_log
WHERE proc_name = 'proc_refresh_user_summary_safe'
ORDER BY created_at DESC
LIMIT 20;
```

常见 Handler 类型：

| Handler            | 说明                       |
| ------------------ | -------------------------- |
| `CONTINUE HANDLER` | 捕获异常后继续执行后续逻辑 |
| `EXIT HANDLER`     | 捕获异常后退出当前代码块   |
| `SQLEXCEPTION`     | 捕获 SQL 异常              |
| `SQLWARNING`       | 捕获 SQL 警告              |
| `NOT FOUND`        | 常用于游标读取结束         |

异常处理建议：

1. 写操作存储过程应明确事务和异常回滚。
2. 异常处理器中可以记录执行日志。
3. 使用 `RESIGNAL` 可以在记录日志后继续抛出异常。
4. 参数非法时使用 `SIGNAL SQLSTATE '45000'` 主动抛错。
5. 不要吞掉异常后假装执行成功，否则排查成本很高。
6. 复杂异常处理更适合应用层完成，存储过程只保留必要处理。

### 删除存储过程

删除存储过程使用 `DROP PROCEDURE`。删除过程不会删除表数据，但会影响依赖该过程的定时任务、应用接口、运维脚本或事件调度器。

删除存储过程：

```sql
-- 删除指定存储过程
DROP PROCEDURE proc_select_current_time;
```

如果存在则删除：

```sql
-- 如果存储过程存在，则删除
DROP PROCEDURE IF EXISTS proc_select_current_time;
```

删除前查看存储过程：

```sql
-- 查看当前数据库中的存储过程
SHOW PROCEDURE STATUS
WHERE Db = DATABASE();

-- 查看过程定义
SHOW CREATE PROCEDURE proc_refresh_user_summary;
```

删除多个过程需要分别执行：

```sql
-- 删除多个存储过程
DROP PROCEDURE IF EXISTS proc_get_user_order_amount;
DROP PROCEDURE IF EXISTS proc_variable_demo;
DROP PROCEDURE IF EXISTS proc_cursor_user_summary;
```

删除存储过程建议：

1. 删除前确认没有应用、任务或脚本依赖。
2. 生产环境删除前应备份过程定义。
3. 存储过程变更应纳入数据库版本管理。
4. 删除后应验证相关定时任务和调用方。
5. 对废弃过程可先停止调用，再观察一段时间，最后删除。


## 存储函数

存储函数是保存在 MySQL 服务端的函数对象，可以接收参数并返回一个值。它适合封装简单、稳定、可复用的计算逻辑，例如状态值转换、脱敏处理、金额计算、编码格式化等。与存储过程不同，存储函数必须有返回值，并且可以在 `SELECT`、`WHERE`、`ORDER BY` 等 SQL 表达式中调用。

在 Java 或 Spring Boot 项目中，不建议把复杂业务逻辑大量写入存储函数。存储函数更适合轻量计算和通用转换，核心业务规则应优先放在应用层，便于测试、发布、日志追踪和版本管理。

本节示例默认使用以下表：

```sql
-- 订单表示例，用于演示存储函数调用
CREATE TABLE IF NOT EXISTS func_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    discount_rate DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '折扣率',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储函数订单示例表';
```

### 创建存储函数

创建存储函数使用 `CREATE FUNCTION`。函数必须声明返回类型，并通过 `RETURN` 返回结果。由于函数体内部包含分号，命令行创建时通常需要使用 `DELIMITER` 临时修改语句结束符。

下面函数用于将订单状态码转换为中文状态名称：

```sql
-- 创建订单状态名称转换函数
DELIMITER //

CREATE FUNCTION fn_order_status_name(p_order_status TINYINT)
RETURNS VARCHAR(50)
DETERMINISTIC
NO SQL
BEGIN
    RETURN CASE p_order_status
        WHEN 0 THEN '待支付'
        WHEN 1 THEN '已支付'
        WHEN 2 THEN '已取消'
        WHEN 3 THEN '已退款'
        ELSE '未知状态'
    END;
END//

DELIMITER ;
```

下面函数用于手机号脱敏：

```sql
-- 创建手机号脱敏函数
DELIMITER //

CREATE FUNCTION fn_mask_mobile(p_mobile VARCHAR(20))
RETURNS VARCHAR(20)
DETERMINISTIC
NO SQL
BEGIN
    IF p_mobile IS NULL OR CHAR_LENGTH(p_mobile) < 7 THEN
        RETURN NULL;
    END IF;

    RETURN CONCAT(LEFT(p_mobile, 3), '****', RIGHT(p_mobile, 4));
END//

DELIMITER ;
```

下面函数用于计算折扣金额：

```sql
-- 创建折扣金额计算函数
DELIMITER //

CREATE FUNCTION fn_discount_amount(
    p_total_amount DECIMAL(18,2),
    p_discount_rate DECIMAL(5,4)
)
RETURNS DECIMAL(18,2)
DETERMINISTIC
NO SQL
BEGIN
    IF p_total_amount IS NULL THEN
        RETURN 0.00;
    END IF;

    IF p_discount_rate IS NULL THEN
        SET p_discount_rate = 1.0000;
    END IF;

    RETURN ROUND(p_total_amount * p_discount_rate, 2);
END//

DELIMITER ;
```

查看存储函数：

```sql
-- 查看当前数据库中的存储函数
SHOW FUNCTION STATUS
WHERE Db = DATABASE();

-- 查看函数创建语句
SHOW CREATE FUNCTION fn_order_status_name;
```

创建存储函数建议：

1. 函数名称建议使用 `fn_` 前缀。
2. 参数名称建议使用 `p_` 前缀，避免与字段名混淆。
3. 函数应尽量保持简单、确定、无副作用。
4. 适合封装状态转换、脱敏、格式化、轻量计算。
5. 不建议在函数中执行复杂查询或大量数据处理。
6. 创建函数时应明确声明 `DETERMINISTIC`、`NO SQL`、`READS SQL DATA` 等特性。

### 调用存储函数

存储函数可以像普通内置函数一样在 SQL 中调用。它可以用于查询字段、条件判断、排序、分组或表达式计算，但在高频查询和大表过滤条件中使用时要谨慎，因为函数计算可能增加查询成本。

直接调用函数：

```sql
-- 直接调用订单状态转换函数
SELECT fn_order_status_name(1) AS order_status_name;

-- 直接调用手机号脱敏函数
SELECT fn_mask_mobile('13800000001') AS mobile_mask;

-- 直接调用折扣金额计算函数
SELECT fn_discount_amount(199.90, 0.8500) AS discount_amount;
```

在查询结果中调用函数：

```sql
-- 查询订单并展示状态名称、脱敏手机号和折扣后金额
SELECT
    id,
    order_no,
    user_id,
    order_status,
    fn_order_status_name(order_status) AS order_status_name,
    total_amount,
    discount_rate,
    fn_discount_amount(total_amount, discount_rate) AS final_amount,
    fn_mask_mobile(mobile) AS mobile_mask,
    created_at
FROM func_order_info
ORDER BY created_at DESC
LIMIT 20;
```

在 `WHERE` 中调用函数：

```sql
-- 示例：根据函数结果过滤订单，不推荐高频大表查询这样写
SELECT
    id,
    order_no,
    order_status
FROM func_order_info
WHERE fn_order_status_name(order_status) = '已支付';
```

更推荐直接使用原字段过滤：

```sql
-- 推荐：直接使用状态码过滤，便于命中索引
SELECT
    id,
    order_no,
    order_status,
    fn_order_status_name(order_status) AS order_status_name
FROM func_order_info
WHERE order_status = 1;
```

调用存储函数建议：

1. 查询展示字段中可以使用函数做格式化和转换。
2. 高频过滤条件中不建议对索引字段套函数。
3. 函数适合处理单行轻量逻辑，不适合处理大批量复杂逻辑。
4. 状态过滤优先使用原始状态码，不要用函数转换后的名称过滤。
5. 查询函数调用较多时，应关注 CPU 消耗和执行耗时。

### 函数参数

存储函数参数用于接收调用方传入的数据。MySQL 存储函数参数默认是输入参数，不需要写 `IN`。函数参数可以是数字、字符串、日期、JSON 等类型，但应尽量保持简单。

创建带多个参数的函数：

```sql
-- 根据金额和折扣率计算优惠金额
DELIMITER //

CREATE FUNCTION fn_discount_reduce_amount(
    p_total_amount DECIMAL(18,2),
    p_discount_rate DECIMAL(5,4)
)
RETURNS DECIMAL(18,2)
DETERMINISTIC
NO SQL
BEGIN
    DECLARE v_final_amount DECIMAL(18,2) DEFAULT 0.00;

    IF p_total_amount IS NULL OR p_total_amount <= 0 THEN
        RETURN 0.00;
    END IF;

    IF p_discount_rate IS NULL OR p_discount_rate < 0 OR p_discount_rate > 1 THEN
        SET p_discount_rate = 1.0000;
    END IF;

    SET v_final_amount = ROUND(p_total_amount * p_discount_rate, 2);

    RETURN p_total_amount - v_final_amount;
END//

DELIMITER ;
```

调用：

```sql
-- 查询订单优惠金额
SELECT
    order_no,
    total_amount,
    discount_rate,
    fn_discount_reduce_amount(total_amount, discount_rate) AS reduce_amount
FROM func_order_info;
```

日期参数函数示例：

```sql
-- 判断指定时间是否在最近 N 天内
DELIMITER //

CREATE FUNCTION fn_is_recent_day(
    p_datetime DATETIME,
    p_days INT
)
RETURNS TINYINT
DETERMINISTIC
NO SQL
BEGIN
    IF p_datetime IS NULL OR p_days IS NULL OR p_days <= 0 THEN
        RETURN 0;
    END IF;

    IF p_datetime >= DATE_SUB(NOW(), INTERVAL p_days DAY) THEN
        RETURN 1;
    END IF;

    RETURN 0;
END//

DELIMITER ;
```

调用：

```sql
-- 判断订单是否为最近 7 天创建
SELECT
    order_no,
    created_at,
    fn_is_recent_day(created_at, 7) AS recent_flag
FROM func_order_info;
```

函数参数建议：

1. 参数名称使用 `p_` 前缀。
2. 参数类型应与传入字段类型一致，避免隐式转换。
3. 参数应做空值和边界处理。
4. 不建议函数参数过多，参数过多说明函数职责可能过重。
5. 日期范围判断等查询过滤逻辑，优先直接写 SQL 条件，函数更适合展示计算。

### 返回值

存储函数必须通过 `RETURNS` 声明返回类型，并通过 `RETURN` 返回结果。返回值可以是数字、字符串、日期、JSON 等类型。返回值设计应稳定、明确，不建议一个函数在不同情况下返回含义不一致的数据。

返回字符串：

```sql
-- 根据状态码返回状态名称
SELECT fn_order_status_name(1) AS status_name;
```

返回数值：

```sql
-- 返回折扣后金额
SELECT fn_discount_amount(299.00, 0.9000) AS final_amount;
```

返回布尔语义的 `TINYINT`：

```sql
-- 返回是否最近创建标识：1是，0否
SELECT fn_is_recent_day(NOW(), 7) AS recent_flag;
```

返回 JSON：

```sql
-- 创建返回 JSON 的函数
DELIMITER //

CREATE FUNCTION fn_build_order_summary_json(
    p_order_no VARCHAR(64),
    p_order_status TINYINT,
    p_total_amount DECIMAL(18,2)
)
RETURNS JSON
DETERMINISTIC
NO SQL
BEGIN
    RETURN JSON_OBJECT(
        'orderNo', p_order_no,
        'orderStatus', p_order_status,
        'orderStatusName', fn_order_status_name(p_order_status),
        'totalAmount', p_total_amount
    );
END//

DELIMITER ;
```

调用：

```sql
-- 查询订单摘要 JSON
SELECT
    order_no,
    fn_build_order_summary_json(order_no, order_status, total_amount) AS order_summary_json
FROM func_order_info
LIMIT 10;
```

返回值设计建议：

1. 返回类型应明确且稳定。
2. 金额类返回值建议使用 `DECIMAL`。
3. 标识类返回值可使用 `TINYINT`，例如 `1` 表示是，`0` 表示否。
4. 状态名称类返回值可使用 `VARCHAR`。
5. 返回 JSON 时应保证结构稳定，避免调用方解析困难。
6. 函数遇到非法参数时，可以返回默认值，也可以使用 `SIGNAL` 抛错，按业务约定统一。

### 函数权限

创建、修改、执行存储函数需要相应权限。常见权限包括 `CREATE ROUTINE`、`ALTER ROUTINE`、`EXECUTE`。生产环境中，应用账号通常不应拥有创建或修改函数的权限，只需要在确有必要时拥有执行权限。

创建函数权限示例：

```sql
-- 授予开发变更账号创建和修改存储函数的权限
GRANT CREATE ROUTINE, ALTER ROUTINE
ON mall_order.*
TO 'ddl_user'@'192.168.1.%';
```

授予执行函数权限：

```sql
-- 授予应用账号执行存储函数的权限
GRANT EXECUTE
ON FUNCTION mall_order.fn_order_status_name
TO 'mall_app'@'192.168.1.%';
```

授予某个库内所有存储过程和函数执行权限：

```sql
-- 授予指定库例程执行权限
GRANT EXECUTE
ON mall_order.*
TO 'mall_app'@'192.168.1.%';
```

查看权限：

```sql
-- 查看用户权限
SHOW GRANTS FOR 'mall_app'@'192.168.1.%';

-- 查看函数定义
SHOW CREATE FUNCTION fn_order_status_name;
```

二进制日志环境中的函数创建限制：

```sql
-- 查看是否开启函数创建信任配置
SHOW VARIABLES LIKE 'log_bin_trust_function_creators';
```

如果开启了 binlog，创建函数时 MySQL 可能要求函数声明确定性或数据访问特征，例如 `DETERMINISTIC`、`NO SQL`、`READS SQL DATA`。不建议为了省事随意开启信任配置：

```sql
-- 谨慎使用：允许非 SUPER 用户在 binlog 环境中创建函数
-- 生产环境应评估复制一致性风险后再设置
SET GLOBAL log_bin_trust_function_creators = 1;
```

函数权限建议：

1. 应用账号通常只授予 `EXECUTE`，不授予 `CREATE ROUTINE`。
2. DDL 变更账号负责创建、修改、删除函数。
3. 函数定义者权限需要谨慎管理。
4. 开启 binlog 的环境中，应明确函数是否确定、是否读取数据。
5. 函数权限变更应纳入数据库变更审批。

### 删除存储函数

删除存储函数使用 `DROP FUNCTION`。删除函数不会删除表数据，但会影响依赖该函数的视图、存储过程、SQL 脚本、报表或应用查询。

删除函数：

```sql
-- 删除订单状态名称函数
DROP FUNCTION fn_order_status_name;
```

如果存在则删除：

```sql
-- 如果函数存在，则删除
DROP FUNCTION IF EXISTS fn_order_status_name;
```

删除前检查依赖：

```sql
-- 查看函数定义
SHOW CREATE FUNCTION fn_order_status_name;

-- 查看当前数据库中的函数
SHOW FUNCTION STATUS
WHERE Db = DATABASE();
```

删除多个函数：

```sql
-- 删除多个存储函数
DROP FUNCTION IF EXISTS fn_mask_mobile;
DROP FUNCTION IF EXISTS fn_discount_amount;
DROP FUNCTION IF EXISTS fn_discount_reduce_amount;
```

删除函数建议：

1. 删除前确认没有视图、过程、报表和应用 SQL 依赖。
2. 删除前备份函数定义。
3. 生产环境删除函数应走变更流程。
4. 可以先替换调用方，再删除函数。
5. 删除后应执行相关查询和任务验证。

### 存储函数使用限制

存储函数虽然可以复用逻辑，但存在明显限制。它不适合承载复杂业务流程，也不适合大量数据处理。滥用存储函数会导致 SQL 难以优化、逻辑难以测试、应用行为不透明。

常见限制：

| 限制                 | 说明                               |
| -------------------- | ---------------------------------- |
| 必须返回值           | 存储函数必须 `RETURN` 一个值       |
| 不适合复杂写逻辑     | 函数应避免修改大量数据             |
| 不适合长事务         | 函数中不应处理复杂事务逻辑         |
| 不适合替代应用层逻辑 | 业务规则难以测试和版本管理         |
| 可能影响索引使用     | 在索引字段上套函数可能导致索引失效 |
| 权限和复制有要求     | binlog 环境中函数创建可能受限制    |
| 调试不便             | 函数内部问题定位不如应用代码直观   |

不推荐在查询条件中滥用函数：

```sql
-- 不推荐：对字段调用函数后再过滤，可能导致索引失效
SELECT
    id,
    order_no,
    created_at
FROM func_order_info
WHERE fn_is_recent_day(created_at, 7) = 1;
```

推荐改写为普通条件：

```sql
-- 推荐：直接使用时间范围条件
SELECT
    id,
    order_no,
    created_at
FROM func_order_info
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

存储函数使用建议：

1. 只封装简单、稳定、轻量的计算逻辑。
2. 不把核心业务流程放进函数。
3. 不在高频大表过滤条件中滥用函数。
4. 创建函数前确认是否可以由应用层、视图或普通 SQL 更清晰地实现。
5. 函数变更应有版本管理和回归验证。
6. 对性能敏感 SQL，应使用 `EXPLAIN` 验证函数调用影响。

## 触发器

触发器是绑定在表上的数据库对象，会在指定表发生 `INSERT`、`UPDATE`、`DELETE` 操作时自动执行。触发器分为 `BEFORE` 和 `AFTER` 两类，可以在写入前校验或修正数据，也可以在写入后记录审计日志、维护冗余数据。

触发器很强，但也很隐蔽。它会让数据变化产生额外副作用，应用层不一定能直观看到。因此生产环境中应谨慎使用触发器，优先用于审计、数据校验、简单冗余维护，不建议承载核心业务流程。

本节示例默认使用以下表：

```sql
-- 触发器用户表
CREATE TABLE IF NOT EXISTS trigger_sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发器用户表示例';

-- 用户审计日志表
CREATE TABLE IF NOT EXISTS trigger_user_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：INSERT，UPDATE，DELETE',
    old_data JSON DEFAULT NULL COMMENT '变更前数据',
    new_data JSON DEFAULT NULL COMMENT '变更后数据',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_user_id_time (user_id, operation_time),
    KEY idx_operation_type_time (operation_type, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发器用户审计日志表';
```

### BEFORE INSERT 触发器

`BEFORE INSERT` 触发器会在插入数据之前执行，适合做字段默认值修正、参数校验、格式清洗等。可以通过 `NEW.column_name` 访问即将插入的新数据，并可修改 `NEW` 的字段值。

下面触发器用于插入用户前清理用户名空格，并校验用户名不能为空：

```sql
-- 创建 BEFORE INSERT 触发器，插入前清洗和校验数据
DELIMITER //

CREATE TRIGGER trg_user_before_insert
BEFORE INSERT ON trigger_sys_user
FOR EACH ROW
BEGIN
    SET NEW.username = TRIM(NEW.username);

    IF NEW.username IS NULL OR NEW.username = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '用户名不能为空';
    END IF;

    IF NEW.status IS NULL THEN
        SET NEW.status = 1;
    END IF;

    IF NEW.deleted IS NULL THEN
        SET NEW.deleted = 0;
    END IF;
END//

DELIMITER ;
```

验证插入：

```sql
-- 插入用户名带空格的数据，触发器会自动 TRIM
INSERT INTO trigger_sys_user (
    username,
    nickname,
    mobile
) VALUES (
    '  zhangsan  ',
    '张三',
    '13800000001'
);

-- 查看插入结果
SELECT
    id,
    username,
    nickname,
    mobile,
    status,
    deleted
FROM trigger_sys_user
WHERE username = 'zhangsan';
```

触发校验失败：

```sql
-- 用户名为空时触发 SIGNAL 异常
INSERT INTO trigger_sys_user (
    username,
    nickname
) VALUES (
    '   ',
    '空用户名'
);
```

`BEFORE INSERT` 使用建议：

1. 适合插入前字段清洗和基础校验。
2. 可以修改 `NEW` 字段值。
3. 不建议执行复杂查询或调用外部逻辑。
4. 复杂业务校验应优先放在应用层。
5. 触发器抛出异常会导致当前写入失败。

### AFTER INSERT 触发器

`AFTER INSERT` 触发器会在插入数据成功后执行，适合记录审计日志、维护统计数据、写入扩展表等。此时可以读取 `NEW` 数据，但不应再修改当前表同一行。

下面触发器用于插入用户后记录审计日志：

```sql
-- 创建 AFTER INSERT 触发器，插入成功后记录审计日志
DELIMITER //

CREATE TRIGGER trg_user_after_insert
AFTER INSERT ON trigger_sys_user
FOR EACH ROW
BEGIN
    INSERT INTO trigger_user_audit_log (
        user_id,
        username,
        operation_type,
        old_data,
        new_data
    ) VALUES (
        NEW.id,
        NEW.username,
        'INSERT',
        NULL,
        JSON_OBJECT(
            'id', NEW.id,
            'username', NEW.username,
            'nickname', NEW.nickname,
            'mobile', NEW.mobile,
            'status', NEW.status,
            'deleted', NEW.deleted
        )
    );
END//

DELIMITER ;
```

验证触发器：

```sql
-- 插入用户
INSERT INTO trigger_sys_user (
    username,
    nickname,
    mobile
) VALUES (
    'lisi',
    '李四',
    '13800000002'
);

-- 查看审计日志
SELECT
    user_id,
    username,
    operation_type,
    old_data,
    new_data,
    operation_time
FROM trigger_user_audit_log
WHERE username = 'lisi'
ORDER BY operation_time DESC;
```

`AFTER INSERT` 使用建议：

1. 适合记录插入审计日志。
2. 适合维护简单冗余数据。
3. 不建议在触发器中写复杂业务流程。
4. 审计日志表应建立合适索引，避免日志查询慢。
5. 写入量很大的表使用触发器记录日志时，应评估额外写入压力。

### BEFORE UPDATE 触发器

`BEFORE UPDATE` 触发器会在更新数据之前执行，适合更新前校验、字段修正、防止非法状态变更等。可以同时访问旧数据 `OLD.column_name` 和新数据 `NEW.column_name`。

下面触发器用于禁止把已删除用户重新改为未删除，并校验状态值：

```sql
-- 创建 BEFORE UPDATE 触发器，更新前校验状态和删除标识
DELIMITER //

CREATE TRIGGER trg_user_before_update
BEFORE UPDATE ON trigger_sys_user
FOR EACH ROW
BEGIN
    IF NEW.status NOT IN (0, 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '用户状态不合法';
    END IF;

    IF OLD.deleted = 1 AND NEW.deleted = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '已删除用户不允许恢复';
    END IF;

    SET NEW.username = TRIM(NEW.username);
END//

DELIMITER ;
```

验证状态校验：

```sql
-- 状态值非法时会触发异常
UPDATE trigger_sys_user
SET status = 9
WHERE username = 'zhangsan';
```

验证删除恢复限制：

```sql
-- 先逻辑删除
UPDATE trigger_sys_user
SET deleted = 1
WHERE username = 'zhangsan';

-- 尝试恢复，会触发异常
UPDATE trigger_sys_user
SET deleted = 0
WHERE username = 'zhangsan';
```

`BEFORE UPDATE` 使用建议：

1. 适合更新前校验字段合法性。
2. 可以通过 `OLD` 和 `NEW` 比较变更前后数据。
3. 可以修改 `NEW` 字段值。
4. 不建议实现复杂状态机，复杂状态机应放在应用层。
5. 更新触发器容易影响批量更新性能，应谨慎使用。

### AFTER UPDATE 触发器

`AFTER UPDATE` 触发器会在更新成功后执行，常用于记录变更审计日志。可以记录旧值和新值，便于追踪谁改了什么数据。MySQL 触发器本身无法直接获取应用用户身份，通常需要应用层通过连接变量或审计字段传入。

下面触发器用于记录用户更新前后的数据：

```sql
-- 创建 AFTER UPDATE 触发器，更新后记录审计日志
DELIMITER //

CREATE TRIGGER trg_user_after_update
AFTER UPDATE ON trigger_sys_user
FOR EACH ROW
BEGIN
    INSERT INTO trigger_user_audit_log (
        user_id,
        username,
        operation_type,
        old_data,
        new_data
    ) VALUES (
        NEW.id,
        NEW.username,
        'UPDATE',
        JSON_OBJECT(
            'id', OLD.id,
            'username', OLD.username,
            'nickname', OLD.nickname,
            'mobile', OLD.mobile,
            'status', OLD.status,
            'deleted', OLD.deleted
        ),
        JSON_OBJECT(
            'id', NEW.id,
            'username', NEW.username,
            'nickname', NEW.nickname,
            'mobile', NEW.mobile,
            'status', NEW.status,
            'deleted', NEW.deleted
        )
    );
END//

DELIMITER ;
```

验证更新日志：

```sql
-- 更新用户信息
UPDATE trigger_sys_user
SET
    nickname = '李四更新',
    status = 0
WHERE username = 'lisi';

-- 查看更新审计日志
SELECT
    user_id,
    username,
    operation_type,
    old_data,
    new_data,
    operation_time
FROM trigger_user_audit_log
WHERE username = 'lisi'
ORDER BY operation_time DESC;
```

只在关键字段变化时记录日志：

```sql
-- 创建示例：只有手机号或状态变化时才记录日志
DELIMITER //

CREATE TRIGGER trg_user_after_update_key_field
AFTER UPDATE ON trigger_sys_user
FOR EACH ROW
BEGIN
    IF NOT (OLD.mobile <=> NEW.mobile) OR OLD.status <> NEW.status THEN
        INSERT INTO trigger_user_audit_log (
            user_id,
            username,
            operation_type,
            old_data,
            new_data
        ) VALUES (
            NEW.id,
            NEW.username,
            'UPDATE_KEY_FIELD',
            JSON_OBJECT(
                'mobile', OLD.mobile,
                'status', OLD.status
            ),
            JSON_OBJECT(
                'mobile', NEW.mobile,
                'status', NEW.status
            )
        );
    END IF;
END//

DELIMITER ;
```

说明：`<=>` 是 MySQL 的 NULL 安全等值比较运算符，适合比较可能为 `NULL` 的字段。

`AFTER UPDATE` 使用建议：

1. 适合记录变更前后值。
2. 审计字段较多时，可以使用 JSON 保存快照。
3. 日志表写入量会随更新量增加，应评估性能和存储。
4. 不建议在触发器中再更新当前表，容易造成复杂副作用。
5. 对高频更新表，审计触发器应谨慎启用。

### BEFORE DELETE 触发器

`BEFORE DELETE` 触发器会在删除数据之前执行，适合删除前校验、防止关键数据被删除、检查关联数据等。可以通过 `OLD.column_name` 访问即将被删除的数据。

下面触发器用于禁止删除启用状态用户：

```sql
-- 创建 BEFORE DELETE 触发器，禁止删除启用用户
DELIMITER //

CREATE TRIGGER trg_user_before_delete
BEFORE DELETE ON trigger_sys_user
FOR EACH ROW
BEGIN
    IF OLD.status = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '启用状态用户不允许物理删除';
    END IF;
END//

DELIMITER ;
```

验证删除限制：

```sql
-- 启用用户删除会失败
DELETE FROM trigger_sys_user
WHERE username = 'lisi';
```

先禁用再删除：

```sql
-- 禁用用户
UPDATE trigger_sys_user
SET status = 0
WHERE username = 'lisi';

-- 禁用后可以物理删除
DELETE FROM trigger_sys_user
WHERE username = 'lisi';
```

`BEFORE DELETE` 使用建议：

1. 适合防止关键数据被误删。
2. 可以根据旧值判断是否允许删除。
3. 业务系统更推荐逻辑删除，而不是频繁物理删除。
4. 删除前复杂关联检查更适合应用层或外键约束。
5. 触发器抛错后，删除操作会失败并回滚。

### AFTER DELETE 触发器

`AFTER DELETE` 触发器会在删除成功后执行，常用于记录删除审计日志、同步删除辅助数据等。删除后只能访问 `OLD` 数据，不能访问 `NEW`。

下面触发器用于删除成功后记录审计日志：

```sql
-- 创建 AFTER DELETE 触发器，删除后记录审计日志
DELIMITER //

CREATE TRIGGER trg_user_after_delete
AFTER DELETE ON trigger_sys_user
FOR EACH ROW
BEGIN
    INSERT INTO trigger_user_audit_log (
        user_id,
        username,
        operation_type,
        old_data,
        new_data
    ) VALUES (
        OLD.id,
        OLD.username,
        'DELETE',
        JSON_OBJECT(
            'id', OLD.id,
            'username', OLD.username,
            'nickname', OLD.nickname,
            'mobile', OLD.mobile,
            'status', OLD.status,
            'deleted', OLD.deleted
        ),
        NULL
    );
END//

DELIMITER ;
```

验证删除日志：

```sql
-- 删除用户
DELETE FROM trigger_sys_user
WHERE username = 'lisi';

-- 查看删除日志
SELECT
    user_id,
    username,
    operation_type,
    old_data,
    operation_time
FROM trigger_user_audit_log
WHERE username = 'lisi'
ORDER BY operation_time DESC;
```

`AFTER DELETE` 使用建议：

1. 适合记录物理删除审计。
2. 删除审计日志应保存足够旧值，便于追踪。
3. 如果使用逻辑删除，通常由 `AFTER UPDATE` 记录 `deleted` 字段变化。
4. 大批量删除会触发大量日志写入，应谨慎执行。
5. 删除触发器不能替代备份恢复。

### 触发器使用场景

触发器适合用于数据库侧自动执行的轻量逻辑，尤其是与数据变更强相关、必须和原 DML 在同一事务中完成的动作。

常见适用场景：

| 场景     | 说明                         |
| -------- | ---------------------------- |
| 审计日志 | 记录新增、修改、删除前后数据 |
| 基础校验 | 插入或更新前校验字段合法性   |
| 字段修正 | 自动清理空格、补默认值       |
| 冗余维护 | 简单维护统计字段或镜像表     |
| 防误删   | 删除前阻止关键数据物理删除   |
| 历史记录 | 更新后保存旧版本快照         |

审计日志场景：

```sql
-- 查询用户审计日志
SELECT
    user_id,
    username,
    operation_type,
    old_data,
    new_data,
    operation_time
FROM trigger_user_audit_log
WHERE user_id = 1
ORDER BY operation_time DESC;
```

字段修正场景：

```sql
-- 插入前自动清理 username 空格
INSERT INTO trigger_sys_user (
    username,
    nickname
) VALUES (
    '  wangwu  ',
    '王五'
);
```

防误删场景：

```sql
-- 删除启用用户时由触发器阻止
DELETE FROM trigger_sys_user
WHERE username = 'wangwu';
```

触发器适用建议：

1. 适合轻量、稳定、与数据变更强绑定的逻辑。
2. 审计日志是触发器较常见的合理使用场景。
3. 简单校验可以使用触发器，但复杂校验应放应用层。
4. 触发器逻辑必须简单可控。
5. 触发器执行失败会导致原 DML 失败，应提前评估影响。

### 触发器风险控制

触发器的主要风险是隐蔽性强、调试困难、性能成本不直观、容易产生副作用。生产环境使用触发器必须有清晰命名、文档、变更管理和监控。

查看触发器：

```sql
-- 查看当前数据库触发器
SHOW TRIGGERS;

-- 查看指定表上的触发器
SHOW TRIGGERS
WHERE `Table` = 'trigger_sys_user';
```

通过 `information_schema` 查看触发器：

```sql
-- 查询触发器定义
SELECT
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    EVENT_OBJECT_TABLE,
    ACTION_TIMING,
    ACTION_STATEMENT,
    CREATED
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = DATABASE()
  AND EVENT_OBJECT_TABLE = 'trigger_sys_user';
```

删除触发器：

```sql
-- 删除触发器
DROP TRIGGER IF EXISTS trg_user_after_update;
```

触发器风险控制建议：

1. 触发器名称应包含表名、时机和事件，例如 `trg_user_after_update`。
2. 每个触发器职责应单一，不写复杂业务流程。
3. 高并发、高频写入表慎用触发器。
4. 大批量导入、更新、删除前应评估触发器影响。
5. 触发器变更必须纳入数据库变更脚本管理。
6. 应提供触发器清单和说明文档，避免隐藏逻辑无人知晓。
7. 不建议在触发器中调用复杂存储过程。
8. 不建议多个触发器之间形成隐式依赖链。
9. 审计触发器应注意日志表容量和清理策略。
10. 如果应用层已经有完整审计机制，数据库触发器审计应避免重复和冲突。

## 事件调度器

事件调度器是 MySQL 内置的定时任务机制，可以按固定时间或固定周期执行 SQL。它适合数据库内部的轻量定时任务，例如刷新统计表、清理临时数据、归档小批量历史数据等。

事件调度器不是完整任务调度平台。复杂任务、分布式任务、需要重试告警的任务，建议使用应用层定时任务、XXL-JOB、Quartz、Airflow、CronJob 或其他调度系统。

本节示例默认使用以下表：

```sql
-- 事件执行日志表
CREATE TABLE IF NOT EXISTS event_execute_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    event_name VARCHAR(100) NOT NULL COMMENT '事件名称',
    execute_status VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS成功，FAIL失败',
    execute_message VARCHAR(1000) DEFAULT NULL COMMENT '执行消息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_event_name_created_at (event_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件执行日志表';

-- 临时数据表，用于演示定时清理
CREATE TABLE IF NOT EXISTS event_temp_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    data_content VARCHAR(1000) DEFAULT NULL COMMENT '数据内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件临时数据表';
```

### 开启事件调度器

事件调度器由系统变量 `event_scheduler` 控制。只有开启后，MySQL 才会自动执行已启用的事件。

查看事件调度器状态：

```sql
-- 查看事件调度器是否开启
SHOW VARIABLES LIKE 'event_scheduler';
```

临时开启事件调度器：

```sql
-- 临时开启事件调度器，MySQL 重启后可能失效
SET GLOBAL event_scheduler = ON;
```

临时关闭事件调度器：

```sql
-- 临时关闭事件调度器
SET GLOBAL event_scheduler = OFF;
```

配置文件中永久开启：

```ini
[mysqld]
# 开启 MySQL 事件调度器
event_scheduler=ON
```

重启 MySQL 后验证：

```sql
-- 验证事件调度器状态
SHOW VARIABLES LIKE 'event_scheduler';
```

事件调度器权限建议：

```sql
-- 授予用户创建事件的权限
GRANT EVENT
ON mall_order.*
TO 'ddl_user'@'192.168.1.%';
```

开启事件调度器建议：

1. 生产环境开启前应确认是否已有事件会自动运行。
2. 事件调度器是全局开关，开启后所有启用事件都可能执行。
3. 创建事件需要 `EVENT` 权限。
4. 建议将事件创建、修改、删除纳入数据库变更管理。
5. 重启后是否自动开启取决于配置文件，不要只依赖 `SET GLOBAL`。

### 创建定时事件

创建事件使用 `CREATE EVENT`。事件可以执行一次，也可以按周期重复执行。事件体中可以写单条 SQL，也可以写 `BEGIN ... END` 包含多条 SQL。

创建一次性事件：

```sql
-- 创建一次性事件，1 小时后插入一条日志
DELIMITER //

CREATE EVENT ev_insert_log_once
ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 HOUR
DO
BEGIN
    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_insert_log_once',
        'SUCCESS',
        '一次性事件执行完成'
    );
END//

DELIMITER ;
```

创建每天执行的事件：

```sql
-- 创建每天凌晨 2 点清理 30 天前临时数据的事件
DELIMITER //

CREATE EVENT ev_clean_temp_data_daily
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '02:00:00')
DO
BEGIN
    DELETE FROM event_temp_data
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
    LIMIT 10000;

    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_clean_temp_data_daily',
        'SUCCESS',
        '临时数据清理完成'
    );
END//

DELIMITER ;
```

创建每小时刷新统计的事件：

```sql
-- 创建每小时刷新前一天用户订单统计的事件
DELIMITER //

CREATE EVENT ev_refresh_yesterday_user_summary_hourly
ON SCHEDULE EVERY 1 HOUR
STARTS CURRENT_TIMESTAMP + INTERVAL 5 MINUTE
DO
BEGIN
    CALL proc_refresh_user_summary(DATE_SUB(CURDATE(), INTERVAL 1 DAY));

    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_refresh_yesterday_user_summary_hourly',
        'SUCCESS',
        '前一天用户订单统计刷新完成'
    );
END//

DELIMITER ;
```

查看事件：

```sql
-- 查看当前数据库中的事件
SHOW EVENTS;

-- 查看事件创建语句
SHOW CREATE EVENT ev_clean_temp_data_daily;
```

创建事件建议：

1. 事件名称建议使用 `ev_` 前缀。
2. 定时事件应写执行日志，便于排查。
3. 删除、更新类任务应限制每次处理数量，避免大事务。
4. 事件执行 SQL 应具备幂等性。
5. 复杂任务不建议放在 MySQL 事件中执行，应使用专业调度系统。

### 修改定时事件

修改事件使用 `ALTER EVENT`，可以修改调度时间、事件体、启用状态、名称等。生产环境修改事件前应先查看原定义，确认当前事件是否正在承担业务任务。

修改执行周期：

```sql
-- 将清理事件改为每 6 小时执行一次
ALTER EVENT ev_clean_temp_data_daily
ON SCHEDULE EVERY 6 HOUR;
```

修改事件体：

```sql
-- 修改事件体，清理 15 天前的数据
DELIMITER //

ALTER EVENT ev_clean_temp_data_daily
DO
BEGIN
    DELETE FROM event_temp_data
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 15 DAY)
    LIMIT 5000;

    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_clean_temp_data_daily',
        'SUCCESS',
        '临时数据清理完成，保留15天'
    );
END//

DELIMITER ;
```

禁用事件：

```sql
-- 禁用事件，事件不会继续自动执行
ALTER EVENT ev_clean_temp_data_daily DISABLE;
```

启用事件：

```sql
-- 启用事件
ALTER EVENT ev_clean_temp_data_daily ENABLE;
```

重命名事件：

```sql
-- 重命名事件
ALTER EVENT ev_clean_temp_data_daily
RENAME TO ev_clean_temp_data_every_6_hour;
```

修改事件建议：

1. 修改前先执行 `SHOW CREATE EVENT` 备份定义。
2. 修改执行周期时，应确认是否会造成任务重叠。
3. 修改删除类任务时，应控制每批删除数量。
4. 暂停任务优先使用 `DISABLE`，不要直接删除。
5. 修改后应查看事件状态和执行日志。

### 删除定时事件

删除事件使用 `DROP EVENT`。删除事件不会删除事件已处理的数据，但会停止后续调度执行。

删除事件：

```sql
-- 删除指定事件
DROP EVENT ev_clean_temp_data_daily;
```

如果存在则删除：

```sql
-- 如果事件存在，则删除
DROP EVENT IF EXISTS ev_clean_temp_data_daily;
```

删除前查看事件：

```sql
-- 查看事件列表
SHOW EVENTS;

-- 查看事件定义
SHOW CREATE EVENT ev_clean_temp_data_daily;
```

删除前先禁用：

```sql
-- 先禁用事件，观察无影响后再删除
ALTER EVENT ev_clean_temp_data_daily DISABLE;

-- 确认无影响后删除
DROP EVENT IF EXISTS ev_clean_temp_data_daily;
```

删除事件建议：

1. 删除前确认没有业务依赖。
2. 生产环境建议先禁用观察，再删除。
3. 删除前备份事件定义。
4. 删除事件不会删除执行日志。
5. 删除后应检查调度任务是否已迁移到其他系统。

### 定时任务使用场景

事件调度器适合数据库内部的轻量定时任务，尤其是任务逻辑简单、只涉及数据库内部 SQL、不需要复杂调度能力的场景。

常见适用场景：

| 场景         | 说明                       |
| ------------ | -------------------------- |
| 清理临时数据 | 定期删除过期临时表数据     |
| 刷新汇总表   | 定期刷新轻量统计结果       |
| 写入心跳日志 | 记录数据库内置任务执行状态 |
| 小批量归档   | 将少量历史数据迁移到归档表 |
| 重置状态     | 定期重置过期任务、过期锁   |
| 数据质量检查 | 定期插入异常统计结果       |

清理过期数据：

```sql
-- 每天清理 30 天前临时数据
DELIMITER //

CREATE EVENT ev_clean_expired_temp_data
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '03:00:00')
DO
BEGIN
    DELETE FROM event_temp_data
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
    LIMIT 10000;

    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_clean_expired_temp_data',
        'SUCCESS',
        '过期临时数据清理完成'
    );
END//

DELIMITER ;
```

刷新统计数据：

```sql
-- 每天凌晨刷新前一天统计数据
DELIMITER //

CREATE EVENT ev_refresh_yesterday_summary
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '01:00:00')
DO
BEGIN
    CALL proc_refresh_user_summary(DATE_SUB(CURDATE(), INTERVAL 1 DAY));

    INSERT INTO event_execute_log (
        event_name,
        execute_status,
        execute_message
    ) VALUES (
        'ev_refresh_yesterday_summary',
        'SUCCESS',
        '昨日统计刷新完成'
    );
END//

DELIMITER ;
```

检查执行日志：

```sql
-- 查看最近事件执行情况
SELECT
    event_name,
    execute_status,
    execute_message,
    created_at
FROM event_execute_log
ORDER BY created_at DESC
LIMIT 50;
```

适合使用事件调度器的任务特征：

1. 任务逻辑简单。
2. 只依赖 MySQL 内部数据。
3. 不需要复杂失败重试。
4. 不需要分布式调度。
5. 不需要调用外部 HTTP、MQ、RPC。
6. 执行时间短，影响范围可控。

不适合使用事件调度器的任务：

1. 大批量数据迁移。
2. 跨系统调用任务。
3. 需要复杂重试和告警的任务。
4. 长时间运行任务。
5. 高并发分布式任务。
6. 涉及复杂业务流程的任务。

### 定时任务注意事项

事件调度器虽然方便，但生产环境使用时要特别注意任务可见性、执行耗时、事务大小、异常处理、日志记录和任务重叠问题。MySQL 内置事件不如专业调度系统易于监控和治理，因此应控制使用范围。

查看事件状态：

```sql
-- 查看事件调度器全局状态
SHOW VARIABLES LIKE 'event_scheduler';

-- 查看事件列表和状态
SHOW EVENTS;
```

通过 `information_schema` 查看事件：

```sql
-- 查看当前数据库事件详情
SELECT
    EVENT_SCHEMA,
    EVENT_NAME,
    STATUS,
    EVENT_TYPE,
    EXECUTE_AT,
    INTERVAL_VALUE,
    INTERVAL_FIELD,
    STARTS,
    ENDS,
    LAST_EXECUTED,
    EVENT_DEFINITION
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA = DATABASE()
ORDER BY EVENT_NAME;
```

事件中控制删除批次：

```sql
-- 删除任务每次限制数量，避免单次删除过多形成大事务
DELETE FROM event_temp_data
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY id
LIMIT 5000;
```

事件执行日志建议：

```sql
-- 事件执行后记录日志，便于排查任务是否运行
INSERT INTO event_execute_log (
    event_name,
    execute_status,
    execute_message
) VALUES (
    'ev_clean_temp_data_daily',
    'SUCCESS',
    '任务执行完成'
);
```

事件调度器注意事项：

1. 开启 `event_scheduler` 前要检查已有事件，避免历史事件突然运行。
2. 事件任务应具备幂等性，重复执行不能造成错误数据。
3. 更新和删除任务应分批处理，避免大事务。
4. 长任务不适合放在 MySQL 事件中。
5. 事件执行失败不一定有明显告警，应主动记录执行日志。
6. 事件调度时间应避开业务高峰。
7. 主从复制环境中要明确事件在哪个实例执行，避免主从都执行同一任务。
8. 事件定义应纳入版本管理。
9. 生产环境复杂定时任务建议放到应用层调度系统。
10. 定期巡检 `information_schema.EVENTS` 和执行日志，确认任务状态正常。


## 临时表

临时表用于在当前会话或 SQL 执行过程中临时保存中间结果。MySQL 中临时表可以分为两类：显式创建的临时表和优化器内部使用的临时表。

显式临时表由开发者通过 `CREATE TEMPORARY TABLE` 创建，只在当前连接中可见，连接断开后自动删除。内部临时表由 MySQL 执行复杂查询时自动创建，例如 `GROUP BY`、`ORDER BY`、`DISTINCT`、`UNION`、派生表、CTE 等场景。

### 会话临时表

会话临时表使用 `CREATE TEMPORARY TABLE` 创建，只在当前数据库连接中可见。不同连接可以创建同名临时表，互不影响。连接关闭后，临时表会自动删除。

创建会话临时表保存用户订单统计结果：

```sql
-- 创建会话临时表，保存用户订单统计中间结果
CREATE TEMPORARY TABLE tmp_user_order_summary (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    total_pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付总金额',
    latest_order_time DATETIME DEFAULT NULL COMMENT '最近下单时间',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单统计临时表';
```

向临时表写入统计结果：

```sql
-- 将订单统计结果写入临时表
INSERT INTO tmp_user_order_summary (
    user_id,
    order_count,
    total_pay_amount,
    latest_order_time
)
SELECT
    user_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_pay_amount,
    MAX(created_at) AS latest_order_time
FROM order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
GROUP BY user_id;
```

查询临时表：

```sql
-- 查询临时统计结果
SELECT
    user_id,
    order_count,
    total_pay_amount,
    latest_order_time
FROM tmp_user_order_summary
WHERE total_pay_amount >= 1000.00
ORDER BY total_pay_amount DESC;
```

手动删除临时表：

```sql
-- 手动删除会话临时表
DROP TEMPORARY TABLE IF EXISTS tmp_user_order_summary;
```

会话临时表使用建议：

1. 适合保存复杂 SQL 的中间结果。
2. 适合批处理、数据修复、临时统计和迁移脚本。
3. 临时表只在当前连接可见，不能跨连接共享。
4. 使用连接池时要注意连接复用，建议任务结束后主动删除临时表。
5. 临时表字段和索引也需要合理设计，否则中间结果查询仍然可能很慢。

### 内存临时表

内存临时表通常指使用 `MEMORY` 引擎创建的临时表，数据存储在内存中，访问速度较快，但容量受内存限制。连接断开后临时表仍会被删除；MySQL 重启或连接关闭后数据也不会保留。

创建内存临时表：

```sql
-- 创建 MEMORY 引擎临时表，适合少量中间数据
CREATE TEMPORARY TABLE tmp_order_id_memory (
    id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    PRIMARY KEY (id),
    KEY idx_order_no (order_no)
) ENGINE=MEMORY COMMENT='订单ID内存临时表';
```

写入少量订单 ID：

```sql
-- 将最近订单写入内存临时表
INSERT INTO tmp_order_id_memory (
    id,
    order_no
)
SELECT
    id,
    order_no
FROM order_info
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC
LIMIT 1000;
```

基于内存临时表关联查询：

```sql
-- 使用内存临时表关联查询订单详情
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.order_status,
    o.pay_amount,
    o.created_at
FROM tmp_order_id_memory AS t
JOIN order_info AS o ON t.id = o.id
ORDER BY o.created_at DESC;
```

内存临时表使用建议：

1. 适合数据量小、字段短、访问频繁的中间结果。
2. 不适合存储大字段，例如 `TEXT`、`BLOB`、大 `JSON`。
3. 数据量过大可能占用大量内存，影响数据库稳定性。
4. 临时表索引仍然需要按查询方式设计。
5. 对于较大中间结果，优先使用 InnoDB 临时表或普通中间表。

### 磁盘临时表

磁盘临时表可以是显式使用 InnoDB 创建的临时表，也可以是 MySQL 执行复杂 SQL 时自动落盘的内部临时表。相比内存临时表，磁盘临时表容量更大，但 IO 成本更高。

显式创建 InnoDB 临时表：

```sql
-- 创建 InnoDB 临时表，适合较大中间结果
CREATE TEMPORARY TABLE tmp_large_order_summary (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL COMMENT '订单状态',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    PRIMARY KEY (user_id, order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大数据量订单统计临时表';
```

写入较大统计结果：

```sql
-- 写入较大范围内的订单统计数据
INSERT INTO tmp_large_order_summary (
    user_id,
    order_status,
    order_count,
    total_amount
)
SELECT
    user_id,
    order_status,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS total_amount
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-04-01 00:00:00'
GROUP BY user_id, order_status;
```

查询内部临时表相关状态：

```sql
-- 查看内部临时表创建情况
SHOW GLOBAL STATUS LIKE 'Created_tmp%';
```

常见指标说明：

| 指标                      | 说明                     |
| ------------------------- | ------------------------ |
| `Created_tmp_tables`      | 创建的内部临时表数量     |
| `Created_tmp_disk_tables` | 创建的磁盘内部临时表数量 |
| `Created_tmp_files`       | 创建的临时文件数量       |

容易产生内部临时表的 SQL：

```sql
-- GROUP BY 后按聚合结果排序，可能产生内部临时表和文件排序
EXPLAIN
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(pay_amount) AS total_pay_amount
FROM order_info
WHERE order_status = 1
GROUP BY user_id
ORDER BY total_pay_amount DESC;
```

磁盘临时表使用建议：

1. 大中间结果优先使用 InnoDB 临时表。
2. 大量磁盘临时表通常意味着 SQL 需要优化。
3. 复杂分组、排序、去重、联合查询可能产生内部临时表。
4. 可以通过索引、缩小数据范围、预聚合表减少临时表成本。
5. 定期关注 `Created_tmp_disk_tables`，判断是否存在大量落盘临时表。

### 临时表使用场景

临时表适合把复杂查询拆成多个步骤，或者保存某个任务中的中间结果。它常用于数据迁移、数据修复、批量导入、报表统计、复杂关联优化等场景。

常见使用场景：

| 场景         | 说明                         |
| ------------ | ---------------------------- |
| 复杂统计拆解 | 先统计中间结果，再关联维度表 |
| 批量数据修复 | 保存待修复主键集合           |
| 数据导入校验 | 保存导入数据并做去重、校验   |
| 大查询优化   | 先筛选小结果集，再关联详情   |
| 报表中间结果 | 暂存统计结果，简化后续 SQL   |
| 分批处理     | 保存待处理 ID，按批次执行    |

使用临时表保存待处理订单：

```sql
-- 创建待处理订单临时表
CREATE TEMPORARY TABLE tmp_pending_order (
    id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待处理订单临时表';
```

写入待处理订单：

```sql
-- 查询超时未支付订单并写入临时表
INSERT INTO tmp_pending_order (
    id,
    order_no
)
SELECT
    id,
    order_no
FROM order_info
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY id
LIMIT 5000;
```

基于临时表执行更新：

```sql
-- 基于临时表取消超时订单
UPDATE order_info AS o
JOIN tmp_pending_order AS t ON o.id = t.id
SET
    o.order_status = 2,
    o.updated_at = NOW()
WHERE o.order_status = 0;
```

临时表使用建议：

1. 临时表适合复杂任务的中间结果，不适合长期保存业务数据。
2. 临时表字段应尽量少，只保存后续处理需要的数据。
3. 临时表也需要主键和必要索引。
4. 批量更新或删除前，可以先用临时表固定目标范围。
5. 任务结束后主动删除临时表，避免连接池复用导致意外影响。

### 临时表性能影响

临时表可以简化 SQL，也可能增加额外写入、排序、磁盘 IO 和内存占用。使用临时表前，应判断它是否真的降低复杂度或提升性能，而不是把一个慢 SQL 拆成多个慢 SQL。

性能影响主要包括：

| 影响         | 说明                                  |
| ------------ | ------------------------------------- |
| 内存占用     | MEMORY 临时表和内部内存临时表占用内存 |
| 磁盘 IO      | 大临时表落盘后产生读写 IO             |
| 索引维护     | 临时表插入和更新也需要维护索引        |
| 连接生命周期 | 会话临时表依赖连接，连接池复用需谨慎  |
| 执行复杂度   | 多步骤 SQL 增加维护成本               |
| binlog 影响  | 临时表相关操作在复制场景中需关注行为  |

减少临时表成本的方式：

```sql
-- 临时表字段只保留必要列，不使用 SELECT *
CREATE TEMPORARY TABLE tmp_order_min AS
SELECT
    id,
    order_no,
    user_id
FROM order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

给临时表补充索引：

```sql
-- 给临时表增加后续关联需要的索引
ALTER TABLE tmp_order_min
  ADD PRIMARY KEY (id),
  ADD KEY idx_user_id (user_id);
```

临时表性能优化建议：

1. 控制临时表数据量，先过滤再写入。
2. 只保留必要字段，不写入大字段。
3. 根据后续查询补充索引。
4. 大批量任务优先分批处理。
5. 关注内部磁盘临时表数量和慢查询日志。
6. 对频繁使用的统计结果，应考虑汇总表，而不是每次创建临时表。

## 分区表

分区表是把一张逻辑表按规则拆分成多个物理分区。应用层仍然访问同一张表，MySQL 根据分区规则决定数据存放和扫描范围。分区表常用于大表管理、历史数据清理、按时间范围查询和冷热数据分离。

分区不是性能万能方案。分区设计不合理时，查询可能扫描多个分区，甚至比普通表更复杂。分区表更偏向数据管理能力，性能提升主要依赖分区裁剪和合理索引。

### RANGE 分区

`RANGE` 分区按字段值范围划分数据，最常见的是按日期、月份、年份或数值范围分区。适合订单、日志、流水、监控数据等按时间增长的大表。

按月份创建订单分区表：

```sql
-- 使用 RANGE COLUMNS 按创建日期范围分区
CREATE TABLE IF NOT EXISTS order_partition_range (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id, created_at),
    UNIQUE KEY uk_order_no_created_at (order_no, created_at),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单RANGE分区表'
PARTITION BY RANGE COLUMNS(created_at) (
    PARTITION p202601 VALUES LESS THAN ('2026-02-01'),
    PARTITION p202602 VALUES LESS THAN ('2026-03-01'),
    PARTITION p202603 VALUES LESS THAN ('2026-04-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

查询指定月份数据：

```sql
-- 查询 2026 年 1 月订单，分区裁剪后理论上只扫描 p202601
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_partition_range
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

查看分区信息：

```sql
-- 查看表分区信息
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    PARTITION_DESCRIPTION,
    TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_partition_range'
ORDER BY PARTITION_ORDINAL_POSITION;
```

`RANGE` 分区建议：

1. 最常见分区字段是时间字段。
2. 查询条件应包含分区字段，才能触发分区裁剪。
3. 时间分区建议使用左闭右开范围查询。
4. 应提前创建未来分区，避免数据落入 `pmax`。
5. 分区表的唯一键通常需要包含分区字段。

### LIST 分区

`LIST` 分区按离散值列表划分数据，适合按地区、租户类型、业务线、状态集合等固定枚举值分区。相比 `RANGE`，`LIST` 更适合非连续取值。

按地区编码分区：

```sql
-- 使用 LIST COLUMNS 按省份编码分区
CREATE TABLE IF NOT EXISTS user_region_partition (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    province_code VARCHAR(20) NOT NULL COMMENT '省份编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id, province_code),
    KEY idx_user_id (user_id),
    KEY idx_province_status (province_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地区LIST分区表'
PARTITION BY LIST COLUMNS(province_code) (
    PARTITION p_zj VALUES IN ('ZJ'),
    PARTITION p_js VALUES IN ('JS'),
    PARTITION p_sh VALUES IN ('SH'),
    PARTITION p_other VALUES IN ('OTHER')
);
```

查询浙江用户：

```sql
-- 查询指定省份，理论上只扫描对应分区
SELECT
    id,
    user_id,
    username,
    province_code,
    status
FROM user_region_partition
WHERE province_code = 'ZJ'
  AND status = 1;
```

插入数据时，分区字段必须匹配某个分区值：

```sql
-- 插入用户数据
INSERT INTO user_region_partition (
    id,
    user_id,
    username,
    province_code,
    status
) VALUES (
    1,
    10001,
    'zhangsan',
    'ZJ',
    1
);
```

`LIST` 分区建议：

1. 适合固定枚举值分区。
2. 分区值集合应稳定，避免频繁调整。
3. 新增枚举值前要先新增或调整分区。
4. 不适合取值非常多且变化频繁的字段。
5. 查询条件应包含分区字段，否则仍可能扫描多个分区。

### HASH 分区

`HASH` 分区根据表达式的哈希结果把数据均匀分散到多个分区中。它适合数据分布均衡、没有明显时间范围特征、希望降低单个分区数据量的场景。

按用户 ID 哈希分区：

```sql
-- 使用 HASH 按 user_id 分成 8 个分区
CREATE TABLE IF NOT EXISTS user_behavior_hash_partition (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    behavior_type TINYINT NOT NULL COMMENT '行为类型：1浏览，2收藏，3购买',
    biz_id BIGINT NOT NULL COMMENT '业务ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id, user_id),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_behavior_type_created_at (behavior_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为HASH分区表'
PARTITION BY HASH(user_id)
PARTITIONS 8;
```

按用户查询：

```sql
-- 根据 user_id 查询时，可以定位到对应哈希分区
SELECT
    id,
    user_id,
    behavior_type,
    biz_id,
    created_at
FROM user_behavior_hash_partition
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

按时间查询可能无法有效裁剪：

```sql
-- 只按时间查询时，HASH 分区通常无法减少分区扫描范围
SELECT
    id,
    user_id,
    behavior_type,
    created_at
FROM user_behavior_hash_partition
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

`HASH` 分区建议：

1. 适合按用户 ID、租户 ID 等分散数据。
2. 哈希分区更偏向数据均衡，不适合按时间快速删除历史分区。
3. 查询条件包含哈希分区字段时更容易裁剪分区。
4. 分区数量不宜频繁调整，调整可能涉及大量数据重组。
5. 如果业务主要按时间清理数据，优先考虑 `RANGE` 分区。

### KEY 分区

`KEY` 分区类似 `HASH` 分区，但哈希算法由 MySQL 内部决定。它可以对非整数列分区，常用于按字符串编码、业务号等字段分散数据。

按订单号 KEY 分区：

```sql
-- 使用 KEY 分区按订单号分散数据
CREATE TABLE IF NOT EXISTS order_key_partition (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id, order_no),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单KEY分区表'
PARTITION BY KEY(order_no)
PARTITIONS 8;
```

按订单号查询：

```sql
-- 按订单号查询时，可以利用 KEY 分区定位
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_key_partition
WHERE order_no = 'O202601010001';
```

`KEY` 分区建议：

1. 适合字符串字段分区。
2. 分区算法由 MySQL 控制，可控性不如 `HASH` 表达式。
3. 适合按业务编码均匀分散数据的场景。
4. 不适合按时间范围管理历史数据。
5. 分区字段应是高频查询条件之一。

### 分区字段选择

分区字段选择决定分区表是否真正有效。分区字段应来自高频查询条件、数据生命周期管理条件或数据天然分布维度。错误的分区字段会导致分区无法裁剪，反而增加管理复杂度。

常见分区字段选择：

| 场景         | 推荐分区字段  | 推荐分区方式     |
| ------------ | ------------- | ---------------- |
| 订单历史数据 | `created_at`  | `RANGE`          |
| 日志数据     | `log_time`    | `RANGE`          |
| 流水数据     | `trade_time`  | `RANGE`          |
| 用户行为     | `user_id`     | `HASH`           |
| 租户数据     | `tenant_id`   | `HASH` 或 `LIST` |
| 地区数据     | `region_code` | `LIST`           |
| 按月清理数据 | 时间字段      | `RANGE`          |

按创建时间分区适合订单历史清理：

```sql
-- 按月份查询和清理都需要 created_at 作为分区字段
SELECT
    COUNT(*) AS order_count
FROM order_partition_range
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

不合理分区字段示例：

```text
订单表主要查询条件：
- created_at 时间范围
- user_id 用户ID
- order_status 订单状态

如果按 pay_amount 分区：
- 大多数查询无法使用分区裁剪
- 历史数据无法按月份快速删除
- 分区维护复杂度增加
```

分区字段选择建议：

1. 分区字段必须频繁出现在查询条件中。
2. 如果主要目的是按时间清理历史数据，优先选择时间字段。
3. 如果主要目的是均衡数据，考虑 `HASH` 或 `KEY`。
4. 分区字段取值不能严重倾斜，否则部分分区会过大。
5. 分区字段变更成本很高，设计前应充分评估查询模式。

### 分区裁剪

分区裁剪是指 MySQL 根据查询条件只扫描相关分区，而不是扫描全部分区。分区表是否有效，很大程度取决于查询能否触发分区裁剪。

使用分区字段过滤：

```sql
-- 查询条件包含分区字段 created_at，有机会裁剪分区
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_partition_range
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00';
```

查看访问分区：

```sql
-- EXPLAIN 输出中的 partitions 字段可以查看访问了哪些分区
EXPLAIN
SELECT
    id,
    order_no,
    created_at
FROM order_partition_range
WHERE created_at >= '2026-02-01 00:00:00'
  AND created_at <  '2026-03-01 00:00:00';
```

无法有效裁剪的查询：

```sql
-- 没有分区字段条件，可能扫描所有分区
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount
FROM order_partition_range
WHERE user_id = 10001;
```

不推荐对分区字段使用函数：

```sql
-- 不推荐：对分区字段使用函数，可能影响分区裁剪和索引使用
EXPLAIN
SELECT
    id,
    order_no,
    created_at
FROM order_partition_range
WHERE DATE(created_at) = '2026-01-01';
```

推荐写法：

```sql
-- 推荐：使用原始字段范围查询
EXPLAIN
SELECT
    id,
    order_no,
    created_at
FROM order_partition_range
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00';
```

分区裁剪建议：

1. 查询条件应包含分区字段。
2. 分区字段尽量不要套函数。
3. 时间分区使用左闭右开范围查询。
4. 通过 `EXPLAIN` 的 `partitions` 字段确认扫描分区。
5. 如果核心查询无法分区裁剪，说明分区设计可能不适合该业务。

### 分区维护

分区维护包括新增分区、删除分区、重组分区、查看分区、分析分区等。分区维护是分区表的重要价值之一，尤其适合按时间快速清理历史数据。

新增分区：

```sql
-- 为 2026 年 4 月新增分区
ALTER TABLE order_partition_range
ADD PARTITION (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01')
);
```

删除历史分区：

```sql
-- 删除 2026 年 1 月分区，分区内数据会被直接删除
ALTER TABLE order_partition_range
DROP PARTITION p202601;
```

重组 `pmax` 分区：

```sql
-- 将 pmax 重组为 2026年4月分区和新的 pmax
ALTER TABLE order_partition_range
REORGANIZE PARTITION pmax INTO (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

查看分区行数估算：

```sql
-- 查看分区元信息
SELECT
    PARTITION_NAME,
    PARTITION_DESCRIPTION,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_partition_range'
ORDER BY PARTITION_ORDINAL_POSITION;
```

分析分区：

```sql
-- 分析指定分区，更新统计信息
ALTER TABLE order_partition_range
ANALYZE PARTITION p202602;
```

分区维护建议：

1. 定期提前创建未来分区。
2. 使用 `DROP PARTITION` 清理历史数据前必须确认备份和保留策略。
3. 删除分区是直接删除该分区数据，不是普通 `DELETE`。
4. 维护脚本应纳入变更管理。
5. 分区命名应规范，例如 `p202601`、`p202602`。
6. 需要监控数据是否落入 `pmax`，避免未来分区缺失。

### 分区表使用限制

分区表有一些限制和额外复杂度。使用前必须确认表结构、唯一键、外键、查询模式和维护方式是否适合分区。

常见限制和注意点：

| 限制             | 说明                                   |
| ---------------- | -------------------------------------- |
| 唯一键限制       | 分区表中所有唯一键通常必须包含分区字段 |
| 外键限制         | 分区表与外键支持存在限制，应谨慎设计   |
| 全局索引限制     | MySQL 分区表索引通常是分区内本地索引   |
| 查询不含分区字段 | 可能扫描全部分区                       |
| 分区过多         | 增加优化器和维护成本                   |
| DDL 复杂         | 分区维护需要更严格变更管理             |
| 不适合小表       | 小表分区收益很低                       |
| 错误分区字段     | 可能比普通表更难优化                   |

唯一键必须包含分区字段示例：

```sql
-- 分区字段为 created_at，因此唯一键中包含 created_at
CREATE TABLE IF NOT EXISTS order_partition_unique_demo (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id, created_at),
    UNIQUE KEY uk_order_no_created_at (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分区唯一键示例表'
PARTITION BY RANGE COLUMNS(created_at) (
    PARTITION p202601 VALUES LESS THAN ('2026-02-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

不适合分区的场景：

```text
不适合分区的表：
1. 数据量不大
2. 查询条件很少包含分区字段
3. 表结构频繁变更
4. 唯一键无法包含分区字段
5. 希望用分区替代合理索引
6. 业务无法接受分区维护复杂度
```

分区表使用建议：

1. 数据量足够大且有明确分区收益时再使用。
2. 分区字段必须与核心查询和清理策略匹配。
3. 不要用分区替代索引。
4. 分区数量要控制，不宜过多。
5. 上线前必须用真实查询验证分区裁剪。
6. 分区维护应自动化并纳入监控。

## 全文检索

全文检索用于在文本字段中搜索关键词，并按相关性返回结果。MySQL 支持 `FULLTEXT` 全文索引和 `MATCH ... AGAINST` 查询。它适合简单文本搜索，例如文章标题、正文、商品描述、知识库内容等。

需要注意，MySQL 全文检索不是专业搜索引擎。复杂中文分词、拼音搜索、同义词、纠错、高亮、权重调优、海量搜索等场景，通常更适合 Elasticsearch、OpenSearch、Solr 等搜索系统。

本节示例默认使用以下文章表：

```sql
-- 文章表，用于演示全文检索
CREATE TABLE IF NOT EXISTS article_fulltext_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT NOT NULL COMMENT '文章正文',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0草稿，1发布，2下架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FULLTEXT KEY ft_title_content (title, content),
    KEY idx_status_created_at (status, created_at),
    KEY idx_author_id_created_at (author_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章全文检索示例表';
```

### 全文索引创建

全文索引使用 `FULLTEXT` 创建，可以建立在 `CHAR`、`VARCHAR`、`TEXT` 类型字段上。InnoDB 支持全文索引，适合对长文本字段进行关键词检索。

建表时创建全文索引：

```sql
-- 建表时创建全文索引
CREATE TABLE IF NOT EXISTS product_search_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_desc TEXT NOT NULL COMMENT '商品描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FULLTEXT KEY ft_product_name_desc (product_name, product_desc),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品全文检索示例表';
```

为已有表添加全文索引：

```sql
-- 为已有文章表添加全文索引
ALTER TABLE article_fulltext_demo
  ADD FULLTEXT KEY ft_title_content (title, content);
```

删除全文索引：

```sql
-- 删除全文索引
ALTER TABLE article_fulltext_demo
  DROP INDEX ft_title_content;
```

查看全文索引：

```sql
-- 查看表索引
SHOW INDEX FROM article_fulltext_demo;
```

全文索引创建建议：

1. 适合文章标题、正文、商品描述、备注说明等文本字段。
2. 不适合替代普通等值查询索引。
3. 写入频繁的表添加全文索引会增加维护成本。
4. 字段过短、关键词过少时，全文检索效果可能不明显。
5. 中文检索需要额外关注分词解析器和分词粒度。

### MATCH AGAINST 查询

全文检索使用 `MATCH(column_list) AGAINST(keyword)` 语法。`MATCH` 中的字段必须与全文索引字段匹配或被全文索引覆盖。

基础全文查询：

```sql
-- 查询包含 MySQL 索引 相关内容的文章
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('MySQL 索引') AS score,
    created_at
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('MySQL 索引')
  AND status = 1
ORDER BY score DESC;
```

查询商品：

```sql
-- 查询商品名称或描述中匹配关键词的数据
SELECT
    id,
    product_name,
    MATCH(product_name, product_desc) AGAINST('蓝牙 耳机') AS score,
    created_at
FROM product_search_demo
WHERE MATCH(product_name, product_desc) AGAINST('蓝牙 耳机')
  AND status = 1
ORDER BY score DESC, created_at DESC;
```

只返回相关性分数大于 0 的结果：

```sql
-- 使用全文相关性分数排序
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('事务 隔离 锁') AS relevance_score
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('事务 隔离 锁') > 0
ORDER BY relevance_score DESC;
```

全文查询建议：

1. `MATCH` 字段列表应与全文索引字段一致。
2. 相关性分数可以作为排序依据。
3. 业务状态、时间范围等普通条件仍应使用普通索引辅助过滤。
4. 不建议对全文检索结果再做大范围复杂排序。
5. 复杂搜索需求应考虑专业搜索引擎。

### 自然语言模式

自然语言模式是 `MATCH ... AGAINST` 的默认模式，用于按自然语言相关性检索。它适合普通关键词搜索，MySQL 会根据词频、字段内容和相关性计算得分。

自然语言模式查询：

```sql
-- 自然语言模式全文检索
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('MySQL 执行计划 优化' IN NATURAL LANGUAGE MODE) AS score,
    created_at
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('MySQL 执行计划 优化' IN NATURAL LANGUAGE MODE)
  AND status = 1
ORDER BY score DESC;
```

默认模式等价写法：

```sql
-- 不指定模式时，默认通常为自然语言模式
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('MySQL 执行计划 优化') AS score
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('MySQL 执行计划 优化');
```

结合普通条件：

```sql
-- 先限制发布状态，再按全文相关性排序
SELECT
    id,
    title,
    author_id,
    MATCH(title, content) AGAINST('索引 优化') AS score,
    created_at
FROM article_fulltext_demo
WHERE status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND MATCH(title, content) AGAINST('索引 优化')
ORDER BY score DESC, created_at DESC;
```

自然语言模式建议：

1. 适合普通用户关键词搜索。
2. 不需要显式指定必须包含或排除某个词。
3. 查询结果按相关性排序更自然。
4. 短词、停用词、低区分度词可能影响检索效果。
5. 中文场景要关注分词器，否则搜索效果可能不符合预期。

### 布尔模式

布尔模式使用 `IN BOOLEAN MODE`，支持必须包含、必须排除、前缀匹配、权重控制等语法。它适合更明确的检索条件，例如必须包含某个词、排除某个词、匹配某类前缀。

布尔模式常见操作符：

| 操作符 | 说明       | 示例                   |
| ------ | ---------- | ---------------------- |
| `+`    | 必须包含   | `+MySQL +索引`         |
| `-`    | 必须不包含 | `+MySQL -Oracle`       |
| `*`    | 前缀匹配   | `optim*`               |
| `"`    | 短语匹配   | `"query optimization"` |
| `>`    | 提高词权重 | `>MySQL 索引`          |
| `<`    | 降低词权重 | `<基础 高级`           |

必须包含两个关键词：

```sql
-- 必须包含 MySQL 和 索引
SELECT
    id,
    title,
    content
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('+MySQL +索引' IN BOOLEAN MODE)
  AND status = 1;
```

包含 MySQL 但排除 Oracle：

```sql
-- 包含 MySQL，排除 Oracle
SELECT
    id,
    title
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('+MySQL -Oracle' IN BOOLEAN MODE);
```

前缀匹配：

```sql
-- 匹配 optimization、optimize 等前缀
SELECT
    id,
    title
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('optim*' IN BOOLEAN MODE);
```

短语匹配：

```sql
-- 匹配完整短语 query optimization
SELECT
    id,
    title
FROM article_fulltext_demo
WHERE MATCH(title, content) AGAINST('"query optimization"' IN BOOLEAN MODE);
```

布尔模式建议：

1. 适合高级搜索和明确搜索条件。
2. 用户输入直接拼接到布尔表达式前应做安全处理。
3. 布尔操作符需要转义或过滤，避免搜索语义被恶意构造。
4. 布尔模式不一定按自然相关性返回，需要结合业务排序。
5. 对普通用户搜索框，自然语言模式通常更简单；对高级搜索可以使用布尔模式。

### 中文全文检索限制

中文全文检索的核心问题是分词。英文文本天然用空格分隔单词，而中文没有空格分隔，普通全文检索可能无法得到理想效果。MySQL 提供 ngram 解析器用于中日韩文本分词，但分词粒度、相关性排序和搜索体验仍不如专业搜索引擎灵活。

使用 ngram 解析器创建全文索引：

```sql
-- 使用 ngram parser 创建中文全文索引
CREATE TABLE IF NOT EXISTS article_ngram_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT NOT NULL COMMENT '文章正文',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0草稿，1发布',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FULLTEXT KEY ft_title_content_ngram (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中文全文检索ngram示例表';
```

中文全文查询：

```sql
-- 查询中文关键词
SELECT
    id,
    title,
    MATCH(title, content) AGAINST('索引优化' IN NATURAL LANGUAGE MODE) AS score
FROM article_ngram_demo
WHERE MATCH(title, content) AGAINST('索引优化' IN NATURAL LANGUAGE MODE)
  AND status = 1
ORDER BY score DESC;
```

布尔模式查询中文：

```sql
-- 布尔模式查询中文关键词
SELECT
    id,
    title
FROM article_ngram_demo
WHERE MATCH(title, content) AGAINST('+索引 +优化' IN BOOLEAN MODE);
```

查看 ngram 分词大小配置：

```sql
-- 查看 ngram 分词大小
SHOW VARIABLES LIKE 'ngram_token_size';
```

中文全文检索限制：

1. ngram 分词是按固定长度切词，不理解业务语义。
2. 同义词、拼音、错别字、简繁转换支持有限。
3. 高亮、权重调优、复杂排序能力有限。
4. 相关性得分可能不符合业务预期。
5. 大规模搜索和复杂搜索建议使用 Elasticsearch、OpenSearch 等搜索引擎。
6. 修改 `ngram_token_size` 后通常需要重建全文索引才能生效。

中文检索建议：

| 场景         | 建议                         |
| ------------ | ---------------------------- |
| 简单后台搜索 | MySQL ngram 全文索引可以考虑 |
| 商品搜索     | 推荐搜索引擎                 |
| 文档搜索     | 推荐搜索引擎                 |
| 拼音搜索     | 推荐搜索引擎或专门字段       |
| 同义词搜索   | 推荐搜索引擎                 |
| 高亮摘要     | 推荐搜索引擎                 |
| 权重排序复杂 | 推荐搜索引擎                 |

### 全文索引使用场景

全文索引适合中小规模、功能简单、对搜索体验要求不高的文本检索。它可以比 `LIKE '%关键词%'` 更适合长文本搜索，但不能完全替代搜索引擎。

适合使用全文索引的场景：

| 场景               | 说明           |
| ------------------ | -------------- |
| 文章标题和正文搜索 | 简单关键词查询 |
| 商品名称和描述搜索 | 小规模后台搜索 |
| 帮助文档搜索       | 简单文档检索   |
| 评论内容搜索       | 后台审核检索   |
| 日志文本搜索       | 小范围文本定位 |
| 知识库搜索         | 简单关键词匹配 |

不适合使用全文索引的场景：

| 场景           | 原因                               |
| -------------- | ---------------------------------- |
| 大规模商品搜索 | 需要分词、权重、筛选、排序综合能力 |
| 搜索高亮       | MySQL 原生能力有限                 |
| 拼音和同义词   | 支持较弱                           |
| 个性化排序     | 更适合搜索引擎                     |
| 多条件复杂检索 | 搜索引擎更灵活                     |
| 海量日志检索   | 更适合日志系统或搜索引擎           |

全文索引与 `LIKE` 对比：

| 对比项       | FULLTEXT            | LIKE                   |
| ------------ | ------------------- | ---------------------- |
| 长文本搜索   | 更适合              | 较差                   |
| 相关性排序   | 支持                | 不支持                 |
| 前缀匹配     | 布尔模式支持        | 支持                   |
| 包含匹配     | 支持关键词匹配      | `LIKE '%词%'` 简单直接 |
| 中文支持     | 需要 ngram 等解析器 | 无分词，仅字符串匹配   |
| 复杂搜索体验 | 一般                | 较弱                   |
| 实现复杂度   | 中等                | 简单                   |

全文检索综合示例：

```sql
-- 后台文章搜索：状态过滤 + 全文检索 + 相关性排序 + 时间排序
SELECT
    id,
    title,
    author_id,
    MATCH(title, content) AGAINST('MySQL 索引 优化' IN NATURAL LANGUAGE MODE) AS score,
    created_at
FROM article_fulltext_demo
WHERE status = 1
  AND MATCH(title, content) AGAINST('MySQL 索引 优化' IN NATURAL LANGUAGE MODE)
ORDER BY score DESC, created_at DESC
LIMIT 20;
```

全文检索使用建议：

1. 简单搜索可以使用 MySQL 全文索引。
2. 不要用全文索引替代普通等值索引和范围索引。
3. 搜索字段应控制数量，避免全文索引过大。
4. 写入频繁表要评估全文索引维护成本。
5. 中文搜索要测试 ngram 效果。
6. 搜索体验要求较高时，应引入专业搜索引擎。


## 数据导入与导出

数据导入与导出用于数据库迁移、环境初始化、数据备份、离线分析、批量导入、问题排查和数据恢复。MySQL 常见方式包括 `mysqldump`、`mysql` 命令、`LOAD DATA`、`SELECT ... INTO OUTFILE`、CSV 文件导入导出等。

导入导出操作涉及数据安全、字符集、权限、锁表、事务、文件路径、数据一致性和大数据量性能。生产环境执行前应明确数据范围、目标环境、备份策略和回滚方案。

### mysqldump 导出

`mysqldump` 是 MySQL 常用逻辑导出工具，可以导出库、表、表结构和数据。它生成的是 SQL 文本文件，通常包含 `CREATE TABLE`、`INSERT` 等语句。

导出单个数据库：

```bash
# 导出 mall_order 数据库到 SQL 文件
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order \
  > mall_order.sql
```

导出多个数据库：

```bash
# 导出多个数据库
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --databases mall_order mall_user \
  > mall_multi_db.sql
```

导出所有数据库：

```bash
# 导出当前实例中的所有数据库
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --all-databases \
  > all_databases.sql
```

只导出表结构，不导出数据：

```bash
# 只导出 mall_order 库的表结构
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --no-data \
  mall_order \
  > mall_order_schema.sql
```

只导出数据，不导出建表语句：

```bash
# 只导出数据，不包含 CREATE TABLE
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --no-create-info \
  mall_order \
  > mall_order_data.sql
```

导出指定表：

```bash
# 导出指定表 order_info 和 order_item
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order order_info order_item \
  > mall_order_tables.sql
```

按条件导出指定表数据：

```bash
# 只导出 2026 年 1 月订单数据
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --where="created_at >= '2026-01-01 00:00:00' AND created_at < '2026-02-01 00:00:00'" \
  mall_order order_info \
  > order_info_202601.sql
```

生产导出 InnoDB 表常用参数：

```bash
# 使用 single-transaction 导出 InnoDB 表，减少锁表影响
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  mall_order \
  > mall_order_backup.sql
```

常用参数说明：

| 参数                      | 说明                                         |
| ------------------------- | -------------------------------------------- |
| `--single-transaction`    | 基于事务一致性快照导出 InnoDB 数据，减少锁表 |
| `--quick`                 | 逐行读取数据，降低客户端内存占用             |
| `--routines`              | 导出存储过程和存储函数                       |
| `--triggers`              | 导出触发器                                   |
| `--events`                | 导出事件调度器                               |
| `--no-data`               | 只导出结构                                   |
| `--no-create-info`        | 只导出数据                                   |
| `--where`                 | 按条件导出表数据                             |
| `--default-character-set` | 指定字符集                                   |
| `--databases`             | 导出多个数据库，并包含 `CREATE DATABASE`     |
| `--all-databases`         | 导出所有数据库                               |

压缩导出：

```bash
# 导出后使用 gzip 压缩，适合备份文件较大的场景
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  mall_order \
  | gzip > mall_order_backup.sql.gz
```

`mysqldump` 使用建议：

1. InnoDB 表导出优先使用 `--single-transaction`。
2. 大表导出建议使用 `--quick`，降低客户端内存占用。
3. 需要完整对象时增加 `--routines --triggers --events`。
4. 生产环境导出前确认磁盘空间。
5. 导出文件包含敏感数据时，应加密存储并控制访问权限。
6. 不建议在业务高峰对大库执行全量导出。

### mysql 命令导入

`mysql` 命令可以执行 SQL 文件，常用于恢复 `mysqldump` 导出的逻辑备份、初始化数据库结构、导入测试数据等。

导入 SQL 文件：

```bash
# 将 mall_order.sql 导入 mall_order 数据库
mysql \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order \
  < mall_order.sql
```

导入包含 `CREATE DATABASE` 的 SQL 文件：

```bash
# 如果 SQL 文件中已经包含 CREATE DATABASE 和 USE 语句，可以不指定数据库名
mysql \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  < all_databases.sql
```

导入压缩 SQL 文件：

```bash
# 解压 gzip 文件并导入数据库
gunzip -c mall_order_backup.sql.gz \
  | mysql \
      -h 127.0.0.1 \
      -P 3306 \
      -u root \
      -p \
      --default-character-set=utf8mb4 \
      mall_order
```

在 MySQL 客户端中导入：

```sql
-- 在 mysql 客户端内执行 SQL 文件
SOURCE /data/backup/mall_order.sql;
```

导入前创建数据库：

```sql
-- 创建目标数据库并指定字符集
CREATE DATABASE IF NOT EXISTS mall_order
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

导入前检查目标库：

```sql
-- 查看当前数据库
SELECT DATABASE();

-- 查看目标库表数量
SELECT
    COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'mall_order';
```

导入建议：

1. 导入前确认目标库是否为空，避免覆盖或重复数据。
2. 导入前确认字符集，推荐使用 `utf8mb4`。
3. 大文件导入前确认 `max_allowed_packet`。
4. 导入生产环境前应先在测试环境验证。
5. 导入失败后不要盲目重复导入，应先确认已导入到哪一步。
6. 重要数据导入前先备份目标库。

### LOAD DATA 导入

`LOAD DATA` 用于从文本文件高速导入数据到表中，通常比大量 `INSERT` 更快。它适合导入 CSV、TSV、日志文件、批量数据文件等。

创建导入目标表：

```sql
-- 创建用户导入表
CREATE TABLE IF NOT EXISTS import_user_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户导入示例表';
```

从 CSV 文件导入：

```sql
-- 从服务端文件导入 CSV 数据
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);
```

从客户端本地文件导入：

```sql
-- 从客户端本地文件导入，需要客户端和服务端允许 local_infile
LOAD DATA LOCAL INFILE '/tmp/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);
```

处理导入字段转换：

```sql
-- 使用用户变量接收 CSV 字段，并做数据清洗后写入表字段
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@username, @nickname, @mobile, @email, @status)
SET
    username = TRIM(@username),
    nickname = NULLIF(TRIM(@nickname), ''),
    mobile = NULLIF(TRIM(@mobile), ''),
    email = NULLIF(TRIM(@email), ''),
    status = IFNULL(NULLIF(@status, ''), 1);
```

查看文件导入目录限制：

```sql
-- 查看 secure_file_priv 配置
SHOW VARIABLES LIKE 'secure_file_priv';
```

`LOAD DATA` 常见限制：

| 配置或权限         | 说明                                     |
| ------------------ | ---------------------------------------- |
| `secure_file_priv` | 限制服务端可导入导出的目录               |
| `local_infile`     | 控制是否允许 `LOAD DATA LOCAL INFILE`    |
| `FILE` 权限        | 使用服务端文件导入导出通常需要 FILE 权限 |
| 文件权限           | MySQL 进程需要能读取导入文件             |
| 字符集             | 文件字符集要与导入设置一致               |

`LOAD DATA` 使用建议：

1. 大批量导入优先考虑 `LOAD DATA`。
2. 文件放置路径应符合 `secure_file_priv` 限制。
3. 导入前确认字段顺序、分隔符、换行符和字符集。
4. 建议先导入临时表，再校验后写入正式表。
5. 导入敏感数据文件后应及时清理文件。
6. 生产环境谨慎开启 `local_infile`。

### SELECT INTO OUTFILE 导出

`SELECT ... INTO OUTFILE` 可以将查询结果导出到 MySQL 服务端文件系统。它适合导出 CSV、报表数据、抽样数据等。

导出 CSV 文件：

```sql
-- 将启用用户导出为 CSV 文件
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status,
    created_at
FROM import_user_demo
WHERE status = 1
INTO OUTFILE '/var/lib/mysql-files/export_user.csv'
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

导出订单报表：

```sql
-- 导出指定月份订单报表
SELECT
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
INTO OUTFILE '/var/lib/mysql-files/order_202601.csv'
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

注意：`SELECT ... INTO OUTFILE` 不会自动导出表头。如果需要表头，可以使用 `UNION ALL` 拼接：

```sql
-- 导出带表头的 CSV 文件
SELECT
    'order_no',
    'user_id',
    'order_status',
    'pay_amount',
    'created_at'
UNION ALL
SELECT
    order_no,
    CAST(user_id AS CHAR),
    CAST(order_status AS CHAR),
    CAST(pay_amount AS CHAR),
    DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s')
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
INTO OUTFILE '/var/lib/mysql-files/order_202601_with_header.csv'
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

`SELECT INTO OUTFILE` 使用建议：

1. 导出路径受 `secure_file_priv` 限制。
2. 目标文件不能已存在，否则会报错。
3. MySQL 服务端进程需要有目录写权限。
4. 导出敏感数据时必须控制文件权限和清理周期。
5. 大数据量导出应避开业务高峰。
6. 如果无法访问服务端文件系统，可以使用客户端工具或程序导出。

### CSV 数据导入

CSV 是常见的数据交换格式。导入 CSV 时，最重要的是确认分隔符、引号、换行符、字符集、表头、空值和字段顺序。

示例 CSV 内容：

```csv
username,nickname,mobile,email,status
zhangsan,张三,13800000001,zhangsan@example.com,1
lisi,李四,13800000002,lisi@example.com,1
wangwu,王五,13800000003,wangwu@example.com,0
```

创建导入临时表：

```sql
-- 创建 CSV 导入临时表，先保存原始文本
CREATE TABLE IF NOT EXISTS import_user_raw (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status_text VARCHAR(20) DEFAULT NULL COMMENT '状态文本',
    import_batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_batch_no (import_batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户CSV导入原始表';
```

导入 CSV 到原始表：

```sql
-- 导入 CSV 到原始表，并记录导入批次号
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_raw
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status_text)
SET import_batch_no = 'BATCH_20260101_001';
```

校验导入数据：

```sql
-- 检查用户名为空的数据
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status_text
FROM import_user_raw
WHERE import_batch_no = 'BATCH_20260101_001'
  AND (username IS NULL OR TRIM(username) = '');

-- 检查手机号重复
SELECT
    mobile,
    COUNT(*) AS duplicate_count
FROM import_user_raw
WHERE import_batch_no = 'BATCH_20260101_001'
  AND mobile IS NOT NULL
  AND mobile <> ''
GROUP BY mobile
HAVING COUNT(*) > 1;
```

校验后写入正式表：

```sql
-- 清洗后写入正式用户表
INSERT INTO import_user_demo (
    username,
    nickname,
    mobile,
    email,
    status
)
SELECT
    TRIM(username) AS username,
    NULLIF(TRIM(nickname), '') AS nickname,
    NULLIF(TRIM(mobile), '') AS mobile,
    NULLIF(TRIM(email), '') AS email,
    CAST(IFNULL(NULLIF(status_text, ''), '1') AS UNSIGNED) AS status
FROM import_user_raw
WHERE import_batch_no = 'BATCH_20260101_001'
  AND username IS NOT NULL
  AND TRIM(username) <> ''
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    mobile = VALUES(mobile),
    email = VALUES(email),
    status = VALUES(status);
```

CSV 导入建议：

1. 生产导入建议先进入原始表，再清洗进入正式表。
2. 使用批次号标识每次导入数据。
3. 导入前确认 CSV 是否包含表头。
4. 中文文件应确认字符集是 `utf8mb4` 或可兼容格式。
5. 空字符串和 `NULL` 要有统一处理规则。
6. 导入后应校验总数、重复数据、非法数据和正式表写入数量。

### CSV 数据导出

CSV 导出常用于报表、数据交换、离线分析和问题排查。导出时要注意字段顺序、表头、字符集、换行符、敏感数据脱敏和文件权限。

导出用户 CSV：

```sql
-- 导出用户数据为 CSV
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    status,
    created_at
FROM import_user_demo
INTO OUTFILE '/var/lib/mysql-files/export_user.csv'
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

导出脱敏用户 CSV：

```sql
-- 导出脱敏后的用户数据，避免泄露完整手机号和邮箱
SELECT
    id,
    username,
    nickname,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask,
    CASE
        WHEN email IS NULL OR LOCATE('@', email) <= 1 THEN NULL
        ELSE CONCAT(LEFT(email, 2), '****', SUBSTRING(email, LOCATE('@', email)))
    END AS email_mask,
    status,
    created_at
FROM import_user_demo
INTO OUTFILE '/var/lib/mysql-files/export_user_mask.csv'
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

使用命令行客户端导出 CSV 风格结果：

```bash
# 使用 mysql 客户端导出制表符分隔数据，再按需转换
mysql \
  -h 127.0.0.1 \
  -P 3306 \
  -u report_user \
  -p \
  --default-character-set=utf8mb4 \
  --batch \
  --raw \
  -e "SELECT id, username, nickname, status, created_at FROM mall_order.import_user_demo" \
  > export_user.tsv
```

CSV 导出建议：

1. 导出敏感数据前必须确认权限和脱敏要求。
2. 服务端导出路径受 `secure_file_priv` 限制。
3. 导出文件不应长期保留在数据库服务器。
4. 大文件导出应压缩、加密并记录审计。
5. 对外提供 CSV 时，应明确字段说明和字符集。
6. Excel 打开 CSV 可能出现编码问题，可使用 UTF-8 BOM 或改用 XLSX 交付。

### 大数据量导入优化

大数据量导入的瓶颈通常包括索引维护、事务日志、唯一检查、外键检查、磁盘 IO、网络传输和单批次数据过大。优化目标是减少重复提交、降低索引维护成本、避免大事务和控制资源占用。

常用优化方式：

| 优化方式         | 说明                                |
| ---------------- | ----------------------------------- |
| 使用 `LOAD DATA` | 通常比大量 `INSERT` 快              |
| 分批导入         | 控制单次事务大小                    |
| 临时关闭自动提交 | 减少每行提交成本                    |
| 先导入临时表     | 先清洗校验，再写正式表              |
| 合理禁用检查     | 特定场景可临时关闭外键或唯一检查    |
| 延后创建索引     | 空表大批量导入后再建索引可能更快    |
| 避开业务高峰     | 降低对线上业务影响                  |
| 调整参数         | 按环境调整 buffer、packet、log 配置 |

关闭自动提交并批量导入：

```sql
-- 当前会话关闭自动提交，导入后手动提交
SET autocommit = 0;

LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);

COMMIT;

SET autocommit = 1;
```

导入前临时关闭外键检查：

```sql
-- 临时关闭当前会话外键检查，适合确认数据一致性的批量导入场景
SET FOREIGN_KEY_CHECKS = 0;

-- 执行导入操作
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);

SET FOREIGN_KEY_CHECKS = 1;
```

导入前临时关闭唯一检查：

```sql
-- 临时关闭唯一检查，需确保导入数据不会破坏唯一性
SET UNIQUE_CHECKS = 0;

-- 执行导入操作
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);

SET UNIQUE_CHECKS = 1;
```

先导入无索引中间表，再写入正式表：

```sql
-- 创建中间表，减少导入阶段索引维护成本
CREATE TABLE IF NOT EXISTS import_user_stage (
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT DEFAULT NULL COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户导入中间表';
```

将 CSV 导入中间表后再清洗写入正式表：

```sql
-- 先导入中间表
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
INTO TABLE import_user_stage
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);

-- 再写入正式表，利用正式表约束做最终控制
INSERT INTO import_user_demo (
    username,
    nickname,
    mobile,
    email,
    status
)
SELECT
    TRIM(username),
    NULLIF(TRIM(nickname), ''),
    NULLIF(TRIM(mobile), ''),
    NULLIF(TRIM(email), ''),
    IFNULL(status, 1)
FROM import_user_stage
WHERE username IS NOT NULL
  AND TRIM(username) <> '';
```

大数据量导入建议：

1. 优先使用 `LOAD DATA`。
2. 大文件分批导入，避免单个事务过大。
3. 空表导入大量数据时，可考虑先导入再建索引。
4. 关闭检查项前必须确认数据质量，否则会引入脏数据。
5. 导入期间监控 CPU、IO、锁等待、磁盘空间和复制延迟。
6. 主从环境大批量导入可能造成从库延迟，应提前评估。

### 数据导入异常处理

数据导入异常常见原因包括字段数量不匹配、字符集错误、文件路径不允许、权限不足、唯一键冲突、数据类型转换失败、日期格式错误、文件换行符不一致等。

常见异常和处理方式：

| 异常                | 常见原因                    | 处理方式                 |
| ------------------- | --------------------------- | ------------------------ |
| 文件无法读取        | 路径不在 `secure_file_priv` | 移动文件或调整配置       |
| `local_infile` 禁用 | 客户端或服务端未开启        | 开启 `local_infile`      |
| 字符乱码            | 文件字符集不一致            | 指定正确 `CHARACTER SET` |
| 字段错位            | 分隔符或引号设置错误        | 检查 `FIELDS` 配置       |
| 重复键错误          | 唯一索引冲突                | 清洗数据或使用 UPSERT    |
| 日期导入失败        | 日期格式不合法              | 使用变量接收后转换       |
| 空值异常            | `NOT NULL` 字段为空         | 导入前清洗或设置默认值   |
| 数据截断            | 字段长度不够                | 调整字段长度或清洗数据   |

查看导入警告：

```sql
-- 查看最近语句产生的警告
SHOW WARNINGS;
```

使用 `IGNORE` 忽略部分错误：

```sql
-- 使用 IGNORE 跳过部分重复或异常数据，需谨慎使用
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
IGNORE
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);
```

使用 `REPLACE` 替换重复数据：

```sql
-- 使用 REPLACE 遇到唯一键冲突时替换旧数据，需谨慎
LOAD DATA INFILE '/var/lib/mysql-files/import_user.csv'
REPLACE
INTO TABLE import_user_demo
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(username, nickname, mobile, email, status);
```

更推荐的异常处理流程：

```text
1. CSV 导入原始表
2. 校验字段为空、重复、格式错误、类型错误
3. 输出错误数据清单
4. 清洗合法数据
5. 写入正式表
6. 记录导入批次、总数、成功数、失败数
```

创建导入批次日志表：

```sql
-- 导入批次日志表
CREATE TABLE IF NOT EXISTS import_batch_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    total_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总行数',
    success_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功行数',
    fail_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败行数',
    import_status VARCHAR(20) NOT NULL COMMENT '导入状态',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入批次日志表';
```

数据导入异常处理建议：

1. 不要把外部文件直接导入正式核心表。
2. 使用原始表承接数据，保留导入批次。
3. 导入后先校验，再写入正式表。
4. 对失败数据输出清单，便于业务修正。
5. 对重复数据明确策略：跳过、覆盖、更新或报错。
6. 导入过程要有日志，便于审计和回滚。

## 备份与恢复

备份与恢复是数据库运维的核心能力。备份不是目标，能恢复才是目标。生产环境必须同时考虑备份方式、备份频率、保留周期、加密存储、异地保存、恢复演练和恢复时间目标。

常见备份类型包括逻辑备份、物理备份、全量备份、增量备份和 binlog 备份。常见恢复需求包括整库恢复、指定库表恢复和指定时间点恢复。

### 逻辑备份

逻辑备份是导出 SQL 语句或逻辑数据，例如 `mysqldump` 导出的 SQL 文件。它可读性强、跨版本和跨平台能力较好，但大库备份和恢复速度较慢。

使用 `mysqldump` 做逻辑备份：

```bash
# 使用 mysqldump 备份 mall_order 数据库
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  mall_order \
  > mall_order_logic_backup.sql
```

压缩逻辑备份：

```bash
# 逻辑备份并压缩
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  mall_order \
  | gzip > mall_order_logic_backup.sql.gz
```

导出结构和数据分离：

```bash
# 只备份表结构
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --no-data \
  mall_order \
  > mall_order_schema.sql

# 只备份数据
mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --no-create-info \
  mall_order \
  > mall_order_data.sql
```

逻辑备份优点：

1. SQL 文本可读，便于审计和排查。
2. 可以按库、按表、按条件备份。
3. 跨机器、跨版本迁移相对方便。
4. 适合中小库、结构迁移、单表恢复。

逻辑备份缺点：

1. 大库备份和恢复速度慢。
2. 备份文件可能很大。
3. 恢复时需要重新执行 SQL 和重建索引。
4. 对数据库性能有一定影响。
5. 无法像物理备份那样快速恢复整个实例。

### 物理备份

物理备份是直接备份数据库的数据文件、日志文件和相关元数据。它通常比逻辑备份更适合大数据量实例，恢复速度更快。常见工具包括 Percona XtraBackup、MySQL Enterprise Backup、存储快照、云数据库快照等。

使用 XtraBackup 全量备份示例：

```bash
# 使用 xtrabackup 执行物理全量备份
xtrabackup \
  --backup \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  --target-dir=/data/backup/mysql/full_20260101
```

准备备份：

```bash
# 准备物理备份，应用 redo log，使备份达到一致状态
xtrabackup \
  --prepare \
  --target-dir=/data/backup/mysql/full_20260101
```

物理恢复前通常需要停止 MySQL，并清理或替换数据目录：

```bash
# 停止 MySQL 服务
sudo systemctl stop mysql

# 备份原数据目录，路径以实际环境为准
sudo mv /var/lib/mysql /var/lib/mysql_bak_$(date +%Y%m%d%H%M%S)

# 创建新的数据目录
sudo mkdir -p /var/lib/mysql
```

恢复物理备份：

```bash
# 将物理备份恢复到 MySQL 数据目录
xtrabackup \
  --copy-back \
  --target-dir=/data/backup/mysql/full_20260101

# 修正数据目录权限
sudo chown -R mysql:mysql /var/lib/mysql

# 启动 MySQL
sudo systemctl start mysql
```

物理备份优点：

1. 适合大库备份和快速恢复。
2. 恢复速度通常快于逻辑备份。
3. 可以结合增量备份。
4. 对整实例恢复更友好。

物理备份缺点：

1. 可读性差，不能直接查看 SQL。
2. 跨版本、跨平台限制更多。
3. 操作复杂度高于逻辑备份。
4. 恢复时通常影响整个实例。
5. 工具和版本兼容性需要重点确认。

### 全量备份

全量备份是备份某个时间点的完整数据。它是最基础的备份方式，也是增量备份和时间点恢复的基础。

每日全量逻辑备份示例：

```bash
# 生成带日期的全量逻辑备份文件
backup_date=$(date +%Y%m%d)

mysqldump \
  -h 127.0.0.1 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  --all-databases \
  | gzip > /data/backup/mysql/full_${backup_date}.sql.gz
```

全量物理备份示例：

```bash
# 生成带日期的全量物理备份目录
backup_date=$(date +%Y%m%d)

xtrabackup \
  --backup \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  --target-dir=/data/backup/mysql/full_${backup_date}
```

全量备份建议：

1. 全量备份是恢复基础，必须定期执行。
2. 小库可以使用每日逻辑全量备份。
3. 大库更适合物理全量备份。
4. 全量备份文件应压缩、加密并异地保存。
5. 全量备份完成后应记录备份时间、文件大小、校验值。
6. 只备份不验证等于没有可靠备份。

### 增量备份

增量备份只备份自上次备份以来发生变化的数据。它适合大数据量场景，可以减少备份时间和存储空间。逻辑备份通常不擅长真正增量，物理备份工具和 binlog 更常用于增量能力。

XtraBackup 增量备份示例：

```bash
# 基于全量备份执行第一次增量备份
xtrabackup \
  --backup \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  --target-dir=/data/backup/mysql/inc_20260102 \
  --incremental-basedir=/data/backup/mysql/full_20260101
```

基于上一次增量继续备份：

```bash
# 基于前一次增量备份继续执行增量备份
xtrabackup \
  --backup \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  --target-dir=/data/backup/mysql/inc_20260103 \
  --incremental-basedir=/data/backup/mysql/inc_20260102
```

准备全量备份和增量备份：

```bash
# 准备全量备份，但保留未提交事务回滚阶段，便于继续应用增量
xtrabackup \
  --prepare \
  --apply-log-only \
  --target-dir=/data/backup/mysql/full_20260101

# 应用第一次增量
xtrabackup \
  --prepare \
  --apply-log-only \
  --target-dir=/data/backup/mysql/full_20260101 \
  --incremental-dir=/data/backup/mysql/inc_20260102

# 应用最后一次增量，不再使用 apply-log-only
xtrabackup \
  --prepare \
  --target-dir=/data/backup/mysql/full_20260101 \
  --incremental-dir=/data/backup/mysql/inc_20260103
```

增量备份建议：

1. 增量备份依赖可靠的全量备份。
2. 增量链条越长，恢复越复杂。
3. 应定期重新做全量备份，缩短增量链。
4. 增量备份目录和顺序必须严格记录。
5. 恢复演练必须覆盖全量加多次增量场景。
6. 增量备份不等于 binlog 时间点恢复，两者可配合使用。

### Binlog 备份

Binlog 是 MySQL 二进制日志，记录数据库变更事件。它用于主从复制和时间点恢复。全量备份加 binlog 可以恢复到备份之后的指定时间点或指定位置。

查看 binlog 是否开启：

```sql
-- 查看 binlog 是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看 binlog 格式
SHOW VARIABLES LIKE 'binlog_format';

-- 查看当前 binlog 文件和位置
SHOW MASTER STATUS;
```

查看 binlog 文件列表：

```sql
-- 查看当前实例 binlog 文件
SHOW BINARY LOGS;
```

使用 `mysqlbinlog` 备份 binlog：

```bash
# 复制指定 binlog 文件到备份目录
mysqlbinlog \
  --read-from-remote-server \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  mysql-bin.000001 \
  > /data/backup/mysql/binlog/mysql-bin.000001.sql
```

持续拉取 binlog：

```bash
# 从远程 MySQL 持续拉取 binlog，适合专门备份服务器
mysqlbinlog \
  --read-from-remote-server \
  --raw \
  --stop-never \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password='Backup_123456' \
  mysql-bin.000001
```

刷新 binlog：

```sql
-- 切换生成新的 binlog 文件
FLUSH BINARY LOGS;
```

binlog 备份建议：

1. 开启 binlog 是时间点恢复的基础。
2. 全量备份时应记录当前 binlog 文件和位置。
3. binlog 应同步备份到异地，不能只保存在数据库服务器本机。
4. 删除 binlog 前必须确认备份和恢复需求。
5. 设置合理保留周期，避免磁盘被 binlog 占满。
6. binlog 中可能包含敏感数据，应控制访问权限。

### 数据恢复

数据恢复是从备份中还原数据。恢复前应明确恢复目标：恢复到新实例、覆盖原实例、恢复单库、恢复单表、恢复到指定时间点，还是只抽取部分数据。

逻辑备份恢复：

```bash
# 从 SQL 备份文件恢复数据库
mysql \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order \
  < mall_order_logic_backup.sql
```

恢复压缩逻辑备份：

```bash
# 解压并恢复 gzip 压缩备份
gunzip -c mall_order_logic_backup.sql.gz \
  | mysql \
      -h 127.0.0.1 \
      -P 3306 \
      -u root \
      -p \
      --default-character-set=utf8mb4 \
      mall_order
```

物理备份恢复的基本流程：

```text
1. 停止 MySQL
2. 备份或清空原数据目录
3. 准备物理备份
4. 拷贝备份文件到数据目录
5. 修正目录权限
6. 启动 MySQL
7. 检查错误日志和数据一致性
```

恢复后检查：

```sql
-- 检查数据库和表
SHOW DATABASES;

USE mall_order;

SHOW TABLES;

-- 检查核心表数据量
SELECT COUNT(*) AS order_count FROM order_info;

-- 检查最近数据
SELECT
    id,
    order_no,
    created_at
FROM order_info
ORDER BY id DESC
LIMIT 10;
```

数据恢复建议：

1. 不要直接在原生产库上试恢复，优先恢复到临时实例验证。
2. 恢复前保留当前现场，避免二次破坏。
3. 恢复后要校验数据量、关键业务数据和应用功能。
4. 恢复过程应记录备份文件、恢复时间、操作人和校验结果。
5. 重大恢复应有回滚方案。
6. 恢复演练应定期执行。

### 指定库表恢复

指定库表恢复常用于误删单表、误更新某张表、单业务库迁移等场景。逻辑备份恢复指定库表较方便；物理备份恢复单表通常复杂得多，常见做法是恢复到临时实例后再导出目标库表。

从全量逻辑备份中恢复指定表的常用思路：

```text
1. 将全量备份恢复到临时实例
2. 在临时实例中导出目标表
3. 在目标实例中导入目标表
4. 校验数据
```

从临时实例导出指定表：

```bash
# 从临时恢复实例导出指定表
mysqldump \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  mall_order order_info \
  > order_info_restore.sql
```

导入指定表到目标库：

```bash
# 将指定表恢复到目标实例
mysql \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order \
  < order_info_restore.sql
```

只恢复数据到临时表：

```bash
# 导出临时实例中的订单表数据
mysqldump \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  --no-create-info \
  mall_order order_info \
  > order_info_data.sql
```

恢复前创建临时恢复表：

```sql
-- 在目标库创建恢复临时表，避免直接覆盖正式表
CREATE TABLE order_info_restore LIKE order_info;
```

将恢复数据导入临时表后比对：

```sql
-- 比对正式表和恢复表数量
SELECT COUNT(*) AS formal_count FROM order_info;

SELECT COUNT(*) AS restore_count FROM order_info_restore;

-- 查找正式表缺失的数据
SELECT
    r.id,
    r.order_no,
    r.user_id,
    r.pay_amount,
    r.created_at
FROM order_info_restore AS r
LEFT JOIN order_info AS o ON r.id = o.id
WHERE o.id IS NULL
LIMIT 100;
```

指定库表恢复建议：

1. 优先恢复到临时实例，不要直接覆盖生产表。
2. 恢复表数据前先备份当前目标表。
3. 通过临时表比对后再合并数据。
4. 如果涉及关联表，要同时恢复相关表或校验一致性。
5. 单表恢复后应检查索引、触发器、自增值和业务约束。
6. 对误更新场景，应根据主键和时间范围精确修复。

### 指定时间点恢复

指定时间点恢复也叫 PITR，Point-In-Time Recovery。它通常依赖一次全量备份和从备份点之后的 binlog。目标是恢复到误操作发生前的某个时间点。

典型流程：

```text
1. 找到误操作发生时间，例如 2026-01-01 10:30:00
2. 找到误操作前最近一次全量备份
3. 将全量备份恢复到临时实例
4. 从备份时的 binlog 位置开始回放 binlog
5. 回放到误操作前一刻停止
6. 校验数据
7. 导出修复数据或切换实例
```

查看全量备份时记录的 binlog 位置：

```text
备份元信息示例：
backup_time=2026-01-01 02:00:00
binlog_file=mysql-bin.000120
binlog_position=45892341
```

使用 `mysqlbinlog` 按时间截取 binlog：

```bash
# 导出指定时间范围内的 binlog SQL
mysqlbinlog \
  --start-datetime="2026-01-01 02:00:00" \
  --stop-datetime="2026-01-01 10:29:59" \
  /data/backup/mysql/binlog/mysql-bin.000120 \
  /data/backup/mysql/binlog/mysql-bin.000121 \
  > recover_to_20260101_102959.sql
```

将 binlog 回放到临时实例：

```bash
# 将截取后的 binlog SQL 回放到临时实例
mysql \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  < recover_to_20260101_102959.sql
```

按位置恢复：

```bash
# 按 binlog 起止位置截取恢复 SQL
mysqlbinlog \
  --start-position=45892341 \
  --stop-position=68900123 \
  /data/backup/mysql/binlog/mysql-bin.000120 \
  > recover_by_position.sql
```

时间点恢复建议：

1. 必须有全量备份和连续 binlog。
2. 全量备份时必须记录 binlog 文件和位置。
3. 误操作发生后，应尽快保护 binlog，避免被清理。
4. 推荐恢复到临时实例后校验，不直接回放到生产。
5. 如果只需要恢复部分数据，可从临时实例导出目标表或目标记录。
6. 时间点恢复演练必须定期执行，否则真实故障时风险很高。

### 备份验证

备份验证用于确认备份文件可用、完整、可恢复。只生成备份文件但不验证，无法保证故障时能恢复。备份验证应包括文件存在、大小合理、校验值、解压测试、恢复测试和数据校验。

生成备份校验值：

```bash
# 生成备份文件 SHA-256 校验值
sha256sum /data/backup/mysql/full_20260101.sql.gz \
  > /data/backup/mysql/full_20260101.sql.gz.sha256
```

校验备份文件：

```bash
# 校验备份文件是否被篡改或损坏
sha256sum -c /data/backup/mysql/full_20260101.sql.gz.sha256
```

测试压缩文件完整性：

```bash
# 测试 gzip 文件是否可正常解压
gzip -t /data/backup/mysql/full_20260101.sql.gz
```

恢复到临时库验证：

```bash
# 将备份恢复到临时实例或临时库进行验证
gunzip -c /data/backup/mysql/full_20260101.sql.gz \
  | mysql \
      -h 127.0.0.1 \
      -P 3307 \
      -u root \
      -p \
      --default-character-set=utf8mb4
```

恢复后执行数据校验：

```sql
-- 检查核心库表是否存在
SELECT
    TABLE_SCHEMA,
    TABLE_NAME,
    TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA IN ('mall_order', 'mall_user')
ORDER BY TABLE_SCHEMA, TABLE_NAME;

-- 检查核心表数据量
SELECT COUNT(*) AS order_count FROM mall_order.order_info;

-- 检查最近订单时间
SELECT MAX(created_at) AS latest_order_time FROM mall_order.order_info;
```

备份验证建议：

1. 每次备份完成后检查文件大小和退出状态。
2. 为备份文件生成校验值。
3. 定期做恢复演练，而不是只检查文件存在。
4. 验证核心表数量、关键数据和最近数据时间。
5. 备份验证结果应记录日志和告警。
6. 异地备份也要验证可下载、可解密、可恢复。

### 备份策略设计

备份策略设计需要结合业务恢复目标。常见指标包括 RPO 和 RTO。

| 指标 | 含义                 | 示例                |
| ---- | -------------------- | ------------------- |
| RPO  | 可接受的数据丢失时间 | 最多丢失 5 分钟数据 |
| RTO  | 可接受的恢复耗时     | 30 分钟内恢复服务   |

常见备份策略示例：

| 业务级别     | 推荐策略                                  |
| ------------ | ----------------------------------------- |
| 低重要性系统 | 每日逻辑全量备份，保留 7 到 30 天         |
| 普通业务系统 | 每日全量备份 + binlog 备份，保留 30 天    |
| 核心交易系统 | 物理全量 + 增量 + 实时 binlog + 异地备份  |
| 大数据量实例 | 周期全量物理备份 + 每日增量 + 持续 binlog |
| 合规要求系统 | 加密备份 + 长周期归档 + 定期恢复演练      |

中小型业务推荐策略：

```text
1. 每天凌晨执行一次 mysqldump 全量备份
2. 开启 binlog，并保留至少 7 到 14 天
3. 备份文件压缩后上传到异地存储
4. 每周至少做一次自动恢复验证
5. 备份保留 30 天
6. 敏感备份文件加密保存
```

核心业务推荐策略：

```text
1. 每周一次物理全量备份
2. 每天一次物理增量备份
3. binlog 实时同步到备份服务器
4. 备份文件本地保留短周期，异地保留长周期
5. 每月至少一次完整恢复演练
6. 明确 RPO、RTO 和负责人
7. 备份、恢复、校验、告警全部自动化
```

备份目录规划示例：

```text
/data/backup/mysql/
  full/
    full_20260101.sql.gz
    full_20260102.sql.gz
  physical/
    full_20260101/
    inc_20260102/
    inc_20260103/
  binlog/
    mysql-bin.000120
    mysql-bin.000121
  checksum/
    full_20260101.sql.gz.sha256
  logs/
    backup_20260101.log
    restore_verify_20260101.log
```

备份脚本核心流程：

```text
1. 创建备份目录
2. 执行备份命令
3. 检查命令退出码
4. 压缩备份文件
5. 生成校验值
6. 上传异地存储
7. 清理过期备份
8. 记录备份日志
9. 触发失败告警
10. 定期执行恢复验证
```

备份策略设计建议：

1. 先明确 RPO 和 RTO，再设计备份方案。
2. 全量备份、增量备份和 binlog 备份要组合使用。
3. 备份文件必须异地保存，不能只放在数据库服务器本机。
4. 备份文件应加密，并限制访问权限。
5. 备份保留周期应满足业务和合规要求。
6. 必须定期恢复演练，验证备份真实可用。
7. 备份任务失败必须告警。
8. 备份策略应写入运维文档，并明确负责人。

## Binlog 日志

Binlog 是 MySQL 的二进制日志，用于记录数据库中的变更事件。它是主从复制、数据恢复、审计追踪和增量同步的重要基础。只要数据库发生了会修改数据或结构的操作，符合条件的事件通常都会被记录到 Binlog 中。

Binlog 属于 MySQL Server 层日志，不是 InnoDB 专属日志。它和 InnoDB 的 Redo Log 作用不同：Redo Log 主要用于崩溃恢复，Binlog 主要用于复制和基于时间点的数据恢复。

### Binlog 基础概念

Binlog 记录的是数据库变更事件，例如 `INSERT`、`UPDATE`、`DELETE`、`CREATE TABLE`、`ALTER TABLE` 等。它不记录普通 `SELECT` 查询。

Binlog 常见用途如下：

| 用途     | 说明                                         |
| -------- | -------------------------------------------- |
| 主从复制 | 主库将 Binlog 发送给从库，从库重放事件       |
| 数据恢复 | 通过全量备份 + Binlog 恢复到指定时间点       |
| 数据审计 | 分析历史变更事件                             |
| 数据同步 | Canal、Debezium 等组件基于 Binlog 做增量同步 |
| 灾备切换 | 结合复制位点或 GTID 追踪数据同步进度         |

查看是否开启 Binlog：

```sql
-- 查看 Binlog 是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看 Binlog 文件名前缀
SHOW VARIABLES LIKE 'log_bin_basename';

-- 查看当前 Binlog 格式
SHOW VARIABLES LIKE 'binlog_format';

-- 查看当前 Binlog 文件和位置
SHOW MASTER STATUS;
```

配置文件中开启 Binlog：

```ini
[mysqld]
# 开启 Binlog，并设置日志文件名前缀
log_bin=mysql-bin

# 设置服务器唯一ID，主从复制必须配置
server_id=1

# 推荐使用 ROW 格式，便于复制和恢复准确性
binlog_format=ROW

# Binlog 过期时间，单位秒，示例为 7 天
binlog_expire_logs_seconds=604800
```

配置说明：

- `log_bin` 用于开启 Binlog。
- `server_id` 是复制环境中的实例唯一标识。
- `binlog_format` 控制 Binlog 记录格式。
- `binlog_expire_logs_seconds` 控制 Binlog 自动过期清理周期。
- 配置修改后通常需要重启 MySQL 生效。

Binlog 使用建议：

1. 生产环境建议开启 Binlog。
2. 开启 Binlog 后应设置合理保留周期，避免磁盘被写满。
3. 全量备份时必须记录当前 Binlog 文件和位置。
4. 主从复制和时间点恢复强依赖 Binlog。
5. Binlog 可能包含敏感数据，备份和访问权限必须严格控制。

### Binlog 格式

MySQL 支持三种 Binlog 格式：`STATEMENT`、`ROW` 和 `MIXED`。不同格式记录的内容不同，对复制一致性、日志大小和恢复精度有直接影响。

查看当前格式：

```sql
-- 查看当前 Binlog 格式
SHOW VARIABLES LIKE 'binlog_format';
```

临时修改当前会话格式：

```sql
-- 只修改当前会话 Binlog 格式
SET SESSION binlog_format = 'ROW';
```

临时修改全局格式：

```sql
-- 修改全局 Binlog 格式，新连接生效
SET GLOBAL binlog_format = 'ROW';
```

三种格式对比：

| 格式        | 记录内容                       | 优点                   | 缺点                                |
| ----------- | ------------------------------ | ---------------------- | ----------------------------------- |
| `STATEMENT` | 记录原始 SQL                   | 日志体积较小，可读性好 | 某些非确定性 SQL 可能导致主从不一致 |
| `ROW`       | 记录每行数据变更               | 复制和恢复更准确       | 日志体积可能较大                    |
| `MIXED`     | 自动在 Statement 和 Row 间切换 | 兼顾体积和一致性       | 行为不如 ROW 直观                   |

生产建议：

1. 复制环境优先使用 `ROW`。
2. 对数据一致性要求高的系统优先使用 `ROW`。
3. `STATEMENT` 可读性较好，但一致性风险更高。
4. `MIXED` 可减少部分日志体积，但排查时不如固定 `ROW` 简单。
5. 选择格式前应结合恢复策略、复制拓扑和磁盘容量评估。

### ROW 模式

`ROW` 模式记录的是行级数据变更。对于每一行被修改的数据，Binlog 会记录该行变更前后或变更后的内容。它不依赖 SQL 在从库重新执行的结果，因此复制一致性更强。

配置 ROW 模式：

```ini
[mysqld]
# 使用 ROW 模式记录 Binlog
binlog_format=ROW

# FULL 记录完整行镜像，便于恢复和审计，但日志更大
binlog_row_image=FULL
```

查看行镜像配置：

```sql
-- 查看 ROW 模式下行镜像记录策略
SHOW VARIABLES LIKE 'binlog_row_image';
```

`binlog_row_image` 常见取值：

| 取值      | 说明                        |
| --------- | --------------------------- |
| `FULL`    | 记录完整行数据              |
| `MINIMAL` | 只记录必要字段              |
| `NOBLOB`  | 除 BLOB/TEXT 外记录完整字段 |

查看 ROW 模式 Binlog 内容时，建议使用 `mysqlbinlog -vv`：

```bash
# 查看 ROW 模式 Binlog 的详细行变更内容
mysqlbinlog \
  --base64-output=DECODE-ROWS \
  -vv \
  /var/lib/mysql/mysql-bin.000001 \
  | less
```

命令说明：

- `--base64-output=DECODE-ROWS` 用于解码行事件。
- `-vv` 显示更详细的行数据内容。
- `/var/lib/mysql/mysql-bin.000001` 替换为实际 Binlog 文件路径。
- `less` 用于分页查看输出内容。

ROW 模式优点：

1. 主从复制一致性更好。
2. 适合包含非确定性函数的 SQL。
3. 适合数据恢复和精确审计。
4. Canal、Debezium 等增量订阅组件通常依赖 ROW 模式。
5. 对复杂 SQL 的复制更安全。

ROW 模式注意事项：

1. 批量更新大量行时，Binlog 体积会明显增加。
2. `binlog_row_image=FULL` 更适合恢复，但日志更大。
3. 大事务会产生大 Binlog 事件，影响复制延迟。
4. 对敏感数据表，Binlog 文件也应按敏感数据管控。
5. 主从环境中推荐结合 GTID 使用，便于故障切换和追踪。

### STATEMENT 模式

`STATEMENT` 模式记录原始 SQL 语句。从库接收到 Binlog 后重新执行这些 SQL。该模式日志体积通常较小，可读性好，但某些 SQL 在主库和从库执行结果可能不一致。

配置 STATEMENT 模式：

```ini
[mysqld]
# 使用 STATEMENT 模式记录 Binlog
binlog_format=STATEMENT
```

查看 STATEMENT 模式 Binlog：

```bash
# 查看 Statement 模式 Binlog 内容
mysqlbinlog \
  /var/lib/mysql/mysql-bin.000001 \
  | less
```

容易产生不确定性的 SQL 示例：

```sql
-- 不推荐在 STATEMENT 模式下依赖不确定性结果
UPDATE user_account
SET balance_amount = balance_amount + 10
WHERE created_at < NOW()
ORDER BY RAND()
LIMIT 10;
```

可能存在风险的场景：

| 场景               | 风险                            |
| ------------------ | ------------------------------- |
| `UUID()`           | 主从生成值可能不同              |
| `RAND()`           | 主从随机结果可能不同            |
| `NOW()` 等时间函数 | 部分场景需要依赖 MySQL 特殊处理 |
| 无序 `LIMIT` 更新  | 主从可能更新不同记录            |
| 依赖环境变量       | 主从环境差异可能导致结果不同    |
| 存储函数副作用     | 结果可能不可控                  |

STATEMENT 模式使用建议：

1. 生产复制环境一般不推荐优先使用。
2. 适合对日志体积敏感且 SQL 确定性强的场景。
3. 执行 `UPDATE ... LIMIT` 时必须有稳定 `ORDER BY`。
4. 避免使用不确定性函数参与写入。
5. 数据一致性要求高时优先使用 `ROW`。

### MIXED 模式

`MIXED` 模式是 `STATEMENT` 和 `ROW` 的混合模式。MySQL 会根据 SQL 风险自动选择记录为语句事件或行事件。它试图兼顾日志体积和复制一致性。

配置 MIXED 模式：

```ini
[mysqld]
# 使用 MIXED 模式记录 Binlog
binlog_format=MIXED
```

查看当前格式：

```sql
-- 查看当前 Binlog 格式
SHOW VARIABLES LIKE 'binlog_format';
```

MIXED 模式适用理解：

```text
MIXED 模式下：
1. 普通确定性 SQL 可能按 STATEMENT 记录
2. 可能导致不一致的 SQL 会自动切换为 ROW 记录
3. 日志体积通常小于纯 ROW
4. 排查复制问题时，需要同时理解两种事件格式
```

MIXED 模式建议：

1. 比 `STATEMENT` 更安全，但不如固定 `ROW` 直观。
2. 对数据一致性敏感的系统仍建议使用 `ROW`。
3. 排查 Binlog 时要注意同一文件中可能混合两种事件。
4. 如果依赖 Binlog 做 CDC，同步组件通常更偏好 `ROW`。
5. 生产环境应统一规范，不要不同实例随意混用格式。

### Binlog 查看

查看 Binlog 可以使用 SQL 命令查看文件列表、当前位点，也可以使用 `mysqlbinlog` 工具解析具体事件内容。

查看当前 Binlog 文件和位置：

```sql
-- 查看当前正在写入的 Binlog 文件和位置
SHOW MASTER STATUS;
```

查看 Binlog 文件列表：

```sql
-- 查看当前实例保留的 Binlog 文件
SHOW BINARY LOGS;
```

查看 Binlog 事件：

```sql
-- 查看指定 Binlog 文件中的事件
SHOW BINLOG EVENTS IN 'mysql-bin.000001'
LIMIT 20;
```

从指定位置查看事件：

```sql
-- 从指定位置开始查看 Binlog 事件
SHOW BINLOG EVENTS IN 'mysql-bin.000001'
FROM 12345
LIMIT 50;
```

使用 `mysqlbinlog` 查看指定文件：

```bash
# 查看指定 Binlog 文件
mysqlbinlog \
  /var/lib/mysql/mysql-bin.000001 \
  | less
```

按时间范围查看：

```bash
# 按时间范围解析 Binlog
mysqlbinlog \
  --start-datetime="2026-01-01 10:00:00" \
  --stop-datetime="2026-01-01 11:00:00" \
  /var/lib/mysql/mysql-bin.000001 \
  > binlog_20260101_10_11.sql
```

按位置范围查看：

```bash
# 按起止位置解析 Binlog
mysqlbinlog \
  --start-position=12345 \
  --stop-position=67890 \
  /var/lib/mysql/mysql-bin.000001 \
  > binlog_pos_12345_67890.sql
```

远程查看 Binlog：

```bash
# 从远程 MySQL 服务器读取 Binlog
mysqlbinlog \
  --read-from-remote-server \
  --host=127.0.0.1 \
  --port=3306 \
  --user=backup_user \
  --password \
  mysql-bin.000001 \
  > mysql-bin.000001.sql
```

Binlog 查看建议：

1. 分析 ROW 模式时使用 `--base64-output=DECODE-ROWS -vv`。
2. 恢复前先解析到文件，确认内容后再回放。
3. 使用时间范围恢复时要确认服务器时区。
4. 使用位置恢复时要确认起止位点准确。
5. Binlog 文件可能很大，解析时注意磁盘空间。

### Binlog 清理

Binlog 会持续增长，必须设置合理清理策略。清理过早会影响主从复制和时间点恢复，清理过晚会占满磁盘。

查看自动过期时间：

```sql
-- 查看 Binlog 自动过期秒数
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';

-- 兼容部分旧版本配置
SHOW VARIABLES LIKE 'expire_logs_days';
```

配置自动清理：

```ini
[mysqld]
# Binlog 保留 7 天，单位秒
binlog_expire_logs_seconds=604800
```

手动清理指定文件之前的 Binlog：

```sql
-- 清理 mysql-bin.000120 之前的 Binlog，不包含 mysql-bin.000120
PURGE BINARY LOGS TO 'mysql-bin.000120';
```

按时间清理：

```sql
-- 清理指定时间之前的 Binlog
PURGE BINARY LOGS BEFORE '2026-01-01 00:00:00';
```

刷新生成新 Binlog：

```sql
-- 切换到新的 Binlog 文件
FLUSH BINARY LOGS;
```

清理前检查从库复制位点：

```sql
-- 从库查看复制状态，确认正在读取的主库 Binlog 文件
SHOW SLAVE STATUS\G

-- MySQL 8 新命令也可能使用
SHOW REPLICA STATUS\G
```

Binlog 清理建议：

1. 不要直接用 `rm` 删除 Binlog 文件。
2. 使用 `PURGE BINARY LOGS` 清理。
3. 清理前确认从库已经消费相关 Binlog。
4. 清理前确认备份和时间点恢复不再需要这些 Binlog。
5. 设置合理自动过期周期，避免磁盘被写满。
6. 核心系统建议将 Binlog 备份到独立存储后再清理本地文件。

### Binlog 恢复数据

Binlog 恢复数据通常用于误删、误更新、误执行 DDL 后的数据恢复。标准方式是先恢复最近一次全量备份，再回放全量备份之后到目标时间点的 Binlog。

恢复到指定时间点：

```bash
# 截取误操作前的 Binlog
mysqlbinlog \
  --start-datetime="2026-01-01 02:00:00" \
  --stop-datetime="2026-01-01 10:29:59" \
  /data/backup/mysql/binlog/mysql-bin.000120 \
  /data/backup/mysql/binlog/mysql-bin.000121 \
  > recover_before_mistake.sql
```

回放到临时实例：

```bash
# 将 Binlog 回放到临时恢复实例
mysql \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  < recover_before_mistake.sql
```

按位置恢复：

```bash
# 按位置截取 Binlog
mysqlbinlog \
  --start-position=45892341 \
  --stop-position=68900123 \
  /data/backup/mysql/binlog/mysql-bin.000120 \
  > recover_by_position.sql
```

跳过某个误操作的恢复思路：

```text
常见处理流程：
1. 恢复全量备份到临时实例
2. 解析 Binlog 找到误操作事件
3. 回放误操作之前的 Binlog
4. 跳过误操作事件
5. 继续回放误操作之后的安全事件
6. 校验数据
7. 从临时实例导出修复数据
```

恢复前解析 Binlog 检查内容：

```bash
# 先解析 Binlog 到文件，人工确认后再执行恢复
mysqlbinlog \
  --base64-output=DECODE-ROWS \
  -vv \
  --start-datetime="2026-01-01 10:00:00" \
  --stop-datetime="2026-01-01 10:40:00" \
  /data/backup/mysql/binlog/mysql-bin.000121 \
  > check_binlog_20260101_1000_1040.sql
```

Binlog 恢复建议：

1. 不要直接在生产库回放未检查的 Binlog。
2. 优先恢复到临时实例验证。
3. 时间点恢复要确认服务器时区。
4. 按位置恢复比按时间更精确。
5. 恢复前保护现场和现有 Binlog。
6. 恢复后校验数据量、关键记录和业务一致性。
7. 对误更新恢复，通常从临时实例导出受影响数据再修复生产。

### Binlog 与主从复制

主从复制依赖 Binlog。主库将变更写入 Binlog，从库通过复制线程拉取并重放这些事件，从而实现数据同步。

复制基本流程：

```text
1. 主库执行事务并写入 Binlog
2. 从库 IO 线程连接主库
3. 从库读取主库 Binlog
4. 从库写入本地 Relay Log
5. 从库 SQL 线程重放 Relay Log
6. 从库数据追上主库
```

主库 Binlog 关键配置：

```ini
[mysqld]
# 主库唯一 server_id
server_id=1

# 开启 Binlog
log_bin=mysql-bin

# 推荐 ROW 格式
binlog_format=ROW

# 保留 7 天 Binlog
binlog_expire_logs_seconds=604800
```

从库通常也建议开启 Binlog：

```ini
[mysqld]
# 从库唯一 server_id，不能与主库重复
server_id=2

# 从库也开启 Binlog，便于级联复制和主从切换
log_bin=mysql-bin

# 记录从库重放事件到自己的 Binlog，级联复制需要
log_replica_updates=ON

# 旧版本可能使用 log_slave_updates
# log_slave_updates=ON
```

复制账号授权：

```sql
-- 在主库创建复制账号
CREATE USER 'repl_user'@'10.20.30.%'
IDENTIFIED BY 'Repl_123456';

-- 授予复制权限
GRANT REPLICATION SLAVE, REPLICATION CLIENT
ON *.*
TO 'repl_user'@'10.20.30.%';
```

Binlog 与复制建议：

1. 主从复制必须开启主库 Binlog。
2. 主从 `server_id` 必须不同。
3. 推荐 `ROW` 格式复制。
4. 从库开启 Binlog 有利于故障切换和级联复制。
5. 清理主库 Binlog 前必须确认从库已经消费。
6. 复制账号只授予复制所需权限，不授予业务库读写权限。

## 主从复制

主从复制用于将主库数据变更同步到一个或多个从库。它常用于读写分离、灾备、备份卸载、报表查询、数据迁移和高可用架构。MySQL 8 中更推荐使用 GTID 复制和 `SOURCE/REPLICA` 新命令，但很多历史环境仍使用 `MASTER/SLAVE` 术语和命令。

### 主从复制原理

主从复制基于 Binlog 和 Relay Log。主库记录变更事件，从库读取主库 Binlog，写入本地 Relay Log，再由 SQL 线程重放。

复制核心组件：

| 组件           | 说明                                |
| -------------- | ----------------------------------- |
| 主库 Binlog    | 记录主库数据变更                    |
| 从库 IO 线程   | 从主库拉取 Binlog                   |
| 从库 Relay Log | 保存从主库拉取的日志                |
| 从库 SQL 线程  | 重放 Relay Log                      |
| 复制位点       | 记录从库同步到主库哪个文件和位置    |
| GTID           | 全局事务 ID，用于更方便追踪复制进度 |

复制流程：

```text
主库：
1. 执行事务
2. 写入 Redo Log
3. 写入 Binlog
4. 提交事务

从库：
1. IO 线程连接主库
2. 拉取主库 Binlog
3. 写入 Relay Log
4. SQL 线程重放 Relay Log
5. 更新复制位点
```

主从复制常见用途：

1. 读写分离，主库写，从库读。
2. 报表查询放到从库，降低主库压力。
3. 备份任务放到从库，减少主库影响。
4. 主库故障时，从库提升为新主库。
5. 数据迁移时先搭建复制，再切换业务流量。

### 异步复制

异步复制是 MySQL 默认复制方式。主库提交事务时，不等待从库确认是否接收或应用 Binlog。它性能较好，但主库宕机时可能存在少量数据尚未同步到从库。

异步复制特点：

| 特点           | 说明                           |
| -------------- | ------------------------------ |
| 性能较好       | 主库提交不等待从库             |
| 延迟可能存在   | 从库异步拉取和重放             |
| 有数据丢失风险 | 主库宕机时，未同步事件可能丢失 |
| 配置简单       | 默认复制方式                   |
| 应用广泛       | 常用于读写分离和普通灾备       |

异步复制配置流程：

```text
1. 主库开启 Binlog
2. 主库创建复制账号
3. 从库初始化主库数据
4. 从库配置主库连接信息
5. 启动复制
6. 查看复制状态
```

异步复制启动示例，基于文件和位置：

```sql
-- 在从库配置主库连接信息
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.10',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_LOG_FILE = 'mysql-bin.000120',
    SOURCE_LOG_POS = 45892341;

-- 启动复制
START REPLICA;
```

旧版本命令：

```sql
-- 旧版本命令写法
CHANGE MASTER TO
    MASTER_HOST = '10.20.30.10',
    MASTER_PORT = 3306,
    MASTER_USER = 'repl_user',
    MASTER_PASSWORD = 'Repl_123456',
    MASTER_LOG_FILE = 'mysql-bin.000120',
    MASTER_LOG_POS = 45892341;

START SLAVE;
```

异步复制建议：

1. 普通读写分离场景可以使用异步复制。
2. 核心交易系统应评估异步复制的数据丢失风险。
3. 从库读请求要考虑复制延迟。
4. 主库故障切换前应尽量确认从库追平。
5. 配合 GTID 可以简化切换和重建复制。

### 半同步复制

半同步复制要求主库提交事务时，至少等待一个从库确认已经接收到 Binlog 后，主库才向客户端返回提交成功。它降低了主库宕机时数据丢失风险，但会增加事务提交延迟。

半同步复制特点：

| 特点           | 说明                         |
| -------------- | ---------------------------- |
| 数据安全性更高 | 至少一个从库确认接收日志     |
| 性能低于异步   | 主库提交需要等待确认         |
| 不等于强同步   | 从库确认接收，不一定已经应用 |
| 网络敏感       | 从库或网络异常会影响提交延迟 |
| 可自动退化     | 超时后可能退化为异步复制     |

安装主库半同步插件：

```sql
-- 主库安装半同步主库插件
INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';

-- 启用主库半同步
SET GLOBAL rpl_semi_sync_master_enabled = ON;

-- 设置等待超时时间，单位毫秒
SET GLOBAL rpl_semi_sync_master_timeout = 1000;
```

安装从库半同步插件：

```sql
-- 从库安装半同步从库插件
INSTALL PLUGIN rpl_semi_sync_slave SONAME 'semisync_slave.so';

-- 启用从库半同步
SET GLOBAL rpl_semi_sync_slave_enabled = ON;

-- 重启复制线程使配置生效
STOP REPLICA;
START REPLICA;
```

MySQL 8 新版本插件和变量名称可能不同，应以实际版本为准。查看半同步变量：

```sql
-- 查看半同步相关变量
SHOW VARIABLES LIKE 'rpl_semi_sync%';

-- 查看半同步状态
SHOW STATUS LIKE 'Rpl_semi_sync%';
```

半同步复制建议：

1. 适合比异步复制更重视数据安全的场景。
2. 不等于零丢失，仍需结合高可用架构设计。
3. 应至少部署多个从库，避免单从库异常影响主库。
4. 超时时间要结合网络延迟和业务吞吐设置。
5. 上线前必须压测提交延迟和故障退化行为。

### GTID 复制

GTID 是 Global Transaction Identifier，全局事务标识。开启 GTID 后，每个事务都有全局唯一 ID，复制时不再强依赖手工指定 Binlog 文件和位置，主从切换、故障恢复和复制重建更方便。

GTID 格式通常类似：

```text
server_uuid:transaction_id
```

开启 GTID 配置：

```ini
[mysqld]
# 每个实例 server_id 必须唯一
server_id=1

# 开启 Binlog
log_bin=mysql-bin

# 开启 GTID
gtid_mode=ON

# 强制事务写入 GTID 兼容日志
enforce_gtid_consistency=ON

# 推荐 ROW 模式
binlog_format=ROW
```

查看 GTID 状态：

```sql
-- 查看 GTID 配置
SHOW VARIABLES LIKE 'gtid_mode';
SHOW VARIABLES LIKE 'enforce_gtid_consistency';

-- 查看当前实例已经执行的 GTID 集合
SHOW MASTER STATUS;
```

基于 GTID 配置复制：

```sql
-- 从库使用 GTID 自动定位主库复制位置
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.10',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

START REPLICA;
```

旧版本写法：

```sql
-- 旧版本 GTID 复制写法
CHANGE MASTER TO
    MASTER_HOST = '10.20.30.10',
    MASTER_PORT = 3306,
    MASTER_USER = 'repl_user',
    MASTER_PASSWORD = 'Repl_123456',
    MASTER_AUTO_POSITION = 1;

START SLAVE;
```

GTID 复制建议：

1. 新建复制环境建议优先使用 GTID。
2. GTID 能简化主从切换和复制重建。
3. 所有复制节点应统一开启 GTID。
4. 开启前应确认现有 SQL 和工具兼容 GTID 一致性要求。
5. 不建议在同一复制拓扑中混乱使用 GTID 和传统位点方式。

### 主库配置

主库必须开启 Binlog，并配置唯一 `server_id`。如果使用 GTID，还需要开启 GTID 相关参数。生产环境还应配置 Binlog 保留策略、ROW 格式和复制账号。

主库配置示例：

```ini
[mysqld]
# 主库唯一ID，复制拓扑中不能重复
server_id=1

# 开启 Binlog
log_bin=mysql-bin

# 推荐 ROW 格式
binlog_format=ROW

# Binlog 保留 7 天
binlog_expire_logs_seconds=604800

# 开启 GTID
gtid_mode=ON

# 强制 GTID 一致性
enforce_gtid_consistency=ON

# 从库级联复制或切换需要时建议开启
log_replica_updates=ON
```

重启 MySQL 后检查配置：

```sql
-- 检查主库关键复制配置
SHOW VARIABLES LIKE 'server_id';
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_format';
SHOW VARIABLES LIKE 'gtid_mode';
SHOW VARIABLES LIKE 'enforce_gtid_consistency';

-- 查看当前 Binlog 文件和位置
SHOW MASTER STATUS;
```

创建复制账号：

```sql
-- 创建复制账号，只允许从库网段连接
CREATE USER 'repl_user'@'10.20.30.%'
IDENTIFIED BY 'Repl_123456';

-- 授予复制权限
GRANT REPLICATION SLAVE, REPLICATION CLIENT
ON *.*
TO 'repl_user'@'10.20.30.%';
```

主库导出一致性数据给从库初始化：

```bash
# 在主库导出全量数据，包含复制位点信息
mysqldump \
  -h 10.20.30.10 \
  -P 3306 \
  -u backup_user \
  -p \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=ON \
  --all-databases \
  | gzip > mysql_full_for_replica.sql.gz
```

主库配置建议：

1. `server_id` 必须唯一。
2. 必须开启 Binlog。
3. 推荐 `binlog_format=ROW`。
4. 复制账号只授予复制权限。
5. Binlog 保留周期应大于从库可能延迟时间和恢复需要。
6. 初始化从库时应使用一致性备份。

### 从库配置

从库需要配置唯一 `server_id`，导入主库基础数据，然后配置复制源信息并启动复制。从库可以开启只读模式，避免业务误写。

从库配置示例：

```ini
[mysqld]
# 从库唯一ID，不能与主库重复
server_id=2

# 建议从库也开启 Binlog，便于级联复制和切换
log_bin=mysql-bin

# 开启 GTID
gtid_mode=ON

# 强制 GTID 一致性
enforce_gtid_consistency=ON

# 从库重放事件也写入自己的 Binlog
log_replica_updates=ON

# 开启只读，防止普通账号误写
read_only=ON

# 超级只读，进一步防止高权限账号误写
super_read_only=ON
```

从库导入主库备份：

```bash
# 在从库导入主库全量备份
gunzip -c mysql_full_for_replica.sql.gz \
  | mysql \
      -h 127.0.0.1 \
      -P 3306 \
      -u root \
      -p \
      --default-character-set=utf8mb4
```

配置 GTID 复制：

```sql
-- 配置从库连接主库并启用 GTID 自动定位
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.10',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

-- 启动复制
START REPLICA;
```

查看从库只读配置：

```sql
-- 查看只读配置
SHOW VARIABLES LIKE 'read_only';
SHOW VARIABLES LIKE 'super_read_only';
```

从库配置建议：

1. 从库 `server_id` 必须与主库不同。
2. 生产从库建议开启 `read_only` 和 `super_read_only`。
3. 从库也建议开启 Binlog，便于故障切换后作为新主库。
4. 从库初始化数据必须与主库某一时点一致。
5. 配置复制后必须检查复制状态和延迟。

### 复制状态查看

复制状态用于判断从库是否正常连接主库、是否正常重放日志、是否存在延迟、错误或中断。

查看复制状态：

```sql
-- MySQL 8 推荐命令
SHOW REPLICA STATUS\G
```

旧版本命令：

```sql
-- 旧版本命令
SHOW SLAVE STATUS\G
```

关键字段说明：

| 字段                    | 说明                       |
| ----------------------- | -------------------------- |
| `Replica_IO_Running`    | IO 线程是否运行            |
| `Replica_SQL_Running`   | SQL 线程是否运行           |
| `Source_Host`           | 主库地址                   |
| `Source_Log_File`       | 当前读取的主库 Binlog 文件 |
| `Read_Source_Log_Pos`   | 当前读取的主库 Binlog 位置 |
| `Relay_Log_File`        | 当前 Relay Log 文件        |
| `Relay_Log_Pos`         | 当前 Relay Log 位置        |
| `Exec_Source_Log_Pos`   | 已执行到的主库 Binlog 位置 |
| `Seconds_Behind_Source` | 复制延迟秒数估算           |
| `Last_IO_Error`         | IO 线程最近错误            |
| `Last_SQL_Error`        | SQL 线程最近错误           |
| `Retrieved_Gtid_Set`    | 已获取 GTID 集合           |
| `Executed_Gtid_Set`     | 已执行 GTID 集合           |

旧字段名称中可能出现 `Master` 和 `Slave`：

| 旧字段                  | 新字段                  |
| ----------------------- | ----------------------- |
| `Slave_IO_Running`      | `Replica_IO_Running`    |
| `Slave_SQL_Running`     | `Replica_SQL_Running`   |
| `Master_Host`           | `Source_Host`           |
| `Master_Log_File`       | `Source_Log_File`       |
| `Seconds_Behind_Master` | `Seconds_Behind_Source` |

通过 performance_schema 查看复制线程：

```sql
-- 查看复制连接状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    SOURCE_UUID,
    THREAD_ID
FROM performance_schema.replication_connection_status;

-- 查看复制应用状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    REMAINING_DELAY,
    COUNT_TRANSACTIONS_RETRIES
FROM performance_schema.replication_applier_status;
```

复制状态判断建议：

1. IO 和 SQL 线程都应为运行状态。
2. `Last_IO_Error` 和 `Last_SQL_Error` 应为空。
3. `Seconds_Behind_Source` 不应长期增长。
4. GTID 模式下重点关注已获取和已执行 GTID 集合差距。
5. 监控系统应采集复制线程状态、延迟和错误信息。

### 复制延迟分析

复制延迟是指从库应用主库变更落后于主库。延迟可能由主库大事务、从库 SQL 线程重放慢、网络问题、从库机器性能不足、锁等待、长查询占用资源等原因导致。

查看延迟：

```sql
-- 查看复制延迟
SHOW REPLICA STATUS\G
```

重点字段：

```text
Replica_IO_Running: Yes
Replica_SQL_Running: Yes
Seconds_Behind_Source: 120
Last_SQL_Error:
```

查看从库正在执行的 SQL：

```sql
-- 查看从库当前线程和 SQL 执行情况
SHOW PROCESSLIST;
```

查看长事务：

```sql
-- 查看当前长事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

可能导致延迟的原因：

| 原因           | 说明                     | 处理方向             |
| -------------- | ------------------------ | -------------------- |
| 主库大事务     | 从库需要重放大量变更     | 拆分大事务           |
| 批量更新删除   | Binlog 和 Relay Log 很大 | 分批处理             |
| 从库性能不足   | CPU、IO、内存瓶颈        | 升级资源或减少读负载 |
| 从库长查询     | 抢占资源，影响重放       | 报表查询隔离         |
| 锁等待         | SQL 线程被阻塞           | 查锁并优化事务       |
| 网络延迟       | IO 线程拉取慢            | 检查网络链路         |
| 单线程重放瓶颈 | SQL 线程应用慢           | 开启并行复制         |

开启并行复制示例：

```ini
[mysqld]
# 并行复制工作线程数，按从库资源和业务测试调整
replica_parallel_workers=4

# 按逻辑时钟并行复制，适合 MySQL 8
replica_parallel_type=LOGICAL_CLOCK

# 保证事务提交顺序
replica_preserve_commit_order=ON
```

旧版本参数可能是：

```ini
[mysqld]
# 旧版本参数名称
slave_parallel_workers=4
slave_parallel_type=LOGICAL_CLOCK
slave_preserve_commit_order=ON
```

复制延迟优化建议：

1. 主库避免大事务，批量变更要分批提交。
2. 从库避免执行高成本报表查询影响复制线程。
3. 为从库配置足够 CPU、IO 和内存。
4. 开启并行复制前应测试一致性和收益。
5. 对读写分离应用，应识别延迟风险，避免读到旧数据。
6. 核心读请求需要强一致时，应读主库或使用延迟判断策略。

### 主从切换

主从切换是指在主库故障或维护时，将从库提升为新主库，并让其他从库改为复制新主库。切换分为计划内切换和故障切换。

计划内切换流程：

```text
1. 停止应用写入或进入只读维护
2. 等待从库追平主库
3. 停止原主库写入
4. 确认从库无延迟
5. 提升从库为新主库
6. 应用切换连接到新主库
7. 其他从库改为复制新主库
8. 验证业务读写
```

从库追平检查：

```sql
-- 在从库查看复制状态，确认延迟为 0
SHOW REPLICA STATUS\G
```

提升从库为新主库：

```sql
-- 停止复制
STOP REPLICA;

-- 清理复制配置，谨慎执行
RESET REPLICA ALL;

-- 关闭只读，允许写入
SET GLOBAL super_read_only = OFF;
SET GLOBAL read_only = OFF;
```

旧版本写法：

```sql
-- 旧版本命令
STOP SLAVE;

RESET SLAVE ALL;

SET GLOBAL super_read_only = OFF;
SET GLOBAL read_only = OFF;
```

其他从库切到新主库，GTID 方式：

```sql
-- 在其他从库上重新指向新主库
STOP REPLICA;

CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.20',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

START REPLICA;
```

切换后检查：

```sql
-- 新主库检查只读状态
SHOW VARIABLES LIKE 'read_only';
SHOW VARIABLES LIKE 'super_read_only';

-- 从库检查复制状态
SHOW REPLICA STATUS\G

-- 检查业务数据写入
SELECT NOW() AS check_time;
```

主从切换建议：

1. 计划内切换应先停止写入并等待从库追平。
2. 故障切换前应选择数据最完整的从库。
3. 使用 GTID 能显著简化切换。
4. 切换过程应由高可用组件或标准脚本执行，避免手工误操作。
5. 切换后要检查应用连接、复制状态、只读配置和数据一致性。
6. 原主库恢复后不能直接加入拓扑，应先确认数据是否需要重建。

### 复制异常处理

复制异常常见于网络中断、账号权限错误、Binlog 被清理、主从数据不一致、重复键、缺表、DDL 不一致、大事务卡住等。处理复制异常时，应先保护现场，查看错误，再决定修复方式。

查看异常：

```sql
-- 查看复制异常详情
SHOW REPLICA STATUS\G
```

重点查看：

```text
Replica_IO_Running
Replica_SQL_Running
Last_IO_Error
Last_SQL_Error
Last_Errno
Last_SQL_Errno
Source_Log_File
Read_Source_Log_Pos
Relay_Log_File
Relay_Log_Pos
Executed_Gtid_Set
```

常见异常和处理方向：

| 异常                 | 可能原因                       | 处理方向                 |
| -------------------- | ------------------------------ | ------------------------ |
| IO 线程失败          | 网络不通、账号错误、主库不可达 | 检查网络、账号、主库状态 |
| SQL 线程失败         | 重放事件报错                   | 查看 `Last_SQL_Error`    |
| Binlog 不存在        | 主库 Binlog 被清理             | 重新初始化从库           |
| Duplicate entry      | 从库已有冲突数据               | 校验数据一致性后修复     |
| Table does not exist | 从库缺表或 DDL 不一致          | 修复结构或重建从库       |
| 延迟持续增长         | 从库重放慢或锁等待             | 分析延迟原因             |
| GTID 冲突            | 拓扑操作不规范                 | 分析 GTID 集合并谨慎修复 |

复制账号错误处理：

```sql
-- 在主库确认复制账号
SHOW GRANTS FOR 'repl_user'@'10.20.30.%';

-- 必要时重置密码
ALTER USER 'repl_user'@'10.20.30.%'
IDENTIFIED BY 'Repl_New_123456';
```

从库重新配置复制账号：

```sql
-- 在从库更新复制账号密码
STOP REPLICA;

CHANGE REPLICATION SOURCE TO
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_New_123456';

START REPLICA;
```

跳过一个事务，GTID 场景需非常谨慎。只有确认该事务可以跳过，并且已经评估数据一致性影响后才可操作：

```sql
-- 示例：跳过指定 GTID 事务，执行前必须确认影响
STOP REPLICA;

SET GTID_NEXT = '主库UUID:事务ID';
BEGIN;
COMMIT;
SET GTID_NEXT = 'AUTOMATIC';

START REPLICA;
```

传统复制跳过一个事件也必须谨慎：

```sql
-- 旧方式跳过一个复制事件，可能造成数据不一致，谨慎使用
STOP REPLICA;

SET GLOBAL sql_slave_skip_counter = 1;

START REPLICA;
```

复制异常处理建议：

1. 不要看到复制报错就直接跳过。
2. 先分析 `Last_IO_Error` 和 `Last_SQL_Error`。
3. 复制报错通常意味着数据或结构已经不一致，需要确认根因。
4. 对数据不一致严重的从库，优先考虑重新初始化。
5. 主库 Binlog 被清理导致从库断点丢失时，通常需要重建从库。
6. 所有修复动作应记录原因、时间、位点、GTID 和处理结果。
7. 核心系统建议使用 Orchestrator、MHA、MGR 或云数据库高可用能力管理复制拓扑。

## 高可用与集群

MySQL 高可用的目标是减少数据库故障对业务的影响，常见能力包括数据冗余、读写分离、故障检测、自动或手动切换、连接路由和数据一致性控制。不同方案在一致性、复杂度、切换速度、运维成本和性能上差异较大。

高可用不是只搭建多个 MySQL 实例，还需要配套复制监控、延迟监控、故障切换流程、备份恢复、权限隔离、应用连接治理和演练机制。

### 主从架构

主从架构是最基础的 MySQL 高可用架构。主库负责写入，从库通过复制同步主库数据。从库可以用于只读查询、备份、报表和故障恢复。

典型结构如下：

```text
应用服务
   |
   v
MySQL 主库
   |
   | Binlog 复制
   v
MySQL 从库
```

主库配置示例：

```ini
[mysqld]
# 主库唯一 server_id
server_id=1

# 开启 Binlog
log_bin=mysql-bin

# 推荐 ROW 模式，复制一致性更好
binlog_format=ROW

# 开启 GTID，便于主从切换和复制重建
gtid_mode=ON
enforce_gtid_consistency=ON

# Binlog 保留 7 天
binlog_expire_logs_seconds=604800
```

从库配置示例：

```ini
[mysqld]
# 从库唯一 server_id，不能与主库重复
server_id=2

# 从库也开启 Binlog，方便后续提升为主库
log_bin=mysql-bin

# 开启 GTID
gtid_mode=ON
enforce_gtid_consistency=ON

# 从库重放的事件也写入自己的 Binlog，便于级联复制和切换
log_replica_updates=ON

# 防止普通账号误写从库
read_only=ON
super_read_only=ON
```

从库配置复制源：

```sql
-- 在从库执行，配置主库连接信息
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.10',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

-- 启动复制
START REPLICA;
```

查看复制状态：

```sql
-- 查看从库复制状态
SHOW REPLICA STATUS\G
```

主从架构优点：

| 优点         | 说明                 |
| ------------ | -------------------- |
| 架构简单     | 容易搭建和理解       |
| 成本较低     | 只需要主库和从库     |
| 支持读扩展   | 从库可以承担部分查询 |
| 支持备份卸载 | 备份任务可放在从库   |
| 支持故障恢复 | 主库故障时可提升从库 |

主从架构风险：

| 风险               | 说明                               |
| ------------------ | ---------------------------------- |
| 异步复制可能丢数据 | 主库宕机时，未同步 Binlog 可能丢失 |
| 存在复制延迟       | 从库可能读到旧数据                 |
| 切换需要流程       | 手动切换容易出错                   |
| 单从库能力有限     | 备份、报表和读流量可能互相影响     |
| 从库误写风险       | 需要开启只读保护                   |

主从架构建议：

1. 新建主从建议使用 GTID。
2. 主库和从库 `server_id` 必须不同。
3. 从库建议开启 `read_only` 和 `super_read_only`。
4. 应用不要直接写从库。
5. 主从延迟必须接入监控。
6. 主库故障后，从库提升前必须确认数据同步进度。

### 一主多从架构

一主多从是在一个主库后挂多个从库。它适合读流量较大、需要报表隔离、备份隔离或多地域只读访问的场景。

典型结构如下：

```text
                 +--> MySQL 从库 1：在线读查询
                 |
MySQL 主库 ------+--> MySQL 从库 2：报表查询
                 |
                 +--> MySQL 从库 3：备份任务
```

一主多从的复制账号：

```sql
-- 在主库创建复制账号，允许多个从库所在网段连接
CREATE USER 'repl_user'@'10.20.30.%'
IDENTIFIED BY 'Repl_123456';

GRANT REPLICATION SLAVE, REPLICATION CLIENT
ON *.*
TO 'repl_user'@'10.20.30.%';
```

每个从库使用不同 `server_id`：

```ini
[mysqld]
# 从库1
server_id=2
read_only=ON
super_read_only=ON
[mysqld]
# 从库2
server_id=3
read_only=ON
super_read_only=ON
[mysqld]
# 从库3
server_id=4
read_only=ON
super_read_only=ON
```

多从库角色划分示例：

| 从库   | 角色         | 说明                   |
| ------ | ------------ | ---------------------- |
| 从库 1 | 在线查询从库 | 承担应用读流量         |
| 从库 2 | 报表从库     | 承担慢报表和运营查询   |
| 从库 3 | 备份从库     | 执行全量备份和恢复验证 |
| 从库 4 | 灾备从库     | 跨机房或跨可用区部署   |

查看所有从库复制状态：

```sql
-- 每台从库分别执行，检查复制线程和延迟
SHOW REPLICA STATUS\G
```

一主多从优点：

1. 可以横向扩展读能力。
2. 可以将报表查询和在线查询隔离。
3. 可以将备份任务从主库卸载。
4. 可以提高故障恢复选择空间。
5. 多个从库可以承担不同业务角色。

一主多从风险：

1. 主库仍然是单写点。
2. 多个从库都可能存在复制延迟。
3. 从库越多，主库 Binlog 发送和网络压力越大。
4. 不同从库数据进度可能不一致。
5. 故障切换时需要选择最合适的新主库。

一主多从使用建议：

1. 从库按角色隔离，不要所有任务混在同一台从库。
2. 报表从库允许延迟更高，在线读从库要求延迟更低。
3. 备份从库不应承载高峰在线读流量。
4. 高可用切换优先选择数据最新、状态健康的从库。
5. 读写分离中必须考虑主从延迟和读一致性。

### 双主架构

双主架构通常指两个 MySQL 实例互为主从。每个实例都开启 Binlog，并互相复制对方的数据。双主可以用于故障切换，也可以用于特定场景下的双向写入，但双向写入风险较高。

典型结构如下：

```text
MySQL A  <------复制------>  MySQL B
```

双主配置示例，节点 A：

```ini
[mysqld]
# 节点A
server_id=1
log_bin=mysql-bin
binlog_format=ROW
gtid_mode=ON
enforce_gtid_consistency=ON
log_replica_updates=ON

# 避免自增主键冲突，A 使用奇数
auto_increment_increment=2
auto_increment_offset=1
```

节点 B：

```ini
[mysqld]
# 节点B
server_id=2
log_bin=mysql-bin
binlog_format=ROW
gtid_mode=ON
enforce_gtid_consistency=ON
log_replica_updates=ON

# 避免自增主键冲突，B 使用偶数
auto_increment_increment=2
auto_increment_offset=2
```

节点 A 指向节点 B：

```sql
-- 在节点A执行，复制节点B
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.12',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

START REPLICA;
```

节点 B 指向节点 A：

```sql
-- 在节点B执行，复制节点A
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.11',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

START REPLICA;
```

双主架构常见模式：

| 模式       | 说明                                         |
| ---------- | -------------------------------------------- |
| 双主单写   | 两个节点互相复制，但同一时间只有一个节点写入 |
| 双主双写   | 两个节点同时接受写入                         |
| 主备切换   | 正常只写 A，A 故障后切到 B                   |
| 跨机房容灾 | 两地互备，但通常仍控制单写                   |

双主双写风险：

1. 主键冲突。
2. 唯一键冲突。
3. 同一行并发修改冲突。
4. 自增 ID 需要额外规划。
5. 数据冲突发现和修复困难。
6. 业务一致性难以保证。

双主架构建议：

1. 生产环境更推荐“双主单写”，不推荐随意双写。
2. 双写前必须确认业务天然分片或冲突可控。
3. 使用自增主键时必须配置不同步长和偏移量。
4. 应用层必须避免同一数据在两个节点同时写入。
5. 双主切换后，要防止旧主恢复后继续接受写入造成脑裂。
6. 如果需要真正多主能力，应评估 MGR、InnoDB Cluster 或分布式数据库方案。

### MGR 组复制

MGR 是 MySQL Group Replication，组复制。它是 MySQL 原生的高可用复制方案，基于组成员通信和事务认证机制实现数据复制。MGR 支持单主模式和多主模式，生产环境更常用单主模式。

MGR 典型结构如下：

```text
        +-------------------+
        |   MySQL Group     |
        |                   |
        |  Node1  Primary   |
        |  Node2  Secondary |
        |  Node3  Secondary |
        +-------------------+
```

MGR 核心特点：

| 特性       | 说明                         |
| ---------- | ---------------------------- |
| 组成员管理 | 自动感知节点加入、退出、故障 |
| 事务认证   | 提交前检测冲突               |
| 单主模式   | 只有 Primary 节点接受写入    |
| 多主模式   | 多节点可写，但冲突风险更高   |
| 自动选主   | Primary 故障后可重新选主     |
| 强一致倾向 | 比传统异步复制一致性更强     |

MGR 基础配置示例：

```ini
[mysqld]
# 每个节点唯一
server_id=1

# 开启 Binlog
log_bin=mysql-bin
binlog_format=ROW
log_replica_updates=ON

# 开启 GTID
gtid_mode=ON
enforce_gtid_consistency=ON

# MGR 要求表有主键，建议开启
sql_require_primary_key=ON

# 组复制插件相关配置
plugin_load_add='group_replication.so'

# 当前节点唯一地址
group_replication_local_address='10.20.30.11:33061'

# 组内所有成员地址
group_replication_group_seeds='10.20.30.11:33061,10.20.30.12:33061,10.20.30.13:33061'

# 组名，必须是 UUID 格式，各节点一致
group_replication_group_name='aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'

# 单主模式
group_replication_single_primary_mode=ON

# 禁用多主冲突检查模式
group_replication_enforce_update_everywhere_checks=OFF
```

启动第一个节点创建组：

```sql
-- 第一个节点引导组，只能在初始化组时开启
SET GLOBAL group_replication_bootstrap_group = ON;

START GROUP_REPLICATION;

SET GLOBAL group_replication_bootstrap_group = OFF;
```

其他节点加入组：

```sql
-- 其他节点直接加入组
START GROUP_REPLICATION;
```

查看组成员状态：

```sql
-- 查看 MGR 组成员
SELECT
    MEMBER_ID,
    MEMBER_HOST,
    MEMBER_PORT,
    MEMBER_STATE,
    MEMBER_ROLE,
    MEMBER_VERSION
FROM performance_schema.replication_group_members;
```

查看当前主节点：

```sql
-- 查看当前 Primary 节点
SELECT
    MEMBER_HOST,
    MEMBER_PORT,
    MEMBER_ROLE
FROM performance_schema.replication_group_members
WHERE MEMBER_ROLE = 'PRIMARY';
```

MGR 使用建议：

1. 生产环境优先使用单主模式。
2. 所有业务表建议有主键。
3. 节点数量通常建议为奇数，例如 3 节点。
4. 多主模式冲突处理复杂，慎用。
5. 网络延迟会影响事务提交性能。
6. MGR 部署和故障处理复杂度高于普通主从。
7. 需要结合 MySQL Router、ProxySQL 或应用配置完成连接路由。

### InnoDB Cluster

InnoDB Cluster 是 MySQL 官方高可用方案，由 MySQL Group Replication、MySQL Shell 和 MySQL Router 组成。它在 MGR 基础上提供更完整的集群管理和路由能力。

InnoDB Cluster 组成：

| 组件                    | 作用                       |
| ----------------------- | -------------------------- |
| MySQL Group Replication | 数据复制和组成员管理       |
| MySQL Shell             | 集群创建、管理、状态检查   |
| MySQL Router            | 应用连接路由、读写分离入口 |
| MySQL Server            | 数据库实例                 |

典型结构如下：

```text
应用服务
   |
   v
MySQL Router
   |
   +--> Primary 节点：写入
   |
   +--> Secondary 节点：只读
   |
   +--> Secondary 节点：只读
```

使用 MySQL Shell 检查实例配置：

```javascript
// 使用 MySQL Shell 连接实例
shell.connect('root@10.20.30.11:3306')

// 检查实例是否满足 InnoDB Cluster 要求
dba.checkInstanceConfiguration('root@10.20.30.11:3306')
```

配置实例：

```javascript
// 自动配置实例参数，执行前应确认影响
dba.configureInstance('root@10.20.30.11:3306')
dba.configureInstance('root@10.20.30.12:3306')
dba.configureInstance('root@10.20.30.13:3306')
```

创建集群：

```javascript
// 创建 InnoDB Cluster
var cluster = dba.createCluster('mallCluster')
```

添加实例：

```javascript
// 添加其他实例到集群
cluster.addInstance('root@10.20.30.12:3306')
cluster.addInstance('root@10.20.30.13:3306')
```

查看集群状态：

```javascript
// 查看集群状态
cluster.status()
```

MySQL Router 初始化：

```bash
# 初始化 MySQL Router，连接到集群
mysqlrouter \
  --bootstrap root@10.20.30.11:3306 \
  --directory /data/mysqlrouter \
  --user=mysqlrouter
```

启动 Router：

```bash
# 启动 MySQL Router
/data/mysqlrouter/start.sh
```

InnoDB Cluster 优点：

1. 官方集成方案，管理能力强于裸 MGR。
2. 支持自动发现 Primary。
3. MySQL Router 可为应用提供稳定入口。
4. MySQL Shell 提供集群管理接口。
5. 适合需要原生 MySQL 高可用方案的场景。

InnoDB Cluster 建议：

1. 适合希望使用官方高可用套件的团队。
2. 部署前应熟悉 MySQL Shell 和 Router。
3. 应使用 3 个或更多节点，避免单点和仲裁问题。
4. 应定期演练 Primary 故障和节点恢复。
5. 应明确 Router 高可用部署方式，避免 Router 自身成为单点。

### ProxySQL

ProxySQL 是常用 MySQL 代理中间件，可以提供连接池、读写分离、查询路由、故障切换配合、SQL 规则、后端健康检查等能力。它常部署在应用和 MySQL 集群之间。

典型结构如下：

```text
应用服务
   |
   v
ProxySQL
   |
   +--> MySQL 主库：写入
   |
   +--> MySQL 从库1：读取
   |
   +--> MySQL 从库2：读取
```

ProxySQL 核心概念：

| 概念          | 说明                |
| ------------- | ------------------- |
| hostgroup     | 后端 MySQL 分组     |
| mysql_servers | 后端 MySQL 实例列表 |
| mysql_users   | 代理层用户          |
| query rules   | SQL 路由规则        |
| monitor       | 后端健康检查        |
| runtime       | 当前运行配置        |
| disk          | 持久化配置          |

登录 ProxySQL 管理端口：

```bash
# 登录 ProxySQL 管理端口，默认 6032
mysql \
  -h 127.0.0.1 \
  -P 6032 \
  -u admin \
  -padmin
```

配置后端 MySQL 节点：

```sql
-- 写库 hostgroup 10
INSERT INTO mysql_servers (
    hostgroup_id,
    hostname,
    port,
    max_connections
) VALUES (
    10,
    '10.20.30.10',
    3306,
    1000
);

-- 读库 hostgroup 20
INSERT INTO mysql_servers (
    hostgroup_id,
    hostname,
    port,
    max_connections
) VALUES
    (20, '10.20.30.11', 3306, 1000),
    (20, '10.20.30.12', 3306, 1000);
```

配置应用用户：

```sql
-- 配置应用连接 ProxySQL 的账号
INSERT INTO mysql_users (
    username,
    password,
    default_hostgroup,
    transaction_persistent
) VALUES (
    'mall_app',
    'Mall_App_123456',
    10,
    1
);
```

配置读写分离规则：

```sql
-- SELECT 默认路由到读库
INSERT INTO mysql_query_rules (
    rule_id,
    active,
    match_pattern,
    destination_hostgroup,
    apply
) VALUES (
    100,
    1,
    '^SELECT.*',
    20,
    1
);

-- SELECT ... FOR UPDATE 路由到写库
INSERT INTO mysql_query_rules (
    rule_id,
    active,
    match_pattern,
    destination_hostgroup,
    apply
) VALUES (
    90,
    1,
    '^SELECT.*FOR UPDATE',
    10,
    1
);
```

加载并保存配置：

```sql
-- 加载到运行配置
LOAD MYSQL SERVERS TO RUNTIME;
LOAD MYSQL USERS TO RUNTIME;
LOAD MYSQL QUERY RULES TO RUNTIME;

-- 保存到磁盘，防止重启丢失
SAVE MYSQL SERVERS TO DISK;
SAVE MYSQL USERS TO DISK;
SAVE MYSQL QUERY RULES TO DISK;
```

查看后端连接状态：

```sql
-- 查看 ProxySQL 后端连接池状态
SELECT
    hostgroup,
    srv_host,
    srv_port,
    status,
    ConnUsed,
    ConnFree,
    Queries
FROM stats_mysql_connection_pool;
```

ProxySQL 使用建议：

1. 读写分离场景中，事务内查询应固定到主库。
2. `SELECT ... FOR UPDATE` 必须路由到写库。
3. 主从延迟较大时，不应把强一致读路由到从库。
4. ProxySQL 本身也需要高可用部署。
5. Query Rules 变更要测试，错误规则可能导致写请求路由到从库。
6. 生产环境应监控后端健康、连接池、查询量和错误数。

### Keepalived

Keepalived 常用于提供虚拟 IP，即 VIP。应用连接 VIP，VIP 指向当前主节点或当前代理节点。主节点故障时，VIP 漂移到备节点，从而实现入口切换。

典型 MySQL + Keepalived 结构：

```text
应用服务
   |
   v
VIP 10.20.30.100
   |
   +--> MySQL A：MASTER，持有 VIP
   |
   +--> MySQL B：BACKUP，故障时接管 VIP
```

Keepalived 配置示例，主节点：

```conf
vrrp_script chk_mysql {
    # 检查 MySQL 是否可用
    script "/etc/keepalived/check_mysql.sh"
    interval 2
    timeout 2
    fall 3
    rise 2
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1

    authentication {
        auth_type PASS
        auth_pass mysqlha
    }

    virtual_ipaddress {
        10.20.30.100/24
    }

    track_script {
        chk_mysql
    }
}
```

备节点配置：

```conf
vrrp_script chk_mysql {
    # 检查 MySQL 是否可用
    script "/etc/keepalived/check_mysql.sh"
    interval 2
    timeout 2
    fall 3
    rise 2
}

vrrp_instance VI_1 {
    state BACKUP
    interface eth0
    virtual_router_id 51
    priority 90
    advert_int 1

    authentication {
        auth_type PASS
        auth_pass mysqlha
    }

    virtual_ipaddress {
        10.20.30.100/24
    }

    track_script {
        chk_mysql
    }
}
```

MySQL 检查脚本：

```bash
#!/bin/bash

# 检查 MySQL 是否可以正常响应
mysqladmin ping \
  -h 127.0.0.1 \
  -P 3306 \
  -u monitor_user \
  -p'Monitor_123456' \
  --silent

exit $?
```

脚本授权并启动服务：

```bash
# 添加执行权限
sudo chmod +x /etc/keepalived/check_mysql.sh

# 启动 Keepalived
sudo systemctl start keepalived

# 设置开机自启
sudo systemctl enable keepalived

# 查看状态
sudo systemctl status keepalived
```

命令说明：

- `check_mysql.sh` 用于判断本机 MySQL 是否可用。
- `virtual_ipaddress` 配置 VIP。
- `priority` 越高，越优先持有 VIP。
- `track_script` 脚本失败后会降低节点可用性，触发 VIP 漂移。

Keepalived 使用建议：

1. Keepalived 只负责 VIP 漂移，不负责数据一致性。
2. MySQL 主从切换逻辑不能只依赖 VIP 漂移。
3. 必须防止两个节点同时可写造成脑裂。
4. 检查脚本不能只检查进程存活，还应检查数据库可写性或角色状态。
5. 更推荐将 Keepalived 放在 ProxySQL、HAProxy 等代理层前面，而不是直接绑 MySQL 主库。
6. VIP 漂移后，应用连接池可能需要重连。

### 读写分离

读写分离是指写请求走主库，读请求走从库。它可以降低主库读压力，但会引入主从延迟和读一致性问题。读写分离通常由应用层、数据库代理层或中间件实现。

读写分离结构：

```text
应用服务
   |
   v
读写分离组件
   |
   +--> 写请求：MySQL 主库
   |
   +--> 读请求：MySQL 从库
```

常见实现方式：

| 方式           | 说明                       |
| -------------- | -------------------------- |
| 应用层路由     | 代码中区分读库和写库数据源 |
| ProxySQL       | 代理层按 SQL 规则路由      |
| MySQL Router   | 配合 InnoDB Cluster 使用   |
| ShardingSphere | 应用层或代理层读写分离     |
| 云数据库代理   | 云厂商提供读写分离地址     |

读写分离关键规则：

| SQL 类型                | 推荐路由 |
| ----------------------- | -------- |
| `INSERT`                | 主库     |
| `UPDATE`                | 主库     |
| `DELETE`                | 主库     |
| DDL                     | 主库     |
| `SELECT ... FOR UPDATE` | 主库     |
| 事务内查询              | 主库     |
| 强一致查询              | 主库     |
| 普通列表查询            | 从库     |
| 报表查询                | 专用从库 |

读写分离风险示例：

```text
1. 用户提交订单，写入主库成功
2. 应用马上查询订单详情
3. 查询被路由到从库
4. 从库复制延迟，订单还没同步
5. 用户看到订单不存在
```

解决方式：

```text
常见处理策略：
1. 写后短时间内读主库
2. 关键查询强制读主库
3. 事务内全部走主库
4. 根据主从延迟动态摘除延迟从库
5. 使用业务缓存或写入结果直接返回
```

强制读主库示例规则：

```text
以下场景应读主库：
- 登录后立即读取用户状态
- 下单后立即查看订单
- 支付后立即查看支付结果
- 修改资料后立即读取资料
- 库存扣减前校验
- 所有 SELECT FOR UPDATE
```

读写分离建议：

1. 不要把所有 `SELECT` 都路由到从库。
2. 事务内读写必须固定主库。
3. 写后读一致性要有明确策略。
4. 从库延迟超过阈值时应自动摘除。
5. 报表查询应使用独立从库，避免影响在线读。
6. 应用层要支持强制读主库标记。
7. 读写分离不能替代缓存、索引和 SQL 优化。

### 故障切换

故障切换是指主库不可用时，将业务写入切换到新的主库。故障切换可以手动执行，也可以由高可用组件自动执行。切换的核心难点是确认数据完整性、防止脑裂、更新连接路由和恢复复制拓扑。

故障切换目标：

| 目标           | 说明                 |
| -------------- | -------------------- |
| 缩短不可用时间 | 尽快恢复写入能力     |
| 减少数据丢失   | 选择数据最新的从库   |
| 防止脑裂       | 确保旧主不能继续写   |
| 保持拓扑完整   | 其他从库改为复制新主 |
| 恢复业务连接   | 应用连接到新主库     |
| 保留现场       | 便于后续排查和补数据 |

手动故障切换流程：

```text
1. 确认主库故障，不是网络抖动或误判
2. 停止应用写入或切断旧主入口
3. 检查各从库复制状态和数据进度
4. 选择数据最完整的从库作为新主库
5. 停止新主库复制线程
6. 关闭新主库只读
7. 将应用写入口切到新主库
8. 其他从库改为复制新主库
9. 验证业务读写
10. 处理旧主库恢复后的数据一致性
```

提升从库为新主库：

```sql
-- 在候选从库执行
STOP REPLICA;

RESET REPLICA ALL;

SET GLOBAL super_read_only = OFF;
SET GLOBAL read_only = OFF;
```

确认新主库可写：

```sql
-- 查看只读状态
SHOW VARIABLES LIKE 'read_only';
SHOW VARIABLES LIKE 'super_read_only';

-- 写入测试表或由业务做健康检查，生产环境应使用专门健康检查表
SELECT NOW() AS check_time;
```

其他从库切换到新主库：

```sql
-- 在其他从库执行，使用 GTID 自动定位
STOP REPLICA;

CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '10.20.30.20',
    SOURCE_PORT = 3306,
    SOURCE_USER = 'repl_user',
    SOURCE_PASSWORD = 'Repl_123456',
    SOURCE_AUTO_POSITION = 1;

START REPLICA;
```

故障切换后检查复制状态：

```sql
-- 在其他从库检查复制状态
SHOW REPLICA STATUS\G
```

常见故障切换工具或组件：

| 组件                 | 说明                         |
| -------------------- | ---------------------------- |
| Orchestrator         | MySQL 拓扑管理和故障切换工具 |
| MHA                  | 经典 MySQL 主从高可用工具    |
| ProxySQL             | 可配合后端状态实现路由切换   |
| Keepalived           | 提供 VIP 漂移                |
| MySQL Router         | 配合 InnoDB Cluster 路由     |
| 云数据库 HA          | 云厂商托管故障切换能力       |
| MGR / InnoDB Cluster | MySQL 原生组复制高可用方案   |

故障切换风险：

1. 旧主库未完全隔离，恢复后继续写入，产生脑裂。
2. 候选从库不是最新节点，导致数据丢失。
3. 应用连接池未及时重连，仍访问旧地址。
4. 其他从库没有正确切到新主库。
5. 切换后只读配置未关闭，业务无法写入。
6. 切换后权限、账号、事件、任务与原主不一致。
7. Binlog 或 GTID 配置不规范，导致拓扑难以恢复。

故障切换建议：

1. 高可用切换必须有标准化流程或工具。
2. 切换前优先确认旧主库已隔离，避免脑裂。
3. 使用 GTID 简化复制重建。
4. 切换后立即验证写入、读取、复制状态和延迟。
5. 自动切换也必须配合人工告警和审计记录。
6. 原主库恢复后不要直接接回拓扑，应先检查数据差异。
7. 定期做故障演练，验证切换流程、应用重连和数据一致性。

## 性能优化

MySQL 性能优化应从业务 SQL、索引、表结构、参数配置、连接池、缓存命中率和慢查询分析等多个层面入手。优化时不要只看单条 SQL 的耗时，还要结合业务调用频率、数据量增长趋势、并发量、锁等待、主从延迟和资源使用情况综合判断。

性能优化的基本原则是：先定位瓶颈，再验证方案，最后上线观察。不要凭感觉盲目加索引、改参数或拆表。

### SQL 优化

SQL 优化的核心目标是减少扫描行数、减少回表、减少临时表、减少文件排序、减少锁范围，并让查询条件尽可能利用索引。

本节示例默认使用订单表：

```sql
-- 订单表示例，用于演示 SQL 优化
CREATE TABLE IF NOT EXISTS perf_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status_created (user_id, order_status, created_at),
    KEY idx_status_created (order_status, created_at),
    KEY idx_product_created (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能优化订单表示例';
```

避免 `SELECT *`：

```sql
-- 不推荐：查询全部字段，容易增加回表、网络传输和内存开销
SELECT
    *
FROM perf_order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

推荐只查询必要字段：

```sql
-- 推荐：只查询列表页需要的字段
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

避免对索引字段使用函数：

```sql
-- 不推荐：对 created_at 使用 DATE 函数，可能导致索引失效
SELECT
    id,
    order_no,
    created_at
FROM perf_order_info
WHERE DATE(created_at) = '2026-01-01';
```

推荐使用范围查询：

```sql
-- 推荐：使用左闭右开时间范围
SELECT
    id,
    order_no,
    created_at
FROM perf_order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00';
```

避免隐式类型转换：

```sql
-- 不推荐：order_no 是字符串字段，条件传入数字可能触发隐式转换
SELECT
    id,
    order_no
FROM perf_order_info
WHERE order_no = 202601010001;
```

推荐类型一致：

```sql
-- 推荐：字符串字段使用字符串条件
SELECT
    id,
    order_no
FROM perf_order_info
WHERE order_no = '202601010001';
```

优化 `OR` 条件。对于不同字段上的 `OR`，优化器可能难以稳定使用索引，可以视情况改为 `UNION ALL`：

```sql
-- 原始写法：不同字段 OR 查询
SELECT
    id,
    order_no,
    user_id,
    product_id,
    created_at
FROM perf_order_info
WHERE user_id = 10001
   OR product_id = 20001;
-- 可选优化：拆成两个可独立利用索引的查询
SELECT
    id,
    order_no,
    user_id,
    product_id,
    created_at
FROM perf_order_info
WHERE user_id = 10001

UNION ALL

SELECT
    id,
    order_no,
    user_id,
    product_id,
    created_at
FROM perf_order_info
WHERE product_id = 20001
  AND user_id <> 10001;
```

SQL 优化建议：

1. 优先减少扫描行数，而不是只追求语法简洁。
2. 避免在索引字段上使用函数、表达式和隐式类型转换。
3. 列表页不要使用 `SELECT *`。
4. 时间范围查询使用左闭右开。
5. 大结果集查询要分页或分批处理。
6. 多表关联时，关联字段必须有索引。
7. SQL 优化必须通过 `EXPLAIN` 和实际耗时验证。

### 索引优化

索引优化的目标是让高频查询、排序、分组和关联能够用较低成本完成。索引不是越多越好，过多索引会增加写入成本、磁盘占用和优化器选择成本。

针对用户订单列表创建联合索引：

```sql
-- 为用户订单列表查询创建联合索引
ALTER TABLE perf_order_info
  ADD KEY idx_user_deleted_created (
      user_id,
      deleted,
      created_at DESC,
      id DESC
  );
```

适合该索引的查询：

```sql
-- 根据用户查询未删除订单，并按时间倒序分页
SELECT
    id,
    order_no,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

针对后台订单列表创建联合索引：

```sql
-- 为状态和时间范围查询创建联合索引
ALTER TABLE perf_order_info
  ADD KEY idx_status_deleted_created (
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

适合该索引的查询：

```sql
-- 后台按状态查询订单列表
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

检查索引使用情况：

```sql
-- 查看执行计划，确认 key、rows、Extra 是否符合预期
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

识别重复索引：

```sql
-- 查看表索引，人工判断是否存在重复或前缀重复索引
SHOW INDEX FROM perf_order_info;
```

索引优化建议：

1. 多条件查询优先设计联合索引。
2. 等值条件字段通常放前面，范围和排序字段放后面。
3. 高频排序字段应与索引顺序匹配。
4. 低选择性字段单独建索引通常价值有限，可放入联合索引辅助过滤。
5. 不要为低频 SQL 创建很重的索引。
6. 定期清理重复索引、无用索引和过宽索引。
7. 新增索引后必须验证写入成本和执行计划。

### 表结构优化

表结构优化主要包括字段类型选择、字段长度控制、范式与反范式权衡、冷热字段拆分、避免大字段频繁读取、合理设计主键和索引。

不合理的字段设计示例：

```sql
-- 不推荐：字段类型过大，状态和金额设计不合理
CREATE TABLE IF NOT EXISTS bad_order_table (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键ID',
    order_no TEXT COMMENT '订单号',
    order_status VARCHAR(50) COMMENT '订单状态',
    pay_amount DOUBLE COMMENT '支付金额',
    remark TEXT COMMENT '备注',
    created_at VARCHAR(50) COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不合理订单表';
```

推荐的字段设计：

```sql
-- 推荐：字段类型明确，长度可控，金额使用 DECIMAL，时间使用 DATETIME
CREATE TABLE IF NOT EXISTS good_order_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    remark VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合理订单表';
```

冷热字段拆分示例：

```sql
-- 订单主表保存高频查询字段
CREATE TABLE IF NOT EXISTS order_main_perf (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单扩展表保存低频大字段
CREATE TABLE IF NOT EXISTS order_extend_perf (
    order_id BIGINT PRIMARY KEY COMMENT '订单ID',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    buyer_remark VARCHAR(1000) DEFAULT NULL COMMENT '买家备注',
    snapshot_json JSON DEFAULT NULL COMMENT '订单快照JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单扩展表';
```

表结构优化建议：

1. 主键尽量短、稳定、递增或趋势递增。
2. 金额使用 `DECIMAL`，不要使用 `DOUBLE`。
3. 状态、类型、标识优先使用 `TINYINT`、`SMALLINT`。
4. 时间字段使用 `DATETIME` 或 `TIMESTAMP`，不要使用字符串。
5. 高频列表表中避免放过多大字段。
6. 大字段、低频字段可拆到扩展表。
7. 字段类型越小越好，但必须满足业务范围。
8. 字符串字段长度要按实际业务控制，不要全部设置为 `VARCHAR(255)`。

### 参数配置优化

参数配置优化用于提升 MySQL 在当前硬件和业务负载下的稳定性和吞吐能力。参数不能照搬，应结合内存、CPU、磁盘、数据量、连接数、读写比例和业务峰值调整。

常见配置示例：

```ini
[mysqld]
# InnoDB 缓冲池大小，数据库专用服务器可配置为物理内存的较大比例
innodb_buffer_pool_size=8G

# Buffer Pool 实例数量，大内存场景可适当增加
innodb_buffer_pool_instances=4

# Redo Log 刷盘策略，1 持久性最好
innodb_flush_log_at_trx_commit=1

# Binlog 刷盘策略，1 更安全，但性能成本更高
sync_binlog=1

# 最大连接数，需结合应用连接池总量设置
max_connections=1000

# 单个通信包最大值，导入大 SQL 或大字段时需要关注
max_allowed_packet=64M

# 慢查询开关
slow_query_log=ON

# 慢查询阈值，单位秒
long_query_time=1

# 记录未使用索引的查询，排查阶段可开启，长期需谨慎
log_queries_not_using_indexes=OFF
```

查看核心参数：

```sql
-- 查看常见性能相关参数
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';
SHOW VARIABLES LIKE 'sync_binlog';
SHOW VARIABLES LIKE 'max_connections';
SHOW VARIABLES LIKE 'max_allowed_packet';
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

查看运行状态：

```sql
-- 查看连接和线程状态
SHOW GLOBAL STATUS LIKE 'Threads%';
SHOW GLOBAL STATUS LIKE 'Connections';
SHOW GLOBAL STATUS LIKE 'Max_used_connections';

-- 查看 InnoDB 缓冲池状态
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool%';
```

参数配置建议：

1. 修改参数前先记录当前值。
2. 优先调整明确瓶颈相关参数，不要批量盲改。
3. 持久性参数要结合数据安全要求，不要只追求性能。
4. 配置文件修改后需要规划重启窗口。
5. 动态参数可以先 `SET GLOBAL` 验证，再固化到配置文件。
6. 参数优化必须结合监控和压测。

### 连接数优化

连接数优化包括 MySQL 最大连接数、应用连接池大小、空闲连接回收、慢连接释放和连接泄漏排查。连接数过低会导致应用无法获取连接，连接数过高会增加数据库上下文切换、内存占用和调度成本。

查看连接数配置和使用情况：

```sql
-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看历史最大使用连接数
SHOW GLOBAL STATUS LIKE 'Max_used_connections';

-- 查看当前连接数
SHOW GLOBAL STATUS LIKE 'Threads_connected';

-- 查看当前活跃线程数
SHOW GLOBAL STATUS LIKE 'Threads_running';
```

查看当前连接明细：

```sql
-- 查看当前连接来源和状态
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
ORDER BY time DESC;
```

按用户和来源统计连接：

```sql
-- 统计不同用户和来源的连接数量
SELECT
    user,
    host,
    COUNT(*) AS connection_count
FROM information_schema.PROCESSLIST
GROUP BY user, host
ORDER BY connection_count DESC;
```

调整最大连接数：

```sql
-- 临时调整最大连接数，重启后失效
SET GLOBAL max_connections = 1000;
```

配置文件中设置：

```ini
[mysqld]
# 最大连接数，应结合应用连接池总量和数据库资源配置
max_connections=1000

# 连接空闲超时时间，单位秒
wait_timeout=600

# 交互式连接空闲超时时间，单位秒
interactive_timeout=600
```

连接数优化建议：

1. 应用连接池总连接数不要超过 MySQL 可承载范围。
2. 多个应用实例的连接池总和要统一核算。
3. `max_connections` 不是越大越好，过大会增加资源压力。
4. `Threads_running` 长期偏高比 `Threads_connected` 更值得关注。
5. 如果大量连接处于 `Sleep`，应检查连接池配置和空闲回收。
6. 如果频繁出现连接打满，应先定位慢 SQL、锁等待或连接泄漏。

### Buffer Pool 优化

Buffer Pool 是 InnoDB 缓存数据页和索引页的核心内存区域。读请求命中 Buffer Pool 可以减少磁盘 IO；写请求也会先修改内存页，再由后台刷盘。

查看 Buffer Pool 配置：

```sql
-- 查看 Buffer Pool 大小和实例数量
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'innodb_buffer_pool_instances';
```

查看 Buffer Pool 状态：

```sql
-- 查看 Buffer Pool 读请求和物理读
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';

-- 查看脏页、空闲页和总页数
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_dirty';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_free';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_total';
```

粗略计算 Buffer Pool 命中率：

```text
命中率估算公式：

1 - Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests

结果越接近 1，说明缓存命中越高。
```

配置 Buffer Pool：

```ini
[mysqld]
# Buffer Pool 大小，示例为 8G
innodb_buffer_pool_size=8G

# Buffer Pool 实例数，大内存场景可设置多个实例
innodb_buffer_pool_instances=4

# 开启 Buffer Pool 状态转储，重启后更快预热
innodb_buffer_pool_dump_at_shutdown=ON
innodb_buffer_pool_load_at_startup=ON
```

Buffer Pool 优化建议：

1. 数据库专用服务器上，Buffer Pool 通常是最重要的内存参数。
2. 配置过小会导致频繁磁盘读。
3. 配置过大会挤压操作系统和其他进程内存。
4. 大量全表扫描会污染 Buffer Pool，应优化 SQL 或放到低峰执行。
5. 热点数据和索引应尽量能被 Buffer Pool 容纳。
6. 调整后通过读请求、物理读、IO 和查询耗时验证效果。

### 慢查询优化

慢查询优化是性能优化中最常见的工作。慢 SQL 不一定是单次耗时最长的 SQL，也可能是单次不慢但调用频率极高的 SQL。

慢 SQL 示例：

```sql
-- 慢查询示例：按状态查询并排序，如果索引不匹配可能扫描和排序成本较高
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

查看执行计划：

```sql
-- 查看慢 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

添加匹配索引：

```sql
-- 根据过滤条件和排序字段创建联合索引
ALTER TABLE perf_order_info
  ADD KEY idx_status_deleted_created_id (
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

优化后再次验证：

```sql
-- 再次查看执行计划，重点看 key、rows、Extra
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

慢查询优化流程：

1. 从慢查询日志或监控中获取 SQL。
2. 确认 SQL 调用频率和业务场景。
3. 使用 `EXPLAIN` 分析执行计划。
4. 检查是否全表扫描、回表过多、临时表、文件排序。
5. 根据查询条件和排序字段设计索引。
6. 重写 SQL，减少扫描行数。
7. 使用测试数据验证耗时和执行计划。
8. 上线后观察慢查询数量、CPU、IO 和锁等待。

### 批量写入优化

批量写入包括批量插入、批量更新、批量删除和批量导入。优化目标是减少网络往返、减少事务提交次数、控制锁范围和避免大事务。

不推荐逐条插入：

```sql
-- 不推荐：大量逐条 INSERT 会产生较多网络往返和提交成本
INSERT INTO perf_order_info (order_no, user_id, product_id, order_status, pay_amount)
VALUES ('O202601010001', 10001, 20001, 1, 99.90);

INSERT INTO perf_order_info (order_no, user_id, product_id, order_status, pay_amount)
VALUES ('O202601010002', 10002, 20002, 1, 199.90);
```

推荐批量插入：

```sql
-- 推荐：一条 INSERT 写入多行
INSERT INTO perf_order_info (
    order_no,
    user_id,
    product_id,
    order_status,
    pay_amount
) VALUES
    ('O202601010001', 10001, 20001, 1, 99.90),
    ('O202601010002', 10002, 20002, 1, 199.90),
    ('O202601010003', 10003, 20003, 1, 299.90);
```

批量更新时分批处理：

```sql
-- 每次只更新一批，避免大事务和长时间锁定
UPDATE perf_order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY id
LIMIT 1000;
```

批量删除时也分批执行：

```sql
-- 分批删除历史数据，避免一次删除过多
DELETE FROM perf_order_info
WHERE deleted = 1
  AND created_at < DATE_SUB(NOW(), INTERVAL 180 DAY)
ORDER BY id
LIMIT 1000;
```

批量写入建议：

1. 批量插入优先使用多值 `INSERT` 或 `LOAD DATA`。
2. 每批数据量要控制，避免 SQL 过大和事务过大。
3. 批量更新、删除必须带明确条件和索引。
4. 大批量变更应分批提交。
5. 批量任务应避开业务高峰。
6. 主从架构下要关注批量写入造成的复制延迟。
7. 批量删除历史数据时，分区表可以直接 `DROP PARTITION`。

### 分页查询优化

分页查询常见问题是深分页。`LIMIT offset, size` 在 offset 很大时，MySQL 需要扫描并跳过大量数据，性能会明显下降。

普通分页：

```sql
-- 浅分页可以使用 LIMIT offset, size
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 0, 20;
```

深分页问题：

```sql
-- 不推荐：深分页需要跳过大量记录
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 100000, 20;
```

使用游标分页：

```sql
-- 推荐：使用上一页最后一条记录的 created_at 和 id 作为游标
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
  AND (
      created_at < '2026-01-10 12:00:00'
      OR (
          created_at = '2026-01-10 12:00:00'
          AND id < 900000
      )
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

配套索引：

```sql
-- 为用户游标分页创建索引
ALTER TABLE perf_order_info
  ADD KEY idx_user_created_id_desc (
      user_id,
      created_at DESC,
      id DESC
  );
```

先查 ID 再回表：

```sql
-- 优化深分页的一种方式：先基于覆盖索引查 ID
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.pay_amount,
    o.created_at
FROM perf_order_info AS o
JOIN (
    SELECT
        id
    FROM perf_order_info
    WHERE user_id = 10001
    ORDER BY created_at DESC, id DESC
    LIMIT 100000, 20
) AS t ON o.id = t.id
ORDER BY o.created_at DESC, o.id DESC;
```

分页优化建议：

1. 浅分页可以使用普通 `LIMIT`。
2. 深分页优先使用游标分页。
3. 排序字段应稳定，建议追加主键。
4. 分页排序字段要有匹配索引。
5. 不要在大表上无限制开放任意页跳转。
6. 后台导出大数据应使用游标或按主键分批，不要使用深分页。

### 大表查询优化

大表查询优化的核心是控制扫描范围。大表通常不能依赖全表扫描、全量排序和临时聚合，应通过索引、分区、汇总表、冷热拆分、归档和分批处理降低查询成本。

大表按时间范围查询：

```sql
-- 推荐：大表查询必须带时间范围或高选择性条件
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-02-01 00:00:00'
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 100;
```

为大表查询创建索引：

```sql
-- 根据状态和时间创建联合索引
ALTER TABLE perf_order_info
  ADD KEY idx_status_created_id (
      order_status,
      created_at DESC,
      id DESC
  );
```

大表统计建议使用汇总表：

```sql
-- 用户日统计表，避免每次扫描订单大表
CREATE TABLE IF NOT EXISTS order_user_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_user (stat_date, user_id),
    KEY idx_user_date (user_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单日汇总表';
```

写入汇总数据：

```sql
-- 按天汇总订单数据
INSERT INTO order_user_daily_summary (
    stat_date,
    user_id,
    order_count,
    pay_amount
)
SELECT
    DATE(created_at) AS stat_date,
    user_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(pay_amount), 0.00) AS pay_amount
FROM perf_order_info
WHERE order_status = 1
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00'
GROUP BY DATE(created_at), user_id
ON DUPLICATE KEY UPDATE
    order_count = VALUES(order_count),
    pay_amount = VALUES(pay_amount),
    updated_at = NOW();
```

大表查询优化建议：

1. 大表查询必须有明确过滤条件。
2. 时间范围查询应避免跨太大范围。
3. 高频统计使用汇总表或 OLAP 系统。
4. 历史数据归档，减少主表体积。
5. 按时间清理的大表可以考虑分区表。
6. 大字段拆分到扩展表。
7. 导出大表数据必须分批。
8. 对大表执行 DDL 前必须评估锁、耗时和回滚方案。

## 慢查询分析

慢查询分析是定位数据库性能问题的重要手段。MySQL 慢查询日志可以记录执行时间超过阈值的 SQL，再结合 `EXPLAIN`、`EXPLAIN ANALYZE`、`mysqldumpslow`、`pt-query-digest` 和业务调用链定位问题。

慢查询分析的目标不是只找出“最慢的一条 SQL”，而是找出“最影响系统的 SQL”。一条 SQL 单次耗时 2 秒、每天执行 10 次，影响可能远小于一条 SQL 单次耗时 200 毫秒、每天执行 100 万次。

### 开启慢查询日志

慢查询日志由 `slow_query_log` 控制。开启后，MySQL 会将超过阈值的 SQL 写入慢查询日志文件。

查看慢查询日志状态：

```sql
-- 查看慢查询是否开启
SHOW VARIABLES LIKE 'slow_query_log';

-- 查看慢查询日志文件路径
SHOW VARIABLES LIKE 'slow_query_log_file';

-- 查看慢查询阈值
SHOW VARIABLES LIKE 'long_query_time';
```

临时开启慢查询日志：

```sql
-- 临时开启慢查询日志，重启后可能失效
SET GLOBAL slow_query_log = ON;

-- 设置慢查询阈值为 1 秒
SET GLOBAL long_query_time = 1;
```

配置文件中开启：

```ini
[mysqld]
# 开启慢查询日志
slow_query_log=ON

# 慢查询日志路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 慢查询阈值，单位秒
long_query_time=1

# 不记录管理语句，按需设置
log_slow_admin_statements=ON

# 记录未使用索引的查询，排查期可开启，长期需谨慎
log_queries_not_using_indexes=OFF
```

重启后验证：

```sql
-- 验证慢查询配置是否生效
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'slow_query_log_file';
SHOW VARIABLES LIKE 'long_query_time';
```

开启慢查询建议：

1. 生产环境建议长期开启慢查询日志。
2. 阈值应结合业务要求设置，常见为 0.5 秒、1 秒或 2 秒。
3. 排查期间可以临时降低阈值，但要注意日志量。
4. `log_queries_not_using_indexes` 可能产生大量日志，不建议长期打开。
5. 慢查询日志路径要规划磁盘空间和轮转策略。

### 慢查询参数配置

慢查询相关参数决定哪些 SQL 会被记录、记录到哪里、记录多少内容。合理配置可以提高问题定位效率，避免日志过多或漏掉关键 SQL。

常见参数：

| 参数                            | 说明                          |
| ------------------------------- | ----------------------------- |
| `slow_query_log`                | 是否开启慢查询日志            |
| `slow_query_log_file`           | 慢查询日志文件路径            |
| `long_query_time`               | 慢查询阈值，单位秒            |
| `min_examined_row_limit`        | 扫描行数低于该值的 SQL 不记录 |
| `log_queries_not_using_indexes` | 是否记录未使用索引的查询      |
| `log_slow_admin_statements`     | 是否记录慢管理语句            |
| `log_output`                    | 日志输出到 FILE 或 TABLE      |

查看慢查询配置：

```sql
-- 查看慢查询相关配置
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'slow_query_log_file';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'min_examined_row_limit';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
SHOW VARIABLES LIKE 'log_slow_admin_statements';
SHOW VARIABLES LIKE 'log_output';
```

设置扫描行数阈值：

```sql
-- 只记录扫描行数超过 1000 的慢查询
SET GLOBAL min_examined_row_limit = 1000;
```

设置日志输出到文件：

```sql
-- 推荐输出到文件，便于使用工具分析
SET GLOBAL log_output = 'FILE';
```

配置文件示例：

```ini
[mysqld]
# 慢查询日志输出到文件
log_output=FILE

# 开启慢查询
slow_query_log=ON

# 慢查询文件路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 超过 1 秒记录
long_query_time=1

# 扫描行数少于 100 的 SQL 不记录
min_examined_row_limit=100

# 记录慢管理语句
log_slow_admin_statements=ON
```

参数配置建议：

1. 慢查询日志优先输出到文件，便于工具分析。
2. `long_query_time` 不宜设置过大，否则容易漏掉高频中慢 SQL。
3. 排查全表扫描时可临时开启 `log_queries_not_using_indexes`。
4. 慢查询日志要做日志轮转，避免磁盘占满。
5. 参数调整后应观察日志量和性能影响。

### 慢查询日志查看

慢查询日志文件中包含 SQL 执行时间、锁等待时间、返回行数、扫描行数、时间戳和 SQL 内容。直接查看适合排查单个问题，批量分析应使用工具。

查看慢查询日志路径：

```sql
-- 查看慢查询日志路径
SHOW VARIABLES LIKE 'slow_query_log_file';
```

Linux 查看最近日志：

```bash
# 查看慢查询日志最后 200 行
sudo tail -n 200 /var/log/mysql/mysql-slow.log

# 实时跟踪慢查询日志
sudo tail -f /var/log/mysql/mysql-slow.log
```

命令说明：

- `tail -n 200` 查看最近 200 行日志。
- `tail -f` 实时观察新增慢查询。
- `/var/log/mysql/mysql-slow.log` 需要替换为实际慢查询日志路径。

慢查询日志示例：

```text
# Time: 2026-01-01T10:30:00.123456+08:00
# User@Host: mall_app[mall_app] @ 10.20.30.15 []
# Query_time: 2.345678  Lock_time: 0.000123 Rows_sent: 20  Rows_examined: 500000
SET timestamp=1760000000;
SELECT id, order_no, user_id, pay_amount, created_at
FROM perf_order_info
WHERE order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

关键字段说明：

| 字段            | 说明                     |
| --------------- | ------------------------ |
| `Query_time`    | SQL 总执行时间           |
| `Lock_time`     | 等待表锁或元数据锁等时间 |
| `Rows_sent`     | 返回给客户端的行数       |
| `Rows_examined` | 扫描或检查的行数         |
| `User@Host`     | 执行用户和来源           |
| `SET timestamp` | SQL 执行时间戳           |
| SQL 内容        | 实际执行语句             |

慢查询日志查看建议：

1. `Rows_examined` 远大于 `Rows_sent`，通常说明过滤效率低。
2. `Query_time` 高但 `Rows_examined` 不高，可能是锁等待、IO 或网络问题。
3. `Lock_time` 高时要排查锁等待和 DDL。
4. 只看单条慢 SQL 不够，还要统计同类 SQL 总耗时。
5. 大量类似 SQL 应用工具聚合分析。

### mysqldumpslow 使用

`mysqldumpslow` 是 MySQL 自带的慢查询日志分析工具，可以对相似 SQL 进行归类统计。它适合快速查看慢查询日志中最慢、最频繁或扫描最多的 SQL 模板。

查看耗时最高的 10 类 SQL：

```bash
# 按平均查询时间排序，查看前 10 类慢 SQL
mysqldumpslow \
  -s at \
  -t 10 \
  /var/log/mysql/mysql-slow.log
```

查看总执行时间最高的 SQL：

```bash
# 按总查询时间排序
mysqldumpslow \
  -s t \
  -t 10 \
  /var/log/mysql/mysql-slow.log
```

查看执行次数最多的 SQL：

```bash
# 按执行次数排序
mysqldumpslow \
  -s c \
  -t 10 \
  /var/log/mysql/mysql-slow.log
```

按返回行数排序：

```bash
# 按平均返回行数排序
mysqldumpslow \
  -s ar \
  -t 10 \
  /var/log/mysql/mysql-slow.log
```

命令参数说明：

| 参数     | 说明               |
| -------- | ------------------ |
| `-s at`  | 按平均查询时间排序 |
| `-s t`   | 按总查询时间排序   |
| `-s c`   | 按执行次数排序     |
| `-s ar`  | 按平均返回行数排序 |
| `-t 10`  | 只显示前 10 条     |
| 日志路径 | 慢查询日志文件路径 |

`mysqldumpslow` 使用建议：

1. 适合快速粗略分析慢查询日志。
2. 先看执行次数最多和总耗时最高的 SQL。
3. 不要只看单次最慢 SQL。
4. 大型日志分析能力有限，复杂分析建议用 `pt-query-digest`。
5. 分析前可先按日期切割日志，避免文件过大。

### pt-query-digest 使用

`pt-query-digest` 是 Percona Toolkit 中的慢查询分析工具，比 `mysqldumpslow` 更强大。它可以统计 SQL 模板的总耗时、平均耗时、95 分位耗时、扫描行数、执行次数、样例 SQL 等信息。

安装 Percona Toolkit 后分析慢查询日志：

```bash
# 使用 pt-query-digest 分析慢查询日志
pt-query-digest \
  /var/log/mysql/mysql-slow.log \
  > slow_report.txt
```

查看分析报告：

```bash
# 查看报告前 100 行
head -n 100 slow_report.txt
```

分析指定时间范围的日志文件：

```bash
# 先截取指定日期日志，再分析
grep -A 20 '2026-01-01' /var/log/mysql/mysql-slow.log > slow_20260101.log

pt-query-digest \
  slow_20260101.log \
  > slow_20260101_report.txt
```

按数据库过滤：

```bash
# 只分析指定库的慢 SQL
pt-query-digest \
  --filter '$event->{db} && $event->{db} eq "mall_order"' \
  /var/log/mysql/mysql-slow.log \
  > mall_order_slow_report.txt
```

报告中重点关注：

| 指标           | 说明         |
| -------------- | ------------ |
| `Query count`  | SQL 执行次数 |
| `Exec time`    | 总执行时间   |
| `Latency`      | 延迟统计     |
| `Rows sent`    | 返回行数     |
| `Rows examine` | 扫描行数     |
| `Query size`   | SQL 大小     |
| `95%`          | 95 分位耗时  |
| `Fingerprint`  | SQL 模板     |
| `Sample`       | 样例 SQL     |

`pt-query-digest` 使用建议：

1. 优先关注总耗时高的 SQL 模板。
2. 其次关注执行次数高、平均耗时中等的 SQL。
3. `Rows examine` 高的 SQL通常需要检查索引。
4. 95 分位耗时高说明尾延迟明显。
5. 报告中的样例 SQL 需要再用 `EXPLAIN` 验证。
6. 建议定期生成报告，观察优化前后变化。

### 慢 SQL 定位

慢 SQL 定位应从日志、监控、执行计划和业务链路一起分析。单看数据库日志可能无法知道 SQL 来自哪个接口、哪个任务或哪个用户操作，因此应用层也应记录 SQL 调用链或接口耗时。

定位流程：

```text
1. 从监控发现数据库 CPU、IO、连接数或慢查询异常
2. 查看慢查询日志，找到高影响 SQL 模板
3. 根据 SQL 中的表、条件和注释定位业务模块
4. 使用 EXPLAIN 分析执行计划
5. 在测试环境复现 SQL
6. 结合数据分布和索引设计优化
7. 上线后验证慢查询是否下降
```

给 SQL 增加业务注释，便于定位来源：

```sql
-- 通过 SQL 注释标识业务接口来源
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
/* module=order, api=listUserOrders */
```

查看当前正在执行的慢 SQL：

```sql
-- 查看当前执行时间较长的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC;
```

查看锁等待相关事务：

```sql
-- 查看当前 InnoDB 长事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

慢 SQL 常见来源：

| 来源       | 特征                       |
| ---------- | -------------------------- |
| 列表接口   | 分页、排序、条件组合复杂   |
| 报表接口   | 聚合、分组、统计范围大     |
| 导出任务   | 大范围扫描和传输           |
| 定时任务   | 批量更新、删除、汇总       |
| 搜索接口   | 模糊查询、动态条件多       |
| 管理后台   | 条件自由组合，容易绕过索引 |
| 第三方同步 | 批量写入和重复更新         |

慢 SQL 定位建议：

1. 慢查询日志负责发现问题 SQL。
2. APM 或应用日志负责定位接口来源。
3. `PROCESSLIST` 可查看当前正在执行的 SQL。
4. `INNODB_TRX` 可排查长事务和锁等待。
5. SQL 中可加入模块注释，便于从日志反查业务。
6. 对报表、导出、定时任务要单独限流和隔离。

### 慢 SQL 优化流程

慢 SQL 优化需要标准流程，避免“看到慢就加索引”的低质量优化。优化结果必须通过执行计划、实际耗时和线上监控验证。

标准优化流程：

```text
1. 收集 SQL：从慢查询日志、APM、监控中获取 SQL
2. 归类 SQL：按 SQL 指纹合并同类 SQL
3. 判断影响：看执行次数、总耗时、平均耗时、扫描行数
4. 分析计划：使用 EXPLAIN 和 EXPLAIN ANALYZE
5. 分析数据：查看表数据量、字段选择性、索引现状
6. 制定方案：重写 SQL、加索引、改表结构、加汇总表或分批处理
7. 测试验证：使用接近生产的数据验证耗时
8. 风险评估：评估索引构建、写入成本、锁影响和回滚方案
9. 上线观察：关注慢查询、CPU、IO、连接数和锁等待
10. 复盘沉淀：记录问题原因和优化方案
```

示例慢 SQL：

```sql
-- 慢 SQL：后台按状态查询最新订单
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

分析执行计划：

```sql
-- 查看执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

检查字段选择性：

```sql
-- 查看状态、逻辑删除和时间字段的数据分布
SELECT
    COUNT(*) AS total_count,
    COUNT(DISTINCT order_status) AS status_distinct_count,
    COUNT(DISTINCT deleted) AS deleted_distinct_count,
    MIN(created_at) AS min_created_at,
    MAX(created_at) AS max_created_at
FROM perf_order_info;
```

添加索引：

```sql
-- 添加匹配过滤和排序的联合索引
ALTER TABLE perf_order_info
  ADD KEY idx_status_deleted_created_id (
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

优化后验证：

```sql
-- 查看优化后的执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM perf_order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

优化效果检查项：

| 检查项   | 期望结果                                     |
| -------- | -------------------------------------------- |
| `type`   | 避免 `ALL`，尽量为 `ref`、`range` 等         |
| `key`    | 命中预期索引                                 |
| `rows`   | 扫描行数明显下降                             |
| `Extra`  | 尽量减少 `Using temporary`、`Using filesort` |
| 实际耗时 | 明显下降且稳定                               |
| 写入成本 | 新索引没有明显拖慢写入                       |
| 线上指标 | CPU、IO、慢查询数量下降                      |

慢 SQL 优化建议：

1. 优先优化总耗时高、调用频繁的 SQL。
2. 不要只优化偶发单次慢 SQL。
3. SQL 改写和索引设计要一起考虑。
4. 复杂报表不一定适合在 MySQL 主库实时计算。
5. 大表统计优先考虑汇总表、离线计算或 OLAP。
6. 索引优化后要观察写入性能。
7. 每次优化应记录优化前后执行计划和耗时。

## 大表设计与治理

大表治理的目标不是简单“拆表”，而是控制单表数据量、索引体积、查询扫描范围、DDL 风险、备份恢复成本和历史数据增长速度。大表问题通常不是某一天突然出现，而是随着业务增长逐步积累，因此应在设计阶段预留治理方案。

### 大表判断标准

大表没有绝对统一标准，需要结合数据量、行宽、索引数量、查询模式、写入频率和运维成本判断。常见判断维度包括行数、数据文件大小、索引大小、单表 DDL 耗时、查询性能和备份恢复耗时。

常见参考标准：

| 维度         | 参考判断                                     |
| ------------ | -------------------------------------------- |
| 行数         | 超过千万级开始重点关注，超过亿级需要治理方案 |
| 数据大小     | 单表数据和索引超过几十 GB 需要关注           |
| 查询耗时     | 高频查询稳定超过业务阈值                     |
| DDL 耗时     | 加字段、加索引耗时明显且影响业务             |
| 备份恢复     | 单表恢复时间过长                             |
| 删除历史数据 | 普通 `DELETE` 清理历史数据成本很高           |
| 索引维护     | 写入明显受索引数量和体积影响                 |

查看表数据量和大小：

```sql
-- 查看当前库中表的数据大小、索引大小和估算行数
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_mb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS total_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY total_mb DESC;
```

查看指定表索引情况：

```sql
-- 查看大表索引信息
SHOW INDEX FROM order_info;
```

统计表中时间范围分布：

```sql
-- 查看订单表按月份的数据量分布
SELECT
    DATE_FORMAT(created_at, '%Y-%m') AS month_value,
    COUNT(*) AS row_count
FROM order_info
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month_value ASC;
```

大表判断建议：

1. 不要只按行数判断，还要看行宽和索引大小。
2. 宽表千万级数据就可能很重，窄表几千万行也可能可接受。
3. 高频在线查询表比离线归档表更需要提前治理。
4. 有明显时间生命周期的数据，应优先考虑归档或分区。
5. 大表治理前先分析 SQL、索引、数据分布和增长速度。

### 冷热数据拆分

冷热数据拆分是将高频访问的热数据和低频访问的冷数据分开存储。常见做法是保留近几个月数据在主表，将历史数据迁移到历史表、归档库或对象存储。

示例：订单主表保存近 6 个月数据，历史表保存 6 个月以前数据。

```sql
-- 热数据订单表
CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 冷数据订单历史表，结构通常与主表保持一致
CREATE TABLE IF NOT EXISTS order_info_history LIKE order_info;
```

将历史数据写入历史表：

```sql
-- 将 6 个月前的数据复制到历史表
INSERT INTO order_info_history
SELECT
    *
FROM order_info
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY id
LIMIT 10000;
```

确认已归档后删除主表历史数据：

```sql
-- 分批删除已归档历史数据，避免大事务
DELETE FROM order_info
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY id
LIMIT 10000;
```

冷热数据查询示例：

```sql
-- 查询热数据
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;

-- 查询历史数据
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info_history
WHERE user_id = 10001
  AND created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY created_at DESC
LIMIT 20;
```

冷热拆分建议：

1. 适合订单、日志、流水、消息、审计记录等有时间生命周期的数据。
2. 热表只保留高频访问数据，降低索引和查询成本。
3. 冷表可以放在低成本存储、归档库或独立实例。
4. 归档过程必须可重试、可校验、可追踪。
5. 删除热表数据前必须确认历史表写入成功。
6. 查询历史数据时应明确走历史查询入口，避免在线接口无意扫描冷数据。

### 水平分表

水平分表是将同一张逻辑表的数据按某个规则拆成多张结构相同的物理表。常见分表键包括用户 ID、订单 ID、租户 ID、业务号哈希值等。

按用户 ID 哈希分 4 张订单表：

```sql
-- 订单分表 0
CREATE TABLE IF NOT EXISTS order_info_0 (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分表0';

-- 其他分表结构保持一致
CREATE TABLE IF NOT EXISTS order_info_1 LIKE order_info_0;
CREATE TABLE IF NOT EXISTS order_info_2 LIKE order_info_0;
CREATE TABLE IF NOT EXISTS order_info_3 LIKE order_info_0;
```

分表路由规则示例：

```text
分表规则：

table_index = user_id % 4

user_id = 10001
10001 % 4 = 1
路由到 order_info_1
```

按用户查询时可以精准路由：

```sql
-- user_id = 10001 时，应用层路由到 order_info_1
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info_1
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

水平分表适合场景：

1. 单表数据量极大，且按某个分片键查询频繁。
2. 写入压力集中在单表，索引维护成本过高。
3. 数据天然可以按用户、租户、订单号等维度拆分。
4. 单表 DDL、备份、恢复成本过高。

水平分表风险：

1. 跨分表查询复杂。
2. 全局唯一约束变难。
3. 分页、排序、聚合需要跨表合并。
4. 后期扩容迁移复杂。
5. 分表键选错后治理成本很高。
6. 应用层或中间件需要负责路由。

水平分表建议：

1. 分表键必须是高频查询条件。
2. 优先选择数据分布均匀的字段。
3. 分表数量不要过少，也不要过多。
4. 全局 ID 应使用雪花 ID 或统一发号器。
5. 需要跨分表统计的场景，应提前设计汇总表或离线计算。
6. 分表前先确认索引、归档、分区是否已无法满足需求。

### 垂直分表

垂直分表是按字段维度拆表，将一张宽表拆成主表和扩展表。主表保存高频访问字段，扩展表保存低频字段、大字段或敏感字段。

订单主表：

```sql
-- 订单主表，保存高频查询字段
CREATE TABLE IF NOT EXISTS order_main (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

订单扩展表：

```sql
-- 订单扩展表，保存低频大字段
CREATE TABLE IF NOT EXISTS order_extend (
    order_id BIGINT PRIMARY KEY COMMENT '订单ID',
    receiver_name VARCHAR(64) DEFAULT NULL COMMENT '收货人姓名',
    receiver_mobile VARCHAR(20) DEFAULT NULL COMMENT '收货人手机号',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    buyer_remark VARCHAR(1000) DEFAULT NULL COMMENT '买家备注',
    order_snapshot JSON DEFAULT NULL COMMENT '订单快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单扩展表';
```

列表页只查主表：

```sql
-- 列表页避免查询扩展表大字段
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_main
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

详情页再关联扩展表：

```sql
-- 详情页按主键查询扩展信息
SELECT
    m.id,
    m.order_no,
    m.user_id,
    m.order_status,
    m.pay_amount,
    e.receiver_name,
    e.receiver_mobile,
    e.receiver_address,
    e.buyer_remark,
    e.order_snapshot,
    m.created_at
FROM order_main AS m
LEFT JOIN order_extend AS e ON m.id = e.order_id
WHERE m.id = 10001;
```

垂直分表建议：

1. 宽表中有大字段、低频字段时优先考虑垂直拆分。
2. 列表页字段和详情页字段应分开设计。
3. 主表字段尽量短小，保证热点查询效率。
4. 扩展表以主表 ID 作为主键，便于一对一关联。
5. 敏感字段也可以拆到独立表，并单独控制权限。
6. 垂直拆分后要避免高频接口每次都关联扩展表。

### 分库分表

分库分表是同时将数据拆分到多个数据库和多张表中，用于解决单库容量、连接数、写入吞吐、单实例资源瓶颈等问题。它比单纯分表复杂得多，通常需要中间件或应用层路由支持。

典型结构：

```text
逻辑表：order_info

实际存储：
mall_order_0.order_info_0
mall_order_0.order_info_1
mall_order_1.order_info_0
mall_order_1.order_info_1
```

路由规则示例：

```text
分库规则：
db_index = user_id % 2

分表规则：
table_index = user_id / 2 % 2

user_id = 10001
db_index = 10001 % 2 = 1
table_index = 10001 / 2 % 2 = 0

路由到：
mall_order_1.order_info_0
```

分库分表后的表结构：

```sql
-- 每个库中的每张分表结构保持一致
CREATE TABLE IF NOT EXISTS order_info_0 (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分库分表示例表';
```

分库分表需要重点解决：

| 问题       | 说明                           |
| ---------- | ------------------------------ |
| 路由规则   | 如何根据分片键定位库表         |
| 全局 ID    | 自增主键不能直接跨库表全局唯一 |
| 跨分片查询 | 需要广播或多路查询合并         |
| 跨分片事务 | 本地事务无法直接覆盖多个库     |
| 扩容迁移   | 分片数量变化后数据迁移复杂     |
| 唯一约束   | 跨分片唯一性难以靠单库索引保证 |
| 运维复杂度 | 备份、恢复、DDL、监控都更复杂  |

分库分表建议：

1. 只有在单库单表治理手段不足时再采用。
2. 分片键必须稳定、不可频繁变更。
3. 核心查询必须尽量带分片键。
4. 全局 ID 使用雪花算法、发号器或号段模式。
5. 跨分片聚合尽量使用汇总表、搜索引擎或离线计算。
6. 分库分表前必须评估未来扩容方案。
7. 优先使用成熟中间件或框架，不建议临时手写复杂路由逻辑。

### 历史数据归档

历史数据归档用于将低频访问数据从在线表迁移到历史表、归档库或外部存储。归档可以降低主表体积、提升查询效率、降低备份成本和减少 DDL 风险。

创建归档任务日志表：

```sql
-- 归档任务日志表
CREATE TABLE IF NOT EXISTS archive_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    archive_condition VARCHAR(500) NOT NULL COMMENT '归档条件',
    archive_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '归档数量',
    delete_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除数量',
    task_status VARCHAR(20) NOT NULL COMMENT '任务状态：SUCCESS成功，FAIL失败',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_task_created (task_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务日志表';
```

分批归档历史订单：

```sql
-- 分批复制历史数据到归档表
INSERT INTO order_info_history
SELECT
    *
FROM order_info
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY id
LIMIT 5000;
```

确认归档后分批删除：

```sql
-- 分批删除已经归档的历史数据
DELETE FROM order_info
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY id
LIMIT 5000;
```

记录归档日志：

```sql
-- 记录归档任务结果
INSERT INTO archive_task_log (
    task_name,
    archive_condition,
    archive_count,
    delete_count,
    task_status,
    error_message
) VALUES (
    'archive_order_info',
    'created_at < current_date - 6 month',
    5000,
    5000,
    'SUCCESS',
    NULL
);
```

归档校验：

```sql
-- 校验指定时间范围内主表和历史表数据量
SELECT
    COUNT(*) AS history_count
FROM order_info_history
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);

SELECT
    COUNT(*) AS hot_count
FROM order_info
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
```

历史数据归档建议：

1. 归档任务必须分批执行。
2. 先复制，校验成功后再删除。
3. 每批归档记录任务日志。
4. 归档条件必须稳定，例如按时间和主键范围。
5. 归档表索引可以少于在线表，按历史查询需求设计。
6. 对分区表，可以直接使用 `DROP PARTITION` 或 `EXCHANGE PARTITION` 做历史数据治理。
7. 归档数据也应有备份和权限控制。

### 在线 DDL

在线 DDL 是指在尽量不阻塞业务读写的情况下修改表结构。MySQL 8 支持较多 Online DDL 能力，但不同操作是否真正无锁、是否重建表、是否消耗大量资源，需要结合具体语句确认。

常见 DDL 操作：

| 操作         | 风险                         |
| ------------ | ---------------------------- |
| 添加普通字段 | 可能较轻，但依赖版本和写法   |
| 添加索引     | 大表耗时较长，消耗 IO 和 CPU |
| 修改字段类型 | 通常风险较高                 |
| 删除字段     | 需要评估依赖和数据恢复       |
| 修改主键     | 高风险                       |
| 修改字符集   | 高风险，可能重建表           |
| 添加唯一索引 | 需要先检查重复数据           |

添加字段示例：

```sql
-- 添加字段，MySQL 8 部分场景可使用 INSTANT
ALTER TABLE order_info
  ADD COLUMN source_type TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型：0未知，1小程序，2后台'
  ALGORITHM=INSTANT;
```

添加索引示例：

```sql
-- 添加索引，尽量使用在线 DDL 能力
ALTER TABLE order_info
  ADD KEY idx_source_created (source_type, created_at),
  ALGORITHM=INPLACE,
  LOCK=NONE;
```

查看 DDL 是否被阻塞：

```sql
-- 查看当前执行中的 DDL 或锁等待
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE info IS NOT NULL
ORDER BY time DESC;
```

在线 DDL 建议：

1. 大表 DDL 前必须在测试环境验证耗时。
2. 执行前检查磁盘空间、主从延迟和业务低峰窗口。
3. 添加唯一索引前必须检查重复数据。
4. 高风险 DDL 可使用 `pt-online-schema-change` 或 `gh-ost`。
5. DDL 前备份表结构和关键数据。
6. DDL 后验证表结构、索引、执行计划和应用功能。
7. 不要在业务高峰对大表执行重型 DDL。

### 大表字段变更

大表字段变更包括添加字段、修改字段类型、修改默认值、修改注释、删除字段等。不同变更风险差异很大，其中修改字段类型、字符集、字段顺序、删除字段通常风险更高。

添加字段：

```sql
-- 添加普通字段，优先放在表末尾，降低变更成本
ALTER TABLE order_info
  ADD COLUMN channel_code VARCHAR(32) DEFAULT NULL COMMENT '渠道编码';
```

修改默认值：

```sql
-- 修改字段默认值
ALTER TABLE order_info
  ALTER COLUMN channel_code SET DEFAULT 'UNKNOWN';
```

修改字段注释：

```sql
-- 修改字段注释时需要保留完整字段类型定义
ALTER TABLE order_info
  MODIFY COLUMN channel_code VARCHAR(32) DEFAULT NULL COMMENT '订单渠道编码';
```

高风险字段类型变更：

```sql
-- 高风险：大表修改字段类型可能重建表，执行前必须评估
ALTER TABLE order_info
  MODIFY COLUMN channel_code VARCHAR(64) DEFAULT NULL COMMENT '订单渠道编码';
```

删除字段：

```sql
-- 高风险：删除字段前必须确认应用不再使用
ALTER TABLE order_info
  DROP COLUMN channel_code;
```

大表字段变更建议：

1. 新增字段优先允许 `NULL` 或提供默认值。
2. 新字段优先加在表末尾，不强制调整字段顺序。
3. 修改字段类型前评估是否会重建表。
4. 删除字段前先下线应用依赖，再观察，再删除。
5. 字段重命名属于破坏性变更，应谨慎。
6. 大表字段变更应拆成多个安全步骤，不要一次做太多操作。
7. 变更后应检查应用日志和慢查询。

### 大表索引变更

大表索引变更包括新增索引、删除索引、修改索引、创建唯一索引和治理重复索引。索引变更通常消耗大量 IO、CPU 和磁盘空间，并可能造成主从延迟。

添加索引前先分析 SQL：

```sql
-- 查看目标 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

添加联合索引：

```sql
-- 添加匹配查询条件和排序的联合索引
ALTER TABLE order_info
  ADD KEY idx_user_status_created (user_id, order_status, created_at DESC, id DESC);
```

添加唯一索引前检查重复：

```sql
-- 检查订单号是否重复
SELECT
    order_no,
    COUNT(*) AS duplicate_count
FROM order_info
GROUP BY order_no
HAVING COUNT(*) > 1
LIMIT 20;
```

确认无重复后添加唯一索引：

```sql
-- 添加订单号唯一索引
ALTER TABLE order_info
  ADD UNIQUE KEY uk_order_no (order_no);
```

删除索引前先隐藏观察：

```sql
-- MySQL 8 支持隐藏索引，删除前可先隐藏观察
ALTER TABLE order_info
  ALTER INDEX idx_user_status_created INVISIBLE;
```

恢复索引可见：

```sql
-- 如果隐藏后出现性能问题，可以恢复索引可见
ALTER TABLE order_info
  ALTER INDEX idx_user_status_created VISIBLE;
```

确认无影响后删除索引：

```sql
-- 删除确认无用的索引
ALTER TABLE order_info
  DROP INDEX idx_user_status_created;
```

大表索引变更建议：

1. 新增索引前必须确认对应 SQL 是高频或高影响 SQL。
2. 联合索引字段顺序要结合等值、范围、排序和覆盖字段设计。
3. 添加索引前检查是否已有相似索引。
4. 添加唯一索引前必须检查重复数据。
5. 删除索引前优先使用隐藏索引观察。
6. 索引变更会影响写入性能和主从延迟。
7. 大表索引变更应避开业务高峰，并准备回滚方案。

## 项目开发表设计规范

项目表设计规范用于统一团队数据库设计风格，降低沟通成本、维护成本和故障风险。规范的核心是字段含义清晰、类型合理、索引可控、审计字段统一、状态字段可读、历史数据可治理。

### 表命名规范

表名应简洁、清晰、稳定，能表达业务含义。建议使用小写字母、数字和下划线，不使用大写、空格、中文或特殊字符。

推荐规则：

| 规则                       | 示例                  |
| -------------------------- | --------------------- |
| 使用小写下划线             | `order_info`          |
| 使用业务名词               | `user_account`        |
| 避免缩写过度               | `product_category`    |
| 关联表使用两个实体名       | `user_role`           |
| 历史表使用 `_history` 后缀 | `order_info_history`  |
| 日志表使用 `_log` 后缀     | `order_change_log`    |
| 临时表使用 `tmp_` 前缀     | `tmp_import_order`    |
| 汇总表使用 `_summary` 后缀 | `order_daily_summary` |

推荐表名：

```text
sys_user
sys_role
sys_user_role
order_info
order_item
order_payment
order_change_log
product_info
product_category
inventory_record
user_login_log
```

不推荐表名：

```text
User
T_ORDER
订单表
order-info
tb_order_info
data
info
orderinfo
```

表命名建议：

1. 不建议使用 `tb_`、`t_` 等无实际业务含义前缀。
2. 表名使用单数或复数要团队统一，推荐使用单数业务名词。
3. 表名不要使用 MySQL 保留字，例如 `order`、`user`。
4. 日志、历史、快照、汇总表通过后缀区分用途。
5. 表注释必须填写清楚业务含义。

### 字段命名规范

字段名应使用小写下划线，表达明确业务含义。字段名不要使用拼音、中文、保留字和过度缩写。

推荐字段名：

```text
id
user_id
order_no
order_status
pay_amount
created_at
updated_at
deleted
version
remark
```

不推荐字段名：

```text
ID
userId
ddh
zt
je
time
desc
type
flag
```

字段命名常见约定：

| 字段         | 说明         |
| ------------ | ------------ |
| `id`         | 主键 ID      |
| `xxx_id`     | 关联对象 ID  |
| `xxx_no`     | 业务编号     |
| `xxx_code`   | 业务编码     |
| `xxx_name`   | 名称         |
| `xxx_status` | 状态         |
| `xxx_type`   | 类型         |
| `xxx_amount` | 金额         |
| `xxx_count`  | 数量         |
| `created_at` | 创建时间     |
| `updated_at` | 更新时间     |
| `deleted`    | 逻辑删除     |
| `version`    | 乐观锁版本号 |
| `remark`     | 备注         |

字段命名建议：

1. 布尔语义字段避免只叫 `flag`，应表达具体含义，例如 `enabled`、`deleted`。
2. 状态字段使用 `status` 后缀，例如 `order_status`。
3. 类型字段使用 `type` 后缀，例如 `source_type`。
4. 时间字段使用 `_at` 后缀，例如 `paid_at`、`created_at`。
5. 数量字段使用 `_count` 后缀。
6. 金额字段使用 `_amount` 后缀。
7. 每个字段都必须有注释。

### 主键设计规范

主键用于唯一标识一行数据。InnoDB 中主键也是聚簇索引，因此主键设计会影响数据组织方式和所有二级索引大小。

推荐主键设计：

```sql
-- 推荐：使用 BIGINT 主键
CREATE TABLE IF NOT EXISTS standard_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准订单表';
```

分布式 ID 主键：

```sql
-- 分布式系统中可以使用雪花ID，由应用层生成
CREATE TABLE IF NOT EXISTS standard_user_info (
    id BIGINT PRIMARY KEY COMMENT '主键ID，雪花ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准用户表';
```

不推荐使用业务字段作为主键：

```sql
-- 不推荐：手机号可能变更，不适合作为主键
CREATE TABLE IF NOT EXISTS bad_user_pk (
    mobile VARCHAR(20) PRIMARY KEY COMMENT '手机号',
    username VARCHAR(64) NOT NULL COMMENT '用户名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不推荐主键示例表';
```

主键设计建议：

1. 每张表必须有主键。
2. 主键推荐使用 `BIGINT`。
3. 单机系统可使用 `AUTO_INCREMENT`。
4. 分布式系统可使用雪花 ID 或统一发号器。
5. 主键不要使用手机号、邮箱、身份证号等业务字段。
6. 主键不要频繁变更。
7. 主键字段越短，二级索引越节省空间。

### 索引设计规范

索引用于优化查询、排序、分组和关联。项目中应基于真实 SQL 设计索引，不要为每个字段都建索引。

常见索引命名：

| 索引类型 | 命名规则          | 示例                      |
| -------- | ----------------- | ------------------------- |
| 主键     | `PRIMARY`         | `PRIMARY KEY (id)`        |
| 唯一索引 | `uk_字段名`       | `uk_order_no`             |
| 普通索引 | `idx_字段名`      | `idx_user_id`             |
| 联合索引 | `idx_字段1_字段2` | `idx_user_status_created` |
| 全文索引 | `ft_字段名`       | `ft_title_content`        |

推荐索引设计：

```sql
-- 推荐：根据唯一约束、查询条件和排序字段设计索引
CREATE TABLE IF NOT EXISTS standard_order_index (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准订单索引表示例';
```

索引设计建议：

1. 唯一业务字段必须建唯一索引。
2. 外键关联字段或逻辑关联字段应建索引。
3. 高频查询条件字段优先考虑索引。
4. 高频排序字段应与查询条件组成联合索引。
5. 联合索引遵循最左前缀原则。
6. 不为低频查询随意建索引。
7. 不为每个字段建单列索引。
8. 索引字段不宜过多，过宽索引会影响写入。
9. 大字段不适合直接建完整索引，可考虑前缀索引或搜索引擎。
10. 删除索引前先确认是否有 SQL 依赖。

### 字段类型规范

字段类型应按业务含义和数据范围选择。类型过大会浪费存储和内存，类型过小会导致溢出或后续变更。

常见类型选择：

| 场景          | 推荐类型          |
| ------------- | ----------------- |
| 主键 ID       | `BIGINT`          |
| 状态          | `TINYINT`         |
| 数量          | `INT` 或 `BIGINT` |
| 金额          | `DECIMAL(18,2)`   |
| 比率          | `DECIMAL(10,4)`   |
| 短名称        | `VARCHAR(64)`     |
| 标题          | `VARCHAR(200)`    |
| 手机号        | `VARCHAR(20)`     |
| 邮箱          | `VARCHAR(128)`    |
| 订单号        | `VARCHAR(64)`     |
| 创建时间      | `DATETIME`        |
| JSON 扩展属性 | `JSON`            |

标准字段示例：

```sql
-- 字段类型选择示例
CREATE TABLE IF NOT EXISTS standard_field_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    buy_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '购买数量',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    discount_rate DECIMAL(10,4) NOT NULL DEFAULT 1.0000 COMMENT '折扣率',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    extend_attr JSON DEFAULT NULL COMMENT '扩展属性',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段类型规范示例表';
```

字段类型建议：

1. 金额必须使用 `DECIMAL`，不要使用 `FLOAT` 或 `DOUBLE`。
2. 手机号、身份证号、订单号按字符串存储。
3. 状态字段使用 `TINYINT`。
4. 时间字段使用 `DATETIME`，不要使用字符串。
5. 大文本字段使用 `TEXT` 前要确认是否真的需要。
6. JSON 适合扩展属性，不适合核心查询字段。
7. 字段尽量设置 `NOT NULL`，并提供合理默认值。

### 时间字段规范

时间字段用于记录数据生命周期和业务节点。常见时间字段包括创建时间、更新时间、支付时间、删除时间、过期时间等。

标准时间字段：

```sql
-- 标准时间字段
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

业务时间字段示例：

```sql
-- 订单业务时间字段
paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
refunded_at DATETIME DEFAULT NULL COMMENT '退款时间',
expired_at DATETIME DEFAULT NULL COMMENT '过期时间'
```

完整示例：

```sql
-- 时间字段规范示例
CREATE TABLE IF NOT EXISTS standard_time_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    expired_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间字段规范示例表';
```

时间字段建议：

1. 每张业务表建议包含 `created_at` 和 `updated_at`。
2. 时间字段统一使用 `DATETIME`。
3. 时间范围查询使用左闭右开。
4. 字段命名使用 `_at` 后缀。
5. 不使用字符串保存时间。
6. 分库、跨时区系统应统一时间标准。
7. 时间字段经常用于查询时应建立合适索引。

### 状态字段规范

状态字段用于表示业务对象当前状态。状态值应使用数值类型存储，注释中明确每个状态含义。应用层可使用枚举维护状态名称。

订单状态字段示例：

```sql
-- 订单状态字段
order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款'
```

用户状态字段示例：

```sql
-- 用户状态字段
user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用'
```

完整示例：

```sql
-- 状态字段规范示例
CREATE TABLE IF NOT EXISTS standard_status_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态字段规范示例表';
```

状态字段建议：

1. 状态字段使用 `TINYINT`。
2. 字段注释必须写清楚枚举值。
3. 默认值必须有业务含义。
4. 不建议使用字符串保存状态。
5. 状态流转应由应用层控制，不要随意更新。
6. 高频状态查询可与时间字段组成联合索引。
7. 状态值不要复用，废弃状态也应保留历史含义。

### 逻辑删除字段规范

逻辑删除用于保留数据，同时让业务查询默认过滤已删除数据。常见字段名为 `deleted`，通常 `0` 表示未删除，`1` 表示已删除。

标准逻辑删除字段：

```sql
-- 逻辑删除字段
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除'
```

带删除时间的设计：

```sql
-- 逻辑删除字段和删除时间
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
deleted_at DATETIME DEFAULT NULL COMMENT '删除时间'
```

完整示例：

```sql
-- 逻辑删除字段规范示例
CREATE TABLE IF NOT EXISTS standard_deleted_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_deleted_created (deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑删除字段规范示例表';
```

逻辑删除查询：

```sql
-- 默认查询必须过滤未删除数据
SELECT
    id,
    username,
    nickname,
    created_at
FROM standard_deleted_field
WHERE deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

逻辑删除更新：

```sql
-- 执行逻辑删除
UPDATE standard_deleted_field
SET
    deleted = 1,
    deleted_at = NOW(),
    updated_at = NOW()
WHERE id = 10001
  AND deleted = 0;
```

逻辑删除建议：

1. 逻辑删除字段统一使用 `deleted`。
2. `0` 表示未删除，`1` 表示已删除。
3. 查询默认必须带 `deleted = 0`。
4. 高频查询可将 `deleted` 放入联合索引。
5. 如果需要审计删除时间，增加 `deleted_at`。
6. 逻辑删除不等于归档，历史数据仍会占用表空间和索引。
7. 大量逻辑删除数据应定期归档或物理清理。

### 版本号字段规范

版本号字段常用于乐观锁，避免并发更新覆盖。常见字段名为 `version`，每次更新时版本号加 1，并在 `WHERE` 条件中带上旧版本号。

标准版本号字段：

```sql
-- 乐观锁版本号
version INT NOT NULL DEFAULT 0 COMMENT '版本号'
```

完整示例：

```sql
-- 版本号字段规范示例
CREATE TABLE IF NOT EXISTS standard_version_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本号字段规范示例表';
```

乐观锁更新：

```sql
-- 使用版本号控制并发更新
UPDATE standard_version_field
SET
    balance_amount = balance_amount - 10.00,
    version = version + 1,
    updated_at = NOW()
WHERE user_id = 10001
  AND version = 3
  AND balance_amount >= 10.00;
```

更新后检查影响行数：

```sql
-- affected_rows = 1 表示更新成功，0 表示版本冲突或条件不满足
SELECT ROW_COUNT() AS affected_rows;
```

版本号字段建议：

1. 版本号字段统一使用 `version`。
2. 类型使用 `INT`，默认值为 `0`。
3. 更新时必须 `version = version + 1`。
4. `WHERE` 条件中必须带旧版本号。
5. 更新影响行数为 0 时，应按并发冲突处理。
6. 乐观锁适合低冲突场景，高冲突场景可能需要行锁或队列化处理。

### 备注字段规范

备注字段用于保存人工输入的补充说明。备注字段通常不是核心查询条件，不应随意建索引。长度应控制，避免所有备注都使用 `TEXT`。

标准备注字段：

```sql
-- 常规备注字段
remark VARCHAR(500) DEFAULT NULL COMMENT '备注'
```

较长备注字段：

```sql
-- 较长备注字段，确实需要长文本时再使用
remark TEXT DEFAULT NULL COMMENT '备注'
```

完整示例：

```sql
-- 备注字段规范示例
CREATE TABLE IF NOT EXISTS standard_remark_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    biz_status TINYINT NOT NULL DEFAULT 0 COMMENT '业务状态：0待处理，1已处理',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_no (biz_no),
    KEY idx_status_created (biz_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备注字段规范示例表';
```

备注字段建议：

1. 普通备注优先使用 `VARCHAR(500)`。
2. 确实需要长文本时再使用 `TEXT`。
3. 备注字段不作为核心查询条件。
4. 不建议给备注字段建普通索引。
5. 用户输入备注要在应用层限制长度。
6. 敏感信息不应随意写入备注。
7. 日志、导出和展示备注时要注意脱敏要求。

## 项目开发 SQL 规范

项目开发 SQL 规范用于统一团队 SQL 编写方式，降低慢查询、锁等待、误更新、误删除和线上故障风险。规范的重点不是限制开发效率，而是让 SQL 更稳定、更可读、更容易被索引优化，也更容易在生产环境排查问题。

### 查询字段避免使用星号

查询字段应明确列出，不建议在业务代码中使用 `SELECT *`。使用星号会导致字段不可控，增加网络传输、内存占用和回表概率，也会让表结构变更影响查询结果。

不推荐写法：

```sql
-- 不推荐：查询所有字段，容易读取不必要的大字段
SELECT
    *
FROM order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

推荐写法：

```sql
-- 推荐：只查询接口或页面需要的字段
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

详情查询也应明确字段：

```sql
-- 推荐：详情页明确查询字段
SELECT
    id,
    order_no,
    user_id,
    product_id,
    order_status,
    pay_amount,
    receiver_name,
    receiver_mobile,
    receiver_address,
    created_at,
    updated_at
FROM order_info
WHERE id = 10001;
```

避免使用星号的原因：

| 问题         | 说明                                     |
| ------------ | ---------------------------------------- |
| 字段不可控   | 表新增字段后查询结果自动变化             |
| 网络开销增加 | 返回不必要字段，增加传输量               |
| 内存开销增加 | 应用层反序列化和对象映射成本增加         |
| 回表概率增加 | 无法利用覆盖索引                         |
| 大字段风险   | 可能读取 `TEXT`、`JSON`、`BLOB` 等大字段 |
| 兼容性风险   | 字段顺序变化可能影响低质量代码           |

开发建议：

1. 所有业务 SQL 必须显式列出字段。
2. 列表页只返回列表展示字段。
3. 详情页再查询详情字段。
4. 大字段不要出现在普通列表查询中。
5. Mapper、DAO、报表 SQL 都应避免 `SELECT *`。
6. 代码评审中应把 `SELECT *` 作为 SQL 规范检查项。

### 条件字段使用索引

高频查询条件字段应有合适索引。没有索引的条件查询在数据量增长后容易变成全表扫描，导致 CPU、IO、慢查询和锁等待问题。

用户订单查询示例：

```sql
-- 高频查询：根据用户查询订单列表
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

推荐索引：

```sql
-- 根据查询条件和排序字段创建联合索引
ALTER TABLE order_info
  ADD KEY idx_user_deleted_created (
      user_id,
      deleted,
      created_at DESC,
      id DESC
  );
```

后台订单查询示例：

```sql
-- 高频查询：后台按状态查询订单
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

推荐索引：

```sql
-- 状态、逻辑删除、创建时间组成联合索引
ALTER TABLE order_info
  ADD KEY idx_status_deleted_created (
      order_status,
      deleted,
      created_at DESC,
      id DESC
  );
```

使用 `EXPLAIN` 验证：

```sql
-- 验证 SQL 是否命中预期索引
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE order_status = 1
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

条件字段索引建议：

1. 高频 `WHERE` 字段应优先考虑索引。
2. 多条件查询优先设计联合索引。
3. 排序字段应尽量和过滤字段组成联合索引。
4. 低选择性字段单独建索引价值有限，例如 `deleted`、`status`。
5. 关联字段必须有索引，例如 `order_id`、`user_id`、`product_id`。
6. 是否命中索引必须通过 `EXPLAIN` 验证。

### 避免隐式类型转换

隐式类型转换是指字段类型和查询条件类型不一致，MySQL 为了比较而自动转换类型。它可能导致索引失效、结果异常或性能下降。

字符串字段使用数字条件是不推荐写法：

```sql
-- 不推荐：order_no 是 VARCHAR，条件却传入数字
SELECT
    id,
    order_no,
    user_id
FROM order_info
WHERE order_no = 202601010001;
```

推荐写法：

```sql
-- 推荐：字符串字段使用字符串条件
SELECT
    id,
    order_no,
    user_id
FROM order_info
WHERE order_no = '202601010001';
```

手机号字段也应使用字符串：

```sql
-- 推荐：手机号是字符串，不要按数字比较
SELECT
    id,
    username,
    mobile
FROM sys_user
WHERE mobile = '13800000001';
```

状态字段使用数值：

```sql
-- 推荐：order_status 是 TINYINT，条件使用数值
SELECT
    id,
    order_no,
    order_status
FROM order_info
WHERE order_status = 1;
```

不推荐写法：

```sql
-- 不推荐：数值字段使用字符串条件，容易形成不必要转换
SELECT
    id,
    order_no,
    order_status
FROM order_info
WHERE order_status = '1';
```

隐式转换常见场景：

| 字段类型         | 不推荐条件              | 推荐条件                              |
| ---------------- | ----------------------- | ------------------------------------- |
| `VARCHAR` 订单号 | `order_no = 10001`      | `order_no = '10001'`                  |
| `VARCHAR` 手机号 | `mobile = 13800000001`  | `mobile = '13800000001'`              |
| `BIGINT` 用户 ID | `user_id = '10001'`     | `user_id = 10001`                     |
| `TINYINT` 状态   | `status = '1'`          | `status = 1`                          |
| `DATETIME` 时间  | `created_at = 20260101` | `created_at >= '2026-01-01 00:00:00'` |

开发建议：

1. 查询条件类型必须与字段类型一致。
2. 订单号、手机号、身份证号、编码字段统一按字符串处理。
3. ID、状态、数量字段按数值处理。
4. Java 参数类型应和数据库字段类型对应。
5. 动态 SQL 中不要把所有参数都拼成字符串。
6. 出现索引未命中时，应检查是否存在隐式类型转换。

### 避免函数导致索引失效

对索引字段使用函数、表达式或计算，可能导致 MySQL 无法直接使用索引定位数据。时间字段、字符串字段和数值字段都容易出现这类问题。

不推荐对时间字段使用函数：

```sql
-- 不推荐：DATE(created_at) 可能导致 created_at 索引失效
SELECT
    id,
    order_no,
    created_at
FROM order_info
WHERE DATE(created_at) = '2026-01-01';
```

推荐使用范围查询：

```sql
-- 推荐：使用时间范围，便于利用 created_at 索引
SELECT
    id,
    order_no,
    created_at
FROM order_info
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at <  '2026-01-02 00:00:00';
```

不推荐对字符串字段使用函数：

```sql
-- 不推荐：对 username 使用函数，可能无法利用 username 索引
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE LOWER(username) = 'admin';
```

推荐提前规范数据或使用函数索引：

```sql
-- 推荐方案一：存储时统一 username 小写，查询时直接等值匹配
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE username = 'admin';
```

如果确实需要函数查询，可以使用 MySQL 8 函数索引：

```sql
-- 可选方案：为 LOWER(username) 创建函数索引
CREATE INDEX idx_lower_username
ON sys_user ((LOWER(username)));
```

不推荐对数值字段计算：

```sql
-- 不推荐：对 pay_amount 做计算后比较，可能影响索引使用
SELECT
    id,
    order_no,
    pay_amount
FROM order_info
WHERE pay_amount * 100 > 10000;
```

推荐改写为字段直接比较：

```sql
-- 推荐：把计算移动到常量侧
SELECT
    id,
    order_no,
    pay_amount
FROM order_info
WHERE pay_amount > 100.00;
```

开发建议：

1. 不要对索引字段套函数后再过滤。
2. 时间查询统一使用范围条件。
3. 计算逻辑尽量放到常量侧或应用层。
4. 大小写不敏感查询应通过统一存储、排序规则或函数索引解决。
5. JSON 路径高频查询可以使用生成列或函数索引。
6. 对已有 SQL 应通过 `EXPLAIN` 确认函数是否影响索引。

### 避免深分页

深分页是指使用很大的 `OFFSET` 查询后面的数据，例如 `LIMIT 100000, 20`。MySQL 需要扫描并丢弃前面大量记录，性能会随着页码增大而明显变差。

不推荐深分页：

```sql
-- 不推荐：深分页需要跳过大量数据
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 100000, 20;
```

推荐使用游标分页：

```sql
-- 推荐：使用上一页最后一条数据作为游标
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
  AND (
      created_at < '2026-01-10 12:00:00'
      OR (
          created_at = '2026-01-10 12:00:00'
          AND id < 900000
      )
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

配套索引：

```sql
-- 游标分页配套索引
ALTER TABLE order_info
  ADD KEY idx_user_created_id (
      user_id,
      created_at DESC,
      id DESC
  );
```

可以先查 ID 再回表：

```sql
-- 可选优化：先通过覆盖索引查询 ID，再关联主表
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.pay_amount,
    o.created_at
FROM order_info AS o
JOIN (
    SELECT
        id
    FROM order_info
    WHERE user_id = 10001
    ORDER BY created_at DESC, id DESC
    LIMIT 100000, 20
) AS t ON o.id = t.id
ORDER BY o.created_at DESC, o.id DESC;
```

分页查询建议：

1. 前台用户列表优先使用游标分页或下一页加载。
2. 后台管理系统应限制最大可跳转页数。
3. 排序字段必须稳定，建议追加主键排序。
4. 分页排序字段必须有匹配索引。
5. 大数据导出不要使用深分页，应按主键或时间范围分批。
6. 需要总数时要谨慎，`COUNT(*)` 在大范围条件下也可能很慢。

### 避免大事务

大事务会持有大量锁、生成大量 Undo Log 和 Redo Log，可能导致锁等待、主从延迟、回滚时间长、Buffer Pool 压力和磁盘压力。项目开发中应尽量控制事务边界和单次处理数据量。

不推荐大事务：

```sql
-- 不推荐：一次性更新大量数据，容易形成大事务
START TRANSACTION;

UPDATE order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);

COMMIT;
```

推荐分批更新：

```sql
-- 推荐：每次只更新一批数据
UPDATE order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY id
LIMIT 1000;
```

分批删除：

```sql
-- 推荐：历史数据分批删除
DELETE FROM order_log
WHERE created_at < DATE_SUB(NOW(), INTERVAL 180 DAY)
ORDER BY id
LIMIT 1000;
```

事务中不应包含外部调用：

```text
不推荐事务流程：

1. 开启事务
2. 修改订单
3. 调用第三方 HTTP 接口
4. 发送 MQ
5. 上传文件
6. 提交事务

问题：
事务时间不可控，锁持有时间过长，容易造成连接占用和锁等待。
```

推荐拆分：

```text
推荐流程：

1. 本地事务只处理必须强一致的数据库写入
2. 提交事务
3. 事务外调用外部接口
4. 使用本地消息表、重试任务或 MQ 处理后续异步动作
```

大事务规避建议：

1. 单个事务只包含必须强一致的数据库操作。
2. 不在事务中执行 HTTP、RPC、MQ、文件上传等外部操作。
3. 批量更新、删除必须分批提交。
4. 事务内查询和更新都要命中索引，减少锁范围。
5. 长事务要接入监控。
6. 定时任务和数据修复脚本要控制每批处理量。
7. 大事务执行失败回滚也会很慢，应提前避免。

### 避免循环单条写入

循环单条写入会产生大量网络往返和事务提交成本。批量新增、批量更新、批量导入应尽量使用批量 SQL、`LOAD DATA` 或应用层批处理。

不推荐逐条插入：

```sql
-- 不推荐：循环执行大量单条 INSERT
INSERT INTO order_item (order_id, product_id, product_name, buy_count, sale_price)
VALUES (10001, 20001, '商品A', 1, 99.00);

INSERT INTO order_item (order_id, product_id, product_name, buy_count, sale_price)
VALUES (10001, 20002, '商品B', 2, 199.00);
```

推荐批量插入：

```sql
-- 推荐：一条 INSERT 插入多行
INSERT INTO order_item (
    order_id,
    product_id,
    product_name,
    buy_count,
    sale_price
) VALUES
    (10001, 20001, '商品A', 1, 99.00),
    (10001, 20002, '商品B', 2, 199.00),
    (10001, 20003, '商品C', 1, 299.00);
```

批量插入并处理重复键：

```sql
-- 批量 UPSERT，适合按唯一键插入或更新
INSERT INTO user_tag (
    user_id,
    tag_code,
    tag_name,
    updated_at
) VALUES
    (10001, 'VIP', '会员用户', NOW()),
    (10002, 'ACTIVE', '活跃用户', NOW())
ON DUPLICATE KEY UPDATE
    tag_name = VALUES(tag_name),
    updated_at = VALUES(updated_at);
```

大文件导入使用 `LOAD DATA`：

```sql
-- 推荐：大批量 CSV 导入使用 LOAD DATA
LOAD DATA INFILE '/var/lib/mysql-files/order_item.csv'
INTO TABLE order_item
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(order_id, product_id, product_name, buy_count, sale_price);
```

开发建议：

1. 批量新增使用多值 `INSERT`。
2. 大文件导入优先使用 `LOAD DATA`。
3. 应用层批处理要控制每批大小，例如 500 到 2000 行。
4. 批量写入不要单次 SQL 过大，避免超过 `max_allowed_packet`。
5. 批量写入应考虑唯一键冲突处理策略。
6. 批量任务要记录成功数、失败数和错误原因。

### 避免无条件更新删除

无条件 `UPDATE` 和 `DELETE` 是高危 SQL，可能导致整表数据被误修改或误删除。生产环境中，更新和删除必须带明确条件，最好带主键、唯一键或明确范围。

高危写法：

```sql
-- 高危：没有 WHERE 条件，会更新整张表
UPDATE order_info
SET order_status = 2;
```

高危删除：

```sql
-- 高危：没有 WHERE 条件，会删除整张表数据
DELETE FROM order_info;
```

推荐按主键更新：

```sql
-- 推荐：按主键和状态条件更新
UPDATE order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE id = 10001
  AND order_status = 0;
```

推荐按明确范围删除：

```sql
-- 推荐：按明确时间范围和状态删除，并分批处理
DELETE FROM order_log
WHERE created_at < '2025-01-01 00:00:00'
  AND log_status = 9
ORDER BY id
LIMIT 1000;
```

更新前先查询确认范围：

```sql
-- 更新前先确认影响范围
SELECT
    COUNT(*) AS affected_count
FROM order_info
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

再执行更新：

```sql
-- 确认数量合理后再更新
UPDATE order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY id
LIMIT 1000;
```

开发建议：

1. `UPDATE` 和 `DELETE` 必须带 `WHERE` 条件。
2. 条件应尽量包含主键、唯一键或索引字段。
3. 批量更新、删除必须先 `SELECT COUNT(*)` 确认影响范围。
4. 大范围更新、删除必须分批执行。
5. 生产执行高危 SQL 前必须备份或确认可恢复。
6. 客户端工具建议开启安全更新模式。
7. 代码评审中应重点检查更新删除条件。

### 避免复杂嵌套查询

复杂嵌套查询可读性差、优化难度高，容易产生临时表、文件排序和大量中间结果。项目开发中应优先使用清晰的 JOIN、CTE、临时表或拆分查询来降低复杂度。

不推荐复杂嵌套：

```sql
-- 不推荐：多层嵌套，阅读和优化都困难
SELECT
    t.user_id,
    t.total_amount
FROM (
    SELECT
        o.user_id,
        SUM(o.pay_amount) AS total_amount
    FROM order_info AS o
    WHERE o.user_id IN (
        SELECT
            u.id
        FROM sys_user AS u
        WHERE u.status = 1
          AND u.deleted = 0
    )
    GROUP BY o.user_id
) AS t
WHERE t.total_amount > 1000.00
ORDER BY t.total_amount DESC;
```

推荐使用 JOIN：

```sql
-- 推荐：使用 JOIN 表达关联关系
SELECT
    o.user_id,
    SUM(o.pay_amount) AS total_amount
FROM order_info AS o
JOIN sys_user AS u ON o.user_id = u.id
WHERE u.status = 1
  AND u.deleted = 0
  AND o.order_status = 1
GROUP BY o.user_id
HAVING SUM(o.pay_amount) > 1000.00
ORDER BY total_amount DESC;
```

MySQL 8 可以使用 CTE 提升可读性：

```sql
-- 使用 CTE 拆分逻辑，提高可读性
WITH active_user AS (
    SELECT
        id
    FROM sys_user
    WHERE status = 1
      AND deleted = 0
),
user_order_summary AS (
    SELECT
        o.user_id,
        SUM(o.pay_amount) AS total_amount
    FROM order_info AS o
    JOIN active_user AS u ON o.user_id = u.id
    WHERE o.order_status = 1
    GROUP BY o.user_id
)
SELECT
    user_id,
    total_amount
FROM user_order_summary
WHERE total_amount > 1000.00
ORDER BY total_amount DESC;
```

复杂统计可以落入汇总表：

```sql
-- 推荐：高频复杂统计使用汇总表承接
CREATE TABLE IF NOT EXISTS user_order_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数量',
    total_pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计支付金额',
    summary_date DATE NOT NULL COMMENT '统计日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, summary_date),
    KEY idx_summary_date_amount (summary_date, total_pay_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单汇总表';
```

开发建议：

1. 嵌套超过两层时，应考虑改写。
2. 关联关系清晰时优先使用 JOIN。
3. 逻辑分段明显时可以使用 CTE。
4. 高频复杂统计应使用汇总表或离线计算。
5. 大量子查询要通过 `EXPLAIN` 验证执行计划。
6. 不要为了“一条 SQL 完成所有事情”牺牲可读性和性能。

### SQL 可读性规范

SQL 可读性规范用于统一团队 SQL 风格，便于代码评审、问题排查和后期维护。好的 SQL 应该字段清晰、条件清晰、缩进一致、别名明确、注释适度。

推荐格式：

```sql
-- 推荐：字段、表、条件、排序结构清晰
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.order_status,
    o.pay_amount,
    o.created_at
FROM order_info AS o
WHERE o.user_id = 10001
  AND o.deleted = 0
  AND o.created_at >= '2026-01-01 00:00:00'
  AND o.created_at <  '2026-02-01 00:00:00'
ORDER BY o.created_at DESC, o.id DESC
LIMIT 20;
```

多表关联推荐格式：

```sql
-- 推荐：JOIN 条件明确，字段带表别名
SELECT
    o.id,
    o.order_no,
    o.user_id,
    u.username,
    u.nickname,
    o.order_status,
    o.pay_amount,
    o.created_at
FROM order_info AS o
JOIN sys_user AS u ON o.user_id = u.id
WHERE o.order_status = 1
  AND o.deleted = 0
  AND u.deleted = 0
ORDER BY o.created_at DESC
LIMIT 20;
```

不推荐格式：

```sql
-- 不推荐：字段混乱、条件堆在一行、别名不清晰
select * from order_info o,sys_user u where o.user_id=u.id and o.order_status=1 and o.deleted=0 order by o.created_at desc limit 20;
```

SQL 编写风格建议：

| 规范项     | 建议                                     |
| ---------- | ---------------------------------------- |
| 关键字     | 使用大写，例如 `SELECT`、`FROM`、`WHERE` |
| 表名字段名 | 使用小写下划线                           |
| 字段列表   | 每行一个字段                             |
| 表别名     | 使用简短且有含义的别名                   |
| JOIN       | 使用显式 `JOIN`，不要使用隐式逗号连接    |
| 条件       | 每个条件单独一行                         |
| 排序       | 明确排序字段和方向                       |
| 分页       | 明确 `LIMIT` 大小                        |
| 注释       | 复杂 SQL 加业务说明                      |
| 时间范围   | 使用左闭右开                             |

SQL 注释示例：

```sql
-- 查询用户最近已支付订单，用于订单列表接口
SELECT
    o.id,
    o.order_no,
    o.pay_amount,
    o.created_at
FROM order_info AS o
WHERE o.user_id = 10001
  AND o.order_status = 1
  AND o.deleted = 0
ORDER BY o.created_at DESC, o.id DESC
LIMIT 20;
```

SQL 可读性建议：

1. SQL 关键字统一大写。
2. 字段列表换行展示。
3. 多表查询必须使用表别名。
4. 关联查询使用显式 `JOIN`。
5. 每个 `WHERE` 条件单独一行。
6. 复杂 SQL 应添加业务注释。
7. 时间范围使用左闭右开。
8. 不要写过长、难以评审的一行 SQL。
9. 动态 SQL 应避免分支过多导致最终 SQL 不可控。
10. 重要 SQL 上线前应保存执行计划和索引设计说明。

## Java 项目集成 MySQL

Java 项目集成 MySQL 通常有几种方式：原生 JDBC、Spring Boot 数据源、MyBatis、MyBatis-Plus、JPA，以及连接池方案如 HikariCP、Druid。实际项目中，Spring Boot + MyBatis-Plus + HikariCP 或 Druid 是较常见组合。

本节示例基于以下环境：

```text
JDK：17+
Spring Boot：3.x
MySQL：8.x
数据库驱动：mysql-connector-j 8.x 或 9.x
基础包名：io.github.atengk
```

示例表结构如下：

```sql
-- 用户表示例，用于 Java 项目集成 MySQL
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile),
    KEY idx_status_created (user_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### JDBC 连接

JDBC 是 Java 连接数据库的基础 API。它适合学习数据库连接原理、编写简单工具脚本或排查连接问题。生产项目中通常不直接使用原生 JDBC，而是通过 MyBatis、JPA、JdbcTemplate 或连接池框架访问数据库。

以下示例演示如何使用原生 JDBC 连接 MySQL 并查询用户。

文件位置：`src/main/java/io/github/atengk/mysql/jdbc/JdbcUserQueryDemo.java`

```java
package io.github.atengk.mysql.jdbc;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 原生 JDBC 查询用户示例
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
public class JdbcUserQueryDemo {

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/mall_user"
            + "?useUnicode=true"
            + "&characterEncoding=utf8"
            + "&serverTimezone=Asia/Shanghai"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true";

    private static final String USERNAME = "root";

    private static final String PASSWORD = "Root_123456";

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     */
    public void queryByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("用户名为空，跳过查询");
            return;
        }

        String sql = """
                SELECT
                    id,
                    username,
                    nickname,
                    mobile,
                    user_status,
                    created_at
                FROM sys_user
                WHERE username = ?
                  AND deleted = 0
                """;

        try (
                Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    BigInteger id = BigInteger.valueOf(resultSet.getLong("id"));
                    String dbUsername = resultSet.getString("username");
                    String nickname = resultSet.getString("nickname");
                    String mobile = resultSet.getString("mobile");
                    Integer userStatus = resultSet.getInt("user_status");

                    log.info("查询到用户，id={}，username={}，nickname={}，mobile={}，userStatus={}",
                            id, dbUsername, nickname, mobile, userStatus);
                }
            }
        } catch (Exception e) {
            log.error("JDBC 查询用户失败，username={}", username, e);
        }
    }

    /**
     * 启动测试
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        JdbcUserQueryDemo demo = new JdbcUserQueryDemo();
        demo.queryByUsername("admin");
    }
}
```

JDBC 连接 URL 常用参数：

```properties
# MySQL 连接地址
jdbc:mysql://127.0.0.1:3306/mall_user

# 使用 Unicode
useUnicode=true

# 字符编码
characterEncoding=utf8

# 服务端时区
serverTimezone=Asia/Shanghai

# 是否启用 SSL，生产跨网络访问建议配置 SSL
useSSL=false

# MySQL 8 公钥检索参数，本地开发常见
allowPublicKeyRetrieval=true
```

JDBC 使用建议：

1. 必须使用 `PreparedStatement`，不要拼接用户输入。
2. 连接、语句和结果集必须关闭，推荐使用 `try-with-resources`。
3. 原生 JDBC 不适合复杂业务项目，生产项目推荐连接池和 ORM 框架。
4. 密码不应硬编码在代码中，应放在配置中心、环境变量或密钥系统中。
5. JDBC URL 中要显式配置字符集和时区。

### Spring Boot 数据源配置

Spring Boot 项目通常通过 `spring.datasource` 配置 MySQL 数据源。默认情况下，Spring Boot 3 使用 HikariCP 作为连接池。如果项目引入 MyBatis、MyBatis-Plus、JPA 等框架，它们都会复用 Spring Boot 管理的数据源。

Maven 依赖示例：

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供 REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot JDBC，提供 DataSource 和 JdbcTemplate 基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类，常用于字符串、集合、日期、加密等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok，减少样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

Spring Boot 数据源配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: mysql-integration-demo

  datasource:
    # MySQL JDBC 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

    # MySQL 连接地址，建议显式配置字符集和时区
    url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

    # 数据库账号，生产环境建议通过环境变量或配置中心注入
    username: root

    # 数据库密码，生产环境不要明文提交到代码仓库
    password: Root_123456

    hikari:
      # 连接池名称，便于日志和监控识别
      pool-name: mysql-hikari-pool

      # 最大连接数，需结合应用实例数量和 MySQL max_connections 设置
      maximum-pool-size: 20

      # 最小空闲连接数
      minimum-idle: 5

      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000

      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000

      # 连接最大生命周期，单位毫秒，应小于数据库或网络层连接最大存活时间
      max-lifetime: 1800000
```

使用 `JdbcTemplate` 查询数据库。

文件位置：`src/main/java/io/github/atengk/mysql/jdbc/UserJdbcRepository.java`

```java
package io.github.atengk.mysql.jdbc;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * JdbcTemplate 用户查询仓储
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 根据用户名查询用户列表
     *
     * @param username 用户名
     * @return 用户数据列表
     */
    public List<Map<String, Object>> listByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("用户名为空，返回空列表");
            return List.of();
        }

        String sql = """
                SELECT
                    id,
                    username,
                    nickname,
                    mobile,
                    email,
                    user_status,
                    created_at
                FROM sys_user
                WHERE username = ?
                  AND deleted = 0
                """;

        log.info("使用 JdbcTemplate 查询用户，username={}", username);
        return jdbcTemplate.queryForList(sql, username);
    }
}
```

Spring Boot 数据源配置建议：

1. 数据库账号和密码不要提交到代码仓库。
2. 生产环境建议通过环境变量、配置中心或密钥系统注入。
3. JDBC URL 中显式设置字符集和时区。
4. 连接池大小要结合应用实例数量统一评估。
5. 业务项目建议统一封装数据访问层，不要在 Controller 中直接写 SQL。

### MyBatis 集成

MyBatis 适合需要手写 SQL、复杂查询较多、对 SQL 可控性要求较高的项目。它通过 Mapper 接口和 XML SQL 映射完成数据库访问。

Maven 依赖：

```xml
<dependencies>
    <!-- MyBatis Spring Boot Starter，集成 MyBatis 和 Spring Boot -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.4</version>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok 简化实体类和日志代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

MyBatis 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: Root_123456

mybatis:
  # Mapper XML 文件位置
  mapper-locations: classpath*:mapper/**/*.xml

  # 实体类别名包
  type-aliases-package: io.github.atengk.mysql.mybatis.entity

  configuration:
    # 开启下划线转驼峰，例如 user_status -> userStatus
    map-underscore-to-camel-case: true

    # SQL 执行日志，开发环境可开启，生产环境谨慎
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

实体类如下。

文件位置：`src/main/java/io/github/atengk/mysql/mybatis/entity/SysUser.java`

```java
package io.github.atengk.mysql.mybatis.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class SysUser {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private String email;

    private Integer userStatus;

    private Integer deleted;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

Mapper 接口如下。

文件位置：`src/main/java/io/github/atengk/mysql/mybatis/mapper/SysUserMapper.java`

```java
package io.github.atengk.mysql.mybatis.mapper;

import io.github.atengk.mysql.mybatis.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 MyBatis Mapper
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据状态查询用户列表
     *
     * @param userStatus 用户状态
     * @param limit      查询数量
     * @return 用户列表
     */
    List<SysUser> listByStatus(@Param("userStatus") Integer userStatus, @Param("limit") Integer limit);
}
```

Mapper XML 如下。

文件位置：`src/main/resources/mapper/SysUserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.mysql.mybatis.mapper.SysUserMapper">

    <!-- 用户基础字段，避免 SELECT * -->
    <sql id="BaseColumnList">
        id,
        username,
        nickname,
        mobile,
        email,
        user_status,
        deleted,
        version,
        created_at,
        updated_at
    </sql>

    <!-- 根据用户名查询用户，使用 #{username} 参数绑定防止 SQL 注入 -->
    <select id="selectByUsername" resultType="io.github.atengk.mysql.mybatis.entity.SysUser">
        SELECT
        <include refid="BaseColumnList"/>
        FROM sys_user
        WHERE username = #{username}
          AND deleted = 0
        LIMIT 1
    </select>

    <!-- 根据状态查询用户列表，限制返回数量 -->
    <select id="listByStatus" resultType="io.github.atengk.mysql.mybatis.entity.SysUser">
        SELECT
        <include refid="BaseColumnList"/>
        FROM sys_user
        WHERE user_status = #{userStatus}
          AND deleted = 0
        ORDER BY created_at DESC, id DESC
        LIMIT #{limit}
    </select>

</mapper>
```

Service 示例：

文件位置：`src/main/java/io/github/atengk/mysql/mybatis/service/SysUserQueryService.java`

```java
package io.github.atengk.mysql.mybatis.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mysql.mybatis.entity.SysUser;
import io.github.atengk.mysql.mybatis.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户查询服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserQueryService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public SysUser getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("用户名为空，无法查询用户");
            return null;
        }

        log.info("查询用户信息，username={}", username);
        return sysUserMapper.selectByUsername(username);
    }

    /**
     * 查询启用用户列表
     *
     * @return 用户列表
     */
    public List<SysUser> listEnabledUsers() {
        log.info("查询启用用户列表");
        return sysUserMapper.listByStatus(1, 100);
    }
}
```

MyBatis 集成建议：

1. XML 中禁止使用 `SELECT *`。
2. 用户输入必须使用 `#{}`，不要使用 `${}` 拼接。
3. 动态排序字段必须通过白名单校验。
4. Mapper 方法参数建议使用 `@Param` 明确命名。
5. 大查询要限制返回条数。
6. 复杂 SQL 应保留必要注释，便于后期维护。

### MyBatis Plus 集成

MyBatis-Plus 在 MyBatis 基础上提供通用 CRUD、条件构造器、分页插件、逻辑删除、乐观锁、自动填充等能力。普通增删改查场景下，它可以减少大量重复代码。

Maven 依赖：

```xml
<dependencies>
    <!-- MyBatis-Plus Spring Boot 3 Starter -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.12</version>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok 简化实体和日志代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

MyBatis-Plus 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: Root_123456

mybatis-plus:
  configuration:
    # 开启下划线转驼峰
    map-underscore-to-camel-case: true

    # 开发环境可以打印 SQL，生产环境不建议使用 stdout
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # 主键策略，自增主键使用 auto
      id-type: auto

      # 逻辑删除字段
      logic-delete-field: deleted

      # 删除值
      logic-delete-value: 1

      # 未删除值
      logic-not-delete-value: 0
```

MyBatis-Plus 插件配置如下。

文件位置：`src/main/java/io/github/atengk/mysql/mybatisplus/config/MybatisPlusConfig.java`

```java
package io.github.atengk.mysql.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置分页和乐观锁插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件，指定 MySQL 数据库类型
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 乐观锁插件，配合 @Version 字段使用
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
```

实体类如下。

文件位置：`src/main/java/io/github/atengk/mysql/mybatisplus/entity/SysUser.java`

```java
package io.github.atengk.mysql.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private String email;

    private Integer userStatus;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Mapper 接口如下。

文件位置：`src/main/java/io/github/atengk/mysql/mybatisplus/mapper/SysUserMapper.java`

```java
package io.github.atengk.mysql.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.mysql.mybatisplus.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 MyBatis-Plus Mapper
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

Service 示例：

文件位置：`src/main/java/io/github/atengk/mysql/mybatisplus/service/SysUserService.java`

```java
package io.github.atengk.mysql.mybatisplus.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.mysql.mybatisplus.entity.SysUser;
import io.github.atengk.mysql.mybatisplus.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统用户服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public SysUser getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("用户名为空，无法查询用户");
            return null;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("LIMIT 1");

        log.info("MyBatis-Plus 查询用户，username={}", username);
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * 分页查询启用用户
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 用户分页结果
     */
    public Page<SysUser> pageEnabledUsers(long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserStatus, 1)
                .eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreatedAt)
                .orderByDesc(SysUser::getId);

        log.info("分页查询启用用户，pageNum={}，pageSize={}", safePageNum, safePageSize);
        return sysUserMapper.selectPage(Page.of(safePageNum, safePageSize), wrapper);
    }
}
```

MyBatis-Plus 使用建议：

1. 简单 CRUD 优先使用 MyBatis-Plus。
2. 复杂 SQL 仍可使用 XML 编写。
3. 条件构造器优先使用 Lambda 形式，避免字段名硬编码。
4. 分页查询要限制最大页大小。
5. 逻辑删除和乐观锁字段要与数据库字段一致。
6. `last()` 会直接拼接 SQL，必须避免拼接用户输入。

### JPA 集成

JPA 适合领域模型较清晰、CRUD 较多、复杂 SQL 较少的项目。Spring Boot 通常通过 Spring Data JPA 集成 Hibernate。对于强 SQL 控制、复杂报表和高性能查询较多的项目，MyBatis 或 MyBatis-Plus 通常更直观。

Maven 依赖：

```xml
<dependencies>
    <!-- Spring Data JPA，默认使用 Hibernate 实现 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok 简化代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

JPA 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: Root_123456

  jpa:
    hibernate:
      # 生产环境不建议使用 update 自动变更表结构，应由数据库变更脚本管理
      ddl-auto: none

    properties:
      hibernate:
        # 格式化 SQL，开发环境便于阅读
        format_sql: true

        # 指定 MySQL 方言
        dialect: org.hibernate.dialect.MySQLDialect

    # 开发环境可以开启 SQL 输出，生产环境谨慎
    show-sql: true
```

JPA 实体如下。

文件位置：`src/main/java/io/github/atengk/mysql/jpa/entity/SysUserJpaEntity.java`

```java
package io.github.atengk.mysql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户 JPA 实体
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "user_status", nullable = false)
    private Integer userStatus;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

Repository 如下。

文件位置：`src/main/java/io/github/atengk/mysql/jpa/repository/SysUserJpaRepository.java`

```java
package io.github.atengk.mysql.jpa.repository;

import io.github.atengk.mysql.jpa.entity.SysUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 系统用户 JPA Repository
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface SysUserJpaRepository extends JpaRepository<SysUserJpaEntity, Long> {

    /**
     * 根据用户名和删除标识查询用户
     *
     * @param username 用户名
     * @param deleted  删除标识
     * @return 用户信息
     */
    Optional<SysUserJpaEntity> findByUsernameAndDeleted(String username, Integer deleted);

    /**
     * 根据用户状态和删除标识查询用户
     *
     * @param userStatus 用户状态
     * @param deleted    删除标识
     * @return 用户列表
     */
    List<SysUserJpaEntity> findTop100ByUserStatusAndDeletedOrderByCreatedAtDescIdDesc(Integer userStatus, Integer deleted);
}
```

JPA Service 示例：

文件位置：`src/main/java/io/github/atengk/mysql/jpa/service/SysUserJpaService.java`

```java
package io.github.atengk.mysql.jpa.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mysql.jpa.entity.SysUserJpaEntity;
import io.github.atengk.mysql.jpa.repository.SysUserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户 JPA 服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserJpaService {

    private final SysUserJpaRepository sysUserJpaRepository;

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public SysUserJpaEntity getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("用户名为空，无法查询用户");
            return null;
        }

        log.info("JPA 查询用户，username={}", username);
        return sysUserJpaRepository.findByUsernameAndDeleted(username, 0).orElse(null);
    }

    /**
     * 查询启用用户
     *
     * @return 用户列表
     */
    public List<SysUserJpaEntity> listEnabledUsers() {
        log.info("JPA 查询启用用户列表");
        return sysUserJpaRepository.findTop100ByUserStatusAndDeletedOrderByCreatedAtDescIdDesc(1, 0);
    }
}
```

JPA 使用建议：

1. 生产环境不建议使用 `ddl-auto=update` 自动改表。
2. 复杂 SQL 较多的系统慎用 JPA 自动查询方法堆叠。
3. JPA 实体字段应明确配置表名、字段名和长度。
4. 大批量写入和复杂报表查询通常不适合 JPA 自动处理。
5. 对 SQL 可控性要求高的系统，MyBatis 更直接。

### 连接池配置

连接池用于复用数据库连接，避免频繁创建和销毁连接。连接池配置不合理会导致连接不足、连接打满、数据库压力过高、应用等待连接超时等问题。

常见连接池参数：

| 参数         | 说明                               |
| ------------ | ---------------------------------- |
| 最大连接数   | 单个应用实例最多持有多少数据库连接 |
| 最小空闲连接 | 空闲时保留多少连接                 |
| 连接超时     | 获取连接等待多久后失败             |
| 空闲超时     | 空闲连接多久后释放                 |
| 最大生命周期 | 连接最长存活时间                   |
| 连接检测     | 判断连接是否可用                   |
| 连接池名称   | 便于日志和监控识别                 |

连接池数量评估：

```text
总连接数 = 应用实例数 × 每个实例最大连接池数量

示例：
应用实例数 = 10
每个实例 maximum-pool-size = 30

理论最大连接数 = 10 × 30 = 300
```

MySQL 连接数检查：

```sql
-- 查看 MySQL 最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看历史最大使用连接数
SHOW GLOBAL STATUS LIKE 'Max_used_connections';

-- 查看当前连接数
SHOW GLOBAL STATUS LIKE 'Threads_connected';

-- 查看当前活跃线程数
SHOW GLOBAL STATUS LIKE 'Threads_running';
```

连接池配置建议：

1. 不要每个应用实例都配置过大的连接池。
2. 多个应用的连接池总量要小于 MySQL 可承载连接数。
3. 连接池耗尽时，优先排查慢 SQL、锁等待和连接泄漏。
4. `maximum-pool-size` 应结合接口并发、SQL 耗时和实例数量设置。
5. 数据库不是连接越多越快，过多连接会增加调度和内存成本。
6. 核心应用应接入连接池监控。

### Druid 配置

Druid 是阿里开源连接池，提供连接池、监控、SQL 防火墙、慢 SQL 统计等能力。它功能较多，适合需要 SQL 监控面板或统一连接池监控的项目。

Maven 依赖：

```xml
<dependencies>
    <!-- Druid Spring Boot 3 Starter -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-3-starter</artifactId>
        <version>1.2.24</version>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

Druid 数据源配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource

    druid:
      # MySQL 驱动
      driver-class-name: com.mysql.cj.jdbc.Driver

      # 数据库连接地址
      url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

      # 数据库账号
      username: root

      # 数据库密码，生产环境应使用密钥系统或环境变量
      password: Root_123456

      # 初始化连接数
      initial-size: 5

      # 最小空闲连接数
      min-idle: 5

      # 最大连接数
      max-active: 30

      # 获取连接最大等待时间，单位毫秒
      max-wait: 30000

      # 检测空闲连接的间隔，单位毫秒
      time-between-eviction-runs-millis: 60000

      # 连接最小空闲时间，单位毫秒
      min-evictable-idle-time-millis: 300000

      # 连接检测 SQL
      validation-query: SELECT 1

      # 空闲时检测连接
      test-while-idle: true

      # 获取连接时不检测，减少性能损耗
      test-on-borrow: false

      # 归还连接时不检测，减少性能损耗
      test-on-return: false

      # 开启监控统计和防火墙
      filters: stat,wall,slf4j

      stat-view-servlet:
        # 开启 Druid 监控页面
        enabled: true

        # 监控页面路径
        url-pattern: /druid/*

        # 登录用户名
        login-username: admin

        # 登录密码，生产环境必须修改
        login-password: Admin_123456

      web-stat-filter:
        # 开启 Web 监控过滤器
        enabled: true

        # 过滤路径
        url-pattern: /*

        # 排除静态资源和监控页面
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
```

Druid 使用建议：

1. 生产环境必须保护 `/druid/*` 监控页面。
2. 不要使用默认账号密码。
3. `wall` 防火墙规则要结合业务 SQL 测试，避免误拦截。
4. `max-active` 要结合应用实例数量和 MySQL 连接数统一评估。
5. 如果只需要高性能连接池，HikariCP 更简洁。
6. 如果需要 SQL 监控和管理页面，Druid 更方便。

### HikariCP 配置

HikariCP 是 Spring Boot 默认连接池，性能好、配置简单、稳定性高。大多数 Spring Boot 项目直接使用 HikariCP 即可。

HikariCP 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: Root_123456

    hikari:
      # 连接池名称
      pool-name: mall-user-hikari-pool

      # 最大连接数
      maximum-pool-size: 30

      # 最小空闲连接数
      minimum-idle: 5

      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000

      # 空闲连接超时时间，单位毫秒
      idle-timeout: 600000

      # 连接最大生命周期，单位毫秒，建议小于数据库和网络设备连接超时时间
      max-lifetime: 1800000

      # 连接检测超时时间，单位毫秒
      validation-timeout: 5000

      # 自动提交，通常由事务管理控制
      auto-commit: true
```

HikariCP 监控建议结合 Spring Boot Actuator：

```xml
<dependencies>
    <!-- Spring Boot Actuator，用于暴露健康检查和指标 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Prometheus，用于导出 Prometheus 指标 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

Actuator 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查、指标和 Prometheus 端点
        include: health,metrics,prometheus

  endpoint:
    health:
      # 显示健康检查详情
      show-details: always
```

HikariCP 使用建议：

1. Spring Boot 默认使用 HikariCP，不需要额外引入连接池依赖。
2. 大多数项目优先使用 HikariCP。
3. `maximum-pool-size` 不应盲目调大。
4. `max-lifetime` 应小于数据库或网关的连接最大生命周期。
5. 接入 Actuator 和 Prometheus 监控连接池状态。
6. 连接池耗尽时优先排查慢 SQL 和事务未释放。

### 多数据源配置

多数据源用于一个项目连接多个数据库，例如业务库和日志库、主库和从库、订单库和用户库等。多数据源会增加事务、Mapper 扫描、配置和排查复杂度，应在确有必要时使用。

以下示例配置两个数据源：`userDataSource` 和 `orderDataSource`。

Maven 依赖：

```xml
<dependencies>
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.4</version>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- HikariCP 通常由 Spring Boot 默认提供，这里无需单独声明 -->
</dependencies>
```

多数据源配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    user:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: Root_123456
      maximum-pool-size: 20
      minimum-idle: 5

    order:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://127.0.0.1:3306/mall_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: Root_123456
      maximum-pool-size: 20
      minimum-idle: 5
```

用户库数据源配置如下。

文件位置：`src/main/java/io/github/atengk/mysql/multidatasource/config/UserDataSourceConfig.java`

```java
package io.github.atengk.mysql.multidatasource.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 用户库数据源配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@MapperScan(
        basePackages = "io.github.atengk.mysql.multidatasource.user.mapper",
        sqlSessionFactoryRef = "userSqlSessionFactory"
)
public class UserDataSourceConfig {

    /**
     * 用户库数据源
     *
     * @return 用户库数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSource userDataSource() {
        return new HikariDataSource();
    }

    /**
     * 用户库 SqlSessionFactory
     *
     * @param userDataSource 用户库数据源
     * @return 用户库 SqlSessionFactory
     * @throws Exception 创建异常
     */
    @Bean
    public SqlSessionFactory userSqlSessionFactory(DataSource userDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(userDataSource);
        return factoryBean.getObject();
    }
}
```

订单库数据源配置如下。

文件位置：`src/main/java/io/github/atengk/mysql/multidatasource/config/OrderDataSourceConfig.java`

```java
package io.github.atengk.mysql.multidatasource.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 订单库数据源配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@MapperScan(
        basePackages = "io.github.atengk.mysql.multidatasource.order.mapper",
        sqlSessionFactoryRef = "orderSqlSessionFactory"
)
public class OrderDataSourceConfig {

    /**
     * 订单库数据源
     *
     * @return 订单库数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.order")
    public DataSource orderDataSource() {
        return new HikariDataSource();
    }

    /**
     * 订单库 SqlSessionFactory
     *
     * @param orderDataSource 订单库数据源
     * @return 订单库 SqlSessionFactory
     * @throws Exception 创建异常
     */
    @Bean
    public SqlSessionFactory orderSqlSessionFactory(DataSource orderDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(orderDataSource);
        return factoryBean.getObject();
    }
}
```

多数据源使用建议：

1. 不同数据源的 Mapper 包必须隔离。
2. 每个数据源要有独立的 `SqlSessionFactory`。
3. 多数据源事务复杂，跨库事务不要默认认为能自动保证一致性。
4. 读写分离场景可以考虑成熟框架或代理，而不是手写复杂切换逻辑。
5. 多数据源项目必须有清晰命名，避免误操作到错误数据库。

### 事务注解配置

Spring 中常用 `@Transactional` 管理事务。它适合保证一组数据库操作要么全部成功，要么全部回滚。事务使用不当会导致事务不生效、长事务、锁等待、大事务和数据不一致。

基础事务示例：

文件位置：`src/main/java/io/github/atengk/mysql/transaction/service/UserRegisterService.java`

```java
package io.github.atengk.mysql.transaction.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mysql.mybatisplus.entity.SysUser;
import io.github.atengk.mysql.mybatisplus.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户注册事务服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegisterService {

    private final SysUserMapper sysUserMapper;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param nickname 用户昵称
     * @param mobile   手机号
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long registerUser(String username, String nickname, String mobile) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setMobile(mobile);
        user.setUserStatus(1);
        user.setDeleted(0);
        user.setVersion(0);

        int rows = sysUserMapper.insert(user);
        if (rows != 1) {
            throw new IllegalStateException("用户注册失败");
        }

        log.info("用户注册成功，username={}，userId={}", username, user.getId());
        return user.getId();
    }
}
```

事务回滚规则示例：

```java
package io.github.atengk.mysql.transaction.service;

import io.github.atengk.mysql.mybatisplus.entity.SysUser;
import io.github.atengk.mysql.mybatisplus.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户状态事务服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final SysUserMapper sysUserMapper;

    /**
     * 禁用用户
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setUserStatus(0);

        int rows = sysUserMapper.updateById(updateUser);
        if (rows != 1) {
            throw new IllegalStateException("禁用用户失败，用户不存在或状态异常");
        }

        log.info("禁用用户成功，userId={}", userId);
    }
}
```

事务注解常用属性：

| 属性          | 说明                 |
| ------------- | -------------------- |
| `rollbackFor` | 指定哪些异常触发回滚 |
| `propagation` | 事务传播行为         |
| `isolation`   | 事务隔离级别         |
| `timeout`     | 事务超时时间         |
| `readOnly`    | 是否只读事务         |

只读事务示例：

```java
package io.github.atengk.mysql.transaction.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mysql.mybatisplus.entity.SysUser;
import io.github.atengk.mysql.mybatisplus.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户只读事务服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class UserReadOnlyService {

    private final SysUserService sysUserService;

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public SysUser getUser(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return sysUserService.getByUsername(username);
    }
}
```

事务不生效的常见情况：

| 场景                 | 原因                         |
| -------------------- | ---------------------------- |
| 同类内部方法调用     | 没有经过 Spring 代理         |
| 方法不是 `public`    | 默认代理机制可能不生效       |
| 异常被捕获未抛出     | Spring 感知不到异常          |
| 默认只回滚运行时异常 | 受检异常需配置 `rollbackFor` |
| 对象不是 Spring Bean | 没有被事务代理管理           |
| 多数据源事务未配置   | 事务管理器不匹配             |

不推荐写法：

```java
package io.github.atengk.mysql.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务不生效示例
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class BadTransactionService {

    /**
     * 外部方法，直接调用同类内部事务方法时事务可能不生效
     */
    public void outerMethod() {
        innerMethod();
    }

    /**
     * 内部方法事务注解可能不生效
     */
    @Transactional(rollbackFor = Exception.class)
    public void innerMethod() {
        log.info("同类内部调用事务方法，可能不会经过 Spring 代理");
    }
}
```

事务使用建议：

1. 事务方法建议定义为 `public`。
2. 写操作事务建议配置 `rollbackFor = Exception.class`。
3. 不要在事务中执行 HTTP、RPC、MQ、文件上传等外部操作。
4. 事务内 SQL 必须尽量命中索引，减少锁范围。
5. 大批量任务不要放在一个大事务中。
6. 查询接口可以使用 `@Transactional(readOnly = true)`。
7. 多数据源项目要明确事务管理器，避免事务作用到错误数据源。
8. 同类内部方法调用事务可能不生效，应通过独立 Service 编排。

## 数据库连接池

数据库连接池用于复用 MySQL 连接，避免应用每次访问数据库都创建和销毁连接。连接池配置直接影响接口吞吐、数据库连接数、慢请求、连接等待和故障恢复能力。项目中常见连接池包括 HikariCP 和 Druid，其中 Spring Boot 默认使用 HikariCP。

连接池优化不能只看单个应用实例，还要计算所有应用实例的连接总量：

```text
数据库总连接压力 = 应用实例数量 × 每个实例最大连接池数量

示例：
应用实例数量 = 10
每个实例 maximum-pool-size = 30
理论最大连接数 = 10 × 30 = 300
```

### 最大连接数

最大连接数表示单个应用实例最多可以同时持有多少个数据库连接。连接数太小会导致应用等待连接，连接数太大则会增加 MySQL 线程调度、内存和锁竞争压力。

查看 MySQL 最大连接数：

```sql
-- 查看 MySQL 允许的最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看历史最大使用连接数
SHOW GLOBAL STATUS LIKE 'Max_used_connections';

-- 查看当前连接数
SHOW GLOBAL STATUS LIKE 'Threads_connected';

-- 查看当前活跃线程数
SHOW GLOBAL STATUS LIKE 'Threads_running';
```

HikariCP 最大连接数配置：

```yaml
spring:
  datasource:
    hikari:
      # 单个应用实例最大连接数
      maximum-pool-size: 30

      # 最小空闲连接数
      minimum-idle: 5

      # 连接池名称，便于日志和监控识别
      pool-name: mall-user-hikari-pool
```

Druid 最大连接数配置：

```yaml
spring:
  datasource:
    druid:
      # 初始化连接数
      initial-size: 5

      # 最小空闲连接数
      min-idle: 5

      # 最大活跃连接数
      max-active: 30
```

最大连接数配置建议：

1. 不要把 `maximum-pool-size` 或 `max-active` 盲目调大。
2. 多个应用实例的连接池总和应小于 MySQL `max_connections`。
3. `Threads_running` 长期较高时，通常说明数据库执行压力大，而不是连接数不够。
4. 如果连接池频繁耗尽，应优先排查慢 SQL、锁等待、外部接口阻塞和事务未释放。
5. 普通业务系统单实例连接池常见范围是 10 到 50，最终以压测和监控为准。

### 最小空闲连接

最小空闲连接表示连接池在空闲状态下保留的连接数量。它可以减少突发请求时创建连接的成本，但配置过高会长期占用数据库连接资源。

HikariCP 配置：

```yaml
spring:
  datasource:
    hikari:
      # 最小空闲连接数，流量稳定的服务可适当设置
      minimum-idle: 5

      # 最大连接数
      maximum-pool-size: 30
```

Druid 配置：

```yaml
spring:
  datasource:
    druid:
      # 初始化连接数
      initial-size: 5

      # 最小空闲连接数
      min-idle: 5

      # 最大活跃连接数
      max-active: 30
```

最小空闲连接建议：

1. 流量稳定的核心服务可以设置适当最小空闲连接。
2. 低频服务、后台任务服务不建议保留过多空闲连接。
3. `minimum-idle` 通常不应等于 `maximum-pool-size`，除非服务高峰非常稳定。
4. 多实例部署时，空闲连接也会叠加占用 MySQL 连接资源。
5. 连接池空闲连接过多时，应结合 MySQL 当前连接数和应用流量调整。

### 连接超时时间

连接超时时间表示应用从连接池获取连接时最多等待多久。如果超过该时间仍无法获取连接，连接池会抛出异常。该参数可以防止请求长时间阻塞。

HikariCP 配置：

```yaml
spring:
  datasource:
    hikari:
      # 获取连接最大等待时间，单位毫秒
      connection-timeout: 30000
```

Druid 配置：

```yaml
spring:
  datasource:
    druid:
      # 获取连接最大等待时间，单位毫秒
      max-wait: 30000
```

连接超时异常常见表现：

```text
HikariCP 常见异常：
Connection is not available, request timed out after 30000ms.

Druid 常见异常：
wait millis 30000, active 30, maxActive 30
```

连接超时时间建议：

1. 常见配置为 10 秒到 30 秒。
2. 超时时间过长会拖慢接口失败响应。
3. 超时时间过短可能在瞬时流量波动时误失败。
4. 出现获取连接超时，不要只调大连接池，应先排查连接为什么没有释放。
5. 应用接口超时时间应大于连接池获取连接超时时间，否则问题更难定位。

### 空闲连接回收

空闲连接回收用于释放长期不用的连接，避免数据库连接资源被长期占用。它需要与数据库端 `wait_timeout`、网络设备连接超时和连接池 `max-lifetime` 配合设置。

查看 MySQL 空闲超时：

```sql
-- 查看 MySQL 普通连接空闲超时时间
SHOW VARIABLES LIKE 'wait_timeout';

-- 查看交互式连接空闲超时时间
SHOW VARIABLES LIKE 'interactive_timeout';
```

HikariCP 空闲回收配置：

```yaml
spring:
  datasource:
    hikari:
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000

      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000
```

Druid 空闲回收配置：

```yaml
spring:
  datasource:
    druid:
      # 检测空闲连接的间隔，单位毫秒
      time-between-eviction-runs-millis: 60000

      # 连接最小空闲时间，单位毫秒
      min-evictable-idle-time-millis: 300000

      # 连接最大空闲时间，单位毫秒
      max-evictable-idle-time-millis: 900000
```

空闲连接回收建议：

1. `max-lifetime` 应小于 MySQL、负载均衡或防火墙的连接最大存活时间。
2. 空闲连接回收不能太激进，否则会频繁创建连接。
3. 连接池应主动回收旧连接，避免使用被网络设备断开的半开连接。
4. 低流量服务应适当降低最小空闲连接。
5. 出现偶发连接断开时，应检查 `wait_timeout`、`max-lifetime` 和网络设备超时。

### 连接有效性检测

连接有效性检测用于判断连接池中的连接是否仍然可用。检测配置不合理时，可能出现应用拿到失效连接，或者每次获取连接都检测导致性能下降。

HikariCP 通常不需要显式配置 `connection-test-query`，它会优先使用 JDBC4 的 `isValid()`。如果需要手动配置：

```yaml
spring:
  datasource:
    hikari:
      # 连接检测超时时间，单位毫秒
      validation-timeout: 5000

      # 一般不需要配置，只有特殊驱动或网络环境下再设置
      connection-test-query: SELECT 1
```

Druid 检测配置：

```yaml
spring:
  datasource:
    druid:
      # 连接检测 SQL
      validation-query: SELECT 1

      # 空闲时检测连接，推荐开启
      test-while-idle: true

      # 获取连接时检测，会增加每次获取连接成本，通常不建议开启
      test-on-borrow: false

      # 归还连接时检测，通常不建议开启
      test-on-return: false
```

连接有效性检测建议：

1. HikariCP 默认机制通常足够。
2. Druid 推荐开启 `test-while-idle`。
3. 不建议长期打开 `test-on-borrow`，否则每次获取连接都会多执行一次检测 SQL。
4. 连接检测 SQL 应简单，例如 `SELECT 1`。
5. 如果频繁出现失效连接，应优先检查网络超时、MySQL 空闲超时和连接池生命周期配置。

### 慢 SQL 监控

连接池层可以辅助发现慢 SQL，但慢 SQL 的根因仍需要结合 MySQL 慢查询日志、执行计划和业务调用链分析。Druid 提供较方便的 SQL 监控能力，HikariCP 通常配合日志、APM、Micrometer 和数据库慢查询日志使用。

Druid 慢 SQL 监控配置：

```yaml
spring:
  datasource:
    druid:
      # 开启统计、SQL 防火墙和日志输出
      filters: stat,wall,slf4j

      filter:
        stat:
          # 开启慢 SQL 记录
          log-slow-sql: true

          # 慢 SQL 阈值，单位毫秒
          slow-sql-millis: 1000

          # 合并相似 SQL，便于统计
          merge-sql: true
```

MySQL 慢查询日志配置：

```yaml
# application.yml 中不能直接配置 MySQL 服务端慢查询，这里应在 my.cnf 配置
[mysqld]
# 开启慢查询日志
slow_query_log=ON

# 慢查询日志路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 慢查询阈值，单位秒
long_query_time=1
```

慢 SQL 监控建议：

1. 应用层监控用于定位接口来源。
2. MySQL 慢查询日志用于定位 SQL 本身。
3. Druid 监控页生产环境必须加访问控制。
4. HikariCP 建议结合 Micrometer、Prometheus 和 APM。
5. 慢 SQL 不能只看平均耗时，还要看执行次数、总耗时和 95 分位耗时。

### 连接泄漏排查

连接泄漏是指应用获取连接后没有正常归还连接池，最终导致连接池耗尽。常见原因包括手写 JDBC 未关闭连接、事务阻塞、线程池任务卡死、外部调用放在事务中、流式查询未关闭等。

HikariCP 连接泄漏检测配置：

```yaml
spring:
  datasource:
    hikari:
      # 连接泄漏检测阈值，单位毫秒
      # 连接被占用超过该时间未归还时输出告警日志
      leak-detection-threshold: 60000
```

Druid 连接泄漏回收配置：

```yaml
spring:
  datasource:
    druid:
      # 开启连接泄漏回收，生产环境需谨慎评估
      remove-abandoned: true

      # 连接占用超过该时间认为可能泄漏，单位秒
      remove-abandoned-timeout: 180

      # 记录泄漏连接日志
      log-abandoned: true
```

查看当前连接：

```sql
-- 查看当前连接和执行状态
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
ORDER BY time DESC;
```

查看长事务：

```sql
-- 查看当前长事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

连接泄漏常见原因：

| 原因             | 说明                          |
| ---------------- | ----------------------------- |
| 原生 JDBC 未关闭 | 没有使用 `try-with-resources` |
| 事务时间过长     | 事务中包含外部调用或复杂处理  |
| 流式查询未关闭   | ResultSet 或 Cursor 未关闭    |
| 线程阻塞         | 业务线程持有连接后卡住        |
| 锁等待           | SQL 等待锁导致连接长期占用    |
| 慢 SQL           | SQL 执行时间过长，占用连接    |
| 连接池过小       | 实际不是泄漏，而是连接不够用  |

连接泄漏排查建议：

1. 打开连接池泄漏检测日志。
2. 查看应用堆栈，定位哪个业务方法长期持有连接。
3. 查看 MySQL `PROCESSLIST` 和 `INNODB_TRX`。
4. 检查事务中是否有 HTTP、RPC、MQ、文件操作。
5. 手写 JDBC 必须使用 `try-with-resources`。
6. 流式查询必须确保游标和结果集关闭。
7. 不要用调大连接池掩盖连接泄漏。

### 连接池参数优化

连接池参数优化应根据业务并发、SQL 耗时、数据库能力和应用实例数量综合调整。优化前应先采集指标，优化后应通过压测和线上监控验证。

HikariCP 推荐基础配置：

```yaml
spring:
  datasource:
    hikari:
      # 连接池名称
      pool-name: mall-user-hikari-pool

      # 最大连接数，结合实例数量和数据库承载能力设置
      maximum-pool-size: 30

      # 最小空闲连接数
      minimum-idle: 5

      # 获取连接超时时间
      connection-timeout: 30000

      # 空闲连接回收时间
      idle-timeout: 600000

      # 连接最大生命周期
      max-lifetime: 1800000

      # 连接检测超时时间
      validation-timeout: 5000

      # 连接泄漏检测，排查阶段开启
      leak-detection-threshold: 60000
```

Druid 推荐基础配置：

```yaml
spring:
  datasource:
    druid:
      # 初始化连接数
      initial-size: 5

      # 最小空闲连接
      min-idle: 5

      # 最大活跃连接
      max-active: 30

      # 获取连接等待超时
      max-wait: 30000

      # 空闲连接检测周期
      time-between-eviction-runs-millis: 60000

      # 最小空闲存活时间
      min-evictable-idle-time-millis: 300000

      # 连接有效性检测 SQL
      validation-query: SELECT 1

      # 空闲时检测
      test-while-idle: true

      # 获取连接时不检测
      test-on-borrow: false

      # 归还连接时不检测
      test-on-return: false

      # 统计和日志
      filters: stat,slf4j
```

连接池优化检查项：

| 检查项       | 说明                                              |
| ------------ | ------------------------------------------------- |
| 当前连接数   | `Threads_connected` 是否长期接近上限              |
| 活跃线程数   | `Threads_running` 是否长期过高                    |
| 最大使用连接 | `Max_used_connections` 是否接近 `max_connections` |
| 获取连接耗时 | 应用是否频繁等待连接                              |
| 慢 SQL       | 慢 SQL 是否占用大量连接                           |
| 长事务       | 是否存在长期不提交事务                            |
| 连接泄漏     | 连接是否长期不归还                                |
| 应用实例数   | 总连接数是否超过数据库承载能力                    |

连接池参数优化建议：

1. 先优化 SQL 和事务，再调大连接池。
2. 单实例连接池大小应结合接口耗时和并发压测决定。
3. 总连接数必须按所有应用实例累加计算。
4. 连接池耗尽不一定是连接池太小，可能是慢 SQL 或连接泄漏。
5. HikariCP 适合大多数 Spring Boot 项目。
6. Druid 适合需要 SQL 监控和连接池管理页面的项目。
7. 连接池参数变更后应观察接口耗时、连接等待、MySQL CPU、IO 和慢查询。

## 数据一致性设计

数据一致性设计用于保证业务数据在并发、失败、重试、跨系统调用和异步处理场景下仍然符合预期。常见手段包括本地事务、分布式事务、最终一致性、幂等、锁、唯一约束、状态机和补偿机制。

实际项目中，应优先使用简单可靠的方案：单库本地事务优先，唯一约束防重优先，状态机控制流转优先，最终一致性优先于强行使用复杂分布式事务。

### 本地事务一致性

本地事务指同一个 MySQL 数据库内的一组操作在同一个事务中完成。它依赖 InnoDB 的事务能力，适合单库内的订单创建、余额扣减、库存扣减、日志写入等场景。

示例表结构：

```sql
-- 订单表
CREATE TABLE IF NOT EXISTS tx_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务订单表';

-- 账户表
CREATE TABLE IF NOT EXISTS tx_user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务用户账户表';
```

本地事务服务示例。

文件位置：`src/main/java/io/github/atengk/consistency/service/OrderPayService.java`

```java
package io.github.atengk.consistency.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 订单支付本地事务服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPayService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 支付订单并扣减账户余额
     *
     * @param orderNo   订单号
     * @param userId    用户ID
     * @param payAmount 支付金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, Long userId, BigDecimal payAmount) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (userId == null || payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付参数不合法");
        }

        int accountRows = jdbcTemplate.update("""
                        UPDATE tx_user_account
                        SET
                            balance_amount = balance_amount - ?,
                            version = version + 1,
                            updated_at = NOW()
                        WHERE user_id = ?
                          AND balance_amount >= ?
                        """,
                payAmount, userId, payAmount
        );

        if (accountRows != 1) {
            log.warn("账户余额不足或账户不存在，orderNo={}，userId={}，payAmount={}", orderNo, userId, payAmount);
            throw new IllegalStateException("账户余额不足");
        }

        int orderRows = jdbcTemplate.update("""
                        UPDATE tx_order_info
                        SET
                            order_status = 1,
                            updated_at = NOW()
                        WHERE order_no = ?
                          AND user_id = ?
                          AND order_status = 0
                        """,
                orderNo, userId
        );

        if (orderRows != 1) {
            log.warn("订单状态异常，orderNo={}，userId={}", orderNo, userId);
            throw new IllegalStateException("订单状态异常");
        }

        log.info("订单支付成功，orderNo={}，userId={}，payAmount={}", orderNo, userId, payAmount);
    }
}
```

本地事务设计建议：

1. 同库强一致优先使用本地事务。
2. 事务内只放必须一致的数据库操作。
3. 不要在事务中调用 HTTP、RPC、MQ 或文件服务。
4. 更新语句应带状态条件，避免重复更新。
5. 扣减余额、库存时应使用条件更新。
6. 事务方法建议配置 `rollbackFor = Exception.class`。
7. 本地事务不能解决跨库、跨服务一致性问题。

### 分布式事务

分布式事务用于处理跨数据库、跨服务、跨资源的一致性问题。常见方案包括 XA、TCC、SAGA、可靠消息最终一致性和事务消息。分布式事务复杂度高，应谨慎使用。

常见方案对比：

| 方案        | 特点                   | 适用场景                   |
| ----------- | ---------------------- | -------------------------- |
| XA          | 强一致，两阶段提交     | 数据库资源少、性能要求不高 |
| TCC         | Try、Confirm、Cancel   | 资金、库存等强业务控制     |
| SAGA        | 长事务拆分和补偿       | 流程长、可补偿业务         |
| 可靠消息    | 本地事务 + 消息表      | 订单、通知、积分等最终一致 |
| MQ 事务消息 | 消息中间件提供事务能力 | RocketMQ 等支持事务消息    |

不推荐随意使用分布式事务的原因：

```text
1. 实现复杂
2. 性能成本高
3. 故障场景多
4. 排查困难
5. 对业务建模要求高
6. 容易因为超时、重试、锁等待造成系统级问题
```

分布式事务适用建议：

1. 能通过业务拆分避免跨服务强一致，就不要使用分布式事务。
2. 能使用最终一致性，就不要强行使用强一致分布式事务。
3. 核心资金类业务可以考虑 TCC，但必须有完整空回滚、幂等和悬挂处理。
4. 普通业务更推荐本地事务 + 消息表 + 异步补偿。
5. 分布式事务必须配合全链路日志、重试、告警和人工处理入口。

### 最终一致性

最终一致性指系统不要求所有数据立即一致，但要求经过重试、补偿和异步处理后最终达到一致状态。它常用于订单创建后发送消息、支付成功后发放积分、用户注册后发送通知等场景。

典型方案是本地消息表：

```sql
-- 本地消息表，用于最终一致性
CREATE TABLE IF NOT EXISTS local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    message_content JSON NOT NULL COMMENT '消息内容',
    message_status TINYINT NOT NULL DEFAULT 0 COMMENT '消息状态：0待发送，1已发送，2发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_retry (message_status, next_retry_at),
    KEY idx_biz_type_no (biz_type, biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';
```

本地事务中写业务数据和消息：

```sql
-- 示例流程：
-- 1. 创建订单
-- 2. 写入本地消息表
-- 3. 提交事务
-- 4. 后台任务异步发送消息
```

最终一致性流程：

```text
1. 本地事务写入业务表
2. 同一事务写入本地消息表
3. 定时任务扫描待发送消息
4. 发送 MQ 或调用下游服务
5. 发送成功后更新消息状态
6. 发送失败则增加重试次数
7. 超过重试次数后进入人工补偿
```

消息发送任务示例。

文件位置：`src/main/java/io/github/atengk/consistency/job/LocalMessageRetryJob.java`

```java
package io.github.atengk.consistency.job;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 本地消息重试任务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMessageRetryJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 扫描并处理待发送消息
     */
    @Scheduled(fixedDelay = 5000)
    public void retryLocalMessage() {
        List<Map<String, Object>> messages = jdbcTemplate.queryForList("""
                SELECT
                    id,
                    message_id,
                    biz_type,
                    biz_no,
                    message_content,
                    retry_count
                FROM local_message
                WHERE message_status IN (0, 2)
                  AND (next_retry_at IS NULL OR next_retry_at <= NOW())
                  AND retry_count < 5
                ORDER BY id ASC
                LIMIT 100
                """);

        if (CollUtil.isEmpty(messages)) {
            return;
        }

        log.info("开始处理本地消息，count={}", messages.size());
        for (Map<String, Object> message : messages) {
            handleMessage(message);
        }
    }

    /**
     * 处理单条本地消息
     *
     * @param message 消息数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleMessage(Map<String, Object> message) {
        Long id = ((Number) message.get("id")).longValue();
        String messageId = String.valueOf(message.get("message_id"));

        try {
            // 实际项目中这里发送 MQ 或调用下游服务
            log.info("发送本地消息成功，messageId={}", messageId);

            jdbcTemplate.update("""
                    UPDATE local_message
                    SET
                        message_status = 1,
                        updated_at = NOW()
                    WHERE id = ?
                      AND message_status IN (0, 2)
                    """, id);
        } catch (Exception e) {
            log.error("发送本地消息失败，messageId={}", messageId, e);

            jdbcTemplate.update("""
                    UPDATE local_message
                    SET
                        message_status = 2,
                        retry_count = retry_count + 1,
                        next_retry_at = DATE_ADD(NOW(), INTERVAL 1 MINUTE),
                        updated_at = NOW()
                    WHERE id = ?
                    """, id);
        }
    }
}
```

最终一致性建议：

1. 本地消息表必须和业务数据在同一事务中写入。
2. 消息消费者必须支持幂等。
3. 发送失败必须可重试。
4. 重试次数超过阈值后要进入人工补偿。
5. 消息状态、重试次数、错误原因要可查询。
6. 最终一致性适合大多数跨系统异步业务。

### 幂等设计

幂等是指同一个请求重复执行多次，结果与执行一次一致。幂等是解决重试、重复提交、MQ 重复消费、接口超时重试的重要手段。

常见幂等方式：

| 方式       | 说明                       |
| ---------- | -------------------------- |
| 唯一约束   | 利用唯一键防止重复插入     |
| 幂等表     | 记录请求号或消息号         |
| 状态机     | 只允许合法状态流转         |
| 乐观锁     | 使用版本号防止并发覆盖     |
| 防重 Token | 前端提交前获取一次性 Token |
| 业务单号   | 订单号、流水号天然唯一     |
| 消息 ID    | 消费前检查是否已处理       |

幂等表结构：

```sql
-- 幂等请求表
CREATE TABLE IF NOT EXISTS idempotent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0处理中，1成功，2失败',
    response_content JSON DEFAULT NULL COMMENT '响应内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_biz_type_no (biz_type, biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等请求记录表';
```

基于唯一请求号的幂等处理示例。

文件位置：`src/main/java/io/github/atengk/consistency/service/IdempotentService.java`

```java
package io.github.atengk.consistency.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 幂等处理服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行幂等业务处理
     *
     * @param requestId 请求ID
     * @param bizType   业务类型
     * @param bizNo     业务编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void process(String requestId, String bizType, String bizNo) {
        if (StrUtil.hasBlank(requestId, bizType)) {
            throw new IllegalArgumentException("幂等参数不能为空");
        }

        try {
            jdbcTemplate.update("""
                    INSERT INTO idempotent_record (
                        request_id,
                        biz_type,
                        biz_no,
                        process_status
                    ) VALUES (?, ?, ?, 0)
                    """, requestId, bizType, bizNo);
        } catch (DuplicateKeyException e) {
            log.warn("重复请求已拦截，requestId={}，bizType={}，bizNo={}", requestId, bizType, bizNo);
            return;
        }

        // 执行业务逻辑
        log.info("开始处理幂等业务，requestId={}，bizType={}，bizNo={}", requestId, bizType, bizNo);

        jdbcTemplate.update("""
                UPDATE idempotent_record
                SET
                    process_status = 1,
                    updated_at = NOW()
                WHERE request_id = ?
                """, requestId);

        log.info("幂等业务处理完成，requestId={}", requestId);
    }
}
```

幂等设计建议：

1. 外部请求必须带全局唯一请求号。
2. MQ 消费必须基于消息 ID 或业务单号做幂等。
3. 支付、退款、发券、积分、库存扣减必须考虑重复调用。
4. 幂等记录表要有唯一索引。
5. 幂等处理要明确处理中、成功、失败状态。
6. 只依赖 Redis 做幂等时，要考虑 Redis 过期、丢失和数据库最终约束。

### 乐观锁

乐观锁适合冲突概率较低的并发更新场景。它通常通过 `version` 字段实现：查询数据时读取版本号，更新时带上旧版本号，更新成功后版本号加 1。

表结构：

```sql
-- 库存表，使用 version 实现乐观锁
CREATE TABLE IF NOT EXISTS product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    stock_count INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';
```

乐观锁扣减库存：

```sql
-- 基于版本号扣减库存
UPDATE product_stock
SET
    stock_count = stock_count - 1,
    version = version + 1,
    updated_at = NOW()
WHERE product_id = 10001
  AND stock_count >= 1
  AND version = 3;
```

更常见的库存扣减可以直接使用条件更新：

```sql
-- 使用条件更新防止超卖
UPDATE product_stock
SET
    stock_count = stock_count - 1,
    version = version + 1,
    updated_at = NOW()
WHERE product_id = 10001
  AND stock_count >= 1;
```

Java 示例。

文件位置：`src/main/java/io/github/atengk/consistency/service/ProductStockService.java`

```java
package io.github.atengk.consistency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品库存服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param count     扣减数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long productId, Integer count) {
        if (productId == null || count == null || count <= 0) {
            throw new IllegalArgumentException("库存扣减参数不合法");
        }

        int rows = jdbcTemplate.update("""
                        UPDATE product_stock
                        SET
                            stock_count = stock_count - ?,
                            version = version + 1,
                            updated_at = NOW()
                        WHERE product_id = ?
                          AND stock_count >= ?
                        """,
                count, productId, count
        );

        if (rows != 1) {
            log.warn("库存扣减失败，库存不足，productId={}，count={}", productId, count);
            throw new IllegalStateException("库存不足");
        }

        log.info("库存扣减成功，productId={}，count={}", productId, count);
    }
}
```

乐观锁建议：

1. 适合读多写少、冲突较低的场景。
2. 更新时必须检查影响行数。
3. 影响行数为 0 表示版本冲突或条件不满足。
4. 高冲突场景下，乐观锁大量失败会导致重试成本升高。
5. 库存、余额扣减常用条件更新，比先查版本再更新更直接。
6. 乐观锁失败后的重试次数必须受限。

### 悲观锁

悲观锁适合冲突概率较高、必须串行处理同一资源的场景。MySQL 中常用 `SELECT ... FOR UPDATE` 加排他锁。悲观锁必须放在事务中使用。

悲观锁查询账户：

```sql
-- 在事务中锁定账户行
START TRANSACTION;

SELECT
    id,
    user_id,
    balance_amount
FROM tx_user_account
WHERE user_id = 10001
FOR UPDATE;

-- 执行业务更新
UPDATE tx_user_account
SET
    balance_amount = balance_amount - 10.00,
    updated_at = NOW()
WHERE user_id = 10001
  AND balance_amount >= 10.00;

COMMIT;
```

Java 悲观锁示例。

文件位置：`src/main/java/io/github/atengk/consistency/service/PessimisticAccountService.java`

```java
package io.github.atengk.consistency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 悲观锁账户服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PessimisticAccountService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用悲观锁扣减余额
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("扣减参数不合法");
        }

        BigDecimal balance = jdbcTemplate.queryForObject("""
                        SELECT
                            balance_amount
                        FROM tx_user_account
                        WHERE user_id = ?
                        FOR UPDATE
                        """,
                BigDecimal.class,
                userId
        );

        if (balance == null || balance.compareTo(amount) < 0) {
            log.warn("余额不足，userId={}，balance={}，amount={}", userId, balance, amount);
            throw new IllegalStateException("余额不足");
        }

        jdbcTemplate.update("""
                        UPDATE tx_user_account
                        SET
                            balance_amount = balance_amount - ?,
                            updated_at = NOW()
                        WHERE user_id = ?
                        """,
                amount, userId
        );

        log.info("悲观锁扣减余额成功，userId={}，amount={}", userId, amount);
    }
}
```

悲观锁建议：

1. `FOR UPDATE` 必须放在事务中。
2. 锁查询条件必须命中索引，最好是主键或唯一索引。
3. 加锁后应尽快提交事务。
4. 不要在持锁期间调用外部接口。
5. 悲观锁适合高冲突关键资源，但会降低并发。
6. 锁等待和死锁必须接入监控和日志。

### 唯一约束防重

唯一约束是最可靠的防重手段之一。它由数据库保证，不依赖应用层判断。常用于订单号、支付流水号、请求号、消息 ID、用户账号、业务编码等唯一性控制。

订单号唯一：

```sql
-- 订单号唯一约束
ALTER TABLE tx_order_info
  ADD UNIQUE KEY uk_order_no (order_no);
```

支付流水表：

```sql
-- 支付流水表，支付流水号唯一
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    pay_amount DECIMAL(18,2) NOT NULL COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0处理中，1成功，2失败',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

插入时利用唯一约束防重：

```sql
-- 重复 payment_no 会插入失败
INSERT INTO payment_record (
    payment_no,
    order_no,
    pay_amount,
    pay_status
) VALUES (
    'PAY202605110001',
    'O202605110001',
    99.90,
    1
);
```

UPSERT 处理：

```sql
-- 重复流水号时更新状态，适合幂等回调
INSERT INTO payment_record (
    payment_no,
    order_no,
    pay_amount,
    pay_status
) VALUES (
    'PAY202605110001',
    'O202605110001',
    99.90,
    1
)
ON DUPLICATE KEY UPDATE
    pay_status = VALUES(pay_status),
    updated_at = NOW();
```

唯一约束建议：

1. 防重优先使用数据库唯一约束。
2. 应用层先查再插不能替代唯一约束。
3. 唯一键字段应稳定，不应频繁变更。
4. 分库分表场景下，唯一约束只能保证单分片唯一，跨分片唯一需要额外设计。
5. 重复键异常应转换为业务幂等结果或明确失败。
6. 添加唯一索引前必须先清理历史重复数据。

### 状态机控制

状态机用于控制业务状态只能按合法路径流转。它可以防止重复处理、逆向流转和非法状态修改。订单、支付、退款、审批、工单等业务都适合使用状态机。

订单状态示例：

```text
订单状态：
0 待支付
1 已支付
2 已取消
3 已退款

合法流转：
待支付 -> 已支付
待支付 -> 已取消
已支付 -> 已退款
```

使用 SQL 条件控制状态流转：

```sql
-- 只能从待支付变为已支付
UPDATE tx_order_info
SET
    order_status = 1,
    updated_at = NOW()
WHERE order_no = 'O202605110001'
  AND order_status = 0;
```

取消订单：

```sql
-- 只能取消待支付订单
UPDATE tx_order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_no = 'O202605110001'
  AND order_status = 0;
```

退款：

```sql
-- 只能退款已支付订单
UPDATE tx_order_info
SET
    order_status = 3,
    updated_at = NOW()
WHERE order_no = 'O202605110001'
  AND order_status = 1;
```

Java 状态机控制示例。

文件位置：`src/main/java/io/github/atengk/consistency/service/OrderStatusService.java`

```java
package io.github.atengk.consistency.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单状态机服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 标记订单支付成功
     *
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        int rows = jdbcTemplate.update("""
                UPDATE tx_order_info
                SET
                    order_status = 1,
                    updated_at = NOW()
                WHERE order_no = ?
                  AND order_status = 0
                """, orderNo);

        if (rows != 1) {
            log.warn("订单支付状态流转失败，orderNo={}", orderNo);
            throw new IllegalStateException("订单状态不允许支付");
        }

        log.info("订单状态流转为已支付，orderNo={}", orderNo);
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        int rows = jdbcTemplate.update("""
                UPDATE tx_order_info
                SET
                    order_status = 2,
                    updated_at = NOW()
                WHERE order_no = ?
                  AND order_status = 0
                """, orderNo);

        if (rows != 1) {
            log.warn("订单取消失败，orderNo={}", orderNo);
            throw new IllegalStateException("订单状态不允许取消");
        }

        log.info("订单取消成功，orderNo={}", orderNo);
    }
}
```

状态机控制建议：

1. 状态字段必须有清晰枚举定义。
2. 状态流转必须带旧状态条件。
3. 更新后检查影响行数。
4. 非法状态流转应明确拒绝。
5. 不要直接无条件修改状态。
6. 状态机适合与幂等设计结合使用。
7. 状态变更应记录操作日志或状态流水。

### 补偿机制

补偿机制用于处理异步失败、部分成功、外部系统异常和最终一致性未达成的情况。补偿不是简单重试，它需要知道业务当前状态、失败原因、重试次数和可执行的修复动作。

补偿任务表：

```sql
-- 补偿任务表
CREATE TABLE IF NOT EXISTS compensate_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_no VARCHAR(64) NOT NULL COMMENT '补偿任务编号',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    task_status TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0待处理，1处理中，2成功，3失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_status_retry (task_status, next_retry_at),
    KEY idx_biz_type_no (biz_type, biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补偿任务表';
```

创建补偿任务：

```sql
-- 创建支付后发积分补偿任务
INSERT INTO compensate_task (
    task_no,
    biz_type,
    biz_no,
    task_status,
    next_retry_at
) VALUES (
    'COMP202605110001',
    'PAY_POINT_GRANT',
    'O202605110001',
    0,
    NOW()
);
```

补偿任务处理示例。

文件位置：`src/main/java/io/github/atengk/consistency/job/CompensateTaskJob.java`

```java
package io.github.atengk.consistency.job;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 补偿任务处理器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensateTaskJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 定时扫描补偿任务
     */
    @Scheduled(fixedDelay = 10000)
    public void scanCompensateTask() {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT
                    id,
                    task_no,
                    biz_type,
                    biz_no,
                    retry_count,
                    max_retry_count
                FROM compensate_task
                WHERE task_status IN (0, 3)
                  AND retry_count < max_retry_count
                  AND (next_retry_at IS NULL OR next_retry_at <= NOW())
                ORDER BY id ASC
                LIMIT 50
                """);

        if (CollUtil.isEmpty(tasks)) {
            return;
        }

        log.info("扫描到补偿任务，count={}", tasks.size());
        for (Map<String, Object> task : tasks) {
            handleTask(task);
        }
    }

    /**
     * 处理单个补偿任务
     *
     * @param task 补偿任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTask(Map<String, Object> task) {
        Long id = ((Number) task.get("id")).longValue();
        String taskNo = String.valueOf(task.get("task_no"));
        String bizType = String.valueOf(task.get("biz_type"));
        String bizNo = String.valueOf(task.get("biz_no"));

        int lockRows = jdbcTemplate.update("""
                UPDATE compensate_task
                SET
                    task_status = 1,
                    updated_at = NOW()
                WHERE id = ?
                  AND task_status IN (0, 3)
                """, id);

        if (lockRows != 1) {
            log.warn("补偿任务已被其他线程处理，taskNo={}", taskNo);
            return;
        }

        try {
            // 实际项目中根据 bizType 调用对应补偿逻辑
            log.info("执行补偿任务，taskNo={}，bizType={}，bizNo={}", taskNo, bizType, bizNo);

            jdbcTemplate.update("""
                    UPDATE compensate_task
                    SET
                        task_status = 2,
                        updated_at = NOW()
                    WHERE id = ?
                    """, id);

            log.info("补偿任务执行成功，taskNo={}", taskNo);
        } catch (Exception e) {
            log.error("补偿任务执行失败，taskNo={}", taskNo, e);

            jdbcTemplate.update("""
                    UPDATE compensate_task
                    SET
                        task_status = 3,
                        retry_count = retry_count + 1,
                        next_retry_at = DATE_ADD(NOW(), INTERVAL 1 MINUTE),
                        error_message = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """, e.getMessage(), id);
        }
    }
}
```

补偿机制建议：

1. 补偿任务必须有唯一任务号。
2. 补偿逻辑必须幂等。
3. 补偿任务要记录状态、重试次数、下次重试时间和错误原因。
4. 失败重试要有最大次数，不能无限重试。
5. 超过最大重试次数后需要告警和人工处理。
6. 补偿任务应支持按业务编号查询。
7. 重要业务补偿要有操作审计和处理结果记录。

## 常用业务建模

常用业务建模是项目数据库设计中的基础能力。用户、角色权限、字典、配置、日志、订单、支付、文件、树形结构和多租户表是大多数后台系统、业务系统和中台系统都会遇到的模型。设计这些表时，应优先保证字段清晰、约束明确、索引合理、扩展可控，并为后续权限、审计、归档和性能优化预留空间。

### 用户表设计

用户表用于保存系统用户、会员用户、后台账号或应用账号的基础信息。用户表通常是高频查询表，应重点关注登录账号唯一性、手机号唯一性、状态字段、逻辑删除、密码安全和审计字段。

后台用户表示例：

```sql
-- 后台用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_mobile (mobile),
    KEY idx_status_created (user_status, created_at),
    KEY idx_deleted_created (deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

会员用户表示例：

```sql
-- 会员用户表
CREATE TABLE IF NOT EXISTS member_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_no VARCHAR(64) NOT NULL COMMENT '会员编号',
    mobile VARCHAR(20) NOT NULL COMMENT '手机号',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    birthday DATE DEFAULT NULL COMMENT '生日',
    user_level TINYINT NOT NULL DEFAULT 0 COMMENT '用户等级',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    register_source TINYINT NOT NULL DEFAULT 0 COMMENT '注册来源：0未知，1小程序，2APP，3后台',
    register_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_member_no (member_no),
    UNIQUE KEY uk_mobile (mobile),
    KEY idx_status_created (user_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员用户表';
```

用户查询示例：

```sql
-- 根据用户名查询后台用户
SELECT
    id,
    username,
    nickname,
    mobile,
    email,
    user_status,
    last_login_at
FROM sys_user
WHERE username = 'admin'
  AND deleted = 0;
```

用户表设计建议：

1. 密码字段只保存哈希值，不保存明文密码。
2. 登录账号、手机号、会员编号等唯一业务字段应建立唯一索引。
3. 手机号、邮箱、身份证号等敏感字段应结合业务要求脱敏或加密。
4. 用户状态字段应明确枚举含义。
5. 逻辑删除字段应纳入常用查询索引。
6. 用户表不要无限增加扩展字段，低频扩展信息可以拆到用户扩展表。

### 角色权限表设计

角色权限表用于实现 RBAC 权限模型。常见模型包括用户表、角色表、菜单表、权限表、用户角色关联表、角色菜单关联表和角色权限关联表。中小型后台系统通常使用用户、角色、菜单权限三类核心表即可。

角色表：

```sql
-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_sort INT NOT NULL DEFAULT 0 COMMENT '角色排序',
    role_status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_status_sort (role_status, role_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

用户角色关联表：

```sql
-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

菜单权限表：

```sql
-- 菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单ID',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type TINYINT NOT NULL COMMENT '菜单类型：1目录，2菜单，3按钮',
    permission_code VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    route_path VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    component_path VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    icon VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    menu_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏，1显示',
    menu_status TINYINT NOT NULL DEFAULT 1 COMMENT '菜单状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_parent_sort (parent_id, menu_sort),
    KEY idx_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';
```

角色菜单关联表：

```sql
-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
```

查询用户权限标识：

```sql
-- 查询用户拥有的权限标识
SELECT DISTINCT
    m.permission_code
FROM sys_user_role AS ur
JOIN sys_role AS r ON ur.role_id = r.id
JOIN sys_role_menu AS rm ON r.id = rm.role_id
JOIN sys_menu AS m ON rm.menu_id = m.id
WHERE ur.user_id = 10001
  AND r.role_status = 1
  AND r.deleted = 0
  AND m.menu_status = 1
  AND m.deleted = 0
  AND m.permission_code IS NOT NULL
  AND m.permission_code <> '';
```

角色权限表设计建议：

1. 角色编码应唯一，例如 `admin`、`operator`、`auditor`。
2. 用户和角色是多对多关系，应使用关联表。
3. 角色和菜单权限也是多对多关系，应使用关联表。
4. 权限标识应稳定，例如 `system:user:add`、`order:info:query`。
5. 菜单和按钮权限可以放在同一张菜单表中，通过 `menu_type` 区分。
6. 权限变更应记录操作日志，便于审计。

### 字典表设计

字典表用于维护状态、类型、枚举、标签等可配置值。字典表可以减少代码硬编码，适合后台管理系统提供动态配置。常见设计是字典类型表和字典数据表。

字典类型表：

```sql
-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    type_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dict_type (dict_type),
    KEY idx_status_created (type_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';
```

字典数据表：

```sql
-- 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    data_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    css_class VARCHAR(100) DEFAULT NULL COMMENT '样式类名',
    list_class VARCHAR(100) DEFAULT NULL COMMENT '回显样式',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：0否，1是',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_value (dict_type, dict_value),
    KEY idx_type_sort (dict_type, dict_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
```

插入订单状态字典：

```sql
-- 插入字典类型
INSERT INTO sys_dict_type (
    dict_type,
    dict_name,
    type_status
) VALUES (
    'order_status',
    '订单状态',
    1
);

-- 插入字典数据
INSERT INTO sys_dict_data (
    dict_type,
    dict_label,
    dict_value,
    dict_sort,
    data_status
) VALUES
    ('order_status', '待支付', '0', 1, 1),
    ('order_status', '已支付', '1', 2, 1),
    ('order_status', '已取消', '2', 3, 1),
    ('order_status', '已退款', '3', 4, 1);
```

查询字典：

```sql
-- 查询启用字典数据
SELECT
    dict_label,
    dict_value,
    dict_sort,
    list_class
FROM sys_dict_data
WHERE dict_type = 'order_status'
  AND data_status = 1
  AND deleted = 0
ORDER BY dict_sort ASC;
```

字典表设计建议：

1. 稳定核心枚举仍应在代码中定义，字典表用于展示和可配置项。
2. 字典类型编码必须唯一。
3. 字典值建议使用字符串，便于兼容数字、字母和组合编码。
4. 字典数据应支持排序和启停。
5. 高频字典查询建议在应用层缓存。
6. 字典变更应考虑缓存刷新。

### 配置表设计

配置表用于保存系统参数、业务开关、阈值、第三方配置等。配置表应支持按配置键查询、状态控制、数据类型标识和敏感配置隔离。

系统配置表：

```sql
-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_name VARCHAR(128) NOT NULL COMMENT '配置名称',
    config_value VARCHAR(2000) DEFAULT NULL COMMENT '配置值',
    config_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING，NUMBER，BOOLEAN，JSON',
    config_group VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '配置分组',
    config_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sensitive TINYINT NOT NULL DEFAULT 0 COMMENT '是否敏感配置：0否，1是',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_group_status (config_group, config_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
```

插入配置：

```sql
-- 插入业务配置
INSERT INTO sys_config (
    config_key,
    config_name,
    config_value,
    config_type,
    config_group,
    config_status,
    sensitive,
    remark
) VALUES
    ('order.timeout.minutes', '订单超时时间分钟数', '30', 'NUMBER', 'order', 1, 0, '待支付订单超过该时间自动取消'),
    ('sms.enabled', '短信开关', 'true', 'BOOLEAN', 'message', 1, 0, '是否启用短信发送'),
    ('pay.callback.secret', '支付回调密钥', '******', 'STRING', 'pay', 1, 1, '敏感配置应加密保存');
```

查询配置：

```sql
-- 根据配置键查询启用配置
SELECT
    config_key,
    config_name,
    config_value,
    config_type
FROM sys_config
WHERE config_key = 'order.timeout.minutes'
  AND config_status = 1;
```

配置表设计建议：

1. 配置键必须唯一，建议使用点分层命名。
2. 敏感配置不建议明文存储，应加密或放入专门密钥系统。
3. 高频配置建议应用层缓存。
4. 配置变更要记录操作日志。
5. 配置类型应明确，应用读取后按类型转换。
6. 复杂配置可以使用 JSON，但不要把大量业务数据塞进配置表。

### 日志表设计

日志表用于记录登录日志、操作日志、接口日志、状态变更日志、审计日志等。日志表通常数据增长快，应重点考虑写入性能、索引控制、归档和清理策略。

操作日志表：

```sql
-- 系统操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '操作用户名',
    module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    request_uri VARCHAR(500) DEFAULT NULL COMMENT '请求地址',
    request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    request_param JSON DEFAULT NULL COMMENT '请求参数',
    response_result JSON DEFAULT NULL COMMENT '响应结果',
    operation_status TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0失败，1成功',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    cost_millis BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_created (user_id, created_at),
    KEY idx_module_created (module_name, created_at),
    KEY idx_trace_id (trace_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';
```

登录日志表：

```sql
-- 用户登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    login_status TINYINT NOT NULL COMMENT '登录状态：0失败，1成功',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_username_login_at (username, login_at),
    KEY idx_login_ip_at (login_ip, login_at),
    KEY idx_login_at (login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';
```

查询操作日志：

```sql
-- 查询某用户最近操作日志
SELECT
    id,
    trace_id,
    username,
    module_name,
    operation_type,
    operation_status,
    cost_millis,
    created_at
FROM sys_operation_log
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 50;
```

日志表设计建议：

1. 日志表不要设计过多索引，避免写入成本过高。
2. 请求参数和响应结果可能包含敏感数据，应脱敏后再入库。
3. 日志表应按时间归档或清理。
4. 大规模日志更适合写入日志系统或搜索引擎。
5. 日志表不应影响核心业务事务提交。
6. 高频日志可以异步写入。

### 订单表设计

订单表是典型核心业务表，通常数据量大、查询频繁、状态流转复杂。订单表设计应关注订单号唯一性、用户维度查询、状态查询、金额精度、时间字段和历史归档。

订单主表：

```sql
-- 订单主表
CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
    pay_at DATETIME DEFAULT NULL COMMENT '支付时间',
    cancel_at DATETIME DEFAULT NULL COMMENT '取消时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (order_status, created_at),
    KEY idx_pay_status_created (pay_status, created_at),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

订单明细表：

```sql
-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_image VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    buy_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买数量',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售单价',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '明细总金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
```

订单状态流转：

```sql
-- 订单支付成功，只允许待支付订单变为已支付
UPDATE order_info
SET
    order_status = 1,
    pay_status = 1,
    pay_at = NOW(),
    updated_at = NOW()
WHERE order_no = 'O202605110001'
  AND order_status = 0
  AND pay_status = 0;
```

订单表设计建议：

1. 订单号必须唯一，不能只依赖主键 ID。
2. 金额字段必须使用 `DECIMAL`。
3. 状态流转更新必须带旧状态条件。
4. 用户订单列表应建立 `(user_id, created_at)` 相关索引。
5. 后台按状态查询应建立状态和时间联合索引。
6. 订单快照、收货地址等大字段可拆到订单扩展表。
7. 订单数据量大时应设计归档、分区或分表方案。

### 支付表设计

支付表用于记录支付流水、支付渠道、支付状态和第三方交易号。支付数据必须支持幂等、防重、对账和状态追踪。

支付流水表：

```sql
-- 支付流水表
CREATE TABLE IF NOT EXISTS pay_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道：WECHAT，ALIPAY，BALANCE',
    out_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方交易号',
    pay_amount DECIMAL(18,2) NOT NULL COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0处理中，1成功，2失败，3已关闭',
    request_content JSON DEFAULT NULL COMMENT '请求内容',
    response_content JSON DEFAULT NULL COMMENT '响应内容',
    pay_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no),
    UNIQUE KEY uk_out_trade_no (out_trade_no),
    KEY idx_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (pay_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

支付回调日志表：

```sql
-- 支付回调日志表
CREATE TABLE IF NOT EXISTS pay_callback_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    callback_no VARCHAR(64) NOT NULL COMMENT '回调编号',
    pay_no VARCHAR(64) DEFAULT NULL COMMENT '支付流水号',
    out_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方交易号',
    callback_content JSON NOT NULL COMMENT '回调内容',
    callback_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1成功，2失败',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_callback_no (callback_no),
    KEY idx_pay_no (pay_no),
    KEY idx_out_trade_no (out_trade_no),
    KEY idx_status_created (callback_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志表';
```

支付成功幂等更新：

```sql
-- 支付成功回调幂等更新
UPDATE pay_record
SET
    pay_status = 1,
    pay_at = NOW(),
    updated_at = NOW()
WHERE pay_no = 'PAY202605110001'
  AND pay_status = 0;
```

支付表设计建议：

1. 支付流水号必须唯一。
2. 第三方交易号应建立唯一索引，允许为空时要结合实际数据库行为评估。
3. 支付回调必须幂等。
4. 支付状态只能按合法状态流转。
5. 支付请求和响应内容可能包含敏感信息，应谨慎保存。
6. 支付表应支持对账查询，例如按渠道、状态、时间范围查询。

### 文件表设计

文件表用于保存上传文件的元数据，例如文件名、大小、类型、存储桶、对象路径、访问地址和业务归属。文件内容通常不直接存 MySQL，而是存储在 MinIO、OSS、S3、本地文件系统等对象存储中。

文件元数据表：

```sql
-- 文件元数据表
CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_no VARCHAR(64) NOT NULL COMMENT '文件编号',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_ext VARCHAR(32) DEFAULT NULL COMMENT '文件扩展名',
    content_type VARCHAR(128) DEFAULT NULL COMMENT '文件内容类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小字节数',
    storage_type VARCHAR(32) NOT NULL DEFAULT 'MINIO' COMMENT '存储类型：LOCAL，MINIO，OSS，S3',
    bucket_name VARCHAR(128) DEFAULT NULL COMMENT '存储桶名称',
    object_key VARCHAR(500) NOT NULL COMMENT '对象存储Key',
    access_url VARCHAR(1000) DEFAULT NULL COMMENT '访问地址',
    file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件哈希值',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    upload_user_id BIGINT DEFAULT NULL COMMENT '上传用户ID',
    file_status TINYINT NOT NULL DEFAULT 1 COMMENT '文件状态：0无效，1有效',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_file_no (file_no),
    KEY idx_biz_type_no (biz_type, biz_no),
    KEY idx_file_hash (file_hash),
    KEY idx_upload_user_created (upload_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';
```

业务附件关联表：

```sql
-- 业务附件关联表
CREATE TABLE IF NOT EXISTS biz_file_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    relation_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_biz_file (biz_type, biz_no, file_id),
    KEY idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务文件关联表';
```

查询业务附件：

```sql
-- 查询指定业务的附件列表
SELECT
    f.id,
    f.file_no,
    f.original_name,
    f.content_type,
    f.file_size,
    f.access_url,
    r.relation_sort
FROM biz_file_relation AS r
JOIN sys_file AS f ON r.file_id = f.id
WHERE r.biz_type = 'ORDER'
  AND r.biz_no = 'O202605110001'
  AND f.file_status = 1
ORDER BY r.relation_sort ASC, f.id ASC;
```

文件表设计建议：

1. MySQL 只保存文件元数据，不建议直接保存文件二进制内容。
2. 文件编号应唯一。
3. 对象存储路径 `object_key` 应稳定保存。
4. 文件哈希可用于秒传、去重和完整性校验。
5. 文件访问地址可能过期，必要时动态生成签名 URL。
6. 文件删除建议先逻辑失效，再异步删除对象存储内容。

### 树形结构表设计

树形结构常用于菜单、部门、分类、区域等层级数据。常见设计方式包括邻接表、路径枚举、闭包表。普通后台系统最常用邻接表，即保存 `parent_id`。

部门表示例：

```sql
-- 部门表，使用 parent_id 表示树形结构
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级部门ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(64) DEFAULT NULL COMMENT '部门编码',
    ancestors VARCHAR(500) DEFAULT NULL COMMENT '祖级列表，例如 0,1,2',
    dept_level INT NOT NULL DEFAULT 1 COMMENT '部门层级',
    dept_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    leader_user_id BIGINT DEFAULT NULL COMMENT '负责人用户ID',
    dept_status TINYINT NOT NULL DEFAULT 1 COMMENT '部门状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent_sort (parent_id, dept_sort),
    KEY idx_status_sort (dept_status, dept_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

查询子部门：

```sql
-- 查询某个部门的直接子部门
SELECT
    id,
    parent_id,
    dept_name,
    dept_code,
    dept_level,
    dept_sort
FROM sys_dept
WHERE parent_id = 100
  AND deleted = 0
ORDER BY dept_sort ASC, id ASC;
```

查询所有后代部门：

```sql
-- 基于 ancestors 查询后代部门
SELECT
    id,
    parent_id,
    dept_name,
    ancestors,
    dept_level
FROM sys_dept
WHERE FIND_IN_SET('100', ancestors)
  AND deleted = 0
ORDER BY dept_level ASC, dept_sort ASC;
```

MySQL 8 递归 CTE 查询树：

```sql
-- 使用递归 CTE 查询部门树
WITH RECURSIVE dept_tree AS (
    SELECT
        id,
        parent_id,
        dept_name,
        dept_sort,
        1 AS tree_level
    FROM sys_dept
    WHERE parent_id = 0
      AND deleted = 0

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_name,
        d.dept_sort,
        dt.tree_level + 1 AS tree_level
    FROM sys_dept AS d
    JOIN dept_tree AS dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT
    id,
    parent_id,
    dept_name,
    tree_level
FROM dept_tree
ORDER BY tree_level ASC, dept_sort ASC;
```

树形结构设计建议：

1. 简单树结构优先使用 `parent_id`。
2. 需要快速查询所有祖先或后代时，可以增加 `ancestors` 字段。
3. 树层级不宜过深。
4. 移动节点时要同步更新子节点的 `ancestors` 和层级。
5. 菜单、部门、分类都应有排序字段。
6. 高频树查询可以在应用层缓存。

### 多租户表设计

多租户表用于支持一个系统服务多个租户。常见方案包括独立数据库、独立 Schema、共享表加 `tenant_id`。中小型 SaaS 系统常用共享表加租户字段，但必须做好租户隔离。

共享表多租户用户表：

```sql
-- 多租户用户表
CREATE TABLE IF NOT EXISTS tenant_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    KEY idx_tenant_status_created (tenant_id, user_status, created_at),
    KEY idx_tenant_mobile (tenant_id, mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多租户用户表';
```

租户表：

```sql
-- 租户表
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_mobile VARCHAR(20) DEFAULT NULL COMMENT '联系人手机号',
    tenant_status TINYINT NOT NULL DEFAULT 1 COMMENT '租户状态：0禁用，1启用',
    expired_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tenant_code (tenant_code),
    KEY idx_status_expired (tenant_status, expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';
```

多租户查询：

```sql
-- 查询当前租户下的用户
SELECT
    id,
    tenant_id,
    username,
    nickname,
    mobile,
    user_status,
    created_at
FROM tenant_user
WHERE tenant_id = 10001
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

多租户表设计建议：

1. 共享表模式下，所有业务表必须带 `tenant_id`。
2. 唯一索引通常需要包含 `tenant_id`，例如 `(tenant_id, username)`。
3. 所有查询必须自动追加租户条件。
4. 禁止跨租户误查、误改、误删。
5. 超大租户可能产生数据倾斜，需要单独治理。
6. 高安全隔离要求场景可考虑独立库或独立实例。

## 数据库变更管理

数据库变更管理用于规范 DDL、DML、版本脚本、灰度发布和回滚流程。数据库变更直接影响数据安全和业务稳定性，必须有审核、测试、发布、验证和回滚机制。

### DDL 变更规范

DDL 变更包括创建表、修改表、添加字段、删除字段、添加索引、删除索引、修改字段类型等。DDL 变更通常风险高，尤其是大表 DDL，可能导致锁表、主从延迟、磁盘上涨或业务异常。

DDL 变更脚本示例：

```sql
-- 版本：V20260511_001
-- 说明：订单表新增渠道编码字段
-- 风险：低
-- 回滚：删除 channel_code 字段，执行前确认应用不再使用

ALTER TABLE order_info
  ADD COLUMN channel_code VARCHAR(32) DEFAULT NULL COMMENT '渠道编码';
```

添加索引变更脚本：

```sql
-- 版本：V20260511_002
-- 说明：订单表新增用户状态时间查询索引
-- 风险：中，大表执行前需评估耗时和主从延迟

ALTER TABLE order_info
  ADD KEY idx_user_status_created (
      user_id,
      order_status,
      created_at DESC,
      id DESC
  );
```

添加唯一索引前检查：

```sql
-- 添加唯一索引前检查重复数据
SELECT
    order_no,
    COUNT(*) AS duplicate_count
FROM order_info
GROUP BY order_no
HAVING COUNT(*) > 1
LIMIT 20;
```

DDL 变更审核项：

| 审核项         | 说明                           |
| -------------- | ------------------------------ |
| 是否影响大表   | 大表 DDL 需要单独评估          |
| 是否会锁表     | 判断是否支持 Online DDL        |
| 是否需要回滚   | 提供可执行回滚方案             |
| 是否影响兼容性 | 字段删除、改名、类型变更风险高 |
| 是否影响主从   | 大 DDL 可能造成复制延迟        |
| 是否有默认值   | 新增非空字段要谨慎             |
| 是否有重复数据 | 添加唯一索引前必须检查         |
| 是否经过测试   | 必须在测试环境执行验证         |

DDL 变更建议：

1. DDL 脚本必须进入版本管理。
2. 大表变更必须评估耗时、锁和磁盘空间。
3. 不要在业务高峰执行重型 DDL。
4. 字段删除、字段改名、类型缩小属于高风险变更。
5. 添加唯一索引前必须检查历史重复数据。
6. DDL 执行后要验证表结构、索引和核心 SQL 执行计划。

### DML 变更规范

DML 变更包括生产数据修复、批量更新、批量删除、初始化数据、状态修正等。DML 变更风险通常比 DDL 更直接，因为它会修改业务数据。

DML 更新前先查询影响范围：

```sql
-- 更新前确认影响数据量
SELECT
    COUNT(*) AS affected_count
FROM order_info
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

分批更新：

```sql
-- 分批取消超时订单
UPDATE order_info
SET
    order_status = 2,
    cancel_at = NOW(),
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY id
LIMIT 1000;
```

更新前备份受影响数据：

```sql
-- 创建备份表保存待修改数据
CREATE TABLE IF NOT EXISTS backup_order_info_20260511 AS
SELECT
    *
FROM order_info
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

删除前备份数据：

```sql
-- 删除前备份待删除数据
CREATE TABLE IF NOT EXISTS backup_order_log_20260511 AS
SELECT
    *
FROM order_log
WHERE created_at < '2025-01-01 00:00:00';
```

DML 变更规范：

1. `UPDATE` 和 `DELETE` 必须带 `WHERE` 条件。
2. 变更前必须先执行 `SELECT COUNT(*)` 确认影响范围。
3. 大批量变更必须分批执行。
4. 生产数据修复前必须备份受影响数据。
5. 高风险 DML 必须提供回滚 SQL。
6. 更新条件应命中索引，避免锁全表。
7. 执行后要校验影响行数和业务结果。

### 版本脚本管理

数据库版本脚本用于记录每次结构和数据变更。所有环境都应使用同一套脚本按顺序执行，避免开发、测试、预发、生产结构不一致。

推荐目录结构：

```text
db/
  migration/
    V20260511_001__create_sys_user.sql
    V20260511_002__create_sys_role.sql
    V20260511_003__alter_order_add_channel_code.sql
  rollback/
    R20260511_003__alter_order_drop_channel_code.sql
  data/
    D20260511_001__init_dict_order_status.sql
```

版本脚本命名建议：

```text
结构变更：
V日期_序号__变更说明.sql

数据初始化：
D日期_序号__变更说明.sql

回滚脚本：
R日期_序号__回滚说明.sql

示例：
V20260511_001__create_order_info.sql
V20260511_002__alter_order_add_index.sql
D20260511_001__init_order_status_dict.sql
R20260511_002__drop_order_index.sql
```

版本脚本示例：

```sql
-- V20260511_001__create_product_category.sql
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级分类ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    category_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_parent_sort (parent_id, category_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';
```

版本脚本管理建议：

1. 数据库脚本必须提交代码仓库。
2. 禁止生产环境手工改表后不补脚本。
3. 脚本应按版本号顺序执行。
4. 每个脚本只做一类明确变更。
5. 脚本执行结果应有记录。
6. 回滚脚本和验证 SQL 应随变更一起提交。

### Flyway 使用

Flyway 是常用数据库版本管理工具，按版本号自动执行 SQL 脚本，并通过元数据表记录已执行版本。它适合 Spring Boot 项目管理数据库结构变更。

Maven 依赖：

```xml
<dependencies>
    <!-- Flyway 核心依赖，用于管理数据库版本脚本 -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- MySQL 支持模块，Flyway 新版本通常需要单独引入 -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>
</dependencies>
```

Spring Boot 配置：

```yaml
spring:
  flyway:
    # 是否启用 Flyway
    enabled: true

    # 迁移脚本路径
    locations: classpath:db/migration

    # 元数据表名称
    table: flyway_schema_history

    # 非空库首次使用时是否建立基线
    baseline-on-migrate: true

    # 基线版本
    baseline-version: 0

    # 脚本编码
    encoding: UTF-8
```

脚本路径：

```text
src/main/resources/db/migration/
  V1__create_sys_user.sql
  V2__create_sys_role.sql
  V3__alter_order_add_channel_code.sql
```

Flyway 脚本示例：

```sql
-- V1__create_sys_user.sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

查看 Flyway 执行记录：

```sql
-- 查看 Flyway 迁移历史
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    success,
    installed_on
FROM flyway_schema_history
ORDER BY installed_rank ASC;
```

Flyway 使用建议：

1. 已执行脚本不要修改内容，否则校验会失败。
2. 新变更应新增脚本，不要覆盖旧脚本。
3. 生产环境执行前先在测试和预发环境验证。
4. 大表 DDL 不建议在应用启动时自动执行，应由变更流程单独执行。
5. Flyway 适合管理版本记录，但不能替代变更审核。

### Liquibase 使用

Liquibase 是另一种数据库版本管理工具，支持 XML、YAML、JSON、SQL 等格式定义变更。它比 Flyway 更强调结构化变更和回滚描述，适合需要更细粒度变更管理的团队。

Maven 依赖：

```xml
<dependencies>
    <!-- Liquibase 核心依赖，用于数据库变更管理 -->
    <dependency>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-core</artifactId>
    </dependency>
</dependencies>
```

Spring Boot 配置：

```yaml
spring:
  liquibase:
    # 是否启用 Liquibase
    enabled: true

    # 主变更文件路径
    change-log: classpath:db/changelog/db.changelog-master.yaml

    # Liquibase 元数据表名称
    database-change-log-table: databasechangelog

    # Liquibase 锁表名称
    database-change-log-lock-table: databasechangeloglock
```

目录结构：

```text
src/main/resources/db/changelog/
  db.changelog-master.yaml
  changes/
    20260511-create-sys-user.yaml
    20260511-alter-order-add-channel.yaml
```

主变更文件：

```yaml
# db.changelog-master.yaml
databaseChangeLog:
  # 引入用户表创建脚本
  - include:
      file: db/changelog/changes/20260511-create-sys-user.yaml

  # 引入订单字段变更脚本
  - include:
      file: db/changelog/changes/20260511-alter-order-add-channel.yaml
```

创建表变更示例：

```yaml
# 20260511-create-sys-user.yaml
databaseChangeLog:
  - changeSet:
      id: 20260511-001-create-sys-user
      author: Ateng
      changes:
        - createTable:
            tableName: sys_user
            remarks: 系统用户表
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  remarks: 主键ID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: username
                  type: VARCHAR(64)
                  remarks: 用户名
                  constraints:
                    nullable: false
              - column:
                  name: password_hash
                  type: VARCHAR(255)
                  remarks: 密码哈希值
                  constraints:
                    nullable: false
              - column:
                  name: user_status
                  type: TINYINT
                  defaultValueNumeric: 1
                  remarks: 用户状态：0禁用，1启用
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: DATETIME
                  defaultValueComputed: CURRENT_TIMESTAMP
                  remarks: 创建时间
                  constraints:
                    nullable: false
        - addUniqueConstraint:
            tableName: sys_user
            columnNames: username
            constraintName: uk_username
      rollback:
        - dropTable:
            tableName: sys_user
```

Liquibase 使用建议：

1. 适合需要结构化变更和回滚定义的团队。
2. `changeSet` 的 `id` 和 `author` 应保持唯一。
3. 已执行的变更不应随意修改。
4. 复杂 SQL 仍可使用 SQL 文件方式管理。
5. 生产大表变更仍应走独立审核和执行流程。
6. Liquibase 功能强，但维护成本高于简单 SQL 脚本。

### 灰度发布数据库变更

数据库变更灰度发布用于降低应用版本和数据库结构不兼容的风险。核心原则是数据库变更要尽量向前兼容，应用发布与数据库变更分阶段执行。

新增字段的灰度流程：

```text
1. 数据库先新增可空字段或带默认值字段
2. 老应用继续运行，不依赖新字段
3. 发布新应用，开始写入新字段
4. 观察数据写入是否正常
5. 如有需要，回填历史数据
6. 所有应用升级完成后，再增加非空约束或清理兼容逻辑
```

字段重命名不推荐直接执行：

```sql
-- 不推荐：直接重命名字段，老应用可能立即报错
-- ALTER TABLE order_info RENAME COLUMN channel_code TO source_channel;
```

兼容式字段重命名流程：

```text
1. 新增新字段 source_channel
2. 应用双写 channel_code 和 source_channel
3. 回填历史数据
4. 应用改为只读 source_channel
5. 观察无异常后下线 channel_code 读取
6. 最后删除旧字段 channel_code
```

新增字段：

```sql
-- 第一步：新增新字段，保持兼容
ALTER TABLE order_info
  ADD COLUMN source_channel VARCHAR(32) DEFAULT NULL COMMENT '来源渠道';
```

回填历史数据：

```sql
-- 分批回填历史数据
UPDATE order_info
SET
    source_channel = channel_code,
    updated_at = NOW()
WHERE source_channel IS NULL
  AND channel_code IS NOT NULL
ORDER BY id
LIMIT 1000;
```

灰度发布建议：

1. 数据库变更优先保证向前兼容。
2. 不要在一个版本中同时删除字段和发布依赖删除后的代码。
3. 字段改名通过“新增字段、双写、回填、切读、删除旧字段”完成。
4. 新增非空字段时，先允许为空或提供默认值。
5. 大表回填必须分批。
6. 删除字段和删除索引应放到最后阶段。
7. 发布期间要监控错误日志、慢查询和数据写入情况。

### 回滚脚本设计

回滚脚本用于在数据库变更出现问题时恢复到变更前状态。并不是所有变更都能完全回滚，尤其是删除字段、删除数据、字段类型缩小等破坏性操作。因此回滚设计必须在变更前完成。

新增字段的回滚：

```sql
-- 回滚：删除新增字段
ALTER TABLE order_info
  DROP COLUMN channel_code;
```

新增索引的回滚：

```sql
-- 回滚：删除新增索引
ALTER TABLE order_info
  DROP INDEX idx_user_status_created;
```

DML 修复回滚示例。先备份：

```sql
-- 变更前备份受影响数据
CREATE TABLE backup_order_status_20260511 AS
SELECT
    id,
    order_status,
    updated_at
FROM order_info
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

执行变更：

```sql
-- 执行状态修复
UPDATE order_info
SET
    order_status = 2,
    updated_at = NOW()
WHERE order_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

回滚数据：

```sql
-- 根据备份表回滚状态
UPDATE order_info AS o
JOIN backup_order_status_20260511 AS b ON o.id = b.id
SET
    o.order_status = b.order_status,
    o.updated_at = b.updated_at;
```

不可完全回滚的变更：

| 变更         | 风险                   |
| ------------ | ---------------------- |
| 删除字段     | 字段数据丢失           |
| 删除表       | 表数据丢失             |
| 删除数据     | 需要备份才能恢复       |
| 字段类型缩小 | 可能截断数据           |
| 字符集转换   | 可能存在乱码或转换失败 |
| 合并字段     | 原始字段可能无法还原   |
| 清理历史数据 | 依赖归档或备份恢复     |

回滚脚本设计建议：

1. 每个变更都应说明回滚方式。
2. DML 变更必须先备份受影响数据。
3. 高风险 DDL 不一定能快速回滚，应提前说明。
4. 删除字段、删除表、删除数据必须特别谨慎。
5. 回滚脚本也要在测试环境验证。
6. 不能回滚的变更必须有备份恢复方案。

### 生产变更审核

生产变更审核用于在执行数据库变更前发现风险，避免误操作、慢 SQL、锁表、数据丢失和应用不兼容。数据库变更应纳入标准发布流程，而不是由个人直接连接生产执行。

生产变更审核清单：

| 审核项       | 说明                      |
| ------------ | ------------------------- |
| 变更目的     | 为什么要改                |
| 变更范围     | 涉及哪些库表              |
| 影响数据量   | 预计影响多少行            |
| 执行时间     | 是否避开业务高峰          |
| 是否可回滚   | 是否提供回滚脚本          |
| 是否有备份   | 高风险变更前是否备份      |
| 是否大表     | 大表 DDL 是否评估锁和耗时 |
| 是否影响应用 | 应用版本是否兼容          |
| 是否影响主从 | 是否可能造成复制延迟      |
| 是否已测试   | 测试环境是否执行成功      |
| 验证方式     | 执行后如何确认成功        |
| 负责人       | 谁执行、谁验证、谁回滚    |

生产变更单内容示例：

```text
变更名称：订单表新增渠道编码字段

变更原因：
支持订单来源渠道统计。

涉及库表：
mall_order.order_info

变更脚本：
ALTER TABLE order_info
  ADD COLUMN channel_code VARCHAR(32) DEFAULT NULL COMMENT '渠道编码';

风险评估：
低风险。新增可空字段，不影响老版本应用。

执行窗口：
2026-05-11 23:00:00 到 2026-05-11 23:30:00

回滚脚本：
ALTER TABLE order_info
  DROP COLUMN channel_code;

验证 SQL：
SHOW COLUMNS FROM order_info LIKE 'channel_code';

负责人：
执行人：DBA
验证人：后端负责人
```

生产变更后验证：

```sql
-- 验证字段是否存在
SHOW COLUMNS FROM order_info LIKE 'channel_code';

-- 验证索引是否存在
SHOW INDEX FROM order_info WHERE Key_name = 'idx_user_status_created';

-- 验证核心查询执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

生产变更审核建议：

1. 所有生产数据库变更必须走审核。
2. 审核内容必须包含脚本、影响范围、回滚方式和验证方式。
3. 高风险变更必须有备份和应急方案。
4. 大表 DDL 应优先使用低峰窗口和在线变更工具。
5. 变更执行后必须立即验证。
6. 变更过程要有日志记录，便于审计和复盘。

## 监控与运维

MySQL 监控与运维的目标是提前发现风险、定位性能瓶颈、保障数据安全和降低故障恢复时间。常见监控维度包括连接数、QPS、TPS、慢查询、锁等待、主从延迟、磁盘空间、Buffer Pool、InnoDB 状态和错误日志。

生产环境建议使用 Prometheus + mysqld_exporter + Grafana，或者云数据库自带监控能力。同时应保留必要的 SQL 巡检脚本，便于临时排查问题。

### 连接数监控

连接数监控用于判断应用连接池、数据库最大连接数和当前连接使用情况是否合理。连接数异常升高通常和慢 SQL、锁等待、连接泄漏、应用流量突增有关。

查看连接数相关状态：

```sql
-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 查看历史最大使用连接数
SHOW GLOBAL STATUS LIKE 'Max_used_connections';

-- 查看当前已连接线程数
SHOW GLOBAL STATUS LIKE 'Threads_connected';

-- 查看当前正在运行的线程数
SHOW GLOBAL STATUS LIKE 'Threads_running';

-- 查看累计连接数
SHOW GLOBAL STATUS LIKE 'Connections';
```

查看当前连接明细：

```sql
-- 查看当前连接来源、状态和正在执行的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
ORDER BY time DESC;
```

按用户和来源统计连接数：

```sql
-- 按用户和来源统计当前连接数量
SELECT
    user,
    host,
    COUNT(*) AS connection_count
FROM information_schema.PROCESSLIST
GROUP BY user, host
ORDER BY connection_count DESC;
```

连接数监控建议：

1. `Threads_connected` 表示当前连接数量。
2. `Threads_running` 表示当前活跃执行线程数量，比连接数更能体现数据库压力。
3. `Max_used_connections` 接近 `max_connections` 时需要关注连接池配置和慢 SQL。
4. 大量 `Sleep` 连接通常和连接池配置有关。
5. 大量长时间运行连接通常需要排查慢 SQL、锁等待和事务未提交。
6. 应用连接池总连接数要按所有实例累加计算。

### QPS 监控

QPS 表示每秒查询数量，通常通过 MySQL 的 `Questions` 或 `Queries` 状态值计算。QPS 可以反映数据库整体请求压力。

查看累计查询数：

```sql
-- 查看客户端发送到 MySQL 的语句数量
SHOW GLOBAL STATUS LIKE 'Questions';

-- 查看服务端执行语句数量，通常包含存储过程内部语句
SHOW GLOBAL STATUS LIKE 'Queries';
```

计算 QPS：

```text
QPS = 两次采样 Questions 差值 / 采样间隔秒数

示例：
第 1 次 Questions = 1000000
第 2 次 Questions = 1003000
间隔 = 60 秒

QPS = (1003000 - 1000000) / 60 = 50
```

Linux 简单采样：

```bash
# 每隔 10 秒查看一次 Questions
mysql -uroot -p -e "SHOW GLOBAL STATUS LIKE 'Questions';"
sleep 10
mysql -uroot -p -e "SHOW GLOBAL STATUS LIKE 'Questions';"
```

QPS 监控建议：

1. QPS 要结合 CPU、IO、慢查询一起看。
2. QPS 突增可能来自流量上涨、定时任务、爬虫或异常重试。
3. QPS 不高但数据库很慢，可能是单条 SQL 代价过高。
4. QPS 很高但资源稳定，说明查询较轻或缓存命中较好。
5. 应区分业务高峰 QPS 和异常 QPS。

### TPS 监控

TPS 表示每秒事务数量。MySQL 可通过 `Com_commit` 和 `Com_rollback` 状态值估算事务提交和回滚情况。

查看事务提交和回滚次数：

```sql
-- 查看累计提交事务数
SHOW GLOBAL STATUS LIKE 'Com_commit';

-- 查看累计回滚事务数
SHOW GLOBAL STATUS LIKE 'Com_rollback';
```

计算 TPS：

```text
TPS = 两次采样期间的提交事务和回滚事务差值 / 采样间隔秒数

TPS = ((Com_commit_2 + Com_rollback_2) - (Com_commit_1 + Com_rollback_1)) / 秒数
```

查看 InnoDB 行变更情况：

```sql
-- 查看 InnoDB 行插入、更新、删除、读取数量
SHOW GLOBAL STATUS LIKE 'Innodb_rows_inserted';
SHOW GLOBAL STATUS LIKE 'Innodb_rows_updated';
SHOW GLOBAL STATUS LIKE 'Innodb_rows_deleted';
SHOW GLOBAL STATUS LIKE 'Innodb_rows_read';
```

TPS 监控建议：

1. TPS 适合观察写入事务压力。
2. TPS 突增可能来自批量任务、导入任务或异常重试。
3. 回滚事务过多说明业务异常、死锁、约束冲突或程序错误较多。
4. TPS 高时要关注 Redo Log、Binlog、磁盘 IO 和主从延迟。
5. 批量写入应避开业务高峰，并监控 TPS 波动。

### 慢查询监控

慢查询监控用于发现执行时间超过阈值的 SQL。慢查询是性能优化的主要入口，应长期启用并定期分析。

查看慢查询配置：

```sql
-- 查看慢查询是否开启
SHOW VARIABLES LIKE 'slow_query_log';

-- 查看慢查询日志文件路径
SHOW VARIABLES LIKE 'slow_query_log_file';

-- 查看慢查询阈值
SHOW VARIABLES LIKE 'long_query_time';

-- 查看未使用索引查询是否记录
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
```

开启慢查询日志：

```sql
-- 临时开启慢查询日志
SET GLOBAL slow_query_log = ON;

-- 设置慢查询阈值为 1 秒
SET GLOBAL long_query_time = 1;
```

配置文件中开启慢查询：

```ini
[mysqld]
# 开启慢查询日志
slow_query_log=ON

# 慢查询日志路径
slow_query_log_file=/var/log/mysql/mysql-slow.log

# 超过 1 秒的 SQL 记录为慢查询
long_query_time=1

# 记录慢管理语句
log_slow_admin_statements=ON
```

查看慢查询日志：

```bash
# 查看最近 200 行慢查询日志
sudo tail -n 200 /var/log/mysql/mysql-slow.log

# 实时观察慢查询日志
sudo tail -f /var/log/mysql/mysql-slow.log
```

慢查询监控建议：

1. 生产环境建议长期启用慢查询日志。
2. 阈值通常设置为 0.5 秒、1 秒或 2 秒，按业务要求调整。
3. 应重点关注总耗时高、执行次数多的 SQL，而不是只看单次最慢。
4. 慢查询日志要配置轮转，避免占满磁盘。
5. 慢 SQL 应结合 `EXPLAIN`、索引、数据量和业务调用链分析。

### 锁等待监控

锁等待监控用于发现事务阻塞、行锁等待、元数据锁等待和长事务问题。锁等待可能导致接口超时、连接堆积、事务回滚和主从延迟。

查看当前 InnoDB 事务：

```sql
-- 查看当前 InnoDB 事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

查看锁等待关系：

```sql
-- 查看 InnoDB 锁等待关系
SELECT
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread_id,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread_id,
    b.trx_query AS blocking_query
FROM information_schema.INNODB_LOCK_WAITS AS w
JOIN information_schema.INNODB_TRX AS r ON w.requesting_trx_id = r.trx_id
JOIN information_schema.INNODB_TRX AS b ON w.blocking_trx_id = b.trx_id;
```

查看进程状态：

```sql
-- 查看当前非 Sleep 线程
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC;
```

锁等待监控建议：

1. 长事务是锁等待和 Undo Log 膨胀的重要原因。
2. 更新、删除条件必须命中索引，避免锁范围扩大。
3. 事务中不要调用外部接口。
4. 出现锁等待时，先找阻塞事务，再决定是否终止。
5. 频繁锁等待应从业务并发模型、索引和事务边界优化。

### 主从延迟监控

主从延迟表示从库应用主库变更落后的时间。主从延迟会影响读写分离的数据一致性，也会影响故障切换的安全性。

查看复制状态：

```sql
-- MySQL 8 推荐命令
SHOW REPLICA STATUS\G
```

旧版本命令：

```sql
-- 旧版本命令
SHOW SLAVE STATUS\G
```

重点字段：

```text
Replica_IO_Running: Yes
Replica_SQL_Running: Yes
Seconds_Behind_Source: 0
Last_IO_Error:
Last_SQL_Error:
Retrieved_Gtid_Set:
Executed_Gtid_Set:
```

通过 `performance_schema` 查看复制状态：

```sql
-- 查看复制连接状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    SOURCE_UUID,
    LAST_ERROR_NUMBER,
    LAST_ERROR_MESSAGE
FROM performance_schema.replication_connection_status;

-- 查看复制应用状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    REMAINING_DELAY,
    LAST_ERROR_NUMBER,
    LAST_ERROR_MESSAGE
FROM performance_schema.replication_applier_status;
```

主从延迟监控建议：

1. 监控 `Seconds_Behind_Source` 或旧字段 `Seconds_Behind_Master`。
2. 同时监控 IO 线程和 SQL 线程状态。
3. 复制延迟超过阈值时，应将从库从读流量中摘除。
4. 大事务、批量更新、从库慢 SQL 都可能导致延迟。
5. 故障切换前必须确认候选从库数据进度。

### 磁盘空间监控

磁盘空间不足会导致 MySQL 无法写入数据、Binlog、Redo Log、临时文件或错误日志，严重时会导致实例不可用。磁盘监控必须覆盖数据目录、Binlog 目录、临时目录和日志目录。

查看数据库大小：

```sql
-- 查看各数据库大小
SELECT
    TABLE_SCHEMA,
    ROUND(SUM(DATA_LENGTH) / 1024 / 1024 / 1024, 2) AS data_gb,
    ROUND(SUM(INDEX_LENGTH) / 1024 / 1024 / 1024, 2) AS index_gb,
    ROUND(SUM(DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024 / 1024, 2) AS total_gb
FROM information_schema.TABLES
GROUP BY TABLE_SCHEMA
ORDER BY total_gb DESC;
```

查看大表：

```sql
-- 查看当前库中最大的表
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024 / 1024, 2) AS data_gb,
    ROUND(INDEX_LENGTH / 1024 / 1024 / 1024, 2) AS index_gb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024 / 1024, 2) AS total_gb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY total_gb DESC
LIMIT 20;
```

Linux 查看磁盘：

```bash
# 查看磁盘空间
df -h

# 查看 MySQL 数据目录大小
sudo du -sh /var/lib/mysql

# 查看 MySQL 目录下各文件大小
sudo du -sh /var/lib/mysql/* | sort -h
```

磁盘空间监控建议：

1. 磁盘使用率超过 80% 应预警，超过 90% 应立即处理。
2. 重点关注 Binlog、临时文件、慢查询日志和错误日志。
3. 不要直接 `rm` 删除 MySQL 管理的 Binlog，应使用 `PURGE BINARY LOGS`。
4. 大表归档和历史数据清理应提前规划。
5. 大查询可能产生临时文件，占用临时目录空间。
6. 备份文件不应长期保存在数据库数据盘。

### Buffer Pool 命中率监控

Buffer Pool 是 InnoDB 缓存数据页和索引页的核心区域。Buffer Pool 命中率低通常意味着频繁读取磁盘，可能导致查询变慢和 IO 压力升高。

查看 Buffer Pool 配置：

```sql
-- 查看 Buffer Pool 大小
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- 查看 Buffer Pool 实例数量
SHOW VARIABLES LIKE 'innodb_buffer_pool_instances';
```

查看 Buffer Pool 状态：

```sql
-- 查看逻辑读和物理读
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';

-- 查看 Buffer Pool 页状态
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_total';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_free';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_pages_dirty';
```

命中率计算：

```text
Buffer Pool 命中率 ≈ 1 - Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests

示例：
Innodb_buffer_pool_read_requests = 100000000
Innodb_buffer_pool_reads = 100000

命中率 = 1 - 100000 / 100000000 = 99.9%
```

Buffer Pool 监控建议：

1. 命中率应结合业务场景判断，不是越接近 100% 就一定健康。
2. 大量全表扫描会污染 Buffer Pool。
3. Buffer Pool 过小会导致物理读增加。
4. Buffer Pool 过大可能挤压操作系统内存。
5. 应结合磁盘 IO、慢查询和数据集大小分析。

### InnoDB 状态监控

InnoDB 状态包含事务、锁、死锁、Buffer Pool、IO、信号量、行操作等信息，是排查性能和锁问题的重要入口。

查看 InnoDB 状态：

```sql
-- 查看 InnoDB 引擎状态
SHOW ENGINE INNODB STATUS\G
```

重点关注内容：

```text
1. LATEST DETECTED DEADLOCK
2. TRANSACTIONS
3. FILE I/O
4. BUFFER POOL AND MEMORY
5. ROW OPERATIONS
6. SEMAPHORES
```

查看 InnoDB 相关状态指标：

```sql
-- 查看 InnoDB 相关状态
SHOW GLOBAL STATUS LIKE 'Innodb%';
```

查看当前事务：

```sql
-- 查看当前活跃事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

InnoDB 状态监控建议：

1. 死锁问题优先查看 `LATEST DETECTED DEADLOCK`。
2. 锁等待问题重点查看 `TRANSACTIONS`。
3. IO 问题关注文件读写、脏页和刷盘。
4. 长事务会影响 Undo Log 和 MVCC。
5. 建议将关键 InnoDB 指标接入监控平台。

### 错误日志监控

错误日志记录 MySQL 启动、关闭、崩溃、复制错误、InnoDB 错误、权限异常等信息。它是排查数据库异常的重要日志。

查看错误日志路径：

```sql
-- 查看错误日志路径
SHOW VARIABLES LIKE 'log_error';
```

Linux 查看错误日志：

```bash
# 查看最近错误日志
sudo tail -n 200 /var/log/mysql/error.log

# 实时查看错误日志
sudo tail -f /var/log/mysql/error.log
```

常见错误日志内容：

```text
1. MySQL 启动失败
2. InnoDB 表空间异常
3. 复制线程异常
4. 连接认证失败
5. 磁盘空间不足
6. 崩溃恢复信息
7. 插件加载失败
8. 配置参数错误
```

错误日志监控建议：

1. 错误日志应接入日志平台或告警系统。
2. 重点告警关键字包括 `ERROR`、`InnoDB`、`crash`、`denied`、`replication`。
3. 错误日志也需要轮转，避免单文件过大。
4. MySQL 异常重启后应第一时间查看错误日志。
5. 主从复制异常通常也会在错误日志中留下线索。

## 常见故障处理

MySQL 常见故障处理应遵循“先止血、再定位、后修复、再复盘”的原则。生产环境不要在原因未明时连续执行高风险操作，例如强杀进程、删除文件、跳过复制错误或直接恢复备份。

### 无法连接数据库

无法连接数据库通常由网络、端口、账号密码、权限、MySQL 服务状态、连接数耗尽、防火墙或绑定地址配置导致。

检查 MySQL 服务状态：

```bash
# 查看 MySQL 服务状态
sudo systemctl status mysql

# 启动 MySQL
sudo systemctl start mysql

# 重启 MySQL
sudo systemctl restart mysql
```

检查端口监听：

```bash
# 查看 3306 端口是否监听
sudo ss -lntp | grep 3306
```

测试连接：

```bash
# 使用 mysql 客户端测试连接
mysql -h 127.0.0.1 -P 3306 -u root -p
```

查看绑定地址：

```sql
-- 查看绑定地址
SHOW VARIABLES LIKE 'bind_address';

-- 查看端口
SHOW VARIABLES LIKE 'port';
```

查看用户权限：

```sql
-- 查看用户权限
SHOW GRANTS FOR 'app_user'@'10.20.30.%';
```

常见原因和处理：

| 原因              | 处理方式                         |
| ----------------- | -------------------------------- |
| MySQL 未启动      | 启动服务并查看错误日志           |
| 端口不通          | 检查防火墙、安全组、监听地址     |
| 账号密码错误      | 重置密码或修正配置               |
| Host 不匹配       | 修改账号 Host 或授权             |
| 连接数打满        | 释放连接、优化慢 SQL、调整连接池 |
| bind_address 限制 | 检查 MySQL 监听地址              |
| 网络异常          | 检查链路、DNS、负载均衡          |

处理建议：

1. 先确认服务是否启动。
2. 再确认端口是否监听和网络是否可达。
3. 再确认账号、密码和 Host 权限。
4. 如果只有应用无法连接，检查应用数据源配置和连接池。
5. 如果所有客户端都无法连接，重点查看 MySQL 服务和错误日志。

### 连接数过多

连接数过多通常表现为应用获取连接超时、MySQL 报 `Too many connections`、接口大量超时。根因可能是连接池配置过大、慢 SQL、锁等待、连接泄漏或流量突增。

查看连接数：

```sql
-- 查看最大连接数和当前连接数
SHOW VARIABLES LIKE 'max_connections';
SHOW GLOBAL STATUS LIKE 'Threads_connected';
SHOW GLOBAL STATUS LIKE 'Threads_running';
SHOW GLOBAL STATUS LIKE 'Max_used_connections';
```

查看连接来源：

```sql
-- 按用户和来源统计连接
SELECT
    user,
    host,
    COUNT(*) AS connection_count
FROM information_schema.PROCESSLIST
GROUP BY user, host
ORDER BY connection_count DESC;
```

查看长时间连接：

```sql
-- 查看执行时间较长或空闲时间较长的连接
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
ORDER BY time DESC
LIMIT 50;
```

临时终止异常连接：

```sql
-- 终止指定连接，执行前确认连接来源和 SQL
KILL 12345;
```

处理建议：

1. 不要第一时间调大 `max_connections`，先找连接来源。
2. 如果大量连接在执行慢 SQL，优先处理慢 SQL。
3. 如果大量连接等待锁，优先处理锁等待。
4. 如果大量连接长期 `Sleep`，检查连接池配置。
5. 如果是连接泄漏，开启连接池泄漏检测。
6. 临时调大连接数只是止血手段，不能替代根因修复。

### SQL 执行慢

SQL 执行慢可能由索引缺失、扫描行数过多、排序临时表、锁等待、磁盘 IO、Buffer Pool 命中率低、返回数据量过大等原因导致。

查看当前慢 SQL：

```sql
-- 查看当前正在执行的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC;
```

分析执行计划：

```sql
-- 使用 EXPLAIN 分析 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

使用 `EXPLAIN ANALYZE`：

```sql
-- MySQL 8 支持 EXPLAIN ANALYZE，能看到实际执行耗时
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

常见慢 SQL 原因：

| 原因           | 表现                              |
| -------------- | --------------------------------- |
| 未命中索引     | `type=ALL`，扫描行数大            |
| 索引不匹配排序 | `Using filesort`                  |
| 分组排序成本高 | `Using temporary`                 |
| 回表过多       | 返回字段过多，覆盖索引不足        |
| 深分页         | `LIMIT 100000,20`                 |
| 锁等待         | SQL 本身不复杂但耗时长            |
| 返回数据过多   | 大结果集传输慢                    |
| 大字段读取     | `TEXT`、`JSON`、`BLOB` 读取成本高 |

处理建议：

1. 先区分是执行慢还是等待慢。
2. 使用慢查询日志和 `PROCESSLIST` 找到 SQL。
3. 使用 `EXPLAIN` 分析索引和扫描行数。
4. 优化索引、SQL 条件、分页方式和返回字段。
5. 对报表统计类 SQL，考虑汇总表或离线计算。
6. 对偶发慢查询，排查锁等待和 IO 抖动。

### 死锁问题

死锁是两个或多个事务互相等待对方持有的锁，MySQL 检测到死锁后会主动回滚其中一个事务。死锁不一定是数据库故障，但频繁死锁说明并发更新顺序、索引或事务设计存在问题。

查看最近死锁：

```sql
-- 查看 InnoDB 状态中的最近死锁信息
SHOW ENGINE INNODB STATUS\G
```

死锁日志重点关注：

```text
LATEST DETECTED DEADLOCK
事务 1 正在执行的 SQL
事务 1 持有和等待的锁
事务 2 正在执行的 SQL
事务 2 持有和等待的锁
最终被回滚的事务
```

常见死锁场景：

```text
事务 A：
1. 更新订单 1
2. 更新订单 2

事务 B：
1. 更新订单 2
2. 更新订单 1

两个事务加锁顺序不一致，容易产生死锁。
```

推荐统一加锁顺序：

```sql
-- 推荐：按主键升序处理，减少死锁概率
SELECT
    id
FROM order_info
WHERE id IN (10001, 10002)
ORDER BY id ASC
FOR UPDATE;
```

死锁处理建议：

1. 应用层必须能处理死锁异常并进行有限重试。
2. 多行更新时保持固定加锁顺序。
3. 更新条件必须命中索引，避免锁范围扩大。
4. 事务尽量短，减少持锁时间。
5. 不要在事务中调用外部接口。
6. 频繁死锁需要分析 InnoDB 死锁日志并优化业务并发流程。

### 锁等待超时

锁等待超时表示事务等待锁超过 `innodb_lock_wait_timeout`，通常会报 `Lock wait timeout exceeded`。它和死锁不同，锁等待超时是等太久后失败，死锁是 MySQL 检测到循环等待后主动回滚。

查看锁等待超时时间：

```sql
-- 查看 InnoDB 锁等待超时时间，单位秒
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';
```

查看锁等待关系：

```sql
-- 查看当前锁等待和阻塞事务
SELECT
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread_id,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread_id,
    b.trx_query AS blocking_query
FROM information_schema.INNODB_LOCK_WAITS AS w
JOIN information_schema.INNODB_TRX AS r ON w.requesting_trx_id = r.trx_id
JOIN information_schema.INNODB_TRX AS b ON w.blocking_trx_id = b.trx_id;
```

终止阻塞线程：

```sql
-- 确认阻塞线程后再终止
KILL 12345;
```

常见原因：

| 原因           | 说明                           |
| -------------- | ------------------------------ |
| 长事务未提交   | 长时间持有行锁                 |
| 更新条件无索引 | 锁范围过大                     |
| 批量更新过大   | 持锁时间长                     |
| 事务中外部调用 | HTTP、RPC 导致事务长时间不提交 |
| DDL 阻塞       | 元数据锁等待                   |
| 热点行竞争     | 多线程同时更新同一行           |

处理建议：

1. 先找阻塞事务，不要盲目调大超时时间。
2. 确认阻塞 SQL 和业务来源。
3. 必要时终止阻塞线程止血。
4. 优化更新条件索引。
5. 缩短事务时间。
6. 热点行更新可考虑队列化、分片或异步聚合。

### 主从延迟

主从延迟表示从库同步主库变更落后。它会导致读写分离读取旧数据，也会影响故障切换时的数据完整性。

查看复制状态：

```sql
-- 查看从库复制状态
SHOW REPLICA STATUS\G
```

重点字段：

```text
Replica_IO_Running
Replica_SQL_Running
Seconds_Behind_Source
Last_IO_Error
Last_SQL_Error
Read_Source_Log_Pos
Exec_Source_Log_Pos
Retrieved_Gtid_Set
Executed_Gtid_Set
```

查看从库长查询：

```sql
-- 查看从库正在执行的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC;
```

常见原因：

| 原因           | 说明                   |
| -------------- | ---------------------- |
| 主库大事务     | 从库重放耗时长         |
| 批量更新删除   | Relay Log 重放压力大   |
| 从库机器性能弱 | CPU、IO 不足           |
| 从库查询压力大 | 报表查询影响复制线程   |
| 单线程复制瓶颈 | 并行复制未开启或效果差 |
| 锁等待         | SQL 线程被阻塞         |
| 网络问题       | IO 线程拉取慢          |

处理建议：

1. 先判断是 IO 线程延迟还是 SQL 线程应用延迟。
2. 暂停或限流从库上的报表查询。
3. 主库批量任务改为分批提交。
4. 开启并行复制前要测试。
5. 读写分离中延迟从库应自动摘除。
6. 延迟严重时不要贸然进行主从切换。

### 磁盘空间不足

磁盘空间不足是高危故障，可能导致写入失败、Binlog 无法生成、临时表无法落盘、实例异常退出。处理时要先确认占用来源，再采取安全清理方式。

查看磁盘空间：

```bash
# 查看磁盘使用情况
df -h

# 查看 MySQL 数据目录大小
sudo du -sh /var/lib/mysql

# 查看 MySQL 目录下各文件大小
sudo du -sh /var/lib/mysql/* | sort -h
```

查看 Binlog：

```sql
-- 查看 Binlog 文件列表和大小
SHOW BINARY LOGS;
```

安全清理 Binlog：

```sql
-- 清理指定文件之前的 Binlog，不包含该文件
PURGE BINARY LOGS TO 'mysql-bin.000120';

-- 或清理指定时间之前的 Binlog
PURGE BINARY LOGS BEFORE '2026-05-01 00:00:00';
```

切勿直接删除 MySQL 管理文件：

```text
不推荐操作：
rm -rf /var/lib/mysql/mysql-bin.000xxx
rm -rf /var/lib/mysql/ibdata1
rm -rf /var/lib/mysql/数据库目录

这些操作可能导致实例异常、复制异常或数据损坏。
```

常见占用来源：

| 来源       | 处理方式                         |
| ---------- | -------------------------------- |
| Binlog     | 确认备份和从库进度后使用 `PURGE` |
| 慢查询日志 | 日志轮转或清理旧日志             |
| 错误日志   | 日志轮转                         |
| 临时文件   | 找到大查询或临时表来源           |
| 大表数据   | 归档、清理、分区                 |
| 备份文件   | 移动到备份盘或对象存储           |
| Relay Log  | 检查复制状态                     |

处理建议：

1. 先确认磁盘占用来源。
2. 不要直接删除 MySQL 数据文件。
3. 清理 Binlog 前确认从库已同步并且备份不再需要。
4. 临时止血可以迁移备份文件、日志文件。
5. 根因治理应做归档、日志轮转、Binlog 保留策略和磁盘扩容。

### 字符乱码

字符乱码通常由客户端字符集、连接字符集、表字符集、字段字符集、文件编码不一致导致。MySQL 8 推荐统一使用 `utf8mb4`。

查看字符集配置：

```sql
-- 查看字符集相关配置
SHOW VARIABLES LIKE 'character_set%';

-- 查看排序规则相关配置
SHOW VARIABLES LIKE 'collation%';
```

查看库字符集：

```sql
-- 查看当前数据库字符集
SELECT
    DEFAULT_CHARACTER_SET_NAME,
    DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = DATABASE();
```

查看表和字段字符集：

```sql
-- 查看表结构和字符集
SHOW CREATE TABLE sys_user;
```

连接 URL 推荐配置：

```properties
jdbc:mysql://127.0.0.1:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

创建数据库时指定字符集：

```sql
-- 创建数据库时指定 utf8mb4
CREATE DATABASE IF NOT EXISTS mall_user
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

修改表字符集：

```sql
-- 修改表默认字符集，并转换字段字符集
ALTER TABLE sys_user
CONVERT TO CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

字符乱码处理建议：

1. 数据库、表、字段、连接统一使用 `utf8mb4`。
2. Java 连接 URL 明确配置字符集。
3. 导入 CSV 时指定 `CHARACTER SET utf8mb4`。
4. 已经乱码的数据不一定能通过修改字符集恢复，需要判断原始编码。
5. 修改大表字符集风险较高，执行前必须备份和验证。

### 数据误删

数据误删包括误执行 `DELETE`、`TRUNCATE`、`DROP TABLE`、错误条件更新等。误删后第一原则是保护现场，避免继续写入覆盖恢复依据。

误删后立即处理流程：

```text
1. 立即停止相关写入或隔离业务入口
2. 保护 Binlog，禁止自动清理
3. 确认误操作时间、操作 SQL、影响库表
4. 找到最近一次可用全量备份
5. 恢复备份到临时实例
6. 回放 Binlog 到误操作前
7. 校验数据
8. 导出误删数据并修复生产
9. 复盘并加防护
```

查看 Binlog 文件：

```sql
-- 查看当前 Binlog 文件
SHOW MASTER STATUS;

-- 查看 Binlog 列表
SHOW BINARY LOGS;
```

按时间恢复到误删前：

```bash
# 截取误删前的 Binlog
mysqlbinlog \
  --start-datetime="2026-05-11 00:00:00" \
  --stop-datetime="2026-05-11 10:29:59" \
  /data/backup/mysql/binlog/mysql-bin.000120 \
  /data/backup/mysql/binlog/mysql-bin.000121 \
  > recover_before_delete.sql
```

回放到临时实例：

```bash
# 在临时实例回放 Binlog
mysql \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  < recover_before_delete.sql
```

从临时实例导出恢复数据：

```bash
# 从临时实例导出误删表
mysqldump \
  -h 127.0.0.1 \
  -P 3307 \
  -u root \
  -p \
  --default-character-set=utf8mb4 \
  mall_order order_info \
  > order_info_restore.sql
```

数据误删处理建议：

1. 不要直接在生产库回放未验证的 Binlog。
2. 优先恢复到临时实例。
3. 保护 Binlog 和全量备份。
4. 确认误操作时间非常关键。
5. `TRUNCATE` 和 `DROP` 恢复难度高于普通 `DELETE`。
6. 生产环境应限制高危权限，并开启 SQL 审核。

### 表损坏

表损坏可能由磁盘故障、文件系统异常、异常断电、硬件问题、MySQL 崩溃或不当删除文件导致。InnoDB 表损坏处理必须谨慎，优先保护数据目录和错误日志。

查看表状态：

```sql
-- 检查表状态
CHECK TABLE order_info;
```

查看错误日志：

```bash
# 查看 MySQL 错误日志
sudo tail -n 300 /var/log/mysql/error.log
```

尝试分析表：

```sql
-- 分析表统计信息
ANALYZE TABLE order_info;
```

MyISAM 表可以尝试修复，但 InnoDB 不推荐依赖该方式：

```sql
-- 主要适用于 MyISAM，InnoDB 场景不要盲目依赖
REPAIR TABLE some_myisam_table;
```

InnoDB 严重损坏时的处理思路：

```text
1. 停止业务写入
2. 备份当前数据目录和错误日志
3. 确认损坏范围
4. 优先使用备份恢复
5. 必要时使用 innodb_force_recovery 只读启动导出数据
6. 重建实例
7. 导入可恢复数据
```

`innodb_force_recovery` 示例：

```ini
[mysqld]
# 仅在紧急恢复时使用，值越大风险越高
innodb_force_recovery=1
```

表损坏处理建议：

1. 不要反复重启 MySQL 试运气。
2. 先备份现场，包括数据目录和错误日志。
3. InnoDB 损坏优先通过备份恢复。
4. `innodb_force_recovery` 只用于紧急导出数据，不应用于正常运行。
5. 检查磁盘、文件系统、内存和硬件健康。
6. 恢复后必须做完整一致性校验和备份验证。

## 常用系统表与诊断工具

MySQL 提供了多类系统库、系统表和命令行工具，用于查看元数据、运行状态、性能指标、锁等待、事务信息、复制状态和日志内容。常用入口包括 `information_schema`、`performance_schema`、`sys` Schema、`SHOW` 系列命令，以及 `mysqladmin`、`mysqlbinlog`、Percona Toolkit 等工具。

这些工具适合在不同场景下使用：

| 工具或系统库                | 主要用途                                              |
| --------------------------- | ----------------------------------------------------- |
| `information_schema`        | 查看库、表、字段、索引、分区、权限等元数据            |
| `performance_schema`        | 查看性能事件、等待事件、线程、SQL、锁、复制等运行指标 |
| `sys` Schema                | 基于 `performance_schema` 的易用诊断视图              |
| `SHOW PROCESSLIST`          | 查看当前连接和正在执行的 SQL                          |
| `SHOW ENGINE INNODB STATUS` | 查看 InnoDB 事务、锁、死锁、Buffer Pool 等状态        |
| `SHOW VARIABLES`            | 查看 MySQL 参数                                       |
| `SHOW STATUS`               | 查看 MySQL 运行状态计数器                             |
| `mysqladmin`                | 命令行查看状态、ping、关闭、刷新等                    |
| `mysqlbinlog`               | 解析 Binlog，用于审计和恢复                           |
| `pt` 工具集                 | 慢 SQL 分析、在线 DDL、数据一致性校验等               |

### information_schema

`information_schema` 是 MySQL 提供的元数据库，用于查询数据库对象的元信息，例如数据库、表、字段、索引、分区、约束、触发器、存储过程等。

查看当前实例中的数据库：

```sql
-- 查看所有数据库
SELECT
    SCHEMA_NAME,
    DEFAULT_CHARACTER_SET_NAME,
    DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
ORDER BY SCHEMA_NAME;
```

查看当前库中的表：

```sql
-- 查看当前数据库中的表信息
SELECT
    TABLE_NAME,
    TABLE_TYPE,
    ENGINE,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_mb,
    TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
```

查看大表：

```sql
-- 查看当前库中数据量和空间占用较大的表
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024 / 1024, 2) AS data_gb,
    ROUND(INDEX_LENGTH / 1024 / 1024 / 1024, 2) AS index_gb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024 / 1024, 2) AS total_gb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY total_gb DESC
LIMIT 20;
```

查看字段信息：

```sql
-- 查看指定表字段定义
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_KEY,
    EXTRA,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_info'
ORDER BY ORDINAL_POSITION;
```

查看索引信息：

```sql
-- 查看指定表索引字段
SELECT
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    COLLATION,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_info'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

查看分区信息：

```sql
-- 查看分区表的分区信息
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    PARTITION_DESCRIPTION,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_mb
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_info'
ORDER BY PARTITION_ORDINAL_POSITION;
```

查看当前事务：

```sql
-- 查看当前 InnoDB 事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

`information_schema` 使用建议：

1. 适合查看元数据和对象结构。
2. 大库中查询 `information_schema` 可能有性能开销，应加明确过滤条件。
3. 排查表大小、字段定义、索引、分区时优先使用。
4. 自动巡检脚本可以基于 `information_schema` 生成表结构、索引和容量报告。
5. 不建议在高频业务逻辑中直接查询 `information_schema`。

### performance_schema

`performance_schema` 是 MySQL 的性能监控系统库，用于采集 SQL、线程、等待事件、锁、事务、复制、内存等运行时信息。它比 `information_schema` 更偏向性能诊断。

查看当前线程：

```sql
-- 查看当前线程信息
SELECT
    THREAD_ID,
    PROCESSLIST_ID,
    PROCESSLIST_USER,
    PROCESSLIST_HOST,
    PROCESSLIST_DB,
    PROCESSLIST_COMMAND,
    PROCESSLIST_TIME,
    PROCESSLIST_STATE,
    PROCESSLIST_INFO
FROM performance_schema.threads
WHERE PROCESSLIST_ID IS NOT NULL
ORDER BY PROCESSLIST_TIME DESC;
```

查看 SQL 语句汇总：

```sql
-- 查看执行次数和总耗时较高的 SQL 模板
SELECT
    DIGEST_TEXT,
    COUNT_STAR AS exec_count,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_seconds,
    ROUND(AVG_TIMER_WAIT / 1000000000000, 4) AS avg_seconds,
    SUM_ROWS_EXAMINED AS rows_examined,
    SUM_ROWS_SENT AS rows_sent
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = DATABASE()
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;
```

查看等待事件：

```sql
-- 查看等待事件汇总
SELECT
    EVENT_NAME,
    COUNT_STAR,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_seconds,
    ROUND(AVG_TIMER_WAIT / 1000000000000, 6) AS avg_seconds
FROM performance_schema.events_waits_summary_global_by_event_name
WHERE COUNT_STAR > 0
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;
```

查看元数据锁：

```sql
-- 查看元数据锁，排查 DDL 或表结构变更阻塞
SELECT
    OBJECT_TYPE,
    OBJECT_SCHEMA,
    OBJECT_NAME,
    LOCK_TYPE,
    LOCK_DURATION,
    LOCK_STATUS,
    OWNER_THREAD_ID
FROM performance_schema.metadata_locks
WHERE OBJECT_SCHEMA = DATABASE()
ORDER BY OBJECT_NAME, LOCK_STATUS;
```

查看表锁等待：

```sql
-- 查看表锁等待情况
SELECT
    OBJECT_SCHEMA,
    OBJECT_NAME,
    COUNT_STAR,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_seconds
FROM performance_schema.table_lock_waits_summary_by_table
WHERE OBJECT_SCHEMA = DATABASE()
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;
```

查看复制连接状态：

```sql
-- 查看复制连接状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    SOURCE_UUID,
    LAST_ERROR_NUMBER,
    LAST_ERROR_MESSAGE
FROM performance_schema.replication_connection_status;
```

查看复制应用状态：

```sql
-- 查看复制应用线程状态
SELECT
    CHANNEL_NAME,
    SERVICE_STATE,
    REMAINING_DELAY,
    COUNT_TRANSACTIONS_RETRIES,
    LAST_ERROR_NUMBER,
    LAST_ERROR_MESSAGE
FROM performance_schema.replication_applier_status;
```

`performance_schema` 使用建议：

1. 适合性能分析和运行时诊断。
2. 慢 SQL 汇总可以通过 `events_statements_summary_by_digest` 查询。
3. 锁等待、元数据锁、复制状态都可以在这里分析。
4. 生产环境通常建议开启，但要根据版本和负载评估采集开销。
5. 如果要做长期监控，建议通过监控系统采集关键指标，而不是人工频繁查询。

### sys Schema

`sys` Schema 是 MySQL 基于 `performance_schema` 和 `information_schema` 提供的一组诊断视图，查询结果更易读，适合日常巡检和问题排查。

查看占用空间较大的表：

```sql
-- 查看表空间占用
SELECT
    table_schema,
    table_name,
    total_latency,
    rows_fetched,
    rows_inserted,
    rows_updated,
    rows_deleted
FROM sys.schema_table_statistics
WHERE table_schema = DATABASE()
ORDER BY rows_fetched DESC
LIMIT 20;
```

查看表 IO 统计：

```sql
-- 查看表 IO 等待统计
SELECT
    table_schema,
    table_name,
    io_read_requests,
    io_read,
    io_write_requests,
    io_write
FROM sys.schema_table_statistics_with_buffer
WHERE table_schema = DATABASE()
ORDER BY io_read DESC
LIMIT 20;
```

查看未使用索引：

```sql
-- 查看未被使用过的索引
SELECT
    object_schema,
    object_name,
    index_name
FROM sys.schema_unused_indexes
WHERE object_schema = DATABASE()
ORDER BY object_name, index_name;
```

查看冗余索引：

```sql
-- 查看冗余索引建议
SELECT
    table_schema,
    table_name,
    redundant_index_name,
    dominant_index_name
FROM sys.schema_redundant_indexes
WHERE table_schema = DATABASE();
```

查看慢 SQL 模板：

```sql
-- 查看总耗时较高的 SQL 模板
SELECT
    query,
    db,
    exec_count,
    total_latency,
    avg_latency,
    rows_examined,
    rows_sent
FROM sys.statement_analysis
WHERE db = DATABASE()
ORDER BY total_latency DESC
LIMIT 20;
```

查看当前锁等待：

```sql
-- 查看当前 InnoDB 锁等待
SELECT
    waiting_pid,
    waiting_query,
    blocking_pid,
    blocking_query,
    wait_age,
    locked_table,
    locked_index
FROM sys.innodb_lock_waits
ORDER BY wait_age DESC;
```

查看当前连接：

```sql
-- 查看当前会话和 SQL
SELECT
    thd_id,
    conn_id,
    user,
    db,
    command,
    time,
    state,
    current_statement
FROM sys.session
WHERE conn_id IS NOT NULL
ORDER BY time DESC;
```

`sys` Schema 使用建议：

1. 日常巡检优先使用 `sys` 视图，结果更直观。
2. 排查慢 SQL 可查看 `sys.statement_analysis`。
3. 排查锁等待可查看 `sys.innodb_lock_waits`。
4. 排查无用索引可查看 `schema_unused_indexes`，但删除前必须观察足够长时间。
5. `sys` 底层依赖 `performance_schema`，如果性能采集未开启，部分视图可能没有数据。

### SHOW PROCESSLIST

`SHOW PROCESSLIST` 用于查看当前 MySQL 连接、线程状态、执行时间和正在执行的 SQL。它是排查连接数过多、SQL 卡住、锁等待、慢查询的常用命令。

基础使用：

```sql
-- 查看当前连接列表
SHOW PROCESSLIST;
```

查看完整 SQL：

```sql
-- 查看完整 SQL 内容
SHOW FULL PROCESSLIST;
```

通过 `information_schema.PROCESSLIST` 查询：

```sql
-- 查询当前非空闲连接
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC;
```

查看长时间运行 SQL：

```sql
-- 查看执行超过 10 秒的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
  AND time >= 10
ORDER BY time DESC;
```

终止异常连接：

```sql
-- 终止指定连接，执行前必须确认连接来源和 SQL
KILL 12345;
```

常见 `command` 说明：

| command       | 说明                  |
| ------------- | --------------------- |
| `Sleep`       | 空闲连接              |
| `Query`       | 正在执行 SQL          |
| `Connect`     | 正在连接              |
| `Binlog Dump` | 主库向从库发送 Binlog |
| `Daemon`      | 后台线程              |
| `Killed`      | 线程正在被终止        |

常见 `state` 说明：

| state                             | 说明                                         |
| --------------------------------- | -------------------------------------------- |
| `Sending data`                    | 正在读取、过滤或发送数据，不一定只是网络发送 |
| `Copying to tmp table`            | 正在复制到临时表                             |
| `Creating sort index`             | 正在排序                                     |
| `Waiting for table metadata lock` | 等待元数据锁                                 |
| `Locked`                          | 等待锁                                       |
| `statistics`                      | 优化器统计阶段                               |

`SHOW PROCESSLIST` 使用建议：

1. 连接数异常时先查看来源和状态。
2. SQL 慢时查看是否正在执行、等待锁或等待元数据锁。
3. 终止线程前必须确认业务影响。
4. 大量 `Sleep` 连接要检查连接池配置。
5. 大量 `Query` 且时间长要结合慢查询和锁等待分析。

### SHOW ENGINE INNODB STATUS

`SHOW ENGINE INNODB STATUS` 用于查看 InnoDB 内部状态，是排查死锁、锁等待、长事务、Buffer Pool 和 IO 问题的重要命令。

执行命令：

```sql
-- 查看 InnoDB 状态
SHOW ENGINE INNODB STATUS\G
```

重点区域：

| 区域                       | 说明                   |
| -------------------------- | ---------------------- |
| `LATEST DETECTED DEADLOCK` | 最近一次死锁详情       |
| `TRANSACTIONS`             | 当前事务和锁等待       |
| `FILE I/O`                 | 文件 IO 状态           |
| `BUFFER POOL AND MEMORY`   | Buffer Pool 和内存状态 |
| `ROW OPERATIONS`           | 行操作和后台线程状态   |
| `SEMAPHORES`               | 内部等待和争用         |

死锁分析重点：

```text
分析 LATEST DETECTED DEADLOCK 时重点看：

1. 哪两个事务互相等待
2. 每个事务执行的 SQL
3. 每个事务持有什么锁
4. 每个事务等待什么锁
5. MySQL 最终回滚了哪个事务
6. 是否存在加锁顺序不一致
7. 是否存在索引缺失导致锁范围扩大
```

事务分析重点：

```text
分析 TRANSACTIONS 时重点看：

1. 是否存在长事务
2. 是否有事务长时间 ACTIVE
3. 是否有 lock wait
4. 事务正在执行什么 SQL
5. 是否有大量 Undo Log
```

常见使用场景：

1. 排查死锁。
2. 排查锁等待。
3. 查看长事务。
4. 查看 Buffer Pool 状态。
5. 查看 InnoDB IO 状态。
6. 查看后台线程工作状态。

使用建议：

1. 死锁发生后尽快保存输出内容。
2. 死锁日志只保留最近一次，后续死锁会覆盖前一次。
3. 锁等待问题要结合 `PROCESSLIST` 和 `INNODB_TRX` 一起分析。
4. 不要只看结论，要分析事务 SQL 和加锁顺序。
5. 频繁死锁应从索引、事务顺序和业务并发模型治理。

### SHOW VARIABLES

`SHOW VARIABLES` 用于查看 MySQL 系统变量，包括配置参数、路径、字符集、连接数、日志、复制、InnoDB 参数等。

查看全部变量：

```sql
-- 查看全部系统变量
SHOW VARIABLES;
```

按名称模糊查询：

```sql
-- 查看连接数配置
SHOW VARIABLES LIKE 'max_connections';

-- 查看字符集配置
SHOW VARIABLES LIKE 'character_set%';

-- 查看排序规则配置
SHOW VARIABLES LIKE 'collation%';

-- 查看 InnoDB Buffer Pool
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- 查看 Binlog 配置
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_format';

-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

查看变量来源和作用域：

```sql
-- 查看系统变量的当前值
SELECT
    VARIABLE_NAME,
    VARIABLE_VALUE
FROM performance_schema.global_variables
WHERE VARIABLE_NAME IN (
    'max_connections',
    'innodb_buffer_pool_size',
    'binlog_format',
    'slow_query_log'
);
```

临时修改全局变量：

```sql
-- 临时修改最大连接数，重启后可能失效
SET GLOBAL max_connections = 1000;
```

修改当前会话变量：

```sql
-- 修改当前会话 SQL 安全更新模式
SET SESSION sql_safe_updates = 1;
```

`SHOW VARIABLES` 使用建议：

1. 用于确认当前 MySQL 实际运行参数。
2. 配置文件修改后，要用 `SHOW VARIABLES` 验证是否生效。
3. `SET GLOBAL` 修改通常只影响新连接，且重启后可能丢失。
4. 重要参数修改应固化到配置文件。
5. 参数优化前后都应记录变量值。

### SHOW STATUS

`SHOW STATUS` 用于查看 MySQL 运行状态计数器，例如连接数、查询数、事务数、缓存命中、临时表、InnoDB 读写等。它适合做实时诊断和监控指标采集。

查看全部状态：

```sql
-- 查看全部状态变量
SHOW GLOBAL STATUS;
```

查看连接状态：

```sql
-- 查看连接相关状态
SHOW GLOBAL STATUS LIKE 'Threads%';
SHOW GLOBAL STATUS LIKE 'Connections';
SHOW GLOBAL STATUS LIKE 'Max_used_connections';
```

查看查询和事务：

```sql
-- 查看查询数量
SHOW GLOBAL STATUS LIKE 'Questions';
SHOW GLOBAL STATUS LIKE 'Queries';

-- 查看事务提交和回滚
SHOW GLOBAL STATUS LIKE 'Com_commit';
SHOW GLOBAL STATUS LIKE 'Com_rollback';
```

查看临时表：

```sql
-- 查看临时表创建情况
SHOW GLOBAL STATUS LIKE 'Created_tmp%';
```

查看 InnoDB Buffer Pool：

```sql
-- 查看 Buffer Pool 逻辑读和物理读
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';
```

查看 InnoDB 行操作：

```sql
-- 查看 InnoDB 行操作统计
SHOW GLOBAL STATUS LIKE 'Innodb_rows%';
```

计算 QPS：

```text
QPS = 两次 Questions 差值 / 采样间隔秒数
```

计算 Buffer Pool 命中率：

```text
Buffer Pool 命中率 = 1 - Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests
```

`SHOW STATUS` 使用建议：

1. 状态值多为累计计数器，需要两次采样计算速率。
2. 排查性能问题时，结合 CPU、IO、慢查询和连接状态一起分析。
3. 临时表、磁盘临时表数量高时，应排查排序、分组、去重 SQL。
4. 回滚次数异常高时，应排查业务异常、死锁和约束冲突。
5. 监控系统通常会定期采集这些状态值。

### mysqladmin

`mysqladmin` 是 MySQL 自带命令行管理工具，可以执行 ping、查看状态、刷新日志、关闭服务、查看变量等操作。它适合脚本化巡检和简单运维。

检查 MySQL 是否存活：

```bash
# 检查 MySQL 是否可用
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  ping
```

查看简要状态：

```bash
# 查看 MySQL 简要状态
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  status
```

持续查看扩展状态：

```bash
# 每 5 秒查看一次扩展状态
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  extended-status \
  -i 5
```

查看变量：

```bash
# 查看 MySQL 变量
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  variables
```

刷新日志：

```bash
# 刷新日志，常用于日志轮转后
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  flush-logs
```

关闭 MySQL：

```bash
# 关闭 MySQL，生产环境谨慎使用
mysqladmin \
  -h 127.0.0.1 \
  -P 3306 \
  -u root \
  -p \
  shutdown
```

`mysqladmin` 使用建议：

1. 适合健康检查和简单状态查看。
2. 生产环境不要随意使用 `shutdown`。
3. 脚本中不要明文写密码，建议使用配置文件或安全凭据管理。
4. `ping` 只能证明 MySQL 有响应，不代表业务 SQL 正常。
5. 监控系统可以使用更专业的 exporter 替代简单脚本。

### mysqlbinlog

`mysqlbinlog` 用于解析 MySQL Binlog。它常用于数据恢复、误操作排查、复制问题分析和增量备份。Binlog 是二进制文件，不能直接用普通文本工具准确阅读。

查看 Binlog 内容：

```bash
# 解析 Binlog 文件
mysqlbinlog \
  /var/lib/mysql/mysql-bin.000120 \
  | less
```

查看 ROW 模式详细变更：

```bash
# 解码 ROW 模式下的行变更
mysqlbinlog \
  --base64-output=DECODE-ROWS \
  -vv \
  /var/lib/mysql/mysql-bin.000120 \
  | less
```

按时间范围解析：

```bash
# 按时间范围解析 Binlog
mysqlbinlog \
  --start-datetime="2026-05-11 10:00:00" \
  --stop-datetime="2026-05-11 11:00:00" \
  /var/lib/mysql/mysql-bin.000120 \
  > binlog_20260511_10_11.sql
```

按位置范围解析：

```bash
# 按位置范围解析 Binlog
mysqlbinlog \
  --start-position=123456 \
  --stop-position=789012 \
  /var/lib/mysql/mysql-bin.000120 \
  > binlog_pos.sql
```

从远程服务器读取 Binlog：

```bash
# 从远程 MySQL 读取 Binlog
mysqlbinlog \
  --read-from-remote-server \
  --host=10.20.30.10 \
  --port=3306 \
  --user=backup_user \
  --password \
  mysql-bin.000120 \
  > mysql-bin.000120.sql
```

持续备份 Binlog：

```bash
# 持续拉取远程 Binlog 原始文件
mysqlbinlog \
  --read-from-remote-server \
  --raw \
  --stop-never \
  --host=10.20.30.10 \
  --port=3306 \
  --user=backup_user \
  --password \
  mysql-bin.000120
```

`mysqlbinlog` 使用建议：

1. ROW 模式分析要加 `--base64-output=DECODE-ROWS -vv`。
2. 时间点恢复前先解析到文件并人工确认。
3. 按位置恢复比按时间恢复更精确。
4. 使用时间范围时要确认服务器时区。
5. Binlog 中可能包含敏感数据，解析文件要控制权限。
6. 不要直接把未检查的 Binlog 回放到生产库。

### pt 工具集

`pt` 工具集通常指 Percona Toolkit，提供 MySQL 运维常用工具，例如慢查询分析、在线 DDL、主从一致性校验、表归档、重复索引检查等。

常用工具：

| 工具                       | 用途               |
| -------------------------- | ------------------ |
| `pt-query-digest`          | 分析慢查询日志     |
| `pt-online-schema-change`  | 在线变更表结构     |
| `pt-table-checksum`        | 校验主从数据一致性 |
| `pt-table-sync`            | 修复主从数据差异   |
| `pt-archiver`              | 归档或清理历史数据 |
| `pt-duplicate-key-checker` | 检查重复索引       |
| `pt-kill`                  | 按规则终止异常 SQL |

分析慢查询：

```bash
# 分析慢查询日志并生成报告
pt-query-digest \
  /var/log/mysql/mysql-slow.log \
  > slow_report.txt
```

在线添加字段：

```bash
# 使用 pt-online-schema-change 在线添加字段
pt-online-schema-change \
  --alter "ADD COLUMN channel_code VARCHAR(32) DEFAULT NULL COMMENT '渠道编码'" \
  D=mall_order,t=order_info \
  --host=127.0.0.1 \
  --port=3306 \
  --user=ddl_user \
  --password \
  --execute
```

检查重复索引：

```bash
# 检查重复索引
pt-duplicate-key-checker \
  --host=127.0.0.1 \
  --port=3306 \
  --user=readonly_user \
  --password
```

归档历史数据：

```bash
# 将历史订单归档到历史表，执行前必须在测试环境验证
pt-archiver \
  --source h=127.0.0.1,P=3306,u=archive_user,p=密码,D=mall_order,t=order_info \
  --dest h=127.0.0.1,P=3306,u=archive_user,p=密码,D=mall_order,t=order_info_history \
  --where "created_at < DATE_SUB(NOW(), INTERVAL 6 MONTH)" \
  --limit 1000 \
  --commit-each \
  --statistics
```

主从一致性校验：

```bash
# 校验主从数据一致性，生产执行前必须评估影响
pt-table-checksum \
  --host=10.20.30.10 \
  --port=3306 \
  --user=check_user \
  --password \
  --databases=mall_order
```

`pt` 工具使用建议：

1. 执行前必须在测试环境验证。
2. 在线 DDL、归档、校验都可能产生额外负载。
3. 大表操作应在低峰期执行。
4. 工具账号应最小权限授权。
5. `pt-table-sync` 会修改数据，必须非常谨慎。
6. 所有工具执行命令和输出应留档，便于审计和复盘。

## 开发环境与生产环境差异

开发环境和生产环境在数据量、权限、安全、配置、性能、备份和发布流程上差异很大。开发环境能正常运行，不代表生产环境稳定可靠。项目设计和测试时必须主动考虑这些差异。

### 配置差异

开发环境通常为了方便调试，会开启 SQL 日志、自动建表、宽松权限和较小连接池。生产环境更关注稳定、安全、性能和可审计。

常见配置差异：

| 配置项       | 开发环境                   | 生产环境                         |
| ------------ | -------------------------- | -------------------------------- |
| SQL 日志     | 可开启详细 SQL             | 谨慎开启，避免日志过大和泄露数据 |
| DDL 自动更新 | 可能使用 `ddl-auto=update` | 禁止自动改表，使用变更脚本       |
| 连接池       | 较小即可                   | 按实例数和数据库承载能力配置     |
| 慢查询       | 可设置较低阈值             | 长期开启，阈值按业务要求         |
| Binlog       | 可不开启                   | 建议开启                         |
| 字符集       | 应与生产一致               | 统一 `utf8mb4`                   |
| 时区         | 容易使用本机默认           | 必须统一                         |

Spring Boot 开发环境示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mall_user_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: Root_123456

  jpa:
    hibernate:
      # 仅开发环境可考虑，生产环境禁用
      ddl-auto: update

mybatis-plus:
  configuration:
    # 开发环境可以输出 SQL
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

生产环境示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-prod.example.com:3306/mall_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
    username: mall_app
    password: ${MYSQL_PASSWORD}

  jpa:
    hibernate:
      # 生产禁止自动变更表结构
      ddl-auto: none

mybatis-plus:
  configuration:
    # 生产环境不使用 stdout 打印 SQL
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

配置差异建议：

1. 开发、测试、预发、生产应使用独立配置。
2. 生产数据库密码必须通过环境变量、配置中心或密钥系统注入。
3. 生产禁止自动 DDL。
4. 生产 SQL 日志应脱敏并控制日志量。
5. 所有环境的字符集、时区、SQL 模式应尽量一致。
6. 配置差异必须文档化，避免上线后才暴露问题。

### 数据量差异

开发环境数据量通常很小，很多 SQL 在开发环境执行很快，但生产环境数据量大后可能变成慢查询。数据量差异是很多线上性能问题的根源。

常见问题：

```text
开发环境：
order_info 表 1000 行
SQL 执行耗时 5ms

生产环境：
order_info 表 2 亿行
同样 SQL 因为缺少索引或深分页，执行耗时 10s
```

开发环境容易忽略的问题：

| 问题       | 原因                   |
| ---------- | ---------------------- |
| 缺索引     | 小数据量全表扫描也很快 |
| 深分页     | 小表看不出性能问题     |
| 大字段查询 | 数据少时传输成本不明显 |
| 分组排序   | 小数据集临时表成本低   |
| 锁等待     | 单人开发没有并发冲突   |
| 批量任务   | 小数据量不会形成大事务 |
| 归档清理   | 没有历史数据压力       |

构造测试数据：

```sql
-- 检查表数据量
SELECT COUNT(*) AS order_count FROM order_info;

-- 查看按月份数据分布
SELECT
    DATE_FORMAT(created_at, '%Y-%m') AS month_value,
    COUNT(*) AS row_count
FROM order_info
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month_value;
```

数据量差异建议：

1. 性能测试环境应尽量接近生产数据量。
2. 核心 SQL 必须在大数据量下验证执行计划。
3. 列表、搜索、报表、导出接口必须重点压测。
4. 不要用开发环境耗时判断生产性能。
5. 生产上线前应确认索引和数据分布。
6. 大表设计要提前考虑归档、分区或拆分。

### 权限差异

开发环境通常使用高权限账号，生产环境应严格执行最小权限原则。权限差异会导致开发环境能执行的 SQL 到生产环境失败，也会带来安全风险。

开发环境常见权限：

```text
root 权限
DDL 权限
DML 权限
所有库表访问权限
文件导入导出权限
```

生产应用账号推荐权限：

```sql
-- 创建生产应用账号
CREATE USER 'mall_app'@'10.20.30.%'
IDENTIFIED BY 'Strong_App_Password';

-- 授予指定业务库的增删改查权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'mall_app'@'10.20.30.%';
```

生产只读账号：

```sql
-- 创建只读账号
CREATE USER 'mall_readonly'@'10.20.30.%'
IDENTIFIED BY 'Strong_Readonly_Password';

-- 授予只读权限
GRANT SELECT
ON mall_order.*
TO 'mall_readonly'@'10.20.30.%';
```

生产 DDL 账号：

```sql
-- 创建 DDL 变更账号
CREATE USER 'ddl_user'@'10.20.30.%'
IDENTIFIED BY 'Strong_Ddl_Password';

-- 授予结构变更权限，具体范围按流程控制
GRANT ALTER, CREATE, DROP, INDEX
ON mall_order.*
TO 'ddl_user'@'10.20.30.%';
```

查看权限：

```sql
-- 查看用户权限
SHOW GRANTS FOR 'mall_app'@'10.20.30.%';
```

权限差异建议：

1. 生产应用账号不应使用 root。
2. 应用账号不应拥有 DDL 权限。
3. 只读服务使用只读账号。
4. 备份账号、复制账号、运维账号应分开。
5. 生产账号权限应定期巡检。
6. 开发环境也应尽量模拟生产权限，避免上线后权限不足。

### 性能差异

开发环境和生产环境的性能差异来自硬件、数据量、并发、网络、参数配置、索引、缓存命中率等多个方面。生产环境有真实并发和真实数据分布，性能问题更复杂。

常见性能差异：

| 维度     | 开发环境           | 生产环境                   |
| -------- | ------------------ | -------------------------- |
| 数据量   | 小                 | 大                         |
| 并发     | 低                 | 高                         |
| 硬件     | 本地或测试机       | 专用服务器或云实例         |
| 网络     | 本机或内网简单链路 | 负载均衡、防火墙、跨区网络 |
| 缓存     | 影响不明显         | Buffer Pool 命中率重要     |
| SQL      | 少量测试           | 高频调用                   |
| 锁竞争   | 很少               | 明显                       |
| 主从延迟 | 通常无             | 可能存在                   |

性能验证 SQL：

```sql
-- 查看核心 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

查看慢查询：

```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

性能差异建议：

1. 核心 SQL 必须在接近生产的数据量下验证。
2. 不要只用开发环境本地 MySQL 评估性能。
3. 生产上线前应关注慢查询、索引、连接池和事务。
4. 压测要覆盖列表、搜索、导出、批量任务和高并发写入。
5. 生产环境性能问题应通过监控和日志定位，不要盲目改参数。
6. 大表 SQL 必须执行 `EXPLAIN` 审核。

### 安全差异

生产环境安全要求远高于开发环境。生产数据库通常包含真实用户信息、订单、支付、文件、日志等敏感数据，必须严格保护。

常见安全差异：

| 安全项     | 开发环境       | 生产环境           |
| ---------- | -------------- | ------------------ |
| 数据敏感性 | 测试数据       | 真实敏感数据       |
| 账号权限   | 可能较宽松     | 最小权限           |
| 网络访问   | 本地可连       | 白名单和安全组     |
| 密码管理   | 可能本地配置   | 密钥系统或配置中心 |
| SQL 审核   | 较宽松         | 必须审核           |
| 数据导出   | 较随意         | 需要审批和脱敏     |
| 日志       | 可打印详细参数 | 敏感字段脱敏       |

敏感字段示例：

```text
手机号
邮箱
身份证号
银行卡号
支付流水
收货地址
访问令牌
密码哈希
第三方密钥
```

生产安全建议：

1. 生产数据导出必须审批。
2. 导出敏感数据必须脱敏。
3. 数据库不允许公网裸露访问。
4. 生产账号必须最小权限。
5. 密码、密钥不能写在代码仓库。
6. 日志中不能明文打印敏感字段。
7. 重要操作应有审计日志。

### 备份策略差异

开发环境通常不需要严格备份，生产环境必须有完整备份策略。生产备份不只是保存文件，还要验证能否恢复。

备份策略差异：

| 项目     | 开发环境   | 生产环境         |
| -------- | ---------- | ---------------- |
| 全量备份 | 可选       | 必须             |
| Binlog   | 可选       | 建议开启并备份   |
| 增量备份 | 通常不需要 | 大库建议使用     |
| 备份保留 | 短期即可   | 按业务和合规要求 |
| 异地备份 | 通常不需要 | 必须考虑         |
| 恢复演练 | 很少       | 定期执行         |
| 加密     | 可选       | 敏感数据必须考虑 |

生产备份建议：

```text
1. 每日全量备份
2. 开启 Binlog 并持续备份
3. 备份文件异地保存
4. 备份文件加密
5. 设置合理保留周期
6. 定期恢复演练
7. 备份失败必须告警
```

查看 Binlog：

```sql
-- 查看 Binlog 是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看当前 Binlog 文件和位置
SHOW MASTER STATUS;
```

备份策略建议：

1. 生产环境必须明确 RPO 和 RTO。
2. 只备份不验证等于没有可靠备份。
3. Binlog 是时间点恢复的关键。
4. 备份文件不应只保存在数据库服务器本机。
5. 备份账号应单独创建并最小授权。
6. 数据误删恢复依赖备份和 Binlog 的完整性。

### 发布流程差异

开发环境发布通常灵活，生产环境发布必须经过构建、测试、审核、灰度、监控和回滚流程。数据库变更尤其需要和应用发布解耦，避免版本不兼容。

常见发布流程差异：

| 阶段       | 开发环境         | 生产环境             |
| ---------- | ---------------- | -------------------- |
| 代码发布   | 开发者自行发布   | 标准流水线           |
| 数据库变更 | 可手动执行       | 变更审核             |
| 配置变更   | 本地修改         | 配置中心和审批       |
| 回滚       | 简单重启或改代码 | 需要明确回滚方案     |
| 验证       | 自测             | 自动化测试和人工验证 |
| 监控       | 可选             | 必须观察             |
| 灰度       | 通常无           | 核心系统建议灰度     |

生产数据库变更发布流程：

```text
1. 开发提交数据库变更脚本
2. 测试环境执行验证
3. 预发环境执行验证
4. 评估影响范围和回滚方案
5. 提交生产变更审批
6. 低峰窗口执行变更
7. 执行后验证表结构、数据和核心 SQL
8. 发布应用版本
9. 观察日志、慢查询、错误率和核心指标
10. 记录变更结果
```

兼容性发布建议：

```text
字段改名不要直接改：

1. 新增新字段
2. 应用双写新旧字段
3. 回填历史数据
4. 应用切换读取新字段
5. 观察无异常
6. 删除旧字段
```

发布流程建议：

1. 生产发布必须有回滚方案。
2. 数据库变更应先于依赖新字段的应用发布。
3. 删除字段、删除索引应放在最后阶段。
4. 应用发布后要观察慢查询和错误日志。
5. 数据库变更脚本必须版本管理。
6. 生产变更应保留执行记录和验证结果。

## MySQL 8 新特性应用

MySQL 8 相比 MySQL 5.7 提供了大量面向开发和运维的新能力，例如窗口函数、CTE、递归查询、JSON 增强、函数索引、降序索引、隐藏索引、`EXPLAIN ANALYZE`、原子 DDL 和角色权限。这些特性可以明显提升 SQL 表达能力、查询可读性、诊断能力和权限管理能力。

本节示例默认使用以下订单表：

```sql
-- MySQL 8 新特性示例订单表
CREATE TABLE IF NOT EXISTS mysql8_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已退款',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    order_attr JSON DEFAULT NULL COMMENT '订单扩展属性',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL 8 新特性订单表示例';
```

### 窗口函数

窗口函数用于在不压缩明细行的情况下，对一组数据进行排名、累计、分组统计、前后行比较等操作。它和 `GROUP BY` 的区别是：`GROUP BY` 会把多行聚合成一行，而窗口函数保留原始行。

常用窗口函数：

| 函数             | 说明                             |
| ---------------- | -------------------------------- |
| `ROW_NUMBER()`   | 按排序生成连续行号               |
| `RANK()`         | 排名，相同值并列，后续排名跳号   |
| `DENSE_RANK()`   | 排名，相同值并列，后续排名不跳号 |
| `LAG()`          | 获取上一行数据                   |
| `LEAD()`         | 获取下一行数据                   |
| `SUM() OVER()`   | 窗口内求和                       |
| `COUNT() OVER()` | 窗口内计数                       |
| `AVG() OVER()`   | 窗口内求平均                     |

查询每个用户的订单排名：

```sql
-- 查询每个用户订单金额排名
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at,
    ROW_NUMBER() OVER (
        PARTITION BY user_id
        ORDER BY pay_amount DESC, id DESC
    ) AS row_num
FROM mysql8_order_info
WHERE order_status = 1;
```

查询每个用户累计支付金额：

```sql
-- 按用户统计订单累计支付金额
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at,
    SUM(pay_amount) OVER (
        PARTITION BY user_id
        ORDER BY created_at ASC, id ASC
    ) AS cumulative_pay_amount
FROM mysql8_order_info
WHERE order_status = 1;
```

查询订单金额占用户总支付金额比例：

```sql
-- 计算每笔订单金额占用户总金额的比例
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    SUM(pay_amount) OVER (PARTITION BY user_id) AS user_total_amount,
    ROUND(
        pay_amount / NULLIF(SUM(pay_amount) OVER (PARTITION BY user_id), 0) * 100,
        2
    ) AS amount_rate
FROM mysql8_order_info
WHERE order_status = 1;
```

窗口函数使用建议：

1. 适合排名、Top N、累计统计、同比环比、前后行比较。
2. 窗口函数不会减少明细行数量。
3. `PARTITION BY` 用于定义分组窗口。
4. `ORDER BY` 用于定义窗口内排序。
5. 大数据量窗口计算可能产生排序和临时表，应结合索引和数据范围控制。
6. 对高频复杂统计，窗口函数不一定替代汇总表。

### CTE 公用表表达式

CTE 是 Common Table Expression，公用表表达式。它使用 `WITH` 定义临时结果集，提升复杂 SQL 的可读性和可维护性。MySQL 8 支持普通 CTE 和递归 CTE。

普通 CTE 示例：

```sql
-- 使用 CTE 拆分查询逻辑
WITH paid_order AS (
    SELECT
        id,
        order_no,
        user_id,
        pay_amount,
        created_at
    FROM mysql8_order_info
    WHERE order_status = 1
),
user_order_summary AS (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM paid_order
    GROUP BY user_id
)
SELECT
    user_id,
    order_count,
    total_pay_amount
FROM user_order_summary
WHERE total_pay_amount >= 1000.00
ORDER BY total_pay_amount DESC;
```

多个 CTE 联合使用：

```sql
-- 使用多个 CTE 分别统计用户订单和最近下单时间
WITH user_total AS (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        SUM(pay_amount) AS total_pay_amount
    FROM mysql8_order_info
    WHERE order_status = 1
    GROUP BY user_id
),
user_latest_order AS (
    SELECT
        user_id,
        MAX(created_at) AS latest_order_time
    FROM mysql8_order_info
    WHERE order_status = 1
    GROUP BY user_id
)
SELECT
    t.user_id,
    t.order_count,
    t.total_pay_amount,
    l.latest_order_time
FROM user_total AS t
JOIN user_latest_order AS l ON t.user_id = l.user_id
ORDER BY t.total_pay_amount DESC;
```

CTE 替代复杂子查询：

```sql
-- 使用 CTE 替代嵌套子查询，提高 SQL 可读性
WITH high_value_user AS (
    SELECT
        user_id
    FROM mysql8_order_info
    WHERE order_status = 1
    GROUP BY user_id
    HAVING SUM(pay_amount) >= 5000.00
)
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.pay_amount,
    o.created_at
FROM mysql8_order_info AS o
JOIN high_value_user AS u ON o.user_id = u.user_id
WHERE o.order_status = 1
ORDER BY o.created_at DESC;
```

CTE 使用建议：

1. 适合拆解复杂查询逻辑。
2. 多段统计、复杂筛选、重复子查询可以考虑使用 CTE。
3. CTE 提高可读性，但不一定天然提升性能。
4. 高频 SQL 仍需通过 `EXPLAIN` 和 `EXPLAIN ANALYZE` 验证。
5. 如果 CTE 结果集很大，可能产生额外中间结果成本。
6. 复杂业务报表可以使用 CTE，但高频报表更推荐汇总表或离线计算。

### 递归查询

递归查询是递归 CTE 的应用，常用于树形结构查询，例如菜单、部门、分类、区域、组织架构等。MySQL 8 之前通常需要应用层递归或多次查询，MySQL 8 可以用 `WITH RECURSIVE` 在 SQL 层完成。

部门表示例：

```sql
-- 部门表，使用 parent_id 构建树形结构
CREATE TABLE IF NOT EXISTS mysql8_sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级部门ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    dept_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_parent_sort (parent_id, dept_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL 8 递归查询部门表';
```

查询完整部门树：

```sql
-- 使用递归 CTE 查询完整部门树
WITH RECURSIVE dept_tree AS (
    SELECT
        id,
        parent_id,
        dept_name,
        dept_sort,
        1 AS dept_level,
        CAST(dept_name AS CHAR(1000)) AS dept_path
    FROM mysql8_sys_dept
    WHERE parent_id = 0
      AND dept_status = 1

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_name,
        d.dept_sort,
        dt.dept_level + 1 AS dept_level,
        CONCAT(dt.dept_path, '/', d.dept_name) AS dept_path
    FROM mysql8_sys_dept AS d
    JOIN dept_tree AS dt ON d.parent_id = dt.id
    WHERE d.dept_status = 1
)
SELECT
    id,
    parent_id,
    dept_name,
    dept_level,
    dept_path
FROM dept_tree
ORDER BY dept_path ASC;
```

查询指定节点的所有子节点：

```sql
-- 查询指定部门及其所有子部门
WITH RECURSIVE child_dept AS (
    SELECT
        id,
        parent_id,
        dept_name,
        1 AS dept_level
    FROM mysql8_sys_dept
    WHERE id = 100

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_name,
        c.dept_level + 1 AS dept_level
    FROM mysql8_sys_dept AS d
    JOIN child_dept AS c ON d.parent_id = c.id
)
SELECT
    id,
    parent_id,
    dept_name,
    dept_level
FROM child_dept
ORDER BY dept_level ASC, id ASC;
```

查询指定节点的所有父节点：

```sql
-- 查询指定部门的所有上级部门
WITH RECURSIVE parent_dept AS (
    SELECT
        id,
        parent_id,
        dept_name,
        1 AS dept_level
    FROM mysql8_sys_dept
    WHERE id = 100

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_name,
        p.dept_level + 1 AS dept_level
    FROM mysql8_sys_dept AS d
    JOIN parent_dept AS p ON d.id = p.parent_id
)
SELECT
    id,
    parent_id,
    dept_name,
    dept_level
FROM parent_dept
ORDER BY dept_level DESC;
```

递归查询建议：

1. 适合层级不深的数据，例如菜单、部门、分类。
2. 必须避免循环引用，例如 A 的父级是 B，B 的父级又是 A。
3. 树层级过深时，应限制最大递归深度。
4. `parent_id` 必须建立索引。
5. 高频树查询建议应用层缓存。
6. 大规模树结构可以考虑闭包表、路径字段或专门图模型。

### JSON 增强

MySQL 8 对 JSON 支持更完善，提供了 JSON 字段、JSON 路径查询、JSON 修改、JSON 聚合、`JSON_TABLE`、函数索引等能力。JSON 适合存储扩展属性、非核心筛选字段和半结构化数据。

插入 JSON 数据：

```sql
-- 插入包含 JSON 扩展属性的订单
INSERT INTO mysql8_order_info (
    order_no,
    user_id,
    order_status,
    pay_amount,
    order_attr
) VALUES (
    'O202605110001',
    10001,
    1,
    199.90,
    JSON_OBJECT(
        'source', 'APP',
        'couponId', 90001,
        'receiver', JSON_OBJECT(
            'name', '张三',
            'city', '杭州'
        )
    )
);
```

查询 JSON 属性：

```sql
-- 查询 JSON 中的来源渠道
SELECT
    id,
    order_no,
    user_id,
    JSON_UNQUOTE(JSON_EXTRACT(order_attr, '$.source')) AS source,
    JSON_EXTRACT(order_attr, '$.couponId') AS coupon_id
FROM mysql8_order_info
WHERE order_attr IS NOT NULL;
```

使用简写操作符：

```sql
-- 使用 -> 和 ->> 读取 JSON 字段
SELECT
    id,
    order_no,
    order_attr -> '$.receiver.city' AS city_json,
    order_attr ->> '$.receiver.city' AS city_text
FROM mysql8_order_info;
```

修改 JSON 属性：

```sql
-- 修改 JSON 中的 source 属性
UPDATE mysql8_order_info
SET
    order_attr = JSON_SET(order_attr, '$.source', 'MINI_PROGRAM'),
    updated_at = NOW()
WHERE order_no = 'O202605110001';
```

删除 JSON 属性：

```sql
-- 删除 JSON 中的 couponId 属性
UPDATE mysql8_order_info
SET
    order_attr = JSON_REMOVE(order_attr, '$.couponId'),
    updated_at = NOW()
WHERE order_no = 'O202605110001';
```

使用 `JSON_TABLE` 将 JSON 数组转成关系表：

```sql
-- JSON_TABLE 示例：将 JSON 数组展开为多行
SELECT
    jt.product_id,
    jt.product_name,
    jt.buy_count
FROM JSON_TABLE(
    '[
        {"productId": 10001, "productName": "键盘", "buyCount": 1},
        {"productId": 10002, "productName": "鼠标", "buyCount": 2}
    ]',
    '$[*]' COLUMNS (
        product_id BIGINT PATH '$.productId',
        product_name VARCHAR(100) PATH '$.productName',
        buy_count INT PATH '$.buyCount'
    )
) AS jt;
```

JSON 使用建议：

1. JSON 适合扩展属性，不适合替代关系模型。
2. 高频查询字段不建议只放在 JSON 中。
3. 高频 JSON 路径查询可以考虑生成列或函数索引。
4. JSON 字段不适合无限增长，应控制结构和大小。
5. 重要业务字段应拆成独立列。
6. JSON 查询和更新应通过执行计划验证成本。

### 函数索引

MySQL 8 支持函数索引，可以对表达式结果建立索引。它适合优化对字段使用函数或表达式的查询，例如 `LOWER(username)`、`DATE(created_at)`、JSON 路径值等。

普通函数查询可能无法使用常规索引：

```sql
-- 对 username 使用 LOWER 函数，普通 username 索引可能无法直接使用
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE LOWER(username) = 'admin';
```

创建函数索引：

```sql
-- 为 LOWER(username) 创建函数索引
CREATE INDEX idx_lower_username
ON sys_user ((LOWER(username)));
```

使用函数索引查询：

```sql
-- 查询表达式需要与函数索引表达式一致
SELECT
    id,
    username,
    nickname
FROM sys_user
WHERE LOWER(username) = 'admin';
```

为 JSON 字段路径创建函数索引：

```sql
-- 为 JSON 中的 source 字段创建函数索引
CREATE INDEX idx_order_source
ON mysql8_order_info ((CAST(order_attr ->> '$.source' AS CHAR(32))));
```

查询 JSON source：

```sql
-- 使用 JSON 路径条件查询
SELECT
    id,
    order_no,
    user_id,
    pay_amount
FROM mysql8_order_info
WHERE CAST(order_attr ->> '$.source' AS CHAR(32)) = 'APP';
```

函数索引使用建议：

1. 适合无法避免函数查询的场景。
2. 查询表达式应与索引表达式保持一致。
3. 不要为低频表达式创建函数索引。
4. 函数索引会增加写入和维护成本。
5. 对 JSON 高频查询字段，更推荐将其设计为普通列或生成列。
6. 创建后必须使用 `EXPLAIN` 验证是否命中索引。

### 降序索引

MySQL 8 支持真正的降序索引。对于经常按时间倒序、ID 倒序查询的场景，可以使用降序索引优化排序。它常用于列表页、时间线、订单列表、日志列表等。

创建降序索引：

```sql
-- 为用户订单倒序列表创建降序索引
ALTER TABLE mysql8_order_info
  ADD KEY idx_user_created_desc (
      user_id,
      created_at DESC,
      id DESC
  );
```

适合的查询：

```sql
-- 查询用户最新订单列表
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM mysql8_order_info
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

查看执行计划：

```sql
-- 验证是否使用降序索引
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM mysql8_order_info
WHERE user_id = 10001
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

混合排序索引：

```sql
-- 创建混合排序索引
ALTER TABLE mysql8_order_info
  ADD KEY idx_status_created_asc_id_desc (
      order_status,
      created_at ASC,
      id DESC
  );
```

降序索引建议：

1. 适合固定倒序排序的高频查询。
2. 排序字段方向应和 SQL 中 `ORDER BY` 一致。
3. 联合索引中的排序方向要整体设计。
4. 低频排序不必单独创建降序索引。
5. 对列表页可以配合游标分页使用。
6. 创建后通过 `EXPLAIN` 检查是否减少 `Using filesort`。

### 隐藏索引

隐藏索引是 MySQL 8 提供的索引可见性控制能力。隐藏后的索引不会被优化器使用，但仍会维护。它适合在删除索引前进行灰度验证，降低误删索引风险。

查看索引：

```sql
-- 查看表索引
SHOW INDEX FROM mysql8_order_info;
```

隐藏索引：

```sql
-- 将索引设置为不可见
ALTER TABLE mysql8_order_info
  ALTER INDEX idx_status_created INVISIBLE;
```

恢复索引可见：

```sql
-- 将索引恢复为可见
ALTER TABLE mysql8_order_info
  ALTER INDEX idx_status_created VISIBLE;
```

检查索引可见性：

```sql
-- 查看索引是否可见
SELECT
    TABLE_NAME,
    INDEX_NAME,
    IS_VISIBLE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mysql8_order_info'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

隐藏索引验证流程：

```text
1. 确认索引疑似无用或重复
2. 将索引设置为 INVISIBLE
3. 观察慢查询、执行计划、CPU、IO
4. 如果出现性能问题，立即恢复 VISIBLE
5. 如果观察周期内无异常，再考虑 DROP INDEX
```

隐藏索引使用建议：

1. 删除索引前优先隐藏观察。
2. 隐藏索引仍然会维护，不能降低写入成本。
3. 隐藏索引用于验证优化器不使用该索引的影响。
4. 主键不能设置为隐藏索引。
5. 观察周期要覆盖业务高峰和定时任务。
6. 最终确认无用后，再执行 `DROP INDEX`。

### EXPLAIN ANALYZE

`EXPLAIN ANALYZE` 是 MySQL 8 提供的执行计划分析能力。它不仅展示优化器预估计划，还会实际执行 SQL，并输出真实执行耗时、实际行数和循环次数。因此它比普通 `EXPLAIN` 更适合验证 SQL 优化效果。

普通 `EXPLAIN`：

```sql
-- 查看优化器预估执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM mysql8_order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

`EXPLAIN ANALYZE`：

```sql
-- 查看真实执行过程和耗时
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    pay_amount,
    created_at
FROM mysql8_order_info
WHERE user_id = 10001
ORDER BY created_at DESC
LIMIT 20;
```

适合分析聚合查询：

```sql
-- 分析分组统计真实执行成本
EXPLAIN ANALYZE
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(pay_amount) AS total_pay_amount
FROM mysql8_order_info
WHERE order_status = 1
GROUP BY user_id
HAVING total_pay_amount >= 1000.00
ORDER BY total_pay_amount DESC
LIMIT 20;
```

使用前注意：

```text
EXPLAIN ANALYZE 会实际执行 SQL。

如果是 UPDATE、DELETE、INSERT 等写 SQL，不要直接在生产环境执行。
对 SELECT 也要注意：
1. 大查询会真实消耗资源
2. 可能读取大量数据
3. 可能造成锁等待或影响缓存
4. 生产环境应谨慎使用
```

`EXPLAIN ANALYZE` 使用建议：

1. 适合验证 SQL 优化前后真实效果。
2. 会实际执行 SQL，生产环境谨慎使用。
3. 大查询建议在测试环境或只读从库分析。
4. 重点关注实际行数和预估行数差异。
5. 如果预估行数严重不准，可以考虑更新统计信息。
6. 与普通 `EXPLAIN`、慢查询日志、索引信息结合使用。

### 原子 DDL

MySQL 8 支持原子 DDL，DDL 操作在数据字典层面具备原子性。也就是说，一个 DDL 操作要么完整成功，要么失败后回滚，不会留下部分完成的元数据状态。这提升了 DDL 在崩溃恢复和异常场景下的一致性。

常见 DDL 示例：

```sql
-- 创建表属于 DDL 操作
CREATE TABLE IF NOT EXISTS mysql8_atomic_ddl_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_biz_no (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子DDL示例表';
```

添加字段：

```sql
-- 添加字段
ALTER TABLE mysql8_atomic_ddl_demo
  ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注';
```

添加索引：

```sql
-- 添加索引
ALTER TABLE mysql8_atomic_ddl_demo
  ADD KEY idx_created_at (created_at);
```

删除表：

```sql
-- 删除表
DROP TABLE IF EXISTS mysql8_atomic_ddl_demo;
```

原子 DDL 的价值：

| 能力           | 说明                                |
| -------------- | ----------------------------------- |
| 元数据一致     | DDL 成功或失败状态更清晰            |
| 崩溃恢复更可靠 | DDL 执行中崩溃后能恢复到一致状态    |
| 减少残留文件   | 降低部分 DDL 失败后遗留异常对象概率 |
| 运维风险降低   | DDL 失败时更容易判断状态            |

需要注意：

```text
原子 DDL 不等于 DDL 没有风险。

它不能消除：
1. 大表 DDL 耗时
2. DDL 导致的锁等待
3. DDL 造成的主从延迟
4. DDL 消耗的 CPU、IO、磁盘空间
5. 字段删除、表删除造成的数据不可逆风险
```

原子 DDL 使用建议：

1. 原子 DDL 提升 DDL 一致性，但不能替代变更审核。
2. 大表 DDL 仍需评估锁、耗时、磁盘和复制延迟。
3. 生产变更仍需回滚方案。
4. 高风险 DDL 仍建议使用低峰窗口或在线变更工具。
5. DDL 完成后应验证表结构、索引和应用兼容性。

### 角色权限

MySQL 8 支持角色权限管理。角色可以理解为一组权限集合。通过角色可以减少重复授权，提高权限管理可维护性。常见做法是创建只读角色、读写角色、运维角色、备份角色，再把角色授予具体用户。

创建只读角色：

```sql
-- 创建只读角色
CREATE ROLE 'role_readonly';
```

给角色授权：

```sql
-- 授予指定库只读权限
GRANT SELECT
ON mall_order.*
TO 'role_readonly';
```

创建读写角色：

```sql
-- 创建读写角色
CREATE ROLE 'role_readwrite';

-- 授予读写权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'role_readwrite';
```

创建用户：

```sql
-- 创建应用用户
CREATE USER 'mall_app'@'10.20.30.%'
IDENTIFIED BY 'Mall_App_123456';

-- 创建只读用户
CREATE USER 'mall_readonly'@'10.20.30.%'
IDENTIFIED BY 'Mall_Readonly_123456';
```

将角色授予用户：

```sql
-- 授予读写角色
GRANT 'role_readwrite'
TO 'mall_app'@'10.20.30.%';

-- 授予只读角色
GRANT 'role_readonly'
TO 'mall_readonly'@'10.20.30.%';
```

设置默认角色：

```sql
-- 设置用户默认启用角色
SET DEFAULT ROLE 'role_readwrite'
TO 'mall_app'@'10.20.30.%';

SET DEFAULT ROLE 'role_readonly'
TO 'mall_readonly'@'10.20.30.%';
```

查看角色权限：

```sql
-- 查看角色权限
SHOW GRANTS FOR 'role_readonly';

-- 查看用户权限
SHOW GRANTS FOR 'mall_app'@'10.20.30.%';
```

回收角色：

```sql
-- 回收用户角色
REVOKE 'role_readwrite'
FROM 'mall_app'@'10.20.30.%';
```

删除角色：

```sql
-- 删除角色
DROP ROLE 'role_readonly';
```

角色权限使用建议：

1. 权限集合通过角色管理，用户只绑定角色。
2. 应用账号不应直接使用 root 权限。
3. 生产账号应遵循最小权限原则。
4. 只读、读写、备份、复制、DDL 账号应分开。
5. 授权后要设置默认角色，否则用户登录后可能未启用角色。
6. 定期审计用户、角色和权限关系。
7. 离职、系统下线或服务迁移后要及时回收权限。

## 项目实践检查清单

项目实践检查清单用于在数据库设计、开发、测试、上线和运维阶段统一检查标准。它不是一次性文档，而应作为代码评审、数据库变更审核、生产发布和故障复盘的固定依据。

检查清单建议覆盖以下阶段：

```text
1. 建表前：检查表结构、字段类型、主键、索引、字符集
2. 开发中：检查 SQL、事务、权限、安全
3. 测试中：检查数据量、执行计划、慢查询、并发
4. 上线前：检查备份、回滚、发布顺序、监控告警
5. 上线后：检查慢查询、连接数、锁等待、主从延迟、错误日志
```

### 表结构检查

表结构检查用于确认表设计是否符合业务语义、性能要求和团队规范。重点检查主键、字段类型、默认值、注释、字符集、审计字段和历史数据治理能力。

表结构基础检查项：

| 检查项   | 要求                                |
| -------- | ----------------------------------- |
| 表名     | 小写下划线，表达业务含义            |
| 表注释   | 必须填写，说明表用途                |
| 主键     | 每张表必须有主键                    |
| 字段名   | 小写下划线，含义清晰                |
| 字段注释 | 每个字段必须有注释                  |
| 字符集   | 统一使用 `utf8mb4`                  |
| 存储引擎 | 业务表使用 InnoDB                   |
| 金额字段 | 使用 `DECIMAL`                      |
| 时间字段 | 使用 `DATETIME`                     |
| 状态字段 | 使用 `TINYINT` 并注明枚举           |
| 逻辑删除 | 需要软删除的表统一使用 `deleted`    |
| 审计字段 | 建议包含 `created_at`、`updated_at` |
| 大字段   | 避免放在高频主表中                  |

推荐标准表结构：

```sql
-- 标准业务表示例
CREATE TABLE IF NOT EXISTS standard_biz_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    biz_name VARCHAR(128) NOT NULL COMMENT '业务名称',
    biz_status TINYINT NOT NULL DEFAULT 1 COMMENT '业务状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_no (biz_no),
    KEY idx_status_created (biz_status, created_at),
    KEY idx_deleted_created (deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准业务信息表';
```

查看表结构：

```sql
-- 查看建表语句
SHOW CREATE TABLE standard_biz_info;

-- 查看字段信息
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_KEY,
    EXTRA,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'standard_biz_info'
ORDER BY ORDINAL_POSITION;
```

检查缺少主键的表：

```sql
-- 检查当前库中没有主键的表
SELECT
    t.TABLE_NAME
FROM information_schema.TABLES AS t
LEFT JOIN information_schema.TABLE_CONSTRAINTS AS c
    ON t.TABLE_SCHEMA = c.TABLE_SCHEMA
   AND t.TABLE_NAME = c.TABLE_NAME
   AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND c.CONSTRAINT_NAME IS NULL;
```

检查缺少表注释的表：

```sql
-- 检查没有表注释的表
SELECT
    TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND (TABLE_COMMENT IS NULL OR TABLE_COMMENT = '');
```

检查缺少字段注释的字段：

```sql
-- 检查没有字段注释的字段
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (COLUMN_COMMENT IS NULL OR COLUMN_COMMENT = '')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

检查字符集：

```sql
-- 检查表字符集和排序规则
SELECT
    TABLE_NAME,
    TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
```

表结构检查建议：

1. 建表脚本必须经过评审。
2. 所有字段必须有注释。
3. 所有业务表必须有主键。
4. 金额、时间、状态、逻辑删除字段必须统一规范。
5. 高频表避免过宽。
6. 大字段应拆分到扩展表。
7. 表结构上线前必须在测试环境执行验证。

### 索引检查

索引检查用于确认索引是否满足核心查询、排序、关联和唯一约束要求，同时避免重复索引、无效索引、过宽索引和低价值索引。

索引基础检查项：

| 检查项     | 要求                             |
| ---------- | -------------------------------- |
| 主键索引   | 每张表必须有主键                 |
| 唯一索引   | 唯一业务字段必须建唯一索引       |
| 查询索引   | 高频查询条件应有索引             |
| 关联索引   | JOIN 字段必须有索引              |
| 排序索引   | 高频排序应和过滤条件组成联合索引 |
| 联合索引   | 遵循最左前缀原则                 |
| 重复索引   | 不允许长期保留重复索引           |
| 低效索引   | 避免为低选择性字段单独建索引     |
| 索引数量   | 不应无限增加                     |
| 大字段索引 | 谨慎使用完整大字段索引           |

查看索引：

```sql
-- 查看指定表索引
SHOW INDEX FROM standard_biz_info;
```

通过系统表查看索引：

```sql
-- 查看当前库所有索引
SELECT
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
```

检查重复索引可以优先使用 `sys` Schema：

```sql
-- 查看冗余索引
SELECT
    table_schema,
    table_name,
    redundant_index_name,
    dominant_index_name
FROM sys.schema_redundant_indexes
WHERE table_schema = DATABASE();
```

查看未使用索引：

```sql
-- 查看未使用索引，删除前必须长期观察并确认
SELECT
    object_schema,
    object_name,
    index_name
FROM sys.schema_unused_indexes
WHERE object_schema = DATABASE()
ORDER BY object_name, index_name;
```

检查索引字段顺序：

```sql
-- 查看联合索引字段顺序
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
```

索引检查建议：

1. 不为每个字段单独建索引。
2. 高频 SQL 必须通过 `EXPLAIN` 验证索引命中。
3. 联合索引字段顺序要结合等值、范围、排序和覆盖字段设计。
4. 删除索引前先设置为隐藏索引观察。
5. 低选择性字段单独建索引价值通常有限。
6. 大表加索引必须评估耗时、磁盘空间和主从延迟。
7. 索引变更要有回滚方案。

### SQL 检查

SQL 检查用于发现可能导致慢查询、索引失效、大量扫描、锁表、误更新、SQL 注入和可读性差的问题。所有核心 SQL 上线前都应经过执行计划检查。

SQL 基础检查项：

| 检查项   | 要求                         |
| -------- | ---------------------------- |
| 查询字段 | 禁止业务 SQL 使用 `SELECT *` |
| 查询条件 | 高频查询条件应命中索引       |
| 参数类型 | 避免隐式类型转换             |
| 函数使用 | 避免对索引字段套函数         |
| 分页方式 | 避免深分页                   |
| 排序字段 | 高频排序应有索引支持         |
| 更新删除 | 必须带明确 `WHERE` 条件      |
| 批量操作 | 必须分批执行                 |
| 动态 SQL | 禁止拼接用户输入             |
| 可读性   | 字段、条件、排序分行书写     |

不推荐写法：

```sql
-- 不推荐：SELECT *、条件不完整、分页不受控
SELECT
    *
FROM order_info
WHERE DATE(created_at) = '2026-05-11'
ORDER BY created_at DESC
LIMIT 100000, 20;
```

推荐写法：

```sql
-- 推荐：明确字段、时间范围、稳定排序、限制分页
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE created_at >= '2026-05-11 00:00:00'
  AND created_at <  '2026-05-12 00:00:00'
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

检查执行计划：

```sql
-- 检查 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

执行计划重点检查：

| 字段            | 检查重点                                     |
| --------------- | -------------------------------------------- |
| `type`          | 避免 `ALL` 全表扫描                          |
| `key`           | 是否命中预期索引                             |
| `rows`          | 扫描行数是否合理                             |
| `Extra`         | 是否出现 `Using temporary`、`Using filesort` |
| `filtered`      | 过滤比例是否合理                             |
| `possible_keys` | 是否有可选索引但未使用                       |

SQL 检查建议：

1. 核心 SQL 必须提供 `EXPLAIN` 结果。
2. 列表查询必须限制返回条数。
3. 时间查询使用左闭右开。
4. 大表查询必须带高选择性条件。
5. 更新删除前必须先查影响行数。
6. 用户输入必须使用参数绑定。
7. 复杂 SQL 应拆分或使用 CTE 提升可读性。
8. 高频报表统计应考虑汇总表或离线计算。

### 事务检查

事务检查用于避免大事务、长事务、锁等待、死锁、回滚时间过长和事务不生效。事务应尽量短，只包含必须强一致的数据库操作。

事务基础检查项：

| 检查项   | 要求                                            |
| -------- | ----------------------------------------------- |
| 事务范围 | 只包含必要数据库操作                            |
| 外部调用 | 事务中禁止 HTTP、RPC、MQ、文件上传              |
| 批量操作 | 禁止超大批量放在一个事务中                      |
| 锁范围   | 更新条件必须命中索引                            |
| 异常回滚 | Spring 建议配置 `rollbackFor = Exception.class` |
| 事务传播 | 明确传播行为，避免误用                          |
| 只读事务 | 查询服务可使用 `readOnly = true`                |
| 长事务   | 必须监控和限制                                  |
| 幂等     | 事务提交失败后重试要可控                        |
| 死锁     | 应用层应支持有限重试                            |

查看当前长事务：

```sql
-- 查看当前 InnoDB 长事务
SELECT
    trx_id,
    trx_state,
    trx_started,
    TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_seconds,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.INNODB_TRX
ORDER BY trx_started ASC;
```

查看锁等待：

```sql
-- 查看锁等待关系
SELECT
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread_id,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread_id,
    b.trx_query AS blocking_query
FROM information_schema.INNODB_LOCK_WAITS AS w
JOIN information_schema.INNODB_TRX AS r ON w.requesting_trx_id = r.trx_id
JOIN information_schema.INNODB_TRX AS b ON w.blocking_trx_id = b.trx_id;
```

事务设计示例：

```text
推荐事务流程：

1. 校验参数
2. 查询必要数据
3. 执行数据库写入
4. 提交事务
5. 事务外发送 MQ、调用外部接口、上传文件
```

不推荐事务流程：

```text
不推荐：

1. 开启事务
2. 更新订单
3. 调用支付接口
4. 调用库存接口
5. 发送 MQ
6. 上传文件
7. 提交事务

问题：
事务时间不可控，锁持有过久，容易造成连接堆积和锁等待。
```

事务检查建议：

1. 事务内 SQL 必须命中索引。
2. 不要在事务中做慢查询和大范围扫描。
3. 批量更新删除必须分批提交。
4. 高并发更新同一资源时要评估乐观锁或悲观锁。
5. 事务方法不要被同类内部调用导致注解失效。
6. 死锁异常要支持有限重试。
7. 长事务必须接入监控。

### 权限检查

权限检查用于确保数据库账号遵循最小权限原则，避免应用账号拥有高危权限，降低误操作和数据泄露风险。生产环境应区分应用账号、只读账号、备份账号、复制账号、DDL 账号和运维账号。

权限基础检查项：

| 账号类型  | 推荐权限                                              |
| --------- | ----------------------------------------------------- |
| 应用账号  | 指定库表 `SELECT`、`INSERT`、`UPDATE`、`DELETE`       |
| 只读账号  | 指定库表 `SELECT`                                     |
| 备份账号  | 备份所需权限                                          |
| 复制账号  | `REPLICATION SLAVE`、`REPLICATION CLIENT`             |
| DDL 账号  | 变更窗口使用，授予 `ALTER`、`CREATE`、`DROP`、`INDEX` |
| 运维账号  | 严格审批，避免长期共享                                |
| root 账号 | 禁止业务使用                                          |

查看用户：

```sql
-- 查看用户列表
SELECT
    user,
    host,
    account_locked,
    password_expired
FROM mysql.user
ORDER BY user, host;
```

查看权限：

```sql
-- 查看指定用户权限
SHOW GRANTS FOR 'mall_app'@'10.20.30.%';
```

创建应用账号：

```sql
-- 创建应用账号
CREATE USER 'mall_app'@'10.20.30.%'
IDENTIFIED BY 'Strong_App_Password';

-- 只授权业务库读写权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON mall_order.*
TO 'mall_app'@'10.20.30.%';
```

创建只读账号：

```sql
-- 创建只读账号
CREATE USER 'mall_readonly'@'10.20.30.%'
IDENTIFIED BY 'Strong_Readonly_Password';

GRANT SELECT
ON mall_order.*
TO 'mall_readonly'@'10.20.30.%';
```

权限检查建议：

1. 应用账号禁止使用 root。
2. 应用账号不应拥有 DDL 权限。
3. 不同系统、不同服务使用独立账号。
4. 账号 Host 不要配置为 `%`，除非有明确网络隔离。
5. 离职人员和下线系统账号必须及时回收。
6. 权限变更要记录审计。
7. 定期巡检高权限账号和长期不用账号。

### 安全检查

安全检查用于避免数据库暴露、敏感数据泄露、密码泄露、SQL 注入、弱口令和日志泄露。生产数据库安全应从网络、账号、权限、数据、日志和备份多个层面控制。

安全基础检查项：

| 检查项    | 要求                     |
| --------- | ------------------------ |
| 网络访问  | 不允许公网裸露数据库端口 |
| 账号密码  | 禁止弱口令               |
| root 登录 | 限制远程 root 登录       |
| 权限控制  | 最小权限原则             |
| SQL 注入  | 使用参数绑定             |
| 敏感数据  | 加密、脱敏或访问控制     |
| 日志输出  | 禁止明文打印敏感信息     |
| 备份文件  | 加密存储和权限控制       |
| 数据导出  | 必须审批和脱敏           |
| 审计日志  | 记录高风险操作           |

检查远程 root：

```sql
-- 检查 root 账号 Host
SELECT
    user,
    host
FROM mysql.user
WHERE user = 'root';
```

检查弱权限账号：

```sql
-- 查看可能存在广泛 Host 权限的账号
SELECT
    user,
    host
FROM mysql.user
WHERE host = '%'
ORDER BY user;
```

敏感字段清单：

```text
常见敏感字段：

1. password_hash
2. mobile
3. email
4. id_card_no
5. bank_card_no
6. receiver_name
7. receiver_mobile
8. receiver_address
9. access_token
10. secret_key
```

脱敏查询示例：

```sql
-- 查询用户时对手机号脱敏
SELECT
    id,
    username,
    nickname,
    CONCAT(LEFT(mobile, 3), '****', RIGHT(mobile, 4)) AS mobile_mask,
    user_status,
    created_at
FROM sys_user
WHERE deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

安全检查建议：

1. 数据库端口只允许可信内网访问。
2. 生产密码不写入代码仓库。
3. 备份文件不能放在公开目录。
4. 导出数据必须审批和脱敏。
5. 应用 SQL 必须使用参数绑定。
6. 日志中不能输出密码、令牌、身份证号、完整手机号等敏感信息。
7. 定期检查账号权限和登录来源。

### 备份检查

备份检查用于确认数据库具备可恢复能力。备份不是只看文件是否存在，还要验证备份完整性、可解压、可导入、可恢复到指定时间点。

备份基础检查项：

| 检查项      | 要求                       |
| ----------- | -------------------------- |
| 全量备份    | 按策略定期执行             |
| Binlog 备份 | 开启并备份                 |
| 备份保留    | 满足业务和合规要求         |
| 异地备份    | 不只保存在数据库服务器本机 |
| 备份加密    | 敏感数据备份应加密         |
| 校验值      | 生成并验证校验值           |
| 恢复演练    | 定期执行                   |
| 告警        | 备份失败必须告警           |
| 权限        | 备份文件访问受控           |
| 记录        | 记录备份时间、大小、结果   |

查看 Binlog 是否开启：

```sql
-- 查看 Binlog 是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看当前 Binlog 文件和位置
SHOW MASTER STATUS;
```

查看备份文件：

```bash
# 查看备份目录
ls -lh /data/backup/mysql

# 查看备份文件大小
du -sh /data/backup/mysql/*
```

生成校验值：

```bash
# 生成备份文件 SHA-256 校验值
sha256sum /data/backup/mysql/full_20260511.sql.gz \
  > /data/backup/mysql/full_20260511.sql.gz.sha256
```

校验备份文件：

```bash
# 校验备份文件完整性
sha256sum -c /data/backup/mysql/full_20260511.sql.gz.sha256
```

测试压缩包：

```bash
# 测试 gzip 文件是否完整
gzip -t /data/backup/mysql/full_20260511.sql.gz
```

备份检查建议：

1. 每次备份后检查退出码、文件大小和校验值。
2. 定期恢复到临时实例验证。
3. 备份和 Binlog 必须配套，才能做时间点恢复。
4. 备份文件应异地保存。
5. 备份失败必须告警。
6. 备份账号应最小权限。
7. 数据误删恢复流程必须定期演练。

### 性能检查

性能检查用于发现慢查询、索引缺失、连接池配置不合理、锁等待、Buffer Pool 命中率低、磁盘 IO 压力和大表风险。性能检查应覆盖上线前和上线后两个阶段。

性能基础检查项：

| 检查项      | 要求                  |
| ----------- | --------------------- |
| 慢查询      | 慢查询日志开启        |
| 核心 SQL    | 已执行 `EXPLAIN`      |
| 索引命中    | 高频 SQL 命中预期索引 |
| 大表        | 有归档或治理方案      |
| 深分页      | 已规避或限制          |
| 批量任务    | 分批执行              |
| 连接池      | 总连接数可控          |
| 锁等待      | 无明显长事务和锁等待  |
| Buffer Pool | 命中率和内存配置合理  |
| 主从延迟    | 可监控可告警          |

查看慢查询配置：

```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'slow_query_log_file';
SHOW VARIABLES LIKE 'long_query_time';
```

查看当前运行 SQL：

```sql
-- 查看当前耗时较长的 SQL
SELECT
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    info
FROM information_schema.PROCESSLIST
WHERE command <> 'Sleep'
ORDER BY time DESC
LIMIT 20;
```

查看 Buffer Pool：

```sql
-- 查看 Buffer Pool 读请求和物理读
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';
```

查看表容量：

```sql
-- 查看当前库大表
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024 / 1024, 2) AS total_gb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY total_gb DESC
LIMIT 20;
```

性能检查建议：

1. 上线前检查核心 SQL 执行计划。
2. 上线后观察慢查询、CPU、IO、连接数和锁等待。
3. 定时任务、导入导出、报表查询要单独压测。
4. 大表必须有归档、分区或拆分计划。
5. 连接池配置要按应用实例总数计算。
6. 查询性能不能只用开发环境小数据量判断。
7. 性能问题优先定位 SQL 和索引，不要盲目改参数。

### 监控检查

监控检查用于确认数据库核心指标已经接入监控系统，并配置合理告警。没有监控的数据库问题通常只能等用户反馈后才发现。

监控基础检查项：

| 监控项      | 说明                     |
| ----------- | ------------------------ |
| MySQL 存活  | 实例是否可连接           |
| 连接数      | 当前连接数、活跃线程数   |
| QPS         | 查询压力                 |
| TPS         | 事务压力                 |
| 慢查询      | 慢查询数量和慢 SQL       |
| 锁等待      | 行锁、元数据锁、长事务   |
| 主从延迟    | 复制线程和延迟秒数       |
| 磁盘空间    | 数据盘、日志盘、备份盘   |
| Buffer Pool | 命中率、脏页、空闲页     |
| 错误日志    | ERROR、崩溃、复制异常    |
| 备份任务    | 备份成功、失败、文件大小 |
| 连接池      | 应用侧连接池活跃和等待   |

推荐告警阈值示例：

| 指标       | 示例阈值                            |
| ---------- | ----------------------------------- |
| 实例不可用 | 立即告警                            |
| 磁盘使用率 | 超过 80% 预警，超过 90% 严重告警    |
| 主从延迟   | 超过 30 秒预警，超过 300 秒严重告警 |
| 连接数     | 超过最大连接数 80% 预警             |
| 慢查询数量 | 按业务基线设置                      |
| 长事务     | 超过 60 秒预警                      |
| 锁等待     | 出现持续锁等待告警                  |
| 备份失败   | 立即告警                            |

查看基础监控指标：

```sql
-- 查看连接数
SHOW GLOBAL STATUS LIKE 'Threads_connected';
SHOW GLOBAL STATUS LIKE 'Threads_running';

-- 查看查询数
SHOW GLOBAL STATUS LIKE 'Questions';

-- 查看事务提交和回滚
SHOW GLOBAL STATUS LIKE 'Com_commit';
SHOW GLOBAL STATUS LIKE 'Com_rollback';

-- 查看慢查询数量
SHOW GLOBAL STATUS LIKE 'Slow_queries';
```

监控检查建议：

1. 数据库存活监控必须有。
2. 连接数、慢查询、主从延迟、磁盘空间必须告警。
3. 备份任务也必须纳入监控。
4. 告警阈值应结合业务基线设置。
5. 告警要有负责人和处理手册。
6. 发布期间应重点观察慢查询、错误日志和连接池。
7. 监控指标应能追溯历史趋势。

### 发布检查

发布检查用于确保数据库变更、应用版本、配置、备份、回滚和监控都已准备完成。数据库发布尤其需要关注兼容性和可回滚性。

发布前检查项：

| 检查项   | 要求                           |
| -------- | ------------------------------ |
| 脚本审核 | DDL、DML 已评审                |
| 测试验证 | 测试环境执行成功               |
| 预发验证 | 预发环境执行成功               |
| 兼容性   | 数据库变更与应用版本兼容       |
| 备份     | 高风险变更前已备份             |
| 回滚脚本 | 已准备并验证                   |
| 执行窗口 | 避开业务高峰                   |
| 影响范围 | 明确涉及库表和数据量           |
| 账号权限 | 执行账号权限符合要求           |
| 监控告警 | 发布期间监控可用               |
| 负责人   | 执行人、验证人、回滚负责人明确 |

发布中检查项：

```text
1. 确认当前执行环境
2. 确认当前数据库和实例
3. 执行前再次核对脚本
4. 执行 DDL 或 DML
5. 记录执行时间和影响行数
6. 执行验证 SQL
7. 观察错误日志、慢查询、连接数、主从延迟
8. 确认应用功能正常
```

发布后验证 SQL：

```sql
-- 验证字段是否存在
SHOW COLUMNS FROM order_info LIKE 'channel_code';

-- 验证索引是否存在
SHOW INDEX FROM order_info WHERE Key_name = 'idx_user_status_created';

-- 验证核心 SQL 执行计划
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    order_status,
    created_at
FROM order_info
WHERE user_id = 10001
  AND order_status = 1
ORDER BY created_at DESC
LIMIT 20;
```

发布回滚检查项：

| 检查项       | 要求             |
| ------------ | ---------------- |
| 回滚触发条件 | 明确什么情况回滚 |
| 回滚脚本     | 已准备           |
| 数据备份     | DML 变更前已备份 |
| 应用版本     | 可回退           |
| 配置版本     | 可回退           |
| 数据库变更   | 确认是否可逆     |
| 验证方式     | 回滚后如何验证   |
| 通知机制     | 相关人员及时同步 |

发布检查建议：

1. 发布前必须确认当前连接的是生产还是测试。
2. 高风险 DML 执行前必须备份受影响数据。
3. 数据库变更应优先做到向前兼容。
4. 字段删除、索引删除应放在最后阶段。
5. 发布后必须观察监控和日志。
6. 变更记录、执行结果和验证结果要留档。
7. 每次生产问题都应复盘并更新检查清单。
