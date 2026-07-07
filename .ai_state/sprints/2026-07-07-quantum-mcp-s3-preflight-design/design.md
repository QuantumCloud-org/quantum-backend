---
stage: ship
status: shipped
path: System
scope: design-only
created: 2026-07-07
updated: 2026-07-07
---

# S3 Preflight Design — quantum-mcp

## 0. 结论

本 sprint 不实现 `quantum-mcp` 代码, 只把 S3 开工前必须冻结的三件事落地:

1. **OAuth token 存储**: 不把 MCP OAuth access token 当普通 Web access token 复用; 新增独立 OAuth token store, 但复用 `LoginUser` 加载、用户下线/改密吊销语义和 `CacheClient` 存储模式。
2. **Consent 页**: `/oauth/authorize` 复用现有登录态, 但必须新增授权同意页; 授权码一次性、短 TTL、绑定 `client_id + redirect_uri + code_challenge + resource + userId`。
3. **Capability Manifest / 身份传递**: Manifest v1 冻结 `resource + tools[] + permission + dataScopeMode + riskLevel + schema`; 调用只使用标准 `Authorization: Bearer <access_token>`, 不加私有用户身份头。多资源场景在 token 请求期使用 RFC 8707 `resource` 参数。

## 1. 来源与本仓锚点

### 官方来源

| 主题 | 来源 |
|---|---|
| MCP Authorization | https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization |
| OAuth 2.1 draft | https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/ |
| OAuth Protected Resource Metadata | https://www.rfc-editor.org/rfc/rfc9728 |
| Authorization Server Metadata | https://www.rfc-editor.org/rfc/rfc8414 |
| Resource Indicators | https://www.rfc-editor.org/rfc/rfc8707 |
| Dynamic Client Registration | https://www.rfc-editor.org/rfc/rfc7591 |
| PKCE | https://www.rfc-editor.org/rfc/rfc7636 |

### 本仓源码事实

| 事实 | 锚点 |
|---|---|
| 普通 access/refresh token 由 `TokenService` 生成 JWT + Redis/Caffeine 缓存 tokenId → `LoginUser` | `quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java` |
| HTTP 请求认证成功后由 `TokenAuthenticationFilter` 写入 `SecurityContextHolder` + `UserContext` | `quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/TokenAuthenticationFilter.java` |
| `@RequiresPermission` 无用户时 fail-closed 抛 `UNAUTHORIZED` | `quantum-common/quantum-common-security/src/main/java/com/alpha/security/aspect/PermissionAspect.java` |
| `DataScopeAspect` 无用户时当前会跳过, MCP 入口必须先 fail-closed | `quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/aspect/DataScopeAspect.java` |
| `UserContext` 是 ThreadLocal/InheritableThreadLocal, tool handler 异步执行必须显式重建上下文 | `quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/context/UserContext.java` |
| 脱敏依赖 Jackson `@Sensitive` + `SensitiveSerializer` 路径, MCP SDK 直出对象会绕过 | `quantum-common/quantum-common-security/src/main/java/com/alpha/security/serializer/SensitiveSerializer.java` |

## 2. S3 范围

### 本轮冻结

- `quantum-mcp` 只读能力适配的认证/授权骨架。
- OAuth endpoints: `/.well-known/oauth-protected-resource`, `/.well-known/oauth-authorization-server`, `/oauth/authorize`, `/oauth/token`, `/oauth/revoke`。
- MCP HTTP endpoint: 默认 `/mcp`, `ai.mcp.enabled=false`。
- 首批 tools: `system.user.search`, `system.dept.tree`, `system.role.list`。
- 明确排除: 写/导出类能力, 动态客户端注册, 下游 chat/RAG/Provider/SSE/token 配额。

### 开工退出条件

如果实现阶段发现需要改造现有 `TokenAuthenticationFilter` 才能让普通 Web token 与 OAuth token 隔离, 立即 re-route 到 Refactor/System 实现 sprint, 不在生成器里临时打补丁。

## 3. OAuth token 存储决策

### 决策

新增 `quantum-mcp` 内部 `OAuthTokenService` / `OAuthTokenStore`, 使用独立 key 前缀:

| 类型 | Key 形态 | 内容 | TTL |
|---|---|---|---|
| 授权码 | `quantum:oauth:code:{codeId}` | `clientId`, `redirectUri`, `codeChallenge`, `codeChallengeMethod`, `resource`, `userId`, `scope`, `nonce` | 5 分钟 |
| access token | `quantum:oauth:access:{tokenId}` | `LoginUser` 快照 + `clientId` + `resource` + `scope` + issuedAt | 短 TTL, 默认 15 分钟 |
| refresh token | `quantum:oauth:refresh:{refreshTokenId}` | access token rotation 绑定信息 + userId + clientId | 配置 TTL |
| 用户索引 | `quantum:oauth:user:{userId}` | OAuth tokenId 集合 | 跟随 refresh TTL |

### 为什么不直接复用普通 Web access token

- 普通 Web token 当前没有 `aud/resource/scope/client_id` 约束; 如果 MCP token 与 Web token 互通, 任一 token 泄露面会扩大。
- MCP 公共客户端不使用 HttpOnly refresh cookie; 普通 refresh cookie 流程与本地 agent 自动刷新语义不同。
- 独立 OAuth store 可以让 `/oauth/revoke`、用户下线、改密失效精确作用于 MCP token, 不影响浏览器会话策略。

### 与现有体系的复用边界

- 复用: `CacheClient`, `LoginUser` 模型, `UserDetailsService` 刷新用户权限, 用户下线/改密吊销事件语义。
- 不复用: 普通 `TokenAuthenticationFilter` 的 Web token 自动续期与 refresh cookie 写回。
- MCP 请求入口使用 `OAuthBearerAuthenticationFilter`: 验 token → 取 `LoginUser` → 校验 `resource` → 写 `SecurityContextHolder` + `UserContext` → tool handler。

## 4. Consent 页决策

### 流程

1. MCP client 请求 `/mcp` 无 token → 401 + protected resource metadata。
2. client 读取 metadata, 跳转 `/oauth/authorize?response_type=code&client_id=...&redirect_uri=...&code_challenge=...&code_challenge_method=S256&state=...&resource=...`。
3. 用户未登录时走现有登录页, 登录后回到 authorize。
4. 用户已登录时展示 consent 页: client 名称、resource、tools/scopes、只读声明、有效期、撤销入口。
5. 用户同意后生成一次性 code; `/oauth/token` 用 code + verifier 换 access/refresh token。

### 安全约束

- `redirect_uri` 必须精确匹配静态 client allowlist; S3 不做动态注册。
- 只允许公共客户端 + PKCE S256; 不接收明文 code challenge method。
- `state` 原样返回给 client; 服务端另存一份授权请求 nonce, 防止用户 session 与授权码错配。
- consent 结果不长期默认记住; S3 首版每个 client/resource 首次连接都显式同意。
- 授权码一次性消费; token refresh rotation; reuse refresh token 立即吊销该 client 的 OAuth token 链。

## 5. Capability Manifest v1

### 最小 schema

```json
{
  "schemaVersion": "quantum.mcp.manifest.v1",
  "resource": "https://backend.example.com/mcp",
  "authorizationServers": ["https://backend.example.com"],
  "transport": "streamable-http",
  "tools": [
    {
      "name": "system.user.search",
      "title": "查询用户",
      "description": "按关键词读取用户列表, 返回结果受当前操作者数据权限过滤",
      "readOnly": true,
      "permission": "system:user:list",
      "dataScopeMode": "required",
      "riskLevel": "low",
      "auditEvent": "MCP_TOOL_CALL",
      "inputSchema": {
        "type": "object",
        "properties": {
          "keyword": {"type": "string", "maxLength": 50},
          "pageNum": {"type": "integer", "minimum": 1},
          "pageSize": {"type": "integer", "minimum": 1, "maximum": 50}
        },
        "required": ["pageNum", "pageSize"]
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "rows": {"type": "array"},
          "total": {"type": "integer"}
        }
      }
    }
  ]
}
```

### 身份传递约定

- ai-service / project-data-reader → quantum-mcp: `Authorization: Bearer <OAuth access token>`。
- 不传 `X-User-Id`, `X-Dept-Ids`, `X-Permissions` 等私有身份头; 身份只能由 token 映射到服务端 `LoginUser`。
- `resource` 在 OAuth 授权/换 token 阶段绑定; tool 调用期由 token 内绑定的 resource 与当前 MCP endpoint 比对。
- tool handler 入口统一断言:
  - `UserContext.getUser() != null`
  - token `resource` 匹配当前 `/mcp`
  - tool `permission` 与用户权限匹配
  - `dataScopeMode=required` 的 tool 必须通过 service 层 `@DataScope` / 显式 guard, 禁止 mapper 直查。

## 6. Round 1 · Critic Findings (main-agent local review)

> 说明: 本 turn 未使用 spawn_agent, 因为当前可用 spawn_agent 工具要求只有用户显式要求 subagent 时才可调用。这里保留 PACE 的 critic 结构, 但标明为主 agent 本地审查, 不冒充独立模型。

### VERDICT: PASS

- F1 [checked] token 复用边界已收窄: 独立 OAuth token store 避免 MCP token 与 Web token 互通。
- F2 [checked] consent 不复用“登录即授权”: 新增显式 consent 页, 授权码绑定 PKCE/resource/user。
- F3 [checked] Manifest 保留 `riskLevel/dataScopeMode` 自定义 metadata, 但身份传递不自定义 header。
- F4 [checked] 动态客户端注册后置, S3 以静态 client allowlist 降低首版复杂度。
- F5 [checked] DataScope fail-open 风险没有被设计掩盖: MCP filter + tool handler 双层 fail-closed, 并保留框架加固待办。

## 7. S3 实现验收门槛

- `mvn test` 至少覆盖 OAuth code 交换、PKCE fail、resource mismatch、refresh reuse revoke、无 UserContext 拒绝、无权限拒绝、数据域过滤、脱敏输出。
- `/mcp` 401 必须带 protected resource metadata。
- `/oauth/token` 只接受 S256 PKCE code flow; password/client_credentials 不在 S3 范围。
- `system.user.search` 在普通用户与 admin 下结果不同, 且普通用户不能看到数据域外用户。
- tool 响应使用应用 ObjectMapper/JsonUtil 序列化后再交给 MCP SDK。
