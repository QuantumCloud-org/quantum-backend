---
# Athena PACE 项目状态 (.ai_state/_index.md)
# v9.9.0 schema. 项目执行 athena-init 时由模板初始化, 之后由主 agent + hooks 维护.
version: "9.9.0"

# === PACE 路由状态 ===
path: "System"                    # Hotfix | Bugfix | Quick | Feature | Refactor | System
stage: "ship"                     # 依赖升级切片：6ea787a 已 push；polish 补跑完成 (F1 已修 + cleanup-pass.md)，收口第二个 commit
current_sprint_slug: "2026-07-30-security-dependency-refresh"
current_roadmap_slug: ""          # 仅 roadmap stage 期间填
skip_polish: false                # 项目级 opt-out (默认 false)
skip_architecture_check: false    # System/Refactor ship 前是否跳过 architecture 更新检查
skip_runtime_verify: false        # v9.8.0: true 跳过运行时验证 (纯库/无运行环境才设; System/Refactor 不建议)
harness_target_outside_repo: false # 依赖升级切片改动全在 repo 内 (root pom.xml)；安全扫描临时 artifacts 随后继 sprint 走

# === 路由审议 (v9.9.0) ===
route_confidence: 0.96            # 0-1, 入口路由审议置信度 (主 agent 审议 Step 3 写)
route_history: ["2026-07-05 Quick: merge origin/main + local workspace, run athena-init, test, push main", "2026-07-06 Refactor: implement 7 cross-module backend security hardening fixes", "2026-07-06 Refactor: harden import contract and consolidate data-scope runtime source", "2026-07-06 System: design AI capability architecture for Claude review", "2026-07-06 Feature: S2 scaffold-module-gen end-to-end loop verify (生成物临时, 验后回滚)", "2026-07-07 System/design-only: quantum-mcp S3 preflight design + state closeout", "2026-07-07 System: implement quantum-mcp S3 OAuth/MCP skeleton", "2026-07-09 Feature: S4 FE scaffold loop verify (design R2, critic R1 P0 已修)", "2026-07-09 Feature: be-runtime-contract-hardening 立项排队 (S4 后接棒)", "2026-07-30 System: 四个 Maven 模块分别做全仓漏洞扫描基线、官方 registry 全量直依赖核验与最新稳定版升级，跨模块/BOM/运行时验证且可能含 breaking 迁移；置信度 0.96", "2026-07-31 System 内切片: 用户显式批准把 sprint 拆为「依赖升级切片」(1 文件 8 行, 绿区, 本轮直推 main) + 「安全报告切片」(44 findings writeup/PoC, 另立 sprint 走全门禁); 降级仅覆盖前者"]
plan_model: ""                    # "" | "fable" — System/Refactor 的 plan/design 审议切 fable-5 (贵, opt-in)

# === 平台与版本 ===
platforms_enabled: ["cx"]         # cc | cx | both
cc_version: ""                    # 由 athena-init 探测
cx_version: "codex-cli 0.142.5"
ag_callable: false                # antigravity (agy) 可调度?

# === 平台原生能力 (athena-init 探测) ===
platform_features:
  cc_subagent_task: false         # CC Task tool (always true)
  cc_ultrathink_supported: false  # CC v2.1.68+ ultrathink keyword
  cc_isolation_worktree: false    # CC v2.x+ subagent frontmatter isolation: worktree
  cc_subagent_stop_hook: false    # CC SubagentStop 原生事件
  cc_worktree_hooks: false        # CC WorktreeCreate/Remove 原生事件
  cc_stop_prompt_hook: false      # CC Stop hook prompt 类型 (2026-03+)
  cx_spawn_agent: true            # Codex spawn_agent (0.128+)
  cx_plan_mode_reasoning_effort: true    # Codex 0.105.0+ plan_mode_reasoning_effort
  cx_goal_default_on: true        # Codex Goals 默认可用 (0.133+)
  cx_spawn_agents_on_csv: true    # 实验性
  ag_parallel_subagents: false    # Antigravity 并行
  ag_headless_p: false            # agy -p

# === 工具可用性 (athena-init 探测) ===
tools_available:
  context7_cli: false             # npx ctx7 可用
  context7_mcp_cx: false
  augment_mcp_cc: false
  augment_mcp_cx: false
  web_search_cc: true             # CC WebSearch (always true)
  web_search_cx: true             # Codex web_search = "live"
  rg_available: true
  jq_available: true
  agentshield_cli: false          # ECC AgentShield (可选)
  vm_available: false             # v9.9.0: ~/.athena/vm.json 存在且 athena-vm doctor 连通

# === 进度计数 (index-updater hook 自动维护, 不手填) ===
counts:
  features_count: 4
  issues_count: 0
  refactors_count: 0
  systems_count: 4
  requirements_count: 1
  reviews_count: 14
  cleanup_count: 6
  compound:
    learning: 1
    trick: 0
    decision: 1
    explore: 0

# === Pointers (指向最新相关文件) ===
pointers:
  latest_design: "sprints/2026-07-30-security-dependency-refresh/design.md"               # sprints/{current_sprint_slug}/design.md
  latest_review: "sprints/2026-07-09-s4-fe-scaffold-loop-verify/reviews/pass2.md"
  latest_cleanup: "sprints/2026-07-30-security-dependency-refresh/cleanup-pass.md"
  latest_brainstorm: ""
  latest_decisions: ["compound/2026-07-06-decision-codegen-security-gates-default-on.md"]
  latest_lessons: ["compound/2026-07-06-learning-templates-replicate-fixed-vulnerabilities.md"]
  latest_architecture_update: "2026-07-31T06:39:51.092Z"
  latest_requirement: "requirements/ai-capability-platform.md"

# === PACE 联动字段 (v9.8.0 新, hook 自动维护) ===
next_action: "另立 sprint 续做安全报告切片: 44 条 writeup/PoC + hardening_final → finalizer → PACE review 2+1 (AC1 与 Done Contract #1/#6 仍未闭环)"
last_subagent: "evaluator"
last_subagent_at: "2026-07-08T09:00:20.936764Z"
active_worktrees: []              # 2026-07-31 移除 quantum-backend-security-deps: 零 commit/零分支, pom.xml 与主 checkout 逐字节相同, 无可合并内容
last_critic_round: 3              # plan stage critic 已跑轮数
design_changed_after_impl: false  # 2026-07-31 用户显式批准: 依赖升级切片 (1 文件 8 行, 绿区) 免 review 三件套直推 main; 该门禁义务随安全报告切片转入后继 sprint

# === 用户偏好 ===
plan_critique_max_rounds: 4       # 默认 4, 可调 2-6
plan_critique_min_rounds: 0       # v9.9.0 (U2): 0=auto (Refactor/System=2, 其余=1); delivery-gate 在 ship 验 design.md 轮数
plan_critique_disabled: false     # 关闭多轮 critique (用户自负责)
skip_impl_subagent_check: true    # 2026-07-31 用户显式批准豁免 (本 sprint 限定, 下个 sprint 须复位 false)。
                                  # 理由: gate 的 validateGeneratorChain 认 role==="generator" 严格相等, 而本项目
                                  # platforms_enabled=["cx"], CX 侧握手写的是描述性文案 "PACE generator for
                                  # isolated dependency upgrade" (task_name: backend_dependency_generator),
                                  # 31 条 assignment 严格匹配 0 条。实质要求 (实现写入经法定 subagent 而非主 agent)
                                  # 事实上满足: 依赖升级由该 generator 落, polish 的 F1 由 polish-worker 落。
                                  # 选择豁免开关而非改写历史 JSONL 的 role 字段 —— 后者是重写审计记录迎合门禁。
                                  # 词表不匹配缺陷已上报 proposals.md (2026-07-31 条)。
network_in_polish: true           # polish_worker 是否允许 network

# === Fingerprint (index-updater 用于 mtime 比对) ===
fingerprint: ""
---

# Athena Project State Index (v9.9.0)

> 本文件由 Athena 自动维护. 不要手工修改 frontmatter 字段以外的部分除非你知道你在做什么.

## 当前状态

> 技术栈 / 阻碍 / 完善路线 (三步走 quantum→Rlues→AI infra): [architecture/blockers-and-roadmap.md](architecture/blockers-and-roadmap.md)

- 2026-07-05: /athena-init 完成；项目位于 CX-only 模式，CX=codex-cli 0.142.5，CC/AG/ctx7 未探测到，rg/jq 可用，VM 未注册。
- 2026-07-05 22:12: origin/main 已先快进到 5c0fd54，再叠加本地业务改动与 Athena 初始化；`mvn test` 通过；提交 71ae4b7 已推送到 main。
- 2026-07-06: 后端安全修复 sprint 完成 7 个修复点，进入 ship 合并；review、runtime-verify、cleanup 与 architecture 档案已落盘。
- 2026-07-06: 非 AI 体系剩余项修复完成: 用户导入契约、导出角色回填、数据域 fail-closed、登录态数据权限来源收敛、MVC/服务层测试; AI 审查/生成体系保留给后续 review。
- 2026-07-06: AI 能力接入进入 System/design: 已落盘 `quantum-biz-ai` 模块化单体方案、Provider/SSE/Tool-MCP/RAG/配额审计设计, 等 Claude review 后再实现。
- 2026-07-06 20:35: AI 能力架构 sprint 走完全程 (design→review CONCERNS→polish P1×3 闭环→ship df6b729 已推送); Convention Pack 数据权限默认启用 + G1-G4 门禁; compound×2 + architecture/ai-collaboration.md 落盘。
- 2026-07-07 11:04: FE/BE Convention Pack 扩充 sprint 已 ship: BE `865a7bf` / FE `8f4d5ab` 均与 origin/main 对齐; 旧 agent worktree 现场已清理。
- 2026-07-07 12:10: S3 `quantum-mcp` 开工前设计冻结: OAuth token 独立 store、consent 页、Capability Manifest v1 / Bearer 身份传递已落盘; 下一步进入代码实现 sprint。
- 2026-07-07 12:31: S3 `quantum-mcp` 实现 sprint 已进入 ship: 新增 module、独立 OAuth store、Bearer filter、well-known/OAuth endpoints、MCP JSON-RPC `initialize/tools/list/tools/call`、首批只读 tools; `mvn -pl quantum-server -am test` 通过。
- 2026-07-08 16:38: Critic Round 1 发现 OAuth scope 未进入 tool 授权链; 已补 `McpAuthenticationDetails` + Manifest `scope` + tool scope guard, `mvn -pl quantum-mcp -am test` 通过。
- 2026-07-09: S4 (FE 生成链闭环) design R2 落盘: critic R1 抓 P0 (导航层无 mock 通路 / BE actuator 前提失实 / interceptor switch 哑弹), 演示实体 notice→asset, 已全修。模块遗留立项 2 sprint: be-runtime-contract-hardening (本仓) + cowork-runtime-contract-docs (cowork 仓)。proposals +2 (drill 路径重复 / F7 真动态 E2E)。推进序: S4 → BE 接棒, current_sprint_slug 串行互斥。
- 2026-07-09: critic Round 2 复核 **三份设计全 PASS** (S4 / BE / cowork), 4 条 P2 观察项已写入各 design 的 impl 关注清单。stage=design 完成, 待用户确认后 S4 进 impl。
- 2026-07-10: be-runtime-contract-hardening ship (28fdf8b, Opus 4.8): DataScope fail-closed (DENY_ALL + switch default 兜底 + SystemDataScope 嵌套逃生门 + aspect 三序) + runtime-env/mcp-test-access 文档 + conventions 验证实体原则. 盘点 @DataScope 仅 2 处业务零改; 16 orm 测试 + 全量 11 模块回归全绿; drill backend-runtime-env 转绿; review pass1 PASS (0 P0/P1). boot 证据环境 blocker; DEPT/SELF 字段级 fail-open 回流 proposals.
- 2026-07-10: S4 review pass1 (fable5 三件套): reviewer 1×P0+2×P1, spec-compliance PASS (MISSING/EXTRA/DEVIATED=0, 5/8 AC 独立复核), evaluator **REWORK** — F1 P0 vite.config 生产构建未隔离 VITE_FEATURE_MOCK 注入 (design 缓解缺口, 非 impl 偏离)。next_action=rework_impl, 最小修复集 3 项见 pass1.md。
- 2026-07-10: S4 rework (Opus) F1/F2/F3 全修 → pass2 review (fable5 三件套) VERDICT=**CONCERNS**: F1 P0 已解决 (reviewer+spec-compliance 各自独立重跑 production build+grep, 6 pattern 全 0 命中, 真 DCE 剔除)。新增 P1: D1 conventions.md 仍引用已删 isMockEnabled (文档漂移) + warn 逻辑零单测。ship 前 D1 (文档同步) + M1 (G4 门禁升级 proposals) 已由 evaluator 补齐; warn 单测记 deferred P1 debt。next_action=ship。
- 2026-07-09: S4 impl+runtime-verify 完成 (Opus 4.8), **8/8 验收 PASS**。永久基建: quantum-front 会话/导航层 mock (src/lib/mock/ + auth/nav api 短路 + vite.config env 注入) + conventions.md 增补 (导航 mock 约定 + 验证实体原则) + FE athena-init (.ai_state)。generator subagent 生成 system/asset 19 文件, 主 agent 独立复核 tsc/lint/build/G1-G6 全绿, Claude_Browser 实测页面渲染 5 行列表 (非降级态)。脚手架无关论成立 (Rlues skill 核心 diff=0)。演示物验后回滚, bun test 15 pass 回归。runtime-verify.md 落 bridge; stage=review 交 fable5。
- 2026-07-31 02:00: **依赖升级切片 ship** (安全报告切片拆出另立 sprint, 用户显式批准)。root `pom.xml` 8 行: 8 个组件升到 Maven Central 最新稳定版 + 删除冗余 Boot/Netty/Jackson BOM import (改用 Boot 4.1.0 官方覆盖属性)。主 agent 独立复核 (非转述 worker 报告): `dependency:list` 实解析 8/8 命中目标版本 (Hutool 5.8.47 / Jackson3 3.2.1 / Jackson2 2.22.1 / HikariCP 7.1.0 / Netty 4.2.16.Final / Tomcat 11.0.24 / PG 42.7.13 / S3 2.49.6); `mvn clean test` 11/11 模块 BUILD SUCCESS, 101 tests 0 failure/error/skip。worktree `quantum-backend-security-deps` 零 commit 零分支、pom 与主 checkout 逐字节相同 → 无可合并内容, 已 remove+prune。ARCHITECTURE.md 新增「依赖治理」节 (单一入口 = spring-boot-starter-parent; 完成证据取实解析版本而非 property 文本)。**未闭环**: AC1 安全扫描 canonical 产物未入仓 (仍在仓外临时 artifacts)、review 三件套未跑、cleanup 未做 → 全部转入后继安全报告 sprint。
- 2026-07-31 03:10: **polish 补跑闭环** (delivery-gate 在 Stop 拦下 "System polish 未跑", 未绕过)。cleanup-pass.md 落盘 4 条 finding: F1 (P1 已修) 删除死属性 `spring-boot.version` —— 删掉 Boot BOM import 后它失去唯一 Maven 消费者, 而 `<parent>` 已硬写 4.1.0, 留着就是同一版本号两处存放, 下次升 Boot 必静默漂移; F2/F3 (P2 记录不修), F4 (P1 已修, architecture 依赖治理节)。可达性论证覆盖三条路径: 不限文件类型全仓检索 / 资源过滤间接消费 (parent POM 只过滤 `application*` 且分隔符 `@` + `useDefaultDelimiters=false`) / 上游 POM 继承链 (两个 Boot POM 对该属性命中数 0)。**验证**: `mvn clean test` 101 tests 全绿; 解析集合 A/B 232 vs 232 diff 为空 (11/11 模块全覆盖)。过程两次自纠已如实记入 cleanup-pass: worker 推翻自己对 `dependency:list` EXIT=1 的首轮归因并主动暴露 "10/11 覆盖" 缺口; 主 agent 首轮 A/B 因 Bash cwd 跨调用保持而两侧可能同源, 作废重做 (改 `-f` 绝对路径 + 跑前先验两侧 POM 确实不同)。**proposals +1**: delivery-gate `role === "generator"` 严格相等与实际描述性 role 词表不匹配 (31 条 assignment 严格匹配 0 条), 且 6ea787a 是在该检查从未通过的情况下推出去的 —— 未改 JSONL 迎合门禁, 按 铁律[Hook 是进化器] 上报。

## 工具调度建议

根据 `tools_available` + `platform_features`, 主 agent 进入每个 stage 时按下表选工具:

### brainstorm stage
- 主 agent 与用户对话, 不读 compound (创意空间不污染)
- 不 spawn subagent, 不 worktree

### roadmap stage
- 主 agent 调研 + 用户确认
- 输出 items.yaml + roadmap.md

### plan / design stage (强制 critique)
- 主 agent 用 ultrathink (CC) / xhigh (CX) 出 design.md 初版
- spawn `critic` subagent (独立 context, read-only)
- 最多 `plan_critique_max_rounds` 轮 (默认 4)
- PASS 才进 impl/design

### impl stage (subagent 始终用)
- CC: Task `generator` subagent
- CX: spawn_agent `generator.toml`
- Refactor/System: 强制 `isolation: worktree` (CC) 或 `git worktree add + --cwd` (CX)
- 并行 ≥ 2 subagent 改文件时: 强制 worktree 隔离

### review stage (3 subagent 并行)
- `reviewer` + `spec-compliance` + `evaluator` 同时跑
- spec-compliance 检查 design.md vs git diff (MISSING/EXTRA/DEVIATED)
- evaluator 给 VERDICT (PASS/CONCERNS/REWORK/FAIL) 写入 _index.next_action

### polish stage (Refactor/System 强制)
- spawn `polish_worker` (workspace-write, network=true 查最佳实践)
- 产出 cleanup-pass.md

### ship stage
- 主 agent commit + push
- Refactor/System 还需检查 architecture/ 更新 (delivery-gate)

## 历史 (由 pace-continuator hook 自动追加, 最多保留近 10 条)
- `2026-07-14 09:40:12`: stage=plan sprint=2026-07-14-first-biz-module-loop turn-end
- `2026-07-14 08:36:00`: stage=ship sprint=2026-07-14-be-env-compose turn-end
- `2026-07-14 08:18:05`: stage=plan sprint=2026-07-14-be-env-compose turn-end
- `2026-07-14 07:44:22`: stage=ship sprint=2026-07-09-be-runtime-contract-hardening turn-end
- `2026-07-11 09:06:15`: stage=impl sprint=2026-07-09-be-runtime-contract-hardening turn-end
- `2026-07-10 12:42:22`: stage=ship sprint=2026-07-09-s4-fe-scaffold-loop-verify turn-end
- `2026-07-10 04:14:29`: stage=design sprint=2026-07-09-s4-fe-scaffold-loop-verify turn-end
- `2026-07-07 03:03:48`: stage=ship sprint=2026-07-07-fe-be-convention-pack-expansion turn-end
- `2026-07-07 01:38:44`: stage=ship sprint=2026-07-06-s2-scaffold-loop-verify turn-end
- `2026-07-06 12:34:04`: stage=ship sprint=2026-07-06-ai-capability-architecture-design turn-end
- 2026-07-06 Claude critic Round 1: NEEDS_REVISION (F1 P0 归属冲突) → Round 2 re-scope: chat→独立 ai-service, ToolRegistry→quantum-mcp; 治理文档 docs/ai-sprint-design.md
- 2026-07-06 impl (S1): Convention Pack 模板补全 + runtime-verify 试算
- 2026-07-06 runtime-verify PASS: 模板实例化 sys_notice 编译一次通过 (9/9 BUILD SUCCESS); 发现并回写 groupId 约定缺口 → stage=review
- 2026-07-06 决策: MCP 授权=OAuth 2.1 (用户拍板, S3 解锁); 交叉 review 用户线下进行; skills 双端(CC/CX)安装包就绪
- 2026-07-07 state closeout: active_worktrees 清零; FE/BE Convention Pack 遗留改为已清理 + scaffold-page-gen 后续实跑项
- 2026-07-07 S3 preflight design: token store / consent / manifest 三项完成, stage=ship
- 2026-07-07 S3 impl: quantum-mcp OAuth/MCP skeleton 完成, runtime/review/polish/architecture 落盘, stage=ship
- 2026-07-08 S3 rework: OAuth token scope 进入 MCP tool guard; quantum-mcp 13 tests PASS
