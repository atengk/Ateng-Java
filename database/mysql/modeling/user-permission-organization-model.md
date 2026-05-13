# 用户、权限与组织模型

## 用户-角色-权限模型

用户-角色-权限模型用于描述系统中“用户拥有什么角色、角色拥有什么权限”的授权关系。该模型是后台管理系统、SaaS 平台、企业内部系统、开放平台控制台中最常见的权限基础模型，通常也称为 RBAC 模型。

### 适用场景

用户-角色-权限模型适合权限规则相对稳定、权限可以按岗位或职责归类、用户权限主要通过角色间接获得的业务系统。它可以避免直接给用户分配大量权限，使权限维护更加清晰。

常见适用场景如下：

| 场景           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 后台管理系统   | 管理员、运营、财务、客服等角色拥有不同菜单和按钮权限 |
| 企业内部系统   | 员工通过岗位或职责分配角色，再通过角色获得系统权限   |
| SaaS 平台      | 租户管理员为租户内用户分配角色，实现租户内权限隔离   |
| 审批系统       | 不同审批角色拥有不同单据查看、审批、驳回、转交权限   |
| 开放平台控制台 | 开发者、管理员、审计员拥有不同资源操作权限           |

该模型不适合直接表达复杂的数据范围规则，例如“只能查看本部门及下级部门数据”“只能查看自己创建的数据”。这类规则通常应由数据权限模型补充实现。

### 建模结构

用户-角色-权限模型的核心结构由用户表、角色表、权限表、用户角色关系表、角色权限关系表组成。用户不直接绑定权限，而是通过角色间接获得权限。

```text
sys_user
   |
   | 1:N
   |
sys_user_role
   |
   | N:1
   |
sys_role
   |
   | 1:N
   |
sys_role_permission
   |
   | N:1
   |
sys_permission
```

核心表说明如下：

| 表名                  | 说明                                           |
| --------------------- | ---------------------------------------------- |
| `sys_user`            | 用户表，保存系统登录用户和基础身份信息         |
| `sys_role`            | 角色表，保存系统角色，例如管理员、财务、客服   |
| `sys_permission`      | 权限表，保存菜单、按钮、接口、数据操作等权限点 |
| `sys_user_role`       | 用户角色关系表，表示用户拥有哪些角色           |
| `sys_role_permission` | 角色权限关系表，表示角色拥有哪些权限           |

推荐将权限分为菜单权限、按钮权限、接口权限三类。菜单权限用于控制页面可见性，按钮权限用于控制页面操作入口，接口权限用于后端接口鉴权。实际项目中也可以只维护菜单和按钮权限，再通过后端注解或权限标识完成接口控制。

### 字段设计

字段设计需要兼顾登录认证、权限授权、审计追踪和后续扩展。业务表建议统一保留状态、排序、备注、创建时间、更新时间、创建人、更新人、逻辑删除字段。

用户表保存登录账号和用户基础信息。

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码，加密存储',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：1普通用户，2管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
```

角色表保存系统内可分配的角色信息。角色编码应具有业务可读性，适合作为程序中的权限判断标识。

```sql
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码，例如 admin、finance、operator',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_type TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型：1系统角色，2业务角色',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
```

权限表保存系统权限点。权限可以按树形结构组织，既可以表示菜单，也可以表示按钮和接口。

```sql
CREATE TABLE sys_permission (
    id BIGINT NOT NULL COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级节点',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码，例如 user:add、user:delete',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    permission_type TINYINT NOT NULL COMMENT '权限类型：1目录，2菜单，3按钮，4接口',
    path VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    component VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    api_method VARCHAR(16) DEFAULT NULL COMMENT '接口请求方法，例如 GET、POST、PUT、DELETE',
    api_path VARCHAR(255) DEFAULT NULL COMMENT '接口路径',
    icon VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：0隐藏，1显示',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
```

用户角色关系表用于维护用户与角色的多对多关系。该表不保存角色名称等冗余信息，避免角色信息变更后出现数据不一致。

```sql
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系表';
```

角色权限关系表用于维护角色与权限的多对多关系。角色授权通常通过重建该表中的角色权限关系来完成。

```sql
CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关系表';
```

字段设计建议如下：

| 字段              | 设计建议                                                     |
| ----------------- | ------------------------------------------------------------ |
| `id`              | 建议使用雪花 ID 或数据库外部生成的全局 ID，避免分库分表后主键冲突 |
| `username`        | 登录账号应全局唯一，通常不建议允许频繁修改                   |
| `password`        | 必须加密存储，不允许保存明文密码                             |
| `role_code`       | 角色编码适合做程序判断，应保持稳定                           |
| `permission_code` | 权限编码是后端鉴权和前端按钮控制的核心标识                   |
| `permission_type` | 用于区分目录、菜单、按钮、接口等不同权限点                   |
| `status`          | 用于禁用用户、角色或权限，不建议直接物理删除                 |
| `deleted`         | 用于逻辑删除，便于保留审计和历史关系                         |

### 索引设计

索引设计需要围绕登录、角色分配、权限加载、唯一性约束和关系去重展开。关系表必须避免同一个用户重复绑定同一个角色、同一个角色重复绑定同一个权限。

```sql
ALTER TABLE sys_user
    ADD UNIQUE KEY uk_sys_user_username (username, deleted),
    ADD KEY idx_sys_user_mobile (mobile),
    ADD KEY idx_sys_user_status (status, deleted),
    ADD KEY idx_sys_user_create_time (create_time);

ALTER TABLE sys_role
    ADD UNIQUE KEY uk_sys_role_code (role_code, deleted),
    ADD KEY idx_sys_role_status_sort (status, sort_no);

ALTER TABLE sys_permission
    ADD UNIQUE KEY uk_sys_permission_code (permission_code, deleted),
    ADD KEY idx_sys_permission_parent (parent_id, deleted),
    ADD KEY idx_sys_permission_type_status (permission_type, status, deleted),
    ADD KEY idx_sys_permission_sort (sort_no);

ALTER TABLE sys_user_role
    ADD UNIQUE KEY uk_sys_user_role (user_id, role_id, deleted),
    ADD KEY idx_sys_user_role_role (role_id, deleted),
    ADD KEY idx_sys_user_role_user (user_id, deleted);

ALTER TABLE sys_role_permission
    ADD UNIQUE KEY uk_sys_role_permission (role_id, permission_id, deleted),
    ADD KEY idx_sys_role_permission_permission (permission_id, deleted),
    ADD KEY idx_sys_role_permission_role (role_id, deleted);
```

索引说明如下：

| 索引                           | 说明                         |
| ------------------------------ | ---------------------------- |
| `uk_sys_user_username`         | 保证未删除用户的登录账号唯一 |
| `uk_sys_role_code`             | 保证未删除角色编码唯一       |
| `uk_sys_permission_code`       | 保证未删除权限编码唯一       |
| `uk_sys_user_role`             | 防止同一用户重复绑定同一角色 |
| `uk_sys_role_permission`       | 防止同一角色重复绑定同一权限 |
| `idx_sys_user_role_user`       | 加速根据用户查询角色         |
| `idx_sys_user_role_role`       | 加速根据角色反查用户         |
| `idx_sys_role_permission_role` | 加速根据角色查询权限         |
| `idx_sys_permission_parent`    | 加速菜单树和权限树查询       |

如果系统不允许逻辑删除后复用账号、角色编码或权限编码，可以将唯一索引改为单字段唯一索引，例如 `UNIQUE KEY uk_sys_user_username (username)`。

### 常用查询

常用查询应围绕登录认证、角色加载、权限加载、菜单树加载和权限校验展开。实际业务中，登录后通常会一次性加载用户角色和权限编码，并缓存到 Redis 或会话上下文中，避免每次接口请求都查询数据库。

#### 查询用户基础信息

该查询用于登录、用户详情页和用户管理列表。登录场景通常通过账号查询单个启用用户。

```sql
SELECT
    id,
    username,
    password,
    nickname,
    mobile,
    email,
    avatar_url,
    user_type,
    status,
    last_login_time
FROM sys_user
WHERE username = 'admin'
  AND status = 1
  AND deleted = 0
LIMIT 1;
```

#### 查询用户拥有的角色

该查询用于登录后加载当前用户的角色集合，也可用于用户详情页展示已分配角色。

```sql
SELECT
    r.id,
    r.role_code,
    r.role_name,
    r.role_type
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
WHERE ur.user_id = 10001
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
ORDER BY r.sort_no ASC, r.id ASC;
```

#### 查询用户拥有的权限编码

该查询用于后端接口鉴权和前端按钮权限控制。使用 `DISTINCT` 可以避免用户拥有多个角色时返回重复权限编码。

```sql
SELECT DISTINCT
    p.permission_code
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_permission rp ON rp.role_id = r.id
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = 10001
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rp.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
ORDER BY p.permission_code ASC;
```

#### 查询用户可见菜单

该查询用于前端加载当前用户菜单。一般只查询目录和菜单，不查询按钮与接口权限。

```sql
SELECT DISTINCT
    p.id,
    p.parent_id,
    p.permission_code,
    p.permission_name,
    p.permission_type,
    p.path,
    p.component,
    p.icon,
    p.visible,
    p.sort_no
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_permission rp ON rp.role_id = r.id
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = 10001
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rp.deleted = 0
  AND p.permission_type IN (1, 2)
  AND p.visible = 1
  AND p.status = 1
  AND p.deleted = 0
ORDER BY p.sort_no ASC, p.id ASC;
```

#### 查询角色已分配权限

该查询用于角色授权页面回显权限树中已勾选的权限节点。

```sql
SELECT
    p.id,
    p.parent_id,
    p.permission_code,
    p.permission_name,
    p.permission_type
FROM sys_role_permission rp
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE rp.role_id = 20001
  AND rp.deleted = 0
  AND p.deleted = 0
ORDER BY p.sort_no ASC, p.id ASC;
```

#### 查询权限树节点

该查询用于权限管理页面加载权限树。MySQL 8 可以使用递归 CTE 查询某个节点下的所有子权限。

```sql
WITH RECURSIVE permission_tree AS (
    SELECT
        id,
        parent_id,
        permission_code,
        permission_name,
        permission_type,
        sort_no,
        1 AS level_no
    FROM sys_permission
    WHERE parent_id = 0
      AND deleted = 0

    UNION ALL

    SELECT
        p.id,
        p.parent_id,
        p.permission_code,
        p.permission_name,
        p.permission_type,
        p.sort_no,
        pt.level_no + 1 AS level_no
    FROM sys_permission p
    INNER JOIN permission_tree pt ON p.parent_id = pt.id
    WHERE p.deleted = 0
)
SELECT
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    sort_no,
    level_no
FROM permission_tree
ORDER BY level_no ASC, sort_no ASC, id ASC;
```

#### 判断用户是否拥有指定权限

该查询用于低频鉴权校验。高并发接口不建议每次请求都实时查询数据库，应优先使用缓存中的权限编码集合。

```sql
SELECT
    COUNT(1) AS permission_count
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_permission rp ON rp.role_id = r.id
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = 10001
  AND p.permission_code = 'user:add'
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rp.deleted = 0
  AND p.status = 1
  AND p.deleted = 0;
```

### 常用写入

常用写入主要包括创建用户、创建角色、创建权限、分配用户角色、分配角色权限和禁用角色。涉及关系表重建时，建议在事务中完成删除旧关系和插入新关系，避免中间状态造成权限异常。

#### 创建用户

创建用户时必须先完成密码加密，再写入用户表。用户名唯一性依赖唯一索引兜底，业务层应先做友好校验。

```sql
INSERT INTO sys_user (
    id,
    username,
    password,
    nickname,
    mobile,
    email,
    user_type,
    status,
    create_by,
    update_by
) VALUES (
    10001,
    'admin',
    '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    '系统管理员',
    '13800000000',
    'admin@example.com',
    2,
    1,
    1,
    1
);
```

#### 创建角色

创建角色时应保证角色编码稳定。角色编码一旦被程序、接口或前端权限逻辑引用，不建议随意修改。

```sql
INSERT INTO sys_role (
    id,
    role_code,
    role_name,
    role_type,
    status,
    sort_no,
    remark,
    create_by,
    update_by
) VALUES (
    20001,
    'admin',
    '系统管理员',
    1,
    1,
    1,
    '系统内置管理员角色',
    1,
    1
);
```

#### 创建权限

创建权限时需要明确权限类型。菜单权限通常维护 `path` 和 `component`，按钮权限通常只维护 `permission_code`，接口权限可以维护 `api_method` 和 `api_path`。

```sql
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    path,
    component,
    api_method,
    api_path,
    visible,
    status,
    sort_no,
    create_by,
    update_by
) VALUES (
    30001,
    0,
    'user:manage',
    '用户管理',
    2,
    '/system/user',
    'system/user/index',
    NULL,
    NULL,
    1,
    1,
    10,
    1,
    1
);
```

#### 分配用户角色

分配用户角色时，建议先逻辑删除用户原有角色关系，再批量插入新的角色关系。该操作必须放在同一个事务中。

```sql
START TRANSACTION;

UPDATE sys_user_role
SET deleted = 1
WHERE user_id = 10001
  AND deleted = 0;

INSERT INTO sys_user_role (
    id,
    user_id,
    role_id,
    create_by
) VALUES
    (40001, 10001, 20001, 1),
    (40002, 10001, 20002, 1);

COMMIT;
```

#### 分配角色权限

分配角色权限时，建议重建角色权限关系。授权完成后，应清理受影响用户的权限缓存，确保新权限及时生效。

```sql
START TRANSACTION;

UPDATE sys_role_permission
SET deleted = 1
WHERE role_id = 20001
  AND deleted = 0;

INSERT INTO sys_role_permission (
    id,
    role_id,
    permission_id,
    create_by
) VALUES
    (50001, 20001, 30001, 1),
    (50002, 20001, 30002, 1),
    (50003, 20001, 30003, 1);

COMMIT;
```

#### 禁用角色

禁用角色不会删除角色关系，但该角色在权限查询中不再生效。该方式适合临时回收一类用户的权限。

```sql
UPDATE sys_role
SET status = 0,
    update_by = 1
WHERE id = 20001
  AND deleted = 0;
```

### 常见问题

用户-角色-权限模型虽然结构清晰，但在实际落地时容易出现权限粒度混乱、缓存不一致、关系表重复写入和逻辑删除唯一约束失效等问题。

| 问题                         | 原因                           | 建议                                                   |
| ---------------------------- | ------------------------------ | ------------------------------------------------------ |
| 用户权限查询结果重复         | 用户多个角色拥有同一权限       | 查询权限编码时使用 `DISTINCT`，业务缓存中使用 Set 结构 |
| 修改角色权限后用户权限不生效 | 登录态或 Redis 中缓存了旧权限  | 授权成功后删除受影响用户的权限缓存                     |
| 菜单权限和按钮权限混在一起   | 没有区分 `permission_type`     | 使用权限类型区分目录、菜单、按钮、接口                 |
| 角色编码被随意修改           | 程序中依赖角色编码判断         | 角色编码应作为稳定业务标识，不建议频繁修改             |
| 关系表出现重复数据           | 缺少唯一约束或批量写入未去重   | 对关系表增加联合唯一索引，写入前做去重                 |
| 删除角色后历史关系丢失       | 直接物理删除角色和关系         | 优先使用状态禁用或逻辑删除                             |
| 接口鉴权性能差               | 每次请求实时关联多张表         | 登录后加载权限集合并缓存，接口请求使用缓存鉴权         |
| 超级管理员权限难维护         | 给超级管理员绑定所有权限成本高 | 超级管理员可通过特殊角色编码绕过细粒度权限校验         |

权限缓存需要特别注意一致性。常见做法是在用户登录后缓存角色编码和权限编码；当用户角色、角色权限、权限状态发生变化时，主动清理相关用户缓存。对于后台系统，可以接受短时间缓存不一致时，也可以设置较短的缓存过期时间作为兜底。

### 总结

用户-角色-权限模型的核心思想是通过角色解耦用户和权限，用户只需要绑定角色，角色再绑定权限。这样可以降低授权维护成本，并让权限体系具备较好的扩展性。

落地时应重点关注以下原则：

| 原则                   | 说明                                         |
| ---------------------- | -------------------------------------------- |
| 用户不直接绑定权限     | 通过角色统一承载权限，减少用户级别授权复杂度 |
| 权限编码保持稳定       | `permission_code` 是前后端权限判断的核心标识 |
| 关系表必须防重         | 用户角色、角色权限关系表都需要联合唯一约束   |
| 查询区分状态和逻辑删除 | 权限加载时必须过滤禁用和已删除数据           |
| 授权操作使用事务       | 重建用户角色或角色权限时保证数据一致性       |
| 高频鉴权使用缓存       | 避免每次接口请求实时关联多张权限表           |

该模型适合作为权限体系的基础层。对于更复杂的“部门数据范围”“本人数据范围”“租户数据隔离”等需求，应在该模型之上继续扩展数据权限模型。



## 数据权限模型

数据权限模型用于控制用户能够查看、编辑或处理哪些业务数据。它通常建立在用户、角色、权限模型之上，用来解决“用户能不能访问某个功能”之外的“用户能访问哪些数据”的问题。

在实际系统中，功能权限回答的是“是否能进入客户管理页面”，数据权限回答的是“进入客户管理页面后能看到哪些客户数据”。

### 适用场景

数据权限模型适合存在组织边界、岗位边界、负责人边界、租户边界或业务归属关系的系统。它通常用于后台管理系统、CRM、ERP、OA、审批系统、工单系统、订单系统和多部门协作系统。

常见适用场景如下：

| 场景                     | 说明                                             |
| ------------------------ | ------------------------------------------------ |
| 只能查看本人数据         | 业务员只能查看自己创建或负责的客户、订单、工单   |
| 只能查看本部门数据       | 部门主管只能查看本部门员工创建的数据             |
| 查看本部门及下级部门数据 | 区域负责人可以查看本区域及下级区域的数据         |
| 指定部门数据范围         | 某角色只能查看被授权的几个部门数据               |
| 查看全部数据             | 超级管理员、审计员、总部管理员可以查看全部数据   |
| 按业务资源控制数据范围   | 客户、订单、合同、工单等不同模块使用不同数据范围 |
| 多角色权限合并           | 用户拥有多个角色时，数据范围需要合并计算         |

数据权限模型通常不单独使用，而是和角色模型、组织架构模型、岗位模型配合使用。角色决定用户拥有哪些数据范围规则，组织架构决定部门上下级关系，业务表中的 `dept_id`、`owner_user_id`、`create_by` 等字段决定具体过滤条件。

### 建模结构

数据权限模型的核心思想是将“角色”和“业务资源的数据范围”建立关系。用户通过角色获得某个业务资源的数据范围，系统在查询业务数据时根据该范围自动追加过滤条件。

```text
sys_user
   |
   | 1:N
   |
sys_user_role
   |
   | N:1
   |
sys_role
   |
   | 1:N
   |
sys_role_data_scope
   |
   | N:1
   |
sys_data_resource
```

当数据范围是“自定义部门”时，需要额外维护角色数据权限与部门之间的关系。

```text
sys_role_data_scope
   |
   | 1:N
   |
sys_role_data_scope_dept
   |
   | N:1
   |
sys_dept
```

核心表说明如下：

| 表名                       | 说明                                               |
| -------------------------- | -------------------------------------------------- |
| `sys_data_resource`        | 数据资源表，定义哪些业务模块需要做数据权限控制     |
| `sys_role_data_scope`      | 角色数据权限表，定义某个角色在某个资源上的数据范围 |
| `sys_role_data_scope_dept` | 角色自定义部门范围表，保存自定义部门授权关系       |
| `sys_user_role`            | 用户角色关系表，用于计算用户拥有的数据权限         |
| `sys_dept`                 | 部门表，用于计算本部门、下级部门、自定义部门范围   |
| 业务表                     | 被控制的数据表，通常需要包含部门字段和用户字段     |

常用数据范围类型如下：

| 范围类型             | 编码 | 说明                                 |
| -------------------- | ---- | ------------------------------------ |
| 全部数据             | `1`  | 不追加部门或用户过滤条件             |
| 本部门及下级部门数据 | `2`  | 查询当前用户部门及所有下级部门数据   |
| 本部门数据           | `3`  | 只查询当前用户所属部门数据           |
| 自定义部门数据       | `4`  | 查询角色被授权的指定部门数据         |
| 仅本人数据           | `5`  | 只查询当前用户创建、负责或归属的数据 |

业务表需要具备可被过滤的归属字段。常见字段如下：

| 字段            | 说明                                           |
| --------------- | ---------------------------------------------- |
| `dept_id`       | 数据所属部门 ID                                |
| `owner_user_id` | 数据负责人用户 ID                              |
| `create_by`     | 数据创建人用户 ID                              |
| `tenant_id`     | 租户 ID，多租户系统中通常必须参与过滤          |
| `deleted`       | 逻辑删除字段，数据权限查询应同时过滤未删除数据 |

### 字段设计

字段设计需要让数据权限既能支持常见范围，又能按业务模块独立配置。不要把数据权限规则硬编码在用户表或角色表中，否则后续模块扩展会变得困难。

数据资源表用于声明哪些业务模块受数据权限控制。

```sql
CREATE TABLE sys_data_resource (
    id BIGINT NOT NULL COMMENT '主键ID',
    resource_code VARCHAR(128) NOT NULL COMMENT '资源编码，例如 customer、order、contract',
    resource_name VARCHAR(64) NOT NULL COMMENT '资源名称',
    table_name VARCHAR(128) NOT NULL COMMENT '业务表名，仅用于权限元数据描述',
    dept_column VARCHAR(64) DEFAULT 'dept_id' COMMENT '部门字段名',
    user_column VARCHAR(64) DEFAULT 'owner_user_id' COMMENT '用户字段名',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用数据权限：0禁用，1启用',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据资源表';
```

角色数据权限表用于定义角色在指定资源上的数据范围。

```sql
CREATE TABLE sys_role_data_scope (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    resource_id BIGINT NOT NULL COMMENT '数据资源ID',
    scope_type TINYINT NOT NULL COMMENT '数据范围：1全部，2本部门及下级，3本部门，4自定义部门，5仅本人',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色数据权限表';
```

角色自定义部门范围表用于保存 `scope_type = 4` 时的部门授权明细。

```sql
CREATE TABLE sys_role_data_scope_dept (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_data_scope_id BIGINT NOT NULL COMMENT '角色数据权限ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    include_children TINYINT NOT NULL DEFAULT 0 COMMENT '是否包含下级部门：0不包含，1包含',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色数据权限自定义部门表';
```

业务表需要预留数据归属字段。下面以客户表为例，展示可被数据权限过滤的关键字段。

```sql
CREATE TABLE biz_customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID',
    owner_user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
```

字段设计建议如下：

| 字段               | 设计建议                                                     |
| ------------------ | ------------------------------------------------------------ |
| `resource_code`    | 建议与业务模块编码保持一致，例如 `customer`、`order`、`contract` |
| `table_name`       | 只作为元数据描述，不建议直接拼接成动态 SQL                   |
| `dept_column`      | 用于描述业务表中的部门归属字段，常见值为 `dept_id`           |
| `user_column`      | 用于描述业务表中的用户归属字段，常见值为 `owner_user_id` 或 `create_by` |
| `scope_type`       | 数据权限核心字段，业务层根据该字段生成过滤条件               |
| `include_children` | 自定义部门范围是否包含下级部门，适合区域授权场景             |
| `dept_id`          | 业务数据的部门归属字段，是部门数据权限过滤的基础             |
| `owner_user_id`    | 业务数据的负责人字段，是本人数据权限过滤的基础               |

### 索引设计

索引设计需要围绕数据权限规则加载、角色权限匹配、自定义部门查询和业务数据过滤展开。关系表应通过唯一索引避免重复授权。

```sql
ALTER TABLE sys_data_resource
    ADD UNIQUE KEY uk_sys_data_resource_code (resource_code, deleted),
    ADD KEY idx_sys_data_resource_enabled (enabled, status, deleted),
    ADD KEY idx_sys_data_resource_sort (sort_no);

ALTER TABLE sys_role_data_scope
    ADD UNIQUE KEY uk_sys_role_data_scope (role_id, resource_id, deleted),
    ADD KEY idx_sys_role_data_scope_resource (resource_id, enabled, status, deleted),
    ADD KEY idx_sys_role_data_scope_role (role_id, enabled, status, deleted),
    ADD KEY idx_sys_role_data_scope_type (scope_type, deleted);

ALTER TABLE sys_role_data_scope_dept
    ADD UNIQUE KEY uk_sys_role_data_scope_dept (role_data_scope_id, dept_id, deleted),
    ADD KEY idx_sys_role_data_scope_dept_dept (dept_id, deleted),
    ADD KEY idx_sys_role_data_scope_dept_scope (role_data_scope_id, deleted);

ALTER TABLE biz_customer
    ADD KEY idx_biz_customer_dept (dept_id, deleted),
    ADD KEY idx_biz_customer_owner (owner_user_id, deleted),
    ADD KEY idx_biz_customer_create_by (create_by, deleted),
    ADD KEY idx_biz_customer_status_time (status, create_time);
```

索引说明如下：

| 索引                               | 说明                                   |
| ---------------------------------- | -------------------------------------- |
| `uk_sys_data_resource_code`        | 保证未删除数据资源编码唯一             |
| `uk_sys_role_data_scope`           | 防止同一角色对同一资源重复配置数据权限 |
| `idx_sys_role_data_scope_role`     | 加速根据角色查询数据权限               |
| `idx_sys_role_data_scope_resource` | 加速根据业务资源查询数据权限规则       |
| `uk_sys_role_data_scope_dept`      | 防止同一数据权限规则重复绑定同一部门   |
| `idx_biz_customer_dept`            | 加速按部门范围过滤客户数据             |
| `idx_biz_customer_owner`           | 加速按负责人过滤客户数据               |
| `idx_biz_customer_create_by`       | 加速按创建人过滤客户数据               |

业务表的数据权限字段必须建立索引。否则一旦数据量增长，追加 `dept_id IN (...)` 或 `owner_user_id = ?` 条件后容易出现慢查询。

### 常用查询

常用查询主要围绕用户数据权限规则加载、部门范围计算、业务数据过滤和权限合并展开。实际项目中，建议登录后或首次访问业务模块时计算用户的数据范围，并缓存到 Redis，避免每次分页查询都重复关联权限表。

#### 查询数据资源配置

该查询用于根据业务资源编码获取数据权限元数据。业务层可以根据资源配置决定是否启用数据权限，以及使用哪个字段作为部门或用户过滤字段。

```sql
SELECT
    id,
    resource_code,
    resource_name,
    table_name,
    dept_column,
    user_column,
    enabled
FROM sys_data_resource
WHERE resource_code = 'customer'
  AND enabled = 1
  AND status = 1
  AND deleted = 0
LIMIT 1;
```

#### 查询用户在指定资源上的数据权限

该查询用于获取当前用户所有角色在某个业务资源上的数据权限配置。

```sql
SELECT
    r.id AS role_id,
    r.role_code,
    r.role_name,
    dr.resource_code,
    rds.id AS role_data_scope_id,
    rds.scope_type
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_data_scope rds ON rds.role_id = r.id
INNER JOIN sys_data_resource dr ON dr.id = rds.resource_id
WHERE ur.user_id = 10001
  AND dr.resource_code = 'customer'
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rds.enabled = 1
  AND rds.status = 1
  AND rds.deleted = 0
  AND dr.enabled = 1
  AND dr.status = 1
  AND dr.deleted = 0
ORDER BY rds.scope_type ASC;
```

#### 判断用户是否拥有全部数据权限

当用户任意一个角色拥有全部数据权限时，通常可以直接跳过部门和用户过滤条件。

```sql
SELECT
    COUNT(1) AS has_all_scope
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_data_scope rds ON rds.role_id = r.id
INNER JOIN sys_data_resource dr ON dr.id = rds.resource_id
WHERE ur.user_id = 10001
  AND dr.resource_code = 'customer'
  AND rds.scope_type = 1
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rds.enabled = 1
  AND rds.status = 1
  AND rds.deleted = 0
  AND dr.enabled = 1
  AND dr.status = 1
  AND dr.deleted = 0;
```

#### 查询用户自定义部门范围

该查询用于获取用户通过角色被授权的自定义部门。多个角色配置了自定义部门时，需要合并部门集合并去重。

```sql
SELECT DISTINCT
    rdsd.dept_id,
    rdsd.include_children
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
INNER JOIN sys_role_data_scope rds ON rds.role_id = r.id
INNER JOIN sys_data_resource dr ON dr.id = rds.resource_id
INNER JOIN sys_role_data_scope_dept rdsd ON rdsd.role_data_scope_id = rds.id
WHERE ur.user_id = 10001
  AND dr.resource_code = 'customer'
  AND rds.scope_type = 4
  AND ur.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND rds.enabled = 1
  AND rds.status = 1
  AND rds.deleted = 0
  AND rdsd.deleted = 0
  AND dr.enabled = 1
  AND dr.status = 1
  AND dr.deleted = 0;
```

#### 查询当前用户部门及下级部门

该查询依赖部门表的父子结构。MySQL 8 可以使用递归 CTE 查询当前部门及其所有下级部门。

```sql
WITH RECURSIVE dept_tree AS (
    SELECT
        id,
        parent_id,
        dept_name,
        1 AS level_no
    FROM sys_dept
    WHERE id = 10
      AND deleted = 0

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_name,
        dt.level_no + 1 AS level_no
    FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT
    id,
    parent_id,
    dept_name,
    level_no
FROM dept_tree
ORDER BY level_no ASC, id ASC;
```

#### 按本人数据范围查询业务数据

该查询适用于 `scope_type = 5` 的场景。本人数据通常按 `owner_user_id` 或 `create_by` 判断，具体使用哪个字段需要按业务模块确定。

```sql
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE owner_user_id = 10001
  AND deleted = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

#### 按本部门数据范围查询业务数据

该查询适用于 `scope_type = 3` 的场景，只查询当前用户所属部门的数据。

```sql
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE dept_id = 10
  AND deleted = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

#### 按部门集合查询业务数据

该查询适用于本部门及下级部门、自定义部门等场景。业务层应先计算可访问部门 ID 集合，再传入业务查询。

```sql
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE dept_id IN (10, 11, 12, 13)
  AND deleted = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

#### 多角色数据权限合并查询

当用户拥有多个角色时，建议在业务层先合并数据范围，再生成最终查询条件。下面示例展示一种常见合并结果：用户可查看部分部门数据，同时也可查看本人负责的数据。

```sql
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE deleted = 0
  AND (
      dept_id IN (10, 11, 12, 13)
      OR owner_user_id = 10001
  )
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

该查询中的 `OR` 条件可能影响索引利用。如果数据量较大，可以拆成两个查询后使用 `UNION` 合并。

```sql
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE dept_id IN (10, 11, 12, 13)
  AND deleted = 0

UNION

SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE owner_user_id = 10001
  AND deleted = 0

ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

### 常用写入

常用写入主要包括创建数据资源、配置角色数据权限、配置自定义部门范围、禁用数据资源和调整业务数据归属。数据权限配置通常影响较大，建议所有授权写入都放在事务中完成，并在提交后清理相关用户缓存。

#### 创建数据资源

创建数据资源时，需要明确业务资源编码、业务表名、部门字段和用户字段。资源编码应保持稳定，避免后续权限配置失效。

```sql
INSERT INTO sys_data_resource (
    id,
    resource_code,
    resource_name,
    table_name,
    dept_column,
    user_column,
    enabled,
    status,
    sort_no,
    remark,
    create_by,
    update_by
) VALUES (
    60001,
    'customer',
    '客户数据',
    'biz_customer',
    'dept_id',
    'owner_user_id',
    1,
    1,
    1,
    '客户模块数据权限资源',
    1,
    1
);
```

#### 配置角色数据权限

配置角色数据权限时，建议一个角色在一个资源上只保留一条有效配置。该约束由 `uk_sys_role_data_scope` 保证。

```sql
INSERT INTO sys_role_data_scope (
    id,
    role_id,
    resource_id,
    scope_type,
    enabled,
    status,
    remark,
    create_by,
    update_by
) VALUES (
    70001,
    20001,
    60001,
    2,
    1,
    1,
    '客户数据权限：本部门及下级部门',
    1,
    1
);
```

#### 修改角色数据权限范围

修改角色的数据范围时，需要同步处理自定义部门明细。如果从自定义部门切换到其他范围，应清理旧的自定义部门关系。

```sql
START TRANSACTION;

UPDATE sys_role_data_scope
SET scope_type = 3,
    update_by = 1
WHERE id = 70001
  AND deleted = 0;

UPDATE sys_role_data_scope_dept
SET deleted = 1
WHERE role_data_scope_id = 70001
  AND deleted = 0;

COMMIT;
```

#### 配置自定义部门范围

配置自定义部门范围时，先将角色数据权限设置为 `scope_type = 4`，再重建自定义部门明细。

```sql
START TRANSACTION;

UPDATE sys_role_data_scope
SET scope_type = 4,
    update_by = 1
WHERE id = 70001
  AND deleted = 0;

UPDATE sys_role_data_scope_dept
SET deleted = 1
WHERE role_data_scope_id = 70001
  AND deleted = 0;

INSERT INTO sys_role_data_scope_dept (
    id,
    role_data_scope_id,
    dept_id,
    include_children,
    create_by
) VALUES
    (80001, 70001, 10, 1, 1),
    (80002, 70001, 20, 0, 1);

COMMIT;
```

#### 禁用某个数据资源权限控制

禁用数据资源后，业务层可以选择不再对该资源追加数据权限过滤条件。该操作应谨慎使用，通常只适合临时关闭某个模块的数据权限控制。

```sql
UPDATE sys_data_resource
SET enabled = 0,
    update_by = 1
WHERE resource_code = 'customer'
  AND deleted = 0;
```

#### 调整业务数据所属部门

当业务数据转移部门时，需要更新业务表的 `dept_id`。这会直接影响哪些用户可以查看该数据。

```sql
UPDATE biz_customer
SET dept_id = 20,
    update_by = 10001
WHERE id = 90001
  AND deleted = 0;
```

#### 调整业务数据负责人

当业务数据转移负责人时，需要更新业务表的 `owner_user_id`。这会影响“仅本人数据”范围下的查询结果。

```sql
UPDATE biz_customer
SET owner_user_id = 10002,
    update_by = 10001
WHERE id = 90001
  AND deleted = 0;
```

### 常见问题

数据权限模型的难点不在表结构本身，而在权限合并、查询性能、缓存一致性和业务字段规范。如果业务表缺少统一的数据归属字段，后续再补数据权限会比较困难。

| 问题                   | 原因                                       | 建议                                              |
| ---------------------- | ------------------------------------------ | ------------------------------------------------- |
| 用户能看到不该看的数据 | 查询时没有追加数据权限条件                 | 所有受控业务查询统一经过数据权限处理              |
| 用户看不到应看到的数据 | 部门归属字段、负责人字段维护错误           | 业务写入时必须正确维护 `dept_id`、`owner_user_id` |
| 多角色权限合并不正确   | 只取了某一个角色的数据范围                 | 应按全部角色合并数据范围，全部数据权限优先级最高  |
| 自定义部门范围失效     | 配置了部门但没有处理下级部门               | `include_children = 1` 时需要递归计算下级部门     |
| 数据权限查询慢         | 业务表缺少 `dept_id`、`owner_user_id` 索引 | 对数据权限过滤字段建立组合索引                    |
| 每个接口都写权限 SQL   | 过滤逻辑散落在业务代码中                   | 在 Mapper、查询构造器、拦截器或服务层统一封装     |
| 授权后权限不生效       | 数据权限结果被缓存                         | 修改角色数据权限后清理相关用户的数据权限缓存      |
| 动态 SQL 存在注入风险  | 直接使用表名、字段名拼接 SQL               | 表名和字段名只能来自可信元数据，不允许前端传入    |

多角色合并时建议遵循以下规则：

| 规则               | 说明                                                 |
| ------------------ | ---------------------------------------------------- |
| 全部数据优先       | 任意角色拥有全部数据权限时，最终结果为全部数据       |
| 部门范围取并集     | 多个部门范围应合并为一个部门 ID 集合                 |
| 本人数据可叠加     | 本人数据可以和部门范围使用 `OR` 合并                 |
| 禁用规则不参与计算 | `enabled = 0` 或 `status = 0` 的规则不生效           |
| 空权限返回空结果   | 没有任何数据权限时，应返回空数据，而不是默认全部数据 |

数据权限不要只依赖前端控制。前端隐藏菜单、按钮或筛选条件只能改善用户体验，真正的数据隔离必须在后端查询中完成。

### 总结

数据权限模型解决的是“用户能够访问哪些业务数据”的问题。它通常基于角色分配数据范围，再结合组织架构、业务归属字段和查询条件实现精细化过滤。

落地时应重点关注以下原则：

| 原则               | 说明                                                   |
| ------------------ | ------------------------------------------------------ |
| 角色承载数据范围   | 不建议直接给用户配置数据权限，优先通过角色统一维护     |
| 资源按模块配置     | 客户、订单、合同等不同资源可以配置不同数据范围         |
| 业务表保留归属字段 | 受控业务表必须有 `dept_id`、`owner_user_id` 或类似字段 |
| 多角色权限取并集   | 用户拥有多个角色时，应合并所有有效数据范围             |
| 全部数据优先级最高 | 只要拥有全部数据权限，就不需要追加数据范围过滤         |
| 高频查询需要缓存   | 用户数据权限范围可以缓存，授权变更后主动清理           |
| 后端必须强制过滤   | 数据权限必须在后端查询中生效，不能只依赖前端控制       |
| 索引跟随过滤字段   | 被数据权限使用的字段必须建立索引                       |

该模型适合作为业务系统的数据隔离基础。对于更复杂的组织层级、岗位职责、跨部门协作和临时授权需求，可以在此模型之上继续扩展组织架构模型、岗位模型和授权委托模型。



## 组织架构模型

组织架构模型用于描述企业、公司、部门、团队等组织单元之间的上下级关系。它通常是用户归属、数据权限、岗位管理、审批流、报表统计和组织范围查询的基础模型。

在实际系统中，组织架构不仅用于展示部门树，还会参与“用户属于哪个部门”“部门负责人是谁”“某个部门有哪些下级部门”“某个用户能查看哪些部门数据”等业务逻辑。

### 适用场景

组织架构模型适合存在层级组织、部门归属、上下级管理、部门负责人、跨部门协作和组织范围统计的业务系统。

常见适用场景如下：

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 企业部门管理 | 维护集团、公司、中心、部门、小组等层级结构   |
| 用户归属部门 | 用户绑定主部门或多个兼任部门                 |
| 数据权限过滤 | 根据用户所在部门查询本部门或下级部门数据     |
| 审批流程     | 根据部门负责人、上级部门、组织层级查找审批人 |
| 报表统计     | 按部门、区域、组织层级汇总业务数据           |
| 组织树展示   | 后台管理系统展示组织架构树                   |
| 部门调整     | 支持部门改名、迁移、禁用、合并等组织变更     |

组织架构模型通常会被数据权限模型、岗位模型、用户模型引用。为了避免后续扩展困难，部门表不应只保存部门名称，还应保存父级部门、层级、路径、负责人、状态、排序等字段。

### 建模结构

组织架构模型通常采用单表树结构表示部门层级。每个组织节点保存自己的父级节点，同时保存树路径和层级，便于快速查询上级链路、下级部门和组织树。

```text
sys_dept
   |
   | 1:N
   |
sys_dept
```

用户与部门之间可以采用两种方式建模。

第一种是单部门模型，适合一个用户只属于一个主部门的系统。

```text
sys_dept
   |
   | 1:N
   |
sys_user
```

第二种是多部门模型，适合一个用户存在主部门、兼任部门、临时借调部门等情况。

```text
sys_user
   |
   | 1:N
   |
sys_user_dept
   |
   | N:1
   |
sys_dept
```

核心表说明如下：

| 表名            | 说明                                             |
| --------------- | ------------------------------------------------ |
| `sys_dept`      | 部门表，保存组织架构节点及上下级关系             |
| `sys_user`      | 用户表，单部门模式下可直接保存主部门 ID          |
| `sys_user_dept` | 用户部门关系表，多部门模式下保存用户与部门的关系 |
| 业务表          | 通常通过 `dept_id` 保存业务数据归属部门          |

推荐优先使用 `sys_dept` + `sys_user_dept` 的组合。即使当前系统只支持一个用户一个部门，也可以通过 `main_flag` 标识主部门，为后续兼任、借调、跨部门权限预留扩展空间。

### 字段设计

字段设计需要兼顾组织树查询、部门排序、负责人维护、数据权限过滤和组织调整。部门表建议保存 `parent_id`、`tree_path`、`level_no`，避免每次查询组织范围都依赖复杂递归。

部门表用于维护组织架构树。

```sql
CREATE TABLE sys_dept (
    id BIGINT NOT NULL COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示顶级部门',
    tree_path VARCHAR(512) NOT NULL DEFAULT '/' COMMENT '树路径，例如 /1/10/100/',
    level_no INT NOT NULL DEFAULT 1 COMMENT '层级，从1开始',
    dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(128) NOT NULL COMMENT '部门名称',
    dept_short_name VARCHAR(64) DEFAULT NULL COMMENT '部门简称',
    dept_type TINYINT NOT NULL DEFAULT 1 COMMENT '部门类型：1公司，2部门，3小组，4区域',
    leader_user_id BIGINT DEFAULT NULL COMMENT '部门负责人用户ID',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
```

用户部门关系表用于支持用户多部门归属。`main_flag` 用于标识主部门，业务中通常只有主部门参与默认数据权限判断，兼任部门是否参与数据权限需要按业务规则确定。

```sql
CREATE TABLE sys_user_dept (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    main_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主部门：0否，1是',
    relation_type TINYINT NOT NULL DEFAULT 1 COMMENT '关系类型：1正式，2兼任，3借调',
    start_date DATE DEFAULT NULL COMMENT '生效日期',
    end_date DATE DEFAULT NULL COMMENT '失效日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户部门关系表';
```

如果系统明确只支持单部门用户，也可以在用户表中增加主部门字段。

```sql
ALTER TABLE sys_user
    ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '主部门ID' AFTER user_type;
```

业务表通常需要保存数据所属部门，便于数据权限过滤和组织维度统计。下面以客户表为例。

```sql
CREATE TABLE biz_customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID',
    owner_user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
```

字段设计建议如下：

| 字段             | 设计建议                                           |
| ---------------- | -------------------------------------------------- |
| `parent_id`      | 表示直接上级部门，适合组织树加载和递归查询         |
| `tree_path`      | 保存完整树路径，适合快速查询某部门下所有子部门     |
| `level_no`       | 保存组织层级，适合限制层级深度和展示缩进           |
| `dept_code`      | 部门编码应稳定，可用于外部系统同步和业务识别       |
| `dept_name`      | 部门名称允许调整，不建议作为唯一业务标识           |
| `dept_type`      | 用于区分公司、部门、小组、区域等不同组织节点       |
| `leader_user_id` | 保存部门负责人，适合审批流和组织负责人查询         |
| `main_flag`      | 多部门用户场景下标识主部门                         |
| `relation_type`  | 区分正式、兼任、借调等用户部门关系                 |
| `dept_id`        | 业务表中的部门归属字段，是数据权限和组织统计的基础 |

### 索引设计

索引设计需要围绕组织树加载、子部门查询、部门编码唯一、用户部门查询和业务数据按部门过滤展开。组织架构查询通常频繁出现在登录初始化、数据权限计算、审批人查找和后台树形组件加载中。

```sql
ALTER TABLE sys_dept
    ADD UNIQUE KEY uk_sys_dept_code (dept_code, deleted),
    ADD KEY idx_sys_dept_parent (parent_id, deleted),
    ADD KEY idx_sys_dept_path (tree_path),
    ADD KEY idx_sys_dept_level (level_no, deleted),
    ADD KEY idx_sys_dept_status_sort (status, sort_no, deleted),
    ADD KEY idx_sys_dept_leader (leader_user_id, deleted);

ALTER TABLE sys_user_dept
    ADD UNIQUE KEY uk_sys_user_dept (user_id, dept_id, deleted),
    ADD KEY idx_sys_user_dept_user (user_id, main_flag, status, deleted),
    ADD KEY idx_sys_user_dept_dept (dept_id, status, deleted),
    ADD KEY idx_sys_user_dept_date (start_date, end_date);

ALTER TABLE sys_user
    ADD KEY idx_sys_user_dept (dept_id, deleted);

ALTER TABLE biz_customer
    ADD KEY idx_biz_customer_dept (dept_id, deleted),
    ADD KEY idx_biz_customer_dept_status_time (dept_id, status, create_time);
```

索引说明如下：

| 索引                                | 说明                             |
| ----------------------------------- | -------------------------------- |
| `uk_sys_dept_code`                  | 保证未删除部门编码唯一           |
| `idx_sys_dept_parent`               | 加速查询某个部门的直接子部门     |
| `idx_sys_dept_path`                 | 加速基于树路径的下级部门范围查询 |
| `idx_sys_dept_status_sort`          | 加速组织树按状态和排序号加载     |
| `idx_sys_dept_leader`               | 加速根据负责人反查负责部门       |
| `uk_sys_user_dept`                  | 防止同一用户重复绑定同一部门     |
| `idx_sys_user_dept_user`            | 加速查询用户所属部门             |
| `idx_sys_user_dept_dept`            | 加速查询部门下的用户             |
| `idx_biz_customer_dept`             | 加速业务数据按部门过滤           |
| `idx_biz_customer_dept_status_time` | 加速部门范围内的分页列表查询     |

如果部门树非常大，且经常查询全部下级部门，可以优先使用 `tree_path LIKE '/1/10/%'` 这种左前缀查询方式。不要使用 `LIKE '%/10/%'` 作为主查询条件，否则普通索引难以有效利用。

### 常用查询

常用查询主要围绕组织树加载、上级链路查询、下级部门查询、用户部门查询、部门用户查询和组织范围业务数据查询展开。组织架构查询通常会被权限、审批、统计等多个模块复用，建议在服务层封装统一方法。

#### 查询顶级部门

该查询用于加载组织树第一层节点。

```sql
SELECT
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_type,
    leader_user_id,
    sort_no,
    status
FROM sys_dept
WHERE parent_id = 0
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

#### 查询某个部门的直接子部门

该查询用于懒加载组织树节点。树形组件按需展开时，通常使用该方式减少一次性加载的数据量。

```sql
SELECT
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_type,
    leader_user_id,
    sort_no,
    status
FROM sys_dept
WHERE parent_id = 10
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

#### 查询完整组织树

该查询用于一次性加载完整组织架构。MySQL 8 可以使用递归 CTE 根据 `parent_id` 构建树形结果。

```sql
WITH RECURSIVE dept_tree AS (
    SELECT
        id,
        parent_id,
        tree_path,
        level_no,
        dept_code,
        dept_name,
        dept_type,
        leader_user_id,
        sort_no,
        status
    FROM sys_dept
    WHERE parent_id = 0
      AND deleted = 0

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.tree_path,
        d.level_no,
        d.dept_code,
        d.dept_name,
        d.dept_type,
        d.leader_user_id,
        d.sort_no,
        d.status
    FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_type,
    leader_user_id,
    sort_no,
    status
FROM dept_tree
ORDER BY tree_path ASC, sort_no ASC, id ASC;
```

#### 查询某个部门及所有下级部门

该查询用于数据权限、部门统计和组织范围筛选。基于 `tree_path` 的查询通常比递归查询更简单。

```sql
SELECT
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_type,
    leader_user_id,
    status
FROM sys_dept
WHERE tree_path LIKE '/1/10/%'
  AND deleted = 0
ORDER BY tree_path ASC, sort_no ASC, id ASC;
```

如果只知道部门 ID，不知道 `tree_path`，可以先查询目标部门路径，再查询下级部门。

```sql
SELECT
    id,
    tree_path
FROM sys_dept
WHERE id = 10
  AND deleted = 0
LIMIT 1;
```

然后使用查到的 `tree_path` 作为前缀查询部门范围。

```sql
SELECT
    id,
    dept_name
FROM sys_dept
WHERE tree_path LIKE CONCAT('/1/10/', '%')
  AND deleted = 0
ORDER BY tree_path ASC, sort_no ASC;
```

#### 查询某个部门的上级链路

该查询用于面包屑展示、审批上级查找和组织路径展示。通过解析目标部门的 `tree_path` 可以获得所有上级节点。

```sql
SELECT
    p.id,
    p.parent_id,
    p.dept_code,
    p.dept_name,
    p.level_no
FROM sys_dept d
INNER JOIN sys_dept p
    ON d.tree_path LIKE CONCAT(p.tree_path, '%')
WHERE d.id = 100
  AND d.deleted = 0
  AND p.deleted = 0
ORDER BY p.level_no ASC;
```

#### 查询用户主部门

该查询用于登录后加载用户上下文、数据权限计算和默认部门展示。

```sql
SELECT
    d.id,
    d.dept_code,
    d.dept_name,
    d.tree_path,
    d.level_no,
    ud.relation_type
FROM sys_user_dept ud
INNER JOIN sys_dept d ON d.id = ud.dept_id
WHERE ud.user_id = 10001
  AND ud.main_flag = 1
  AND ud.status = 1
  AND ud.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
LIMIT 1;
```

#### 查询用户所有所属部门

该查询适合用户存在兼任、借调或多部门归属的场景。

```sql
SELECT
    d.id,
    d.dept_code,
    d.dept_name,
    d.tree_path,
    d.level_no,
    ud.main_flag,
    ud.relation_type,
    ud.start_date,
    ud.end_date
FROM sys_user_dept ud
INNER JOIN sys_dept d ON d.id = ud.dept_id
WHERE ud.user_id = 10001
  AND ud.status = 1
  AND ud.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND (ud.start_date IS NULL OR ud.start_date <= CURRENT_DATE)
  AND (ud.end_date IS NULL OR ud.end_date >= CURRENT_DATE)
ORDER BY ud.main_flag DESC, d.level_no ASC, d.sort_no ASC;
```

#### 查询部门下的用户

该查询用于部门成员管理页面。只查询直接绑定当前部门的用户，不包含下级部门用户。

```sql
SELECT
    u.id,
    u.username,
    u.nickname,
    u.mobile,
    u.email,
    ud.main_flag,
    ud.relation_type
FROM sys_user_dept ud
INNER JOIN sys_user u ON u.id = ud.user_id
WHERE ud.dept_id = 10
  AND ud.status = 1
  AND ud.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
ORDER BY ud.main_flag DESC, u.id ASC;
```

#### 查询部门及下级部门的所有用户

该查询用于查询一个组织范围内的所有成员，例如部门负责人查看本部门及下级部门人员。

```sql
SELECT DISTINCT
    u.id,
    u.username,
    u.nickname,
    u.mobile,
    u.email,
    d.id AS dept_id,
    d.dept_name
FROM sys_dept d
INNER JOIN sys_user_dept ud ON ud.dept_id = d.id
INNER JOIN sys_user u ON u.id = ud.user_id
WHERE d.tree_path LIKE '/1/10/%'
  AND d.deleted = 0
  AND ud.status = 1
  AND ud.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
ORDER BY d.tree_path ASC, u.id ASC;
```

#### 按组织范围查询业务数据

该查询用于部门数据权限过滤。业务层先计算可访问部门 ID 或部门路径，再追加到业务查询中。

```sql
SELECT
    c.id,
    c.customer_name,
    c.dept_id,
    d.dept_name,
    c.owner_user_id,
    c.status,
    c.create_time
FROM biz_customer c
INNER JOIN sys_dept d ON d.id = c.dept_id
WHERE d.tree_path LIKE '/1/10/%'
  AND c.deleted = 0
  AND d.deleted = 0
ORDER BY c.create_time DESC
LIMIT 20 OFFSET 0;
```

如果业务查询更关注性能，建议先查出部门 ID 集合，再通过 `dept_id IN (...)` 查询业务表。

```sql
SELECT
    id
FROM sys_dept
WHERE tree_path LIKE '/1/10/%'
  AND deleted = 0;
SELECT
    id,
    customer_name,
    dept_id,
    owner_user_id,
    status,
    create_time
FROM biz_customer
WHERE dept_id IN (10, 11, 12, 13)
  AND deleted = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

### 常用写入

常用写入主要包括创建部门、修改部门、移动部门、绑定用户部门、调整主部门和禁用部门。涉及部门移动时，需要同步更新当前部门及所有下级部门的 `tree_path` 和 `level_no`，建议在事务中完成。

#### 创建顶级部门

创建顶级部门时，`parent_id` 为 `0`，`tree_path` 通常使用 `/{id}/` 形式。

```sql
INSERT INTO sys_dept (
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_short_name,
    dept_type,
    leader_user_id,
    sort_no,
    status,
    create_by,
    update_by
) VALUES (
    1,
    0,
    '/1/',
    1,
    'GROUP',
    '集团总部',
    '总部',
    1,
    10001,
    1,
    1,
    1,
    1
);
```

#### 创建子部门

创建子部门时，需要基于父部门的 `tree_path` 和 `level_no` 生成当前部门的路径和层级。

```sql
INSERT INTO sys_dept (
    id,
    parent_id,
    tree_path,
    level_no,
    dept_code,
    dept_name,
    dept_short_name,
    dept_type,
    leader_user_id,
    sort_no,
    status,
    create_by,
    update_by
)
SELECT
    10 AS id,
    p.id AS parent_id,
    CONCAT(p.tree_path, '10/') AS tree_path,
    p.level_no + 1 AS level_no,
    'TECH' AS dept_code,
    '技术部' AS dept_name,
    '技术' AS dept_short_name,
    2 AS dept_type,
    10002 AS leader_user_id,
    10 AS sort_no,
    1 AS status,
    1 AS create_by,
    1 AS update_by
FROM sys_dept p
WHERE p.id = 1
  AND p.deleted = 0;
```

#### 修改部门基础信息

修改部门名称、负责人、排序等基础字段时，不需要调整下级部门。

```sql
UPDATE sys_dept
SET dept_name = '研发中心',
    dept_short_name = '研发',
    leader_user_id = 10003,
    sort_no = 20,
    update_by = 1
WHERE id = 10
  AND deleted = 0;
```

#### 移动部门

移动部门会影响当前部门及所有下级部门的树路径和层级。该操作必须放在事务中，并且需要先禁止将部门移动到自己的下级节点下。

下面示例表示将部门 `10` 移动到新父部门 `2` 下。

```sql
START TRANSACTION;

SELECT
    @old_path := tree_path,
    @old_level := level_no
FROM sys_dept
WHERE id = 10
  AND deleted = 0;

SELECT
    @new_parent_path := tree_path,
    @new_parent_level := level_no
FROM sys_dept
WHERE id = 2
  AND deleted = 0;

UPDATE sys_dept
SET tree_path = REPLACE(tree_path, @old_path, CONCAT(@new_parent_path, '10/')),
    level_no = level_no + ((@new_parent_level + 1) - @old_level),
    update_by = 1
WHERE tree_path LIKE CONCAT(@old_path, '%')
  AND deleted = 0;

UPDATE sys_dept
SET parent_id = 2,
    update_by = 1
WHERE id = 10
  AND deleted = 0;

COMMIT;
```

移动部门前应先判断新父部门不能是当前部门本身，也不能是当前部门的下级部门。

```sql
SELECT
    COUNT(1) AS invalid_parent_count
FROM sys_dept
WHERE id = 2
  AND tree_path LIKE '/1/10/%'
  AND deleted = 0;
```

如果 `invalid_parent_count > 0`，说明新父部门位于当前部门下级，不能移动。

#### 绑定用户主部门

绑定用户主部门时，建议先取消用户原有主部门，再插入或更新新的主部门关系。该操作需要放在事务中。

```sql
START TRANSACTION;

UPDATE sys_user_dept
SET main_flag = 0,
    update_by = 1
WHERE user_id = 10001
  AND main_flag = 1
  AND deleted = 0;

INSERT INTO sys_user_dept (
    id,
    user_id,
    dept_id,
    main_flag,
    relation_type,
    status,
    create_by,
    update_by
) VALUES (
    90001,
    10001,
    10,
    1,
    1,
    1,
    1,
    1
);

COMMIT;
```

如果用户表中同时维护了 `dept_id`，需要同步更新用户主部门字段。

```sql
UPDATE sys_user
SET dept_id = 10,
    update_by = 1
WHERE id = 10001
  AND deleted = 0;
```

#### 添加用户兼任部门

添加兼任部门时，`main_flag` 应为 `0`，`relation_type` 可以设置为兼任。

```sql
INSERT INTO sys_user_dept (
    id,
    user_id,
    dept_id,
    main_flag,
    relation_type,
    start_date,
    end_date,
    status,
    create_by,
    update_by
) VALUES (
    90002,
    10001,
    20,
    0,
    2,
    CURRENT_DATE,
    NULL,
    1,
    1,
    1
);
```

#### 禁用部门

禁用部门通常不建议直接级联禁用所有下级部门。是否级联应根据业务明确处理。

```sql
UPDATE sys_dept
SET status = 0,
    update_by = 1
WHERE id = 10
  AND deleted = 0;
```

如果业务要求禁用当前部门及所有下级部门，可以基于 `tree_path` 批量处理。

```sql
UPDATE sys_dept
SET status = 0,
    update_by = 1
WHERE tree_path LIKE '/1/10/%'
  AND deleted = 0;
```

#### 逻辑删除部门

删除部门前需要检查是否存在未删除的子部门和有效用户。如果存在，不建议直接删除。

```sql
SELECT
    COUNT(1) AS child_count
FROM sys_dept
WHERE parent_id = 10
  AND deleted = 0;
SELECT
    COUNT(1) AS user_count
FROM sys_user_dept
WHERE dept_id = 10
  AND status = 1
  AND deleted = 0;
```

确认没有子部门和有效用户后，再进行逻辑删除。

```sql
UPDATE sys_dept
SET deleted = 1,
    update_by = 1
WHERE id = 10
  AND deleted = 0;
```

### 常见问题

组织架构模型的主要问题集中在树路径维护、部门移动、用户多部门归属、历史归属变化和数据权限联动上。组织架构一旦被业务数据引用，修改成本通常较高。

| 问题                         | 原因                               | 建议                                         |
| ---------------------------- | ---------------------------------- | -------------------------------------------- |
| 部门树查询慢                 | 只使用 `parent_id`，每次都递归查询 | 同时维护 `tree_path` 和 `level_no`           |
| 部门移动后下级路径错误       | 只更新了当前部门，没有更新下级部门 | 移动部门时批量更新整棵子树                   |
| 出现循环层级                 | 允许部门移动到自己的下级部门下     | 移动前校验新父部门不能位于当前部门子树中     |
| 用户主部门不唯一             | 多部门关系缺少业务校验             | 绑定主部门时先取消原主部门，再设置新主部门   |
| 禁用部门后用户还能登录       | 用户状态和部门状态没有联动         | 登录时根据业务判断是否校验主部门状态         |
| 数据权限结果不正确           | 业务数据的 `dept_id` 维护错误      | 业务写入时必须明确数据所属部门               |
| 部门编码重复                 | 没有唯一约束或逻辑删除处理不当     | 对 `dept_code` 和 `deleted` 建立联合唯一索引 |
| 删除部门造成历史数据无法追溯 | 物理删除组织节点                   | 使用逻辑删除或禁用，保留历史引用             |
| 组织调整影响历史报表         | 历史数据只引用当前部门             | 报表强依赖历史组织时，需要引入组织快照       |

组织架构模型需要特别注意“当前组织”和“历史组织”的区别。大多数后台系统只需要当前组织结构；如果业务涉及历史绩效、历史审批、历史销售归属，则需要在业务单据中保存部门名称快照，或单独设计组织历史版本模型。

### 总结

组织架构模型是用户、权限、数据权限、岗位、审批和报表统计的基础模型。它的核心是用部门表维护组织树，用用户部门关系表维护人员归属，用业务表中的部门字段承接数据归属。

落地时应重点关注以下原则：

| 原则                   | 说明                                               |
| ---------------------- | -------------------------------------------------- |
| 使用稳定部门编码       | `dept_code` 适合作为外部同步和业务识别字段         |
| 同时维护父级和路径     | `parent_id` 适合树形结构，`tree_path` 适合范围查询 |
| 部门移动必须更新子树   | 修改父级时同步更新当前部门及所有下级部门路径       |
| 用户归属建议关系表承载 | 多部门、兼任、借调场景更容易扩展                   |
| 主部门需要唯一         | 用户只能有一个主部门，兼任部门按业务规则处理       |
| 业务数据保存部门归属   | 受数据权限控制的业务表必须维护 `dept_id`           |
| 禁用优先于删除         | 组织节点被历史数据引用时，不建议物理删除           |
| 历史统计需要快照       | 对历史组织敏感的业务，应保存组织名称或组织版本快照 |

该模型适合作为组织层级和人员归属的基础设计。后续岗位、数据权限、审批流和组织统计都可以在该模型之上继续扩展。



## 岗位模型

岗位模型用于描述组织中的职责位置，例如部门经理、销售专员、财务会计、研发工程师、审批专员等。岗位通常与组织架构、用户、角色、审批流和数据权限联动，用来表达“某个用户在某个部门承担什么职责”。

岗位和角色需要区分。岗位偏向组织管理和业务职责，角色偏向系统权限授权。一个岗位可以关联一个或多个角色，但不建议将岗位直接等同于角色。

### 适用场景

岗位模型适合存在职责分工、组织任职、岗位权限、审批人查找、岗位变更和人员任职历史的业务系统。它通常与组织架构模型一起使用。

常见适用场景如下：

| 场景             | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 用户任职管理     | 维护用户在某个部门下担任的岗位                       |
| 主岗位与兼任岗位 | 用户可以有一个主岗位，也可以兼任多个岗位             |
| 审批流找人       | 根据部门经理、财务负责人、HR 专员等岗位查找审批人    |
| 岗位授权         | 岗位关联角色，用户任职后自动获得对应角色             |
| 数据权限扩展     | 根据岗位判断是否能查看本部门、下级部门或指定业务数据 |
| 组织人事管理     | 支持调岗、兼岗、离岗、岗位禁用                       |
| 报表统计         | 按岗位统计人员数量、业务处理量、审批效率等           |

岗位模型不建议只用一个字符串字段保存在用户表中。这样会导致岗位变更、兼任、历史追溯和审批流查找都难以扩展。推荐使用岗位表和用户岗位关系表建模。

### 建模结构

岗位模型的基础结构由岗位表和用户岗位关系表组成。岗位描述职责，用户岗位关系描述用户在某个部门下担任哪个岗位。

```text
sys_dept
   |
   | 1:N
   |
sys_user_post
   |
   | N:1
   |
sys_user

sys_post
   |
   | 1:N
   |
sys_user_post
```

如果岗位需要自动关联系统权限，可以增加岗位角色关系表。用户绑定岗位后，系统可以根据岗位关联的角色为用户派生权限。

```text
sys_post
   |
   | 1:N
   |
sys_post_role
   |
   | N:1
   |
sys_role
```

核心表说明如下：

| 表名            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| `sys_post`      | 岗位表，保存岗位编码、岗位名称、岗位类型、岗位等级等基础信息 |
| `sys_user_post` | 用户岗位关系表，保存用户在部门下的岗位任职关系               |
| `sys_post_role` | 岗位角色关系表，用于岗位自动关联权限角色                     |
| `sys_user`      | 用户表，表示任职人员                                         |
| `sys_dept`      | 部门表，表示岗位任职所属组织                                 |
| `sys_role`      | 角色表，表示岗位可派生的系统权限                             |

岗位通常分为两类设计方式。

第一种是全局岗位。岗位本身不绑定具体部门，例如“部门经理”“销售专员”“Java 开发工程师”。用户任职时再通过 `sys_user_post.dept_id` 指定所属部门。该方式复用性更好，推荐优先使用。

第二种是部门岗位。岗位直接绑定部门，例如“华东销售部经理”“北京研发部工程师”。该方式表达直观，但岗位数量容易膨胀，不适合组织规模较大的系统。

本文采用全局岗位 + 用户岗位关系的方式建模。

### 字段设计

字段设计需要支持岗位编码唯一、岗位分类、岗位等级、岗位排序、主岗位、兼任岗位、任职生效时间和岗位角色派生。岗位本身应保持相对稳定，用户任职关系可以频繁变化。

岗位表用于维护系统中的岗位字典。

```sql
CREATE TABLE sys_post (
    id BIGINT NOT NULL COMMENT '主键ID',
    post_code VARCHAR(64) NOT NULL COMMENT '岗位编码，例如 dept_manager、sales_staff、java_dev',
    post_name VARCHAR(128) NOT NULL COMMENT '岗位名称',
    post_type TINYINT NOT NULL DEFAULT 1 COMMENT '岗位类型：1管理岗，2业务岗，3技术岗，4职能岗',
    post_level INT NOT NULL DEFAULT 1 COMMENT '岗位等级，数值越大等级越高',
    post_category VARCHAR(64) DEFAULT NULL COMMENT '岗位分类，例如 manage、sales、tech、finance',
    duty_desc VARCHAR(512) DEFAULT NULL COMMENT '岗位职责描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
```

用户岗位关系表用于维护用户在部门下的任职关系。该表是岗位模型的核心，因为岗位只有和用户、部门结合后才具备业务含义。

```sql
CREATE TABLE sys_user_post (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    post_id BIGINT NOT NULL COMMENT '岗位ID',
    main_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主岗位：0否，1是',
    relation_type TINYINT NOT NULL DEFAULT 1 COMMENT '任职类型：1正式，2兼任，3代理，4临时',
    start_date DATE DEFAULT NULL COMMENT '任职开始日期',
    end_date DATE DEFAULT NULL COMMENT '任职结束日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户岗位关系表';
```

岗位角色关系表用于支持岗位自动派生系统角色。该表不是所有系统都必须具备，但在企业后台、OA、ERP 中非常常见。

```sql
CREATE TABLE sys_post_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '岗位ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位角色关系表';
```

如果系统需要保留岗位变更历史，可以增加岗位任职历史表。该表通常由用户岗位关系变更时异步或同步写入。

```sql
CREATE TABLE sys_user_post_history (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    post_id BIGINT NOT NULL COMMENT '岗位ID',
    main_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主岗位：0否，1是',
    relation_type TINYINT NOT NULL COMMENT '任职类型：1正式，2兼任，3代理，4临时',
    change_type TINYINT NOT NULL COMMENT '变更类型：1任职，2调岗，3离岗，4禁用',
    start_date DATE DEFAULT NULL COMMENT '任职开始日期',
    end_date DATE DEFAULT NULL COMMENT '任职结束日期',
    change_reason VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户岗位历史表';
```

字段设计建议如下：

| 字段            | 设计建议                                               |
| --------------- | ------------------------------------------------------ |
| `post_code`     | 岗位编码应稳定，适合审批流、规则引擎、外部系统同步使用 |
| `post_name`     | 岗位名称可以展示给用户，不建议作为程序判断依据         |
| `post_type`     | 用于区分管理岗、业务岗、技术岗、职能岗等类型           |
| `post_level`    | 用于岗位层级判断，例如审批时查找更高等级岗位           |
| `post_category` | 用于岗位分类检索和报表统计                             |
| `duty_desc`     | 保存岗位职责描述，避免只维护岗位名称                   |
| `main_flag`     | 标识用户主岗位，通常一个用户只能有一个有效主岗位       |
| `relation_type` | 区分正式、兼任、代理、临时等任职关系                   |
| `start_date`    | 控制岗位任职生效时间                                   |
| `end_date`      | 控制岗位任职失效时间                                   |
| `status`        | 用于临时禁用岗位或任职关系                             |
| `deleted`       | 用于逻辑删除，保留历史引用和审计线索                   |

### 索引设计

索引设计需要围绕岗位编码唯一、岗位列表查询、用户岗位查询、部门岗位查询、岗位找人、岗位派生角色和任职有效期判断展开。

```sql
ALTER TABLE sys_post
    ADD UNIQUE KEY uk_sys_post_code (post_code, deleted),
    ADD KEY idx_sys_post_type_level (post_type, post_level, deleted),
    ADD KEY idx_sys_post_category (post_category, deleted),
    ADD KEY idx_sys_post_status_sort (status, sort_no, deleted);

ALTER TABLE sys_user_post
    ADD UNIQUE KEY uk_sys_user_post (user_id, dept_id, post_id, deleted),
    ADD KEY idx_sys_user_post_user (user_id, main_flag, status, deleted),
    ADD KEY idx_sys_user_post_dept (dept_id, status, deleted),
    ADD KEY idx_sys_user_post_post (post_id, status, deleted),
    ADD KEY idx_sys_user_post_dept_post (dept_id, post_id, status, deleted),
    ADD KEY idx_sys_user_post_date (start_date, end_date);

ALTER TABLE sys_post_role
    ADD UNIQUE KEY uk_sys_post_role (post_id, role_id, deleted),
    ADD KEY idx_sys_post_role_post (post_id, deleted),
    ADD KEY idx_sys_post_role_role (role_id, deleted);

ALTER TABLE sys_user_post_history
    ADD KEY idx_sys_user_post_history_user (user_id, create_time),
    ADD KEY idx_sys_user_post_history_dept (dept_id, create_time),
    ADD KEY idx_sys_user_post_history_post (post_id, create_time),
    ADD KEY idx_sys_user_post_history_change (change_type, create_time);
```

索引说明如下：

| 索引                             | 说明                                         |
| -------------------------------- | -------------------------------------------- |
| `uk_sys_post_code`               | 保证未删除岗位编码唯一                       |
| `idx_sys_post_type_level`        | 加速按岗位类型和岗位等级查询                 |
| `idx_sys_post_status_sort`       | 加速岗位列表展示和排序                       |
| `uk_sys_user_post`               | 防止用户在同一部门重复绑定同一岗位           |
| `idx_sys_user_post_user`         | 加速查询用户当前岗位                         |
| `idx_sys_user_post_dept`         | 加速查询部门下所有任职人员                   |
| `idx_sys_user_post_post`         | 加速根据岗位查找任职用户                     |
| `idx_sys_user_post_dept_post`    | 加速按部门和岗位查找人员，例如查找某部门经理 |
| `uk_sys_post_role`               | 防止岗位重复绑定同一角色                     |
| `idx_sys_post_role_post`         | 加速岗位派生角色查询                         |
| `idx_sys_user_post_history_user` | 加速查询用户岗位变更历史                     |

如果系统要求一个用户只能有一个有效主岗位，MySQL 无法直接通过普通唯一索引表达“仅 `main_flag = 1` 且未删除且有效”的条件。建议在业务层事务中保证：设置新主岗位前，先取消用户原有主岗位。

### 常用查询

常用查询主要围绕岗位列表、用户岗位、部门岗位、岗位找人、岗位派生角色、审批人查找和任职历史展开。岗位查询通常会被组织管理、审批流、权限初始化和用户详情页复用。

#### 查询岗位列表

该查询用于岗位管理页面。通常按状态、岗位类型、岗位分类进行筛选。

```sql
SELECT
    id,
    post_code,
    post_name,
    post_type,
    post_level,
    post_category,
    duty_desc,
    status,
    sort_no,
    create_time
FROM sys_post
WHERE deleted = 0
  AND status = 1
ORDER BY sort_no ASC, post_level DESC, id ASC;
```

#### 根据岗位编码查询岗位

该查询适合审批流、规则引擎或系统初始化逻辑根据稳定编码查找岗位。

```sql
SELECT
    id,
    post_code,
    post_name,
    post_type,
    post_level,
    status
FROM sys_post
WHERE post_code = 'dept_manager'
  AND status = 1
  AND deleted = 0
LIMIT 1;
```

#### 查询用户当前有效岗位

该查询用于用户详情、登录上下文、权限初始化和任职信息展示。

```sql
SELECT
    up.id AS user_post_id,
    up.user_id,
    d.id AS dept_id,
    d.dept_code,
    d.dept_name,
    p.id AS post_id,
    p.post_code,
    p.post_name,
    p.post_type,
    p.post_level,
    up.main_flag,
    up.relation_type,
    up.start_date,
    up.end_date
FROM sys_user_post up
INNER JOIN sys_dept d ON d.id = up.dept_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE up.user_id = 10001
  AND up.status = 1
  AND up.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
  AND (up.start_date IS NULL OR up.start_date <= CURRENT_DATE)
  AND (up.end_date IS NULL OR up.end_date >= CURRENT_DATE)
ORDER BY up.main_flag DESC, p.post_level DESC, d.level_no ASC;
```

#### 查询用户主岗位

该查询用于获取用户默认组织身份。主岗位通常参与默认数据权限、审批人展示和用户卡片展示。

```sql
SELECT
    up.user_id,
    d.id AS dept_id,
    d.dept_name,
    p.id AS post_id,
    p.post_code,
    p.post_name,
    p.post_level
FROM sys_user_post up
INNER JOIN sys_dept d ON d.id = up.dept_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE up.user_id = 10001
  AND up.main_flag = 1
  AND up.status = 1
  AND up.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
  AND (up.start_date IS NULL OR up.start_date <= CURRENT_DATE)
  AND (up.end_date IS NULL OR up.end_date >= CURRENT_DATE)
LIMIT 1;
```

#### 查询部门下的岗位任职人员

该查询用于部门人员管理页面。可以查询某个部门下所有岗位任职人员。

```sql
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    d.id AS dept_id,
    d.dept_name,
    p.id AS post_id,
    p.post_code,
    p.post_name,
    up.main_flag,
    up.relation_type
FROM sys_user_post up
INNER JOIN sys_user u ON u.id = up.user_id
INNER JOIN sys_dept d ON d.id = up.dept_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE up.dept_id = 10
  AND up.status = 1
  AND up.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
ORDER BY p.post_level DESC, up.main_flag DESC, u.id ASC;
```

#### 查询某部门指定岗位人员

该查询适合审批流查找“某部门经理”“某部门财务负责人”“某部门 HR 专员”。

```sql
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    u.mobile,
    u.email,
    d.id AS dept_id,
    d.dept_name,
    p.post_code,
    p.post_name
FROM sys_user_post up
INNER JOIN sys_user u ON u.id = up.user_id
INNER JOIN sys_dept d ON d.id = up.dept_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE up.dept_id = 10
  AND p.post_code = 'dept_manager'
  AND up.status = 1
  AND up.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
  AND (up.start_date IS NULL OR up.start_date <= CURRENT_DATE)
  AND (up.end_date IS NULL OR up.end_date >= CURRENT_DATE)
ORDER BY up.main_flag DESC, p.post_level DESC;
```

#### 查询某岗位下的所有用户

该查询用于查询所有部门经理、所有销售专员、所有财务人员等业务场景。

```sql
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    d.id AS dept_id,
    d.dept_name,
    p.id AS post_id,
    p.post_code,
    p.post_name,
    up.main_flag,
    up.relation_type
FROM sys_user_post up
INNER JOIN sys_user u ON u.id = up.user_id
INNER JOIN sys_dept d ON d.id = up.dept_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE p.post_code = 'sales_staff'
  AND up.status = 1
  AND up.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
  AND d.status = 1
  AND d.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
ORDER BY d.tree_path ASC, u.id ASC;
```

#### 查询部门及下级部门中的指定岗位人员

该查询适合区域审批、组织范围通知、跨部门负责人查找等场景。

```sql
SELECT DISTINCT
    u.id AS user_id,
    u.username,
    u.nickname,
    d.id AS dept_id,
    d.dept_name,
    p.post_code,
    p.post_name
FROM sys_dept d
INNER JOIN sys_user_post up ON up.dept_id = d.id
INNER JOIN sys_user u ON u.id = up.user_id
INNER JOIN sys_post p ON p.id = up.post_id
WHERE d.tree_path LIKE '/1/10/%'
  AND p.post_code = 'dept_manager'
  AND d.deleted = 0
  AND up.status = 1
  AND up.deleted = 0
  AND u.status = 1
  AND u.deleted = 0
  AND p.status = 1
  AND p.deleted = 0
ORDER BY d.tree_path ASC, p.post_level DESC;
```

#### 查询岗位派生角色

该查询用于根据岗位计算用户应该获得的角色集合。实际项目中可以在用户任职变更后同步用户角色，也可以登录时动态合并岗位角色。

```sql
SELECT DISTINCT
    r.id AS role_id,
    r.role_code,
    r.role_name
FROM sys_user_post up
INNER JOIN sys_post_role pr ON pr.post_id = up.post_id
INNER JOIN sys_role r ON r.id = pr.role_id
WHERE up.user_id = 10001
  AND up.status = 1
  AND up.deleted = 0
  AND pr.deleted = 0
  AND r.status = 1
  AND r.deleted = 0
  AND (up.start_date IS NULL OR up.start_date <= CURRENT_DATE)
  AND (up.end_date IS NULL OR up.end_date >= CURRENT_DATE)
ORDER BY r.role_code ASC;
```

#### 查询用户岗位变更历史

该查询用于人事记录、审计追溯和历史任职展示。

```sql
SELECT
    h.id,
    h.user_id,
    d.dept_name,
    p.post_name,
    h.main_flag,
    h.relation_type,
    h.change_type,
    h.start_date,
    h.end_date,
    h.change_reason,
    h.create_time
FROM sys_user_post_history h
INNER JOIN sys_dept d ON d.id = h.dept_id
INNER JOIN sys_post p ON p.id = h.post_id
WHERE h.user_id = 10001
ORDER BY h.create_time DESC, h.id DESC;
```

### 常用写入

常用写入主要包括创建岗位、修改岗位、绑定岗位角色、给用户分配岗位、调整主岗位、调岗、离岗和禁用岗位。涉及用户任职变更时，建议在事务中同时处理当前关系和历史记录。

#### 创建岗位

创建岗位时应保证岗位编码稳定。岗位编码一旦被审批流、规则引擎或外部系统引用，不建议随意修改。

```sql
INSERT INTO sys_post (
    id,
    post_code,
    post_name,
    post_type,
    post_level,
    post_category,
    duty_desc,
    status,
    sort_no,
    remark,
    create_by,
    update_by
) VALUES (
    100001,
    'dept_manager',
    '部门经理',
    1,
    5,
    'manage',
    '负责部门管理、审批和业务协调',
    1,
    1,
    '通用管理岗位',
    1,
    1
);
```

#### 修改岗位基础信息

修改岗位名称、类型、等级和职责描述时，不影响用户岗位关系。岗位编码不建议频繁修改。

```sql
UPDATE sys_post
SET post_name = '部门负责人',
    post_type = 1,
    post_level = 6,
    duty_desc = '负责部门管理、审批决策和跨部门协同',
    update_by = 1
WHERE id = 100001
  AND deleted = 0;
```

#### 绑定岗位角色

岗位绑定角色后，用户获得该岗位时可以派生对应角色。授权方式需要结合系统策略决定是“动态计算”还是“同步写入用户角色表”。

```sql
INSERT INTO sys_post_role (
    id,
    post_id,
    role_id,
    create_by
) VALUES
    (110001, 100001, 20001, 1),
    (110002, 100001, 20002, 1);
```

#### 重建岗位角色关系

修改岗位对应角色时，建议先逻辑删除旧关系，再批量插入新关系。该操作需要放在事务中。

```sql
START TRANSACTION;

UPDATE sys_post_role
SET deleted = 1
WHERE post_id = 100001
  AND deleted = 0;

INSERT INTO sys_post_role (
    id,
    post_id,
    role_id,
    create_by
) VALUES
    (110003, 100001, 20001, 1),
    (110004, 100001, 20003, 1);

COMMIT;
```

#### 给用户分配主岗位

分配主岗位时，应先取消用户原有主岗位，再插入新的主岗位关系。该操作必须放在事务中。

```sql
START TRANSACTION;

UPDATE sys_user_post
SET main_flag = 0,
    update_by = 1
WHERE user_id = 10001
  AND main_flag = 1
  AND deleted = 0;

INSERT INTO sys_user_post (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    start_date,
    end_date,
    status,
    create_by,
    update_by
) VALUES (
    120001,
    10001,
    10,
    100001,
    1,
    1,
    CURRENT_DATE,
    NULL,
    1,
    1,
    1
);

INSERT INTO sys_user_post_history (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    change_type,
    start_date,
    end_date,
    change_reason,
    create_by
) VALUES (
    130001,
    10001,
    10,
    100001,
    1,
    1,
    1,
    CURRENT_DATE,
    NULL,
    '分配主岗位',
    1
);

COMMIT;
```

#### 给用户添加兼任岗位

兼任岗位不应影响用户主岗位。`main_flag` 设置为 `0`，`relation_type` 设置为兼任。

```sql
INSERT INTO sys_user_post (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    start_date,
    end_date,
    status,
    create_by,
    update_by
) VALUES (
    120002,
    10001,
    20,
    100002,
    0,
    2,
    CURRENT_DATE,
    NULL,
    1,
    1,
    1
);
```

#### 用户调岗

调岗通常需要结束旧岗位关系，并新增新岗位关系。为了保留历史，建议不要直接覆盖旧关系。

```sql
START TRANSACTION;

UPDATE sys_user_post
SET end_date = CURRENT_DATE,
    status = 0,
    update_by = 1
WHERE user_id = 10001
  AND main_flag = 1
  AND status = 1
  AND deleted = 0;

INSERT INTO sys_user_post (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    start_date,
    end_date,
    status,
    create_by,
    update_by
) VALUES (
    120003,
    10001,
    30,
    100003,
    1,
    1,
    CURRENT_DATE,
    NULL,
    1,
    1,
    1
);

INSERT INTO sys_user_post_history (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    change_type,
    start_date,
    end_date,
    change_reason,
    create_by
) VALUES (
    130002,
    10001,
    30,
    100003,
    1,
    1,
    2,
    CURRENT_DATE,
    NULL,
    '用户主岗位调整',
    1
);

COMMIT;
```

#### 用户离岗

离岗时可以结束用户岗位关系，而不是删除岗位关系。这样可以保留任职轨迹。

```sql
START TRANSACTION;

UPDATE sys_user_post
SET end_date = CURRENT_DATE,
    status = 0,
    update_by = 1
WHERE user_id = 10001
  AND dept_id = 10
  AND post_id = 100001
  AND status = 1
  AND deleted = 0;

INSERT INTO sys_user_post_history (
    id,
    user_id,
    dept_id,
    post_id,
    main_flag,
    relation_type,
    change_type,
    start_date,
    end_date,
    change_reason,
    create_by
) VALUES (
    130003,
    10001,
    10,
    100001,
    1,
    1,
    3,
    NULL,
    CURRENT_DATE,
    '用户离岗',
    1
);

COMMIT;
```

#### 禁用岗位

禁用岗位不会删除用户岗位关系，但该岗位不再参与有效岗位查询、审批人查找和岗位角色派生。

```sql
UPDATE sys_post
SET status = 0,
    update_by = 1
WHERE id = 100001
  AND deleted = 0;
```

如果业务要求禁用岗位后同步禁用用户任职关系，可以按岗位批量处理。

```sql
UPDATE sys_user_post
SET status = 0,
    update_by = 1
WHERE post_id = 100001
  AND deleted = 0;
```

### 常见问题

岗位模型的常见问题主要集中在岗位与角色混用、主岗位不唯一、岗位任职历史丢失、岗位派生权限不一致和审批人查找不准确。

| 问题                   | 原因                               | 建议                                                      |
| ---------------------- | ---------------------------------- | --------------------------------------------------------- |
| 岗位和角色混为一谈     | 用角色同时表达职责和权限           | 岗位表示组织职责，角色表示系统权限，两者可通过关系表关联  |
| 一个用户出现多个主岗位 | 缺少主岗位变更约束                 | 设置主岗位时先取消旧主岗位，并在事务中完成                |
| 用户调岗后历史丢失     | 直接覆盖原岗位关系                 | 结束旧关系，新增新关系，并写入岗位历史表                  |
| 审批人找不到           | 岗位编码不稳定或用户岗位失效       | 审批流使用稳定 `post_code`，并过滤有效任职时间            |
| 岗位授权不生效         | 岗位角色关系变更后缓存未清理       | 修改岗位角色后清理受影响用户权限缓存                      |
| 岗位数量膨胀           | 将部门名称写入岗位名称             | 优先设计全局岗位，通过用户岗位关系绑定部门                |
| 兼任岗位影响主身份     | 没有区分主岗位和兼任岗位           | 使用 `main_flag` 和 `relation_type` 明确区分              |
| 离岗用户仍可审批       | 查询审批人时未过滤任职状态和有效期 | 查询岗位人员时必须过滤 `status`、`start_date`、`end_date` |
| 岗位删除影响历史记录   | 物理删除岗位                       | 使用禁用或逻辑删除，保留历史引用                          |

岗位角色派生需要特别注意一致性。常见做法有两种。

| 方式     | 说明                                               | 适用场景                                     |
| -------- | -------------------------------------------------- | -------------------------------------------- |
| 动态计算 | 登录时根据用户岗位查询岗位角色，再合并用户直接角色 | 权限实时性要求较高，岗位关系变化较少         |
| 同步写入 | 用户岗位变更时，将岗位角色同步写入用户角色关系表   | 查询性能要求高，希望权限模型统一走用户角色表 |

如果采用同步写入方式，需要区分“手工分配角色”和“岗位派生角色”。否则用户离岗时可能误删手工分配的角色。可以在 `sys_user_role` 中增加 `source_type` 和 `source_id` 字段，用于标记角色来源。

```sql
ALTER TABLE sys_user_role
    ADD COLUMN source_type TINYINT NOT NULL DEFAULT 1 COMMENT '来源类型：1手工分配，2岗位派生' AFTER role_id,
    ADD COLUMN source_id BIGINT DEFAULT NULL COMMENT '来源ID，例如岗位ID' AFTER source_type;
```

### 总结

岗位模型用于表达用户在组织中的职责身份，是组织架构、审批流、权限派生和人事管理的重要基础。它不应替代角色模型，而应与角色模型协作：岗位描述职责，角色描述系统权限。

落地时应重点关注以下原则：

| 原则                 | 说明                                             |
| -------------------- | ------------------------------------------------ |
| 岗位和角色分离       | 岗位用于组织职责，角色用于权限授权               |
| 岗位编码保持稳定     | 审批流、规则引擎和外部系统应依赖 `post_code`     |
| 用户岗位关系承载任职 | 用户在哪个部门担任什么岗位，应由关系表表达       |
| 主岗位必须唯一       | 用户只能有一个当前有效主岗位                     |
| 兼任岗位独立维护     | 使用 `relation_type` 区分正式、兼任、代理、临时  |
| 任职有效期必须过滤   | 查询当前岗位人员时应过滤开始日期、结束日期和状态 |
| 调岗保留历史         | 不建议直接覆盖岗位关系，应写入岗位历史记录       |
| 岗位可派生角色       | 岗位与角色通过关系表关联，不要直接混用           |
| 授权变更清理缓存     | 岗位角色变化后应清理相关用户权限缓存             |

该模型适合作为组织职责和任职关系的基础设计。与用户-角色-权限模型、数据权限模型、组织架构模型组合后，可以支撑大部分后台管理系统、OA、ERP、CRM 和企业级业务系统的权限与组织管理需求。