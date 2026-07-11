# Runtime Verify — be-runtime-contract-hardening (2026-07-10)

> impl 由 generator subagent (a1f413b14a6179c9e, DataScope 代码) + 主 agent (文档) 完成;
> 主 agent 独立复核编译/测试 (S2 先例, 不采信自报)。

## 非 servlet 调用点盘点复核 (验收 5)

| 项 | 结论 | 证据 |
|---|---|---|
| `@DataScope` 用法总数 | **2 处** (SysUserServiceImpl:66,77, 均 DEPT_AND_CHILD) = critic 基线 | `rg @DataScope --type java` |
| 非 servlet 入口 | 2 个, **均不触达 @DataScope 查询** | 见下 |
| `LogRetentionScheduler` (@Scheduled) | 只调 `operLogService.deleteOperLogByDays` / `loginLogService.cleanExpiredLogs` (日志清理) | rg 源码 |
| `OperLogEventListener` (@EventListener) | 只调 `operLogService.insertOperLog` (写入, 非查询) | rg 源码 |
| **廉价退出判定** | **不触发** (无 ≥3 处依赖 fail-open); 业务模块**零改** | — |

## 测试场景 (实跑, 主 agent 独立复核)

| 场景 | 类型 | 命令 | 实际输出 | 判定 |
|---|---|---|---|---|
| orm 单测 (四+一分支) | 单测 | `mvn -pl quantum-common/quantum-common-orm -am test` | aspect 3 + system 3 + interceptor 10 = **16 pass / 0 fail**, BUILD SUCCESS | ✅ |
| 全量回归 (11 模块) | 回归 | `mvn -pl quantum-server -am test` | Reactor 11/11 SUCCESS; biz-system 25 (含 UserDetailsServiceImplDataScopeTest 7 + SysUserServiceImplSecurityTest 7), mcp 17, orm 16 | ✅ |
| switch default 兜底 | 代码评审 | `rg "default ->\|case DENY_ALL" interceptor` | `case DENY_ALL -> and("1 = 0")` + `default -> and("1 = 0")` 均在 | ✅ |
| runtime-env 契约 | drill | `test-end-to-end-drill.py` | `backend-runtime-env: ok` (转绿), `failures: []` | ✅ |
| 凭证门禁 | grep | mcp-test-access.md 查 bcrypt/密码/token | PASS: 仅 `<YOUR_*>` 占位符, 无真实凭证 | ✅ |

### 四+一测试分支断言
1. **null user → DENY_ALL**: aspect.before() 后 context=DENY_ALL, applyDataScope 的 QueryWrapper.toSQL() 含 `1 = 0` (数据面空)。
2. **SystemDataScope.execute → ALL**: execute 内 before() 注入 ALL, applyDataScope 后 SQL 与 base 一致 (不过滤) + log.info 审计。
3. **admin → ALL** (既有行为回归)。
4. **枚举穷尽** (@ParameterizedTest @EnumSource): 7 值除 ALL 外全落具体条件或 default fail-closed; DEFAULT 落新增 default 分支 (原哑弹已堵)。
5. **嵌套 execute** (critic R2 P2): 内层退出后外层仍 active (AtomicInteger 深度计数), 最外层退出彻底清空; 异常路径 finally 仍清理。

## Blocked (环境降级, 诚实标注)

- **boot 实跑 200 证据**: dev profile 需 PostgreSQL(5432) + Redis(6379), 校准时实测**均 DOWN** → 无法起服 curl actuator。
  runtime-env.md / mcp-test-access.md 的值从 application.yml + pom + 已 ship 的 quantum-mcp 源码**权威取值**,
  但 "curl /actuator/health 200" 与 OAuth 全链实跑证据待中间件就绪环境补录。同 F6 drill blocked dynamic cases 先例
  (athena-runtime-verify 例外: 无运行环境降级)。验收 1 的 drill 静态检查已 PASS, 属**静态基线转绿**, 非动态 E2E。

## Reflect (还有哪里没完善)

- [x] fail-closed 核心 (null→DENY_ALL + switch default 兜底 + 嵌套逃生门) 实证覆盖, 全量回归零破坏。
- [ ] **相邻风险 (已回流 proposals)**: interceptor 的 `DEPT`(deptId null)/`SELF`(userId null) case 内字段级 fail-open,
      与 DEPT_AND_CHILD/CUSTOM 的 fail-closed else 不对称。本 sprint 范围只到 switch-default 层, 未触; 生产路径值恒在, 风险低。
- [ ] boot 实跑证据待环境 (blocked, 非本 sprint 可解)。
- [ ] mcp-test-access 的 OAuth 全链冒烟待中间件就绪 (F7 真动态 E2E 承载)。

## VERDICT: PASS (交付 1-4 全落地; boot 证据为环境 blocker, 已诚实降级)
