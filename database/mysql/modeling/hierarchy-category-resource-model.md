# 层级、分类与资源模型



层级、分类与资源模型用于描述业务系统中具有父子关系、归类关系、标签关系和资源挂载关系的数据结构。本章节先从树形层级模型开始，说明如何在 MySQL 8 中设计一套适合菜单、组织、区域、类目等业务场景的通用树结构。

## 树形层级模型

树形层级模型用于表达一条数据与另一条数据之间的上下级关系。典型结构是每个节点保存自己的父节点，同时冗余保存层级路径、层级深度、排序号等字段，以便兼顾写入简单性和查询性能。

### 适用场景

树形层级模型适合层级关系清晰、节点数量可控、业务上需要频繁按父节点或子树查询的数据。

常见业务场景包括：

- 系统菜单：一级菜单、二级菜单、按钮权限等。
- 组织架构：集团、公司、部门、小组等。
- 商品类目：一级类目、二级类目、叶子类目等。
- 行政区域：国家、省、市、区县、街道等。
- 知识目录：文档空间、目录、子目录等。
- 文件目录：文件夹、子文件夹、文件资源等。

该模型适合读多写少或读写均衡的业务。如果业务中存在大量跨层级移动、超深层级、复杂祖先后代统计，单纯的父子节点模型可能不够，需要结合闭包表模型或路径枚举模型增强查询能力。

### 建模结构

树形层级模型建议采用“父节点 ID + 路径字段 + 层级字段”的混合设计。`parent_id` 用于表达直接父子关系，`tree_path` 用于快速查询整棵子树，`tree_level` 用于控制层级深度和排序展示。

下面的表结构只描述建模字段，不在这里放置二级索引。索引统一在“索引设计”章节给出。

```sql
CREATE TABLE biz_tree_node (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父节点ID，根节点固定为0',
    node_code VARCHAR(64) NOT NULL COMMENT '节点编码，同一父节点下建议唯一',
    node_name VARCHAR(128) NOT NULL COMMENT '节点名称',
    node_type VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '节点类型，如menu、dept、category',
    tree_path VARCHAR(1024) NOT NULL COMMENT '节点路径，如/100/101/102/',
    tree_level INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '节点层级，根节点为1',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '同级排序号，值越小越靠前',
    leaf_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否叶子节点：0否，1是',
    enabled_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='业务树形节点表';
```

核心建模规则如下：

| 字段         | 设计规则                                              |
| ------------ | ----------------------------------------------------- |
| `id`         | 建议由业务侧提前生成，便于插入时直接拼接 `tree_path`  |
| `parent_id`  | 根节点固定为 `0`，非根节点保存直接父节点 ID           |
| `tree_path`  | 保存从根节点到当前节点的完整 ID 路径，例如 `/1/8/20/` |
| `tree_level` | 根节点为 `1`，子节点等于父节点层级加 `1`              |
| `sort_no`    | 只表示同一父节点下的展示顺序                          |
| `leaf_flag`  | 用于快速判断是否叶子节点，写入子节点时需要同步维护    |
| `deleted`    | 建议使用逻辑删除，避免物理删除导致历史引用断裂        |
| `tenant_id`  | 多租户系统必须参与主要查询条件和唯一约束              |

`tree_path` 推荐使用“前后都有斜杠”的格式，例如 `/100/200/300/`。这样可以避免 ID 前缀误匹配问题，例如查询 `/1/` 时不会误匹配 `/10/`。

### 字段设计

字段设计应围绕“树结构表达、业务标识、展示控制、状态控制、审计追踪”几个方面展开。树结构字段负责层级关系，业务字段负责节点本身的业务含义，通用字段负责状态、版本和审计。

| 字段分类 | 字段                                                   | 说明                                                       |
| -------- | ------------------------------------------------------ | ---------------------------------------------------------- |
| 主键字段 | `id`                                                   | 节点唯一标识，建议使用雪花 ID、号段 ID 或其他业务侧生成 ID |
| 租户字段 | `tenant_id`                                            | 多租户隔离字段，所有核心查询都应携带                       |
| 层级字段 | `parent_id`                                            | 直接父节点 ID                                              |
| 层级字段 | `tree_path`                                            | 完整层级路径，用于快速查询子树                             |
| 层级字段 | `tree_level`                                           | 节点层级，用于限制最大层级和展示缩进                       |
| 业务字段 | `node_code`                                            | 节点编码，适合菜单编码、部门编码、类目编码                 |
| 业务字段 | `node_name`                                            | 节点名称，用于页面展示和模糊查询                           |
| 业务字段 | `node_type`                                            | 节点类型，用于同一张表承载多类树节点                       |
| 展示字段 | `sort_no`                                              | 同级排序                                                   |
| 状态字段 | `leaf_flag`                                            | 是否叶子节点                                               |
| 状态字段 | `enabled_flag`                                         | 是否启用                                                   |
| 状态字段 | `deleted`                                              | 逻辑删除标记                                               |
| 并发字段 | `version`                                              | 乐观锁版本号                                               |
| 审计字段 | `create_by`、`create_time`、`update_by`、`update_time` | 记录数据创建和修改信息                                     |

字段设计建议：

- `parent_id` 不建议允许 `NULL`，根节点统一使用 `0`，查询条件更简单。
- `tree_path` 不建议只保存父路径，建议保存包含自身 ID 的完整路径。
- `tree_level` 建议从 `1` 开始，不建议从 `0` 开始，便于业务人员理解。
- `node_code` 适合作为业务稳定标识，但不建议替代主键。
- `sort_no` 只解决同级排序，不要用于表达层级。
- `leaf_flag` 是冗余字段，需要在新增、删除、移动节点时维护。
- `enabled_flag` 与 `deleted` 不要混用，禁用表示业务不可用，删除表示数据不再展示。

### 索引设计

索引设计需要围绕树形数据最常见的查询路径展开，包括按父节点查询子节点、按路径查询子树、按编码定位节点、按名称搜索节点等。

```sql
ALTER TABLE biz_tree_node
    ADD PRIMARY KEY (id);
```

主键索引用于按节点 ID 精确定位数据，也是父子关系、路径查询和写入维护的基础。

```sql
CREATE UNIQUE INDEX uk_tree_tenant_parent_code_deleted
    ON biz_tree_node (tenant_id, parent_id, node_code, deleted);
```

该唯一索引用于保证同一租户、同一父节点下，未删除节点的编码不重复。由于包含 `deleted`，逻辑删除后可以重新创建相同编码的节点。

```sql
CREATE INDEX idx_tree_tenant_parent_sort
    ON biz_tree_node (tenant_id, parent_id, deleted, enabled_flag, sort_no, id);
```

该索引用于查询某个父节点下的直接子节点，并按 `sort_no` 和 `id` 稳定排序。

```sql
CREATE INDEX idx_tree_tenant_path_deleted
    ON biz_tree_node (tenant_id, tree_path(255), deleted);
```

该索引用于按 `tree_path LIKE '/xxx/%'` 查询子树。`tree_path` 较长时可以使用前缀索引，但需要控制路径长度和树深度。

```sql
CREATE INDEX idx_tree_tenant_level_sort
    ON biz_tree_node (tenant_id, tree_level, deleted, sort_no, id);
```

该索引用于按层级查询节点，例如查询所有一级节点、二级节点，或者进行层级统计。

```sql
CREATE INDEX idx_tree_tenant_name
    ON biz_tree_node (tenant_id, node_name, deleted);
```

该索引用于节点名称精确匹配或前缀匹配。如果需要高频模糊搜索，例如 `%关键字%`，应考虑搜索辅助表、全文索引或外部搜索引擎。

索引设计注意事项：

- 查询条件中应优先带上 `tenant_id` 和 `deleted`。
- 子节点查询优先走 `(tenant_id, parent_id, deleted, enabled_flag, sort_no, id)`。
- 子树查询优先走 `tree_path LIKE '固定前缀%'`，不要写成 `LIKE '%/100/%'`。
- `tree_path` 过长时，前缀索引可能无法覆盖全部路径，需要结合最大层级进行约束。
- 如果节点移动非常频繁，`tree_path` 会带来批量更新成本，需要评估是否改用递归 CTE 或闭包表。

### 常用查询

常用查询应覆盖树形结构的核心读场景，包括根节点查询、子节点查询、子树查询、祖先链查询、面包屑查询和叶子节点查询。

#### 查询根节点

根节点用于树形组件首次加载，通常查询 `parent_id = 0` 的节点。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    node_type,
    tree_path,
    tree_level,
    sort_no,
    leaf_flag,
    enabled_flag
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND parent_id = 0
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY sort_no ASC, id ASC;
```

该查询适合懒加载树的第一层数据。业务上如果只允许一个根节点，可以在写入层控制根节点数量。

#### 查询直接子节点

直接子节点查询是树形组件懒加载中最常用的查询方式。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    node_type,
    tree_path,
    tree_level,
    sort_no,
    leaf_flag,
    enabled_flag
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND parent_id = :parentId
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY sort_no ASC, id ASC;
```

该查询只返回下一层节点，不返回孙级节点。它适合前端按需展开，避免一次性加载整棵树导致响应过大。

#### 查询整棵子树

如果维护了 `tree_path`，查询某个节点及其所有后代节点可以直接使用路径前缀匹配。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    node_type,
    tree_path,
    tree_level,
    sort_no,
    leaf_flag,
    enabled_flag
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND tree_path LIKE CONCAT(:nodePath, '%')
  AND deleted = 0
ORDER BY tree_level ASC, sort_no ASC, id ASC;
```

`:nodePath` 应传入当前节点完整路径，例如 `/100/200/`。该查询会包含当前节点自身。如果只查询后代节点，需要增加 `AND id <> :nodeId`。

#### 查询后代节点但不包含自身

有些业务只需要当前节点下面的子孙节点，不需要当前节点本身，例如统计下级部门。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    tree_path,
    tree_level,
    sort_no
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND tree_path LIKE CONCAT(:nodePath, '%')
  AND id <> :nodeId
  AND deleted = 0
ORDER BY tree_level ASC, sort_no ASC, id ASC;
```

该查询适合统计、权限范围展开、批量授权等场景。

#### 使用递归 CTE 查询子树

如果没有维护 `tree_path`，或者需要临时基于 `parent_id` 递归查询，可以使用 MySQL 8 的递归 CTE。

```sql
WITH RECURSIVE tree_cte AS (
    SELECT
        id,
        parent_id,
        node_code,
        node_name,
        tree_level,
        sort_no,
        CAST(node_name AS CHAR(2000)) AS full_name
    FROM biz_tree_node
    WHERE tenant_id = :tenantId
      AND id = :nodeId
      AND deleted = 0

    UNION ALL

    SELECT
        child.id,
        child.parent_id,
        child.node_code,
        child.node_name,
        child.tree_level,
        child.sort_no,
        CONCAT(tree_cte.full_name, '/', child.node_name) AS full_name
    FROM biz_tree_node child
    INNER JOIN tree_cte
        ON child.parent_id = tree_cte.id
    WHERE child.tenant_id = :tenantId
      AND child.deleted = 0
)
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    tree_level,
    sort_no,
    full_name
FROM tree_cte
ORDER BY tree_level ASC, sort_no ASC, id ASC;
```

递归 CTE 适合临时查询和低频查询。高频子树查询仍建议维护 `tree_path`，减少递归计算成本。

#### 查询祖先链

祖先链用于查询当前节点的所有上级节点，例如部门路径、类目路径、菜单路径。

```sql
SELECT
    parent.id,
    parent.parent_id,
    parent.node_code,
    parent.node_name,
    parent.tree_path,
    parent.tree_level
FROM biz_tree_node current_node
INNER JOIN biz_tree_node parent
    ON FIND_IN_SET(
        CAST(parent.id AS CHAR),
        REPLACE(TRIM(BOTH '/' FROM current_node.tree_path), '/', ',')
    ) > 0
WHERE current_node.tenant_id = :tenantId
  AND current_node.id = :nodeId
  AND current_node.deleted = 0
  AND parent.tenant_id = :tenantId
  AND parent.deleted = 0
ORDER BY parent.tree_level ASC;
```

该查询依赖 `tree_path` 中保存完整 ID 路径，返回结果包含当前节点自身。如果只需要上级节点，需要增加 `AND parent.id <> current_node.id`。

#### 查询面包屑路径

面包屑路径通常用于页面展示，例如“系统管理 / 用户管理 / 用户列表”。

```sql
SELECT
    GROUP_CONCAT(parent.node_name ORDER BY parent.tree_level ASC SEPARATOR ' / ') AS breadcrumb_name
FROM biz_tree_node current_node
INNER JOIN biz_tree_node parent
    ON FIND_IN_SET(
        CAST(parent.id AS CHAR),
        REPLACE(TRIM(BOTH '/' FROM current_node.tree_path), '/', ',')
    ) > 0
WHERE current_node.tenant_id = :tenantId
  AND current_node.id = :nodeId
  AND current_node.deleted = 0
  AND parent.tenant_id = :tenantId
  AND parent.deleted = 0;
```

该查询适合详情页展示。如果面包屑访问非常频繁，可以冗余保存 `path_name` 字段，但需要在节点改名和移动时同步维护。

#### 查询叶子节点

叶子节点用于查询最末级类目、最末级部门、末级菜单等数据。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    tree_path,
    tree_level,
    sort_no
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND leaf_flag = 1
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY tree_level ASC, sort_no ASC, id ASC;
```

`leaf_flag` 是冗余字段，必须在新增、移动、删除节点时维护准确，否则该查询结果会失真。

#### 查询同级节点

同级节点常用于校验同级名称、同级编码、同级排序展示。

```sql
SELECT
    id,
    parent_id,
    node_code,
    node_name,
    sort_no,
    enabled_flag
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND parent_id = :parentId
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

如果用于修改节点时校验同级编码重复，需要排除当前节点。

```sql
SELECT
    COUNT(1) AS duplicate_count
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND parent_id = :parentId
  AND node_code = :nodeCode
  AND id <> :currentNodeId
  AND deleted = 0;
```

### 常用写入

树形层级模型的写入操作必须保证 `parent_id`、`tree_path`、`tree_level`、`leaf_flag` 的一致性。涉及新增、移动、删除子树的操作建议放在事务中执行。

#### 创建根节点

创建根节点时，`parent_id` 固定为 `0`，`tree_path` 使用自身 ID 组成。

```sql
INSERT INTO biz_tree_node (
    id,
    tenant_id,
    parent_id,
    node_code,
    node_name,
    node_type,
    tree_path,
    tree_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    create_by,
    update_by
) VALUES (
    :id,
    :tenantId,
    0,
    :nodeCode,
    :nodeName,
    :nodeType,
    CONCAT('/', :id, '/'),
    1,
    :sortNo,
    1,
    1,
    :operator,
    :operator
);
```

该写法要求业务侧提前生成 `id`。如果使用 MySQL 自增 ID，则需要先插入节点，再根据生成的 ID 回写 `tree_path`。

#### 创建子节点

创建子节点时，需要读取父节点的路径和层级，再生成子节点的 `tree_path` 与 `tree_level`。

```sql
START TRANSACTION;

SELECT
    id,
    tree_path,
    tree_level
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND id = :parentId
  AND deleted = 0
FOR UPDATE;

INSERT INTO biz_tree_node (
    id,
    tenant_id,
    parent_id,
    node_code,
    node_name,
    node_type,
    tree_path,
    tree_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    create_by,
    update_by
)
SELECT
    :id,
    :tenantId,
    parent.id,
    :nodeCode,
    :nodeName,
    :nodeType,
    CONCAT(parent.tree_path, :id, '/'),
    parent.tree_level + 1,
    :sortNo,
    1,
    1,
    :operator,
    :operator
FROM biz_tree_node parent
WHERE parent.tenant_id = :tenantId
  AND parent.id = :parentId
  AND parent.deleted = 0;

UPDATE biz_tree_node
SET leaf_flag = 0,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :parentId
  AND deleted = 0;

COMMIT;
```

该写入流程先锁定父节点，避免并发写入时父节点状态维护不一致。

#### 修改节点基础信息

修改节点名称、编码、类型、排序等基础信息时，不需要变更子树路径。

```sql
UPDATE biz_tree_node
SET node_code = :nodeCode,
    node_name = :nodeName,
    node_type = :nodeType,
    sort_no = :sortNo,
    enabled_flag = :enabledFlag,
    remark = :remark,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :id
  AND deleted = 0
  AND version = :version;
```

该写法使用 `version` 做乐观锁，适合后台管理页面编辑节点信息。

#### 移动节点

移动节点是树形模型中风险最高的写入操作。移动前必须校验不能把节点移动到自己的子孙节点下面，否则会形成循环层级。

移动前先校验目标父节点不是当前节点的后代。

```sql
SELECT
    COUNT(1) AS invalid_count
FROM biz_tree_node target_parent
WHERE target_parent.tenant_id = :tenantId
  AND target_parent.id = :newParentId
  AND target_parent.deleted = 0
  AND target_parent.tree_path LIKE CONCAT(:oldNodePath, '%');
```

如果 `invalid_count > 0`，表示目标父节点位于当前节点的子树内，必须拒绝移动。

移动时建议由业务代码先计算：

- `:oldNodePath`：移动前当前节点路径，例如 `/100/200/`
- `:newNodePath`：移动后当前节点路径，例如 `/300/200/`
- `:levelDelta`：新层级与旧层级的差值

然后批量更新当前节点及其所有后代节点。

```sql
START TRANSACTION;

UPDATE biz_tree_node
SET parent_id = CASE
        WHEN id = :nodeId THEN :newParentId
        ELSE parent_id
    END,
    tree_path = CONCAT(:newNodePath, SUBSTRING(tree_path, CHAR_LENGTH(:oldNodePath) + 1)),
    tree_level = tree_level + :levelDelta,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND tree_path LIKE CONCAT(:oldNodePath, '%')
  AND deleted = 0;

UPDATE biz_tree_node
SET leaf_flag = CASE
        WHEN EXISTS (
            SELECT 1
            FROM (
                SELECT id
                FROM biz_tree_node
                WHERE tenant_id = :tenantId
                  AND parent_id = :oldParentId
                  AND deleted = 0
                LIMIT 1
            ) AS child_exists
        ) THEN 0
        ELSE 1
    END,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :oldParentId
  AND deleted = 0;

UPDATE biz_tree_node
SET leaf_flag = 0,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :newParentId
  AND deleted = 0;

COMMIT;
```

移动节点会批量更新子树路径。子树越大，更新成本越高。因此对于频繁拖拽排序和移动的大树，需要重点评估性能。

#### 逻辑删除节点及子树

删除节点通常有两种策略：只允许删除叶子节点，或者允许删除整棵子树。后台管理系统中建议优先使用“只允许删除叶子节点”，数据风险更低。

删除叶子节点前先检查是否存在未删除子节点。

```sql
SELECT
    COUNT(1) AS child_count
FROM biz_tree_node
WHERE tenant_id = :tenantId
  AND parent_id = :nodeId
  AND deleted = 0;
```

如果允许删除整棵子树，可以按路径逻辑删除当前节点及其所有后代节点。

```sql
START TRANSACTION;

UPDATE biz_tree_node
SET deleted = 1,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND tree_path LIKE CONCAT(:nodePath, '%')
  AND deleted = 0;

UPDATE biz_tree_node
SET leaf_flag = CASE
        WHEN EXISTS (
            SELECT 1
            FROM (
                SELECT id
                FROM biz_tree_node
                WHERE tenant_id = :tenantId
                  AND parent_id = :parentId
                  AND deleted = 0
                LIMIT 1
            ) AS child_exists
        ) THEN 0
        ELSE 1
    END,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :parentId
  AND deleted = 0;

COMMIT;
```

逻辑删除子树后，历史数据仍然可以保留。如果业务存在订单、权限、资源引用树节点，不建议直接物理删除。

### 常见问题

树形层级模型虽然结构直观，但在路径维护、节点移动、层级深度和并发写入方面容易出现问题。

| 问题                       | 原因                                   | 建议                                                         |
| -------------------------- | -------------------------------------- | ------------------------------------------------------------ |
| 子树查询慢                 | 没有维护 `tree_path`，只能递归查       | 读多场景维护 `tree_path`，并建立路径索引                     |
| 移动节点后层级错乱         | 只更新了当前节点，没有更新后代节点     | 移动节点时批量更新整棵子树的 `tree_path` 和 `tree_level`     |
| 节点形成循环               | 允许移动到自己的后代节点下             | 移动前校验目标父节点的路径不能以当前节点路径开头             |
| 叶子状态不准确             | 新增、删除、移动时没有维护 `leaf_flag` | 所有写入操作统一封装在服务层事务中                           |
| 根节点规则混乱             | 有的使用 `NULL`，有的使用 `0`          | 统一规定根节点 `parent_id = 0`                               |
| 路径字段过长               | 树层级过深或 ID 太长                   | 限制最大层级，必要时改用闭包表                               |
| 同级节点重复               | 没有唯一约束或只在代码中校验           | 使用唯一索引约束 `tenant_id + parent_id + node_code + deleted` |
| 模糊搜索性能差             | 对 `node_name LIKE '%xxx%'` 依赖过重   | 使用搜索辅助表、全文索引或外部搜索服务                       |
| 并发新增导致父节点状态异常 | 多事务同时写入同一父节点               | 新增子节点时锁定父节点并在事务中维护状态                     |
| 逻辑删除后编码不能复用     | 唯一索引没有包含 `deleted`             | 唯一索引中加入 `deleted` 或使用删除版本号字段                |

设计时应提前明确几个业务边界：

- 最大树深度是多少。
- 是否允许拖拽移动节点。
- 是否允许删除非叶子节点。
- 是否需要跨层级统计。
- 是否需要按权限过滤树节点。
- 是否需要多租户隔离。
- 是否存在节点编码复用规则。

这些规则应在数据库约束、服务层校验和前端交互中保持一致。

### 总结

树形层级模型的核心是用 `parent_id` 表达直接父子关系，用 `tree_path` 加速子树查询，用 `tree_level` 辅助层级控制和展示，用 `sort_no` 处理同级排序。

在 MySQL 8 中，通用业务系统建议优先采用“父节点 ID + 路径字段”的混合模型。该模型实现简单，查询直观，适合菜单、组织、类目、区域等大多数业务场景。

设计该模型时需要重点关注四点：

- 建模结构中明确根节点、路径、层级和排序规则。
- 字段设计中区分业务标识、层级字段、状态字段和审计字段。
- 索引设计中分别覆盖父节点查询、子树查询、层级查询和编码唯一性。
- 写入操作中使用事务维护 `tree_path`、`tree_level`、`leaf_flag` 的一致性。

如果业务树节点规模很大、层级很深、移动频繁，或者祖先后代查询非常复杂，应考虑在该模型基础上引入闭包表、搜索辅助表或专门的读模型。



## 分类模型

分类模型用于描述业务对象与分类目录之间的归属关系。分类通常具有相对稳定的业务含义，例如商品分类、文章分类、知识库分类、客户分类、资源分类等。分类模型可以是一级分类，也可以结合树形层级模型形成多级分类。

分类模型与树形层级模型的区别在于：树形层级模型强调节点之间的父子结构，分类模型强调业务对象如何被归入某个分类，以及分类本身如何被管理、启用、排序、展示和引用。

### 适用场景

分类模型适合用于业务对象需要按照固定维度进行归类、筛选、统计和展示的场景。

常见业务场景包括：

- 商品分类：手机、电脑、家电、服饰等。
- 文章分类：公告、新闻、教程、帮助文档等。
- 知识库分类：产品文档、开发文档、运维文档等。
- 客户分类：普通客户、重点客户、渠道客户等。
- 资源分类：图片、视频、文档、压缩包等。
- 工单分类：咨询、故障、投诉、需求等。
- 财务分类：收入分类、支出分类、费用分类等。

分类模型适合分类数量相对可控、分类结构稳定、查询条件明确的业务。如果业务对象可以同时属于多个分类，需要使用“对象分类关系表”。如果业务对象只允许属于一个分类，可以直接在业务主表中保存 `category_id`。

### 建模结构

分类模型通常分为两种设计方式：单分类归属和多分类归属。

单分类归属适合一个业务对象只能属于一个分类的场景。例如一篇文章只能属于一个栏目，一个商品只能属于一个主分类。这种方式可以直接在业务表中保存 `category_id`。

多分类归属适合一个业务对象可以属于多个分类的场景。例如一篇知识文档可以同时归入“Java”“Spring Boot”“数据库”，一个资源文件可以同时归入多个资源目录。这种方式需要增加对象分类关系表。

分类主表用于保存分类本身的信息。

```sql
CREATE TABLE biz_category (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码，同一分类类型下建议唯一',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    category_type VARCHAR(32) NOT NULL COMMENT '分类类型，如product、article、resource',
    parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID，一级分类固定为0',
    category_path VARCHAR(1024) NOT NULL COMMENT '分类路径，如/100/101/102/',
    category_level INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '分类层级，一级分类为1',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '同级排序号，值越小越靠前',
    icon_url VARCHAR(500) DEFAULT NULL COMMENT '分类图标地址',
    description VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
    leaf_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否叶子分类：0否，1是',
    enabled_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    system_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统内置：0否，1是',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='业务分类表';
```

如果业务对象只能属于一个分类，可以在业务主表中直接保存分类 ID。

```sql
CREATE TABLE biz_article (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    category_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '分类ID',
    article_title VARCHAR(200) NOT NULL COMMENT '文章标题',
    article_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '文章状态：draft草稿，published已发布',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='文章表';
```

如果业务对象可以属于多个分类，需要使用对象分类关系表。

```sql
CREATE TABLE biz_object_category_rel (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    object_type VARCHAR(32) NOT NULL COMMENT '对象类型，如article、product、resource',
    object_id BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    main_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否主分类：0否，1是',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '对象在分类下的排序号',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='对象分类关系表';
```

建模时应优先明确业务对象和分类之间的关系：

| 关系类型 | 设计方式                                  | 适用场景                               |
| -------- | ----------------------------------------- | -------------------------------------- |
| 单分类   | 业务主表保存 `category_id`                | 商品主分类、文章栏目、工单类型         |
| 多分类   | 使用对象分类关系表                        | 知识文档多分类、资源多目录、内容多频道 |
| 多级分类 | 分类表维护 `parent_id` 和 `category_path` | 商品类目、知识目录、资源目录           |
| 平铺分类 | 分类表只使用 `category_type` 区分         | 客户类型、费用类型、文章类型           |

分类模型不建议把分类名称直接冗余到业务主表作为唯一依据。名称可以修改，ID 才是稳定关联字段。如果需要提升列表展示性能，可以在读模型或宽表中冗余分类名称，但主关系仍应以 `category_id` 为准。

### 字段设计

分类字段设计应围绕分类标识、分类层级、分类类型、展示控制、状态控制和审计追踪展开。

| 字段分类 | 字段                                                   | 说明                                   |
| -------- | ------------------------------------------------------ | -------------------------------------- |
| 主键字段 | `id`                                                   | 分类唯一标识，建议使用业务侧生成 ID    |
| 租户字段 | `tenant_id`                                            | 多租户隔离字段                         |
| 编码字段 | `category_code`                                        | 分类编码，适合用于接口、规则、配置引用 |
| 名称字段 | `category_name`                                        | 分类名称，用于页面展示                 |
| 类型字段 | `category_type`                                        | 分类类型，用于区分不同业务分类体系     |
| 层级字段 | `parent_id`                                            | 父分类 ID，一级分类固定为 `0`          |
| 层级字段 | `category_path`                                        | 分类路径，用于查询分类子树             |
| 层级字段 | `category_level`                                       | 分类层级，用于控制展示层级和最大深度   |
| 展示字段 | `sort_no`                                              | 同级排序号                             |
| 展示字段 | `icon_url`                                             | 分类图标地址                           |
| 说明字段 | `description`                                          | 分类说明                               |
| 状态字段 | `leaf_flag`                                            | 是否叶子分类                           |
| 状态字段 | `enabled_flag`                                         | 是否启用                               |
| 状态字段 | `system_flag`                                          | 是否系统内置                           |
| 状态字段 | `deleted`                                              | 逻辑删除标记                           |
| 并发字段 | `version`                                              | 乐观锁版本号                           |
| 审计字段 | `create_by`、`create_time`、`update_by`、`update_time` | 创建和更新信息                         |

字段设计建议：

- `category_type` 应作为分类体系的边界，不同业务分类不要混在同一套编码规则中。
- `category_code` 适合作为稳定业务编码，不建议频繁修改。
- `category_name` 可以允许重复，但同一父分类下是否允许重复需要按业务规则确定。
- `parent_id` 固定使用 `0` 表示一级分类，不建议一部分使用 `NULL`，一部分使用 `0`。
- `category_path` 建议保存包含自身 ID 的完整路径，例如 `/10/20/30/`。
- `category_level` 建议从 `1` 开始，便于业务展示和限制最大层级。
- `system_flag = 1` 的内置分类通常不允许删除，只允许修改名称、排序或启用状态。
- `enabled_flag = 0` 表示分类不可选，但历史数据仍可继续关联该分类。
- `deleted = 1` 表示分类不再展示，通常也不允许新增业务对象继续关联。

对象分类关系表字段设计建议：

- `object_type` 和 `object_id` 一起标识被分类的业务对象。
- `category_id` 保存目标分类 ID。
- `main_flag` 用于多分类场景下标记主分类。
- `sort_no` 用于分类详情页中控制对象展示顺序。
- 关系表也建议使用逻辑删除，便于保留历史关联记录。

### 索引设计

索引设计需要覆盖分类管理、分类树查询、分类下对象查询、对象所属分类查询等常用访问路径。

```sql
ALTER TABLE biz_category
    ADD PRIMARY KEY (id);
```

主键索引用于按分类 ID 精确定位分类信息。

```sql
CREATE UNIQUE INDEX uk_category_tenant_type_code_deleted
    ON biz_category (tenant_id, category_type, category_code, deleted);
```

该唯一索引用于保证同一租户、同一分类类型下，未删除分类的编码不重复。

```sql
CREATE INDEX idx_category_tenant_type_parent_sort
    ON biz_category (tenant_id, category_type, parent_id, deleted, enabled_flag, sort_no, id);
```

该索引用于查询某个分类类型下的子分类列表，并按排序号稳定展示。

```sql
CREATE INDEX idx_category_tenant_type_path
    ON biz_category (tenant_id, category_type, category_path(255), deleted);
```

该索引用于按分类路径查询某个分类及其所有子分类。

```sql
CREATE INDEX idx_category_tenant_type_level
    ON biz_category (tenant_id, category_type, category_level, deleted, sort_no, id);
```

该索引用于按分类层级查询分类，例如查询所有一级分类或二级分类。

```sql
CREATE INDEX idx_category_tenant_type_name
    ON biz_category (tenant_id, category_type, category_name, deleted);
```

该索引用于分类名称精确匹配或前缀匹配。如果需要高频包含式模糊搜索，应考虑全文索引或搜索辅助表。

如果业务主表采用单分类设计，需要给业务表的 `category_id` 建立索引。

```sql
ALTER TABLE biz_article
    ADD PRIMARY KEY (id);
CREATE INDEX idx_article_tenant_category_status
    ON biz_article (tenant_id, category_id, article_status, deleted, publish_time);
```

该索引用于查询某个分类下的文章列表，并结合状态和发布时间排序或筛选。

如果采用多分类关系表，需要对关系表建立双向查询索引。

```sql
ALTER TABLE biz_object_category_rel
    ADD PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_rel_tenant_object_category_deleted
    ON biz_object_category_rel (tenant_id, object_type, object_id, category_id, deleted);
```

该唯一索引用于避免同一个对象重复绑定同一个分类。

```sql
CREATE INDEX idx_rel_tenant_category_object
    ON biz_object_category_rel (tenant_id, category_id, object_type, deleted, sort_no, object_id);
```

该索引用于查询某个分类下绑定了哪些业务对象。

```sql
CREATE INDEX idx_rel_tenant_object
    ON biz_object_category_rel (tenant_id, object_type, object_id, deleted, main_flag);
```

该索引用于查询某个业务对象属于哪些分类。

索引设计注意事项：

- 多租户系统中，分类表和关系表的主要索引都应以 `tenant_id` 开头。
- 分类树查询应带上 `category_type`，避免不同分类体系之间互相干扰。
- 子分类查询优先走 `(tenant_id, category_type, parent_id, deleted, enabled_flag, sort_no, id)`。
- 子树查询优先使用 `category_path LIKE '固定路径%'`，不要使用 `LIKE '%/分类ID/%'`。
- 关系表必须同时支持“按分类查对象”和“按对象查分类”两个方向。
- 如果业务对象只允许一个主分类，主表 `category_id` 的查询性能通常优于关系表。
- 如果分类下对象数量很大，应结合分页条件、发布时间、状态字段一起设计复合索引。

### 常用查询

常用查询应覆盖分类管理、分类选择器、业务对象列表、分类树展示和多分类绑定查询等场景。

#### 查询一级分类

一级分类通常用于分类导航、下拉选择器和分类管理页面的第一层数据。

```sql
SELECT
    id,
    category_code,
    category_name,
    category_type,
    parent_id,
    category_path,
    category_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    icon_url
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND parent_id = 0
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY sort_no ASC, id ASC;
```

该查询适合分类懒加载的第一层数据。如果分类是平铺结构，仍然可以固定 `parent_id = 0`。

#### 查询子分类

子分类查询用于分类树懒加载，前端展开某个分类时按需加载下一层。

```sql
SELECT
    id,
    category_code,
    category_name,
    category_type,
    parent_id,
    category_path,
    category_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    icon_url
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND parent_id = :parentId
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY sort_no ASC, id ASC;
```

该查询只返回直接子分类，不返回更深层级分类，适合分类数量较多的后台管理页面。

#### 查询分类子树

分类子树查询用于一次性加载某个分类下的全部子分类，例如商品类目选择、知识库目录展示、分类迁移校验等。

```sql
SELECT
    id,
    category_code,
    category_name,
    category_type,
    parent_id,
    category_path,
    category_level,
    sort_no,
    leaf_flag,
    enabled_flag
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND category_path LIKE CONCAT(:categoryPath, '%')
  AND deleted = 0
ORDER BY category_level ASC, sort_no ASC, id ASC;
```

`:categoryPath` 应传入当前分类完整路径，例如 `/100/200/`。该查询会包含当前分类自身。如果只需要子孙分类，需要增加 `AND id <> :categoryId`。

#### 查询可选分类列表

业务对象新增或编辑页面通常只允许选择启用状态的分类，部分业务还只允许选择叶子分类。

```sql
SELECT
    id,
    category_code,
    category_name,
    parent_id,
    category_path,
    category_level,
    leaf_flag
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND deleted = 0
  AND enabled_flag = 1
  AND leaf_flag = 1
ORDER BY category_level ASC, sort_no ASC, id ASC;
```

该查询适合只允许选择末级分类的业务，例如商品必须挂到末级类目。

#### 按分类查询业务对象

单分类设计下，可以直接通过业务主表的 `category_id` 查询对象列表。

```sql
SELECT
    id,
    category_id,
    article_title,
    article_status,
    publish_time,
    create_time
FROM biz_article
WHERE tenant_id = :tenantId
  AND category_id = :categoryId
  AND article_status = 'published'
  AND deleted = 0
ORDER BY publish_time DESC, id DESC
LIMIT :offset, :pageSize;
```

该查询适合文章栏目、商品主分类等单分类归属场景。

#### 按分类子树查询业务对象

如果需要查询某个分类及其所有子分类下的业务对象，可以先查分类子树，再按分类 ID 查询对象。

```sql
SELECT
    article.id,
    article.category_id,
    article.article_title,
    article.article_status,
    article.publish_time
FROM biz_article article
INNER JOIN biz_category category
    ON category.id = article.category_id
   AND category.tenant_id = article.tenant_id
WHERE article.tenant_id = :tenantId
  AND category.category_type = :categoryType
  AND category.category_path LIKE CONCAT(:categoryPath, '%')
  AND category.deleted = 0
  AND article.article_status = 'published'
  AND article.deleted = 0
ORDER BY article.publish_time DESC, article.id DESC
LIMIT :offset, :pageSize;
```

该查询适合分类导航页展示当前分类及下级分类的所有内容。数据量较大时，可以先查出分类 ID 列表，再使用业务表索引分页查询。

#### 查询对象所属分类

多分类关系表设计下，可以查询某个业务对象绑定的全部分类。

```sql
SELECT
    category.id,
    category.category_code,
    category.category_name,
    category.category_type,
    category.category_path,
    rel.main_flag,
    rel.sort_no
FROM biz_object_category_rel rel
INNER JOIN biz_category category
    ON category.id = rel.category_id
   AND category.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.object_id = :objectId
  AND rel.deleted = 0
  AND category.deleted = 0
ORDER BY rel.main_flag DESC, rel.sort_no ASC, category.id ASC;
```

该查询适合详情页回显对象分类，也适合编辑页面加载已选分类。

#### 查询分类下的对象关系

多分类关系表设计下，可以查询某个分类绑定了哪些对象 ID，再回业务表查询对象详情。

```sql
SELECT
    object_id,
    main_flag,
    sort_no,
    create_time
FROM biz_object_category_rel
WHERE tenant_id = :tenantId
  AND category_id = :categoryId
  AND object_type = :objectType
  AND deleted = 0
ORDER BY sort_no ASC, object_id DESC
LIMIT :offset, :pageSize;
```

该查询适合分类详情页、资源列表页、内容聚合页。

#### 查询分类面包屑

分类面包屑用于页面展示，例如“文档中心 / Java / Spring Boot”。

```sql
SELECT
    GROUP_CONCAT(parent.category_name ORDER BY parent.category_level ASC SEPARATOR ' / ') AS breadcrumb_name
FROM biz_category current_category
INNER JOIN biz_category parent
    ON FIND_IN_SET(
        CAST(parent.id AS CHAR),
        REPLACE(TRIM(BOTH '/' FROM current_category.category_path), '/', ',')
    ) > 0
WHERE current_category.tenant_id = :tenantId
  AND current_category.id = :categoryId
  AND current_category.deleted = 0
  AND parent.tenant_id = :tenantId
  AND parent.deleted = 0;
```

如果分类面包屑在列表页中高频展示，可以在读模型中冗余分类路径名称，避免每行都执行祖先查询。

### 常用写入

分类模型的写入操作主要包括创建分类、修改分类、移动分类、删除分类、绑定对象分类和解绑对象分类。涉及分类层级变化时，需要放在事务中维护路径、层级和叶子状态。

#### 创建一级分类

创建一级分类时，`parent_id` 固定为 `0`，分类路径由自身 ID 组成。

```sql
INSERT INTO biz_category (
    id,
    tenant_id,
    category_code,
    category_name,
    category_type,
    parent_id,
    category_path,
    category_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    system_flag,
    create_by,
    update_by
) VALUES (
    :id,
    :tenantId,
    :categoryCode,
    :categoryName,
    :categoryType,
    0,
    CONCAT('/', :id, '/'),
    1,
    :sortNo,
    1,
    1,
    0,
    :operator,
    :operator
);
```

该写法要求业务侧提前生成分类 ID。如果使用数据库自增 ID，需要插入后再回写 `category_path`。

#### 创建子分类

创建子分类时，需要读取父分类的路径和层级，再生成子分类路径和层级。

```sql
START TRANSACTION;

SELECT
    id,
    category_path,
    category_level
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :parentId
  AND deleted = 0
FOR UPDATE;

INSERT INTO biz_category (
    id,
    tenant_id,
    category_code,
    category_name,
    category_type,
    parent_id,
    category_path,
    category_level,
    sort_no,
    leaf_flag,
    enabled_flag,
    system_flag,
    create_by,
    update_by
)
SELECT
    :id,
    :tenantId,
    :categoryCode,
    :categoryName,
    parent.category_type,
    parent.id,
    CONCAT(parent.category_path, :id, '/'),
    parent.category_level + 1,
    :sortNo,
    1,
    1,
    0,
    :operator,
    :operator
FROM biz_category parent
WHERE parent.tenant_id = :tenantId
  AND parent.category_type = :categoryType
  AND parent.id = :parentId
  AND parent.deleted = 0;

UPDATE biz_category
SET leaf_flag = 0,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :parentId
  AND deleted = 0;

COMMIT;
```

创建子分类时需要锁定父分类，避免并发创建导致父分类叶子状态维护不一致。

#### 修改分类基础信息

修改分类名称、编码、排序、图标、描述、启用状态等基础字段时，不需要修改子分类路径。

```sql
UPDATE biz_category
SET category_code = :categoryCode,
    category_name = :categoryName,
    sort_no = :sortNo,
    icon_url = :iconUrl,
    description = :description,
    enabled_flag = :enabledFlag,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :id
  AND deleted = 0
  AND version = :version;
```

如果分类是系统内置分类，可以限制只允许修改名称、排序、图标和启用状态，不允许修改编码和类型。

#### 移动分类

移动分类时需要校验目标父分类不能是当前分类本身或当前分类的下级分类，否则会形成循环结构。

```sql
SELECT
    COUNT(1) AS invalid_count
FROM biz_category target_parent
WHERE target_parent.tenant_id = :tenantId
  AND target_parent.category_type = :categoryType
  AND target_parent.id = :newParentId
  AND target_parent.deleted = 0
  AND target_parent.category_path LIKE CONCAT(:oldCategoryPath, '%');
```

如果 `invalid_count > 0`，表示目标父分类位于当前分类子树中，必须拒绝移动。

移动分类时需要同步更新当前分类及其所有子分类的路径和层级。

```sql
START TRANSACTION;

UPDATE biz_category
SET parent_id = CASE
        WHEN id = :categoryId THEN :newParentId
        ELSE parent_id
    END,
    category_path = CONCAT(:newCategoryPath, SUBSTRING(category_path, CHAR_LENGTH(:oldCategoryPath) + 1)),
    category_level = category_level + :levelDelta,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND category_path LIKE CONCAT(:oldCategoryPath, '%')
  AND deleted = 0;

UPDATE biz_category
SET leaf_flag = CASE
        WHEN EXISTS (
            SELECT 1
            FROM (
                SELECT id
                FROM biz_category
                WHERE tenant_id = :tenantId
                  AND category_type = :categoryType
                  AND parent_id = :oldParentId
                  AND deleted = 0
                LIMIT 1
            ) AS child_exists
        ) THEN 0
        ELSE 1
    END,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :oldParentId
  AND deleted = 0;

UPDATE biz_category
SET leaf_flag = 0,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :newParentId
  AND deleted = 0;

COMMIT;
```

分类移动会影响子分类路径，也可能影响分类下业务对象的聚合查询结果。生产系统中应记录操作日志，便于追踪分类结构变更。

#### 删除分类

删除分类前需要明确业务规则：是否允许删除非叶子分类，是否允许删除已经被业务对象引用的分类。

只允许删除叶子分类时，先检查是否存在子分类。

```sql
SELECT
    COUNT(1) AS child_count
FROM biz_category
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND parent_id = :categoryId
  AND deleted = 0;
```

再检查是否存在业务对象引用。

```sql
SELECT
    COUNT(1) AS ref_count
FROM biz_article
WHERE tenant_id = :tenantId
  AND category_id = :categoryId
  AND deleted = 0;
```

如果分类没有子分类，也没有业务引用，可以逻辑删除。

```sql
UPDATE biz_category
SET deleted = 1,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :categoryId
  AND system_flag = 0
  AND deleted = 0;
```

如果允许删除整棵分类子树，需要同时处理分类本身、子分类、关系表绑定数据和父分类叶子状态。

```sql
START TRANSACTION;

UPDATE biz_category
SET deleted = 1,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND category_path LIKE CONCAT(:categoryPath, '%')
  AND system_flag = 0
  AND deleted = 0;

UPDATE biz_object_category_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_id IN (
      SELECT id
      FROM (
          SELECT id
          FROM biz_category
          WHERE tenant_id = :tenantId
            AND category_type = :categoryType
            AND category_path LIKE CONCAT(:categoryPath, '%')
      ) AS deleted_category
  )
  AND deleted = 0;

UPDATE biz_category
SET leaf_flag = CASE
        WHEN EXISTS (
            SELECT 1
            FROM (
                SELECT id
                FROM biz_category
                WHERE tenant_id = :tenantId
                  AND category_type = :categoryType
                  AND parent_id = :parentId
                  AND deleted = 0
                LIMIT 1
            ) AS child_exists
        ) THEN 0
        ELSE 1
    END,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND category_type = :categoryType
  AND id = :parentId
  AND deleted = 0;

COMMIT;
```

删除分类是高风险操作。对于已经被订单、商品、文章、资源引用的分类，建议优先禁用而不是删除。

#### 绑定对象分类

多分类场景下，对象和分类之间通过关系表绑定。

```sql
INSERT INTO biz_object_category_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    category_id,
    main_flag,
    sort_no,
    create_by,
    update_by
) VALUES (
    :id,
    :tenantId,
    :objectType,
    :objectId,
    :categoryId,
    :mainFlag,
    :sortNo,
    :operator,
    :operator
);
```

绑定前应校验分类存在、分类启用、分类未删除。如果业务只允许绑定叶子分类，还需要校验 `leaf_flag = 1`。

#### 批量重置对象分类

对象编辑页面常见做法是先删除旧关系，再插入新关系。该操作应放在事务中执行。

```sql
START TRANSACTION;

UPDATE biz_object_category_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND deleted = 0;

INSERT INTO biz_object_category_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    category_id,
    main_flag,
    sort_no,
    create_by,
    update_by
) VALUES
    (:id1, :tenantId, :objectType, :objectId, :categoryId1, 1, 1, :operator, :operator),
    (:id2, :tenantId, :objectType, :objectId, :categoryId2, 0, 2, :operator, :operator),
    (:id3, :tenantId, :objectType, :objectId, :categoryId3, 0, 3, :operator, :operator);

COMMIT;
```

如果需要支持高并发编辑，应结合唯一索引、乐观锁或业务版本号，避免重复绑定和覆盖更新。

### 常见问题

分类模型常见问题主要集中在分类关系不清晰、分类编码混乱、删除规则不明确和查询路径不稳定。

| 问题                   | 原因                               | 建议                                                         |
| ---------------------- | ---------------------------------- | ------------------------------------------------------------ |
| 分类和标签混用         | 分类用于固定归类，标签用于灵活标记 | 分类和标签分表建模，不要共用一张表承载所有语义               |
| 分类编码重复           | 缺少分类类型和租户维度约束         | 使用 `tenant_id + category_type + category_code + deleted` 唯一约束 |
| 分类删除后历史数据异常 | 已有关联数据仍然物理删除分类       | 使用逻辑删除，历史数据继续保留分类 ID                        |
| 禁用和删除语义混乱     | 把不可选分类直接删除               | 不可新增选择时使用禁用，彻底下线时才逻辑删除                 |
| 子分类查询慢           | 没有针对 `parent_id` 建索引        | 建立分类类型、父分类、状态、排序组合索引                     |
| 分类子树查询慢         | 没有维护 `category_path`           | 读多场景维护路径字段，并限制最大层级                         |
| 分类移动后路径错乱     | 只更新当前分类，没有更新子分类     | 移动时批量更新整个分类子树                                   |
| 多分类重复绑定         | 关系表缺少唯一约束                 | 对 `object_type + object_id + category_id` 建唯一约束        |
| 主分类不唯一           | 多分类关系表没有控制 `main_flag`   | 服务层保证一个对象只有一个主分类，必要时增加业务校验         |
| 分类层级过深           | 没有限制分类最大深度               | 在服务层限制最大层级，例如不超过 5 级                        |
| 分类名称修改影响展示   | 列表页实时关联查询分类名称         | 高频列表可使用读模型冗余分类名称                             |
| 分类统计不准确         | 分类移动或删除后统计数据未刷新     | 分类统计应使用异步汇总或定时重算机制                         |

分类模型设计时应提前确定以下规则：

- 一个业务对象是单分类还是多分类。
- 分类是否支持多级结构。
- 是否只允许选择叶子分类。
- 分类是否允许移动。
- 分类删除前是否需要检查业务引用。
- 系统内置分类是否允许修改或删除。
- 分类名称是否允许同级重复。
- 分类编码是否对外暴露或参与业务规则。

这些规则应在数据库约束、服务层校验、后台管理页面和接口文档中保持一致。

### 总结

分类模型的核心是用分类表维护稳定的分类体系，用业务主表或关系表表达业务对象与分类之间的归属关系。

如果业务对象只允许一个分类，优先在业务主表中保存 `category_id`，结构简单、查询高效。如果业务对象允许多个分类，应使用对象分类关系表，分别支持按对象查询分类和按分类查询对象。

分类模型设计时需要重点关注四点：

- 分类体系要通过 `category_type` 明确边界，避免不同业务分类混在一起。
- 多级分类要维护 `parent_id`、`category_path` 和 `category_level`，提高树查询效率。
- 分类引用关系要根据单分类或多分类选择不同建模方式。
- 删除分类前必须处理业务引用，生产系统中更推荐禁用分类而不是直接删除分类。

分类模型通常会与树形层级模型、标签模型、附件资源模型组合使用。分类负责稳定归类，标签负责灵活标记，附件资源负责承载文件或媒体内容，三者共同支撑复杂业务对象的组织、筛选和展示。



## 标签模型

标签模型用于描述业务对象与多个灵活标记之间的关系。标签通常不强调严格层级，而是用于补充分类之外的筛选、检索、聚合和个性化展示能力。

分类模型解决“对象属于哪个固定分类”的问题，标签模型解决“对象具有什么特征”的问题。例如一篇文章可以属于“技术文档”分类，同时拥有“Java”“Spring Boot”“MySQL”“实战案例”等多个标签。

### 适用场景

标签模型适合业务对象需要被灵活标记、组合筛选、快速检索和多维展示的场景。

常见业务场景包括：

- 文章标签：Java、MySQL、Spring Boot、性能优化等。
- 商品标签：新品、热卖、推荐、限时折扣等。
- 用户标签：高价值用户、活跃用户、沉默用户、渠道用户等。
- 客户标签：重点客户、意向客户、续费客户、风险客户等。
- 工单标签：紧急、线上故障、待复盘、客户投诉等。
- 资源标签：图片、合同、发票、设计稿、归档资料等。
- 内容推荐：根据标签匹配相似内容或相似对象。
- 运营筛选：通过多个标签组合圈选目标数据。

标签模型适合标签数量较多、变化频繁、一个对象可以拥有多个标签的业务。它不适合表达稳定的上下级分类关系。如果业务需要严格层级、固定目录、父子结构，应优先使用分类模型或树形层级模型。

### 建模结构

标签模型通常由标签主表和对象标签关系表组成。标签主表保存标签本身的信息，对象标签关系表保存业务对象与标签之间的多对多关系。

标签主表负责维护标签名称、标签类型、标签分组、颜色、启用状态和使用次数等信息。

```sql
CREATE TABLE biz_tag (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    tag_code VARCHAR(64) NOT NULL COMMENT '标签编码，同一标签类型下建议唯一',
    tag_name VARCHAR(128) NOT NULL COMMENT '标签名称',
    tag_type VARCHAR(32) NOT NULL COMMENT '标签类型，如article、product、user、resource',
    tag_group VARCHAR(64) DEFAULT NULL COMMENT '标签分组，如system、business、custom、operation',
    tag_color VARCHAR(32) DEFAULT NULL COMMENT '标签颜色，如#409EFF、success、warning',
    description VARCHAR(500) DEFAULT NULL COMMENT '标签描述',
    usage_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '标签使用次数，可实时维护或异步汇总',
    system_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统内置：0否，1是',
    enabled_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='业务标签表';
```

对象标签关系表负责维护业务对象和标签之间的绑定关系。通过 `object_type` 支持多个业务模块共用一套关系表。

```sql
CREATE TABLE biz_object_tag_rel (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    object_type VARCHAR(32) NOT NULL COMMENT '对象类型，如article、product、user、resource',
    object_id BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型：manual手动，rule规则，import导入，system系统',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '标签在对象上的展示顺序',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='对象标签关系表';
```

建模时需要明确标签和业务对象的关系：

| 关系类型         | 设计方式                   | 适用场景                       |
| ---------------- | -------------------------- | ------------------------------ |
| 一个对象多个标签 | 使用对象标签关系表         | 文章、商品、用户、资源         |
| 一个标签多个对象 | 使用对象标签关系表反向查询 | 标签聚合页、标签筛选页         |
| 标签按业务隔离   | 使用 `tag_type`            | 文章标签、商品标签、用户标签   |
| 标签按来源区分   | 使用 `source_type`         | 手动标签、规则标签、系统标签   |
| 标签按展示分组   | 使用 `tag_group`           | 热门标签、系统标签、自定义标签 |

标签模型一般不建议把多个标签 ID 直接保存到业务主表的一个字符串字段中，例如 `tag_ids = '1,2,3'`。这种设计会导致查询困难、索引失效、关系维护复杂，也不利于统计和去重。

如果列表页需要展示标签名称，可以在读模型或宽表中冗余标签名称集合，但主关系仍应以对象标签关系表为准。

### 字段设计

标签字段设计应围绕标签标识、业务隔离、展示属性、统计属性、状态控制和审计追踪展开。

| 字段分类 | 字段                                                   | 说明                                     |
| -------- | ------------------------------------------------------ | ---------------------------------------- |
| 主键字段 | `id`                                                   | 标签唯一标识，建议使用业务侧生成 ID      |
| 租户字段 | `tenant_id`                                            | 多租户隔离字段                           |
| 编码字段 | `tag_code`                                             | 标签编码，适合接口、规则、配置引用       |
| 名称字段 | `tag_name`                                             | 标签展示名称                             |
| 类型字段 | `tag_type`                                             | 标签类型，用于区分不同业务对象的标签体系 |
| 分组字段 | `tag_group`                                            | 标签分组，用于后台管理和前端展示         |
| 展示字段 | `tag_color`                                            | 标签颜色或样式标识                       |
| 说明字段 | `description`                                          | 标签说明                                 |
| 统计字段 | `usage_count`                                          | 标签使用次数                             |
| 状态字段 | `system_flag`                                          | 是否系统内置                             |
| 状态字段 | `enabled_flag`                                         | 是否启用                                 |
| 状态字段 | `deleted`                                              | 逻辑删除标记                             |
| 并发字段 | `version`                                              | 乐观锁版本号                             |
| 审计字段 | `create_by`、`create_time`、`update_by`、`update_time` | 创建和更新信息                           |

对象标签关系表字段设计建议：

| 字段分类 | 字段                                                   | 说明                                 |
| -------- | ------------------------------------------------------ | ------------------------------------ |
| 主键字段 | `id`                                                   | 关系记录唯一标识                     |
| 租户字段 | `tenant_id`                                            | 多租户隔离字段                       |
| 对象字段 | `object_type`                                          | 被打标签对象的业务类型               |
| 对象字段 | `object_id`                                            | 被打标签对象的主键 ID                |
| 标签字段 | `tag_id`                                               | 标签 ID                              |
| 来源字段 | `source_type`                                          | 标签来源，例如手动、规则、导入、系统 |
| 展示字段 | `sort_no`                                              | 对象详情页中的标签展示顺序           |
| 状态字段 | `deleted`                                              | 逻辑删除标记                         |
| 审计字段 | `create_by`、`create_time`、`update_by`、`update_time` | 关系创建和更新信息                   |

字段设计建议：

- `tag_type` 是标签体系边界，不同业务对象的标签不要只靠名称区分。
- `tag_code` 适合系统内置标签和规则标签，不建议完全依赖 `tag_name`。
- `tag_name` 可以用于展示，但要注意大小写、空格、全角半角和特殊符号的归一化。
- `tag_group` 只负责管理和展示分组，不建议承担业务权限或业务状态含义。
- `usage_count` 可以实时维护，也可以异步汇总；高并发写入场景建议异步汇总。
- `source_type` 适合区分手动标签和系统规则标签，便于后续批量清理或规则重算。
- `system_flag = 1` 的标签通常不允许删除，只允许禁用或修改展示属性。
- `enabled_flag = 0` 表示标签不可继续选择，但历史绑定关系可以保留。
- `deleted = 1` 表示标签不再展示，也不允许新增绑定。

### 索引设计

索引设计需要覆盖标签管理、标签搜索、对象查标签、标签查对象、多标签筛选和使用次数排序等访问路径。

```sql
ALTER TABLE biz_tag
    ADD PRIMARY KEY (id);
```

主键索引用于按标签 ID 精确定位标签信息。

```sql
CREATE UNIQUE INDEX uk_tag_tenant_type_code_deleted
    ON biz_tag (tenant_id, tag_type, tag_code, deleted);
```

该唯一索引用于保证同一租户、同一标签类型下，未删除标签编码不重复。

```sql
CREATE UNIQUE INDEX uk_tag_tenant_type_name_deleted
    ON biz_tag (tenant_id, tag_type, tag_name, deleted);
```

该唯一索引用于避免同一标签类型下出现重复标签名称。如果业务允许同名标签属于不同分组，可以把 `tag_group` 加入唯一约束。

```sql
CREATE INDEX idx_tag_tenant_type_group_sort
    ON biz_tag (tenant_id, tag_type, tag_group, deleted, enabled_flag, sort_no, id);
```

如果标签需要按分组展示，可以增加 `sort_no` 字段。当前建模表中没有 `sort_no` 时，可以去掉该索引，或者在标签主表中补充 `sort_no INT NOT NULL DEFAULT 0 COMMENT '标签排序号'`。

更推荐在需要标签分组排序的场景中补充排序字段。

```sql
ALTER TABLE biz_tag
    ADD COLUMN sort_no INT NOT NULL DEFAULT 0 COMMENT '标签排序号' AFTER usage_count;
```

补充排序字段后，再创建分组展示索引。

```sql
CREATE INDEX idx_tag_tenant_type_group_sort
    ON biz_tag (tenant_id, tag_type, tag_group, deleted, enabled_flag, sort_no, id);
CREATE INDEX idx_tag_tenant_type_usage
    ON biz_tag (tenant_id, tag_type, deleted, enabled_flag, usage_count, id);
```

该索引用于查询热门标签、标签云和高频标签列表。

```sql
CREATE INDEX idx_tag_tenant_type_name
    ON biz_tag (tenant_id, tag_type, tag_name, deleted);
```

该索引用于标签名称精确匹配或前缀匹配。如果需要 `%关键字%` 形式的模糊搜索，应考虑全文索引、搜索辅助表或外部搜索服务。

关系表需要同时支持“按对象查标签”和“按标签查对象”。

```sql
ALTER TABLE biz_object_tag_rel
    ADD PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_rel_tenant_object_tag_deleted
    ON biz_object_tag_rel (tenant_id, object_type, object_id, tag_id, deleted);
```

该唯一索引用于避免同一个对象重复绑定同一个标签。

```sql
CREATE INDEX idx_rel_tenant_object
    ON biz_object_tag_rel (tenant_id, object_type, object_id, deleted, sort_no, tag_id);
```

该索引用于查询某个对象已经绑定的标签列表。

```sql
CREATE INDEX idx_rel_tenant_tag_object
    ON biz_object_tag_rel (tenant_id, tag_id, object_type, deleted, object_id);
```

该索引用于查询某个标签下有哪些业务对象。

```sql
CREATE INDEX idx_rel_tenant_object_type_tag
    ON biz_object_tag_rel (tenant_id, object_type, tag_id, deleted, object_id);
```

该索引用于按对象类型和标签筛选对象，适合内容列表、商品列表、客户列表等查询场景。

索引设计注意事项：

- 多租户系统中，标签表和关系表的核心索引都应以 `tenant_id` 开头。
- 标签主表查询通常需要带上 `tag_type`，避免不同业务标签互相干扰。
- 关系表必须支持双向查询：按对象查标签、按标签查对象。
- 多标签组合查询会依赖 `tag_id`、`object_type`、`object_id` 的组合索引。
- `tag_name LIKE '%xxx%'` 很难稳定利用普通 BTree 索引。
- `usage_count` 如果频繁更新，会带来写放大，需要评估实时统计还是异步统计。
- 如果标签关系数据量很大，可以按 `object_type` 或业务对象维度拆分关系表。

### 常用查询

常用查询应覆盖标签列表、标签搜索、对象标签回显、按标签查询对象、多标签交集查询、多标签并集查询和热门标签查询。

#### 查询启用标签列表

标签选择器通常只展示启用、未删除的标签。

```sql
SELECT
    id,
    tag_code,
    tag_name,
    tag_type,
    tag_group,
    tag_color,
    usage_count,
    system_flag,
    enabled_flag
FROM biz_tag
WHERE tenant_id = :tenantId
  AND tag_type = :tagType
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY tag_group ASC, sort_no ASC, id ASC;
```

该查询适合新增或编辑页面加载可选标签。如果标签数量很多，应使用分页或按关键字搜索。

#### 按标签名称搜索

标签搜索常用于输入框联想、标签选择器和后台管理查询。

```sql
SELECT
    id,
    tag_code,
    tag_name,
    tag_type,
    tag_group,
    tag_color,
    usage_count
FROM biz_tag
WHERE tenant_id = :tenantId
  AND tag_type = :tagType
  AND tag_name LIKE CONCAT(:keyword, '%')
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY usage_count DESC, id DESC
LIMIT :limitSize;
```

该查询使用前缀匹配，普通索引更容易生效。如果需要包含式搜索，应使用全文索引或搜索辅助表。

#### 查询对象已绑定标签

对象详情页和编辑页通常需要回显当前对象已经绑定的标签。

```sql
SELECT
    tag.id,
    tag.tag_code,
    tag.tag_name,
    tag.tag_type,
    tag.tag_group,
    tag.tag_color,
    rel.source_type,
    rel.sort_no
FROM biz_object_tag_rel rel
INNER JOIN biz_tag tag
    ON tag.id = rel.tag_id
   AND tag.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.object_id = :objectId
  AND rel.deleted = 0
  AND tag.deleted = 0
ORDER BY rel.sort_no ASC, tag.id ASC;
```

该查询返回对象绑定的所有标签。如果编辑页面只允许展示启用标签，可以额外增加 `AND tag.enabled_flag = 1`。

#### 按单个标签查询对象

按标签查询对象适合标签聚合页，例如查询所有带有“Spring Boot”标签的文章。

```sql
SELECT
    rel.object_id,
    rel.create_time AS bind_time
FROM biz_object_tag_rel rel
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.tag_id = :tagId
  AND rel.deleted = 0
ORDER BY rel.object_id DESC
LIMIT :offset, :pageSize;
```

该查询只返回对象 ID。实际业务中通常会再关联业务主表查询对象详情。

```sql
SELECT
    article.id,
    article.article_title,
    article.article_status,
    article.publish_time
FROM biz_object_tag_rel rel
INNER JOIN biz_article article
    ON article.id = rel.object_id
   AND article.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = 'article'
  AND rel.tag_id = :tagId
  AND rel.deleted = 0
  AND article.deleted = 0
  AND article.article_status = 'published'
ORDER BY article.publish_time DESC, article.id DESC
LIMIT :offset, :pageSize;
```

该查询适合内容列表页，但在大数据量场景中需要关注分页性能。

#### 按多个标签查询对象并集

多标签并集表示对象命中任意一个标签即可返回。例如查询包含“Java”或“MySQL”的文章。

```sql
SELECT DISTINCT
    rel.object_id
FROM biz_object_tag_rel rel
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.tag_id IN (:tagIdList)
  AND rel.deleted = 0
ORDER BY rel.object_id DESC
LIMIT :offset, :pageSize;
```

并集查询适合宽松筛选和推荐场景。由于使用 `DISTINCT`，标签命中数据量大时需要关注临时表和排序成本。

#### 按多个标签查询对象交集

多标签交集表示对象必须同时拥有所有指定标签。例如查询同时包含“Java”“Spring Boot”“MySQL”的文章。

```sql
SELECT
    rel.object_id
FROM biz_object_tag_rel rel
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.tag_id IN (:tagIdList)
  AND rel.deleted = 0
GROUP BY rel.object_id
HAVING COUNT(DISTINCT rel.tag_id) = :tagCount
ORDER BY rel.object_id DESC
LIMIT :offset, :pageSize;
```

`:tagCount` 应等于 `:tagIdList` 中去重后的标签数量。交集查询适合精准筛选，但标签组合越多，查询成本越高。

#### 查询热门标签

热门标签通常根据 `usage_count` 排序，用于标签云、推荐标签和运营看板。

```sql
SELECT
    id,
    tag_code,
    tag_name,
    tag_type,
    tag_group,
    tag_color,
    usage_count
FROM biz_tag
WHERE tenant_id = :tenantId
  AND tag_type = :tagType
  AND deleted = 0
  AND enabled_flag = 1
ORDER BY usage_count DESC, id DESC
LIMIT :limitSize;
```

如果 `usage_count` 是异步汇总字段，热门标签结果会存在短时间延迟，但整体写入性能更稳定。

#### 统计对象标签数量

该查询用于统计某类对象下各标签的使用数量，也可以用于校准 `usage_count`。

```sql
SELECT
    tag.id,
    tag.tag_name,
    COUNT(rel.id) AS bind_count
FROM biz_tag tag
LEFT JOIN biz_object_tag_rel rel
    ON rel.tag_id = tag.id
   AND rel.tenant_id = tag.tenant_id
   AND rel.object_type = :objectType
   AND rel.deleted = 0
WHERE tag.tenant_id = :tenantId
  AND tag.tag_type = :tagType
  AND tag.deleted = 0
GROUP BY
    tag.id,
    tag.tag_name
ORDER BY bind_count DESC, tag.id DESC;
```

该查询适合后台统计，不建议在高频接口中实时执行。大数据量场景应使用汇总表或定时任务。

#### 查询相似对象

相似对象可以通过共同标签数量进行简单匹配。例如查询与当前对象拥有相同标签的其他对象。

```sql
SELECT
    other_rel.object_id,
    COUNT(DISTINCT other_rel.tag_id) AS same_tag_count
FROM biz_object_tag_rel current_rel
INNER JOIN biz_object_tag_rel other_rel
    ON other_rel.tenant_id = current_rel.tenant_id
   AND other_rel.object_type = current_rel.object_type
   AND other_rel.tag_id = current_rel.tag_id
   AND other_rel.deleted = 0
WHERE current_rel.tenant_id = :tenantId
  AND current_rel.object_type = :objectType
  AND current_rel.object_id = :objectId
  AND current_rel.deleted = 0
  AND other_rel.object_id <> :objectId
GROUP BY other_rel.object_id
ORDER BY same_tag_count DESC, other_rel.object_id DESC
LIMIT :limitSize;
```

该查询适合轻量推荐。如果推荐逻辑复杂，应使用专门的推荐模型或离线计算结果表。

### 常用写入

标签模型的写入操作包括创建标签、修改标签、绑定标签、重置对象标签、解绑标签、合并标签和删除标签。关系写入需要重点保证去重和统计一致性。

#### 创建标签

创建标签时，需要保证同一租户、同一标签类型下标签编码或名称不重复。

```sql
INSERT INTO biz_tag (
    id,
    tenant_id,
    tag_code,
    tag_name,
    tag_type,
    tag_group,
    tag_color,
    description,
    usage_count,
    system_flag,
    enabled_flag,
    create_by,
    update_by
) VALUES (
    :id,
    :tenantId,
    :tagCode,
    :tagName,
    :tagType,
    :tagGroup,
    :tagColor,
    :description,
    0,
    0,
    1,
    :operator,
    :operator
);
```

如果标签由用户输入创建，建议在服务层先做名称归一化，例如去除首尾空格、统一大小写规则、限制特殊字符。

#### 修改标签基础信息

修改标签名称、颜色、分组、描述、启用状态时，不影响对象标签关系。

```sql
UPDATE biz_tag
SET tag_name = :tagName,
    tag_group = :tagGroup,
    tag_color = :tagColor,
    description = :description,
    enabled_flag = :enabledFlag,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND tag_type = :tagType
  AND id = :id
  AND deleted = 0
  AND version = :version;
```

如果标签是系统内置标签，可以限制不允许修改 `tag_code`，只允许调整展示属性。

#### 绑定单个标签

绑定标签时需要校验标签存在、未删除、已启用，并通过唯一索引避免重复绑定。

```sql
START TRANSACTION;

INSERT INTO biz_object_tag_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    tag_id,
    source_type,
    sort_no,
    create_by,
    update_by
)
SELECT
    :id,
    :tenantId,
    :objectType,
    :objectId,
    tag.id,
    :sourceType,
    :sortNo,
    :operator,
    :operator
FROM biz_tag tag
WHERE tag.tenant_id = :tenantId
  AND tag.tag_type = :tagType
  AND tag.id = :tagId
  AND tag.deleted = 0
  AND tag.enabled_flag = 1;

UPDATE biz_tag
SET usage_count = usage_count + 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :tagId
  AND deleted = 0;

COMMIT;
```

如果高并发场景下重复绑定概率较高，可以使用唯一索引兜底，并在业务层捕获重复键异常。

#### 批量绑定标签

批量绑定适合对象创建或编辑时一次性提交多个标签。

```sql
INSERT INTO biz_object_tag_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    tag_id,
    source_type,
    sort_no,
    create_by,
    update_by
) VALUES
    (:id1, :tenantId, :objectType, :objectId, :tagId1, :sourceType, 1, :operator, :operator),
    (:id2, :tenantId, :objectType, :objectId, :tagId2, :sourceType, 2, :operator, :operator),
    (:id3, :tenantId, :objectType, :objectId, :tagId3, :sourceType, 3, :operator, :operator);
```

批量绑定前建议先校验标签 ID 是否全部属于当前租户、当前标签类型，并且处于启用状态。

#### 重置对象标签

对象编辑页面常见做法是先逻辑删除旧标签关系，再插入新的标签关系。

```sql
START TRANSACTION;

UPDATE biz_object_tag_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND deleted = 0;

INSERT INTO biz_object_tag_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    tag_id,
    source_type,
    sort_no,
    create_by,
    update_by
) VALUES
    (:id1, :tenantId, :objectType, :objectId, :tagId1, 'manual', 1, :operator, :operator),
    (:id2, :tenantId, :objectType, :objectId, :tagId2, 'manual', 2, :operator, :operator);

COMMIT;
```

这种写法实现简单，但会导致标签使用次数需要重新统计。对于 `usage_count` 要求准确的业务，建议在事务中分别计算新增和移除的标签，或者通过异步任务重算。

#### 解绑单个标签

解绑标签通常采用逻辑删除关系记录。

```sql
START TRANSACTION;

UPDATE biz_object_tag_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND tag_id = :tagId
  AND deleted = 0;

UPDATE biz_tag
SET usage_count = CASE
        WHEN usage_count > 0 THEN usage_count - 1
        ELSE 0
    END,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :tagId
  AND deleted = 0;

COMMIT;
```

如果业务允许重复导入或规则重算，解绑前需要确认该标签来源是否允许被手动移除。

#### 合并标签

标签合并用于处理重复标签，例如把“SpringBoot”合并到“Spring Boot”。

```sql
START TRANSACTION;

UPDATE biz_object_tag_rel source_rel
SET source_rel.tag_id = :targetTagId,
    source_rel.update_by = :operator,
    source_rel.update_time = CURRENT_TIMESTAMP
WHERE source_rel.tenant_id = :tenantId
  AND source_rel.tag_id = :sourceTagId
  AND source_rel.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM (
          SELECT object_type, object_id
          FROM biz_object_tag_rel
          WHERE tenant_id = :tenantId
            AND tag_id = :targetTagId
            AND deleted = 0
      ) AS target_rel
      WHERE target_rel.object_type = source_rel.object_type
        AND target_rel.object_id = source_rel.object_id
  );

UPDATE biz_object_tag_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND tag_id = :sourceTagId
  AND deleted = 0;

UPDATE biz_tag
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :sourceTagId
  AND system_flag = 0
  AND deleted = 0;

COMMIT;
```

标签合并后建议异步重算源标签和目标标签的 `usage_count`，避免实时计算复杂化。

#### 删除标签

删除标签前需要判断是否已经被对象绑定。生产系统中更推荐禁用标签，而不是直接删除。

```sql
SELECT
    COUNT(1) AS ref_count
FROM biz_object_tag_rel
WHERE tenant_id = :tenantId
  AND tag_id = :tagId
  AND deleted = 0;
```

如果标签没有绑定关系，可以逻辑删除。

```sql
UPDATE biz_tag
SET deleted = 1,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :tagId
  AND system_flag = 0
  AND deleted = 0;
```

如果标签已有绑定关系，建议先禁用标签，禁止继续选择，但保留历史绑定。

```sql
UPDATE biz_tag
SET enabled_flag = 0,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :tagId
  AND deleted = 0;
```

#### 重算标签使用次数

当标签关系发生批量导入、批量删除、标签合并时，可以通过汇总关系表重算 `usage_count`。

```sql
UPDATE biz_tag tag
LEFT JOIN (
    SELECT
        tag_id,
        COUNT(1) AS bind_count
    FROM biz_object_tag_rel
    WHERE tenant_id = :tenantId
      AND deleted = 0
    GROUP BY tag_id
) rel_stat
    ON rel_stat.tag_id = tag.id
SET tag.usage_count = COALESCE(rel_stat.bind_count, 0),
    tag.update_time = CURRENT_TIMESTAMP
WHERE tag.tenant_id = :tenantId
  AND tag.deleted = 0;
```

该操作适合定时任务或运维修复，不建议在高频接口中执行。

### 常见问题

标签模型常见问题集中在标签重复、标签滥用、关系表膨胀、多标签查询性能和统计一致性方面。

| 问题                 | 原因                           | 建议                                             |
| -------------------- | ------------------------------ | ------------------------------------------------ |
| 标签和分类混用       | 没有区分固定归类和灵活标记     | 分类用于稳定目录，标签用于灵活特征               |
| 标签重复             | 用户输入未归一化，缺少唯一约束 | 对标签名称做标准化，并建立唯一索引               |
| 标签名称混乱         | 大小写、空格、同义词不统一     | 增加标签审核、标签合并和别名机制                 |
| 标签数量失控         | 允许用户随意创建标签           | 设置创建权限、审核流程或推荐标签                 |
| 对象重复绑定标签     | 关系表缺少唯一约束             | 建立 `object_type + object_id + tag_id` 唯一约束 |
| 多标签交集查询慢     | 关系表数据量大，组合筛选复杂   | 使用复合索引、搜索辅助表或离线倒排索引           |
| 热门标签不准确       | `usage_count` 维护不一致       | 使用异步汇总或定时重算                           |
| 删除标签影响历史数据 | 物理删除标签或关系记录         | 使用逻辑删除，优先禁用标签                       |
| 系统标签被误删       | 没有区分系统标签和自定义标签   | 使用 `system_flag` 并在服务层限制删除            |
| 标签来源不可追溯     | 关系表没有记录来源             | 使用 `source_type` 区分手动、规则、导入和系统    |
| 标签关系表过大       | 所有对象共用一张关系表且无归档 | 按对象类型拆表或建立归档策略                     |
| 列表页标签展示慢     | 每行都实时关联标签表           | 使用读模型冗余标签名称集合                       |

标签模型设计时应提前确定以下规则：

- 是否允许用户自由创建标签。
- 标签名称是否区分大小写。
- 标签是否需要审核。
- 标签是否区分业务类型。
- 标签是否有系统内置和用户自定义之分。
- 一个对象最多允许绑定多少个标签。
- 是否允许规则自动打标签。
- 标签删除时是否保留历史绑定。
- 热门标签是否要求实时准确。
- 多标签筛选是交集查询还是并集查询。

这些规则需要在数据库约束、服务层校验、后台管理页面、导入任务和搜索查询中保持一致。

### 总结

标签模型的核心是通过标签主表维护标签元数据，通过对象标签关系表表达业务对象与标签之间的多对多关系。

标签模型适合灵活标记和组合筛选，不适合表达严格的层级结构。分类负责稳定归类，标签负责特征标记，两者应分开建模，不要混用。

设计标签模型时需要重点关注四点：

- 标签主表通过 `tag_type` 区分业务标签体系，通过 `tag_code` 和 `tag_name` 保证标签可管理。
- 关系表通过 `object_type + object_id + tag_id` 表达多对多绑定关系，并通过唯一索引避免重复绑定。
- 高频查询要同时支持按对象查标签、按标签查对象、多标签交集和多标签并集。
- 标签统计字段如 `usage_count` 要明确实时维护还是异步汇总，避免高并发写入下统计不一致。

在中小规模业务中，标签表加关系表已经可以满足大多数查询需求。如果标签关系数据量很大、标签组合筛选非常频繁，或者需要复杂推荐能力，应进一步引入搜索辅助表、倒排索引、缓存或独立搜索服务。



## 附件资源模型

附件资源模型用于描述业务系统中文件、图片、视频、文档等资源的元数据管理，以及资源与业务对象之间的绑定关系。该模型不直接保存文件二进制内容，而是保存文件在对象存储、本地存储或第三方存储中的访问位置、文件属性、状态和引用关系。

附件资源模型的核心是把“文件本身”和“文件被哪个业务对象使用”分开建模。附件表负责管理资源元数据，关系表负责管理业务对象与附件之间的挂载关系。

### 适用场景

附件资源模型适合业务对象需要上传、查看、下载、预览、归档或复用文件资源的场景。

常见业务场景包括：

- 用户头像、企业 Logo、商品图片、文章封面图。
- 合同附件、发票附件、报销凭证、审批材料。
- 工单截图、问题日志、客户反馈文件。
- 知识库文档、压缩包、安装包、导入模板。
- 富文本编辑器中的图片、视频和文件资源。
- 业务单据的多个附件，例如订单附件、售后附件、项目附件。
- 文件资源复用，例如同一个文件被多个业务对象引用。
- 临时上传文件转正式业务附件，例如先上传后提交表单。

附件资源模型适合文件元数据查询、权限校验、业务绑定、资源清理和下载审计。如果业务需要全文检索文件内容，应额外设计文件解析表、搜索辅助表或接入外部搜索服务。

### 建模结构

附件资源模型建议拆成两张核心表：附件资源表和对象附件关系表。

附件资源表只保存文件本身的元数据，例如文件名、扩展名、大小、哈希、存储桶、对象 Key、访问地址、上传状态等。

```sql
CREATE TABLE biz_attachment (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_ext VARCHAR(32) DEFAULT NULL COMMENT '文件扩展名，如jpg、png、pdf',
    content_type VARCHAR(128) DEFAULT NULL COMMENT '文件MIME类型，如image/png、application/pdf',
    file_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件哈希值，如MD5、SHA256，用于去重或校验',
    storage_type VARCHAR(32) NOT NULL COMMENT '存储类型：local本地，minio对象存储，oss阿里云，cos腾讯云，s3亚马逊',
    bucket_name VARCHAR(128) DEFAULT NULL COMMENT '存储桶名称',
    object_key VARCHAR(500) NOT NULL COMMENT '对象存储Key或本地相对路径',
    access_url VARCHAR(1000) DEFAULT NULL COMMENT '访问地址，可保存公开地址或内部地址',
    access_scope VARCHAR(32) NOT NULL DEFAULT 'private' COMMENT '访问范围：private私有，public公开',
    resource_type VARCHAR(32) NOT NULL DEFAULT 'file' COMMENT '资源类型：image图片，video视频，audio音频，document文档，file普通文件',
    file_status VARCHAR(32) NOT NULL DEFAULT 'temporary' COMMENT '文件状态：temporary临时，active有效，disabled禁用，deleted已删除',
    ref_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '引用次数，用于判断资源是否仍被业务使用',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间，临时文件可使用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='附件资源表';
```

对象附件关系表保存业务对象与附件之间的绑定关系。通过 `object_type` 和 `object_id` 支持多业务模块共用一张附件关系表。

```sql
CREATE TABLE biz_object_attachment_rel (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID，建议由业务侧雪花算法生成',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    object_type VARCHAR(32) NOT NULL COMMENT '对象类型，如user、article、product、order、ticket',
    object_id BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
    attachment_id BIGINT UNSIGNED NOT NULL COMMENT '附件ID',
    attachment_biz_type VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '附件业务类型，如cover封面，image图片，contract合同，invoice发票',
    display_name VARCHAR(255) DEFAULT NULL COMMENT '附件展示名称，不填时使用原始文件名',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '展示排序号，值越小越靠前',
    main_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否主附件：0否，1是，如主图、封面图',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='对象附件关系表';
```

如果系统需要严格记录文件上传、下载、预览、删除等行为，可以额外增加附件操作日志表。

```sql
CREATE TABLE biz_attachment_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    attachment_id BIGINT UNSIGNED NOT NULL COMMENT '附件ID',
    object_type VARCHAR(32) DEFAULT NULL COMMENT '对象类型',
    object_id BIGINT UNSIGNED DEFAULT NULL COMMENT '对象ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型：upload上传，download下载，preview预览，delete删除',
    operation_result VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT '操作结果：success成功，fail失败',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) COMMENT='附件操作日志表';
```

建模时应明确附件资源与业务对象之间的关系：

| 关系类型           | 设计方式                                                  | 适用场景                             |
| ------------------ | --------------------------------------------------------- | ------------------------------------ |
| 一个对象多个附件   | 使用对象附件关系表                                        | 订单附件、工单截图、审批材料         |
| 一个附件多个对象   | 使用对象附件关系表复用附件                                | 公共模板、共享资料、重复上传文件复用 |
| 单个封面图         | 关系表使用 `main_flag` 或业务表保存 `cover_attachment_id` | 商品主图、文章封面                   |
| 附件分业务类型     | 使用 `attachment_biz_type`                                | 合同、发票、截图、说明文档           |
| 临时文件转正式文件 | 附件表使用 `file_status`                                  | 先上传文件，再提交业务表单           |
| 私有文件访问控制   | 使用 `access_scope` 并通过后端签名下载                    | 合同、发票、内部资料                 |

附件资源模型不建议把文件访问地址直接散落保存到各个业务表中。这样会导致资源清理困难、权限控制分散、文件迁移成本高，也不利于统计文件引用和操作日志。

### 字段设计

字段设计应围绕文件元数据、存储位置、访问控制、业务绑定、生命周期和审计追踪展开。

| 字段分类     | 字段                                                   | 说明                                   |
| ------------ | ------------------------------------------------------ | -------------------------------------- |
| 主键字段     | `id`                                                   | 附件唯一标识，建议使用业务侧生成 ID    |
| 租户字段     | `tenant_id`                                            | 多租户隔离字段                         |
| 文件字段     | `file_name`                                            | 原始文件名                             |
| 文件字段     | `file_ext`                                             | 文件扩展名                             |
| 文件字段     | `content_type`                                         | 文件 MIME 类型                         |
| 文件字段     | `file_size`                                            | 文件大小，单位字节                     |
| 校验字段     | `file_hash`                                            | 文件哈希，用于去重、秒传、完整性校验   |
| 存储字段     | `storage_type`                                         | 存储类型，如 MinIO、OSS、COS、S3、本地 |
| 存储字段     | `bucket_name`                                          | 存储桶名称                             |
| 存储字段     | `object_key`                                           | 文件在存储系统中的对象 Key 或相对路径  |
| 访问字段     | `access_url`                                           | 文件访问地址                           |
| 访问字段     | `access_scope`                                         | 公开或私有                             |
| 类型字段     | `resource_type`                                        | 图片、视频、音频、文档或普通文件       |
| 状态字段     | `file_status`                                          | 临时、有效、禁用、删除                 |
| 统计字段     | `ref_count`                                            | 文件引用次数                           |
| 生命周期字段 | `expire_time`                                          | 临时文件过期时间                       |
| 状态字段     | `deleted`                                              | 逻辑删除标记                           |
| 并发字段     | `version`                                              | 乐观锁版本号                           |
| 审计字段     | `create_by`、`create_time`、`update_by`、`update_time` | 创建和更新信息                         |

对象附件关系表字段设计建议：

| 字段分类 | 字段                                                   | 说明                     |
| -------- | ------------------------------------------------------ | ------------------------ |
| 主键字段 | `id`                                                   | 关系记录唯一标识         |
| 租户字段 | `tenant_id`                                            | 多租户隔离字段           |
| 对象字段 | `object_type`                                          | 被挂载附件的业务对象类型 |
| 对象字段 | `object_id`                                            | 被挂载附件的业务对象 ID  |
| 附件字段 | `attachment_id`                                        | 附件 ID                  |
| 业务字段 | `attachment_biz_type`                                  | 附件在业务中的用途       |
| 展示字段 | `display_name`                                         | 附件展示名称             |
| 展示字段 | `sort_no`                                              | 展示排序号               |
| 标识字段 | `main_flag`                                            | 是否主附件               |
| 状态字段 | `deleted`                                              | 逻辑删除标记             |
| 审计字段 | `create_by`、`create_time`、`update_by`、`update_time` | 关系创建和更新信息       |

字段设计建议：

- `file_name` 保存用户上传时的原始文件名，不建议直接作为存储路径。
- `object_key` 应由系统生成，避免文件名冲突和路径穿越风险。
- `file_ext` 应从文件名或 MIME 解析后标准化保存，例如统一小写。
- `content_type` 不应完全信任前端传值，后端应做文件类型校验。
- `file_hash` 可用于文件秒传、重复文件识别和完整性校验。
- `access_url` 可以不存储，改为通过 `storage_type + bucket_name + object_key` 动态生成签名 URL。
- `access_scope = private` 的文件不应直接暴露永久公网地址。
- `file_status = temporary` 的文件应设置 `expire_time`，便于定时清理。
- `ref_count` 是冗余统计字段，需要在绑定和解绑时维护，或通过定时任务重算。
- `attachment_biz_type` 用于区分同一对象下不同用途的附件，例如封面图、详情图、合同、发票。
- `main_flag` 适合主图、封面图、默认附件等业务，但需要服务层保证同一对象同一业务类型下只有一个主附件。

### 索引设计

索引设计需要覆盖附件定位、对象查附件、附件查引用、临时文件清理、哈希去重和日志审计等访问路径。

```sql
ALTER TABLE biz_attachment
    ADD PRIMARY KEY (id);
```

主键索引用于按附件 ID 精确定位附件元数据。

```sql
CREATE INDEX idx_attachment_tenant_status_time
    ON biz_attachment (tenant_id, file_status, deleted, create_time, id);
```

该索引用于按文件状态查询附件，例如查询临时文件、有效文件或待清理文件。

```sql
CREATE INDEX idx_attachment_tenant_hash
    ON biz_attachment (tenant_id, file_hash, file_size, deleted);
```

该索引用于文件去重、秒传和完整性校验。仅使用哈希可能存在极小概率冲突，建议同时比较文件大小。

```sql
CREATE INDEX idx_attachment_tenant_storage_key
    ON biz_attachment (tenant_id, storage_type, bucket_name, object_key(255), deleted);
```

该索引用于根据存储位置定位附件，例如对象存储回调、文件迁移、资源校验等场景。

```sql
CREATE INDEX idx_attachment_tenant_expire
    ON biz_attachment (tenant_id, file_status, expire_time, deleted);
```

该索引用于清理过期临时文件。

```sql
CREATE INDEX idx_attachment_tenant_resource_type
    ON biz_attachment (tenant_id, resource_type, deleted, create_time, id);
```

该索引用于按资源类型筛选附件，例如只查询图片、视频、文档等。

关系表索引用于支持业务对象和附件之间的双向查询。

```sql
ALTER TABLE biz_object_attachment_rel
    ADD PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_rel_tenant_object_attachment_biz_deleted
    ON biz_object_attachment_rel (tenant_id, object_type, object_id, attachment_id, attachment_biz_type, deleted);
```

该唯一索引用于避免同一个业务对象在同一附件业务类型下重复绑定同一个附件。

```sql
CREATE INDEX idx_rel_tenant_object_biz_sort
    ON biz_object_attachment_rel (tenant_id, object_type, object_id, attachment_biz_type, deleted, main_flag, sort_no, id);
```

该索引用于查询某个业务对象下某类附件，并按主附件和排序号展示。

```sql
CREATE INDEX idx_rel_tenant_attachment
    ON biz_object_attachment_rel (tenant_id, attachment_id, deleted, object_type, object_id);
```

该索引用于查询某个附件被哪些业务对象引用。

```sql
CREATE INDEX idx_rel_tenant_object
    ON biz_object_attachment_rel (tenant_id, object_type, object_id, deleted, sort_no, id);
```

该索引用于查询某个业务对象下的全部附件。

如果启用附件操作日志表，需要建立日志查询索引。

```sql
ALTER TABLE biz_attachment_log
    ADD PRIMARY KEY (id);
CREATE INDEX idx_attachment_log_tenant_attachment_time
    ON biz_attachment_log (tenant_id, attachment_id, create_time);
```

该索引用于查询某个附件的操作记录。

```sql
CREATE INDEX idx_attachment_log_tenant_operator_time
    ON biz_attachment_log (tenant_id, create_by, create_time);
```

该索引用于按操作人查询上传、下载、预览记录。

索引设计注意事项：

- 多租户系统中，附件表和关系表核心索引都应以 `tenant_id` 开头。
- 高频对象附件列表查询应优先使用 `tenant_id + object_type + object_id + attachment_biz_type`。
- 文件哈希去重查询应同时带上 `file_hash` 和 `file_size`。
- `object_key` 较长时可以使用前缀索引，但需要确保业务查询前缀具有足够区分度。
- 私有文件下载通常先按 `attachment_id` 查元数据，再校验业务权限，不应只依赖 URL。
- 操作日志表数据增长较快，应按时间归档或分区。

### 常用查询

常用查询应覆盖附件详情、对象附件列表、主附件查询、附件引用查询、临时文件清理、哈希去重和附件日志查询等场景。

#### 查询附件详情

附件详情查询用于下载、预览、权限校验和文件迁移。

```sql
SELECT
    id,
    tenant_id,
    file_name,
    file_ext,
    content_type,
    file_size,
    file_hash,
    storage_type,
    bucket_name,
    object_key,
    access_url,
    access_scope,
    resource_type,
    file_status,
    ref_count,
    expire_time,
    create_by,
    create_time
FROM biz_attachment
WHERE tenant_id = :tenantId
  AND id = :attachmentId
  AND deleted = 0;
```

该查询只返回附件元数据。私有文件需要在业务服务层完成权限校验后，再生成临时下载地址或签名 URL。

#### 查询对象附件列表

对象附件列表用于详情页、编辑页和附件管理区域。

```sql
SELECT
    rel.id AS rel_id,
    rel.object_type,
    rel.object_id,
    rel.attachment_biz_type,
    rel.display_name,
    rel.sort_no,
    rel.main_flag,
    attachment.id AS attachment_id,
    attachment.file_name,
    attachment.file_ext,
    attachment.content_type,
    attachment.file_size,
    attachment.storage_type,
    attachment.bucket_name,
    attachment.object_key,
    attachment.access_url,
    attachment.access_scope,
    attachment.resource_type,
    attachment.file_status,
    attachment.create_time
FROM biz_object_attachment_rel rel
INNER JOIN biz_attachment attachment
    ON attachment.id = rel.attachment_id
   AND attachment.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.object_id = :objectId
  AND rel.deleted = 0
  AND attachment.deleted = 0
  AND attachment.file_status = 'active'
ORDER BY rel.main_flag DESC, rel.sort_no ASC, rel.id ASC;
```

该查询适合一次性查询某个业务对象下的全部有效附件。

#### 查询指定业务类型附件

同一个业务对象可能存在不同用途的附件，例如封面图、详情图、合同附件、发票附件。

```sql
SELECT
    rel.id AS rel_id,
    rel.attachment_biz_type,
    rel.display_name,
    rel.sort_no,
    rel.main_flag,
    attachment.id AS attachment_id,
    attachment.file_name,
    attachment.file_ext,
    attachment.content_type,
    attachment.file_size,
    attachment.access_url,
    attachment.resource_type
FROM biz_object_attachment_rel rel
INNER JOIN biz_attachment attachment
    ON attachment.id = rel.attachment_id
   AND attachment.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.object_id = :objectId
  AND rel.attachment_biz_type = :attachmentBizType
  AND rel.deleted = 0
  AND attachment.deleted = 0
  AND attachment.file_status = 'active'
ORDER BY rel.main_flag DESC, rel.sort_no ASC, rel.id ASC;
```

该查询适合业务详情页分别加载不同附件区域。

#### 查询主附件

主附件用于商品主图、文章封面、用户头像等场景。

```sql
SELECT
    attachment.id,
    attachment.file_name,
    attachment.content_type,
    attachment.file_size,
    attachment.access_url,
    attachment.storage_type,
    attachment.bucket_name,
    attachment.object_key
FROM biz_object_attachment_rel rel
INNER JOIN biz_attachment attachment
    ON attachment.id = rel.attachment_id
   AND attachment.tenant_id = rel.tenant_id
WHERE rel.tenant_id = :tenantId
  AND rel.object_type = :objectType
  AND rel.object_id = :objectId
  AND rel.attachment_biz_type = :attachmentBizType
  AND rel.main_flag = 1
  AND rel.deleted = 0
  AND attachment.deleted = 0
  AND attachment.file_status = 'active'
ORDER BY rel.sort_no ASC, rel.id ASC
LIMIT 1;
```

如果主附件查询非常高频，可以在业务主表中冗余 `cover_attachment_id` 或 `avatar_attachment_id`，但附件真实关系仍建议保留在关系表中。

#### 查询附件被哪些对象引用

该查询用于资源审计、文件删除前校验和引用分析。

```sql
SELECT
    rel.object_type,
    rel.object_id,
    rel.attachment_biz_type,
    rel.main_flag,
    rel.create_by,
    rel.create_time
FROM biz_object_attachment_rel rel
WHERE rel.tenant_id = :tenantId
  AND rel.attachment_id = :attachmentId
  AND rel.deleted = 0
ORDER BY rel.create_time DESC, rel.id DESC;
```

删除附件前应先查询是否存在有效引用。如果仍有业务对象引用，不应直接删除附件资源。

#### 查询未绑定的临时文件

临时文件清理任务需要查询过期且没有被正式引用的文件。

```sql
SELECT
    id,
    file_name,
    storage_type,
    bucket_name,
    object_key,
    expire_time,
    ref_count
FROM biz_attachment
WHERE tenant_id = :tenantId
  AND file_status = 'temporary'
  AND deleted = 0
  AND expire_time IS NOT NULL
  AND expire_time < CURRENT_TIMESTAMP
  AND ref_count = 0
ORDER BY expire_time ASC
LIMIT :limitSize;
```

该查询适合定时清理任务。清理时应先删除存储系统中的物理文件，再逻辑删除数据库记录，或使用补偿任务处理物理删除失败。

#### 根据文件哈希查询重复文件

文件秒传或重复上传识别可以通过文件哈希和文件大小判断。

```sql
SELECT
    id,
    file_name,
    file_ext,
    content_type,
    file_size,
    file_hash,
    storage_type,
    bucket_name,
    object_key,
    access_url,
    file_status
FROM biz_attachment
WHERE tenant_id = :tenantId
  AND file_hash = :fileHash
  AND file_size = :fileSize
  AND deleted = 0
  AND file_status IN ('temporary', 'active')
ORDER BY create_time DESC
LIMIT 1;
```

如果查询到可复用文件，业务可以直接增加引用关系，避免重复上传物理文件。

#### 按资源类型查询附件

资源管理后台通常需要按图片、视频、文档等类型筛选附件。

```sql
SELECT
    id,
    file_name,
    file_ext,
    content_type,
    file_size,
    resource_type,
    file_status,
    ref_count,
    create_by,
    create_time
FROM biz_attachment
WHERE tenant_id = :tenantId
  AND resource_type = :resourceType
  AND deleted = 0
ORDER BY create_time DESC, id DESC
LIMIT :offset, :pageSize;
```

该查询适合附件资源管理页面。若需要按文件名搜索，可增加 `file_name` 查询条件。

#### 查询附件操作日志

附件下载、预览、删除等敏感操作建议记录日志，便于审计追踪。

```sql
SELECT
    id,
    attachment_id,
    object_type,
    object_id,
    operation_type,
    operation_result,
    client_ip,
    error_message,
    create_by,
    create_time
FROM biz_attachment_log
WHERE tenant_id = :tenantId
  AND attachment_id = :attachmentId
ORDER BY create_time DESC, id DESC
LIMIT :offset, :pageSize;
```

操作日志表一般只追加写入，不建议频繁更新。数据量较大时应按时间归档。

### 常用写入

附件资源模型的写入操作主要包括上传元数据、绑定业务对象、重置附件、解绑附件、设置主附件、删除附件和清理临时文件。涉及附件关系和引用次数的操作建议放在事务中执行。

#### 上传临时附件

文件上传成功后，先写入附件元数据，状态为 `temporary`。等业务表单提交成功后，再转为 `active` 并绑定业务对象。

```sql
INSERT INTO biz_attachment (
    id,
    tenant_id,
    file_name,
    file_ext,
    content_type,
    file_size,
    file_hash,
    storage_type,
    bucket_name,
    object_key,
    access_url,
    access_scope,
    resource_type,
    file_status,
    ref_count,
    expire_time,
    create_by,
    update_by
) VALUES (
    :id,
    :tenantId,
    :fileName,
    :fileExt,
    :contentType,
    :fileSize,
    :fileHash,
    :storageType,
    :bucketName,
    :objectKey,
    :accessUrl,
    :accessScope,
    :resourceType,
    'temporary',
    0,
    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 DAY),
    :operator,
    :operator
);
```

临时附件适合“先上传文件，再提交业务表单”的交互模式。未提交表单的临时文件可以由定时任务清理。

#### 绑定附件到业务对象

业务表单提交成功后，将附件绑定到业务对象，并把附件状态改为 `active`。

```sql
START TRANSACTION;

INSERT INTO biz_object_attachment_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    attachment_id,
    attachment_biz_type,
    display_name,
    sort_no,
    main_flag,
    create_by,
    update_by
) VALUES (
    :relId,
    :tenantId,
    :objectType,
    :objectId,
    :attachmentId,
    :attachmentBizType,
    :displayName,
    :sortNo,
    :mainFlag,
    :operator,
    :operator
);

UPDATE biz_attachment
SET file_status = 'active',
    ref_count = ref_count + 1,
    expire_time = NULL,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :attachmentId
  AND deleted = 0;

COMMIT;
```

绑定前应校验附件属于当前租户，且附件状态是 `temporary` 或 `active`。

#### 批量绑定附件

一个业务对象通常会一次性提交多个附件，例如工单截图、审批材料、商品详情图。

```sql
START TRANSACTION;

INSERT INTO biz_object_attachment_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    attachment_id,
    attachment_biz_type,
    display_name,
    sort_no,
    main_flag,
    create_by,
    update_by
) VALUES
    (:relId1, :tenantId, :objectType, :objectId, :attachmentId1, :attachmentBizType, :displayName1, 1, 1, :operator, :operator),
    (:relId2, :tenantId, :objectType, :objectId, :attachmentId2, :attachmentBizType, :displayName2, 2, 0, :operator, :operator),
    (:relId3, :tenantId, :objectType, :objectId, :attachmentId3, :attachmentBizType, :displayName3, 3, 0, :operator, :operator);

UPDATE biz_attachment
SET file_status = 'active',
    ref_count = ref_count + 1,
    expire_time = NULL,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id IN (:attachmentId1, :attachmentId2, :attachmentId3)
  AND deleted = 0;

COMMIT;
```

批量绑定前应校验附件 ID 列表去重，避免同一对象重复绑定同一附件。

#### 重置对象附件

对象编辑页面常见做法是先删除旧关系，再插入新的附件关系。该操作适合表单整体保存。

```sql
START TRANSACTION;

UPDATE biz_object_attachment_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND attachment_biz_type = :attachmentBizType
  AND deleted = 0;

INSERT INTO biz_object_attachment_rel (
    id,
    tenant_id,
    object_type,
    object_id,
    attachment_id,
    attachment_biz_type,
    display_name,
    sort_no,
    main_flag,
    create_by,
    update_by
) VALUES
    (:relId1, :tenantId, :objectType, :objectId, :attachmentId1, :attachmentBizType, :displayName1, 1, 1, :operator, :operator),
    (:relId2, :tenantId, :objectType, :objectId, :attachmentId2, :attachmentBizType, :displayName2, 2, 0, :operator, :operator);

COMMIT;
```

这种写法实现简单，但会影响 `ref_count` 的准确性。对引用次数要求严格的系统，应分别计算被新增和被移除的附件，再增减 `ref_count`。

#### 设置主附件

设置主附件时，需要保证同一业务对象、同一附件业务类型下只有一个主附件。

```sql
START TRANSACTION;

UPDATE biz_object_attachment_rel
SET main_flag = 0,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND attachment_biz_type = :attachmentBizType
  AND deleted = 0;

UPDATE biz_object_attachment_rel
SET main_flag = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND attachment_biz_type = :attachmentBizType
  AND attachment_id = :attachmentId
  AND deleted = 0;

COMMIT;
```

主附件适合封面图、商品主图、默认合同等业务。高并发场景下建议在服务层加锁或使用业务版本号避免并发覆盖。

#### 调整附件排序

附件排序通常由前端拖拽产生，后端按关系记录更新 `sort_no`。

```sql
UPDATE biz_object_attachment_rel
SET sort_no = :sortNo,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND attachment_id = :attachmentId
  AND attachment_biz_type = :attachmentBizType
  AND deleted = 0;
```

如果一次调整多个附件排序，建议批量更新并放在事务中执行。

#### 解绑附件

解绑附件只删除业务关系，不一定删除附件资源本身。附件可能被其他业务对象继续引用。

```sql
START TRANSACTION;

UPDATE biz_object_attachment_rel
SET deleted = 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND object_type = :objectType
  AND object_id = :objectId
  AND attachment_id = :attachmentId
  AND attachment_biz_type = :attachmentBizType
  AND deleted = 0;

UPDATE biz_attachment
SET ref_count = CASE
        WHEN ref_count > 0 THEN ref_count - 1
        ELSE 0
    END,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :attachmentId
  AND deleted = 0;

COMMIT;
```

解绑后如果 `ref_count = 0`，可以将附件转为待清理状态，也可以保留一段时间后由任务清理。

#### 删除附件资源

删除附件资源前必须确认没有有效业务引用。仍被引用的附件不应直接删除。

```sql
SELECT
    COUNT(1) AS ref_count
FROM biz_object_attachment_rel
WHERE tenant_id = :tenantId
  AND attachment_id = :attachmentId
  AND deleted = 0;
```

没有引用时，可以逻辑删除附件记录。

```sql
UPDATE biz_attachment
SET file_status = 'deleted',
    deleted = 1,
    version = version + 1,
    update_by = :operator,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND id = :attachmentId
  AND ref_count = 0
  AND deleted = 0;
```

物理文件删除建议由异步任务执行。数据库先标记删除，任务再删除对象存储中的文件，并记录删除结果。

#### 清理过期临时附件

临时附件清理通常由定时任务执行，先查询过期文件，再删除物理文件，最后逻辑删除数据库记录。

```sql
UPDATE biz_attachment
SET file_status = 'deleted',
    deleted = 1,
    update_by = 'system',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = :tenantId
  AND file_status = 'temporary'
  AND ref_count = 0
  AND expire_time IS NOT NULL
  AND expire_time < CURRENT_TIMESTAMP
  AND deleted = 0
LIMIT :limitSize;
```

如果使用 MySQL 执行清理标记，物理文件删除应由应用服务读取待删除记录后执行，避免数据库和对象存储状态不一致。

### 常见问题

附件资源模型常见问题集中在文件存储分散、权限控制不足、临时文件未清理、引用次数不准确和物理文件删除不一致等方面。

| 问题                   | 原因                             | 建议                                         |
| ---------------------- | -------------------------------- | -------------------------------------------- |
| 文件地址散落在业务表中 | 各业务模块独立保存 URL           | 统一使用附件资源表和关系表管理               |
| 私有文件被直接访问     | 保存了永久公开 URL               | 私有文件通过后端鉴权后生成临时签名 URL       |
| 文件名冲突             | 使用原始文件名作为存储路径       | 使用系统生成的 `object_key`                  |
| 路径穿越风险           | 直接信任前端上传文件名           | 文件名只做展示，存储路径由后端生成           |
| 临时文件越来越多       | 上传后未提交表单，也没有清理任务 | 使用 `file_status` 和 `expire_time` 定时清理 |
| 附件被误删             | 删除前没有检查引用关系           | 删除前查询关系表，仍有引用时禁止删除         |
| 引用次数不准确         | 绑定和解绑时没有同步维护         | 使用事务维护，或定时重算 `ref_count`         |
| 同一个对象重复绑定附件 | 关系表缺少唯一约束               | 建立对象、附件、业务类型唯一索引             |
| 主附件出现多个         | 没有控制 `main_flag` 唯一语义    | 设置主附件时先清空再设置，必要时服务层加锁   |
| 文件类型校验不可靠     | 只相信扩展名或前端 MIME          | 后端同时校验扩展名、MIME 和文件头            |
| 大文件上传失败率高     | 一次性上传，没有分片机制         | 大文件使用分片上传、断点续传和合并校验       |
| 物理文件和数据库不一致 | 数据库写入成功但对象存储删除失败 | 使用异步补偿任务和操作日志                   |
| 下载审计缺失           | 下载接口没有记录日志             | 对敏感文件记录下载、预览、删除日志           |
| 列表页加载慢           | 每条业务数据实时查附件           | 高频列表可在读模型中冗余封面附件 ID 或 URL   |

设计附件资源模型时应提前明确以下规则：

- 文件是公开访问还是私有访问。
- 文件是否允许复用。
- 是否需要文件秒传和哈希去重。
- 临时文件保留多久。
- 是否允许删除已经被业务引用的附件。
- 是否需要记录下载和预览日志。
- 是否需要图片压缩、视频转码、文档预览。
- 是否需要大文件分片上传。
- 是否需要多存储供应商迁移。
- 是否需要对附件做病毒扫描或内容安全检测。

这些规则应在数据库设计、上传服务、下载服务、权限校验、定时清理和运维监控中保持一致。

### 总结

附件资源模型的核心是把文件元数据和业务挂载关系拆开。附件资源表管理文件本身，对象附件关系表管理文件被哪些业务对象使用。

该模型适合大多数业务系统中的文件上传、附件管理、图片展示、资源复用和私有下载场景。相比直接在业务表中保存文件 URL，统一附件模型更利于权限控制、资源清理、存储迁移、引用统计和操作审计。

设计附件资源模型时需要重点关注四点：

- 附件表保存文件元数据、存储位置、访问范围、文件状态和生命周期信息。
- 关系表通过 `object_type + object_id + attachment_id` 表达业务对象与附件之间的绑定关系。
- 私有文件不要直接暴露永久地址，应通过后端鉴权和临时签名 URL 访问。
- 临时文件、无引用文件和物理文件删除需要通过状态字段、引用次数和定时任务协同管理。

附件资源模型通常会与分类模型、标签模型和权限模型组合使用。分类负责资源归类，标签负责资源特征标记，权限模型负责资源访问控制，附件模型负责资源元数据和业务挂载关系。