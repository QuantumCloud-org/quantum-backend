# Athena 框架改进提案 (proposals)

> 铁律[Hook 是进化器]: Stop 反思沉淀于此, 定期回流 Rlues 框架仓库。

> **2026-07-14 消化对账 (Rlues 9.9.2/9.9.3 发布后)**: P1 gate schema + P6 CX 互认 ✅ 已消化;
> P2/P3 drill 相关 ♻️ 标的物已删作废/转化; P4 ⏳ 待核; P5 frontmatter 共享库 + skip flag sprint 级粒度 ❌ 仍开放;
> P7 DEPT/SELF 本仓自留。逐条明细见 `Rlues/.ai_state/architecture/blockers-and-roadmap.md` 对账表。

## 2026-07-06 · 跨平台 sprint 的 generator 证据互认

- **现象**: delivery-gate (CC) 要求 System 路径 impl 必须在 CC 端 subagent-log.md 有 generator 记录;
  但 platforms_enabled=["cx"] 的项目里, impl 常由 CX 端 generator 管线完成 (本例: Convention Pack 经 PR #2/#3 合入),
  CC 端只做 review/polish — 门禁必然误报, 只能靠 skip_impl_subagent_check 豁免。
- **提案**: delivery-gate 增加第三种通过条件 — checklist.yaml 的 done 项带 CX 侧证据
  (evidence 字段指向真实产物 + evaluator Evidence Cross-Check PASS) 时, 视同 generator 记录;
  或 CX 端 subagent-tracker.py 在 ship 前把 spawn_agent 记录镜像写入 sprints/{slug}/subagent-log.md 统一格式。
- **触发 sprint**: 2026-07-06-ai-capability-architecture-design (豁免留痕见其 cleanup-pass.md)。

## 2026-07-06 · frontmatter 解析需要单一共享实现

- **现象**: CC 端 4 个 hook 各自复制粘贴 readFrontmatter, 其中"剥首尾引号"逻辑对带行尾注释的值出错,
  实际制造过脏 sprint 目录; 修复要同步 8 个文件 (部署 + Rlues 源) — 见 Rlues 366ee6b。
- **提案**: 抽 `~/.claude/hooks/lib/frontmatter.cjs` 共享模块 (或构建期注入), 消灭 4 份拷贝;
  CX 端 Python 同理 (read_field 已散在 5+ 文件, 目前实现碰巧无病但同样脆弱)。

## 2026-07-09 · drill 脚本 test_account_doc 检查路径重复

- **现象**: `Rlues/scripts/test-end-to-end-drill.py` 的 "OAuth/test account handoff" 检查项 (`test_account_doc`,
  L95-97) 与 "backend-runtime-env" 检查 (L58-62) 指向**同一文件** `docs/ai/convention-pack/runtime-env.md` —
  runtime-env.md 一落地两个 blocker 同时消失, `mcp-test-access.md` 永远不被 drill 校验。
- **提案**: `test_account_doc` 改指 `docs/ai/mcp-test-access.md` 真路径; 同时 cowork 检查升级为文件级
  (当前只判 `docs/ai` 目录存在, 不校验 README/runtime-env/mcp-provider 三件套)。
- **触发 sprint**: 2026-07-09-be-runtime-contract-hardening (critic Round 1 F4) +
  2026-07-09-cowork-runtime-contract-docs (critic F2)。

## 2026-07-09 · drill 只是静态基线, 真·动态 E2E 缺独立载体

- **现象**: `test-end-to-end-drill.py` 本质只做文件存在性 + git fetch, 不起服务、不走 OAuth→tool-call 链;
  三个 runtime contract sprint 清完后 drill 转绿 ≠ 跨服务联调已验证 (F6 runtime-verify.md Reflect 自认)。
- **提案**: fullstack-delivery roadmap 增补 F7 (真·动态 E2E): 起 FE(mock off)+BE, 走
  authorize→consent→token→/mcp tools/call 全链 + playwright-e2e 证据; 前置 = S4 + be-hardening + cowork-docs 三 sprint。
- **触发 sprint**: 本轮三 sprint 设计 critic Round 1 全局 G2。

## 2026-07-10 · scaffold-page-gen G4 门禁应升级为构建产物层校验

- **现象**: FE Convention Pack 的 G4 门禁 (validate.md §2) 只 grep `.env.dev`/`.env.produce` 有无
  `VITE_FEATURE_MOCK=true`, 挡"已提交文件"向量; 但 **shell 传参 → production build** 这条向量
  完全没覆盖 (开发者跑完 `VITE_FEATURE_MOCK=true bun run dev` demo 后同 shell 跑 `bun run build`,
  或 CI 复用容器 env, 会把会话/导航 mock 短路烤进生产 bundle)。S4 review pass1 F1 (P0) 即此洞。
- **提案**: G4 从"grep .env 文本"升级为**构建产物层真实校验** —
  `VITE_FEATURE_MOCK=true bun run build` 后 grep `dist/` 确认无 `VITE_FEATURE_MOCK`/fixture 名残留;
  或 CI 显式 `unset VITE_FEATURE_MOCK` 后再跑 production build。S4 已在 `vite.config.ts` 用
  `mode==='production'` pin 'false' + 短路裸内联 DCE 从代码侧堵死, 但 pack 门禁文本尚未同步这条验收。
- **触发 sprint**: 2026-07-09-s4-fe-scaffold-loop-verify (review pass1 F1 / pass2 evaluator 返工清单第 3 项)。

## 2026-07-10 · delivery-gate 新档案要求缺文档, ship 现场 5 轮试错

- **现象**: delivery-gate 升级后要求 subagent-assignments/events.jsonl (schema_version=1 精确 6/6 字段 +
  Start≤assign≤Stop 时序)、checklist status 字面量 `completed` (非 done)、evidence.yaml 每条带
  `result: pass|fail|unknown`、review 末行整行 `VERDICT: PASS`、design.md 含字面 "Critic Findings" 段、
  design mtime ≤ latest review — 这些约定**没有任何 skill/文档记载**, S4 ship 时靠读 hook 源码 + 5 轮
  现场试错才过检。主 agent 正常执行流程 (subagent 真跑了、critic 真审了) 仍被拦, 纯格式债。
- **提案**: ① Rlues 侧把 gate 档案 schema 写进 pace/references 或 athena-review skill (含模板);
  ② 更优: subagent-tracker hook 在 spawn/stop 时**自动写** assignments/events.jsonl (机器生成机器验,
  别让 agent 手补); checklist/evidence 模板默认用 gate 认可的词表。
- **追加 (2026-07-11, cowork 复踩)**: `skip_impl_subagent_check` 是**项目级** flag 而 gate 检查的是
  current_sprint — 纯文档 sprint ship 后想复位 flag (防未来代码 sprint 被跳检) 会被 gate 反拦
  (指针仍指旧 sprint, 复位即要 generator 档案)。建议 ③: 豁免改 **sprint 级粒度** (如 checklist.yaml
  或 route-note frontmatter 里声明 `no_generator: docs-only`), gate 按 sprint 自身声明判定,
  项目级 flag 废除 — 同一坑 S4/cowork 两轮各踩一次。
- **触发 sprint**: 2026-07-09-s4-fe-scaffold-loop-verify (ship 收尾, 本条) + 2026-07-09-cowork-runtime-contract-docs (追加段)。

## 2026-07-10 · DataPermissionInterceptor 分支内字段级 fail-open (相邻风险, 本 sprint 未触)

- **现象**: fail-closed 加固 (be-runtime-contract-hardening) 修的是 **switch 缺 default 导致枚举漏 case 静默放行**
  这一层 (已加 `DENY_ALL` case + `default -> 1=0`)。但 generator 盘点发现相邻未修点: `DEPT` case 内
  `deptId==null` 时、`SELF` case 内 `userId==null` 时, 仍是"条件为 null 就不追加任何过滤" (字段级 fail-open),
  与 `DEPT_AND_CHILD/CUSTOM` 已有的 `else → 1=0` fail-closed 分支**不对称**。
- **现状风险低**: 生产路径 deptId/userId 始终有值 (登录时计算), 全量 16+回归测试未暴露; critic F2/F3 与
  本 sprint design 明确只指向 switch-default 层, 未要求改分支内部。
- **提案**: 后续单独立项把 `DEPT`/`SELF` case 内的 null 分支也补 fail-closed `else → and("1=0")`, 使三类
  数据域分支的 null 处理对称。非紧急, 攒进下一次 orm 加固。
- **触发 sprint**: 2026-07-09-be-runtime-contract-hardening (generator 盘点发现)。


## 2026-07-14 · 9.9.3 review-manifest 对历史/外部项目 sprint 追溯误拦

> ✅ **已修复 (2026-07-14 hotfix, Rlues 6e7a4b9)**: manifest 按 path 分级 + opt-in (R/S 强制) +
> Evidence Cross-Check 限 R/S + ship 期维护写入放行 + idle 态合法化, 双端同构落地并同步 9.9.3 分发包。

- **现象**: 9.9.3 delivery-gate 的 `review-manifest.yaml` 要求**精确 9 个文件** hash
  (含 `rework-notes.md` / `tdd-evidence.yaml` / `architecture/athena-9.9.3.md`) — 这是 Rlues 自身
  release sprint 的专属产物集, 硬编码进了通用 gate。quantum-backend 的 be-runtime-contract-hardening
  (9.9.1 时代已 ship 的 Feature sprint, 无 polish/rework, 无 athena-9.9.3.md) 被追溯拦截,
  且 stage=ship 期间 gate 拦所有写入 → 状态机死锁 (改 stage 也要写文件), 只能经 Bash 通道解。
- **提案**: ① manifest required 文件集按 path 分级 (Feature 不要求 rework/cleanup/tdd 档) 且
  `architecture/athena-*.md` 类版本档案不进通用 schema; ② gate 新档案要求只对 schema 升级**之后
  开工**的 sprint 生效 (按 route-note 时间戳判定), 不追溯已 ship sprint; ③ 与既有 "sprint 级豁免粒度"
  提案同属一类: gate 判定依据应来自 sprint 自身档案而非全局硬编码。
- **追加 (2026-07-14 第三次触发)**: Quick 路径 (be-env-compose) ship 收口后同样被 review-manifest 拦 —
  证明该检查独立于 GENERATOR_PATHS, **所有 path 的 ship 终态都会被追打**, 收口后只能靠"立项下一个
  sprint 前移指针"逃逸。修复优先级应提到最高。
- **触发 sprint**: 2026-07-09-be-runtime-contract-hardening (被追溯拦) → 2026-07-14-be-env-compose (立项消解, 后自身又被拦) → 2026-07-14-first-biz-module-loop (再次前移消解)。
