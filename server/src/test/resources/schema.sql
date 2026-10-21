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

drop table if exists chat_group;
create table chat_group (
    id         bigint auto_increment primary key,
    name       varchar(100) not null,
    owner_id   int not null,
    created_at timestamp(6) not null default current_timestamp
);

drop table if exists chat_member;
create table chat_member (
    group_id         bigint not null,
    user_id          int not null,
    joined_at        timestamp(6) not null default current_timestamp,
    joined_after_id  bigint not null default 0,
    history_start_id bigint not null default 0
);

drop table if exists chat_message;
create table chat_message (
    id                bigint auto_increment primary key,
    group_id          bigint not null,
    sender_id         int not null,
    sender_name       varchar(100) not null,
    content           varchar(2000) not null,
    client_message_id varchar(64) not null,
    sent_at           timestamp(6) not null default current_timestamp
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

insert into sys_menu(id, parent_id, name, type, path, permission, sort_order, enabled, builtin) values
    (10, null, '用户管理', 'MENU', '/user', 'user:read', 10, 1, 1),
    (40, null, '群组聊天', 'MENU', '/chat', 'chat:access', 40, 1, 1);

-- member 角色有群聊权限、没有用户管理权限
insert into sys_role_menu(role_id, menu_id) values (2, 40);

-- 群 1：admin 和 manager 都在；群 2：只有 manager 在
insert into chat_group(id, name, owner_id) values (1, '测试群', 50), (2, '别人的群', 51);
-- 显式插入 id 后要把自增序列推到后面，否则新建群会和已有 id 撞主键
alter table chat_group alter column id restart with 100;
insert into chat_member(group_id, user_id) values (1, 50), (1, 51), (2, 51);

insert into chat_message(group_id, sender_id, sender_name, content, client_message_id) values
    (1, 50, '管理员', '你好', 'seed-1'),
    (1, 51, '管理员二', '收到', 'seed-2');
