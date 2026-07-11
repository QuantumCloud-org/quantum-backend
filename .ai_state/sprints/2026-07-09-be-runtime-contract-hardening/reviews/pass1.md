# BE Hardening Review Pass 1 — 2026-07-10

> reviewer (a070e22ecc40f9031) + spec-compliance (a9b3d30a436de4222) 独立并行, 均亲跑命令复核;
> evaluator 综合内联 (两家零 P0/P1 干净 PASS, 机械综合, 不再 spawn 第三 subagent — 遵用户"不要过度 review")。

## Reviewer (代码层 findings)

### P0
无

### P1
无

### P2

**F1 [P2] DEPT/SELF case 内字段级 fail-open 不对称 (已知, 已回流, 非本 sprint 阻塞项)**
- File: `DataPermissionInterceptor.java:45-49,65-69`
- 问题: `DEPT` case 在 `deptId == null`、`SELF` case 在 `userId == null` 时不追加过滤 (静默放行), 与 DEPT_AND_CHILD/CUSTOM 的 `else -> and("1=0")` 不对称。生产路径 deptId/userId 恒有值, 风险低; design 明确本 sprint 只修 switch-default 层。
- 依据: 已在 proposals.md (2026-07-10) 回流 + runtime-verify Reflect 标注。建议: 无需本轮修复。

### INFO
**F2 [INFO] `DataScopeAspect.before()` 方法体 40 行, 卡 P0 上限**未超限; 三分支互斥, 现在拆分反增调用面; 未来再加分支时抽私有方法。

### 维度小结
1. Correctness: OK (三序分支顺序正确, fromCode 兜底仍 SELF 未被 DENY_ALL 污染, 独立验证 16/16 pass)
2. Security: OK (switch 穷尽 case DENY_ALL + default 均注入 1=0, null-user fail-closed 用 toSQL().contains("1 = 0") 真实数据面断言, mcp-test-access grep 无凭证)
3. Test: OK (独立重跑 16 pass; @EnumSource 真遍历全枚举断言非空转; 嵌套 execute 真断言外层存活 + 异常路径清理)
4. Design 一致: OK (git diff --stat 仅 orm+docs+.ai_state, 业务模块零改, 盘点独立复核一致)
5. Code quality: OK (无重复/无硬编码密钥/无裸类型; before() 贴近未超 40 行)

## Spec Compliance (spec-compliance)

### MISSING (做少了)
无。交付 1-4 + 7 条验收标准逐一核对有对应实现:
- 交付1 runtime-env.md (health 复用 /actuator/health 未新增端点); 交付2 mcp-test-access.md (grep 仅 `<YOUR_*>` 占位);
  交付3 DENY_ALL + interceptor default 兜底 + SystemDataScope(AtomicInteger 深度计数) + aspect 三序 + 16 测试;
  交付4 conventions 验证实体原则。
- 验收5 盘点落 route-note+runtime-verify (2 处 @DataScope, 2 非 servlet 入口不触达)。验收7 Reactor 11/11 SUCCESS。
- 验收1 boot 200 证据因 postgres/redis DOWN blocked, design 风险表 L114 预留环境降级空间, runtime-verify/route-note 诚实标注 (静态基线转绿), drill BE 两项 PASS 满足 design L119-120 口径 → **可接受降级, 非 MISSING**。

### EXTRA (做多了)
- E1 [合理]: proposals.md +1 (DEPT/SELF 相邻风险回流) — 盘点发现如实回流, 不扩交付面, 合铁律[Hook 是进化器]。
- 无代码侧 scope creep: git diff --name-only 排除 .ai_state/docs 后仅 orm 下 3 改 + 2 新增 permission 类, 与影响范围表精确对应, 无表外业务模块改动。

### DEVIATED (做偏了)
无实质偏离。两处 design 内已授权的实现自由度: switch 语句+default (design 并列等价选项); SystemDataScopeContext 用 AtomicInteger (兑现 impl 关注清单 P2 深度计数器要求)。

### 总评: PASS (MISSING=0, EXTRA=1 合理, DEVIATED=0)

## Evaluator VERDICT (2026-07-10, 主 agent 综合)

两家独立复核均 PASS, 零 P0/P1:
- reviewer 5 维度全 OK, 亲跑 16 测试确认; 唯一 P2 (DEPT/SELF fail-open) 已回流 proposals, 非本 sprint 范围。
- spec-compliance MISSING/DEVIATED=0, 唯一 EXTRA 是合理的风险回流。
- 验收 1 的 boot 证据为环境 blocker (postgres/redis DOWN), 已诚实降级 (design 风险表预留 + drill 静态检查 PASS), 属可接受降级而非缺陷。

Evidence Cross-Check: checklist 8 done 项 evidence 均指向真实产物 (16 测试 / drill ok / grep pass / diff 零业务改动), done_without_evidence=0。

VERDICT: PASS

### next_action: ship
Feature 路径无强制 polish。安全变更 (DataScope fail-closed) 核心已实证 (真实数据面 1=0 断言 + 全量回归零破坏)。
遗留 (非阻塞, 已记录): DEPT/SELF 字段级 fail-open (proposals, 下次 orm 加固); boot 实跑证据 (环境 blocker, F7 承载)。

注: design.md 于 verdict 后追加 "Critic Findings" 存档段 (gate 要求的审议记录格式化补录, 无设计内容变更), 不触发 re-review。
