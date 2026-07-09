# Design — BE 运行时契约补齐 + DataScope fail-closed 加固

> Sprint: `2026-07-09-be-runtime-contract-hardening` · Path: Feature · 置信度 0.85
> 状态: design Round 2 (critic Round 1 F1/F2 P0 + F3/F4 P1 已修) → 待确认进 impl

## 背景

三个遗留项收敛为一个 BE sprint:

1. **runtime-env.md 缺失** — F6 drill blocker: `docs/ai/convention-pack/runtime-env.md` 不存在,
   drill 无法安全推断 BE 启动命令与探活 URL (FE 侧已有同名文件, BE 缺位)。
2. **OAuth 测试账号交接文档缺失** — MCP endpoint 的 OAuth/测试账号信息无 repo-safe 载体。
   **注意 (critic F4)**: drill 脚本的 `test_account_doc` 检查实际指向的也是 `runtime-env.md`
   (test-end-to-end-drill.py:95-97 与 :58-62 同路径) — 即 drill **不校验** `mcp-test-access.md`,
   该文档质量仅由本 sprint 的 grep 门禁 + review 保障; drill 脚本路径重复已作为提案回流 Rlues (proposals.md)。
3. **DataScopeAspect fail-open** — ai-sprint-design.md §9 明列的框架加固待办:
   `DataScopeAspect.before()` 在 `UserContext.getUserId() == null` 时 `log.debug + return` 静默跳过
   (DataScopeAspect.java:59-62), `DataPermissionInterceptor.applyDataScope()` 遇 null context 直接 no-op。
   常规 HTTP 链路认证前置不可达, 但这是**所有非 servlet 入口**的共同隐患。MCP 入口已在 S3 自保, 框架层缺口未修。
   7-06 "数据域 fail-closed" 修的是**有 permission 无部门集合**分支, 与本项不同层。

**盘点实测支撑 (critic 已验)**: 全仓 `@DataScope(` 注解当前仅 2 处 (`SysUserServiceImpl.java:66,77`),
无 `@Scheduled`/MQ listener 触达 — route-note "预期 0-2 处"假设成立, 廉价退出条款大概率不触发。

## 方案

### 交付 1: `docs/ai/convention-pack/runtime-env.md`

与 FE 同构的表格声明 (消费方只读此文件, 不从 pom 推断):

| key | 值 (impl 时实跑校准) |
|---|---|
| dev_command | `mvn -pl quantum-server -am spring-boot:run` (或等价 `java -jar` 产物路径) |
| port | `8080` (application.yml server.port) |
| health_url | `http://127.0.0.1:8080/actuator/health` |
| teardown | 前台进程 Ctrl-C 或 kill 占用 8080 的进程 |

**health_url 决策 (critic F1 P0 修正)**: 原设计"无 actuator、无 /health"前提**失实** —
`spring-boot-starter-actuator` 已是 `quantum-common-framework` 直接依赖, `application.yml` 已暴露
`health,info,prometheus,metrics` 且 `show-details: when-authorized`, `SecurityConfig` 已将
`GET /actuator/health` permitAll。**直接复用 `/actuator/health`, 不新增任何端点** (原案 A 自定义
/health controller 删除 — 重复建设且反而扩大暴露面)。
已知取舍: actuator health 聚合 datasource 检查, DB 抖动会拖累存活判定 — 对 drill 场景可接受
(探活语义本就要求依赖就绪); 若未来需要纯存活探针, 另议 liveness/readiness 分组, 不在本 sprint。

另加 MCP 小节: `ai.mcp.enabled=false` 默认关闭, 开启方式 + `AI_MCP_ISSUER` / `AI_MCP_RESOURCE` 环境变量说明。

### 交付 2: `docs/ai/mcp-test-access.md` (repo-safe)

- 静态 client allowlist 配置示例 (application.yml `ai.mcp` 节, client_id/redirect_uri 用占位符)。
- 测试账号 provisioning 步骤: 基于 deploy/init.sql 初始管理员 → 建独立测试账号 + 最小权限角色
  (仅 manifest tools 对应的 `system:*:list/query` 权限点), 密码走环境变量引用。
- OAuth 2.1 全流程手册: `/.well-known/oauth-protected-resource` → `/oauth/authorize` (PKCE S256 + consent)
  → `/oauth/token` → Bearer 调 `/mcp`。
- **门禁**: 占位符统一 `<YOUR_*>` 形态, ship 前 grep 校验无真实凭证。

### 交付 3: DataScopeAspect fail-closed (TDD)

**行为变更**: `userId == null` 时不再静默跳过, 改为注入**恒假数据权限** + `log.warn`。

**机制三件 (critic F2 P0 / F3 P1 修正后)**:

1. **DENY_ALL 语义**: `DataScopeType` 枚举新增 `DENY_ALL`; interceptor 的 switch **改为 switch 表达式
   (编译期穷尽) 或显式 `default -> queryWrapper.and("1 = 0")` 兜底** — 现有 switch 语句无 default,
   枚举漏 case 会编译通过且静默不过滤 (把"拒绝"变"放行", 与目标相反), 必须堵死。
2. **逃生门机制选定**: 新增独立 ThreadLocal 标记 `SystemDataScopeContext` +
   `SystemDataScope.execute(Supplier)` 包装器 (try/finally 必清 ThreadLocal, 防泄漏)。
   `DataScopeAspect.before()` 判定顺序: ① SystemDataScopeContext 激活 → 注入 ALL + `log.info` 审计行
   ② userId == null → 注入 DENY_ALL + `log.warn` ③ 现有 admin/正常链路不变。
   **否决备选**: 伪造 synthetic system LoginUser 塞 UserContext — 会污染 `isAdmin()` 语义与操作审计字段,
   且影响面不可控。独立标记 = 侵入面最小、审计可 grep。
3. **上下文清理**: DENY_ALL/ALL 注入复用现有 `DataPermissionContext` 生命周期; impl 首日核实其
   清理时机 (@After/finally), 确保新分支同路径清理, 无跨请求残留。

**否决备选 (原 §9 两案之一)**: 抛未认证异常 — 把静默漏洞翻转成任务崩溃, 可用性风险 + 迁移成本陡增;
恒假条件保证数据面 fail-closed 且 warn 可观测, 留出显式化窗口。

**TDD 顺序 (先测后码, 四分支)**:
1. null user + @DataScope 查询 → wrapper 含 `1 = 0` 且结果空;
2. `SystemDataScope.execute` 内 → 不注入过滤 + 审计日志;
3. admin → ALL (既有行为回归);
4. **枚举穷尽 (critic F2)**: 参数化测试遍历 `DataScopeType` 全部值, 断言 interceptor 每个值都有非默认处理
   或落入 fail-closed default (防未来新增枚举值再造哑弹)。

**impl 第一步 = 盘点复核**: 以 critic 实测 (2 处 @DataScope, 无 @Scheduled 触达) 为基线重跑 grep 确认;
命中处逐个判定显式 `SystemDataScope` 或无需数据域, 结论落 runtime-verify.md。触发廉价退出 (≥3 处依赖
fail-open) 则按 route-note 降级。

### 交付 4 (顺手, 制度化 critic G3): BE Convention Pack `conventions.md` 增补"验证实体选择原则"

一句条款: 验证/演示实体必须含部门或角色关联字段 (SysNotice 式简单实体掩盖数据权限分支的教训,
见 compound/2026-07-06-learning)。与 FE 侧 (S4 步骤 3) 对称落地。

## 影响范围

| 位置 | 改动 |
|---|---|
| `docs/ai/convention-pack/runtime-env.md` | 新增 (文档) |
| `docs/ai/mcp-test-access.md` | 新增 (文档) |
| `docs/ai/convention-pack/conventions.md` | 增补验证实体条款 |
| `quantum-common-orm` | DataScopeType.DENY_ALL + interceptor default 兜底 + SystemDataScope(Context) + DataScopeAspect 分支 + 单测×4 |
| `.ai_state/proposals.md` | 回流 Rlues: drill test_account_doc 路径重复 |
| 业务模块 | 仅盘点命中处显式化 (实测基线 2 处, 预期 0 改) |

**不新增**: /health controller (复用 actuator), spring-boot-actuator 依赖 (已存在)。

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| 定时任务/未来非 servlet 入口 fail-closed 后静默查空 | 盘点复核 + warn 日志可观测 + 全量 `mvn test` 回归 |
| 枚举新增值绕过 fail-closed | switch 穷尽/default 兜底 + 枚举穷尽参数化测试 (TDD ④) |
| ThreadLocal 泄漏 | SystemDataScope.execute try/finally 强制清理 + 单测断言执行后上下文为空 |
| runtime-env 值与真实启动不符 | impl 实跑 boot + curl /actuator/health 校准后落盘 |
| 测试账号文档泄漏凭证 | 占位符纪律 + ship 前 grep 门禁 |

## 验收标准

1. `runtime-env.md` 存在且值经实跑校准 (boot 日志 + `curl http://127.0.0.1:8080/actuator/health` 200 证据);
   重跑 `test-end-to-end-drill.py`, BE 两项检查 PASS — 明确这只是 **drill 静态基线转绿**, 非动态 E2E。
2. `mcp-test-access.md` 存在; grep 无真实凭证命中。
3. orm 四分支单测全 PASS (null→DENY_ALL / SystemDataScope→ALL+审计 / admin 回归 / 枚举穷尽)。
4. interceptor 对 `DataScopeType` 全值穷尽处理 (switch 表达式或 default fail-closed), 代码评审可见。
5. 非 servlet 调用点盘点复核清单落 runtime-verify.md。
6. BE conventions.md 含验证实体选择条款。
7. 全量 `mvn test` BUILD SUCCESS (回归)。

## 依赖与衔接

- 与 S4 共享 BE `.ai_state` 单例状态字段: **串行推进, S4 先行** (critic G1; 用户指令续 S4),
  本 sprint 在 S4 ship 后接棒 `current_sprint_slug`。
- 本 sprint 的 runtime-env.md 是 S4 备选 C (真实 BE 联调) 的解锁前提 — 若 S4 中途触发降级通道,
  本 sprint 插队先交付交付 1。
- 真·跨服务动态 E2E 为独立待办 (见 _index.next_action), 不在本 sprint。

## impl 关注清单 (critic Round 2 P2, 非阻塞)

1. `SystemDataScopeContext` **用深度计数器 (int) 而非布尔标记**: 嵌套 `execute()` 时内层 finally
   会提前清掉外层标记 — TDD 补一条嵌套调用回归测试。
2. "S4 触发降级通道时本 sprint 插队" 与 `current_sprint_slug` 串行互斥的切换/切回操作步骤, 届时在
   route-note 追加 re-route 记录并显式换指针, 不留隐式状态。
