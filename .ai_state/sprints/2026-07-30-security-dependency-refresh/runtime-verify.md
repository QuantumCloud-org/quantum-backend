# quantum-backend 运行时验证

## 完成条件与停止条件

- Checker：全 reactor `mvn clean test`、升级后 effective POM/SBOM/OSV、dependency tree、真实 HTTP 状态码与 fail-closed 断言。
- 场景：正常启动/健康、默认 MCP disabled、未授权业务请求，以及外部 PostgreSQL/Redis 不可用时的可复现失败证据。
- 最多 3 轮启动尝试；仅使用本机与仓库已有 dev 配置，不访问生产或共享写接口，不猜测或打印额外凭据。
- 允许变更仅限已批准的 root `pom.xml` 和本 sprint `.ai_state` 证据；若发现依赖兼容性源码缺口，回到隔离 worktree 交 generator 最小修复。

## 测试场景

| 场景 | 实跑命令/环境 | 关键实际输出 | 结果 |
|---|---|---|---|
| 全量回归 | `mvn clean test`，Java 25 / Maven 3.9.14 | 11/11 reactor modules `SUCCESS`；101 tests，0 failure/error/skipped；10.359s | PASS |
| 可执行包 | `mvn -pl quantum-server -am package -DskipTests` | 11/11 modules `SUCCESS`；Spring Boot repackage 生成 `quantum-server.jar` | PASS |
| 正常启动与外部依赖 | `java -jar quantum-server/target/quantum-server.jar`，dev profile | PostgreSQL 启动自检通过；cache.mode=local，按配置跳过 Redis；Tomcat 8080 启动，应用 2.921s ready | PASS |
| 健康检查 | `GET http://127.0.0.1:8080/actuator/health` | HTTP 200，`{"groups":["liveness","readiness"],"status":"UP"}` | PASS |
| 未授权业务请求 | 无 token `GET /system/user/page` | HTTP 401，统一响应 `code=401`、`message=未认证` | PASS |
| MCP 默认关闭的 fail-closed 边界 | 默认配置无 token `GET /mcp`；随后仅在 8081 临时把 `/mcp` 加入白名单并 `POST initialize` | 默认链路 HTTP 401；绕过通用认证后日志为 `NoResourceFoundException: No static resource mcp`，证明 `McpController` 未注册、请求未进入 MCP；统一异常层将该缺失路由返回 HTTP 500 | PASS（观察项） |
| 依赖树与漏洞复核 | dependency tree + CycloneDX 211 components + OSV Query Batch 209 purls | 8 个目标实际解析版本正确；OSV 0 vulnerable components / 0 advisories | PASS |

观察项：缺失路由由 `GlobalExceptionHandler` 统一映射为 HTTP 500 而不是 404。这是扫描基线中的既有通用异常语义，不是本次依赖升级引入的兼容性回归；MCP 仍保持不可达、未 fail-open，本轮不扩大为业务异常处理重构。

## 自测自改记录

1. 隔离 worktree 首次目标版本断言在基线退出 1（RED）；升级 POM 后同一断言退出 0（GREEN），11 模块 compile 通过。
2. 主 checkout 落入同一 POM diff 后重新执行 `mvn clean test`，无需源码兼容修复。
3. 第一次默认启动完成健康与 401 验证；第二次仅用命令行临时白名单隔离认证层，确认 MCP controller 在 `ai.mcp.enabled=false` 时未注册。两次均使用 Ctrl-C 完成 graceful shutdown，无残留进程。

## Reflect

- AC3/AC4/AC5/AC6/AC7 已由 effective resolution、clean test、dependency tree、OSV 与真实 HTTP 覆盖。
- PostgreSQL 环境实际可用，未触发外部环境 blocker；Redis 因 `cache.mode=local` 按设计不属于本次启动前提。
- 未发现依赖升级导致的新 API、启动、健康或鉴权回归，因此不回到 impl。
- 通用 404→500 语义作为既有观察项进入安全报告/后续治理，不阻塞本次依赖升级进入 review。

## VERDICT

PASS
