# Quantum 后端 AI 生成体系设计

日期: 2026-07-06
状态: draft
来源: 2026-07-05 后端完整 review 后的架构设计沉淀
范围: `quantum-backend`
修订: 2026-07-06 逐条源码复核 Review 摘要 (9/9 属实), 补充证据位置; P1-4 升格为写侧越权, P2-3 确认注解为死代码

## 1. 背景

当前后端是 Spring Boot + Maven 多模块的模块化单体:

- `quantum-common-*`: framework, security, orm, cache, logging, file 等通用能力
- `quantum-biz-system`: 系统业务域, 用户、角色、菜单、部门、文件等
- `quantum-server`: 启动与 profile 配置

当前主线代码可构建, `mvn test` 通过, 但测试主要覆盖缓存与少量系统工具类, 安全、权限、数据域和控制器契约测试不足。后续目标不是直接改成微服务, 而是在现有模块化单体上补齐边界、契约、策略和生成门禁, 让 AI 通过 CLI/MCP 自动创建后端代码时有明确护栏。

## 2. Review 摘要

2026-07-06 逐条对照源码复核, 9 条断言全部属实。每条附证据位置 (文件:行号), 修复与测试可直接引用。

### P1 安全与权限

1. **CORS 反射任意 Origin 且带凭证**。`quantum-server/src/main/resources/application.yml:70` 配置 `security.cors-allowed-origins: "*"`; `SecurityConfig.java:227` 用 `setAllowedOriginPatterns` (而非 `setAllowedOrigins`, 后者与 credentials 组合会在运行时报错, 前者不会) + `SecurityConfig.java:231` `setAllowCredentials(true)`。效果: 任意 Origin 均被反射且允许带 cookie。`application-prod.yml` 无 cors 键, 生产不覆盖。修复: 生产显式域名白名单。
2. **refresh token cookie 无 Secure 开关**。`CookieUtil.java:42-57` 仅拼 `HttpOnly + SameSite=Strict + Path`, 全库无 `Secure` 属性。注意: 类注释写明"当前部署环境为内网 HTTP, 因此明确不设置 Secure" — 属已知决策而非疏漏, 但缺少 HTTPS 部署时的 profile 开关, 迁移 HTTPS 会静默裸奔。修复: 配置驱动 Secure 开关 (如 `security.cookie-secure`), 生产 profile 默认 true。
3. **读侧越权 (IDOR-read)**。`SysUserServiceImpl.java:85` `selectUserById` 为裸 `getById`; `SysRoleServiceImpl.java:72` `selectRoleIdsByUserId` 为裸 mapper 查询。两者对应端点 `GET /system/user/{userId}` 与 `GET /system/user/{userId}/roles` 仅有 `system:user:query` 权限码, 无数据域断言。对照: 列表侧 `selectUserPage/selectUserList` 有 `@DataScope(DEPT_AND_CHILD)`; 写侧 update/delete/resetPwd/changeStatus 已有 `assertTargetUserWritable`。持 `system:user:query` 者可遍历 ID 读取全量用户与角色绑定。
4. **导入路径写侧越权 (IDOR-write) + 校验绕过**。`SysUserServiceImpl.java:293-340` `importUsers`:
   - 新增分支 (`:311` 裸 `save`): 无手机号/邮箱唯一性校验 (对照 `insertUser` `:105-111`), 无 `assertDeptInDataScope` (对照 `:116`)。
   - 更新分支 (`:319` 裸 `updateById`): 无 `assertTargetUserWritable` (对照 `updateUser` `:155`) — **持 `system:user:import` 者可通过 Excel 改写数据域之外任意用户的昵称/邮箱/手机/状态**, 严重度与第 3 条持平, 不只是"校验不一致"; 且不发布 `UserCacheRefreshEvent` (对照 `:175`), 被改用户登录态缓存脏读。
   - 修复: 导入分支复用 `insertUser/updateUser` 或抽取共享校验 + 事件发布。

### P2 架构与一致性

1. **角色 `dataScope` 校验不一致**。`RoleCreateRequest.java:42` 与 `RoleUpdateRequest.java:54` 均为裸 `Integer` 无范围注解; 服务层 `insertRole/updateRole` 也不校验; 仅独立接口 `updateDataScope` (`SysRoleServiceImpl.java:188`) 有 `1-5` 检查。经新增/编辑通道可写入非法 dataScope 值, 登录态解析 `DataScopeType.fromCode` 走 default 分支产生非预期行为。修复: DTO 加 `@Min(1)/@Max(5)` + service 双层校验。
2. **数据域双来源混用**。`SysUser.java:77` 与 `SysRole.java:39` 各有 `dataScope` 字段; `UserDetailsServiceImpl` 登录时以 `user.getDataScope()` 决定类型 (`:68,:81`), 但 CUSTOM 分支又从角色侧 `role_dept` 聚合 (`:88,:93`)。语义割裂, 对后续 AI 生成代码不够清晰, 见第 7 节单一来源方案。
3. **分页校验注解为死代码 (确认无隐式兜底)**。`PageQuery.java:31-38` 的 `@Min/@Max(2000)` 依赖 `@Validated/@Valid` 触发, 但全部 6 个 list 控制器 (SysUser/SysRole/SysDict/SysLoginLog/SysOperLog/File) 的 GET 查询入参均未标注, 控制器类级也无 `@Validated`。Spring 对 model attribute 不做隐式校验, `Page.of(pageNum, pageSize)` 不截断 — **`pageSize=10000000` 直达数据库, 认证用户即可构成资源耗尽面**。实际风险高于最初"可能依赖框架隐式行为"的措辞。修复: 全部查询入参补 `@Validated` + MockMvc 400 断言。
4. **RustFS 对象 key 零校验**。Local (`LocalFileStorageServiceImpl.java:63-88`) 有 `normalize` + `startsWith` 断言 + 自定义文件名禁 `/` `\` `..`; RustFS (`RustfsFileStorageServiceImpl.java:92-111`) 将调用方传入的 `path` 与 `fileName` 原样拼接为 `objectKey`, 无任何字符校验。S3 语义下 `..` 不构成文件系统穿越, 但 key 可被注入 `/` 改写目录层级、覆盖同 bucket 其他对象。修复: 与 Local 对齐的 key 白名单策略。
5. **契约测试缺失**。全仓仅 3 个测试类: `LocalCacheClientTest` (缓存)、`ServerInfoTest` (DTO)、`SysDeptServiceImplTest` (部门服务)。安全过滤链、权限码、数据域、文件上传、controller 层均无自动化测试 — 上述 P1 问题无一能被现有测试捕获。

## 3. 设计目标

1. 保留模块化单体, 用工程约束提升可维护性。
2. 建立契约先行的后端生成体系, 避免 AI 直接散写 Java 文件。
3. 把权限、数据域、分页、审计、迁移和测试变成生成门禁。
4. 支持两种入口:
   - CLI: 给开发者和 CI 使用。
   - MCP: 给 AI Agent 使用, 通过工具读写和验证。
5. 所有自动生成代码必须可 diff、可回滚、可测试、可审查。

## 4. 推荐架构

### 4.1 模块化单体增强

短期继续使用 Maven 多模块:

- `quantum-common-security`: 鉴权、权限注解、cookie、CORS、token 策略
- `quantum-common-orm`: 分页、数据权限、审计字段、查询策略
- `quantum-common-file`: 文件校验、存储 key 策略、上传下载安全策略
- `quantum-biz-system`: 系统域业务
- 新业务域按 `quantum-biz-{domain}` 扩展

新增架构约束:

- 用 Spring Modulith 或 ArchUnit 校验模块依赖方向。
- 禁止业务模块跨域直接访问其他模块 internal 包。
- 业务模块只通过 public API、application service 或 domain event 协作。
- 共享能力进入 common, 业务规则留在 biz module。

### 4.2 契约层

建立 `contracts/` 目录作为 AI 生成输入源:

```text
contracts/
  openapi/
    system.openapi.yaml
  schema/
    user.schema.json
    role.schema.json
  feature/
    system-user.feature.yaml
  policy/
    permissions.yaml
    data-scope.yaml
  db/
    migrations/
```

契约职责:

- OpenAPI: API 路径、请求、响应、错误码、认证方式。
- JSON Schema: DTO 字段、校验、枚举、默认值。
- Feature manifest: 聚合 controller/service/mapper/test/migration 生成意图。
- Policy manifest: endpoint 权限码、数据域、写权限、审计和幂等要求。
- DB migration: 表结构变更必须有迁移文件, 禁止只改 entity。

### 4.3 生成器

新增 `quantum-codegen` 模块, 提供模板和校验:

```text
quantum-codegen/
  src/main/java/com/alpha/codegen/
  src/main/resources/templates/
    controller.java.peb
    service.java.peb
    mapper.java.peb
    dto.java.peb
    test.java.peb
    migration.sql.peb
```

生成步骤:

1. 读取 feature manifest。
2. 校验 OpenAPI、JSON Schema、policy、DB migration。
3. 生成 DTO、controller、service、mapper、convert、test。
4. 运行格式化、单测、架构测试、OpenAPI diff。
5. 输出变更报告。

AI 不直接自由创建后端文件, 而是先生成 manifest, 再调用 codegen。

## 5. CLI 模式

CLI 第一阶段建议使用 Picocli, 原因是轻量、适合 CI、启动快。若需要交互式命令台, 再补 Spring Shell。

建议命令:

```bash
quantum gen feature \
  --module system \
  --entity sys_user \
  --crud \
  --permission system:user \
  --data-scope dept-and-child

quantum verify feature --manifest contracts/feature/system-user.feature.yaml
quantum diff openapi --baseline contracts/openapi/system.openapi.yaml
quantum inspect schema --table sys_user
quantum add permission --code system:user:export --menu 用户管理
```

CLI 输出要求:

- 列出将新增/修改文件。
- 列出权限码、数据域策略、审计策略。
- 列出待执行测试。
- 默认 dry-run, 显式 `--write` 才写入。

## 6. MCP 模式

MCP 第二阶段建设, 面向 AI Agent。MCP 工具不应暴露任意文件写入, 只暴露受约束的后端开发动作。

建议工具:

| tool | 作用 |
|---|---|
| `inspect_project` | 返回模块、包结构、技术栈、测试状态 |
| `inspect_schema` | 查看表、字段、索引、迁移历史 |
| `generate_feature_plan` | 根据需求生成 feature manifest 草案 |
| `validate_manifest` | 校验 OpenAPI、JSON Schema、policy、DB migration |
| `create_crud_slice` | 按模板生成 CRUD 垂直切片 |
| `add_permission` | 增加权限码、菜单、角色绑定迁移 |
| `verify_policy` | 检查 endpoint 权限、数据域、写域完整性 |
| `run_tests` | 运行指定 Maven 测试和架构测试 |
| `openapi_diff` | 输出 API 兼容性差异 |

MCP 写入规则:

- 只能写 `contracts/`, codegen 目标文件和测试文件。
- 写入前返回 plan, 写入后返回 diff summary。
- 安全策略失败时拒绝生成业务代码。
- 所有工具返回结构化 JSON, 方便 Agent 决策。

## 7. 权限与数据域策略

建立统一 policy contract:

```yaml
resource: system.user
entity: SysUser
idField: id
permissions:
  list: system:user:list
  query: system:user:query
  add: system:user:add
  edit: system:user:edit
  remove: system:user:remove
dataScope:
  read: dept-and-child
  write: dept-and-child
guards:
  - forbidSelfDelete
  - forbidAdminMutation
  - requireOptimisticLock
```

生成器必须据此生成:

- `@RequiresPermission`
- read scope guard
- write scope guard
- optimistic lock
- cache/session refresh event
- MockMvc 权限测试
- service 层越权测试

数据权限需要尽快明确单一来源:

- 方案 A: 用户登录态保存最终 data scope, 角色只用于登录时聚合。
- 方案 B: 角色为唯一来源, 用户不再存独立 data scope。

建议优先方案 A, 因为当前代码已接近该模式, 改动较小。

## 8. 测试与门禁

生成代码必须通过:

1. `mvn test`
2. controller MockMvc 测试
3. 权限码覆盖测试
4. 数据域 read/write 越权测试
5. 文件上传安全测试
6. OpenAPI diff
7. ArchUnit 或 Spring Modulith module verification
8. migration 可执行性测试

重点补齐当前缺口 (对应第 2 节各条, 修复必须带回归测试):

- CORS 测试: 生产 profile 下非白名单 Origin 不被反射、不带 `Access-Control-Allow-Credentials`
- refresh cookie 测试: secure 开关开启时 `Set-Cookie` 含 `Secure`, 关闭时不含
- 读侧数据域测试: 域外用户 `GET /system/user/{id}` 与 `/{id}/roles` 返回拒绝 (P1-3)
- 导入越权测试: 持 `system:user:import` 者更新域外用户被拒; 导入重复手机/邮箱被拒; 导入更新后缓存刷新事件发布 (P1-4)
- 角色 dataScope 测试: 新增/编辑通道传 `dataScope=99` 返回 400 (P2-1)
- 分页校验测试: `pageSize=999999` 返回 400, 覆盖全部 6 个 list 端点 (P2-3)
- RustFS object key 测试: `fileName` 含 `/` `..` 被拒 (P2-4)

## 9. 演进路线

### Phase 1: 安全基线修复

按第 2 节证据位置逐项修复, 每项带回归测试 (见第 8 节缺口清单):

1. 收紧生产 CORS: `application-prod.yml` 显式域名白名单覆盖 `cors-allowed-origins` (P1-1)。
2. refresh cookie 增加配置驱动 Secure 开关, 生产 profile 默认开 (P1-2)。
3. 用户详情/角色读取补数据域断言, 与写侧 `assertTargetUserWritable` 对齐 (P1-3)。
4. 用户导入复用 `insertUser/updateUser` 校验链: 唯一性 + 数据域 + 缓存刷新事件 (P1-4, 写侧越权, 优先级与 3 持平)。
5. 角色 dataScope DTO `@Min/@Max` + service 双层校验 (P2-1)。
6. 全部 list 端点查询入参补 `@Validated`, 激活 `PageQuery` 死注解 (P2-3, 资源耗尽面, 提前到 Phase 1)。
7. RustFS object key 校验与 Local 策略对齐 (P2-4)。

### Phase 2: 契约与测试

- 新增 `contracts/`。
- 定义 feature manifest v1。
- 补 policy manifest。
- 补安全与权限契约测试。
- 引入 OpenAPI diff。

### Phase 3: CLI 生成器

- 新增 `quantum-codegen`。
- 实现 dry-run 和 `--write`。
- 生成 CRUD 垂直切片。
- 自动生成测试和 migration。

### Phase 4: MCP 工具化

- 暴露 inspect/validate/generate/verify/run_tests 工具。
- AI 只能通过 manifest 和 codegen 落代码。
- 加入审查报告和失败回滚。

### Phase 5: 架构健身函数

- Spring Modulith 或 ArchUnit module verification。
- endpoint 权限覆盖率。
- policy 完整性扫描。
- schema/API 兼容性检查。

## 10. 官方依据

- Spring Security CORS: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- Spring Framework Web MVC CORS: https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html
- Spring Modulith: https://docs.spring.io/spring-modulith/reference/
- OpenAPI Specification: https://spec.openapis.org/oas/latest.html
- JSON Schema: https://json-schema.org/specification
- MCP Server Tools: https://modelcontextprotocol.io/specification/2025-11-25/server/tools
- Picocli: https://picocli.info/
- Spring Shell: https://docs.spring.io/spring-shell/reference/
- Flyway: https://documentation.red-gate.com/flyway

## 11. 决策

当前建议:

1. 不急于拆微服务。
2. 先把模块化单体变成契约驱动单体。
3. CLI 先于 MCP。
4. 生成 manifest 先于生成 Java。
5. 权限、数据域、测试和迁移必须成为生成器默认产物。
