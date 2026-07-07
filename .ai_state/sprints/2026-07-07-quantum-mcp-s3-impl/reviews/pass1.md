# Review Pass 1 — quantum-mcp S3 implementation

> 说明: 当前 Codex 可见工具未直接提供 Athena reviewer/spec-compliance/evaluator subagent 调度入口; 本 pass1 由主 agent 本地执行 6 维审查, 不冒充独立 subagent。

## Reviewer (代码层 findings)

### P0

无。

### P1

无。

### P2

1. Live HTTP curl 未覆盖。
   - 原因: 本机 `127.0.0.1:5432` 未开放, dev profile 会因 PostgreSQL 不可用阻断完整应用启动。
   - 处置: `runtime-verify.md` 记录为 ENV GAP; 代码侧以 Mock servlet filter + controller JSON-RPC unit tests 覆盖认证/响应路径。

## Spec Compliance (spec-compliance, local)

### MISSING

无。preflight design 验收映射:

| design 验收 | 实现 |
|---|---|
| 新 `quantum-mcp` module | root pom module + `quantum-mcp/pom.xml` + server dependency |
| 默认 disabled | `McpProperties.enabled=false` + `application.yml ai.mcp.enabled=false` |
| well-known/OAuth endpoints | `OAuthMetadataController` + `OAuthController` |
| OAuth 独立 token store | `OAuthTokenService` + `quantum:oauth:*` key prefix |
| PKCE S256 / one-time code / resource binding | `PkceUtil` + `OAuthTokenServiceTest` |
| refresh rotation / reuse revoke | `OAuthTokenService.refresh` + test |
| `/mcp` 401 metadata | `OAuthBearerAuthenticationFilterTest` |
| Manifest v1 | `CapabilityManifestService` + test |
| Bearer-only identity, no private headers | Manifest test asserts no `X-User-Id/X-Dept-Ids/X-Permissions` |
| UserContext fail-closed / permission guard | `McpToolServiceTest` |
| JsonUtil output | `McpToolService.callTool` returns `jsonUtil.toJson(result)` |

### EXTRA

- JSON-RPC `initialize/tools/list/tools/call` envelope: 合理补强, 让 `/mcp` POST 更贴近标准 MCP transport。

### DEVIATED

- 未使用 MCP Java SDK。判定: 可接受的 S3 skeleton 偏差; design 的硬约束是先走应用 `JsonUtil` 序列化, 当前实现满足。后续若接入 SDK, 保持 SDK 不直接序列化业务对象。

### 总评

PASS。

## Evaluator (综合判定)

VERDICT: PASS

- P0/P1 = 0。
- P2 = 1 (live HTTP curl 环境缺口), 已记录且不阻断 ship。
- Tests: `mvn -pl quantum-server -am test` BUILD SUCCESS; `quantum-mcp` 12 tests PASS。
- next_action: polish。
