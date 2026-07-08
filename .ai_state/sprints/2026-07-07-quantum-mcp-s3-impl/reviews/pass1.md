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

## Spec Compliance (spec-compliance, refreshed 2026-07-08)

### MISSING

无。当前工作树满足最新 `design.md` 验收标准。

| design 验收 | 实现 / 证据 |
|---|---|
| 新 `quantum-mcp` module 并接入 `quantum-server` | root/server/module poms; `mvn -pl quantum-server -am test` PASS |
| `ai.mcp.enabled=false` 默认关闭 | `McpProperties` + `application.yml` |
| OAuth metadata / authorize / token / revoke | `OAuthMetadataController` + `OAuthController` |
| 独立 OAuth token store + PKCE S256 + refresh rotation | `OAuthTokenServiceTest` 覆盖 code/resource/PKCE/rotation |
| `/mcp` Bearer fail-closed + 401 metadata | `OAuthBearerAuthenticationFilterTest` |
| Manifest v1 + per-tool scope/permission/dataScope/risk | `CapabilityManifestServiceTest` |
| MCP JSON-RPC `initialize/tools/list/tools/call` | `McpControllerTest` |
| `tools/call` 业务异常返回 JSON-RPC error envelope | `McpControllerTest` |
| 缺 `UserContext`、OAuth scope 不足、权限不足均 fail-closed | `McpToolServiceTest` |
| output 走 `JsonUtil` | `McpToolService` + service tests |

### DEVIATED

无。

### EXTRA

- 流程产物: `runtime-verify.md`, `cleanup-pass.md`, architecture 更新, review 记录。
- legacy 非 JSON-RPC `tools/call` 兼容分支: 保留为兼容路径; reviewer refresh 发现的 error payload 缺口已修复。

### 总评

PASS。

## Review Refresh / Closeout (2026-07-08)

| finding | 当前状态 | 结论 |
|---|---|---|
| F1 OAuth scope 未进入 tool guard | fixed | `OAuthBearerAuthenticationFilter` 写入 `McpAuthenticationDetails`; Manifest tool `scope`; `McpToolService` 同时校验 OAuth scope + 用户 permission |
| F3 JSON-RPC error 契约未冻结 | fixed | JSON-RPC `tools/call` 捕获 `BizException`; 401/403 -> `-32003`, 400/404 -> `-32602`, 其余 -> `-32603` |
| Reviewer P2: legacy 非 JSON-RPC tools/call error payload | fixed | legacy 分支返回顶层 `error.code/message` payload |
| Reviewer P2: 数字参数错误映射 | fixed | 参数解析失败转 `BizException(ResultCode.PARAM_INVALID)`; JSON-RPC 映射 `-32602` |
| F2 live HTTP 启用态未覆盖 | remaining P2 / ENV GAP | 本机 DB 不可用导致缺完整 HTTP curl; 默认关闭 + 单测/聚合测试已覆盖代码契约, 不阻断 ship |
| F4 非 local 配置护栏 | remaining P2 / future hardening | 非 local 启用前补启动配置 fail-fast; 当前 S3 默认关闭, 不阻断 ship |

P0/P1 = 0。remaining P2 仅 `live HTTP ENV GAP` 与 `非 local 配置 future hardening`; legacy/param P2 均已 fixed。

Final tests: `mvn -pl quantum-server -am test` PASS; `quantum-mcp` 17 tests PASS. Targeted cleanup tests: `McpControllerTest` 5 PASS + `McpToolServiceTest` 4 PASS.

## Evidence Cross-Check

Scope: cross-checked `checklist.yaml`, `evidence.yaml`, latest `design.md`, and this refreshed `pass1.md`.

| checklist item | status | matched evidence | verdict |
|---|---:|---|---|
| module | done | file evidence in checklist + Maven aggregate PASS | OK |
| config | done | `McpProperties` + `application.yml`; default disabled mapped in Spec Compliance | OK |
| oauth-store | done | `OAuthTokenServiceTest`; Maven aggregate PASS | OK |
| bearer-filter | done | `OAuthBearerAuthenticationFilterTest`; targeted + aggregate PASS | OK |
| endpoints | done | controllers mapped; `McpControllerTest` targeted PASS | OK |
| manifest-tools | done | `CapabilityManifestServiceTest`; aggregate PASS | OK |
| tool-guard | done | `McpToolServiceTest` targeted PASS; scope + permission fail-closed | OK |
| runtime-verify | done | `runtime-verify.md`; `mvn -pl quantum-server -am test` PASS | OK |
| review-polish | done | this file refreshed; cleanup/architecture evidence recorded | OK |
| ship | done | evidence records merge/worktree cleanup/push state | OK |

Summary: `done_without_evidence = 0`. 文件类 task 通过 checklist 文件路径、Spec Compliance 映射与 `git diff` 现场状态补认; Bash 证据链覆盖 Maven/targeted tests。

## VERDICT (evaluator, 2026-07-07-quantum-mcp-s3-impl)

**判定**: PASS WITH CONCERNS

### 评分依据 (4 维)

| 维度 | 得分 (0-5) | 说明 |
|---|---:|---|
| Functionality | 4.5 | 最新 `design.md` 验收项均有实现/测试映射; live HTTP 仍是环境缺口 |
| Spec Compliance | 5.0 | MISSING=无, DEVIATED=无, EXTRA 仅流程产物/legacy 兼容分支 |
| Craft | 4.5 | OAuth scope + permission 双门禁、JSON-RPC/legacy error mapping、参数错误映射已补测试 |
| Robustness | 4.0 | fail-closed 路径覆盖; 非 local 启用配置仍需 future hardening |

总评: 4.5 / 5.0

### 触发判定的关键 findings

- F2 (P2): live HTTP curl 缺口为 ENV GAP, 不阻断 ship。
- F4 (P2): 非 local 配置启动护栏为 future hardening, 不阻断 ship。
- F1/F3: 已 fixed。
- Reviewer refresh P2 legacy/param: 已 fixed。

### 行动建议

- 必须立即修: 无。
- 进入 ship 后 hardening: F2 live HTTP dev DB/VM smoke; F4 非 local 配置 fail-fast。
- 推迟到下 sprint: 无强制项。

### Sisyphus 完整性检查

- [x] design.md 中所有 Task 完成
- [x] 所有 Task 验收标准过测试
- [x] (Refactor/System 路径) 准备进入 ship; PASS WITH CONCERNS 不阻断 ship
