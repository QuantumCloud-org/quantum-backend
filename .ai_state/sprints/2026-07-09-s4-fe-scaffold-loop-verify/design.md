# Design — S4: FE 生成链闭环 (scaffold-page-gen 端到端实跑 + 脚手架无关验证)

> Sprint: `2026-07-09-s4-fe-scaffold-loop-verify` · Path: Feature · 置信度 0.82 (见 route-note.md)
> 状态: design Round 2 (critic Round 1 F1 P0 / F2 P1 已修) → 待确认进 impl

## 背景

- S 系列 (docs/ai-sprint-design.md §8) S0–S3 已 ship, S4 = quantum-front Convention Pack 实跑验证。
- F2 (Rlues fullstack-delivery) 已交付 scaffold-page-gen skill + quantum-front adapter + FE Convention Pack
  (conventions/validate/runtime-env/templates, 位于 `quantum-front/docs/ai/convention-pack/`)。
- 缺口: skill 与 pack 只做过静态契约校验 (F6 drill `static-ok-dynamic-blocked`), **从未在 quantum-front 上端到端实跑**。
- S4 核心论证: **脚手架无关** — skill 核心 (SKILL.md / workflow / scripts) 零改动, 只换 Convention Pack 即可对接 FE。
  S2 是 BE 侧同构先例 (quantum-biz-asset 零返工 BUILD SUCCESS), S4 复制该验证形态到 FE。
- 附带产出: quantum-front 执行 athena-init, 建立 FE 自有 `.ai_state/` (route-note 已定: 本 sprint 状态仍落 BE .ai_state 作 bridge)。

## 关键设计事实 (critic Round 1 核实)

1. **导航层无 mock 通路 (F1 P0)**: 动态路由 `src/routes/_authenticated/$section/$page.tsx` 的 `beforeLoad`
   强制 `ensureQueryData(navigationQueryOptions())` → `fetchMenuRouters()` 真实请求 `/system/menu/getRouters`;
   匹配不到菜单节点即 `redirect('/403')`。现有 mock 约定只覆盖实体级 `api.ts` 的 `USE_MOCK` 短路,
   不覆盖 `src/app/navigation/api.ts`。**不补导航 mock, mock-only demo 的页面永远打不开。**
2. **演示实体选择教训 (F2 P1 / 全局 G3)**: `compound/2026-07-06-learning-templates-replicate-fixed-vulnerabilities.md`
   记录过 SysNotice (无部门维度) 掩盖数据权限分支缺口的返工。演示实体必须含部门/角色关联字段。
3. `system/asset` 在 `src/features/system/` 与 `page-registry.tsx` 均无冲突 (critic 已核实同名 notice 也无冲突, 本设计直接换 asset)。

## 方案

### 选定: 导航层 mock 补齐 + 带关联维度演示实体全流程生成 + 验后回滚

演示实体: **`system/asset`** (资产, 与 S2 BE 先例同构: assetName/assetCode/**deptId**/status)。
含部门树关联下拉 → 强制覆盖"关联操作前置权限一并校验"分支 (`canCreateAsset && system:dept:treeselect`),
不重演 notice 式简单实体掩盖缺口。

按 scaffold-page-gen SKILL.md 工作流执行:

| 步骤 | 动作 | 证据落点 |
|---|---|---|
| 0 | 环境预检: `bun install` (lockfile 未变可跳) + 端口 5173 空闲 | runtime-verify.md |
| 1 | quantum-front 执行 athena-init (建 `.ai_state/_index.md`, 探测 bun/rg/jq) | FE `.ai_state/_index.md` |
| 2 | `python3 scripts/check_frontend_pack.py quantum-front/docs/ai/convention-pack` | stdout PASS |
| 3 | **导航层 mock 基建** (永久改动, 非回滚项): 探明 `_authenticated` beforeLoad 完整网络依赖链 (auth 会话 + `fetchMenuRouters`), 为 `src/app/navigation/api.ts` (及链上其他出口) 加 `VITE_FEATURE_MOCK` 短路; mock 菜单节点的 `component` 必须精确匹配 page-registry `backendComponent`; conventions.md mock 章节同步增补导航层约定 (pack 纠偏留痕) | 依赖链清单 + diff |
| 4 | CC generator subagent 按 skill 生成 `system/asset` 完整页面模块: page / api / model / mock / `asset-management-access.ts` (含 dept treeselect 前置权限) / page-registry 注册; API 契约冻结为 mock schema (分页 `pageNum/pageSize/total/pages/records`) | 生成文件清单 |
| 5 | 校验链: `bunx tsc -b --noEmit` → `bun run lint` → `bun run build`, 失败读错自修回环 | 命令输出 |
| 6 | 安全门禁 G1–G6 (validate.md §2), 主 agent **独立复核**不采信 subagent 自报 (S2 先例) | G1–G6 结果表 |
| 7 | `VITE_FEATURE_MOCK=true bun run dev -- --host 127.0.0.1 --port 5173` 起 demo, 探活 200, 经 mock 导航直达 `system/asset` 页面并渲染列表 (非 403/404/降级态) | HTTP 200 + 页面日志/截图 |
| 8 | 回滚演示生成物 (asset 模块 + 注册行), **保留**导航 mock 基建与 pack 纠偏; `git status` 仅余永久项, `bun test` 回归 | 回滚证据 |

**脚手架无关判定**: 全程对 Rlues skill 核心 (`SKILL.md` / `references/frontend-convention-pack.md` /
`scripts/check_frontend_pack.py`) diff = 0。允许的例外: `references/quantum-front-adapter.md` 与
FE Convention Pack 的**数据纠偏** (含步骤 3 导航 mock 约定增补, 留痕继续); 若必须改 skill 核心 → 停 impl, 上报设计发现。

### 备选对比

| 备选 | 内容 | 处置 |
|---|---|---|
| A: 生成物保留合入 main | asset 页面当真功能交付 | 否决: S4 验证 skill/pack 而非交付业务; 与 S2 先例一致回滚 |
| B: 只跑静态校验不起 demo | tsc/lint/build 即收工 | 否决: F6 已覆盖静态面, S4 增量价值在动态实跑 |
| C: 对接真实 BE 联调 | 起 quantum-server 供导航/登录/数据接口 | **降级通道保留**: 若步骤 3 探明 mock 依赖链 >2 个网络出口 (短路成本失控), 切换 C — 前提是 `be-runtime-contract-hardening` sprint 先完成 runtime-env.md; 触发即在 route-note 追加 re-route 记录 |

## 影响范围

| 目标 | 改动 | 持久性 |
|---|---|---|
| quantum-front `src/features/system/asset/` + page-registry 注册行 | 演示生成物 | **临时**, 步骤 8 回滚 |
| quantum-front `src/app/navigation/api.ts` (及依赖链出口) | USE_MOCK 短路 | **永久** (mock 基建) |
| quantum-front `docs/ai/convention-pack/conventions.md` | 导航 mock 约定 + 验证实体选择原则 (须含部门/角色关联字段) | 永久, 留痕 |
| quantum-front `.ai_state/` | athena-init 产物 | 永久 |
| quantum-backend `.ai_state/` | 本 sprint 档案 (bridge) | 永久 |
| Rlues skill 核心 | 零改动 (验收标准) | — |

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| 导航 mock 依赖链过深 | 步骤 3 先探链再动手; >2 个出口触发备选 C 降级通道 |
| skill 核心与 FE pack 不兼容, 非改不可 | 廉价退出: 停 impl 上报设计发现 |
| mock 开关误提交 | G4 门禁 (.env.dev/.env.produce 无 VITE_FEATURE_MOCK=true) |
| 端口 5173 被占 | runtime-env teardown 约定 |
| 回滚残留 | `git status --porcelain` 仅余永久项白名单 + `bun test` 回归作 ship 门禁 |
| generator 自报不实 | 主 agent 独立复核编译链 + G1–G6 (S2 先例固化) |

## 验收标准 (8 条, 全部达成才进 review)

1. `check_frontend_pack.py` PASS。
2. 导航层 mock 基建落地: 依赖链清单落 runtime-verify.md, mock 菜单 `component` 与 page-registry `backendComponent` 精确匹配。
3. 生成模块 `bunx tsc -b --noEmit` + `bun run lint` + `bun run build` 全绿 (0 error)。
4. G1–G6 全 PASS; `asset-management-access.ts` 含 dept treeselect 前置权限合并判断 (关联分支实证覆盖)。
5. mock demo 探活 200, 且**经导航直达** `system/asset` 页面渲染列表 (非 403/404/降级态), 证据入 runtime-verify.md。
6. Rlues skill 核心 diff = 0 行; adapter/pack 纠偏逐条留痕。
7. 回滚后 `git status` 仅余永久项 (导航 mock/pack/.ai_state), `bun test` 回归通过。
8. quantum-front `.ai_state/_index.md` 存在且探测字段属实。

## 依赖与衔接

- 主通道 (mock) 不依赖 BE sprint; 备选 C 触发时依赖 `2026-07-09-be-runtime-contract-hardening` 的 runtime-env.md 先行。
- **BE .ai_state 状态写入互斥** (critic G1): S4 与 BE sprint 共享 `_index.md` 单例字段, 串行推进 —
  S4 先行 (用户指令), BE sprint 次之, `current_sprint_slug` 一次只指向一个。
- 三 sprint (S4 / BE / cowork) 全清后重跑 `Rlues/scripts/test-end-to-end-drill.py` — 效果是 **drill 静态基线转绿**
  (该脚本只做文件存在性 + git fetch 检查, 不起服务)。**真·跨服务动态 E2E (起 FE+BE 走 OAuth→tool-call 链) 是独立待办**,
  不在本三 sprint 范围, 完成后另立 sprint (见 _index.next_action)。
- S5 (下游 chat 产品对接 MCP) 为平面 C 独立项目; 接口冻结已由 S3 preflight 完成。

## impl 关注清单 (critic Round 2 P2, 非阻塞)

1. 依赖链清单必须含 **mock 会话用户的权限点/角色**: `ensureRouteAccess` 依赖 `meta.permission`/`meta.roles`
   判定链 — mock 用户若无 asset 对应权限, 过了导航仍会在第二道 `/403` 卡住。
2. 备选 C 触发阈值 "网络出口 >2" 在步骤 3 探链时**定义为具体请求列表** (auth 会话可能拆多个请求, 计数边界要先钉死)。

## Critic Findings (审议记录)

### Critic Findings Round 1 (2026-07-09, critic acfd82c7a06340fdb) — NEEDS_REVISION

- F1 **P0**: mock-only demo 无法达成验收 4 — `_authenticated` 动态路由 beforeLoad 强制真实
  `fetchMenuRouters()`/auth 会话, 实体级 mock 不覆盖导航层, 页面永远打不开 → 修: 新增步骤 3 导航层
  mock 基建 (依赖链探明 + navigation api 短路 + component 精确匹配), 备选 C 从否决改为量化降级通道。
- F2 P1: 演示实体 notice 复刻历史教训 (无部门维度掩盖数据权限分支, compound/2026-07-06-learning) →
  修: 换 `system/asset` (含 deptId + dept treeselect 前置权限进验收 4)。
- F3 P2: `system/asset` 与现有代码无冲突 (已核实)。
- 全局 G1 (BE .ai_state 单例竞态) → 修: 串行互斥条款; G2 (drill 措辞过度) → 修: "静态基线转绿";
  G3 (验证实体原则制度化) → 修: 写入 FE/BE conventions。

### Critic Findings Round 2 (2026-07-09, 同 critic 续审) — PASS

- Round 1 findings 全部 CLOSED (逐条核实修法落盘)。
- 残留 2 条 P2 (非阻塞) 记入上方 "impl 关注清单"。
