### 数据库

~~~sql
-- 1. 创建用户
CREATE USER talent_manger WITH PASSWORD '占位';

-- 2. 创建数据库（归属这个用户）
CREATE DATABASE talent_manger OWNER talent_manger;

-- 3. 授权所有权限talent_manger
GRANT ALL PRIVILEGES ON DATABASE talent_manger TO talent_manger;
~~~
