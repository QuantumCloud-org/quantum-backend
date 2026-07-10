# S4 Review Pass 2 — 2026-07-10 (rework 后)

> pass1 VERDICT=REWORK (F1 P0)。impl 方 (Opus) rework 三项。pass2 由 fable5 重过三件套。
> reviewer (a9297bfacf9d88d9d) + spec-compliance (a22e9b6c2281e42fd) 独立复核 (均亲自重跑 production grep, 不采信 transcript); evaluator VERDICT 见文末。

## Reviewer pass2 (代码层 findings)

### 三 finding 复核

**F1 [已解决]** — production build 隔离生效。
- 亲自跑 `rm -rf dist && VITE_FEATURE_MOCK=true bun run build` (built 580ms), 逐 pattern grep:
  `demo.asset` / `演示资产专员` / `asset-operator` / `system:asset:list` / `资产管理` → 全部 none。
- 补充: `grep MOCK_DEMO_USER|MOCK_MENU_ROUTERS|warnOnUnpairedMockRoutes|parseMockFlag dist/` → none。fixture/helper 名字本身也未泄漏, 是真 DCE 剔除而非字符串巧合无命中。
- 反面验证 (dev 未误伤): vite.config `mode==='production' ? 'false' : shell flag`; package.json build=`tsc -b && vite build` (未传 --mode, vite build 默认 production, 与判定对齐)。dev/preview 非 production mode 下 shell flag 仍透传。逻辑自洽。

**F2 [部分解决]** — 症状 (静默 404) 消, 根因 (fixture 未配对) 未修。
- page-registry 注册清单不含 `system/asset/index`; MOCK_MENU_ROUTERS 仍引用它。demo 打开仍 404/无法渲染, 行为本体未变。
- warnOnUnpairedMockRoutes 用 isSupportedBackendComponent 正确识别 orphan, console.warn 逻辑正确 — 把"静默"变"有诊断"。F2 原文点名的唯一问题 (静默 404) 字面已解决。
- 若 evaluator 期望"demo 能跑"而非"失败时有提示", 应判未完全解决。按 F2 原文措辞 (静默 404) 严格对齐, 判部分解决。

**F3 [已解决]** — 单测新增且覆盖安全默认值。
- `bun test src/lib/mock/index.test.ts` → 3 pass / 7 expect。`parseMockFlag(undefined) → false` 有专门 case (命名点明 "safe default, the prod/no-flag path")。
- 但 parseMockFlag **零生产调用者** (grep 只命中 index.ts 定义 + test), prod 路径跑的是 api.ts / navigation/api.ts 两处裸内联 `=== 'true'`。测试保护的是"契约文档"而非"落地代码"。见新发现。

### 新发现

- **[P1, Test risk]** `warnOnUnpairedMockRoutes()` (session-fixtures.ts:100-118) 零单测。本轮 rework 新增、承载 F2 修复的核心逻辑, 却违反这轮自身宣传的"补测试覆盖"主题。建议: mock isSupportedBackendComponent 返回 false, 断言 warn 触发一次 + 重复调用去重。
- **[P1, Design 一致性 / 文档即真相]** conventions.md 新增 "会话 / 导航层 mock" 章节 (本次 diff 内新增) 仍写 `src/lib/mock/index.ts`(`isMockEnabled()` ...)、`if (isMockEnabled()) return <fixture>`。但 `isMockEnabled` 已被完全删除, 调用点改裸内联。文档描述的机制与代码不符, 是同一 commit 内自相矛盾的新鲜漂移。File: conventions.md:205/210/227 vs auth/api.ts:35 / navigation/api.ts:17。
- **[P2]** F2 demo 当前仍不可用 (只是 warn), 建议 conventions/PR 明确"当前 demo 不可用, 需重新生成 asset 或改配对", 避免误读为"已修可跑"。
- **[P2, DRY]** mock-flag 判定 3 处副本 (parseMockFlag + 2 裸内联), 注释已说明裸内联为 DCE 必需, 可接受; 但无机制 (lint/test) 保证三处同步。风险敞口小, P2 观察。
- **[INFO]** warnedOrphans module-level 可变 Set, HMR/长驻 dev 跨会话不重置属预期; 未来给 warn 补测试需注意模块级状态跨 test 泄漏。

回归: tsc 0 / lint 0 / bun test 18 pass。

### 维度小结
Correctness: 部分 (F2 症状消根因未消) · Security: 通过 (F1 验证通过) · Test: 不足 (warn 零覆盖) · Design一致: 有漂移 (conventions 与代码矛盾) · Quality: 良好

## Spec Compliance pass2 (spec-compliance)

### 返工清单兑现

1. **必修 F1** — **兑现且更严**: vite.config production 强制 pin 字面量 'false' + 短路裸内联 (为 DCE)。独立复核 prod build + 6 项 grep 全 0 命中 (修复前同批全命中)。比清单"仅门控注入"更进一步, 属同向加固。
2. **顺手 F2** — **部分兑现 (第三方案)**: 清单给"改指现存页面 或 加 TODO", 实际两者字面都没做, 改为 warnOnUnpairedMockRoutes + PAIRING CONTRACT 注释 + conventions 条款。缺口: session-fixtures.ts:63 仍指向已回滚页面, 演示入口依旧 404 (从静默变带指引)。design 声明的"永久基建"唯一验证锚点仍不可用。
3. **顺手 F3** — **兑现 (目标函数因 F1 重构已不存在, 改测等价函数)**: isMockEnabled 因裸内联 DCE 已移除, 抽 parseMockFlag 纯函数 + 三态测试。bun test 18 pass。测试意图 (默认关闭) 完整达成。

### MISSING (漏做的承诺)
- **M1 [非阻塞, 清单已标注]**: 返工清单第 3 项 "G4 门禁升级为构建产物层校验, 记录 proposals/design/下个 sprint" 未落地。核实 proposals.md (无 G4 条目) / design.md (风险表未追加) / checklist.yaml (无 G4) / runtime-verify Rework 段均无留痕。清单原文标注"记录不阻塞", 不触发 REWORK, 但属未完成承诺, 需显式决定本轮补录或滚下 sprint。

### EXTRA (超范围)
无 scope creep。git status = pass1 时 5 tracked + 2 untracked (含新增 index.test.ts), 无 page-registry 等表外改动。parseMockFlag 抽取 / warnedOrphans 去重均是 F1/F2 修复必要技术细节, 合理 refactor。

### DEVIATED (改偏)
- **D1 [本轮引入]**: conventions.md:205/210/227 仍描述 `isMockEnabled()`, 但 F1 已把它整体移除。同批次改了 conventions (补配对/生产隔离两条款) 却未同步修正三处失效函数名。文档与代码不一致, 恰是本次安全修复 (F1) 的核心机制描述 — 正是"文档即真相"要护的漂移。
- **D2**: F2 判读边界 (清单二选一 vs 实现第三方案), 已并入兑现段, 供 evaluator 定夺。

### 总评 (PASS)
必修 F1 完全兑现且更严, 独立复核通过; F3 兑现; F2 部分兑现 (非阻塞层); M1 未落地 (非阻塞); D1 文档漂移建议 ship 前顺手一行修正, 不构成二次 REWORK。

## Evaluator VERDICT pass2 (2026-07-10, fable5)

**VERDICT: CONCERNS** (REWORK 触发点已清除; 剩 P1/P2, 无新 P0)

### 判定
- **REWORK 唯一触发点 F1 (P0) — 已解决**: 两家独立各自亲跑 production build + grep, 6 pattern (含 fixture/helper 名字) 全 0 命中 — 真 DCE 剔除, 非字符串巧合。修前同批全命中。安全攻击面 (未登录访客渲染成 fixture 用户进生产) 消除。这是解除 REWORK 的充要条件, 已满足。
- **无新 P0 / 无安全回退 / 无功能破坏**; dev mock 机制经实证未被误伤 (裸内联与原 helper 在 dev 语义等价)。
- **新增/残留 P1 (均可推后, 不触发二次 REWORK)**:
  - D1 (文档即真相): conventions.md 三处仍引用已删的 isMockEnabled — 两家独立同指。**纯文档同步, 零功能风险, ship 前必修** (见下)。
  - warn 逻辑零单测 (reviewer P1): dev-only 诊断脚手架, 测试 nice-to-have; 本 sprint 是 verify-and-rollback 形态, mock 基建属演示工具而非交付产品逻辑, 可作快速 follow-up 或 debt。
  - F2 部分解决: demo 仍 404 (带指引)。原 F2 即 P1 非阻塞; warn 是可接受缓解 (本 sprint 演示物本就验后回滚, 无长驻 demo 入口是预期)。
- **M1 (非阻塞)**: G4 门禁升级记录未落地 — pass1 已标"记录不阻塞", 本轮补录 proposals 即可。

### Evidence Cross-Check (rework 三修 + 实证)
| 修复 | evidence | 判定 |
|---|---|---|
| F1 production grep 零泄漏 | 两 subagent 各自独立重跑 build+grep 全 none | **真实 (双独立复核)** |
| F3 parseMockFlag 三态单测 | bun test 18 pass, 独立复跑一致 | **真实** |
| F2 warn 触发 | reviewer 读逻辑确认 + impl transcript 浏览器实证 | 真实 (逻辑) + transcript (运行) |
| 回归 tsc/lint/test | 两 subagent 独立复跑全绿 | **真实** |

done_without_evidence = 0。

### next_action: ship (Feature 路径无 polish), 但附 ship 前清理条件:
1. **必做 (D1, 纯文档零风险)**: conventions.md L205/210/227 的 isMockEnabled 引用同步为"裸内联 import.meta.env + parseMockFlag 规则" — evaluator 在本轮 review 收尾时直接修 (文档同步非 impl)。
2. **必做 (M1, 一行)**: proposals.md 追加 G4 门禁升级为构建产物层校验的条目。
3. **可推后 (P1 debt)**: warnOnUnpairedMockRoutes 单测 — 快速 follow-up 或记 debt; F2 demo 不可用状态已在 conventions 配对条款说明。

D1+M1 为纯文档/记录, 零功能风险, evaluator 直接补齐后即可 ship, 不需二次 impl 往返。
