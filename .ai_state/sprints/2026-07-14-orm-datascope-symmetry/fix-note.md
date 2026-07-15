# Fix Note — orm-datascope-symmetry (2026-07-15)

> Bugfix 三件套: report/analyze 见同目录 route-note.md (输入段 + Analyze 段), 本档为 fix-note。

## 缺陷

`DataPermissionInterceptor.applyDataScope` 的 `DEPT` (deptId==null) 与 `SELF` (userId==null) 分支
在字段为 null 时**静默不注入任何过滤条件** (fail-open), 与同方法内 `DEPT_AND_CHILD/CUSTOM` 的
`else → and("1 = 0")` 兜底、`DENY_ALL` case 及 `default` fail-closed 兜底**不对称**。
来源: proposals P7 (be-hardening generator 盘点发现, pre-existing); 生产路径 deptId/userId 恒有值
(登录时计算), 仅手工构造 permission 或未来新调用面失误时触达 — 触达即越权面。

## 修法 (TDD 红→绿)

1. **RED**: 先写 2 个测试 `deptWithNullDeptIdShouldFailClosed` / `selfWithNullUserIdShouldFailClosed`
   (断言 SQL 含 `1 = 0`), 实跑确认恰好这 2 个失败、其余 16 个不受影响 — 现状 fail-open 实证。
2. **GREEN**: 两个 case 各补 `else -> queryWrapper.and("1 = 0")` (共 8 行含注释), 与既有 fail-closed 语义对称。
3. 复跑: orm **18/18 PASS**; 全量 `mvn -pl quantum-server -am test` **11 模块 Reactor BUILD SUCCESS** —
   无任何既有用例依赖"null 字段静默放行"语义 (route-note 廉价退出条款未触发)。

## 影响面

- `DataPermissionInterceptor.java` 2 个 case 分支 (+8 行) + `DataPermissionInterceptorTest.java` (+2 测试)。
- 业务模块零改; 行为变化仅覆盖异常态 (正常链路无感知)。
- 至此 `DataScopeType` 全部枚举值 × 全部字段空缺组合均为 fail-closed, quantum-backend
  已知安全债清零 (proposals P7 闭环)。

## 复核

主线程自审 (Bugfix 路径, 按 athena-review 仅 reviewer 维度; F3 fallback 先例):
diff 8 行逐行核 — 恒假条件字符串与既有三处完全一致 (`"1 = 0"`), 无新分支泄漏;
TDD 红绿差分即行为证明; 全量回归为无回归证据。
