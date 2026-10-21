-- 集成测试用的 stu 表结构（生产环境这些表属于 stu 项目，测试库里需要自己造出来）。
-- 注意：USER 是 H2 2.x 的关键字，测试库 URL 上加了 NON_KEYWORDS=USER，所以表名可以不加引号。

drop table if exists user;
create table user (
    id     int auto_increment primary key,
    name   varchar(100),
    account varchar(100) not null unique,
    passwd varchar(100) not null
);

drop table if exists sys_role;
create table sys_role (
    id          bigint auto_increment primary key,
    name        varchar(100) not null,
    code        varchar(64) not null unique,
    description varchar(255),
    enabled     tinyint not null default 1,
    builtin     tinyint not null default 0
);

drop table if exists sys_user_role;
create table sys_user_role (
    user_id int not null,
    role_id bigint not null
);

drop table if exists sys_menu;
create table sys_menu (
    id          bigint auto_increment primary key,
    parent_id   bigint,
    name        varchar(100) not null,
    type        varchar(20) not null,
    path        varchar(200),
    permission  varchar(100),
    sort_order  int not null default 0,
    enabled     tinyint not null default 1,
    builtin     tinyint not null default 0
);

drop table if exists sys_role_menu;
create table sys_role_menu (
    role_id bigint not null,
    menu_id bigint not null
);

-- 密码均为 123456（与 stu 相同的三重 MD5）
insert into user(id, name, account, passwd) values
    (50, '管理员', 'admin', 'c56d0e9a7ccec67b4ea131655038d604'),
    (51, '管理员二', 'manager', 'c56d0e9a7ccec67b4ea131655038d604'),
    (52, '只读用户', 'viewer', 'c56d0e9a7ccec67b4ea131655038d604');

insert into sys_role(id, name, code, description, enabled, builtin) values
    (1, '超级管理员', 'super_admin', '拥有全部系统权限', 1, 1),
    (2, '普通用户', 'member', '默认角色', 1, 1);

insert into sys_user_role(user_id, role_id) values (50, 1), (51, 2), (52, 2);
