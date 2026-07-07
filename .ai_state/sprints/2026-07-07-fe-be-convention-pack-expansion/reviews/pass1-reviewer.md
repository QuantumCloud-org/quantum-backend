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
