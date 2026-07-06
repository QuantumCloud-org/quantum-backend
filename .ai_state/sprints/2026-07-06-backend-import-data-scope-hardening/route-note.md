# Route Note

- 感知: Claude review 前继续实现非 AI 体系剩余项; 当前缺口集中在用户导入契约、service 数据域 fail-closed、数据权限来源收敛和端到端测试。
- 假设: Bugfix 可修两个 review finding; Refactor 更符合数据权限语义与测试补齐的跨文件改动。
- 权衡: 涉及 controller/service/dto/login-state/test/.ai_state, 预估超过 5 文件, 回归风险中等但可由单测和 MVC 契约测试覆盖。
- 决策: 路径=Refactor, stage=impl, 置信度=0.88。
- 排除: 本轮不做 contracts/codegen/CLI/MCP 等 AI 生成/审查体系。
- 验收: 导入契约闭环、无 UserContext fail-closed、登录态数据权限单一来源、`mvn test` 通过。
