# docs/ai — AI 开发工具资产索引

本目录是 AI Sprint（详见 [`../ai-sprint-design.md`](../ai-sprint-design.md)）的落地资产。
**quantum-backend 不含任何 AI 运行时**；这里只有**平面 B 的契约实现** — 给 AI 生成/读取用的约定。

```
docs/ai/
├── convention-pack/            # 平面 B —— quantum-backend 的契约①实现（本仓库的活）
│   ├── conventions.md          #   生成约定：分层/命名/权限/数据权限 + 验证实体选择原则
│   ├── db-conventions.md       #   数据库设计约定
│   ├── test-conventions.md     #   测试约定
│   ├── runtime-env.md          #   启动命令 / 端口 / 探活 URL（drill 与 skill 消费）
│   ├── templates/              #   各层骨架模板（占位 {{Entity}} 等）
│   └── validate.md             #   校验命令 + 自修流程 + 人工确认项
└── mcp-test-access.md          # 契约② 的测试接入手册（OAuth 2.1 流程 + 测试账号 provisioning，repo-safe）
```

## 历史注记：skills/ 已迁出（2026-07-14 清理）

早期（S0, 2026-07-06）本目录曾放两个 skill 的**孵化起点**（`scaffold-module-gen` / `project-data-reader`），
设计文档 §5.5 当时即声明"最终迁入 aether/pace"。它们已在 Rlues（提示词架构仓）正式落户并演化两代：

- `scaffold-module-gen` → 9.9.3 的 **`quantum-codegen`**（单 skill 六 mode: page/module/db/unit/security/e2e）
- `project-data-reader` → 9.9.3 的 **`quantum-data`**

skill 的唯一真相源 = `Rlues/vibeCoding/{claude,codex}/{version}/`；本仓库不再保留副本（避免双真相源）。

## 边界提醒

- `convention-pack/` 是 quantum-backend 专属（平面 B）；换脚手架就换一份 convention-pack，skill 不动。
- 运行期 MCP 能力适配（契约②，`quantum-mcp` 模块，标准 MCP 协议）在设计文档 §3.2 / §7，S3 已交付。
