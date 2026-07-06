# Route Note

- 感知: 用户要求合并云端 Fable 5 AI 架构方案与 Codex Sprint 设计, 写完推 main, 先给 Claude 审核; 当前项目是 Spring Boot 4.1/Java 25 多模块单体。
- 假设: Quick 可只写一页方案; System/design 更符合新模块、SSE、Provider、Tool/MCP、RAG、配额与拆服务触发条件的跨系统规划。
- 权衡: 不写业务代码, 可逆性高; 但设计影响模块、数据、权限、运行时和部署, 需求清晰, 爆炸半径属系统级。
- 决策: 路径=System, stage=design, 置信度=0.90。
- 验收: 产出 requirement、design、checklist、Claude review brief; 官方来源标注; 不修改业务源码; 推送 main。
