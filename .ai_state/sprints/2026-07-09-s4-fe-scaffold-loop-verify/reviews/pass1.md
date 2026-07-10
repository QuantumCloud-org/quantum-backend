# S4 Review Pass 1 — 2026-07-10

> Sprint: 2026-07-09-s4-fe-scaffold-loop-verify · impl: Opus 4.8 · review: fable5 三件套
> reviewer (aaeb96c766be374bd) + spec-compliance (af4891bfe679e1891) 并行独立产出,
> 主 agent 仅机械拼接本文件; evaluator VERDICT 见文末。

## Reviewer (代码层 findings)

### P0

**F1 — vite.config.ts 生产构建未隔离 `VITE_FEATURE_MOCK` 注入 (Security)**
- File: `vite.config.ts:55-58`
- 问题: `process.env.VITE_FEATURE_MOCK` 注入 `define` **不区分 `mode`**。`defineConfig(({ mode }) => ...)` 已拿到 mode 却未用于门控。若构建机 (CI/本地) shell 残留 `VITE_FEATURE_MOCK=true` (常见场景: 开发者跑完 `VITE_FEATURE_MOCK=true bun run dev` demo 后同一 shell session 里执行 `bun run build`, 或 CI runner 复用容器/env), `vite build --mode production` 会把该值原样烤进生产 bundle 的 `import.meta.env.VITE_FEATURE_MOCK`。`isMockEnabled()` (`src/lib/mock/index.ts:10`) 据此短路 `fetchCurrentUser`/`fetchMenuRouters` (`src/app/auth/api.ts:33` / `src/app/navigation/api.ts:12`), 生产环境下**任意未登录访客**会被渲染成已认证用户 "demo.asset" (固定 roles/permissions), 跳过真实登录/菜单请求 — 这是客户端认证短路进生产的真实攻击面。
- 风险等级评估: 后端数据接口仍独立鉴权 (design 已确认), 故不会造成数据泄露, 但会话/导航层的"伪装已登录"本身已违反 P0 认证边界; 且触发条件 (env 残留) 在真实 CI/本地开发流程中并不罕见, 修复成本极低, 应按 P0 处理而非接受风险。
- G4 门禁盲区: design.md 风险表 "mock 开关误提交 → G4 门禁 (.env.dev/.env.produce 无 VITE_FEATURE_MOCK=true)" 只覆盖**已提交文件**向量, `check_frontend_pack.py` (Rlues 侧) 也只校验 validate.md 文本含 G1-G6 marker, 不做实际 grep/自动化检查。**shell 传参进生产构建**这条向量完全未被任何门禁覆盖, 是设计层面的缓解缺口, 非仅实现疏漏。
- 依据: `rules/security-checklist.md` 权限检查 P0 (客户端不应能在无凭证下伪造已认证态); `rules/coding-standards.md` P0 安全条款。
- 建议: `vite.config.ts` 加 `if (mockFlag !== undefined && mode !== 'production') { define[...] = ... }`; 同时把 G4 门禁从"文本 marker 存在"升级为构建脚本层面的真实校验 (例如 `vite build` 后 grep 产物 chunk 确认无 `VITE_FEATURE_MOCK` 字符串, 或在 CI 显式 `unset VITE_FEATURE_MOCK` 后再跑 production build)。

### P1

**F2 — mock 菜单 fixture 指向已回滚页面, 导致"永久基建"当前不可用 (Correctness / Design 一致性)**
- File: `src/lib/mock/session-fixtures.ts:50` (`component: 'system/asset/index'`) vs `src/app/navigation/page-registry.tsx` (回滚后无 `system/asset/index` 条目)
- 实测追踪: `src/routes/_authenticated/$section/$page.tsx` 的 `beforeLoad` **先** `getBackendPageByPath('/system/asset')` (page-registry 查表) **再**发起 `navigationQueryOptions()` 网络请求。回滚后该查表直接 `undefined` → 立即 `redirect({to:'/404'})`, 根本不会走到 mock 的菜单/权限判定分支。侧栏渲染 (`registry.ts` 的 `getBackendPageByComponent`) 同样查不到, 会静默丢弃该菜单项。不是"崩溃", 是**当前状态下打开即 404**。
- 与设计矛盾: design 影响范围表把 `src/lib/mock/` 定性为"永久 (mock 基建)", 目的是复用于后续生成循环; runtime-verify.md Reflect 也自陈"非死引用", 但实测结论应更正为"当前唯一 demo 入口已失效, 需要换实体时才能验证基建仍工作"。对下一个用这套 mock 基建的人 (设计声明的复用场景) 而言, 打开就是死路, 体验与文档承诺不符。
- 依据: `rules/coding-standards.md` P1 (设计一致性隐含要求 — 交付物应达成其声明目的); `rules/doc-style.md` TODO 需带说明 (现有注释是散文提示, 无可执行的 issue 追踪)。
- 建议: 二选一 — (a) fixture 指向一个**仍在 page-registry 里**的真实页面 (如 `system/user/index`) 作为长期烟测锚点; 或 (b) 显式标注 `// TODO(下次生成实体时替换)` 并在 conventions.md 补一句"每次演示后必须同步更新 fixture 的 component/permission, 否则 mock 基建自检失效"。

**F3 — 新增 mock 基建零单测 (Test)**
- File: `src/lib/mock/index.ts`, `src/lib/mock/session-fixtures.ts` (无对应 `*.test.ts`)
- 问题: `isMockEnabled()` 是永久保留、影响认证/导航短路的安全关键逻辑 (对照 F1), 却无任何测试断言其默认安全 (`VITE_FEATURE_MOCK` 未定义或非 `'true'` 时必须返回 `false`)。`bun test` 15 pass 是既有 baseline 回归, 未新增覆盖本次改动。
- 依据: `rules/coding-standards.md` P1 "测试覆盖关键路径 (每个 Feature ≥ 1 个测试, 边界条件覆盖)"。
- 建议: 至少加 1 个测试文件覆盖 `isMockEnabled()` 的 true/false/undefined 三态, 尤其是"默认关闭"这条安全默认值。

### P2

无额外发现 (JSDoc 齐全, 命名清晰, conventions.md 增补条款与代码互相印证, 符合 `rules/doc-style.md`)。

### 回滚完整性核实 (附带, 非 finding)
`git status --porcelain` = 5 modified (`conventions.md` / `auth/api.ts` / `navigation/api.ts` / `vite-env.d.ts` / `vite.config.ts`) + 2 untracked (`src/lib/mock/` / `.ai_state/`), 与 design.md 影响范围表"永久"行精确一致, 无多无少; `bun test` 15 pass 复核通过; `system/asset` 生成物在 `page-registry.tsx` / `src/features/` 均已确认清除。回滚本身 OK。

### 维度小结
1. Correctness: CONCERNS
2. Security: FAIL
3. Test: CONCERNS
4. Design 一致性: CONCERNS
5. Code quality: OK

## Spec Compliance (spec-compliance, 2026-07-10)

### MISSING (做少了)

无。8 条验收标准逐条核对如下 (依据链见备注):

| AC | 内容 | 状态 | 验证方式 |
|---|---|---|---|
| 1 | check_frontend_pack.py PASS | ✅ | 独立重跑 `python3 .../check_frontend_pack.py quantum-front/docs/ai/convention-pack` → `{"status":"ok","errors":[],"warnings":[]}`, 与 runtime-verify.md 一致, **可复核** |
| 2 | 导航 mock 基建 + component 精确匹配 | ✅(部分自报) | 依赖链清单在 runtime-verify.md L7-17 落地; `src/app/auth/api.ts`/`src/app/navigation/api.ts` 短路 diff 已核实; component 匹配 page-registry 一项发生在生成态 (已回滚), **不可从当前 diff 复核**, 仅存于 runtime-verify.md 自报 |
| 3 | tsc/lint/build 全绿 (生成模块) | ⚠️自报only | 生成态编译结果无法复核 (asset 模块已删); 但**回滚后基线** tsc/lint 独立重跑均 0 error, 与 checklist.yaml 声称一致的编译链路径可信 |
| 4 | G1-G6 + dept treeselect 权限分支 | ⚠️自报only | `asset-management-access.ts` 已随回滚删除, 无法复核; 唯一存留佐证是 `session-fixtures.ts` L4-14 注释与 MOCK_DEMO_USER.permissions 含 `system:dept:treeselect`, 与 runtime-verify.md 引用的 grep 输出逻辑自洽 |
| 5 | mock demo 探活 + 经导航渲染 asset 列表 | ⚠️自报only | 浏览器截图/DOM 断言为 ephemeral 产物, 不落 diff, **仅存于 runtime-verify.md L31 + transcript** |
| 6 | Rlues skill 核心 diff=0 | ✅ | 独立执行 `git -C Rlues status --porcelain .../scaffold-page-gen/` → 空输出, **可复核确认** |
| 7 | 回滚后 git status 仅余永久项, bun test 回归 | ✅ | 独立执行 `bun test` → 15 pass/0 fail, 与声称一致; `grep -rln "canCreateAsset\|assetName\|assetCode" src/` 无匹配, 确认业务代码零残留; page-registry.tsx grep "asset" 无匹配 |
| 8 | FE `.ai_state/_index.md` 存在且字段属实 | ✅ | Read 确认存在, `tools_available.bun: "1.3.14"` 与实测 `bun test v1.3.14` 一致, 非编造字段 |

**方法论备注**: AC3/4/5 依赖"生成→验证→回滚"设计本身 (design.md 步骤 8 + 风险表"generator 自报不实"缓解项), 演示产物验后必然消失, spec-compliance 站在 review 时点**结构性无法**从 diff 独立复核这三条, 只能验证"主 agent 独立复核"这一监督环节本身是否被设计要求且被 checklist.yaml 记录 (是, static-gates/demo-verify 两项 evidence 字段均写明"主 checkout 独立复核"而非采信 subagent). 这是该 sprint 自选验证形态 (S2 先例) 的固有局限, 不计入 MISSING, 但**建议 reviewer/evaluator 知悉**: 这三条的可信度上限 = 对主 agent transcript 的信任度, 非本 subagent 可独立证伪或证实。

### EXTRA (做多了, 分合理 refactor / scope creep)

无 scope creep。git diff 5 个已跟踪文件改动 + 2 个新增目录, 逐一对照"影响范围"表:

- `docs/ai/convention-pack/conventions.md` [合理, 已声明]: 对应表格行 3 "导航 mock 约定 + 验证实体选择原则", diff 34 行新增与设计描述逐字对应 (含"演示实体不得选无关联维度简单实体"教训条款)
- `src/app/auth/api.ts` / `src/app/navigation/api.ts` [合理, 已声明]: 对应表格行 2 "USE_MOCK 短路", diff 6+6 行, 均为 `if (isMockEnabled()) return <fixture>` 单行短路, 未改动真实分支
- `src/vite-env.d.ts` / `vite.config.ts` [合理, 未在表格显式列名但属同一行"依赖链出口"实现细节]: runtime-verify.md L39-41 自测自改记录已说明 vite 环境变量注入是 mock 短路生效的**必要前置修复**, 非独立新增功能, 判定为该表格行的实现延伸而非表外改动
- `src/lib/mock/` (新增, 未跟踪) [合理, 已声明]: 对应表格行 2 括注"及依赖链出口", 是短路机制的载体模块, design.md 步骤 3 已预告
- `.ai_state/` (新增, 未跟踪) [合理, 已声明]: 对应表格行 4 "athena-init 产物"

无未声明业务模块、无跨范围改动。

### DEVIATED (做偏了)

无实质偏离。检查细节:

- design.md 步骤 3 "mock 菜单节点的 component 必须精确匹配 page-registry backendComponent" — `session-fixtures.ts` L48-50 `component: 'system/asset/index'` 命名格式与仓库 `page-registry.tsx` 现有条目命名惯例 (`<module>/<entity>/index`) 一致, 未见偏离迹象, 但该字段目前指向的 asset 页面已回滚删除, 成为**孤儿引用** (纯字符串常量, 不参与运行时解析, `tsc`/`lint` 已独立确认 0 error, 不构成技术缺陷) — runtime-verify.md Reflect 段 L58 已自行记录此点并说明"非死引用", 判定为**已知晓且可接受**的技术债, 非偏离
- impl 关注清单 P2#1 (mock 用户权限点须入依赖链清单): runtime-verify.md L16-17 单独一段"权限判定 (无网络)"补充覆盖, 兑现
- impl 关注清单 P2#2 (备选 C 阈值定义为具体请求列表): runtime-verify.md L9-14 表格精确列出 2 个 `GET` 请求, 阈值边界钉死, 兑现

### 总评 (PASS)

- MISSING: 0
- EXTRA: 0 (合理 5 处 / scope creep 0)
- DEVIATED: 0 (1 处孤儿引用已自报知晓, 判定为可接受技术债非偏离)
- 独立可复核的验收标准 (AC1/2部分/6/7/8): 5 条**全部复核通过**
- 结构性不可复核 (AC3/4/5 生成态产物, 已随设计回滚消失): 依赖 runtime-verify.md 自报 + "主 agent 独立复核而非采信 subagent" 的监督声明
- **建议: PASS** — MISSING=0, DEVIATED=0, scope creep=0; 唯一保留项是 AC3/4/5 的证据链性质 (transcript-only), 建议 evaluator 在最终 VERDICT 中显式标注该已知方法论局限, 而非作为 REWORK 理由

## Evaluator VERDICT (2026-07-10)

**VERDICT: REWORK** (evaluator a540d6f70688a4bd3)

- 唯一触发点: reviewer F1 (P0) — `vite.config.ts:55-58` 生产构建未隔离 `VITE_FEATURE_MOCK` 注入, evaluator 独立复核源码属实。判定为 **design 缓解缺口** (design 风险表只承诺 G4 挡 .env 提交向量, shell→production 向量不在承诺内) — 与 spec-compliance "DEVIATED=0" 不矛盾: 后者判"未偏离已定设计", 前者判"设计本身有安全空洞", 标的不同层面。
- 按 coding-standards P0 (安全违反=REWORK) 裁定; 修复未 commit、一行 if 收窄、零回归风险 → REWORK 非 FAIL。
- F2/F3 (P1) 与 spec-compliance PASS 均不单独触发。
- Evidence Cross-Check: 9 done 项中 5 项真实可核 / 1 项部分真实 / 3 项 ephemeral transcript-only; **done_without_evidence = 0**, 不因证据链降级。

### next_action: `rework_impl`

返工清单 (最小修复集):
1. **必修**: `vite.config.ts` mockFlag 注入加 `mode !== 'production'` 门控; 验收 = `VITE_FEATURE_MOCK=true` 下跑 production build, grep 产物无 `VITE_FEATURE_MOCK` 残留。
2. 顺手 (避免二次往返): F2 fixture 改指现存页面或加 TODO 标注; F3 补 `isMockEnabled()` 三态单测 (重点"默认关闭")。
3. 记录不阻塞: G4 门禁升级为构建产物层校验 (design 层缺口, 下个 sprint / proposals)。

修复后重过 review (安全类变更, 不可局部复核跳过) → pass2.md。
