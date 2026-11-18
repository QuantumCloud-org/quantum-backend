# Auth And Security Architecture

最近更新: 2026-11-18（sprint: 2026-11-deps-upgrade-audit）

## CORS

- `security.cors-allowed-origins` 是浏览器跨域可信前端 Origin 列表。
- Credentials 开启时禁止配置精确 wildcard `*`。
- 生产 profile 使用 `SECURITY_CORS_ALLOWED_ORIGINS` 注入; 为空时不反射任意 Origin。

## Web 会话与 Refresh（S17，2026-11-18）

- 过滤器不再自动续期：access token 无效即未认证（401）。续期只走 `POST /auth/refresh`（permitAll，无 body，不带 Authorization，带登录时的 `X-Device-Id`）。
- refresh 成功：body 复用 `LoginResponse` 的 token 字段（`accessToken`、`expiresIn`、`expireTime`、`sessionId`），响应头 `Authorization`，下发新 refresh cookie 并清除旧 `Path=/` cookie。失败返回 401 并清两处 cookie。并发轮换落败返回 409（`DUPLICATE_REQUEST`），保留 cookie。依赖故障返回 503，前端保留会话。
- refresh cookie：HttpOnly、`SameSite=Strict`；`Path` 取 `security.refresh-cookie-path`（默认 `/api/auth`，prod `/talent_manger/auth`，须覆盖浏览器侧 login/refresh/logout）；`Secure` 由 `security.refresh-cookie-secure` 控制。
- 会话代际：每个用户有一个代际号。强退、改密、按用户吊销时递增代际，同时吊销 Web 与 MCP 会话；签发、刷新、校验都核对代际，所以吊销前已开始的 refresh 拿不到有效会话。local 模式下，代际与通用缓存淘汰分开存储。
- 上线影响：切到会话代际后，已有 Web 会话需要重新登录，MCP 会话需要重新授权。
- 登录：竞态或代际不一致返回 409 `SESSION_TRANSITION`，不计入失败次数；成功日志和 `clearLoginFail` 在签发成功之后才执行。验证码失败不计入账号失败次数；验证码校验用 `getAndDelete` 原子完成。

## 密码（D03，2026-11-18）

- `DelegatingPasswordEncoder`：新哈希一律为 `{bcrypt}` 标准 BCrypt。无前缀的旧哈希按 `BCrypt(username+password)` 校验，登录成功后用 compare-and-set 升级，只更新 password 列。
- 仍为旧哈希的用户不能改用户名；改名会撤销旧会话。
- 认证与密码写入以 72 个 UTF-8 字节为上限。

## Redis Codec（S19）

- Redisson Jackson codec 的多态白名单：`com.alpha.`、`java.util.`、`java.time.`、`java.lang.`、基本类型数组及上述类型的对象数组；泛型参数一并校验。字典失效 topic 的发布与订阅使用同一个定型 codec。

## 管理员与委派（S04 / S17 / D17）

- 超管判定使用 `LoginUser.isAdmin()` / `LoginUser.isSuperAdmin(userId)`（`quantum-common-framework`）；清空审计表与 `/auth/info.canClearAuditLogs` 共用同一判定。
- 角色委派规则在 `quantum-biz-system` 的 `security/RoleDelegationPolicy`：非超管不能分配 admin 角色、ALL 数据域，以及超出自己权限集的角色；新增、编辑、导入三个入口都适用。
- 清空登录日志、操作日志限超管；`/auth/info` 返回 `canClearAuditLogs` 供前端显示按钮。

## 用户数据域

- 用户列表与导出走 `@DataScope(DEFAULT)`，按运行时数据域判定（SELF 只看本人），与详情、写操作一致；显式的系统级数据域放行。
- 用户详情、用户角色读取、用户写操作均按当前 `LoginUser` 的 dept scope 断言。
- 超管账号仅超管本人可读写。
- 用户本人始终可读取/维护自己的资料。
- `LoginUser.dataScope/deptIds` 登录时优先由有效角色聚合:
  - 任一角色为 ALL 时得到全部数据权限。
  - DEPT / DEPT_AND_CHILD / CUSTOM 角色合并为最终 dept ID 集合, 运行态写入 CUSTOM。
  - 无有效角色时才回退到 `SysUser.dataScope`, 用于兼容历史数据。
- service 层部门数据域 guard 在缺少 `UserContext` 时返回 `UNAUTHORIZED`, 防止内部直调绕过数据权限。
- 菜单/权限 SQL 只计入启用且未删除的角色；修改父节点时检查祖先链，防止成环，递归 SQL 做去重。

## 脱敏

- `@Sensitive` 带 `@JacksonAnnotationsInside`，已真正生效：`UserVO`、`DeptVO`、`/auth/info` 返回的手机号和邮箱均为掩码。编辑接口（`UserEditVO`）返回明文；部门编辑省略联系方式时保留原值。

## 导入

- 导入 Excel 字段包含用户名、昵称、邮箱、手机号、性别、状态、部门 ID、角色 ID 列表。
- 导入新增用户复用 `UserCreateRequest` 校验用户名、手机号、邮箱、初始密码、部门和角色。
- 导入更新用户复用 `UserUpdateRequest` 与目标用户数据域校验; 角色 ID 列为空时保留现有角色, 非空时替换角色绑定。
- 导出用户会批量回填角色 ID 列, 便于导出后再导入。
- 导入上限 1000 行、导出上限 10000 行；结果为结构化的 `[{row, username, success, message}]`（行号为物理行号），失败时整批回滚。
- 导入更新成功后发布 `UserCacheRefreshEvent`。

## 文件

- 上传补偿只在事务明确回滚时删除对象；结果未知时保留对象并记录对账信息。
- 删除元数据后，在 afterCommit 阶段删除物理对象；仍有其他行引用同一路径时不删。

## 输入边界

- Role `dataScope` 只允许 1-5。
- 分页查询入参必须触发 `PageQuery` / `LogPageQuery` 的 Bean Validation。
- Local 与 RustFS 文件存储共享 object key path/fileName 校验策略。
- 字典写接口使用 DTO（`DictType*/DictData*Request`），更新必须带 `id` 和 `version`；非超管不可读写 admin 字典。
- `X-Trace-Id` 校验格式，不合法时重新生成。JSON 请求体有读取上限。

## Schema

- 2026-11-18 在线库与 `deploy/init.sql` 对齐：`deploy/migrations/2026-11-18-sync-init-columns.sql`（幂等），补齐 `sys_user.login_location` 和 `sys_dict_type.business`。`sys_config` 无代码引用，未建。
