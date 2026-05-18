/*
 Navicat Premium Dump SQL

 Source Server         : 100.107.62.19——PG
 Source Server Type    : PostgreSQL
 Source Server Version : 180003 (180003)
 Source Host           : 100.107.62.19:15432
 Source Catalog        : wuxi_civil
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180003 (180003)
 File Encoding         : 65001

 Date: 18/05/2026 13:03:48
*/


-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_config";
CREATE TABLE "public"."sys_config" (
  "id" int8 NOT NULL,
  "config_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "config_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "config_value" varchar(500) COLLATE "pg_catalog"."default",
  "config_type" char(1) COLLATE "pg_catalog"."default" DEFAULT 'N'::bpchar,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_config" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_config"."id" IS '参数主键';
COMMENT ON COLUMN "public"."sys_config"."config_name" IS '参数名称';
COMMENT ON COLUMN "public"."sys_config"."config_key" IS '参数键名';
COMMENT ON COLUMN "public"."sys_config"."config_value" IS '参数键值';
COMMENT ON COLUMN "public"."sys_config"."config_type" IS '系统内置（Y-是 N-否）';
COMMENT ON COLUMN "public"."sys_config"."remark" IS '备注';
COMMENT ON TABLE "public"."sys_config" IS '系统参数配置表';

-- ----------------------------
-- Records of sys_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dept";
CREATE TABLE "public"."sys_dept" (
  "id" int8 NOT NULL,
  "parent_id" int8 DEFAULT 0,
  "ancestors" varchar(500) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "dept_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "order_num" int4 DEFAULT 0,
  "leader" varchar(50) COLLATE "pg_catalog"."default",
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "email" varchar(100) COLLATE "pg_catalog"."default",
  "status" int4 DEFAULT 1,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_dept" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_dept"."id" IS '部门ID';
COMMENT ON COLUMN "public"."sys_dept"."parent_id" IS '父部门ID';
COMMENT ON COLUMN "public"."sys_dept"."ancestors" IS '祖级列表';
COMMENT ON COLUMN "public"."sys_dept"."dept_name" IS '部门名称';
COMMENT ON COLUMN "public"."sys_dept"."order_num" IS '显示顺序';
COMMENT ON COLUMN "public"."sys_dept"."leader" IS '负责人';
COMMENT ON COLUMN "public"."sys_dept"."phone" IS '联系电话';
COMMENT ON COLUMN "public"."sys_dept"."email" IS '邮箱';
COMMENT ON COLUMN "public"."sys_dept"."status" IS '状态（0-停用，1-正常）';
COMMENT ON TABLE "public"."sys_dept" IS '部门表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_dept" ("id", "parent_id", "ancestors", "dept_name", "order_num", "leader", "phone", "email", "status", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (0, 0, '0', '无锡市', 0, '管理员', '13800000000', '123@alpha.com', 1, '2026-04-13 02:43:21.633577', NULL, '2026-05-18 12:53:05.762827', 1, 0, 1);
INSERT INTO "public"."sys_dept" ("id", "parent_id", "ancestors", "dept_name", "order_num", "leader", "phone", "email", "status", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (101, 0, '0', '湖滨区', 1, '管理员', '13800000000', '123@alpha.com', 1, '2026-04-13 02:43:21.633577', NULL, '2026-05-18 12:53:19.460334', 1, 0, 3);
INSERT INTO "public"."sys_dept" ("id", "parent_id", "ancestors", "dept_name", "order_num", "leader", "phone", "email", "status", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (102, 0, '0,100', '梁溪区', 2, '管理员', '13800000000', '123@alpha.com', 1, '2026-04-13 02:43:21.633577', NULL, '2026-05-18 12:53:34.233355', 1, 0, 1);
INSERT INTO "public"."sys_dept" ("id", "parent_id", "ancestors", "dept_name", "order_num", "leader", "phone", "email", "status", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (413845152638095360, 0, '0', '惠山区', 3, '管理员', '13800000000', '123@alpha.com', 1, '2026-05-18 12:53:53.390495', 1, '2026-05-18 12:53:57.568529', 1, 0, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_data";
CREATE TABLE "public"."sys_dict_data" (
  "id" int8 NOT NULL,
  "dict_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_label" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_value" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_sort" int4 DEFAULT 0,
  "css_class" varchar(100) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "list_class" varchar(100) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "is_default" char(1) COLLATE "pg_catalog"."default" DEFAULT 'N'::bpchar,
  "status" int4 DEFAULT 1,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_dict_data" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_dict_data"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_label" IS '字典标签';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_value" IS '字典值';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_sort" IS '排序';
COMMENT ON COLUMN "public"."sys_dict_data"."css_class" IS '样式属性';
COMMENT ON COLUMN "public"."sys_dict_data"."list_class" IS '表格回显样式';
COMMENT ON COLUMN "public"."sys_dict_data"."is_default" IS '是否默认（Y-是 N-否）';
COMMENT ON COLUMN "public"."sys_dict_data"."status" IS '状态（0-停用，1-正常）';
COMMENT ON TABLE "public"."sys_dict_data" IS '字典数据表';

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (2, 'sys_user_sex', '女', '2', 2, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (3, 'sys_user_sex', '未知', '0', 3, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (4, 'sys_normal_disable', '正常', '1', 1, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (5, 'sys_normal_disable', '停用', '0', 2, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (6, 'sys_yes_no', '是', 'Y', 1, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (7, 'sys_yes_no', '否', 'N', 2, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (8, 'sys_oper_type', '其他', '0', 0, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (9, 'sys_oper_type', '新增', '1', 1, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (10, 'sys_oper_type', '修改', '2', 2, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (11, 'sys_oper_type', '删除', '3', 3, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (12, 'sys_oper_type', '授权', '4', 4, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (13, 'sys_oper_type', '导出', '5', 5, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (14, 'sys_oper_type', '导入', '6', 6, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (15, 'sys_oper_type', '强退', '7', 7, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (16, 'sys_common_status', '成功', '0', 1, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (17, 'sys_common_status', '失败', '1', 2, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-13 02:44:26.759722', NULL, 0, 0);
INSERT INTO "public"."sys_dict_data" ("id", "dict_type", "dict_label", "dict_value", "dict_sort", "css_class", "list_class", "is_default", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1, 'sys_user_sex', '男', '1', 1, '', '', 'N', 1, NULL, '2026-04-13 02:44:26.759722', NULL, '2026-04-20 17:54:25.673856', 1, 0, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_type";
CREATE TABLE "public"."sys_dict_type" (
  "id" int8 NOT NULL,
  "dict_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int4 DEFAULT 1,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0,
  "business" varchar(40) COLLATE "pg_catalog"."default" DEFAULT 'admin'::character varying
)
;
ALTER TABLE "public"."sys_dict_type" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_dict_type"."dict_name" IS '字典名称';
COMMENT ON COLUMN "public"."sys_dict_type"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."sys_dict_type"."status" IS '状态（0-停用，1-正常）';
COMMENT ON COLUMN "public"."sys_dict_type"."business" IS '业务类型(admin 管理员，talnet 人才)';
COMMENT ON TABLE "public"."sys_dict_type" IS '字典类型表';

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (2, '系统状态', 'sys_normal_disable', 1, '系统状态列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (3, '是否', 'sys_yes_no', 1, '是否列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (4, '通知类型', 'sys_notice_type', 1, '通知类型列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (5, '通知状态', 'sys_notice_status', 1, '通知状态列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (6, '操作类型', 'sys_oper_type', 1, '操作类型列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (7, '系统开关', 'sys_common_status', 1, '系统开关列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-13 02:44:26.744728', NULL, 0, 0, 'admin');
INSERT INTO "public"."sys_dict_type" ("id", "dict_name", "dict_type", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "business") VALUES (1, '用户性别', 'sys_user_sex', 1, '用户性别列表', '2026-04-13 02:44:26.744728', NULL, '2026-04-20 17:54:29.187165', 1, 0, 1, 'admin');
COMMIT;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_file";
CREATE TABLE "public"."sys_file" (
  "id" int8 NOT NULL,
  "original_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "storage_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "file_path" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "url" varchar(500) COLLATE "pg_catalog"."default",
  "size" int8 DEFAULT 0,
  "extension" varchar(20) COLLATE "pg_catalog"."default",
  "content_type" varchar(100) COLLATE "pg_catalog"."default",
  "md5" varchar(32) COLLATE "pg_catalog"."default",
  "storage_type" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'local'::character varying,
  "bucket" varchar(100) COLLATE "pg_catalog"."default",
  "biz_type" varchar(50) COLLATE "pg_catalog"."default",
  "biz_id" int8,
  "upload_by" int8,
  "upload_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_file" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_file"."original_name" IS '原始文件名';
COMMENT ON COLUMN "public"."sys_file"."storage_name" IS '存储文件名';
COMMENT ON COLUMN "public"."sys_file"."file_path" IS '文件路径';
COMMENT ON COLUMN "public"."sys_file"."url" IS '访问URL';
COMMENT ON COLUMN "public"."sys_file"."size" IS '文件大小（字节）';
COMMENT ON COLUMN "public"."sys_file"."extension" IS '文件扩展名';
COMMENT ON COLUMN "public"."sys_file"."content_type" IS 'MIME类型';
COMMENT ON COLUMN "public"."sys_file"."md5" IS '文件MD5';
COMMENT ON COLUMN "public"."sys_file"."storage_type" IS '存储类型（local-本地 rustfs-RustFS）';
COMMENT ON COLUMN "public"."sys_file"."bucket" IS 'Bucket名称';
COMMENT ON COLUMN "public"."sys_file"."biz_type" IS '业务类型';
COMMENT ON COLUMN "public"."sys_file"."biz_id" IS '业务ID';
COMMENT ON COLUMN "public"."sys_file"."upload_by" IS '上传人ID';
COMMENT ON COLUMN "public"."sys_file"."upload_time" IS '上传时间';
COMMENT ON TABLE "public"."sys_file" IS '文件信息表';

-- ----------------------------
-- Records of sys_file
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_login_log";
CREATE TABLE "public"."sys_login_log" (
  "info_id" int8 NOT NULL,
  "username" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "ipaddr" varchar(128) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "login_location" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "browser" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "os" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "status" int4 DEFAULT 0,
  "msg" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "login_time" timestamp(6)
)
;
ALTER TABLE "public"."sys_login_log" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_login_log"."info_id" IS '日志主键';
COMMENT ON COLUMN "public"."sys_login_log"."username" IS '用户账号';
COMMENT ON COLUMN "public"."sys_login_log"."ipaddr" IS '登录IP';
COMMENT ON COLUMN "public"."sys_login_log"."login_location" IS '登录地点';
COMMENT ON COLUMN "public"."sys_login_log"."browser" IS '浏览器类型';
COMMENT ON COLUMN "public"."sys_login_log"."os" IS '操作系统';
COMMENT ON COLUMN "public"."sys_login_log"."status" IS '登录状态（0-成功 1-失败）';
COMMENT ON COLUMN "public"."sys_login_log"."msg" IS '提示消息';
COMMENT ON COLUMN "public"."sys_login_log"."login_time" IS '登录时间';
COMMENT ON TABLE "public"."sys_login_log" IS '登录日志表';

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_menu";
CREATE TABLE "public"."sys_menu" (
  "id" int8 NOT NULL,
  "parent_id" int8 DEFAULT 0,
  "menu_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "path" varchar(200) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "component" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "query_param" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "perms" varchar(100) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "icon" varchar(100) COLLATE "pg_catalog"."default" DEFAULT '#'::character varying,
  "menu_type" char(1) COLLATE "pg_catalog"."default" DEFAULT ''::bpchar,
  "order_num" int4 DEFAULT 0,
  "visible" int4 DEFAULT 1,
  "status" int4 DEFAULT 1,
  "is_frame" int4 DEFAULT 1,
  "is_cache" int4 DEFAULT 0,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_menu" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_menu"."id" IS '菜单ID';
COMMENT ON COLUMN "public"."sys_menu"."parent_id" IS '父菜单ID';
COMMENT ON COLUMN "public"."sys_menu"."menu_name" IS '菜单名称';
COMMENT ON COLUMN "public"."sys_menu"."path" IS '路由地址';
COMMENT ON COLUMN "public"."sys_menu"."component" IS '组件路径';
COMMENT ON COLUMN "public"."sys_menu"."query_param" IS '路由参数';
COMMENT ON COLUMN "public"."sys_menu"."perms" IS '权限标识';
COMMENT ON COLUMN "public"."sys_menu"."icon" IS '菜单图标';
COMMENT ON COLUMN "public"."sys_menu"."menu_type" IS '菜单类型（M-目录，C-菜单，F-按钮）';
COMMENT ON COLUMN "public"."sys_menu"."order_num" IS '显示顺序';
COMMENT ON COLUMN "public"."sys_menu"."visible" IS '是否可见（0-隐藏，1-显示）';
COMMENT ON COLUMN "public"."sys_menu"."status" IS '状态（0-禁用，1-正常）';
COMMENT ON COLUMN "public"."sys_menu"."is_frame" IS '是否外链（0-是，1-否）';
COMMENT ON COLUMN "public"."sys_menu"."is_cache" IS '是否缓存（0-缓存，1-不缓存）';
COMMENT ON TABLE "public"."sys_menu" IS '菜单表';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1000, 100, '用户查询', '', '', '', 'system:user:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1001, 100, '用户新增', '', '', '', 'system:user:add', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1002, 100, '用户修改', '', '', '', 'system:user:edit', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1003, 100, '用户删除', '', '', '', 'system:user:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1004, 100, '用户导出', '', '', '', 'system:user:export', '#', 'F', 5, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1005, 100, '用户导入', '', '', '', 'system:user:import', '#', 'F', 6, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1006, 100, '重置密码', '', '', '', 'system:user:resetPwd', '#', 'F', 7, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1100, 101, '角色查询', '', '', '', 'system:role:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1101, 101, '角色新增', '', '', '', 'system:role:add', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1102, 101, '角色修改', '', '', '', 'system:role:edit', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1103, 101, '角色删除', '', '', '', 'system:role:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1104, 101, '角色导出', '', '', '', 'system:role:export', '#', 'F', 5, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1200, 102, '菜单查询', '', '', '', 'system:menu:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1201, 102, '菜单新增', '', '', '', 'system:menu:add', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1202, 102, '菜单修改', '', '', '', 'system:menu:edit', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1203, 102, '菜单删除', '', '', '', 'system:menu:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1300, 103, '部门查询', '', '', '', 'system:dept:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1301, 103, '部门新增', '', '', '', 'system:dept:add', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1302, 103, '部门修改', '', '', '', 'system:dept:edit', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1303, 103, '部门删除', '', '', '', 'system:dept:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1070, 107, '文件查询', '', '', '', 'system:file:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1071, 107, '文件上传', '', '', '', 'system:file:upload', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1072, 107, '文件下载', '', '', '', 'system:file:download', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1073, 107, '文件删除', '', '', '', 'system:file:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1, 0, '系统管理', 'system', NULL, '', '', 'settings', 'M', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (2, 0, '系统监控', 'monitor', NULL, '', '', 'monitor-cog', 'M', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (100, 1, '用户管理', 'user', 'system/user/index', '', 'system:user:list', 'users', 'C', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (101, 1, '角色管理', 'role', 'system/role/index', '', 'system:role:list', 'users-round', 'C', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (102, 1, '菜单管理', 'menu', 'system/menu/index', '', 'system:menu:list', 'list-tree', 'C', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (103, 1, '部门管理', 'dept', 'system/dept/index', '', 'system:dept:list', 'folder-tree', 'C', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (105, 1, '字典管理', 'dict', 'system/dict/index', '', 'system:dict:list', 'book-open-text', 'C', 6, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (107, 1, '文件管理', 'file', 'system/file/index', '', 'system:file:query', 'files', 'C', 8, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (200, 2, '在线用户', 'online', 'monitor/online/index', '', 'monitor:online:list', 'badge-check', 'C', 1, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (201, 2, '操作日志', 'operlog', 'monitor/operlog/index', '', 'monitor:operlog:list', 'logs', 'C', 2, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (202, 2, '登录日志', 'loginlog', 'monitor/loginlog/index', '', 'monitor:loginlog:list', 'logs', 'C', 3, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (203, 2, '缓存监控', 'cache', 'monitor/cache/index', '', 'monitor:cache:list', 'database-zap', 'C', 4, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (204, 2, '服务监控', 'server', 'monitor/server/index', '', 'monitor:server:list', 'server-cog', 'C', 5, 1, 1, 1, 0, NULL, '2026-04-13 02:44:26.642746', NULL, '2026-04-13 02:44:26.642746', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (2010, 201, '操作日志详情', '', '', '', 'monitor:operlog:query', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-19 06:28:01.180419', NULL, '2026-04-19 06:28:01.180419', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (2011, 201, '操作日志删除', '', '', '', 'monitor:operlog:remove', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-19 06:28:01.180419', NULL, '2026-04-19 06:28:01.180419', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (2020, 202, '登录日志删除', '', '', '', 'monitor:loginlog:remove', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-19 06:28:01.180419', NULL, '2026-04-19 06:28:01.180419', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1050, 105, '字典详情', '', '', '', 'system:dict:detail', '#', 'F', 1, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1051, 105, '字典新增', '', '', '', 'system:dict:add', '#', 'F', 2, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1052, 105, '字典修改', '', '', '', 'system:dict:edit', '#', 'F', 3, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1053, 105, '字典删除', '', '', '', 'system:dict:remove', '#', 'F', 4, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1054, 105, '所有字典类型', '', '', '', 'system:dict:alltype', '#', 'F', 5, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1055, 105, '字典数据查询', '', '', '', 'system:dict:listData', '#', 'F', 6, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
INSERT INTO "public"."sys_menu" ("id", "parent_id", "menu_name", "path", "component", "query_param", "perms", "icon", "menu_type", "order_num", "visible", "status", "is_frame", "is_cache", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1056, 105, '刷新字典缓存', '', '', '', 'system:dict:refreshCache', '#', 'F', 7, 1, 1, 1, 0, NULL, '2026-04-19 06:32:21.561052', NULL, '2026-04-19 06:32:21.561052', NULL, 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_oper_log";
CREATE TABLE "public"."sys_oper_log" (
  "oper_id" int8 NOT NULL,
  "title" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "business_type" int4 DEFAULT 0,
  "method" varchar(200) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "request_method" varchar(10) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "operator_type" int4 DEFAULT 0,
  "oper_name" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "dept_name" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_ip" varchar(128) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_location" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_param" text COLLATE "pg_catalog"."default",
  "json_result" text COLLATE "pg_catalog"."default",
  "status" int4 DEFAULT 0,
  "error_msg" varchar(2000) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_time" timestamp(6),
  "cost_time" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_oper_log" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_oper_log"."oper_id" IS '日志主键';
COMMENT ON COLUMN "public"."sys_oper_log"."title" IS '模块标题';
COMMENT ON COLUMN "public"."sys_oper_log"."business_type" IS '业务类型（0-其它 1-新增 2-修改 3-删除 4-授权 5-导出 6-导入 7-强退 8-清空）';
COMMENT ON COLUMN "public"."sys_oper_log"."method" IS '方法名称';
COMMENT ON COLUMN "public"."sys_oper_log"."request_method" IS '请求方式';
COMMENT ON COLUMN "public"."sys_oper_log"."operator_type" IS '操作类别（0-其它 1-后台 2-手机端）';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_name" IS '操作人员';
COMMENT ON COLUMN "public"."sys_oper_log"."dept_name" IS '部门名称';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_url" IS '请求URL';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_ip" IS '主机地址';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_location" IS '操作地点';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_param" IS '请求参数';
COMMENT ON COLUMN "public"."sys_oper_log"."json_result" IS '返回参数';
COMMENT ON COLUMN "public"."sys_oper_log"."status" IS '操作状态（0-正常 1-异常）';
COMMENT ON COLUMN "public"."sys_oper_log"."error_msg" IS '错误消息';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_time" IS '操作时间';
COMMENT ON COLUMN "public"."sys_oper_log"."cost_time" IS '消耗时间（毫秒）';
COMMENT ON TABLE "public"."sys_oper_log" IS '操作日志表';

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845456960016384, '清空操作日志', 9, 'com.alpha.system.controller.SysOperLogController.clean()', 'DELETE', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/operlog/clean', '127.0.0.1', '内网IP', '{}', '{"code":200,"message":"操作成功","timestamp":"1779080105816","traceId":"e040d39ac6bd4d88"}', 0, NULL, '2026-05-18 12:55:05.816113', 37);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845457031319552, '操作日志', 4, 'com.alpha.system.controller.SysOperLogController.list()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/operlog/list', '127.0.0.1', '内网IP', '{"query":{},"pageQuery":{"pageNum":1,"pageSize":10}}', '{"code":200,"data":{"pageNum":"1","pageSize":"10","pages":"0","records":[],"total":"0"},"message":"操作成功","timestamp":"1779080105832","traceId":"c94b13a30642474f"}', 0, NULL, '2026-05-18 12:55:05.832373', 9);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845462341308416, '登录日志', 4, 'com.alpha.system.controller.SysLoginLogController.list()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/login/log/list', '127.0.0.1', '内网IP', '{"query":{"asc":false,"page":{"pageNumber":"1","pageSize":"10","records":[],"totalPage":"-1","totalRow":"-1"},"pageNum":1,"pageSize":10}}', '{"code":200,"data":{"pageNum":"1","pageSize":"10","pages":"1","records":[{"browser":"MSEdge","infoId":"413844914879778816","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 12:52:56","msg":"登录成功","os":"Mac","status":0,"username":"admin"},{"browser":"MSEdge","infoId":"413844885981024256","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 12:52:49","msg":"验证码错误","os":"Mac","status":1,"username":"admin"},{"browser":"MSEdge","infoId":"413840245075939328","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 12:34:23","msg":"登录成功","os":"Mac","status":0,"username":"admin"},{"browser":"MSEdge","infoId":"413821983231496192","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 11:21:49","msg":"登录成功","os":"Mac","status":0,"username":"admin"},{"browser":"MSEdge","infoId":"413821960984907776","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 11:21:43","msg":"验证码错误","os":"Mac","status":1,"username":"admin"},{"browser":"MSEdge","infoId":"413821893716660224","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 11:21:27","msg":"登录成功","os":"Mac","status":0,"username":"admin"},{"browser":"MSEdge","infoId":"413821477448744960","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 11:19:48","msg":"登录成功","os":"Mac","status":0,"username":"admin"},{"browser":"MSEdge","infoId":"413813719555014656","ipaddr":"127.0.0.1","loginLocation":"内网IP","loginTime":"2026-05-18 10:48:59","msg":"登录成功","os":"Mac","status":0,"username":"admin"}],"total":"8"},"message":"操作成功","timestamp":"1779080107098","traceId":"d4ec665f6a4747c5"}', 0, NULL, '2026-05-18 12:55:07.098683', 46);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845470423732224, '登录日志', 9, 'com.alpha.system.controller.SysLoginLogController.clean()', 'DELETE', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/login/log/clean', '127.0.0.1', '内网IP', '{}', '{"code":200,"message":"操作成功","timestamp":"1779080109026","traceId":"a27a18faeaed43a2"}', 0, NULL, '2026-05-18 12:55:09.02634', 30);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845470507618304, '登录日志', 4, 'com.alpha.system.controller.SysLoginLogController.list()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/login/log/list', '127.0.0.1', '内网IP', '{"query":{"asc":false,"page":{"pageNumber":"1","pageSize":"10","records":[],"totalPage":"-1","totalRow":"-1"},"pageNum":1,"pageSize":10}}', '{"code":200,"data":{"pageNum":"1","pageSize":"10","pages":"0","records":[],"total":"0"},"message":"操作成功","timestamp":"1779080109045","traceId":"21ccc11441964d69"}', 0, NULL, '2026-05-18 12:55:09.045719', 13);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845473670123520, '缓存监控', 4, 'com.alpha.system.controller.MonitorController.cacheList()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/cache/list', '127.0.0.1', '内网IP', '{}', '{"code":200,"data":[{"name":"permission"},{"name":"dict"},{"name":"user"},{"name":"config"}],"message":"操作成功","timestamp":"1779080109797","traceId":"550a64383c9a479e"}', 0, NULL, '2026-05-18 12:55:09.797614', 1);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845473741426688, '缓存监控', 4, 'com.alpha.system.controller.MonitorController.cacheInfo()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/cache/permission', '127.0.0.1', '内网IP', '{"cacheName":"permission"}', '{"code":200,"data":{"error":"Cache not found: permission"},"message":"操作成功","timestamp":"1779080109817","traceId":"50de2ea57c64478c"}', 0, NULL, '2026-05-18 12:55:09.817421', 0);
INSERT INTO "public"."sys_oper_log" ("oper_id", "title", "business_type", "method", "request_method", "operator_type", "oper_name", "dept_name", "oper_url", "oper_ip", "oper_location", "oper_param", "json_result", "status", "error_msg", "oper_time", "cost_time") VALUES (413845479948996608, '服务器监控', 4, 'com.alpha.system.controller.MonitorController.serverInfo()', 'GET', 1, 'admin', 'Alpha科技', '/wuxi_civil/monitor/server', '127.0.0.1', '内网IP', '{}', '{"code":200,"data":{"cpu":{"cpuNum":15,"free":100.0,"sys":0.0,"total":0.0,"used":0.0,"wait":0.0},"jvm":{"free":"5.84 GB","home":"/Users/mi_manchi/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home","max":"6.00 GB","name":"Java HotSpot(TM) 64-Bit Server VM","runTime":"0天0小时2分钟","startTime":"2026-05-18 12:52:32","total":"188.00 MB","usage":2.63,"used":"161.56 MB","version":"25"},"mem":{"free":"24.42 MB","total":"6.00 GB","usage":2.66,"used":"163.58 MB"},"sys":{"computerIp":"127.0.0.1","computerName":"Akari-MacBook-Pro.local","osArch":"aarch64","osName":"Mac OS X","userDir":"/Users/mi_manchi/workspace/无锡公务员管理系统/quantum-backend"},"sysFiles":[{"dirName":"/","free":"801.05 GB","sysTypeName":"/","total":"926.30 GB","typeName":"本地磁盘","usage":13.52,"used":"125.25 GB"}]},"message":"操作成功","timestamp":"1779080111295","traceId":"7317d024f0ff43e9"}', 0, NULL, '2026-05-18 12:55:11.295708', 52);
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
  "id" int8 NOT NULL,
  "role_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "role_key" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "order_num" int4 DEFAULT 0,
  "data_scope" int4 DEFAULT 5,
  "status" int4 DEFAULT 1,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0
)
;
ALTER TABLE "public"."sys_role" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_role"."id" IS '角色ID';
COMMENT ON COLUMN "public"."sys_role"."role_name" IS '角色名称';
COMMENT ON COLUMN "public"."sys_role"."role_key" IS '角色标识';
COMMENT ON COLUMN "public"."sys_role"."order_num" IS '显示顺序';
COMMENT ON COLUMN "public"."sys_role"."data_scope" IS '数据权限（1-全部 2-本部门 3-本部门及子部门 4-自定义 5-仅本人）';
COMMENT ON COLUMN "public"."sys_role"."status" IS '状态（0-禁用，1-正常）';
COMMENT ON COLUMN "public"."sys_role"."remark" IS '备注';
COMMENT ON TABLE "public"."sys_role" IS '角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_role" ("id", "role_name", "role_key", "order_num", "data_scope", "status", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version") VALUES (1, '超级管理员', 'admin', 1, 1, 1, '超级管理员', '2026-04-13 02:44:26.563811', NULL, '2026-04-13 02:44:26.563811', NULL, 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_dept";
CREATE TABLE "public"."sys_role_dept" (
  "role_id" int8 NOT NULL,
  "dept_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_role_dept" OWNER TO "admin";
COMMENT ON TABLE "public"."sys_role_dept" IS '角色部门关联表（数据权限）';

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_menu";
CREATE TABLE "public"."sys_role_menu" (
  "role_id" int8 NOT NULL,
  "menu_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_role_menu" OWNER TO "admin";
COMMENT ON TABLE "public"."sys_role_menu" IS '角色菜单关联表';

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 2);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 100);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 101);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 102);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 103);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 105);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 107);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 200);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 201);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 202);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 203);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 204);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1000);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1001);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1002);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1003);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1004);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1005);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1006);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1100);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1101);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1102);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1103);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1104);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1200);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1201);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1202);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1203);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1300);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1301);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1302);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1303);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1070);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1071);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1072);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1073);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 2010);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 2011);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 2020);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1050);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1051);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1052);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1053);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1054);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1055);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (1, 1056);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (2, 1);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (2, 100);
INSERT INTO "public"."sys_role_menu" ("role_id", "menu_id") VALUES (2, 1000);
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
  "id" int8 NOT NULL,
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(50) COLLATE "pg_catalog"."default",
  "email" varchar(100) COLLATE "pg_catalog"."default",
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "avatar" varchar(255) COLLATE "pg_catalog"."default",
  "sex" int4 DEFAULT 0,
  "dept_id" int8,
  "status" int4 DEFAULT 1,
  "data_scope" int4 DEFAULT 5,
  "login_ip" varchar(128) COLLATE "pg_catalog"."default",
  "login_date" timestamp(6),
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "create_by" int8,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "deleted" int4 DEFAULT 0,
  "version" int8 DEFAULT 0,
  "login_location" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."sys_user" OWNER TO "admin";
COMMENT ON COLUMN "public"."sys_user"."id" IS '用户ID';
COMMENT ON COLUMN "public"."sys_user"."username" IS '用户名';
COMMENT ON COLUMN "public"."sys_user"."password" IS '密码';
COMMENT ON COLUMN "public"."sys_user"."nickname" IS '昵称';
COMMENT ON COLUMN "public"."sys_user"."email" IS '邮箱';
COMMENT ON COLUMN "public"."sys_user"."phone" IS '手机号';
COMMENT ON COLUMN "public"."sys_user"."avatar" IS '头像';
COMMENT ON COLUMN "public"."sys_user"."sex" IS '性别（0-未知，1-男，2-女）';
COMMENT ON COLUMN "public"."sys_user"."dept_id" IS '部门ID';
COMMENT ON COLUMN "public"."sys_user"."status" IS '状态（0-禁用，1-正常）';
COMMENT ON COLUMN "public"."sys_user"."data_scope" IS '数据权限（1-全部 2-本部门 3-本部门及子部门 4-自定义 5-仅本人）';
COMMENT ON COLUMN "public"."sys_user"."login_ip" IS '最后登录IP';
COMMENT ON COLUMN "public"."sys_user"."login_date" IS '最后登录时间';
COMMENT ON COLUMN "public"."sys_user"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_user"."deleted" IS '逻辑删除（0-未删除，1-已删除）';
COMMENT ON COLUMN "public"."sys_user"."version" IS '乐观锁版本号';
COMMENT ON COLUMN "public"."sys_user"."login_location" IS '最后登录地点';
COMMENT ON TABLE "public"."sys_user" IS '用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_user" ("id", "username", "password", "nickname", "email", "phone", "avatar", "sex", "dept_id", "status", "data_scope", "login_ip", "login_date", "remark", "create_time", "create_by", "update_time", "update_by", "deleted", "version", "login_location") VALUES (1, 'admin', '$2a$10$SOFfiNmzNMCinUqLkQihve9fJ2W0vTyp7SP/KO/51RpthQusxM6EG', '超级管理员', '123@alpha.com', '13800000209', NULL, 1, 0, 1, 1, '127.0.0.1', '2026-05-18 12:52:56.552743', NULL, '2026-04-13 02:44:26.535737', NULL, '2026-05-18 12:52:56.710253', 1, 0, 220, '内网IP');
COMMIT;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
  "user_id" int8 NOT NULL,
  "role_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_user_role" OWNER TO "admin";
COMMENT ON TABLE "public"."sys_user_role" IS '用户角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO "public"."sys_user_role" ("user_id", "role_id") VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Uniques structure for table sys_config
-- ----------------------------
ALTER TABLE "public"."sys_config" ADD CONSTRAINT "uk_sys_config_key" UNIQUE ("config_key");

-- ----------------------------
-- Primary Key structure for table sys_config
-- ----------------------------
ALTER TABLE "public"."sys_config" ADD CONSTRAINT "sys_config_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_dept
-- ----------------------------
CREATE INDEX "idx_sys_dept_parent" ON "public"."sys_dept" USING btree (
  "parent_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_dept
-- ----------------------------
ALTER TABLE "public"."sys_dept" ADD CONSTRAINT "sys_dept_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_dict_data
-- ----------------------------
CREATE INDEX "idx_dict_data_type" ON "public"."sys_dict_data" USING btree (
  "dict_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_dict_data
-- ----------------------------
ALTER TABLE "public"."sys_dict_data" ADD CONSTRAINT "sys_dict_data_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "uk_sys_dict_type" UNIQUE ("dict_type");

-- ----------------------------
-- Primary Key structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "sys_dict_type_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_file
-- ----------------------------
CREATE INDEX "idx_sys_file_biz" ON "public"."sys_file" USING btree (
  "biz_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_sys_file_md5" ON "public"."sys_file" USING btree (
  "md5" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_file
-- ----------------------------
ALTER TABLE "public"."sys_file" ADD CONSTRAINT "sys_file_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_login_log
-- ----------------------------
CREATE INDEX "idx_login_log_time" ON "public"."sys_login_log" USING btree (
  "login_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_login_log_user" ON "public"."sys_login_log" USING btree (
  "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_login_log
-- ----------------------------
ALTER TABLE "public"."sys_login_log" ADD CONSTRAINT "sys_login_log_pkey" PRIMARY KEY ("info_id");

-- ----------------------------
-- Indexes structure for table sys_menu
-- ----------------------------
CREATE INDEX "idx_sys_menu_parent" ON "public"."sys_menu" USING btree (
  "parent_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_menu
-- ----------------------------
ALTER TABLE "public"."sys_menu" ADD CONSTRAINT "sys_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_oper_log
-- ----------------------------
CREATE INDEX "idx_oper_log_business" ON "public"."sys_oper_log" USING btree (
  "business_type" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oper_log_status" ON "public"."sys_oper_log" USING btree (
  "status" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oper_log_time" ON "public"."sys_oper_log" USING btree (
  "oper_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oper_log_user" ON "public"."sys_oper_log" USING btree (
  "oper_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_oper_log
-- ----------------------------
ALTER TABLE "public"."sys_oper_log" ADD CONSTRAINT "sys_oper_log_pkey" PRIMARY KEY ("oper_id");

-- ----------------------------
-- Uniques structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "uk_sys_role_key" UNIQUE ("role_key");

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_role_dept
-- ----------------------------
ALTER TABLE "public"."sys_role_dept" ADD CONSTRAINT "sys_role_dept_pkey" PRIMARY KEY ("role_id", "dept_id");

-- ----------------------------
-- Primary Key structure for table sys_role_menu
-- ----------------------------
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_pkey" PRIMARY KEY ("role_id", "menu_id");

-- ----------------------------
-- Indexes structure for table sys_user
-- ----------------------------
CREATE INDEX "idx_sys_user_dept" ON "public"."sys_user" USING btree (
  "dept_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_sys_user_status" ON "public"."sys_user" USING btree (
  "status" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "uk_sys_user_username" UNIQUE ("username");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("user_id", "role_id");
