## Spec Compliance (spec-compliance, 2026-07-07T02:57:01Z)

范围: route-note.md 验收标准 3 条 vs quantum-backend commit 4b0c067 + 工作区未提交修复
(docs/ai/convention-pack/{validate,db-conventions,test-conventions}.md) + quantum-front commit 8f4d5ab。

### AC1 — FE 约定包 (route-note.md L13)

要求: 约定/模板与真实 feature 模块逐文件对齐; validate 命令实跑通过; 权限守卫默认生成
(对齐 decision codegen-security-gates-default-on); 含 mock 数据约定。

- 满足: `quantum-front` commit 8f4d5ab, 9 文件 (conventions.md + 7 模板 + validate.md)。
- 满足: conventions.md 基于 `src/features/system/user/` 真实结构实探 (五件套/`page-registry.tsx`/
  `toId`/单动态路由), runtime-verify.md 场景1记录 `bun run lint` + `bunx tsc -b --noEmit` 零输出全过。
- 满足: conventions.md 含权限守卫默认生成描述, validate.md G1/G2 为"守卫存在+接入"门禁 (审查报告
  pass1-reviewer.md 未发对 FE 侧此项提出质疑, 视为已核实)。
- 满足: conventions.md 含 mock 数据约定 (`VITE_FEATURE_MOCK` env 开关 + api.ts 顶层短路), templates/
  mock.ts.tmpl 存在, G4 门禁防误提交。
- 判定: **满足**。

### AC2 — BE 增补 (route-note.md L14)

要求: 表设计文档+DDL 两个分离文档模板与约定 (方言以 deploy/init.sql 实探为准); 单测+debug loop+
测试报告模板 (报告 = YAML frontmatter + md)。

- 满足: db-conventions.md 声明双文档分离硬约定 (validate.md L94 "无豁免路径")。
- 满足: templates/schema-design.md.tmpl + templates/ddl.sql.tmpl 两个分离模板存在。
- 满足: db-conventions.md PG 方言实探结论 (int8/int4/timestamp(6)/bpchar/双引号标识符) 经
  pass1-reviewer.md "未发现问题" 段核对与 deploy/init.sql 逐条一致, 无虚构。
- 满足: test-conventions.md 含四层测试形态 + debug loop 每轮留痕描述; templates/test-report.md.tmpl
  为 YAML frontmatter + md 结构, 与 route-note 要求的 "F1 报告 schema 方向对齐" 表述一致。
- 判定: **满足**。

### AC3 — 两仓库各自 commit, 主 agent 独立复核 validate 实跑 (route-note.md L15)

要求: 两仓库各自 commit; 主 agent 独立复核 validate 实跑。

- 满足 (commit 部分): FE commit 8f4d5ab 已存在; BE commit 4b0c067 已存在 (含 db/test-conventions 初版)。
- 部分完成 (push 未执行): checklist.yaml `commits` 项 status=pending, 描述为"两仓库各自 commit + push"。
  push 依赖 evaluator VERDICT 后 ship stage 执行, 按任务框定属流程内正常状态, 不计 MISSING。
- 满足 (独立复核 validate 实跑部分, REWORK 后): 初版 (commit 4b0c067 时点) 复核方式为
  "逐文件 Read 复核", review pass1 F2 [P0] 指出未覆盖门禁实跑这一关键验收点。
  工作区未提交改动 (runtime-verify.md 场景4) 已补齐: G5a-G5e 正例 fixture 全 PASS
  (含首轮抓到 G5d 真 bug: schema 名 "public" 误判为列, 已修为"仅缩进列定义行抽取"复验 PASS);
  反例 (删互引/留 TODO/空测试类) 分别命中 G5b/G5e/G6a-G6b FAIL, fail-closed 方向正确;
  G6 正例 (标准名假测试) 全 PASS。
- 判定: **满足** (以工作区未提交的 REWORK 修复 + runtime-verify.md 场景4 为准; 若仅看 commit
  4b0c067 单独快照则为 DEVIATED, 见下)。

### DEVIATED（如仅按已提交 commit 快照评估）

- D1: route-note.md AC3 "主 agent 独立复核 validate 实跑" 在 **commit 4b0c067 时点** 实际只做了
  "逐文件 Read 复核"(runtime-verify.md 场景2, 未提交前版本), 与"实跑"要求有偏差 —
  此偏差已被 review pass1 F1/F2 [P0] 抓出, 且工作区未提交的修复 (validate.md G5/G6 改动 +
  runtime-verify.md 场景4) 已回填实跑证据, 构成**同一 sprint 内的自我纠正闭环**而非遗留偏离。
  引用: route-note.md:15 vs runtime-verify.md:24-38(修复前) → runtime-verify.md:39-50(修复后)。
- 不再计入 MISSING/DEVIATED 计数（已闭环）。

### EXTRA

- E1 [合理]: db-conventions.md 新增跨仓库互链 ("前端侧对应约定包" 一行), test-conventions.md
  标题/适用范围澄清, 均为 review F5/F7 建议项的顺手落实, 不引入 spec 未声明的新功能。
- 无 scope creep。

### Spec Compliance 总评

- MISSING 数: 0
- EXTRA 数: 1 (合理 refactor 1 个 / scope creep 0 个)
- DEVIATED 数: 0 (1 处曾偏离已在 sprint 内自我纠正闭环, 不计入未闭环 DEVIATED)
- AC1: 满足 / AC2: 满足 / AC3: 满足 (push 待 ship, 流程内正常)
- **建议**: PASS
