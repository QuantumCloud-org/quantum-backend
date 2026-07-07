---
stage: ship
status: shipped
path: System
created: 2026-07-07
updated: 2026-07-07
---

# S3 Implementation — quantum-mcp

## 0. 目标

把 `2026-07-07-quantum-mcp-s3-preflight-design` 冻结的三件事落到代码:

1. 新增 `quantum-mcp` Maven module, 与 `quantum-biz-system` 平级, 由 `quantum-server` 引入。
2. MCP OAuth token 与普通 Web token 隔离: opaque token + `quantum:oauth:*` 独立 store + `OAuthBearerAuthenticationFilter`。
3. 暴露默认关闭的 `/mcp`、OAuth metadata、authorize/token/revoke 骨架与 Manifest v1; `/mcp` 支持 JSON-RPC 2.0 的 `initialize` / `tools/list` / `tools/call`; 首批只读 tools 为用户/部门/角色读取。

## 1. 官方来源

沿用 preflight design:

| 主题 | URL |
|---|---|
| MCP Authorization | https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization |
| OAuth 2.1 draft | https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/ |
| OAuth Protected Resource Metadata | https://www.rfc-editor.org/rfc/rfc9728 |
| Authorization Server Metadata | https://www.rfc-editor.org/rfc/rfc8414 |
| Resource Indicators | https://www.rfc-editor.org/rfc/rfc8707 |
| PKCE | https://www.rfc-editor.org/rfc/rfc7636 |

## 2. 实现结构

| 文件/包 | 职责 |
|---|---|
| `quantum-mcp/pom.xml` | 依赖 security/cache/framework/biz-system, 不反向污染 biz/common |
| `com.alpha.mcp.config.McpProperties` | `ai.mcp.*`, 默认 `enabled=false`, 静态 client allowlist |
| `com.alpha.mcp.oauth.*` | authorization code、access/refresh token、PKCE S256、refresh rotation/reuse revoke |
| `com.alpha.mcp.security.OAuthBearerAuthenticationFilter` | 仅过滤 `/mcp/**`, Bearer token → `SecurityContextHolder` + `UserContext`, invalid → 401 + metadata |
| `com.alpha.mcp.controller.*` | well-known / OAuth / MCP manifest + tool call endpoint |
| `com.alpha.mcp.manifest.CapabilityManifestService` | Manifest v1, 工具 schema + permission + dataScopeMode + riskLevel |
| `com.alpha.mcp.tool.McpToolService` | fail-closed user context + permission guard + `JsonUtil` 序列化 output |

## 3. 验收标准

- `ai.mcp.enabled=false` 默认关闭, `application.yml` 显式声明。
- `/oauth/token` 只接受 `authorization_code` + PKCE S256 与 `refresh_token`, 不支持 password/client_credentials。
- authorization code 一次性消费并绑定 client/redirect/resource/PKCE/user。
- refresh token rotation; 旧 refresh 复用会吊销该 client token 链。
- `/mcp` 缺失或错误 Bearer token 返回 401, `WWW-Authenticate` 带 protected resource metadata。
- Manifest 包含 `resource/authorizationServers/transport/tools[]` 与每个 tool 的 `permission/dataScopeMode/riskLevel/inputSchema/outputSchema`。
- `/mcp` POST 支持 MCP JSON-RPC envelope: `initialize`, `tools/list`, `tools/call`。
- tool call 缺 `UserContext` 或权限不足 fail-closed; 输出走 `JsonUtil`。

## 4. 非目标

- 不做动态客户端注册。
- 不实现写类 tool。
- 不引入 chat/RAG/Provider/SSE/token quota。
- 不把普通 Web access token 当 MCP OAuth token 使用。
