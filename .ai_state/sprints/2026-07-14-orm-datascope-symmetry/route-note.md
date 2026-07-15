# Route Note — 2026-07-14-orm-datascope-symmetry

> v9.9.3 路由审议落盘。

- **输入**: 清最后一条已知安全债 — `DataPermissionInterceptor` 的 `DEPT` (deptId null) / `SELF` (userId null)
  case 内字段级 fail-open, 与 `DEPT_AND_CHILD/CUSTOM` 的 `else → 1=0` 及 `DENY_ALL/default` 兜底不对称
  (proposals P7, be-hardening generator 盘点发现, 用户拍板"弄完")
- **候选**: Bugfix (证据: 修 pre-existing 缺陷, 非新能力; 影响面 = 单文件 2 个 case 分支 + 测试) vs
  Feature (反对: 无新功能面, 无 spec 需求, Feature 档案要求对 6 行修复过重)
- **权衡**: 爆炸半径=orm 数据域 SQL 注入条件 (生产路径 deptId/userId 恒有值, 行为变化仅覆盖异常态) ·
  可逆=高 · 紧急=低 (债) · 不确定性=低 (be-hardening 已有同构先例 + 测试基建)
- **决策**: **Bugfix** · 置信度 0.9
- **假设**: 现有测试与业务无依赖"null 字段静默放行"语义 (全量回归验证)
- **廉价退出**: 全量回归若暴露依赖 fail-open 的用例 → 停, 上报逐例分析
- **家**: 本仓 .ai_state; 占位 sprint `2026-07-14-first-biz-module-loop` (plan, 等点题) 暂让指针, 本 sprint ship 后切回

## Analyze (Bugfix 三件套之二, 并入本档)

- **根因**: 三类数据域分支的 null 处理策略不一致 — DEPT_AND_CHILD/CUSTOM 在 be-hardening 前即有 fail-closed
  else; DEPT/SELF 是更早的代码, 写时假设"值恒在", 未跟进对称加固。
- **触发条件**: `DataPermissionContext` 有 permission 且 type=DEPT/SELF, 但 deptId/userId 为 null —
  正常登录链路不可达 (登录时已算好), 仅在手工构造 permission 或未来新调用面失误时暴露。
- **修法**: 两个 case 各补 `else -> queryWrapper.and("1 = 0")`, 与既有 fail-closed 语义完全对称; TDD 先红后绿。
