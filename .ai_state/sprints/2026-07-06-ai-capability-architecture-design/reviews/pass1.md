## Reviewer (代码层 findings, 2026-07-06T12:00:00Z)

### P0 (REWORK 级)

无。设计 sprint 无业务代码上线；治理文档 docs/ai-sprint-design.md 已按 Round1/Round2 收敛（chat/RAG/Provider/SSE/配额移出本仓库），与 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md 契约驱动路线（codegen 先于 MCP 自动写代码）无冲突；quantum-mcp 仍待实现（S3 pending），未提前引入运行时代码或密钥。

### P1 (CONCERNS 级)

1. ServiceImpl 模板的写侧数据权限校验是注释掉的 TODO, 非编译期强制, runtime-verify 未验证到该分支
   - File: docs/ai/convention-pack/templates/ServiceImpl.java.tmpl:67-68 (update), :55 (insert), :79 (deleteByIds)
   - 问题: 三处均为 "// TODO: 写操作数据权限校验..." / "// assertWritable(old);"（被注释掉）。对照真实 quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java:166,202,227,257,365 都是真实调用 assertTargetUserWritable/assertDeptInDataScope。而 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:30-33 记录的 P1 IDOR-write（导入路径裸 updateById 越权改写数据域外用户）正是同一类缺陷。模板把这条硬性要求降级为注释提示, validate.md:25（人工确认清单）是唯一兜底, 且清单勾选无强制机制——若跳过 TODO, mvn compile 依然 BUILD SUCCESS, 与 conventions.md/validate.md:3 宣称的"正确性由编译器兜底"不符: 编译器只能兜底语法, 不能兜底权限逻辑被略过。
   - 证据: runtime-verify.md:6-7 明确用无部门维度的 SysNotice 试算模块验证编译, 写侧越权分支（有 dept 维度实体）从未被实际编译/测试路径触达, 实证范围小于宣称的"模板全层齐套"。
   - 建议: 二选一 —（a）validate.md 自修流程加硬性 grep 门禁（生成后检测 update/delete 方法体内必须出现 assert.*Writable|assert.*DataScope 字样, 否则判定未完成）;（b）在 conventions.md 明确注明"数据权限校验为人工必查项, 非编译期保证"。
   - 引用: rules/coding-standards.md P0 安全（用户输入必须验证）、P0 Sisyphus 完整性。

2. 读侧 selectById 同样只留 TODO 注释, 复现 2026-07-05 P1 IDOR-read 模式
   - File: docs/ai/convention-pack/templates/ServiceImpl.java.tmpl:48
   - 问题: "// TODO: 若为敏感数据，补充读数据权限校验（参照 SysUserServiceImpl.assertTargetUserReadable）"。对照 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:29 记录的真实 IDOR-read（selectUserById 曾裸 getById, 现已用 assertTargetUserReadable 修复）。模板复刻的是"裸 getById + TODO"的修复前状态, 对新模块相当于把已修复的漏洞模式当默认起点重新引入。
   - 建议: validate.md 人工确认清单补一条"若实体含 dept 维度或敏感字段, selectById 必须调用读权限校验, 不得裸 getById"。当前清单（validate.md:25-29）唯独没有单独点名 selectById 读侧。
   - 引用: rules/security-checklist.md 权限检查 P0。

3. menu-permission.sql.tmpl 的子节点 ID 生成方式是字符串拼接 magic pattern, 非可执行 SQL 表达式
   - File: docs/ai/convention-pack/templates/menu-permission.sql.tmpl:6-9
   - 问题: {{menuId}}1, {{menuId}}2, {{menuId}}3, {{menuId}}4 依赖模板变量替换后人工目测拼接十进制数字（如 menuId=100 → 1001/1002/1003/1004）, 不是数据库自增或表达式。位数变化或替换方式不同会产生 ID 冲突, 且 validate.md 只校验 Java 编译, 未校验 SQL 可执行性（对照 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:235 建议的"migration 可执行性测试"）。
   - 建议: 改用真实 SQL 自增或显式列出独立占位（childMenuId1..4）, 或补 SQL 语法/去重校验步骤。
   - 引用: rules/coding-standards.md P0 DRY/Magic number。

### P2 (建议)

1. conventions.md/validate.md 措辞"正确性由编译器兜底"与实际权限校验非编译期保证之间的落差
   - File: docs/ai/convention-pack/validate.md:3
   - 建议: 改为"结构/语法正确性由编译器兜底, 权限/数据域正确性依赖人工确认清单", 避免读者误判校验强度。

2. Entity.java.tmpl 的 dept_id 注释提示依赖使用者自行判断
   - File: docs/ai/convention-pack/templates/Entity.java.tmpl:22
   - 建议: 非阻塞, ServiceImpl.java.tmpl 已有对应警示注释（:31-32）, 可考虑后续补静态检查（超出本 sprint 范围）。

### 无 finding 的维度

- Correctness（设计断言与仓库事实）: 治理文档 Round1/Round2 定位收敛（chat/RAG 移出仓库、ToolRegistry 归 quantum-mcp）与用户拍板记录一致; docs/ai-sprint-design.md:258-264 OAuth 2.1 定案、S3 两条硬性技术要求（fail-closed UserContext、@Sensitive 序列化路径）与现有 DataScopeAspect/@Sensitive 机制核实吻合, 无幻觉引用。Controller.java.tmpl:35 全量 @Validated 覆盖修复了 2026-07-05 P2#3 分页校验死代码问题, 属正向亮点。
- Security（skill 层面）: project-data-reader/SKILL.md:45 明确"禁止把长期 token 写进 agent 配置文件或 prompt"; install-to-rlues.sh 全部变量加引号、set -euo pipefail、路径存在性检查, 无命令注入/路径穿越风险。
- Design 一致性（依赖引入）: 未新增运行时依赖; quantum-mcp 仅设计未落代码, 与 2026-07-05 定案的"角色聚合优先, SysUser.dataScope 仅回退"语义未冲突。
- Test risk: runtime-verify.md 提供真实 mvn compile 实证（非纸面断言）, 但覆盖面有限（见 P1#1）, 已按 P1 记录而非单独 P0, 因为本 sprint 交付物是模板非生产代码。

## Spec Compliance (spec-compliance, 2026-07-06T11:44:12Z)

范围: git diff 8dde00b..c0683b1 (22 files, +1018/-4)。design.md 经两轮 critic 重定范围 (Round1 VERDICT NEEDS_REVISION → Round2 re-scope)，权威范围以 docs/ai-sprint-design.md §8 阶段表 + checklist.yaml 为准: 本 sprint 实际交付 = S0(设计文档) + S1(Convention Pack 全层模板 + runtime-verify)；S2/S3/S4/S5 均标注"待办"。

### MISSING (做少了)
无。checklist.yaml 中 status=done 的 12 项全部有对应 diff 产物:
- convention-pack-complete → docs/ai/convention-pack/templates/ 11 个 .tmpl (Entity/Mapper/IService/ServiceImpl/Controller/Query/Create/Update/VO/Convert/menu-permission.sql)，与 conventions.md L74 声明的组成一致
- runtime-verify-trial → runtime-verify.md 记录 mvn compile BUILD SUCCESS 实证，且其声称"回写 conventions.md groupId 约定缺口"在 conventions.md:79-81 可核实存在
- claude-critic-round1 / rescope-round2 → design.md#Round 1/Round 2 段落存在
- 两个 skill (scaffold-module-gen / project-data-reader) → docs/ai/skills/ 下 SKILL.md 齐备，frontmatter 与 ai-sprint-design.md §5.5 示例逐字一致
- quantum-mcp-s3 状态为 pending (非 done)，无产物属预期，不计 MISSING

### EXTRA (做多了)
- E1 [合理补充]: docs/ai/skills/install-to-rlues.sh — 设计文档未逐字提及此安装脚本文件，但属于"两个 skill 迁入 aether/pace"既定目标 (ai-sprint-design.md §5.5/§6/§7) 的自然收尾工具，不引入新范围，判定合理。
- E2 [合理补充]: commit c0683b1 消息含"MCP OAuth 2.1 授权定案"，但 diff 中无任何 quantum-mcp 模块代码、无 OAuth 路由实现 (grep oauth/quantum-mcp 均为空)，实际只在 project-data-reader/SKILL.md 与 ai-sprint-design.md §9 记录为"决策已定案、S3 待实现"。文字先行不算代码 scope creep，且 checklist 明确标 quantum-mcp-s3: pending，未过度承诺。
- 无 scope creep 项（无 quantum-biz-ai / SSE / RAG / Provider SPI / token 配额代码，均按 Round2 裁决正确移出本仓库范围）

### DEVIATED (做偏了)
无重大偏离。细节核对:
- design.md 原 Round0 方案 (quantum-biz-ai 单体先行) 被 Round1 critic 判 P0 冲突后经 Round2 re-scope 作废，属**同一文档内部自我修正并记录**，非"文档说 A 实际做 B"的偏离，不计入 DEVIATED。
- Convention Pack 实际内容 (conventions.md 分层/命名/权限/DataScope/MyBatis-Flex 惯例 + templates + validate.md) 与 ai-sprint-design.md §2 契约①定义的四组成 (约定文档/代码模板/校验命令/能力清单映射) 一一对应，menu-permission.sql.tmpl 落地了"能力清单映射"要求。
- runtime-verify.md 声明"现场清理，试算模块不进 git 历史" — 核实 diff/name-only 中确无 quantum-biz-notice 相关文件，说法属实。

### 总评: PASS

## Evaluator (综合判定, 2026-07-06T12:30:00Z)

### Evidence Cross-Check

| task (checklist.yaml) | evidence | 核实 |
|---|---|---|
| req | .ai_state/requirements/ai-capability-platform.md | 路径存在 → ✅ |
| official-sources | design.md#官方来源 | 锚点存在 → ✅ |
| module-boundary | design.md#模块边界 | 锚点存在 → ✅ |
| provider | design.md#provider-抽象 | 锚点存在 → ✅ |
| sse-security | design.md#sse-与现有安全链冲突 | 锚点存在 → ✅ |
| tool-mcp | design.md#tool-use--mcp | 锚点存在 → ✅ |
| rag-quota | design.md#rag-与数据模型 | 锚点存在 → ✅ |
| claude-review | claude-review-brief.md | 路径存在 → ✅ |
| claude-critic-round1 | design.md#round-1 | 锚点存在, 且 reviewer/spec 均核实 Round1 VERDICT NEEDS_REVISION 记录属实 → ✅ |
| rescope-round2 | design.md#round-2-re-scope | 锚点存在, spec-compliance 核实 Round0→Round1→Round2 修正链完整 → ✅ |
| convention-pack-complete | docs/ai/convention-pack/templates/ (11 个 .tmpl) | spec-compliance 逐文件核对 diff, 与 conventions.md L74 声明一致 → ✅ |
| runtime-verify-trial | runtime-verify.md (mvn compile BUILD SUCCESS 实证) | reviewer 核实为真实编译日志非纸面断言, spec-compliance 核实 conventions.md 回写缺口属实 → ✅ |

implementation (excluded) / quantum-mcp-s3 (pending) 均按状态说明处理, 非"声称完成", 不计入检查。

**done_without_evidence = 0**。checklist 12 项 done 全部有可核实产物或锚点, 未触发 v9.9.0 evidence 上限规则。

### VERDICT: CONCERNS

### 理由 (3-5 行)

- P0 = 0, MISSING = 0, DEVIATED = 0 → 不触发 REWORK/FAIL。
- P1 = 3 (reviewer#1 写侧权限校验降级为注释 TODO 且无 grep 门禁兜底; reviewer#2 读侧 selectById 复现 2026-07-05 IDOR-read 修复前模式; reviewer#3 menu-permission.sql.tmpl ID 生成靠人工目测拼接非可执行表达式) — 达到"≥3 P1"CONCERNS 门槛。
- 三项 P1 性质一致: Convention Pack 模板把安全/正确性关键校验以注释/人工清单形式留白, 且 validate.md 现状缺硬性机制防止跳过, 属于本 sprint 交付物 (设计+模板, 非生产代码) 的可控风险, 不必阻断进入 polish。
- Evidence Cross-Check 全绿, 排除"静默假过"风险, 支持 CONCERNS 而非更严判定。

### next_action: polish


## Delta Review — design 变更增量复核 (evaluator, 2026-07-06T12:53:47Z)

### 变更清单 (pass1 之后, c0683b1..2a339f0)

| 文件 | 变更性质 |
|---|---|
| design.md frontmatter | stage: design→ship, status: draft-for-claude-review→shipped, +superseded_by |
| design.md 正文头部 | 新增「⚠ 本文档状态」提示块 (Round 3 F1), 指向权威文档 docs/ai-sprint-design.md |
| design.md Round 2 段 | "blocked on 授权决策" 改为 "OAuth 2.1 已定案 + 待实现清单" (Round 3 F3) |
| design.md 归属表 | 补 report.export.request 排除出 S3 首批一行 (Round 3 F6) |
| design.md 追加两段 | `## Round 3 · Critic Findings` (独立 critic, VERDICT=NEEDS_REVISION, F1-F6) + `## Round 3 处置` (逐条闭环记录) |
| docs/ai-sprint-design.md §9 | 新增「S3 前置设计项」3 条 (token 存储对齐/consent 页/跨项目冻结接口, Round 3 F4/F5 立项非实现) |
| docs/ai/convention-pack/templates/ServiceImpl.java.tmpl | pass1 P1#1/#2 落地修复: assertWritable/assertReadable/assertInDataScope 真实调用取消注释, 非 TODO |
| docs/ai/convention-pack/templates/menu-permission.sql.tmpl | pass1 P1#3 落地修复: `{{menuId}}1..4` 拼接改显式独立占位符 (btnQueryId 等) |
| docs/ai/convention-pack/validate.md | 新增 §2 安全门禁 (grep G1-G4 硬性检查 + data-scope-exempt 豁免机制), 呼应 pass1 P1#1 建议(a) |

checklist.yaml 未被本次 commit 触碰, S1(Convention Pack)/S2/S3 交付物边界不变; quantum-mcp-s3 仍标 pending。

### 判定: pass1 VERDICT 维持 CONCERNS→已闭环

### 理由

- 变更性质全部落在**文档一致性修正**(frontmatter/正文过时措辞/交叉引用指引, Round 3 F1-F3/F6)与**pass1 P1 findings 的直接修复**(P1#1/#2/#3 对应模板/validate.md 改动逐条可核对), 未新增/删减 checklist.yaml 中任何 done 项, S2/S3 范围标注未变(quantum-mcp-s3 仍 pending), 不构成"已交付物范围/架构决策变更"。
- Round 3 是 delivery-gate U2 要求的第二轮独立 critic, 针对 re-scope 后最终形态, VERDICT=NEEDS_REVISION 判据是 F1(正文未随 re-scope 改写)+F2(frontmatter 冻结在 draft) 两个文档层 P0, 而非对 Round 1/2 已定的"chat 移出仓库"架构裁决翻案; 处置表(design.md `## Round 3 处置`)显示 F1/F2/F3/F6 已当场修复, F4/F5(OAuth token 存储/consent 页/跨项目 schema)已转化为治理文档 §9 的"S3 前置设计项"立项, 非本 sprint 范围(S3 另开 sprint 的 plan 阶段工作), 与 pass1 原判"S2/S3/S4/S5 均标注待办"一致, 无 re-scope。
- pass1 三条 P1 (ServiceImpl 写/读侧权限校验降级为注释 TODO、menu-permission.sql ID 拼接) 现已被 templates/ServiceImpl.java.tmpl 与 menu-permission.sql.tmpl 的实际 diff 证实修复(assertWritable/assertReadable/assertInDataScope 从注释变真实调用, 5 个 ID 改独立占位符), 且 validate.md 新增 grep G1-G4 硬性门禁, 直接回应 pass1 P1#1 建议(a)"加硬性 grep 门禁"——原判"CONCERNS, 进 polish 处理"路径成立, 现已在 polish 落地闭环, 不构成需要 REWORK 的残留风险。
- 无 evidence 缺口: design.md/docs/ai-sprint-design.md 改动与 Round 3 critic 记录、处置表逐条对应, 无"声称修复但无 diff 证据"情形; checklist.yaml 未变, done_without_evidence 仍为 0。
- 结论: 不满足"改变已交付物范围/架构决策"的 RERUN_REQUIRED 门槛, pass1 VERDICT=CONCERNS 对最新状态依然成立, 且原挂起的 next_action=polish 已完成对应修复, 可视为该 CONCERNS 已闭环。
