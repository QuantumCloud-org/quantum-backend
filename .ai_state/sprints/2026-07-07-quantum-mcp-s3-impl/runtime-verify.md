# Runtime Verify — quantum-mcp S3 implementation

## /goal 完成条件

- `quantum-mcp` OAuth/token/filter/tool/JSON-RPC tests pass.
- `quantum-server` aggregate build sees the new module and passes.
- Feasible local runtime checks are recorded; unavailable services are called out as environment gaps, not hidden.

## 测试场景 (实跑)

| 场景 | 类型 | 命令 | 实际输出 | 判定 |
|---|---|---|---|---|
| OAuth / MCP module regression | Maven | `mvn -pl quantum-mcp -am test` | `BUILD SUCCESS`; `quantum-mcp` 10 tests PASS before JSON-RPC addition, 12 tests PASS after final server aggregate, 14 tests PASS after scope/error rework, 17 tests PASS after reviewer P2 cleanup | PASS |
| Server aggregate compile/test | Maven | `mvn -pl quantum-server -am test` | Reactor 11 modules `BUILD SUCCESS`; `quantum-mcp` 17 tests, dependency modules 66 tests | PASS |
| Targeted runtime slice | Maven | `mvn -pl quantum-mcp -am -Dtest=OAuthTokenServiceTest,OAuthBearerAuthenticationFilterTest,McpToolServiceTest,CapabilityManifestServiceTest,PkceUtilTest -Dsurefire.failIfNoSpecifiedTests=false test` | `BUILD SUCCESS`; 10 targeted MCP tests PASS | PASS |
| Scope guard rework | Maven | `mvn -pl quantum-mcp -am -Dtest=McpToolServiceTest,OAuthBearerAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false test` | red: missing `McpAuthenticationDetails`; green: `BUILD SUCCESS`, 5 tests PASS | PASS |
| JSON-RPC error rework | Maven | `mvn -pl quantum-mcp -am -Dtest=McpControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | red: `BizException` leaked; green: `BUILD SUCCESS`, 3 tests PASS | PASS |
| Reviewer P2 cleanup | Maven | `mvn -pl quantum-mcp -am -Dtest=McpControllerTest,McpToolServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` | red: legacy `BizException` leaked + bad integer `NumberFormatException`; green: `BUILD SUCCESS`, 9 tests PASS | PASS |
| Local DB availability | shell | `nc -z 127.0.0.1 5432; echo postgres_port=$?` | `postgres_port=1` | ENV GAP |
| Local app port availability | shell | `nc -z 127.0.0.1 8080; echo app_port=$?` | `app_port=1` | INFO |
| Diff hygiene | shell | `git diff --check` | no output | PASS |
| Debug marker scan | shell | `rg -n "TODO|FIXME|debugger|System\\.out|console\\.log|printStackTrace" quantum-mcp/src/main pom.xml quantum-server/pom.xml quantum-server/src/main/resources/application.yml` | no output | PASS |

## 自测自改记录

- 首版 `/mcp` POST 仅支持简化 method 分发; review 前自查发现与 MCP JSON-RPC envelope 不够贴合, 已补 `initialize` / `tools/list` / `tools/call` JSON-RPC 2.0 响应与 `McpControllerTest`。
- `OAuthController` consent preview 原先会让未知 client 的 `IllegalArgumentException` 冒出, 已改为 `BizException(ACCESS_DENIED)`。
- Critic Round 1 发现 OAuth scope 只签发不执行, 已补 `McpAuthenticationDetails`、Manifest tool `scope`、tool scope guard 与红/绿测试。
- Critic Round 1 发现 JSON-RPC error 契约未冻结, 已补 `BizException` 到 JSON-RPC error envelope 的映射与红/绿测试。
- Reviewer refresh 发现 legacy 非 JSON-RPC 分支和数字参数错误语义不一致, 已补 legacy error payload 与 `PARAM_INVALID` 参数错误。
- 目标测试命令两次写错:
  - 缺 `-am` 时 Maven 去远程解析本仓内部 `${revision}` 依赖并失败。
  - 加 `-am` 后 `-Dtest=...` 传给上游模块, Surefire 因上游模块无匹配测试失败。
  - 最终用 `-Dsurefire.failIfNoSpecifiedTests=false` 修正, targeted tests PASS。

## Reflect

- 代码级验收已覆盖: PKCE RFC 向量、授权码一次性、resource mismatch、refresh reuse revoke、Bearer filter 401 metadata、UserContext/OAuth scope/permission fail-closed、Manifest scope metadata、JSON-RPC wrapper/error envelope、legacy error payload、参数类型错误。
- 本机 PostgreSQL 未运行, 所以没有启动完整 Spring Boot 进程做 curl 级 HTTP 冒烟。此缺口不影响编译/单测结论, 但启用真实 `ai.mcp.enabled=true` 前建议在有 dev DB 的环境复跑 `/mcp` 401 和 well-known curl。
- 未引入动态客户端注册、写 tool、chat/RAG/Provider/SSE/token quota, 与 S3 非目标一致。

## VERDICT

PASS — 代码与聚合测试通过; live HTTP curl 因本机 DB 未运行未覆盖, 已作为环境缺口记录。
