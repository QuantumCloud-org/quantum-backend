# Route Note — FE/BE Convention Pack 扩充 (F2/F3 quantum 侧)

- 日期: 2026-07-07
- 感知: 双 session 并行分工 — Rlues session 跑 F1 框架设计, 本 session 做 quantum 侧约定包。
  接口契约: skill 读约定包 (S2 已实证), 故约定包可先行, F2/F3 的 skill 后接。
- 范围 (跨仓库):
  a. quantum-front/docs/ai/convention-pack/ 新建 (conventions + templates + validate) — 基于 shadcn-admin 真实结构
  b. quantum-backend/docs/ai/convention-pack/ 增补 db-conventions + test-conventions + 对应模板
- 决策: path=Feature, 黄区多文件写入 → 两个 generator subagent (双仓库无写冲突, 并行)。
  sprint 治理档案落本仓库 (quantum-front 无 .ai_state), 产物按归属落各仓库。
- 置信度: 0.85
- 验收标准:
  1. FE 约定包: 约定/模板与真实 feature 模块 (system/user 等) 逐文件对齐; validate 命令在现工程实跑通过; 权限守卫默认生成 (对齐 decision codegen-security-gates-default-on); 含 mock 数据约定 (支撑 14 步流程的"前端 demo mock 优先")
  2. BE 增补: 表设计文档与 DDL **两个分离文档**的模板 + 约定 (方言以 deploy/init.sql 实探为准); 单测+debug loop+测试报告模板 (报告 = YAML frontmatter + md, 与 F1 报告 schema 方向对齐)
  3. 两仓库各自 commit, 主 agent 独立复核 validate 实跑
