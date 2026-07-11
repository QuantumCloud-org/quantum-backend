# quantum-backend MCP 测试接入 (repo-safe)

本文件给 end-to-end drill / `project-data-reader` 提供**接入 quantum-mcp 只读能力所需的 repo-safe 契约**:
如何开启 MCP、注册测试 client、建测试账号、走完 OAuth 2.1 拿到 access token 调 `/mcp`。

> **凭证纪律**: 本文件**不含任何真实密码 / token / 密钥**。所有敏感值用 `<YOUR_*>` 占位, 由运行者从
> 环境变量或密钥管理注入。ship 前 grep 校验无真实凭证 (见 sprint 门禁)。

## 0. 开启 MCP

MCP 默认关闭 (`ai.mcp.enabled=false`)。开启:

```bash
# application-dev.yml 或环境变量
ai.mcp.enabled=true
# 可选 (默认见 application.yml):
export AI_MCP_ISSUER=http://localhost:8080
export AI_MCP_RESOURCE=http://localhost:8080/mcp
```

## 1. 静态 client allowlist (已配置, 见 application.yml `ai.mcp.clients`)

S3 首版用静态 allowlist (动态客户端注册 RFC 7591 后置)。仓库自带一个本地 agent client:

| 字段 | 值 |
|---|---|
| client_id | `quantum-local-agent` |
| redirect_uris | `http://127.0.0.1/callback` / `http://localhost/callback` |
| scopes | `system.user.read` / `system.dept.read` / `system.role.read` |
| 客户端类型 | 公共客户端 (public), 强制 PKCE S256, 无 client_secret |

新增测试 client: 在 `application.yml` 的 `ai.mcp.clients` 下加一条 (redirect_uri 用回环地址; 公共客户端不配 secret)。

## 2. 测试账号 provisioning

- 初始超管: `deploy/init.sql` 内置 `id=1, username=admin` (口令 hash 在 init.sql, **不在本文件复制**)。
- **不要用超管跑 MCP 测试** — 超管 `*:*:*` 会掩盖数据权限判定。建独立测试账号 + 最小权限角色:
  1. 用 admin 登录后台, 建角色 `mcp-tester`, 只授 manifest tools 对应的读权限点:
     `system:user:list` / `system:user:query` / `system:dept:list` / `system:role:list` (对齐 client scopes)。
  2. 建用户 `<YOUR_TEST_USERNAME>`, 密码从环境变量注入 `<YOUR_TEST_PASSWORD>` (不入库文档), 绑定
     `mcp-tester` 角色 + 一个**非顶级部门** (这样数据权限 DEPT_AND_CHILD 分支才被真实走到, 不是 ALL)。

## 3. OAuth 2.1 全流程 (agent 自动化, 用户不手动粘 token)

已实现端点 (`quantum-mcp` 模块):

| 步骤 | 端点 | 说明 |
|---|---|---|
| 1 受保护资源元数据 | `GET /.well-known/oauth-protected-resource` | agent 首次 401 时据此发现授权服务器 (RFC 9728) |
| 1' 授权服务器元数据 | `GET /.well-known/oauth-authorization-server` | authorize/token 端点发现 |
| 2 授权 + 同意 | `GET /oauth/authorize` | 复用登录态 + consent 页; 参数 `client_id` + `redirect_uri` + `code_challenge` (S256) + `resource` + `scope` |
| 3 换 token | `POST /oauth/token` (form) | 授权码 + PKCE `code_verifier` → 短时 access token + refresh token |
| 4 调用能力 | `POST /mcp` (Bearer) | `Authorization: Bearer <access_token>`, JSON-RPC `initialize` / `tools/list` / `tools/call` |
| 撤销 | `POST /oauth/revoke` (form) | 主动吊销; 用户下线/改密也失效 |

**身份传递铁律**: 调 `/mcp` **只传 `Authorization: Bearer <token>`**, 禁止 `X-User-Id` / `X-Dept-Ids` /
`X-Permissions` 等私有身份头 (服务端从 token 重建 LoginUser, 走 @RequiresPermission + DataScope)。

多资源绑定: authorize/token 阶段用 RFC 8707 `resource` 参数; tool 调用期 token 内 resource 与 `/mcp` endpoint 比对。

## 4. 冒烟示例 (占位, 需先起服 + 中间件就绪)

```bash
# 发现元数据
curl -s http://127.0.0.1:8080/.well-known/oauth-protected-resource | jq .
# authorize (浏览器完成 PKCE + consent) → 拿 code
# 换 token
curl -s -X POST http://127.0.0.1:8080/oauth/token \
  -d grant_type=authorization_code -d client_id=quantum-local-agent \
  -d code=<AUTH_CODE> -d code_verifier=<PKCE_VERIFIER> \
  -d redirect_uri=http://127.0.0.1/callback -d resource=http://localhost:8080/mcp | jq .
# 调 tools/list
curl -s -X POST http://127.0.0.1:8080/mcp \
  -H "Authorization: Bearer <ACCESS_TOKEN>" -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq .
```

> **实跑证据状态 (2026-07-10)**: 与 runtime-env.md 同, boot 需 PostgreSQL+Redis 就绪, 校准时两者 DOWN,
> 全链实跑证据待环境补录。端点/client/scope 值均从 application.yml + 已 ship 的 quantum-mcp 源码权威取值。
