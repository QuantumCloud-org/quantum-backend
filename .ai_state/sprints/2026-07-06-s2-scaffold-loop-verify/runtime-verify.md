# Runtime Verify — S2: scaffold-module-gen 端到端编译闭环 (2026-07-06)

## 验证方式

CC generator subagent (agent-a97f09563e06d95f2) 按 scaffold-module-gen skill 工作流完整执行:
读 Convention Pack (conventions.md + validate.md + 全 11 模板) → 实例化独立模块 `quantum-biz-asset`
(Asset 实体**含 dept 维度**: assetName/assetCode/deptId/status, 权限 asset:asset:*, 菜单号段 2200-2204)
→ 根 pom 注册 → 编译自修环 → G1-G4 门禁。主 agent 随后**独立复核** (不采信 subagent 自报)。

与前两轮实证的差异: 首次全 11 模板 + 独立 maven 模块 + skill 完整工作流 (前两轮分别是
无 dept 实体旧模板 / 手动 5 模板局部替换)。

## 测试场景 (实跑)

### 场景 1: generator 编译自修环

第 1 轮 (唯一一轮) 即通过, 零返工:

```
$ mvn -B -pl quantum-biz-asset -am -DskipTests compile
[INFO] Building quantum-biz-asset 1.0.0                                   [9/9]
[INFO] BUILD SUCCESS
```

MyBatis-Flex processor 产出 AssetTableDef, 静态导入解析成功; S1 踩过的 common 依赖坐标坑
(com.alpha.<module>:quantum-common-<module>) 未复现 — conventions.md 回写生效。

### 场景 2: 主 agent 独立复核 (编译 + G1-G4)

```
$ mvn -B -pl quantum-biz-asset -am -DskipTests compile   # 主 agent 重跑
[INFO] Building quantum-biz-asset 1.0.0                                   [9/9]
[INFO] BUILD SUCCESS

$ # validate.md §2 门禁, 主 agent 重跑
G1 PASS   # update/deleteByIds 调 assertWritable
G2 PASS   # selectById 调 assertReadable, 非裸 getById
G3 PASS   # insert 调 assertInDataScope
G4a PASS  # menu-permission.sql 无 {{ 残留
G4b PASS  # 行首 ID 2200-2204 无重复 (注: 全文第二个 "(2200" 是自查注释 IN (2200,...) 的合法引用)
```

抽查 AssetServiceImpl: @DataScope 与 applyDataScope 成对 (L33/36); assertReadable(L47)/
assertInDataScope(L58)/assertWritable(L74,89) 全为真实调用; 辅助方法三件套齐 (L113-118)。
实体含 deptId, 走非豁免路径, 无 data-scope-exempt 行 — 符合规则。

### 场景 3: 现场清理与回归

生成物 (quantum-biz-asset/ 13 文件 + 根 pom 注册行) 验证后回滚删除, `git status` 无残留;
清理后全量 `mvn test` BUILD SUCCESS (见 ship 前证据) — 仓库还原无损。

## 实跑发现

1. (建议级, 已修) menu-permission.sql 落库路径无统一约定 (本次放 src/main/resources/sql/),
   已回写 conventions.md 显式规定。
2. 无其他缺口: 占位符/字段类型/pom 坐标/SQL 列结构与 Convention Pack 描述一致。

## 结论

S2 验收标准 5/5 达成。skill 工作流端到端可执行, 零返工一次通过 — Convention Pack 判定可生产使用。
S1 polish 加固 (数据权限默认启用 + G1-G4) 在 generator 无提示情况下被自然遵循, 门禁设计闭环生效。
