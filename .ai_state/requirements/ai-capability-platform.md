---
slug: ai-capability-platform
status: active
created: 2026-07-06
linked_sprints:
  - 2026-07-06-ai-capability-architecture-design
---

# 需求: AI 能力平台接入

## 原始用户故事

用户希望把云端 Fable 5 提出的 AI 能力接入方案, 结合 Codex 对现有后端的 Sprint 设计, 整理成可交给 Claude 审核的后端 AI 架构方案。目标不是立即写 AI 业务代码, 而是先确定模块边界、Provider 抽象、SSE 流式接口、Tool/MCP 复用 RBAC、RAG 存储、配额审计和未来拆服务触发条件。

## 当时的权衡

- 要: 新增 `quantum-biz-ai` 的模块化单体方案, 让 AI 能力先随单体交付, 同时按未来 `ai-service` 拆分边界设计。
- 要: 模型供应商可切换, 覆盖 Claude 官方 SDK 和 DeepSeek/Qwen OpenAI-compatible 形态。
- 要: 工具调用必须复用既有权限和数据权限链路, 模型只提出工具请求, 业务层仍做最终权限裁决。
- 要: SSE 长连接、token 配额、RAG、审计和敏感信息处理在第一版设计中就进入验收。
- 不要: 本 sprint 不实现 contracts/codegen/CLI/MCP 自动创建后端代码, 只预留 ToolRegistry 到 MCP Server 的包装边界。
- 不要: 不把模型价格、模型 ID、SDK 版本硬编码成长期事实, 上线日按官方文档复核。
- 取舍: 先用内部 `LlmProvider` SPI 包住模型差异, Claude 高级能力可直连官方 SDK, OpenAI-compatible 提供商可走统一 adapter; 等 Provider 差异稳定后再决定是否全量迁移到 Spring AI `ChatModel`。

## 高层验收

- [ ] Claude review 能直接判断 `quantum-biz-ai` 模块边界是否可拆服务。
- [ ] 设计覆盖 SSE 与现有 `RepeatSubmitFilter` / `RateLimitFilter` / compression / 浏览器鉴权的冲突点。
- [ ] 设计覆盖显式 `LoginUser` 传递, 不依赖流式异步中的 `UserContext` 隐式继承。
- [ ] 设计覆盖 Tool Use 权限裁决、审计、限环、写操作确认和未来 MCP 包装。
- [ ] 设计覆盖 RAG 表、向量库选择、数据权限过滤和 token 配额。
- [ ] 设计列出官方文档来源, 标注版本/价格需上线日复核。

## 逃生通道备注

如果后续弃码重生, 必须保留三个边界: AI 编排不直接访问 mapper; Tool 执行复用业务 service + RBAC/DataScope; AI 模块的长连接/配额/审计从第一版开始独立于 CRUD 业务设计。
