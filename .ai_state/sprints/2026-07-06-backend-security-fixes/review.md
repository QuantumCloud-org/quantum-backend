# Review

## 范围

对照用户确认的 7 个修复点检查 git diff 与测试覆盖。

## 结论

VERDICT: PASS

## 检查结果

1. CORS: `application.yml` 移除 `*`; `SecurityConfig` 拒绝 wildcard + credentials; prod 使用 `SECURITY_CORS_ALLOWED_ORIGINS`。
2. Cookie: `refresh-cookie-secure` 配置接入登录、续期、登出。
3. 用户读侧: `selectUserById` 增加 read guard; 用户角色接口先走用户数据域校验。
4. 用户导入: 新增/更新分支补唯一性、数据域、乐观锁冲突和缓存刷新事件。
5. Role dataScope: DTO 与 service 均补 1-5 范围校验。
6. PageQuery: 7 个分页入口补 `@Validated`。
7. RustFS key: Local/RustFS 共用 object key path/fileName 校验。

## 风险

- `SECURITY_CORS_ALLOWED_ORIGINS` 为空时生产不允许浏览器跨域访问, 需要部署方显式配置前端域名。
- 本轮未改变数据权限双来源模型, 该架构问题留到后续独立收敛。
