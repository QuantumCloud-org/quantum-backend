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

dev profile 需要外部中间件就绪 — **连接参数以开发者本地 `application-dev.yml` 为准** (host/port/凭证属
开发者环境配置, 不在本契约文件硬编码; 仓库内默认值为 127.0.0.1:5432 / 6379 形态, 各开发者可指向自有实例):

- **PostgreSQL**, 库 `baseweb` (schema 由 `deploy/init.sql` 初始化)。
- **Redis** (L1 Caffeine + L2 Redis 双级缓存)。

> 探活语义说明: `/actuator/health` 聚合 datasource 健康检查, 因此 DB/Redis 未就绪时 health 非 200 (`DOWN`)。
> 这是"依赖就绪才算健康"的预期行为; 已启用 liveness/readiness 分组 (health 响应 `groups` 字段实证)。

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

## 校准状态 (2026-07-14 实跑实证, 原 BLOCKED 已解除)

- port / context_path / health_url / dev_command / MCP 值: 从配置权威取值 (2026-07-10), 并于 2026-07-14
  **boot 实跑校准通过** (sprint `2026-07-14-be-env-compose`):

```
$ java -jar quantum-server/target/*.jar --spring.profiles.active=dev --ai.mcp.enabled=true
Application 'quantum-backend' is running!  →  http://127.0.0.1:8080/
$ curl -w '%{http_code}' http://127.0.0.1:8080/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}   →   200
$ curl -w '%{http_code}' http://127.0.0.1:8080/.well-known/oauth-protected-resource   →   200 (scopes/authorization_servers 齐)
$ curl -w '%{http_code}' http://127.0.0.1:8080/.well-known/oauth-authorization-server →   200 (authorize/token/revoke endpoints 齐)
$ curl -X POST http://127.0.0.1:8080/mcp -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
{"code":401,"message":"invalid MCP OAuth token"}    →   401 (无 token fail-closed 实证)
```

- 剩余深链 (authorize→consent→token→tools/call 带真实测试账号) 属 F7 范围, 手册见 `../mcp-test-access.md`。
