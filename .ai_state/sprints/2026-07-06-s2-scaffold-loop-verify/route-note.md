# Route Note — S2: scaffold-module-gen 编译闭环实跑

- 日期: 2026-07-06
- 感知: 治理文档 §8 S2 = "skill 迁入 + 编译闭环实跑"。迁入已完成 (双端 9.9.0 + 运行时); 上 sprint runtime-verify 场景 2 已验"带 dept 实体 + 加固模板 + G1-G4" 但系手动局部替换 (5 模板, 寄生在 quantum-biz-system)。
- 缺口: skill 完整工作流从未端到端跑过 — 全 11 模板 + 独立 quantum-biz-<module> 模块 + 根 pom 注册 + 编译自修环 + G1-G4 + 报告。
- 假设: 生成物为临时验证物, 验证后回滚删除; 入 git 的只有 sprint 档案、§8 状态回写、以及实跑发现的 Convention Pack/SKILL 缺口修正。
- 四维: 规模小 (临时生成)、风险低 (可回滚, 不触业务代码)、验收明确、不确定性低 (模板已两轮实证)。
- 决策: path=Feature (轻), 生成写入走 generator subagent (铁律[零写入] 黄区); 无新 design (S2 已在 docs/ai-sprint-design.md §8 定义, skill 工作流即执行规格)。
- 置信度: 0.85
- 验收标准:
  1. generator 按 scaffold-module-gen 工作流生成独立 quantum-biz-asset 模块 (带 dept 维度实体, 全 11 模板)
  2. `mvn -pl quantum-biz-asset -am -DskipTests compile` BUILD SUCCESS (自修环记录每次失败与修复)
  3. validate.md §2 G1-G4 全 PASS
  4. 现场清理后 git 无生成物残留
  5. runtime-verify.md 含实跑命令+输出; §8 表 S2 → 完成
