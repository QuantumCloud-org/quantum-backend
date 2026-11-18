-- 2026-11-18 在线库与 deploy/init.sql 列对齐（幂等，可重复执行）
-- 来源：E2E 发现在线库缺 init.sql 已声明的两列，导致登录信息回写与字典 business 过滤报 SQL 错误。
-- sys_config 表在 init.sql 中声明但代码无引用，暂不创建。

ALTER TABLE "public"."sys_user" ADD COLUMN IF NOT EXISTS "login_location" varchar(255);
COMMENT ON COLUMN "public"."sys_user"."login_location" IS '最后登录地点';

ALTER TABLE "public"."sys_dict_type" ADD COLUMN IF NOT EXISTS "business" varchar(40) DEFAULT 'admin';
COMMENT ON COLUMN "public"."sys_dict_type"."business" IS '业务类型(admin 管理员，talnet 人才)';
