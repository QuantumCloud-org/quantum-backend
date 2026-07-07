# AI 协作基座 (docs/ai)

更新: 2026-07-07 (sprint: 2026-07-07-quantum-mcp-s3-preflight-design)

## 定位

两条 AI 能力主线, 均为**文档/协议资产**, 本仓库暂无 AI 运行时代码:

| 主线 | 载体 | 状态 |
|---|---|---|
| 生成期 (dev-time): AI 按约定生成业务模块 | `docs/ai/convention-pack/` + `scaffold-module-gen` skill | 已交付 |
| 运行期 (runtime): 外部 Agent 经 MCP 按操作者身份读业务数据 | `project-data-reader` skill + `quantum-mcp` 模块 (S3) | 开工前设计冻结, 待实现 |

治理文档: `docs/ai-sprint-design.md` (Round1/Round2 收敛记录: chat/RAG/Provider/SSE/配额移出本仓库)。

## Convention Pack (生成期)

- `docs/ai/convention-pack/conventions.md` — 分层/命名/权限/数据权限约定
- `templates/*.tmpl` — 11 个各层模板; **数据权限校验默认启用 (fail-closed)**, 无部门维度实体须显式豁免 (`data-scope-exempt` 行)
- `validate.md` — 三段校验: 编译 → 安全门禁 G1-G4 (grep 检测 assert 调用与 SQL 占位残留) → 人工清单
- 设计决策见 compound/2026-07-06-decision-codegen-security-gates-default-on.md

## MCP 能力服务 (运行期, S3 待实现)

- 授权: OAuth 2.1 标准流程 (RFC 9728 资源元数据 + PKCE), 禁止长期 token 落盘
- 权限裁决在服务端: token 映射回登录用户 → 复用 @RequiresPermission + 数据域链路, fail-closed
- 硬性技术要求 (design 定案): UserContext fail-closed 传递、@Sensitive 序列化路径复用
- 2026-07-07 S3 前置设计冻结:
  - token 存储: 独立 `quantum:oauth:*` store, 不与普通 Web access token 互通; 复用 LoginUser / CacheClient / 用户吊销语义
  - consent: 复用登录态 + 新增授权同意页; 授权码绑定 client/redirect/PKCE/resource/user, 一次性短 TTL
  - Manifest v1: `resource + tools[] + permission + dataScopeMode + riskLevel + schema`; 调用只用 `Authorization: Bearer`

## Skills 分发

`docs/ai/skills/{project-data-reader,scaffold-module-gen}/SKILL.md` 为源, 经 `install-to-rlues.sh`
进 Rlues 仓库 `vibeCoding/{claude,codex}/9.9.0`, 再同步到 `~/.claude/skills` + `~/.codex/skills` 生效。
