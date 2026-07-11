# quantum-backend Runtime Env

本文件是 `scaffold-module-gen` / `project-data-reader` / end-to-end drill 消费的 runtime-env 声明。
**只读本文件推断启动方式与探活 URL, 不要从 pom / application.yml 现场推断。** 契约变更时更新本文件。

| key | value |
|---|---|
| dev_command | `mvn -pl quantum-server -am spring-boot:run -Dspring-boot.run.profiles=dev` |
| port | `8080` |
| context_path | `/` |
| health_url | `http://127.0.0.1:8080/actuator/health` |
| teardown | 前台进程 Ctrl-C, 或 kill 占用 8080 的进程 |

## 依赖 (启动前必须就绪)

dev profile 需要外部中间件在本机运行 (见 `application-dev.yml`):

- **PostgreSQL** `127.0.0.1:5432`, 库 `baseweb` (dev 默认账号见 application-dev.yml)。
- **Redis** `127.0.0.1:6379` (L1 Caffeine + L2 Redis 双级缓存, dev 默认口令见 application-dev.yml)。

> 探活语义说明: `/actuator/health` 聚合 datasource 健康检查, 因此 DB/Redis 未就绪时 health 非 200 (`DOWN`)。
> 这是"依赖就绪才算健康"的预期行为; 若未来需要纯存活探针 (不聚合下游), 另配 liveness/readiness 分组。

## MCP 端点 (可选, 默认关闭)

MCP 只读能力适配 (`quantum-mcp` 模块) 默认关闭, 不给不需要的部署引入表面积:

| key | value |
|---|---|
| enable_flag | `ai.mcp.enabled=true` (默认 `false`) |
| mcp_endpoint | `http://127.0.0.1:8080/mcp` |
| oauth_metadata | `http://127.0.0.1:8080/.well-known/oauth-protected-resource` |
| issuer (env) | `AI_MCP_ISSUER` (默认 `http://localhost:8080`) |
| resource (env) | `AI_MCP_RESOURCE` (默认 `http://localhost:8080/mcp`) |

开启 MCP + 走 OAuth 测试账号获取 token 的完整步骤见 [`../mcp-test-access.md`](../mcp-test-access.md)。

## 校准状态 (2026-07-10)

- port / context_path / health_url / dev_command / MCP 值: 从 `application.yml` + `application-dev.yml` + pom **权威取值**。
- **boot 实跑 200 证据: BLOCKED** — 校准时实测 PostgreSQL(5432) 与 Redis(6379) 均未运行, 无法起服探活。
  与 F6 drill 的 blocked dynamic cases 同性质 (无运行环境降级)。本文件值可信 (源自配置真相), 但
  "curl /actuator/health 200" 的实跑证据待中间件就绪的环境补录 (见 sprint runtime-verify.md)。
