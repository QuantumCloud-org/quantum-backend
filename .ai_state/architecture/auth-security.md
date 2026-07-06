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

## 导入

- 导入新增用户复用用户名、手机号、邮箱、初始密码和部门数据域校验。
- 导入更新用户复用目标用户数据域校验, 失败时整批回滚。
- 导入更新成功后发布 `UserCacheRefreshEvent`。

## 输入边界

- Role `dataScope` 只允许 1-5。
- 分页查询入参必须触发 `PageQuery` / `LogPageQuery` 的 Bean Validation。
- Local 与 RustFS 文件存储共享 object key path/fileName 校验策略。
