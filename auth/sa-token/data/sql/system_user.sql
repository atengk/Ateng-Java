-- 创建 sys_permission 表
DROP TABLE IF EXISTS "sys_permission";
CREATE TABLE "sys_permission"
(
    "permission_id"   serial PRIMARY KEY, -- 权限ID，主键，自增
    "permission_name" varchar(50) NOT NULL, -- 权限名称
    "description"     varchar(200), -- 权限描述
    "created_time"    timestamp(6) DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间
    "updated_time"    timestamp(6) DEFAULT CURRENT_TIMESTAMP -- 更新时间，默认当前时间
);
COMMENT ON COLUMN "sys_permission"."permission_id" IS '权限ID，主键，自增';
COMMENT ON COLUMN "sys_permission"."permission_name" IS '权限名称';
COMMENT ON COLUMN "sys_permission"."description" IS '权限描述';
COMMENT ON COLUMN "sys_permission"."created_time" IS '创建时间';
COMMENT ON COLUMN "sys_permission"."updated_time" IS '更新时间';
COMMENT ON TABLE "sys_permission" IS '存储系统中的权限信息';

-- 插入数据
INSERT INTO "sys_permission" (permission_name, description)
VALUES
  ('*', '所有权限'),
  ('system.user.add', '系统设置-用户设置-新增'),
  ('system.user.get', '系统设置-用户设置-查看'),
  ('system.user.delete', '系统设置-用户设置-删除'),
  ('system.user.update', '系统设置-用户设置-修改');

-- 创建 sys_role 表
DROP TABLE IF EXISTS "sys_role";
CREATE TABLE "sys_role"
(
    "role_id"      serial PRIMARY KEY, -- 角色ID，主键，自增
    "role_name"    varchar(50) NOT NULL, -- 角色名称
    "description"  varchar(200), -- 角色描述
    "created_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间
    "updated_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP -- 更新时间，默认当前时间
);
COMMENT ON COLUMN "sys_role"."role_id" IS '角色ID，主键，自增';
COMMENT ON COLUMN "sys_role"."role_name" IS '角色名称';
COMMENT ON COLUMN "sys_role"."description" IS '角色描述';
COMMENT ON COLUMN "sys_role"."created_time" IS '创建时间';
COMMENT ON COLUMN "sys_role"."updated_time" IS '更新时间';
COMMENT ON TABLE "sys_role" IS '存储系统中的角色信息';

-- 插入数据
INSERT INTO "sys_role" (role_name, description)
VALUES
  ('super-admin', '超级管理员'),
  ('admin', '管理员'),
  ('user', '普通用户');

-- 创建 sys_role_permission 表，角色与权限的多对多关系
DROP TABLE IF EXISTS "sys_role_permission";
CREATE TABLE "sys_role_permission"
(
    "role_id"       int NOT NULL, -- 角色ID
    "permission_id" int NOT NULL, -- 权限ID
    PRIMARY KEY ("role_id", "permission_id") -- 角色和权限的复合主键
);
COMMENT ON COLUMN "sys_role_permission"."role_id" IS '角色ID';
COMMENT ON COLUMN "sys_role_permission"."permission_id" IS '权限ID';
COMMENT ON TABLE "sys_role_permission" IS '实现角色与权限之间的多对多关系';

-- 插入数据
INSERT INTO "sys_role_permission" ("role_id", "permission_id")
VALUES
  (1, 1), -- 超级管理员拥有所有权限
  (2, 2), (2, 3), (2, 4), (2, 5), -- 管理员拥有部分权限
  (3, 3); -- 普通用户只拥有查看权限

-- 创建 sys_user 表
DROP TABLE IF EXISTS "sys_user";
CREATE TABLE "sys_user"
(
    "user_id"      serial PRIMARY KEY, -- 用户ID，主键，自增
    "user_name"    varchar(30) NOT NULL, -- 用户名
    "nick_name"    varchar(30) NOT NULL, -- 用户昵称
    "password"     varchar(100) NOT NULL, -- 用户密码（加密后）
    "sex"          varchar(10), -- 性别
    "email"        varchar(50), -- 用户邮箱
    "phone_number" varchar(20), -- 手机号码
    "create_time"  timestamp(6) DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    "update_time"  timestamp(6) DEFAULT CURRENT_TIMESTAMP -- 修改时间
);
COMMENT ON COLUMN "sys_user"."user_id" IS '用户ID，主键，自增';
COMMENT ON COLUMN "sys_user"."user_name" IS '用户名';
COMMENT ON COLUMN "sys_user"."nick_name" IS '用户昵称';
COMMENT ON COLUMN "sys_user"."password" IS '用户密码（加密后）';
COMMENT ON COLUMN "sys_user"."sex" IS '性别';
COMMENT ON COLUMN "sys_user"."email" IS '用户邮箱';
COMMENT ON COLUMN "sys_user"."phone_number" IS '手机号码';
COMMENT ON COLUMN "sys_user"."create_time" IS '创建时间';
COMMENT ON COLUMN "sys_user"."update_time" IS '修改时间';
COMMENT ON TABLE "sys_user" IS '存储用户的基本信息';

-- 插入数据
INSERT INTO "sys_user" (user_name, nick_name, password, sex, email, phone_number)
VALUES
  ('ateng', '阿腾', '$2a$10$mSl7i4wOGibcFeF25e.Ra.eY5yi22rXfOwqa5r4mw1p60xfMMNAPe', '男', '2385569970@qq.com', '17623062936'),
  ('admin', '管理员', '$2a$10$Z2dd/HCSu0KG5FavJph0J.g3J8wVuvIkcO3wyflVu3pSka3ZnJXC.', '男', '', ''),
  ('kongyu', '孔余', '$2a$10$KKeMn5w5K9qCIx79uF1.auzNfmtqqJH0Bj2l9SqG5UStL3AlLImx2', '男', '', '');

-- 创建 sys_user_role 表，用户与角色的多对多关系
DROP TABLE IF EXISTS "sys_user_role";
CREATE TABLE "sys_user_role"
(
    "user_id" int NOT NULL, -- 用户ID
    "role_id" int NOT NULL, -- 角色ID
    PRIMARY KEY ("user_id", "role_id") -- 用户和角色的复合主键
);
COMMENT ON COLUMN "sys_user_role"."user_id" IS '用户ID';
COMMENT ON COLUMN "sys_user_role"."role_id" IS '角色ID';
COMMENT ON TABLE "sys_user_role" IS '实现用户与角色之间的多对多关系';

-- 插入数据
INSERT INTO "sys_user_role" ("user_id", "role_id")
VALUES
  (1, 1), -- 用户1 (ateng) 是超级管理员
  (2, 2), -- 用户2 (admin) 是管理员
  (3, 3); -- 用户3 (kongyu) 是普通用户

-- 外键约束

-- sys_role_permission 外键约束
ALTER TABLE "sys_role_permission"
    ADD CONSTRAINT "sys_role_permission_permission_id_fkey" FOREIGN KEY ("permission_id") REFERENCES "sys_permission" ("permission_id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "sys_role_permission"
    ADD CONSTRAINT "sys_role_permission_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "sys_role" ("role_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- sys_user_role 外键约束
ALTER TABLE "sys_user_role"
    ADD CONSTRAINT "sys_user_role_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "sys_role" ("role_id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "sys_user_role"
    ADD CONSTRAINT "sys_user_role_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "sys_user" ("user_id") ON DELETE CASCADE ON UPDATE NO ACTION;
