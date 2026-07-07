# Runtime Verify — FE/BE Convention Pack 扩充 (2026-07-07)

## 验证方式

两个 generator subagent 分仓库产出, 主 agent 独立复核 (BE 侧分类器当时不可用, 执行逐文件 Read 全量复核替代抽查)。

## 测试场景 (实跑)

### 场景 1: FE 约定包校验命令实跑 (generator a1d881a8d6a040fb7, 主 agent 采信其记录 + 抽查产物)

```
$ bun run lint          # 零输出全过
$ bunx tsc -b --noEmit  # 零输出全过
$ # validate.md G1-G6 对真实 system/user 模块烟测: 全 PASS
$ # 其中 G3 首版 grep "{{" 在 users-provider.tsx 的 JSX value={{...}} 上误报,
$ # 已修为精确正则 \{\{[A-Za-z_]+\}\} 后复验 PASS — 门禁自身经过了假阳性校准
```

关键侦查结论 (写入 conventions.md): 新页面**不新增路由文件** — 单动态路由
`$section/$page.tsx` 经 `page-registry.tsx` 数组 + 后端菜单树双重校验挂载;
`backendComponent` 字符串必须与后端 `sys_menu.component` 逐字一致 (人工清单已含两侧核对项)。
mock 机制系从零设计 (全仓 grep 确认原无): `VITE_FEATURE_MOCK` env 开关 + api.ts 顶层短路 + G4 防误提交门禁。

### 场景 2: BE 约定包主 agent 全量复核

逐文件 Read 复核 6 文件: db-conventions.md (PG 方言实探正确、dept_id 五条判定规则含 sys_dept
树形特例、双文档无豁免硬约定) / test-conventions.md (四层测试形态自真实测试类提炼、5 必测用例、
豁免须正向验证) / schema-design.md.tmpl + ddl.sql.tmpl (审计五件套与 init.sql 逐列一致) /
test-report.md.tmpl (frontmatter 机器可解析) / validate.md (G5/G6 续排, 风格一致)。

落盘时修正 2 处: schema-design.md.tmpl 引用 "4 条判定规则"→"5 条" (与 db-conventions 同步);
G6a 的 `grep || grep && echo` 优先级歧义 → 单正则。

### 场景 3: generator 反向验证 (sys_dept, 未落盘)

BE generator 用 init.sql 真实表 sys_dept 反向填模板确认占位齐全, 并因此发现判定规则盲区
(组织架构树自引用表) → 补第 5 条规则。证据见其报告 (tool-trace)。

## 场景 4 (REWORK 后补, 2026-07-07): G5/G6 门禁正反双向实跑

review pass1 P0×2 (F1: G6 正则与真实测试不匹配; F2: 场景 2 仅 Read 复核未实跑门禁) 的修复验证:

- 修复: G6 改为按 test-conventions 标准方法名精确检测 + 明确"只跑生成物不约束存量"适用范围;
  G5 补 G5e (数据域归属判定必填); test-conventions 标题/适用范围澄清; db-conventions 补跨仓库互链。
- 实跑 (正例最小 fixture): G5a-G5e 全 PASS — 其中**首轮实跑即抓到 G5d 真 bug**
  (列名抽取把 schema 名 "public" 误判为列 → 必然 FAIL), 修为"仅缩进列定义行抽取"后复验 PASS。
- 实跑 (反例): 删互引 → G5b FAIL; 留 TODO → G5e FAIL; 空测试类 → G6a/G6b FAIL — fail-closed 方向正确。
- 实跑 (G6 正例, 标准名假测试): G6a-G6e 全 PASS。

教训 (回应 F2): 门禁类产物的验收必须含"正例过 + 反例拦"双向实跑, Read 复核只能验文字一致性。

## 遗留 / 收口

- generator worktree (agent-ad6b93c24b2355f4e) 已清理。2026-07-07 复核: `git worktree list`
  只剩主仓库 main; `/Users/mi_manchi/workspace/quantum/quantum-backend-worktrees` 不存在。
- FE G1-G6 门禁未在"新生成页面"上跑过 (仅烟测存量 user 模块) — F2 的 scaffold-page-gen 端到端实跑时补。

## 结论

验收标准 1/2 达成 (FE validate 实跑 + 权限守卫默认生成 + mock 约定; BE 双文档模板 + PG 实探 + 测试/报告约定)。
标准 3 (双仓库 commit) 已在 ship 步完成: BE `865a7bf` / FE `8f4d5ab`, 两仓库 `origin/main...main = 0 0`。
