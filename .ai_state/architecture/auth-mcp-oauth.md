# MCP OAuth Architecture

更新: 2026-07-08 (sprint: 2026-07-07-quantum-mcp-s3-impl)

## 定位

`quantum-mcp` 是运行期只读能力适配模块, 与 `quantum-biz-system` 平级, 由 `quantum-server`
引入。模块依赖 biz service 接口, biz/common 不反向依赖它。

## 配置

- `ai.mcp.enabled=false` 默认关闭。
- `ai.mcp.issuer/resource/endpoint` 控制 OAuth metadata 与 MCP endpoint。
- `ai.mcp.clients.*` 是静态 public client allowlist; S3 不做动态客户端注册。

## OAuth Store

独立于普通 Web token:

| 类型 | Key |
|---|---|
| 授权码 | `quantum:oauth:code:{codeId}` |
| access token | `quantum:oauth:access:{tokenId}` |
| refresh token | `quantum:oauth:refresh:{refreshTokenId}` |
| used refresh marker | `quantum:oauth:refresh-used:{refreshTokenId}` |
| 用户 token 索引 | `quantum:oauth:user:{userId}` |

授权码绑定 client/redirect/resource/PKCE/user, 一次性消费。refresh token 旋转; 旧 refresh 复用会吊销该 client token 链。

## HTTP Surface

- `/.well-known/oauth-protected-resource`
- `/.well-known/oauth-authorization-server`
- `/oauth/authorize`
- `/oauth/token`
- `/oauth/revoke`
- `/mcp`

`/mcp` 支持:

- `GET /mcp`: Quantum Manifest v1
- `POST /mcp`: JSON-RPC 2.0 `initialize`, `tools/list`, `tools/call`

## Auth Runtime

`OAuthBearerAuthenticationFilter` 只处理 `/mcp/**`:

1. 读取 `Authorization: Bearer <opaque-token>`。
2. `OAuthTokenService.validateAccessToken` 校验 token + resource + enabled LoginUser。
3. 写入 `SecurityContextHolder` (`McpAuthenticationDetails` 保存 client/resource/scopes) 与 `UserContext`。
4. 缺失/无效 token 返回 401, `WWW-Authenticate` 指向 protected resource metadata。

普通 Web JWT/refresh cookie 不作为 MCP OAuth token 使用。

## Tool Guard

首批只读 tools:

| tool | OAuth scope | permission | dataScopeMode |
|---|---|---|---|
| `system.user.search` | `system.user.read` | `system:user:list` | required |
| `system.dept.tree` | `system.dept.read` | `system:dept:list` | permission-only |
| `system.role.list` | `system.role.read` | `system:role:query` | permission-only |

`McpToolService` 在 service 调用前断言 `UserContext.getUser() != null`、OAuth token scope 与 tool permission。输出统一先经 `JsonUtil` 序列化为 JSON 文本。JSON-RPC `tools/call` 捕获业务异常并返回 JSON-RPC error envelope; legacy 非 JSON-RPC `tools/call` 分支返回显式 `error.code/message` payload。数字参数解析失败归一为 `PARAM_INVALID`。
