-- ======================================================
-- Quantum Backend 数据库初始化脚本
-- 数据库：PostgreSQL 15+
-- 字符集：UTF-8
-- ======================================================

-- ==================== 系统管理模块 ====================

-- 1. 用户表
CREATE TABLE sys_user
(
    id          BIGINT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    avatar      VARCHAR(255),
    sex         INT       DEFAULT 0,
    dept_id     BIGINT,
    status      INT       DEFAULT 1,
    data_scope  INT       DEFAULT 5,
    login_ip       VARCHAR(128),
    login_location VARCHAR(255),
    login_date     TIMESTAMP,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     INT       DEFAULT 0,
    version     BIGINT    DEFAULT 0,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

COMMENT
ON TABLE sys_user IS '用户表';
COMMENT
ON COLUMN sys_user.id IS '用户ID';
COMMENT
ON COLUMN sys_user.username IS '用户名';
COMMENT
ON COLUMN sys_user.password IS '密码';
COMMENT
ON COLUMN sys_user.nickname IS '昵称';
COMMENT
ON COLUMN sys_user.email IS '邮箱';
COMMENT
ON COLUMN sys_user.phone IS '手机号';
COMMENT
ON COLUMN sys_user.avatar IS '头像';
COMMENT
ON COLUMN sys_user.sex IS '性别（0-未知，1-男，2-女）';
COMMENT
ON COLUMN sys_user.dept_id IS '部门ID';
COMMENT
ON COLUMN sys_user.status IS '状态（0-禁用，1-正常）';
COMMENT
ON COLUMN sys_user.data_scope IS '数据权限（1-全部 2-本部门 3-本部门及子部门 4-自定义 5-仅本人）';
COMMENT
ON COLUMN sys_user.login_ip IS '最后登录IP';
COMMENT
ON COLUMN sys_user.login_location IS '最后登录地点';
COMMENT
ON COLUMN sys_user.login_date IS '最后登录时间';
COMMENT
ON COLUMN sys_user.remark IS '备注';
COMMENT
ON COLUMN sys_user.deleted IS '逻辑删除（0-未删除，1-已删除）';
COMMENT
ON COLUMN sys_user.version IS '乐观锁版本号';

CREATE INDEX idx_sys_user_dept ON sys_user (dept_id);
CREATE INDEX idx_sys_user_status ON sys_user (status);

-- 2. 部门表
CREATE TABLE sys_dept
(
    id          BIGINT PRIMARY KEY,
    parent_id   BIGINT       DEFAULT 0,
    ancestors   VARCHAR(500) DEFAULT '',
    dept_name   VARCHAR(50) NOT NULL,
    order_num   INT          DEFAULT 0,
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    status      INT          DEFAULT 1,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     INT       DEFAULT 0,
    version     BIGINT       DEFAULT 0
);

COMMENT
ON TABLE sys_dept IS '部门表';
COMMENT
ON COLUMN sys_dept.id IS '部门ID';
COMMENT
ON COLUMN sys_dept.parent_id IS '父部门ID';
COMMENT
ON COLUMN sys_dept.ancestors IS '祖级列表';
COMMENT
ON COLUMN sys_dept.dept_name IS '部门名称';
COMMENT
ON COLUMN sys_dept.order_num IS '显示顺序';
COMMENT
ON COLUMN sys_dept.leader IS '负责人';
COMMENT
ON COLUMN sys_dept.phone IS '联系电话';
COMMENT
ON COLUMN sys_dept.email IS '邮箱';
COMMENT
ON COLUMN sys_dept.status IS '状态（0-停用，1-正常）';

CREATE INDEX idx_sys_dept_parent ON sys_dept (parent_id);

-- 3. 角色表
CREATE TABLE sys_role
(
    id          BIGINT PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL,
    role_key    VARCHAR(50) NOT NULL,
    order_num   INT       DEFAULT 0,
    data_scope  INT       DEFAULT 5,
    status      INT       DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted  INT       DEFAULT 0,
    version     BIGINT    DEFAULT 0,
    CONSTRAINT uk_sys_role_key UNIQUE (role_key)
);

COMMENT
ON TABLE sys_role IS '角色表';
COMMENT
ON COLUMN sys_role.id IS '角色ID';
COMMENT
ON COLUMN sys_role.role_name IS '角色名称';
COMMENT
ON COLUMN sys_role.role_key IS '角色标识';
COMMENT
ON COLUMN sys_role.order_num IS '显示顺序';
COMMENT
ON COLUMN sys_role.data_scope IS '数据权限（1-全部 2-本部门 3-本部门及子部门 4-自定义 5-仅本人）';
COMMENT
ON COLUMN sys_role.status IS '状态（0-禁用，1-正常）';
COMMENT
ON COLUMN sys_role.remark IS '备注';

-- 4. 菜单表
CREATE TABLE sys_menu
(
    id          BIGINT PRIMARY KEY,
    parent_id   BIGINT       DEFAULT 0,
    menu_name   VARCHAR(50) NOT NULL,
    path        VARCHAR(200) DEFAULT '',
    component   VARCHAR(255) DEFAULT '',
    query_param VARCHAR(255) DEFAULT '',
    perms       VARCHAR(100) DEFAULT '',
    icon        VARCHAR(100) DEFAULT '#',
    menu_type   CHAR(1)      DEFAULT '',
    order_num   INT          DEFAULT 0,
    visible     INT          DEFAULT 1,
    status      INT          DEFAULT 1,
    is_frame    INT          DEFAULT 1,
    is_cache    INT          DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     INT       DEFAULT 0,
    version     BIGINT       DEFAULT 0
);

COMMENT
ON TABLE sys_menu IS '菜单表';
COMMENT
ON COLUMN sys_menu.id IS '菜单ID';
COMMENT
ON COLUMN sys_menu.parent_id IS '父菜单ID';
COMMENT
ON COLUMN sys_menu.menu_name IS '菜单名称';
COMMENT
ON COLUMN sys_menu.path IS '路由地址';
COMMENT
ON COLUMN sys_menu.component IS '组件路径';
COMMENT
ON COLUMN sys_menu.query_param IS '路由参数';
COMMENT
ON COLUMN sys_menu.perms IS '权限标识';
COMMENT
ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT
ON COLUMN sys_menu.menu_type IS '菜单类型（M-目录，C-菜单，F-按钮）';
COMMENT
ON COLUMN sys_menu.order_num IS '显示顺序';
COMMENT
ON COLUMN sys_menu.visible IS '是否可见（0-隐藏，1-显示）';
COMMENT
ON COLUMN sys_menu.status IS '状态（0-禁用，1-正常）';
COMMENT
ON COLUMN sys_menu.is_frame IS '是否外链（0-是，1-否）';
COMMENT
ON COLUMN sys_menu.is_cache IS '是否缓存（0-缓存，1-不缓存）';

CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);

-- 5. 用户角色关联表
CREATE TABLE sys_user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT
ON TABLE sys_user_role IS '用户角色关联表';

-- 6. 角色菜单关联表
CREATE TABLE sys_role_menu
(
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT
ON TABLE sys_role_menu IS '角色菜单关联表';

-- 7. 角色部门关联表（数据权限）
CREATE TABLE sys_role_dept
(
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id)
);

COMMENT
ON TABLE sys_role_dept IS '角色部门关联表（数据权限）';

-- 8. 操作日志表
CREATE TABLE sys_oper_log
(
    oper_id        BIGINT PRIMARY KEY,
    title          VARCHAR(50)   DEFAULT '',
    business_type  INT           DEFAULT 0,
    method         VARCHAR(200)  DEFAULT '',
    request_method VARCHAR(10)   DEFAULT '',
    operator_type  INT           DEFAULT 0,
    oper_name      VARCHAR(50)   DEFAULT '',
    dept_name      VARCHAR(50)   DEFAULT '',
    oper_url       VARCHAR(255)  DEFAULT '',
    oper_ip        VARCHAR(128)  DEFAULT '',
    oper_location  VARCHAR(255)  DEFAULT '',
    oper_param     TEXT,
    json_result    TEXT,
    status         INT           DEFAULT 0,
    error_msg      VARCHAR(2000) DEFAULT '',
    oper_time      TIMESTAMP,
    cost_time      BIGINT        DEFAULT 0
);

COMMENT
ON TABLE sys_oper_log IS '操作日志表';
COMMENT
ON COLUMN sys_oper_log.oper_id IS '日志主键';
COMMENT
ON COLUMN sys_oper_log.title IS '模块标题';
COMMENT
ON COLUMN sys_oper_log.business_type IS '业务类型（0-其它 1-新增 2-修改 3-删除 4-授权 5-导出 6-导入 7-强退 8-清空）';
COMMENT
ON COLUMN sys_oper_log.method IS '方法名称';
COMMENT
ON COLUMN sys_oper_log.request_method IS '请求方式';
COMMENT
ON COLUMN sys_oper_log.operator_type IS '操作类别（0-其它 1-后台 2-手机端）';
COMMENT
ON COLUMN sys_oper_log.oper_name IS '操作人员';
COMMENT
ON COLUMN sys_oper_log.dept_name IS '部门名称';
COMMENT
ON COLUMN sys_oper_log.oper_url IS '请求URL';
COMMENT
ON COLUMN sys_oper_log.oper_ip IS '主机地址';
COMMENT
ON COLUMN sys_oper_log.oper_location IS '操作地点';
COMMENT
ON COLUMN sys_oper_log.oper_param IS '请求参数';
COMMENT
ON COLUMN sys_oper_log.json_result IS '返回参数';
COMMENT
ON COLUMN sys_oper_log.status IS '操作状态（0-正常 1-异常）';
COMMENT
ON COLUMN sys_oper_log.error_msg IS '错误消息';
COMMENT
ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT
ON COLUMN sys_oper_log.cost_time IS '消耗时间（毫秒）';

CREATE INDEX idx_oper_log_time ON sys_oper_log (oper_time);
CREATE INDEX idx_oper_log_user ON sys_oper_log (oper_name);
CREATE INDEX idx_oper_log_status ON sys_oper_log (status);
CREATE INDEX idx_oper_log_business ON sys_oper_log (business_type);

-- 9. 登录日志表
CREATE TABLE sys_login_log
(
    info_id        BIGINT PRIMARY KEY,
    username       VARCHAR(50)  DEFAULT '',
    ipaddr         VARCHAR(128) DEFAULT '',
    login_location VARCHAR(255) DEFAULT '',
    browser        VARCHAR(50)  DEFAULT '',
    os             VARCHAR(50)  DEFAULT '',
    status         INT          DEFAULT 0,
    msg            VARCHAR(255) DEFAULT '',
    login_time     TIMESTAMP
);

COMMENT
ON TABLE sys_login_log IS '登录日志表';
COMMENT
ON COLUMN sys_login_log.info_id IS '日志主键';
COMMENT
ON COLUMN sys_login_log.username IS '用户账号';
COMMENT
ON COLUMN sys_login_log.ipaddr IS '登录IP';
COMMENT
ON COLUMN sys_login_log.login_location IS '登录地点';
COMMENT
ON COLUMN sys_login_log.browser IS '浏览器类型';
COMMENT
ON COLUMN sys_login_log.os IS '操作系统';
COMMENT
ON COLUMN sys_login_log.status IS '登录状态（0-成功 1-失败）';
COMMENT
ON COLUMN sys_login_log.msg IS '提示消息';
COMMENT
ON COLUMN sys_login_log.login_time IS '登录时间';

CREATE INDEX idx_login_log_time ON sys_login_log (login_time);
CREATE INDEX idx_login_log_user ON sys_login_log (username);

-- 10. 字典类型表
CREATE TABLE sys_dict_type
(
    id          BIGINT PRIMARY KEY,
    dict_name   VARCHAR(100) NOT NULL,
    dict_type   VARCHAR(100) NOT NULL,
    status      INT       DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted  INT       DEFAULT 0,
    version     BIGINT    DEFAULT 0,
    CONSTRAINT uk_sys_dict_type UNIQUE (dict_type)
);

COMMENT
ON TABLE sys_dict_type IS '字典类型表';
COMMENT
ON COLUMN sys_dict_type.dict_name IS '字典名称';
COMMENT
ON COLUMN sys_dict_type.dict_type IS '字典类型';
COMMENT
ON COLUMN sys_dict_type.status IS '状态（0-停用，1-正常）';

-- 11. 字典数据表
CREATE TABLE sys_dict_data
(
    id          BIGINT PRIMARY KEY,
    dict_type   VARCHAR(100) NOT NULL,
    dict_label  VARCHAR(100) NOT NULL,
    dict_value  VARCHAR(100) NOT NULL,
    dict_sort   INT          DEFAULT 0,
    css_class   VARCHAR(100) DEFAULT '',
    list_class  VARCHAR(100) DEFAULT '',
    is_default  CHAR(1)      DEFAULT 'N',
    status      INT          DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    deleted     INT       DEFAULT 0,
    version     BIGINT       DEFAULT 0
);

COMMENT
ON TABLE sys_dict_data IS '字典数据表';
COMMENT
ON COLUMN sys_dict_data.dict_type IS '字典类型';
COMMENT
ON COLUMN sys_dict_data.dict_label IS '字典标签';
COMMENT
ON COLUMN sys_dict_data.dict_value IS '字典值';
COMMENT
ON COLUMN sys_dict_data.dict_sort IS '排序';
COMMENT
ON COLUMN sys_dict_data.css_class IS '样式属性';
COMMENT
ON COLUMN sys_dict_data.list_class IS '表格回显样式';
COMMENT
ON COLUMN sys_dict_data.is_default IS '是否默认（Y-是 N-否）';
COMMENT
ON COLUMN sys_dict_data.status IS '状态（0-停用，1-正常）';

CREATE INDEX idx_dict_data_type ON sys_dict_data (dict_type);

-- ==================== 文件管理模块 ====================

-- 12. 文件信息表
CREATE TABLE sys_file
(
    id            BIGINT PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    storage_name  VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    url           VARCHAR(500),
    size          BIGINT      DEFAULT 0,
    extension     VARCHAR(20),
    content_type  VARCHAR(100),
    md5           VARCHAR(32),
    storage_type  VARCHAR(20) DEFAULT 'local',
    bucket        VARCHAR(100),
    biz_type      VARCHAR(50),
    biz_id        BIGINT,
    upload_by     BIGINT,
    upload_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    deleted    INT         DEFAULT 0,
    version       BIGINT      DEFAULT 0
);

COMMENT
ON TABLE sys_file IS '文件信息表';
COMMENT
ON COLUMN sys_file.original_name IS '原始文件名';
COMMENT
ON COLUMN sys_file.storage_name IS '存储文件名';
COMMENT
ON COLUMN sys_file.file_path IS '文件路径';
COMMENT
ON COLUMN sys_file.url IS '访问URL';
COMMENT
ON COLUMN sys_file.size IS '文件大小（字节）';
COMMENT
ON COLUMN sys_file.extension IS '文件扩展名';
COMMENT
ON COLUMN sys_file.content_type IS 'MIME类型';
COMMENT
ON COLUMN sys_file.md5 IS '文件MD5';
COMMENT
ON COLUMN sys_file.storage_type IS '存储类型（local-本地 rustfs-RustFS）';
COMMENT
ON COLUMN sys_file.bucket IS 'Bucket名称';
COMMENT
ON COLUMN sys_file.biz_type IS '业务类型';
COMMENT
ON COLUMN sys_file.biz_id IS '业务ID';
COMMENT
ON COLUMN sys_file.upload_by IS '上传人ID';
COMMENT
ON COLUMN sys_file.upload_time IS '上传时间';

CREATE INDEX idx_sys_file_md5 ON sys_file (md5);
CREATE INDEX idx_sys_file_biz ON sys_file (biz_type, biz_id);

-- ==================== 初始化数据 ====================

-- 1. 初始化部门
INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, order_num, leader, status)
VALUES
    (0, 0, '0', 'Alpha科技', 0, '管理员', 1),
    (101, 0, '0,100', '深圳总公司', 1, '', 1),
    (102, 0, '0,100', '北京分公司', 2, '', 1),
    (103, 101, '0,100,101', '研发部门', 1, '', 1);

-- 2. 初始化用户（密码：123456；加密规则：BCrypt(strength=10) of username+password）
INSERT INTO sys_user (id, username, password, nickname, email, phone, dept_id, status, data_scope)
VALUES (1, 'admin', '$2a$10$HjRIXDA/crTT5WCbxkcZkuZ2RI9cwx2V6K086/Z9sXJZqUQevESRy', '超级管理员', 'admin@alpha.com',
        '13800000000', 0, 1, 1),
       (2, 'test', '$2a$10$Z8FuLfV3AOFclsWqKRjZOeuw9OnQ8cKDaWA5AWESfflqvV0t0RAP.', '测试用户', 'test@alpha.com',
        '13800000001', 103, 1, 5);

-- 3. 初始化角色
INSERT INTO sys_role (id, role_name, role_key, order_num, data_scope, status, remark)
VALUES (1, '超级管理员', 'admin', 1, 1, 1, '超级管理员'),
       (2, '普通角色', 'common', 2, 5, 1, '普通角色');

-- 4. 初始化用户角色关联
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1),
       (2, 2);

-- 5. 初始化菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, icon, menu_type, order_num, visible, status)
VALUES
    -- 一级菜单
    (1, 0, '系统管理', 'system', NULL, '', 'settings', 'M', 1, 1, 1),
    (2, 0, '系统监控', 'monitor', NULL, '', 'monitor-cog', 'M', 2, 1, 1),

    -- 系统管理子菜单
    (100, 1, '用户管理', 'user', 'system/user/index', 'system:user:list', 'users', 'C', 1, 1, 1),
    (101, 1, '角色管理', 'role', 'system/role/index', 'system:role:list', 'users-round', 'C', 2, 1, 1),
    (102, 1, '菜单管理', 'menu', 'system/menu/index', 'system:menu:list', 'list-tree', 'C', 3, 1, 1),
    (103, 1, '部门管理', 'dept', 'system/dept/index', 'system:dept:list', 'folder-tree', 'C', 4, 1, 1),
    (105, 1, '字典管理', 'dict', 'system/dict/index', 'system:dict:list', 'book-open-text', 'C', 6, 1, 1),
    (107, 1, '文件管理', 'file', 'system/file/index', 'system:file:query', 'files', 'C', 8, 1, 1),

    -- 系统监控子菜单
    (200, 2, '在线用户', 'online', 'monitor/online/index', 'monitor:online:list', 'badge-check', 'C', 1, 1, 1),
    (201, 2, '操作日志', 'operlog', 'monitor/operlog/index', 'monitor:operlog:list', 'logs', 'C', 2, 1, 1),
    (202, 2, '登录日志', 'loginlog', 'monitor/loginlog/index', 'monitor:loginlog:list', 'logs', 'C', 3, 1, 1),
    (203, 2, '缓存监控', 'cache', 'monitor/cache/index', 'monitor:cache:list', 'database-zap', 'C', 4, 1, 1),
    (204, 2, '服务监控', 'server', 'monitor/server/index', 'monitor:server:list', 'server-cog', 'C', 5, 1, 1),

    -- 用户管理按钮
    (1000, 100, '用户查询', '', '', 'system:user:query', '#', 'F', 1, 1, 1),
    (1001, 100, '用户新增', '', '', 'system:user:add', '#', 'F', 2, 1, 1),
    (1002, 100, '用户修改', '', '', 'system:user:edit', '#', 'F', 3, 1, 1),
    (1003, 100, '用户删除', '', '', 'system:user:remove', '#', 'F', 4, 1, 1),
    (1004, 100, '用户导出', '', '', 'system:user:export', '#', 'F', 5, 1, 1),
    (1005, 100, '用户导入', '', '', 'system:user:import', '#', 'F', 6, 1, 1),
    (1006, 100, '重置密码', '', '', 'system:user:resetPwd', '#', 'F', 7, 1, 1),

    -- 角色管理按钮
    (1100, 101, '角色查询', '', '', 'system:role:query', '#', 'F', 1, 1, 1),
    (1101, 101, '角色新增', '', '', 'system:role:add', '#', 'F', 2, 1, 1),
    (1102, 101, '角色修改', '', '', 'system:role:edit', '#', 'F', 3, 1, 1),
    (1103, 101, '角色删除', '', '', 'system:role:remove', '#', 'F', 4, 1, 1),
    (1104, 101, '角色导出', '', '', 'system:role:export', '#', 'F', 5, 1, 1),

    -- 菜单管理按钮
    (1200, 102, '菜单查询', '', '', 'system:menu:query', '#', 'F', 1, 1, 1),
    (1201, 102, '菜单新增', '', '', 'system:menu:add', '#', 'F', 2, 1, 1),
    (1202, 102, '菜单修改', '', '', 'system:menu:edit', '#', 'F', 3, 1, 1),
    (1203, 102, '菜单删除', '', '', 'system:menu:remove', '#', 'F', 4, 1, 1),

    -- 部门管理按钮
    (1300, 103, '部门查询', '', '', 'system:dept:query', '#', 'F', 1, 1, 1),
    (1301, 103, '部门新增', '', '', 'system:dept:add', '#', 'F', 2, 1, 1),
    (1302, 103, '部门修改', '', '', 'system:dept:edit', '#', 'F', 3, 1, 1),
    (1303, 103, '部门删除', '', '', 'system:dept:remove', '#', 'F', 4, 1, 1),

    -- 字典管理按钮
    (1050, 105, '字典详情', '', '', 'system:dict:detail', '#', 'F', 1, 1, 1),
    (1051, 105, '字典新增', '', '', 'system:dict:add', '#', 'F', 2, 1, 1),
    (1052, 105, '字典修改', '', '', 'system:dict:edit', '#', 'F', 3, 1, 1),
    (1053, 105, '字典删除', '', '', 'system:dict:remove', '#', 'F', 4, 1, 1),
    (1054, 105, '所有字典类型', '', '', 'system:dict:alltype', '#', 'F', 5, 1, 1),
    (1055, 105, '字典数据查询', '', '', 'system:dict:listData', '#', 'F', 6, 1, 1),
    (1056, 105, '刷新字典缓存', '', '', 'system:dict:refreshCache', '#', 'F', 7, 1, 1),

    -- 文件管理按钮
    (1070, 107, '文件查询', '', '', 'system:file:query', '#', 'F', 1, 1, 1),
    (1071, 107, '文件上传', '', '', 'system:file:upload', '#', 'F', 2, 1, 1),
    (1072, 107, '文件下载', '', '', 'system:file:download', '#', 'F', 3, 1, 1),
    (1073, 107, '文件删除', '', '', 'system:file:remove', '#', 'F', 4, 1, 1),

    -- 操作日志按钮
    (2010, 201, '操作日志详情', '', '', 'monitor:operlog:query', '#', 'F', 1, 1, 1),
    (2011, 201, '操作日志删除', '', '', 'monitor:operlog:remove', '#', 'F', 2, 1, 1),

    -- 登录日志按钮
    (2020, 202, '登录日志删除', '', '', 'monitor:loginlog:remove', '#', 'F', 1, 1, 1);

-- 6. 初始化角色菜单关联（管理员拥有所有菜单权限）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id
FROM sys_menu;

-- 普通角色只有部分权限
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (2, 1),
       (2, 100),
       (2, 1000);

-- 7. 初始化字典类型
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark)
VALUES (1, '用户性别', 'sys_user_sex', 1, '用户性别列表'),
       (2, '系统状态', 'sys_normal_disable', 1, '系统状态列表'),
       (3, '是否', 'sys_yes_no', 1, '是否列表'),
       (4, '通知类型', 'sys_notice_type', 1, '通知类型列表'),
       (5, '通知状态', 'sys_notice_status', 1, '通知状态列表'),
       (6, '操作类型', 'sys_oper_type', 1, '操作类型列表'),
       (7, '系统开关', 'sys_common_status', 1, '系统开关列表');

-- 8. 初始化字典数据
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, status)
VALUES (1, 'sys_user_sex', '男', '1', 1, 1),
       (2, 'sys_user_sex', '女', '2', 2, 1),
       (3, 'sys_user_sex', '未知', '0', 3, 1),
       (4, 'sys_normal_disable', '正常', '1', 1, 1),
       (5, 'sys_normal_disable', '停用', '0', 2, 1),
       (6, 'sys_yes_no', '是', 'Y', 1, 1),
       (7, 'sys_yes_no', '否', 'N', 2, 1),
       (8, 'sys_oper_type', '其他', '0', 0, 1),
       (9, 'sys_oper_type', '新增', '1', 1, 1),
       (10, 'sys_oper_type', '修改', '2', 2, 1),
       (11, 'sys_oper_type', '删除', '3', 3, 1),
       (12, 'sys_oper_type', '授权', '4', 4, 1),
       (13, 'sys_oper_type', '导出', '5', 5, 1),
       (14, 'sys_oper_type', '导入', '6', 6, 1),
       (15, 'sys_oper_type', '强退', '7', 7, 1),
       (16, 'sys_common_status', '成功', '0', 1, 1),
       (17, 'sys_common_status', '失败', '1', 2, 1);
