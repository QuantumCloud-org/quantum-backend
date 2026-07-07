# Review Pass 1 — Sprint 2026-07-07-fe-be-convention-pack-expansion

范围: quantum-backend commit 4b0c067 (db-conventions/test-conventions/3 模板/validate G5-G6) +
quantum-front/docs/ai/convention-pack/ (9 文件, 未提交)。

## Findings (按严重度排序)

### F1 [SEVERITY=P0] G6 门禁对自己引用的真实测试类全部 FAIL，门禁与现实脱节
- File: quantum-backend/docs/ai/convention-pack/validate.md:104-116 (G6a/b/c/e)
- 问题: test-conventions.md 声称五类必测用例"从真实测试提炼"，并把 `SysUserServiceImplSecurityTest` /
  `PageQueryValidationContractTest` / `SysUserControllerContractTest` 列为范例。实测对**这些原始范例文件**
  逐条跑 G6 grep 命令：
  - G6a `void .*[Rr]eject.*OutOfScope.*\(` → PASS (`selectUserByIdShouldRejectOutOfScopeUser`)
  - G6b `void .*(update|delete|remove).*Reject.*OutOfScope.*\(` → **FAIL**（真实写侧越权测试名是
    `importUsersShouldRejectOutOfScopeUpdate`，动词在中间不在开头，正则锚点位置不匹配）
  - G6c `grep RequiresPermission` 对 `SysUserControllerContractTest.java` → **FAIL**（该文件里根本不含
    `RequiresPermission` 字符串，全仓 `src/test/java` 搜索也是 0 命中——test-conventions.md §3 描述的
    "反射断言注解值"根本没有真的产生 grep 能命中的字面量）
  - G6e `void .*[Cc]onflict.*[Vv]ersion.*\(|void .*[Vv]ersion.*[Mm]ismatch.*\(` → **FAIL**（全仓搜索
    `Conflict.*Version|Version.*Mismatch` 零命中，乐观锁冲突用例在现有测试套件里根本不存在，
    是本次约定新提出的要求，而不是"从真实提炼"）
  即约定包自称的范例基准，套用它自己写的门禁命令会判"生成未完成"，门禁公式与举例证据自相矛盾。
- 建议: 三选一并落实：(a) 调整 G6b/c/e 正则以匹配这套代码库真实的命名习惯（"动作在中间"如
  `importUsersShouldRejectOutOfScopeUpdate`，且明确 G6c 改为反射断言检测而非字符串 grep）；
  (b) 明确声明"这四类用例中乐观锁/权限反射断言是新增要求，现有范例类不满足，需要生成器对新模块
  从零产出"，去掉"从真实提炼"这个误导性表述；(c) 提供一个真实跑通 G6 全绿的样例文件路径作为回归基准。
  当前二选一都没做，等于门禁没有被实证过。
- 引用: rules/coding-standards.md P1 "测试覆盖关键路径"（此处是覆盖率**声明**与实际不符，误导后续生成器）

### F2 [SEVERITY=P0] runtime-verify.md 未提供 G5/G6 grep 命令实跑证据，仅有 Read 复核
- File: .ai_state/sprints/2026-07-07-fe-be-convention-pack-expansion/runtime-verify.md:24-38
- 问题: "场景 2: BE 约定包主 agent 全量复核" 描述的验证方式是"逐文件 Read 复核 6 文件"，evidence.yaml
  中对应的 tool_use 也全部是 Write/Edit（落盘动作），没有一条 Bash tool_use 对应"执行 G5/G6 grep 命令
  验证其在真实文件上的行为"。铁律 6（完成度证据）要求"报完成附 tool_use ID"，但这里的"完成"只覆盖了
  "文件写好了"，没有覆盖"门禁命令跑得通"这个更关键的验收点——而 F1 证明这个命令实际跑起来是会 FAIL 的。
  Read 复核能发现文字层面的不一致（如"4条"→"5条"），但发现不了 grep 正则与真实代码结构不匹配这类
  运行时问题，这正是本次唯一漏检的一类缺陷。
- 建议: 对新增的 G5/G6（以及沿用的 G1-G4）在至少一个真实模块（如 sys_config 或 sys_dept）上补跑一次
  端到端 grep，把 PASS/FAIL 结果写进 runtime-verify.md，FAIL 项要么改正则要么在文档里承认限制。
- 引用: 铁律[运行时验证]（Refactor/System 强制 runtime-verify 先于 review；本 sprint 虽判定为 Feature，
  但"验证方式"章节本身承诺了实证，实证未覆盖门禁自检这一核心风险点）

### F3 [SEVERITY=P1] G6c 权限码校验只有字符串 grep，与 test-conventions.md 描述的"反射扫描"方式不一致
- File: quantum-backend/docs/ai/convention-pack/validate.md:109-110；test-conventions.md:66-69
- 问题: test-conventions.md 明确说"可用反射读注解值断言，参照 PageQueryValidationContractTest 的反射扫描
  模式"，但 G6 门禁本身用的是最朴素的字面量 `grep -qE "RequiresPermission" "$CONTRACT"`。如果生成器真的
  按"反射扫描"实现（即测试代码里出现的是 `Method.getAnnotation(RequiresPermission.class)` 而不是硬编码
  权限点字符串），grep 命中的只是注解类名，不校验"每个写接口方法都标注了"这个语义，等于门禁比约定描述
  弱一档、可被"随便 import 一次注解但不逐个断言"的敷衍实现蒙混过关。
- 建议: G6c 至少要求命中 `assertThat(...).isNotNull()` 或 `.hasAnnotation(RequiresPermission.class)` 之类
  与断言逻辑绑定的模式，而不只是类名字符串出现过。
- 引用: rules/coding-standards.md P1 "测试覆盖关键路径"

### F4 [SEVERITY=P1] db-conventions.md dept_id 第 4/5 条规则边界未在 G5 门禁体现
- File: quantum-backend/docs/ai/convention-pack/db-conventions.md:79-83；validate.md:65-90 (G5)
- 问题: 判定规则第 4 条"含糊默认加 dept_id"、第 5 条"组织树自引用表不适用本规则"都是业务判断，
  但 G5 门禁只做机械的"DDL 列名 ⊆ 设计文档字段表"字符串比对（G5d），完全没有校验"设计文档是否真的写了
  数据域归属判定结论"这一步——即便设计文档的"数据域归属"那一行留空或写占位符 `<!-- TODO -->`
  未替换，G5a-c 仍可能全 PASS（TODO 注释不含 `{{` 因此绕过 G5c 占位符检测）。
- 建议: G5 增加一条检测"表基本信息"表格的"数据域归属（dept_id 判定）"单元格不是 `<!-- TODO`
  开头（即已经填写），否则算 G5 FAIL，防止判定结论被留空直接通过双文档门禁。
- 引用: rules/coding-standards.md P0 "Sisyphus 完整性"（子项留空但门禁判过，属于"差不多了"模式）

### F5 [SEVERITY=P1] test-conventions.md 与 validate.md 对"5 类 vs 4 类"用例计数存在残留表述歧义
- File: quantum-backend/docs/ai/convention-pack/test-conventions.md:42,89-90
- 问题: 第 42 行标题"每个生成模块必须覆盖的四类测试"，第 89-90 行又解释"本约定按测试点拆为上述 5 个
  具体用例（读/写越权各一个用例，共 4 类关注点）"。commit message 提到主 agent 已修正"schema-design 引用
  4 条→5 条判定规则"，但 test-conventions.md 内部这处"四类/5个用例"的措辞仍然共存，读者（未来的
  unit-test-gen 生成器）第一次读到标题会以为只需 4 个测试方法，需要读到第 90 行小字说明才能纠正，
  容易被生成器只做表面理解漏掉一类。
- 建议: 统一标题为"必须覆盖的 5 类测试用例"，或在标题旁加脚注锚点直接指向说明段，减少生成器读漏的风险。
- 引用: rules/doc-style.md "复杂业务逻辑非显然" 应有清楚解释，此处解释存在但位置弱化，建议前移。

### F6 [SEVERITY=P2] G4b（BE menu-permission.sql 去重检测）正则假设"每行都以 `(数字` 开头"过于脆弱
- File: quantum-backend/docs/ai/convention-pack/validate.md:43（此为既有 G1-G4，非本次新增，仅顺带核对）
- 问题: `grep -oE '^\s*\(([0-9]+)'` 假设 INSERT 语句每行都以 `(<id>` 开头且 id 是首个字段，若未来
  `menu-permission.sql` 换成多行 VALUES 或字段顺序变化（id 不是第一个），检测会静默失效但不报错。
  非本次改动引入，仅记录供后续 polish 参考。
- 建议: 后续 polish 阶段补一个更稳健的实现（如显式 `awk -F',' '{print $1}'` 定位 id 列），本次 sprint
  不阻塞。
- 引用: rules/coding-standards.md P2 (建议级)

### F7 [SEVERITY=P2] FE conventions.md「数据域归属」判定描述依赖 BE 术语但未显式互链版本
- File: quantum-front/docs/ai/convention-pack/conventions.md:73-74
- 问题: FE 提到 "`page-registry.tsx` 里的 `backendComponent` 字符串必须与后端 `sys_menu.component` 一致"，
  这与 BE validate.md 的人工清单第 4 条"menu-permission.sql 权限点与 controller RequiresPermission 一致"
  是同一枚硬约束的两面，但两份文档互相都没有写"另一侧文件路径"的显式引用（如 FE 侧写"参见
  quantum-backend/docs/ai/convention-pack/conventions.md 的菜单落库一节"）。目前只能靠人工经验对齐，
  一旦两包分别迭代容易漂移且难以察觉。
- 建议: 跨包引用建议至少在两份 conventions.md 顶部互相加一行"关联约定: <另一仓库路径>"，不强制但降低
  漂移风险。属于建议级，不阻塞本次 ship。
- 引用: rules/doc-style.md README 规范精神（跨文档引用应显式）

## 未发现问题的检查点（供 evaluator 参考）

- BE PG 方言实探（int8/int4/timestamp(6)/bpchar/双引号标识符/`OWNER TO "admin"`）与 `deploy/init.sql`
  逐条核对一致，无虚构。
- 审计五件套/乐观锁/软删列声明与 `sys_config`/`sys_dept`/`sys_user`/`sys_login_log` 实测列一致；
  `sys_login_log` 无审计列的"存量特例"表述准确。
- dept_id 判定规则第 3 条（关联表 `sys_role_dept`/`sys_role_menu`/`sys_user_role` 无审计/version/dept_id）
  经实测确认成立；第 5 条（`sys_dept.parent_id` 自引用）与真实建表语句一致。
- FE conventions.md 与真实 `src/features/system/user/` 目录结构（文件命名/组件拆分/`toId`/
  `page-registry.tsx` 存在性/`.env.dev`+`.env.produce` 无 `VITE_FEATURE_MOCK=true`）逐项核对一致。
- FE G3 占位符检测正则 `\{\{[A-Za-z_]+\}\}` 对 `users-provider.tsx` 的 JSX `value={{...}}` 无误报，
  证实此前假阳性已被修正。
- FE G1-G6 grep/test 命令语法层面可执行（`bash -n` 通过），G5 的 `||`/`&&` 混合优先级实测两种边界
  case（mock.ts 缺失 / mock.ts 存在但未 import model）行为均符合预期，非 bug。
- 双仓库门禁风格（编译/lint 校验 → grep 安全门禁 → 人工清单 → 报告）结构对称，豁免机制
  （`data-scope-exempt` / `access-guard-exempt`）命名和"无豁免行即 FAIL"的强制措辞一致。

## Spec Compliance

（取自 pass1-spec.md, spec-compliance subagent, 2026-07-07T02:57:01Z）

范围: route-note.md 验收标准 3 条 vs quantum-backend commit 4b0c067 + 工作区未提交修复
(docs/ai/convention-pack/{validate,db-conventions,test-conventions}.md) + quantum-front commit 8f4d5ab。

### AC1 — FE 约定包: 满足

conventions.md 基于 `src/features/system/user/` 真实结构实探; 权限守卫默认生成; mock 约定
(`VITE_FEATURE_MOCK` + G4 门禁) 齐备。

### AC2 — BE 增补: 满足

db-conventions.md 双文档分离硬约定; schema-design.md.tmpl + ddl.sql.tmpl 两个分离模板; PG 方言实探
与 deploy/init.sql 逐条一致; test-conventions.md 四层测试形态 + debug loop + test-report 模板齐备。

### AC3 — 两仓库 commit + 主 agent 独立复核 validate 实跑: 满足（REWORK 闭环后）

- commit 部分满足 (FE 8f4d5ab / BE 4b0c067); push 未执行, status=pending 待 ship, 流程内正常, 不计 MISSING。
- 独立复核 validate 实跑: commit 4b0c067 时点仅"逐文件 Read 复核"(未覆盖门禁实跑), 与 AC3 有偏差 —
  已被 review pass1 F1/F2 [P0] 抓出。工作区未提交修复 (runtime-verify.md 场景4) 已回填双向实跑证据
  (G5a-G5e 正例全 PASS + 反例 fail-closed + G6 正例全 PASS), 构成同 sprint 内自我纠正闭环。

### DEVIATED（按已提交快照单独评估时）

D1: commit 4b0c067 时点 AC3 与"实跑"要求有偏差, 已被 F1/F2 抓出且在工作区修复中闭环, **不计入
未闭环 DEVIATED**。

### Spec Compliance 总评

MISSING=0 / EXTRA=1(合理, 跨仓库互链+标题澄清, 无 scope creep) / DEVIATED=0(1处已闭环)。
AC1/AC2/AC3 均满足。**建议 PASS**。

## Evidence Cross-Check

checklist.yaml 5 项 vs evidence.yaml + runtime-verify.md + git diff (工作区未提交修复):

| task | status | evidence | 判定 |
|---|---|---|---|
| fe-pack | done | runtime-verify.md 场景1 lint+tsc 实跑零输出 (独立 worktree a1d881a8d6a040fb7 产出) | PASS |
| be-db-pack | done | evidence.yaml toolu_01SqugYdLNgt26vbWB3Hv57u/017Mf4kZMfY9negCikZxyd1Q (Write db-conventions/ddl.tmpl) + 工作区 diff 补跨仓库互链 | PASS |
| be-test-pack | done | evidence.yaml toolu_01PYH4gJuDNyhV5H892PRHbn (Write test-conventions.md) + toolu_01HweWBLdHBD4qXLzQH3otty (Write validate.md) + 工作区 diff G5e/G6 改动 | PASS |
| main-review | done | runtime-verify.md 场景2(初版 Read 复核) + 场景4(REWORK 后补, toolu_01QfnioTKT6grZwqynNmJds4 Edit) 双向实跑记录, git diff 三文件改动确认落地 | PASS |
| commits | pending | 未标 done, 无需证据 (push 留给 ship stage) | N/A |

- done_without_evidence: **0**。P0 修复 (F1/F2) 引用的 validate.md G5d/G5e/G6a-e 改动、test-conventions.md
  标题+适用范围段、db-conventions.md 互链、runtime-verify.md 场景4, 均以 `git diff` 逐一核实存在
  且内容与 pass1-reviewer.md 建议一一对应, 非声明未落实的假过。

## VERDICT (evaluator, 2026-07-07-fe-be-convention-pack-expansion)

**判定**: PASS

### 评分依据 (4 维)

| 维度 | 得分 | 说明 |
|---|---|---|
| Functionality | 4.6 | AC1-AC3 全满足(spec-compliance), F1/F2 两个 P0 经工作区修复并附正反双向实跑证据闭环 |
| Spec Compliance | 4.7 | MISSING=0 DEVIATED=0(1处闭环) EXTRA=1(合理), 无 scope creep |
| Craft | 4.3 | G6 正则改为标准名精确匹配+适用范围声明, 消除"举例证据自相矛盾"; F3/F5 表述澄清已落实 |
| Robustness | 4.4 | 门禁新增 fail-closed 验证(反例删互引/留TODO/空测试类均正确 FAIL); F4(G5e)/F6(存量脆弱正则)分级处理得当 |

总评: 4.5 / 5.0

### 触发判定的关键 findings

- F1 (P0, G6 正则失配) → 已修: G6a-e 改为按 test-conventions 标准方法名精确检测, 明确"只跑生成物不约束存量"适用范围, 消除与真实测试类的矛盾。`git diff docs/ai/convention-pack/validate.md` 核实。
- F2 (P0, 未实跑门禁) → 已修: runtime-verify.md 场景4 补 G5/G6 正反双向实跑, 过程中新抓到 G5d 真 bug (schema 名 "public" 误判为列) 并当场修复复验 PASS, 证明补跑非走过场。
- F3 (P1, G6c 字面量 grep 弱于"反射扫描"描述) → 已收窄: G6c 增加标准方法名 `writeEndpointsShouldRequirePermissionCode` 检测项, 与字符串 grep 并列作为过渡态。
- F4 (P1, G5 未校验设计文档判定结论留空) → 已修: 新增 G5e 检测"数据域归属"行禁止残留 TODO。
- F5 (P1, "4类/5用例"表述歧义) → 已修: 标题改"五个必测用例（四类关注点）", 说明段位置强化。
- F7 (P2, 跨仓库互链缺失) → 已修: db-conventions.md 顶部加 FE 侧对应约定包引用。
- F6 (P2, G4b 正则脆弱) → 确认为存量既有问题非本次引入, 缓办不阻塞, 处置合理。

### 行动建议

- 立即修: 无 (P0 均已闭环并有 git diff + 实跑证据)
- 后续 sprint 处理: F6 (G4b 正则健壮性), FE G1-G6 在"新生成页面"上端到端实跑 (runtime-verify.md 遗留项, 留给 scaffold-page-gen sprint)
- 推迟: 无

### Sisyphus 完整性检查

- [x] 所有 Task 完成 (checklist.yaml fe-pack/be-db-pack/be-test-pack/main-review = done; commits = pending 属 ship 阶段正常状态, 非遗漏)
- [x] 所有 Task 验收过测试 (FE lint+tsc 实跑; BE G5/G6 正反双向 fixture 实跑, 见 runtime-verify.md 场景1/4)
- [x] (Feature 路径, 非 Refactor/System) 无强制 polish 要求, 直接进 ship

**下一步**: ship（两仓库 commit + push；BE 工作区未提交改动一并纳入本次 commit）。
