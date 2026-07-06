# Route Note

- 感知: 用户确认开始实现 design.md 中第一轮 7 个后端安全修复点。
- 假设: Bugfix 可覆盖单点安全洞, Refactor 更符合跨模块修复和测试补齐。
- 权衡: 涉及 security / biz-system / common-file / server / test, 预估超过 5 文件, 爆炸半径中高。
- 决策: 路径=Refactor, stage=impl, 置信度=0.92。
- 验收: 7 个修复点均有回归测试, `mvn test` 通过。
