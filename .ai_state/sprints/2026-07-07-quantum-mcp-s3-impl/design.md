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
| `com.alpha.mcp.security.OAuthBearerAuthenticationFilter` | 仅过滤 `/mcp/**`, Bearer token → `SecurityContextHolder` + `UserContext` + OAuth scopes, invalid → 401 + metadata |
| `com.alpha.mcp.controller.*` | well-known / OAuth / MCP manifest + tool call endpoint |
| `com.alpha.mcp.manifest.CapabilityManifestService` | Manifest v1, 工具 schema + OAuth scope + permission + dataScopeMode + riskLevel |
| `com.alpha.mcp.tool.McpToolService` | fail-closed user context + OAuth scope guard + permission guard + `JsonUtil` 序列化 output |

## 3. 验收标准

- `ai.mcp.enabled=false` 默认关闭, `application.yml` 显式声明。
- `/oauth/token` 只接受 `authorization_code` + PKCE S256 与 `refresh_token`, 不支持 password/client_credentials。
- authorization code 一次性消费并绑定 client/redirect/resource/PKCE/user。
- refresh token rotation; 旧 refresh 复用会吊销该 client token 链。
- `/mcp` 缺失或错误 Bearer token 返回 401, `WWW-Authenticate` 带 protected resource metadata。
- Manifest 包含 `resource/authorizationServers/transport/tools[]` 与每个 tool 的 `scope/permission/dataScopeMode/riskLevel/inputSchema/outputSchema`。
- `/mcp` POST 支持 MCP JSON-RPC envelope: `initialize`, `tools/list`, `tools/call`。
- JSON-RPC `tools/call` 的业务异常返回 JSON-RPC error envelope, 不泄漏到普通 HTTP 错误形态。
- tool call 缺 `UserContext`、OAuth scope 不足或权限不足均 fail-closed; 输出走 `JsonUtil`。

## 4. 非目标

- 不做动态客户端注册。
- 不实现写类 tool。
- 不引入 chat/RAG/Provider/SSE/token quota。
- 不把普通 Web access token 当 MCP OAuth token 使用。

## Critic Findings — Round 1

### VERDICT: REWORK (NEEDS_REVISION)

模块隔离、默认关闭、PKCE / refresh rotation、Bearer filter、Manifest skeleton 与单测证据基本成立；但设计遗漏了 OAuth scope 到 MCP tool 授权的强约束。当前代码可作为默认关闭 skeleton 留存，但启用 `ai.mcp.enabled=true` 前必须补授权路径与验收测试。

### 评分表 (6 维 1-5 分)

| 维度 | 分 | 评估 |
|---|---:|---|
| 边界条件遗漏 | 3 | code/resource/refresh 已覆盖；scope 降权、真实 HTTP 启用、JSON-RPC 错误路径未覆盖。 |
| 错误处理不完整 | 3 | 401 metadata 有单测；tool `BizException` 未定义 JSON-RPC error envelope。 |
| 测试覆盖盲区 | 2 | `runtime-verify.md` 明确 DB 缺失导致无 curl 级启动验证；scope/data-scope 端到端未测。 |
| compound/decision 冲突 | 3 | 未直接冲突；但弱化了 `codegen-security-gates-default-on` 的 fail-closed 精神。 |
| 实现复杂度评估 | 4 | 不需要拆 roadmap；一次 rework 可补 scope context、测试、错误映射。 |
| compound/learning 冲突 | 3 | `templates-replicate-fixed-vulnerabilities` 要求安全默认值可验；当前 scope 约束靠文档/consent 展示但未强制。 |

### Findings

- F1 P1 — OAuth scope 只签发不执行，consent/allowlist 不能限制 tool 访问。`OAuthAccessToken.scopes` 已存储，`OAuthBearerAuthenticationFilter` 只把 `LoginUser` 写入上下文，`McpToolService.requirePermission` 只检查用户权限，Manifest 也没有每个 tool 的 `oauthScope` 字段。结果是客户端请求较小 scope 后，只要用户本身有更大权限，仍可能调用未授权 tool。Action: 在 Manifest 定义 tool scope；Bearer filter 把 token scopes 带入认证上下文；tool guard 同时校验 `token scope + user permission`；新增测试: 只含 `system.user.read` 的 token 调 `system.role.list` 必须拒绝。
- F2 P1 — 启用态 HTTP 验收缺失。`runtime-verify.md` 把 PostgreSQL 不可用记录为 ENV GAP，review 仅列 P2；但 design 验收包含 well-known、OAuth token、`/mcp` 401/JSON-RPC，这些目前只由单测/Mock 覆盖。Action: 增加 dev DB/Testcontainers 或可复跑 profile，实跑 `ai.mcp.enabled=true` 下的 metadata、token、401、tools/list、tools/call smoke。
- F3 P2 — JSON-RPC 错误契约未冻结。`McpController` 只对未知 method 返回 JSON-RPC error；权限、参数、tool 执行异常会落到全局 HTTP 错误处理，可能破坏 MCP 客户端契约。Action: 设计补充 JSON-RPC error mapping，至少覆盖 authz denied、invalid params、tool not found、internal error。
- F4 P2 — 生产启用配置护栏不足。`application.yml` / `McpProperties` 默认 issuer/resource/client 都是 localhost，设计只写“默认关闭 + 静态 allowlist”，没有规定非本地环境启用时必须覆盖 issuer/resource/redirect allowlist。Action: 增加验收: 非 local profile `ai.mcp.enabled=true` 时必须显式配置外部 issuer/resource 与 client redirect allowlist；本地 loopback 只允许 local profile。

### 建议下一轮重点

1. 先补 F1 scope-to-tool 双重授权与红/绿测试。
2. 再补启用态 HTTP smoke，把 ENV GAP 变成可复跑证据。
3. 最后冻结 JSON-RPC error 与非 local 配置护栏，避免后续 tool 扩展复制安全缺口。

### Round 1 Resolution (2026-07-08)

- F1 已修复: `OAuthBearerAuthenticationFilter` 将 `OAuthAccessToken.scopes` 写入 `McpAuthenticationDetails`; Manifest 每个 tool 声明 `scope`; `McpToolService` 同时校验 OAuth scope 与用户 permission。新增 `McpToolServiceTest.toolCallRequiresMatchingOAuthScope` 与 filter details 断言。
- F3 已修复: JSON-RPC `tools/call` 捕获 `BizException` 并返回 error envelope; 401/403 映射 `-32003`, 400/404 映射 `-32602`, 其余映射 `-32603`。新增 `McpControllerTest.mapsToolBizExceptionToJsonRpcErrorEnvelope`。
- F2 保留为 ENV GAP: 本机 PostgreSQL 未运行, live HTTP curl 仍需在 dev DB/VM 环境补跑。
- F4 保留为启用前护栏: 当前 S3 默认关闭; 非 local 启用时仍要求显式配置 issuer/resource/client redirect allowlist。

## Critic Findings — Round 2

### VERDICT: PASS WITH CONCERNS

Round 1 的 F1/F3 已闭环, 当前没有 P0/P1 阻断项。F2 属于 ENV GAP: 本地 PostgreSQL 不可用导致无法做 `ai.mcp.enabled=true` 的 live HTTP curl, 但单测与 `quantum-server` 聚合测试已覆盖代码契约。F4 属于 future hardening / 启用前门禁: 默认关闭状态下不阻断 S3 ship, 但任何非 local 环境启用前必须补可执行配置护栏。JSON-RPC error object 形态参考: https://www.jsonrpc.org/specification#error_object。

### 评分表 (6 维 1-5 分)

| 维度 | 分 | 评估 |
|---|---:|---|
| 边界条件遗漏 | 4 | OAuth scope 缩权、缺 authentication details、缺用户 permission 均 fail-closed; live HTTP 与非 local 启用态仍未实证。 |
| 错误处理不完整 | 4 | JSON-RPC `tools/call` 已捕获 `BizException` / `RuntimeException`; legacy 非 JSON-RPC `tools/call` 仍走普通 HTTP 异常路径。 |
| 测试覆盖盲区 | 4 | `quantum-mcp` 14 tests 与 `mvn -pl quantum-server -am test` PASS; 缺 dev DB/VM 下端口级 smoke。 |
| compound/decision 冲突 | 5 | `codegen-security-gates-default-on` 的 fail-closed 精神已落实到 tool scope + permission 双门禁。 |
| 实现复杂度评估 | 4 | 不需要拆 roadmap; 剩余项是运行环境和启用前校验, 可作为 ship 后 hardening slice。 |
| compound/learning 冲突 | 4 | 已避免"文档声明安全、代码未强制"的复刻漏洞; 非 local 配置仍需从约定升级为自动门禁。 |

### Findings

- F1 P1 — CLOSED: OAuth scope 已进入 MCP tool 授权链。证据: `OAuthBearerAuthenticationFilter` 写入 `McpAuthenticationDetails`; Manifest tool 含 `scope`; `McpToolService` 先校验 OAuth scope 再校验用户 permission; `McpToolServiceTest.toolCallRequiresMatchingOAuthScope` 覆盖低 scope token 调高 scope tool 被拒。
- F3 P2 — CLOSED: JSON-RPC error 契约已冻结到 controller 层。证据: `McpController` 对 JSON-RPC `tools/call` 捕获 `BizException` 并返回 `error.code/message`; 401/403 -> `-32003`, 400/404 -> `-32602`, 其他 -> `-32603`; `McpControllerTest.mapsToolBizExceptionToJsonRpcErrorEnvelope` 覆盖。
- F2 P2 — ENV GAP, not ship-blocking: 当前缺完整 Spring Boot 进程 + HTTP curl 证据, 原因是本机 DB 未运行。S3 ship 可接受, 因为默认关闭且聚合测试通过; 但启用前必须在 dev DB/VM 环境复跑 metadata、401 `WWW-Authenticate`、OAuth code/token、`tools/list`、授权失败 `tools/call`。
- F4 P2 — FUTURE HARDENING, not ship-blocking: `application.yml` 与 `McpProperties` 仍保留 localhost issuer/resource/client 默认值; 当前仅靠默认关闭与注释要求非 local 覆盖。非 local profile 启用前应加启动校验: `ai.mcp.enabled=true` 时禁止 loopback issuer/resource, 且 client redirect allowlist 必须显式来自环境/profile。
- F5 P2 — Future cleanup: legacy 非 JSON-RPC `/mcp` `tools/call` 分支仍可能把 tool `BizException` 交给普通 HTTP 错误处理。若后续只承诺 MCP JSON-RPC, 建议删除或标记该兼容分支; 若继续暴露, 需补同等 error mapping。

### 建议下一轮重点

1. 在有 dev DB/VM 的环境补 live HTTP smoke, 把 F2 从 ENV GAP 变成可复跑证据。
2. 非 local 启用前补启动配置校验, 把 F4 从文档约定变成自动 fail-fast。
3. 决定 legacy 非 JSON-RPC `/mcp` 分支去留; 保留则补错误契约测试。

### Round 2 Resolution (2026-07-08)

- Reviewer P2 legacy 非 JSON-RPC error 已修复: legacy `tools/call` 分支捕获 `BizException` / `RuntimeException`, 返回顶层 `error.code/message` payload; 新增 `McpControllerTest.mapsLegacyToolCallBizExceptionToErrorPayload`。
- Reviewer P2 参数类型错误已修复: `McpToolService.intValue` 将数字解析失败转换为 `BizException(ResultCode.PARAM_INVALID, "invalid MCP tool argument: {name}")`; JSON-RPC 映射为 `-32602`; 新增 `mapsInvalidParamsBizExceptionToJsonRpcInvalidParams` 与 `toolCallRejectsInvalidIntegerArguments`。
- 最终验证: `mvn -pl quantum-server -am test` PASS, `quantum-mcp` 17 tests PASS。
