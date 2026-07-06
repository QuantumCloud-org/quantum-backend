# Claude Review Brief

## 请重点审核

1. `quantum-biz-ai` 先跑在单体里, 但依赖方向按未来 `ai-service` 拆分设计, 是否合理。
2. `LlmProvider` 自研薄 SPI + Spring AI adapter 的折中是否合适; 是否应直接采用 Spring AI `ChatModel` 作为唯一抽象。
3. Claude provider 是否应直连官方 Java SDK 以保留 prompt caching、tool use、streaming、thinking 等能力。
4. SSE 方案是否遗漏 Spring Boot 4.1 / Tomcat 11 / 虚拟线程下的连接释放、超时、compression 配置。
5. Tool Use 是否足够复用现有 RBAC/DataScope; 是否需要新增 tool-level 策略表。
6. pgvector 第一版是否足够; 是否需要预留 VectorStore SPI 和 embedding 维度迁移策略。
7. prompt/response 审计默认脱敏保存是否足够; 是否允许加密保存原文。

## 本轮明确不做

- 不写 `quantum-biz-ai` 源码。
- 不新增数据库 migration。
- 不实现 CLI/MCP 自动生成后端代码。
- 不把模型价格和模型 ID 固化到代码。

## 已核对来源

- Anthropic Java SDK 当前 README 示例版本为 `2.48.0`, 旧方案的 `2.34.0` 需要更新或上线前再复核。
- Anthropic pricing 页面显示 Claude Sonnet 5 过渡价到 2026-08-31, 因此价格策略必须配置化。
- WHATWG EventSource 构造参数无自定义 header, 所以前端默认 fetch stream 或 ticket fallback。
- Spring AI 提供 `ChatModel` / `StreamingChatModel`; pgvector 可作为 Spring AI VectorStore。
