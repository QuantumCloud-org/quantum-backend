# docs/ai — AI 开发工具资产索引

本目录是 AI Sprint（详见 [`../ai-sprint-design.md`](../ai-sprint-design.md)）的落地资产。
**quantum-backend 不含任何 AI 运行时**；这里只有两类东西：给 AI 生成/读取用的**约定与 skill 起点**。

```
docs/ai/
├── skills/                     # 平面 A —— 两个脚手架无关的 Agent Skill 起点
│   │                           #   （最终迁入你的 aether/pace，遵循官方 SKILL.md 规范）
│   ├── scaffold-module-gen/    #   ① 基于需求 + 框架约定，快速生成模块（dev-time，写源码）
│   │   └── SKILL.md
│   └── project-data-reader/    #   ② 通过 MCP 读取运行中项目的数据能力（runtime，只读）
│       └── SKILL.md
│
└── convention-pack/            # 平面 B —— quantum-backend 的契约①实现（本仓库的活）
    ├── conventions.md          #   生成约定：分层/命名/权限/数据权限
    ├── templates/              #   各层骨架模板（占位 {{Entity}} 等）
    │   ├── Entity.java.tmpl
    │   ├── ServiceImpl.java.tmpl
    │   └── Controller.java.tmpl
    └── validate.md             #   校验命令 + 自修流程 + 人工确认项
```

## 两个 skill 一句话区分

| skill | 时机 | 作用对象 | 碰源码 | 碰运行时数据 |
|---|---|---|---|---|
| `scaffold-module-gen` | dev-time | 脚手架源码仓库 | 是 | 否 |
| `project-data-reader` | runtime | 运行实例（经 MCP） | 否 | 是（穿权限+脱敏） |

## 边界提醒

- `skills/` 里两个 SKILL.md 是**脚手架无关**的起点，属于你的 aether/pace（平面 A），放这里只是给你/codex 一个现成骨架；
  它们不依赖 quantum-backend，接 quantum-front / 客户脚手架时不改。
- `convention-pack/` 才是 quantum-backend 专属（平面 B）；换脚手架就换一份 convention-pack，skill 不动。
- 运行期 MCP 能力适配（契约②，`quantum-mcp` 模块，标准 MCP 协议）在设计文档 §3.2 / §7，S3 阶段交付。
