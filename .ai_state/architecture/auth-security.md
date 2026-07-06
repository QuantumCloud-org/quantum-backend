# Auth And Security Architecture

## CORS

- `security.cors-allowed-origins` 是浏览器跨域可信前端 Origin 列表。
- Credentials 开启时禁止配置精确 wildcard `*`。
- 生产 profile 使用 `SECURITY_CORS_ALLOWED_ORIGINS` 注入; 为空时不反射任意 Origin。

## Refresh Cookie

- refresh token 只通过 HttpOnly cookie 传输。
- cookie 固定 `SameSite=Strict` 和 `Path=/`。
- `security.refresh-cookie-secure` 控制 `Secure`; prod 默认 true, dev 默认 false。

## 用户数据域

- 用户列表走 `@DataScope(DEPT_AND_CHILD)`。
- 用户详情、用户角色读取、用户写操作均按当前 `LoginUser` 的 dept scope 断言。
- 超管账号仅超管本人可读写。
- 用户本人始终可读取/维护自己的资料。
- `LoginUser.dataScope/deptIds` 登录时优先由有效角色聚合:
  - 任一角色为 ALL 时得到全部数据权限。
  - DEPT / DEPT_AND_CHILD / CUSTOM 角色合并为最终 dept ID 集合, 运行态写入 CUSTOM。
  - 无有效角色时才回退到 `SysUser.dataScope`, 用于兼容历史数据。
- service 层部门数据域 guard 在缺少 `UserContext` 时返回 `UNAUTHORIZED`, 防止内部直调绕过数据权限。

## 导入

- 导入 Excel 字段包含用户名、昵称、邮箱、手机号、性别、状态、部门 ID、角色 ID 列表。
- 导入新增用户复用 `UserCreateRequest` 校验用户名、手机号、邮箱、初始密码、部门和角色。
- 导入更新用户复用 `UserUpdateRequest` 与目标用户数据域校验; 角色 ID 列为空时保留现有角色, 非空时替换角色绑定。
- 导出用户会批量回填角色 ID 列, 便于导出后再导入。
- 导入失败时整批回滚。
- 导入更新成功后发布 `UserCacheRefreshEvent`。

## 输入边界

- Role `dataScope` 只允许 1-5。
- 分页查询入参必须触发 `PageQuery` / `LogPageQuery` 的 Bean Validation。
- Local 与 RustFS 文件存储共享 object key path/fileName 校验策略。
