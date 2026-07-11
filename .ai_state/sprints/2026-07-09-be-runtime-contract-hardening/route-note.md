# Route Note — 2026-07-09-be-runtime-contract-hardening

> v9.9.0 路由审议落盘.

- **输入**: F6 动态 E2E blocker 2 条落 BE (runtime-env.md 缺失 / OAuth 测试账号交接文档缺失) + ai-sprint-design.md §9 框架加固待办 (DataScopeAspect 无用户上下文 fail-open)
- **候选**: Quick (证据: 2/3 交付物是纯文档) vs Feature (证据: fail-closed 是 quantum-common-orm 安全行为变更, 需 TDD + 非 servlet 调用点盘点; 反对 Quick: 跨 docs + orm 两处, 超绿区单文件 30 行上限)
- **权衡**: 爆炸半径=orm 数据权限链 (所有 @DataScope 查询) · 可逆=高 (git) 但行为变更影响定时任务 · 紧急=中 (F6 动态解锁前置) · 不确定性=中 (非 servlet 调用点数量未盘点)
- **决策**: **Feature** · 置信度 0.85
- **假设**: 现有定时任务/消息消费中不存在依赖 fail-open 语义的 @DataScope 查询 (impl 第一步盘点验证)
- **廉价退出**: 若盘点发现 ≥3 处合法无用户调用依赖跳过语义 → fail-closed 方案降级为"显式 SystemContext 白名单 + 逐处迁移"另立 sprint, 本 sprint 只交付两份文档
- **家**: sprint 档案落本仓库 .ai_state

## impl 第一步盘点结论 (2026-07-10, Opus)

- **@DataScope 用法**: 仅 2 处 (`SysUserServiceImpl.java:66,77`, 均 DEPT_AND_CHILD) = critic 基线, 廉价退出**不触发**。
- **非 servlet 入口 2 个, 均不触达 @DataScope 查询**:
  - `LogRetentionScheduler` (@Scheduled): 只调 `operLogService.deleteOperLogByDays` / `loginLogService.cleanExpiredLogs` (日志清理, 非 @DataScope 方法)。
  - `OperLogEventListener` (@EventListener): 只调 `operLogService.insertOperLog` (写入, 非查询)。
  - 结论: fail-closed 行为变更**安全, 业务模块零改**。
- **boot 校准可行性**: dev profile 需 PostgreSQL(5432) + Redis(6379), 实测**两者均 DOWN**。真 boot + curl actuator 校准**不可行** → runtime-env.md 从 application.yml/pom 权威取值, AC1 的 "boot 200 证据" 降级为**记录 blocker** (同 F6 drill blocked dynamic cases 先例; athena-runtime-verify 例外条款: 无运行环境降级)。不阻塞交付。
