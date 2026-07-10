# Runtime Verify — S4: FE 生成链闭环 (2026-07-09)

> impl + runtime-verify 由主 agent (Opus 4.8) 承载 DOER + CHECKER 环。CHECKER = tsc/lint/build 退出码 +
> G1–G6 门禁 + 浏览器真实 DOM 断言 (Claude_Browser preview pane)。所有命令与输出晒进会话 transcript。
> review 三件套 (reviewer + spec-compliance + evaluator) 按用户指令交 fable5 执行, 不在本档。

## 导航层依赖链清单 (验收 2 · critic F1 P0 核心)

访问 `/system/asset` 前, `beforeLoad` 经过的**真实网络出口**精确 2 个 (未触发备选 C 阈值 >2, 走主通道 mock):

| # | 出口 | 源 | mock 短路点 |
|---|---|---|---|
| 1 | `GET /auth/info` (当前用户) | `_authenticated/route.tsx` → `bootstrapAuthSession()` → `fetchCurrentUser()` | `src/app/auth/api.ts` `if (isMockEnabled()) return MOCK_DEMO_USER` |
| 2 | `GET /system/menu/getRouters` (菜单) | `route.tsx` + `$section/$page.tsx` → `navigationQueryOptions()` → `fetchMenuRouters()` | `src/app/navigation/api.ts` `if (isMockEnabled()) return MOCK_MENU_ROUTERS` |

权限判定 (无网络): `$page.tsx` → `ensureRouteAccess({permissions:['system:asset:list']})` 用 `MOCK_DEMO_USER`
(非 admin, 精确权限) → 判定分支真实走到, 非超管全放行。

## 测试场景 (实跑)

| 场景 | 类型 | 命令 / 动作 | 实际输出 | 判定 |
|---|---|---|---|---|
| pack 结构 | 脚本 | `check_frontend_pack.py docs/ai/convention-pack` | `{"status":"ok","errors":[],"warnings":[]}` | ✅ |
| mock 基建编译 | 类型 | `bunx tsc -b --noEmit` (asset 生成前) | exit 0 | ✅ |
| asset 生成 (generator subagent) | 生成 | scaffold-page-gen 工作流 → `src/features/system/asset/` 19 文件 + page-registry 注册 | 主 checkout 独立复核 | ✅ |
| 编译链 (主 agent 独立复核) | 类型/构建 | `tsc -b --noEmit` / `bun run lint` / `bun run build` | 0 / 0 / built 769ms | ✅ |
| asset model 单测 | 单测 | `bun test src/features/system/asset/model.test.ts` | 6 pass / 0 fail | ✅ |
| 门禁 G1–G6 (主 agent 独立复核) | 门禁 | validate.md §2, MODULE_DIR=asset | G1–G6 全 PASS | ✅ |
| 关联权限分支 | 门禁 | grep asset-management-access.ts | `canCreateAssets = system:asset:add && system:dept:treeselect` 实证 | ✅ |
| demo 探活 | API | `VITE_FEATURE_MOCK=true` dev server → `curl http://127.0.0.1:5173/` | `200` | ✅ |
| **页面渲染 (核心)** | 浏览器 | Claude_Browser preview → navigate `/system/asset` → 读真实 DOM | URL=`/system/asset` (非 403/404/sign-in); 1 table / 5 tbody 行; "新建资产"按钮渲染; console 0 error | ✅ |
| 脚手架无关 | diff | `git status` Rlues `scaffold-page-gen/` | **空 (skill 核心 diff=0 行)** | ✅ |
| 回滚回归 | 回归 | 删 asset + `git checkout page-registry` → `tsc`/`lint`/`bun test` | 0 / 0 / 15 pass (baseline 一致) | ✅ |

DOM 证据 (textSample): `系统管理 / 资产管理 ... 新建资产 ... 资产名称 资产编码 使用部门 状态 创建时间 联想 ThinkPad X1 AST-0001 资产一组 启用 ... 会议室投影仪 AST-0005 信息技术部 停用` — 5 条 mock 数据 + 部门维度 + 状态维度完整渲染。

## 自测自改记录

1. **vite 不读 shell 环境变量** (基建纠偏): `vite.config.ts` 的 `loadCustomEnv` 只读 `.env` 文件,
   `VITE_FEATURE_MOCK=true bun run dev` 的 shell 传参不生效, 而 G4 禁止写进 `.env.dev`。
   → 修: `vite.config.ts` 显式把 `process.env.VITE_FEATURE_MOCK` 注入 `define` (永久基建)。复跑 demo 渲染成功。
2. **实体级 mock 不足以打开页面** (critic F1 P0 实证): 仅短路 asset `api.ts` 的 `USE_MOCK`, 页面仍被
   `bootstrapAuthSession` + `fetchMenuRouters` 两道真实网络出口挡在 /sign-in 或 /403。
   → 修: 新增会话/导航层 mock 基建 (`src/lib/mock/`), 短路 fetchCurrentUser + fetchMenuRouters。
3. **generator worktree 隔离**: subagent 在独立 worktree 生成 (cwd 空目录), 产物同步回主 checkout 后
   主 agent 独立复核 (不采信 subagent 自报, S2 先例), 全部复核通过。
4. preview_start 用 workspace root `.claude/launch.json` 且 sh PATH 无 vite → 改用 `node_modules/.bin/vite`
   传 mock env, demo 起服成功 (临时 launch.json 验后删)。

## Reflect (还有哪里没完善)

- [x] 脚手架无关论**成立**: Rlues skill 核心 (SKILL.md / frontend-convention-pack.md / check_frontend_pack.py)
      零改动即跑通 quantum-front; 全部改动落在 quantum 侧 (pack 数据纠偏 + FE mock 基建)。
- [x] 关联权限分支实证覆盖 (asset 带 deptId + dept treeselect, 非 notice 式简单实体)。
- [ ] 未做**负面权限测试** (移除 mock 用户 system:dept:treeselect → "新建资产"应消失)。正面渲染 +
      access 文件代码已证判定分支存在; 负面留作后续 pack 回归样例, 不阻塞本 sprint。
- [ ] 未点开**新建表单**触发 dept 树下拉真实加载路径 (列表渲染已达验收; 表单交互属更深 demo)。
- [ ] mock fixtures 保留 asset 样例节点 (指向已回滚的页面): 属 demo 参考数据, 换实体时改
      `session-fixtures.ts` — 已在文件注释说明, 非死引用 (纯字符串, tsc/lint 通过)。
- [ ] 真·跨服务动态 E2E (mock off + 真实 BE) 仍是独立待办 (F7, 见 proposals.md), 不在本 sprint。

## 永久产物 vs 临时产物

- **永久** (保留): `src/lib/mock/` (基建) · auth/navigation api 短路 · vite.config + vite-env.d.ts (env 注入) ·
  conventions.md 增补 (导航 mock 约定 + 验证实体选择原则) · quantum-front `.ai_state/` (athena-init)。
- **临时** (已回滚): `src/features/system/asset/` 19 文件 · page-registry 注册行 · 根 `.claude/launch.json`。
  回滚后 `git status` 仅余永久白名单, `bun test` 15 pass 回归一致。

## VERDICT (impl+runtime-verify): PASS (8/8 验收达成)

---

## Rework (2026-07-10 · review pass1 REWORK 后, Opus 4.8)

review pass1 evaluator VERDICT=REWORK (F1 P0)。按 reviews/pass1.md 返工清单三项全修 + 实证:

### F1 (P0) — 生产构建 mock 隔离 (安全)

- 修: `vite.config.ts` 在 `mode === 'production'` 时把 `VITE_FEATURE_MOCK` 强制 pin `'false'`, 无视 shell env。
- 修: 两个短路点 (`auth/api.ts` fetchCurrentUser / `navigation/api.ts` fetchMenuRouters) 改为**裸内联** `import.meta.env.VITE_FEATURE_MOCK === 'true'` (不经 helper 调用) — 实测确认导出函数 `isMockEnabled()` 调用会阻止 vite 常量折叠, 裸内联才能 DCE。
- **实证**: `VITE_FEATURE_MOCK=true bun run build` (production, 带 stray shell flag) 后 grep 产物:

  | pattern | 结果 |
  |---|---|
  | `demo.asset` (fixture 用户名) | (none) |
  | `演示资产专员` (nickname) | (none) |
  | `asset-operator` (role) | (none) |
  | `system:asset:list` (权限点) | (none) |
  | `资产管理` (菜单 title) | (none) |

  fixtures 被 dead-code 完全消除, 生产 bundle 零 mock 泄漏。修复前同一 grep 全部命中 (fixtures 在 index chunk)。

### F2 (P1) — mock fixture 孤儿引用

- 修: 新增 `warnOnUnpairedMockRoutes()` (`session-fixtures.ts`), 在 mock 分支运行时校验菜单 component 是否在 page-registry; 缺失则 `console.warn` (去重, 每 orphan 一次)。
- 修: session-fixtures 顶部加**配对契约**注释 (机制 vs fixture 两部分, 换实体三处 lockstep); conventions.md 补配对更新条款 + 生产隔离条款。
- **实证**: dev mock 起服导航 `/system/asset` → console 输出 `[mock] menu fixture references component(s) not registered in page-registry: system/asset/index. ... regenerate it or update session-fixtures.ts`。静默 404 → 响亮带指引提示。

### F3 (P1) — mock 判定单测

- 修: 抽 `parseMockFlag(flag)` 纯函数 (mock 判定规则的可测定义); 新增 `src/lib/mock/index.test.ts` 三态测试 (true / undefined默认关闭 / 各种非 true 值)。
- **实证**: `bun test` 18 pass (15 baseline + 3 parseMockFlag)。

### rework 回归 + dev 机制未破坏

- tsc 0 / lint 0 / test 18 pass。
- dev mock 机制实证完好: 起服后已登录为 MOCK_DEMO_USER (演示资产专员/demo.asset@example.com), dashboard 完整渲染 → auth 短路生效, 裸内联与原 isMockEnabled() 在 dev 语义等价。
- git status 仍为永久白名单 (mock 基建 5 改 + src/lib/mock/ [+index.test.ts] + .ai_state/), 无演示物残留。

## VERDICT (rework): 三 finding 全修 + 实证, 待 pass2 review (fable5, 安全变更需重过完整三件套)
