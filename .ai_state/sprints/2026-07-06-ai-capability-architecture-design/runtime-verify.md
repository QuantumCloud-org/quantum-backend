# Runtime Verify — Convention Pack 模板实证 (2026-07-06)

## 验证方式

模板实例化真实模块（非纸面检查）：`docs/ai/convention-pack/templates/` 11 个模板
→ sed 替换占位符生成 `quantum-biz-notice`（SysNotice：title/content/status，无部门维度，
按约定裁掉 @DataScope 分支）→ 注册根 pom → 实跑编译。

## 证据

```
$ mvn -B -pl quantum-biz-notice -am -DskipTests compile
[INFO] Building quantum-biz-notice 1.0.0    [9/9]
[INFO] BUILD SUCCESS
```

10 个 Java 文件（Entity/Mapper/IService/ServiceImpl/Controller/Query/Create/Update/VO/Convert）
一次编译通过，含 MyBatis-Flex processor 生成 SysNoticeTableDef 并被静态导入解析。

## 实跑发现并已修复

- F: common 模块依赖坐标是 `com.alpha.<module>:quantum-common-<module>`（如 com.alpha.logging），
  不是统一 `com.alpha`——首次编译因此失败。已回写 conventions.md「新增模块 pom」一节。

## 现场清理

试算模块验证后删除、根 pom 还原，不进 git 历史；本文件为证据存档。

## 测试场景 (实跑)

### 场景 1 (S1 原始, Codex 2026-07-06): 无部门维度实体全层编译

见上文「验证方式/证据」段: SysNotice (title/content/status, 无 dept) 10 文件一次编译通过, Reactor 9/9。

### 场景 2 (polish 后复验, CC 2026-07-06): 带部门维度实体 + 加固模板 + G1-G4 门禁实跑

背景: review pass1 P1#1 指出场景 1 用无 dept 实体, 数据权限分支从未被编译路径触达;
polish 又把模板校验从 TODO 注释改为默认启用 (新增 UserContext/LoginUser/DataScopeType 引用 + switch 表达式),
故用带 `deptId` 的试算实体 ScopeTrial 重新实证。

实跑步骤: templates/ 5 模板 (Entity/Mapper/IService/Query/ServiceImpl) → python 占位替换,
Entity 补 `private Long deptId;` → 落入 quantum-biz-system → 编译 → 跑 validate.md §2 门禁 → 删现场。

```
$ mvn -B -pl quantum-biz-system -am -DskipTests compile
[INFO] Building quantum-biz-system 1.0.0                                  [9/9]
[INFO] BUILD SUCCESS
```

一次编译通过: assertReadable/assertWritable/assertInDataScope 三个辅助方法、
DataScopeType switch 全分支、MyBatis-Flex processor 生成 ScopeTrialTableDef 均解析成功。

```
$ # validate.md §2 安全门禁 (首次实战)
G1 PASS   # update/deleteByIds 含 assertWritable(
G2 PASS   # selectById 含 assertReadable(
G3 PASS   # insert 含 assertInDataScope(
G4a PASS  # menu-permission.sql 实例化后无 {{ 占位残留
G4b PASS  # 5 ID 无重复: ['2100', '2101', '2102', '2103', '2104']
```

现场清理: 5 个试算 Java 文件 + /tmp sql 已删, `git status` 无 Scope* 残留 (grep 计数 0)。

### 结论

加固后的 Convention Pack 在两种实体形态 (有/无 dept 维度) 下均可一次编译通过,
G1-G4 门禁可执行且全 PASS — review P1#1 指出的实证盲区已闭合。
